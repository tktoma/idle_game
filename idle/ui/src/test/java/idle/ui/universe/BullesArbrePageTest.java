package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.ui.FxTestSupport;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.142 : les bulles de l'arbre sur la page de la graine, sous Xvfb. Elles naissent avec les paliers, se
 * cliquent pour s'acheter avec l'énergie du vide accumulée, suivent leurs liens ; le sous-onglet « Vide » est parti.
 */
class BullesArbrePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static Game at(int stage, double accumulated) {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(stage));
        if (accumulated > 0) game.state().noteVoidHistory("3", accumulated);
        return game;
    }

    private static UniversePage page(Game game) {
        UniversePage page = new UniversePage(game);
        new Scene(page, 1200, 760);
        page.select(UniversePage.SEED);
        page.refresh();
        page.applyCss();
        page.layout();
        for (int i = 0; i < 10; i++) page.frame(0.016, true);
        return page;
    }

    @Test
    void leSousOngletDuVideEstParti() {
        FxTestSupport.fx(() -> {
            UniversePage page = page(at(4, TreeBubble.SEED.cost()));
            for (Node tab : page.subTabs().getChildren()) assertFalse(((Button) tab).getText().contains("Vide"));
            assertEquals(3, page.subTabs().getChildren().size());
            return null;
        });
    }

    @Test
    void laBulleDeLaBaseSAcheteDUnClic() {
        FxTestSupport.fx(() -> {
            double base = TreeBubble.SEED.cost();
            Game game = at(4, base * 2.5);
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(1, view.bubbleCount(), "au palier 4, une bulle");
            assertTrue(page.voidCumulLabel().getText().contains("bulles de l'arbre"), page.voidCumulLabel().getText());
            double[] at = view.bubblePoint(0);
            view.hover(at[0], at[1]);
            assertEquals(0, view.hoveredBubble(), "l'étiquette de la bulle s'affiche au survol");
            assertTrue(view.clickBubble(at[0], at[1]));
            assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
            assertEquals(base * 1.5, game.voidAvailable(), 1e-9);
            assertTrue(page.voidCumulLabel().getText().contains("dépenser pour les bulles"), page.voidCumulLabel().getText());
            assertFalse(view.clickBubble(5, 5), "à côté, rien");
            return null;
        });
    }

    @Test
    void lesBullesSuiventLeursLiensJusquALaCime() {
        FxTestSupport.fx(() -> {
            Game game = at(6, 1e12);
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(6, view.bubbleCount(), "la base, le feuillage, la cime");
            // La cime, avant la base : rien.
            double[] crown = view.bubblePoint(4);
            view.clickBubble(crown[0], crown[1]);
            assertFalse(game.isTreeBubbleBought(TreeBubble.CROWN_MIDDLE));
            double[] base = view.bubblePoint(0), left = view.bubblePoint(1);
            assertTrue(base[1] > left[1], "la base est sous le feuillage");
            assertTrue(crown[1] < left[1], "la cime au-dessus");
            view.clickBubble(base[0], base[1]);
            page.frame(0.016, true);
            left = view.bubblePoint(1);
            view.clickBubble(left[0], left[1]);
            page.frame(0.016, true);
            crown = view.bubblePoint(4);
            view.clickBubble(crown[0], crown[1]);
            assertTrue(game.isTreeBubbleBought(TreeBubble.LEAVES_LEFT));
            assertTrue(game.isTreeBubbleBought(TreeBubble.CROWN_MIDDLE), "reliée à la bulle de gauche, achetée");
            return null;
        });
    }
}
