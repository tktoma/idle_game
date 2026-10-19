package idle.core.elements;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Le cumul des effets de tous les éléments possédés, calculé une fois puis réutilisé tant que
 * la collection ne change pas. {@link Game} y lit les bonus au lieu de reparcourir le tableau
 * périodique à chaque instant.
 *
 * <p>Les ensembles complets ({@link ElementSets}) y ajoutent leur bonus.
 *
 * <p>La force d'un élément est la racine carrée de son nombre d'exemplaires : 1 pour le
 * premier, 2 pour quatre, 3 pour neuf. Un nouvel élément rapporte donc toujours plus qu'un doublon.
 */
public final class ElementBonuses {

    /** Une synergie possédée : la grandeur qu'elle améliore et le bonus par unité de sa source. */
    public record SynergyTerm(ElementEffect.SynergySource source, ElementEffect.Stat stat, double perUnit) {}

    /** Bonus additifs par grandeur : la grandeur est multipliée par {@code 1 + somme}. */
    private final EnumMap<ElementEffect.Stat, Double> added = new EnumMap<>(ElementEffect.Stat.class);
    /** Multiplicateurs par grandeur : ils se multiplient entre eux. */
    private final EnumMap<ElementEffect.Stat, Double> multiplied = new EnumMap<>(ElementEffect.Stat.class);
    // Par générateur (indice = numéro du générateur) : des tableaux plutôt que des tables, lus à chaque
    // instant pour chaque générateur. 1 partout tant qu'aucun élément ne les touche.
    private double[] generatorParticles = new double[0];
    private double[] generatorSpeed = new double[0];
    private final EnumMap<ElementEffect.CostTarget, Double> costs = new EnumMap<>(ElementEffect.CostTarget.class);
    private final EnumMap<ElementEffect.AutomationTarget, Double> automations =
            new EnumMap<>(ElementEffect.AutomationTarget.class);
    private final EnumMap<ElementEffect.UpgradeTarget, Double> upgrades =
            new EnumMap<>(ElementEffect.UpgradeTarget.class);
    private final List<SynergyTerm> synergies = new ArrayList<>();
    private double doubleDraw = 0;
    private double luck = 0;
    private int completedSets = 0;
    private int halfSets = 0;

    /** Cumule les effets des éléments possédés (numéro atomique → exemplaires). */
    public static ElementBonuses of(Map<Integer, Integer> owned) {
        return of(owned, 1);
    }

    /**
     * Comme {@link #of(Map)}, avec une force des éléments multipliée par {@code elementStrength}
     * (1.15 = tous les éléments agissent 15 % plus fort). Les ensembles n'en profitent pas.
     */
    public static ElementBonuses of(Map<Integer, Integer> owned, double elementStrength) {
        ElementBonuses bonuses = new ElementBonuses();
        for (Map.Entry<Integer, Integer> entry : owned.entrySet()) {
            if (entry.getValue() <= 0) continue;
            bonuses.apply(PeriodicTable.element(entry.getKey()).effect(), Math.sqrt(entry.getValue()) * elementStrength);
        }
        // Les ensembles : leur bonus s'ajoute à ceux des éléments, au quart de sa force dès la
        // moitié réunie, à pleine force une fois complets.
        for (ElementSet set : ElementSets.DEFAULT) {
            double strength = set.strength(owned);
            if (strength <= 0) continue;
            bonuses.apply(set.effect(), strength);
            if (strength >= 1) bonuses.completedSets++;
            else bonuses.halfSets++;
        }
        return bonuses;
    }

    private void apply(ElementEffect effect, double strength) {
        switch (effect) {
            case ElementEffect.GeneratorBoost boost -> {
                boolean speed = boost.stat() == ElementEffect.Stat.SPEED;
                double[] target = grown(speed ? generatorSpeed : generatorParticles, boost.generator());
                target[boost.generator()] *= Math.pow(boost.factor(), strength);
                if (speed) generatorSpeed = target;
                else generatorParticles = target;
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
    public double stat(ElementEffect.Stat stat) {
        return (1 + added.getOrDefault(stat, 0.0)) * multiplied.getOrDefault(stat, 1.0);
    }

    /** Multiplicateur des particules d'un générateur (numéroté à partir de 1). */
    public double generatorParticles(int generator) {
        return generator >= 0 && generator < generatorParticles.length ? generatorParticles[generator] : 1.0;
    }

    /** Multiplicateur de la vitesse d'un générateur (numéroté à partir de 1). */
    public double generatorSpeed(int generator) {
        return generator >= 0 && generator < generatorSpeed.length ? generatorSpeed[generator] : 1.0;
    }

    /** Le tableau, agrandi au besoin pour contenir cet indice ; les cases ajoutées valent 1. */
    private static double[] grown(double[] values, int index) {
        if (index < values.length) return values;
        double[] bigger = java.util.Arrays.copyOf(values, index + 1);
        java.util.Arrays.fill(bigger, values.length, bigger.length, 1.0);
        return bigger;
    }

    /** Diviseur d'un prix : 1 sans bonus, 2 quand le prix est divisé par deux. */
    public double costDivisor(ElementEffect.CostTarget target) {
        return 1 + costs.getOrDefault(target, 0.0);
    }

    /** Diviseur du délai d'un automatisme, bonus « tous les automatismes » compris. */
    public double automationDivisor(ElementEffect.AutomationTarget target) {
        return 1 + automations.getOrDefault(target, 0.0)
                + automations.getOrDefault(ElementEffect.AutomationTarget.ALL, 0.0);
    }

    /** Ce que les éléments ajoutent à une amélioration (0 sans élément). */
    public double upgradeBoost(ElementEffect.UpgradeTarget target) {
        return upgrades.getOrDefault(target, 0.0);
    }

    public List<SynergyTerm> synergies() {
        return synergies;
    }

    /** Chance qu'une synthèse donne un second élément, entre 0 et 1. */
    public double doubleDrawChance() {
        return Math.min(1, doubleDraw);
    }

    /** Nombre d'ensembles complets dans la collection. */
    public int completedSets() {
        return completedSets;
    }

    /** Nombre d'ensembles à moitié réunis, sans être complets. */
    public int halfSets() {
        return halfSets;
    }

    /** Multiplicateur de la chance des familles rares. */
    public double luckMultiplier() {
        return 1 + luck;
    }
}
