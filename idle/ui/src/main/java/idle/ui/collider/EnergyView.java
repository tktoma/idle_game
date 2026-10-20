package idle.ui.collider;

import idle.core.Game;
import idle.core.collider.Quark;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * L'anneau d'énergie du collisionneur (version 0.57, collisions en 0.59) : un grand anneau ovale où
 * deux faisceaux tournent en sens contraires. Chaque fois qu'ils se croisent dans le détecteur, des
 * étincelles jaillissent ; quand le croisement donne une collision, une onde part du détecteur et
 * le quark obtenu s'envole, de la couleur de sa saveur, sa lettre dessus. Un clic au moment du
 * croisement capte {@link Game#CROSSING_SECONDS} secondes de production et assure une collision
 * au butin doublé.
 *
 * <p>Version 0.78 : lancées assez vite (« Faisceaux rapides » au niveau 3), les boules prennent
 * feu, une flamme qui traîne derrière elles et grandit avec la vitesse ; et à chaque croisement,
 * de l'électricité crépite entre elles.
 */
public final class EnergyView extends Canvas {

    private static final Color BACK = Color.web("#060b14");
    private static final Color RING = Color.web("#173250");
    private static final Color LIGHT = Color.web("#9ad0ff");
    private static final Color BEAM_A = Color.web("#6fe3ff");
    private static final Color BEAM_B = Color.web("#ff7ad9");
    private static final Color GOLD = Color.web("#ffd27f");
    private static final Color FLAME_CORE = Color.web("#fff3b0");
    private static final Color FLAME_MID = Color.web("#ffa53a");
    private static final Color FLAME_EDGE = Color.web("#ff4a1c");

    /**
     * Vitesse des faisceaux (par rapport à celle de départ) à partir de laquelle les boules
     * prennent feu, et celle où la flamme est à son plein : ×1,5 et ×2, soit les niveaux 3 et 5
     * de « Faisceaux rapides ».
     */
    static final double FLAME_FROM = 1.5;
    static final double FLAME_FULL = 2;
    private static final int STEPS = 72;
    private static final double[] XS = new double[STEPS + 1];
    private static final double[] YS = new double[STEPS + 1];

    /** Une étincelle, un « +12 énergie » qui monte et s'efface, ou un quark (sa lettre dans un rond). */
    private static final class Spark {
        double x, y, vx, vy, life, age, size;
        Color color;
        String text;
        String quark;
    }

    /** L'onde d'une collision, qui s'élargit autour du détecteur. */
    private static final class Wave {
        double age;
        Color color;
        boolean caught;
    }

    /** La couleur d'une saveur de quark : la même partout dans l'onglet. */
    public static Color color(Quark quark) {
        return Color.web(ColliderPane.quarkColor(quark));
    }

    private final Game game;
    private final List<Spark> sparks = new ArrayList<>();
    /** Faux si le joueur a coupé les étincelles : le détecteur ne jaillit plus, l'énergie gagnée reste écrite. */
    private static boolean sparksOn = true;

    /** Faux si le joueur a coupé l'animation du collisionneur : ni ondes ni envol, l'anneau ne scintille plus ; les faisceaux restent. */
    private static boolean motion = true;

    /** Allume ou coupe l'animation du collisionneur (réglage « Animations du collisionneur »). */
    public static void setMotion(boolean on) {
        motion = on;
    }

    /** Allume ou coupe les gerbes d'étincelles du détecteur (réglage « Étincelles du collisionneur »). */
    public static void setSparks(boolean on) {
        sparksOn = on;
    }
    private final List<Wave> waves = new ArrayList<>();
    /** Le numéro de la dernière collision dessinée ; −1 avant la première image, pour ne pas rejouer les anciennes. */
    private long lastSerial = -1;
    private int collisionsShown;
    private double time;
    private boolean wasCrossing;
    private double lastGain;

    /** @param onCatch ce qui suit un clic capté : la page se recopie (l'énergie gagnée en argument) */
    EnergyView(Game game, DoubleConsumer onCatch) {
        super(780, 170);
        this.game = game;
        setOnMouseClicked(event -> {
            double gain = game.catchCrossing();
            if (gain > 0) {
                lastGain = gain;
                burst(true, gain);
            }
            onCatch.accept(gain);
        });
    }

    /**
     * La force de la flamme des boules, de 0 (pas de flamme) à 1 : elle naît quand les faisceaux
     * tournent {@link #FLAME_FROM} fois plus vite qu'au départ et grandit jusqu'à {@link #FLAME_FULL}.
     */
    public double flameHeat() {
        double speed = Game.RESONANCE_PERIOD / game.colliderPeriod();
        if (speed < FLAME_FROM - 1e-9) return 0;
        return Math.min(1, 0.2 + 0.8 * (speed - FLAME_FROM) / (FLAME_FULL - FLAME_FROM));
    }

    /** L'énergie du dernier croisement capté : pour les vérifications. */
    public double lastGain() {
        return lastGain;
    }

    public int sparks() {
        return sparks.size();
    }

    /** Collisions montrées depuis l'ouverture : pour les vérifications. */
    public int collisionsShown() {
        return collisionsShown;
    }

    private double cx() {
        return getWidth() / 2;
    }

    private double cy() {
        return getHeight() / 2 - 6;
    }

    private double rx() {
        return Math.min(getWidth() * 0.36, 300);
    }

    private double ry() {
        return getHeight() * 0.32;
    }

    /** Un point de l'anneau : l'angle 0 en haut (l'injecteur), π en bas (le détecteur). */
    private double px(double angle) {
        return cx() + rx() * Math.sin(angle);
    }

    private double py(double angle) {
        return cy() - ry() * Math.cos(angle);
    }

    /** Les secondes de la dernière image, et la couleur des arcs du croisement (dorés sous le faisceau doré). */
    private double lastElapsed;
    private Color lightningColor = LIGHT;

    public void frame(double elapsed) {
        lastElapsed = Math.min(elapsed, 0.1);
        if (motion) time += elapsed;
        boolean crossing = game.isCrossing();
        // Chaque croisement fait jaillir des étincelles, même sans clic : les faisceaux produisent toujours.
        if (crossing && !wasCrossing) burst(false, 0);
        wasCrossing = crossing;
        showCollisions();
        for (Wave wave : waves) wave.age += elapsed;
        waves.removeIf(wave -> wave.age >= 1.2);
        // Un effet coupé l'est tout de suite : les ondes et les étincelles déjà lancées disparaissent (version 0.67).
        if (!motion) waves.clear();
        if (!sparksOn) sparks.removeIf(spark -> spark.quark == null && spark.text == null);
        for (Spark spark : sparks) {
            spark.age += elapsed;
            // Sans animation, un quark ou un gain reste au détecteur le temps de se lire, sans s'envoler.
            if (!motion) continue;
            spark.x += spark.vx * elapsed;
            spark.y += spark.vy * elapsed;
            spark.vy += (spark.quark != null ? 18 : spark.text == null ? 60 : -10) * elapsed;
        }
        sparks.removeIf(spark -> spark.age >= spark.life);
        draw();
    }

    /** Les collisions arrivées depuis la dernière image : une onde, et le quark qui s'envole. */
    private void showCollisions() {
        List<Game.CollisionResult> recent = game.recentCollisions();
        if (lastSerial < 0) {
            lastSerial = recent.isEmpty() ? 0 : recent.get(recent.size() - 1).serial();
            return;
        }
        int shown = 0;
        for (Game.CollisionResult result : recent) {
            if (result.serial() <= lastSerial) continue;
            lastSerial = result.serial();
            if (shown++ >= 6 || sparks.size() > 220) continue;      // beaucoup d'un coup (déclencheur rapide) : quelques-unes suffisent
            collisionsShown++;
            Wave wave = new Wave();
            wave.color = color(result.quark());
            wave.caught = result.caught();
            waves.add(wave);
            double seed = Math.sin(result.serial() * 12.9898) * 43758.5453;
            double unit = seed - Math.floor(seed);
            Spark quark = new Spark();
            quark.x = px(Math.PI);
            quark.y = py(Math.PI);
            double angle = -Math.PI * (0.2 + 0.6 * unit);
            quark.vx = Math.cos(angle) * 70;
            quark.vy = Math.sin(angle) * 70;
            quark.life = 1.8;
            quark.size = result.caught() ? 22 : 16;
            quark.color = wave.color;
            quark.quark = result.quark().symbol();
            quark.text = result.count() > 1 ? "×" + result.count() : null;
            sparks.add(quark);
        }
    }

    /** Des étincelles au détecteur ; dorées et plus nombreuses quand le croisement est capté, avec ce qu'il rapporte. */
    private void burst(boolean caught, double gain) {
        double x = px(Math.PI), y = py(Math.PI);
        int count = !sparksOn ? 0 : caught ? 34 : 10;
        Color[] palette = caught ? new Color[] {GOLD, Color.WHITE, BEAM_A, BEAM_B} : new Color[] {BEAM_A, BEAM_B};
        for (int k = 0; k < count; k++) {
            Spark spark = new Spark();
            double seed = Math.sin((k + 1) * 12.9898 + time * 78.233) * 43758.5453;
            double unit = seed - Math.floor(seed);
            double angle = -Math.PI * unit;
            double speed = (caught ? 90 : 40) * (0.5 + unit);
            spark.x = x;
            spark.y = y;
            spark.vx = Math.cos(angle) * speed;
            spark.vy = Math.sin(angle) * speed;
            spark.life = caught ? 1.1 : 0.6;
            spark.color = palette[k % palette.length];
            sparks.add(spark);
        }
        if (caught) {
            Spark text = new Spark();
            text.x = x;
            text.y = y - 20;
            text.vy = -30;
            text.life = 1.4;
            text.color = GOLD;
            text.text = "+" + ColliderPane.whole(gain) + " énergie";
            sparks.add(text);
        }
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        idle.core.universe.Skin look = game.chosenSkin(idle.core.universe.SkinVisual.COLLIDER);
        g.setFill(look == idle.core.universe.Skin.COLLIDER_NEON ? Color.web("#05020a")
                : look == idle.core.universe.Skin.COLLIDER_PLASMA ? Color.web("#0c0408")
                : look == idle.core.universe.Skin.COLLIDER_ICE ? Color.web("#06121c") : BACK);
        g.fillRect(0, 0, w, h);
        if (look == idle.core.universe.Skin.COLLIDER_ICE) {
            // La glace : des flocons qui descendent lentement.
            g.setStroke(Color.web("#cfefff"));
            g.setLineWidth(0.8);
            for (int i = 0; i < 26; i++) {
                double x = (i * 0.754877666 % 1) * w, y = ((i * 0.569840291 + time * 0.03 * (1 + i % 3)) % 1) * h;
                g.setGlobalAlpha(0.35);
                for (int k = 0; k < 3; k++) {
                    double a = Math.PI * k / 3 + time * 0.2;
                    g.strokeLine(x - 3 * Math.cos(a), y - 3 * Math.sin(a), x + 3 * Math.cos(a), y + 3 * Math.sin(a));
                }
            }
        } else {
            g.setFill(look == idle.core.universe.Skin.COLLIDER_PLASMA ? Color.web("#ffb07a") : Color.web("#9ab4d0"));
            for (int i = 0; i < 80; i++) {
                g.setGlobalAlpha(0.12 + 0.25 * (0.5 + 0.5 * Math.sin(time * 0.6 + i)));
                g.fillOval((i * 0.754877666 % 1) * w, (i * 0.569840291 % 1) * h, 1.4, 1.4);
            }
        }
        g.setGlobalAlpha(1);
        double phase = game.colliderBeamPhase();
        boolean crossing = game.isCrossing(), catchable = game.canCatchCrossing();
        // L'anneau.
        if (look == idle.core.universe.Skin.COLLIDER_NEON) neonRing(g);
        else if (look == idle.core.universe.Skin.COLLIDER_PLASMA) plasmaRing(g);
        else if (look == idle.core.universe.Skin.COLLIDER_ICE) iceRing(g);
        else {
            ellipse(g, 0, 2 * Math.PI, RING, 10, 1);
            ellipse(g, 0, 2 * Math.PI, LIGHT, 2, 0.25 + 0.1 * Math.sin(time * 2));
        }
        // La zone du croisement, autour du détecteur.
        double half = Math.PI * game.colliderWindow();
        ellipse(g, Math.PI - half, Math.PI + half, GOLD, catchable ? 8 : 5, catchable ? 1 : 0.4);
        // L'injecteur et le détecteur.
        g.setFill(Color.web("#2c4866"));
        g.fillRoundRect(px(0) - 9, py(0) - 6, 18, 12, 3, 3);
        g.setFill(catchable ? GOLD : Color.web("#3b5578"));
        g.fillRoundRect(px(Math.PI) - 13, py(Math.PI) - 7, 26, 14, 4, 4);
        // Les deux faisceaux : partis ensemble de l'injecteur, ils se croisent dans le détecteur.
        double angle = 2 * Math.PI * phase;
        boolean gold = game.chosenSkin(idle.core.universe.SkinVisual.COLLIDER_BEAM) == idle.core.universe.Skin.BEAM_GOLD;
        if (!gold) goldDust.clear();
        if (gold) {
            goldBeam(g, angle, 1);
            goldBeam(g, -angle, -1);
            drawGoldDust(g, lastElapsed);
        } else {
            beam(g, angle, 1, BEAM_A);
            beam(g, -angle, -1, BEAM_B);
        }
        // Quand les boules se croisent dans le détecteur, de l'électricité saute entre elles.
        arcs = 0;
        lightningColor = gold ? Color.web("#ffcf4a") : LIGHT;
        if (crossing && sparksOn) lightning(g, angle);
        if (catchable) glow(g, px(Math.PI), py(Math.PI), 46, GOLD, 0.5);
        // Les ondes des collisions.
        for (Wave wave : waves) {
            double grow = wave.age / 1.2;
            double radius = 10 + grow * (wave.caught ? 90 : 60);
            g.setStroke(wave.color);
            g.setLineWidth(wave.caught ? 4 : 2.5);
            g.setGlobalAlpha(Math.max(0, 1 - grow));
            g.strokeOval(px(Math.PI) - radius, py(Math.PI) - radius * 0.6, 2 * radius, 1.2 * radius);
        }
        g.setGlobalAlpha(1);
        // Les étincelles, et les gains qui montent.
        for (Spark spark : sparks) {
            double fade = 1 - spark.age / spark.life;
            g.setGlobalAlpha(Math.max(0, fade));
            if (spark.quark != null) {
                double s = spark.size;
                glow(g, spark.x, spark.y, s * 1.3, spark.color, 0.45);
                g.setFill(spark.color);
                g.fillOval(spark.x - s / 2, spark.y - s / 2, s, s);
                g.setFill(BACK);
                g.setFont(Font.font(s * 0.62));
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText(spark.quark, spark.x, spark.y + s * 0.22);
                if (spark.text != null) {
                    g.setFill(spark.color);
                    g.setFont(Font.font(12));
                    g.fillText(spark.text, spark.x + s * 0.9, spark.y - s * 0.4);
                }
                g.setTextAlign(TextAlignment.LEFT);
            } else if (spark.text != null) {
                g.setFont(Font.font(14));
                g.setTextAlign(TextAlignment.CENTER);
                g.setFill(spark.color);
                g.fillText(spark.text, spark.x, spark.y);
                g.setTextAlign(TextAlignment.LEFT);
            } else {
                g.setFill(spark.color);
                g.fillOval(spark.x - 2, spark.y - 2, 4, 4);
            }
        }
        g.setGlobalAlpha(1);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(13));
        g.setFill(LIGHT);
        g.fillText("+" + ColliderPane.rate(game.colliderEnergyPerSecond()) + " énergie par seconde", cx(), cy() + 5);
        g.setFont(Font.font(11));
        g.setFill(catchable ? GOLD : Color.web("#5a7088"));
        g.fillText(catchable ? "ils se croisent : cliquer !" : crossing ? "croisement capté" : "cliquer quand les faisceaux se croisent en bas",
                cx(), h - 8);
        // En haut à gauche : la chance d'une collision à chaque croisement.
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        g.setFill(LIGHT);
        g.fillText("Chance de collision : " + Math.round(game.collisionChance() * 100) + " %", 12, 18);
        g.setFill(Color.web("#5a7088"));
        g.fillText("un croisement toutes les " + seconds(game.colliderPeriod()) + " s", 12, 34);
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** « 2 », « 1,4 » : des secondes, avec une décimale au besoin. */
    static String seconds(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        return rounded == Math.floor(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded).replace('.', ',');
    }

    private void ellipse(GraphicsContext g, double from, double to, Color color, double width, double alpha) {
        for (int k = 0; k <= STEPS; k++) {
            double a = from + (to - from) * k / STEPS;
            XS[k] = px(a);
            YS[k] = py(a);
        }
        g.setStroke(color);
        g.setLineWidth(width);
        g.setGlobalAlpha(alpha);
        g.strokePolyline(XS, YS, STEPS + 1);
        g.setGlobalAlpha(1);
    }

    private void beam(GraphicsContext g, double angle, int direction, Color color) {
        g.setFill(color);
        for (int k = 14; k >= 1; k--) {
            double a = angle - direction * k * 0.05;
            g.setGlobalAlpha(0.5 * (1 - k / 15.0));
            double size = 6 * (1 - 0.6 * k / 14);
            g.fillOval(px(a) - size / 2, py(a) - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
        double heat = flameHeat();
        if (heat > 0) flame(g, angle, direction, heat);
        glow(g, px(angle), py(angle), 12, color, 0.8);
        g.setFill(Color.WHITE);
        g.fillOval(px(angle) - 3.5, py(angle) - 3.5, 7, 7);
    }

    /**
     * Le néon (version 0.89) : l'anneau en tubes de néon rose et cyan, coupés en segments comme une
     * enseigne, qui grésillent ; l'injecteur et le détecteur en contours lumineux.
     */
    private void neonRing(GraphicsContext g) {
        int segments = 14;
        for (int k = 0; k < segments; k++) {
            double from = 2 * Math.PI * k / segments + 0.04, to = 2 * Math.PI * (k + 1) / segments - 0.04;
            boolean off = Math.sin(time * 9 + k * 5.3) > 0.97;          // un segment qui grésille
            Color color = k % 2 == 0 ? Color.web("#ff4fd8") : Color.web("#3ff0ff");
            ellipse(g, from, to, color, 9, off ? 0.05 : 0.18);
            ellipse(g, from, to, color, 4, off ? 0.1 : 0.45);
            ellipse(g, from, to, Color.WHITE, 1.4, off ? 0.2 : 0.9);
        }
    }

    /** Le plasma : un tube de gaz brûlant, des bulles orange et violettes qui roulent et des filaments qui ondulent. */
    private void plasmaRing(GraphicsContext g) {
        ellipse(g, 0, 2 * Math.PI, Color.web("#2a0c14"), 14, 1);
        for (int k = 0; k < 40; k++) {
            double a = 2 * Math.PI * k / 40 + time * (k % 2 == 0 ? 0.3 : -0.2);
            double size = 8 + 5 * Math.sin(time * 3 + k * 1.3);
            glow(g, px(a), py(a), size, k % 3 == 0 ? Color.web("#b04cff") : Color.web("#ff7a2a"), 0.35);
        }
        for (int f = 0; f < 2; f++) {
            for (int k = 0; k <= STEPS; k++) {
                double a = 2 * Math.PI * k / STEPS;
                double wave = 4 * Math.sin(a * 9 + time * (4 + f * 2) + f * 2);
                XS[k] = cx() + (rx() + wave) * Math.sin(a);
                YS[k] = cy() - (ry() + wave) * Math.cos(a);
            }
            g.setStroke(f == 0 ? Color.web("#ffd27f") : Color.web("#ff9ff0"));
            g.setLineWidth(1.2);
            g.setGlobalAlpha(0.7);
            g.strokePolyline(XS, YS, STEPS + 1);
        }
        g.setGlobalAlpha(1);
    }

    /** La glace : un anneau épais bleu pâle, hérissé de cristaux qui pointent vers l'extérieur. */
    private void iceRing(GraphicsContext g) {
        ellipse(g, 0, 2 * Math.PI, Color.web("#7fb8d8"), 11, 0.55);
        ellipse(g, 0, 2 * Math.PI, Color.web("#e8f8ff"), 2.5, 0.85);
        g.setFill(Color.web("#d8f3ff"));
        int spikes = 36;
        for (int k = 0; k < spikes; k++) {
            double a = 2 * Math.PI * k / spikes;
            double x = px(a), y = py(a);
            double nx = x - cx(), ny = y - cy(), length = Math.max(1e-9, Math.hypot(nx, ny));
            nx /= length;
            ny /= length;
            double tall = 6 + 6 * Math.abs(Math.sin(k * 2.3));
            double side = 2.4;
            g.setGlobalAlpha(0.75);
            g.fillPolygon(new double[] {x - ny * side, x + ny * side, x + nx * tall},
                    new double[] {y + nx * side, y - nx * side, y + ny * tall}, 3);
        }
        g.setGlobalAlpha(1);
    }

    /** Les paillettes d'or qui tombent du faisceau doré : x, y, vitesse x, vitesse y, âge. */
    private final List<double[]> goldDust = new ArrayList<>();

    /**
     * Le faisceau doré (version 0.89) : chaque faisceau devient un ruban d'or massif qui remplit
     * l'anneau depuis l'injecteur jusqu'à sa tête, une grande étoile dorée à six branches qui tourne
     * dans un halo, et qui sème des paillettes qui tombent. Au croisement, les arcs sont dorés.
     */
    private void goldBeam(GraphicsContext g, double angle, int direction) {
        // Le ruban : de l'injecteur à la tête, de plus en plus épais et vif.
        int steps = Math.max(2, (int) Math.ceil(Math.abs(angle) / (2 * Math.PI) * STEPS));
        for (int pass = 0; pass < 3; pass++) {
            for (int k = 0; k < steps; k++) {
                double a0 = angle * k / steps, a1 = angle * (k + 1) / steps;
                double u = (k + 1) / (double) steps;
                g.setStroke(pass == 0 ? Color.web("#b8860b") : pass == 1 ? Color.web("#ffcf4a") : Color.web("#fff8dc"));
                g.setLineWidth(pass == 0 ? 3 + 9 * u : pass == 1 ? 1.5 + 4 * u : 0.8 + 1.4 * u);
                g.setGlobalAlpha((pass == 0 ? 0.25 : pass == 1 ? 0.75 : 0.9) * (0.25 + 0.75 * u));
                g.strokeLine(px(a0), py(a0), px(a1), py(a1));
            }
        }
        double x = px(angle), y = py(angle);
        // Les paillettes semées par la tête.
        if (motion && goldDust.size() < 160) {
            for (int k = 0; k < 2; k++) {
                double seed = random((long) (time * 1000) + k * 31 + direction * 7);
                goldDust.add(new double[] {x, y, (seed - 0.5) * 40, -20 - 30 * seed, 0});
            }
        }
        double heat = flameHeat();
        if (heat > 0) flame(g, angle, direction, heat);
        glow(g, x, y, 30, Color.web("#ffb23f"), 0.9);
        glow(g, x, y, 14, Color.WHITE, 0.9);
        g.setGlobalAlpha(1);
        g.setFill(Color.web("#ffd36b"));
        star(g, x, y, 15, 5, 6, time * 5 * direction);
        g.setFill(Color.web("#fffbe8"));
        star(g, x, y, 8, 3, 6, time * 5 * direction + Math.PI / 6);
    }

    /** Fait tomber et dessine les paillettes d'or. */
    private void drawGoldDust(GraphicsContext g, double elapsed) {
        g.setFill(Color.web("#ffe08a"));
        for (double[] dust : goldDust) {
            if (motion) {
                dust[0] += dust[2] * elapsed;
                dust[1] += dust[3] * elapsed;
                dust[3] += 70 * elapsed;
                dust[4] += elapsed;
            }
            double fade = Math.max(0, 1 - dust[4] / 1.4);
            g.setGlobalAlpha(fade);
            double size = 1.5 + 2 * fade;
            g.fillPolygon(new double[] {dust[0], dust[0] + size / 2, dust[0], dust[0] - size / 2},
                    new double[] {dust[1] - size, dust[1], dust[1] + size, dust[1]}, 4);
        }
        goldDust.removeIf(dust -> dust[4] >= 1.4);
        g.setGlobalAlpha(1);
    }

    private static void star(GraphicsContext g, double cx, double cy, double outer, double inner, int points, double turn) {
        double[] xs = new double[points * 2], ys = new double[points * 2];
        for (int k = 0; k < points * 2; k++) {
            double r = k % 2 == 0 ? outer : inner, a = turn + Math.PI * k / points;
            xs[k] = cx + r * Math.cos(a);
            ys[k] = cy + r * Math.sin(a);
        }
        g.fillPolygon(xs, ys, points * 2);
    }

    /** Nombre d'arcs électriques dessinés à la dernière image : pour les vérifications. */
    private int arcs;

    public int arcs() {
        return arcs;
    }

    /**
     * L'électricité du croisement : des arcs en zigzag entre les deux boules et autour d'elles,
     * blancs bordés de bleu, redessinés trente fois par seconde pour crépiter. Plus nombreux et
     * plus longs quand les boules sont en feu.
     */
    private void lightning(GraphicsContext g, double angle) {
        double heat = flameHeat();
        double ax = px(angle), ay = py(angle), bx = px(-angle), by = py(-angle);
        double mx = (ax + bx) / 2, my = (ay + by) / 2;
        long tick = (long) Math.floor(time * 30);
        int count = 3 + (int) Math.round(3 * heat);
        arcs = 0;
        for (int k = 0; k < count; k++) {
            double fromX, fromY, toX, toY;
            if (k == 0) {
                // L'arc entre les deux boules.
                fromX = ax; fromY = ay; toX = bx; toY = by;
            } else {
                // Les autres jaillissent du milieu, vers le haut et les côtés.
                double direction = -Math.PI * (0.05 + 0.9 * random(tick * 31 + k * 7));
                double reach = (16 + 22 * random(tick * 31 + k * 7 + 1)) * (1 + 0.6 * heat);
                fromX = mx; fromY = my;
                toX = mx + Math.cos(direction) * reach;
                toY = my + Math.sin(direction) * reach;
            }
            double length = Math.hypot(toX - fromX, toY - fromY);
            if (length < 2) {
                // Les boules l'une sur l'autre : un petit arc qui les enjambe.
                toX = fromX + 10;
                toY = fromY - 6;
                length = Math.hypot(10, 6);
            }
            double nx = -(toY - fromY) / length, ny = (toX - fromX) / length;
            int points = 8;
            double[] xs = new double[points + 1], ys = new double[points + 1];
            double jag = (3 + 4 * heat) * Math.min(1, length / 20);
            for (int p = 0; p <= points; p++) {
                double t = p / (double) points;
                double offset = p == 0 || p == points ? 0 : (random(tick * 131 + k * 17 + p) - 0.5) * 2 * jag;
                xs[p] = fromX + (toX - fromX) * t + nx * offset;
                ys[p] = fromY + (toY - fromY) * t + ny * offset;
            }
            g.setStroke(lightningColor);
            g.setLineWidth(4);
            g.setGlobalAlpha(0.3);
            g.strokePolyline(xs, ys, points + 1);
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.3);
            g.setGlobalAlpha(0.95);
            g.strokePolyline(xs, ys, points + 1);
            arcs++;
        }
        g.setGlobalAlpha(1);
        glow(g, mx, my, 18 + 10 * heat, LIGHT, 0.55);
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double random(long seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return value - Math.floor(value);
    }

    /**
     * La flamme d'une boule lancée vite : des langues de feu qui traînent derrière elle, du jaune
     * pâle contre la boule au rouge au bout, qui vacillent et montent un peu ; plus longues et plus
     * vives avec la vitesse ({@code heat}, de 0 à 1).
     */
    private void flame(GraphicsContext g, double angle, int direction, double heat) {
        double bx = px(angle), by = py(angle);
        glow(g, bx, by, 18 + 10 * heat, FLAME_MID, 0.45 + 0.35 * heat);
        int tongues = 11;
        for (int k = tongues; k >= 1; k--) {
            double t = k / (double) tongues;
            double a = angle - direction * k * (0.018 + 0.026 * heat);
            double x = px(a), y = py(a);
            // Un vacillement de part et d'autre de l'anneau, et la flamme qui monte.
            double nx = x - cx(), ny = y - cy();
            double length = Math.max(1e-9, Math.hypot(nx, ny));
            double flicker = 0.55 * Math.sin(time * 23 + k * 1.7) + 0.45 * Math.sin(time * 37 + k * 0.9);
            x += nx / length * flicker * (1.5 + 2.5 * heat) * t;
            y += ny / length * flicker * (1.5 + 2.5 * heat) * t - k * 1.1 * heat;
            Color color = t < 0.3 ? FLAME_CORE : t < 0.65 ? FLAME_MID : FLAME_EDGE;
            double size = (15 - 9 * t) * (0.6 + 0.6 * heat) * (0.85 + 0.15 * Math.sin(time * 31 + k * 2.3));
            glow(g, x, y, size, color, Math.min(1, (0.5 + 0.6 * heat) * (1 - 0.7 * t)));
        }
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), strength)),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }
}
