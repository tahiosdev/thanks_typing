package com.thankstyping;

import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogLine {
    private static final Pattern MARKER = Pattern.compile("\\[(ROJO|VERDE|AMARILLO):([^\\]]+)\\]");

    public int id;
    public String speaker;
    public String mood;
    public int seconds;
    public List<Segment> segments = new ArrayList<Segment>();

    public String expectedInput() {
        StringBuilder result = new StringBuilder();
        for (Segment segment : segments) result.append(segment.input);
        return result.toString();
    }

    public void resetProgress() {
        for (Segment segment : segments) {
            segment.progress = 0;
            segment.selectedOption = -1;
            segment.completedChoice = false;
            segment.viableOptions.clear();
            if (segment.options != null) {
                for (int i = 0; i < segment.options.size(); i++) segment.viableOptions.add(i);
            }
            segment.typed.setLength(0);
        }
    }

    public static DialogLine from(DialogScript.ScriptLine source) {
        DialogLine line = new DialogLine();
        line.id = source.id;
        line.speaker = source.hablante;
        line.mood = source.emocion;
        line.seconds = source.tiempo_limite;

        Matcher matcher = MARKER.matcher(source.texto_original);
        int previousEnd = 0;
        int choiceIndex = 0;
        boolean removeNextLeadingSpace = false;
        while (matcher.find()) {
            removeNextLeadingSpace = addNormal(line,
                    source.texto_original.substring(previousEnd, matcher.start()), removeNextLeadingSpace);
            String kind = matcher.group(1);
            String value = matcher.group(2);
            Segment segment = new Segment();
            segment.display = value;

            if ("ROJO".equals(kind)) {
                segment.type = "OMIT";
                char[] spaces = new char[value.length()];
                java.util.Arrays.fill(spaces, ' ');
                segment.input = new String(spaces);
                removeNextLeadingSpace = false;
            } else if ("VERDE".equals(kind)) {
                segment.type = "ADD";
                segment.input = value;
                removeNextLeadingSpace = false;
            } else {
                segment.type = "CHOICE";
                segment.options = new ArrayList<String>();
                for (String option : value.split("/")) segment.options.add(option.trim());
                if (segment.options.size() < 2) {
                    throw new IllegalArgumentException("La línea " + source.id + " necesita al menos dos opciones amarillas.");
                }
                if (segment.options.contains("")) {
                    throw new IllegalArgumentException("Las opciones amarillas de la línea " + source.id + " no pueden estar vacías.");
                }
                for (int i = 0; i < segment.options.size(); i++) {
                    for (int j = i + 1; j < segment.options.size(); j++) {
                        String first = keyboardForm(segment.options.get(i));
                        String second = keyboardForm(segment.options.get(j));
                        if (first.startsWith(second) || second.startsWith(first)) {
                            throw new IllegalArgumentException("Las alternativas amarillas de la línea " + source.id
                                    + " deben poder distinguirse mientras se tipean.");
                        }
                    }
                }
                String correct = correctOption(source, choiceIndex);
                if (correct == null || !segment.options.contains(correct.trim())) {
                    throw new IllegalArgumentException("La opción correcta amarilla de la línea " + source.id
                            + " debe aparecer en opciones_correctas y coincidir con una alternativa.");
                }
                segment.correctOption = correct.trim();
                segment.input = segment.correctOption;
                segment.display = String.join(" / ", segment.options);
                choiceIndex++;
                removeNextLeadingSpace = false;
            }
            line.segments.add(segment);
            previousEnd = matcher.end();
        }
        addNormal(line, source.texto_original.substring(previousEnd), removeNextLeadingSpace);
        if (line.segments.isEmpty()) addNormal(line, source.texto_original, false);
        if (source.opciones_correctas != null && source.opciones_correctas.size() != choiceIndex) {
            throw new IllegalArgumentException("opciones_correctas debe tener una entrada por cada marca AMARILLO en la línea "
                    + source.id + ".");
        }
        line.resetProgress();
        return line;
    }

    private static String correctOption(DialogScript.ScriptLine source, int index) {
        if (source.opciones_correctas != null && index < source.opciones_correctas.size()) {
            return source.opciones_correctas.get(index);
        }
        return null;
    }

    private static String keyboardForm(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replace("\u0301", "").replace("\u0308", "");
    }

    private static boolean addNormal(DialogLine line, String text, boolean removeLeadingSpace) {
        if (text.isEmpty()) return removeLeadingSpace;
        Segment segment = new Segment();
        segment.type = "NORMAL";
        segment.display = text;
        segment.input = text;
        if (removeLeadingSpace && Character.isWhitespace(text.charAt(0))) {
            segment.input = text.substring(1);
            segment.inputStart = 1;
        }
        line.segments.add(segment);
        return false;
    }

    public static final class Segment {
        public String type;
        public String display;
        public String input;
        public int inputStart;
        public List<String> options;
        public String correctOption;
        public int progress;
        public int selectedOption = -1;
        public boolean completedChoice;
        public final List<Integer> viableOptions = new ArrayList<Integer>();
        public final StringBuilder typed = new StringBuilder();
    }
}
