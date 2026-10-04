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
            new Upgrade("speed", "Vitesse de création", Resource.PARTICLES,
                    BigNum.of(4), 1.27, Upgrade.NO_LIMIT,
                    new Effect.MultiplySpeed(1.10)),
            // Chaque niveau ajoute un générateur ; le premier est offert au démarrage, d'où le « - 1 ».
            // Coûts : 20, 42, 89, 186, 389, 817, 1 716, 3 603, 7 565.
            // Durée : ×2.0 ≈ 30 min au total, ×2.1 ≈ 37 min, ×2.3 ≈ 1 h.
            new Upgrade("generator", "Nouveau générateur", Resource.PARTICLES,
                    BigNum.of(20), 2.1, MAX_GENERATORS - 1,
                    new Effect.AddGenerator()),

            // ----- Améliorations payées en atomes : définitives, conservées après chaque fusion. -----
            // Toutes multiplient les particules obtenues à chaque création, pas la vitesse :
            // l'animation des générateurs reste lisible.

            // ×2 par niveau. Coûts : 1, 4, 16, 64 atomes.
            new Upgrade("atom_double", "Dédoublement", Resource.ATOMS,
                    BigNum.of(1), 4, Upgrade.NO_LIMIT,
                    new Effect.MultiplyParticles(2)),
            // ×(1 + 5 % par atome créé depuis le début) : ×1,2 à 4 atomes, ×2 à 20, ×6,9 à 118.
            new Upgrade("atom_mass", "Masse atomique", Resource.ATOMS,
                    BigNum.of(2), 1, 1,
                    new Effect.MultiplyByAtoms(0.05)),
            // ×(1 + 0,25 × √minutes depuis la dernière fusion) : ×1,25 à 1 min, ×1,5 à 4 min, ×2 à 16 min.
            new Upgrade("atom_patience", "Patience", Resource.ATOMS,
                    BigNum.of(1), 1, 1,
                    new Effect.MultiplyByRunTime(0.25)),

            // ----- Améliorations en atomes qui agissent sur les améliorations de particules. -----

            // Chaque niveau de « Vitesse de création » gagne 1 point : +10 % devient +11 %, puis +12 %…
            // Coûts : 3, 6, 12, 24, 48 atomes.
            new Upgrade("atom_boost", "Catalyseur", Resource.ATOMS,
                    BigNum.of(3), 2, 5,
                    new Effect.StrengthenSpeed(0.01)),
            // Générateurs 20 % moins chers par niveau : −20 %, −36 %, −49 %, −59 %, −67 %.
            // Coûts : 3, 6, 12, 24, 48 atomes.
            new Upgrade("atom_discount", "Compression", Resource.ATOMS,
                    BigNum.of(3), 2, 5,
                    new Effect.DiscountGenerators(0.8)),
            // La fusion ne remet plus « Vitesse de création » à zéro. Très puissant : une partie
            // ne demande plus que de racheter les générateurs, d'où le coût élevé.
            new Upgrade("atom_keep", "Persistance", Resource.ATOMS,
                    BigNum.of(25), 1, 1,
                    new Effect.KeepUpgradesOnFusion()));

    private Upgrades() {}
}
