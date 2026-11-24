package idle.ui.bigbang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.sky.Cosmos;
import idle.core.state.GameStats;
import idle.ui.FxTestSupport;
import javafx.event.Event;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.299 : le bouton du rassemblement accéléré, dans le sous-onglet Cosmos. Il n'apparaît qu'avec la bulle et un
 * délai qui court ; tenu, il avance le délai de trois secondes de plus par seconde.
 */
class RassemblementAccelereTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    /** Une partie où la galaxie vient d'être formée : l'amas attend ses deux heures. */
    private static Game galaxyFormed(boolean bubble) {
        Game game = new Game(42L);
        game.start();
        game.testFormUniverse();
        game.state().setCosmosLevel(1);
        game.stats().noteStep(GameStats.Step.GALAXY, game.state().timePlayed());
        if (bubble) game.state().setTreeBubbleBought(Game.TreeBubble.GATHER.ordinal(), true);
        return game;
    }

    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type) {
        return new MouseEvent(type, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false,
                false, false, null);
    }

    @Test
    void leBoutonTenuAvanceLeDelai() throws Exception {
        FxTestSupport.fx(() -> {
            Game game = galaxyFormed(true);
            GalaxyPane pane = new GalaxyPane(game, () -> {}, () -> {});
            pane.refresh();
            assertTrue(pane.holdButton().isVisible(), "la bulle achetée, un délai qui court : le bouton");
            assertTrue(pane.holdButton().getText().contains("l'amas de galaxies"), pane.holdButton().getText());
            double before = game.cosmosSettleRemaining(Cosmos.CLUSTER);
            Event.fireEvent(pane.holdButton(), mouse(MouseEvent.MOUSE_PRESSED));
            assertTrue(pane.holding());
            pane.frame(10);
            assertEquals(before - 30, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-6);
            Event.fireEvent(pane.holdButton(), mouse(MouseEvent.MOUSE_RELEASED));
            assertFalse(pane.holding());
            pane.frame(10);
            assertEquals(before - 30, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-6, "relâché, plus rien");
            return null;
        });
    }

    @Test
    void sansLaBullePasDeBouton() throws Exception {
        FxTestSupport.fx(() -> {
            GalaxyPane pane = new GalaxyPane(galaxyFormed(false), () -> {}, () -> {});
            pane.refresh();
            assertFalse(pane.holdButton().isVisible());
            assertFalse(pane.holdButton().isManaged());
            return null;
        });
    }
}
