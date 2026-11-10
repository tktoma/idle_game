package idle.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.ui.Settings;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Version 0.200 : les six notations nouvelles, sur 0, 1, 1 234, 1e6, 1e21, 1e85000 et une durée. Sans affichage. */
class NotationsTest {

    private static final List<BigNum> SAMPLES = List.of(BigNum.of(0), BigNum.of(1), BigNum.of(1234), BigNum.of(1, 6), BigNum.of(1, 21), BigNum.of(1, 85_000));
    private static final List<Settings.Notation> NEW = List.of(Settings.Notation.BINARY, Settings.Notation.HEX, Settings.Notation.JAPANESE,
            Settings.Notation.MORSE, Settings.Notation.SI, Settings.Notation.HEARTBEATS);

    @AfterEach
    void scientifique() {
        Format.setNotation(Settings.Notation.SCIENTIFIC);
    }

    @Test
    void chaqueNotationNouvelleEstADebloquerEtResteLisible() {
        assertEquals(14, Settings.Notation.values().length);
        Format.setNotation(Settings.Notation.SCIENTIFIC);
        List<String> classic = SAMPLES.stream().map(Format::big).toList();
        for (Settings.Notation notation : NEW) {
            assertTrue(notation.unlock() != null, notation + " se débloque");
            Format.setNotation(notation);
            for (int i = 0; i < SAMPLES.size(); i++) {
                String text = Format.big(SAMPLES.get(i));
                assertFalse(text.isBlank(), notation + " sur " + SAMPLES.get(i));
                assertTrue(text.length() <= 60, notation + " reste court sur " + SAMPLES.get(i) + " : " + text);
                // En dessous de mille, toutes les notations écrivent pareil ; au-dessus, la nouvelle (hors battements) change l'écriture.
                // Les préfixes SI s'arrêtent au quetta (10^30) : au-delà, c'est la scientifique, comme l'annonce le skin.
                boolean repli = notation == Settings.Notation.SI && !SAMPLES.get(i).lt(BigNum.of(1, 33));
                if (i < 2 || notation == Settings.Notation.HEARTBEATS || repli) assertEquals(classic.get(i), text, notation + " sur " + SAMPLES.get(i));
                else assertFalse(classic.get(i).equals(text), notation + " doit changer " + SAMPLES.get(i));
            }
        }
    }

    @Test
    void binaireEtHexadecimal() {
        Format.setNotation(Settings.Notation.BINARY);
        assertEquals("1.21×2^10", Format.big(BigNum.of(1234)));
        assertEquals("1.91×2^19", Format.big(BigNum.of(1, 6)));
        assertEquals("1.00×2^10", Format.big(BigNum.of(1024)));
        assertTrue(Format.big(BigNum.of(1, 85_000)).endsWith("×2^282 363"), Format.big(BigNum.of(1, 85_000)));
        Format.setNotation(Settings.Notation.HEX);
        assertEquals("0x4D2", Format.big(BigNum.of(1234)));
        assertEquals("0xF4240", Format.big(BigNum.of(1, 6)));
        assertTrue(Format.big(BigNum.of(1, 21)).matches("\\d\\.\\d\\d×16\\^17"), Format.big(BigNum.of(1, 21)));
        assertTrue(Format.big(BigNum.of(1, 85_000)).endsWith("×16^70 590"), Format.big(BigNum.of(1, 85_000)));
    }

    @Test
    void chiffresJaponaisMorseEtPrefixesSI() {
        Format.setNotation(Settings.Notation.JAPANESE);
        assertEquals("1 234", Format.big(BigNum.of(1234)));
        assertEquals("100万", Format.big(BigNum.of(1, 6)));
        assertEquals("10.0垓", Format.big(BigNum.of(1, 21)), "trois chiffres, comme partout");
        assertEquals("1.00無量大数", Format.big(BigNum.of(1, 68)));
        assertEquals("1.00×10^85000", Format.big(BigNum.of(1, 85_000)), "au-delà du 無量大数, le repli");
        Format.setNotation(Settings.Notation.MORSE);
        assertEquals("·−−−− ·−·−·− ··−−− ···−− e3", Format.big(BigNum.of(1234)));
        assertEquals("·−−−− ·−·−·− −−−−− −−−−− e85000", Format.big(BigNum.of(1, 85_000)));
        Format.setNotation(Settings.Notation.SI);
        assertEquals("1.23 k", Format.big(BigNum.of(1234)));
        assertEquals("1.00 M", Format.big(BigNum.of(1, 6)));
        assertEquals("1.00 Z", Format.big(BigNum.of(1, 21)));
        assertEquals("1.00 Q", Format.big(BigNum.of(1, 30)));
        assertEquals(BigNum.of(1, 33).format(), Format.big(BigNum.of(1, 33)), "au-delà du quetta, la scientifique");
    }

    @Test
    void lesBattementsDeCoeurComptentLesDurees() {
        Format.setNotation(Settings.Notation.SCIENTIFIC);
        assertEquals("1 h 00 min", Format.duration(3600));
        Format.setNotation(Settings.Notation.HEARTBEATS);
        assertEquals("3 600 battements", Format.duration(3600));
        assertEquals("1 battement", Format.duration(1.2));
        assertEquals("0 battement", Format.duration(0));
        assertEquals("43 battements", Format.wait(42.2), "l'attente arrondit à la seconde supérieure");
        assertEquals("1.23e6", Format.big(BigNum.of(1.23, 6)), "les nombres restent en scientifique");
        Format.setNotation(Settings.Notation.MORSE);
        assertEquals("1 h 00 min", Format.duration(3600), "les autres notations ne touchent pas aux durées");
    }
}
