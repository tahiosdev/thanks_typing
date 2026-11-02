package thankstyping;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Demostración breve de las reglas, accesible desde el menú. */
public final class PanelTutorial extends JPanel {

    private static final int ANCHO = 640;
    private static final int ALTO = 360;
    private final Runnable volverAlMenu;
    private final Timer animacion;
    private int pagina;
    private int progreso;

    public PanelTutorial(Runnable volverAlMenu) {
        this.volverAlMenu = volverAlMenu;
        setFocusable(true);
        setPreferredSize(new Dimension(ANCHO * 2, ALTO * 2));
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent evento) {
                if (evento.getKeyCode() == KeyEvent.VK_ESCAPE) terminar();
                if (evento.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (pagina == 4) terminar(); else pagina++;
                    progreso = 0;
                    repaint();
                }
            }
        });
        animacion = new Timer(110, evento -> {
            if (pagina == 0) {
                progreso = (progreso + 1) % 32;
                repaint();
            }
        });
        animacion.start();
    }

    private void terminar() {
        animacion.stop();
        volverAlMenu.run();
    }

    @Override protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        Graphics2D pantalla = (Graphics2D) graficos.create();
        double escala = Math.min(getWidth() / (double) ANCHO, getHeight() / (double) ALTO);
        int ancho = (int) (ANCHO * escala);
        int alto = (int) (ALTO * escala);
        int x = (getWidth() - ancho) / 2;
        int y = (getHeight() - alto) / 2;
        pantalla.translate(x, y);
        pantalla.scale(escala, escala);
        pantalla.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        dibujar(pantalla);
        pantalla.dispose();
    }

    private void dibujar(Graphics2D grafica) {
        grafica.setColor(new Color(34, 38, 55));
        grafica.fillRect(0, 0, ANCHO, ALTO);
        grafica.setColor(new Color(53, 57, 73));
        grafica.fillRect(0, 25, ANCHO, 160);
        grafica.setColor(new Color(73, 51, 47));
        grafica.fillRect(0, 185, ANCHO, 175);
        grafica.setFont(new Font("Arial", Font.BOLD, 17));
        grafica.setColor(new Color(239, 221, 176));
        centrado(grafica, "CONTROLES", 45);
        grafica.setColor(new Color(239, 221, 176));
        grafica.fillRect(18, 112, 604, 214);
        grafica.setColor(new Color(89, 68, 53));

        if (pagina == 0) mostrarEscritura(grafica);
        else if (pagina == 1) mostrarRegla(grafica, "ROJO", new Color(196, 70, 68),
                "No escribas el texto rojo.", "Avanzá dejando espacios.");
        else if (pagina == 2) mostrarRegla(grafica, "VERDE", new Color(64, 139, 97),
                "Escribí el texto verde.", "Para que cuente, escribí sus palabras en MAYÚSCULAS.");
        else if (pagina == 3) mostrarRegla(grafica, "AMARILLO", new Color(205, 157, 48),
                "Elegí una de las opciones amarillas.", "La frase que escribas queda registrada en el acta.");
        else mostrarFinal(grafica);
    }

    private void mostrarEscritura(Graphics2D grafica) {
        grafica.setFont(new Font("Arial", Font.BOLD, 12));
        grafica.drawString("ESCRIBÍ SIGUIENDO EL PUNTO", 38, 144);
        String ejemplo = "El acusado declara su versión.";
        int cantidad = Math.min(progreso, ejemplo.length());
        grafica.setFont(new Font("Arial", Font.PLAIN, 18));
        grafica.setColor(new Color(30, 26, 32));
        grafica.drawString(ejemplo.substring(0, cantidad), 38, 192);
        grafica.setColor(new Color(126, 124, 121));
        grafica.drawString(ejemplo.substring(cantidad), 38 + grafica.getFontMetrics().stringWidth(ejemplo.substring(0, cantidad)), 192);
        grafica.setFont(new Font("Arial", Font.PLAIN, 13));
        grafica.setColor(new Color(89, 68, 53));
        grafica.drawString("Cada tecla correcta hace avanzar la transcripción.", 38, 242);
        pie(grafica, "[ENTER para ver las reglas]");
    }

    private void mostrarRegla(Graphics2D grafica, String titulo, Color color, String lineaUno, String lineaDos) {
        grafica.setFont(new Font("Arial", Font.BOLD, 22));
        grafica.setColor(color);
        centrado(grafica, titulo, 165);
        grafica.setFont(new Font("Arial", Font.PLAIN, 15));
        grafica.setColor(new Color(41, 31, 35));
        centrado(grafica, lineaUno, 210);
        centrado(grafica, lineaDos, 238);
        pie(grafica, "[ENTER para continuar]");
    }

    private void mostrarFinal(Graphics2D grafica) {
        grafica.setFont(new Font("Arial", Font.BOLD, 20));
        grafica.setColor(new Color(41, 31, 35));
        centrado(grafica, "LISTO PARA EL JUICIO", 188);
        grafica.setFont(new Font("Arial", Font.PLAIN, 14));
        centrado(grafica, "Tu acta puede cambiar el resultado.", 222);
        pie(grafica, "[ENTER para volver al menú]");
    }

    private void pie(Graphics2D grafica, String texto) {
        grafica.setFont(new Font("Arial", Font.PLAIN, 12));
        grafica.setColor(new Color(89, 68, 53));
        centrado(grafica, texto, 300);
    }

    private void centrado(Graphics2D grafica, String texto, int y) {
        FontMetrics metricas = grafica.getFontMetrics();
        grafica.drawString(texto, (ANCHO - metricas.stringWidth(texto)) / 2, y);
    }
}
