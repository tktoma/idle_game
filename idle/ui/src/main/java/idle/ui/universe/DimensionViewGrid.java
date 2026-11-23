package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.util.List;
import java.util.Locale;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : la grille et le miroir. */
public abstract class DimensionViewGrid extends DimensionViewBrane {
    public DimensionViewGrid(Game game) {
        super(game);
    }

    /** Le côté de la carte de la couche, à l'écran. */
    double gridSide() { return Math.min(getHeight() * 0.8, getWidth() * 0.55); }

    double gridLeft() { return getWidth() * 0.4 - gridSide() / 2; }

    double gridTop() { return getHeight() / 2 - gridSide() / 2; }

    /** Vrai quand la grille est assez petite pour se montrer case par case, et qu'un clic y essaie la case. */
    public boolean gridClickable() { return game.gridDivisions() <= GRID_CLICK_CELLS; }

    /** Les cases par côté que la carte montre : toute la couche sans zoom, quatre fois moins par cran, une petite grille entière. */
    public int gridSpan() {
        int n = game.gridDivisions();
        if (n <= GRID_CLICK_CELLS) return n;
        return (int) Math.max(Math.min(n, GRID_CLICK_CELLS), n / Math.pow(GRID_ZOOM, gridZoom));
    }

    /** La première colonne et la première ligne de la carte : la case visée au milieu, sans sortir de la grille. */
    public int[] gridOrigin() {
        int n = game.gridDivisions(), span = gridSpan();
        return new int[] {Math.max(0, Math.min(n - span, gridSelX - span / 2)), Math.max(0, Math.min(n - span, gridSelY - span / 2))};
    }

    /** Un cran de zoom de plus ({@code +1}) ou de moins ({@code −1}) ; au plus près, la carte montre {@value #GRID_CLICK_CELLS} cases par côté. */
    public void zoomGrid(int step) {
        if (step > 0 && gridSpan() > Math.min(game.gridDivisions(), GRID_CLICK_CELLS)) gridZoom++;
        else if (step < 0) gridZoom = Math.max(0, gridZoom - 1);
    }

    /** Le zoom de la carte : 0 pour toute la couche. */
    public int gridZoom() { return gridZoom; }

    /** Le centre de la case (i, j) de la couche, à l'écran, d'après la partie de la couche que la carte montre. */
    public double[] gridCellScreen(int i, int j) {
        int[] origin = gridOrigin();
        double cell = gridSide() / gridSpan();
        return new double[] {gridLeft() + (i - origin[0] + 0.5) * cell, gridTop() + (j - origin[1] + 0.5) * cell};
    }

    /** La case (i, j) sous ce point de la vue, ou {@code null} hors de la carte. */
    public int[] gridCellAt(double x, double y) {
        int span = gridSpan();
        int[] origin = gridOrigin();
        double cell = gridSide() / span, fx = (x - gridLeft()) / cell, fy = (y - gridTop()) / cell;
        if (fx < 0 || fy < 0 || fx >= span || fy >= span) return null;
        return new int[] {origin[0] + (int) fx, origin[1] + (int) fy};
    }

    /** La couche montrée (z, de 0 à N − 1). */
    public int gridLayer() { return Math.max(0, Math.min(game.gridDivisions() - 1, gridLayer)); }

    /** Montre la couche {@code layer}, ramenée dans la grille. */
    public void setGridLayer(int layer) { gridLayer = Math.max(0, Math.min(game.gridDivisions() - 1, layer)); }

    /** Monte ou descend d'une couche (la molette, les flèches, les boutons de la fenêtre). */
    public void shiftGridLayer(int step) { setGridLayer(gridLayer() + step); }

    /** La case visée : x, y, et la couche z. */
    public int[] gridSelected() {
        int n = game.gridDivisions();
        return new int[] {Math.max(0, Math.min(n - 1, gridSelX)), Math.max(0, Math.min(n - 1, gridSelY)), gridLayer()};
    }

    /** Vise la case (x, y, z), ramenée dans la grille. */
    public void selectGrid(int x, int y, int z) {
        int n = game.gridDivisions();
        gridSelX = Math.max(0, Math.min(n - 1, x));
        gridSelY = Math.max(0, Math.min(n - 1, y));
        setGridLayer(z);
    }

    /** Essaie la case visée. Rend l'essai, ou {@code null} pendant l'attente. */
    public Game.GridProbe tryGrid() {
        int[] at = gridSelected();
        Game.GridProbe probe = game.probeGrid(at[0], at[1], at[2]);
        if (probe != null) {
            noteProbe(probe, false);
            lastGridProbes = game.gridProbes();
        }
        return probe;
    }

    /**
     * Un clic sur la carte : il vise la case sous la souris ; quand la grille se montre entière (petite), il l'essaie
     * aussitôt. Vrai s'il est tombé sur la carte, même pendant l'attente.
     */
    boolean pressGrid(double x, double y) {
        if (!game.isGridOpen()) return false;
        int[] cell = gridCellAt(x, y);
        if (cell == null) return false;
        gridSelX = cell[0];
        gridSelY = cell[1];
        if (gridClickable()) tryGrid();
        return true;
    }

    /**
     * Retient un essai, et fait monter son gain de sa case (« Trouvé ! ×5 » quand il a trouvé le point). Un essai qui a
     * fait partir son point (trouvé, ou le dernier essai) ne garde pas de couleur : les essais contre ce point s'oublient.
     */
    void noteProbe(Game.GridProbe probe, boolean auto) {
        gridProbed.put(gridKey(probe.i(), probe.j(), probe.k()), probe);
        forgetDeadProbes();
        GridMark mark = new GridMark();
        mark.i = probe.i();
        mark.j = probe.j();
        mark.k = probe.k();
        mark.found = probe.found();
        mark.auto = auto;
        mark.text = (probe.found() ? "Trouvé ! ×" + ElementText.number(Game.GRID_FOUND) + "  " : "")
                + "+" + Format.darkMatter(BigNum.of(probe.gain()));
        if (gridMarks.size() >= 12) gridMarks.remove(0);
        gridMarks.add(mark);
    }

    /** Oublie les cases essayées contre un point qui n'est plus là : elles ne se colorent plus. */
    void forgetDeadProbes() { gridProbed.values().removeIf(probe -> !game.isGridPointAlive(probe.pointId())); }

    /** La souris bouge : la case sous elle s'éclaire. */
    void hoverGrid(double x, double y) {
        int[] cell = dimension == Dimension.GRID && game.isGridOpen() ? gridCellAt(x, y) : null;
        gridHoverI = cell == null ? -1 : cell[0];
        gridHoverJ = cell == null ? -1 : cell[1];
    }

    /** Ce qu'a donné la case (i, j, k) au dernier essai qui l'a touchée, ou {@code null} : pour les vérifications. */
    public Game.GridProbe gridProbedAt(int i, int j, int k) { return gridProbed.get(gridKey(i, j, k)); }

    /** Le texte du dernier gain monté, ou rien : pour les vérifications. */
    public String lastGridMark() { return gridMarks.isEmpty() ? "" : gridMarks.get(gridMarks.size() - 1).text; }

    /** Les essais automatiques que la vue a montrés depuis l'ouverture : pour les vérifications. */
    public long gridAutoFlashes() { return gridAutoFlashes; }

    /**
     * La couleur d'une case essayée : sans l'affichage de la distance (niveau 0), neutre, rien ne dit si elle brûle ;
     * ensuite, du bleu (froid, loin du point) au rouge (chaud, tout près), en part de la grille ; dorée quand elle a
     * trouvé le point.
     */
    public Color gridProbeColor(Game.GridProbe probe) {
        if (probe.found()) return GRID_FOUND;
        if (game.gridReveal() < 1) return GRID_PROBED;
        double heat = Math.max(0, Math.min(1, 1 - probe.distance() / (game.gridDivisions() * 0.6)));
        return heat < 0.5 ? GRID_COLD.interpolate(GRID_MILD, heat * 2) : GRID_MILD.interpolate(GRID_HOT, heat * 2 - 1);
    }

    /**
     * La couche (z) puis la colonne (x) du point visé par le dernier essai, autant que l'affichage le permet : −1 pour ce
     * qu'il ne montre pas encore (la couche au niveau 2, la colonne au niveau 3), sans essai, ou une fois le point parti
     * (trouvé, ou ses essais épuisés : il s'est caché ailleurs, et il n'y a plus rien à en dire).
     */
    public int[] gridRevealed() {
        Game.GridProbe last = game.gridLastProbe();
        if (last == null || game.gridReveal() < 2 || !game.isGridPointAlive(last.pointId())) return new int[] {-1, -1};
        Game.GridPoint aimed = null;
        for (int n = 0; n < game.gridPoints().size(); n++) if (game.gridPointId(n) == last.pointId()) aimed = game.gridPoints().get(n);
        if (aimed == null) return new int[] {-1, -1};
        return new int[] {game.gridCell(aimed.z()), game.gridReveal() >= 3 ? game.gridCell(aimed.x()) : -1};
    }

    /**
     * La grille avance d'une image : les gains montent et s'effacent ; les cases essayées contre un point parti (trouvé,
     * ou ses essais épuisés) s'oublient, leurs couleurs ne diraient plus rien ; une grille réduite oublie
     * toutes les cases essayées, revient à la carte entière et ramène la case visée en elle ; les essais de l'automatisme depuis l'image d'avant se montrent comme ceux du joueur.
     */
    void runGrid(double elapsed) {
        for (GridMark mark : gridMarks) mark.age += elapsed;
        gridMarks.removeIf(mark -> mark.age > GRID_MARK_SECONDS);
        if (!game.isGridOpen()) return;
        if (gridProbedDivisions != game.gridDivisions()) {
            gridProbed.clear();
            gridProbedDivisions = game.gridDivisions();
            gridZoom = 0;
            int[] at = gridSelected();
            selectGrid(at[0], at[1], at[2]);
        }
        long probes = game.gridProbes();
        if (lastGridProbes >= 0 && probes > lastGridProbes && game.gridLastProbe() != null) {
            // Plusieurs essais depuis l'image d'avant (une absence) : on ne montre que le dernier.
            noteProbe(game.gridLastProbe(), true);
            gridAutoFlashes += probes - lastGridProbes;
        }
        lastGridProbes = probes;
        forgetDeadProbes();
    }

    /**
     * La grille : à gauche, la carte de la couche montrée (toute la couche, ou la partie autour de la case visée au
     * zoom), avec ses cases quand elles se voient, celles déjà essayées colorées (selon l'affichage), la case visée, la
     * case sous la souris, et les gains qui montent ; à droite, le cube vu de biais, la couche montrée, les cases
     * essayées, et ce que l'affichage montre du point visé (sa couche, puis sa colonne). Au-dessus, la couche et ce que
     * la carte montre ; pendant l'attente, la carte pâlit. Fermée, un cube vide.
     */
    void gridScene(GraphicsContext g, double w, double h) {
        Color c = color();
        boolean open = game.isGridOpen();
        int n = open ? game.gridDivisions() : Game.GRID_MIN_CELLS, layer = open ? gridLayer() : 0, span = open ? gridSpan() : n;
        int[] origin = open ? gridOrigin() : new int[] {0, 0}, revealed = open ? gridRevealed() : new int[] {-1, -1};
        double side = gridSide(), left = gridLeft(), top = gridTop(), cell = side / span;
        boolean waiting = open && game.gridWait() > 0;
        // Sous le skin, la carte est une plaque de démineur : ses cases en relief, et un cadre biseauté.
        boolean bevels = skinned && span <= 64 && cell >= 6;
        if (skinned) {
            g.setGlobalAlpha(1);
            g.setFill(MINE_FACE);
            g.fillRect(left, top, side, side);
            if (bevels) {
                for (int i = 0; i < span; i++) {
                    for (int j = 0; j < span; j++) bevel(g, left + i * cell, top + j * cell, cell, true);
                }
            }
        }
        // Les cases essayées de la couche, dans la partie que la carte montre (trois pixels au moins : elles se voient).
        for (Game.GridProbe probe : gridProbed.values()) {
            if (probe.k() != layer || probe.i() < origin[0] || probe.j() < origin[1] || probe.i() >= origin[0] + span
                    || probe.j() >= origin[1] + span) continue;
            double size = Math.max(3, cell - 2), x = left + (probe.i() - origin[0] + 0.5) * cell, y = top + (probe.j() - origin[1] + 0.5) * cell;
            if (skinned) {
                mineCell(g, probe, x, y, cell, waiting);
                continue;
            }
            g.setGlobalAlpha(waiting ? 0.45 : 0.75);
            g.setFill(gridProbeColor(probe));
            g.fillRect(x - size / 2, y - size / 2, size, size);
        }
        if (gridHoverI >= 0 && cell >= 3) {
            g.setGlobalAlpha(0.25);
            g.setFill(Color.WHITE);
            g.fillRect(left + (gridHoverI - origin[0]) * cell, top + (gridHoverJ - origin[1]) * cell, cell, cell);
        }
        // La colonne du point visé (affichage 3) : une bande sur toute la couche, si elle est dans la carte.
        if (revealed[1] >= origin[0] && revealed[1] < origin[0] + span) {
            g.setStroke(GRID_HINT);
            g.setLineWidth(3);
            g.setGlobalAlpha(revealed[0] == layer ? 0.95 : 0.45);
            g.strokeRect(left + (revealed[1] - origin[0]) * cell + 1.5, top + 1.5, Math.max(3, cell - 3), side - 3);
        }
        // Les lignes des cases quand elles se voient ; sinon, un repère tous les dixièmes. Les cases en relief ont déjà leurs biseaux.
        g.setStroke(skinned ? MINE_DARK : c);
        int every = span <= 64 ? 1 : Math.max(1, span / 10);
        for (int k = 0; k <= span && !bevels; k += every) {
            boolean edge = k == 0;
            g.setLineWidth(edge ? 2 : 1);
            g.setGlobalAlpha(edge ? 0.8 : span <= 64 ? 0.3 : 0.15);
            g.strokeLine(left + k * cell, top, left + k * cell, top + side);
            g.strokeLine(left, top + k * cell, left + side, top + k * cell);
        }
        if (skinned) bevel(g, left - 4, top - 4, side + 8, false);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.8);
        g.strokeRect(left, top, side, side);
        // La case visée : un carré et une croix.
        if (open) {
            int[] at = gridSelected();
            double[] p = gridCellScreen(at[0], at[1]);
            double r = Math.max(5, cell / 2);
            g.setStroke(skinned ? MINE_TEXT : Color.WHITE);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.95);
            g.strokeRect(p[0] - r, p[1] - r, r * 2, r * 2);
            if (!gridClickable()) {
                g.strokeLine(p[0] - r - 8, p[1], p[0] - r - 2, p[1]);
                g.strokeLine(p[0] + r + 2, p[1], p[0] + r + 8, p[1]);
                g.strokeLine(p[0], p[1] - r - 8, p[0], p[1] - r - 2);
                g.strokeLine(p[0], p[1] + r + 2, p[0], p[1] + r + 8);
            }
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.setGlobalAlpha(1);
        g.setFill(revealed[0] == layer ? GRID_HINT : skinned ? MINE_TEXT : Color.WHITE);
        g.setFont(Font.font(14));
        g.fillText(layerText(layer, n) + (revealed[0] == layer ? " : le point visé est dans cette couche" : ""), left + side / 2, top - 12);
        if (open && span < n) {
            g.setFill(skinned ? MINE_GRAY : Color.web("#8a93b0"));
            g.setFont(Font.font(12));
            g.fillText("x de " + origin[0] + " à " + (origin[0] + span - 1) + ", y de " + origin[1] + " à " + (origin[1] + span - 1),
                    left + side / 2, top + side + 38);
        }
        // Dans les cases essayées assez grandes, le gain (et la distance, si l'affichage la montre) ; le démineur y met son chiffre.
        if (cell >= 38 && !skinned) {
            g.setFont(Font.font(11));
            for (Game.GridProbe probe : gridProbed.values()) {
                if (probe.k() != layer || probe.i() < origin[0] || probe.j() < origin[1] || probe.i() >= origin[0] + span
                        || probe.j() >= origin[1] + span) continue;
                double[] at = gridCellScreen(probe.i(), probe.j());
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.9);
                g.fillText("+" + Format.darkMatter(BigNum.of(probe.gain())), at[0], at[1] + (game.gridReveal() >= 1 ? -2 : 4));
                if (game.gridReveal() >= 1) g.fillText(ElementText.number(probe.distance()), at[0], at[1] + 12);
            }
        }
        if (waiting) {
            g.setGlobalAlpha(0.85);
            g.setFill(skinned ? MINE_GRAY : Color.web("#8a93b0"));
            g.setFont(Font.font(13));
            g.fillText(String.format(Locale.ROOT, "Attente : %.1f s", game.gridWait()), left + side / 2, top + side + 20);
        }
        // Le cube vu de biais.
        gridCube(g, w * 0.82, h / 2, Math.min(w * 0.12, h * 0.26), n, layer, revealed, c);
        // Les gains des essais montent de leur case (de la couche montrée, dans la carte) ; trouvé, un éclat doré.
        for (GridMark mark : gridMarks) {
            double t = mark.age / GRID_MARK_SECONDS;
            if (mark.k != layer || mark.i < origin[0] || mark.j < origin[1] || mark.i >= origin[0] + span || mark.j >= origin[1] + span) continue;
            double[] at = gridCellScreen(mark.i, mark.j);
            Color tint = mark.found ? GRID_FOUND : mark.auto ? Color.web("#9be7a8") : Color.WHITE;
            double glow = Math.max(10, mark.found ? cell * 1.2 : cell * 0.5);
            UniverseView.glow(g, at[0], at[1], glow * (0.6 + t), tint, (mark.found ? 0.9 : 0.5) * (1 - t));
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFill(skinned ? mark.found ? MINE_HIT : mark.auto ? SKY_AUTO : MINE_TEXT : tint);
            g.setFont(mark.found ? Font.font(null, FontWeight.BOLD, 17) : Font.font(14));
            g.fillText(mark.text, at[0], at[1] - 10 - 30 * t);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Une case essayée, en démineur : elle s'enfonce (plate, cernée de sombre) et montre, si la case est assez grande,
     * un chiffre coloré quand l'affichage dit la distance (1 au plus loin, 8 au plus près), un drapeau sinon ; trouvée,
     * une mine sur fond rouge. Petite, elle prend la couleur de son chiffre.
     */
    void mineCell(GraphicsContext g, Game.GridProbe probe, double x, double y, double cell, boolean waiting) {
        double size = Math.max(3, cell - 1);
        g.setGlobalAlpha(waiting ? 0.6 : 1);
        int digit = mineDigit(probe);
        boolean revealed = game.gridReveal() >= 1;
        if (cell < 10) {
            g.setFill(probe.found() ? MINE_HIT : revealed ? MINE_DIGITS[digit - 1] : FLAG);
            g.fillRect(x - size / 2, y - size / 2, size, size);
            return;
        }
        g.setFill(probe.found() ? MINE_HIT : MINE_SUNK);
        g.fillRect(x - size / 2, y - size / 2, size, size);
        g.setStroke(MINE_DARK);
        g.setLineWidth(1);
        g.strokeRect(x - size / 2 + 0.5, y - size / 2 + 0.5, size - 1, size - 1);
        if (probe.found()) {
            // La mine : un disque noir, quatre pointes, un reflet.
            double r = cell * 0.22;
            g.setFill(MINE_TEXT);
            g.fillOval(x - r, y - r, r * 2, r * 2);
            g.setStroke(MINE_TEXT);
            g.setLineWidth(Math.max(1.5, r * 0.25));
            g.strokeLine(x - r * 1.5, y, x + r * 1.5, y);
            g.strokeLine(x, y - r * 1.5, x, y + r * 1.5);
            g.setFill(Color.WHITE);
            g.fillOval(x - r * 0.6, y - r * 0.6, r * 0.5, r * 0.5);
        } else if (revealed) {
            g.setFill(MINE_DIGITS[digit - 1]);
            g.setFont(Font.font(null, FontWeight.BOLD, cell * 0.55));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(String.valueOf(digit), x, y + cell * 0.2);
        } else {
            // Le drapeau : un mât et un fanion rouge.
            g.setStroke(MINE_TEXT);
            g.setLineWidth(Math.max(1, cell * 0.06));
            g.strokeLine(x, y - cell * 0.3, x, y + cell * 0.3);
            g.strokeLine(x - cell * 0.2, y + cell * 0.3, x + cell * 0.2, y + cell * 0.3);
            g.setFill(FLAG);
            g.fillPolygon(new double[] {x, x - cell * 0.3, x}, new double[] {y - cell * 0.3, y - cell * 0.12, y + cell * 0.05}, 3);
        }
    }

    /** Le chiffre de démineur d'un essai, de 1 (loin du point) à 8 (tout près), d'après la même chaleur que sa couleur. */
    int mineDigit(Game.GridProbe probe) {
        double heat = Math.max(0, Math.min(1, 1 - probe.distance() / (game.gridDivisions() * 0.6)));
        return 1 + (int) Math.round(7 * heat);
    }

    /**
     * Le cube de biais : ses arêtes, la couche montrée en plaque, la case visée, les cases essayées en petits losanges
     * colorés, et, selon l'affichage, la couche du point visé (un contour rose) et sa colonne (un trait sur cette couche).
     */
    void gridCube(GraphicsContext g, double ox, double oy, double s, int n, int layer, int[] revealed, Color c) {
        if (skinned) mineFaces(g, ox, oy, s, n);
        g.setStroke(skinned ? MINE_DARK : c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.7);
        for (int a = 0; a < 8; a++) {
            for (int bit : new int[] {1, 2, 4}) {
                int b = a ^ bit;
                if (b < a) continue;
                double[] p = iso(ox, oy, s, a & 1, (a >> 1) & 1, (a >> 2) & 1), q = iso(ox, oy, s, b & 1, (b >> 1) & 1, (b >> 2) & 1);
                g.strokeLine(p[0], p[1], q[0], q[1]);
            }
        }
        slab(g, ox, oy, s, (layer + 0.5) / n, c, 0.3, false);
        if (revealed[0] >= 0) slab(g, ox, oy, s, (revealed[0] + 0.5) / n, GRID_HINT, 0.9, true);
        if (revealed[0] >= 0 && revealed[1] >= 0) {
            double x = (revealed[1] + 0.5) / n, z = (revealed[0] + 0.5) / n;
            double[] p = iso(ox, oy, s, x, 0, z), q = iso(ox, oy, s, x, 1, z);
            g.setStroke(GRID_HINT);
            g.setLineWidth(3);
            g.setGlobalAlpha(0.95);
            g.strokeLine(p[0], p[1], q[0], q[1]);
        }
        for (Game.GridProbe probe : gridProbed.values()) {
            double[] at = iso(ox, oy, s, (probe.i() + 0.5) / n, (probe.j() + 0.5) / n, (probe.k() + 0.5) / n);
            double r = Math.max(2.5, s / n * 0.3);
            g.setGlobalAlpha(probe.k() == layer ? 1 : 0.55);
            g.setFill(gridProbeColor(probe));
            g.fillPolygon(new double[] {at[0], at[0] + r, at[0], at[0] - r}, new double[] {at[1] - r * 0.6, at[1], at[1] + r * 0.6, at[1]}, 4);
        }
        if (game.isGridOpen()) {
            int[] at = gridSelected();
            double[] p = iso(ox, oy, s, (at[0] + 0.5) / n, (at[1] + 0.5) / n, (at[2] + 0.5) / n);
            g.setStroke(skinned ? MINE_TEXT : Color.WHITE);
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.9);
            g.strokeOval(p[0] - 4, p[1] - 4, 8, 8);
        }
        for (GridMark mark : gridMarks) {
            double[] at = iso(ox, oy, s, (mark.i + 0.5) / n, (mark.j + 0.5) / n, (mark.k + 0.5) / n);
            UniverseView.glow(g, at[0], at[1], 10 + 10 * mark.age, mark.found ? GRID_FOUND : Color.WHITE, 0.8 * Math.max(0, 1 - mark.age / GRID_MARK_SECONDS));
        }
        g.setGlobalAlpha(1);
    }

    /** Les agrandissements du plateau que la vue a montrés depuis l'ouverture : pour les vérifications. */
    public long mirrorGrowths() { return mirrorGrowths; }

    /** Le côté d'une case du plateau, à l'écran. */
    double mirrorCell() {
        return Math.min(getWidth() * 0.8 / game.mirrorColumns(), getHeight() * 0.72 / game.mirrorRows());
    }

    /** Le centre de la case ({@code column}, {@code row}), à l'écran. */
    public double[] mirrorCellScreen(int column, int row) {
        double cell = mirrorCell(), left = getWidth() / 2 - cell * game.mirrorColumns() / 2, top = getHeight() / 2 - cell * game.mirrorRows() / 2 + 14;
        return new double[] {left + (column + 0.5) * cell, top + (row + 0.5) * cell};
    }

    /** La case sous ce point de la vue, ou {@code null} hors du plateau. */
    public Game.MirrorCell mirrorCellAt(double x, double y) {
        double cell = mirrorCell(), left = getWidth() / 2 - cell * game.mirrorColumns() / 2, top = getHeight() / 2 - cell * game.mirrorRows() / 2 + 14;
        double fx = (x - left) / cell, fy = (y - top) / cell;
        if (fx < 0 || fy < 0 || fx >= game.mirrorColumns() || fy >= game.mirrorRows()) return null;
        return new Game.MirrorCell((int) fx, (int) fy);
    }

    /** Un clic sur le plateau : sur la moitié droite, pendant la reproduction, il joue la case. Vrai s'il est tombé sur le plateau. */
    boolean pressMirror(double x, double y) {
        if (!game.isMirrorOpen()) return false;
        Game.MirrorCell cell = mirrorCellAt(x, y);
        if (cell == null) return false;
        clickMirror(cell.column(), cell.row());
        return true;
    }

    /** Joue la case ({@code column}, {@code row}) : elle s'éclaire, verte si elle était juste, rouge sinon ; rien si le clic est sans effet. */
    public Game.MirrorAnswer clickMirror(int column, int row) {
        Game.MirrorAnswer answer = game.clickMirror(column, row);
        lastMirrorCount = mirrorCount();
        lastMirrorMistakes = game.mirrorMistakes();
        if (answer != Game.MirrorAnswer.IGNORED) mirrorFlash(column, row, answer == Game.MirrorAnswer.RIGHT || answer == Game.MirrorAnswer.SUCCESS, false);
        return answer;
    }

    void mirrorFlash(int column, int row, boolean right, boolean auto) {
        MirrorFlash flash = new MirrorFlash();
        flash.column = column;
        flash.row = row;
        flash.right = right;
        flash.auto = auto;
        if (mirrorFlashes.size() >= 12) mirrorFlashes.remove(0);
        mirrorFlashes.add(flash);
    }

    /** Tout ce qu'un clic fait avancer : cases reproduites, erreurs, motifs réussis et manqués. */
    long mirrorCount() {
        return game.mirrorPosition() + game.mirrorMistakes() * 1000L + (game.mirrorSuccesses() + game.mirrorFails()) * 1_000_000L;
    }

    /** Les clics de l'automatisme que la vue a montrés depuis l'ouverture : pour les vérifications. */
    public long mirrorAutoFlashes() { return mirrorAutoFlashes; }

    /** Les cases du motif allumées à gauche à la dernière image (une pendant l'allumage, aucune sinon) : pour les vérifications. */
    public int mirrorPatternDrawn() { return mirrorPatternDrawn; }

    /** Les cases reproduites montrées à droite à la dernière image : pour les vérifications. */
    public int mirrorReproducedDrawn() { return mirrorReproducedDrawn; }

    /**
     * Le miroir avance d'une image : les éclats des clics s'éteignent (un plateau agrandi les efface) ; ce que
     * l'automatisme a joué depuis l'image d'avant s'éclaire comme un clic du joueur : la case reproduite en vert, son
     * erreur en rouge, là où il a cliqué (la case voisine de la bonne, sur la même ligne).
     */
    void runMirror(double elapsed) {
        for (MirrorFlash flash : mirrorFlashes) flash.age += elapsed;
        mirrorFlashes.removeIf(flash -> flash.age > MIRROR_FLASH_SECONDS);
        if (!game.isMirrorOpen()) return;
        if (mirrorShownColumns != game.mirrorColumns()) {
            mirrorFlashes.clear();
            // Le plateau s'agrandit : un éclat le dit.
            if (mirrorShownColumns >= 0 && game.mirrorColumns() > mirrorShownColumns) {
                mirrorGrowAge = 0;
                mirrorGrowths++;
            }
        }
        mirrorShownColumns = game.mirrorColumns();
        if (mirrorGrowAge >= 0) mirrorGrowAge = mirrorGrowAge + elapsed > MIRROR_GROW_SECONDS ? -1 : mirrorGrowAge + elapsed;
        long count = mirrorCount();
        if (lastMirrorCount >= 0 && count != lastMirrorCount && game.isDimensionAutoPlayed(9)) {
            List<Game.MirrorCell> pattern = game.mirrorPattern();
            int position = Math.min(game.mirrorPosition(), pattern.size());
            boolean failed = game.mirrorPhase() == Game.MirrorPhase.PAUSE && game.mirrorLastResult() == Game.MirrorAnswer.FAIL;
            boolean mistaken = failed || game.mirrorMistakes() > lastMirrorMistakes;
            if (mistaken && position < pattern.size()) {
                Game.MirrorCell target = game.mirrored(pattern.get(position));
                int column = target.column() + 1 < game.mirrorColumns() ? target.column() + 1 : game.mirrorHalf();
                mirrorFlash(column, target.row(), false, true);
            } else if (position > 0) {
                Game.MirrorCell done = game.mirrored(pattern.get(position - 1));
                mirrorFlash(done.column(), done.row(), true, true);
            }
            mirrorAutoFlashes++;
        }
        lastMirrorCount = count;
        lastMirrorMistakes = game.mirrorMistakes();
    }

    /**
     * Le plateau : ses cases, le miroir en trait lumineux au milieu. Pendant l'allumage, la case du motif qui s'allume, à
     * gauche, et son rang ; pendant la reproduction, les cases déjà reproduites à droite, jamais le motif ; pendant la
     * pause, le résultat. Au-dessus, la longueur, le record et les erreurs. Les clics s'éclairent, verts s'ils sont
     * justes, rouges sinon. Fermé, un plateau vide.
     */
    void mirrorScene(GraphicsContext g, double w, double h) {
        Color c = color();
        boolean open = game.isMirrorOpen();
        int columns = open ? game.mirrorColumns() : 2 * Game.MIRROR_HALF, rows = open ? game.mirrorRows() : Game.MIRROR_ROWS;
        double cell = open ? mirrorCell() : Math.min(w * 0.8 / columns, h * 0.72 / rows);
        double left = w / 2 - cell * columns / 2, top = h / 2 - cell * rows / 2 + 14;
        mirrorPatternDrawn = 0;
        mirrorReproducedDrawn = 0;
        // Le vitrail : un halo de lumière qui traverse, puis chaque case en verre sombre cerné de plomb.
        if (skinned) UniverseView.glow(g, left + cell * columns / 2 + Math.sin(time * 0.3) * cell, top + rows * cell / 2, cell * rows * 0.9, DAYLIGHT, 0.18);
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                if (skinned) {
                    glass(g, left + column * cell, top + row * cell, cell, GLASS_DARK[glassIndex(column, row)]);
                    continue;
                }
                boolean right = column >= columns / 2;
                g.setGlobalAlpha(right ? 0.5 : 0.3);
                g.setFill(Color.web(right ? "#141a2c" : "#10141f"));
                g.fillRoundRect(left + column * cell + 3, top + row * cell + 3, cell - 6, cell - 6, 8, 8);
                g.setStroke(c);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.35);
                g.strokeRoundRect(left + column * cell + 3, top + row * cell + 3, cell - 6, cell - 6, 8, 8);
            }
        }
        g.setTextAlign(TextAlignment.CENTER);
        if (open) {
            List<Game.MirrorCell> pattern = game.mirrorPattern();
            switch (game.mirrorPhase()) {
                case SHOW -> {
                    // Seule la case qui s'allume se voit, et son rang : le motif ne se montre jamais en entier.
                    int shown = game.mirrorShown();
                    if (shown >= 0 && shown < pattern.size()) {
                        Game.MirrorCell lit = pattern.get(shown);
                        double[] at = mirrorCellScreen(lit.column(), lit.row());
                        UniverseView.glow(g, at[0], at[1], cell * 0.9, skinned ? Color.WHITE : MIRROR_LIT, 0.9);
                        g.setGlobalAlpha(1);
                        if (skinned) glass(g, at[0] - cell / 2, at[1] - cell / 2, cell, GLASS_LIT[glassIndex(lit.column(), lit.row())]);
                        else {
                            g.setFill(MIRROR_LIT);
                            g.fillRoundRect(at[0] - cell / 2 + 5, at[1] - cell / 2 + 5, cell - 10, cell - 10, 8, 8);
                        }
                        g.setFill(BACK);
                        g.setFont(Font.font(null, FontWeight.BOLD, Math.max(12, cell * 0.3)));
                        g.fillText(String.valueOf(shown + 1), at[0], at[1] + cell * 0.1);
                        mirrorPatternDrawn = 1;
                    }
                }
                case INPUT -> {
                    // Les cases déjà reproduites, à droite, avec leur rang ; la suite reste à trouver.
                    for (int n = 0; n < Math.min(game.mirrorPosition(), pattern.size()); n++) {
                        Game.MirrorCell done = game.mirrored(pattern.get(n));
                        double[] at = mirrorCellScreen(done.column(), done.row());
                        if (skinned) {
                            g.setGlobalAlpha(1);
                            glass(g, at[0] - cell / 2, at[1] - cell / 2, cell, GLASS_LIT[glassIndex(done.column(), done.row())]);
                        } else {
                            g.setGlobalAlpha(0.55);
                            g.setFill(MIRROR_DONE);
                            g.fillRoundRect(at[0] - cell / 2 + 6, at[1] - cell / 2 + 6, cell - 12, cell - 12, 8, 8);
                        }
                        g.setGlobalAlpha(1);
                        g.setFill(BACK);
                        g.setFont(Font.font(Math.max(11, cell * 0.25)));
                        g.fillText(String.valueOf(n + 1), at[0], at[1] + cell * 0.08);
                        mirrorReproducedDrawn++;
                    }
                }
                case PAUSE -> {
                    Game.MirrorAnswer result = game.mirrorLastResult();
                    if (result == Game.MirrorAnswer.SUCCESS || result == Game.MirrorAnswer.FAIL) {
                        boolean success = result == Game.MirrorAnswer.SUCCESS;
                        g.setGlobalAlpha(1);
                        g.setFill(success ? MIRROR_DONE : MIRROR_WRONG);
                        g.setFont(Font.font(null, FontWeight.BOLD, 22));
                        g.fillText((success ? "Réussi ! +" : "Manqué : +") + Format.darkMatter(BigNum.of(game.mirrorLastGain())),
                                w / 2, top + rows * cell + 30);
                    }
                }
            }
        }
        // Le miroir : un trait lumineux entre les deux moitiés ; sur le vitrail, une baguette de plomb.
        double mx = left + cell * columns / 2;
        if (skinned) {
            g.setGlobalAlpha(1);
            g.setStroke(LEAD);
            g.setLineWidth(7);
            g.strokeLine(mx, top - 6, mx, top + rows * cell + 6);
            g.setStroke(LEAD_SHINE);
            g.setLineWidth(1.5);
            g.strokeLine(mx - 1.5, top - 6, mx - 1.5, top + rows * cell + 6);
        } else {
            for (int k = 0; k < 6; k++) UniverseView.glow(g, mx, top + (k + 0.5) * rows * cell / 6, cell * 0.5, c, 0.35 + 0.1 * Math.sin(time * 2 + k));
            g.setStroke(Color.WHITE);
            g.setLineWidth(3);
            g.setGlobalAlpha(0.9);
            g.strokeLine(mx, top - 6, mx, top + rows * cell + 6);
        }
        // Les éclats des clics.
        for (MirrorFlash flash : mirrorFlashes) {
            // Un clic ne se joue qu'à droite : rien ne s'éclaire jamais du côté du motif.
            if (flash.column < columns / 2 || flash.column >= columns || flash.row >= rows) continue;
            double t = flash.age / MIRROR_FLASH_SECONDS;
            double[] at = mirrorCellScreen(flash.column, flash.row);
            Color tint = flash.right ? MIRROR_DONE : MIRROR_WRONG;
            UniverseView.glow(g, at[0], at[1], cell * (0.5 + 0.5 * t), tint, 0.85 * (1 - t));
            g.setStroke(tint);
            g.setLineWidth(3);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.strokeRoundRect(at[0] - cell / 2 + 3, at[1] - cell / 2 + 3, cell - 6, cell - 6, 8, 8);
        }
        // L'agrandissement du plateau : son bord s'illumine, et une ligne le dit.
        if (mirrorGrowAge >= 0) {
            double t = mirrorGrowAge / MIRROR_GROW_SECONDS;
            g.setStroke(MIRROR_LIT);
            g.setLineWidth(3);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.strokeRoundRect(left - 6 - 10 * t, top - 6 - 10 * t, columns * cell + 12 + 20 * t, rows * cell + 12 + 20 * t, 12, 12);
            g.setFill(MIRROR_LIT);
            g.setFont(Font.font(null, FontWeight.BOLD, 20));
            g.fillText("Le plateau s'agrandit !", w / 2, top + rows * cell / 2);
        }
        if (open) {
            g.setGlobalAlpha(1);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(14));
            g.fillText(mirrorStatus(), w / 2, top - 16);
        }
        g.setGlobalAlpha(1);
    }

    /** « Motif de 5 cases · record 7 · erreurs 1 / 2 » et la phase : au-dessus du plateau. */
    public String mirrorStatus() {
        String phase = switch (game.mirrorPhase()) {
            case SHOW -> "Regardez : le motif s'allume";
            case INPUT -> "À vous : " + game.mirrorPosition() + " / " + game.mirrorLength();
            case PAUSE -> "Motif suivant…";
        };
        return phase + " · motif de " + game.mirrorLength() + " cases · record " + game.mirrorBest()
                + (game.mirrorAllowedMistakes() > 0 ? " · erreurs " + game.mirrorMistakes() + " / " + game.mirrorAllowedMistakes() : "");
    }
}
