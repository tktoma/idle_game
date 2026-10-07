package idle.core;

import java.util.List;

/**
 * Catalogue des succès : soixante-six. Les cinquante premiers vont du premier générateur à la
 * dixième explosion ; vingt et un d'entre eux récompensent une action précise et portent un petit
 * bonus à eux. Les seize derniers jalonnent le troisième acte, de la première molécule à l'univers,
 * et trois d'entre eux portent un bonus.
 *
 * <p>Tous donnent en plus le bonus commun, +1 % de particules chacun. Les bonus propres sont
 * volontairement petits : réunis, ceux qu'on peut obtenir avant la première explosion valent
 * +2 % d'atomes, −1,5 % sur le prix de la synthèse et +2 % de cadence. Assez pour avoir envie de
 * les chercher, trop peu pour décider de la durée d'une partie.
 *
 * <p>Le catalogue est rangé dans l'ordre où un joueur les obtient d'ordinaire, de la première
 * particule à la dixième explosion : c'est l'ordre dans lequel l'interface les affiche.
 *
 * <p>Les noms sont provisoires : seuls les identifiants comptent pour le jeu.
 */
public final class Achievements {

    // Les succès accordés par le jeu au moment de l'action : Game les désigne par ces identifiants.
    public static final String BARE_FUSION = "feat_bare_fusion";
    public static final String PATIENT_FUSION = "feat_patient_fusion";
    public static final String FAST_FUSION = "feat_fast_fusion";
    public static final String MASS_FUSION = "feat_mass_fusion";
    public static final String BULK_PURCHASE = "feat_bulk_purchase";
    public static final String BUY_EVERYTHING = "feat_buy_everything";
    public static final String EXACT_CHANGE = "feat_exact_change";
    public static final String DOUBLE_DRAW = "feat_double_draw";
    public static final String TRIPLE_DRAW = "feat_triple_draw";
    public static final String TARGETED = "feat_targeted";
    public static final String TARGETED_ACTINIDE = "feat_targeted_actinide";
    public static final String HOLD_LOCK = "feat_hold_lock";
    public static final String FAST_EXPLOSION = "feat_fast_explosion";
    public static final String ABANDON = "feat_abandon";

    /** Durée d'attente, en secondes, que récompense le succès de la fusion patiente. */
    public static final double PATIENT_SECONDS = 30 * 60;
    /** Durée, en secondes, sous laquelle une partie entre deux fusions compte comme un sprint. */
    public static final double FAST_FUSION_SECONDS = 30;
    /** Durée, en secondes, sous laquelle une explosion compte comme rapide. */
    public static final double FAST_EXPLOSION_SECONDS = 10 * 60;
    /** Niveaux à acheter d'un seul coup pour le succès de l'achat groupé. */
    public static final int BULK_LEVELS = 25;
    /** Niveaux que « tout acheter » doit prendre d'un coup pour son succès. */
    public static final int BUY_EVERYTHING_LEVELS = 20;
    /** Groupes de générateurs à fusionner d'un coup pour le succès de la fusion de masse. */
    public static final int MASS_FUSION_GROUPS = 5;
    /** Prix minimal, en atomes, de l'achat qui doit vider exactement la réserve du joueur. */
    public static final double EXACT_CHANGE_COST = 20;

    public static final List<Achievement> DEFAULT = List.of(
            // ----- La première heure -----
            Achievement.step("step_first_particle", "Première étincelle", "1 particule",
                    "Créer une première particule.",
                    game -> game.state().stats().particlesCreated().gte(BigNum.ONE)),
            Achievement.step("step_milestone", "Premier palier", "1 palier de vitesse",
                    "Atteindre un palier de vitesse : il double les particules.",
                    game -> game.speedMilestones() >= 1),
            Achievement.step("step_all_generators", "Au complet", "Tous les générateurs",
                    "Réunir tous les générateurs qu'il faut pour fusionner.",
                    Game::hasAllGenerators),
            Achievement.step("step_first_atom", "Premier atome", "1 fusion",
                    "Fusionner les générateurs en un atome.",
                    game -> game.state().stats().fusions() >= 1),
            Achievement.feat(PATIENT_FUSION, "Patience d'ange", "Fusionner après 30 min",
                    "Fusionner plus de 30 minutes après la fusion précédente.", Achievement.Bonus.PARTICLES, 0.02),
            Achievement.step("step_atom_upgrade", "Premier investissement", "1 amélioration en atomes",
                    "Acheter une amélioration en atomes.",
                    game -> game.state().stats().atomUpgradesBought() >= 1),
            Achievement.feat(BARE_FUSION, "Mains nues", "Fusionner sans vitesse",
                    "Fusionner sans un seul niveau de vitesse ni de couplage.", Achievement.Bonus.PARTICLES, 0.02),

            // ----- Jusqu'aux automatismes (1 h à 5 h) -----
            Achievement.feat(BULK_PURCHASE, "Achat en gros", "25 niveaux d'un clic",
                    "Acheter 25 niveaux d'une amélioration d'un seul clic.", Achievement.Bonus.GENERATOR_COST, 0.02),
            Achievement.feat(BUY_EVERYTHING, "Razzia", "« Tout acheter » : 20 niveaux",
                    "Acheter au moins 20 niveaux d'un coup avec « tout acheter ».", Achievement.Bonus.GENERATOR_COST, 0.02),
            Achievement.step("step_unlock", "Trente atomes", "30 atomes créés",
                    "Créer assez d'atomes pour ouvrir l'automatisation et le tableau périodique.",
                    game -> game.state().totalAtoms().gte(Game.UNLOCK_TOTAL_ATOMS)),
            Achievement.step("step_automation", "Premier automatisme", "1 automatisme",
                    "Posséder un automatisme.",
                    game -> !game.state().ownedAutomations().isEmpty()),
            Achievement.feat(EXACT_CHANGE, "Compte rond", "Payer 20 atomes pile",
                    "Payer une amélioration d'au moins 20 atomes avec exactement ce que vous avez.",
                    Achievement.Bonus.ATOMS, 0.005),
            Achievement.step("step_keep", "Mémoire longue", "Persistance",
                    "Acheter l'amélioration qui garde la vitesse d'une fusion à l'autre.",
                    Game::keepsUpgradesOnFusion),
            Achievement.step("step_fusions_100", "Cent fusions", "100 fusions",
                    "Fusionner cent fois.",
                    game -> game.state().stats().fusions() >= 100),

            // ----- La mise en place des automatismes (5 h à 10 h) -----
            Achievement.step("step_production_6", "Un million par seconde", "1e6 particules / s",
                    "Produire un million de particules par seconde.",
                    game -> game.productionPerSecond().gte(BigNum.of(1, 6))),
            Achievement.feat(FAST_FUSION, "Sprint", "2 fusions en 30 s",
                    "Fusionner moins de 30 secondes après la fusion précédente.", Achievement.Bonus.AUTOMATION, 0.01),
            Achievement.step("step_automations", "Tout seul", "Tous les automatismes",
                    "Posséder tous les automatismes.",
                    game -> !game.automations().isEmpty()
                            && game.state().ownedAutomations().size() == game.automations().size()),
            Achievement.step("step_yield", "Rendement", "Rendement",
                    "Acheter l'amélioration qui change la production en atomes.",
                    game -> owns(game, Effect.MultiplyAtomsByProduction.class, 1)),
            Achievement.step("step_element", "Premier élément", "1 élément",
                    "Synthétiser un élément.",
                    game -> game.discoveredElements() >= 1),
            Achievement.step("step_unique", "Pièce unique", "1 élément unique ★",
                    "Obtenir un élément unique.",
                    Game::isSynthesisAutomationUnlocked),
            Achievement.feat("feat_full_atoms", "Plein à craquer", "118 atomes en poche",
                    "Posséder le maximum d'atomes.", Achievement.Bonus.ATOMS, 0.005,
                    game -> game.state().atoms().gte(Game.MAX_ATOMS)),

            // ----- Le tableau périodique (10 h à 24 h) -----
            Achievement.feat("feat_reserve", "Bas de laine", "Réserve de 50 atomes tenue",
                    "Avoir de côté une réserve d'au moins 50 atomes pendant que la synthèse automatique tourne.",
                    Achievement.Bonus.ATOMS, 0.005,
                    game -> game.synthesisReserve().gte(BigNum.of(50)) && game.state().atoms().gte(game.synthesisReserve())
                            && game.automations().stream().anyMatch(automation ->
                                    automation.kind() == Automation.Kind.SYNTHESIS && game.isAutomationEnabled(automation.id()))),
            Achievement.step("step_cadence", "Cadence infernale", "Cadences au maximum",
                    "Porter tous les automatismes à leur cadence maximale.",
                    game -> !game.automations().isEmpty() && game.automations().stream()
                            .allMatch(automation -> game.ownsAutomation(automation.id()) && game.isAutomationMaxed(automation.id()))),
            Achievement.feat(DOUBLE_DRAW, "Coup double", "1 tirage double",
                    "Obtenir un tirage double à la synthèse.", Achievement.Bonus.SYNTHESIS_COST, 0.005),
            Achievement.step("step_half_set", "Début de collection", "Un demi-ensemble",
                    "Réunir la moitié d'un ensemble.",
                    game -> game.halfSets() + game.completedSets() >= 1),
            Achievement.feat("feat_water", "De l'eau", "Hydrogène + oxygène",
                    "Posséder de l'hydrogène et de l'oxygène.", Achievement.Bonus.AUTOMATION, 0.01,
                    game -> game.elementCount(1) >= 1 && game.elementCount(8) >= 1),
            Achievement.feat("feat_salt", "Pincée de sel", "Sodium + chlore",
                    "Posséder du sodium et du chlore.", Achievement.Bonus.ATOMS, 0.005,
                    game -> game.elementCount(11) >= 1 && game.elementCount(17) >= 1),
            Achievement.step("step_overload", "Sous tension", "Surcharge niveau 10",
                    "Porter la Surcharge au niveau 10.",
                    game -> owns(game, Effect.Overload.class, 10)),
            Achievement.feat("feat_gold", "Pierre philosophale", "De l'or",
                    "Synthétiser de l'or.", Achievement.Bonus.PARTICLES, 0.02,
                    game -> game.elementCount(79) >= 1),
            Achievement.step("step_production_30", "Mille milliards de milliards de milliards", "1e30 particules / s",
                    "Produire 1e30 particules par seconde.",
                    game -> game.productionPerSecond().gte(BigNum.of(1, 30))),
            Achievement.step("step_milestones", "Dix paliers", "10 paliers de vitesse",
                    "Atteindre dix paliers de vitesse.",
                    game -> game.speedMilestones() >= 10),
            Achievement.feat("feat_nine_copies", "Neuf sur neuf", "9 exemplaires d'un élément",
                    "Porter un élément à neuf exemplaires.", Achievement.Bonus.SYNTHESIS_COST, 0.005,
                    game -> game.state().elements().values().stream().anyMatch(copies -> copies >= 9)),
            Achievement.step("step_set", "Ensemble complet", "1 ensemble complet",
                    "Compléter un ensemble.",
                    game -> game.completedSets() >= 1),
            Achievement.step("step_half_table", "À mi-chemin", "59 éléments",
                    "Découvrir la moitié des éléments.",
                    game -> game.discoveredElements() >= PeriodicTable.ELEMENTS.size() / 2),
            Achievement.step("step_production_100", "Gogol", "1e100 particules / s",
                    "Produire 1e100 particules par seconde.",
                    game -> game.productionPerSecond().gte(BigNum.of(1, 100))),
            Achievement.step("step_full_table", "Mendeleïev", "118 éléments",
                    "Découvrir les 118 éléments.",
                    game -> game.discoveredElements() >= PeriodicTable.ELEMENTS.size()),
            Achievement.step("step_sets", "Tout est rangé", "Les 17 ensembles",
                    "Compléter tous les ensembles.",
                    game -> game.completedSets() >= game.elementSets().size()),
            Achievement.step("step_explosion", "Table rase", "1 explosion",
                    "Faire exploser le tableau périodique.",
                    game -> game.state().explosions() >= 1),

            // ----- La matière noire -----
            Achievement.feat(TARGETED, "Dans le mille", "1 synthèse ciblée",
                    "Mener une synthèse ciblée jusqu'au bout.", Achievement.Bonus.SYNTHESIS_COST, 0.005),
            Achievement.feat(HOLD_LOCK, "Pilote automatique", "Verrouiller l'appui",
                    "Verrouiller l'appui sur la matière noire.", Achievement.Bonus.EXPANSION, 0.05),
            Achievement.feat(MASS_FUSION, "Fusion de masse", "5 groupes d'un coup",
                    "Fusionner au moins cinq groupes de générateurs d'un seul coup.", Achievement.Bonus.ATOMS, 0.01),
            Achievement.feat(ABANDON, "Demi-tour", "Abandonner un défi",
                    "Abandonner un défi.", Achievement.Bonus.PARTICLES, 0.01),
            Achievement.step("step_challenge", "Relevé", "1 défi réussi",
                    "Réussir un défi.",
                    game -> game.completedChallenges() >= 1),
            Achievement.feat("feat_long_hold", "Bras de fer", "1 h d'appui",
                    "Cumuler une heure d'appui sur la matière noire.", Achievement.Bonus.EXPANSION, 0.05,
                    game -> game.state().stats().holdTime() >= 3600),
            Achievement.feat(TRIPLE_DRAW, "Brelan", "3 éléments d'un coup",
                    "Obtenir trois éléments ou plus en une seule synthèse.", Achievement.Bonus.SYNTHESIS_COST, 0.005),
            Achievement.feat(TARGETED_ACTINIDE, "Chasse au trésor", "Viser un actinide",
                    "Obtenir un actinide par une synthèse ciblée.", Achievement.Bonus.PARTICLES, 0.02),
            Achievement.step("step_landmarks", "Plus grand que tout", "Les 26 paliers de taille",
                    "Atteindre tous les paliers de taille.",
                    game -> game.landmarksReached() >= SizeScale.MILESTONES.size()),
            Achievement.feat(FAST_EXPLOSION, "Feu d'artifice", "Exploser en 10 min",
                    "Faire exploser le tableau moins de dix minutes après l'explosion précédente.",
                    Achievement.Bonus.EXPANSION, 0.05),
            Achievement.step("step_explosions", "Dix explosions", "10 explosions",
                    "Faire exploser le tableau dix fois.",
                    game -> game.state().explosions() >= 10),
            Achievement.step("step_fusions_10000", "Dix mille fusions", "10 000 fusions",
                    "Fusionner dix mille fois.",
                    game -> game.state().stats().fusions() >= 10_000),

            // ----- Le troisième acte : du premier Big Bang à l'univers -----
            Achievement.step("step_first_molecule", "Première liaison", "1 molécule",
                    "Créer une première molécule.",
                    game -> game.moleculesCreated() >= 1),
            Achievement.step("step_first_gathering", "Premier amas", "1 sorte rassemblée",
                    "Rassembler une sorte de molécules dans le lieu de son état.",
                    game -> game.substancesFormed() >= 1),
            Achievement.step("step_second_bang", "Encore !", "2 Big Bangs",
                    "Déclencher un deuxième Big Bang.",
                    game -> game.bigBangs() >= 2),
            Achievement.step("step_fifty_kinds", "Chimiste", "50 sortes de molécules",
                    "Créer au moins une molécule de cinquante sortes.",
                    game -> game.moleculeKindsCreated() >= 50),
            Achievement.step("step_first_assembly", "Première pierre", "1 assemblage",
                    "Former un premier assemblage.",
                    game -> game.assembliesFormed() >= 1),
            Achievement.step("step_first_body", "Premier astre", "1 astre",
                    "Former un premier astre.",
                    game -> game.bodiesFormed() >= 1),
            Achievement.feat("feat_comet", "Attrapée au vol", "Saisir une comète",
                    "Saisir une comète pendant qu'elle traverse l'expansion de la matière.",
                    Achievement.Bonus.ATOMS, 0.01, game -> game.cometsCaught() >= 1),
            Achievement.step("step_half_collection", "Demi-rayon", "La moitié d'un rayon",
                    "Créer au moins une molécule de la moitié des sortes d'un rayon.",
                    game -> game.collectionsAt(1) >= 1),
            Achievement.step("step_ten_bodies", "Petit système", "10 astres",
                    "Former dix astres.",
                    game -> game.bodiesFormed() >= 10),
            Achievement.step("step_five_bangs", "Univers mûr", "5 Big Bangs",
                    "Déclencher un cinquième Big Bang : tous les paliers sont atteints.",
                    game -> game.bigBangs() >= 5),
            Achievement.step("step_first_star", "Que la lumière soit", "1 étoile",
                    "Former une première étoile.",
                    game -> game.bodiesFormed(Body.Tier.STAR) >= 1),
            Achievement.step("step_galaxy", "Voie lactée", "La galaxie",
                    "Former la galaxie.",
                    game -> game.hasGalaxy()),
            Achievement.feat("feat_full_collection", "Rayon complet", "Un rayon entier",
                    "Créer au moins une molécule de chaque sorte d'un rayon.",
                    Achievement.Bonus.ATOMS, 0.02, game -> game.collectionsAt(2) >= 1),
            Achievement.feat("feat_bang_challenge", "Sous contrainte", "1 défi de Big Bang",
                    "Réussir un défi de Big Bang.",
                    Achievement.Bonus.AUTOMATION, 0.02, game -> game.completedBangChallenges() >= 1),
            Achievement.step("step_cluster", "Superamas", "L'amas de galaxies",
                    "Former l'amas de galaxies.",
                    game -> game.hasCosmos(Cosmos.CLUSTER)),
            Achievement.step("step_universe", "Tout ce qui est", "L'univers",
                    "Former l'univers.",
                    game -> game.hasCosmos(Cosmos.UNIVERSE)));

    /** Vrai si le joueur possède au moins {@code level} niveaux d'une amélioration de ce type. */
    private static boolean owns(Game game, Class<? extends Effect> type, int level) {
        for (Upgrade upgrade : game.upgrades()) {
            if (type.isInstance(upgrade.effect()) && game.levelOf(upgrade.id()) >= level) return true;
        }
        return false;
    }

    private Achievements() {}
}
