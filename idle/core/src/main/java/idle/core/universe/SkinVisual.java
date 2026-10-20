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
    COLLIDER("Collisionneur");

    private final String label;

    SkinVisual(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
