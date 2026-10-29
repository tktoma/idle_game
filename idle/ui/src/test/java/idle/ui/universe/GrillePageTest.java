package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.156, refaite en 0.157 : la grille, huitième dimension, et la graine profonde, sous Xvfb. La grande grille
 * (mille cases par côté) se vise d'un clic sur la carte ou par les champs x, y, z, et « Essayer » essaie la case ; un seul
 * essai par point ; le zoom resserre la carte ; réduite, la grille se montre case par case et un clic essaie. L'attente
 * suit chaque essai ; la case du point le trouve (×5). Depuis la version 0.158, les cases essayées contre un point parti
 * s'effacent. Ce que la grille montre suit strictement l'affichage du point : le gain seul, puis la distance (et les
 * couleurs du froid au chaud), la couche, la colonne. Les améliorations s'achètent dans la fenêtre ; les essais automatiques jouent seuls. La graine profonde est dans la branche « Grille » ; la grille a
 * sa section dans l'automatisation, ses lignes dans les statistiques.
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

    /** Un clic à la souris sur la case (i, j) de la couche montrée : il la vise, et l'essaie sur une petite grille. */
    private static void probe(DimensionView view, int i, int j) {
        double[] at = view.gridCellScreen(i, j);
        view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, at[0], at[1]));
        view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, at[0], at[1]));
    }

    /** Un seul point caché, là. */
    private static void hide(Game game, double x, double y, double z) {
        game.testSetGridPoints(List.of(new Game.GridPoint(x, y, z)));
    }

    /** Vise la case (x, y, z) par les champs de la fenêtre, puis « Essayer ». */
    private static void aim(DimensionWindow window, int x, int y, int z) {
        TextField[] fields = window.gridFields();
        fields[0].setText(String.valueOf(x));
        fields[1].setText(String.valueOf(y));
        fields[2].setText(String.valueOf(z));
        window.gridTryButton().fire();
    }

    @Test
    void laGrandeGrilleSeViseParSesCoordonneesEtUnEssaiParPoint() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals("Dimension 8 : La grille", window.titleText());
            assertEquals("Repères de la grille : 0", window.gridShardsText());
            assertTrue(window.gridThresholdText().startsWith("Seuil : 0 / "), window.gridThresholdText());
            assertEquals(Game.GRID_CELLS, game.gridDivisions());
            assertTrue(window.gridSizeText().startsWith("Grille de 1 000 × 1 000 × 1 000 cases"), window.gridSizeText());
            assertTrue(window.gridSizeText().endsWith("1 essai par point"), window.gridSizeText());
            assertEquals(DimensionView.layerText(0, 1000), window.gridLayerText());
            assertFalse(view.gridClickable(), "mille cases par côté : un clic vise, il n'essaie pas");
            hide(game, 500.5, 500.5, 500.5);
            // Un clic sur la carte vise la case sous la souris, sans l'essayer.
            probe(view, 10, 20);
            assertArrayEquals(new int[] {10, 20, 0}, view.gridSelected());
            assertEquals(0, game.gridProbes());
            window.frame(0.016, true);
            assertEquals("10", window.gridFields()[0].getText(), "les champs suivent la case visée");
            assertEquals("20", window.gridFields()[1].getText());
            // Les champs visent la case du point, et « Essayer » l'essaie.
            aim(window, 500, 500, 500);
            Game.GridProbe last = game.gridLastProbe();
            assertNotNull(last);
            assertTrue(last.found(), "en plein sur le point");
            assertEquals(500, view.gridLayer(), "la couche suit le champ z");
            assertTrue(view.lastGridMark().startsWith("Trouvé ! ×5"), view.lastGridMark());
            assertNull(view.gridProbedAt(500, 500, 500), "le point trouvé s'est caché ailleurs : sa case ne se colore plus");
            window.frame(0.016, true);
            assertTrue(window.gridTryButton().isDisable(), "pendant l'attente, on n'essaie pas");
            assertTrue(window.gridHintText().contains("Dernier essai : trouvé ! +"), window.gridHintText());
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            assertFalse(window.gridTryButton().isDisable());
            // Un essai à côté : le point n'en avait qu'un, il se cache ailleurs.
            hide(game, 100.5, 900.5, 600.5);
            aim(window, 101, 900, 600);
            assertFalse(game.gridLastProbe().found());
            assertEquals(0, game.gridLastProbe().triesLeft());
            window.frame(0.016, true);
            assertTrue(window.gridHintText().contains("plus d'essai : le point s'est caché ailleurs"), window.gridHintText());
            // Le zoom resserre la carte autour de la case visée ; les couches changent aux flèches.
            window.gridZoomInButton().fire();
            assertEquals(250, view.gridSpan());
            int[] origin = view.gridOrigin();
            assertTrue(origin[0] <= 101 && 101 < origin[0] + 250, "la case visée reste dans la carte");
            window.gridZoomOutButton().fire();
            assertEquals(1000, view.gridSpan());
            assertTrue(window.key(KeyCode.DOWN, true));
            assertEquals(599, view.gridLayer());
            assertTrue(window.key(KeyCode.UP, true));
            assertEquals(600, view.gridLayer());
            assertFalse(view.press(2, 2, false), "hors de la carte : rien");
            return null;
        });
    }

    @Test
    void uneGrilleReduiteSeMontreCaseParCaseEtUnClicEssaie() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            game.state().setGridLevel(Game.GridUpgrade.DIVISION.ordinal(), Game.GridUpgrade.DIVISION.max());
            assertEquals(Game.GRID_MIN_CELLS, game.gridDivisions());
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertTrue(view.gridClickable());
            assertTrue(window.gridSizeText().startsWith("Grille de 2 × 2 × 2 cases ·"), window.gridSizeText());
            hide(game, 750, 750, 750);
            probe(view, 0, 0);
            assertEquals(1, game.gridProbes(), "petite, un clic essaie la case");
            assertFalse(game.gridLastProbe().found());
            probe(view, 1, 1);
            assertEquals(1, game.gridProbes(), "pendant l'attente, le clic n'essaie pas");
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            window.gridUpButton().fire();
            assertEquals(1, view.gridLayer());
            assertTrue(window.gridUpButton().isDisable());
            hide(game, 750, 750, 750);
            probe(view, 1, 1);
            assertTrue(game.gridLastProbe().found());
            assertEquals(1, game.gridFound());
            return null;
        });
    }

    @Test
    void laGrilleNeMontreQueCeQueLAffichagePermet() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            // Le point : colonne x = 100, ligne y = 900, couche z = 600.
            hide(game, 100.5, 900.5, 600.5);
            aim(window, 101, 900, 600);
            window.frame(0.016, true);
            String hint = window.gridHintText();
            assertTrue(hint.contains("Dernier essai : +"), hint);
            assertFalse(hint.contains("cases du point visé") || hint.contains("couche z =") || hint.contains("colonne x ="),
                    "niveau 0 : rien que le gain : " + hint);
            assertArrayEquals(new int[] {-1, -1}, view.gridRevealed());
            Game.GridProbe near = new Game.GridProbe(0, 0, 0, 1, false, 1, 0, 0), far = new Game.GridProbe(0, 0, 0, 1500, false, 1, 0, 0);
            assertEquals(view.gridProbeColor(near), view.gridProbeColor(far), "niveau 0 : la couleur ne dit pas la distance");
            game.state().setGridShards(1e12);
            window.frame(0.016, true);
            assertTrue(window.gridButton(Game.GridUpgrade.ATTEMPTS).getText().startsWith("Essais : 1 par point → 2"),
                    window.gridButton(Game.GridUpgrade.ATTEMPTS).getText());
            window.gridButton(Game.GridUpgrade.ATTEMPTS).fire();
            assertTrue(window.gridButton(Game.GridUpgrade.REVEAL).getText().startsWith("Affichage du point : rien que le gain → la distance"),
                    window.gridButton(Game.GridUpgrade.REVEAL).getText());
            for (int level = 1; level <= 3; level++) {
                window.gridButton(Game.GridUpgrade.REVEAL).fire();
                run(game, window, Game.GRID_COOLDOWN + 0.1);
                hide(game, 100.5, 900.5, 600.5);
                aim(window, 101, 900, 600);
                window.frame(0.016, true);
                hint = window.gridHintText();
                assertTrue(hint.contains("cases du point visé") && hint.contains("encore 1 essai sur ce point"), hint);
                assertEquals(level >= 2, hint.contains("dans la couche z = 600"), "niveau " + level + " : " + hint);
                assertEquals(level >= 3, hint.contains("la colonne x = 100"), "niveau " + level + " : " + hint);
                assertArrayEquals(new int[] {level >= 2 ? 600 : -1, level >= 3 ? 100 : -1}, view.gridRevealed());
            }
            assertNotEquals(view.gridProbeColor(near), view.gridProbeColor(far), "du froid au chaud");
            assertTrue(window.gridButton(Game.GridUpgrade.REVEAL).getText().endsWith("Au maximum"));
            // Version 0.158 : tant que le point est là, sa case essayée garde sa couleur ; parti, elle s'efface.
            assertNotNull(view.gridProbedAt(101, 900, 600));
            run(game, window, Game.GRID_COOLDOWN + 0.1);
            aim(window, 102, 900, 600);
            assertEquals(0, game.gridLastProbe().triesLeft());
            assertNull(view.gridProbedAt(101, 900, 600), "le point parti, les couleurs des essais contre lui s'effacent");
            assertNull(view.gridProbedAt(102, 900, 600));
            assertArrayEquals(new int[] {-1, -1}, view.gridRevealed());
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentEtLesEssaisAutomatiquesJouentSeuls() {
        FxTestSupport.fx(() -> {
            Game game = eighth();
            DimensionWindow window = window(game);
            assertTrue(window.gridButton(Game.GridUpgrade.DIVISION).isDisable(), "sans repère, rien ne s'achète");
            game.state().setGridShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.gridButton(Game.GridUpgrade.DIVISION).getText().startsWith("Réduction de la grille : 1 000 → 700 cases par côté"),
                    window.gridButton(Game.GridUpgrade.DIVISION).getText());
            assertTrue(window.gridButton(Game.GridUpgrade.GAIN).getText().startsWith("Gain : chaque essai vaut 1 → 2 repères"),
                    window.gridButton(Game.GridUpgrade.GAIN).getText());
            assertTrue(window.gridButton(Game.GridUpgrade.POINTS).getText().startsWith("Nombre de points : 1 cachés → 2"),
                    window.gridButton(Game.GridUpgrade.POINTS).getText());
            assertTrue(window.gridButton(Game.GridUpgrade.PROXIMITY).getText().startsWith("Multiplicateur de proximité : sur le point ×5 → ×6"),
                    window.gridButton(Game.GridUpgrade.PROXIMITY).getText());
            for (Game.GridUpgrade upgrade : Game.GridUpgrade.values()) window.gridButton(upgrade).fire();
            assertEquals(700, game.gridDivisions());
            assertEquals(2, game.gridPointCount());
            assertEquals(2, game.gridAttempts());
            assertEquals(2, game.gridValue(), 1e-9);
            window.frame(0.016, true);
            assertTrue(window.gridSizeText().startsWith("Grille de 700 × 700 × 700 cases"), window.gridSizeText());
            assertTrue(window.gridSizeText().endsWith("2 essais par point"), window.gridSizeText());
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setClickerShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.GRID_PROBE));
            window.frame(0.016, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.gridHintText().startsWith("Essais automatiques"), window.gridHintText());
            run(game, window, Game.GRID_AUTO_SECONDS + 0.2);
            assertTrue(game.gridAutoProbes() >= 1, "ils essaient seuls");
            assertTrue(window.view().gridAutoFlashes() >= 1, "et leurs essais se voient");
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
            assertTrue(play.text().contains("Essaie une case près d'un point toutes les 2 s"), play.text());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.GRID_PROBE));
            assertEquals(7, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 8).count(),
                    "sept automatismes pour la grille");
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Grille : repères : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Grille : seuil : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Essais de la grille : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Points trouvés : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.equals("Automatismes des dimensions : 1 / " + DimensionAutomation.values().length
                    + " achetés")), rows.toString());
            assertTrue(Version.CURRENT.compareTo("0.156") >= 0, Version.CURRENT);
            return null;
        });
    }
}
