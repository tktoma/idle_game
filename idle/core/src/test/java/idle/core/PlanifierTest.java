package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Version 0.85 : planifier plusieurs astres, plafonner la création par état, réserver par recette, cibler le prochain astre. */
class PlanifierTest {

    private static Game game() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        game.state().setBigBangs(1);
        game.state().setSpace(BigNum.of(1, 12));
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
        }
        return game;
    }

    /** Les astres du catalogue qui ont des assemblages, dans l'ordre. */
    private static List<Body> withAssemblies(Game game) {
        List<Body> list = new ArrayList<>();
        for (Body body : game.bodies()) if (!body.assemblies().isEmpty()) list.add(body);
        return list;
    }

    private static void formFive(Game game) {
        int formed = 0;
        for (Body body : game.bodies()) {
            if (formed == Game.BODY_PLANNING_BODIES) break;
            if (body.tier() == Body.Tier.RUBBLE || body.tier() == Body.Tier.COMET) {
                game.state().addBody(body.id());
                formed++;
            }
        }
    }

    @Test
    void laPlanificationSOuvreACinqAstresEtEnPrendTroisAuPlus() {
        Game game = game();
        List<Body> candidates = new ArrayList<>(game.bodies());
        assertFalse(game.isBodyPlanningUnlocked());
        assertFalse(game.togglePlannedBody(candidates.get(candidates.size() - 1).id()));
        formFive(game);
        assertTrue(game.isBodyPlanningUnlocked());
        List<Body> free = new ArrayList<>();
        for (Body body : game.bodies()) if (!game.state().hasBody(body.id())) free.add(body);
        for (int i = 0; i < Game.MAX_PLANNED_BODIES; i++) assertTrue(game.togglePlannedBody(free.get(i).id()));
        assertFalse(game.togglePlannedBody(free.get(3).id()), "trois au plus");
        assertTrue(game.togglePlannedBody(free.get(0).id()), "un second clic le retire");
        assertEquals(2, game.plannedBodies().size());
        assertFalse(game.togglePlannedBody(game.state().plannedBodies().isEmpty() ? "" : "rocky_rubble"), "un astre formé ne se planifie pas");
        // Un astre planifié qui se forme quitte le plan.
        game.state().addBody(free.get(1).id());
        assertEquals(List.of(free.get(2)), game.plannedBodies());
    }

    @Test
    void lesBesoinsSAjoutentPourLesAssemblagesEtSePartagentPourLesSeuils() {
        Game game = game();
        formFive(game);
        List<Body> list = withAssemblies(game);
        Body a = null, b = null;
        // Deux astres qui demandent la même matière : elle doit se compter une seule fois, au plus haut.
        for (Body first : list) {
            for (Body second : list) {
                if (first == second || game.state().hasBody(first.id()) || game.state().hasBody(second.id())) continue;
                for (Molecule.State matter : first.matter().keySet()) {
                    if (second.matter().containsKey(matter) && a == null) {
                        a = first;
                        b = second;
                    }
                }
            }
        }
        assertTrue(a != null, "deux astres du catalogue partagent un état de matière");
        assertTrue(game.togglePlannedBody(a.id()) && game.togglePlannedBody(b.id()));
        List<Game.PlanNeed> needs = game.plannedNeeds();
        for (Molecule.State matter : a.matter().keySet()) {
            if (!b.matter().containsKey(matter)) continue;
            Game.PlanNeed need = needs.stream().filter(n -> n.kind() == Game.PlanNeed.Kind.MATTER && n.id().equals(matter.name())).findFirst().orElseThrow();
            assertEquals(Math.max(a.matter().get(matter), b.matter().get(matter)), need.needed(), "un seuil : le plus haut des deux");
            assertTrue(need.shared(), "une exigence simultanée");
            assertFalse(need.satisfied());
        }
        // Les ingrédients des assemblages s'ajoutent.
        Map<String, Integer> sum = new java.util.HashMap<>();
        for (Body body : List.of(a, b)) {
            for (String id : body.assemblies()) {
                for (Map.Entry<String, Integer> ingredient : game.assembly(id).ingredients().entrySet()) sum.merge(ingredient.getKey(), ingredient.getValue(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> ingredient : sum.entrySet()) {
            Game.PlanNeed need = needs.stream().filter(n -> n.kind() == Game.PlanNeed.Kind.MOLECULE && n.id().equals(ingredient.getKey())).findFirst().orElseThrow();
            int threshold = Math.max(a.molecules().getOrDefault(ingredient.getKey(), 0), b.molecules().getOrDefault(ingredient.getKey(), 0));
            assertEquals(Math.max(threshold, Math.max(Game.SUBSTANCE_MOLECULES, ingredient.getValue())), need.needed(), ingredient.getKey());
            assertTrue(need.gather());
        }
        // Réunir une exigence la fait passer après les autres, marquée réunie.
        Game.PlanNeed first = needs.get(0);
        if (first.kind() == Game.PlanNeed.Kind.ASSEMBLY) game.state().addAssembly(first.id());
        else if (first.kind() == Game.PlanNeed.Kind.BODY) game.state().addBody(first.id());
        else if (first.kind() == Game.PlanNeed.Kind.MOLECULE) {
            game.state().setMoleculeCount(first.id(), (int) first.needed());
            game.state().addSubstance(first.id());
        }
        List<Game.PlanNeed> after = game.plannedNeeds();
        Game.PlanNeed moved = after.stream().filter(n -> n.kind() == first.kind() && n.id().equals(first.id())).findFirst().orElse(null);
        if (moved != null) {
            assertTrue(moved.satisfied());
            assertTrue(after.indexOf(moved) > after.indexOf(after.stream().filter(n -> !n.satisfied()).findFirst().orElseThrow()));
        }
    }

    @Test
    void lePlafondDUnEtatRetientLaCreationAutomatique() {
        Game game = game();
        game.state().setBigBangs(8);     // la création automatique vient d'un palier de Big Bang
        Molecule gas = game.molecules().stream().filter(m -> m.state() == Molecule.State.GAS && game.isMoleculeKindUnlocked(m.kind()))
                .findFirst().orElseThrow();
        assertTrue(game.isStateCapUnlocked(), "les plafonds s'ouvrent avec la création automatique et les états");
        for (int number = 1; number <= idle.core.elements.PeriodicTable.ELEMENTS.size(); number++) game.state().setElementCount(number, 200);
        assertEquals(1, game.stateCap(Molecule.State.GAS), 0);
        game.setStateCap(Molecule.State.GAS, 0.7);
        assertEquals(0.7, game.stateCap(Molecule.State.GAS), 1e-12);
        game.setStateCap(Molecule.State.GAS, 0.01);
        assertEquals(0.05, game.stateCap(Molecule.State.GAS), 1e-12, "cinq pour cent au moins");
        game.setStateCap(Molecule.State.GAS, 2);
        assertEquals(1, game.stateCap(Molecule.State.GAS), 0, "au-delà de 100 % : pas de plafond");
        // L'espace juste assez grand pour une création : avec un plafond de 5 %, elle ne se fait plus.
        BigNum volume = game.moleculeVolume(gas.id()).multiply(game.moleculesNextCreation(gas.id()));
        game.state().setSpace(volume.multiply(10));
        assertTrue(game.stateShare(Molecule.State.GAS) == 0, "pas encore de gaz");
        game.setStateCap(Molecule.State.GAS, 0.05);
        assertTrue(game.spaceInState(Molecule.State.GAS).isZero());
        game.state().setSpace(volume.multiply(10));
        // L'automatisme réglé sur cette seule sorte : il ne la crée pas au-delà du plafond.
        game.state().addSubstance(gas.id());
        game.state().setMoleculeAutomated(gas.id(), true);
        game.state().setAutoFollowList(false);
        assertTrue(game.isAutoCreatingMolecules(), "la création automatique tourne");
        {
            for (int i = 0; i < 20; i++) game.tick(1);
            assertTrue(game.stateShare(Molecule.State.GAS) <= 0.05 + 1e-9, "au plus 5 % : " + game.stateShare(Molecule.State.GAS));
            game.setStateCap(Molecule.State.GAS, 1);
            for (int i = 0; i < 20; i++) game.tick(1);
            assertTrue(game.stateShare(Molecule.State.GAS) > 0.05, "sans plafond, elle remplit plus : " + game.stateShare(Molecule.State.GAS));
        }
    }

    @Test
    void laReservationParRecetteDitCeQuIlManqueEtPrevientDeLaSaturation() {
        Game game = game();
        assertFalse(game.isRecipeReservationUnlocked());
        Body body = game.nextBody();
        Assembly next = game.nextAssembly();
        assertTrue(body != null && next != null);
        assertEquals(game.assembly(body.assemblies().get(0)), next, "le premier assemblage de l'astre visé");
        // Un assemblage formé ailleurs ouvre la réservation.
        Assembly elsewhere = null;
        for (Assembly assembly : game.assemblies()) if (!body.assemblies().contains(assembly.id()) && elsewhere == null) elsewhere = assembly;
        game.state().addAssembly(elsewhere.id());
        assertTrue(game.isRecipeReservationUnlocked());
        assertFalse(game.isRecipeReservationOn());
        game.setRecipeReservation(true);
        assertTrue(game.isRecipeReservationOn());
        Map.Entry<String, Integer> ingredient = next.ingredients().entrySet().iterator().next();
        int already = game.assembledMolecules(ingredient.getKey());
        assertEquals(already + ingredient.getValue(), game.recipeRemaining(ingredient.getKey()) + game.moleculeCount(ingredient.getKey()));
        String other = game.molecules().stream().map(Molecule::id).filter(id -> !next.ingredients().containsKey(id)).findFirst().orElseThrow();
        assertEquals(-1, game.recipeRemaining(other), "pas un ingrédient");
        game.state().setMoleculeCount(ingredient.getKey(), already + ingredient.getValue());
        assertEquals(0, game.recipeRemaining(ingredient.getKey()));
        // Assez pour la recette, et plus de place pour le reste : la création de plus est saturante.
        game.state().setSpace(game.usedSpace());
        assertTrue(game.isSaturating(ingredient.getKey()));
        game.state().setSpace(BigNum.of(1, 30));
        assertFalse(game.isSaturating(ingredient.getKey()), "la place ne manque pas");
        game.setRecipeReservation(false);
        game.state().setSpace(game.usedSpace());
        assertFalse(game.isSaturating(ingredient.getKey()), "éteinte, elle ne prévient pas");
    }

    @Test
    void leFiltreDuProchainAstreDonneLesElementsDesRecettesUtiles() {
        Game game = game();
        assertTrue(game.isNextBodyFilterUnlocked());
        java.util.Set<Integer> numbers = game.elementsForNextStep();
        assertFalse(numbers.isEmpty());
        for (var item : game.shoppingList().items()) {
            if (game.isMoleculeKindUnlocked(item.molecule().kind())) assertTrue(numbers.containsAll(item.molecule().recipe().keySet()), item.molecule().id());
        }
        Game before = new Game();
        before.start();
        assertFalse(before.isNextBodyFilterUnlocked());
        assertTrue(before.elementsForNextStep().isEmpty());
    }
}
