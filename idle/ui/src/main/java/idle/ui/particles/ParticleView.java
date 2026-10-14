package idle.ui.particles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Animation d'un générateur : des poussières convergent vers le centre,
 * le noyau grossit, et quand il est complet une particule est créée (onde + remise à zéro).
 *
 * <p>Depuis la version 0.61, l'allure suit la vitesse du générateur ({@link #intensity(double)}) :
 * lent, des poussières bleues et un noyau calme ; plus vite, les poussières laissent des traînées
 * et tournent en spirale, des anneaux d'accrétion apparaissent et tournent, le noyau bat plus
 * vite et se couronne de rayons ; très vite, la couleur passe du bleu au violet puis à l'or, et des
 * arcs d'énergie jaillissent du noyau.
 *
 * <p>À sa création, le générateur joue une animation de naissance : un cercle se
 * resserre sur le centre pendant qu'une salve de poussières s'y précipite, puis un
 * éclat marque l'allumage.
 *
 * <p>Purement décoratif : cette classe ne modifie jamais le jeu, elle reçoit
 * seulement l'avancement et la vitesse de création à chaque image.
 */
public final class ParticleView extends Canvas {

    /** Taille pour laquelle les dimensions ci-dessous sont exprimées ; le reste est mis à l'échelle. */
    private static final double REFERENCE_SIZE = 200;
    /** Nombre de poussières qui arrivent pendant la formation d'une particule. */
    private static final double DUST_PER_PARTICLE = 20;
    private static final double MAX_DUST_PER_SECOND = 200;
    /** Durée du trajet d'une poussière jusqu'au centre, en secondes. */
    private static final double FLIGHT_SECONDS = 1.6;
    /** Au-delà de cette vitesse, on n'anime plus les particules une par une. */
    private static final double FAST_RATE = 6;
    private static final double RING_SECONDS = 0.6;

    /** Durée de l'animation de naissance, en secondes. */
    private static final double BIRTH_SECONDS = 1.2;
    /** Nombre de poussières de la salve de naissance. */
    private static final int BIRTH_DUST = 45;
    /** Durée de l'éclat qui suit la naissance, en secondes. */
    private static final double FLASH_SECONDS = 0.5;

    private static final Color DUST = Color.web("#9fd0ff");
    private static final Color CORE = Color.web("#e8f4ff");
    /** Les couleurs de la vitesse, de la plus lente à la plus rapide. */
    private static final Color[] HEAT = {Color.web("#9fd0ff"), Color.web("#7fe8ff"), Color.web("#c9a6ff"), Color.web("#ffd27f")};

    private final List<Dust> dust = new ArrayList<>();
    private final List<Double> rings = new ArrayList<>(); // âge de chaque onde, en secondes
    private final Random random = new Random();

    private double spawnBudget = 0;
    private double lastProgress = 0;
    private double sinceLastRing = 1;
    private double time = 0;
    private double age = 0; // temps écoulé depuis la naissance du générateur
    /** La vitesse du générateur ramenée de 0 à 1, adoucie d'une image à l'autre. */
    private double intensity = 0;
    /** Faux si le joueur a coupé l'allure des générateurs : ils gardent alors leur allure calme, quelle que soit la vitesse. */
    private static boolean speedLook = true;

    /** Faux si le joueur a coupé la création de particules : ni onde ni poussières, le noyau seul suit l'avancement. */
    private static boolean creationLook = true;

    /** Allume ou coupe l'onde et les poussières de chaque création (réglage « Création de particules »). */
    public static void setCreationLook(boolean on) {
        creationLook = on;
    }

    /** Allume ou coupe l'allure qui suit la vitesse, pour tous les générateurs (réglage « Allure des générateurs »). */
    public static void setSpeedLook(boolean on) {
        speedLook = on;
    }
    /** L'angle des anneaux d'accrétion, qui tournent plus vite avec la vitesse. */
    private double spin = 0;
    /** Les arcs d'énergie en cours : leur angle et leur âge. */
    private final List<double[]> arcs = new ArrayList<>();

    /** La taille réelle est fixée ensuite par {@link GeneratorPane}, selon la place disponible. */
    ParticleView() {
        super(REFERENCE_SIZE, REFERENCE_SIZE);
        // Salve de naissance : des poussières parties du bord, qui arrivent toutes avant la fin.
        for (int i = 0; i < BIRTH_DUST; i++) {
            Dust d = newDust();
            d.distance = 0.8 + 0.2 * random.nextDouble();
            d.flight = BIRTH_SECONDS * (0.5 + 0.5 * random.nextDouble());
            dust.add(d);
        }
    }

    /**
     * Avance l'animation et redessine.
     *
     * @param elapsed  temps écoulé depuis la dernière image, en secondes
     * @param progress avancement de la particule en cours, de 0 à 1
     * @param rate     particules créées par seconde sur ce générateur
     */
    public void frame(double elapsed, double progress, double rate) {
        double dt = Math.min(elapsed, 0.1); // après une pause (fenêtre déplacée…), on évite une rafale
        time += dt;
        sinceLastRing += dt;
        boolean fast = rate > FAST_RATE;
        // La vitesse s'installe en une demi-seconde : pas de saut quand un palier l'accélère d'un coup.
        // Un effet coupé l'est tout de suite : rien ne finit de s'éteindre (version 0.67).
        if (speedLook) intensity += (intensity(rate) - intensity) * Math.min(1, dt * 2);
        else intensity = 0;
        if (!speedLook) arcs.clear();
        if (!creationLook) {
            dust.clear();
            rings.clear();
        }
        spin += dt * (0.3 + 4 * intensity);
        // Très vite : des arcs d'énergie, de plus en plus fréquents.
        if (intensity > 0.6 && random.nextDouble() < dt * 6 * (intensity - 0.6) / 0.4) {
            arcs.add(new double[] {random.nextDouble() * 2 * Math.PI, 0});
        }
        for (double[] arc : arcs) arc[1] += dt;
        arcs.removeIf(arc -> arc[1] >= 0.25);

        // Fin de la naissance : une onde part du centre.
        boolean justBorn = age < BIRTH_SECONDS && age + dt >= BIRTH_SECONDS;
        age += dt;

        // Une particule vient d'être créée : l'avancement est reparti de zéro.
        boolean created = progress < lastProgress || rate * dt >= 1;
        lastProgress = progress;
        if (creationLook && (justBorn || (created && sinceLastRing > (fast ? 0.2 : 0)))) {
            rings.add(0.0);
            sinceLastRing = 0;
        }

        if (creationLook) spawnDust(dt, rate);
        dust.removeIf(d -> (d.age += dt) >= d.flight);
        rings.replaceAll(ringAge -> ringAge + dt);
        rings.removeIf(ringAge -> ringAge >= RING_SECONDS);

        draw(fast ? 1 : progress, fast);
    }

    /** La vitesse ramenée de 0 à 1 : 0 pour une particule toutes les dix secondes, 1 à partir de mille par seconde. */
    public static double intensity(double rate) {
        if (!(rate > 0)) return 0;
        return Math.max(0, Math.min(1, (Math.log10(rate) + 1) / 4));
    }

    /** La couleur de cette vitesse, entre le bleu du calme et l'or de la très grande vitesse. */
    static Color heat(double intensity) {
        double at = Math.max(0, Math.min(1, intensity)) * (HEAT.length - 1);
        int low = (int) Math.min(HEAT.length - 2, Math.floor(at));
        return HEAT[low].interpolate(HEAT[low + 1], at - low);
    }

    /** La vitesse lissée, de 0 à 1 : pour les vérifications. */
    public double intensity() {
        return intensity;
    }

    private void spawnDust(double dt, double rate) {
        // Très vite, moins de poussières, mais chacune avec sa traînée : la vue reste lisible.
        spawnBudget += dt * Math.min(rate * DUST_PER_PARTICLE, MAX_DUST_PER_SECOND * (1 - 0.6 * intensity));
        while (spawnBudget >= 1) {
            spawnBudget -= 1;
            dust.add(newDust());
        }
    }

    private Dust newDust() {
        Dust d = new Dust();
        d.angle = random.nextDouble() * 2 * Math.PI;
        d.distance = 0.6 + 0.4 * random.nextDouble();
        // Plus le générateur va vite, plus les poussières tournent avant d'arriver.
        d.swirl = (0.6 + 0.8 * random.nextDouble()) * (1 + 1.5 * intensity);
        d.size = 1.5 + 1.5 * random.nextDouble();
        d.flight = FLIGHT_SECONDS;
        return d;
    }

    private void draw(double progress, boolean fast) {
        GraphicsContext g = getGraphicsContext2D();
        double cx = getWidth() / 2;
        double cy = getHeight() / 2;
        // Tout est exprimé pour une vue de REFERENCE_SIZE pixels, puis mis à l'échelle de la vue réelle.
        double maxRadius = Math.min(getWidth(), getHeight()) / 2;
        double scale = maxRadius * 2 / REFERENCE_SIZE;

        // Naissance : 0 au tout début, 1 une fois le générateur allumé.
        double birth = Math.min(1, age / BIRTH_SECONDS);
        double fadeIn = Math.min(1, age / 0.25);

        // Fond transparent : on voit les poussières de fond à travers.
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, getWidth(), getHeight());

        Color color = heat(intensity);

        // Un halo de fond, plus large et plus vif avec la vitesse.
        double halo = maxRadius * (0.45 + 0.4 * intensity) * birth;
        if (halo > 1) {
            g.setGlobalAlpha(fadeIn);
            g.setFill(new RadialGradient(0, 0, cx, cy, halo, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.10 + 0.18 * intensity)),
                    new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
            g.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
        }

        // Les anneaux d'accrétion : un dès que ça s'accélère, trois à pleine vitesse, inclinés, qui tournent.
        int discs = intensity < 0.2 ? 0 : intensity < 0.5 ? 1 : intensity < 0.8 ? 2 : 3;
        for (int k = 0; k < discs; k++) {
            double rx = maxRadius * (0.42 + 0.14 * k) * birth, ry = rx * (0.28 + 0.08 * k);
            double tilt = spin * (k % 2 == 0 ? 1 : -0.7) * 0.15 + k * 1.1;
            ellipse(g, cx, cy, rx, ry, tilt, color, Math.max(0.8, scale), (0.18 + 0.2 * intensity) * fadeIn);
            // Un grain brillant qui suit chaque anneau.
            double a = spin * (1.2 + 0.5 * k) + k * 2.1;
            double px = rx * Math.cos(a), py = ry * Math.sin(a);
            double x = cx + px * Math.cos(tilt) - py * Math.sin(tilt), y = cy + px * Math.sin(tilt) + py * Math.cos(tilt);
            g.setFill(CORE);
            g.setGlobalAlpha(0.9 * fadeIn);
            circle(g, x, y, 1.8 * Math.max(scale, 0.8));
        }

        // Poussières : elles accélèrent en approchant du centre, en tournant ; vite, elles laissent une traînée.
        g.setFill(color);
        g.setStroke(color);
        for (Dust d : dust) {
            double t = d.age / d.flight;                // 0 = départ, 1 = arrivée
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t;
            double x = cx + radius * Math.cos(angle), y = cy + radius * Math.sin(angle);
            double alpha = Math.min(1, t * 4) * fadeIn;         // apparition en fondu
            if (intensity > 0.25) {
                double back = Math.max(0, t - 0.04 - 0.08 * intensity);
                double radiusBack = maxRadius * d.distance * (1 - back * back);
                double angleBack = d.angle + d.swirl * back;
                g.setGlobalAlpha(alpha * 0.3);
                g.setLineWidth(d.size * Math.max(scale, 0.8) * 0.6);
                g.strokeLine(cx + radiusBack * Math.cos(angleBack), cy + radiusBack * Math.sin(angleBack), x, y);
            }
            g.setGlobalAlpha(alpha);
            circle(g, x, y, d.size * Math.max(scale, 0.8) / 2);
        }

        // Naissance : un cercle part du bord et se resserre sur le centre.
        if (birth < 1) {
            double radius = maxRadius * 0.95 + (16 * scale - maxRadius * 0.95) * birth * birth;
            g.setStroke(DUST);
            g.setLineWidth(1.5 * Math.max(scale, 0.75));
            g.setGlobalAlpha((0.15 + 0.75 * birth) * fadeIn);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        // Ondes émises à l'allumage, puis à chaque particule créée, dans la couleur de la vitesse.
        g.setStroke(CORE.interpolate(color, 0.5));
        g.setLineWidth(2 * Math.max(scale, 0.75));
        for (double ringAge : rings) {
            double t = ringAge / RING_SECONDS;
            double radius = (16 + 60 * t) * scale;
            g.setGlobalAlpha(1 - t);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        // Éclat juste après l'allumage : un halo blanc qui s'élargit en s'effaçant.
        double sinceBirth = age - BIRTH_SECONDS;
        if (sinceBirth >= 0 && sinceBirth < FLASH_SECONDS) {
            double t = sinceBirth / FLASH_SECONDS;
            g.setFill(CORE);
            g.setGlobalAlpha(0.45 * (1 - t));
            circle(g, cx, cy, (22 + 45 * t) * scale);
        }

        // Noyau : grossit avec l'avancement, entouré d'un halo. Il apparaît pendant la naissance, et bat
        // d'autant plus vite que le générateur va vite.
        double coreRadius = 3 + 13 * Math.sqrt(progress);
        if (fast) coreRadius += (1.5 + 1.5 * intensity) * Math.sin(time * (8 + 16 * intensity));
        coreRadius *= scale * birth * birth;
        // La couronne : des rayons qui tournent, plus longs avec la vitesse.
        if (intensity > 0.4 && coreRadius > 0) {
            int rays = 8;
            double length = coreRadius * (1.2 + 2.2 * (intensity - 0.4) / 0.6);
            g.setStroke(color);
            g.setLineWidth(1.4 * Math.max(scale, 0.75));
            for (int k = 0; k < rays; k++) {
                double a = spin * 0.6 + k * 2 * Math.PI / rays;
                double flicker = 0.7 + 0.3 * Math.sin(time * 11 + k * 1.7);
                g.setGlobalAlpha(0.5 * flicker * fadeIn);
                g.strokeLine(cx + coreRadius * 1.1 * Math.cos(a), cy + coreRadius * 1.1 * Math.sin(a),
                        cx + (coreRadius + length * flicker) * Math.cos(a), cy + (coreRadius + length * flicker) * Math.sin(a));
            }
        }
        // Les arcs d'énergie : une ligne brisée du noyau vers le bord, qui s'éteint en un quart de seconde.
        for (double[] arc : arcs) {
            double fade = 1 - arc[1] / 0.25;
            double previousX = cx, previousY = cy;
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.2 * Math.max(scale, 0.75));
            g.setGlobalAlpha(0.8 * fade * fadeIn);
            for (int k = 1; k <= 5; k++) {
                double r = maxRadius * 0.75 * k / 5;
                double a = arc[0] + 0.25 * Math.sin(k * 3.1 + arc[0] * 7);
                double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
                g.strokeLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
        }
        g.setFill(color);
        g.setGlobalAlpha(0.08);
        circle(g, cx, cy, coreRadius * 2.6);
        g.setGlobalAlpha(0.18);
        circle(g, cx, cy, coreRadius * 1.7);
        g.setFill(CORE);
        g.setGlobalAlpha(1);
        circle(g, cx, cy, coreRadius);
    }

    /** Une ellipse inclinée, tracée point par point. */
    private static void ellipse(GraphicsContext g, double cx, double cy, double rx, double ry, double tilt, Color color, double width, double alpha) {
        int steps = 48;
        double[] xs = new double[steps + 1], ys = new double[steps + 1];
        for (int k = 0; k <= steps; k++) {
            double a = 2 * Math.PI * k / steps;
            double px = rx * Math.cos(a), py = ry * Math.sin(a);
            xs[k] = cx + px * Math.cos(tilt) - py * Math.sin(tilt);
            ys[k] = cy + px * Math.sin(tilt) + py * Math.cos(tilt);
        }
        g.setStroke(color);
        g.setLineWidth(width);
        g.setGlobalAlpha(alpha);
        g.strokePolyline(xs, ys, steps + 1);
    }

    private static void circle(GraphicsContext g, double x, double y, double radius) {
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    /** Une poussière en route vers le centre. */
    private static final class Dust {
        double angle;
        double distance; // distance de départ, en fraction du rayon de la vue
        double swirl;
        double size;
        double flight;   // durée totale du trajet, en secondes
        double age;
    }
}
