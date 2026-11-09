package idle.ui;

import idle.core.universe.Skin;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Le fond de la fenêtre (version 0.190, visuel {@link idle.core.universe.SkinVisual#AMBIENT}) : un dessin sous tout le
 * jeu, dans la pile de la fenêtre, qui ne se voit que dans les marges et à travers un voile sombre sous les pages. Le skin
 * habille les bords, il ne touche pas à la lisibilité : textes et cartes restent sur leur fond presque noir.
 *
 * <p>Sans skin (« Classique »), rien n'est dessiné et la toile est cachée : le fond du jeu reste celui de toujours,
 * {@link #rootStyle}. Avec un skin, la racine du jeu reçoit une marge ({@link #rootPadding}) et un voile
 * {@code #0b0e14} à 78 % : Papier (grain léger sur crème), Ardoise (traits de craie le long des bords, quelques traces
 * effacées), Nuit étoilée (80 étoiles fixes, dont une vingtaine scintillent), Terminal (une bordure de caractères ASCII en
 * vert, un curseur qui clignote en bas à gauche), Blueprint (un quadrillage blanc, un cartouche de plan en bas à droite).
 *
 * <p>Le dessin se refait quand la fenêtre change de taille ({@link #fit}) ou de skin ({@link #setSkin}), pas à chaque
 * image : seuls la Nuit étoilée (le scintillement) et le Terminal (le curseur) se redessinent, deux fois par seconde au
 * plus ({@link #frame}). Rien n'est alloué par image : les étoiles sont tirées une fois pour toutes, les couleurs et les
 * polices sont des constantes.
 *
 * <p>Comme tout le reste de la fenêtre, le fond est écrit pour le thème sombre : il passe par le filtre des thèmes
 * ({@link ThemeFilter}), posé sur la pile entière. Le thème clair l'inverse donc, Papier devenant sombre et Nuit étoilée
 * claire ; c'est admis, comme pour chaque dessin du jeu.
 */
final class Chrome extends Canvas {

    /** Le fond de toujours, quand aucun skin n'est choisi : celui de la racine depuis la première version. */
    static final String CLASSIC_ROOT_STYLE = "-fx-background-color: #0b0e14;";
    /** Sous un skin : le voile sombre sous les pages, posé en retrait pour laisser le fond paraître dans les marges. */
    static final String SKINNED_ROOT_STYLE = "-fx-background-color: rgba(11, 14, 20, 0.78); -fx-background-radius: 6;";
    /** La marge laissée au fond tout autour du jeu, en pixels : assez pour une ligne de caractères du Terminal. */
    static final double MARGIN = 16;
    private static final Insets NO_PADDING = Insets.EMPTY;
    private static final Insets SKINNED_PADDING = new Insets(MARGIN);
    /** Intervalle entre deux redessins des fonds qui bougent (le scintillement, le curseur), en secondes. */
    static final double LIVE_INTERVAL = 0.5;

    private static final Color PAPER = Color.web("#e9e2d2");
    private static final Color PAPER_GRAIN = Color.web("#7a6a50", 0.10);
    private static final Color SLATE = Color.web("#1d2a24");
    private static final Color CHALK = Color.web("#f0ede4", 0.12);
    private static final Color CHALK_SMEAR = Color.web("#f0ede4", 0.05);
    private static final Color NIGHT = Color.web("#06080f");
    private static final Color STAR = Color.web("#fff6e0");
    private static final Color TERMINAL = Color.BLACK;
    private static final Color PHOSPHOR = Color.web("#6dff8f");
    private static final Color BLUEPRINT = Color.web("#1a3a6b");
    private static final Color GRID_FINE = Color.web("#ffffff", 0.10);
    private static final Color GRID_BOLD = Color.web("#ffffff", 0.22);
    private static final Color CARTOUCHE_INK = Color.web("#ffffff", 0.75);
    private static final Font MONO = Font.font("Monospaced", 14);
    private static final Font MONO_SMALL = Font.font("Monospaced", 11);
    private static final Font TITLE = Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 18);
    /** Le pas du quadrillage du Blueprint, et le nombre de pas entre deux lignes marquées. */
    private static final double GRID_STEP = 24;
    private static final int GRID_BOLD_EVERY = 5;
    private static final String VERSION_LINE = "Version " + Version.CURRENT;
    /** La graine des étoiles et du grain : la même à chaque lancement, le ciel ne change pas d'une session à l'autre. */
    private static final long SEED = 0x1D1E_5EEDL;
    private static final int STARS = 80;
    private static final int TWINKLING = 20;
    private static final int GRAINS = 1400;
    private static final int SMEARS = 5;

    private Skin skin = null;
    /** Les étoiles : position en fraction de la fenêtre, taille en pixels ; les premières {@link #TWINKLING} scintillent. */
    private final double[] starX = new double[STARS];
    private final double[] starY = new double[STARS];
    private final double[] starSize = new double[STARS];
    private final double[] starPhase = new double[STARS];
    /** Le grain du papier et les traces de l'ardoise, en fractions de la fenêtre. */
    private final double[] grainX = new double[GRAINS];
    private final double[] grainY = new double[GRAINS];
    private final double[] smearX = new double[SMEARS];
    private final double[] smearY = new double[SMEARS];
    private final double[] smearW = new double[SMEARS];
    /** La largeur d'un caractère de la police du Terminal, mesurée une fois. */
    private final double cell;
    /** Le temps des fonds qui bougent, et le temps écoulé depuis leur dernier redessin. */
    private double time = 0;
    private double sinceDraw = 0;

    Chrome() {
        setManaged(false);
        setMouseTransparent(true);
        setVisible(false);
        java.util.Random random = new java.util.Random(SEED);
        for (int i = 0; i < STARS; i++) {
            starX[i] = random.nextDouble();
            starY[i] = random.nextDouble();
            starSize[i] = 1 + random.nextDouble() * (i < TWINKLING ? 2.2 : 1.4);
            starPhase[i] = random.nextDouble() * Math.PI * 2;
        }
        for (int i = 0; i < GRAINS; i++) {
            grainX[i] = random.nextDouble();
            grainY[i] = random.nextDouble();
        }
        for (int i = 0; i < SMEARS; i++) {
            smearX[i] = 0.1 + random.nextDouble() * 0.8;
            smearY[i] = 0.1 + random.nextDouble() * 0.8;
            smearW[i] = 40 + random.nextDouble() * 90;
        }
        Text probe = new Text("-");
        probe.setFont(MONO);
        cell = Math.max(4, probe.getLayoutBounds().getWidth());
    }

    /** Le skin du fond, ou {@code null} pour le Classique. */
    Skin skin() {
        return skin;
    }

    /** Change le skin : la toile se cache sans skin, sinon elle se redessine aussitôt. */
    void setSkin(Skin skin) {
        if (this.skin == skin) return;
        this.skin = skin;
        setVisible(skin != null);
        draw();
    }

    /** Le style de la racine du jeu sous ce skin : celui de toujours sans skin, sinon le voile sombre. */
    static String rootStyle(Skin skin) {
        return skin == null ? CLASSIC_ROOT_STYLE : SKINNED_ROOT_STYLE;
    }

    /** La marge de la racine du jeu sous ce skin : aucune sans skin, sinon {@link #MARGIN} tout autour. */
    static Insets rootPadding(Skin skin) {
        return skin == null ? NO_PADDING : SKINNED_PADDING;
    }

    /** La toile prend la taille de la fenêtre, et se redessine si elle a changé. */
    void fit(double width, double height) {
        if (width <= 0 || height <= 0 || (width == getWidth() && height == getHeight())) return;
        setWidth(width);
        setHeight(height);
        draw();
    }

    /** Une image de plus : la Nuit étoilée scintille et le curseur du Terminal clignote, deux redessins par seconde au plus. */
    void frame(double dt) {
        if (skin != Skin.AMBIENT_NIGHT && skin != Skin.AMBIENT_TERMINAL) return;
        time += dt;
        sinceDraw += dt;
        if (sinceDraw < LIVE_INTERVAL) return;
        sinceDraw = 0;
        draw();
    }

    /** Dessine le fond entier pour le skin et la taille du moment ; rien sans skin ni sans taille. */
    private void draw() {
        double w = getWidth(), h = getHeight();
        if (skin == null || w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        switch (skin) {
            case AMBIENT_PAPER -> drawPaper(g, w, h);
            case AMBIENT_SLATE -> drawSlate(g, w, h);
            case AMBIENT_NIGHT -> drawNight(g, w, h);
            case AMBIENT_TERMINAL -> drawTerminal(g, w, h);
            case AMBIENT_BLUEPRINT -> drawBlueprint(g, w, h);
            default -> { }     // un skin d'un autre visuel ne peut pas arriver ici : {@code chosenSkin(AMBIENT)} ne rend que les siens
        }
    }

    /** Papier : un crème uni, et un grain de points très faibles. */
    private void drawPaper(GraphicsContext g, double w, double h) {
        g.setFill(PAPER);
        g.fillRect(0, 0, w, h);
        g.setFill(PAPER_GRAIN);
        for (int i = 0; i < GRAINS; i++) g.fillRect(grainX[i] * w, grainY[i] * h, 1.5, 1.5);
    }

    /** Ardoise : un vert sombre, des traits de craie le long des bords, et quelques traces effacées. */
    private void drawSlate(GraphicsContext g, double w, double h) {
        g.setFill(SLATE);
        g.fillRect(0, 0, w, h);
        g.setFill(CHALK_SMEAR);
        for (int i = 0; i < SMEARS; i++) g.fillOval(smearX[i] * w - smearW[i] / 2, smearY[i] * h - 9, smearW[i], 18);
        g.setStroke(CHALK);
        g.setLineWidth(2);
        double inset = MARGIN / 2;
        // Le trait de craie n'est pas droit : chaque bord est fait de petits segments légèrement décalés.
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            double length = horizontal ? w : h;
            double fixed = side == 0 ? inset : side == 1 ? h - inset : side == 2 ? inset : w - inset;
            for (double at = inset; at < length - inset; at += 18) {
                double wobble = ((at * 7 + side * 13) % 5) - 2;
                double end = Math.min(at + 14, length - inset);
                if (horizontal) g.strokeLine(at, fixed + wobble, end, fixed + wobble);
                else g.strokeLine(fixed + wobble, at, fixed + wobble, end);
            }
        }
    }

    /** Nuit étoilée : un bleu presque noir, 80 étoiles fixes dont les premières scintillent avec le temps. */
    private void drawNight(GraphicsContext g, double w, double h) {
        g.setFill(NIGHT);
        g.fillRect(0, 0, w, h);
        g.setFill(STAR);
        for (int i = 0; i < STARS; i++) {
            double size = starSize[i];
            if (i < TWINKLING) {
                double glow = 0.55 + 0.45 * Math.sin(time * 1.7 + starPhase[i]);
                g.setGlobalAlpha(glow);
            } else {
                g.setGlobalAlpha(0.75);
            }
            g.fillOval(starX[i] * w - size / 2, starY[i] * h - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
    }

    /** Terminal : noir, une bordure de caractères ASCII en vert phosphore, et un curseur qui clignote en bas à gauche. */
    private void drawTerminal(GraphicsContext g, double w, double h) {
        g.setFill(TERMINAL);
        g.fillRect(0, 0, w, h);
        g.setFill(PHOSPHOR);
        g.setFont(MONO);
        int columns = Math.max(2, (int) ((w - 4) / cell));
        int rows = Math.max(2, (int) ((h - 4) / MARGIN));
        double left = (w - columns * cell) / 2;
        double top = MARGIN - 3;
        double bottom = top + (rows - 1) * MARGIN;
        for (int c = 0; c < columns; c++) {
            String glyph = c == 0 || c == columns - 1 ? "+" : "-";
            g.fillText(glyph, left + c * cell, top);
            g.fillText(glyph, left + c * cell, bottom);
        }
        for (int r = 1; r < rows - 1; r++) {
            g.fillText("|", left, top + r * MARGIN);
            g.fillText("|", left + (columns - 1) * cell, top + r * MARGIN);
        }
        // Le curseur : un bloc plein une demi-seconde sur deux, dans le coin en bas à gauche, à l'intérieur du cadre.
        if (((int) (time / LIVE_INTERVAL)) % 2 == 0) g.fillRect(left + cell + 2, bottom - MARGIN - 1, cell, MARGIN - 4);
    }

    /** Blueprint : un bleu de plan, un quadrillage blanc léger marqué tous les cinq pas, et un cartouche en bas à droite. */
    private void drawBlueprint(GraphicsContext g, double w, double h) {
        g.setFill(BLUEPRINT);
        g.fillRect(0, 0, w, h);
        g.setLineWidth(1);
        int k = 0;
        for (double x = 0; x <= w; x += GRID_STEP, k++) {
            g.setStroke(k % GRID_BOLD_EVERY == 0 ? GRID_BOLD : GRID_FINE);
            g.strokeLine(x + 0.5, 0, x + 0.5, h);
        }
        k = 0;
        for (double y = 0; y <= h; y += GRID_STEP, k++) {
            g.setStroke(k % GRID_BOLD_EVERY == 0 ? GRID_BOLD : GRID_FINE);
            g.strokeLine(0, y + 0.5, w, y + 0.5);
        }
        // Le cartouche : un cadre double, le titre et la version, dans le coin en bas à droite.
        double cw = 150, ch = 52;
        double x = w - cw - MARGIN - 4, y = h - ch - MARGIN - 4;
        g.setFill(BLUEPRINT);
        g.fillRect(x, y, cw, ch);
        g.setStroke(CARTOUCHE_INK);
        g.setLineWidth(1.5);
        g.strokeRect(x + 0.5, y + 0.5, cw, ch);
        g.setLineWidth(0.8);
        g.strokeRect(x + 4.5, y + 4.5, cw - 8, ch - 8);
        g.setFill(CARTOUCHE_INK);
        g.setFont(TITLE);
        g.fillText("IDLE", x + 12, y + 26);
        g.setFont(MONO_SMALL);
        g.fillText(VERSION_LINE, x + 12, y + 42);
    }
}
