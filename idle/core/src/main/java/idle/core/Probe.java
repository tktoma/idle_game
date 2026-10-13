package idle.core;

/**
 * Une sonde : en vol, ou arrivée et qui attend qu'on ouvre son butin. Elle garde sa place au
 * hangar tant que son butin n'est pas ouvert en entier.
 *
 * @param destination où elle va
 * @param trip        la durée choisie
 * @param left        secondes avant son arrivée ; 0 une fois arrivée
 * @param total       durée de son voyage, moteurs compris
 * @param id          son numéro, qui relie son butin à elle ({@link ProbeReward#probe()})
 */
public record Probe(ProbeDestination destination, ProbeTrip trip, double left, double total, long id) {

    /** Une sonde sans numéro, comme dans les sauvegardes d'avant la version 0.56. */
    public Probe(ProbeDestination destination, ProbeTrip trip, double left, double total) {
        this(destination, trip, left, total, -1);
    }

    /** Vrai une fois arrivée : son butin attend. */
    public boolean landed() {
        return left <= 0;
    }

    /** La part du trajet faite, de 0 au départ à 1 à l'arrivée. */
    public double progress() {
        return total <= 0 ? 1 : Math.max(0, Math.min(1, 1 - left / total));
    }
}
