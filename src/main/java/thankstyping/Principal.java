package thankstyping;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class Principal {

    private Principal() { }

    public static void main(String[] argumentos) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Thanks, Typing!");
            ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            PanelTribunal panel = new PanelTribunal();
            ventana.setContentPane(panel);
            ventana.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent evento) { panel.requestFocusInWindow(); }
            });
            ventana.setPreferredSize(new Dimension(1280, 720));
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setResizable(true);
            ventana.setVisible(true);
        });
    }
}
