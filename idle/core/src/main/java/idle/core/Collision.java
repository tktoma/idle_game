package idle.core;

/**
 * Une collision en cours dans le collisionneur.
 *
 * @param tier      son énergie
 * @param left      secondes de jeu avant qu'elle ne se termine
 * @param resonated vrai si le joueur a déjà tenté la résonance (une seule tentative par collision)
 * @param doubled   vrai si la résonance a réussi : le butin sera doublé
 */
public record Collision(CollisionTier tier, double left, boolean resonated, boolean doubled) {

    /** Temps déjà écoulé depuis le lancement. */
    public double elapsed() {
        return tier.seconds() - left;
    }

    /** Avancée de 0 à 1. */
    public double progress() {
        return Math.min(1, Math.max(0, elapsed() / tier.seconds()));
    }
}
