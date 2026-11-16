package idle.ui;

import idle.core.Game;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Un son de fond en boucle, joué tant que ce qu'il accompagne est à l'écran, et lu d'autant plus vite (donc plus aigu)
 * que ce qu'il accompagne va vite. La vitesse glisse et le son entre et sort en fondu : rien ne claque. Il joue sur son
 * propre fil, comme la {@link Music}, et se coupe seul après son fondu quand il n'a plus à jouer.
 *
 * <ul>
 * <li>{@link #DARK_MATTER} : le grondement de la matière noire qui s'agrandit (3,66 s, mono), de ×1 quand elle apparaît
 * à ×{@value #DARK_MATTER_MAX_RATE} à l'élan qu'elle a à la fin du premier univers ({@value #FULL_MOMENTUM_LOG} ordres
 * de grandeur), et pas plus vite ensuite ;</li>
 * <li>{@link #COLLIDER} : le fond du collisionneur (1,5 s, stéréo), sur son sous-onglet Énergie, à la vitesse des faisceaux : ×1 au départ, ×2 avec
 * les cinq niveaux de « Faisceaux rapides ».</li>
 * <li>{@link #EXPANSION} : le fond de l'expansion de la matière (4,1 s, stéréo), sur son sous-onglet du Big Bang, à
 * vitesse fixe.</li>
 * <li>{@link #BRANE} : la corde de la brane qu'on tire, tenue tant qu'on tire : plus on tire, plus elle est tendue (plus
 * aiguë) et forte ;</li>
 * <li>{@link #DURATION} : l'appui de la durée, une note tenue qui monte à mesure qu'on approche du plus long appui
 * possible ;</li>
 * <li>{@link #CHRONO} : le tic-tac du chrono, tant qu'il accélère le jeu, d'autant plus pressé que l'accélération est
 * forte.</li>
 * </ul>
 * Chaque boucle a sa fin fondue dans son début, et le niveau des sons répétitifs ({@link Sound#REPEATED_DB}).
 */
final class Hum {

    /** La vitesse de lecture du grondement au seuil : trois fois plus vite, une octave et une quinte plus haut. */
    static final double DARK_MATTER_MAX_RATE = 3;
    /** L'ordre de grandeur de l'élan au seuil : 10²⁴, l'élan juste avant que le premier univers se forme. */
    static final double FULL_MOMENTUM_LOG = 24;
    /** Le temps que met la vitesse à rejoindre la nouvelle, en secondes. */
    static final double GLIDE = 0.25;
    /** Le fondu d'entrée et de sortie, en secondes. */
    static final double FADE = 0.3;
    /** Les images (un échantillon par canal) de chaque bloc envoyé. */
    static final int BLOCK = 1_024;

    /** Une boucle : ses échantillons de -1 à 1 (canaux entrelacés), leur nombre de canaux et leur cadence ; vide si la ressource manque. */
    record Loop(float[] samples, int channels, float rate) {
        int frames() { return samples.length / channels; }

        /** Une boucle synthétisée de {@code seconds} secondes (mono), mise au niveau des sons répétitifs. */
        static Loop synthesized(double seconds, java.util.function.DoubleUnaryOperator wave) {
            int length = (int) Math.round(seconds * 44_100);
            short[] raw = new short[length];
            double peak = 0;
            double[] values = new double[length];
            for (int i = 0; i < length; i++) {
                values[i] = wave.applyAsDouble(i / 44_100.0);
                peak = Math.max(peak, Math.abs(values[i]));
            }
            for (int i = 0; i < length; i++) raw[i] = (short) Math.round(values[i] / Math.max(peak, 1e-9) * 0.9 * Short.MAX_VALUE);
            short[] leveled = Sound.leveled(raw, 1, Sound.REPEATED_DB);
            float[] samples = new float[length];
            for (int i = 0; i < length; i++) samples[i] = leveled[i] / 32768f;
            return new Loop(samples, 1, 44_100f);
        }

        /** La boucle {@code resource}, mise au niveau des sons répétitifs ; vide si elle manque (le jeu continue sans ce son). */
        static Loop load(String resource) {
            try (InputStream raw = Hum.class.getResourceAsStream(resource)) {
                if (raw == null) return new Loop(new float[0], 1, 44_100f);
                try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                    AudioFormat format = in.getFormat();
                    int channels = format.getChannels();
                    byte[] bytes = in.readAllBytes();
                    short[] read = new short[bytes.length / 2];
                    for (int i = 0; i < read.length; i++) read[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                    short[] leveled = Sound.leveled(read, channels, Sound.REPEATED_DB);
                    float[] samples = new float[leveled.length - leveled.length % channels];
                    for (int i = 0; i < samples.length; i++) samples[i] = leveled[i] / 32768f;
                    return new Loop(samples, channels, format.getSampleRate());
                }
            } catch (Exception unreadable) {
                return new Loop(new float[0], 1, 44_100f);
            }
        }
    }

    static final Loop DARK_MATTER = Loop.load("matiere-noire.wav");
    static final Loop COLLIDER = Loop.load("collisionneur.wav");
    static final Loop EXPANSION = Loop.load("expansion.wav");
    /**
     * La corde tendue de la brane : une note grave (la, 110 Hz) aux harmoniques de corde frottée, un frottement de crin
     * par-dessus. Une seconde exacte, cent dix périodes : la boucle se rejoint sans couture.
     */
    static final Loop BRANE = Loop.synthesized(1, t -> {
        double string = 0;
        for (int h = 1; h <= 12; h++) string += Math.sin(2 * Math.PI * 110 * h * t) / h * (h % 2 == 0 ? 0.6 : 1);
        double bow = 0.5 + 0.5 * Math.sin(2 * Math.PI * 6 * t);   // le crin qui accroche, six fois par seconde
        return 0.5 * string + 0.12 * bow * Math.sin(2 * Math.PI * 1760 * t) * Math.sin(2 * Math.PI * 2310 * t);
    });
    /** Le tic-tac du chrono : un tic aigu, un tac plus grave une demi-seconde après, sur une seconde exacte. */
    static final Loop CHRONO = Loop.synthesized(1, t -> {
        double tick = t < 0.5 ? t : t - 0.5;
        double pitch = t < 0.5 ? 2600 : 1700;
        return Math.exp(-tick * 90) * (Math.sin(2 * Math.PI * pitch * tick) + 0.4 * Math.sin(2 * Math.PI * pitch * 2.7 * tick));
    });

    /** La vitesse du tic-tac pour cette accélération du chrono : celle du jeu, de ×1 à ×3 au plus (au-delà, il bourdonnerait). */
    static double chronoRate(double speed) { return Double.isFinite(speed) ? Math.max(1, Math.min(3, speed)) : 1; }

    /** L'appui de la durée : une note douce (do, 262 Hz, avec son octave et sa quinte), qui se rejoint en une seconde exacte. */
    static final Loop DURATION = Loop.synthesized(1, t -> 0.6 * Math.sin(2 * Math.PI * 262 * t) + 0.25 * Math.sin(2 * Math.PI * 524 * t)
            + 0.15 * Math.sin(2 * Math.PI * 393 * t) * (0.6 + 0.4 * Math.sin(2 * Math.PI * 3 * t)));

    /** La vitesse du grondement pour cet élan : ×1 sans élan, ×{@value #DARK_MATTER_MAX_RATE} à 10²⁴ et au-delà, selon son ordre de grandeur. */
    static double darkMatterRate(double momentum) {
        double share = Math.log10(1 + momentum) / FULL_MOMENTUM_LOG;
        if (!(share > 0)) return 1;   // aucun élan, ou un élan illisible (NaN)
        return 1 + (DARK_MATTER_MAX_RATE - 1) * Math.min(1, share);
    }

    /** La vitesse du fond du collisionneur pour ce temps entre deux croisements : celle des faisceaux, ×1 au départ. */
    static double colliderRate(double period) {
        double rate = Game.RESONANCE_PERIOD / period;
        return rate > 0 && Double.isFinite(rate) ? rate : 1;
    }

    final Loop loop;
    private final String name;
    private volatile boolean wanted;
    private volatile double speed = 1;
    private volatile int volume;
    /** La force du son, de 0 à 1, en plus du volume : la tension de la corde. */
    private volatile double level = 1;
    private volatile boolean broken;
    /** Le fil du son, créé quand il doit jouer ; visible des essais. */
    Thread player;
    /** Où partent les blocs : la sortie audio, ou un relevé pour les essais. */
    Consumer<byte[]> sink = this::output;
    private SourceDataLine line;

    /** @param name le nom du fil, pour s'y retrouver */
    Hum(Loop loop, String name) {
        this.loop = loop;
        this.name = name;
    }

    /**
     * À chaque image : si ce qu'il accompagne est à l'écran et en marche, sa vitesse de lecture, l'interrupteur et le
     * volume (0 à 100). Ne bloque jamais : le fil démarre quand le son doit jouer, et s'arrête après son fondu.
     */
    void update(boolean playing, double speed, boolean enabled, int volume) { update(playing, speed, enabled, volume, 1); }

    /** Comme {@link #update(boolean, double, boolean, int)}, avec une force de 0 à 1 qui glisse d'un bloc au suivant. */
    void update(boolean playing, double speed, boolean enabled, int volume, double level) {
        this.level = Double.isFinite(level) ? Math.max(0, Math.min(1, level)) : 1;
        this.speed = speed;
        this.volume = Math.max(0, Math.min(100, volume));
        wanted = playing && enabled && volume > 0 && loop.frames() > 0 && !broken;
        if (wanted && (player == null || !player.isAlive())) {
            player = new Thread(this::run, name);
            player.setDaemon(true);
            player.start();
        }
    }

    private void run() {
        Reader reader = new Reader(loop);
        double fade = 0;
        double was = level;
        while (!broken) {
            boolean on = wanted;
            if (!on && fade <= 0) break;
            double step = BLOCK / loop.rate() / FADE;
            double next = on ? Math.min(1, fade + step) : Math.max(0, fade - step);
            // Le volume en carré, comme les autres sons ; la force glisse d'un bloc au suivant, sans à-coup.
            double gain = volume / 100.0, now = level;
            sink.accept(Music.pcm(reader.read(BLOCK, speed), fade * gain * gain * was, next * gain * gain * now));
            fade = next;
            was = now;
        }
        if (line != null) {
            line.drain();
            line.close();
            line = null;
        }
    }

    /** La lecture d'une boucle à vitesse glissante : une position fractionnaire, interpolée entre deux images. */
    static final class Reader {
        private final Loop loop;
        private double position;
        private double speed = 1;

        Reader(Loop loop) { this.loop = loop; }

        /** Les {@code frames} images suivantes (canaux entrelacés), la vitesse glissant vers {@code target} (en {@value #GLIDE} s environ). */
        float[] read(int frames, double target) {
            int channels = loop.channels();
            int length = loop.frames();
            float[] samples = loop.samples();
            float[] out = new float[frames * channels];
            double pull = Math.min(1, 1 / (GLIDE * loop.rate()));
            for (int i = 0; i < frames; i++) {
                speed += (target - speed) * pull;
                int at = (int) position;
                int after = (at + 1) % length;
                double frac = position - at;
                for (int c = 0; c < channels; c++) {
                    out[i * channels + c] = (float) (samples[at * channels + c] * (1 - frac) + samples[after * channels + c] * frac);
                }
                position = (position + speed) % length;
            }
            return out;
        }

        double speed() { return speed; }
    }

    private void output(byte[] pcm) {
        try {
            if (line == null) {
                AudioFormat format = new AudioFormat(loop.rate(), 16, loop.channels(), true, false);
                line = AudioSystem.getSourceDataLine(format);
                line.open(format, BLOCK * 2 * loop.channels() * 4);
                line.start();
            }
            line.write(pcm, 0, pcm.length);
        } catch (Exception | LinkageError noAudio) {
            // Pas de carte son, sortie occupée : la partie continue sans ce son.
            broken = true;
        }
    }
}
