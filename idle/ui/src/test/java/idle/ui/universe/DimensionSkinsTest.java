package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Les skins des dix mini-jeux (version 0.187), sous Xvfb : pour chaque dimension, avec son skin choisi, la vue se dessine
 * sans exception à plusieurs instants, et les positions de ce qu'on clique sont les mêmes avec et sans skin.
 */
class DimensionSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie où la dimension est ouverte : autant d'univers ajoutés qu'il en faut après le premier formé. */
    private static Game opened(Dimension dimension) {
        Game game = new Game(42L);
        game.start();
        game.testFormUniverse();
        for (int k = 1; k < dimension.number(); k++) assertTrue(game.testAddUniverse());
        return game;
    }

    /** La vue de la dimension, dessinée une première fois. */
    private static DimensionView view(Game game, Dimension dimension) {
        DimensionView view = new DimensionView(game);
        view.show(dimension);
        view.frame(0.05, true);
        return view;
    }

    /** Fait passer le temps du jeu et de la vue ensemble, par images de 0,1 s : la vue se dessine à chacune. */
    private static void run(Game game, DimensionView view, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.1) {
            game.tick(0.1);
            view.frame(0.1, true);
        }
    }

    /** Ce que lisent les tests des fenêtres et des pages : les positions de ce qu'on clique dans chaque dimension. */
    private static double[] positions(DimensionView view, Dimension dimension) {
        double w = view.getWidth(), h = view.getHeight();
        return switch (dimension) {
            case LINE, PLANE -> new double[] {w, h};
            case VOLUME -> {
                double[] at = view.screenOf(0.3, 0.2, 0.1);
                yield at == null ? new double[] {w, h} : new double[] {w, h, at[0], at[1]};
            }
            case DURATION -> new double[] {view.overDurationButton(w / 2, h / 2) ? 1 : 0, view.overDurationButton(w / 2 + w * 0.3, h / 2) ? 1 : 0};
            case LOOP -> {
                double[] point = view.loopPoint(0);
                yield new double[] {view.loopRadius(0), view.loopRadius(2), point[0], point[1], view.loopRingAt(point[0], point[1])};
            }
            case BRANE -> {
                double[] at = view.braneScreen(0.3, -0.2);
                yield new double[] {view.braneHalf(), at[0], at[1], view.overBrane(at[0], at[1]) ? 1 : 0};
            }
            case CLICKER -> {
                double[] at = view.clickerScreen(0.25, 0.75);
                yield new double[] {view.clickerHalf(), at[0], at[1], view.overClicker(at[0], at[1]) ? 1 : 0};
            }
            case GRID -> {
                double[] at = view.gridCellScreen(3, 4);
                int[] cell = view.gridCellAt(at[0], at[1]);
                yield new double[] {at[0], at[1], cell[0], cell[1], view.gridSpan()};
            }
            case MIRROR -> {
                double[] at = view.mirrorCellScreen(1, 2);
                Game.MirrorCell cell = view.mirrorCellAt(at[0], at[1]);
                yield new double[] {at[0], at[1], cell.column(), cell.row()};
            }
            case MIX -> {
                double[] arena = view.mixArena(), at = view.mixScreen(0.3, 0.6), back = view.mixAt(at[0], at[1]);
                yield new double[] {arena[0], arena[1], arena[2], at[0], at[1], back[0], back[1]};
            }
        };
    }

    @Test
    void chaqueMiniJeuSeDessineAvecSonSkinSansBougerSesClics() {
        FxTestSupport.fx(() -> {
            for (Dimension dimension : Dimension.values()) {
                SkinVisual visual = SkinVisual.DIMENSIONS[dimension.ordinal()];
                Skin skin = Skin.DIMENSION_SKINS[dimension.ordinal()];
                assertEquals(visual, skin.visual(), dimension + " : le skin habille le visuel de sa dimension");
                // Sans skin : le Classique.
                Game classic = opened(dimension);
                assertNull(classic.chosenSkin(visual));
                DimensionView plain = view(classic, dimension);
                double[] before = positions(plain, dimension);
                run(classic, plain, 0.5);
                // Avec le skin choisi : la vue se dessine à plusieurs instants, et rien de cliquable n'a bougé.
                Game game = opened(dimension);
                game.testAllSkins(true);
                assertTrue(game.chooseSkin(visual, skin), dimension + " : le skin débloqué se choisit");
                assertEquals(skin, game.chosenSkin(visual));
                DimensionView view = view(game, dimension);
                assertArrayEquals(before, positions(view, dimension), 1e-9, dimension + " : les positions de clic ne dépendent pas du skin");
                run(game, view, 0.5);
                view.frame(0.1, false);
                run(game, view, 2);
                assertArrayEquals(before, positions(view, dimension), 1e-9, dimension + " : ni après deux secondes et demie");
                assertEquals(dimension, view.dimension());
            }
            return null;
        });
    }

    @Test
    void leSkinSeDessineAussiPendantQuOnJoue() {
        FxTestSupport.fx(() -> {
            // La durée tenue, la boucle jouée, la brane tirée, le clicker cliqué, la grille essayée, le mélange mené : avec le skin.
            Game game = opened(Dimension.DURATION);
            game.testAllSkins(true);
            for (Dimension dimension : Dimension.values()) {
                assertTrue(game.chooseSkin(SkinVisual.DIMENSIONS[dimension.ordinal()], Skin.DIMENSION_SKINS[dimension.ordinal()]));
            }
            DimensionView view = view(game, Dimension.DURATION);
            assertTrue(view.press(view.getWidth() / 2, view.getHeight() / 2, false), "le bouton du vinyle se tient");
            run(game, view, 1);
            assertTrue(view.releaseHold());
            run(game, view, 0.5);
            for (int k = Dimension.DURATION.number(); k < Dimension.MIX.number(); k++) assertTrue(game.testAddUniverse());
            assertTrue(game.isMixOpen());
            view.show(Dimension.LOOP);
            run(game, view, 0.5);
            double[] point = view.loopPoint(0);
            assertTrue(view.press(point[0], point[1], false), "une flèche se plante sur la cible");
            run(game, view, 0.5);
            view.show(Dimension.BRANE);
            run(game, view, 0.3);
            double[] from = view.braneScreen(0, 0), to = view.braneScreen(0.3, 0);
            assertTrue(view.press(from[0], from[1], false), "la toile se tient");
            assertTrue(view.dragBrane(to[0], to[1]));
            run(game, view, 0.2);
            assertTrue(view.releaseBrane(), "et l'onde fait frémir les fils");
            run(game, view, 1);
            view.show(Dimension.CLICKER);
            double[] at = view.clickerScreen(0.5, 0.5);
            assertTrue(view.press(at[0], at[1], false), "une bulle de savon monte");
            run(game, view, 1);
            view.show(Dimension.GRID);
            run(game, view, 0.3);
            assertNotNull(view.tryGrid(), "une case du démineur s'enfonce");
            run(game, view, 1);
            view.show(Dimension.MIRROR);
            run(game, view, 1);
            view.show(Dimension.MIX);
            run(game, view, 1);
            double[] arena = view.mixScreen(0.5, 0.5);
            view.press(arena[0], arena[1], false);
            run(game, view, 1);
            return null;
        });
    }
}
