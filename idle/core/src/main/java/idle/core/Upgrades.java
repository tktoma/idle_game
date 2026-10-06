package idle.core;

import java.util.List;

/**
 * Catalogue des améliorations du jeu.
 *
 * <p>Équilibrage de la phase 1 (du premier au dixième générateur), réglé par simulation :
 * <ul>
 *   <li>environ 35 à 40 minutes de jeu actif pour débloquer le dixième générateur (37 en
 *       achetant tout dès que possible, 32 en choisissant bien entre vitesse, couplage et
 *       générateurs) ;</li>
 *   <li>premier achat possible après 16 secondes, deuxième générateur avant 4 minutes ;</li>
 *   <li>une cinquantaine d'achats, sans attente de plus d'une minute et demie entre deux ;</li>
 *   <li>le premier palier de vitesse (niveau 25) tombe vers la 28ᵉ minute ;</li>
 *   <li>environ 5 créations par seconde et par générateur à la fin, pour que l'animation reste
 *       lisible : c'est pour cela que les paliers et le couplage multiplient les particules,
 *       pas la vitesse.</li>
 * </ul>
 * Pour allonger ou raccourcir la phase, voir les deux lignes commentées « durée ».
 * Le test {@code GameTest.Simulation} vérifie que la durée reste dans la fourchette voulue.
 *
 * <p>La suite (atomes, automatismes, tableau périodique) a été réglée de la même façon, en
 * simulant plusieurs ordres d'achat sur toute la progression, avec huit tirages de hasard chacun.
 * La première explosion, celle qui donne la première matière noire, demande <b>environ un jour
 * de jeu</b> à un joueur qui optimise : 24 h en moyenne, de 21 h 30 à 27 h 20 selon la chance. Un
 * joueur qui achète sans réfléchir met plutôt 27 h 30 à 30 h. Ces durées comptent les succès
 * qui tombent en chemin (une trentaine) et leurs bonus. Repères du meilleur ordre d'achat trouvé :
 * 30 atomes créés (automatisation et tableau périodique débloqués, {@link Game#UNLOCK_TOTAL_ATOMS})
 * vers 3 h, Persistance vers 4 h 25, les cinq automatismes à leur cadence maximale vers 9 h 35,
 * premier élément unique (et synthèse automatique) vers 10 h 15, premier ensemble à moitié réuni
 * vers 13 h, 30 éléments vers 17 h 15, premier ensemble complet vers 20 h 45, la moitié du
 * tableau vers 21 h 40, les 118 éléments vers 23 h 50, puis tous portés à leur maximum
 * d'exemplaires (811 en tout) en une dizaine de minutes.
 *
 * <p>Ce meilleur ordre : Dédoublement, Patience, Masse atomique, un second Dédoublement, un
 * Catalyseur et une Compression avant les 30 atomes ; puis l'automatisme des générateurs, celui
 * de la fusion, deux niveaux de cadence pour les générateurs, Persistance, l'automatisme de la
 * vitesse, et le reste au moins cher, Surcharge comprise : l'acheter plus tôt ou plus tard ne
 * déplace la fin que d'une vingtaine de minutes.
 *
 * <p>Le début (premier atome à 37 minutes) est resté celui de la version courte : c'est la
 * partie où le joueur clique. Ce sont les phases automatisées qui durent, par la cadence des
 * automatismes ({@link Automation#DEFAULT_INTERVAL}), le Rendement, et les éléments qui
 * accélèrent la synthèse elle-même (voir {@link PeriodicTable}).
 *
 * <p>Attention au coût de la vitesse : chaque niveau rapporte ×1,10, jusqu'à ×1,25 avec le
 * Catalyseur et les éléments, et les paliers ajoutent ×2 tous les 25 niveaux, soit ×1,028 par
 * niveau en moyenne. Le coût doit monter plus vite que ce total (1,25 × 1,028 = 1,285), sinon
 * chaque achat rembourse le suivant et la production s'emballe : d'où 1,31.
 */
public final class Upgrades {

    /** Nombre maximal de générateurs, le premier compris. */
    public static final int MAX_GENERATORS = 10;

    /** Écart entre deux paliers de « Vitesse de création », en niveaux. */
    public static final int SPEED_MILESTONE_EVERY = 25;

    public static final List<Upgrade> DEFAULT = List.of(
            // Chaque niveau rend la création 10 % plus rapide, sur tous les générateurs à la fois.
            // Le coût monte plus vite (+31 %) que le gain (+10 %) : les achats s'espacent peu à peu.
            // Durée : 1.29 ≈ 34 min au total, 1.31 ≈ 37 min, 1.32 ≈ 39 min (avec le coût ×2.3 ci-dessous).
            // Palier : tous les 25 niveaux, les particules de chaque création doublent. C'est une
            // raison de pousser quelques niveaux de plus avant de fusionner.
            new Upgrade("speed", "Vitesse de création", Resource.PARTICLES,
                    BigNum.of(4), 1.31, Upgrade.NO_LIMIT,
                    new Effect.MultiplySpeed(1.10, SPEED_MILESTONE_EVERY, 2)),
            // Chaque niveau : +2 % de particules par autre générateur. Sans effet avec un seul
            // générateur, +18 % par niveau avec les dix : il vaut mieux l'acheter tard dans la partie.
            // Coûts : 60, 90, 135, 203, 304, 456…
            new Upgrade("coupling", "Couplage", Resource.PARTICLES,
                    BigNum.of(60), 1.5, Upgrade.NO_LIMIT,
                    new Effect.MultiplyByGenerators(0.02)),
            // Chaque niveau ajoute un générateur ; le premier est offert au démarrage, d'où le « - 1 ».
            // Coûts : 20, 46, 106, 244, 560, 1 288, 2 961, 6 810, 15 663.
            // Durée : ×2.2 ≈ 34 min au total, ×2.3 ≈ 37 min, ×2.4 ≈ 41 min.
            new Upgrade("generator", "Nouveau générateur", Resource.PARTICLES,
                    BigNum.of(20), 2.3, MAX_GENERATORS - 1,
                    new Effect.AddGenerator()),

            // ----- Améliorations payées en atomes : définitives, conservées après chaque fusion. -----
            // Toutes multiplient les particules obtenues à chaque création, pas la vitesse :
            // l'animation des générateurs reste lisible.

            // ×2 par niveau. Coûts : 1, 4, 16, 64 atomes.
            new Upgrade("atom_double", "Dédoublement", Resource.ATOMS,
                    BigNum.of(1), 4, Upgrade.NO_LIMIT,
                    new Effect.MultiplyParticles(2)),
            // ×(1 + 5 % par atome créé depuis le début, jusqu'à 118) : ×1,2 à 4 atomes, ×2 à 20, ×6,9 au maximum.
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
            // La fusion ne remet plus « Vitesse de création » à zéro. Elle ne débloque plus rien :
            // l'automatisation et le tableau périodique s'ouvrent à 30 atomes créés
            // (Game.UNLOCK_TOTAL_ATOMS). À 25 atomes au lieu de 8, c'est un vrai choix : l'acheter avant
            // ce cap le retarde d'une heure et demie ; le mieux est de la prendre juste après les
            // premiers automatismes, vers 4 h 40 de jeu.
            new Upgrade("atom_keep", "Persistance", Resource.ATOMS,
                    BigNum.of(25), 1, 1,
                    new Effect.KeepUpgradesOnFusion()),

            // ----- Le puits d'atomes. -----

            // ×1,3 de particules par niveau, sans limite de niveau. Le prix ne monte que de 20 % par
            // niveau (12, 15, 18, 21, 25, 30, 36, 43, 52, 62, 75, 90, 107) pour rester payable sous
            // le plafond de 118 atomes : treize niveaux pendant le tableau périodique, la suite une
            // fois le plafond levé. Chaque niveau se pèse contre une synthèse.
            new Upgrade("atom_overload", "Surcharge", Resource.ATOMS,
                    BigNum.of(12), 1.2, Upgrade.NO_LIMIT,
                    new Effect.Overload(1.3)),

            // ----- Le lien entre les particules et les atomes. -----

            // +9 % d'atomes par fusion chaque fois que la production est multipliée par dix, à partir
            // de cinquante millions de particules par seconde. Sans lui, les particules ne serviraient
            // plus à rien une fois les parties automatisées ; avec lui, tout ce qui augmente la
            // production (vitesse et ses paliers, couplage, surcharge, éléments, ensembles, succès)
            // finit par rapporter des atomes.
            new Upgrade("atom_yield", "Rendement", Resource.ATOMS,
                    BigNum.of(40), 1, 1,
                    new Effect.MultiplyAtomsByProduction(50_000_000, 0.09)));

    private Upgrades() {}
}
