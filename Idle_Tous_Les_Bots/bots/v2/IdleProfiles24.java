import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.math.*;
import idle.core.molecules.*;
import idle.core.sky.*;
import idle.core.collider.*;
import idle.core.probes.*;
import idle.core.save.*;
import java.util.*;
import java.nio.file.*;

/**
 * 24 joueurs idle : comportements explicites, interventions et absence réelle.
 * Aucune modification de Game/GameState; chaque essai charge la même sauvegarde.
 * Les profils sont des hypothèses comportementales, pas des statistiques de joueurs réels.
 */
public class IdleProfiles24 extends RobotCosmosPlanner {
  record Profile(int id, String name, String label, int actionEverySeconds,
      int cycleSeconds, int onlineSeconds, double attention, double planning,
      double ordinaryActions, double techAttention, double skyAttention,
      int shopMode, String bias) {}

  // cycle=0 signifie jeu ouvert 100% du temps. Pour les intermittents, le jeu fermé reçoit
  // tickAway(dt * game.offlineRate()) et les décisions humaines sont absentes.
  static final Profile[] PROFILES = {
    new Profile( 1,"Speedrunner","Optimaliste / clics et micro-gestion",20,0,0,1,1,1,1,1,0,"doubleplan"),
    new Profile( 2,"Veteran","Habitué du genre, réagit vite",20,0,0,1,.95,1,1,1,0,"smart"),
    new Profile( 3,"Regular","Joueur régulier équilibré",60,0,0,.90,.88,.9,.85,.9,0,"normal"),
    new Profile( 4,"Methodical","Économe qui planifie",120,0,0,.95,1,.75,.75,1,0,"save"),
    new Profile( 5,"AchievementHunter","Chasseur de succès",40,0,0,.85,.80,.8,.8,.9,0,"achievement"),
    new Profile( 6,"Collector","Complétionniste des molécules",40,0,0,.95,.70,.8,.9,.9,2,"collect"),
    new Profile( 7,"Chemist","Passionné des recettes",40,0,0,1,1,.8,.7,1,0,"chemistry"),
    new Profile( 8,"Astronomer","Priorité astres et systèmes",40,0,0,.95,1,.8,.9,1,0,"sky"),
    new Profile( 9,"Explorer","Sondes et collisionneur d'abord",40,0,0,.95,.85,.9,1,1,0,"tech"),
    new Profile(10,"PassiveOptimist","Adepte des automatismes",120,0,0,.9,.78,.7,.8,.8,0,"auto"),
    new Profile(11,"PrestigeAddict","Prestige dès que possible",40,0,0,.9,.75,1,.8,.85,0,"prestige"),
    new Profile(12,"Hoarder","Accumule et hésite à dépenser",120,0,0,.85,.8,.45,.7,.85,0,"hoard"),
    new Profile(13,"BulkBuyer","N'achète qu'en grosses séries",300,0,0,1,.85,.95,.8,.9,0,"bulk"),
    new Profile(14,"OneClick","Un achat à la fois",60,0,0,.85,.75,.6,.65,.75,1,"oneclick"),
    new Profile(15,"RandomClicker","Clique partout, objectifs approximatifs",40,0,0,.75,.35,.85,.5,.45,3,"random"),
    new Profile(16,"Beginner","Débutant qui apprend",180,0,0,.75,.55,.65,.45,.55,1,"beginner"),
    new Profile(17,"AFKOpen","Laisse le jeu ouvert, passe parfois",3600,0,0,1,.8,.65,.65,.7,0,"afk"),
    new Profile(18,"BackgroundWorker","Joue en travaillant (fenêtre ouverte)",600,0,0,.85,.85,.8,.75,.8,0,"background"),
    new Profile(19,"MobileSnacker","5 minutes actives par heure",60,3600,300,.95,.85,.9,.85,.85,0,"mobile"),
    new Profile(20,"LunchBreak","30 minutes toutes les 4 heures",60,14400,1800,.95,.9,.9,.8,.9,0,"lunch"),
    new Profile(21,"EveningPlayer","2 heures toutes les 24 heures",30,86400,7200,1,.9,1,1,1,0,"evening"),
    new Profile(22,"WeekendPlayer","4 heures toutes les 48 heures",40,172800,14400,.95,.85,.9,.85,.85,0,"weekend"),
    new Profile(23,"Forgetful","Des pauses et oublis fréquents",180,3600,1200,.55,.6,.7,.45,.55,1,"forgetful"),
    new Profile(24,"ReturningVeteran","Expert mais seulement 15 min / 3 h",60,10800,900,1,1,1,1,1,0,"returning")
  };

  final Profile p;
  final Random rng;
  double wallSeconds=0, onlineSeconds=0, creditedOfflineSeconds=0;
  int actions=0, planningActions=0, techActions=0, otherActions=0;
  int first8=-1, galaxy=-1, cluster=-1, universe=-1;
  int lastBang, lastBody, lastCosmos;
  boolean originalMoleculeAutomation;

  IdleProfiles24(Game game, Profile profile, long seed, double decisionStep){
    super(game,decisionStep);this.p=profile;this.rng=new Random(seed ^ (profile.id*11939L));
    lastBang=game.bigBangs();lastBody=game.bodiesFormed();lastCosmos=game.cosmosFormed();
  }

  @Override void probes(){ if(rng.nextDouble()<p.techAttention()) { super.probes(); techActions++; } }
  @Override void collider(){ if(rng.nextDouble()<p.techAttention()) super.collider(); }

  // Maintient les mêmes règles du jeu, mais pas les mêmes choix et fréquences.
  @Override void cosmos(){
    if(rng.nextDouble()>=p.planning())return;
    planningActions++;
    if(p.bias().equals("collect") || p.bias().equals("random")){
      int max=p.bias().equals("collect")?12:8;
      int count=0;
      for(Molecule m:g.molecules()){
        if(++count>max)break;
        if(g.canCreateMolecule(m.id()))g.createMolecule(m.id());
      }
    }
    if(p.bias().equals("tech") && planningActions%3==0){super.probes();super.collider();}
    if(p.bias().equals("sky") && planningActions%4==0){systems();preEighthBody();}
    // La stratégie 'random' ne lit la liste des tâches que rarement, les autres la suivent.
    if(p.bias().equals("random") && rng.nextDouble()<.60)return;
    if(p.shopMode()==1 && rng.nextDouble()<.22)return;
    super.cosmos();
    if(p.bias().equals("chemistry") || p.bias().equals("doubleplan")){
      if(rng.nextDouble()<.50)super.cosmos();
    }
    if(p.bias().equals("auto"))g.setMoleculeAutomationEnabled(true);
  }

  @Override void resourceLoop(){
    if(rng.nextDouble()>p.ordinaryActions())return;
    otherActions++;
    if(p.shopMode()==1){
      // Effet joueur qui n'optimise pas : il s'arrête plus souvent avant de traiter tous les achats.
      if(rng.nextDouble()<.35)return;
    }
    if(p.bias().equals("hoard") && rng.nextDouble()<.50){
      // Il optimise seulement une décision sur deux, par peur de dépenser ses réserves.
      if(g.canExplode() && rng.nextDouble()<.2)g.explode();
      return;
    }
    super.resourceLoop();
    if(p.bias().equals("prestige") && g.canExplode())g.explode();
    if(p.bias().equals("auto")){g.setMoleculeAutomationEnabled(true);g.setAutoGatherEnabled(true);g.setAutoFormEnabled(true);}
  }

  private boolean online(){
    if(p.cycleSeconds()==0)return true;
    return (wallSeconds % p.cycleSeconds()) < p.onlineSeconds();
  }

  private static String fmt(double d){return String.format(Locale.ROOT,"%.4f",d);}
  private void event(String what){
    System.out.printf(Locale.ROOT,"EVENT,%d,%s,%s,wall_h=%.4f,game_h=%.4f,bb=%d,bodies=%d,cosmos=%d%n",p.id(),p.name(),what,wallSeconds/3600,g.state().timePlayed()/3600,g.bigBangs(),g.bodiesFormed(),g.cosmosFormed());
  }

  void execute(double horizonWallHours,Path output) throws Exception{
    // Pas de simulation identique pour tous pour isoler les comportements. Les interventions
    // de 20s sont min et celles de plus de 20s sont des décisions moins fréquentes.
    double dt=step;
    int count=(int)Math.ceil(horizonWallHours*3600/dt);
    double nextActionAt=0;
    double startGameTime=g.state().timePlayed();
    int startBang=g.bigBangs(),startBody=g.bodiesFormed();
    StringBuilder logs=new StringBuilder("wall_hours,game_hours,online_hours,bb,bodies,mature,systems,cosmos,particles_log,atoms_log,space,shop\n");
    for(int i=0;i<count;i++){
      boolean isOnline=online();
      if(isOnline){g.tick(dt);onlineSeconds+=dt;}
      else {double credited=dt*g.offlineRate();g.tickAway(credited);creditedOfflineSeconds+=credited;}
      wallSeconds+=dt;
      if(isOnline && wallSeconds>=nextActionAt && rng.nextDouble()<p.attention()){
        actions++;
        if(g.isCometVisible() && rng.nextDouble()<p.skyAttention())g.catchComet();
        if(g.isPulseUnlocked() && rng.nextDouble()<p.skyAttention())for(int k=0;k<g.pulseCharges();k++)g.pulse();
        if(g.state().explosions()>0 || g.isBigBangUnlocked())collider();
        probes();cosmos();resourceLoop();
        nextActionAt=wallSeconds+p.actionEverySeconds();
      }
      if(lastBang<8 && g.bigBangs()>=8){first8=i;event("BB8");}
      if(!g.hasCosmos(Cosmos.GALAXY) && lastCosmos>=1){} // événements captés via les transitions ci-dessous
      if(lastCosmos<1 && g.cosmosFormed()>=1){galaxy=i;event("GALAXY");}
      if(lastCosmos<2 && g.cosmosFormed()>=2){cluster=i;event("CLUSTER");}
      if(lastCosmos<3 && g.cosmosFormed()>=3){universe=i;event("UNIVERSE");}
      lastBang=g.bigBangs();lastBody=g.bodiesFormed();lastCosmos=g.cosmosFormed();
      if((i+1)%(int)Math.ceil(3600/dt)==0 || universe>=0){
        logs.append(String.format(Locale.ROOT,"%.4f,%.4f,%.4f,%d,%d,%d,%d,%d,%.3f,%.3f,%s,%s%n",wallSeconds/3600,g.state().timePlayed()/3600,onlineSeconds/3600,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),g.cosmosFormed(),g.state().particles().log10(),g.state().atoms().log10(),g.state().space(),g.shoppingList().target().replace(',',';')));
      }
      if(universe>=0)break;
    }
    Files.createDirectories(output);
    Files.writeString(output.resolve(String.format(Locale.ROOT,"%02d_%s_%s.csv",p.id(),p.name(),System.getProperty("idle.stage","late"))),logs.toString());
    // Référence exacte de la victoire dans la sauvegarde finale de chaque robot gagnant.
    if(universe>=0)Files.writeString(output.resolve(String.format(Locale.ROOT,"%02d_%s_winner.save",p.id(),p.name())),SaveCodec.write(g.state(),0));
    System.out.printf(Locale.ROOT,"RESULT,%d,%s,%s,wall=%.4f,game=%.4f,startgame=%.4f,online=%.4f,offlineCredited=%.4f,bb=%d,bodies=%d,mature=%d,systems=%d,cosmos=%d,actions=%d,plans=%d,tech=%d,ordinary=%d,BB8at=%s,galaxyAt=%s,clusterAt=%s,universeAt=%s%n",p.id(),p.name(),p.bias(),wallSeconds/3600,g.state().timePlayed()/3600,startGameTime/3600,onlineSeconds/3600,creditedOfflineSeconds/3600,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),g.cosmosFormed(),actions,planningActions,techActions,otherActions,first8<0?"-":fmt((first8+1)*dt/3600),galaxy<0?"-":fmt((galaxy+1)*dt/3600),cluster<0?"-":fmt((cluster+1)*dt/3600),universe<0?"-":fmt((universe+1)*dt/3600));
  }
  public static void main(String[]args)throws Exception{
    if(args.length<5){System.err.println("Usage: id  checkpoint  wallHours  stepSeconds outputDir [seed] [stage]");System.exit(2);}
    int id=Integer.parseInt(args[0]);Profile p=PROFILES[id-1];
    Path file=Path.of(args[1]);double hours=Double.parseDouble(args[2]),step=Double.parseDouble(args[3]);
    Path out=Path.of(args[4]);long seed=args.length>5?Long.parseLong(args[5]):42;
    if(args.length>6)System.setProperty("idle.stage",args[6]);
    GameState state=SaveCodec.read(Files.readString(file)).state();
    Game game=new Game(state,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(seed));
    new IdleProfiles24(game,p,seed,step).execute(hours,out);
  }
}
