package idle.core;

import idle.core.GameUniverse.TreeBubble;
import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * L'automatisation des dimensions (version 0.150), que les paliers de la graine et les univers ouvrent dimension par
 * dimension (version 0.182 ; la bulle du sommet de l'arbre-monde avant) : des automatismes
 * qui jouent les dimensions à la place du joueur et achètent leurs améliorations ({@link DimensionAutomation}). Ils
 * s'achètent l'un après l'autre dans chaque dimension, ceux de la ligne en graines d'univers, ceux de chaque dimension
 * suivante avec la ressource de la précédente. Achetés, ils traversent les univers, comme les dimensions ; chacun se
 * coupe à part, et « Tout couper » les arrête tous.
 */
public abstract class GameDimensionAutomation extends GameHadrons {

    GameDimensionAutomation(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /**
     * Version 0.182 : les automatismes de la dimension n s'ouvrent quand la graine a atteint le palier n + 2 (le palier
     * {@link GameUniverse#SEED_STAGES} au plus) et que n + 1 univers sont formés : ceux de la ligne au bonsaï et au deuxième univers,
     * ceux du plan au cinquième palier et au troisième univers, et ainsi de suite jusqu'au mélange, à l'arbre-monde et au
     * onzième univers. Avant, la bulle du sommet de l'arbre-monde les ouvrait tous d'un coup ; elle fait maintenant
     * moitié prix ({@link #SUMMIT_DISCOUNT}).
     */
    public static final int AUTOMATION_STAGE_AHEAD = 2;

    /** Ce que la bulle du sommet de l'arbre-monde retire du prix des automatismes des dimensions (version 0.182). */
    public static final double SUMMIT_DISCOUNT = 0.5;

    /** Le palier de la graine qu'il faut pour les automatismes de cette dimension : n + 2, l'arbre-monde au plus. */
    public static int dimensionAutomationStage(int dimension) {
        return Math.min(dimension + AUTOMATION_STAGE_AHEAD, GameUniverse.SEED_STAGES);
    }

    /** Les univers formés qu'il faut pour les automatismes de cette dimension : n + 1. */
    public static int dimensionAutomationUniverses(int dimension) {
        return dimension + 1;
    }

    /** Vrai si les automatismes de cette dimension sont ouverts : le palier de la graine et les univers y sont. */
    public boolean isDimensionAutomationOpen(int dimension) {
        return dimension >= 1 && dimension <= GameUniverse.MAX_DIMENSIONS && seedStage() >= dimensionAutomationStage(dimension)
                && universes() >= dimensionAutomationUniverses(dimension);
    }

    /** Vrai si les automatismes d'au moins une dimension sont ouverts : le sous-onglet existe. */
    public boolean isDimensionAutomationUnlocked() {
        return isDimensionAutomationOpen(1);
    }

    public boolean ownsDimensionAutomation(DimensionAutomation automation) {
        return state.dimensionAutomations().contains(automation.name());
    }

    /** Vrai si on peut l'acheter, ou l'a acheté : sa dimension ouverte aux automatismes, et celui d'avant acheté. */
    public boolean isDimensionAutomationAvailable(DimensionAutomation automation) {
        DimensionAutomation previous = automation.previous();
        return isDimensionAutomationOpen(automation.dimension()) && dimensionsUnlocked() >= automation.dimension()
                && (previous == null || ownsDimensionAutomation(previous));
    }

    /** Son prix : celui du catalogue, moitié prix avec la bulle du sommet de l'arbre-monde (version 0.182). */
    public double dimensionAutomationCost(DimensionAutomation automation) {
        return automation.cost() * (hasBubble(TreeBubble.SUMMIT) ? SUMMIT_DISCOUNT : 1);
    }

    /** Ce que le joueur a pour le payer : ses graines d'univers, ou la ressource de la dimension d'avant. */
    public double dimensionAutomationBalance(DimensionAutomation automation) {
        return automation.paidWith() == 0 ? state.universeSeeds().toDouble() : dimensionResource(automation.paidWith());
    }

    /** Le nom de ce qui le paie : « graines d'univers », « segments »… */
    public static String dimensionAutomationCurrency(DimensionAutomation automation) {
        return automation.paidWith() == 0 ? "graines d'univers" : dimensionResourceName(automation.paidWith());
    }

    public boolean canBuyDimensionAutomation(DimensionAutomation automation) {
        return isDimensionAutomationAvailable(automation) && !ownsDimensionAutomation(automation)
                && dimensionAutomationBalance(automation) >= dimensionAutomationCost(automation) - 1e-9;
    }

    /** Achète l'automatisme ; il se met en marche. Vrai si c'est fait. */
    public boolean buyDimensionAutomation(DimensionAutomation automation) {
        if (!canBuyDimensionAutomation(automation)) return false;
        double cost = dimensionAutomationCost(automation);
        switch (automation.paidWith()) {
            case 0 -> state.setUniverseSeeds(state.universeSeeds().subtract(BigNum.of(cost)).max(BigNum.ZERO));
            // Les autres, avec la ressource de la dimension d'avant (version 0.155 : un seul endroit, pour n'en oublier aucune).
            default -> spendDimensionResource(automation.paidWith(), cost);
        }
        state.addDimensionAutomation(automation.name());
        return true;
    }

    /** Vrai s'il est acheté et en marche (pas coupé à part, pas coupé avec les autres). */
    public boolean isDimensionAutomationEnabled(DimensionAutomation automation) {
        return ownsDimensionAutomation(automation) && !state.dimensionAutomationsOff().contains(automation.name());
    }

    /** Vrai s'il agit en ce moment : en marche, « Tout couper » relâché. */
    public boolean isDimensionAutomationRunning(DimensionAutomation automation) {
        return isDimensionAutomationEnabled(automation) && !state.automationPaused() && state.started()
                && !(state.testBlocking() && state.testBlocked("dimension:" + automation.dimension()));
    }

    /** Met en marche ou coupe un automatisme acheté ; faux s'il ne l'est pas. */
    public boolean setDimensionAutomationEnabled(DimensionAutomation automation, boolean enabled) {
        if (!ownsDimensionAutomation(automation)) return false;
        state.setDimensionAutomationOff(automation.name(), !enabled);
        return true;
    }

    /** Les automatismes achetés, toutes dimensions. */
    public int dimensionAutomationsOwned() {
        return state.dimensionAutomations().size();
    }

    /** Ceux qui achètent : chacun prend son amélioration tant qu'elle est payable (cent fois par pas au plus). */
    void runDimensionAutomation() {
        if (state.dimensionAutomations().isEmpty()) return;
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.kind() != DimensionAutomation.Kind.UPGRADE || !isDimensionAutomationRunning(automation)) continue;
            for (int bought = 0; bought < MAX_AUTO_ACTIONS_PER_STEP && buyDimensionUpgrade(automation); bought++) { }
        }
    }

    private boolean buyDimensionUpgrade(DimensionAutomation automation) {
        int index = automation.upgrade();
        return switch (automation.dimension()) {
            case 1 -> buyLine(LineUpgrade.values()[index]);
            case 2 -> buyPlane(PlaneUpgrade.values()[index]);
            case 3 -> buyVolume(VolumeUpgrade.values()[index]);
            case 4 -> buyDuration(DurationUpgrade.values()[index]);
            case 5 -> buyLoopUpgrade(index);
            case 6 -> buyBraneUpgrade(index);
            case 7 -> buyClickerUpgrade(index);
            case 8 -> buyGridUpgrade(index);
            case 9 -> buyMirrorUpgrade(index);
            case 10 -> buyMixUpgrade(index);
            default -> false;
        };
    }

    /** Achète l'amélioration de la boucle de ce rang (version 0.153, la boucle est un étage plus haut). */
    abstract boolean buyLoopUpgrade(int index);

    /** Achète l'amélioration de la brane de ce rang (version 0.154). */
    abstract boolean buyBraneUpgrade(int index);

    /** Achète l'amélioration du clicker de ce rang (version 0.155). */
    abstract boolean buyClickerUpgrade(int index);

    /** Achète l'amélioration de la grille de ce rang (version 0.156). */
    abstract boolean buyGridUpgrade(int index);

    /** Achète l'amélioration du miroir de ce rang (version 0.157). */
    abstract boolean buyMirrorUpgrade(int index);

    /** Achète l'amélioration du mélange de ce rang (version 0.158). */
    abstract boolean buyMixUpgrade(int index);

    /** La visée du plan : le point file vers l'éclat le plus proche quand le joueur ne tient aucune direction. */
    @Override
    double[] planeAutoDirection() {
        if (!isDimensionAutomationRunning(DimensionAutomation.PLANE_AIM)) return null;
        PlaneShard nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (PlaneShard shard : planeShardsOnBoard()) {
            double distance = Math.hypot(shard.x() - planeX(), shard.y() - planeY());
            if (distance < best) {
                best = distance;
                nearest = shard;
            }
        }
        if (nearest == null || best < 1e-9) return null;
        return new double[] {(nearest.x() - planeX()) / best, (nearest.y() - planeY()) / best};
    }

    @Override
    boolean isVolumeAutomatic() {
        return isDimensionAutomationRunning(DimensionAutomation.VOLUME_DIVE);
    }

    @Override
    boolean isDurationAutomatic() {
        return isDimensionAutomationRunning(DimensionAutomation.DURATION_HOLD);
    }
}
