package idle.ui;

import idle.core.BigNum;
import java.util.Locale;

/** Mise en forme des nombres affichés au joueur. */
final class Format {

    private static final BigNum THOUSAND = BigNum.of(1, 3);
    private static final BigNum MILLION = BigNum.of(1, 6);

    /**
     * Quantité de ressource : en entier tant que c'est lisible, puis en notation scientifique.
     * Arrondi vers le bas : afficher 13 quand on a 12,6 laisserait croire qu'un achat à 13 est possible.
     */
    static String count(BigNum value) {
        // Le petit ajout absorbe les erreurs d'arrondi des double (2,9999999 doit s'afficher 3).
        return value.lt(MILLION) ? String.valueOf((long) Math.floor(value.toDouble() + 1e-9)) : value.format();
    }

    /** Multiplicateur : « ×2 », « ×1.25 », puis en notation scientifique. */
    static String multiplier(BigNum value) {
        if (!value.lt(THOUSAND)) return "×" + value.format();
        String text = String.format(Locale.ROOT, "%.2f", value.toDouble());
        return "×" + text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Quantité qui peut être fractionnaire : « 1 », « 2.35 », puis en notation scientifique. */
    static String amount(BigNum value) {
        return multiplier(value).substring(1);
    }

    /** Production par minute : une décimale tant que le nombre est petit, puis comme les autres nombres. */
    static String perMinute(BigNum perSecond) {
        BigNum value = perSecond.multiply(60);
        if (value.lt(THOUSAND)) {
            String text = String.format(Locale.ROOT, "%.1f", value.toDouble());
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
        return count(value);
    }

    private Format() {}
}
