package idle.ui.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.universe.DimensionAutomation;
import idle.ui.FxTestSupport;
import idle.ui.common.Card;
import idle.ui.universe.*;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.*;

/**
 * L'automatisation des dimensions dans l'interface, sous Xvfb. Le sous-onglet « Dimensions » de
 * l'automatisation (visible avec la bulle du sommet), ses sections et ses cartes, l'achat, la pause, la coupure
 * générale, la partie secondaire ; puis les fenêtres du plan, du volume et de la durée jouées seules.
 */
class DimensionAutomationPageTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    /**
     * Une partie à {@code universes} univers (autant de dimensions ouvertes), de quoi payer ; {@code open} ouvre les automatismes
     * de ces dimensions (version 0.182 : la graine à l'arbre-monde et un univers de plus ; la bulle du sommet avant).
     */
    private static Game game(int universes, boolean open) {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 1; k < universes; k++) assertTrue(game.testAddUniverse());
        if (open) {
            assertTrue(game.testAddUniverse());
            game.state().setSeedSprouted(true);
            game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
            assertTrue(game.isDimensionAutomationOpen(universes));
        }
        game.state().setUniverseSeeds(BigNum.of(100));
        game.state().setDimensionShards(1e9);
        game.state().setPlaneShards(1e9);
        game.state().setVolumeShards(1e9);
        game.state().setDurationShards(1e9);
        return game;
    }

    private static AutomationPage page(Game game) {
        AutomationPage page = new AutomationPage(game);
        new Scene(page, 1200, 760);
        page.refresh();
        page.applyCss();
        page.layout();
        return page;
    }

    /**
     * Achète, dans l'ordre, les automatismes de sa dimension jusqu'à {@code last} compris (version 0.209 : ceux qui achètent
     * d'abord, coupés ensuite pour que seul celui qui joue tourne).
     */
    private static void buyUpTo(Game game, DimensionAutomation last) {
        FxTestSupport.buyPlay(game, last);
        assertTrue(game.ownsDimensionAutomation(last), last.toString());
    }

    /** Fait passer le temps du jeu et de la fenêtre ensemble. */
    private static void run(Game game, DimensionWindow window, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) {
            game.tick(0.05);
            window.frame(0.05, true);
        }
    }

    @Test
    void leSousOngletDimensionsApparaitAvecLePalierDeLaGraineEtLesUnivers() {
        FxTestSupport.fx(() -> {
            AutomationPage bare = page(game(4, false));
            assertTrue(bare.ordinaryTab().isVisible());
            assertFalse(bare.dimensionsTab().isVisible(), "sans le palier de la graine, pas de « Dimensions »");
            bare.step(1);
            assertEquals(AutomationPage.ORDINARY, bare.selected());

            Game game = game(4, true);
            assertTrue(AutomationPage.hasContent(game));
            AutomationPage page = page(game);
            assertTrue(page.dimensionsTab().isVisible());
            page.dimensionsTab().fire();
            assertEquals(AutomationPage.DIMENSIONS, page.selected());
            DimensionAutomationPane dims = page.dimensionsPane();
            for (int number = 1; number <= 4; number++) assertTrue(dims.sectionShown(number), "section " + number);
            assertEquals("Dimension 2 : Le plan", dims.headingText(2));
            assertTrue(dims.introText().contains("« Couper tous les automatismes » les arrête tous"), dims.introText());
            assertTrue(dims.introText().contains("traversent les univers"), dims.introText());
            page.step(1);
            assertEquals(AutomationPage.ORDINARY, page.selected(), "Haut/Bas passe d'un sous-onglet à l'autre");

            AutomationPage few = page(game(2, true));
            few.select(AutomationPage.DIMENSIONS);
            few.refresh();
            assertTrue(few.dimensionsPane().sectionShown(2));
            assertTrue(few.dimensionsPane().sectionShown(3), "trois univers : le volume est ouvert, ses automatismes pas encore");
            assertEquals(Card.State.LOCKED, few.dimensionsPane().card(DimensionAutomation.VOLUME_TIME).state());
            assertTrue(few.dimensionsPane().card(DimensionAutomation.VOLUME_TIME).text().contains("Avec le bonsaï et 4 univers formés"),
                    few.dimensionsPane().card(DimensionAutomation.VOLUME_TIME).text());
            assertFalse(few.dimensionsPane().sectionShown(4), "la durée n'est pas ouverte");
            return null;
        });
    }

    @Test
    void lesCartesSAchetentPuisSeMettentEnPause() {
        FxTestSupport.fx(() -> {
            Game game = game(4, true);
            AutomationPage page = page(game);
            page.select(AutomationPage.DIMENSIONS);
            page.refresh();
            DimensionAutomationPane dims = page.dimensionsPane();
            Card yield = dims.card(DimensionAutomation.LINE_YIELD);
            assertEquals(Card.State.READY, yield.state());
            assertTrue(yield.text().contains("Achète « Densité » dès qu'il est payable"), yield.text());
            assertTrue(yield.text().contains(DimensionAutomationPane.price(game, DimensionAutomation.LINE_YIELD)) && yield.text().contains(" segments"), yield.text());
            Card ticks = dims.card(DimensionAutomation.LINE_TICKS);
            assertEquals(Card.State.LOCKED, ticks.state());
            assertTrue(ticks.text().contains("Après : Densité"), ticks.text());
            // Ceux qui jouent, en dernier : une seule carte pour leurs cinq paliers (0.268), qui dit le palier d'avant.
            Card aim = dims.card(DimensionAutomation.PLANE_AIM_5S);
            assertSame(aim, dims.card(DimensionAutomation.PLANE_AIM), "les cinq paliers sur la même carte");
            assertTrue(aim.text().contains("Visée automatique"), aim.text());
            assertTrue(aim.text().contains("0/5"), aim.text());
            assertTrue(aim.text().contains("Mène le point vers l'éclat le plus proche quand vous ne tenez aucune touche ; reprend sa direction toutes les 5 s"), aim.text());
            assertTrue(aim.text().contains("Après : Taille du point"), aim.text());
            assertTrue(dims.card(DimensionAutomation.VOLUME_DIVE_05S).text().contains("Plonge seul et cueille une boule toutes les 5 s"));

            yield.click();
            assertTrue(game.ownsDimensionAutomation(DimensionAutomation.LINE_YIELD));
            assertEquals(Card.State.ON, yield.state());
            assertTrue(yield.text().contains("en marche"), yield.text());
            assertEquals(Card.State.READY, ticks.state(), "le suivant s'ouvre");
            yield.click();
            assertFalse(game.isDimensionAutomationEnabled(DimensionAutomation.LINE_YIELD));
            assertTrue(yield.text().contains("en pause"), yield.text());
            yield.click();
            assertTrue(game.isDimensionAutomationRunning(DimensionAutomation.LINE_YIELD));

            dims.pauseButton().fire();
            assertTrue(game.isAutomationPaused());
            assertFalse(game.isDimensionAutomationRunning(DimensionAutomation.LINE_YIELD), "« Tout couper » l'arrête");
            assertTrue(yield.text().contains("tout coupé"), yield.text());
            dims.pauseButton().fire();
            assertFalse(game.isAutomationPaused());

            page.setOldTabsHidden(true);
            page.refresh();
            assertFalse(page.ordinaryTab().isVisible(), "partie secondaire : seules les dimensions");
            assertEquals(AutomationPage.DIMENSIONS, page.selected());
            return null;
        });
    }

    /** Version 0.268 : la carte des paliers achète le suivant quand il est payable, sinon met tout en pause ou relance. */
    @Test
    void lesCinqPaliersSAchetentSurLaMemeCarte() {
        FxTestSupport.fx(() -> {
            Game game = game(4, true);
            AutomationPage page = page(game);
            page.select(AutomationPage.DIMENSIONS);
            buyUpTo(game, DimensionAutomation.PLANE_SIZE);
            FxTestSupport.fillDimension(game, 2, 1e30);
            page.refresh();
            DimensionAutomationPane dims = page.dimensionsPane();
            Card aim = dims.card(DimensionAutomation.PLANE_AIM_5S);
            assertEquals(Card.State.READY, aim.state());
            assertFalse(dims.playSwitch(2).isVisible(), "rien d'acheté : pas de bouton « Couper »");
            aim.click();
            assertTrue(game.ownsDimensionAutomation(DimensionAutomation.PLANE_AIM_5S));
            // Version 0.269 : le bouton sous la carte coupe même quand le palier suivant est payable.
            assertTrue(dims.playSwitch(2).isVisible());
            assertEquals("Couper", dims.playSwitch(2).getText());
            dims.playSwitch(2).fire();
            assertFalse(game.isDimensionPlayEnabled(2));
            assertEquals("Relancer", dims.playSwitch(2).getText());
            assertTrue(aim.text().contains("en pause"), aim.text());
            dims.playSwitch(2).fire();
            assertTrue(game.isDimensionPlayEnabled(2));
            assertEquals(Card.State.READY, aim.state(), "le palier suivant, payable : la carte reste à portée");
            assertTrue(aim.text().contains("1/5") && aim.text().contains("en marche, toutes les 5 s") && aim.text().contains("→ toutes les 2 s"), aim.text());
            for (int i = 0; i < 4; i++) aim.click();
            assertEquals(DimensionAutomation.PLANE_AIM, game.ownedPlayTier(2), "cinq clics, cinq paliers");
            assertNull(game.nextPlayTier(2));
            assertEquals(Card.State.ON, aim.state());
            assertTrue(aim.text().contains("5/5") && aim.text().contains("sans attente"), aim.text());
            aim.click();
            assertFalse(game.isDimensionPlayEnabled(2), "tout acheté : un clic met tous les paliers en pause");
            for (DimensionAutomation tier : DimensionAutomation.playTiers(2)) assertFalse(game.isDimensionAutomationEnabled(tier));
            assertEquals(Card.State.OFF, aim.state());
            aim.click();
            assertTrue(game.isDimensionPlayEnabled(2));
            assertEquals(DimensionAutomation.PLANE_AIM, game.dimensionPlayTier(2));
            return null;
        });
    }

    @Test
    void lesFenetresSuiventLeJeuQuiJoueSeul() {
        FxTestSupport.fx(() -> {
            Game game = game(4, true);
            DimensionWindow window = new DimensionWindow(game);
            new Scene(window, 1200, 760);

            window.open(Dimension.PLANE);
            window.frame(0.05, true);
            assertEquals("", window.autoBadgeText());
            buyUpTo(game, DimensionAutomation.PLANE_AIM);
            window.frame(0.05, true);
            assertEquals("automatique", window.autoBadgeText());
            assertTrue(window.planeHintText().contains("Visée automatique"), window.planeHintText());
            window.close();

            buyUpTo(game, DimensionAutomation.VOLUME_DIVE);
            window.open(Dimension.VOLUME);
            DimensionView view = window.view();
            boolean inside = false;
            for (int i = 0; i < 100 && !inside; i++) {
                game.tick(0.05);
                window.frame(0.05, true);
                inside = view.dive() == DimensionView.Dive.INSIDE;
            }
            assertTrue(inside, "sans clic, la vue plonge avec la partie");
            assertTrue(window.volumeHintText().startsWith("Plongée automatique"), window.volumeHintText());
            view.press(view.getWidth() / 2, view.getHeight() / 2, true);
            window.close();
            game.tick(0.05);
            assertTrue(game.isInVolume(), "fenêtre fermée, la plongée automatique continue");

            buyUpTo(game, DimensionAutomation.DURATION_HOLD);
            window.open(Dimension.DURATION);
            run(game, window, 0.1);
            assertTrue(game.isDurationHeld());
            assertTrue(window.durationHintText().startsWith("Appui automatique"), window.durationHintText());
            long releases = game.durationReleases();
            view.press(view.getWidth() / 2, view.getHeight() / 2, false);
            assertFalse(view.releaseHold(), "le joueur ne relâche pas l'appui automatique");
            window.key(KeyCode.SPACE, true);
            window.key(KeyCode.SPACE, false);
            assertTrue(game.isDurationHeld());
            assertEquals(releases, game.durationReleases());
            run(game, window, game.durationMaxSeconds() + 0.2);
            assertTrue(game.durationReleases() > releases, "il relâche seul au centre");
            assertTrue(view.durationPickupsShown() > 0, "et ses instants montent du bouton");
            game.setAutomationPaused(true);
            run(game, window, 0.1);
            assertEquals("", window.autoBadgeText());
            assertTrue(view.releaseHold(), "tout coupé : le relâchement du joueur compte de nouveau");
            game.setAutomationPaused(false);
            window.close();
            return null;
        });
    }
}
