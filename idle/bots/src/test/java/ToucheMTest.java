import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Version 0.206 : tenir la touche M ne demande pas de décider vite. Le Speedrunner des audits (une décision toutes les
 * 20 s, attention entière) la tient, par morceaux de deux secondes ; un profil moins attentif ou plus lent ne la tient pas.
 */
class ToucheMTest {

    private static IdleProfiles24 bot(int profil) {
        return new IdleProfiles24(Bots.fresh(201), IdleProfiles24.PROFILES[profil - 1], 201, 20);
    }

    @Test
    void leSpeedrunnerTientMSansDeciderVite() {
        IdleProfiles24 speedrunner = bot(1);
        assertEquals(20, IdleProfiles24.PROFILES[0].actionEverySeconds());
        assertTrue(speedrunner.holdsBuyAll(), "une décision toutes les 20 s, attention entière : M tenue");
        assertEquals(IdleProfiles24.HOLD_STEP_SLOW, speedrunner.holdStep(), "des morceaux de deux secondes, pas d'une demi-seconde");
        assertEquals(2.0, IdleProfiles24.HOLD_STEP_SLOW);
    }

    @Test
    void unProfilMoinsAttentifOuPlusLentNeLaTientPas() {
        assertFalse(bot(3).holdsBuyAll(), "le Régulier : une décision par minute");
        assertFalse(bot(13).holdsBuyAll(), "l'acheteur en séries : attention entière, mais une décision toutes les 5 min");
    }

    @Test
    void laMecaniqueCoupeeLaFaitLacher() {
        IdleProfiles24 speedrunner = bot(1);
        speedrunner.sansMecaniques.add("appui_m");
        assertFalse(speedrunner.holdsBuyAll());
    }
}
