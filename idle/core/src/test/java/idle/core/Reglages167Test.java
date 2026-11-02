package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.collider.ColliderUpgrade;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import org.junit.jupiter.api.Test;

/**
 * Version 0.167 : Persistance en cinq niveaux, l'amorti de l'espace au-delà de 10 000 par seconde, la matière noire des
 * univers en +1 par univers, et la coupure générale qui arrête aussi les automates du collisionneur.
 */
class Reglages167Test {

    private static Upgrade persistence() {
        return Upgrades.DEFAULT.stream().filter(u -> u.id().equals("atom_keep")).findFirst().orElseThrow();
    }

    @Test
    void persistanceACinqNiveauxDeHuitACentDixHuitAtomes() {
        Upgrade keep = persistence();
        assertEquals(5, keep.maxLevel());
        double[] costs = {8, 16, 31, 61, 118};
        for (int level = 0; level < 5; level++) assertEquals(costs[level], keep.costAt(level).toDouble(), 0, "niveau " + (level + 1));
        assertTrue(keep.costAt(4).lte(Game.MAX_ATOMS), "le dernier tient sous le plafond d'avant la première explosion");
        assertEquals(java.util.List.of(0.10, 0.25, 0.50, 0.75, 1.0), Upgrades.PERSISTENCE_SHARES);
    }

    @Test
    void chaqueNiveauGardeSaPartDesAmeliorationsAlaFusion() {
        double[] shares = {0, 0.10, 0.25, 0.50, 0.75, 1.0};
        for (int level = 0; level <= 5; level++) {
            Game game = GameTestSupport.realGame();
            game.state().setLevel("atom_keep", level);
            assertEquals(shares[level], game.fusionKeepShare(), 1e-12, "niveau " + level);
            assertEquals(level > 0, game.keepsUpgradesOnFusion());
            game.state().setLevel("speed", 10);
            game.state().setLevel("coupling", 7);
            game.state().setLevel("generator", game.generatorsPerAtom() - 1);
            assertTrue(game.canFuse());
            assertTrue(game.fuse());
            assertEquals((int) Math.floor(10 * shares[level] + 1e-9), game.levelOf("speed"), "vitesse, niveau " + level);
            assertEquals((int) Math.floor(7 * shares[level] + 1e-9), game.levelOf("coupling"), "couplage, niveau " + level);
            assertEquals(0, game.levelOf("generator"), "les générateurs fusionnent toujours");
        }
    }

    @Test
    void chaqueAutomatismeAcheteEnLotPayeEnMatiereNoire() {
        Game game = GameTestSupport.realGame();
        assertFalse(game.isAutomationBatchAvailable("auto_generator"), "pas avant la matière noire");
        game.state().setExplosions(1);
        game.state().setDarkMatter(idle.core.math.BigNum.of(10_000));
        assertTrue(game.isAutomationBatchAvailable("auto_generator"));
        assertTrue(game.isAutomationBatchAvailable("auto_speed"));
        assertFalse(game.isAutomationBatchAvailable("auto_fusion"), "la fusion n'achète rien");
        double[] costs = {2, 8, 32, 128, 512, 2048};
        for (int level = 0; level < Game.AUTOMATION_BATCH_LEVELS; level++) {
            assertEquals(1 << level, game.automationBatch("auto_generator"));
            assertEquals(costs[level], game.automationBatchCost("auto_generator").toDouble(), 1e-9);
            assertTrue(game.buyAutomationBatch("auto_generator"), "niveau " + (level + 1));
        }
        assertEquals(64, game.automationBatch("auto_generator"));
        assertTrue(game.isAutomationBatchMaxed("auto_generator"));
        assertFalse(game.buyAutomationBatch("auto_generator"), "pas au-delà de 64");
        assertEquals(10_000 - 2730, game.state().darkMatter().toDouble(), 1e-9, "payé en matière noire");
        assertEquals(1, game.automationBatch("auto_speed"), "chacun le sien");
        game.state().clearDarkMatter();
        assertEquals(64, game.automationBatch("auto_generator"), "il traverse le Big Bang");
        game.state().reset();
        assertEquals(1, game.automationBatch("auto_generator"), "il part avec l'univers");
    }

    @Test
    void uneActionAcheteAuPlusSonLot() {
        Game game = GameTestSupport.realGame();
        game.state().setExplosions(1);
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        game.state().setAtoms(idle.core.math.BigNum.of(100));
        assertTrue(game.buyAutomation("auto_generator"));
        game.state().setAutomationBatchLevel("auto_generator", 2);
        game.state().setParticles(idle.core.math.BigNum.of(1e30));
        int before = game.levelOf("generator");
        game.tick(game.automationInterval("auto_generator") + 0.01);
        assertEquals(before + 4, game.levelOf("generator"), "par 4");
    }

    @Test
    void lEspaceSAmortitAuDelaDeDixMilleParSeconde() {
        assertEquals(5_000, Game.dampedSpace(5_000), 1e-9, "sous le seuil, rien ne change");
        assertEquals(10_000, Game.dampedSpace(10_000), 1e-9);
        assertEquals(100_000, Game.dampedSpace(1e7), 1e-6, "mille fois les bonus : dix fois l'espace");
        assertEquals(1e6, Game.dampedSpace(1e10), 1e-3);
        Game game = GameTestSupport.realGame();
        assertEquals(0, game.rawSpacePerSecond(), 0, "pas d'expansion avant le premier Big Bang");
        game.state().setBigBangs(3);
        assertTrue(game.rawSpacePerSecond() >= 3 * Game.SPACE_PER_SECOND, "trois Big Bangs");
        assertEquals(Game.dampedSpace(game.rawSpacePerSecond()) * game.probeBoostMultiplier(),
                game.steadySpacePerSecond().toDouble(), 1e-9);
    }

    @Test
    void chaqueUniversAjouteUnALaMatiereNoire() {
        assertEquals(1, Game.UNIVERSE_DARK_MATTER_STEP, 0);
        assertEquals(1, GameTestSupport.realGame().universeDarkMatterFactor(), 0);
        assertEquals(5, GameTestSupport.universe(4).universeDarkMatterFactor(), 0, "quatre univers : ×5 (×16 avant)");
    }

    @Test
    void laCoupureGeneraleArreteLesAutomatesDuCollisionneur() {
        Game game = GameTestSupport.realGame();
        ColliderUpgrade auto = ColliderUpgrade.automationOf(idle.core.collider.ColliderGenerator.BEAM);
        game.state().setColliderUpgradeLevel(auto, 1);
        assertTrue(game.isColliderAutomationRunning(auto));
        game.setAutomationPaused(true);
        assertFalse(game.isColliderAutomationRunning(auto), "coupé avec tout le reste");
        assertFalse(game.state().isColliderPaused(auto), "son propre réglage ne change pas");
        game.setAutomationPaused(false);
        assertTrue(game.isColliderAutomationRunning(auto));
        assertFalse(game.anyColliderAutomationPaused());
        game.setColliderAutomationsPaused(true);
        assertTrue(game.anyColliderAutomationPaused());
        assertFalse(game.isColliderAutomationRunning(auto));
        game.setColliderAutomationsPaused(false);
        assertTrue(game.isColliderAutomationRunning(auto));
    }
}
