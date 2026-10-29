package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.156 : la grille, huitième dimension, et la graine profonde, sous Xvfb. On choisit une couche et l'on sonde
 * une case à la souris ; l'attente suit chaque sondage ; la case qui contient le point le trouve (×5). Ce que la grille
 * montre suit strictement l'affichage du point : le gain seul, puis la distance (et les couleurs du froid au chaud), la
 * couche, la colonne. Les améliorations s'achètent dans la fenêtre ; le sondage automatique joue seul. La graine profonde
 * est dans la branche « Grille » ; la grille a sa section dans l'automatisation, ses lignes dans les statistiques.
 */
class GrillePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au huitième univers : la grille est ouverte. */
    private static Game eighth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 7; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isGridOpen());
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.GRID);
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
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false,
                type == MouseEvent.MOUSE_PRESSED, false, false, false, false, false, null);
    }

    /** Un clic à la souris sur la case (i, j) de la couche montrée. */
    private static void probe(DimensionView view, int i, int j) {
        double[] at = view.gridCellScreen(i, j);
        view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, at[0], at[1]));
        view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, at[0], at[1]));
    }

    /** Un seul point caché, là. */
    private static void hide(Game game, double x, double y, double z) {
        game.testSetGridPoints(List.of(new Game.GridPoint(x, y, z)));
    }

    @Test
    void unSondageALaSourisDonneDesReperesEtLaGrilleAttend() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals("Dimension 8 : La grille", window.titleText());
            assertEquals("Repères de la grille : 0", window.gridShardsText());
            assertTrue(window.gridThresholdText().startsWith("Seuil : 0 / "), window.gridThresholdText());
            assertEquals("Couche 1 / 2", window.gridLayerText());
            assertTrue(window.gridHintText().startsWith("Cliquez une case"), window.gridHintText());
            hide(game, 750, 750, 750);
            probe(view, 0, 0);
            Game.GridProbe last = game.gridLastProbe();
            assertNotNull(last);
            assertEquals(0, last.k(), "dans la couche montrée");
            assertFalse(last.found());
            assertEquals(game.gridValue() * game.gridProximityFactor(500 * Math.sqrt(3)), game.state().gridShards(), 1e-9);
            assertEquals(last, view.gridProbedAt(0, 0, 0), "la case garde ce qu'elle a donné");
            assertEquals("+" + idle.ui.common.Format.darkMatter(last.gain()), view.lastGridMark());
            probe(view, 1, 0);
            assertEquals(1, game.gridProbes(), "pendant l'attente, le clic ne sonde pas");
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            // Le point est dans la case (1, 1) de la couche 2 : on y monte et on le trouve.
            window.gridUpButton().fire();
            assertEquals(1, view.gridLayer());
            assertEquals("Couche 2 / 2", window.gridLayerText());
            assertTrue(window.gridUpButton().isDisable(), "plus haut, rien");
            probe(view, 1, 1);
            assertTrue(game.gridLastProbe().found());
            assertEquals(1, game.gridFound());
            assertTrue(view.lastGridMark().startsWith("Trouvé ! ×5"), view.lastGridMark());
            window.frame(0.016, true);
            assertTrue(window.gridHintText().contains("Dernier sondage : trouvé ! +"), window.gridHintText());
            assertTrue(window.key(KeyCode.DOWN, true), "la flèche du bas descend d'une couche");
            assertEquals(0, view.gridLayer());
            assertTrue(window.key(KeyCode.UP, true));
            assertEquals(1, view.gridLayer());
            assertFalse(view.press(2, 2, false), "hors de la grille : rien");
            return null;
        });
    }

    @Test
    void laGrilleNeMontreQueCeQueLAffichagePermet() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            // Le point : colonne 1 (i = 0), ligne 2 (j = 1), couche 2 (k = 1).
            hide(game, 100, 900, 600);
            probe(view, 1, 0);
            window.frame(0.016, true);
            String hint = window.gridHintText();
            assertTrue(hint.contains("Dernier sondage : +"), hint);
            assertFalse(hint.contains("du point le plus proche") || hint.contains("couche 2") || hint.contains("colonne"),
                    "niveau 0 : rien que le gain : " + hint);
            assertArrayEquals(new int[] {-1, -1}, view.gridRevealed());
            Game.GridProbe near = new Game.GridProbe(0, 0, 0, 10, false, 1), far = new Game.GridProbe(0, 0, 0, 1500, false, 1);
            assertEquals(view.gridProbeColor(near), view.gridProbeColor(far), "niveau 0 : la couleur ne dit pas la distance");
            game.state().setGridShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.gridButton(Game.GridUpgrade.REVEAL).getText().startsWith("Affichage du point : rien que le gain → la distance"),
                    window.gridButton(Game.GridUpgrade.REVEAL).getText());
            window.gridButton(Game.GridUpgrade.REVEAL).fire();
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            probe(view, 1, 0);
            window.frame(0.016, true);
            hint = window.gridHintText();
            assertTrue(hint.contains("du point le plus proche") && !hint.contains("dans la couche") && !hint.contains("colonne"),
                    "niveau 1 : la distance seule : " + hint);
            assertNotEquals(view.gridProbeColor(near), view.gridProbeColor(far), "du froid au chaud");
            assertArrayEquals(new int[] {-1, -1}, view.gridRevealed());
            window.gridButton(Game.GridUpgrade.REVEAL).fire();
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            probe(view, 1, 0);
            window.frame(0.016, true);
            hint = window.gridHintText();
            assertTrue(hint.contains("dans la couche 2") && !hint.contains("colonne"), "niveau 2 : la couche : " + hint);
            assertArrayEquals(new int[] {1, -1}, view.gridRevealed());
            window.gridButton(Game.GridUpgrade.REVEAL).fire();
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            probe(view, 1, 0);
            window.frame(0.016, true);
            assertTrue(window.gridHintText().contains("dans la couche 2, la colonne 1"), window.gridHintText());
            assertArrayEquals(new int[] {1, 0}, view.gridRevealed());
            assertTrue(window.gridButton(Game.GridUpgrade.REVEAL).getText().endsWith("Au maximum"));
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentEtLeSondageAutomatiqueJoueSeul() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            DimensionWindow window = window(game);
            assertTrue(window.gridButton(Game.GridUpgrade.DIVISION).isDisable(), "sans repère, rien ne s'achète");
            game.state().setGridShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.gridButton(Game.GridUpgrade.DIVISION).getText().startsWith("Division de la grille : 2 → 3 cases par côté"),
                    window.gridButton(Game.GridUpgrade.DIVISION).getText());
            assertTrue(window.gridButton(Game.GridUpgrade.GAIN).getText().startsWith("Gain : chaque sondage vaut 1 → 2 repères"),
                    window.gridButton(Game.GridUpgrade.GAIN).getText());
            assertTrue(window.gridButton(Game.GridUpgrade.POINTS).getText().startsWith("Nombre de points : 1 cachés → 2"),
                    window.gridButton(Game.GridUpgrade.POINTS).getText());
            assertTrue(window.gridButton(Game.GridUpgrade.PROXIMITY).getText().startsWith("Multiplicateur de proximité : sur le point ×5 → ×6"),
                    window.gridButton(Game.GridUpgrade.PROXIMITY).getText());
            for (Game.GridUpgrade upgrade : Game.GridUpgrade.values()) window.gridButton(upgrade).fire();
            assertEquals(3, game.gridDivisions());
            assertEquals(2, game.gridPointCount());
            assertEquals(2, game.gridValue(), 1e-9);
            window.frame(0.016, true);
            assertEquals("Couche 1 / 3", window.gridLayerText());
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setClickerShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.GRID_PROBE));
            window.frame(0.016, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.gridHintText().startsWith("Sondage automatique"), window.gridHintText());
            run(game, window, Game.GRID_AUTO_SECONDS + 0.2);
            assertTrue(game.gridAutoProbes() >= 1, "il sonde seul");
            assertTrue(window.view().gridAutoFlashes() >= 1, "et ses sondages se voient");
            game.setDimensionAutomationEnabled(DimensionAutomation.GRID_PROBE, false);
            window.frame(0.016, true);
            assertTrue(window.autoBadgeText().isEmpty());
            return null;
        });
    }

    @Test
    void laGraineProfondeEstDansLaBrancheGrille() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            game.state().setWateringCan(true);
            game.state().noteVoidHistory("1", 1e16);
            UniversePage page = new UniversePage(game);
            new Scene(page, 1000, 700);
            page.select(UniversePage.SEED);
            page.refresh();
            SeedUpgradesPane upgrades = page.seedUpgrades();
            assertEquals("Grille", SeedUpgradesPane.branchName(8));
            assertTrue(upgrades.isBranchOpen(8));
            upgrades.select(8);
            assertEquals("Repères de la grille : 0", upgrades.resourceText().getText());
            double full = game.deepSeedFull();
            assertTrue(full >= 4, "√16 au moins : " + full);
            String next = idle.ui.common.Format.multiplier(1 + (full - 1) / 10);
            assertTrue(upgrades.button(SeedUpgrade.DEEP_SEED).getText().startsWith("Graine profonde (niveau 0) : tout l'arrosage ×1 → " + next),
                    upgrades.button(SeedUpgrade.DEEP_SEED).getText());
            double rate = game.wateringCanRate();
            game.state().setGridShards(1e6);
            upgrades.refresh();
            upgrades.button(SeedUpgrade.DEEP_SEED).fire();
            assertEquals(1 + (full - 1) / 10, game.deepSeed(), 1e-9);
            assertEquals(rate * game.deepSeed(), game.wateringCanRate(), 1e-9);
            assertEquals(1, java.util.Arrays.stream(SeedUpgrade.values()).filter(u -> u.dimension() == 8).count());
            assertFalse(page.dimensionsView().isMarked(8));
            game.state().setGridShardsEarned(Game.GRID_THRESHOLD);
            page.refresh();
            assertTrue(page.dimensionsView().isMarked(8), "le seuil atteint : le rond vert");
            return null;
        });
    }

    @Test
    void laGrilleALaSectionDansLAutomatisationSesStatistiquesEtSaVersion() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setClickerShards(1e9);
            DimensionAutomationPane pane = new DimensionAutomationPane(game);
            new Scene(pane, 1200, 760);
            pane.refresh();
            assertTrue(pane.sectionShown(8));
            assertEquals("Dimension 8 : La grille", pane.headingText(8));
            assertTrue(pane.balanceText(8).startsWith("Payés en étincelles"), pane.balanceText(8));
            Card play = pane.card(DimensionAutomation.GRID_PROBE);
            assertEquals(Card.State.READY, play.state());
            assertTrue(play.text().contains("Sonde une case près d'un point toutes les 2 s"), play.text());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.GRID_PROBE));
            assertEquals(6, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 8).count(),
                    "six automatismes pour la grille");
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Grille : repères : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Grille : seuil : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Sondages de la grille : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Points trouvés : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.equals("Automatismes des dimensions : 1 / " + DimensionAutomation.values().length
                    + " achetés")), rows.toString());
            assertTrue(Version.CURRENT.compareTo("0.156") >= 0, Version.CURRENT);
            return null;
        });
    }
}
