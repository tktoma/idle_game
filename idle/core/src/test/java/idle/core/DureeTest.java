package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.DurationUpgrade;
import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Version 0.134 : la durée, quatrième dimension. Un bouton au milieu d'un rond : tant qu'on le tient, les spirales
 * avancent vers le centre, qu'elles atteignent au temps au plus ; au relâchement, chaque spirale donne ses éclats,
 * d'autant plus qu'on a tenu longtemps. Quatre améliorations : vitesse, nombre de spirales, valeur, temps au plus.
 */
class DureeTest {

    /** Une partie au quatrième univers : la quatrième dimension est ouverte. */
    static Game fourth() {
        Game game = new Game();
        game.start();
        DeuxiemeUniversTest.formUniverse(game);
        for (int k = 0; k < 3; k++) assertTrue(game.testAddUniverse());
        assertEquals(4, game.universes());
        return game;
    }

    /** Tient le bouton {@code seconds} secondes, par petits pas, puis le relâche ; rend ce qu'il a donné. */
    private static double hold(Game game, double seconds) {
        assertTrue(game.holdDuration());
        double left = seconds;
        while (left > 1e-9) {
            double step = Math.min(0.25, left);
            game.tick(step);
            left -= step;
        }
        return game.releaseDuration();
    }

    @Test
    void laDureeSOuvreAuQuatriemeUnivers() {
        Game game = new Game();
        game.start();
        DeuxiemeUniversTest.formUniverse(game);
        assertTrue(game.testAddUniverse());
        assertTrue(game.testAddUniverse());
        assertEquals(3, game.universes());
        assertFalse(game.isDurationOpen(), "trois univers : la quatrième dimension est fermée");
        assertFalse(game.holdDuration(), "on ne tient pas le bouton d'une dimension fermée");
        assertEquals(0, game.releaseDuration());
        assertTrue(game.testAddUniverse());
        assertTrue(game.isDurationOpen());
        assertTrue(game.holdDuration());
        assertTrue(game.isDurationHeld());
        assertFalse(game.holdDuration(), "déjà tenu");
    }

    @Test
    void lesSpiralesAvancentVersLeCentreEtSArretentAuTempsAuPlus() {
        Game game = fourth();
        assertEquals(6, game.durationMaxSeconds(), 1e-12, "six secondes au départ");
        assertEquals(0.5, game.durationTurnsPerSecond(), 1e-12);
        assertEquals(3, game.durationTurnsMax(), 1e-12, "trois tours du bord au centre");
        assertEquals(0, game.durationProgress());
        assertTrue(game.holdDuration());
        game.tick(3);
        assertEquals(3, game.durationHeldSeconds(), 1e-9);
        assertEquals(0.5, game.durationProgress(), 1e-9, "à mi-chemin");
        game.tick(10);
        assertEquals(6, game.durationHeldSeconds(), 1e-9, "tenir plus longtemps n'avance plus");
        assertEquals(1, game.durationProgress(), 1e-12, "au centre");
        assertEquals(game.durationRewardFor(6), game.durationPending(), 1e-9);
        assertEquals(6, game.releaseDuration(), 1e-9, "trois tours : 1 + 2 + 3");
        assertFalse(game.isDurationHeld());
        assertEquals(0, game.durationProgress(), "les spirales repartent du bord");
    }

    @Test
    void chaqueTourVautUnDePlusQueLePrecedent() {
        Game game = fourth();
        // n tours : (n² + n) ÷ 2 fois la valeur, et c'est continu entre deux tours.
        assertEquals(0, game.durationRewardFor(0), 1e-12);
        assertEquals(1, game.durationRewardFor(2), 1e-12, "un tour : 1");
        assertEquals(3, game.durationRewardFor(4), 1e-12, "deux tours : 1 + 2");
        assertEquals(0.375, game.durationRewardFor(1), 1e-12, "un demi-tour");
        assertEquals(6, game.durationRewardFor(60), 1e-12, "au-delà du temps au plus, rien de plus");
        // Tenir longtemps rapporte bien plus que plusieurs appuis courts.
        double once = game.durationRewardFor(6), sixShort = 6 * game.durationRewardFor(1);
        assertTrue(once > 2.5 * sixShort, once + " contre " + sixShort);
        assertEquals(0, game.durationRewardFor(-3), 1e-12);
    }

    @Test
    void relacherDonneLesEclatsDeLaDuree() {
        Game game = fourth();
        double reward = hold(game, 4);
        assertEquals(3, reward, 1e-9, "quatre secondes : deux tours");
        assertEquals(3, game.state().durationShards(), 1e-9);
        assertEquals(3, game.state().durationShardsEarned(), 1e-9);
        assertEquals(1, game.durationReleases());
        assertEquals(3, game.durationLastReward(), 1e-9);
        assertEquals(0, game.releaseDuration(), "rien de tenu, rien à donner");
        // Un appui aussitôt relâché ne donne rien et ne compte pas.
        assertTrue(game.holdDuration());
        assertEquals(0, game.releaseDuration());
        assertEquals(1, game.durationReleases());
    }

    @Test
    void lesQuatreAmeliorations() {
        Game game = fourth();
        game.state().setDurationShards(1e9);
        assertEquals(3, game.durationCost(DurationUpgrade.SPEED), 1e-12);
        assertTrue(game.buyDuration(DurationUpgrade.SPEED));
        assertEquals(0.6, game.durationTurnsPerSecond(), 1e-12, "+20 %");
        assertEquals(7.2, game.durationCost(DurationUpgrade.SPEED), 1e-9, "×2,4 par niveau");
        assertTrue(game.buyDuration(DurationUpgrade.COUNT));
        assertEquals(2, game.durationSpirals());
        assertTrue(game.buyDuration(DurationUpgrade.VALUE));
        assertEquals(2, game.durationValue(), 1e-12);
        assertTrue(game.buyDuration(DurationUpgrade.TIME));
        assertEquals(8, game.durationMaxSeconds(), 1e-12, "+2 s au temps au plus");
        // Deux spirales, valeur 2, 0,6 tour par seconde pendant 8 s : 4,8 tours chacune.
        double turns = 0.6 * 8;
        assertEquals(2 * 2 * (turns * turns + turns) / 2, game.durationRewardFor(100), 1e-9);
        assertEquals(1, game.durationLevel(DurationUpgrade.TIME));
    }

    @Test
    void lesAmeliorationsSAchetentBoutonRelacheEtOntUnPlafond() {
        Game game = fourth();
        game.state().setDurationShards(1e30);
        assertTrue(game.holdDuration());
        assertFalse(game.canBuyDuration(DurationUpgrade.VALUE), "pas pendant qu'on tient le bouton");
        assertFalse(game.buyDuration(DurationUpgrade.VALUE));
        game.releaseDuration();
        assertTrue(game.buyDuration(DurationUpgrade.VALUE));
        for (DurationUpgrade upgrade : DurationUpgrade.values()) {
            if (upgrade.max() == Integer.MAX_VALUE) continue;
            game.state().setDurationLevel(upgrade.ordinal(), upgrade.max());
            assertFalse(game.canBuyDuration(upgrade), upgrade + " au plafond");
            assertEquals(Double.POSITIVE_INFINITY, game.durationCost(upgrade));
        }
        assertEquals(10, game.durationSpirals(), "dix spirales au plus");
        assertEquals(46, game.durationMaxSeconds(), 1e-12, "46 s au plus");
        game.state().setDurationShards(0);
        assertFalse(game.buyDuration(DurationUpgrade.VALUE), "pas assez d'éclats");
        Game closed = new Game();
        closed.start();
        closed.state().setDurationShards(1e9);
        assertFalse(closed.buyDuration(DurationUpgrade.VALUE), "dimension fermée");
    }

    @Test
    void uneAbsenceRelacheLeBoutonEtDonneCeQuIlAvait() {
        Game game = fourth();
        assertTrue(game.holdDuration());
        game.tick(2);
        double pending = game.durationPending();
        game.tickAway(3600);
        assertFalse(game.isDurationHeld());
        assertEquals(pending, game.state().durationShardsEarned(), 1e-9, "ce qui était tenu, pas une heure de plus");
    }

    @Test
    void unePartieRelueNeReprendPasLeBoutonTenu() throws Exception {
        Game game = fourth();
        assertTrue(game.holdDuration());
        game.tick(3);
        GameState read = SaveCodec.read(SaveCodec.write(game.state(), 0)).state();
        Game back = new Game(read, idle.core.upgrades.Upgrades.DEFAULT);
        back.restored();
        assertFalse(back.isDurationHeld());
        game.restored();
        assertFalse(game.isDurationHeld(), "relue, la partie lâche le bouton sans rien donner");
        assertEquals(0, game.state().durationShardsEarned());
    }

    @Test
    void leSeuilDeLaDureeOuvreLePalierCinqDeLaGraine() {
        Game game = fourth();
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        game.state().setPlaneShardsEarned(Game.PLANE_THRESHOLD);
        game.state().setVolumeShardsEarned(Game.VOLUME_THRESHOLD);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(5));
        assertEquals(4, game.seedStage(), "sans le seuil de la durée, la graine reste au palier 4");
        assertFalse(game.canTendSeed());
        game.state().setDurationShardsEarned(Game.DURATION_THRESHOLD - 0.5);
        assertFalse(game.isDurationThresholdReached());
        game.state().setDurationShardsEarned(Game.DURATION_THRESHOLD);
        assertTrue(game.isDurationThresholdReached());
        assertTrue(game.isDimensionThresholdReached(4));
        assertEquals(5, game.seedStage(), "le seuil atteint, la graine passe au palier 5");
        assertFalse(game.isDimensionThresholdReached(5), "la dimension 5 n'a pas encore de seuil");
    }

    @Test
    void laDureeTraverseLesUniversEtSeRelit() throws Exception {
        Game game = fourth();
        game.state().setDurationShards(7);
        game.state().setDurationShardsEarned(40);
        game.state().setDurationLevel(DurationUpgrade.COUNT.ordinal(), 2);
        assertTrue(game.holdDuration());
        assertTrue(game.startNextUniverse());
        assertEquals(7, game.state().durationShards(), 1e-12, "les éclats de la durée traversent les univers");
        assertEquals(40, game.state().durationShardsEarned(), 1e-12);
        assertEquals(2, game.durationLevel(DurationUpgrade.COUNT));
        assertFalse(game.isDurationHeld(), "un nouvel univers ne commence pas bouton tenu");
        GameState read = SaveCodec.read(SaveCodec.write(game.state(), 0)).state();
        assertEquals(7, read.durationShards(), 1e-12);
        assertEquals(40, read.durationShardsEarned(), 1e-12);
        assertEquals(2, read.durationLevel(DurationUpgrade.COUNT.ordinal()));
    }

    @Test
    void uneSauvegardeDAvantLaDureePartDeZero() throws Exception {
        GameState read = SaveCodec.read(SaveCodec.write(new GameState(), 0)).state();
        assertEquals(0, read.durationShards());
        assertEquals(0, read.durationShardsEarned());
        for (int i = 0; i < DurationUpgrade.values().length; i++) assertEquals(0, read.durationLevel(i));
    }

    @Test
    void leGuideDeLUniversSuivantDitQueLaDureeReste() {
        Game game = fourth();
        assertTrue(game.universeTransition(true).kept().stream().anyMatch(line -> line.contains("de la durée")));
    }

    @Test
    void lOutilDEssaiRemetLaDureeAZero() {
        Game game = fourth();
        game.state().setDurationShards(12);
        game.state().setDurationShardsEarned(12);
        game.state().setDurationLevel(0, 3);
        assertTrue(game.holdDuration());
        game.testResetUniverses();
        assertEquals(0, game.state().durationShards());
        assertEquals(0, game.state().durationShardsEarned());
        assertEquals(0, game.durationLevel(DurationUpgrade.SPEED));
        assertFalse(game.isDurationHeld());
    }
}
