package idle.core.probes;

/**
 * La durée d'un voyage de sonde. Plus il est long, plus le butin est riche par heure : c'est la
 * boucle « je pars, je reviens » ; revenir en retard ne fait rien perdre, le butin attend.
 */
public enum ProbeTrip {
    SHORT("Saut court", 15 * 60, 1, 2),
    HOUR("Voyage d'une heure", 60 * 60, 5, 3),
    LONG("Long voyage", 4 * 60 * 60, 24, 4),
    NIGHT("Expédition", 8 * 60 * 60, 56, 5),
    /**
     * L'onde d'urgence chimique (version 0.086) : 20 minutes, un seul petit lot de l'élément déjà
     * découvert que l'astre visé demande le plus ; jamais un élément inconnu. Elle s'ouvre au troisième Big Bang.
     */
    URGENT("Onde d'urgence chimique", 20 * 60, 2, 1);

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

    /** Vrai pour l'onde d'urgence chimique, qui ne part vers aucune destination et rapporte un seul lot. */
    public boolean isUrgent() {
        return this == URGENT;
    }

    /** Les voyages ordinaires, vers une destination, du plus court au plus long. */
    public static java.util.List<ProbeTrip> ordinary() {
        return java.util.List.of(SHORT, HOUR, LONG, NIGHT);
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
