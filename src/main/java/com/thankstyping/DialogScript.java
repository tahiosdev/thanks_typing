package com.thankstyping;

import java.util.ArrayList;
import java.util.List;

public final class DialogScript {
    public int juicio_id;
    public String titulo;
    public List<ScriptLine> lineas = new ArrayList<ScriptLine>();

    public static final class ScriptLine {
        public int id;
        public String hablante;
        public String emocion;
        public String texto_original;
        public int tiempo_limite;
        public List<String> opciones_correctas;
    }
}
