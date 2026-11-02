package thankstyping;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class PanelMenu extends JPanel {

    private static final Color PAPEL = new Color(239, 221, 176);
    private static final Color DORADO = new Color(199, 165, 111);

    public PanelMenu(Runnable nivelUno, Runnable nivelDos, Runnable controles) {
        setOpaque(true);
        setLayout(new GridBagLayout());

        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setOpaque(false);
        GridBagConstraints restriccion = new GridBagConstraints();
        restriccion.gridx = 0;
        restriccion.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = etiqueta("THANKS, TYPING!", 38, PAPEL);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        restriccion.gridy = 0;
        restriccion.insets = new Insets(0, 0, 28, 0);
        contenido.add(titulo, restriccion);

        JPanel niveles = new JPanel(new GridBagLayout());
        niveles.setOpaque(false);
        GridBagConstraints tarjeta = new GridBagConstraints();
        tarjeta.gridy = 0;
        tarjeta.insets = new Insets(0, 8, 0, 8);
        JButton botonUno = crearBoton("NIVEL 1", "NO TILDES");
        JButton botonDos = crearBoton("NIVEL 2", "SI TILDES");
        botonUno.addActionListener(evento -> nivelUno.run());
        botonDos.addActionListener(evento -> nivelDos.run());
        tarjeta.gridx = 0;
        niveles.add(botonUno, tarjeta);
        tarjeta.gridx = 1;
        niveles.add(botonDos, tarjeta);
        restriccion.gridy = 1;
        restriccion.insets = new Insets(0, 0, 0, 0);
        contenido.add(niveles, restriccion);

        JButton botonControles = crearBoton("CONTROLES", "");
        botonControles.addActionListener(evento -> controles.run());
        restriccion.gridy = 2;
        restriccion.insets = new Insets(14, 0, 0, 0);
        contenido.add(botonControles, restriccion);

        add(contenido, new GridBagConstraints());
    }

    private JLabel etiqueta(String texto, int tamano, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(color);
        etiqueta.setFont(new Font("Arial", Font.BOLD, tamano));
        return etiqueta;
    }

    private JButton crearBoton(String nivel, String tildes) {
        JButton boton = new JButton("<html><div style='text-align:center; width:210px;'>"
                + "<span style='font-size:17px;'><b>" + nivel + "</b></span><br>"
                + (tildes.isEmpty() ? "" : "<br><br><span style='font-size:12px; color:#ead9ad;'>" + tildes + "</span>")
                + "</div></html>");
        boton.setPreferredSize(new Dimension(245, 132));
        boton.setFont(new Font("Arial", Font.PLAIN, 13));
        boton.setForeground(new Color(221, 215, 204));
        boton.setBackground(new Color(54, 48, 55));
        boton.setBorder(crearBorde(DORADO));
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addChangeListener(evento -> {
            boolean sobreBoton = boton.getModel().isRollover();
            boton.setBackground(sobreBoton ? new Color(76, 62, 58) : new Color(54, 48, 55));
            boton.setBorder(crearBorde(sobreBoton ? PAPEL : DORADO));
        });
        return boton;
    }

    private javax.swing.border.Border crearBorde(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    @Override protected void paintComponent(Graphics graficos) {
        Graphics2D grafica = (Graphics2D) graficos.create();
        grafica.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        grafica.setPaint(new GradientPaint(0, 0, new Color(23, 28, 42), 0, getHeight(), new Color(62, 42, 43)));
        grafica.fillRect(0, 0, getWidth(), getHeight());
        grafica.setColor(new Color(75, 68, 79, 110));
        grafica.fillRect(0, getHeight() - 180, getWidth(), 180);
        grafica.setColor(new Color(119, 85, 62, 150));
        grafica.fillRect(0, getHeight() - 38, getWidth(), 38);
        grafica.setColor(new Color(222, 191, 128, 110));
        grafica.fillRect(0, 28, getWidth(), 2);
        int centro = getWidth() / 2;
        grafica.setColor(new Color(33, 35, 47, 170));
        grafica.fillRect(centro - 95, 55, 190, 100);
        grafica.setColor(DORADO);
        grafica.fillRect(centro - 98, 53, 196, 3);
        grafica.dispose();
    }
}
