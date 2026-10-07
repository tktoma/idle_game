package idle.ui;

/**
 * Les confirmations que le joueur a gardées ou retirées dans les réglages, pour les pages qui
 * n'ont pas les réglages sous la main : l'arbre de matière noire (Big Bang) et les défis. Comme
 * le mode détails ({@link Detail}), c'est un état commun à toute l'interface, tenu à jour par
 * {@link Settings}.
 *
 * <p>La confirmation de l'explosion, elle, est lue directement dans les réglages par {@link GameApp}.
 */
final class Confirm {

    private static boolean bigBang = true;
    private static boolean challenge = true;

    private Confirm() {
    }

    static void set(boolean confirmBigBang, boolean confirmChallenge) {
        bigBang = confirmBigBang;
        challenge = confirmChallenge;
    }

    /** Vrai si le Big Bang demande un second clic. */
    static boolean bigBang() {
        return bigBang;
    }

    /** Vrai si commencer ou abandonner un défi demande un second clic. */
    static boolean challenge() {
        return challenge;
    }
}
