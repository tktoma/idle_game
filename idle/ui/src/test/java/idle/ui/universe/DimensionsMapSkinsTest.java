package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.197 : la carte des dimensions et le remplissage du défi final, sous Xvfb. Chaque skin de la carte se dessine
 * à deux tailles, avec deux, six et dix dimensions ouvertes, défi final ouvert ou non, et change le dessin par rapport au
 * Classique ; le clic trouve toujours les points. Chaque remplissage se dessine aux paliers partiels et atteints, et change
 * le dessin. La fin se joue sous un skin. Les points de la carte skinnée suivent la palette des dimensions, et la page de
 * l'univers passe les skins à la carte.
 */
class DimensionsMapSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static int differing(Canvas a, Canvas b) {
        WritableImage first = a.snapshot(null, null);
        WritableImage second = b.snapshot(null, null);
        int count = 0;
        for (int x = 0; x < (int) a.getWidth(); x += 3) {
            for (int y = 0; y < (int) a.getHeight(); y += 3) {
                if (first.getPixelReader().getArgb(x, y) != second.getPixelReader().getArgb(x, y)) count++;
            }
        }
        return count;
    }

    /** Une carte immobile de {@code unlocked} dimensions ouvertes, le défi final ouvert ou non, habillée des skins donnés. */
    private static DimensionsView map(double width, int unlocked, boolean finalOpen, Skin mapSkin, Skin fillSkin) {
        DimensionsView view = new DimensionsView();
        view.setWidth(width);
        view.setHeight(width * 0.55);
        view.setUnlocked(unlocked);
        for (int number = 1; number <= unlocked; number += 2) view.setMarked(number, true);
        view.setFinalOpen(finalOpen);
        if (finalOpen) {
            for (int number = 1; number <= unlocked; number++) {
                view.setFinalProgress(number, number * 0.1, number % 4 == 0, number % 4 == 0 ? "Palier atteint : 1e6" : "1e3 / 1e6");
            }
            view.setFinalTiers(unlocked / 4);
        }
        view.setSkins(mapSkin, fillSkin);
        for (int k = 0; k < 3; k++) view.frame(0.1, false);
        return view;
    }

    @Test
    void chaqueSkinDeLaCarteSeDessineEtChangeLeDessin() {
        FxTestSupport.fx(() -> {
            DimensionPalette.setSkin(null);
            assertEquals(5, skinsOf(SkinVisual.DIMENSIONS_MAP).size());
            for (double width : new double[] {500, 1000}) {
                for (int unlocked : new int[] {2, 6, 10}) {
                    for (boolean finalOpen : new boolean[] {false, true}) {
                        DimensionsView classic = map(width, unlocked, finalOpen, null, null);
                        assertEquals(0, differing(classic, map(width, unlocked, finalOpen, null, null)), "deux Classiques identiques");
                        for (Skin skin : skinsOf(SkinVisual.DIMENSIONS_MAP)) {
                            DimensionsView skinned = map(width, unlocked, finalOpen, skin, null);
                            assertTrue(differing(classic, skinned) > 40, skin.label() + " à " + width + ", " + unlocked + " dimensions");
                            for (int number = 1; number <= 10; number++) {
                                assertEquals(number, skinned.dimensionAt(skinned.x(number), skinned.y(number)), "le clic trouve le point " + number);
                            }
                            for (int k = 0; k < 4; k++) skinned.frame(0.1, true);
                        }
                    }
                }
            }
            return null;
        });
    }

    @Test
    void chaqueRemplissageDuDefiFinalSeDessineEtChangeLeDessin() {
        FxTestSupport.fx(() -> {
            assertEquals(4, skinsOf(SkinVisual.FINALE_FILL).size());
            for (double width : new double[] {500, 1000}) {
                DimensionsView classic = map(width, 10, true, null, null);
                assertEquals(1, classic.filled(4), "le palier atteint : plein");
                assertEquals(0.3, classic.filled(3), 1e-9);
                for (Skin skin : skinsOf(SkinVisual.FINALE_FILL)) {
                    DimensionsView skinned = map(width, 10, true, null, skin);
                    assertTrue(differing(classic, skinned) > 10, skin.label() + " à " + width);
                    assertEquals(1, skinned.filled(4), "le remplissage ne change pas la part remplie");
                    assertEquals(0, differing(map(width, 10, false, null, skin), map(width, 10, false, null, null)), skin.label() + " sans défi : rien");
                    for (int k = 0; k < 4; k++) skinned.frame(0.1, true);
                }
                // Carte et remplissage ensemble, et la fin qui se joue sous les deux.
                DimensionsView both = map(width, 10, true, Skin.MAP_MENHIRS, Skin.FINALE_IVY);
                assertTrue(differing(map(width, 10, true, Skin.MAP_MENHIRS, null), both) > 10, "le lierre sur les menhirs");
                both.playFinale();
                for (int k = 0; k < 80; k++) both.frame(0.1, true);
                assertFalse(both.isFinalePlaying(), "la fin s'est jouée jusqu'au bout");
            }
            return null;
        });
    }

    @Test
    void lesPointsDeLaCarteSkinneeSuiventLaPalette() {
        FxTestSupport.fx(() -> {
            DimensionsView classic = map(700, 10, false, Skin.MAP_PLANETS, null);
            DimensionPalette.setSkin(Skin.DIM_PALETTE_SEPIA);
            DimensionsView sepia = map(700, 10, false, Skin.MAP_PLANETS, null);
            DimensionPalette.setSkin(null);
            assertTrue(differing(classic, sepia) > 20, "les planètes prennent les couleurs de la palette");
            // La page de l'univers passe les skins choisis à la carte, et la palette.
            Game game = new Game(7L);
            game.start();
            game.testFormUniverse();
            for (int k = 0; k < 5; k++) assertTrue(game.testAddUniverse());
            game.state().setSkinsOpen(true);
            game.tick(1);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.MAP_MENHIRS) && !game.hasSkin(Skin.MAP_TOWER), "six dimensions : les menhirs, pas la tour");
            assertTrue(game.chooseSkin(SkinVisual.DIMENSIONS_MAP, Skin.MAP_MENHIRS));
            assertTrue(game.chooseSkin(SkinVisual.DIMENSION_PALETTE, Skin.DIM_PALETTE_RAINBOW));
            UniversePage page = new UniversePage(game);
            new javafx.scene.Scene(page, 1100, 700);
            page.select(UniversePage.DIMENSIONS);
            page.refresh();
            page.applyCss();
            page.layout();
            for (int k = 0; k < 5; k++) page.frame(0.05, true);
            assertEquals(Skin.DIM_PALETTE_RAINBOW, DimensionPalette.skin());
            assertEquals(6, page.dimensionsView().unlocked());
            DimensionPalette.setSkin(null);
            return null;
        });
    }
}
