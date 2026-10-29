package idle.ui.hadrons;

import idle.core.Game;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * La liaison (version 0.151) : le sous-onglet qui montre les gluons à l'œuvre. Les quarks en main y flottent, de la
 * couleur de leur saveur (celle du collisionneur) ; quand un hadron se lie, d'où que vienne le clic, les quarks de sa
 * recette se rapprochent, des ressorts de gluons ondulés les relient, le hadron formé brille, puis rejoint la rangée des
 * hadrons formés, en bas, où chaque sorte ouverte a sa place et son compte.
 *
 * <p>Sobre : au plus {@link #MAX_FLOATING} quarks dessinés (au-delà, chacun en représente plusieurs, au prorata des
 * saveurs) et {@link #MAX_BINDINGS} liaisons à la fois (une de plus fait finir la plus ancienne). Elle n'avance qu'à
 * {@link #frame}, que la page n'appelle que lorsque le sous-onglet est à l'écran : une liaison faite ailleurs attend
 * donc qu'on vienne la voir. Sans animation, les liaisons finissent d'un coup et les quarks restent figés.
 */
public final class BindingView extends Canvas {

    /** Les quarks qui flottent, au plus. */
    static final int MAX_FLOATING = 48;
    /** Les liaisons animées à la fois, au plus. */
    static final int MAX_BINDINGS = 6;
    /** La durée d'une liaison, du rapprochement à l'arrivée dans la rangée, en secondes. */
    static final double DURATION = 2.4;
    /** La hauteur de la rangée des hadrons formés, en bas. */
    private static final double ROW_HEIGHT = 58;
    /** Les points d'un ressort de gluon : assez pour onduler, pas davantage. */
    private static final int SPRING_POINTS = 15;

    private static final Color BACK = Color.web("#0b0708");
    private static final Color GLOW = Color.web(HadronsPage.COLOR);
    private static final Color TEXT = Color.web("#e8f4ff");
    private static final Color DIM = Color.web("#8fa3b8");
    private static final Font SMALL = Font.font(10);
    private static final Font LETTER = Font.font("System", FontWeight.BOLD, 10);
    private static final Font NAME = Font.font(11);
    private static final Map<Quark, Color> COLORS = new EnumMap<>(Quark.class);

    static {
        for (Quark quark : Quark.values()) COLORS.put(quark, Color.web(ColliderPane.quarkColor(quark)));
    }

    /** Un quark qui flotte : position et vitesse en fractions de la zone, âge pour son apparition en douceur. */
    private static final class Floating {
        Quark quark;
        double x, y, vx, vy, phase, age;
    }

    /** Une liaison en cours : ses quarks partent de leur place et se rejoignent au centre de la liaison. */
    private static final class Binding {
        Hadron hadron;
        long count;
        double age, cx, cy, spin;
        Quark[] quarks;
        boolean[] anti;
        double[] startX, startY;
    }

    private final Game game;
    private final Random random = new Random(1964);
    private final List<Floating> floating = new ArrayList<>();
    private final List<Binding> bindings = new ArrayList<>();
    /** L'éclat de la place de chaque sorte dans la rangée, quand un hadron vient d'y arriver : de 1 à 0. */
    private final Map<Hadron, Double> flash = new EnumMap<>(Hadron.class);
    /** Les places de la rangée : les sortes ouvertes, dans l'ordre. */
    private final List<Hadron> slots = new ArrayList<>();
    private final double[] springX = new double[SPRING_POINTS], springY = new double[SPRING_POINTS];
    private double time;

    BindingView(Game game) {
        super(780, 340);
        this.game = game;
        draw();
    }

    /**
     * Un hadron vient de se lier, {@code count} fois d'un coup : sa liaison commence. Ses quarks sont pris parmi ceux
     * qui flottent quand il y en a de la bonne saveur, sinon ils arrivent d'un bord.
     */
    public void bind(Hadron hadron, long count) {
        if (count <= 0) return;
        if (bindings.size() >= MAX_BINDINGS) finish(bindings.remove(0));
        String[] tokens = hadron.formula().split(" ");
        Binding binding = new Binding();
        binding.hadron = hadron;
        binding.count = count;
        binding.quarks = new Quark[tokens.length];
        binding.anti = new boolean[tokens.length];
        binding.startX = new double[tokens.length];
        binding.startY = new double[tokens.length];
        binding.cx = 0.25 + 0.5 * random.nextDouble();
        binding.cy = 0.25 + 0.35 * random.nextDouble();
        binding.spin = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < tokens.length; i++) {
            Quark quark = flavor(tokens[i].charAt(0));
            binding.quarks[i] = quark;
            // La barre (un caractère de plus) : un antiquark, dessiné en anneau.
            binding.anti[i] = tokens[i].length() > 1;
            Floating taken = take(quark);
            if (taken != null) {
                binding.startX[i] = taken.x;
                binding.startY[i] = taken.y;
            } else {
                boolean side = random.nextBoolean();
                binding.startX[i] = side ? (random.nextBoolean() ? 0 : 1) : random.nextDouble();
                binding.startY[i] = side ? random.nextDouble() : 0;
            }
        }
        bindings.add(binding);
    }

    private static Quark flavor(char symbol) {
        for (Quark quark : Quark.values()) if (quark.symbol().charAt(0) == symbol) return quark;
        throw new IllegalArgumentException("saveur inconnue : " + symbol);
    }

    /** Retire de ceux qui flottent un quark de cette saveur, le plus ancien ; {@code null} s'il n'y en a pas. */
    private Floating take(Quark quark) {
        for (int i = 0; i < floating.size(); i++) if (floating.get(i).quark == quark) return floating.remove(i);
        return null;
    }

    /** Une liaison arrivée au bout : sa place dans la rangée s'éclaire. */
    private void finish(Binding binding) {
        flash.put(binding.hadron, 1.0);
    }

    /** Les liaisons animées en ce moment : pour les vérifications. */
    public int bindingsPlaying() {
        return bindings.size();
    }

    /** Les quarks qui flottent en ce moment : pour les vérifications. */
    public int floatingQuarks() {
        return floating.size();
    }

    /** Une image : les quarks suivent ceux en main, les liaisons avancent si l'animation est allumée, puis le dessin. */
    public void frame(double elapsed, boolean motion) {
        follow();
        double dt = motion ? Math.min(elapsed, 0.1) : 0;
        time += dt;
        for (Floating quark : floating) {
            // Figés, ils se montrent d'un coup ; sinon ils apparaissent en une demi-seconde.
            quark.age += motion ? dt : 1;
            quark.x += quark.vx * dt;
            quark.y += quark.vy * dt;
            if (quark.x < 0.02 || quark.x > 0.98) quark.vx = -quark.vx;
            if (quark.y < 0.04 || quark.y > 0.96) quark.vy = -quark.vy;
            quark.x = Math.max(0.02, Math.min(0.98, quark.x));
            quark.y = Math.max(0.04, Math.min(0.96, quark.y));
        }
        for (int i = bindings.size() - 1; i >= 0; i--) {
            Binding binding = bindings.get(i);
            binding.age += dt;
            if (!motion || binding.age >= DURATION) finish(bindings.remove(i));
        }
        for (Map.Entry<Hadron, Double> entry : flash.entrySet()) entry.setValue(Math.max(0, entry.getValue() - (motion ? dt * 1.5 : 1)));
        draw();
    }

    /** Ajuste ceux qui flottent aux quarks en main : autant que dans la main, ou leur part de {@link #MAX_FLOATING}. */
    private void follow() {
        long total = 0;
        for (Quark quark : HadronsPage.KEPT) total += Math.max(0, game.state().quarks(quark));
        for (Quark quark : HadronsPage.KEPT) {
            long held = Math.max(0, game.state().quarks(quark));
            long wanted = total <= MAX_FLOATING ? held : held > 0 ? Math.max(1, held * MAX_FLOATING / total) : 0;
            int shown = 0;
            for (Floating each : floating) if (each.quark == quark) shown++;
            for (; shown > wanted; shown--) take(quark);
            for (; shown < wanted && floating.size() < MAX_FLOATING; shown++) floating.add(born(quark));
        }
        slots.clear();
        for (Hadron hadron : Hadron.values()) if (game.isHadronAvailable(hadron)) slots.add(hadron);
    }

    private Floating born(Quark quark) {
        Floating each = new Floating();
        each.quark = quark;
        each.x = 0.04 + 0.92 * random.nextDouble();
        each.y = 0.06 + 0.88 * random.nextDouble();
        double angle = random.nextDouble() * Math.PI * 2, speed = 0.012 + 0.02 * random.nextDouble();
        each.vx = Math.cos(angle) * speed;
        each.vy = Math.sin(angle) * speed;
        each.phase = random.nextDouble() * Math.PI * 2;
        return each;
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        double fieldH = Math.max(1, h - ROW_HEIGHT);
        double r = Math.max(w, fieldH) * 0.6;
        g.setFill(new RadialGradient(0, 0, w / 2, fieldH / 2, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, tint(GLOW, 0.07)), new Stop(1, Color.TRANSPARENT)));
        g.fillRect(0, 0, w, fieldH);
        for (Floating quark : floating) {
            double x = quark.x * w + 3 * Math.sin(time * 0.9 + quark.phase), y = quark.y * fieldH + 3 * Math.cos(time * 0.7 + quark.phase);
            quarkDot(g, x, y, 4, COLORS.get(quark.quark), false, Math.min(1, quark.age * 2) * 0.85);
        }
        row(g, w, h);
        for (Binding binding : bindings) binding(g, binding, w, fieldH, h);
    }

    /** La rangée des hadrons formés : une place par sorte ouverte, éteinte tant qu'on n'en a aucun. */
    private void row(GraphicsContext g, double w, double h) {
        double top = h - ROW_HEIGHT;
        g.setStroke(tint(GLOW, 0.25));
        g.setLineWidth(1);
        g.strokeLine(12, top, w - 12, top);
        g.setTextAlign(TextAlignment.CENTER);
        int n = slots.size();
        for (int i = 0; i < n; i++) {
            Hadron hadron = slots.get(i);
            double x = slotX(i, n, w), y = top + 18;
            // Ceux qui sont encore en route ne comptent qu'à leur arrivée.
            long owned = game.hadrons(hadron);
            for (Binding binding : bindings) if (binding.hadron == hadron) owned -= binding.count;
            owned = Math.max(0, owned);
            double lit = flash.getOrDefault(hadron, 0.0);
            if (lit > 0) halo(g, x, y, 10 + 14 * lit, GLOW, 0.5 * lit);
            g.setFill(tint(GLOW, owned > 0 ? 0.9 : 0.2));
            g.fillOval(x - 7, y - 7, 14, 14);
            g.setFill(owned > 0 ? TEXT : DIM);
            g.setFont(NAME);
            g.fillText(hadron.label(), x, y + 22);
            g.setFont(SMALL);
            g.fillText(Format.whole(owned), x, y + 34);
        }
    }

    private double slotX(int index, int count, double w) {
        return w * (index + 0.5) / Math.max(1, count);
    }

    /** Une liaison : rapprochement, ressorts, éclat, puis le hadron qui file vers sa place. */
    private void binding(GraphicsContext g, Binding binding, double w, double fieldH, double h) {
        double t = binding.age / DURATION;
        double cx = binding.cx * w, cy = binding.cy * fieldH;
        int k = binding.quarks.length;
        double approach = ease(clamp(t / 0.3));
        double tighten = ease(clamp((t - 0.3) / 0.3));
        double radius = 32 - 16 * tighten;
        double spin = binding.spin + time * (1.2 + 2.5 * tighten);
        double fade = 1 - clamp((t - 0.68) / 0.12);
        double[] qx = new double[k], qy = new double[k];
        for (int i = 0; i < k; i++) {
            double angle = spin + Math.PI * 2 * i / k;
            double tx = cx + radius * Math.cos(angle), ty = cy + radius * Math.sin(angle);
            qx[i] = binding.startX[i] * w + (tx - binding.startX[i] * w) * approach;
            qy[i] = binding.startY[i] * fieldH + (ty - binding.startY[i] * fieldH) * approach;
        }
        // Les ressorts de gluons : ils apparaissent quand les quarks arrivent, et ondulent.
        double springs = clamp((t - 0.18) / 0.15) * fade;
        if (springs > 0) {
            g.setStroke(tint(GLOW, 0.75 * springs));
            g.setLineWidth(1.4);
            for (int i = 0; i < (k == 2 ? 1 : k); i++) spring(g, qx[i], qy[i], qx[(i + 1) % k], qy[(i + 1) % k], 3 + 1.5 * Math.sin(time * 9 + i));
        }
        // L'éclat : le hadron qui naît brille, d'autant plus fort que la liaison en fait beaucoup.
        double shine = Math.sin(Math.PI * clamp((t - 0.5) / 0.3));
        if (shine > 0) halo(g, cx, cy, 22 + 26 * shine, Color.WHITE, 0.35 * shine);
        if (fade > 0) {
            g.setFont(LETTER);
            g.setTextAlign(TextAlignment.CENTER);
            for (int i = 0; i < k; i++) {
                Color color = COLORS.get(binding.quarks[i]);
                quarkDot(g, qx[i], qy[i], 6.5, color, binding.anti[i], fade);
                g.setFill(tint(TEXT, fade));
                g.fillText(binding.quarks[i].symbol(), qx[i], qy[i] - 11);
            }
        }
        // Le hadron formé paraît pendant que ses quarks s'effacent, dit son nom, puis file vers sa place dans la rangée.
        double travel = ease(clamp((t - 0.8) / 0.2));
        if (t > 0.62) {
            int index = slots.indexOf(binding.hadron);
            double tx = index < 0 ? cx : slotX(index, slots.size(), w), ty = h - ROW_HEIGHT + 18;
            double x = cx + (tx - cx) * travel, y = cy + (ty - cy) * travel;
            double appear = clamp((t - 0.62) / 0.1);
            halo(g, x, y, 14, GLOW, 0.4 * appear);
            g.setFill(tint(GLOW, appear));
            g.fillOval(x - 8, y - 8, 16, 16);
            g.setFill(tint(TEXT, appear * (1 - travel)));
            g.setFont(SMALL);
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(binding.count > 1 ? binding.hadron.label() + " ×" + Format.whole(binding.count) : binding.hadron.label(), x, y - 16);
        }
    }

    /** Un ressort de gluon : un trait ondulé d'un quark à l'autre. */
    private void spring(GraphicsContext g, double x1, double y1, double x2, double y2, double amplitude) {
        double dx = x2 - x1, dy = y2 - y1, length = Math.hypot(dx, dy);
        if (length < 1) return;
        double nx = -dy / length, ny = dx / length;
        for (int i = 0; i < SPRING_POINTS; i++) {
            double s = i / (double) (SPRING_POINTS - 1);
            double offset = amplitude * Math.sin(s * Math.PI * 6 + time * 14) * Math.sin(Math.PI * s);
            springX[i] = x1 + dx * s + nx * offset;
            springY[i] = y1 + dy * s + ny * offset;
        }
        g.strokePolyline(springX, springY, SPRING_POINTS);
    }

    /** Un quark : un rond plein, ou un anneau pour un antiquark, dans un halo de sa couleur. */
    private static void quarkDot(GraphicsContext g, double x, double y, double r, Color color, boolean anti, double alpha) {
        if (alpha <= 0) return;
        g.setFill(tint(color, 0.22 * alpha));
        g.fillOval(x - r * 2, y - r * 2, r * 4, r * 4);
        if (anti) {
            g.setStroke(tint(color, alpha));
            g.setLineWidth(1.8);
            g.strokeOval(x - r, y - r, r * 2, r * 2);
        } else {
            g.setFill(tint(color, alpha));
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
    }

    private static void halo(GraphicsContext g, double x, double y, double r, Color color, double alpha) {
        if (alpha <= 0 || r <= 0) return;
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, tint(color, alpha)), new Stop(1, Color.TRANSPARENT)));
        g.fillOval(x - r, y - r, r * 2, r * 2);
    }

    private static Color tint(Color color, double alpha) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** Un départ et une arrivée en douceur. */
    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }
}
