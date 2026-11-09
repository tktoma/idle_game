package idle.ui.atoms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.elements.ElementCategory;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.191 : les skins de l'atome et du tableau périodique, sous Xvfb. Chaque skin de l'atome dessine le
 * grand atome à tous les remplissages, avec ses copies et l'arrivée d'une orbe, sans exception ; chaque skin du
 * tableau peint les cases (pas obtenues, possédées, au maximum, uniques, visées) et donne à au moins une case un
 * style différent du classique ; « Minuscule » réduit les cases de moitié ; sans skin, tout revient au classique.
 */
class AtomTableSkinsTest {

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

    /** Fait vivre le grand atome sous son skin : vide, puis chaque couche qui se remplit, puis les copies. */
    private static void live(AtomModelView view) {
        view.setWidth(900);
        view.setHeight(500);
        for (int orbs : new int[] {0, 1, 2, 10, 28, 60, 92, 110, 118}) {
            view.setOrbs(orbs);
            view.celebrate();
            for (int k = 0; k < 12; k++) view.frame(0.1);
        }
        view.setExtraDecades(6);
        assertTrue(view.copies() > 1);
        for (int k = 0; k < 5; k++) view.frame(0.1);
        view.frame(0);     // image figée : sans animation
    }

    @Test
    void chaqueSkinDeLAtomeSeDessineSansException() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.ATOM);
            assertEquals(7, skins.size());
            for (Skin skin : skins) {
                AtomModelView view = new AtomModelView(240);
                view.setSkin(skin);
                assertEquals(skin, view.skin());
                live(view);
            }
            AtomModelView classic = new AtomModelView(240);
            classic.setSkin(Skin.TABLE_WOOD);
            assertNull(classic.skin(), "un skin d'un autre visuel : le classique");
            live(classic);
            return null;
        });
    }

    /** Un jeu avec tous les skins, quelques éléments possédés, un au maximum, et une famille visée. */
    private static Game game() {
        Game game = new Game();
        game.start();
        game.testAllSkins(true);
        game.state().setElementCount(1, 1);
        game.state().setElementCount(6, 2);
        game.state().setElementCount(26, 99);     // bien au-delà du maximum
        game.state().setElementCount(2, 1);       // un gaz noble, unique
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);     // le tableau est ouvert
        game.state().setDarkLevel("dark_shop_target", 1);     // la synthèse ciblée, achetée dans l'arbre de matière noire
        assertTrue(game.isTargetedSynthesisUnlocked());
        assertTrue(game.setSynthesisTarget(ElementCategory.HALOGEN));
        return game;
    }

    private static PeriodicTablePage page(Game game) {
        PeriodicTablePage page = new PeriodicTablePage(game);
        new Scene(page, 1200, 760);
        page.refresh();
        page.applyCss();
        page.layout();
        return page;
    }

    @Test
    void chaqueSkinDuTableauChangeAuMoinsUneCase() {
        FxTestSupport.fx(() -> {
            Game game = game();
            PeriodicTablePage page = page(game);
            double classicSize = page.shownCellSize();
            String classicTable = page.tableStyle();
            assertEquals("", classicTable);
            int[] watched = {1, 6, 26, 2, 9, 118};     // possédé, possédé, au maximum, unique possédé, halogène visé, jamais obtenu
            List<String> classic = new ArrayList<>();
            for (int number : watched) classic.add(page.cellStyle(number));
            List<Skin> skins = skinsOf(SkinVisual.TABLE);
            assertEquals(8, skins.size());
            for (Skin skin : skins) {
                assertTrue(game.chooseSkin(SkinVisual.TABLE, skin), skin.name());
                page.refresh();
                page.applyCss();
                page.layout();
                for (int k = 0; k < watched.length; k++) {
                    assertNotEquals(classic.get(k), page.cellStyle(watched[k]), skin.name() + " case " + watched[k]);
                    assertFalse(page.cellStyle(watched[k]).isEmpty(), skin.name());
                }
                if (skin == Skin.TABLE_TINY) {
                    assertEquals(classicSize / 2, page.shownCellSize(), 1e-9, "cases deux fois plus petites");
                    assertEquals("Fe", page.cellText(26), "rien que les lettres");
                } else {
                    assertEquals(classicSize, page.shownCellSize(), 1e-9, skin.name());
                }
                if (skin == Skin.TABLE_MAP) {
                    assertTrue(page.cellText(26).endsWith("✕"), "le trésor déterré porte une croix");
                    assertTrue(page.cellStyle(9).contains("#b02a2a"), "une île de la famille visée est marquée en rouge");
                    assertFalse(page.cellStyle(118).contains("#b02a2a"), "une île d'une autre famille ne l'est pas");
                    assertTrue(page.tableStyle().contains("#d8c49a"), "le parchemin");
                    // Changer de cible repeint les îles marquées.
                    assertTrue(game.setSynthesisTarget(null));
                    page.refresh();
                    assertFalse(page.cellStyle(9).contains("#b02a2a"), "plus de cible, plus de croix");
                    assertTrue(game.setSynthesisTarget(ElementCategory.HALOGEN));
                }
                if (skin == Skin.TABLE_NEON) assertTrue(page.tableStyle().contains("#050508"), "le fond noir");
            }
            // Retour au classique : mêmes styles, même taille, pas de fond.
            assertTrue(game.chooseSkin(SkinVisual.TABLE, null));
            page.refresh();
            for (int k = 0; k < watched.length; k++) assertEquals(classic.get(k), page.cellStyle(watched[k]), "classique, case " + watched[k]);
            assertEquals(classicSize, page.shownCellSize(), 1e-9);
            assertEquals("", page.tableStyle());
            assertEquals("Fe\n" + game.maxCopiesOf(idle.core.elements.PeriodicTable.element(26)) + "/"
                    + game.maxCopiesOf(idle.core.elements.PeriodicTable.element(26)), page.cellText(26));
            return null;
        });
    }
}
