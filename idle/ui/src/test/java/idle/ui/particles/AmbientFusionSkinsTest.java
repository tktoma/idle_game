package idle.ui.particles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.192 : les skins du fond animé des particules et de la fusion des générateurs, sous Xvfb. Chaque fond se
 * dessine à deux tailles, à production nulle et forte, sans exception ; « Silence » ne dessine rien ; chaque skin de
 * fusion déplace les générateurs vers le centre et les y laisse à la taille voulue, par un autre chemin que le classique.
 */
class AmbientFusionSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    @Test
    void chaqueFondSeDessineAuxDeuxTaillesEtLeSilenceNeDessineRien() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.AMBIENT_DUST);
            assertEquals(8, skins.size());
            assertEquals(7, Skin.DUSTS.length);
            for (Skin skin : Skin.DUSTS) assertTrue(AmbientSkinArt.draws(skin), skin.name());
            assertFalse(AmbientSkinArt.draws(Skin.DUST_SILENCE));
            assertFalse(AmbientSkinArt.draws(null));
            GeneratorPane pane = new GeneratorPane();
            AmbientView ambient = pane.ambient();
            assertNull(ambient.skin());
            for (Skin skin : skins) {
                pane.setDustSkin(skin);
                assertEquals(skin, ambient.skin());
                for (double[] size : new double[][] {{400, 200}, {1400, 700}}) {
                    pane.resize(size[0], size[1]);
                    pane.layout();
                    assertEquals(size[0], ambient.getWidth());
                    double before = ambient.time();
                    pane.animateBackground(0.05, 0);
                    for (int k = 0; k < 10; k++) pane.animateBackground(0.05, 1e12);
                    assertEquals(before + 0.55, ambient.time(), 1e-9, skin.name());
                }
                pane.clearBackground();
            }
            // Un skin d'un autre visuel vaut le Classique.
            pane.setDustSkin(Skin.AMBIENT_PAPER);
            assertNull(ambient.skin());
            pane.animateBackground(0.05, 10);
            pane.setDustSkin(null);
            assertNull(ambient.skin());
            return null;
        });
    }

    @Test
    void chaqueSkinDeFusionMeneLesGenerateursAuCentre() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.GENERATOR_FUSION);
            assertEquals(3, skins.size());
            for (Skin skin : skins) {
                GeneratorPane pane = new GeneratorPane();
                pane.setFusionSkin(skin);
                assertEquals(skin, pane.fusionSkin());
                pane.resize(800, 400);
                pane.setCount(6);
                pane.layout();
                List<ParticleView> views = pane.views();
                assertEquals(6, views.size());
                ParallelTransition all = new ParallelTransition();
                GeneratorFusionArt.animate(skin, all, views, 400, 200, 1.4, 0.35);
                assertEquals(6, all.getChildren().size());
                all.play();
                all.jumpTo(Duration.seconds(0.7));
                // À mi-chemin, chaque vue a bougé, et ne suit pas la ligne droite du classique.
                boolean offLine = false;
                for (ParticleView view : views) {
                    double toX = 400 - (view.getLayoutX() + view.getWidth() / 2), toY = 200 - (view.getLayoutY() + view.getHeight() / 2);
                    assertTrue(Math.abs(view.getTranslateX()) + Math.abs(view.getTranslateY()) > 1e-6, skin + " : la vue a bougé");
                    double cross = view.getTranslateX() * toY - view.getTranslateY() * toX;
                    if (Math.abs(cross) > 1 || view.getRotate() != 0 || Math.abs(view.getScaleX() - view.getScaleY()) > 1e-6) offLine = true;
                }
                assertTrue(offLine, skin + " : un autre chemin que la ligne droite");
                all.jumpTo(Duration.seconds(1.4));
                for (ParticleView view : views) {
                    double toX = 400 - (view.getLayoutX() + view.getWidth() / 2), toY = 200 - (view.getLayoutY() + view.getHeight() / 2);
                    assertEquals(toX, view.getTranslateX(), 1e-6, skin + " : au centre");
                    assertEquals(toY, view.getTranslateY(), 1e-6, skin + " : au centre");
                    assertEquals(0.35, view.getScaleX(), 1e-6, skin + " : à la taille de fin");
                    assertEquals(0.35, view.getScaleY(), 1e-6, skin + " : à la taille de fin");
                    assertEquals(1, view.getOpacity(), 1e-9);
                }
                all.stop();
            }
            // Un skin d'un autre visuel vaut le Classique.
            GeneratorPane pane = new GeneratorPane();
            pane.setFusionSkin(Skin.CARD_INDEX);
            assertNull(pane.fusionSkin());
            return null;
        });
    }
}
