package idle.core.darkmatter;

/**
 * Description d'un défi d'explosion : une partie rejouée avec une contrainte, de son premier
 * générateur jusqu'à l'explosion du tableau périodique. Donnée pure ; le défi en cours et ceux
 * qui sont réussis sont dans {@link GameState}.
 *
 * <p>Commencer un défi efface la partie en cours sans rien rapporter, et suspend les mémoires
 * de la matière noire le temps du défi (voir {@link Game#startChallenge(String)}). Le réussir, c'est faire exploser le tableau malgré la contrainte : l'explosion
 * donne sa matière noire comme d'habitude, et le défi laisse en plus une récompense définitive.
 *
 * @param id         identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name       nom affiché au joueur
 * @param rule       la contrainte, tant que le défi est en cours
 * @param darkMatter matière noire qu'il faut avoir gagnée pour pouvoir le tenter ; elle n'est pas dépensée
 * @param reward     ce que le défi laisse une fois réussi
 */
public record Challenge(String id, String name, Rule rule, int darkMatter, Reward reward) {

    /** La contrainte d'un défi. */
    public enum Rule {
        /** Tous les automatismes, ordinaires et de matière noire, sont quatre fois plus lents, jamais sous une demi-seconde. */
        RUSTY_AUTOMATIONS,
        /** Il faut deux fois plus de générateurs pour fusionner : vingt au lieu de dix. */
        HEAVY_FUSION,
        /** Les particules s'évaporent : la moitié de celles que possède le joueur disparaît chaque seconde. */
        VOLATILE_PARTICLES,
        /** Chaque fusion remet aussi à zéro les améliorations payées en atomes. */
        AMNESIA,
        /** Les éléments et leurs ensembles ne donnent aucun bonus. */
        INERT_ELEMENTS,
        /** Les éléments se désintègrent : chaque exemplaire a une demi-vie de 35 secondes. */
        DECAY,
        /** Version 0.070 : les voies d'absence de l'arbre n'agissent plus, ni l'accord d'absence. Seules comptent celles de présence. */
        PRESENCE_ONLY,
        /** Version 0.070 : les voies de présence n'agissent plus, ni l'accord de présence. Seules comptent celles d'absence. */
        ABSENCE_ONLY,
        /** Version 0.070 : la branche des particules de l'arbre n'agit plus ; restent les atomes, la taille et les liaisons. */
        CUT_BRANCH,
        /** Version 0.070 : aucune voie, aucune liaison n'agit : il ne reste que le tronc de l'arbre. */
        BARE_TREE,
        /**
         * Version 0.115 : la fuite, du deuxième univers. Les particules fuient de plus en plus vite tant que le
         * joueur n'achète aucune amélioration ; il faut deux minutes au moins avant d'exploser.
         */
        LEAK
    }

    /** La récompense définitive d'un défi réussi. */
    public enum Reward {
        /** Délai des automatismes ordinaires divisé par {@link Game#REWARD_AUTOMATION_DIVISOR}. */
        FASTER_AUTOMATIONS,
        /** La prime de groupe de la fusion passe à {@link Game#REWARD_GROUP_BONUS} par groupe. */
        GROUP_BONUS,
        /** Les paliers de vitesse multiplient les particules par un de plus : ×3 au lieu de ×2. */
        STRONGER_MILESTONES,
        /** Améliorations payées en atomes {@link Game#REWARD_ATOM_UPGRADE_DIVISOR} fois moins chères. */
        CHEAPER_ATOM_UPGRADES,
        /** Tous les éléments agissent {@link Game#REWARD_ELEMENT_STRENGTH} fois plus fort. */
        STRONGER_ELEMENTS,
        /** Un élément de plus à chaque synthèse. */
        EXTRA_ELEMENT,
        /** Les voies de présence comptent double. */
        STRONGER_PRESENCE,
        /** Les voies d'absence comptent double, et ce qu'elles rendent du temps d'absence aussi. */
        STRONGER_ABSENCE,
        /** Les liaisons et les accords comptent double. */
        STRONGER_LINKS,
        /** Une matière noire de plus à chaque explosion. */
        EXTRA_DARK_MATTER,
        /** Version 0.115 : le calme du Big Bang ({@link Game#bigBangCalmRelief()}). */
        BIG_BANG_CALM
    }

    public Challenge {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (rule == null) throw new IllegalArgumentException("contrainte manquante");
        if (reward == null) throw new IllegalArgumentException("récompense manquante");
        if (darkMatter < 1) throw new IllegalArgumentException("Un défi demande au moins une matière noire");
    }
}
