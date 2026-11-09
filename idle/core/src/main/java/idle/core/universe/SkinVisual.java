package idle.core.universe;

/**
 * Les visuels du jeu qui peuvent changer d'apparence (version 0.087) : l'onglet Skins en donne un
 * choix pour chacun. « Classique » est l'apparence d'origine, toujours là.
 */
public enum SkinVisual {
    PARTICLE_FUSION("Fusion des particules"),
    ATOM("Atome"),
    TABLE("Tableau périodique"),
    DARK_BALL("Boule de matière noire"),
    DARK_ANIMATION("Animation de la matière noire"),
    BIG_BANG("Big Bang"),
    COLLIDER_BEAM("Faisceau du collisionneur"),
    COLLIDER("Collisionneur"),
    /** Version 0.185 : l'arbre de la graine, du bonsaï à l'arbre-monde, et ses essences. */
    TREE("L'arbre"),
    /** Version 0.186 : les astres, dans l'espace, le ciel et leurs cartes. */
    BODY("Les astres"),
    /** Version 0.187 : les dix mini-jeux des dimensions, un visuel chacun. */
    LINE("La ligne"),
    PLANE("Le plan"),
    VOLUME("Le volume"),
    DURATION("La durée"),
    LOOP("La boucle"),
    BRANE("La brane"),
    CLICKER("Le clicker"),
    GRID("La grille"),
    MIRROR("Le miroir"),
    MIX("Le mélange"),
    /** Version 0.188 : les molécules, boules et bâtons. */
    MOLECULE("Les molécules"),
    /** Version 0.189 : le cadre de l'avatar du profil. */
    AVATAR_FRAME("Le cadre de l'avatar"),
    /** Version 0.190 : le chrome de la fenêtre, en trois visuels. */
    AMBIENT("Le fond de la fenêtre"),
    TABS("Les onglets"),
    CARD("Les cartes"),
    /** Version 0.192 : les icônes d'onglet, les signes, les info-bulles, le fond des particules, leur fusion et la page du tableau. */
    TAB_ICONS("Les icônes d'onglet"),
    CARD_SIGNS("Les signes d'état des cartes"),
    MATTER_SIGNS("Les signes des états de la matière"),
    TOOLTIP("Les info-bulles"),
    AMBIENT_DUST("Le fond animé des particules"),
    GENERATOR_FUSION("La fusion des générateurs"),
    TABLE_PAGE("La page du tableau périodique");

    /** Les visuels des dix dimensions, dans l'ordre (version 0.187) : {@code DIMENSIONS[n - 1]} pour la dimension n. */
    public static final SkinVisual[] DIMENSIONS = {LINE, PLANE, VOLUME, DURATION, LOOP, BRANE, CLICKER, GRID, MIRROR, MIX};

    private final String label;

    SkinVisual(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
