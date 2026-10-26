package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.FxTestSupport;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.137 : l'arrosoir de la graine, sous Xvfb. Le bouton de l'étagère le débloque ; on le prend, on le glisse
 * sur la graine, il se penche et verse ; relâché, il revient sur l'étagère.
 */
class ArrosoirPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** La page de la graine, dans une scène de 1000 × 700, l'objectif de la dimension 1 atteint. */
    private static UniversePage page(Game game) {
        UniversePage page = new UniversePage(game);
        new Scene(page, 1000, 700);
        page.select(UniversePage.SEED);
        page.refresh();
        page.applyCss();
        page.layout();
        page.frame(0.016, true);
        return page;
    }

    private static Game ready() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        return game;
    }

    @Test
    void leBoutonDeLEtagereDebloqueLArrosoir() {
        FxTestSupport.fx(() -> {
            Game game = ready();
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertTrue(view.getWidth() > 100 && view.getHeight() > 100, "la vue a sa taille : " + view.getWidth());
            assertTrue(page.unlockCanButton().isVisible());
            assertTrue(page.seedStatus().getText().contains("Débloquez l'arrosoir"), page.seedStatus().getText());
            // Le bouton se tient au-dessus de l'étagère.
            double middle = page.unlockCanButton().getLayoutX() + page.unlockCanButton().getWidth() / 2;
            assertEquals(view.shelfX(), middle, 2);
            assertTrue(page.unlockCanButton().getLayoutY() < view.shelfTop());
            assertFalse(view.grab(view.canX(), view.canY()), "verrouillé, il ne se prend pas");
            page.unlockCanButton().fire();
            assertTrue(game.isWateringCanUnlocked());
            assertFalse(page.unlockCanButton().isVisible(), "le bouton laisse la place à l'arrosoir");
            assertTrue(view.isCanUnlocked());
            return null;
        });
    }

    @Test
    void glisseSurLaGraineIlSePencheEtArrosePuisRevientSurLEtagere() {
        FxTestSupport.fx(() -> {
            Game game = ready();
            assertTrue(game.unlockWateringCan());
            UniversePage page = page(game);
            SeedView view = page.seedView();
            double homeX = view.canX(), homeY = view.canY();
            assertTrue(view.grab(homeX, homeY), "on le prend sur l'étagère");
            // Le bec au-dessus de la graine, au milieu de la vue.
            double size = Math.max(26, Math.min(view.getWidth(), view.getHeight()) * 0.11);
            view.drag(view.getWidth() / 2 - size * 0.95, view.getHeight() * 0.25);
            for (int i = 0; i < 60; i++) page.frame(0.016, true);
            assertTrue(view.tilt() > 0.9, "penché : " + view.tilt());
            assertTrue(view.isPouring());
            assertTrue(game.isWatering(), "la page dit au jeu que l'arrosoir verse");
            game.tick(1);
            page.frame(0.016, true);
            assertTrue(game.state().seedSprouted(), "la première seconde fait germer la graine");
            assertTrue(view.streamDrops() > 0, "l'eau coule du bec");
            game.tick(3);
            page.frame(0.016, true);
            assertEquals(3, game.state().seedWaterings());
            assertTrue(page.seedStatus().getText().contains("3 arrosages"), page.seedStatus().getText());
            view.release();
            page.frame(0.016, true);
            assertFalse(view.isPouring());
            assertFalse(game.isWatering(), "relâché, il ne verse plus");
            for (int i = 0; i < 120; i++) page.frame(0.016, true);
            assertEquals(homeX, view.canX(), 1, "il revient sur l'étagère");
            assertEquals(homeY, view.canY(), 1);
            assertTrue(view.tilt() < 0.05, "et se redresse");
            return null;
        });
    }

    @Test
    void tenuLoinDeLaGraineIlNeVersePas() {
        FxTestSupport.fx(() -> {
            Game game = ready();
            assertTrue(game.unlockWateringCan());
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertTrue(view.grab(view.canX(), view.canY()));
            view.drag(view.getWidth() - 60, view.getHeight() - 30);
            for (int i = 0; i < 60; i++) page.frame(0.016, true);
            assertFalse(view.isPouring());
            assertTrue(view.tilt() < 0.05, "droit, loin de la graine");
            assertFalse(game.isWatering());
            return null;
        });
    }
}
