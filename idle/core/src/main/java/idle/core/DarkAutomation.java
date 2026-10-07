package idle.core;

/**
 * Description d'un automatisme de matière noire : il fait à la place du joueur ce que les
 * automatismes ordinaires ({@link Automation}) ne font pas. Aucun ne refait le travail d'un
 * automatisme ordinaire : le premier les offre, les autres achètent ou font exploser. Donnée
 * pure ; ce qui est en marche est dans {@link GameState}.
 *
 * <p>Il ne s'achète pas : il se débloque quand le joueur possède assez de matière noire, sans
 * la dépenser, et reste débloqué. Le joueur le met en marche ou le coupe librement ; il est
 * coupé au départ, parce que chacun dépense ou détruit quelque chose que le joueur peut vouloir garder.
 *
 * @param id         identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name       nom affiché au joueur
 * @param kind       ce que fait l'automatisme
 * @param darkMatter matière noire qu'il faut avoir gagnée pour le débloquer
 * @param interval   délai entre deux actions, en secondes
 */
public record DarkAutomation(String id, String name, Kind kind, BigNum darkMatter, double interval) {

    /** Ce que fait un automatisme de matière noire. */
    public enum Kind {
        /**
         * Offre les automatismes ordinaires au lieu de faire leur travail : ceux qui achètent et
         * fusionnent dès le début de chaque partie, sans attendre le seuil d'atomes créés ni
         * payer ; la synthèse automatique une fois les autres à leur cadence maximale et le
         * tableau périodique ouvert, sans attendre un élément unique.
         */
        GRANT_AUTOMATIONS,
        /** Achète les améliorations payées en atomes, la moins chère d'abord. */
        ATOM_UPGRADES,
        /** Achète les automatismes ordinaires et leurs niveaux de cadence, le moins cher d'abord. */
        AUTOMATIONS,
        /** Fait exploser le tableau périodique dès que c'est possible. */
        EXPLOSION,
        /**
         * Rachète l'arbre de matière noire, la case la moins chère d'abord, et les améliorations
         * payées en matière noire qui ont un nombre de niveaux limité. Il ne vient qu'après un
         * premier Big Bang : c'est en refaisant l'arbre qu'il sert.
         */
        DARK_TREE
    }

    public DarkAutomation {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (kind == null) throw new IllegalArgumentException("type manquant");
        if (darkMatter.sign() < 0) throw new IllegalArgumentException("Seuil de matière noire négatif");
        if (!(interval > 0)) throw new IllegalArgumentException("Le délai doit être positif");
    }
}
