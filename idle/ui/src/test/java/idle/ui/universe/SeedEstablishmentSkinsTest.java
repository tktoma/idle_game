package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Version 0.196 : la graine et son établi, sous Xvfb. Pour chacun des huit visuels (graine, arrosoir, étagère, tuyau,
 * chrono, rond de croissance, météo, saisons), chaque skin dessine la vue à deux tailles, à plusieurs paliers de la graine,
 * l'arrosoir penché ou non, le chrono qui tourne ou non, par beau temps et par orage, à chaque saison, sans exception ;
 * immobile, la vue habillée diffère du Classique en pixels, et le choix se lit par visuel.
 */
class SeedEstablishmentSkinsTest {

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

    /** Une vue où tout l'établi est là : la graine au palier {@code stage}, l'arrosoir, le tuyau qui verse, le chrono, le rond, la météo, les saisons. */
    private static SeedView view(double width, int stage, Game.Weather weather, Game.Season season, boolean running) {
        SeedView view = new SeedView();
        view.setWidth(width);
        view.setHeight(320);
        view.setSeeds(3);
        view.setSprout(true, 40);
        view.setStage(stage);
        view.setCanUnlocked(true);
        view.setHose(true, true);
        view.setChrono(true, 0.6, running);
        view.setBloom(true, 0.3, 0.35, 7, Game.BLOOM_VISIBLE);
        view.setBloomEffect(12, 0.45);
        view.setWeather(weather, 20, weather == Game.Weather.SUN ? 1.3 : weather == Game.Weather.STORM ? 0.9 : 1);
        view.setSeason(season, 70, 0.3);
        return view;
    }

    /** Dessine la vue immobile une fois : le temps ne bouge pas, deux vues dessinées ainsi sont comparables. */
    private static SeedView still(Skin skin) {
        SeedView view = view(600, 3, Game.Weather.STORM, Game.Season.AUTUMN, true);
        if (skin != null) view.setVisualSkin(skin.visual(), skin);
        for (int k = 0; k < 3; k++) view.frame(0.1, false);
        return view;
    }

    /** Fait vivre la vue habillée du skin à deux tailles et dans plusieurs états : paliers, temps, saisons, arrosoir pris et penché. */
    private static void live(Skin skin) {
        for (double width : new double[] {780, 380}) {
            for (int stage : new int[] {1, 3, 4, 7}) {
                for (Game.Weather weather : Game.Weather.values()) {
                    Game.Season season = Game.Season.values()[(stage + weather.ordinal()) % Game.Season.values().length];
                    SeedView view = view(width, stage, weather, season, stage % 2 == 0);
                    view.setVisualSkin(skin.visual(), skin);
                    assertEquals(skin, view.visualSkin(skin.visual()));
                    for (int k = 0; k < 4; k++) view.frame(0.1, true);
                    // L'arrosoir pris et porté au-dessus de la graine : il se penche et verse.
                    assertTrue(view.grab(view.canX(), view.canY()), "l'arrosoir se prend : " + skin);
                    for (int k = 0; k < 12; k++) {
                        view.drag(width / 2 - view.getWidth() * 0.05, view.getHeight() * 0.4);
                        view.frame(0.1, true);
                    }
                    assertTrue(view.tilt() > 0.6, "penché : " + skin + " " + view.tilt());
                    view.release();
                    for (int k = 0; k < 6; k++) view.frame(0.1, true);
                    // Le passage d'un palier, et la vue sans graine ni météo.
                    view.setStage(stage + 1);
                    for (int k = 0; k < 6; k++) view.frame(0.1, true);
                    view.setSeeds(0);
                    view.setWeather(null, 0, 1);
                    view.setSeason(null, 0, 0);
                    view.frame(0.1, true);
                }
            }
        }
    }

    private static void check(SkinVisual visual, int expected, int minimumDifference) {
        List<Skin> skins = skinsOf(visual);
        assertEquals(expected, skins.size(), visual.label());
        SeedView classic = still(null);
        assertNull(classic.visualSkin(visual));
        for (Skin skin : skins) {
            live(skin);
            SeedView skinned = still(skin);
            assertTrue(differing(classic, skinned) >= minimumDifference, "« " + skin.label() + " » se voit : " + differing(classic, skinned));
            skinned.setVisualSkin(visual, null);
            assertNull(skinned.visualSkin(visual), "Classique de nouveau");
        }
    }

    @Test
    void chaqueGraineSeDessineEtDiffereDuClassique() {
        FxTestSupport.fx(() -> {
            check(SkinVisual.SEED, 7, 30);
            return null;
        });
    }

    @Test
    void chaqueArrosoirEtChaqueEtagereSeDessinentEtDifferentDuClassique() {
        FxTestSupport.fx(() -> {
            check(SkinVisual.WATERING_CAN, 5, 10);
            check(SkinVisual.SHELF, 4, 20);
            return null;
        });
    }

    @Test
    void chaqueTuyauEtChaqueChronoSeDessinentEtDifferentDuClassique() {
        FxTestSupport.fx(() -> {
            check(SkinVisual.HOSE, 4, 10);
            check(SkinVisual.CHRONO, 4, 5);
            return null;
        });
    }

    @Test
    void leRondLaMeteoEtLesSaisonsSeDessinentEtDifferentDuClassique() {
        FxTestSupport.fx(() -> {
            check(SkinVisual.GROWTH_RING, 3, 10);
            check(SkinVisual.WEATHER, 3, 20);
            check(SkinVisual.SEASONS, 3, 5);
            // Tout l'établi habillé d'un coup, et le Classique partout : rien ne casse.
            SeedView all = view(700, 5, Game.Weather.SUN, Game.Season.WINTER, true);
            for (SkinVisual visual : SeedView.ESTABLISHMENT) all.setVisualSkin(visual, skinsOf(visual).get(0));
            for (int k = 0; k < 10; k++) all.frame(0.1, true);
            for (SkinVisual visual : SeedView.ESTABLISHMENT) all.setVisualSkin(visual, null);
            for (int k = 0; k < 10; k++) all.frame(0.1, true);
            assertEquals(8, SeedView.ESTABLISHMENT.length);
            return null;
        });
    }
}
