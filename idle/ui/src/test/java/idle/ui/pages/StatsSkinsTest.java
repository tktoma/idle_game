package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.history.Pace;
import idle.core.history.PurchaseHistory;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.198 : les skins des statistiques, sous Xvfb. Les courbes ({@link SkinVisual#CHART}) et la palette des
 * séries ({@link SkinVisual#CHART_PALETTE}) : chaque combinaison dessine un graphique à une et deux séries, avec des
 * points manquants et des marqueurs, à deux largeurs, autrement que le Classique ; la palette remplace les huit couleurs
 * et laisse les autres ; la frise ({@link SkinVisual#TIMELINE}) : chaque skin redessine les lignes des univers, autant
 * qu'il y en a, avec les mêmes textes ; la page des statistiques pousse les skins choisis.
 */
class StatsSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @AfterEach
    void classic() {
        ChartPane.setSkins(null, null);
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        skins.add(null);
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static boolean same(WritableImage a, WritableImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < (int) a.getHeight(); y++) {
            for (int x = 0; x < (int) a.getWidth(); x++) if (a.getPixelReader().getArgb(x, y) != b.getPixelReader().getArgb(x, y)) return false;
        }
        return true;
    }

    private static ChartPane chart(double width) {
        ChartPane pane = new ChartPane("Essai", "échelle", true, Format::clock, value -> Format.whole(Math.round(value)), false, Double.NaN, Double.NaN)
                .withMarkers();
        pane.resize(width, 210);
        pane.layoutChildren();
        return pane;
    }

    private static List<ChartPane.Series> series(int count) {
        double[] x = new double[20], a = new double[20], b = new double[20];
        for (int i = 0; i < 20; i++) {
            x[i] = i * 60;
            a[i] = i == 7 ? Double.NaN : 10 + i * i;
            b[i] = 400 - 10 * i;
        }
        List<ChartPane.Series> series = new ArrayList<>();
        series.add(new ChartPane.Series("Une", ChartPane.BLUE, x, a));
        if (count > 1) series.add(new ChartPane.Series("Deux", ChartPane.ORANGE, x, b));
        return series;
    }

    @Test
    void chaqueCourbeEtChaquePaletteDessinentAutrementQueLeClassique() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.CHART).size());
            assertEquals(4, skinsOf(SkinVisual.CHART_PALETTE).size());
            for (double width : new double[] {640, 320}) {
                for (int count = 1; count <= 2; count++) {
                    ChartPane.setSkins(null, null);
                    ChartPane plain = chart(width);
                    plain.show(series(count));
                    WritableImage classic = plain.snapshot(null, null);
                    for (Skin skin : skinsOf(SkinVisual.CHART)) {
                        for (Skin palette : skinsOf(SkinVisual.CHART_PALETTE)) {
                            ChartPane.setSkins(skin, palette);
                            assertEquals(skin, ChartPane.skin());
                            assertEquals(palette, ChartPane.palette());
                            ChartPane pane = chart(width);
                            pane.show(series(count));
                            int draws = pane.draws();
                            pane.show(series(count));
                            assertEquals(draws, pane.draws(), "rien n'a changé : pas de nouveau dessin");
                            boolean classicToo = skin == null && palette == null;
                            assertEquals(classicToo, same(classic, pane.snapshot(null, null)), skin + " + " + palette + " à " + width + ", " + count + " série(s)");
                            // Un skin qui change redessine le graphique déjà à l'écran.
                            ChartPane.setSkins(skin == null ? Skin.CHART_PENCIL : null, palette);
                            pane.show(series(count));
                            assertEquals(draws + 1, pane.draws(), "le skin a changé : un dessin de plus");
                        }
                    }
                }
            }
            ChartPane.setSkins(Skin.FUSION_FLASH, Skin.FUSION_FLASH);
            assertNull(ChartPane.skin(), "un skin d'un autre visuel : le Classique");
            assertNull(ChartPane.palette());
            return null;
        });
    }

    @Test
    void laPaletteRemplaceLesHuitCouleursEtLaisseLesAutres() {
        List<String> classic = List.of(ChartPane.BLUE, ChartPane.ORANGE, ChartPane.YELLOW, ChartPane.VIOLET, ChartPane.GREEN, ChartPane.TEAL,
                ChartPane.MAGENTA, ChartPane.AQUA);
        for (Skin palette : skinsOf(SkinVisual.CHART_PALETTE)) {
            List<String> mapped = new ArrayList<>();
            for (String color : classic) {
                String to = ChartSkinStyles.seriesColor(palette, color);
                assertEquals(palette == null, to.equals(color), palette + " : " + color);
                assertFalse(mapped.contains(to), palette + " garde huit couleurs distinctes : " + to);
                mapped.add(to);
            }
            assertEquals("#123456", ChartSkinStyles.seriesColor(palette, "#123456"), "une autre couleur reste telle quelle");
            if (palette != null) assertTrue(palette.isFree(), palette + " est libre");
        }
        assertEquals(ChartPane.BLUE, ChartSkinStyles.seriesColor(Skin.FUSION_FLASH, ChartPane.BLUE));
        assertNotEquals(ChartSkinStyles.strokeColor(Skin.CHART_OSCILLOSCOPE, ChartPane.BLUE, 0), ChartSkinStyles.strokeColor(Skin.CHART_OSCILLOSCOPE, ChartPane.BLUE, 1),
                "deux séries de l'oscilloscope ont deux verts");
    }

    @Test
    void laFriseRedessineLesUniversDansChaqueSkinAvecLesMemesTextes() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.state().setTimePlayed(7_200);
            game.testFormUniverse();
            game.state().setTimePlayed(10_800);
            game.testFormUniverse();
            game.state().setTimePlayed(12_000);
            game.testFormUniverse();
            game.tick(2);
            StatsPage page = new StatsPage(game, new PurchaseHistory(), new Pace());
            page.refresh();
            assertTrue(page.openSubPage("Chronologie"));
            assertNull(page.timelineSkin());
            List<String> classic = page.timelineLines();
            int nodes = page.timelineUniverseNodes();
            assertEquals(3, nodes);
            game.testAllSkins(true);
            for (Skin skin : skinsOf(SkinVisual.TIMELINE)) {
                assertTrue(game.chooseSkin(SkinVisual.TIMELINE, skin));
                page.refresh();
                assertEquals(skin, page.timelineSkin());
                assertEquals(nodes, page.timelineUniverseNodes(), skin + " : une ligne par univers");
                assertEquals(classic, page.timelineLines(), skin + " : les mêmes textes");
            }
            assertTrue(game.chooseSkin(SkinVisual.CHART, Skin.CHART_WATERCOLOR));
            assertTrue(game.chooseSkin(SkinVisual.CHART_PALETTE, Skin.CHART_NEON));
            page.refresh();
            assertEquals(Skin.CHART_WATERCOLOR, ChartPane.skin(), "la page pousse le skin des courbes");
            assertEquals(Skin.CHART_NEON, ChartPane.palette());
            return null;
        });
    }
}
