package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameGrid.GridPoint;
import idle.core.GameGrid.GridProbe;
import idle.core.GameGrid.GridUpgrade;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.156 : la grille, huitième dimension. Un cube de 1 000 de côté où des points sont cachés ; sonder une case
 * rapporte d'autant plus que son centre est proche d'un point, et beaucoup plus si le point est dedans. Et la graine
 * profonde, qui multiplie l'arrosage par √(log10 E) de l'énergie du vide récupérée.
 */
class GrilleTest {

    private static Game eight() {
        return GameTestSupport.universe(8);
    }

    @Test
    void laGrilleSOuvreAuHuitiemeUnivers() {
        assertFalse(GameTestSupport.universe(7).isGridOpen());
        Game game = eight();
        assertTrue(game.isGridOpen());
        assertEquals("repères", Game.dimensionResourceName(8));
        assertEquals(1_000, game.gridDivisions(), "une énorme grille : mille cases par côté");
        assertEquals(1, game.gridCellSize(), 1e-12);
        assertEquals(1, game.gridPoints().size());
        assertEquals(1, game.gridAttempts(), "un essai par point caché");
        for (GridPoint point : game.gridPoints()) {
            assertTrue(point.x() >= 0 && point.x() <= 1_000 && point.z() >= 0 && point.z() <= 1_000, "dans le cube");
        }
    }

    @Test
    void tomberSurLaCaseTrouveLePoint() {
        Game game = eight();
        game.testSetGridPoints(List.of(new GridPoint(100.5, 100.5, 100.5)));
        GridProbe probe = game.probeGrid(100, 100, 100);
        assertNotNull(probe);
        assertTrue(probe.found());
        assertEquals(0, probe.distance(), 1e-9);
        assertEquals(5 * GameGrid.GRID_FOUND, probe.gain(), 1e-9, "×5 sur le point, ×5 trouvé");
        assertEquals(probe.gain(), game.state().gridShards(), 1e-9);
        assertNull(game.probeGrid(1, 1, 1), "une courte attente après chaque essai");
        game.tick(GameGrid.GRID_COOLDOWN + 0.01);
        assertTrue(game.canProbeGrid());
        assertFalse(game.gridPoints().get(0).equals(new GridPoint(100.5, 100.5, 100.5)), "trouvé, il se cache ailleurs");
        assertEquals(1, game.gridFound());
    }

    @Test
    void unEssaiParPointPuisIlSeCacheAilleurs() {
        Game game = eight();
        game.testSetGridPoints(List.of(new GridPoint(500.5, 500.5, 500.5)));
        GridProbe miss = game.probeGrid(0, 0, 0);
        assertFalse(miss.found());
        assertEquals(500 * Math.sqrt(3), miss.distance(), 1e-9, "en cases");
        assertEquals(0, miss.triesLeft());
        assertFalse(game.gridPoints().get(0).equals(new GridPoint(500.5, 500.5, 500.5)), "manqué, il se cache ailleurs aussi");
        // Avec l'amélioration des essais : trois essais sur le même point.
        game.state().setGridLevel(GridUpgrade.ATTEMPTS.ordinal(), 2);
        assertEquals(3, game.gridAttempts());
        game.testSetGridPoints(List.of(new GridPoint(500.5, 500.5, 500.5)));
        assertEquals(3, game.gridTriesLeft(0));
        assertEquals(2, game.probeGrid(0, 0, 0).triesLeft());
        game.tick(0.6);
        assertEquals(1, game.probeGrid(999, 999, 999).triesLeft());
        assertEquals(new GridPoint(500.5, 500.5, 500.5), game.gridPoints().get(0), "il attend ses essais");
        game.tick(0.6);
        assertEquals(0, game.probeGrid(10, 10, 10).triesLeft());
        assertFalse(game.gridPoints().get(0).equals(new GridPoint(500.5, 500.5, 500.5)));
        assertEquals(3, game.gridTriesLeft(0), "le suivant a ses trois essais");
        // Version 0.158 : chaque point a son numéro ; l'interface oublie les essais des points partis.
        long first = game.gridLastProbe().pointId();
        assertFalse(game.isGridPointAlive(first), "parti ailleurs, il a un autre numéro");
        assertTrue(game.isGridPointAlive(game.gridPointId(0)));
    }

    @Test
    void reduireLaGrilleAideAToucher() {
        assertEquals(1_000, GameGrid.cellsAt(0));
        assertEquals(700, GameGrid.cellsAt(1));
        assertEquals(2, GameGrid.cellsAt(GridUpgrade.DIVISION.max()), "deux cases par côté au plus réduit");
        Game game = eight();
        game.state().setGridLevel(GridUpgrade.DIVISION.ordinal(), GridUpgrade.DIVISION.max());
        assertEquals(500, game.gridCellSize(), 1e-12);
        int found = 0;
        java.util.Random random = new java.util.Random(3);
        for (int n = 0; n < 400; n++) {
            game.tick(0.6);
            GridProbe probe = game.probeGrid(random.nextInt(2), random.nextInt(2), random.nextInt(2));
            if (probe.found()) found++;
        }
        assertTrue(found > 25 && found < 80, "une chance sur huit : " + found + " sur 400");
    }

    @Test
    void plusProcheRapportePlus() {
        Game game = eight();
        game.testSetGridPoints(List.of(new GridPoint(555.5, 555.5, 555.5)));
        double far = game.probeGrid(999, 999, 999).gain();
        game.tick(0.6);
        game.testSetGridPoints(List.of(new GridPoint(555.5, 555.5, 555.5)));
        double near = game.probeGrid(560, 555, 555).gain();
        assertTrue(near > far, "à cinq cases : " + near + ", au coin : " + far);
        assertEquals(1, game.gridProximityFactor(1_000 * Math.sqrt(3)), 1e-12, "au plus loin, rien de plus");
        assertEquals(5, game.gridProximityFactor(0), 1e-12, "sur le point, ×5");
        game.state().setGridLevel(GridUpgrade.DIVISION.ordinal(), 5);
        assertEquals(1, game.gridProximityFactor(game.gridDivisions() * Math.sqrt(3)), 1e-12, "en part de la grille, réduite ou non");
    }

    @Test
    void lePointLePlusProcheCompte() {
        Game game = eight();
        game.state().setGridLevel(GridUpgrade.POINTS.ordinal(), 1);
        game.state().setGridLevel(GridUpgrade.DIVISION.ordinal(), GridUpgrade.DIVISION.max());
        game.testSetGridPoints(List.of(new GridPoint(900, 900, 900), new GridPoint(400, 400, 400)));
        assertEquals(new GridPoint(400, 400, 400), game.gridNearest(0, 0, 0));
        assertEquals(new GridPoint(900, 900, 900), game.gridNearest(1, 1, 1));
        assertEquals(1, game.gridTriesLeft(1));
    }

    @Test
    void lesAmeliorationsChangentLaGrille() {
        Game game = eight();
        game.state().setGridShards(1e15);
        for (GridUpgrade upgrade : GridUpgrade.values()) assertTrue(game.buyGrid(upgrade), upgrade.toString());
        assertEquals(700, game.gridDivisions());
        assertEquals(2 * game.hadronBoost(idle.core.collider.Hadron.OMEGA), game.gridValue(), 1e-9);
        assertEquals(1, game.gridReveal());
        assertEquals(2, game.gridPointCount());
        assertEquals(2, game.gridPoints().size(), "un point de plus, caché aussitôt");
        assertEquals(5, game.gridProximityStep(), 1e-12);
        assertEquals(2, game.gridAttempts());
        game.state().setGridLevel(GridUpgrade.ATTEMPTS.ordinal(), GridUpgrade.ATTEMPTS.max());
        assertEquals(4, game.gridAttempts());
        assertFalse(game.canBuyGrid(GridUpgrade.ATTEMPTS));
    }

    @Test
    void lAutomatismeEssaieSeul() {
        Game game = eight();
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.GRID_PROBE.name());
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertEquals(30, game.gridAutoProbes(), 1, "un essai toutes les deux secondes");
        assertEquals(0, game.gridFound(), "sur un milliard de cases, il ne trouve pas");
        assertTrue(game.state().gridShardsEarned() > 30, "mais il vise près : " + game.state().gridShardsEarned());
        game.state().setGridLevel(GridUpgrade.DIVISION.ordinal(), GridUpgrade.DIVISION.max());
        game.tick(200);
        assertEquals(130, game.gridAutoProbes(), 2, "une absence les compte aussi");
        assertEquals(100, game.gridFound(), 2, "réduite à deux cases par côté, il trouve à chaque fois");
        assertEquals(7, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 8).count());
        assertEquals(7, DimensionAutomation.GRID_PROBE.paidWith(), "payés en étincelles du clicker");
    }

    @Test
    void laGraineProfondeMultiplieLArrosage() {
        Game game = eight();
        game.state().setVoidTrickled(1e16);
        assertEquals(4, game.deepSeedFull(), 1e-9, "√16 : seize puissances de dix");
        double rate = game.wateringCanRate();
        game.state().setSeedUpgradeLevel(SeedUpgrade.DEEP_SEED.ordinal(), 5);
        assertEquals(rate * 2.5, game.wateringCanRate(), 1e-9, "la moitié de l'effet au niveau 5");
        game.state().setSeedUpgradeLevel(SeedUpgrade.DEEP_SEED.ordinal(), 10);
        assertEquals(rate * 4, game.wateringCanRate(), 1e-9);
        assertEquals(rate * 4 * game.hoseShare(), game.hoseRate(), 1e-9, "le tuyau aussi");
        assertEquals(8, SeedUpgrade.DEEP_SEED.dimension(), "payée en repères");
        game.state().setVoidTrickled(0);
        assertEquals(1, game.deepSeedFull(), 1e-12, "sans énergie du vide, rien");
    }

    @Test
    void laGrilleTraverseLesUniversEtSeSauve() throws Exception {
        Game game = eight();
        game.state().setGridShards(1e6);
        assertTrue(game.buyGrid(GridUpgrade.GAIN));
        game.state().setGridShardsEarned(GameUniverse.GRID_THRESHOLD);
        assertTrue(game.isDimensionThresholdReached(8));
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.GRID_SEEKER));
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(1, back.gridLevel(GridUpgrade.GAIN.ordinal()));
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("grid")) old.append(line).append('\n');
        assertEquals(0, SaveCodec.read(old.toString()).state().gridShards(), 0);
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertEquals(1, game.gridLevel(GridUpgrade.GAIN));
    }
}
