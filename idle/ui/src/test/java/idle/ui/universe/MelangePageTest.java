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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.159 : le mélange refait, sous Xvfb. Un seul mini-jeu, sur une arène, qui mêle étape par étape les
 * mécaniques des autres dimensions : l'étape, sa barre et « Mêler la dimension suivante » ; la ligne à l'étape 1 ; puis,
 * étape par étape (posée par {@code setMixStage}), le point mené à la souris et au clavier, la profondeur (molette, A et
 * E), la charge (bouton droit, barre d'espace), le rythme, l'onde, les clics, le trésor et les paires du miroir. Les
 * améliorations des étapes à venir restent grises ; le mélange automatique joue et passe les étapes ; le mélange a sa
 * section dans l'automatisation (69 automatismes), ses lignes dans les statistiques et sa note de version.
 */
class MelangePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au dixième univers : le mélange est ouvert, à l'étape 1. */
    private static Game tenth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 9; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isMixOpen());
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.MIX);
        window.frame(0.016, true);
        return window;
    }

    /** Le mélange à cette étape, l'arène vide. */
    private static void stage(Game game, DimensionWindow window, int stage) {
        game.state().setMixStage(stage);
        game.testClearMix();
        window.frame(0.016, true);
    }

    private static void run(Game game, DimensionWindow window, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) {
            game.tick(0.05);
            window.frame(0.05, true);
        }
    }

    private static boolean gone(Game game, long id) {
        return game.mixShardList().stream().noneMatch(shard -> shard.id() == id);
    }

    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type, double[] at, MouseButton button) {
        boolean down = type != MouseEvent.MOUSE_RELEASED;
        return new MouseEvent(type, at[0], at[1], at[0], at[1], button, 1, false, false, false, false,
                down && button == MouseButton.PRIMARY, false, down && button == MouseButton.SECONDARY, false, false, false, null);
    }

    private static ScrollEvent wheel(double deltaY) {
        return new ScrollEvent(ScrollEvent.SCROLL, 400, 240, 400, 240, false, false, false, false, false, false, 0, deltaY, 0,
                deltaY, ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null);
    }

    @Test
    void lEtapeUnEstLaLigneEtLeBoutonMeleLaSuivante() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            DimensionWindow window = window(game);
            assertEquals("Dimension 10 : Le mélange", window.titleText());
            assertTrue(window.mixStageShown());
            assertEquals("Étape 1 sur 9 : ligne", window.mixStageText());
            assertEquals("0 / 1000 fragments gagnés à cette étape", window.mixProgressText());
            assertEquals(0, window.mixProgressShare(), 1e-9);
            assertTrue(window.mixAdvanceButton().isDisable(), "l'étape n'est pas gagnée");
            assertEquals("Malus 10 % · étape ×1 · une graduation vaut 0.1", window.mixInfoText());
            assertTrue(window.mixHintText().startsWith("La boule va et vient sur la ligne"), window.mixHintText());
            assertEquals("Vitesse du point\nÀ l'étape 2", window.mixButton(Game.MixUpgrade.SPEED).getText());
            assertTrue(window.mixButton(Game.MixUpgrade.SPEED).isDisable());
            assertEquals("Paires\nÀ l'étape 9", window.mixButton(Game.MixUpgrade.PAIRS).getText());
            assertTrue(window.mixButton(Game.MixUpgrade.GAIN).getText().startsWith("Gain : 1 → 2 par graduation"));
            run(game, window, 4.5);
            assertTrue(game.state().mixShards() > 0, "la boule passe les graduations");
            assertTrue(window.mixProgressShare() > 0);
            game.state().setMixShards(1e6);
            window.frame(0.016, true);
            window.mixButton(Game.MixUpgrade.TICKS).fire();
            assertEquals(4, game.mixTicks());
            assertTrue(window.mixButton(Game.MixUpgrade.SPEED).isDisable(), "même riche, pas avant son étape");
            game.state().setMixStageEarned(game.mixStageGoal());
            window.frame(0.016, true);
            assertFalse(window.mixAdvanceButton().isDisable());
            window.mixAdvanceButton().fire();
            assertEquals(2, game.mixStage());
            assertEquals(0, game.mixLevel(Game.MixUpgrade.TICKS), "les améliorations repartent de zéro");
            assertEquals("Étape 2 sur 9 : ligne + plan", window.mixStageText());
            assertFalse(window.mixButton(Game.MixUpgrade.SPEED).isDisable(), "la vitesse du point s'ouvre à l'étape 2");
            window.open(Dimension.LINE);
            assertFalse(window.mixStageShown(), "l'étape ne se montre que dans le mélange");
            return null;
        });
    }

    @Test
    void lePointSeMeneALaSourisEtAuClavierEtPlongeALaMolette() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            stage(game, window, 2);
            game.testPlaceMixCursor(0.2, 0.2, 0);
            view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, view.mixScreen(0.8, 0.2), MouseButton.PRIMARY));
            run(game, window, 0.5);
            assertTrue(game.mixX() > 0.3, "le point file vers le clic : " + game.mixX());
            view.getOnMouseDragged().handle(mouse(MouseEvent.MOUSE_DRAGGED, view.mixScreen(0.8, 0.8), MouseButton.PRIMARY));
            run(game, window, 0.5);
            assertTrue(game.mixY() > 0.25, "la souris tenue l'emmène");
            view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, view.mixScreen(0.8, 0.8), MouseButton.PRIMARY));
            double x = game.mixX();
            assertTrue(window.key(KeyCode.Q, true));
            run(game, window, 0.5);
            assertTrue(game.mixX() < x - 0.1, "Q le mène à gauche");
            assertTrue(window.key(KeyCode.Q, false));
            double stopped = game.mixX();
            run(game, window, 0.3);
            assertEquals(stopped, game.mixX(), 1e-9, "la touche lâchée l'arrête");
            game.testPlaceMixCursor(0.3, 0.3, 0);
            long shard = game.testAddMixShard(0.32, 0.3, 0, 0);
            window.frame(0.016, true);
            double before = game.state().mixShards();
            run(game, window, 0.05);
            assertTrue(gone(game, shard) && game.state().mixShards() > before, "l'éclat ramassé donne des fragments");
            assertTrue(view.mixFlashesShown() > 0, "et s'éclaire");
            stage(game, window, 3);
            game.testPlaceMixCursor(0.5, 0.3, 0);
            view.getOnScroll().handle(wheel(-40));
            run(game, window, 0.3);
            assertEquals(0.1, game.mixDepth(), 1e-9, "la molette vers soi fait plonger");
            window.key(KeyCode.E, true);
            window.key(KeyCode.E, false);
            run(game, window, 0.3);
            assertEquals(0.2, game.mixDepth(), 1e-9, "E plonge");
            window.key(KeyCode.A, true);
            window.key(KeyCode.A, false);
            view.getOnScroll().handle(wheel(40));
            run(game, window, 0.3);
            assertEquals(0, game.mixDepth(), 1e-9, "A et la molette vers l'avant le remontent");
            long deep = game.testAddMixShard(0.5, 0.3, 0.8, 0);
            run(game, window, 0.05);
            assertFalse(gone(game, deep), "trop profond");
            game.testPlaceMixCursor(0.5, 0.3, 0.8);
            run(game, window, 0.05);
            assertTrue(gone(game, deep), "à sa profondeur, le point le ramasse");
            return null;
        });
    }

    @Test
    void laChargeLeRythmeLOndeLesClicsLeTresorEtLesPaires() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            // La durée : le bouton droit tient la charge, et la verse.
            stage(game, window, 4);
            game.testPlaceMixCursor(0.5, 0.3, 0);
            view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, view.mixScreen(0.5, 0.5), MouseButton.SECONDARY));
            assertTrue(game.isMixHolding() && view.mixHeld());
            game.testAddMixShard(0.5, 0.3, 0, 0);
            run(game, window, 0.05);
            assertTrue(game.mixPot() > 0 && window.mixInfoText().contains("· charge "), window.mixInfoText());
            long releases = game.mixReleases();
            view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, view.mixScreen(0.5, 0.5), MouseButton.SECONDARY));
            run(game, window, 0.05);
            assertFalse(game.isMixHolding());
            assertEquals(releases + 1, game.mixReleases());
            assertTrue(view.mixLastMark().startsWith("+"), view.mixLastMark());
            assertTrue(window.key(KeyCode.SPACE, true) && game.isMixHolding(), "la barre d'espace tient la charge");
            assertTrue(window.key(KeyCode.SPACE, false) && !game.isMixHolding());
            // La boucle : relâcher dans la zone.
            stage(game, window, 5);
            game.testPlaceMixCursor(0.5, 0.3, 0);
            window.key(KeyCode.SPACE, true);
            game.testAddMixShard(0.5, 0.3, 0, 0);
            game.tick(0.05);
            game.testSetMixRing(0.5, 0.5);
            window.key(KeyCode.SPACE, false);
            run(game, window, 0.05);
            assertEquals(Game.MixRhythm.EXCELLENT, game.mixLastRhythm());
            assertTrue(view.mixLastMark().startsWith("Excellent ! ×3 +"), view.mixLastMark());
            // La brane : le relâchement lance une onde, qui ramasse.
            stage(game, window, 6);
            game.testPlaceMixCursor(0.2, 0.2, 0);
            window.key(KeyCode.SPACE, true);
            window.key(KeyCode.SPACE, false);
            assertEquals(1, game.mixWaves().size());
            long struck = game.testAddMixShard(0.2, 0.05, 0, 0);
            run(game, window, 0.5);
            assertTrue(gone(game, struck), "l'onde ramasse l'éclat qu'elle touche");
            // Le clicker : les clics sur l'arène font la cadence.
            stage(game, window, 7);
            for (int k = 0; k < 10; k++) {
                view.getOnMousePressed().handle(mouse(MouseEvent.MOUSE_PRESSED, view.mixScreen(0.6, 0.6), MouseButton.PRIMARY));
                view.getOnMouseReleased().handle(mouse(MouseEvent.MOUSE_RELEASED, view.mixScreen(0.6, 0.6), MouseButton.PRIMARY));
            }
            window.frame(0.016, true);
            assertEquals(5, game.mixClickRate(), 1e-9);
            assertTrue(window.mixInfoText().contains("cadence 5 clics/s ×1.25"), window.mixInfoText());
            // La grille : le halo chauffe, le trésor se trouve en entrant dans sa case.
            stage(game, window, 8);
            game.testHideTreasure(0.95, 0.95);
            game.testPlaceMixCursor(0.05, 0.05, 0);
            double cold = game.mixHeat();
            game.testPlaceMixCursor(0.94, 0.94, 0);
            run(game, window, 0.05);
            assertTrue(cold < 0.2);
            assertEquals(1, game.mixTreasures());
            assertTrue(view.mixLastMark().startsWith("Trésor ! +"), view.mixLastMark());
            // Le miroir : un éclat puis son reflet.
            stage(game, window, 9);
            long probe = game.testAddMixShard(0, 0.95, 0, 0);
            game.testClearMix();
            game.testAddMixShard(0.3, 0.4, 0, probe + 2);
            game.testAddMixShard(0.7, 0.4, 0, probe + 1);
            game.testPlaceMixCursor(0.3, 0.4, 0);
            game.tick(0.05);
            game.testPlaceMixCursor(0.7, 0.4, 0);
            run(game, window, 0.05);
            assertEquals(1, game.mixPairStreak());
            assertTrue(window.mixInfoText().contains("paires de suite 1"), window.mixInfoText());
            assertFalse(window.mixAdvanceButton().isVisible(), "à la dernière étape, plus rien à mêler");
            assertEquals("Tout est mêlé", window.mixProgressText());
            window.close();
            return null;
        });
    }

    @Test
    void leMelangeAutomatiqueJoueEtPasseLesEtapes() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            DimensionWindow window = window(game);
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setMirrorShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.MIX_AUTO));
            game.state().setMixStageEarned(game.mixStageGoal());
            run(game, window, 0.5);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.mixHintText().startsWith("Mélange automatique : il mène le point"), window.mixHintText());
            assertEquals(2, game.mixStage(), "il passe seul à l'étape suivante");
            return null;
        });
    }

    @Test
    void laBulleDesRacinesAttendLaDerniereAmeliorationDeLaGraine() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            UniversePage page = new UniversePage(game);
            new Scene(page, 1000, 700);
            page.select(UniversePage.SEED);
            page.refresh();
            SeedUpgradesPane upgrades = page.seedUpgrades();
            upgrades.select(10);
            assertEquals("Fragments du mélange : 0", upgrades.resourceText().getText());
            game.state().setMixShards(1e6);
            upgrades.refresh();
            upgrades.button(SeedUpgrade.ROOT_BUBBLE).fire();
            assertEquals(1, game.seedUpgradeLevel(SeedUpgrade.ROOT_BUBBLE));
            assertTrue(game.isTreeBubbleOpen(TreeBubble.ROOTS));
            game.state().setMixShardsEarned(Game.MIX_THRESHOLD);
            page.refresh();
            assertTrue(page.dimensionsView().isMarked(10), "le seuil atteint : le rond vert");
            return null;
        });
    }

    @Test
    void leMelangeALaSectionDansLAutomatisationSesStatistiquesEtSaVersion() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setMirrorShards(1e9);
            DimensionAutomationPane pane = new DimensionAutomationPane(game);
            new Scene(pane, 1200, 760);
            pane.refresh();
            assertTrue(pane.sectionShown(10));
            Card play = pane.card(DimensionAutomation.MIX_AUTO);
            assertEquals(Card.State.READY, play.state());
            assertTrue(play.text().contains("Joue le mélange : mène le point"), play.text());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.MIX_AUTO));
            assertEquals(69, DimensionAutomation.values().length);
            assertEquals(12, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 10).count(),
                    "douze automatismes pour le mélange");
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Mélange : fragments : ")), rows.toString());
            assertTrue(rows.contains("Étape du mélange : 1 sur 9, 0 / 1000 fragments gagnés pour la suivante"), rows.toString());
            assertTrue(Version.CURRENT.compareTo("0.159") >= 0, Version.CURRENT);
            return null;
        });
    }
}
