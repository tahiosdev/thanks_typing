// Declara el paquete Java donde se encuentran las clases principales del juego.
package thankstyping;

// Gson convierte el contenido JSON en objetos Java.
import com.google.gson.Gson;
// Esta excepci�n representa errores al interpretar o validar el JSON.
import com.google.gson.JsonParseException;

// IOException permite informar errores al leer el archivo del disco.
import java.io.IOException;
// StandardCharsets.UTF_8 fija la codificaci�n para conservar tildes y �.
import java.nio.charset.StandardCharsets;
// Files y Path son utilidades modernas de Java para archivos y rutas.
import java.nio.file.Files;
import java.nio.file.Path;

/** Lee un archivo JSON y comprueba que tenga la estructura m�nima de un juicio v�lido. */
public final class CargadorGuion {
    // Constructor privado: esta clase solo ofrece operaciones est�ticas y no se instancia.
    private CargadorGuion() { }

    /**
     * Carga y valida el guion indicado.
     * @param ruta ubicaci�n del archivo JSON
     * @return guion convertido a objetos Java
     * @throws IOException si el archivo no existe o no puede leerse
     * @throws JsonParseException si el JSON o alguno de sus datos no es v�lido
     */
    public static GuionJuicio cargar(Path ruta) throws IOException {
        // Lee todos los bytes del archivo y los convierte a texto UTF-8.
        String textoJson = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
        // Gson relaciona las claves del JSON con los campos de GuionJuicio.
        GuionJuicio guion = new Gson().fromJson(textoJson, GuionJuicio.class);
        // Un guion sin objeto, sin lista de l�neas o con lista vac�a no se puede jugar.
        if (guion == null || guion.lineas == null || guion.lineas.isEmpty()) {
            throw new JsonParseException("El juicio debe tener al menos una l�nea.");
        }
        // Recorre todas las l�neas para detectar problemas antes de iniciar la partida.
        for (GuionJuicio.LineaGuion linea : guion.lineas) {
            // Estos datos se necesitan para mostrar y procesar el di�logo.
            if (linea.hablante == null || linea.emocion == null || linea.texto_original == null) {
                throw new JsonParseException("Cada l�nea necesita hablante, emoci�n y texto_original.");
            }
            // El reloj debe tener una duraci�n positiva.
            if (linea.tiempo_limite <= 0) {
                throw new JsonParseException("La l�nea " + linea.identificador + " debe tener tiempo_limite mayor que cero.");
            }
            try {
                // Tambi�n valida que las marcas ROJO/VERDE/AMARILLO se puedan interpretar.
                LineaDialogo.crearDesde(linea);
            } catch (IllegalArgumentException excepcion) {
                // Presenta errores de formato del di�logo como errores de lectura del JSON.
                throw new JsonParseException(excepcion.getMessage(), excepcion);
            }
        }
        // Si todas las comprobaciones pasaron, entrega el guion listo para usar.
        return guion;
    }
}
