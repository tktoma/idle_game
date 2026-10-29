package idle.core;

import idle.core.achievements.Achievement;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Body;
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
     * Le vide (version 0.099), deuxième acte de la partie secondaire. Depuis la version 0.129, il se propose au
     * premier Big Bang du troisième univers ({@link #isVoidOffered()}) et ne s'ouvre qu'une fois payé en énergie du
     * collisionneur ; vrai une fois ouvert dans cet univers.
     */
    public boolean isVoidUnlocked() {
        // La bulle du géant de gauche, en bas (version 0.148) : le vide toujours ouvert, sans le payer.
        return state.voidOpen() || hasBubble(TreeBubble.GIANT_LEFT_LOW);
    }

    /** Vrai quand le vide se propose dans le collisionneur : premier Big Bang du troisième univers passé, pas encore ouvert. */
    public boolean isVoidOffered() {
        return universes() >= 2 && state.bigBangs() > 0 && !isVoidUnlocked();
    }

    // ------------------------------------------------------------------
    // Les améliorations du vide (version 0.129), et l'allègement des besoins du ciel
    // ------------------------------------------------------------------

    /**
     * Les huit améliorations du vide, payées en énergie du vide : leur prix de départ, la croissance, le plafond. Dans
     * l'ordre de la sauvegarde : gain par paire, rythme, coût en énergie, espace par paire, fonte, allègement, maturation,
     * et depuis la version 0.132 le rassemblement. Version 0.132 : les cinq qui font grandir le vide montent bien plus vite
     * de prix (×3,7 à ×4,1 par niveau, au lieu de ×2,2 à ×2,6), pour que le vide ne se remplisse pas en une heure ; les
     * trois qui raccourcissent le troisième acte vont plus loin (maturation −70 %, rassemblement −80 %) et montent de
     * prix moins vite (×2) : elles se gagnent au fil de l'acte, mesurées sur le Speedrunner du troisième univers.
     */
    public enum VoidUpgrade implements Priced {
        GAIN(10, 3.7, Integer.MAX_VALUE), RATE(20, 3.9, 20), ENERGY(15, 4.0, 15), SPACE(15, 4.0, 15), DECAY(25, 4.1, 15),
        LIGHTEN(300, 2.0, 10), MATURE(200, 2.0, 10), SETTLE(800, 2.0, 5);

        private final Price price;

        VoidUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Le prix d'une amélioration à niveaux : {@code base × growth^niveau}, jusqu'au niveau {@code max} exclu. */
    public record Price(double base, double growth, int max) {
    }

    /** Une amélioration à niveaux dont le prix suit un {@link Price} : ce que partagent les améliorations des dimensions, du vide et de la graine. */
    public interface Priced {
        Price price();

        /** Le prix du premier niveau. */
        default double baseCost() {
            return price().base();
        }

        /** Ce par quoi le prix est multiplié à chaque niveau. */
        default double growth() {
            return price().growth();
        }

        /** Le niveau le plus haut. */
        default int max() {
            return price().max();
        }

        /** Le prix du niveau suivant {@code level} ; infini une fois au plus haut. */
        default double cost(int level) {
            return level >= max() ? Double.POSITIVE_INFINITY : baseCost() * Math.pow(growth(), level);
        }
    }

    /** L'espace que tiennent les paires du vide encore vivantes ({@link GameVoid}) : il compte dans l'espace utilisé. */
    public BigNum voidHeldSpace() {
        return BigNum.ZERO;
    }

    /** Ce que chaque niveau d'allègement retire aux besoins des astres, de l'amas et de l'univers : 3 %, dix niveaux au plus. */
    public static final double VOID_LIGHTEN_STEP = 0.03;

    public int voidLevel(VoidUpgrade upgrade) {
        return state.voidLevel(upgrade.ordinal());
    }

    /** La part des besoins du ciel qui reste avec l'allègement du vide : 1 sans lui, 0,7 au plus fort. */
    public double needShare() {
        return 1 - VOID_LIGHTEN_STEP * Math.min(VoidUpgrade.LIGHTEN.max(), voidLevel(VoidUpgrade.LIGHTEN));
    }

    /** Ce que chaque niveau de rassemblement du vide retire au délai de l'amas et de l'univers (version 0.132) : 16 %, cinq niveaux, −80 % au plus. */
    public static final double VOID_SETTLE_STEP = 0.16;

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

    private Map<String, Integer> lightenedMolecules(Map<String, Integer> raw) {
        if (needShare() >= 1) return raw;
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        raw.forEach((id, need) -> out.put(id, lightened(need)));
        return out;
    }

    private Map<Molecule.State, Integer> lightenedMatter(Map<Molecule.State, Integer> raw) {
        if (needShare() >= 1) return raw;
        Map<Molecule.State, Integer> out = new java.util.EnumMap<>(Molecule.State.class);
        raw.forEach((matter, need) -> out.put(matter, lightened(need)));
        return out;
    }

    /** Les molécules que demande cet astre, allégées par le vide (version 0.129). */
    public Map<String, Integer> bodyMolecules(Body body) {
        return lightenedMolecules(body.molecules());
    }

    /** La matière que demande cet astre, par état, allégée par le vide. */
    public Map<Molecule.State, Integer> bodyMatter(Body body) {
        return lightenedMatter(body.matter());
    }

    /** La matière que demande cette échelle du cosmos (l'amas, l'univers), par état, allégée par le vide. */
    public Map<Molecule.State, Integer> cosmosMatter(Cosmos scale) {
        return lightenedMatter(scale.matter());
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
    /**
     * Le seuil de la ligne (version 0.102) : tant d'éclats gagnés en tout, et la dimension 1 est marquée. Version 0.136 :
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

        private final Price price;

        LineUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
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
        // Version 0.151 : les omégas renforcent les éclats des quatre dimensions.
        return LINE_SHARD * (1 + state.lineYieldLevel()) * hadronBoost(idle.core.collider.Hadron.OMEGA);
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

    /** Éclats par seconde : chaque aller d'une boule touche tous les traits sauf celui d'où elle part. */
    public double lineShardsPerSecond() {
        return isLineOpen() ? (lineTicks() - 1) * lineShardsPerTouch() * lineBalls() * lineCount() / lineHalfTrip() : 0;
    }

    /** Éclats du plan à gagner pour son seuil (version 0.120) : 3 500 depuis la version 0.136 (100 avant), environ 15 minutes de jeu appliqué. */
    public static final double PLANE_THRESHOLD = 3_500;

    /** Vrai une fois le seuil du plan atteint : {@link #PLANE_THRESHOLD} éclats du plan gagnés en tout. */
    public boolean isPlaneThresholdReached() {
        return state.planeShardsEarned() >= PLANE_THRESHOLD - 1e-9;
    }

    /** Vrai si la dimension {@code number} (1 la ligne, 2 le plan, 3 le volume, 4 la durée, 5 la boucle, 6 la brane, 7 le clicker) a atteint son seuil ; faux pour celles qui n'existent pas encore. */
    public boolean isDimensionThresholdReached(int number) {
        return switch (number) {
            case 1 -> isLineThresholdReached();
            case 2 -> isPlaneThresholdReached();
            case 3 -> isVolumeThresholdReached();
            case 4 -> isDurationThresholdReached();
            case 5 -> isLoopThresholdReached();
            case 6 -> state.braneShardsEarned() >= BRANE_THRESHOLD - 1e-9;
            case 7 -> state.clickerShardsEarned() >= CLICKER_THRESHOLD - 1e-9;
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
        return state.lineLevel(upgrade.ordinal());
    }

    /** Le niveau le plus haut d'une amélioration de la ligne ; sans limite pour la densité. */
    public int lineMaxLevel(LineUpgrade upgrade) {
        return upgrade.max();
    }

    /** Prix du prochain niveau, en éclats ; infini une fois au plus haut. */
    public double lineCost(LineUpgrade upgrade) {
        return upgrade.cost(lineLevel(upgrade));
    }

    public boolean canBuyLine(LineUpgrade upgrade) {
        return isLineOpen() && lineLevel(upgrade) < lineMaxLevel(upgrade) && state.dimensionShards() >= lineCost(upgrade) - 1e-9;
    }

    /** Achète un niveau d'une amélioration de la ligne. */
    public boolean buyLine(LineUpgrade upgrade) {
        if (!canBuyLine(upgrade)) return false;
        state.setDimensionShards(Math.max(0, state.dimensionShards() - lineCost(upgrade)));
        state.setLineLevel(upgrade.ordinal(), lineLevel(upgrade) + 1);
        return true;
    }

    // Les améliorations de densité et de traits sous leur nom de la 0.101 : des tests s'en servent encore.
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
    public enum PlaneUpgrade implements Priced {
        VALUE(5, 3, Integer.MAX_VALUE), SPAWN(3, 2.5, 15), COUNT(4, 3, 17), SPEED(3, 2.5, 15), SIZE(4, 2.5, 15);

        private final Price price;

        PlaneUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
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
        return (1 + planeLevel(PlaneUpgrade.VALUE)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
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
        // La visée automatique (version 0.150) ne prend la main que quand le joueur ne tient aucune direction.
        double inputX = planeInputX, inputY = planeInputY;
        if (inputX == 0 && inputY == 0) {
            double[] auto = planeAutoDirection();
            if (auto != null) {
                inputX = auto[0];
                inputY = auto[1];
            }
        }
        double length = Math.hypot(inputX, inputY);
        if (length > 0) {
            double step = planeSpeed() * Math.min(dt, 0.25) / Math.max(1, length);
            setPlanePosition(planeX + inputX * step, planeY + inputY * step);
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

    /** La direction que donne la visée automatique du plan (version 0.150) ; {@code null} sans elle. */
    double[] planeAutoDirection() {
        return null;
    }

    /** Prix du prochain niveau, en éclats du plan ; infini une fois au plus haut. */
    public double planeCost(PlaneUpgrade upgrade) {
        return upgrade.cost(planeLevel(upgrade));
    }

    public boolean canBuyPlane(PlaneUpgrade upgrade) {
        return isPlaneOpen() && planeLevel(upgrade) < upgrade.max() && state.planeShards() >= planeCost(upgrade) - 1e-9;
    }

    public boolean buyPlane(PlaneUpgrade upgrade) {
        if (!canBuyPlane(upgrade)) return false;
        state.setPlaneShards(Math.max(0, state.planeShards() - planeCost(upgrade)));
        state.setPlaneLevel(upgrade.ordinal(), planeLevel(upgrade) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // Le volume, troisième dimension (version 0.128)
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

        private final Price price;

        VolumeUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Une boule qui flotte dans le volume, en attendant qu'on la cueille. */
    public record VolumeBall(double x, double y, double z) {
    }

    // Ce qui ne se sauvegarde pas : la plongée en cours, son minuteur, les boules du cube.
    private boolean inVolume;
    private double volumeTimeLeft, volumeSpawnTimer;
    private final java.util.List<VolumeBall> volumeBalls = new java.util.ArrayList<>();
    private final java.util.Random volumeRandom = new java.util.Random(3);
    private long volumeCollected, volumeDives;

    /** Vrai dès que la troisième dimension est ouverte : au troisième univers. */
    public boolean isVolumeOpen() {
        return dimensionsUnlocked() >= 3;
    }

    public int volumeLevel(VolumeUpgrade upgrade) {
        return state.volumeLevel(upgrade.ordinal());
    }

    /** Durée d'une plongée : 12 s, et 3 s de plus par niveau. */
    public double volumeDiveSeconds() {
        return VOLUME_DIVE_SECONDS + VOLUME_DIVE_STEP * volumeLevel(VolumeUpgrade.TIME);
    }

    /** Éclats du volume que donne chaque boule cueillie : 1, et 1 de plus par niveau. */
    public double volumeValue() {
        return (1 + volumeLevel(VolumeUpgrade.VALUE)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    /** Secondes entre deux boules : 1,5, ×0,85 par niveau. */
    public double volumeSpawnSeconds() {
        return VOLUME_SPAWN_SECONDS * Math.pow(0.85, volumeLevel(VolumeUpgrade.SPAWN));
    }

    /** Boules présentes à la fois, au plus : 4, et 1 de plus par niveau (vingt au plus). */
    public int volumeMaxBalls() {
        return VOLUME_MAX_BALLS + volumeLevel(VolumeUpgrade.COUNT);
    }

    /** Rayon d'une boule : 0,025, +20 % par niveau (quinze niveaux : ×4, 0,1). Plus grosse, elle se vise mieux. */
    public double volumeBallRadius() {
        return VOLUME_BALL_RADIUS * (1 + VOLUME_SIZE_STEP * volumeLevel(VolumeUpgrade.SIZE));
    }

    /** Vrai pendant une plongée : le joueur est au centre du cube. */
    public boolean isInVolume() {
        return inVolume;
    }

    /** Secondes qu'il reste avant que le cube rejette le joueur ; 0 hors du cube. */
    public double volumeTimeLeft() {
        return inVolume ? volumeTimeLeft : 0;
    }

    /** Les boules du cube, dans l'ordre où elles sont apparues. */
    public java.util.List<VolumeBall> volumeBalls() {
        return java.util.Collections.unmodifiableList(volumeBalls);
    }

    /** Boules cueillies depuis le lancement : l'interface y voit celles de la dernière image. */
    public long volumeCollected() {
        return volumeCollected;
    }

    /** Plongées commencées depuis le lancement. */
    public long volumeDives() {
        return volumeDives;
    }

    /**
     * Plonge dans le cube : le minuteur part de la durée d'une plongée, et le cube vide reçoit aussitôt sa
     * première boule. Impossible si la dimension est fermée ou si le joueur y est déjà.
     */
    public boolean enterVolume() {
        if (!isVolumeOpen() || inVolume) return false;
        inVolume = true;
        volumeTimeLeft = volumeDiveSeconds();
        volumeBalls.clear();
        volumeBalls.add(spawnVolumeBall());
        volumeSpawnTimer = 0;
        volumeDives++;
        return true;
    }

    /** Sort du cube : les boules qui restaient s'en vont avec la plongée. */
    public void leaveVolume() {
        inVolume = false;
        volumeTimeLeft = 0;
        volumeBalls.clear();
        volumeSpawnTimer = 0;
    }

    /**
     * Le volume avance de {@code dt} secondes pendant une plongée : les boules apparaissent à leur rythme tant
     * qu'il en manque, et le minuteur descend ; à zéro, le cube rejette le joueur.
     */
    void runVolume(double dt) {
        // La plongée automatique (version 0.150) : elle replonge dès qu'elle sort, et vise une boule à son rythme.
        if (isVolumeAutomatic() && isVolumeOpen() && dt > 0) {
            if (!inVolume) enterVolume();
            volumeShotTimer += dt;
            while (volumeShotTimer >= VOLUME_AUTO_SHOT_SECONDS && !volumeBalls.isEmpty()) {
                volumeShotTimer -= VOLUME_AUTO_SHOT_SECONDS;
                VolumeBall ball = volumeBalls.get(0);
                shootVolume(ball.x(), ball.y(), ball.z());
            }
            if (volumeBalls.isEmpty()) volumeShotTimer = Math.min(volumeShotTimer, VOLUME_AUTO_SHOT_SECONDS);
        }
        if (!inVolume || dt <= 0) return;
        if (!isVolumeOpen()) {
            leaveVolume();
            return;
        }
        // Les boules n'apparaissent que dans le temps qui reste : une longue image ne remplit pas un cube déjà fini.
        double lived = Math.min(dt, volumeTimeLeft);
        volumeSpawnTimer += lived;
        double every = volumeSpawnSeconds();
        while (volumeSpawnTimer >= every && volumeBalls.size() < volumeMaxBalls()) {
            volumeSpawnTimer -= every;
            volumeBalls.add(spawnVolumeBall());
        }
        // Plein : le minuteur attend, il ne s'accumule pas.
        if (volumeBalls.size() >= volumeMaxBalls()) volumeSpawnTimer = Math.min(volumeSpawnTimer, every);
        volumeTimeLeft -= dt;
        if (volumeTimeLeft <= 1e-9) leaveVolume();
    }

    /** Une boule à un endroit tiré au hasard dans le cube, entre {@link #VOLUME_SPAWN_NEAR} et {@link #VOLUME_SPAWN_FAR} du centre. */
    private VolumeBall spawnVolumeBall() {
        double limit = VOLUME_EDGE - volumeBallRadius();
        for (int tries = 0; tries < 64; tries++) {
            double x = (volumeRandom.nextDouble() * 2 - 1) * limit;
            double y = (volumeRandom.nextDouble() * 2 - 1) * limit;
            double z = (volumeRandom.nextDouble() * 2 - 1) * limit;
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d >= VOLUME_SPAWN_NEAR && d <= VOLUME_SPAWN_FAR) return new VolumeBall(x, y, z);
        }
        return new VolumeBall(0, 0, (VOLUME_SPAWN_NEAR + VOLUME_SPAWN_FAR) / 2);
    }

    /**
     * Cueille la boule que touche le regard lancé du centre du cube dans la direction ({@code dx}, {@code dy},
     * {@code dz}) : la plus proche que le rayon traverse. Rend la boule cueillie, ou {@code null} s'il n'y en a pas
     * (hors du cube, direction nulle, rayon qui passe à côté).
     */
    public VolumeBall shootVolume(double dx, double dy, double dz) {
        if (!inVolume) return null;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(length > 1e-12) || !Double.isFinite(length)) return null;
        double ux = dx / length, uy = dy / length, uz = dz / length, radius = volumeBallRadius();
        VolumeBall hit = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (VolumeBall ball : volumeBalls) {
            double along = ball.x() * ux + ball.y() * uy + ball.z() * uz;
            if (along <= 0) continue;
            double aside = ball.x() * ball.x() + ball.y() * ball.y() + ball.z() * ball.z() - along * along;
            if (aside <= radius * radius + 1e-12 && along < nearest) {
                nearest = along;
                hit = ball;
            }
        }
        if (hit == null) return null;
        volumeBalls.remove(hit);
        volumeCollected++;
        state.setVolumeShards(state.volumeShards() + volumeValue());
        state.setVolumeShardsEarned(state.volumeShardsEarned() + volumeValue());
        return hit;
    }

    /** La plongée automatique cueille une boule toutes les {@value} secondes. */
    public static final double VOLUME_AUTO_SHOT_SECONDS = 0.4;
    private double volumeShotTimer;

    /** Vrai quand la plongée automatique joue le volume (version 0.150). */
    boolean isVolumeAutomatic() {
        return false;
    }

    /** Pose une boule dans le cube : pour les essais. */
    public void addVolumeBall(double x, double y, double z) {
        if (volumeBalls.size() < volumeMaxBalls()) volumeBalls.add(new VolumeBall(x, y, z));
    }

    /** Outil d'essai : retire toutes les boules du cube. */
    public void testClearVolume() {
        volumeBalls.clear();
        volumeSpawnTimer = 0;
    }

    /** Vrai une fois le seuil du volume atteint : {@link #VOLUME_THRESHOLD} éclats du volume gagnés en tout. */
    public boolean isVolumeThresholdReached() {
        return state.volumeShardsEarned() >= VOLUME_THRESHOLD - 1e-9;
    }

    /** Prix du prochain niveau, en éclats du volume ; infini une fois au plus haut. */
    public double volumeCost(VolumeUpgrade upgrade) {
        return upgrade.cost(volumeLevel(upgrade));
    }

    /** Les améliorations du volume s'achètent hors du cube, jamais pendant une plongée. */
    public boolean canBuyVolume(VolumeUpgrade upgrade) {
        return isVolumeOpen() && !inVolume && volumeLevel(upgrade) < upgrade.max()
                && state.volumeShards() >= volumeCost(upgrade) - 1e-9;
    }

    public boolean buyVolume(VolumeUpgrade upgrade) {
        if (!canBuyVolume(upgrade)) return false;
        state.setVolumeShards(Math.max(0, state.volumeShards() - volumeCost(upgrade)));
        state.setVolumeLevel(upgrade.ordinal(), volumeLevel(upgrade) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // La durée, quatrième dimension (version 0.134)
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
     * l'ordre de la sauvegarde : vitesse des spirales, nombre de spirales, valeur, temps au plus. Version 0.136 : chaque
     * niveau coûte une fois de plus le prix du précédent (×3,4, ×4, ×4, ×3,2 au lieu de ×2,4, ×3, ×3, ×2,2) ; avant, les
     * trois améliorations plafonnées étaient au plus haut en trois minutes.
     */
    public enum DurationUpgrade implements Priced {
        SPEED(3, 3.4, 15), COUNT(6, 4, 9), VALUE(5, 4, Integer.MAX_VALUE), TIME(4, 3.2, 20);

        private final Price price;

        DurationUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Le bouton tenu, et depuis combien de secondes ; ni l'un ni l'autre ne se sauvegarde. */
    private boolean durationHeld;
    private double durationHeldSeconds;
    private long durationReleases;
    private double durationLastReward;

    /** La durée s'ouvre avec le quatrième univers : une dimension par univers. */
    public boolean isDurationOpen() {
        return dimensionsUnlocked() >= 4;
    }

    public int durationLevel(DurationUpgrade upgrade) {
        return state.durationLevel(upgrade.ordinal());
    }

    /** Le temps au bout duquel les spirales atteignent le centre : 6 s, et 2 s de plus par niveau. */
    public double durationMaxSeconds() {
        return DURATION_MAX_SECONDS + DURATION_TIME_STEP * durationLevel(DurationUpgrade.TIME);
    }

    /** Les tours que fait chaque spirale par seconde tenue : 0,5, ×1,2 par niveau de vitesse. */
    public double durationTurnsPerSecond() {
        return DURATION_TURNS * Math.pow(1 + DURATION_SPEED_STEP, durationLevel(DurationUpgrade.SPEED));
    }

    /** Les spirales qui avancent ensemble : 1, et une de plus par niveau. */
    public int durationSpirals() {
        return 1 + durationLevel(DurationUpgrade.COUNT);
    }

    /** Ce que vaut le premier tour d'une spirale ; le n-ième vaut n fois autant. */
    public double durationValue() {
        return (1 + durationLevel(DurationUpgrade.VALUE)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    /** Les tours d'une spirale entière, du bord au centre. */
    public double durationTurnsMax() {
        return durationTurnsPerSecond() * durationMaxSeconds();
    }

    /** Ce que donne un relâchement après {@code seconds} secondes tenues : chaque spirale, (n² + n) ÷ 2 fois la valeur pour n tours. */
    public double durationRewardFor(double seconds) {
        double turns = durationTurnsPerSecond() * Math.max(0, Math.min(seconds, durationMaxSeconds()));
        return durationSpirals() * durationValue() * (turns * turns + turns) / 2;
    }

    public boolean isDurationHeld() {
        return durationHeld;
    }

    /** Depuis combien de secondes le bouton est tenu, sans dépasser le temps au plus. */
    public double durationHeldSeconds() {
        return durationHeld ? durationHeldSeconds : 0;
    }

    /** Où en sont les spirales : 0 au bord, 1 au centre. */
    public double durationProgress() {
        return durationHeld ? Math.min(1, durationHeldSeconds / durationMaxSeconds()) : 0;
    }

    /** Ce que donnerait un relâchement maintenant. */
    public double durationPending() {
        return durationHeld ? durationRewardFor(durationHeldSeconds) : 0;
    }

    /** Les relâchements qui ont donné des éclats, et ce qu'a donné le dernier : pour la fenêtre. */
    public long durationReleases() {
        return durationReleases;
    }

    public double durationLastReward() {
        return durationLastReward;
    }

    /** Le bouton est enfoncé : les spirales repartent du bord. Faux si la dimension est fermée ou déjà tenue. */
    public boolean holdDuration() {
        if (!isDurationOpen() || durationHeld) return false;
        durationHeld = true;
        durationHeldSeconds = 0;
        return true;
    }

    /** Le bouton est relâché : les spirales donnent leurs éclats et repartent du bord. Rend les éclats donnés. */
    public double releaseDuration() {
        if (!durationHeld) return 0;
        double reward = isDurationOpen() ? durationRewardFor(durationHeldSeconds) : 0;
        durationHeld = false;
        durationHeldSeconds = 0;
        if (reward > 0) {
            state.setDurationShards(state.durationShards() + reward);
            state.setDurationShardsEarned(state.durationShardsEarned() + reward);
            durationReleases++;
            durationLastReward = reward;
        }
        return reward;
    }

    /** Le bouton lâché sans rien donner : une partie relue, un outil d'essai. */
    public void dropDuration() {
        durationHeld = false;
        durationHeldSeconds = 0;
    }

    /** Le temps passe pour le bouton tenu, jusqu'au temps au plus. */
    void runDuration(double dt) {
        // L'appui automatique (version 0.150) : tenu jusqu'au centre, relâché, repris, autant de fois que le temps le permet.
        if (isDurationAutomatic() && isDurationOpen() && dt > 0) {
            if (!durationHeld) {
                durationHeld = true;
                durationHeldSeconds = 0;
            }
            double max = durationMaxSeconds(), total = durationHeldSeconds + dt;
            long cycles = (long) Math.floor(total / max + 1e-9);
            if (cycles > 0) {
                double each = durationRewardFor(max), reward = each * cycles;
                state.setDurationShards(state.durationShards() + reward);
                state.setDurationShardsEarned(state.durationShardsEarned() + reward);
                durationReleases += cycles;
                durationLastReward = each;
                total = Math.max(0, total - cycles * max);
            }
            durationHeldSeconds = total;
            return;
        }
        if (!durationHeld || dt <= 0) return;
        if (!isDurationOpen()) {
            durationHeld = false;
            durationHeldSeconds = 0;
            return;
        }
        durationHeldSeconds = Math.min(durationMaxSeconds(), durationHeldSeconds + dt);
    }

    /** Vrai quand l'appui automatique joue la durée (version 0.150). */
    boolean isDurationAutomatic() {
        return false;
    }

    /** Vrai une fois le seuil de la durée atteint : {@link #DURATION_THRESHOLD} éclats de la durée gagnés en tout. */
    public boolean isDurationThresholdReached() {
        return state.durationShardsEarned() >= DURATION_THRESHOLD - 1e-9;
    }

    /** Prix du prochain niveau, en éclats de la durée ; infini une fois au plus haut. */
    public double durationCost(DurationUpgrade upgrade) {
        return upgrade.cost(durationLevel(upgrade));
    }

    /**
     * Les améliorations de la durée s'achètent bouton relâché : les spirales ne changent pas en cours de route. Sauf
     * pendant l'appui automatique (version 0.150), qui ne relâche jamais : elles valent alors dès le cycle en cours.
     */
    public boolean canBuyDuration(DurationUpgrade upgrade) {
        return isDurationOpen() && (!durationHeld || isDurationAutomatic()) && durationLevel(upgrade) < upgrade.max()
                && state.durationShards() >= durationCost(upgrade) - 1e-9;
    }

    public boolean buyDuration(DurationUpgrade upgrade) {
        if (!canBuyDuration(upgrade)) return false;
        state.setDurationShards(Math.max(0, state.durationShards() - durationCost(upgrade)));
        state.setDurationLevel(upgrade.ordinal(), durationLevel(upgrade) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // La graine qui germe (version 0.103)
    // ------------------------------------------------------------------

    /**
     * Vrai si la graine peut être arrosée : l'univers est formé (il n'y a qu'une graine, la sienne) et elle n'est pas
     * encore l'arbre-monde. Version 0.139 : les seuils des dimensions ne la retiennent plus (de la 0.103 à la 0.138, il
     * fallait l'objectif de la dimension 1 pour la faire germer, puis le seuil de la dimension de chaque palier) ; ils
     * ne donnent plus que le rond vert de leur dimension.
     */
    public boolean canTendSeed() {
        return isUniverseFormed() && seedStage() < SEED_STAGES;
    }

    /** Arrosages qui font du germe une brindille (version 0.106) : c'est elle qui ouvre le deuxième univers. */
    public static final long SEED_TWIG_WATERINGS = 30;

    /**
     * Les paliers de la graine (version 0.120), de 1 à {@link #SEED_STAGES} : le palier N ouvre l'univers N. Chacun
     * demande {@link #SEED_TWIG_WATERINGS} arrosages de plus que le précédent (version 0.139 : seulement des arrosages ;
     * avant, aussi le seuil de la dimension du palier d'avant).
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
        while (stage < SEED_STAGES && state.seedWaterings() >= seedStageWaterings(stage + 1)) stage++;
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

    // ------------------------------------------------------------------
    // L'arrosoir (version 0.137)
    // ------------------------------------------------------------------

    /** Arrosages par seconde que donne l'arrosoir penché sur la graine. */
    public static final double WATERING_CAN_PER_SECOND = 1;

    /** L'arrosoir penché sur la graine, et la part d'arrosage déjà versée ; ni l'un ni l'autre ne se sauvegarde. */
    private boolean watering;
    private double wateringPoured;

    /** Vrai une fois l'arrosoir débloqué. */
    public boolean isWateringCanUnlocked() {
        return state.wateringCan();
    }

    /** L'arrosoir se débloque dès que la graine existe, c'est-à-dire dès le premier univers formé ; il est gratuit. */
    public boolean canUnlockWateringCan() {
        return isUniverseFormed() && !state.wateringCan();
    }

    /** Débloque l'arrosoir, posé sur son étagère. */
    public boolean unlockWateringCan() {
        if (!canUnlockWateringCan()) return false;
        state.setWateringCan(true);
        return true;
    }

    /**
     * L'arrosoir est penché sur la graine ({@code true}) ou repris ({@code false}). Penché, il verse
     * {@link #WATERING_CAN_PER_SECOND} arrosage par seconde, tant que la graine peut être arrosée : le premier la fait
     * germer, comme le faisait le premier clic du bouton.
     */
    public void setWatering(boolean pouring) {
        boolean next = pouring && state.wateringCan();
        if (!next) wateringPoured = 0;
        watering = next;
    }

    public boolean isWatering() {
        return watering;
    }

    /** La part de l'arrosage en cours déjà versée, de 0 à 1. */
    public double wateringPoured() {
        return watering ? wateringPoured : 0;
    }

    /**
     * L'arrosoir tenu et le tuyau (version 0.139) versent pendant {@code dt} secondes : un arrosage à chaque arrosage
     * plein, tant que la graine l'accepte. Le tuyau verse seul, sans qu'on le tienne.
     */
    void runWatering(double dt) {
        if (dt <= 0) return;
        if (watering && !state.wateringCan()) setWatering(false);
        if (!canTendSeed()) {
            wateringPoured = 0;
            hosePoured = 0;
            return;
        }
        if (watering) {
            wateringPoured += dt * wateringCanRate();
            while (wateringPoured >= 1 && canTendSeed()) {
                wateringPoured -= 1;
                tendSeed();
            }
        }
        double hose = hoseRate();
        if (hose > 0) {
            hosePoured += dt * hose;
            while (hosePoured >= 1 && canTendSeed()) {
                hosePoured -= 1;
                tendSeed();
            }
        }
        if (!canTendSeed()) {
            wateringPoured = 0;
            hosePoured = 0;
        }
    }

    /**
     * Le premier arrosage fait germer la graine ; les suivants arrosent son germe, qui grandit, et chacun compte pour
     * {@link #seedSoilFactor()} grâce à la terre fertile (version 0.139). Faux si fermé.
     */
    public boolean tendSeed() {
        if (!canTendSeed()) return false;
        if (!state.seedSprouted()) state.setSeedSprouted(true);
        else state.setSeedWaterings(state.seedWaterings() + seedSoilFactor());
        return true;
    }

    // ------------------------------------------------------------------
    // Les bulles de l'arbre (version 0.142)
    // ------------------------------------------------------------------

    /**
     * Les bulles de l'arbre (version 0.142, réseau complet à la 0.143) : un réseau d'améliorations posé sur l'arbre de la
     * graine, payé avec l'énergie du vide accumulée. Au palier qui ouvre l'univers 4 (la fin de la dimension 3), une
     * bulle naît à la base, sur la graine ; chaque palier suivant en ajoute, reliées aux bulles d'avant, jusqu'à
     * l'arbre-monde : 1, 2, 3, 2, 2, 2, 4 puis 3 (dont une dans les racines, sous la graine). Une bulle s'ouvre quand une
     * bulle à laquelle elle est reliée est achetée : la base d'abord, puis le réseau de proche en proche. Pour
     * l'instant, un visuel : achetée, une bulle s'allume, sans autre effet.
     *
     * <p>L'ordre des bulles est celui de la sauvegarde (un bit par bulle) : les nouvelles s'ajoutent à la fin.
     */
    public enum TreeBubble {
        // Les prix : le Speedrunner garde environ 4 500 d'énergie du vide à la formation de l'univers 3 (mesuré à la
        // 0.142) ; de quoi payer la base et une bulle du bonsaï. Ensuite, environ ×1,5 par palier (choix arbitraire).
        // Les effets (version 0.148) ; les bulles qui se rachètent coûtent deux fois plus à chaque niveau.
        SEED(4, "Bulle de la graine", 1_000, 1,
                "+1 place de maturation des astres par dimension ouverte"),
        LEAVES_LEFT(5, "Bulle du bonsaï, à gauche", 2_000, 5,
                "×2 sur l'énergie du vide gagnée, par niveau", 0),
        LEAVES_RIGHT(5, "Bulle du bonsaï, à droite", 2_000, 1,
                "Les automatismes agissent sans délai et achètent tout ce qu'ils peuvent", 0),
        CROWN_LEFT(6, "Bulle de l'arbuste, à gauche", 4_000, 1,
                "Le collisionneur est ouvert dès le début de l'univers", 1),
        CROWN_MIDDLE(6, "Bulle de l'arbuste, au milieu", 4_000, 1,
                "L'onglet des hadrons se propose dans le collisionneur, comme le vide", 1, 2),
        CROWN_RIGHT(6, "Bulle de l'arbuste, à droite", 4_000, 1,
                "Persistance dès le début, et des atomes chaque seconde selon les générateurs, sans fusionner", 2),
        YOUNG_LEFT(7, "Bulle du jeune arbre, à gauche", 6_000, 1,
                "Les hadrons sont ouverts dès le début de l'univers, sans les payer", 3, 4),
        YOUNG_RIGHT(7, "Bulle du jeune arbre, à droite", 6_000, 1,
                "Plus de plafond de générateurs, mais au-delà de l'ancien, leur prix grimpe très vite", 4, 5),
        MATURE_LEFT(8, "Bulle de l'arbre mûr, à gauche", 9_000, 1,
                "Le collisionneur traverse les univers : toujours ouvert, jamais remis à zéro", 3, 6),
        MATURE_RIGHT(8, "Bulle de l'arbre mûr, à droite", 9_000, 1,
                "Un nouvel univers garde la matière noire, son arbre, les explosions et les défis réussis", 5, 7),
        ANCIENT_LEFT(9, "Bulle de l'arbre ancien, à gauche", 13_000, 1,
                "Les hadrons traversent les univers : jamais remis à zéro", 6, 8),
        ANCIENT_RIGHT(9, "Bulle de l'arbre ancien, à droite", 13_000, 1,
                "Les astres n'ont plus besoin de mûrir", 7, 9),
        GIANT_LEFT_HIGH(10, "Bulle de l'arbre géant, tout à gauche", 20_000, 5,
                "5 % de l'énergie du vide gagnée vont aussi, chaque seconde, au cumul des bulles, par niveau", 10),
        GIANT_LEFT_LOW(10, "Bulle de l'arbre géant, à gauche, en bas", 20_000, 1,
                "Le vide traverse les univers : toujours ouvert, jamais remis à zéro", 8, 12),
        GIANT_RIGHT_HIGH(10, "Bulle de l'arbre géant, tout à droite", 20_000, 1,
                "De la matière noire chaque seconde selon les atomes, sans exploser", 11),
        GIANT_RIGHT_LOW(10, "Bulle de l'arbre géant, à droite, en bas", 20_000, 1,
                "Un nouvel univers garde les défis du Big Bang réussis, et tous les paliers du Big Bang sont ouverts", 9, 14),
        ROOTS(11, "Bulle des racines", 30_000, 1,
                "Le défi final (à venir)", 0, 13, 15),
        HEART(11, "Bulle de l'arbre-monde, au cœur", 30_000, 1,
                "Les particules exotiques s'assemblent : pions, kaons, mésons charmés…", 4, 6, 7),
        SUMMIT(11, "Bulle de l'arbre-monde, au sommet", 30_000, 1,
                "L'automatisation des dimensions : des automatismes qui les jouent et achètent leurs améliorations", 10, 11, 17);

        /** Ce que chaque niveau de plus coûte de plus, pour les bulles qui se rachètent : ×2. */
        public static final double LEVEL_GROWTH = 2;

        private final int stage;
        private final String label;
        private final double cost;
        private final int maxLevel;
        private final String effect;
        /** Les bulles d'avant auxquelles elle est reliée (leurs rangs) : chaque lien n'est noté qu'une fois, ici. */
        private final int[] below;

        TreeBubble(int stage, String label, double cost, int maxLevel, String effect, int... below) {
            this.stage = stage;
            this.label = label;
            this.cost = cost;
            this.maxLevel = maxLevel;
            this.effect = effect;
            this.below = below;
        }

        /** Combien de fois elle s'achète : 1, ou 5 pour les deux qui se rachètent (version 0.148). */
        public int maxLevel() {
            return maxLevel;
        }

        /** Ce qu'elle fait, en une phrase (version 0.148) ; « à venir » pour celles qui attendent leur onglet. */
        public String effect() {
            return effect;
        }

        /** Vrai pour celles dont l'effet n'existe pas encore : les hadrons, l'automatisation des dimensions, le défi final. */
        public boolean isComingSoon() {
            return effect.contains("(à venir)");
        }

        /** Le palier de la graine où elle naît. */
        public int stage() {
            return stage;
        }

        public String label() {
            return label;
        }

        /** Son prix, en énergie du vide accumulée : celui du premier niveau. */
        public double cost() {
            return cost;
        }

        /** Le prix du niveau suivant quand elle en a déjà {@code level} : ×{@link #LEVEL_GROWTH} par niveau. */
        public double costAt(int level) {
            return cost * Math.pow(LEVEL_GROWTH, Math.max(0, level));
        }

        /** Les bulles d'avant auxquelles elle est reliée ; aucune pour celle de la base. */
        public java.util.List<TreeBubble> below() {
            java.util.List<TreeBubble> list = new java.util.ArrayList<>();
            for (int index : below) list.add(values()[index]);
            return list;
        }

        /** Toutes les bulles reliées à elle : celles d'avant, et celles d'après qui la nomment. */
        public java.util.List<TreeBubble> links() {
            java.util.List<TreeBubble> list = new java.util.ArrayList<>(below());
            for (TreeBubble other : values()) if (other.below().contains(this)) list.add(other);
            return list;
        }
    }

    /**
     * L'énergie du vide qu'on peut dépenser : ce qu'il restait à la formation de chaque univers, additionné, moins ce
     * que les bulles ont déjà coûté.
     */
    public double voidAvailable() {
        return Math.max(0, state.voidAccumulated() - state.voidSpent());
    }

    /** Vrai si la bulle est née : l'univers formé, la graine à son palier. */
    public boolean isTreeBubbleShown(TreeBubble bubble) {
        return isUniverseFormed() && seedStage() >= bubble.stage;
    }

    public boolean isTreeBubbleBought(TreeBubble bubble) {
        return state.treeBubbleBought(bubble.ordinal());
    }

    /** Le niveau de la bulle (version 0.148) : 0 pas achetée ; jusqu'à {@link TreeBubble#maxLevel()}. */
    public int treeBubbleLevel(TreeBubble bubble) {
        return Math.min(bubble.maxLevel(), state.treeBubbleLevel(bubble.ordinal()));
    }

    /** Vrai quand la bulle est à son dernier niveau. */
    public boolean isTreeBubbleMaxed(TreeBubble bubble) {
        return treeBubbleLevel(bubble) >= bubble.maxLevel();
    }

    /** Le prix de son prochain niveau, en énergie du vide accumulée. */
    public double treeBubbleCost(TreeBubble bubble) {
        return bubble.costAt(treeBubbleLevel(bubble));
    }

    /** Les bulles nées : pour l'affichage. */
    public java.util.List<TreeBubble> treeBubblesShown() {
        java.util.List<TreeBubble> shown = new java.util.ArrayList<>();
        for (TreeBubble bubble : TreeBubble.values()) if (isTreeBubbleShown(bubble)) shown.add(bubble);
        return shown;
    }

    /**
     * Vrai si la bulle est née et qu'une bulle reliée à elle est achetée (version 0.143 : d'un côté comme de l'autre du
     * lien). La base n'attend rien : c'est par elle que le réseau commence.
     */
    public boolean isTreeBubbleOpen(TreeBubble bubble) {
        if (!isTreeBubbleShown(bubble)) return false;
        if (bubble == TreeBubble.SEED) return true;
        for (TreeBubble linked : bubble.links()) if (isTreeBubbleBought(linked)) return true;
        return false;
    }

    public boolean canBuyTreeBubble(TreeBubble bubble) {
        return isTreeBubbleOpen(bubble) && !isTreeBubbleMaxed(bubble) && voidAvailable() >= treeBubbleCost(bubble) - 1e-9;
    }

    /** Achète un niveau de la bulle avec l'énergie du vide accumulée ; vrai si c'est fait. */
    public boolean buyTreeBubble(TreeBubble bubble) {
        if (!canBuyTreeBubble(bubble)) return false;
        state.setVoidSpent(state.voidSpent() + treeBubbleCost(bubble));
        state.setTreeBubbleLevel(bubble.ordinal(), treeBubbleLevel(bubble) + 1);
        bubbleBought(bubble);
        return true;
    }

    /** Ce qu'une bulle change tout de suite, à son achat (version 0.148) : rien par défaut. */
    void bubbleBought(TreeBubble bubble) {
        switch (bubble) {
            // Le vide s'ouvre pour de bon : la suite le garde (state.startUniverse).
            case GIANT_LEFT_LOW -> state.setVoidOpen(true);
            // Plus de maturation : les astres qui mûrissaient sont mûrs.
            case ANCIENT_RIGHT -> {
                for (String id : java.util.List.copyOf(state.maturing().keySet())) state.setMaturing(id, 0);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // Les améliorations de la graine et le chrono (version 0.139)
    // ------------------------------------------------------------------

    /**
     * Le nom de la ressource de chaque dimension (version 0.139), ses éclats : les segments de la ligne, les facettes
     * du plan, les sphères du volume, les instants de la durée. De 5 à 10, pas encore de nom : ces dimensions n'existent
     * pas encore.
     */
    private static final java.util.List<String> DIMENSION_RESOURCES = java.util.List.of("segments", "facettes", "sphères", "instants", "nœuds", "ondes", "étincelles");

    /** Le nom de la ressource de la dimension {@code number}, ou « éclats de la dimension N » pour celles qui n'existent pas. */
    public static String dimensionResourceName(int number) {
        return number >= 1 && number <= DIMENSION_RESOURCES.size() ? DIMENSION_RESOURCES.get(number - 1)
                : "éclats de la dimension " + number;
    }

    /** La réserve de la ressource de la dimension {@code number} (1 à 7) ; 0 pour les autres. */
    public double dimensionResource(int number) {
        return switch (number) {
            case 1 -> state.dimensionShards();
            case 2 -> state.planeShards();
            case 3 -> state.volumeShards();
            case 4 -> state.durationShards();
            case 5 -> state.loopShards();
            case 6 -> state.braneShards();
            case 7 -> state.clickerShards();
            default -> 0;
        };
    }

    /** Retire {@code amount} de la ressource de la dimension {@code number} (1 à 7), sans descendre sous zéro. */
    void spendDimensionResource(int number, double amount) {
        switch (number) {
            case 1 -> state.setDimensionShards(Math.max(0, state.dimensionShards() - amount));
            case 2 -> state.setPlaneShards(Math.max(0, state.planeShards() - amount));
            case 3 -> state.setVolumeShards(Math.max(0, state.volumeShards() - amount));
            case 4 -> state.setDurationShards(Math.max(0, state.durationShards() - amount));
            case 5 -> state.setLoopShards(Math.max(0, state.loopShards() - amount));
            case 6 -> state.setBraneShards(Math.max(0, state.braneShards() - amount));
            case 7 -> state.setClickerShards(Math.max(0, state.clickerShards() - amount));
            default -> throw new IllegalArgumentException("Pas de ressource pour la dimension " + number);
        }
    }

    /**
     * Les améliorations de la graine, payées avec la ressource d'une dimension : quatre pour l'arrosage et l'étagère,
     * quatre pour le chrono, une par dimension dans chaque groupe. Dans l'ordre de la sauvegarde.
     */
    public enum SeedUpgrade implements Priced {
        /** Segments de la ligne : +1 arrosage par seconde de l'arrosoir tenu, par niveau. */
        WATERING(1, "Débit de l'arrosoir", 25_000, 3, 20, false),
        /** Facettes du plan : le tuyau d'arrosage, qui verse seul 10 % de ce que verse l'arrosoir, +10 % par niveau. */
        HOSE(2, "Tuyau d'arrosage", 350, 2.5, 10, false),
        /** Sphères du volume : la terre fertile, chaque arrosage compte double, ×2 encore par niveau. */
        SOIL(3, "Terre fertile", 800, 3, 10, false),
        /** Instants de la durée : un étage de plus à l'étagère, avec le chrono. */
        CHRONO(4, "Étage du chrono", 1_500_000, 1, 1, false),
        /** Segments : le chrono se remplit plus vite. */
        CHRONO_FILL(1, "Remplissage du chrono", 50_000, 3, 20, true),
        /** Facettes : il garde plus de temps. */
        CHRONO_CAPACITY(2, "Réserve du chrono", 500, 2.5, 20, true),
        /** Sphères : il accélère davantage. */
        CHRONO_SPEED(3, "Accélération du chrono", 1_000, 3, 8, true),
        /** Instants : il perd moins à grande vitesse. */
        CHRONO_LOSS(4, "Pertes du chrono", 2_000_000, 4, 15, true),
        /** Nœuds de la boucle (version 0.153) : le rond de croissance dure plus longtemps, +10 s par niveau. */
        BLOOM_DURATION(5, "Durée du rond de croissance", 200, 2.5, 15, false),
        /** Nœuds : le rond de croissance accélère davantage l'arrosage, +10 points par niveau. */
        BLOOM_POWER(5, "Force du rond de croissance", 300, 2.7, 20, false),
        /** Ondes de la brane (version 0.154) : la lumière astrale, +20 % d'arrosage par seconde de l'arrosoir et du tuyau, par niveau. */
        ASTRAL_LIGHT(6, "Lumière astrale", 400, 2.8, 20, false),
        /** Étincelles du clicker (version 0.155) : le beau temps dure plus longtemps, +10 s par niveau. */
        WEATHER_SUN(7, "Beau temps plus long", 500, 2.6, 15, false),
        /** Étincelles : les orages passent plus vite, −3 s par niveau. */
        WEATHER_STORM(7, "Orages plus courts", 400, 2.5, 8, false),
        /** Étincelles : le beau temps arrose davantage, +10 points par niveau. */
        WEATHER_POWER(7, "Soleil plus fort", 600, 2.8, 20, false);

        private final int dimension;
        private final String label;
        private final Price price;
        private final boolean chrono;

        SeedUpgrade(int dimension, String label, double cost, double growth, int max, boolean chrono) {
            this.dimension = dimension;
            this.label = label;
            this.price = new Price(cost, growth, max);
            this.chrono = chrono;
        }

        /** La dimension dont la ressource la paie. */
        public int dimension() {
            return dimension;
        }

        public String label() {
            return label;
        }

        @Override
        public Price price() {
            return price;
        }

        /** Vrai pour celles du chrono, qui attendent son étage. */
        public boolean chrono() {
            return chrono;
        }
    }

    public int seedUpgradeLevel(SeedUpgrade upgrade) {
        return state.seedUpgradeLevel(upgrade.ordinal());
    }

    /** Prix du prochain niveau, dans la ressource de sa dimension ; infini une fois au plus haut. */
    public double seedUpgradeCost(SeedUpgrade upgrade) {
        // Arrondi à l'unité au-dessus : les croissances ×2,5 ne laissent pas de prix à virgule.
        return Math.ceil(upgrade.cost(seedUpgradeLevel(upgrade)) - 1e-9);
    }

    /**
     * Vrai si l'amélioration est ouverte : sa dimension existe, l'arrosoir est débloqué (pour son débit), l'étage du
     * chrono est là (pour celles du chrono).
     */
    public boolean isSeedUpgradeOpen(SeedUpgrade upgrade) {
        if (!isUniverseFormed() || dimensionsUnlocked() < upgrade.dimension) return false;
        if (upgrade == SeedUpgrade.WATERING && !state.wateringCan()) return false;
        return !upgrade.chrono || isChronoUnlocked();
    }

    public boolean canBuySeedUpgrade(SeedUpgrade upgrade) {
        return isSeedUpgradeOpen(upgrade) && seedUpgradeLevel(upgrade) < upgrade.max()
                && dimensionResource(upgrade.dimension) >= seedUpgradeCost(upgrade) - 1e-9;
    }

    public boolean buySeedUpgrade(SeedUpgrade upgrade) {
        if (!canBuySeedUpgrade(upgrade)) return false;
        spendDimensionResource(upgrade.dimension, seedUpgradeCost(upgrade));
        state.setSeedUpgradeLevel(upgrade.ordinal(), seedUpgradeLevel(upgrade) + 1);
        return true;
    }

    /** Arrosages par seconde de l'arrosoir tenu : 1, et 1 de plus par niveau de débit. */
    public double wateringCanRate() {
        return WATERING_CAN_PER_SECOND * (1 + seedUpgradeLevel(SeedUpgrade.WATERING)) * wateringBoost();
    }

    /** Ce qui accélère l'arrosage pour un temps (version 0.153) : le rond de croissance ; 1 sans lui. */
    public double wateringBoost() {
        return 1;
    }

    /** Ondes de la brane à gagner pour son seuil (version 0.154). */
    public static final double BRANE_THRESHOLD = 5_000;

    /** Étincelles du clicker à gagner pour son seuil (version 0.155). */
    public static final double CLICKER_THRESHOLD = 5_000;

    /** Nœuds de la boucle à gagner pour son seuil (version 0.153) : un quart d'heure de jeu appliqué environ. */
    public static final double LOOP_THRESHOLD = 5_000;

    /** Vrai une fois le seuil de la boucle atteint : {@link #LOOP_THRESHOLD} nœuds gagnés en tout. */
    public boolean isLoopThresholdReached() {
        return state.loopShardsEarned() >= LOOP_THRESHOLD - 1e-9;
    }

    /** Ce que verse le tuyau, en part de ce que verse l'arrosoir : 10 % par niveau, 0 sans tuyau. */
    public double hoseShare() {
        return 0.1 * seedUpgradeLevel(SeedUpgrade.HOSE);
    }

    /** Arrosages par seconde du tuyau, qui verse seul. */
    public double hoseRate() {
        return hoseShare() * wateringCanRate();
    }

    public boolean hasHose() {
        return seedUpgradeLevel(SeedUpgrade.HOSE) > 0;
    }

    /** Ce que compte chaque arrosage grâce à la terre fertile : 1, puis ×2 par niveau. */
    public long seedSoilFactor() {
        return 1L << Math.min(40, seedUpgradeLevel(SeedUpgrade.SOIL));
    }

    /** Le tuyau verse seul : la part déjà versée de son prochain arrosage, qui ne se sauvegarde pas. */
    private double hosePoured;

    // Le chrono : il se remplit de temps tout seul, et ce temps sert à faire passer le jeu plus vite.

    /** Au départ, le chrono garde 0,1 seconde par seconde de jeu, 5 minutes au plus, et accélère jusqu'à ×2. */
    public static final double CHRONO_FILL = 0.1, CHRONO_CAPACITY = 300, CHRONO_MAX_SPEED = 2;
    /** Ce que perd le chrono par point d'accélération au-delà de ×1 : 10 %, ×0,85 par niveau de pertes, 90 % au plus. */
    public static final double CHRONO_LOSS_STEP = 0.1, CHRONO_LOSS_CUT = 0.85, CHRONO_LOSS_MAX = 0.9;

    public boolean isChronoUnlocked() {
        return seedUpgradeLevel(SeedUpgrade.CHRONO) > 0;
    }

    /** Secondes gardées par seconde : 0,1, et 0,1 de plus par niveau de remplissage. */
    public double chronoFillRate() {
        return CHRONO_FILL * (1 + seedUpgradeLevel(SeedUpgrade.CHRONO_FILL));
    }

    /** Le temps que garde le chrono au plus : 5 minutes, et 5 de plus par niveau de réserve. */
    public double chronoCapacity() {
        return CHRONO_CAPACITY * (1 + seedUpgradeLevel(SeedUpgrade.CHRONO_CAPACITY));
    }

    /** L'accélération la plus forte : ×2, et +1 par niveau. */
    public double chronoMaxSpeed() {
        return CHRONO_MAX_SPEED + seedUpgradeLevel(SeedUpgrade.CHRONO_SPEED);
    }

    /** La part perdue du temps du chrono à l'accélération {@code speed} : plus on va vite, plus on perd. */
    public double chronoLoss(double speed) {
        double extra = Math.max(0, speed - 1);
        return Math.min(CHRONO_LOSS_MAX, CHRONO_LOSS_STEP * extra * Math.pow(CHRONO_LOSS_CUT, seedUpgradeLevel(SeedUpgrade.CHRONO_LOSS)));
    }

    /** Le temps gardé dans le chrono, en secondes. */
    public double chronoStored() {
        return state.chronoStored();
    }

    /** L'accélération choisie, de ×1 (arrêté) à {@link #chronoMaxSpeed()}. */
    public double chronoSpeed() {
        return Math.max(1, Math.min(chronoMaxSpeed(), state.chronoSpeed()));
    }

    /** Règle l'accélération, entre ×1 et le plus fort ; ×1 l'arrête. */
    public void setChronoSpeed(double speed) {
        state.setChronoSpeed(Double.isFinite(speed) ? Math.max(1, Math.min(chronoMaxSpeed(), speed)) : 1);
    }

    /** Vrai tant que le chrono accélère le jeu : réglé au-dessus de ×1, avec du temps gardé. */
    public boolean isChronoRunning() {
        return isChronoUnlocked() && chronoSpeed() > 1 + 1e-9 && state.chronoStored() > 1e-9;
    }

    /**
     * Le chrono pendant {@code dt} secondes réelles : il se remplit, et s'il accélère, il rend le temps de jeu à jouer.
     * À l'accélération k, une seconde réelle en vaut k ; les k − 1 de plus se prennent dans le chrono, plus ce qui
     * s'en perd ({@link #chronoLoss(double)}). Sans assez de temps gardé, l'accélération se réduit à ce qui reste.
     * Une absence remplit le chrono sans rien accélérer.
     */
    double runChrono(double dt, boolean accelerate) {
        if (!isChronoUnlocked() || dt <= 0) return dt;
        double stored = Math.min(chronoCapacity(), state.chronoStored() + dt * chronoFillRate());
        double played = dt;
        double speed = chronoSpeed();
        if (accelerate && speed > 1) {
            double keep = 1 - chronoLoss(speed);
            double extra = (speed - 1) * dt, cost = extra / keep;
            if (cost > stored) {
                extra = stored * keep;
                cost = stored;
            }
            stored -= cost;
            played += extra;
        }
        state.setChronoStored(Math.max(0, stored));
        return played;
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
        state.setVoidOpen(false);
        state.setVoidHoleOpen(false);
        state.setVoidSacrificed(BigNum.ZERO);
        for (int i = 0; i < VoidUpgrade.values().length; i++) state.setVoidLevel(i, 0);
        state.clearVoidHistory();
        state.setDimensionShards(0);
        state.setLinePhase(0);
        for (int i = 0; i < LineUpgrade.values().length; i++) state.setLineLevel(i, 0);
        state.setDimensionShardsEarned(0);
        state.setSeedSprouted(false);
        state.setSeedWaterings(0);
        state.setWateringCan(false);
        setWatering(false);
        for (int i = 0; i < SeedUpgrade.values().length; i++) state.setSeedUpgradeLevel(i, 0);
        state.setChronoStored(0);
        state.setChronoSpeed(1);
        state.clearTreeBubbles();
        state.setVoidSpent(0);
        state.setVoidTrickled(0);
        state.setVoidCarried(0);
        state.setHadronsOpen(false);
        state.setHadronsPrice(0);
        state.clearDimensionAutomations();
        for (idle.core.collider.Hadron hadron : idle.core.collider.Hadron.values()) state.setHadrons(hadron, 0);
        state.setPlaneShards(0);
        state.setPlaneShardsEarned(0);
        for (int i = 0; i < PlaneUpgrade.values().length; i++) state.setPlaneLevel(i, 0);
        state.setVolumeShards(0);
        state.setVolumeShardsEarned(0);
        for (int i = 0; i < VolumeUpgrade.values().length; i++) state.setVolumeLevel(i, 0);
        state.setDurationShards(0);
        state.setDurationShardsEarned(0);
        for (int i = 0; i < DurationUpgrade.values().length; i++) state.setDurationLevel(i, 0);
        state.setLoopShards(0);
        state.setLoopShardsEarned(0);
        for (int i = 0; i < GameStateBase.LOOP_UPGRADES; i++) state.setLoopLevel(i, 0);
        state.setBraneShards(0);
        state.setBraneShardsEarned(0);
        for (int i = 0; i < GameStateBase.BRANE_UPGRADES; i++) state.setBraneLevel(i, 0);
        state.setClickerShards(0);
        state.setClickerShardsEarned(0);
        for (int i = 0; i < GameStateBase.CLICKER_UPGRADES; i++) state.setClickerLevel(i, 0);
        dropDuration();
        leaveVolume();
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
        for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(scale).entrySet()) {
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
        return Math.max(0, since + COSMOS_SETTLE_SECONDS * settleShare() - state.timePlayed());
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
        for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(scale).entrySet()) {
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
    private static final BigNum[] DARK_TIERS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 4), BigNum.of(1, 5)};
    private static final idle.core.universe.Skin[] DARK_SKINS = {idle.core.universe.Skin.DARK_OBSIDIAN, idle.core.universe.Skin.DARK_VORTEX,
            idle.core.universe.Skin.DARK_NEBULA, idle.core.universe.Skin.DARK_PULSAR};
    /** Les paliers du Big Bang (Big Bangs du deuxième univers) et leurs skins. */
    private static final int[] BANG_TIERS = {1, 3};
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
            // Les bulles de l'arbre (version 0.148) en gardent une part : elle est dite gardée plus bas.
            if (!hasBubble(TreeBubble.MATURE_RIGHT)) {
                lost.add("Matière noire, son arbre et ses défis (" + state.completedChallenges().size() + " réussis).");
            }
            lost.add(state.bigBangs() + " Big Bang" + (state.bigBangs() > 1 ? "s" : "")
                    + (hasBubble(TreeBubble.GIANT_RIGHT_LOW) ? " et leurs paliers." : ", leurs paliers et leurs défis."));
            lost.add("Molécules, assemblages, astres (" + bodiesFormed() + " formés), systèmes solaires, galaxie et amas.");
            lost.add("L'espace et ses améliorations, les comètes et les anomalies du ciel.");
            lost.add(hasBubble(TreeBubble.MATURE_LEFT) ? "Les sondes."
                    : "Le collisionneur (énergie, générateurs, quarks, améliorations) et les sondes.");
            lost.add("Les plans suivis de l'arbre et des systèmes (leurs codes sauvegardés, eux, restent).");
            // Les hadrons (version 0.149) : repris, sauf avec la bulle de l'arbre ancien, à gauche.
            if (state.hadronsOpen() && !hasBubble(TreeBubble.ANCIENT_LEFT)) {
                lost.add("Les hadrons (" + hadronCount() + " assemblés) et leur onglet, à rouvrir dans le collisionneur.");
            }
            // Le vide (version 0.129) : sa réserve repart de zéro, ce qu'il en reste rejoint le cumul.
            if (state.voidOpen() && !hasBubble(TreeBubble.GIANT_LEFT_LOW)) {
                lost.add("Le vide, son trou noir et ses améliorations : les " + wholeText(state.voidEnergy())
                        + " d'énergie du vide qui restent s'ajoutent au cumul.");
            }
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
            kept.add("Les graines d'univers (" + wholeText(state.universeSeeds()) + ") et le cumul du vide ("
                    + wholeText(BigNum.of(state.voidAccumulated() + (!launching && state.voidOpen() ? state.voidEnergy().toDouble() : 0)))
                    + ").");
            // Version 0.128 : le volume traverse les univers lui aussi.
            // Version 0.134 : la durée traverse les univers elle aussi.
            kept.add(dimensionsUnlocked() >= 7 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle, de la brane et du clicker, et leurs améliorations."
                    : dimensionsUnlocked() >= 6 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle et de la brane, et leurs améliorations."
                    : dimensionsUnlocked() >= 5 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée et de la boucle, et leurs améliorations."
                    : isDurationOpen() ? "Les dimensions : éclats de la ligne, du plan, du volume et de la durée, et leurs améliorations."
                    : isVolumeOpen() ? "Les dimensions : éclats de la ligne, du plan et du volume, et leurs améliorations."
                    : "Les dimensions : éclats de la ligne et du plan, et leurs améliorations.");
            kept.add("La graine : palier " + seedStage() + ", et ses arrosages."
                    + (launching && canOpenNextUniverse()
                    ? " L'univers d'après demandera le palier " + Math.min(SEED_STAGES, nextUniverseStage() + 1) + "."
                    : ""));
            kept.add("Les skins (" + state.unlockedSkins().size() + " débloqués) et ceux que vous avez choisis.");
            kept.add("Les améliorations d'univers, les épreuves réussies (" + state.completedTrials().size()
                    + ") et les recherches (" + state.researches().size() + ").");
        }
        kept.add("Vos réglages de jeu, les codes de plan sauvegardés et les fenêtres d'explication déjà vues.");
        // Les bulles de l'arbre qui font traverser les univers (version 0.148), comme GameState#startUniverse.
        // L'automatisation des dimensions (version 0.150) traverse toujours les univers.
        if (!state.dimensionAutomations().isEmpty()) {
            kept.add("Les automatismes des dimensions (" + state.dimensionAutomations().size() + " achetés).");
        }
        if (hasBubble(TreeBubble.MATURE_LEFT)) kept.add("Le collisionneur, grâce à la bulle de l'arbre mûr, à gauche.");
        if (hasBubble(TreeBubble.GIANT_LEFT_LOW)) {
            kept.add("Le vide, son énergie (" + wholeText(state.voidEnergy()) + "), son trou noir et ses améliorations, grâce à la "
                    + "bulle du géant de gauche" + (launching ? "." : " ; ce qu'il a gagné depuis la dernière formation rejoint aussi le cumul."));
        }
        if (hasBubble(TreeBubble.MATURE_RIGHT)) {
            kept.add("La matière noire, son arbre, les explosions et les défis réussis (" + state.completedChallenges().size()
                    + "), grâce à la bulle de l'arbre mûr, à droite.");
        }
        if (hasBubble(TreeBubble.ANCIENT_LEFT) && (state.hadronsOpen() || hadronCount() > 0)) {
            kept.add("Les hadrons (" + hadronCount() + " assemblés), grâce à la bulle de l'arbre ancien, à gauche.");
        }
        if (hasBubble(TreeBubble.GIANT_RIGHT_LOW)) {
            kept.add("Les défis du Big Bang réussis (" + state.completedBangChallenges().size() + "), grâce à la bulle du géant de droite.");
        }
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

    /** Les hadrons assemblés, toutes sortes (version 0.149), pour le guide. */
    private long hadronCount() {
        long total = 0;
        for (idle.core.collider.Hadron hadron : idle.core.collider.Hadron.values()) total += state.hadrons(hadron);
        return total;
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
        // Le vide (version 0.129) : ce qu'il en reste à la formation s'ajoute au cumul, gardé d'un univers à l'autre.
        if (state.voidOpen()) {
            // Version 0.152 : l'énergie que le vide gardé (bulle du géant de gauche, en bas) apportait d'un univers d'avant
            // est déjà dans le cumul ; seule celle gagnée depuis s'y ajoute. Elle était comptée à chaque formation.
            double stock = state.voidEnergy().toDouble();
            state.noteVoidHistory(String.valueOf(another ? 2 + state.extraUniverses() : 1), Math.max(0, stock - state.voidCarried()));
            state.setVoidCarried(hasBubble(TreeBubble.GIANT_LEFT_LOW) ? stock : 0);
        }
        state.startUniverse();
        if (another) state.setExtraUniverses(state.extraUniverses() + 1);
        state.setUniverseSeeds(state.universeSeeds().add(BigNum.of(SEEDS_PER_UNIVERSE)));
    }
}
