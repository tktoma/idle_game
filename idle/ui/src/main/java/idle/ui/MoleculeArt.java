package idle.ui;

import idle.core.Element;
import idle.core.Molecule;
import idle.core.PeriodicTable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Le dessin d'une molécule en boules et bâtons, tiré de sa seule formule.
 *
 * <p>Ce n'est pas sa vraie géométrie : le jeu ne connaît que le nombre d'atomes de chaque
 * élément. Les atomes sont posés sur un pavage en triangles, en spirale depuis le centre : d'abord
 * les éléments les moins nombreux et les plus lourds (le soufre de H₂SO₄ se retrouve au milieu),
 * puis les autres, et les hydrogènes en dernier, à la périphérie. Deux atomes voisins sur le
 * pavage sont reliés ; un hydrogène ne l'est qu'à un seul voisin. Le résultat se lit comme une
 * molécule, et deux formules différentes donnent deux dessins différents.
 *
 * <p>Les couleurs sont celles des modèles de chimie pour les éléments courants (hydrogène blanc,
 * carbone gris, azote bleu, oxygène rouge, soufre jaune, halogènes verts) ; les autres prennent la
 * couleur de leur famille dans le tableau périodique. Un atome assez grand porte son symbole,
 * sauf le carbone et l'hydrogène.
 *
 * <p>La mise en place d'une molécule est calculée une fois ({@link #of(Molecule)}), puis dessinée
 * à l'échelle, à l'endroit et sous l'angle voulus ({@link Shape#draw}).
 */
final class MoleculeArt {

    /** Un atome placé : sa position et son rayon, en pas du pavage, sa couleur et son symbole. */
    record Atom(double x, double y, double radius, Color color, String symbol, boolean labelled) {}

    /** Une liaison entre deux atomes, par leurs rangs dans {@link Shape#atoms()}. */
    record Bond(int from, int to) {}

    private static final Color BOND = Color.web("#6b7a8c");
    private static final Color INK = Color.web("#10151f");
    private static final double HYDROGEN_RADIUS = 0.24;
    /** En dessous de ce rayon à l'écran, en pixels, un atome ne porte pas son symbole. */
    private static final double LABEL_FROM = 6.5;
    /** En dessous de ce rayon à l'écran, les liaisons ne se voient plus : on ne dessine que les atomes. */
    private static final double BONDS_FROM = 1.6;

    /** Les couleurs des modèles moléculaires, pour les éléments qu'on y croise le plus. */
    private static final Map<String, String> MODEL_COLORS = Map.ofEntries(
            Map.entry("H", "#f4f6f8"), Map.entry("C", "#8b95a3"), Map.entry("N", "#6f9bff"), Map.entry("O", "#ff6b5e"),
            Map.entry("F", "#b6ee7a"), Map.entry("Cl", "#5fd35f"), Map.entry("Br", "#c9694b"), Map.entry("I", "#a878e0"),
            Map.entry("S", "#ffd84a"), Map.entry("P", "#ffa040"), Map.entry("B", "#f2b8a2"), Map.entry("Si", "#d9c08a"),
            Map.entry("Se", "#ffb85c"));

    private static final Map<String, Shape> SHAPES = new HashMap<>();
    /** Les places du pavage, en spirale depuis le centre : assez pour la plus grosse molécule du catalogue. */
    private static final List<double[]> SITES = sites(6);

    /**
     * Une molécule mise en place : ses atomes centrés sur l'origine, ses liaisons, et le rayon du
     * cercle qui la contient, en pas du pavage.
     */
    record Shape(List<Atom> atoms, List<Bond> bonds, double extent, Color dominant) {

        /**
         * Dessine la molécule centrée en ({@code cx}, {@code cy}), dans un cercle de {@code radius}
         * pixels, tournée de {@code angle} radians. Trop petite pour qu'on y distingue quoi que ce
         * soit, elle se réduit à un point de sa couleur dominante.
         */
        void draw(GraphicsContext g, double cx, double cy, double radius, double angle) {
            double scale = radius / extent;
            if (scale * 0.4 < 0.7) {
                g.setFill(dominant);
                double dot = Math.max(1, radius);
                g.fillOval(cx - dot, cy - dot, 2 * dot, 2 * dot);
                return;
            }
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            if (scale * 0.4 >= BONDS_FROM) {
                g.setStroke(BOND);
                g.setLineWidth(Math.max(1, scale * 0.16));
                for (Bond bond : bonds) {
                    Atom from = atoms.get(bond.from());
                    Atom to = atoms.get(bond.to());
                    g.strokeLine(cx + scale * (from.x() * cos - from.y() * sin), cy + scale * (from.x() * sin + from.y() * cos),
                            cx + scale * (to.x() * cos - to.y() * sin), cy + scale * (to.x() * sin + to.y() * cos));
                }
            }
            for (Atom atom : atoms) {
                double x = cx + scale * (atom.x() * cos - atom.y() * sin);
                double y = cy + scale * (atom.x() * sin + atom.y() * cos);
                double r = Math.max(0.8, scale * atom.radius());
                g.setFill(atom.color());
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                if (atom.labelled() && r >= LABEL_FROM) {
                    double size = Math.min(13, r * (atom.symbol().length() > 1 ? 0.95 : 1.15));
                    g.setFill(INK);
                    g.setFont(label(size));
                    g.setTextAlign(TextAlignment.CENTER);
                    g.fillText(atom.symbol(), x, y + size * 0.36);
                }
            }
        }
    }

    /**
     * La police des symboles à cette taille. À un instant donné, tous les atomes d'une même sorte
     * ont la même taille à l'écran : une police est donc demandée une fois, puis reprise.
     */
    private static Font label(double size) {
        Double key = size;
        Font font = LABELS.get(key);
        if (font == null) {
            if (LABELS.size() >= 256) LABELS.clear();
            font = Font.font("System", FontWeight.BOLD, size);
            LABELS.put(key, font);
        }
        return font;
    }

    private static final Map<Double, Font> LABELS = new HashMap<>();

    /** La mise en place d'une molécule, calculée à la première demande. */
    static Shape of(Molecule molecule) {
        return SHAPES.computeIfAbsent(molecule.id(), id -> layout(molecule));
    }

    private static Shape layout(Molecule molecule) {
        // Les éléments dans l'ordre où ils prennent place : les hydrogènes en dernier, et avant eux
        // les moins nombreux d'abord, les plus lourds en premier à nombre égal.
        List<Map.Entry<Integer, Integer>> order = new ArrayList<>(molecule.recipe().entrySet());
        boolean onlyHydrogen = order.size() == 1 && order.get(0).getKey() == 1;
        order.sort(Comparator.<Map.Entry<Integer, Integer>>comparingInt(atom -> atom.getKey() == 1 && !onlyHydrogen ? 1 : 0)
                .thenComparingInt(Map.Entry::getValue)
                .thenComparingInt(atom -> -atom.getKey()));
        List<Atom> placed = new ArrayList<>();
        Color dominant = null;
        for (Map.Entry<Integer, Integer> entry : order) {
            Element element = PeriodicTable.element(entry.getKey());
            boolean hydrogen = element.number() == 1 && !onlyHydrogen;
            Color color = color(element);
            if (dominant == null) dominant = color;
            double radius = hydrogen ? HYDROGEN_RADIUS : 0.32 + 0.12 * Math.min(1, element.number() / 60.0);
            boolean labelled = element.number() != 1 && element.number() != 6;
            for (int count = 0; count < entry.getValue() && placed.size() < SITES.size(); count++) {
                double[] site = SITES.get(placed.size());
                placed.add(new Atom(site[0], site[1], radius, color, element.symbol(), labelled));
            }
        }
        // Les liaisons : entre voisins du pavage. Un hydrogène n'en a qu'une, vers l'atome le plus proche posé avant lui.
        List<Bond> bonds = new ArrayList<>();
        for (int index = 1; index < placed.size(); index++) {
            Atom atom = placed.get(index);
            boolean hydrogen = atom.radius() == HYDROGEN_RADIUS;
            int nearest = -1;
            double nearestDistance = Double.MAX_VALUE;
            for (int other = 0; other < index; other++) {
                Atom neighbour = placed.get(other);
                double distance = Math.hypot(atom.x() - neighbour.x(), atom.y() - neighbour.y());
                boolean otherHydrogen = neighbour.radius() == HYDROGEN_RADIUS;
                if (!hydrogen && !otherHydrogen && distance < 1.01) bonds.add(new Bond(other, index));
                // Un hydrogène s'accroche de préférence à un atome lourd.
                double rank = distance + (otherHydrogen ? 10 : 0);
                if (rank < nearestDistance) {
                    nearestDistance = rank;
                    nearest = other;
                }
            }
            if (hydrogen && nearest >= 0) bonds.add(new Bond(nearest, index));
        }
        // Centrée sur le milieu de ses atomes, pour tourner sur elle-même sans se promener.
        double centerX = placed.stream().mapToDouble(Atom::x).average().orElse(0);
        double centerY = placed.stream().mapToDouble(Atom::y).average().orElse(0);
        List<Atom> centered = new ArrayList<>();
        double extent = 0;
        for (Atom atom : placed) {
            Atom moved = new Atom(atom.x() - centerX, atom.y() - centerY, atom.radius(), atom.color(), atom.symbol(), atom.labelled());
            centered.add(moved);
            extent = Math.max(extent, Math.hypot(moved.x(), moved.y()) + moved.radius());
        }
        return new Shape(List.copyOf(centered), List.copyOf(bonds), Math.max(0.5, extent), dominant);
    }

    /** La couleur d'un élément : celle des modèles moléculaires s'il en a une, sinon celle de sa famille. */
    static Color color(Element element) {
        String model = MODEL_COLORS.get(element.symbol());
        return Color.web(model != null ? model : PeriodicTablePage.familyColor(element.category()));
    }

    /**
     * Les places d'un pavage en triangles, en spirale : le centre, puis les six voisins, puis la
     * couronne suivante, jusqu'à {@code rings} couronnes. Le pas du pavage vaut 1.
     */
    private static List<double[]> sites(int rings) {
        // Les six directions du pavage, en coordonnées obliques (colonne, rangée).
        int[][] directions = {{-1, 1}, {-1, 0}, {0, -1}, {1, -1}, {1, 0}, {0, 1}};
        List<double[]> sites = new ArrayList<>();
        sites.add(new double[] {0, 0});
        for (int ring = 1; ring <= rings; ring++) {
            int column = ring;
            int row = 0;
            for (int[] direction : directions) {
                for (int step = 0; step < ring; step++) {
                    sites.add(new double[] {column + row / 2.0, row * Math.sqrt(3) / 2});
                    column += direction[0];
                    row += direction[1];
                }
            }
        }
        return sites;
    }

    private MoleculeArt() {}
}
