package idle.ui;

import idle.core.ElementEffect;
import java.util.Locale;

/**
 * Les textes qui décrivent l'effet d'un élément du tableau périodique.
 *
 * <p>Tout est dans un {@code switch} sur {@link ElementEffect} : quand un nouveau type d'effet
 * est ajouté dans core, le compilateur signale qu'il manque ici.
 */
final class ElementText {

    /** Ce que fait un élément, en une phrase, pour un exemplaire. */
    static String describe(ElementEffect effect) {
        return switch (effect) {
            case ElementEffect.GeneratorBoost boost ->
                    "Générateur " + boost.generator() + " : "
                            + (boost.stat() == ElementEffect.Stat.SPEED ? "vitesse" : "particules")
                            + " ×" + number(boost.factor());
            case ElementEffect.Add add ->
                    "+" + percent(add.bonus()) + " " + of(add.stat());
            case ElementEffect.Multiply multiply ->
                    capitalized(multiply.stat()) + " ×" + number(multiply.factor());
            case ElementEffect.MultiplyProduction production ->
                    "Particules ×" + number(production.factor()) + " et vitesse ×" + number(production.factor());
            case ElementEffect.MultiplyEverything everything ->
                    "Particules, vitesse et atomes par fusion ×" + number(everything.factor());
            case ElementEffect.CostReduction reduction ->
                    "Prix " + of(reduction.target()) + " ÷" + number(1 + reduction.bonus());
            case ElementEffect.AutomationSpeed speed ->
                    "+" + percent(speed.bonus()) + " de cadence pour " + of(speed.target());
            case ElementEffect.UpgradeBoost boost -> describe(boost);
            case ElementEffect.Synergy synergy ->
                    "+" + percent(synergy.perUnit()) + " " + of(synergy.stat()) + " " + per(synergy.source());
            case ElementEffect.DoubleDraw draw ->
                    "+" + percent(draw.chance()) + " de chance qu'une synthèse donne deux éléments";
            case ElementEffect.Luck luck ->
                    "Familles rares " + percent(luck.bonus()) + " plus fréquentes à la synthèse";
        };
    }

    /** Comment les exemplaires d'un même élément se cumulent, selon son effet. */
    static String stacking(ElementEffect effect, int copies) {
        if (copies <= 1) return "";
        String strength = number(Math.sqrt(copies));
        return switch (effect) {
            case ElementEffect.GeneratorBoost boost -> "multiplicateur appliqué " + strength + " fois";
            case ElementEffect.Multiply multiply -> "multiplicateur appliqué " + strength + " fois";
            case ElementEffect.MultiplyProduction production -> "multiplicateur appliqué " + strength + " fois";
            case ElementEffect.MultiplyEverything everything -> "multiplicateur appliqué " + strength + " fois";
            case ElementEffect.Add add -> "effet ×" + strength;
            case ElementEffect.CostReduction reduction -> "effet ×" + strength;
            case ElementEffect.AutomationSpeed speed -> "effet ×" + strength;
            case ElementEffect.UpgradeBoost boost -> "effet ×" + strength;
            case ElementEffect.Synergy synergy -> "effet ×" + strength;
            case ElementEffect.DoubleDraw draw -> "effet ×" + strength;
            case ElementEffect.Luck luck -> "effet ×" + strength;
        };
    }

    private static String describe(ElementEffect.UpgradeBoost boost) {
        double amount = boost.amount();
        return switch (boost.target()) {
            case DOUBLING -> "Renforce « Dédoublement » : +" + number(amount) + " à son multiplicateur, à chaque niveau";
            case MASS -> "Renforce « Masse atomique » : +" + percent(amount) + " de particules par atome créé";
            case PATIENCE -> "Renforce « Patience » : son bonus grandit plus vite (+" + number(amount) + ")";
            case CATALYST -> "Renforce « Catalyseur » : +" + number(amount * 100) + " point de vitesse par niveau de Catalyseur";
            case YIELD -> "Renforce « Rendement » : +" + percent(amount) + " d'atomes à chaque ×10 de production";
            case SPEED -> "Renforce « Vitesse de création » : +" + number(amount * 100) + " point à chaque niveau";
        };
    }

    /** « de particules », « de vitesse », « d'atomes par fusion ». */
    private static String of(ElementEffect.Stat stat) {
        return switch (stat) {
            case PARTICLES -> "de particules";
            case SPEED -> "de vitesse";
            case ATOMS -> "d'atomes par fusion";
        };
    }

    private static String capitalized(ElementEffect.Stat stat) {
        return switch (stat) {
            case PARTICLES -> "Particules";
            case SPEED -> "Vitesse";
            case ATOMS -> "Atomes par fusion";
        };
    }

    /** Ce dont le prix baisse : « de la vitesse de création »… */
    static String of(ElementEffect.CostTarget target) {
        return switch (target) {
            case SPEED_UPGRADES -> "de la vitesse de création";
            case ATOM_UPGRADES -> "des améliorations en atomes";
            case SYNTHESIS -> "de la synthèse";
        };
    }

    private static String of(ElementEffect.AutomationTarget target) {
        return switch (target) {
            case SPEED_UPGRADES -> "l'achat automatique de la vitesse";
            case GENERATORS -> "l'achat automatique des générateurs";
            case FUSION -> "la fusion automatique";
            case SYNTHESIS -> "la synthèse automatique";
            case ALL -> "tous les automatismes";
        };
    }

    private static String per(ElementEffect.SynergySource source) {
        return switch (source) {
            case DISTINCT_ELEMENTS -> "par élément différent découvert";
            case SPEED_LEVELS -> "par niveau de « Vitesse de création »";
            case GENERATORS -> "par générateur en activité";
            case AVAILABLE_ATOMS -> "par atome disponible";
            case AUTOMATIONS -> "par automatisme possédé";
        };
    }

    /** 0.3 → « 30 % », 0.025 → « 2.5 % ». */
    static String percent(double fraction) {
        return number(fraction * 100) + " %";
    }

    /** Deux décimales au plus, sans zéros inutiles : 2.0 → « 2 », 1.25 → « 1.25 ». */
    static String number(double value) {
        // Les mêmes nombres reviennent d'une image à l'autre : chacun n'est mis en forme qu'une fois.
        Double key = value;
        String known = NUMBERS.get(key);
        if (known != null) return known;
        String text = String.format(Locale.ROOT, "%.2f", value);
        // Les zéros de la fin, puis le point s'il ne reste rien derrière.
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') end--;
        if (end > 0 && text.charAt(end - 1) == '.') end--;
        text = text.substring(0, end);
        if (NUMBERS.size() >= REMEMBERED) NUMBERS.clear();
        NUMBERS.put(key, text);
        return text;
    }

    /** Les nombres déjà mis en forme par {@link #number(double)}, jusqu'à {@link #REMEMBERED} : au-delà, on repart de rien. */
    private static final java.util.Map<Double, String> NUMBERS = new java.util.HashMap<>();
    private static final int REMEMBERED = 4096;

    private ElementText() {}
}
