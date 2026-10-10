package com.thankstyping;

import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LineaDialogo {
    private static final Pattern PATRON_MARCA = Pattern.compile("\\[(ROJO|VERDE|AMARILLO):([^\\]]+)\\]");

    public int identificador;
    public String hablante;
    public String emocion;
    public int segundos;
    public List<Tramo> tramos = new ArrayList<Tramo>();

    public String entradaEsperada() {
        StringBuilder resultado = new StringBuilder();
        for (Tramo tramo : tramos) resultado.append(tramo.entrada);
        return resultado.toString();
    }

    public void reiniciarProgreso() {
        for (Tramo tramo : tramos) {
            tramo.progreso = 0;
            tramo.opcionElegida = -1;
            tramo.eleccionCompletada = false;
            tramo.opcionesPosibles.clear();
            if (tramo.opciones != null) {
                for (int indice = 0; indice < tramo.opciones.size(); indice++) tramo.opcionesPosibles.add(indice);
            }
            tramo.tecleado.setLength(0);
        }
    }

    public static LineaDialogo crearDesde(GuionJuicio.LineaGuion origen) {
        LineaDialogo linea = new LineaDialogo();
        linea.identificador = origen.identificador;
        linea.hablante = origen.hablante;
        linea.emocion = origen.emocion;
        linea.segundos = origen.tiempo_limite;

        Matcher coincidencias = PATRON_MARCA.matcher(origen.texto_original);
        int finAnterior = 0;
        int indiceEleccion = 0;
        boolean quitarProximoEspacioInicial = false;
        while (coincidencias.find()) {
            quitarProximoEspacioInicial = agregarTextoNormal(linea,
                    origen.texto_original.substring(finAnterior, coincidencias.start()), quitarProximoEspacioInicial);
            String claseMarca = coincidencias.group(1);
            String valor = coincidencias.group(2);
            Tramo tramo = new Tramo();
            tramo.textoVisible = valor;

            if ("ROJO".equals(claseMarca)) {
                tramo.tipo = "OMITIR";
                char[] espacios = new char[valor.length()];
                java.util.Arrays.fill(espacios, ' ');
                tramo.entrada = new String(espacios);
                quitarProximoEspacioInicial = false;
            } else if ("VERDE".equals(claseMarca)) {
                tramo.tipo = "AGREGAR";
                tramo.entrada = valor;
                quitarProximoEspacioInicial = false;
            } else {
                tramo.tipo = "ELEGIR";
                tramo.opciones = new ArrayList<String>();
                for (String opcion : valor.split("/")) tramo.opciones.add(opcion.trim());
                if (tramo.opciones.size() < 2) {
                    throw new IllegalArgumentException("La línea " + origen.identificador + " necesita al menos dos opciones amarillas.");
                }
                if (tramo.opciones.contains("")) {
                    throw new IllegalArgumentException("Las opciones amarillas de la línea " + origen.identificador + " no pueden estar vacías.");
                }
                for (int indice = 0; indice < tramo.opciones.size(); indice++) {
                    for (int indiceInterno = indice + 1; indiceInterno < tramo.opciones.size(); indiceInterno++) {
                        String primera = formaDeTeclado(tramo.opciones.get(indice));
                        String segunda = formaDeTeclado(tramo.opciones.get(indiceInterno));
                        if (primera.startsWith(segunda) || segunda.startsWith(primera)) {
                            throw new IllegalArgumentException("Las alternativas amarillas de la línea " + origen.identificador
                                    + " deben poder distinguirse mientras se tipean.");
                        }
                    }
                }
                String correcta = obtenerOpcionCorrecta(origen, indiceEleccion);
                if (correcta == null || !tramo.opciones.contains(correcta.trim())) {
                    throw new IllegalArgumentException("La opción correcta amarilla de la línea " + origen.identificador
                            + " debe aparecer en opciones_correctas y coincidir con una alternativa.");
                }
                tramo.opcionCorrecta = correcta.trim();
                tramo.entrada = tramo.opcionCorrecta;
                tramo.textoVisible = String.join(" / ", tramo.opciones);
                indiceEleccion++;
                quitarProximoEspacioInicial = false;
            }
            linea.tramos.add(tramo);
            finAnterior = coincidencias.end();
        }
        agregarTextoNormal(linea, origen.texto_original.substring(finAnterior), quitarProximoEspacioInicial);
        if (linea.tramos.isEmpty()) agregarTextoNormal(linea, origen.texto_original, false);
        if (origen.opciones_correctas != null && origen.opciones_correctas.size() != indiceEleccion) {
            throw new IllegalArgumentException("opciones_correctas debe tener una entrada por cada marca AMARILLO en la línea "
                    + origen.identificador + ".");
        }
        linea.reiniciarProgreso();
        return linea;
    }

    private static String obtenerOpcionCorrecta(GuionJuicio.LineaGuion origen, int indice) {
        if (origen.opciones_correctas != null && indice < origen.opciones_correctas.size()) {
            return origen.opciones_correctas.get(indice);
        }
        return null;
    }

    private static String formaDeTeclado(String texto) {
        String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return descompuesto.replace("\u0301", "").replace("\u0308", "");
    }

    private static boolean agregarTextoNormal(LineaDialogo linea, String texto, boolean quitarEspacioInicial) {
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

    public static final class Tramo {
        public String tipo;
        public String textoVisible;
        public String entrada;
        public int inicioEntrada;
        public List<String> opciones;
        public String opcionCorrecta;
        public int progreso;
        public int opcionElegida = -1;
        public boolean eleccionCompletada;
        public final List<Integer> opcionesPosibles = new ArrayList<Integer>();
        public final StringBuilder tecleado = new StringBuilder();
    }
}
