package idle.ui.universe;

import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.*;

/**
 * La graine d'univers, le visuel de son sous-onglet : une graine en amande, dorée, qui respire au centre d'un halo, des
 * ondes qui partent d'elle, et une poussière qui monte. Les graines de plus tournent autour d'elle, plus petites,
 * jusqu'à {@link #MAX_ORBITING}.
 *
 * <p>La graine ne germe plus seule. Le bouton de la sous-page la fait germer une fois l'objectif de la dimension 1
 * atteint ; ensuite, chaque clic fait tomber une goutte d'eau sur le germe, qui grandit et prend des feuilles.
 *
 * <p>Plus de bouton, un arrosoir. Il attend sur une étagère de bois, en bas à droite ; un bouton posé sur l'étagère le
 * débloque. On le prend à la souris et on le glisse sur la graine : il se penche, l'eau coule du bec, et le jeu compte
 * un arrosage par seconde ({@code Game#setWatering}). Relâché, il revient tout seul sur l'étagère.
 *
 * <p>Avec la boucle, le rond de croissance. De temps en temps, un rond vert pousse quelque part dans la vue et bat
 * doucement, son anneau et ses secondes comptant ce qu'il reste à le cueillir ; un clic le cueille. Pendant son effet,
 * l'arrosage va plus vite, et une ligne au-dessus de l'étagère le dit : « Croissance +45 % : 23 s ».
 *
 * <p>Avec le clicker, la météo. En haut à gauche, le ciel de la graine : un soleil qui rayonne par beau temps, un
 * soleil voilé d'un nuage par temps variable, un nuage sombre qui pleut et s'éclaire d'un éclair par orage ; dessous,
 * son effet sur l'arrosage et le temps qu'il lui reste (« Beau temps +30 % : 32 s »). Par beau temps, une lumière
 * chaude tombe sur la vue ; par orage, la pluie la traverse.
 *
 * <p>Avec le miroir, les saisons. Au-dessus de l'étagère, près du chrono, la saison, son effet et le temps qu'il lui
 * reste, et ce que chaque arrosage verse au chrono une fois l'amélioration achetée (« Printemps ×1,5 : 92 s · +0.3 s au
 * chrono par arrosage »).
 */
public final class SeedView extends SeedViewDraw {
    public SeedView() {
        super();
    }

    /** L'arrosoir, Classique ou habillé du skin choisi, à la même place et penché de même. */
    void paintCan(GraphicsContext g, double x, double y, double s, double degrees, boolean ghost) {
        Skin canSkin = skins.get(SkinVisual.WATERING_CAN);
        if (canSkin != null) ToolsSkinArt.can(g, x, y, s, degrees, ghost, canSkin);
        else drawCan(g, x, y, s, degrees, ghost);
    }

    /** La lance du tuyau : près du pied de la plante, à droite, tournée vers elle. */
    double[] hoseNozzle() {
        double s = canSize();
        return new double[] {plantX - Math.max(plantHalf * 0.75, s * 1.6), plantBottom - s * 0.15};
    }

    /**
     * Les positions des bulles, avant qu'elles flottent : la base sur la graine, et la rangée la plus haute des bulles
     * nées en haut du feuillage. Le schéma garde ses proportions ; s'il est trop large pour la vue, il rétrécit. Les
     * racines, sous la graine, restent dans la vue.
     */
    void placeBubbles() {
        int n = bubbles.size();
        double baseX = seedX, baseY = seedY + seedSize * 0.05;
        double top = 0, wide = 0;
        for (Bubble bubble : bubbles) {
            double[] at = SCHEMA[Math.max(0, Math.min(SCHEMA.length - 1, bubble.place()))];
            top = Math.max(top, at[1]);
            wide = Math.max(wide, Math.abs(at[0]));
        }
        double k = 0, kx = 0;
        if (top > 0 && crownR > 0) {
            // Le bonsaï n'a que sa rangée du feuillage : elle se pose dans ses plateaux, pas au-dessus.
            boolean bonsai = top < SCHEMA[3][1];
            double crownTop = bonsai ? crownY + crownR * 0.1 : crownY - crownR * 0.35;
            k = Math.max(0, baseY - crownTop) / top;
            if (wide > 0) k = Math.min(k, (getWidth() * 0.47 - bubbleR) / wide);
            k = Math.min(k, (baseY - bubbleR * 1.6) / top);
            kx = bonsai && wide > 0 ? Math.min(k, crownR * 0.75 / wide) : k;
        }
        for (int i = 0; i < n; i++) {
            double[] at = SCHEMA[Math.max(0, Math.min(SCHEMA.length - 1, bubbles.get(i).place()))];
            bubbleX[i] = baseX + at[0] * kx;
            bubbleY[i] = Math.min(getHeight() - bubbleR * 1.2, baseY - at[1] * k);
        }
        // Les deux bulles du bas à l'arbre-monde : la graine et les racines remontent un peu et
        // s'écartent, au lieu d'être serrées au bas de la vue.
        int seed = -1, roots = -1;
        for (int i = 0; i < n; i++) {
            if (bubbles.get(i).place() == 0) seed = i;
            if (bubbles.get(i).place() == ROOTS_PLACE) roots = i;
        }
        if (seed >= 0 && roots >= 0) {
            double gap = bubbleR * 3.6;
            double rootsY = Math.min(baseY + gap - bubbleR, getHeight() - bubbleR * 2.0);
            bubbleY[roots] = rootsY;
            bubbleY[seed] = rootsY - gap;
        }
    }

    /** Le rayon des bulles au dernier dessin : pour les vérifications. */
    public double bubbleRadius() { return bubbleR; }

    /**
     * Les bulles de l'arbre et leurs liens : chaque bulle reliée aux bulles d'en dessous qui l'ouvrent.
     * Elles flottent un peu. Achetable, une bulle pulse ; achetée, elle reste allumée ; sinon elle n'est qu'un reflet,
     * en pointillé tant qu'aucune bulle reliée n'est achetée.
     */
    void drawBubbles() {
        int n = bubbles.size();
        if (bubbleX.length != n) {
            bubbleX = new double[n];
            bubbleY = new double[n];
        }
        if (n == 0 || seedSize <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        // Une taille tenue par la vue, pas par la graine (qui rapetisse quand l'arbre grandit) : lisible à tous les paliers.
        bubbleR = Math.max(13, Math.min(getWidth(), getHeight()) * 0.036);
        placeBubbles();
        for (int i = 0; i < n; i++) bubbleY[i] += Math.sin(time * 1.3 + i * 2.1) * bubbleR * 0.12;
        // Les liens d'abord, derrière : de bord à bord, allumés quand les deux bulles sont achetées.
        Skin linkSkin = skins.get(SkinVisual.TREE_BUBBLE_LINKS), bubbleSkin = skins.get(SkinVisual.TREE_BUBBLES);
        for (int i = 0; i < n; i++) {
            for (int j : bubbles.get(i).below()) {
                if (j < 0 || j >= n) continue;
                double dx = bubbleX[i] - bubbleX[j], dy = bubbleY[i] - bubbleY[j], len = Math.hypot(dx, dy);
                if (len <= 2 * bubbleR) continue;
                double ux = dx / len, uy = dy / len;
                boolean lit = bubbles.get(i).state() == BubbleState.BOUGHT && bubbles.get(j).state() == BubbleState.BOUGHT;
                // Un lien vaut dans les deux sens : plein dès qu'un de ses bouts est acheté.
                boolean open = bubbles.get(j).state() == BubbleState.BOUGHT || bubbles.get(i).state() == BubbleState.BOUGHT;
                if (TreeBubbleSkinArt.links(linkSkin)) {
                    // Les liens en branches, fils de lumière ou racines.
                    TreeBubbleSkinArt.link(g, bubbleX[j] + ux * bubbleR, bubbleY[j] + uy * bubbleR, bubbleX[i] - ux * bubbleR, bubbleY[i] - uy * bubbleR,
                            bubbleR, lit, open, time, i * 7 + j, linkSkin);
                    continue;
                }
                g.setStroke(BUBBLE);
                g.setLineWidth(lit ? 2.4 : 1.5);
                g.setGlobalAlpha(lit ? 0.9 : open ? 0.55 : 0.25);
                if (!open) g.setLineDashes(5, 5);
                g.strokeLine(bubbleX[j] + ux * bubbleR, bubbleY[j] + uy * bubbleR, bubbleX[i] - ux * bubbleR, bubbleY[i] - uy * bubbleR);
                g.setLineDashes();
            }
        }
        for (int i = 0; i < n; i++) {
            Bubble bubble = bubbles.get(i);
            double x = bubbleX[i], y = bubbleY[i];
            double r = bubbleR * (i == hovered ? 1.08 : 1);
            double pulse = 0.5 + 0.5 * Math.sin(time * 3 + i);
            // Les racines : une aura dorée qui respire, achetées ou non.
            if (bubble.place() == ROOTS_PLACE) {
                double breath = 0.5 + 0.5 * Math.sin(time * 1.6);
                UniverseView.glow(g, x, y, r * (2.8 + 0.6 * breath), ROOTS_GOLD, 0.65 + 0.25 * breath);
                UniverseView.glow(g, x, y, r * 1.6, Color.web("#fff1b8"), 0.5);
                g.setStroke(ROOTS_GOLD);
                g.setLineWidth(2.2);
                g.setGlobalAlpha(0.55 + 0.35 * breath);
                g.strokeOval(x - r * 1.3, y - r * 1.3, r * 2.6, r * 2.6);
            }
            // Un fond sombre derrière la bulle : elle se détache du feuillage.
            g.setFill(Color.web("#06121a"));
            g.setGlobalAlpha(0.6);
            g.fillOval(x - r * 1.08, y - r * 1.08, r * 2.16, r * 2.16);
            if (TreeBubbleSkinArt.bubbles(bubbleSkin)) {
                // Les bulles en fruits, lanternes, gouttes, pierres ou graines.
                TreeBubbleSkinArt.bubble(g, x, y, r, bubble.state(), pulse, time, i, bubbleSkin);
                continue;
            }
            switch (bubble.state()) {
                case BOUGHT -> {
                    UniverseView.glow(g, x, y, r * 2.4, BUBBLE, 0.55);
                    g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.3, r * 1.3, false,
                            CycleMethod.NO_CYCLE, new Stop(0, Color.web("#e8fbff")),
                            new Stop(0.55, BUBBLE), new Stop(1, BUBBLE_DEEP)));
                    g.setGlobalAlpha(0.85);
                    g.fillOval(x - r, y - r, 2 * r, 2 * r);
                }
                case BUY -> {
                    UniverseView.glow(g, x, y, r * (1.8 + 0.6 * pulse), BUBBLE, 0.25 + 0.25 * pulse);
                    g.setFill(BUBBLE);
                    g.setGlobalAlpha(0.18);
                    g.fillOval(x - r, y - r, 2 * r, 2 * r);
                }
                default -> {
                    g.setFill(BUBBLE);
                    g.setGlobalAlpha(0.07);
                    g.fillOval(x - r, y - r, 2 * r, 2 * r);
                }
            }
            // Le bord, et le reflet en haut à gauche.
            g.setStroke(BUBBLE);
            g.setLineWidth(bubble.state() == BubbleState.BUY ? 2.2 : 1.4);
            g.setGlobalAlpha(switch (bubble.state()) {
                case BOUGHT -> 0.95;
                case BUY -> 0.6 + 0.4 * pulse;
                case SHORT -> 0.45;
                case CLOSED -> 0.22;
            });
            if (bubble.state() == BubbleState.CLOSED) g.setLineDashes(4, 4);
            g.strokeOval(x - r, y - r, 2 * r, 2 * r);
            g.setLineDashes();
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(bubble.state() == BubbleState.CLOSED ? 0.2 : 0.55);
            g.fillOval(x - r * 0.55, y - r * 0.62, r * 0.42, r * 0.26);
        }
        g.setGlobalAlpha(1);
    }

    /** L'étiquette de la bulle survolée : son nom et son prix, au-dessus d'elle, par-dessus le reste. */
    void drawBubbleLabel() {
        if (hovered < 0 || hovered >= bubbles.size() || hovered >= bubbleX.length) return;
        Bubble bubble = bubbles.get(hovered);
        GraphicsContext g = getGraphicsContext2D();
        g.save();
        Font name = Font.font(null, FontWeight.BOLD, 13);
        Font price = Font.font(12);
        Text measure = new Text(bubble.name());
        measure.setFont(name);
        double width = measure.getLayoutBounds().getWidth();
        // Ce qu'elle fait, entre le nom et le prix : sur deux lignes quand elle est longue.
        List<String> effect = effectLines(bubble.effect());
        measure.setFont(price);
        for (String line : effect) {
            measure.setText(line);
            width = Math.max(width, measure.getLayoutBounds().getWidth());
        }
        measure.setText(bubble.price());
        width = Math.min(getWidth() - 8, Math.max(width, measure.getLayoutBounds().getWidth()) + 20);
        double height = 42 + 17 * effect.size();
        double x = Math.max(4, Math.min(getWidth() - width - 4, bubbleX[hovered] - width / 2));
        double y = Math.max(4, bubbleY[hovered] - bubbleR - height - 8);
        g.setGlobalAlpha(0.92);
        g.setFill(Color.web("#0d1a24"));
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setStroke(BUBBLE);
        g.setLineWidth(1.2);
        g.strokeRoundRect(x, y, width, height, 10, 10);
        g.setGlobalAlpha(1);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.setFont(name);
        g.setFill(Color.web("#e8fbff"));
        g.fillText(bubble.name(), x + 10, y + 6);
        g.setFont(price);
        g.setFill(Color.web("#c9dcff"));
        for (int i = 0; i < effect.size(); i++) g.fillText(effect.get(i), x + 10, y + 23 + 17 * i);
        g.setFill(switch (bubble.state()) {
            case BUY, BOUGHT -> Color.web("#a8f0b0");
            case SHORT -> Color.web("#ffb48a");
            case CLOSED -> Color.web("#9aa3bd");
        });
        g.fillText(bubble.price(), x + 10, y + 23 + 17 * effect.size());
        g.restore();
    }

    void drawHose(GraphicsContext g, double s) {
        if (!hoseShown || plantHalf <= 0) return;
        double h = getHeight();
        double[] tip = hoseNozzle();
        double floor = h - s * 0.12;
        Skin hoseSkin = skins.get(SkinVisual.HOSE);
        if (hoseSkin != null) {
            ToolsSkinArt.hose(g, s, tip, floor, hoseSkin);
            return;
        }
        g.setLineCap(StrokeLineCap.ROUND);
        for (int pass = 0; pass < 2; pass++) {
            g.setStroke(pass == 0 ? HOSE : HOSE_LIGHT);
            g.setLineWidth(pass == 0 ? Math.max(4, s * 0.16) : Math.max(1.5, s * 0.05));
            g.setGlobalAlpha(pass == 0 ? 1 : 0.6);
            g.beginPath();
            g.moveTo(-s, floor);
            g.bezierCurveTo(s * 3, floor + s * 0.05, tip[0] - s * 2.5, floor, tip[0] - s * 0.55, tip[1] + s * 0.05);
            g.stroke();
        }
        g.setGlobalAlpha(1);
        // La lance, de laiton, tournée vers la plante.
        g.save();
        g.translate(tip[0], tip[1]);
        g.rotate(-20);
        g.setFill(BRASS);
        g.fillRoundRect(-s * 0.05, -s * 0.08, s * 0.55, s * 0.16, 4, 4);
        g.fillOval(-s * 0.1, -s * 0.11, s * 0.16, s * 0.22);
        g.restore();
    }

    /**
     * Le chrono de l'étage du haut : un cadran sombre que remplit le temps gardé (en bleu), une aiguille qui tourne
     * vite quand il accélère le jeu, lentement sinon, et un remontoir de laiton.
     */
    void drawChrono(GraphicsContext g, double x, double y, double r) {
        if (chronoRunning) UniverseView.glow(g, x, y, r * 2.2, CHRONO_TIME, 0.35);
        g.setFill(BRASS);
        g.fillRect(x - r * 0.12, y - r * 1.25, r * 0.24, r * 0.3);
        g.fillOval(x - r * 0.22, y - r * 1.4, r * 0.44, r * 0.22);
        g.setFill(CHRONO_FACE);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(CHRONO_TIME);
        g.setGlobalAlpha(0.8);
        g.fillArc(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7, 90, -360 * chronoFill, ArcType.ROUND);
        g.setGlobalAlpha(1);
        g.setStroke(BRASS);
        g.setLineWidth(Math.max(2, r * 0.14));
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.setLineWidth(Math.max(1, r * 0.05));
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            g.strokeLine(x + Math.cos(a) * r * 0.78, y + Math.sin(a) * r * 0.78, x + Math.cos(a) * r * 0.9, y + Math.sin(a) * r * 0.9);
        }
        double a = chronoHand - Math.PI / 2;
        g.setStroke(SEAM);
        g.setLineWidth(Math.max(1.5, r * 0.08));
        g.strokeLine(x, y, x + Math.cos(a) * r * 0.7, y + Math.sin(a) * r * 0.7);
        g.setFill(BRASS);
        g.fillOval(x - r * 0.1, y - r * 0.1, r * 0.2, r * 0.2);
    }
}
