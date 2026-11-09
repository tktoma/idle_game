package idle.ui.particles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.191 : les neuf skins de la fusion, sous Xvfb. Chacun se laisse choisir et dessine, image après
 * image, un générateur qui naît, crée des particules à toutes les vitesses (poussières, ondes, arcs d'énergie)
 * et change de taille, sans exception ; un skin d'un autre visuel ramène au classique.
 */
class ParticleSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @AfterEach
    void classic() {
        ParticleView.setSkin(null);
    }

    /** Les skins de la fusion, dans l'ordre du catalogue. */
    private static List<Skin> fusionSkins() {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == SkinVisual.PARTICLE_FUSION) skins.add(skin);
        return skins;
    }

    /** Fait vivre un générateur sous le skin courant : naissance, régime lent, puis très rapide, à deux tailles. */
    private static void live(ParticleView view) {
        for (double size : new double[] {200, 90}) {
            view.setWidth(size);
            view.setHeight(size);
            // La naissance, puis des particules une par une : l'avancement repart de zéro, une onde part.
            for (int k = 0; k < 40; k++) view.frame(0.05, (k % 10) / 10.0, 0.5);
            // Très vite : la vitesse s'installe, les arcs d'énergie jaillissent, le noyau bat.
            for (int k = 0; k < 60; k++) view.frame(0.05, 1, 5000);
            // Au point mort : plus rien n'arrive, tout s'éteint.
            for (int k = 0; k < 10; k++) view.frame(0.05, 0.3, 0);
        }
    }

    @Test
    void chaqueSkinDeLaFusionSeDessineSansException() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = fusionSkins();
            assertEquals(9, skins.size());
            for (Skin skin : skins) {
                ParticleView.setSkin(skin);
                assertEquals(skin, ParticleView.skin());
                live(new ParticleView());
            }
            return null;
        });
    }

    @Test
    void unSkinDUnAutreVisuelOuAucunDonneLeClassique() {
        FxTestSupport.fx(() -> {
            ParticleView.setSkin(Skin.ATOM_CHALK);
            assertNull(ParticleView.skin());
            live(new ParticleView());
            ParticleView.setSkin(null);
            assertNull(ParticleView.skin());
            live(new ParticleView());
            return null;
        });
    }
}
