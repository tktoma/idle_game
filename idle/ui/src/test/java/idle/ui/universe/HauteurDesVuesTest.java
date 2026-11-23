package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.296 : la fenêtre des dimensions garde de la place à sa vue, même à la plus petite taille du jeu ; ce qui ne
 * tient pas dessous défile. Avant, en 820 × 600, la vue du mélange tombait à 18 pixels de haut. Et la bulle de l'arbre
 * qui rend les automatismes sans délai reste dans la graine et se survole, à toutes les tailles essayées.
 */
class HauteurDesVuesTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void chaqueVueGardeSaPlaceALaPlusPetiteFenetre() throws Exception {
        List<String> small = FxTestSupport.fx(() -> {
            Game game = new Game(42L);
            game.start();
            game.testFormUniverse();
            for (int k = 0; k < 10; k++) game.testAddUniverse();
            assertTrue(game.isMixOpen());
            List<String> out = new ArrayList<>();
            for (double[] size : new double[][] {{820, 600}, {1280, 800}}) {
                DimensionWindow window = new DimensionWindow(game);
                StackPane root = new StackPane(window);
                new Scene(root, size[0], size[1]);
                for (Dimension dimension : Dimension.values()) {
                    window.open(dimension);
                    window.frame(0.05, true);
                    root.applyCss();
                    root.layout();
                    double view = window.framed.getHeight();
                    if (view < DimensionWindowBase.MIN_VIEW - 1) out.add(size[0] + " " + dimension + " : " + view);
                    // À la taille par défaut, rien ne défile : le bas reçoit tout ce qu'il demande.
                    if (size[0] == 1280 && window.bottomScroll.getHeight() < window.bottom.prefHeight(size[0]) - 1) {
                        out.add("1280 " + dimension + " : le bas défile");
                    }
                }
            }
            return out;
        });
        assertEquals(List.of(), small);
    }

    @Test
    void laBulleSansDelaiResteVisible() throws Exception {
        List<String> out = FxTestSupport.fx(() -> {
            List<String> wrong = new ArrayList<>();
            Game.TreeBubble[] all = Game.TreeBubble.values();
            int right = Game.TreeBubble.LEAVES_RIGHT.ordinal();
            for (double[] size : new double[][] {{1100, 520}, {780, 300}, {500, 200}}) {
                SeedView view = new SeedView();
                view.setWidth(size[0]);
                view.setHeight(size[1]);
                view.setSeeds(3);
                view.setSprout(true, 40);
                for (int stage = 2; stage <= 11; stage++) view.setStage(stage);
                List<SeedView.Bubble> bubbles = new ArrayList<>();
                for (Game.TreeBubble bubble : all) {
                    List<Integer> below = new ArrayList<>();
                    for (Game.TreeBubble under : bubble.below()) below.add(under.ordinal());
                    bubbles.add(new SeedView.Bubble(bubble.ordinal(), bubble.label(), bubble.effect(), "Achetée",
                            SeedView.BubbleState.BOUGHT, below));
                }
                view.setBubbles(bubbles);
                for (int k = 0; k < 40; k++) view.frame(0.1, true);
                for (int i = 0; i < all.length; i++) {
                    double[] p = view.bubblePoint(i);
                    double r = view.bubbleR;
                    if (r <= 0) wrong.add(size[0] + " : bulles non dessinées");
                    if (p[0] < r || p[1] < r || p[0] > size[0] - r || p[1] > size[1] - r) wrong.add(size[0] + " " + all[i]);
                }
                double[] p = view.bubblePoint(right);
                view.hover(p[0], p[1]);
                if (view.hoveredBubble() != right) wrong.add(size[0] + " : survol " + view.hoveredBubble() + " en " + p[0] + "," + p[1] + " r" + view.bubbleR);
            }
            return wrong;
        });
        assertEquals(List.of(), out);
    }
}
