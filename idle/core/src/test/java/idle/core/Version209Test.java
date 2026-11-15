package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import org.junit.jupiter.api.Test;

/** Version 0.209 : Noyaux sombres avant tout achat en matière noire, et l'effet d'une bulle coupé par une mesure. */
class Version209Test {

    private static Game gameWithDarkMatter(int darkMatter) {
        Game game = new Game();
        game.start();
        game.state().setDarkMatter(BigNum.of(darkMatter));
        game.state().setExplosions(1);
        game.tick(1);
        return game;
    }

    @Test
    void lesAchatsEnMatiereNoireAttendentNoyauxSombres() {
        Game game = gameWithDarkMatter(10);
        assertTrue(game.isDarkAvailable(Game.KEY_DARK_UPGRADE));
        for (DarkUpgrade dark : game.darkUpgrades()) {
            if (dark.branch() != DarkUpgrade.Branch.DARK_MATTER || dark.id().equals(Game.KEY_DARK_UPGRADE)) continue;
            assertFalse(game.isDarkAvailable(dark.id()), dark.id());
            assertFalse(game.buyDark(dark.id()), dark.id());
            assertTrue(game.darkWaitsForKey(dark.id()), dark.id());
        }
        assertFalse(game.darkWaitsForKey("dark_density"), "l'arbre payé en particules n'attend pas");
        assertTrue(game.buyDark(Game.KEY_DARK_UPGRADE));
        assertTrue(game.buyDark("dark_shop_cadence"));
        assertFalse(game.darkWaitsForKey("dark_shop_cadence"));
    }

    @Test
    void uneAncienneSauvegardeGardeSesNiveaux() {
        Game game = gameWithDarkMatter(10);
        game.state().setDarkLevel("dark_shop_cadence", 3);
        assertEquals(3, game.darkLevelOf("dark_shop_cadence"));
        assertFalse(game.canBuyDark("dark_shop_cadence"), "le niveau suivant attend Noyaux sombres");
        assertTrue(game.buyDark(Game.KEY_DARK_UPGRADE));
        assertTrue(game.canBuyDark("dark_shop_cadence"));
    }

    @Test
    void lAutomatismeDeLArbrePrendNoyauxSombresEnPremier() {
        Game game = gameWithDarkMatter(1);
        GameTestSupport.bangs(game, 1);
        game.state().setDarkMatter(BigNum.of(1, 9));
        assertTrue(game.setDarkAutomationEnabled("dark_auto_tree", true));
        game.tick(1.05);
        assertEquals(1, game.darkLevelOf(Game.KEY_DARK_UPGRADE));
    }

    @Test
    void uneMesurePeutCouperLEffetDUneBulleAchetee() {
        Game game = GameTestSupport.universe(2);
        int cap = game.maxGeneratorCount();
        game.state().setTreeBubbleLevel(GameUniverse.TreeBubble.YOUNG_RIGHT.ordinal(), 1);
        assertTrue(game.maxGeneratorCount() > cap, "la bulle lève le plafond");
        int bought = game.treeBubblesBought();
        game.state().testBlock("bulle:YOUNG_RIGHT");
        assertEquals(cap, game.maxGeneratorCount(), "effet coupé : le plafond reste");
        assertTrue(game.isTreeBubbleBought(GameUniverse.TreeBubble.YOUNG_RIGHT), "elle reste achetée");
        assertEquals(bought, game.treeBubblesBought(), "et compte pour les skins de bulles");
    }
}
