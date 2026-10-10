package com.thankstyping;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Paths;

public final class CourtroomPanel extends JPanel {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final Color INK = new Color(30, 26, 32);
    private static final Color NORMAL_TEXT = new Color(126, 124, 121);
    private static final Color PAPER = new Color(239, 221, 176);
    private static final Color RED = new Color(196, 70, 68);
    private static final Color DARK_RED = new Color(130, 38, 40);
    private static final Color GREEN = new Color(64, 139, 97);
    private static final Color DARK_GREEN = new Color(30, 88, 56);
    private static final Color YELLOW = new Color(205, 157, 48);
    private static final Color DARK_YELLOW = new Color(142, 95, 20);

    private final BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    private final Timer timer;
    private DialogScript script;
    private DialogLine line;
    private int lineIndex;
    private String expected = "";
    private int cursor;
    private int completedCharacters;
    private int totalCharacters;
    private int completedSpecialWords;
    private int totalSpecialWords;
    private int remainingSeconds;
    private long lastSecond;
    private boolean finished;
    private boolean waitingForNext;
    private boolean lineRecorded;
    private boolean success;
    private String status = "CARGANDO ACTA...";
    private int mistakes;
    private final StringBuilder typedInput = new StringBuilder();

    public CourtroomPanel() {
        setFocusable(true);
        setPreferredSize(new Dimension(WIDTH * 2, HEIGHT * 2));
        setBackground(Color.BLACK);
        loadDialog();
        addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent event) { handleTyped(event.getKeyChar()); }
            @Override public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE && !finished && !waitingForNext && cursor > 0) {
                    typedInput.setLength(typedInput.length() - 1);
                    rebuildProgress();
                    repaint();
                } else if ((finished || waitingForNext) && event.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (finished) restartLine(); else advanceOrFinish();
                }
            }
        });
        timer = new Timer(100, event -> updateClock());
        timer.start();
    }

    private void loadDialog() {
        try {
            script = DialogLoader.load(Paths.get("assets", "dialogs", "juicio_demo.json"));
            totalCharacters = 0;
            totalSpecialWords = 0;
            for (DialogScript.ScriptLine source : script.lineas) {
                DialogLine parsed = DialogLine.from(source);
                totalCharacters += parsed.expectedInput().length();
                totalSpecialWords += countSpecialWords(parsed, false);
            }
            beginLine(0);
        } catch (Exception exception) {
            status = "ERROR AL CARGAR EL JSON: " + exception.getMessage();
            finished = true;
        }
    }

    private void beginLine(int index) {
        lineIndex = index;
        line = DialogLine.from(script.lineas.get(index));
        expected = line.expectedInput();
        typedInput.setLength(0);
        cursor = 0;
        mistakes = 0;
        lineRecorded = false;
        waitingForNext = false;
        finished = false;
        remainingSeconds = line.seconds;
        lastSecond = System.currentTimeMillis();
        status = "TRANSCRIBÍ EL ACTA SIN EQUIVOCARTE";
        repaint();
    }

    private void recordLine() {
        if (!lineRecorded) {
            completedCharacters += correctProgress();
            completedSpecialWords += countSpecialWords(line, true);
            lineRecorded = true;
        }
    }

    private int countSpecialWords(DialogLine dialogLine, boolean onlyCompleted) {
        int count = 0;
        for (DialogLine.Segment segment : dialogLine.segments) {
            if ("OMIT".equals(segment.type)) {
                count += onlyCompleted ? correctlyFilledRedWords(segment) : wordCount(segment.display);
            } else if ("ADD".equals(segment.type)) {
                count += onlyCompleted ? correctlyUppercaseGreenWords(segment) : wordCount(segment.display);
            } else if ("CHOICE".equals(segment.type)) {
                boolean correct = segment.completedChoice && segment.selectedOption >= 0
                        && segment.options.get(segment.selectedOption).equals(segment.correctOption);
                if (!onlyCompleted || correct) count += wordCount(segment.correctOption);
            }
        }
        return count;
    }

    private int wordCount(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private int correctlyFilledRedWords(DialogLine.Segment segment) {
        int count = 0;
        int index = 0;
        while (index < segment.display.length()) {
            while (index < segment.display.length() && Character.isWhitespace(segment.display.charAt(index))) index++;
            int start = index;
            while (index < segment.display.length() && !Character.isWhitespace(segment.display.charAt(index))) index++;
            if (start == index || segment.progress < index) continue;

            boolean filledWithSpaces = true;
            for (int i = start; i < index; i++) {
                if (segment.typed.charAt(i) != ' ') {
                    filledWithSpaces = false;
                    break;
                }
            }
            if (filledWithSpaces) count++;
        }
        return count;
    }

    private int correctlyUppercaseGreenWords(DialogLine.Segment segment) {
        int count = 0;
        int index = 0;
        while (index < segment.display.length()) {
            while (index < segment.display.length() && Character.isWhitespace(segment.display.charAt(index))) index++;
            int start = index;
            while (index < segment.display.length() && !Character.isWhitespace(segment.display.charAt(index))) index++;
            if (start < index && isUppercaseWord(segment, start, index)) count++;
        }
        return count;
    }

    private int correctGreenCharacters(DialogLine.Segment segment) {
        boolean[] correctWordCharacters = new boolean[segment.display.length()];
        int index = 0;
        while (index < segment.display.length()) {
            while (index < segment.display.length() && Character.isWhitespace(segment.display.charAt(index))) index++;
            int start = index;
            while (index < segment.display.length() && !Character.isWhitespace(segment.display.charAt(index))) index++;
            if (start < index && isUppercaseWord(segment, start, index)) {
                for (int i = start; i < index; i++) correctWordCharacters[i] = true;
            }
        }

        int correct = 0;
        for (int i = 0; i < segment.progress; i++) {
            if (correctWordCharacters[i]
                    || (!Character.isLetter(segment.display.charAt(i))
                    && sameLetter(segment.typed.charAt(i), segment.input.charAt(i)))) correct++;
        }
        return correct;
    }

    private boolean isUppercaseWord(DialogLine.Segment segment, int start, int end) {
        if (segment.progress < end) return false;
        boolean containsLetter = false;
        for (int i = start; i < end; i++) {
            if (Character.isLetter(segment.display.charAt(i))) {
                containsLetter = true;
                if (!Character.isUpperCase(segment.typed.charAt(i))) return false;
            }
        }
        return containsLetter;
    }

    private void advanceOrFinish() {
        if (!waitingForNext || script == null) return;
        if (lineIndex + 1 < script.lineas.size()) {
            beginLine(lineIndex + 1);
            return;
        }
        int score = totalCharacters == 0 ? 0 : completedCharacters * 100 / totalCharacters;
        success = score == 100;
        finished = true;
        waitingForNext = false;
        status = "JUICIO TERMINADO · SABOTAJE " + score + "% · ENTER PARA REPETIR";
        repaint();
    }

    private void handleTyped(char character) {
        if (finished || waitingForNext || line == null || Character.isISOControl(character)) return;
        if (isCombiningAccent(character)) return;
        if (applyCharacter(character)) {
            typedInput.append(character);
            cursor++;
            status = "TRANSCRIBÍ EL ACTA SIN EQUIVOCARTE";
            if (isLineComplete()) {
                recordLine();
                waitingForNext = true;
                status = "LÍNEA COMPLETADA · ENTER PARA CONTINUAR";
            }
        } else {
            mistakes++;
            status = "ERROR DE TIPEO · REVISÁ LA PRÓXIMA LETRA";
        }
        repaint();
    }

    private boolean applyCharacter(char typed) {
        for (DialogLine.Segment segment : line.segments) {
            if ("OMIT".equals(segment.type)) {
                if (segment.progress >= segment.input.length()) continue;
                segment.progress++;
                segment.typed.append(typed);
                return true;
            }
            if ("CHOICE".equals(segment.type)) {
                if (segment.completedChoice) continue;
                List<Integer> matching = new ArrayList<Integer>();
                for (Integer optionIndex : segment.viableOptions) {
                    String option = segment.options.get(optionIndex);
                    if (segment.progress < option.length()
                            && sameLetter(typed, option.charAt(segment.progress))) matching.add(optionIndex);
                }
                if (matching.isEmpty()) return false;
                segment.progress++;
                segment.typed.append(typed);
                segment.viableOptions.clear();
                segment.viableOptions.addAll(matching);
                int completedIndex = -1;
                for (Integer optionIndex : matching) {
                    if (segment.options.get(optionIndex).length() == segment.progress) {
                        if (completedIndex >= 0) return false;
                        completedIndex = optionIndex;
                    }
                }
                if (completedIndex >= 0) {
                    segment.selectedOption = completedIndex;
                    segment.completedChoice = true;
                } else if (matching.size() == 1) {
                    segment.selectedOption = matching.get(0);
                }
                return true;
            }
            if (segment.progress >= segment.input.length()) continue;
            char expectedCharacter = segment.input.charAt(segment.progress);
            if ("ADD".equals(segment.type)) {
                if (!sameLetter(Character.toUpperCase(typed), Character.toUpperCase(expectedCharacter))) return false;
            } else if (!sameLetter(typed, expectedCharacter)) {
                return false;
            }
            segment.progress++;
            segment.typed.append(typed);
            return true;
        }
        return false;
    }

    private boolean sameLetter(char typed, char expectedLetter) {
        return withoutAccent(typed).equals(withoutAccent(expectedLetter));
    }

    private boolean isCombiningAccent(char character) {
        int type = Character.getType(character);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || character == '\u00B4' || character == '\u00A8';
    }

    private String withoutAccent(char character) {
        String decomposed = Normalizer.normalize(String.valueOf(character), Normalizer.Form.NFD);
        return decomposed.replace("\u0301", "").replace("\u0308", "");
    }

    private boolean isLineComplete() {
        for (DialogLine.Segment segment : line.segments) {
            if ("OMIT".equals(segment.type)) {
                if (segment.progress < segment.input.length()) return false;
                continue;
            }
            if ("CHOICE".equals(segment.type) && !segment.completedChoice) return false;
            if (!"CHOICE".equals(segment.type) && segment.progress < segment.input.length()) return false;
        }
        return true;
    }

    private int correctProgress() {
        int correct = 0;
        for (DialogLine.Segment segment : line.segments) {
            if ("OMIT".equals(segment.type)) {
                for (int i = 0; i < segment.progress; i++) {
                    if (segment.typed.charAt(i) == ' ') correct++;
                }
                continue;
            }
            if ("ADD".equals(segment.type)) {
                correct += correctGreenCharacters(segment);
            } else if (!"CHOICE".equals(segment.type)) {
                correct += segment.progress;
            } else if (segment.selectedOption >= 0) {
                if (segment.options.get(segment.selectedOption).equals(segment.correctOption)) correct += segment.progress;
            } else if (segment.viableOptions.contains(segment.options.indexOf(segment.correctOption))) {
                correct += segment.progress;
            }
        }
        return correct;
    }

    private void rebuildProgress() {
        line.resetProgress();
        cursor = 0;
        for (int i = 0; i < typedInput.length(); i++) {
            if (applyCharacter(typedInput.charAt(i))) cursor++;
        }
    }

    private void updateClock() {
        if (!finished && !waitingForNext && line != null && System.currentTimeMillis() - lastSecond >= 1000) {
            long elapsed = (System.currentTimeMillis() - lastSecond) / 1000;
            remainingSeconds -= (int) elapsed;
            lastSecond += elapsed * 1000;
            if (remainingSeconds <= 0) {
                remainingSeconds = 0;
                recordLine();
                waitingForNext = true;
                status = "TIEMPO AGOTADO · ENTER PARA CONTINUAR";
            }
            repaint();
        }
    }

    private void restartLine() {
        completedCharacters = 0;
        completedSpecialWords = 0;
        beginLine(0);
        requestFocusInWindow();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawCourtroom(g);
        drawAct(g);
        g.dispose();

        Graphics2D screen = (Graphics2D) graphics.create();
        screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        screen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        int drawWidth = (int) (WIDTH * scale);
        int drawHeight = (int) (HEIGHT * scale);
        int x = (getWidth() - drawWidth) / 2;
        int y = (getHeight() - drawHeight) / 2;
        screen.drawImage(canvas, x, y, drawWidth, drawHeight, null);
        screen.dispose();
    }

    private void drawCourtroom(Graphics2D g) {
        g.setColor(new Color(34, 38, 55)); g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(53, 57, 73)); g.fillRect(0, 25, WIDTH, 158);
        g.setColor(new Color(83, 69, 73)); g.fillRect(0, 180, WIDTH, 48);
        g.setColor(new Color(104, 76, 57)); g.fillRect(0, 226, WIDTH, 20);
        g.setColor(new Color(121, 87, 58)); g.fillRect(0, 244, WIDTH, 12);
        g.setColor(new Color(73, 51, 47)); g.fillRect(0, 256, WIDTH, 104);
        for (int x = 8; x < WIDTH; x += 32) {
            g.setColor(new Color(87, 60, 52)); g.fillRect(x, 259, 20, 83);
            g.setColor(new Color(111, 77, 59)); g.fillRect(x + 3, 262, 2, 77);
        }
        g.setColor(new Color(126, 99, 81)); g.fillRect(271, 42, 98, 108);
        g.setColor(new Color(59, 65, 75)); g.fillRect(281, 52, 78, 88);
        g.setColor(new Color(177, 151, 112)); g.fillRect(314, 58, 12, 72);
        g.setColor(new Color(186, 159, 113)); g.fillRect(290, 81, 60, 5);
        g.setColor(new Color(126, 99, 81)); g.fillRect(246, 147, 148, 13);
        g.setColor(new Color(83, 50, 43)); g.fillRect(255, 160, 130, 24);
        drawPerson(g, 61, 111, new Color(48, 69, 91), new Color(206, 157, 126), new Color(39, 38, 49));
        drawPerson(g, 512, 111, new Color(88, 51, 57), new Color(196, 143, 113), new Color(42, 37, 45));
        g.setColor(new Color(245, 198, 112)); g.fillRect(97, 47, 5, 7); g.fillRect(538, 47, 5, 7);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(new Color(221, 205, 173));
        String title = script == null || script.titulo == null ? "SESIÓN EXTRAORDINARIA" : script.titulo.toUpperCase();
        g.drawString("SALA DEL TRIBUNAL · " + title, 16, 18);
    }

    private void drawPerson(Graphics2D g, int x, int y, Color jacket, Color skin, Color hair) {
        g.setColor(new Color(25, 27, 39)); g.fillRect(x - 16, y + 49, 33, 64);
        g.setColor(jacket); g.fillRect(x - 13, y + 46, 27, 64);
        g.setColor(skin); g.fillRect(x - 10, y + 16, 22, 30);
        g.setColor(hair); g.fillRect(x - 11, y + 10, 24, 11); g.fillRect(x - 13, y + 17, 5, 12);
        g.setColor(new Color(42, 37, 45)); g.fillRect(x - 5, y + 27, 3, 2); g.fillRect(x + 5, y + 27, 3, 2);
        g.setColor(new Color(109, 67, 65)); g.fillRect(x - 3, y + 38, 9, 2);
        g.setColor(new Color(221, 204, 177)); g.fillRect(x - 4, y + 49, 8, 21);
        g.setColor(new Color(110, 69, 70)); g.fillRect(x, y + 49, 2, 29);
        g.setColor(new Color(32, 31, 43)); g.fillRect(x - 19, y + 108, 14, 5); g.fillRect(x + 5, y + 108, 14, 5);
    }

    private void drawAct(Graphics2D g) {
        g.setColor(new Color(25, 24, 31, 90)); g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(41, 31, 35)); g.fillRect(12, 194, 616, 154);
        g.setColor(new Color(199, 165, 111)); g.fillRect(12, 194, 616, 2);
        g.setColor(PAPER); g.fillRect(18, 201, 604, 137);
        g.setColor(new Color(181, 146, 99)); g.fillRect(18, 201, 604, 2);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(new Color(89, 68, 53));
        String speaker = line == null ? "ACTA" : line.speaker;
        g.drawString(speaker + ":", 28, 216);
        drawSegments(g, 28, 238, 568);
        g.setColor(new Color(180, 161, 125)); g.fillRect(28, 306, 566, 1);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(INK); g.drawString("TIEMPO " + remainingSeconds + "s", 28, 326);
        g.setColor(new Color(177, 159, 125)); g.fillRect(96, 319, 150, 8);
        g.setColor(remainingSeconds <= 4 ? RED : GREEN);
        int barWidth = line == null ? 0 : (int) (150 * (remainingSeconds / (double) line.seconds));
        g.fillRect(96, 319, Math.max(0, barWidth), 8);
        g.setColor(INK);
        g.drawString("ERRORES " + mistakes, 270, 326);
        int currentSpecialWords = lineRecorded || line == null ? 0 : countSpecialWords(line, true);
        g.drawString("ESPECIALES " + (completedSpecialWords + currentSpecialWords) + "/" + totalSpecialWords, 360, 326);
        g.setColor(finished ? (success ? GREEN : RED) : new Color(89, 68, 53));
        g.drawString(status, 28, 339);
    }

    private void drawSegments(Graphics2D g, int x, int y, int maxWidth) {
        if (line == null) {
            g.setFont(new Font("Arial", Font.BOLD, 12)); g.setColor(RED);
            g.drawString(status, x, y);
            return;
        }
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        FontMetrics metrics = g.getFontMetrics();
        int drawX = x;
        int drawY = y;
        boolean showCursor = !finished && !waitingForNext;
        boolean cursorPlaced = false;
        for (DialogLine.Segment segment : line.segments) {
            if ("CHOICE".equals(segment.type)) {
                boolean currentChoice = showCursor && !cursorPlaced && !segment.completedChoice;
                for (int optionIndex = 0; optionIndex < segment.options.size(); optionIndex++) {
                    if (optionIndex > 0) {
                        int[] position = drawText(g, " / ", drawX, drawY, x, maxWidth, metrics, YELLOW, -1, YELLOW, -1, false);
                        drawX = position[0]; drawY = position[1];
                    }
                    String option = segment.options.get(optionIndex);
                    boolean active = segment.viableOptions.contains(optionIndex);
                    int typedPrefix = active ? Math.min(segment.progress, option.length()) : 0;
                    int targetIndex = currentChoice && active
                            && segment.progress < option.length() ? segment.progress : -1;
                    if (targetIndex >= 0) cursorPlaced = true;
                    int[] position = drawText(g, option, drawX, drawY, x, maxWidth, metrics,
                            YELLOW, typedPrefix, DARK_YELLOW, targetIndex, true);
                    drawX = position[0]; drawY = position[1];
                }
                continue;
            }

            boolean omitted = "OMIT".equals(segment.type);
            boolean added = "ADD".equals(segment.type);
            Color baseColor = omitted ? RED : added ? GREEN : NORMAL_TEXT;
            Color typedColor = omitted ? DARK_RED : added ? DARK_GREEN : INK;
            for (int i = 0; i < segment.display.length(); i++) {
                String glyph = segment.display.substring(i, i + 1);
                int glyphWidth = metrics.stringWidth(glyph);
                if (drawX + glyphWidth > x + maxWidth) { drawX = x; drawY += 18; }
                boolean typed = i >= segment.inputStart && i < segment.inputStart + segment.progress;
                g.setColor(typed ? typedColor : baseColor);
                if (showCursor && !cursorPlaced && i == segment.inputStart + segment.progress
                        && segment.progress < segment.input.length()) {
                    drawCursor(g, drawX, drawY, glyphWidth, typedColor);
                    cursorPlaced = true;
                }
                g.drawString(glyph, drawX, drawY);
                drawX += glyphWidth;
            }
        }
    }

    private int[] drawText(Graphics2D g, String text, int drawX, int drawY, int x,
                           int maxWidth, FontMetrics metrics, Color base, int typedPrefix, Color typedColor,
                           int cursorIndex, boolean drawMarker) {
        for (int i = 0; i < text.length(); i++) {
            String glyph = text.substring(i, i + 1);
            int glyphWidth = metrics.stringWidth(glyph);
            if (drawX + glyphWidth > x + maxWidth) { drawX = x; drawY += 18; }
            g.setColor(i < typedPrefix ? typedColor : base);
            if (drawMarker && i == cursorIndex) drawCursor(g, drawX, drawY, glyphWidth, typedColor);
            g.drawString(glyph, drawX, drawY);
            drawX += glyphWidth;
        }
        return new int[] { drawX, drawY };
    }

    private void drawCursor(Graphics2D g, int x, int baseline, int glyphWidth, Color color) {
        Color previousColor = g.getColor();
        g.setColor(color);
        int dotX = x + Math.max(1, glyphWidth / 2);
        g.fillOval(dotX - 1, baseline + 2, 3, 3);
        g.setColor(previousColor);
    }
}
