import idle.core.*;
import idle.core.achievements.*;
import idle.core.darkmatter.*;
import idle.core.bigbang.*;
import idle.core.math.*;
import idle.core.molecules.*;
import idle.core.save.*;
import idle.core.upgrades.*;
import java.nio.file.*;
import java.util.*;

/** Sauvegarde synthétique contrôlée pour tester UNE mécanique, pas une durée de partie normale. */
public final class MakeAutoMatterCheckpoint {
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("chemin_sortie.save");
        Game g=new Game(new GameState(),Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,
                Achievements.DEFAULT,new Random(42));
        g.start();
        g.state().setBigBangs(8);
        // Depuis la version 0.124, un palier atteint ne sert qu'une fois payé : celui de la création automatique compris.
        for(BigBangMilestone milestone:g.bigBangMilestones())g.state().addBoughtMilestone(milestone.name());
        g.state().setSpace(BigNum.of(1,14));
        for(SpaceUpgrade u:g.spaceUpgrades())
            if(!(u.effect() instanceof SpaceUpgrade.Boost))g.state().addSpaceUpgrade(u.id());
        for(int i=1;i<=118;i++)g.state().setElementCount(i,200);
        g.setAutoFollowList(false);
        g.setAutoMoleculeReserve(0.10);
        Set<Molecule.State> missing=g.shoppingList().matter().keySet();
        int added=0;
        for(Molecule.State state:missing){
            for(Molecule m:g.molecules()){
                if(!m.hasState()||m.state()!=state||!g.isMoleculeKindUnlocked(m.kind()))continue;
                g.state().setMoleculeCount(m.id(),Game.SUBSTANCE_MOLECULES);
                g.state().addSubstance(m.id());
                added++;
                break;
            }
        }
        if(added==0 || g.autoMatterTargets().isEmpty())
            throw new IllegalStateException("Le catalogue ne permet pas de préparer un besoin de matière");
        Path target=Path.of(args[0]);Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target,SaveCodec.write(g.state(),0));
        System.out.println("CHECKPOINT_AB_SYNTHETIQUE,"+target+",substances="+added+",besoins="+missing);
    }
}
