import idle.core.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.save.*;import idle.core.achievements.*;import java.nio.file.*;import java.util.*;
public class SyntheticCosmicStart{
public static void main(String[]args)throws Exception{
Game g=new Game(new GameState(),Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,Achievements.DEFAULT,new Random(42));g.start();g.state().setBigBangs(1);for(int e=1;e<=118;e++)g.state().setElementCount(e,20);
Files.writeString(Path.of("/mnt/data/idle_analysis/synthetic_cosmic.save"),SaveCodec.write(g.state(),0));System.out.printf("N=%d kinds=%d unlocked=%d bb=%d%n",g.molecules().size(),g.moleculeKindsCreated(),g.moleculeKindsUnlocked(),g.bigBangs());
}}
