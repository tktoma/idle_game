package idle.core;

import java.util.List;

/**
 * Un palier de Big Bang : un gros bonus acquis d'office au premier, au deuxième, puis à chaque Big
 * Bang jusqu'au cinquième ({@link BigBangMilestones}) ; un même Big Bang peut en donner deux. Rien à acheter : il suffit d'avoir déclenché
 * assez de Big Bangs ({@link Game#isBigBangMilestoneReached(BigBangMilestone)}), et seule la remise
 * à zéro du jeu le reprend.
 *
 * <p>Recommencer coûte cher : tout repart d'un seul générateur, et le tableau périodique reste
 * vide plusieurs heures. Les paliers sont ce qui paie ce détour.
 *
 * @param bigBangs nombre de Big Bangs qu'il faut avoir déclenchés
 * @param name     nom affiché
 * @param effects  ce qu'il donne : un effet, parfois deux
 */
public record BigBangMilestone(int bigBangs, String name, List<Effect> effects) {

    /**
     * Ce que donne un palier.
     *
     * <p>Interface scellée : pour ajouter une sorte d'effet, on ajoute un record ici, puis le
     * compilateur signale les {@code switch} à compléter.
     */
    public sealed interface Effect {}

    /** Multiplie une grandeur du jeu : l'espace par seconde, les atomes, les particules ({@link Game#bigBangMilestoneBoost(Molecule.Stat)}). */
    public record Boost(Molecule.Stat stat, double factor) implements Effect {
        public Boost {
            if (stat == null) throw new IllegalArgumentException("Palier sans grandeur");
            if (!(factor > 1)) throw new IllegalArgumentException("Facteur invalide : " + factor);
        }
    }

    /**
     * Multiplie la matière noire que laisse chaque explosion ({@link Game#darkMatterPerExplosion()}) :
     * il en faut autant qu'avant pour l'arbre et pour le Big Bang suivant, donc moitié moins d'explosions.
     */
    public record DarkMatter(double factor) implements Effect {
        public DarkMatter {
            if (!(factor > 1)) throw new IllegalArgumentException("Facteur invalide : " + factor);
        }
    }

    /** Multiplie ce qu'un amas attire à chaque création ({@link Game#moleculesPerCreation(String)}). */
    public record Accretion(double factor) implements Effect {
        public Accretion {
            if (!(factor > 1)) throw new IllegalArgumentException("Facteur invalide : " + factor);
        }
    }

    /**
     * Fait dépendre l'expansion de la matière noire en réserve : l'espace par seconde est multiplié
     * par {@code 1 + perRoot × √(matière noire)} ({@link Game#darkMatterSpaceBoost()}). La matière
     * noire sert ainsi encore une fois l'arbre fini : chaque explosion de plus accélère l'expansion,
     * et la dépenser la ralentit. Un Big Bang la reprend, et ce bonus avec elle, jusqu'à ce qu'elle revienne.
     *
     * @param perRoot ce qu'ajoute chaque unité de la racine carrée de la matière noire (0.1 = +10 %)
     */
    public record DarkMatterSpace(double perRoot) implements Effect {
        public DarkMatterSpace {
            if (!(perRoot > 0)) throw new IllegalArgumentException("Part invalide : " + perRoot);
        }
    }

    /**
     * Donne l'automatisme « Création automatique » : à intervalle régulier, une création dans chaque
     * amas que le joueur a choisi ({@link Game#setMoleculeAutomated(String, boolean)}), tant que le
     * tableau périodique et l'espace le permettent. Il se règle dans l'onglet Automatisation.
     */
    public record AutoMolecules() implements Effect {}

    /**
     * L'arbre de matière noire traverse le Big Bang ({@link Game#bigBangKeepsDarkTree()}) : ses
     * cases, ses automatismes et leurs réglages restent, avec la matière noire déjà dépensée, celle
     * qui ouvre les cases et les défis. Le Big Bang ne reprend plus que la réserve, la taille, la
     * masse du tableau et les défis réussis. Cela vaut dès le Big Bang qui atteint le palier : c'est
     * lui qui s'en trouve raccourci, et tous les suivants.
     */
    public record KeepDarkTree() implements Effect {}

    public BigBangMilestone {
        if (bigBangs <= 0) throw new IllegalArgumentException("Un palier demande au moins un Big Bang");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Palier sans nom");
        if (effects == null || effects.isEmpty()) throw new IllegalArgumentException("Palier sans effet : " + name);
        effects = List.copyOf(effects);
    }
}
