package idle.core.universe;

/**
 * Les épreuves du deuxième univers (version 0.88) : facultatives, une à la fois, et leur seul gain
 * est un skin. Chacune dit sa contrainte et son but ; une épreuve à durée limitée échoue au bout de
 * son temps, les autres attendent leur but tant qu'on ne les abandonne pas.
 *
 * @param skin    le skin qu'elle donne
 * @param label   son nom
 * @param rule    sa contrainte et son but, en une phrase
 * @param seconds sa durée, en secondes de jeu ; 0 sans limite
 * @param goal    le nombre à atteindre (croisements captés…), quand il y en a un
 */
public enum Trial {
    LONE_GENERATOR(Skin.FUSION_LONE, "Premier générateur seulement",
            "Un seul générateur pour toute la partie, mais le couplage compte comme si les dix étaient là, deux fois. "
                    + "But : 1e12 particules en main.", 0, 0),
    SOFT_CONTRACTION(Skin.DARK_CONTRACTED, "Contraction douce",
            "Toutes les 10 s, 2 % des particules s'en vont ; chaque fusion du cycle donne 5 % d'atomes de plus, jusqu'à ×2. "
                    + "But : exploser.", 0, 0),
    COLD_BANG(Skin.BANG_CRYSTAL, "Big Bang à froid",
            "L'expansion de la matière va deux fois moins vite. But : le Big Bang suivant.", 0, 0),
    DETECTOR(Skin.BEAM_GOLD, "Défi du détecteur",
            "En 60 s, capter cinq croisements de suite, sans en laisser passer un.", 60, 5),
    NO_OVERHEAT(Skin.COLLIDER_NEON, "Sans surchauffe",
            "En 3 min, capter dix croisements, sans surchauffe pendant l'épreuve.", 180, 10),
    FULL_CHARGE(Skin.COLLIDER_PLASMA, "Pleine charge",
            "En 2 min, produire l'énergie de 5 minutes au rythme du départ : il faut capter les croisements.", 120, 0),
    LONE_COLLIDER(Skin.COLLIDER_ICE, "Un seul générateur",
            "En 2 min, la chaîne du collisionneur arrêtée, capter huit croisements.", 120, 8);

    /** Particules à avoir en main pour l'épreuve du premier générateur seul. */
    public static final double LONE_GOAL_LOG10 = 12;
    /** Part des particules qui s'en va, et tous les combien de secondes, pendant la contraction douce. */
    public static final double CONTRACTION_LOSS = 0.02;
    public static final double CONTRACTION_EVERY = 10;
    /** Ce que chaque fusion de la contraction ajoute aux atomes, et le plafond. */
    public static final double CONTRACTION_FUSION_GAIN = 0.05;
    public static final double CONTRACTION_MAX = 2;
    /** Ce par quoi le Big Bang à froid divise l'expansion. */
    public static final double COLD_EXPANSION = 0.5;
    /** Secondes de production d'énergie à réunir pour la pleine charge. */
    public static final double FULL_CHARGE_SECONDS = 300;

    private final Skin skin;
    private final String label;
    private final String rule;
    private final double seconds;
    private final int goal;

    Trial(Skin skin, String label, String rule, double seconds, int goal) {
        this.skin = skin;
        this.label = label;
        this.rule = rule;
        this.seconds = seconds;
        this.goal = goal;
    }

    public Skin skin() {
        return skin;
    }

    public String label() {
        return label;
    }

    public String rule() {
        return rule;
    }

    /** Sa durée en secondes de jeu, 0 sans limite. */
    public double seconds() {
        return seconds;
    }

    /** Le nombre à atteindre, 0 quand le but n'est pas un compte. */
    public int goal() {
        return goal;
    }

    /** Vrai pour les épreuves du collisionneur. */
    public boolean isCollider() {
        return this == DETECTOR || this == NO_OVERHEAT || this == FULL_CHARGE || this == LONE_COLLIDER;
    }
}
