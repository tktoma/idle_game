package idle.ui;

import java.io.InputStream;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Le grondement de la matière noire qui s'agrandit : une boucle de 3,66 s ({@code matiere-noire.wav}, sa fin fondue
 * dans son début), jouée tant qu'elle grossit sous les yeux du joueur. Elle est lue d'autant plus vite, et donc plus
 * aiguë, que l'élan de la croissance ({@link idle.core.Game#darkMatterMomentum()}) est grand : de ×1 quand la matière
 * noire apparaît à ×{@value #MAX_RATE} à l'élan qu'elle a à la fin du premier univers ({@value #FULL_MOMENTUM_LOG}
 * ordres de grandeur), et pas plus vite ensuite. La vitesse glisse et le son entre et sort en fondu : rien ne claque.
 *
 * <p>Il joue sur son propre fil, comme la {@link Music}, et se coupe seul après son fondu quand il n'a plus à jouer.
 */
final class DarkMatterHum {

    /** La vitesse de lecture au seuil : trois fois plus vite, une octave et une quinte plus haut. */
    static final double MAX_RATE = 3;
    /** L'ordre de grandeur de l'élan au seuil : 10²⁴, l'élan juste avant que le premier univers se forme. */
    static final double FULL_MOMENTUM_LOG = 24;
    /** Le temps que met la vitesse à rejoindre la nouvelle, en secondes. */
    static final double GLIDE = 0.25;
    /** Le fondu d'entrée et de sortie, en secondes. */
    static final double FADE = 0.3;
    static final float RATE = 22_050f;
    static final int BLOCK = 1_024;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    /** La boucle, en échantillons de -1 à 1, au niveau des sons répétitifs ; vide si la ressource manque (le jeu continue sans ce son). */
    static final float[] LOOP = load();

    private volatile boolean wanted;
    private volatile double momentum;
    private volatile int volume;
    private volatile boolean broken;
    /** Le fil du son, créé quand il doit jouer ; visible des essais. */
    Thread player;
    /** Où partent les blocs : la sortie audio, ou un relevé pour les essais. */
    Consumer<byte[]> sink = this::output;
    private SourceDataLine line;

    /** La vitesse de lecture pour cet élan : ×1 sans élan, ×{@value #MAX_RATE} à 10²⁴ et au-delà, selon son ordre de grandeur. */
    static double rate(double momentum) {
        double share = Math.log10(1 + momentum) / FULL_MOMENTUM_LOG;
        if (!(share > 0)) return 1;   // aucun élan, ou un élan illisible (NaN)
        return 1 + (MAX_RATE - 1) * Math.min(1, share);
    }

    /**
     * À chaque image : si la matière noire grossit à l'écran, son élan, l'interrupteur et le volume (0 à 100). Ne bloque
     * jamais : le fil démarre quand le son doit jouer, et s'arrête après son fondu.
     */
    void update(boolean growing, double momentum, boolean enabled, int volume) {
        this.momentum = momentum;
        this.volume = Math.max(0, Math.min(100, volume));
        wanted = growing && enabled && volume > 0 && LOOP.length > 0 && !broken;
        if (wanted && (player == null || !player.isAlive())) {
            player = new Thread(this::run, "matière noire");
            player.setDaemon(true);
            player.start();
        }
    }

    private void run() {
        Reader reader = new Reader();
        double fade = 0;
        while (!broken) {
            boolean on = wanted;
            if (!on && fade <= 0) break;
            double step = BLOCK / RATE / FADE;
            double next = on ? Math.min(1, fade + step) : Math.max(0, fade - step);
            // Le volume en carré, comme les autres sons.
            double level = volume / 100.0;
            sink.accept(Music.pcm(reader.read(BLOCK, rate(momentum)), fade * level * level, next * level * level));
            fade = next;
        }
        if (line != null) {
            line.drain();
            line.close();
            line = null;
        }
    }

    /** La lecture de la boucle à vitesse glissante : une position fractionnaire, interpolée entre deux échantillons. */
    static final class Reader {
        private double position;
        private double speed = 1;

        /** Le bloc suivant, la vitesse glissant vers {@code target} (en {@value #GLIDE} s environ). */
        float[] read(int length, double target) {
            float[] out = new float[length];
            double pull = Math.min(1, 1 / (GLIDE * RATE));
            for (int i = 0; i < length; i++) {
                speed += (target - speed) * pull;
                int at = (int) position;
                double frac = position - at;
                out[i] = (float) (LOOP[at] * (1 - frac) + LOOP[(at + 1) % LOOP.length] * frac);
                position = (position + speed) % LOOP.length;
            }
            return out;
        }

        double speed() { return speed; }
    }

    private void output(byte[] pcm) {
        try {
            if (line == null) {
                line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, BLOCK * 2 * 4);
                line.start();
            }
            line.write(pcm, 0, pcm.length);
        } catch (Exception | LinkageError noAudio) {
            // Pas de carte son, sortie occupée : la partie continue sans ce son.
            broken = true;
        }
    }

    private static float[] load() {
        try (InputStream raw = DarkMatterHum.class.getResourceAsStream("matiere-noire.wav")) {
            if (raw == null) return new float[0];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                byte[] bytes = in.readAllBytes();
                short[] read = new short[bytes.length / 2];
                for (int i = 0; i < read.length; i++) read[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                short[] leveled = Sound.leveled(read, 1, Sound.REPEATED_DB);   // au niveau des sons répétitifs
                float[] samples = new float[leveled.length];
                for (int i = 0; i < samples.length; i++) samples[i] = leveled[i] / 32768f;
                return samples;
            }
        } catch (Exception unreadable) {
            return new float[0];
        }
    }
}
