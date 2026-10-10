# Thanks, Typing!

Prototipo de mecanografía en Java Swing. El juego lee los juicios desde `assets/dialogs/juicio_demostracion.json`.

## Ejecutar

Desde la carpeta raíz del proyecto, con JDK 17 y Maven instalados:

```powershell
mvn compile exec:java
```

## Estructura

- `src/main/java/com/thankstyping/Principal.java`: inicia la ventana.
- `src/main/java/com/thankstyping/PanelTribunal.java`: dibuja la sala y administra teclado, tiempo y puntaje.
- `src/main/java/com/thankstyping/CargadorGuion.java`: abre y valida los archivos JSON.
- `src/main/java/com/thankstyping/GuionJuicio.java`: representa los datos leídos del JSON.
- `src/main/java/com/thankstyping/LineaDialogo.java`: convierte las marcas de color en partes que el juego puede procesar.
- `assets/dialogs/juicio_demostracion.json`: diálogo de ejemplo.
- `EXPLICACION_DEL_CODIGO.md`: explicación detallada del código y de la lógica.

## Marcas del diálogo

- `[ROJO:palabra]`: se muestra en rojo y hay que pulsar una tecla por cada letra. Cada tecla hace avanzar el cursor, pero la palabra solo suma como correcta si se llenó usando espacios.
- `[VERDE:palabra]`: se muestra tal como aparece en el JSON. Se puede escribir en cualquier combinación de mayúsculas y minúsculas; cada palabra solo suma como correcta si se escribió completamente en mayúsculas.
- `[AMARILLO:opción 1/opción 2]`: permite escribir cualquiera de las alternativas. `opciones_correctas` indica cuál es la válida para el puntaje.

Se pueden combinar varias marcas en una misma frase. Cada línea tiene `hablante`, `emocion`, `texto_original`, `tiempo_limite` y un `identificador`. Cada juicio tiene `identificador_juicio`, `titulo` y `lineas`.

Las tildes se muestran, pero no son obligatorias al tipear. La `ñ` sí se distingue de la `n`.
