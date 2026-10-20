package idle.ui.darkmatter;

import idle.core.darkmatter.Landmark;
import idle.core.darkmatter.SizeScale;
import idle.core.math.BigNum;
import idle.core.universe.Skin;
import idle.ui.common.Format;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Le point de matière noire et l'échelle des grandeurs autour de lui.
 *
 * <p>La vue fonctionne comme une caméra qui recule : quand la matière noire grossit, le point enfle
 * un instant, puis la caméra dézoome pour le ramener à sa taille habituelle à l'écran. Ce qui
 * montre la croissance, ce sont donc les repères autour de lui :
 * <ul>
 *   <li>des cercles gradués, un par longueur « ronde » (1, 2 ou 5 × 10ⁿ), chacun avec sa taille ;</li>
 *   <li>des cercles plus marqués pour les repères de {@link SizeScale} (atome, virus, Terre…),
 *       avec leur nom ;</li>
 *   <li>une règle graduée en bas à gauche, qui indique ce que vaut une longueur à l'écran.</li>
 * </ul>
 * Tous ces cercles se resserrent vers le centre à mesure que la caméra recule, jusqu'à être
 * avalés par le point.
 * <ul>
 * </ul>
 *
 * <p>La vue se dessine d'après sa taille du moment : elle suit la fenêtre, et la caméra se
 * recale d'elle-même quand la vue change de taille.
 *
 * <p>Tous les calculs se font sur le logarithme des tailles : elles vont du proton à l'univers
 * et au-delà, bien plus qu'un nombre de pixels ne peut en contenir.
 */
public final class DarkMatterView extends Canvas {

    /** Part de la plus petite dimension de la vue qu'occupe le rayon du point, au repos. */
    private static final double REST_RADIUS = 0.16;
    /** Rayon maximal du point à l'écran, même quand il grossit plus vite que la caméra ne recule. */
    private static final double MAX_RADIUS = 0.40;
    /** Vitesse à laquelle la caméra rattrape la taille du point : plus c'est grand, plus elle suit de près. */
    private static final double CAMERA_SPEED = 1.5;
    /** Rayon minimal de la zone cliquable, en pixels, pour que le point reste facile à viser. */
    private static final double MIN_HIT_RADIUS = 30;
    /** Longueur maximale de la règle graduée, en pixels. */
    private static final double RULER_MAX = 120;
    /** Place qu'il faut à droite d'un cercle pour y écrire son texte, en pixels. */
    private static final double LABEL_ROOM = 110;
    /** Les longueurs « rondes » d'une puissance de dix : 1, 2 et 5. */
    private static final double[] ROUND_VALUES = {1, 2, 5};

    private static final Color BACKGROUND = Color.web("#0d0a16");
    private static final Color CORE = Color.web("#efe4ff");
    private static final Color GLOW = Color.web("#c9a6ff");
    private static final Color RING = Color.web("#8f7fb8");
    private static final Color TEXT = Color.web("#c9bfe0");

    /** Logarithme décimal du nombre de mètres que représente un pixel ; {@code NaN} avant la première image. */
    private double logScale = Double.NaN;
    private double time = 0;
    private double blobRadius = 0;
    /** La force de la chute de matière, de 0 à 1, adoucie ; et l'avancée de la spirale. */
    private double fall = 0;
    private double spiral = 0;
    /** Faux si le joueur a coupé la chute de matière : la matière noire ne fait plus que grossir. */
    private static boolean fallOn = true;

    /** Faux si le joueur a coupé l'animation de la matière noire : elle ne palpite plus, rien ne scintille. */
    private static boolean motion = true;

    /** Allume ou coupe l'animation de la matière noire (réglage « Matière noire animée »). */
    public static void setMotion(boolean on) {
        motion = on;
    }

    /** Allume ou coupe la spirale de matière qui tombe (réglage « Chute de matière noire »). */
    public static void setFall(boolean on) {
        fallOn = on;
    }
    /** La chute de matière : pour les vérifications. */
    double fall() {
        return fall;
    }

    /**
     * Le skin de la boule (version 0.089) : {@code null} pour le classique. « Obsidienne » : une pierre
     * noire taillée en facettes, aux arêtes violettes et aux reflets vifs. « Nébuleuse » : un nuage de
     * gaz rose et bleu qui ondule, piqué d'étoiles naissantes. « Contractée » : un cœur très dense
     * vers lequel des anneaux se resserrent sans fin.
     */
    private Skin skin;

    void setSkin(Skin chosen) {
        skin = chosen == Skin.DARK_OBSIDIAN || chosen == Skin.DARK_NEBULA || chosen == Skin.DARK_CONTRACTED ? chosen : null;
    }

    Skin skin() {
        return skin;
    }

    /**
     * Le skin de l'animation (version 0.089) : {@code null} pour le classique, où la matière ne tombe
     * en spirale que pendant la croissance. « Vortex » : un tourbillon de cinq bras violets qui tourne
     * sans cesse autour du point et l'alimente, bien plus vite quand on le fait grossir. « Pulsar » :
     * deux faisceaux qui balaient la vue en tournant et des anneaux qui battent depuis le point.
     */
    private Skin animation;

    void setAnimation(Skin chosen) {
        animation = chosen == Skin.DARK_VORTEX || chosen == Skin.DARK_PULSAR ? chosen : null;
    }

    Skin animation() {
        return animation;
    }

    /** L'angle du tourbillon ou des faisceaux, qui avance même au repos. */
    private double whirl = 0;

    DarkMatterView(double width, double height) {
        super(width, height);
    }

    /** Vrai si ce point de la vue est sur la matière noire (avec un peu de marge quand elle est petite). */
    boolean isOnDarkMatter(double x, double y) {
        double dx = x - getWidth() / 2;
        double dy = y - getHeight() / 2;
        double reach = Math.max(blobRadius, MIN_HIT_RADIUS);
        return dx * dx + dy * dy <= reach * reach;
    }

    /**
     * Avance l'animation et redessine.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     * @param size    taille de la matière noire (son diamètre), en mètres
     * @param growing vrai tant que le joueur maintient le point appuyé
     */
    void frame(double elapsed, BigNum size, boolean growing) {
        frame(elapsed, size, growing, 1);
    }

    /**
     * Avance l'animation et redessine (version 0.061) : quand la matière noire grossit, de la
     * matière tombe vers elle en spirale, d'autant plus vite et plus dense que sa croissance est forte.
     *
     * @param momentum l'élan de la croissance ({@link idle.core.Game#darkMatterMomentum()})
     */
    void frame(double elapsed, BigNum size, boolean growing, double momentum) {
        double wanted = fallOn && growing && momentum > 0 ? Math.max(0.15, Math.min(1, Math.log10(1 + momentum) / 2)) : 0;
        // Coupée, la chute s'arrête tout de suite au lieu de s'éteindre peu à peu (version 0.067).
        fall = fallOn ? fall + (wanted - fall) * Math.min(1, Math.min(elapsed, 0.1) * 2) : 0;
        spiral += Math.min(elapsed, 0.1) * (0.4 + 3 * fall);
        double dt = Math.min(elapsed, 0.1);
        if (motion) time += dt;
        double w = getWidth();
        double h = getHeight();
        double small = Math.min(w, h);
        if (small < 40) return;                              // fenêtre trop petite pour dessiner quoi que ce soit
        double logRadius = size.log10() - Math.log10(2);

        // La caméra : elle vise l'échelle où le point a sa taille de repos, et s'en approche peu à peu.
        double target = logRadius - Math.log10(small * REST_RADIUS);
        if (Double.isNaN(logScale)) logScale = target;
        logScale += (target - logScale) * Math.min(1, dt * CAMERA_SPEED);
        // Si le point grossit trop vite pour elle, elle recule quand même assez pour qu'il tienne dans la vue.
        logScale = Math.max(logScale, logRadius - Math.log10(small * MAX_RADIUS));
        blobRadius = Math.pow(10, logRadius - logScale);

        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, w, h);
        double cx = w / 2;
        double cy = h / 2;

        // L'échelle des grandeurs : des cercles gradués, et des cercles plus marqués pour les objets connus.
        g.setLineWidth(1);
        double minRadius = blobRadius * 1.08;
        double maxRadius = w / 2;
        double[] landmarkRadii = new double[SizeScale.LANDMARKS.size()];
        int landmarks = 0;
        for (Landmark landmark : SizeScale.LANDMARKS) {
            double logRing = landmark.size().log10() - Math.log10(2) - logScale;
            if (logRing > 4) continue;                       // des milliers de fois plus grand que la vue
            double radius = Math.pow(10, logRing);
            if (radius <= minRadius || radius > maxRadius) continue;
            landmarkRadii[landmarks++] = radius;
            drawRing(g, cx, cy, radius, landmark.name() + " (" + Format.length(landmark.size()) + ")", true);
        }
        // Les graduations : en mètres, ou en années-lumière quand les kilomètres ne suffisent plus.
        double logMeters = Math.log10(2 * minRadius) + logScale;
        double logUnit = logMeters >= Format.LIGHT_YEARS_FROM.log10() ? Format.LIGHT_YEAR.log10() : 0;
        for (double exponent = Math.floor(logMeters - logUnit); ; exponent++) {
            boolean beyond = false;
            for (double round : ROUND_VALUES) {
                double logSize = Math.log10(round) + exponent + logUnit;     // diamètre du cercle, en mètres
                double radius = Math.pow(10, logSize - Math.log10(2) - logScale);
                if (radius > maxRadius) { beyond = true; break; }
                if (radius <= minRadius) continue;
                boolean taken = false;                       // un repère occupe déjà cette place
                for (int i = 0; i < landmarks; i++) taken |= Math.abs(landmarkRadii[i] - radius) < 14;
                if (!taken) drawRing(g, cx, cy, radius, Format.length(BigNum.pow10(logSize)), false);
            }
            if (beyond) break;
        }

        if (motion) whirl += dt * (animation == Skin.DARK_PULSAR ? 1.2 + 3 * fall : 0.5 + 3.5 * fall + (growing ? 1 : 0));
        if (animation == Skin.DARK_VORTEX) vortex(g, cx, cy, small, growing);
        else if (animation == Skin.DARK_PULSAR) pulsar(g, cx, cy, w, h, growing);
        // La matière qui tombe : des grains en spirale, plus nombreux et plus rapides avec la croissance.
        if (fall > 0.02 && animation == null) {
            int grains = (int) (14 + 46 * fall);
            g.setFill(GLOW);
            for (int i = 0; i < grains; i++) {
                double seed = i * 0.618034;
                double phase = (seed + spiral * (0.35 + 0.15 * (i % 3))) % 1;   // 0 au bord, 1 avalé
                double radius = blobRadius * 1.1 + (small * 0.45 - blobRadius * 1.1) * (1 - phase) * (1 - phase);
                double angle = seed * 2 * Math.PI * 7 + phase * (3 + 5 * fall);
                g.setGlobalAlpha(Math.min(1, fall * 1.5) * Math.sin(Math.PI * phase) * 0.8);
                double r = 1 + 1.5 * fall;
                g.fillOval(cx + radius * Math.cos(angle) - r, cy + radius * Math.sin(angle) - r, 2 * r, 2 * r);
            }
            g.setGlobalAlpha(1);
        }

        // Le point : un halo qui bat quand on appuie, d'autant plus vite qu'il grossit fort, et un cœur clair.
        double pulse = growing && motion ? 1 + (0.08 + 0.06 * fall) * Math.sin(time * (9 + 12 * fall)) : 1;
        if (skin != null) {
            drawSkin(g, cx, cy, blobRadius * pulse, growing);
            drawRuler(g, h);
            return;
        }
        g.setFill(GLOW);
        g.setGlobalAlpha(growing ? 0.22 : 0.12);
        double halo = blobRadius * 1.5 * pulse;
        g.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
        g.setGlobalAlpha(growing ? 0.5 : 0.35);
        double mid = blobRadius * 1.18 * pulse;
        g.fillOval(cx - mid, cy - mid, mid * 2, mid * 2);
        g.setGlobalAlpha(1);
        g.setFill(growing ? CORE : GLOW);
        g.fillOval(cx - blobRadius, cy - blobRadius, blobRadius * 2, blobRadius * 2);

        drawRuler(g, h);
    }

    /** Le tourbillon : cinq bras de grains qui s'enroulent jusqu'au point, et un disque d'accrétion. */
    private void vortex(GraphicsContext g, double cx, double cy, double small, boolean growing) {
        double outer = small * 0.48, inner = Math.max(blobRadius * 1.05, 4);
        int arms = 5;
        double strength = 0.45 + 0.55 * Math.max(fall, growing ? 0.6 : 0);
        for (int arm = 0; arm < arms; arm++) {
            int steps = 50;
            double[] xs = new double[steps], ys = new double[steps];
            for (int k = 0; k < steps; k++) {
                double u = k / (double) (steps - 1);
                double r = inner + (outer - inner) * u;
                double a = -whirl + arm * 2 * Math.PI / arms + (1 - u) * 5;
                xs[k] = cx + r * Math.cos(a);
                ys[k] = cy + r * Math.sin(a) * 0.8;
            }
            g.setStroke(arm % 2 == 0 ? Color.web("#7a3cff") : Color.web("#c06bff"));
            g.setLineWidth(10);
            g.setGlobalAlpha(0.12 * strength);
            g.strokePolyline(xs, ys, steps);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.6 * strength);
            g.strokePolyline(xs, ys, steps);
        }
        // Les grains qui suivent les bras vers le centre.
        int grains = growing ? 90 : 50;
        g.setFill(Color.web("#efe4ff"));
        for (int i = 0; i < grains; i++) {
            double seed = (i * 0.618034) % 1;
            double phase = (seed + whirl * 0.12) % 1;
            double r = inner + (outer - inner) * (1 - phase);
            double a = -whirl + (i % arms) * 2 * Math.PI / arms + phase * 5 + seed * 0.4;
            double size = 1.2 + 2 * phase;
            g.setGlobalAlpha(0.85 * strength * Math.sin(Math.PI * phase));
            g.fillOval(cx + r * Math.cos(a) - size / 2, cy + r * Math.sin(a) * 0.8 - size / 2, size, size);
        }
        // Le disque d'accrétion, serré contre le point.
        g.setStroke(Color.web("#e0c8ff"));
        for (int k = 0; k < 3; k++) {
            double r = inner * (1.25 + 0.2 * k);
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.5 * strength);
            g.strokeOval(cx - r, cy - r * 0.8, r * 2, r * 1.6);
        }
        g.setGlobalAlpha(1);
    }

    /** Le pulsar : deux faisceaux qui tournent depuis le point, et des anneaux qui battent. */
    private void pulsar(GraphicsContext g, double cx, double cy, double w, double h, boolean growing) {
        double far = Math.hypot(w, h);
        double strength = growing ? 1 : 0.6;
        for (int side = 0; side < 2; side++) {
            double a = whirl + side * Math.PI;
            double spread = 0.07;
            g.setFill(Color.web("#3cf0ff"));
            g.setGlobalAlpha(0.18 * strength);
            g.fillPolygon(new double[] {cx, cx + far * Math.cos(a - spread), cx + far * Math.cos(a + spread)},
                    new double[] {cy, cy + far * Math.sin(a - spread), cy + far * Math.sin(a + spread)}, 3);
            g.setStroke(Color.web("#bff8ff"));
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.8 * strength);
            g.strokeLine(cx, cy, cx + far * Math.cos(a), cy + far * Math.sin(a));
        }
        g.setStroke(Color.web("#7fe8ff"));
        for (int k = 0; k < 3; k++) {
            double phase = (time * 0.8 + k / 3.0) % 1;
            double r = blobRadius * 1.1 + phase * Math.min(w, h) * 0.5;
            g.setLineWidth(2.5 * (1 - phase) + 0.5);
            g.setGlobalAlpha(0.7 * (1 - phase) * strength);
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.setGlobalAlpha(1);
    }

    /** La boule sous un skin. */
    private void drawSkin(GraphicsContext g, double cx, double cy, double r, boolean growing) {
        r = Math.max(r, 3);
        if (skin == Skin.DARK_OBSIDIAN) {
            // Une pierre taillée : neuf facettes, chacune de sa nuance, qui tournent lentement.
            int sides = 9;
            double turn = time * 0.15;
            double[] xs = new double[sides], ys = new double[sides];
            for (int k = 0; k < sides; k++) {
                double rr = r * (0.88 + 0.12 * Math.sin(k * 2.7 + 1));
                xs[k] = cx + rr * Math.cos(turn + 2 * Math.PI * k / sides);
                ys[k] = cy + rr * Math.sin(turn + 2 * Math.PI * k / sides);
            }
            g.setGlobalAlpha(growing ? 0.35 : 0.2);
            g.setFill(GLOW);
            g.fillOval(cx - r * 1.3, cy - r * 1.3, r * 2.6, r * 2.6);
            for (int k = 0; k < sides; k++) {
                int next = (k + 1) % sides;
                double shade = 0.06 + 0.12 * (0.5 + 0.5 * Math.sin(k * 1.9 + time * 0.6));
                g.setGlobalAlpha(1);
                g.setFill(Color.color(shade, shade * 0.8, shade * 1.4 > 1 ? 1 : shade * 1.4));
                g.fillPolygon(new double[] {cx + r * 0.15, xs[k], xs[next]}, new double[] {cy - r * 0.1, ys[k], ys[next]}, 3);
            }
            g.setStroke(Color.web("#a07cff"));
            g.setLineWidth(1.2);
            g.setGlobalAlpha(0.9);
            for (int k = 0; k < sides; k++) {
                int next = (k + 1) % sides;
                g.strokeLine(xs[k], ys[k], xs[next], ys[next]);
                g.setGlobalAlpha(0.35);
                g.strokeLine(cx + r * 0.15, cy - r * 0.1, xs[k], ys[k]);
                g.setGlobalAlpha(0.9);
            }
            // Un reflet qui glisse sur la pierre.
            double shine = Math.sin(time * 0.8);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(0.55);
            g.fillPolygon(new double[] {cx - r * 0.5 + shine * r * 0.2, cx - r * 0.2 + shine * r * 0.2, cx - r * 0.35 + shine * r * 0.2},
                    new double[] {cy - r * 0.55, cy - r * 0.6, cy - r * 0.2}, 3);
        } else if (skin == Skin.DARK_NEBULA) {
            // Un nuage : des volutes roses et bleues qui ondulent, plus larges que le point.
            Color[] gas = {Color.web("#ff7ad9"), Color.web("#7fb8ff"), Color.web("#c9a6ff")};
            for (int k = 0; k < 16; k++) {
                double a = k * 2.39996 + time * (0.1 + 0.03 * (k % 4));
                double d = r * (0.25 + 0.6 * ((k * 0.618034) % 1));
                double size = r * (0.55 + 0.25 * Math.sin(time * 0.7 + k));
                double x = cx + d * Math.cos(a), y = cy + d * Math.sin(a) * 0.8;
                Color c = gas[k % gas.length];
                g.setGlobalAlpha(1);
                g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, size, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), growing ? 0.35 : 0.25)),
                        new javafx.scene.paint.Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
                g.fillOval(x - size, y - size, size * 2, size * 2);
            }
            // Des étoiles qui naissent dans le nuage.
            g.setFill(Color.WHITE);
            for (int k = 0; k < 12; k++) {
                double a = k * 1.7 + 0.3, d = r * (0.1 + 0.8 * ((k * 0.381966) % 1));
                double twinkle = 0.5 + 0.5 * Math.sin(time * (1.5 + k * 0.2) + k);
                double size = 1 + 1.6 * twinkle;
                g.setGlobalAlpha(0.4 + 0.6 * twinkle);
                g.fillOval(cx + d * Math.cos(a) - size / 2, cy + d * Math.sin(a) * 0.8 - size / 2, size, size);
            }
        } else {
            // Contractée : des anneaux qui se resserrent sans fin vers un cœur minuscule et très vif.
            g.setStroke(GLOW);
            for (int k = 0; k < 6; k++) {
                double phase = ((time * 0.5 + k / 6.0) % 1);
                double rr = r * 1.6 * (1 - phase) + r * 0.2 * phase;
                g.setLineWidth(1 + 2.5 * phase);
                g.setGlobalAlpha(0.15 + 0.7 * phase);
                g.strokeOval(cx - rr, cy - rr, rr * 2, rr * 2);
            }
            // Des flèches qui pointent vers le centre.
            g.setStroke(Color.web("#e0c8ff"));
            g.setLineWidth(1.2);
            for (int k = 0; k < 8; k++) {
                double a = 2 * Math.PI * k / 8 + time * 0.2;
                double out = r * 1.5, in = r * 1.15;
                g.setGlobalAlpha(0.6);
                g.strokeLine(cx + out * Math.cos(a), cy + out * Math.sin(a), cx + in * Math.cos(a), cy + in * Math.sin(a));
                g.strokeLine(cx + in * Math.cos(a), cy + in * Math.sin(a), cx + (in + r * 0.1) * Math.cos(a + 0.08), cy + (in + r * 0.1) * Math.sin(a + 0.08));
                g.strokeLine(cx + in * Math.cos(a), cy + in * Math.sin(a), cx + (in + r * 0.1) * Math.cos(a - 0.08), cy + (in + r * 0.1) * Math.sin(a - 0.08));
            }
            double core = r * 0.35;
            g.setGlobalAlpha(0.5);
            g.setFill(GLOW);
            g.fillOval(cx - core * 1.8, cy - core * 1.8, core * 3.6, core * 3.6);
            g.setGlobalAlpha(1);
            g.setFill(Color.WHITE);
            g.fillOval(cx - core, cy - core, core * 2, core * 2);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Un cercle de l'échelle, avec son texte au-dessus (ou en haut à droite quand le haut du
     * cercle sort de la vue).
     *
     * @param landmark vrai pour un objet connu, dessiné plus net qu'une simple graduation
     */
    private void drawRing(GraphicsContext g, double cx, double cy, double radius, String text, boolean landmark) {
        g.setGlobalAlpha(landmark ? 0.9 : 0.35);
        g.setStroke(landmark ? GLOW : RING);
        g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setGlobalAlpha(landmark ? 1 : 0.6);
        g.setFill(landmark ? CORE : TEXT);
        double top = cy - radius - 5;
        double diagonal = radius * Math.sqrt(0.5);
        if (top > 12) {
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(text, cx, top);
        } else if (cy - diagonal - 5 > 12) {
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(text, cx + diagonal + 5, cy - diagonal - 5);
        } else if (cx + radius + 6 + LABEL_ROOM < getWidth()) {
            // Fenêtre large et basse : seul le côté du cercle est visible, le texte se met à sa droite.
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(text, cx + radius + 6, cy + 4);
        }
    }

    /** La règle graduée : la plus grande longueur « ronde » (1, 2 ou 5 × 10ⁿ mètres) qui tient en {@link #RULER_MAX} pixels. */
    private void drawRuler(GraphicsContext g, double h) {
        double logMax = Math.log10(RULER_MAX) + logScale;    // ce que valent RULER_MAX pixels, en mètres
        // Ronde dans l'unité affichée : en années-lumière quand les kilomètres ne suffisent plus.
        double logUnit = logMax >= Format.LIGHT_YEARS_FROM.log10() ? Format.LIGHT_YEAR.log10() : 0;
        double exponent = Math.floor(logMax - logUnit);
        double mantissa = Math.pow(10, logMax - logUnit - exponent);   // entre 1 et 10
        double round = mantissa >= 5 ? 5 : mantissa >= 2 ? 2 : 1;
        double logLength = Math.log10(round) + exponent + logUnit;     // en mètres
        double length = Math.pow(10, logLength - logScale);
        double x = 16;
        double y = h - 16;
        g.setGlobalAlpha(1);
        g.setStroke(TEXT);
        g.setLineWidth(1.5);
        g.strokeLine(x, y, x + length, y);
        g.strokeLine(x, y - 5, x, y + 5);
        g.strokeLine(x + length, y - 5, x + length, y + 5);
        g.setFill(TEXT);
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(Format.length(BigNum.pow10(logLength)), x, y - 9);
    }
}
