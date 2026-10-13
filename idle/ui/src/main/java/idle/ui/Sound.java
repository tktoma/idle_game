package idle.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 * Les sons du jeu, tous synthétisés à la volée : aucun fichier. Chaque son est une courte somme
 * de sinusoïdes (et d'un peu de bruit pour les explosions) sous une enveloppe, calculée une fois
 * puis rejouée au volume du moment.
 *
 * <p>Les sons suivent les annonces ({@link Notifier}) : un succès, un élément nouveau, une
 * explosion, un Big Bang… Pour ne pas assourdir quand les automatismes enchaînent tout, un
 * même son ne revient pas avant {@link #SAME_CUE_GAP} secondes, deux sons ne se suivent pas à
 * moins de {@link #ANY_CUE_GAP} secondes, et d'une fournée d'annonces seul le son le plus
 * important est joué.
 *
 * <p>Sans carte son, ou si le système refuse la sortie audio, le jeu reste muet sans erreur :
 * le premier échec coupe les sons jusqu'à la fermeture.
 */
final class Sound {

    /** Un son, du plus important au moins important : l'ordre décide lequel jouer d'une fournée. */
    enum Cue {
        BIG_BANG, EXPLOSION, VICTORY, ACHIEVEMENT, COMET, ELEMENT, LEVEL;
    }

    static final double SAME_CUE_GAP = 0.8;
    static final double ANY_CUE_GAP = 0.12;

    private static final float RATE = 44_100f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    private final Map<Cue, short[]> samples = new EnumMap<>(Cue.class);
    private final Map<Cue, Long> lastPlayed = new EnumMap<>(Cue.class);
    private long lastAny = Long.MIN_VALUE / 2;
    private volatile boolean broken = false;
    private ExecutorService player;
    /** Où partent les échantillons : la sortie audio, ou un relevé pour les essais. */
    java.util.function.Consumer<byte[]> sink = this::output;

    /** Le son d'une annonce, ou {@code null} si elle n'en a pas. */
    static Cue cueOf(String text) {
        if (text.startsWith("Big Bang")) return Cue.BIG_BANG;
        if (text.startsWith("Explosion")) return Cue.EXPLOSION;
        if (text.startsWith("Défi réussi") || text.startsWith("Record battu")
                || (text.startsWith("Défi de Big Bang") && text.contains("réussi"))) return Cue.VICTORY;
        if (text.startsWith("Succès")) return Cue.ACHIEVEMENT;
        if (text.startsWith("Une comète")) return Cue.COMET;
        if (text.startsWith("Nouvel élément") || text.startsWith("Élément unique")) return Cue.ELEMENT;
        if (text.startsWith("Collision") && text.contains("quark top")) return Cue.COMET;
        if (text.startsWith("Palier du collisionneur")) return Cue.ACHIEVEMENT;
        if (text.startsWith("Astre mûr") || text.contains("astres mûrs")) return Cue.ELEMENT;
        if (text.startsWith("Sonde") || text.contains("sondes revenues")) return Cue.COMET;
        if (text.startsWith("Palier de vitesse") || text.startsWith("Collection") || text.startsWith("Collision")) return Cue.LEVEL;
        return null;
    }

    /** Le son le plus important d'une fournée d'annonces, ou {@code null}. */
    static Cue strongest(List<String> news) {
        Cue best = null;
        for (String text : news) {
            Cue cue = cueOf(text);
            if (cue != null && (best == null || cue.ordinal() < best.ordinal())) best = cue;
        }
        return best;
    }

    /**
     * Joue le son le plus important de ces annonces, au volume réglé (0 à 100), si les sons sont
     * actifs et que le rythme le permet. Ne bloque jamais : la lecture se fait sur un fil à part.
     */
    void announce(List<String> news, boolean enabled, int volume) {
        if (!enabled || volume <= 0 || broken || news.isEmpty()) return;
        Cue cue = strongest(news);
        if (cue != null) play(cue, volume);
    }

    /** Joue un son, si le rythme le permet. Vrai s'il est parti. */
    boolean play(Cue cue, int volume) {
        if (broken || volume <= 0) return false;
        long now = System.nanoTime();
        if (now - lastAny < ANY_CUE_GAP * 1e9) return false;
        Long last = lastPlayed.get(cue);
        if (last != null && now - last < SAME_CUE_GAP * 1e9) return false;
        lastAny = now;
        lastPlayed.put(cue, now);
        byte[] pcm = pcm(samples.computeIfAbsent(cue, Sound::synthesize), Math.min(100, volume) / 100.0);
        if (player == null) {
            player = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "sons");
                thread.setDaemon(true);
                return thread;
            });
        }
        player.execute(() -> sink.accept(pcm));
        return true;
    }

    /** Le son d'essai du réglage du volume : joué même s'il vient de l'être. */
    void test(int volume) {
        lastPlayed.remove(Cue.ACHIEVEMENT);
        lastAny = Long.MIN_VALUE / 2;
        play(Cue.ACHIEVEMENT, volume);
    }

    /** Vrai tant que la sortie audio n'a pas échoué. */
    boolean available() {
        return !broken;
    }

    private void output(byte[] pcm) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) clip.close();
            });
            clip.open(FORMAT, pcm, 0, pcm.length);
            clip.start();
        } catch (Exception | LinkageError noAudio) {
            // Pas de carte son, sortie occupée, système sans audio : le jeu continue en silence.
            broken = true;
        }
    }

    /** Les échantillons, mis au volume voulu, en octets 16 bits petit-boutistes. */
    static byte[] pcm(short[] samples, double volume) {
        // Une courbe en carré : la moitié du curseur sonne comme la moitié du volume.
        double gain = volume * volume;
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int value = (int) Math.round(samples[i] * gain);
            out[2 * i] = (byte) value;
            out[2 * i + 1] = (byte) (value >> 8);
        }
        return out;
    }

    /** Fabrique un son. Les sons restent sous 70 % de la pleine échelle : jamais de saturation. */
    static short[] synthesize(Cue cue) {
        return switch (cue) {
            // Un grondement qui descend, une longue traîne.
            case BIG_BANG -> render(2.2, t -> {
                double pitch = 55 + 90 * Math.exp(-t * 2.2);
                return envelope(t, 0.02, 2.2) * (0.6 * sine(pitch, t) + 0.3 * sine(pitch * 1.5, t) + 0.25 * noise(t) * Math.exp(-t * 3));
            });
            // Un souffle bref sur une note grave.
            case EXPLOSION -> render(0.9, t -> envelope(t, 0.005, 0.9)
                    * (0.55 * noise(t) * Math.exp(-t * 6) + 0.5 * sine(80 + 60 * Math.exp(-t * 8), t)));
            // Trois notes qui montent : do, mi, sol.
            case VICTORY -> render(0.75, t -> notes(t, 0.12, 0.75, 523.25, 659.25, 783.99));
            // Deux notes claires.
            case ACHIEVEMENT -> render(0.55, t -> notes(t, 0.11, 0.55, 659.25, 987.77));
            // Un scintillement aigu.
            case COMET -> render(0.7, t -> envelope(t, 0.01, 0.7)
                    * (0.35 * sine(1760 + 400 * Math.sin(t * 30), t) + 0.25 * sine(2637, t) * Math.exp(-t * 4)));
            // Une cloche : une fondamentale et un partiel inharmonique.
            case ELEMENT -> render(0.6, t -> envelope(t, 0.004, 0.6) * (0.5 * sine(880, t) + 0.25 * sine(880 * 2.76, t) * Math.exp(-t * 5)));
            // Un petit bip doux.
            case LEVEL -> render(0.18, t -> envelope(t, 0.005, 0.18) * 0.45 * sine(1046.5, t));
        };
    }

    private interface Wave {
        double at(double seconds);
    }

    private static short[] render(double seconds, Wave wave) {
        short[] out = new short[(int) (seconds * RATE)];
        for (int i = 0; i < out.length; i++) {
            double value = Math.max(-0.7, Math.min(0.7, wave.at(i / RATE)));
            out[i] = (short) Math.round(value * Short.MAX_VALUE);
        }
        return out;
    }

    /** Attaque linéaire puis décroissance jusqu'au silence à la fin. */
    private static double envelope(double t, double attack, double length) {
        if (t < attack) return t / attack;
        double rest = Math.max(0, 1 - (t - attack) / (length - attack));
        return rest * rest;
    }

    private static double sine(double frequency, double t) {
        return Math.sin(2 * Math.PI * frequency * t);
    }

    /** Un bruit déterministe : le même son à chaque fois. */
    private static double noise(double t) {
        long n = (long) (t * RATE) * 2_654_435_761L;
        n ^= n >>> 13;
        n *= 0x5bd1e995L;
        n ^= n >>> 15;
        return ((n & 0xffff) / 32_767.5) - 1;
    }

    /** Des notes l'une après l'autre, chacune tenue jusqu'à la fin. */
    private static double notes(double t, double step, double length, double... frequencies) {
        double sum = 0;
        for (int i = 0; i < frequencies.length; i++) {
            double start = i * step;
            if (t < start) break;
            double local = t - start;
            sum += 0.3 * envelope(local, 0.006, length - start) * (sine(frequencies[i], local) + 0.3 * sine(2 * frequencies[i], local));
        }
        return sum;
    }
}
