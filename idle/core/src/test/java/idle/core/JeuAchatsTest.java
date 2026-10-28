package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les paliers de vitesse, le couplage, les achats groupés, le temps restant et la surcharge.
 */
class JeuAchatsTest extends GameTestSupport {


    @Nested
    class PaliersDeVitesse {

        /** Une vitesse qui n'accélère rien, mais triple les particules tous les cinq niveaux. */
        private final Upgrade stepped = new Upgrade("speed", "Vitesse", Resource.PARTICLES,
                BigNum.of(1), 1, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(1, 5, 3));

        private Game steppedGame() {
            Game game = new Game(new GameState(), List.of(stepped, GENERATOR, KEEP), List.of(AUTO_FUSION), new Random(42));
            game.start();
            game.state().setParticles(BigNum.of(1e6));
            return game;
        }

        @Test
        void rienAvantLePremierPalier() {
            Game game = steppedGame();
            assertEquals(4, game.buy("speed", 4));
            assertEquals(0, game.speedMilestones());
            assertEquals(BigNum.ONE, game.speedMilestoneMultiplier());
            assertValue(1, game.particlesPerCreation());
            assertEquals(5, game.nextSpeedMilestone("speed"));
        }

        @Test
        void chaquePalierMultiplieLesParticulesEtPasLaVitesse() {
            Game game = steppedGame();
            game.buy("speed", 5);
            assertEquals(1, game.speedMilestones());
            assertValue(3, game.particlesPerCreation());
            assertValue(BASE, game.speed());                  // l'animation ne s'emballe pas
            assertValue(BASE * 3, game.productionPerSecond());
            assertEquals(10, game.nextSpeedMilestone("speed"));

            game.buy("speed", 6);                             // niveau 11 : deux paliers
            assertEquals(2, game.speedMilestones());
            assertValue(9, game.particlesPerCreation());
            assertEquals(15, game.nextSpeedMilestone("speed"));
        }

        @Test
        void lesNiveauxOffertsParLaMatiereNoireComptent() {
            Game game = steppedGame();
            game.state().setDarkLevel("dark_priming", 1);     // dix niveaux de vitesse offerts
            assertEquals(10, game.speedLevels("speed"));
            assertEquals(2, game.speedMilestones());
            assertValue(9, game.particlesPerCreation());
            game.buy("speed", 5);
            assertEquals(3, game.speedMilestones());
        }

        @Test
        void laFusionLesReprendSaufAvecPersistance() {
            Game game = steppedGame();
            game.buy("speed", 5);
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(0, game.speedMilestones());

            Game keeper = steppedGame();
            keeper.state().setAtoms(BigNum.of(1));
            keeper.buy("keep");
            keeper.buy("speed", 5);
            keeper.buy("gen", 2);
            assertTrue(keeper.fuse());
            assertEquals(1, keeper.speedMilestones());
        }

        @Test
        void uneVitesseSansPalierNEnAJamais() {
            Game game = gameWithParticles(1e30);
            game.buy("speed", 60);
            assertEquals(0, game.speedMilestones());
            assertEquals(BigNum.ONE, game.speedMilestoneMultiplier());
            assertEquals(0, game.nextSpeedMilestone("speed"));
            assertEquals(0, game.nextSpeedMilestone("gen"));   // pas une amélioration de vitesse
        }

        @Test
        void unPalierMalDefiniEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplySpeed(1.1, -1, 2));
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplySpeed(1.1, 25, 0.5));
            assertFalse(new Effect.MultiplySpeed(1.1).hasMilestones());
            assertEquals(3, new Effect.MultiplySpeed(1.1, 25, 2).milestonesAt(80));
        }

        @Test
        void dansLeJeuLesParticulesDoublentTousLes25Niveaux() {
            Game game = GameTestSupport.realGame();
            Upgrade speed = game.upgrades().stream().filter(u -> u.id().equals("speed")).findFirst().orElseThrow();
            assertEquals(new Effect.MultiplySpeed(1.10, Upgrades.SPEED_MILESTONE_EVERY, 2), speed.effect());
            assertEquals(25, game.nextSpeedMilestone("speed"));
            game.state().setLevel("speed", 24);
            assertValue(1, game.particlesPerCreation());
            game.state().setLevel("speed", 25);
            assertValue(2, game.particlesPerCreation());
            game.state().setLevel("speed", 75);
            assertValue(8, game.particlesPerCreation());
        }
    }

    @Nested
    class Couplage {

        /** +50 % de particules par niveau et par autre générateur. */
        private final Upgrade coupling = new Upgrade("coupling", "Couplage", Resource.PARTICLES,
                BigNum.of(1), 1, Upgrade.NO_LIMIT, new Effect.MultiplyByGenerators(0.5));

        private Game coupledGame() {
            Game game = new Game(new GameState(), List.of(GENERATOR, coupling, KEEP),
                    List.of(Automation.buying("autoCoupling", "Couplage", BigNum.of(2), "coupling").withCadence(0, 0, BigNum.ONE, 1)),
                    new Random(42));
            game.start();
            game.state().setParticles(BigNum.of(1e6));
            return game;
        }

        @Test
        void sansEffetAvecUnSeulGenerateur() {
            Game game = coupledGame();
            assertEquals(2, game.buy("coupling", 2));
            assertValue(1, game.particlesPerCreation());
            assertValue(1, game.particlesMultiplier("coupling", 2));
            assertValue(BASE, game.productionPerSecond());
        }

        @Test
        void chaqueAutreGenerateurAjouteSaPart() {
            Game game = coupledGame();
            game.buy("coupling", 2);
            game.buy("gen");                                  // 2 générateurs : 1 + 0,5 × 2 × 1
            assertValue(2, game.particlesPerCreation());
            game.buy("gen");                                  // 3 générateurs : 1 + 0,5 × 2 × 2
            assertValue(3, game.particlesPerCreation());
            assertValue(3, game.particlesMultiplier("coupling", 2));
            assertValue(4, game.particlesMultiplier("coupling", 3));   // ce que donnerait un niveau de plus
            assertValue(BASE * 3 * 3, game.productionPerSecond());
        }

        @Test
        void laProductionAuMomentDeFusionnerCompteTousLesGenerateurs() {
            Game game = coupledGame();
            game.buy("coupling", 2);                          // un seul générateur pour l'instant
            assertValue(BASE, game.productionPerSecond());
            assertValue(BASE * 3 * 3, game.productionAtFusion());   // comme s'ils étaient déjà trois
        }

        @Test
        void laFusionLeReprendSaufAvecPersistance() {
            Game game = coupledGame();
            game.buy("coupling", 3);
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(0, game.levelOf("coupling"));

            Game keeper = coupledGame();
            keeper.state().setAtoms(BigNum.of(1));
            keeper.buy("keep");
            keeper.buy("coupling", 3);
            keeper.buy("gen", 2);
            assertTrue(keeper.fuse());
            assertEquals(3, keeper.levelOf("coupling"));
        }

        @Test
        void sonAutomatismeLAcheteToutSeul() {
            Game game = coupledGame();
            game.state().setAtoms(BigNum.of(10));
            createEnoughAtoms(game);
            assertTrue(game.buyAutomation("autoCoupling"));
            game.tick(0.35);                                  // une action tous les 0,1 s
            assertEquals(3, game.levelOf("coupling"));
        }

        @Test
        void unBonusNegatifEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplyByGenerators(-0.1));
        }

        @Test
        void leJeuASonCouplageEtSonAutomatisme() {
            Game game = GameTestSupport.realGame();
            Upgrade upgrade = game.upgrades(Resource.PARTICLES).stream()
                    .filter(u -> u.id().equals("coupling")).findFirst().orElseThrow();
            assertEquals(new Effect.MultiplyByGenerators(0.02), upgrade.effect());
            Automation automation = game.automations().stream()
                    .filter(a -> a.id().equals("auto_coupling")).findFirst().orElseThrow();
            assertEquals("coupling", automation.upgradeId());
            // Trois améliorations en particules : la vitesse, le couplage et les générateurs.
            assertEquals(3, game.upgrades(Resource.PARTICLES).size());
        }
    }

    @Nested
    class AchatsGroupes {

        /** Vitesse à 10, 15, 23, 34, 51… particules ; générateur à 100 puis 1000, deux au plus. */
        private Game simpleGame(double particles) {
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR), List.of(AUTO_FUSION), new Random(42));
            game.start();
            game.state().setParticles(BigNum.of(particles));
            return game;
        }

        @Test
        void leCoutDePlusieursNiveauxEstLaSommeDesCouts() {
            Game game = simpleGame(0);
            assertValue(10, game.costOf("speed", 1));
            assertValue(10 + 15 + 23, game.costOf("speed", 3));
            game.state().setLevel("speed", 1);
            assertValue(15 + 23, game.costOf("speed", 2));
        }

        @Test
        void leCoutSArreteAuMaximumDeLAmelioration() {
            Game game = simpleGame(0);
            assertValue(100 + 1000, game.costOf("gen", 50));  // deux générateurs à acheter, pas cinquante
            game.state().setLevel("gen", 2);
            assertValue(10000, game.costOf("gen", 50));       // au maximum : le prix qu'aurait le suivant
        }

        @Test
        void onCompteCeQueLeJoueurPeutPayer() {
            Game game = simpleGame(50);                       // 10 + 15 + 23 = 48
            assertEquals(3, game.affordableLevels("speed", 10));
            assertEquals(2, game.affordableLevels("speed", 2));
            assertEquals(3, game.affordableLevels("speed", Integer.MAX_VALUE));
            assertEquals(0, game.affordableLevels("gen", 10));
            game.state().setParticles(BigNum.of(1e9));
            assertEquals(2, game.affordableLevels("gen", 10)); // limité par le maximum
        }

        @Test
        void unAchatGroupePrendCeQuiEstAPortee() {
            Game game = simpleGame(50);
            assertEquals(3, game.buy("speed", 10));
            assertEquals(3, game.levelOf("speed"));
            assertValue(2, game.state().particles());
            assertEquals(0, game.buy("speed", 10));           // plus rien à portée
        }

        @Test
        void unAchatGroupeNeDepassePasLeNombreDemande() {
            Game game = simpleGame(1e9);
            assertEquals(2, game.buy("speed", 2));
            assertEquals(2, game.levelOf("speed"));
            assertEquals(2, game.buy("gen", Integer.MAX_VALUE));
            assertEquals(3, game.generatorCount());
        }

        @Test
        void rienNeSAcheteAvantLeDemarrage() {
            Game game = unstartedGame();
            game.state().setParticles(BigNum.of(1e9));
            assertEquals(0, game.affordableLevels("speed", 10));
            assertEquals(0, game.buy("speed", 10));
            assertEquals(0, game.buyAllWithParticles());
        }

        @Test
        void toutAcheterPrendChaqueFoisLeMoinsCher() {
            Game game = simpleGame(130);                      // vitesse : 10, 15, 23, 34 = 82 ; la suivante vaut 51
            assertEquals(4, game.buyAllWithParticles());
            assertEquals(4, game.levelOf("speed"));
            assertEquals(1, game.generatorCount());           // le générateur à 100 n'a jamais été le moins cher payable
            assertValue(48, game.state().particles());
        }

        @Test
        void toutAcheterVaJusquAuBout() {
            Game game = simpleGame(1000);
            int bought = game.buyAllWithParticles();
            assertEquals(bought, game.levelOf("speed") + game.levelOf("gen"));
            assertEquals(2, game.generatorCount());           // le premier générateur (100) est passé, pas le second (1000)
            assertFalse(game.canBuy("speed"));
            assertFalse(game.canBuy("gen"));
            assertEquals(0, game.buyAllWithParticles());
        }

        @Test
        void lesAchatsGroupesComptentDansLesStatistiques() {
            Game game = simpleGame(50);
            game.buy("speed", 10);
            assertEquals(3, game.stats().particleUpgradesBought());
        }
    }

    @Nested
    class TempsRestant {

        @Test
        void leTempsAvantUnAchatSuitLaProduction() {
            Game game = newGame();                            // une particule toutes les ONE_PARTICLE secondes
            assertEquals(10 * ONE_PARTICLE, game.secondsUntilParticles(BigNum.of(10)), 1e-6);
            game.state().setParticles(BigNum.of(5));
            assertEquals(5 * ONE_PARTICLE, game.secondsUntilParticles(BigNum.of(10)), 1e-6);
            game.state().setParticles(BigNum.of(10));
            assertEquals(0, game.secondsUntilParticles(BigNum.of(10)), 0);
        }

        @Test
        void lAttenteDiminueAvecLeTempsEtPasSeulementAChaqueParticule() {
            double one = ONE_PARTICLE;                        // le temps d'une création
            Game game = newGame();
            assertEquals(one, game.secondsUntilParticles(BigNum.of(1)), 1e-6);
            game.tick(one / 4);                               // un quart de la création est fait
            assertEquals(0.75 * one, game.secondsUntilParticles(BigNum.of(1)), 1e-6);
            game.tick(0.625 * one);
            assertEquals(0.125 * one, game.secondsUntilParticles(BigNum.of(1)), 1e-6);
            assertEquals(1.125 * one, game.secondsUntilParticles(BigNum.of(2)), 1e-6);   // la suivante, une création plus tard
            game.tick(0.125 * one);                           // la particule arrive : rien ne saute
            assertEquals(0, game.secondsUntilParticles(BigNum.of(1)), 0);
            assertEquals(one, game.secondsUntilParticles(BigNum.of(2)), 1e-6);
        }

        @Test
        void lAttenteCompteLesCreationsDeChaqueGenerateur() {
            Game game = gameWithParticles(1100);
            game.buy("gen", 2);                               // trois générateurs, plus rien en poche
            game.tick(ONE_PARTICLE / 4);
            // Trois créations en cours, toutes au quart : les trois particules arrivent ensemble aux trois quarts
            // d'une création, alors que la moyenne (2 particules manquantes, trois générateurs) annoncerait moins.
            assertEquals(0.75 * ONE_PARTICLE, game.secondsUntilParticles(BigNum.of(2)), 1e-6);
            assertEquals(1.75 * ONE_PARTICLE, game.secondsUntilParticles(BigNum.of(4)), 1e-6);   // puis la fournée suivante
        }

        @Test
        void auxGrandsNombresLAttenteEstLaMoyenne() {
            Game game = gameWithParticles(0);
            game.state().setLevel("huge", 1);                  // une vitesse gigantesque
            BigNum cost = game.productionPerSecond().multiply(90);
            assertEquals(90, game.secondsUntilParticles(cost), 1e-6);
        }

        @Test
        void sansProductionLAttenteEstInfinie() {
            Game game = unstartedGame();
            assertEquals(Double.POSITIVE_INFINITY, game.secondsUntilParticles(BigNum.of(10)), 0);
        }

        @Test
        void lesFusionsAvantUnAchatSuiventLesAtomesParFusion() {
            Game game = newGame();                            // 1 atome par fusion
            game.state().setAtoms(BigNum.of(1));
            assertEquals(0, game.fusionsUntilAtoms(BigNum.of(1)));
            assertEquals(3, game.fusionsUntilAtoms(BigNum.of(4)));
            game.state().setAtoms(BigNum.of(1.5));
            assertEquals(3, game.fusionsUntilAtoms(BigNum.of(4)));   // 2,5 atomes manquent : trois fusions
        }

        @Test
        void unPrixAuDessusDuPlafondDAtomesEstHorsDePortee() {
            Game game = newGame();
            assertEquals(-1, game.fusionsUntilAtoms(BigNum.of(200)));
            game.state().setDarkLevel("dark_overflow", 1);    // plafond levé
            assertEquals(200, game.fusionsUntilAtoms(BigNum.of(200)));
        }
    }

    @Nested
    class Surcharge {

        private final Upgrade overload = new Upgrade("over", "Surcharge", Resource.ATOMS,
                BigNum.of(1), 1, Upgrade.NO_LIMIT, new Effect.Overload(1.5));

        private Game overloadGame() {
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR, DOUBLE, overload), List.of(AUTO_FUSION), new Random(42));
            game.start();
            game.state().setAtoms(BigNum.of(50));
            return game;
        }

        @Test
        void chaqueNiveauMultiplieLesParticules() {
            Game game = overloadGame();
            game.buy("over");
            assertValue(1.5, game.particlesPerCreation());
            game.buy("over");
            assertValue(2.25, game.particlesPerCreation());
            assertValue(2.25, game.particlesMultiplier("over", 2));
            assertValue(48, game.state().atoms());
        }

        @Test
        void lesElementsNeLaRenforcentPas() {
            Game game = overloadGame();
            game.buy("over");
            game.buy("double");
            game.state().setElementCount(1, 1);               // l'hydrogène renforce le Dédoublement
            assertTrue(game.particlesMultiplier("double", 1).gt(BigNum.of(2)));
            assertValue(1.5, game.particlesMultiplier("over", 1));
        }

        @Test
        void laFusionLaConserve() {
            Game game = overloadGame();
            game.buy("over");
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(1, game.levelOf("over"));
        }

        @Test
        void uneSurchargeQuiReduitEstRefusee() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.Overload(0.9));
        }

        @Test
        void dansLeJeuElleResteSousLePlafondDAtomesPendantTreizeNiveaux() {
            Game game = GameTestSupport.realGame();
            Upgrade upgrade = game.upgrades(Resource.ATOMS).stream()
                    .filter(u -> u.id().equals("atom_overload")).findFirst().orElseThrow();
            assertEquals(new Effect.Overload(1.3), upgrade.effect());
            assertFalse(upgrade.hasLimit());
            assertValue(12, game.costOf("atom_overload"));
            game.state().setLevel("atom_overload", 12);
            assertTrue(game.costOf("atom_overload").lte(Game.MAX_ATOMS));
            game.state().setLevel("atom_overload", 13);
            assertTrue(game.costOf("atom_overload").gt(Game.MAX_ATOMS));
            assertEquals(-1, game.fusionsUntilAtoms(game.costOf("atom_overload")));
        }
    }
}
