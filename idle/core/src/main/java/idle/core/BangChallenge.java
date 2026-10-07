package idle.core;

/**
 * Un défi de Big Bang : refaire tout le chemin jusqu'au Big Bang suivant sous une contrainte. Ils
 * s'ouvrent une fois tous les paliers atteints ({@link Game#isBangChallengesUnlocked()}), quand
 * un Big Bang de plus ne rapporte presque plus rien : chacun laisse une récompense qui reste.
 *
 * <p>Commencer un défi remet la partie à zéro comme le ferait un Big Bang, sans en compter un
 * ({@link Game#startBangChallenge}). La contrainte tient jusqu'au Big Bang suivant, qui réussit le
 * défi et compte normalement. On peut l'abandonner à tout moment : la contrainte tombe, rien n'est gagné.
 */
public enum BangChallenge {
    /** La matière ne compte plus : molécules, amas, assemblages, astres et cosmos ne multiplient rien. */
    VOID("Univers vide", "La matière ne multiplie plus rien", "La matière ne donne plus rien : ni les molécules, ni les amas, ni les assemblages, ni les astres, "
            + "ni la galaxie. Restent les améliorations d'espace, les collections et les paliers.",
            Reward.SPACE, "Espace par seconde ×2"),
    /** Les paliers de Big Bang qui multiplient quelque chose n'agissent plus. */
    FORGOTTEN("Sans mémoire", "Les paliers ne multiplient plus rien", "Les paliers de Big Bang ne multiplient plus rien : matière noire des explosions, espace, "
            + "attraction des amas, énergie sombre. Ce qu'ils gardent ou automatisent reste.",
            Reward.ACCRETION, "Les amas attirent ×1.5"),
    /** Aucun automatisme de matière noire ni du troisième acte. */
    BY_HAND("À la main", "Ni automatisme de matière noire, ni du Big Bang", "Aucun automatisme de matière noire, et aucun de ceux du Big Bang : appui, création, "
            + "rassemblement et formation automatiques. Les automatismes ordinaires restent.",
            Reward.DARK_MATTER, "Matière noire des explosions ×2");

    /** Ce que laisse un défi de Big Bang réussi. */
    public enum Reward { SPACE, ACCRETION, DARK_MATTER }

    private final String label;
    private final String brief;
    private final String rule;
    private final Reward reward;
    private final String rewardText;

    BangChallenge(String label, String brief, String rule, Reward reward, String rewardText) {
        this.label = label;
        this.brief = brief;
        this.rule = rule;
        this.reward = reward;
        this.rewardText = rewardText;
    }

    /** Nom affiché. */
    public String label() {
        return label;
    }

    /** La contrainte, en quelques mots : la ligne de sa carte. */
    public String brief() {
        return brief;
    }

    /** La contrainte, en une ou deux phrases. */
    public String rule() {
        return rule;
    }

    public Reward reward() {
        return reward;
    }

    /** La récompense, en quelques mots : « Espace par seconde ×2 ». */
    public String rewardText() {
        return rewardText;
    }
}
