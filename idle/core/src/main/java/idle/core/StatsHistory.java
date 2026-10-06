package idle.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * L'historique d'une partie : des relevés pris à intervalles réguliers, pour tracer des courbes.
 * Chaque relevé garde une valeur par statistique suivie ({@link Stat}) : en ajouter une à
 * l'énumération, et la mesurer dans {@code Game}, suffit à pouvoir la tracer.
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
     * Ce qu'un relevé retient du jeu : une valeur par statistique, de quoi tracer une courbe pour
     * chacune. Les quantités qui s'étalent sur des dizaines d'ordres de grandeur sont notées en
     * puissance de dix (3 pour 1 000) ; les autres telles quelles.
     */
    public enum Stat {
        // ----- Particules -----
        /**
         * La plus forte production de particules par seconde depuis le relevé précédent, en
         * puissance de dix. Prendre le maximum évite une courbe en dents de scie quand plusieurs
         * fusions, qui remettent chacune la production à zéro, tiennent entre deux relevés.
         */
        PRODUCTION,
        /** Particules créées depuis le début du jeu, en puissance de dix. */
        PARTICLES_CREATED,
        /** Particules dépensées en améliorations depuis le début du jeu, en puissance de dix. */
        PARTICLES_SPENT,
        /** Créations par seconde et par générateur, en puissance de dix. */
        SPEED,
        /** Particules par création, en puissance de dix. */
        PARTICLES_PER_CREATION,
        /** Nombre de générateurs en activité. */
        GENERATORS,
        /** Niveaux de vitesse, offerts compris. */
        SPEED_LEVELS,
        /** Paliers de vitesse atteints. */
        SPEED_MILESTONES,
        /** Niveaux d'améliorations en particules achetés depuis le début du jeu, générateurs compris. */
        PARTICLE_UPGRADES_BOUGHT,
        /** Ce que l'arbre de matière noire multiplie aux particules, en puissance de dix. */
        DARK_PARTICLES,

        // ----- Atomes -----
        /** Atomes que rapporte une fusion, en puissance de dix. */
        ATOMS_PER_FUSION,
        /** Durée de la dernière partie entre deux fusions, en secondes. */
        FUSION_TIME,
        /** Atomes créés depuis le début du jeu, en puissance de dix. */
        ATOMS_CREATED,
        /** Atomes dépensés depuis le début du jeu, en puissance de dix. */
        ATOMS_SPENT,
        /** Fusions depuis le début du jeu. */
        FUSIONS,
        /** Multiplicateur du rendement de fusion (1 sans l'amélioration). */
        FUSION_YIELD,
        /** Niveaux d'améliorations en atomes achetés depuis le début du jeu. */
        ATOM_UPGRADES_BOUGHT,

        // ----- Automatisation -----
        /** Actions faites par tous les automatismes depuis le début du jeu, en puissance de dix. */
        AUTOMATION_ACTIONS,

        // ----- Tableau périodique -----
        /** Part des éléments découverts, de 0 à 1. */
        ELEMENTS_SHARE,
        /** Part des exemplaires possédés, de 0 à 1. */
        COPIES_SHARE,
        /** Synthèses depuis le début du jeu. */
        SYNTHESES,
        /** Prix de la prochaine synthèse en atomes, en puissance de dix. */
        SYNTHESIS_COST,
        /** Chance de tirage double, de 0 à 1. */
        DOUBLE_DRAW,
        /** Ensembles complets. */
        SETS,
        /** Ensembles réunis à moitié, sans être complets. */
        HALF_SETS,
        /** Ce que les éléments multiplient aux particules, en puissance de dix. */
        ELEMENT_PARTICLES,
        /** Ce que les éléments multiplient à la vitesse, en puissance de dix. */
        ELEMENT_SPEED,
        /** Ce que les éléments multiplient aux atomes par fusion, en puissance de dix. */
        ELEMENT_ATOMS,

        // ----- Matière noire -----
        /** Taille de la matière noire en mètres, en puissance de dix. */
        DARK_MATTER_SIZE,
        /** Matière noire gagnée depuis le début du jeu. */
        DARK_MATTER_EARNED,
        /** Explosions depuis le début du jeu. */
        EXPLOSIONS,
        /** Taux de croissance de la matière noire par seconde d'appui (0,02 pour +2 %), en puissance de dix. */
        DARK_GROWTH,
        /** Ce qui freine la croissance de la matière noire, en puissance de dix. */
        DARK_RESISTANCE,
        /** Temps d'appui cumulé sur la matière noire, en secondes. */
        HOLD_TIME,
        /** Paliers de taille atteints. */
        LANDMARKS,

        // ----- Général -----
        /** Succès obtenus. */
        ACHIEVEMENTS,

        // ----- Big Bang -----
        /** Espace créé par l'expansion de la matière, en puissance de dix. Sans valeur avant le premier Big Bang. */
        SPACE,
        /** Espace utilisé par les molécules et leurs lieux de rassemblement, en puissance de dix. */
        SPACE_USED,
        /** Molécules créées. */
        MOLECULES,
        /** Ce que les molécules, les assemblages et les astres multiplient : l'espace, les particules, les atomes, la matière noire, en puissance de dix. */
        MOLECULE_SPACE,
        MOLECULE_PARTICLES,
        MOLECULE_ATOMS,
        MOLECULE_DARK,
        /** Assemblages et astres formés, galaxie comprise. */
        SKY
    }

    /**
     * L'état du jeu à un instant.
     *
     * <p>Une valeur qui n'a pas encore de sens vaut {@code NaN} : la durée d'une partie avant la
     * première fusion, le délai d'un automatisme qu'on ne possède pas. La courbe s'interrompt là.
     *
     * @param time   instant du relevé, en secondes depuis le début de l'historique
     * @param values une valeur par {@link Stat}, rangées dans l'ordre de l'énumération
     * @param delays délai entre deux actions de chaque automatisme possédé, en secondes, par
     *               identifiant d'automatisme
     */
    public record Sample(double time, double[] values, Map<String, Double> delays) {

        /** La valeur d'une statistique dans ce relevé. */
        public double value(Stat stat) {
            return values[stat.ordinal()];
        }

        /** Le délai d'un automatisme dans ce relevé, en secondes, ou {@code NaN} s'il n'était pas possédé. */
        public double delay(String automationId) {
            return delays.getOrDefault(automationId, Double.NaN);
        }

        public double production() {
            return value(Stat.PRODUCTION);
        }

        public double atomsPerFusion() {
            return value(Stat.ATOMS_PER_FUSION);
        }

        public double fusionTime() {
            return value(Stat.FUSION_TIME);
        }

        public double elementsShare() {
            return value(Stat.ELEMENTS_SHARE);
        }

        public double copiesShare() {
            return value(Stat.COPIES_SHARE);
        }

        public double darkMatterSize() {
            return value(Stat.DARK_MATTER_SIZE);
        }
    }

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
