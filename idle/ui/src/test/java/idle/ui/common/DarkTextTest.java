package idle.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import idle.core.darkmatter.DarkEffect;
import org.junit.jupiter.api.Test;

/** Version 0.126 : les crans des isotopes se lisent dans le catalogue. Sans affichage JavaFX. */
class DarkTextTest {

    @Test
    void lesCransDesIsotopesSuiventChaqueFamille() {
        assertEquals("9 → 16 → 25 (4 → 9 → 16 pour les lanthanides)", DarkText.copiesLadder(new DarkEffect.IncreaseMaxCopies(1)));
    }
}
