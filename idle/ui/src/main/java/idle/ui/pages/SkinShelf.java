package idle.ui.pages;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Le rangement de l'onglet Skins (version 0.249) : les visuels par catégorie, chacune un sous-onglet, et quels skins s'y
 * montrent. Un skin ne se montre qu'une fois son palier atteint : l'univers qu'il attend
 * ({@link Skin#minUniverses()}) et, pour le skin d'une dimension, cette dimension ouverte ; un skin déjà débloqué se
 * montre toujours. Sans affichage, pour les vérifications.
 */
public final class SkinShelf {
    private SkinShelf() {
    }

    /** Les catégories, dans l'ordre des sous-onglets, après « Collection ». */
    public enum Category {
        WINDOW("Fenêtre", SkinVisual.AMBIENT, SkinVisual.TABS, SkinVisual.CARD, SkinVisual.TAB_ICONS, SkinVisual.CARD_SIGNS,
                SkinVisual.TOOLTIP, SkinVisual.PANEL, SkinVisual.TOAST, SkinVisual.CURSOR, SkinVisual.START_BUTTON),
        MATTER("Particules et atomes", SkinVisual.PARTICLE_FUSION, SkinVisual.AMBIENT_DUST, SkinVisual.GENERATOR_FUSION,
                SkinVisual.ATOM, SkinVisual.TABLE, SkinVisual.TABLE_PAGE),
        DARK("Matière noire", SkinVisual.DARK_BALL, SkinVisual.DARK_ANIMATION, SkinVisual.DARK_RULER, SkinVisual.DARK_TREE,
                SkinVisual.CHALLENGES),
        COLLIDER("Collisionneur et hadrons", SkinVisual.COLLIDER_BEAM, SkinVisual.COLLIDER, SkinVisual.DETECTOR,
                SkinVisual.LIGHTNING, SkinVisual.SPARKS, SkinVisual.QUARK_FLIGHT, SkinVisual.QUARKS, SkinVisual.GLUONS,
                SkinVisual.HADRON_BACKGROUND),
        VOID("Le vide", SkinVisual.VOID_FOAM, SkinVisual.VOID_PAIRS, SkinVisual.VOID_HOLE),
        BIG_BANG("Big Bang", SkinVisual.BIG_BANG, SkinVisual.BODY, SkinVisual.MOLECULE, SkinVisual.MOLECULE_PALETTE,
                SkinVisual.MATTER_SIGNS, SkinVisual.CLUMPS, SkinVisual.BLOCKS, SkinVisual.GAS, SkinVisual.SKY, SkinVisual.SYSTEM,
                SkinVisual.GALAXY, SkinVisual.GALAXY_CLUSTER, SkinVisual.COSMIC_WEB, SkinVisual.PROBE),
        UNIVERSE("Univers", SkinVisual.UNIVERSE_BUBBLE, SkinVisual.UNIVERSE_BIRTH, SkinVisual.UNIVERSE_LINKS, SkinVisual.LAUNCH,
                SkinVisual.TRANSITION, SkinVisual.DIMENSIONS_MAP, SkinVisual.FINALE_FILL),
        SEED("Graine", SkinVisual.SEED, SkinVisual.TREE, SkinVisual.TREE_SEASON, SkinVisual.TREE_FANTASY, SkinVisual.TREE_GROUND,
                SkinVisual.TREE_BUBBLES, SkinVisual.TREE_BUBBLE_LINKS, SkinVisual.WATERING_CAN, SkinVisual.HOSE, SkinVisual.SHELF,
                SkinVisual.CHRONO, SkinVisual.GROWTH_RING, SkinVisual.WEATHER, SkinVisual.SEASONS),
        DIMENSIONS("Dimensions", dimensions()),
        PROFILE("Profil et statistiques", SkinVisual.AVATAR_FRAME, SkinVisual.AVATAR_STYLE, SkinVisual.ALBUM,
                SkinVisual.ACHIEVEMENT_TILE, SkinVisual.ACHIEVEMENT_LOCKED, SkinVisual.CHART, SkinVisual.CHART_PALETTE,
                SkinVisual.TIMELINE);

        private final String label;
        private final List<SkinVisual> visuals;

        Category(String label, SkinVisual... visuals) {
            this.label = label;
            this.visuals = List.of(visuals);
        }

        public String label() { return label; }

        public List<SkinVisual> visuals() { return visuals; }

        private static SkinVisual[] dimensions() {
            List<SkinVisual> all = new ArrayList<>(Arrays.asList(SkinVisual.DIMENSIONS));
            all.add(SkinVisual.DIMENSION_FRAME);
            all.add(SkinVisual.DIMENSION_PALETTE);
            return all.toArray(new SkinVisual[0]);
        }
    }

    /** Vrai si le skin se montre : débloqué, ou son palier atteint (son univers, et pour une dimension, la dimension ouverte). */
    public static boolean shown(Game game, Skin skin) {
        if (game.hasSkin(skin)) return true;
        if (game.universes() < skin.minUniverses()) return false;
        int dimension = Arrays.asList(SkinVisual.DIMENSIONS).indexOf(skin.visual()) + 1;
        return dimension == 0 || game.dimensionsUnlocked() >= dimension;
    }

    /** Les skins qui se montrent pour ce visuel, dans l'ordre du jeu. */
    public static List<Skin> shownSkins(Game game, SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual && shown(game, skin)) skins.add(skin);
        return skins;
    }

    /** Vrai si le visuel a au moins un skin qui se montre : sinon, sa section (le Classique seul) reste cachée. */
    public static boolean shown(Game game, SkinVisual visual) { return !shownSkins(game, visual).isEmpty(); }

    /** Vrai si la catégorie a au moins un visuel qui se montre : sinon, son sous-onglet reste caché. */
    public static boolean shown(Game game, Category category) {
        for (SkinVisual visual : category.visuals()) if (shown(game, visual)) return true;
        return false;
    }

    /** Les skins débloqués et les skins montrés de la catégorie : « 3 / 12 » sur son sous-onglet. */
    public static int[] counts(Game game, Category category) {
        int unlocked = 0, shown = 0;
        for (SkinVisual visual : category.visuals()) {
            for (Skin skin : shownSkins(game, visual)) {
                shown++;
                if (game.hasSkin(skin)) unlocked++;
            }
        }
        return new int[] {unlocked, shown};
    }
}
