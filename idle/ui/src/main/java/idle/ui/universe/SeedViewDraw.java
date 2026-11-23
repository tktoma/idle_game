package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.CanvasPane;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.*;

/** Une partie de {@link SeedView} (découpée en 0.293 pour rester sous 1 000 lignes) : le dessin de toute la vue, l'étagère et l'arrosoir. */
public abstract class SeedViewDraw extends SeedViewCan {
    public SeedViewDraw() {
        super();
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = CanvasPane.begin(this, BACK);
        if (g == null) return;
        double zoom = zoom();
        // Le passage secoue un instant la graine, quand elle craque.
        double shake = evolveAge >= 0 && evolveAge < 0.6 ? Math.sin(evolveAge * 70) * 3 * (1 - evolveAge / 0.6) : 0;
        // Plus la vue s'éloigne, plus la graine descend : l'arbre monte jusqu'en haut de la vue.
        double cx = w / 2 + shake, cy = h * (0.55 + 0.33 * Math.min(1, (1 - zoom) / 0.75));
        double size = Math.min(w, h) * 0.16 * zoom;
        boolean any = seeds >= 1;
        plantHalf = 0;
        seedX = cx;
        seedY = cy;
        seedSize = any ? size : 0;
        crownR = 0;

        // La poussière qui monte, lentement, et s'éteint en haut ; de plus en plus dense avec les paliers.
        int motes = MOTES + (stage > 4 ? 30 * (stage - 4) : 0);
        for (int k = 0; k < motes; k++) {
            double speed = 8 + 14 * UniverseView.noise(k * 7 + 1);
            double y = h - ((UniverseView.noise(k * 7 + 2) * h + time * speed) % h);
            double x = w * UniverseView.noise(k * 7 + 3) + Math.sin(time * 0.6 + k) * 6;
            double fade = y / h;
            g.setGlobalAlpha(Math.min(1, (0.15 + 0.45 * fade * UniverseView.noise(k * 7 + 4)) * (1 + SeedTree.prominence(stage))));
            g.setFill(MOTE);
            double r = 0.8 + 1.4 * UniverseView.noise(k * 7 + 5);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);

        if (!any) {
            // Pas encore de graine : un creux vide, à peine marqué.
            g.setStroke(SHELL);
            g.setGlobalAlpha(0.35);
            g.setLineWidth(1.5);
            Skin hollow = skins.get(SkinVisual.SEED);
            if (hollow != null) SeedSkinArt.seed(g, cx, cy, size, 0, hollow);
            else seedPath(g, cx, cy, size, 0, false);
            g.setGlobalAlpha(1);
            return;
        }

        // Les ondes de germination.
        g.setStroke(HALO);
        g.setLineWidth(1.2);
        for (int k = 0; k < WAVES; k++) {
            double phase = ((time / WAVE_SECONDS) + k / (double) WAVES) % 1;
            double r = size * (1 + 2.6 * phase);
            g.setGlobalAlpha(0.35 * (1 - phase));
            g.strokeOval(cx - r, cy - r * 0.9, 2 * r, 1.8 * r);
        }
        g.setGlobalAlpha(1);

        // Le halo, qui respire.
        double breath = 1 + 0.035 * Math.sin(time * 1.7);
        UniverseView.glow(g, cx, cy, size * 2.4 * breath, HALO, 0.30);
        UniverseView.glow(g, cx, cy, size * 1.2 * breath, SEAM, 0.25);

        // Les graines de plus, en orbite : derrière (en haut) plus petites et plus pâles, devant plus grandes.
        // La graine habillée : chaque amande, grande ou petite, entière ou brisée, prend la forme du skin.
        Skin seedSkin = skins.get(SkinVisual.SEED);
        int count = orbiting();
        for (int k = 0; k < count; k++) {
            double angle = 2 * Math.PI * k / Math.max(1, count) + time * (0.12 + 0.05 * (k % 3));
            double ring = size * (2.0 + 0.6 * UniverseView.noise(k * 11 + 1));
            double x = cx + Math.cos(angle) * ring * 1.3;
            double y = cy + Math.sin(angle) * ring * 0.45;
            double depth = 0.5 + 0.5 * Math.sin(angle);
            double s = size * (0.16 + 0.12 * UniverseView.noise(k * 11 + 2)) * (0.75 + 0.35 * depth);
            UniverseView.glow(g, x, y, s * 2.2, HALO, 0.25 + 0.2 * depth);
            g.setGlobalAlpha(0.55 + 0.45 * depth);
            g.setFill(FLESH);
            if (seedSkin != null) SeedSkinArt.seed(g, x, y, s, angle * 0.7, seedSkin);
            else seedPath(g, x, y, s, angle * 0.7, true);
        }
        g.setGlobalAlpha(1);

        // Le germe, une tige courbe qui sort de la pointe, et ses feuilles.
        double grow = shownSprout * (stage >= 4 ? 1.3 : 1) * (stage >= 3 ? 0.85 + 0.15 * growth() : 1);
        double tipX = cx + Math.sin(-0.35) * size * 1.0, tipY = cy - Math.cos(-0.35) * size * 1.0;
        double topX = tipX, topY = tipY;
        // Du bonsaï à l'arbre-monde : un arbre dessiné de la graine brisée.
        if (stage >= 5) {
            double[] top = SeedTree.draw(g, w, h, cx, cy - size * 0.2, stage, growth(), time, skin,
                    skins.get(SkinVisual.TREE_SEASON), skins.get(SkinVisual.TREE_FANTASY), skins.get(SkinVisual.TREE_GROUND));
            topX = top[0];
            topY = top[1];
            crownX = top[0];
            crownY = top[1];
            crownR = top[2];
            grow = 0;
        }
        // Plus épaisse à chaque palier : la tige craquelée puis brisée.
        double thick = stage >= 4 ? 2.6 : stage >= 3 ? 1.8 : 1;
        if (stage >= 3) thick = 1 + (thick - 1) * growth();
        if (grow > 0.005) {
            int n = 14;
            double[] xs = new double[n], ys = new double[n];
            for (int i = 0; i < n; i++) {
                double t = i / (double) (n - 1);
                double sway = Math.sin(time * 1.1) * 4 * t;
                xs[i] = tipX - size * 0.5 * grow * Math.sin(t * 1.4) + sway;
                ys[i] = tipY - size * 1.6 * grow * t;
            }
            // Brindille : la tige devient bois, plus épaisse, et deux rameaux en partent.
            g.setStroke(twig ? WOOD : SPROUT);
            g.setLineWidth(Math.max(1.5, size * (twig ? 0.09 : 0.05) * thick));
            g.setGlobalAlpha(0.95);
            g.strokePolyline(xs, ys, n);
            if (twig) {
                for (int b = 0; b < 2; b++) {
                    int at = b == 0 ? n / 2 : (3 * n) / 4;
                    double side = b == 0 ? -1 : 1, len = size * 0.45 * grow;
                    double bx = xs[at] + side * len, by = ys[at] - len * 0.6;
                    g.setLineWidth(Math.max(1, size * 0.04));
                    g.strokeLine(xs[at], ys[at], bx, by);
                    double leaf = size * 0.2;
                    g.setFill(SPROUT);
                    g.setGlobalAlpha(0.85);
                    g.fillPolygon(new double[] {bx, bx + side * leaf, bx + side * leaf * 1.3, bx + side * leaf * 0.5},
                            new double[] {by, by - leaf * 0.5, by - leaf * 0.05, by + leaf * 0.25}, 4);
                    g.setGlobalAlpha(0.95);
                }
            }
            topX = xs[n - 1];
            topY = ys[n - 1];
            int leaves = leaves();
            for (int k = 0; k < leaves; k++) {
                // La première au bout ; les autres le long de la tige, de chaque côté.
                int at = k == 0 ? n - 1 : Math.max(2, n - 1 - k * 2);
                double lx = xs[at], ly = ys[at], side = k % 2 == 0 ? 1 : -1;
                double leaf = size * (k == 0 ? 0.35 : 0.24) * grow;
                double[] fx = {lx, lx + side * leaf, lx + side * leaf * 1.4, lx + side * leaf * 0.6};
                double[] fy = {ly, ly - leaf * 0.5, ly - leaf * 0.1, ly + leaf * 0.25};
                g.setFill(SPROUT);
                g.setGlobalAlpha(0.8);
                g.fillPolygon(fx, fy, 4);
            }
            g.setGlobalAlpha(1);
        }

        // Où l'arrosoir verse : la plante, de son pied à son sommet.
        plantX = cx;
        plantTop = Math.min(topY, cy - size);
        plantBottom = cy + size * 0.5;
        plantHalf = Math.max(size * 1.6, (cy - plantTop) * 0.45);

        // Les gouttes : elles tombent du haut sur le germe, puis éclaboussent.
        for (Drop drop : drops) {
            double targetX = topX + drop.drift * size * 0.2;
            if (drop.age < DROP_SECONDS) {
                double t = drop.age / DROP_SECONDS;
                double y = -10 + (topY + 10) * t * t, r = Math.max(3, size * 0.06);
                g.setFill(WATER);
                g.setGlobalAlpha(0.9);
                double[] px = {targetX, targetX + r, targetX, targetX - r};
                double[] py = {y - r * 2.2, y, y + r, y};
                g.fillPolygon(px, py, 4);
                g.fillOval(targetX - r, y - r * 0.2, r * 2, r * 1.4);
            } else {
                double t = (drop.age - DROP_SECONDS) / 0.6;
                g.setFill(WATER);
                g.setGlobalAlpha(0.8 * (1 - t));
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI + Math.PI * k / 5;
                    double d = size * 0.35 * t;
                    double r = Math.max(1.5, size * 0.025);
                    g.fillOval(targetX + Math.cos(a) * d - r, topY + Math.sin(a) * d * 0.6 - r + size * 0.2 * t * t, 2 * r, 2 * r);
                }
                UniverseView.glow(g, targetX, topY, size * 0.4 * (1 - t) + 2, WATER, 0.5 * (1 - t));
            }
        }
        g.setGlobalAlpha(1);

        if (flash > 0) UniverseView.glow(g, cx, cy - size, size * (2 + 4 * (1 - flash)), SEAM, 0.8 * flash);

        // La graine elle-même : la coque, la chair, la couture. Brisée à partir du palier 4 : ses deux
        // moitiés s'écartent au pied de la plante.
        double s = size * breath;
        if (stage >= 4) {
            double apart = size * (0.35 + 0.25 * growth());
            for (int side = -1; side <= 1; side += 2) {
                double hx = cx + side * apart, tilt = -0.35 + side * 0.9;
                if (seedSkin != null) {
                    SeedSkinArt.seed(g, hx, cy + size * 0.15, s * 0.75, tilt, seedSkin);
                    continue;
                }
                g.setFill(SHELL);
                seedPath(g, hx, cy + size * 0.15, s * 0.75, tilt, true);
                g.setFill(FLESH);
                g.setGlobalAlpha(0.8);
                seedPath(g, hx + side * s * 0.08, cy + size * 0.12, s * 0.55, tilt, true);
                g.setGlobalAlpha(1);
            }
            if (flash > 0) UniverseView.glow(g, cx, cy - size, size * (2 + 4 * (1 - flash)), SEAM, 0.8 * flash);
            if (evolveAge >= 0) SeedTree.evolveBurst(g, w, h, cx, cy, stage, evolveAge);
            return;
        }
        if (seedSkin != null) SeedSkinArt.seed(g, cx, cy, s, -0.35, seedSkin);
        else {
            g.setFill(SHELL);
            seedPath(g, cx, cy, s * 1.06, -0.35, true);
            g.setFill(FLESH);
            seedPath(g, cx, cy, s, -0.35, true);
        }
        if (stage >= 3) {
            // Les fissures : elles partent de la pointe, et brillent un instant quand la graine craque.
            double bright = evolveAge >= 0 && evolveAge < 1.2 ? 1 - evolveAge / 1.2 : 0;
            g.setStroke(bright > 0 ? SEAM : CRACK);
            g.setLineWidth(Math.max(1.2, size * 0.035));
            for (int c = 0; c < 3; c++) {
                double[] xs = new double[5], ys = new double[5];
                double angle = -0.35 + (c - 1) * 0.5;
                for (int k = 0; k < 5; k++) {
                    double t = k / 4.0, jag = (k % 2 == 0 ? 1 : -1) * size * 0.08;
                    xs[k] = cx + Math.sin(angle) * s * (0.95 - 0.9 * t) + Math.cos(angle) * jag;
                    ys[k] = cy - Math.cos(angle) * s * (0.95 - 0.9 * t) + Math.sin(angle) * jag;
                }
                g.setGlobalAlpha(0.9);
                g.strokePolyline(xs, ys, 5);
            }
            g.setGlobalAlpha(1);
            if (bright > 0) UniverseView.glow(g, cx, cy - s * 0.4, s * 1.4, SEAM, 0.6 * bright);
        }
        if (seedSkin == null) {
            // La couture et le reflet de l'amande : le skin a les siens.
            g.setStroke(SEAM);
            g.setLineWidth(Math.max(1, size * 0.03));
            g.setGlobalAlpha(0.8);
            double ax = Math.sin(-0.35), ay = -Math.cos(-0.35);
            g.strokeLine(cx - ax * s * 0.75, cy - ay * s * 0.75, cx + ax * s * 0.85, cy + ay * s * 0.85);
            g.setGlobalAlpha(1);
            UniverseView.glow(g, cx - s * 0.25, cy - s * 0.15, s * 0.45, SEAM, 0.45);
        }
        if (evolveAge >= 0) SeedTree.evolveBurst(g, w, h, cx, cy, stage, evolveAge);
    }

    /**
     * L'étagère de bois et l'arrosoir, par-dessus le reste : avant le déblocage, l'étagère est vide et
     * l'arrosoir n'y est qu'une ombre, sous le bouton qui le débloque.
     */
    void drawShelfAndCan() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || seeds < 1) return;
        GraphicsContext g = getGraphicsContext2D();
        double s = canSize(), x = shelfX(), top = shelfTop(), plank = s * 2.4, thick = Math.max(5, s * 0.14);
        // Une petite étagère de bois : deux montants, un fond plus sombre, une planche du haut et celle où se pose
        // l'arrosoir, posée sur deux pieds. Un étage de plus pour le chrono, au-dessus de l'arrosoir.
        double level = s * 1.35, mid = top - level, high = top - level * (chronoShown ? 2 : 1);
        double left = x - plank / 2, right = x + plank / 2, post = Math.max(4, s * 0.1);
        drawHose(g, s);
        // L'étagère habillée : ToolsSkinArt la dessine à la même place, montants, planches et étage du chrono.
        Skin shelfSkin = skins.get(SkinVisual.SHELF);
        if (shelfSkin != null) ToolsSkinArt.shelf(g, left, right, top, mid, high, thick, post, chronoShown, shelfSkin);
        else {
            g.setGlobalAlpha(0.55);
            g.setFill(SHELF_EDGE);
            g.fillRect(left + post, high, plank - 2 * post, top - high);
            g.setGlobalAlpha(1);
            g.setFill(SHELF);
            g.fillRect(left, high - thick, post, top + thick * 2 - high);
            g.fillRect(right - post, high - thick, post, top + thick * 2 - high);
            for (double board : chronoShown ? new double[] {top, mid, high - thick} : new double[] {top, high - thick}) {
                g.setFill(SHELF);
                g.fillRoundRect(left - thick * 0.4, board, plank + thick * 0.8, thick, 3, 3);
                g.setFill(SHELF_EDGE);
                g.fillRect(left - thick * 0.4, board + thick * 0.65, plank + thick * 0.8, thick * 0.35);
            }
        }
        if (chronoShown) {
            Skin chronoSkin = skins.get(SkinVisual.CHRONO);
            if (chronoSkin != null) ToolsSkinArt.chrono(g, x, mid - level / 2, s * 0.45, chronoFill, chronoRunning, chronoHand, time, chronoSkin);
            else drawChrono(g, x, mid - level / 2, s * 0.45);
        }
        // La saison, au-dessus de l'étagère, près du chrono qu'elle remplit.
        String season = seasonText();
        if (!season.isEmpty()) {
            double textX = Math.min(w - plank * 0.6, x), textY = high - thick - 28;
            // L'emblème de la saison, au-dessus de sa ligne, quand un skin des saisons est choisi.
            Skin seasonSkin = skins.get(SkinVisual.SEASONS);
            if (seasonSkin != null) {
                SeedSkinArt.season(g, textX, textY - 30, 14, this.season, seasonLeft / Game.SEASON_SECONDS, seasonSkin);
            }
            g.setFont(Font.font(12));
            g.setTextAlign(TextAlignment.CENTER);
            g.setGlobalAlpha(0.95);
            g.setFill(SEASON_COLORS[this.season.ordinal()]);
            g.fillText(season, textX, textY);
            g.setGlobalAlpha(1);
        }
        // L'effet du rond de croissance, au-dessus de l'étagère : l'arrosage va plus vite.
        String boost = bloomText();
        if (!boost.isEmpty()) {
            g.setFont(Font.font(13));
            g.setTextAlign(TextAlignment.CENTER);
            g.setGlobalAlpha(0.85 + 0.15 * Math.sin(time * 4));
            g.setFill(BLOOM);
            g.fillText(boost, Math.min(w - plank * 0.6, x), high - thick - 10);
            g.setGlobalAlpha(1);
        }
        // Le fil du bois.
        if (shelfSkin == null) {
            g.setStroke(SHELF_EDGE);
            g.setGlobalAlpha(0.35);
            g.setLineWidth(1);
            g.strokeLine(left + plank * 0.05, top + thick * 0.35, x + plank * 0.1, top + thick * 0.3);
            g.strokeLine(x - plank * 0.1, high - thick * 0.6, right - plank * 0.05, high - thick * 0.65);
            g.setGlobalAlpha(1);
        }

        // L'eau qui coule du bec, puis éclabousse.
        for (Stream drop : stream) {
            double r = Math.max(2, s * 0.045);
            g.setFill(WATER);
            if (drop.splashed) {
                double t = drop.age / 0.35;
                g.setGlobalAlpha(0.7 * (1 - t));
                for (int k = 0; k < 3; k++) {
                    double a = Math.PI + Math.PI * (k + 0.5) / 3, d = s * 0.25 * t;
                    g.fillOval(drop.x + Math.cos(a) * d - r, drop.y + Math.sin(a) * d * 0.5 - r, 2 * r, 2 * r);
                }
            } else {
                g.setGlobalAlpha(0.9);
                g.fillOval(drop.x - r, drop.y - r * 1.4, 2 * r, 2.8 * r);
                UniverseView.glow(g, drop.x, drop.y, r * 2.5, WATER, 0.25);
            }
        }
        for (Stream drop : hoseSpray) {
            double r = Math.max(1.6, s * 0.035);
            g.setFill(WATER);
            if (drop.splashed) {
                g.setGlobalAlpha(0.6 * (1 - drop.age / 0.35));
                g.fillOval(drop.x - r * 1.5, drop.y - r * 0.5, r * 3, r);
            } else {
                g.setGlobalAlpha(0.85);
                g.fillOval(drop.x - r, drop.y - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(1);

        if (!canUnlocked) {
            // Une ombre sur l'étagère : ce que le bouton débloque.
            g.setGlobalAlpha(0.22);
            paintCan(g, homeX(), homeY(), s, 0, true);
            g.setGlobalAlpha(1);
            return;
        }
        paintCan(g, canX(), canY(), s, CAN_TILT_DEGREES * tilt, false);
    }
}
