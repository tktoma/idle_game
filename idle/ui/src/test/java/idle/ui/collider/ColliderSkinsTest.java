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
 * Version 0.191 : les sept anneaux et les cinq faisceaux du collisionneur, sous Xvfb. Chaque combinaison d'un
 * anneau et d'un faisceau dessine la vue à deux tailles, les faisceaux loin l'un de l'autre puis au croisement,
 * croisement capté compris, sans exception ; la comète et l'encre laissent quelque chose au détecteur ; sous le
 * ruban de Möbius, un faisceau passe dessus pendant que l'autre passe dessous.
 */
class ColliderSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Un jeu après une première explosion, avec tous les skins et de l'énergie. */
    private static Game game() {
        Game game = new Game(42L);
        game.start();
        game.state().setExplosions(1);
        game.state().setColliderEnergy(1e6);
        game.testAllSkins(true);
        assertTrue(game.isColliderUnlocked());
        return game;
    }

    private static MouseEvent click() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, true, null);
    }

    /** Fait vivre la vue : à deux tailles, loin du croisement, au croisement (capté au clic), puis au-delà. */
    private static void live(Game game, EnergyView view) {
        for (double width : new double[] {780, 400}) {
            view.setWidth(width);
            view.setHeight(170);
            game.state().setColliderPhase(0.15);
            for (int k = 0; k < 10; k++) view.frame(0.05);
            assertFalse(game.isCrossing());
            game.state().setColliderPhase(0.5);
            game.state().setColliderCaught(-1);     // ce croisement n'a pas encore été capté
            assertTrue(game.isCrossing());
            assertTrue(game.canCatchCrossing());
            view.frame(0.05);
            view.getOnMouseClicked().handle(click());
            for (int k = 0; k < 10; k++) view.frame(0.05);
            game.state().setColliderPhase(0.8);
            for (int k = 0; k < 20; k++) view.frame(0.05);
            view.frame(0);     // image figée
        }
    }

    @Test
    void chaqueAnneauAvecChaqueFaisceauSeDessineSansException() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> rings = skinsOf(SkinVisual.COLLIDER);
            List<Skin> beams = skinsOf(SkinVisual.COLLIDER_BEAM);
            assertEquals(7, rings.size());
            assertEquals(5, beams.size());
            rings.add(null);
            beams.add(null);
            for (Skin ring : rings) {
                assertTrue(game.chooseSkin(SkinVisual.COLLIDER, ring));
                for (Skin beam : beams) {
                    assertTrue(game.chooseSkin(SkinVisual.COLLIDER_BEAM, beam));
                    EnergyView view = new EnergyView(game, gain -> { });
                    live(game, view);
                    assertTrue(view.lastGain() > 0, "le croisement capté rapporte");
                }
            }
            return null;
        });
    }

    @Test
    void laCometeEtLEncreLaissentQuelqueChoseAuDetecteur() {
        FxTestSupport.fx(() -> {
            Game game = game();
            for (Skin beam : new Skin[] {Skin.BEAM_COMET, Skin.BEAM_INK}) {
                assertTrue(game.chooseSkin(SkinVisual.COLLIDER_BEAM, beam));
                EnergyView view = new EnergyView(game, gain -> { });
                view.setWidth(780);
                view.setHeight(170);
                game.state().setColliderPhase(0.15);
                view.frame(0.05);
                assertEquals(0, view.beamRemains(), beam.name() + " : rien avant le croisement");
                game.state().setColliderPhase(0.5);
                for (int k = 0; k < 3; k++) view.frame(0.05);
                assertTrue(view.beamRemains() > 0, beam.name() + " : de la neige ou une tache");
                // Changer de faisceau efface ce qui restait.
                assertTrue(game.chooseSkin(SkinVisual.COLLIDER_BEAM, Skin.BEAM_LASER));
                view.frame(0.05);
                assertEquals(0, view.beamRemains());
            }
            return null;
        });
    }

    @Test
    void sousLeRubanDeMobiusUnFaisceauPasseDessusEtLAutreDessous() {
        double angle = 2 * Math.PI * 0.2;
        assertFalse(ColliderSkinArt.under(angle), "le premier faisceau, à droite, passe dessus");
        assertTrue(ColliderSkinArt.under(-angle), "le second, à gauche, passe dessous");
        double beyond = 2 * Math.PI * 0.7;
        assertTrue(ColliderSkinArt.under(beyond), "passé le détecteur, le premier passe dessous");
        assertFalse(ColliderSkinArt.under(-beyond), "et le second dessus");
    }
}
