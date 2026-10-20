package idle.core.universe;

/**
 * Le petit arbre du deuxième univers (version 0.087) : trois choix qui ne se cumulent pas, un seul
 * par cycle, d'un Big Bang au suivant. Le Big Bang le rend, gratuitement : le choix se refait.
 */
public enum CycleChoice {
    FUSION("Fusion", "Chaque fusion donne 25 % d'atomes de plus."),
    SYNTHESIS("Synthèse", "La synthèse coûte 20 % d'atomes de moins."),
    RESERVE("Réserve", "Chaque explosion donne 20 % de matière noire de plus.");

    /** Ce que la fusion donne de plus avec le choix « Fusion ». */
    public static final double FUSION_GAIN = 0.25;
    /** Ce que la synthèse coûte de moins avec le choix « Synthèse ». */
    public static final double SYNTHESIS_SAVING = 0.20;
    /** Ce que l'explosion donne de plus avec le choix « Réserve ». */
    public static final double RESERVE_GAIN = 0.20;

    private final String label;
    private final String effect;

    CycleChoice(String label, String effect) {
        this.label = label;
        this.effect = effect;
    }

    public String label() {
        return label;
    }

    /** Ce que fait le choix, en une phrase. */
    public String effect() {
        return effect;
    }
}
