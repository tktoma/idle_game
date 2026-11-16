package idle.ui;

import java.util.*;
import idle.core.universe.Skin;
import java.util.function.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/**
 * Les sons du jeu, tous synthétisés à la volée : aucun fichier. Chaque son est une courte somme de sinusoïdes (et d'un
 * peu de bruit pour les explosions) sous une enveloppe, calculée une fois puis rejouée au volume du moment.
 *
 * <p>Les sons suivent les annonces ({@link Notifier}) : un succès, un élément nouveau, une explosion, un Big Bang… Pour
 * ne pas assourdir quand les automatismes enchaînent tout, un même son ne revient pas avant {@link #SAME_CUE_GAP}
 * secondes, deux sons ne se suivent pas à moins de {@link #ANY_CUE_GAP} secondes, et d'une fournée d'annonces seul le
 * son le plus important est joué.
 *
 * <p>Le métronome de la boucle a son tic à part ({@link #beat(int)}) : il bat en rythme, plusieurs fois par seconde,
 * sans attendre ni retarder les sons des annonces.
 *
 * <p>Les skins des sons ({@link SoundBanks}) changent le timbre de tous les sons (la banque), celui du Big Bang et le
 * tic du métronome, toujours sans fichier audio.
 *
 * <p>Des sons pour les moments qui n'en avaient pas (univers formé, palier du défi final, skin débloqué, défi qui
 * s'ouvre ou commence, matière noire, jalons), et des sons d'interface qui ne passent pas par les annonces : l'achat ou
 * l'interrupteur d'une carte ({@link #click(Cue, int)}) et le geste du joueur dans le mini-jeu d'une dimension ({@link
 * #gesture(int, int)}), un timbre par dimension. Ceux-là ne comptent pas dans le rythme des annonces, comme le
 * métronome. La musique de fond est à part ({@link Music}).
 *
 * <p>Des bruitages pour ce qui se passe en jouant, relevés image par image ({@link SoundEvents}) : une particule créée,
 * une fusion, une collision, des hadrons, le vide, la graine (arrosage et palier), un point gagné dans une dimension,
 * un clic sur un bouton, un changement d'onglet. Chaque son appartient à une catégorie ({@link SoundKind}), que les
 * réglages gardent ou coupent.
 *
 * <p>Sans carte son, ou si le système refuse la sortie audio, le jeu reste muet sans erreur : le premier échec coupe
 * les sons jusqu'à la fermeture.
 */
final class Sound {
    /** Un son, du plus important au moins important : l'ordre décide lequel jouer d'une fournée. */
    enum Cue {
        BIG_BANG,
        /** Version 0.210 : un univers formé, et la fin de la partie. */
        UNIVERSE,
        /** Version 0.210 : un palier du défi final. */
        FINALE,
        EXPLOSION, VICTORY, ACHIEVEMENT,
        /** Version 0.210 : un skin débloqué. */
        SKIN,
        /** Version 0.210 : un défi qui s'ouvre ou qui commence. */
        CHALLENGE,
        /** La matière noire (Noyaux sombres à portée, un automatisme de matière noire, un palier de taille). */
        DARK,
        COMET, ELEMENT,
        /** Version 0.210 : un jalon (du cosmos, des gluons, des racines, un ensemble, un titre, le premier atome). */
        MILESTONE,
        LEVEL,
        /** Version 0.210 : un achat à la main, sur une carte à portée ; aucune annonce ne le joue. */
        PURCHASE,
        /** Version 0.210 : une carte qui s'allume ou s'éteint ; aucune annonce ne le joue. */
        TOGGLE,
        /** Une particule créée, un atome (une fusion), une collision, des hadrons, une paire du vide. */
        PARTICLE, ATOM, COLLISION, HADRON, VOID,
        /** Version 0.213 : un palier de la graine, un arrosage. */
        SEED, WATER,
        /** Version 0.213 : un clic sur un bouton, un changement d'onglet. */
        CLICK, TAB,
        /** Le tic du métronome de la boucle : aucune annonce ne le joue. */
        BEAT;

        /** Vrai pour les sons d'interface et les bruitages : ils ne comptent pas dans le rythme des annonces, qui ne les retarde pas. */
        boolean isInterface() { return ordinal() >= PURCHASE.ordinal(); }

        /** Le temps, en secondes, avant que ce son d'interface ne revienne : les bruitages qui se répètent sans cesse attendent plus. */
        double gap() {
            return switch (this) {
                case BEAT -> BEAT_GAP;
                case PARTICLE -> 0.25;
                case ATOM, COLLISION, HADRON, WATER -> 0.3;
                case VOID -> 0.4;
                case SEED -> 0.5;
                case CLICK, TAB -> 0.04;
                default -> INTERFACE_GAP;
            };
        }

        /** La catégorie que les réglages gardent ou coupent ; aucune pour le tic du métronome, qui a son propre réglage. */
        SoundKind kind() {
            return switch (this) {
                case BIG_BANG -> SoundKind.BIG_BANG;
                case UNIVERSE, FINALE -> SoundKind.UNIVERSE;
                case EXPLOSION -> SoundKind.EXPLOSION;
                case PURCHASE, TOGGLE -> SoundKind.UPGRADES;
                case PARTICLE -> SoundKind.PARTICLES;
                case ATOM -> SoundKind.ATOMS;
                case COLLISION -> SoundKind.COLLIDER;
                case HADRON -> SoundKind.HADRONS;
                case VOID -> SoundKind.VOID;
                case SEED, WATER -> SoundKind.SEED;
                case CLICK -> SoundKind.CLICKS;
                case TAB -> SoundKind.TABS;
                case BEAT -> null;
                default -> SoundKind.NEWS;
            };
        }
    }

    /** Les catégories gardées : toutes au départ ; les réglages branchent les leurs. */
    private Predicate<SoundKind> allowed = kind -> true;

    /** Branche les catégories gardées ; {@code null} : toutes. */
    void setAllowed(Predicate<SoundKind> allowed) { this.allowed = allowed == null ? kind -> true : allowed; }

    private boolean allowed(Cue cue) { return cue.kind() == null || allowed.test(cue.kind()); }

    static final double SAME_CUE_GAP = 0.8;
    static final double ANY_CUE_GAP = 0.12;
    /** Deux tics du métronome ne se suivent pas à moins de ce temps, en secondes : deux zones collées n'en font qu'un. */
    static final double BEAT_GAP = 0.05;
    /**
     * Deux sons d'interface pareils (un achat, un geste dans la même dimension) ne se suivent pas à moins de
     * ce temps : assez court pour suivre des clics rapides, assez long pour qu'un clic tenu ne fasse pas un bourdon.
     */
    static final double INTERFACE_GAP = 0.07;
    /** Les gestes dans les dimensions sonnent moins fort que les annonces : ils reviennent à chaque clic. */
    static final double GESTURE_LEVEL = 0.6;

    private static final float RATE = 44_100f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    private static final AudioFormat STEREO = new AudioFormat(RATE, 16, 2, true, false);

    /** Un son prêt à partir : ses octets 16 bits petit-boutistes, en mono ou en stéréo (gauche, droite, gauche…). */
    record Pcm(byte[] bytes, boolean stereo) {}

    /**
     * Le Big Bang par défaut : l'explosion enregistrée ({@code big-bang.wav}, stéréo) et l'accord synthétisé du Big Bang
     * ({@link #synthesize(Cue)}) qui monte par-dessus, mêlés comme à l'écoute. La stéréo est gardée : ramenée en mono,
     * l'explosion perd l'essentiel de son ampleur. Vide si la ressource manque : le Big Bang reste alors synthétisé.
     */
    static final short[] BIG_BANG_STEREO = bigBangStereo();
    /** Le gain de l'explosion enregistrée dans le mélange, et celui de l'accord : ceux retenus à l'écoute. */
    static final double RECORDED_GAIN = 1.7483, CHORD_GAIN = 0.7;
    /** La crête du mélange : il est mis à ce niveau une fois mêlé. */
    static final double BIG_BANG_PEAK = 0.98;

    private final Map<Cue, short[]> samples = new EnumMap<>(Cue.class);
    /** Les skins des sons choisis : la banque, le son du Big Bang, le tic du métronome ; {@code null} = Classique. */
    private Skin bank, bangSkin, metronomeSkin;
    private final Map<Cue, Long> lastPlayed = new EnumMap<>(Cue.class);
    private long lastAny = Long.MIN_VALUE / 2;
    private volatile boolean broken = false;
    /** Le fil des sons, créé au premier son ; visible des essais pour attendre sa fin. */
    ExecutorService player;
    /** Où partent les échantillons : la sortie audio, ou un relevé pour les essais. */
    Consumer<Pcm> sink = this::output;

    /** Le son d'une annonce, ou {@code null} si elle n'en a pas. */
    static Cue cueOf(String text) {
        if (text.startsWith("Big Bang")) return Cue.BIG_BANG;
        if (text.startsWith("Univers formé") || text.startsWith("Les dix paliers sont atteints")) return Cue.UNIVERSE;
        if (text.startsWith("Défi final")) return Cue.FINALE;
        if (text.startsWith("Explosion")) return Cue.EXPLOSION;
        if (text.startsWith("Défi réussi") || text.startsWith("Record battu")
                || (text.startsWith("Défi de Big Bang") && text.contains("réussi"))) return Cue.VICTORY;
        if (text.startsWith("Succès")) return Cue.ACHIEVEMENT;
        if (text.startsWith("Skin débloqué")) return Cue.SKIN;
        if (text.startsWith("Nouveau défi") || text.startsWith("Défi « ") && text.contains("» commencé")) return Cue.CHALLENGE;
        if (text.contains("» à portée dans l'arbre") || text.startsWith("Nouvel automatisme de matière noire")
                || text.startsWith("Palier de taille") || text.startsWith("Le tableau périodique peut exploser")) return Cue.DARK;
        if (text.startsWith("Une comète")) return Cue.COMET;
        if (text.startsWith("Nouvel élément") || text.startsWith("Élément unique")) return Cue.ELEMENT;
        if (text.startsWith("Collision") && text.contains("quark top")) return Cue.COMET;
        if (text.startsWith("Palier du collisionneur")) return Cue.ACHIEVEMENT;
        if (text.startsWith("Astre mûr") || text.contains("astres mûrs")) return Cue.ELEMENT;
        if (text.startsWith("Sonde") || text.contains("sondes revenues")) return Cue.COMET;
        if (text.startsWith("Palier de vitesse") || text.startsWith("Collision")) return Cue.LEVEL;
        if (text.startsWith("Jalon") || text.startsWith("Ensemble") || text.startsWith("Palier de gluons") || text.startsWith("Racines liées")
                || text.startsWith(Notifier.NEW_TITLE) || text.startsWith("Premier atome") || text.startsWith("La création a pris")
                || text.startsWith("Automatisme offert") || text.startsWith("Molécules") || text.startsWith("Espace")
                || text.startsWith("Vide")) return Cue.MILESTONE;
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

    /** Joue un son, si le rythme le permet. Vrai s'il est parti. */
    boolean play(Cue cue, int volume) {
        if (broken || volume <= 0 || !allowed(cue)) return false;
        if (cue.isInterface()) return click(cue, volume);
        long now = System.nanoTime();
        if (now - lastAny < ANY_CUE_GAP * 1e9) return false;
        Long last = lastPlayed.get(cue);
        if (last != null && now - last < SAME_CUE_GAP * 1e9) return false;
        boolean stereo = cue == Cue.BIG_BANG && BIG_BANG_STEREO.length > 0 && SoundBanks.classicBigBang(bank, bangSkin);
        short[] sound = stereo ? BIG_BANG_STEREO : samples.computeIfAbsent(cue, this::synthesizeSkinned);
        // Le silence partiel : un son vide ne part pas et ne compte pas dans le rythme.
        if (sound.length == 0) return false;
        lastAny = now;
        lastPlayed.put(cue, now);
        send(new Pcm(pcm(sound, Math.min(100, volume) / 100.0), stereo));
        return true;
    }

    /**
     * Les skins des sons : la banque change le timbre de tous les sons, le son du Big Bang et le tic du
     * métronome ont le leur. Les sons déjà calculés sont oubliés quand le choix change.
     */
    void setSkins(Skin bank, Skin bang, Skin metronome) {
        if (bank == this.bank && bang == this.bangSkin && metronome == this.metronomeSkin) return;
        this.bank = bank;
        this.bangSkin = bang;
        this.metronomeSkin = metronome;
        samples.clear();
    }

    /** Un son dans les skins choisis : {@link SoundBanks} les connaît, le Classique reste {@link #synthesize(Cue)}. */
    private short[] synthesizeSkinned(Cue cue) { return SoundBanks.synthesize(cue, bank, bangSkin, metronomeSkin); }

    /**
     * Le tic du métronome de la boucle, au volume réglé : il ne compte pas dans le rythme des annonces, qui ne le retarde
     * pas et qu'il ne retarde pas. Vrai s'il est parti.
     */
    boolean beat(int volume) { return click(Cue.BEAT, volume); }

    /**
     * Un son d'interface : l'achat ou l'interrupteur d'une carte, le tic du métronome, et depuis la 0.213
     * les bruitages. Hors du rythme des annonces ; le même ne revient pas avant son écart ({@link Cue#gap()}). Vrai s'il est parti.
     */
    boolean click(Cue cue, int volume) {
        if (broken || volume <= 0 || !cue.isInterface() || !allowed(cue)) return false;
        long now = System.nanoTime();
        Long last = lastPlayed.get(cue);
        if (last != null && now - last < cue.gap() * 1e9) return false;
        short[] sound = samples.computeIfAbsent(cue, this::synthesizeSkinned);
        if (sound.length == 0) return false;
        lastPlayed.put(cue, now);
        send(new Pcm(pcm(sound, Math.min(100, volume) / 100.0), false));
        return true;
    }

    /** Les sons des gestes, un par dimension (1 à 10), calculés à la première fois. */
    private final short[][] gestures = new short[11][];
    private final long[] lastGesture = new long[11];

    /**
     * Le geste du joueur dans le mini-jeu de la dimension {@code dimension} : un tir dans le volume, un
     * clic sur la surface, un sondage de la grille… Plus doux que les annonces, hors de leur rythme ; le silence partiel le
     * coupe, comme tout ce qui n'est pas le Big Bang. Vrai s'il est parti.
     */
    boolean gesture(int dimension, int volume) {
        if (broken || volume <= 0 || dimension < 1 || dimension > 10 || bank == Skin.SOUND_SILENCE) return false;
        if (!allowed.test(SoundKind.DIMENSIONS)) return false;
        long now = System.nanoTime();
        if (lastGesture[dimension] != 0 && now - lastGesture[dimension] < INTERFACE_GAP * 1e9) return false;
        if (gestures[dimension] == null) gestures[dimension] = synthesizeGesture(dimension);
        lastGesture[dimension] = now;
        send(new Pcm(pcm(gestures[dimension], GESTURE_LEVEL * Math.min(100, volume) / 100.0), false));
        return true;
    }

    /** Les sons des points gagnés, un par dimension (1 à 10), et leur dernier passage. */
    private final short[][] gains = new short[11][];
    private final long[] lastGain = new long[11];
    /** Deux points gagnés dans la même dimension ne sonnent pas à moins de ce temps : l'automatisme en gagne des dizaines par seconde. */
    static final double GAIN_GAP = 0.15;

    /**
     * Un point gagné dans la dimension {@code dimension}, dont la fenêtre est ouverte : une note claire, plus
     * haute d'une dimension à la suivante, sur la gamme pentatonique. Doux comme les gestes. Vrai s'il est parti.
     */
    boolean gain(int dimension, int volume) {
        if (broken || volume <= 0 || dimension < 1 || dimension > 10 || bank == Skin.SOUND_SILENCE) return false;
        if (!allowed.test(SoundKind.DIMENSIONS)) return false;
        long now = System.nanoTime();
        if (lastGain[dimension] != 0 && now - lastGain[dimension] < GAIN_GAP * 1e9) return false;
        if (gains[dimension] == null) gains[dimension] = synthesizeGain(dimension);
        lastGain[dimension] = now;
        send(new Pcm(pcm(gains[dimension], GESTURE_LEVEL * Math.min(100, volume) / 100.0), false));
        return true;
    }

    /** Les rapports de la gamme pentatonique sur deux octaves : la note de chaque dimension au-dessus du sol (392 Hz). */
    private static final double[] GAIN_RATIOS = {1, 9 / 8.0, 5 / 4.0, 3 / 2.0, 5 / 3.0, 2, 9 / 4.0, 5 / 2.0, 3, 10 / 3.0};

    /** Le son d'un point gagné dans une dimension : une note brève et ronde, avec un peu d'octave. */
    static short[] synthesizeGain(int dimension) {
        double f = 392 * GAIN_RATIOS[dimension - 1];
        return render(0.14, t -> envelope(t, 0.002, 0.14) * Math.exp(-t * 18) * (0.4 * sine(f, t) + 0.12 * sine(2 * f, t)));
    }

    /** Envoie des échantillons à la sortie, sur le fil des sons : la lecture ne bloque jamais le jeu. */
    private void send(Pcm pcm) {
        if (player == null) {
            player = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "sons");
                thread.setDaemon(true);
                return thread;
            });
        }
        player.execute(() -> sink.accept(pcm));
    }

    /**
     * Fait entendre un son tout de suite, même s'il vient d'être joué : l'aperçu d'un skin de son que le
     * joueur vient de choisir (la banque, le Big Bang, le tic du métronome). Vrai s'il est parti.
     */
    boolean preview(Cue cue, int volume) {
        lastPlayed.remove(cue);
        lastAny = Long.MIN_VALUE / 2;
        return cue == Cue.BEAT ? beat(volume) : play(cue, volume);
    }

    /** Le son d'essai du réglage du volume : joué même s'il vient de l'être. */
    void test(int volume) {
        lastPlayed.remove(Cue.ACHIEVEMENT);
        lastAny = Long.MIN_VALUE / 2;
        play(Cue.ACHIEVEMENT, volume);
    }

    /** Le son du geste dans une dimension, court et sans saturation : chacun rappelle son mini-jeu. */
    static short[] synthesizeGesture(int dimension) {
        return switch (dimension) {
            // Le plan : un éclat ramassé, une bulle qui monte.
            case 2 -> render(0.14, t -> envelope(t, 0.003, 0.14) * 0.5 * sine(1, 500 * t + 400 * t * t / 0.28));
            // Le volume : un tir, une hauteur qui plonge.
            case 3 -> render(0.16, t -> envelope(t, 0.002, 0.16) * 0.45 * sine(1, 400 * t + 1200 * (1 - Math.exp(-25 * t)) / 25));
            // La durée : un souffle grave qui s'ouvre, comme une respiration retenue.
            case 4 -> render(0.3, t -> envelope(t, 0.08, 0.3) * (0.35 * sine(196, t) + 0.15 * sine(294, t)));
            // La boucle : un coup de bois.
            case 5 -> render(0.12, t -> envelope(t, 0.001, 0.12) * Math.exp(-t * 30) * (0.5 * sine(660, t) + 0.2 * sine(1980, t)));
            // La brane : la membrane relâchée, un « boing » qui ondule.
            case 6 -> render(0.35, t -> envelope(t, 0.004, 0.35) * 0.45 * sine(1, 220 * t + 0.6 * Math.sin(2 * Math.PI * 9 * t) + 110 * t * t / 0.7));
            // Le clicker : un clic sec, une étincelle.
            case 7 -> render(0.06, t -> envelope(t, 0.0005, 0.06) * (0.35 * noise(t) * Math.exp(-t * 200) + 0.4 * sine(2093, t) * Math.exp(-t * 60)));
            // La grille : un bip de sonde.
            case 8 -> render(0.09, t -> envelope(t, 0.002, 0.09) * 0.3 * (sine(1318.5, t) >= 0 ? 1 : -1));
            // Le miroir : deux notes de verre, l'une le reflet de l'autre.
            case 9 -> render(0.3, t -> envelope(t, 0.002, 0.3) * Math.exp(-t * 8) * (0.3 * sine(2093, t) + 0.3 * sine(3136, Math.max(0, t - 0.05))));
            // Le mélange : un « bloup » liquide qui descend.
            case 10 -> render(0.22, t -> envelope(t, 0.004, 0.22) * 0.5 * sine(1, 150 * t + 150 * (1 - Math.exp(-12 * t)) / 12));
            // La ligne : une corde pincée.
            default -> render(0.2, t -> envelope(t, 0.001, 0.2) * Math.exp(-t * 12) * (0.45 * sine(440, t) + 0.15 * sine(880, t)));
        };
    }

    /** Vrai tant que la sortie audio n'a pas échoué. */
    boolean available() { return !broken; }

    private void output(Pcm pcm) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP) clip.close(); });
            if (pcm.stereo()) {
                try {
                    clip.open(STEREO, pcm.bytes(), 0, pcm.bytes().length);
                } catch (Exception | LinkageError noStereo) {
                    // Une sortie qui refuse la stéréo : le son part en mono plutôt que de couper tous les sons.
                    byte[] mono = mono(pcm.bytes());
                    clip.open(FORMAT, mono, 0, mono.length);
                }
            } else {
                clip.open(FORMAT, pcm.bytes(), 0, pcm.bytes().length);
            }
            clip.start();
        } catch (Exception | LinkageError noAudio) {
            // Pas de carte son, sortie occupée, système sans audio : le jeu continue en silence.
            broken = true;
        }
    }

    /** L'explosion enregistrée et l'accord du Big Bang, mêlés en stéréo ; vide si la ressource manque ou se lit mal. */
    private static short[] bigBangStereo() {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream("big-bang.wav")) {
            if (raw == null) return new short[0];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 2 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0];
                byte[] bytes = in.readAllBytes();
                short[] chord = synthesize(Cue.BIG_BANG);
                int frames = Math.max(bytes.length / 4, chord.length);
                double[] mix = new double[frames * 2];
                for (int i = 0; i < bytes.length / 2; i++) {
                    mix[i] = RECORDED_GAIN * (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8)) / 32768.0;
                }
                for (int i = 0; i < chord.length; i++) {
                    mix[2 * i] += CHORD_GAIN * chord[i] / 32768.0;
                    mix[2 * i + 1] += CHORD_GAIN * chord[i] / 32768.0;
                }
                double peak = 0;
                for (double value : mix) peak = Math.max(peak, Math.abs(value));
                short[] out = new short[mix.length];
                for (int i = 0; i < mix.length; i++) out[i] = (short) Math.round(mix[i] / peak * BIG_BANG_PEAK * Short.MAX_VALUE);
                return out;
            }
        } catch (Exception unreadable) {
            return new short[0];
        }
    }

    /** Des octets stéréo ramenés en mono : la moyenne des deux canaux. */
    static byte[] mono(byte[] stereo) {
        byte[] out = new byte[stereo.length / 2];
        for (int i = 0; i < out.length / 2; i++) {
            int left = (short) ((stereo[4 * i] & 0xff) | (stereo[4 * i + 1] << 8));
            int right = (short) ((stereo[4 * i + 2] & 0xff) | (stereo[4 * i + 3] << 8));
            int value = (left + right) / 2;
            out[2 * i] = (byte) value;
            out[2 * i + 1] = (byte) (value >> 8);
        }
        return out;
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

    /** Fabrique un son, dans le timbre Classique. Les sons restent sous 70 % de la pleine échelle : jamais de saturation. */
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
            // Un accord parfait qui s'élargit, la quinte et l'octave par-dessus, sur une basse qui tient.
            case UNIVERSE -> render(2.4, t -> envelope(t, 0.15, 2.4) * (0.25 * sine(130.8, t) + 0.12 * sine(261.6, t))
                    + notes(t, 0.18, 2.4, 523.25, 659.25, 783.99, 1046.5));
            // Une fanfare de deux notes, la seconde à la quinte, sur un écho grave.
            case FINALE -> render(1.0, t -> notes(t, 0.16, 1.0, 587.33, 880) + envelope(t, 0.01, 1.0) * 0.2 * sine(146.8, t));
            // Une pluie de trois notes aiguës, très vite.
            case SKIN -> render(0.5, t -> notes(t, 0.05, 0.5, 1567.98, 1975.53, 2349.32));
            // Deux notes graves, la seconde un demi-ton plus haut : une contrainte qui s'annonce.
            case CHALLENGE -> render(0.6, t -> notes(t, 0.2, 0.6, 293.66, 311.13));
            // Un son sombre qui enfle : une quinte grave et un battement.
            case DARK -> render(0.9, t -> envelope(t, 0.2, 0.9) * (0.35 * sine(110, t) + 0.25 * sine(164.8, t) + 0.1 * sine(111.5, t)));
            // Une note claire et sa quinte, ensemble.
            case MILESTONE -> render(0.45, t -> envelope(t, 0.005, 0.45) * (0.3 * sine(784, t) + 0.2 * sine(1174.7, t)));
            // Un petit « tchip » de caisse : le prix payé.
            case PURCHASE -> render(0.07, t -> envelope(t, 0.001, 0.07) * Math.exp(-t * 45) * (0.35 * sine(1760, t) + 0.15 * sine(2637, t)));
            // Un interrupteur : un bruit bref, une note sourde.
            case TOGGLE -> render(0.05, t -> envelope(t, 0.0005, 0.05) * (0.3 * noise(t) * Math.exp(-t * 300) + 0.3 * sine(523.25, t) * Math.exp(-t * 60)));
            // Version 0.213, les bruitages. Une particule : un point de lumière, très bref et léger.
            case PARTICLE -> render(0.05, t -> envelope(t, 0.001, 0.05) * Math.exp(-t * 60) * 0.3 * sine(2349.3, t));
            // Une fusion : deux particules qui se rejoignent, une hauteur qui monte.
            case ATOM -> render(0.14, t -> envelope(t, 0.002, 0.14) * 0.4 * sine(1, 600 * t + 300 * t * t / 0.28));
            // Une collision : un choc bref et un tintement de métal.
            case COLLISION -> render(0.25, t -> envelope(t, 0.001, 0.25) * (0.2 * noise(t) * Math.exp(-t * 60)
                    + 0.3 * sine(1200, t) * Math.exp(-t * 12) + 0.15 * sine(1797, t) * Math.exp(-t * 16)));
            // Des hadrons : trois quarks liés, trois notes presque ensemble.
            case HADRON -> render(0.35, t -> notes(t, 0.03, 0.35, 783.99, 987.77, 1174.66));
            // Le vide : une paire née de rien, un souffle grave qui enfle et retombe.
            case VOID -> render(0.6, t -> envelope(t, 0.15, 0.6) * (0.3 * sine(220, t) + 0.15 * sine(110, t)));
            // Un palier de la graine : quatre notes qui poussent vers le haut.
            case SEED -> render(0.9, t -> notes(t, 0.09, 0.9, 523.25, 659.25, 783.99, 1046.5));
            // Un arrosage : une goutte, une hauteur qui retombe.
            case WATER -> render(0.12, t -> envelope(t, 0.002, 0.12) * 0.4 * sine(1, 1200 * t - 600 * (1 - Math.exp(-20 * t)) / 20));
            // Un clic : un tic sec et léger.
            case CLICK -> render(0.03, t -> envelope(t, 0.0005, 0.03) * Math.exp(-t * 120) * 0.3 * sine(1800, t));
            // Un changement d'onglet : un « toc » de bois, une page qu'on tourne.
            case TAB -> render(0.07, t -> envelope(t, 0.001, 0.07) * Math.exp(-t * 45) * (0.35 * sine(700, t) + 0.12 * sine(1400, t)));
            // Un tic de bois, très court : une note aiguë qui s'éteint aussitôt, un souffle d'attaque.
            case BEAT -> render(0.1, t -> envelope(t, 0.001, 0.1) * Math.exp(-t * 25) * (0.55 * sine(1568, t) + 0.2 * noise(t) * Math.exp(-t * 120)));
        };
    }

    /** Une forme d'onde : l'amplitude à un instant, en secondes. */
    interface Wave {
        double at(double seconds);
    }

    /** Calcule un son de cette durée, borné à 70 % de la pleine échelle. */
    static short[] render(double seconds, Wave wave) {
        short[] out = new short[(int) (seconds * RATE)];
        for (int i = 0; i < out.length; i++) {
            double value = Math.max(-0.7, Math.min(0.7, wave.at(i / RATE)));
            out[i] = (short) Math.round(value * Short.MAX_VALUE);
        }
        return out;
    }

    /** Attaque linéaire puis décroissance jusqu'au silence à la fin. */
    static double envelope(double t, double attack, double length) {
        if (t < attack) return t / attack;
        double rest = Math.max(0, 1 - (t - attack) / (length - attack));
        return rest * rest;
    }

    static double sine(double frequency, double t) { return Math.sin(2 * Math.PI * frequency * t); }

    /** Un bruit déterministe : le même son à chaque fois. */
    static double noise(double t) {
        long n = (long) (t * RATE) * 2_654_435_761L;
        n ^= n >>> 13;
        n *= 0x5bd1e995L;
        n ^= n >>> 15;
        return ((n & 0xffff) / 32_767.5) - 1;
    }

    /** Des notes l'une après l'autre, chacune tenue jusqu'à la fin. */
    static double notes(double t, double step, double length, double... frequencies) {
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
