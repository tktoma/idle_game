package idle.ui.bigbang;

import idle.core.molecules.Molecule;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les skins de ce que la vue de l'espace ({@link SpaceView}) montre entre les molécules et les astres
 * (version 0.194) : les amas rassemblés, les blocs d'assemblage, le gaz au loin. Trois visuels, qui
 * se combinent entre eux et avec les skins du Big Bang, des molécules et des astres.
 *
 * <p><b>Les amas</b> ({@link SkinVisual#CLUMPS}) : chaque molécule rassemblée n'est plus un rond mais
 * une forme qui dit son état ({@link #clumpPiece}) : une flaque aplatie pour un liquide, un cristal à
 * six facettes tourné à sa façon pour un cristal, trois boules de coton pour un gaz. Les ponts et les
 * creux entre voisines restent ce qu'ils sont : l'amas reste une seule surface. La palette change avec
 * le skin ({@link #clumpColor}) : « Minéraux » donne aux liserés le gris du quartz, le bleu du cuivre et
 * le violet de l'améthyste, et éteint la couleur des molécules ; « Néon » fait des amas des tubes sombres
 * cernés d'un liseré vif deux fois plus large ({@link #rimScale}) ; « Pastel » tire tout vers le blanc.
 *
 * <p><b>Les blocs</b> ({@link SkinVisual#BLOCKS}) : les places d'un bloc deviennent des pierres carrées
 * jointoyées de sombre (« Pierre taillée »), des lingots couchés à reflet (« Lingots »), des éclats
 * anguleux à bord blanc (« Cristaux bruts ») ou des pains carrés givrés (« Pains de glace »)
 * ({@link #blockPiece}), et la couleur de leur matière prend la teinte du skin ({@link #blockColor}).
 *
 * <p><b>Le gaz</b> ({@link SkinVisual#GAS}) : les bouffées floutées de loin changent de nature.
 * « Brume » : un voile blanc bleuté, large et léger, plus flou ; « Fumée » : des volutes grises qui
 * montent et grossissent avec le temps ; « Aurore » : des rideaux verts, bleus et violets, étirés en
 * hauteur. Rien n'est alloué au dessin : les couleurs sont fixes ou reprises des tableaux de la vue.
 */
final class ClumpSkinArt {

    private static final Color WHITE = Color.WHITE;

    // Minéraux : le gris du quartz, le bleu du cuivre, le violet de l'améthyste.
    private static final Color QUARTZ = Color.web("#d9d4cc");
    private static final Color COPPER_BLUE = Color.web("#2f7fb0");
    private static final Color AMETHYST = Color.web("#8a6bb0");
    // Néon : des tubes allumés sur un corps presque noir.
    private static final Color NEON_GAS = Color.web("#39ff14");
    private static final Color NEON_LIQUID = Color.web("#00e5ff");
    private static final Color NEON_CRYSTAL = Color.web("#ff2fd6");
    private static final Color NEON_BODY = Color.web("#0b0f16");
    // Pastel : des liserés poudrés.
    private static final Color PASTEL_GAS = Color.web("#fbe4d8");
    private static final Color PASTEL_LIQUID = Color.web("#cfe3ff");
    private static final Color PASTEL_CRYSTAL = Color.web("#e9d5ff");
    // Les blocs : le mortier de la pierre, l'or des lingots, le blanc des éclats et du givre.
    private static final Color MORTAR = Color.web("#2b2620");
    private static final Color GOLD = Color.web("#d4a84b");
    private static final Color GOLD_RIM = Color.web("#fff1b8");
    private static final Color ICE = Color.web("#bfe6ff");
    private static final Color SHARD_RIM = Color.web("#f4fbff");
    // Le gaz : la brume, la fumée, les trois rideaux de l'aurore.
    private static final Color HAZE = Color.web("#dce8f2");
    private static final Color SMOKE = Color.web("#8e8e94");
    private static final Color[] AURORA = {Color.web("#5dffa8"), Color.web("#6fc8ff"), Color.web("#b77bff")};

    /** Fumée : ce dont une volute monte par seconde, en bouffées, et ce dont elle grossit en montant. */
    private static final double SMOKE_RISE = 0.6;
    private static final double SMOKE_SWELL = 1.5;
    /** Aurore : la hauteur d'un rideau, en largeurs. */
    private static final double CURTAIN = 3.2;

    private ClumpSkinArt() {}

    /** Vrai si ce skin habille les amas, les blocs ou le gaz : {@code null} ou un autre visuel, c'est le Classique. */
    static boolean draws(Skin skin) {
        return skin != null && (skin.visual() == SkinVisual.CLUMPS || skin.visual() == SkinVisual.BLOCKS || skin.visual() == SkinVisual.GAS);
    }

    // ------------------------------------------------------------------
    // Les amas
    // ------------------------------------------------------------------

    /**
     * La couleur d'un amas de cet état dans ce skin : son liseré ({@code rim} vrai) ou son corps, à partir
     * de la couleur classique ({@code classic} : celle de l'état pour le liseré, celle de la molécule fondue
     * dans son état pour le corps).
     */
    static Color clumpColor(Skin clumps, Molecule.State state, boolean rim, Color classic) {
        return switch (clumps) {
            case CLUMPS_MINERALS -> {
                Color mineral = switch (state) {
                    case GAS -> QUARTZ;
                    case LIQUID -> COPPER_BLUE;
                    case CRYSTAL -> AMETHYST;
                };
                yield rim ? mineral : classic.deriveColor(0, 0.45, 0.8, 1).interpolate(mineral, 0.35);
            }
            case CLUMPS_NEON -> {
                Color neon = switch (state) {
                    case GAS -> NEON_GAS;
                    case LIQUID -> NEON_LIQUID;
                    case CRYSTAL -> NEON_CRYSTAL;
                };
                yield rim ? neon : NEON_BODY.interpolate(classic, 0.2);
            }
            case CLUMPS_PASTEL -> {
                Color pastel = switch (state) {
                    case GAS -> PASTEL_GAS;
                    case LIQUID -> PASTEL_LIQUID;
                    case CRYSTAL -> PASTEL_CRYSTAL;
                };
                yield rim ? pastel : classic.interpolate(WHITE, 0.5);
            }
            default -> classic;
        };
    }

    /** Ce par quoi la largeur du liseré des amas est multipliée : deux pour les tubes du « Néon », un sinon. */
    static double rimScale(Skin clumps) {
        return clumps == Skin.CLUMPS_NEON ? 2 : 1;
    }

    /**
     * La forme d'une molécule rassemblée dans son amas, dans la couleur de remplissage en cours, tenant
     * dans le cercle de rayon {@code r} centré en ({@code x}, {@code y}) : flaque, cristal ou coton selon l'état.
     *
     * @param index le rang de la place : il tourne les cristaux et pose les boules de coton
     * @param xs    deux tableaux de six nombres au moins, pour ne rien allouer
     */
    static void clumpPiece(GraphicsContext g, Molecule.State state, double x, double y, double r, int index, double[] xs, double[] ys) {
        switch (state) {
            case LIQUID -> g.fillOval(x - r, y - 0.72 * r, 2 * r, 1.44 * r);
            case CRYSTAL -> {
                double turn = index * 0.7;
                for (int k = 0; k < 6; k++) {
                    double a = turn + k * Math.PI / 3;
                    xs[k] = x + r * Math.cos(a);
                    ys[k] = y + r * Math.sin(a);
                }
                g.fillPolygon(xs, ys, 6);
            }
            case GAS -> {
                double puff = 0.6 * r;
                double a = index * 1.9;
                g.fillOval(x - 0.85 * r, y - 0.85 * r, 1.7 * r, 1.7 * r);
                g.fillOval(x + 0.4 * r * Math.cos(a) - puff, y + 0.4 * r * Math.sin(a) - puff, 2 * puff, 2 * puff);
                g.fillOval(x - 0.4 * r * Math.cos(a) - puff, y - 0.4 * r * Math.sin(a) - puff, 2 * puff, 2 * puff);
            }
        }
    }

    // ------------------------------------------------------------------
    // Les blocs
    // ------------------------------------------------------------------

    /** La couleur d'un bloc d'assemblage dans ce skin : son contour ({@code rim} vrai) ou son corps, à partir de la couleur de sa matière. */
    static Color blockColor(Skin blocks, boolean rim, Color tint) {
        return switch (blocks) {
            case BLOCKS_STONE -> rim ? MORTAR : tint.deriveColor(0, 0.35, 0.9, 1);
            case BLOCKS_INGOTS -> rim ? GOLD_RIM : tint.interpolate(GOLD, 0.55);
            case BLOCKS_CRYSTALS -> rim ? SHARD_RIM : tint.deriveColor(0, 1.4, 1.1, 1);
            case BLOCKS_ICE -> rim ? WHITE : tint.interpolate(ICE, 0.6);
            default -> tint;
        };
    }

    /**
     * La forme d'une place d'un bloc d'assemblage, dans la couleur de remplissage en cours, tenant dans le
     * cercle de rayon {@code r} centré en ({@code x}, {@code y}) : une pierre carrée, un lingot couché, un
     * éclat anguleux, un pain de glace.
     *
     * @param index le rang de la place : il tourne les éclats
     * @param xs    deux tableaux de six nombres au moins, pour ne rien allouer
     */
    static void blockPiece(GraphicsContext g, Skin blocks, double x, double y, double r, int index, double[] xs, double[] ys) {
        switch (blocks) {
            case BLOCKS_STONE -> g.fillRect(x - 0.9 * r, y - 0.9 * r, 1.8 * r, 1.8 * r);
            case BLOCKS_INGOTS -> g.fillRoundRect(x - r, y - 0.62 * r, 2 * r, 1.24 * r, 0.5 * r, 0.5 * r);
            case BLOCKS_CRYSTALS -> {
                double turn = index * 1.3;
                for (int k = 0; k < 5; k++) {
                    double a = turn + k * 2 * Math.PI / 5;
                    double reach = k % 2 == 0 ? r : 0.7 * r;
                    xs[k] = x + reach * Math.cos(a);
                    ys[k] = y + reach * Math.sin(a);
                }
                g.fillPolygon(xs, ys, 5);
            }
            case BLOCKS_ICE -> g.fillRoundRect(x - 0.9 * r, y - 0.9 * r, 1.8 * r, 1.8 * r, 0.6 * r, 0.6 * r);
            default -> g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
    }

    /**
     * Le reflet qui donne sa matière à une place de bloc, dessiné après le corps : une arête claire sur
     * un lingot, un éclat de givre sur la glace ; rien sur la pierre ni les cristaux. Le remplissage en
     * cours est celui du contour du bloc.
     */
    static void blockShine(GraphicsContext g, Skin blocks, double x, double y, double r) {
        if (r < 2.5) return;
        switch (blocks) {
            case BLOCKS_INGOTS -> g.fillRoundRect(x - 0.7 * r, y - 0.45 * r, 1.4 * r, 0.18 * r, 0.1 * r, 0.1 * r);
            case BLOCKS_ICE -> g.fillOval(x - 0.6 * r, y - 0.6 * r, 0.4 * r, 0.4 * r);
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // Le gaz
    // ------------------------------------------------------------------

    /** Ce par quoi le rayon d'une bouffée est multiplié : la brume est large, la fumée un peu, l'aurore étroite. */
    static double puffScale(Skin gas) {
        return switch (gas) {
            case GAS_HAZE -> 1.8;
            case GAS_SMOKE -> 1.2;
            case GAS_AURORA -> 0.55;
            default -> 1;
        };
    }

    /** Ce par quoi l'opacité des bouffées est multipliée : la brume est légère, l'aurore vive. */
    static double alphaScale(Skin gas) {
        return switch (gas) {
            case GAS_HAZE -> 0.55;
            case GAS_SMOKE -> 0.8;
            case GAS_AURORA -> 1.3;
            default -> 1;
        };
    }

    /** Ce par quoi le flou du gaz est multiplié : plus pour la brume, moins pour les rideaux de l'aurore. */
    static double blurScale(Skin gas) {
        return switch (gas) {
            case GAS_HAZE -> 1.6;
            case GAS_AURORA -> 0.7;
            default -> 1;
        };
    }

    /** La couleur d'une bouffée : la brume et la fumée ont la leur, l'aurore prend l'une de trois teintes selon le rang de la place. */
    static Color puffColor(Skin gas, int index, Color own) {
        return switch (gas) {
            case GAS_HAZE -> HAZE;
            case GAS_SMOKE -> SMOKE;
            case GAS_AURORA -> AURORA[index % AURORA.length];
            default -> own;
        };
    }

    /**
     * Une bouffée de gaz dans ce skin, de rayon {@code puff} pixels, centrée en ({@code x}, {@code y}), dans la
     * couleur de remplissage en cours : un disque pour la brume, une volute qui monte et grossit avec le temps
     * pour la fumée, un rideau étiré en hauteur pour l'aurore.
     *
     * @param time  les secondes de la vue : la fumée monte avec elles
     * @param phase un nombre propre à la place, entre -1 et 1 : décale chaque volute dans sa montée
     */
    static void puff(GraphicsContext g, Skin gas, double x, double y, double puff, double time, double phase) {
        switch (gas) {
            case GAS_SMOKE -> {
                // La volute monte de SMOKE_RISE bouffées par seconde et reprend du bas tous les quatre rayons ; elle grossit en montant.
                double climb = (time * SMOKE_RISE + 2 * (phase + 1)) % 4;
                double swell = puff * (1 + SMOKE_SWELL * climb / 4);
                g.fillOval(x - swell, y - climb * puff - swell, 2 * swell, 2 * swell);
            }
            case GAS_AURORA -> g.fillOval(x - puff, y - CURTAIN * puff, 2 * puff, 2 * CURTAIN * puff);
            default -> g.fillOval(x - puff, y - puff, 2 * puff, 2 * puff);
        }
    }
}
