package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : l'automatisation, sa cadence et son rythme, le tableau périodique et la synthèse.
 */
class JeuAutomatismesTest extends GameTestSupport {


    @Nested
    class Automatisation {

        /** Partie où l'automatisation est débloquée, avec {@code atoms} atomes à dépenser, sans particule. */
        private Game gameWithAutomation(int atoms) {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(atoms + 1));
            game.buy("keep");
            createEnoughAtoms(game);
            return game;
        }

        @Test
        void verrouilleeAvantTrenteAtomesCrees() {
            assertValue(30, Game.UNLOCK_TOTAL_ATOMS);
            Game game = gameWithParticles(1e9);
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(BigNum.of(29));
            game.buy("keep");                                 // la persistance ne débloque plus rien
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.canBuyAutomation("autoSpeed"));
            assertFalse(game.buyAutomation("autoSpeed"));
            assertFalse(game.ownsAutomation("autoSpeed"));
            assertValue(99, game.state().atoms());
        }

        @Test
        void seDebloqueATrenteAtomesCreesMemeDepenses() {
            Game game = newGame();
            game.state().setTotalAtoms(BigNum.of(30));
            game.state().setAtoms(BigNum.of(3));              // les 27 autres ont été dépensés
            assertTrue(game.isAutomationUnlocked());
            assertEquals(0, game.levelOf("keep"));            // sans persistance
            assertTrue(game.buyAutomation("autoSpeed"));
            assertValue(1, game.state().atoms());
        }

        @Test
        void laFusionQuiCreeLeTrentiemeAtomeDebloqueToutDeSuite() {
            Game game = gameReadyToFuse();
            game.state().setTotalAtoms(BigNum.of(29));
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.isPeriodicTableUnlocked());
            assertTrue(game.fuse());
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.isPeriodicTableUnlocked());
        }

        @Test
        void unAutomatismeSAcheteEnAtomes() {
            Game game = gameWithAutomation(5);
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.canBuyAutomation("autoSpeed"));
            assertTrue(game.buyAutomation("autoSpeed"));
            assertValue(3, game.state().atoms());
            assertTrue(game.ownsAutomation("autoSpeed"));
            assertTrue(game.isAutomationEnabled("autoSpeed"));   // mis en marche à l'achat
        }

        @Test
        void sansAssezDAtomesLAchatEstRefuse() {
            Game game = gameWithAutomation(2);
            assertFalse(game.canBuyAutomation("autoGen"));       // coûte 3
            assertFalse(game.buyAutomation("autoGen"));
            assertValue(2, game.state().atoms());
            assertFalse(game.ownsAutomation("autoGen"));
        }

        @Test
        void onNePaieQuUneFois() {
            Game game = gameWithAutomation(10);
            assertTrue(game.buyAutomation("autoSpeed"));
            assertFalse(game.canBuyAutomation("autoSpeed"));
            assertFalse(game.buyAutomation("autoSpeed"));
            assertValue(8, game.state().atoms());
        }

        @Test
        void unAutomatismeNonAcheteNePeutPasEtreActive() {
            Game game = gameWithAutomation(10);
            assertFalse(game.setAutomationEnabled("autoSpeed", true));
            assertFalse(game.isAutomationEnabled("autoSpeed"));
            game.state().setParticles(BigNum.of(1e9));
            game.tick(5);
            assertEquals(0, game.levelOf("speed"));
        }

        @Test
        void unAutomatismeAchetePeutEtreCoupePuisRelance() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            assertTrue(game.setAutomationEnabled("autoSpeed", false));
            assertFalse(game.isAutomationEnabled("autoSpeed"));
            game.state().setParticles(BigNum.of(1e9));
            game.tick(5);
            assertEquals(0, game.levelOf("speed"));

            assertTrue(game.setAutomationEnabled("autoSpeed", true));
            game.tick(1);
            assertTrue(game.levelOf("speed") > 5);
        }

        @Test
        void acheteToutSeulDesQueCEstPossible() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.tick(ONE_PARTICLE * 9);    // 9 particules : pas assez pour le premier niveau à 10
            assertEquals(0, game.levelOf("speed"));
            game.tick(ONE_PARTICLE + 0.3);  // la dixième arrive : achat dans les dixièmes de seconde suivants
            assertEquals(1, game.levelOf("speed"));
            assertEquals(BigNum.ZERO, game.state().particles());
        }

        @Test
        void neTouchePasAuxAutresAmeliorations() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);
            assertTrue(game.levelOf("speed") > 5);
            assertEquals(0, game.levelOf("gen"));
            assertEquals(0, game.levelOf("double"));
        }

        @Test
        void laFusionAutomatiqueCreeDesAtomesSansClic() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");                    // reste 10 − 3 − 5 = 2 atomes
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);                   // achète les deux générateurs, puis fusionne
            assertValue(3, game.state().atoms());
            assertEquals(1, game.generatorCount());
            // Il ne reste que ce que le générateur a créé depuis la fusion, en moins d'une seconde.
            assertTrue(game.state().particles().lt(BigNum.of(BASE)));
        }

        @Test
        void sansFusionAutomatiqueLeJoueurGardeLaMain() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoGen");
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);
            assertTrue(game.canFuse());
            assertValue(7, game.state().atoms());
        }

        @Test
        void lesAutomatismesSontConservesApresUneFusion() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoGen");
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);
            game.fuse();
            assertTrue(game.ownsAutomation("autoGen"));
            assertTrue(game.isAutomationEnabled("autoGen"));
            assertEquals(0, game.levelOf("gen"));
        }

        @Test
        void leJeuTourneSeulAvecTousLesAutomatismes() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");                    // reste 0 atome
            for (int i = 0; i < 20 * 600; i++) game.tick(0.05); // 10 minutes sans aucune action
            assertTrue(game.state().atoms().gte(BigNum.of(3)),
                    "atomes créés en 10 minutes : " + game.state().atoms().format());
        }

        @Test
        void auDelaiMinimalDixAchatsParSeconde() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            assertEquals(Game.MIN_AUTOMATION_INTERVAL, game.automationInterval("autoSpeed"));
            game.state().setParticles(BigNum.of(1, 30));
            game.tick(2.5);
            int level = game.levelOf("speed");
            assertTrue(level >= 24 && level <= 26, "achats en 2,5 s : " + level);
        }

        @Test
        void unGrosTickVautPlusieursPetits() {
            Game small = gameWithAutomation(10);
            small.buyAutomation("autoGen");
            int seconds = (int) (150 * ONE_PARTICLE);               // de quoi créer 150 particules : un générateur, pas deux
            for (int i = 0; i < 20 * seconds; i++) small.tick(0.05);   // à 20 ticks/s
            Game big = gameWithAutomation(10);
            big.buyAutomation("autoGen");
            big.tick(seconds);                                      // d'un seul coup, comme au retour d'une absence
            assertEquals(1, small.levelOf("gen"));
            assertEquals(1, big.levelOf("gen"));
            assertEquals(total(small), total(big), total(small) * 0.02);
        }

        @Test
        void catalogueIncoherentRefuse() {
            assertThrows(IllegalArgumentException.class, () -> game("autoInconnu"));
            assertThrows(IllegalArgumentException.class, () -> new Game(new GameState(),
                    List.of(SPEED), List.of(AUTO_SPEED, AUTO_SPEED)));
            assertThrows(IllegalArgumentException.class, () -> new Game(new GameState(),
                    List.of(SPEED), List.of(AUTO_GEN)));             // vise « gen », absent du catalogue
            // un automatisme d'achat doit désigner une amélioration, les autres non
            assertThrows(IllegalArgumentException.class, () -> new Automation("x", "X",
                    Automation.Kind.UPGRADE, BigNum.ONE, null, 1, 0, BigNum.ONE, 1));
            assertThrows(IllegalArgumentException.class, () -> new Automation("x", "X",
                    Automation.Kind.FUSION, BigNum.ONE, "speed", 1, 0, BigNum.ONE, 1));
        }

        private void game(String automationId) {
            newGame().buyAutomation(automationId);
        }
    }

    @Nested
    class CadenceDesAutomatismes {

        /** Partie avec l'automatisme lent acheté, {@code atoms} atomes restants et beaucoup de particules. */
        private Game gameWithSlowAutomation(int atoms) {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(atoms + 2));
            game.buy("keep");
            createEnoughAtoms(game);
            game.buyAutomation("autoSlow");
            game.state().setParticles(BigNum.of(1e9));
            return game;
        }

        @Test
        void leDelaiSeDiviseParDeuxJusquAuPlafond() {
            assertEquals(4, AUTO_SLOW.intervalAt(0));
            assertEquals(2, AUTO_SLOW.intervalAt(1));
            assertEquals(1, AUTO_SLOW.intervalAt(2));
            assertEquals(1, AUTO_SLOW.intervalAt(7));     // au-delà du dernier niveau, plus rien ne change
        }

        @Test
        void lesReglagesParDefaut() {
            Automation automation = Automation.fusing("f", "Fusion", BigNum.of(1));
            assertEquals(16, automation.intervalAt(0));
            assertEquals(2, automation.intervalAt(3));
            assertEquals(2, automation.intervalAt(9));
            assertValue(5, automation.speedCostAt(0));
            assertValue(20, automation.speedCostAt(1));
            assertValue(80, automation.speedCostAt(2));
        }

        @Test
        void unSeulAchatParDelai() {
            Game game = gameWithSlowAutomation(0);
            assertEquals(4, game.automationInterval("autoSlow"));
            game.tick(3.8);
            assertEquals(0, game.levelOf("speed"));   // le délai n'est pas écoulé
            game.tick(0.3);
            assertEquals(1, game.levelOf("speed"));
            game.tick(4);
            assertEquals(2, game.levelOf("speed"));
            game.tick(12);                            // trois délais d'un coup
            assertEquals(5, game.levelOf("speed"));
        }

        @Test
        void unAutomatismePretAttendDePouvoirPayer() {
            Game game = gameWithSlowAutomation(0);
            game.state().setParticles(BigNum.ZERO);
            game.tick(9 * ONE_PARTICLE);              // prêt (délai de 4 s passé), mais 9 particules pour un prix de 10
            assertEquals(0, game.levelOf("speed"));
            game.state().setParticles(BigNum.of(1e9));
            game.tick(0.1);                           // achète tout de suite…
            assertEquals(1, game.levelOf("speed"));
            game.tick(3);                             // …puis le délai repart
            assertEquals(1, game.levelOf("speed"));
            game.tick(1.2);
            assertEquals(2, game.levelOf("speed"));
        }

        @Test
        void accelererCouteDesAtomesEtReduitLeDelai() {
            Game game = gameWithSlowAutomation(10);
            assertFalse(game.isAutomationMaxed("autoSlow"));
            assertValue(2, game.automationSpeedCost("autoSlow"));
            assertTrue(game.canSpeedUpAutomation("autoSlow"));
            assertTrue(game.speedUpAutomation("autoSlow"));
            assertValue(8, game.state().atoms());
            assertEquals(1, game.automationSpeedLevel("autoSlow"));
            assertEquals(2, game.automationInterval("autoSlow"));

            game.tick(4.2);
            assertEquals(2, game.levelOf("speed"));   // deux achats en 4 s au lieu d'un
        }

        @Test
        void auDernierNiveauLeDelaiNeDisparaitPas() {
            Game game = gameWithSlowAutomation(10);
            game.speedUpAutomation("autoSlow");       // 2 atomes
            assertValue(6, game.automationSpeedCost("autoSlow"));
            game.speedUpAutomation("autoSlow");       // 6 atomes
            assertValue(2, game.state().atoms());
            assertTrue(game.isAutomationMaxed("autoSlow"));
            assertEquals(1, game.automationInterval("autoSlow"));   // plafonné, pas instantané
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertValue(2, game.state().atoms());

            game.tick(3.05);
            assertEquals(3, game.levelOf("speed"));   // un achat par seconde, pas plus
        }

        @Test
        void onNAccelerePasCeQuOnNePossedePas() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(50));
            game.buy("keep");
            createEnoughAtoms(game);
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertEquals(0, game.automationSpeedLevel("autoSlow"));
        }

        @Test
        void sansAssezDAtomesOnNAccelerePas() {
            Game game = gameWithSlowAutomation(1);
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertValue(1, game.state().atoms());
        }

        @Test
        void lesElementsAccelerentAuDelaDuPlafondMaisJamaisSousLeMinimum() {
            Game game = gameWithSlowAutomation(10);
            game.speedUpAutomation("autoSlow");
            game.speedUpAutomation("autoSlow");       // plafond des atomes : 1 s
            game.state().setElementCount(36, 1);      // krypton : tous les automatismes 40 % plus rapides
            assertEquals(1 / 1.4, game.automationInterval("autoSlow"), 1e-12);
            game.state().setElementCount(87, 1_000_000);   // une quantité absurde de francium
            assertEquals(Game.MIN_AUTOMATION_INTERVAL, game.automationInterval("autoSlow"));
        }
    }

    @Nested
    class RythmeDesAutomatismes {

        /** Partie où la fusion et l'achat de générateurs sont automatiques, au délai minimal. */
        private Game fullyAutomatedGame() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(1 + 3 + 5));
            game.buy("keep");
            createEnoughAtoms(game);
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");
            return game;
        }

        @Test
        void auPlusDixActionsParSecondeParAutomatisme() {
            // Les deux générateurs sont rachetés d'un coup, puis la fusion suit dans le même dixième de seconde :
            // même avec des particules illimitées, dix fusions par seconde au mieux.
            Game game = fullyAutomatedGame();
            for (int i = 0; i < 100; i++) {          // 5 secondes à 20 ticks/s
                game.state().setParticles(BigNum.of(1e9));
                game.tick(0.05);
            }
            double atoms = game.state().atoms().toDouble();
            assertTrue(atoms >= 45 && atoms <= 51, "fusions en 5 s : " + atoms);
        }

        @Test
        void leRythmeNeDependPasDuNombreDImagesParSeconde() {
            Game slow = fullyAutomatedGame();        // 20 images par seconde
            Game fast = fullyAutomatedGame();        // 200 images par seconde
            for (int i = 0; i < 100; i++) {
                slow.state().setParticles(BigNum.of(1e9));
                slow.tick(0.05);
            }
            for (int i = 0; i < 1000; i++) {
                fast.state().setParticles(BigNum.of(1e9));
                fast.tick(0.005);
            }
            assertEquals(slow.state().atoms().toDouble(), fast.state().atoms().toDouble(), 3);
        }
    }

    @Nested
    class TableauPeriodique {

        @Test
        void leTableauCompte118ElementsNumerotesDansLOrdre() {
            assertEquals(118, PeriodicTable.ELEMENTS.size());
            for (int number = 1; number <= 118; number++) {
                assertEquals(number, PeriodicTable.element(number).number());
                assertTrue(PeriodicTable.element(number).effect() != null);
            }
            assertEquals(118, PeriodicTable.ELEMENTS.stream().map(Element::symbol).distinct().count());
            assertEquals(118, PeriodicTable.ELEMENTS.stream().map(Element::name).distinct().count());
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.element(0));
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.element(119));
        }

        @Test
        void quelquesElementsConnus() {
            assertEquals("Hydrogène", PeriodicTable.element(1).name());
            assertEquals("Fe", PeriodicTable.element(26).symbol());
            assertEquals(ElementCategory.NONMETAL, PeriodicTable.element(1).category());
            assertEquals(ElementCategory.TRANSITION_METAL, PeriodicTable.element(26).category());
            assertEquals(ElementCategory.NOBLE_GAS, PeriodicTable.element(2).category());
            assertEquals(ElementCategory.ALKALI_METAL, PeriodicTable.element(11).category());
            assertEquals(ElementCategory.HALOGEN, PeriodicTable.element(17).category());
            assertEquals(ElementCategory.LANTHANIDE, PeriodicTable.element(64).category());
            assertEquals(ElementCategory.ACTINIDE, PeriodicTable.element(92).category());
            assertEquals("Og", PeriodicTable.element(118).symbol());
        }

        @Test
        void chaqueFamilleASonNombreDElements() {
            Map<ElementCategory, Integer> expected = new EnumMap<>(ElementCategory.class);
            expected.put(ElementCategory.TRANSITION_METAL, 38);
            expected.put(ElementCategory.POST_TRANSITION_METAL, 12);
            expected.put(ElementCategory.NONMETAL, 7);
            expected.put(ElementCategory.ALKALI_METAL, 6);
            expected.put(ElementCategory.ALKALINE_EARTH_METAL, 6);
            expected.put(ElementCategory.METALLOID, 6);
            expected.put(ElementCategory.HALOGEN, 6);
            expected.put(ElementCategory.LANTHANIDE, 15);
            expected.put(ElementCategory.NOBLE_GAS, 7);
            expected.put(ElementCategory.ACTINIDE, 15);
            for (ElementCategory category : ElementCategory.values()) {
                assertEquals(expected.get(category).intValue(), PeriodicTable.elements(category).size());
            }
        }

        @Test
        void lesProbabilitesFont100PourCent() {
            double total = 0;
            for (ElementCategory category : ElementCategory.values()) total += category.chance();
            assertEquals(1, total, 1e-12);
        }

        @Test
        void lesDeuxFamillesLesPlusRaresSontUniques() {
            for (ElementCategory category : ElementCategory.values()) {
                boolean rarest = category == ElementCategory.NOBLE_GAS || category == ElementCategory.ACTINIDE;
                assertEquals(rarest, category.unique());
                if (!rarest) {
                    assertTrue(category.chance() > ElementCategory.NOBLE_GAS.chance());
                }
            }
        }

        @Test
        void chaqueMetalDeTransitionAmelioreLeGenerateurDeSaColonne() {
            // 4ᵉ période : particules ; 5ᵉ : vitesse ; 6ᵉ et 7ᵉ : pareil, en plus fort.
            for (int generator = 1; generator <= 10; generator++) {
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.PARTICLES, 1.5),
                        PeriodicTable.element(20 + generator).effect());
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.SPEED, 1.25),
                        PeriodicTable.element(38 + generator).effect());
            }
            for (int generator = 2; generator <= 10; generator++) {
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.PARTICLES, 2),
                        PeriodicTable.element(70 + generator).effect());
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.SPEED, 1.5),
                        PeriodicTable.element(102 + generator).effect());
            }
        }
    }

    @Nested
    class Synthese {

        private static final double MAX = Game.MAX_ATOMS.toDouble();

        /** Partie où le tableau périodique est débloqué, sans atome ni automatisme en marche. */
        private Game unlockedGame() {
            Game game = newGame();
            unlock(game);
            return game;
        }

        /** Vide la collection, pour que chaque tirage se fasse avec les probabilités de base. */
        private void forget(Game game, int... keep) {
            for (int number : List.copyOf(game.state().elements().keySet())) {
                boolean kept = false;
                for (int k : keep) kept |= k == number;
                if (!kept) game.state().setElementCount(number, 0);
            }
        }

        @Test
        void leTableauSOuvreATrenteAtomesCreesSansRienAcheter() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(BigNum.of(29.9));
            assertFalse(game.isPeriodicTableUnlocked());

            game.state().setTotalAtoms(BigNum.of(30));
            assertTrue(game.isPeriodicTableUnlocked());       // ni persistance ni automatisme exigés
            assertEquals(0, game.levelOf("keep"));
            assertFalse(game.ownsAutomation("autoSpeed"));
            assertTrue(game.canSynthesize());
        }

        @Test
        void sansAssezDAtomesCreesNiPersistanceNiAutomatismesNOuvrentLeTableau() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(BigNum.of(10));
            game.buy("keep");
            // Des automatismes déjà possédés, comme après une explosion qui les conserve.
            for (Automation automation : game.automations()) {
                game.state().addAutomation(automation.id());
                game.state().setAutomationSpeedLevel(automation.id(), automation.maxSpeedLevel());
            }
            assertTrue(game.isAutomationUnlocked());
            assertFalse(game.isPeriodicTableUnlocked());
        }

        @Test
        void tableauVerrouillePasDeSyntheseMemeAvecLesAtomes() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(MAX));
            assertFalse(game.canSynthesize());
            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX, game.state().atoms());
            assertEquals(0, game.state().synthesisCount());
        }

        @Test
        void couperUnAutomatismeNeReverrouillePasLeTableau() {
            Game game = unlockedGame();                       // tous les automatismes sont coupés
            game.state().setAtoms(BigNum.of(2));
            assertTrue(game.canSynthesize());
        }

        @Test
        void lePrixDoubleAChaqueSyntheseJusquAuPlafond() {
            Game game = unlockedGame();
            double[] expected = {2, 4, 8, 16, 32, 64, MAX, MAX, MAX};
            for (double cost : expected) {
                assertValue(cost, game.synthesisCost());
                game.state().setAtoms(BigNum.of(MAX));
                game.synthesize();
                forget(game);                                 // sans élément, aucune réduction de prix
            }
            assertEquals(expected.length, game.state().synthesisCount());
            game.state().setSynthesisCount(5000);
            assertValue(MAX, game.synthesisCost());
        }

        @Test
        void impossibleSansAssezDAtomes() {
            Game game = unlockedGame();
            game.state().setAtoms(BigNum.of(1));              // la première synthèse coûte 2
            assertFalse(game.canSynthesize());
            assertTrue(game.synthesize().isEmpty());
            assertValue(1, game.state().atoms());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.state().synthesisCount());
        }

        @Test
        void laSyntheseConsommeSonPrixEtDonneUnElement() {
            Game game = unlockedGame();
            game.state().setAtoms(BigNum.of(5));
            List<Element> obtained = game.synthesize();
            assertEquals(1, obtained.size());
            assertValue(3, game.state().atoms());             // 5 − 2
            assertEquals(1, game.elementCount(obtained.get(0).number()));
            assertEquals(1, game.discoveredElements());
            assertFalse(game.canSynthesize());                // la suivante coûte 4
        }

        @Test
        void laSyntheseNeToucheNiAuxAmeliorationsNiAuxGenerateurs() {
            Game game = gameReadyToFuse();
            unlock(game);
            game.state().setAtoms(BigNum.of(MAX));
            game.buy("double");                               // 1 atome
            game.state().setAtoms(BigNum.of(MAX));
            double created = game.state().totalAtoms().toDouble();
            game.synthesize();
            assertEquals(1, game.levelOf("double"));
            assertEquals(1, game.levelOf("speed"));
            assertEquals(3, game.generatorCount());
            assertValue(created, game.state().totalAtoms());
            assertTrue(game.canFuse());                       // le plafond est libéré
        }

        @Test
        void unDoublonAjouteUnExemplaire() {
            Game game = unlockedGame();
            game.state().setElementCount(26, 2);
            game.state().setAtoms(BigNum.of(MAX));
            int before = game.state().elements().values().stream().mapToInt(Integer::intValue).sum();
            int obtained = game.synthesize().size();
            int after = game.state().elements().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(before + obtained, after);
        }

        @Test
        void lesTiragesSuiventLesProbabilitesDesFamilles() {
            Game game = unlockedGame();                       // hasard reproductible (graine 42)
            game.state().setElementCount(118, 1);             // un élément unique, pour écarter la garantie
            Map<ElementCategory, Integer> draws = new EnumMap<>(ElementCategory.class);
            int total = 40_000;
            for (int i = 0; i < total; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                List<Element> obtained = game.synthesize();
                assertEquals(1, obtained.size());             // sans élément de tirage double, un seul à la fois
                draws.merge(obtained.get(0).category(), 1, Integer::sum);
                forget(game, 118);
            }
            for (ElementCategory category : ElementCategory.values()) {
                double frequency = draws.getOrDefault(category, 0) / (double) total;
                assertEquals(category.chance(), frequency, 0.01,
                        () -> category.label() + " : " + frequency);
            }
        }

        @Test
        void chaqueElementSArreteASonMaximumPuisLeTableauEstComplet() {
            Game game = unlockedGame();
            assertFalse(game.isPeriodicTableComplete());
            for (int i = 0; i < 3_000 && !game.isPeriodicTableComplete(); i++) {
                game.state().setAtoms(BigNum.of(MAX));
                assertFalse(game.synthesize().isEmpty());
            }
            assertTrue(game.isPeriodicTableComplete());
            assertEquals(118, game.discoveredElements());
            for (Element element : PeriodicTable.ELEMENTS) {
                assertEquals(element.category().maxCopies(), game.elementCount(element.number()),
                        () -> element.name() + " devrait être à son maximum");
                assertTrue(game.isElementMaxed(element.number()));
            }
            assertEquals(Game.maxCopies(), game.ownedCopies());

            // Plus rien à synthétiser : les atomes ne sont pas consommés et plus aucune famille ne sort.
            game.state().setAtoms(BigNum.of(MAX));
            int count = game.state().synthesisCount();
            assertFalse(game.canSynthesize());
            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX, game.state().atoms());
            assertEquals(count, game.state().synthesisCount());
            for (ElementCategory category : ElementCategory.values()) {
                assertEquals(0, game.categoryChance(category));
            }
        }

        @Test
        void leNombreMaximalDExemplairesDependDeLaFamille() {
            assertEquals(9, ElementCategory.TRANSITION_METAL.maxCopies());
            assertEquals(4, ElementCategory.LANTHANIDE.maxCopies());
            assertEquals(1, ElementCategory.NOBLE_GAS.maxCopies());
            assertEquals(1, ElementCategory.ACTINIDE.maxCopies());
            for (ElementCategory category : ElementCategory.values()) {
                // Des carrés : la force d'un élément au maximum est un nombre entier.
                double strength = Math.sqrt(category.maxCopies());
                assertEquals(Math.rint(strength), strength, 1e-12, () -> category.label());
                assertEquals(category.maxCopies() == 1, category.unique());
            }
            // 38 + 12 + 7 + 6 + 6 + 6 + 6 éléments à 9 exemplaires, 15 lanthanides à 4, 22 uniques.
            assertEquals(81 * 9 + 15 * 4 + 22, Game.maxCopies());
        }

        @Test
        void unElementAuMaximumNeSortPlus() {
            Game game = unlockedGame();
            game.state().setElementCount(118, 1);             // un élément unique, pour écarter la garantie
            game.state().setElementCount(26, 8);              // fer : encore un exemplaire possible
            assertFalse(game.isElementMaxed(26));
            game.state().setElementCount(26, 9);
            assertTrue(game.isElementMaxed(26));
            for (int i = 0; i < 3_000; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                for (Element element : game.synthesize()) {
                    assertTrue(element.number() != 26 && element.number() != 118, "tiré : " + element.name());
                }
                forget(game, 26, 118);
                game.state().setElementCount(26, 9);
                game.state().setElementCount(118, 1);
            }
        }

        @Test
        void uneFamilleEpuiseeLaisseSaChanceAuxAutres() {
            Game game = unlockedGame();
            for (Element element : PeriodicTable.elements(ElementCategory.LANTHANIDE)) {
                game.state().setElementCount(element.number(), 4);
            }
            assertEquals(0, game.categoryChance(ElementCategory.LANTHANIDE));
            double total = 0;
            for (ElementCategory category : ElementCategory.values()) total += game.categoryChance(category);
            assertEquals(1, total, 1e-12);
            // Les autres familles se partagent leurs 6 %. L'ensemble des lanthanides, complet, double la
            // chance des deux familles rares qui restent : 3 % et 2 % comptent pour 6 % et 4 %.
            assertEquals(0.30 / 0.99, game.categoryChance(ElementCategory.TRANSITION_METAL), 1e-12);
            assertEquals(0.06 / 0.99, game.categoryChance(ElementCategory.NOBLE_GAS), 1e-12);
        }

        @Test
        void leTirageDoubleNeDonneQuUnElementSIlNEnRestequUn() {
            Game game = unlockedGame();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.state().setElementCount(26, 8);              // il ne manque qu'un exemplaire de fer
            // Tableau presque complet : 85 % de tirage double avec les halogènes, 10 % de plus avec leur ensemble.
            assertEquals(0.95, game.doubleDrawChance(), 1e-12);
            game.state().setElementCount(35, 10_000);         // on force le tirage double à 100 %
            assertEquals(1, game.doubleDrawChance());
            assertEquals(Game.maxCopies() - 1, game.ownedCopies());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();
            assertEquals(1, obtained.size());
            assertEquals(26, obtained.get(0).number());
            assertTrue(game.isPeriodicTableComplete());
        }

        @Test
        void lesExemplairesAuDelaDuMaximumNeComptentPasDansLeTotal() {
            Game game = unlockedGame();
            game.state().setElementCount(26, 500);            // une sauvegarde trafiquée, par exemple
            assertEquals(9, game.ownedCopies());
        }

        @Test
        void sansElementUniqueLaHuitiemeSyntheseEnDonneUnACoupSur() {
            assertEquals(8, Game.GUARANTEED_UNIQUE_SYNTHESIS);
            Game game = unlockedGame();
            for (int synthesis = 1; synthesis <= 7; synthesis++) {
                game.state().setAtoms(BigNum.of(MAX));
                game.synthesize();
                forget(game);                                 // on fait comme si rien d'unique n'était sorti
            }
            assertFalse(game.isSynthesisAutomationUnlocked());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();       // la huitième
            assertTrue(obtained.get(0).category().unique());
            assertTrue(game.isSynthesisAutomationUnlocked());
        }

        @Test
        void laGarantieNeJouePlusUneFoisUnElementUniquePossede() {
            Game game = unlockedGame();
            game.state().setElementCount(92, 1);              // uranium
            game.state().setSynthesisCount(500);
            int uniques = 0;
            for (int i = 0; i < 200; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                if (game.synthesize().get(0).category().unique()) uniques++;
                forget(game, 92);
            }
            assertTrue(uniques < 40, "trop d'éléments uniques : " + uniques);   // environ 5 %, pas 100 %
        }

        @Test
        void sansElementLesChancesSontCellesDeBase() {
            Game game = unlockedGame();
            for (ElementCategory category : ElementCategory.values()) {
                assertEquals(category.chance(), game.categoryChance(category), 1e-12);
            }
            assertEquals(0, game.doubleDrawChance());
        }

        @Test
        void laChanceRendLesFamillesRaresPlusFrequentes() {
            Game game = unlockedGame();
            game.state().setElementCount(53, 1);              // iode : familles rares +10 %
            double total = 0.89 + 0.11 * 1.10;                // 89 % de familles communes, 11 % de rares
            assertEquals(0.06 * 1.10 / total, game.categoryChance(ElementCategory.LANTHANIDE), 1e-12);
            assertEquals(0.03 * 1.10 / total, game.categoryChance(ElementCategory.NOBLE_GAS), 1e-12);
            assertEquals(0.30 / total, game.categoryChance(ElementCategory.TRANSITION_METAL), 1e-12);
        }

        @Test
        void leTirageDoubleDonneParfoisDeuxElements() {
            Game game = unlockedGame();
            game.state().setElementCount(86, 1);              // radon : 25 % de tirage double
            assertEquals(0.25, game.doubleDrawChance(), 1e-12);
            int doubles = 0;
            int total = 8000;
            for (int i = 0; i < total; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                List<Element> obtained = game.synthesize();
                assertTrue(obtained.size() == 1 || obtained.size() == 2);
                if (obtained.size() == 2) doubles++;
                forget(game, 86);
            }
            assertEquals(0.25, doubles / (double) total, 0.02);
        }

        @Test
        void laChanceDeTirageDoubleNeDepassePas100PourCent() {
            Game game = unlockedGame();
            game.state().setElementCount(35, 10_000);         // brome : 10 % × 100
            assertEquals(1, game.doubleDrawChance());
        }
    }
}
