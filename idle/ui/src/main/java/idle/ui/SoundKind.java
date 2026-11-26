package idle.ui;

/** Les catégories de sons, que les réglages gardent ou coupent une à une. */
public enum SoundKind {
    PARTICLES("Particules créées"),
    ATOMS("Fusions (atomes)"),
    DARK_MATTER("Matière noire qui grossit"),
    EXPLOSION("Explosions du tableau"),
    BIG_BANG("Big Bangs"),
    UNIVERSE("Univers formés"),
    DIMENSIONS("Dimensions : gestes et points gagnés"),
    UPGRADES("Achats et interrupteurs"),
    CLICKS("Clics sur les boutons"),
    TABS("Changements d'onglet"),
    COLLIDER("Le collisionneur (collisions et son de fond)"),
    HADRONS("Hadrons créés"),
    VOID("Le vide"),
    SEED("La graine"),
    EXPANSION("L'expansion de la matière (son de fond)"),
    PROBES("Sondes lancées"),
    CHRONO("Le chrono qui tourne"),
    NEWS("Autres annonces (succès, éléments…)");

    private final String label;

    SoundKind(String label) { this.label = label; }

    /** Le nom de la catégorie, sur son bouton dans les réglages. */
    public String label() { return label; }
}
