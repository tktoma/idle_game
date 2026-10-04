package idle.core;

import java.util.List;

/**
 * Catalogue des améliorations du jeu.
 *
 * <p>Contenu PROVISOIRE : deux exemples pour faire tourner le moteur.
 * À remplacer par les vraies améliorations quand les spécifications seront fixées.
 */
public final class Upgrades {

    public static final List<Upgrade> DEFAULT = List.of(
            new Upgrade("condenser", "Condenseur",
                    BigNum.of(10), 1.15,
                    new Effect.AddProduction(BigNum.ONE)),
            new Upgrade("accelerator", "Accélérateur",
                    BigNum.of(1000), 10,
                    new Effect.MultiplyProduction(2)));

    private Upgrades() {}
}
