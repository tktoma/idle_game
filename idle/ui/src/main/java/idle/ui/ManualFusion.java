package idle.ui;

/**
 * Quand une fusion demandée par le joueur se fait dans le moteur sans attendre l'animation de regroupement des
 * générateurs (1.4 s). Sans JavaFX, pour se tester seule.
 */
final class ManualFusion {
    private ManualFusion() {}

    /**
     * Vrai quand la fusion se fait tout de suite : l'onglet des particules n'est pas affiché (personne ne verrait
     * l'animation), ou une animation de fusion est déjà en cours. Dans ce second cas, avant la version 0.306, la fusion
     * était refusée : les générateurs se reformaient plus vite que le regroupement, et le joueur ne pouvait pas
     * fusionner plus d'une fois par animation.
     */
    static boolean atOnce(boolean particlesShown, boolean animating) { return !particlesShown || animating; }
}
