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
 * La fenêtre du plan (dimension 2), sous Xvfb : Z Q S D et les flèches dirigent le point tant que la touche est tenue,
 * passer sur un éclat le ramasse et le gain monte à l'écran, les facettes et le seuil s'affichent, les boutons des
 * cinq améliorations suivent les facettes ; fermer la fenêtre arrête le point.
 */
class PlaneWindowTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au deuxième univers : le plan est ouvert, le point au centre. */
    private static Game second() {
        Game game = new Game(42L);
        game.start();
        game.testFormUniverse();
        assertTrue(game.testAddUniverse());
        assertTrue(game.isPlaneOpen());
        game.setPlanePosition(0, 0);
        return game;
    }

    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.PLANE);
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
    void laFenetreDitLesFacettesEtLeSeuil() {
        FxTestSupport.fx(() -> {
            Game game = second();
            DimensionWindow window = window(game);
            assertEquals("Dimension 2 : Le plan", window.titleText());
            assertEquals("Facettes du plan : 0", window.planeShardsText());
            game.state().setPlaneShards(7);
            game.state().setPlaneShardsEarned(Game.PLANE_THRESHOLD);
            window.frame(0.1, true);
            assertEquals("Facettes du plan : 7", window.planeShardsText());
            return null;
        });
    }

    @Test
    void lesTouchesDirigentLePointTantQuEllesSontTenues() {
        FxTestSupport.fx(() -> {
            Game game = second();
            DimensionWindow window = window(game);
            assertTrue(window.key(KeyCode.D, true), "D sert au plan");
            run(game, window, 0.5);
            assertEquals(0.3, game.planeX(), 1e-9, "0,6 par seconde vers la droite");
            assertEquals(0, game.planeY(), 1e-9);
            assertTrue(window.key(KeyCode.D, false));
            run(game, window, 1);
            assertEquals(0.3, game.planeX(), 1e-9, "relâchée, le point s'arrête");
            // Z et D ensemble : en diagonale, vers le haut à droite.
            assertTrue(window.key(KeyCode.Z, true) && window.key(KeyCode.D, true));
            run(game, window, 0.5);
            assertEquals(0.3 + 0.3 / Math.sqrt(2), game.planeX(), 1e-9);
            assertEquals(-0.3 / Math.sqrt(2), game.planeY(), 1e-9, "Z monte (y négatif)");
            assertTrue(window.key(KeyCode.Z, false) && window.key(KeyCode.D, false));
            // Les flèches valent les lettres ; Q et S vont à gauche et en bas.
            game.setPlanePosition(0, 0);
            assertTrue(window.key(KeyCode.LEFT, true) && window.key(KeyCode.S, true));
            run(game, window, 0.5);
            assertEquals(-0.3 / Math.sqrt(2), game.planeX(), 1e-9);
            assertEquals(0.3 / Math.sqrt(2), game.planeY(), 1e-9);
            assertTrue(window.key(KeyCode.LEFT, false) && window.key(KeyCode.S, false));
            // Les autres touches ne servent pas ici.
            assertFalse(window.key(KeyCode.SPACE, true));
            assertFalse(window.key(KeyCode.A, true));
            // Le point ne sort pas du plan.
            assertTrue(window.key(KeyCode.RIGHT, true));
            run(game, window, 5);
            assertEquals(1 - game.planePlayerRadius(), game.planeX(), 1e-9, "arrêté au bord");
            return null;
        });
    }

    @Test
    void passerSurUnEclatLeRamasseEtLeGainMonte() {
        FxTestSupport.fx(() -> {
            Game game = second();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            run(game, window, Game.PLANE_SPAWN_SECONDS * 4);
            assertEquals(3, game.planeShardsOnBoard().size(), "trois éclats posés");
            assertEquals(0, view.pickupsShown());
            Game.PlaneShard shard = game.planeShardsOnBoard().get(0);
            game.setPlanePosition(shard.x(), shard.y());
            game.tick(0.01);
            window.frame(0.01, true);
            assertEquals(1, game.state().planeShards(), 1e-9, "une facette ramassée");
            assertEquals(1, view.pickupsShown(), "le gain monte de l'éclat");
            assertEquals("Facettes du plan : 1", window.planeShardsText());
            run(game, window, 1);
            assertEquals(0, view.pickupsShown(), "puis s'efface");
            return null;
        });
    }

    @Test
    void lesBoutonsDesAmeliorationsSuiventLesFacettes() {
        FxTestSupport.fx(() -> {
            Game game = second();
            DimensionWindow window = window(game);
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).isDisable(), "aucune facette : rien ne s'achète");
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).getText().contains("Prix : 5 facettes"), window.planeButton(Game.PlaneUpgrade.VALUE).getText());
            assertTrue(window.planeButton(Game.PlaneUpgrade.SPAWN).getText().contains("Prix : 3 facettes"));
            game.state().setPlaneShards(5);
            window.frame(0.1, true);
            assertFalse(window.planeButton(Game.PlaneUpgrade.VALUE).isDisable());
            assertFalse(window.planeButton(Game.PlaneUpgrade.COUNT).isDisable(), "le nombre en demande 4");
            window.planeButton(Game.PlaneUpgrade.VALUE).fire();
            assertEquals(1, game.planeLevel(Game.PlaneUpgrade.VALUE));
            assertEquals(2, game.planeValue(), 1e-12);
            assertEquals(0, game.state().planeShards(), 1e-9);
            window.frame(0.1, true);
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).getText().contains("Prix : 15 facettes"), "×3 par niveau");
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).isDisable(), "plus de facettes");
            // Au plus haut : « Au maximum ».
            game.state().setPlaneLevel(Game.PlaneUpgrade.COUNT.ordinal(), Game.PlaneUpgrade.COUNT.max());
            game.state().setPlaneShards(1e9);
            window.frame(0.1, true);
            assertTrue(window.planeButton(Game.PlaneUpgrade.COUNT).getText().contains("Au maximum"));
            assertTrue(window.planeButton(Game.PlaneUpgrade.COUNT).isDisable());
            return null;
        });
    }

    @Test
    void fermerLaFenetreArreteLePointEtLesTouchesNeServentPlus() {
        FxTestSupport.fx(() -> {
            Game game = second();
            DimensionWindow window = window(game);
            assertTrue(window.key(KeyCode.D, true));
            run(game, window, 0.5);
            double x = game.planeX();
            assertTrue(x > 0);
            window.close();
            assertFalse(window.isOpen());
            game.tick(1);
            assertEquals(x, game.planeX(), 1e-9, "fermée, la direction tenue est lâchée");
            assertFalse(window.key(KeyCode.D, true), "fermée, les touches vont au reste du jeu");
            game.tick(1);
            assertEquals(x, game.planeX(), 1e-9);
            // Rouverte, elle repart sans direction.
            window.open(Dimension.PLANE);
            game.tick(1);
            assertEquals(x, game.planeX(), 1e-9);
            return null;
        });
    }

    @Test
    void avantLeDeuxiemeUniversLePlanResteUnVisuel() {
        FxTestSupport.fx(() -> {
            Game game = new Game(42L);
            game.start();
            game.testFormUniverse();
            assertFalse(game.isPlaneOpen());
            DimensionWindow window = window(game);
            assertFalse(window.key(KeyCode.D, true), "fermé, le plan ne prend pas les touches");
            run(game, window, 1);
            assertEquals(0, game.planeX(), 0);
            assertTrue(game.planeShardsOnBoard().isEmpty());
            return null;
        });
    }
}
