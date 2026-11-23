package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.util.Locale;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : la durée : le rond, le bouton, les spirales. */
public abstract class DimensionViewDuration extends DimensionViewVolume {
    public DimensionViewDuration(Game game) {
        super(game);
    }

    // ------------------------------------------------------------------
    // La durée : le rond, le bouton, les spirales
    // ------------------------------------------------------------------

    double durationRing() { return Math.min(getWidth(), getHeight()) * DURATION_RING; }

    double durationButton() { return Math.min(getWidth(), getHeight()) * DURATION_BUTTON; }

    /** Vrai si ce point de la vue est sur le bouton du milieu. */
    public boolean overDurationButton(double x, double y) {
        return Math.hypot(x - getWidth() / 2, y - getHeight() / 2) <= durationButton();
    }

    /** Un clic sur le bouton du milieu : les spirales partent du bord. Ailleurs dans le rond, rien. */
    boolean pressDuration(double x, double y) {
        if (!game.isDurationOpen() || !overDurationButton(x, y)) return false;
        return game.holdDuration();
    }

    /**
     * Le bouton relâché (la souris, la barre d'espace, la fenêtre qui se ferme) : les spirales donnent leurs éclats,
     * qui montent du bouton. Vrai s'il était tenu. Tant que l'appui automatique le tient, le joueur ne
     * le relâche pas : c'est l'automatisme qui relâche, au centre.
     */
    public boolean releaseHold() {
        if (dimension != Dimension.DURATION || !game.isDurationHeld() || game.isDimensionAutoPlayed(4)) return false;
        durationPickup(game.releaseDuration());
        lastReleases = game.durationReleases();
        return true;
    }

    /** Les éclats d'un relâchement montent du bouton ; rien pour un relâchement qui n'a rien donné. */
    void durationPickup(double reward) {
        if (reward <= 0 || durationPickups.size() >= 12) return;
        Pickup mark = new Pickup();
        mark.x = getWidth() / 2;
        mark.y = getHeight() / 2;
        mark.text = "+" + Format.darkMatter(BigNum.of(reward));
        durationPickups.add(mark);
    }

    /** Les gains de la durée qui montent encore : pour les vérifications. */
    public int durationPickupsShown() { return durationPickups.size(); }

    /**
     * Le rond et ses spirales : chacune part du bord et s'enroule jusqu'au bouton ; la part déjà faite brille, la
     * pointe avance tant qu'on tient. Au centre, le bouton, son anneau qui se remplit, et ce que donnerait un
     * relâchement maintenant.
     */
    void durationScene(GraphicsContext g, double cx, double cy, double u) {
        Color c = color();
        double ring = durationRing(), button = durationButton(), progress = game.durationProgress();
        boolean held = game.isDurationHeld();
        // Le rond ; sous le skin, le disque noir, ses sillons, son reflet et son étiquette.
        if (skinned) vinyl(g, cx, cy, ring, button, c);
        else {
            UniverseView.glow(g, cx, cy, ring * 1.08, c, 0.12);
            g.setStroke(c);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.7);
            g.strokeOval(cx - ring, cy - ring, ring * 2, ring * 2);
        }
        // Les spirales : au plus DURATION_DRAWN_TURNS tours dessinés, quoi que vaille la vitesse.
        int spirals = game.durationSpirals();
        double turns = Math.max(0.5, Math.min(DURATION_DRAWN_TURNS, game.durationTurnsMax()));
        double spin = time * 0.08;
        int steps = (int) Math.ceil(turns * 72);
        // Où en est la pointe de la première spirale : l'aiguille du vinyle s'y pose.
        double headX = cx, headY = cy - ring;
        for (int k = 0; k < spirals; k++) {
            double start = spin + Math.PI * 2 * k / spirals;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double f = s / (double) steps, angle = start + f * turns * Math.PI * 2, r = ring - (ring - button * 1.3) * f;
                xs[s] = cx + Math.cos(angle) * r;
                ys[s] = cy + Math.sin(angle) * r;
            }
            g.setStroke(skinned ? GROOVE_READ : c);
            g.setLineWidth(1);
            g.setGlobalAlpha(skinned ? 0.35 : 0.22);
            g.strokePolyline(xs, ys, steps + 1);
            int done = (int) Math.round(progress * steps);
            if (done > 0) {
                g.setStroke(skinned ? Color.WHITE : c);
                g.setLineWidth(2.4);
                g.setGlobalAlpha(0.95);
                g.strokePolyline(xs, ys, done + 1);
                UniverseView.glow(g, xs[done], ys[done], 14, c, 0.9);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(1);
                g.fillOval(xs[done] - 3, ys[done] - 3, 6, 6);
            } else {
                g.setFill(c);
                g.setGlobalAlpha(0.8);
                g.fillOval(xs[0] - 3, ys[0] - 3, 6, 6);
            }
            if (k == 0) {
                headX = xs[done];
                headY = ys[done];
            }
        }
        if (skinned && spirals > 0) tonearm(g, cx, cy, ring, headX, headY);
        // Le bouton : un anneau qui se remplit avec le temps tenu, et qui bat une fois au centre.
        boolean full = held && progress >= 1;
        double beat = full ? 0.5 + 0.5 * Math.sin(time * 8) : 0;
        UniverseView.glow(g, cx, cy, button * (1.8 + 0.6 * beat), c, held ? 0.6 + 0.3 * beat : 0.3);
        g.setGlobalAlpha(1);
        g.setFill(held ? c.deriveColor(0, 1, 0.55, 1) : skinned ? VINYL : Color.web("#141a2c"));
        g.fillOval(cx - button, cy - button, button * 2, button * 2);
        g.setStroke(c);
        g.setLineWidth(2);
        g.strokeOval(cx - button, cy - button, button * 2, button * 2);
        if (held) {
            g.setLineWidth(4);
            g.strokeArc(cx - button - 6, cy - button - 6, button * 2 + 12, button * 2 + 12, 90, -360 * progress,
                    ArcType.OPEN);
        }
        g.setFill(Color.WHITE);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(14));
        g.fillText(!held ? "Tenir" : full ? "Relâchez" : String.format(Locale.ROOT, "%.1f s", game.durationHeldSeconds()),
                cx, cy + 5);
        if (held) {
            g.setFont(Font.font(16));
            g.fillText("+" + Format.darkMatter(BigNum.of(game.durationPending())), cx, cy - button - 18);
        }
        // Les éclats donnés au relâchement montent du bouton.
        for (Pickup pickup : durationPickups) {
            double t = pickup.age / 1.4;
            UniverseView.glow(g, pickup.x, pickup.y, button + 60 * t, c, 0.7 * (1 - t));
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFont(Font.font(20));
            g.fillText(pickup.text, pickup.x, pickup.y - button - 24 - 50 * t);
        }
        g.setGlobalAlpha(1);
    }

    /** Le disque du vinyle : noir, ses sillons concentriques, deux reflets qui tournent lentement, et l'étiquette colorée au milieu. */
    void vinyl(GraphicsContext g, double cx, double cy, double ring, double button, Color label) {
        g.setGlobalAlpha(1);
        g.setFill(VINYL);
        g.fillOval(cx - ring, cy - ring, ring * 2, ring * 2);
        g.setStroke(GROOVE);
        g.setLineWidth(1);
        int k = 0;
        for (double r = ring * 0.97; r > button * 2.6; r -= 5) {
            g.setGlobalAlpha(0.35 + 0.4 * UniverseView.noise(k++));
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.07);
        double shine = 100 + 8 * Math.sin(time * 0.5);
        g.fillArc(cx - ring, cy - ring, ring * 2, ring * 2, shine, 35, ArcType.ROUND);
        g.fillArc(cx - ring, cy - ring, ring * 2, ring * 2, shine + 180, 35, ArcType.ROUND);
        g.setGlobalAlpha(1);
        g.setFill(label);
        g.fillOval(cx - button * 2.4, cy - button * 2.4, button * 4.8, button * 4.8);
        g.setStroke(VINYL);
        g.setLineWidth(1.5);
        g.strokeOval(cx - button * 2.4, cy - button * 2.4, button * 4.8, button * 4.8);
    }

    /** Le bras de la platine : du pivot, hors du disque en haut à droite, à la cellule posée sur la pointe du sillon lu. */
    void tonearm(GraphicsContext g, double cx, double cy, double ring, double headX, double headY) {
        double px = Math.min(getWidth() - 14, cx + ring * 1.12), py = Math.max(14, cy - ring * 1.02);
        g.setGlobalAlpha(1);
        g.setStroke(TONEARM);
        g.setLineWidth(5);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(px, py, headX, headY);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setFill(GROOVE_READ);
        g.fillOval(px - 9, py - 9, 18, 18);
        g.setFill(CARTRIDGE);
        g.fillRoundRect(headX - 7, headY - 7, 14, 14, 3, 3);
    }
}
