package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.197 : les cadres et la palette des mini-jeux, sous Xvfb. Chaque cadre se dessine à deux tailles et change le
 * calque, vide sans skin ; la fenêtre laisse sa marge au cadre choisi et la reprend sans lui. La palette, libre, change la
 * couleur de chaque dimension dans l'arène, dans les étiquettes et sur les boutons d'achat de la fenêtre, d'un coup ; sans
 * skin, la fenêtre garde ses couleurs d'origine.
 */
class DimensionFrameSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static int opaque(Canvas canvas) {
        WritableImage image = canvas.snapshot(new javafx.scene.SnapshotParameters() {
            {
                setFill(Color.TRANSPARENT);
            }
        }, null);
        int count = 0;
        for (int x = 0; x < (int) canvas.getWidth(); x += 3) {
            for (int y = 0; y < (int) canvas.getHeight(); y += 3) {
                if (image.getPixelReader().getColor(x, y).getOpacity() > 0.05) count++;
            }
        }
        return count;
    }

    /** Une partie au deuxième univers, le plan ouvert, des facettes pour acheter. */
    private static Game second() {
        Game game = new Game(42L);
        game.start();
        game.testFormUniverse();
        assertTrue(game.testAddUniverse());
        game.state().setPlaneShards(1_000_000);
        game.state().setSkinsOpen(true);
        return game;
    }

    private static DimensionWindow window(Game game, Dimension dimension) {
        DimensionWindow window = new DimensionWindow(game);
        new Scene(window, 1000, 700);
        window.open(dimension);
        window.applyCss();
        window.layout();
        window.frame(0.05, true);
        return window;
    }

    @Test
    void chaqueCadreSeDessineEtLeCalqueResteVideSansSkin() {
        FxTestSupport.fx(() -> {
            assertEquals(4, skinsOf(SkinVisual.DIMENSION_FRAME).size());
            for (double[] size : new double[][] {{400, 300}, {900, 600}}) {
                Canvas layer = new Canvas(size[0], size[1]);
                DimensionFrameArt.draw(layer, null, 1);
                assertEquals(0, opaque(layer), "sans skin, un calque vide");
                DimensionFrameArt.draw(layer, Skin.TREE_BIRCH, 1);
                assertEquals(0, opaque(layer), "un skin d'un autre visuel : vide");
                for (Skin skin : skinsOf(SkinVisual.DIMENSION_FRAME)) {
                    DimensionFrameArt.draw(layer, skin, 1.5);
                    int painted = opaque(layer);
                    int border = (int) ((size[0] / 3) * (size[1] / 3)) - (int) (((size[0] - 2 * DimensionFrameArt.THICKNESS) / 3) * ((size[1] - 2 * DimensionFrameArt.THICKNESS) / 3));
                    assertTrue(painted >= border * 0.6, skin.label() + " à " + size[0] + " : " + painted + " points peints pour une bordure de " + border);
                    DimensionFrameArt.draw(layer, skin, 2.5);
                }
                DimensionFrameArt.draw(layer, null, 3);
                assertEquals(0, opaque(layer), "le Classique efface le cadre");
            }
            return null;
        });
    }

    @Test
    void laFenetreLaisseSaMargeAuCadreChoisi() {
        FxTestSupport.fx(() -> {
            Game game = second();
            // Dix dimensions ouvertes : les quatre cadres gagnés.
            while (game.universes() < 10) assertTrue(game.testAddUniverse());
            game.tick(1);
            game.tick(1);
            DimensionWindow window = window(game, Dimension.PLANE);
            assertEquals(0, window.frameInset(), "sans cadre, pas de marge");
            for (Skin skin : skinsOf(SkinVisual.DIMENSION_FRAME)) {
                assertTrue(game.hasSkin(skin), skin.label());
                assertTrue(game.chooseSkin(SkinVisual.DIMENSION_FRAME, skin), skin.label());
                window.frame(0.1, true);
                assertEquals(DimensionFrameArt.THICKNESS, window.frameInset(), skin.label());
                window.layout();
                window.frame(0.1, true);
            }
            assertTrue(game.chooseSkin(SkinVisual.DIMENSION_FRAME, null));
            window.frame(0.1, true);
            assertEquals(0, window.frameInset(), "le Classique reprend la marge");
            return null;
        });
    }

    @Test
    void laPaletteChangeLesCouleursPartoutDUnCoup() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.DIMENSION_PALETTE).size());
            for (Skin skin : skinsOf(SkinVisual.DIMENSION_PALETTE)) assertTrue(skin.isFree(), skin.label() + " est libre");
            DimensionPalette.setSkin(null);
            assertEquals(Color.web("#9fd0ff"), DimensionPalette.color(1));
            assertEquals(UniversePage.COLOR, DimensionPalette.hex(1));
            assertEquals("#ffd27f", DimensionPalette.hex(2));
            assertEquals(DimensionPalette.hex(2), DimensionWindow.color(2));
            for (Skin skin : skinsOf(SkinVisual.DIMENSION_PALETTE)) {
                DimensionPalette.setSkin(skin);
                List<Color> colors = new ArrayList<>();
                for (int number = 1; number <= 10; number++) {
                    Color color = DimensionPalette.color(number);
                    assertEquals(DimensionPalette.hex(color), DimensionPalette.hex(number), skin.label() + " " + number);
                    assertFalse(colors.contains(color), skin.label() + " : dix couleurs distinctes (" + number + ")");
                    colors.add(color);
                }
                assertNotEquals(Color.web("#ffd27f"), DimensionPalette.color(2), skin.label() + " change le plan");
            }
            DimensionPalette.setSkin(null);
            // La fenêtre : les étiquettes et les boutons suivent, l'arène aussi, dès l'image suivante.
            Game game = second();
            DimensionWindow window = window(game, Dimension.PLANE);
            assertTrue(window.shardsStyle(2).contains("#ffd27f"), window.shardsStyle(2));
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).getStyle().contains("#ffd27f"), window.planeButton(Game.PlaneUpgrade.VALUE).getStyle());
            assertTrue(game.chooseSkin(SkinVisual.DIMENSION_PALETTE, Skin.DIM_PALETTE_SAFE));
            window.frame(0.1, true);
            assertEquals(Skin.DIM_PALETTE_SAFE, DimensionPalette.skin());
            assertTrue(window.shardsStyle(2).contains("#e69f00"), window.shardsStyle(2));
            assertTrue(window.shardsStyle(10).contains("#cccccc"), window.shardsStyle(10));
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).getStyle().contains("#e69f00"), window.planeButton(Game.PlaneUpgrade.VALUE).getStyle());
            assertEquals("#e69f00", DimensionWindow.color(2));
            for (Dimension dimension : Dimension.values()) {
                window.open(dimension);
                window.frame(0.1, true);
            }
            assertTrue(game.chooseSkin(SkinVisual.DIMENSION_PALETTE, null));
            window.open(Dimension.PLANE);
            window.frame(0.1, true);
            assertTrue(window.shardsStyle(2).contains("#ffd27f"), "le Classique rend ses couleurs");
            assertTrue(window.planeButton(Game.PlaneUpgrade.VALUE).getStyle().contains("#ffd27f"));
            return null;
        });
    }
}
