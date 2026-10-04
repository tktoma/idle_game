package idle.core;

/**
 * Ce que fait un élément du tableau périodique quand le joueur le possède.
 *
 * <p>Chaque élément a son propre effet. La force d'un effet dépend du nombre d'exemplaires
 * possédés : le premier compte pour 1, puis les doublons de moins en moins (racine carrée
 * du nombre d'exemplaires). Les éléments uniques ne s'obtiennent qu'une fois : leur force vaut toujours 1.
 *
 * <p>Interface scellée : pour ajouter un type d'effet, on ajoute un record ici, puis le
 * compilateur signale les {@code switch} à compléter.
 */
public sealed interface ElementEffect {

    /** Les trois grandeurs de production du jeu. */
    enum Stat {
        /** Particules obtenues à chaque création. */
        PARTICLES,
        /** Vitesse de création des générateurs. */
        SPEED,
        /** Atomes gagnés à chaque fusion. */
        ATOMS
    }

    /** Ce dont un élément peut réduire le prix. */
    enum CostTarget { SPEED_UPGRADES, ATOM_UPGRADES, SYNTHESIS }

    /** Les automatismes qu'un élément peut accélérer. */
    enum AutomationTarget { SPEED_UPGRADES, GENERATORS, FUSION, SYNTHESIS, ALL }

    /** Les améliorations qu'un élément peut renforcer. */
    enum UpgradeTarget { DOUBLING, MASS, PATIENCE, CATALYST, YIELD, SPEED }

    /** Ce sur quoi une synergie se compte. */
    enum SynergySource {
        /** Nombre d'éléments différents découverts. */
        DISTINCT_ELEMENTS,
        /** Niveaux achetés des améliorations de vitesse. */
        SPEED_LEVELS,
        /** Générateurs actifs dans la partie en cours. */
        GENERATORS,
        /** Atomes disponibles, non dépensés. */
        AVAILABLE_ATOMS,
        /** Automatismes achetés. */
        AUTOMATIONS
    }

    /**
     * Améliore un seul générateur : ses particules ou sa vitesse sont multipliées par {@code factor}.
     * Les éléments d'un même générateur se multiplient entre eux.
     */
    record GeneratorBoost(int generator, Stat stat, double factor) implements ElementEffect {}

    /** Ajoute {@code bonus} à une grandeur ; les éléments de ce type s'additionnent entre eux. */
    record Add(Stat stat, double bonus) implements ElementEffect {}

    /** Multiplie une grandeur par {@code factor} ; les éléments de ce type se multiplient entre eux. */
    record Multiply(Stat stat, double factor) implements ElementEffect {}

    /** Multiplie à la fois les particules et la vitesse par {@code factor} : la production gagne {@code factor²}. */
    record MultiplyProduction(double factor) implements ElementEffect {}

    /** Multiplie les trois grandeurs à la fois par {@code factor}. */
    record MultiplyEverything(double factor) implements ElementEffect {}

    /** Réduit un prix : il est divisé par {@code 1 + somme des bonus} (1.0 = prix divisé par deux). */
    record CostReduction(CostTarget target, double bonus) implements ElementEffect {}

    /** Accélère un automatisme : son délai est divisé par {@code 1 + somme des bonus}. */
    record AutomationSpeed(AutomationTarget target, double bonus) implements ElementEffect {}

    /** Renforce une amélioration existante de {@code amount} (le sens dépend de l'amélioration). */
    record UpgradeBoost(UpgradeTarget target, double amount) implements ElementEffect {}

    /** Bonus qui grandit avec le jeu : {@code +perUnit} sur une grandeur pour chaque unité de la source. */
    record Synergy(SynergySource source, Stat stat, double perUnit) implements ElementEffect {}

    /** Chance qu'une synthèse donne un second élément (0.1 = 10 %). */
    record DoubleDraw(double chance) implements ElementEffect {}

    /** Rend les familles rares plus probables : leur chance est multipliée par {@code 1 + somme des bonus}. */
    record Luck(double bonus) implements ElementEffect {}
}
