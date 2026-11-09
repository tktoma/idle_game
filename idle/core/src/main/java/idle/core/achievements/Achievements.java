package idle.core.achievements;

import idle.core.Game;
import idle.core.bigbang.BangChallenge;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.List;

/**
 * Catalogue des succès : quatre-vingt-dix-neuf. Les cinquante premiers vont du premier générateur à la
 * dixième explosion ; vingt et un d'entre eux récompensent une action précise et portent un petit
 * bonus à eux. Les seize suivants jalonnent le troisième acte, de la première molécule à l'univers,
 * et trois d'entre eux portent un bonus. Version 0.174 : dix-sept succès du deuxième univers. Douze
 * ouvrent chacun une nouveauté de l'univers 2 au moment logique de la remontée (l'onglet Skins après
 * cinq fusions, le noyau dense et les épreuves à la première explosion, le petit arbre du cycle au
 * premier Big Bang…) ; cinq, un par défi de Big Bang, portent ses bonus d'univers. Leur bonus, c'est
 * ce qu'ils ouvrent. Puis « Grappe d'univers » (version 0.081), qui
 * ouvre la partie secondaire : cinq univers. Enfin quinze succès de fin de partie, du vide à la fin
 * des temps, dont six portent un petit bonus.
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

    // Les succès du deuxième univers (version 0.174) : chacun ouvre une nouveauté ; Game les lit par ces identifiants.
    public static final String UNIVERSE_SKINS = "univ_skins";
    public static final String UNIVERSE_REGIME = "univ_regime";
    public static final String UNIVERSE_SYNERGY = "univ_synergy";
    public static final String UNIVERSE_PERFECT = "univ_perfect";
    public static final String UNIVERSE_FREE = "univ_free";
    public static final String UNIVERSE_TRIALS = "univ_trials";
    public static final String UNIVERSE_DENSE = "univ_dense";
    public static final String UNIVERSE_PREFERENCE = "univ_preference";
    public static final String UNIVERSE_COLLIDER_TRIALS = "univ_collider_trials";
    public static final String UNIVERSE_LEAK = "univ_leak";
    public static final String UNIVERSE_CYCLE = "univ_cycle";
    public static final String UNIVERSE_PLANETS = "univ_planets";

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

            // ----- Jusqu'aux automatismes (1 h à 5 h, heures d'avant le tempo) -----
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

            // ----- La mise en place des automatismes (5 h à 10 h, heures d'avant le tempo) -----
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

            // ----- Le tableau périodique (10 h à 24 h, heures d'avant le tempo) -----
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
            // Version 0.178 : trente sortes sur un catalogue de 73 (cinquante sur 430 avant) ; l'identifiant reste.
            Achievement.step("step_fifty_kinds", "Chimiste", "30 sortes de molécules",
                    "Créer au moins une molécule de trente sortes.",
                    game -> game.moleculeKindsCreated() >= 30),
            Achievement.step("step_first_assembly", "Première pierre", "1 assemblage",
                    "Former un premier assemblage.",
                    game -> game.assembliesFormed() >= 1),
            Achievement.step("step_first_body", "Premier astre", "1 astre",
                    "Former un premier astre.",
                    game -> game.bodiesFormed() >= 1),
            Achievement.feat("feat_comet", "Attrapée au vol", "Saisir une comète",
                    "Saisir une comète pendant qu'elle traverse l'expansion de la matière.",
                    Achievement.Bonus.ATOMS, 0.01, game -> game.cometsCaught() >= 1),
            Achievement.step("step_ten_bodies", "Petit système", "10 astres",
                    "Former dix astres.",
                    game -> game.bodiesFormed() >= 10),
            Achievement.step("step_five_bangs", "Univers mûr", "5 Big Bangs",
                    "Déclencher un cinquième Big Bang : le palier « Univers mûr » s'ouvre.",
                    game -> game.bigBangs() >= 5),
            Achievement.step("step_first_star", "Que la lumière soit", "1 étoile",
                    "Former une première étoile.",
                    game -> game.bodiesFormed(Body.Tier.STAR) >= 1),
            Achievement.step("step_galaxy", "Voie lactée", "La galaxie",
                    "Former la galaxie.",
                    game -> game.hasGalaxy()),
            Achievement.feat("feat_bang_challenge", "Sous contrainte", "1 défi de Big Bang",
                    "Réussir un défi de Big Bang.",
                    Achievement.Bonus.AUTOMATION, 0.02, game -> game.completedBangChallenges() >= 1),
            Achievement.step("step_cluster", "Superamas", "L'amas de galaxies",
                    "Former l'amas de galaxies.",
                    game -> game.hasCosmos(Cosmos.CLUSTER)),
            Achievement.step("step_universe", "Tout ce qui est", "L'univers",
                    "Former l'univers.",
                    game -> game.hasCosmos(Cosmos.UNIVERSE)),
            // ----- Le deuxième univers (version 0.174) : chaque nouveauté s'ouvre par son succès, dans l'ordre des actes -----
            Achievement.step(UNIVERSE_SKINS, "Nouvelle donne", "5 fusions (univers 2+)",
                    "Fusionner cinq fois dans un nouvel univers. Ouvre l'onglet Skins.",
                    game -> game.isSecondUniverse() && (game.fusionsSinceUniverse() >= idle.core.GameUniverse.SKINS_FUSIONS
                            || game.state().skinsOpen())),
            Achievement.step(UNIVERSE_REGIME, "Plein régime", "3 générateurs (univers 2+)",
                    "Avoir trois générateurs dans un nouvel univers. Ouvre le régime des générateurs, dans l'onglet Particules.",
                    game -> game.isSecondUniverse() && (game.generatorCount() >= idle.core.GameUniverse.REGIME_GENERATORS
                            || game.state().regimeOpen())),
            Achievement.step(UNIVERSE_SYNERGY, "Rangs serrés", "10 générateurs (univers 2+)",
                    "Avoir dix générateurs dans un nouvel univers. Ouvre la synergie des rangs.",
                    game -> game.isSecondUniverse() && (game.generatorCount() >= idle.core.GameUniverse.SYNERGY_GENERATORS
                            || game.state().synergyOpen())),
            Achievement.step(UNIVERSE_PERFECT, "Sans accroc", "Persistance (univers 2+)",
                    "Racheter la Persistance dans un nouvel univers. Ouvre la prime de cycle parfait.",
                    game -> game.isSecondUniverse() && game.keepsUpgradesOnFusion()),
            Achievement.step(UNIVERSE_FREE, "Collection reprise", "10 éléments (univers 2+)",
                    "Découvrir dix éléments dans un nouvel univers. Ouvre les éléments gratuits : un tous les dix éléments découverts.",
                    game -> game.isSecondUniverse() && game.discoveredElements() >= idle.core.GameUniverse.FREE_ELEMENT_EVERY),
            Achievement.step(UNIVERSE_TRIALS, "Deuxième souffle", "1 explosion (univers 2+)",
                    "Faire exploser le tableau dans un nouvel univers, l'onglet Skins ouvert. Ouvre les épreuves.",
                    game -> game.isSkinsUnlocked() && game.state().explosions() > 0),
            Achievement.step(UNIVERSE_DENSE, "Noyau dense", "1 M d'atomes et 1 explosion",
                    "Avoir créé un million d'atomes et fait exploser le tableau dans un nouvel univers, l'onglet Skins ouvert. "
                            + "Ouvre la recherche sur le noyau dense.",
                    game -> game.isSkinsUnlocked() && game.state().explosions() > 0
                            && game.state().stats().atomsCreated().gte(idle.core.GameUniverse.DENSE_UNLOCK_ATOMS)),
            Achievement.step(UNIVERSE_PREFERENCE, "Tir groupé", "Synthèse ciblée (univers 2+)",
                    "Ouvrir la synthèse ciblée dans un nouvel univers. Ouvre la famille préférée.",
                    game -> game.isSecondUniverse() && game.isTargetedSynthesisUnlocked()),
            Achievement.step(UNIVERSE_COLLIDER_TRIALS, "Banc d'essai", "Collisionneur (univers 2+)",
                    "Rouvrir le collisionneur dans un nouvel univers, l'onglet Skins ouvert. Ouvre les épreuves du collisionneur.",
                    game -> game.isSkinsUnlocked() && game.isColliderUnlocked()),
            Achievement.step(UNIVERSE_LEAK, "Dix sur dix", "10 défis (univers 2+)",
                    "Réussir les dix défis de matière noire dans un nouvel univers. Ouvre la fuite, un onzième défi.",
                    game -> game.isSecondUniverse() && game.isDarkMatterUnlocked() && game.baseChallengesDone()),
            Achievement.step(UNIVERSE_CYCLE, "Nouveau cycle", "1 Big Bang (univers 2+)",
                    "Déclencher un Big Bang dans un nouvel univers. Ouvre le petit arbre du cycle et l'épreuve du Big Bang à froid.",
                    game -> game.isSecondUniverse() && game.state().bigBangs() > 0),
            universeBang(BangChallenge.VOID, "Rien n'y fait"),
            universeBang(BangChallenge.FORGOTTEN, "Mémoire neuve"),
            universeBang(BangChallenge.BY_HAND, "Fait main"),
            universeBang(BangChallenge.RAISED, "Barre franchie"),
            universeBang(BangChallenge.FROZEN, "Dégel"),
            Achievement.step(UNIVERSE_PLANETS, "Double appartenance", "Les trois défis d'origine",
                    "Réussir les défis de Big Bang « Univers vide », « Sans mémoire » et « À la main » une fois un univers formé. "
                            + "Ouvre la double appartenance : tant qu'ils comptent, une planète peut entrer dans deux systèmes solaires.",
                    game -> game.isSecondUniverse() && game.isBangChallengeCompleted(BangChallenge.VOID)
                            && game.isBangChallengeCompleted(BangChallenge.FORGOTTEN) && game.isBangChallengeCompleted(BangChallenge.BY_HAND)),
            // ----- La partie secondaire (version 0.081) -----
            Achievement.step("step_universe_cluster", "Grappe d'univers", "5 univers",
                    "Créer cinq univers.",
                    game -> game.universes() >= 5),
            // ----- Jusqu'à la fin de la partie (version 0.174) : quelques petits bonus, comme avant -----
            Achievement.step("late_void", "Écume", "Le vide ouvert",
                    "Ouvrir le vide dans le collisionneur.",
                    Game::isVoidUnlocked),
            Achievement.feat("late_void_hole", "Horizon", "Le trou noir du vide",
                    "Former un trou noir le vide ouvert : son onglet s'ouvre.",
                    Achievement.Bonus.EXPANSION, 0.02, Game::isVoidHoleOpen),
            Achievement.step("late_bubble", "Bourgeon", "1 bulle de l'arbre",
                    "Acheter une première bulle de l'arbre de la graine.",
                    game -> game.isTreeBubbleBought(idle.core.GameUniverse.TreeBubble.SEED)),
            Achievement.feat("late_void_max", "Vide comblé", "1 amélioration du vide finie",
                    "Mener une amélioration du vide à son dernier niveau.",
                    Achievement.Bonus.AUTOMATION, 0.02, Achievements::voidUpgradeMaxed),
            Achievement.step("late_seed_half", "Jeune pousse", "Palier 6 de la graine",
                    "Faire passer à la graine son sixième palier.",
                    game -> game.seedStage() >= 6),
            Achievement.feat("late_hadrons", "Hadronisation", "Les hadrons",
                    "Ouvrir l'onglet des hadrons, dans le collisionneur.",
                    Achievement.Bonus.ATOMS, 0.02, Game::isHadronsUnlocked),
            Achievement.step("late_crossing", "Traversée", "Le collisionneur traverse",
                    "Acheter la bulle de l'arbre mûr, à gauche : le collisionneur traverse les univers.",
                    game -> game.isTreeBubbleBought(idle.core.GameUniverse.TreeBubble.MATURE_LEFT)),
            Achievement.feat("late_ten_universes", "Dix univers", "10 univers",
                    "Créer dix univers.",
                    Achievement.Bonus.GENERATOR_COST, 0.02, game -> game.universes() >= 10),
            Achievement.feat("late_world_tree", "Arbre-monde", "Le dernier palier de la graine",
                    "Faire passer à la graine son dernier palier : l'arbre-monde.",
                    Achievement.Bonus.PARTICLES, 0.02, game -> game.seedStage() >= idle.core.GameUniverse.SEED_STAGES),
            Achievement.step("late_exotic", "Bestiaire exotique", "Les hadrons exotiques",
                    "Ouvrir les particules exotiques : pions, kaons, mésons charmés…",
                    Game::isExoticHadronsUnlocked),
            Achievement.feat("late_dimension_pilot", "Les dimensions jouent seules", "Automatisation des dimensions",
                    "Ouvrir l'automatisation des dimensions.",
                    Achievement.Bonus.AUTOMATION, 0.02, Game::isDimensionAutomationUnlocked),
            Achievement.step("late_first_final", "Premier sommet", "1 palier final de dimension",
                    "Atteindre le palier final d'une dimension.",
                    game -> game.finalTiersReached() >= 1),
            Achievement.step("late_roots", "Les racines", "Le défi final ouvert",
                    "Ouvrir le défi final, avec la bulle des racines.",
                    Game::isFinalChallengeOpen),
            Achievement.step("late_five_finals", "À mi-hauteur", "5 paliers finaux",
                    "Atteindre le palier final de cinq dimensions.",
                    game -> game.finalTiersReached() >= 5),
            Achievement.step("late_end", "Fin des temps", "La partie finie",
                    "Atteindre les dix paliers finaux du défi final : la partie est finie.",
                    Game::isGameFinished));

    /** L'identifiant du succès qui fait compter ce défi de Big Bang pour les bonus d'univers. */
    public static String universeBangId(BangChallenge challenge) {
        return "univ_bang_" + challenge.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Le succès d'un défi de Big Bang réussi une fois un univers formé : ses bonus d'univers comptent tant qu'il est réussi. */
    private static Achievement universeBang(BangChallenge challenge, String name) {
        return Achievement.step(universeBangId(challenge), name, "« " + challenge.label() + " » univers 2+",
                "Réussir le défi de Big Bang « " + challenge.label() + " » une fois un univers formé. Ouvre ses bonus d'univers : "
                        + "tant qu'il est réussi, particules et molécules ×2, maturation des astres plus rapide, vitesse et "
                        + "récompenses des sondes ×2.",
                game -> game.isSecondUniverse() && game.isBangChallengeCompleted(challenge));
    }

    /** Vrai si une amélioration du vide est à son dernier niveau. */
    private static boolean voidUpgradeMaxed(Game game) {
        for (idle.core.GameUniverse.VoidUpgrade upgrade : idle.core.GameUniverse.VoidUpgrade.values()) {
            if (game.voidLevel(upgrade) >= upgrade.max()) return true;
        }
        return false;
    }

    private static final java.util.Map<String, Achievement> BY_ID = new java.util.HashMap<>();
    static {
        for (Achievement achievement : DEFAULT) BY_ID.put(achievement.id(), achievement);
    }

    /** Le succès du catalogue qui porte cet identifiant, ou {@code null}. */
    public static Achievement byId(String id) {
        return BY_ID.get(id);
    }

    /** Vrai si le joueur possède au moins {@code level} niveaux d'une amélioration de ce type. */
    private static boolean owns(Game game, Class<? extends Effect> type, int level) {
        for (Upgrade upgrade : game.upgrades()) {
            if (type.isInstance(upgrade.effect()) && game.levelOf(upgrade.id()) >= level) return true;
        }
        return false;
    }

    private Achievements() {}
}
