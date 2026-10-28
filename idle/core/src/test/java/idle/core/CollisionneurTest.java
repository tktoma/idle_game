package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.collider.ColliderAchievement;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.CollisionTier;
import idle.core.collider.Quark;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.sky.Body;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Le collisionneur de quarks : énergie, collisions, quarks, la chaîne, les trous noirs, le quark top. */
class CollisionneurTest {

    private static Game newGame() {
        return GameTestSupport.realGame();
    }

    /** Après une première explosion, avec de l'énergie à volonté, et l'amélioration « Collisions » prise. */
    private static Game opened(double energy) {
        Game game = bare(energy);
        game.state().setColliderUpgradeLevel(ColliderUpgrade.COLLISIONS, 1);
        return game;
    }

    /** Après une première explosion, sans aucune amélioration du collisionneur. */
    private static Game bare(double energy) {
        Game game = newGame();
        game.state().setExplosions(1);
        game.state().setColliderEnergy(energy);
        return game;
    }

    @Nested
    class Ouverture {

        @Test
        void ilSOuvreALaPremiereExplosion() {
            Game game = newGame();
            assertFalse(game.isColliderUnlocked());
            assertEquals(0, game.colliderEnergyPerSecond());
            game.state().setExplosions(1);
            assertTrue(game.isColliderUnlocked());
        }

        @Test
        void lEnergieVientDesFaisceauxCroises() {
            Game game = opened(0);
            // Un faisceau d'office : 1 énergie par seconde, sans rien acheter.
            assertEquals(ColliderGenerator.BEAM.power(), game.colliderEnergyPerSecond(), 1e-9);
            game.state().setColliderEnergy(0);
            game.tick(10);
            assertEquals(10 * ColliderGenerator.BEAM.power(), game.colliderEnergy(), 1e-6);
        }

        @Test
        void seulesLaBasseEnergieEstOuverteAuDebut() {
            Game game = opened(1e9);
            assertTrue(game.isCollisionTierOpen(CollisionTier.LOW));
            assertFalse(game.isCollisionTierOpen(CollisionTier.MEDIUM));
            game.state().setColliderUpgradeLevel(ColliderUpgrade.FLAVOR_STRANGE, 1);
            assertTrue(game.isCollisionTierOpen(CollisionTier.MEDIUM));
            assertFalse(game.isCollisionTierOpen(CollisionTier.HIGH));
        }

        @Test
        void sansLAmeliorationCollisionsLesCroisementsNeDonnentQueDeLEnergie() {
            Game game = bare(0);
            assertEquals(0, game.collisionChance(), 0);
            assertFalse(game.isQuarkOpen(Quark.UP));
            for (int i = 0; i < 200; i++) game.tick(Game.RESONANCE_PERIOD);
            assertEquals(0, game.quarks(Quark.UP) + game.quarks(Quark.DOWN));
            untilCrossing(game);
            assertTrue(game.catchCrossing() > 0, "un clic au croisement donne toujours son énergie");
            assertEquals(0, game.state().collisionRuns(), "mais pas de collision");
        }
    }

    /** Les quarks obtenus, toutes saveurs. */
    private static int quarkTotal(Game game) {
        int total = 0;
        for (Quark quark : Quark.values()) total += game.quarks(quark);
        return total;
    }

    /** Jusqu'au prochain croisement, sans le dépasser. */
    private static void untilCrossing(Game game) {
        while (!game.isCrossing()) game.tick(0.02);
    }

    @Nested
    class Croisements {

        @Test
        void unCroisementParTourEtUneChanceDeCollision() {
            Game game = opened(0);
            for (int i = 0; i < 1000; i++) game.tick(Game.RESONANCE_PERIOD);
            assertEquals(1000, game.state().colliderCrossings());
            int ud = quarkTotal(game);
            // 15 % des croisements donnent une collision, d'un quark : environ 150.
            assertTrue(ud > 100 && ud < 200, ud + " quarks u et d");
            assertEquals(0, game.quarks(Quark.STRANGE), "l'étrange n'est pas encore ouvert");
            assertEquals(ud, game.state().collisionRuns());
            assertFalse(game.takeFinishedCollisions().isEmpty());
            assertTrue(game.takeFinishedCollisions().isEmpty(), "annoncées une fois");
        }

        @Test
        void lesSaveursSOuvrentUneAUneParLesAmeliorations() {
            Game game = opened(0);
            assertTrue(game.isQuarkOpen(Quark.UP) && game.isQuarkOpen(Quark.DOWN));
            assertFalse(game.isCollisionTierOpen(CollisionTier.MEDIUM));
            game.state().setColliderUpgradeLevel(ColliderUpgrade.FLAVOR_STRANGE, 1);
            assertTrue(game.isCollisionTierOpen(CollisionTier.MEDIUM));
            assertTrue(game.quarkWeight(Quark.STRANGE) > 0);
            assertEquals(CollisionTier.MEDIUM, game.colliderTier());
            assertFalse(game.isQuarkOpen(Quark.CHARM));
            assertEquals(1, game.quarkShare(Quark.UP) + game.quarkShare(Quark.DOWN) + game.quarkShare(Quark.STRANGE), 1e-9);
        }

        @Test
        void unLongPasCompteLeButinEnMoyenne() {
            Game game = opened(0);
            game.tick(10_800);                                             // d'un coup : bien plus de 400 croisements
            assertEquals(5_400, game.state().colliderCrossings(), 1);
            double expected = 5_400 * Game.COLLISION_CHANCE / 2;
            assertEquals(2 * expected, quarkTotal(game), expected * 0.2);
        }

        @Test
        void laFocalisationMonteLaChance() {
            Game game = opened(0);
            assertEquals(0.15, game.collisionChance(), 1e-9);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.LUCK, 2);
            assertEquals(0.21, game.collisionChance(), 1e-9);
        }

        @Test
        void lesAnciennesCollisionsSeTerminentDUnCoup() {
            Game game = opened(0);
            game.state().setCollisions(java.util.List.of(new Collision(CollisionTier.LOW, 20, true, true)));
            game.tick(0.01);
            assertTrue(game.state().collisions().isEmpty());
            assertEquals(12, quarkTotal(game), "trois u et trois d, doublés");
        }

        @Test
        void rienNeSePerdALExplosionNiAuBigBang() {
            Game game = opened(1234);
            game.state().setQuarks(Quark.UP, 5);
            game.state().setColliderEnergyProduced(5e9);
            game.state().setColliderGenerators(ColliderGenerator.MAGNET, 4);
            game.state().setColliderGeneratorsBought(ColliderGenerator.MAGNET, 4);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.ENERGY, 2);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), element.category().maxCopies());
            assertTrue(game.explode());
            assertEquals(5, game.quarks(Quark.UP));
            assertEquals(5e9, game.colliderEnergyProduced(), 1);
            readyForBigBang(game);
            double energy = game.colliderEnergy();
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertTrue(game.isColliderUnlocked(), "le collisionneur reste ouvert après le Big Bang");
            assertEquals(energy, game.colliderEnergy(), 1e-6);
            assertEquals(5, game.quarks(Quark.UP));
            assertEquals(5e9, game.colliderEnergyProduced(), 1);
            assertEquals(4, game.state().colliderGeneratorsBought(ColliderGenerator.MAGNET));
            assertEquals(2, game.colliderUpgradeLevel(ColliderUpgrade.ENERGY));
            game.reset();                                                  // seul « Recommencer de zéro » efface tout
            assertEquals(0, game.colliderEnergyProduced(), 0);
            assertEquals(0, game.colliderUpgradeLevel(ColliderUpgrade.ENERGY));
            assertEquals(0, game.colliderEnergy(), 0);
        }
    }

    private static void readyForBigBang(Game game) {
        GameTestSupport.readyForBigBang(game);
    }

    @Nested
    class Capte {

        @Test
        void unClicAuCroisementDonneTroisSecondesDEnergieEtUneCollisionDoublee() {
            Game game = opened(0);
            untilCrossing(game);
            double before = game.colliderEnergy();
            long runs = game.state().collisionRuns();
            double gain = game.catchCrossing();
            assertEquals(Game.CROSSING_SECONDS * game.colliderEnergyPerSecond(), gain, 1e-9);
            assertEquals(before + gain, game.colliderEnergy(), 1e-9);
            assertTrue(game.state().collisionRuns() > runs, "une collision assurée");
            Game.CollisionResult last = game.recentCollisions().get(game.recentCollisions().size() - 1);
            assertTrue(last.caught());
            assertEquals(2, last.count(), "butin doublé");
            assertEquals(0, game.catchCrossing(), 0, "un seul par croisement");
            while (game.isCrossing()) game.tick(0.02);
            assertEquals(0, game.catchCrossing(), 0, "à côté, rien");
            untilCrossing(game);
            assertTrue(game.catchCrossing() > 0, "le croisement suivant se capte de nouveau");
        }

        @Test
        void leDeclencheurCapteSeul() {
            Game game = opened(0);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH, 3);
            for (int i = 0; i < 10; i++) game.tick(Game.RESONANCE_PERIOD);
            assertEquals(20, quarkTotal(game), "dix croisements captés, deux quarks chacun");
            assertEquals(20 + 10 * Game.CROSSING_SECONDS, game.colliderEnergy(), 1e-6);
        }

        @Test
        void leDeclencheurCapteUnCroisementSurCinqPuisUnSurTroisPuisTous() {
            // Sans « Collisions », les croisements laissés passer ne donnent rien : l'énergie dit combien ont été captés
            // (trois secondes de production chacun), en plus des trente secondes de production des quinze croisements.
            Game none = bare(0);
            for (int i = 0; i < 15; i++) none.tick(Game.RESONANCE_PERIOD);
            assertEquals(30, none.colliderEnergy(), 1e-6, "sans le déclencheur, rien ne se capte seul");
            assertEquals(-1, none.state().colliderCaught(), "aucun croisement capté");
            Game first = bare(0);
            first.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH, 1);
            for (int i = 0; i < 15; i++) first.tick(Game.RESONANCE_PERIOD);
            assertEquals(15, first.state().colliderCrossings());
            assertEquals(30 + 3 * Game.CROSSING_SECONDS, first.colliderEnergy(), 1e-6, "niveau 1 : un croisement sur cinq, trois captés");
            assertEquals(15, first.state().colliderCaught(), "le dernier capté : le quinzième");
            Game second = bare(0);
            second.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH, 2);
            for (int i = 0; i < 15; i++) second.tick(Game.RESONANCE_PERIOD);
            assertEquals(30 + 5 * Game.CROSSING_SECONDS, second.colliderEnergy(), 1e-6, "niveau 2 : un sur trois, cinq captés");
            assertEquals(15, second.state().colliderCaught());
            Game third = bare(0);
            third.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH, 50);
            for (int i = 0; i < 15; i++) third.tick(Game.RESONANCE_PERIOD);
            assertEquals(30 + 15 * Game.CROSSING_SECONDS, third.colliderEnergy(), 1e-6, "du niveau 3 au-delà : tous");
        }

        @Test
        void lesFaisceauxRapidesEtLeDetecteurElargi() {
            Game game = opened(0);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.SPEED, 5);
            assertEquals(Game.RESONANCE_PERIOD / 2, game.colliderPeriod(), 1e-9);
            for (int i = 0; i < 100; i++) game.tick(1);
            assertEquals(100, game.state().colliderCrossings());
            game.state().setColliderUpgradeLevel(ColliderUpgrade.WINDOW, 2);
            assertEquals(Game.RESONANCE_WINDOW * 1.5, game.colliderWindow(), 1e-9);
        }
    }

    @Nested
    class QuarkTop {

        /**
         * Le hasard du collisionneur est un hachage du numéro du croisement : avec toutes les saveurs ouvertes, le
         * 3 883e croisement est le premier à donner un top (17 % de collision × 0,13 % de top). On part juste avant,
         * plutôt que de rejouer les 3 882 d'avant.
         */
        private Game withTop() {
            Game game = opened(0);
            for (Quark quark : Quark.values()) game.state().setColliderUpgradeLevel(ColliderUpgrade.flavorOf(quark), 1);
            assertTrue(game.isQuarkOpen(Quark.TOP));
            game.state().setColliderCrossings(3_870);
            for (int crossing = 0; crossing < 30 && !game.hasTop(); crossing++) game.tick(Game.RESONANCE_PERIOD);
            assertEquals(3_883, game.state().colliderCrossings(), "le top arrive par une collision, au croisement attendu");
            return game;
        }

        @Test
        void ilArriveAvecLEnergieExtremeEtSUtiliseEnUneMinute() {
            Game game = withTop();
            assertTrue(game.hasTop());
            game.state().setOverheatLeft(0);
            BigNum particles = game.particlesPerCreation();
            assertTrue(game.useTop());
            assertFalse(game.hasTop());
            assertEquals(particles.toDouble() * Game.OVERHEAT_FACTOR, game.particlesPerCreation().toDouble(), particles.toDouble() * 1e-9);
            assertEquals(Game.OVERHEAT_SECONDS, game.overheatLeft(), 1e-9);
        }

        @Test
        void rateIlLaisseQuandMemeDesQuarksBeaux() {
            Game game = opened(0);
            game.state().setTopLeft(10);
            int bottom = game.quarks(Quark.BOTTOM);
            game.tick(11);
            assertFalse(game.hasTop());
            assertFalse(game.useTop());
            assertEquals(bottom + Game.TOP_CONSOLATION_BOTTOM, game.quarks(Quark.BOTTOM));
        }

        @Test
        void leTopStableDureDavantage() {
            Game game = opened(0);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.TOP_LIFE, 2);
            assertEquals(4 * Game.TOP_SECONDS, game.topSeconds(), 1e-9);
            assertEquals(2 * Game.OVERHEAT_SECONDS, game.overheatSeconds(), 1e-9);
        }
    }



    @Test
    void lesDeuxTrousNoirsDemandentUnPalierDEnergie() {
        Game game = bare(0);
        List<Body> needing = game.bodies().stream().filter(game::needsColliderEnergy).toList();
        assertEquals(List.of("black_hole", "supermassive_black_hole"), needing.stream().map(Body::id).toList(), "les deux trous noirs, et eux seuls");
        Body hole = game.body("black_hole"), core = game.body("supermassive_black_hole");
        assertEquals(Game.BLACK_HOLE_ENERGY, game.colliderEnergyNeeded(hole), 0);
        assertEquals(Game.SUPERMASSIVE_ENERGY, game.colliderEnergyNeeded(core), 0);
        assertEquals(core.conditions() + 1, game.bodyConditions(core.id()));
        int before = game.bodyConditionsMet(hole.id()), coreBefore = game.bodyConditionsMet(core.id());
        game.state().setColliderEnergyProduced(Game.BLACK_HOLE_ENERGY);
        assertEquals(before + 1, game.bodyConditionsMet(hole.id()));
        assertEquals(coreBefore, game.bodyConditionsMet(core.id()), "le supermassif en demande davantage");
        game.state().setColliderEnergyProduced(Game.SUPERMASSIVE_ENERGY);
        assertEquals(coreBefore + 1, game.bodyConditionsMet(core.id()));
    }

    @Test
    void lEnergieProduiteSeCompteEtNeSeDepensePas() {
        Game game = bare(0);
        game.tick(10);
        assertEquals(10, game.colliderEnergyProduced(), 1e-6);
        game.state().setColliderEnergy(10);
        assertTrue(game.buyColliderGenerator(ColliderGenerator.BEAM));
        assertEquals(10, game.colliderEnergyProduced(), 1e-6, "acheter ne retire rien à l'énergie produite");
        untilCrossing(game);
        double before = game.colliderEnergyProduced();
        double gain = game.catchCrossing();
        assertTrue(gain > 0);
        assertEquals(before + gain, game.colliderEnergyProduced(), 1e-6, "un croisement capté compte aussi");
    }

    @Test
    void lesHuitGenerateursSeProduisentLUnLAutre() {
        Game game = GameTestSupport.realGame();
        game.state().setExplosions(1);
        assertTrue(game.isColliderGeneratorOpen(ColliderGenerator.BEAM));
        assertFalse(game.isColliderGeneratorOpen(ColliderGenerator.MAGNET), "l'aimant attend un premier faisceau");
        assertFalse(game.buyColliderGenerator(ColliderGenerator.BEAM));
        game.state().setColliderEnergy(10);
        assertTrue(game.buyColliderGenerator(ColliderGenerator.BEAM));
        assertEquals(0, game.colliderEnergy(), 1e-9);
        assertEquals(10 * 1.4, game.colliderGeneratorCost(ColliderGenerator.BEAM), 1e-9);
        assertEquals(2 * ColliderGenerator.BEAM.power(), game.colliderEnergyPerSecond(), 1e-9);
        assertTrue(game.isColliderGeneratorOpen(ColliderGenerator.MAGNET));
        game.state().setColliderEnergy(100);
        assertTrue(game.buyColliderGenerator(ColliderGenerator.MAGNET));
        // L'aimant produit des faisceaux : 0,02 par seconde ; en 100 s, 2 de plus.
        for (int i = 0; i < 100; i++) game.tick(1);
        assertEquals(1 + 100 * ColliderGenerator.MAGNET.power(), game.state().colliderGenerators(ColliderGenerator.BEAM), 1e-6);
        assertEquals(1, game.state().colliderGeneratorsBought(ColliderGenerator.BEAM), "les produits ne comptent pas pour le prix");
        game.state().setColliderEnergy(1e6);
        int bought = game.buyMaxColliderGenerator(ColliderGenerator.BEAM);
        assertTrue(bought > 10 && !game.canBuyColliderGenerator(ColliderGenerator.BEAM));
    }

    @Nested
    class Ameliorations {

        private Game withAllGenerators() {
            Game game = bare(0);
            for (ColliderGenerator generator : ColliderGenerator.values()) {
                game.state().setColliderGeneratorsBought(generator, 1);
                game.state().setColliderGenerators(generator, 1);
            }
            return game;
        }

        @Test
        void ellesSOuvrentAvecLeHuitiemeGenerateur() {
            Game game = bare(0);
            assertFalse(game.isColliderUpgradesUnlocked());
            game = withAllGenerators();
            assertTrue(game.isColliderUpgradesUnlocked());
            int open = 0;
            for (ColliderUpgrade upgrade : ColliderUpgrade.values()) if (game.isColliderUpgradeOpen(upgrade)) open++;
            assertEquals(1, open, "au début, une seule");
            assertTrue(game.isColliderUpgradeOpen(ColliderUpgrade.COLLISIONS));
        }

        @Test
        void chacuneEnOuvreDAutres() {
            Game game = withAllGenerators();
            game.state().setColliderEnergy(ColliderUpgrade.COLLISIONS.cost(0));
            assertTrue(game.buyColliderUpgrade(ColliderUpgrade.COLLISIONS));
            assertEquals(0, game.colliderEnergy(), 1);
            assertFalse(game.buyColliderUpgrade(ColliderUpgrade.COLLISIONS), "un seul niveau");
            assertTrue(game.isCollisionsUnlocked());
            for (ColliderUpgrade open : java.util.List.of(ColliderUpgrade.FLAVOR_STRANGE, ColliderUpgrade.AUTO_BEAM, ColliderUpgrade.ENERGY,
                    ColliderUpgrade.LUCK, ColliderUpgrade.SPEED, ColliderUpgrade.GENERATOR_POWER)) {
                assertTrue(game.isColliderUpgradeOpen(open), open.name());
            }
            assertFalse(game.isColliderUpgradeOpen(ColliderUpgrade.FLAVOR_CHARM), "les saveurs une à une");
            assertFalse(game.isColliderUpgradeOpen(ColliderUpgrade.AUTO_MAGNET), "les automates un à un");
            assertFalse(game.isColliderUpgradeOpen(ColliderUpgrade.ATOMS), "la chaîne pas à pas");
            // Toutes, de proche en proche, sont atteignables.
            for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
                ColliderUpgrade at = upgrade;
                int steps = 0;
                while (at.requires() != null && steps++ < 30) {
                    assertTrue(at.requiredLevel() <= at.requires().maxLevel(), at.name());
                    at = at.requires();
                }
                assertEquals(ColliderUpgrade.COLLISIONS, at, upgrade.name());
            }
        }

        @Test
        void lesAutomatesAchetentEtSeMettentEnPause() {
            Game game = withAllGenerators();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_BEAM, 1);
            game.state().setColliderEnergy(1000);
            game.tick(0.01);
            assertTrue(game.state().colliderGeneratorsBought(ColliderGenerator.BEAM) > 5);
            assertFalse(game.canBuyColliderGenerator(ColliderGenerator.BEAM));
            game.toggleColliderAutomation(ColliderUpgrade.AUTO_BEAM);
            int bought = game.state().colliderGeneratorsBought(ColliderGenerator.BEAM);
            game.state().setColliderEnergy(1e6);
            game.tick(0.01);
            assertEquals(bought, game.state().colliderGeneratorsBought(ColliderGenerator.BEAM), "en pause, il n'achète plus");
        }

        @Test
        void energieEtEspace() {
            Game game = withAllGenerators();
            double base = game.colliderEnergyPerSecond();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.ENERGY, 3);
            assertEquals(base * 8, game.colliderEnergyPerSecond(), base * 1e-9);
            double space = game.colliderSpaceMultiplier();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.SPACE, 2);
            assertEquals(space * 1.5, game.colliderSpaceMultiplier(), 1e-9);
        }



        @Test
        void laChaineSePaieAussiEnQuarks() {
            Game game = withAllGenerators();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.COLLISIONS, 1);
            game.state().setColliderEnergy(1e30);
            assertEquals(Quark.UP, ColliderUpgrade.ENERGY.quark());
            assertEquals(10, game.colliderUpgradeQuarks(ColliderUpgrade.ENERGY));
            assertFalse(game.canBuyColliderUpgrade(ColliderUpgrade.ENERGY), "sans quarks haut, rien");
            game.state().setQuarks(Quark.UP, 25);
            assertTrue(game.buyColliderUpgrade(ColliderUpgrade.ENERGY));
            assertEquals(15, game.quarks(Quark.UP));
            assertEquals(20, game.colliderUpgradeQuarks(ColliderUpgrade.ENERGY), "le double au niveau suivant");
            assertFalse(game.canBuyColliderUpgrade(ColliderUpgrade.ENERGY));
            assertEquals(0, game.colliderUpgradeQuarks(ColliderUpgrade.LUCK), "les autres ne coûtent que de l'énergie");
            assertEquals(java.util.List.of(Quark.UP, Quark.UP, Quark.DOWN, Quark.STRANGE, Quark.CHARM, Quark.BOTTOM),
                    java.util.List.of(ColliderUpgrade.ENERGY.quark(), ColliderUpgrade.PARTICLES.quark(), ColliderUpgrade.ATOMS.quark(),
                            ColliderUpgrade.DARK_MATTER.quark(), ColliderUpgrade.SPACE.quark(), ColliderUpgrade.MOLECULES.quark()));
        }

        @Test
        void ellesSeSauvegardent() throws Exception {
            Game game = withAllGenerators();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.LUCK, 3);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_BEAM, 1);
            game.toggleColliderAutomation(ColliderUpgrade.AUTO_BEAM);
            game.state().setColliderPhase(0.3);
            game.state().setColliderCrossings(77);
            GameState back = idle.core.save.SaveCodec.read(idle.core.save.SaveCodec.write(game.state(), 1_700_000_000_000L)).state();
            assertEquals(3, back.colliderUpgradeLevel(ColliderUpgrade.LUCK));
            assertTrue(back.isColliderPaused(ColliderUpgrade.AUTO_BEAM));
            assertEquals(0.3, back.colliderPhase(), 1e-9);
            assertEquals(77, back.colliderCrossings());
        }
    }
    @Nested
    class SuccesEtChaine {

        @Test
        void lesSuccesRenforcentLesGenerateurs() {
            Game game = bare(0);
            assertEquals(0, game.colliderAchievements());
            double cost = game.colliderGeneratorCost(ColliderGenerator.MAGNET);
            double energy = game.colliderEnergyPerSecond();
            game.state().setColliderGeneratorsBought(ColliderGenerator.BEAM, 10);
            assertTrue(game.hasColliderAchievement(ColliderAchievement.BEAM_OUTPUT));
            assertEquals(energy * 2, game.colliderEnergyPerSecond(), 1e-9, "dix faisceaux achetés : énergie ×2");
            game.state().setColliderGeneratorsBought(ColliderGenerator.MAGNET, 30);
            game.state().setColliderGenerators(ColliderGenerator.MAGNET, 1);
            assertTrue(game.hasColliderAchievement(ColliderAchievement.MAGNET_COST));
            assertEquals(ColliderGenerator.MAGNET.cost(30) / 2, game.colliderGeneratorCost(ColliderGenerator.MAGNET), 1e-6);
            assertEquals(ColliderGenerator.MAGNET.power() * 2, game.colliderGeneratorOutput(ColliderGenerator.MAGNET), 1e-12,
                    "trente aimants : le succès de production aussi");
            assertTrue(cost < game.colliderGeneratorCost(ColliderGenerator.MAGNET));
            assertEquals(40, game.colliderGeneratorsBoughtTotal());
            assertFalse(game.hasColliderAchievement(ColliderAchievement.HUNDRED));
            game.state().setColliderGeneratorsBought(ColliderGenerator.CAVITY, 60);
            assertTrue(game.hasColliderAchievement(ColliderAchievement.HUNDRED));
            game.state().setCollisionRuns(1000);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.COLLISIONS, 1);
            assertEquals(0.17, game.collisionChance(), 1e-9, "mille collisions : +2 points");
        }

        @Test
        void lesGenerateursRenforces() {
            Game game = bare(0);
            game.state().setColliderGenerators(ColliderGenerator.CAVITY, 3);
            double before = game.colliderGeneratorOutput(ColliderGenerator.CAVITY), energy = game.colliderEnergyPerSecond();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.GENERATOR_POWER, 2);
            assertEquals(before * 4, game.colliderGeneratorOutput(ColliderGenerator.CAVITY), 1e-12);
            assertEquals(energy, game.colliderEnergyPerSecond(), 1e-12, "l'énergie a ses propres améliorations");
        }

        @Test
        void centGenerateursRenforcentLEnergieEtDeuxCentCinquanteToutLeMonde() {
            Game game = bare(0);
            game.state().setColliderGenerators(ColliderGenerator.CAVITY, 1);
            double energy = game.colliderEnergyPerSecond(), cavity = game.colliderGeneratorOutput(ColliderGenerator.CAVITY);
            assertEquals(1, energy, 1e-12, "un faisceau : une énergie par seconde");
            // Cent générateurs achetés (ici des cavités, qui gagnent aussi leur succès de production ×2 à dix).
            game.state().setColliderGeneratorsBought(ColliderGenerator.CAVITY, 100);
            assertTrue(game.hasColliderAchievement(ColliderAchievement.HUNDRED));
            assertFalse(game.hasColliderAchievement(ColliderAchievement.QUARTER_THOUSAND));
            assertEquals(energy * 1.5, game.colliderEnergyPerSecond(), 1e-12, "cent générateurs : énergie ×1,5");
            assertEquals(cavity * 2, game.colliderGeneratorOutput(ColliderGenerator.CAVITY), 1e-12, "les cavités : leur succès de production, et rien de plus");
            // Deux cent cinquante : chaque générateur produit ×1,5, les faisceaux compris.
            game.state().setColliderGeneratorsBought(ColliderGenerator.CAVITY, 250);
            assertTrue(game.hasColliderAchievement(ColliderAchievement.QUARTER_THOUSAND));
            assertEquals(energy * 1.5 * 1.5, game.colliderEnergyPerSecond(), 1e-12, "×1,5 des cent, ×1,5 des deux cent cinquante");
            assertEquals(cavity * 2 * 1.5, game.colliderGeneratorOutput(ColliderGenerator.CAVITY), 1e-12);
            // Les générateurs renforcés : ×2 par niveau, sauf pour les faisceaux ; l'amélioration d'énergie : ×2 par niveau.
            game.state().setColliderUpgradeLevel(ColliderUpgrade.GENERATOR_POWER, 2);
            assertEquals(cavity * 12, game.colliderGeneratorOutput(ColliderGenerator.CAVITY), 1e-12);
            assertEquals(energy * 2.25, game.colliderEnergyPerSecond(), 1e-12, "les faisceaux ne sont pas renforcés");
            game.state().setColliderUpgradeLevel(ColliderUpgrade.ENERGY, 3);
            assertEquals(energy * 2.25 * 8, game.colliderEnergyPerSecond(), 1e-12, "2³ de l'amélioration d'énergie");
        }

        @Test
        void lesPrixDesAmeliorationsDuCollisionneur() {
            Game game = bare(0);
            for (ColliderGenerator generator : ColliderGenerator.values()) game.state().setColliderGeneratorsBought(generator, 1);
            assertTrue(game.isColliderUpgradesUnlocked(), "les huit générateurs achetés ouvrent les améliorations");
            assertEquals(1e10, game.colliderUpgradeCost(ColliderUpgrade.COLLISIONS), 1, "Collisions : dix milliards d'énergie");
            assertEquals(5e10, game.colliderUpgradeCost(ColliderUpgrade.LUCK), 1);
            assertEquals(2e10, game.colliderUpgradeCost(ColliderUpgrade.ENERGY), 1);
            assertEquals(1e12, game.colliderUpgradeCost(ColliderUpgrade.AUTO_CATCH), 1);
            assertEquals(1e10, game.colliderUpgradeCost(ColliderUpgrade.AUTO_BEAM), 1, "l'automate des faisceaux");
            game.state().setColliderUpgradeLevel(ColliderUpgrade.LUCK, 1);
            assertEquals(2.5e11, game.colliderUpgradeCost(ColliderUpgrade.LUCK), 1, "faisceaux focalisés : ×5 par niveau");
            game.state().setColliderUpgradeLevel(ColliderUpgrade.ENERGY, 2);
            assertEquals(7.2e11, game.colliderUpgradeCost(ColliderUpgrade.ENERGY), 1, "énergie : ×6 par niveau");
            game.state().setColliderUpgradeLevel(ColliderUpgrade.SPEED, 3);
            assertEquals(2e13, game.colliderUpgradeCost(ColliderUpgrade.SPEED), 1, "faisceaux rapides : ×10 par niveau");
            game.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH, 2);
            assertEquals(2.5e15, game.colliderUpgradeCost(ColliderUpgrade.AUTO_CATCH), 1, "déclencheur : ×50 par niveau");
            // Le prix se paie : à un près, l'achat est refusé ; à l'exact, il est pris.
            game.state().setColliderEnergy(1e10 - 1);
            assertFalse(game.canBuyColliderUpgrade(ColliderUpgrade.COLLISIONS));
            game.state().setColliderEnergy(1e10);
            assertTrue(game.buyColliderUpgrade(ColliderUpgrade.COLLISIONS));
            assertEquals(0, game.colliderEnergy(), 1e-6);
            assertEquals(1, game.colliderUpgradeLevel(ColliderUpgrade.COLLISIONS));
        }

        @Test
        void laChaineRenforceLeResteDuJeu() {
            Game game = bare(0);
            BigNum particles = game.particlesPerCreation();
            BigNum atoms = game.atomsPerFusion();
            double dark = game.darkMatterMomentum();
            game.state().setColliderUpgradeLevel(ColliderUpgrade.PARTICLES, 2);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.ATOMS, 3);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.DARK_MATTER, 5);
            assertEquals(particles.toDouble() * 2.25, game.particlesPerCreation().toDouble(), particles.toDouble() * 1e-9);
            assertEquals(atoms.toDouble() * 1.3, game.atomsPerFusion().toDouble(), atoms.toDouble() * 1e-6);
            assertEquals(dark * 2, game.darkMatterMomentum(), dark * 1e-9);
            Game space = bare(0);
            GameTestSupport.bangs(space, 1);
            BigNum before = space.steadySpacePerSecond();
            space.state().setColliderUpgradeLevel(ColliderUpgrade.SPACE, 2);
            assertEquals(before.toDouble() * 1.5, space.steadySpacePerSecond().toDouble(), 1e-9);
            space.state().setColliderUpgradeLevel(ColliderUpgrade.MOLECULES, 4);
            assertEquals(0.2, space.moleculeDoublingChance(), 1e-9);
        }

        @Test
        void lesHuitGenerateursArriventEnQuelquesHeures() {
            // Un joueur qui ne fait qu'acheter, du plus haut au plus bas, sans cliquer.
            Game game = bare(0);
            double hours = 0;
            while (game.state().colliderGeneratorsBought(ColliderGenerator.PLANCK) == 0 && hours < 24) {
                for (int i = 0; i < 60; i++) game.tick(1);
                for (int k = ColliderGenerator.values().length - 1; k >= 0; k--) game.buyMaxColliderGenerator(ColliderGenerator.values()[k]);
                hours += 1 / 60.0;
            }
            assertTrue(hours > 1.5 && hours < 5, "le collisionneur de Planck à " + hours + " h");
        }
    }
    @Nested
    class SuccesDuBigBang {

        @Test
        void ilsSOuvrentAuPremierBigBangEtRenforcentLeurGrandeur() {
            Game game = bare(0);
            assertFalse(game.isBangAchievementsUnlocked());
            assertEquals(1, game.bangAchievementMultiplier(idle.core.bigbang.BangAchievement.Reward.SPACE), 0);
            BigNum particles = game.particlesPerCreation();
            GameTestSupport.bangs(game, 3);
            assertTrue(game.isBangAchievementsUnlocked());
            assertTrue(game.hasBangAchievement(idle.core.bigbang.BangAchievement.BANGS_1));
            assertTrue(game.hasBangAchievement(idle.core.bigbang.BangAchievement.BANGS_3));
            assertFalse(game.hasBangAchievement(idle.core.bigbang.BangAchievement.BANGS_5));
            assertEquals(3, game.bangAchievementProgress(idle.core.bigbang.BangAchievement.BANGS_5));
            assertEquals(1.10, game.bangAchievementMultiplier(idle.core.bigbang.BangAchievement.Reward.ATOMS), 1e-9);
            assertEquals(particles.toDouble() * 1.25, game.particlesPerCreation().toDouble(), particles.toDouble() * 1e-9,
                    "trois Big Bangs : particules +25 %");
            game.state().addBody("rocky_planet");
            assertEquals(1, game.bangAchievementProgress(idle.core.bigbang.BangAchievement.BODIES_5));
        }
    }

    /** Les outils de test de la version 0.063 : la série de croisements captés, et les remises à zéro par acte. */
    @Nested
    class OutilsDeTest {

        @Test
        void laSerieCompteLesCroisementsCaptesALaMainEtRepartAuPremierManque() {
            Game game = opened(0);
            game.state().setColliderGeneratorsBought(ColliderGenerator.BEAM, 1);
            for (int i = 0; i < 5; i++) {
                int guard = 0;
                while (!game.canCatchCrossing() && guard++ < 10_000) game.tick(0.01);
                assertTrue(game.catchCrossing() > 0);
                // On laisse passer le milieu du croisement, sans le compter comme manqué.
                guard = 0;
                while (game.isCrossing() && guard++ < 10_000) game.tick(0.01);
            }
            assertEquals(5, game.catchStreak());
            // Un tour entier sans clic : le croisement suivant passe, la série repart de zéro.
            game.tick(game.colliderPeriod() * 1.2);
            assertEquals(0, game.catchStreak());
        }

        @Test
        void viderLeCollisionneurNeToucheQuALui() {
            Game game = opened(5e6);
            game.state().setColliderGeneratorsBought(ColliderGenerator.BEAM, 3);
            game.state().setQuarks(Quark.UP, 12);
            game.state().setColliderEnergyProduced(Game.BLACK_HOLE_ENERGY);
            game.state().setParticles(BigNum.of(1234));
            game.state().clearCollider();
            assertEquals(0, game.colliderEnergy());
            assertEquals(0, game.colliderEnergyProduced());
            assertEquals(0, game.state().colliderGeneratorsBought(ColliderGenerator.BEAM));
            assertEquals(0, game.quarks(Quark.UP));
            assertEquals(0, game.colliderUpgradeLevel(ColliderUpgrade.COLLISIONS));
            assertEquals(BigNum.of(1234), game.state().particles());
            assertEquals(1, game.state().explosions());
        }

        @Test
        void refaireLActeDuBigBangGardeLaMatiereNoireEtLeCollisionneur() {
            Game game = opened(5e6);
            GameTestSupport.bangs(game, 3);
            game.state().setMoleculeCount("H2O", 4);
            game.state().addBody("rocky_planet");
            game.state().setSpace(BigNum.of(5000));
            game.state().setDarkMatter(BigNum.of(7));
            game.state().clearBigBangAct();
            assertEquals(0, game.bigBangs());
            assertEquals(0, game.moleculeCount("H2O"));
            assertFalse(game.hasBody("rocky_planet"));
            assertTrue(game.state().space().isZero());
            assertEquals(BigNum.of(7), game.state().darkMatter());
            assertEquals(5e6, game.colliderEnergy());
        }
    }
}
