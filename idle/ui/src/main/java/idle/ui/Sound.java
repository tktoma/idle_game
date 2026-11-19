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
 * <p>Les sons suivent les annonces ({@link Notifier}) : un succès, un élément nouveau, une explosion, un Big Bang… D'une
 * fournée d'annonces, seul le son le plus important est joué. Depuis la 0.247, aucun son n'attend un délai avant de
 * revenir : seul le nombre de sons qui jouent en même temps est borné ({@link #MAX_VOICES}), pour ne pas saturer la
 * sortie audio.
 *
 * <p>Le métronome de la boucle a son tic à part ({@link #beat(int)}) : il bat en rythme, plusieurs fois par seconde,
 * sans attendre ni retarder les sons des annonces.
 *
 * <p>Les sons n'ont plus de skins depuis la 0.251 (les banques, le son du Big Bang et les tics du métronome sont retirés) :
 * chaque son a une seule voix.
 *
 * <p>Des sons pour les moments qui n'en avaient pas (univers formé, palier du défi final, skin débloqué, défi qui
 * s'ouvre ou commence, matière noire, jalons), et des sons d'interface qui ne passent pas par les annonces : l'achat ou
 * l'interrupteur d'une carte ({@link #click(Cue, int)}) et le geste du joueur dans le mini-jeu d'une dimension ({@link
 * #gesture(int, int)}), un timbre par dimension. Ceux-là ne passent pas par les fournées d'annonces, comme le
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
        /** Version 0.210 : un univers formé (l'enregistrement du joueur depuis la 0.265). */
        UNIVERSE,
        /** Version 0.266 : la fin de la partie, à part de l'univers formé ; l'enregistrement du joueur depuis la 0.267. */
        GAME_END,
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
        /** Un palier de vitesse des particules : le son du joueur depuis la 0.264 ({@code palier-vitesse.wav}). */
        LEVEL,
        /** Version 0.264 : une annonce de collision, à part du palier de vitesse : elle garde le petit bip. */
        COLLISION_NEWS,
        /** Version 0.210 : un achat à la main, sur une carte à portée ; aucune annonce ne le joue. */
        PURCHASE,
        /** Version 0.210 : une carte qui s'allume ou s'éteint ; aucune annonce ne le joue. */
        TOGGLE,
        /** Une particule créée, un atome (une fusion), une collision, des hadrons, une paire du vide. */
        PARTICLE, ATOM, COLLISION, HADRON, VOID,
        /** Version 0.230 : un atome créé, vu sur le grand atome de l'onglet Atomes. */
        NEW_ATOM,
        /** Version 0.240 : une sonde lancée ; une note de piano du miroir (son motif, puis les cases reproduites). */
        PROBE, MIRROR,
        /** Version 0.213 : un palier de la graine, un arrosage. */
        SEED,
        /** Version 0.243 : le rond de croissance cueilli ; le passage d'une saison (un accord de bois par saison). */
        BLOOM, SEASON,
        /** Version 0.213 : un clic sur un bouton, un changement d'onglet. */
        CLICK, TAB,
        /** Le tic du métronome de la boucle : aucune annonce ne le joue. */
        BEAT;

        /** Vrai pour les sons d'interface et les bruitages : ils ne passent pas par les fournées d'annonces. */
        boolean isInterface() { return ordinal() >= PURCHASE.ordinal(); }

        /** La catégorie que les réglages gardent ou coupent ; aucune pour le tic du métronome, qui a son propre réglage. */
        SoundKind kind() {
            return switch (this) {
                case BIG_BANG -> SoundKind.BIG_BANG;
                case UNIVERSE, GAME_END, FINALE -> SoundKind.UNIVERSE;
                case EXPLOSION -> SoundKind.EXPLOSION;
                case PURCHASE, TOGGLE -> SoundKind.UPGRADES;
                case PARTICLE -> SoundKind.PARTICLES;
                case ATOM, NEW_ATOM -> SoundKind.ATOMS;
                case PROBE -> SoundKind.PROBES;
                case MIRROR -> SoundKind.DIMENSIONS;
                case COLLISION -> SoundKind.COLLIDER;
                case HADRON -> SoundKind.HADRONS;
                case VOID -> SoundKind.VOID;
                case SEED, BLOOM, SEASON -> SoundKind.SEED;
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

    /**
     * Le volume perçu de chaque son (la plus forte moyenne quadratique sur 50 ms, en dB sous la pleine échelle) : les sons
     * rares, les grands moments, tous au même niveau ; les sons répétitifs, tous au même niveau, plus bas, pour ne pas
     * lasser. Chaque son y est mis une fois, à sa fabrication, enregistrements compris.
     */
    static final double RARE_DB = -11, REPEATED_DB = -20;
    /** La collision du collisionneur, un peu au-dessus des autres sons répétitifs (demandé à l'écoute). */
    static final double COLLISION_DB = REPEATED_DB + 4;
    /** Les sons qui reviennent souvent : bruitages, sons d'interface, la fusion (et, à part, les gestes, les points et le grondement). */
    static final Set<Cue> REPEATED = EnumSet.of(Cue.BLOOM, Cue.SEASON, Cue.PARTICLE, Cue.ATOM, Cue.NEW_ATOM, Cue.MIRROR, Cue.COLLISION, Cue.HADRON, Cue.VOID,
            Cue.CLICK, Cue.TAB, Cue.PURCHASE, Cue.TOGGLE);
    /** Aucun son ne dépasse cette crête, même mis à son niveau. */
    static final double MAX_PEAK = 0.98;

    /** Le niveau visé pour ce son. */
    static double targetDb(Cue cue) { return cue == Cue.COLLISION ? COLLISION_DB : REPEATED.contains(cue) ? REPEATED_DB : RARE_DB; }

    /** Le volume perçu, en amplitude (de 0 à 1) : la plus forte moyenne quadratique sur 50 ms, tous canaux mêlés. */
    static double loudness(short[] samples, int channels) {
        int window = (int) (0.05 * RATE) * channels;
        double best = 0;
        for (int start = 0; start < Math.max(1, samples.length - window + 1); start += Math.max(1, window / 4)) {
            double sum = 0;
            int end = Math.min(samples.length, start + window);
            for (int i = start; i < end; i++) sum += (double) samples[i] * samples[i];
            if (end > start) best = Math.max(best, Math.sqrt(sum / (end - start)) / 32768.0);
        }
        return best;
    }

    /** Ce son mis au niveau perçu {@code targetDb}, sans dépasser {@link #MAX_PEAK} ; un son vide reste vide. */
    static short[] leveled(short[] samples, int channels, double targetDb) {
        double loudness = loudness(samples, channels);
        if (samples.length == 0 || loudness <= 0) return samples;
        int peak = 0;
        for (short sample : samples) peak = Math.max(peak, Math.abs(sample));
        double gain = Math.min(Math.pow(10, targetDb / 20) / loudness, MAX_PEAK * Short.MAX_VALUE / peak);
        short[] out = new short[samples.length];
        for (int i = 0; i < out.length; i++) out[i] = (short) Math.round(samples[i] * gain);
        return out;
    }
    /**
     * Version 0.247 : au plus ce nombre de sons qui jouent en même temps, tous confondus ; au-delà, un nouveau son ne part
     * pas. Chaque son prend une ligne de la sortie audio, qui n'en a pas sans fin.
     */
    static final int MAX_VOICES = 24;
    /** Au plus ce nombre de particules qui sonnent en même temps : la sortie audio n'est pas noyée sous les générateurs rapides. */
    static final int MAX_PARTICLE_VOICES = 6;

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
    static final short[] BIG_BANG_STEREO = leveled(bigBangStereo(), 2, RARE_DB);
    /**
     * L'explosion du tableau par défaut : un enregistrement ({@code explosion.wav}, stéréo, 3,6 s), mis au niveau de
     * l'explosion synthétisée d'avant.
     */
    static final short[] EXPLOSION_STEREO = leveled(stereo("explosion.wav"), 2, RARE_DB);
    /**
     * La fusion des générateurs par défaut : un enregistrement ({@code fusion.wav}, 2,1 s), tel quel. Ses deux canaux
     * sont presque opposés : large au casque, il s'efface sur un haut-parleur unique (choisi ainsi à l'écoute).
     */
    static final short[] FUSION_STEREO = leveled(stereo("fusion.wav"), 2, REPEATED_DB);
    /** Un atome créé, sur l'onglet Atomes : un enregistrement ({@code atome.wav}, 1,45 s, sans son silence de fin). */
    static final short[] NEW_ATOM_STEREO = leveled(stereo("atome.wav"), 2, REPEATED_DB);
    /**
     * Un palier de la graine, d'où qu'il vienne (0.256) : l'enregistrement du joueur ({@code graine-palier.wav}, 2,04 s,
     * remplacé en 0.257 : une montée qui culmine vers 0,75 s puis s'éteint), son silence de début coupé ; au niveau des sons rares.
     */
    static final short[] SEED_STEREO = leveled(stereo("graine-palier.wav"), 2, RARE_DB);
    /**
     * Un univers formé (version 0.265 ; la fin de la partie a son propre son depuis la 0.266) : l'enregistrement du joueur ({@code univers.wav}, 5,2 s,
     * une montée qui tient puis s'éteint), son silence de début coupé ; au niveau des sons rares.
     */
    static final short[] UNIVERSE_STEREO = leveled(stereo("univers.wav"), 2, RARE_DB);
    /**
     * La fin de la partie (version 0.267) : l'enregistrement du joueur ({@code fin.wav}, 9,6 s, plus long que l'animation
     * de la fin : il continue sur l'écran de fin), son silence de début coupé ; au niveau des sons rares.
     */
    static final short[] GAME_END_STEREO = leveled(stereo("fin.wav"), 2, RARE_DB);
    /**
     * Un nouvel élément (version 0.273) : le son choisi par le joueur ({@code nouvel-element.wav}, « Unlock Stinger » de
     * Pixabay, 1,46 s en stéréo), son demi-seconde de silence de début coupée ; à son niveau d'avant. Il sert aussi à
     * l'élément unique et à l'astre mûr, qui partageaient ce son. Vide si le fichier manque : la cloche synthétisée.
     */
    static final short[] ELEMENT_STEREO = leveled(stereo("nouvel-element.wav"), 2, targetDb(Cue.ELEMENT));
    /**
     * Une collision réussie du collisionneur (un croisement raté ne sonne pas) : un enregistrement ({@code collision.wav},
     * 1,5 s), sa montée presque inaudible coupée pour que le choc tombe tout de suite, sa queue inaudible coupée en fondu.
     */
    /**
     * Le piano du miroir : seize notes de la gamme pentatonique de do, de do4 à do7, tirées du Salamander Grand Piano V3
     * (Alexander Holm, licence CC BY 3.0), chacune mise au niveau des sons répétitifs ; vide si la ressource manque.
     */
    static final short[][] PIANO = piano();
    /** Le nombre de notes du piano, et la durée de chacune dans la ressource {@code piano.wav}. */
    static final int PIANO_NOTES = 16;
    static final double PIANO_NOTE_SECONDS = 1.2;
    /** Une faute dans le miroir : les deux notes graves ensemble, plus bas. */
    static final short[] PIANO_WRONG = pianoWrong();
    /**
     * La membrane de la brane relâchée : un coup sec, puis la membrane qui ondule (choisi à l'écoute en 0.242,
     * {@code brane-claquement.wav}, 0,8 s) ; plus aigu et plus fort selon la tirée. Vide si la ressource manque : le geste
     * synthétisé d'avant le remplace.
     */
    static final short[] BRANE_SNAP = leveled(monoResource("brane-claquement.wav"), 1, REPEATED_DB);
    /** Le rond de croissance cueilli : une grosse bulle d'eau qui remonte (choisie à l'écoute en 0.243, {@code graine-rond.wav}). */
    static final short[] BLOOM_SOUND = leveled(monoResource("graine-rond.wav"), 1, REPEATED_DB);
    /**
     * Le passage d'une saison : un accord de bois (marimba) par saison, du printemps à l'hiver, choisi à l'écoute en 0.243
     * ({@code graine-saisons.wav}, quatre accords de 1,2 s) ; aucun si la ressource manque.
     */
    static final short[][] SEASONS = seasons();
    static final double SEASON_SECONDS = 1.2;
    /** Une sonde lancée : un décollage grondant, qui monte puis s'éloigne en s'assourdissant (choisi à l'écoute en 0.244, {@code sonde.wav}). */
    static final short[] PROBE_SOUND = leveled(monoResource("sonde.wav"), 1, RARE_DB);
    /**
     * Le clic du clicker : un tapotement doux, un doigt sur du bois, rond et feutré (choisi à l'écoute en 0.245, plus
     * grave que l'essai).
     */
    /**
     * Le clic (version 0.258) : le son choisi par le joueur, un « clic » de 7 ms de Kenney ({@code clic.wav}, CC0), pour
     * chaque bouton et pour le clicker ; au niveau des sons répétitifs. Vide si le fichier manque.
     */
    static final short[] CLICK_SOUND = leveled(monoResource("clic.wav"), 1, REPEATED_DB);
    /**
     * L'achat (version 0.263) : le son choisi par le joueur ({@code achat.wav}, « click2 », 49 ms), au niveau des sons
     * répétitifs. Vide si le fichier manque : l'achat reprend alors le bip synthétisé.
     */
    static final short[] PURCHASE_SOUND = leveled(monoResource("achat.wav"), 1, REPEATED_DB);
    /**
     * Le palier de vitesse des particules (version 0.264) : le son choisi par le joueur ({@code palier-vitesse.wav},
     * 0,18 s), au niveau des sons rares. Vide si le fichier manque : le palier reprend alors le petit bip.
     */
    static final short[] SPEED_SOUND = leveled(monoResource("palier-vitesse.wav"), 1, RARE_DB);
    /**
     * Un palier du défi final (version 0.271) : le son choisi par le joueur ({@code defi-palier.wav}, 1,7 s), au niveau
     * des sons rares. Vide si le fichier manque : le palier reprend alors ses deux notes synthétisées.
     */
    static final short[] FINALE_TIER_SOUND = leveled(monoResource("defi-palier.wav"), 1, RARE_DB);
    /**
     * Un défi réussi (version 0.271) : le son choisi par le joueur ({@code defi-reussi.wav}, « confirmation », 0,49 s), au
     * niveau des sons rares ; il sert aussi au record battu, qui partageait déjà ce son. Vide si le fichier manque : les
     * trois notes synthétisées.
     */
    static final short[] VICTORY_SOUND = leveled(monoResource("defi-reussi.wav"), 1, RARE_DB);
    /**
     * Un défi qui s'ouvre, ou qui commence (version 0.271) : le son choisi par le joueur ({@code defi-ouvert.wav},
     * « drop », 0,19 s), à son niveau d'avant. Vide si le fichier manque : les deux notes synthétisées.
     */
    static final short[] CHALLENGE_SOUND = leveled(monoResource("defi-ouvert.wav"), 1, targetDb(Cue.CHALLENGE));
    /** Le clic du clicker : le clic du joueur, ou le tapotement synthétisé de la 0.245 si le fichier manque. */
    static final short[] CLICKER_TAP = CLICK_SOUND.length > 0 ? CLICK_SOUND : leveled(render(0.09, t -> 0.45 * ((Math.sin(2 * Math.PI * 600 * t)
            + 0.4 * Math.sin(2 * Math.PI * 1500 * t)) * Math.min(1, t / 0.0008) * Math.exp(-t / 0.018) + 0.25 * noise(t) * Math.exp(-t / 0.004))),
            1, REPEATED_DB);
    /** Les intervalles de l'accord du bonus, au-dessus du clic : la tierce, la quinte, l'octave, la dixième. */
    static final double[] CLICKER_CHORD = {1.25, 1.5, 2, 2.5};
    /** L'écart entre deux notes de l'accord du bonus, en secondes : un arpège rapide (0.258), le clic étant trop bref pour un accord plaqué. */
    static final double CLICKER_STRUM = 0.025;
    /**
     * Le clic dans une zone ×2 : le même clic, en accord arpégé (0.258 : une note toutes les {@link #CLICKER_STRUM}
     * secondes) ; chaque zone superposée sous le clic y ajoute une note (une zone : le clic et sa tierce ; quatre :
     * jusqu'à la dixième).
     */
    static final short[][] CLICKER_BONUS = clickerBonus();
    static final short[] COLLISION_STEREO = leveled(stereo("collision.wav"), 2, COLLISION_DB);
    /**
     * Le son d'une particule récupérée, un par palier de vitesse ({@link idle.ui.particles.SpeedStages}) : l'enregistrement
     * {@code particule.wav} (depuis la 0.274 le « bong » choisi par le joueur, un si bémol de 0,12 s ; un fa de 0,52 s
     * avant), lu plus vite (plus aigu) ou plus lentement (plus grave) selon le palier.
     * Vide si la ressource manque : le son synthétisé reste alors.
     */
    static final short[][] PARTICLE_STAGES = particleStages();
    /** La crête du son de particule : celle des autres bruitages. */
    static final double PARTICLE_PEAK = 0.3;

    /** Le gain de l'explosion enregistrée dans le mélange, et celui de l'accord : ceux retenus à l'écoute. */
    static final double RECORDED_GAIN = 1.7483, CHORD_GAIN = 0.7;
    /** La crête du mélange : il est mis à ce niveau une fois mêlé. */
    static final double BIG_BANG_PEAK = 0.98;

    private final Map<Cue, short[]> samples = new EnumMap<>(Cue.class);
    private volatile boolean broken = false;
    /** La fin de chaque son qui joue encore, en nanosecondes : leur nombre est borné par {@link #MAX_VOICES}. */
    private final java.util.PriorityQueue<Long> voiceEnds = new java.util.PriorityQueue<>();
    /** Les échecs de la sortie audio à la suite : passé {@link #FAILURES_BEFORE_SILENCE}, le jeu se tait. */
    private int failures;
    /** Une ligne refusée de temps en temps (sortie pleine) ne coupe pas les sons ; ce nombre d'échecs à la suite, si. */
    static final int FAILURES_BEFORE_SILENCE = 20;
    /** Le fil des sons, créé au premier son ; visible des essais pour attendre sa fin. */
    ExecutorService player;
    /** Où partent les échantillons : la sortie audio, ou un relevé pour les essais. */
    Consumer<Pcm> sink = this::output;

    /** Le son d'une annonce, ou {@code null} si elle n'en a pas. */
    static Cue cueOf(String text) {
        if (text.startsWith("Big Bang")) return Cue.BIG_BANG;
        if (text.startsWith("Univers formé")) return Cue.UNIVERSE;
        if (text.startsWith("Les dix paliers sont atteints")) return Cue.GAME_END;
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
        if (text.startsWith("Palier de vitesse")) return Cue.LEVEL;
        if (text.startsWith("Collision")) return Cue.COLLISION_NEWS;
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
        // Version 0.271 : les enregistrements mono des annonces (le palier de vitesse, le palier du défi final) partent
        // aussi d'ici ; avant, seuls les stéréo y partaient, et ces annonces gardaient leur son synthétisé.
        short[] mono = monoRecorded(cue);
        short[] recorded = mono == null ? recorded(cue) : null;
        boolean stereo = recorded != null;
        short[] sound = mono != null ? mono : stereo ? recorded : samples.computeIfAbsent(cue, Sound::leveledSound);
        // Un son vide (une ressource qui manque) ne part pas.
        if (sound.length == 0) return false;
        return send(new Pcm(pcm(sound, Math.min(100, volume) / 100.0), stereo));
    }

    /** L'enregistrement mono de ce son, ou {@code null} (le rond de croissance, le printemps par défaut, la sonde, le clic, l'achat, le palier de vitesse, le palier du défi final, le défi réussi, le défi qui s'ouvre). */
    private static short[] monoRecorded(Cue cue) {
        if (cue == Cue.BLOOM && BLOOM_SOUND.length > 0) return BLOOM_SOUND;
        if (cue == Cue.SEASON && SEASONS.length > 0) return SEASONS[0];
        if (cue == Cue.PROBE && PROBE_SOUND.length > 0) return PROBE_SOUND;
        if (cue == Cue.CLICK && CLICK_SOUND.length > 0) return CLICK_SOUND;
        if (cue == Cue.PURCHASE && PURCHASE_SOUND.length > 0) return PURCHASE_SOUND;
        if (cue == Cue.LEVEL && SPEED_SOUND.length > 0) return SPEED_SOUND;
        if (cue == Cue.FINALE && FINALE_TIER_SOUND.length > 0) return FINALE_TIER_SOUND;
        if (cue == Cue.VICTORY && VICTORY_SOUND.length > 0) return VICTORY_SOUND;
        if (cue == Cue.CHALLENGE && CHALLENGE_SOUND.length > 0) return CHALLENGE_SOUND;
        return null;
    }

    /** L'enregistrement stéréo de ce son, ou {@code null} : le son synthétisé. */
    private static short[] recorded(Cue cue) {
        if (cue == Cue.BIG_BANG && BIG_BANG_STEREO.length > 0) return BIG_BANG_STEREO;
        if (cue == Cue.EXPLOSION && EXPLOSION_STEREO.length > 0) return EXPLOSION_STEREO;
        if (cue == Cue.ATOM && FUSION_STEREO.length > 0) return FUSION_STEREO;
        if (cue == Cue.NEW_ATOM && NEW_ATOM_STEREO.length > 0) return NEW_ATOM_STEREO;
        if (cue == Cue.COLLISION && COLLISION_STEREO.length > 0) return COLLISION_STEREO;
        if (cue == Cue.SEED && SEED_STEREO.length > 0) return SEED_STEREO;
        if (cue == Cue.UNIVERSE && UNIVERSE_STEREO.length > 0) return UNIVERSE_STEREO;
        if (cue == Cue.GAME_END && GAME_END_STEREO.length > 0) return GAME_END_STEREO;
        if (cue == Cue.ELEMENT && ELEMENT_STEREO.length > 0) return ELEMENT_STEREO;
        return null;
    }

    /** Un son synthétisé, mis à son niveau ; le tic du métronome garde le sien (il a son propre réglage). */
    static short[] leveledSound(Cue cue) {
        short[] sound = synthesize(cue);
        return cue == Cue.BEAT ? sound : leveled(sound, 1, targetDb(cue));
    }

    /**
     * Le tic du métronome de la boucle, au volume réglé, à part des annonces. Vrai s'il est parti.
     */
    boolean beat(int volume) { return click(Cue.BEAT, volume); }

    /**
     * Un son d'interface : l'achat ou l'interrupteur d'une carte, le tic du métronome, et depuis la 0.213
     * les bruitages. Vrai s'il est parti.
     */
    boolean click(Cue cue, int volume) {
        return sound(cue, volume, null);
    }

    /**
     * Une particule récupérée sur un générateur de ce palier de vitesse : l'enregistrement, plus grave à chaque palier.
     * Même réglage que le son des particules ; six au plus en même temps.
     */
    boolean particle(int stage, int volume) {
        long now = System.nanoTime();
        while (!particleEnds.isEmpty() && particleEnds.peekFirst() <= now) particleEnds.pollFirst();
        if (particleEnds.size() >= MAX_PARTICLE_VOICES) return false;
        boolean recorded = PARTICLE_STAGES.length > 0;
        short[] sound = recorded ? PARTICLE_STAGES[Math.max(0, Math.min(PARTICLE_STAGES.length - 1, stage))] : null;
        if (!sound(Cue.PARTICLE, volume, sound)) return false;
        int length = sound != null ? sound.length : samples.getOrDefault(Cue.PARTICLE, new short[0]).length;
        particleEnds.addLast(now + (long) (length / (double) RATE * 1e9));
        return true;
    }

    /** La fin de chaque particule qui sonne encore, dans l'ordre où elles sont parties (les plus anciennes finissent d'abord, à peu près). */
    private final ArrayDeque<Long> particleEnds = new ArrayDeque<>();

    /**
     * Une note du piano du miroir ({@code note} de 0 à {@value #PIANO_NOTES} − 1, du grave à l'aigu), ou la faute
     * ({@code note} négatif).
     */
    boolean piano(int note, int volume) {
        short[] sound = note < 0 ? PIANO_WRONG : PIANO.length == 0 ? null : PIANO[Math.min(PIANO.length - 1, note)];
        return sound(Cue.MIRROR, volume, sound != null && sound.length == 0 ? null : sound);
    }

    /** Un son d'interface, l'enregistrement {@code recorded} s'il est donné, sinon celui des skins. */
    private boolean sound(Cue cue, int volume, short[] recorded) {
        if (broken || volume <= 0 || !cue.isInterface() || !allowed(cue)) return false;
        if (recorded == null) recorded = monoRecorded(cue);
        short[] stereo = recorded == null ? recorded(cue) : null;
        short[] sound = recorded != null ? recorded : stereo != null ? stereo : samples.computeIfAbsent(cue, Sound::leveledSound);
        if (sound.length == 0) return false;
        return send(new Pcm(pcm(sound, Math.min(100, volume) / 100.0), stereo != null));
    }

    /** Les sons des gestes, un par dimension (1 à 10), calculés à la première fois. */
    private final short[][] gestures = new short[11][];

    /**
     * Le geste du joueur dans le mini-jeu de la dimension {@code dimension} : un tir dans le volume, un
     * clic sur la surface, un sondage de la grille… Au niveau des sons répétitifs. Vrai s'il est parti.
     */
    boolean gesture(int dimension, int volume) { return gesture(dimension, volume, 1); }

    /** Le geste joué plus aigu ({@code pitch} > 1) ou plus grave : la brane relâchée sonne selon la force du tirage. */
    boolean gesture(int dimension, int volume, double pitch) {
        if (dimension < 1 || dimension > 10) return false;
        if (gestures[dimension] == null) gestures[dimension] = dimension == 6 && BRANE_SNAP.length > 0 ? BRANE_SNAP
                : dimension == 7 ? CLICKER_TAP : leveled(synthesizeGesture(dimension), 1, REPEATED_DB);
        return gestureWith(dimension, pitch == 1 || !(pitch > 0) ? gestures[dimension] : resampled(gestures[dimension], pitch), volume);
    }

    /** Le geste de la dimension {@code dimension}, avec ce son et son réglage. */
    private boolean gestureWith(int dimension, short[] sound, int volume) {
        if (broken || volume <= 0 || dimension < 1 || dimension > 10 ) return false;
        if (!allowed.test(SoundKind.DIMENSIONS)) return false;
        return send(new Pcm(pcm(sound, Math.min(100, volume) / 100.0), false));
    }

    /** Les sons des points gagnés, un par dimension (1 à 10). */
    private final short[][] gains = new short[11][];

    /**
     * Un point gagné dans la dimension {@code dimension}, dont la fenêtre est ouverte : une note claire, plus
     * haute d'une dimension à la suivante, sur la gamme pentatonique. Doux comme les gestes. Vrai s'il est parti.
     */
    boolean gain(int dimension, int volume) {
        if (broken || volume <= 0 || dimension < 1 || dimension > 10 ) return false;
        if (!allowed.test(SoundKind.DIMENSIONS)) return false;
        if (gains[dimension] == null) gains[dimension] = leveled(synthesizeGain(dimension), 1, REPEATED_DB);
        return send(new Pcm(pcm(gains[dimension], Math.min(100, volume) / 100.0), false));
    }

    /** Les rapports de la gamme pentatonique sur deux octaves : la note de chaque dimension au-dessus du sol (392 Hz). */
    private static final double[] GAIN_RATIOS = {1, 9 / 8.0, 5 / 4.0, 3 / 2.0, 5 / 3.0, 2, 9 / 4.0, 5 / 2.0, 3, 10 / 3.0};

    /** Le son d'un point gagné dans une dimension : une note brève et ronde, avec un peu d'octave. */
    static short[] synthesizeGain(int dimension) {
        double f = 392 * GAIN_RATIOS[dimension - 1];
        return render(0.14, t -> envelope(t, 0.002, 0.14) * Math.exp(-t * 18) * (0.4 * sine(f, t) + 0.12 * sine(2 * f, t)));
    }

    /**
     * Envoie des échantillons à la sortie, sur le fil des sons : la lecture ne bloque jamais le jeu. Faux s'il y a déjà
     * {@link #MAX_VOICES} sons qui jouent : celui-ci ne part pas.
     */
    private boolean send(Pcm pcm) {
        long now = System.nanoTime();
        synchronized (voiceEnds) {
            while (!voiceEnds.isEmpty() && voiceEnds.peek() <= now) voiceEnds.poll();
            if (voiceEnds.size() >= MAX_VOICES) return false;
            double seconds = pcm.bytes().length / 2.0 / (pcm.stereo() ? 2 : 1) / RATE;
            voiceEnds.add(now + (long) (seconds * 1e9));
        }
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

    /**
     * Fait entendre un son tout de suite : l'écoute d'un son depuis le panneau de test (le tic du métronome par son
     * propre chemin). Vrai s'il est parti.
     */
    boolean preview(Cue cue, int volume) {
        return cue == Cue.BEAT ? beat(volume) : play(cue, volume);
    }

    /** Le son d'essai du réglage du volume : joué même s'il vient de l'être. */
    void test(int volume) {
        play(Cue.ACHIEVEMENT, volume);
    }

    /** Le son du geste dans une dimension, court et sans saturation : chacun rappelle son mini-jeu (le clicker a son tapotement, {@link #CLICKER_TAP}). */
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
            failures = 0;
        } catch (Exception | LinkageError noAudio) {
            // Pas de carte son, système sans audio : le jeu continue en silence. Une sortie pleine un instant ne coupe
            // que ce son : il faut une suite d'échecs pour tout couper.
            if (++failures >= FAILURES_BEFORE_SILENCE) broken = true;
        }
    }

    /** L'explosion enregistrée et l'accord du Big Bang, mêlés en stéréo ; vide si la ressource manque ou se lit mal. */
    private static short[] bigBangStereo() {
        short[] recorded = stereo("big-bang.wav");
        if (recorded.length == 0) return recorded;
        short[] chord = synthesize(Cue.BIG_BANG);
        double[] mix = new double[Math.max(recorded.length, chord.length * 2)];
        for (int i = 0; i < recorded.length; i++) mix[i] = RECORDED_GAIN * recorded[i] / 32768.0;
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

    /**
     * Un enregistrement stéréo des ressources du jeu (16 bits, 44,1 kHz), en échantillons gauche, droite, gauche… ; vide
     * s'il manque ou se lit mal : le son synthétisé reste alors.
     */
    static short[] stereo(String resource) {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream(resource)) {
            if (raw == null) return new short[0];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 2 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0];
                byte[] bytes = in.readAllBytes();
                short[] out = new short[bytes.length / 2];
                for (int i = 0; i < out.length; i++) out[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                return out;
            }
        } catch (Exception unreadable) {
            return new short[0];
        }
    }

    private static short[][] seasons() {
        short[] all = monoResource("graine-saisons.wav");
        int each = (int) Math.round(SEASON_SECONDS * RATE);
        if (all.length < 4 * each) return new short[0][];
        short[][] out = new short[4][];
        for (int s = 0; s < 4; s++) out[s] = leveled(Arrays.copyOfRange(all, s * each, (s + 1) * each), 1, REPEATED_DB);
        return out;
    }

    /** Le passage de la saison {@code season} (0 le printemps à 3 l'hiver). */
    boolean season(int season, int volume) {
        short[] sound = SEASONS.length == 4 ? SEASONS[Math.floorMod(season, 4)] : null;
        return sound(Cue.SEASON, volume, sound);
    }

    private static short[][] clickerBonus() {
        short[][] out = new short[CLICKER_CHORD.length][];
        int gap = (int) (CLICKER_STRUM * RATE);
        for (int stack = 1; stack <= CLICKER_CHORD.length; stack++) {
            // Version 0.258 : le clic est trop bref pour un accord plaqué ; chaque note part un peu après la précédente.
            double[] mix = new double[CLICKER_TAP.length + stack * gap];
            for (int i = 0; i < CLICKER_TAP.length; i++) mix[i] = CLICKER_TAP[i];
            for (int note = 0; note < stack; note++) {
                short[] up = resampled(CLICKER_TAP, CLICKER_CHORD[note]);
                int at = (note + 1) * gap;
                for (int i = 0; i < up.length && at + i < mix.length; i++) mix[at + i] += up[i];
            }
            short[] chord = new short[mix.length];
            for (int i = 0; i < mix.length; i++) chord[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(mix[i] / (1 + stack))));
            out[stack - 1] = leveled(chord, 1, REPEATED_DB);
        }
        return out;
    }

    /**
     * Le clic du joueur sur le clicker, avec {@code stack} zones ×2 sous lui : le clic, ou son accord, plus aigu à
     * chaque zone ({@link #clickerSound(int)}). Un geste, comme ceux des autres dimensions (même réglage). Vrai s'il est parti.
     */
    boolean clicker(int stack, int volume) {
        return gestureWith(7, clickerSound(stack), volume);
    }

    /** Le demi-ton de plus par zone ×2 superposée (0.259), jusqu'à {@link #CLICKER_RISE_ZONES} zones : une montée légère. */
    static final double CLICKER_RISE = Math.pow(2, 1 / 12.0);
    static final int CLICKER_RISE_ZONES = 8;
    /** Le son du clicker pour chaque nombre de zones, calculé une fois (0 : le clic seul). */
    private static final short[][] CLICKER_SOUNDS = new short[CLICKER_RISE_ZONES + 1][];

    /** La hauteur du son du clicker sous {@code stack} zones : un demi-ton de plus par zone, au plus {@link #CLICKER_RISE_ZONES}. */
    static double clickerPitch(int stack) { return Math.pow(CLICKER_RISE, Math.max(0, Math.min(CLICKER_RISE_ZONES, stack))); }

    /**
     * Le son du clicker sous {@code stack} zones ×2 : le clic seul, ou l'accord arpégé (une note de plus par zone, jusqu'à
     * quatre), un peu plus aigu à chaque zone (0.259).
     */
    static short[] clickerSound(int stack) {
        int zones = Math.max(0, Math.min(CLICKER_RISE_ZONES, stack));
        synchronized (CLICKER_SOUNDS) {
            if (CLICKER_SOUNDS[zones] == null) {
                CLICKER_SOUNDS[zones] = zones == 0 ? CLICKER_TAP
                        : leveled(resampled(CLICKER_BONUS[Math.min(CLICKER_BONUS.length, zones) - 1], clickerPitch(zones)), 1, REPEATED_DB);
            }
            return CLICKER_SOUNDS[zones];
        }
    }

    /** Un enregistrement mono (16 bits, 44,1 kHz) ; vide s'il manque ou se lit mal. */
    static short[] monoResource(String resource) {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream(resource)) {
            if (raw == null) return new short[0];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0];
                byte[] bytes = in.readAllBytes();
                short[] out = new short[bytes.length / 2];
                for (int i = 0; i < out.length; i++) out[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                return out;
            }
        } catch (Exception unreadable) {
            return new short[0];
        }
    }

    /** Les notes du piano, découpées dans {@code piano.wav} ; aucune si la ressource manque ou se lit mal. */
    private static short[][] piano() {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream("piano.wav")) {
            if (raw == null) return new short[0][];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0][];
                byte[] bytes = in.readAllBytes();
                int each = (int) Math.round(PIANO_NOTE_SECONDS * RATE);
                if (bytes.length / 2 < each * PIANO_NOTES) return new short[0][];
                short[][] notes = new short[PIANO_NOTES][];
                for (int note = 0; note < PIANO_NOTES; note++) {
                    short[] out = new short[each];
                    for (int i = 0; i < each; i++) {
                        int at = 2 * (note * each + i);
                        out[i] = (short) ((bytes[at] & 0xff) | (bytes[at + 1] << 8));
                    }
                    notes[note] = leveled(out, 1, REPEATED_DB);
                }
                return notes;
            }
        } catch (Exception unreadable) {
            return new short[0][];
        }
    }

    private static short[] pianoWrong() {
        if (PIANO.length < 2) return new short[0];
        short[] out = new short[PIANO[0].length];
        for (int i = 0; i < out.length; i++) out[i] = (short) Math.round(0.45 * (PIANO[0][i] + PIANO[1][i]));
        return out;
    }

    /** L'enregistrement de la particule à la hauteur de chaque palier ; vide si la ressource manque ou se lit mal. */
    private static short[][] particleStages() {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream("particule.wav")) {
            if (raw == null) return new short[0][];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0][];
                byte[] bytes = in.readAllBytes();
                double[] source = new double[bytes.length / 2];
                double peak = 0;
                for (int i = 0; i < source.length; i++) {
                    source[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                    peak = Math.max(peak, Math.abs(source[i]));
                }
                short[][] stages = new short[idle.ui.particles.SpeedStages.COUNT][];
                for (int stage = 0; stage < stages.length; stage++) {
                    // Lu à ce rythme : un pas de plus d'un échantillon monte le son, un pas de moins le descend.
                    double step = idle.ui.particles.SpeedStages.pitch(stage);
                    short[] out = new short[(int) ((source.length - 1) / step)];
                    for (int i = 0; i < out.length; i++) {
                        double at = i * step;
                        int low = (int) at;
                        double value = source[low] + (source[low + 1] - source[low]) * (at - low);
                        out[i] = (short) Math.round(value / peak * PARTICLE_PEAK * Short.MAX_VALUE);
                    }
                    stages[stage] = leveled(out, 1, REPEATED_DB);
                }
                return stages;
            }
        } catch (Exception unreadable) {
            return new short[0][];
        }
    }

    /** Le son lu {@code pitch} fois plus vite : plus aigu et plus court (interpolé entre deux échantillons). */
    static short[] resampled(short[] sound, double pitch) {
        short[] out = new short[Math.max(0, (int) ((sound.length - 1) / pitch))];
        for (int i = 0; i < out.length; i++) {
            double at = i * pitch;
            int low = (int) at;
            out[i] = (short) Math.round(sound[low] + (sound[low + 1] - sound[low]) * (at - low));
        }
        return out;
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
            // Un défi réussi, si defi-reussi.wav manque.
            case VICTORY -> render(0.75, t -> notes(t, 0.12, 0.75, 523.25, 659.25, 783.99));
            // Deux notes claires.
            case ACHIEVEMENT -> render(0.55, t -> notes(t, 0.11, 0.55, 659.25, 987.77));
            // Un scintillement, une octave sous celui d'avant la 0.247.
            case COMET -> render(0.7, t -> envelope(t, 0.01, 0.7)
                    * (0.35 * sine(880 + 200 * Math.sin(t * 30), t) + 0.25 * sine(1318.5, t) * Math.exp(-t * 4)));
            // Une cloche : une fondamentale et un partiel inharmonique.
            // (Une octave plus grave depuis la 0.247.)
            // Un nouvel élément, si nouvel-element.wav manque.
            case ELEMENT -> render(0.6, t -> envelope(t, 0.004, 0.6) * (0.5 * sine(440, t) + 0.25 * sine(440 * 2.76, t) * Math.exp(-t * 5)));
            // Un petit bip doux, une octave plus grave depuis la 0.247 : l'annonce de collision, et le palier de vitesse si palier-vitesse.wav manque.
            case LEVEL, COLLISION_NEWS -> render(0.18, t -> envelope(t, 0.005, 0.18) * 0.45 * sine(523.25, t));
            // Un accord parfait qui s'élargit, la quinte et l'octave par-dessus, sur une basse qui tient.
            // L'univers formé si univers.wav manque, la fin de la partie si fin.wav manque : leur son d'avant.
            case UNIVERSE, GAME_END -> render(2.4, t -> envelope(t, 0.15, 2.4) * (0.25 * sine(130.8, t) + 0.12 * sine(261.6, t))
                    + notes(t, 0.18, 2.4, 523.25, 659.25, 783.99, 1046.5));
            // Une fanfare de deux notes, la seconde à la quinte, sur un écho grave.
            // Un palier du défi final, si defi-palier.wav manque.
            case FINALE -> render(1.0, t -> notes(t, 0.16, 1.0, 587.33, 880) + envelope(t, 0.01, 1.0) * 0.2 * sine(146.8, t));
            // Une pluie de trois notes aiguës, très vite.
            case SKIN -> render(0.5, t -> notes(t, 0.05, 0.5, 1567.98, 1975.53, 2349.32));
            // Deux notes graves, la seconde un demi-ton plus haut : une contrainte qui s'annonce.
            // Un défi qui s'ouvre ou commence, si defi-ouvert.wav manque.
            case CHALLENGE -> render(0.6, t -> notes(t, 0.2, 0.6, 293.66, 311.13));
            // Un son sombre qui enfle : une quinte grave et un battement.
            case DARK -> render(0.9, t -> envelope(t, 0.2, 0.9) * (0.35 * sine(110, t) + 0.25 * sine(164.8, t) + 0.1 * sine(111.5, t)));
            // Une note claire et sa quinte, ensemble.
            case MILESTONE -> render(0.45, t -> envelope(t, 0.005, 0.45) * (0.3 * sine(784, t) + 0.2 * sine(1174.7, t)));
            // Un petit « tchip » de caisse : le prix payé.
            // L'achat : le bip d'avant la 0.263, si achat.wav manque.
            case PURCHASE -> render(0.07, t -> envelope(t, 0.001, 0.07) * Math.exp(-t * 45) * (0.35 * sine(1760, t) + 0.15 * sine(2637, t)));
            // Un interrupteur : un bruit bref, une note sourde.
            case TOGGLE -> render(0.05, t -> envelope(t, 0.0005, 0.05) * (0.3 * noise(t) * Math.exp(-t * 300) + 0.3 * sine(523.25, t) * Math.exp(-t * 60)));
            // Version 0.213, les bruitages. Une particule : un point de lumière, très bref et léger.
            case PARTICLE -> render(0.05, t -> envelope(t, 0.001, 0.05) * Math.exp(-t * 60) * 0.3 * sine(2349.3, t));
            // Une fusion : deux particules qui se rejoignent, une hauteur qui monte.
            case ATOM -> render(0.14, t -> envelope(t, 0.002, 0.14) * 0.4 * sine(1, 600 * t + 300 * t * t / 0.28));
            // Un atome créé (si l'enregistrement manque) : deux notes claires, une quinte qui monte.
            case NEW_ATOM -> render(0.3, t -> notes(t, 0.05, 0.3, 880, 1318.5));
            // Une sonde lancée (si l'enregistrement manque) : un souffle qui monte et s'éloigne, sur un grondement de moteur.
            case PROBE -> render(1.1, t -> envelope(t, 0.05, 1.1) * (0.25 * noise(t) * (0.3 + t) + 0.3 * sine(1, 80 * t + 400 * t * t)));
            // Une note du miroir (si le piano manque) : une note claire.
            case MIRROR -> render(0.6, t -> envelope(t, 0.003, 0.6) * Math.exp(-t * 5) * 0.4 * sine(523.25, t));
            // Une collision (si l'enregistrement manque) : un choc bref et un tintement de métal.
            case COLLISION -> render(0.25, t -> envelope(t, 0.001, 0.25) * (0.2 * noise(t) * Math.exp(-t * 60)
                    + 0.3 * sine(1200, t) * Math.exp(-t * 12) + 0.15 * sine(1797, t) * Math.exp(-t * 16)));
            // Des hadrons : trois quarks liés, trois notes presque ensemble.
            case HADRON -> render(0.35, t -> notes(t, 0.03, 0.35, 783.99, 987.77, 1174.66));
            // Le vide : une paire née de rien, un souffle grave qui enfle et retombe.
            case VOID -> render(0.6, t -> envelope(t, 0.15, 0.6) * (0.3 * sine(220, t) + 0.15 * sine(110, t)));
            // Un palier de la graine, si son enregistrement manque : quatre notes qui poussent vers le haut.
            case SEED -> render(0.9, t -> notes(t, 0.09, 0.9, 523.25, 659.25, 783.99, 1046.5));
            // Le rond de croissance (si l'enregistrement manque) : une bulle qui remonte.
            case BLOOM -> render(0.4, t -> envelope(t, 0.005, 0.4) * 0.4 * sine(1, 240 * t + 600 * t * t));
            // Le passage d'une saison (si l'enregistrement manque) : deux notes de bois.
            case SEASON -> render(0.6, t -> notes(t, 0.1, 0.6, 523.25, 783.99));
            // Un clic : un tic sec et léger.
            // Un clic de bouton, si son enregistrement manque.
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
