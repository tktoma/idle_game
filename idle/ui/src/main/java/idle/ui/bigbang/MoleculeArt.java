package idle.ui.bigbang;

import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.molecules.Molecule;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.atoms.PeriodicTablePage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
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
 *
 * <p>Version 0.188 : les quatre skins des molécules ({@link SkinVisual#MOLECULE}) redessinent les
 * mêmes positions autrement, sans toucher à la mise en place ni à l'emprise que lisent les vues.
 * « Boules pleines » ({@link Skin#MOLECULE_SPACE_FILLING}) gonfle chaque atome en une sphère de
 * rayon ×{@link #FILLING} qui recouvre ses voisines, ombrée d'un dégradé clair en haut à gauche et
 * cernée de sombre, sans bâtons, dessinées du cœur vers la périphérie ; l'échelle est réduite pour
 * que l'ensemble ne déborde pas de plus de {@link #OVERFLOW} du cercle demandé. « Traits »
 * ({@link Skin#MOLECULE_SKELETON}) ne garde que les liaisons, en traits fins de la couleur
 * dominante (le modèle ne connaît pas les liaisons doubles : elles sont toutes simples), avec aux
 * sommets la lettre du symbole en police à chasse fixe, blanche à ombre sombre, dès
 * {@link #LETTERS_FROM} pixels de rayon, et un petit point en dessous. « Perles »
 * ({@link Skin#MOLECULE_PEARLS}) fait de chaque atome une perle nacrée (couleur très éclaircie,
 * reflet blanc décalé, bord irisé de deux teintes voisines) sur une légère ombre portée, reliée
 * par un fil d'un pixel. « Lego » ({@link Skin#MOLECULE_BRICKS}) pose à chaque atome une brique
 * carrée vue de trois quarts, face avant de la couleur saturée, dessus plus clair avec deux tenons,
 * côté droit plus sombre, cernée de noir ; les briques ne tournent pas et se touchent, la plus liée
 * au centre, dessinées de l'arrière-plan vers l'avant. Les dégradés et couleurs dérivées sont
 * calculés une fois par molécule, par couleur d'atome, à la mise en place ({@link Looks}) : rien
 * n'est alloué au dessin au-delà de ce que fait le Classique.
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

    /** Boules pleines : ce par quoi le rayon d'un atome est multiplié, un rayon de van der Waals approché. */
    static final double FILLING = 1.6;
    /** Boules pleines et Lego : ce dont le dessin peut déborder du cercle demandé, au plus (1.15 = 15 %). */
    static final double OVERFLOW = 1.15;
    /** Traits : à partir de ce rayon demandé, en pixels, les sommets portent leur lettre plutôt qu'un point. */
    static final double LETTERS_FROM = 14;
    /** Traits : la taille de la lettre, en part du rayon demandé, et son plafond en pixels. */
    private static final double LETTER_SHARE = 0.3;
    private static final double LETTER_MAX = 16;
    /** Perles : le fil qui relie les perles, et l'ombre sous chacune. */
    private static final Color THREAD = Color.web("#d8d2c4");
    private static final Color PEARL_SHADOW = Color.rgb(0, 0, 0, 0.22);
    /** Lego : la profondeur des faces du dessus et de droite, en part du côté de la face avant. */
    private static final double BRICK_DEPTH = 0.35;
    /** Lego : le côté d'une brique, en pas du pavage, pour un atome de ce rayon (bornée à un pas : les voisines se touchent). */
    private static final double BRICK_SIDE_PER_RADIUS = 2.8;
    /** Lego : où sont les deux tenons sur la largeur du dessus. */
    private static final double[] STUDS = {0.3, 0.7};

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
     * Ce que les skins ont précalculé pour une molécule : les dégradés et couleurs dérivées de chaque
     * couleur d'atome ({@code palette[atome]} dit laquelle), les emprises des dessins qui dépassent
     * les boules, et les tampons que le dessin réutilise d'une image à l'autre (positions à l'écran,
     * ordre de profondeur) pour ne rien allouer.
     */
    static final class Looks {
        final int[] palette;
        final RadialGradient[] sphere;
        final Color[] sphereEdge;
        final RadialGradient[] pearl;
        final Color[] brickFront;
        final Color[] brickTop;
        final Color[] brickSide;
        final Color[] brickStud;
        /** L'emprise des boules pleines et des briques, en pas du pavage : plus grande que celle du Classique. */
        final double filledExtent;
        final double brickExtent;
        final double[] screenX;
        final double[] screenY;
        final int[] order;
        /** Les quatre sommets d'une face de brique, repris pour chaque face. */
        final double[] quadX = new double[4];
        final double[] quadY = new double[4];

        Looks(List<Atom> atoms) {
            Map<Color, Integer> indexes = new HashMap<>();
            palette = new int[atoms.size()];
            List<Color> colors = new ArrayList<>();
            double filled = 0;
            double bricks = 0;
            for (int index = 0; index < atoms.size(); index++) {
                Atom atom = atoms.get(index);
                Integer known = indexes.get(atom.color());
                if (known == null) {
                    known = colors.size();
                    colors.add(atom.color());
                    indexes.put(atom.color(), known);
                }
                palette[index] = known;
                double away = Math.hypot(atom.x(), atom.y());
                filled = Math.max(filled, away + atom.radius() * FILLING);
                // Une brique vue de trois quarts tient dans le carré de son côté plus sa profondeur : sa demi-diagonale.
                double side = brickSide(atom);
                bricks = Math.max(bricks, away + side * (1 + BRICK_DEPTH) * Math.sqrt(2) / 2);
            }
            filledExtent = filled;
            brickExtent = bricks;
            sphere = new RadialGradient[colors.size()];
            sphereEdge = new Color[colors.size()];
            pearl = new RadialGradient[colors.size()];
            brickFront = new Color[colors.size()];
            brickTop = new Color[colors.size()];
            brickSide = new Color[colors.size()];
            brickStud = new Color[colors.size()];
            for (int index = 0; index < colors.size(); index++) {
                Color color = colors.get(index);
                sphere[index] = new RadialGradient(0, 0, 0.35, 0.35, 0.72, true, CycleMethod.NO_CYCLE,
                        new Stop(0, color.interpolate(Color.WHITE, 0.55)), new Stop(0.55, color),
                        new Stop(1, color.deriveColor(0, 1, 0.55, 1)));
                sphereEdge[index] = color.deriveColor(0, 1, 0.4, 1);
                Color nacre = color.interpolate(Color.WHITE, 0.7);
                pearl[index] = new RadialGradient(0, 0, 0.38, 0.34, 0.78, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.WHITE), new Stop(0.4, nacre),
                        new Stop(0.82, color.deriveColor(-25, 1, 1, 1).interpolate(Color.WHITE, 0.45)),
                        new Stop(1, color.deriveColor(25, 1, 1, 1).interpolate(Color.WHITE, 0.4)));
                Color vivid = color.deriveColor(0, 1.3, 1, 1);
                brickFront[index] = vivid;
                brickTop[index] = vivid.interpolate(Color.WHITE, 0.3);
                brickSide[index] = vivid.deriveColor(0, 1, 0.62, 1);
                brickStud[index] = vivid.interpolate(Color.WHITE, 0.45);
            }
            screenX = new double[atoms.size()];
            screenY = new double[atoms.size()];
            order = new int[atoms.size()];
        }

        /** Le côté de la brique d'un atome, en pas du pavage. */
        static double brickSide(Atom atom) {
            return Math.min(1, atom.radius() * BRICK_SIDE_PER_RADIUS);
        }
    }

    /**
     * Une molécule mise en place : ses atomes centrés sur l'origine, ses liaisons, et le rayon du
     * cercle qui la contient, en pas du pavage ; {@code looks} est ce que les skins y ont précalculé.
     */
    record Shape(List<Atom> atoms, List<Bond> bonds, double extent, Color dominant, Looks looks) {

        /**
         * Dessine la molécule centrée en ({@code cx}, {@code cy}), dans un cercle de {@code radius}
         * pixels, tournée de {@code angle} radians, en boules et bâtons : le dessin classique.
         */
        void draw(GraphicsContext g, double cx, double cy, double radius, double angle) {
            draw(g, cx, cy, radius, angle, null);
        }

        /**
         * Dessine la molécule centrée en ({@code cx}, {@code cy}), dans un cercle de {@code radius}
         * pixels, tournée de {@code angle} radians, dans le skin des molécules {@code skin}
         * ({@code null} ou un skin d'un autre visuel : le Classique). Trop petite pour qu'on y
         * distingue quoi que ce soit, elle se réduit à un point de sa couleur dominante, quel que
         * soit le skin.
         */
        void draw(GraphicsContext g, double cx, double cy, double radius, double angle, Skin skin) {
            double scale = radius / extent;
            if (scale * 0.4 < 0.7) {
                g.setFill(dominant);
                double dot = Math.max(1, radius);
                g.fillOval(cx - dot, cy - dot, 2 * dot, 2 * dot);
                return;
            }
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            if (skin != null && skin.visual() == SkinVisual.MOLECULE) {
                switch (skin) {
                    case MOLECULE_SPACE_FILLING -> drawFilled(g, cx, cy, Math.min(scale, radius * OVERFLOW / looks.filledExtent), cos, sin);
                    case MOLECULE_SKELETON -> drawSkeleton(g, cx, cy, radius, scale, cos, sin);
                    case MOLECULE_PEARLS -> drawPearls(g, cx, cy, scale, cos, sin);
                    case MOLECULE_BRICKS -> drawBricks(g, cx, cy, Math.min(scale, radius * OVERFLOW / looks.brickExtent), cos, sin);
                    default -> drawClassic(g, cx, cy, scale, cos, sin);
                }
                return;
            }
            drawClassic(g, cx, cy, scale, cos, sin);
        }

        /** Le Classique : les bâtons, puis les boules, chacune avec son symbole si elle est assez grande. */
        private void drawClassic(GraphicsContext g, double cx, double cy, double scale, double cos, double sin) {
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

        /** Place les atomes à l'écran, tournés et mis à l'échelle, dans les tampons de {@link Looks}. */
        private void project(double cx, double cy, double scale, double cos, double sin) {
            for (int index = 0; index < atoms.size(); index++) {
                Atom atom = atoms.get(index);
                looks.screenX[index] = cx + scale * (atom.x() * cos - atom.y() * sin);
                looks.screenY[index] = cy + scale * (atom.x() * sin + atom.y() * cos);
            }
        }

        /** Boules pleines : des sphères gonflées qui se recouvrent, du cœur de la molécule vers sa périphérie. */
        private void drawFilled(GraphicsContext g, double cx, double cy, double scale, double cos, double sin) {
            project(cx, cy, scale, cos, sin);
            g.setLineWidth(Math.max(1, scale * 0.06));
            for (int index = 0; index < atoms.size(); index++) {
                double r = Math.max(0.8, scale * atoms.get(index).radius() * FILLING);
                double x = looks.screenX[index];
                double y = looks.screenY[index];
                g.setFill(looks.sphere[looks.palette[index]]);
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                g.setStroke(looks.sphereEdge[looks.palette[index]]);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
            }
        }

        /** Traits : les liaisons seules, et à chaque sommet une lettre ou un point. */
        private void drawSkeleton(GraphicsContext g, double cx, double cy, double radius, double scale, double cos, double sin) {
            project(cx, cy, scale, cos, sin);
            g.setStroke(dominant);
            g.setLineWidth(Math.max(1.5, Math.min(2, scale * 0.12)));
            for (Bond bond : bonds) {
                g.strokeLine(looks.screenX[bond.from()], looks.screenY[bond.from()], looks.screenX[bond.to()], looks.screenY[bond.to()]);
            }
            if (radius >= LETTERS_FROM) {
                double size = Math.min(LETTER_MAX, radius * LETTER_SHARE);
                g.setFont(mono(size));
                g.setTextAlign(TextAlignment.CENTER);
                for (int index = 0; index < atoms.size(); index++) {
                    String symbol = atoms.get(index).symbol();
                    double x = looks.screenX[index];
                    double y = looks.screenY[index] + size * 0.36;
                    g.setFill(INK);
                    g.fillText(symbol, x + 1, y + 1);
                    g.setFill(Color.WHITE);
                    g.fillText(symbol, x, y);
                }
            } else {
                g.setFill(dominant);
                double dot = Math.max(1, scale * 0.1);
                for (int index = 0; index < atoms.size(); index++) {
                    g.fillOval(looks.screenX[index] - dot, looks.screenY[index] - dot, 2 * dot, 2 * dot);
                }
            }
        }

        /** Perles : les ombres, le fil, puis les perles nacrées. */
        private void drawPearls(GraphicsContext g, double cx, double cy, double scale, double cos, double sin) {
            project(cx, cy, scale, cos, sin);
            g.setFill(PEARL_SHADOW);
            for (int index = 0; index < atoms.size(); index++) {
                double r = Math.max(0.8, scale * atoms.get(index).radius());
                g.fillOval(looks.screenX[index] - r, looks.screenY[index] - r * 0.7, 2 * r, 2 * r);
            }
            g.setStroke(THREAD);
            g.setLineWidth(1);
            for (Bond bond : bonds) {
                g.strokeLine(looks.screenX[bond.from()], looks.screenY[bond.from()], looks.screenX[bond.to()], looks.screenY[bond.to()]);
            }
            for (int index = 0; index < atoms.size(); index++) {
                double r = Math.max(0.8, scale * atoms.get(index).radius());
                g.setFill(looks.pearl[looks.palette[index]]);
                g.fillOval(looks.screenX[index] - r, looks.screenY[index] - r, 2 * r, 2 * r);
            }
        }

        /**
         * Lego : une brique par atome, vue de trois quarts et sans rotation, dessinées de l'arrière-plan
         * (en haut à gauche) vers l'avant (en bas à droite), ce qui dit la face qui en cache une autre.
         */
        private void drawBricks(GraphicsContext g, double cx, double cy, double scale, double cos, double sin) {
            project(cx, cy, scale, cos, sin);
            // Un tri par insertion sur le tampon d'ordre : quelques dizaines d'atomes au plus, et rien d'alloué.
            int[] order = looks.order;
            for (int index = 0; index < order.length; index++) {
                int atom = index;
                int slot = index;
                while (slot > 0 && depth(order[slot - 1]) > depth(atom)) {
                    order[slot] = order[slot - 1];
                    slot--;
                }
                order[slot] = atom;
            }
            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            for (int rank = 0; rank < order.length; rank++) {
                int index = order[rank];
                int tone = looks.palette[index];
                double side = scale * Looks.brickSide(atoms.get(index));
                double depth = side * BRICK_DEPTH;
                double left = looks.screenX[index] - (side + depth) / 2;
                double top = looks.screenY[index] - (side - depth) / 2;
                // La face avant, puis le dessus et le côté droit, décalés de la profondeur vers le haut et la droite.
                g.setFill(looks.brickFront[tone]);
                g.fillRect(left, top, side, side);
                g.strokeRect(left, top, side, side);
                face(g, looks.brickTop[tone], left, top, left + depth, top - depth, left + side + depth, top - depth, left + side, top);
                face(g, looks.brickSide[tone], left + side, top, left + side + depth, top - depth, left + side + depth, top + side - depth,
                        left + side, top + side);
                // Deux tenons ronds sur le dessus, aplatis par la perspective.
                if (side >= 6) {
                    double studX = side * 0.13;
                    double studY = studX * 0.5;
                    g.setFill(looks.brickStud[tone]);
                    for (double share : STUDS) {
                        double x = left + depth / 2 + side * share;
                        double y = top - depth / 2;
                        g.fillOval(x - studX, y - studY, 2 * studX, 2 * studY);
                        g.strokeOval(x - studX, y - studY, 2 * studX, 2 * studY);
                    }
                }
            }
        }

        /** Une face de brique à quatre sommets, pleine et cernée, dans les tampons de {@link Looks}. */
        private void face(GraphicsContext g, Color fill, double x0, double y0, double x1, double y1, double x2, double y2,
                          double x3, double y3) {
            double[] xs = looks.quadX;
            double[] ys = looks.quadY;
            xs[0] = x0; xs[1] = x1; xs[2] = x2; xs[3] = x3;
            ys[0] = y0; ys[1] = y1; ys[2] = y2; ys[3] = y3;
            g.setFill(fill);
            g.fillPolygon(xs, ys, 4);
            g.strokePolygon(xs, ys, 4);
        }

        /** La profondeur d'un atome à l'écran pour les briques : plus il est bas et à droite, plus il est devant. */
        private double depth(int index) {
            return looks.screenY[index] + 0.5 * looks.screenX[index];
        }
    }

    /**
     * La police des symboles à cette taille. À un instant donné, tous les atomes d'une même sorte
     * ont la même taille à l'écran : une police est donc demandée une fois, puis reprise.
     */
    private static Font label(double size) {
        return font(LABELS, "System", size);
    }

    /** La police à chasse fixe des lettres du skin « Traits », à cette taille, reprise de même. */
    private static Font mono(double size) {
        return font(MONO, "Monospaced", size);
    }

    private static Font font(Map<Double, Font> cache, String family, double size) {
        Double key = size;
        Font font = cache.get(key);
        if (font == null) {
            if (cache.size() >= 256) cache.clear();
            font = Font.font(family, FontWeight.BOLD, size);
            cache.put(key, font);
        }
        return font;
    }

    private static final Map<Double, Font> LABELS = new HashMap<>();
    private static final Map<Double, Font> MONO = new HashMap<>();

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
        List<Atom> atoms = List.copyOf(centered);
        return new Shape(atoms, List.copyOf(bonds), Math.max(0.5, extent), dominant, new Looks(atoms));
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
