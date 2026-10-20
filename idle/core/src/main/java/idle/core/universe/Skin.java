package idle.core.universe;

/**
 * Les skins du deuxième univers (version 0.87 pour l'onglet, 0.88 pour la façon de les obtenir) :
 * des apparences de rechange pour les visuels du jeu. Chacun débloqué ajoute
 * {@link #BONUS} à toutes les ressources d'en dessous de l'univers ; le choisir ne change que l'apparence.
 *
 * @param visual ce qu'il habille
 * @param label  son nom
 * @param source comment on l'obtient, en quelques mots
 */
public enum Skin {
    FUSION_FLASH(SkinVisual.PARTICLE_FUSION, "Éclair", "Record de reconstruction : de 1 à 10 générateurs en moins d'une minute"),
    FUSION_PRISM(SkinVisual.PARTICLE_FUSION, "Prisme", "Recherche « Fusion prismatique », payée en particules"),
    FUSION_LONE(SkinVisual.PARTICLE_FUSION, "Solitaire", "Défi volontaire « Premier générateur seulement »"),
    ATOM_DENSE(SkinVisual.ATOM, "Noyau dense", "Recherche sur le noyau dense, palier 1"),
    TABLE_LEAD(SkinVisual.TABLE, "Plomb", "Recherche sur le noyau dense, palier 2"),
    ATOM_STELLAR(SkinVisual.ATOM, "Noyau stellaire", "Recherche sur le noyau dense, palier 3"),
    TABLE_GOLD(SkinVisual.TABLE, "Or", "Recherche sur le noyau dense, palier 4"),
    DARK_OBSIDIAN(SkinVisual.DARK_BALL, "Obsidienne", "Matière noire, palier 1"),
    DARK_VORTEX(SkinVisual.DARK_ANIMATION, "Vortex", "Matière noire, palier 2"),
    DARK_NEBULA(SkinVisual.DARK_BALL, "Nébuleuse", "Matière noire, palier 3"),
    DARK_PULSAR(SkinVisual.DARK_ANIMATION, "Pulsar", "Matière noire, palier 4"),
    DARK_CONTRACTED(SkinVisual.DARK_BALL, "Contractée", "Défi « Contraction douce »"),
    BANG_AURORA(SkinVisual.BIG_BANG, "Aurore", "Big Bang, palier 1"),
    BANG_SUPERNOVA(SkinVisual.BIG_BANG, "Supernova", "Big Bang, palier 2"),
    BANG_CRYSTAL(SkinVisual.BIG_BANG, "Cristal", "Défi « Big Bang à froid »"),
    BEAM_GOLD(SkinVisual.COLLIDER_BEAM, "Faisceau doré", "Défi du détecteur"),
    COLLIDER_NEON(SkinVisual.COLLIDER, "Néon", "Défi « Sans surchauffe »"),
    COLLIDER_PLASMA(SkinVisual.COLLIDER, "Plasma", "Défi « Pleine charge »"),
    COLLIDER_ICE(SkinVisual.COLLIDER, "Glace", "Défi « Un seul générateur »");

    /** Ce qu'ajoute chaque skin débloqué à toutes les ressources d'en dessous de l'univers : +2 %. */
    public static final double BONUS = 0.02;

    private final SkinVisual visual;
    private final String label;
    private final String source;

    Skin(SkinVisual visual, String label, String source) {
        this.visual = visual;
        this.label = label;
        this.source = source;
    }

    /** L'identifiant gardé dans la sauvegarde. */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public SkinVisual visual() {
        return visual;
    }

    public String label() {
        return label;
    }

    /** Comment on l'obtient. */
    public String source() {
        return source;
    }

    /** Le skin de cet identifiant, ou {@code null}. */
    public static Skin byId(String id) {
        for (Skin skin : values()) if (skin.id().equals(id)) return skin;
        return null;
    }
}
