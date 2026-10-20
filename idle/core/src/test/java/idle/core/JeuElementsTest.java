package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.Element;
import idle.core.elements.ElementEffect;
import idle.core.elements.ElementSet;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les effets des éléments, le rendement et la synthèse automatique.
 */
class JeuElementsTest extends GameTestSupport {


    @Nested
    class EffetsDesElements {

        /** Partie avec trois générateurs et rien d'autre : ni particule, ni amélioration de vitesse. */
        private Game threeGenerators() {
            Game game = gameWithParticles(1e9);
            game.buy("gen");
            game.buy("gen");
            game.state().setParticles(BigNum.ZERO);
            return game;
        }

        /** Partie dont les quatre automatismes agissent toutes les 4 secondes, pour comparer leurs délais. */
        private Game cadenceGame() {
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR), List.of(
                    Automation.buying("s", "Vitesse", BigNum.ONE, "speed").withCadence(4, 0, BigNum.ONE, 1),
                    Automation.buying("g", "Générateur", BigNum.ONE, "gen").withCadence(4, 0, BigNum.ONE, 1),
                    Automation.fusing("f", "Fusion", BigNum.ONE).withCadence(4, 0, BigNum.ONE, 1),
                    Automation.synthesizing("y", "Synthèse", BigNum.ONE).withCadence(4, 0, BigNum.ONE, 1)));
            game.start();
            return game;
        }

        @Test
        void sansElementAucunBonus() {
            Game game = threeGenerators();
            for (ElementEffect.Stat stat : ElementEffect.Stat.values()) {
                assertEquals(1, game.elementMultiplier(stat));
            }
            for (ElementEffect.CostTarget target : ElementEffect.CostTarget.values()) {
                assertEquals(1, game.elementCostDivisor(target));
            }
            for (int generator = 0; generator < 3; generator++) {
                assertEquals(1, game.generatorSpeedMultiplier(generator));
                assertEquals(1, game.generatorParticlesMultiplier(generator));
            }
        }

        @Test
        void unMetalDeTransitionNAmelioreQueLesParticulesDeSonGenerateur() {
            Game game = threeGenerators();
            game.state().setElementCount(22, 1);              // titane : générateur 2, particules ×1,5
            assertValue(1, game.particlesPerCreation(0));
            assertValue(1.5, game.particlesPerCreation(1));
            assertValue(1, game.particlesPerCreation(2));
            assertValue(BASE * 3.5, game.productionPerSecond());

            game.tick(ONE_PARTICLE);                          // chaque générateur termine une création
            assertValue(3.5, game.state().particles());
        }

        @Test
        void unMetalDeTransitionNAmelioreQueLaVitesseDeSonGenerateur() {
            Game game = threeGenerators();
            game.state().setElementCount(41, 1);              // niobium : générateur 3, vitesse ×1,25
            assertValue(BASE, game.speed(0));
            assertValue(BASE, game.speed(1));
            assertValue(BASE * 1.25, game.speed(2));

            game.tick(ONE_PARTICLE / 2);
            assertEquals(0.5, game.state().formation(0), 1e-9);
            assertEquals(0.625, game.state().formation(2), 1e-9);
        }

        @Test
        void lesDeuxPeriodesDUneMemeColonneSeMultiplient() {
            Game game = threeGenerators();
            game.state().setElementCount(22, 1);              // titane : générateur 2, ×1,5
            game.state().setElementCount(72, 1);              // hafnium : générateur 2, ×2
            assertValue(3, game.particlesPerCreation(1));
            game.state().setElementCount(40, 1);              // zirconium : générateur 2, vitesse ×1,25
            game.state().setElementCount(104, 1);             // rutherfordium : générateur 2, vitesse ×1,5
            assertValue(BASE * 1.875, game.speed(1));
            assertValue(BASE * (1 + 3 * 1.875 + 1), game.productionPerSecond());
        }

        @Test
        void lesDoublonsRapportentDeMoinsEnMoins() {
            Game game = threeGenerators();
            game.state().setElementCount(22, 4);              // quatre titanes : ×1,5 deux fois, pas quatre
            assertValue(2.25, game.particlesPerCreation(1));
            game.state().setElementCount(22, 9);              // neuf : trois fois
            assertValue(3.375, game.particlesPerCreation(1));
        }

        @Test
        void lesAlcalinoTerreuxDonnentPlusDAtomes() {
            Game game = gameReadyToFuse();
            assertValue(1, game.atomsPerFusion());
            game.state().setElementCount(4, 1);               // béryllium : +1,5 %
            game.state().setElementCount(88, 1);              // radium : +5 %
            assertValue(1.065, game.atomsPerFusion());
            game.fuse();
            assertValue(1.065, game.state().atoms());
            assertValue(1.065, game.state().totalAtoms());
        }

        @Test
        void lesLanthanidesSeMultiplientEntreEux() {
            Game game = threeGenerators();
            game.state().setElementCount(57, 1);              // lanthane : particules ×1,3
            game.state().setElementCount(58, 1);              // cérium : particules ×1,3
            assertEquals(1.69, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            assertValue(1.69, game.particlesPerCreation());

            game.state().setElementCount(62, 1);              // samarium : vitesse ×1,12
            game.state().setElementCount(67, 1);              // holmium : atomes ×1,03
            assertValue(BASE * 1.12, game.speed());
            assertValue(1.03, game.atomsPerFusion());
        }

        @Test
        void unLanthanideEnDoubleCompteCommeUnExposant() {
            Game game = threeGenerators();
            game.state().setElementCount(57, 4);              // 1,3 à la puissance √4
            assertEquals(1.69, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void lesGazNoblesApportentUnBonusMajeur() {
            Game game = threeGenerators();
            game.state().setElementCount(2, 1);               // hélium : particules ×2
            game.state().setElementCount(10, 1);              // néon : vitesse ×1,5
            game.state().setElementCount(18, 1);              // argon : atomes ×1,15
            assertValue(2, game.particlesPerCreation());
            assertValue(BASE * 1.5, game.speed());
            assertValue(1.15, game.atomsPerFusion());
            game.state().setElementCount(118, 1);             // oganesson : ×1,25 partout
            // Quatre gaz nobles sur sept : la moitié de leur ensemble, soit le quart de son ×1,25 partout.
            double half = Math.pow(1.25, ElementSet.HALF_STRENGTH);
            assertValue(2.5 * half, game.particlesPerCreation());
            assertValue(BASE * 1.875 * half, game.speed());
            assertValue(1.15 * 1.25 * half, game.atomsPerFusion());
        }

        @Test
        void lesActinidesDoublentParticulesEtVitesse() {
            Game game = threeGenerators();
            game.state().setElementCount(92, 1);              // uranium : particules ×2 et vitesse ×2
            assertValue(BASE * 3 * 4, game.productionPerSecond());
            game.state().setElementCount(94, 1);              // plutonium : pareil
            assertEquals(4, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            assertEquals(4, game.elementMultiplier(ElementEffect.Stat.SPEED), 1e-12);
            assertEquals(1, game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);
            assertValue(4, game.particlesPerCreation());
            assertValue(BASE * 4, game.speed());
            assertValue(BASE * 3 * 16, game.productionPerSecond());
            assertValue(1, game.atomsPerFusion());
        }

        @Test
        void additifsEtMultiplicatifsSeCombinent() {
            Game game = gameReadyToFuse();
            game.state().setElementCount(4, 1);               // béryllium : atomes +1,5 %
            game.state().setElementCount(18, 1);              // argon : atomes ×1,15
            assertValue(1.015 * 1.15, game.atomsPerFusion());
        }

        @Test
        void lesMetauxPauvresReduisentChacunUnPrix() {
            Game game = threeGenerators();
            game.state().setLevel("gen", 0);
            assertValue(100, game.costOf("gen"));
            assertValue(10, game.costOf("speed"));

            game.state().setElementCount(113, 1);             // nihonium : vitesse, diviseur 1,2
            assertValue(Math.ceil(10 / 1.2), game.costOf("speed"));
            assertValue(100, game.costOf("gen"));             // les générateurs ne sont pas concernés
            game.state().setElementCount(13, 1);              // aluminium en plus : diviseur 1,25
            assertValue(8, game.costOf("speed"));

            assertValue(2, game.costOf("mass"));
            game.state().setElementCount(114, 25);            // flérovium : améliorations en atomes, diviseur 1 + 0,2 × 5
            assertValue(1, game.costOf("mass"));
            assertValue(1, game.costOf("double"));            // jamais moins d'un atome
            game.state().setElementCount(115, 100);           // moscovium en plus : diviseur 3
            game.state().setLevel("double", 3);               // prix de base 27
            assertValue(9, game.costOf("double"));
        }

        @Test
        void lePrixDeLaSynthesePeutBaisser() {
            Game game = threeGenerators();
            game.state().setSynthesisCount(20);               // prix au plafond
            assertValue(118, game.synthesisCost());
            game.state().setElementCount(84, 1);              // polonium : diviseur 1,05
            assertValue(Math.ceil(118 / 1.05), game.synthesisCost());
            game.state().setElementCount(54, 1);              // xénon : diviseur +0,5
            assertValue(Math.ceil(118 / 1.55), game.synthesisCost());
        }

        @Test
        void chaqueMetalAlcalinAccelereSonAutomatisme() {
            Game game = cadenceGame();
            for (String id : List.of("s", "g", "f", "y")) assertEquals(4, game.automationInterval(id));

            game.state().setElementCount(3, 1);               // lithium : achat de la vitesse
            assertEquals(4 / 1.09, game.automationInterval("s"), 1e-12);
            assertEquals(4, game.automationInterval("g"));

            game.state().setElementCount(11, 1);              // sodium : achat des générateurs
            assertEquals(4 / 1.09, game.automationInterval("g"), 1e-12);
            game.state().setElementCount(19, 1);              // potassium : fusion
            game.state().setElementCount(37, 1);              // rubidium : synthèse
            // Quatre alcalins sur six : la moitié de leur ensemble, soit le quart de ses +20 % pour tous.
            for (String id : List.of("s", "g", "f", "y")) assertEquals(4 / 1.14, game.automationInterval(id), 1e-12);
        }

        @Test
        void cesiumEtFranciumAccelerentTousLesAutomatismes() {
            Game game = cadenceGame();
            game.state().setElementCount(55, 1);              // césium : tous, +6 %
            for (String id : List.of("s", "g", "f", "y")) {
                assertEquals(4 / 1.06, game.automationInterval(id), 1e-12);
            }
            game.state().setElementCount(3, 1);               // lithium en plus : les bonus s'additionnent
            assertEquals(4 / 1.15, game.automationInterval("s"), 1e-12);
            assertEquals(4 / 1.06, game.automationInterval("g"), 1e-12);
        }

        @Test
        void lHydrogeneRenforceLeDedoublement() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.buy("double");
            game.buy("double");
            assertValue(4, game.particlesPerCreation());
            game.state().setElementCount(1, 1);               // ×2 par niveau devient ×2,1
            assertValue(2.1 * 2.1, game.particlesPerCreation());
        }

        @Test
        void leCarboneRenforceLaMasseAtomique() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.state().setTotalAtoms(BigNum.of(10));
            game.buy("mass");                                 // 1 + 0,5 × 10
            assertValue(6, game.particlesPerCreation());
            game.state().setElementCount(6, 1);               // +0,01 par atome
            assertValue(1 + 0.51 * 10, game.particlesPerCreation());
        }

        @Test
        void lAzoteRenforceLaPatience() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.buy("patience");
            game.tick(60);                                    // 1 + 1 × √1 minute
            assertValue(2, game.particlesPerCreation());
            game.state().setElementCount(7, 1);               // facteur +0,05
            assertValue(2.05, game.particlesPerCreation());
        }

        @Test
        void lOxygeneRenforceLeCatalyseur() {
            Game game = gameWithParticles(1e9);
            game.state().setAtoms(BigNum.of(10));
            game.buy("speed");
            game.buy("boost");                                // chaque niveau de vitesse : ×2 devient ×2,5
            assertValue(BASE * 2.5, game.speed());
            game.state().setElementCount(8, 1);               // le Catalyseur donne 0,002 de plus par niveau
            assertValue(BASE * 2.502, game.speed());
        }

        @Test
        void leSoufreRenforceLaVitesseDeCreation() {
            Game game = gameWithParticles(1e9);
            game.buy("speed");
            game.buy("speed");
            assertValue(BASE * 4, game.speed());
            game.state().setElementCount(16, 1);              // ×2 par niveau devient ×2,005
            assertValue(BASE * 2.005 * 2.005, game.speed());
        }

        @Test
        void lesElementsNAjoutentPasPlusDeDixPointsALaVitesse() {
            Game game = gameWithParticles(1e9);
            game.buy("speed");
            game.state().setElementCount(16, 100);            // soufre : 0,005 × 10
            assertEquals(0.05, game.speedExtraPerLevel(), 1e-12);
            game.state().setElementCount(16, 1_000_000);      // une quantité absurde de soufre
            assertEquals(Game.MAX_ELEMENT_SPEED_EXTRA, game.speedExtraPerLevel(), 1e-12);
            assertValue(BASE * 2.1, game.speed());

            game.state().setAtoms(BigNum.of(10));
            game.buy("boost");                                // le Catalyseur, lui, s'ajoute toujours en entier
            game.state().setElementCount(8, 1_000_000);       // et l'oxygène ne fait pas sauter le plafond
            assertEquals(0.5 + Game.MAX_ELEMENT_SPEED_EXTRA, game.speedExtraPerLevel(), 1e-12);
            assertValue(BASE * 2.6, game.speed());
        }

        @Test
        void leBoreGranditAvecLaCollection() {
            Game game = threeGenerators();
            game.state().setElementCount(5, 1);               // +2 % de particules par élément différent
            assertEquals(1.02, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            game.state().setElementCount(3, 1);
            game.state().setElementCount(11, 1);              // trois éléments différents
            assertEquals(1.06, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            game.state().setElementCount(11, 50);             // un doublon ne compte pas
            assertEquals(1.06, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void leSiliciumGranditAvecLesNiveauxDeVitesse() {
            Game game = gameWithParticles(1e9);
            game.state().setElementCount(14, 1);              // +1 % de particules par niveau de vitesse
            assertValue(1, game.particlesPerCreation());
            game.buy("speed");
            game.buy("speed");
            game.buy("speed");
            assertValue(1.03, game.particlesPerCreation());
        }

        @Test
        void leGermaniumEtLeSeleniumGrandissentAvecLesGenerateurs() {
            Game game = gameWithParticles(1e9);
            game.state().setElementCount(32, 1);              // +5 % de particules par générateur
            game.state().setElementCount(34, 1);              // +2 % de vitesse par générateur
            assertValue(1.05, game.particlesPerCreation());
            assertValue(BASE * 1.02, game.speed());
            game.buy("gen");
            game.buy("gen");
            assertValue(1.15, game.particlesPerCreation());
            assertValue(BASE * 1.06, game.speed());
        }

        @Test
        void lArsenicRecompenseLesAtomesGardes() {
            Game game = newGame();
            game.state().setElementCount(33, 1);              // +0,5 % de vitesse par atome disponible
            assertValue(BASE, game.speed());
            game.state().setAtoms(BigNum.of(40));
            assertValue(BASE * 1.2, game.speed());
            game.buy("double");                               // dépenser un atome fait baisser le bonus
            assertValue(BASE * 1.195, game.speed());
        }

        @Test
        void lAntimoineGranditAvecLesAutomatismes() {
            Game game = newGame();
            game.state().setElementCount(51, 1);              // +10 % de particules par automatisme possédé
            game.state().setAtoms(BigNum.of(20));
            game.buy("keep");
            createEnoughAtoms(game);
            assertValue(1, game.particlesPerCreation());
            game.buyAutomation("autoSpeed");
            game.buyAutomation("autoGen");
            game.setAutomationEnabled("autoSpeed", false);
            game.setAutomationEnabled("autoGen", false);
            assertValue(1.2, game.particlesPerCreation());
        }

        @Test
        void leTellureDonneDesAtomesSelonLaCollection() {
            Game game = gameReadyToFuse();
            game.state().setElementCount(52, 1);              // +0,1 % d'atomes par élément différent
            game.state().setElementCount(26, 1);
            assertValue(1.002, game.atomsPerFusion());
        }

        @Test
        void uneSynergieSeMultiplieAvecLesAutresBonus() {
            Game game = threeGenerators();
            game.state().setElementCount(32, 1);              // germanium : +5 % par générateur, soit +15 %
            game.state().setElementCount(2, 1);               // hélium : ×2
            assertValue(2 * 1.15, game.particlesPerCreation());
        }
    }

    @Nested
    class Rendement {

        /** Production d'un générateur à la vitesse de départ, particules doublées : l'unité de ces essais. */
        private static final double UNIT = 2 * BASE;
        /** +50 % d'atomes par fusion pour chaque ×10 de production au-delà de 200 fois cette unité. */
        private static final Upgrade YIELD = new Upgrade("yield", "Rendement", Resource.ATOMS,
                BigNum.of(3), 1, 1, new Effect.MultiplyAtomsByProduction(200 * UNIT, 0.5));
        /** Vitesse ×10 par niveau, pour choisir la production à la puissance de dix près. */
        private static final Upgrade TENFOLD = new Upgrade("tenfold", "×10", Resource.PARTICLES,
                BigNum.ONE, 1, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(10));

        /** Deux générateurs à la vitesse de départ : une fois tous débloqués et les particules doublées, deux unités par seconde. */
        private Game yieldGame() {
            Game game = new Game(new GameState(),
                    List.of(TENFOLD, new Upgrade("gen", "Générateur", Resource.PARTICLES, BigNum.ONE, 1, 1,
                            new Effect.AddGenerator()), DOUBLE, KEEP, YIELD),
                    List.of(), new Random(42));
            game.start();
            game.state().setAtoms(BigNum.of(10));
            return game;
        }

        /** Porte la production à la fusion à {@code 10^levels} générateurs de départ ; le seuil du rendement est à 100 avec les particules doublées. */
        private void setSpeedLevels(Game game, int levels) {
            game.state().setLevel("tenfold", levels);
        }

        @Test
        void sansLAmeliorationLaProductionNeChangeRienAuxAtomes() {
            Game game = yieldGame();
            setSpeedLevels(game, 20);
            assertEquals(1, game.fusionYield());
            assertValue(1, game.atomsPerFusion());
        }

        @Test
        void sousLeSeuilAucunEffet() {
            Game game = yieldGame();
            game.buy("yield");
            setSpeedLevels(game, 2);                          // 50 particules par seconde, seuil à 100
            assertEquals(1, game.fusionYield());
            assertValue(1, game.atomsPerFusion());
        }

        @Test
        void chaqueFoisDixDeProductionAjouteUnBonus() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");                               // particules ×2 : 1 × 10^n par seconde
            setSpeedLevels(game, 2);                          // 100 : pile au seuil
            assertEquals(1, game.fusionYield(), 1e-9);
            setSpeedLevels(game, 3);                          // 1 000 : une puissance de dix au-dessus
            assertEquals(1.5, game.fusionYield(), 1e-9);
            setSpeedLevels(game, 6);                          // 1 000 000 : quatre
            assertEquals(3, game.fusionYield(), 1e-9);
            assertValue(3, game.atomsPerFusion());
            setSpeedLevels(game, 302);                        // bien au-delà de ce que contient un double
            assertEquals(151, game.fusionYield(), 1e-6);
        }

        @Test
        void cEstLaProductionAvecTousLesGenerateursQuiCompte() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 3);
            assertEquals(1, game.generatorCount());           // un seul générateur pour l'instant
            assertValue(1000 * UNIT, game.productionPerSecond());
            assertValue(2000 * UNIT, game.productionAtFusion());     // mais deux au moment de fusionner
            assertEquals(1.5, game.fusionYield(), 1e-9);
        }

        @Test
        void laFusionRapporteLesAtomesMultiplies() {
            Game game = yieldGame();
            game.buy("yield");                                // 3 atomes
            game.buy("double");                               // 1 atome
            setSpeedLevels(game, 4);                          // 10 000 par seconde : +50 % deux fois
            game.state().setParticles(BigNum.of(10));
            game.buy("gen");
            assertTrue(game.fuse());
            assertValue(6 + 2, game.state().atoms());
        }

        @Test
        void seMultiplieAvecLesElements() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 4);                          // rendement ×2
            game.state().setElementCount(18, 1);              // argon : atomes ×1,15
            assertValue(2.3, game.atomsPerFusion());
        }

        @Test
        void lePhosphoreRenforceLeRendement() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 4);                          // deux puissances de dix au-dessus du seuil
            assertEquals(2, game.fusionYield(), 1e-9);
            game.state().setElementCount(15, 1);              // +50 % devient +51 %
            assertEquals(2.02, game.fusionYield(), 1e-9);
            game.state().setElementCount(15, 100);            // cent exemplaires : +60 %
            assertEquals(2.2, game.fusionYield(), 1e-9);
        }

        @Test
        void lesElementsQuiAugmententLaProductionAugmententLeRendement() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 3);                          // 1 000 par seconde
            assertEquals(1.5, game.fusionYield(), 1e-9);
            game.state().setElementCount(92, 1);              // uranium : particules ×2 et vitesse ×2
            assertEquals(1 + 0.5 * Math.log10(40), game.fusionYield(), 1e-9);
        }

        @Test
        void seuilEtBonusDoiventEtreValides() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplyAtomsByProduction(0, 0.5));
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplyAtomsByProduction(100, -1));
        }

        @Test
        void leRendementDuJeuDemarreAQuatreCentsMillionsDeParticulesParSeconde() {
            Game game = new Game();
            game.start();
            Upgrade upgrade = game.upgrades().stream().filter(u -> u.id().equals("atom_yield")).findFirst().orElseThrow();
            assertEquals(Resource.ATOMS, upgrade.resource());
            assertEquals(new Effect.MultiplyAtomsByProduction(50_000_000 * Game.TEMPO, 0.09), upgrade.effect());
        }
    }

    @Nested
    class SyntheseAutomatique {

        /** Partie où le tableau est débloqué, avec {@code atoms} atomes et aucun automatisme en marche. */
        private Game unlockedGame(int atoms) {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(atoms));
            return game;
        }

        @Test
        void verrouilleeTantQuAucunElementUniqueNEstObtenu() {
            Game game = unlockedGame(100);
            game.state().setElementCount(26, 3);              // du fer, même en plusieurs exemplaires, ne suffit pas
            assertFalse(game.isSynthesisAutomationUnlocked());
            assertFalse(game.isAutomationAvailable("autoSynth"));
            assertFalse(game.buyAutomation("autoSynth"));
            assertValue(100, game.state().atoms());
        }

        @Test
        void unGazNobleOuUnActinideLaDebloque() {
            Game noble = unlockedGame(100);
            noble.state().setElementCount(10, 1);             // néon
            assertTrue(noble.isSynthesisAutomationUnlocked());
            assertTrue(noble.isAutomationAvailable("autoSynth"));

            Game actinide = unlockedGame(100);
            actinide.state().setElementCount(92, 1);          // uranium
            assertTrue(actinide.isAutomationAvailable("autoSynth"));
            assertTrue(actinide.buyAutomation("autoSynth"));
            assertValue(96, actinide.state().atoms());
            assertTrue(actinide.isAutomationEnabled("autoSynth"));
        }

        @Test
        void lesAutresAutomatismesNOntPasBesoinDElement() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.buy("keep");
            createEnoughAtoms(game);
            assertTrue(game.isAutomationAvailable("autoSpeed"));
            assertFalse(game.isAutomationAvailable("autoSynth"));
        }

        @Test
        void elleSynthetiseSansClicASonRythme() {
            Game game = unlockedGame(100);
            game.state().setElementCount(92, 1);
            game.buyAutomation("autoSynth");                  // une action toutes les 2 secondes
            game.state().setAtoms(BigNum.of(100));
            game.tick(1.8);
            assertEquals(0, game.state().synthesisCount());
            game.tick(0.4);
            assertEquals(1, game.state().synthesisCount());
            assertValue(98, game.state().atoms());            // la première synthèse coûte 2
            game.tick(4);
            assertEquals(3, game.state().synthesisCount());   // puis 4, puis 8
            assertValue(86, game.state().atoms());
        }

        @Test
        void elleSArreteQuandLeTableauEstComplet() {
            Game game = unlockedGame(100);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.buyAutomation("autoSynth");
            game.state().setAtoms(BigNum.of(100));
            game.tick(30);
            assertEquals(0, game.state().synthesisCount());
            assertValue(100, game.state().atoms());
        }

        @Test
        void laReserveEstVideParDefautEtNEstJamaisNegative() {
            Game game = unlockedGame(100);
            assertEquals(BigNum.ZERO, game.synthesisReserve());
            game.setSynthesisReserve(BigNum.of(25));
            assertValue(25, game.synthesisReserve());
            game.setSynthesisReserve(BigNum.of(-3));
            assertEquals(BigNum.ZERO, game.synthesisReserve());
        }

        @Test
        void elleNEntamePasLaReserve() {
            Game game = unlockedGame(100);
            game.state().setElementCount(92, 1);
            game.buyAutomation("autoSynth");                  // une action toutes les 2 secondes
            game.state().setAtoms(BigNum.of(10));
            game.setSynthesisReserve(BigNum.of(9));           // 2 atomes de prix + 9 de réserve : il en faut 11
            game.tick(10);
            assertEquals(0, game.state().synthesisCount());
            assertValue(10, game.state().atoms());

            game.setSynthesisReserve(BigNum.of(8));           // il en faut 10 : la synthèse part
            game.tick(0.2);
            assertEquals(1, game.state().synthesisCount());
            assertValue(8, game.state().atoms());
            game.tick(10);                                    // la suivante coûte 4 : 8 atomes ne suffisent plus
            assertEquals(1, game.state().synthesisCount());
        }

        @Test
        void laSyntheseALaMainIgnoreLaReserve() {
            Game game = unlockedGame(10);
            game.setSynthesisReserve(BigNum.of(100));
            assertFalse(game.synthesize().isEmpty());
            assertValue(8, game.state().atoms());
        }

        @Test
        void uneReserveTropHauteBloqueLaSyntheseSousLePlafondDAtomes() {
            Game game = unlockedGame(100);
            game.setSynthesisReserve(BigNum.of(100));         // 2 + 100 atomes : possible
            assertFalse(game.isSynthesisReserveBlocking());
            game.setSynthesisReserve(BigNum.of(117));         // 2 + 117 dépassent le plafond de 118
            assertTrue(game.isSynthesisReserveBlocking());
            game.state().setDarkLevel("dark_overflow", 1);    // plafond levé : plus de blocage
            assertFalse(game.isSynthesisReserveBlocking());
        }

        @Test
        void laReserveSurvitALExplosionMaisPasALaRemiseAZero() {
            Game game = unlockedGame(100);
            game.setSynthesisReserve(BigNum.of(30));
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertValue(30, game.synthesisReserve());
            game.reset();
            assertEquals(BigNum.ZERO, game.synthesisReserve());
        }

        @Test
        void elleAttendDAvoirAssezDAtomes() {
            Game game = unlockedGame(100);
            game.state().setElementCount(92, 1);
            game.buyAutomation("autoSynth");
            game.state().setAtoms(BigNum.of(1));              // pas de quoi payer 2 atomes
            game.tick(10);
            assertEquals(0, game.state().synthesisCount());
            assertValue(1, game.state().atoms());
        }
    }
}
