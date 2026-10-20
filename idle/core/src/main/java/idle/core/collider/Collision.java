package idle.core.collider;

/**
 * Une collision lancée avant la version 0.059, quand on les achetait une à une : lue dans une
 * ancienne sauvegarde, elle se termine aussitôt et donne son butin.
 *
 * @param tier      son énergie
 * @param left      secondes de jeu qu'il lui restait
 * @param resonated vrai si le joueur avait déjà tenté la résonance
 * @param doubled   vrai si la résonance avait réussi : le butin est doublé
 */
public record Collision(CollisionTier tier, double left, boolean resonated, boolean doubled) {
}
