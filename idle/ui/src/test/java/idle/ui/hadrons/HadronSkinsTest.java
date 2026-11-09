package idle.ui.hadrons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.195 : les quarks, les gluons et le fond de la liaison, sous Xvfb. Chaque skin dessine la vue à deux tailles,
 * les quarks flottant puis une liaison en cours (un proton et un méson, l'antiquark compris), avec et sans animation,
 * sans exception ; chaque fond change la couleur d'un coin de la vue par rapport au Classique.
 */
class HadronSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** L'onglet ouvert avec les exotiques, trente quarks de chaque saveur, tous les skins. */
    private static Game game() {
        Game game = new Game(7L);
        game.start();
        game.testFormUniverse();
        game.state().setTreeBubbleBought(TreeBubble.CROWN_LEFT.ordinal(), true);
        game.state().setTreeBubbleBought(TreeBubble.HEART.ordinal(), true);
        assertTrue(game.startNextUniverse());
        game.state().setHadronsOpen(true);
        for (Quark quark : Quark.values()) game.state().setQuarks(quark, 30);
        game.testAllSkins(true);
        assertTrue(game.isHadronAvailable(Hadron.PION));
        return game;
    }

    /** Fait vivre la vue : à deux tailles, les quarks qui flottent, puis un proton et un pion qui se lient, animés puis figés. */
    private static void live(Game game, BindingView view) {
        for (double width : new double[] {780, 400}) {
            view.setWidth(width);
            view.setHeight(340);
            for (int k = 0; k < 10; k++) view.frame(0.05, true);
            assertTrue(view.floatingQuarks() > 0, "des quarks flottent");
            assertTrue(game.bindHadron(Hadron.PROTON));
            view.bind(Hadron.PROTON, 1);
            assertTrue(game.bindHadron(Hadron.PION));
            view.bind(Hadron.PION, 1);
            assertEquals(2, view.bindingsPlaying());
            for (int k = 0; k < 60; k++) view.frame(0.05, true);
            assertEquals(0, view.bindingsPlaying(), "les liaisons finies en trois secondes");
            assertTrue(game.bindHadron(Hadron.NEUTRON));
            view.bind(Hadron.NEUTRON, 1);
            view.frame(0.05, false);
            assertEquals(0, view.bindingsPlaying(), "sans animation, la liaison finit d'un coup");
        }
    }

    @Test
    void chaqueSkinDesQuarksEtDesGluonsSeDessineSansException() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> quarks = skinsOf(SkinVisual.QUARKS);
            List<Skin> gluons = skinsOf(SkinVisual.GLUONS);
            assertEquals(4, quarks.size());
            assertEquals(4, gluons.size());
            quarks.add(null);
            gluons.add(null);
            for (Skin quark : quarks) {
                assertTrue(game.chooseSkin(SkinVisual.QUARKS, quark));
                for (Skin gluon : gluons) {
                    assertTrue(game.chooseSkin(SkinVisual.GLUONS, gluon));
                    for (Quark flavor : Quark.values()) game.state().setQuarks(flavor, 30);
                    live(game, new BindingView(game));
                }
            }
            return null;
        });
    }

    @Test
    void chaqueFondChangeLaCouleurDUnCoin() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> backgrounds = skinsOf(SkinVisual.HADRON_BACKGROUND);
            assertEquals(3, backgrounds.size());
            assertTrue(game.chooseSkin(SkinVisual.HADRON_BACKGROUND, null));
            Color classic = corner(game);
            for (Skin background : backgrounds) {
                assertTrue(game.chooseSkin(SkinVisual.HADRON_BACKGROUND, background));
                live(game, new BindingView(game));
                Color color = corner(game);
                assertTrue(!color.equals(classic), background.name() + " : un fond différent du Classique (" + color + ")");
            }
            return null;
        });
    }

    /** La couleur d'un point du coin bas gauche de la vue, dans la rangée, loin des quarks, après une image figée. */
    private static Color corner(Game game) {
        BindingView view = new BindingView(game);
        view.setWidth(780);
        view.setHeight(340);
        view.frame(0.05, false);
        WritableImage image = new WritableImage(780, 340);
        view.snapshot(null, image);
        return image.getPixelReader().getColor(9, 338);
    }
}
