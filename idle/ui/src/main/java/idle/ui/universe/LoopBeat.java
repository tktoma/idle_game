package idle.ui.universe;

/**
 * Le métronome de la boucle : un tic à chaque huitième de tour du point du premier cercle, calé sur la place de sa
 * première zone quand la fenêtre s'ouvre. Version 0.240 : il battait au passage du point sur le centre d'une zone, mais
 * une zone touchée (par le joueur ou le rythme automatique, qui cliquent avant le centre) part ailleurs aussitôt ; le
 * point ne passait presque plus aucun centre, et le métronome se taisait.
 */
final class LoopBeat {
    /** Un tic par huitième de tour (version 0.241 ; un quart avant). */
    static final double STEP = 0.125;

    private LoopBeat() {
    }

    /**
     * Vrai si le point est passé sur un battement entre {@code from} et {@code to} (des phases de 0 à 1, dans le sens
     * du tour), les battements tombant à {@code offset} près d'un multiple de {@link #STEP}. Plus d'un demi-tour depuis
     * l'image d'avant (la fenêtre était fermée) : rien à rattraper.
     */
    static boolean crossed(double from, double to, double offset) {
        if (Double.isNaN(from) || Double.isNaN(to)) return false;
        double move = to - from;
        if (move < 0) move += 1;
        if (!(move > 0 && move < 0.5)) return false;
        // La distance jusqu'au prochain battement, strictement devant : un point posé sur un battement l'a déjà joué.
        double since = (from - offset) / STEP;
        double ahead = (Math.floor(since) + 1 - since) * STEP;
        return ahead <= move + 1e-12;
    }
}
