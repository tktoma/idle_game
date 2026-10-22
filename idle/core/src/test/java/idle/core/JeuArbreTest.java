package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.ElementSet;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : l'arbre de matière noire.
 */
class JeuArbreTest extends GameTestSupport {


    @Nested
    class ArbreDeMatiereNoire {

        private static final double START = Game.DARK_MATTER_START_SIZE.toDouble();

        /** Partie démarrée, avec assez de matière noire (20) pour que toutes les cases soient accessibles. */
        private Game darkGame() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(20));
            game.state().setExplosions(1);
            return game;
        }

        /** Donne directement {@code level} niveaux d'une amélioration de matière noire. */
        private void give(Game game, String id, int level) {
            game.state().setDarkLevel(id, level);
        }

        private void fillTable(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
        }

        // ----- L'arbre lui-même -----

        @Test
        void troisBranchesDontChaqueCaseDemandeCelleDuDessus() {
            Map<DarkUpgrade.Branch, Integer> count = new EnumMap<>(DarkUpgrade.Branch.class);
            Map<DarkUpgrade.Branch, String> previous = new EnumMap<>(DarkUpgrade.Branch.class);
            Map<String, String> forkParent = new java.util.HashMap<>();
            for (DarkUpgrade dark : DarkUpgrades.DEFAULT) {
                if (dark.isLink()) continue;                    // les liaisons (0.70) sont vérifiées à part
                count.merge(dark.branch(), 1, Integer::sum);
                if (!dark.branch().inTree()) continue;         // celles payées en matière noire ne sont pas dans l'arbre
                // Une bifurcation (0.069) : ses deux voies partent de la même case ; la case d'après demande la bifurcation.
                if (dark.isFork()) {
                    String fork = DarkUpgrade.FORK_PREFIX + dark.fork();
                    if (!fork.equals(previous.get(dark.branch()))) {
                        assertEquals(previous.get(dark.branch()), dark.requires());
                        previous.put(dark.branch(), fork);
                        forkParent.put(dark.fork(), dark.requires());
                    } else {
                        assertEquals(forkParent.get(dark.fork()), dark.requires());   // l'autre voie, à côté de la première
                    }
                    continue;
                }
                assertEquals(previous.get(dark.branch()), dark.requires());   // la case du dessus
                previous.put(dark.branch(), dark.id());
            }
            assertEquals(19, count.get(DarkUpgrade.Branch.PARTICLES).intValue());   // + six cases du seuil du Big Bang (0.97)
            assertEquals(12, count.get(DarkUpgrade.Branch.ATOMS).intValue());
            assertEquals(15, count.get(DarkUpgrade.Branch.SIZE).intValue());        // + Fond diffus et Mur de Planck (0.97)
            assertEquals(6, count.get(DarkUpgrade.Branch.DARK_MATTER).intValue());
            assertEquals(7, DarkUpgrades.DEFAULT.stream().map(DarkUpgrade::fork).filter(java.util.Objects::nonNull).distinct().count());
            assertEquals(10, DarkUpgrades.DEFAULT.stream().filter(DarkUpgrade::isLink).count());   // six liaisons, quatre accords
            assertEquals(4, DarkUpgrades.DEFAULT.stream().filter(DarkUpgrade::isAccord).count());
            assertEquals(62, DarkUpgrades.DEFAULT.stream().map(DarkUpgrade::id).distinct().count());   // 54 avant les huit cases du seuil du Big Bang (0.97)
            assertEquals(62, DarkUpgrades.DEFAULT.stream().map(DarkUpgrade::name).distinct().count());   // 54 avant les huit cases du seuil du Big Bang (0.97)
        }

        @Test
        void fermeAvantLaPremiereExplosion() {
            Game game = gameWithParticles(1e9);
            assertFalse(game.isDarkAvailable("dark_density"));
            assertFalse(game.canBuyDark("dark_density"));
            assertFalse(game.buyDark("dark_density"));
            assertValue(1e9, game.state().particles());
        }

        @Test
        void uneCaseDemandeCelleDuDessus() {
            Game game = darkGame();
            game.state().setParticles(BigNum.of(1, 45));
            assertTrue(game.isDarkAvailable("dark_density"));
            assertFalse(game.isDarkAvailable("dark_swarm"));
            assertFalse(game.buyDark("dark_swarm"));
            assertTrue(game.buyDark("dark_density"));
            assertTrue(game.isDarkAvailable("dark_swarm"));
            assertTrue(game.buyDark("dark_swarm"));
        }

        @Test
        void uneCaseNAttendQueSaCaseDuDessus() {
            Game game = darkGame();
            game.state().setDarkMatter(BigNum.ONE);           // juste après la première explosion
            game.state().setParticles(BigNum.of(1, 45));
            assertFalse(game.isDarkAvailable("dark_swarm"));   // la case du dessus manque
            assertTrue(game.buyDark("dark_density"));
            assertTrue(game.buyDark("dark_swarm"));            // une seule matière noire suffit depuis la 0.070
            assertEquals(1, game.state().darkMatter().toDouble()); // la matière noire n'est pas dépensée
        }

        @Test
        void lesParticulesSontDepenseesEtLePrixMonte() {
            Game game = darkGame();
            game.state().setParticles(BigNum.of(25));
            assertValue(20, game.darkCostOf("dark_density"));       // version 0.124 : 20 particules, puis ×2,51·10¹⁷
            assertTrue(game.buyDark("dark_density"));
            assertValue(5, game.state().particles());
            assertEquals(1, game.darkLevelOf("dark_density"));
            assertValue(20 * 2.51e17, game.darkCostOf("dark_density"));
            assertFalse(game.canBuyDark("dark_density"));
        }

        @Test
        void lesAtomesSontDepenses() {
            Game game = darkGame();
            game.state().setAtoms(BigNum.of(30));
            assertTrue(game.buyDark("dark_nucleus"));          // 10 atomes
            assertValue(20, game.state().atoms());
            assertValue(25, game.darkCostOf("dark_nucleus"));
            assertFalse(game.buyDark("dark_nucleus"));
        }

        @Test
        void laTailleEstUnSeuilPasUnPrix() {
            Game game = darkGame();
            assertFalse(game.canBuyDark("dark_inflation"));   // un proton, il faut 1 nanomètre
            game.state().setDarkMatterSize(BigNum.of(2, -9));
            assertTrue(game.buyDark("dark_inflation"));
            assertValue(2e-9, game.state().darkMatterSize()); // la matière noire n'a pas rétréci
            assertValue(1e-3, game.darkCostOf("dark_inflation"));
            assertFalse(game.canBuyDark("dark_inflation"));
        }

        @Test
        void uneCaseAUnNombreMaximalDeNiveaux() {
            Game game = darkGame();
            game.state().setDarkMatterSize(BigNum.of(1, 40));
            int bought = 0;
            while (game.buyDark("dark_inflation")) bought++;
            assertEquals(5, bought);
            assertTrue(game.isDarkMaxed("dark_inflation"));
            assertFalse(game.canBuyDark("dark_inflation"));
        }

        @Test
        void lExplosionNeReprendPasLesCasesAcquises() {
            Game game = darkGame();
            give(game, "dark_density", 2);
            give(game, "dark_inflation", 3);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(2, game.darkLevelOf("dark_density"));
            assertEquals(3, game.darkLevelOf("dark_inflation"));
        }

        @Test
        void catalogueIncoherentRefuse() {
            DarkUpgrade orphan = new DarkUpgrade("a", "A", DarkUpgrade.Branch.ATOMS, BigNum.ONE, 1, 1, "absent",
                    new DarkEffect.UncapAtoms());
            assertThrows(IllegalArgumentException.class,
                    () -> new Game(new GameState(), List.of(SPEED), List.of(), List.of(orphan), new Random(1)));
            DarkUpgrade root = new DarkUpgrade("a", "A", DarkUpgrade.Branch.ATOMS, BigNum.ONE, 1, 1, null,
                    new DarkEffect.UncapAtoms());
            assertThrows(IllegalArgumentException.class,
                    () -> new Game(new GameState(), List.of(SPEED), List.of(), List.of(root, root), new Random(1)));
            assertThrows(IllegalArgumentException.class, () -> new DarkUpgrade("a", "A", DarkUpgrade.Branch.ATOMS,
                    BigNum.ZERO, 1, 1, null, new DarkEffect.UncapAtoms()));
            assertThrows(IllegalArgumentException.class, () -> newGame().buyDark("inconnue"));
        }

        @Test
        void lesPrixEnParticulesEtEnAtomesSArrondissentMaisPasLesTailles() {
            DarkUpgrade multi = DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("dark_multisynthesis")).findFirst().orElseThrow();
            assertValue(266, multi.costAt(2));                // 118 × 1,5² = 265,5
            DarkUpgrade inflation = DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("dark_inflation")).findFirst().orElseThrow();
            assertValue(1e-9, inflation.costAt(0));
        }

        // ----- Branche des atomes -----

        @Test
        void noyauLourdAjouteUnAtomeParFusionEtParNiveau() {
            Game game = gameReadyToFuse();
            assertValue(1, game.atomsPerFusion());
            give(game, "dark_nucleus", 1);
            assertValue(2, game.atomsPerFusion());
            give(game, "dark_nucleus", 4);
            assertValue(5, game.atomsPerFusion());
            game.state().setElementCount(18, 1);              // argon ×1,15 : il multiplie aussi les atomes ajoutés
            assertValue(5.75, game.atomsPerFusion());
            assertTrue(game.fuse());
            assertValue(5.75, game.state().atoms());
        }

        @Test
        void syntheseMultipleTirePlusieursElements() {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(100));
            assertEquals(1, game.elementsPerSynthesis());
            give(game, "dark_multisynthesis", 2);
            assertEquals(3, game.elementsPerSynthesis());
            List<Element> obtained = game.synthesize();
            assertEquals(3, obtained.size());
            assertEquals(3, game.ownedCopies());
            assertValue(98, game.state().atoms());            // un seul prix pour les trois
            assertEquals(1, game.state().synthesisCount());
        }

        @Test
        void syntheseMultipleSArreteQuandLeTableauEstComplet() {
            Game game = newGame();
            unlock(game);
            fillTable(game);
            game.state().setElementCount(26, 7);              // il manque deux exemplaires de fer
            give(game, "dark_multisynthesis", 4);
            game.state().setAtoms(BigNum.of(118));
            assertEquals(2, game.synthesize().size());
            assertTrue(game.isPeriodicTableComplete());
        }

        @Test
        void memoireDesMachinesGardeLesAutomatismes() {
            Game game = newGame();
            unlock(game);                                     // tous achetés, à la cadence maximale, coupés
            game.setAutomationEnabled("autoSpeed", true);
            give(game, "dark_machines", 1);
            fillTable(game);
            assertTrue(game.explode());

            assertTrue(game.ownsAutomation("autoSlow"));
            assertEquals(2, game.automationSpeedLevel("autoSlow"));
            assertTrue(game.isAutomationEnabled("autoSpeed"));
            assertFalse(game.isAutomationEnabled("autoGen")); // coupé avant, coupé après
            assertEquals(0, game.levelOf("keep"));            // les améliorations, elles, sont perdues
            assertTrue(game.isAutomationUnlocked());          // mais l'onglet reste, puisqu'il y a des automatismes
            assertFalse(game.isPeriodicTableUnlocked());      // le tableau, lui, attend de nouveau 30 atomes créés
        }

        @Test
        void unAutomatismeGardeContinueDAgirApresLExplosion() {
            Game game = newGame();
            unlock(game);
            game.setAutomationEnabled("autoSpeed", true);
            give(game, "dark_machines", 1);
            fillTable(game);
            game.explode();
            game.state().setParticles(BigNum.of(10));         // le prix d'un niveau de vitesse
            game.tick(0.2);
            assertEquals(1, game.levelOf("speed"));
        }

        @Test
        void memoireDeLaMatiereGardeLesAmeliorationsSaufLesGenerateurs() {
            Game game = gameReadyToFuse();                    // 3 générateurs, vitesse niveau 1
            game.state().setAtoms(BigNum.of(50));
            game.buy("double");
            game.buy("double");
            game.buy("keep");
            give(game, "dark_memory", 1);
            fillTable(game);
            assertTrue(game.explode());

            assertEquals(1, game.levelOf("speed"));
            assertEquals(2, game.levelOf("double"));
            assertEquals(1, game.levelOf("keep"));
            assertEquals(0, game.levelOf("gen"));
            assertEquals(1, game.generatorCount());
            assertValue(0, game.state().atoms());             // les atomes et les éléments, eux, ont disparu
            assertEquals(0, game.discoveredElements());
            assertFalse(game.ownsAutomation("autoSpeed"));    // et les automatismes aussi, sans l'autre case
        }

        @Test
        void coupDePouceAvanceLElementUniqueGaranti() {
            Game game = newGame();
            unlock(game);
            assertEquals(8, game.guaranteedUniqueSynthesis());
            give(game, "dark_luck", 1);
            assertEquals(3, game.guaranteedUniqueSynthesis());
            for (int synthesis = 1; synthesis <= 2; synthesis++) {
                game.state().setAtoms(BigNum.of(118));
                game.synthesize();
                for (int number : List.copyOf(game.state().elements().keySet())) game.state().setElementCount(number, 0);
            }
            game.state().setAtoms(BigNum.of(118));
            assertTrue(game.synthesize().get(0).category().unique());   // la troisième
        }

        @Test
        void tableauEntameGardeLesElementsUniques() {
            Game game = darkGame();
            give(game, "dark_relics", 1);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(22, game.discoveredElements());      // 7 gaz nobles et 15 actinides
            for (int number : game.state().elements().keySet()) {
                assertTrue(PeriodicTable.element(number).category().unique());
                assertEquals(1, game.elementCount(number));
            }
            assertTrue(game.isSynthesisAutomationUnlocked());
            // L'argon (×1,15) et l'oganesson (×1,25) agissent encore, l'ensemble des gaz nobles (×1,25) est resté
            // complet, et la 7ᵉ période est à moitié réunie : le quart de son ×1,5.
            assertValue(1.15 * 1.25 * Math.pow(1.5, ElementSet.HALF_STRENGTH),
                    BigNum.of(game.elementMultiplier(ElementEffect.Stat.ATOMS) / 1.25));
            assertEquals(2, game.completedSets());            // les gaz nobles et les actinides
            assertEquals(1, game.halfSets());                 // la 7ᵉ période : 15 actinides et l'oganesson sur 32
        }

        @Test
        void tableauEntameNeGardeQueCeQuiEtaitObtenu() {
            Game game = darkGame();
            give(game, "dark_relics", 1);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertEquals(22, game.discoveredElements());
            assertEquals(0, game.elementCount(26));           // le fer, lui, a disparu
        }

        @Test
        void isotopesRepousseLeMaximumDExemplaires() {
            Game game = newGame();
            unlock(game);
            assertEquals(9, game.maxCopiesOf(ElementCategory.TRANSITION_METAL));
            assertEquals(811, game.maxTotalCopies());
            give(game, "dark_isotopes", 1);
            assertEquals(16, game.maxCopiesOf(ElementCategory.TRANSITION_METAL));
            assertEquals(9, game.maxCopiesOf(ElementCategory.LANTHANIDE));
            assertEquals(1, game.maxCopiesOf(ElementCategory.NOBLE_GAS));   // un élément unique le reste
            assertEquals(81 * 16 + 15 * 9 + 22, game.maxTotalCopies());
            give(game, "dark_isotopes", 2);
            assertEquals(25, game.maxCopiesOf(ElementCategory.HALOGEN));
            assertEquals(16, game.maxCopiesOf(ElementCategory.LANTHANIDE));
        }

        @Test
        void avecIsotopesUnTableauPleinAncienneManiereNEstPlusComplet() {
            Game game = newGame();
            unlock(game);
            fillTable(game);                                  // 9 exemplaires de chaque
            assertTrue(game.isPeriodicTableComplete());
            give(game, "dark_isotopes", 1);
            assertFalse(game.isPeriodicTableComplete());
            assertFalse(game.isElementMaxed(26));
            assertTrue(game.canExplode());                    // tous découverts : l'explosion n'attend pas les exemplaires
            game.state().setAtoms(BigNum.of(118));
            int obtained = game.synthesize().size();          // on peut de nouveau synthétiser
            assertTrue(obtained >= 1);
            assertEquals(811 + obtained, game.ownedCopies());
        }

        // ----- Branche des particules -----

        @Test
        void densiteDoubleLesParticulesDeBaseAChaqueNiveau() {
            Game game = darkGame();
            assertValue(1, game.particlesPerCreation());
            give(game, "dark_density", 1);
            assertValue(2, game.particlesPerCreation());
            give(game, "dark_density", 3);
            assertValue(8, game.particlesPerCreation());
            game.tick(ONE_PARTICLE);
            assertValue(8, game.state().particles());
        }

        @Test
        void sansEssaimLeNombreDeGenerateursResteLimite() {
            Game game = gameWithParticles(1e30);
            while (game.buy("gen")) { /* autant que possible */ }
            assertEquals(3, game.generatorCount());
            assertEquals(3, game.maxGeneratorCount());
            assertTrue(game.isMaxed("gen"));
            assertEquals(1, game.fusionGroups());
        }

        @Test
        void essaimLeveLaLimiteDesGenerateurs() {
            Game game = gameWithParticles(1e30);
            give(game, "dark_swarm", 1);
            assertEquals(3, game.generatorsPerAtom());        // il en faut toujours trois pour fusionner
            assertEquals(100, game.maxGeneratorCount());
            for (int i = 0; i < 6; i++) assertTrue(game.buy("gen"));
            assertEquals(7, game.generatorCount());
            assertFalse(game.isMaxed("gen"));
            assertValue(BASE * 7, game.productionPerSecond());
            game.tick(ONE_PARTICLE);                          // les sept générateurs produisent
            assertEquals(7, total(game) - game.state().particles().toDouble() + 7, 1e-6);
        }

        @Test
        void avecEssaimLaFusionRapporteUnAtomeParGroupeDeGenerateurs() {
            Game game = gameWithParticles(1e30);
            give(game, "dark_swarm", 1);
            for (int i = 0; i < 2; i++) game.buy("gen");      // 3 générateurs : un groupe
            assertEquals(1, game.fusionGroups());
            assertValue(1, game.atomsPerFusion());
            for (int i = 0; i < 4; i++) game.buy("gen");      // 7 générateurs : deux groupes complets
            assertEquals(2, game.fusionGroups());
            assertValue(2.2, game.atomsPerFusion());          // deux groupes, plus la prime de 10 % du second
            assertTrue(game.fuse());
            assertValue(2.2, game.state().atoms());
            assertEquals(1, game.generatorCount());           // tous consommés
        }

        @Test
        void essaimSArreteASaLimite() {
            Game game = gameWithParticles(1e300);
            give(game, "dark_swarm", 1);
            game.state().setLevel("gen", 99);
            assertEquals(100, game.generatorCount());
            assertTrue(game.isMaxed("gen"));
            assertFalse(game.buy("gen"));
            assertEquals(33, game.fusionGroups());
        }

        @Test
        void germesFaitDemarrerAvecPlusDeGenerateurs() {
            Game game = darkGame();
            assertEquals(1, game.generatorCount());
            give(game, "dark_seeds", 1);
            assertEquals(1, game.startingGenerators());
            assertEquals(2, game.generatorCount());
            game.state().setParticles(BigNum.of(1e9));
            assertTrue(game.buy("gen"));                      // il n'en reste qu'un à acheter
            assertEquals(3, game.generatorCount());
            assertTrue(game.isMaxed("gen"));
            assertFalse(game.buy("gen"));
            assertTrue(game.fuse());
            assertEquals(2, game.generatorCount());           // la partie repart avec deux générateurs
        }

        @Test
        void germesNeDonneJamaisPlusQuIlNEnFautPourFusionner() {
            Game game = darkGame();
            give(game, "dark_seeds", 9);
            assertEquals(2, game.startingGenerators());       // trois générateurs au plus dans ce catalogue
            assertEquals(3, game.generatorCount());
            assertTrue(game.canFuse());                       // on peut fusionner d'entrée
            assertTrue(game.fuse());
            assertTrue(game.canFuse());
        }

        @Test
        void germesNExistePasAvantLeDemarrage() {
            Game game = unstartedGame();
            give(game, "dark_seeds", 2);
            assertEquals(0, game.generatorCount());
        }

        @Test
        void resonanceMultiplieLesParticulesParLeCarreDesAnneesLumiere() {
            Game game = darkGame();
            give(game, "dark_resonance", 1);
            assertValue(1, game.particlesPerCreation());      // bien moins d'une année-lumière : aucun effet
            // Les paliers de taille multiplient eux aussi les particules : on les met de côté.
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(0.5));
            assertValue(1, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(10));
            assertValue(100, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(1e6));
            assertValue(1e12, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
            give(game, "dark_density", 1);                    // se multiplie avec la densité
            assertValue(2e12, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
        }

        @Test
        void sansResonanceLaTailleNeChangeRienAuxParticules() {
            Game game = darkGame();
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(1e6));
            // Seuls les paliers de taille comptent : la taille elle-même ne multiplie rien.
            assertValue(1, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
        }

        @Test
        void sansSeuilLaFusionAutomatiqueFusionneDesQuePossible() {
            Game game = gameWithParticles(1e30);
            give(game, "dark_swarm", 1);
            assertFalse(game.isFusionThresholdUnlocked());
            assertFalse(game.setFusionThreshold(6));
            assertEquals(3, game.fusionThreshold());
        }

        @Test
        void leSeuilFaitAttendreLaFusionAutomatique() {
            Game game = gameWithParticles(1e30);
            game.state().setAtoms(BigNum.of(100));
            game.buy("keep");
            createEnoughAtoms(game);
            game.buyAutomation("autoFusion");                 // agit toutes les 0,1 s
            give(game, "dark_swarm", 1);
            give(game, "dark_threshold", 1);
            assertTrue(game.setFusionThreshold(6));
            assertEquals(6, game.fusionThreshold());
            double atoms = game.state().atoms().toDouble();

            for (int i = 0; i < 4; i++) game.buy("gen");      // 5 générateurs : assez pour fusionner, pas pour le seuil
            game.tick(1);
            assertEquals(5, game.generatorCount());
            assertValue(atoms, game.state().atoms());

            game.buy("gen");                                  // le sixième : deux groupes de trois
            game.tick(0.2);
            assertEquals(1, game.generatorCount());
            assertValue(atoms + 2.2, game.state().atoms());   // deux groupes et leur prime
        }

        @Test
        void laPrimeDeGroupeGranditAvecLeNombreDeGroupes() {
            Game game = gameWithParticles(1e300);
            assertEquals(1, game.fusionGroups());
            assertEquals(1, game.fusionGroupBonus(), 1e-12);  // un seul groupe : pas de prime
            give(game, "dark_swarm", 1);
            game.state().setLevel("gen", 99);                 // 100 générateurs : 33 groupes de trois
            assertEquals(33, game.fusionGroups());
            assertEquals(1 + Game.FUSION_GROUP_BONUS * 32, game.fusionGroupBonus(), 1e-12);
            assertValue(33 * 4.2, game.atomsPerFusion());
        }

        @Test
        void leSeuilNEmpechePasDeFusionnerALaMain() {
            Game game = gameReadyToFuse();
            give(game, "dark_swarm", 1);
            give(game, "dark_threshold", 1);
            game.setFusionThreshold(50);
            assertTrue(game.canFuse());
            assertTrue(game.fuse());
        }

        @Test
        void leSeuilResteEntreLeMinimumEtLaLimiteDeGenerateurs() {
            Game game = darkGame();
            give(game, "dark_threshold", 1);
            assertEquals(3, game.fusionThreshold());          // pas encore réglé
            game.setFusionThreshold(1);
            assertEquals(3, game.fusionThreshold());
            game.setFusionThreshold(500);
            assertEquals(3, game.fusionThreshold());          // sans l'Essaim, la limite est de trois
            give(game, "dark_swarm", 1);
            game.setFusionThreshold(500);
            assertEquals(100, game.fusionThreshold());
        }

        @Test
        void amorcageOffreDesNiveauxDeVitesse() {
            // Un catalogue avec une seule amélioration de vitesse, comme le vrai jeu.
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR), List.of(), new Random(1));
            game.start();
            assertEquals(0, game.startingSpeedLevels());
            give(game, "dark_priming", 1);
            assertEquals(10, game.startingSpeedLevels());
            assertValue(BASE * 1024, game.speed());           // dix niveaux à ×2 dans ce catalogue
            assertValue(10, game.costOf("speed"));            // le prix, lui, ne bouge pas
            game.state().setParticles(BigNum.of(10));
            game.buy("speed");
            assertValue(BASE * 2048, game.speed());
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen");
            game.buy("gen");
            game.fuse();                                      // la fusion efface le niveau acheté, pas les niveaux offerts
            assertValue(BASE * 1024, game.speed());
        }

        @Test
        void masseSombreDoubleLesParticulesPourChaqueMatiereNoire() {
            Game game = darkGame();
            game.state().setDarkMatter(BigNum.ONE);
            give(game, "dark_mass", 1);
            assertValue(2, game.particlesPerCreation());      // 1 matière noire
            game.state().setDarkMatter(BigNum.of(5));
            assertValue(32, game.particlesPerCreation());
            game.state().setDarkMatter(BigNum.of(1, 30));     // une quantité absurde reste calculable
            assertTrue(game.particlesPerCreation().gt(BigNum.of(1, 100)));
        }

        @Test
        void automatismesVifsAbaisseLeDelaiMinimal() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.buy("keep");
            createEnoughAtoms(game);
            game.buyAutomation("autoSpeed");                  // au délai minimal dès l'achat
            assertEquals(0.1, game.minAutomationInterval());
            assertEquals(0.1, game.automationInterval("autoSpeed"));
            give(game, "dark_reflexes", 1);
            assertEquals(0.05, game.minAutomationInterval());
            assertEquals(0.05, game.automationInterval("autoSpeed"));

            game.state().setParticles(BigNum.of(1e12));
            game.tick(0.5);                                   // dix actions en une demi-seconde, au lieu de cinq
            assertEquals(10, game.levelOf("speed"));
        }

        // ----- Branche de la taille -----

        @Test
        void inflationDoubleLaCroissanceAChaqueNiveau() {
            Game game = darkGame();
            game.state().setDarkMatter(BigNum.ONE);
            double unit = Game.DARK_MATTER_GROWTH_PER_UNIT;
            assertEquals(unit, game.darkMatterMomentum(), 1e-12);
            give(game, "dark_inflation", 1);
            assertEquals(2 * unit, game.darkMatterMomentum(), 1e-12);
            give(game, "dark_inflation", 5);
            assertEquals(32 * unit, game.darkMatterMomentum(), 1e-12);
            game.growDarkMatter(1);
            assertValue(START * Math.pow(1 + 0.1 * 32 * unit, 10), game.state().darkMatterSize());
        }

        @Test
        void sansExpansionSpontaneeLeTempsNeFaitRienGrossir() {
            Game game = darkGame();
            game.tick(100_000);
            assertValue(START, game.state().darkMatterSize());
            assertEquals(0, game.darkAutoExpansionShare());
        }

        @Test
        void expansionSpontaneeGrossitAUnMilliemeDeLaVitesseDAppui() {
            Game held = darkGame();
            held.growDarkMatter(1);
            Game idle = darkGame();
            give(idle, "dark_spontaneous", 1);
            assertEquals(0.001, idle.darkAutoExpansionShare());
            idle.tick(1000);                                  // mille secondes sans appuyer = une seconde d'appui
            assertEquals(held.state().darkMatterSize().toDouble(), idle.state().darkMatterSize().toDouble(), START * 1e-9);
        }

        @Test
        void expansionSpontaneeEstDixFoisPlusForteAChaqueNiveau() {
            Game game = darkGame();
            give(game, "dark_spontaneous", 2);
            assertEquals(0.01, game.darkAutoExpansionShare(), 1e-12);
            give(game, "dark_spontaneous", 3);
            assertEquals(0.1, game.darkAutoExpansionShare(), 1e-12);
        }

        @Test
        void leVerrouSeDebloqueDansLArbre() {
            Game game = darkGame();
            assertFalse(game.isHoldLockUnlocked());
            give(game, "dark_lock", 1);
            assertTrue(game.isHoldLockUnlocked());
        }

        @Test
        void ondeDeFusionFaitGrossirLaMatiereNoireAChaqueFusion() {
            Game game = gameReadyToFuse();
            game.state().setDarkMatter(BigNum.ONE);
            game.state().setExplosions(1);
            assertEquals(0, game.darkFusionPulse());
            assertTrue(game.fuse());
            assertValue(START, game.state().darkMatterSize()); // sans l'amélioration, rien ne bouge

            // Deux parties identiques : l'une fusionne avec l'onde, l'autre reçoit 0,2 s d'appui à la main.
            Game pulsed = gameReadyToFuse();
            Game pressed = gameReadyToFuse();
            for (Game each : List.of(pulsed, pressed)) {
                each.state().setDarkMatter(BigNum.ONE);
                each.state().setExplosions(1);
            }
            give(pulsed, "dark_wave", 1);
            assertEquals(0.2, pulsed.darkFusionPulse());
            pressed.growDarkMatter(0.2);                      // avec la production d'avant la fusion
            assertTrue(pulsed.fuse());
            assertTrue(pulsed.state().darkMatterSize().gt(Game.DARK_MATTER_START_SIZE));
            assertEquals(pressed.state().darkMatterSize(), pulsed.state().darkMatterSize());
        }

        @Test
        void condensationAjouteDeLaMatiereNoireAChaqueExplosion() {
            Game game = darkGame();
            assertValue(1, game.darkMatterPerExplosion());
            give(game, "dark_condensation", 2);
            assertValue(3, game.darkMatterPerExplosion());
            fillTable(game);
            assertTrue(game.explode());
            assertValue(20 + 3, game.state().darkMatter());
            assertEquals(2, game.state().explosions());
        }

        @Test
        void debordementLeveLePlafondDAtomes() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(Game.MAX_ATOMS);
            assertTrue(game.isAtomCapReached());
            assertFalse(game.canFuse());
            give(game, "dark_overflow", 1);
            assertTrue(game.isAtomCapLifted());
            assertFalse(game.isAtomCapReached());
            assertTrue(game.fuse());
            assertValue(119, game.state().atoms());
        }

        @Test
        void sansDebordementLesAtomesRestentPlafonnes() {
            Game game = gameReadyToFuse();
            give(game, "dark_nucleus", 50);                   // 51 atomes par fusion
            game.state().setAtoms(BigNum.of(100));
            assertTrue(game.fuse());
            assertValue(118, game.state().atoms());
            assertValue(51, game.state().totalAtoms());
        }

        @Test
        void lePrixDeLaSyntheseRestePlafonneMemeSansPlafondDAtomes() {
            Game game = newGame();
            unlock(game);
            give(game, "dark_overflow", 1);
            game.state().setSynthesisCount(50);
            assertValue(118, game.synthesisCost());
        }
    }
}
