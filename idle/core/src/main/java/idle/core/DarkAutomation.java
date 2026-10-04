package idle.core;

/**
 * Description d'un automatisme de matière noire : il fait à la place du joueur ce que les
 * automatismes ordinaires ({@link Automation}) ne font pas. Donnée pure ; ce qui est en marche
 * est dans {@link GameState}.
 *
 * <p>Il ne s'achète pas : il se débloque quand le joueur possède assez de matière noire, sans
 * la dépenser, et reste débloqué. Le joueur le met en marche ou le coupe librement ; il est
 * coupé au départ, parce que chacun dépense ou détruit quelque chose que le joueur peut vouloir garder.
 *
 * @param id         identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name       nom affiché au joueur
 * @param kind       ce que fait l'automatisme
 * @param darkMatter matière noire qu'il faut posséder pour le débloquer
 * @param interval   délai entre deux actions, en secondes
 */
public record DarkAutomation(String id, String name, Kind kind, BigNum darkMatter, double interval) {

    /** Ce que fait un automatisme de matière noire. */
    public enum Kind {
        /**
         * Fait ce que les automatismes ordinaires ne font pas encore : achète les améliorations
         * payées en particules, fusionne et synthétise, tant que l'automatisme ordinaire
         * correspondant n'est pas en marche.
         */
        PARTICLE_UPGRADES,
        /** Achète les améliorations payées en atomes, la moins chère d'abord. */
        ATOM_UPGRADES,
        /** Achète les automatismes ordinaires et leurs niveaux de cadence, le moins cher d'abord. */
        AUTOMATIONS,
        /** Fait exploser le tableau périodique dès que c'est possible. */
        EXPLOSION
    }

    public DarkAutomation {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (kind == null) throw new IllegalArgumentException("type manquant");
        if (darkMatter.sign() < 0) throw new IllegalArgumentException("Seuil de matière noire négatif");
        if (!(interval > 0)) throw new IllegalArgumentException("Le délai doit être positif");
    }
}
