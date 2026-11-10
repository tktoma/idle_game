package idle.ui.bigbang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Version 0.203 : la grappe de raisin range les galaxies de l'amas en vraie grappe. Les grains tiennent dans la vue, se
 * touchent sans se recouvrir, s'effilent vers le bas, et celui du joueur (le premier) est au cœur de la grappe.
 */
class GrappeEtEmpreinteTest {

    @Test
    void lesGrainsSeSerrentEnGrappeSansSeRecouvrir() {
        for (int count : new int[] {1, 3, 6, 9, 10, 12}) {
            for (double[] size : new double[][] {{600, 230}, {900, 420}, {320, 200}}) {
                double w = size[0], h = size[1];
                double[] xs = new double[count], ys = new double[count], radii = new double[count];
                GalaxySkinArt.bunch(xs, ys, radii, w, h);
                double lowest = 0, highest = h;
                int low = -1;
                for (int a = 0; a < count; a++) {
                    assertTrue(radii[a] > 3, "un grain visible (" + count + " grains, " + w + "×" + h + ")");
                    assertTrue(xs[a] - radii[a] >= 0 && xs[a] + radii[a] <= w && ys[a] - radii[a] >= 0 && ys[a] + radii[a] <= h,
                            "le grain " + a + " tient dans la vue (" + count + " grains, " + w + "×" + h + ")");
                    for (int b = a + 1; b < count; b++) {
                        double apart = Math.hypot(xs[a] - xs[b], ys[a] - ys[b]);
                        assertTrue(apart >= 0.8 * (radii[a] + radii[b]), "les grains " + a + " et " + b + " ne se recouvrent pas (" + count + ")");
                    }
                    if (ys[a] > lowest) {
                        lowest = ys[a];
                        low = a;
                    }
                    highest = Math.min(highest, ys[a]);
                }
                if (count >= 6) {
                    // La pointe : un seul grain au plus bas ; le grain du joueur n'est ni en haut ni à la pointe.
                    int atTip = 0;
                    for (int a = 0; a < count; a++) if (Math.abs(ys[a] - lowest) < 1e-6) atTip++;
                    assertEquals(1, atTip, "la grappe finit en pointe (" + count + ")");
                    assertTrue(ys[0] > highest + 1 && low != 0, "le grain du joueur est au cœur de la grappe (" + count + ")");
                }
            }
        }
    }
}
