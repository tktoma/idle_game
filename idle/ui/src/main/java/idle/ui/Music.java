package idle.ui;

import idle.core.Game;
import idle.core.sky.Cosmos;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * La musique de fond (version 0.210), composée par le jeu pendant qu'on joue : aucun fichier, comme les sons
 * ({@link Sound}). Une musique d'espace, lente et large, écrite comme un morceau : une grille d'accords par scène, des
 * nappes qui passent d'un accord au suivant par le plus court chemin (les notes communes restent tenues), une basse sur
 * la fondamentale, un arpège pincé sur les notes de l'accord, une mélodie faite de motifs qui reviennent (A, A, B, A,
 * la dernière phrase revenant à la tonique), et une réverbération qui donne l'espace.
 *
 * <p>Elle suit la partie ({@link #moodOf}). Chaque étape d'un univers a sa scène : l'aube avant la première explosion
 * (des accords ouverts, quelques cloches), l'élan vers le premier Big Bang (I-V-vi-IV, une mélodie), les Big Bangs
 * (mixolydien, une pulsation douce), l'attente des astres (lydien, des cloches lentes), l'amas (dorien), la formation
 * (vi-IV-I-V, plus animée), l'univers formé, un défi (mineur, I-VII-VI-VII, plus serré), une dimension ouverte (son
 * tempo et son arpège à elle), le défi final (mineur épique, i-VI-III-VII) et la fin (une cadence plagale, IV-I). Chaque
 * univers change de tonalité, de quinte en quinte : le onzième ne sonne pas comme le premier.
 *
 * <p>Les règles d'écriture : toutes les notes viennent de la gamme de la scène ; les nappes, la basse et l'arpège ne
 * jouent que des notes de l'accord ; la mélodie se pose sur une note de l'accord à chaque temps fort, et n'est jamais un
 * demi-ton au-dessus d'une note de l'accord. Une nouvelle scène ou une nouvelle tonalité attend la fin de la section
 * (huit mesures) et repart du premier accord de sa grille : rien ne glisse de hauteur, aucune phrase n'est coupée. Un Big
 * Bang ou un univers formé fait monter la musique un moment, avec un arpège de cloches ({@link #accent()}).
 *
 * <p>La façon de jouer ({@link Style}) a été choisie à l'écoute, parmi des séries de variantes (version 0.212) : un
 * tempo fixe de 69 dans toutes les scènes (c'est la densité qui change, jamais une accélération), des cloches de verre
 * basses, une mélodie douce qui monte au plus au sol, peu d'écho, et un fond très doux qui respire : chaque accord entre
 * puis s'éteint deux temps avant le suivant.
 *
 * <p>La synthèse ({@link Engine}) se fait sur un fil à part, par blocs, vers la sortie audio ; sans carte son, la
 * musique se tait sans erreur, comme les sons. Elle a son interrupteur et son volume dans les réglages, à part des sons.
 */
final class Music {

    /** Les modes : les demi-tons des sept degrés au-dessus de la tonique, et la pentatonique de la mélodie. */
    enum Mode {
        IONIAN(new int[] {0, 2, 4, 7, 9}, 0, 2, 4, 5, 7, 9, 11),
        LYDIAN(new int[] {0, 2, 4, 7, 9}, 0, 2, 4, 6, 7, 9, 11),
        MIXOLYDIAN(new int[] {0, 2, 4, 7, 9}, 0, 2, 4, 5, 7, 9, 10),
        DORIAN(new int[] {0, 3, 5, 7, 10}, 0, 2, 3, 5, 7, 9, 10),
        AEOLIAN(new int[] {0, 3, 5, 7, 10}, 0, 2, 3, 5, 7, 8, 10);

        private final int[] steps;
        /** La pentatonique de la mélodie : cinq notes sans demi-ton entre elles. */
        final int[] pentatonic;

        Mode(int[] pentatonic, int... steps) {
            this.pentatonic = pentatonic;
            this.steps = steps;
        }

        /** Les demi-tons du degré {@code degree} (0 la tonique), sur autant d'octaves qu'il faut. */
        int semitones(int degree) {
            return steps[Math.floorMod(degree, 7)] + 12 * Math.floorDiv(degree, 7);
        }

        /** Vrai si cette classe de hauteur (0 à 11, au-dessus de la tonique) est dans la gamme. */
        boolean contains(int pitchClass) {
            for (int step : steps) if (step == Math.floorMod(pitchClass, 12)) return true;
            return false;
        }
    }

    /** Les accords : les degrés empilés sur la fondamentale. */
    static final int[] SEVENTH = {0, 2, 4, 6};
    /** Un accord ouvert : fondamentale, quinte, neuvième et tierce au-dessus ; pour les scènes immobiles. */
    static final int[] OPEN = {0, 4, 8, 9};

    /** Les motifs de l'arpège, en rangs dans l'accord (0 à 3) et à l'octave (4 à 7). */
    static final int[] UP = {0, 1, 2, 3, 4, 5, 6, 7};
    static final int[] UP_DOWN = {0, 1, 2, 3, 4, 3, 2, 1};
    static final int[] BROKEN = {0, 2, 1, 3, 2, 4, 3, 5};
    static final int[] ALBERTI = {0, 2, 1, 2};
    static final int[] ROLL = {0, 1, 2, 3};

    /** La voix de la mélodie : une flûte douce, ou des cloches. */
    enum Lead { FLUTE, BELL }

    /** Le timbre des cloches : modulation de fréquence, verre, marimba, vibraphone. */
    enum Bell { FM, GLASS, MARIMBA, VIBES }

    /**
     * La façon de jouer : le tempo (fixe, ou celui de la scène rapproché de 72 selon {@code tempoSpread}, de 0 à 1), le
     * moment où une nouvelle scène entre (en doubles croches : 16 à la mesure, 32 à la fin de la phrase, 128 à la fin de
     * la section), la plus haute note de la flûte, la plus haute cloche et leur timbre, la voix de la mélodie ({@code null} : celle de la scène), le
     * froissement et la pulsation, la réverbération et l'écho, la saturation douce, les sections sans mélodie, l'arpège,
     * le poids de la mélodie ; puis le fond : le poids des nappes, leur clarté (1 : celle de la scène, moins : plus
     * sourdes), leur désaccord (1 : un cent et demi entre leurs deux oscillateurs, 0 : un seul oscillateur, aucun
     * battement) et le poids de la basse ; enfin la respiration des nappes ({@code padBreath} : 0 tenues sans
     * arrêt, 1 qui s'éteignent avant chaque nouvel accord et laissent {@code padGap} temps de silence, 2 qui jouent quatre
     * accords et se taisent sur les deux suivants, 3 qui se taisent sur un accord tiré au hasard avec la chance {@code padGap}).
     */
    record Style(int tempo, double tempoSpread, int changeEvery, int fluteHigh, int bellHigh, Bell bell, Lead lead, boolean shaker,
            boolean pulse, double reverb, double echo, double drive, boolean rests, boolean arpeggio, double melody,
            double pad, double padBright, double padDetune, double bass, int padBreath, double padGap) {
        /** Le style retenu à l'écoute (variantes du 15 novembre 2026) : tempo fixe 69, fond très doux qui respire à chaque accord. */
        static final Style DEFAULT = new Style(69, 0, 128, 79, 72, Bell.GLASS, null, false, true, 1, 0.4, 0.8, true, true, 0.65,
                1, 0.4, 1, 1, 1, 2);
    }

    /** Les scènes de la musique, une par moment de la partie. */
    enum Scene {
        AUBE(Mode.IONIAN, new int[] {0, 3, 5, 3}, OPEN, 4, 64, 0, 0, ROLL, 1, Lead.BELL, 0, false, 0.9, 0.5, 0, 0.55, 900, 1.2),
        ELAN(Mode.IONIAN, new int[] {0, 4, 5, 3}, SEVENTH, 2, 76, 1, 2, UP_DOWN, 2, Lead.FLUTE, 0, false, 0.7, 0.55, 0.45, 0.5, 1400, 1),
        BIG_BANGS(Mode.MIXOLYDIAN, new int[] {0, 6, 3, 0}, SEVENTH, 2, 88, 2, 2, BROKEN, 2, Lead.FLUTE, 1, true, 0.6, 0.55, 0.45, 0.45, 1800, 0.9),
        GALAXIE(Mode.LYDIAN, new int[] {0, 1, 0, 1}, OPEN, 4, 60, 0, 1, ROLL, 1, Lead.BELL, 0, false, 1, 0.45, 0.3, 0.55, 1000, 1.3),
        AMAS(Mode.DORIAN, new int[] {0, 3, 0, 3}, SEVENTH, 2, 68, 1, 2, ALBERTI, 2, Lead.FLUTE, 0, false, 0.85, 0.5, 0.4, 0.45, 1200, 1.1),
        FORMATION(Mode.IONIAN, new int[] {5, 3, 0, 4}, SEVENTH, 2, 80, 1, 2, UP, 3, Lead.FLUTE, 1, true, 0.7, 0.55, 0.45, 0.5, 1700, 1),
        UNIVERS(Mode.IONIAN, new int[] {0, 5, 3, 4}, SEVENTH, 2, 70, 1, 2, UP_DOWN, 2, Lead.BELL, 0, false, 0.85, 0.5, 0.4, 0.5, 1500, 1.2),
        DEFI(Mode.AEOLIAN, new int[] {0, 6, 5, 6}, SEVENTH, 2, 96, 2, 2, BROKEN, 2, Lead.FLUTE, 2, true, 0.55, 0.6, 0.5, 0.45, 1600, 0.8),
        DIMENSION(Mode.LYDIAN, new int[] {0, 1, 0, 4}, SEVENTH, 2, 72, 1, 2, UP, 2, Lead.FLUTE, 0, false, 0.75, 0.5, 0.5, 0.45, 1500, 1.1),
        FINALE(Mode.AEOLIAN, new int[] {0, 5, 2, 6}, SEVENTH, 2, 100, 2, 2, BROKEN, 3, Lead.FLUTE, 2, true, 0.7, 0.6, 0.5, 0.5, 2000, 1),
        FIN(Mode.IONIAN, new int[] {3, 0, 3, 0}, OPEN, 4, 56, 0, 0, ROLL, 1, Lead.BELL, 0, false, 1, 0.5, 0, 0.55, 900, 1.4);

        final Mode mode;
        /** Les degrés des quatre accords de la grille. */
        final int[] progression;
        final int[] chord;
        final int barsPerChord;
        final double bpm;
        /** La basse : 0 une note par mesure, 1 deux par mesure, 2 sur chaque croche. */
        final int bass;
        /** Les notes d'arpège par temps (0 : pas d'arpège) et leur motif. */
        final int arpPerBeat;
        final int[] arpPattern;
        /** La mélodie : 1 rare, 2 chantée, 3 animée. */
        final int melody;
        final Lead lead;
        /** La pulsation : 0 aucune, 1 sur les temps 1 et 3, 2 sur chaque temps ; et le froissement des contretemps. */
        final int kick;
        final boolean shaker;
        /** Le poids des couches, la clarté des nappes (en hertz) et la part de réverbération. */
        final double padLevel, bassLevel, arpLevel, leadLevel, brightness, space;

        Scene(Mode mode, int[] progression, int[] chord, int barsPerChord, double bpm, int bass, int arpPerBeat, int[] arpPattern,
                int melody, Lead lead, int kick, boolean shaker, double padLevel, double bassLevel, double arpLevel, double leadLevel,
                double brightness, double space) {
            this.mode = mode;
            this.progression = progression;
            this.chord = chord;
            this.barsPerChord = barsPerChord;
            this.bpm = bpm;
            this.bass = bass;
            this.arpPerBeat = arpPerBeat;
            this.arpPattern = arpPattern;
            this.melody = melody;
            this.lead = lead;
            this.kick = kick;
            this.shaker = shaker;
            this.padLevel = padLevel;
            this.bassLevel = bassLevel;
            this.arpLevel = arpLevel;
            this.leadLevel = leadLevel;
            this.brightness = brightness;
            this.space = space;
        }
    }

    /**
     * Ce que la musique suit : la scène, l'univers en cours (1 à 11) qui donne la tonalité, et la dimension ouverte
     * (1 à 10, 0 pour aucune) qui donne à la scène d'une dimension son tempo et son arpège.
     */
    record Mood(Scene scene, int universe, int dimension) {}

    /** Après ce nombre de Big Bangs, le dernier préparé, la partie attend les astres et la galaxie. */
    static final int LAST_BIG_BANG = 5 + Game.BIG_BANG_SKY.length;

    /**
     * La scène de ce moment de la partie, {@code openDimension} la dimension dont la fenêtre est ouverte (0 sinon). Ce
     * qui presse passe devant : la fin, le défi final, un défi en cours, puis la dimension ouverte, puis l'étape de
     * l'univers.
     */
    static Mood moodOf(Game game, int openDimension) {
        int universe = Math.max(1, Math.min(11, game.isUniverseFormed() && !game.isReplaying() ? game.universes() : game.universes() + 1));
        return new Mood(sceneOf(game, openDimension), universe, openDimension);
    }

    static Scene sceneOf(Game game, int openDimension) {
        if (game.isGameFinished()) return Scene.FIN;
        if (game.isFinalChallengeOpen()) return Scene.FINALE;
        if (game.activeChallenge() != null) return Scene.DEFI;
        if (openDimension > 0) return Scene.DIMENSION;
        if (game.hasCosmos(Cosmos.UNIVERSE)) return Scene.UNIVERS;
        if (game.cosmosFormed() >= 2) return Scene.FORMATION;
        if (game.cosmosFormed() == 1) return Scene.AMAS;
        if (game.bigBangs() >= LAST_BIG_BANG) return Scene.GALAXIE;
        if (game.bigBangs() >= 1) return Scene.BIG_BANGS;
        if (game.state().explosions() > 0) return Scene.ELAN;
        return Scene.AUBE;
    }

    /** La tonique de l'univers {@code universe} (1 à 11), en note MIDI : de quinte en quinte depuis le la grave (45), repliée de mi à ré dièse. */
    static int tonic(int universe) {
        int fifths = Math.floorMod(7 * (universe - 1), 12);
        return 45 + (fifths > 6 ? fifths - 12 : fifths);
    }

    /** La fréquence d'une note MIDI, en hertz. */
    static double frequency(double midi) {
        return 440 * Math.pow(2, (midi - 69) / 12);
    }

    // ------------------------------------------------------------------
    // La lecture, sur un fil à part
    // ------------------------------------------------------------------

    static final float RATE = 22_050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    /** Un bloc de synthèse : 2 048 échantillons, un peu moins d'un dixième de seconde. */
    static final int BLOCK = 2_048;
    /** Le temps du fondu quand la musique se coupe ou reprend, en secondes. */
    static final double FADE = 0.6;

    private volatile Mood target = new Mood(Scene.AUBE, 1, 0);
    private volatile boolean wanted;
    private volatile int volume;
    private volatile int accents;
    private volatile boolean broken;
    /** Le fil de la musique, créé quand elle doit jouer ; visible des essais. */
    Thread player;
    /** Où partent les blocs : la sortie audio, ou un relevé pour les essais. */
    java.util.function.Consumer<byte[]> sink = this::output;
    private SourceDataLine line;

    /**
     * À chaque image : la scène à suivre, l'interrupteur et le volume de la musique (0 à 100). Ne bloque jamais : le fil de
     * la musique démarre au premier appel où elle doit jouer, et s'arrête après un fondu quand elle est coupée.
     */
    void update(Mood mood, boolean enabled, int volume) {
        target = mood;
        this.volume = Math.max(0, Math.min(100, volume));
        wanted = enabled && volume > 0 && !broken;
        if (wanted && (player == null || !player.isAlive())) {
            player = new Thread(this::run, "musique");
            player.setDaemon(true);
            player.start();
        }
    }

    /** Un grand moment (un Big Bang, un univers formé) : la musique monte quelques secondes. */
    void accent() {
        accents++;
    }

    /** La boucle du fil : un bloc après l'autre, au volume du moment, avec un fondu à l'entrée et à la sortie. */
    private void run() {
        Engine engine = new Engine(201);
        float[] block = new float[BLOCK];
        double fade = 0, gain = 0;
        int accentsSeen = accents;
        while (!broken) {
            boolean on = wanted;
            if (!on && fade <= 0) break;
            engine.setMood(target);
            while (accentsSeen != accents) {
                accentsSeen++;
                engine.accent();
            }
            engine.render(block);
            double step = BLOCK / RATE / FADE;
            double nextFade = on ? Math.min(1, fade + step) : Math.max(0, fade - step);
            // Une courbe en carré, comme les sons : la moitié du curseur sonne comme la moitié du volume.
            double level = volume / 100.0;
            double nextGain = nextFade * level * level;
            sink.accept(pcm(block, gain, nextGain));
            fade = nextFade;
            gain = nextGain;
        }
        if (line != null) {
            line.drain();
            line.close();
            line = null;
        }
    }

    /** Les échantillons en octets 16 bits petit-boutistes, le gain glissant de {@code from} à {@code to} sur le bloc. */
    static byte[] pcm(float[] samples, double from, double to) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            double gain = from + (to - from) * i / samples.length;
            int value = (int) Math.round(samples[i] * gain * Short.MAX_VALUE);
            out[2 * i] = (byte) value;
            out[2 * i + 1] = (byte) (value >> 8);
        }
        return out;
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
            // Pas de carte son, sortie occupée : la partie continue sans musique.
            broken = true;
        }
    }

    // ------------------------------------------------------------------
    // La composition et la synthèse
    // ------------------------------------------------------------------

    /**
     * Le compositeur et l'orchestre. Il tient le temps en doubles croches, pose l'accord de la grille au début de ses
     * mesures, écrit la mélodie phrase par phrase, et calcule les échantillons : les voix, puis une réverbération
     * (quatre filtres en peigne et deux passe-tout) et un écho à la croche pointée. Le même hasard, la même musique :
     * deux moteurs de même graine, menés pareil, rendent les mêmes échantillons.
     */
    static final class Engine {

        /** Les rôles des notes, et donc leur timbre. */
        enum Role { PAD, BASS, ARP, FLUTE, BELL, KICK, SHAKER }

        /** Une note jouée, telle que les essais la relèvent : son rôle, sa hauteur, l'accord et la gamme du moment. */
        record Played(Role role, int midi, int tonic, int[] chord, Mode mode, boolean strong) {}

        private static final int TABLE = 4_096;
        private static final float[] SINE = new float[TABLE + 1];
        /** Une dent de scie adoucie (huit harmoniques en 1/n^1,4) : le timbre des nappes. */
        private static final float[] SOFT_SAW = new float[TABLE + 1];

        static {
            double max = 0;
            double[] saw = new double[TABLE + 1];
            for (int i = 0; i <= TABLE; i++) {
                double x = 2 * Math.PI * i / TABLE;
                SINE[i] = (float) Math.sin(x);
                for (int n = 1; n <= 8; n++) saw[i] += Math.sin(n * x) / Math.pow(n, 1.4);
                max = Math.max(max, Math.abs(saw[i]));
            }
            for (int i = 0; i <= TABLE; i++) SOFT_SAW[i] = (float) (saw[i] / max);
        }

        private static double wave(float[] table, double turns) {
            double at = (turns - Math.floor(turns)) * TABLE;
            int i = (int) at;
            return table[i] + (table[i + 1] - table[i]) * (at - i);
        }

        /** Les notes qui sonnent à la fois ; quand il n'y a plus de place, la plus ancienne qui n'est pas une nappe la cède. */
        private static final int MAX_NOTES = 64;
        /** Le registre des nappes ; celui de la mélodie descend d'une octave et demie sous sa plus haute note (selon le style). */
        static final int PAD_LOW = 52, PAD_HIGH = 76;

        private static final class Note {
            Role role;
            int midi;
            double frequency, amplitude, age, length, phase, phase2, lowpass;
            boolean alive;
        }

        private final SplittableRandom random;
        /** Le hasard des silences des nappes, à part : une variante qui en change ne change pas la mélodie. */
        private final SplittableRandom padRandom;
        private final Style style;
        private final Note[] notes = new Note[MAX_NOTES];
        private long notesStarted;
        /** Ce qui relève les notes jouées : rien dans le jeu, un relevé pour les essais. */
        java.util.function.Consumer<Played> onPlayed = played -> { };

        private Mood wanted = new Mood(Scene.AUBE, 1, 0);
        /** La scène et la tonique qui jouent : elles ne changent qu'au début d'une mesure. */
        private Scene scene = Scene.AUBE;
        private int tonic = tonic(1), dimension;
        private double bpm;
        private double beat;
        private long sixteenth = -1, sceneStart;
        /** Les couches, glissantes : elles vont vers celles de la scène en deux secondes environ. */
        private double pad, bass, arp, lead, brightness = Scene.AUBE.brightness, space = Scene.AUBE.space;
        private double accent, accentGoal;
        /** L'accord en cours : ses classes de hauteur au-dessus de la tonique, sa fondamentale, et la disposition des nappes. */
        private int[] chord = new int[4];
        private int chordRoot;
        private int[] voicing = {57, 64, 67, 71};
        private int arpStep;
        /** La mélodie : les motifs de la section, la phrase en cours (début en doubles croches, durée, hauteur). */
        private int[][] motifA, motifB;
        private final List<int[]> phrase = new ArrayList<>();
        private int lastLead = 70;
        /** Les cloches d'un grand moment, à jouer dans les doubles croches qui viennent. */
        private final List<int[]> pendingBells = new ArrayList<>();

        // La réverbération et l'écho.
        private static final int[] COMBS = {557, 593, 641, 677};
        private static final int[] ALLPASSES = {225, 341};
        private final float[][] comb = new float[COMBS.length][];
        private final int[] combAt = new int[COMBS.length];
        private final double[] combLow = new double[COMBS.length];
        private final float[][] allpass = new float[ALLPASSES.length][];
        private final int[] allpassAt = new int[ALLPASSES.length];
        private final float[] echo = new float[(int) (2 * RATE)];
        private int echoAt;
        private double padLow, masterLow, masterLow2;

        Engine(long seed) {
            this(seed, Style.DEFAULT);
        }

        Engine(long seed, Style style) {
            random = new SplittableRandom(seed);
            padRandom = new SplittableRandom(seed + 1);
            this.style = style;
            bpm = tempoOf(Scene.AUBE, 0);
            for (int i = 0; i < MAX_NOTES; i++) notes[i] = new Note();
            for (int i = 0; i < COMBS.length; i++) comb[i] = new float[COMBS[i]];
            for (int i = 0; i < ALLPASSES.length; i++) allpass[i] = new float[ALLPASSES[i]];
        }

        void setMood(Mood mood) {
            wanted = mood;
        }

        /** Un grand moment : la musique s'éclaire quelques secondes, et l'accord monte en cloches. */
        void accent() {
            accentGoal = 1;
            for (int i = 0; i < 6; i++) pendingBells.add(new int[] {2 * i, voicing[i % 4] + 12 * (i / 4)});
        }

        /** Le nombre de notes lancées depuis le début : pour les essais. */
        long notesStarted() {
            return notesStarted;
        }

        /** Le tempo du moment, en temps par minute. */
        double bpm() {
            return bpm;
        }

        /** La tonique qui joue, en note MIDI. */
        int playingTonic() {
            return tonic;
        }

        /** Remplit {@code out} d'échantillons, chacun entre −0,7 et 0,7. */
        void render(float[] out) {
            double glide = 1 - Math.exp(-out.length / (2.0 * RATE));
            pad += (scene.padLevel - pad) * glide;
            bass += (scene.bassLevel - bass) * glide;
            arp += (scene.arpLevel - arp) * glide;
            lead += (scene.leadLevel - lead) * glide;
            brightness += (scene.brightness - brightness) * glide;
            space += (scene.space - space) * glide;
            double padCut = 1 - Math.exp(-2 * Math.PI * style.padBright() * brightness * (1 + 0.6 * accent) / RATE);
            double masterCut = 1 - Math.exp(-2 * Math.PI * 7_000 / RATE);
            int echoDelay = Math.min(echo.length - 1, (int) (0.75 * 60 / bpm * RATE));
            for (int i = 0; i < out.length; i++) {
                beat += bpm / 60 / RATE;
                long now = (long) Math.floor(beat * 4);
                if (now != sixteenth) {
                    sixteenth = now;
                    onSixteenth();
                }
                accentGoal *= 1 - 1 / (6.0 * RATE);
                accent += (accentGoal - accent) / (0.3 * RATE);
                // Les voix, en trois bus : les nappes (filtrées), le reste au sec, et ce qui part dans l'écho.
                double padBus = 0, dry = 0, toEcho = 0, toReverb = 0;
                for (Note note : notes) {
                    if (!note.alive) continue;
                    double v = voice(note);
                    note.age += 1 / RATE;
                    switch (note.role) {
                        case PAD -> padBus += v;
                        case ARP, FLUTE -> {
                            dry += v;
                            toEcho += style.echo() * v;
                            toReverb += 0.6 * v;
                        }
                        case BELL -> {
                            dry += v;
                            toEcho += 0.5 * style.echo() * v;
                            toReverb += v;
                        }
                        case BASS -> {
                            dry += v;
                            toReverb += 0.1 * v;
                        }
                        default -> dry += v;
                    }
                }
                padLow += (padBus - padLow) * padCut;
                dry += padLow;
                toReverb += 0.7 * padLow;
                // L'écho à la croche pointée, qui revient trois fois en s'éteignant.
                int back = echoAt - echoDelay;
                if (back < 0) back += echo.length;
                double echoed = echo[back];
                echo[echoAt] = (float) (toEcho + 0.33 * echoed);
                echoAt = (echoAt + 1) % echo.length;
                dry += 0.35 * echoed;
                toReverb += 0.35 * echoed;
                double wet = reverb(toReverb * (0.8 + 0.4 * accent));
                double mix = dry + 0.55 * style.reverb() * space * wet;
                // Deux filtres d'un pôle à la suite : le haut du spectre s'adoucit sans rien couper de la musique.
                masterLow += (mix - masterLow) * masterCut;
                masterLow2 += (masterLow - masterLow2) * masterCut;
                out[i] = (float) (0.7 * Math.tanh(style.drive() * masterLow2));
            }
        }

        /**
         * Le tempo d'une scène : celui du style s'il est fixe, sinon celui de la scène rapproché de 72 ; une dimension ouverte
         * presse un peu plus à chaque dimension. Il change d'un coup, quand la scène entre, jamais en cours de phrase.
         */
        private double tempoOf(Scene scene, int dimension) {
            if (style.tempo() > 0) return style.tempo();
            double own = scene == Scene.DIMENSION ? scene.bpm + 2 * dimension : scene.bpm;
            return 72 + (own - 72) * style.tempoSpread();
        }

        /** Une réverbération de Schroeder : quatre peignes amortis en parallèle, puis deux passe-tout en série. */
        private double reverb(double in) {
            double sum = 0;
            for (int c = 0; c < COMBS.length; c++) {
                float[] buffer = comb[c];
                double out = buffer[combAt[c]];
                combLow[c] += (out - combLow[c]) * 0.7;
                buffer[combAt[c]] = (float) (in + 0.8 * combLow[c]);
                combAt[c] = (combAt[c] + 1) % buffer.length;
                sum += out;
            }
            double x = sum * 0.25;
            for (int a = 0; a < ALLPASSES.length; a++) {
                float[] buffer = allpass[a];
                double delayed = buffer[allpassAt[a]];
                double y = -0.5 * x + delayed;
                buffer[allpassAt[a]] = (float) (x + 0.5 * y);
                allpassAt[a] = (allpassAt[a] + 1) % buffer.length;
                x = y;
            }
            return x;
        }

        // ----- L'écriture -----

        /** Une double croche : la mesure qui change, puis la basse, l'arpège, la mélodie, la pulsation. */
        private void onSixteenth() {
            int inBar = (int) Math.floorMod(sixteenth, 16L);
            if (Math.floorMod(sixteenth - sceneStart, (long) style.changeEvery()) == 0) changeMood();
            long since = sixteenth - sceneStart;
            int chordBar = scene.barsPerChord * 16;
            if (since % chordBar == 0) nextChord((int) (since / chordBar));
            boolean onBeat = inBar % 4 == 0;
            // La basse : la fondamentale, à l'octave grave.
            boolean bassHit = switch (scene.bass) {
                case 0 -> inBar == 0;
                case 1 -> inBar == 0 || inBar == 8;
                default -> inBar % 2 == 0;
            };
            if (bassHit) {
                // Liée quand elle tient la mesure ou la demi-mesure : la note suivante prend la place de la précédente, qui
                // s'efface en quelques centièmes ; détachée sur les croches.
                double length = scene.bass == 0 ? 16 * 15 / bpm : scene.bass == 1 ? 8 * 15 / bpm : 2 * 0.6 * 15 / bpm;
                for (Note note : notes) if (note.alive && note.role == Role.BASS && note.age < note.length) note.length = note.age;
                start(Role.BASS, bassNote(), 0.32, length, true);
            }
            // L'arpège : les notes de l'accord, selon le motif de la scène (ou de la dimension ouverte).
            int perBeat = scene.arpPerBeat;
            if (perBeat > 0 && style.arpeggio() && inBar % (4 / perBeat) == 0) {
                int[] pattern = scene == Scene.DIMENSION ? dimensionPattern() : scene.arpPattern;
                int index = pattern[arpStep++ % pattern.length];
                start(Role.ARP, voicing[index % 4] + 12 * (index / 4), 0.35, 0.2, onBeat);
            }
            // La mélodie : une phrase de deux mesures, écrite à son début.
            int inPhrase = (int) Math.floorMod(since, 32L);
            if (inPhrase == 0) writePhrase((int) Math.floorMod(since / 32, 4L));
            // Une section sur trois, la mélodie se tait : les nappes et l'arpège jouent seuls, puis elle revient.
            boolean singing = !style.rests() || Math.floorMod(since / 128, 3L) != 2;
            for (int[] note : phrase) {
                if (!singing) break;
                if (note[0] != inPhrase) continue;
                boolean strong = inPhrase % 8 == 0;
                int midi = sing(note[2], strong, note[3] == 1);
                Role role = leadVoice() == Lead.BELL ? Role.BELL : Role.FLUTE;
                start(role, midi, 0.4 * style.melody(), note[1] * 15 / bpm, strong);
            }
            // La pulsation, très douce, et le froissement des contretemps.
            if (style.pulse() && onBeat && (scene.kick == 2 || scene.kick == 1 && inBar % 8 == 0)) start(Role.KICK, 33, 0.35, 0.3, true);
            if (style.shaker() && scene.shaker && inBar % 4 == 2) start(Role.SHAKER, 0, 0.12, 0.08, false);
            // Les cloches d'un grand moment.
            for (int i = pendingBells.size() - 1; i >= 0; i--) {
                int[] bell = pendingBells.get(i);
                if (bell[0]-- <= 0) {
                    int pitch = nearestChordTone(bell[1]);
                    while (pitch > style.bellHigh()) pitch -= 12;
                    start(Role.BELL, pitch, 0.32, 1, true);
                    pendingBells.remove(i);
                }
            }
        }

        /**
         * Le moment où une nouvelle scène ou une nouvelle tonalité peut entrer (à la fin d'une phrase, d'une section ou
         * d'une mesure, selon le style) : elle repart du premier accord de sa grille, à son tempo.
         */
        private void changeMood() {
            Mood mood = wanted;
            int newTonic = tonic(mood.universe());
            if (mood.scene() == scene && newTonic == tonic && mood.dimension() == dimension) return;
            scene = mood.scene();
            tonic = newTonic;
            dimension = mood.dimension();
            bpm = tempoOf(scene, dimension);
            sceneStart = sixteenth;
            arpStep = 0;
            motifA = null;
            // La mélodie repart dans le registre de la nouvelle scène, à l'octave la plus proche de sa dernière note.
            while (lastLead > leadHigh()) lastLead -= 12;
            while (lastLead < leadLow()) lastLead += 12;
        }

        /** Le motif d'arpège d'une dimension ouverte : chacune le sien. */
        private int[] dimensionPattern() {
            return switch (dimension % 4) {
                case 0 -> UP;
                case 1 -> UP_DOWN;
                case 2 -> BROKEN;
                default -> ALBERTI;
            };
        }

        /** Pose l'accord {@code index} de la grille : les nappes vont aux notes les plus proches, les notes communes restent. */
        private void nextChord(int index) {
            int degree = scene.progression[Math.floorMod(index, scene.progression.length)];
            Mode mode = scene.mode;
            for (int v = 0; v < 4; v++) chord[v] = Math.floorMod(mode.semitones(degree + scene.chord[v]), 12);
            chordRoot = chord[0];
            int[] next = voiceLead(voicing, chord);
            if (style.padBreath() == 1 || padSilent(index)) {
                breathe(next);
                return;
            }
            // Les nappes : celles qui ne sont plus dans l'accord s'éteignent, les nouvelles entrent en fondu.
            for (Note note : notes) {
                if (!note.alive || note.role != Role.PAD || note.length < Double.MAX_VALUE) continue;
                boolean kept = false;
                for (int midi : next) kept |= midi == note.midi;
                if (!kept) note.length = note.age;
            }
            for (int midi : next) {
                boolean sounding = false;
                for (Note note : notes) sounding |= note.alive && note.role == Role.PAD && note.length == Double.MAX_VALUE && note.midi == midi;
                if (!sounding) start(Role.PAD, midi, 0.16, Double.MAX_VALUE, true);
            }
            voicing = next;
        }

        /** Les accords posés depuis le début de la musique, toutes scènes confondues. */
        private long chordsPlayed;

        /** Vrai si cet accord reste sans nappes : deux accords sur six, ou tiré au hasard, selon le style. */
        private boolean padSilent(int index) {
            chordsPlayed++;
            return switch (style.padBreath()) {
                case 2 -> chordsPlayed % 6 == 5 || chordsPlayed % 6 == 0;
                case 3 -> index > 0 && padRandom.nextDouble() < style.padGap();
                default -> false;
            };
        }

        /**
         * Les nappes qui respirent : celles qui sonnent s'éteignent en fondu ; puis, si les nappes respirent à chaque
         * accord, le nouvel accord entre et s'éteindra avant le suivant, en laissant {@code padGap} temps de silence. Un
         * accord sans nappes s'arrête là.
         */
        private void breathe(int[] next) {
            for (Note note : notes) if (note.alive && note.role == Role.PAD && note.length > note.age) note.length = note.age;
            voicing = next;
            if (style.padBreath() != 1) return;
            double chordSeconds = scene.barsPerChord * 4 * 60 / bpm;
            double length = Math.max(1, chordSeconds - style.padGap() * 60 / bpm - 1.5);
            for (int midi : next) start(Role.PAD, midi, 0.16, length, true);
        }

        /**
         * La disposition la plus proche de {@code from} pour ces classes de hauteur : chaque note dans le registre des
         * nappes, à au moins deux demi-tons de sa voisine (trois sous le do du milieu, pour ne pas brouiller le grave).
         */
        private int[] voiceLead(int[] from, int[] classes) {
            int[] best = null;
            double bestCost = Double.MAX_VALUE;
            int[] choice = new int[4];
            for (int mask = 0; mask < 81; mask++) {
                int m = mask;
                for (int v = 0; v < 4; v++) {
                    int pitch = tonic + classes[v];
                    while (pitch < PAD_LOW) pitch += 12;
                    pitch += 12 * (m % 3);
                    m /= 3;
                    choice[v] = pitch;
                }
                int[] sorted = choice.clone();
                java.util.Arrays.sort(sorted);
                if (sorted[3] > PAD_HIGH) continue;
                boolean muddy = false;
                for (int v = 1; v < 4; v++) {
                    int gap = sorted[v] - sorted[v - 1];
                    if (gap < 2 || sorted[v - 1] < 60 && gap < 3) muddy = true;
                }
                if (muddy) continue;
                int[] previous = from.clone();
                java.util.Arrays.sort(previous);
                double cost = 0;
                for (int v = 0; v < 4; v++) cost += Math.abs(sorted[v] - previous[v]);
                // Une légère préférence pour le milieu du registre : la disposition ne dérive pas vers un bord.
                cost += 0.05 * Math.abs((sorted[0] + sorted[3]) / 2.0 - 64);
                if (cost < bestCost) {
                    bestCost = cost;
                    best = sorted;
                }
            }
            return best == null ? from.clone() : best;
        }

        /** La fondamentale de l'accord, entre mi grave et ré dièse. */
        private int bassNote() {
            int pitch = tonic + chordRoot;
            while (pitch >= 52) pitch -= 12;
            while (pitch < 40) pitch += 12;
            return pitch;
        }

        /**
         * Prépare la phrase {@code index} (0 à 3) de la section : A, A, B, puis A qui se pose sur la fondamentale de son
         * accord. Les motifs sont des rythmes et des pas dans la pentatonique ; ils se tirent au début de la section, et le
         * motif A d'une section revient une fois sur deux dans la suivante. Les hauteurs se décident note à note, au moment
         * de jouer ({@link #sing}), contre l'accord qui sonne alors : un même motif repris sur un autre accord garde son
         * dessin.
         */
        private void writePhrase(int index) {
            if (index == 0 || motifA == null) {
                if (motifA == null || random.nextBoolean()) motifA = motif();
                motifB = motif();
            }
            int[][] motif = index == 2 ? motifB : motifA;
            phrase.clear();
            leadPosition = pentatonicIndex(lastLead);
            for (int n = 0; n < motif.length; n++) {
                boolean cadence = index == 3 && n == motif.length - 1;
                phrase.add(new int[] {motif[n][0], motif[n][1], motif[n][2], cadence ? 1 : 0});
            }
        }

        /** Où en est la mélodie dans la pentatonique, toutes octaves comptées. */
        private int leadPosition;
        /** Le plus grand saut de la mélodie d'une note à la suivante : une quinte juste. */
        static final int MAX_LEAP = 7;

        /**
         * La hauteur de la note suivante de la mélodie : un pas dans la pentatonique, puis, sur un temps fort, la note de
         * l'accord la plus proche (à la fin de la phrase, sa fondamentale) ; ailleurs la note telle quelle, sauf un demi-ton
         * au-dessus d'une note de l'accord, qui grincerait. Jamais un saut de plus d'une quinte, jamais un triton.
         */
        private int sing(int step, boolean strong, boolean cadence) {
            leadPosition += step;
            int target = pentatonicMidi(leadPosition);
            // Le registre de la mélodie : une phrase qui en sort repart de l'autre côté du pas.
            if (target > leadHigh() || target < leadLow()) {
                leadPosition -= 2 * step;
                // Encore dehors (une scène qui change de registre) : une octave de pentatonique, cinq notes, à la fois.
                while (pentatonicMidi(leadPosition) > leadHigh()) leadPosition -= 5;
                while (pentatonicMidi(leadPosition) < leadLow()) leadPosition += 5;
                target = pentatonicMidi(leadPosition);
            }
            int midi;
            if (strong || cadence) {
                midi = landing(target, cadence, step);
            } else {
                midi = avoidClash(target);
                if (badLeap(midi)) midi = avoidClash(pentatonicMidi(pentatonicIndex(midi) + (midi > lastLead ? -1 : 1)));
            }
            lastLead = midi;
            leadPosition = pentatonicIndex(midi);
            return midi;
        }

        /**
         * La note de l'accord (ou sa fondamentale) la plus proche de {@code target}, sans saut trop grand depuis la
         * précédente, et qui ne reste pas sur place quand le motif fait un pas ({@code step}).
         */
        private int landing(int target, boolean root, int step) {
            int best = -1;
            double bestScore = Double.MAX_VALUE;
            for (int midi = Math.max(leadLow(), target - 7); midi <= Math.min(leadHigh(), target + 7); midi++) {
                int pitchClass = Math.floorMod(midi - tonic, 12);
                if (root ? pitchClass != chordRoot : !isChordTone(midi)) continue;
                // Une note qui revient sur elle-même quand le motif bouge fige la ligne : elle cède devant une voisine.
                double score = Math.abs(midi - target) + (badLeap(midi) ? 10 : 0) + (midi == lastLead && step != 0 ? 3 : 0);
                if (score < bestScore) {
                    bestScore = score;
                    best = midi;
                }
            }
            return best < 0 ? nearestChordTone(target) : best;
        }

        /** La voix de la mélodie : celle du style, sinon celle de la scène. */
        private Lead leadVoice() {
            return style.lead() != null ? style.lead() : scene.lead;
        }

        /** Le haut du registre de la mélodie : celui de la flûte ou celui des cloches, selon le style. */
        private int leadHigh() {
            return leadVoice() == Lead.BELL ? style.bellHigh() : style.fluteHigh();
        }

        /** Le bas du registre de la mélodie : une octave et demie sous son haut. */
        private int leadLow() {
            return leadHigh() - 17;
        }

        private boolean badLeap(int midi) {
            int leap = Math.abs(midi - lastLead);
            return leap > MAX_LEAP || leap == 6;
        }

        /** Une note un demi-ton au-dessus d'une note de l'accord descend sur elle. */
        private int avoidClash(int midi) {
            int pitchClass = Math.floorMod(midi - tonic, 12);
            for (int tone : chord) if (Math.floorMod(pitchClass - tone, 12) == 1) return midi - 1;
            return midi;
        }

        /** Les rythmes de la mélodie selon la scène, en doubles croches sur deux mesures : début et durée de chaque note. */
        // Chaque phrase finit par un silence : la mélodie respire avant la suivante.
        private static final int[][][] SPARSE = {
            {{0, 12}, {16, 10}}, {{0, 8}, {12, 4}, {16, 10}}, {{4, 12}, {16, 8}}};
        private static final int[][][] SUNG = {
            {{0, 6}, {6, 2}, {8, 8}, {16, 10}}, {{0, 4}, {4, 4}, {8, 12}, {24, 4}}, {{2, 2}, {4, 4}, {8, 8}, {16, 6}},
            {{0, 3}, {3, 1}, {4, 4}, {8, 4}, {12, 4}, {16, 8}}};
        private static final int[][][] LIVELY = {
            {{0, 2}, {2, 2}, {4, 4}, {8, 2}, {10, 2}, {12, 4}, {16, 4}, {20, 6}},
            {{0, 3}, {3, 3}, {6, 2}, {8, 4}, {12, 4}, {16, 2}, {18, 2}, {20, 6}},
            {{0, 4}, {4, 2}, {6, 2}, {8, 6}, {16, 4}, {20, 4}, {24, 4}}};

        /** Un motif : un rythme de la scène, et pour chaque note un pas dans la pentatonique, le plus souvent conjoint. */
        private int[][] motif() {
            int[][][] rhythms = scene.melody == 1 ? SPARSE : scene.melody == 2 ? SUNG : LIVELY;
            int[][] rhythm = rhythms[random.nextInt(rhythms.length)];
            int[][] motif = new int[rhythm.length][];
            int drift = 0;
            for (int n = 0; n < rhythm.length; n++) {
                double r = random.nextDouble();
                int step = n == 0 ? 0 : r < 0.4 ? 1 : r < 0.8 ? -1 : r < 0.88 ? 2 : r < 0.96 ? -2 : 0;
                motif[n] = new int[] {rhythm[n][0], rhythm[n][1], step};
                drift += step;
            }
            // Un motif qui monte ou descend trop ne revient pas : quelques pas changent de sens jusqu'à ce qu'il reste près de son départ.
            for (int n = motif.length - 1; n > 0 && Math.abs(drift) > 2; n--) {
                if (Integer.signum(motif[n][2]) != Integer.signum(drift)) continue;
                drift -= 2 * motif[n][2];
                motif[n][2] = -motif[n][2];
            }
            return motif;
        }

        /** Le rang d'une note dans la pentatonique de la tonique, toutes octaves comptées. */
        private int pentatonicIndex(int midi) {
            int best = 0;
            int bestDistance = Integer.MAX_VALUE;
            for (int index = -10; index < 40; index++) {
                int distance = Math.abs(pentatonicMidi(index) - midi);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = index;
                }
            }
            return best;
        }

        private int pentatonicMidi(int index) {
            int[] scale = scene.mode.pentatonic;
            return tonic + scale[Math.floorMod(index, scale.length)] + 12 * Math.floorDiv(index, scale.length);
        }

        private int nearestChordTone(int midi) {
            for (int distance = 0; distance < 12; distance++) {
                if (isChordTone(midi - distance)) return midi - distance;
                if (isChordTone(midi + distance)) return midi + distance;
            }
            return midi;
        }

        private boolean isChordTone(int midi) {
            int pitchClass = Math.floorMod(midi - tonic, 12);
            for (int tone : chord) if (tone == pitchClass) return true;
            return false;
        }

        private void start(Role role, int midi, double amplitude, double length, boolean strong) {
            Note note = freeNote();
            note.role = role;
            note.midi = midi;
            note.frequency = frequency(midi);
            note.amplitude = amplitude;
            note.age = 0;
            note.length = length;
            note.phase = random.nextDouble();
            note.phase2 = random.nextDouble();
            note.lowpass = 0;
            note.alive = true;
            notesStarted++;
            if (role != Role.KICK && role != Role.SHAKER) onPlayed.accept(new Played(role, midi, tonic, chord.clone(), scene.mode, strong));
        }

        /**
         * Une place pour une note : une libre, sinon la plus ancienne qui n'est pas une nappe. Avant, la place suivante
         * était prise quoi qu'il y sonne : une nappe tenue pouvait s'arrêter net, d'où un claquement et un trou dans l'accord.
         */
        private Note freeNote() {
            Note oldest = null;
            for (Note note : notes) {
                if (!note.alive) return note;
                if (note.role != Role.PAD && (oldest == null || note.age > oldest.age)) oldest = note;
            }
            return oldest != null ? oldest : notes[0];
        }

        // ----- Les timbres -----

        /** Le son d'une note à son âge ; elle s'éteint d'elle-même. */
        private double voice(Note note) {
            double t = note.age;
            switch (note.role) {
                case PAD -> {
                    // Deux dents de scie adoucies, la seconde plus faible et à peine désaccordée (un cent et demi) : de la
                    // largeur sans battement. De même force et plus écartées, elles s'annulaient toutes les deux secondes.
                    note.phase += note.frequency / RATE;
                    note.phase2 += note.frequency * (1 + 0.0009 * style.padDetune()) / RATE;
                    double envelope = Math.min(1, t / 2.0) * release(note, 3.0);
                    double second = style.padDetune() > 0 ? 0.45 : 0;
                    return style.pad() * pad * note.amplitude * envelope * (wave(SOFT_SAW, note.phase) + second * wave(SOFT_SAW, note.phase2)) / (1 + second);
                }
                case BASS -> {
                    note.phase += note.frequency / RATE;
                    double envelope = Math.min(1, t / 0.03) * (0.7 + 0.3 * Math.exp(-t * 1.5)) * release(note, 0.05);
                    return style.bass() * bass * note.amplitude * envelope * (wave(SINE, note.phase) + 0.25 * wave(SINE, 2 * note.phase));
                }
                case ARP -> {
                    // Une corde pincée : les harmoniques hautes s'éteignent les premières.
                    note.phase += note.frequency / RATE;
                    if (t > 2.5) note.alive = false;
                    double body = 0;
                    for (int k = 1; k <= 4; k++) body += wave(SINE, k * note.phase) * Math.exp(-t * (2.2 + 2.5 * k)) / k;
                    return arp * note.amplitude * Math.min(1, t / 0.003) * body;
                }
                case FLUTE -> {
                    // Une flûte douce : un vibrato qui vient après l'attaque, un peu de deuxième harmonique.
                    double vibrato = 1 + 0.004 * Math.min(1, Math.max(0, t - 0.25) / 0.3) * wave(SINE, 5.2 * t);
                    note.phase += note.frequency * vibrato / RATE;
                    double envelope = Math.min(1, t / 0.07) * release(note, 0.6);
                    return lead * note.amplitude * envelope * (wave(SINE, note.phase) + 0.18 * wave(SINE, 2 * note.phase) + 0.05 * wave(SINE, 3 * note.phase));
                }
                case BELL -> {
                    note.phase += note.frequency / RATE;
                    if (t > 4) note.alive = false;
                    double attack = Math.min(1, t / 0.004);
                    double level = Math.max(lead, 0.4) * note.amplitude * attack;
                    return level * switch (style.bell()) {
                        // Une modulation de fréquence qui s'adoucit en sonnant.
                        case FM -> {
                            note.phase2 += note.frequency * 3.5 / RATE;
                            double index = 0.5 * Math.exp(-t * 2.5);
                            yield Math.exp(-t * 1.1) * wave(SINE, note.phase + index * wave(SINE, note.phase2) / (2 * Math.PI));
                        }
                        // Du verre : la note et son octave, qui s'éteint plus vite.
                        case GLASS -> Math.exp(-t * 1.2) * (wave(SINE, note.phase) + 0.25 * wave(SINE, 2 * note.phase) * Math.exp(-t * 2));
                        // Une lame de bois : un partiel à la double octave, très bref.
                        case MARIMBA -> Math.exp(-t * 3) * (wave(SINE, note.phase) + 0.3 * wave(SINE, 4 * note.phase) * Math.exp(-t * 20));
                        // Un vibraphone : une note ronde et un tremblement lent.
                        default -> Math.exp(-t * 1.0) * (1 - 0.3 * (0.5 + 0.5 * wave(SINE, 5 * t))) * (wave(SINE, note.phase) + 0.08 * wave(SINE, 3 * note.phase));
                    };
                }
                case KICK -> {
                    // Un battement sourd : une hauteur qui tombe vite.
                    note.phase += (45 + 60 * Math.exp(-t * 30)) / RATE;
                    if (t > 0.4) note.alive = false;
                    return note.amplitude * Math.min(1, t / 0.002) * Math.exp(-t * 9) * wave(SINE, note.phase);
                }
                default -> {
                    // Un froissement : un bruit filtré, très bref.
                    if (t > 0.1) note.alive = false;
                    double noise = random.nextDouble() * 2 - 1;
                    double high = noise - note.lowpass;
                    note.lowpass += (noise - note.lowpass) * 0.5;
                    return note.amplitude * Math.min(1, t / 0.002) * Math.exp(-t * 45) * high;
                }
            }
        }

        /** Le facteur de sortie d'une note : 1 tant qu'elle est tenue, puis vers 0 en {@code release} secondes ; alors elle se tait. */
        private static double release(Note note, double release) {
            if (note.age < note.length) return 1;
            double left = 1 - (note.age - note.length) / release;
            if (left <= 0) {
                note.alive = false;
                return 0;
            }
            return left * left;
        }
    }
}
