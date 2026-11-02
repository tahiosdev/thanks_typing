package thankstyping;

import java.io.File;
import java.util.Random;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** Reproductor sencillo y tolerante a fallos para los sonidos locales del juego. */
public final class SonidosJuego {

    private static final String CARPETA = "assets/sounds/Sonidos/";
    private final Random aleatorio = new Random();
    private Clip musica;

    public void iniciarMusica() {
        detenerMusica();
        try {
            musica = AudioSystem.getClip();
            musica.open(AudioSystem.getAudioInputStream(new File(CARPETA + "CANCIÓN FONDO JUEGO.wav")));
            musica.setFramePosition(0);
            musica.setLoopPoints(0, -1);
            musica.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception excepcion) {
            // El juego sigue funcionando aunque el equipo no tenga audio disponible.
            musica = null;
        }
    }

    public void detenerMusica() {
        if (musica != null) {
            musica.stop();
            musica.close();
            musica = null;
        }
    }

    public void tecla() { reproducir("TECLAS " + (aleatorio.nextInt(3) + 1) + ".wav"); }
    public void error() { reproducir("RUIDO ERROR " + (aleatorio.nextInt(2) + 1) + ".wav"); }
    public void pagina() { reproducir("SONIDO PASE DE PAGINA.wav"); }
    public void abogadoEnojado() { reproducir("RUIDO ABOGADO ENOJADO.wav"); }
    public void abogadoPensativo() { reproducir("RUIDO ABOGADO PENSATIVO.wav"); }

    private void reproducir(String archivo) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File(CARPETA + archivo)));
            clip.addLineListener(evento -> {
                if (evento.getType() == javax.sound.sampled.LineEvent.Type.STOP) clip.close();
            });
            clip.start();
        } catch (Exception excepcion) {
            // Los recursos de audio no deben impedir jugar.
        }
    }
}
