package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import java.util.function.IntConsumer;
import javafx.geometry.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.util.List;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : la boucle : les cercles, leurs points, leurs zones. */
public abstract class DimensionViewLoop extends DimensionViewDuration {
    public DimensionViewLoop(Game game) {
        super(game);
    }

    /** Branche ce qui suit un geste du joueur dans le mini-jeu : un tir, un clic, un sondage, la membrane relâchée… */
    public void setOnGesture(IntConsumer gesture) {
        onGesture = gesture == null ? dimension -> { } : gesture;
    }

    /** Un geste du joueur dans la dimension montrée. */
    void gesture() { onGesture.accept(dimension.number()); }

    /** Fait suivre d'un geste une action qui a servi, et rend si elle a servi. */
    boolean gestured(boolean done) {
        if (done) gesture();
        return done;
    }

    /** Le métronome de la boucle : ce qu'il joue à chaque battement, ou {@code null} pour le couper. */
    public void setOnBeat(Runnable beat) { onBeat = beat; }

    /** Les battements du métronome depuis l'ouverture du jeu : pour les vérifications. */
    public long beats() { return beats; }

    /** Les mots de clics qui montent encore : pour les vérifications. */
    public int loopMarksShown() { return loopMarks.size(); }

    /** Les clics du rythme automatique que la vue a montrés sur leur cercle : pour les vérifications. */
    public long loopAutoFlashes() { return loopAutoFlashes; }

    /** Le texte du dernier mot de clic, ou rien : pour les vérifications. */
    public String lastLoopMark() { return loopMarks.isEmpty() ? "" : loopMarks.get(loopMarks.size() - 1).text; }

    /** L'écart entre deux cercles voisins : sept tiennent entre le premier et le dernier rayon. */
    double loopGap() {
        return Math.min(getWidth(), getHeight()) * (LOOP_OUTER - LOOP_INNER) / (Game.MAX_RINGS - 1);
    }

    /** Le rayon du cercle {@code ring} : le premier dehors, les suivants vers le centre. */
    public double loopRadius(int ring) { return Math.min(getWidth(), getHeight()) * LOOP_OUTER - ring * loopGap(); }

    /** Le cercle sous ce point de la vue (à une demi-largeur d'écart près), ou −1. */
    public int loopRingAt(double x, double y) {
        double d = Math.hypot(x - getWidth() / 2, y - getHeight() / 2), reach = Math.max(10, loopGap() / 2);
        int best = -1;
        for (int ring = 0; ring < game.loopRings(); ring++) {
            double off = Math.abs(d - loopRadius(ring));
            if (off <= reach && (best < 0 || off < Math.abs(d - loopRadius(best)))) best = ring;
        }
        return best;
    }

    /** Où est le point du cercle {@code ring} à l'écran. */
    public double[] loopPoint(int ring) {
        double a = -Math.PI / 2 + game.loopPhase(ring) * Math.PI * 2, r = loopRadius(ring);
        return new double[] {getWidth() / 2 + Math.cos(a) * r, getHeight() / 2 + Math.sin(a) * r};
    }

    /** Un clic dans la vue de la boucle : il joue le cercle sous la souris. Vrai s'il en a touché un. */
    boolean pressLoop(double x, double y) {
        if (!game.isLoopOpen()) return false;
        int ring = loopRingAt(x, y);
        if (ring < 0) return false;
        clickRing(ring);
        return true;
    }

    /**
     * Joue le cercle {@code ring}, à la souris ou par sa touche : ce que le clic vaut monte du point (« Excellent »,
     * « Bien », « Mauvais » ou « Raté », et le gain), et le cercle s'éclaire de sa couleur. Un clic pendant l'attente du
     * cercle ne compte pas : rien ne monte.
     */
    public Game.LoopHit clickRing(int ring) {
        Game.LoopHit hit = game.clickLoop(ring);
        lastLoopHits = game.loopHits();
        if (hit == Game.LoopHit.WAIT || ring < 0 || ring >= Game.MAX_RINGS) return hit;
        gesture();
        // Le cercle attend : ce n'est pas le rythme automatique qui l'a joué.
        lastLoopWait[ring] = game.loopWait(ring);
        ringMark(ring, hit);
        return hit;
    }

    /** Le mot d'un clic monte du point du cercle, avec son gain, et le cercle s'éclaire de sa couleur. */
    void ringMark(int ring, Game.LoopHit hit) {
        double[] at = loopPoint(ring);
        LoopMark mark = new LoopMark();
        mark.x = at[0];
        mark.y = at[1];
        mark.hit = hit;
        mark.text = hitWord(hit);
        mark.gain = hit == Game.LoopHit.MISS ? "" : "+" + Format.darkMatter(BigNum.of(game.loopReward(hit)));
        if (loopMarks.size() >= 20) loopMarks.remove(0);
        loopMarks.add(mark);
        ringFlash[ring] = 1;
        ringFlashHit[ring] = hit;
    }

    /**
     * La boucle avance d'une image : les mots montent, les éclats des cercles s'éteignent, et le métronome bat si le
     * point du premier cercle a passé un huitième de tour depuis l'image d'avant. Les clics du rythme automatique
     * (version 0.154 : joués un par un) se montrent comme ceux du joueur : le cercle dont l'attente vient de remonter
     * s'éclaire, et le mot monte de son point. Ceux qu'il compte sans les jouer (un grand pas) montent du centre.
     */
    void runLoop(double elapsed) {
        for (LoopMark mark : loopMarks) mark.age += elapsed;
        loopMarks.removeIf(mark -> mark.age > LOOP_MARK_SECONDS);
        for (int ring = 0; ring < ringFlash.length; ring++) ringFlash[ring] = Math.max(0, ringFlash[ring] - elapsed / 0.4);
        if (!game.isLoopOpen()) return;
        long hits = game.loopHits(), auto = game.loopAutoClicks(), played = 0;
        if (lastLoopAuto >= 0 && auto > lastLoopAuto) {
            Game.LoopHit hit = game.lastLoopHit() == Game.LoopHit.EXCELLENT ? Game.LoopHit.EXCELLENT : Game.LoopHit.GOOD;
            for (int ring = 0; ring < game.loopRings() && played < auto - lastLoopAuto; ring++) {
                if (game.loopWait(ring) <= lastLoopWait[ring] + 1e-9) continue;
                ringMark(ring, hit);
                played++;
                loopAutoFlashes++;
            }
        }
        lastLoopAuto = auto;
        for (int ring = 0; ring < Game.MAX_RINGS; ring++) lastLoopWait[ring] = game.loopWait(ring);
        long counted = lastLoopHits < 0 ? 0 : hits - lastLoopHits - played;
        if (counted > 0 && loopMarks.size() < 20) {
            LoopMark mark = new LoopMark();
            mark.x = getWidth() / 2;
            mark.y = getHeight() / 2;
            mark.hit = Game.LoopHit.GOOD;
            mark.text = "";
            mark.gain = "+" + Format.darkMatter(BigNum.of(game.loopReward(Game.LoopHit.GOOD) * counted));
            loopMarks.add(mark);
        }
        lastLoopHits = hits;
        double phase = game.loopPhase(0);
        if (Double.isNaN(lastBeatPhase)) {
            List<Double> zones = game.loopZones(0);
            beatOffset = zones.isEmpty() ? 0 : zones.get(0);
        } else if (onBeat != null && LoopBeat.crossed(lastBeatPhase, phase, beatOffset)) {
            beats++;
            onBeat.run();
        }
        lastBeatPhase = phase;
    }

    /**
     * Les cercles, leurs zones et leurs points. Chaque zone se dessine en trois arcs l'un sur l'autre, du bord au centre ;
     * le point brille davantage quand il est dans une zone, et pendant l'attente du cercle il pâlit, entouré d'un anneau
     * qui se remplit. Fermée, la boucle montre un seul cercle, sans zone, dont le point tourne au rythme du départ.
     */
    void loopScene(GraphicsContext g, double cx, double cy) {
        Color c = color();
        boolean open = game.isLoopOpen();
        int rings = open ? game.loopRings() : 1;
        double band = Math.max(6, Math.min(16, loopGap() * 0.42)), half = game.loopZoneHalfWidth();
        if (skinned) {
            // La cible : sa paille, puis ses anneaux pleins du dehors au centre, blanc, noir, bleu, rouge, or.
            double straw = loopRadius(0) + loopGap() * 0.7;
            g.setGlobalAlpha(1);
            g.setFill(STRAW);
            g.fillOval(cx - straw, cy - straw, straw * 2, straw * 2);
            g.setStroke(STRAW_EDGE);
            g.setLineWidth(3);
            g.strokeOval(cx - straw, cy - straw, straw * 2, straw * 2);
            for (int ring = 0; ring < rings; ring++) {
                double r = loopRadius(ring) + loopGap() * 0.5;
                g.setFill(TARGET[targetColor(ring, rings)]);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
            }
        } else {
            UniverseView.glow(g, cx, cy, loopRadius(rings - 1) * 0.6, c, 0.15);
        }
        for (int ring = 0; ring < rings; ring++) {
            double r = loopRadius(ring), flash = ringFlash[ring];
            int target = targetColor(ring, rings);
            // Le cercle, qui s'éclaire un instant de la couleur du dernier clic.
            g.setStroke(flash > 0 && ringFlashHit[ring] != null ? hitColor(ringFlashHit[ring]) : skinned ? LEAD : c);
            g.setLineWidth((skinned ? 1 : 2) + 2 * flash);
            g.setGlobalAlpha(Math.min(1, (skinned ? 0.5 : 0.35) + 0.5 * flash));
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
            if (open) {
                for (double center : game.loopZones(ring)) {
                    zoneArc(g, cx, cy, r, center, half, band, skinned ? TARGET_ZONES[target][0] : LOOP_BAD, skinned ? 1 : 0.75);
                    zoneArc(g, cx, cy, r, center, half * Game.LOOP_GOOD, band, skinned ? TARGET_ZONES[target][1] : LOOP_GOOD, skinned ? 1 : 0.9);
                    zoneArc(g, cx, cy, r, center, half * Game.LOOP_EXCELLENT, band, skinned ? TARGET_ZONES[target][2] : LOOP_EXCELLENT, 1);
                }
            }
            double phase = open ? game.loopPhase(ring) : (time * Game.LOOP_TURNS) % 1;
            double a = -Math.PI / 2 + phase * Math.PI * 2, x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
            double wait = open ? game.loopWait(ring) : 0;
            boolean inside = open && wait <= 0 && game.loopAim(ring) != Game.LoopHit.MISS;
            UniverseView.glow(g, x, y, band * (inside ? 2.4 : 1.6), inside ? Color.WHITE : c, wait > 0 ? 0.35 : 0.9);
            g.setGlobalAlpha(wait > 0 ? 0.5 : 1);
            double dot = Math.max(3, band * 0.38);
            if (skinned) arrow(g, x, y, a, band * 2.6);
            else {
                g.setFill(Color.WHITE);
                g.fillOval(x - dot, y - dot, dot * 2, dot * 2);
            }
            if (wait > 0) {
                // L'attente du cercle : un anneau autour du point, qui se remplit jusqu'au prochain clic compté.
                double done = 1 - Math.min(1, wait / Game.LOOP_COOLDOWN), ring2 = dot + 5;
                g.setGlobalAlpha(0.3);
                g.setStroke(Color.WHITE);
                g.setLineWidth(2);
                g.strokeOval(x - ring2, y - ring2, ring2 * 2, ring2 * 2);
                g.setGlobalAlpha(0.95);
                g.setStroke(c);
                g.strokeArc(x - ring2, y - ring2, ring2 * 2, ring2 * 2, 90, -360 * done, ArcType.OPEN);
            }
        }
        // Ce qu'ont valu les derniers clics : le mot et le gain montent du point, et s'effacent ; sur la cible, le clic plante une flèche.
        g.setTextAlign(TextAlignment.CENTER);
        for (LoopMark mark : loopMarks) {
            double t = mark.age / LOOP_MARK_SECONDS;
            Color tint = hitColor(mark.hit);
            if (!mark.text.isEmpty()) UniverseView.glow(g, mark.x, mark.y, 8 + 22 * t, tint, 0.6 * (1 - t));
            if (skinned && !mark.text.isEmpty()) plantedArrow(g, mark.x, mark.y, 1 - t);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFill(tint);
            g.setFont(Font.font(15));
            if (!mark.text.isEmpty()) g.fillText(mark.text, mark.x, mark.y - 16 - 26 * t);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(12));
            if (!mark.gain.isEmpty()) g.fillText(mark.gain, mark.x, mark.y - (mark.text.isEmpty() ? 6 : 32) - 26 * t);
        }
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.SQUARE);
    }
}
