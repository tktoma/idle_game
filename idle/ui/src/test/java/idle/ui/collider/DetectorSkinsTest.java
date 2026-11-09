package idle.ui.collider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.195 : le détecteur, les éclairs, les étincelles et les quarks qui s'envolent, sous Xvfb. Chaque skin dessine la
 * vue à deux tailles, loin du croisement puis au croisement capté, sans exception ; l'appareil photo flashe au croisement
 * et pas avant ; les éclairs d'un skin comptent autant d'arcs que les classiques ; les étincelles et les quarks d'un skin
 * jaillissent comme les classiques.
 */
class DetectorSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static Game game() {
        Game game = new Game(42L);
        game.start();
        game.state().setExplosions(1);
        game.state().setColliderEnergy(1e6);
        // « Collisions » prise : un croisement capté assure un quark, qui s'envole.
        game.state().setColliderUpgradeLevel(idle.core.collider.ColliderUpgrade.COLLISIONS, 1);
        game.testAllSkins(true);
        assertTrue(game.isColliderUnlocked());
        assertTrue(game.isCollisionsUnlocked());
        return game;
    }

    private static MouseEvent click() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, true, null);
    }

    /** Fait vivre la vue à deux tailles : loin du croisement, au croisement capté au clic, puis au-delà ; rend les arcs du croisement. */
    private static int live(Game game, EnergyView view) {
        int arcs = 0;
        for (double width : new double[] {780, 400}) {
            view.setWidth(width);
            view.setHeight(170);
            game.state().setColliderPhase(0.15);
            for (int k = 0; k < 10; k++) view.frame(0.05);
            assertFalse(game.isCrossing());
            game.state().setColliderPhase(0.5);
            game.state().setColliderCaught(-1);
            assertTrue(game.canCatchCrossing());
            view.frame(0.05);
            arcs = Math.max(arcs, view.arcs());
            view.getOnMouseClicked().handle(click());
            for (int k = 0; k < 10; k++) view.frame(0.05);
            game.state().setColliderPhase(0.8);
            for (int k = 0; k < 20; k++) view.frame(0.05);
            view.frame(0);
        }
        assertTrue(view.lastGain() > 0, "le croisement capté rapporte");
        return arcs;
    }

    @Test
    void chaqueDetecteurSeDessineEtLAppareilPhotoFlasheAuCroisement() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> detectors = skinsOf(SkinVisual.DETECTOR);
            assertEquals(4, detectors.size());
            for (Skin detector : detectors) {
                assertTrue(game.chooseSkin(SkinVisual.DETECTOR, detector));
                EnergyView view = new EnergyView(game, gain -> { });
                live(game, view);
            }
            assertTrue(game.chooseSkin(SkinVisual.DETECTOR, Skin.DETECTOR_CAMERA));
            EnergyView view = new EnergyView(game, gain -> { });
            view.setWidth(780);
            view.setHeight(170);
            game.state().setColliderPhase(0.15);
            for (int k = 0; k < 5; k++) view.frame(0.05);
            assertFalse(view.flashing(), "pas de flash loin du croisement");
            game.state().setColliderPhase(0.5);
            view.frame(0.05);
            assertTrue(view.flashing(), "le flash au croisement");
            for (int k = 0; k < 12; k++) view.frame(0.05);
            assertFalse(view.flashing(), "éteint au bout d'un tiers de seconde");
            return null;
        });
    }

    @Test
    void lesEclairsDUnSkinComptentAutantDArcsQueLesClassiques() {
        FxTestSupport.fx(() -> {
            Game game = game();
            assertTrue(game.chooseSkin(SkinVisual.LIGHTNING, null));
            int classic = live(game, new EnergyView(game, gain -> { }));
            assertTrue(classic >= 3, "les arcs classiques au croisement");
            List<Skin> bolts = skinsOf(SkinVisual.LIGHTNING);
            assertEquals(3, bolts.size());
            for (Skin skin : bolts) {
                assertTrue(game.chooseSkin(SkinVisual.LIGHTNING, skin));
                assertEquals(classic, live(game, new EnergyView(game, gain -> { })), skin.name());
            }
            return null;
        });
    }

    @Test
    void lesEtincellesEtLesQuarksDUnSkinJaillissent() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> sparks = skinsOf(SkinVisual.SPARKS);
            List<Skin> flights = skinsOf(SkinVisual.QUARK_FLIGHT);
            assertEquals(3, sparks.size());
            assertEquals(3, flights.size());
            sparks.add(null);
            flights.add(null);
            for (Skin spark : sparks) {
                assertTrue(game.chooseSkin(SkinVisual.SPARKS, spark));
                for (Skin flight : flights) {
                    assertTrue(game.chooseSkin(SkinVisual.QUARK_FLIGHT, flight));
                    EnergyView view = new EnergyView(game, gain -> { });
                    view.setWidth(780);
                    view.setHeight(170);
                    game.state().setColliderPhase(0.15);
                    for (int k = 0; k < 5; k++) view.frame(0.05);
                    game.state().setColliderPhase(0.5);
                    game.state().setColliderCaught(-1);
                    view.frame(0.05);
                    view.getOnMouseClicked().handle(click());
                    view.frame(0.05);
                    assertTrue(view.sparks() > 30, spark + " / " + flight + " : la gerbe du croisement capté");
                    assertTrue(view.collisionsShown() > 0, spark + " / " + flight + " : le quark de la collision assurée");
                    for (int k = 0; k < 40; k++) view.frame(0.05);
                    live(game, view);
                }
            }
            return null;
        });
    }
}
