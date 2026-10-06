package idle.core;

import java.util.List;
import java.util.Map;

/**
 * Un ensemble du tableau périodique : une famille entière ou une période entière. Le joueur qui
 * possède au moins un exemplaire de chacun de ses éléments gagne son bonus, en plus de ceux des
 * éléments eux-mêmes. Dès la moitié de l'ensemble réunie, le bonus agit au quart de sa force : cela
 * fait deux objectifs par ensemble, dont un qui arrive tôt.
 *
 * @param id       identifiant stable
 * @param name     nom affiché au joueur
 * @param category la famille à réunir, ou {@code null} pour une période
 * @param period   la période à réunir (de 1 à 7), ou 0 pour une famille
 * @param effect   le bonus de l'ensemble complet, appliqué avec une force de 1
 */
public record ElementSet(String id, String name, ElementCategory category, int period, ElementEffect effect) {

    /** Force du bonus quand la moitié de l'ensemble est réunie (0.25 = le quart de l'effet). */
    public static final double HALF_STRENGTH = 0.25;

    public ElementSet {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if ((category == null) == (period == 0)) {
            throw new IllegalArgumentException("Un ensemble réunit une famille ou une période, pas les deux");
        }
        if (period < 0 || period > PeriodicTable.PERIODS) throw new IllegalArgumentException("Période inconnue : " + period);
        if (effect == null) throw new IllegalArgumentException("effet manquant");
    }

    /** Ensemble d'une famille entière. */
    public static ElementSet family(ElementCategory category, ElementEffect effect) {
        return new ElementSet("family_" + category.name().toLowerCase(java.util.Locale.ROOT), category.label(),
                category, 0, effect);
    }

    /** Ensemble d'une période entière. */
    public static ElementSet period(int period, ElementEffect effect) {
        return new ElementSet("period_" + period, "Période " + period, null, period, effect);
    }

    /** Vrai pour un ensemble de famille, faux pour un ensemble de période. */
    public boolean isFamily() {
        return category != null;
    }

    /** Les éléments à réunir, par numéro atomique croissant. */
    public List<Element> members() {
        return isFamily() ? PeriodicTable.elements(category) : PeriodicTable.elementsOfPeriod(period);
    }

    /** Nombre d'éléments de l'ensemble présents dans une collection (numéro atomique → exemplaires). */
    public int progress(Map<Integer, Integer> owned) {
        int found = 0;
        for (Element element : members()) {
            if (owned.getOrDefault(element.number(), 0) > 0) found++;
        }
        return found;
    }

    /** Vrai si la collection contient au moins un exemplaire de chaque élément de l'ensemble. */
    public boolean isComplete(Map<Integer, Integer> owned) {
        return progress(owned) == members().size();
    }

    /**
     * Nombre d'éléments à réunir pour le premier palier de l'ensemble : la moitié, arrondie
     * au-dessus, et jamais moins de deux. Un ensemble de deux éléments n'a donc pas de premier
     * palier : un seul élément ne fait pas un ensemble.
     */
    public int halfSize() {
        return Math.max(2, (members().size() + 1) / 2);
    }

    /**
     * Force du bonus de l'ensemble pour une collection : 0 tant qu'il en manque plus de la
     * moitié, {@link #HALF_STRENGTH} à partir de la moitié, 1 quand l'ensemble est complet.
     */
    public double strength(Map<Integer, Integer> owned) {
        int found = progress(owned);
        if (found == members().size()) return 1;
        return found >= halfSize() ? HALF_STRENGTH : 0;
    }
}
