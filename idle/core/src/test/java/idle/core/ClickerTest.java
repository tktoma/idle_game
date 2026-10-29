package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameClicker.ClickerUpgrade;
import idle.core.GameClicker.Weather;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.155 : le clicker, septième dimension. Plus on clique vite, plus chaque clic rapporte ; les zones ×2, qui
 * peuvent se superposer, rapportent davantage. Et la météo de la graine : beau temps, ciel variable, orage.
 */
class ClickerTest {

    private static Game seven() {
        Game game = GameTestSupport.universe(7);
        game.testClearClicker();
        return game;
    }

    private static void run(Game game, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) game.tick(0.05);
    }

    @Test
    void leClickerSOuvreAuSeptiemeUnivers() {
        assertFalse(GameTestSupport.universe(6).isClickerOpen());
        Game game = seven();
        assertTrue(game.isClickerOpen());
        assertEquals("étincelles", Game.dimensionResourceName(7));
        assertEquals(0, game.clickerMaxZones(), "les zones se débloquent");
        assertEquals(1 + 0.05 * 0.5, game.clickClicker(0.5, 0.5), 1e-9, "un clic : un demi-clic par seconde de cadence");
        assertEquals(game.clickClicker(0.5, 0.5), game.state().clickerShards() - (1 + 0.05 * 0.5), 1e-9);
    }

    @Test
    void cliquerViteRapportePlus() {
        Game game = seven();
        double slow = 0;
        for (int i = 0; i < 10; i++) {
            slow += game.clickClicker(0.5, 0.5);
            game.tick(2.1);
        }
        game.testClearClicker();
        double fast = 0;
        for (int i = 0; i < 10; i++) {
            fast += game.clickClicker(0.5, 0.5);
            game.tick(0.1);
        }
        assertTrue(fast > slow * 1.1, "vite : " + fast + ", lentement : " + slow);
        assertEquals(1 + 0.05 * GameClicker.CLICKER_MAX_RATE, game.clickerSpeedFactor(1_000), 1e-12, "la cadence plafonne");
    }

    @Test
    void lesZonesSuperposeesMultiplient() {
        Game game = seven();
        game.state().setClickerLevel(ClickerUpgrade.ZONES.ordinal(), 1);
        game.addClickerZone(0.5, 0.5);
        game.addClickerZone(0.55, 0.5);
        assertEquals(2, game.clickerStackAt(0.52, 0.5));
        assertEquals(1, game.clickerStackAt(0.42, 0.5));
        assertEquals(0, game.clickerStackAt(0.1, 0.1));
        assertEquals(2 * 1.5, game.clickerStackMultiplier(2), 1e-12);
        double base = game.clickerGainAt(0.1, 0.1);
        assertEquals(base * 3, game.clickerGainAt(0.52, 0.5), 1e-9);
        double[] spot = game.clickerBestSpot();
        assertEquals(2, game.clickerStackAt(spot[0], spot[1]), "le meilleur endroit est sous les deux");
        game.clickClicker(0.52, 0.5);
        assertEquals(2, game.clickerBestStack());
    }

    @Test
    void lesZonesApparaissentEtSEteignent() {
        Game game = seven();
        game.state().setClickerLevel(ClickerUpgrade.ZONES.ordinal(), 1);
        game.state().setClickerLevel(ClickerUpgrade.ZONE_COUNT.ordinal(), 3);
        game.state().setClickerLevel(ClickerUpgrade.ZONE_TIME.ordinal(), 10);
        assertEquals(4, game.clickerMaxZones());
        assertEquals(19, game.clickerZoneSeconds(), 1e-12);
        run(game, 3.05);
        assertEquals(1, game.clickerZones().size());
        run(game, 12);
        assertEquals(4, game.clickerZones().size(), "jamais plus que le nombre au plus");
        for (GameClicker.ClickerZone zone : game.clickerZones()) {
            assertTrue(zone.x() >= zone.radius() - 1e-9 && zone.x() <= 1 - zone.radius() + 1e-9, "sur la surface");
        }
        Game short_ = seven();
        short_.state().setClickerLevel(ClickerUpgrade.ZONES.ordinal(), 1);
        short_.addClickerZone(0.5, 0.5);
        run(short_, 4.1);
        assertTrue(short_.clickerZones().stream().noneMatch(z -> z.x() == 0.5 && z.y() == 0.5), "4 s et elle s'éteint");
    }

    @Test
    void lesAmeliorationsChangentLeClicker() {
        Game game = seven();
        game.state().setClickerShards(1e15);
        assertFalse(game.canBuyClicker(ClickerUpgrade.ZONE_MULT), "pas avant les zones");
        for (ClickerUpgrade upgrade : ClickerUpgrade.values()) assertTrue(game.buyClicker(upgrade), upgrade.toString());
        assertFalse(game.canBuyClicker(ClickerUpgrade.ZONES), "une seule fois");
        assertEquals(2 * game.hadronBoost(idle.core.collider.Hadron.OMEGA), game.clickerValue(), 1e-9);
        assertEquals(0.075, game.clickerSpeedStep(), 1e-12);
        assertEquals(2.5, game.clickerZoneFactor(), 1e-12);
        assertEquals(5.5, game.clickerZoneSeconds(), 1e-12);
        assertEquals(2, game.clickerMaxZones());
        assertEquals(0.11, game.clickerZoneRadius(), 1e-12);
        assertEquals(1.75, game.clickerStackFactor(), 1e-12);
    }

    @Test
    void lAutomatismeCliqueVraimentEtEnAbsence() {
        Game game = seven();
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.CLICKER_PLAY.name());
        run(game, 10);
        assertTrue(Math.abs(game.clickerAutoClicks() - 60) <= 2, "six par seconde : " + game.clickerAutoClicks());
        assertEquals(6, game.clickerRate(), 0.6);
        double before = game.state().clickerShardsEarned();
        game.tick(10);
        assertTrue(Math.abs(game.clickerAutoClicks() - 120) <= 3, "une absence les compte aussi : " + game.clickerAutoClicks());
        assertEquals(60 * game.clickerValue() * game.clickerSpeedFactor(6), game.state().clickerShardsEarned() - before, 1e-6);
        assertEquals(9, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 7).count());
        assertEquals(6, DimensionAutomation.CLICKER_PLAY.paidWith(), "payés en ondes de la brane");
        assertEquals(2_000, DimensionAutomation.CLICKER_PLAY.cost(), 0);
    }

    @Test
    void laMeteoChangeLArrosage() {
        Game game = seven();
        assertTrue(game.isWeatherUnlocked());
        assertFalse(GameTestSupport.universe(6).isWeatherUnlocked(), "pas avant le clicker");
        double rate = game.wateringCanRate();
        game.testSetWeather(Weather.SUN);
        assertEquals(rate * 1.3, game.wateringCanRate(), 1e-9, "beau temps : +30 %");
        game.testSetWeather(Weather.STORM);
        assertEquals(rate * 0.9, game.wateringCanRate(), 1e-9, "orage : un léger malus");
        assertEquals(30, game.weatherLeft(), 1e-9);
        game.state().setSeedUpgradeLevel(SeedUpgrade.WEATHER_STORM.ordinal(), 4);
        game.state().setSeedUpgradeLevel(SeedUpgrade.WEATHER_SUN.ordinal(), 2);
        game.state().setSeedUpgradeLevel(SeedUpgrade.WEATHER_POWER.ordinal(), 3);
        assertEquals(18, game.weatherStormSeconds(), 1e-9);
        assertEquals(60, game.weatherSunSeconds(), 1e-9);
        game.testSetWeather(Weather.SUN);
        assertEquals(rate * 1.6, game.wateringCanRate(), 1e-9);
        assertEquals(7, SeedUpgrade.WEATHER_SUN.dimension(), "payées en étincelles");
        // Une heure : le ciel change, plus souvent beau qu'orageux.
        game.testSetWeather(Weather.VARIABLE);
        for (int i = 0; i < 3_600; i++) game.tick(1);
        assertTrue(game.sunnySpells() > game.storms() && game.storms() > 0, game.sunnySpells() + " beaux temps, " + game.storms() + " orages");
    }

    @Test
    void leClickerTraverseLesUniversEtSeSauve() throws Exception {
        Game game = seven();
        game.state().setClickerShards(1e6);
        assertTrue(game.buyClicker(ClickerUpgrade.GAIN));
        game.state().setClickerShardsEarned(GameUniverse.CLICKER_THRESHOLD);
        assertTrue(game.isDimensionThresholdReached(7));
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.CLICK_VIRTUOSO));
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(1, back.clickerLevel(ClickerUpgrade.GAIN.ordinal()));
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("clicker")) old.append(line).append('\n');
        assertEquals(0, SaveCodec.read(old.toString()).state().clickerShards(), 0);
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertEquals(1, game.clickerLevel(ClickerUpgrade.GAIN));
    }
}
