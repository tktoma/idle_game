import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le bot expert depuis le septième Big Bang jusqu'au premier univers : le plus long des tests des bots (150 s seul,
 * un quart d'heure sur une machine chargée ; 25 h de jeu). Version 0.170 : dans sa propre classe, pour que Gradle le
 * joue dans une JVM à part pendant que les autres tests des bots tournent (maxParallelForks) ; avant, il allongeait
 * BotsTest d'autant. Il vient de BotsTest, à l'identique.
 */
class ExpertPremierUniversTest {

    private static Game load(Path save) throws Exception {
        return Bots.load(save, 42);
    }

    @Test
    void expertTermineLePremierUniversDepuisLeSeptiemeBigBang(@TempDir Path out) throws Exception {
        Game g = load(Path.of("sauvegardes", "reference", "checkpoint_before_eighth.save"));
        AdvancedLab expert = new AdvancedLab(g, 20, "EXPERT_COMPLET", 42);
        expert.runLab(25, out);
        System.out.printf("EXPERT_CHECK,bb=%d,cosmos=%d,conditions=%d/%d,free=%s,space=%s,goal=%s%n",
                g.bigBangs(), g.cosmosFormed(),
                g.cosmosConditionsMet(idle.core.sky.Cosmos.CLUSTER),
                g.cosmosConditions(idle.core.sky.Cosmos.CLUSTER),
                g.freeSpace(), g.state().space(), g.shoppingList().target());
        for(var state : idle.core.molecules.Molecule.State.values()){
            System.out.printf("EXPERT_STATE,%s,gathered=%d,sorts=%d,needed=%s,sortsNeeded=%s%n",
                state, g.gatheredInState(state), g.sortsInState(state),
                idle.core.sky.Cosmos.CLUSTER.matter().getOrDefault(state,0),
                idle.core.sky.Cosmos.CLUSTER.sorts().getOrDefault(state,0));
            var target = FirstUniversePolicy.matterTarget(g, state);
            if (target != null) {
                System.out.printf("EXPERT_TARGET,%s,id=%s,count=%d,draw=%d,canCreate=%s,auto=%s,enabled=%s,interval=%s,recipe=%s%n",
                        state,target.id(),g.moleculeCount(target.id()),g.moleculesPerCreation(target.id()),
                        g.canCreateMolecule(target.id()),g.isMoleculeAutomated(target.id()),
                        g.isAutoCreatingMolecules(),g.moleculeAutomationInterval(),target.recipe());
                for (var atom : target.recipe().entrySet())
                    System.out.printf("EXPERT_ATOM,%s,z=%d,stock=%d,need=%d%n",
                            state,atom.getKey(),g.elementCount(atom.getKey()),atom.getValue());
            }
        }
        assertTrue(g.hasCosmos(idle.core.sky.Cosmos.UNIVERSE),
                () -> "Le bot expert doit terminer le premier univers : bb="+g.bigBangs()
                +", cosmos="+g.cosmosFormed()+", mature="+g.bodiesMature()
                +", cible="+g.shoppingList().target()+", libre="+g.freeSpace()
                +", espace="+g.state().space()+", auto="+g.isAutoCreatingMolecules()
                +", synthese="+g.isAutomationEnabled("auto_synthesis")
                +", ingredients="+g.shoppingList().items()
                +", matiere="+g.shoppingList().matter());
        assertTrue(Files.exists(out.resolve("winner.save")));
    }
}
