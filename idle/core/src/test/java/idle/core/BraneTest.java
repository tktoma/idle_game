package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameBrane.BraneUpgrade;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.154 : la brane, sixième dimension. On tire la membrane, une onde en part, grandit jusqu'à sa portée et
 * rebondit sur les bords ; chaque point qu'elle touche donne des ondes, plus après un rebond, plus pour chaque point de
 * plus touché par la même onde. Et la lumière astrale, qui multiplie l'arrosage par seconde.
 */
class BraneTest {

    private static Game six() {
        Game game = GameTestSupport.universe(6);
        game.testClearBrane();
        return game;
    }

    private static void run(Game game, double seconds) {
        for (double t = 0; t < seconds; t += 0.05) game.tick(0.05);
    }

    @Test
    void laBraneSOuvreAuSixiemeUnivers() {
        assertFalse(GameTestSupport.universe(5).isBraneOpen());
        Game game = six();
        assertTrue(game.isBraneOpen());
        assertEquals("ondes", Game.dimensionResourceName(6));
        assertEquals(GameBrane.BRANE_REACH, game.braneMaxReach(), 1e-12, "petites au début");
        assertEquals(GameBrane.BRANE_REACH * GameBrane.BRANE_MIN_PULL, game.braneReach(0), 1e-12, "même tirée à peine");
    }

    @Test
    void uneOndeToucheLesPointsDeSaPortee() {
        Game game = six();
        game.addBranePoint(0.2, 0);
        game.addBranePoint(0.9, 0.9);
        assertTrue(game.pullBrane(0, 0, 1));
        game.tick(0.01);
        assertEquals(0, game.braneTouches(), "le front n'y est pas encore");
        run(game, 1);
        assertEquals(1, game.braneTouches(), "le point proche, pas l'autre, hors de portée");
        assertEquals(1, game.state().braneShards(), 1e-9);
        assertTrue(game.braneWaves().isEmpty(), "l'onde s'éteint à sa portée");
        assertEquals(1, game.branePoints().size(), "le point touché disparaît");
    }

    @Test
    void lesRebondsRetrouventLesPointsEtLaSuiteRapportePlus() {
        Game game = six();
        game.state().setBraneLevel(BraneUpgrade.SIZE.ordinal(), 3);
        assertEquals(0.665, game.braneMaxReach(), 1e-12);
        // Près du bord droit : l'onde touche le premier point en direct (0,10), puis renvoyée par le bord (0,40) ;
        // le second en direct (0,51), puis renvoyé (0,54).
        game.addBranePoint(0.75, 0);
        game.addBranePoint(0.95, 0.5);
        assertTrue(game.pullBrane(0.85, 0, 1));
        run(game, 1.5);
        assertEquals(4, game.braneTouches(), "deux points, chacun deux fois");
        assertEquals(2, game.braneBounces());
        double expected = game.braneReward(1, 0) + game.braneReward(2, 1) + game.braneReward(3, 0) + game.braneReward(4, 1);
        assertEquals(expected, game.state().braneShards(), 1e-9);
        assertEquals(1 + 1.5 * 1.1 + 1.2 + 1.5 * 1.3, expected, 1e-9);
        assertTrue(game.branePoints().isEmpty(), "les points touchés partent avec l'onde");
    }

    @Test
    void unCoinCompteDeuxRebonds() {
        Game game = six();
        assertEquals(GameBrane.BRANE_BOUNCE, game.braneReward(1, 1) / game.braneReward(1, 0), 1e-12);
        assertEquals(GameBrane.BRANE_BOUNCE * GameBrane.BRANE_BOUNCE, game.braneReward(1, 2) / game.braneReward(1, 0), 1e-12);
        game.state().setBraneLevel(BraneUpgrade.SIZE.ordinal(), 5);
        // Dans le coin, le point posé sur la source : direct à 0, par chaque bord à 0,2, par le coin à 0,28.
        game.addBranePoint(0.9, 0.9);
        assertTrue(game.pullBrane(0.9, 0.9, 1));
        run(game, 1.9);
        assertEquals(4, game.braneTouches(), "direct, deux bords, le coin");
        double expected = game.braneReward(1, 0) + game.braneReward(2, 1) + game.braneReward(3, 1) + game.braneReward(4, 2);
        assertEquals(expected, game.state().braneShards(), 1e-9);
    }

    @Test
    void horsDePorteeRienNeRevient() {
        Game game = six();
        // Portée 0,35 : le bord est à 0,4, aucun rebond ne revient.
        game.addBranePoint(0.5, 0);
        assertTrue(game.pullBrane(0.6, 0, 1));
        run(game, 1);
        assertEquals(1, game.braneTouches());
        assertEquals(0, game.braneBounces());
        assertEquals(1, game.state().braneShards(), 1e-9);
    }

    /** Version 0.155 : une seule onde à la fois. */
    @Test
    void uneSeuleOndeALaFois() {
        Game game = six();
        assertTrue(game.pullBrane(0, 0, 1));
        assertFalse(game.pullBrane(0.5, 0.5, 1), "la précédente court encore");
        run(game, 1);
        assertTrue(game.braneWaves().isEmpty());
        assertTrue(game.pullBrane(0, 0, 1), "éteinte, la suivante peut partir");
    }

    /** Version 0.155 : l'onde rebondit plusieurs fois, et le point touché compte à chaque passage tant qu'elle court. */
    @Test
    void lOndeRebonditPlusieursFois() {
        Game game = six();
        game.state().setBraneLevel(BraneUpgrade.SIZE.ordinal(), BraneUpgrade.SIZE.max());
        assertEquals(0.35 * 8.5, game.braneMaxReach(), 1e-12);
        // D'un bord à l'autre : direct à 1,8 ; renvoyée par l'un des deux bords à 2,0 ; par les deux à 2,2.
        game.addBranePoint(0.9, 0);
        assertTrue(game.pullBrane(-0.9, 0, 1));
        run(game, 5.2);
        assertTrue(game.braneMostBounces() >= 2, "deux rebonds au moins : " + game.braneMostBounces());
        assertTrue(game.braneTouches() >= 4, "le point compte à chaque passage : " + game.braneTouches());
        assertFalse(game.branePoints().contains(new GameBrane.BranePoint(0.9, 0)), "il part avec l'onde");
        double[][] xs = GameBrane.images(0.25);
        assertEquals(10, xs.length);
        for (double[] image : xs) {
            double d = Math.abs(image[0] - 0.25);
            assertEquals(image[1] == 0, d < 1e-12, "seule l'onde même est sans rebond");
        }
    }

    @Test
    void lesAmeliorationsChangentLesOndes() {
        Game game = six();
        game.state().setBraneShards(1e12);
        for (BraneUpgrade upgrade : BraneUpgrade.values()) assertTrue(game.buyBrane(upgrade), upgrade.toString());
        assertEquals(2 * game.hadronBoost(idle.core.collider.Hadron.OMEGA), game.braneValue(), 1e-9);
        assertEquals(GameBrane.BRANE_REACH * 1.3, game.braneMaxReach(), 1e-12);
        assertEquals(game.braneMaxReach() * 0.6, game.braneReach(0.5), 1e-12, "élasticité : une demi-tirée vaut 60 %");
        assertEquals(1.75, game.braneBounceFactor(), 1e-12);
        assertEquals(0.15, game.braneComboStep(), 1e-12);
    }

    @Test
    void lAutomatismeTireSeulEtLesPointsReviennent() {
        Game game = six();
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.BRANE_PULL.name());
        run(game, 60);
        assertTrue(game.braneWavesSent() >= 15, "une onde toutes les trois secondes : " + game.braneWavesSent());
        assertTrue(game.braneTouches() > 0 && game.state().braneShardsEarned() > 0);
        assertEquals(44, DimensionAutomation.values().length, "six de plus pour la brane, puis neuf pour le clicker");
        assertEquals(5, DimensionAutomation.BRANE_PULL.paidWith(), "payés en nœuds de la boucle");
    }

    @Test
    void laLumiereAstraleMultiplieLArrosage() {
        Game game = six();
        double rate = game.wateringCanRate(), hose = game.hoseRate();
        game.state().setSeedUpgradeLevel(SeedUpgrade.ASTRAL_LIGHT.ordinal(), 3);
        assertEquals(rate * 1.6, game.wateringCanRate(), 1e-9);
        assertEquals(hose * 1.6, game.hoseRate(), 1e-9);
        assertEquals(6, SeedUpgrade.ASTRAL_LIGHT.dimension(), "payée en ondes");
    }

    @Test
    void laBraneTraverseLesUniversEtSeSauve() throws Exception {
        Game game = six();
        game.state().setBraneShards(1e6);
        assertTrue(game.buyBrane(BraneUpgrade.BOUNCE));
        game.state().setBraneShardsEarned(GameUniverse.BRANE_THRESHOLD);
        assertTrue(game.isDimensionThresholdReached(6));
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.BRANE_SURFER));
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(1, back.braneLevel(BraneUpgrade.BOUNCE.ordinal()));
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("brane")) old.append(line).append('\n');
        assertEquals(0, SaveCodec.read(old.toString()).state().braneShards(), 0);
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertEquals(1, game.braneLevel(BraneUpgrade.BOUNCE));
    }
}
