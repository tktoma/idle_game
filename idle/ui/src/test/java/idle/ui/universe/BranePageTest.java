package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Version 0.154 : la brane, sixième dimension, et la lumière astrale de la graine, sous Xvfb. La membrane se tire à la
 * souris (appui, glissé, relâché) : une onde part du point appuyé, touche les points en direct puis par ses rebonds sur
 * les bords, et chaque passage donne des ondes ; une seule onde à la fois (version 0.155), qui rebondit plusieurs fois
 * si elle porte loin, ses rebonds marqués d'un éclat et comptés dans le gain ; les améliorations s'achètent dans la
 * fenêtre ; la tirée automatique joue seule. La lumière astrale est dans la branche « Brane » de la graine ; la brane a
 * sa section dans l'automatisation, ses lignes dans les statistiques et son rond vert ; la version suit.
 */
class BranePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au sixième univers : la brane est ouverte. */
    private static Game sixth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 5; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isBraneOpen());
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.BRANE);
        window.frame(0.016, true);
        return window;
    }

    /** Fait passer le temps du jeu et de la fenêtre ensemble. */
    private static void run(Game game, DimensionWindow window, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) {
            game.tick(0.05);
            window.frame(0.05, true);
        }
    }

    /** Un geste de la souris (bouton gauche) en ce point de la vue, comme JavaFX le donne. */
    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type, double x, double y) {
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false,
                type != MouseEvent.MOUSE_RELEASED, false, false, false, false, false, null);
    }

    /** Tire la membrane à la souris : appui en ({@code fx}, {@code fy}), glissé jusqu'à ({@code tx}, {@code ty}), relâché. */
    private static void pull(DimensionView view, double fx, double fy, double tx, double ty) {
        double[] from = view.braneScreen(fx, fy), to = view.braneScreen(tx, ty);
        view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, from[0], from[1]));
        view.getOnMouseDragged().handle(mouse(MouseEvent.MOUSE_DRAGGED, to[0], to[1]));
        view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, to[0], to[1]));
    }

    @Test
    void laMembraneSeTireALaSourisEtLOndeDonneDesOndes() {
        FxTestSupport.fx(() -> {
            Game game = sixth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals("Dimension 6 : La brane", window.titleText());
            assertEquals("Ondes de la brane : 0", window.braneShardsText());
            assertTrue(window.braneThresholdText().startsWith("Seuil : 0 / "), window.braneThresholdText());
            assertTrue(window.braneHintText().startsWith("Appuyez sur la membrane"), window.braneHintText());
            assertTrue(window.autoBadgeText().isEmpty());
            game.testClearBrane();
            game.addBranePoint(0.1, 0);
            // L'appui, puis le glissé : la membrane se tend, la fenêtre dit la force et la portée.
            double[] from = view.braneScreen(0, 0), to = view.braneScreen(0.3, 0);
            view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, from[0], from[1]));
            assertTrue(view.braneHeld());
            view.getOnMouseDragged().handle(mouse(MouseEvent.MOUSE_DRAGGED, to[0], to[1]));
            assertEquals(0.5, view.branePull(), 1e-9, "un glissé de 0,3 : la moitié de la tirée à fond");
            window.frame(0.016, true);
            assertTrue(window.braneHintText().startsWith("Tirée à 50 % : l'onde portera à "), window.braneHintText());
            assertTrue(game.braneWaves().isEmpty(), "rien ne part tant qu'on tient");
            view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, to[0], to[1]));
            assertFalse(view.braneHeld());
            assertEquals(1, game.braneWaves().size(), "relâchée : une onde part");
            Game.BraneWave wave = game.braneWaves().get(0);
            assertEquals(0, wave.x(), 1e-9);
            assertEquals(game.braneReach(0.5), wave.reach(), 1e-9);
            run(game, window, 0.2);
            assertTrue(view.braneFrontsDrawn() >= 1, "l'onde se dessine");
            assertTrue(wave.hasStruck(game.branePoints().get(0)), "le point touché reste tant que l'onde court");
            assertEquals(1, game.state().braneShards(), 1e-9, "un passage : une onde");
            assertEquals("+1", view.lastBraneMark());
            run(game, window, 0.2);
            assertTrue(game.braneWaves().isEmpty(), "l'onde s'éteint à sa portée");
            assertTrue(window.braneHintText().endsWith("Dernier passage : +1."), window.braneHintText());
            assertEquals(0, view.braneFrontsDrawn());
            return null;
        });
    }

    @Test
    void lesRebondsComptentEtUneSeuleOndeALaFois() {
        FxTestSupport.fx(() -> {
            Game game = sixth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            game.testClearBrane();
            game.addBranePoint(0.9, 0);
            pull(view, 0.8, 0, 0.8, 0.6);
            assertEquals(game.braneMaxReach(), game.braneWaves().get(0).reach(), 1e-9, "tirée à fond");
            run(game, window, 0.7);
            // En direct (une onde), puis par le bord de droite : ×1,5, et +10 % pour le deuxième passage.
            assertEquals(2, game.braneTouches());
            assertEquals(1, game.braneBounces());
            assertEquals(1 + 1.5 * 1.1, game.state().braneShards(), 1e-9);
            assertTrue(view.lastBraneMark().endsWith(", rebond"), view.lastBraneMark());
            game.testClearBrane();
            assertEquals(1, Game.BRANE_MAX_WAVES);
            pull(view, 0, 0, 0, 0.6);
            assertEquals(1, game.braneWaves().size());
            double[] at = view.braneScreen(0.5, 0.5);
            assertFalse(view.press(at[0], at[1], false), "une onde court : la membrane ne se tient pas");
            window.frame(0.016, true);
            assertTrue(window.braneHintText().startsWith("Une onde court déjà"), window.braneHintText());
            assertFalse(view.press(view.getWidth() - 2, 2, false), "hors de la membrane : rien");
            return null;
        });
    }

    @Test
    void uneOndeQuiPorteLoinRebonditPlusieursFoisEtLePointComptePassages() {
        FxTestSupport.fx(() -> {
            Game game = sixth();
            game.state().setBraneLevel(Game.BraneUpgrade.SIZE.ordinal(), Game.BraneUpgrade.SIZE.max());
            assertEquals(2.975, game.braneMaxReach(), 1e-9);
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            game.testClearBrane();
            game.addBranePoint(0.5, 0);
            Game.BranePoint point = game.branePoints().get(0);
            pull(view, 0.9, 0, 0.9, 0.6);
            run(game, window, 3.6);
            // En direct, par le bord de droite, par ceux du haut et du bas, puis par les deux coins : six passages.
            assertEquals(1, game.braneWaves().size(), "l'onde court encore");
            assertTrue(game.branePoints().contains(point), "le point touché reste tant qu'elle court");
            assertTrue(view.branePasses(point) >= 6, "et compte ses passages : " + view.branePasses(point));
            assertTrue(game.braneMostBounces() >= 2, "deux rebonds en un passage");
            assertTrue(view.braneEdgeFlashes() > 0, "un éclat marque chaque bord où elle rebondit");
            assertTrue(view.braneFrontsDrawn() > 1, "ses rebonds se dessinent");
            assertEquals("+9, 2 rebonds", DimensionView.passText(9, 2));
            run(game, window, 2);
            assertTrue(game.braneWaves().isEmpty());
            assertFalse(game.branePoints().contains(point), "le point part avec l'onde");
            assertEquals(0, view.branePasses(point));
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentEtLaTireeAutomatiqueJoueSeule() {
        FxTestSupport.fx(() -> {
            Game game = sixth();
            DimensionWindow window = window(game);
            assertTrue(window.braneButton(Game.BraneUpgrade.GAIN).isDisable(), "sans onde, rien ne s'achète");
            game.state().setBraneShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.braneButton(Game.BraneUpgrade.GAIN).getText().startsWith("Gain : chaque passage vaut 1 → 2 ondes"),
                    window.braneButton(Game.BraneUpgrade.GAIN).getText());
            assertTrue(window.braneButton(Game.BraneUpgrade.ELASTICITY).getText()
                    .startsWith("Élasticité : l'onde porte à fond dès 100 % de tirée → 83 %"), window.braneButton(Game.BraneUpgrade.ELASTICITY).getText());
            assertTrue(window.braneButton(Game.BraneUpgrade.SIZE).getText().startsWith("Grandeur des ondes : portée à fond 0.35 → 0.45"),
                    window.braneButton(Game.BraneUpgrade.SIZE).getText());
            assertTrue(window.braneButton(Game.BraneUpgrade.BOUNCE).getText().startsWith("Rebond : un passage après un bord vaut ×1.5 → ×1.75"),
                    window.braneButton(Game.BraneUpgrade.BOUNCE).getText());
            assertTrue(window.braneButton(Game.BraneUpgrade.COMBO).getText().startsWith("Points en une onde : chaque passage de plus de la même onde +10 % → +15 %"),
                    window.braneButton(Game.BraneUpgrade.COMBO).getText());
            for (Game.BraneUpgrade upgrade : Game.BraneUpgrade.values()) window.braneButton(upgrade).fire();
            assertEquals(2, game.braneValue(), 1e-9);
            assertEquals(1.75, game.braneBounceFactor(), 1e-9);
            assertEquals(0.35 * 1.3, game.braneMaxReach(), 1e-9);
            assertTrue(window.braneButton(Game.BraneUpgrade.GAIN).getText().startsWith("Gain : chaque passage vaut 2 → 3 ondes"));
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setLoopShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.BRANE_PULL));
            window.frame(0.016, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.braneHintText().startsWith("Tirée automatique"), window.braneHintText());
            long sent = game.braneWavesSent();
            run(game, window, Game.BRANE_AUTO_SECONDS + 0.5);
            assertTrue(game.braneWavesSent() > sent, "elle tire seule");
            game.setDimensionAutomationEnabled(DimensionAutomation.BRANE_PULL, false);
            window.frame(0.016, true);
            assertTrue(window.autoBadgeText().isEmpty(), "en pause : plus de mention");
            return null;
        });
    }

    @Test
    void laLumiereAstraleEstDansLaBrancheBraneDeLaGraine() {
        FxTestSupport.fx(() -> {
            Game game = sixth();
            game.state().setWateringCan(true);
            UniversePage page = new UniversePage(game);
            new Scene(page, 1000, 700);
            page.select(UniversePage.SEED);
            page.refresh();
            SeedUpgradesPane upgrades = page.seedUpgrades();
            assertEquals("Brane", SeedUpgradesPane.branchName(6));
            assertTrue(upgrades.isBranchOpen(6));
            upgrades.select(6);
            assertEquals("Ondes de la brane : 0", upgrades.resourceText().getText());
            assertTrue(upgrades.button(SeedUpgrade.ASTRAL_LIGHT).getText().startsWith(
                    "Lumière astrale (niveau 0) : arrosage par seconde de l'arrosoir et du tuyau ×1 → ×1.2"),
                    upgrades.button(SeedUpgrade.ASTRAL_LIGHT).getText());
            assertTrue(upgrades.button(SeedUpgrade.ASTRAL_LIGHT).isDisabled());
            double rate = game.wateringCanRate();
            game.state().setBraneShards(1e6);
            upgrades.refresh();
            upgrades.button(SeedUpgrade.ASTRAL_LIGHT).fire();
            assertEquals(1.2, game.astralLight(), 1e-9);
            assertEquals(rate * 1.2, game.wateringCanRate(), 1e-9);
            assertTrue(upgrades.button(SeedUpgrade.ASTRAL_LIGHT).getText().startsWith("Lumière astrale (niveau 1)"));
            assertFalse(page.dimensionsView().isMarked(6));
            game.state().setBraneShardsEarned(Game.BRANE_THRESHOLD);
            page.refresh();
            assertTrue(page.dimensionsView().isMarked(6), "le seuil atteint : le rond vert");
            return null;
        });
    }

    @Test
    void laBraneALaSectionDansLAutomatisationSesStatistiquesEtSaVersion() {
        FxTestSupport.fx(() -> {
            Game game = sixth();
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setLoopShards(1e9);
            DimensionAutomationPane pane = new DimensionAutomationPane(game);
            new Scene(pane, 1200, 760);
            pane.refresh();
            assertTrue(pane.sectionShown(6));
            assertEquals("Dimension 6 : La brane", pane.headingText(6));
            assertTrue(pane.balanceText(6).startsWith("Payés en nœuds"), pane.balanceText(6));
            Card play = pane.card(DimensionAutomation.BRANE_PULL);
            assertEquals(Card.State.READY, play.state());
            assertTrue(play.text().contains("Tire la membrane à fond au milieu des points"), play.text());
            assertEquals(Card.State.LOCKED, pane.card(DimensionAutomation.BRANE_COMBO).state());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.BRANE_PULL));
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Brane : ondes : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Brane : seuil : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Ondes tirées sur la brane : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Points touchés de la brane : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.equals("Automatismes des dimensions : " + game.dimensionAutomationsOwned()
                    + " / 44 achetés")), rows.toString());
            assertEquals("0.155", Version.CURRENT);
            return null;
        });
    }
}
