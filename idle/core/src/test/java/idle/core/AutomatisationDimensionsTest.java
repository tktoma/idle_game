package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.150 : l'automatisation des dimensions, ouverte par la bulle du sommet de l'arbre-monde. Dans chaque
 * dimension, les automatismes s'achètent l'un après l'autre : ceux de la ligne en graines d'univers, ceux de la
 * dimension n avec la ressource de la dimension n − 1.
 */
class AutomatisationDimensionsTest {

    /** Quatre dimensions ouvertes, la bulle du sommet, et de quoi payer. */
    private static Game four() {
        Game game = GameTestSupport.universe(4);
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        game.state().setUniverseSeeds(BigNum.of(100));
        game.state().setDimensionShards(1e9);
        game.state().setPlaneShards(1e9);
        game.state().setVolumeShards(1e9);
        game.state().setDurationShards(1e9);
        return game;
    }

    private static void buyAll(Game game, int dimension) {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() == dimension) assertTrue(game.buyDimensionAutomation(automation), automation.toString());
        }
    }

    @Test
    void lesAutomatismesSuiventLesDimensionsEtLeursAmeliorations() {
        int[] counts = new int[9];
        for (DimensionAutomation automation : DimensionAutomation.values()) counts[automation.dimension()]++;
        assertEquals(5, counts[1], "la ligne : ses cinq améliorations, rien à jouer");
        assertEquals(6, counts[2], "le plan : la visée, puis ses cinq améliorations");
        assertEquals(6, counts[3]);
        assertEquals(5, counts[4]);
        assertEquals(7, counts[5], "la boucle (version 0.153) : le rythme, puis ses six améliorations");
        assertEquals(6, counts[6], "la brane (version 0.154) : la tirée, puis ses cinq améliorations");
        assertEquals(9, counts[7], "le clicker (version 0.155) : les clics, puis ses huit améliorations");
        assertEquals(6, counts[8], "la grille (version 0.156) : le sondage, puis ses cinq améliorations");
        assertEquals(50, DimensionAutomation.values().length);
        assertNull(DimensionAutomation.PLANE_AIM.previous(), "la visée vient d'abord");
        assertEquals(DimensionAutomation.PLANE_AIM, DimensionAutomation.PLANE_VALUE.previous());
        assertEquals(DimensionAutomation.Kind.PLAY, DimensionAutomation.VOLUME_DIVE.kind());
        assertEquals(DimensionAutomation.Kind.PLAY, DimensionAutomation.DURATION_HOLD.kind());
        // Les prix : la ligne en graines (1, 1, 2, 2, 3), les suivantes ×2 par automatisme, avec la ressource d'avant.
        assertEquals(1, DimensionAutomation.LINE_YIELD.cost(), 0);
        assertEquals(3, DimensionAutomation.LINE_LINES.cost(), 0);
        assertEquals(0, DimensionAutomation.LINE_YIELD.paidWith(), "les graines d'univers");
        assertEquals(1, DimensionAutomation.PLANE_AIM.paidWith(), "les segments de la ligne");
        assertEquals(DimensionAutomation.PLANE_AIM.cost() * 2, DimensionAutomation.PLANE_VALUE.cost(), 0);
        assertEquals("segments", Game.dimensionAutomationCurrency(DimensionAutomation.PLANE_AIM));
        assertEquals("sphères", Game.dimensionAutomationCurrency(DimensionAutomation.DURATION_HOLD));
    }

    @Test
    void ilFautLaBulleDuSommetPuisLOrdre() {
        Game game = four();
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), false);
        assertFalse(game.canBuyDimensionAutomation(DimensionAutomation.LINE_YIELD), "sans la bulle du sommet");
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        assertFalse(game.canBuyDimensionAutomation(DimensionAutomation.LINE_TICKS), "pas avant celui d'avant");
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.LINE_YIELD));
        assertEquals(99, game.state().universeSeeds().toDouble(), 1e-9, "une graine");
        assertFalse(game.buyDimensionAutomation(DimensionAutomation.LINE_YIELD), "une seule fois");
        assertTrue(game.canBuyDimensionAutomation(DimensionAutomation.LINE_TICKS));
        double segments = game.state().dimensionShards();
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.PLANE_AIM));
        assertEquals(segments - DimensionAutomation.PLANE_AIM.cost(), game.state().dimensionShards(), 1e-6, "payé en segments");
        // Une dimension pas encore ouverte n'a pas ses automatismes.
        Game two = GameTestSupport.universe(2);
        two.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        two.state().setDimensionShards(1e9);
        two.state().setPlaneShards(1e9);
        assertTrue(two.isDimensionAutomationAvailable(DimensionAutomation.PLANE_AIM));
        assertFalse(two.isDimensionAutomationAvailable(DimensionAutomation.VOLUME_DIVE), "deux dimensions ouvertes");
    }

    @Test
    void lesAchatsAutomatiquesPrennentLesAmeliorations() {
        Game game = four();
        buyAll(game, 1);
        int before = game.lineLevel(Game.LineUpgrade.SPEED);
        game.tick(0.1);
        assertTrue(game.lineLevel(Game.LineUpgrade.SPEED) > before, "l'élan s'achète seul");
        assertEquals(game.lineMaxLevel(Game.LineUpgrade.BALLS), game.lineLevel(Game.LineUpgrade.BALLS), "les boules jusqu'au plus haut");
        // Coupé à part, il n'achète plus ; « Tout couper » les arrête tous.
        game.state().setDimensionShards(1e12);
        assertTrue(game.setDimensionAutomationEnabled(DimensionAutomation.LINE_YIELD, false));
        int yield = game.lineLevel(Game.LineUpgrade.YIELD);
        game.tick(0.1);
        assertEquals(yield, game.lineLevel(Game.LineUpgrade.YIELD));
        assertTrue(game.setDimensionAutomationEnabled(DimensionAutomation.LINE_YIELD, true));
        game.setAutomationPaused(true);
        game.tick(0.1);
        assertEquals(yield, game.lineLevel(Game.LineUpgrade.YIELD), "tout coupé");
        game.setAutomationPaused(false);
        game.tick(0.1);
        assertTrue(game.lineLevel(Game.LineUpgrade.YIELD) > yield);
    }

    @Test
    void laViseeDuPlanRamasseSeule() {
        Game game = four();
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.PLANE_AIM));
        double before = game.state().planeShardsEarned();
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertTrue(game.state().planeShardsEarned() > before + 5, "une minute : " + (game.state().planeShardsEarned() - before));
        Game idle = four();
        double still = idle.state().planeShardsEarned();
        for (int i = 0; i < 1_200; i++) idle.tick(0.05);
        assertTrue(idle.state().planeShardsEarned() - still <= 1, "sans elle, le point ne bouge pas");
    }

    @Test
    void laPlongeeDuVolumeCueilleSeule() {
        Game game = four();
        buyAll(game, 2);
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.VOLUME_DIVE));
        double before = game.state().volumeShardsEarned();
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertTrue(game.volumeDives() >= 2, "elle replonge : " + game.volumeDives() + " plongées en une minute");
        assertTrue(game.state().volumeShardsEarned() > before + 10, "des sphères : " + (game.state().volumeShardsEarned() - before));
    }

    @Test
    void lAppuiDeLaDureeSeTientEtSeRelacheSeul() {
        Game game = four();
        buyAll(game, 3);
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.DURATION_HOLD));
        double max = game.durationMaxSeconds();
        double before = game.state().durationShardsEarned();
        game.tick(0.05);
        assertTrue(game.isDurationHeld());
        game.tick(max * 3);
        assertEquals(3, game.durationReleases(), "un gros pas fait ses trois relâchements d'un coup");
        assertEquals(before + 3 * game.durationRewardFor(max), game.state().durationShardsEarned(), 1e-6);
        // Le bouton reste tenu : ses améliorations s'achètent quand même, à la main comme par leurs automatismes.
        assertTrue(game.isDurationHeld());
        assertTrue(game.canBuyDuration(Game.DurationUpgrade.VALUE));
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() == 4 && automation != DimensionAutomation.DURATION_HOLD) assertTrue(game.buyDimensionAutomation(automation));
        }
        int value = game.durationLevel(Game.DurationUpgrade.VALUE);
        game.state().setDurationShards(1e30);
        game.tick(0.05);
        assertTrue(game.durationLevel(Game.DurationUpgrade.VALUE) > value, "la valeur s'achète pendant l'appui");
    }

    @Test
    void ilsTraversentLesUniversEtSeSauvent() throws Exception {
        Game game = four();
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.LINE_YIELD));
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.PLANE_AIM));
        game.setDimensionAutomationEnabled(DimensionAutomation.PLANE_AIM, false);
        assertTrue(game.universeTransition(true).kept().stream().anyMatch(line -> line.startsWith("Les automatismes des dimensions (2")));
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertTrue(game.ownsDimensionAutomation(DimensionAutomation.LINE_YIELD) && game.ownsDimensionAutomation(DimensionAutomation.PLANE_AIM));
        assertFalse(game.isDimensionAutomationEnabled(DimensionAutomation.PLANE_AIM), "coupé, il le reste");
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(game.state().dimensionAutomations(), back.dimensionAutomations());
        assertEquals(game.state().dimensionAutomationsOff(), back.dimensionAutomationsOff());
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("dimensionAutomations")) old.append(line).append('\n');
        assertTrue(SaveCodec.read(old.toString()).state().dimensionAutomations().isEmpty(), "une sauvegarde d'avant n'en a pas");
        game.reset();
        assertTrue(game.state().dimensionAutomations().isEmpty(), "« Recommencer de zéro » les reprend");
    }

    /** Version 0.155 : chaque automatisme s'achète, payé avec la ressource de la dimension d'avant (la brane paie le clicker). */
    @Test
    void tousLesAutomatismesSAchetentAvecLaBonneRessource() {
        Game game = GameTestSupport.universe(8);
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        game.state().setUniverseSeeds(BigNum.of(1_000));
        game.state().setDimensionShards(1e30);
        game.state().setPlaneShards(1e30);
        game.state().setVolumeShards(1e30);
        game.state().setDurationShards(1e30);
        game.state().setLoopShards(1e30);
        game.state().setBraneShards(1e30);
        game.state().setClickerShards(1e30);
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            double before = game.dimensionAutomationBalance(automation);
            assertTrue(game.buyDimensionAutomation(automation), automation.toString());
            assertEquals(before - automation.cost(), game.dimensionAutomationBalance(automation), before * 1e-12 + 1e-9, automation.toString());
        }
        assertEquals(DimensionAutomation.values().length, game.dimensionAutomationsOwned());
    }
}
