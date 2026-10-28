package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Body;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ce qui aide à s'y retrouver : l'astre visé, l'espace que demande encore la liste de courses et
 * le temps qu'il faudra, ce que rapporte une création pour l'espace qu'elle prend, et le meilleur
 * achat du moment.
 */
class LisibiliteTest {

    private static Game newGame() {
        return GameTestSupport.realGame();
    }

    /** Après un premier Big Bang, tout ouvert jusqu'aux astres, sans les automatismes de rassemblement et de formation. */
    private static Game openGame(double space) {
        Game game = newGame();
        GameTestSupport.bangs(game, 1);
        game.state().setSpace(BigNum.of(space));
        GameTestSupport.openSpace(game);
        game.state().setAutoGather(false);
        game.state().setAutoForm(false);
        return game;
    }

    private static void fillTable(Game game, int copies) {
        GameTestSupport.fillTable(game, copies);
    }

    @Nested
    class AstreVise {

        @Test
        void aucunAvantLesAstres() {
            Game game = newGame();
            assertNull(game.nextBody());
            GameTestSupport.bangs(game, 1);
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
        void lesMatieresExotiquesAttendentLeHuitiemeBigBang() {
            // Version 0.138 : au septième Big Bang, la nébuleuse planétaire (zéro condition manquante une fois sa
            // matière réunie) ne doit pas passer devant le dernier astre ordinaire, le seul que le joueur peut former.
            Game game = openGame(1e12);
            GameTestSupport.bangs(game, 7);
            assertFalse(game.isExoticUnlocked());
            for (Body body : game.bodies()) {
                if (body.tier() != Body.Tier.EXOTIC && !body.id().equals("supermassive_black_hole")) game.state().addBody(body.id());
            }
            assertEquals("supermassive_black_hole", game.nextBody().id(), "le dernier astre ordinaire");
            assertEquals(ShoppingList.Kind.BODY, game.shoppingList().kind());
            assertEquals(game.body("supermassive_black_hole").name(), game.shoppingList().target());
            game.state().addBody("supermassive_black_hole");
            assertNull(game.nextBody(), "avant le huitième Big Bang, aucune matière exotique n'est visée");
            GameTestSupport.bangs(game, 8);
            assertTrue(game.isExoticUnlocked());
            assertEquals(Body.Tier.EXOTIC, game.nextBody().tier(), "au huitième, les matières exotiques reviennent");
        }

        @Test
        void tousFormesIlNYEnAPlus() {
            Game game = openGame(1e12);
            for (Body body : game.bodies()) game.state().addBody(body.id()); Systemes.trois(game);
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
            // La liste vise l'amas rocheux : la roche sableuse (250 quartz, 30 calcites, 10 hématites, 5 zircons, chacun
            // à rassembler), et 400 cristaux de matière.
            Game game = openGame(1e12);
            ShoppingList list = game.shoppingList();
            assertEquals("Amas rocheux", list.target());
            assertEquals(4, list.items().size());
            assertEquals(java.util.Map.of(Molecule.State.CRYSTAL, 400), list.matter());
            // Les molécules : 250 × 300 + 30 × 500 + 10 × 760 + 5 × 860 = 101 900 ; leurs quatre lieux : 900 + 1 500 + 2 280 + 2 580 = 7 260 ;
            // la matière au-delà de ces 295 cristaux : 105 fois le plus petit cristal (40) = 4 200.
            assertEquals(113_360, game.shoppingSpace().toDouble(), 1e-6);
            // Les 250 quartz créés (pas encore rassemblés) : leurs 75 000 ne manquent plus, leur lieu (900) si ; et comme ils
            // n'apporteront plus de cristaux en les rassemblant, la matière qui manque remonte de 105 à 355 (+10 000).
            game.state().setMoleculeCount("SiO2", 250);
            assertEquals(48_360, game.shoppingSpace().toDouble(), 1e-6);
        }

        @Test
        void leTempsEstCeQuiManqueDiviseParLExpansion() {
            Game game = openGame(1e12);
            // L'espace libre suffit largement.
            assertEquals(0, game.secondsUntilShopping(), 0);
            // Presque plus de place : il faut attendre l'expansion.
            assertEquals(113_360, game.shoppingSpace().toDouble(), 1e-6);
            game.state().setSpace(game.usedSpace().add(BigNum.of(56_680)));
            assertEquals(1, game.spacePerSecond().toDouble(), 1e-9, "après un Big Bang, une unité d'espace par seconde");
            assertEquals(56_680, game.secondsUntilShopping(), 1e-6, "la moitié qui manque, à une unité par seconde");
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
            for (Body body : game.bodies()) game.state().addBody(body.id()); Systemes.trois(game);
            game.state().setCosmosLevel(1);                           // la galaxie est formée
            ShoppingList list = game.shoppingList();
            assertEquals(ShoppingList.Kind.COSMOS, list.kind());
            // L'amas de galaxies demande sa matière dans les cinq états, et de la variété dans chacun.
            assertEquals(java.util.Map.of(Molecule.State.GAS, 100_000_000, Molecule.State.LIQUID, 10_000_000, Molecule.State.SOLID, 2_000_000,
                    Molecule.State.CRYSTAL, 20_000_000, Molecule.State.METAL, 1_000_000), list.matter());
            assertEquals(java.util.Map.of(Molecule.State.GAS, 25, Molecule.State.LIQUID, 35, Molecule.State.SOLID, 30,
                    Molecule.State.CRYSTAL, 120, Molecule.State.METAL, 4), list.sorts());
            // Chaque unité de matière compte pour la plus petite molécule de son état, chaque sorte pour ses trois premières
            // molécules et son lieu (six fois la plus petite) : 43 605 192 360 en tout.
            assertEquals(4.360519236e10, game.shoppingSpace().toDouble(), 1);
            // Sans espace libre, il faut attendre toute cette place : 7 160,4 unités par seconde, soit 70 jours et demi.
            game.state().setSpace(game.usedSpace());
            assertEquals(7_160.4, game.spacePerSecond().toDouble(), 1e-6);
            assertEquals(6_089_770.45, game.secondsUntilShopping(), 0.01);
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
            assertEquals(0.05, game.moleculeGain("H2O"), 1e-12, "la première eau : +5 %");
            assertEquals(200, game.moleculeVolume("H2O").toDouble(), 1e-9);
            assertEquals(200, game.moleculeNextSpace("H2O").toDouble(), 1e-9, "non rassemblée, une création prend une molécule de place");
            assertEquals(2.5e-4, game.moleculeYield("H2O"), 1e-15, "0,05 pour 200 unités d'espace");
            // Rassemblée, une création ajoute plusieurs molécules : elle prend la place de toutes.
            game.state().setMoleculeCount("H2O", 9);
            game.state().addSubstance("H2O");
            assertEquals(4, game.moleculesPerCreation("H2O"), "neuf eaux rassemblées : quatre par création");
            assertEquals(800, game.moleculeNextSpace("H2O").toDouble(), 1e-9);
        }

        @Test
        void uneMoleculeQuiReleveUnPlafondNeRapporteRienIci() {
            Game game = openGame(1e9);
            int uncaps = 0;
            for (Molecule molecule : game.molecules()) {
                if (!(molecule.bonus() instanceof Molecule.Uncap)) continue;
                uncaps++;
                assertEquals(0, game.moleculeGain(molecule.id()), 0, molecule.id());
            }
            assertEquals(153, uncaps, "le dihydrogène, le quartz… : 153 molécules relèvent un plafond");
            assertTrue(game.molecule("H2").bonus() instanceof Molecule.Uncap);
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
