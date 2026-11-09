package idle.ui.particles;

import idle.core.universe.Skin;
import idle.ui.particles.ParticleView.Dust;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Les six skins de la fusion de la version 0.191, dessinés à côté de {@link ParticleView} pour le
 * garder lisible. Mêmes mouvements que le classique (poussières qui convergent, ondes à chaque
 * particule créée, noyau qui grossit avec l'avancement), autres formes :
 * <ul>
 * <li>« Braise » : des braises orangées qui rougeoient en approchant, des étincelles qui montent, un
 * charbon craquelé qui luit par les fentes ;</li>
 * <li>« Encre » : un rond de papier crème, des gouttes d'encre bleu nuit qui se diffusent en halo,
 * une tache au centre ;</li>
 * <li>« Essaim » : une nuée de petits points qui tournent autour du centre, une ruche qui grossit ;</li>
 * <li>« Horloge » : un cadran, trois aiguilles qui portent les poussières vers le pivot, la fusion
 * fait un tic ;</li>
 * <li>« Verre soufflé » : des bulles translucides irisées au reflet blanc, qui éclatent en arrivant ;</li>
 * <li>« Pixel » : tout en carrés nets alignés sur une grille, seize couleurs 8 bits.</li>
 * </ul>
 */
final class ParticleSkinArt {

    /** Les seize couleurs du skin « Pixel » : noir, bleu nuit, bleu, bleu clair, blanc, jaune, orange, rouge. */
    private static final Color[] PALETTE = {Color.web("#000000"), Color.web("#1d2b53"), Color.web("#29adff"), Color.web("#83c6ff"),
        Color.web("#fff1e8"), Color.web("#ffec27"), Color.web("#ffa300"), Color.web("#ff004d")};

    private static final Color EMBER = Color.web("#ffa53a");
    private static final Color EMBER_HOT = Color.web("#ff4a1a");
    private static final Color INK = Color.web("#141a3a");
    private static final Color PAPER = Color.web("#f3ecd8");
    private static final Color SWARM = Color.web("#e8ff9a");
    private static final Color BRASS = Color.web("#d8b86a");
    private static final Color IVORY = Color.web("#efe6d2");

    private ParticleSkinArt() {
    }

    /** Vrai pour les skins de la fusion dessinés ici : ceux de la version 0.191. */
    static boolean draws(Skin skin) {
        return skin == Skin.FUSION_EMBER || skin == Skin.FUSION_INK || skin == Skin.FUSION_SWARM
                || skin == Skin.FUSION_CLOCK || skin == Skin.FUSION_GLASS || skin == Skin.FUSION_PIXEL;
    }

    /**
     * Dessine la fusion sous ce skin, sur une vue déjà effacée.
     *
     * @param maxRadius le rayon de la vue, en pixels
     * @param scale     l'échelle par rapport à la vue de référence
     * @param birth     0 au tout début de la naissance, 1 une fois le générateur allumé
     * @param fadeIn    l'apparition en fondu de la vue
     * @param progress  l'avancement de la particule en cours, de 0 à 1
     * @param fast      vrai quand les particules ne sont plus animées une par une
     */
    static void draw(Skin skin, GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double progress, boolean fast) {
        double core = view.coreRadius(progress, fast, scale, birth);
        switch (skin) {
            case FUSION_EMBER -> ember(g, view, cx, cy, maxRadius, scale, birth, fadeIn, core);
            case FUSION_INK -> ink(g, view, cx, cy, maxRadius, scale, birth, fadeIn, core, fast);
            case FUSION_SWARM -> swarm(g, view, cx, cy, maxRadius, scale, birth, fadeIn, core);
            case FUSION_CLOCK -> clock(g, view, cx, cy, maxRadius, scale, birth, fadeIn, progress, fast, core);
            case FUSION_GLASS -> glass(g, view, cx, cy, maxRadius, scale, birth, fadeIn, core);
            default -> pixel(g, view, cx, cy, maxRadius, scale, birth, fadeIn, core);
        }
        g.setGlobalAlpha(1);
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double noise(int seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }

    // ---- Braise -------------------------------------------------------------------------------------

    private static void ember(GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double core) {
        double unit = Math.max(scale, 0.8);
        double time = view.time();
        double intensity = view.intensity();
        // Une lueur rouge sombre au fond, qui respire.
        double halo = maxRadius * (0.5 + 0.35 * intensity) * birth;
        if (halo > 1) {
            g.setGlobalAlpha(fadeIn * (0.8 + 0.2 * Math.sin(time * 2.3)));
            g.setFill(new RadialGradient(0, 0, cx, cy, halo, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(1, 0.3, 0.05, 0.22)), new Stop(1, Color.color(0.6, 0.1, 0, 0))));
            g.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
        }
        // Les braises : orange au départ, rouges et plus vives en approchant, chacune auréolée.
        for (Dust d : view.dust()) {
            double t = d.age / d.flight;
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t;
            double x = cx + radius * Math.cos(angle), y = cy + radius * Math.sin(angle);
            double glow = 0.6 + 0.4 * Math.sin(time * 9 + d.angle * 13);
            Color c = EMBER.interpolate(EMBER_HOT, t);
            double alpha = Math.min(1, t * 4) * fadeIn;
            g.setFill(c);
            g.setGlobalAlpha(alpha * 0.25 * glow);
            ParticleView.circle(g, x, y, d.size * unit * 1.6);
            g.setGlobalAlpha(alpha * glow);
            ParticleView.circle(g, x, y, d.size * unit * 0.6);
        }
        // Les étincelles qui montent du foyer, plus nombreuses quand ça va vite.
        int sparks = 6 + (int) (10 * intensity);
        g.setFill(Color.web("#ffe08a"));
        for (int k = 0; k < sparks; k++) {
            double life = (time * (0.5 + 0.5 * noise(k * 7 + 1)) + noise(k * 7 + 2)) % 1;
            double sway = Math.sin(time * 3 + k) * 6 * unit;
            double x = cx + (noise(k * 7 + 3) - 0.5) * 30 * unit + sway;
            double y = cy - life * maxRadius * 0.9 * birth;
            g.setGlobalAlpha((1 - life) * 0.9 * fadeIn * birth);
            ParticleView.circle(g, x, y, (1.6 - life) * unit);
        }
        // Les ondes : un anneau de feu qui s'éteint.
        g.setLineWidth(3 * unit);
        for (double ringAge : view.rings()) {
            double t = ringAge / ParticleView.RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setStroke(EMBER.interpolate(EMBER_HOT, t));
            g.setGlobalAlpha((1 - t) * 0.8);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        if (core <= 0) return;
        // Le charbon : un noyau sombre craquelé, la lueur qui passe par les fentes.
        double pulse = 0.55 + 0.45 * Math.sin(time * 3.1);
        g.setGlobalAlpha(fadeIn);
        g.setFill(new RadialGradient(0, 0, cx, cy, core * 2.2, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.55, 0.1, 0.35 * pulse)), new Stop(1, Color.color(1, 0.3, 0, 0))));
        g.fillOval(cx - core * 2.2, cy - core * 2.2, core * 4.4, core * 4.4);
        g.setFill(EMBER_HOT.interpolate(Color.web("#ffe08a"), pulse));
        ParticleView.circle(g, cx, cy, core);
        g.setFill(Color.web("#2a0e08"));
        for (int k = 0; k < 6; k++) {
            // Six morceaux de charbon, séparés par des fentes qui laissent voir la lueur.
            double a0 = k * Math.PI / 3 + 0.12, a1 = a0 + Math.PI / 3 - 0.24;
            g.fillPolygon(new double[] {cx + core * 0.25 * Math.cos((a0 + a1) / 2), cx + core * Math.cos(a0), cx + core * Math.cos(a1)},
                    new double[] {cy + core * 0.25 * Math.sin((a0 + a1) / 2), cy + core * Math.sin(a0), cy + core * Math.sin(a1)}, 3);
        }
    }

    // ---- Encre -------------------------------------------------------------------------------------

    private static void ink(GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double core, boolean fast) {
        double unit = Math.max(scale, 0.8);
        double time = view.time();
        // Un rond de papier crème, aux fibres à peine visibles.
        double paper = maxRadius * 0.98 * (0.3 + 0.7 * birth);
        g.setGlobalAlpha(fadeIn);
        g.setFill(PAPER);
        ParticleView.circle(g, cx, cy, paper);
        g.setStroke(Color.web("#d9cfb4"));
        g.setLineWidth(unit * 0.6);
        g.setGlobalAlpha(0.5 * fadeIn);
        for (int k = 0; k < 7; k++) {
            double y = cy - paper + paper * 2 * (k + 0.5) / 7;
            double half = Math.sqrt(Math.max(0, paper * paper - (y - cy) * (y - cy))) * 0.9;
            g.strokeLine(cx - half, y, cx + half, y + 2 * unit * Math.sin(k * 1.7));
        }
        // Les gouttes : une tache qui se diffuse en halo pâle à mesure qu'elle coule vers le centre.
        for (Dust d : view.dust()) {
            double t = d.age / d.flight;
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t * 0.5;
            double x = cx + radius * Math.cos(angle), y = cy + radius * Math.sin(angle);
            double alpha = Math.min(1, t * 4) * fadeIn;
            double drop = d.size * unit * 0.9;
            g.setFill(INK);
            g.setGlobalAlpha(alpha * 0.12);
            ParticleView.circle(g, x, y, drop * (1.5 + 2.5 * t));
            g.setGlobalAlpha(alpha * 0.3);
            ParticleView.circle(g, x, y, drop * (1.2 + 0.8 * t));
            g.setGlobalAlpha(alpha * 0.9);
            ParticleView.circle(g, x, y, drop);
        }
        // Les ondes : un cerne d'encre qui s'élargit et pâlit.
        for (double ringAge : view.rings()) {
            double t = ringAge / ParticleView.RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setStroke(INK);
            g.setLineWidth((4 - 3 * t) * unit);
            g.setGlobalAlpha((1 - t) * 0.5);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        if (core <= 0) return;
        // La tache centrale : un halo de diffusion, puis un bord irrégulier, bleu nuit.
        g.setGlobalAlpha(fadeIn);
        g.setFill(new RadialGradient(0, 0, cx, cy, core * 2.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.08, 0.1, 0.23, 0.4)), new Stop(1, Color.color(0.08, 0.1, 0.23, 0))));
        g.fillOval(cx - core * 2.4, cy - core * 2.4, core * 4.8, core * 4.8);
        int points = 24;
        double[] xs = new double[points], ys = new double[points];
        for (int k = 0; k < points; k++) {
            double a = 2 * Math.PI * k / points;
            double wobble = 1 + 0.14 * Math.sin(a * 3 + time * (fast ? 4 : 0.7)) + 0.07 * Math.sin(a * 7 - time * 0.5);
            xs[k] = cx + core * wobble * Math.cos(a);
            ys[k] = cy + core * wobble * Math.sin(a);
        }
        g.setFill(INK);
        g.fillPolygon(xs, ys, points);
        g.setFill(Color.web("#3a4a8a"));
        g.setGlobalAlpha(0.5 * fadeIn);
        ParticleView.circle(g, cx - core * 0.3, cy - core * 0.3, core * 0.25);
    }

    // ---- Essaim ------------------------------------------------------------------------------------

    private static void swarm(GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double core) {
        double unit = Math.max(scale, 0.8);
        double time = view.time();
        double intensity = view.intensity();
        // La nuée permanente : des dizaines de points qui tournent autour du centre, chacun à son rayon et à sa
        // vitesse, en changeant de cap d'un coup de temps en temps.
        int count = 40 + (int) (40 * intensity);
        g.setFill(SWARM);
        for (int k = 0; k < count; k++) {
            double speed = (0.6 + 1.2 * noise(k * 5 + 1)) * (1 + 2 * intensity) * (k % 2 == 0 ? 1 : -1);
            double jolt = Math.floor(time * (0.5 + noise(k * 5 + 4))) * 0.8;
            double a = time * speed + noise(k * 5 + 2) * 2 * Math.PI + Math.sin(jolt + k) * 0.6;
            double r = maxRadius * (0.25 + 0.6 * noise(k * 5 + 3)) * (1 + 0.12 * Math.sin(time * 2.5 + k)) * birth;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            // Un petit trait dans le sens de la marche.
            double dx = -Math.sin(a) * speed, dy = Math.cos(a) * speed;
            double len = Math.hypot(dx, dy);
            g.setGlobalAlpha((0.45 + 0.5 * noise(k * 5 + 5)) * fadeIn);
            g.setStroke(SWARM);
            g.setLineWidth(1.1 * unit);
            g.strokeLine(x, y, x + dx / len * 3 * unit, y + dy / len * 3 * unit);
        }
        // Les poussières de la fusion : des points qui tournent beaucoup en convergeant.
        for (Dust d : view.dust()) {
            double t = d.age / d.flight;
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t * 5;
            double x = cx + radius * Math.cos(angle), y = cy + radius * Math.sin(angle);
            g.setGlobalAlpha(Math.min(1, t * 4) * fadeIn);
            ParticleView.circle(g, x, y, d.size * unit * 0.45);
        }
        // Les ondes : un cercle de points qui s'éparpille.
        g.setLineWidth(1.5 * unit);
        g.setStroke(SWARM);
        for (double ringAge : view.rings()) {
            double t = ringAge / ParticleView.RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setGlobalAlpha(1 - t);
            ParticleView.dashedCircle(g, cx, cy, radius, 24, t * 2);
        }
        if (core <= 0) return;
        // La ruche : un hexagone d'alvéoles, et une couronne de points serrés qui la frôlent.
        g.setGlobalAlpha(fadeIn);
        g.setFill(Color.web("#b8a040"));
        ParticleView.polygon(g, cx, cy, core * 1.3, 6, Math.PI / 6, true);
        g.setStroke(Color.web("#5a4a10"));
        g.setLineWidth(unit);
        ParticleView.polygon(g, cx, cy, core * 1.3, 6, Math.PI / 6, false);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            ParticleView.polygon(g, cx + core * 0.7 * Math.cos(a), cy + core * 0.7 * Math.sin(a), core * 0.38, 6, Math.PI / 6, false);
        }
        ParticleView.polygon(g, cx, cy, core * 0.38, 6, Math.PI / 6, false);
        g.setFill(SWARM);
        for (int k = 0; k < 12; k++) {
            double a = time * 4 + k * Math.PI / 6;
            double r = core * (1.6 + 0.2 * Math.sin(time * 7 + k));
            ParticleView.circle(g, cx + r * Math.cos(a), cy + r * Math.sin(a), 1.2 * unit);
        }
    }

    // ---- Horloge -----------------------------------------------------------------------------------

    private static void clock(GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double progress, boolean fast, double core) {
        double unit = Math.max(scale, 0.8);
        double time = view.time();
        double intensity = view.intensity();
        double dial = maxRadius * 0.9 * birth;
        // Le cadran : son cercle, soixante graduations, douze plus longues.
        g.setGlobalAlpha(0.85 * fadeIn);
        g.setStroke(IVORY);
        g.setLineWidth(1.5 * unit);
        g.strokeOval(cx - dial, cy - dial, dial * 2, dial * 2);
        for (int k = 0; k < 60; k++) {
            double a = k * Math.PI / 30;
            double inner = dial * (k % 5 == 0 ? 0.88 : 0.95);
            g.setLineWidth((k % 5 == 0 ? 1.5 : 0.8) * unit);
            g.strokeLine(cx + inner * Math.cos(a), cy + inner * Math.sin(a), cx + dial * Math.cos(a), cy + dial * Math.sin(a));
        }
        // Les aiguilles : les heures suivent le temps, les minutes l'avancement de la particule, et la
        // trotteuse saute d'un cran à chaque tic, d'autant plus vite que le générateur va vite.
        double hour = -Math.PI / 2 + time * 0.05;
        double minute = -Math.PI / 2 + 2 * Math.PI * (fast ? (time * 0.5) % 1 : progress);
        double second = -Math.PI / 2 + Math.floor(time * (2 + 10 * intensity)) * Math.PI / 30;
        double[] hands = {hour, minute, second};
        double[] lengths = {0.5, 0.75, 0.85};
        for (int h = 0; h < 3; h++) {
            g.setStroke(h == 2 ? Color.web("#d04a3a") : BRASS);
            g.setLineWidth((h == 0 ? 3.5 : h == 1 ? 2.5 : 1.2) * unit);
            g.setGlobalAlpha(fadeIn);
            g.strokeLine(cx, cy, cx + dial * lengths[h] * Math.cos(hands[h]), cy + dial * lengths[h] * Math.sin(hands[h]));
        }
        // Les poussières : portées par une aiguille, elles glissent le long jusqu'au pivot.
        g.setFill(Color.web("#fff0b0"));
        for (Dust d : view.dust()) {
            double t = d.age / d.flight;
            int hand = (int) Math.min(2, Math.floor(d.angle / (2 * Math.PI) * 3));
            double radius = dial * lengths[hand] * d.distance * (1 - t * t);
            double x = cx + radius * Math.cos(hands[hand]), y = cy + radius * Math.sin(hands[hand]);
            g.setGlobalAlpha(Math.min(1, t * 4) * fadeIn);
            ParticleView.circle(g, x, y, d.size * unit * 0.7);
        }
        // Les ondes : le tic, un cercle fin qui part du pivot.
        g.setStroke(IVORY);
        g.setLineWidth(unit);
        for (double ringAge : view.rings()) {
            double t = ringAge / ParticleView.RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setGlobalAlpha(1 - t);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        if (core <= 0) return;
        // Le pivot : un disque de laiton qui grossit avec l'avancement, un axe sombre au milieu.
        g.setGlobalAlpha(fadeIn);
        g.setFill(BRASS);
        ParticleView.circle(g, cx, cy, core);
        g.setFill(Color.web("#7a5a18"));
        ParticleView.circle(g, cx, cy, core * 0.55);
        g.setFill(Color.web("#2a2018"));
        ParticleView.circle(g, cx, cy, Math.max(1.2 * unit, core * 0.2));
    }

    // ---- Verre soufflé -----------------------------------------------------------------------------

    private static void glass(GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double core) {
        double unit = Math.max(scale, 0.8);
        double time = view.time();
        // Les bulles : translucides, irisées d'une teinte lente, cerclées de blanc, un reflet en haut à gauche ;
        // près du centre, elles éclatent en un anneau qui s'élargit.
        for (Dust d : view.dust()) {
            double t = d.age / d.flight;
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t * 0.7;
            double x = cx + radius * Math.cos(angle), y = cy + radius * Math.sin(angle);
            double r = d.size * unit * 2.2;
            Color tint = Color.hsb((time * 25 + Math.toDegrees(d.angle)) % 360, 0.5, 1);
            if (t > 0.85) {
                double pop = (t - 0.85) / 0.15;
                g.setStroke(Color.WHITE);
                g.setLineWidth(unit * (1.5 - pop));
                g.setGlobalAlpha((1 - pop) * fadeIn);
                ParticleView.dashedCircle(g, x, y, r * (1 + 1.5 * pop), 8, pop);
                continue;
            }
            double alpha = Math.min(1, t * 4) * fadeIn;
            g.setFill(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.22));
            g.setGlobalAlpha(alpha);
            ParticleView.circle(g, x, y, r);
            g.setStroke(Color.color(1, 1, 1, 0.6));
            g.setLineWidth(0.8 * unit);
            g.strokeOval(x - r, y - r, r * 2, r * 2);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(alpha * 0.85);
            ParticleView.circle(g, x - r * 0.4, y - r * 0.4, r * 0.25);
        }
        // Les ondes : des gouttelettes projetées en cercle.
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5 * unit);
        for (double ringAge : view.rings()) {
            double t = ringAge / ParticleView.RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setGlobalAlpha(1 - t);
            ParticleView.dashedCircle(g, cx, cy, radius, 10, t * 0.5);
        }
        if (core <= 0) return;
        // La goutte de verre : une grande bulle irisée qui ondule un peu, son reflet blanc.
        double wobble = 1 + 0.06 * Math.sin(time * 2.7);
        double rx = core * 1.25 * wobble, ry = core * 1.25 / wobble;
        Color tint = Color.hsb((time * 25) % 360, 0.45, 1);
        g.setGlobalAlpha(fadeIn);
        g.setFill(new RadialGradient(0, 0, cx + rx * 0.25, cy + ry * 0.25, Math.max(1, rx), false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 1, 1, 0.08)), new Stop(0.75, Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.3)),
                new Stop(1, Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.55))));
        g.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);
        g.setStroke(Color.color(1, 1, 1, 0.8));
        g.setLineWidth(1.2 * unit);
        g.strokeOval(cx - rx, cy - ry, rx * 2, ry * 2);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.9 * fadeIn);
        g.fillOval(cx - rx * 0.55, cy - ry * 0.65, rx * 0.4, ry * 0.25);
    }

    // ---- Pixel -------------------------------------------------------------------------------------

    private static void pixel(GraphicsContext g, ParticleView view, double cx, double cy, double maxRadius, double scale,
            double birth, double fadeIn, double core) {
        // Le pas de la grille : des carrés de quatre pixels à la taille de référence, jamais moins de deux.
        double px = Math.max(2, Math.round(4 * scale));
        double time = view.time();
        double intensity = view.intensity();
        g.setGlobalAlpha(fadeIn);
        // Un halo en anneaux carrés concentriques, bleu nuit, dont le nombre suit la vitesse.
        int halos = 1 + (int) (3 * intensity);
        for (int k = 0; k < halos; k++) {
            double r = maxRadius * (0.35 + 0.15 * k) * birth;
            pixelRing(g, cx, cy, r, px, PALETTE[1]);
        }
        // Les poussières : des carrés, bleu puis bleu clair puis blanc en approchant.
        for (Dust d : view.dust()) {
            double t = d.age / d.flight;
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t;
            double x = cx + radius * Math.cos(angle), y = cy + radius * Math.sin(angle);
            g.setFill(PALETTE[t < 0.4 ? 2 : t < 0.8 ? 3 : 4]);
            g.setGlobalAlpha(t < 0.25 ? 0 : fadeIn);
            double side = px * (d.size > 2.2 ? 2 : 1);
            g.fillRect(snap(x, px), snap(y, px), side, side);
        }
        // Les ondes : des anneaux de carrés jaunes puis orange.
        for (double ringAge : view.rings()) {
            double t = ringAge / ParticleView.RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setGlobalAlpha(t < 0.5 ? 1 : 0.5);
            pixelRing(g, cx, cy, radius, px, PALETTE[t < 0.5 ? 5 : 6]);
        }
        // La naissance : un anneau rouge qui se resserre.
        if (birth < 1) {
            double radius = maxRadius * 0.95 + (16 * scale - maxRadius * 0.95) * birth * birth;
            g.setGlobalAlpha(fadeIn);
            pixelRing(g, cx, cy, radius, px, PALETTE[7]);
        }
        if (core <= 0) return;
        // Le noyau : un disque en marches d'escalier, bleu clair cerné de bleu, blanc au centre ; les arcs d'énergie
        // en escaliers de carrés blancs.
        g.setGlobalAlpha(fadeIn);
        pixelDisc(g, cx, cy, core * 1.6, px, PALETTE[2]);
        pixelDisc(g, cx, cy, core * 1.2, px, PALETTE[3]);
        pixelDisc(g, cx, cy, core, px, PALETTE[4]);
        g.setFill(PALETTE[4]);
        for (double[] arc : view.arcs()) {
            int steps = 1 + (int) (maxRadius * 0.7 / px);
            for (int k = 0; k < steps; k++) {
                double r = k * px;
                double a = arc[0] + 0.3 * Math.sin(k * 0.7 + time);
                g.fillRect(snap(cx + r * Math.cos(a), px), snap(cy + r * Math.sin(a), px), px, px);
            }
        }
    }

    /** La coordonnée ramenée sur la grille de pas {@code px}. */
    private static double snap(double v, double px) {
        return Math.floor(v / px) * px;
    }

    /** Un anneau de carrés alignés sur la grille. */
    private static void pixelRing(GraphicsContext g, double cx, double cy, double r, double px, Color color) {
        if (r < px) return;
        g.setFill(color);
        int steps = (int) Math.max(8, 2 * Math.PI * r / px);
        for (int k = 0; k < steps; k++) {
            double a = 2 * Math.PI * k / steps;
            g.fillRect(snap(cx + r * Math.cos(a), px), snap(cy + r * Math.sin(a), px), px, px);
        }
    }

    /** Un disque en marches d'escalier : une rangée de carrés par ligne de la grille. */
    private static void pixelDisc(GraphicsContext g, double cx, double cy, double r, double px, Color color) {
        g.setFill(color);
        double top = snap(cy - r, px);
        for (double y = top; y < cy + r; y += px) {
            double dy = y + px / 2 - cy;
            double half = Math.sqrt(Math.max(0, r * r - dy * dy));
            if (half < px / 2) continue;
            double left = snap(cx - half, px), right = snap(cx + half, px) + px;
            g.fillRect(left, y, right - left, px);
        }
    }
}
