package idle.ui;

import idle.core.Game;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Le journal des événements : les cent dernières annonces du jeu, avec l'heure et le temps de jeu
 * où elles sont tombées. Une notification disparaît en quelques secondes ; le journal la garde,
 * que les notifications soient affichées ou coupées.
 *
 * <p>Il vit le temps d'un lancement : il n'est pas écrit dans la sauvegarde. Toutes les pages
 * lisent le même ({@link #of(Game)}) ; c'est {@link GameApp} qui l'alimente, la sous-page
 * « Journal » des statistiques qui le montre.
 */
final class EventLog {

    /** Nombre d'annonces gardées : au-delà, les plus anciennes s'effacent. */
    static final int CAPACITY = 100;
    private static final Map<Game, EventLog> SHARED = new WeakHashMap<>();

    /**
     * Une annonce du journal.
     *
     * @param clock    l'heure de l'horloge, « 14:32 »
     * @param played   le temps de jeu à ce moment, en secondes
     * @param text     l'annonce, telle que la notification l'a dite
     */
    record Entry(String clock, double played, String text) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    private int version = 0;

    private EventLog() {
    }

    /** Le journal de cette partie : le même objet pour toutes les pages. */
    static EventLog of(Game game) {
        synchronized (SHARED) {
            return SHARED.computeIfAbsent(game, each -> new EventLog());
        }
    }

    /** Note une annonce, à l'heure qu'il est. */
    void add(Game game, String text) {
        LocalTime now = LocalTime.now();
        add(String.format("%02d:%02d", now.getHour(), now.getMinute()), game.state().timePlayed(), text);
    }

    /** Note une annonce à une heure donnée : pour les vérifications. */
    void add(String clock, double played, String text) {
        if (text == null || text.isBlank()) return;
        entries.addFirst(new Entry(clock, played, text));
        while (entries.size() > CAPACITY) entries.removeLast();
        version++;
    }

    /** Les annonces gardées, de la plus récente à la plus ancienne. */
    List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    int size() {
        return entries.size();
    }

    /** Change à chaque annonce : la page ne se recompose que lorsqu'il a bougé. */
    int version() {
        return version;
    }

    /** Vide le journal : la partie vient d'être remplacée ou remise à zéro. */
    void clear() {
        entries.clear();
        version++;
    }
}
