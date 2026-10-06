package idle.ui;

import idle.core.BigNum;
import idle.core.SizeScale;
import java.util.Locale;

/**
 * Mise en forme des nombres affichés au joueur. Les grands nombres suivent la notation choisie
 * dans les réglages ({@link Settings.Notation}).
 */
final class Format {

    private static final BigNum THOUSAND = BigNum.of(1, 3);
    private static final BigNum MILLION = BigNum.of(1, 6);
    /** Abréviations des milliers successifs : 1e3, 1e6, 1e9… jusqu'à 1e33. */
    private static final String[] LETTERS = {"K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};

    private static Settings.Notation notation = Settings.Notation.SCIENTIFIC;

    /** Change la façon d'écrire les grands nombres, partout dans le jeu. */
    static void setNotation(Settings.Notation chosen) {
        notation = chosen;
    }

    /**
     * Grand nombre dans la notation choisie : « 1.23e7 », « 12.3e6 » ou « 12.3 M ». En dessous de
     * mille, toutes les notations donnent la même chose : « 123.45 ».
     */
    static String big(BigNum value) {
        if (notation == Settings.Notation.SCIENTIFIC || value.sign() <= 0 || value.lt(THOUSAND)) return value.format();
        // Mantisse ramenée entre 1 et 1000, avec un exposant multiple de trois.
        long exponent = Math.floorDiv(value.exponent(), 3L) * 3;
        double mantissa = value.mantissa() * Math.pow(10, value.exponent() - exponent);
        if (mantissa >= 999.5) {            // 999.7 s'arrondirait à 1000 : on passe au millier suivant
            mantissa /= 1000;
            exponent += 3;
        }
        // Trois chiffres significatifs : 1.23, 12.3, 123.
        String digits = String.format(Locale.ROOT, mantissa < 9.995 ? "%.2f" : mantissa < 99.95 ? "%.1f" : "%.0f", mantissa);
        int letter = (int) (exponent / 3) - 1;
        if (notation == Settings.Notation.LETTERS && letter < LETTERS.length) return digits + " " + LETTERS[letter];
        return notation == Settings.Notation.LETTERS ? value.format() : digits + "e" + exponent;
    }

    /**
     * Durée lisible : « 8.2 s », « 12 min 05 s », « 3 h 12 min », « 2 j 3 h ». Deux unités au
     * plus : au-delà, la précision n'apporte rien.
     */
    static String duration(double seconds) {
        if (seconds < 60) {
            String text = String.format(Locale.ROOT, "%.1f", Math.max(0, seconds));
            return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + " s";
        }
        long total = (long) Math.floor(seconds);
        long days = total / 86_400, hours = total % 86_400 / 3_600, minutes = total % 3_600 / 60, rest = total % 60;
        if (days > 0) return days + " j " + hours + " h";
        if (hours > 0) return hours + " h " + String.format(Locale.ROOT, "%02d", minutes) + " min";
        return minutes + " min " + String.format(Locale.ROOT, "%02d", rest) + " s";
    }

    /**
     * Attente avant un achat : « 42 s », « 3 min 05 s », « 2 h 10 min ». Arrondie à la seconde
     * supérieure, pour ne pas annoncer « 0 s » quand il manque encore une fraction de seconde, et
     * bornée : au-delà d'un mois, le nombre n'apprend plus rien.
     */
    static String wait(double seconds) {
        if (Double.isNaN(seconds) || seconds > 30 * 86_400.0) return "plus d'un mois";
        return duration(Math.ceil(Math.max(0, seconds)));
    }

    /**
     * Durée courte, pour les graduations d'un graphique : « 30 s », « 12 min », « 12 min 05 »,
     * « 2 h », « 2 h 30 », « 1 j », « 1 j 4 h ».
     */
    static String clock(double seconds) {
        long total = Math.round(Math.max(0, seconds));
        if (seconds < 60 && total < 60) {
            String text = String.format(Locale.ROOT, "%.1f", Math.max(0, seconds));
            return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + " s";
        }
        long days = total / 86_400, hours = total % 86_400 / 3_600, minutes = total % 3_600 / 60, rest = total % 60;
        if (days > 0) return days + " j" + (hours > 0 ? " " + hours + " h" : "");
        if (hours > 0) return hours + " h" + (minutes > 0 ? String.format(Locale.ROOT, " %02d", minutes) : "");
        return minutes + " min" + (rest > 0 ? String.format(Locale.ROOT, " %02d", rest) : "");
    }

    /**
     * Une puissance de dix ronde, pour les graduations d'un graphique : « 1e20 » en notation
     * scientifique (plus court que « 1.00e20 »), sinon comme les autres grands nombres.
     */
    static String powerOfTen(long exponent) {
        return notation == Settings.Notation.SCIENTIFIC ? "1e" + exponent : big(BigNum.of(1, exponent));
    }

    /** Nombre entier avec des espaces entre les milliers : « 12 345 ». */
    static String whole(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    /**
     * Quantité de ressource : en entier tant que c'est lisible, puis en notation scientifique.
     * Arrondi vers le bas : afficher 13 quand on a 12,6 laisserait croire qu'un achat à 13 est possible.
     */
    static String count(BigNum value) {
        // Le petit ajout absorbe les erreurs d'arrondi des double (2,9999999 doit s'afficher 3).
        return value.lt(MILLION) ? String.valueOf((long) Math.floor(value.toDouble() + 1e-9)) : big(value);
    }

    /** Multiplicateur : « ×2 », « ×1.25 », puis en notation scientifique. */
    static String multiplier(BigNum value) {
        if (!value.lt(THOUSAND)) return "×" + big(value);
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
