package idle.core.collider;

/**
 * Les paliers du collisionneur : des seuils de hadrons qui changent le collisionneur lui-même ou
 * la route vers la galaxie. Ils se déduisent des hadrons formés, qui ne se perdent jamais : un
 * palier atteint le reste.
 */
public enum ColliderMilestone {
    /** 10 protons et 10 neutrons : la chance de collision monte de cinq points. */
    PLASMA("Plasma quark-gluon", "10 protons et 10 neutrons",
            "Chance de collision à chaque croisement : 20 % au lieu de 15 %"),
    /** 5 hadrons étranges (kaons et lambdas) : une collision sur quatre donne un quark de plus. */
    STRANGE("Matière étrange", "5 hadrons étranges (kaons et lambdas)", "Une collision sur quatre donne un quark de plus"),
    /** 3 J/psi : les sondes rapportent aussi des quarks charmés et beaux. */
    CHARM("Charme", "3 J/psi", "Les sondes rapportent aussi des quarks c et b"),
    /** 1 upsilon : le cœur de galaxie, que demande le trou noir supermassif. */
    CORE("Cœur de galaxie", "1 upsilon", "Le trou noir supermassif peut se former : la galaxie en dépend");

    private final String label;
    private final String condition;
    private final String reward;

    ColliderMilestone(String label, String condition, String reward) {
        this.label = label;
        this.condition = condition;
        this.reward = reward;
    }

    public String label() {
        return label;
    }

    /** Ce qu'il demande, en mots. */
    public String condition() {
        return condition;
    }

    /** Ce qu'il donne, en mots. */
    public String reward() {
        return reward;
    }
}
