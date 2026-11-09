package idle.ui.universe;

import idle.core.Game;
import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins de la graine et de ce qui vit autour d'elle (version 0.196), dessinés à côté de {@link SeedView} pour la
 * garder lisible : la graine, le rond de croissance, la météo, les saisons.
 * <ul>
 * <li>La graine : « Noix » (une coque brune, ronde et ridée, sa couture au milieu), « Gland » (un fruit lisse et fauve sous
 * sa cupule écailleuse, une petite queue), « Noyau d'avocat » (un gros noyau rond, brun rosé, à la base plate), « Pépin »
 * (un pépin de pomme brun foncé, pointu et luisant), « Tournesol » (une graine rayée de gris et de blanc), « Œuf » (une
 * coquille pâle et tachetée), « Pierre brute » (un cristal violet aux facettes irrégulières) ; chacune garde la place et
 * la taille de l'amande du Classique, pointe en haut ;</li>
 * <li>le rond de croissance : « Anneau de fée » (un cercle de petits champignons autour d'une lueur verte), « Halo doré »
 * (un anneau d'or qui rayonne), « Soleil levant » (un demi-soleil orangé qui se lève sur son horizon) ;</li>
 * <li>la météo : « Bulletin télé » (des pictogrammes plats dans une vignette), « Gravure japonaise » (un soleil rouge, des
 * nuages en bandes d'encre, la pluie en longs traits), « Pastel » (des formes douces et poudrées) ;</li>
 * <li>les saisons : « Cartes à jouer » (une carte dont l'enseigne dit la saison), « Roue de couleurs » (une roue à quatre
 * quartiers, dont l'aiguille tourne avec la saison), « Vitrail » (une rosace à quatre pétales, celui de la saison
 * allumé).</li>
 * </ul>
 */
final class SeedSkinArt {

    private static final Color WALNUT = Color.web("#9a6b3c");
    private static final Color WALNUT_DARK = Color.web("#5e3d1e");
    private static final Color ACORN = Color.web("#c9924a");
    private static final Color ACORN_CAP = Color.web("#6b4a26");
    private static final Color AVOCADO = Color.web("#8a5a4a");
    private static final Color AVOCADO_LIGHT = Color.web("#c08a78");
    private static final Color PIP = Color.web("#4a2a14");
    private static final Color PIP_LIGHT = Color.web("#8a5a30");
    private static final Color SUNFLOWER = Color.web("#50505a");
    private static final Color STRIPE = Color.web("#e8e4dc");
    private static final Color EGG = Color.web("#f3e8d2");
    private static final Color SPECKLE = Color.web("#a88a62");
    private static final Color GEM = Color.web("#8a5cff");
    private static final Color GEM_LIGHT = Color.web("#d8c8ff");
    private static final Color GEM_DARK = Color.web("#4a2a9a");
    private static final Color MUSHROOM = Color.web("#d8c8a8");
    private static final Color MUSHROOM_CAP = Color.web("#b8503a");
    private static final Color FAIRY = Color.web("#9fe08a");
    private static final Color GOLD = Color.web("#ffd24a");
    private static final Color GOLD_LIGHT = Color.web("#fff1b8");
    private static final Color SUNRISE = Color.web("#ff8a3a");
    private static final Color SUNRISE_LIGHT = Color.web("#ffd27f");
    private static final Color HORIZON = Color.web("#6a4a8a");
    private static final Color TV_BACK = Color.web("#1d3557");
    private static final Color TV_SUN = Color.web("#ffd23f");
    private static final Color TV_CLOUD = Color.web("#e9ecef");
    private static final Color TV_STORM = Color.web("#8d99ae");
    private static final Color TV_RAIN = Color.web("#4cc9f0");
    private static final Color INK = Color.web("#1b1f3a");
    private static final Color INK_LIGHT = Color.web("#5a6a9a");
    private static final Color INK_SUN = Color.web("#d6453d");
    private static final Color PASTEL_SUN = Color.web("#ffe08a");
    private static final Color PASTEL_CLOUD = Color.web("#e8dcf0");
    private static final Color PASTEL_STORM = Color.web("#a8a0c8");
    private static final Color PASTEL_RAIN = Color.web("#b8d8f8");
    private static final Color CARD = Color.web("#faf6ee");
    private static final Color CARD_RED = Color.web("#c8323c");
    private static final Color CARD_BLACK = Color.web("#1a1a22");
    private static final Color LEAD = Color.web("#2a2430");
    /** Une couleur par saison, reprise de la vue : vert tendre, or, roux, bleu froid. */
    private static final Color[] SEASONS = {Color.web("#9fe08a"), Color.web("#ffd27f"), Color.web("#ffb36b"), Color.web("#a8d8ff")};

    private SeedSkinArt() {
    }

    // ------------------------------------------------------------------
    // La graine
    // ------------------------------------------------------------------

    /**
     * La graine habillée du skin, centrée en ({@code x}, {@code y}), haute de {@code 2 × size} comme l'amande du Classique,
     * pointe en haut et tournée de {@code turn} radians ; l'opacité en cours est gardée.
     */
    static void seed(GraphicsContext g, double x, double y, double size, double turn, Skin skin) {
        double alpha = g.getGlobalAlpha();
        g.save();
        g.translate(x, y);
        g.rotate(Math.toDegrees(turn));
        switch (skin) {
            case SEED_WALNUT -> walnut(g, size, alpha);
            case SEED_ACORN -> acorn(g, size, alpha);
            case SEED_AVOCADO -> avocado(g, size, alpha);
            case SEED_PIP -> pip(g, size, alpha);
            case SEED_SUNFLOWER -> sunflower(g, size, alpha);
            case SEED_EGG -> egg(g, size, alpha);
            default -> gem(g, size, alpha);
        }
        g.restore();
        g.setGlobalAlpha(alpha);
    }

    private static void walnut(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(new RadialGradient(0, 0, -s * 0.3, -s * 0.3, s * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, WALNUT.brighter()), new Stop(1, WALNUT_DARK)));
        g.fillOval(-s * 0.82, -s * 0.95, s * 1.64, s * 1.9);
        // La couture, et les rides qui en partent de chaque côté.
        g.setStroke(WALNUT_DARK);
        g.setLineWidth(Math.max(1.2, s * 0.06));
        g.strokeLine(0, -s * 0.9, 0, s * 0.9);
        g.setLineWidth(Math.max(0.8, s * 0.03));
        g.setGlobalAlpha(alpha * 0.7);
        for (int k = 0; k < 5; k++) {
            double yy = -s * 0.6 + k * s * 0.3, bend = (k % 2 == 0 ? 1 : -1) * s * 0.12;
            for (int side = -1; side <= 1; side += 2) {
                g.beginPath();
                g.moveTo(side * s * 0.1, yy);
                g.quadraticCurveTo(side * s * 0.45, yy + bend, side * s * 0.72, yy - bend * 0.5);
                g.stroke();
            }
        }
    }

    private static void acorn(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        // Le fruit, lisse et fauve, pointe en bas ; la cupule écailleuse en haut, et sa queue.
        g.setFill(new RadialGradient(0, 0, -s * 0.25, -s * 0.1, s * 1.2, false, CycleMethod.NO_CYCLE,
                new Stop(0, ACORN.brighter()), new Stop(1, ACORN.darker())));
        g.beginPath();
        g.moveTo(-s * 0.68, -s * 0.2);
        g.bezierCurveTo(-s * 0.7, s * 0.5, -s * 0.3, s * 0.95, 0, s);
        g.bezierCurveTo(s * 0.3, s * 0.95, s * 0.7, s * 0.5, s * 0.68, -s * 0.2);
        g.closePath();
        g.fill();
        g.setFill(ACORN_CAP);
        g.fillOval(-s * 0.75, -s * 0.75, s * 1.5, s * 0.9);
        g.fillRect(-s * 0.75, -s * 0.3, s * 1.5, s * 0.12);
        g.setStroke(ACORN_CAP.brighter());
        g.setLineWidth(Math.max(0.8, s * 0.03));
        g.setGlobalAlpha(alpha * 0.6);
        for (int row = 0; row < 3; row++) {
            for (int k = -3; k <= 3; k++) {
                double cx = k * s * 0.2 + (row % 2) * s * 0.1, cy = -s * 0.6 + row * s * 0.14;
                if (Math.abs(cx) > s * 0.65) continue;
                g.strokeArc(cx - s * 0.1, cy, s * 0.2, s * 0.14, 180, 180, ArcType.OPEN);
            }
        }
        g.setGlobalAlpha(alpha);
        g.setStroke(ACORN_CAP);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(Math.max(1.5, s * 0.08));
        g.strokeLine(0, -s * 0.7, s * 0.1, -s * 1.05);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(alpha * 0.35);
        g.fillOval(-s * 0.4, 0, s * 0.18, s * 0.45);
    }

    private static void avocado(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(new RadialGradient(0, 0, -s * 0.3, -s * 0.35, s * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, AVOCADO_LIGHT), new Stop(0.6, AVOCADO), new Stop(1, AVOCADO.darker())));
        // Un gros noyau rond, dont la base est plate.
        g.beginPath();
        g.moveTo(-s * 0.8, s * 0.45);
        g.bezierCurveTo(-s * 1.0, -s * 0.3, -s * 0.6, -s, 0, -s);
        g.bezierCurveTo(s * 0.6, -s, s * 1.0, -s * 0.3, s * 0.8, s * 0.45);
        g.bezierCurveTo(s * 0.7, s * 0.75, -s * 0.7, s * 0.75, -s * 0.8, s * 0.45);
        g.closePath();
        g.fill();
        g.setStroke(AVOCADO.darker());
        g.setLineWidth(Math.max(0.8, s * 0.03));
        g.setGlobalAlpha(alpha * 0.5);
        g.strokeLine(-s * 0.72, s * 0.62, s * 0.72, s * 0.62);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(alpha * 0.3);
        g.fillOval(-s * 0.5, -s * 0.6, s * 0.4, s * 0.3);
    }

    private static void pip(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(new RadialGradient(0, 0, -s * 0.15, -s * 0.1, s * 1.1, false, CycleMethod.NO_CYCLE,
                new Stop(0, PIP_LIGHT), new Stop(1, PIP)));
        // Une goutte pointue en haut, renflée en bas.
        g.beginPath();
        g.moveTo(0, -s);
        g.bezierCurveTo(s * 0.3, -s * 0.5, s * 0.62, 0, s * 0.56, s * 0.5);
        g.bezierCurveTo(s * 0.5, s * 0.9, -s * 0.5, s * 0.9, -s * 0.56, s * 0.5);
        g.bezierCurveTo(-s * 0.62, 0, -s * 0.3, -s * 0.5, 0, -s);
        g.closePath();
        g.fill();
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(alpha * 0.55);
        g.save();
        g.translate(-s * 0.22, -s * 0.05);
        g.rotate(15);
        g.fillOval(-s * 0.07, -s * 0.3, s * 0.14, s * 0.6);
        g.restore();
    }

    private static void sunflower(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(SUNFLOWER);
        g.beginPath();
        g.moveTo(0, -s);
        g.bezierCurveTo(s * 0.45, -s * 0.5, s * 0.62, s * 0.1, s * 0.5, s * 0.6);
        g.bezierCurveTo(s * 0.4, s * 0.95, -s * 0.4, s * 0.95, -s * 0.5, s * 0.6);
        g.bezierCurveTo(-s * 0.62, s * 0.1, -s * 0.45, -s * 0.5, 0, -s);
        g.closePath();
        g.fill();
        // Les rayures blanches, qui suivent la forme, de la pointe au pied.
        g.setStroke(STRIPE);
        g.setLineWidth(Math.max(1, s * 0.07));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setGlobalAlpha(alpha * 0.85);
        for (int k = -2; k <= 2; k++) {
            double spread = k * 0.2;
            g.beginPath();
            g.moveTo(spread * s * 0.3, -s * 0.8);
            g.bezierCurveTo(spread * s * 2.2, -s * 0.2, spread * s * 2.4, s * 0.4, spread * s * 1.8, s * 0.8);
            g.stroke();
        }
        g.setStroke(SUNFLOWER.darker());
        g.setLineWidth(1);
        g.setGlobalAlpha(alpha);
        g.beginPath();
        g.moveTo(0, -s);
        g.bezierCurveTo(s * 0.45, -s * 0.5, s * 0.62, s * 0.1, s * 0.5, s * 0.6);
        g.bezierCurveTo(s * 0.4, s * 0.95, -s * 0.4, s * 0.95, -s * 0.5, s * 0.6);
        g.bezierCurveTo(-s * 0.62, s * 0.1, -s * 0.45, -s * 0.5, 0, -s);
        g.closePath();
        g.stroke();
    }

    private static void egg(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(new RadialGradient(0, 0, -s * 0.3, -s * 0.2, s * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.6, EGG), new Stop(1, EGG.darker())));
        g.beginPath();
        g.moveTo(0, -s);
        g.bezierCurveTo(s * 0.4, -s, s * 0.72, -s * 0.1, s * 0.68, s * 0.35);
        g.bezierCurveTo(s * 0.64, s * 0.85, s * 0.35, s, 0, s);
        g.bezierCurveTo(-s * 0.35, s, -s * 0.64, s * 0.85, -s * 0.68, s * 0.35);
        g.bezierCurveTo(-s * 0.72, -s * 0.1, -s * 0.4, -s, 0, -s);
        g.closePath();
        g.fill();
        g.setFill(SPECKLE);
        for (int k = 0; k < 16; k++) {
            double a = UniverseView.noise(k * 13 + 3) * Math.PI * 2, d = Math.sqrt(UniverseView.noise(k * 13 + 4));
            double sx = Math.cos(a) * d * s * 0.5, sy = s * 0.15 + Math.sin(a) * d * s * 0.7;
            double dot = s * (0.025 + 0.04 * UniverseView.noise(k * 13 + 5));
            g.setGlobalAlpha(alpha * 0.45);
            g.fillOval(sx - dot, sy - dot * 0.8, 2 * dot, 1.6 * dot);
        }
    }

    private static void gem(GraphicsContext g, double s, double alpha) {
        g.setGlobalAlpha(alpha);
        // Un cristal brut : un polygone irrégulier, et ses facettes en triangles plus clairs ou plus sombres.
        double[] xs = {0, s * 0.45, s * 0.7, s * 0.5, -s * 0.2, -s * 0.65, -s * 0.55};
        double[] ys = {-s, -s * 0.55, s * 0.1, s * 0.85, s * 0.95, s * 0.4, -s * 0.4};
        g.setFill(GEM);
        g.fillPolygon(xs, ys, xs.length);
        double hx = -s * 0.1, hy = -s * 0.1;
        for (int k = 0; k < xs.length; k++) {
            int next = (k + 1) % xs.length;
            g.setFill(k % 2 == 0 ? GEM_LIGHT : GEM_DARK);
            g.setGlobalAlpha(alpha * (k % 2 == 0 ? 0.45 : 0.5));
            g.fillPolygon(new double[] {hx, xs[k], xs[next]}, new double[] {hy, ys[k], ys[next]}, 3);
        }
        g.setStroke(GEM_LIGHT);
        g.setLineWidth(Math.max(0.8, s * 0.03));
        g.setGlobalAlpha(alpha * 0.8);
        g.strokePolygon(xs, ys, xs.length);
        for (int k = 0; k < xs.length; k += 2) g.strokeLine(hx, hy, xs[k], ys[k]);
    }

    // ------------------------------------------------------------------
    // Le rond de croissance
    // ------------------------------------------------------------------

    /** Le rond de croissance habillé du skin, centré en ({@code x}, {@code y}), de rayon {@code r} (il bat déjà), {@code share} du temps restant. */
    static void bloom(GraphicsContext g, double x, double y, double r, double share, double time, Skin skin) {
        switch (skin) {
            case RING_FAIRY -> fairyRing(g, x, y, r, share, time);
            case RING_HALO -> halo(g, x, y, r, share, time);
            default -> sunrise(g, x, y, r, share, time);
        }
        g.setGlobalAlpha(1);
    }

    private static void fairyRing(GraphicsContext g, double x, double y, double r, double share, double time) {
        UniverseView.glow(g, x, y, r * 1.8, FAIRY, 0.45);
        // Les champignons en cercle : un pied clair, un chapeau rouge à points blancs.
        int count = 9;
        for (int k = 0; k < count; k++) {
            double a = Math.PI * 2 * k / count + time * 0.15, mx = x + Math.cos(a) * r * 1.05, my = y + Math.sin(a) * r * 0.75;
            double m = r * 0.26 * (0.85 + 0.3 * UniverseView.noise(k * 5 + 40));
            g.setGlobalAlpha(0.95);
            g.setFill(MUSHROOM);
            g.fillRoundRect(mx - m * 0.22, my - m * 0.1, m * 0.44, m * 0.7, m * 0.2, m * 0.2);
            g.setFill(MUSHROOM_CAP);
            g.fillArc(mx - m * 0.6, my - m * 0.55, m * 1.2, m * 0.9, 0, 180, ArcType.ROUND);
            g.setFill(Color.WHITE);
            g.fillOval(mx - m * 0.35, my - m * 0.4, m * 0.16, m * 0.16);
            g.fillOval(mx + m * 0.1, my - m * 0.5, m * 0.14, m * 0.14);
        }
        // L'anneau du temps qu'il reste, en vert, entre les champignons et le centre.
        double ring = r * 0.6;
        g.setStroke(FAIRY);
        g.setLineWidth(2.5);
        g.setGlobalAlpha(0.9);
        g.strokeArc(x - ring, y - ring, ring * 2, ring * 2, 90, 360 * share, ArcType.OPEN);
        g.setFill(FAIRY);
        g.setGlobalAlpha(0.5);
        g.fillOval(x - ring * 0.5, y - ring * 0.5, ring, ring);
    }

    private static void halo(GraphicsContext g, double x, double y, double r, double share, double time) {
        UniverseView.glow(g, x, y, r * 2.4, GOLD, 0.55);
        // Les rayons, qui tournent lentement.
        g.setStroke(GOLD_LIGHT);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.6);
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12 + time * 0.4, in = r * 1.1, out = r * (1.5 + 0.25 * Math.sin(time * 3 + k));
            g.strokeLine(x + Math.cos(a) * in, y + Math.sin(a) * in, x + Math.cos(a) * out, y + Math.sin(a) * out);
        }
        // L'anneau d'or, épais, et dessus le temps qu'il reste en plus clair.
        g.setStroke(GOLD);
        g.setLineWidth(Math.max(4, r * 0.3));
        g.setGlobalAlpha(0.85);
        g.strokeOval(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7);
        g.setStroke(GOLD_LIGHT);
        g.setLineWidth(Math.max(2, r * 0.12));
        g.setGlobalAlpha(0.95);
        g.strokeArc(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7, 90, 360 * share, ArcType.OPEN);
    }

    private static void sunrise(GraphicsContext g, double x, double y, double r, double share, double time) {
        UniverseView.glow(g, x, y, r * 2.2, SUNRISE, 0.45);
        // Le demi-soleil au-dessus de l'horizon, ses rayons en éventail, l'horizon en trait violet.
        g.setGlobalAlpha(0.9);
        g.setStroke(SUNRISE_LIGHT);
        g.setLineWidth(2);
        for (int k = 0; k <= 8; k++) {
            double a = Math.PI + Math.PI * k / 8, out = r * (1.4 + 0.2 * Math.sin(time * 2 + k));
            g.strokeLine(x + Math.cos(a) * r * 1.05, y + Math.sin(a) * r * 1.05, x + Math.cos(a) * out, y + Math.sin(a) * out);
        }
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE, new Stop(0, SUNRISE_LIGHT), new Stop(1, SUNRISE)));
        g.fillArc(x - r, y - r, r * 2, r * 2, 0, 180, ArcType.ROUND);
        g.setStroke(HORIZON);
        g.setLineWidth(3);
        g.strokeLine(x - r * 1.6, y, x + r * 1.6, y);
        // Le temps qu'il reste : un arc au-dessus du soleil, de gauche à droite.
        g.setStroke(SUNRISE_LIGHT);
        g.setLineWidth(3);
        g.setGlobalAlpha(0.95);
        g.strokeArc(x - r * 1.2, y - r * 1.2, r * 2.4, r * 2.4, 180 - 180 * share, 180 * share, ArcType.OPEN);
    }

    // ------------------------------------------------------------------
    // La météo
    // ------------------------------------------------------------------

    /**
     * Le ciel de la graine habillé du skin, en haut à gauche en ({@code x}, {@code y}) et au rayon {@code r}, et ce que le
     * temps fait à toute la vue ; la ligne de texte reste à la vue.
     */
    static void weather(GraphicsContext g, double w, double h, double x, double y, double r, Game.Weather weather, double time, Skin skin) {
        switch (skin) {
            case WEATHER_TV -> tv(g, w, h, x, y, r, weather, time);
            case WEATHER_UKIYOE -> ukiyoe(g, w, h, x, y, r, weather, time);
            default -> pastel(g, w, h, x, y, r, weather, time);
        }
        g.setGlobalAlpha(1);
    }

    private static void tv(GraphicsContext g, double w, double h, double x, double y, double r, Game.Weather weather, double time) {
        if (weather == Game.Weather.STORM) rain(g, w, h, time, TV_RAIN, 0.3, 8, 2, 50);
        // La vignette du bulletin : un cadre bleu nuit, le pictogramme plat dedans.
        g.setGlobalAlpha(0.9);
        g.setFill(TV_BACK);
        g.fillRoundRect(x - r * 1.7, y - r * 1.5, r * 3.4, r * 3.0, r * 0.4, r * 0.4);
        g.setStroke(TV_CLOUD);
        g.setLineWidth(1.5);
        g.strokeRoundRect(x - r * 1.7, y - r * 1.5, r * 3.4, r * 3.0, r * 0.4, r * 0.4);
        g.setGlobalAlpha(1);
        switch (weather) {
            case SUN -> {
                UniverseView.glow(g, x, y, Math.min(w, h) * 0.5, TV_SUN, 0.1);
                g.setFill(TV_SUN);
                g.fillOval(x - r * 0.65, y - r * 0.65, r * 1.3, r * 1.3);
                g.setStroke(TV_SUN);
                g.setLineWidth(Math.max(2, r * 0.12));
                g.setLineCap(StrokeLineCap.ROUND);
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * k / 4;
                    g.strokeLine(x + Math.cos(a) * r * 0.9, y + Math.sin(a) * r * 0.9, x + Math.cos(a) * r * 1.2, y + Math.sin(a) * r * 1.2);
                }
            }
            case VARIABLE -> {
                g.setFill(TV_SUN);
                g.fillOval(x - r * 0.9, y - r * 1.0, r * 1.0, r * 1.0);
                flatCloud(g, x + r * 0.15, y + r * 0.3, r * 0.85, TV_CLOUD);
            }
            case STORM -> {
                flatCloud(g, x, y - r * 0.2, r * 0.9, TV_STORM);
                g.setFill(TV_SUN);
                g.fillPolygon(new double[] {x + r * 0.15, x - r * 0.25, x + r * 0.02, x - r * 0.15, x + r * 0.35, x + r * 0.1},
                        new double[] {y + r * 0.3, y + r * 0.85, y + r * 0.85, y + r * 1.3, y + r * 0.7, y + r * 0.7}, 6);
            }
        }
    }

    /** Un nuage plat de bulletin : trois ronds et une base, d'une seule couleur unie. */
    private static void flatCloud(GraphicsContext g, double x, double y, double r, Color color) {
        g.setFill(color);
        g.fillOval(x - r * 1.2, y - r * 0.4, r * 1.1, r * 0.9);
        g.fillOval(x - r * 0.6, y - r * 0.9, r * 1.3, r * 1.3);
        g.fillOval(x + r * 0.2, y - r * 0.5, r * 1.0, r * 0.9);
        g.fillRoundRect(x - r * 1.2, y - r * 0.05, r * 2.4, r * 0.55, r * 0.5, r * 0.5);
    }

    private static void ukiyoe(GraphicsContext g, double w, double h, double x, double y, double r, Game.Weather weather, double time) {
        // La pluie en longs traits fins, serrés et parallèles, sur toute la vue.
        if (weather == Game.Weather.STORM) rain(g, w, h, time, INK_LIGHT, 0.5, 34, 1, 90);
        switch (weather) {
            case SUN -> {
                UniverseView.glow(g, x, y, Math.min(w, h) * 0.4, INK_SUN, 0.08);
                g.setGlobalAlpha(0.95);
                g.setFill(INK_SUN);
                g.fillOval(x - r * 0.9, y - r * 0.9, r * 1.8, r * 1.8);
            }
            case VARIABLE -> {
                g.setGlobalAlpha(0.95);
                g.setFill(INK_SUN);
                g.fillOval(x - r * 0.9, y - r * 1.1, r * 1.5, r * 1.5);
                inkClouds(g, x, y + r * 0.4, r, INK_LIGHT);
            }
            case STORM -> inkClouds(g, x, y - r * 0.2, r * 1.1, INK);
        }
        g.setGlobalAlpha(1);
    }

    /** Des nuages d'estampe : des bandes horizontales arrondies, étagées, une plus courte sur l'autre. */
    private static void inkClouds(GraphicsContext g, double x, double y, double r, Color color) {
        g.setGlobalAlpha(0.95);
        g.setFill(color);
        g.fillRoundRect(x - r * 1.5, y, r * 3.0, r * 0.4, r * 0.4, r * 0.4);
        g.fillRoundRect(x - r * 1.0, y - r * 0.45, r * 2.3, r * 0.4, r * 0.4, r * 0.4);
        g.fillRoundRect(x - r * 0.4, y - r * 0.9, r * 1.4, r * 0.4, r * 0.4, r * 0.4);
        g.setStroke(Color.WHITE);
        g.setLineWidth(1);
        g.setGlobalAlpha(0.5);
        g.strokeLine(x - r * 1.3, y + r * 0.2, x + r * 1.3, y + r * 0.2);
    }

    private static void pastel(GraphicsContext g, double w, double h, double x, double y, double r, Game.Weather weather, double time) {
        if (weather == Game.Weather.STORM) {
            // La pluie en points doux, poudrés.
            g.setFill(PASTEL_RAIN);
            for (int k = 0; k < 70; k++) {
                double fall = (time * 0.5 + UniverseView.noise(k * 3 + 1)) % 1;
                double rx = UniverseView.noise(k * 3 + 2) * w, ry = fall * h;
                g.setGlobalAlpha(0.25);
                g.fillOval(rx - 2.5, ry - 4, 5, 8);
            }
        }
        switch (weather) {
            case SUN -> {
                UniverseView.glow(g, x, y, Math.min(w, h) * 0.5, PASTEL_SUN, 0.15);
                smudge(g, x, y, r * 1.1, PASTEL_SUN);
            }
            case VARIABLE -> {
                smudge(g, x - r * 0.4, y - r * 0.3, r * 0.9, PASTEL_SUN);
                smudge(g, x + r * 0.6, y + r * 0.4, r * 0.8, PASTEL_CLOUD);
                smudge(g, x - r * 0.1, y + r * 0.6, r * 0.7, PASTEL_CLOUD);
            }
            case STORM -> {
                smudge(g, x - r * 0.5, y, r * 0.9, PASTEL_STORM);
                smudge(g, x + r * 0.5, y - r * 0.2, r * 1.0, PASTEL_STORM);
                smudge(g, x, y + r * 0.5, r * 0.8, PASTEL_STORM);
                smudge(g, x - r * 0.1, y + r * 1.6, r * 0.35, PASTEL_SUN);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Une tache de pastel : des ronds superposés, de plus en plus petits et opaques, aux bords poudrés. */
    private static void smudge(GraphicsContext g, double x, double y, double r, Color color) {
        for (int k = 0; k < 4; k++) {
            double rr = r * (1.25 - 0.22 * k);
            g.setGlobalAlpha(0.22 + 0.15 * k);
            g.setFill(color);
            g.fillOval(x - rr + Math.sin(k * 2.1) * r * 0.08, y - rr + Math.cos(k * 1.7) * r * 0.08, 2 * rr, 2 * rr);
        }
    }

    /** La pluie sur toute la vue, en traits penchés : {@code count} traits, longs de {@code length}, larges de {@code width}. */
    private static void rain(GraphicsContext g, double w, double h, double time, Color color, double alpha, double length, double width, int count) {
        g.setStroke(color);
        g.setLineWidth(width);
        g.setGlobalAlpha(alpha);
        for (int k = 0; k < count; k++) {
            double fall = (time * 0.9 + UniverseView.noise(k * 3 + 1)) % 1;
            double rx = UniverseView.noise(k * 3 + 2) * (w + h * 0.2) - h * 0.1 * fall, ry = fall * h;
            g.strokeLine(rx, ry, rx - length * 0.28, ry + length);
        }
    }

    // ------------------------------------------------------------------
    // Les saisons
    // ------------------------------------------------------------------

    /** L'emblème de la saison habillé du skin, centré en ({@code x}, {@code y}), haut de {@code 2 × r} ; {@code left} la part de la saison qu'il reste. */
    static void season(GraphicsContext g, double x, double y, double r, Game.Season season, double left, Skin skin) {
        switch (skin) {
            case SEASONS_CARDS -> card(g, x, y, r, season);
            case SEASONS_WHEEL -> wheel(g, x, y, r, season, left);
            default -> stained(g, x, y, r, season);
        }
        g.setGlobalAlpha(1);
    }

    private static void card(GraphicsContext g, double x, double y, double r, Game.Season season) {
        // Une carte blanche, et l'enseigne de la saison : cœur au printemps, carreau l'été, trèfle à l'automne, pique l'hiver.
        double cw = r * 1.4, ch = r * 2;
        g.setGlobalAlpha(0.95);
        g.setFill(CARD);
        g.fillRoundRect(x - cw / 2, y - ch / 2, cw, ch, r * 0.25, r * 0.25);
        g.setStroke(LEAD);
        g.setLineWidth(1);
        g.strokeRoundRect(x - cw / 2, y - ch / 2, cw, ch, r * 0.25, r * 0.25);
        boolean red = season == Game.Season.SPRING || season == Game.Season.SUMMER;
        g.setFill(red ? CARD_RED : CARD_BLACK);
        double e = r * 0.55;
        switch (season) {
            case SPRING -> {
                g.fillOval(x - e, y - e * 0.7, e, e);
                g.fillOval(x, y - e * 0.7, e, e);
                g.fillPolygon(new double[] {x - e * 0.95, x + e * 0.95, x}, new double[] {y - e * 0.1, y - e * 0.1, y + e * 0.9}, 3);
            }
            case SUMMER -> g.fillPolygon(new double[] {x, x + e * 0.75, x, x - e * 0.75}, new double[] {y - e, y, y + e, y}, 4);
            case AUTUMN -> {
                g.fillOval(x - e * 0.45, y - e, e * 0.9, e * 0.9);
                g.fillOval(x - e, y - e * 0.35, e * 0.9, e * 0.9);
                g.fillOval(x + e * 0.1, y - e * 0.35, e * 0.9, e * 0.9);
                g.fillPolygon(new double[] {x - e * 0.15, x + e * 0.15, x + e * 0.35, x - e * 0.35}, new double[] {y, y, y + e, y + e}, 4);
            }
            case WINTER -> {
                g.fillPolygon(new double[] {x, x + e * 0.95, x - e * 0.95}, new double[] {y - e, y + e * 0.2, y + e * 0.2}, 3);
                g.fillOval(x - e, y - e * 0.3, e, e);
                g.fillOval(x, y - e * 0.3, e, e);
                g.fillPolygon(new double[] {x - e * 0.15, x + e * 0.15, x + e * 0.35, x - e * 0.35}, new double[] {y, y, y + e, y + e}, 4);
            }
        }
        // Les index en coin, un trait de la couleur de la saison.
        g.setFill(SEASONS[season.ordinal()]);
        g.fillRect(x - cw / 2 + r * 0.15, y - ch / 2 + r * 0.15, r * 0.2, r * 0.35);
        g.fillRect(x + cw / 2 - r * 0.35, y + ch / 2 - r * 0.5, r * 0.2, r * 0.35);
    }

    private static void wheel(GraphicsContext g, double x, double y, double r, Game.Season season, double left) {
        // Quatre quartiers, un par saison, celui en cours plus vif ; l'aiguille le parcourt à mesure que la saison passe.
        for (int k = 0; k < 4; k++) {
            boolean now = k == season.ordinal();
            g.setGlobalAlpha(now ? 0.95 : 0.45);
            g.setFill(SEASONS[k]);
            g.fillArc(x - r, y - r, 2 * r, 2 * r, 90 - 90 * (k + 1), 90, ArcType.ROUND);
        }
        g.setGlobalAlpha(0.9);
        g.setStroke(LEAD);
        g.setLineWidth(1.5);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.strokeLine(x - r, y, x + r, y);
        g.strokeLine(x, y - r, x, y + r);
        double a = Math.toRadians(-90 + 90 * season.ordinal() + 90 * (1 - Math.max(0, Math.min(1, left))));
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(x, y, x + Math.cos(a) * r * 0.85, y + Math.sin(a) * r * 0.85);
        g.setFill(LEAD);
        g.fillOval(x - r * 0.15, y - r * 0.15, r * 0.3, r * 0.3);
    }

    private static void stained(GraphicsContext g, double x, double y, double r, Game.Season season) {
        // Une rosace : quatre pétales de verre sertis de plomb, celui de la saison allumé, les autres éteints.
        for (int k = 0; k < 4; k++) {
            boolean now = k == season.ordinal();
            double a = -Math.PI / 2 + k * Math.PI / 2;
            double px = x + Math.cos(a) * r * 0.5, py = y + Math.sin(a) * r * 0.5;
            if (now) UniverseView.glow(g, px, py, r * 0.9, SEASONS[k], 0.6);
            g.setGlobalAlpha(now ? 0.95 : 0.35);
            g.setFill(SEASONS[k]);
            g.save();
            g.translate(px, py);
            g.rotate(Math.toDegrees(a) + 90);
            g.fillOval(-r * 0.36, -r * 0.5, r * 0.72, r);
            g.restore();
        }
        g.setGlobalAlpha(0.95);
        g.setStroke(LEAD);
        g.setLineWidth(2);
        for (int k = 0; k < 4; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 2;
            double px = x + Math.cos(a) * r * 0.5, py = y + Math.sin(a) * r * 0.5;
            g.save();
            g.translate(px, py);
            g.rotate(Math.toDegrees(a) + 90);
            g.strokeOval(-r * 0.36, -r * 0.5, r * 0.72, r);
            g.restore();
        }
        g.setFill(GOLD_LIGHT);
        g.fillOval(x - r * 0.18, y - r * 0.18, r * 0.36, r * 0.36);
        g.strokeOval(x - r * 0.18, y - r * 0.18, r * 0.36, r * 0.36);
    }
}
