package idle.core;

import java.util.List;

/**
 * Catalogue des améliorations du jeu.
 *
 * <p>Équilibrage de la phase 1 (du premier au dixième générateur), réglé par simulation :
 * <ul>
 *   <li>environ 35 à 40 minutes de jeu actif pour débloquer le dixième générateur ;</li>
 *   <li>premier achat possible après 16 secondes, deuxième générateur avant 4 minutes ;</li>
 *   <li>une quarantaine d'achats, sans attente de plus de 3 minutes entre deux ;</li>
 *   <li>environ 5 particules par seconde et par générateur à la fin, pour que
 *       l'animation reste lisible.</li>
 * </ul>
 * Pour allonger ou raccourcir la phase, voir les deux lignes commentées « durée ».
 * Le test {@code GameTest.Simulation} vérifie que la durée reste dans la fourchette voulue.
 */
public final class Upgrades {

    /** Nombre maximal de générateurs, le premier compris. */
    public static final int MAX_GENERATORS = 10;

    public static final List<Upgrade> DEFAULT = List.of(
            // Chaque niveau rend la création 10 % plus rapide, sur tous les générateurs à la fois.
            // Le coût monte plus vite (+27 %) que le gain (+10 %) : les achats s'espacent peu à peu.
            // Durée : 1.25 ≈ 30 min au total, 1.27 ≈ 37 min, 1.30 ≈ 1 h (avec le coût ×2.3 ci-dessous).
            new Upgrade("speed", "Vitesse de création",
                    BigNum.of(4), 1.27, Upgrade.NO_LIMIT,
                    new Effect.MultiplySpeed(1.10)),
            // Chaque niveau ajoute un générateur ; le premier est offert au démarrage, d'où le « - 1 ».
            // Coûts : 20, 42, 89, 186, 389, 817, 1 716, 3 603, 7 565.
            // Durée : ×2.0 ≈ 30 min au total, ×2.1 ≈ 37 min, ×2.3 ≈ 1 h.
            new Upgrade("generator", "Nouveau générateur",
                    BigNum.of(20), 2.1, MAX_GENERATORS - 1,
                    new Effect.AddGenerator()));

    private Upgrades() {}
}