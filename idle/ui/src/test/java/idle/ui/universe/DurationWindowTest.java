package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.FxTestSupport;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.134 : la fenêtre de la durée, sous Xvfb. Le bouton du milieu se tient à la souris ou à la barre d'espace,
 * les spirales avancent, le relâchement donne les éclats ; les améliorations attendent que le bouton soit relâché.
 */
class DurationWindowTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au quatrième univers. */
    private static Game fourth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 3; k++) assertTrue(game.testAddUniverse());
        assertTrue(game.isDurationOpen());
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.DURATION);
        window.frame(0.05, true);
        return window;
    }

    /** Fait passer le temps du jeu et de la fenêtre ensemble. */
    private static void run(Game game, DimensionWindow window, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.1) {
            game.tick(0.1);
            window.frame(0.1, true);
        }
    }

    @Test
    void tenirLeBoutonDuMilieuFaitAvancerLesSpiralesEtRelacherDonneLesEclats() {
        FxTestSupport.fx(() -> {
            Game game = fourth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertTrue(window.titleText().contains("Dimension 4"));
            assertTrue(window.durationShardsText().contains("Instants de la durée"));
            double cx = view.getWidth() / 2, cy = view.getHeight() / 2;
            assertFalse(view.press(cx + view.getWidth() * 0.3, cy, false), "ailleurs dans le rond, rien");
            assertFalse(game.isDurationHeld());
            assertTrue(view.overDurationButton(cx, cy));
            assertTrue(view.press(cx, cy, false), "le bouton du milieu");
            assertTrue(game.isDurationHeld());
            run(game, window, 4);
            assertEquals(4, game.durationHeldSeconds(), 0.15);
            assertTrue(window.durationHintText().contains("relâcher maintenant donnerait"), window.durationHintText());
            assertTrue(window.durationButton(Game.DurationUpgrade.VALUE).isDisable(), "pas d'achat bouton tenu");
            assertTrue(view.releaseHold());
            assertFalse(game.isDurationHeld());
            assertEquals(3, game.state().durationShards(), 0.3, "deux tours : 1 + 2");
            assertEquals(1, view.durationPickupsShown(), "le gain monte du bouton");
            run(game, window, 2);
            assertEquals(0, view.durationPickupsShown(), "puis s'efface");
            assertFalse(view.releaseHold(), "rien n'est tenu");
            return null;
        });
    }

    @Test
    void auCentreLaFenetreDitDeRelacher() {
        FxTestSupport.fx(() -> {
            Game game = fourth();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertTrue(view.press(view.getWidth() / 2, view.getHeight() / 2, false));
            run(game, window, 7);
            assertEquals(1, game.durationProgress(), 1e-9);
            assertTrue(window.durationHintText().contains("au centre"), window.durationHintText());
            window.close();
            assertFalse(game.isDurationHeld(), "fermer la fenêtre relâche le bouton");
            assertEquals(6, game.state().durationShards(), 1e-9, "et donne ce qu'il avait : trois tours");
            return null;
        });
    }

    @Test
    void laBarreDEspaceTientLeBoutonAussi() {
        FxTestSupport.fx(() -> {
            Game game = fourth();
            DimensionWindow window = window(game);
            assertTrue(window.key(KeyCode.SPACE, true));
            assertTrue(game.isDurationHeld());
            run(game, window, 2);
            assertTrue(window.key(KeyCode.SPACE, true), "la répétition de la touche ne relance rien");
            assertEquals(2, game.durationHeldSeconds(), 0.15);
            assertTrue(window.key(KeyCode.SPACE, false));
            assertFalse(game.isDurationHeld());
            assertTrue(game.state().durationShards() > 0);
            assertFalse(window.key(KeyCode.Z, true), "les autres touches ne servent pas ici");
            return null;
        });
    }

    @Test
    void lesAmeliorationsSAchetentBoutonRelache() {
        FxTestSupport.fx(() -> {
            Game game = fourth();
            game.state().setDurationShards(100);
            DimensionWindow window = window(game);
            assertFalse(window.durationButton(Game.DurationUpgrade.COUNT).isDisable());
            window.durationButton(Game.DurationUpgrade.COUNT).fire();
            assertEquals(2, game.durationSpirals());
            window.frame(0.1, true);
            assertTrue(window.durationButton(Game.DurationUpgrade.COUNT).getText().contains("Spirales : 2"));
            return null;
        });
    }

    @Test
    void avantLeQuatriemeUniversLaDureeResteUnVisuel() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.testFormUniverse();
            game.testAddUniverse();
            game.testAddUniverse();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertFalse(view.press(view.getWidth() / 2, view.getHeight() / 2, false));
            assertFalse(game.isDurationHeld());
            assertFalse(window.key(KeyCode.SPACE, true));
            return null;
        });
    }
}
