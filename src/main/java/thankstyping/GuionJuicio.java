// Paquete que agrupa el c�digo del juego.
package thankstyping;

// ArrayList es la implementaci�n concreta de la lista que inicializamos por defecto.
import java.util.ArrayList;
// List permite guardar una cantidad variable de l�neas del juicio.
import java.util.List;
import com.google.gson.annotations.SerializedName;

/** Modelo de datos que representa el guion completo cargado desde un JSON. */
public final class GuionJuicio {
    // Se corresponde con la propiedad "identificador_juicio" del JSON.
    public int identificador_juicio;
    // T�tulo que se muestra en la parte superior de la sala.
    public String titulo;
    // Di�logos del juicio en el mismo orden en que se jugar�n.
    public List<LineaGuion> lineas = new ArrayList<LineaGuion>();

    /** Modelo de datos de una l�nea individual del JSON. */
    public static final class LineaGuion {
        // N�mero identificador de la l�nea dentro del guion.
        public int identificador;
        // Nombre de quien est� hablando.
        public String hablante;
        // Estado de �nimo asociado a quien habla.
        public String emocion;
        // Texto visible; puede incluir marcas [ROJO:...], [VERDE:...] o [AMARILLO:...].
        public String texto_original;
        // Tiempo disponible para completar esta l�nea, en segundos.
        public int tiempo_limite;
        // JSON property "break" maps here because break is reserved in Java.
        @SerializedName("break")
        public boolean pausaAlFinalizar;
        // Respuesta correcta para cada marca AMARILLO, seg�n el orden en el texto.
        public List<String> opciones_correctas;
    }
}
