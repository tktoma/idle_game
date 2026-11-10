package idle.ui.probes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeTrip;
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
 * Version 0.194 : les quatre skins des sondes, sous Xvfb. Chacun dessine la vue des sondes, à deux tailles, avec
 * une sonde en vol et une arrivée, sans exception, et autrement que le Classique ; le dos de la carte du butin le suit.
 */
class ProbeSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> probeSkins() {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == SkinVisual.PROBE) skins.add(skin);
        return skins;
    }

    /** Un jeu avec tous les skins et deux sondes : une à mi-chemin, une arrivée. */
    private static Game game() {
        Game game = new Game();
        game.start();
        game.testAllSkins(true);
        List<Probe> probes = new ArrayList<>();
        probes.add(new Probe(ProbeDestination.GAS_CLOUD, ProbeTrip.SHORT, 450, 900, 1));
        probes.add(new Probe(ProbeDestination.NEBULA, ProbeTrip.HOUR, 0, 3600, 2));
        game.state().setProbes(probes);
        // Deux sondes en vol demandent deux places : avec une seule, la seconde voie tombait hors de la vue.
        game.state().setProbeUpgradeLevel(idle.core.probes.ProbeUpgrade.HANGAR, 1);
        game.state().setProbeRewards(List.of(new idle.core.probes.ProbeReward(idle.core.probes.ProbeReward.Type.SPACE, "", 100, ProbeDestination.NEBULA, 2)));
        return game;
    }

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

    @Test
    void chaqueSkinDesSondesSeDessineAutrementQueLeClassique() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> skins = probeSkins();
            assertEquals(4, skins.size());
            // Le témoin a sa propre partie, sans skin : la vue lit le skin choisi dans la partie à chaque image.
            ProbesView plain = new ProbesView(game(), id -> { });
            for (Skin skin : skins) {
                assertTrue(game.chooseSkin(SkinVisual.PROBE, skin), skin.name());
                ProbesView view = new ProbesView(game, id -> { });
                for (double size : new double[] {780, 420}) {
                    plain.setWidth(size);
                    plain.setHeight(ProbesView.heightFor(2));
                    view.setWidth(size);
                    view.setHeight(ProbesView.heightFor(2));
                    for (int k = 0; k < 3; k++) {
                        plain.frame(0.2);
                        view.frame(0.2);
                    }
                    assertTrue(differing(plain, view) > 0, skin.name() + " à " + size);
                }
                // Le clic sur la sonde arrivée répond toujours.
                // La sonde arrivée est la seconde : sa voie est aux trois quarts de la hauteur utile quand il y en a deux.
                assertEquals(2, game.probeSlots());
                assertNotEquals(null, view.probeAt(view.getWidth() - 90, 20 + (view.getHeight() - 40) * 0.75));
            }
            assertTrue(game.chooseSkin(SkinVisual.PROBE, null));
            assertFalse(ProbeSkinArt.draws(Skin.CARD_TAG));
            return null;
        });
    }

    @Test
    void leDosDeLaCarteDuButinSuitLeSkin() {
        FxTestSupport.fx(() -> {
            Game game = game();
            LootPane loot = new LootPane(game, () -> { });
            for (Skin skin : probeSkins()) {
                assertTrue(game.chooseSkin(SkinVisual.PROBE, skin));
                loot.open(2);
                assertTrue(loot.isOpen(), skin.name());
                assertTrue(loot.cardText().startsWith("Récompense |"), loot.cardText());
                assertNotEquals("?", ProbeSkinArt.cardMark(skin));
                assertTrue(ProbeSkinArt.cardBack(skin).startsWith("#"));
                loot.frame(0.1);
                loot.close();
            }
            return null;
        });
    }
}
