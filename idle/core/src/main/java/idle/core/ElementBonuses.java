package idle.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Le cumul des effets de tous les éléments possédés, calculé une fois puis réutilisé tant que
 * la collection ne change pas. {@link Game} y lit les bonus au lieu de reparcourir le tableau
 * périodique à chaque instant.
 *
 * <p>La force d'un élément est la racine carrée de son nombre d'exemplaires : 1 pour le
 * premier, 2 pour quatre, 3 pour neuf. Un nouvel élément rapporte donc toujours plus qu'un doublon.
 */
final class ElementBonuses {

    /** Une synergie possédée : la grandeur qu'elle améliore et le bonus par unité de sa source. */
    record SynergyTerm(ElementEffect.SynergySource source, ElementEffect.Stat stat, double perUnit) {}

    /** Bonus additifs par grandeur : la grandeur est multipliée par {@code 1 + somme}. */
    private final EnumMap<ElementEffect.Stat, Double> added = new EnumMap<>(ElementEffect.Stat.class);
    /** Multiplicateurs par grandeur : ils se multiplient entre eux. */
    private final EnumMap<ElementEffect.Stat, Double> multiplied = new EnumMap<>(ElementEffect.Stat.class);
    private final Map<Integer, Double> generatorParticles = new java.util.HashMap<>();
    private final Map<Integer, Double> generatorSpeed = new java.util.HashMap<>();
    private final EnumMap<ElementEffect.CostTarget, Double> costs = new EnumMap<>(ElementEffect.CostTarget.class);
    private final EnumMap<ElementEffect.AutomationTarget, Double> automations =
            new EnumMap<>(ElementEffect.AutomationTarget.class);
    private final EnumMap<ElementEffect.UpgradeTarget, Double> upgrades =
            new EnumMap<>(ElementEffect.UpgradeTarget.class);
    private final List<SynergyTerm> synergies = new ArrayList<>();
    private double doubleDraw = 0;
    private double luck = 0;

    /** Cumule les effets des éléments possédés (numéro atomique → exemplaires). */
    static ElementBonuses of(Map<Integer, Integer> owned) {
        ElementBonuses bonuses = new ElementBonuses();
        for (Map.Entry<Integer, Integer> entry : owned.entrySet()) {
            if (entry.getValue() <= 0) continue;
            bonuses.apply(PeriodicTable.element(entry.getKey()).effect(), Math.sqrt(entry.getValue()));
        }
        return bonuses;
    }

    private void apply(ElementEffect effect, double strength) {
        switch (effect) {
            case ElementEffect.GeneratorBoost boost -> {
                Map<Integer, Double> target = boost.stat() == ElementEffect.Stat.SPEED
                        ? generatorSpeed : generatorParticles;
                target.merge(boost.generator(), Math.pow(boost.factor(), strength), (a, b) -> a * b);
            }
            case ElementEffect.Add add -> added.merge(add.stat(), add.bonus() * strength, Double::sum);
            case ElementEffect.Multiply multiply ->
                    multiplied.merge(multiply.stat(), Math.pow(multiply.factor(), strength), (a, b) -> a * b);
            case ElementEffect.MultiplyProduction production -> {
                multiplied.merge(ElementEffect.Stat.PARTICLES, Math.pow(production.factor(), strength), (a, b) -> a * b);
                multiplied.merge(ElementEffect.Stat.SPEED, Math.pow(production.factor(), strength), (a, b) -> a * b);
            }
            case ElementEffect.MultiplyEverything everything -> {
                for (ElementEffect.Stat stat : ElementEffect.Stat.values()) {
                    multiplied.merge(stat, Math.pow(everything.factor(), strength), (a, b) -> a * b);
                }
            }
            case ElementEffect.CostReduction reduction ->
                    costs.merge(reduction.target(), reduction.bonus() * strength, Double::sum);
            case ElementEffect.AutomationSpeed speed ->
                    automations.merge(speed.target(), speed.bonus() * strength, Double::sum);
            case ElementEffect.UpgradeBoost boost ->
                    upgrades.merge(boost.target(), boost.amount() * strength, Double::sum);
            case ElementEffect.Synergy synergy ->
                    synergies.add(new SynergyTerm(synergy.source(), synergy.stat(), synergy.perUnit() * strength));
            case ElementEffect.DoubleDraw draw -> doubleDraw += draw.chance() * strength;
            case ElementEffect.Luck more -> luck += more.bonus() * strength;
        }
    }

    /** Multiplicateur fixe d'une grandeur, hors synergies : {@code (1 + additifs) × multiplicatifs}. */
    double stat(ElementEffect.Stat stat) {
        return (1 + added.getOrDefault(stat, 0.0)) * multiplied.getOrDefault(stat, 1.0);
    }

    /** Multiplicateur des particules d'un générateur (numéroté à partir de 1). */
    double generatorParticles(int generator) {
        return generatorParticles.getOrDefault(generator, 1.0);
    }

    /** Multiplicateur de la vitesse d'un générateur (numéroté à partir de 1). */
    double generatorSpeed(int generator) {
        return generatorSpeed.getOrDefault(generator, 1.0);
    }

    /** Diviseur d'un prix : 1 sans bonus, 2 quand le prix est divisé par deux. */
    double costDivisor(ElementEffect.CostTarget target) {
        return 1 + costs.getOrDefault(target, 0.0);
    }

    /** Diviseur du délai d'un automatisme, bonus « tous les automatismes » compris. */
    double automationDivisor(ElementEffect.AutomationTarget target) {
        return 1 + automations.getOrDefault(target, 0.0)
                + automations.getOrDefault(ElementEffect.AutomationTarget.ALL, 0.0);
    }

    /** Ce que les éléments ajoutent à une amélioration (0 sans élément). */
    double upgradeBoost(ElementEffect.UpgradeTarget target) {
        return upgrades.getOrDefault(target, 0.0);
    }

    List<SynergyTerm> synergies() {
        return synergies;
    }

    /** Chance qu'une synthèse donne un second élément, entre 0 et 1. */
    double doubleDrawChance() {
        return Math.min(1, doubleDraw);
    }

    /** Multiplicateur de la chance des familles rares. */
    double luckMultiplier() {
        return 1 + luck;
    }
}
