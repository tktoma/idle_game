package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.GameUniverse.TreeBubble;
import idle.core.universe.DimensionAutomation;
import idle.ui.FxTestSupport;
import idle.ui.Version;
import idle.ui.automation.DimensionAutomationPane;
import idle.ui.common.Card;
import idle.ui.pages.StatsPage;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.155 : le clicker, septième dimension, et la météo de la graine, sous Xvfb. La surface se clique à la souris ;
 * la cadence multiplie chaque clic ; les zones ×2, une fois débloquées, se superposent et se colorent selon leur nombre,
 * le multiplicateur s'écrit sous la souris ; les améliorations s'achètent dans la fenêtre ; les clics automatiques
 * jouent seuls. La météo se voit dans la vue de la graine et change l'arrosage ; ses trois améliorations sont dans la
 * branche « Clicker » ; le clicker a sa section dans l'automatisation, ses lignes dans les statistiques et son rond vert.
 */
class ClickerPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au septième univers : le clicker est ouvert. */
    private static Game seventh() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 6; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isClickerOpen());
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.CLICKER);
        window.frame(0.016, true);
        return window;
    }

    private static void run(Game game, DimensionWindow window, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) {
            game.tick(0.05);
            window.frame(0.05, true);
        }
    }

    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type, double x, double y) {
        return new MouseEvent(type, x, y, x, y, type == MouseEvent.MOUSE_MOVED ? MouseButton.NONE : MouseButton.PRIMARY, 1,
                false, false, false, false, type == MouseEvent.MOUSE_PRESSED, false, false, false, false, false, null);
    }

    /** Un clic à la souris en ce point de la surface (0 à 1). */
    private static void click(DimensionView view, double x, double y) {
        double[] at = view.clickerScreen(x, y);
        view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, at[0], at[1]));
        view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, at[0], at[1]));
    }

    @Test
    void unClicDonneDesEtincellesEtLaCadenceLesMultiplie() {
        FxTestSupport.fx(() -> {
            Game game = seventh();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals("Dimension 7 : Le clicker", window.titleText());
            assertEquals("Étincelles du clicker : 0", window.clickerShardsText());
            assertTrue(window.clickerThresholdText().startsWith("Seuil : 0 / "), window.clickerThresholdText());
            assertTrue(window.clickerHintText().startsWith("Cliquez la surface"), window.clickerHintText());
            click(view, 0.5, 0.5);
            assertEquals(1 + Game.CLICKER_SPEED * 0.5, game.state().clickerShards(), 1e-9, "un clic, à une demi-cadence");
            assertEquals("+1", view.lastClickerMark());
            assertEquals(1, view.clickerMarksShown());
            for (int k = 0; k < 9; k++) click(view, 0.3, 0.6);
            assertEquals(5, game.clickerRate(), 1e-9, "dix clics en deux secondes");
            window.frame(0.016, true);
            assertTrue(window.clickerHintText().contains("5 clics par seconde, ×1.25"), window.clickerHintText());
            assertTrue(window.clickerHintText().endsWith("Dernier clic : +" + idle.ui.common.Format.darkMatter(game.clickerLastGain()) + "."),
                    window.clickerHintText());
            double[] outside = {view.getWidth() - 2, 2};
            assertFalse(view.press(outside[0], outside[1], false), "hors de la surface : rien");
            run(game, window, 2.5);
            assertEquals(0, game.clickerRate(), 1e-9, "la cadence retombe");
            assertEquals(0, view.clickerMarksShown(), "les gains s'effacent");
            return null;
        });
    }

    @Test
    void lesZonesSeSuperposentEtSeColorentSelonLeurNombre() {
        FxTestSupport.fx(() -> {
            Game game = seventh();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            game.state().setClickerShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.clickerButton(Game.ClickerUpgrade.ZONE_MULT).isDisable(), "sans les zones, pas leur multiplicateur");
            assertTrue(window.clickerButton(Game.ClickerUpgrade.ZONE_MULT).getText().contains("(après « Zones ×2 »)"),
                    window.clickerButton(Game.ClickerUpgrade.ZONE_MULT).getText());
            window.clickerButton(Game.ClickerUpgrade.ZONES).fire();
            assertTrue(game.areClickerZonesUnlocked());
            assertTrue(window.clickerButton(Game.ClickerUpgrade.ZONES).getText().endsWith("Au maximum"));
            assertFalse(window.clickerButton(Game.ClickerUpgrade.ZONE_MULT).isDisable());
            game.testClearClicker();
            game.addClickerZone(0.5, 0.5);
            game.addClickerZone(0.55, 0.5);
            assertEquals(2, game.clickerStackAt(0.52, 0.5));
            assertNotEquals(DimensionView.stackColor(1), DimensionView.stackColor(2), "chaque nombre de zones a sa couleur");
            double[] at = view.clickerScreen(0.52, 0.5);
            view.getOnMouseMoved().handle(mouse(MouseEvent.MOUSE_MOVED, at[0], at[1]));
            assertEquals(2, view.clickerHoverStack(), "sous la souris, deux zones");
            double expected = game.clickerGainAt(0.52, 0.5);
            assertEquals(game.clickerValue() * game.clickerSpeedFactor(0.5) * 3, expected, 1e-9, "×2 puis ×1,5");
            click(view, 0.52, 0.5);
            assertEquals(expected, game.clickerLastGain(), 1e-9);
            assertEquals(2, game.clickerBestStack());
            view.getOnMouseExited().handle(mouse(MouseEvent.MOUSE_MOVED, -1, -1));
            assertEquals(-1, view.clickerHoverStack());
            run(game, window, game.clickerZoneSeconds() + 0.2);
            assertTrue(game.clickerZones().size() <= game.clickerMaxZones(), "elles s'éteignent, d'autres viennent");
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentEtLesClicsAutomatiquesJouentSeuls() {
        FxTestSupport.fx(() -> {
            Game game = seventh();
            DimensionWindow window = window(game);
            assertTrue(window.clickerButton(Game.ClickerUpgrade.GAIN).isDisable(), "sans étincelle, rien ne s'achète");
            game.state().setClickerShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.clickerButton(Game.ClickerUpgrade.GAIN).getText().startsWith("Gain : chaque clic vaut 1 → 2 étincelles"),
                    window.clickerButton(Game.ClickerUpgrade.GAIN).getText());
            assertTrue(window.clickerButton(Game.ClickerUpgrade.SPEED).getText().startsWith("Multiplicateur de cadence : +5 % → +7.5 %"),
                    window.clickerButton(Game.ClickerUpgrade.SPEED).getText());
            for (Game.ClickerUpgrade upgrade : Game.ClickerUpgrade.values()) window.clickerButton(upgrade).fire();
            assertEquals(2, game.clickerValue(), 1e-9);
            assertEquals(2.5, game.clickerZoneFactor(), 1e-9);
            assertEquals(2, game.clickerMaxZones());
            assertTrue(window.clickerButton(Game.ClickerUpgrade.STACK).getText().startsWith("Zones superposées : chaque zone de plus ×1.75 → ×2"),
                    window.clickerButton(Game.ClickerUpgrade.STACK).getText());
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setBraneShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.CLICKER_PLAY));
            window.frame(0.016, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.clickerHintText().startsWith("Clics automatiques"), window.clickerHintText());
            run(game, window, 1);
            assertTrue(game.clickerAutoClicks() >= 5, "six clics par seconde");
            assertTrue(window.view().clickerAutoFlashes() > 0 && window.view().clickerMarksShown() > 0, "qui se voient comme des clics");
            game.setDimensionAutomationEnabled(DimensionAutomation.CLICKER_PLAY, false);
            window.frame(0.016, true);
            assertTrue(window.autoBadgeText().isEmpty());
            return null;
        });
    }

    @Test
    void laMeteoSeVoitDansLaGraineEtSesAmeliorationsSontDansLaBrancheClicker() {
        FxTestSupport.fx(() -> {
            Game game = seventh();
            game.state().setWateringCan(true);
            UniversePage page = new UniversePage(game);
            new Scene(page, 1000, 700);
            page.select(UniversePage.SEED);
            page.refresh();
            page.frame(0.016, true);
            SeedView view = page.seedView();
            assertTrue(game.isWeatherUnlocked());
            assertEquals("Variable : 60 s", view.weatherText());
            double rate = game.wateringCanRate();
            game.testSetWeather(Game.Weather.SUN);
            page.frame(0.016, true);
            assertEquals("Beau temps +30 % : 40 s", view.weatherText());
            assertEquals(rate * 1.3, game.wateringCanRate(), 1e-9);
            game.testSetWeather(Game.Weather.STORM);
            page.frame(0.016, true);
            assertEquals("Orage −10 % : 30 s", view.weatherText());
            assertEquals(rate * 0.9, game.wateringCanRate(), 1e-9);
            SeedUpgradesPane upgrades = page.seedUpgrades();
            assertEquals("Clicker", SeedUpgradesPane.branchName(7));
            assertTrue(upgrades.isBranchOpen(7));
            upgrades.select(7);
            assertEquals("Étincelles du clicker : 0", upgrades.resourceText().getText());
            assertTrue(upgrades.button(SeedUpgrade.WEATHER_SUN).getText().startsWith("Beau temps plus long (niveau 0) : 40 → 50 s de soleil"),
                    upgrades.button(SeedUpgrade.WEATHER_SUN).getText());
            assertTrue(upgrades.button(SeedUpgrade.WEATHER_STORM).getText().startsWith("Orages plus courts (niveau 0) : 30 → 27 s d'orage"),
                    upgrades.button(SeedUpgrade.WEATHER_STORM).getText());
            assertTrue(upgrades.button(SeedUpgrade.WEATHER_POWER).getText().startsWith("Soleil plus fort (niveau 0) : beau temps, arrosage +30 % → +40 %"),
                    upgrades.button(SeedUpgrade.WEATHER_POWER).getText());
            game.state().setClickerShards(1e6);
            upgrades.refresh();
            upgrades.button(SeedUpgrade.WEATHER_SUN).fire();
            upgrades.button(SeedUpgrade.WEATHER_POWER).fire();
            assertEquals(50, game.weatherSunSeconds(), 1e-9);
            assertEquals(0.4, game.weatherSunGain(), 1e-9);
            assertEquals(14, SeedUpgrade.values().length);
            assertFalse(page.dimensionsView().isMarked(7));
            game.state().setClickerShardsEarned(Game.CLICKER_THRESHOLD);
            page.refresh();
            assertTrue(page.dimensionsView().isMarked(7), "le seuil atteint : le rond vert");
            return null;
        });
    }

    @Test
    void leClickerALaSectionDansLAutomatisationSesStatistiquesEtSaVersion() {
        FxTestSupport.fx(() -> {
            Game game = seventh();
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setBraneShards(1e9);
            DimensionAutomationPane pane = new DimensionAutomationPane(game);
            new Scene(pane, 1200, 760);
            pane.refresh();
            assertTrue(pane.sectionShown(7));
            assertEquals("Dimension 7 : Le clicker", pane.headingText(7));
            assertTrue(pane.balanceText(7).startsWith("Payés en ondes"), pane.balanceText(7));
            Card play = pane.card(DimensionAutomation.CLICKER_PLAY);
            assertEquals(Card.State.READY, play.state());
            assertTrue(play.text().contains("Clique 6 fois par seconde au meilleur endroit"), play.text());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.CLICKER_PLAY));
            assertEquals(44, DimensionAutomation.values().length);
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Clicker : étincelles : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Clicker : seuil : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Clics du clicker : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Météo de la graine : Variable")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.equals("Automatismes des dimensions : 1 / 44 achetés")), rows.toString());
            assertEquals("0.155", Version.CURRENT);
            return null;
        });
    }
}
