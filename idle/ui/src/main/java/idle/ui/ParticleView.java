package idle.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Animation d'un générateur : des poussières convergent vers le centre,
 * le noyau grossit, et quand il est complet une particule est créée (onde + remise à zéro).
 *
 * <p>À sa création, le générateur joue une animation de naissance : un cercle se
 * resserre sur le centre pendant qu'une salve de poussières s'y précipite, puis un
 * éclat marque l'allumage.
 *
 * <p>Purement décoratif : cette classe ne modifie jamais le jeu, elle reçoit
 * seulement l'avancement et la vitesse de création à chaque image.
 */
final class ParticleView extends Canvas {

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

    private final List<Dust> dust = new ArrayList<>();
    private final List<Double> rings = new ArrayList<>(); // âge de chaque onde, en secondes
    private final Random random = new Random();

    private double spawnBudget = 0;
    private double lastProgress = 0;
    private double sinceLastRing = 1;
    private double time = 0;
    private double age = 0; // temps écoulé depuis la naissance du générateur

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
    void frame(double elapsed, double progress, double rate) {
        double dt = Math.min(elapsed, 0.1); // après une pause (fenêtre déplacée…), on évite une rafale
        time += dt;
        sinceLastRing += dt;
        boolean fast = rate > FAST_RATE;

        // Fin de la naissance : une onde part du centre.
        boolean justBorn = age < BIRTH_SECONDS && age + dt >= BIRTH_SECONDS;
        age += dt;

        // Une particule vient d'être créée : l'avancement est reparti de zéro.
        boolean created = progress < lastProgress || rate * dt >= 1;
        lastProgress = progress;
        if (justBorn || (created && sinceLastRing > (fast ? 0.2 : 0))) {
            rings.add(0.0);
            sinceLastRing = 0;
        }

        spawnDust(dt, rate);
        dust.removeIf(d -> (d.age += dt) >= d.flight);
        rings.replaceAll(ringAge -> ringAge + dt);
        rings.removeIf(ringAge -> ringAge >= RING_SECONDS);

        draw(fast ? 1 : progress, fast);
    }

    private void spawnDust(double dt, double rate) {
        spawnBudget += dt * Math.min(rate * DUST_PER_PARTICLE, MAX_DUST_PER_SECOND);
        while (spawnBudget >= 1) {
            spawnBudget -= 1;
            dust.add(newDust());
        }
    }

    private Dust newDust() {
        Dust d = new Dust();
        d.angle = random.nextDouble() * 2 * Math.PI;
        d.distance = 0.6 + 0.4 * random.nextDouble();
        d.swirl = 0.6 + 0.8 * random.nextDouble();
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

        // Poussières : elles accélèrent en approchant du centre, en tournant légèrement.
        g.setFill(DUST);
        for (Dust d : dust) {
            double t = d.age / d.flight;                // 0 = départ, 1 = arrivée
            double radius = maxRadius * d.distance * (1 - t * t);
            double angle = d.angle + d.swirl * t;
            g.setGlobalAlpha(Math.min(1, t * 4) * fadeIn); // apparition en fondu
            circle(g, cx + radius * Math.cos(angle), cy + radius * Math.sin(angle),
                    d.size * Math.max(scale, 0.8) / 2);
        }

        // Naissance : un cercle part du bord et se resserre sur le centre.
        if (birth < 1) {
            double radius = maxRadius * 0.95 + (16 * scale - maxRadius * 0.95) * birth * birth;
            g.setStroke(DUST);
            g.setLineWidth(1.5 * Math.max(scale, 0.75));
            g.setGlobalAlpha((0.15 + 0.75 * birth) * fadeIn);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        // Ondes émises à l'allumage, puis à chaque particule créée.
        g.setStroke(CORE);
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

        // Noyau : grossit avec l'avancement, entouré d'un halo. Il apparaît pendant la naissance.
        double coreRadius = 3 + 13 * Math.sqrt(progress);
        if (fast) coreRadius += 1.5 * Math.sin(time * 8);
        coreRadius *= scale * birth * birth;
        g.setFill(DUST);
        g.setGlobalAlpha(0.08);
        circle(g, cx, cy, coreRadius * 2.6);
        g.setGlobalAlpha(0.18);
        circle(g, cx, cy, coreRadius * 1.7);
        g.setFill(CORE);
        g.setGlobalAlpha(1);
        circle(g, cx, cy, coreRadius);
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