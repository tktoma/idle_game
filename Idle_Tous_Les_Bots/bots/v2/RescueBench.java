import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.math.*;
import idle.core.sky.*;
import idle.core.save.*;
import java.util.*;import java.nio.file.*;
/** Trois corrections isolees depuis une sauvegarde figee apres 72 heures, memes parametres. */
public class RescueBench extends IdleProfiles24 {
 final int which; final boolean help;
 RescueBench(Game g,Profile p,long seed,double step,boolean help){super(g,p,seed,step);which=p.id();this.help=help;}
 @Override void cosmos(){
   if(!help){super.cosmos();return;}
   switch(which){
     case 11 -> { // CONSEIL 1 : viser l'astre manquant au lieu du prestige.
       if(g.bigBangs()==7 && g.bodiesMature()<28){preEighthBody();systems();}
       super.cosmos();
     }
     case 12 -> { // CONSEIL 2 : depenser directement pour la liste de courses.
       super.cosmos();
       super.cosmos();
     }
     case 17 -> { // CONSEIL 3 : executer une liste entiere lors du passage horaire.
       for(int i=0;i<8;i++)super.cosmos();
     }
     default -> super.cosmos();
   }
 }
 @Override void resourceLoop(){
   if(!help){super.resourceLoop();return;}
   if(which==11 && g.bigBangs()==7 && g.bodiesMature()<28){ // arreter les explosions volontairement
     g.setDarkAutomationEnabled("dark_auto_explosion",false);
     particle(g,500);
     for(int i=0;i<5&&g.canFuse();i++){g.fuse();atoms(g,50);particle(g,100);}
     for(Automation a:g.automations()){if(g.canBuyAutomation(a.id()))g.buyAutomation(a.id());if(g.canSpeedUpAutomation(a.id()))g.speedUpAutomation(a.id());}
     atoms(g,60);
     for(int i=0;i<20&&g.canSynthesize();i++)g.synthesize();
     g.growDarkMatter(step);
     dark(g,40);
     return;
   }
   if(which==12){ // reste un echantillon du meme taux d'attention mais n'hesite plus a depenser
      if(rng.nextDouble()<=p.ordinaryActions())super.resourceLoop();
      return;
   }
   super.resourceLoop();
 }
 public static void main(String[]a)throws Exception{
   if(a.length<6)throw new IllegalArgumentException("id checkpoint heures step help output [seed]");
   int id=Integer.parseInt(a[0]);Path cp=Path.of(a[1]);double hours=Double.parseDouble(a[2]),step=Double.parseDouble(a[3]);boolean help=Boolean.parseBoolean(a[4]);Path out=Path.of(a[5]);long seed=a.length>6?Long.parseLong(a[6]):42;
   GameState state=SaveCodec.read(Files.readString(cp)).state();
   Game g=new Game(state,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(seed));
   System.out.printf(Locale.ROOT,"START_RESCUE,%d,%b,bb=%d,bodies=%d,mature=%d,cosmos=%d%n",id,help,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.cosmosFormed());
   new RescueBench(g,PROFILES[id-1],seed,step,help).execute(hours,out);
   Files.createDirectories(out); Files.writeString(out.resolve("end_state.save"),SaveCodec.write(g.state(),0));
   System.out.printf(Locale.ROOT,"END_RESCUE,%d,%b,bb=%d,bodies=%d,mature=%d,cosmos=%d%n",id,help,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.cosmosFormed());
 }
}
