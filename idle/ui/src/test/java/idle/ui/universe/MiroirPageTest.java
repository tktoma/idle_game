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
 * Version 0.157 : le miroir, neuvième dimension, et les saisons de la graine, sous Xvfb. Le motif ne se montre que
 * pendant qu'il s'allume, une case à la fois ; on le refait à la souris, en miroir, et seules les cases reproduites se
 * voient ; un motif réussi donne des reflets et s'allonge, un motif manqué raccourcit ; la seconde chance permet une
 * erreur ; les améliorations s'achètent dans la fenêtre ; le miroir automatique joue seul. Les saisons se lisent dans
 * la vue de la graine et remplissent le chrono ; le miroir a sa section dans l'automatisation et ses lignes dans les
 * statistiques.
 */
class MiroirPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au neuvième univers : le miroir est ouvert. */
    private static Game ninth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 8; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isMirrorOpen());
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.MIRROR);
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

    /** Un clic à la souris sur la case ({@code column}, {@code row}). */
    private static void click(DimensionView view, int column, int row) {
        double[] at = view.mirrorCellScreen(column, row);
        view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, at[0], at[1]));
        view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, at[0], at[1]));
    }

    /** Un clic sur le reflet de cette case de la moitié gauche. */
    private static void reflect(Game game, DimensionView view, Game.MirrorCell cell) {
        Game.MirrorCell mirrored = game.mirrored(cell);
        click(view, mirrored.column(), mirrored.row());
    }

    private static final List<Game.MirrorCell> PATTERN = List.of(new Game.MirrorCell(0, 0), new Game.MirrorCell(1, 2), new Game.MirrorCell(0, 3));

    @Test
    void leMotifNeSeMontreQuePendantQuIlSAllume() {
        FxTestSupport.fx(() -> {
            Game game = ninth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals("Dimension 9 : Le miroir", window.titleText());
            assertEquals("Reflets du miroir : 0", window.mirrorShardsText());
            assertTrue(window.mirrorThresholdText().startsWith("Seuil : 0 / "), window.mirrorThresholdText());
            for (int i = 0; i < 100 && game.mirrorPhase() != Game.MirrorPhase.SHOW; i++) run(game, window, 0.05);
            assertEquals(Game.MirrorPhase.SHOW, game.mirrorPhase());
            window.frame(0.016, true);
            assertEquals(1, view.mirrorPatternDrawn(), "une case du motif à la fois, celle qui s'allume");
            assertTrue(window.mirrorHintText().startsWith("Regardez le motif"), window.mirrorHintText());
            for (int i = 0; i < 200 && game.mirrorPhase() != Game.MirrorPhase.INPUT; i++) run(game, window, 0.05);
            assertEquals(Game.MirrorPhase.INPUT, game.mirrorPhase());
            window.frame(0.016, true);
            assertEquals(0, view.mirrorPatternDrawn(), "éteint pendant la reproduction : rien du motif ne se voit");
            assertEquals(0, view.mirrorReproducedDrawn());
            assertTrue(window.mirrorHintText().startsWith("De mémoire, cliquez à droite"), window.mirrorHintText());
            return null;
        });
    }

    @Test
    void unMotifRefaitALaSourisDonneDesRefletsEtUnMotifManqueRaccourcit() {
        FxTestSupport.fx(() -> {
            Game game = ninth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            game.testSetMirrorPattern(PATTERN);
            click(view, 0, 0);
            assertEquals(0, game.mirrorPosition(), "la moitié gauche ne se joue pas");
            reflect(game, view, PATTERN.get(0));
            assertEquals(1, game.mirrorPosition());
            window.frame(0.016, true);
            assertEquals(1, view.mirrorReproducedDrawn(), "la case reproduite se voit à droite");
            assertEquals(0, view.mirrorPatternDrawn(), "le reste du motif, non");
            assertTrue(window.mirrorHintText().contains("1 / 3"), window.mirrorHintText());
            reflect(game, view, PATTERN.get(1));
            reflect(game, view, PATTERN.get(2));
            assertEquals(Game.MirrorAnswer.SUCCESS, game.mirrorLastResult());
            assertEquals(game.mirrorReward(3), game.state().mirrorShards(), 1e-9);
            assertEquals(3, game.mirrorBest());
            assertEquals(4, game.mirrorLength(), "le suivant a une case de plus");
            window.frame(0.016, true);
            assertTrue(window.mirrorCountsText().startsWith("Motif de 4 cases · record 3 · réussis 1 · manqués 0"), window.mirrorCountsText());
            assertTrue(window.mirrorHintText().endsWith("Dernier motif réussi : +" + idle.ui.common.Format.darkMatter(game.mirrorLastGain()) + "."),
                    window.mirrorHintText());
            // Manqué : une case qui n'est pas le reflet attendu.
            game.testSetMirrorPattern(List.of(new Game.MirrorCell(0, 0), new Game.MirrorCell(1, 1), new Game.MirrorCell(0, 2), new Game.MirrorCell(1, 3)));
            Game.MirrorCell wrong = game.mirrored(new Game.MirrorCell(1, 0));
            click(view, wrong.column(), wrong.row());
            assertEquals(Game.MirrorAnswer.FAIL, game.mirrorLastResult());
            assertEquals(1, game.mirrorFails());
            assertEquals(3, game.mirrorLength(), "le suivant a une case de moins");
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentEtLeMiroirAutomatiqueJoueSeul() {
        FxTestSupport.fx(() -> {
            Game game = ninth();
            DimensionWindow window = window(game);
            assertTrue(window.mirrorButton(Game.MirrorUpgrade.GAIN).isDisable(), "sans reflet, rien ne s'achète");
            game.state().setMirrorShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.mirrorButton(Game.MirrorUpgrade.GAIN).getText().startsWith("Gain : chaque case d'un motif réussi vaut 1 → 2 reflets"),
                    window.mirrorButton(Game.MirrorUpgrade.GAIN).getText());
            assertTrue(window.mirrorButton(Game.MirrorUpgrade.LENGTH).getText().startsWith("Multiplicateur de longueur : ×1.1 → ×1.15"),
                    window.mirrorButton(Game.MirrorUpgrade.LENGTH).getText());
            assertTrue(window.mirrorButton(Game.MirrorUpgrade.MEMORY).getText().startsWith("Temps de mémorisation : 0.8 s → 0.95 s"),
                    window.mirrorButton(Game.MirrorUpgrade.MEMORY).getText());
            assertTrue(window.mirrorButton(Game.MirrorUpgrade.MISTAKES).getText().startsWith("Seconde chance : 0 erreur permise → 1"),
                    window.mirrorButton(Game.MirrorUpgrade.MISTAKES).getText());
            assertTrue(window.mirrorButton(Game.MirrorUpgrade.BOARD).getText().startsWith("Plateau plus grand : 4 × 4 → 6 × 5 cases"),
                    window.mirrorButton(Game.MirrorUpgrade.BOARD).getText());
            for (Game.MirrorUpgrade upgrade : Game.MirrorUpgrade.values()) window.mirrorButton(upgrade).fire();
            assertEquals(6, game.mirrorColumns());
            assertEquals(1, game.mirrorAllowedMistakes());
            // La seconde chance : une erreur est permise.
            game.testSetMirrorPattern(PATTERN);
            Game.MirrorCell wrong = game.mirrored(new Game.MirrorCell(1, 0));
            click(window.view(), wrong.column(), wrong.row());
            assertEquals(1, game.mirrorMistakes());
            assertEquals(Game.MirrorPhase.INPUT, game.mirrorPhase(), "une erreur permise : le motif continue");
            window.frame(0.016, true);
            assertTrue(window.mirrorCountsText().endsWith("erreurs 1 / 1"), window.mirrorCountsText());
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setGridShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.MIRROR_PLAY));
            window.frame(0.016, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.mirrorHintText().startsWith("Miroir automatique"), window.mirrorHintText());
            run(game, window, 12);
            assertTrue(game.mirrorSuccesses() >= 1, "il refait des motifs");
            assertTrue(window.view().mirrorAutoFlashes() > 0, "et ses clics se voient");
            game.setDimensionAutomationEnabled(DimensionAutomation.MIRROR_PLAY, false);
            window.frame(0.016, true);
            assertTrue(window.autoBadgeText().isEmpty());
            return null;
        });
    }

    @Test
    void lesSaisonsSeLisentDansLaGraineEtRemplissentLeChrono() {
        FxTestSupport.fx(() -> {
            Game game = ninth();
            game.state().setWateringCan(true);
            UniversePage page = new UniversePage(game);
            new Scene(page, 1000, 700);
            page.select(UniversePage.SEED);
            page.refresh();
            page.frame(0.016, true);
            SeedView view = page.seedView();
            assertTrue(game.areSeasonsUnlocked());
            assertEquals("Printemps ×1.5 : 120 s", view.seasonText());
            game.testSetSeason(Game.Season.WINTER);
            page.frame(0.016, true);
            assertEquals("Hiver ×0.5 : 120 s", view.seasonText());
            SeedUpgradesPane upgrades = page.seedUpgrades();
            assertEquals("Miroir", SeedUpgradesPane.branchName(9));
            assertTrue(upgrades.isBranchOpen(9));
            upgrades.select(9);
            assertEquals("Reflets du miroir : 0", upgrades.resourceText().getText());
            game.state().setMirrorShards(1e6);
            upgrades.refresh();
            assertTrue(upgrades.button(SeedUpgrade.SEASONS).isDisabled(), "les saisons attendent l'étage du chrono");
            assertTrue(upgrades.button(SeedUpgrade.SEASONS).getText().endsWith("Il faut d'abord l'étage du chrono (branche Durée)"),
                    upgrades.button(SeedUpgrade.SEASONS).getText());
            game.state().setSeedUpgradeLevel(SeedUpgrade.CHRONO.ordinal(), 1);
            upgrades.refresh();
            assertTrue(upgrades.button(SeedUpgrade.SEASONS).getText().startsWith("Saisons (niveau 0) : chaque arrosage remplit le chrono de 0 → 0.2 s"),
                    upgrades.button(SeedUpgrade.SEASONS).getText());
            upgrades.button(SeedUpgrade.SEASONS).fire();
            assertEquals(Game.SEASON_FILL * Game.Season.WINTER.fill(), game.seasonFillPerWatering(), 1e-9);
            page.frame(0.016, true);
            assertEquals("Hiver ×0.5 : 120 s · +0.1 s au chrono par arrosage", view.seasonText());
            assertEquals(1, java.util.Arrays.stream(SeedUpgrade.values()).filter(u -> u.dimension() == 9).count());
            assertFalse(page.dimensionsView().isMarked(9));
            game.state().setMirrorShardsEarned(Game.MIRROR_THRESHOLD);
            page.refresh();
            assertTrue(page.dimensionsView().isMarked(9), "le seuil atteint : le rond vert");
            return null;
        });
    }

    @Test
    void leMiroirALaSectionDansLAutomatisationSesStatistiquesEtSaVersion() {
        FxTestSupport.fx(() -> {
            Game game = ninth();
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setGridShards(1e9);
            DimensionAutomationPane pane = new DimensionAutomationPane(game);
            new Scene(pane, 1200, 760);
            pane.refresh();
            assertTrue(pane.sectionShown(9));
            assertEquals("Dimension 9 : Le miroir", pane.headingText(9));
            assertTrue(pane.balanceText(9).startsWith("Payés en repères"), pane.balanceText(9));
            Card play = pane.card(DimensionAutomation.MIRROR_PLAY);
            assertEquals(Card.State.READY, play.state());
            assertTrue(play.text().contains("Reproduit le motif, une case toutes les 0,4 s"), play.text());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.MIRROR_PLAY));
            assertEquals(6, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 9).count(),
                    "six automatismes pour le miroir");
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Miroir : reflets : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Miroir : seuil : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Motifs du miroir : 0 réussis, 0 manqués")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Plus long motif réussi : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Saison de la graine : Printemps")), rows.toString());
            assertTrue(Version.CURRENT.compareTo("0.157") >= 0, Version.CURRENT);
            return null;
        });
    }
}
