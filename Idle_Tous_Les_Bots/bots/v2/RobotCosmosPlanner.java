import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.bigbang.*;
import idle.core.math.*;
import idle.core.molecules.*;
import idle.core.sky.*;
import idle.core.collider.*;
import idle.core.probes.*;
import idle.core.save.*;
import java.util.*;
import java.nio.file.*;

/** Politique à objectifs : progression de matière + prérequis des Big Bangs et du cosmos. */
public class RobotCosmosPlanner {
  static final Path OUTPUT_DIR=Path.of(System.getProperty("idle.outputDir", "/mnt/data/idle_analysis"));
  final Game g;
  final double step;
  double elapsed=0;
  int prevBangs,prevBodies,prevAssemblies,prevSub,prevCosmos,prevMature;
  long prevMolecules;
  boolean abandoned=false;
  final List<String> csv=new ArrayList<>();
  int tickCount=0;
  RobotCosmosPlanner(Game game,double step){this.g=game;this.step=step;prevBangs=g.bigBangs();csv.add("total_hours,bigbangs,explosions,challenges,achievements,elements,molecules,substances,assemblies,bodies,mature,systems,cosmos,space,freeSpace,spaceRate,shopping");}
  static void particle(Game g,int limit){for(int i=0;i<limit;i++){Upgrade b=null;for(Upgrade u:g.upgrades(Resource.PARTICLES))if(g.canBuy(u.id()) && (b==null||g.costOf(u.id()).lt(g.costOf(b.id()))))b=u;if(b==null)break;g.buy(b.id());}}
  static void atoms(Game g,int limit){for(int i=0;i<limit;i++){Upgrade b=null;for(Upgrade u:g.upgrades(Resource.ATOMS))if(!u.id().equals("atom_keep") && g.canBuy(u.id()) && (b==null||g.costOf(u.id()).lt(g.costOf(b.id()))))b=u;if(b==null)break;g.buy(b.id());}}
  static void dark(Game g,int limit){for(int i=0;i<limit;i++){boolean bought=false;for(DarkUpgrade u:g.darkUpgrades()){if(g.canBuyDark(u.id())) { // On préserve au moins 500 lorsque l'arbre est suffisamment avancé.
            if(g.darkLevelOf("dark_density")>=3 && g.state().darkMatter().lt(BigNum.of(700)))continue;
            g.buyDark(u.id());bought=true;break;}}
          if(!bought)break;}}
  void collider(){
    if(!g.isColliderUnlocked())return;
    for(int i=0;i<70;i++){
      boolean bought=false;
      for(ColliderGenerator gen:ColliderGenerator.values()){
        if(g.canBuyColliderGenerator(gen)){g.buyColliderGenerator(gen);bought=true;break;}
      }
      if(!bought)break;
    }
    for(int i=0;i<40;i++){
      boolean bought=false;
      for(ColliderUpgrade up:ColliderUpgrade.values())if(g.canBuyColliderUpgrade(up)){g.buyColliderUpgrade(up);bought=true;break;}
      if(!bought)break;
    }
    if(g.isCrossing() && g.canCatchCrossing())g.catchCrossing();
  }
  void probes(){if(!g.isProbesUnlocked())return;
    for(Probe p:new ArrayList<>(g.landedProbes()))g.claimAllProbeRewards(p.id());
    for(ProbeUpgrade u:ProbeUpgrade.values())if(g.canBuyProbeUpgrade(u))g.buyProbeUpgrade(u);
    for(ProbeDestination d:ProbeDestination.values()){
       if(g.canLaunchProbe(d,ProbeTrip.SHORT))g.launchProbe(d,ProbeTrip.SHORT);
    }
  }
  void systems(){
     if(!g.isSolarSystemsUnlocked())return;
     for(String star:new ArrayList<>(g.freeForSystems(Body.Tier.STAR)))g.foundSystem(star);
     for(int i=0;i<g.state().solarSystems().size();i++){
       for(String planet:new ArrayList<>(g.freeForSystems(Body.Tier.PLANET))){if(g.state().solarSystems().get(i).planets().size()>=2)break;g.addPlanet(i,planet);}
       for(String moon:new ArrayList<>(g.freeForSystems(Body.Tier.MOON))){if(g.state().solarSystems().get(i).moons().size()>=g.state().solarSystems().get(i).planets().size())break;g.addMoon(i,moon);}
       g.arrangeSystem(i);
     }
  }
  /** Vise explicitement les deux étoiles mortes indispensables au huitième Big Bang. */
  boolean preEighthBody(){
    if(g.bigBangs()!=7 || g.bodiesMature()>=28)return false;
    String id=!g.hasBody("white_dwarf")?"white_dwarf":(!g.hasBody("neutron_star")?"neutron_star":null);
    if(id==null)return false;
    Body body=g.body(id);
    if(g.canFormBody(id)){g.formBody(id);return true;}
    for(String aId:body.assemblies()){
      if(g.state().hasAssembly(aId))continue;
      Assembly a=g.assembly(aId);
      for(var required:a.ingredients().entrySet()){
        String m=required.getKey();
        long objective=(long)g.assembledMolecules(m)+required.getValue();
        for(int i=0;i<25 && g.moleculeCount(m)<objective && g.canCreateMolecule(m);i++)g.createMolecule(m);
        if(g.canFormSubstance(m))g.formSubstance(m);
      }
      if(g.canFormAssembly(aId))g.formAssembly(aId);
    }
    for(var required:body.molecules().entrySet()){
      String m=required.getKey();
      for(int i=0;i<25 && g.moleculeCount(m)<required.getValue() && g.canCreateMolecule(m);i++)g.createMolecule(m);
      if(g.canFormSubstance(m))g.formSubstance(m);
    }
    for(var required:body.matter().entrySet()){
      var matter=required.getKey();long objective=required.getValue();
      if(g.gatheredInState(matter)>=objective)continue;
      Molecule cheapest=null;double min=Double.POSITIVE_INFINITY;
      for(Molecule m:g.molecules()) if(m.hasState()&&m.state()==matter&&g.isMoleculeKindUnlocked(m.kind())&&g.isMoleculeWithinReach(m.id())&&g.canCreateMolecule(m.id())){
          double v=g.moleculeVolume(m.id()).toDouble();
          if(g.hasSubstance(m.id()) && v<min){cheapest=m;min=v;}
      }
      if(cheapest==null){
        for(Molecule m:g.molecules()) if(m.hasState()&&m.state()==matter&&g.isMoleculeKindUnlocked(m.kind())&&g.isMoleculeWithinReach(m.id())&&g.canCreateMolecule(m.id())){
            double v=g.moleculeVolume(m.id()).toDouble();if(v<min){cheapest=m;min=v;}
        }
        if(cheapest!=null){for(int k=0;k<4&&g.moleculeCount(cheapest.id())<3&&g.canCreateMolecule(cheapest.id());k++)g.createMolecule(cheapest.id());
          if(g.canFormSubstance(cheapest.id()))g.formSubstance(cheapest.id());}
      }
      if(cheapest!=null&&g.hasSubstance(cheapest.id())){
        for(int i=0;i<25&&g.gatheredInState(matter)<objective&&g.canCreateMolecule(cheapest.id());i++)g.createMolecule(cheapest.id());
      }
    }
    if(g.canFormBody(id))g.formBody(id);
    return true;
  }
  void cosmos(){
    for(SpaceUpgrade up:g.spaceUpgrades())if(g.canBuySpaceUpgrade(up.id()))g.buySpaceUpgrade(up.id());
    g.setAutoFollowList(true);
    g.setAutoMoleculeReserve(0.10);
    // L'automatisation générale mange tout l'espace : on contrôle nous-mêmes les créations.
    g.setMoleculeAutomationEnabled(false);
    g.setAutoGatherEnabled(true);
    g.setAutoFormEnabled(true);
    for(int i=0;i<5;i++){
       int s=g.formAllSubstances(),a=g.formAllAssemblies(),b=g.formAllBodies();
       if(s+a+b==0)break;
    }
    if(g.nextCosmos()!=null&&g.canFormCosmos(g.nextCosmos())){Cosmos c=g.nextCosmos();if(g.formCosmos(c))System.out.printf("COSMOS_DONE,%.4f,%s%n",elapsed/3600,c);}
    systems();
    // Objectif spécial de huitième Big Bang : contourne la liste qui privilégie les astres exotiques verrouillés.
    if(preEighthBody()){systems();return;}
    // Liste intégrée au moteur : besoins exacts du prochain assemblage / astre.
    ShoppingList wish=g.shoppingList();
    int attempts=0,creates=0;
    for(ShoppingList.Item item:wish.items()){
       String id=item.molecule().id();
       int needed=Math.max(item.needed(),item.gather()?3:0);
       for(int k=0;k<16 && g.moleculeCount(id)<needed && g.canCreateMolecule(id);k++){
         if(g.createMolecule(id)){creates++;} else break;
       }
       if(item.gather() && g.canFormSubstance(id))g.formSubstance(id);
    }
    // Pour des déficits de matière / diversité, privilégier les molécules peu volumineuses.
    for(var e:wish.sorts().entrySet()){
       List<Molecule> options=new ArrayList<>();
       for(Molecule m:g.molecules())if(m.hasState()&&m.state()==e.getKey()&&g.isMoleculeKindUnlocked(m.kind())&&!g.hasSubstance(m.id()))options.add(m);
       options.sort(Comparator.comparingDouble(m->g.moleculeVolume(m.id()).toDouble()));
       int left=e.getValue();
       for(Molecule m:options){if(left<=0)break;
         for(int i=0;i<4&&g.moleculeCount(m.id())<3&&g.canCreateMolecule(m.id());i++)g.createMolecule(m.id());
         if(g.canFormSubstance(m.id())){g.formSubstance(m.id());left--;}
       }
    }
    for(var e:wish.matter().entrySet()){
      long target=g.gatheredInState(e.getKey())+e.getValue(); // shoppingList.matter contient le MANQUE, pas le seuil absolu
      Molecule choice=null;double min=Double.POSITIVE_INFINITY;
      for(Molecule m:g.molecules())if(m.hasState()&&m.state()==e.getKey()&&g.hasSubstance(m.id())&&g.canCreateMolecule(m.id())){
        double v=g.moleculeVolume(m.id()).toDouble();if(v<min){min=v;choice=m;}
      }
      if(choice==null){ // Aucun amas rassemblé dans cet état : en ouvrir un avant de produire en masse.
        Molecule starter=null;double cheapest=Double.POSITIVE_INFINITY;
        for(Molecule m:g.molecules())if(m.hasState() && m.state()==e.getKey() && g.isMoleculeKindUnlocked(m.kind()) && !g.hasSubstance(m.id()) && g.isMoleculeWithinReach(m.id())){
          double v=g.moleculeVolume(m.id()).toDouble();if(v<cheapest){starter=m;cheapest=v;}
        }
        if(starter!=null){for(int i=0;i<5&&g.moleculeCount(starter.id())<3&&g.canCreateMolecule(starter.id());i++)g.createMolecule(starter.id());
            if(g.canFormSubstance(starter.id()))g.formSubstance(starter.id());
            if(g.hasSubstance(starter.id()))choice=starter;
        }
      }
      if(choice!=null){for(int i=0;i<18&&g.gatheredInState(e.getKey())<target&&g.canCreateMolecule(choice.id());i++)g.createMolecule(choice.id());}
    }
    // Amorçage : avant ouverture des assemblages, répartir les premiers achats entre sortes utiles.
    if(wish.kind()==ShoppingList.Kind.NONE){
       for(Molecule m:g.molecules()){
          if(!m.hasState() || !g.isMoleculeKindUnlocked(m.kind()) || g.hasSubstance(m.id()))continue;
          if(!g.isMoleculeWithinReach(m.id()))continue;
          for(int i=0;i<4&&g.moleculeCount(m.id())<3&&g.canCreateMolecule(m.id());i++)g.createMolecule(m.id());
          if(g.canFormSubstance(m.id()))g.formSubstance(m.id());
          if(++attempts>=12)break;
       }
    }
    if(creates==0 && g.isStatesUnlocked()){
       // Réserve de croissance : une molécule d'espace à bon rendement avant de tenter l'objectif suivant.
       Molecule best=g.bestMolecule();
       if(best!=null && g.freeSpace().toDouble()>g.shoppingSpace().toDouble()*1.5 && g.moleculeCount(best.id())<15)g.createMolecule(best.id());
    }
    for(int i=0;i<4;i++){
       int s=g.formAllSubstances(),a=g.formAllAssemblies(),b=g.formAllBodies();
       if(s+a+b==0)break;
    }
    systems();
  }
  void resourceLoop(){
    // Réduire les décisions pendant la phase de production pure.
    particle(g,500);
    boolean finalMode=g.completedChallenges()==g.challenges().size() && g.state().darkMatter().gte(BigNum.of(500)) && g.achievementCount()>=40 && g.productionPerSecond().gte(g.bigBangParticles());
    boolean stopFusion=finalMode && g.state().atoms().gte(Game.BIG_BANG_ATOMS);
    if(stopFusion){g.setAutomationEnabled("auto_fusion",false);g.setAutomationEnabled("auto_synthesis",false);}
    for(int i=0;i<5 && !stopFusion && g.canFuse();i++){g.fuse();if(!finalMode)atoms(g,50);particle(g,200);}
    for(Automation a:g.automations()){if(g.canBuyAutomation(a.id()))g.buyAutomation(a.id());if(g.canSpeedUpAutomation(a.id()))g.speedUpAutomation(a.id());}
    if(!finalMode)atoms(g,60);
    for(int i=0;i<20 && !finalMode && g.canSynthesize();i++)g.synthesize();
    if(g.state().explosions()>0)g.growDarkMatter(step);
    dark(g,50);
    // Défis : après préparation des 40 premières explosions.
    if(!g.inChallenge() && g.completedChallenges()<g.challenges().size() && g.state().explosions()>=40 && g.darkLevelOf("dark_density")>=3){
        for(Challenge c:g.challenges())if(g.isChallengeUnlocked(c.id())&&!g.isChallengeCompleted(c.id())){
           if(g.startChallenge(c.id())){ if(!abandoned && g.achievementCount()<40){g.abandonChallenge();abandoned=true;}break; }
        }
    }
    // Ne pas exploser avec une branche de densité incomplète : laisser les particules
    // croître jusqu'au coût astronomique du niveau 3.
    boolean prepareDensity=false; // ne pas figer le prestige à 40 explosions : il faut dépasser les seuils de coût du tronc.
    if(g.bigBangs()<8 && g.canExplode() && (g.inChallenge() || (!finalMode && !prepareDensity)))g.explode();
    if(g.canBigBang() && (g.bigBangs()<6 || (g.bigBangs()<8 && g.bodiesMature()>=g.bigBangSkyRequirement()))){
        double current=elapsed/3600;
        int old=g.bigBangs();
        if(g.bigBang()){System.out.printf("BIGBANG_DONE,%.4f,%d,%d%n",current,old,g.bigBangs());abandoned=true;}
    }
  }
  void snapshot(){
    String target=g.shoppingList().target().replace(',',';');
    String line=String.format(java.util.Locale.ROOT,"%.4f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%s,%s",
      (g.state().timePlayed())/3600,g.bigBangs(),g.state().explosions(),g.completedChallenges(),g.achievementCount(),g.discoveredElements(),g.moleculesCreated(),
      g.substancesFormed(),g.assembliesFormed(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),g.cosmosFormed(),g.state().space(),g.freeSpace(),g.spacePerSecond(),target);
    csv.add(line);System.out.println("HOUR,"+line);
  }
  void run(double hours) throws Exception {
    Files.createDirectories(OUTPUT_DIR);
    int max=(int)Math.ceil(hours*3600/step);
    for(int i=1;i<=max;i++){
      elapsed+=step;tickCount++;
      g.tick(step);
      if(g.isCometVisible())g.catchComet();
      if(g.isPulseUnlocked())for(int c=0;c<g.pulseCharges();c++)g.pulse();
      if(g.state().explosions()>0 || g.isBigBangUnlocked()){
        collider();
      }
      probes();
      cosmos();
      resourceLoop();
      if(g.bodiesFormed()!=prevBodies || g.assembliesFormed()!=prevAssemblies || g.cosmosFormed()!=prevCosmos || g.bodiesMature()!=prevMature){
         if(g.bodiesFormed()!=prevBodies)System.out.printf("BODY,%.4f,%d,%s%n",g.state().timePlayed()/3600,g.bodiesFormed(),g.nextBody()==null?"-":g.nextBody().id());
         if(g.assembliesFormed()!=prevAssemblies)System.out.printf("ASSEMBLY,%.4f,%d%n",g.state().timePlayed()/3600,g.assembliesFormed());
         prevBodies=g.bodiesFormed();prevAssemblies=g.assembliesFormed();prevCosmos=g.cosmosFormed();prevMature=g.bodiesMature();
      }
      if(i%(int)Math.max(1,Math.round(3600/step))==0){snapshot();Files.writeString(OUTPUT_DIR.resolve("cosmic_planner_checkpoint.save"),SaveCodec.write(g.state(),0));Files.write(OUTPUT_DIR.resolve("cosmic_planner.csv"),csv);}
      if(g.hasCosmos(Cosmos.UNIVERSE)){System.out.println("UNIVERSE_DONE,"+g.state().timePlayed()/3600);snapshot();Files.writeString(OUTPUT_DIR.resolve("checkpoint_universe_finished.save"),SaveCodec.write(g.state(),0));break;}
    }
    Files.write(OUTPUT_DIR.resolve("cosmic_planner.csv"),csv);
    System.out.println("FINISHED,bangs="+g.bigBangs()+",molecules="+g.moleculesCreated()+",bodies="+g.bodiesFormed()+",cosmos="+g.cosmosFormed());
  }
  public static void main(String[] args) throws Exception {
    Path checkpoint=Path.of(args.length>0?args[0]:"/mnt/data/idle_analysis/checkpoint_bigbang.save");
    GameState state=SaveCodec.read(Files.readString(checkpoint)).state();
    Game game=new Game(state,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(42));
    System.out.printf("LOADED,bb=%d,space=%s,time=%.3f,cosmos=%d%n",game.bigBangs(),game.state().space(),game.state().timePlayed()/3600,game.cosmosFormed());
    new RobotCosmosPlanner(game,args.length>2?Double.parseDouble(args[2]):10).run(args.length>1?Double.parseDouble(args[1]):20);
  }
}
