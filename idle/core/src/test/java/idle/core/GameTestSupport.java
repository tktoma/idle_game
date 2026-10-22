package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Random;

/**
 * Ce que partagent les tests des règles du jeu : un petit catalogue d'améliorations et d'automatismes,
 * et les parties toutes prêtes.
 */
abstract class GameTestSupport {

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
