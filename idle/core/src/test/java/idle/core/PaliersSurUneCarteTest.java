package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.universe.DimensionAutomation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.268 : les cinq paliers de celui qui joue une dimension s'achètent sur une seule carte. Le jeu dit le palier
 * acheté le plus rapide, le prochain à acheter, et met en pause ou relance tous les paliers ensemble ; un achat ne relance
 * pas une carte en pause.
 */
class PaliersSurUneCarteTest {
    private static Game four() {
        Game game = GameTestSupport.universe(4);
        GameTestSupport.openDimensionAutomations(game, 4);
        game.state().setUniverseSeeds(BigNum.of(100));
        game.state().setPlaneShards(1e30);
        return game;
    }

    @Test
    void lesPaliersVontDuPlusLentAuPlusRapide() {
        assertEquals(List.of(DimensionAutomation.PLANE_AIM_5S, DimensionAutomation.PLANE_AIM_2S, DimensionAutomation.PLANE_AIM_1S,
                DimensionAutomation.PLANE_AIM_05S, DimensionAutomation.PLANE_AIM), DimensionAutomation.playTiers(2));
        assertTrue(DimensionAutomation.playTiers(1).isEmpty(), "la ligne n'a rien à jouer");
        for (int dimension = 2; dimension <= 10; dimension++) assertEquals(5, DimensionAutomation.playTiers(dimension).size(), "dimension " + dimension);
    }

    @Test
    void laCarteAcheteLeSuivantEtCoupeTousLesPaliers() {
        Game game = four();
        assertNull(game.ownedPlayTier(2));
        assertEquals(DimensionAutomation.PLANE_AIM_5S, game.nextPlayTier(2));
        assertFalse(game.buyNextPlayTier(2), "pas avant les améliorations");
        AutomatisationDimensionsTest.buyUpTo(game, DimensionAutomation.PLANE_SIZE);
        assertFalse(game.setDimensionPlayEnabled(2, false), "rien d'acheté, rien à couper");

        assertTrue(game.buyNextPlayTier(2));
        assertEquals(DimensionAutomation.PLANE_AIM_5S, game.ownedPlayTier(2));
        assertEquals(DimensionAutomation.PLANE_AIM_2S, game.nextPlayTier(2));
        assertTrue(game.isDimensionPlayEnabled(2), "acheté, il joue");
        assertTrue(game.buyNextPlayTier(2));
        assertEquals(DimensionAutomation.PLANE_AIM_2S, game.dimensionPlayTier(2), "le plus rapide joue");

        assertTrue(game.setDimensionPlayEnabled(2, false));
        assertFalse(game.isDimensionPlayEnabled(2));
        assertNull(game.dimensionPlayTier(2), "en pause : aucun palier ne joue");
        assertTrue(game.buyNextPlayTier(2));
        assertFalse(game.isDimensionPlayEnabled(2), "un achat ne relance pas une carte en pause");
        assertNull(game.dimensionPlayTier(2));

        assertTrue(game.setDimensionPlayEnabled(2, true));
        assertEquals(DimensionAutomation.PLANE_AIM_1S, game.dimensionPlayTier(2));
        assertTrue(game.buyNextPlayTier(2) && game.buyNextPlayTier(2));
        assertEquals(DimensionAutomation.PLANE_AIM, game.ownedPlayTier(2));
        assertNull(game.nextPlayTier(2), "les cinq achetés");
        assertFalse(game.buyNextPlayTier(2));
        assertEquals(DimensionAutomation.PLANE_AIM, game.dimensionPlayTier(2));
    }
}
