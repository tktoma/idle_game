import idle.core.*;
import java.util.*;
import java.nio.file.*;

/**
 * Une partie neuve par profil, sans boost, qui ne joue que les deux premiers actes : à chaque décision, seulement
 * {@link RobotCosmosPlanner#resourceLoop()} (achats, fusions, synthèses, arbre de matière noire, défis, explosions et
 * Big Bangs), avec l'attention et la présence du profil. Ni cosmos, ni sondes, ni collisionneur, ni comètes : le ciel
 * ne se construit pas, les Big Bangs s'arrêtent au cinquième et l'univers ne se forme jamais ici. Pour le chemin
 * complet depuis zéro, voir {@link FreshUniverseAudit}. C'est la partie « début » de l'empreinte du moteur
 * ({@link Empreinte}) : son déroulement ne doit pas changer sans refaire l'empreinte.
 */
public class FreshPhaseProfiles extends IdleProfiles24 {
  FreshPhaseProfiles(Game g,Profile p,long seed,double step){super(g,p,seed,step);}
  void runFresh(double hours, Path output) throws Exception {
    Files.createDirectories(output);
    double wall=0,online=0,credited=0,nextAction=0;
    int fusionAt=-1,automationAt=-1,firstElementAt=-1,explosionAt=-1, bbAt=-1;
    int n=(int)Math.ceil(hours*3600/step),actionsTaken=0;
    Random attendance=new Random(30000+p.id());
    StringBuilder progress=new StringBuilder("wall_hours,game_hours,online_hours,explosions,bb,fusions,elements,achievements,atoms_log,particles_log\n");
    for(int i=0;i<n;i++){
      boolean on=p.cycleSeconds()==0 || (wall%p.cycleSeconds()<p.onlineSeconds());
      if(on){back();g.tick(step);online+=step;g.buyAllBigBangMilestones();}
      else credited+=away(step);
      wall+=step;wallSeconds=wall;
      if(on && wall>=nextAction && attendance.nextDouble()<p.attention()){
        actionsTaken++;super.resourceLoop();
        nextAction=wall+p.actionEverySeconds();
      }
      if(fusionAt<0 && g.state().stats().fusions()>0)fusionAt=i;
      if(automationAt<0 && g.isAutomationUnlocked())automationAt=i;
      if(firstElementAt<0 && g.discoveredElements()>0)firstElementAt=i;
      if(explosionAt<0 && g.state().explosions()>0)explosionAt=i;
      if(bbAt<0 && g.bigBangs()>0)bbAt=i;
      if((i+1)%Math.max(1,Math.round((float)(1800/step)))==0)progress.append(String.format(Locale.ROOT,"%.4f,%.4f,%.4f,%d,%d,%d,%d,%d,%.3f,%.3f%n",wall/3600,g.state().timePlayed()/3600,online/3600,g.state().explosions(),g.bigBangs(),g.state().stats().fusions(),g.discoveredElements(),g.achievementCount(),g.state().atoms().log10(),g.state().particles().log10()));
    }
    Files.writeString(output.resolve(String.format(Locale.ROOT,"%02d_%s_fresh.csv",p.id(),p.name())),progress.toString());
    System.out.printf(Locale.ROOT,"FRESH,%d,%s,wall=%.4f,game=%.4f,online=%.4f,offlineCredited=%.4f,actions=%d,fusions=%d,elements=%d,explosions=%d,bigbangs=%d,firstFusion=%s,automation=%s,firstElement=%s,firstExplosion=%s,firstBang=%s,universes=%d,universeTimes=%s%n",p.id(),p.name(),wall/3600,g.state().timePlayed()/3600,online/3600,credited/3600,actionsTaken,g.state().stats().fusions(),g.discoveredElements(),g.state().explosions(),g.bigBangs(),when(fusionAt),when(automationAt),when(firstElementAt),when(explosionAt),when(bbAt),g.universes(),universeHours());
  }
  String when(int i){return i<0?"-":String.format(Locale.ROOT,"%.4f",(i+1)*step/3600);}
  public static void main(String[]args)throws Exception{
    if(args.length<4){System.err.println("Usage: id  wallHours  stepSeconds  outputDir [seed]");System.exit(2);}
    int id=Integer.parseInt(args[0]);double hours=Double.parseDouble(args[1]),dt=Double.parseDouble(args[2]);Path out=Path.of(args[3]);long seed=args.length>4?Long.parseLong(args[4]):42;
    Game g=Bots.fresh(seed);
    new FreshPhaseProfiles(g,PROFILES[id-1],seed,dt).runFresh(hours,out);
  }
}
