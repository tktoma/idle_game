package idle.ui.common;

import idle.core.math.BigNum;
import java.util.Locale;

/**
 * Les notations de la version 0.200 (SKINS.md, section 8) : « Binaire », « Hexadécimale », « Chiffres japonais »,
 * « Morse », « Préfixes SI » et « Battements de cœur ». {@link Format#big} les appelle ; toutes restent lisibles jusqu'aux
 * plus grands nombres du jeu (1e85000 : « 2^282 361 », « 16^70 590 », « ×10^85000 »).
 */
final class Notations {

    /** Les préfixes du Système international, de 1e3 (kilo) à 1e30 (quetta). */
    static final String[] SI_PREFIXES = {"k", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q"};
    /** Les unités japonaises de quatre en quatre chiffres : 万 (1e4), 億 (1e8)… 無量大数 (1e68). */
    static final String[] JAPANESE_UNITS = {"万", "億", "兆", "京", "垓", "秭", "穣", "溝", "澗", "正", "載", "極",
            "恒河沙", "阿僧祇", "那由他", "不可思議", "無量大数"};
    /** Les chiffres en morse, de 0 à 9. */
    static final String[] MORSE_DIGITS = {"−−−−−", "·−−−−", "··−−−", "···−−", "····−", "·····", "−····", "−−···", "−−−··", "−−−−·"};
    /** Le point, en morse. */
    static final String MORSE_POINT = "·−·−·−";
    /** En dessous de 1e15, l'hexadécimal s'écrit chiffre à chiffre, exactement. */
    private static final BigNum HEX_EXACT = BigNum.of(1, 15);

    /**
     * Une puissance de {@code base} : « 1.21×2^10 », « 1.00×16^5 », l'exposant avec des espaces entre les milliers
     * (« 2^282 361 »). La mantisse est entre 1 et la base, à trois chiffres significatifs.
     */
    static String power(BigNum value, int base) {
        double log = (value.exponent() + Math.log10(value.mantissa())) / Math.log10(base);
        long exponent = (long) Math.floor(log);
        double mantissa = Math.pow(base, log - exponent);
        String digits = String.format(Locale.ROOT, "%.2f", mantissa);
        if (Double.parseDouble(digits) >= base) {     // 1.999 s'arrondit à 2.00 : la puissance suivante
            digits = "1.00";
            exponent++;
        }
        return digits + "×" + base + "^" + Format.whole(exponent);
    }

    /** Hexadécimal : « 0x4D2 » tant que le nombre tient exactement, puis une puissance de seize : « 1.23×16^70 590 ». */
    static String hex(BigNum value) {
        if (value.lt(HEX_EXACT)) return "0x" + Long.toHexString((long) Math.floor(value.toDouble() + 1e-9)).toUpperCase(Locale.ROOT);
        return power(value, 16);
    }

    /**
     * Les chiffres japonais : « 1234 » en dessous de 万, puis une mantisse entre 1 et 9 999 et l'unité (« 100万 », « 10垓 »,
     * « 1.23無量大数 ») ; au-delà de 1e72, « 1.23×10^85000 ».
     */
    static String japanese(BigNum value) {
        long exponent = Math.floorDiv(value.exponent(), 4L) * 4;
        if (exponent == 0) return Format.whole((long) Math.floor(value.toDouble() + 1e-9));
        double mantissa = value.mantissa() * Math.pow(10, value.exponent() - exponent);
        if (mantissa >= 9999.5) {
            mantissa /= 10_000;
            exponent += 4;
        }
        int unit = (int) (exponent / 4) - 1;
        if (unit >= JAPANESE_UNITS.length) return Format.mantissa(value) + "×10^" + Format.roundedExponent(value);
        String digits = String.format(Locale.ROOT, mantissa < 9.995 ? "%.2f" : mantissa < 99.95 ? "%.1f" : "%.0f", mantissa);
        return digits + JAPANESE_UNITS[unit];
    }

    /** Le morse (fantaisie) : la mantisse chiffre à chiffre, le point compris, puis l'exposant en clair : « ·−−−− ·−·−·− ··−−− ···−− e6 ». */
    static String morse(String mantissa, long exponent) {
        StringBuilder text = new StringBuilder();
        for (char sign : mantissa.toCharArray()) {
            if (text.length() > 0) text.append(' ');
            text.append(sign == '.' ? MORSE_POINT : MORSE_DIGITS[sign - '0']);
        }
        return text + " e" + exponent;
    }

    /** Une durée en battements de cœur, un par seconde (60 par minute) : « 1 battement », « 3 600 battements ». */
    static String heartbeats(double seconds) {
        long beats = Math.round(Math.max(0, seconds));
        return Format.whole(beats) + (beats > 1 ? " battements" : " battement");
    }

    private Notations() {}
}
