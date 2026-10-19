import idle.core.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.save.*;import idle.core.molecules.*;import idle.core.sky.*;import java.nio.file.*;import java.util.*;
public class DiagnoseState {
 public static void main(String[] a)throws Exception{
  for(String file:a){GameState s=SaveCodec.read(Files.readString(Path.of(file))).state();Game g=new Game(s,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(42));
   ShoppingList wish=g.shoppingList();System.out.println("=== "+file);
   System.out.printf("time=%.1f bb=%d bodies=%d mature=%d cosmos=%d molecules=%d kinds=%d sub=%d assemblies=%d%n",s.timePlayed()/3600,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.cosmosFormed(),g.moleculesCreated(),g.moleculeKindsCreated(),g.substancesFormed(),g.assembliesFormed());
   System.out.println("space="+s.space()+" free="+g.freeSpace()+" perSec="+g.spacePerSecond()+" occupied="+g.usedSpace());
   System.out.println("WISH="+wish.kind()+" "+wish.target()+" ready="+wish.ready()+" items="+wish.items().size()+" matter="+wish.matter()+" sorts="+wish.sorts());
   for(Molecule.State st:Molecule.State.values()){long n=g.gatheredInState(st); System.out.println("MATTER "+st+" "+n);}
   int creatable=0, within=0;
   for(Molecule m:g.molecules()){if(g.isMoleculeWithinReach(m.id()))within++;if(g.canCreateMolecule(m.id()))creatable++;}
   System.out.println("withinReach="+within+" creatable="+creatable+" elements="+g.discoveredElements()+" atoms="+s.atoms()+" particlelog="+s.particles().log10()+" ppslog="+g.productionPerSecond().log10()+" canFuse="+g.canFuse()+" canSynth="+g.canSynthesize()+" final="+g.canBigBang()+" next="+(g.nextBody()==null?"none":g.nextBody().id()));
   for(int i=0;i<Math.min(5,wish.items().size());i++){var item=wish.items().get(i);System.out.println("  missing="+item.missing()+" "+item.molecule().id()+" can="+g.canCreateMolecule(item.molecule().id()));}
  }
 }
}
