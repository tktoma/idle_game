package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.ui.FxTestSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Les essences de l'arbre (version 0.185), sous Xvfb : chacun des huit skins de l'arbre se dessine à chaque palier,
 * du bonsaï (5) à l'arbre-monde (11), sans exception ; le centre du feuillage rendu reste dans la vue, au-dessus du pied
 * et près de son axe (les bulles de l'arbre s'y posent, même pour le bouquet du palmier et la couronne du baobab) ; et
 * le nombre de touffes (ou de palmes) reste borné, pour le coût du dessin. Sans skin, l'essence est le Classique.
 */
class SeedTreeEssencesTest {

    private static final double W = 800, H = 600, BASE_X = 400, BASE_Y = 560;
    /** L'arbre-monde Classique en dessine environ 150 : aucune essence ne doit en coûter plus du double. */
    private static final int MAX_TIPS = 400;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static GraphicsContext canvas() {
        return new Canvas(W, H).getGraphicsContext2D();
    }

    @Test
    void chaqueEssenceSeDessineAChaquePalierLeFeuillageDansLaVue() {
        FxTestSupport.fx(() -> {
            GraphicsContext g = canvas();
            for (Skin skin : Skin.TREES) {
                for (int stage = 5; stage <= 11; stage++) {
                    for (double growth : new double[] {0, 0.5, 1}) {
                        double[] crown = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, growth, 1.7, skin);
                        String what = skin.label() + ", palier " + stage + ", croissance " + growth;
                        assertEquals(3, crown.length, what);
                        assertTrue(crown[0] > 0 && crown[0] < W, what + " : le centre du feuillage dans la largeur de la vue (" + crown[0] + ")");
                        assertTrue(crown[1] > 0 && crown[1] < BASE_Y, what + " : au-dessus du pied et dans la vue (" + crown[1] + ")");
                        assertEquals(BASE_X, crown[0], crown[2] * 0.6, what + " : près de l'axe du pied");
                        assertTrue(crown[2] > 0, what + " : un rayon de feuillage");
                        assertTrue(SeedTree.lastTips > 0 && SeedTree.lastTips <= MAX_TIPS,
                                what + " : des touffes, pas plus de " + MAX_TIPS + " (" + SeedTree.lastTips + ")");
                    }
                }
            }
            return null;
        });
    }

    @Test
    void lesEssencesPortentLeFeuillageALaHauteurDeLArbre() {
        FxTestSupport.fx(() -> {
            GraphicsContext g = canvas();
            for (Skin skin : Skin.TREES) {
                for (int stage = 7; stage <= 11; stage++) {
                    double height = H * new double[] {0.36, 0.40, 0.50, 0.51, 0.54, 0.56, 0.58}[stage - 5];
                    double crownY = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 0, skin)[1];
                    assertTrue(crownY <= BASE_Y - height * 0.45 && crownY >= BASE_Y - height * 1.4,
                            skin.label() + ", palier " + stage + " : feuillage à " + (BASE_Y - crownY) + " au-dessus du pied, pour un arbre de " + height);
                }
            }
            return null;
        });
    }

    @Test
    void sansSkinLEssenceEstLeClassiqueEtChaqueSkinALaSienne() {
        assertSame(SeedTree.Essence.CLASSIC, SeedTree.Essence.of(null));
        assertSame(SeedTree.Essence.CLASSIC, SeedTree.Essence.of(Skin.ATOM_DENSE), "un skin d'un autre visuel : le Classique");
        for (Skin skin : Skin.TREES) assertNotEquals(SeedTree.Essence.CLASSIC, SeedTree.Essence.of(skin), skin.label());
        assertEquals(Skin.TREES.length, java.util.Arrays.stream(Skin.TREES).map(SeedTree.Essence::of).distinct().count(), "une essence par skin");
        FxTestSupport.fx(() -> {
            // Le chemin par défaut : dessiner sans skin ou avec null rend la même chose.
            GraphicsContext g = canvas();
            for (int stage = 5; stage <= 11; stage++) {
                double[] classic = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 2.5);
                int tips = SeedTree.lastTips;
                double[] withNull = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 2.5, null);
                assertEquals(classic[0], withNull[0], 0, "palier " + stage);
                assertEquals(classic[1], withNull[1], 0, "palier " + stage);
                assertEquals(classic[2], withNull[2], 0, "palier " + stage);
                assertEquals(tips, SeedTree.lastTips, "palier " + stage);
            }
            return null;
        });
    }
}
