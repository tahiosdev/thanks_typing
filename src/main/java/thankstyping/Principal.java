package thankstyping;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.Dimension;
import java.awt.CardLayout;
import java.nio.file.Paths;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class Principal {

    private Principal() { }

    public static void main(String[] argumentos) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Thanks, Typing!");
            ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            CardLayout tarjetas = new CardLayout();
            javax.swing.JPanel contenido = new javax.swing.JPanel(tarjetas);
            PanelMenu menu = new PanelMenu(
                    () -> abrirNivel(contenido, tarjetas, 1),
                    () -> abrirNivel(contenido, tarjetas, 2),
                    () -> abrirTutorial(contenido, tarjetas));
            contenido.add(menu, "menu");
            ventana.setContentPane(contenido);
            ventana.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent evento) { menu.requestFocusInWindow(); }
            });
            ventana.setPreferredSize(new Dimension(1280, 720));
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setResizable(true);
            ventana.setVisible(true);
        });
    }

    private static void abrirNivel(javax.swing.JPanel contenido, CardLayout tarjetas, int nivel) {
        String juicio = nivel == 1 ? "JuicioPrimero.json" : "JuicioSegundo.json";
        String carpetaImagenes = nivel == 1 ? "juicio 1" : "juicio 2";
        PanelTribunal panel = new PanelTribunal(
                Paths.get("assets", "dialogs", juicio),
                new java.nio.file.Path[] {
                        Paths.get("assets", "images", carpetaImagenes, "1.png"),
                        Paths.get("assets", "images", carpetaImagenes, "2.png")
                },
                () -> tarjetas.show(contenido, "menu"));
        contenido.add(panel, "juego");
        tarjetas.show(contenido, "juego");
        panel.requestFocusInWindow();
    }

    private static void abrirTutorial(javax.swing.JPanel contenido, CardLayout tarjetas) {
        PanelTutorial tutorial = new PanelTutorial(() -> tarjetas.show(contenido, "menu"));
        contenido.add(tutorial, "tutorial");
        tarjetas.show(contenido, "tutorial");
        tutorial.requestFocusInWindow();
    }
}
