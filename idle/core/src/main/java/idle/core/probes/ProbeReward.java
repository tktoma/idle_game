package idle.core.probes;

/**
 * Une récompense rapportée par une sonde, en attente d'être réclamée d'un clic. Son montant se
 * calcule au moment du clic, d'après sa valeur et les améliorations du moment.
 *
 * @param type        sa sorte
 * @param target      l'élément (son numéro) ou le quark (son nom) ; vide sinon
 * @param value       sa valeur : celle du voyage, instruments compris
 * @param destination d'où elle vient
 * @param probe       le numéro de la sonde qui la rapporte ({@link Probe#id()}), -1 si elle n'en a pas
 */
public record ProbeReward(Type type, String target, double value, ProbeDestination destination, long probe) {

    /** Une récompense sans sonde, comme dans les sauvegardes d'avant la version 0.056. */
    public ProbeReward(Type type, String target, double value, ProbeDestination destination) {
        this(type, target, value, destination, -1);
    }

    public enum Type {
        /** Des exemplaires d'un élément déjà découvert. */
        ELEMENTS,
        /** De l'espace : quelques minutes d'expansion d'un coup. */
        SPACE,
        /** Un sursaut : l'espace par seconde doublé un moment. */
        BOOST,
        /** Des échantillons, qui paient les améliorations des sondes. */
        SAMPLES,
        /** Des quarks charmés et beaux, avec le palier du charme. */
        QUARKS
    }
}
