package idle.ui;

/**
 * La forme d'une galaxie spirale, vue de face : où va un point d'un disque quand ce disque
 * s'enroule en bras.
 *
 * <p>Un point est repéré par sa distance au centre, en parts du rayon (0 au centre, 1 au bord), et
 * par son angle. La galaxie a {@link #ARMS} bras, qui tournent de {@link #TWIST} du centre au bord.
 * Au centre, dans le bulbe, rien ne bouge ; au-delà, chaque point est tiré vers le bras le plus
 * proche, d'autant plus qu'il en était près : les bras sont denses en leur milieu, et il reste
 * entre eux des couloirs presque vides. Aucun point ne change de distance au centre, et deux
 * points voisins restent dans le même ordre : la galaxie tient dans le disque de départ.
 */
final class Spiral {

    static final int ARMS = 2;
    /** L'angle dont un bras tourne du centre au bord, en radians : un peu plus d'un tour. */
    static final double TWIST = 2.2 * Math.PI;
    /** Jusqu'où va le bulbe, où rien n'est déplacé, et d'où les bras sont tout à fait formés, en parts du rayon. */
    private static final double BULGE = 0.12;
    private static final double ARMS_FROM = 0.32;
    /** Ce qu'il reste de l'écart d'un point au milieu de son bras, tout près de ce milieu : en dessous de 1, les bras se resserrent. */
    private static final double SQUEEZE = 0.3;

    private Spiral() {}

    /** L'angle du milieu du premier bras à cette distance du centre. Les autres bras suivent, à parts égales du tour. */
    static double arm(double share) {
        return TWIST * share;
    }

    /** À quel point les bras sont resserrés à cette distance : 1 dans le bulbe, {@link #SQUEEZE} une fois les bras formés. */
    static double squeeze(double share) {
        double along = Math.max(0, Math.min(1, (share - BULGE) / (ARMS_FROM - BULGE)));
        along = along * along * (3 - 2 * along);
        return 1 + (SQUEEZE - 1) * along;
    }

    /** Le nouvel angle d'un point qui était à cette distance et à cet angle : tiré vers le bras le plus proche. */
    static double bend(double share, double angle) {
        double period = 2 * Math.PI / ARMS;
        double off = angle - arm(share);
        double nearest = Math.rint(off / period);
        double delta = off - nearest * period;
        double tight = squeeze(share);
        // Près du milieu du bras, l'écart est réduit à sa part resserrée ; au milieu du couloir, il ne change pas.
        double part = delta / (period / 2);
        return arm(share) + nearest * period + delta * (tight + (1 - tight) * part * part);
    }
}
