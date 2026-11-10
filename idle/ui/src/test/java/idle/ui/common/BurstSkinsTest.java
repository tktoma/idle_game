package idle.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.191 : les animations plein écran des six animations de la matière noire et des neuf skins du Big
 * Bang, sous Xvfb. Chacune joue du début à la fin, à deux tailles de fenêtre et avant la première mise en page,
 * sans exception, puis s'arrête d'elle-même.
 */
class BurstSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Joue l'animation entière à cette taille, par pas d'un vingtième de seconde. */
    private static void play(BurstView view, Skin skin, double width, double height) {
        view.play(skin, 2);
        assertTrue(view.isPlaying());
        assertEquals(skin, view.playing());
        int frames = 0;
        while (view.isPlaying() && frames++ < 100) view.frame(0.05, width, height);
        assertFalse(view.isPlaying(), skin.name() + " doit finir");
        assertFalse(view.isVisible());
    }

    @Test
    void chaqueAnimationDeLaMatiereNoireJoueJusquAuBout() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.DARK_ANIMATION);
            assertEquals(4, skins.size(), "version 0.203 : la pluie et le feu d'artifice retirés");
            BurstView view = new BurstView();
            for (Skin skin : skins) {
                play(view, skin, 1040, 640);
                play(view, skin, 360, 900);
                play(view, skin, 0, 0);     // avant la première mise en page : la taille d'attente
            }
            return null;
        });
    }

    @Test
    void chaqueBigBangJoueJusquAuBout() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.BIG_BANG);
            assertEquals(9, skins.size());
            BurstView view = new BurstView();
            for (Skin skin : skins) {
                play(view, skin, 1040, 640);
                play(view, skin, 360, 900);
            }
            return null;
        });
    }

    @Test
    void lEclairDeLExplosionAUneCouleurParAnimation() {
        List<String> colors = new ArrayList<>();
        colors.add(idle.ui.GameAppTabs.explosionFlashColor(null));
        for (Skin skin : skinsOf(SkinVisual.DARK_ANIMATION)) {
            String color = idle.ui.GameAppTabs.explosionFlashColor(skin);
            assertTrue(color.matches("#[0-9a-f]{6}"), color);
            assertFalse(colors.contains(color), skin.name() + " : une couleur à elle");
            colors.add(color);
        }
    }
}
