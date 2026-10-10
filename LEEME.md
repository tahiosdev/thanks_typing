# Thanks, Typing!

Prototipo de mecanografía en Java Swing. El juego lee los juicios desde `assets/dialogs/juicio_demostracion.json`.

## Ejecutar

Desde la carpeta raíz del proyecto, con JDK 17 y Maven instalados:

```powershell
mvn compile exec:java
```

## Estructura

- `src/main/java/thankstyping/Principal.java`: inicia la ventana.
- `src/main/java/thankstyping/PanelTribunal.java`: dibuja la sala y administra teclado, tiempo y puntaje.
- `src/main/java/thankstyping/CargadorGuion.java`: abre y valida los archivos JSON.
- `src/main/java/thankstyping/GuionJuicio.java`: representa los datos leídos del JSON.
- `src/main/java/thankstyping/LineaDialogo.java`: convierte las marcas de color en partes que el juego puede procesar.
- `assets/dialogs/juicio_demostracion.json`: diálogo de ejemplo.
- `EXPLICACION_DEL_CODIGO.md`: explicación detallada del código y de la lógica.

## Marcas del diálogo

- `[ROJO:palabra]`: se muestra en rojo. En cada posición se puede escribir un espacio o el carácter visible correspondiente; solo las posiciones rellenadas con espacios cuentan como sabotaje correcto.
- `[VERDE:palabra]`: se muestra tal como aparece en el JSON. Se puede escribir en cualquier combinación de mayúsculas y minúsculas; cada palabra solo suma como correcta si se escribió completamente en mayúsculas.
- `[AMARILLO:opción 1/opción 2]`: permite escribir cualquiera de las alternativas. `opciones_correctas` indica cuál es la válida para el puntaje.

Se pueden combinar varias marcas en una misma frase. Cada línea tiene `hablante`, `emocion`, `texto_original`, `tiempo_limite` y un `identificador`. Cada juicio tiene `identificador_juicio`, `titulo` y `lineas`.

Las líneas avanzan automáticamente al completarse o agotarse el tiempo. Agregá `"break": true` en una línea si querés que se detenga y espere Enter antes de continuar; si se omite, se considera `false`.

Las tildes se muestran, pero no son obligatorias al tipear. La `ñ` sí se distingue de la `n`.
