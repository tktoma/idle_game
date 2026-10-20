package idle.ui.bigbang;

import idle.core.Game;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Le dessin d'un système solaire (version 0.059) : l'étoile à gauche, une orbite par planète, de la
 * plus proche à la plus lointaine, les lunes autour de leur planète. Chaque orbite dit d'un coup
 * d'œil si sa planète est à sa place : verte quand elle l'est, orange sinon. La zone habitable d'un
 * Soleil ou d'une naine rouge est un voile vert sur les orbites 2 et 3.
 */
public final class SystemView extends Canvas {

    private static final Color BACK = Color.web("#0a1019");
    private static final Color GOOD = Color.web("#9be7a8");
    private static final Color BAD = Color.web("#ffb36b");
    private static final Color TEXT = Color.web("#cfd8e3");
    private static final Color MUTED = Color.web("#6f8296");
    private static final int STEPS = 40;
    private static final double[] XS = new double[STEPS + 1];
    private static final double[] YS = new double[STEPS + 1];

    private final Game game;
    private SolarSystem system;
    private double time;

    SystemView(Game game, SolarSystem system) {
        super(760, 200);
        this.game = game;
        this.system = system;
        draw();
    }

    void setSystem(SolarSystem system) {
        this.system = system;
        draw();
    }

    /** Fait tourner les planètes et leurs lunes. */
    public void frame(double elapsed) {
        if (BigBangPage.moving()) time += elapsed;
        draw();
    }

    private double starX() {
        return 62;
    }

    private double cy() {
        return getHeight() / 2 - 10;
    }

    /** L'écart entre deux orbites : la place se partage, sans dépasser 120 px. */
    private double spacing(int orbits) {
        return Math.min(120, (getWidth() - 150) / Math.max(orbits, 3));
    }

    private double orbitX(int orbit, int orbits) {
        return 128 + spacing(orbits) * (orbit + 0.5);
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        // Quelques étoiles lointaines.
        g.setFill(Color.web("#8aa0bc"));
        for (int i = 0; i < 50; i++) {
            g.setGlobalAlpha(0.15 + 0.2 * (0.5 + 0.5 * Math.sin(time * 0.7 + i)));
            g.fillOval((i * 0.754877666 % 1) * w, (i * 0.569840291 % 1) * h, 1.3, 1.3);
        }
        g.setGlobalAlpha(1);
        List<String> planets = system.planets();
        int orbits = Math.max(planets.size(), 2);
        double cy = cy();

        // La zone habitable, sur les orbites 2 et 3.
        if (Game.hasHabitableZone(system)) {
            double from = orbitX(1, orbits) - spacing(orbits) / 2, to = orbitX(2, orbits) + spacing(orbits) / 2;
            g.setFill(Color.color(GOOD.getRed(), GOOD.getGreen(), GOOD.getBlue(), 0.08));
            g.fillRect(from, 18, to - from, h - 54);
            g.setFill(Color.color(GOOD.getRed(), GOOD.getGreen(), GOOD.getBlue(), 0.75));
            g.setFont(Font.font(11));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("zone habitable : une planète océan ici, +2", (from + to) / 2, 14);
        }

        // L'étoile.
        Body star = game.body(system.star());
        Color glow = Color.web(star.tint());
        g.setFill(new RadialGradient(0, 0, starX(), cy, 70, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0.35)),
                new Stop(1, Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0))));
        g.fillOval(starX() - 70, cy - 70, 140, 140);
        BodyArt.draw(g, star, starX(), cy, 28);

        // Les orbites et leurs planètes.
        for (int orbit = 0; orbit < orbits; orbit++) {
            double x = orbitX(orbit, orbits);
            double radius = x - starX();
            boolean has = orbit < planets.size();
            boolean placed = has && game.isPlanetWellPlaced(system, orbit);
            Color color = !has ? MUTED : placed ? GOOD : BAD;
            arc(g, radius, cy, color, has ? 2 : 1, has ? 0.8 : 0.35);
            g.setTextAlign(TextAlignment.CENTER);
            if (!has) {
                g.setFill(MUTED);
                g.setFont(Font.font(11));
                g.fillText("orbite " + (orbit + 1) + " libre", x, h - 22);
                continue;
            }
            Body planet = game.body(planets.get(orbit));
            // La planète glisse sur son orbite, sans descendre jusqu'aux étiquettes.
            double angle = Math.asin(Math.min(1, 34 / radius)) * Math.sin(time * 0.35 + orbit * 1.7);
            double px = starX() + radius * Math.cos(angle), py = cy + radius * Math.sin(angle);
            BodyArt.draw(g, planet, px, py, 13);
            if (orbit < system.moons().size()) {
                Body moon = game.body(system.moons().get(orbit));
                double a = time * (0.9 + 0.2 * orbit) + orbit;
                BodyArt.draw(g, moon, px + 24 * Math.cos(a), py + 13 * Math.sin(a), 5);
            }
            g.setFill(TEXT);
            g.setFont(Font.font(11));
            g.fillText(planet.name(), x, h - 34);
            g.setFill(color);
            g.fillText((placed ? "✓ " : "✗ ") + kind(planet), x, h - 20);
            g.setFill(MUTED);
            g.fillText("orbite " + (orbit + 1), x, h - 6);
        }
        g.setTextAlign(TextAlignment.LEFT);
        if (planets.isEmpty()) {
            g.setFill(MUTED);
            g.setFont(Font.font(12));
            g.fillText("Ajoutez des planètes : les telluriques près de l'étoile, les glacées et gazeuses au loin.", 128, 30);
        }
    }

    /** « tellurique → près », « gazeuse → au loin » : la sorte de la planète, et sa place. */
    private String kind(Body planet) {
        if (game.isTelluric(planet.id())) return "tellurique → près";
        return (planet.look() == Body.Look.BANDED ? "gazeuse" : "glacée") + " → au loin";
    }

    /** Un arc d'orbite autour de l'étoile, sur la hauteur du dessin. */
    private void arc(GraphicsContext g, double radius, double cy, Color color, double width, double alpha) {
        double half = Math.min(0.6, Math.asin(Math.min(1, (getHeight() * 0.36) / Math.max(radius, 1))));
        for (int k = 0; k <= STEPS; k++) {
            double a = -half + 2 * half * k / STEPS;
            XS[k] = starX() + radius * Math.cos(a);
            YS[k] = cy + radius * Math.sin(a);
        }
        g.setStroke(color);
        g.setLineWidth(width);
        g.setGlobalAlpha(alpha);
        g.strokePolyline(XS, YS, STEPS + 1);
        g.setGlobalAlpha(1);
    }
}
