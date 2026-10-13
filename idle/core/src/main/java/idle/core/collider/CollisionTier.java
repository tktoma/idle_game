package idle.core.collider;

/**
 * Les cinq énergies de collision. Chacune coûte de l'énergie, dure un temps fixe et rapporte des
 * quarks ; elle s'ouvre quand le hadron de l'énergie précédente a été formé une fois.
 */
public enum CollisionTier {
    LOW("Basse énergie", 100, 30, null),
    MEDIUM("Moyenne énergie", 10_000, 5 * 60, Hadron.PROTON),
    HIGH("Haute énergie", 1e8, 30 * 60, Hadron.KAON),
    VERY_HIGH("Très haute énergie", 1e12, 2 * 3600, Hadron.JPSI),
    EXTREME("Énergie extrême", 1e16, 8 * 3600, Hadron.UPSILON);

    private final String label;
    private final double cost;
    private final double seconds;
    private final Hadron opener;

    CollisionTier(String label, double cost, double seconds, Hadron opener) {
        this.label = label;
        this.cost = cost;
        this.seconds = seconds;
        this.opener = opener;
    }

    public String label() {
        return label;
    }

    /** Énergie dépensée au lancement. */
    public double cost() {
        return cost;
    }

    /** Durée de la collision, en secondes de jeu. */
    public double seconds() {
        return seconds;
    }

    /** Le hadron qu'il faut avoir formé une fois pour ouvrir cette énergie ; {@code null} pour la première. */
    public Hadron opener() {
        return opener;
    }
}
