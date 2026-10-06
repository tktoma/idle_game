package idle.core;

import java.util.List;

/**
 * Catalogue des ensembles du tableau périodique : les dix familles et les sept périodes.
 *
 * <p>Ce sont trente-quatre objectifs intermédiaires là où il n'y avait que « finir le tableau » :
 * chaque ensemble donne le quart de son bonus dès la moitié réunie, puis le bonus entier. Les
 * familles donnent un bonus dans le ton de la famille ; les périodes, un bonus plus général qui
 * grandit avec la longueur de la ligne. Les ensembles arrivent tard quand on laisse faire le
 * hasard (une famille commune se complète en 150 à 250 synthèses, les gaz nobles en 600) : la
 * synthèse ciblée ({@link Game#setSynthesisTarget(ElementCategory)}) sert à les avancer.
 *
 * <p>Comme pour les éléments, ce qui accélère la synthèse elle-même reste modeste : ce sont ces
 * bonus-là qui décident de la durée du tableau.
 */
public final class ElementSets {

    public static final List<ElementSet> DEFAULT = List.of(
            // ----- Les familles -----
            // Les 38 du bloc central : toute la production en profite.
            ElementSet.family(ElementCategory.TRANSITION_METAL,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 10)),
            ElementSet.family(ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SYNTHESIS, 0.20)),
            // Le Rendement gagne 2 points par ×10 de production.
            ElementSet.family(ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.YIELD, 0.02)),
            ElementSet.family(ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.ALL, 0.20)),
            ElementSet.family(ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.15)),
            // +0,1 % d'atomes par élément découvert : +12 % avec le tableau entier.
            ElementSet.family(ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.DISTINCT_ELEMENTS, ElementEffect.Stat.ATOMS, 0.001)),
            ElementSet.family(ElementCategory.HALOGEN,
                    new ElementEffect.DoubleDraw(0.10)),
            ElementSet.family(ElementCategory.LANTHANIDE,
                    new ElementEffect.Luck(1.0)),
            ElementSet.family(ElementCategory.NOBLE_GAS,
                    new ElementEffect.MultiplyEverything(1.25)),
            // Le dernier à se compléter : il sert surtout aux parties suivantes.
            ElementSet.family(ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(10)),

            // ----- Les périodes -----
            // Hydrogène et hélium : deux éléments, mais l'hélium est un gaz noble.
            ElementSet.period(1, new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 3)),
            ElementSet.period(2, new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.10)),
            ElementSet.period(3, new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.SYNTHESIS, 0.20)),
            ElementSet.period(4, new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 2)),
            ElementSet.period(5, new ElementEffect.CostReduction(ElementEffect.CostTarget.ATOM_UPGRADES, 0.50)),
            ElementSet.period(6, new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.20)),
            ElementSet.period(7, new ElementEffect.MultiplyEverything(1.5)));

    private ElementSets() {}
}
