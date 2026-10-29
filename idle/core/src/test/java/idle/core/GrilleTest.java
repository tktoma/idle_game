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
        assertEquals(2, game.gridDivisions());
        assertEquals(500, game.gridCellSize(), 1e-12);
        assertEquals(1, game.gridPoints().size());
        for (GridPoint point : game.gridPoints()) {
            assertTrue(point.x() >= 0 && point.x() <= 1_000 && point.z() >= 0 && point.z() <= 1_000, "dans le cube");
        }
    }

    @Test
    void sonderLaBonneCaseTrouveLePoint() {
        Game game = eight();
        game.testSetGridPoints(List.of(new GridPoint(100, 100, 100)));
        GridProbe probe = game.probeGrid(0, 0, 0);
        assertNotNull(probe);
        assertTrue(probe.found());
        double d = Math.sqrt(3) * 150;
        assertEquals(d, probe.distance(), 1e-9);
        double p = 1 - d / GameGrid.GRID_DIAGONAL;
        assertEquals((1 + 4 * p * p * p) * GameGrid.GRID_FOUND, probe.gain(), 1e-9);
        assertEquals(probe.gain(), game.state().gridShards(), 1e-9);
        assertNull(game.probeGrid(1, 1, 1), "une courte attente après chaque sondage");
        game.tick(GameGrid.GRID_COOLDOWN + 0.01);
        assertTrue(game.canProbeGrid());
        assertFalse(game.gridPoints().get(0).equals(new GridPoint(100, 100, 100)), "trouvé, il se cache ailleurs");
        assertEquals(1, game.gridFound());
    }

    @Test
    void plusProcheRapportePlus() {
        Game game = eight();
        game.state().setGridLevel(GridUpgrade.DIVISION.ordinal(), 8);
        assertEquals(10, game.gridDivisions());
        game.testSetGridPoints(List.of(new GridPoint(555, 555, 555)));
        double far = game.probeGrid(9, 9, 9).gain();
        game.testSetGridPoints(List.of(new GridPoint(555, 555, 555)));
        double near = game.probeGrid(6, 5, 5).gain();
        assertTrue(near > far, "voisine : " + near + ", au coin : " + far);
        game.testSetGridPoints(List.of(new GridPoint(555, 555, 555)));
        GridProbe found = game.probeGrid(5, 5, 5);
        assertTrue(found.found() && found.gain() > near * GameGrid.GRID_FOUND * 0.9);
        assertEquals(GameGrid.GRID_DIAGONAL, GameGrid.GRID_SIZE * Math.sqrt(3), 1e-9);
        assertEquals(1, game.gridProximityFactor(GameGrid.GRID_DIAGONAL), 1e-12, "au plus loin, rien de plus");
        assertEquals(5, game.gridProximityFactor(0), 1e-12, "sur le point, ×5");
    }

    @Test
    void lePointLePlusProcheCompte() {
        Game game = eight();
        game.state().setGridLevel(GridUpgrade.POINTS.ordinal(), 1);
        game.testSetGridPoints(List.of(new GridPoint(900, 900, 900), new GridPoint(400, 400, 400)));
        assertEquals(new GridPoint(400, 400, 400), game.gridNearest(0, 0, 0));
        assertEquals(new GridPoint(900, 900, 900), game.gridNearest(1, 1, 1));
    }

    @Test
    void lesAmeliorationsChangentLaGrille() {
        Game game = eight();
        game.state().setGridShards(1e15);
        for (GridUpgrade upgrade : GridUpgrade.values()) assertTrue(game.buyGrid(upgrade), upgrade.toString());
        assertEquals(3, game.gridDivisions());
        assertEquals(2 * game.hadronBoost(idle.core.collider.Hadron.OMEGA), game.gridValue(), 1e-9);
        assertEquals(1, game.gridReveal());
        assertEquals(2, game.gridPointCount());
        assertEquals(2, game.gridPoints().size(), "un point de plus, caché aussitôt");
        assertEquals(5, game.gridProximityStep(), 1e-12);
        game.state().setGridLevel(GridUpgrade.DIVISION.ordinal(), GridUpgrade.DIVISION.max());
        assertEquals(24, game.gridDivisions());
        assertFalse(game.canBuyGrid(GridUpgrade.DIVISION));
    }

    @Test
    void lAutomatismeSondeSeul() {
        Game game = eight();
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.GRID_PROBE.name());
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertEquals(30, game.gridAutoProbes(), 1, "un sondage toutes les deux secondes");
        assertTrue(game.gridFound() > 2 && game.gridFound() < 20, "il trouve une fois sur quatre environ : " + game.gridFound());
        game.tick(200);
        assertEquals(130, game.gridAutoProbes(), 2, "une absence les compte aussi");
        assertEquals(6, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 8).count());
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
