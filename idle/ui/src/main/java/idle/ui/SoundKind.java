package idle.ui;

/** Les catégories de sons, que les réglages gardent ou coupent une à une. */
public enum SoundKind {
    PARTICLES("Particules créées"),
    ATOMS("Fusions (atomes)"),
    EXPLOSION("Explosions du tableau"),
    BIG_BANG("Big Bangs"),
    UNIVERSE("Univers formés et défi final"),
    DIMENSIONS("Dimensions : gestes et points gagnés"),
    UPGRADES("Achats et interrupteurs"),
    CLICKS("Clics sur les boutons"),
    TABS("Changements d'onglet"),
    COLLIDER("Collisions du collisionneur"),
    HADRONS("Hadrons créés"),
    VOID("Le vide"),
    SEED("La graine"),
    NEWS("Autres annonces (succès, défis, skins, éléments…)");

    private final String label;

    SoundKind(String label) { this.label = label; }

    /** Le nom de la catégorie, sur son bouton dans les réglages. */
    public String label() { return label; }
}
