package idle.core.collider;

/**
 * Les cinq énergies du collisionneur (version 0.060 : elles s'ouvrent une à une par les
 * améliorations). Chacune ajoute une saveur de quark à ce que peut donner un croisement des
 * faisceaux ; l'amélioration qui l'ouvre est {@link ColliderUpgrade#flavorOf(Quark)}.
 */
public enum CollisionTier {
    LOW("Basse énergie", Quark.DOWN),
    MEDIUM("Moyenne énergie", Quark.STRANGE),
    HIGH("Haute énergie", Quark.CHARM),
    VERY_HIGH("Très haute énergie", Quark.BOTTOM),
    EXTREME("Énergie extrême", Quark.TOP);

    private final String label;
    private final Quark flavor;

    CollisionTier(String label, Quark flavor) {
        this.label = label;
        this.flavor = flavor;
    }

    public String label() {
        return label;
    }

    /** La saveur la plus lourde qu'ouvre cette énergie ; la basse énergie donne les quarks haut et bas. */
    public Quark flavor() {
        return flavor;
    }

    /** L'amélioration qui ouvre cette énergie. */
    public ColliderUpgrade upgrade() {
        return ColliderUpgrade.flavorOf(flavor);
    }

    /** L'énergie qui ouvre cette saveur. */
    public static CollisionTier of(Quark quark) {
        return switch (quark) {
            case UP, DOWN -> LOW;
            case STRANGE -> MEDIUM;
            case CHARM -> HIGH;
            case BOTTOM -> VERY_HIGH;
            case TOP -> EXTREME;
        };
    }
}
