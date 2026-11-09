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
    TABLE_PAGE("La page du tableau périodique"),
    /** Version 0.193 : la matière noire hors de la boule : la règle et l'échelle, l'arbre, les défis. */
    DARK_RULER("L'échelle des grandeurs"),
    DARK_TREE("L'arbre de matière noire"),
    CHALLENGES("Les défis"),
    /** Version 0.194 : le Big Bang au-delà des molécules : leur palette, les amas, les blocs, le gaz, le ciel, le cosmos, les systèmes, les sondes. */
    MOLECULE_PALETTE("La palette des molécules"),
    CLUMPS("Les amas rassemblés"),
    BLOCKS("Les blocs d'assemblage"),
    GAS("Le gaz au loin"),
    SKY("Le ciel des astres"),
    GALAXY("La galaxie"),
    GALAXY_CLUSTER("L'amas de galaxies"),
    COSMIC_WEB("La toile de l'univers"),
    SYSTEM("Les systèmes solaires"),
    PROBE("Les sondes"),
    /** Version 0.195 : le reste du collisionneur, les hadrons et le vide. */
    DETECTOR("Le détecteur"),
    LIGHTNING("Les éclairs du croisement"),
    SPARKS("Les étincelles"),
    QUARK_FLIGHT("Les quarks qui s'envolent"),
    QUARKS("Les quarks de la liaison"),
    GLUONS("Les gluons"),
    HADRON_BACKGROUND("Le fond des hadrons"),
    VOID_FOAM("L'écume du vide"),
    VOID_PAIRS("Les paires du vide"),
    VOID_HOLE("Le trou noir du vide"),
    /** Version 0.196 : l'univers : ses bulles, leur naissance et leurs liens, le lancement, la graine et son établi. */
    UNIVERSE_BUBBLE("Les bulles des univers"),
    UNIVERSE_BIRTH("La naissance d'un univers"),
    UNIVERSE_LINKS("Les liens entre les univers"),
    LAUNCH("Le lancement de l'univers suivant"),
    SEED("La graine"),
    WATERING_CAN("L'arrosoir"),
    SHELF("L'étagère"),
    HOSE("Le tuyau d'arrosage"),
    CHRONO("Le chrono"),
    GROWTH_RING("Le rond de croissance"),
    WEATHER("La météo de la graine"),
    SEASONS("Les saisons"),
    /** Version 0.197 : l'arbre au-delà de ses essences, ses bulles, la carte des dimensions, le défi final, les cadres et la palette des mini-jeux. */
    TREE_SEASON("La saison de l'arbre"),
    TREE_FANTASY("L'arbre de fantaisie"),
    TREE_GROUND("Le sol de l'arbre"),
    TREE_BUBBLES("Les bulles de l'arbre"),
    TREE_BUBBLE_LINKS("Les liens des bulles de l'arbre"),
    DIMENSIONS_MAP("La carte des dimensions"),
    FINALE_FILL("Le remplissage du défi final"),
    DIMENSION_FRAME("Le cadre des mini-jeux"),
    DIMENSION_PALETTE("La palette des dimensions"),
    /** Version 0.198 : le profil, les succès et les statistiques : le style des avatars, les albums, les tuiles des succès, les courbes, la frise. */
    AVATAR_STYLE("Le style des avatars"),
    ALBUM("Les albums"),
    ACHIEVEMENT_TILE("Les tuiles des succès"),
    ACHIEVEMENT_LOCKED("Les succès non obtenus"),
    CHART("Les courbes des statistiques"),
    CHART_PALETTE("La palette des courbes"),
    TIMELINE("La frise des univers");

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
