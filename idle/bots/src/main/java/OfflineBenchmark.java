import idle.core.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.save.*;import idle.core.sky.*;import java.util.*;import java.nio.file.*;import java.io.*;
/** Comparaison calendaire / temps actif / rejeu hors ligne avec meme sauvegarde. */
public class OfflineBenchmark {
 record Pattern(String name,int periodSeconds,int onlineSeconds,boolean actions){}
 static final Pattern[] P={
  new Pattern("present_actif",0,0,true),new Pattern("jeu_ouvert_AFK",0,0,false),
  new Pattern("15min_toutes_3h",10800,900,true),new Pattern("2h_puis_10h_absence",43200,7200,true),
  new Pattern("1h_par_jour",86400,3600,true)
 };
 public static void main(String[]a)throws Exception{
  Path out=Path.of(a[0]); Path checkpoint=Path.of(a[1]);double hours=Double.parseDouble(a[2]);double dt=20;Files.createDirectories(out.getParent());
  try(PrintWriter w=new PrintWriter(Files.newBufferedWriter(out))){
   w.println("scenario,calendrier_h,actif_h,temps_jeu_credite_h,taux_hors_ligne_depart,bb,astres_murs,galaxie,amas,univers,espace,sortes_rassemblees,assemblages");
   for(Pattern p:P){
    GameState st=SaveCodec.read(Files.readString(checkpoint)).state();Game g=new Game(st,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(42));
    RobotCosmosPlanner bot=new RobotCosmosPlanner(g,dt);double online=0,credited=0;double rate=g.offlineRate();
    for(int i=0;i<(int)Math.ceil(hours*3600/dt);i++){
      double wall=(i*dt);boolean on=p.periodSeconds()==0 || wall%p.periodSeconds()<p.onlineSeconds();
      if(on){bot.back();g.tick(dt);online+=dt; if(p.actions()){
        if(g.isCometVisible())g.catchComet();if(g.isPulseUnlocked())for(int k=0;k<g.pulseCharges();k++)g.pulse();
        bot.collider();bot.probes();bot.cosmos();bot.resourceLoop();
      }}else credited+=bot.away(dt);
    }
    w.printf(Locale.ROOT,"%s,%.4f,%.4f,%.4f,%.5f,%d,%d,%b,%b,%b,%s,%d,%d%n",p.name(),hours,online/3600,credited/3600+online/3600,rate,g.bigBangs(),g.bodiesMature(),g.hasCosmos(Cosmos.GALAXY),g.hasCosmos(Cosmos.CLUSTER),g.hasCosmos(Cosmos.UNIVERSE),g.state().space(),g.substancesFormed(),g.assembliesFormed()); w.flush();
   }
  }
  System.out.println("OFFLINE_DONE,"+out);
 }
}
