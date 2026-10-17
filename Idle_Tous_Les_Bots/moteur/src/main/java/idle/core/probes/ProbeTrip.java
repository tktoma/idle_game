package idle.core.probes;

/**
 * La durée d'un voyage de sonde. Plus il est long, plus le butin est riche par heure : c'est la
 * boucle « je pars, je reviens » ; revenir en retard ne fait rien perdre, le butin attend.
 */
public enum ProbeTrip {
    SHORT("Saut court", 15 * 60, 1, 2),
    HOUR("Voyage d'une heure", 60 * 60, 5, 3),
    LONG("Long voyage", 4 * 60 * 60, 24, 4),
    NIGHT("Expédition", 8 * 60 * 60, 56, 5);

    private final String label;
    private final double seconds;
    private final double value;
    private final int rewards;

    ProbeTrip(String label, double seconds, double value, int rewards) {
        this.label = label;
        this.seconds = seconds;
        this.value = value;
        this.rewards = rewards;
    }

    public String label() {
        return label;
    }

    /** Durée du voyage, avant les moteurs. */
    public double seconds() {
        return seconds;
    }

    /** Ce que vaut chaque récompense rapportée, à multiplier par les instruments : 1 pour un saut court. */
    public double value() {
        return value;
    }

    /** Nombre de récompenses rapportées, avant la soute. */
    public int rewards() {
        return rewards;
    }
}
