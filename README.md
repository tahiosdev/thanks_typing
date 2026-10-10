# Thanks, Typing!

Prototipo inicial de juego de mecanografía en Java Swing, con escena dibujada a resolución lógica de 640 × 360 y escalado por vecino más cercano para conservar el aspecto pixel art.

## Requisitos

- JDK 8 o superior (incluye `javac`).
- Maven 3.6 o superior.
- La primera ejecución necesita conexión para descargar Gson.

## Ejecutar desde la raíz del proyecto

```powershell
mvn compile exec:java
```

El prototipo carga todas las líneas de `assets/dialogs/juicio_demo.json` en orden. Se tipea directamente con el teclado. Backspace borra una letra; al completar la línea o agotarse el tiempo, Enter continúa con la siguiente. Al final se muestra el porcentaje del texto correcto que se alcanzó a tipear; Enter reinicia el juicio.

## Formato del diálogo

- `juicio_id`, `titulo` y `lineas` describen cada juicio.
- Cada línea usa `hablante`, `emocion`, `texto_original` y `tiempo_limite`.
- `[ROJO:palabra]` se muestra tachado y se omite al tipear.
- `[VERDE:palabra]` se muestra y se incluye al tipear.
- `[AMARILLO:opción 1/opción 2]` se muestra como alternativas y acepta cualquiera de ellas. La opción elegida se resalta; las opciones correctas se indican en `opciones_correctas`, en el mismo orden que aparecen las marcas amarillas de esa línea.
- Se pueden combinar y repetir marcas rojas, verdes y amarillas en una misma línea.
- El texto conserva tildes en pantalla, pero al tipear se aceptan las vocales con o sin tilde. La `ñ` sí se distingue de la `n`.
- No hace falta un campo `tipo_sabotaje`; cada marca determina su regla.

El texto fuera de las marcas se escribe tal cual. Al omitir una palabra, el juego evita dejar un espacio duplicado en la transcripción.
