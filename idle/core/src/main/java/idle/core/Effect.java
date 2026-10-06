package idle.core;

/**
 * Ce que fait une amélioration, par niveau acheté.
 *
 * <p>Interface scellée : la liste des effets possibles est fermée, donc le
 * compilateur signale tout {@code switch} qui oublierait un cas. Pour ajouter
 * un nouveau type d'effet, on ajoute un record ici et on le traite dans {@link Game}.
 */
public sealed interface Effect {

    /**
     * Multiplie la vitesse de création de chaque générateur par {@code perLevel} à chaque niveau.
     *
     * <p>Avec des paliers ({@code milestoneEvery > 0}), tous les {@code milestoneEvery} niveaux
     * les particules de chaque création sont en plus multipliées par {@code milestoneFactor}.
     * Le palier agit sur les particules et non sur la vitesse : l'animation des générateurs
     * reste lisible.
     */
    record MultiplySpeed(double perLevel, int milestoneEvery, double milestoneFactor) implements Effect {
        public MultiplySpeed {
            if (milestoneEvery < 0) throw new IllegalArgumentException("Écart entre paliers négatif");
            if (milestoneFactor < 1) throw new IllegalArgumentException("Un palier ne peut pas réduire la production");
        }

        /** Sans palier. */
        public MultiplySpeed(double perLevel) {
            this(perLevel, 0, 1);
        }

        public boolean hasMilestones() {
            return milestoneEvery > 0 && milestoneFactor > 1;
        }

        /** Nombre de paliers atteints avec {@code level} niveaux. */
        public int milestonesAt(int level) {
            return hasMilestones() ? Math.max(0, level) / milestoneEvery : 0;
        }
    }

    /**
     * Chaque générateur renforce les autres : multiplie les particules de chaque création par
     * {@code 1 + perGenerator × niveau × (générateurs − 1)}. Sans effet avec un seul générateur,
     * au plus fort juste avant la fusion.
     */
    record MultiplyByGenerators(double perGenerator) implements Effect {
        public MultiplyByGenerators {
            if (perGenerator < 0) throw new IllegalArgumentException("Bonus négatif");
        }
    }

    /** Ajoute un générateur par niveau, qui forme ses particules en parallèle des autres. */
    record AddGenerator() implements Effect {}

    /** Multiplie les particules obtenues à chaque création par {@code perLevel} à chaque niveau. */
    record MultiplyParticles(double perLevel) implements Effect {}

    /**
     * Multiplie les particules par {@code perLevel} à chaque niveau, comme
     * {@link MultiplyParticles}, mais sans qu'aucun élément ne vienne le renforcer : c'est le
     * puits d'atomes de la fin de partie, il doit rester prévisible.
     */
    record Overload(double perLevel) implements Effect {
        public Overload {
            if (perLevel < 1) throw new IllegalArgumentException("La surcharge ne peut pas réduire la production");
        }
    }

    /**
     * Multiplie les particules par {@code 1 + perAtom × atomes créés depuis le début du jeu}.
     * Le bonus grandit donc à chaque fusion, même si les atomes ont été dépensés.
     */
    record MultiplyByAtoms(double perAtom) implements Effect {}

    /**
     * Multiplie les particules par {@code 1 + factor × √minutes écoulées depuis la dernière fusion}.
     * Le bonus repart de ×1 à chaque fusion et monte de moins en moins vite.
     */
    record MultiplyByRunTime(double factor) implements Effect {}

    /**
     * Renforce les améliorations de vitesse : chacun de leurs niveaux multiplie la vitesse par
     * {@code extraPerLevel} de plus. Avec 0.01, un niveau qui donnait ×1.10 donne ×1.11.
     */
    record StrengthenSpeed(double extraPerLevel) implements Effect {}

    /** Multiplie le coût des générateurs par {@code factorPerLevel} à chaque niveau (0.8 = −20 %). */
    record DiscountGenerators(double factorPerLevel) implements Effect {}

    /**
     * La fusion ne remet plus à zéro les améliorations payées en particules.
     * Les générateurs, eux, sont toujours consommés : ce sont eux qui fusionnent.
     */
    record KeepUpgradesOnFusion() implements Effect {}

    /**
     * Augmente les atomes gagnés à la fusion selon la production de particules : {@code +perDecade}
     * chaque fois que la production par seconde est multipliée par dix au-delà de {@code threshold}
     * (0.5 = +50 %). Sans effet sous le seuil.
     * C'est ce qui donne un intérêt aux particules une fois les parties automatisées.
     */
    record MultiplyAtomsByProduction(double threshold, double perDecade) implements Effect {
        public MultiplyAtomsByProduction {
            if (!(threshold > 0)) throw new IllegalArgumentException("Le seuil doit être positif");
            if (perDecade < 0) throw new IllegalArgumentException("Bonus négatif");
        }
    }
}
