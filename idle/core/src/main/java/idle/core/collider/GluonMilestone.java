package idle.core.collider;

/**
 * Les paliers de gluons (version 0.151) : chaque quark lié dans un hadron compte un gluon, et certains comptes font
 * garder quelque chose à l'explosion ou au Big Bang. Ils agissent dès qu'ils sont atteints, sans rien payer, et
 * s'ajoutent à ce que l'arbre de matière noire et les paliers de Big Bang gardent déjà.
 */
public enum GluonMilestone {
    UPGRADES(1_000, "Mémoire des liaisons", "L'explosion garde les améliorations payées en particules (pas les générateurs)."),
    AUTOMATIONS(10_000, "Automatismes liés", "L'explosion garde les automatismes, allumés comme avant, et leur cadence."),
    ELEMENTS(50_000, "Éléments liés", "L'explosion garde les éléments uniques du tableau périodique."),
    DARK_TREE(150_000, "Arbre lié", "Le Big Bang garde l'arbre de matière noire."),
    CHALLENGES(400_000, "Défis liés", "Le Big Bang garde les défis de matière noire réussis.");

    private final long gluons;
    private final String label;
    private final String effect;

    GluonMilestone(long gluons, String label, String effect) {
        this.gluons = gluons;
        this.label = label;
        this.effect = effect;
    }

    /** Les gluons qu'il faut : les quarks liés en tout. */
    public long gluons() {
        return gluons;
    }

    public String label() {
        return label;
    }

    /** Ce qu'il fait garder, en une phrase. */
    public String effect() {
        return effect;
    }
}
