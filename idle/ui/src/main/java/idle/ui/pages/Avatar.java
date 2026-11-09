package idle.ui.pages;

import idle.core.Game;
import idle.core.profile.Title;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Les avatars que dessine le jeu (version 0.147) : un par étape de la partie, de la particule à l'arbre-monde. Les
 * deux premiers sont là d'emblée ou presque ; les autres attendent un titre, celui qui dit qu'on est allé jusque-là.
 * Ils se sauvegardent par leur identifiant ({@link #id()}), dans {@code game.state().avatar()} ; l'image importée par
 * le joueur s'y note {@link #CUSTOM}.
 *
 * <p>Version 0.198 : huit avatars de plus (le hadron, la sonde, la toile de l'univers, la bulle, l'arrosoir, le chrono,
 * le miroir, le kaléidoscope). Les cinq qui ont un titre l'attendent ; la sonde, l'arrosoir et le chrono, qui n'en ont
 * pas, attendent dix voyages de sondes, le tuyau d'arrosage et le chrono achetés.
 */
public enum Avatar {
    PARTICLE("Particule", "#9fd0ff", null),
    ATOM("Atome", "#ffd27f", Title.APPRENTICE_ALCHEMIST),
    MOLECULE("Molécule", "#fff1c9", Title.BANG_MAKER),
    DARK_MATTER("Matière noire", "#c9a6ff", Title.SHADOW_KEEPER),
    QUARK("Quark", "#9ad0ff", Title.QUARK_HUNTER),
    HADRON("Hadron", "#ff8a75", Title.GLUON_WEAVER),
    COMET("Comète", "#9fe6ff", Title.COMET_CHASER),
    STAR("Étoile", "#ffe08a", Title.STAR_SMITH),
    PLANET("Planète", "#8fd6b0", Title.PLANET_GARDENER),
    BLACK_HOLE("Trou noir", "#ff9a3a", Title.VOID_WATCHER),
    GALAXY("Galaxie", "#a8b8ff", Title.GALAXY_ARCHITECT),
    PROBE("Sonde", "#d6dde6", "après dix voyages de sondes", "Faire voyager dix sondes", game -> game.state().probeRuns() >= 10),
    COSMIC_WEB("Toile de l'univers", "#e0c8ff", Title.UNIVERSE_CREATOR),
    BUBBLE("Bulle d'univers", "#b8e6ff", Title.MULTIVERSE_WEAVER),
    WATERING_CAN("Arrosoir", "#6fc3b5", "avec le tuyau d'arrosage", "Acheter le tuyau d'arrosage de la graine", Game::hasHose),
    CHRONO("Chrono", "#f0c060", "avec le chrono", "Acheter le chrono de la graine", Game::isChronoUnlocked),
    MIRROR("Miroir", "#c9d6ff", Title.MIRROR_KEEPER),
    KALEIDOSCOPE("Kaléidoscope", "#f0a8c0", Title.MIX_MASTER),
    SEED("Graine d'univers", "#e6d27a", Title.SEED_GARDENER),
    WORLD_TREE("Arbre-monde", "#9be7a8", Title.WORLD_TREE_KEEPER);

    /** L'identifiant sauvegardé quand le joueur porte l'image qu'il a importée. */
    public static final String CUSTOM = "custom";

    private final String label;
    private final String color;
    private final Title unlock;
    /** Comment il s'ouvre, en quelques mots : « avec le titre « Alchimiste » » ; vide pour un avatar libre. */
    private final String how;
    /** Ce qu'il faut faire pour l'ouvrir : la condition du titre, ou celle de l'avatar sans titre. */
    private final String condition;
    private final Predicate<Game> open;

    Avatar(String label, String color, Title unlock) {
        this.label = label;
        this.color = color;
        this.unlock = unlock;
        this.how = unlock == null ? "" : "avec le titre « " + unlock.label() + " »";
        this.condition = unlock == null ? "" : unlock.condition();
        this.open = unlock == null ? game -> true : game -> game.hasTitle(unlock);
    }

    /** Version 0.198 : un avatar sans titre, ouvert par une condition à lui. */
    Avatar(String label, String color, String how, String condition, Predicate<Game> open) {
        this.label = label;
        this.color = color;
        this.unlock = null;
        this.how = how;
        this.condition = condition;
        this.open = open;
    }

    /** Le nom affiché sous l'avatar. */
    public String label() {
        return label;
    }

    /** Sa couleur : celle du cercle qui l'entoure. */
    public String color() {
        return color;
    }

    /** Le titre qui l'ouvre, ou {@code null} pour un avatar libre ou ouvert par autre chose qu'un titre ({@link #how()}). */
    public Title unlock() {
        return unlock;
    }

    /** Comment il s'ouvre (version 0.198) : « avec le titre « Alchimiste » », « avec le chrono » ; vide pour un avatar libre. */
    public String how() {
        return how;
    }

    /** Ce qu'il faut faire pour l'ouvrir (version 0.198), en quelques mots ; vide pour un avatar libre. */
    public String condition() {
        return condition;
    }

    /** L'identifiant sauvegardé : « black_hole ». */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Vrai si le joueur peut le porter dans cette partie. */
    public boolean isOpen(Game game) {
        return open.test(game);
    }

    /** L'avatar de cet identifiant, ou {@code null} (vide, image importée, ou un avatar d'une autre version). */
    public static Avatar of(String id) {
        for (Avatar avatar : values()) if (avatar.id().equals(id)) return avatar;
        return null;
    }
}
