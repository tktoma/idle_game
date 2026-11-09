package idle.ui.pages;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.TextAlignment;

/**
 * Un avatar dans son cadre (version 0.147) : l'un de ceux que dessine le jeu ({@link Avatar}), ou l'image importée par
 * le joueur, recadrée au carré du milieu puis à la forme du cadre. Le cadre est le skin choisi pour
 * {@link SkinVisual#AVATAR_FRAME} (version 0.189) : le Classique est le rond d'origine ; les six autres (rond cerné,
 * hexagone, écusson, cadre doré baroque, timbre-poste, polaroid) réduisent l'image pour se loger dans le même carré.
 * La vue ne se redessine que lorsqu'on lui donne autre chose à montrer, ou un autre cadre.
 */
public final class AvatarView extends Canvas {

    private static final String BACKGROUND = "#0e1420";
    /** Le bord d'une image importée, là où un avatar du jeu prend sa propre couleur. */
    private static final String IMAGE_EDGE = "#d6dde6";
    private static final String GOLD = "#c9a24a", GOLD_LIGHT = "#fff2c8", GOLD_DARK = "#6e5218";
    private static final String SILVER = "#c8ccd4", SILVER_DARK = "#3b414c";
    private static final String PAPER = "#f4f1e8", INK = "#3b4a7a";
    private static final String POLAROID = "#fafafa", PENCIL = "#3a3a44";
    /** L'inclinaison du polaroid, en degrés. */
    private static final double TILT = -4;

    /** L'or du cadre baroque, éclairé en haut à gauche ; proportionnel, il suit la forme remplie. */
    private static final RadialGradient GOLD_SHINE = new RadialGradient(0, 0, 0.4, 0.35, 0.75, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(GOLD_LIGHT)), new Stop(0.45, Color.web(GOLD)), new Stop(1, Color.web(GOLD_DARK)));
    /** L'argent de l'écusson, du reflet au métal sombre. */
    private static final LinearGradient SILVER_SHINE = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#eef1f6")), new Stop(0.5, Color.web(SILVER)), new Stop(1, Color.web("#8e949e")));

    private Avatar avatar = Avatar.PARTICLE;
    private Image image;
    /** Le cadre ; {@code null} pour le Classique. */
    private Skin frame;
    /** La « valeur » du timbre-poste : le nombre de titres obtenus. */
    private int stampValue;

    public AvatarView(double size) {
        super(size, size);
        setMouseTransparent(true);
        draw();
    }

    /** Met ce cadre autour de l'avatar ; {@code null}, ou un skin d'un autre visuel, remet le Classique. */
    public void setFrame(Skin skin) {
        Skin wanted = skin != null && skin.visual() == SkinVisual.AVATAR_FRAME ? skin : null;
        if (wanted == frame) return;
        frame = wanted;
        draw();
    }

    /** Le cadre en place, ou {@code null} pour le Classique. */
    public Skin frame() {
        return frame;
    }

    /** La valeur écrite en bas à droite du timbre-poste : le nombre de titres obtenus. */
    public void setStampValue(int value) {
        if (value == stampValue) return;
        stampValue = value;
        if (frame == Skin.FRAME_STAMP) draw();
    }

    /** Montre un avatar du jeu. */
    public void show(Avatar shown) {
        Avatar wanted = shown == null ? Avatar.PARTICLE : shown;
        if (image == null && wanted == avatar) return;
        avatar = wanted;
        image = null;
        draw();
    }

    /** Montre une image ; {@code null} ou une image illisible ramène l'avatar par défaut. */
    public void show(Image shown) {
        if (shown == null || shown.isError() || shown.getWidth() <= 0 || shown.getHeight() <= 0) {
            show(Avatar.PARTICLE);
            return;
        }
        if (shown == image) return;
        image = shown;
        draw();
    }

    /** Ce qui est montré : l'identifiant de l'avatar, ou {@link Avatar#CUSTOM} pour une image. */
    public String shown() {
        return image != null ? Avatar.CUSTOM : avatar.id();
    }

    private void draw() {
        double s = getWidth();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, s, s);
        if (frame == null) {
            drawClassic(g, s);
            return;
        }
        switch (frame) {
            case FRAME_ROUND -> drawRound(g, s);
            case FRAME_HEXAGON -> drawHexagon(g, s);
            case FRAME_SHIELD -> drawShield(g, s);
            case FRAME_BAROQUE -> drawBaroque(g, s);
            case FRAME_STAMP -> drawStamp(g, s);
            case FRAME_POLAROID -> drawPolaroid(g, s);
            default -> drawClassic(g, s);
        }
    }

    /** Le Classique : le rond d'origine, cerné d'un trait de la couleur de l'avatar. */
    private void drawClassic(GraphicsContext g, double s) {
        g.save();
        g.beginPath();
        circle(g, s / 2, s / 2, s / 2 - 1);
        g.clip();
        picture(g, 0, 0, s);
        g.restore();
        g.setGlobalAlpha(1);
        g.setStroke(edge());
        g.setLineWidth(Math.max(1.5, s / 40));
        g.strokeOval(1, 1, s - 2, s - 2);
    }

    /** « Rond » : le Classique cerné d'un anneau net de 3 px à la couleur du profil, posé sur un anneau plus sombre. */
    private void drawRound(GraphicsContext g, double s) {
        g.save();
        g.beginPath();
        circle(g, s / 2, s / 2, s / 2 - 1);
        g.clip();
        picture(g, 0, 0, s);
        g.restore();
        g.setGlobalAlpha(1);
        g.setStroke(Color.web("#4a3b2a"));
        g.setLineWidth(5);
        g.strokeOval(2.5, 2.5, s - 5, s - 5);
        g.setStroke(Color.web(ProfilePage.COLOR));
        g.setLineWidth(3);
        g.strokeOval(2.5, 2.5, s - 5, s - 5);
    }

    /** « Hexagone » : la découpe à six côtés, pointe en haut, au bord biseauté : clair sur les côtés hauts, sombre en bas. */
    private void drawHexagon(GraphicsContext g, double s) {
        double c = s / 2, r = c - 2;
        double[] xs = new double[6], ys = new double[6];
        for (int k = 0; k < 6; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 3;
            xs[k] = c + r * Math.cos(a);
            ys[k] = c + r * Math.sin(a);
        }
        g.save();
        g.beginPath();
        g.moveTo(xs[0], ys[0]);
        for (int k = 1; k < 6; k++) g.lineTo(xs[k], ys[k]);
        g.closePath();
        g.clip();
        picture(g, 0, 0, s);
        // Le biseau, dans la découpe : chaque côté n'en montre que la moitié intérieure.
        Color tone = edge();
        g.setLineWidth(Math.max(4, s / 12));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int k = 0; k < 6; k++) {
            g.setStroke(k >= 1 && k <= 3 ? tone.darker().darker() : tone.brighter());
            g.strokeLine(xs[k], ys[k], xs[(k + 1) % 6], ys[(k + 1) % 6]);
        }
        g.restore();
    }

    /** « Écusson » : haut droit, flancs qui descendent puis se referment en pointe ; bordure argentée sur un filet sombre. */
    private void drawShield(GraphicsContext g, double s) {
        double left = 2, right = s - 2, top = 2 + s * 0.04, bottom = s - 2;
        g.save();
        g.beginPath();
        g.moveTo(left, top);
        g.lineTo(right, top);
        g.lineTo(right, s * 0.45);
        g.quadraticCurveTo(right, s * 0.80, s / 2, bottom);
        g.quadraticCurveTo(left, s * 0.80, left, s * 0.45);
        g.closePath();
        g.clip();
        picture(g, 0, 0, s);
        // Le même contour tracé deux fois dans la découpe : le filet sombre déborde sous l'argent, vers l'intérieur.
        g.beginPath();
        g.moveTo(left, top);
        g.lineTo(right, top);
        g.lineTo(right, s * 0.45);
        g.quadraticCurveTo(right, s * 0.80, s / 2, bottom);
        g.quadraticCurveTo(left, s * 0.80, left, s * 0.45);
        g.closePath();
        g.setStroke(Color.web(SILVER_DARK));
        g.setLineWidth(s / 9);
        g.stroke();
        g.setStroke(SILVER_SHINE);
        g.setLineWidth(s / 14);
        g.stroke();
        g.restore();
    }

    /** « Cadre doré baroque » : l'image en rond dans un anneau d'or épais à quatre volutes, avec son ombre et ses rehauts. */
    private void drawBaroque(GraphicsContext g, double s) {
        double c = s / 2, outer = s * 0.455, inner = outer - s * 0.12;
        g.setGlobalAlpha(0.45);
        g.setFill(Color.BLACK);
        g.fillOval(c - outer + 2, c - outer + 3, 2 * outer, 2 * outer);
        g.setGlobalAlpha(1);
        g.setFill(GOLD_SHINE);
        g.fillOval(c - outer, c - outer, 2 * outer, 2 * outer);
        g.save();
        g.beginPath();
        circle(g, c, c, inner);
        g.clip();
        picture(g, c - inner, c - inner, 2 * inner);
        g.restore();
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.ROUND);
        // Le filet sombre contre l'image, et le rehaut qui court sur le haut de l'anneau.
        g.setStroke(Color.web(GOLD_DARK));
        g.setLineWidth(Math.max(1, s / 80));
        g.strokeOval(c - inner, c - inner, 2 * inner, 2 * inner);
        g.setStroke(Color.web(GOLD_LIGHT));
        g.setLineWidth(Math.max(1, s / 60));
        double mid = (outer + inner) / 2;
        g.strokeArc(c - mid, c - mid, 2 * mid, 2 * mid, 100, 70, ArcType.OPEN);
        // Quatre volutes aux points cardinaux : deux boucles qui s'enroulent de part et d'autre de chaque point.
        double loop = s * 0.055;
        g.setLineWidth(Math.max(1.2, s / 45));
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2;
            double px = c + mid * Math.cos(a), py = c + mid * Math.sin(a);
            double tx = -Math.sin(a) * loop * 1.3, ty = Math.cos(a) * loop * 1.3;
            double start = Math.toDegrees(-a);
            g.setStroke(Color.web(GOLD_DARK));
            g.strokeArc(px + tx - loop, py + ty - loop, 2 * loop, 2 * loop, start + 60, 270, ArcType.OPEN);
            g.strokeArc(px - tx - loop, py - ty - loop, 2 * loop, 2 * loop, start + 30, -270, ArcType.OPEN);
            g.setStroke(Color.web(GOLD_LIGHT));
            g.strokeArc(px + tx - loop, py + ty - loop, 2 * loop, 2 * loop, start + 120, 80, ArcType.OPEN);
            g.strokeArc(px - tx - loop, py - ty - loop, 2 * loop, 2 * loop, start + 160, 80, ArcType.OPEN);
        }
    }

    /** « Timbre-poste » : un carré crème aux bords dentelés, l'image au carré dans sa marge, la valeur en bas à droite, un cachet. */
    private void drawStamp(GraphicsContext g, double s) {
        double m = 2, side = s - 2 * m;
        int teeth = Math.max(6, (int) Math.round(side / (s * 0.09)));
        double pitch = side / teeth, bite = pitch * 0.3;
        g.save();
        g.beginPath();
        g.rect(m, m, side, side);
        g.clip();
        // Les perforations mordent le bord : le carré moins les ronds, par la règle pair-impair.
        g.setFillRule(FillRule.EVEN_ODD);
        g.beginPath();
        g.rect(m, m, side, side);
        for (int k = 0; k <= teeth; k++) {
            double at = m + k * pitch;
            circle(g, at, m, bite);
            circle(g, at, m + side, bite);
            circle(g, m, at, bite);
            circle(g, m + side, at, bite);
        }
        g.clip();
        g.setFill(Color.web(PAPER));
        g.fillRect(0, 0, s, s);
        double inner = s * 0.74, x = (s - inner) / 2;
        g.save();
        g.beginPath();
        g.rect(x, x, inner, inner);
        g.clip();
        picture(g, x, x, inner);
        g.restore();
        g.setGlobalAlpha(1);
        g.setStroke(Color.web(INK));
        g.setLineWidth(Math.max(1, s / 90));
        g.strokeRect(x, x, inner, inner);
        g.setFill(Color.web(INK));
        g.setFont(Font.font("serif", s * 0.085));
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText(Integer.toString(stampValue), s - x * 0.55, s - x * 0.2);
        // Le cachet, en encre : un rond dont on ne voit qu'une partie, à cheval sur le coin en haut à droite de l'image.
        g.setGlobalAlpha(0.55);
        g.setLineWidth(Math.max(1, s / 55));
        double stamp = s * 0.17, sx = s * 0.78, sy = s * 0.3;
        g.strokeArc(sx - stamp, sy - stamp, 2 * stamp, 2 * stamp, 30, 230, ArcType.OPEN);
        g.strokeArc(sx - stamp * 0.8, sy - stamp * 0.8, 1.6 * stamp, 1.6 * stamp, 50, 180, ArcType.OPEN);
        g.setGlobalAlpha(1);
        g.restore();
    }

    /** « Polaroid » : un carré blanc penché, l'image carrée en haut, la marge basse large où s'écrit le nom de l'avatar. */
    private void drawPolaroid(GraphicsContext g, double s) {
        double c = s / 2, width = s * 0.76, margin = s * 0.04, inner = width - 2 * margin, height = (width - margin) / 0.78;
        double left = c - width / 2, top = c - height / 2;
        g.save();
        g.translate(c, c);
        g.rotate(TILT);
        g.translate(-c, -c);
        g.setGlobalAlpha(0.4);
        g.setFill(Color.BLACK);
        g.fillRect(left + 2, top + 3, width, height);
        g.setGlobalAlpha(1);
        g.setFill(Color.web(POLAROID));
        g.fillRect(left, top, width, height);
        g.save();
        g.beginPath();
        g.rect(left + margin, top + margin, inner, inner);
        g.clip();
        picture(g, left + margin, top + margin, inner);
        g.restore();
        g.setGlobalAlpha(1);
        if (image == null) {
            g.setFill(Color.web(PENCIL));
            g.setFont(Font.font("serif", FontPosture.ITALIC, s * 0.085));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(avatar.label(), c, top + margin + inner + (height - margin - inner) / 2);
        }
        g.restore();
    }

    /** Le fond, puis l'image recadrée ou l'avatar du jeu, dans le carré de côté {@code side} posé en ({@code x}, {@code y}). */
    private void picture(GraphicsContext g, double x, double y, double side) {
        g.setFill(Color.web(BACKGROUND));
        g.fillRect(x, y, side, side);
        if (image != null) {
            double cut = Math.min(image.getWidth(), image.getHeight());
            g.drawImage(image, (image.getWidth() - cut) / 2, (image.getHeight() - cut) / 2, cut, cut, x, y, side, side);
        } else {
            g.save();
            g.translate(x, y);
            paint(g, avatar, side);
            g.restore();
        }
    }

    /** La couleur du bord : celle de l'avatar du jeu, ou un gris clair pour une image. */
    private Color edge() {
        return Color.web(image != null ? IMAGE_EDGE : avatar.color());
    }

    /** Ajoute un cercle fermé au chemin en cours. */
    private static void circle(GraphicsContext g, double x, double y, double r) {
        g.arc(x, y, r, r, 0, 360);
        g.closePath();
    }

    /** Dessine un avatar du jeu sur un carré de côté {@code s}, déjà découpé en rond. */
    static void paint(GraphicsContext g, Avatar avatar, double s) {
        Color color = Color.web(avatar.color());
        double c = s / 2;
        g.setLineCap(StrokeLineCap.ROUND);
        // Un halo de la couleur de l'avatar, au fond.
        for (int k = 0; k < 4; k++) {
            g.setGlobalAlpha(0.06);
            g.setFill(color);
            double r = s * (0.48 - 0.08 * k);
            g.fillOval(c - r, c - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);
        g.setFill(color);
        g.setStroke(color);
        switch (avatar) {
            case PARTICLE -> {
                g.setLineWidth(s / 60);
                g.setGlobalAlpha(0.35);
                g.strokeOval(c - s * 0.3, c - s * 0.3, s * 0.6, s * 0.6);
                g.strokeOval(c - s * 0.18, c - s * 0.18, s * 0.36, s * 0.36);
                g.setGlobalAlpha(1);
                dot(g, c, c, s * 0.09);
                dot(g, c + s * 0.3, c, s * 0.04);
                dot(g, c - s * 0.13, c - s * 0.11, s * 0.035);
                dot(g, c - s * 0.21, c + s * 0.21, s * 0.04);
            }
            case ATOM -> {
                g.setLineWidth(s / 45);
                for (int k = 0; k < 3; k++) {
                    g.save();
                    g.translate(c, c);
                    g.rotate(k * 60);
                    g.strokeOval(-s * 0.36, -s * 0.13, s * 0.72, s * 0.26);
                    g.restore();
                }
                g.setFill(Color.web("#ff8a75"));
                dot(g, c - s * 0.035, c - s * 0.02, s * 0.05);
                dot(g, c + s * 0.03, c + s * 0.03, s * 0.05);
                g.setFill(Color.web("#9fd0ff"));
                dot(g, c + s * 0.035, c - s * 0.035, s * 0.05);
                dot(g, c - s * 0.03, c + s * 0.035, s * 0.05);
            }
            case MOLECULE -> {
                g.setLineWidth(s / 22);
                g.setStroke(Color.web("#8fa3b8"));
                g.strokeLine(c, c + s * 0.04, c - s * 0.24, c + s * 0.2);
                g.strokeLine(c, c + s * 0.04, c + s * 0.24, c + s * 0.2);
                g.setFill(Color.web("#ff8a75"));
                dot(g, c, c + s * 0.02, s * 0.16);
                g.setFill(color);
                dot(g, c - s * 0.25, c + s * 0.21, s * 0.1);
                dot(g, c + s * 0.25, c + s * 0.21, s * 0.1);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.5);
                dot(g, c - s * 0.05, c - s * 0.04, s * 0.04);
            }
            case DARK_MATTER -> {
                g.setLineWidth(s / 36);
                for (int arm = 0; arm < 2; arm++) {
                    double previousX = c, previousY = c;
                    for (int k = 1; k <= 24; k++) {
                        double t = k / 24.0;
                        double a = arm * Math.PI + t * 3.6;
                        double r = t * s * 0.42;
                        double x = c + r * Math.cos(a), y = c + r * Math.sin(a);
                        g.setGlobalAlpha(1 - 0.7 * t);
                        g.strokeLine(previousX, previousY, x, y);
                        previousX = x;
                        previousY = y;
                    }
                }
                g.setGlobalAlpha(1);
                g.setFill(Color.web("#1a0f2e"));
                dot(g, c, c, s * 0.09);
                g.setFill(color);
                dot(g, c, c, s * 0.04);
            }
            case QUARK -> {
                double[][] at = {{c, c - s * 0.2}, {c - s * 0.19, c + s * 0.13}, {c + s * 0.19, c + s * 0.13}};
                g.setLineWidth(s / 50);
                g.setGlobalAlpha(0.6);
                for (int k = 0; k < 3; k++) {
                    double[] from = at[k], to = at[(k + 1) % 3];
                    wave(g, from[0], from[1], to[0], to[1], s * 0.03);
                }
                g.setGlobalAlpha(1);
                String[] colors = {"#ff6b6b", "#6bdc8b", "#6ba8ff"};
                for (int k = 0; k < 3; k++) {
                    g.setFill(Color.web(colors[k]));
                    dot(g, at[k][0], at[k][1], s * 0.09);
                }
            }
            case COMET -> {
                double hx = c + s * 0.17, hy = c - s * 0.14;
                for (int k = 0; k < 6; k++) {
                    double t = k / 6.0;
                    g.setGlobalAlpha(0.5 * (1 - t));
                    double w = s * (0.1 - 0.012 * k);
                    double x = hx - s * 0.08 * (k + 1), y = hy + s * 0.07 * (k + 1);
                    g.fillOval(x - w, y - w, 2 * w, 2 * w);
                }
                g.setGlobalAlpha(1);
                dot(g, hx, hy, s * 0.1);
                g.setFill(Color.WHITE);
                dot(g, hx + s * 0.02, hy - s * 0.02, s * 0.04);
            }
            case STAR -> {
                double[] xs = new double[16], ys = new double[16];
                for (int k = 0; k < 16; k++) {
                    double a = -Math.PI / 2 + k * Math.PI / 8;
                    double r = k % 2 == 0 ? s * 0.4 : s * 0.22;
                    xs[k] = c + r * Math.cos(a);
                    ys[k] = c + r * Math.sin(a);
                }
                g.setGlobalAlpha(0.55);
                g.fillPolygon(xs, ys, 16);
                g.setGlobalAlpha(1);
                dot(g, c, c, s * 0.2);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.6);
                dot(g, c - s * 0.05, c - s * 0.05, s * 0.08);
            }
            case PLANET -> {
                g.setLineWidth(s / 30);
                g.setStroke(Color.web("#e6d27a"));
                g.save();
                g.translate(c, c);
                g.rotate(-18);
                g.strokeArc(-s * 0.42, -s * 0.11, s * 0.84, s * 0.22, 180, 180, ArcType.OPEN);
                g.restore();
                dot(g, c, c, s * 0.24);
                g.setFill(Color.web("#4f9a7a"));
                g.setGlobalAlpha(0.8);
                g.fillOval(c - s * 0.2, c - s * 0.08, s * 0.4, s * 0.06);
                g.fillOval(c - s * 0.22, c + s * 0.05, s * 0.44, s * 0.05);
                g.setGlobalAlpha(1);
                g.save();
                g.translate(c, c);
                g.rotate(-18);
                g.strokeArc(-s * 0.42, -s * 0.11, s * 0.84, s * 0.22, 0, 180, ArcType.OPEN);
                g.restore();
            }
            case BLACK_HOLE -> {
                g.setLineWidth(s / 16);
                g.setGlobalAlpha(0.8);
                g.strokeOval(c - s * 0.38, c - s * 0.13, s * 0.76, s * 0.26);
                g.setGlobalAlpha(1);
                g.setFill(Color.BLACK);
                dot(g, c, c, s * 0.17);
                g.setLineWidth(s / 60);
                g.setStroke(Color.web("#ffd8a8"));
                g.strokeOval(c - s * 0.18, c - s * 0.18, s * 0.36, s * 0.36);
            }
            case GALAXY -> {
                for (int arm = 0; arm < 3; arm++) {
                    for (int p = 1; p <= 12; p++) {
                        double t = p / 12.0;
                        double a = arm * 2 * Math.PI / 3 + t * 3.4;
                        double r = s * 0.42 * t;
                        double dot = s * (0.05 - 0.03 * t);
                        g.setGlobalAlpha(1 - 0.5 * t);
                        g.fillOval(c + r * Math.cos(a) - dot / 2, c + r * Math.sin(a) * 0.8 - dot / 2, dot, dot);
                    }
                }
                g.setGlobalAlpha(1);
                g.setFill(Color.WHITE);
                dot(g, c, c, s * 0.06);
            }
            case SEED -> {
                g.setLineWidth(s / 30);
                g.setStroke(Color.web("#9be7a8"));
                g.beginPath();
                g.moveTo(c, c - s * 0.02);
                g.bezierCurveTo(c - s * 0.02, c - s * 0.16, c + s * 0.06, c - s * 0.24, c + s * 0.02, c - s * 0.33);
                g.stroke();
                g.setFill(Color.web("#9be7a8"));
                g.fillOval(c + s * 0.02, c - s * 0.34, s * 0.16, s * 0.08);
                g.fillOval(c - s * 0.15, c - s * 0.25, s * 0.15, s * 0.07);
                g.setFill(color);
                g.fillOval(c - s * 0.17, c - s * 0.04, s * 0.34, s * 0.3);
                g.setFill(Color.web("#8a7a3a"));
                g.setGlobalAlpha(0.6);
                g.fillOval(c - s * 0.08, c + s * 0.04, s * 0.08, s * 0.12);
                g.setGlobalAlpha(1);
            }
            case WORLD_TREE -> {
                g.setStroke(Color.web("#b08a5a"));
                g.setLineWidth(s / 14);
                g.strokeLine(c, c + s * 0.3, c, c - s * 0.02);
                g.setLineWidth(s / 30);
                g.strokeLine(c, c + s * 0.06, c - s * 0.16, c - s * 0.1);
                g.strokeLine(c, c + s * 0.02, c + s * 0.17, c - s * 0.12);
                g.strokeLine(c, c + s * 0.3, c - s * 0.18, c + s * 0.38);
                g.strokeLine(c, c + s * 0.3, c + s * 0.18, c + s * 0.38);
                g.setFill(color);
                double[][] leaves = {{0, -0.2, 0.17}, {-0.17, -0.13, 0.12}, {0.18, -0.15, 0.12}, {-0.08, -0.3, 0.1}, {0.1, -0.3, 0.1}};
                for (double[] leaf : leaves) dot(g, c + s * leaf[0], c + s * leaf[1], s * leaf[2]);
                g.setFill(Color.web("#fff1c9"));
                for (double[] leaf : leaves) dot(g, c + s * leaf[0] + s * 0.03, c + s * leaf[1] - s * 0.02, s * 0.018);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Un rond plein de rayon {@code r} centré en ({@code x}, {@code y}). */
    private static void dot(GraphicsContext g, double x, double y, double r) {
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
    }

    /** Un trait ondulé d'un point à l'autre : le gluon qui lie deux quarks. */
    private static void wave(GraphicsContext g, double x1, double y1, double x2, double y2, double amplitude) {
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        double nx = -dy / length, ny = dx / length;
        double previousX = x1, previousY = y1;
        for (int k = 1; k <= 16; k++) {
            double t = k / 16.0;
            double offset = amplitude * Math.sin(t * Math.PI * 6);
            double x = x1 + dx * t + nx * offset, y = y1 + dy * t + ny * offset;
            g.strokeLine(previousX, previousY, x, y);
            previousX = x;
            previousY = y;
        }
    }
}
