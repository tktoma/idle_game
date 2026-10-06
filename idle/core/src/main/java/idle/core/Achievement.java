package idle.core;

import java.util.function.Predicate;

/**
 * Description d'un succès : un but atteint ou une action précise réussie, noté une fois pour
 * toutes. Donnée pure ; les succès obtenus sont dans {@link GameState}, et aucune explosion ne
 * les reprend.
 *
 * <p>Chaque succès obtenu ajoute un petit bonus de particules, le même pour tous
 * ({@link Game#ACHIEVEMENT_PARTICLES}). Ceux qui récompensent une action précise ont en plus un
 * petit bonus à eux.
 *
 * @param id          identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name        nom affiché au joueur
 * @param summary     ce qu'il faut faire, en trois ou quatre mots, pour l'afficher sur une tuile
 * @param description ce qu'il faut faire pour l'obtenir, en une phrase complète
 * @param bonus       ce que le succès apporte en propre, ou {@link Bonus#NONE}
 * @param amount      taille de ce bonus (0.05 = 5 %), 0 sans bonus propre
 * @param condition   ce qui doit être vrai pour l'obtenir, vérifié régulièrement ; {@code null}
 *                    pour un succès que le jeu accorde au moment de l'action elle-même
 */
public record Achievement(String id, String name, String summary, String description, Bonus bonus, double amount,
                          Predicate<Game> condition) {

    /** Ce qu'un succès peut apporter en propre. Les bonus d'une même sorte s'additionnent. */
    public enum Bonus {
        /** Rien de plus que le bonus commun à tous les succès. */
        NONE,
        /** Particules de chaque création. */
        PARTICLES,
        /** Atomes de chaque fusion. */
        ATOMS,
        /** Cadence des automatismes ordinaires. */
        AUTOMATION,
        /** Prix de la synthèse, réduit. */
        SYNTHESIS_COST,
        /** Prix des générateurs, réduit. */
        GENERATOR_COST,
        /** Croissance de la matière noire. */
        EXPANSION
    }

    public Achievement {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("résumé manquant : " + id);
        if (bonus == null) throw new IllegalArgumentException("bonus manquant");
        if (amount < 0) throw new IllegalArgumentException("Bonus négatif");
        if ((bonus == Bonus.NONE) != (amount == 0)) {
            throw new IllegalArgumentException("Un bonus propre a une taille, et réciproquement : " + id);
        }
    }

    /** Succès d'étape : un but atteint, sans bonus propre. */
    public static Achievement step(String id, String name, String summary, String description, Predicate<Game> condition) {
        return new Achievement(id, name, summary, description, Bonus.NONE, 0, condition);
    }

    /** Succès d'action, avec son petit bonus, vérifié régulièrement. */
    public static Achievement feat(String id, String name, String summary, String description, Bonus bonus, double amount,
                                   Predicate<Game> condition) {
        return new Achievement(id, name, summary, description, bonus, amount, condition);
    }

    /** Succès d'action que le jeu accorde au moment où l'action a lieu. */
    public static Achievement feat(String id, String name, String summary, String description, Bonus bonus, double amount) {
        return new Achievement(id, name, summary, description, bonus, amount, null);
    }

    /** Vrai pour un succès qui a un bonus à lui, en plus du bonus commun. */
    public boolean hasBonus() {
        return bonus != Bonus.NONE;
    }
}
