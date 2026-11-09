package idle.ui.bigbang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.188 : les quatre skins des molécules dessinent chaque molécule du catalogue à toutes les
 * tailles sans exception, ne changent ni l'emprise ni la couleur dominante que lisent les vues, et
 * sans skin ({@code null}) le dessin est, au pixel, celui de l'appel sans argument.
 */
class MoleculeArtSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Les skins qui habillent les molécules, dans l'ordre de l'énumération. */
    private static List<Skin> moleculeSkins() {
        return java.util.Arrays.stream(Skin.values()).filter(skin -> skin.visual() == SkinVisual.MOLECULE).toList();
    }

    @Test
    void chaqueMoleculeSeDessineDansChaqueSkinAToutesLesTailles() {
        FxTestSupport.fx(() -> {
            Canvas canvas = new Canvas(200, 200);
            GraphicsContext g = canvas.getGraphicsContext2D();
            List<Skin> skins = moleculeSkins();
            assertEquals(4, skins.size());
            for (Skin skin : skins) {
                for (Molecule molecule : Molecules.DEFAULT) {
                    // Minuscule, au seuil des lettres du skin « Traits », et grande : aucun ne doit lever d'exception.
                    for (double size : new double[] {6, MoleculeArt.LETTERS_FROM, 40}) {
                        g.clearRect(0, 0, 200, 200);
                        MoleculeArt.of(molecule).draw(g, 100, 100, size, 0.4, skin);
                    }
                }
            }
            // Un skin qui n'habille pas les molécules, ou aucun, donne le dessin classique.
            MoleculeArt.of(Molecules.DEFAULT.get(0)).draw(g, 100, 100, 20, 0, Skin.TREE_BIRCH);
            MoleculeArt.of(Molecules.DEFAULT.get(0)).draw(g, 100, 100, 20, 0, null);
            return null;
        });
    }

    @Test
    void lEmpriseEtLaDominanteNeDependentPasDuSkin() {
        for (Molecule molecule : Molecules.DEFAULT) {
            MoleculeArt.Shape shape = MoleculeArt.of(molecule);
            assertTrue(shape.extent() >= 0.5, molecule.id());
            assertEquals(shape.atoms().get(0).color(), shape.dominant(), molecule.id());
            // Les boules pleines et les briques dépassent les boules du Classique, mais le dessin les ramène dans le cercle demandé.
            assertTrue(shape.looks().filledExtent >= shape.extent(), molecule.id() + " boules pleines");
            assertTrue(shape.looks().brickExtent > 0, molecule.id() + " briques");
        }
    }

    @Test
    void sansSkinLeDessinEstLeClassique() {
        FxTestSupport.fx(() -> {
            // Les deux appels sont dessinés sur deux toiles et comparés pixel à pixel, sur quelques molécules.
            Canvas plain = new Canvas(120, 120);
            Canvas nulled = new Canvas(120, 120);
            for (Molecule molecule : Molecules.DEFAULT.subList(0, Math.min(8, Molecules.DEFAULT.size()))) {
                plain.getGraphicsContext2D().clearRect(0, 0, 120, 120);
                nulled.getGraphicsContext2D().clearRect(0, 0, 120, 120);
                MoleculeArt.of(molecule).draw(plain.getGraphicsContext2D(), 60, 60, 40, -Math.PI / 6);
                MoleculeArt.of(molecule).draw(nulled.getGraphicsContext2D(), 60, 60, 40, -Math.PI / 6, null);
                WritableImage a = plain.snapshot(null, null);
                WritableImage b = nulled.snapshot(null, null);
                for (int px = 0; px < 120; px += 2) {
                    for (int py = 0; py < 120; py += 2) {
                        assertEquals(a.getPixelReader().getArgb(px, py), b.getPixelReader().getArgb(px, py), molecule.id() + " en " + px + "," + py);
                    }
                }
            }
            return null;
        });
    }
}
