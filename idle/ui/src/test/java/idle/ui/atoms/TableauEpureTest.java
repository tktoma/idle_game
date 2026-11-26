package idle.ui.atoms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.FxTestSupport;
import javafx.scene.Scene;
import org.junit.jupiter.api.*;

/**
 * Version 0.314 : le haut du tableau périodique (décompte, carte de synthèse, explosion, dernier tirage) sur une ligne en
 * grand, la carte et l'explosion côte à côte en petit ; les textes d'explication passés au survol.
 */
class TableauEpureTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    private static Game game() {
        Game game = new Game();
        game.start();
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        return game;
    }

    private static PeriodicTablePage page(Game game, double width) {
        PeriodicTablePage page = new PeriodicTablePage(game);
        new Scene(page, width, 760);
        page.applyCss();
        page.layout();
        page.refresh();
        return page;
    }

    @Test
    void leDecompteTientSurPlusieursLignesEnGrandEtSurUneEnPetit() {
        FxTestSupport.fx(() -> {
            Game game = game();
            PeriodicTablePage wide = page(game, 1400);
            assertTrue(wide.progressText().contains("\n"), "en grand, une ligne par nombre : " + wide.progressText());
            PeriodicTablePage narrow = page(game, 600);
            assertFalse(narrow.progressText().contains("\n"), "en petit, une seule ligne : " + narrow.progressText());
            assertTrue(narrow.progressText().contains("|"), narrow.progressText());
            return null;
        });
    }

    @Test
    void lExplosionNeSeMontreQueQuandLeTableauPeutExploser() {
        FxTestSupport.fx(() -> {
            Game game = game();
            PeriodicTablePage page = page(game, 1400);
            int[] clicks = {0};
            page.setExplosion(() -> clicks[0]++, () -> 0, () -> "+12 matière noire");
            page.refresh();
            assertEquals(game.canExplode(), page.explodeButton().isVisible());
            if (!game.canExplode()) assertFalse(page.explodeButton().isManaged() && !game.isDarkMatterUnlocked(),
                    "avant la matière noire, pas même sa place");
            page.explodeButton().fire();
            assertEquals(1, clicks[0], "le clic demande l'explosion, avec la confirmation du jeu");
            return null;
        });
    }
}
