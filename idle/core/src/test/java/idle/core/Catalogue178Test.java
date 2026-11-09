package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import idle.core.probes.ProbeDestination;
import idle.core.sky.Assemblies;
import idle.core.sky.Assembly;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Le catalogue de la version 0.178, figé : 73 molécules en quatre rayons et trois états, toutes demandées par le ciel ;
 * 30 assemblages, un par astre qui en a ; les sortes que demandent l'amas de galaxies et l'univers tiennent dans le
 * catalogue ; les trois rayons payants s'ouvrent dans l'ordre ; les sondes couvrent les quatre rayons ; le succès
 * « Chimiste » demande trente sortes, et plus aucun succès ne parle de collection.
 */
class Catalogue178Test {

    private static long count(Molecule.Kind kind) {
        return Molecules.DEFAULT.stream().filter(molecule -> molecule.kind() == kind).count();
    }

    private static long count(Molecule.State state) {
        return Molecules.DEFAULT.stream().filter(molecule -> molecule.hasState() && molecule.state() == state).count();
    }

    @Test
    void soixanteTreizeMoleculesEnQuatreRayonsEtTroisEtats() {
        assertEquals(73, Molecules.DEFAULT.size());
        assertEquals(73, Molecules.DEFAULT.stream().map(Molecule::id).distinct().count(), "deux molécules ont le même identifiant");
        assertEquals(73, Molecules.DEFAULT.stream().map(Molecule::name).distinct().count(), "deux molécules ont le même nom");
        assertEquals(List.of(Molecule.Kind.SIMPLE, Molecule.Kind.ACID, Molecule.Kind.MINERAL, Molecule.Kind.ORGANIC),
                List.of(Molecule.Kind.values()));
        assertEquals(List.of(Molecule.State.GAS, Molecule.State.LIQUID, Molecule.State.CRYSTAL), List.of(Molecule.State.values()));
        assertEquals("Petites molécules", Molecule.Kind.SIMPLE.label());
        assertEquals("Acides et sels", Molecule.Kind.ACID.label());
        assertEquals("Minéraux et matériaux", Molecule.Kind.MINERAL.label());
        assertEquals("Chimie du carbone", Molecule.Kind.ORGANIC.label());
        // Par rayon : 14, 11, 31, 17.
        assertEquals(14, count(Molecule.Kind.SIMPLE));
        assertEquals(11, count(Molecule.Kind.ACID));
        assertEquals(31, count(Molecule.Kind.MINERAL));
        assertEquals(17, count(Molecule.Kind.ORGANIC));
        // Par état : 18 gaz, 15 liquides, 40 cristaux ; chaque molécule en a un.
        for (Molecule molecule : Molecules.DEFAULT) assertTrue(molecule.hasState(), molecule.id() + " n'a pas d'état");
        assertEquals(18, count(Molecule.State.GAS));
        assertEquals(15, count(Molecule.State.LIQUID));
        assertEquals(40, count(Molecule.State.CRYSTAL));
        // Les anciens solides sont des cristaux.
        for (String id : List.of("S8", "C10H8", "C14H10")) {
            Molecule molecule = Molecules.DEFAULT.stream().filter(each -> each.id().equals(id)).findFirst().orElseThrow();
            assertEquals(Molecule.State.CRYSTAL, molecule.state(), id);
        }
    }

    @Test
    void chaqueMoleculeEstDemandeeParUnAssemblageOuUnAstre() {
        Set<String> inAssemblies = new HashSet<>();
        for (Assembly assembly : Assemblies.DEFAULT) inAssemblies.addAll(assembly.ingredients().keySet());
        Set<String> inBodies = new HashSet<>();
        for (Body body : Bodies.DEFAULT) inBodies.addAll(body.molecules().keySet());
        Set<String> catalog = new LinkedHashSet<>();
        for (Molecule molecule : Molecules.DEFAULT) catalog.add(molecule.id());
        for (String id : catalog) {
            assertTrue(inAssemblies.contains(id) || inBodies.contains(id),
                    id + " n'entre dans aucun assemblage et aucun astre ne la demande : elle est de trop dans le catalogue");
        }
        // Et réciproquement : le ciel ne demande rien que le catalogue n'ait.
        for (String id : inAssemblies) assertTrue(catalog.contains(id), "un assemblage demande " + id + ", qui n'est pas au catalogue");
        for (String id : inBodies) assertTrue(catalog.contains(id), "un astre demande " + id + ", qui n'est pas au catalogue");
    }

    @Test
    void trenteAssemblagesUnParAstreNonExotique() {
        assertEquals(30, Assemblies.DEFAULT.size());
        assertEquals(30, Assemblies.DEFAULT.stream().map(Assembly::id).distinct().count());
        assertEquals(35, Bodies.DEFAULT.size());
        Map<String, String> owner = new HashMap<>();
        int exotic = 0;
        for (Body body : Bodies.DEFAULT) {
            if (body.tier() == Body.Tier.EXOTIC) {
                exotic++;
                assertTrue(body.assemblies().isEmpty(), body.id() + " : une matière exotique n'a pas d'assemblage");
                continue;
            }
            assertEquals(1, body.assemblies().size(), body.id() + " devrait demander exactement un assemblage");
            String assembly = body.assemblies().get(0);
            assertNull(owner.put(assembly, body.id()), assembly + " appartient à deux astres : " + owner.get(assembly) + " et " + body.id());
        }
        assertEquals(5, exotic);
        assertEquals(30, owner.size());
        for (Assembly assembly : Assemblies.DEFAULT) {
            assertTrue(owner.containsKey(assembly.id()), assembly.id() + " n'appartient à aucun astre");
        }
        Game game = GameTestSupport.realGame();
        for (Assembly assembly : Assemblies.DEFAULT) assertEquals(owner.get(assembly.id()), game.bodyOf(assembly.id()).id(), assembly.id());
        assertEquals(100, Assembly.MOLECULES_PER_WHOLE, 0);
    }

    @Test
    void lesSortesDuCosmosTiennentDansLeCatalogue() {
        assertTrue(Cosmos.GALAXY.sorts().isEmpty() && Cosmos.GALAXY.matter().isEmpty());
        Map<Molecule.State, Integer> most = new EnumMap<>(Molecule.State.class);
        most.put(Molecule.State.GAS, 18);
        most.put(Molecule.State.LIQUID, 15);
        most.put(Molecule.State.CRYSTAL, 40);
        for (Cosmos scale : List.of(Cosmos.CLUSTER, Cosmos.UNIVERSE)) {
            assertEquals(EnumSet.allOf(Molecule.State.class), scale.matter().keySet(), scale + " : de la matière dans chaque état");
            assertEquals(EnumSet.allOf(Molecule.State.class), scale.sorts().keySet(), scale + " : des sortes dans chaque état");
            for (Molecule.State state : Molecule.State.values()) {
                int sorts = scale.sorts().get(state);
                assertTrue(sorts > 0 && sorts <= most.get(state),
                        scale + " demande " + sorts + " sortes de " + state.label() + " sur " + most.get(state) + " au catalogue");
                assertEquals((long) most.get(state), count(state), state.name());
            }
        }
        for (Molecule.State state : Molecule.State.values()) {
            assertTrue(Cosmos.UNIVERSE.sorts().get(state) > Cosmos.CLUSTER.sorts().get(state), state.name());
            assertTrue(Cosmos.UNIVERSE.matter().get(state) > Cosmos.CLUSTER.matter().get(state), state.name());
        }
        assertEquals(Map.of(Molecule.State.GAS, 60_000_000, Molecule.State.LIQUID, 6_000_000, Molecule.State.CRYSTAL, 13_750_000),
                Cosmos.CLUSTER.matter());
        assertEquals(Map.of(Molecule.State.GAS, 12, Molecule.State.LIQUID, 10, Molecule.State.CRYSTAL, 26), Cosmos.CLUSTER.sorts());
        assertEquals(Map.of(Molecule.State.GAS, 240_000_000, Molecule.State.LIQUID, 24_000_000, Molecule.State.CRYSTAL, 55_000_000),
                Cosmos.UNIVERSE.matter());
        assertEquals(Map.of(Molecule.State.GAS, 15, Molecule.State.LIQUID, 13, Molecule.State.CRYSTAL, 34), Cosmos.UNIVERSE.sorts());
    }

    @Test
    void lesTroisRayonsPayantsSOuvrentDansLOrdre() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 1);
        SpaceUpgrade acid = game.spaceUpgrades().stream().filter(each -> each.id().equals("space_acid")).findFirst().orElseThrow();
        SpaceUpgrade mineral = game.spaceUpgrades().stream().filter(each -> each.id().equals("space_mineral")).findFirst().orElseThrow();
        SpaceUpgrade organic = game.spaceUpgrades().stream().filter(each -> each.id().equals("space_organic")).findFirst().orElseThrow();
        assertEquals(BigNum.of(2_000), acid.space());
        assertEquals(BigNum.of(20_000), mineral.space());
        assertEquals(BigNum.of(200_000), organic.space());
        assertNull(acid.requires());
        assertEquals("space_acid", mineral.requires());
        assertEquals("space_mineral", organic.requires());
        // Aucune autre amélioration n'ouvre de rayon.
        long opening = game.spaceUpgrades().stream().filter(each -> each.effect() instanceof SpaceUpgrade.OpenKind).count();
        assertEquals(3, opening);
        for (String gone : List.of("space_salt", "space_material", "space_life", "space_rare")) {
            assertTrue(game.spaceUpgrades().stream().noneMatch(each -> each.id().equals(gone)), gone + " existe encore");
        }
        // Dans le jeu : les petites molécules d'emblée, puis acide, minéral, organique, chacun après le précédent.
        game.state().setSpace(BigNum.of(1, 9));
        assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.SIMPLE));
        assertEquals(Molecule.Kind.ACID, game.nextMoleculeKind());
        assertFalse(game.isSpaceUpgradeAvailable("space_mineral"));
        assertFalse(game.isSpaceUpgradeAvailable("space_organic"));
        assertTrue(game.buySpaceUpgrade("space_acid"));
        assertEquals(Molecule.Kind.MINERAL, game.nextMoleculeKind());
        assertFalse(game.isSpaceUpgradeAvailable("space_organic"));
        assertFalse(game.buySpaceUpgrade("space_organic"));
        assertTrue(game.buySpaceUpgrade("space_mineral"));
        assertEquals(Molecule.Kind.ORGANIC, game.nextMoleculeKind());
        assertTrue(game.buySpaceUpgrade("space_organic"));
        assertNull(game.nextMoleculeKind());
        assertEquals(4, game.moleculeKindsUnlocked());
    }

    @Test
    void lesDestinationsDeSondesCouvrentLesQuatreRayons() {
        assertEquals(5, ProbeDestination.values().length);
        assertNull(ProbeDestination.VOID.kind());
        Set<Molecule.Kind> covered = EnumSet.noneOf(Molecule.Kind.class);
        for (ProbeDestination destination : ProbeDestination.values()) {
            if (destination.kind() == null) continue;
            assertTrue(covered.add(destination.kind()), "deux destinations pour le rayon " + destination.kind());
        }
        assertEquals(EnumSet.allOf(Molecule.Kind.class), covered);
        assertEquals(Molecule.Kind.SIMPLE, ProbeDestination.GAS_CLOUD.kind());
        assertEquals(Molecule.Kind.ACID, ProbeDestination.SALT_MOON.kind());
        assertEquals(Molecule.Kind.MINERAL, ProbeDestination.ASTEROIDS.kind());
        assertEquals(Molecule.Kind.ORGANIC, ProbeDestination.NEBULA.kind());
    }

    @Test
    void leChimisteDemandeTrenteSortesEtAucunSuccesNeParleDeCollection() {
        Achievement chemist = Achievements.byId("step_fifty_kinds");
        assertEquals("Chimiste", chemist.name());
        assertTrue(chemist.summary().contains("30"), chemist.summary());
        Game game = GameTestSupport.realGame();
        int kinds = 0;
        for (Molecule molecule : Molecules.DEFAULT) {
            if (kinds == 29) break;
            game.state().setMoleculeCount(molecule.id(), 1);
            kinds++;
        }
        assertEquals(29, game.moleculeKindsCreated());
        assertFalse(chemist.condition().test(game), "vingt-neuf sortes ne suffisent pas");
        game.state().setMoleculeCount("C4H4S", 1);
        assertEquals(30, game.moleculeKindsCreated());
        assertTrue(chemist.condition().test(game), "trente sortes : Chimiste");
        for (Achievement achievement : Achievements.DEFAULT) {
            assertFalse(achievement.id().equals("step_half_collection") || achievement.id().equals("feat_full_collection"),
                    achievement.id() + " : les collections de rayon n'existent plus");
            assertFalse(achievement.id().contains("collection"), achievement.id());
            assertFalse(achievement.name().toLowerCase().contains("collection de rayon"), achievement.id());
        }
        assertEquals(97, Achievements.DEFAULT.size());
    }
}
