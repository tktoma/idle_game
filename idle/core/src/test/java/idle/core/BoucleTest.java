package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameLoop.LoopHit;
import idle.core.GameLoop.LoopUpgrade;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.153 : la boucle, cinquième dimension. Un point tourne sur chaque cercle ; un clic dans une zone rapporte des
 * nœuds, selon la part touchée (bord, « bien », centre), puis le cercle attend un instant. Et le rond de croissance,
 * près de la graine, qui accélère l'arrosage pour un temps.
 */
class BoucleTest {

    private static Game five() {
        return GameTestSupport.universe(5);
    }

    @Test
    void laBoucleSOuvreAuCinquiemeUnivers() {
        assertFalse(GameTestSupport.universe(4).isLoopOpen());
        Game game = five();
        assertTrue(game.isLoopOpen());
        assertEquals("nœuds", Game.dimensionResourceName(5));
        assertEquals(1, game.loopRings());
        assertEquals(1, game.loopZoneCount());
        assertEquals(20, 1 / game.loopTurnsPerSecond(), 1e-9, "très lente au début : un tour en vingt secondes");
    }

    @Test
    void unClicRapporteSelonLaPartTouchee() {
        Game game = five();
        double half = game.loopZoneHalfWidth();
        game.setLoopZone(0, 0, 0.5);
        game.setLoopPhase(0, 0.5);
        assertEquals(LoopHit.EXCELLENT, game.loopAim(0));
        game.setLoopPhase(0, 0.5 + half * 0.4);
        assertEquals(LoopHit.GOOD, game.loopAim(0));
        game.setLoopPhase(0, 0.5 - half * 0.9);
        assertEquals(LoopHit.BAD, game.loopAim(0));
        game.setLoopPhase(0, 0.5 + half * 1.5);
        assertEquals(LoopHit.MISS, game.loopAim(0));
        // Au centre : trois nœuds ; la zone touchée part ailleurs, et le cercle attend.
        game.setLoopPhase(0, 0.5);
        assertEquals(LoopHit.EXCELLENT, game.clickLoop(0));
        assertEquals(3, game.state().loopShards(), 1e-9);
        assertTrue(Math.abs(game.loopZones(0).get(0) - 0.5) > half, "la zone s'est posée ailleurs");
        assertEquals(LoopHit.WAIT, game.clickLoop(0), "cliquer sans arrêt ne compte pas");
        game.tick(GameLoop.LOOP_COOLDOWN + 0.01);
        assertFalse(game.loopAim(0) == LoopHit.WAIT);
    }

    @Test
    void unClicAcoteFaitAttendreAussi() {
        Game game = five();
        game.setLoopZone(0, 0, 0.5);
        game.setLoopPhase(0, 0.0);
        assertEquals(LoopHit.MISS, game.clickLoop(0));
        assertEquals(0, game.state().loopShards(), 0);
        assertTrue(game.loopWait(0) > 0, "le matraquage ne paie pas");
    }

    @Test
    void lesAmeliorationsChangentLaBoucle() {
        Game game = five();
        game.state().setLoopShards(1e12);
        for (LoopUpgrade upgrade : LoopUpgrade.values()) assertTrue(game.buyLoop(upgrade), upgrade.toString());
        assertEquals(2, game.loopRings());
        assertEquals(2, game.loopZoneCount());
        assertEquals(GameLoop.LOOP_TURNS * GameLoop.LOOP_SPEED_STEP, game.loopTurnsPerSecond(), 1e-12);
        assertEquals(GameLoop.LOOP_ZONE_HALF * 1.12, game.loopZoneHalfWidth(), 1e-12);
        assertEquals(2 * 3 * 1.25, game.loopReward(LoopHit.EXCELLENT), 1e-9, "gain 2, centre ×3, précision +25 %");
        game.state().setLoopLevel(LoopUpgrade.RINGS.ordinal(), 99);
        assertEquals(GameLoop.MAX_RINGS, game.loopRings(), "sept cercles au plus");
        game.state().setLoopLevel(LoopUpgrade.RINGS.ordinal(), LoopUpgrade.RINGS.max());
        assertFalse(game.canBuyLoop(LoopUpgrade.RINGS));
    }

    @Test
    void lAutomatismeCliqueBienAChaquePassage() {
        Game game = five();
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.LOOP_PLAY.name());
        game.tick(200);
        // 200 s, un tour en 20 s, une zone : dix passages, dix « bien ».
        assertEquals(10 * game.loopReward(LoopHit.GOOD), game.state().loopShardsEarned(), 1e-6);
        assertEquals(29, DimensionAutomation.values().length, "sept de plus pour la boucle");
        assertEquals(4, DimensionAutomation.LOOP_PLAY.paidWith(), "payés en instants de la durée");
    }

    @Test
    void leRondDeCroissanceAccelereLArrosage() {
        Game game = five();
        assertTrue(game.isBloomUnlocked());
        double rate = game.wateringCanRate();
        game.testShowBloom();
        assertTrue(game.isBloomVisible());
        assertTrue(game.catchBloom());
        assertFalse(game.isBloomVisible());
        assertEquals(rate * 1.25, game.wateringCanRate(), 1e-9, "+25 %");
        assertEquals(30, game.bloomActiveLeft(), 1e-9);
        game.state().setSeedUpgradeLevel(SeedUpgrade.BLOOM_POWER.ordinal(), 2);
        game.state().setSeedUpgradeLevel(SeedUpgrade.BLOOM_DURATION.ordinal(), 3);
        assertEquals(rate * 1.45, game.wateringCanRate(), 1e-9, "+25 % et deux fois 10 points");
        assertEquals(60, game.bloomSeconds(), 1e-9);
        game.tick(31);
        assertEquals(rate, game.wateringCanRate(), 1e-9, "fini");
        // Il revient seul, au bout de deux minutes environ, et repart s'il n'est pas cueilli.
        boolean seen = false;
        for (int i = 0; i < 400 && !seen; i++) {
            game.tick(1);
            seen = game.isBloomVisible();
        }
        assertTrue(seen);
        game.tick(GameLoop.BLOOM_VISIBLE + 1);
        assertFalse(game.isBloomVisible());
        assertFalse(GameTestSupport.universe(4).isBloomUnlocked(), "pas avant la boucle");
    }

    @Test
    void laBoucleTraverseLesUniversEtSeSauve() throws Exception {
        Game game = five();
        game.state().setLoopShards(1e9);
        assertTrue(game.buyLoop(LoopUpgrade.GAIN));
        game.state().setLoopShardsEarned(GameUniverse.LOOP_THRESHOLD);
        assertTrue(game.isDimensionThresholdReached(5));
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.LOOP_DANCER));
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(1, back.loopLevel(LoopUpgrade.GAIN.ordinal()));
        assertEquals(game.state().loopShards(), back.loopShards(), 1e-6);
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("loop")) old.append(line).append('\n');
        assertEquals(0, SaveCodec.read(old.toString()).state().loopShards(), 0, "une sauvegarde d'avant part de zéro");
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertEquals(1, game.loopLevel(LoopUpgrade.GAIN), "comme les autres dimensions, elle traverse les univers");
    }

    @Test
    void cueillirLeRondRelanceSonAttente() {
        Game game = five();
        int caught = 0;
        for (int second = 0; second < 1_200; second++) {
            game.tick(1);
            if (game.catchBloom()) caught++;
        }
        assertTrue(caught >= 7 && caught <= 15, "une dizaine en vingt minutes : " + caught);
    }
}
