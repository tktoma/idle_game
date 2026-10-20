package idle.ui.atoms;

import idle.core.universe.Skin;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Grand atome de l'onglet « Atomes » : un noyau entouré d'orbes qui tournent sur des
 * couches concentriques. Il y a une orbe par atome disponible : une orbe apparaît à chaque
 * atome gagné et disparaît à chaque atome dépensé.
 *
 * <p>Les couches se remplissent de l'intérieur vers l'extérieur, avec les capacités des
 * couches électroniques du dernier élément du tableau périodique : 2, 8, 18, 32, 32, 18, 8,
 * soit 118 orbes au maximum.
 *
 * <p>Au-delà de 118 atomes (version 0.077), le grand atome ne change pas : des copies de lui,
 * complètes (noyau, couches et 118 orbes), apparaissent autour. Elles ne sont jamais plus
 * grandes que lui, ont des tailles variées et sont vues de biais, chacune sous son angle.
 * Il en vient de plus en plus à mesure que les atomes en trop grandissent, jusqu'à
 * {@link #MAX_COPIES}, où la vue est bien chargée. Les plus petites, comme plus lointaines,
 * sont plus pâles et passent derrière les grandes.
 *
 * <p>La vue occupe toute la largeur de la page ; le grand atome reste au centre, carré, à sa
 * taille d'avant ({@link #MAX_SIDE} pixels de côté au plus).
 */
final class AtomModelView extends Canvas {

    /** Nombre d'orbes que peut porter chaque couche, de la plus proche du noyau à la plus lointaine. */
    private static final int[] SHELL_CAPACITIES = {2, 8, 18, 32, 32, 18, 8};

    /** Nombre maximal d'orbes : le nombre d'éléments du tableau périodique, comme {@code Game.MAX_ATOMS}. */
    static final int MAX_ORBS = 118;

    /** Durée de l'apparition d'une nouvelle orbe, en secondes. */
    private static final double POP_SECONDS = 0.9;

    /** Côté maximal du grand atome, en pixels : celui d'avant. */
    static final double MAX_SIDE = 440;

    /** Nombre maximal de copies de l'atome autour du grand : la vue est alors bien chargée. */
    static final int MAX_COPIES = 40;

    /** Copies de plus par ordre de grandeur d'atomes en trop : 1 dès 119 atomes, toutes vers 10¹⁰. */
    private static final double COPIES_PER_DECADE = 4;

    /** Taille d'une copie, au plus, par rapport au grand atome : jamais plus grande que lui. */
    static final double MAX_COPY_SCALE = 0.7;

    private static final Color NUCLEUS = Color.web("#ffd27f");
    private static final Color SHELL = Color.web("#ffe9c2");
    private static final Color ORB = Color.web("#fff4dd");

    /**
     * Une copie de l'atome : sa place, sa taille par rapport au grand, et comment on la voit de
     * biais (l'axe {@code turn} selon lequel elle est aplatie, de {@code tilt}).
     */
    record Copy(double x, double y, double scale, double turn, double tilt, double phase) {
    }

    private int orbs = 0;
    /** Ordres de grandeur des atomes au-delà de 118 (log10 de leur nombre + 1), 0 s'il n'y en a pas. */
    private double extraDecades = 0;
    private double time = 0;
    private double sinceLastOrb = POP_SECONDS; // temps écoulé depuis l'arrivée de la dernière orbe
    /** Durée du coup de grossissement quand un atome est créé, en secondes. */
    private static final double ARRIVE_SECONDS = 0.6;
    private double sinceArrive = ARRIVE_SECONDS;

    /** Les places des copies, calculées une fois pour une taille de vue. */
    private List<Copy> layout = List.of();
    private double layoutWidth = -1;
    private double layoutHeight = -1;

    /**
     * Le skin de l'atome (version 0.089) : {@code null} pour le classique. « Noyau dense » : un noyau
     * de protons et de neutrons serrés, des couches en pointillés, des électrons bleus qui laissent
     * une traînée. « Noyau stellaire » : un petit soleil couronné de rayons, et des planètes de
     * toutes les couleurs sur leurs orbites.
     */
    private Skin skin;

    void setSkin(Skin chosen) {
        skin = chosen == Skin.ATOM_DENSE || chosen == Skin.ATOM_STELLAR ? chosen : null;
    }

    Skin skin() {
        return skin;
    }

    private static final Color[] PLANETS = {Color.web("#7fc8ff"), Color.web("#ff9f6b"), Color.web("#9be7a8"),
            Color.web("#e2c48f"), Color.web("#c9a6ff"), Color.web("#ff7a9a")};

    /** @param size côté de la vue au départ, en pixels */
    AtomModelView(double size) {
        super(size, size);
    }

    /** Fixe le nombre d'orbes (plafonné à {@link #MAX_ORBS}). Une orbe ajoutée joue son apparition. */
    void setOrbs(int count) {
        int capped = Math.max(0, Math.min(MAX_ORBS, count));
        if (capped > orbs) sinceLastOrb = 0;
        orbs = capped;
    }

    /**
     * Les atomes au-delà des 118 orbes, en ordres de grandeur : {@code log10(atomes en trop + 1)}.
     * 0 quand il n'y en a pas.
     */
    void setExtraDecades(double decades) {
        extraDecades = Double.isFinite(decades) ? Math.max(0, decades) : 0;
    }

    /** Nombre de copies de l'atome affichées autour du grand : une dès le premier atome en trop, {@link #MAX_COPIES} au plus. */
    int copies() {
        if (extraDecades <= 0) return 0;
        return (int) Math.min(MAX_COPIES, Math.max(1, Math.floor(extraDecades * COPIES_PER_DECADE)));
    }

    /** Les copies affichées, de la plus petite (au fond) à la plus grande : pour les vérifications. */
    List<Copy> shownCopies() {
        return sortedCopies(copies());
    }

    /** Un atome vient d'être créé : le grand atome grossit d'un coup puis reprend sa taille. Les copies ne bougent pas. */
    void celebrate() {
        sinceArrive = 0;
    }

    /** Avance l'animation de {@code elapsed} secondes et redessine. */
    void frame(double elapsed) {
        double dt = Math.min(elapsed, 0.1);
        time += dt;
        sinceLastOrb += dt;
        // Sans animation (réglage coupé, image figée), pas de grossissement qui resterait en plan.
        sinceArrive = dt <= 0 ? ARRIVE_SECONDS : sinceArrive + dt;

        GraphicsContext g = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, width, height);

        // Le grand atome : au centre, carré, à sa taille d'avant.
        double side = Math.min(MAX_SIDE, Math.min(width, height));
        double cx = width / 2;
        double cy = height / 2;

        // Les copies d'abord, des plus petites aux plus grandes : le grand atome passe devant toutes.
        for (Copy copy : sortedCopies(copies())) {
            double fade = 0.35 + 0.55 * copy.scale() / MAX_COPY_SCALE;
            drawAtom(g, copy.x(), copy.y(), side * copy.scale(), MAX_ORBS, copy.turn(), copy.tilt(), time + copy.phase(), fade, false);
        }
        double arrive = sinceArrive < ARRIVE_SECONDS ? 1 + 0.6 * (1 - sinceArrive / ARRIVE_SECONDS) : 1;
        drawAtom(g, cx, cy, side * arrive, orbs, 0, 1, time, 1, true);
        g.setGlobalAlpha(1);
    }

    /** Les {@code count} premières copies, triées de la plus petite à la plus grande. */
    private List<Copy> sortedCopies(int count) {
        List<Copy> all = layout(getWidth(), getHeight());
        List<Copy> shown = new ArrayList<>(all.subList(0, Math.min(count, all.size())));
        shown.sort((a, b) -> Double.compare(a.scale(), b.scale()));
        return shown;
    }

    /**
     * Place les {@link #MAX_COPIES} copies sur la vue, une fois pour toutes pour cette taille :
     * chacune tirée au hasard (toujours le même), en évitant le cœur du grand atome et en se
     * serrant peu à peu contre les autres. Les premières sont les plus grandes ; les suivantes,
     * plus petites, comblent les trous. Ajouter une copie ne déplace jamais les précédentes.
     */
    private List<Copy> layout(double width, double height) {
        if (width == layoutWidth && height == layoutHeight) return layout;
        layoutWidth = width;
        layoutHeight = height;
        double side = Math.min(MAX_SIDE, Math.min(width, height));
        double cx = width / 2;
        double cy = height / 2;
        List<Copy> copies = new ArrayList<>();
        int seed = 0;
        for (int k = 0; k < MAX_COPIES; k++) {
            double scale = MAX_COPY_SCALE * (0.3 + 0.7 * noise(k * 11 + 1)) * (1 - 0.6 * k / (double) MAX_COPIES);
            double radius = side * scale * 0.46;
            Copy placed = null;
            // On cherche une place libre ; à force d'essais, on accepte qu'elles se chevauchent davantage.
            for (int attempt = 0; attempt < 400 && placed == null; attempt++, seed++) {
                double looseness = 0.9 - 0.8 * attempt / 400.0;
                double x = width * noise(seed * 3 + 101);
                double y = height * noise(seed * 3 + 102);
                // Le cœur du grand atome reste dégagé : on le voit toujours en entier.
                if (Math.hypot(x - cx, y - cy) < side * 0.42 + radius * 0.6) continue;
                boolean free = true;
                for (Copy other : copies) {
                    double room = (radius + side * other.scale() * 0.46) * looseness;
                    if (Math.hypot(x - other.x(), y - other.y()) < room) {
                        free = false;
                        break;
                    }
                }
                if (!free) continue;
                double turn = Math.PI * noise(k * 11 + 2);
                double tilt = 0.35 + 0.65 * noise(k * 11 + 3);
                double phase = 20 * noise(k * 11 + 4);
                placed = new Copy(x, y, scale, turn, tilt, phase);
            }
            if (placed == null) {
                // Plus aucune place : la copie se glisse n'importe où, hors du cœur, pour garder le compte.
                double angle = 2 * Math.PI * noise(k * 11 + 5);
                double far = side * 0.5 + radius;
                placed = new Copy(cx + far * Math.cos(angle), cy + far * Math.sin(angle), scale,
                        Math.PI * noise(k * 11 + 2), 0.35 + 0.65 * noise(k * 11 + 3), 20 * noise(k * 11 + 4));
            }
            copies.add(placed);
        }
        layout = List.copyOf(copies);
        return layout;
    }

    /**
     * Dessine un atome : le noyau, ses couches et ses orbes, centré en ({@code cx}, {@code cy}),
     * de côté {@code size}. Vu de biais quand {@code tilt} est sous 1 : aplati de {@code tilt}
     * le long de l'axe {@code turn}, ses couches deviennent des ellipses.
     *
     * @param fade  transparence d'ensemble (1 : pleine)
     * @param main  vrai pour le grand atome : seule sa dernière orbe arrivée joue son apparition
     */
    private void drawAtom(GraphicsContext g, double cx, double cy, double size, int orbCount, double turn, double tilt,
            double clock, double fade, boolean main) {
        double nucleus = size * 0.07;
        // Les couches occupées se partagent l'espace entre le noyau et le bord. Avec peu de
        // couches, l'atome reste compact ; à partir de quatre, il occupe toute la vue.
        int shells = occupiedShells(orbCount);
        double innerRadius = nucleus * 1.6;
        double outerRadius = innerRadius + (size * 0.46 - innerRadius) * Math.min(1, shells / 4.0);
        double orbRadius = Math.max(main ? 2 : 0.7, size * 0.011);
        boolean flat = tilt >= 1;
        // L'aplatissement : tourner de -turn, écraser l'axe vertical, tourner de +turn.
        double cos = Math.cos(turn), sin = Math.sin(turn);
        double a = cos * cos + tilt * sin * sin;
        double b = cos * sin * (1 - tilt);
        double d = sin * sin + tilt * cos * cos;

        int remaining = orbCount;
        for (int shell = 0; shell < shells; shell++) {
            int onShell = Math.min(remaining, SHELL_CAPACITIES[shell]);
            remaining -= onShell;
            double radius = innerRadius + (outerRadius - innerRadius) * (shell + 1) / shells;

            g.setStroke(skin == Skin.ATOM_STELLAR ? Color.web("#ffcf6b") : skin == Skin.ATOM_DENSE ? Color.web("#7fd0ff") : SHELL);
            g.setLineWidth(1);
            g.setGlobalAlpha((skin == Skin.ATOM_STELLAR ? 0.18 : 0.3) * fade);
            if (skin == Skin.ATOM_DENSE) {
                // Des couches en pointillés.
                int dots = 24 + shell * 10;
                for (int p = 0; p < dots; p++) {
                    double t = 2 * Math.PI * p / dots + clock * 0.05;
                    double ux = radius * Math.cos(t), uy = radius * Math.sin(t);
                    g.setFill(Color.web("#7fd0ff"));
                    g.fillOval(cx + a * ux + b * uy - 0.8, cy + b * ux + d * uy - 0.8, 1.6, 1.6);
                }
            } else if (flat) {
                g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
            } else {
                int points = 48;
                double[] px = new double[points + 1];
                double[] py = new double[points + 1];
                for (int p = 0; p <= points; p++) {
                    double t = 2 * Math.PI * p / points;
                    double ux = radius * Math.cos(t), uy = radius * Math.sin(t);
                    px[p] = cx + a * ux + b * uy;
                    py[p] = cy + b * ux + d * uy;
                }
                g.strokePolyline(px, py, points + 1);
            }

            // Chaque couche tourne à sa vitesse, une sur deux dans l'autre sens.
            double direction = shell % 2 == 0 ? 1 : -1;
            double rotation = direction * clock * 0.9 / (shell + 1) + shell * 0.7;
            boolean lastShell = remaining == 0;
            for (int i = 0; i < onShell; i++) {
                double angle = rotation + 2 * Math.PI * i / onShell;
                double ux = radius * Math.cos(angle), uy = radius * Math.sin(angle);
                double x = cx + a * ux + b * uy;
                double y = cy + b * ux + d * uy;
                boolean newest = main && lastShell && i == onShell - 1 && sinceLastOrb < POP_SECONDS;
                if (newest) {
                    // La dernière orbe arrivée : un halo qui se resserre sur elle.
                    double t = sinceLastOrb / POP_SECONDS;
                    double halo = orbRadius * (1 + 5 * (1 - t));
                    g.setFill(NUCLEUS);
                    g.setGlobalAlpha(0.5 * (1 - t));
                    g.fillOval(x - halo, y - halo, halo * 2, halo * 2);
                }
                if (skin == Skin.ATOM_DENSE) {
                    // Un électron bleu, et sa traînée le long de la couche.
                    for (int k = 3; k >= 1; k--) {
                        double back = angle - direction * k * 0.06;
                        double bx = radius * Math.cos(back), by = radius * Math.sin(back);
                        g.setFill(Color.web("#7fd0ff"));
                        g.setGlobalAlpha(fade * 0.25 * (4 - k) / 3);
                        double r = orbRadius * (1 - 0.2 * k);
                        g.fillOval(cx + a * bx + b * by - r, cy + b * bx + d * by - r, r * 2, r * 2);
                    }
                    g.setFill(Color.web("#d8f1ff"));
                    g.setGlobalAlpha(fade);
                    g.fillOval(x - orbRadius, y - orbRadius, orbRadius * 2, orbRadius * 2);
                } else if (skin == Skin.ATOM_STELLAR) {
                    // Une planète, sa couleur et sa taille à elle, éclairée du côté du soleil.
                    double r = orbRadius * (1.1 + 0.5 * ((i * 7 + shell) % 3));
                    g.setFill(PLANETS[(i + shell * 3) % PLANETS.length]);
                    g.setGlobalAlpha(fade);
                    g.fillOval(x - r, y - r, r * 2, r * 2);
                    double away = Math.max(1e-9, Math.hypot(x - cx, y - cy));
                    double sx = (x - cx) / away * r * 0.45, sy = (y - cy) / away * r * 0.45;
                    g.setFill(Color.web("#0b0e14"));
                    g.setGlobalAlpha(fade * 0.55);
                    g.fillOval(x + sx - r * 0.8, y + sy - r * 0.8, r * 1.6, r * 1.6);
                    if ((i + shell) % 5 == 0) {
                        // Quelques-unes ont un anneau.
                        g.setStroke(Color.web("#e2c48f"));
                        g.setGlobalAlpha(fade * 0.8);
                        g.strokeLine(x - r * 1.8, y + r * 0.5, x + r * 1.8, y - r * 0.5);
                    }
                } else {
                    g.setFill(ORB);
                    g.setGlobalAlpha(fade);
                    g.fillOval(x - orbRadius, y - orbRadius, orbRadius * 2, orbRadius * 2);
                }
            }
        }

        if (skin == Skin.ATOM_DENSE) {
            // Le noyau dense : des protons rouges et des neutrons gris serrés en grappe.
            int nucleons = 19;
            double ball = nucleus * 0.42;
            for (int k = nucleons - 1; k >= 0; k--) {
                double r = nucleus * 0.95 * Math.sqrt(k / (double) nucleons);
                double t = k * 2.39996 + clock * 0.2;
                double x = cx + r * Math.cos(t), y = cy + r * Math.sin(t) * (flat ? 1 : 0.9);
                g.setFill(k % 2 == 0 ? Color.web("#ff6b5b") : Color.web("#c9ced6"));
                g.setGlobalAlpha(fade);
                g.fillOval(x - ball, y - ball, ball * 2, ball * 2);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(fade * 0.5);
                g.fillOval(x - ball * 0.55, y - ball * 0.6, ball * 0.6, ball * 0.6);
            }
            return;
        }
        if (skin == Skin.ATOM_STELLAR) {
            // Le noyau stellaire : un soleil, sa couronne de rayons qui tournent et ses éruptions.
            int rays = 14;
            g.setStroke(Color.web("#ffb23f"));
            g.setLineWidth(Math.max(1, size * 0.004));
            for (int k = 0; k < rays; k++) {
                double t = clock * 0.3 + 2 * Math.PI * k / rays;
                double length = nucleus * (1.6 + 0.5 * Math.sin(clock * 2 + k * 1.3));
                g.setGlobalAlpha(0.55 * fade);
                g.strokeLine(cx + nucleus * 1.05 * Math.cos(t), cy + nucleus * 1.05 * Math.sin(t),
                        cx + length * Math.cos(t), cy + length * Math.sin(t));
            }
            g.setFill(Color.web("#ff8a2a"));
            g.setGlobalAlpha(0.25 * fade);
            double corona = nucleus * 1.45;
            g.fillOval(cx - corona, cy - corona, corona * 2, corona * 2);
            g.setFill(Color.web("#ffd27f"));
            g.setGlobalAlpha(fade);
            g.fillOval(cx - nucleus, cy - nucleus, nucleus * 2, nucleus * 2);
            g.setFill(Color.web("#fff6d8"));
            g.fillOval(cx - nucleus * 0.55, cy - nucleus * 0.55, nucleus * 1.1, nucleus * 1.1);
            return;
        }
        // Le noyau, avec un halo qui respire doucement.
        double halo = nucleus * (1.5 + 0.12 * Math.sin(clock * 3));
        g.setFill(NUCLEUS);
        g.setGlobalAlpha(0.2 * fade);
        g.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
        g.setGlobalAlpha(fade);
        g.fillOval(cx - nucleus, cy - nucleus, nucleus * 2, nucleus * 2);
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine : les copies ne bougent pas d'une image à l'autre. */
    private static double noise(int seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }

    /** Nombre de couches qui portent au moins une orbe, pour {@code count} orbes. */
    private static int occupiedShells(int count) {
        int remaining = count;
        int shells = 0;
        while (remaining > 0 && shells < SHELL_CAPACITIES.length) {
            remaining -= SHELL_CAPACITIES[shells];
            shells++;
        }
        return shells;
    }
}
