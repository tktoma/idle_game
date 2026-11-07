package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.sky.Body;
import org.junit.jupiter.api.Test;

/**
 * Version 0.177 : le trou noir du vide, ouvert, raccourcit la maturation du trou noir et du trou noir supermassif de
 * 10 % par univers formé à partir du quatrième (durée ÷ (1 + 10 % × univers comptés)).
 */
class TrouNoirVideTest {

    /** Le vide ouvert, avec {@code formed} univers formés en tout. */
    private static Game withUniverses(int formed) {
        Game game = VideTest.open();
        while (game.universes() < formed) assertTrue(game.testAddUniverse());
        assertEquals(formed, game.universes());
        return game;
    }

    private static Body ofTier(Game game, Body.Tier tier) {
        for (Body body : game.bodies()) if (body.tier() == tier) return body;
        throw new AssertionError("pas d'astre " + tier);
    }

    @Test
    void avantLeQuatriemeUniversRienNeCompte() {
        Game game = withUniverses(3);
        game.state().setVoidHoleOpen(true);
        assertTrue(game.isVoidHoleOpen());
        assertEquals(0, game.voidHoleUniverses());
        assertEquals(1, game.voidHoleMaturationFactor(), 0);
        assertTrue(game.testAddUniverse());
        assertEquals(1, game.voidHoleUniverses(), "le quatrième univers formé compte");
        assertEquals(1.1, game.voidHoleMaturationFactor(), 1e-12);
    }

    @Test
    void dixPourCentParUniversCompteSurLesDeuxTrousNoirsSeulement() {
        Game game = withUniverses(6);
        Body hole = ofTier(game, Body.Tier.BLACK_HOLE), core = ofTier(game, Body.Tier.CORE), star = ofTier(game, Body.Tier.STAR);
        double holeBefore = game.maturationSeconds(hole), coreBefore = game.maturationSeconds(core), starBefore = game.maturationSeconds(star);
        game.state().setVoidHoleOpen(true);
        assertEquals(3, game.voidHoleUniverses());
        assertEquals(1.3, game.voidHoleMaturationFactor(), 1e-12);
        assertEquals(holeBefore / 1.3, game.maturationSeconds(hole), 1e-6);
        assertEquals(coreBefore / 1.3, game.maturationSeconds(core), 1e-6);
        assertEquals(starBefore, game.maturationSeconds(star), 1e-9, "les autres astres ne changent pas");
    }

    @Test
    void trouNoirFermeRienNeChange() {
        Game game = withUniverses(6);
        assertFalse(game.isVoidHoleOpen());
        assertEquals(1, game.voidHoleMaturationFactor(), 0);
        game.state().setVoidOpen(false);
        game.state().setVoidHoleOpen(true);
        assertFalse(game.isVoidHoleOpen(), "le vide fermé : pas de trou noir du vide");
        assertEquals(1, game.voidHoleMaturationFactor(), 0);
    }

    /** Le garde-fou du rythme : d'un univers à l'autre, le trou noir supermassif mûrit de plus en plus vite. */
    @Test
    void leSupermassifMuritPlusViteAChaqueUnivers() {
        Game game = withUniverses(4);
        game.state().setVoidHoleOpen(true);
        Body core = ofTier(game, Body.Tier.CORE);
        double first = game.maturationSeconds(core), previous = Double.MAX_VALUE;
        for (int n = 0; n < 5; n++) {
            double seconds = game.maturationSeconds(core);
            assertTrue(seconds < previous, game.universes() + " univers : " + seconds + " s, avant " + previous + " s");
            previous = seconds;
            assertTrue(game.testAddUniverse());
        }
        assertEquals(9, game.universes());
        assertEquals(1.6, game.voidHoleMaturationFactor(), 1e-12, "six univers comptés");
        assertEquals(first * 1.1 / 1.6, game.maturationSeconds(core), 1e-6);
    }

    /** Le trou noir qui ouvre celui du vide mûrit déjà : son reste raccourcit d'autant, les autres astres non. */
    @Test
    void leTrouNoirQuiMuritDejaRaccourcitALOuverture() {
        Game game = withUniverses(5);
        game.state().addBody("black_hole");
        game.state().setMaturing("black_hole", 1000);
        game.state().addBody("rocky_rubble");
        game.state().setMaturing("rocky_rubble", 1000);
        assertFalse(game.isVoidHoleOpen());
        game.tick(1);
        assertTrue(game.isVoidHoleOpen(), "le premier trou noir formé ouvre celui du vide");
        assertEquals(1000 / 1.2, game.maturationLeft("black_hole"), 1.5, "deux univers comptés : ÷1,2");
        assertEquals(1000, game.maturationLeft("rocky_rubble"), 1.5);
    }
}
