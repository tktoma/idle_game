package idle.ui.atoms;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les cinq skins de l'atome de la version 0.191, dessinés à côté de {@link AtomModelView} pour le
 * garder lisible. Même géométrie que le classique (un noyau, les couches 2, 8, 18, 32, 32, 18, 8 qui
 * tournent chacune à sa vitesse, un atome vu de biais pour les copies), autres formes :
 * <ul>
 * <li>« Modèle de Bohr au tableau noir » : une ardoise vert sombre, des cercles et des ronds de craie
 * blanche un peu tremblés, des « + » dans le noyau et des « − » dans les électrons ;</li>
 * <li>« Nuage de probabilité » : pas d'orbites, chaque électron est un nuage de points flous qui
 * respire ;</li>
 * <li>« Système à anneaux » : un noyau en planète à bandes, des anneaux inclinés où roulent les
 * électrons comme des cailloux de glace ;</li>
 * <li>« Horlogerie » : les protons et les neutrons en roues dentées de laiton qui s'engrènent et
 * tournent, les électrons en billes d'acier sur des roues ;</li>
 * <li>« Vitrail » : des secteurs de verre coloré cernés de plomb noir, les électrons en perles de
 * verre, un noyau en rosace.</li>
 * </ul>
 */
final class AtomSkinArt {

    private static final Color CHALK = Color.web("#f4f1e6");
    private static final Color SLATE = Color.web("#1d2a24");
    private static final Color CLOUD = Color.web("#ffe9c2");
    private static final Color BRASS = Color.web("#c9a24a");
    private static final Color BRASS_DARK = Color.web("#6b5218");
    private static final Color LEAD = Color.web("#0a0a0c");
    /** Les verres du vitrail : bleu roi, rubis, émeraude, ambre, violet, cyan. */
    private static final Color[] GLASS = {Color.web("#2a4fcf"), Color.web("#c62a3a"), Color.web("#2a9a5a"),
        Color.web("#e2a52a"), Color.web("#7a3ab8"), Color.web("#2aa5c6")};

    private AtomSkinArt() {
    }

    /** Vrai pour les skins de l'atome dessinés ici : ceux de la version 0.191. */
    static boolean draws(Skin skin) {
        return skin == Skin.ATOM_CHALK || skin == Skin.ATOM_CLOUD || skin == Skin.ATOM_RINGS
                || skin == Skin.ATOM_GEARS || skin == Skin.ATOM_STAINED;
    }

    /** Le fond du tableau noir : une ardoise vert sombre, quelques traces d'éponge. */
    static void slate(GraphicsContext g, double width, double height) {
        g.setFill(SLATE);
        g.fillRect(0, 0, width, height);
        g.setFill(Color.color(1, 1, 1, 0.035));
        for (int k = 0; k < 6; k++) {
            double x = width * AtomModelView.noise(k * 3 + 300), y = height * AtomModelView.noise(k * 3 + 301);
            double w = width * (0.2 + 0.3 * AtomModelView.noise(k * 3 + 302)), h = w * 0.3;
            g.fillOval(x - w / 2, y - h / 2, w, h);
        }
    }

    /**
     * La géométrie d'un atome vu de biais : son centre, et l'aplatissement de {@code tilt} le long de
     * l'axe {@code turn}, appliqué par {@link #x} et {@link #y} à un point du plan de l'atome.
     */
    private record Frame(double cx, double cy, double a, double b, double d) {
        static Frame of(double cx, double cy, double turn, double tilt) {
            double cos = Math.cos(turn), sin = Math.sin(turn);
            return new Frame(cx, cy, cos * cos + tilt * sin * sin, cos * sin * (1 - tilt), sin * sin + tilt * cos * cos);
        }

        double x(double ux, double uy) {
            return cx + a * ux + b * uy;
        }

        double y(double ux, double uy) {
            return cy + b * ux + d * uy;
        }

        /** Le cercle de rayon {@code r} du plan de l'atome, en {@code points} points, fermé. */
        double[][] circle(double r, int points, double jitter, int seed) {
            double[] px = new double[points + 1], py = new double[points + 1];
            for (int p = 0; p <= points; p++) {
                double t = 2 * Math.PI * p / points;
                double rr = r * (1 + jitter * (AtomModelView.noise(seed + (p % points) * 7) - 0.5));
                px[p] = x(rr * Math.cos(t), rr * Math.sin(t));
                py[p] = y(rr * Math.cos(t), rr * Math.sin(t));
            }
            return new double[][] {px, py};
        }
    }

    /**
     * Dessine un atome sous ce skin.
     *
     * @param size        le côté de l'atome, en pixels
     * @param orbCount    le nombre d'électrons
     * @param clock       le temps de cet atome, en secondes
     * @param fade        la transparence d'ensemble (1 : pleine)
     * @param main        vrai pour le grand atome : sa dernière orbe arrivée joue son apparition
     * @param sinceLastOrb le temps écoulé depuis l'arrivée de la dernière orbe, en secondes
     */
    static void draw(Skin skin, GraphicsContext g, double cx, double cy, double size, int orbCount, double turn, double tilt,
            double clock, double fade, boolean main, double sinceLastOrb) {
        Frame f = Frame.of(cx, cy, turn, tilt);
        double nucleus = size * 0.07;
        int shells = AtomModelView.occupiedShells(orbCount);
        double innerRadius = nucleus * 1.6;
        double outerRadius = innerRadius + (size * 0.46 - innerRadius) * Math.min(1, shells / 4.0);
        double orbRadius = Math.max(main ? 2 : 0.7, size * 0.011);
        if (skin == Skin.ATOM_STAINED) panels(g, f, nucleus, innerRadius, outerRadius, shells, fade);
        int remaining = orbCount;
        for (int shell = 0; shell < shells; shell++) {
            int onShell = Math.min(remaining, AtomModelView.SHELL_CAPACITIES[shell]);
            remaining -= onShell;
            double radius = innerRadius + (outerRadius - innerRadius) * (shell + 1) / shells;
            double direction = shell % 2 == 0 ? 1 : -1;
            double rotation = direction * clock * 0.9 / (shell + 1) + shell * 0.7;
            double ringTilt = 0.35 + 0.2 * Math.sin(shell * 1.9);   // l'inclinaison de l'anneau, pour le système à anneaux
            switch (skin) {
                case ATOM_CHALK -> chalkOrbit(g, f, radius, shell, fade);
                case ATOM_RINGS -> ring(g, f, radius, ringTilt, shell, fade);
                case ATOM_GEARS -> wheel(g, f, radius, shell, clock, fade);
                case ATOM_STAINED -> leadCircle(g, f, radius, fade);
                default -> { }
            }
            boolean lastShell = remaining == 0;
            for (int i = 0; i < onShell; i++) {
                double angle = rotation + 2 * Math.PI * i / onShell;
                double ux = radius * Math.cos(angle), uy = radius * Math.sin(angle) * (skin == Skin.ATOM_RINGS ? ringTilt : 1);
                double x = f.x(ux, uy), y = f.y(ux, uy);
                if (main && lastShell && i == onShell - 1 && sinceLastOrb < AtomModelView.POP_SECONDS) {
                    // La dernière orbe arrivée : un halo qui se resserre sur elle.
                    double t = sinceLastOrb / AtomModelView.POP_SECONDS;
                    double halo = orbRadius * (1 + 5 * (1 - t));
                    g.setFill(skin == Skin.ATOM_CHALK ? CHALK : CLOUD);
                    g.setGlobalAlpha(0.5 * (1 - t));
                    g.fillOval(x - halo, y - halo, halo * 2, halo * 2);
                }
                switch (skin) {
                    case ATOM_CHALK -> chalkElectron(g, x, y, orbRadius, fade);
                    case ATOM_CLOUD -> cloud(g, x, y, orbRadius, shell * 97 + i * 13, clock, fade);
                    case ATOM_RINGS -> rock(g, x, y, orbRadius * (0.8 + 0.4 * ((i * 5 + shell) % 3) / 2.0), Math.sin(angle) > 0, fade);
                    case ATOM_GEARS -> bearing(g, x, y, orbRadius, fade);
                    default -> bead(g, x, y, orbRadius * 1.2, GLASS[(i + shell * 2) % GLASS.length], fade);
                }
            }
        }
        switch (skin) {
            case ATOM_CHALK -> chalkNucleus(g, f, nucleus, fade);
            case ATOM_CLOUD -> cloudNucleus(g, cx, cy, nucleus, clock, fade);
            case ATOM_RINGS -> planet(g, cx, cy, nucleus, clock, fade);
            case ATOM_GEARS -> gears(g, cx, cy, nucleus, clock, fade);
            default -> rosette(g, f, nucleus, clock, fade);
        }
        // Le trait rond de la craie et des bandes ne doit pas rester pour le prochain dessin.
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    // ---- Modèle de Bohr au tableau noir ------------------------------------------------------------

    /** Une orbite à la craie : un cercle tremblé, tracé par tronçons plus ou moins appuyés. */
    private static void chalkOrbit(GraphicsContext g, Frame f, double radius, int shell, double fade) {
        double[][] c = f.circle(radius, 60, 0.025, shell * 1000 + 17);
        g.setStroke(CHALK);
        g.setLineWidth(1.6);
        g.setLineCap(StrokeLineCap.ROUND);
        for (int p = 0; p < 60; p++) {
            g.setGlobalAlpha(fade * (0.35 + 0.55 * AtomModelView.noise(shell * 1000 + p * 3 + 5)));
            g.strokeLine(c[0][p], c[1][p], c[0][p + 1], c[1][p + 1]);
        }
    }

    /** Un électron à la craie : un rond au trait, et un petit « − » dedans. */
    private static void chalkElectron(GraphicsContext g, double x, double y, double r, double fade) {
        double rr = r * 1.15;
        g.setStroke(CHALK);
        g.setLineWidth(1.4);
        g.setGlobalAlpha(0.9 * fade);
        g.strokeOval(x - rr, y - rr, rr * 2, rr * 2);
        g.strokeLine(x - rr * 0.5, y, x + rr * 0.5, y);
    }

    /** Le noyau à la craie : un cercle plus épais, hachuré, avec des « + ». */
    private static void chalkNucleus(GraphicsContext g, Frame f, double nucleus, double fade) {
        double[][] c = f.circle(nucleus, 40, 0.04, 4242);
        g.setStroke(CHALK);
        g.setLineWidth(2.2);
        g.setGlobalAlpha(0.9 * fade);
        g.strokePolyline(c[0], c[1], 41);
        g.setLineWidth(1.2);
        g.setGlobalAlpha(0.45 * fade);
        for (int k = -3; k <= 3; k++) {
            double off = k * nucleus * 0.28;
            double half = Math.sqrt(Math.max(0, nucleus * nucleus - off * off)) * 0.9;
            g.strokeLine(f.x(-half, off + half * 0.3), f.y(-half, off + half * 0.3), f.x(half, off - half * 0.3), f.y(half, off - half * 0.3));
        }
        g.setGlobalAlpha(0.95 * fade);
        g.setLineWidth(1.5);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4, r = nucleus * 0.5, s = nucleus * 0.14;
            double ux = r * Math.cos(a), uy = r * Math.sin(a);
            g.strokeLine(f.x(ux - s, uy), f.y(ux - s, uy), f.x(ux + s, uy), f.y(ux + s, uy));
            g.strokeLine(f.x(ux, uy - s), f.y(ux, uy - s), f.x(ux, uy + s), f.y(ux, uy + s));
        }
    }

    // ---- Nuage de probabilité ----------------------------------------------------------------------

    /** Un électron en nuage : des points flous, plus denses au milieu, qui respirent. */
    private static void cloud(GraphicsContext g, double x, double y, double r, int seed, double clock, double fade) {
        double breath = 1 + 0.25 * Math.sin(clock * 1.5 + seed);
        g.setFill(CLOUD);
        for (int k = 0; k < 14; k++) {
            // Un point d'autant plus loin du centre qu'il est rare : la racine serre la densité au milieu.
            double dist = r * 3.2 * breath * Math.pow(AtomModelView.noise(seed * 31 + k * 2), 0.6);
            double a = 2 * Math.PI * AtomModelView.noise(seed * 31 + k * 2 + 1) + clock * 0.4;
            double dot = r * (0.9 - 0.2 * dist / (r * 3.2));
            g.setGlobalAlpha(fade * (0.3 - 0.2 * dist / (r * 3.2 * breath)));
            g.fillOval(x + dist * Math.cos(a) - dot, y + dist * Math.sin(a) - dot, dot * 2, dot * 2);
        }
        g.setGlobalAlpha(fade * 0.7);
        g.fillOval(x - r * 0.6, y - r * 0.6, r * 1.2, r * 1.2);
    }

    /** Le noyau en nuage : une grappe de points dorés, serrée, avec un cœur plein. */
    private static void cloudNucleus(GraphicsContext g, double cx, double cy, double nucleus, double clock, double fade) {
        g.setFill(Color.web("#ffd27f"));
        for (int k = 0; k < 40; k++) {
            double dist = nucleus * 1.3 * Math.pow(AtomModelView.noise(k * 2 + 900), 0.7);
            double a = 2 * Math.PI * AtomModelView.noise(k * 2 + 901) - clock * 0.2;
            double dot = nucleus * 0.14;
            g.setGlobalAlpha(fade * (0.5 - 0.35 * dist / (nucleus * 1.3)));
            g.fillOval(cx + dist * Math.cos(a) - dot, cy + dist * Math.sin(a) - dot, dot * 2, dot * 2);
        }
        g.setGlobalAlpha(0.9 * fade);
        g.fillOval(cx - nucleus * 0.45, cy - nucleus * 0.45, nucleus * 0.9, nucleus * 0.9);
    }

    // ---- Système à anneaux -------------------------------------------------------------------------

    /** Un anneau : une ellipse inclinée, large et pâle, cernée d'un trait plus net. */
    private static void ring(GraphicsContext g, Frame f, double radius, double ringTilt, int shell, double fade) {
        double[] px = new double[49], py = new double[49];
        for (int p = 0; p <= 48; p++) {
            double t = 2 * Math.PI * p / 48;
            px[p] = f.x(radius * Math.cos(t), radius * Math.sin(t) * ringTilt);
            py[p] = f.y(radius * Math.cos(t), radius * Math.sin(t) * ringTilt);
        }
        g.setStroke(Color.web("#d8e4f0"));
        g.setLineWidth(Math.max(2, radius * 0.045));
        g.setGlobalAlpha(0.14 * fade);
        g.strokePolyline(px, py, 49);
        g.setLineWidth(1);
        g.setGlobalAlpha((0.35 + 0.1 * (shell % 2)) * fade);
        g.strokePolyline(px, py, 49);
    }

    /** Un caillou de glace sur son anneau : gris bleuté, plus vif devant la planète que derrière. */
    private static void rock(GraphicsContext g, double x, double y, double r, boolean front, double fade) {
        g.setFill(front ? Color.web("#e8f0f8") : Color.web("#9aa8b8"));
        g.setGlobalAlpha(fade * (front ? 1 : 0.6));
        g.fillOval(x - r, y - r * 0.8, r * 2, r * 1.6);
    }

    /** Le noyau en planète : une sphère ocre à bandes, éclairée d'en haut à gauche, dans l'ombre en bas à droite. */
    private static void planet(GraphicsContext g, double cx, double cy, double nucleus, double clock, double fade) {
        g.setGlobalAlpha(fade);
        g.setFill(new RadialGradient(0, 0, cx - nucleus * 0.35, cy - nucleus * 0.35, nucleus * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f0c890")), new Stop(0.6, Color.web("#c98a4a")), new Stop(1, Color.web("#5a3418"))));
        g.fillOval(cx - nucleus, cy - nucleus, nucleus * 2, nucleus * 2);
        // Des bandes nuageuses qui dérivent lentement, chacune à sa latitude.
        g.setStroke(Color.web("#7a4a22"));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int k = -2; k <= 2; k++) {
            double y = cy + nucleus * 0.32 * k + nucleus * 0.08 * Math.sin(clock * 0.3 + k);
            double half = Math.sqrt(Math.max(0, nucleus * nucleus - (y - cy) * (y - cy))) * 0.92;
            double drift = Math.sin(clock * 0.2 + k * 2) * half * 0.3;
            g.setLineWidth(nucleus * (0.08 + 0.04 * (k % 2 == 0 ? 1 : 0)));
            g.setGlobalAlpha(0.45 * fade);
            g.strokeLine(cx - half + Math.max(0, drift), y, cx + half + Math.min(0, drift), y);
        }
        g.setFill(Color.color(0, 0, 0, 0.35));
        g.setGlobalAlpha(fade);
        g.fillOval(cx - nucleus * 0.4, cy - nucleus * 0.2, nucleus * 1.5, nucleus * 1.3);
    }

    // ---- Horlogerie --------------------------------------------------------------------------------

    /** Une roue dentée de laiton pleine, avec son moyeu, tournée de {@code turn}. */
    private static void gear(GraphicsContext g, double x, double y, double r, int teeth, double turn, double fade) {
        int n = teeth * 2;
        double[] px = new double[n], py = new double[n];
        for (int k = 0; k < n; k++) {
            double rr = k % 2 == 0 ? r : r * 0.78;
            double a = turn + Math.PI * k / teeth;
            px[k] = x + rr * Math.cos(a);
            py[k] = y + rr * Math.sin(a);
        }
        g.setGlobalAlpha(fade);
        g.setFill(BRASS);
        g.fillPolygon(px, py, n);
        g.setStroke(BRASS_DARK);
        g.setLineWidth(Math.max(0.6, r * 0.08));
        g.strokePolygon(px, py, n);
        g.setFill(BRASS_DARK);
        g.fillOval(x - r * 0.22, y - r * 0.22, r * 0.44, r * 0.44);
    }

    /** Le noyau en horlogerie : une roue au milieu, six autour qui s'engrènent sur elle et tournent à l'envers. */
    private static void gears(GraphicsContext g, double cx, double cy, double nucleus, double clock, double fade) {
        double r0 = nucleus * 0.48, r1 = nucleus * 0.4;
        int teeth0 = 8, teeth1 = 6;
        double turn0 = clock * 0.8;
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3 + Math.PI / 6;
            double x = cx + (r0 + r1) * 0.92 * Math.cos(a), y = cy + (r0 + r1) * 0.92 * Math.sin(a);
            // La roue voisine tourne à l'envers, au rapport des dents, décalée d'une demi-dent pour s'emboîter.
            double turn1 = -turn0 * teeth0 / teeth1 + a * (1 + (double) teeth0 / teeth1) + Math.PI / teeth1;
            gear(g, x, y, r1, teeth1, turn1, fade * (k % 2 == 0 ? 1 : 0.85));
        }
        gear(g, cx, cy, r0, teeth0, turn0, fade);
    }

    /** Une couche en roue : une jante de laiton à dents fines, et quelques rayons courts vers l'intérieur. */
    private static void wheel(GraphicsContext g, Frame f, double radius, int shell, double clock, double fade) {
        int teeth = 24 + shell * 12;
        int n = teeth * 2;
        double turn = (shell % 2 == 0 ? 1 : -1) * clock * 0.9 / (shell + 1);
        double[] px = new double[n + 1], py = new double[n + 1];
        for (int k = 0; k <= n; k++) {
            double rr = radius * (k % 2 == 0 ? 1.015 : 0.985);
            double a = turn + Math.PI * k / teeth;
            px[k] = f.x(rr * Math.cos(a), rr * Math.sin(a));
            py[k] = f.y(rr * Math.cos(a), rr * Math.sin(a));
        }
        g.setStroke(BRASS);
        g.setLineWidth(1.2);
        g.setGlobalAlpha(0.55 * fade);
        g.strokePolyline(px, py, n + 1);
        g.setLineWidth(0.8);
        g.setGlobalAlpha(0.3 * fade);
        for (int k = 0; k < 12; k++) {
            double a = turn + k * Math.PI / 6;
            g.strokeLine(f.x(radius * 0.93 * Math.cos(a), radius * 0.93 * Math.sin(a)), f.y(radius * 0.93 * Math.cos(a), radius * 0.93 * Math.sin(a)),
                    f.x(radius * Math.cos(a), radius * Math.sin(a)), f.y(radius * Math.cos(a), radius * Math.sin(a)));
        }
    }

    /** Un électron en bille d'acier : gris sombre, un reflet blanc. */
    private static void bearing(GraphicsContext g, double x, double y, double r, double fade) {
        g.setFill(Color.web("#3a3f48"));
        g.setGlobalAlpha(fade);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setStroke(Color.web("#9aa0a8"));
        g.setLineWidth(0.8);
        g.strokeOval(x - r, y - r, r * 2, r * 2);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.8 * fade);
        g.fillOval(x - r * 0.6, y - r * 0.65, r * 0.6, r * 0.6);
    }

    // ---- Vitrail -----------------------------------------------------------------------------------

    /** Les panneaux de verre : une couronne par couche, découpée en secteurs de couleurs alternées, le plomb entre. */
    private static void panels(GraphicsContext g, Frame f, double nucleus, double innerRadius, double outerRadius, int shells, double fade) {
        double inner = nucleus * 1.1;
        for (int shell = 0; shell < shells; shell++) {
            double outer = innerRadius + (outerRadius - innerRadius) * (shell + 1) / shells;
            int sectors = 6 + 4 * shell;
            for (int s = 0; s < sectors; s++) {
                double a0 = 2 * Math.PI * s / sectors + shell * 0.3, a1 = a0 + 2 * Math.PI / sectors;
                double[] px = new double[8], py = new double[8];
                for (int p = 0; p < 4; p++) {
                    double a = a0 + (a1 - a0) * p / 3;
                    px[p] = f.x(outer * Math.cos(a), outer * Math.sin(a));
                    py[p] = f.y(outer * Math.cos(a), outer * Math.sin(a));
                    px[7 - p] = f.x(inner * Math.cos(a), inner * Math.sin(a));
                    py[7 - p] = f.y(inner * Math.cos(a), inner * Math.sin(a));
                }
                g.setFill(GLASS[(s + shell * 2) % GLASS.length]);
                g.setGlobalAlpha(0.5 * fade);
                g.fillPolygon(px, py, 8);
                g.setStroke(LEAD);
                g.setLineWidth(2);
                g.setGlobalAlpha(0.9 * fade);
                g.strokeLine(px[0], py[0], px[7], py[7]);
            }
            inner = outer;
        }
    }

    /** Un cercle de plomb : la baguette qui sépare deux couronnes de verre. */
    private static void leadCircle(GraphicsContext g, Frame f, double radius, double fade) {
        double[][] c = f.circle(radius, 48, 0, 0);
        g.setStroke(LEAD);
        g.setLineWidth(2.5);
        g.setGlobalAlpha(0.9 * fade);
        g.strokePolyline(c[0], c[1], 49);
    }

    /** Une perle de verre : un rond coloré clair, cerné de plomb, un éclat blanc. */
    private static void bead(GraphicsContext g, double x, double y, double r, Color glass, double fade) {
        g.setFill(glass.interpolate(Color.WHITE, 0.45));
        g.setGlobalAlpha(fade);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setStroke(LEAD);
        g.setLineWidth(1.2);
        g.strokeOval(x - r, y - r, r * 2, r * 2);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.8 * fade);
        g.fillOval(x - r * 0.55, y - r * 0.6, r * 0.5, r * 0.5);
    }

    /** Le noyau en rosace : huit pétales de verre cernés de plomb, qui tournent à peine, un cœur doré. */
    private static void rosette(GraphicsContext g, Frame f, double nucleus, double clock, double fade) {
        double[][] rim = f.circle(nucleus, 40, 0, 0);
        g.setFill(LEAD);
        g.setGlobalAlpha(fade);
        g.fillPolygon(rim[0], rim[1], 40);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + clock * 0.05;
            // Un pétale : une goutte du centre au bord, large au milieu ; on monte par un flanc, on redescend par l'autre.
            double[] px = new double[12], py = new double[12];
            for (int p = 0; p < 12; p++) {
                double t = (p < 6 ? p : 11 - p) / 5.0;
                double side = p < 6 ? 1 : -1;
                double along = nucleus * 0.9 * t, across = nucleus * 0.28 * Math.sin(Math.PI * t);
                double ux = along * Math.cos(a) - side * across * Math.sin(a), uy = along * Math.sin(a) + side * across * Math.cos(a);
                px[p] = f.x(ux, uy);
                py[p] = f.y(ux, uy);
            }
            g.setFill(GLASS[k % GLASS.length].interpolate(Color.WHITE, 0.2));
            g.setGlobalAlpha(0.9 * fade);
            g.fillPolygon(px, py, 12);
            g.setStroke(LEAD);
            g.setLineWidth(1.2);
            g.strokePolygon(px, py, 12);
        }
        double heart = nucleus * 0.22;
        g.setFill(Color.web("#ffd27f"));
        g.setGlobalAlpha(fade);
        g.fillOval(f.x(0, 0) - heart, f.y(0, 0) - heart, heart * 2, heart * 2);
    }
}
