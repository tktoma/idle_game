package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.SeedUpgrade;
import idle.core.GameUniverse.TreeBubble;
import idle.core.math.BigNum;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.182 : les automatismes de la dimension n s'ouvrent au palier n + 2 de la graine (l'arbre-monde au plus) et à
 * n + 1 univers formés, dimension par dimension ; la bulle du sommet de l'arbre-monde les fait moitié prix. L'étage du
 * chrono est passé au plan, le tuyau au volume, la terre fertile à la durée.
 */
class Automatismes182Test {

    @Test
    void chaqueDimensionOuvreSesAutomatismesAUnPalierEtUnUniversDePlusQueLaPrecedente() {
        for (int dimension = 1; dimension <= Game.MAX_DIMENSIONS; dimension++) {
            assertEquals(Math.min(dimension + 2, Game.SEED_STAGES), Game.dimensionAutomationStage(dimension), "palier, dimension " + dimension);
            assertEquals(dimension + 1, Game.dimensionAutomationUniverses(dimension), "univers, dimension " + dimension);
        }
        assertEquals(3, Game.dimensionAutomationStage(1), "la ligne : la graine craquelée");
        assertEquals(Game.SEED_STAGES, Game.dimensionAutomationStage(9), "le miroir : l'arbre-monde");
        assertEquals(Game.SEED_STAGES, Game.dimensionAutomationStage(10), "le mélange : l'arbre-monde aussi, pas de douzième palier");
        // Deux univers, la graine au palier 3 : la ligne seule.
        Game game = GameTestSupport.universe(2);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(3));
        assertEquals(3, game.seedStage());
        assertTrue(game.isDimensionAutomationOpen(1));
        assertTrue(game.isDimensionAutomationUnlocked(), "le sous-onglet existe");
        assertFalse(game.isDimensionAutomationOpen(2), "le plan : palier 4 et trois univers");
        assertFalse(game.isDimensionAutomationOpen(0) || game.isDimensionAutomationOpen(11));
        // Le palier sans l'univers, l'univers sans le palier : fermé.
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(4, game.seedStage(), "deux paliers d'avance au plus sur deux univers");
        assertFalse(game.isDimensionAutomationOpen(2), "le palier 4 y est, pas le troisième univers");
        assertTrue(game.testAddUniverse());
        assertTrue(game.isDimensionAutomationOpen(2));
        game.state().setSeedWaterings(Game.seedStageWaterings(3));
        assertFalse(game.isDimensionAutomationOpen(2), "trois univers, mais la graine est redescendue au palier 3");
        // Rien avant le deuxième univers, quoi que fasse la graine.
        Game first = GameTestSupport.universe(1);
        first.state().setSeedSprouted(true);
        first.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertFalse(first.isDimensionAutomationUnlocked());
        // La bulle du sommet n'ouvre plus rien.
        first.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        assertFalse(first.isDimensionAutomationUnlocked());
    }

    @Test
    void lesAutomatismesSAchetentDimensionParDimensionEtLaBulleDuSommetFaitMoitiePrix() {
        Game game = GameTestSupport.universe(3);
        GameTestSupport.openDimensionAutomations(game, 2);
        game.state().setUniverseSeeds(BigNum.of(100));
        game.state().setDimensionShards(1e9);
        game.state().setPlaneShards(1e9);
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() <= 2) assertTrue(game.buyDimensionAutomation(automation), automation.toString());
            else assertFalse(game.canBuyDimensionAutomation(automation), automation + " : sa dimension n'est pas ouverte aux automatismes");
        }
        assertTrue(game.testAddUniverse());
        assertTrue(game.isDimensionAutomationOpen(3), "quatre univers, l'arbre-monde : le volume");
        assertEquals(DimensionAutomation.VOLUME_DIVE.cost(), game.dimensionAutomationCost(DimensionAutomation.VOLUME_DIVE), 1e-9);
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        assertEquals(DimensionAutomation.VOLUME_DIVE.cost() * Game.SUMMIT_DISCOUNT, game.dimensionAutomationCost(DimensionAutomation.VOLUME_DIVE), 1e-9);
        game.state().setPlaneShards(DimensionAutomation.VOLUME_DIVE.cost() * 0.6);
        assertTrue(game.canBuyDimensionAutomation(DimensionAutomation.VOLUME_DIVE), "60 % du prix suffisent avec la bulle");
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.VOLUME_DIVE));
        assertEquals(DimensionAutomation.VOLUME_DIVE.cost() * 0.1, game.state().planeShards(), 1e-6);
        // Les automatismes achetés tournent sans la bulle, et la carte de la bulle dit son nouveau rôle.
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), false);
        assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.VOLUME_DIVE));
        assertTrue(TreeBubble.SUMMIT.effect().contains("moitié prix"), TreeBubble.SUMMIT.effect());
    }

    @Test
    void leChronoVientAvecLePlanLeTuyauAvecLeVolumeLaTerreFertileAvecLaDuree() {
        assertEquals(2, SeedUpgrade.CHRONO.dimension());
        assertEquals(3, SeedUpgrade.HOSE.dimension());
        assertEquals(4, SeedUpgrade.SOIL.dimension());
        assertEquals(1, SeedUpgrade.WATERING.dimension());
        // Chaque dimension garde deux améliorations parmi les quatre premières : une de l'arrosage ou de l'étagère, une du chrono.
        for (int dimension = 1; dimension <= 4; dimension++) {
            int plain = 0, chrono = 0;
            for (SeedUpgrade upgrade : SeedUpgrade.values()) {
                if (upgrade.dimension() != dimension) continue;
                if (upgrade.chrono()) chrono++; else plain++;
            }
            assertEquals(1, plain, "dimension " + dimension);
            assertEquals(1, chrono, "dimension " + dimension);
        }
        Game game = GameTestSupport.universe(2);
        game.state().setPlaneShards(1e6);
        assertTrue(game.canBuySeedUpgrade(SeedUpgrade.CHRONO), "deux univers : l'étage du chrono s'achète en facettes");
        assertFalse(game.isSeedUpgradeOpen(SeedUpgrade.HOSE), "le tuyau attend le volume");
        assertTrue(game.buySeedUpgrade(SeedUpgrade.CHRONO));
        assertTrue(game.isChronoUnlocked());
        assertEquals(1e6 - 1_167, game.state().planeShards(), 1e-6);
    }
}
