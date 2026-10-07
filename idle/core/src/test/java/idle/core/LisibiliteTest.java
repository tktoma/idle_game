package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ce qui aide à s'y retrouver : l'astre visé, l'espace que demande encore la liste de courses et
 * le temps qu'il faudra, ce que rapporte une création pour l'espace qu'elle prend, et le meilleur
 * achat du moment.
 */
class LisibiliteTest {

    private static Game newGame() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    /** Après un premier Big Bang, tout ouvert jusqu'aux astres, sans les automatismes de rassemblement et de formation. */
    private static Game openGame(double space) {
        Game game = newGame();
        game.state().setBigBangs(1);
        game.state().setSpace(BigNum.of(space));
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
        }
        game.state().setAutoGather(false);
        game.state().setAutoForm(false);
        return game;
    }

    private static void fillTable(Game game, int copies) {
        for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), copies);
    }

    @Nested
    class AstreVise {

        @Test
        void aucunAvantLesAstres() {
            Game game = newGame();
            assertNull(game.nextBody());
            game.state().setBigBangs(1);
            assertNull(game.nextBody());
        }

        @Test
        void cEstCeluiDeLaListeDeCourses() {
            Game game = openGame(1e12);
            Body body = game.nextBody();
            assertNotNull(body);
            assertEquals(ShoppingList.Kind.BODY, game.shoppingList().kind());
            assertEquals(body.name(), game.shoppingList().target());
            for (String smaller : body.bodies()) assertTrue(game.hasBody(smaller));
        }

        @Test
        void tousFormesIlNYEnAPlus() {
            Game game = openGame(1e12);
            for (Body body : game.bodies()) game.state().addBody(body.id());
            assertNull(game.nextBody());
        }
    }

    @Nested
    class EspaceDeLaListeDeCourses {

        @Test
        void rienAViserNeDemandeRien() {
            Game game = newGame();
            assertTrue(game.shoppingSpace().isZero());
            assertEquals(0, game.secondsUntilShopping(), 0);
        }

        @Test
        void cEstLeVolumeDeCeQuiManqueEtDesLieuxAOuvrir() {
            Game game = openGame(1e12);
            ShoppingList list = game.shoppingList();
            assertFalse(list.items().isEmpty());
            double expected = 0;
            java.util.Map<Molecule.State, Integer> covered = new java.util.EnumMap<>(Molecule.State.class);
            for (ShoppingList.Item item : list.items()) {
                double volume = game.moleculeVolume(item.molecule().id()).toDouble();
                expected += item.missing() * volume;
                if (item.gather()) {
                    expected += game.substanceSpace(item.molecule().id()).toDouble();
                    covered.merge(item.molecule().state(), item.missing(), Integer::sum);
                }
            }
            // La matière qui manque dans un état, au-delà de ce que ces molécules apporteront, compte au moins pour quelque chose.
            double space = game.shoppingSpace().toDouble();
            assertTrue(space >= expected - 1e-6, space + " < " + expected);
            boolean matterLeft = false;
            for (java.util.Map.Entry<Molecule.State, Integer> lacking : list.matter().entrySet()) {
                matterLeft |= lacking.getValue() > covered.getOrDefault(lacking.getKey(), 0);
            }
            if (!matterLeft) assertEquals(expected, space, 1e-6);
            else assertTrue(space > expected);
        }

        @Test
        void leTempsEstCeQuiManqueDiviseParLExpansion() {
            Game game = openGame(1e12);
            // L'espace libre suffit largement.
            assertEquals(0, game.secondsUntilShopping(), 0);
            // Presque plus de place : il faut attendre l'expansion.
            double needed = game.shoppingSpace().toDouble();
            game.state().setSpace(game.usedSpace().add(BigNum.of(needed / 2)));
            double wait = game.secondsUntilShopping();
            double rate = game.spacePerSecond().toDouble();
            assertEquals((needed - needed / 2) / rate, wait, wait * 1e-6);
            assertTrue(wait > 0);
        }

        @Test
        void uneMoleculeCreeeFaitBaisserLEspaceDemande() {
            Game game = openGame(1e12);
            fillTable(game, 500);
            ShoppingList.Item first = game.shoppingList().items().get(0);
            double before = game.shoppingSpace().toDouble();
            assertTrue(game.createMolecule(first.molecule().id()));
            assertTrue(game.shoppingSpace().toDouble() < before);
        }

        @Test
        void pourLeCosmosCEstLaPlusPetiteMoleculeDeChaqueEtat() {
            Game game = openGame(1e12);
            for (Body body : game.bodies()) game.state().addBody(body.id());
            game.state().setCosmosLevel(1);                           // la galaxie est formée
            ShoppingList list = game.shoppingList();
            assertEquals(ShoppingList.Kind.COSMOS, list.kind());
            assertFalse(list.matter().isEmpty());
            double expected = 0;
            for (java.util.Map.Entry<Molecule.State, Integer> lacking : list.matter().entrySet()) {
                double smallest = Double.POSITIVE_INFINITY;
                for (Molecule molecule : game.molecules()) {
                    if (molecule.hasState() && molecule.state() == lacking.getKey() && game.isMoleculeKindUnlocked(molecule.kind())) {
                        smallest = Math.min(smallest, game.moleculeVolume(molecule.id()).toDouble());
                    }
                }
                expected += lacking.getValue() * smallest;
            }
            // Et la variété : chaque sorte qui manque compte pour ses premières molécules et pour son lieu.
            assertFalse(list.sorts().isEmpty());
            for (java.util.Map.Entry<Molecule.State, Integer> lacking : list.sorts().entrySet()) {
                double smallest = Double.POSITIVE_INFINITY;
                for (Molecule molecule : game.molecules()) {
                    if (molecule.hasState() && molecule.state() == lacking.getKey() && game.isMoleculeKindUnlocked(molecule.kind())) {
                        smallest = Math.min(smallest, game.moleculeVolume(molecule.id()).toDouble());
                    }
                }
                expected += lacking.getValue() * 2.0 * Game.SUBSTANCE_MOLECULES * smallest;
            }
            assertEquals(expected, game.shoppingSpace().toDouble(), expected * 1e-9);
            // Sans espace libre, il faut attendre toute cette place.
            game.state().setSpace(game.usedSpace());
            assertEquals(expected / game.spacePerSecond().toDouble(), game.secondsUntilShopping(), expected * 1e-9);
        }
    }

    @Nested
    class MeilleurAchat {

        @Test
        void rienAvantLeBigBang() {
            Game game = newGame();
            assertNull(game.bestMolecule());
        }

        @Test
        void laPremiereMoleculeRapportePlusQueLaCentieme() {
            Game game = openGame(1e9);
            fillTable(game, 500);
            double first = game.moleculeGain("H2O");
            assertTrue(first > 0);
            game.state().setMoleculeCount("H2O", 100);
            double later = game.moleculeGain("H2O");
            assertTrue(later > 0 && later < first, later + " contre " + first);
        }

        @Test
        void leGainEstCeQueLaCreationAjouteVraiment() {
            Game game = openGame(1e9);
            fillTable(game, 500);
            Molecule water = game.molecule("H2O");
            Molecule.Boost boost = (Molecule.Boost) water.bonus();
            double before = game.matterBoost(boost.stat());
            double promised = game.moleculeGain("H2O");
            assertTrue(game.createMolecule("H2O"));
            double after = game.matterBoost(boost.stat());
            // Non rassemblée, l'eau n'agit que sur sa propre grandeur : le gain promis est le rapport des deux.
            assertEquals(after / before - 1, promised, 1e-9);
        }

        @Test
        void leRendementEstLeGainParUniteDEspace() {
            Game game = openGame(1e9);
            assertEquals(game.moleculeGain("H2O") / game.moleculeNextSpace("H2O").toDouble(), game.moleculeYield("H2O"), 1e-15);
            assertEquals(game.moleculeVolume("H2O").toDouble(), game.moleculeNextSpace("H2O").toDouble(), 1e-9);
            // Rassemblée, une création ajoute plusieurs molécules : elle prend la place de toutes.
            game.state().setMoleculeCount("H2O", 9);
            game.state().addSubstance("H2O");
            assertEquals(game.moleculeVolume("H2O").toDouble() * game.moleculesPerCreation("H2O"),
                    game.moleculeNextSpace("H2O").toDouble(), 1e-9);
        }

        @Test
        void uneMoleculeQuiReleveUnPlafondNeRapporteRienIci() {
            Game game = openGame(1e9);
            for (Molecule molecule : game.molecules()) {
                if (molecule.bonus() instanceof Molecule.Uncap) {
                    assertEquals(0, game.moleculeGain(molecule.id()), 0, molecule.id());
                    return;
                }
            }
        }

        @Test
        void leMeilleurAchatEstCreableEtAucunAutreCreableNeRendPlus() {
            Game game = openGame(1e9);
            fillTable(game, 500);
            Molecule best = game.bestMolecule();
            assertNotNull(best);
            assertTrue(game.canCreateMolecule(best.id()));
            double top = game.moleculeYield(best.id());
            for (Molecule molecule : game.molecules()) {
                if (game.canCreateMolecule(molecule.id())) assertTrue(game.moleculeYield(molecule.id()) <= top + 1e-18, molecule.id());
            }
        }

        @Test
        void sansElementsNiEspaceIlNYAPasDeMeilleurAchat() {
            Game game = openGame(1e9);
            assertNull(game.bestMolecule());                       // tableau périodique vide
            fillTable(game, 500);
            game.state().setSpace(BigNum.ZERO);
            assertNull(game.bestMolecule());                       // plus de place
        }

        @Test
        void ilChangeQuandLaSorteSEpuise() {
            Game game = openGame(1e9);
            fillTable(game, 500);
            Molecule first = game.bestMolecule();
            for (int i = 0; i < 200 && first.equals(game.bestMolecule()); i++) {
                fillTable(game, 500);
                assertTrue(game.createMolecule(first.id()));
            }
            assertFalse(first.equals(game.bestMolecule()), "à force d'en créer, une autre sorte devient plus rentable");
        }
    }
}
