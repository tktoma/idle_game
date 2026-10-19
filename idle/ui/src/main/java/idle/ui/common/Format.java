package idle.ui.common;

import idle.core.darkmatter.SizeScale;
import idle.core.math.BigNum;
import idle.ui.Settings;
import java.util.Locale;

/**
 * Mise en forme des nombres affichés au joueur. Les grands nombres suivent la notation choisie
 * dans les réglages ({@link Settings.Notation}).
 */
public final class Format {

    private static final BigNum THOUSAND = BigNum.of(1, 3);
    private static final BigNum MILLION = BigNum.of(1, 6);
    /** Abréviations des milliers successifs : 1e3, 1e6, 1e9… jusqu'à 1e33. */
    private static final String[] LETTERS = {"K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};

    private static Settings.Notation notation = Settings.Notation.SCIENTIFIC;

    /** Change la façon d'écrire les grands nombres, partout dans le jeu. */
    public static void setNotation(Settings.Notation chosen) {
        notation = chosen;
    }

    /** La façon d'écrire les grands nombres en ce moment. */
    public static Settings.Notation notation() {
        return notation;
    }

    /** Les noms des milliers, au singulier, de 1e3 (mille) à 1e63 (décilliard), selon l'échelle longue en usage en français. */
    private static final String[] WORDS = {"mille", "million", "milliard", "billion", "billiard", "trillion", "trilliard",
            "quadrillion", "quadrilliard", "quintillion", "quintilliard", "sextillion", "sextilliard", "septillion", "septilliard",
            "octillion", "octilliard", "nonillion", "nonilliard", "décillion", "décilliard"};
    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    /**
     * Grand nombre dans la notation choisie : « 1.23e7 », « 12.3e6 », « 12.3 M », « 12.3 millions »,
     * « 1.23×10⁷ », « 12.3 b », « 10^7.09 » ou « 1.23 ×X^VII ». En dessous de mille, toutes les
     * notations donnent la même chose : « 123.45 ».
     */
    public static String big(BigNum value) {
        if (notation == Settings.Notation.SCIENTIFIC || value.sign() <= 0 || value.lt(THOUSAND)) return value.format();
        switch (notation) {
            case POWERS -> {
                long exponent = roundedExponent(value);
                return mantissa(value) + "×10" + superscript(exponent);
            }
            case LOG -> {
                double log = value.exponent() + Math.log10(value.mantissa());
                return "10^" + String.format(Locale.ROOT, log < 1e5 ? "%.2f" : "%.0f", log);
            }
            case ROMAN -> {
                long exponent = roundedExponent(value);
                return exponent < 4000 ? mantissa(value) + " ×X^" + roman((int) exponent) : value.format();
            }
            default -> { }
        }
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
        switch (notation) {
            case LETTERS -> {
                return letter < LETTERS.length ? digits + " " + LETTERS[letter] : value.format();
            }
            case WORDS -> {
                if (letter >= WORDS.length) return value.format();
                // « mille » ne prend pas de s ; les autres, à partir de deux.
                return digits + " " + WORDS[letter] + (letter > 0 && Double.parseDouble(digits) >= 2 ? "s" : "");
            }
            case ALPHABET -> {
                return digits + " " + alphabet(letter);
            }
            default -> {
                return digits + "e" + exponent;
            }
        }
    }

    /** La mantisse, à trois chiffres significatifs : « 1.23 » ; 9.996 s'arrondit à « 1.00 », la puissance suivante. */
    private static String mantissa(BigNum value) {
        String digits = String.format(Locale.ROOT, "%.2f", value.mantissa());
        return digits.equals("10.00") ? "1.00" : digits;
    }

    /** La puissance de dix qui va avec {@link #mantissa(BigNum)}. */
    private static long roundedExponent(BigNum value) {
        return String.format(Locale.ROOT, "%.2f", value.mantissa()).equals("10.00") ? value.exponent() + 1 : value.exponent();
    }

    /** Un entier en exposant : « ⁷ », « ¹²³ », « ⁻⁴ ». */
    static String superscript(long number) {
        StringBuilder text = new StringBuilder(number < 0 ? "⁻" : "");
        for (char digit : String.valueOf(Math.abs(number)).toCharArray()) text.append(SUPERSCRIPTS.charAt(digit - '0'));
        return text.toString();
    }

    /** Le rang d'un millier en lettres : 0 → a, 25 → z, 26 → aa, 27 → ab… */
    static String alphabet(long index) {
        StringBuilder text = new StringBuilder();
        long n = index;
        do {
            text.insert(0, (char) ('a' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return text.toString();
    }

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SIGNS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /** Un entier de 1 à 3 999 en chiffres romains. */
    static String roman(int number) {
        StringBuilder text = new StringBuilder();
        int rest = number;
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (rest >= ROMAN_VALUES[i]) {
                text.append(ROMAN_SIGNS[i]);
                rest -= ROMAN_VALUES[i];
            }
        }
        return text.toString();
    }

    /**
     * Durée lisible : « 8.2 s », « 12 min 05 s », « 3 h 12 min », « 2 j 3 h ». Deux unités au
     * plus : au-delà, la précision n'apporte rien.
     */
    public static String duration(double seconds) {
        if (seconds < 60) {
            String text = String.format(Locale.ROOT, "%.1f", Math.max(0, seconds));
            return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + " s";
        }
        long total = (long) Math.floor(seconds);
        long days = total / 86_400, hours = total % 86_400 / 3_600, minutes = total % 3_600 / 60, rest = total % 60;
        if (days > 0) return days + " j " + hours + " h";
        if (hours > 0) return hours + " h " + (minutes < 10 ? "0" : "") + minutes + " min";
        return minutes + " min " + (rest < 10 ? "0" : "") + rest + " s";
    }

    /**
     * Attente avant un achat : « 42 s », « 3 min 05 s », « 2 h 10 min ». Arrondie à la seconde
     * supérieure, pour ne pas annoncer « 0 s » quand il manque encore une fraction de seconde, et
     * bornée : au-delà d'un mois, le nombre n'apprend plus rien.
     */
    public static String wait(double seconds) {
        if (Double.isNaN(seconds) || seconds > 30 * 86_400.0) return "plus d'un mois";
        return duration(Math.ceil(Math.max(0, seconds)));
    }

    /**
     * Durée courte, pour les graduations d'un graphique : « 30 s », « 12 min », « 12 min 05 »,
     * « 2 h », « 2 h 30 », « 1 j », « 1 j 4 h ».
     */
    public static String clock(double seconds) {
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
    public static String powerOfTen(long exponent) {
        return notation == Settings.Notation.SCIENTIFIC ? "1e" + exponent : big(BigNum.of(1, exponent));
    }

    /** Nombre entier avec des espaces entre les milliers : « 12 345 ». */
    public static String whole(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    /**
     * Quantité de ressource : en entier tant que c'est lisible, puis en notation scientifique.
     * Arrondi vers le bas : afficher 13 quand on a 12,6 laisserait croire qu'un achat à 13 est possible.
     */
    public static String count(BigNum value) {
        // Le petit ajout absorbe les erreurs d'arrondi des double (2,9999999 doit s'afficher 3).
        return value.lt(MILLION) ? String.valueOf((long) Math.floor(value.toDouble() + 1e-9)) : big(value);
    }

    /** Multiplicateur : « ×2 », « ×1.25 », puis en notation scientifique. */
    public static String multiplier(BigNum value) {
        if (!value.lt(THOUSAND)) return "×" + big(value);
        return "×" + ElementText.number(value.toDouble());
    }

    /** Quantité qui peut être fractionnaire : « 1 », « 2.35 », puis en notation scientifique. */
    public static String amount(BigNum value) {
        return multiplier(value).substring(1);
    }

    /** Unités de longueur, de la plus petite à la plus grande : symbole et valeur en mètres. */
    private static final String[] LENGTH_UNITS = {"fm", "pm", "nm", "µm", "mm", "m", "km", "années-lumière"};
    /** Une année-lumière, en mètres. */
    public static final BigNum LIGHT_YEAR = SizeScale.LIGHT_YEAR;
    private static final BigNum[] LENGTH_VALUES = {BigNum.of(1, -15), BigNum.of(1, -12), BigNum.of(1, -9),
            BigNum.of(1, -6), BigNum.of(1, -3), BigNum.ONE, BigNum.of(1, 3), LIGHT_YEAR};
    /** À partir d'un dixième d'année-lumière, on quitte les kilomètres. */
    public static final BigNum LIGHT_YEARS_FROM = LIGHT_YEAR.multiply(0.1);

    /**
     * Longueur en mètres, écrite dans l'unité la plus lisible : « 3.2 nm », « 150 m »,
     * « 384400 km », « 2.5 années-lumière », puis en notation scientifique.
     */
    public static String length(BigNum meters) {
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
    public static String perMinute(BigNum perSecond) {
        BigNum value = perSecond.multiply(60);
        if (value.lt(THOUSAND)) {
            String text = String.format(Locale.ROOT, "%.1f", value.toDouble());
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
        return count(value);
    }

    private Format() {}
}
