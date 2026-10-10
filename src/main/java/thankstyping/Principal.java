// Paquete del juego.
package thankstyping;

// JFrame es la ventana; SwingUtilities crea la interfaz en el hilo gr�fico de Swing.
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
// Define qu� debe hacer la ventana cuando se cierra.
import javax.swing.WindowConstants;
// Dimension fija el tama�o preferido de la ventana.
import java.awt.Dimension;
// Estos eventos permiten pedir el foco del teclado al abrir la ventana.
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Punto de entrada: crea la ventana principal y coloca dentro el panel del juego. */
public final class Principal {
    // Evita crear instancias: la aplicaci�n se inicia mediante main.
    private Principal() { }

    /** M�todo que ejecuta Java al iniciar el programa. */
    public static void main(String[] argumentos) {
        // Swing requiere construir y actualizar la interfaz en su hilo de eventos.
        SwingUtilities.invokeLater(() -> {
            // Crea la ventana y establece el t�tulo de la aplicaci�n.
            JFrame ventana = new JFrame("Thanks, Typing!");
            // Cierra el proceso cuando el jugador cierra la ventana.
            ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            // El panel contiene la l�gica y el dibujo del juego.
            PanelTribunal panel = new PanelTribunal();
            // Hace que el panel ocupe el �rea interior de la ventana.
            ventana.setContentPane(panel);
            // Al abrir, intenta que el panel reciba las teclas del jugador.
            ventana.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent evento) { panel.requestFocusInWindow(); }
            });
            // Establece el tama�o inicial; pack lo aplica usando el tama�o preferido del contenido.
            ventana.setPreferredSize(new Dimension(1280, 720));
            ventana.pack();
            // Centra la ventana en la pantalla.
            ventana.setLocationRelativeTo(null);
            // Permite cambiar su tama�o; el panel adapta la escala del dibujo.
            ventana.setResizable(true);
            // Muestra la ventana una vez configurada.
            ventana.setVisible(true);
        });
    }
}
