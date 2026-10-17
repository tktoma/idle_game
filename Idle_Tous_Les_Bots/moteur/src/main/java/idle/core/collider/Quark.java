package idle.core.collider;

/** Les six saveurs de quarks que donne le collisionneur, de la plus courante à la plus rare. */
public enum Quark {
    UP("haut", "u"),
    DOWN("bas", "d"),
    STRANGE("étrange", "s"),
    CHARM("charmé", "c"),
    BOTTOM("beau", "b"),
    /** Le quark top ne se garde pas : il se désintègre une minute après son arrivée. */
    TOP("top", "t");

    private final String label;
    private final String symbol;

    Quark(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    /** « étrange », pour « quark étrange ». */
    public String label() {
        return label;
    }

    /** La lettre du quark : u, d, s, c, b, t. */
    public String symbol() {
        return symbol;
    }
}
