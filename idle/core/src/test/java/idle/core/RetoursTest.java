package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les retours de jeu de la version 0.47 : l'achat groupé en atomes dès le début, les gestes de
 * confort moins chers, l'automatisme des automatismes pendant les défis, et la liste de courses
 * qui suit les assemblages et les astres formés.
 */
class RetoursTest {

    private static Game newGame() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    @Nested
    class AchatGroupeEnAtomes {

        @Test
        void ilSAcheteEnAtomesDesLesPremiersAtomes() {
            Game game = newGame();
            assertFalse(game.isBulkAtomBuyUnlocked());
            game.state().setAtoms(BigNum.of(8));
            assertTrue(game.buy("atom_bulk"));
            assertTrue(game.isBulkAtomBuyUnlocked());
            assertTrue(game.isMaxed("atom_bulk"));               // un seul niveau
        }

        @Test
        void ilTientAuxFusionsMaisPasALExplosion() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(8));
            game.buy("atom_bulk");
            game.state().setParticles(BigNum.of(1, 9));
            while (game.buy("generator")) { /* de quoi fusionner */ }
            assertTrue(game.fuse());
            assertTrue(game.isBulkAtomBuyUnlocked());            // les améliorations en atomes restent à la fusion
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), element.category().maxCopies());
            assertTrue(game.explode());
            assertFalse(game.isBulkAtomBuyUnlocked());           // mais pas à l'explosion…
            game.state().setDarkLevel("dark_shop_bulk", 1);
            assertTrue(game.isBulkAtomBuyUnlocked());            // …sauf avec celui de la matière noire, pour toujours
        }
    }

    @Nested
    class GestesMoinsChers {

        @Test
        void creationContinueEtGestesGroupesCoutentDixFoisMoins() {
            Game game = newGame();
            assertEquals(BigNum.of(100), space(game, "space_hold_create"));
            assertEquals(BigNum.of(1_500), space(game, "space_bulk_form"));
        }

        private BigNum space(Game game, String id) {
            return game.spaceUpgrades().stream().filter(u -> u.id().equals(id)).findFirst().orElseThrow().space();
        }
    }

    @Nested
    class Defis {

        @Test
        void lAutomatismeDesAutomatismesAgitPendantUnDefi() {
            Game game = newGame();
            game.state().setExplosions(3);
            game.state().setDarkMatter(BigNum.of(1_000));
            assertTrue(game.setDarkAutomationEnabled("dark_auto_start", true));
            assertTrue(game.startChallenge("challenge_rust"));
            assertTrue(game.inChallenge());
            game.tick(60);
            assertTrue(game.ownsAutomation("auto_generator"), "les automatismes sont offerts pendant le défi");
        }
    }

    @Nested
    class Impulsion {

        private Game afterBigBang() {
            Game game = newGame();
            game.state().setBigBangs(1);
            return game;
        }

        @Test
        void avantLePremierBigBangIlNYEnAPas() {
            Game game = newGame();
            game.tick(60);
            assertFalse(game.isPulseUnlocked());
            assertEquals(0, game.pulseCharges());
            assertFalse(game.pulse());
        }

        @Test
        void uneChargeRevientToutesLesTroisSecondesJusquAQuarante() {
            Game game = afterBigBang();
            game.tick(Game.PULSE_REFILL_SECONDS * 5 + 0.01);
            assertEquals(5, game.pulseCharges());
            game.tick(3600);
            assertEquals(Game.PULSE_MAX_CHARGES, game.pulseCharges());
            assertEquals(0, game.secondsUntilPulse());
        }

        @Test
        void uneImpulsionDonneDixSecondesDExpansionDUnCoup() {
            Game game = afterBigBang();
            game.state().setPulseCharges(2);
            BigNum expected = game.spacePerSecond().multiply(Game.PULSE_SECONDS);
            BigNum before = game.state().space();
            assertTrue(game.pulse());
            assertEquals(before.add(expected).toDouble(), game.state().space().toDouble(), 1e-9);
            assertEquals(1, game.pulseCharges());
            assertEquals(1, game.pulses());
            assertTrue(game.pulse());
            assertFalse(game.pulse());                           // plus de charge
            assertEquals(2, game.pulses());
        }

        @Test
        void unJoueurQuiCliqueSansArretVaJusquAQuatreFoisPlusVite() {
            Game idle = afterBigBang();
            Game active = afterBigBang();
            for (int i = 0; i < 600; i++) {                      // dix minutes, une seconde à la fois
                idle.tick(1);
                active.tick(1);
                while (active.pulse()) { /* chaque charge dès qu'elle revient */ }
            }
            double ratio = active.state().space().toDouble() / idle.state().space().toDouble();
            assertTrue(ratio > 4 && ratio < 4.5, "rapport " + ratio);
        }
    }

    @Nested
    class AchatSimple {

        private Game afterBigBangs(int bigBangs) {
            Game game = newGame();
            game.state().setBigBangs(bigBangs);
            game.state().setSpace(BigNum.of(1, 6));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            return game;
        }

        @Test
        void auHasardDansUnRayonCreeUneMoleculeDeCeRayon() {
            Game game = afterBigBangs(1);
            assertTrue(game.canCreateInKind(Molecule.Kind.SIMPLE));
            Molecule made = game.createRandomMolecule(Molecule.Kind.SIMPLE);
            assertTrue(made != null && made.kind() == Molecule.Kind.SIMPLE);
            assertEquals(1, game.moleculesCreated());
        }

        @Test
        void auHasardPrendDAbordCeQueDemandeLeProchainPas() {
            Game game = afterBigBangs(1);
            for (String id : new String[] {"space_states", "space_assemblies"}) game.state().addSpaceUpgrade(id);
            java.util.Map<Molecule.Kind, Integer> missing = game.missingByKind();
            assertFalse(missing.isEmpty());
            Molecule.Kind kind = missing.keySet().iterator().next();
            for (int i = 0; i < 5; i++) {
                ShoppingList before = game.shoppingList();
                Molecule made = game.createRandomMolecule(kind);
                if (made == null) break;
                assertTrue(before.wants(made) || before.items().stream().noneMatch(item -> item.molecule().kind() == kind && item.missing() > 0),
                        made.name() + " n'était pas demandée alors que le rayon avait des sortes utiles");
            }
        }

        @Test
        void unRayonFermeOuSansRienDeCreableNeDonneRien() {
            Game game = afterBigBangs(1);
            assertFalse(game.canCreateInKind(Molecule.Kind.RARE));
            assertEquals(null, game.createRandomMolecule(Molecule.Kind.RARE));
        }

        @Test
        void laCreationAutomatiqueSuitLaListeDeCoursesSansRienAvoirAConfier() {
            Game game = afterBigBangs(2);
            for (String id : new String[] {"space_states", "space_assemblies"}) game.state().addSpaceUpgrade(id);
            assertTrue(game.isAutoFollowingList());
            assertEquals(0, game.automatedMolecules());
            assertTrue(game.isAutoCreatingMolecules());
            ShoppingList first = game.shoppingList();
            ShoppingList.Item wanted = first.items().stream().filter(item -> item.missing() > 0).findFirst().orElseThrow();
            game.tick(game.moleculeAutomationInterval() + 0.01);              // un passage
            assertTrue(game.stats().autoMoleculeCreations() > 0, "rien n'a été créé");
            assertTrue(game.moleculeCount(wanted.molecule().id()) > wanted.owned(), wanted.molecule().name() + " était demandée");
            assertTrue(game.setAutoFollowList(false));
            assertFalse(game.isAutoCreatingMolecules());                // sans amas confié, elle attend
        }
    }

    @Nested
    class ListeDeCourses {

        @Test
        void unAstreFormeChangeLaListeMemeSansToucherAuxMolecules() {
            Game game = newGame();
            game.state().setBigBangs(1);
            for (String id : new String[] {"space_states", "space_assemblies", "space_bodies"}) game.state().addSpaceUpgrade(id);
            ShoppingList first = game.shoppingList();
            assertEquals(ShoppingList.Kind.BODY, first.kind());
            Body target = game.nextBody();
            // L'astre visé est formé (ici sans passer par ses molécules) : la liste doit viser le suivant.
            game.state().addBody(target.id());
            ShoppingList next = game.shoppingList();
            assertNotEquals(first.target(), next.target(), "la liste visait encore un astre déjà formé");
        }

        @Test
        void unAssemblageFormeChangeAussiLaListe() {
            Game game = newGame();
            game.state().setBigBangs(1);
            for (String id : new String[] {"space_states", "space_assemblies"}) game.state().addSpaceUpgrade(id);
            ShoppingList first = game.shoppingList();
            assertEquals(ShoppingList.Kind.ASSEMBLY, first.kind());
            Assembly target = game.assemblies().stream().filter(a -> a.name().equals(first.target())).findFirst().orElseThrow();
            game.state().addAssembly(target.id());
            assertNotEquals(first.target(), game.shoppingList().target());
        }
    }
}
