package idle.ui.universe;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * L'arbre au-delà de ses essences (version 0.197) : trois visuels qui se combinent avec l'essence ({@link SeedTree}).
 *
 * <ul>
 *   <li>La saison figée ({@link idle.core.universe.SkinVisual#TREE_SEASON}) : l'arbre reste en automne (feuillage roux,
 *   feuilles qui tombent en nombre, un tapis de feuilles au pied), en hiver (nu et givré, de la neige au pied, ni lucioles
 *   ni feuilles qui tombent) ou au printemps (feuillage tendre, des fleurs à chaque bout, des pétales qui tombent), quel
 *   que soit le temps du jeu. Avec un arbre de fantaisie, le feuillage de la fantaisie reste ; la saison ne garde que ce
 *   qui l'entoure (ce qui tombe, les lucioles, le pied).</li>
 *   <li>La fantaisie ({@link idle.core.universe.SkinVisual#TREE_FANTASY}) remplace le feuillage et le bois : cristal
 *   (bois de glace, feuilles en losanges qui brillent), corail (bois rose, feuilles en boules, polypes blancs), circuit
 *   lumineux (bois vert sombre, pistes claires, feuilles en puces), nuages (feuillage en bouffées blanches), inversé (tout
 *   l'arbre la tête en bas, les racines en haut), pixel art (feuilles en carrés sur une grille).</li>
 *   <li>Le sol ({@link idle.core.universe.SkinVisual#TREE_GROUND}) remplace le pied : herbe, sable, neige, mousse,
 *   dallage, eau (l'arbre pousse sur une île).</li>
 * </ul>
 *
 * Sans skin ({@link Look#CLASSIC}), rien ne change : SeedTree dessine comme avant.
 */
final class TreeSkinArt {

    private TreeSkinArt() {
    }

    /** La saison figée. */
    enum Season {
        NONE(null), AUTUMN(tones("#7a3a16", "#c4571f", "#f2a24f")), WINTER(null), SPRING(tones("#6fa84f", "#a4d67a", "#e9f7c6"));

        /** Les teintes du feuillage de la saison ; {@code null} garde celles de l'arbre (l'hiver n'en a pas : il est nu). */
        final Color[] tones;

        Season(Color[] tones) {
            this.tones = tones;
        }
    }

    /** L'arbre de fantaisie. */
    enum Fantasy {
        NONE(null, null, null, null),
        CRYSTAL(tones("#5fb7d8", "#9fe1f5", "#e8fbff"), "#6ea6c4", "#b9e4f3", "#eefcff"),
        CORAL(tones("#c23f5e", "#f26d8a", "#ffb6c8"), "#8a2d45", "#d4556f", "#f08aa0"),
        CIRCUIT(tones("#1d7a4a", "#3ddc84", "#c6ffde"), "#0f2f22", "#1a5c3a", "#2a7a4c"),
        CLOUDS(tones("#b9c4d6", "#e6ecf5", "#ffffff"), "#5f6a7a", "#8792a4", "#aab4c4"),
        INVERTED(null, null, null, null),
        PIXEL(tones("#1d6b2a", "#3fa545", "#8ee06a"), "#4a2a14", "#7a4a24", "#a8703a");

        /** Les teintes du feuillage ; {@code null} garde celles de l'arbre. */
        final Color[] tones;
        /** Le bois : l'ombre, la face, la face éclairée ; {@code null} garde ceux de l'essence. */
        final Color barkDark, bark, barkLight;

        Fantasy(Color[] tones, String dark, String bark, String light) {
            this.tones = tones;
            this.barkDark = dark == null ? null : Color.web(dark);
            this.bark = bark == null ? null : Color.web(bark);
            this.barkLight = light == null ? null : Color.web(light);
        }
    }

    /** Le sol. */
    enum Ground {
        NONE, GRASS, SAND, SNOW, MOSS, PAVING, WATER
    }

    /** Les trois skins de l'arbre, lus en une fois ; {@link #CLASSIC} sans aucun. */
    record Look(Season season, Fantasy fantasy, Ground ground) {

        static final Look CLASSIC = new Look(Season.NONE, Fantasy.NONE, Ground.NONE);

        /** Les skins choisis pour la saison, la fantaisie et le sol ({@code null} ou un autre visuel : rien). */
        static Look of(Skin season, Skin fantasy, Skin ground) {
            Season s = season == null ? Season.NONE : switch (season) {
                case TREE_AUTUMN -> Season.AUTUMN;
                case TREE_WINTER -> Season.WINTER;
                case TREE_SPRING -> Season.SPRING;
                default -> Season.NONE;
            };
            Fantasy f = fantasy == null ? Fantasy.NONE : switch (fantasy) {
                case TREE_CRYSTAL -> Fantasy.CRYSTAL;
                case TREE_CORAL -> Fantasy.CORAL;
                case TREE_CIRCUIT -> Fantasy.CIRCUIT;
                case TREE_CLOUDS -> Fantasy.CLOUDS;
                case TREE_INVERTED -> Fantasy.INVERTED;
                case TREE_PIXEL -> Fantasy.PIXEL;
                default -> Fantasy.NONE;
            };
            Ground g = ground == null ? Ground.NONE : switch (ground) {
                case GROUND_GRASS -> Ground.GRASS;
                case GROUND_SAND -> Ground.SAND;
                case GROUND_SNOW -> Ground.SNOW;
                case GROUND_MOSS -> Ground.MOSS;
                case GROUND_PAVING -> Ground.PAVING;
                case GROUND_WATER -> Ground.WATER;
                default -> Ground.NONE;
            };
            return s == Season.NONE && f == Fantasy.NONE && g == Ground.NONE ? CLASSIC : new Look(s, f, g);
        }

        /** Les teintes du feuillage : celles de la fantaisie, sinon celles de la saison, sinon {@code tones}, celles de l'arbre. */
        Color[] tones(Color[] tones) {
            if (fantasy.tones != null) return fantasy.tones;
            if (season.tones != null) return season.tones;
            return tones;
        }

        /** Vrai quand l'arbre est nu : l'hiver figé, sans fantaisie qui garde son feuillage. */
        boolean bare() {
            return season == Season.WINTER && fantasy == Fantasy.NONE;
        }

        /** Vrai quand chaque bout fleurit : le printemps figé, sans fantaisie. */
        boolean blossoms() {
            return season == Season.SPRING && fantasy == Fantasy.NONE;
        }

        /** Le bois de la fantaisie à la place de celui de l'essence : {@code wood} est sa face ou sa face éclairée. */
        Color wood(Color wood, Color essenceBark) {
            if (fantasy.bark == null) return wood;
            return wood == essenceBark ? fantasy.bark : fantasy.barkLight;
        }

        Color barkDark(Color essenceDark) {
            return fantasy.barkDark == null ? essenceDark : fantasy.barkDark;
        }
    }

    private static Color[] tones(String shade, String mid, String light) {
        return new Color[] {Color.web(shade), Color.web(mid), Color.web(light)};
    }

    private static final Color FROST = Color.web("#dff6ff"), SNOW = Color.web("#f4f9ff"), SNOW_SHADE = Color.web("#b9d3ea");
    private static final Color SAND = Color.web("#d9c28a"), SAND_SHADE = Color.web("#b89d62");
    private static final Color MOSS = Color.web("#4f8a3c"), MOSS_LIGHT = Color.web("#7fbf5a");
    private static final Color SLAB = Color.web("#6d7078"), SLAB_LIGHT = Color.web("#8e929b"), JOINT = Color.web("#3a3c42");
    private static final Color WATER = Color.web("#2a6f9e"), WATER_LIGHT = Color.web("#6fb4dd"), ISLAND = Color.web("#5a4a30");
    private static final Color GRASS_DARK = Color.web("#3c7a34"), GRASS = Color.web("#5fae4b"), GRASS_LIGHT = Color.web("#9ad96f");
    private static final Color TRACE = Color.web("#8dffc2"), POLYP = Color.web("#fff3f6"), BLOSSOM = Color.web("#ffb7d0");
    private static final Color[] FLOWERS = {Color.web("#ffd27f"), Color.web("#ff9eb5"), Color.web("#e8f4ff")};

    // ------------------------------------------------------------------
    // Le sol
    // ------------------------------------------------------------------

    /**
     * Le sol du skin, à la place du pied Classique : une ellipse de la largeur du pied, et ce qui le couvre. Rend la
     * demi-largeur du sol où se posent neige et feuilles de la saison.
     */
    static double ground(GraphicsContext g, double baseX, double baseY, double height, int stage, double time, Ground ground) {
        double width = height * (0.55 + 0.08 * (stage - 5)), thick = height * 0.09;
        switch (ground) {
            case GRASS -> {
                g.setGlobalAlpha(0.95);
                g.setFill(GRASS_DARK);
                g.fillOval(baseX - width, baseY - thick * 0.4, width * 2, thick);
                int blades = 50 + 10 * (stage - 5);
                for (int k = 0; k < blades; k++) {
                    double x = baseX - width * 0.97 + 2 * width * 0.97 * UniverseView.noise(k * 3 + 311);
                    double len = height * (0.03 + 0.04 * UniverseView.noise(k * 3 + 312));
                    double bend = Math.sin(time * 1.4 + k) * len * 0.3;
                    g.setStroke(k % 3 == 0 ? GRASS_LIGHT : GRASS);
                    g.setLineWidth(Math.max(1.2, height * 0.006));
                    g.setGlobalAlpha(0.9);
                    g.strokeLine(x, baseY, x + bend, baseY - len);
                }
                for (int k = 0; k < 6 + stage; k++) {
                    double x = baseX - width * 0.9 + 1.8 * width * UniverseView.noise(k * 5 + 331), r = Math.max(1.5, height * 0.008);
                    g.setFill(FLOWERS[k % FLOWERS.length]);
                    g.setGlobalAlpha(0.9);
                    g.fillOval(x - r, baseY - height * 0.02 - r, 2 * r, 2 * r);
                }
            }
            case SAND -> {
                g.setGlobalAlpha(0.95);
                g.setFill(SAND);
                g.fillOval(baseX - width, baseY - thick * 0.4, width * 2, thick);
                g.setStroke(SAND_SHADE);
                g.setLineWidth(Math.max(1, height * 0.004));
                g.setGlobalAlpha(0.6);
                for (int k = 0; k < 7; k++) {
                    double y = baseY - thick * 0.3 + thick * 0.9 * (k + 0.5) / 7, half = width * Math.sqrt(Math.max(0, 1 - Math.pow((y - baseY - thick * 0.1) / (thick * 0.5), 2))) * 0.9;
                    g.strokeLine(baseX - half, y, baseX + half, y + Math.sin(k) * 1.5);
                }
            }
            case SNOW -> {
                g.setGlobalAlpha(0.95);
                g.setFill(SNOW_SHADE);
                g.fillOval(baseX - width, baseY - thick * 0.3, width * 2, thick);
                g.setFill(SNOW);
                g.fillOval(baseX - width * 0.97, baseY - thick * 0.45, width * 1.94, thick * 0.8);
                g.setFill(Color.WHITE);
                for (int k = 0; k < 20; k++) {
                    double x = baseX - width * 0.9 + 1.8 * width * UniverseView.noise(k * 7 + 351), y = baseY - thick * 0.3 + thick * 0.5 * UniverseView.noise(k * 7 + 352);
                    g.setGlobalAlpha(0.4 + 0.6 * (0.5 + 0.5 * Math.sin(time * 2 + k)));
                    g.fillOval(x - 1, y - 1, 2, 2);
                }
            }
            case MOSS -> {
                g.setGlobalAlpha(0.95);
                g.setFill(MOSS);
                g.fillOval(baseX - width, baseY - thick * 0.4, width * 2, thick);
                g.setFill(MOSS_LIGHT);
                for (int k = 0; k < 24 + 4 * (stage - 5); k++) {
                    double x = baseX - width * 0.9 + 1.8 * width * UniverseView.noise(k * 5 + 371), r = height * (0.01 + 0.016 * UniverseView.noise(k * 5 + 372));
                    g.setGlobalAlpha(0.75);
                    g.fillOval(x - r, baseY - thick * 0.25 - r * 0.8, r * 2, r * 1.6);
                }
            }
            case PAVING -> {
                g.setGlobalAlpha(0.95);
                g.setFill(JOINT);
                g.fillOval(baseX - width, baseY - thick * 0.4, width * 2, thick);
                double slab = Math.max(6, height * 0.05);
                for (double x = baseX - width + 2; x < baseX + width - slab; x += slab + 2) {
                    for (int row = 0; row < 2; row++) {
                        double y = baseY - thick * 0.3 + row * (thick * 0.42), half = width * 0.92;
                        if (Math.abs(x + slab / 2 - baseX) > half) continue;
                        g.setFill(((int) ((x - baseX) / slab) + row) % 2 == 0 ? SLAB : SLAB_LIGHT);
                        g.setGlobalAlpha(0.95);
                        g.fillRoundRect(x, y, slab, thick * 0.36, 2, 2);
                    }
                }
            }
            case WATER -> {
                g.setGlobalAlpha(0.9);
                g.setFill(WATER);
                g.fillOval(baseX - width * 1.6, baseY - thick * 0.5, width * 3.2, thick * 1.8);
                g.setStroke(WATER_LIGHT);
                g.setLineWidth(Math.max(1, height * 0.004));
                for (int k = 0; k < 4; k++) {
                    double t = ((time * 0.25 + k * 0.25) % 1), rx = width * (0.6 + t), ry = thick * (0.25 + 0.5 * t);
                    g.setGlobalAlpha(0.6 * (1 - t));
                    g.strokeOval(baseX - rx, baseY + thick * 0.2 - ry, 2 * rx, 2 * ry);
                }
                g.setGlobalAlpha(1);
                g.setFill(ISLAND);
                g.fillOval(baseX - width * 0.5, baseY - thick * 0.35, width, thick * 0.7);
                g.setFill(GRASS);
                g.fillOval(baseX - width * 0.45, baseY - thick * 0.4, width * 0.9, thick * 0.45);
                g.setGlobalAlpha(1);
                return width * 0.45;
            }
            default -> {
            }
        }
        g.setGlobalAlpha(1);
        return width * 0.95;
    }

    /** Ce que la saison figée dépose au pied, sur une demi-largeur {@code half} : la neige de l'hiver, les feuilles de l'automne. */
    static void groundSeason(GraphicsContext g, double baseX, double baseY, double height, Season season, double half, Color[] tones) {
        if (season == Season.WINTER) {
            g.setGlobalAlpha(0.92);
            g.setFill(SNOW);
            g.fillOval(baseX - half, baseY - height * 0.045, half * 2, height * 0.06);
            g.setGlobalAlpha(1);
        } else if (season == Season.AUTUMN) {
            for (int k = 0; k < 40; k++) {
                double x = baseX - half + 2 * half * UniverseView.noise(k * 3 + 401), y = baseY - height * 0.02 + height * 0.035 * UniverseView.noise(k * 3 + 402);
                double r = Math.max(1.5, height * 0.007);
                g.setFill(tones[k % 3]);
                g.setGlobalAlpha(0.85);
                g.fillOval(x - r, y - r * 0.6, 2 * r, 1.2 * r);
            }
            g.setGlobalAlpha(1);
        }
    }

    // ------------------------------------------------------------------
    // Le bois et les feuilles
    // ------------------------------------------------------------------

    /** Ce que la fantaisie ajoute sur un segment de bois : la piste lumineuse du circuit, le reflet du cristal. */
    static void wood(GraphicsContext g, Fantasy fantasy, double x0, double y0, double x1, double y1, double w0, double w1, int seed) {
        if (fantasy == Fantasy.CIRCUIT) {
            g.setStroke(TRACE);
            g.setLineWidth(Math.max(1, w0 * 0.12));
            g.setGlobalAlpha(0.55 + 0.35 * UniverseView.noise(seed * 3 + 7));
            g.strokeLine(x0, y0, x1, y1);
            double r = Math.max(1.2, w1 * 0.25);
            g.setFill(TRACE);
            g.fillOval(x1 - r, y1 - r, 2 * r, 2 * r);
            g.setGlobalAlpha(1);
        } else if (fantasy == Fantasy.CRYSTAL) {
            double dx = x1 - x0, dy = y1 - y0, len = Math.max(1e-6, Math.hypot(dx, dy)), nx = -dy / len, ny = dx / len;
            g.setStroke(Color.WHITE);
            g.setLineWidth(Math.max(0.8, w0 * 0.08));
            g.setGlobalAlpha(0.7);
            g.strokeLine(x0 + nx * w0 * 0.3, y0 + ny * w0 * 0.3, x1 + nx * w1 * 0.3, y1 + ny * w1 * 0.3);
            g.setGlobalAlpha(1);
        }
    }

    /** Une feuille de la fantaisie : losange de cristal, boule de corail, puce de circuit, carré de pixel art. */
    static void leaf(GraphicsContext g, Fantasy fantasy, double x, double y, double size, double angle, Color tone, double alpha) {
        g.setFill(tone);
        g.setGlobalAlpha(alpha);
        switch (fantasy) {
            case CRYSTAL -> {
                double c = Math.cos(angle), s = Math.sin(angle), l = size * 1.3, hw = 0.4;
                g.setGlobalAlpha(alpha * 0.75);
                g.fillPolygon(new double[] {x - c * l, x - s * l * hw, x + c * l, x + s * l * hw},
                        new double[] {y - s * l, y + c * l * hw, y + s * l, y - c * l * hw}, 4);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(alpha * 0.6);
                g.fillOval(x - size * 0.2, y - size * 0.2, size * 0.4, size * 0.4);
            }
            case CORAL -> {
                g.fillOval(x - size * 0.9, y - size * 0.9, size * 1.8, size * 1.8);
                g.setFill(POLYP);
                g.setGlobalAlpha(alpha * 0.7);
                g.fillOval(x - size * 0.2, y - size * 0.45, size * 0.4, size * 0.4);
            }
            case CIRCUIT -> {
                g.fillRect(x - size * 0.7, y - size * 0.5, size * 1.4, size);
                g.setFill(TRACE);
                g.setGlobalAlpha(alpha * 0.8);
                g.fillRect(x - size * 0.25, y - size * 0.2, size * 0.5, size * 0.4);
            }
            default -> {
                // Le pixel art : un carré posé sur une grille de la taille de la feuille.
                double px = Math.max(3, Math.round(size * 1.4));
                double gx = Math.floor(x / px) * px, gy = Math.floor(y / px) * px;
                g.fillRect(gx, gy, px, px);
            }
        }
    }

    /** Une touffe de nuage : trois bouffées qui se chevauchent, dans deux teintes. */
    static void cloud(GraphicsContext g, double x, double y, double r, double flat, Color mid, Color light, int index) {
        for (int k = 0; k < 3; k++) {
            double ox = (k - 1) * r * 0.55, oy = (UniverseView.noise(index * 7 + k) - 0.5) * r * 0.3, rr = r * (0.55 + 0.25 * UniverseView.noise(index * 7 + k + 3));
            g.setFill(k == 1 ? light : mid);
            g.setGlobalAlpha(0.85);
            g.fillOval(x + ox - rr, y + oy - rr * flat, 2 * rr, 2 * rr * flat);
        }
    }

    /** Le givre d'un bout nu : quelques traits blancs, fins, en éventail, qui scintillent un peu. */
    static void frost(GraphicsContext g, double x, double y, double size, double angle, double time, int index) {
        g.setStroke(FROST);
        g.setLineWidth(Math.max(0.8, size * 0.08));
        g.setGlobalAlpha(0.45 + 0.35 * (0.5 + 0.5 * Math.sin(time * 1.5 + index)));
        for (int k = -1; k <= 1; k++) {
            double a = angle - Math.PI / 2 + k * 0.5, l = size * (0.5 + 0.3 * UniverseView.noise(index * 5 + k + 1));
            g.strokeLine(x, y, x + Math.cos(a) * l, y + Math.sin(a) * l);
        }
        g.setGlobalAlpha(1);
    }

    /** Une fleur du printemps figé : un rond rose à chaque bout, et son cœur clair. */
    static void blossom(GraphicsContext g, double x, double y, double r) {
        g.setFill(BLOSSOM);
        g.setGlobalAlpha(0.95);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(Color.web("#fff6e0"));
        g.fillOval(x - r * 0.35, y - r * 0.35, r * 0.7, r * 0.7);
    }
}
