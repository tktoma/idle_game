package idle.core;

import idle.core.state.GameState;

import idle.core.GameUniverseBase.TreeBubble;
import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.*;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * L'automatisation des dimensions, que les paliers de la graine et les univers ouvrent dimension par
 * dimension (version 0.182 ; la bulle du sommet de l'arbre-monde avant) : des automatismes
 * qui jouent les dimensions à la place du joueur et achètent leurs améliorations ({@link DimensionAutomation}). Ils
 * s'achètent l'un après l'autre dans chaque dimension, avec la ressource de la dimension (version 0.209 ; avant, ceux de
 * la ligne en graines d'univers, ceux de chaque dimension suivante avec la ressource de la précédente). Achetés, ils
 * traversent les univers, comme les dimensions ; chacun se coupe à part, et « Tout couper » les arrête tous.
 *
 * <p>Ceux qui jouent viennent en dernier, en cinq paliers de cadence ; le plus rapide en marche joue
 * ({@link #dimensionPlayPeriod}). Ceux qui achètent gardent de quoi payer l'automatisme suivant de leur dimension
 * ({@link #dimensionAutomationReserve}).
 */
public abstract class GameDimensionAutomation extends GameHadrons {
    GameDimensionAutomation(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /**
     * Les automatismes de la dimension n s'ouvrent quand la graine a atteint le palier n + 2 (le palier
     * {@link GameUniverse#SEED_STAGES} au plus) et que n + 1 univers sont formés : ceux de la ligne au bonsaï et au deuxième univers,
     * ceux du plan au cinquième palier et au troisième univers, et ainsi de suite jusqu'au mélange, à l'arbre-monde et au
     * onzième univers. Avant, la bulle du sommet de l'arbre-monde les ouvrait tous d'un coup ; elle fait maintenant
     * moitié prix ({@link #SUMMIT_DISCOUNT}).
     */
    public static final int AUTOMATION_STAGE_AHEAD = 2;

    /** Ce que la bulle du sommet de l'arbre-monde retire du prix des automatismes des dimensions. */
    public static final double SUMMIT_DISCOUNT = 0.5;

    /** Le palier de la graine qu'il faut pour les automatismes de cette dimension : n + 2, l'arbre-monde au plus. */
    public static int dimensionAutomationStage(int dimension) {
        return Math.min(dimension + AUTOMATION_STAGE_AHEAD, GameUniverse.SEED_STAGES);
    }

    /** Les univers formés qu'il faut pour les automatismes de cette dimension : n + 1. */
    public static int dimensionAutomationUniverses(int dimension) { return dimension + 1; }

    /** Vrai si les automatismes de cette dimension sont ouverts : le palier de la graine et les univers y sont. */
    public boolean isDimensionAutomationOpen(int dimension) {
        return dimension >= 1 && dimension <= GameUniverse.MAX_DIMENSIONS && seedStage() >= dimensionAutomationStage(dimension)
                && universes() >= dimensionAutomationUniverses(dimension);
    }

    /** Vrai si les automatismes d'au moins une dimension sont ouverts : le sous-onglet existe. */
    public boolean isDimensionAutomationUnlocked() { return isDimensionAutomationOpen(1); }

    public boolean ownsDimensionAutomation(DimensionAutomation automation) {
        return state.dimensionAutomations().contains(automation.name());
    }

    /** Vrai si on peut l'acheter, ou l'a acheté : sa dimension ouverte aux automatismes, et celui d'avant acheté. */
    public boolean isDimensionAutomationAvailable(DimensionAutomation automation) {
        DimensionAutomation previous = automation.previous();
        return isDimensionAutomationOpen(automation.dimension()) && dimensionsUnlocked() >= automation.dimension()
                && (previous == null || ownsDimensionAutomation(previous));
    }

    /** Son prix : celui du catalogue, moitié prix avec la bulle du sommet de l'arbre-monde. */
    public double dimensionAutomationCost(DimensionAutomation automation) {
        return automation.cost() * (hasBubble(TreeBubble.SUMMIT) ? SUMMIT_DISCOUNT : 1);
    }

    /** Ce que le joueur a pour le payer : la ressource de sa dimension. */
    public double dimensionAutomationBalance(DimensionAutomation automation) {
        return dimensionResource(automation.paidWith());
    }

    /** Le nom de ce qui le paie : « segments », « facettes »… */
    public static String dimensionAutomationCurrency(DimensionAutomation automation) {
        return dimensionResourceName(automation.paidWith());
    }

    /** Le prochain automatisme à acheter dans cette dimension : le premier pas encore acheté, ou {@code null}. */
    public DimensionAutomation nextDimensionAutomation(int dimension) {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() == dimension && !ownsDimensionAutomation(automation)) return automation;
        }
        return null;
    }

    /**
     * Ce que ceux qui achètent laissent dans la réserve de leur dimension : le prix de l'automatisme
     * suivant, s'il est ouvert. Sans cela, ils dépenseraient tout dès qu'une amélioration est payable, et les paliers qui
     * jouent, en dernier et les plus chers, ne se paieraient jamais.
     */
    public double dimensionAutomationReserve(int dimension) {
        DimensionAutomation next = nextDimensionAutomation(dimension);
        return next == null || !isDimensionAutomationAvailable(next) ? 0 : dimensionAutomationCost(next);
    }

    public boolean canBuyDimensionAutomation(DimensionAutomation automation) {
        return isDimensionAutomationAvailable(automation) && !ownsDimensionAutomation(automation)
                && dimensionAutomationBalance(automation) >= dimensionAutomationCost(automation) - 1e-9;
    }

    /** Achète l'automatisme ; il se met en marche. Vrai si c'est fait. */
    public boolean buyDimensionAutomation(DimensionAutomation automation) {
        if (!canBuyDimensionAutomation(automation)) return false;
        spendDimensionResource(automation.paidWith(), dimensionAutomationCost(automation));
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

    /**
     * Le palier le plus rapide acheté de celui qui joue cette dimension, en marche ou non ; {@code null} avant le premier
     * (version 0.268 : la carte unique des paliers).
     */
    public DimensionAutomation ownedPlayTier(int dimension) {
        DimensionAutomation owned = null;
        for (DimensionAutomation tier : DimensionAutomation.playTiers(dimension)) if (ownsDimensionAutomation(tier)) owned = tier;
        return owned;
    }

    /** Le prochain palier à acheter de celui qui joue cette dimension ; {@code null} une fois tous achetés, et pour la ligne. */
    public DimensionAutomation nextPlayTier(int dimension) {
        for (DimensionAutomation tier : DimensionAutomation.playTiers(dimension)) if (!ownsDimensionAutomation(tier)) return tier;
        return null;
    }

    /** Vrai si un palier acheté de celui qui joue cette dimension est en marche. */
    public boolean isDimensionPlayEnabled(int dimension) {
        for (DimensionAutomation tier : DimensionAutomation.playTiers(dimension)) if (isDimensionAutomationEnabled(tier)) return true;
        return false;
    }

    /** Met en marche ou coupe ensemble tous les paliers achetés de celui qui joue cette dimension ; faux sans aucun. */
    public boolean setDimensionPlayEnabled(int dimension, boolean enabled) {
        boolean any = false;
        for (DimensionAutomation tier : DimensionAutomation.playTiers(dimension)) any |= setDimensionAutomationEnabled(tier, enabled);
        return any;
    }

    /**
     * Achète le prochain palier de celui qui joue cette dimension ; s'il était en pause, le nouveau palier l'est aussi
     * (un achat ne relance rien). Vrai si c'est fait.
     */
    public boolean buyNextPlayTier(int dimension) {
        DimensionAutomation next = nextPlayTier(dimension);
        if (next == null) return false;
        boolean paused = ownedPlayTier(dimension) != null && !isDimensionPlayEnabled(dimension);
        if (!buyDimensionAutomation(next)) return false;
        if (paused) setDimensionAutomationEnabled(next, false);
        return true;
    }

    /** Les automatismes achetés, toutes dimensions. */
    public int dimensionAutomationsOwned() { return state.dimensionAutomations().size(); }

    /**
     * Ceux qui achètent : chacun prend son amélioration tant qu'elle est payable (cent fois par pas au plus), sans entamer
     * ce qu'il faut pour l'automatisme suivant de sa dimension ({@link #dimensionAutomationReserve}).
     */
    void runDimensionAutomation() {
        if (state.dimensionAutomations().isEmpty()) return;
        int reserved = 0;
        double reserve = 0;
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.kind() != DimensionAutomation.Kind.UPGRADE || !isDimensionAutomationRunning(automation)) continue;
            int dimension = automation.dimension();
            if (dimension != reserved) {
                reserved = dimension;
                reserve = dimensionAutomationReserve(dimension);
            }
            for (int bought = 0; bought < MAX_AUTO_ACTIONS_PER_STEP; bought++) {
                double cost = dimensionUpgradeCost(dimension, automation.upgrade());
                if (dimensionResource(dimension) - cost < reserve - 1e-9 || !buyDimensionUpgrade(automation)) break;
            }
        }
    }

    /** Le prix du prochain niveau de l'amélioration de ce rang dans cette dimension ; l'infini au plus haut. */
    abstract double dimensionUpgradeCost(int dimension, int index);

    /** Le prix du prochain niveau de l'amélioration qu'achète cet automatisme ; l'infini pour celui qui joue. */
    public double dimensionUpgradeCost(DimensionAutomation automation) {
        return automation.kind() == DimensionAutomation.Kind.UPGRADE ? dimensionUpgradeCost(automation.dimension(), automation.upgrade())
                : Double.POSITIVE_INFINITY;
    }

    /**
     * Achète un niveau de l'amélioration qu'achète cet automatisme, comme le ferait le joueur d'un clic sur son bouton
     * (version 0.209 : pour les bots, qui jouent les dimensions à la main). Vrai si c'est fait.
     */
    public boolean buyDimensionUpgradeByHand(DimensionAutomation automation) {
        return automation.kind() == DimensionAutomation.Kind.UPGRADE && buyDimensionUpgrade(automation);
    }

    /**
     * Le temps entre deux actions de celui qui joue cette dimension : celui du palier le plus rapide qui
     * tourne, ou celui du joueur qu'un bot imite ({@link #playDimensionByHand}) ; l'infini si personne ne la joue. Aucune
     * bulle ni la matière noire n'y touchent.
     */
    public double dimensionPlayPeriod(int dimension) {
        double period = handDimension == dimension ? handPeriod : Double.POSITIVE_INFINITY;
        if (state.dimensionAutomations().isEmpty()) return period;
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() != dimension || automation.kind() != DimensionAutomation.Kind.PLAY) continue;
            if (automation.playPeriod() < period && isDimensionAutomationRunning(automation)) period = automation.playPeriod();
        }
        return period;
    }

    /** Vrai si quelqu'un joue cette dimension : un palier qui tourne, ou la main d'un bot. */
    public boolean isDimensionPlayed(int dimension) {
        return dimensionPlayPeriod(dimension) < Double.POSITIVE_INFINITY;
    }

    /** Le palier le plus rapide qui joue cette dimension et tourne, ou {@code null} : pour les textes. */
    public DimensionAutomation dimensionPlayTier(int dimension) {
        DimensionAutomation fastest = null;
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() != dimension || automation.kind() != DimensionAutomation.Kind.PLAY
                    || !isDimensionAutomationRunning(automation)) continue;
            if (fastest == null || automation.playPeriod() < fastest.playPeriod()) fastest = automation;
        }
        return fastest;
    }

    /** Vrai si un palier de celui qui joue cette dimension tourne : pour les textes de la fenêtre. */
    public boolean isDimensionAutoPlayed(int dimension) { return dimensionPlayTier(dimension) != null; }

    /** La dimension que joue la main d'un bot, 0 pour aucune, et son temps entre deux actions : jamais sauvegardés. */
    private int handDimension;
    private double handPeriod = Double.POSITIVE_INFINITY;

    /**
     * Un bot joue cette dimension à la main, une action toutes les {@code period} secondes, avec ce que
     * ferait l'automatisme qui la joue ; 0 ou une période infinie arrête. Une seule à la fois, comme un joueur. Pour les
     * bots seulement : l'interface n'y passe jamais, le joueur joue avec la souris, et rien n'en est sauvegardé.
     */
    public void playDimensionByHand(int dimension, double period) {
        boolean stop = dimension <= 0 || !(period > 0) || Double.isInfinite(period);
        handDimension = stop ? 0 : dimension;
        handPeriod = stop ? Double.POSITIVE_INFINITY : Math.max(DimensionAutomation.INSTANT_PERIOD, period);
    }

    /** La dimension que joue la main d'un bot, 0 pour aucune. */
    public int dimensionPlayedByHand() { return handDimension; }

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

    /** Achète l'amélioration de la boucle de ce rang (la boucle est un étage plus haut). */
    abstract boolean buyLoopUpgrade(int index);

    /** Achète l'amélioration de la brane de ce rang. */
    abstract boolean buyBraneUpgrade(int index);

    /** Achète l'amélioration du clicker de ce rang. */
    abstract boolean buyClickerUpgrade(int index);

    /** Achète l'amélioration de la grille de ce rang. */
    abstract boolean buyGridUpgrade(int index);

    /** Achète l'amélioration du miroir de ce rang. */
    abstract boolean buyMirrorUpgrade(int index);

    /** Achète l'amélioration du mélange de ce rang. */
    abstract boolean buyMixUpgrade(int index);

    @Override
    boolean isPlaneAutomatic() { return isDimensionPlayed(2); }

    /** Ce qui reste avant que la visée reprenne sa direction, et la direction tenue entre deux. */
    private double planeAimWait;
    private double[] planeAim;

    /**
     * La visée du plan : le point file vers l'éclat le plus proche quand le joueur ne tient aucune direction. Version
     * 0.209 : elle reprend sa direction une fois par action ({@link #dimensionPlayPeriod}) et la tient entre deux.
     */
    @Override
    double[] planeAutoDirection(double dt) {
        double period = dimensionPlayPeriod(2);
        if (period == Double.POSITIVE_INFINITY) {
            planeAim = null;
            return null;
        }
        planeAimWait -= dt;
        if (planeAimWait > 1e-9) return planeAim;
        planeAimWait = period;
        planeAim = nearestPlaneShardDirection();
        return planeAim;
    }

    private double[] nearestPlaneShardDirection() {
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
    boolean isVolumeAutomatic() { return isDimensionPlayed(3); }

    @Override
    double volumeShotPeriod() { return dimensionPlayPeriod(3); }

    @Override
    boolean isDurationAutomatic() { return isDimensionPlayed(4); }

    @Override
    double durationPressDelay() { return dimensionPlayPeriod(4); }
}
