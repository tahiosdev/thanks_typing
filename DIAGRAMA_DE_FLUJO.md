# Diagrama de flujo de Thanks, Typing

Este documento describe el recorrido que implementa actualmente el proyecto: desde que Java inicia hasta que el jugador termina o reinicia el juicio.

## 1. Inicio y carga del juicio

```mermaid
flowchart TD
    A[Java ejecuta Principal.main] --> B[SwingUtilities crea la interfaz en el hilo de Swing]
    B --> C[Principal crea JFrame y PanelTribunal]
    C --> D[PanelTribunal configura tamano, foco y eventos del teclado]
    D --> E[cargarGuion abre assets/dialogs/juicio_demostracion.json]
    E --> F[CargadorGuion lee bytes como UTF-8]
    F --> G[Gson convierte JSON a GuionJuicio]
    G --> H{El guion tiene lineas y cada linea tiene datos validos?}
    H -- No --> I[Se muestra el mensaje de error y la partida queda detenida]
    H -- Si --> J[Validar cada linea con la regla global tildes y convertirla a tramos]
    J --> K{Las marcas y opciones amarillas son validas?}
    K -- No --> I
    K -- Si --> L[PanelTribunal recorre las lineas y calcula los totales]
    L --> M[Iniciar la linea de indice 0]
    M --> N[Crear lienzo 640 x 360, iniciar Timer y esperar teclado]
```

### Que valida el cargador

- Que el JSON se pueda interpretar y que exista al menos una linea.
- Que cada linea tenga `hablante`, `emocion`, `texto_original` y `tiempo_limite` positivo.
- Que cada marca `ROJO`, `VERDE` o `AMARILLO` se pueda convertir en un tramo.
- Que cada marca amarilla tenga alternativas no vacias y distinguibles mientras se escriben.
- Que `opciones_correctas` tenga una respuesta coincidente por cada marca amarilla.

`GuionJuicio` y `GuionJuicio.LineaGuion` son modelos de datos: Gson copia ahi las propiedades del JSON. `LineaDialogo` transforma cada cadena marcada en una secuencia de tramos que la pantalla y el teclado pueden procesar.

## 2. Conversion del texto del JSON a tramos

```mermaid
flowchart TD
    A[LineaGuion.texto_original] --> B[Buscar marcas con el patron ROJO VERDE AMARILLO]
    B --> C[Agregar el fragmento normal anterior a la marca]
    C --> D{Tipo de marca}
    D -- ROJO --> R[Mostrar la palabra roja y preparar una entrada de espacios de igual longitud]
    D -- VERDE --> V[Mostrar la palabra verde y preparar su texto como entrada]
    D -- AMARILLO --> Y[Separar alternativas por / y buscar la respuesta correcta]
    D -- Sin mas marcas --> N[Agregar el resto como texto normal]
    R --> E[Agregar tramo a LineaDialogo.tramos]
    V --> E
    Y --> E
    E --> F{Quedan marcas en el texto?}
    F -- Si --> B
    F -- No --> N
    N --> G[Reiniciar progreso y habilitar todas las opciones amarillas]
    G --> H[LineaDialogo lista para jugar]
```

Un tramo guarda por separado `textoVisible` y `entrada`: por eso una palabra roja se ve como palabra, aunque para avanzar se reciban espacios. Los tramos amarillos conservan la lista de opciones, la correcta, las alternativas que todavia coinciden y el progreso de escritura.

## 3. Bucle de una linea: teclado, tiempo y avance

```mermaid
flowchart TD
    A[Linea activa] --> B{Que evento llega?}
    B -- Tecla escribible --> C[procesarTecla]
    B -- Retroceso --> D{Hay caracteres aceptados para borrar?}
    D -- Si --> E[Quitar el ultimo y reconstruir los tramos desde el historial]
    D -- No --> A
    E --> A
    B -- Timer cada 100 ms --> F[actualizarReloj calcula el tiempo real transcurrido]
    F --> G{Se agoto el limite?}
    G -- No --> A
    G -- Si --> H[Terminar linea y registrar el resultado una sola vez]
    C --> I{La tecla es control o acento separado?}
    I -- Si --> A
    I -- No --> J[aplicarCaracter busca el primer tramo incompleto]
    J --> K{La tecla cumple la regla del tramo?}
    K -- No --> L[Sumar error; no avanzar el cursor]
    L --> A
    K -- Si --> M[Guardar tecla y actualizar progreso]
    M --> N{Todos los tramos obligatorios estan completos?}
    N -- No --> A
    N -- Si --> H
    H --> O{La linea tiene break true?}
    O -- Si --> P[Esperar Enter]
    O -- No --> Q[Avanzar automaticamente o terminar el juicio]
    P --> R{Quedan lineas?}
    Q --> R
    R -- Si --> S[Iniciar siguiente linea y reiniciar reloj]
    S --> A
    R -- No --> T[Calcular porcentaje y mostrar resultado]
    T --> U[Enter reinicia desde la primera linea]
```

El temporizador consulta cada 100 ms, pero descuenta segundos usando `System.currentTimeMillis()`. Al completar la linea o agotarse el tiempo, si el JSON tiene `"break": true` el juego espera Enter. Si la propiedad falta o es `false`, avanza automaticamente. El ultimo dialogo muestra el resultado; Enter queda disponible para reiniciar.

## 4. Reglas que aplica `aplicarCaracter`

```mermaid
flowchart TD
    A[Tomar el siguiente tramo incompleto] --> B{Tipo de tramo}
    B -- NORMAL --> C[Comparar con el caracter esperado ignorando tilde y dieresis]
    B -- OMITIR rojo --> D[Aceptar espacio o caracter visible correspondiente]
    B -- AGREGAR verde --> E[Comparar sin distinguir mayusculas/minusculas]
    B -- ELEGIR amarillo --> F[Filtrar las opciones que coinciden en esta posicion]
    C --> G{Coincide?}
    E --> G
    G -- No --> X[Devolver error; no mover progreso]
    G -- Si --> H[Guardar caracter y avanzar]
    D --> H
    F --> I{Queda alguna alternativa posible?}
    I -- No --> X
    I -- Si --> J[Conservar alternativas compatibles]
    J --> K{Se completo una alternativa?}
    K -- Si --> L[Guardar indice elegido y cerrar tramo amarillo]
    K -- No --> H
    L --> H
    H --> M[Actualizar dibujo y puntaje potencial]
```

El color verde acepta que se escriba una palabra con mezcla de mayusculas y minusculas para poder seguir; al puntuar, cada palabra verde solo vale si se completo totalmente en mayusculas. En rojo cada posicion acepta un espacio o el caracter visible correspondiente, pero solo se cuentan como correctas las palabras cuyas posiciones se rellenaron todas con espacios. En amarillo se puede empezar cualquiera de las alternativas; solo la opcion indicada por `opciones_correctas` suma como correcta.

La propiedad global `tildes` del JSON controla las comparaciones normales, rojas, verdes y amarillas: con `true` se exige el mismo carácter acentuado; con `false` se ignoran tildes y diéresis. Las comparaciones siguen distinguiendo mayúsculas y minúsculas, salvo en verde, donde el avance convierte ambos caracteres a mayúsculas.

## 5. Puntaje y fin del juicio

```mermaid
flowchart TD
    A[Linea termina o vence su tiempo] --> B[registrarLinea evita sumar dos veces]
    B --> C[progresoCorrecto evalua caracteres segun cada tipo]
    C --> D[Sumar caracteres correctos acumulados]
    B --> E[contarPalabrasEspeciales evalua palabras especiales completas]
    E --> F[Actualizar contador de especiales acertadas]
    D --> G{Hay otra linea?}
    G -- Si --> H[Continuar al pulsar Enter]
    G -- No --> I[Calcular caracteresCorrectos * 100 / caracteresTotales]
    I --> J{El resultado es 100 por ciento?}
    J -- Si --> K[exito = true]
    J -- No --> L[exito = false]
    K --> M[Mostrar resultado y permitir repetir con Enter]
    L --> M
```

El resultado actual usa el porcentaje de caracteres correctos y marca `exito` solo si llega al 100 %. El contador `ESPECIALES` se calcula aparte: cuenta palabras rojas rellenadas con espacios, palabras verdes completadas en mayusculas y opciones amarillas correctas.

## 6. Dibujo de cada fotograma

```mermaid
flowchart LR
    A[Swing llama paintComponent] --> B[Crear Graphics2D sobre el lienzo logico]
    B --> C[dibujarSala: fondo, estrado y personajes rectangulares]
    C --> D[dibujarActa: papel, hablante y barra de tiempo]
    D --> E[dibujarTramos: caracteres, colores, progreso y cursor]
    E --> F[Calcular escala uniforme para el panel]
    F --> G[Ampliar el lienzo con vecino mas cercano]
    G --> H[Mostrar la imagen pixelada en la ventana]
```

## Archivos y responsabilidades

| Archivo | Responsabilidad en el flujo |
| --- | --- |
| `Principal.java` | Crea la ventana Swing y coloca el panel del juego. |
| `PanelTribunal.java` | Coordina teclado, temporizador, lineas, puntaje y dibujo. |
| `CargadorGuion.java` | Lee y valida el JSON con Gson. |
| `GuionJuicio.java` | Define los modelos que reciben los datos JSON. |
| `LineaDialogo.java` | Convierte el texto marcado en tramos y administra su progreso. |
| `assets/dialogs/juicio_demostracion.json` | Contiene el guion que carga actualmente el panel. |

## Limites de la implementacion actual

- La escena y las personas se dibujan con rectangulos en `PanelTribunal`; todavia no se cargan sprites PNG.
- Los contadores de errores y sabotajes se calculan internamente, pero no se dibujan en pantalla.
- El campo `emocion` se carga desde el JSON, pero actualmente no cambia la expresion de los personajes.
- La precuela de cuatro imagenes, varios juicios seleccionables y una sentencia narrativa todavia no forman parte del flujo implementado.
- El juicio de demostracion se carga desde una ruta relativa a la carpeta raiz del proyecto.
