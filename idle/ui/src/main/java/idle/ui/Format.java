package idle.ui;

import idle.core.BigNum;
import idle.core.SizeScale;
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

    /** Unités de longueur, de la plus petite à la plus grande : symbole et valeur en mètres. */
    private static final String[] LENGTH_UNITS = {"fm", "pm", "nm", "µm", "mm", "m", "km", "années-lumière"};
    /** Une année-lumière, en mètres. */
    static final BigNum LIGHT_YEAR = SizeScale.LIGHT_YEAR;
    private static final BigNum[] LENGTH_VALUES = {BigNum.of(1, -15), BigNum.of(1, -12), BigNum.of(1, -9),
            BigNum.of(1, -6), BigNum.of(1, -3), BigNum.ONE, BigNum.of(1, 3), LIGHT_YEAR};
    /** À partir d'un dixième d'année-lumière, on quitte les kilomètres. */
    static final BigNum LIGHT_YEARS_FROM = LIGHT_YEAR.multiply(0.1);

    /**
     * Longueur en mètres, écrite dans l'unité la plus lisible : « 3.2 nm », « 150 m »,
     * « 384400 km », « 2.5 années-lumière », puis en notation scientifique.
     */
    static String length(BigNum meters) {
        int unit = 0;
        for (int i = 1; i < LENGTH_UNITS.length; i++) {
            BigNum from = i == LENGTH_UNITS.length - 1 ? LIGHT_YEARS_FROM : LENGTH_VALUES[i];
            if (meters.gte(from)) unit = i;
        }
        BigNum value = meters.divide(LENGTH_VALUES[unit]);
        String text = value.lt(THOUSAND) ? amount(value) : count(value);
        boolean lightYears = unit == LENGTH_UNITS.length - 1;
        return text + " " + (lightYears && value.lt(BigNum.of(1.995)) ? "année-lumière" : LENGTH_UNITS[unit]);
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
