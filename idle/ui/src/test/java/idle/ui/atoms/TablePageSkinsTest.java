package idle.ui.atoms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementSet;
import idle.core.elements.PeriodicTable;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.192 : les skins de la page du tableau périodique hors cases, sous Xvfb. « Médailles » pose une médaille sur
 * chaque tuile, de bronze, d'argent ou d'or selon l'avancement ; « Creuset » pose le creuset sur la carte de synthèse et
 * le fait fumer ; « Spectre » donne à chaque ligne de la légende une bande de raies de sa couleur ; sans skin, tout
 * revient au classique, la légende au caractère près, et les cases ne changent pas.
 */
class TablePageSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Un jeu avec tous les skins, un ensemble complet (les gaz nobles), un entamé (un halogène) et le tableau ouvert. */
    private static Game game() {
        Game game = new Game();
        game.start();
        game.testAllSkins(true);
        for (var noble : PeriodicTable.elements(ElementCategory.NOBLE_GAS)) game.state().setElementCount(noble.number(), 1);
        game.state().setElementCount(9, 1);
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        return game;
    }

    private static PeriodicTablePage page(Game game, double width) {
        PeriodicTablePage page = new PeriodicTablePage(game);
        new Scene(page, width, 760);
        page.refresh();
        page.applyCss();
        page.layout();
        return page;
    }

    @Test
    void chaqueSkinDeLaPageChangeSaPartEtLeClassiqueRevient() {
        FxTestSupport.fx(() -> {
            Game game = game();
            for (double width : new double[] {700, 1200}) {
                PeriodicTablePage page = page(game, width);
                String classicLegend = page.legendStyle(ElementCategory.HALOGEN);
                String classicCell = page.cellStyle(9);
                assertEquals("-fx-font-size: 12px; -fx-text-fill: #8fd0ff;", classicLegend);
                assertFalse(page.medalled());
                assertNull(page.crucible());
                List<Skin> skins = skinsOf(SkinVisual.TABLE_PAGE);
                assertEquals(3, skins.size());
                for (Skin skin : skins) {
                    assertTrue(game.chooseSkin(SkinVisual.TABLE_PAGE, skin), skin.name());
                    page.refresh();
                    page.applyCss();
                    page.layout();
                    page.frame(0.3);
                    page.frame(0.3);
                    assertEquals(skin == Skin.PAGE_MEDALS, page.medalled(), skin.name());
                    assertEquals(skin == Skin.PAGE_CRUCIBLE, page.crucible() != null, skin.name());
                    assertEquals(skin == Skin.PAGE_SPECTRUM, !classicLegend.equals(page.legendStyle(ElementCategory.HALOGEN)), skin.name());
                    assertEquals(classicCell, page.cellStyle(9), skin + " : les cases ne changent pas");
                    if (skin == Skin.PAGE_SPECTRUM) {
                        assertTrue(page.legendStyle(ElementCategory.HALOGEN).contains("linear-gradient"), "les raies");
                        assertTrue(page.legendStyle(ElementCategory.HALOGEN).contains("#8fd0ff"), "de la couleur de la famille");
                        assertNotEquals(page.legendStyle(ElementCategory.HALOGEN), page.legendStyle(ElementCategory.ACTINIDE), "chaque famille son spectre");
                    }
                    if (skin == Skin.PAGE_CRUCIBLE) {
                        assertTrue(page.crucible() instanceof Canvas);
                        assertEquals(TablePageSkinArt.CRUCIBLE_WIDTH, ((Canvas) page.crucible()).getWidth());
                    }
                }
                // Les médailles suivent l'avancement : or pour les gaz nobles complets, bronze pour les halogènes entamés, rien ailleurs.
                assertTrue(game.chooseSkin(SkinVisual.TABLE_PAGE, Skin.PAGE_MEDALS));
                page.refresh();
                int gold = 0, bronze = 0, none = 0;
                for (ElementSet set : game.elementSets()) {
                    int tier = page.medalTier(set);
                    if (set.isFamily() && set.category() == ElementCategory.NOBLE_GAS) assertEquals(3, tier, "or");
                    if (set.isFamily() && set.category() == ElementCategory.HALOGEN) assertEquals(1, tier, "bronze");
                    if (tier == 3) gold++;
                    else if (tier == 1) bronze++;
                    else if (tier == 0) none++;
                }
                assertTrue(gold >= 1 && bronze >= 1 && none >= 1);
                // Retour au classique : la légende au caractère près, plus de médaille ni de creuset.
                assertTrue(game.chooseSkin(SkinVisual.TABLE_PAGE, null));
                page.refresh();
                assertEquals(classicLegend, page.legendStyle(ElementCategory.HALOGEN));
                assertFalse(page.medalled());
                assertNull(page.crucible());
                assertEquals(classicCell, page.cellStyle(9));
            }
            return null;
        });
    }

    @Test
    void lesDessinsDesMedaillesEtDuCreusetTiennentATousLesPaliers() {
        FxTestSupport.fx(() -> {
            Canvas medal = new Canvas(TablePageSkinArt.MEDAL_SIZE, TablePageSkinArt.MEDAL_SIZE);
            for (int tier = 0; tier <= 3; tier++) TablePageSkinArt.drawMedal(medal, tier);
            Canvas crucible = new Canvas(TablePageSkinArt.CRUCIBLE_WIDTH, TablePageSkinArt.CRUCIBLE_HEIGHT);
            for (double time = 0; time < 5; time += 0.4) {
                TablePageSkinArt.drawCrucible(crucible, time, true);
                TablePageSkinArt.drawCrucible(crucible, time, false);
            }
            assertNotNull(TablePageSkinArt.spectrumStyle("#f4a6a6"));
            assertTrue(TablePageSkinArt.spectrumStyle("#f4a6a6").contains("#f4a6a6ff"));
            return null;
        });
    }
}
