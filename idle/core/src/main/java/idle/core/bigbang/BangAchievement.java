package idle.core.bigbang;

/**
 * Les succès du Big Bang (version 0.061), ouverts au premier Big Bang. Ils se déduisent de ce que le
 * troisième acte a bâti : les Big Bangs, les molécules, les astres, la galaxie, les systèmes, les
 * sondes et les comètes. Rien de tout cela ne se perd au Big Bang : un succès gagné le reste, sauf
 * à la remise à zéro. Chacun renforce une grandeur ({@link Reward}).
 */
public enum BangAchievement {
    BANGS_1(Stat.BIG_BANGS, 1, Reward.ATOMS, 0.10, "Premier Big Bang"),
    BANGS_3(Stat.BIG_BANGS, 3, Reward.PARTICLES, 0.25, "Trois Big Bangs"),
    BANGS_5(Stat.BIG_BANGS, 5, Reward.ATOMS, 0.25, "Cinq Big Bangs"),
    BANGS_8(Stat.BIG_BANGS, 8, Reward.DARK_GROWTH, 0.50, "Huit Big Bangs"),
    MOLECULES_100(Stat.MOLECULES, 100, Reward.ATOMS, 0.10, "Cent molécules"),
    MOLECULES_10K(Stat.MOLECULES, 10_000, Reward.SPACE, 0.20, "Dix mille molécules"),
    MOLECULES_1M(Stat.MOLECULES, 1_000_000, Reward.PARTICLES, 0.50, "Un million de molécules"),
    BODIES_5(Stat.BODIES, 5, Reward.ATOMS, 0.15, "Cinq astres"),
    BODIES_15(Stat.BODIES, 15, Reward.DARK_GROWTH, 0.25, "Quinze astres"),
    BODIES_30(Stat.BODIES, 30, Reward.SPACE, 0.30, "Trente astres"),
    GALAXY(Stat.GALAXY, 1, Reward.PARTICLES, 1.00, "Une galaxie"),
    SYSTEMS_3(Stat.SYSTEMS, 3, Reward.ATOMS, 0.25, "Trois systèmes solaires"),
    PROBES_10(Stat.PROBES, 10, Reward.SPACE, 0.15, "Dix voyages de sondes"),
    COMETS_25(Stat.COMETS, 25, Reward.DARK_GROWTH, 0.20, "Vingt-cinq comètes");

    /** Ce qu'il compte. */
    public enum Stat {
        BIG_BANGS("Big Bangs"),
        MOLECULES("molécules créées"),
        BODIES("astres formés"),
        GALAXY("galaxie formée"),
        SYSTEMS("systèmes solaires complets"),
        PROBES("voyages de sondes terminés"),
        COMETS("comètes saisies");

        private final String label;

        Stat(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Ce qu'il renforce. */
    public enum Reward {
        SPACE("de l'espace par seconde"),
        PARTICLES("de particules"),
        ATOMS("d'atomes par fusion"),
        DARK_GROWTH("de croissance de la matière noire");

        private final String label;

        Reward(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Stat stat;
    private final int threshold;
    private final Reward reward;
    private final double amount;
    private final String title;

    BangAchievement(Stat stat, int threshold, Reward reward, double amount, String title) {
        this.stat = stat;
        this.threshold = threshold;
        this.reward = reward;
        this.amount = amount;
        this.title = title;
    }

    public Stat stat() {
        return stat;
    }

    public int threshold() {
        return threshold;
    }

    public Reward reward() {
        return reward;
    }

    /** Ce qu'il ajoute à sa grandeur : 0,25 pour +25 %. */
    public double amount() {
        return amount;
    }

    public String title() {
        return title;
    }
}
