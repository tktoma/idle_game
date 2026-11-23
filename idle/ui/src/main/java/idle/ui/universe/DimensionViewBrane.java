package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : la brane et le clicker. */
public abstract class DimensionViewBrane extends DimensionViewLoop {
    public DimensionViewBrane(Game game) {
        super(game);
    }

    /** La demi-largeur de la membrane à l'écran, en pixels. */
    public double braneHalf() { return Math.min(getWidth(), getHeight()) * BRANE_HALF; }

    /** Où se dessine ce point de la membrane (−1 à 1 sur chaque axe). */
    public double[] braneScreen(double x, double y) {
        return new double[] {getWidth() / 2 + x * braneHalf(), getHeight() / 2 + y * braneHalf()};
    }

    /** Le point de la membrane sous ce point de la vue (hors d'elle, au-delà de ±1). */
    double[] braneAt(double x, double y) {
        double half = Math.max(1e-9, braneHalf());
        return new double[] {(x - getWidth() / 2) / half, (y - getHeight() / 2) / half};
    }

    /** Vrai si ce point de la vue est sur la membrane. */
    public boolean overBrane(double x, double y) {
        double[] at = braneAt(x, y);
        return Math.abs(at[0]) <= Game.BRANE_EDGE && Math.abs(at[1]) <= Game.BRANE_EDGE;
    }

    /** Vrai pendant qu'on tient la membrane. */
    public boolean braneHeld() { return braneHeld; }

    /** La force de la dernière tirée relâchée, de 0 à 1 : le son de la membrane qui part en dépend. */
    public double lastBranePull() { return lastBranePull; }

    /** La force de la tirée en cours, de 0 à 1 : la longueur du glissé, à fond à {@link #BRANE_FULL_PULL}. */
    public double branePull() {
        if (!braneHeld) return 0;
        return Math.min(1, Math.hypot(braneToX - braneFromX, braneToY - braneFromY) / BRANE_FULL_PULL);
    }

    /** Un appui sur la membrane : on la tient là. Faux hors d'elle, ou si l'onde d'avant court encore. */
    boolean pressBrane(double x, double y) {
        if (!game.isBraneOpen() || !overBrane(x, y) || !game.canPullBrane()) return false;
        double[] at = braneAt(x, y);
        braneHeld = true;
        braneFromX = braneToX = at[0];
        braneFromY = braneToY = at[1];
        return true;
    }

    /** La souris tenue glisse : la membrane se tend vers elle. Vrai si on la tenait. */
    public boolean dragBrane(double x, double y) {
        if (!braneHeld || dimension != Dimension.BRANE) return false;
        double[] at = braneAt(x, y);
        braneToX = at[0];
        braneToY = at[1];
        return true;
    }

    /** La membrane relâchée : une onde part du point appuyé, à la force du glissé. Vrai si une onde est partie. */
    public boolean releaseBrane() {
        if (!braneHeld) return false;
        double pull = branePull();
        lastBranePull = pull;
        braneHeld = false;
        return dimension == Dimension.BRANE && game.pullBrane(braneFromX, braneFromY, pull);
    }

    /** La membrane lâchée sans onde : la fenêtre se ferme, ou la vue change de dimension. */
    public void cancelBrane() { braneHeld = false; }

    /** Les fronts d'onde (l'onde et ses rebonds) dessinés à la dernière image : pour les vérifications. */
    public int braneFrontsDrawn() { return braneFronts; }

    /** Le texte du dernier gain monté, ou rien : pour les vérifications. */
    public String lastBraneMark() { return braneMarks.isEmpty() ? "" : braneMarks.get(braneMarks.size() - 1).text; }

    /** Les éclats de bord (le front touche un bord, et rebondit) depuis l'ouverture : pour les vérifications. */
    public long braneEdgeFlashes() { return braneEdgeFlashes; }

    /** Les passages que la vue a comptés sur ce point, tant qu'il est sur la membrane : pour les vérifications. */
    public int branePasses(Game.BranePoint point) { return branePointPasses.getOrDefault(point, 0); }

    /**
     * La brane avance d'une image : les gains et les éclats de bord montent et s'effacent. Chaque front d'image qui
     * vient d'atteindre la membrane allume un éclat sur le bord où l'onde rebondit. Les passages comptés depuis l'image
     * d'avant se retrouvent, chaque front passé sur un point entre l'ancien rayon de son onde et le nouveau (ou sa
     * portée, si elle s'est éteinte), dans l'ordre où ils sont arrivés : le gain de chacun monte de son point, qui
     * compte ses passages.
     */
    void runBrane(double elapsed) {
        for (BraneMark mark : braneMarks) mark.age += elapsed;
        braneMarks.removeIf(mark -> mark.age > BRANE_MARK_SECONDS);
        for (BraneMark flash : braneFlashes) flash.age += elapsed;
        braneFlashes.removeIf(flash -> flash.age > BRANE_FLASH_SECONDS);
        if (!game.isBraneOpen()) {
            braneHeld = false;
            return;
        }
        // L'onde d'avant court encore (la tirée automatique a pu la lancer) : la tirée en cours n'en lancera pas.
        if (braneHeld && !game.canPullBrane()) braneHeld = false;
        long touches = game.braneTouches();
        List<Game.BraneWave> current = game.braneWaves();
        Set<Game.BraneWave> waves = Collections.newSetFromMap(new IdentityHashMap<>());
        waves.addAll(braneSeen.keySet());
        waves.addAll(current);
        Set<Game.BranePoint> points = new LinkedHashSet<>(braneSeenPoints);
        points.addAll(game.branePoints());
        long left = lastBraneTouches < 0 ? 0 : touches - lastBraneTouches;
        for (Game.BraneWave wave : waves) {
            double[] seen = braneSeen.getOrDefault(wave, new double[] {0, 0});
            boolean running = current.stream().anyMatch(other -> other == wave);
            double to = running ? wave.radius() : wave.reach();
            if (lastBraneTouches >= 0) edgeFlashes(wave, seen[0], to);
            if (left > 0) left -= branePasses(wave, seen[0], to, (int) seen[1], points, left);
        }
        braneSeen.clear();
        for (Game.BraneWave wave : current) braneSeen.put(wave, new double[] {wave.radius(), wave.touched()});
        braneSeenPoints = List.copyOf(game.branePoints());
        branePointPasses.keySet().retainAll(new HashSet<>(game.branePoints()));
        lastBraneTouches = touches;
    }

    /** Les fronts d'images de l'onde qui atteignent la membrane entre les rayons {@code from} et {@code to} : un éclat là où ils entrent. */
    void edgeFlashes(Game.BraneWave wave, double from, double to) {
        double[][] xs = Game.images(wave.x()), ys = Game.images(wave.y());
        for (double[] ix : xs) {
            for (double[] iy : ys) {
                int bounces = (int) (ix[1] + iy[1]);
                if (bounces == 0) continue;
                double dx = Math.max(0, Math.abs(ix[0]) - Game.BRANE_EDGE), dy = Math.max(0, Math.abs(iy[0]) - Game.BRANE_EDGE);
                double d = Math.hypot(dx, dy);
                if (d <= from || d > to) continue;
                double[] at = braneScreen(Math.max(-Game.BRANE_EDGE, Math.min(Game.BRANE_EDGE, ix[0])),
                        Math.max(-Game.BRANE_EDGE, Math.min(Game.BRANE_EDGE, iy[0])));
                BraneMark flash = new BraneMark();
                flash.x = at[0];
                flash.y = at[1];
                flash.bounces = bounces;
                flash.vertical = dx > 0 && dy == 0;
                flash.horizontal = dy > 0 && dx == 0;
                if (braneFlashes.size() >= 30) braneFlashes.remove(0);
                braneFlashes.add(flash);
                braneEdgeFlashes++;
            }
        }
    }

    /**
     * Les passages d'une onde entre les rayons {@code from} et {@code to}, au plus {@code most} : le gain de chacun
     * monte de son point, qui en compte un de plus. {@code rank} est le nombre de passages qu'elle avait déjà. Rend le
     * nombre de passages montrés.
     */
    int branePasses(Game.BraneWave wave, double from, double to, int rank, Collection<Game.BranePoint> points, long most) {
        record Pass(double distance, Game.BranePoint point, int bounces) {
        }
        List<Pass> passes = new ArrayList<>();
        double[][] xs = Game.images(wave.x()), ys = Game.images(wave.y());
        for (Game.BranePoint point : points) {
            for (double[] ix : xs) {
                for (double[] iy : ys) {
                    double d = Math.hypot(point.x() - ix[0], point.y() - iy[0]);
                    if (d > from - 1e-12 && d <= to + 1e-12) passes.add(new Pass(d, point, (int) (ix[1] + iy[1])));
                }
            }
        }
        passes.sort(Comparator.comparingDouble(Pass::distance));
        int shown = 0;
        for (Pass pass : passes) {
            if (shown >= most) break;
            shown++;
            branePointPasses.merge(pass.point(), 1, Integer::sum);
            double[] at = braneScreen(pass.point().x(), pass.point().y());
            BraneMark mark = new BraneMark();
            mark.x = at[0];
            mark.y = at[1];
            mark.bounces = pass.bounces();
            mark.text = passText(game.braneReward(rank + shown, pass.bounces()), pass.bounces());
            if (braneMarks.size() >= 24) braneMarks.remove(0);
            braneMarks.add(mark);
        }
        return shown;
    }

    /**
     * Le point ({@code bx}, {@code by}) de la membrane à l'écran, écrit au rang {@code s} de {@code xs} et {@code ys} : creusé vers
     * la souris autour du point tenu (les bords restent tenus, la membrane ne bouge qu'en dedans) ; fermée, une houle lente.
     */
    void warp(double bx, double by, double pullX, double pullY, boolean open, double cx, double cy, double half,
            double[] xs, double[] ys, int s) {
        double near = braneHeld ? Math.exp(-(Math.pow(bx - braneFromX, 2) + Math.pow(by - braneFromY, 2)) / 0.18) : 0;
        double edge = (1 - bx * bx) * (1 - by * by);
        double sway = open ? 0 : 0.015 * Math.sin(time * 1.4 + bx * 3 + by * 2) * edge;
        xs[s] = cx + (bx + pullX * near * edge * 0.6) * half;
        ys[s] = cy + (by + pullY * near * edge * 0.6 + sway) * half;
    }

    /**
     * La toile d'araignée tendue dans le cadre carré : ses fils radiaux du centre aux bords, son fil en spirale
     * polygonale d'un fil radial au suivant, le cadre, et une petite araignée dans le coin haut gauche. Les fils suivent
     * la même déformation que le quadrillage classique.
     */
    void web(GraphicsContext g, double cx, double cy, double half, boolean open, double pullX, double pullY) {
        g.setStroke(THREAD);
        g.setLineCap(StrokeLineCap.ROUND);
        double[] xs = new double[WEB_RADIALS * BRANE_GRID + 1], ys = new double[WEB_RADIALS * BRANE_GRID + 1];
        for (int i = 0; i < WEB_RADIALS; i++) {
            double a = webAngle(i), cos = Math.cos(a), sin = Math.sin(a), reach = 1 / Math.max(Math.abs(cos), Math.abs(sin));
            for (int s = 0; s <= WEB_STEPS; s++) {
                double f = (double) s / WEB_STEPS;
                warp(f * reach * cos, f * reach * sin, pullX, pullY, open, cx, cy, half, xs, ys, s);
            }
            g.setGlobalAlpha(0.55);
            g.setLineWidth(1);
            g.strokePolyline(xs, ys, WEB_STEPS + 1);
        }
        // Le fil en spirale : un pas vers le bord à chaque fil radial passé.
        int points = WEB_RADIALS * BRANE_GRID;
        for (int n = 0; n <= points; n++) {
            double a = webAngle(n % WEB_RADIALS), cos = Math.cos(a), sin = Math.sin(a), reach = 1 / Math.max(Math.abs(cos), Math.abs(sin));
            double f = Math.min(1, 0.06 + 0.94 * n / points);
            warp(f * reach * cos, f * reach * sin, pullX, pullY, open, cx, cy, half, xs, ys, n);
        }
        g.setGlobalAlpha(0.45);
        g.setLineWidth(1);
        g.strokePolyline(xs, ys, points + 1);
        // Le cadre : des fils plus tendus, que la tirée ne bouge pas (les bords restent tenus).
        g.setGlobalAlpha(0.85);
        g.setLineWidth(1.5);
        g.strokeRect(cx - half, cy - half, half * 2, half * 2);
        g.setLineCap(StrokeLineCap.SQUARE);
        spider(g, cx - half * 0.86, cy - half * 0.86, half * 0.05);
    }

    /** Une petite araignée : un corps en deux ovales et huit pattes, qui remuent à peine. */
    void spider(GraphicsContext g, double x, double y, double size) {
        g.setGlobalAlpha(0.95);
        g.setStroke(SPIDER);
        g.setLineWidth(1.2);
        for (int leg = 0; leg < 8; leg++) {
            double side = leg < 4 ? -1 : 1, a = (-0.6 + 0.4 * (leg % 4)) + 0.08 * Math.sin(time * 3 + leg);
            double kx = x + side * Math.cos(a) * size * 1.6, ky = y + Math.sin(a) * size * 1.6 - size * 0.3;
            g.strokeLine(x, y, kx, ky);
            g.strokeLine(kx, ky, kx + side * size * 0.6, ky + size * 1.2);
        }
        g.setFill(SPIDER);
        g.fillOval(x - size * 0.8, y - size * 0.4, size * 1.6, size * 1.4);
        g.fillOval(x - size * 0.45, y - size * 1.1, size * 0.9, size * 0.9);
    }

    /**
     * La membrane : un carré quadrillé, qui se creuse vers la souris tant qu'on la tient, avec le cercle en pointillé de
     * l'onde qui partirait. L'onde et ses rebonds (les cercles de ses images, découpés au carré), d'autant plus épais et
     * colorés qu'ils ont rebondi ; un éclat sur le bord où le front rebondit ; les points, dorés une fois touchés, avec
     * leurs passages ; les gains des passages, qui montent, plus gros après des rebonds. Fermée, la membrane ondule
     * seule, sans point.
     */
    void braneScene(GraphicsContext g, double cx, double cy) {
        Color c = color();
        double half = braneHalf();
        boolean open = game.isBraneOpen();
        if (!skinned) UniverseView.glow(g, cx, cy, half * 1.1, c, 0.1);
        // Le quadrillage, creusé vers la souris autour du point tenu ; fermée, une houle lente. Sous le skin, la toile.
        double pullX = braneHeld ? braneToX - braneFromX : 0, pullY = braneHeld ? braneToY - braneFromY : 0;
        if (skinned) web(g, cx, cy, half, open, pullX, pullY);
        else {
            g.setStroke(c);
            int steps = 24;
            for (int axis = 0; axis < 2; axis++) {
                for (int k = 0; k <= BRANE_GRID; k++) {
                    double fixed = -1 + 2.0 * k / BRANE_GRID;
                    double[] xs = new double[steps + 1], ys = new double[steps + 1];
                    for (int s = 0; s <= steps; s++) {
                        double run = -1 + 2.0 * s / steps;
                        warp(axis == 0 ? run : fixed, axis == 0 ? fixed : run, pullX, pullY, open, cx, cy, half, xs, ys, s);
                    }
                    g.setGlobalAlpha(k == 0 || k == BRANE_GRID ? 0.8 : 0.18);
                    g.setLineWidth(k == 0 || k == BRANE_GRID ? 2 : 1);
                    g.strokePolyline(xs, ys, steps + 1);
                }
            }
        }
        braneFronts = 0;
        if (!open) {
            g.setGlobalAlpha(1);
            return;
        }
        // Les ondes, découpées au carré : la source, puis ses images, dont les cercles sont les rebonds.
        g.save();
        g.beginPath();
        g.moveTo(cx - half, cy - half);
        g.lineTo(cx + half, cy - half);
        g.lineTo(cx + half, cy + half);
        g.lineTo(cx - half, cy + half);
        g.closePath();
        g.clip();
        for (Game.BraneWave wave : game.braneWaves()) {
            double fade = Math.max(0.15, 1 - wave.radius() / Math.max(1e-9, wave.reach()));
            double r = wave.radius() * half;
            if (r <= 0.5) continue;
            for (double[] ix : Game.images(wave.x())) {
                for (double[] iy : Game.images(wave.y())) {
                    double[] at = braneScreen(ix[0], iy[0]);
                    // Seuls les fronts qui entrent dans le carré se voient : les autres, découpés, ne se dessinent pas.
                    if (!reachesSquare(at[0], at[1], r, cx, cy, half)) continue;
                    int bounces = (int) (ix[1] + iy[1]);
                    // Un front qui a rebondi se voit mieux : plus épais, de la couleur de ses rebonds, et un halo. Sur la
                    // toile, l'onde est un frémissement des fils : des cercles plus fins, en fils plus brillants.
                    double thin = skinned ? 0.6 : 1;
                    g.setStroke(bounceColor(bounces));
                    g.setLineWidth((bounces == 0 ? 2.5 : 2 + 0.5 * Math.min(4, bounces)) * thin);
                    g.setGlobalAlpha(Math.min(1, (bounces == 0 ? 0.9 : 0.75 + 0.05 * bounces) * fade + 0.1));
                    g.strokeOval(at[0] - r, at[1] - r, r * 2, r * 2);
                    braneFronts++;
                    // Un second front, plus pâle, juste derrière : l'onde a de l'épaisseur.
                    double inner = r * 0.92;
                    g.setStroke(bounces == 0 ? skinned ? THREAD : c : bounceColor(bounces));
                    g.setLineWidth((bounces == 0 ? 1 : 3) * thin);
                    g.setGlobalAlpha((bounces == 0 ? 0.35 : 0.22) * fade);
                    g.strokeOval(at[0] - inner, at[1] - inner, inner * 2, inner * 2);
                }
            }
            double[] from = braneScreen(wave.x(), wave.y());
            UniverseView.glow(g, from[0], from[1], 10, c, 0.5 * fade);
        }
        // L'onde qui partirait : un cercle en pointillé, de sa portée, autour du point tenu ; un trait vers la souris.
        if (braneHeld) {
            double[] from = braneScreen(braneFromX, braneFromY), to = braneScreen(braneToX, braneToY);
            double reach = game.braneReach(branePull()) * half;
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.75);
            g.setLineDashes(6, 6);
            g.strokeOval(from[0] - reach, from[1] - reach, reach * 2, reach * 2);
            g.setLineDashes((double[]) null);
            g.setStroke(c);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.6 + 0.4 * branePull());
            g.strokeLine(from[0], from[1], to[0], to[1]);
            UniverseView.glow(g, to[0], to[1], 8 + 10 * branePull(), c, 0.8);
        }
        g.restore();
        // Les éclats des bords : là où le front touche un bord et rebondit, un trait de lumière le long du bord.
        for (BraneMark flash : braneFlashes) {
            double t = flash.age / BRANE_FLASH_SECONDS, len = half * 0.14 * (1 + t);
            Color tint = bounceColor(flash.bounces);
            UniverseView.glow(g, flash.x, flash.y, 10 + 18 * t, tint, 0.9 * (1 - t));
            g.setStroke(tint);
            g.setLineWidth(3);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            if (flash.vertical) g.strokeLine(flash.x, flash.y - len, flash.x, flash.y + len);
            else if (flash.horizontal) g.strokeLine(flash.x - len, flash.y, flash.x + len, flash.y);
        }
        // Les points : dorés une fois touchés par l'onde qui court, avec leurs passages ; ils partent avec elle.
        int k = 0;
        g.setTextAlign(TextAlignment.CENTER);
        for (Game.BranePoint point : game.branePoints()) {
            boolean struck = false;
            for (Game.BraneWave wave : game.braneWaves()) struck |= wave.hasStruck(point);
            double[] at = braneScreen(point.x(), point.y());
            double r = 5 + (struck ? 2 : 0.8 * Math.sin(time * 3 + k++));
            Color tint = struck ? BRANE_STRUCK : skinned ? THREAD : c;
            UniverseView.glow(g, at[0], at[1], r * (struck ? 3.4 : 2.6), tint, struck ? 0.9 : skinned ? 0.4 : 0.6);
            g.setGlobalAlpha(1);
            g.setFill(struck ? BRANE_STRUCK : skinned ? DEW : Color.WHITE);
            g.fillOval(at[0] - r * 0.6, at[1] - r * 0.6, r * 1.2, r * 1.2);
            if (skinned) {
                // Une goutte de rosée : son reflet, en haut à gauche.
                g.setFill(Color.WHITE);
                g.fillOval(at[0] - r * 0.4, at[1] - r * 0.4, r * 0.35, r * 0.35);
            }
            int passes = branePasses(point);
            if (struck && passes > 0) {
                g.setFont(Font.font(null, FontWeight.BOLD, 11));
                g.setFill(BRANE_STRUCK);
                g.fillText(passes + (passes > 1 ? " passages" : " passage"), at[0], at[1] + r + 14);
            }
        }
        // La force de la tirée, près de la souris.
        if (braneHeld) {
            double[] to = braneScreen(braneToX, braneToY);
            g.setGlobalAlpha(1);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(13));
            g.fillText("Force " + Math.round(branePull() * 100) + " %", to[0], to[1] - 16);
        }
        // Les gains des passages montent du point, sur un fond sombre ; plus gros et colorés après des rebonds.
        for (BraneMark mark : braneMarks) {
            double t = mark.age / BRANE_MARK_SECONDS, size = 13 + 2.5 * Math.min(4, mark.bounces);
            Color tint = mark.bounces == 0 ? BRANE_STRUCK : bounceColor(mark.bounces);
            double y = mark.y - 16 - 28 * t, width = mark.text.length() * size * 0.56 + 10;
            UniverseView.glow(g, mark.x, mark.y, 8 + 24 * t, tint, 0.6 * (1 - t));
            g.setGlobalAlpha(0.7 * Math.max(0, 1 - t));
            g.setFill(BACK);
            g.fillRoundRect(mark.x - width / 2, y - size, width, size + 6, 8, 8);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFill(tint);
            g.setFont(mark.bounces > 0 ? Font.font(null, FontWeight.BOLD, size) : Font.font(size));
            g.fillText(mark.text, mark.x, y);
        }
        g.setGlobalAlpha(1);
    }

    /** La demi-largeur de la surface à l'écran, en pixels. */
    public double clickerHalf() { return Math.min(getWidth(), getHeight()) * CLICKER_HALF; }

    /** Où se dessine ce point de la surface (0 à 1 sur chaque axe). */
    public double[] clickerScreen(double x, double y) {
        double half = clickerHalf();
        return new double[] {getWidth() / 2 - half + x * 2 * half, getHeight() / 2 - half + y * 2 * half};
    }

    /** Le point de la surface sous ce point de la vue (hors d'elle, hors de 0 à 1). */
    double[] clickerAt(double x, double y) {
        double half = Math.max(1e-9, clickerHalf());
        return new double[] {(x - getWidth() / 2 + half) / (2 * half), (y - getHeight() / 2 + half) / (2 * half)};
    }

    /** Vrai si ce point de la vue est sur la surface. */
    public boolean overClicker(double x, double y) {
        double[] at = clickerAt(x, y);
        return at[0] >= 0 && at[0] <= 1 && at[1] >= 0 && at[1] <= 1;
    }

    /** Un clic sur la surface : il rapporte, et son gain monte du point. Faux hors d'elle ou dimension fermée. */
    boolean pressClicker(double x, double y) {
        if (!game.isClickerOpen() || !overClicker(x, y)) return false;
        double[] at = clickerAt(x, y);
        int stack = game.clickerStackAt(at[0], at[1]);
        lastClickerStack = stack;
        clickerMark(x, y, game.clickClicker(at[0], at[1]), stack, false);
        return true;
    }

    /** Les zones ×2 superposées sous le dernier clic du joueur sur le clicker (0 hors des zones) : son son en dépend. */
    public int lastClickerStack() { return lastClickerStack; }

    void clickerMark(double x, double y, double gain, int stack, boolean auto) {
        ClickerMark mark = new ClickerMark();
        mark.x = x;
        mark.y = y;
        mark.text = "+" + Format.darkMatter(BigNum.of(gain));
        mark.auto = auto;
        mark.stack = stack;
        if (clickerMarks.size() >= 30) clickerMarks.remove(0);
        clickerMarks.add(mark);
    }

    /** La souris bouge sur la vue : sur la surface, la vue dit le multiplicateur des zones sous elle. */
    void hoverClicker(double x, double y) {
        boolean over = dimension == Dimension.CLICKER && overClicker(x, y);
        double[] at = clickerAt(x, y);
        clickerMouseX = over ? at[0] : Double.NaN;
        clickerMouseY = over ? at[1] : Double.NaN;
    }

    /** Les zones superposées sous la souris, ou −1 quand elle n'est pas sur la surface : pour les vérifications. */
    public int clickerHoverStack() {
        return Double.isNaN(clickerMouseX) ? -1 : game.clickerStackAt(clickerMouseX, clickerMouseY);
    }

    /** Les gains de clics qui montent encore : pour les vérifications. */
    public int clickerMarksShown() { return clickerMarks.size(); }

    /** Le texte du dernier gain monté, ou rien : pour les vérifications. */
    public String lastClickerMark() {
        return clickerMarks.isEmpty() ? "" : clickerMarks.get(clickerMarks.size() - 1).text;
    }

    /** Les clics automatiques que la vue a montrés depuis l'ouverture : pour les vérifications. */
    public long clickerAutoFlashes() { return clickerAutoFlashes; }

    /**
     * Le clicker avance d'une image : les gains montent et s'effacent ; les clics automatiques joués depuis l'image
     * d'avant font jaillir leur éclat au meilleur endroit, là où l'automatisme clique.
     */
    void runClicker(double elapsed) {
        for (ClickerMark mark : clickerMarks) mark.age += elapsed;
        clickerMarks.removeIf(mark -> mark.age > CLICKER_MARK_SECONDS);
        if (!game.isClickerOpen()) return;
        long auto = game.clickerAutoClicks();
        if (lastClickerAuto >= 0 && auto > lastClickerAuto) {
            double[] spot = game.clickerBestSpot(), at = clickerScreen(spot[0], spot[1]);
            int stack = game.clickerStackAt(spot[0], spot[1]);
            // Six clics par seconde : un éclat par clic, un peu écartés pour qu'ils se voient tous.
            for (long k = 0; k < Math.min(3, auto - lastClickerAuto); k++) {
                double a = (clickerAutoFlashes + k) * 2.4;
                clickerMark(at[0] + Math.cos(a) * 6, at[1] + Math.sin(a) * 6, game.clickerLastGain(), stack, true);
            }
            clickerAutoFlashes += auto - lastClickerAuto;
        }
        lastClickerAuto = auto;
    }

    /**
     * La surface : un carré sombre au bord lumineux. Les zones colorent chaque endroit selon combien d'entre elles le
     * couvrent : un disque doré par zone, et là où plusieurs se superposent, une case par endroit à la couleur de leur
     * nombre (de l'orange pour deux au blanc pour huit) ; chacune a son bord et l'anneau du temps qu'il lui reste, qui
     * se vide. Sous la souris, le multiplicateur des zones (×N). En haut, la cadence et son multiplicateur. Les clics
     * font une onde et leur gain monte, vert pour les clics automatiques. Fermée, la surface attend, sans rien.
     */
    void clickerScene(GraphicsContext g, double cx, double cy) {
        Color c = color();
        double half = clickerHalf();
        boolean open = game.isClickerOpen();
        if (skinned) {
            // Sur le ciel, la surface est une vitre claire.
            g.setGlobalAlpha(0.18);
            g.setFill(Color.WHITE);
            g.fillRoundRect(cx - half, cy - half, half * 2, half * 2, 14, 14);
            g.setStroke(Color.WHITE);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.7);
            g.strokeRoundRect(cx - half, cy - half, half * 2, half * 2, 14, 14);
        } else {
            UniverseView.glow(g, cx, cy, half * 1.15, c, 0.1 + (open ? 0.04 * Math.min(1, game.clickerRate() / 10) : 0));
            g.setGlobalAlpha(0.6);
            g.setFill(Color.web("#0c0f1c"));
            g.fillRoundRect(cx - half, cy - half, half * 2, half * 2, 14, 14);
            g.setStroke(c);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.8);
            g.strokeRoundRect(cx - half, cy - half, half * 2, half * 2, 14, 14);
        }
        if (!open) {
            UniverseView.glow(g, cx, cy, 10 + 4 * Math.sin(time * 3), c, 0.6);
            g.setGlobalAlpha(1);
            return;
        }
        // Chaque zone, un disque doré ; là où plusieurs se superposent, chaque case prend la couleur de leur nombre.
        // Sous le skin, chaque zone est une grande bulle translucide.
        List<Game.ClickerZone> zones = game.clickerZones();
        int k = 0;
        for (Game.ClickerZone zone : zones) {
            double[] at = clickerScreen(zone.x(), zone.y());
            double r = zone.radius() * half * 2;
            if (skinned) {
                g.setGlobalAlpha(1);
                g.setFill(BUBBLE_FILL);
                g.fillOval(at[0] - r, at[1] - r, r * 2, r * 2);
                bubbleShine(g, at[0], at[1], r, k++);
                continue;
            }
            g.setGlobalAlpha(0.3);
            g.setFill(stackColor(1));
            g.fillOval(at[0] - r, at[1] - r, r * 2, r * 2);
        }
        if (zones.size() > 1) {
            double cell = half * 2 / CLICKER_CELLS;
            for (int i = 0; i < CLICKER_CELLS; i++) {
                for (int j = 0; j < CLICKER_CELLS; j++) {
                    int stack = game.clickerStackAt((i + 0.5) / CLICKER_CELLS, (j + 0.5) / CLICKER_CELLS);
                    if (stack < 2) continue;
                    g.setGlobalAlpha(Math.min(0.9, 0.5 + 0.05 * stack));
                    g.setFill(stackColor(stack));
                    g.fillRect(cx - half + i * cell, cy - half + j * cell, cell, cell);
                }
            }
        }
        // Le bord de chaque zone, et l'anneau du temps qu'il lui reste.
        for (Game.ClickerZone zone : zones) {
            double[] at = clickerScreen(zone.x(), zone.y());
            double r = zone.radius() * half * 2, share = Math.max(0, Math.min(1, zone.left() / zone.lifetime()));
            if (!skinned) {
                g.setStroke(stackColor(1));
                g.setLineWidth(1.5);
                g.setGlobalAlpha(0.8);
                g.strokeOval(at[0] - r, at[1] - r, r * 2, r * 2);
            }
            double ring = r + 5;
            g.setStroke(skinned ? SKY_INK : Color.WHITE);
            g.setLineWidth(3);
            g.setGlobalAlpha(share < 0.25 ? 0.6 + 0.4 * Math.sin(time * 14) : 0.85);
            g.strokeArc(at[0] - ring, at[1] - ring, ring * 2, ring * 2, 90, 360 * share, ArcType.OPEN);
        }
        g.setTextAlign(TextAlignment.CENTER);
        // Sous la souris : combien de zones, et ce qu'elles multiplient.
        if (!Double.isNaN(clickerMouseX)) {
            int stack = game.clickerStackAt(clickerMouseX, clickerMouseY);
            double[] at = clickerScreen(clickerMouseX, clickerMouseY);
            Color tint = stack > 0 ? stackColor(stack) : c;
            g.setStroke(tint);
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.8);
            g.strokeOval(at[0] - 9, at[1] - 9, 18, 18);
            if (stack > 0) {
                g.setGlobalAlpha(1);
                g.setFill(tint);
                g.setFont(Font.font(null, FontWeight.BOLD, 14));
                String text = "×" + ElementText.number(game.clickerStackMultiplier(stack))
                        + (stack > 1 ? " (" + stack + " zones)" : "");
                // Sur un fond sombre : lisible au-dessus des zones.
                double width = text.length() * 8 + 10;
                g.setGlobalAlpha(0.75);
                g.setFill(BACK);
                g.fillRoundRect(at[0] - width / 2, at[1] - 34, width, 22, 8, 8);
                g.setGlobalAlpha(1);
                g.setFill(tint);
                g.fillText(text, at[0], at[1] - 18);
            }
        }
        // La cadence, au-dessus de la surface.
        double rate = game.clickerRate();
        g.setGlobalAlpha(1);
        g.setFill(skinned ? SKY_INK : rate > 0 ? Color.WHITE : Color.web("#8a93b0"));
        g.setFont(Font.font(14));
        g.fillText(cadenceText(rate), cx, cy - half - 12);
        // Les clics : une onde qui s'élargit, et le gain qui monte ; sous le skin, une bulle qui monte et éclate.
        for (ClickerMark mark : clickerMarks) {
            double t = mark.age / CLICKER_MARK_SECONDS, r = 6 + 34 * t;
            Color tint = mark.auto ? CLICKER_AUTO : mark.stack > 0 ? stackColor(mark.stack) : Color.WHITE;
            if (skinned) {
                soapBubble(g, mark.x, mark.y, t);
                tint = mark.auto ? SKY_AUTO : SKY_INK;
            } else {
                g.setStroke(tint);
                g.setLineWidth(2);
                g.setGlobalAlpha(0.8 * (1 - t));
                g.strokeOval(mark.x - r, mark.y - r, r * 2, r * 2);
                UniverseView.glow(g, mark.x, mark.y, 6 + 10 * (1 - t), tint, 0.7 * (1 - t));
            }
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFill(tint);
            g.setFont(Font.font(mark.stack > 0 ? 15 : 13));
            g.fillText(mark.text, mark.x, mark.y - 14 - 30 * t);
        }
        g.setGlobalAlpha(1);
    }

    /** Le contour irisé d'une bulle, dont la teinte tourne avec le temps, et son reflet blanc en haut à gauche. */
    void bubbleShine(GraphicsContext g, double x, double y, double r, int seed) {
        g.setStroke(BUBBLE_HUES[(int) ((time * 6 + seed * 5) % BUBBLE_HUES.length)]);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.7);
        g.strokeOval(x - r, y - r, r * 2, r * 2);
        g.setStroke(Color.WHITE);
        g.setLineWidth(Math.max(1.5, r * 0.08));
        g.setGlobalAlpha(0.8);
        g.strokeArc(x - r * 0.75, y - r * 0.75, r * 1.5, r * 1.5, 110, 40, ArcType.OPEN);
    }

    /** La bulle d'un clic, d'âge {@code t} de 0 à 1 : elle monte, puis éclate en gouttelettes qui s'écartent. */
    void soapBubble(GraphicsContext g, double x, double y, double t) {
        double rise = 40 * t;
        if (t < 0.7) {
            double r = 7 + 6 * (1 - t);
            g.setGlobalAlpha(1);
            g.setFill(BUBBLE_FILL);
            g.fillOval(x - r, y - rise - r, r * 2, r * 2);
            bubbleShine(g, x, y - rise, r, (int) x);
            return;
        }
        double burst = (t - 0.7) / 0.3, spread = 8 + 14 * burst;
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.8 * (1 - burst));
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            g.fillOval(x + Math.cos(a) * spread - 1.5, y - rise + Math.sin(a) * spread - 1.5, 3, 3);
        }
    }

    /** « 3.5 clics par seconde, ×1.18 » : la cadence et ce qu'elle multiplie. */
    public String cadenceText(double rate) {
        return ElementText.number(rate) + (rate >= 2 ? " clics" : " clic") + " par seconde, "
                + Format.multiplier(game.clickerSpeedFactor(rate));
    }
}
