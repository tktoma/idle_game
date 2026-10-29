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
 * La grille, huitième dimension (version 0.156) : un cube de {@value #GRID_SIZE} sur {@value #GRID_SIZE} sur
 * {@value #GRID_SIZE}, où des points sont cachés au hasard. Le joueur sonde une case de la grille (le cube est divisé en
 * d cases par côté) : plus le centre de la case sondée est proche d'un point, plus il gagne de repères (la ressource).
 * Une case qui contient un point le trouve : le gain est multiplié, et le point se cache ailleurs. Une courte attente
 * suit chaque sondage, pour que balayer toutes les cases ne paie pas.
 *
 * <p>Améliorations ({@link GridUpgrade}) : division de la grille (plus de cases, des sondages plus précis), gain,
 * affichage du point (d'abord la distance, puis sa couche, puis sa colonne), nombre de points, multiplicateur de
 * proximité.
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
        /** Une case de plus par côté : 2 au départ, 24 au plus. */
        DIVISION(5, 2.4, 22),
        /** Chaque sondage donne un repère de plus par niveau. */
        GAIN(6, 3, Integer.MAX_VALUE),
        /** Ce que la grille montre : la distance du dernier sondage, puis la couche du point le plus proche, puis sa colonne. */
        REVEAL(40, 6, 3),
        /** Un point caché de plus par niveau : 7 au plus. */
        POINTS(30, 3.5, 6),
        /** La proximité compte davantage : +1 au multiplicateur par niveau. */
        PROXIMITY(10, 2.8, 20);

        private final Price price;

        GridUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Un point caché, en coordonnées de 0 à {@value #GRID_SIZE}. */
    public record GridPoint(double x, double y, double z) {
        public double distance(double px, double py, double pz) {
            return Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
        }
    }

    /** Ce qu'a donné un sondage : la case, la distance au point le plus proche, s'il l'a trouvé, et le gain. */
    public record GridProbe(int i, int j, int k, double distance, boolean found, double gain) {
    }

    /** Le côté du cube. */
    public static final double GRID_SIZE = 1_000;
    /** La plus grande distance dans le cube : sa diagonale. */
    public static final double GRID_DIAGONAL = GRID_SIZE * Math.sqrt(3);
    /** Deux cases par côté au départ. */
    public static final int GRID_DIVISIONS = 2;
    /** Le multiplicateur de proximité : ×(1 + 4 p³) au départ, p allant de 0 (au plus loin) à 1 (sur le point) ; +1 par niveau. */
    public static final double GRID_PROXIMITY = 4, GRID_PROXIMITY_STEP = 1;
    /** Trouver un point (il est dans la case sondée) : ×5. */
    public static final double GRID_FOUND = 5;
    /** L'attente après chaque sondage, en secondes. */
    public static final double GRID_COOLDOWN = 0.5;
    /** L'automatisme sonde toutes les {@value} secondes. */
    public static final double GRID_AUTO_SECONDS = 2;

    private final java.util.Random gridRandom = new java.util.Random(8);
    private final List<GridPoint> gridPoints = new ArrayList<>();
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

    /** Le nombre de cases par côté. */
    public int gridDivisions() {
        return GRID_DIVISIONS + gridLevel(GridUpgrade.DIVISION);
    }

    /** Le côté d'une case. */
    public double gridCellSize() {
        return GRID_SIZE / gridDivisions();
    }

    /** Les points cachés à la fois. */
    public int gridPointCount() {
        return 1 + gridLevel(GridUpgrade.POINTS);
    }

    /** Ce que donne un sondage, avant la proximité : 1, plus 1 par niveau de gain, et le bonus des omégas. */
    public double gridValue() {
        return (1 + gridLevel(GridUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    public double gridProximityStep() {
        return GRID_PROXIMITY + GRID_PROXIMITY_STEP * gridLevel(GridUpgrade.PROXIMITY);
    }

    /** Le multiplicateur de proximité à la distance {@code distance} : de 1 au plus loin à 1 + 4 (et plus) sur le point. */
    public double gridProximityFactor(double distance) {
        double p = Math.max(0, 1 - distance / GRID_DIAGONAL);
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

    /** Le point le plus proche du centre de la case (i, j, k). */
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

    /** Le dernier sondage, ou {@code null}. */
    public GridProbe gridLastProbe() {
        return gridLast;
    }

    /** Ce qu'il reste à attendre avant le prochain sondage. */
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
     * Sonde la case (i, j, k) : gagne selon la distance de son centre au point le plus proche, et trouve le point s'il est
     * dans la case (gain ×{@value #GRID_FOUND}, il se cache ailleurs). Rend le sondage ; {@code null} pendant l'attente ou
     * la dimension fermée.
     */
    public GridProbe probeGrid(int i, int j, int k) {
        if (!canProbeGrid()) return null;
        int n = gridDivisions();
        i = Math.max(0, Math.min(n - 1, i));
        j = Math.max(0, Math.min(n - 1, j));
        k = Math.max(0, Math.min(n - 1, k));
        GridPoint nearest = gridNearest(i, j, k);
        double distance = nearest.distance(gridCenter(i), gridCenter(j), gridCenter(k));
        boolean found = gridCell(nearest.x()) == i && gridCell(nearest.y()) == j && gridCell(nearest.z()) == k;
        double gain = gridValue() * gridProximityFactor(distance) * (found ? GRID_FOUND : 1);
        state.setGridShards(state.gridShards() + gain);
        state.setGridShardsEarned(state.gridShardsEarned() + gain);
        if (found) {
            gridPoints.set(gridPoints.indexOf(nearest), randomGridPoint());
            gridFound++;
        }
        gridProbes++;
        gridWait = GRID_COOLDOWN;
        gridLast = new GridProbe(i, j, k, distance, found, gain);
        return gridLast;
    }

    /** Pose les points cachés : pour les essais ; remplace ceux qu'il y avait. */
    public void testSetGridPoints(List<GridPoint> points) {
        gridPoints.clear();
        gridPoints.addAll(points);
        gridWait = 0;
    }

    /**
     * La grille avance de {@code dt} secondes : l'attente s'écoule, il y a autant de points que le niveau le veut, et
     * l'automatisme sonde toutes les {@value #GRID_AUTO_SECONDS} secondes une case voisine de celle d'un point (il
     * cherche bien, sans trouver à coup sûr : une fois sur quatre, il tombe dans la bonne case).
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
            int i = gridCell(target.x()), j = gridCell(target.y()), k = gridCell(target.z());
            if (gridRandom.nextInt(4) != 0) {
                int axis = gridRandom.nextInt(3), step = gridRandom.nextBoolean() ? 1 : -1, n = gridDivisions();
                if (axis == 0) i = Math.floorMod(i + step, n);
                else if (axis == 1) j = Math.floorMod(j + step, n);
                else k = Math.floorMod(k + step, n);
            }
            gridWait = 0;
            if (probeGrid(i, j, k) != null) gridAutoProbes++;
        }
    }

    private void ensureGridPoints() {
        while (gridPoints.size() < gridPointCount()) gridPoints.add(randomGridPoint());
        while (gridPoints.size() > gridPointCount()) gridPoints.remove(gridPoints.size() - 1);
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
