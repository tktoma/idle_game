package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.bigbang.BangChallenge;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import org.junit.jupiter.api.Test;

/**
 * Version 0.166 : les réglages tirés du bilan des mesures. La maturation du deuxième univers ×5 au lieu de ×128 (×4 depuis la 0.171), les
 * bulles qui se rachètent ×2,5 par niveau, et la réserve du tout premier Big Bang.
 */
class ReglagesTest {

    @Test
    void lesTroisDefisEtLaBarreHauteFontMurirQuatreFoisPlusVite() {
        Game game = DeuxiemeUniversTest.second();
        Body supermassif = null;
        for (Body b : Bodies.DEFAULT) if (supermassif == null || game.maturationSeconds(b) > game.maturationSeconds(supermassif)) supermassif = b;
        double avant = game.maturationSeconds(supermassif);
        assertEquals(360 * 60, avant, 1e-6, "le plus long des astres : 6 h sans défi");
        for (BangChallenge c : new BangChallenge[] {BangChallenge.VOID, BangChallenge.FORGOTTEN, BangChallenge.RAISED}) {
            game.state().addCompletedBangChallenge(c.name());
        }
        // Trois défis réussis, dont la barre haute : (2^(1/3))^3 pour l'univers (version 0.171 ; 2,5^(1/3) en 0.166), ×2 pour sa récompense.
        assertEquals(90 * 60, game.maturationSeconds(supermassif), 1e-6, "trois défis et la barre haute : ×4, 90 minutes");
        game.state().addCompletedBangChallenge(BangChallenge.BY_HAND.name());
        assertEquals(avant / Math.pow(Game.BANG_UNIVERSE_MATURATION, 4) / 2, game.maturationSeconds(supermassif), 1e-6, "un quatrième défi");
    }

    @Test
    void lesBullesQuiSeRachetentCoutentDeuxFoisEtDemiePlusAChaqueNiveau() {
        assertEquals(2.5, TreeBubble.LEVEL_GROWTH, 0);
        assertEquals(TreeBubble.LEAVES_LEFT.cost() * 2.5 * 2.5, TreeBubble.LEAVES_LEFT.costAt(2), 1e-9);
        assertEquals(TreeBubble.GIANT_LEFT_HIGH.cost() * Math.pow(2.5, 4), TreeBubble.GIANT_LEFT_HIGH.costAt(4), 1e-9);
    }

    @Test
    void seulLeToutPremierBigBangDemandeUneGrandeReserve() {
        Game game = GameTestSupport.realGame();
        assertEquals(Game.FIRST_BIG_BANG_DARK_MATTER.toDouble(), game.bigBangDarkMatter().toDouble(), 1e-9, "univers 1, aucun Big Bang");
        assertEquals(42_000, Game.FIRST_BIG_BANG_DARK_MATTER.toDouble(), 1e-9, "version 0.181 : 42 000 (120 000 avant)");
        game.state().setBigBangs(1);
        assertEquals(500, game.bigBangDarkMatter().toDouble(), 1e-9, "le deuxième Big Bang");
        game.state().setBigBangs(0);
        game.state().setUniverseFormed(true);
        assertEquals(500, game.bigBangDarkMatter().toDouble(), 1e-9, "le premier Big Bang des univers suivants");
        game.state().setUniverseFormed(false);
        game.state().setActiveBangChallenge(BangChallenge.RAISED.name());
        assertEquals(84_000, game.bigBangDarkMatter().toDouble(), 1e-9, "la barre haute double toujours");
        assertTrue(Game.FIRST_BIG_BANG_DARK_MATTER.gt(Game.BIG_BANG_DARK_MATTER));
    }
}
