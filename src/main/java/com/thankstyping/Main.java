package com.thankstyping;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Thanks, Typing!");
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            CourtroomPanel panel = new CourtroomPanel();
            window.setContentPane(panel);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent event) { panel.requestFocusInWindow(); }
            });
            window.setPreferredSize(new Dimension(1280, 720));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setResizable(true);
            window.setVisible(true);
        });
    }
}
