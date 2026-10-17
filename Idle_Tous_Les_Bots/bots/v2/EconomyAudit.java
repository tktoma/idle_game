import idle.core.*;import idle.core.math.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.save.*;import java.util.*;import java.nio.file.*;import java.io.*;
/** Audit des achats reellement abordables dans des etats reproductibles.
 * Delta production et delai de retour estimes statiquement, SANS extrapolation de prestige.
 */
public class EconomyAudit {
 static Game from(GameState st,long seed){return new Game(st,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(seed));}
 static Game copy(Game g){try{return from(SaveCodec.read(SaveCodec.write(g.state(),0)).state(),42);}catch(Exception e){throw new IllegalStateException(e);}}
 static String num(double n){return Double.isFinite(n)?String.format(Locale.ROOT,"%.6g",n):"NA";}
 static void audit(Game g,PrintWriter w,String label){
  BigNum base=g.productionPerSecond();
  for(Upgrade u:g.upgrades(Resource.PARTICLES)){
   if(!g.canBuy(u.id()))continue;
   BigNum cost=g.costOf(u.id());Game v=copy(g);
   if(!v.buy(u.id()))continue;
   BigNum next=v.productionPerSecond(),delta=next.subtract(base);
   double gain=next.log10()-base.log10();
   double payback=delta.gt(BigNum.ZERO)?cost.divide(delta).toDouble():Double.NaN;
   w.printf(Locale.ROOT,"%s,%s,%s,%s,%s,%s,%s,%s%n",label,"PARTICLES",u.id(),cost,num(base.log10()),num(next.log10()),num(gain),num(payback));
  }
  for(Upgrade u:g.upgrades(Resource.ATOMS)){
   if(!g.canBuy(u.id()))continue;
   BigNum cost=g.costOf(u.id());Game v=copy(g);
   if(!v.buy(u.id()))continue;
   BigNum next=v.productionPerSecond(),delta=next.subtract(base);
   double gain=next.log10()-base.log10();
   double payback=delta.gt(BigNum.ZERO)?cost.divide(delta).toDouble():Double.NaN;
   w.printf(Locale.ROOT,"%s,%s,%s,%s,%s,%s,%s,%s%n",label,"ATOMS",u.id(),cost,num(base.log10()),num(next.log10()),num(gain),num(payback));
  }
 }
 public static void main(String[]a)throws Exception{
  Path out=Path.of(a[0]);Files.createDirectories(out.getParent());
  try(PrintWriter w=new PrintWriter(Files.newBufferedWriter(out))){
   w.println("checkpoint,ressource,amelioration,cout,production_avant_log10,production_apres_log10,gain_log10,retour_sur_investissement_s_approx");
   Game g=from(new GameState(),42);g.start();g.tick(180);audit(g,w,"neuf_3min_sans_achats");
   g=from(new GameState(),42);g.start();g.tick(1800);audit(g,w,"neuf_30min_sans_achats");
   for(String cp:new String[]{"checkpoint_bigbang.save","checkpoint_before_eighth.save"}){
    Path p=Path.of(a.length>1?a[1]:"/mnt/data/idle_lab_v2/work/full/sauvegardes",cp);
    Game state=from(SaveCodec.read(Files.readString(p)).state(),42);
    audit(state,w,cp);
   }
  }
  System.out.println("ECONOMY_CSV,"+out);
 }
}
