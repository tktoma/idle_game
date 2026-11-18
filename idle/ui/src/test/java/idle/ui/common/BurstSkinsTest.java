package idle.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import idle.ui.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.*;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * Les animations plein écran des six animations de la matière noire et des neuf skins du Big
 * Bang, sous Xvfb. Chacune joue du début à la fin, à deux tailles de fenêtre et avant la première mise en page,
 * sans exception, puis s'arrête d'elle-même.
 */
class BurstSkinsTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

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
            assertEquals(2, skins.size(), "la pluie et le feu d'artifice retirés en 0.203, la neige et le sablier en 0.248");
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

    /** Version 0.246 : le Big Bang sans skin a son animation, qui joue jusqu'au bout et cède la place à un skin. */
    @Test
    void leBigBangClassiqueJoueJusquAuBout() {
        FxTestSupport.fx(() -> {
            BurstView view = new BurstView();
            for (double[] size : new double[][] {{1040, 640}, {360, 900}, {0, 0}}) {
                view.playBigBang(3.5);
                assertTrue(view.isPlaying() && view.playingBigBang() && view.isVisible());
                assertEquals(null, view.playing(), "pas de skin");
                int frames = 0;
                while (view.isPlaying() && frames++ < 200) view.frame(0.05, size[0], size[1]);
                assertFalse(view.isPlaying() || view.playingBigBang() || view.isVisible(), "il finit de lui-même");
            }
            view.playBigBang(3.5);
            view.play(Skin.BANG_AURORA, 2);
            assertFalse(view.playingBigBang(), "un skin qui joue ensuite prend la place du classique");
            return null;
        });
    }

    /** La boule de feu du Big Bang classique refroidit : blanche au départ, sombre à la fin. */
    @Test
    void laBouleDeFeuRefroidit() {
        FxTestSupport.fx(() -> {
            assertTrue(BurstView.fire(0).getBrightness() > 0.95);
            assertTrue(BurstView.fire(1).getBrightness() < 0.4);
            assertTrue(BurstView.fire(-1).equals(BurstView.fire(0)) && BurstView.fire(2).equals(BurstView.fire(1)), "bornée");
            return null;
        });
    }

    @Test
    void lEclairDeLExplosionAUneCouleurParAnimation() {
        List<String> colors = new ArrayList<>();
        colors.add(GameAppTabs.explosionFlashColor(null));
        for (Skin skin : skinsOf(SkinVisual.DARK_ANIMATION)) {
            String color = GameAppTabs.explosionFlashColor(skin);
            assertTrue(color.matches("#[0-9a-f]{6}"), color);
            assertFalse(colors.contains(color), skin.name() + " : une couleur à elle");
            colors.add(color);
        }
    }
}
