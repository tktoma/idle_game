package idle.ui.pages;

import idle.core.universe.Skin;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Le style des avatars (version 0.198, {@link idle.core.universe.SkinVisual#AVATAR_STYLE}) : l'avatar du jeu est d'abord
 * dessiné tel quel, hors de l'écran, puis son image est retravaillée pixel par pixel, le même traitement pour les vingt
 * avatars. « Trait » ne garde que ses contours, à l'encre sur du papier, les zones sombres hachurées ; « Pixel » le
 * dessine tout petit puis l'agrandit sans lissage ; « Craie » le poudre en pastels grenus sur une ardoise ; « Vitrail »
 * aplatit ses couleurs en panneaux de verre éclairés en biais, séparés par des plombs. L'image importée par le joueur
 * n'est jamais retouchée.
 */
final class AvatarSkinArt {

    private static final Color PAPER = Color.web("#f4f1e8"), INK = Color.web("#2b2b33");
    private static final Color SLATE = Color.web("#2e3a35");
    private static final Color LEAD = Color.web("#141418");
    private static final Color GLASS_NIGHT = Color.web("#1e2a5a");
    /** Le côté du dessin réduit du style « Pixel », en pixels, pour 96 px d'avatar ; il suit la taille, 12 au moins. */
    private static final double PIXELS_PER_SIDE = 19.0 / 96;

    private AvatarSkinArt() {
    }

    /** Dessine l'avatar dans ce style sur le carré de côté {@code side} posé à l'origine ; {@code style} est un skin du visuel. */
    static void paint(GraphicsContext g, Avatar avatar, double side, Skin style) {
        int n = Math.max(1, (int) Math.round(side));
        switch (style) {
            case AVATAR_PIXEL -> {
                int small = Math.max(12, (int) Math.round(side * PIXELS_PER_SIDE));
                WritableImage tiny = plain(avatar, small);
                g.save();
                g.setImageSmoothing(false);
                g.drawImage(tiny, 0, 0, small, small, 0, 0, side, side);
                g.restore();
            }
            case AVATAR_LINE -> g.drawImage(lineArt(plain(avatar, n), n), 0, 0, side, side);
            case AVATAR_CHALK -> g.drawImage(chalk(plain(avatar, n), n), 0, 0, side, side);
            case AVATAR_STAINED -> {
                g.drawImage(stained(plain(avatar, n), n), 0, 0, side, side);
                // Trois plombs qui partent du centre, comme les rayons d'une rosace, par-dessus les panneaux.
                g.setStroke(LEAD);
                g.setLineWidth(Math.max(1.5, side / 48));
                for (int k = 0; k < 3; k++) {
                    double a = -Math.PI / 2 + 0.4 + k * 2 * Math.PI / 3;
                    g.strokeLine(side / 2, side / 2, side / 2 + side * Math.cos(a), side / 2 + side * Math.sin(a));
                }
            }
            default -> AvatarView.paint(g, avatar, side);     // un skin d'un autre visuel ne peut pas arriver ici : {@code setStyle} ne garde que les siens
        }
    }

    /** L'avatar du jeu dessiné tel quel, sur son fond, en {@code n} pixels de côté. */
    private static WritableImage plain(Avatar avatar, int n) {
        Canvas canvas = new Canvas(n, n);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web(AvatarView.BACKGROUND));
        g.fillRect(0, 0, n, n);
        AvatarView.paint(g, avatar, n);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.web(AvatarView.BACKGROUND));
        return canvas.snapshot(parameters, null);
    }

    /** « Trait » : les contours à l'encre là où la clarté saute d'un pixel au voisin, des hachures sur ce qui est sombre, le reste en papier. */
    private static WritableImage lineArt(WritableImage source, int n) {
        PixelReader in = source.getPixelReader();
        WritableImage out = new WritableImage(n, n);
        PixelWriter w = out.getPixelWriter();
        double[] lum = new double[n * n];
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) lum[y * n + x] = luminance(in.getColor(x, y));
        double background = luminance(Color.web(AvatarView.BACKGROUND));
        double threshold = 0.1;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double here = lum[y * n + x];
                boolean edge = false;
                if (x + 1 < n && Math.abs(here - lum[y * n + x + 1]) > threshold) edge = true;
                if (y + 1 < n && Math.abs(here - lum[(y + 1) * n + x]) > threshold) edge = true;
                if (x > 0 && Math.abs(here - lum[y * n + x - 1]) > threshold) edge = true;
                if (y > 0 && Math.abs(here - lum[(y - 1) * n + x]) > threshold) edge = true;
                // Les hachures : une diagonale sur quatre, là où l'avatar est franchement plus sombre que son halo.
                boolean hatched = here < background + 0.02 && (x + y) % 4 == 0 && insideHalo(x, y, n);
                w.setColor(x, y, edge ? INK : hatched ? INK.interpolate(PAPER, 0.55) : PAPER);
            }
        }
        return out;
    }

    /** « Craie » : chaque pixel plus clair que le fond devient un grain de craie pastel, d'autant plus sûrement qu'il est clair, sur une ardoise. */
    private static WritableImage chalk(WritableImage source, int n) {
        PixelReader in = source.getPixelReader();
        WritableImage out = new WritableImage(n, n);
        PixelWriter w = out.getPixelWriter();
        double background = luminance(Color.web(AvatarView.BACKGROUND));
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                Color color = in.getColor(x, y);
                double strength = Math.min(1, Math.max(0, (luminance(color) - background) / 0.55));
                double grain = noise(x, y);
                if (grain < strength * 0.9 + 0.02) {
                    // Un grain : la couleur poussée vers le blanc, légèrement voilée par l'ardoise selon le hasard du grain.
                    Color pastel = color.interpolate(Color.WHITE, 0.55).interpolate(SLATE, 0.25 * noise(y, x));
                    w.setColor(x, y, pastel);
                } else {
                    // L'ardoise, un peu poussiéreuse près des traits.
                    w.setColor(x, y, strength > 0.3 ? SLATE.interpolate(Color.WHITE, 0.08) : SLATE);
                }
            }
        }
        return out;
    }

    /** « Vitrail » : les couleurs aplaties en quelques tons de verre éclairés en biais, et des plombs entre deux tons différents. */
    private static WritableImage stained(WritableImage source, int n) {
        PixelReader in = source.getPixelReader();
        WritableImage out = new WritableImage(n, n);
        PixelWriter w = out.getPixelWriter();
        int[] tones = new int[n * n];
        Color[] glass = new Color[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                Color color = in.getColor(x, y);
                int r = (int) Math.round(color.getRed() * 3), gr = (int) Math.round(color.getGreen() * 3), b = (int) Math.round(color.getBlue() * 3);
                tones[y * n + x] = r * 16 + gr * 4 + b;
                Color flat = Color.color(r / 3.0, gr / 3.0, b / 3.0);
                // Le fond sombre devient du verre bleu nuit ; les autres tons gardent leur teinte, en verre un peu plus clair.
                glass[y * n + x] = luminance(flat) < 0.12 ? GLASS_NIGHT : flat.interpolate(Color.WHITE, 0.12);
            }
        }
        int lead = n >= 80 ? 2 : 1;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                boolean edge = false;
                for (int d = 1; d <= lead && !edge; d++) {
                    if (x + d < n && tones[y * n + x + d] != tones[y * n + x]) edge = true;
                    if (y + d < n && tones[(y + d) * n + x] != tones[y * n + x]) edge = true;
                }
                if (edge) {
                    w.setColor(x, y, LEAD);
                } else {
                    // La lumière en biais : une bande claire sur deux, en diagonale, comme le jour à travers le verre.
                    double sheen = ((x - y + 4 * n) / Math.max(1, n / 6)) % 2 == 0 ? 0.12 : 0;
                    w.setColor(x, y, glass[y * n + x].interpolate(Color.WHITE, sheen));
                }
            }
        }
        return out;
    }

    /** Vrai dans le rond du halo de l'avatar, celui que les cadres découpent : les hachures ne débordent pas sur les coins. */
    private static boolean insideHalo(int x, int y, int n) {
        double dx = x + 0.5 - n / 2.0, dy = y + 0.5 - n / 2.0;
        return dx * dx + dy * dy <= 0.23 * n * n;
    }

    private static double luminance(Color color) {
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
    }

    /** Un bruit stable de 0 à 1 par pixel : le même avatar grenu de la même façon à chaque dessin. */
    private static double noise(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xffff) / 65535.0;
    }
}
