package idle.ui.darkmatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.191 : les huit boules et les six animations de la matière noire, sous Xvfb. Chaque skin se laisse
 * choisir et dessine, image après image, la boule au repos puis pressée, du proton à l'univers observable (la
 * croissance qui allonge les fissures de l'œuf et la flamme de la lanterne), à deux tailles de vue, sans
 * exception ; un skin d'un autre visuel ramène au classique.
 */
class DarkMatterSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Les skins d'un visuel, dans l'ordre du catalogue. */
    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Du proton à l'univers observable : petite, moyenne, prête à tout avaler. */
    private static final BigNum[] SIZES = {BigNum.of(1, -15), BigNum.of(1.27, 7), BigNum.of(8.8, 26)};

    /** Fait vivre la vue : à deux tailles, pour chaque grandeur, au repos, puis pressée avec de l'élan, puis relâchée. */
    private static void live(DarkMatterView view) {
        for (double size : new double[] {420, 160}) {
            view.setWidth(size);
            view.setHeight(size * 0.7);
            for (BigNum matter : SIZES) {
                for (int k = 0; k < 8; k++) view.frame(0.05, matter, false, 0);
                // L'appui : la secousse du mercure, le nœud qui se resserre, la chute de matière.
                for (int k = 0; k < 20; k++) view.frame(0.05, matter, true, 50);
                for (int k = 0; k < 8; k++) view.frame(0.05, matter, false, 0);
            }
        }
        // Une image figée, et une vue trop petite pour dessiner.
        view.frame(0, SIZES[1], true, 1);
        view.setWidth(20);
        view.setHeight(20);
        view.frame(0.05, SIZES[1], false, 0);
    }

    @Test
    void chaqueBouleSeDessineSansException() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.DARK_BALL);
            assertEquals(7, skins.size(), "version 0.203 : l'œuf retiré");
            for (Skin skin : skins) {
                DarkMatterView view = new DarkMatterView(400, 300);
                view.setSkin(skin);
                assertEquals(skin, view.skin());
                live(view);
            }
            return null;
        });
    }

    @Test
    void chaqueAnimationSeDessineSansExceptionAvecChaqueBoule() {
        FxTestSupport.fx(() -> {
            List<Skin> animations = skinsOf(SkinVisual.DARK_ANIMATION);
            assertEquals(4, animations.size(), "version 0.203 : la pluie et le feu d'artifice retirés");
            for (Skin animation : animations) {
                DarkMatterView view = new DarkMatterView(400, 300);
                view.setAnimation(animation);
                assertEquals(animation, view.animation());
                live(view);
                // Avec une boule habillée aussi.
                view.setSkin(Skin.DARK_LANTERN);
                live(view);
            }
            return null;
        });
    }

    @Test
    void laCroissanceEtLAppuiSontMesures() {
        FxTestSupport.fx(() -> {
            DarkMatterView view = new DarkMatterView(400, 300);
            view.setSkin(Skin.DARK_LANTERN);
            view.frame(0.05, SIZES[0], false, 0);
            assertEquals(0, view.growth(), 1e-9, "un proton : la flamme est au plus bas");
            view.frame(0.05, SIZES[2], false, 0);
            assertEquals(1, view.growth(), 1e-9, "l'univers observable : la flamme est à son plein");
            view.frame(0.05, SIZES[1], true, 10);
            assertTrue(view.press() > 0.8, "la secousse part à l'appui");
            for (int k = 0; k < 30; k++) view.frame(0.05, SIZES[1], true, 10);
            assertEquals(0, view.press(), 1e-9, "et s'éteint tant qu'on tient");
            assertTrue(view.hold() > 0.9, "l'appui tenu est à son plein");
            return null;
        });
    }

    @Test
    void unSkinDUnAutreVisuelOuAucunDonneLeClassique() {
        FxTestSupport.fx(() -> {
            DarkMatterView view = new DarkMatterView(400, 300);
            view.setSkin(Skin.DARK_SNOW);
            view.setAnimation(Skin.DARK_HOLE);
            assertNull(view.skin());
            assertNull(view.animation());
            live(view);
            view.setSkin(null);
            view.setAnimation(null);
            live(view);
            return null;
        });
    }
}
