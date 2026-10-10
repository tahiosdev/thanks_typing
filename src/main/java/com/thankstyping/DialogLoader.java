package com.thankstyping;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DialogLoader {
    private DialogLoader() { }

    public static DialogScript load(Path path) throws IOException {
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        DialogScript script = new Gson().fromJson(json, DialogScript.class);
        if (script == null || script.lineas == null || script.lineas.isEmpty()) {
            throw new JsonParseException("El juicio debe tener al menos una línea.");
        }
        for (DialogScript.ScriptLine line : script.lineas) {
            if (line.hablante == null || line.emocion == null || line.texto_original == null) {
                throw new JsonParseException("Cada línea necesita hablante, emoción y texto_original.");
            }
            if (line.tiempo_limite <= 0) {
                throw new JsonParseException("La línea " + line.id + " debe tener tiempo_limite mayor que cero.");
            }
            try {
                DialogLine.from(line);
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(exception.getMessage(), exception);
            }
        }
        return script;
    }
}
