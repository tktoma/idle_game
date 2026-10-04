package idle.core;

/**
 * Un élément du tableau périodique.
 *
 * @param number   numéro atomique, de 1 à 118 ; sert d'identifiant dans les sauvegardes
 * @param symbol   symbole chimique
 * @param name     nom français
 * @param category famille, qui fixe sa rareté et son bonus
 */
public record Element(int number, String symbol, String name, ElementCategory category) {}
