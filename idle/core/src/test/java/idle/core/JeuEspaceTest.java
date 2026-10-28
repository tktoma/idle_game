package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.BigBangMilestones;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.bigbang.SpaceUpgrades;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les améliorations d'espace et les états de la matière.
 */
class JeuEspaceTest extends GameTestSupport {


    @Nested
    class AmeliorationsDEspace {

        private Game game(double space) {
            Game game = GameTestSupport.realGame();
            GameTestSupport.bangs(game, 1);
            game.state().setSpace(BigNum.of(space));
            return game;
        }

        @Test
        void leCatalogueOuvreChaqueRayonEtLaFusion() {
            List<SpaceUpgrade> upgrades = SpaceUpgrades.DEFAULT;
            assertEquals(23, upgrades.size());
            assertEquals(23, upgrades.stream().map(SpaceUpgrade::id).distinct().count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.AutoGather).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.AutoForm).count());
            assertEquals(2, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.CreationPace).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.HoldCreate).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.BulkForm).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.AutoHold).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.OpenBodies).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.OpenStates).count());
            assertEquals(1, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.OpenAssemblies).count());
            assertEquals(6, upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.Boost).count());
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                long opening = upgrades.stream().filter(upgrade -> upgrade.effect() instanceof SpaceUpgrade.OpenKind open
                        && open.kind() == kind).count();
                assertEquals(kind == Molecule.Kind.SIMPLE ? 0 : 1, opening, "ameliorations qui ouvrent " + kind);
            }
        }

        @Test
        void laMatierePrimordialeMultiplieLesAtomesEtLaLumiereLesParticules() {
            Game game = game(299);
            assertFalse(game.canBuySpaceUpgrade("space_primordial_atoms"));
            game.state().setSpace(BigNum.of(300));
            double atoms = game.atomsPerFusion().toDouble();
            assertTrue(atoms > 0);
            assertTrue(game.buySpaceUpgrade("space_primordial_atoms"));
            assertEquals(10, game.spaceUpgradeBoost(Molecule.Stat.ATOMS), 0);
            assertEquals(10, game.moleculeBoost(Molecule.Stat.ATOMS), 0);
            assertEquals(10 * atoms, game.atomsPerFusion().toDouble(), atoms * 1e-9);
            assertEquals(1, game.spaceUpgradeBoost(Molecule.Stat.PARTICLES), 0);
            assertFalse(game.canBuySpaceUpgrade("space_primordial_particles"));
            game.state().setSpace(BigNum.of(1_200));
            double particles = game.particlesPerCreation().log10();
            assertTrue(game.buySpaceUpgrade("space_primordial_particles"));
            assertEquals(particles + 1, game.particlesPerCreation().log10(), 1e-9);
            // Elles multiplient ce que donnent les molécules, au lieu de s'y ajouter.
            game.state().setMoleculeCount("NH3", 1);                  // +3 % d'atomes
            assertEquals(10.3, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-9);
            // L'explosion ne les reprend pas ; la remise à zéro, si.
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertEquals(10, game.spaceUpgradeBoost(Molecule.Stat.ATOMS), 0);
            game.reset();
            assertEquals(1, game.spaceUpgradeBoost(Molecule.Stat.ATOMS), 0);
        }

        @Test
        void lInflationDoubleLExpansionDeuxFois() {
            Game game = game(30_000);
            assertValue(1, game.spacePerSecond());
            assertFalse(game.isSpaceUpgradeAvailable("space_inflation_2"));
            assertTrue(game.buySpaceUpgrade("space_inflation_1"));
            assertValue(2, game.spacePerSecond());
            assertTrue(game.buySpaceUpgrade("space_inflation_2"));
            assertValue(4, game.spacePerSecond());
            assertEquals(4, game.spaceUpgradeBoost(Molecule.Stat.SPACE), 0);
            // Chaque Big Bang compte toujours, avec son palier, et les molécules aussi.
            GameTestSupport.bangs(game, 3);
            assertValue(3 * 2 * 4, game.spacePerSecond());
            game.state().setMoleculeCount("CO2", 1);                  // +1,5 %
            assertValue(24 * 1.015, game.spacePerSecond());
        }

        @Test
        void unFacteurDoitMultiplier() {
            assertThrows(IllegalArgumentException.class, () -> new SpaceUpgrade.Boost(null, 2));
            assertThrows(IllegalArgumentException.class, () -> new SpaceUpgrade.Boost(Molecule.Stat.SPACE, 1));
            assertThrows(IllegalArgumentException.class, () -> new SpaceUpgrade.Boost(Molecule.Stat.SPACE, Double.NaN));
        }

        @Test
        void lesPaliersVontJusquAuCinquiemeBigBang() {
            List<BigBangMilestone> milestones = BigBangMilestones.DEFAULT;
            // Onze paliers sur huit Big Bangs : le deuxième en donne deux, le troisième trois, et aucun ne demande plus de huit Big Bangs.
            assertEquals(11, milestones.size());
            int previous = 1;
            for (BigBangMilestone milestone : milestones) {
                assertTrue(milestone.bigBangs() >= previous && milestone.bigBangs() <= 8, milestone.name());
                assertFalse(milestone.effects().isEmpty());
                previous = milestone.bigBangs();
            }
            assertEquals(2, milestones.stream().filter(milestone -> milestone.bigBangs() == 2).count());
            assertEquals(3, milestones.stream().filter(milestone -> milestone.bigBangs() == 3).count());
            assertEquals(List.of(new BigBangMilestone.KeepChallenges()), milestones.get(4).effects());
            // Le cinquième donne deux choses : l'expansion triplée, et l'arbre de matière noire qui traverse le Big Bang.
            assertEquals(List.of(new BigBangMilestone.Boost(Molecule.Stat.SPACE, 3), new BigBangMilestone.KeepDarkTree()),
                    milestones.get(7).effects());
            Game game = game(0);
            GameTestSupport.bangs(game, 0);
            assertEquals(milestones, game.bigBangMilestones());
            assertEquals(0, game.bigBangMilestonesReached());
            assertEquals(milestones.get(0), game.nextBigBangMilestone());
            int[] reached = {0, 1, 3, 6, 7, 8, 9, 10, 11};
            for (int bigBangs = 1; bigBangs <= 8; bigBangs++) {
                GameTestSupport.bangs(game, bigBangs);
                assertEquals(reached[bigBangs], game.bigBangMilestonesReached());
                assertEquals(bigBangs < 8 ? milestones.get(reached[bigBangs]) : null, game.nextBigBangMilestone());
            }
            // Au-delà du huitième, plus de palier ; et depuis le cinquième, plus aucun multiplicateur.
            double space = game.bigBangMilestoneBoost(Molecule.Stat.SPACE);
            GameTestSupport.bangs(game, 9);
            assertEquals(11, game.bigBangMilestonesReached());
            assertEquals(space, game.bigBangMilestoneBoost(Molecule.Stat.SPACE), 0);
            assertThrows(IllegalArgumentException.class, () -> new BigBangMilestone(0, "Rien", List.of(new BigBangMilestone.DarkMatter(2)), Resource.PARTICLES, BigNum.ONE));
            assertThrows(IllegalArgumentException.class, () -> new BigBangMilestone(1, "Rien", List.of(), Resource.PARTICLES, BigNum.ONE));
            assertThrows(IllegalArgumentException.class, () -> new BigBangMilestone.Accretion(1));
            assertThrows(IllegalArgumentException.class, () -> new BigBangMilestone.Boost(null, 2));
            assertThrows(IllegalArgumentException.class, () -> new BigBangMilestone.DarkMatterSpace(0));
        }

        @Test
        void lePremierPalierDoubleLaMatiereNoireDesExplosions() {
            Game game = game(0);
            GameTestSupport.bangs(game, 0);
            double before = game.darkMatterPerExplosion().toDouble();
            assertEquals(1, game.bigBangDarkMatterFactor(), 0);
            GameTestSupport.bangs(game, 1);
            assertEquals(2, game.bigBangDarkMatterFactor(), 0);
            assertEquals(2 * before, game.darkMatterPerExplosion().toDouble(), 1e-9);
            // Une explosion la laisse vraiment.
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertEquals(2 * before, game.state().darkMatter().toDouble(), 1e-9);
        }

        @Test
        void leTroisiemePalierDoubleCeQueLesAmasAttirent() {
            Game game = game(1e9);
            game.state().addSpaceUpgrade("space_states");
            game.state().setMoleculeCount("H2O", 100);
            assertTrue(game.formSubstance("H2O"));
            assertEquals(1, game.accretionFactor(), 0);
            assertEquals(11, game.moleculesPerCreation("H2O"));
            GameTestSupport.bangs(game, 3);
            assertEquals(2, game.accretionFactor(), 0);
            assertEquals(21, game.moleculesPerCreation("H2O"));
            game.state().setMoleculeCount("H2O", 10_000);
            assertEquals(201, game.moleculesPerCreation("H2O"));
            // Une sorte qui n'est pas rassemblée n'attire toujours rien.
            game.state().setMoleculeCount("NaCl", 50);
            assertEquals(1, game.moleculesPerCreation("NaCl"));
        }

        @Test
        void leQuatriemePalierFaitDependreLExpansionDeLaMatiereNoire() {
            Game game = game(1);
            GameTestSupport.bangs(game, 3);
            game.state().setDarkMatter(BigNum.of(400));
            // Avant le palier, la matière noire ne change rien à l'expansion.
            assertEquals(1, game.darkMatterSpaceBoost(), 0);
            assertValue(3 * 2, game.spacePerSecond());
            GameTestSupport.bangs(game, 4);
            // 1 + 0,1 × √400 = 3 : trois fois plus d'espace avec 400 de matière noire en réserve.
            assertEquals(3, game.darkMatterSpaceBoost(), 1e-12);
            assertValue(4 * 2 * 3, game.spacePerSecond());
            game.tick(10);
            assertValue(1 + 240, game.state().space());
            game.state().setDarkMatter(BigNum.of(2_500));
            assertEquals(6, game.darkMatterSpaceBoost(), 1e-12);
            // La dépenser ralentit l'expansion ; sans matière noire, le palier ne donne rien.
            game.state().setDarkMatter(BigNum.of(100));
            assertEquals(2, game.darkMatterSpaceBoost(), 1e-12);
            game.state().setDarkMatter(BigNum.ZERO);
            assertEquals(1, game.darkMatterSpaceBoost(), 0);
            assertValue(4 * 2, game.spacePerSecond());
            // Il ne touche ni les atomes ni les particules, et se multiplie avec le cinquième palier.
            assertEquals(1, game.bigBangMilestoneBoost(Molecule.Stat.ATOMS), 0);
            assertEquals(1, game.bigBangMilestoneBoost(Molecule.Stat.PARTICLES), 0);
            GameTestSupport.bangs(game, 5);
            game.state().setDarkMatter(BigNum.of(400));
            assertEquals(6, game.bigBangMilestoneBoost(Molecule.Stat.SPACE), 0);
            assertValue(5 * 6 * 3, game.spacePerSecond());
            // Un Big Bang reprend la matière noire : le bonus repart de rien. La remise à zéro reprend tout.
            game.state().clearDarkMatter();
            assertEquals(1, game.darkMatterSpaceBoost(), 0);
            game.reset();
            assertEquals(0, game.bigBangMilestonesReached());
            assertEquals(1, game.bigBangMilestoneBoost(Molecule.Stat.SPACE), 0);
        }

        @Test
        void lesCreationsDeMoleculesSeComptent() {
            // Assez d'espace pour créer, pas assez pour l'amélioration qui accélère la création automatique.
            Game game = game(90_000);
            game.state().setAutoFollowList(false);         // seulement les amas confiés : la liste de courses a son propre essai
            game.state().addSpaceUpgrade("space_states");
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            GameStats stats = game.stats();
            assertEquals(0, stats.moleculeCreations());
            assertEquals(0, stats.biggestCreation());
            assertFalse(stats.reached(GameStats.Step.FIRST_MOLECULE));

            // Une création à la main : une molécule, trois exemplaires d'éléments (deux hydrogènes, un oxygène).
            game.state().setTimePlayed(500);
            assertTrue(game.createMolecule("H2O"));
            assertEquals(1, stats.moleculeCreations());
            assertEquals(0, stats.autoMoleculeCreations());
            assertEquals(0, stats.moleculesAttracted());
            assertEquals(1, stats.biggestCreation());
            assertEquals(3, stats.moleculeElementsSpent());
            assertEquals(500, stats.reachedAt(GameStats.Step.FIRST_MOLECULE), 1e-9);

            game.state().setTimePlayed(800);
            assertTrue(game.createMolecule("H2O"));
            assertTrue(game.createMolecule("H2O"));
            assertFalse(stats.reached(GameStats.Step.FIRST_GATHERING));
            assertTrue(game.formSubstance("H2O"));
            assertEquals(800, stats.reachedAt(GameStats.Step.FIRST_GATHERING), 1e-9);
            assertEquals(500, stats.reachedAt(GameStats.Step.FIRST_MOLECULE), 1e-9);     // seule la première fois compte

            // L'amas attire : une création en ajoute deux, dont une « attirée ».
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            assertTrue(game.createMolecule("H2O"));
            assertEquals(5, game.moleculeCount("H2O"));
            assertEquals(4, stats.moleculeCreations());
            assertEquals(1, stats.moleculesAttracted());
            assertEquals(2, stats.biggestCreation());
            assertEquals(12, stats.moleculeElementsSpent());

            // L'automatisme : ses créations se comptent à part, et dans le total.
            GameTestSupport.bangs(game, 2);
            assertTrue(game.setMoleculeAutomated("H2O", true));
            game.tick(Game.MOLECULE_AUTOMATION_SECONDS);
            assertEquals(8, game.moleculeCount("H2O"));                // 1 + √5 : trois d'un coup
            assertEquals(5, stats.moleculeCreations());
            assertEquals(1, stats.autoMoleculeCreations());
            assertEquals(3, stats.moleculesAttracted());
            assertEquals(3, stats.biggestCreation());
            assertEquals(15, stats.moleculeElementsSpent());
            // Une création refusée ne compte pas.
            game.state().setElementCount(1, 0);
            assertFalse(game.createMolecule("H2O"));
            assertEquals(5, stats.moleculeCreations());
        }

        @Test
        void laPartDeLaMatiereSeLitSansLesAmeliorationsNiLesPaliers() {
            Game game = game(1e9);
            for (Molecule.Stat stat : Molecule.Stat.values()) assertEquals(1, game.matterBoost(stat), 0);
            GameTestSupport.bangs(game, 2);                               // le palier du deuxième Big Bang : espace ×2
            game.state().addSpaceUpgrade("space_inflation_1");         // l'inflation : espace ×2
            assertEquals(1, game.matterBoost(Molecule.Stat.SPACE), 0);
            assertEquals(4, game.moleculeBoost(Molecule.Stat.SPACE), 1e-12);
            game.state().setMoleculeCount("H2O", 3);
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                assertEquals(game.moleculeBoost(stat), game.matterBoost(stat) * game.spaceUpgradeBoost(stat)
                        * game.bigBangMilestoneBoost(stat), 1e-9, stat.name());
            }
            assertTrue(game.matterBoost(Molecule.Stat.PARTICLES) > 1);
        }

        @Test
        void leDeuxiemeBigBangDonneLaCreationAutomatique() {
            // Assez d'espace pour créer, pas assez pour l'amélioration qui accélère la création automatique.
            Game game = game(90_000);
            game.state().setAutoFollowList(false);         // seulement les amas confiés : la liste de courses a son propre essai
            game.state().addSpaceUpgrade("space_states");
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            game.state().setMoleculeCount("H2O", 3);
            game.state().setMoleculeCount("H2O2", 3);
            game.state().setMoleculeCount("CO2", 1);
            assertTrue(game.formSubstance("H2O"));
            assertTrue(game.formSubstance("H2O2"));
            // Un seul Big Bang : pas d'automatisme, rien ne se règle.
            assertFalse(game.isMoleculeAutomationUnlocked());
            assertFalse(game.setMoleculeAutomated("H2O", true));
            assertFalse(game.setMoleculeAutomationEnabled(false));
            GameTestSupport.bangs(game, 2);
            assertTrue(game.isMoleculeAutomationUnlocked());
            assertTrue(game.isMoleculeAutomationEnabled());
            assertFalse(game.isAutoCreatingMolecules());              // aucun amas ne lui est confié
            game.tick(60);
            assertEquals(3, game.moleculeCount("H2O"));
            // Seul un amas peut lui être confié.
            assertFalse(game.setMoleculeAutomated("CO2", true));
            assertTrue(game.setMoleculeAutomated("H2O", true));
            assertTrue(game.isMoleculeAutomated("H2O") && !game.isMoleculeAutomated("H2O2"));
            assertEquals(1, game.automatedMolecules());
            assertTrue(game.isAutoCreatingMolecules());
            // Rien avant cinq secondes ; à cinq, une création, au prix et avec l'attraction d'une création à la main.
            game.tick(4.9);
            assertEquals(3, game.moleculeCount("H2O"));
            game.tick(0.1);
            assertEquals(5, game.moleculeCount("H2O"));               // 1 + √3 : deux d'un coup
            assertEquals(7, game.state().elementCount(1));
            assertEquals(8, game.state().elementCount(8));
            assertEquals(3, game.moleculeCount("H2O2"));
            // Elle laisse toujours un exemplaire de chaque élément : sans hydrogène, elle attend.
            game.state().setElementCount(1, 2);
            game.tick(30);
            assertEquals(5, game.moleculeCount("H2O"));
            assertEquals(2, game.state().elementCount(1));
            // Coupée, elle ne fait rien ; remise en marche, elle reprend. Elle traverse l'explosion.
            game.state().setElementCount(1, 9);
            assertTrue(game.setMoleculeAutomationEnabled(false));
            game.tick(30);
            assertEquals(5, game.moleculeCount("H2O"));
            assertTrue(game.setMoleculeAutomationEnabled(true));
            assertTrue(game.setMoleculeAutomated("H2O2", true));
            game.tick(5);
            assertEquals(8, game.moleculeCount("H2O"));               // 1 + √5 : trois de plus
            assertEquals(5, game.moleculeCount("H2O2"));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.isMoleculeAutomated("H2O") && game.isMoleculeAutomationEnabled());
            // Retirer un amas, puis la remise à zéro.
            assertTrue(game.setMoleculeAutomated("H2O", false));
            assertEquals(1, game.automatedMolecules());
            game.reset();
            assertEquals(0, game.automatedMolecules());
            assertFalse(game.isMoleculeAutomationUnlocked());
        }

        @Test
        void quandLEspaceManqueLaCreationAutomatiqueSertLesAmasATourDeRole() {
            Game game = game(1e9);
            GameTestSupport.bangs(game, 2);
            for (String id : new String[] {"space_states", "space_acid", "space_salt"}) game.state().addSpaceUpgrade(id);
            game.state().setMoleculeCount("NaCl", 3);
            game.state().setMoleculeCount("KCl", 3);
            assertTrue(game.formSubstance("NaCl"));
            assertTrue(game.formSubstance("KCl"));
            assertTrue(game.setMoleculeAutomated("NaCl", true));
            assertTrue(game.setMoleculeAutomated("KCl", true));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            // La place d'une seule molécule de plus (la plus grosse des deux) à chaque passage.
            double used = game.usedSpace().toDouble();
            double room = Math.max(game.moleculeVolume("NaCl").toDouble(), game.moleculeVolume("KCl").toDouble());
            int salt = 3;
            int potash = 3;
            for (int round = 0; round < 4; round++) {
                game.state().setSpace(BigNum.of(game.usedSpace().toDouble() + room));
                game.tick(Game.MOLECULE_AUTOMATION_SECONDS);
            }
            assertTrue(game.moleculeCount("NaCl") > salt, "le sel a ete servi");
            assertTrue(game.moleculeCount("KCl") > potash, "le chlorure de potassium aussi");
            assertTrue(game.usedSpace().toDouble() > used);
            // Un long temps d'un coup est rattrapé, sans jamais dépasser l'espace.
            game.state().setSpace(BigNum.of(1, 9));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            int before = game.moleculeCount("NaCl");
            game.tick(3_600);
            assertTrue(game.moleculeCount("NaCl") > before);
            assertTrue(game.state().elementCount(11) >= 1);
        }

        @Test
        void lesRayonsSOuvrentDansLOrdreEtDePlusEnPlusLoin() {
            Game game = game(0);
            SpaceUpgrade previous = null;
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                if (kind == Molecule.Kind.SIMPLE) continue;
                SpaceUpgrade upgrade = game.spaceUpgrades().stream()
                        .filter(each -> each.effect() instanceof SpaceUpgrade.OpenKind open && open.kind() == kind)
                        .findFirst().orElseThrow();
                assertEquals(previous == null ? null : previous.id(), upgrade.requires(), kind.toString());
                if (previous != null) assertTrue(upgrade.space().gt(previous.space()), kind + " devrait demander plus d'espace");
                previous = upgrade;
            }
        }

        @Test
        void rienNeSAcheteAvantLePremierBigBang() {
            Game game = game(1e9);
            GameTestSupport.bangs(game, 0);
            assertFalse(game.isSpaceUpgradeAvailable("space_acid"));
            assertFalse(game.buySpaceUpgrade("space_acid"));
        }

        @Test
        void uneAmeliorationNeDepenseRien() {
            Game game = game(2_500);
            assertTrue(game.canBuySpaceUpgrade("space_acid"));
            assertTrue(game.buySpaceUpgrade("space_acid"));
            assertTrue(game.ownsSpaceUpgrade("space_acid"));
            assertTrue(game.reservedSpace().isZero());
            assertValue(2_500, game.freeSpace());               // tout l'espace reste libre
            assertValue(2_500, game.state().space());
            assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.ACID));
            assertEquals(Molecule.Kind.SALT, game.nextMoleculeKind());
            // Elle ne se prend qu'une fois.
            assertFalse(game.canBuySpaceUpgrade("space_acid"));
            assertFalse(game.buySpaceUpgrade("space_acid"));
        }

        @Test
        void cEstLEspaceGagneQuiCompteEtPasCeluiQuiReste() {
            Game game = game(1_999);
            assertFalse(game.canBuySpaceUpgrade("space_acid"));
            game.state().setSpace(BigNum.of(2_000));
            assertTrue(game.canBuySpaceUpgrade("space_acid"));
            // L'espace est plein de molécules, et un rassemblement en réserve : l'amélioration se prend quand même.
            game.state().setMoleculeCount("H2O", 4);                  // 4 × 200
            game.state().addSubstance("H2O");                         // un lieu de 600 pour le liquide
            game.state().setMoleculeCount("NaCl", 2);                 // 2 × 280
            game.state().setMoleculeCount("LiH", 1);                  // 40
            assertValue(1_400, game.occupiedSpace());
            assertValue(600, game.reservedSpace());
            assertTrue(game.freeSpace().isZero());
            assertTrue(game.canBuySpaceUpgrade("space_acid"));
            assertTrue(game.buySpaceUpgrade("space_acid"));
            assertValue(600, game.reservedSpace());
            assertValue(1_400, game.occupiedSpace());
        }

        @Test
        void unRayonAttendCeluiDAvant() {
            Game game = game(1e9);
            assertTrue(game.isSpaceUpgradeAvailable("space_acid"));
            assertTrue(game.isSpaceUpgradeAvailable("space_states"));
            assertFalse(game.isSpaceUpgradeAvailable("space_salt"));
            assertFalse(game.buySpaceUpgrade("space_salt"));
            assertTrue(game.buySpaceUpgrade("space_acid"));
            assertTrue(game.buySpaceUpgrade("space_salt"));
            assertEquals(3, game.moleculeKindsUnlocked());
            assertTrue(game.reservedSpace().isZero());
        }

        @Test
        void unMillionDEspaceGagneOuvreToutLeCatalogue() {
            Game game = game(999_999);
            assertFalse(game.isSpaceUpgradeAvailable("space_rare"));
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                // Les améliorations de fin de partie (au-delà du million) attendent bien plus d'espace.
                if (upgrade.space().gt(BigNum.of(1_000_000))) continue;
                if (!upgrade.id().equals("space_rare")) assertTrue(game.buySpaceUpgrade(upgrade.id()), upgrade.id());
            }
            assertFalse(game.buySpaceUpgrade("space_rare"));
            assertEquals(Molecule.Kind.RARE, game.nextMoleculeKind());
            game.state().setSpace(BigNum.of(1_000_000));
            assertTrue(game.buySpaceUpgrade("space_rare"));
            assertTrue(game.reservedSpace().isZero());
            assertValue(1_000_000, game.freeSpace());
        }

        @Test
        void toutPrendreOuvreToutLeCatalogue() {
            Game game = game(1e10);
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) assertTrue(game.buySpaceUpgrade(upgrade.id()), upgrade.id());
            assertEquals(Molecule.Kind.values().length, game.moleculeKindsUnlocked());
            assertTrue(game.nextMoleculeKind() == null);
            assertTrue(game.isStatesUnlocked());
            assertTrue(game.reservedSpace().isZero());
        }

        @Test
        void lExpansionFinitParLesOuvrir() {
            Game game = game(500);
            assertEquals(1_500.0, game.secondsUntilSpace(BigNum.of(2_000)), 1e-6);
            game.tick(1_499);
            assertFalse(game.canBuySpaceUpgrade("space_acid"));
            assertFalse(game.ownsSpaceUpgrade("space_acid"));
            game.tick(1);
            assertEquals(0.0, game.secondsUntilSpace(BigNum.of(2_000)), 1e-9);
            // Le seuil atteint, l'amélioration est acquise d'elle-même : il n'y a rien à cliquer.
            assertTrue(game.ownsSpaceUpgrade("space_acid"));
            assertFalse(game.buySpaceUpgrade("space_acid"));
            // Les molécules créées en route ne retardent rien : seul l'espace gagné compte.
            game.state().setMoleculeCount("H2O", 3);
            assertEquals(8_000.0, game.secondsUntilSpace(BigNum.of(10_000)), 1e-6);
            // Deux Big Bangs : deux fois plus vite, et le palier du deuxième double encore.
            GameTestSupport.bangs(game, 2);
            assertEquals(2_000.0, game.secondsUntilSpace(BigNum.of(10_000)), 1e-6);
        }

        @Test
        void lesAmeliorationsSurviventALExplosionEtAuBigBang() {
            Game game = game(1e9);
            assertTrue(game.buySpaceUpgrade("space_acid"));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.ownsSpaceUpgrade("space_acid"));
            GameTestSupport.readyForBigBang(game);
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertTrue(game.ownsSpaceUpgrade("space_acid"));
            game.reset();
            assertFalse(game.state().ownsSpaceUpgrade("space_acid"));
            assertTrue(game.reservedSpace().isZero());
        }

        @Test
        void uneAmeliorationInconnueEstRefusee() {
            Game game = game(1e9);
            assertThrows(IllegalArgumentException.class, () -> game.buySpaceUpgrade("space_nowhere"));
            assertThrows(IllegalArgumentException.class,
                    () -> new SpaceUpgrade("x", "X", BigNum.ZERO, null, new SpaceUpgrade.OpenStates()));
        }
    }

    @Nested
    class EtatsDeLaMatiere {

        private static final String WATER = "H2O";

        /** Après un premier Big Bang, rassemblement ouvert, beaucoup d'espace, et {@code count} molécules de chaque sorte demandée. */
        private Game game(int count, String... moleculeIds) {
            Game game = GameTestSupport.realGame();
            GameTestSupport.bangs(game, 1);
            game.state().setSpace(BigNum.of(1, 9));
            game.state().addSpaceUpgrade("space_states");
            for (String id : moleculeIds) game.state().setMoleculeCount(id, count);
            return game;
        }

        @Test
        void chaqueMoleculeAUnEtat() {
            Map<Molecule.State, Integer> all = new EnumMap<>(Molecule.State.class);
            Map<Molecule.State, Integer> small = new EnumMap<>(Molecule.State.class);
            for (Molecule molecule : Molecules.DEFAULT) {
                assertTrue(molecule.hasState(), molecule.id());
                all.merge(molecule.state(), 1, Integer::sum);
                if (molecule.kind() == Molecule.Kind.SIMPLE) small.merge(molecule.state(), 1, Integer::sum);
                // Les minéraux sont tous des cristaux, et rien de vivant n'est un métal.
                if (molecule.kind() == Molecule.Kind.MINERAL) assertEquals(Molecule.State.CRYSTAL, molecule.state(), molecule.id());
                if (molecule.kind() == Molecule.Kind.LIFE || molecule.kind() == Molecule.Kind.ORGANIC) {
                    assertTrue(molecule.state() != Molecule.State.METAL && molecule.state() != Molecule.State.CRYSTAL, molecule.id());
                }
            }
            assertEquals(Map.of(Molecule.State.GAS, 27, Molecule.State.LIQUID, 8, Molecule.State.SOLID, 6), small);
            assertEquals(Map.of(Molecule.State.GAS, 45, Molecule.State.LIQUID, 61, Molecule.State.SOLID, 85,
                    Molecule.State.CRYSTAL, 227, Molecule.State.METAL, 12), all);
            assertEquals(Molecule.State.LIQUID, state(WATER));
            assertEquals(Molecule.State.GAS, state("C3H8"));
            assertEquals(Molecule.State.LIQUID, state("C2H5OH"));
            assertEquals(Molecule.State.SOLID, state("C6H12O6"));
            assertEquals(Molecule.State.CRYSTAL, state("NaCl"));
            assertEquals(Molecule.State.CRYSTAL, state("SiO2"));
            assertEquals(Molecule.State.METAL, state("NiTi"));
            assertEquals(Molecule.State.METAL, state("Nd2Fe14B"));
        }

        private Molecule.State state(String id) {
            return Molecules.DEFAULT.stream().filter(each -> each.id().equals(id)).findFirst().orElseThrow().state();
        }

        @Test
        void unCristalEtUnMetalSeRassemblentCommeLesAutres() {
            Game game = game(7, "NaCl", "NiTi");
            assertValue(3 * 280, game.substanceSpace("NaCl"));               // cristal : juste la place de ses molécules
            assertValue(3 * 500 * 0.75, game.substanceSpace("NiTi"));        // métal : les trois quarts
            assertTrue(game.formSubstance("NaCl"));
            assertTrue(game.formSubstance("NiTi"));
            double crystal = Math.pow(3, 1.40);                               // sept molécules : trois doublements
            double metal = Math.pow(3, 1.45);
            assertEquals(crystal, game.effectiveMolecules("NaCl"), 1e-12);
            assertEquals(metal, game.effectiveMolecules("NiTi"), 1e-12);
            // Le sel relève le plafond du sodium : 3^1,4 = 4,65, donc quatre exemplaires au lieu de trois.
            assertEquals(4, game.elementUncap(11));
            assertEquals(4, game.elementUncap(28));                          // nitinol : 3^1,45 = 4,92
            // Un cristal : 3 % de particules et 5 % d'atomes ; un métal : 8 % d'atomes.
            assertEquals(1 + 0.03 * crystal, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(1 + 0.05 * crystal + 0.08 * metal, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
            assertEquals(List.of("NaCl", "NiTi"), game.state().substances());
        }

        @Test
        void lExposantDUnEtatEstPetit() {
            for (Molecule.State matter : Molecule.State.values()) {
                assertTrue(matter.exponent() > 1 && matter.exponent() <= 1.5, matter + " : " + matter.exponent());
                assertTrue(matter.particles() + matter.atoms() > 0, matter + " n'ajoute ni particules ni atomes");
            }
            assertTrue(Molecule.State.METAL.exponent() > Molecule.State.CRYSTAL.exponent());
            assertTrue(Molecule.State.CRYSTAL.exponent() > Molecule.State.SOLID.exponent());
            assertTrue(Molecule.State.METAL.spaceFactor() < Molecule.State.CRYSTAL.spaceFactor());
            assertTrue(Molecule.State.SOLID.exponent() > Molecule.State.LIQUID.exponent());
            assertTrue(Molecule.State.LIQUID.exponent() > Molecule.State.GAS.exponent());
            assertTrue(Molecule.State.GAS.spaceFactor() > Molecule.State.LIQUID.spaceFactor());
        }

        @Test
        void pasDeRassemblementSansLAmeliorationDEspace() {
            Game game = game(3, WATER);
            assertTrue(game.isStatesUnlocked());
            assertTrue(game.canFormSubstance(WATER));
            Game closed = game(3, WATER);
            closed.reset();
            closed.start();
            GameTestSupport.bangs(closed, 1);
            closed.state().setSpace(BigNum.of(1, 9));
            closed.state().setMoleculeCount(WATER, 3);
            assertFalse(closed.isStatesUnlocked());
            assertFalse(closed.canFormSubstance(WATER));
            assertFalse(closed.formSubstance(WATER));
            assertFalse(closed.hasSubstance(WATER));
        }

        @Test
        void troisMoleculesDEauSeRassemblentEnLiquide() {
            Game game = game(3, WATER);
            assertValue(600, game.occupiedSpace());
            assertValue(600, game.substanceSpace(WATER));           // la place de trois molécules d'eau, à 200 chacune
            assertFalse(game.hasSubstance(WATER));
            assertEquals(0, game.gatheredMolecules(WATER));
            assertTrue(game.formSubstance(WATER));
            assertTrue(game.hasSubstance(WATER));
            assertEquals(1, game.substancesFormed());
            assertEquals(List.of(WATER), game.state().substances());
            // Aucune molécule n'est consommée : elles sont toutes dans leur lieu, et gardent leur place.
            assertEquals(3, game.moleculeCount(WATER));
            assertEquals(3, game.gatheredMolecules(WATER));
            assertEquals(3, game.state().moleculeLog().size());
            assertValue(600, game.occupiedSpace());
            assertValue(600, game.reservedSpace());
            assertValue(1_200, game.usedSpace());
            assertValue(1e9 - 1_200, game.freeSpace());
        }

        @Test
        void ilFautTroisMolecules() {
            Game game = game(2, WATER);
            assertFalse(game.canFormSubstance(WATER));
            assertFalse(game.formSubstance(WATER));
            assertFalse(game.hasSubstance(WATER));
            game.state().setMoleculeCount(WATER, 3);
            assertTrue(game.formSubstance(WATER));
        }

        @Test
        void cEstUnAchatUnique() {
            Game game = game(3, WATER);
            assertTrue(game.formSubstance(WATER));
            assertFalse(game.canFormSubstance(WATER));
            assertFalse(game.formSubstance(WATER));
            // Dix molécules de plus : toujours un seul rassemblement, et le lieu ne demande rien de plus.
            game.state().setMoleculeCount(WATER, 13);
            assertFalse(game.canFormSubstance(WATER));
            assertFalse(game.formSubstance(WATER));
            assertEquals(1, game.substancesFormed());
            assertValue(600, game.reservedSpace());
            assertValue(600, game.substanceSpace(WATER));
        }

        @Test
        void leLieuDemandeDeLEspaceLibre() {
            Game game = game(3, WATER);
            // Les trois molécules occupent 600 et leur lieu en réserve autant : il faut 1 200 en tout.
            game.state().setSpace(BigNum.of(1_199));
            assertFalse(game.canFormSubstance(WATER));
            assertFalse(game.formSubstance(WATER));
            game.state().setSpace(BigNum.of(1_200));
            assertTrue(game.formSubstance(WATER));
            assertTrue(game.freeSpace().isZero());
            assertFalse(game.hasSpaceForMolecule(WATER));
        }

        @Test
        void unGazDemandeUnLieuPlusVasteQuUnSolide() {
            Game game = game(3, "H2", "S8", WATER);
            assertValue(3 * 20 * 20, game.substanceSpace("H2"));             // gaz : vingt fois la place de ses protons
            assertValue(3 * 1_280, game.substanceSpace("S8"));               // solide : juste leur place
            assertValue(3 * 100 * 2, game.substanceSpace(WATER));            // liquide : deux fois
            assertTrue(game.formSubstance("H2"));
            assertTrue(game.formSubstance("S8"));
            assertTrue(game.formSubstance(WATER));
            assertValue(1_200 + 3_840 + 600, game.reservedSpace());
            assertEquals(List.of("H2", "S8", WATER), game.state().substances());
            // À poids égal, un gaz prend vingt fois la place d'un cristal, un liquide deux fois, un métal les trois quarts.
            assertEquals(20, Molecule.State.GAS.spaceFactor(), 0);
            assertEquals(2, Molecule.State.LIQUID.spaceFactor(), 0);
            assertEquals(1, Molecule.State.SOLID.spaceFactor(), 0);
            assertEquals(1, Molecule.State.CRYSTAL.spaceFactor(), 0);
            assertEquals(0.75, Molecule.State.METAL.spaceFactor(), 0);
        }

        @Test
        void lePrixEnElementsNeChangePas() {
            Game game = game(3, WATER);
            assertEquals(Map.of(1, 2, 8, 1), game.nextMoleculeCost(WATER));
            assertTrue(game.formSubstance(WATER));
            assertEquals(Map.of(1, 2, 8, 1), game.nextMoleculeCost(WATER));
            assertValue(200, game.moleculeVolume(WATER));
        }

        @Test
        void lesMoleculesRassembleesComptentUnPeuPlusQuElles() {
            Game game = game(3, WATER);
            assertEquals(2.0, game.effectiveMolecules(WATER), 1e-12);         // trois molécules : deux doublements
            assertEquals(1.10, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertTrue(game.formSubstance(WATER));
            double gathered = Math.pow(2, 1.25);                      // 2,38 au lieu de 2
            assertEquals(gathered, game.effectiveMolecules(WATER), 1e-12);
            // Le bonus des molécules, à l'exposant du liquide, plus les particules et les atomes d'un liquide.
            assertEquals(1 + 0.05 * gathered + 0.05 * gathered, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(1 + 0.02 * gathered, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
        }

        @Test
        void unAmasAttireLaMatiere() {
            Game game = game(3, WATER);
            // Tant que la sorte n'est pas rassemblée, une création donne une molécule.
            assertEquals(1, game.moleculesPerCreation(WATER));
            assertTrue(game.formSubstance(WATER));
            // Rassemblée : une, plus la racine du nombre de molécules de l'amas.
            assertEquals(2, game.moleculesPerCreation(WATER));
            game.state().setMoleculeCount(WATER, 9);
            assertEquals(4, game.moleculesPerCreation(WATER));
            game.state().setMoleculeCount(WATER, 100);
            assertEquals(11, game.moleculesPerCreation(WATER));
            game.state().setMoleculeCount(WATER, 10_000);
            assertEquals(101, game.moleculesPerCreation(WATER));
            assertEquals(101, game.moleculesNextCreation(WATER));
            // Le prix reste celui d'une molécule.
            game.state().setElementCount(1, 3);
            game.state().setElementCount(8, 2);
            assertTrue(game.createMolecule(WATER));
            assertEquals(10_101, game.moleculeCount(WATER));
            assertEquals(1, game.state().elementCount(1));
            assertEquals(1, game.state().elementCount(8));
            assertEquals(10_101, game.state().moleculeLog().size());
            assertEquals(10_101, game.moleculesCreated());
            assertValue(10_101 * 200.0, game.occupiedSpace());
        }

        @Test
        void lAmasNAttireQueCeQuiTientDansLEspaceLibre() {
            Game game = game(100, WATER);
            assertTrue(game.formSubstance(WATER));
            // Cent molécules et leur lieu : 20 600 d'espace. Il reste la place de quatre molécules, sur les onze attirées.
            game.state().setSpace(BigNum.of(20_600 + 4 * 200 + 150));
            assertEquals(11, game.moleculesPerCreation(WATER));
            assertEquals(4, game.moleculesNextCreation(WATER));
            game.state().setElementCount(1, 9);
            game.state().setElementCount(8, 9);
            assertTrue(game.createMolecule(WATER));
            assertEquals(104, game.moleculeCount(WATER));
            // Plus de place pour une seule : la création est refusée, et rien n'est dépensé.
            assertFalse(game.canCreateMolecule(WATER));
            assertFalse(game.createMolecule(WATER));
            assertEquals(7, game.state().elementCount(1));
            assertEquals(104, game.moleculeCount(WATER));
        }

        @Test
        void uneSorteComptePourSesDoublements() {
            assertEquals(0, Game.doublings(0), 0);
            assertEquals(1, Game.doublings(1), 1e-12);
            assertEquals(2, Game.doublings(3), 1e-12);
            assertEquals(3, Game.doublings(7), 1e-12);
            assertEquals(10, Game.doublings(1_023), 1e-12);
            // La première molécule donne son bonus en entier ; ensuite il faut doubler leur nombre pour le redonner.
            Game game = game(1, WATER);
            assertEquals(1.05, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            game.state().setMoleculeCount(WATER, 3);
            assertEquals(1.10, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            game.state().setMoleculeCount(WATER, 7);
            assertEquals(1.15, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            game.state().setMoleculeCount(WATER, 1_023);
            assertEquals(1.50, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            // Deux sortes à sept molécules valent donc mieux qu'une seule à quatorze.
            Game two = game(7, WATER, "H2O2");
            Game one = game(14, WATER);
            assertTrue(two.moleculeBoost(Molecule.Stat.PARTICLES) > one.moleculeBoost(Molecule.Stat.PARTICLES));
        }

        @Test
        void leRassemblementSuitLeNombreDExemplaires() {
            Game game = game(3, WATER);
            assertTrue(game.formSubstance(WATER));
            double before = game.moleculeBoost(Molecule.Stat.PARTICLES);
            // Les molécules créées ensuite rejoignent le lieu d'elles-mêmes : deux d'un coup, l'amas en attire une.
            game.state().setElementCount(1, 3);
            game.state().setElementCount(8, 2);
            assertTrue(game.createMolecule(WATER));
            assertEquals(5, game.gatheredMolecules(WATER));
            double counted = Math.pow(Game.doublings(5), 1.25);
            assertEquals(counted, game.effectiveMolecules(WATER), 1e-12);
            // Le bonus de l'eau, puis les particules et les atomes d'un liquide, tous comptés de la même façon.
            assertEquals(1 + 0.05 * counted + 0.05 * counted, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(1 + 0.02 * counted, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
            assertTrue(game.moleculeBoost(Molecule.Stat.PARTICLES) > before);
            // Dix molécules d'eau rassemblées comptent pour 4,72 ; mille, pour 17,7.
            game.state().setMoleculeCount(WATER, 10);
            assertEquals(4.72, game.effectiveMolecules(WATER), 0.005);
            game.state().setMoleculeCount(WATER, 1_000);
            assertEquals(17.71, game.effectiveMolecules(WATER), 0.005);
            // Sans molécule, le lieu est vide et ne donne rien.
            game.state().setMoleculeCount(WATER, 0);
            assertTrue(game.hasSubstance(WATER));
            assertEquals(0, game.gatheredMolecules(WATER));
            assertEquals(0.0, game.effectiveMolecules(WATER), 0);
            assertEquals(1.0, game.moleculeBoost(Molecule.Stat.PARTICLES), 0);
            assertEquals(1.0, game.moleculeBoost(Molecule.Stat.ATOMS), 0);
        }

        @Test
        void seulesLesMoleculesRassembleesProfitentDeLExposant() {
            Game game = game(5, WATER, "H2O2");
            assertTrue(game.formSubstance(WATER));
            assertEquals(Math.pow(Game.doublings(5), 1.25), game.effectiveMolecules(WATER), 1e-12);
            assertEquals(Game.doublings(5), game.effectiveMolecules("H2O2"), 0);
            assertEquals(5, game.gatheredMolecules(WATER));
            assertEquals(0, game.gatheredMolecules("H2O2"));
        }

        @Test
        void unGazAjouteDesParticulesUnSolideDesAtomes() {
            Game gas = game(3, "CO2");
            assertTrue(gas.formSubstance("CO2"));
            double counted = Math.pow(2, 1.15);                       // trois molécules : deux doublements, à l'exposant du gaz
            assertEquals(1 + 0.015 * counted, gas.moleculeBoost(Molecule.Stat.SPACE), 1e-12);
            assertEquals(1 + 0.08 * counted, gas.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(1.0, gas.moleculeBoost(Molecule.Stat.ATOMS), 0);

            Game solid = game(3, "P4O10");
            assertTrue(solid.formSubstance("P4O10"));
            counted = Math.pow(2, 1.35);
            assertEquals(1 + 0.10 * counted, solid.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(1 + 0.05 * counted, solid.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
        }

        @Test
        void unGazDeDihydrogeneReleveUnPeuPlusLePlafond() {
            Game game = game(3, "H2");
            Element hydrogen = PeriodicTable.element(1);
            assertEquals(11, game.maxCopiesOf(hydrogen));             // trois molécules : deux doublements
            assertTrue(game.formSubstance("H2"));
            assertEquals(2, game.elementUncap(1));                    // 2^1,15 = 2,22 : toujours deux
            assertEquals(11, game.maxCopiesOf(hydrogen));
            game.state().setMoleculeCount("H2", 31);                  // cinq doublements
            assertEquals(6, game.elementUncap(1));                    // 5^1,15 = 6,36 : six, un de plus que cinq
            game.state().setMoleculeCount("H2", 63);
            assertEquals(7, game.elementUncap(1));                    // 6^1,15 = 7,85
            assertEquals(16, game.maxCopiesOf(hydrogen));
        }

        @Test
        void rassemblerNeFaitJamaisRienPerdre() {
            // Une seule partie, remise dans le même état pour chacune des molécules : 430 constructions complètes prenaient des secondes.
            Game game = game(3);
            for (Molecule molecule : Molecules.DEFAULT) {
                if (!molecule.hasState()) continue;
                game.reset();
                game.start();
                GameTestSupport.bangs(game, 1);
                game.state().setSpace(BigNum.of(1, 9));
                game.state().addSpaceUpgrade("space_states");
                game.state().setMoleculeCount(molecule.id(), 3);
                double[] before = new double[Molecule.Stat.values().length];
                for (Molecule.Stat stat : Molecule.Stat.values()) before[stat.ordinal()] = game.moleculeBoost(stat);
                int copies = game.maxTotalCopies();
                assertTrue(game.formSubstance(molecule.id()), molecule.id());
                assertEquals(3, game.moleculeCount(molecule.id()), molecule.id());
                for (Molecule.Stat stat : Molecule.Stat.values()) {
                    assertTrue(game.moleculeBoost(stat) >= before[stat.ordinal()] - 1e-12, molecule.id() + " perd sur " + stat);
                }
                assertTrue(game.maxTotalCopies() >= copies, molecule.id() + " perd un plafond");
                assertTrue(game.moleculeBoost(Molecule.Stat.PARTICLES) + game.moleculeBoost(Molecule.Stat.ATOMS)
                        > before[Molecule.Stat.PARTICLES.ordinal()] + before[Molecule.Stat.ATOMS.ordinal()], molecule.id() + " n'ajoute rien");
            }
        }

        @Test
        void uneMoleculeSansEtatNeSeRassemblePas() {
            // Toutes celles du catalogue en ont un ; une molécule décrite sans état n'en a pas.
            Molecule bare = Molecule.of(Molecule.Kind.SALT, "Sel de table", "NaCl");
            assertFalse(bare.hasState());
            assertTrue(bare.crystal().hasState());
            assertEquals(Molecule.State.METAL, bare.metal().state());
            Game game = game(3, "NaCl");
            assertThrows(IllegalArgumentException.class, () -> game.formSubstance("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.hasSubstance("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.substanceSpace("phlogistique"));
        }

        @Test
        void lesRassemblementsSurviventALExplosionEtAuBigBang() {
            Game game = game(3, WATER);
            assertTrue(game.formSubstance(WATER));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.hasSubstance(WATER));
            GameTestSupport.readyForBigBang(game);
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertTrue(game.hasSubstance(WATER));
            assertEquals(3, game.gatheredMolecules(WATER));
            assertValue(600, game.reservedSpace());
            assertTrue(game.isStatesUnlocked());
            game.reset();
            assertFalse(game.state().hasSubstance(WATER));
            assertTrue(game.reservedSpace().isZero());
            assertEquals(1.0, game.moleculeBoost(Molecule.Stat.PARTICLES), 0);
        }
    }
}
