package idle.core;

import java.util.Locale;

/**
 * Nombre décimal à très grande amplitude : {@code mantissa × 10^exponent}.
 *
 * <p>Même principe que break_infinity.js (Antimatter Dimensions) : on garde
 * ~15 chiffres significatifs dans un {@code double} et l'ordre de grandeur
 * dans un {@code long}. On peut donc aller bien au-delà de 1e308, avec des
 * opérations aussi rapides que sur des {@code double}.
 *
 * <p>Toute instance est normalisée par le constructeur :
 * <ul>
 *   <li>zéro est toujours {@code (0.0, 0)} ;</li>
 *   <li>sinon {@code 1 <= |mantissa| < 10}.</li>
 * </ul>
 * Grâce à ça, {@code equals}, {@code hashCode} et {@code compareTo} sont cohérents.
 * Le type est immuable : chaque opération renvoie une nouvelle instance.
 */
public record BigNum(double mantissa, long exponent) implements Comparable<BigNum> {

    public static final BigNum ZERO = new BigNum(0, 0);
    public static final BigNum ONE = new BigNum(1, 0);
    public static final BigNum TEN = new BigNum(1, 1);

    /** Au-delà de cet écart d'exposants, le plus petit terme d'une addition est invisible. */
    private static final int MAX_SIGNIFICANT_DIGITS = 17;

    /** Puissances de 10 exactes de 1e-323 à 1e308 (toute la plage d'un double). */
    private static final int POW10_MIN = -323;
    private static final int POW10_MAX = 308;
    private static final double[] POW10 = new double[POW10_MAX - POW10_MIN + 1];

    static {
        for (int i = POW10_MIN; i <= POW10_MAX; i++) {
            POW10[i - POW10_MIN] = Double.parseDouble("1e" + i);
        }
    }

    /** Constructeur canonique : normalise la mantisse dans [1, 10). */
    public BigNum {
        if (Double.isNaN(mantissa) || Double.isInfinite(mantissa)) {
            throw new IllegalArgumentException("Mantisse invalide : " + mantissa);
        }
        if (mantissa == 0) {
            mantissa = 0.0; // évite -0.0, qui casserait equals()
            exponent = 0;
        } else {
            double abs = Math.abs(mantissa);
            if (abs < 1e-300) { // doubles sous-normaux : on les remonte d'abord
                mantissa *= 1e300;
                abs *= 1e300;
                exponent = Math.subtractExact(exponent, 300L);
            }
            if (abs < 1 || abs >= 10) {
                int shift = (int) Math.floor(Math.log10(abs));
                mantissa /= pow10(shift);
                exponent = Math.addExact(exponent, (long) shift);
            }
            // Rattrapage des erreurs d'arrondi (9.999999… ou 10.000…)
            if (Math.abs(mantissa) >= 10) {
                mantissa /= 10;
                exponent = Math.addExact(exponent, 1L);
            } else if (Math.abs(mantissa) < 1) {
                mantissa *= 10;
                exponent = Math.subtractExact(exponent, 1L);
            }
        }
    }

    // ------------------------------------------------------------------
    // Création
    // ------------------------------------------------------------------

    public static BigNum of(double value) {
        return new BigNum(value, 0);
    }

    public static BigNum of(double mantissa, long exponent) {
        return new BigNum(mantissa, exponent);
    }

    /** Renvoie 10^power, y compris pour une puissance non entière ou gigantesque. */
    public static BigNum pow10(double power) {
        if (Double.isNaN(power) || Math.abs(power) >= 9e18) {
            throw new ArithmeticException("Exposant hors limites : " + power);
        }
        double whole = Math.floor(power);
        return new BigNum(Math.pow(10, power - whole), (long) whole);
    }

    /** Relit le format produit par {@link #toString()}, ou un nombre classique ("1500", "2.5e30"). */
    public static BigNum parse(String text) {
        String s = text.trim();
        int e = Math.max(s.indexOf('e'), s.indexOf('E'));
        if (e < 0) {
            return of(Double.parseDouble(s));
        }
        return new BigNum(Double.parseDouble(s.substring(0, e)), Long.parseLong(s.substring(e + 1)));
    }

    // ------------------------------------------------------------------
    // Arithmétique
    // ------------------------------------------------------------------

    public BigNum add(BigNum other) {
        if (isZero()) return other;
        if (other.isZero()) return this;

        BigNum big = exponent >= other.exponent ? this : other;
        BigNum small = big == this ? other : this;
        long diff = big.exponent - small.exponent;
        if (diff > MAX_SIGNIFICANT_DIGITS) {
            return big; // le petit terme est sous la précision du double
        }
        return new BigNum(big.mantissa + small.mantissa / pow10((int) diff), big.exponent);
    }

    public BigNum subtract(BigNum other) {
        return add(other.negate());
    }

    public BigNum multiply(BigNum other) {
        if (isZero() || other.isZero()) return ZERO;
        return new BigNum(mantissa * other.mantissa, Math.addExact(exponent, other.exponent));
    }

    /** Raccourci pour les cas fréquents : {@code production.multiply(dt)}. */
    public BigNum multiply(double factor) {
        return new BigNum(mantissa * factor, exponent);
    }

    public BigNum divide(BigNum other) {
        if (other.isZero()) throw new ArithmeticException("Division par zéro");
        if (isZero()) return ZERO;
        return new BigNum(mantissa / other.mantissa, Math.subtractExact(exponent, other.exponent));
    }

    public BigNum divide(double divisor) {
        if (divisor == 0) throw new ArithmeticException("Division par zéro");
        return new BigNum(mantissa / divisor, exponent);
    }

    public BigNum negate() {
        return isZero() ? this : new BigNum(-mantissa, exponent);
    }

    public BigNum abs() {
        return mantissa < 0 ? negate() : this;
    }

    /**
     * Élève à la puissance {@code power}.
     * Une base négative n'est acceptée qu'avec une puissance entière.
     */
    public BigNum pow(double power) {
        if (Double.isNaN(power) || Double.isInfinite(power)) {
            throw new ArithmeticException("Puissance invalide : " + power);
        }
        if (power == 0) return ONE;
        if (isZero()) {
            if (power < 0) throw new ArithmeticException("0 élevé à une puissance négative");
            return ZERO;
        }
        if (mantissa < 0) {
            if (power != Math.rint(power)) {
                throw new ArithmeticException("Base négative avec une puissance non entière");
            }
            BigNum result = abs().pow(power);
            return Math.abs(power) % 2 == 1 ? result.negate() : result;
        }

        double newExponent = exponent * power;
        if (Math.abs(newExponent) >= 9e18) {
            throw new ArithmeticException("Résultat hors limites");
        }
        // Chemin précis : l'exposant reste entier, on élève seulement la mantisse.
        if (newExponent == Math.rint(newExponent)) {
            double m = Math.pow(mantissa, power);
            if (m != 0 && !Double.isInfinite(m)) {
                return new BigNum(m, (long) newExponent);
            }
        }
        // Cas général : on reporte la partie fractionnaire de l'exposant dans la mantisse.
        double whole = Math.floor(newExponent);
        double m = Math.pow(10, power * Math.log10(mantissa) + (newExponent - whole));
        if (m != 0 && !Double.isInfinite(m)) {
            return new BigNum(m, (long) whole);
        }
        return pow10(power * log10());
    }

    public BigNum sqrt() {
        if (mantissa < 0) throw new ArithmeticException("Racine carrée d'un nombre négatif");
        return pow(0.5);
    }

    /**
     * Logarithme décimal, avec les mêmes conventions que {@link Math#log10} :
     * {@code -Infinity} pour zéro, {@code NaN} pour un nombre négatif.
     */
    public double log10() {
        if (isZero()) return Double.NEGATIVE_INFINITY;
        if (mantissa < 0) return Double.NaN;
        return exponent + Math.log10(mantissa);
    }

    // ------------------------------------------------------------------
    // Comparaison
    // ------------------------------------------------------------------

    public boolean isZero() {
        return mantissa == 0;
    }

    /** -1, 0 ou 1. */
    public int sign() {
        return (int) Math.signum(mantissa);
    }

    @Override
    public int compareTo(BigNum other) {
        int sign = sign();
        if (sign != other.sign()) return Integer.compare(sign, other.sign());
        if (sign == 0) return 0;
        if (exponent != other.exponent) return sign * Long.compare(exponent, other.exponent);
        return Double.compare(mantissa, other.mantissa);
    }

    public boolean gt(BigNum other)  { return compareTo(other) > 0; }
    public boolean gte(BigNum other) { return compareTo(other) >= 0; }
    public boolean lt(BigNum other)  { return compareTo(other) < 0; }
    public boolean lte(BigNum other) { return compareTo(other) <= 0; }

    public BigNum max(BigNum other) { return gte(other) ? this : other; }
    public BigNum min(BigNum other) { return lte(other) ? this : other; }

    // ------------------------------------------------------------------
    // Conversion et affichage
    // ------------------------------------------------------------------

    /** Conversion en double : ±Infinity si trop grand, 0 si trop petit. */
    public double toDouble() {
        if (exponent > POW10_MAX) return mantissa > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        if (exponent < POW10_MIN) return 0;
        return mantissa * pow10((int) exponent);
    }

    /**
     * Affichage pour le joueur : "123.45" sous 1000, puis notation scientifique "1.23e45".
     */
    public String format() {
        if (exponent < 3) {
            String plain = String.format(Locale.ROOT, "%.2f", toDouble());
            if (plain.equals("-0.00")) return "0.00";
            // 999.999 s'arrondit à "1000.00" : dans ce cas on passe en notation scientifique
            if (Math.abs(Double.parseDouble(plain)) < 1000) return plain;
        }
        double m = Math.rint(mantissa * 100) / 100;
        long e = exponent;
        if (Math.abs(m) >= 10) { // 9.999 arrondi à 10.00 → 1.00 à l'exposant suivant
            m /= 10;
            e++;
        }
        return String.format(Locale.ROOT, "%.2fe%d", m, e);
    }

    /** Format technique sans perte, relisible par {@link #parse(String)} (pour les sauvegardes). */
    @Override
    public String toString() {
        return mantissa + "e" + exponent;
    }

    private static double pow10(int power) {
        return POW10[power - POW10_MIN];
    }
}
