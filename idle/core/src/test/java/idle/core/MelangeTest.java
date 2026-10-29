package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameMix.MixRhythm;
import idle.core.GameMix.MixUpgrade;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.GameUniverse.TreeBubble;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.159 : le mélange refait. Un seul mini-jeu qui mêle, étape par étape, les mécaniques des dimensions : la
 * ligne d'abord, puis le plan, le volume… jusqu'au miroir. Chaque étape gagnée ouvre la suivante et remet à zéro les
 * améliorations du mélange. Et la dernière amélioration de la graine, qui ouvre la bulle des racines.
 */
class MelangeTest {

    private static Game stage(int stage) {
        Game game = GameTestSupport.universe(10);
        game.state().setMixStage(stage);
        game.testClearMix();
        return game;
    }

    private static void run(Game game, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) game.tick(0.05);
    }

    @Test
    void leMelangeSOuvreAuDixiemeUniversALaLigne() {
        assertFalse(GameTestSupport.universe(9).isMixOpen());
        Game game = stage(1);
        assertTrue(game.isMixOpen());
        assertEquals("fragments", Game.dimensionResourceName(10));
        assertTrue(game.isMixing(1));
        assertFalse(game.isMixing(2), "seule la ligne au départ");
        assertEquals(0.1, game.mixMalus(), 1e-12, "un énorme malus");
        assertEquals(1_000, game.mixStageGoal(), 1e-9);
    }

    @Test
    void etape1LaBouleRapporteASesGraduations() {
        Game game = stage(1);
        double value = game.mixValue();
        run(game, 4.1);
        // Un aller (4 s) passe les trois graduations.
        assertEquals(3 * value, game.state().mixShardsEarned(), 1e-9);
        assertTrue(game.mixShardList().isEmpty(), "pas d'éclats avant le plan");
    }

    @Test
    void chaqueEtapeGagneeOuvreLaSuivanteEtRemetLesAmeliorations() {
        Game game = stage(1);
        game.state().setMixShards(1_000);
        assertTrue(game.buyMix(MixUpgrade.GAIN));
        assertFalse(game.canBuyMix(MixUpgrade.SPEED), "pas avant le plan");
        assertFalse(game.advanceMix(), "l'étape n'est pas gagnée");
        game.state().setMixStageEarned(1_000);
        assertTrue(game.advanceMix());
        assertEquals(2, game.mixStage());
        assertEquals(0, game.mixLevel(MixUpgrade.GAIN), "les améliorations repartent de zéro");
        assertEquals(12_000, game.mixStageGoal(), 1e-9);
        assertEquals(1.5, game.mixStageMultiplier(), 1e-12, "+50 % par dimension mêlée");
        assertTrue(game.canBuyMix(MixUpgrade.SPEED));
        game.state().setMixStage(9);
        game.state().setMixStageEarned(1e30);
        assertFalse(game.advanceMix(), "tout est mêlé à l'étape 9");
    }

    @Test
    void etape2LePointRamasseLesEclatsQuePoseLaBoule() {
        Game game = stage(2);
        game.testPlaceMixCursor(0, 0, 0);
        run(game, 4.1);
        assertEquals(3, game.mixShardList().size(), "chaque graduation pose un éclat");
        assertEquals(0, game.state().mixShardsEarned(), 1e-12);
        game.testClearMix();
        game.testAddMixShard(0.5, 0.5, 0, 0);
        game.testPlaceMixCursor(0.5, 0.47, 0);
        game.tick(0.05);
        assertTrue(game.mixShardList().isEmpty());
        assertEquals(game.mixValue() * GameMix.MIX_SHARD_VALUE, game.state().mixShardsEarned(), 1e-9);
        // Le point file vers la souris.
        game.setMixTarget(0.9, 0.3);
        run(game, 3);
        assertEquals(0.9, game.mixX(), 1e-9);
    }

    @Test
    void etape3IlFautPlongerALaBonneProfondeur() {
        Game game = stage(3);
        game.testAddMixShard(0.5, 0.5, 0.8, 0);
        game.testPlaceMixCursor(0.5, 0.5, 0);
        game.tick(0.05);
        assertEquals(1, game.mixShardList().size(), "trop haut");
        game.setMixDepth(0.8);
        run(game, 0.95);
        assertTrue(game.mixShardList().stream().noneMatch(shard -> shard.x() == 0.5), "ramassé une fois plongé");
        assertEquals(game.mixValue() * 2 * 1.8, game.state().mixShardsEarned(), 1e-9, "plus profond, plus riche");
    }

    @Test
    void etapes4Et5LaChargeEtLeRythme() {
        Game game = stage(5);
        game.testPlaceMixCursor(0.2, 0.2, 0);
        game.holdMix(true);
        game.testAddMixShard(0.2, 0.2, 0, 0);
        game.tick(0.05);
        double pot = game.mixPot();
        assertTrue(pot > 0);
        assertEquals(0, game.state().mixShardsEarned(), 1e-12, "dans la charge, pas encore versé");
        run(game, GameMix.MIX_CHARGE_SECONDS);
        assertEquals(2, game.mixChargeMultiplier(), 1e-9, "×2 en huit secondes tenues");
        game.testSetMixRing(0.5, 0.5);
        assertEquals(MixRhythm.EXCELLENT, game.mixRhythmNow());
        game.holdMix(false);
        assertEquals(MixRhythm.EXCELLENT, game.mixLastRhythm());
        assertEquals(pot * 2 * 3, game.state().mixShardsEarned(), 1e-9, "charge ×2, rythme ×3");
        assertEquals(0, game.mixPot(), 0);
    }

    @Test
    void etape6LeRelachementLanceUneOnde() {
        Game game = stage(6);
        game.testPlaceMixCursor(0.3, 0.5, 0);
        game.testAddMixShard(0.5, 0.5, 0, 0);
        game.holdMix(true);
        game.holdMix(false);
        assertEquals(1, game.mixWaves().size());
        run(game, 0.95);
        assertTrue(game.mixShardList().stream().noneMatch(shard -> shard.x() == 0.5), "l'onde l'a ramassé");
        assertTrue(game.state().mixShardsEarned() > 0);
        assertTrue(game.mixWaves().isEmpty(), "éteinte à sa portée");
    }

    @Test
    void etape7CliquerViteEtLesZones() {
        Game game = stage(7);
        double still = game.mixValue();
        for (int i = 0; i < 20; i++) game.clickMix();
        assertEquals(still * (1 + 0.05 * 10), game.mixValue(), 1e-9, "dix clics par seconde : +50 %");
        game.testAddMixZone(0.5, 0.5);
        game.testAddMixShard(0.5, 0.5, 0, 0);
        game.testPlaceMixCursor(0.5, 0.5, 0);
        double value = game.mixValue();
        game.tick(0.01);
        assertEquals(value * 2 * 2, game.state().mixShardsEarned(), value * 0.2, "dans une zone ×2");
    }

    @Test
    void etape8LeTresor() {
        Game game = stage(8);
        game.testHideTreasure(0.85, 0.15);
        game.testPlaceMixCursor(0.1, 0.9, 0);
        game.tick(0.01);
        assertTrue(game.mixHeat() < 0.3, "loin : froid");
        assertTrue(game.mixHeat() > 0);
        game.testPlaceMixCursor(0.84, 0.16, 0);
        double value = game.mixValue();
        game.tick(0.01);
        assertEquals(1, game.mixTreasures());
        assertEquals(value * GameMix.MIX_TREASURE, game.state().mixShardsEarned(), value);
    }

    @Test
    void etape9LesPairesEnMiroir() {
        Game game = stage(9);
        long a = game.testAddMixShard(0.2, 0.5, 0, 0);
        long b = game.testAddMixShard(0.8, 0.5, 0, a);
        game.testClearMix();
        // Deux éclats reflets l'un de l'autre : le second ramassé juste après le premier vaut ×2.
        long first = game.testAddMixShard(0.2, 0.5, 0, b + 2);
        long second = game.testAddMixShard(0.8, 0.5, 0, first);
        assertEquals(b + 2, second, "les numéros se suivent");
        game.testPlaceMixCursor(0.2, 0.5, 0);
        game.tick(0.01);
        double one = game.state().mixShardsEarned();
        game.testPlaceMixCursor(0.8, 0.5, 0);
        game.tick(0.01);
        assertEquals(one * GameMix.MIX_PAIR, game.state().mixShardsEarned() - one, one * 0.2);
        assertEquals(1, game.mixPairStreak());
        // À l'étape 9, la boule pose les éclats par paires symétriques.
        game.testClearMix();
        game.testPlaceMixCursor(0, 0, 0);
        run(game, 4.1);
        assertEquals(6, game.mixShardList().size());
        GameMix.MixShard shard = game.mixShardList().get(0);
        assertTrue(game.mixShardList().stream().anyMatch(s -> s.id() == shard.twin() && Math.abs(s.x() - (1 - shard.x())) < 1e-9));
    }

    @Test
    void lesAmeliorationsSOuvrentAvecLeurEtape() {
        Game game = stage(9);
        game.state().setMixShards(1e15);
        for (MixUpgrade upgrade : MixUpgrade.values()) assertTrue(game.buyMix(upgrade), upgrade.toString());
        assertEquals(0.15, game.mixMalus(), 1e-12);
        assertEquals(4, game.mixTicks());
        assertEquals(0.345, game.mixCursorSpeed(), 1e-12);
        assertEquals(0.2, game.mixDepthTolerance(), 1e-12);
        assertEquals(0.36, game.mixWaveReach(), 1e-12);
        assertEquals(2, game.mixMaxZones());
        assertEquals(2.25, game.mixPairFactor(), 1e-12);
        game.state().setMixLevel(MixUpgrade.MALUS.ordinal(), MixUpgrade.MALUS.max());
        assertEquals(1, game.mixMalus(), 1e-12, "au plus haut, plus de malus");
    }

    @Test
    void leMelangeAutomatiqueJoueEtPasseLesEtapes() {
        Game game = stage(9);
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.MIX_AUTO.name());
        run(game, 120);
        assertTrue(game.state().mixShardsEarned() > 0, "il ramasse seul");
        assertTrue(game.mixReleases() > 3, "il tient et relâche la charge : " + game.mixReleases());
        assertTrue(game.mixClickRate() >= 5, "il clique");
        double before = game.state().mixShardsEarned();
        game.tick(3_600);
        assertTrue(game.state().mixShardsEarned() > before, "une absence aussi");
        Game early = stage(1);
        early.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        early.state().addDimensionAutomation(DimensionAutomation.MIX_AUTO.name());
        early.state().setMixStageEarned(1_000);
        early.tick(0.05);
        assertEquals(2, early.mixStage(), "il passe à l'étape suivante");
        assertEquals(12, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 10).count());
        assertEquals(9, DimensionAutomation.MIX_AUTO.paidWith(), "payés en reflets du miroir");
    }

    @Test
    void laBulleDesRacinesAttendLaGraine() {
        Game game = stage(1);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(TreeBubble.ROOTS.stage()));
        game.state().setTreeBubbleBought(TreeBubble.SEED.ordinal(), true);
        assertTrue(game.isTreeBubbleShown(TreeBubble.ROOTS));
        assertFalse(game.isTreeBubbleOpen(TreeBubble.ROOTS), "sans l'amélioration de la graine, fermée");
        game.state().setSeedUpgradeLevel(SeedUpgrade.ROOT_BUBBLE.ordinal(), 1);
        assertTrue(game.isTreeBubbleOpen(TreeBubble.ROOTS));
        assertEquals(10, SeedUpgrade.ROOT_BUBBLE.dimension(), "payée en fragments");
        assertEquals(1, SeedUpgrade.ROOT_BUBBLE.max());
    }

    @Test
    void leMelangeTraverseLesUniversEtSeSauve() throws Exception {
        Game game = stage(4);
        game.state().setMixShards(1e6);
        assertTrue(game.buyMix(MixUpgrade.GAIN));
        game.state().setMixShardsEarned(GameUniverse.MIX_THRESHOLD);
        game.state().setMixStageEarned(12);
        assertTrue(game.isDimensionThresholdReached(10));
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.MIX_MASTER));
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(1, back.mixLevel(MixUpgrade.GAIN.ordinal()));
        assertEquals(4, back.mixStage());
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("mix")) old.append(line).append('\n');
        GameState fresh = SaveCodec.read(old.toString()).state();
        assertEquals(0, fresh.mixShards(), 0);
        assertEquals(1, fresh.mixStage(), "une sauvegarde d'avant repart de la ligne");
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertEquals(1, game.mixLevel(MixUpgrade.GAIN));
        assertEquals(4, game.mixStage());
    }
}
