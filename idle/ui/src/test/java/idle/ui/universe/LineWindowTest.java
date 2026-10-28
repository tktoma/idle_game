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
 * La fenêtre de la ligne (dimension 1), sous Xvfb : son titre et ses textes, les traits qui s'allument quand une boule
 * les touche, les boutons des cinq améliorations ; elle ne prend ni les touches ni les clics, la ligne se joue seule.
 */
class LineWindowTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au premier univers formé : la ligne est ouverte, la boule au départ, sans segment. */
    private static Game first() {
        Game game = new Game(42L);
        game.start();
        game.testFormUniverse();
        assertTrue(game.isLineOpen());
        game.state().setLinePhase(0);
        game.state().setDimensionShards(0);
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.LINE);
        window.frame(0.05, true);
        return window;
    }

    /** Fait passer le temps du jeu et de la fenêtre ensemble, par images de 0,1 s. */
    private static void run(Game game, DimensionWindow window, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.1) {
            game.tick(0.1);
            window.frame(0.1, true);
        }
    }

    @Test
    void laFenetreDitLesSegmentsEtLeSeuil() {
        FxTestSupport.fx(() -> {
            Game game = first();
            DimensionWindow window = window(game);
            assertTrue(window.isOpen());
            assertEquals(Dimension.LINE, window.dimension());
            assertEquals("Dimension 1 : La ligne", window.titleText());
            assertTrue(window.shardsText().startsWith("Segments de la ligne : 0 "), window.shardsText());
            assertTrue(window.shardsText().contains("par seconde"), window.shardsText());
            assertEquals("Seuil : 0 / 250000 segments gagnés", window.thresholdText());
            game.state().setDimensionShards(12.5);
            game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
            window.frame(0.1, true);
            assertTrue(window.shardsText().startsWith("Segments de la ligne : 12.5 "), window.shardsText());
            assertTrue(window.thresholdText().startsWith("Seuil atteint : 250000 segments gagnés"), window.thresholdText());
            window.close();
            assertFalse(window.isOpen());
            return null;
        });
    }

    @Test
    void unTraitSAllumeQuandLaBouleLeTouchePuisSEteint() {
        FxTestSupport.fx(() -> {
            Game game = first();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals(0, view.touchesShown(), "au départ, aucun trait allumé");
            // La boule met 2,4 s à atteindre le bout : à mi-chemin, rien ; au bout, le trait s'allume.
            run(game, window, 1.0);
            assertEquals(0, view.touchesShown());
            run(game, window, 1.5);
            assertEquals(1, view.touchesShown(), "le trait du bout");
            assertEquals(0.1, game.state().dimensionShards(), 1e-9, "et son dixième de segment");
            // Un trait reste allumé 1,2 s, puis s'éteint ; le retour au début allume l'autre bout.
            run(game, window, 1.3);
            assertEquals(0, view.touchesShown());
            run(game, window, 1.1);
            assertEquals(1, view.touchesShown(), "le trait du début, au retour");
            assertEquals(0.2, game.state().dimensionShards(), 1e-9);
            return null;
        });
    }

    @Test
    void lesBoutonsDesAmeliorationsSuiventLesSegments() {
        FxTestSupport.fx(() -> {
            Game game = first();
            DimensionWindow window = window(game);
            assertTrue(window.lineButton(Game.LineUpgrade.YIELD).isDisable(), "aucun segment : rien ne s'achète");
            assertTrue(window.lineButton(Game.LineUpgrade.YIELD).getText().contains("Densité (niveau 0)"));
            assertTrue(window.lineButton(Game.LineUpgrade.YIELD).getText().contains("Prix : 1 segment"), window.lineButton(Game.LineUpgrade.YIELD).getText());
            assertTrue(window.lineButton(Game.LineUpgrade.TICKS).getText().contains("Prix : 2 segments"));
            assertTrue(window.lineButton(Game.LineUpgrade.LINES).getText().contains("Prix : 25 segments"));
            assertEquals(window.lineButton(Game.LineUpgrade.YIELD), window.yieldButton());
            assertEquals(window.lineButton(Game.LineUpgrade.TICKS), window.ticksButton());
            game.state().setDimensionShards(3);
            window.frame(0.1, true);
            assertFalse(window.lineButton(Game.LineUpgrade.YIELD).isDisable(), "1 segment : la densité s'achète");
            assertFalse(window.lineButton(Game.LineUpgrade.TICKS).isDisable());
            assertTrue(window.lineButton(Game.LineUpgrade.SPEED).isDisable(), "l'élan en demande 5");
            window.lineButton(Game.LineUpgrade.YIELD).fire();
            assertEquals(1, game.state().lineYieldLevel());
            assertEquals(2, game.state().dimensionShards(), 1e-9);
            window.frame(0.1, true);
            assertTrue(window.lineButton(Game.LineUpgrade.YIELD).getText().contains("Densité (niveau 1)"));
            assertTrue(window.lineButton(Game.LineUpgrade.YIELD).getText().contains("Prix : 3 segments"), "×3 par niveau");
            // Au plus haut : « Au maximum », et le bouton est éteint même riche.
            game.state().setLineCountLevel(Game.LINE_MAX_EXTRA_LINES);
            game.state().setDimensionShards(1e9);
            window.frame(0.1, true);
            assertTrue(window.lineButton(Game.LineUpgrade.LINES).getText().contains("Au maximum"));
            assertTrue(window.lineButton(Game.LineUpgrade.LINES).isDisable());
            assertTrue(window.lineButton(Game.LineUpgrade.LINES).getText().contains("Lignes (5)"));
            return null;
        });
    }

    @Test
    void laLigneNePrendNiLesTouchesNiLesClics() {
        FxTestSupport.fx(() -> {
            Game game = first();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            for (KeyCode code : new KeyCode[] {KeyCode.Z, KeyCode.Q, KeyCode.S, KeyCode.D, KeyCode.SPACE, KeyCode.UP}) {
                assertFalse(window.key(code, true), code + " ne sert pas sur la ligne");
            }
            assertFalse(view.press(view.getWidth() / 2, view.getHeight() / 2, false), "un clic sur la ligne ne fait rien");
            assertFalse(view.releaseHold());
            assertEquals(0, game.planeX(), 0, "le point du plan n'a pas bougé");
            return null;
        });
    }

    @Test
    void avantLUniversLaLigneResteUnVisuel() {
        FxTestSupport.fx(() -> {
            Game game = new Game(42L);
            game.start();
            assertFalse(game.isLineOpen());
            DimensionWindow window = window(game);
            assertEquals("Dimension 1 : La ligne", window.titleText());
            run(game, window, 3);
            assertEquals(0, window.view().touchesShown(), "fermée, la ligne ne touche aucun trait");
            assertEquals(0, game.state().dimensionShards(), 0);
            return null;
        });
    }
}
