# Explicación del código

Esta guía recorre los archivos y la lógica del prototipo. El orden sigue el recorrido normal: arranque, carga del juicio, conversión de diálogos, entrada del teclado, puntaje y dibujo.

## Cómo se conectan las clases

```text
Principal
  └─ crea PanelTribunal
       ├─ CargadorGuion lee el JSON como GuionJuicio
       ├─ LineaDialogo convierte cada línea en tramos de texto
       ├─ recibe el teclado y actualiza reloj/puntajes
       └─ dibuja la sala, el acta y el progreso
```

`GuionJuicio` es el formato de datos del archivo. `LineaDialogo` transforma esos datos para que el juego pueda procesar cada carácter. `PanelTribunal` coordina la partida y presenta el resultado.

## `Principal.java`: arranque de la aplicación

`Principal` contiene el punto de entrada de Java.

- `main(String[] argumentos)`: método que la máquina virtual de Java exige para iniciar una aplicación. `argumentos` son parámetros opcionales recibidos desde la terminal; este prototipo no los usa.
- `SwingUtilities.invokeLater(...)`: programa la creación de la interfaz en el hilo de eventos de Swing, que es el hilo donde Swing espera que se creen y actualicen sus ventanas.
- `JFrame ventana`: crea la ventana del juego. El título visible del producto sigue siendo “Thanks, Typing!”.
- `setDefaultCloseOperation(EXIT_ON_CLOSE)`: cierra el proceso al cerrar la ventana.
- `PanelTribunal panel`: crea el panel que contiene toda la escena y la lógica de la partida.
- `setContentPane(panel)`: coloca ese panel dentro de la ventana.
- `WindowAdapter` y `windowOpened(...)`: cuando la ventana aparece, pide el foco del teclado para que se pueda empezar a tipear inmediatamente. `windowOpened` conserva ese nombre porque es el método definido por Swing que se está sobrescribiendo.
- `setPreferredSize`, `pack`, `setLocationRelativeTo` y `setResizable`: fijan un tamaño inicial de 1280 × 720, ajustan la ventana a ese contenido, la centran y permiten cambiar su tamaño.
- `setVisible(true)`: muestra la ventana.

## `CargadorGuion.java`: lectura y validación del JSON

Esta clase convierte el archivo UTF‑8 en objetos Java y comprueba que el guion tenga los datos mínimos.

- `cargar(Path ruta)`: recibe la dirección del archivo y devuelve un `GuionJuicio`. Puede lanzar `IOException` si no se puede leer el archivo.
- `Files.readAllBytes(ruta)`: lee todos los bytes del JSON. `StandardCharsets.UTF_8` los interpreta como texto UTF‑8 para preservar tildes y `ñ`.
- `new Gson().fromJson(...)`: la biblioteca Gson interpreta el JSON y llena los campos de `GuionJuicio` y `LineaGuion` cuyos nombres coinciden con las propiedades JSON.
- La primera condición rechaza un JSON vacío o sin `lineas`.
- El ciclo revisa cada línea. `hablante`, `emocion` y `texto_original` son obligatorios; `tiempo_limite` debe ser mayor que cero.
- `LineaDialogo.crearDesde(linea)`: además de revisar los campos simples, intenta interpretar las marcas de color. Así se detectan marcas mal formadas, alternativas amarillas inválidas o una respuesta correcta que no exista entre las alternativas.
- `JsonParseException`: comunica errores del formato del juicio. Si ocurre una excepción al interpretar una línea, se conserva el mensaje original para ayudar a encontrar el problema.

## `GuionJuicio.java`: modelo de los datos

Gson necesita clases cuyos campos correspondan a las propiedades del JSON.

### `GuionJuicio`

- `identificador_juicio`: número identificador del juicio.
- `titulo`: título del juicio.
- `lineas`: lista de todas las intervenciones, en el orden en que se jugarán.

### `GuionJuicio.LineaGuion`

Es la estructura de una entrada dentro de `lineas`:

- `identificador`: número de la línea.
- `hablante`: quién dice la frase, por ejemplo `Fiscal`.
- `emocion`: estado indicado por el guion. Se conserva como dato para futuras expresiones visuales.
- `texto_original`: frase con las marcas `[ROJO:...]`, `[VERDE:...]` y `[AMARILLO:...]`.
- `tiempo_limite`: tiempo disponible, en segundos.
- `opciones_correctas`: lista con una respuesta válida por cada marca amarilla de la frase, en el mismo orden.

## `LineaDialogo.java`: convertir una frase en tramos

El texto del JSON mezcla texto normal y marcas. Esta clase lo divide en partes ordenadas para que el panel pueda dibujar y procesar cada regla por separado.

### Campos de `LineaDialogo`

- `identificador`, `hablante`, `emocion` y `segundos`: copia los datos principales de la línea JSON.
- `tramos`: lista ordenada de objetos `Tramo`. Cada tramo representa texto normal o una marca especial.
- `PATRON_MARCA`: expresión regular que encuentra marcas con esta forma: `[ROJO:contenido]`, `[VERDE:contenido]` o `[AMARILLO:contenido]`.

### Métodos de `LineaDialogo`

- `entradaEsperada()`: concatena la entrada que debe producir el jugador para cada tramo. Para un tramo rojo crea una secuencia de espacios de la misma longitud que el texto rojo.
- `reiniciarProgreso()`: pone el progreso de cada tramo en cero, borra lo tipeado, quita la opción elegida y vuelve a habilitar todas las alternativas amarillas. Se usa al iniciar una línea y al reconstruir el estado después de Backspace.
- `crearDesde(LineaGuion origen)`: construye el modelo jugable a partir de los datos JSON.
  1. Copia hablante, emoción, identificador y tiempo.
  2. Busca la siguiente marca con `Matcher`.
  3. Convierte el texto previo a la marca en un tramo `NORMAL`.
  4. Si la marca es `ROJO`, crea un tramo `OMITIR`: muestra la palabra roja y pide una tecla por cada carácter.
  5. Si es `VERDE`, crea un tramo `AGREGAR`: muestra y pide el contenido tal como figura en JSON.
  6. Si es `AMARILLO`, separa las alternativas usando `/`, elimina espacios sobrantes alrededor de cada alternativa y verifica que haya al menos dos opciones no vacías.
  7. Comprueba que las alternativas amarillas se puedan distinguir desde el comienzo. Una alternativa no puede ser prefijo de otra: por ejemplo, `sí` y `sí, claro` serían ambiguas al empezar a escribir.
  8. Busca en `opciones_correctas` la respuesta correcta de esa marca y verifica que coincida con una alternativa.
  9. Agrega el texto posterior a la última marca y reinicia el progreso.
- `obtenerOpcionCorrecta(...)`: devuelve la respuesta correcta correspondiente a la marca amarilla que se está procesando, o `null` si falta.
- `formaDeTeclado(...)`: descompone caracteres Unicode y quita los acentos agudo y diéresis para comparar entradas. Esto permite mostrar tildes sin obligar a escribirlas; la `ñ` no se convierte en `n`.
- `agregarTextoNormal(...)`: agrega un tramo normal. Si se le pide quitar un espacio inicial, deja el espacio visible pero lo excluye de la secuencia que hay que escribir.

### Campos de `Tramo`

- `tipo`: regla del tramo: `NORMAL`, `OMITIR`, `AGREGAR` o `ELEGIR`.
- `textoVisible`: caracteres que se dibujan en el acta.
- `entrada`: caracteres que hacen avanzar el tramo. En los rojos contiene espacios; en las verdes y normales contiene el texto; en los amarillos contiene la opción correcta como referencia para el puntaje.
- `inicioEntrada`: posición inicial dentro de `textoVisible` para alinear el progreso cuando se retira un espacio inicial de la entrada.
- `opciones`: alternativas de un tramo amarillo.
- `opcionCorrecta`: alternativa amarilla que da puntaje.
- `progreso`: cantidad de caracteres que ya se aceptaron en el tramo.
- `opcionElegida`: índice de la alternativa amarilla que quedó seleccionada, o `-1` antes de elegir.
- `eleccionCompletada`: indica que una alternativa amarilla llegó al final.
- `opcionesPosibles`: alternativas amarillas que todavía coinciden con los caracteres escritos.
- `tecleado`: caracteres reales pulsados en ese tramo. Se usa para comprobar mayúsculas en las verdes y espacios en las rojas.

## `PanelTribunal.java`: estado y reglas de la partida

Es la clase central. Hereda de `JPanel`, recibe eventos de teclado, mantiene el temporizador, calcula el resultado y dibuja el juego.

### Constantes y estado

- `ANCHO` y `ALTO`: resolución lógica del dibujo, 640 × 360. Al final la imagen se amplía conservando sus píxeles.
- `TINTA`, `TEXTO_NORMAL`, `PAPEL`, `ROJO`, `ROJO_OSCURO`, `VERDE`, `VERDE_OSCURO`, `AMARILLO` y `AMARILLO_OSCURO`: colores del acta y sus estados de progreso.
- `lienzo`: imagen de 640 × 360 donde primero se dibuja cada fotograma.
- `temporizador`: evento periódico que revisa el reloj cada 100 milisegundos.
- `guion` y `linea`: juicio completo y línea que se está jugando.
- `indiceLinea`: posición actual dentro de `guion.lineas`.
- `entradaEsperada`: secuencia calculada para la línea actual.
- `cursorEntrada`: cantidad de teclas aceptadas en la secuencia actual.
- `caracteresCorrectos` y `caracteresTotales`: valores usados para el porcentaje final.
- `palabrasEspecialesCorrectas` y `palabrasEspecialesTotales`: progreso del contador de palabras rojas, verdes y amarillas correctas.
- `segundosRestantes` y `ultimoSegundo`: estado del límite de tiempo.
- `terminado`, `esperandoSiguiente` y `lineaRegistrada`: controlan si el juego ya terminó, espera Enter o ya contabilizó la línea.
- `exito`, `estado` y `errores`: resultado final, mensaje mostrado y cantidad de errores.
- `entradaEscrita`: historial de caracteres aceptados. Permite reconstruir el cursor al borrar con Backspace.

### Inicio y avance

- `PanelTribunal()`: configura el tamaño, activa el foco, carga el guion, registra el teclado y arranca el temporizador.
- `cargarGuion()`: lee `assets/dialogs/juicio_demostracion.json`, calcula cuántos caracteres y palabras especiales hay en el juicio y comienza la primera línea. Si ocurre un error, lo muestra en `estado`.
- `iniciarLinea(indice)`: crea el modelo de la línea, vacía la entrada anterior, reinicia errores y estados, carga el límite de tiempo y redibuja.
- `registrarLinea()`: suma una sola vez el puntaje y las palabras especiales de la línea. `lineaRegistrada` evita sumar dos veces.
- `avanzarOFinalizar()`: al pulsar Enter después de una línea, inicia la siguiente. Después de la última calcula el porcentaje y muestra el desenlace. Enter en la pantalla final reinicia el juicio.
- `reiniciarLinea()`: reinicia los contadores globales y vuelve a la primera línea.

### Contadores de palabras y puntaje

- `contarPalabrasEspeciales(...)`: cuenta las palabras objetivo de cada tramo rojo, verde y amarillo. En modo de puntaje cuenta solo las que se completaron correctamente.
- `contarPalabras(...)`: divide un texto por espacios para contar sus palabras.
- `contarRojasCorrectas(...)`: revisa cada palabra roja. La palabra suma cuando ya se recorrieron sus letras y cada tecla registrada para esas letras fue un espacio.
- `contarVerdesMayusculas(...)`: cuenta las palabras verdes que se completaron usando mayúsculas en todas sus letras.
- `contarCaracteresVerdesCorrectos(...)`: devuelve los caracteres verdes que aportan al puntaje. Una palabra solo aporta si está completa y toda en mayúsculas; separadores y signos se validan por separado.
- `estaPalabraEnMayusculas(...)`: verifica que la palabra se haya completado y que cada letra registrada esté en mayúscula.
- `progresoCorrecto()`: suma el progreso válido de cada tramo. En rojos solo acredita los espacios; en verdes usa la regla de palabra completamente mayúscula; en amarillos acredita la opción correcta.

### Entrada de teclado

- El `KeyAdapter` manda los caracteres escritos a `procesarTecla(...)`.
- `keyPressed(...)` maneja Backspace y Enter. Backspace elimina un carácter del historial y llama a `reconstruirProgreso()`. Enter avanza solo cuando la línea está completa o se agotó el tiempo.
- `procesarTecla(caracter)`: ignora entradas de control y acentos combinados, pide a `aplicarCaracter(...)` que procese la tecla y suma un error si no corresponde. Cuando la línea se completa, registra su resultado y espera Enter.
- `aplicarCaracter(tecleado)`: busca, en orden, el primer tramo que todavía necesita entrada.
  - En `OMITIR`, cualquier tecla hace avanzar una letra. La tecla se guarda y luego `contarRojasCorrectas` decide si fue un espacio.
  - En `ELEGIR`, conserva solo las alternativas que coinciden con la letra. Cuando queda completa una opción, la marca como elegida.
  - En `AGREGAR`, acepta mayúsculas y minúsculas para que se pueda avanzar; el puntaje verifica después que toda la palabra esté en mayúsculas.
  - En `NORMAL`, exige la letra esperada, sin obligar a los acentos agudos o diéresis.
- `mismaLetra(...)`: compara dos caracteres después de quitar los acentos aceptados.
- `esAcentoCombinado(...)`: detecta la tecla de acento separada que algunos teclados emiten antes de la vocal y evita tratarla como una letra independiente.
- `sinAcento(...)`: normaliza un carácter para hacer esa comparación.
- `lineaCompleta()`: devuelve verdadero cuando todos los caracteres rojos, verdes y normales están completos y cada tramo amarillo tiene una opción completa.
- `reconstruirProgreso()`: reinicia los tramos y vuelve a reproducir carácter por carácter el historial guardado. Se usa después de borrar.

### Tiempo y dibujo

- `actualizarReloj()`: resta los segundos transcurridos. Al llegar a cero registra lo conseguido y deja la partida esperando Enter.
- `paintComponent(...)`: método requerido por Swing. Dibuja el fotograma en `lienzo` y luego lo escala a la ventana usando interpolación de vecino más cercano.
- `dibujarSala(...)`: pinta el fondo, el estrado, los personajes y el título de la sala mediante rectángulos y colores planos.
- `dibujarPersona(...)`: compone una figura sencilla con rectángulos de ropa, cara, pelo y rasgos.
- `dibujarActa(...)`: dibuja el panel del acta, el nombre del hablante, los tramos de texto, el reloj, los errores y el contador de palabras especiales.
- `dibujarTramos(...)`: recorre los tramos de la línea. El texto normal empieza gris y se vuelve negro; el rojo, verde y amarillo usan sus versiones oscuras para señalar avance. También decide dónde aparece el punto bajo la próxima letra.
- `dibujarTexto(...)`: dibuja una alternativa amarilla carácter por carácter, ajusta el salto de línea y pinta el prefijo avanzado.
- `dibujarCursor(...)`: dibuja el punto de posición debajo de la letra objetivo y restaura el color anterior para no cambiar accidentalmente el color del texto.

## Cómo funciona una tecla, de principio a fin

1. Swing entrega una tecla a `procesarTecla`.
2. `aplicarCaracter` busca el primer tramo incompleto.
3. Si la tecla coincide con su regla, aumenta `progreso`, guarda la tecla real y avanza `cursorEntrada`.
4. `repaint()` solicita un nuevo dibujo. `dibujarTramos` usa el progreso guardado para colorear y colocar el punto.
5. Cuando `lineaCompleta()` devuelve verdadero, `registrarLinea()` guarda los puntos y Enter permite continuar.

Si se pulsa Backspace, se quita el último carácter guardado y se vuelve a aplicar el resto desde cero. Esto evita tener que deshacer por separado el estado de cada tramo y las alternativas amarillas.

## Nombres que permanecen como los define Java o Maven

Los nombres propios del proyecto y del código están en español. Algunos identificadores no se traducen porque son contratos de herramientas externas: `main` lo exige Java para arrancar; `paintComponent`, `keyTyped`, `keyPressed` y `windowOpened` son métodos de Swing que se sobrescriben; `pom.xml`, `src/main/java`, `Gson`, `Path`, `Matcher` y métodos como `start()` o `drawString()` pertenecen a Maven, Gson o las bibliotecas de Java.
