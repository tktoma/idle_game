package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.ui.FxTestSupport;
import java.util.List;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.148 : les bulles disent ce qu'elles font, et celles qui se rachètent leur niveau, sous Xvfb.
 */
class BullesEffetsPageTest {

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
    void chaqueBulleDitCeQuElleFaitEtSonNiveau() {
        FxTestSupport.fx(() -> {
            Game game = at(5, 1e6);
            assertTrue(game.buyTreeBubble(TreeBubble.SEED));
            assertTrue(game.buyTreeBubble(TreeBubble.LEAVES_LEFT));
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(3, view.bubbleCount());
            for (int i = 0; i < view.bubbleCount(); i++) {
                assertEquals(game.treeBubblesShown().get(i).effect(), view.bubble(i).effect(), "l'effet de la bulle " + i);
            }
            int left = game.treeBubblesShown().indexOf(TreeBubble.LEAVES_LEFT);
            assertEquals(SeedView.BubbleState.BUY, view.bubble(left).state(), "au niveau 1 sur 5, elle s'achète encore");
            assertTrue(view.bubble(left).price().startsWith("Niveau 1 / 5. Prix : 4"), view.bubble(left).price());
            assertEquals("Achetée", view.bubble(game.treeBubblesShown().indexOf(TreeBubble.SEED)).price());
            double[] at = view.bubblePoint(left);
            for (int k = 0; k < 4; k++) view.clickBubble(at[0], at[1]);
            assertEquals(5, game.treeBubbleLevel(TreeBubble.LEAVES_LEFT));
            assertEquals(SeedView.BubbleState.BOUGHT, view.bubble(left).state());
            assertTrue(view.bubble(left).price().contains("au plus haut"), view.bubble(left).price());
            return null;
        });
    }

    @Test
    void lesEffetsLongsTiennentSurDeuxLignes() {
        for (TreeBubble bubble : TreeBubble.values()) {
            List<String> lines = SeedView.effectLines(bubble.effect());
            assertTrue(lines.size() >= 1 && lines.size() <= 2, bubble.toString());
            assertEquals(bubble.effect(), String.join(" ", lines), "rien de perdu à la coupe");
            for (String line : lines) assertTrue(line.length() <= 60, bubble + " : " + line);
        }
        assertEquals(List.of(), SeedView.effectLines(""));
    }
}
