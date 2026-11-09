package idle.ui.vacuum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.195 : l'écume, les paires et le trou noir du vide, sous Xvfb. Chaque combinaison d'une écume et de paires
 * dessine la vue à deux tailles, des paires à tous les âges, le trou noir fermé puis ouvert sous chacun de ses skins,
 * animée puis figée, sans exception ; chaque écume change la couleur d'un coin de la vue par rapport au Classique.
 */
class VoidSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Fait vivre la vue : à deux tailles, deux cents paires vivantes puis des naissances, sans trou noir puis avec, à toutes les masses. */
    private static void live(VoidView view) {
        for (double width : new double[] {780, 400}) {
            view.setWidth(width);
            view.setHeight(360);
            view.follow(1_000, 200, 6, -1);
            assertTrue(view.drawnPairs() > 0);
            for (int k = 0; k < 20; k++) {
                view.follow(1_000 + k * 3, 200, 6, -1);
                view.frame(0.05, true);
            }
            for (double mass : new double[] {0, 7.5, 15}) {
                view.follow(1_100, 200, 3, mass);
                for (int k = 0; k < 10; k++) view.frame(0.05, true);
                view.frame(0.05, false);
            }
            view.frame(0, false);
        }
    }

    @Test
    void chaqueEcumeAvecChaquePaireEtChaqueTrouNoirSeDessineSansException() {
        FxTestSupport.fx(() -> {
            List<Skin> foams = skinsOf(SkinVisual.VOID_FOAM);
            List<Skin> pairs = skinsOf(SkinVisual.VOID_PAIRS);
            List<Skin> holes = skinsOf(SkinVisual.VOID_HOLE);
            assertEquals(4, foams.size());
            assertEquals(3, pairs.size());
            assertEquals(5, holes.size());
            foams.add(null);
            pairs.add(null);
            holes.add(null);
            for (Skin foam : foams) {
                for (Skin pair : pairs) {
                    VoidView view = new VoidView();
                    view.skins(foam, pair, null);
                    live(view);
                }
            }
            for (Skin hole : holes) {
                VoidView view = new VoidView();
                view.skins(null, null, hole);
                live(view);
            }
            return null;
        });
    }

    @Test
    void chaqueEcumeChangeLaCouleurDUnCoin() {
        FxTestSupport.fx(() -> {
            Color classic = corner(null);
            for (Skin foam : skinsOf(SkinVisual.VOID_FOAM)) {
                Color color = corner(foam);
                assertTrue(!color.equals(classic), foam.name() + " : une écume différente du Classique (" + color + ")");
            }
            return null;
        });
    }

    /** La couleur du coin haut gauche de la vue, sans paire ni trou noir, après une image figée. */
    private static Color corner(Skin foam) {
        VoidView view = new VoidView();
        view.setWidth(780);
        view.setHeight(360);
        view.skins(foam, null, null);
        view.follow(0, 0, 6, -1);
        view.frame(0.05, false);
        assertEquals(0, view.drawnPairs());
        WritableImage image = new WritableImage(780, 360);
        view.snapshot(null, image);
        return image.getPixelReader().getColor(3, 3);
    }
}
