package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BigNumTest {

    /** Compare deux BigNum à une erreur relative près (les double ne sont jamais exacts). */
    private static void assertClose(BigNum expected, BigNum actual) {
        assertEquals(expected.exponent(), actual.exponent(),
                () -> "exposant : attendu " + expected + ", obtenu " + actual);
        assertEquals(expected.mantissa(), actual.mantissa(), 1e-9,
                () -> "mantisse : attendu " + expected + ", obtenu " + actual);
    }

    @Nested
    class Normalisation {

        @ParameterizedTest
        @CsvSource({
                "1500,     1.5,  3",
                "0.025,    2.5, -2",
                "1,        1,    0",
                "9.99,     9.99, 0",
                "10,       1,    1",
                "-42,     -4.2,  1",
                "1e308,    1,    308",
                "1e-300,   1,   -300",
        })
        void mantisseRameneeEntre1Et10(double value, double mantissa, long exponent) {
            assertClose(new BigNum(mantissa, exponent), BigNum.of(value));
        }

        @Test
        void mantisseHorsPlageReporteeSurLExposant() {
            assertClose(BigNum.of(1.23, 102), BigNum.of(123, 100));
            assertClose(BigNum.of(5, 99), BigNum.of(0.05, 101));
        }

        @Test
        void zeroEstToujoursLeMeme() {
            assertEquals(BigNum.ZERO, BigNum.of(0));
            assertEquals(BigNum.ZERO, BigNum.of(-0.0));
            assertEquals(BigNum.ZERO, BigNum.of(0, 12345));
            assertEquals(BigNum.ZERO.hashCode(), BigNum.of(-0.0, 7).hashCode());
        }

        @Test
        void doublesSousNormaux() {
            assertClose(BigNum.of(4.940656458412465, -324), BigNum.of(Double.MIN_VALUE));
        }

        @Test
        void mantisseToujoursDansLIntervalle() {
            for (int e = -320; e <= 307; e++) {
                for (double m : new double[] {1, 1.0000000001, 3.3, 9.9999999999}) {
                    BigNum n = BigNum.of(Double.parseDouble(m + "e" + e));
                    double abs = Math.abs(n.mantissa());
                    assertTrue(abs >= 1 && abs < 10, () -> "mal normalisé : " + n);
                }
            }
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void valeursInvalidesRefusees(double value) {
            assertThrows(IllegalArgumentException.class, () -> BigNum.of(value));
        }
    }

    @Nested
    class Addition {

        @Test
        void memeOrdreDeGrandeur() {
            assertClose(BigNum.of(5), BigNum.of(2).add(BigNum.of(3)));
            assertClose(BigNum.of(1.5, 100), BigNum.of(1, 100).add(BigNum.of(5, 99)));
        }

        @Test
        void retenueSurLExposant() {
            assertClose(BigNum.of(1.4, 51), BigNum.of(7, 50).add(BigNum.of(7, 50)));
        }

        @Test
        void auDelaDe1e308() {
            assertClose(BigNum.of(2, 5000), BigNum.of(1, 5000).add(BigNum.of(1, 5000)));
        }

        @Test
        void termeNegligeableIgnore() {
            BigNum big = BigNum.of(1, 100);
            assertSame(big, big.add(BigNum.of(1, 50)));
            assertSame(big, BigNum.of(1, 50).add(big));
        }

        @Test
        void zeroEstNeutre() {
            BigNum n = BigNum.of(3, 7);
            assertSame(n, n.add(BigNum.ZERO));
            assertSame(n, BigNum.ZERO.add(n));
        }

        @Test
        void soustraction() {
            assertClose(BigNum.of(7, 2), BigNum.of(1000).subtract(BigNum.of(300)));
            assertClose(BigNum.of(-7, 2), BigNum.of(300).subtract(BigNum.of(1000)));
            assertEquals(BigNum.ZERO, BigNum.of(1, 40).subtract(BigNum.of(1, 40)));
        }

        @Test
        void soustractionAvecPerteDeChiffres() {
            // 1000 - 999 = 1 : la mantisse 0.001 doit être renormalisée
            assertClose(BigNum.ONE, BigNum.of(1000).subtract(BigNum.of(999)));
        }
    }

    @Nested
    class MultiplicationEtDivision {

        @Test
        void multiplication() {
            assertClose(BigNum.of(6, 300), BigNum.of(2, 100).multiply(BigNum.of(3, 200)));
            assertClose(BigNum.of(2.5, 1001), BigNum.of(5, 500).multiply(BigNum.of(5, 500)));
            assertClose(BigNum.of(-6, 3), BigNum.of(-20).multiply(BigNum.of(300)));
        }

        @Test
        void multiplicationParUnDouble() {
            assertClose(BigNum.of(5, 98), BigNum.of(1, 100).multiply(0.05));
            assertEquals(BigNum.ZERO, BigNum.of(1, 100).multiply(0));
        }

        @Test
        void multiplicationParZero() {
            assertEquals(BigNum.ZERO, BigNum.of(1, 100).multiply(BigNum.ZERO));
            assertEquals(BigNum.ZERO, BigNum.ZERO.multiply(BigNum.of(1, 100)));
        }

        @Test
        void division() {
            assertClose(BigNum.of(2, 100), BigNum.of(6, 300).divide(BigNum.of(3, 200)));
            assertClose(BigNum.of(5, -1), BigNum.ONE.divide(BigNum.of(2)));
            assertClose(BigNum.of(2.5, 9), BigNum.of(1, 10).divide(4));
            assertEquals(BigNum.ZERO, BigNum.ZERO.divide(BigNum.of(3)));
        }

        @Test
        void divisionParZero() {
            assertThrows(ArithmeticException.class, () -> BigNum.ONE.divide(BigNum.ZERO));
            assertThrows(ArithmeticException.class, () -> BigNum.ONE.divide(0));
        }
    }

    @Nested
    class Puissances {

        @Test
        void puissancesEntieres() {
            assertClose(BigNum.of(1024), BigNum.of(2).pow(10));
            assertClose(BigNum.of(1, 300), BigNum.of(1, 100).pow(3));
            assertClose(BigNum.of(8, 30), BigNum.of(2, 10).pow(3));
        }

        @Test
        void resultatAuDelaDe1e308() {
            // 2^2000 = 1.148130695274254e602
            assertClose(BigNum.of(1.148130695274254, 602), BigNum.of(2).pow(2000));
            assertClose(BigNum.of(1, 1_000_000), BigNum.TEN.pow(1_000_000));
        }

        @Test
        void puissancesNonEntieres() {
            assertClose(BigNum.of(Math.sqrt(10), 0), BigNum.TEN.pow(0.5));
            assertClose(BigNum.of(Math.sqrt(10), -8), BigNum.of(1, 5).pow(-1.5));
            assertClose(BigNum.of(1, 50), BigNum.of(1, 100).sqrt());
            assertClose(BigNum.of(3, 50), BigNum.of(9, 100).sqrt());
            assertClose(BigNum.of(Math.sqrt(90), 50), BigNum.of(9, 101).sqrt());
        }

        @Test
        void casParticuliers() {
            assertEquals(BigNum.ONE, BigNum.of(5, 20).pow(0));
            assertEquals(BigNum.ONE, BigNum.ZERO.pow(0));
            assertEquals(BigNum.ZERO, BigNum.ZERO.pow(3));
            assertThrows(ArithmeticException.class, () -> BigNum.ZERO.pow(-1));
        }

        @Test
        void baseNegative() {
            assertClose(BigNum.of(-8), BigNum.of(-2).pow(3));
            assertClose(BigNum.of(4), BigNum.of(-2).pow(2));
            assertThrows(ArithmeticException.class, () -> BigNum.of(-2).pow(0.5));
            assertThrows(ArithmeticException.class, () -> BigNum.of(-4).sqrt());
        }

        @Test
        void pow10() {
            assertClose(BigNum.of(1, 500), BigNum.pow10(500));
            assertClose(BigNum.of(Math.sqrt(10), 2), BigNum.pow10(2.5));
            assertClose(BigNum.of(Math.sqrt(10), -3), BigNum.pow10(-2.5));
        }

        @Test
        void log10() {
            assertEquals(100, BigNum.of(1, 100).log10(), 1e-12);
            assertEquals(5000 + Math.log10(2.5), BigNum.of(2.5, 5000).log10(), 1e-9);
            assertEquals(Double.NEGATIVE_INFINITY, BigNum.ZERO.log10());
            assertTrue(Double.isNaN(BigNum.of(-1).log10()));
        }

        @Test
        void coutExponentielDUnIdle() {
            // coût = base × multiplicateur^achats, la formule type d'un idle
            BigNum cost = BigNum.of(10).multiply(BigNum.of(1.15).pow(5000));
            assertEquals(1 + 5000 * Math.log10(1.15), cost.log10(), 1e-9);
        }
    }

    @Nested
    class Comparaison {

        @Test
        void ordreDesPositifs() {
            assertTrue(BigNum.of(2, 10).gt(BigNum.of(9, 9)));
            assertTrue(BigNum.of(3, 10).gt(BigNum.of(2, 10)));
            assertTrue(BigNum.of(1, 400).gt(BigNum.of(9.99, 399)));
            assertTrue(BigNum.of(0.5).lt(BigNum.ONE));
        }

        @Test
        void zeroEtPetitsNombres() {
            // 0.5 a un exposant de -1, inférieur à celui de zéro : le signe doit primer
            assertTrue(BigNum.of(0.5).gt(BigNum.ZERO));
            assertTrue(BigNum.of(1, -500).gt(BigNum.ZERO));
            assertTrue(BigNum.of(-1, -500).lt(BigNum.ZERO));
        }

        @Test
        void ordreDesNegatifs() {
            assertTrue(BigNum.of(-1, 10).lt(BigNum.of(-1, 5)));
            assertTrue(BigNum.of(-3).lt(BigNum.of(-2)));
            assertTrue(BigNum.of(-1, 100).lt(BigNum.of(1, -100)));
        }

        @Test
        void egalite() {
            assertEquals(0, BigNum.of(1500).compareTo(BigNum.of(1.5, 3)));
            assertEquals(BigNum.of(1500), BigNum.of(1.5, 3));
            assertTrue(BigNum.of(5).gte(BigNum.of(5)));
            assertTrue(BigNum.of(5).lte(BigNum.of(5)));
            assertFalse(BigNum.of(5).gt(BigNum.of(5)));
            assertFalse(BigNum.of(5).lt(BigNum.of(5)));
        }

        @Test
        void minEtMax() {
            BigNum small = BigNum.of(1, 10);
            BigNum big = BigNum.of(1, 20);
            assertSame(big, small.max(big));
            assertSame(small, small.min(big));
        }

        @Test
        void signeEtValeurAbsolue() {
            assertEquals(1, BigNum.of(3).sign());
            assertEquals(0, BigNum.ZERO.sign());
            assertEquals(-1, BigNum.of(-3).sign());
            assertEquals(BigNum.of(3, 9), BigNum.of(-3, 9).abs());
            assertEquals(BigNum.of(-3, 9), BigNum.of(3, 9).negate());
            assertEquals(BigNum.ZERO, BigNum.ZERO.negate());
        }
    }

    @Nested
    class ConversionEtAffichage {

        @Test
        void versDouble() {
            assertEquals(1500, BigNum.of(1.5, 3).toDouble(), 1e-9);
            assertEquals(0.025, BigNum.of(2.5, -2).toDouble(), 1e-15);
            assertEquals(Double.POSITIVE_INFINITY, BigNum.of(1, 309).toDouble());
            assertEquals(Double.NEGATIVE_INFINITY, BigNum.of(-1, 309).toDouble());
            assertEquals(0, BigNum.of(1, -400).toDouble());
        }

        @ParameterizedTest
        @CsvSource({
                "0,        0,     0.00",
                "1,        0,     1.00",
                "1.5,      1,     15.00",
                "9.9999,   2,     999.99",
                "9.99999,  2,     1.00e3",
                "1,        3,     1.00e3",
                "1.234,    45,    1.23e45",
                "9.999,    45,    1.00e46",
                "2.5,      5000,  2.50e5000",
                "5,        -1,    0.50",
                "1,        -9,    0.00",
                "-1,       -9,    0.00",
                "-2.5,     1,     -25.00",
                "-1.5,     12,    -1.50e12",
        })
        void format(double mantissa, long exponent, String expected) {
            assertEquals(expected, BigNum.of(mantissa, exponent).format());
        }

        @ParameterizedTest
        @CsvSource({"0, 0", "1.5, 3", "-7.25, 12", "3.141592653589793, 123456789", "2, -40"})
        void toStringPuisParseRedonneLeMemeNombre(double mantissa, long exponent) {
            BigNum original = BigNum.of(mantissa, exponent);
            assertEquals(original, BigNum.parse(original.toString()));
        }

        @Test
        void parseAccepteLesEcrituresCourantes() {
            assertClose(BigNum.of(1.5, 3), BigNum.parse("1500"));
            assertClose(BigNum.of(2.5, 30), BigNum.parse("2.5e30"));
            assertClose(BigNum.of(1, 5000), BigNum.parse(" 1E5000 "));
            assertThrows(NumberFormatException.class, () -> BigNum.parse("abc"));
        }
    }

    @Nested
    class Simulation {

        @Test
        void croissanceExponentielleSurUneLonguePartie() {
            // ×1.1 par tick pendant 100 000 ticks : très au-delà de la limite des double
            BigNum amount = BigNum.ONE;
            for (int i = 0; i < 100_000; i++) {
                amount = amount.multiply(1.1);
            }
            assertEquals(100_000 * Math.log10(1.1), amount.log10(), 1e-6);
            assertTrue(amount.gt(BigNum.of(1, 4000)));
        }

        @Test
        void accumulationParTicks() {
            // 20 ticks/s pendant 60 s à 1e50/s → 6e51
            BigNum production = BigNum.of(1, 50);
            BigNum amount = BigNum.ZERO;
            double dt = 0.05;
            for (int i = 0; i < 20 * 60; i++) {
                amount = amount.add(production.multiply(dt));
            }
            assertClose(BigNum.of(6, 51), amount);
        }
    }
}
