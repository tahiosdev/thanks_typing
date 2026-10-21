package thankstyping;

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

public final class PanelTribunal extends JPanel {

    private static final int ANCHO = 640;
    private static final int ALTO = 360;
    private static final Color TINTA = new Color(30, 26, 32);
    private static final Color TEXTO_NORMAL = new Color(126, 124, 121);
    private static final Color PAPEL = new Color(239, 221, 176);
    private static final Color ROJO = new Color(196, 70, 68);
    private static final Color ROJO_OSCURO = new Color(130, 38, 40);
    private static final Color VERDE = new Color(64, 139, 97);
    private static final Color VERDE_OSCURO = new Color(30, 88, 56);
    private static final Color AMARILLO = new Color(205, 157, 48);
    private static final Color AMARILLO_OSCURO = new Color(142, 95, 20);
    private final BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
    private final Timer temporizador;
    private GuionJuicio guion;
    private LineaDialogo linea;
    private int indiceLinea;
    private String entradaEsperada = "";
    private int cursorEntrada;
    private int caracteresCorrectos;
    private int caracteresTotales;
    private int palabrasEspecialesCorrectas;
    private int palabrasEspecialesTotales;
    private int segundosRestantes;
    private long ultimoSegundo;
    private boolean terminado;
    private boolean esperandoSiguiente;
    private boolean lineaRegistrada;
    private boolean exito;
    private String estado = "CARGANDO ACTA...";
    private int errores;
    private final StringBuilder entradaEscrita = new StringBuilder();

    public PanelTribunal() {
        setFocusable(true);
        setPreferredSize(new Dimension(ANCHO * 2, ALTO * 2));
        setBackground(Color.BLACK);
        cargarGuion();
        addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent evento) { procesarTecla(evento.getKeyChar()); }
            @Override public void keyPressed(KeyEvent evento) {
                if (evento.getKeyCode() == KeyEvent.VK_BACK_SPACE && !terminado && !esperandoSiguiente && cursorEntrada > 0) {
                    entradaEscrita.setLength(entradaEscrita.length() - 1);
                    reconstruirProgreso();
                    repaint();
                } else if ((terminado || esperandoSiguiente) && evento.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (terminado) reiniciarLinea(); else avanzarOFinalizar();
                }
            }
        });
        temporizador = new Timer(100, evento -> actualizarReloj());
        temporizador.start();
    }

    private void cargarGuion() {
        try {
            guion = CargadorGuion.cargar(Paths.get("assets", "dialogs", "juicio_demostracion.json"));
            caracteresTotales = 0;
            palabrasEspecialesTotales = 0;
            for (GuionJuicio.LineaGuion origen : guion.lineas) {
                LineaDialogo lineaInterpretada = LineaDialogo.crearDesde(origen, guion.tildes);
                caracteresTotales += lineaInterpretada.entradaEsperada().length();
                palabrasEspecialesTotales += contarPalabrasEspeciales(lineaInterpretada, false);
            }
            iniciarLinea(0);
        } catch (Exception exception) {
            estado = "ERROR AL CARGAR EL JSON: " + exception.getMessage();
            terminado = true;
        }
    }

    private void iniciarLinea(int indice) {
        indiceLinea = indice;
        linea = LineaDialogo.crearDesde(guion.lineas.get(indice), guion.tildes);
        entradaEsperada = linea.entradaEsperada();
        entradaEscrita.setLength(0);
        cursorEntrada = 0;
        lineaRegistrada = false;
        esperandoSiguiente = false;
        terminado = false;
        segundosRestantes = linea.segundos;
        ultimoSegundo = System.currentTimeMillis();
        estado = "TRANSCRIBÍ EL ACTA SIN EQUIVOCARTE";
        repaint();
    }

    private void registrarLinea() {
        if (!lineaRegistrada) {
            caracteresCorrectos += progresoCorrecto();
            palabrasEspecialesCorrectas += contarPalabrasEspeciales(linea, true);
            lineaRegistrada = true;
        }
    }

    private int contarPalabrasEspeciales(LineaDialogo lineaDialogo, boolean soloCompletadas) {
        int cantidad = 0;
        for (LineaDialogo.Tramo tramo : lineaDialogo.tramos) {
            if ("OMITIR".equals(tramo.tipo)) {
                cantidad += soloCompletadas ? contarRojasCorrectas(tramo) : contarPalabras(tramo.textoVisible);
            } else if ("AGREGAR".equals(tramo.tipo)) {
                cantidad += soloCompletadas ? contarVerdesMayusculas(tramo) : contarPalabras(tramo.textoVisible);
            } else if ("ELEGIR".equals(tramo.tipo)) {
                boolean correcta = tramo.eleccionCompletada && tramo.opcionElegida >= 0
                        && tramo.opciones.get(tramo.opcionElegida).equals(tramo.opcionCorrecta);
                if (!soloCompletadas || correcta) cantidad += contarPalabras(tramo.opcionCorrecta);
            }
        }
        return cantidad;
    }

    private int contarPalabras(String texto) {
        String recortado = texto == null ? "" : texto.trim();
        return recortado.isEmpty() ? 0 : recortado.split("\\s+").length;
    }

    private int contarRojasCorrectas(LineaDialogo.Tramo tramo) {
        int cantidad = 0;
        int indice = 0;
        while (indice < tramo.textoVisible.length()) {
            while (indice < tramo.textoVisible.length() && Character.isWhitespace(tramo.textoVisible.charAt(indice))) indice++;
            int inicio = indice;
            while (indice < tramo.textoVisible.length() && !Character.isWhitespace(tramo.textoVisible.charAt(indice))) indice++;
            if (inicio == indice || tramo.progreso < indice) continue;
            boolean completadaConEspacios = true;
            for (int posicion = inicio; posicion < indice; posicion++) {
                if (tramo.tecleado.charAt(posicion) != ' ') {
                    completadaConEspacios = false;
                    break;
                }
            }
            if (completadaConEspacios) cantidad++;
        }
        return cantidad;
    }

    private int contarVerdesMayusculas(LineaDialogo.Tramo tramo) {
        int cantidad = 0;
        int indice = 0;
        while (indice < tramo.textoVisible.length()) {
            while (indice < tramo.textoVisible.length() && Character.isWhitespace(tramo.textoVisible.charAt(indice))) indice++;
            int inicio = indice;
            while (indice < tramo.textoVisible.length() && !Character.isWhitespace(tramo.textoVisible.charAt(indice))) indice++;
            if (inicio < indice && estaPalabraEnMayusculas(tramo, inicio, indice)) cantidad++;
        }
        return cantidad;
    }

    private int contarCaracteresVerdesCorrectos(LineaDialogo.Tramo tramo) {
        boolean[] caracteresDePalabraCorrecta = new boolean[tramo.textoVisible.length()];
        int indice = 0;
        while (indice < tramo.textoVisible.length()) {
            while (indice < tramo.textoVisible.length() && Character.isWhitespace(tramo.textoVisible.charAt(indice))) indice++;
            int inicio = indice;
            while (indice < tramo.textoVisible.length() && !Character.isWhitespace(tramo.textoVisible.charAt(indice))) indice++;
            if (inicio < indice && estaPalabraEnMayusculas(tramo, inicio, indice)) {
                for (int posicion = inicio; posicion < indice; posicion++) caracteresDePalabraCorrecta[posicion] = true;
            }
        }
        int correcta = 0;
        for (int posicion = 0; posicion < tramo.progreso; posicion++) {
            if (caracteresDePalabraCorrecta[posicion]
                    || (!Character.isLetter(tramo.textoVisible.charAt(posicion))
                    && mismaLetra(tramo.tecleado.charAt(posicion), tramo.entrada.charAt(posicion)))) correcta++;
        }
        return correcta;
    }

    private boolean estaPalabraEnMayusculas(LineaDialogo.Tramo tramo, int inicio, int fin) {
        if (tramo.progreso < fin) return false;
        boolean contieneLetra = false;
        for (int indice = inicio; indice < fin; indice++) {
            if (Character.isLetter(tramo.textoVisible.charAt(indice))) {
                contieneLetra = true;
                if (!Character.isUpperCase(tramo.tecleado.charAt(indice))) return false;
            }
        }
        return contieneLetra;
    }

    private void avanzarOFinalizar() {
        if (!esperandoSiguiente || guion == null) return;
        if (indiceLinea + 1 < guion.lineas.size()) {
            iniciarLinea(indiceLinea + 1);
            return;
        }
        int puntaje = caracteresTotales == 0 ? 0 : caracteresCorrectos * 100 / caracteresTotales;
        exito = puntaje == 100;
        terminado = true;
        esperandoSiguiente = false;
        estado = "JUICIO TERMINADO · SABOTAJE " + puntaje + "% · ENTER PARA REPETIR";
        repaint();
    }

    private void terminarLinea(boolean tiempoAgotado) {
        registrarLinea();
        esperandoSiguiente = true;
        if (linea.pausaAlFinalizar) {
            estado = tiempoAgotado
                    ? "TIEMPO AGOTADO · ENTER PARA CONTINUAR"
                    : "LÍNEA COMPLETADA · ENTER PARA CONTINUAR";
        } else {
            avanzarOFinalizar();
        }
        repaint();
    }

    private void procesarTecla(char caracter) {
        if (terminado || esperandoSiguiente || linea == null || Character.isISOControl(caracter)) return;
        if (esAcentoCombinado(caracter)) return;
        if (aplicarCaracter(caracter)) {
            entradaEscrita.append(caracter);
            cursorEntrada++;
            estado = "TRANSCRIBÍ EL ACTA SIN EQUIVOCARTE";
            if (lineaCompleta()) {
                terminarLinea(false);
            }
        } else {
            errores++;
            estado = "ERROR DE TIPEO \u00B7 REVIS\u00C1 LA PR\u00D3XIMA LETRA";
        }
        repaint();
    }

    private boolean aplicarCaracter(char tecleado) {
        for (LineaDialogo.Tramo tramo : linea.tramos) {
            if ("OMITIR".equals(tramo.tipo)) {
                if (tramo.progreso >= tramo.entrada.length()) continue;
                char caracterVisible = tramo.textoVisible.charAt(tramo.progreso);
                if (tecleado != ' ' && !mismaLetra(tecleado, caracterVisible)) return false;
                tramo.progreso++;
                tramo.tecleado.append(tecleado);
                return true;
            }
            if ("ELEGIR".equals(tramo.tipo)) {
                if (tramo.eleccionCompletada) continue;
                List<Integer> coincidentes = new ArrayList<Integer>();
                for (Integer indiceOpcion : tramo.opcionesPosibles) {
                    String opcion = tramo.opciones.get(indiceOpcion);
                    if (tramo.progreso < opcion.length()
                            && mismaLetra(tecleado, opcion.charAt(tramo.progreso))) coincidentes.add(indiceOpcion);
                }
                if (coincidentes.isEmpty()) return false;
                tramo.progreso++;
                tramo.tecleado.append(tecleado);
                tramo.opcionesPosibles.clear();
                tramo.opcionesPosibles.addAll(coincidentes);
                int indiceCompletado = -1;
                for (Integer indiceOpcion : coincidentes) {
                    if (tramo.opciones.get(indiceOpcion).length() == tramo.progreso) {
                        if (indiceCompletado >= 0) return false;
                        indiceCompletado = indiceOpcion;
                    }
                }
                if (indiceCompletado >= 0) {
                    tramo.opcionElegida = indiceCompletado;
                    tramo.eleccionCompletada = true;
                } else if (coincidentes.size() == 1) {
                    tramo.opcionElegida = coincidentes.get(0);
                }
                return true;
            }
            if (tramo.progreso >= tramo.entrada.length()) continue;
            char caracterEsperado = tramo.entrada.charAt(tramo.progreso);
            if ("AGREGAR".equals(tramo.tipo)) {
                if (!mismaLetra(Character.toUpperCase(tecleado), Character.toUpperCase(caracterEsperado))) return false;
            } else if (!mismaLetra(tecleado, caracterEsperado)) {
                return false;
            }
            tramo.progreso++;
            tramo.tecleado.append(tecleado);
            return true;
        }
        return false;
    }

    private boolean mismaLetra(char tecleado, char letraEsperada) {
        if (guion != null && guion.tildes) return tecleado == letraEsperada;
        return sinAcento(tecleado).equals(sinAcento(letraEsperada));
    }

    private boolean esAcentoCombinado(char caracter) {
        if (guion != null && guion.tildes) return false;
        int tipo = Character.getType(caracter);
        return tipo == Character.NON_SPACING_MARK || tipo == Character.COMBINING_SPACING_MARK
                || caracter == '\u00B4' || caracter == '\u00A8';
    }

    private String sinAcento(char caracter) {
        String descompuesto = Normalizer.normalize(String.valueOf(caracter), Normalizer.Form.NFD);
        return descompuesto.replace("\u0301", "").replace("\u0308", "");
    }

    private boolean lineaCompleta() {
        for (LineaDialogo.Tramo tramo : linea.tramos) {
            if ("OMITIR".equals(tramo.tipo)) {
                if (tramo.progreso < tramo.entrada.length()) return false;
                continue;
            }
            if ("ELEGIR".equals(tramo.tipo) && !tramo.eleccionCompletada) return false;
            if (!"ELEGIR".equals(tramo.tipo) && tramo.progreso < tramo.entrada.length()) return false;
        }
        return true;
    }

    private int progresoCorrecto() {
        int correcta = 0;
        for (LineaDialogo.Tramo tramo : linea.tramos) {
            if ("OMITIR".equals(tramo.tipo)) {
                for (int indice = 0; indice < tramo.progreso; indice++) {
                    if (tramo.tecleado.charAt(indice) == ' ') correcta++;
                }
                continue;
            }
            if ("AGREGAR".equals(tramo.tipo)) {
                correcta += contarCaracteresVerdesCorrectos(tramo);
            } else if (!"ELEGIR".equals(tramo.tipo)) {
                correcta += tramo.progreso;
            } else if (tramo.opcionElegida >= 0) {
                if (tramo.opciones.get(tramo.opcionElegida).equals(tramo.opcionCorrecta)) correcta += tramo.progreso;
            } else if (tramo.opcionesPosibles.contains(tramo.opciones.indexOf(tramo.opcionCorrecta))) {
                correcta += tramo.progreso;
            }
        }
        return correcta;
    }

    private void reconstruirProgreso() {
        linea.reiniciarProgreso();
        cursorEntrada = 0;
        for (int indice = 0; indice < entradaEscrita.length(); indice++) {
            if (aplicarCaracter(entradaEscrita.charAt(indice))) cursorEntrada++;
        }
    }

    private void actualizarReloj() {
        if (!terminado && !esperandoSiguiente && linea != null && System.currentTimeMillis() - ultimoSegundo >= 1000) {
            long tiempoTranscurrido = (System.currentTimeMillis() - ultimoSegundo) / 1000;
            segundosRestantes -= (int) tiempoTranscurrido;
            ultimoSegundo += tiempoTranscurrido * 1000;
            if (segundosRestantes <= 0) {
                segundosRestantes = 0;
                terminarLinea(true);
            }
            repaint();
        }
    }

    private void reiniciarLinea() {
        caracteresCorrectos = 0;
        palabrasEspecialesCorrectas = 0;
        errores = 0;
        iniciarLinea(0);
        requestFocusInWindow();
    }

    @Override protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        Graphics2D grafica = lienzo.createGraphics();
        grafica.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        grafica.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        dibujarSala(grafica);
        dibujarActa(grafica);
        grafica.dispose();
        Graphics2D pantalla = (Graphics2D) graficos.create();
        pantalla.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        pantalla.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        double escala = Math.min(getWidth() / (double) ANCHO, getHeight() / (double) ALTO);
        int anchoDibujo = (int) (ANCHO * escala);
        int altoDibujo = (int) (ALTO * escala);
        int posicionX = (getWidth() - anchoDibujo) / 2;
        int posicionY = (getHeight() - altoDibujo) / 2;
        pantalla.drawImage(lienzo, posicionX, posicionY, anchoDibujo, altoDibujo, null);
        pantalla.dispose();
    }

    private void dibujarSala(Graphics2D grafica) {
        grafica.setColor(new Color(34, 38, 55)); grafica.fillRect(0, 0, ANCHO, ALTO);
        grafica.setColor(new Color(53, 57, 73)); grafica.fillRect(0, 25, ANCHO, 158);
        grafica.setColor(new Color(83, 69, 73)); grafica.fillRect(0, 180, ANCHO, 48);
        grafica.setColor(new Color(104, 76, 57)); grafica.fillRect(0, 226, ANCHO, 20);
        grafica.setColor(new Color(121, 87, 58)); grafica.fillRect(0, 244, ANCHO, 12);
        grafica.setColor(new Color(73, 51, 47)); grafica.fillRect(0, 256, ANCHO, 104);
        for (int posicionX = 8; posicionX < ANCHO; posicionX += 32) {
            grafica.setColor(new Color(87, 60, 52)); grafica.fillRect(posicionX, 259, 20, 83);
            grafica.setColor(new Color(111, 77, 59)); grafica.fillRect(posicionX + 3, 262, 2, 77);
        }
        grafica.setColor(new Color(126, 99, 81)); grafica.fillRect(271, 42, 98, 108);
        grafica.setColor(new Color(59, 65, 75)); grafica.fillRect(281, 52, 78, 88);
        grafica.setColor(new Color(177, 151, 112)); grafica.fillRect(314, 58, 12, 72);
        grafica.setColor(new Color(186, 159, 113)); grafica.fillRect(290, 81, 60, 5);
        grafica.setColor(new Color(126, 99, 81)); grafica.fillRect(246, 147, 148, 13);
        grafica.setColor(new Color(83, 50, 43)); grafica.fillRect(255, 160, 130, 24);
        dibujarPersona(grafica, 61, 111, new Color(48, 69, 91), new Color(206, 157, 126), new Color(39, 38, 49));
        dibujarPersona(grafica, 512, 111, new Color(88, 51, 57), new Color(196, 143, 113), new Color(42, 37, 45));
        grafica.setColor(new Color(245, 198, 112)); grafica.fillRect(97, 47, 5, 7); grafica.fillRect(538, 47, 5, 7);
        grafica.setFont(new Font("Arial", Font.BOLD, 10));
        grafica.setColor(new Color(221, 205, 173));
        String titulo = guion == null || guion.titulo == null ? "SESI\u00D3N EXTRAORDINARIA" : guion.titulo.toUpperCase();
        grafica.drawString("SALA DEL TRIBUNAL · " + titulo, 16, 18);
    }

    private void dibujarPersona(Graphics2D grafica, int posicionX, int posicionY, Color chaqueta, Color piel, Color cabello) {
        grafica.setColor(new Color(25, 27, 39)); grafica.fillRect(posicionX - 16, posicionY + 49, 33, 64);
        grafica.setColor(chaqueta); grafica.fillRect(posicionX - 13, posicionY + 46, 27, 64);
        grafica.setColor(piel); grafica.fillRect(posicionX - 10, posicionY + 16, 22, 30);
        grafica.setColor(cabello); grafica.fillRect(posicionX - 11, posicionY + 10, 24, 11); grafica.fillRect(posicionX - 13, posicionY + 17, 5, 12);
        grafica.setColor(new Color(42, 37, 45)); grafica.fillRect(posicionX - 5, posicionY + 27, 3, 2); grafica.fillRect(posicionX + 5, posicionY + 27, 3, 2);
        grafica.setColor(new Color(109, 67, 65)); grafica.fillRect(posicionX - 3, posicionY + 38, 9, 2);
        grafica.setColor(new Color(221, 204, 177)); grafica.fillRect(posicionX - 4, posicionY + 49, 8, 21);
        grafica.setColor(new Color(110, 69, 70)); grafica.fillRect(posicionX, posicionY + 49, 2, 29);
        grafica.setColor(new Color(32, 31, 43)); grafica.fillRect(posicionX - 19, posicionY + 108, 14, 5); grafica.fillRect(posicionX + 5, posicionY + 108, 14, 5);
    }

    private void dibujarActa(Graphics2D grafica) {
        grafica.setColor(new Color(25, 24, 31, 90)); grafica.fillRect(0, 0, ANCHO, ALTO);
        grafica.setColor(new Color(41, 31, 35)); grafica.fillRect(12, 194, 616, 154);
        grafica.setColor(new Color(199, 165, 111)); grafica.fillRect(12, 194, 616, 2);
        grafica.setColor(PAPEL); grafica.fillRect(18, 201, 604, 137);
        grafica.setColor(new Color(181, 146, 99)); grafica.fillRect(18, 201, 604, 2);
        grafica.setFont(new Font("Arial", Font.BOLD, 10));
        grafica.setColor(new Color(89, 68, 53));
        String hablante = linea == null ? "ACTA" : linea.hablante;
        grafica.drawString(hablante + ":", 28, 216);
        dibujarTramos(grafica, 28, 238, 568);
        if (esperandoSiguiente && linea != null && linea.pausaAlFinalizar) {
            String indicacionEnter = "[ENTER para continuar]";
            grafica.setFont(new Font("Arial", Font.BOLD, 9));
            grafica.setColor(new Color(89, 68, 53));
            int posicionTextoX = (ANCHO - grafica.getFontMetrics().stringWidth(indicacionEnter)) / 2;
            grafica.drawString(indicacionEnter, posicionTextoX, 310);
        }
        int anchoBarraTotal = 566;
        int altoBarra = 14;
        int posicionBarraX = 28;
        int posicionBarraY = 316;
        grafica.setColor(new Color(77, 62, 56));
        grafica.fillRect(posicionBarraX, posicionBarraY, anchoBarraTotal, altoBarra);
        double tiempoProporcional = linea == null || linea.segundos <= 0
                ? 0.0
                : Math.max(0.0, Math.min(1.0, segundosRestantes / (double) linea.segundos));
        int anchoRestante = (int) (anchoBarraTotal * tiempoProporcional);
        int posicionRestanteX = posicionBarraX + (anchoBarraTotal - anchoRestante) / 2;
        grafica.setColor(segundosRestantes <= 4 ? ROJO : VERDE);
        grafica.fillRect(posicionRestanteX, posicionBarraY, anchoRestante, altoBarra);
    }

    private void dibujarTramos(Graphics2D grafica, int posicionX, int posicionY, int anchoMaximo) {
        if (linea == null) {
            grafica.setFont(new Font("Arial", Font.BOLD, 12)); grafica.setColor(ROJO);
            grafica.drawString(estado, posicionX, posicionY);
            return;
        }
        grafica.setFont(new Font("Arial", Font.PLAIN, 13));
        FontMetrics metricasFuente = grafica.getFontMetrics();
        int posicionDibujoX = posicionX;
        int posicionDibujoY = posicionY;
        boolean mostrarCursor = !terminado && !esperandoSiguiente;
        boolean cursorColocado = false;
        for (LineaDialogo.Tramo tramo : linea.tramos) {
            if ("ELEGIR".equals(tramo.tipo)) {
                boolean eleccionActual = mostrarCursor && !cursorColocado && !tramo.eleccionCompletada;
                for (int indiceOpcion = 0; indiceOpcion < tramo.opciones.size(); indiceOpcion++) {
                    if (indiceOpcion > 0) {
                        int[] posicion = dibujarTexto(grafica, " / ", posicionDibujoX, posicionDibujoY, posicionX, anchoMaximo, metricasFuente, AMARILLO, -1, AMARILLO, -1, false);
                        posicionDibujoX = posicion[0]; posicionDibujoY = posicion[1];
                    }
                    String opcion = tramo.opciones.get(indiceOpcion);
                    boolean activo = tramo.opcionesPosibles.contains(indiceOpcion);
                    int prefijoEscrito = activo ? Math.min(tramo.progreso, opcion.length()) : 0;
                    int indiceObjetivo = eleccionActual && activo
                            && tramo.progreso < opcion.length() ? tramo.progreso : -1;
                    if (indiceObjetivo >= 0) cursorColocado = true;
                    int[] posicion = dibujarTexto(grafica, opcion, posicionDibujoX, posicionDibujoY, posicionX, anchoMaximo, metricasFuente,
                            AMARILLO, prefijoEscrito, AMARILLO_OSCURO, indiceObjetivo, true);
                    posicionDibujoX = posicion[0]; posicionDibujoY = posicion[1];
                }
                continue;
            }
            boolean omitido = "OMITIR".equals(tramo.tipo);
            boolean agregado = "AGREGAR".equals(tramo.tipo);
            Color colorBase = omitido ? ROJO : agregado ? VERDE : TEXTO_NORMAL;
            Color colorEscrito = omitido ? ROJO_OSCURO : agregado ? VERDE_OSCURO : TINTA;
            for (int indice = 0; indice < tramo.textoVisible.length(); indice++) {
                String glifo = tramo.textoVisible.substring(indice, indice + 1);
                int anchoGlifo = metricasFuente.stringWidth(glifo);
                if (posicionDibujoX + anchoGlifo > posicionX + anchoMaximo) { posicionDibujoX = posicionX; posicionDibujoY += 18; }
                boolean tecleado = indice >= tramo.inicioEntrada && indice < tramo.inicioEntrada + tramo.progreso;
                grafica.setColor(tecleado ? colorEscrito : colorBase);
                if (mostrarCursor && !cursorColocado && indice == tramo.inicioEntrada + tramo.progreso
                        && tramo.progreso < tramo.entrada.length()) {
                    dibujarCursor(grafica, posicionDibujoX, posicionDibujoY, anchoGlifo, colorEscrito);
                    cursorColocado = true;
                }
                grafica.drawString(glifo, posicionDibujoX, posicionDibujoY);
                posicionDibujoX += anchoGlifo;
            }
        }
    }

    private int[] dibujarTexto(Graphics2D grafica, String texto, int posicionDibujoX, int posicionDibujoY, int posicionX,
                           int anchoMaximo, FontMetrics metricasFuente, Color colorBase, int prefijoEscrito, Color colorEscrito,
                           int indiceCursor, boolean mostrarPunto) {
        for (int indice = 0; indice < texto.length(); indice++) {
            String glifo = texto.substring(indice, indice + 1);
            int anchoGlifo = metricasFuente.stringWidth(glifo);
            if (posicionDibujoX + anchoGlifo > posicionX + anchoMaximo) { posicionDibujoX = posicionX; posicionDibujoY += 18; }
            grafica.setColor(indice < prefijoEscrito ? colorEscrito : colorBase);
            if (mostrarPunto && indice == indiceCursor) dibujarCursor(grafica, posicionDibujoX, posicionDibujoY, anchoGlifo, colorEscrito);
            grafica.drawString(glifo, posicionDibujoX, posicionDibujoY);
            posicionDibujoX += anchoGlifo;
        }
        return new int[] { posicionDibujoX, posicionDibujoY };
    }

    private void dibujarCursor(Graphics2D grafica, int posicionX, int lineaBase, int anchoGlifo, Color color) {
        Color colorAnterior = grafica.getColor();
        grafica.setColor(color);
        int posicionPuntoX = posicionX + Math.max(1, anchoGlifo / 2);
        grafica.fillOval(posicionPuntoX - 1, lineaBase + 2, 3, 3);
        grafica.setColor(colorAnterior);
    }
}
