package idle.ui.bigbang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.191 : les neuf skins du Big Bang, sous Xvfb. Chacun, une fois choisi, dessine l'espace créé avec ses
 * molécules et ses amas, vu en entier (tout en gaz) puis de tout près, à deux tailles de vue, sans exception ;
 * et sans skin, la vue redevient classique.
 */
class SpaceSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Un jeu avec tous les skins, trois sortes rassemblées et un espace immense. */
    private static Game game() {
        Game game = new Game();
        game.start();
        game.testAllSkins(true);
        game.state().setMoleculeCount("H2O", 300);
        game.state().setMoleculeCount("CO2", 200);
        game.state().setMoleculeCount("NaCl", 100);
        game.state().addSubstance("H2O");
        game.state().addSubstance("CO2");
        game.state().addSubstance("NaCl");
        game.state().setSpace(idle.core.math.BigNum.of(1e12));
        return game;
    }

    /** Fait vivre la vue : à deux tailles, vue en entier puis rapprochée jusqu'à ce que le gaz disparaisse. */
    private static void live(SpaceView view) {
        for (double size : new double[] {600, 240}) {
            view.setWidth(size);
            view.setHeight(size * 0.6);
            view.fit();
            for (int k = 0; k < 6; k++) view.frame(0.1);
            for (int step = 0; step < 40 && view.mist() > 0; step++) {
                view.zoom(SpaceView.ZOOM_STEP);
                view.frame(0.1);
            }
            assertEquals(0, view.mist(), 1e-9, "de tout près, plus de gaz");
        }
    }

    @Test
    void chaqueSkinDuBigBangSeDessineSansException() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> skins = skinsOf(SkinVisual.BIG_BANG);
            assertEquals(9, skins.size());
            for (Skin skin : skins) {
                assertTrue(game.chooseSkin(SkinVisual.BIG_BANG, skin), skin.name());
                assertEquals(skin, game.chosenSkin(SkinVisual.BIG_BANG));
                live(new SpaceView(game));
            }
            assertTrue(game.chooseSkin(SkinVisual.BIG_BANG, null));
            live(new SpaceView(game));
            return null;
        });
    }
}
