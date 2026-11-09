package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La création automatique de la version 0.178 : plus de réglage à part pour la liste de courses ni pour la matière.
 * Dès qu'elle est acquise et en marche, elle suit le prochain pas : les sortes qui manquent à la liste de courses,
 * la matière et la variété qui manquent par état ({@link Game#autoMatterTargets()}, {@link Game#autoSortTargets()}),
 * puis les amas confiés, servis à tour de rôle.
 */
class Automatisme178Test {

    /** Après huit Big Bangs, tout ouvert, beaucoup d'espace et deux cents exemplaires de chaque élément. */
    private static Game prepared() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 8);
        game.state().setSpace(BigNum.of(1, 14));
        GameTestSupport.openSpace(game);
        GameTestSupport.fillTable(game, 200);
        assertTrue(game.isMoleculeAutomationUnlocked());
        assertTrue(game.isStatesUnlocked());
        return game;
    }

    /** Le même, le cosmos entier formé : la liste de courses est vide, seuls les amas confiés ont à être servis. */
    private static Game settled() {
        Game game = prepared();
        for (Body body : game.bodies()) game.state().addBody(body.id());
        Systemes.trois(game);
        game.state().setCosmosLevel(Cosmos.values().length);
        assertEquals(ShoppingList.NONE, game.shoppingList());
        return game;
    }

    @Test
    void elleCreeLesSortesManquantesDeLaListeDeCoursesSansRienRegler() {
        Game game = prepared();
        assertEquals(0, game.automatedMolecules());
        assertTrue(game.isAutoCreatingMolecules(), "acquise et en marche : elle agit, sans amas confié ni réglage");
        ShoppingList list = game.shoppingList();
        assertEquals(ShoppingList.Kind.BODY, list.kind());
        List<ShoppingList.Item> wanted = list.items().stream().filter(item -> item.missing() > 0).toList();
        assertFalse(wanted.isEmpty(), "le premier astre demande des molécules");
        for (ShoppingList.Item item : wanted) assertEquals(0, game.moleculeCount(item.molecule().id()), item.molecule().id());
        game.tick(game.moleculeAutomationInterval() + 0.01);                 // un passage
        assertTrue(game.stats().autoMoleculeCreations() >= wanted.size(), "une création par sorte demandée : " + game.stats().autoMoleculeCreations());
        for (ShoppingList.Item item : wanted) {
            assertTrue(game.moleculeCount(item.molecule().id()) > 0, item.molecule().name() + " était demandée");
        }
        // Coupée, elle s'arrête ; en pause aussi ; remise, elle reprend.
        long created = game.stats().autoMoleculeCreations();
        assertTrue(game.setMoleculeAutomationEnabled(false));
        assertFalse(game.isAutoCreatingMolecules());
        game.tick(5 * game.moleculeAutomationInterval());
        assertEquals(created, game.stats().autoMoleculeCreations());
        assertTrue(game.setMoleculeAutomationEnabled(true));
        game.setAutomationPaused(true);
        assertFalse(game.isAutoCreatingMolecules());
        game.tick(5 * game.moleculeAutomationInterval());
        assertEquals(created, game.stats().autoMoleculeCreations());
        game.setAutomationPaused(false);
        game.tick(game.moleculeAutomationInterval() + 0.01);
        assertTrue(game.stats().autoMoleculeCreations() > created);
    }

    @Test
    void laVarieteDuCosmosFaitCreerUneSorteNonRassembleeParEtat() {
        Game game = prepared();
        for (Body body : game.bodies()) game.state().addBody(body.id());
        Systemes.trois(game);
        game.state().setCosmosLevel(1);                                       // la galaxie : l'amas de galaxies est le prochain pas
        ShoppingList list = game.shoppingList();
        assertEquals(ShoppingList.Kind.COSMOS, list.kind());
        assertEquals(Cosmos.CLUSTER.sorts(), list.sorts(), "aucune sorte rassemblée : tout manque");
        // Sans rien de rassemblé, pas de matière à grossir ; mais une sorte à créer par état.
        assertTrue(game.autoMatterTargets().isEmpty());
        List<String> targets = game.autoSortTargets();
        assertEquals(Molecule.State.values().length, targets.size(), targets.toString());
        for (String id : targets) {
            Molecule molecule = game.molecule(id);
            assertTrue(list.sorts().containsKey(molecule.state()), id + " : son état ne manque pas de sortes");
            assertFalse(game.hasSubstance(id), id + " est déjà rassemblée");
            assertTrue(game.isMoleculeKindUnlocked(molecule.kind()) && game.isMoleculeWithinReach(id), id);
        }
        // La moins encombrante de chaque état : le dihydrogène, l'eau, l'hydrure de lithium.
        assertEquals(List.of("H2", "H2O", "LiH"), targets);
        // Une fois le dihydrogène rassemblé, il n'est plus proposé : une autre sorte de gaz prend sa place.
        game.state().setMoleculeCount("H2", Game.SUBSTANCE_MOLECULES);
        game.state().addSubstance("H2");
        targets = game.autoSortTargets();
        assertEquals(3, targets.size());
        assertFalse(targets.contains("H2"));
        for (String id : targets) assertFalse(game.hasSubstance(id), id + " est déjà rassemblée");
        Molecule gas = game.molecule(targets.get(0));
        assertEquals(Molecule.State.GAS, gas.state());
        // Et une sorte qui a déjà ses trois molécules attend d'être rassemblée : la suivante du même état la remplace.
        game.state().setMoleculeCount(gas.id(), Game.SUBSTANCE_MOLECULES);
        assertFalse(game.autoSortTargets().contains(gas.id()));
        assertEquals(3, game.autoSortTargets().size());
        assertEquals(Molecule.State.GAS, game.molecule(game.autoSortTargets().get(0)).state());
        game.state().setMoleculeCount(gas.id(), 0);
        // Un passage : chaque sorte proposée reçoit une création, sans qu'on ait rien confié.
        game.state().setAutoGather(false);
        assertEquals(0, game.automatedMolecules());
        targets = game.autoSortTargets();
        game.tick(game.moleculeAutomationInterval() + 0.01);
        for (String id : targets) assertTrue(game.moleculeCount(id) > 0, id + " devait être créée");
        // Le dihydrogène, rassemblé, est la matière du gaz : il grossit aussi, de lui-même.
        assertTrue(game.autoMatterTargets().contains("H2"));
        assertTrue(game.moleculeCount("H2") > Game.SUBSTANCE_MOLECULES, "la matière du gaz s'entretient : " + game.moleculeCount("H2"));
    }

    @Test
    void lesSortesDejaRassembleesNeSontJamaisProposeesCommeVariete() {
        Game game = prepared();
        for (Body body : game.bodies()) game.state().addBody(body.id());
        Systemes.trois(game);
        game.state().setCosmosLevel(1);
        // Toutes les sortes d'un état rassemblées : plus rien à proposer pour lui, même si le cosmos en demande plus.
        for (Molecule molecule : game.molecules()) {
            if (molecule.state() == Molecule.State.LIQUID) {
                game.state().setMoleculeCount(molecule.id(), Game.SUBSTANCE_MOLECULES);
                game.state().addSubstance(molecule.id());
            }
        }
        assertFalse(game.shoppingList().sorts().containsKey(Molecule.State.LIQUID), "quinze liquides rassemblés : la variété du liquide y est");
        for (String id : game.autoSortTargets()) {
            assertFalse(game.hasSubstance(id), id);
            assertTrue(game.molecule(id).state() != Molecule.State.LIQUID, id);
        }
        // Un seul liquide rassemblé, un seul gaz : la proposition de chaque état est une autre sorte.
        Game other = prepared();
        for (Body body : other.bodies()) other.state().addBody(body.id());
        Systemes.trois(other);
        other.state().setCosmosLevel(1);
        for (String id : List.of("H2O", "H2", "LiH", "NaCl")) {
            other.state().setMoleculeCount(id, 10);
            other.state().addSubstance(id);
        }
        Map<Molecule.State, Integer> sorts = other.shoppingList().sorts();
        assertEquals(Cosmos.CLUSTER.sorts().get(Molecule.State.GAS) - 1, sorts.get(Molecule.State.GAS));
        assertEquals(Cosmos.CLUSTER.sorts().get(Molecule.State.CRYSTAL) - 2, sorts.get(Molecule.State.CRYSTAL));
        List<String> targets = other.autoSortTargets();
        assertEquals(3, targets.size());
        for (String id : List.of("H2O", "H2", "LiH", "NaCl")) assertFalse(targets.contains(id), id + " est rassemblée");
        for (String id : targets) assertFalse(other.hasSubstance(id), id);
    }

    @Test
    void lesAmasConfiesSontServisATourDeRole() {
        Game game = settled();
        for (String id : List.of("NaCl", "KCl", "MgO")) {
            game.state().setMoleculeCount(id, 3);
            assertTrue(game.formSubstance(id), id);
            assertTrue(game.setMoleculeAutomated(id, true), id);
        }
        assertEquals(3, game.automatedMolecules());
        assertTrue(game.isAutoCreatingMolecules());
        assertTrue(game.autoMatterTargets().isEmpty() && game.autoSortTargets().isEmpty(), "sans liste de courses, rien d'autre à servir");
        // Beaucoup de place : chaque passage sert les trois amas.
        game.tick(game.moleculeAutomationInterval() + 0.01);
        for (String id : List.of("NaCl", "KCl", "MgO")) assertTrue(game.moleculeCount(id) > 3, id + " n'a pas été servi");
        // La place d'une seule molécule de plus à chaque passage : le premier servi tourne, aucun n'est oublié.
        int[] before = new int[3];
        List<String> ids = List.of("NaCl", "KCl", "MgO");
        for (int i = 0; i < 3; i++) before[i] = game.moleculeCount(ids.get(i));
        double room = 0;
        for (String id : ids) room = Math.max(room, game.moleculeVolume(id).toDouble() * game.moleculesNextCreation(id));
        for (int round = 0; round < 9; round++) {
            game.state().setSpace(BigNum.of(game.usedSpace().toDouble() + room));
            game.tick(game.moleculeAutomationInterval() + 0.01);
        }
        for (int i = 0; i < 3; i++) assertTrue(game.moleculeCount(ids.get(i)) > before[i], ids.get(i) + " a été oublié");
        // Un amas retiré n'est plus servi ; les autres le sont toujours.
        assertTrue(game.setMoleculeAutomated("MgO", false));
        game.state().setSpace(BigNum.of(1, 14));
        int oxide = game.moleculeCount("MgO");
        int salt = game.moleculeCount("NaCl");
        game.tick(game.moleculeAutomationInterval() + 0.01);
        assertEquals(oxide, game.moleculeCount("MgO"));
        assertTrue(game.moleculeCount("NaCl") > salt);
    }
}
