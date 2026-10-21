package thankstyping;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public final class GuionJuicio {

    public int identificador_juicio;
    public String titulo;
    public boolean tildes;
    public List<LineaGuion> lineas = new ArrayList<LineaGuion>();

    public static final class LineaGuion {

        public int identificador;
        public String hablante;
        public String emocion;
        public String texto_original;
        public int tiempo_limite;
        @SerializedName("break")
        public boolean pausaAlFinalizar;
        public List<String> opciones_correctas;
    }
}
