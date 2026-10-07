package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le confort de jeu : ce qui épargne des clics sans rien changer aux règles. Les améliorations
 * d'espace acquises d'elles-mêmes, l'achat groupé en atomes, la création continue, les gestes
 * groupés, la coupure générale des automatismes, les réglages qui traversent les remises à zéro,
 * les molécules favorites et la liste de courses.
 */
class ConfortTest {

    private static Game newGame() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    /** Après un premier Big Bang, sans aucune amélioration d'espace et sans espace. */
    private static Game afterBigBang() {
        Game game = newGame();
        game.state().setBigBangs(1);
        return game;
    }

    /** Après un premier Big Bang, tout ouvert jusqu'aux astres, beaucoup d'espace. */
    private static Game openGame() {
        Game game = afterBigBang();
        game.state().setSpace(BigNum.of(1, 12));
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
        }
        return game;
    }

    private static void give(Game game, String id, int count) {
        game.state().setMoleculeCount(id, count);
        game.state().addSubstance(id);
    }

    @Nested
    class AmeliorationsDEspacePrisesSeules {

        @Test
        void leSeuilAtteintLAmeliorationEstAcquiseSansClic() {
            Game game = afterBigBang();
            game.tick(299);
            assertFalse(game.ownsSpaceUpgrade("space_primordial_atoms"));
            assertEquals(List.of(), game.takeNewSpaceUpgrades());
            game.tick(2);
            assertTrue(game.ownsSpaceUpgrade("space_primordial_atoms"));
            List<SpaceUpgrade> taken = game.takeNewSpaceUpgrades();
            assertEquals(1, taken.size());
            assertEquals("space_primordial_atoms", taken.get(0).id());
            // Annoncée une seule fois.
            assertEquals(List.of(), game.takeNewSpaceUpgrades());
        }

        @Test
        void uneAmeliorationPeutEnOuvrirUneAutreDejaAPortee() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(50_000));
            game.tick(0.1);
            // Les états à 3 000, puis les assemblages à 40 000 qui en dépendent : les deux d'un coup.
            assertTrue(game.ownsSpaceUpgrade("space_states"));
            assertTrue(game.ownsSpaceUpgrade("space_assemblies"));
            assertFalse(game.ownsSpaceUpgrade("space_bodies"));
            assertTrue(game.isStatesUnlocked() && game.isAssembliesUnlocked());
        }

        @Test
        void toutesFinissentParEtreAcquises() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(1, 12));
            game.tick(0.1);
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) assertTrue(game.ownsSpaceUpgrade(upgrade.id()), upgrade.id());
            assertEquals(game.spaceUpgrades().size(), game.takeNewSpaceUpgrades().size());
            game.tick(10);
            assertEquals(List.of(), game.takeNewSpaceUpgrades());
        }

        @Test
        void rienAvantLePremierBigBang() {
            Game game = newGame();
            game.state().setSpace(BigNum.of(1, 12));
            game.tick(10);
            assertEquals(0, game.state().spaceUpgradeCount());
        }

        @Test
        void leChargementDUnePartieNAnnonceRien() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(5_000));
            game.tick(0.1);
            game.restored();
            assertEquals(List.of(), game.takeNewSpaceUpgrades());
        }
    }

    @Nested
    class AchatGroupeEnAtomes {

        @Test
        void ilSOuvreAvecUneMatiereNoire() {
            Game game = newGame();
            assertFalse(game.isBulkAtomBuyUnlocked());
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.ONE);
            assertTrue(game.canBuyDark("dark_shop_bulk"));
            assertTrue(game.buyDark("dark_shop_bulk"));
            assertTrue(game.isBulkAtomBuyUnlocked());
            assertTrue(game.isDarkMaxed("dark_shop_bulk"));
            assertTrue(game.state().darkMatter().isZero());
        }

        @Test
        void plusieursNiveauxEnAtomesSAchetentDUnCoup() {
            Game game = newGame();
            game.state().setTotalAtoms(BigNum.of(100));
            game.state().setAtoms(BigNum.of(100));
            Upgrade upgrade = game.upgrades(Resource.ATOMS).stream().filter(each -> !each.hasLimit() || each.maxLevel() > 3).findFirst().orElseThrow();
            int affordable = game.affordableLevels(upgrade.id(), Integer.MAX_VALUE);
            assertTrue(affordable >= 2, upgrade.id() + " : " + affordable + " niveaux à portée");
            BigNum cost = game.costOf(upgrade.id(), affordable);
            assertEquals(affordable, game.buy(upgrade.id(), Integer.MAX_VALUE));
            assertEquals(affordable, game.levelOf(upgrade.id()));
            assertEquals(100 - cost.toDouble(), game.state().atoms().toDouble(), 1e-6);
        }
    }

    @Nested
    class CreationContinueEtGestesGroupes {

        @Test
        void ilsViennentAvecLEspace() {
            Game game = afterBigBang();
            assertFalse(game.isHoldCreateUnlocked());
            assertFalse(game.isBulkFormUnlocked());
            game.state().setSpace(BigNum.of(800));
            game.tick(0.1);
            assertTrue(game.isHoldCreateUnlocked());
            assertFalse(game.isBulkFormUnlocked());
            game.state().setSpace(BigNum.of(15_000));
            game.tick(0.1);
            assertTrue(game.isBulkFormUnlocked());
        }

        @Test
        void sansLAmeliorationLesGestesGroupesNeFontRien() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(1, 9));
            game.state().addSpaceUpgrade("space_states");
            game.state().setMoleculeCount("H2O", 5);
            assertEquals(1, game.substancesReady());
            assertEquals(0, game.formAllSubstances());
            assertFalse(game.hasSubstance("H2O"));
        }

        @Test
        void toutRassemblerPrendToutesLesSortesPretes() {
            Game game = openGame();
            game.state().setMoleculeCount("H2O", 5);
            game.state().setMoleculeCount("H2", 3);
            game.state().setMoleculeCount("CO2", 2);       // pas assez : il en faut trois
            assertEquals(2, game.substancesReady());
            assertEquals(2, game.formAllSubstances());
            assertTrue(game.hasSubstance("H2O") && game.hasSubstance("H2"));
            assertFalse(game.hasSubstance("CO2"));
            assertEquals(0, game.substancesReady());
            assertEquals(0, game.formAllSubstances());
        }

        @Test
        void toutRassemblerSArreteQuandLEspaceManque() {
            Game game = openGame();
            game.state().setMoleculeCount("H2O", 3);
            game.state().setMoleculeCount("H2", 3);
            // De la place pour un seul des deux lieux : celui du premier du catalogue.
            BigNum used = game.usedSpace();
            BigNum first = game.substanceSpace("H2");
            game.state().setSpace(used.add(first).multiply(1.0001));
            assertEquals(1, game.formAllSubstances());
            assertEquals(1, game.substancesFormed());
        }

        @Test
        void toutAssemblerFormeCeQuiEstPret() {
            Game game = openGame();
            Assembly first = game.assemblies().get(0);
            Assembly second = game.assemblies().get(1);
            for (Map.Entry<String, Integer> ingredient : first.ingredients().entrySet()) give(game, ingredient.getKey(), ingredient.getValue());
            assertEquals(1, game.assembliesReady());
            assertEquals(1, game.formAllAssemblies());
            assertTrue(game.hasAssembly(first.id()));
            assertFalse(game.hasAssembly(second.id()));
            assertEquals(0, game.formAllAssemblies());
        }

        @Test
        void formerLesAstresEnchaineCeuxQuUnAstreRendPossibles() {
            Game game = openGame();
            // Tout ce que demandent les trois premiers astres, sauf les astres eux-mêmes.
            List<Body> bodies = game.bodies();
            int supplied = 0;
            for (Body body : bodies) {
                if (supplied == 3) break;
                for (String assembly : body.assemblies()) game.state().addAssembly(assembly);
                for (Map.Entry<String, Integer> molecule : body.molecules().entrySet()) {
                    game.state().setMoleculeCount(molecule.getKey(), Math.max(game.moleculeCount(molecule.getKey()), molecule.getValue()));
                }
                for (Map.Entry<Molecule.State, Integer> matter : body.matter().entrySet()) {
                    Molecule molecule = game.molecules().stream().filter(each -> each.state() == matter.getKey()).findFirst().orElseThrow();
                    game.state().addSubstance(molecule.id());
                    int missing = matter.getValue() - game.gatheredInState(matter.getKey());
                    if (missing > 0) game.state().setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + missing);
                }
                supplied++;
            }
            assertTrue(game.bodiesReady() >= 1);
            int formed = game.formAllBodies();
            assertEquals(3, formed);
            assertEquals(3, game.bodiesFormed());
            assertEquals(0, game.bodiesReady());
        }
    }

    @Nested
    class CoupureDesAutomatismes {

        private Game automated() {
            Game game = newGame();
            game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
            game.state().addAutomation("auto_speed");
            game.state().setAutomationEnabled("auto_speed", true);
            game.state().setParticles(BigNum.of(1, 6));
            return game;
        }

        @Test
        void coupesAucunNAgitEtChacunGardeSonReglage() {
            Game game = automated();
            assertTrue(game.hasAnyAutomation());
            assertFalse(game.isAutomationPaused());
            game.setAutomationPaused(true);
            game.tick(30);
            assertEquals(0, game.levelOf("speed"));
            assertEquals(0, game.stats().automationActions());
            assertTrue(game.isAutomationEnabled("auto_speed"), "le réglage de l'automatisme n'a pas bougé");
            game.setAutomationPaused(false);
            game.tick(30);
            assertTrue(game.levelOf("speed") > 0);
        }

        @Test
        void laCoupureArreteLAppuiAutomatiqueMaisPasLeVerrou() {
            Game game = openGame();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(5));
            game.state().setDarkLevel("dark_lock", 1);
            assertTrue(game.isAutoHolding());
            game.setAutomationPaused(true);
            assertFalse(game.isAutoHolding());
            BigNum size = game.state().darkMatterSize();
            game.tick(60);
            assertEquals(size, game.state().darkMatterSize());
            // Le verrou est un geste du joueur, pas un automatisme : il continue.
            game.setHoldLocked(true);
            game.tick(60);
            assertTrue(game.state().darkMatterSize().gt(size));
        }

        @Test
        void laCoupureArreteLaCreationAutomatique() {
            Game game = openGame();
            game.state().setBigBangs(2);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            give(game, "H2O", 3);
            assertTrue(game.setMoleculeAutomated("H2O", true));
            game.setAutomationPaused(true);
            assertFalse(game.isAutoCreatingMolecules());
            game.tick(60);
            assertEquals(3, game.moleculeCount("H2O"));
            game.setAutomationPaused(false);
            game.tick(60);
            assertTrue(game.moleculeCount("H2O") > 3);
        }

        @Test
        void niLExplosionNiLeBigBangNeLeventLaCoupure() {
            Game game = automated();
            game.setAutomationPaused(true);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.isAutomationPaused());
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertTrue(game.isAutomationPaused());
            game.reset();
            assertFalse(game.isAutomationPaused());
        }

        @Test
        void sansAutomatismeIlNYARienACouper() {
            assertFalse(newGame().hasAnyAutomation());
        }
    }

    @Nested
    class ReglagesRetenus {

        @Test
        void lesAutomatismesDeMatiereNoireReprennentQuandLaMatiereNoireRevient() {
            Game game = newGame();
            DarkAutomation automation = game.darkAutomations().iterator().next();
            game.state().setExplosions(5);
            game.state().setDarkMatter(automation.darkMatter());
            assertTrue(game.setDarkAutomationEnabled(automation.id(), true));
            assertTrue(game.isDarkAutomationEnabled(automation.id()));
            // Un Big Bang reprend la matière noire : l'automatisme est de nouveau verrouillé, mais son réglage reste.
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertFalse(game.isDarkAutomationUnlocked(automation.id()));
            assertFalse(game.isDarkAutomationEnabled(automation.id()));
            game.state().setExplosions(1);
            game.state().setDarkMatter(automation.darkMatter());
            assertTrue(game.isDarkAutomationEnabled(automation.id()), "il repart sans qu'on y touche");
        }

        @Test
        void leSeuilDeFusionEtLeVerrouResserventUneFoisRouverts() {
            Game game = newGame();
            game.state().setExplosions(3);
            game.state().setDarkLevel("dark_swarm", 1);
            game.state().setDarkLevel("dark_threshold", 1);
            game.state().setDarkLevel("dark_lock", 1);
            assertTrue(game.setFusionThreshold(40));
            assertTrue(game.setHoldLocked(true));
            game.setSynthesisReserve(BigNum.of(1, 6));
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertFalse(game.isHoldLocked());
            assertEquals(game.generatorsPerAtom(), game.fusionThreshold());
            // La réserve de synthèse, elle, repart de zéro : sous le plafond d'atomes revenu, elle bloquerait la synthèse.
            assertTrue(game.synthesisReserve().isZero());
            game.state().setExplosions(1);
            game.state().setDarkLevel("dark_swarm", 1);
            game.state().setDarkLevel("dark_threshold", 1);
            game.state().setDarkLevel("dark_lock", 1);
            assertTrue(game.isHoldLocked());
            assertEquals(40, game.fusionThreshold());
        }

        @Test
        void recommencerDeZeroOublieCesReglages() {
            Game game = newGame();
            game.state().setHoldLocked(true);
            game.state().setFusionThreshold(40);
            game.state().setDarkAutomationEnabled("dark_auto_explosion", true);
            game.reset();
            assertFalse(game.state().holdLocked());
            assertEquals(0, game.state().fusionThreshold());
            assertFalse(game.state().isDarkAutomationEnabled("dark_auto_explosion"));
        }
    }

    @Nested
    class MoleculesFavorites {

        @Test
        void uneMoleculeSEpingleEtSeRetire() {
            Game game = afterBigBang();
            assertFalse(game.isMoleculeFavorite("H2O"));
            game.setMoleculeFavorite("H2O", true);
            game.setMoleculeFavorite("CO2", true);
            game.setMoleculeFavorite("H2O", true);
            assertTrue(game.isMoleculeFavorite("H2O"));
            assertEquals(List.of("H2O", "CO2"), game.favoriteMolecules().stream().map(Molecule::id).toList());
            game.setMoleculeFavorite("H2O", false);
            assertEquals(List.of("CO2"), game.favoriteMolecules().stream().map(Molecule::id).toList());
        }

        @Test
        void uneMoleculeInconnueEstRefusee() {
            Game game = afterBigBang();
            assertThrows(IllegalArgumentException.class, () -> game.setMoleculeFavorite("XxYy", true));
        }

        @Test
        void lesFavoritesTraversentLesRemisesAZeroSaufLaDerniere() {
            Game game = afterBigBang();
            game.setMoleculeFavorite("H2O", true);
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertTrue(game.isMoleculeFavorite("H2O"));
            game.reset();
            assertFalse(game.state().isMoleculeFavorite("H2O"));
        }
    }

    @Nested
    class ListeDeCourses {

        @Test
        void rienAViserAvantLeBigBang() {
            assertEquals(ShoppingList.Kind.NONE, newGame().shoppingList().kind());
            assertTrue(ShoppingList.NONE.items().isEmpty());
        }

        @Test
        void avantLesAssemblagesIlNYARienAViser() {
            Game game = afterBigBang();
            game.state().addSpaceUpgrade("space_states");
            assertEquals(ShoppingList.Kind.NONE, game.shoppingList().kind());
        }

        @Test
        void avantLesAstresElleViseLAssemblageLePlusAvance() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(1, 12));
            game.state().addSpaceUpgrade("space_states");
            game.state().addSpaceUpgrade("space_assemblies");
            Assembly granite = game.assembly("granitic_rock");
            // Rien d'entamé : le premier du catalogue.
            assertEquals(game.assemblies().get(0).name(), game.shoppingList().target());
            // Un ingrédient du granite complet, un autre entamé mais pas rassemblé, le dernier absent.
            give(game, "SiO2", 120);
            game.state().setMoleculeCount("KAlSi3O8", 40);
            ShoppingList list = game.shoppingList();
            assertEquals(ShoppingList.Kind.ASSEMBLY, list.kind());
            assertEquals(granite.name(), list.target());
            assertFalse(list.ready());
            assertEquals(2, list.items().size());
            ShoppingList.Item feldspar = list.items().get(0);
            assertEquals("KAlSi3O8", feldspar.molecule().id());
            assertEquals(40, feldspar.owned());
            assertEquals(90, feldspar.needed());
            assertEquals(50, feldspar.missing());
            assertTrue(feldspar.gather());
            assertEquals(granite.name(), feldspar.purpose());
            ShoppingList.Item albite = list.items().get(1);
            assertEquals(60, albite.missing());
            assertTrue(list.wants(game.molecule("NaAlSi3O8")));
            assertFalse(list.wants(game.molecule("SiO2")), "l'ingrédient complet et rassemblé n'est plus à chercher");
            assertFalse(list.wants(game.molecule("H2O")));
            // Tout réuni : il n'y a plus qu'à assembler.
            give(game, "KAlSi3O8", 90);
            give(game, "NaAlSi3O8", 60);
            list = game.shoppingList();
            assertTrue(list.ready());
            assertTrue(list.items().isEmpty());
            assertEquals(List.of(granite), list.assemblies());
        }

        @Test
        void unIngredientCompletMaisPasRassembleResteSurLaListe() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(1, 12));
            game.state().addSpaceUpgrade("space_states");
            game.state().addSpaceUpgrade("space_assemblies");
            give(game, "SiO2", 120);
            give(game, "KAlSi3O8", 90);
            game.state().setMoleculeCount("NaAlSi3O8", 60);
            ShoppingList list = game.shoppingList();
            assertEquals(1, list.items().size());
            assertEquals(0, list.items().get(0).missing());
            assertTrue(list.items().get(0).gather());
        }

        @Test
        void lesMoleculesPrisesParUnAutreAssemblageNeComptentPas() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(1, 12));
            game.state().addSpaceUpgrade("space_states");
            game.state().addSpaceUpgrade("space_assemblies");
            // Un autre assemblage formé a déjà pris de la silice : il en faut d'autant plus pour le granite.
            Assembly other = game.assemblies().stream()
                    .filter(each -> !each.id().equals("granitic_rock") && each.ingredients().containsKey("SiO2")).findFirst().orElseThrow();
            int taken = other.ingredients().get("SiO2");
            game.state().addAssembly(other.id());
            give(game, "SiO2", taken + 20);
            game.state().setMoleculeCount("KAlSi3O8", 1);
            ShoppingList list = game.shoppingList();
            assertEquals("Roche granitique", list.target());
            ShoppingList.Item silica = list.items().stream().filter(item -> item.molecule().id().equals("SiO2")).findFirst().orElseThrow();
            assertEquals(taken + 120, silica.needed());
            assertEquals(100, silica.missing());
            assertFalse(silica.gather());
        }

        @Test
        void avecLesAstresElleViseLAstreAPorteeLePlusAvance() {
            Game game = openGame();
            Body first = game.bodies().get(0);
            ShoppingList list = game.shoppingList();
            assertEquals(ShoppingList.Kind.BODY, list.kind());
            assertFalse(list.ready());
            // Les ingrédients de tous ses assemblages, et la matière qu'il demande.
            int ingredients = 0;
            for (String assembly : first.assemblies()) ingredients += game.assembly(assembly).ingredients().size();
            if (list.target().equals(first.name())) {
                assertTrue(list.items().size() >= Math.min(1, ingredients));
                for (Molecule.State state : first.matter().keySet()) assertTrue(list.matter().containsKey(state), state.name());
            }
            // Un astre dont les astres plus petits ne sont pas formés n'est jamais visé.
            Body target = game.bodies().stream().filter(body -> body.name().equals(list.target())).findFirst().orElseThrow();
            for (String smaller : target.bodies()) assertTrue(game.hasBody(smaller));
        }

        @Test
        void unAstreDontToutEstReuniEstPret() {
            Game game = openGame();
            Body first = game.bodies().get(0);
            for (String assembly : first.assemblies()) game.state().addAssembly(assembly);
            for (Map.Entry<String, Integer> molecule : first.molecules().entrySet()) game.state().setMoleculeCount(molecule.getKey(), molecule.getValue());
            for (Map.Entry<Molecule.State, Integer> matter : first.matter().entrySet()) {
                Molecule molecule = game.molecules().stream().filter(each -> each.state() == matter.getKey()).findFirst().orElseThrow();
                game.state().addSubstance(molecule.id());
                int missing = matter.getValue() - game.gatheredInState(matter.getKey());
                if (missing > 0) game.state().setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + missing);
            }
            assertTrue(game.canFormBody(first.id()));
            ShoppingList list = game.shoppingList();
            assertEquals(first.name(), list.target());
            assertTrue(list.ready());
            assertTrue(list.items().isEmpty() && list.matter().isEmpty());
            // Formé, la liste passe au suivant.
            assertTrue(game.formBody(first.id()));
            assertFalse(game.shoppingList().target().equals(first.name()));
        }

        @Test
        void laMatiereQuiManqueRendUtilesLesMoleculesDeCetEtat() {
            Game game = openGame();
            Body body = game.bodies().stream().filter(each -> each.bodies().isEmpty() && !each.matter().isEmpty()).findFirst().orElseThrow();
            // Tout sauf la matière : la liste ne demande plus qu'elle.
            for (Body other : game.bodies()) {
                if (other != body && other.bodies().isEmpty()) game.state().addBody(other.id());
            }
            for (String assembly : body.assemblies()) game.state().addAssembly(assembly);
            for (Map.Entry<String, Integer> molecule : body.molecules().entrySet()) game.state().setMoleculeCount(molecule.getKey(), molecule.getValue());
            ShoppingList list = game.shoppingList();
            if (list.target().equals(body.name())) {
                Molecule.State state = body.matter().keySet().iterator().next();
                int missing = body.matter().get(state) - game.gatheredInState(state);
                if (missing > 0) {
                    assertEquals(missing, list.matter().get(state));
                    Molecule any = game.molecules().stream().filter(each -> each.state() == state).findFirst().orElseThrow();
                    assertTrue(list.wants(any));
                }
            }
        }

        @Test
        void tousLesAstresFormesElleViseLaProchaineEchelle() {
            Game game = openGame();
            for (Body body : game.bodies()) game.state().addBody(body.id());
            ShoppingList list = game.shoppingList();
            assertEquals(ShoppingList.Kind.COSMOS, list.kind());
            assertEquals(Cosmos.GALAXY.phrase(), list.target());
            assertTrue(list.ready(), "tous les astres sont là : la galaxie n'attend plus qu'un clic");
            assertTrue(game.formGalaxy());
            list = game.shoppingList();
            assertEquals(Cosmos.CLUSTER.phrase(), list.target());
            assertFalse(list.ready());
            assertEquals(Cosmos.CLUSTER.matter().size(), list.matter().size());
            assertEquals(Cosmos.CLUSTER.matter().get(Molecule.State.GAS), list.matter().get(Molecule.State.GAS));
            assertTrue(list.wants(game.molecule("H2")));
            // Un gaz rassemblé en quantité : il manque d'autant moins.
            give(game, "H2", 1_000_000);
            assertEquals(Cosmos.CLUSTER.matter().get(Molecule.State.GAS) - 1_000_000, game.shoppingList().matter().get(Molecule.State.GAS));
        }

        @Test
        void lUniversFormeIlNYAPlusRienAViser() {
            Game game = openGame();
            for (Body body : game.bodies()) game.state().addBody(body.id());
            game.state().setCosmosLevel(3);
            assertEquals(ShoppingList.Kind.NONE, game.shoppingList().kind());
        }

        @Test
        void laListeSuitLaPartie() {
            Game game = afterBigBang();
            game.state().setSpace(BigNum.of(1, 12));
            game.state().addSpaceUpgrade("space_states");
            game.state().addSpaceUpgrade("space_assemblies");
            give(game, "SiO2", 10);
            int before = game.shoppingList().items().stream().filter(item -> item.molecule().id().equals("SiO2")).findFirst().orElseThrow().missing();
            game.state().setMoleculeCount("SiO2", 50);
            int after = game.shoppingList().items().stream().filter(item -> item.molecule().id().equals("SiO2")).findFirst().orElseThrow().missing();
            assertEquals(before - 40, after);
        }
    }
}
