package idle.core;

import idle.core.achievements.Achievement;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Les univers : la partie secondaire, le deuxième univers et les skins à obtenir.
 */
public abstract class GameUniverse extends GameAtoms {

    GameUniverse(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract void forgetDerived();
    public abstract BigNum darkMatterEarned();
    public abstract long gatheredInState(Molecule.State matter);
    public abstract boolean isGalaxyUnlocked();
    public abstract boolean canFormGalaxy();
    public abstract boolean keepsUpgradesOnFusion();


    // ------------------------------------------------------------------
    // Le cosmos : la galaxie, l'amas de galaxies, l'univers
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // La partie secondaire (version 0.081) : les univers, sans logique pour l'instant
    // ------------------------------------------------------------------

    /** Nombre de dimensions à ouvrir, une par univers jusqu'au dixième. */
    public static final int MAX_DIMENSIONS = 10;

    /** Univers créés : 0 avant l'univers, 1 une fois formé, plus ceux d'après (créés pour l'instant par les outils d'essai). */
    public int universes() {
        return isUniverseFormed() ? 1 + state.extraUniverses() : 0;
    }

    /** Vrai une fois un univers formé, même pendant la partie qui mène au suivant (version 0.087). */
    public boolean isUniverseFormed() {
        return state.universeFormed() || state.cosmosLevel() >= Cosmos.values().length;
    }

    /** Vrai dès le premier univers (version 0.100 ; avant, au deuxième) : la sous-page des dimensions s'ouvre. */
    public boolean isDimensionsUnlocked() {
        return universes() >= 1;
    }

    /** Dimensions ouvertes : une par univers, la première dès le premier, dix au plus. */
    public int dimensionsUnlocked() {
        return isDimensionsUnlocked() ? Math.min(MAX_DIMENSIONS, universes()) : 0;
    }

    /** Les améliorations de l'univers, dans l'ordre. */
    public List<idle.core.universe.UniverseUpgrade> universeUpgrades() {
        return List.of(idle.core.universe.UniverseUpgrade.values());
    }

    /** Niveau d'une amélioration de l'univers. */
    public int universeUpgradeLevel(idle.core.universe.UniverseUpgrade upgrade) {
        return state.universeUpgradeLevel(upgrade.id());
    }

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
    public void testAddUniverseSeeds(BigNum amount) {
        state.setUniverseSeeds(state.universeSeeds().add(amount));
    }

    /**
     * Le vide (version 0.099), deuxième acte de la partie secondaire : un palier du collisionneur, avec sa
     * ressource, l'énergie du vide. Il s'ouvre au premier Big Bang du troisième univers (version 0.116 ; avant,
     * du deuxième). Pour l'instant sans logique : il ne fait que se montrer.
     */
    public boolean isVoidUnlocked() {
        return universes() >= 2 && state.bigBangs() > 0;
    }

    // ------------------------------------------------------------------
    // La ligne, première dimension (version 0.101)
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
    /** Le seuil de la ligne (version 0.102) : tant d'éclats gagnés en tout, et la dimension 1 est marquée. */
    public static final double LINE_THRESHOLD = 100;

    /** Prix des deux premières améliorations de la ligne, en éclats, et leur croissance à chaque niveau. */
    public static final double LINE_YIELD_COST = 1, LINE_TICKS_COST = 2, LINE_COST_GROWTH = 3;

    /** Les améliorations de la ligne : leur prix de départ en éclats, et la croissance à chaque niveau. */
    public enum LineUpgrade {
        YIELD(LINE_YIELD_COST, LINE_COST_GROWTH), TICKS(LINE_TICKS_COST, LINE_COST_GROWTH), SPEED(5, 3), BALLS(10, 4), LINES(25, 5);

        private final double cost, growth;

        LineUpgrade(double cost, double growth) {
            this.cost = cost;
            this.growth = growth;
        }
    }

    /** Vrai dès que la première dimension est ouverte : les boules courent, les éclats tombent. */
    public boolean isLineOpen() {
        return dimensionsUnlocked() >= 1;
    }

    /** Traits sur chaque ligne, ses deux bouts compris, également espacés. */
    public int lineTicks() {
        return 2 + state.lineTicksLevel();
    }

    /** Boules sur chaque ligne. */
    public int lineBalls() {
        return 1 + state.lineBallsLevel();
    }

    /** Lignes. */
    public int lineCount() {
        return 1 + state.lineCountLevel();
    }

    /** La vitesse des boules, ×1 au départ. */
    public double lineSpeed() {
        return 1 + LINE_SPEED_STEP * state.lineSpeedLevel();
    }

    /** Secondes pour un aller, à la vitesse actuelle. */
    public double lineHalfTrip() {
        return LINE_HALF_TRIP / lineSpeed();
    }

    /** Éclats de chaque trait touché : 0,1, et 0,1 de plus par niveau de l'amélioration. */
    public double lineShardsPerTouch() {
        return LINE_SHARD * (1 + state.lineYieldLevel());
    }

    /**
     * Le décalage d'une boule sur la route (aller puis retour, longueur 2) : les boules d'une même ligne
     * se répartissent également, et chaque ligne est un peu décalée de la précédente.
     */
    public double lineOffset(int line, int ball) {
        return (2.0 * ball / lineBalls() + 0.5 * line / lineCount()) % 2;
    }

    /** Où est cette boule de cette ligne : 0 au début, 1 au bout. */
    public double linePosition(int line, int ball) {
        double phase = (state.linePhase() + lineOffset(line, ball)) % 2;
        return phase <= 1 ? phase : 2 - phase;
    }

    /** Où est la première boule de la première ligne. */
    public double linePosition() {
        return linePosition(0, 0);
    }

    /** Vrai tant que la première boule va vers le bout de la ligne ; faux au retour. */
    public boolean lineGoingOut() {
        return state.linePhase() < 1;
    }

    /** Éclats par seconde : chaque aller d'une boule touche tous les traits sauf celui d'où elle part. */
    public double lineShardsPerSecond() {
        return isLineOpen() ? (lineTicks() - 1) * lineShardsPerTouch() * lineBalls() * lineCount() / lineHalfTrip() : 0;
    }

    /** Éclats du plan à gagner pour son seuil (version 0.120), comme les cent de la ligne. */
    public static final double PLANE_THRESHOLD = 100;

    /** Vrai une fois le seuil du plan atteint : {@link #PLANE_THRESHOLD} éclats du plan gagnés en tout. */
    public boolean isPlaneThresholdReached() {
        return state.planeShardsEarned() >= PLANE_THRESHOLD - 1e-9;
    }

    /** Vrai si la dimension {@code number} (1 la ligne, 2 le plan) a atteint son seuil ; faux pour celles qui n'existent pas encore. */
    public boolean isDimensionThresholdReached(int number) {
        return switch (number) {
            case 1 -> isLineThresholdReached();
            case 2 -> isPlaneThresholdReached();
            default -> false;
        };
    }

    /** Vrai une fois le seuil de la ligne atteint : {@link #LINE_THRESHOLD} éclats gagnés en tout. */
    public boolean isLineThresholdReached() {
        return state.dimensionShardsEarned() >= LINE_THRESHOLD - 1e-9;
    }

    /**
     * Fait avancer les boules de {@code dt} secondes et donne les éclats des traits touchés. L'aller et
     * le retour mis bout à bout font une seule route de longueur 2 où les traits tombent tous les
     * 1/(traits − 1) : le compte est exact quel que soit le pas, même pour une longue absence.
     */
    void runLine(double dt) {
        if (!isLineOpen() || dt <= 0) return;
        int gaps = lineTicks() - 1;
        double from = state.linePhase(), move = dt / lineHalfTrip();
        long touches = 0;
        for (int line = 0; line < lineCount(); line++) {
            for (int ball = 0; ball < lineBalls(); ball++) {
                double start = from + lineOffset(line, ball);
                touches += (long) Math.floor((start + move) * gaps + 1e-9) - (long) Math.floor(start * gaps + 1e-9);
            }
        }
        if (touches > 0) {
            double gain = touches * lineShardsPerTouch();
            state.setDimensionShards(state.dimensionShards() + gain);
            state.setDimensionShardsEarned(state.dimensionShardsEarned() + gain);
        }
        state.setLinePhase((from + move) % 2);
    }

    /** Le niveau d'une amélioration de la ligne. */
    public int lineLevel(LineUpgrade upgrade) {
        return switch (upgrade) {
            case YIELD -> state.lineYieldLevel();
            case TICKS -> state.lineTicksLevel();
            case SPEED -> state.lineSpeedLevel();
            case BALLS -> state.lineBallsLevel();
            case LINES -> state.lineCountLevel();
        };
    }

    /** Le niveau le plus haut d'une amélioration de la ligne ; sans limite pour la densité. */
    public int lineMaxLevel(LineUpgrade upgrade) {
        return switch (upgrade) {
            case YIELD -> Integer.MAX_VALUE;
            case TICKS -> LINE_MAX_EXTRA_TICKS;
            case SPEED -> LINE_MAX_SPEED;
            case BALLS -> LINE_MAX_EXTRA_BALLS;
            case LINES -> LINE_MAX_EXTRA_LINES;
        };
    }

    /** Prix du prochain niveau, en éclats ; infini une fois au plus haut. */
    public double lineCost(LineUpgrade upgrade) {
        int level = lineLevel(upgrade);
        return level >= lineMaxLevel(upgrade) ? Double.POSITIVE_INFINITY : upgrade.cost * Math.pow(upgrade.growth, level);
    }

    public boolean canBuyLine(LineUpgrade upgrade) {
        return isLineOpen() && lineLevel(upgrade) < lineMaxLevel(upgrade) && state.dimensionShards() >= lineCost(upgrade) - 1e-9;
    }

    /** Achète un niveau d'une amélioration de la ligne. */
    public boolean buyLine(LineUpgrade upgrade) {
        if (!canBuyLine(upgrade)) return false;
        state.setDimensionShards(Math.max(0, state.dimensionShards() - lineCost(upgrade)));
        int level = lineLevel(upgrade) + 1;
        switch (upgrade) {
            case YIELD -> state.setLineYieldLevel(level);
            case TICKS -> state.setLineTicksLevel(level);
            case SPEED -> state.setLineSpeedLevel(level);
            case BALLS -> state.setLineBallsLevel(level);
            case LINES -> state.setLineCountLevel(level);
        }
        return true;
    }

    // Les deux premières, sous leur nom de la 0.101.
    public double lineYieldCost() {
        return lineCost(LineUpgrade.YIELD);
    }

    public double lineTicksCost() {
        return lineCost(LineUpgrade.TICKS);
    }

    public boolean canBuyLineYield() {
        return canBuyLine(LineUpgrade.YIELD);
    }

    public boolean canBuyLineTicks() {
        return canBuyLine(LineUpgrade.TICKS);
    }

    public boolean buyLineYield() {
        return buyLine(LineUpgrade.YIELD);
    }

    public boolean buyLineTicks() {
        return buyLine(LineUpgrade.TICKS);
    }

    // ------------------------------------------------------------------
    // Le plan, deuxième dimension (version 0.104)
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
    public enum PlaneUpgrade {
        VALUE(5, 3, Integer.MAX_VALUE), SPAWN(3, 2.5, 15), COUNT(4, 3, 17), SPEED(3, 2.5, 15), SIZE(4, 2.5, 15);

        private final double cost, growth;
        private final int max;

        PlaneUpgrade(double cost, double growth, int max) {
            this.cost = cost;
            this.growth = growth;
            this.max = max;
        }

        public int max() {
            return max;
        }
    }

    /** Un éclat posé sur le plan : il attend qu'on passe dessus. */
    public record PlaneShard(double x, double y) {
    }

    // Ce qui ne se sauvegarde pas : où est le point, où sont les éclats, la direction tenue.
    private double planeX, planeY, planeInputX, planeInputY, planeSpawnTimer;
    private final java.util.List<PlaneShard> planeShardsOnBoard = new java.util.ArrayList<>();
    private final java.util.Random planeRandom = new java.util.Random(2);
    private long planeCollected;

    /** Vrai dès que la deuxième dimension est ouverte. */
    public boolean isPlaneOpen() {
        return dimensionsUnlocked() >= 2;
    }

    public int planeLevel(PlaneUpgrade upgrade) {
        return state.planeLevel(upgrade.ordinal());
    }

    /** Éclats du plan que donne chaque éclat ramassé : 1, et 1 de plus par niveau. */
    public double planeValue() {
        return 1 + planeLevel(PlaneUpgrade.VALUE);
    }

    /** Secondes entre deux apparitions : 3, ×0,85 par niveau. */
    public double planeSpawnSeconds() {
        return PLANE_SPAWN_SECONDS * Math.pow(0.85, planeLevel(PlaneUpgrade.SPAWN));
    }

    /** Éclats présents à la fois, au plus : 3, et 1 de plus par niveau (vingt au plus). */
    public int planeMaxShards() {
        return PLANE_MAX_SHARDS + planeLevel(PlaneUpgrade.COUNT);
    }

    /** Vitesse du point du joueur : 0,6 par seconde, +20 % par niveau. */
    public double planeSpeed() {
        return PLANE_SPEED * (1 + 0.2 * planeLevel(PlaneUpgrade.SPEED));
    }

    /** Rayon du point du joueur : 0,025, +25 % par niveau (quinze niveaux : ×4,75). */
    public double planePlayerRadius() {
        return PLANE_PLAYER_RADIUS * (1 + PLANE_SIZE_STEP * planeLevel(PlaneUpgrade.SIZE));
    }

    public double planeX() {
        return planeX;
    }

    public double planeY() {
        return planeY;
    }

    /** Les éclats posés sur le plan, dans l'ordre où ils sont apparus. */
    public java.util.List<PlaneShard> planeShardsOnBoard() {
        return java.util.Collections.unmodifiableList(planeShardsOnBoard);
    }

    /** Éclats ramassés depuis le lancement : l'interface y voit ceux de la dernière image. */
    public long planeCollected() {
        return planeCollected;
    }

    /** La direction que tient le joueur, de −1 à 1 sur chaque axe (ZQSD) ; 0, 0 pour s'arrêter. */
    public void setPlaneInput(double dx, double dy) {
        planeInputX = Math.max(-1, Math.min(1, dx));
        planeInputY = Math.max(-1, Math.min(1, dy));
    }

    /** Pose le point du joueur : pour les essais. */
    public void setPlanePosition(double x, double y) {
        planeX = clampPlane(x, planePlayerRadius());
        planeY = clampPlane(y, planePlayerRadius());
    }

    /** Outil d'essai : retire tous les éclats posés sur le plan. */
    public void testClearPlane() {
        planeShardsOnBoard.clear();
        planeSpawnTimer = 0;
    }

    /** Pose un éclat sur le plan : pour les essais. */
    public void addPlaneShard(double x, double y) {
        if (planeShardsOnBoard.size() < planeMaxShards()) planeShardsOnBoard.add(new PlaneShard(x, y));
    }

    private static double clampPlane(double value, double radius) {
        return Math.max(-PLANE_EDGE + radius, Math.min(PLANE_EDGE - radius, value));
    }

    /**
     * Le plan avance de {@code dt} secondes : un éclat apparaît à son rythme tant qu'il en manque, le point
     * suit la direction tenue sans sortir du plan, et ramasse les éclats qu'il touche.
     */
    void runPlane(double dt) {
        if (!isPlaneOpen() || dt <= 0) return;
        planeSpawnTimer += dt;
        double every = planeSpawnSeconds();
        while (planeSpawnTimer >= every && planeShardsOnBoard.size() < planeMaxShards()) {
            planeSpawnTimer -= every;
            double margin = PLANE_SHARD_RADIUS * 2;
            planeShardsOnBoard.add(new PlaneShard((planeRandom.nextDouble() * 2 - 1) * (PLANE_EDGE - margin),
                    (planeRandom.nextDouble() * 2 - 1) * (PLANE_EDGE - margin)));
        }
        // Plein : le minuteur attend, il ne s'accumule pas.
        if (planeShardsOnBoard.size() >= planeMaxShards()) planeSpawnTimer = Math.min(planeSpawnTimer, every);
        double length = Math.hypot(planeInputX, planeInputY);
        if (length > 0) {
            double step = planeSpeed() * Math.min(dt, 0.25) / Math.max(1, length);
            setPlanePosition(planeX + planeInputX * step, planeY + planeInputY * step);
        }
        double reach = planePlayerRadius() + PLANE_SHARD_RADIUS;
        for (java.util.Iterator<PlaneShard> it = planeShardsOnBoard.iterator(); it.hasNext(); ) {
            PlaneShard shard = it.next();
            if (Math.hypot(shard.x() - planeX, shard.y() - planeY) > reach) continue;
            it.remove();
            planeCollected++;
            state.setPlaneShards(state.planeShards() + planeValue());
            state.setPlaneShardsEarned(state.planeShardsEarned() + planeValue());
        }
    }

    /** Prix du prochain niveau, en éclats du plan ; infini une fois au plus haut. */
    public double planeCost(PlaneUpgrade upgrade) {
        int level = planeLevel(upgrade);
        return level >= upgrade.max ? Double.POSITIVE_INFINITY : upgrade.cost * Math.pow(upgrade.growth, level);
    }

    public boolean canBuyPlane(PlaneUpgrade upgrade) {
        return isPlaneOpen() && planeLevel(upgrade) < upgrade.max && state.planeShards() >= planeCost(upgrade) - 1e-9;
    }

    public boolean buyPlane(PlaneUpgrade upgrade) {
        if (!canBuyPlane(upgrade)) return false;
        state.setPlaneShards(Math.max(0, state.planeShards() - planeCost(upgrade)));
        state.setPlaneLevel(upgrade.ordinal(), planeLevel(upgrade) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // La graine qui germe (version 0.103)
    // ------------------------------------------------------------------

    /**
     * Vrai si le bouton de la graine peut servir : l'univers est formé (il n'y a qu'une graine, la sienne)
     * et l'objectif de la dimension 1 (son seuil, {@link #isLineThresholdReached()}) est atteint. Version
     * 0.105 : il ne demande plus de graine en réserve, ce qui le laissait grisé une fois l'objectif atteint.
     */
    public boolean canTendSeed() {
        // Version 0.120 : la graine ne s'arrose qu'une fois le seuil de la dimension de son palier atteint (la ligne
        // au premier, le plan au deuxième…), et plus au dernier.
        int stage = seedStage();
        return isUniverseFormed() && stage < SEED_STAGES && isDimensionThresholdReached(stage);
    }

    /** Arrosages qui font du germe une brindille (version 0.106) : c'est elle qui ouvre le deuxième univers. */
    public static final long SEED_TWIG_WATERINGS = 30;

    /**
     * Les paliers de la graine (version 0.120), de 1 à {@link #SEED_STAGES} : le palier N ouvre l'univers N. Chacun
     * demande {@link #SEED_TWIG_WATERINGS} arrosages de plus que le précédent, une fois atteint le seuil de la
     * dimension du palier d'avant : la ligne pour la tige (palier 2), le plan pour le palier 3, et ainsi de suite.
     */
    public static final int SEED_STAGES = 11;

    /** Le nom de chaque palier, du premier au onzième. */
    public static final java.util.List<String> SEED_STAGE_NAMES = java.util.List.of("la graine", "la tige", "la graine craquelée",
            "la graine brisée", "le bonsaï", "l'arbuste", "le jeune arbre", "l'arbre mûr", "l'arbre ancien", "l'arbre géant",
            "l'arbre-monde");

    /** Arrosages qu'il faut en tout pour ce palier (2 à 11) : trente de plus à chaque palier. */
    public static long seedStageWaterings(int stage) {
        return SEED_TWIG_WATERINGS * Math.max(0, stage - 1);
    }

    /** Le palier atteint par la graine, de 1 (pas encore de tige) à {@link #SEED_STAGES}. */
    public int seedStage() {
        if (!state.seedSprouted()) return 1;
        int stage = 1;
        while (stage < SEED_STAGES && state.seedWaterings() >= seedStageWaterings(stage + 1) && isDimensionThresholdReached(stage)) stage++;
        return stage;
    }

    /** Vrai une fois le germe devenu brindille : la graine a au moins son deuxième palier. */
    public boolean isSeedTwig() {
        return seedStage() >= 2;
    }

    /**
     * Vrai si l'univers suivant peut s'ouvrir depuis la page Univers. Version 0.120 : l'univers N attend le palier N
     * de la graine (la tige pour le deuxième, la graine craquelée pour le troisième…).
     */
    public boolean canOpenNextUniverse() {
        // Comptés par la graine elle-même : l'outil d'essai « +1 univers » ne change pas le palier demandé.
        return canStartNextUniverse() && seedStage() >= state.seedUniversesOpened() + 2;
    }

    /** Le palier de la graine qu'attend l'univers suivant : 2 pour le deuxième, 3 pour le troisième… */
    public int nextUniverseStage() {
        return Math.min(SEED_STAGES, state.seedUniversesOpened() + 2);
    }

    /**
     * Vrai si la brindille doit ouvrir le deuxième univers maintenant (version 0.109) : elle est là, l'univers
     * est formé, aucune partie n'est en cours, et elle ne l'a pas déjà ouvert. Le nombre d'univers n'y entre
     * pas : l'outil d'essai « +1 univers » (pour ouvrir le plan) ne l'empêche plus.
     */
    public boolean twigOpensUniverse() {
        return canOpenNextUniverse() && !state.twigOpened();
    }

    /** Ouvre le deuxième univers si la brindille le demande ; vrai si c'est fait. */
    public boolean openUniverseFromTwig() {
        return twigOpensUniverse() && startNextUniverse();
    }

    /** Le premier clic fait germer la graine ; les suivants arrosent son germe, qui grandit. Faux si fermé. */
    public boolean tendSeed() {
        if (!canTendSeed()) return false;
        if (!state.seedSprouted()) state.setSeedSprouted(true);
        else state.setSeedWaterings(state.seedWaterings() + 1);
        return true;
    }

    /** Outil d'essai : de l'énergie du vide en plus. */
    public void testAddVoidEnergy(BigNum amount) {
        state.setVoidEnergy(state.voidEnergy().add(amount));
    }

    /** Outil d'essai : un niveau de plus à une amélioration de l'univers, sans coût ni effet. */
    public void testRaiseUniverseUpgrade(idle.core.universe.UniverseUpgrade upgrade) {
        state.setUniverseUpgradeLevel(upgrade.id(), state.universeUpgradeLevel(upgrade.id()) + 1);
    }

    /** Outil d'essai : la partie secondaire revient à son début (un seul univers, rien en réserve). */
    public void testResetUniverses() {
        state.setExtraUniverses(0);
        state.setUniverseSeeds(BigNum.ZERO);
        state.setVoidEnergy(BigNum.ZERO);
        state.setDimensionShards(0);
        state.setLinePhase(0);
        state.setLineYieldLevel(0);
        state.setLineTicksLevel(0);
        state.setLineSpeedLevel(0);
        state.setLineBallsLevel(0);
        state.setLineCountLevel(0);
        state.setDimensionShardsEarned(0);
        state.setSeedSprouted(false);
        state.setSeedWaterings(0);
        state.setPlaneShards(0);
        state.setPlaneShardsEarned(0);
        for (int i = 0; i < PlaneUpgrade.values().length; i++) state.setPlaneLevel(i, 0);
        for (idle.core.universe.UniverseUpgrade upgrade : idle.core.universe.UniverseUpgrade.values()) {
            state.setUniverseUpgradeLevel(upgrade.id(), 0);
        }
    }

    /** Vrai si cette échelle du cosmos est formée. */
    public boolean hasCosmos(Cosmos scale) {
        // L'univers reste formé après la remise à zéro qui l'accompagne (version 0.083), quand le cosmos repart de rien.
        // Pendant la partie du deuxième univers (version 0.087), seul compte ce que cette partie a formé.
        if (scale == Cosmos.UNIVERSE && state.universeFormed() && !state.replaying()) return true;
        return state.cosmosLevel() > scale.ordinal();
    }

    /** Nombre d'échelles du cosmos formées : 0 avant la galaxie, 3 une fois l'univers formé. */
    public int cosmosFormed() {
        return state.cosmosLevel();
    }

    /** La prochaine échelle du cosmos à former, ou {@code null} une fois l'univers formé. */
    public Cosmos nextCosmos() {
        return state.cosmosLevel() >= Cosmos.values().length ? null : Cosmos.values()[state.cosmosLevel()];
    }

    /**
     * Vrai une fois cette échelle en vue : la galaxie à la première étoile ({@link #isGalaxyUnlocked()}),
     * les suivantes dès que la précédente est formée.
     */
    public boolean isCosmosUnlocked(Cosmos scale) {
        return scale == Cosmos.GALAXY ? isGalaxyUnlocked() : hasCosmos(scale.previous());
    }

    /**
     * Vrai si cette échelle peut être formée maintenant : la précédente l'est, elle ne l'est pas
     * encore, et ce qu'elle demande est réuni. La galaxie demande tous les astres
     * ({@link #canFormGalaxy()}) ; les suivantes, assez de matière rassemblée dans chaque état
     * ({@link Cosmos#matter()}).
     */
    public boolean canFormCosmos(Cosmos scale) {
        if (scale == Cosmos.GALAXY) return canFormGalaxy();
        if (!state.started() || hasCosmos(scale) || !hasCosmos(scale.previous())) return false;
        if (cosmosSettleRemaining(scale) > 0) return false;
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            if (gatheredInState(need.getKey()) < need.getValue()) return false;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) < need.getValue()) return false;
        }
        return true;
    }

    /**
     * Temps de jeu qu'il faut laisser à l'échelle d'avant pour se rassembler avant de former l'amas,
     * puis l'univers (version 0.073) : deux heures chacun depuis la version 0.125 (45 minutes avant). Un joueur qui réunit la matière plus vite
     * attend ; l'infatigable, qui met déjà près d'une heure à chaque échelle, ne le voit pas.
     */
    public static final double COSMOS_SETTLE_SECONDS = 120 * 60;

    /** Secondes de jeu qu'il reste avant que cette échelle puisse se former, 0 si le délai est passé. */
    public double cosmosSettleRemaining(Cosmos scale) {
        if (scale == Cosmos.GALAXY || !hasCosmos(scale.previous())) return 0;
        // Version 0.126 : la date de l'échelle d'avant dans cette partie, pas la toute première fois qu'elle s'est formée.
        double since = state.stats().reachedThisUniverseAt(scale.previous() == Cosmos.GALAXY ? GameStats.Step.GALAXY : GameStats.Step.CLUSTER);
        if (Double.isNaN(since)) return 0;
        return Math.max(0, since + COSMOS_SETTLE_SECONDS - state.timePlayed());
    }

    private final Map<Molecule.State, Integer> sortsByState = new java.util.EnumMap<>(Molecule.State.class);
    int sortsByStateVersion = -1;

    /** Nombre de sortes de molécules rassemblées dans cet état : la variété que demandent l'amas de galaxies et l'univers ({@link Cosmos#sorts()}). */
    public int sortsInState(Molecule.State matter) {
        if (sortsByStateVersion != state.moleculesVersion()) {
            sortsByStateVersion = state.moleculesVersion();
            sortsByState.clear();
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) sortsByState.merge(molecule.state(), 1, Integer::sum);
            }
        }
        return sortsByState.getOrDefault(matter, 0);
    }

    /** Nombre de conditions de cette échelle déjà réunies, sur {@link #cosmosConditions(Cosmos)}. */
    public int cosmosConditionsMet(Cosmos scale) {
        if (scale == Cosmos.GALAXY) {
            return hasGalaxy() ? bodies.size() + 1 : bodiesMature() + (solarSystemsComplete() >= SYSTEMS_FOR_GALAXY ? 1 : 0);
        }
        int met = hasCosmos(scale.previous()) ? 1 : 0;
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            if (gatheredInState(need.getKey()) >= need.getValue()) met++;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) >= need.getValue()) met++;
        }
        return met;
    }

    /** Nombre de conditions de cette échelle : les astres du catalogue pour la galaxie ; sinon l'échelle d'avant, plus un état de la matière par ligne. */
    public int cosmosConditions(Cosmos scale) {
        return scale == Cosmos.GALAXY ? bodies.size() + 1 : 1 + scale.matter().size() + scale.sorts().size();
    }

    /**
     * Forme cette échelle du cosmos. C'est un achat unique, qui ne consomme rien : la matière reste
     * rassemblée, les astres restent formés. Elle multiplie par {@link Cosmos#factor()} chacune des
     * grandeurs que la matière augmente, et rien ne la défait, ni l'explosion ni le Big Bang.
     *
     * @return {@code true} si elle a été formée
     */
    public boolean formCosmos(Cosmos scale) {
        if (!canFormCosmos(scale)) return false;
        state.setCosmosLevel(scale.ordinal() + 1);
        noteStep(switch (scale) {
            case GALAXY -> GameStats.Step.GALAXY;
            case CLUSTER -> GameStats.Step.CLUSTER;
            case UNIVERSE -> GameStats.Step.UNIVERSE;
        });
        if (scale == Cosmos.UNIVERSE) formUniverse();
        return true;
    }

    // ------------------------------------------------------------------
    // Le deuxième univers (version 0.087)
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

    /**
     * Vrai dans le deuxième univers et au-delà : un univers est déjà formé. C'est là que s'ouvre
     * tout ce que la liste des idées range sous « tout se débloque lors du deuxième univers ».
     */
    public boolean isSecondUniverse() {
        return state.universeFormed();
    }

    /** Vrai pendant la partie qui mène à l'univers suivant : les anciens onglets et leurs paliers sont revenus. */
    public boolean isReplaying() {
        return state.universeFormed() && state.replaying();
    }

    /** Vrai si l'on peut rouvrir les paliers pour jouer l'univers suivant. */
    public boolean canStartNextUniverse() {
        // Version 0.110 : un univers atteint par l'échelle du cosmos seule (outil d'essai, ancienne partie) compte aussi.
        return isUniverseFormed() && !state.replaying();
    }

    /**
     * Rouvre les paliers (version 0.087) : les anciens onglets reviennent, pour une partie neuve, celle
     * de l'univers suivant. La partie a déjà été remise à zéro par la formation de l'univers. Former
     * l'univers à la fin de cette partie crée l'univers suivant, avec une remise à zéro et une graine de plus.
     */
    public boolean startNextUniverse() {
        if (!canStartNextUniverse()) return false;
        // Version 0.108 : les trois actes repartent vraiment de zéro, même si quelque chose s'est joué depuis la
        // formation de l'univers (les anciens onglets montrés, une partie d'avant). La partie secondaire reste :
        // univers, graine, dimensions, skins, succès et temps de jeu.
        boolean twig = isSeedTwig();
        boolean bySeed = canOpenNextUniverse();
        state.startUniverse();
        forgetDerived();
        state.setReplaying(true);
        if (twig) state.setTwigOpened(true);
        if (bySeed) state.setSeedUniversesOpened(state.seedUniversesOpened() + 1);
        return true;
    }

    /** Fusions faites depuis que le dernier univers s'est formé. */
    public long fusionsSinceUniverse() {
        return Math.max(0, state.stats().fusions() - state.fusionsAtUniverse());
    }

    /** Vrai une fois l'onglet Skins ouvert : cinq fusions dans le deuxième univers. */
    public boolean isSkinsUnlocked() {
        return state.skinsOpen();
    }

    /** Vrai si ce skin est débloqué. */
    public boolean hasSkin(idle.core.universe.Skin skin) {
        return state.unlockedSkins().contains(skin.id());
    }

    /** Nombre de skins débloqués. */
    public int skinsUnlocked() {
        int count = 0;
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) if (hasSkin(skin)) count++;
        return count;
    }

    /** Débloque un skin ; vrai s'il ne l'était pas. */
    public boolean unlockSkin(idle.core.universe.Skin skin) {
        if (hasSkin(skin)) return false;
        state.unlockSkin(skin.id());
        moleculeBonusesVersion = -1;
        return true;
    }

    /** Le skin choisi pour ce visuel, ou {@code null} pour l'apparence classique. */
    public idle.core.universe.Skin chosenSkin(idle.core.universe.SkinVisual visual) {
        idle.core.universe.Skin skin = idle.core.universe.Skin.byId(state.chosenSkins().get(visual.name()));
        return skin != null && skin.visual() == visual && hasSkin(skin) ? skin : null;
    }

    /** Choisit un skin débloqué pour son visuel ; {@code null} revient à l'apparence classique de ce visuel. */
    public boolean chooseSkin(idle.core.universe.SkinVisual visual, idle.core.universe.Skin skin) {
        if (skin != null && (skin.visual() != visual || !hasSkin(skin))) return false;
        state.chooseSkin(visual.name(), skin == null ? null : skin.id());
        return true;
    }

    /** Ce que les skins débloqués multiplient, sur toutes les ressources d'en dessous de l'univers. */
    public double skinBoost() {
        return 1 + idle.core.universe.Skin.BONUS * skinsUnlocked();
    }

    public boolean isRegimeUnlocked() {
        return state.regimeOpen();
    }

    /** Vrai si le régime rapide peut être lancé : ouvert, ni en cours ni au repos. */
    public boolean canStartRegime() {
        return isRegimeUnlocked() && state.regimeLeft() <= 0 && state.regimeRest() <= 0;
    }

    /** Lance le régime rapide : +15 % de cadence pendant 20 s, puis 40 s de repos. Un geste à la main, jamais automatique. */
    public boolean startRegime() {
        if (!canStartRegime()) return false;
        state.setRegimeLeft(REGIME_SECONDS);
        return true;
    }

    /** La cadence du régime rapide : ×1,15 pendant qu'il dure. */
    public double regimeMultiplier() {
        return state.regimeLeft() > 0 ? 1 + REGIME_GAIN : 1;
    }

    public boolean isSynergyUnlocked() {
        return state.synergyOpen();
    }

    /** La synergie par rang : +0,5 % au plus à chaque générateur, selon les autres rangs possédés dans le cycle. */
    public double synergyMultiplier() {
        if (!isSynergyUnlocked()) return 1;
        int others = Math.max(0, generatorCount() - 1);
        return 1 + SYNERGY_MAX * Math.min(1, others / (double) (SYNERGY_GENERATORS - 1));
    }

    /** Vrai une fois la prime de cycle parfait ouverte : Persistance achetée, dans le deuxième univers. */
    public boolean isPerfectCycleUnlocked() {
        return isSecondUniverse() && keepsUpgradesOnFusion();
    }

    /** Ce que la prime de cycle parfait multiplie dans les particules, pendant qu'elle dure : elle ne se cumule pas. */
    public double perfectCycleMultiplier() {
        return state.perfectLeft() > 0 ? 1 + PERFECT_GAIN : 1;
    }

    void notePerfectCycle(boolean capped) {
        if (!isPerfectCycleUnlocked()) return;
        if (capped) {
            state.setPerfectStreak(0);
            return;
        }
        state.setPerfectStreak(state.perfectStreak() + 1);
        if (state.perfectStreak() >= PERFECT_FUSIONS) {
            state.setPerfectStreak(0);
            state.setPerfectLeft(PERFECT_SECONDS);
        }
    }

    public boolean isCycleChoiceUnlocked() {
        return isSecondUniverse() && state.bigBangs() > 0;
    }

    /** Le choix du petit arbre pour ce cycle, ou {@code null}. */
    public idle.core.universe.CycleChoice cycleChoice() {
        if (!isCycleChoiceUnlocked() || state.cycleChoice() == null) return null;
        try {
            return idle.core.universe.CycleChoice.valueOf(state.cycleChoice());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Fait le choix du cycle : un seul, jusqu'au prochain Big Bang. */
    public boolean chooseCycle(idle.core.universe.CycleChoice choice) {
        if (!isCycleChoiceUnlocked() || choice == null || cycleChoice() != null) return false;
        state.setCycleChoice(choice.name());
        return true;
    }

    public boolean isFamilyPreferenceUnlocked() {
        return isSecondUniverse() && isTargetedSynthesisUnlocked();
    }

    /** La famille préférée, ou {@code null}. */
    public ElementCategory preferredFamily() {
        if (!isFamilyPreferenceUnlocked() || state.preferredFamily() == null) return null;
        try {
            return ElementCategory.valueOf(state.preferredFamily());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Choisit la famille préférée ({@code null} : aucune) ; la préférence repart de zéro. */
    public boolean setPreferredFamily(ElementCategory family) {
        if (!isFamilyPreferenceUnlocked()) return false;
        state.setPreferredFamily(family == null ? null : family.name());
        state.setPreferenceDraws(0);
        return true;
    }

    /** Ce par quoi la préférence multiplie les chances de sa famille : +5 % par tirage au hasard, ×2 au plus. */
    public double preferenceMultiplier(ElementCategory category) {
        ElementCategory preferred = preferredFamily();
        if (preferred == null || preferred != category) return 1;
        return 1 + PREFERENCE_STEP * Math.min(PREFERENCE_MAX_DRAWS, state.preferenceDraws());
    }

    /** Éléments gratuits à choisir maintenant : un tous les dix éléments découverts, dans le deuxième univers. */
    public int freeElementsAvailable() {
        if (!isSecondUniverse()) return 0;
        return Math.max(0, discoveredElements() / FREE_ELEMENT_EVERY - state.freeElementsUsed());
    }

    /** Prend gratuitement un élément pas encore découvert. */
    public boolean claimFreeElement(int atomicNumber) {
        if (freeElementsAvailable() <= 0 || elementCount(atomicNumber) > 0) return false;
        state.setElementCount(PeriodicTable.element(atomicNumber).number(), 1);
        state.setFreeElementsUsed(state.freeElementsUsed() + 1);
        return true;
    }

    /** Outil d'essai : forme l'univers sur-le-champ, comme si ses conditions étaient réunies (remise à zéro comprise). */
    public void testFormUniverse() {
        state.setCosmosLevel(Cosmos.values().length);
        noteStep(GameStats.Step.UNIVERSE);
        formUniverse();
    }

    /** Outil d'essai : compte des fusions de plus dans les statistiques, sans rien changer d'autre. */
    public void testAddFusions(int count) {
        for (int i = 0; i < count; i++) state.stats().noteFusion(BigNum.ONE, 1);
    }

    /** Outil d'essai : débloque tous les skins, ou les reverrouille tous. */
    public void testAllSkins(boolean unlocked) {
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) {
            if (unlocked) state.unlockSkin(skin.id());
            else state.lockSkin(skin.id());
        }
        moleculeBonusesVersion = -1;
    }

    /** Ce qui s'ouvre et s'écoule dans le deuxième univers, à chaque instant. */
    void runSecondUniverse(double dt) {
        if (!isSecondUniverse()) return;
        if (!state.skinsOpen() && fusionsSinceUniverse() >= SKINS_FUSIONS) state.setSkinsOpen(true);
        if (!state.regimeOpen() && generatorCount() >= REGIME_GENERATORS) state.setRegimeOpen(true);
        if (!state.synergyOpen() && generatorCount() >= SYNERGY_GENERATORS) state.setSynergyOpen(true);
        if (dt <= 0) return;
        // Le régime rapide et la prime de cycle parfait s'écoulent avec la production qu'ils multiplient :
        // voir Game#runTimedBonuses (version 0.126).
        noteSkinUnlocks();
        runTrial(dt);
    }

    // ------------------------------------------------------------------
    // Les skins à obtenir (version 0.088)
    // ------------------------------------------------------------------

    /** Le record de reconstruction qui donne le skin « Éclair » : de la fusion au dixième générateur en une minute au plus. */
    public static final double REBUILD_FAST_SECONDS = 60;
    /** Le prix de la recherche « Fusion prismatique », en particules. */
    public static final BigNum PRISM_COST = BigNum.of(1, 50);
    /** Atomes créés qu'il faut pour la recherche sur le noyau dense, et le prix de ses quatre paliers, en atomes. */
    public static final BigNum DENSE_UNLOCK_ATOMS = BigNum.of(1, 6);
    public static final BigNum[] DENSE_COSTS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 5), BigNum.of(1, 8)};
    private static final idle.core.universe.Skin[] DENSE_SKINS = {idle.core.universe.Skin.ATOM_DENSE, idle.core.universe.Skin.TABLE_LEAD,
            idle.core.universe.Skin.ATOM_STELLAR, idle.core.universe.Skin.TABLE_GOLD};
    /** Les paliers de la matière noire (gagnée dans le deuxième univers) et leurs skins. */
    public static final BigNum[] DARK_TIERS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 4), BigNum.of(1, 5)};
    private static final idle.core.universe.Skin[] DARK_SKINS = {idle.core.universe.Skin.DARK_OBSIDIAN, idle.core.universe.Skin.DARK_VORTEX,
            idle.core.universe.Skin.DARK_NEBULA, idle.core.universe.Skin.DARK_PULSAR};
    /** Les paliers du Big Bang (Big Bangs du deuxième univers) et leurs skins. */
    public static final int[] BANG_TIERS = {1, 3};
    private static final idle.core.universe.Skin[] BANG_SKINS = {idle.core.universe.Skin.BANG_AURORA, idle.core.universe.Skin.BANG_SUPERNOVA};

    /** Les paliers de la matière noire atteints, de 0 à 4. */
    public int darkSkinTiers() {
        int tiers = 0;
        for (BigNum tier : DARK_TIERS) if (darkMatterEarned().gte(tier)) tiers++;
        return tiers;
    }

    /** Les paliers du Big Bang atteints, de 0 à 2. */
    public int bangSkinTiers() {
        int tiers = 0;
        for (int tier : BANG_TIERS) if (state.bigBangs() >= tier) tiers++;
        return tiers;
    }

    /** Les skins des paliers et du record se débloquent seuls, dès que l'onglet Skins est ouvert. */
    private void noteSkinUnlocks() {
        if (!isSkinsUnlocked()) return;
        for (int i = 0; i < darkSkinTiers(); i++) unlockSkin(DARK_SKINS[i]);
        for (int i = 0; i < bangSkinTiers(); i++) unlockSkin(BANG_SKINS[i]);
        if (state.rebuildPending() && generatorCount() >= generatorsPerAtom()) {
            double time = state.timeSinceFusion();
            state.setRebuildPending(false);
            if (state.rebuildRecord() <= 0 || time < state.rebuildRecord()) state.setRebuildRecord(time);
            if (time <= REBUILD_FAST_SECONDS) unlockSkin(idle.core.universe.Skin.FUSION_FLASH);
        }
    }

    /** Le record de reconstruction, en secondes ; 0 sans record. */
    public double rebuildRecord() {
        return state.rebuildRecord();
    }

    /** Vrai si la recherche « Fusion prismatique » est faite. */
    public boolean hasPrismResearch() {
        return state.researches().contains("prism");
    }

    public boolean canBuyPrismResearch() {
        return isSkinsUnlocked() && !hasPrismResearch() && state.particles().gte(PRISM_COST);
    }

    /** La recherche facultative en particules : elle donne le skin « Prisme » de la fusion. */
    public boolean buyPrismResearch() {
        if (!canBuyPrismResearch()) return false;
        state.setParticles(state.particles().subtract(PRISM_COST).max(BigNum.ZERO));
        state.stats().addParticlesSpent(PRISM_COST);
        state.addResearch("prism");
        unlockSkin(idle.core.universe.Skin.FUSION_PRISM);
        return true;
    }

    /** Vrai une fois la recherche sur le noyau dense ouverte : un million d'atomes créés, dans le deuxième univers. */
    public boolean isDenseResearchUnlocked() {
        return isSkinsUnlocked() && state.stats().atomsCreated().gte(DENSE_UNLOCK_ATOMS);
    }

    /** Paliers de la recherche sur le noyau dense déjà faits, de 0 à 4. */
    public int denseTiers() {
        int tiers = 0;
        while (tiers < DENSE_COSTS.length && state.researches().contains("dense" + (tiers + 1))) tiers++;
        return tiers;
    }

    public boolean canBuyDenseTier() {
        int next = denseTiers();
        return isDenseResearchUnlocked() && next < DENSE_COSTS.length && state.atoms().gte(DENSE_COSTS[next]);
    }

    /** Le palier suivant de la recherche sur le noyau dense : un skin d'atome ou de tableau à chaque palier. */
    public boolean buyDenseTier() {
        if (!canBuyDenseTier()) return false;
        int next = denseTiers();
        state.setAtoms(state.atoms().subtract(DENSE_COSTS[next]).max(BigNum.ZERO));
        state.stats().addAtomsSpent(DENSE_COSTS[next]);
        state.addResearch("dense" + (next + 1));
        unlockSkin(DENSE_SKINS[next]);
        return true;
    }

    /** L'épreuve en cours, ou {@code null}. */
    public idle.core.universe.Trial activeTrial() {
        if (state.activeTrial() == null) return null;
        try {
            return idle.core.universe.Trial.valueOf(state.activeTrial());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Vrai pendant cette épreuve. */
    public boolean inTrial(idle.core.universe.Trial trial) {
        return trial.name().equals(state.activeTrial());
    }

    /** Vrai si cette épreuve a déjà été réussie. */
    public boolean isTrialCompleted(idle.core.universe.Trial trial) {
        return state.completedTrials().contains(trial.name());
    }

    /** Vrai si cette épreuve est ouverte : l'onglet Skins, et ce qu'elle demande. */
    public boolean isTrialUnlocked(idle.core.universe.Trial trial) {
        if (!isSkinsUnlocked()) return false;
        return switch (trial) {
            case LONE_GENERATOR -> state.explosions() > 0;
            case SOFT_CONTRACTION -> isDarkMatterUnlocked();
            case COLD_BANG -> state.bigBangs() > 0;
            case DETECTOR -> isColliderUnlocked() && colliderUpgradeLevel(ColliderUpgrade.WINDOW) >= 3;
            case NO_OVERHEAT, FULL_CHARGE, LONE_COLLIDER -> isColliderUnlocked();
        };
    }

    /** Vrai si l'épreuve peut commencer : ouverte, aucune autre en cours, et pour le premier générateur, un seul générateur. */
    public boolean canStartTrial(idle.core.universe.Trial trial) {
        if (!isTrialUnlocked(trial) || activeTrial() != null) return false;
        return trial != idle.core.universe.Trial.LONE_GENERATOR || generatorCount() == 1;
    }

    /** Commence une épreuve. */
    public boolean startTrial(idle.core.universe.Trial trial) {
        if (!canStartTrial(trial)) return false;
        state.setActiveTrial(trial.name());
        state.setTrialStart(state.timePlayed());
        state.setTrialCount(0);
        state.setTrialBase(trial == idle.core.universe.Trial.FULL_CHARGE
                ? colliderEnergyProduced() + colliderEnergyPerSecond() * idle.core.universe.Trial.FULL_CHARGE_SECONDS : 0);
        return true;
    }

    /** Abandonne l'épreuve en cours : rien n'est perdu, rien n'est gagné. */
    public boolean abandonTrial() {
        if (activeTrial() == null) return false;
        state.setActiveTrial(null);
        return true;
    }

    /** Secondes qu'il reste à l'épreuve en cours ; l'infini sans limite, 0 sans épreuve. */
    public double trialLeft() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return 0;
        if (trial.seconds() <= 0) return Double.POSITIVE_INFINITY;
        return Math.max(0, trial.seconds() - (state.timePlayed() - state.trialStart()));
    }

    /** Où en est l'épreuve en cours, de 0 à 1. */
    public double trialProgress() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return 0;
        return switch (trial) {
            case LONE_GENERATOR -> Math.min(1, Math.max(0, state.particles().log10() / idle.core.universe.Trial.LONE_GOAL_LOG10));
            case SOFT_CONTRACTION, COLD_BANG -> 0;
            case FULL_CHARGE -> state.trialBase() <= 0 ? 0 : Math.min(1, colliderEnergyProduced() / state.trialBase());
            default -> Math.min(1, state.trialCount() / (double) trial.goal());
        };
    }

    /** Ce que la contraction douce ajoute aux atomes de chaque fusion : +5 % par fusion du cycle, ×2 au plus. */
    public double contractionAtomsMultiplier() {
        if (!inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) return 1;
        return Math.min(idle.core.universe.Trial.CONTRACTION_MAX, 1 + idle.core.universe.Trial.CONTRACTION_FUSION_GAIN * state.trialCount());
    }

    void completeTrial() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return;
        state.setActiveTrial(null);
        state.addCompletedTrial(trial.name());
        unlockSkin(trial.skin());
    }

    void failTrial() {
        state.setActiveTrial(null);
    }

    /** L'épreuve en cours : sa contrainte au fil du temps, son but, sa limite. */
    private void runTrial(double dt) {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return;
        if (trial == idle.core.universe.Trial.SOFT_CONTRACTION && dt > 0) {
            state.setParticles(state.particles().multiply(Math.pow(1 - idle.core.universe.Trial.CONTRACTION_LOSS, dt / idle.core.universe.Trial.CONTRACTION_EVERY)));
        }
        if (trial == idle.core.universe.Trial.NO_OVERHEAT && state.overheatLeft() > 0) {
            failTrial();
            return;
        }
        boolean done = switch (trial) {
            case LONE_GENERATOR -> state.particles().log10() >= idle.core.universe.Trial.LONE_GOAL_LOG10;
            case SOFT_CONTRACTION, COLD_BANG -> false;
            case FULL_CHARGE -> state.trialBase() > 0 && colliderEnergyProduced() >= state.trialBase();
            default -> state.trialCount() >= trial.goal();
        };
        if (done) {
            completeTrial();
            return;
        }
        if (trial.seconds() > 0 && state.timePlayed() - state.trialStart() >= trial.seconds()) failTrial();
    }

    /**
     * Le guide de transition (version 0.127) : ce que fera la prochaine remise à zéro de l'univers, avec les chiffres
     * de la partie en cours. Il suit {@link GameState#startUniverse()} : ce qu'elle garde, il le dit gardé, et
     * {@code TransitionTest} le vérifie.
     *
     * @param launching vrai pour « Lancer l'univers » ({@link #startNextUniverse()}), faux pour la formation de l'univers
     */
    public idle.core.universe.UniverseTransition universeTransition(boolean launching) {
        java.util.List<String> lost = new java.util.ArrayList<>();
        if (launching) {
            lost.add("Ce qui s'est joué dans les trois actes depuis la formation de l'univers : tout repart du premier générateur.");
        } else {
            lost.add("Particules, atomes, éléments, améliorations et automatismes.");
            lost.add("Matière noire, son arbre et ses défis (" + state.completedChallenges().size() + " réussis).");
            lost.add(state.bigBangs() + " Big Bang" + (state.bigBangs() > 1 ? "s" : "") + ", leurs paliers et leurs défis.");
            lost.add("Molécules, assemblages, astres (" + bodiesFormed() + " formés), systèmes solaires, galaxie et amas.");
            lost.add("L'espace et ses améliorations, les comètes et les anomalies du ciel.");
            lost.add("Le collisionneur (énergie, générateurs, quarks, améliorations) et les sondes.");
            lost.add("Les plans suivis de l'arbre et des systèmes (leurs codes sauvegardés, eux, restent).");
        }
        // Remis à zéro par les deux transitions, et pas rendus ensuite (GameState#startUniverse).
        lost.add("Les records des défis et des explosions (ceux des étapes restent).");
        if (state.universeFormed()) {
            lost.add("Ce qui s'ouvre en cours de partie : régime rapide, synergie, choix de cycle, famille préférée, "
                    + "éléments offerts.");
        }
        idle.core.universe.Trial trial = activeTrial();
        if (trial != null) lost.add("L'épreuve en cours, « " + trial.label() + " » : elle s'arrête sans être réussie.");
        java.util.List<String> kept = new java.util.ArrayList<>();
        kept.add("Les succès (" + achievementCount() + "), les records des étapes, le temps de jeu et les statistiques.");
        // La partie secondaire n'existe qu'une fois un univers formé : à la toute première formation, elle est à venir.
        boolean secondary = state.universeFormed();
        if (secondary) {
            kept.add("Les graines d'univers (" + wholeText(state.universeSeeds()) + ") et l'énergie du vide ("
                    + wholeText(state.voidEnergy()) + ").");
            kept.add("Les dimensions : éclats de la ligne et du plan, et leurs améliorations.");
            kept.add("La graine : palier " + seedStage() + ", et ses arrosages."
                    + (launching && canOpenNextUniverse()
                    ? " L'univers d'après demandera le palier " + Math.min(SEED_STAGES, nextUniverseStage() + 1) + "."
                    : ""));
            kept.add("Les skins (" + state.unlockedSkins().size() + " débloqués) et ceux que vous avez choisis.");
            kept.add("Les améliorations d'univers, les épreuves réussies (" + state.completedTrials().size()
                    + ") et les recherches (" + state.researches().size() + ").");
        }
        kept.add("Vos réglages de jeu, les codes de plan sauvegardés et les fenêtres d'explication déjà vues.");
        java.util.List<String> gained = new java.util.ArrayList<>();
        if (launching) {
            gained.add("Une partie neuve, celle de l'univers suivant : les trois actes reviennent.");
        } else {
            gained.add("+" + SEEDS_PER_UNIVERSE + " graine d'univers.");
            if (!secondary) gained.add("L'onglet Univers s'ouvre : ce qui s'y gagne (graines, dimensions, skins) traverse "
                    + "ensuite chaque univers.");
            gained.add("Un univers de plus : la matière noire des explosions et sa croissance ×"
                    + (int) idle.core.GameDarkMatter.UNIVERSE_DARK_MATTER_FACTOR + " pour la suite.");
        }
        return new idle.core.universe.UniverseTransition(launching, lost, kept, gained);
    }

    /** Un nombre entier lisible pour le guide : « 3 », « 1 250 », puis en notation scientifique. */
    private static String wholeText(BigNum value) {
        if (value.lt(BigNum.of(1, 6))) {
            String digits = String.valueOf((long) Math.floor(value.toDouble() + 1e-9));
            return digits.replaceAll("(\\d)(?=(\\d{3})+$)", "$1\u00a0");
        }
        return value.toString();
    }

    /** Graines d'univers que donne la formation de l'univers (version 0.083). */
    public static final int SEEDS_PER_UNIVERSE = 1;

    /**
     * L'univers se forme (version 0.083) : tout ce qui y a mené est remis à zéro
     * ({@link GameState#startUniverse()}), et le joueur reçoit {@link #SEEDS_PER_UNIVERSE} graine
     * d'univers. Les succès gagnés par la formation sont relevés avant.
     */
    private void formUniverse() {
        checkAchievements();
        resetForUniverse();
        forgetDerived();
    }

    /** La remise à zéro de l'univers, son bilan relevé avant, et sa graine. */
    void resetForUniverse() {
        // Un univers de plus (version 0.087) : celui-ci était déjà formé, la partie rejouée en forme un autre.
        boolean another = state.universeFormed();
        state.stats().noteUniverseTally(state.bigBangs(), moleculesCreated(), state.collisionRuns(), state.cometsCaught());
        state.startUniverse();
        if (another) state.setExtraUniverses(state.extraUniverses() + 1);
        state.setUniverseSeeds(state.universeSeeds().add(BigNum.of(SEEDS_PER_UNIVERSE)));
    }
}
