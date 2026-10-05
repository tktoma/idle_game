package idle.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L'historique d'une partie : des relevés pris à intervalles réguliers, pour tracer des courbes.
 *
 * <p>Sa taille est bornée. Quand il est plein ({@link #CAPACITY} relevés), un relevé sur deux
 * est abandonné et l'intervalle entre deux relevés double : l'historique couvre ainsi toute la
 * partie, qu'elle dure dix minutes ou dix jours, avec une précision qui s'adapte à sa durée.
 *
 * <p>Comme les autres statistiques, il ne sert qu'à l'affichage : aucune règle ne le lit.
 */
public final class StatsHistory {

    /** Nombre maximal de relevés gardés. */
    public static final int CAPACITY = 480;
    /** Intervalle de départ entre deux relevés, en secondes de jeu. */
    public static final double FIRST_INTERVAL = 5;

    /**
     * L'état du jeu à un instant.
     *
     * @param time           instant du relevé, en secondes depuis le début de l'historique
     * @param production     la plus forte production de particules par seconde depuis le relevé
     *                       précédent, en puissance de dix (3 pour 1 000 par seconde). Prendre le
     *                       maximum évite une courbe en dents de scie quand plusieurs fusions,
     *                       qui remettent chacune la production à zéro, tiennent entre deux relevés
     * @param atomsPerFusion atomes que rapporte une fusion, en puissance de dix
     * @param fusionTime     durée de la dernière partie entre deux fusions, en secondes, ou
     *                       {@code NaN} tant qu'il n'y a pas eu de fusion
     * @param elementsShare  part des éléments découverts, de 0 à 1
     * @param copiesShare    part des exemplaires possédés, de 0 à 1
     * @param darkMatterSize taille de la matière noire en mètres, en puissance de dix
     */
    public record Sample(double time, double production, double atomsPerFusion, double fusionTime,
                         double elementsShare, double copiesShare, double darkMatterSize) {}

    private final List<Sample> samples = new ArrayList<>();
    private double interval = FIRST_INTERVAL;
    private double nextTime = 0;
    /** La plus forte production vue depuis le dernier relevé, en puissance de dix. */
    private double peakProduction = Double.NEGATIVE_INFINITY;

    /** Les relevés, du plus ancien au plus récent, en lecture seule. */
    public List<Sample> samples() {
        return Collections.unmodifiableList(samples);
    }

    /** Intervalle actuel entre deux relevés, en secondes de jeu. */
    public double interval() {
        return interval;
    }

    /** Vrai quand il est temps de prendre un nouveau relevé. */
    boolean isDue(double time) {
        return time >= nextTime;
    }

    /** Signale une production atteinte entre deux relevés (juste avant une fusion, par exemple). */
    void notePeak(double production) {
        if (production > peakProduction) peakProduction = production;
    }

    /** La production à inscrire dans le prochain relevé : celle du moment, ou le pic atteint depuis le dernier s'il est plus haut. */
    double peak(double current) {
        return Math.max(current, peakProduction);
    }

    void add(Sample sample) {
        peakProduction = Double.NEGATIVE_INFINITY;
        samples.add(sample);
        if (samples.size() >= CAPACITY) thin();
        nextTime = sample.time() + interval;
    }

    /** Garde un relevé sur deux, et espace les suivants deux fois plus. */
    private void thin() {
        List<Sample> kept = new ArrayList<>(samples.size() / 2 + 1);
        for (int i = 0; i < samples.size(); i += 2) kept.add(samples.get(i));
        samples.clear();
        samples.addAll(kept);
        interval *= 2;
    }

    void clear() {
        samples.clear();
        interval = FIRST_INTERVAL;
        nextTime = 0;
        peakProduction = Double.NEGATIVE_INFINITY;
    }
}
