package idle.ui.darkmatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.203 : les skins de l'arbre de matière noire habillent aussi le fond de l'arbre et ses en-têtes (avant : les
 * traits et une vignette de 18 pixels). Chaque skin a son fond (une couleur et un motif qui se répète sans couture, fait
 * une seule fois) et son style d'en-tête ; le Classique n'a ni l'un ni l'autre.
 */
class ArbreHabilleTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @Test
    void chaqueSkinASonFondEtSesEnTetes() {
        FxTestSupport.fx(() -> {
            assertNull(DarkTreeSkinArt.backdrop(null), "le Classique n'a pas de fond");
            assertEquals("", DarkTreeSkinArt.header(null, "#ffffff"));
            assertNull(DarkTreeSkinArt.backdrop(Skin.DARK_HOLE), "un skin d'un autre visuel non plus");
            Set<String> headers = new HashSet<>();
            Set<Color> bases = new HashSet<>();
            for (Skin skin : Skin.values()) {
                if (skin.visual() != SkinVisual.DARK_TREE) continue;
                assertNotNull(DarkTreeSkinArt.backdrop(skin), skin.label());
                assertSame(DarkTreeSkinArt.backdrop(skin), DarkTreeSkinArt.backdrop(skin), "le fond se fait une fois");
                assertEquals(1, DarkTreeSkinArt.backdrop(skin).getImages().size(), "un motif répété");
                String header = DarkTreeSkinArt.header(skin, "#8fd3ff");
                assertTrue(header.contains("-fx-background-color") && header.contains("-fx-border-color"), skin.label());
                headers.add(header);
                // Le motif : quelque chose est dessiné, et rien sur le bord, pour qu'il se répète sans couture.
                Canvas canvas = new Canvas(DarkTreeSkinArt.TILE, DarkTreeSkinArt.TILE);
                Color base = DarkTreeSkinArt.tile(skin, canvas.getGraphicsContext2D());
                assertTrue(base.getBrightness() < 0.2, skin.label() + " : un fond sombre, comme le reste de la page");
                bases.add(base);
                javafx.scene.SnapshotParameters parameters = new javafx.scene.SnapshotParameters();
                parameters.setFill(Color.TRANSPARENT);
                WritableImage image = canvas.snapshot(parameters, null);
                int drawn = 0, last = DarkTreeSkinArt.TILE - 1;
                for (int x = 0; x <= last; x++) {
                    for (int y = 0; y <= last; y++) {
                        boolean inked = (image.getPixelReader().getArgb(x, y) >>> 24) != 0;
                        if (inked) drawn++;
                        if (x == 0 || y == 0 || x == last || y == last) assertTrue(!inked, skin.label() + " : rien sur le bord du motif (" + x + ", " + y + ")");
                    }
                }
                assertTrue(drawn > 100, skin.label() + " : le motif se voit (" + drawn + " pixels)");
            }
            assertEquals(4, headers.size(), "quatre en-têtes différents");
            assertEquals(4, bases.size(), "quatre fonds différents");
            assertNotEquals(18.0, DarkTreeSkinArt.ART, "les vignettes ont grandi");
            return null;
        });
    }
}
