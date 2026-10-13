package idle.core;

/** Les améliorations des sondes, payées en échantillons. Elles ne se perdent pas au Big Bang. */
public enum ProbeUpgrade {
    LOOT("Instruments", "Chaque récompense vaut 25 % de plus", 10, 10, 1.8),
    ENGINE("Moteurs ioniques", "Les voyages durent 10 % de moins", 5, 15, 2),
    BOOST("Réserves d'énergie", "Les sursauts d'expansion durent 50 % de plus", 6, 12, 1.8),
    CARGO("Soute", "Une récompense de plus par voyage", 3, 30, 3),
    HANGAR("Hangar", "Une sonde de plus en vol", 2, 40, 4);

    private final String label;
    private final String effect;
    private final int maxLevel;
    private final double baseCost;
    private final double growth;

    ProbeUpgrade(String label, String effect, int maxLevel, double baseCost, double growth) {
        this.label = label;
        this.effect = effect;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.growth = growth;
    }

    public String label() {
        return label;
    }

    /** Ce que fait chaque niveau, en mots. */
    public String effect() {
        return effect;
    }

    public int maxLevel() {
        return maxLevel;
    }

    /** Prix du niveau suivant, quand {@code level} sont déjà achetés. */
    public long cost(int level) {
        return Math.round(baseCost * Math.pow(growth, level));
    }
}
