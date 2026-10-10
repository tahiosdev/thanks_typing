// Paquete de las clases del juego.
package thankstyping;

// Listas din�micas para los segmentos del di�logo y sus opciones.
import java.util.ArrayList;
import java.util.List;
// Normalizer permite comparar letras con y sin algunos signos diacr�ticos.
import java.text.Normalizer;
// Matcher y Pattern buscan las marcas especiales dentro del texto JSON.
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Representaci�n lista para jugar de una l�nea del juicio. */
public final class LineaDialogo {
    // Reconoce las tres marcas admitidas y captura su contenido hasta el corchete de cierre.
    private static final Pattern PATRON_MARCA = Pattern.compile("\\[(ROJO|VERDE|AMARILLO):([^\\]]+)\\]");

    // Datos de presentaci�n y juego copiados desde la l�nea del JSON.
    public int identificador;
    public String hablante;
    public String emocion;
    public int segundos;
    // Si es true, espera Enter al terminar esta linea.
    public boolean pausaAlFinalizar;
    // Secuencia de tramos normales y especiales que componen el di�logo.
    public List<Tramo> tramos = new ArrayList<Tramo>();

    /** Une lo que el jugador debe teclear para completar todos los tramos. */
    public String entradaEsperada() {
        StringBuilder resultado = new StringBuilder();
        // Cada tramo conoce su secuencia de entrada, incluso si su texto visible es distinto.
        for (Tramo tramo : tramos) resultado.append(tramo.entrada);
        return resultado.toString();
    }

    /** Devuelve cada tramo a su estado inicial para volver a jugar esta l�nea. */
    public void reiniciarProgreso() {
        for (Tramo tramo : tramos) {
            // Borra el avance y la selecci�n que se hicieron en el intento anterior.
            tramo.progreso = 0;
            tramo.opcionElegida = -1;
            tramo.eleccionCompletada = false;
            // Al reiniciar, todas las alternativas vuelven a estar disponibles.
            tramo.opcionesPosibles.clear();
            if (tramo.opciones != null) {
                // Guarda los �ndices de cada alternativa amarilla.
                for (int indice = 0; indice < tramo.opciones.size(); indice++) tramo.opcionesPosibles.add(indice);
            }
            // Borra las teclas registradas para poder evaluar un intento nuevo.
            tramo.tecleado.setLength(0);
        }
    }

    /** Convierte la l�nea del JSON en tramos de texto que el juego puede dibujar y evaluar. */
    public static LineaDialogo crearDesde(GuionJuicio.LineaGuion origen, boolean tildesSensibles) {
        // Crea el objeto de ejecuci�n y copia sus metadatos.
        LineaDialogo linea = new LineaDialogo();
        linea.identificador = origen.identificador;
        linea.hablante = origen.hablante;
        linea.emocion = origen.emocion;
        linea.segundos = origen.tiempo_limite;
        linea.pausaAlFinalizar = origen.pausaAlFinalizar;

        // El patr�n encuentra cada bloque especial dentro de texto_original.
        Matcher coincidencias = PATRON_MARCA.matcher(origen.texto_original);
        // finAnterior marca d�nde termin� el texto procesado en la b�squeda anterior.
        int finAnterior = 0;
        // Cuenta marcas amarillas para relacionarlas con opciones_correctas.
        int indiceEleccion = 0;
        // Permite evitar que el espacio de separaci�n se pida dos veces al salir de una marca.
        boolean quitarProximoEspacioInicial = false;
        while (coincidencias.find()) {
            // Antes de cada marca, agrega el fragmento ordinario que la precede.
            quitarProximoEspacioInicial = agregarTextoNormal(linea,
                    origen.texto_original.substring(finAnterior, coincidencias.start()), quitarProximoEspacioInicial);
            // El primer grupo es el tipo (ROJO/VERDE/AMARILLO) y el segundo el contenido.
            String claseMarca = coincidencias.group(1);
            String valor = coincidencias.group(2);
            // El tramo mantiene por separado lo que se ve y lo que se debe escribir.
            Tramo tramo = new Tramo();
            tramo.textoVisible = valor;

            if ("ROJO".equals(claseMarca)) {
                // En rojo se ve la palabra, pero cada posici�n se completa pulsando espacio.
                tramo.tipo = "OMITIR";
                char[] espacios = new char[valor.length()];
                // Prepara una secuencia de espacios con la misma longitud que el texto rojo.
                java.util.Arrays.fill(espacios, ' ');
                tramo.entrada = new String(espacios);
                quitarProximoEspacioInicial = false;
            } else if ("VERDE".equals(claseMarca)) {
                // En verde la palabra se agrega a la transcripci�n y se escribe en may�sculas.
                tramo.tipo = "AGREGAR";
                tramo.entrada = valor;
                quitarProximoEspacioInicial = false;
            } else {
                // Amarillo muestra alternativas separadas por barras.
                tramo.tipo = "ELEGIR";
                tramo.opciones = new ArrayList<String>();
                // Divide las alternativas y quita espacios sobrantes a sus extremos.
                for (String opcion : valor.split("/")) tramo.opciones.add(opcion.trim());
                // Debe haber al menos dos alternativas.
                if (tramo.opciones.size() < 2) {
                    throw new IllegalArgumentException("La l�nea " + origen.identificador + " necesita al menos dos opciones amarillas.");
                }
                if (tramo.opciones.contains("")) {
                    throw new IllegalArgumentException("Las opciones amarillas de la l�nea " + origen.identificador + " no pueden estar vac�as.");
                }
                for (int indice = 0; indice < tramo.opciones.size(); indice++) {
                    for (int indiceInterno = indice + 1; indiceInterno < tramo.opciones.size(); indiceInterno++) {
                        String primera = formaDeTeclado(tramo.opciones.get(indice), tildesSensibles);
                        String segunda = formaDeTeclado(tramo.opciones.get(indiceInterno), tildesSensibles);
                        if (primera.startsWith(segunda) || segunda.startsWith(primera)) {
                            throw new IllegalArgumentException("Las alternativas amarillas de la l�nea " + origen.identificador
                                    + " deben poder distinguirse mientras se tipean.");
                        }
                    }
                }
                // Busca cu�l alternativa debe contar como correcta en esta marca.
                String correcta = obtenerOpcionCorrecta(origen, indiceEleccion);
                if (correcta == null || !tramo.opciones.contains(correcta.trim())) {
                    throw new IllegalArgumentException("La opci�n correcta amarilla de la l�nea " + origen.identificador
                            + " debe aparecer en opciones_correctas y coincidir con una alternativa.");
                }
                tramo.opcionCorrecta = correcta.trim();
                tramo.entrada = tramo.opcionCorrecta;
                tramo.textoVisible = String.join(" / ", tramo.opciones);
                indiceEleccion++;
                quitarProximoEspacioInicial = false;
            }
            // Incorpora el tramo ya preparado a la l�nea.
            linea.tramos.add(tramo);
            // La b�squeda siguiente comenzar� despu�s de esta marca.
            finAnterior = coincidencias.end();
        }
        // Agrega el fragmento normal que queda despu�s de la �ltima marca.
        agregarTextoNormal(linea, origen.texto_original.substring(finAnterior), quitarProximoEspacioInicial);
        // Si no hab�a marcas, todo el texto original es un tramo normal.
        if (linea.tramos.isEmpty()) agregarTextoNormal(linea, origen.texto_original, false);
        // Comprueba que haya exactamente una respuesta por marca amarilla.
        if (origen.opciones_correctas != null && origen.opciones_correctas.size() != indiceEleccion) {
            throw new IllegalArgumentException("opciones_correctas debe tener una entrada por cada marca AMARILLO en la l�nea "
                    + origen.identificador + ".");
        }
        // Inicializa las listas de opciones y el progreso antes de devolver la l�nea.
        linea.reiniciarProgreso();
        return linea;
    }

    /** Obtiene la respuesta correcta amarilla correspondiente al �ndice indicado. */
    private static String obtenerOpcionCorrecta(GuionJuicio.LineaGuion origen, int indice) {
        // Devuelve el elemento solo si la lista existe y contiene ese �ndice.
        if (origen.opciones_correctas != null && indice < origen.opciones_correctas.size()) {
            return origen.opciones_correctas.get(indice);
        }
        return null;
    }

    /** Normaliza una letra para poder comparar acentos escritos como caracteres combinados. */
    private static String formaDeTeclado(String texto, boolean tildesSensibles) {
        if (tildesSensibles) return texto;
        String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return descompuesto.replace("\u0301", "").replace("\u0308", "");
    }

    /** Agrega un fragmento com�n y aplica, si corresponde, la regla del espacio inicial. */
    private static boolean agregarTextoNormal(LineaDialogo linea, String texto, boolean quitarEspacioInicial) {
        // Si no hay contenido, conserva la instrucci�n pendiente para el siguiente fragmento.
        if (texto.isEmpty()) return quitarEspacioInicial;
        Tramo tramo = new Tramo();
        tramo.tipo = "NORMAL";
        tramo.textoVisible = texto;
        tramo.entrada = texto;
        if (quitarEspacioInicial && Character.isWhitespace(texto.charAt(0))) {
            tramo.entrada = texto.substring(1);
            tramo.inicioEntrada = 1;
        }
        linea.tramos.add(tramo);
        return false;
    }

    /** Un tramo es una parte continua del texto con una regla de escritura propia. */
    public static final class Tramo {
        // Categor�a: NORMAL, OMITIR, AGREGAR o ELEGIR.
        public String tipo;
        // Contenido que aparece en pantalla.
        public String textoVisible;
        // Secuencia que se acepta para avanzar (puede diferir de lo visible).
        public String entrada;
        // Posici�n del primer car�cter escribible, usada cuando se quit� espacio inicial.
        public int inicioEntrada;
        // Alternativas disponibles para los tramos amarillos.
        public List<String> opciones;
        // Alternativa que suma como correcta.
        public String opcionCorrecta;
        // Cantidad de posiciones procesadas.
        public int progreso;
        // �ndice de la alternativa elegida; -1 significa que todav�a no hay elecci�n.
        public int opcionElegida = -1;
        // Indica que ya se complet� una alternativa amarilla.
        public boolean eleccionCompletada;
        // �ndices amarillos que a�n coinciden con las teclas escritas.
        public final List<Integer> opcionesPosibles = new ArrayList<Integer>();
        // Teclas realmente pulsadas, necesarias para evaluar espacios y may�sculas.
        public final StringBuilder tecleado = new StringBuilder();
    }
}
