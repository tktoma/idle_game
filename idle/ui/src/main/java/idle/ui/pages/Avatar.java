package idle.ui.pages;

import idle.core.Game;
import idle.core.profile.Title;
import java.util.Locale;

/**
 * Les avatars que dessine le jeu (version 0.147) : un par étape de la partie, de la particule à l'arbre-monde. Les
 * deux premiers sont là d'emblée ou presque ; les autres attendent un titre, celui qui dit qu'on est allé jusque-là.
 * Ils se sauvegardent par leur identifiant ({@link #id()}), dans {@code game.state().avatar()} ; l'image importée par
 * le joueur s'y note {@link #CUSTOM}.
 */
public enum Avatar {
    PARTICLE("Particule", "#9fd0ff", null),
    ATOM("Atome", "#ffd27f", Title.APPRENTICE_ALCHEMIST),
    MOLECULE("Molécule", "#fff1c9", Title.BANG_MAKER),
    DARK_MATTER("Matière noire", "#c9a6ff", Title.SHADOW_KEEPER),
    QUARK("Quark", "#9ad0ff", Title.QUARK_HUNTER),
    COMET("Comète", "#9fe6ff", Title.COMET_CHASER),
    STAR("Étoile", "#ffe08a", Title.STAR_SMITH),
    PLANET("Planète", "#8fd6b0", Title.PLANET_GARDENER),
    BLACK_HOLE("Trou noir", "#ff9a3a", Title.VOID_WATCHER),
    GALAXY("Galaxie", "#a8b8ff", Title.GALAXY_ARCHITECT),
    SEED("Graine d'univers", "#e6d27a", Title.SEED_GARDENER),
    WORLD_TREE("Arbre-monde", "#9be7a8", Title.WORLD_TREE_KEEPER);

    /** L'identifiant sauvegardé quand le joueur porte l'image qu'il a importée. */
    public static final String CUSTOM = "custom";

    private final String label;
    private final String color;
    private final Title unlock;

    Avatar(String label, String color, Title unlock) {
        this.label = label;
        this.color = color;
        this.unlock = unlock;
    }

    /** Le nom affiché sous l'avatar. */
    public String label() {
        return label;
    }

    /** Sa couleur : celle du cercle qui l'entoure. */
    public String color() {
        return color;
    }

    /** Le titre qui l'ouvre, ou {@code null} pour un avatar libre. */
    public Title unlock() {
        return unlock;
    }

    /** L'identifiant sauvegardé : « black_hole ». */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Vrai si le joueur peut le porter dans cette partie. */
    public boolean isOpen(Game game) {
        return unlock == null || game.hasTitle(unlock);
    }

    /** L'avatar de cet identifiant, ou {@code null} (vide, image importée, ou un avatar d'une autre version). */
    public static Avatar of(String id) {
        for (Avatar avatar : values()) if (avatar.id().equals(id)) return avatar;
        return null;
    }
}
