// Paquete que contiene las clases Java del juego.
package thankstyping;

// JPanel es la superficie donde se dibuja la partida; Timer actualiza el reloj periódicamente.
import javax.swing.JPanel;
import javax.swing.Timer;
// Clases gráficas de AWT usadas para color, tamaño, fuentes y dibujo.
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
// Eventos del teclado que permiten leer las teclas del jugador.
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// BufferedImage sirve como lienzo de resolución fija para el estilo pixel art.
import java.awt.image.BufferedImage;
// Normalizer ayuda a comparar letras ignorando tildes.
import java.text.Normalizer;
// Listas para manejar las alternativas amarillas que aún coinciden.
import java.util.ArrayList;
import java.util.List;
// Paths construye la ruta del archivo JSON de forma compatible entre sistemas operativos.
import java.nio.file.Paths;

/** Panel principal: procesa las entradas, controla la partida y dibuja la escena completa. */
public final class PanelTribunal extends JPanel {
    // Resolución lógica del juego; la imagen se amplía al tamaño real de la ventana.
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

    // El lienzo de trabajo conserva siempre la resolución pixelada del juego.
    private final BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
    // Timer Swing despierta cada 100 milisegundos para revisar el reloj.
    private final Timer temporizador;
    // Guion completo y línea actual interpretada.
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
    // Historial de teclas aceptadas; permite reconstruir el avance al borrar.
    private final StringBuilder entradaEscrita = new StringBuilder();

    /** Configura el panel, carga el guion, conecta el teclado e inicia el reloj. */
    public PanelTribunal() {
        // El panel debe poder recibir directamente los eventos del teclado.
        setFocusable(true);
        // Tamaño visual inicial: el contenido se dibuja al doble de su resolución lógica.
        setPreferredSize(new Dimension(ANCHO * 2, ALTO * 2));
        setBackground(Color.BLACK);
        // Carga el JSON y deja lista la primera línea antes de conectar el temporizador.
        cargarGuion();
        // El listener traduce eventos físicos del teclado en acciones de juego.
        addKeyListener(new KeyAdapter() {
            // keyTyped entrega caracteres que se pueden escribir, como letras y espacios.
            @Override public void keyTyped(KeyEvent evento) { procesarTecla(evento.getKeyChar()); }
            // keyPressed permite detectar teclas de control como retroceso y Enter.
            @Override public void keyPressed(KeyEvent evento) {
                // Retroceso elimina el último carácter aceptado y vuelve a calcular el estado.
                if (evento.getKeyCode() == KeyEvent.VK_BACK_SPACE && !terminado && !esperandoSiguiente && cursorEntrada > 0) {
                    entradaEscrita.setLength(entradaEscrita.length() - 1);
                    reconstruirProgreso();
                    repaint();
                } else if ((terminado || esperandoSiguiente) && evento.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (terminado) reiniciarLinea(); else avanzarOFinalizar();
                }
            }
        });
        // El reloj se consulta seis veces por segundo; el cálculo real usa tiempo transcurrido.
        temporizador = new Timer(100, evento -> actualizarReloj());
        temporizador.start();
    }

    /** Lee el guion de demostración, calcula el total y prepara la primera línea. */
    private void cargarGuion() {
        try {
            // La ruta es relativa a la carpeta raíz desde la que se ejecuta el programa.
            guion = CargadorGuion.cargar(Paths.get("assets", "dialogs", "juicio_demostracion.json"));
            // Reinicia los totales para sumar los requisitos de cada línea del guion.
            caracteresTotales = 0;
            palabrasEspecialesTotales = 0;
            // Recorre el modelo JSON para contar lo que se espera escribir y sabotear.
            for (GuionJuicio.LineaGuion origen : guion.lineas) {
                // Convierte las marcas textuales del JSON a tramos de juego.
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

    /** Prepara una línea específica y reinicia su reloj y su progreso. */
    private void iniciarLinea(int indice) {
        // Guarda la ubicación de esta línea y crea su representación ejecutable.
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

    /** Suma una única vez el resultado de la línea actual a las estadísticas del juicio. */
    private void registrarLinea() {
        // La marca evita duplicar puntos cuando se acaba el tiempo y luego se pulsa Enter.
        if (!lineaRegistrada) {
            caracteresCorrectos += progresoCorrecto();
            palabrasEspecialesCorrectas += contarPalabrasEspeciales(linea, true);
            lineaRegistrada = true;
        }
    }

    /** Cuenta las palabras especiales del diálogo o las que ya se resolvieron correctamente. */
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

    /** Cuenta grupos de caracteres separados por uno o más espacios. */
    private int contarPalabras(String texto) {
        String recortado = texto == null ? "" : texto.trim();
        return recortado.isEmpty() ? 0 : recortado.split("\\s+").length;
    }

    /** Cuenta palabras rojas completas cuyo jugador pulsó espacio en todas sus posiciones. */
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

    /** Cuenta las palabras verdes completas que fueron efectivamente tecleadas en mayúsculas. */
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

    /** Calcula los caracteres verdes que aportan al puntaje según la regla de mayúsculas. */
    private int contarCaracteresVerdesCorrectos(LineaDialogo.Tramo tramo) {
        // Arreglo paralelo: true indica que el carácter pertenece a una palabra verde correcta.
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

    /** Indica si una palabra ya terminó y todas sus letras se pulsaron en mayúsculas. */
    private boolean estaPalabraEnMayusculas(LineaDialogo.Tramo tramo, int inicio, int fin) {
        // Una palabra a medio escribir aún no puede recibir crédito.
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

    /** Avanza a la próxima línea o calcula el resultado final si ya no quedan diálogos. */
    private void avanzarOFinalizar() {
        // Esta acción solo tiene sentido después de terminar una línea.
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

    /** Registra la linea y espera Enter solo cuando break es true en el JSON. */
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

    /** Procesa una tecla escribible y comprueba si se completo la linea. */
    private void procesarTecla(char caracter) {
        // No se admiten teclas si el juego está detenido, esperando Enter o sin línea válida.
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

    /** Aplica una tecla al primer tramo incompleto y actualiza su progreso si coincide. */
    private boolean aplicarCaracter(char tecleado) {
        // Busca el tramo que debe recibir la próxima tecla.
        for (LineaDialogo.Tramo tramo : linea.tramos) {
            if ("OMITIR".equals(tramo.tipo)) {
                // Rojo acepta un espacio (sabotaje) o el carácter visible correspondiente.
                if (tramo.progreso >= tramo.entrada.length()) continue;
                char caracterVisible = tramo.textoVisible.charAt(tramo.progreso);
                if (tecleado != ' ' && !mismaLetra(tecleado, caracterVisible)) return false;
                tramo.progreso++;
                tramo.tecleado.append(tecleado);
                return true;
            }
            if ("ELEGIR".equals(tramo.tipo)) {
                // Una elección ya completada deja paso a los siguientes tramos.
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

    /** Compara dos caracteres después de quitar tildes y diéresis. */
    private boolean mismaLetra(char tecleado, char letraEsperada) {
        if (guion != null && guion.tildes) return tecleado == letraEsperada;
        return sinAcento(tecleado).equals(sinAcento(letraEsperada));
    }

    /** Identifica marcas de acento pulsadas como teclas separadas por ciertos teclados. */
    private boolean esAcentoCombinado(char caracter) {
        if (guion != null && guion.tildes) return false;
        int tipo = Character.getType(caracter);
        return tipo == Character.NON_SPACING_MARK || tipo == Character.COMBINING_SPACING_MARK
                || caracter == '\u00B4' || caracter == '\u00A8';
    }

    /** Devuelve el carácter sin los signos agudo o diéresis para facilitar la comparación. */
    private String sinAcento(char caracter) {
        String descompuesto = Normalizer.normalize(String.valueOf(caracter), Normalizer.Form.NFD);
        return descompuesto.replace("\u0301", "").replace("\u0308", "");
    }

    /** Comprueba que todos los tramos obligatorios hayan sido completados. */
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

    /** Calcula cuántas posiciones de esta línea cumplen las reglas para el puntaje. */
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

    /** Reconstruye el progreso después de borrar una tecla, reaplicando el historial restante. */
    private void reconstruirProgreso() {
        // Restablece todos los tramos antes de reproducir las teclas que aún quedan.
        linea.reiniciarProgreso();
        cursorEntrada = 0;
        for (int indice = 0; indice < entradaEscrita.length(); indice++) {
            if (aplicarCaracter(entradaEscrita.charAt(indice))) cursorEntrada++;
        }
    }

    /** Reduce el tiempo restante usando el tiempo real transcurrido desde la última actualización. */
    private void actualizarReloj() {
        // No corre mientras se espera Enter, la partida terminó o no existe una línea activa.
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

    /** Reinicia las estadísticas de la partida y vuelve a cargar la primera línea. */
    private void reiniciarLinea() {
        caracteresCorrectos = 0;
        palabrasEspecialesCorrectas = 0;
        errores = 0;
        iniciarLinea(0);
        requestFocusInWindow();
    }

    /** Dibuja el lienzo lógico y lo escala para ajustarlo al tamaño real del panel. */
    @Override protected void paintComponent(Graphics graficos) {
        // Pinta primero el fondo estándar del componente Swing.
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

    /** Dibuja con rectángulos de color la sala del tribunal y sus personajes pixelados. */
    private void dibujarSala(Graphics2D grafica) {
        // Bandas del fondo: pared, paneles, muebles y piso.
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

    /** Dibuja una figura simplificada usando rectángulos, como un sprite pixel art. */
    private void dibujarPersona(Graphics2D grafica, int posicionX, int posicionY, Color chaqueta, Color piel, Color cabello) {
        // Cada rectángulo representa una parte del cuerpo o la ropa.
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

    /** Dibuja el papel, el hablante, el diálogo y la barra de tiempo. */
    private void dibujarActa(Graphics2D grafica) {
        // Sombra y borde oscuro bajo el papel para separarlo del escenario.
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
        // El fondo ocupa todo el espacio; el tiempo restante se encoge desde ambos extremos hacia el centro.
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

    /** Dibuja los tramos del diálogo en varias líneas y coloca el punto bajo la tecla actual. */
    private void dibujarTramos(Graphics2D grafica, int posicionX, int posicionY, int anchoMaximo) {
        // Si no hay línea, muestra el error de carga en el área del diálogo.
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
        // Cada tramo usa su propio color y regla de resaltado.
        for (LineaDialogo.Tramo tramo : linea.tramos) {
            if ("ELEGIR".equals(tramo.tipo)) {
                // En amarillo, marca la alternativa activa y presenta todas las opciones posibles.
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
                // Los tramos amarillos ya se dibujaron; sigue al próximo tramo.
                continue;
            }

            // Determina el color base y el color oscuro para el texto ya tecleado.
            boolean omitido = "OMITIR".equals(tramo.tipo);
            boolean agregado = "AGREGAR".equals(tramo.tipo);
            Color colorBase = omitido ? ROJO : agregado ? VERDE : TEXTO_NORMAL;
            Color colorEscrito = omitido ? ROJO_OSCURO : agregado ? VERDE_OSCURO : TINTA;
            // Dibuja el texto carácter por carácter para poder colorear el progreso parcial.
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

    /** Dibuja una cadena por caracteres y devuelve la posición donde debe continuar el siguiente tramo. */
    private int[] dibujarTexto(Graphics2D grafica, String texto, int posicionDibujoX, int posicionDibujoY, int posicionX,
                           int anchoMaximo, FontMetrics metricasFuente, Color colorBase, int prefijoEscrito, Color colorEscrito,
                           int indiceCursor, boolean mostrarPunto) {
        // El dibujo individual permite colorear cada prefijo y ubicar el cursor preciso.
        for (int indice = 0; indice < texto.length(); indice++) {
            String glifo = texto.substring(indice, indice + 1);
            int anchoGlifo = metricasFuente.stringWidth(glifo);
            if (posicionDibujoX + anchoGlifo > posicionX + anchoMaximo) { posicionDibujoX = posicionX; posicionDibujoY += 18; }
            grafica.setColor(indice < prefijoEscrito ? colorEscrito : colorBase);
            if (mostrarPunto && indice == indiceCursor) dibujarCursor(grafica, posicionDibujoX, posicionDibujoY, anchoGlifo, colorEscrito);
            grafica.drawString(glifo, posicionDibujoX, posicionDibujoY);
            posicionDibujoX += anchoGlifo;
        }
        // Devuelve la coordenada final para continuar dibujando el texto siguiente.
        return new int[] { posicionDibujoX, posicionDibujoY };
    }

    /** Dibuja un punto pequeño debajo de la posición actual de escritura. */
    private void dibujarCursor(Graphics2D grafica, int posicionX, int lineaBase, int anchoGlifo, Color color) {
        // Conserva el color original para no afectar los dibujos que vienen después.
        Color colorAnterior = grafica.getColor();
        grafica.setColor(color);
        int posicionPuntoX = posicionX + Math.max(1, anchoGlifo / 2);
        grafica.fillOval(posicionPuntoX - 1, lineaBase + 2, 3, 3);
        grafica.setColor(colorAnterior);
    }
}
