package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievement;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.Challenge;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import idle.core.sky.Body;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Random;

/**
 * Ce que partagent les tests des règles du jeu : un petit catalogue d'améliorations et d'automatismes,
 * les parties toutes prêtes, et les mises en situation du vrai jeu (catalogues complets, hasard graîné) :
 * l'espace ouvert, le Big Bang prêt, la bifurcation « matière », le n-ième univers, la relecture d'une sauvegarde.
 */
abstract class GameTestSupport {

    // ------------------------------------------------------------------
    // Le vrai jeu, avec ses catalogues complets et un hasard reproductible
    // ------------------------------------------------------------------

    /** Le vrai jeu, catalogues complets et succès compris, démarré, le hasard de la synthèse tiré de la graine 42. */
    static Game realGame() {
        Game game = new Game(42L);
        game.start();
        return game;
    }

    /** Une partie au n-ième univers (1 au moins) : le premier formé, les autres ajoutés par l'outil d'essai ; n dimensions ouvertes. */
    static Game universe(int count) {
        Game game = realGame();
        game.testFormUniverse();
        for (int k = 1; k < count; k++) assertTrue(game.testAddUniverse());
        assertEquals(count, game.universes());
        return game;
    }

    /**
     * Donne toutes les améliorations d'espace sauf celles qui multiplient une grandeur ({@link SpaceUpgrade.Boost}) :
     * elles fausseraient les comptes et ont leurs propres tests.
     */
    static void openSpace(Game game) {
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
        }
    }

    /**
     * Réunit exactement ce que le Big Bang demande : une explosion, les particules, les atomes, la matière noire,
     * les succès (juste le compte demandé), tous les défis, et du sixième au huitième Big Bang les astres mûrs.
     */
    static void readyForBigBang(Game game) {
        game.state().setExplosions(Math.max(1, game.state().explosions()));
        game.state().setParticles(Game.BIG_BANG_PARTICLES);
        game.state().setAtoms(Game.BIG_BANG_ATOMS);
        game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
        int given = 0;
        for (Achievement achievement : game.achievements()) {
            if (given++ < Game.BIG_BANG_ACHIEVEMENTS) game.state().addAchievement(achievement.id());
        }
        for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
        for (Body body : game.bodies()) {
            if (game.bodiesMature() >= game.bigBangSkyRequirement()) break;
            game.state().addBody(body.id());
        }
    }

    /** Remplit le tableau périodique : {@code copies} exemplaires de chacun des 118 éléments. */
    static void fillTable(Game game, int copies) {
        for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), copies);
    }

    /** Remplit le tableau périodique jusqu'au plafond de chaque élément. */
    static void fillTableToMax(Game game) {
        for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), element.category().maxCopies());
    }

    /** Donne {@code count} exemplaires de cette molécule, rassemblée en substance. */
    static void give(Game game, String moleculeId, int count) {
        game.state().setMoleculeCount(moleculeId, count);
        game.state().addSubstance(moleculeId);
    }

    /**
     * Une partie à la bifurcation « matière » de l'arbre de matière noire, ses deux voies payables : douze explosions,
     * douze de matière noire, les trois cases qui y mènent, et 5 000 atomes.
     */
    static Game atFork() {
        Game game = realGame();
        game.state().setDarkMatter(BigNum.of(12));
        game.state().setExplosions(12);
        for (String id : List.of("dark_nucleus", "dark_multisynthesis", "dark_luck")) game.state().setDarkLevel(id, 1);
        game.state().setAtoms(BigNum.of(5000));
        return game;
    }

    /** Écrit cet état en texte de sauvegarde et le relit : ce que verrait une partie rechargée. */
    static GameState reread(GameState state) {
        try {
            return SaveCodec.read(SaveCodec.write(state, 0)).state();
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
    }

    /**
     * Met la partie à ce nombre de Big Bangs, paliers payés (version 0.124 : un palier ouvert ne
     * compte qu'une fois payé). Les tests des prix des paliers posent le compte eux-mêmes.
     */
    static void bangs(Game game, int bigBangs) {
        game.state().setBigBangs(bigBangs);
        payMilestones(game);
    }

    /** Paie tous les paliers déjà ouverts, sans rien dépenser : la plupart des tests parlent de leurs effets, pas de leur prix. */
    static void payMilestones(Game game) {
        for (idle.core.bigbang.BigBangMilestone milestone : idle.core.bigbang.BigBangMilestones.DEFAULT) {
            if (game.state().bigBangs() >= milestone.bigBangs()) game.state().addBoughtMilestone(milestone.name());
        }
    }


    /** Vitesse de départ d'un point de création, en particules par seconde. */
    static final double BASE = Game.BASE_SPEED.toDouble();

    /** Temps nécessaire pour former une particule au départ, en secondes. */
    static final double ONE_PARTICLE = 1 / BASE;
    /** De quoi créer une particule et laisser passer la vérification des succès, faite une fois par seconde. */
    static final double FIRST_CHECK = Math.max(ONE_PARTICLE, 1.25);

    // Catalogue fixe pour les tests : ils ne cassent pas quand le vrai catalogue change.
    static final Upgrade SPEED = new Upgrade("speed", "Vitesse", Resource.PARTICLES,
            BigNum.of(10), 1.5, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(2));
    static final Upgrade GENERATOR = new Upgrade("gen", "Point de création", Resource.PARTICLES,
            BigNum.of(100), 10, 2, new Effect.AddGenerator());
    static final Upgrade HUGE = new Upgrade("huge", "Énorme", Resource.PARTICLES,
            BigNum.of(1), 2, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(1e30));

    // Améliorations payées en atomes.
    static final Upgrade DOUBLE = new Upgrade("double", "Dédoublement", Resource.ATOMS,
            BigNum.of(1), 3, Upgrade.NO_LIMIT, new Effect.MultiplyParticles(2));
    static final Upgrade MASS = new Upgrade("mass", "Masse", Resource.ATOMS,
            BigNum.of(2), 1, 1, new Effect.MultiplyByAtoms(0.5));
    static final Upgrade PATIENCE = new Upgrade("patience", "Patience", Resource.ATOMS,
            BigNum.of(1), 1, 1, new Effect.MultiplyByRunTime(1));

    static final Upgrade BOOST = new Upgrade("boost", "Catalyseur", Resource.ATOMS,
            BigNum.of(1), 1, 3, new Effect.StrengthenSpeed(0.5));
    static final Upgrade DISCOUNT = new Upgrade("discount", "Compression", Resource.ATOMS,
            BigNum.of(1), 1, 3, new Effect.DiscountGenerators(0.5));
    static final Upgrade KEEP = new Upgrade("keep", "Persistance", Resource.ATOMS,
            BigNum.of(1), 1, 1, new Effect.KeepUpgradesOnFusion());

    // Automatismes, payés en atomes. Les trois premiers sont au délai minimal (0,1 s) dès l'achat,
    // sans niveau de cadence à acheter, pour tester les achats eux-mêmes.
    static final Automation AUTO_SPEED =
            Automation.buying("autoSpeed", "Vitesse", BigNum.of(2), "speed").withCadence(0, 0, BigNum.ONE, 1);
    static final Automation AUTO_GEN =
            Automation.buying("autoGen", "Générateur", BigNum.of(3), "gen").withCadence(0, 0, BigNum.ONE, 1);
    static final Automation AUTO_FUSION =
            Automation.fusing("autoFusion", "Fusion", BigNum.of(5)).withCadence(0, 0, BigNum.ONE, 1);
    /** Une action toutes les 4 s, puis 2 s, puis 1 s au maximum ; la cadence coûte 2 puis 6 atomes. */
    static final Automation AUTO_SLOW =
            Automation.buying("autoSlow", "Lent", BigNum.of(1), "speed").withCadence(4, 2, BigNum.of(2), 3);
    /** Synthèse automatique, une action toutes les 2 s. */
    static final Automation AUTO_SYNTH =
            Automation.synthesizing("autoSynth", "Synthèse", BigNum.of(4)).withCadence(2, 0, BigNum.ONE, 1);

    /** Partie pas encore démarrée : aucun générateur. */
    static Game unstartedGame() {
        return new Game(new GameState(),
                List.of(SPEED, GENERATOR, HUGE, DOUBLE, MASS, PATIENCE, BOOST, DISCOUNT, KEEP),
                List.of(AUTO_SPEED, AUTO_GEN, AUTO_FUSION, AUTO_SLOW, AUTO_SYNTH), new Random(42));
    }

    /** Partie démarrée : le premier générateur est créé. */
    static Game newGame() {
        Game game = unstartedGame();
        game.start();
        return game;
    }

    static Game gameWithParticles(double particles) {
        Game game = newGame();
        game.state().setParticles(BigNum.of(particles));
        return game;
    }

    static void assertValue(double expected, BigNum actual) {
        assertEquals(expected, actual.toDouble(), Math.abs(expected) * 1e-9 + 1e-9);
    }

    /** Matière possédée plus particules en cours de formation. */
    static double total(Game game) {
        double total = game.state().particles().toDouble();
        for (double formation : game.state().formations()) total += formation;
        return total;
    }

    /** Partie avec tous les générateurs du catalogue de test débloqués (trois). */
    static Game gameReadyToFuse() {
        Game game = gameWithParticles(1e9);
        game.buy("gen");
        game.buy("gen");
        game.buy("speed");
        game.tick(ONE_PARTICLE / 8);
        return game;
    }

    /** Fait comme si la partie avait déjà créé les atomes qui débloquent l'automatisation et le tableau périodique. */
    static void createEnoughAtoms(Game game) {
        game.state().setTotalAtoms(game.state().totalAtoms().max(Game.UNLOCK_TOTAL_ATOMS));
    }

    /** Débloque le tableau, achète Persistance et tous les automatismes disponibles, les porte à leur cadence maximale, puis les coupe. */
    static void unlock(Game game) {
        BigNum atoms = game.state().atoms();
        game.state().setAtoms(BigNum.of(100));
        game.buy("keep");
        createEnoughAtoms(game);
        for (Automation automation : game.automations()) {
            game.buyAutomation(automation.id());
            while (game.speedUpAutomation(automation.id())) { /* jusqu'à la cadence maximale */ }
            game.setAutomationEnabled(automation.id(), false);
        }
        game.state().setAtoms(atoms);
        assertTrue(game.isPeriodicTableUnlocked());
    }

    /** Redébloque tous les générateurs puis fusionne. */
    static void fuseAgain(Game game) {
        game.state().setParticles(BigNum.of(1e9));
        game.buy("gen");
        game.buy("gen");
        assertTrue(game.fuse());
    }
}
