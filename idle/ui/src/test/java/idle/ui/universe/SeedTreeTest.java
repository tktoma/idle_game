package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.ui.FxTestSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * L'arbre de la graine dessiné palier par palier (versions 0.120 à 0.143), sous Xvfb : du bonsaï (palier 5) à
 * l'arbre-monde (palier 11), chaque palier dessine un arbre plus haut, au feuillage posé au-dessus du pied, qui
 * grandit pendant le passage ; l'intensité des effets monte de 0 au palier 4 à 1 à l'arbre-monde.
 */
class SeedTreeTest {

    private static final double W = 800, H = 600, BASE_X = 400, BASE_Y = 560;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static GraphicsContext canvas() {
        return new Canvas(W, H).getGraphicsContext2D();
    }

    @Test
    void chaquePalierDessineUnArbreAuFeuillagePoseAuDessusDuPied() {
        FxTestSupport.fx(() -> {
            GraphicsContext g = canvas();
            double previousCrown = BASE_Y;
            for (int stage = 5; stage <= 11; stage++) {
                double[] crown = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 0);
                assertEquals(3, crown.length);
                assertEquals(BASE_X, crown[0], 1e-9, "palier " + stage + " : le feuillage est centré sur le pied");
                assertTrue(crown[1] < BASE_Y, "palier " + stage + " : le feuillage est au-dessus du pied");
                assertTrue(crown[1] > 0, "palier " + stage + " : et dans la vue");
                assertTrue(crown[2] > 0, "palier " + stage + " : un rayon de feuillage");
                assertTrue(crown[1] < previousCrown, "palier " + stage + " : plus haut que le palier d'avant (" + crown[1] + " contre " + previousCrown + ")");
                assertTrue(SeedTree.lastTips > 0, "palier " + stage + " : des touffes de feuilles");
                previousCrown = crown[1];
            }
            // Les hauteurs : du bonsaï à 36 % de la vue à l'arbre-monde à 58 %, le feuillage à 72 % de la hauteur.
            assertEquals(BASE_Y - 0.36 * H * 0.72, SeedTree.draw(g, W, H, BASE_X, BASE_Y, 5, 1, 0)[1], 1e-9);
            assertEquals(BASE_Y - 0.58 * H * 0.72, SeedTree.draw(g, W, H, BASE_X, BASE_Y, 11, 1, 0)[1], 1e-9);
            return null;
        });
    }

    @Test
    void lArbreGranditPendantLePassage() {
        FxTestSupport.fx(() -> {
            GraphicsContext g = canvas();
            double[] young = SeedTree.draw(g, W, H, BASE_X, BASE_Y, 7, 0, 0);
            double[] half = SeedTree.draw(g, W, H, BASE_X, BASE_Y, 7, 0.5, 0);
            double[] grown = SeedTree.draw(g, W, H, BASE_X, BASE_Y, 7, 1, 0);
            assertTrue(young[1] > half[1] && half[1] > grown[1], "le feuillage monte à mesure que l'arbre pousse");
            assertTrue(young[2] < half[2] && half[2] < grown[2], "et s'élargit");
            // À zéro, l'arbre fait 35 % de sa taille.
            assertEquals(BASE_Y - 0.50 * H * 0.35 * 0.72, young[1], 1e-9);
            return null;
        });
    }

    @Test
    void lIntensiteDesEffetsMonteDuPalierQuatreALArbreMonde() {
        assertEquals(0, SeedTree.prominence(4), 1e-12, "au palier 4, aucun effet");
        assertEquals(0, SeedTree.prominence(1), 1e-12, "pas de valeur négative avant");
        assertEquals(1, SeedTree.prominence(11), 1e-12, "l'arbre-monde : tout");
        assertEquals(1, SeedTree.prominence(15), 1e-12, "jamais plus");
        assertEquals(3 / 7.0, SeedTree.prominence(7), 1e-12, "le jeune arbre : trois septièmes");
        for (int stage = 5; stage <= 11; stage++) assertTrue(SeedTree.prominence(stage) > SeedTree.prominence(stage - 1), "palier " + stage);
    }

    @Test
    void laVueDeLaGraineJoueLePassageDUnPalierAuSuivant() {
        FxTestSupport.fx(() -> {
            SeedView view = new SeedView();
            view.setStage(4);
            assertEquals(4, view.stage());
            assertFalse(view.isEvolving(), "posé d'un coup, sans passage");
            view.setStage(5);
            assertEquals(5, view.stage());
            assertTrue(view.isEvolving(), "un palier de plus : le passage se joue");
            for (int i = 0; i < 60; i++) view.frame(0.1, true);
            assertFalse(view.isEvolving(), "six secondes plus tard, le passage est fini");
            view.setStage(3);
            assertEquals(3, view.stage());
            assertFalse(view.isEvolving(), "un palier de moins (nouvelle partie) : pas de passage");
            view.setStage(20);
            assertEquals(11, view.stage(), "onze paliers au plus");
            return null;
        });
    }
}
