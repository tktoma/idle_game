package idle.ui;

import static idle.ui.Sound.envelope;
import static idle.ui.Sound.noise;
import static idle.ui.Sound.render;
import static idle.ui.Sound.sine;

import idle.core.universe.Skin;
import idle.ui.Sound.Cue;

/**
 * Les timbres des skins des sons (version 0.200), synthétisés comme le Classique : quelques formes d'onde (sinus, carré,
 * dents de scie), des enveloppes et un bruit lissé, sans fichier audio.
 *
 * <p>La banque de sons change le timbre de tous les sons : « Bois » (xylophone et claves), « Verre » (carillon),
 * « 8 bits » (ondes carrées), « Orchestre » (cordes), « Nature » (gouttes, vent, oiseaux), « Silence partiel » (seuls
 * les Big Bangs sonnent). Le son du Big Bang (« Carillon géant », « Souffle de vent », « Orgue ») et le tic du métronome
 * (« Bois », « Métal », « Goutte d'eau », « Battement de cœur ») ont chacun leur skin, qui passe devant la banque.
 */
final class SoundBanks {

    /** Une voix : l'amplitude d'une note de cette fréquence, {@code t} secondes après son départ, pour une note de cette durée. */
    private interface Voice {
        double tone(double frequency, double t, double length);
    }

    /** Un son dans les skins choisis ({@code null} = Classique) ; vide pour un son que le silence partiel coupe. */
    static short[] synthesize(Cue cue, Skin bank, Skin bang, Skin metronome) {
        if (cue == Cue.BIG_BANG && bang != null) return bigBang(bang);
        if (cue == Cue.BEAT && metronome != null) return beat(metronome);
        if (bank == null) return Sound.synthesize(cue);
        return switch (bank) {
            case SOUND_SILENCE -> cue == Cue.BIG_BANG ? Sound.synthesize(cue) : new short[0];
            case SOUND_WOOD -> wood(cue);
            case SOUND_GLASS -> glass(cue);
            case SOUND_BITS -> bits(cue);
            case SOUND_ORCHESTRA -> orchestra(cue);
            case SOUND_NATURE -> nature(cue);
            default -> Sound.synthesize(cue);
        };
    }

    // ----- Les banques -----

    /** Bois : une attaque sèche, une décroissance rapide, un partiel impair comme une lame de xylophone. */
    private static final Voice WOOD = (f, t, length) -> envelope(t, 0.001, length) * Math.exp(-t * 14)
            * (0.45 * sine(f, t) + 0.2 * sine(3 * f, t) * Math.exp(-t * 40) + 0.15 * noise(t) * Math.exp(-t * 300));

    private static short[] wood(Cue cue) {
        return switch (cue) {
            // Un roulement de marimba qui descend, du la 220 au la 110.
            case BIG_BANG -> render(2.0, t -> {
                double sum = 0;
                for (int hit = 0; hit < 22; hit++) {
                    double start = hit * 0.09;
                    if (t < start) break;
                    sum += 0.7 * WOOD.tone(220 * Math.pow(0.5, hit / 21.0), t - start, 2.0 - start);
                }
                return sum * envelope(t, 0.01, 2.0);
            });
            // Trois claves en grappe sur un coup de caisse en bois.
            case EXPLOSION -> render(0.6, t -> 0.8 * (sequence(t, 0.065, 0.6, WOOD, 2500, 2100, 1800))
                    + envelope(t, 0.002, 0.6) * Math.exp(-t * 9) * 0.5 * sine(120 * (1 + 0.3 * Math.exp(-t * 20)), t));
            // Un bloc de bois, plus grave et plus rond que le tic Classique.
            case BEAT -> render(0.08, t -> envelope(t, 0.001, 0.08) * Math.exp(-t * 35) * (0.55 * sine(900, t) + 0.25 * sine(1350, t)));
            default -> melody(cue, WOOD, 0.12);
        };
    }

    /** Verre : une octave plus haut, des partiels inharmoniques et une longue résonance de carillon. */
    private static final Voice GLASS = (f, t, length) -> envelope(t, 0.002, length) * Math.exp(-t * 3.5)
            * (0.35 * sine(2 * f, t) + 0.18 * sine(2 * f * 2.32, t) * Math.exp(-t * 5) + 0.1 * sine(2 * f * 4.25, t) * Math.exp(-t * 8));

    private static short[] glass(Cue cue) {
        return switch (cue) {
            case BIG_BANG -> render(3.0, t -> bell(110, t, 3.0) + 0.6 * bell(146.8, Math.max(0, t - 0.4), 2.6) + 0.5 * bell(220, Math.max(0, t - 0.8), 2.2));
            // Un bris : un bruit clair qui s'éteint, et des éclats qui tintent à des hauteurs dépareillées.
            case EXPLOSION -> render(0.8, t -> envelope(t, 0.001, 0.8) * (0.4 * noise(t) * noise(t * 1.37) * Math.exp(-t * 9)
                    + 0.6 * sequence(t, 0.04, 0.8, GLASS, 1480, 2217, 1865, 2794)));
            case BEAT -> render(0.12, t -> envelope(t, 0.001, 0.12) * Math.exp(-t * 30) * (0.45 * sine(3136, t) + 0.2 * sine(3136 * 2.32, t)));
            default -> melody(cue, GLASS, 0.1);
        };
    }

    /** Une cloche grave : la fondamentale et ses partiels de cloche, chacun avec sa propre décroissance. */
    private static double bell(double f, double t, double length) {
        return envelope(t, 0.003, length) * (0.3 * sine(f, t) * Math.exp(-t * 0.8) + 0.2 * sine(f * 2.0, t) * Math.exp(-t * 1.2)
                + 0.15 * sine(f * 2.76, t) * Math.exp(-t * 1.8) + 0.1 * sine(f * 4.07, t) * Math.exp(-t * 2.5) + 0.06 * sine(f * 5.4, t) * Math.exp(-t * 3.5));
    }

    /** 8 bits : des ondes carrées nues, une attaque et une fin nettes. */
    private static final Voice BITS = (f, t, length) -> envelope(t, 0.002, length) * (0.3 * square(f, t) + 0.08 * square(2 * f, t));

    private static short[] bits(Cue cue) {
        return switch (cue) {
            // Un carré qui plonge du la 440 au la 55, sur un bruit à quatre niveaux.
            case BIG_BANG -> render(1.8, t -> envelope(t, 0.005, 1.8)
                    * (0.4 * square(1, 55 * t + 385 * (1 - Math.exp(-2 * t)) / 2) + 0.2 * Math.round(noise(t) * 2) / 2.0 * Math.exp(-t * 2.5)));
            case EXPLOSION -> render(0.6, t -> envelope(t, 0.002, 0.6) * (0.4 * Math.signum(noise(t)) * Math.exp(-t * 7)
                    + 0.3 * square(1, 60 * t + 50 * (1 - Math.exp(-8 * t)) / 8)));
            case BEAT -> render(0.06, t -> envelope(t, 0.001, 0.06) * 0.35 * square(1568, t));
            default -> melody(cue, BITS, 0.11);
        };
    }

    /** Orchestre : des cordes en dents de scie, une attaque lente et un vibrato. */
    private static final Voice STRINGS = (f, t, length) -> envelope(t, 0.08, length) * 0.3 * saw(f * (1 + 0.004 * Math.sin(2 * Math.PI * 5.5 * t)), t);

    private static short[] orchestra(Cue cue) {
        return switch (cue) {
            // Un accord grave qui enfle puis s'éteint, en trémolo.
            case BIG_BANG -> render(2.5, t -> {
                double swell = Math.min(1, t / 0.6) * envelope(t, 0.01, 2.5) * (0.85 + 0.15 * Math.sin(2 * Math.PI * 7 * t));
                return swell * 0.22 * (saw(55, t) + saw(82.4, t) + saw(110, t) + 0.6 * saw(164.8, t));
            });
            // Une timbale et un pizzicato grave.
            case EXPLOSION -> render(0.9, t -> envelope(t, 0.003, 0.9) * (0.5 * sine(1, 70 * t + 50 * (1 - Math.exp(-6 * t)) / 6) * Math.exp(-t * 3)
                    + 0.25 * saw(110, t) * Math.exp(-t * 12)));
            case BEAT -> render(0.1, t -> envelope(t, 0.001, 0.1) * Math.exp(-t * 30) * 0.45 * saw(784, t));
            default -> melody(cue, STRINGS, 0.14);
        };
    }

    /** Nature : des gouttes, une hauteur qui glisse vers le bas en s'éteignant. */
    private static final Voice DROP = (f, t, length) -> envelope(t, 0.002, length) * Math.exp(-t * 10) * 0.5 * glide(f, 0.5, 35, t);
    /** Un pépiement : une hauteur qui monte vite, en tremblant. */
    private static final Voice BIRD = (f, t, length) -> envelope(t, 0.004, length) * Math.exp(-t * 8) * (0.6 + 0.4 * Math.sin(2 * Math.PI * 40 * t))
            * 0.35 * glide(f, -0.4, 30, t);

    private static short[] nature(Cue cue) {
        return switch (cue) {
            // Une rafale de vent sur un grondement d'orage.
            case BIG_BANG -> render(2.4, t -> {
                double gust = Math.pow(Math.sin(Math.PI * t / 2.4), 2);
                return gust * (0.5 * wind(t) + 0.25 * sine(40, t) * Math.exp(-t * 1.5));
            });
            // Une grosse goutte qui s'écrase : la goutte, puis l'éclaboussure.
            case EXPLOSION -> render(0.8, t -> envelope(t, 0.002, 0.8) * (0.5 * glide(400, 1.2, 12, t) * Math.exp(-t * 6) + 0.35 * wind(t) * Math.exp(-t * 5)));
            case COMET -> render(0.7, t -> sequence(t, 0.16, 0.7, BIRD, 2000, 2400, 2000, 2800));
            case BEAT -> render(0.12, t -> envelope(t, 0.001, 0.12) * Math.exp(-t * 25) * 0.5 * glide(900, 1.0, 40, t));
            default -> melody(cue, DROP, 0.13);
        };
    }

    // ----- Le son du Big Bang -----

    private static short[] bigBang(Skin bang) {
        return switch (bang) {
            // Trois grandes cloches, la grave d'abord.
            case BANG_SOUND_CHIME -> render(3.2, t -> 1.2 * bell(82.4, t, 3.2) + 0.9 * bell(123.5, Math.max(0, t - 0.5), 2.7) + 0.7 * bell(164.8, Math.max(0, t - 1.0), 2.2));
            // Un souffle qui monte et retombe, avec un sifflement ténu.
            case BANG_SOUND_WIND -> render(2.8, t -> {
                double gust = Math.pow(Math.sin(Math.PI * t / 2.8), 1.5);
                return gust * (0.6 * wind(t) + 0.08 * sine(600 + 80 * Math.sin(2 * Math.PI * 0.7 * t), t));
            });
            // Un accord d'orgue : des tuyaux riches en harmoniques, qui s'ouvrent puis se referment.
            case BANG_SOUND_ORGAN -> render(2.6, t -> {
                double open = Math.min(1, t / 0.15) * Math.min(1, Math.max(0, (2.6 - t) / 0.8));
                return open * 0.2 * (pipe(65.4, t) + pipe(98, t) + pipe(130.8, t) + 0.7 * pipe(196, t));
            });
            default -> Sound.synthesize(Cue.BIG_BANG);
        };
    }

    /** Un tuyau d'orgue : la fondamentale et ses harmoniques, en amplitudes décroissantes. */
    private static double pipe(double f, double t) {
        return sine(f, t) + 0.6 * sine(2 * f, t) + 0.4 * sine(3 * f, t) + 0.3 * sine(4 * f, t) + 0.2 * sine(6 * f, t) + 0.15 * sine(8 * f, t);
    }

    // ----- Le métronome -----

    private static short[] beat(Skin metronome) {
        return switch (metronome) {
            case METRONOME_WOOD -> render(0.08, t -> envelope(t, 0.001, 0.08) * Math.exp(-t * 40) * (0.55 * sine(800, t) + 0.25 * sine(1200, t)));
            case METRONOME_METAL -> render(0.15, t -> envelope(t, 0.001, 0.15) * Math.exp(-t * 18) * (0.4 * sine(3200, t) + 0.25 * sine(3200 * 1.47, t)));
            case METRONOME_DROP -> render(0.12, t -> envelope(t, 0.001, 0.12) * Math.exp(-t * 22) * 0.5 * glide(800, 1.25, 30, t));
            // Deux coups sourds, le second plus faible : « poum-poum ».
            case METRONOME_HEART -> render(0.3, t -> envelope(t, 0.002, 0.3)
                    * (0.6 * glide(55, 0.6, 15, t) * Math.exp(-t * 20) + (t < 0.14 ? 0 : 0.4 * glide(50, 0.6, 15, t - 0.14) * Math.exp(-(t - 0.14) * 20))));
            default -> Sound.synthesize(Cue.BEAT);
        };
    }

    // ----- Les outils -----

    /** Les sons mélodiques, aux notes du Classique, joués par la voix d'une banque ; {@code step} sépare les notes. */
    private static short[] melody(Cue cue, Voice voice, double step) {
        return switch (cue) {
            case VICTORY -> render(0.75, t -> sequence(t, step, 0.75, voice, 523.25, 659.25, 783.99));
            case ACHIEVEMENT -> render(0.55, t -> sequence(t, step, 0.55, voice, 659.25, 987.77));
            // Un trille, à la place du scintillement.
            case COMET -> render(0.7, t -> sequence(t, step / 2, 0.7, voice, 1760, 2093, 2637, 2093, 1760));
            case ELEMENT -> render(0.6, t -> voice.tone(880, t, 0.6));
            case LEVEL -> render(0.18, t -> voice.tone(1046.5, t, 0.18));
            default -> throw new IllegalArgumentException("pas un son mélodique : " + cue);
        };
    }

    /** Des notes l'une après l'autre, chacune tenue jusqu'à la fin, par une voix. */
    private static double sequence(double t, double step, double length, Voice voice, double... frequencies) {
        double sum = 0;
        for (int i = 0; i < frequencies.length; i++) {
            double start = i * step;
            if (t < start) break;
            sum += voice.tone(frequencies[i], t - start, length - start);
        }
        return sum;
    }

    /** Une onde carrée. */
    private static double square(double frequency, double t) {
        return sine(frequency, t) >= 0 ? 1 : -1;
    }

    /** Une dent de scie approchée : six harmoniques en amplitudes 1/n. */
    private static double saw(double frequency, double t) {
        double sum = 0;
        for (int n = 1; n <= 6; n++) sum += sine(n * frequency, t) / n;
        return sum * 0.6;
    }

    /**
     * Une hauteur qui glisse : de {@code f × (1 + amount)} vers {@code f}, à la vitesse {@code k} ; {@code amount} négatif
     * monte. La phase est intégrée pour que le glissement reste pur.
     */
    private static double glide(double f, double amount, double k, double t) {
        return sine(1, f * (t + amount * (1 - Math.exp(-k * t)) / k));
    }

    /** Un vent : un bruit lissé, deux octaves de bruit de valeur interpolé, loin du bruit blanc. */
    private static double wind(double t) {
        return 0.7 * smooth(t, 300) + 0.4 * smooth(t, 1100);
    }

    /** Un bruit de valeur : le bruit tiré sur une grille de {@code grid} points par seconde, interpolé en douceur entre deux. */
    private static double smooth(double t, double grid) {
        double x = t * grid;
        double cell = Math.floor(x);
        double frac = x - cell;
        double a = noise(cell / 44_100.0), b = noise((cell + 1) / 44_100.0);
        double eased = frac * frac * (3 - 2 * frac);
        return a + (b - a) * eased;
    }

    private SoundBanks() {}
}
