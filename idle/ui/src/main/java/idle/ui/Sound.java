package idle.ui;

import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/**
 * Les sons du jeu : la plupart sont des enregistrements (dans les ressources), les autres sont synthétisés (une courte
 * somme de sinusoïdes sous une enveloppe). Chacun est préparé une fois, mis à son niveau, puis rejoué au volume du
 * moment ; leur fabrication est dans {@link SoundSamples}.
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
final class Sound extends SoundSamples {
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
        /**
         * Version 0.278 : toute autre annonce, celles qui n'ont pas de son à elles (l'enregistrement du joueur,
         * {@code annonce.wav}) ; le moins important de tous, il ne part que si rien d'autre ne sonne.
         */
        NOTICE,
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

    /** Le niveau visé pour ce son. */
    static double targetDb(Cue cue) {
        if (cue == Cue.ELEMENT) return RARE_DB + ELEMENT_DB;
        if (cue == Cue.ACHIEVEMENT) return RARE_DB + ACHIEVEMENT_DB;
        if (cue == Cue.NOTICE) return RARE_DB + NOTICE_DB;
        return cue == Cue.COLLISION ? COLLISION_DB : REPEATED.contains(cue) ? REPEATED_DB : RARE_DB;
    }

    /**
     * Version 0.247 : au plus ce nombre de sons qui jouent en même temps, tous confondus ; au-delà, un nouveau son ne part
     * pas. Chaque son prend une ligne de la sortie audio, qui n'en a pas sans fin.
     */
    static final int MAX_VOICES = 24;
    /** Au plus ce nombre de particules qui sonnent en même temps : la sortie audio n'est pas noyée sous les générateurs rapides. */
    static final int MAX_PARTICLE_VOICES = 6;

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
    /**
     * Un jalon (version 0.277) : le son choisi par le joueur ({@code jalon.wav}, « rollover1 », 0,22 s en stéréo), au
     * niveau des sons rares. Il sert à toutes les annonces de jalon (cosmos, ensembles, gluons, racines, titres, premier
     * atome…). Vide si le fichier manque : les deux notes synthétisées.
     */
    static final short[] MILESTONE_STEREO = leveled(stereo("jalon.wav"), 2, RARE_DB);
    /**
     * Un skin débloqué (version 0.277) : le son choisi par le joueur ({@code skin.wav}, « switch_006 », deux déclics de
     * 0,15 s, l'un à gauche, l'autre à droite), au niveau des sons rares. Vide si le fichier manque : les trois notes.
     */
    static final short[] SKIN_STEREO = leveled(stereo("skin.wav"), 2, RARE_DB);
    /**
     * Les hadrons créés (version 0.278) : le son choisi par le joueur ({@code hadron.wav}, une montée de 2,1 s en
     * stéréo), au niveau des sons répétitifs ; deux au plus en même temps ({@link #MAX_HADRON_VOICES}).
     */
    static final short[] HADRON_STEREO = leveled(stereo("hadron.wav"), 2, REPEATED_DB);
    /** Version 0.278 : au plus deux sons de hadrons à la fois, longs de deux secondes : ils ne prennent pas toutes les lignes. */
    static final int MAX_HADRON_VOICES = 2;
    /**
     * Toute autre annonce (version 0.278) : le son choisi par le joueur ({@code annonce.wav}, « rollover6 », 0,17 s en
     * stéréo), 6 dB sous les sons rares, comme le nouvel élément.
     */
    static final short[] NOTICE_STEREO = leveled(stereo("annonce.wav"), 2, targetDb(Cue.NOTICE));
    /** Version 0.278 : la suite d'achats des récompenses d'une sonde, une toutes les 70 ms, douze au plus. */
    static final double PURCHASE_RUN_GAP = 0.07;
    static final int PURCHASE_RUN_MAX = 12;
    /**
     * Un succès (version 0.277) : le son choisi par le joueur ({@code succes.wav}, « toggle_002 », 0,14 s), au niveau des
     * sons rares ; aussi le palier du collisionneur et l'essai du volume, qui partageaient ce son. Vide si le fichier
     * manque : les deux notes.
     */
    static final short[] ACHIEVEMENT_SOUND = leveled(monoResource("succes.wav"), 1, targetDb(Cue.ACHIEVEMENT));
    /** Version 0.279 : le succès, 6 dB sous les sons rares, à la demande du joueur. */
    static final double ACHIEVEMENT_DB = -6;
    /** Version 0.278 : les autres annonces, 6 dB sous les sons rares. */
    static final double NOTICE_DB = -6;
    static final short[] ELEMENT_STEREO = leveled(stereo("nouvel-element.wav"), 2, targetDb(Cue.ELEMENT));
    /** Le nouvel élément, moins fort que les autres sons rares : −6 dB en 0.276, −12 dB depuis la 0.279 (à peu près quatre fois moins fort à l'oreille). */
    static final double ELEMENT_DB = -12;
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
        if (silent(text)) return null;
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

    /**
     * Le son d'une fournée d'annonces une fois triée (version 0.278) : {@code strongest} s'il y en a un ; sinon, si une
     * annonce n'a pas de son à elle (et n'est pas muette, {@link #silent}), le son des autres annonces
     * ({@link Cue#NOTICE}) ; sinon rien.
     */
    static Cue orNotice(Cue strongest, List<String> news) {
        if (strongest != null) return strongest;
        for (String text : news) if (cueOf(text) == null && !silent(text)) return Cue.NOTICE;
        return null;
    }

    /** Les annonces muettes, voulues sans son (version 0.279 : le quark top, qui sonnait comme une comète). */
    static boolean silent(String text) { return text.startsWith("Collision") && text.contains("quark top"); }

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
        if (cue == Cue.HADRON) return hadron(volume);
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

    /** Des hadrons créés : leur son, deux au plus à la fois (0.278), car il dure deux secondes. Vrai s'il est parti. */
    private boolean hadron(int volume) {
        long now = System.nanoTime();
        while (!hadronEnds.isEmpty() && hadronEnds.peekFirst() <= now) hadronEnds.pollFirst();
        if (hadronEnds.size() >= MAX_HADRON_VOICES) return false;
        if (!click(Cue.HADRON, volume)) return false;
        double seconds = HADRON_STEREO.length > 0 ? HADRON_STEREO.length / 2.0 / RATE : 0.35;
        hadronEnds.addLast(now + (long) (seconds * 1e9));
        return true;
    }

    /**
     * Les récompenses d'une sonde ouvertes d'un coup (version 0.278) : le son d'achat une fois par carte, une toutes les
     * {@link #PURCHASE_RUN_GAP} secondes ({@link #purchaseRun}). Vrai s'il est parti.
     */
    boolean purchases(int cards, int volume) {
        if (cards <= 0) return false;
        if (cards == 1) return click(Cue.PURCHASE, volume);
        return sound(Cue.PURCHASE, volume, purchaseRun(cards));
    }

    /** Le son d'achat {@code cards} fois de suite (douze au plus), mêlés sans saturer. */
    static short[] purchaseRun(int cards) {
        short[] one = PURCHASE_SOUND.length > 0 ? PURCHASE_SOUND : leveledSound(Cue.PURCHASE);
        int count = Math.max(1, Math.min(PURCHASE_RUN_MAX, cards)), gap = (int) (PURCHASE_RUN_GAP * RATE);
        double[] mix = new double[one.length + (count - 1) * gap];
        for (int k = 0; k < count; k++) for (int i = 0; i < one.length; i++) mix[k * gap + i] += one[i];
        short[] out = new short[mix.length];
        for (int i = 0; i < out.length; i++) out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(mix[i])));
        return out;
    }

    /** L'enregistrement mono de ce son, ou {@code null} (le rond de croissance, le printemps par défaut, la sonde, le clic, l'achat, le palier de vitesse, le palier du défi final, le défi réussi, le défi qui s'ouvre, le succès). */
    private static short[] monoRecorded(Cue cue) {
        if (cue == Cue.BLOOM && BLOOM_SOUND.length > 0) return BLOOM_SOUND;
        if (cue == Cue.SEASON && SEASONS.length > 0) return SEASONS[0];
        if (cue == Cue.PROBE && PROBE_SOUND.length > 0) return PROBE_SOUND;
        if (cue == Cue.CLICK && CLICK_SOUND.length > 0) return CLICK_SOUND;
        if (cue == Cue.PURCHASE && PURCHASE_SOUND.length > 0) return PURCHASE_SOUND;
        if (cue == Cue.LEVEL && SPEED_SOUND.length > 0) return SPEED_SOUND;
        if (cue == Cue.FINALE && FINALE_TIER_SOUND.length > 0) return FINALE_TIER_SOUND;
        if (cue == Cue.VICTORY && VICTORY_SOUND.length > 0) return VICTORY_SOUND;
        if (cue == Cue.ACHIEVEMENT && ACHIEVEMENT_SOUND.length > 0) return ACHIEVEMENT_SOUND;
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
        if (cue == Cue.MILESTONE && MILESTONE_STEREO.length > 0) return MILESTONE_STEREO;
        if (cue == Cue.SKIN && SKIN_STEREO.length > 0) return SKIN_STEREO;
        if (cue == Cue.HADRON && HADRON_STEREO.length > 0) return HADRON_STEREO;
        if (cue == Cue.NOTICE && NOTICE_STEREO.length > 0) return NOTICE_STEREO;
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
    /** La fin de chaque son de hadron qui joue encore, en nanosecondes (0.278). */
    private final ArrayDeque<Long> hadronEnds = new ArrayDeque<>();

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

    /** Le passage de la saison {@code season} (0 le printemps à 3 l'hiver). */
    boolean season(int season, int volume) {
        short[] sound = SEASONS.length == 4 ? SEASONS[Math.floorMod(season, 4)] : null;
        return sound(Cue.SEASON, volume, sound);
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

}
