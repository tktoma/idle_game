package idle.ui.common;

/**
 * Le calcul du défilement à la molette (version 0.307), sans JavaFX pour se tester seul : chaque cran avance
 * {@link #SPEED} fois plus loin que le défilement de base de JavaFX, qui demandait beaucoup de crans pour traverser une
 * longue page (l'arbre de matière noire, les succès, les réglages).
 */
public final class WheelScroll {
    /** Combien de pixels de page chaque pixel de molette fait défiler. */
    public static final double SPEED = 3;

    private WheelScroll() {}

    /**
     * La nouvelle position (de 0 en haut à 1 en bas) d'une page de {@code content} pixels vue par une fenêtre de
     * {@code viewport} pixels, après un cran de molette de {@code deltaY} pixels (positif vers le haut, comme JavaFX).
     * Une page qui tient dans sa fenêtre ne bouge pas.
     */
    public static double next(double value, double deltaY, double content, double viewport) {
        double travel = content - viewport;
        if (travel <= 0 || deltaY == 0) return value;
        return Math.max(0, Math.min(1, value - deltaY * SPEED / travel));
    }

    /** Vrai si la page peut encore défiler dans le sens de ce cran : sinon, c'est à la page qui l'entoure de bouger. */
    public static boolean canMove(double value, double deltaY, double content, double viewport) {
        if (content - viewport <= 0 || deltaY == 0) return false;
        return deltaY > 0 ? value > 0 : value < 1;
    }

    /** La position que donne un clic sur le rail, à {@code y} pixels de son haut sur {@code height} : on y saute. */
    public static double jump(double y, double height) {
        return height <= 0 ? 0 : Math.max(0, Math.min(1, y / height));
    }
}
