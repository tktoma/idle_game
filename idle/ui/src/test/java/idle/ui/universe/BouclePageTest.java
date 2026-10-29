package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.GameUniverse.TreeBubble;
import idle.core.universe.DimensionAutomation;
import idle.ui.FxTestSupport;
import idle.ui.automation.DimensionAutomationPane;
import idle.ui.common.Card;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.153 : la boucle, cinquième dimension, et le rond de croissance de la graine, sous Xvfb. Les cercles se
 * jouent à la souris ou à leurs touches ; un clic juste donne des nœuds, un clic à côté rien, et le cercle attend après
 * chaque clic ; le métronome bat sur le centre des zones du premier cercle ; les améliorations s'achètent dans la
 * fenêtre ; le rythme automatique joue seul. Le rond de croissance pousse dans la vue de la graine, se cueille d'un clic
 * et accélère l'arrosage ; ses deux améliorations sont dans la branche « Boucle ».
 */
class BouclePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au cinquième univers : la boucle est ouverte. */
    private static Game fifth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 4; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isLoopOpen());
        return game;
    }

    /** Des réglages de boucle comptés : les touches d'origine, ou celles qu'on change, et les battements du métronome. */
    private static final class Counted implements DimensionWindow.LoopSettings {
        final KeyCode[] keys = DimensionWindow.DEFAULT_LOOP_KEYS.toArray(new KeyCode[0]);
        boolean metronome;
        int beats;

        @Override
        public KeyCode loopKey(int ring) {
            return keys[ring];
        }

        @Override
        public boolean metronome() {
            return metronome;
        }

        @Override
        public void setMetronome(boolean on) {
            metronome = on;
        }

        @Override
        public void beat() {
            beats++;
        }
    }

    private static DimensionWindow window(Game game, Counted settings) {
        DimensionWindow window = new DimensionWindow(game);
        window.setLoopSettings(settings);
        window.open(Dimension.LOOP);
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

    /** Pose la première zone du cercle à 0,5, et son point à {@code share} demi-largeur de son centre. */
    private static void aim(Game game, int ring, double share) {
        game.setLoopZone(ring, 0, 0.5);
        game.setLoopPhase(ring, 0.5 + share * game.loopZoneHalfWidth());
    }

    @Test
    void unClicJusteDonneDesNoeudsAColeRienEtLeCercleAttend() {
        FxTestSupport.fx(() -> {
            Game game = fifth();
            DimensionWindow window = window(game, new Counted());
            DimensionView view = window.view();
            assertEquals("Dimension 5 : La boucle", window.titleText());
            assertEquals("Nœuds de la boucle : 0", window.loopShardsText());
            assertTrue(window.loopHintText().contains("sa touche (S)"), window.loopHintText());
            aim(game, 0, 0);
            window.frame(0.016, true);
            double[] point = view.loopPoint(0);
            assertEquals(0, view.loopRingAt(point[0], point[1]));
            assertTrue(view.press(point[0], point[1], false), "un clic sur le cercle le joue");
            assertEquals(Game.LoopHit.EXCELLENT, game.lastLoopHit());
            assertEquals(3, game.state().loopShards(), 1e-9, "au centre : trois nœuds");
            assertEquals("Excellent", view.lastLoopMark());
            assertTrue(game.loopWait(0) > 0, "le cercle attend");
            aim(game, 0, 0);
            assertEquals(Game.LoopHit.WAIT, view.clickRing(0), "pendant l'attente, le clic ne compte pas");
            assertEquals(3, game.state().loopShards(), 1e-9);
            assertEquals(1, view.loopMarksShown(), "et rien ne monte");
            run(game, window, 0.5);
            assertEquals(0, game.loopWait(0), 1e-9);
            game.setLoopZone(0, 0, 0.5);
            game.setLoopPhase(0, 0.1);
            assertEquals(Game.LoopHit.MISS, view.clickRing(0));
            assertEquals("Raté", view.lastLoopMark());
            assertEquals(3, game.state().loopShards(), 1e-9, "à côté : rien");
            assertTrue(game.loopWait(0) > 0, "mais le cercle attend quand même");
            run(game, window, 0.5);
            aim(game, 0, 0.4);
            assertEquals(Game.LoopHit.GOOD, view.clickRing(0));
            assertEquals("Bien", view.lastLoopMark());
            run(game, window, 0.5);
            aim(game, 0, -0.8);
            assertEquals(Game.LoopHit.BAD, view.clickRing(0));
            assertEquals("Mauvais", view.lastLoopMark());
            assertEquals(4.25, game.state().loopShards(), 1e-9);
            window.frame(0.016, true);
            assertTrue(window.loopHintText().endsWith("Dernier clic : mauvais."), window.loopHintText());
            assertFalse(view.press(view.getWidth() / 2, view.getHeight() / 2, false), "au milieu, loin des cercles : rien");
            run(game, window, 1);
            assertEquals(0, view.loopMarksShown(), "les mots s'effacent");
            return null;
        });
    }

    @Test
    void chaqueCercleASaToucheEtUneToucheTenueNeRejouePas() {
        FxTestSupport.fx(() -> {
            Game game = fifth();
            Counted settings = new Counted();
            DimensionWindow window = window(game, settings);
            aim(game, 0, 0);
            assertTrue(window.key(KeyCode.S, true));
            assertEquals(3, game.state().loopShards(), 1e-9, "S joue le premier cercle");
            run(game, window, 0.5);
            aim(game, 0, 0);
            assertTrue(window.key(KeyCode.S, true), "la répétition de la touche tenue");
            assertEquals(3, game.state().loopShards(), 1e-9, "ne rejoue pas");
            window.key(KeyCode.S, false);
            assertTrue(window.key(KeyCode.S, true));
            assertEquals(6, game.state().loopShards(), 1e-9, "relâchée puis rappuyée : elle rejoue");
            window.key(KeyCode.S, false);
            long hits = game.loopHits();
            assertTrue(window.key(KeyCode.D, true), "la touche d'un cercle pas encore acheté reste à la fenêtre");
            assertEquals(hits, game.loopHits());
            window.key(KeyCode.D, false);
            assertFalse(window.key(KeyCode.V, true), "les autres touches vont au reste du jeu");
            game.state().setLoopShards(1e9);
            assertTrue(game.buyLoop(Game.LoopUpgrade.RINGS));
            run(game, window, 0.5);
            aim(game, 1, 0);
            double before = game.state().loopShards();
            assertTrue(window.key(KeyCode.D, true));
            assertEquals(before + 3, game.state().loopShards(), 1e-9, "D joue le deuxième cercle");
            assertTrue(game.loopWait(1) > 0 && game.loopWait(0) == 0, "qui attend seul");
            window.key(KeyCode.D, false);
            settings.keys[0] = KeyCode.Q;
            aim(game, 0, 0);
            assertFalse(window.key(KeyCode.S, true), "la touche changée : S ne joue plus");
            assertTrue(window.key(KeyCode.Q, true), "Q, oui");
            window.close();
            assertFalse(window.key(KeyCode.Q, true), "fenêtre fermée : rien");
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentDansLaFenetreEtLeRythmeAutomatiqueJoueSeul() {
        FxTestSupport.fx(() -> {
            Game game = fifth();
            DimensionWindow window = window(game, new Counted());
            assertTrue(window.loopButton(Game.LoopUpgrade.ZONES).isDisable(), "sans nœud, rien ne s'achète");
            game.state().setLoopShards(1e9);
            window.frame(0.016, true);
            assertTrue(window.loopButton(Game.LoopUpgrade.RINGS).getText().startsWith("Cercles : 1, un de plus, sur la touche D"),
                    window.loopButton(Game.LoopUpgrade.RINGS).getText());
            window.loopButton(Game.LoopUpgrade.RINGS).fire();
            window.loopButton(Game.LoopUpgrade.ZONES).fire();
            window.loopButton(Game.LoopUpgrade.GAIN).fire();
            assertEquals(2, game.loopRings());
            assertEquals(2, game.loopZones(0).size());
            assertEquals(2, game.loopValue(), 1e-9);
            assertTrue(window.loopButton(Game.LoopUpgrade.GAIN).getText().startsWith("Gain par zone : chaque zone vaut 2 → 3 nœuds"),
                    window.loopButton(Game.LoopUpgrade.GAIN).getText());
            for (int k = 0; k < 10; k++) window.loopButton(Game.LoopUpgrade.RINGS).fire();
            assertEquals(Game.MAX_RINGS, game.loopRings());
            assertTrue(window.loopButton(Game.LoopUpgrade.RINGS).getText().endsWith("Au maximum"));
            window.frame(0.016, true);
            assertTrue(window.loopHintText().contains("(S, D, F, Espace, J, K, L)"), window.loopHintText());
            assertTrue(window.autoBadgeText().isEmpty());
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setDurationShards(1e9);
            assertTrue(game.buyDimensionAutomation(DimensionAutomation.LOOP_PLAY));
            window.frame(0.016, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.loopHintText().startsWith("Rythme automatique"), window.loopHintText());
            long hits = game.loopHits();
            boolean rose = false;
            for (int i = 0; i < 400 && !rose; i++) {
                game.tick(0.05);
                window.frame(0.05, true);
                rose = window.view().loopMarksShown() > 0;
            }
            assertTrue(rose && game.loopHits() > hits, "ses clics font monter leurs nœuds");
            return null;
        });
    }

    @Test
    void leMetronomeBatAuCentreDesZonesDuPremierCercle() {
        FxTestSupport.fx(() -> {
            Game game = fifth();
            Counted settings = new Counted();
            DimensionWindow window = window(game, settings);
            DimensionView view = window.view();
            assertEquals("Métronome : coupé", window.metronomeButton().getText());
            game.setLoopZone(0, 0, 0.6);
            game.setLoopPhase(0, 0.55);
            window.frame(0.016, true);
            run(game, window, 1.2);
            assertEquals(0, settings.beats, "coupé : pas de tic");
            window.metronomeButton().fire();
            assertTrue(settings.metronome);
            assertTrue(window.metronomeButton().getText().startsWith("Métronome : activé"));
            game.setLoopPhase(0, 0.55);
            window.frame(0.016, true);
            run(game, window, 1.2);
            assertEquals(1, settings.beats, "un tic au passage du centre");
            assertEquals(1, view.beats());
            run(game, window, 1);
            assertEquals(1, settings.beats, "un seul");
            return null;
        });
    }

    @Test
    void leRondDeCroissancePousseSeCueilleEtAccelereLArrosage() {
        FxTestSupport.fx(() -> {
            Game game = fifth();
            game.state().setWateringCan(true);
            UniversePage page = new UniversePage(game);
            new Scene(page, 1000, 700);
            page.select(UniversePage.SEED);
            page.refresh();
            page.applyCss();
            page.layout();
            page.frame(0.016, true);
            SeedView view = page.seedView();
            assertFalse(view.isBloomShown());
            game.testShowBloom();
            page.frame(0.016, true);
            assertTrue(view.isBloomShown());
            double[] at = view.bloomPoint();
            assertEquals(game.bloomX() * view.getWidth(), at[0], 1e-9);
            assertFalse(view.clickBloom(at[0] + view.bloomRadius() * 3, at[1] + view.bloomRadius() * 3), "à côté : rien");
            double rate = game.wateringCanRate();
            assertTrue(view.clickBloom(at[0], at[1]));
            assertFalse(game.isBloomVisible());
            assertEquals(30, game.bloomActiveLeft(), 1e-9);
            assertEquals(rate * 1.25, game.wateringCanRate(), 1e-9);
            page.frame(0.016, true);
            assertEquals("Croissance +25 % : 30 s", view.bloomText());
            assertFalse(view.isBloomShown());
            for (int i = 0; i < 70; i++) {
                game.tick(0.1);
                page.frame(0.1, true);
            }
            assertEquals("Croissance +25 % : 23 s", view.bloomText());
            assertFalse(game.isBloomVisible());
            game.tick(30);
            page.frame(0.016, true);
            assertEquals("", view.bloomText());
            assertEquals(rate, game.wateringCanRate(), 1e-9);
            // Poussé seul, au bout de deux minutes environ, puis cueilli : il attend de nouveau avant de repousser.
            for (int i = 0; i < 400 && !game.isBloomVisible(); i++) game.tick(1);
            assertTrue(game.catchBloom());
            game.tick(1);
            assertFalse(game.isBloomVisible(), "pas aussitôt après la cueillette");
            SeedUpgradesPane upgrades = page.seedUpgrades();
            assertEquals("Boucle", SeedUpgradesPane.branchName(5));
            assertTrue(upgrades.isBranchOpen(5));
            upgrades.select(5);
            assertTrue(upgrades.button(SeedUpgrade.BLOOM_DURATION).getText()
                    .startsWith("Durée du rond de croissance (niveau 0) : 30 → 40 s"), upgrades.button(SeedUpgrade.BLOOM_DURATION).getText());
            game.state().setLoopShards(1000);
            upgrades.refresh();
            upgrades.button(SeedUpgrade.BLOOM_POWER).fire();
            assertEquals(0.35, game.bloomGain(), 1e-9);
            assertTrue(upgrades.button(SeedUpgrade.BLOOM_POWER).getText().startsWith("Force du rond de croissance (niveau 1) : arrosage +35 % → +45 %"),
                    upgrades.button(SeedUpgrade.BLOOM_POWER).getText());
            return null;
        });
    }

    @Test
    void laBoucleALaSectionDansLAutomatisationEtSonRondVert() {
        FxTestSupport.fx(() -> {
            Game game = fifth();
            game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
            game.state().setDurationShards(1e9);
            DimensionAutomationPane pane = new DimensionAutomationPane(game);
            new Scene(pane, 1200, 760);
            pane.refresh();
            assertTrue(pane.sectionShown(5));
            assertEquals("Dimension 5 : La boucle", pane.headingText(5));
            Card play = pane.card(DimensionAutomation.LOOP_PLAY);
            assertEquals(Card.State.READY, play.state());
            assertTrue(play.text().contains("Clique « bien » à chaque passage d'un point dans une zone"), play.text());
            play.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.LOOP_PLAY));
            UniversePage page = new UniversePage(game);
            page.refresh();
            assertFalse(page.dimensionsView().isMarked(5));
            game.state().setLoopShardsEarned(Game.LOOP_THRESHOLD);
            page.refresh();
            assertTrue(page.dimensionsView().isMarked(5), "le seuil atteint : le rond vert");
            return null;
        });
    }
}
