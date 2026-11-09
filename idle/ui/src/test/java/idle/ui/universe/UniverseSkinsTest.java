package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Version 0.196 : les univers en bulles et le lancement, sous Xvfb. Chaque skin des bulles, de la naissance et des liens
 * dessine la vue à deux tailles, avec un ou dix univers, pendant la naissance du premier et d'un nouvel univers, sans
 * exception et sans changer le nombre de bulles ni de liens ; immobile, une bulle ou un lien habillé diffère du Classique
 * en pixels. Chaque skin du lancement joue l'animation entière, lance l'univers à l'éclat et dessine à chaque image.
 */
class UniverseSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Le nombre de pixels qui diffèrent entre deux dessins, un sur trois dans chaque sens. */
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

    /** Une vue immobile de {@code universes} univers, habillée des skins donnés, dessinée une fois sans animation. */
    private static UniverseView still(double width, int universes, Skin bubble, Skin birth, Skin links) {
        UniverseView view = new UniverseView();
        view.setWidth(width);
        view.setHeight(300);
        view.setUniverses(universes);
        view.setSkins(bubble, birth, links);
        view.frame(0.1, false);
        return view;
    }

    /** Joue la naissance entière avec les skins donnés, à deux tailles, pour un premier univers puis un nouveau ; rend la vue. */
    private static UniverseView live(int universes, Skin bubble, Skin birth, Skin links) {
        UniverseView view = new UniverseView();
        view.setSkins(bubble, birth, links);
        for (double width : new double[] {780, 360}) {
            view.setWidth(width);
            view.setHeight(300);
            for (int born : new int[] {1, universes}) {
                view.setUniverses(born);
                view.playBirth();
                assertTrue(view.isBirthPlaying());
                for (double t = 0; t < UniverseView.BIRTH_SECONDS + 0.5; t += 0.1) view.frame(0.1, true);
                assertFalse(view.isBirthPlaying());
                assertEquals(Math.min(UniverseView.MAX_BUBBLES, born), view.bubbles());
                assertEquals(view.bubbles(), view.litBubbles(), "tout est allumé après la naissance");
            }
        }
        return view;
    }

    @Test
    void chaqueSkinDesBullesSeDessineEtDiffereDuClassique() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.UNIVERSE_BUBBLE);
            assertEquals(6, skins.size());
            UniverseView classic = still(600, 7, null, null, null);
            int links = classic.shownLinks();
            for (Skin skin : skins) {
                UniverseView view = live(10, skin, null, null);
                assertEquals(10, view.bubbles());
                UniverseView skinned = still(600, 7, skin, null, null);
                assertEquals(links, skinned.shownLinks(), "les liens ne changent pas avec les bulles : " + skin);
                assertTrue(differing(classic, skinned) > 50, "la bulle « " + skin.label() + " » se voit");
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDeLaNaissanceJoueLaNaissanceEntiere() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.UNIVERSE_BIRTH);
            assertEquals(4, skins.size());
            for (Skin skin : skins) {
                UniverseView view = live(6, null, skin, null);
                // Une image au milieu de la naissance : elle se dessine aussi, pas seulement à la fin.
                view.setUniverses(3);
                view.playBirth();
                for (int k = 0; k < 20; k++) view.frame(0.1, true);
                assertTrue(view.isBirthPlaying(), skin.label());
                assertTrue(view.birthProgress() > 0.2 && view.birthProgress() < 0.4, "à mi-chemin : " + view.birthProgress());
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDesLiensGardeLesLiensEtDiffereDuClassique() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.UNIVERSE_LINKS);
            assertEquals(4, skins.size());
            UniverseView classic = still(600, 10, null, null, null);
            assertTrue(classic.shownLinks() >= 9, "dix univers : au moins neuf liens");
            for (Skin skin : skins) {
                live(10, null, null, skin);
                UniverseView skinned = still(600, 10, null, null, skin);
                assertEquals(classic.shownLinks(), skinned.shownLinks(), skin.label());
                assertTrue(differing(classic, skinned) > 20, "le lien « " + skin.label() + " » se voit");
            }
            // Les trois visuels ensemble, et le Classique partout : rien ne casse.
            live(10, Skin.BUBBLE_EGG, Skin.BIRTH_PAGE, Skin.LINKS_CONSTELLATION);
            live(10, null, null, null);
            return null;
        });
    }

    @Test
    void chaqueSkinDuLancementLanceLUniversEtDessineChaqueImage() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.LAUNCH);
            assertEquals(5, skins.size());
            List<Skin> all = new ArrayList<>(skins);
            all.add(null);
            for (Skin skin : all) {
                for (double width : new double[] {780, 360}) {
                    LaunchView view = new LaunchView();
                    view.setWidth(width);
                    view.setHeight(300);
                    view.setSkin(skin);
                    int[] launched = {0};
                    view.play(() -> launched[0]++);
                    assertTrue(view.isPlaying());
                    double age = 0;
                    while (view.isPlaying()) {
                        view.frame(0.05);
                        age += 0.05;
                        assertTrue(age < LaunchView.SECONDS + 0.2, "l'animation finit : " + skin);
                        assertEquals(age >= LaunchView.LAUNCH_AT - 1e-9 ? 1 : 0, launched[0], "l'univers se lance à l'éclat : " + skin);
                    }
                    assertEquals(1, launched[0], "une fois : " + skin);
                }
            }
            // Immobile au milieu de l'animation, un lancement habillé diffère du Classique.
            LaunchView classic = new LaunchView(), skinned = new LaunchView();
            for (LaunchView view : new LaunchView[] {classic, skinned}) {
                view.setWidth(600);
                view.setHeight(300);
            }
            skinned.setSkin(Skin.LAUNCH_CURTAIN);
            classic.play(null);
            skinned.play(null);
            for (int k = 0; k < 6; k++) {
                classic.frame(0.1);
                skinned.frame(0.1);
            }
            assertTrue(differing(classic, skinned) > 100, "le rideau se voit");
            return null;
        });
    }
}
