package idle.core;

/**
 * Ce que fait une amélioration de matière noire, par niveau acheté.
 *
 * <p>Interface scellée, comme {@link Effect} : pour ajouter un type d'effet, on ajoute un
 * record ici et le compilateur signale les {@code switch} à compléter.
 */
public sealed interface DarkEffect {

    // ----- Branche des atomes -----

    /** Ajoute {@code perLevel} atomes à ce que rapporte une fusion, avant tous les multiplicateurs. */
    record AddAtomsPerFusion(double perLevel) implements DarkEffect {}

    /** Chaque synthèse tire {@code perLevel} éléments de plus. */
    record AddElementsPerSynthesis(int perLevel) implements DarkEffect {}

    /** L'explosion ne détruit plus les automatismes : ils restent achetés, à leur cadence, en marche ou coupés. */
    record KeepAutomationsOnExplosion() implements DarkEffect {}

    /**
     * L'explosion ne remet plus les améliorations à zéro, qu'elles soient payées en particules ou
     * en atomes. Les générateurs, eux, sont toujours perdus.
     */
    record KeepUpgradesOnExplosion() implements DarkEffect {}

    /** Sans élément unique, la synthèse n° {@code rank} en donne un à coup sûr (au lieu de la huitième). */
    record EarlierGuaranteedUnique(int rank) implements DarkEffect {
        public EarlierGuaranteedUnique {
            if (rank < 1) throw new IllegalArgumentException("Rang invalide : " + rank);
        }
    }

    /** L'explosion ne détruit plus les éléments uniques déjà obtenus : la synthèse automatique reste débloquée. */
    record KeepUniqueElementsOnExplosion() implements DarkEffect {}

    /** L'explosion devient possible dès que tous les éléments sont découverts, sans attendre tous les exemplaires. */
    record ExplodeWhenDiscovered() implements DarkEffect {}

    /**
     * Repousse le maximum d'exemplaires des éléments non uniques : leur force maximale (la racine
     * du nombre d'exemplaires) gagne {@code perLevel} par niveau. Un maximum de 9 passe à 16, puis 25.
     */
    record IncreaseMaxCopies(int perLevel) implements DarkEffect {}

    // ----- Branche des particules -----

    /** Multiplie par {@code perLevel} les particules de base de chaque création, à chaque niveau. */
    record MultiplyBaseParticles(double perLevel) implements DarkEffect {}

    /**
     * Lève la limite du nombre de générateurs, qui passe à {@code limit}. Une fusion consomme
     * alors tous les générateurs et rapporte autant de fois ses atomes qu'il y a de groupes
     * complets : avec 20 générateurs au lieu de 10, deux fois plus.
     */
    record UncapGenerators(int limit) implements DarkEffect {
        public UncapGenerators {
            if (limit < 1) throw new IllegalArgumentException("Limite invalide : " + limit);
        }
    }

    /** Après une fusion ou une explosion, la partie repart avec {@code perLevel} générateurs de plus par niveau. */
    record StartingGenerators(int perLevel) implements DarkEffect {}

    /**
     * Multiplie les particules par la taille de la matière noire, comptée en années-lumière et
     * élevée à la puissance {@code exponent} (2 = au carré). Sans effet en dessous d'une année-lumière.
     */
    record ParticlesByLightYears(double exponent) implements DarkEffect {}

    /**
     * Débloque le réglage du seuil de fusion : la fusion automatique attend d'avoir le nombre de
     * générateurs choisi par le joueur ({@link Game#setFusionThreshold(int)}).
     */
    record FusionThreshold() implements DarkEffect {}

    /**
     * Offre {@code perLevel} niveaux de vitesse par niveau : ils s'ajoutent à ceux achetés, ne
     * coûtent rien et ne se perdent jamais.
     */
    record StartingSpeedLevels(int perLevel) implements DarkEffect {}

    /** Multiplie les particules par {@code perUnit} pour chaque matière noire possédée (2 = ×2 chacune). */
    record ParticlesByDarkMatter(double perUnit) implements DarkEffect {}

    /** Abaisse le délai minimal des automatismes à {@code minInterval} secondes. */
    record FasterAutomations(double minInterval) implements DarkEffect {
        public FasterAutomations {
            if (!(minInterval > 0)) throw new IllegalArgumentException("Délai invalide : " + minInterval);
        }
    }

    // ----- Branche de la taille -----

    /** Multiplie par {@code perLevel} la vitesse de croissance de la matière noire, à chaque niveau. */
    record MultiplyExpansion(double perLevel) implements DarkEffect {}

    /**
     * La matière noire grossit toute seule, sans appui, à une fraction {@code share} de la
     * vitesse d'appui (0.001 = 0,1 %) au premier niveau, multipliée par {@code growth} à chaque
     * niveau suivant.
     */
    record AutoExpansion(double share, double growth) implements DarkEffect {}

    /** Le joueur peut verrouiller l'appui d'un clic, au lieu de garder le bouton enfoncé. L'effet est dans l'interface. */
    record HoldLock() implements DarkEffect {}

    /** Chaque fusion fait grossir la matière noire comme un appui de {@code seconds} secondes. */
    record FusionPulse(double seconds) implements DarkEffect {}

    /** Chaque explosion laisse {@code perLevel} matière noire de plus par niveau. */
    record AddDarkMatterPerExplosion(double perLevel) implements DarkEffect {}

    /** Lève le plafond d'atomes ({@link Game#MAX_ATOMS}) : on peut en posséder autant qu'on veut. */
    record UncapAtoms() implements DarkEffect {}

    // ----- Améliorations payées en matière noire : leur effet grandit avec la matière noire gagnée -----

    /**
     * Multiplie les atomes de chaque fusion par {@code 1 + perUnit × niveau × matière noire gagnée}
     * (0.2 = +20 % par matière noire et par niveau).
     */
    record AtomsByDarkMatter(double perUnit) implements DarkEffect {}

    /**
     * Accélère tous les automatismes ordinaires : leur délai est divisé par
     * {@code 1 + perUnit × niveau × matière noire gagnée}.
     */
    record AutomationsByDarkMatter(double perUnit) implements DarkEffect {}

    /** Divise le prix de la synthèse par {@code 1 + perUnit × niveau × matière noire gagnée}. */
    record SynthesisByDarkMatter(double perUnit) implements DarkEffect {}

    /**
     * Chaque partie démarre avec des atomes déjà comptés comme créés : {@code perUnit} par
     * matière noire gagnée et par niveau, sans dépasser le seuil qui débloque l'automatisation et
     * le tableau périodique ({@link Game#UNLOCK_TOTAL_ATOMS}). Ce sont des atomes « créés », pas
     * des atomes à dépenser.
     */
    record HeadStartByDarkMatter(double perUnit) implements DarkEffect {}
}
