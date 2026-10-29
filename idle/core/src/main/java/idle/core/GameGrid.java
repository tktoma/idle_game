package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * La grille, huitième dimension (version 0.156, revue à la 0.157) : une énorme grille de {@value #GRID_CELLS} cases
 * sur {@value #GRID_CELLS} sur {@value #GRID_CELLS}, où un point est caché au hasard. Le joueur a un essai par point
 * caché : il choisit une case, et plus elle est proche du point, plus il gagne de repères (la ressource). S'il tombe
 * sur la case même, il le trouve : le gain est multiplié. Dans les deux cas, le point se cache ailleurs pour l'essai
 * suivant. Une courte attente suit chaque essai.
 *
 * <p>Améliorations ({@link GridUpgrade}) : réduction de la grille (moins de cases par côté, plus de chances de toucher
 * la cible), gain, affichage du point (d'abord la distance, puis sa couche, puis sa colonne), nombre de points,
 * multiplicateur de proximité, essais (plusieurs essais par point, guidés par l'affichage).
 *
 * <p>Avec la grille vient la graine profonde : une amélioration de la graine, payée en repères, qui multiplie tout
 * l'arrosage par la racine carrée de la puissance de dix de l'énergie du vide récupérée.
 */
public abstract class GameGrid extends GameClicker {

    GameGrid(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Les améliorations de la grille, dans l'ordre de la sauvegarde. */
    public enum GridUpgrade implements Priced {
        /** Réduit la grille : ×0,7 cases par côté par niveau, de 1 000 à 2. */
        DIVISION(5, 2.4, 18),
        /** Chaque essai donne un repère de plus par niveau. */
        GAIN(6, 3, Integer.MAX_VALUE),
        /** Ce que la grille montre : la distance du dernier essai, puis la couche du point le plus proche, puis sa colonne. */
        REVEAL(40, 6, 3),
        /** Un point caché de plus par niveau : 7 au plus. */
        POINTS(30, 3.5, 6),
        /** La proximité compte davantage : +1 au multiplicateur par niveau. */
        PROXIMITY(10, 2.8, 20),
        /** Un essai de plus par point caché (version 0.157) : 4 au plus. */
        ATTEMPTS(60, 8, 3);

        private final Price price;

        GridUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Un point caché, en coordonnées de 0 à {@value #GRID_SIZE} (la case se lit de ses coordonnées). */
    public record GridPoint(double x, double y, double z) {
        public double distance(double px, double py, double pz) {
            return Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
        }
    }

    /**
     * Ce qu'a donné un essai : la case, la distance au point visé (en cases), s'il l'a trouvé, le gain, et les essais
     * qu'il reste sur ce point (0 : il s'est caché ailleurs), et le numéro du point visé (version 0.158) : l'interface
     * n'y garde que les essais des points encore cachés là, et efface les couleurs des anciens.
     */
    public record GridProbe(int i, int j, int k, double distance, boolean found, double gain, int triesLeft, long pointId) {
    }

    /** Le côté du cube, en coordonnées : les cases le découpent, mille par côté au départ. */
    public static final double GRID_SIZE = 1_000;
    /** Mille cases par côté au départ : un milliard de cases. */
    public static final int GRID_CELLS = 1_000;
    /** Chaque niveau de réduction : ×0,7 cases par côté, 2 au moins. */
    public static final double GRID_SHRINK = 0.7;
    public static final int GRID_MIN_CELLS = 2;
    /** Le multiplicateur de proximité : ×(1 + 4 p³) au départ, p allant de 0 (au plus loin) à 1 (sur le point) ; +1 par niveau. */
    public static final double GRID_PROXIMITY = 4, GRID_PROXIMITY_STEP = 1;
    /** Trouver un point (l'essai tombe sur sa case) : ×5. */
    public static final double GRID_FOUND = 5;
    /** L'attente après chaque essai, en secondes. */
    public static final double GRID_COOLDOWN = 0.5;
    /** L'automatisme essaie toutes les {@value} secondes. */
    public static final double GRID_AUTO_SECONDS = 2;

    private final java.util.Random gridRandom = new java.util.Random(8);
    private final List<GridPoint> gridPoints = new ArrayList<>();
    /** Les essais qu'il reste sur chaque point, dans l'ordre de {@link #gridPoints}. */
    private final List<Integer> gridTries = new ArrayList<>();
    /** Le numéro de chaque point, dans l'ordre de {@link #gridPoints} : un nouveau à chaque fois qu'un point se cache. */
    private final List<Long> gridIds = new ArrayList<>();
    private long gridNextId = 1;
    private double gridWait, gridAutoTimer;
    private GridProbe gridLast;
    private long gridProbes, gridFound, gridAutoProbes;

    /** Vrai dès que la huitième dimension est ouverte : au huitième univers. */
    public boolean isGridOpen() {
        return dimensionsUnlocked() >= 8;
    }

    public int gridLevel(GridUpgrade upgrade) {
        return state.gridLevel(upgrade.ordinal());
    }

    /** Le nombre de cases par côté : 1 000, ×0,7 par niveau de réduction, 2 au moins. */
    public int gridDivisions() {
        return cellsAt(gridLevel(GridUpgrade.DIVISION));
    }

    /** Le nombre de cases par côté au niveau de réduction {@code level}. */
    public static int cellsAt(int level) {
        return (int) Math.max(GRID_MIN_CELLS, Math.round(GRID_CELLS * Math.pow(GRID_SHRINK, level)));
    }

    /** Les essais par point caché : 1, et un de plus par niveau. */
    public int gridAttempts() {
        return 1 + gridLevel(GridUpgrade.ATTEMPTS);
    }

    /** Le numéro du point {@code index} : il change quand le point se cache ailleurs. */
    public long gridPointId(int index) {
        ensureGridPoints();
        return index >= 0 && index < gridIds.size() ? gridIds.get(index) : 0;
    }

    /** Vrai si le point de ce numéro est encore caché là (ni trouvé, ni parti après ses essais). */
    public boolean isGridPointAlive(long id) {
        ensureGridPoints();
        return gridIds.contains(id);
    }

    /** Les essais qu'il reste sur le point {@code index}. */
    public int gridTriesLeft(int index) {
        ensureGridPoints();
        return index >= 0 && index < gridTries.size() ? gridTries.get(index) : 0;
    }

    /** Le côté d'une case. */
    public double gridCellSize() {
        return GRID_SIZE / gridDivisions();
    }

    /** Les points cachés à la fois. */
    public int gridPointCount() {
        return 1 + gridLevel(GridUpgrade.POINTS);
    }

    /** Ce que donne un essai, avant la proximité : 1, plus 1 par niveau de gain, et le bonus des omégas. */
    public double gridValue() {
        return (1 + gridLevel(GridUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    public double gridProximityStep() {
        return GRID_PROXIMITY + GRID_PROXIMITY_STEP * gridLevel(GridUpgrade.PROXIMITY);
    }

    /**
     * Le multiplicateur de proximité à {@code distance} cases du point : de 1 au plus loin (la diagonale de la grille) à
     * 1 + 4 (et plus) sur le point. Il se compte en part de la grille : la réduire ne l'abaisse pas.
     */
    public double gridProximityFactor(double distance) {
        double p = Math.max(0, 1 - distance / (gridDivisions() * Math.sqrt(3)));
        return 1 + gridProximityStep() * p * p * p;
    }

    /** Ce que la grille montre : 0 rien, 1 la distance, 2 la couche, 3 la colonne aussi. */
    public int gridReveal() {
        return gridLevel(GridUpgrade.REVEAL);
    }

    /** Les points cachés : l'interface n'en montre que ce que {@link #gridReveal()} permet. */
    public List<GridPoint> gridPoints() {
        ensureGridPoints();
        return Collections.unmodifiableList(gridPoints);
    }

    /** Le centre de la case (i, j, k), sur un axe. */
    public double gridCenter(int index) {
        return (index + 0.5) * gridCellSize();
    }

    /** La case d'une coordonnée, sur un axe. */
    public int gridCell(double coordinate) {
        return Math.max(0, Math.min(gridDivisions() - 1, (int) Math.floor(coordinate / gridCellSize())));
    }

    /** La distance, en cases, du centre de la case (i, j, k) au point. */
    public double gridDistance(GridPoint point, int i, int j, int k) {
        return point.distance(gridCenter(i), gridCenter(j), gridCenter(k)) / gridCellSize();
    }

    /** Le point le plus proche du centre de la case (i, j, k) : c'est lui que vise un essai. */
    public GridPoint gridNearest(int i, int j, int k) {
        ensureGridPoints();
        GridPoint best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (GridPoint point : gridPoints) {
            double d = point.distance(gridCenter(i), gridCenter(j), gridCenter(k));
            if (d < distance) {
                distance = d;
                best = point;
            }
        }
        return best;
    }

    /** Le dernier essai, ou {@code null}. */
    public GridProbe gridLastProbe() {
        return gridLast;
    }

    /** Ce qu'il reste à attendre avant le prochain essai. */
    public double gridWait() {
        return Math.max(0, gridWait);
    }

    public long gridProbes() {
        return gridProbes;
    }

    public long gridFound() {
        return gridFound;
    }

    public long gridAutoProbes() {
        return gridAutoProbes;
    }

    public boolean canProbeGrid() {
        return isGridOpen() && gridWait <= 0;
    }

    /**
     * Un essai sur la case (i, j, k) : il vise le point le plus proche et gagne selon la distance ; tombé sur la case du
     * point, il le trouve (gain ×{@value #GRID_FOUND}). Le point se cache ailleurs une fois trouvé, ou ses essais
     * épuisés. Rend l'essai ; {@code null} pendant l'attente ou la dimension fermée.
     */
    public GridProbe probeGrid(int i, int j, int k) {
        if (!canProbeGrid()) return null;
        int n = gridDivisions();
        i = Math.max(0, Math.min(n - 1, i));
        j = Math.max(0, Math.min(n - 1, j));
        k = Math.max(0, Math.min(n - 1, k));
        GridPoint nearest = gridNearest(i, j, k);
        int index = gridPoints.indexOf(nearest);
        double distance = gridDistance(nearest, i, j, k);
        boolean found = gridCell(nearest.x()) == i && gridCell(nearest.y()) == j && gridCell(nearest.z()) == k;
        double gain = gridValue() * gridProximityFactor(distance) * (found ? GRID_FOUND : 1);
        state.setGridShards(state.gridShards() + gain);
        state.setGridShardsEarned(state.gridShardsEarned() + gain);
        int left = found ? 0 : gridTries.get(index) - 1;
        long id = gridIds.get(index);
        if (left <= 0) {
            gridPoints.set(index, randomGridPoint());
            gridTries.set(index, gridAttempts());
            gridIds.set(index, gridNextId++);
        } else {
            gridTries.set(index, left);
        }
        if (found) gridFound++;
        gridProbes++;
        gridWait = GRID_COOLDOWN;
        gridLast = new GridProbe(i, j, k, distance, found, gain, Math.max(0, left), id);
        return gridLast;
    }

    /** Pose les points cachés : pour les essais ; remplace ceux qu'il y avait. */
    public void testSetGridPoints(List<GridPoint> points) {
        gridPoints.clear();
        gridPoints.addAll(points);
        gridTries.clear();
        gridIds.clear();
        for (int n = 0; n < points.size(); n++) {
            gridTries.add(gridAttempts());
            gridIds.add(gridNextId++);
        }
        gridWait = 0;
    }

    /**
     * La grille avance de {@code dt} secondes : l'attente s'écoule, il y a autant de points que le niveau le veut, et
     * l'automatisme essaie toutes les {@value #GRID_AUTO_SECONDS} secondes une case près d'un point : à au plus un
     * huitième de la grille sur chaque axe. Sur une grande grille, il ne trouve presque jamais ; réduite, il trouve
     * souvent.
     */
    void runGrid(double dt) {
        if (!isGridOpen() || dt <= 0) return;
        gridWait -= dt;
        ensureGridPoints();
        if (!isGridAutomatic()) return;
        gridAutoTimer += dt;
        for (int turn = 0; gridAutoTimer >= GRID_AUTO_SECONDS && turn < 100_000; turn++) {
            gridAutoTimer -= GRID_AUTO_SECONDS;
            GridPoint target = gridPoints.get(gridRandom.nextInt(gridPoints.size()));
            int n = gridDivisions(), spread = Math.max(0, n / 8);
            int i = near(gridCell(target.x()), spread, n), j = near(gridCell(target.y()), spread, n), k = near(gridCell(target.z()), spread, n);
            gridWait = 0;
            if (probeGrid(i, j, k) != null) gridAutoProbes++;
        }
    }

    /** Une case à au plus {@code spread} de {@code cell}, dans la grille. */
    private int near(int cell, int spread, int n) {
        return Math.max(0, Math.min(n - 1, cell + (spread == 0 ? 0 : gridRandom.nextInt(2 * spread + 1) - spread)));
    }

    private void ensureGridPoints() {
        while (gridPoints.size() < gridPointCount()) {
            gridPoints.add(randomGridPoint());
            gridTries.add(gridAttempts());
            gridIds.add(gridNextId++);
        }
        while (gridPoints.size() > gridPointCount()) {
            gridPoints.remove(gridPoints.size() - 1);
            gridTries.remove(gridTries.size() - 1);
            gridIds.remove(gridIds.size() - 1);
        }
        for (int n = 0; n < gridTries.size(); n++) gridTries.set(n, Math.min(gridTries.get(n), gridAttempts()));
    }

    private GridPoint randomGridPoint() {
        return new GridPoint(gridRandom.nextDouble() * GRID_SIZE, gridRandom.nextDouble() * GRID_SIZE, gridRandom.nextDouble() * GRID_SIZE);
    }

    /** Vrai quand l'automatisme de la grille joue (automatisation des dimensions, version 0.156). */
    boolean isGridAutomatic() {
        return isDimensionAutomationRunning(idle.core.universe.DimensionAutomation.GRID_PROBE);
    }

    /** Prix du prochain niveau, en repères ; infini une fois au plus haut. */
    public double gridCost(GridUpgrade upgrade) {
        return upgrade.cost(gridLevel(upgrade));
    }

    public boolean canBuyGrid(GridUpgrade upgrade) {
        return isGridOpen() && gridLevel(upgrade) < upgrade.max() && state.gridShards() >= gridCost(upgrade) - 1e-9;
    }

    public boolean buyGrid(GridUpgrade upgrade) {
        if (!canBuyGrid(upgrade)) return false;
        state.setGridShards(Math.max(0, state.gridShards() - gridCost(upgrade)));
        state.setGridLevel(upgrade.ordinal(), gridLevel(upgrade) + 1);
        return true;
    }

    @Override
    boolean buyGridUpgrade(int index) {
        return buyGrid(GridUpgrade.values()[index]);
    }

    // ------------------------------------------------------------------
    // La graine profonde
    // ------------------------------------------------------------------

    /** L'énergie du vide récupérée : le cumul des univers passés, et ce qu'il reste dans celui-ci. */
    public idle.core.math.BigNum recoveredVoidEnergy() {
        idle.core.math.BigNum total = idle.core.math.BigNum.of(Math.max(0, voidAccumulated()));
        return state.voidOpen() ? total.add(voidEnergy()) : total;
    }

    /**
     * Le multiplicateur entier de la graine profonde : la racine carrée de la puissance de dix de l'énergie du vide
     * récupérée, √(log10 E) ; 1 au moins.
     */
    public double deepSeedFull() {
        idle.core.math.BigNum energy = recoveredVoidEnergy();
        double power = energy.compareTo(idle.core.math.BigNum.ONE) > 0 ? energy.log10() : 0;
        return Math.max(1, Math.sqrt(power));
    }

    /** Le multiplicateur de la graine profonde : un dixième de l'effet entier par niveau, tout au niveau 10. */
    public double deepSeed() {
        int level = seedUpgradeLevel(SeedUpgrade.DEEP_SEED);
        return 1 + (deepSeedFull() - 1) * Math.min(1, level / (double) SeedUpgrade.DEEP_SEED.max());
    }

    /** Tout l'arrosage, multiplié par la graine profonde. */
    @Override
    public double wateringBoost() {
        return super.wateringBoost() * deepSeed();
    }
}
