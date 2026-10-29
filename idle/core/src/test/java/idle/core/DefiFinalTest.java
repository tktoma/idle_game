package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Version 0.160 : le défi final. La bulle des racines achetée, chaque dimension a un palier à atteindre ; la
 * progression se compte en puissances de dix. Les dix paliers atteints, la partie est finie.
 */
class DefiFinalTest {

    private static Game ten() {
        Game game = GameTestSupport.universe(10);
        game.state().setTreeBubbleBought(TreeBubble.ROOTS.ordinal(), true);
        return game;
    }

    private static void reachAll(Game game) {
        game.state().setDimensionShardsEarned(Game.finalGoal(1));
        game.state().setPlaneShardsEarned(Game.finalGoal(2));
        game.state().setVolumeShardsEarned(Game.finalGoal(3));
        game.state().setDurationShardsEarned(Game.finalGoal(4));
        game.state().setLoopShardsEarned(Game.finalGoal(5));
        game.state().setBraneShardsEarned(Game.finalGoal(6));
        game.state().setClickerShardsEarned(Game.finalGoal(7));
        game.state().setGridShardsEarned(Game.finalGoal(8));
        game.state().setMirrorShardsEarned(Game.finalGoal(9));
        game.state().setMixShardsEarned(Game.finalGoal(10));
    }

    @Test
    void laBulleDesRacinesOuvreLeDefi() {
        Game game = GameTestSupport.universe(10);
        assertFalse(game.isFinalChallengeOpen());
        game.state().setTreeBubbleBought(TreeBubble.ROOTS.ordinal(), true);
        assertTrue(game.isFinalChallengeOpen());
        assertFalse(TreeBubble.ROOTS.effect().contains("à venir"));
        for (int d = 1; d <= 9; d++) assertTrue(Game.finalGoal(d) > 0, "un palier par dimension");
        assertEquals(1e7, Game.finalGoal(10), 0);
    }

    @Test
    void laProgressionSeCompteEnPuissancesDeDix() {
        Game game = ten();
        game.state().setLoopShardsEarned(0);
        assertEquals(0, game.finalProgress(5), 1e-12);
        double goal = Game.finalGoal(5);
        game.state().setLoopShardsEarned(Math.sqrt(1 + goal) - 1);
        assertEquals(0.5, game.finalProgress(5), 1e-9, "la racine du palier : à mi-chemin");
        assertFalse(game.isFinalTierReached(5));
        game.state().setLoopShardsEarned(goal * 10);
        assertEquals(1, game.finalProgress(5), 0, "plafonnée à 1");
        assertTrue(game.isFinalTierReached(5));
    }

    @Test
    void lesDixPaliersFinissentLaPartie() throws Exception {
        Game game = ten();
        reachAll(game);
        game.state().setMixShardsEarned(Game.finalGoal(10) / 2);
        assertEquals(9, game.finalTiersReached());
        game.tick(0.1);
        assertFalse(game.isGameFinished());
        reachAll(game);
        assertTrue(game.isFinalChallengeComplete(), "les dix paliers");
        game.tick(0.1);
        assertTrue(game.isGameFinished(), "la partie est finie");
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.WORLD_ROOTS), "le titre");
        assertTrue(GameTestSupport.reread(game.state()).gameFinished(), "se sauve");
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("gameFinished")) old.append(line).append('\n');
        assertFalse(SaveCodec.read(old.toString()).state().gameFinished(), "une sauvegarde d'avant n'est pas finie");
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse(), "l'univers suivant");
        assertTrue(game.isGameFinished(), "un univers de plus ne la défait pas");
    }

    @Test
    void sansLaBulleRienNeFinit() {
        Game game = GameTestSupport.universe(10);
        reachAll(game);
        game.tick(0.1);
        assertFalse(game.isFinalChallengeComplete());
        assertFalse(game.isGameFinished());
    }
}
