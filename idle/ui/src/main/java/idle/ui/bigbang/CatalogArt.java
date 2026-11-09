package idle.ui.bigbang;

import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les dessins du catalogue pour les albums du profil (version 0.198) : une molécule ({@link MoleculeArt}), un astre
 * ({@link BodyArt}) ou un assemblage (son bloc, dans le skin des blocs), chacun centré dans un cadre donné et dans les
 * skins que le joueur a choisis pour le Big Bang. C'est la seule porte de ce paquet vers les albums.
 */
public final class CatalogArt {

    /** Le rayon d'une molécule, en pixels par pas d'emprise, comme sur les cartes du Big Bang. */
    private static final double PER_EXTENT = 24;
    /** Le plus grand rayon d'un astre, comme sur les cartes des astres. */
    private static final double LARGEST_BODY = 30;
    /** Le liseré d'un bloc d'assemblage, en part de son rayon. */
    private static final double BLOCK_RIM = 0.18;

    private CatalogArt() {
    }

    /** Dessine une molécule au milieu d'un cadre {@code width} × {@code height}, dans le skin et la palette des molécules. */
    public static void drawMolecule(GraphicsContext g, Molecule molecule, double width, double height, Skin moleculeSkin, Skin palette) {
        MoleculeArt.setPalette(palette);
        MoleculeArt.Shape shape = MoleculeArt.of(molecule);
        double radius = Math.min(height / 2 - 3, shape.extent() * PER_EXTENT);
        shape.draw(g, width / 2, height / 2, radius, -Math.PI / 6, moleculeSkin);
    }

    /** Dessine un astre au milieu d'un cadre {@code width} × {@code height}, dans le skin des astres, queue et anneaux compris. */
    public static void drawBody(GraphicsContext g, Body body, double width, double height, Skin bodySkin) {
        double r = Math.min((height - 6) / (BodyArt.reachUp(body) + BodyArt.reachDown(body)),
                (width - 6) / (BodyArt.reachLeft(body) + BodyArt.reachRight(body)));
        r = Math.min(r, LARGEST_BODY);
        double x = width / 2 + (BodyArt.reachLeft(body) - BodyArt.reachRight(body)) * r / 2;
        double y = height / 2 + (BodyArt.reachUp(body) - BodyArt.reachDown(body)) * r / 2;
        BodyArt.draw(g, body, x, y, r, bodySkin);
    }

    /**
     * Dessine le bloc d'un assemblage au milieu d'un cadre {@code width} × {@code height} : une place par ingrédient
     * (sept au plus), serrées en grappe, de la couleur de sa matière sous son liseré, dans le skin des blocs
     * ({@link ClumpSkinArt}) ou en ronds pour le Classique.
     */
    public static void drawAssembly(GraphicsContext g, Assembly assembly, double width, double height, Skin blockSkin) {
        Color tint = Color.web(assembly.tint());
        Color rim = blockSkin == null ? Color.web("#e8f4ff") : ClumpSkinArt.blockColor(blockSkin, true, tint);
        Color ink = blockSkin == null ? tint : ClumpSkinArt.blockColor(blockSkin, false, tint);
        int places = Math.max(1, Math.min(7, assembly.size()));
        double r = Math.min(height, width) * (places == 1 ? 0.3 : 0.17);
        double[] xs = new double[6], ys = new double[6];
        g.setGlobalAlpha(1);
        for (int pass = 0; pass < 2; pass++) {
            Color color = pass == 0 ? rim : ink;
            double round = pass == 0 ? r : r * (1 - BLOCK_RIM);
            g.setFill(color);
            g.setStroke(color);
            for (int index = 0; index < places; index++) {
                double x = width / 2, y = height / 2;
                if (index > 0) {
                    double a = -Math.PI / 2 + (index - 1) * 2 * Math.PI / Math.max(1, places - 1);
                    x += 1.7 * r * Math.cos(a);
                    y += 1.7 * r * Math.sin(a);
                }
                if (blockSkin == null) g.fillOval(x - round, y - round, 2 * round, 2 * round);
                else ClumpSkinArt.blockPiece(g, blockSkin, x, y, round, index, xs, ys);
                if (pass == 1 && blockSkin != null) {
                    g.setFill(rim);
                    ClumpSkinArt.blockShine(g, blockSkin, x, y, round);
                    g.setFill(color);
                }
            }
        }
    }
}
