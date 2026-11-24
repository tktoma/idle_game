package idle.core;

import idle.core.state.*;
import idle.core.achievements.*;
import idle.core.collider.*;
import idle.core.darkmatter.*;
import idle.core.profile.Title;
import idle.core.universe.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.*;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameUniverse} (découpée en 0.293 pour rester sous 1 000 lignes) : les champs, les types (améliorations du vide, des dimensions, de la graine, bulles) et le vide. */
public abstract class GameUniverseBase extends GameAtoms {
    // Ce que fournissent les parties suivantes.
    abstract public boolean canStartNextUniverse();
    abstract public boolean isDurationThresholdReached();
    abstract public boolean isLoopThresholdReached();
    abstract public boolean isVolumeThresholdReached();
    abstract public boolean startNextUniverse();
    abstract public double hoseRate();
    abstract public double wateringCanRate();
    abstract public int seedStage();
    abstract public long seedSoilFactor();
    abstract void formUniverse();
    abstract void noteSkinUnlocks();
    abstract void runTrial(double dt);

    GameUniverseBase(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract void forgetDerived();

    public abstract BigNum darkMatterEarned();

    public abstract long gatheredInState(Molecule.State matter);

    public abstract boolean isGalaxyUnlocked();

    public abstract boolean canFormGalaxy();

    public abstract boolean keepsUpgradesOnFusion();

    // Version 0.188 à 0.190 : ce que les conditions des skins lisent plus bas dans la chaîne des classes.
    public abstract int moleculeKindsCreated();

    public abstract int substancesFormed();

    public abstract long moleculesCreated();

    public abstract int assembliesFormed();

    public abstract int baseChallengesCompleted();

    public abstract boolean isBigBangUnlocked();

    public abstract boolean hasTitle(Title title);

    // Ce que lisent les skins des hadrons et du vide, définis dans GameHadrons et GameVoid.
    public abstract long hadronsMade();

    public abstract double voidPairsBorn();

    public abstract int voidHoleUniverses();

    // Ce que lisent les skins de l'établi de la graine, comptés depuis l'ouverture de la fenêtre dans GameLoop,
    // GameClicker et GameMirror : les ronds de croissance cueillis, les orages, les cycles complets des saisons.
    public abstract long bloomsCaught();

    public abstract long storms();

    public abstract int seasonCycles();

    // Les saisons figées de l'arbre lisent les saisons débloquées, définies dans GameMirror.
    public abstract boolean areSeasonsUnlocked();

    // Les albums du profil lisent les catalogues des assemblages et des astres, définis dans GameMolecules.
    public abstract List<Assembly> assemblies();

    public abstract List<Body> bodies();

    // ------------------------------------------------------------------
    // Le cosmos : la galaxie, l'amas de galaxies, l'univers
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // La partie secondaire : les univers, sans logique pour l'instant
    // ------------------------------------------------------------------

    /** Nombre de dimensions à ouvrir, une par univers jusqu'au dixième. */
    public static final int MAX_DIMENSIONS = 10;

    /** Univers créés : 0 avant l'univers, 1 une fois formé, plus ceux d'après (créés pour l'instant par les outils d'essai). */
    @Override
    public int universes() { return isUniverseFormed() ? 1 + state.extraUniverses() : 0; }

    /** Vrai une fois un univers formé, même pendant la partie qui mène au suivant. */
    public boolean isUniverseFormed() {
        return state.universeFormed() || state.cosmosLevel() >= Cosmos.values().length;
    }

    /** Vrai dès le premier univers (version 0.100 ; avant, au deuxième) : la sous-page des dimensions s'ouvre. */
    public boolean isDimensionsUnlocked() { return universes() >= 1; }

    /** Dimensions ouvertes : une par univers, la première dès le premier, dix au plus. */
    public int dimensionsUnlocked() { return isDimensionsUnlocked() ? Math.min(MAX_DIMENSIONS, universes()) : 0; }

    /** Outil d'essai : un univers de plus, une fois le premier formé. Faux s'il n'y en a pas encore. */
    public boolean testAddUniverse() {
        if (!isUniverseFormed()) return false;
        state.setExtraUniverses(state.extraUniverses() + 1);
        return true;
    }

    /** Outil d'essai : un univers de moins, sans descendre sous le premier. */
    public boolean testRemoveUniverse() {
        if (state.extraUniverses() <= 0) return false;
        state.setExtraUniverses(state.extraUniverses() - 1);
        return true;
    }

    /** Outil d'essai : des graines d'univers en plus. */
    public void testAddUniverseSeeds(BigNum amount) { state.setUniverseSeeds(state.universeSeeds().add(amount)); }

    /**
     * Le vide, deuxième acte de la partie secondaire. Depuis la version 0.129, il se propose au
     * premier Big Bang du troisième univers ({@link #isVoidOffered()}) et ne s'ouvre qu'une fois payé en énergie du
     * collisionneur ; vrai une fois ouvert dans cet univers.
     */
    public boolean isVoidUnlocked() {
        // La bulle du géant de gauche, en bas : le vide toujours ouvert, sans le payer.
        return state.voidOpen() || hasBubble(TreeBubble.GIANT_LEFT_LOW);
    }

    /** Vrai quand le vide se propose dans le collisionneur : premier Big Bang du troisième univers passé, pas encore ouvert. */
    public boolean isVoidOffered() { return universes() >= 2 && state.bigBangs() > 0 && !isVoidUnlocked(); }

    // ------------------------------------------------------------------
    // Les améliorations du vide, et l'allègement des besoins du ciel
    // ------------------------------------------------------------------

    /**
     * Les huit améliorations du vide, payées en énergie du vide : leur prix de départ, la croissance, le plafond. Dans
     * l'ordre de la sauvegarde : gain par paire, rythme, coût en énergie, espace par paire, fonte, allègement, maturation,
     * et depuis la version 0.132 le rassemblement. Les cinq qui font grandir le vide montent bien plus vite
     * de prix (×3,7 à ×4,1 par niveau, au lieu de ×2,2 à ×2,6), pour que le vide ne se remplisse pas en une heure ; les
     * trois qui raccourcissent le troisième acte vont plus loin (maturation −70 %, rassemblement −80 %) et montent de
     * prix moins vite (×2) : elles se gagnent au fil de l'acte, mesurées sur le Speedrunner du troisième univers.
     *
     * <p>Les prix suivent l'énergie du vide de la partie, comme les bulles de l'arbre. Le premier niveau
     * coûte de l'ordre des premières bulles (500 à 5 000, la graine en vaut 5 200), le dernier de l'ordre des dernières
     * (70 000 à 600 000, l'arbre-monde vaut 69 300 et les derniers rachats 400 000 à 840 000) ; chaque niveau entre les
     * deux multiplie le prix d'autant. Le gain par paire n'a plus de niveau sans fin : quinze au plus. Le rassemblement et
     * la maturation raccourcissent de moitié au plus.
     *
     * <p>Le premier niveau dix fois moins cher (50 à 500), à la portée de l'énergie d'un seul univers,
     * puisque les améliorations du vide repartent de zéro à chaque univers ; le dernier trente fois plus cher (2,1 à
     * 18 millions), à la portée de l'équilibre du vide en fin de partie. Le prix monte donc plus vite d'un niveau à
     * l'autre (×1,9 à ×8,1), et les achats s'étalent d'un univers à l'autre au lieu de tous tomber dans le même : essayé
     * sur le Speedrunner de la 0.169 (graine 201), l'allègement, la maturation et le rassemblement sont au plafond à
     * l'univers 7, le gain, l'énergie et la fonte à deux ou trois niveaux du leur.
     */
    public enum VoidUpgrade implements Priced {
        GAIN(50, 12_000_000, 15), RATE(80, 15_000_000, 20), ENERGY(60, 9_000_000, 15), SPACE(60, 9_000_000, 15),
        DECAY(100, 18_000_000, 15), LIGHTEN(200, 6_000_000, 10), MATURE(150, 4_500_000, 10), SETTLE(500, 2_100_000, 5);

        final Price price;

        /** Du prix du premier niveau à celui du dernier, en {@code max} niveaux. */
        VoidUpgrade(double first, double last, int max) {
            price = new Price(first, Math.pow(last / first, 1.0 / (max - 1)), max);
        }

        @Override
        public Price price() { return price; }
    }

    /** Le prix d'une amélioration à niveaux : {@code base × growth^niveau}, jusqu'au niveau {@code max} exclu. */
    public record Price(double base, double growth, int max) {
    }

    /** Une amélioration à niveaux dont le prix suit un {@link Price} : ce que partagent les améliorations des dimensions, du vide et de la graine. */
    public interface Priced {
        Price price();

        /** Le prix du premier niveau. */
        default double baseCost() { return price().base(); }

        /** Ce par quoi le prix est multiplié à chaque niveau. */
        default double growth() { return price().growth(); }

        /** Le niveau le plus haut. */
        default int max() { return price().max(); }

        /** Le prix du niveau suivant {@code level} ; infini une fois au plus haut. */
        default double cost(int level) {
            return level >= max() ? Double.POSITIVE_INFINITY : baseCost() * Math.pow(growth(), level);
        }
    }

    /** L'espace que tiennent les paires du vide encore vivantes ({@link GameVoid}) : il compte dans l'espace utilisé. */
    public BigNum voidHeldSpace() { return BigNum.ZERO; }

    /** Ce que chaque niveau d'allègement retire aux besoins des astres, de l'amas et de l'univers : 3 %, dix niveaux au plus. */
    public static final double VOID_LIGHTEN_STEP = 0.03;

    /** Le niveau d'une amélioration du vide, jamais au-dessus de son plafond (version 0.169 : une sauvegarde d'avant peut le dépasser). */
    public int voidLevel(VoidUpgrade upgrade) { return Math.min(upgrade.max(), state.voidLevel(upgrade.ordinal())); }

    /** La part des besoins du ciel qui reste avec l'allègement du vide : 1 sans lui, 0,7 au plus fort. */
    public double needShare() {
        return 1 - VOID_LIGHTEN_STEP * Math.min(VoidUpgrade.LIGHTEN.max(), voidLevel(VoidUpgrade.LIGHTEN));
    }

    /**
     * Ce que chaque niveau de rassemblement du vide retire au délai de l'amas et de l'univers : 10 % depuis la version 0.169
     * (16 % depuis la 0.132), cinq niveaux, −50 % au plus.
     */
    public static final double VOID_SETTLE_STEP = 0.10;

    /** La part du délai de rassemblement qui reste avec le vide : 1 sans l'amélioration. */
    public double settleShare() {
        return 1 - VOID_SETTLE_STEP * Math.min(VoidUpgrade.SETTLE.max(), voidLevel(VoidUpgrade.SETTLE));
    }

    /** Un besoin après l'allègement : arrondi au-dessus, jamais sous 1. */
    public int lightened(int need) {
        double share = needShare();
        if (share >= 1 || need <= 0) return need;
        return Math.max(1, (int) Math.ceil(need * share - 1e-9));
    }

    Map<String, Integer> lightenedMolecules(Map<String, Integer> raw) {
        if (needShare() >= 1) return raw;
        Map<String, Integer> out = new LinkedHashMap<>();
        raw.forEach((id, need) -> out.put(id, lightened(need)));
        return out;
    }

    Map<Molecule.State, Integer> lightenedMatter(Map<Molecule.State, Integer> raw) {
        if (needShare() >= 1) return raw;
        Map<Molecule.State, Integer> out = new EnumMap<>(Molecule.State.class);
        raw.forEach((matter, need) -> out.put(matter, lightened(need)));
        return out;
    }

    /** Les molécules que demande cet astre, allégées par le vide. */
    public Map<String, Integer> bodyMolecules(Body body) { return lightenedMolecules(body.molecules()); }

    /** La matière que demande cet astre, par état, allégée par le vide. */
    public Map<Molecule.State, Integer> bodyMatter(Body body) { return lightenedMatter(body.matter()); }

    /** La matière que demande cette échelle du cosmos (l'amas, l'univers), par état, allégée par le vide. */
    public Map<Molecule.State, Integer> cosmosMatter(Cosmos scale) { return lightenedMatter(scale.matter()); }

    // ------------------------------------------------------------------
    // La ligne, première dimension
    // ------------------------------------------------------------------

    /** Secondes que met la boule de la ligne pour aller d'un bout à l'autre, avant l'accélération. */
    public static final double LINE_HALF_TRIP = 2.4;

    /** Éclats de dimension que donne chaque trait touché, avant l'amélioration. */
    public static final double LINE_SHARD = 0.1;

    /** Traits ajoutés au plus entre les deux bouts : la ligne en compte alors quarante. */
    public static final int LINE_MAX_EXTRA_TICKS = 38;

    /** Chaque niveau de vitesse : +25 % de vitesse, vingt niveaux au plus (×6). */
    public static final double LINE_SPEED_STEP = 0.25;

    public static final int LINE_MAX_SPEED = 20;

    /** Boules et lignes en plus, au plus : dix boules par ligne, cinq lignes. */
    public static final int LINE_MAX_EXTRA_BALLS = 9, LINE_MAX_EXTRA_LINES = 4;

    /**
     * Le seuil de la ligne : tant d'éclats gagnés en tout, et la dimension 1 est marquée. Version 0.136 :
     * 250 000 au lieu de 100, qu'un joueur appliqué atteignait en 3 minutes ; il lui en faut maintenant environ 15 (bot
     * {@code DimensionBots} des bots).
     */
    public static final double LINE_THRESHOLD = 250_000;

    /** Prix des deux premières améliorations de la ligne, en éclats, et leur croissance à chaque niveau. */
    public static final double LINE_YIELD_COST = 1, LINE_TICKS_COST = 2, LINE_COST_GROWTH = 3;

    /** Les améliorations de la ligne : leur prix de départ en éclats, et la croissance à chaque niveau. */
    public enum LineUpgrade implements Priced {
        YIELD(LINE_YIELD_COST, LINE_COST_GROWTH, Integer.MAX_VALUE), TICKS(LINE_TICKS_COST, LINE_COST_GROWTH, LINE_MAX_EXTRA_TICKS),
        SPEED(5, 3, LINE_MAX_SPEED), BALLS(10, 4, LINE_MAX_EXTRA_BALLS), LINES(25, 5, LINE_MAX_EXTRA_LINES);

        final Price price;

        LineUpgrade(double cost, double growth, int max) { price = new Price(cost, growth, max); }

        @Override
        public Price price() { return price; }
    }

    /** Éclats du plan à gagner pour son seuil : 3 500 depuis la version 0.136 (100 avant), environ 15 minutes de jeu appliqué. */
    public static final double PLANE_THRESHOLD = 3_500;

    /** Vrai si la dimension {@code number} (1 la ligne, 2 le plan, 3 le volume, 4 la durée, 5 la boucle, 6 la brane, 7 le clicker, 8 la grille, 9 le miroir, 10 le mélange) a atteint son seuil ; faux pour celles qui n'existent pas encore. */
    /**
     * Le seuil d'une dimension, en éclats gagnés : un quart d'heure de jeu appliqué (version 0.179 : c'est aussi la
     * mesure qui fixe les prix de la graine, {@link SeedUpgrade}).
     */
    public static double dimensionThreshold(int number) {
        return switch (number) {
            case 1 -> LINE_THRESHOLD;
            case 2 -> PLANE_THRESHOLD;
            case 3 -> VOLUME_THRESHOLD;
            case 4 -> DURATION_THRESHOLD;
            case 5 -> LOOP_THRESHOLD;
            case 6 -> BRANE_THRESHOLD;
            case 7 -> CLICKER_THRESHOLD;
            case 8 -> GRID_THRESHOLD;
            case 9 -> MIRROR_THRESHOLD;
            case 10 -> MIX_THRESHOLD;
            default -> Double.POSITIVE_INFINITY;
        };
    }

    // ------------------------------------------------------------------
    // Le plan, deuxième dimension
    // ------------------------------------------------------------------

    /** Le plan va de −1 à 1 dans les deux sens : le point du joueur n'en sort pas. */
    public static final double PLANE_EDGE = 1;

    /**
     * Rayon des éclats qui apparaissent, et rayon de départ du point du joueur : 0,025 depuis la version
     * 0.107 (0,06 avant), pour que l'amélioration de taille compte.
     */
    public static final double PLANE_SHARD_RADIUS = 0.03, PLANE_PLAYER_RADIUS = 0.025;

    /** Chaque niveau de taille : +25 % de rayon (version 0.107 ; +15 % avant). */
    public static final double PLANE_SIZE_STEP = 0.25;

    /** Au départ : un éclat toutes les 3 s, trois au plus à la fois, et le point parcourt 0,6 par seconde. */
    public static final double PLANE_SPAWN_SECONDS = 3, PLANE_SPEED = 0.6;

    public static final int PLANE_MAX_SHARDS = 3;

    /** Les améliorations du plan : leur prix de départ en éclats du plan, la croissance, le plafond. */
    public enum PlaneUpgrade implements Priced {
        VALUE(5, 3, Integer.MAX_VALUE), SPAWN(3, 2.5, 15), COUNT(4, 3, 17), SPEED(3, 2.5, 15), SIZE(4, 2.5, 15);

        final Price price;

        PlaneUpgrade(double cost, double growth, int max) { price = new Price(cost, growth, max); }

        @Override
        public Price price() { return price; }
    }

    /** Un éclat posé sur le plan : il attend qu'on passe dessus. */
    public record PlaneShard(double x, double y) {
    }

    // Ce qui ne se sauvegarde pas : où est le point, où sont les éclats, la direction tenue.
    double planeX, planeY, planeInputX, planeInputY, planeSpawnTimer;

    final List<PlaneShard> planeShardsOnBoard = new ArrayList<>();

    final Random planeRandom = new Random(2);

    long planeCollected;

    static double clampPlane(double value, double radius) {
        return Math.max(-PLANE_EDGE + radius, Math.min(PLANE_EDGE - radius, value));
    }

    /**
     * Le morceau de temps des automatismes de jeu sur un grand pas : celui du plan, du volume et de la
     * brane. Assez court pour que le point du plan ne passe à côté d'aucun éclat ; en jeu ouvert, une image dure moins.
     */
    public static final double PLANE_AUTO_STEP = 0.05;

    // ------------------------------------------------------------------
    // Le volume, troisième dimension
    // ------------------------------------------------------------------

    /** Le volume est le cube de −1 à 1 sur les trois axes ; le joueur y plonge et regarde depuis son centre. */
    public static final double VOLUME_EDGE = 1;

    /**
     * Rayon d'une boule au départ, et ce que chaque niveau de taille lui ajoute (+20 %). Petite au départ (le cube va
     * de −1 à 1) : il faut viser, et l'amélioration de taille compte.
     */
    public static final double VOLUME_BALL_RADIUS = 0.025, VOLUME_SIZE_STEP = 0.2;

    /** Les boules naissent entre ces deux distances du centre : jamais sur le joueur, jamais dans un coin hors de vue. */
    public static final double VOLUME_SPAWN_NEAR = 0.35, VOLUME_SPAWN_FAR = 0.9;

    /** Au départ : une boule toutes les 1,5 s, quatre au plus à la fois. */
    public static final double VOLUME_SPAWN_SECONDS = 1.5;

    public static final int VOLUME_MAX_BALLS = 4;

    /** Une plongée dure 12 s au départ, et chaque niveau de durée ajoute 3 s : le cube rejette le joueur à la fin. */
    public static final double VOLUME_DIVE_SECONDS = 12, VOLUME_DIVE_STEP = 3;

    /**
     * Éclats du volume (ses sphères) à gagner pour son seuil, qui lui met le rond vert (jusqu'à la 0.138, il ouvrait
     * aussi le palier 4 de la graine). 8 000 depuis la version 0.136 (100 avant), environ 15 minutes de jeu appliqué.
     */
    public static final double VOLUME_THRESHOLD = 8_000;

    /**
     * Les améliorations du volume, qui s'achètent hors du cube : leur prix de départ en éclats du volume, la
     * croissance, le plafond. Dans l'ordre de la sauvegarde : durée, valeur, réapparition, nombre, taille.
     */
    public enum VolumeUpgrade implements Priced {
        TIME(3, 2.2, 20), VALUE(5, 3, Integer.MAX_VALUE), SPAWN(3, 2.5, 15), COUNT(4, 3, 16), SIZE(4, 2.5, 15);

        final Price price;

        VolumeUpgrade(double cost, double growth, int max) { price = new Price(cost, growth, max); }

        @Override
        public Price price() { return price; }
    }

    /** Une boule qui flotte dans le volume, en attendant qu'on la cueille. */
    public record VolumeBall(double x, double y, double z) {
    }

    // Ce qui ne se sauvegarde pas : la plongée en cours, son minuteur, les boules du cube.
    boolean inVolume;

    double volumeTimeLeft, volumeSpawnTimer;

    final List<VolumeBall> volumeBalls = new ArrayList<>();

    final Random volumeRandom = new Random(3);

    long volumeCollected, volumeDives;

    double volumeShotTimer;

    // ------------------------------------------------------------------
    // La durée, quatrième dimension
    // ------------------------------------------------------------------

    /**
     * Un rond, un bouton au milieu : tant qu'on le tient, les spirales avancent du bord vers le centre, de
     * {@link #durationTurnsPerSecond()} tours par seconde, et l'atteignent au bout de {@link #durationMaxSeconds()}
     * secondes ; tenir plus longtemps n'apporte plus rien, sans rien faire perdre. Au relâchement, chaque spirale donne
     * ses éclats de la durée : chaque tour vaut un de plus que le précédent (1, 2, 3… fois la valeur), donc tenir
     * longtemps rapporte bien plus que plusieurs appuis courts.
     */
    public static final double DURATION_MAX_SECONDS = 6, DURATION_TIME_STEP = 2;

    /** Tours par seconde au départ, et ce que chaque niveau de vitesse leur ajoute (+20 %). */
    public static final double DURATION_TURNS = 0.5, DURATION_SPEED_STEP = 0.2;

    /**
     * Éclats de la durée (ses instants) à gagner pour son seuil, qui lui met le rond vert (jusqu'à la 0.138, il ouvrait
     * aussi le palier 5 de la graine). 15 millions depuis la version 0.136 (100 avant) : les éclats d'un relâchement
     * montent avec le carré des tours, et le seuil vient après environ 15 minutes de jeu appliqué.
     */
    public static final double DURATION_THRESHOLD = 15_000_000;

    /**
     * Les améliorations de la durée : leur prix de départ en éclats de la durée, la croissance, le plafond. Dans
     * l'ordre de la sauvegarde : vitesse des spirales, nombre de spirales, valeur, temps au plus. Chaque
     * niveau coûte une fois de plus le prix du précédent (×3,4, ×4, ×4, ×3,2 au lieu de ×2,4, ×3, ×3, ×2,2) ; avant, les
     * trois améliorations plafonnées étaient au plus haut en trois minutes.
     */
    public enum DurationUpgrade implements Priced {
        SPEED(3, 3.4, 15), COUNT(6, 4, 9), VALUE(5, 4, Integer.MAX_VALUE), TIME(4, 3.2, 20);

        final Price price;

        DurationUpgrade(double cost, double growth, int max) { price = new Price(cost, growth, max); }

        @Override
        public Price price() { return price; }
    }

    /** Le bouton tenu, et depuis combien de secondes ; ni l'un ni l'autre ne se sauvegarde. */
    boolean durationHeld;

    double durationHeldSeconds;

    long durationReleases;

    double durationLastReward;

    /** Arrosages qui font du germe une brindille : c'est elle qui ouvre le deuxième univers. */
    public static final long SEED_TWIG_WATERINGS = 30;

    /**
     * Les paliers de la graine, de 1 à {@link #SEED_STAGES} : le palier N ouvre l'univers N, et dès le
     * quatrième des bulles naissent dans l'arbre. Chaque palier demande ses propres arrosages
     * ({@link #SEED_STAGE_STEPS}), de plus en plus, au lieu de trente de plus à chaque fois (300 en tout : l'arbre-monde
     * venait en cinq minutes d'arrosoir, et rien de ce qui se gagne dans les dimensions ne servait à y arriver). À
     * l'arrosoir seul (un arrosage par seconde), le bonsaï demande un quart d'heure, le jeune arbre trois heures et
     * l'arbre-monde douze : ce sont les améliorations de la graine, payées avec les éclats des dimensions, qui
     * raccourcissent le chemin, et les attentes de l'amas et de la formation (deux heures par univers) qui le paient.
     */
    public static final int SEED_STAGES = 11;

    /** Arrosages que demande chaque palier de 2 à 11, en plus du précédent. */
    static final long[] SEED_STAGE_STEPS = {30, 60, 900, 2_700, 5_400, 10_800, 16_200, 21_600, 32_400, 43_200};

    /** Le nom de chaque palier, du premier au onzième. */
    public static final List<String> SEED_STAGE_NAMES = List.of("la graine", "la tige", "la graine craquelée",
            "la graine brisée", "le bonsaï", "l'arbuste", "le jeune arbre", "l'arbre mûr", "l'arbre ancien", "l'arbre géant",
            "l'arbre-monde");

    /** Arrosages qu'il faut en tout pour ce palier (2 à 11) : la somme de {@link #SEED_STAGE_STEPS} jusqu'à lui. */
    public static long seedStageWaterings(int stage) {
        long total = 0;
        for (int each = 2; each <= Math.min(stage, SEED_STAGES); each++) total += SEED_STAGE_STEPS[each - 2];
        return total;
    }

    /** Paliers d'avance que la graine peut prendre sur les univers formés. */
    public static final int SEED_STAGES_AHEAD = 2;

    // ------------------------------------------------------------------
    // L'arrosoir
    // ------------------------------------------------------------------

    /** Arrosages par seconde que donne l'arrosoir penché sur la graine. */
    public static final double WATERING_CAN_PER_SECOND = 1;

    /** L'arrosoir penché sur la graine, et la part d'arrosage déjà versée ; ni l'un ni l'autre ne se sauvegarde. */
    boolean watering;

    double wateringPoured;

    // ------------------------------------------------------------------
    // Les bulles de l'arbre
    // ------------------------------------------------------------------

    /**
     * Les bulles de l'arbre (réseau complet à la 0.143) : un réseau d'améliorations posé sur l'arbre de la
     * graine, payé avec l'énergie du vide accumulée. Au palier qui ouvre l'univers 4 (la fin de la dimension 3), une
     * bulle naît à la base, sur la graine ; chaque palier suivant en ajoute, reliées aux bulles d'avant, jusqu'à
     * l'arbre-monde : 1, 2, 3, 2, 2, 2, 4 puis 3 (dont une dans les racines, sous la graine). Une bulle s'ouvre quand une
     * bulle à laquelle elle est reliée est achetée : la base d'abord, puis le réseau de proche en proche. Pour
     * l'instant, un visuel : achetée, une bulle s'allume, sans autre effet.
     *
     * <p>L'ordre des bulles est celui de la sauvegarde (un bit par bulle) : les nouvelles s'ajoutent à la fin.
     */
    public enum TreeBubble {
        // Les prix : les bulles qui naissent à un palier de la graine coûtent ensemble environ 1,4 fois
        // l'énergie du vide que le Speedrunner a gardée à la formation de l'univers d'avant (mesuré sur la partie de
        // référence, graine 201). Il ne peut pas toutes les prendre d'un coup : il choisit, et garde pour la suite. Avant,
        // environ ×1,5 par palier à partir de 1 000 : à l'univers 5, l'énergie gardée valait déjà cent fois les prix.
        // Les effets ; les bulles qui se rachètent coûtent 2,5 fois plus à chaque niveau.
        SEED(4, "Bulle de la graine", 5_200, 1, "+1 place de maturation des astres par dimension ouverte"),
        LEAVES_LEFT(5, "Bulle du bonsaï, à gauche", 10_400, 5, "×2 sur l'énergie du vide gagnée, par niveau", 0),
        LEAVES_RIGHT(5, "Bulle du bonsaï, à droite", 10_400, 1,
                "Les automatismes agissent sans délai et achètent tout ce qu'ils peuvent", 0),
        // Le gain de la 0.175 passe de l'arbre mûr à l'arbuste, à gauche (GameCollider.crossingGain) ;
        // Le premier univers formé ne compte plus.
        CROWN_LEFT(6, "Bulle de l'arbuste, à gauche", 21_400, 1,
                "Collisionneur ouvert dès le début de l'univers ; il gagne 20 % d'énergie et de hadrons par univers formé après le premier", 1),
        CROWN_MIDDLE(6, "Bulle de l'arbuste, au milieu", 21_400, 1,
                "L'onglet des hadrons se propose dans le collisionneur, comme le vide", 1, 2),
        // Elle se rachète cinq fois, un niveau de Persistance offert par niveau.
        CROWN_RIGHT(6, "Bulle de l'arbuste, à droite", 21_400, 5,
                "Un niveau de Persistance dès le début par niveau, et des atomes chaque seconde selon les générateurs, sans fusionner", 2),
        YOUNG_LEFT(7, "Bulle du jeune arbre, à gauche", 32_100, 1,
                "Les hadrons sont ouverts dès le début de l'univers, sans les payer", 3, 4),
        YOUNG_RIGHT(7, "Bulle du jeune arbre, à droite", 32_100, 1,
                "Plus de plafond de générateurs, mais au-delà de l'ancien, leur prix grimpe très vite", 4, 5),
        MATURE_LEFT(8, "Bulle de l'arbre mûr, à gauche", 32_100, 1,
                "Le collisionneur traverse les univers : toujours ouvert, jamais remis à zéro", 3, 6),
        MATURE_RIGHT(8, "Bulle de l'arbre mûr, à droite", 32_100, 1,
                "Un nouvel univers garde la matière noire, son arbre, les explosions et les défis réussis", 5, 7),
        ANCIENT_LEFT(9, "Bulle de l'arbre ancien, à gauche", 7_900, 1,
                "Les hadrons traversent les univers : jamais remis à zéro", 6, 8),
        ANCIENT_RIGHT(9, "Bulle de l'arbre ancien, à droite", 7_900, 1, "Les astres n'ont plus besoin de mûrir", 7, 9),
        GIANT_LEFT_HIGH(10, "Bulle de l'arbre géant, tout à gauche", 12_100, 5,
                "5 % de l'énergie du vide gagnée vont aussi, chaque seconde, au cumul des bulles, par niveau", 10),
        GIANT_LEFT_LOW(10, "Bulle de l'arbre géant, à gauche, en bas", 12_100, 1,
                "Le vide traverse les univers : toujours ouvert, jamais remis à zéro", 8, 12),
        GIANT_RIGHT_HIGH(10, "Bulle de l'arbre géant, tout à droite", 12_100, 1,
                "De la matière noire chaque seconde selon les atomes, sans exploser", 11),
        GIANT_RIGHT_LOW(10, "Bulle de l'arbre géant, à droite, en bas", 12_100, 1,
                "Un nouvel univers garde les défis du Big Bang réussis, et tous les paliers du Big Bang sont ouverts", 9, 14),
        ROOTS(11, "Bulle des racines", 69_300, 1,
                // Deux lignes de 56 caractères au plus dans l'arbre (205 caractères en 0.169, qui débordaient) ;
                // le jalon des racines est détaillé dans les paliers des hadrons.
                "Le défi final : un palier par dimension, les dix finissent la partie ; les gluons l'abaissent jusqu'à moitié",
                0, 13, 15),
        HEART(11, "Bulle de l'arbre-monde, au cœur", 69_300, 1,
                "Les particules exotiques s'assemblent : pions, kaons, mésons charmés…", 4, 6, 7),
        SUMMIT(11, "Bulle de l'arbre-monde, au sommet", 69_300, 1,
                "Les automatismes des dimensions à moitié prix (ils s'ouvrent par les paliers de la graine et les univers)", 10, 11, 17),
        // Version 0.299 : le rassemblement accéléré, née avec l'arbre mûr (univers 8 à 11), dessinée sous la bulle de
        // l'arbuste du milieu, à laquelle elle est reliée : du temps actif contre l'attente de l'amas et de l'univers.
        GATHER(8, "Bulle du rassemblement", 32_100, 1,
                "Tenir le clic sur l'amas ou l'univers qui se rassemble : chaque seconde tenue en vaut quatre", 4);

        /** Ce que chaque niveau de plus coûte de plus, pour les bulles qui se rachètent : ×2,5 (version 0.166 ; ×2 avant). */
        public static final double LEVEL_GROWTH = 2.5;

        final int stage;
        final String label;
        final double cost;
        final int maxLevel;
        final String effect;
        /** Les bulles d'avant auxquelles elle est reliée (leurs rangs) : chaque lien n'est noté qu'une fois, ici. */
        final int[] below;

        TreeBubble(int stage, String label, double cost, int maxLevel, String effect, int... below) {
            this.stage = stage;
            this.label = label;
            this.cost = cost;
            this.maxLevel = maxLevel;
            this.effect = effect;
            this.below = below;
        }

        /** Combien de fois elle s'achète : 1, ou 5 pour les trois qui se rachètent (version 0.148 ; l'arbuste à droite depuis la 0.167). */
        public int maxLevel() { return maxLevel; }

        /** Ce qu'elle fait, en une phrase ; « à venir » pour celles qui attendent leur onglet. */
        public String effect() { return effect; }

        /** Le palier de la graine où elle naît. */
        public int stage() { return stage; }

        public String label() { return label; }

        /** Son prix, en énergie du vide accumulée : celui du premier niveau. */
        public double cost() { return cost; }

        /** Le prix du niveau suivant quand elle en a déjà {@code level} : ×{@link #LEVEL_GROWTH} par niveau. */
        public double costAt(int level) { return cost * Math.pow(LEVEL_GROWTH, Math.max(0, level)); }

        /** Les bulles d'avant auxquelles elle est reliée ; aucune pour celle de la base. */
        public List<TreeBubble> below() {
            List<TreeBubble> list = new ArrayList<>();
            for (int index : below) list.add(values()[index]);
            return list;
        }

        /** Toutes les bulles reliées à elle : celles d'avant, et celles d'après qui la nomment. */
        public List<TreeBubble> links() {
            List<TreeBubble> list = new ArrayList<>(below());
            for (TreeBubble other : values()) if (other.below().contains(this)) list.add(other);
            return list;
        }
    }

    // ------------------------------------------------------------------
    // Les améliorations de la graine et le chrono
    // ------------------------------------------------------------------

    /**
     * Le nom de la ressource de chaque dimension, ses éclats : les segments de la ligne, les facettes du plan, les
     * sphères du volume, les instants de la durée. De 5 à 10, pas encore de nom : ces dimensions n'existent pas encore.
     */
    static final List<String> DIMENSION_RESOURCES = List.of("segments", "facettes", "sphères", "instants", "nœuds", "ondes", "étincelles", "repères", "reflets", "fragments");

    /** Le nom de la ressource de la dimension {@code number}, ou « éclats de la dimension N » pour celles qui n'existent pas. */
    public static String dimensionResourceName(int number) {
        return number >= 1 && number <= DIMENSION_RESOURCES.size() ? DIMENSION_RESOURCES.get(number - 1)
                : "éclats de la dimension " + number;
    }

    /**
     * Les améliorations de la graine, payées avec la ressource d'une dimension : quatre pour l'arrosage et l'étagère,
     * quatre pour le chrono, une par dimension dans chaque groupe. Dans l'ordre de la sauvegarde.
     *
     * <p>Les prix se mesurent en temps de jeu de leur dimension : le seuil d'une dimension vaut un quart
     * d'heure de jeu appliqué ({@link #dimensionThreshold(int)}), et chaque amélioration coûte au premier niveau
     * {@link #SEED_PRICE_SHARE} de ce seuil (trois minutes), puis {@link #SEED_PRICE_GROWTH} fois plus à chaque niveau ;
     * un achat unique (l'étage du chrono, la bulle des racines) coûte {@link #SEED_SINGLE_SHARE} du seuil. Avant, les
     * prix allaient de 350 à 1 500 000 sans rapport entre eux ni avec ce que rapporte la dimension, et grandissaient de
     * ×2,5 à ×4 par niveau : au-delà du troisième niveau, plus rien ne s'achetait avant les automatismes des dimensions.
     */
    public enum SeedUpgrade implements Priced {
        /** Segments de la ligne : +1 arrosage par seconde de l'arrosoir tenu, par niveau. */
        WATERING(1, "Débit de l'arrosoir", 20, false),
        /** Sphères du volume (version 0.182 ; facettes avant) : le tuyau d'arrosage, qui verse seul 10 % de ce que verse l'arrosoir, +10 % par niveau. */
        HOSE(3, "Tuyau d'arrosage", 10, false),
        /** Instants de la durée (version 0.182 ; sphères avant) : la terre fertile, chaque arrosage compte pour un de plus par niveau ({@link #seedSoilFactor()}). */
        SOIL(4, "Terre fertile", 10, false),
        /**
         * Facettes du plan (version 0.182 ; instants de la durée avant) : un étage de plus à l'étagère, avec le chrono. Le
         * chrono vient ainsi au deuxième univers, et ses réglages se paient dans les quatre premières dimensions.
         */
        CHRONO(2, "Étage du chrono", 1, false),
        /** Segments : le chrono se remplit plus vite. */
        CHRONO_FILL(1, "Remplissage du chrono", 20, true),
        /** Facettes : il garde plus de temps. */
        CHRONO_CAPACITY(2, "Réserve du chrono", 20, true),
        /** Sphères : il accélère davantage. */
        CHRONO_SPEED(3, "Accélération du chrono", 8, true),
        /** Instants : il perd moins à grande vitesse. */
        CHRONO_LOSS(4, "Pertes du chrono", 15, true),
        /** Nœuds de la boucle : le rond de croissance dure plus longtemps, +10 s par niveau. */
        BLOOM_DURATION(5, "Durée du rond de croissance", 15, false),
        /** Nœuds : le rond de croissance accélère davantage l'arrosage, +10 points par niveau. */
        BLOOM_POWER(5, "Force du rond de croissance", 20, false),
        /** Ondes de la brane : la lumière astrale, +20 % d'arrosage par seconde de l'arrosoir et du tuyau, par niveau. */
        ASTRAL_LIGHT(6, "Lumière astrale", 20, false),
        /** Étincelles du clicker : le beau temps dure plus longtemps, +10 s par niveau. */
        WEATHER_SUN(7, "Beau temps plus long", 15, false),
        /** Étincelles : les orages passent plus vite, −3 s par niveau. */
        WEATHER_STORM(7, "Orages plus courts", 8, false),
        /** Étincelles : le beau temps arrose davantage, +10 points par niveau. */
        WEATHER_POWER(7, "Soleil plus fort", 20, false),
        /**
         * Repères de la grille : la graine profonde, qui multiplie tout l'arrosage par √(log10 E), E
         * l'énergie du vide récupérée ; un dixième de l'effet par niveau.
         */
        DEEP_SEED(8, "Graine profonde", 10, false),
        /** Reflets du miroir : les saisons, chaque arrosage remplit le chrono de 0,2 s par niveau, selon la saison. */
        SEASONS(9, "Saisons", 15, true),
        /** Fragments du mélange : ouvre la bulle des racines, sous la graine. Une fois. */
        ROOT_BUBBLE(10, "Bulle des racines", 1, false);

        /** Le premier niveau d'une amélioration : un cinquième du seuil de sa dimension, trois minutes de jeu. */
        public static final double SEED_PRICE_SHARE = 0.2;
        /** Ce par quoi le prix est multiplié à chaque niveau. */
        public static final double SEED_PRICE_GROWTH = 1.6;
        /** Un achat unique : un tiers du seuil de sa dimension, cinq minutes de jeu. */
        public static final double SEED_SINGLE_SHARE = 1.0 / 3;

        final int dimension;
        final String label;
        final Price price;
        final boolean chrono;

        SeedUpgrade(int dimension, String label, int max, boolean chrono) {
            this.dimension = dimension;
            this.label = label;
            double threshold = dimensionThreshold(dimension);
            this.price = max == 1 ? new Price(threshold * SEED_SINGLE_SHARE, 1, 1)
                    : new Price(threshold * SEED_PRICE_SHARE, SEED_PRICE_GROWTH, max);
            this.chrono = chrono;
        }

        /** La dimension dont la ressource la paie. */
        public int dimension() { return dimension; }

        public String label() { return label; }

        @Override
        public Price price() { return price; }

        /** Vrai pour celles du chrono, qui attendent son étage. */
        public boolean chrono() { return chrono; }
    }

    /** Ondes de la brane à gagner pour son seuil. */
    public static final double BRANE_THRESHOLD = 5_000;

    /** Étincelles du clicker à gagner pour son seuil. */
    public static final double CLICKER_THRESHOLD = 5_000;

    /** Repères de la grille à gagner pour son seuil. */
    public static final double GRID_THRESHOLD = 5_000;

    /** Reflets du miroir à gagner pour son seuil. */
    public static final double MIRROR_THRESHOLD = 5_000;

    /** Fragments du mélange à gagner pour son seuil. */
    public static final double MIX_THRESHOLD = 5_000;

    /** Nœuds de la boucle à gagner pour son seuil : un quart d'heure de jeu appliqué environ. */
    public static final double LOOP_THRESHOLD = 5_000;

    /** Le tuyau verse seul : la part déjà versée de son prochain arrosage, qui ne se sauvegarde pas. */
    double hosePoured;

    // Le chrono : il se remplit de temps tout seul, et ce temps sert à faire passer le jeu plus vite.

    /** Au départ, le chrono garde 0,1 seconde par seconde de jeu, 5 minutes au plus, et accélère jusqu'à ×2. */
    public static final double CHRONO_FILL = 0.1, CHRONO_CAPACITY = 300, CHRONO_MAX_SPEED = 2;

    /** Ce que perd le chrono par point d'accélération au-delà de ×1 : 10 %, ×0,85 par niveau de pertes, 90 % au plus. */
    public static final double CHRONO_LOSS_STEP = 0.1, CHRONO_LOSS_CUT = 0.85, CHRONO_LOSS_MAX = 0.9;

    /**
     * Temps de jeu qu'il faut laisser à l'échelle d'avant pour se rassembler avant de former l'amas,
     * puis l'univers : deux heures chacun depuis la version 0.125 (45 minutes avant). Un joueur qui réunit la matière plus vite
     * attend ; l'infatigable, qui met déjà près d'une heure à chaque échelle, ne le voit pas.
     */
    public static final double COSMOS_SETTLE_SECONDS = 120 * 60;

    final Map<Molecule.State, Integer> sortsByState = new EnumMap<>(Molecule.State.class);

    int sortsByStateVersion = -1;

    // ------------------------------------------------------------------
    // Le deuxième univers
    // ------------------------------------------------------------------

    /** Fusions du deuxième univers qu'il faut pour ouvrir l'onglet Skins. */
    public static final int SKINS_FUSIONS = 5;

    /** Générateurs qu'il faut pour le régime des générateurs. */
    public static final int REGIME_GENERATORS = 3;

    /** Ce que le régime rapide ajoute à la cadence des générateurs, et combien de temps. */
    public static final double REGIME_GAIN = 0.15;

    public static final double REGIME_SECONDS = 20;

    /** Le repos qui suit le régime rapide, avant de pouvoir le relancer. */
    public static final double REGIME_REST_SECONDS = 40;

    /** Générateurs qu'il faut pour la synergie par rang. */
    public static final int SYNERGY_GENERATORS = 10;

    /** Ce que la synergie ajoute, au plus, à chaque générateur : +0,5 %. */
    public static final double SYNERGY_MAX = 0.005;

    /** Fusions de suite sans le plafond d'atomes pour la prime de cycle parfait, sa durée et son gain. */
    public static final int PERFECT_FUSIONS = 3;

    public static final double PERFECT_SECONDS = 60;

    public static final double PERFECT_GAIN = 0.10;

    /** Ce que chaque tirage ajoute à la famille préférée, et le nombre de tirages qui comptent au plus (×2). */
    public static final double PREFERENCE_STEP = 0.05;

    public static final int PREFERENCE_MAX_DRAWS = 20;

    /** Un élément gratuit à choisir tous les tant d'éléments découverts. */
    public static final int FREE_ELEMENT_EVERY = 10;

    // ------------------------------------------------------------------
    // Les skins à obtenir
    // ------------------------------------------------------------------

    /** Le record de reconstruction qui donne le skin « Éclair » : de la fusion au dixième générateur en une minute au plus. */
    public static final double REBUILD_FAST_SECONDS = 60;

    /** Le prix de la recherche « Fusion prismatique », en particules. */
    public static final BigNum PRISM_COST = BigNum.of(1, 50);

    /** Atomes créés qu'il faut pour la recherche sur le noyau dense, et le prix de ses quatre paliers, en atomes. */
    public static final BigNum DENSE_UNLOCK_ATOMS = BigNum.of(1, 6);

    public static final BigNum[] DENSE_COSTS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 5), BigNum.of(1, 8)};

    static final Skin[] DENSE_SKINS = {Skin.ATOM_DENSE, Skin.TABLE_LEAD, Skin.ATOM_STELLAR, Skin.TABLE_GOLD};

    /** Les paliers de la matière noire (gagnée dans le deuxième univers) et leurs skins. */
    static final BigNum[] DARK_TIERS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 4), BigNum.of(1, 5)};

    static final Skin[] DARK_SKINS = {Skin.DARK_OBSIDIAN, Skin.DARK_VORTEX, Skin.DARK_NEBULA, Skin.DARK_PULSAR};

    /** Les paliers du Big Bang (Big Bangs du deuxième univers) et leurs skins. */
    static final int[] BANG_TIERS = {1, 3};

    static final Skin[] BANG_SKINS = {Skin.BANG_AURORA, Skin.BANG_SUPERNOVA};

    /** Les notifications affichées depuis l'ouverture du jeu, comptées par la fenêtre ; non sauvegardées. */
    int notificationsShown;

    static boolean albumComplete(Map<String, Double> found, int catalog) {
        return catalog > 0 && found.size() >= catalog;
    }

    /** Un nombre entier lisible pour le guide : « 3 », « 1 250 », puis en notation scientifique. */
    static String wholeText(BigNum value) {
        if (value.lt(BigNum.of(1, 6))) {
            String digits = String.valueOf((long) Math.floor(value.toDouble() + 1e-9));
            return digits.replaceAll("(\\d)(?=(\\d{3})+$)", "$1\u00a0");
        }
        return value.toString();
    }

    /** Graines d'univers que donne la formation de l'univers. */
    public static final int SEEDS_PER_UNIVERSE = 1;
}
