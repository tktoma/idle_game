package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'automatisation du troisième acte et ses réglages : l'ordre d'achat choisi par le joueur,
 * l'arbre de matière noire qui se rachète seul, la réserve d'espace et la cadence de la création
 * automatique, « tout confier », le rassemblement et la formation automatiques.
 */
class AutomatisationTest {

    private static Game newGame() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    /** Une partie qui connaît la matière noire : assez pour tous ses automatismes, sauf celui de l'arbre. */
    private static Game darkGame() {
        Game game = newGame();
        game.state().setDarkMatter(BigNum.of(20));
        game.state().setExplosions(1);
        return game;
    }

    /** Après un Big Bang, de l'espace, et les améliorations d'espace données par leur identifiant. */
    private static Game spaceGame(double space, String... upgrades) {
        Game game = newGame();
        game.state().setBigBangs(1);
        game.state().setSpace(BigNum.of(space));
        for (String id : upgrades) game.state().addSpaceUpgrade(id);
        return game;
    }

    private static void fillTable(Game game, int copies) {
        for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), copies);
    }

    private static void give(Game game, String id, int count) {
        game.state().setMoleculeCount(id, count);
        game.state().addSubstance(id);
    }

    private static List<String> ids(List<Upgrade> upgrades) {
        List<String> ids = new ArrayList<>();
        for (Upgrade upgrade : upgrades) ids.add(upgrade.id());
        return ids;
    }

    @Nested
    class OrdreDesAmeliorationsEnAtomes {

        @Test
        void sansReglageCEstLOrdreDuCatalogueEtLeMoinsCherDAbord() {
            Game game = darkGame();
            assertFalse(game.isAtomUpgradeOrdered());
            List<String> catalog = new ArrayList<>();
            for (Upgrade upgrade : game.upgrades()) {
                if (upgrade.resource() == Resource.ATOMS) catalog.add(upgrade.id());
            }
            assertEquals(catalog, ids(game.atomUpgradeOrder()));
            assertTrue(catalog.size() >= 3);
        }

        @Test
        void uneAmeliorationMonteEtDescendDUnRang() {
            Game game = darkGame();
            List<String> order = ids(game.atomUpgradeOrder());
            String first = order.get(0);
            String second = order.get(1);
            String last = order.get(order.size() - 1);
            assertFalse(game.moveAtomUpgrade(first, -1));           // déjà en tête
            assertFalse(game.moveAtomUpgrade(last, 1));             // déjà en queue
            assertTrue(game.moveAtomUpgrade(second, -1));
            assertEquals(second, game.atomUpgradeOrder().get(0).id());
            assertEquals(first, game.atomUpgradeOrder().get(1).id());
            assertTrue(game.moveAtomUpgrade(second, 1));
            assertEquals(order, ids(game.atomUpgradeOrder()));
            // Une direction plus grande que 1 ne déplace que d'un rang.
            assertTrue(game.moveAtomUpgrade(first, 5));
            assertEquals(first, game.atomUpgradeOrder().get(1).id());
            assertThrows(IllegalArgumentException.class, () -> game.moveAtomUpgrade("inconnue", 1));
        }

        @Test
        void dansLOrdreDuJoueurLaPremiereEstAttendue() {
            Game game = darkGame();
            // La plus chère de celles qui tiennent sous le plafond d'atomes, mise en tête.
            Upgrade costly = null;
            for (Upgrade upgrade : game.atomUpgradeOrder()) {
                BigNum cost = game.costOf(upgrade.id());
                if (cost.gt(game.atomCap())) continue;
                if (costly == null || cost.gt(game.costOf(costly.id()))) costly = upgrade;
            }
            BigNum cost = game.costOf(costly.id());
            assertTrue(cost.gt(BigNum.ONE), "il faut une amélioration plus chère que les moins chères");
            while (game.moveAtomUpgrade(costly.id(), -1)) { }
            assertEquals(costly.id(), game.atomUpgradeOrder().get(0).id());
            game.setAtomUpgradeOrdered(true);
            assertTrue(game.setDarkAutomationEnabled("dark_auto_atoms", true));

            // Pas tout à fait assez : rien n'est acheté, alors que d'autres améliorations sont payables.
            game.state().setAtoms(cost.subtract(BigNum.ONE));
            game.tick(5);
            assertEquals(cost.subtract(BigNum.ONE), game.state().atoms());
            for (Upgrade upgrade : game.atomUpgradeOrder()) assertEquals(0, game.levelOf(upgrade.id()), upgrade.id());

            // Assez : c'est elle qui est achetée, et pas une moins chère.
            game.state().setAtoms(cost);
            game.tick(1.05);
            assertEquals(1, game.levelOf(costly.id()));

            // Revenu au moins cher d'abord, il reprend les petites.
            game.setAtomUpgradeOrdered(false);
            game.state().setAtoms(BigNum.ONE);
            game.tick(1.05);
            assertTrue(game.state().atoms().isZero());
        }

        @Test
        void ceQuiDepasseLePlafondOuEstAuMaximumEstSaute() {
            Game game = darkGame();
            game.setAtomUpgradeOrdered(true);
            assertTrue(game.setDarkAutomationEnabled("dark_auto_atoms", true));
            // Une amélioration que le plafond d'atomes interdit, mise en tête : elle ne bloque pas la suite.
            Upgrade blocked = null;
            for (Upgrade upgrade : game.atomUpgradeOrder()) {
                if (game.costOf(upgrade.id()).gt(game.atomCap())) blocked = upgrade;
            }
            if (blocked != null) {
                while (game.moveAtomUpgrade(blocked.id(), -1)) { }
            }
            game.state().setAtoms(game.atomCap());
            game.tick(1.05);
            int bought = 0;
            for (Upgrade upgrade : game.atomUpgradeOrder()) bought += game.levelOf(upgrade.id());
            assertEquals(1, bought);
            if (blocked != null) assertEquals(0, game.levelOf(blocked.id()));

            // Une amélioration au maximum en tête : il passe à la suivante.
            Upgrade limited = null;
            for (Upgrade upgrade : game.atomUpgradeOrder()) {
                if (upgrade != blocked && upgrade.hasLimit() && limited == null) limited = upgrade;
            }
            if (limited != null) {
                game.state().setLevel(limited.id(), limited.maxLevel());
                while (game.moveAtomUpgrade(limited.id(), -1)) { }
                assertTrue(game.isMaxed(limited.id()));
                game.state().setAtoms(game.atomCap());
                int before = 0;
                for (Upgrade upgrade : game.atomUpgradeOrder()) before += game.levelOf(upgrade.id());
                game.tick(1.05);
                int after = 0;
                for (Upgrade upgrade : game.atomUpgradeOrder()) after += game.levelOf(upgrade.id());
                assertEquals(before + 1, after);
            }
        }

        @Test
        void lOrdreTraverseLExplosionEtLeBigBangMaisPasLaRemiseAZero() {
            Game game = darkGame();
            String second = game.atomUpgradeOrder().get(1).id();
            assertTrue(game.moveAtomUpgrade(second, -1));
            game.setAtomUpgradeOrdered(true);
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertTrue(game.isAtomUpgradeOrdered());
            assertEquals(second, game.atomUpgradeOrder().get(0).id());
            game.reset();
            assertFalse(game.isAtomUpgradeOrdered());
            assertFalse(second.equals(game.atomUpgradeOrder().get(0).id()));
        }

        @Test
        void unIdentifiantInconnuDansLaSauvegardeEstIgnore() {
            Game game = darkGame();
            List<String> order = ids(game.atomUpgradeOrder());
            game.state().setAtomUpgradeOrder(List.of("disparue", order.get(2), order.get(2)));
            List<String> read = ids(game.atomUpgradeOrder());
            assertEquals(order.size(), read.size());
            assertEquals(order.get(2), read.get(0));
            assertEquals(order.get(0), read.get(1));
        }
    }

    @Nested
    class OrdreDesAutomatismes {

        @Test
        void unAutomatismeMonteEtDescend() {
            Game game = darkGame();
            List<Automation> order = game.automationOrder();
            assertEquals(game.automations().size(), order.size());
            assertFalse(game.isAutomationOrdered());
            String last = order.get(order.size() - 1).id();
            assertFalse(game.moveAutomation(last, 1));
            while (game.moveAutomation(last, -1)) { }
            assertEquals(last, game.automationOrder().get(0).id());
            assertThrows(IllegalArgumentException.class, () -> game.moveAutomation("inconnu", 1));
        }

        @Test
        void dansLOrdreDuJoueurLePremierEstServiJusquAuBout() {
            Game game = darkGame();
            game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
            game.state().setLevel("atom_keep", 1);                       // Persistance : sans elle, il n'achète pas d'automatisme
            // La fusion automatique en tête : elle est achetée puis portée à sa cadence maximale avant tout autre.
            Automation fusion = null;
            for (Automation automation : game.automations()) {
                if (automation.kind() == Automation.Kind.FUSION) fusion = automation;
            }
            while (game.moveAutomation(fusion.id(), -1)) { }
            game.setAutomationOrdered(true);
            assertTrue(game.setDarkAutomationEnabled("dark_auto_machines", true));
            game.state().setAtoms(game.atomCap());
            for (int i = 0; i < 40 && !(game.ownsAutomation(fusion.id()) && game.isAutomationMaxed(fusion.id())); i++) {
                game.state().setAtoms(game.atomCap());
                game.tick(1.05);
                if (!(game.ownsAutomation(fusion.id()) && game.isAutomationMaxed(fusion.id()))) {
                    for (Automation automation : game.automations()) {
                        if (automation != fusion) assertFalse(game.ownsAutomation(automation.id()), automation.id());
                    }
                }
            }
            assertTrue(game.ownsAutomation(fusion.id()));
            assertTrue(game.isAutomationMaxed(fusion.id()));
            // Ensuite seulement, le suivant de la liste.
            game.state().setAtoms(game.atomCap());
            game.tick(1.05);
            assertTrue(game.ownsAutomation(game.automationOrder().get(1).id()));
        }
    }

    @Nested
    class ArbreDeMatiereNoire {

        @Test
        void ilAttendLePremierBigBang() {
            Game game = darkGame();
            assertFalse(game.isDarkAutomationUnlocked("dark_auto_tree"));
            assertFalse(game.setDarkAutomationEnabled("dark_auto_tree", true));
            game.state().setBigBangs(1);
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_tree"));
            assertTrue(game.setDarkAutomationEnabled("dark_auto_tree", true));
        }

        @Test
        void ilAcheteLesCasesEtLesAmeliorationsLimiteesMaisPasLesAutres() {
            Game game = darkGame();
            game.state().setBigBangs(1);
            game.state().setDarkMatter(BigNum.of(1, 9));
            game.state().setParticles(BigNum.of(1, 300));
            game.state().setAtoms(BigNum.of(1, 300));
            game.state().setDarkMatterSize(BigNum.of(1, 40));
            assertTrue(game.setDarkAutomationEnabled("dark_auto_tree", true));
            game.tick(1.05);
            int afterOne = 0;
            for (DarkUpgrade upgrade : game.darkUpgrades()) afterOne += game.darkLevelOf(upgrade.id());
            assertEquals(1, afterOne);                              // une action, un achat
            for (int i = 0; i < 400; i++) {
                game.state().setParticles(BigNum.of(1, 300));
                game.state().setAtoms(BigNum.of(1, 300));
                game.tick(1.05);
            }
            for (DarkUpgrade upgrade : game.darkUpgrades()) {
                int level = game.darkLevelOf(upgrade.id());
                boolean tree = upgrade.branch().inTree();
                if (upgrade.hasLimit()) {
                    // Tout ce qui a un dernier niveau y est porté, case ou boutique.
                    if (game.isDarkAvailable(upgrade.id())) assertEquals(upgrade.maxLevel(), level, upgrade.id());
                } else {
                    // Une case sans dernier niveau n'est prise qu'une fois, pour ouvrir la suivante ; en boutique, jamais.
                    assertEquals(tree ? 1 : 0, level, upgrade.id());
                }
            }
            // Les particules et les atomes ne sont donc pas engloutis : il en reste pour le Big Bang.
            assertTrue(game.state().particles().gt(BigNum.of(1, 299)));
        }

        @Test
        void ilNAcheteRienPendantUnDefiNiCoupe() {
            Game game = darkGame();
            game.state().setBigBangs(1);
            game.state().setDarkMatter(BigNum.of(1, 9));
            assertTrue(game.setDarkAutomationEnabled("dark_auto_tree", true));
            game.setAutomationPaused(true);
            game.tick(5);
            for (DarkUpgrade upgrade : game.darkUpgrades()) assertEquals(0, game.darkLevelOf(upgrade.id()), upgrade.id());
            game.setAutomationPaused(false);
            game.tick(1.05);
            int bought = 0;
            for (DarkUpgrade upgrade : game.darkUpgrades()) bought += game.darkLevelOf(upgrade.id());
            assertEquals(1, bought);
        }
    }

    @Nested
    class CreationAutomatique {

        private Game game(double space) {
            Game game = spaceGame(space, "space_states");
            game.state().setBigBangs(2);
            fillTable(game, 500);
            return game;
        }

        @Test
        void laCadencePasseDeCinqSecondesADeux() {
            // Assez d'espace pour créer, pas assez pour la seconde cadence (20 millions).
            Game game = game(1e6);
            assertEquals(Game.MOLECULE_AUTOMATION_SECONDS, game.moleculeAutomationInterval(), 0);
            game.state().addSpaceUpgrade("space_creation_pace");
            assertEquals(2, game.moleculeAutomationInterval(), 0);
            give(game, "H2O", 3);
            assertTrue(game.setMoleculeAutomated("H2O", true));
            game.tick(1.9);
            assertEquals(3, game.moleculeCount("H2O"));
            game.tick(0.2);
            assertTrue(game.moleculeCount("H2O") > 3);
            // La seconde, en fin de partie : une création par seconde.
            game.state().addSpaceUpgrade("space_creation_pace_2");
            assertEquals(1, game.moleculeAutomationInterval(), 0);
        }

        @Test
        void laReserveLaisseSaPartDEspaceLibre() {
            Game game = game(1e9);
            give(game, "H2O", 3);
            assertTrue(game.setMoleculeAutomated("H2O", true));
            assertEquals(0, game.autoMoleculeReserve(), 0);
            // L'espace est presque tout pris : il en reste moins que la réserve voulue.
            game.setAutoMoleculeReserve(0.5);
            game.state().setSpace(game.usedSpace().multiply(1.5));       // un tiers de libre, moins que la moitié
            int before = game.moleculeCount("H2O");
            game.tick(Game.MOLECULE_AUTOMATION_SECONDS * 3);
            assertEquals(before, game.moleculeCount("H2O"));
            // À la main, la réserve ne compte pas.
            assertTrue(game.createMolecule("H2O"));
            int byHand = game.moleculeCount("H2O");
            // Une réserve plus petite que ce qui reste libre : la création reprend.
            game.setAutoMoleculeReserve(0.10);
            game.state().setSpace(game.usedSpace().multiply(3));
            game.tick(Game.MOLECULE_AUTOMATION_SECONDS);
            assertTrue(game.moleculeCount("H2O") > byHand);
        }

        @Test
        void laReserveResteEntreZeroEtQuatreVingtQuinzePourCent() {
            Game game = game(1e9);
            game.setAutoMoleculeReserve(-1);
            assertEquals(0, game.autoMoleculeReserve(), 0);
            game.setAutoMoleculeReserve(2);
            assertEquals(0.95, game.autoMoleculeReserve(), 1e-12);
            game.setAutoMoleculeReserve(Double.NaN);
            assertEquals(0, game.autoMoleculeReserve(), 0);
        }

        @Test
        void toutConfierPrendToutesLesSortesRassemblees() {
            Game game = game(1e9);
            give(game, "H2O", 3);
            give(game, "CO2", 3);
            game.state().setMoleculeCount("H2O2", 5);                  // créée, pas rassemblée
            assertTrue(game.setMoleculeAutomated("H2O", true));
            assertEquals(1, game.setAllMoleculesAutomated(true));      // il n'en restait qu'une à confier
            assertTrue(game.isMoleculeAutomated("CO2"));
            assertFalse(game.isMoleculeAutomated("H2O2"));
            assertEquals(2, game.automatedMolecules());
            assertEquals(0, game.setAllMoleculesAutomated(true));
            assertEquals(2, game.setAllMoleculesAutomated(false));
            assertEquals(0, game.automatedMolecules());
        }

        @Test
        void toutConfierNeFaitRienSansLAutomatisme() {
            Game game = game(1e9);
            game.state().setBigBangs(1);
            give(game, "H2O", 3);
            assertEquals(0, game.setAllMoleculesAutomated(true));
            assertEquals(0, game.automatedMolecules());
        }
    }

    @Nested
    class RassemblementEtFormationAutomatiques {

        @Test
        void ilsViennentAvecLEspace() {
            Game game = spaceGame(0, "space_states");
            assertFalse(game.isAutoGatherUnlocked() || game.isAutoFormUnlocked());
            assertFalse(game.setAutoGatherEnabled(false));
            assertFalse(game.setAutoFormEnabled(false));
            game.state().setSpace(BigNum.of(1, 12));
            game.tick(0.1);
            assertTrue(game.isAutoGatherUnlocked() && game.isAutoFormUnlocked());
            // En marche dès qu'ils sont acquis.
            assertTrue(game.isAutoGatherEnabled() && game.isAutoGathering());
            assertTrue(game.isAutoFormEnabled() && game.isAutoForming());
        }

        @Test
        void uneSortePreteSeRassembleSeule() {
            Game game = spaceGame(1e9, "space_states", "space_bulk_form", "space_auto_gather");
            game.state().setMoleculeCount("H2O", Game.SUBSTANCE_MOLECULES);
            game.state().setMoleculeCount("CO2", Game.SUBSTANCE_MOLECULES - 1);
            assertEquals(0, game.takeAutoGathered());
            game.tick(Game.AUTO_FORM_SECONDS / 2);
            assertFalse(game.hasSubstance("H2O"));                      // une fois par seconde
            game.tick(Game.AUTO_FORM_SECONDS / 2 + 0.01);
            assertTrue(game.hasSubstance("H2O"));
            assertFalse(game.hasSubstance("CO2"));                      // il lui manque une molécule
            assertEquals(1, game.takeAutoGathered());
            assertEquals(0, game.takeAutoGathered());                  // annoncé une seule fois
        }

        @Test
        void coupeIlNeRassemblePlus() {
            Game game = spaceGame(1e9, "space_states", "space_bulk_form", "space_auto_gather");
            game.state().setMoleculeCount("H2O", Game.SUBSTANCE_MOLECULES);
            assertTrue(game.setAutoGatherEnabled(false));
            assertFalse(game.isAutoGathering());
            game.tick(5);
            assertFalse(game.hasSubstance("H2O"));
            assertTrue(game.setAutoGatherEnabled(true));
            // La coupure générale l'arrête aussi.
            game.setAutomationPaused(true);
            assertFalse(game.isAutoGathering());
            game.tick(5);
            assertFalse(game.hasSubstance("H2O"));
            game.setAutomationPaused(false);
            game.tick(1.05);
            assertTrue(game.hasSubstance("H2O"));
        }

        @Test
        void lesAssemblagesEtLesAstresPretsSeFormentSeuls() {
            Game game = newGame();
            game.state().setBigBangs(1);
            game.state().setSpace(BigNum.of(1, 12));
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
            }
            game.takeNewSpaceUpgrades();
            // De tout, en quantité : chaque assemblage puis chaque astre à portée doit se former sans un clic.
            for (Molecule molecule : game.molecules()) give(game, molecule.id(), 100_000);
            int assemblies = game.assembliesReady();
            assertTrue(assemblies > 0);
            game.tick(Game.AUTO_FORM_SECONDS + 0.01);
            List<String> formed = game.takeAutoFormed();
            assertTrue(formed.size() >= assemblies, formed.toString());
            assertEquals(0, game.assembliesReady());
            assertEquals(0, game.bodiesReady());
            assertTrue(game.bodiesFormed() > 0);
            assertEquals(List.of(), game.takeAutoFormed());
            // Les échelles du cosmos restent au joueur.
            assertEquals(0, game.cosmosFormed());
        }

        @Test
        void coupeeLaFormationNeFaitRien() {
            Game game = newGame();
            game.state().setBigBangs(1);
            game.state().setSpace(BigNum.of(1, 12));
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
            }
            for (Molecule molecule : game.molecules()) give(game, molecule.id(), 100_000);
            assertTrue(game.setAutoFormEnabled(false));
            int ready = game.assembliesReady();
            game.tick(5);
            assertEquals(ready, game.assembliesReady());
            assertEquals(List.of(), game.takeAutoFormed());
        }

        @Test
        void lesReglagesTraversentLeBigBangMaisPasLaRemiseAZero() {
            Game game = spaceGame(1e9, "space_states", "space_bulk_form", "space_auto_gather", "space_auto_form");
            assertTrue(game.setAutoGatherEnabled(false));
            assertTrue(game.setAutoFormEnabled(false));
            game.setAutoMoleculeReserve(0.25);
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertFalse(game.isAutoGatherEnabled());
            assertFalse(game.isAutoFormEnabled());
            assertEquals(0.25, game.autoMoleculeReserve(), 0);
            game.reset();
            assertTrue(game.isAutoGatherEnabled());
            assertTrue(game.isAutoFormEnabled());
            assertEquals(0, game.autoMoleculeReserve(), 0);
        }
    }
}
