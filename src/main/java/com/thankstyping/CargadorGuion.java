package com.thankstyping;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CargadorGuion {
    private CargadorGuion() { }

    public static GuionJuicio cargar(Path ruta) throws IOException {
        String textoJson = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
        GuionJuicio guion = new Gson().fromJson(textoJson, GuionJuicio.class);
        if (guion == null || guion.lineas == null || guion.lineas.isEmpty()) {
            throw new JsonParseException("El juicio debe tener al menos una línea.");
        }
        for (GuionJuicio.LineaGuion linea : guion.lineas) {
            if (linea.hablante == null || linea.emocion == null || linea.texto_original == null) {
                throw new JsonParseException("Cada línea necesita hablante, emoción y texto_original.");
            }
            if (linea.tiempo_limite <= 0) {
                throw new JsonParseException("La línea " + linea.identificador + " debe tener tiempo_limite mayor que cero.");
            }
            try {
                LineaDialogo.crearDesde(linea);
            } catch (IllegalArgumentException excepcion) {
                throw new JsonParseException(excepcion.getMessage(), excepcion);
            }
        }
        return guion;
    }
}
