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

/** Experience V2: huit agents, retraits de mecaniques, journal d'evenements et mesures de stagnation.
 * Les robots sont des politiques experimentales, PAS des utilisateurs mesures.
 */
public class AdvancedLab extends RobotCosmosPlanner {
  static final String[] MODES={"ECONOMISTE","PLAN_PRESTIGE","DEPENDANCES","ADAPTATIF","EXPERT_COMPLET","DEBUTANT_APPRENANT","GUIDE_UI","RECUPERATION", "SANS_IMPULSIONS","SANS_COMETES","SANS_TECH","SANS_LISTE"};
  final String mode;
  final Random policyRandom;
  final ArrayList<String> rows =new ArrayList<>();
  final ArrayList<String> events =new ArrayList<>();
  double wall=0, lastMilestone=0, lastUseful=0, maxStall=0, idleMinutes=0;
  int acts=0, meaningful=0, failed=0, extras=0, caught=0, pulses=0, collisionRuns=0, probeRuns=0;
  int prevB,prevA,prevM,prevT,prevS,prevC,prevBl,prevExpl;
  long prevMol;
  String oldTarget="";
  double reachedGalaxy=Double.NaN,reachedCluster=Double.NaN,reachedUniverse=Double.NaN,reachedB8=Double.NaN;
  AdvancedLab(Game g,double dt,String mode,long seed){
    super(g,dt);this.mode=mode;policyRandom=new Random(seed ^ 0x71abcL);
    // Version 0.131 : le débutant joue le deuxième univers en débutant ; les autres modes, en experts.
    if(mode.equals("DEBUTANT_APPRENANT")){
      nextUniverse=new NextUniversePolicy(IdleProfiles24.STYLES[15],new Random(seed ^ 0x2b2L));
      voidPolicy=new VoidPolicy(IdleProfiles24.VOID_STYLES[15],new Random(seed ^ 0x701dL));
    }
    prevB=g.bodiesMature();prevA=g.assembliesFormed();prevM=g.moleculeKindsCreated();prevT=g.substancesFormed();
    prevS=g.solarSystemsComplete();prevC=RobotCosmosPlanner.cosmosStage(g);prevBl=g.bigBangs();prevExpl=g.state().explosions();prevMol=g.moleculesCreated();
    rows.add("wall_h,game_h,bb,explosions,parts_log,atoms_log,space,space_per_s,space_free,molecules,sortes,substances,assemblages,astres_murs,systemes,cosmos,objectif,decisions,interventions_inutiles,stagnation_h");
    events.add("wall_h,event,detail");
  }
  /** Les relevés des univers se comptent en temps réel, comme le reste du bilan. */
  @Override double clockSeconds(){return wall;}
  @Override
  boolean waitForPatientFusion(){
    return mode.equals("EXPERT_COMPLET")
        && !g.hasAchievement(idle.core.achievements.Achievements.PATIENT_FUSION)
        && !g.inChallenge()
        && g.state().timeSinceFusion()<idle.core.achievements.Achievements.PATIENT_SECONDS;
  }
  boolean getsPlanning(){
    switch(mode){
      case "DEBUTANT_APPRENANT":return policyRandom.nextDouble()<Math.min(.95,.28+.10*(wall/3600));
      case "ADAPTATIF":return policyRandom.nextDouble()<(wall-lastMilestone>3600?.98:.75);
      case "PLAN_PRESTIGE":return policyRandom.nextDouble()<.82;
      case "RECUPERATION":return policyRandom.nextDouble()<(wall-lastMilestone>2700?.99:.68);
      case "SANS_LISTE":return policyRandom.nextDouble()<.28;
      default:return true;
    }
  }
  void extraChemistry(){
    Molecule best=g.bestMolecule();
    if(best!=null && g.canCreateMolecule(best.id()) && g.freeSpace().toDouble()>0.0){
      if(g.createMolecule(best.id()))extras++;
    }
  }
  void act(){
    acts++;
    if(!mode.equals("SANS_COMETES") && g.isCometVisible() && g.catchComet())caught++;
    // Version 0.131 : les anomalies du ciel, comme les comètes (sauf dans le mode qui les ignore).
    if(!mode.equals("SANS_COMETES"))catchSkyEvent();
    if(!mode.equals("SANS_IMPULSIONS") && g.isPulseUnlocked()){
      int toUse=mode.equals("DEBUTANT_APPRENANT")?Math.min(2,g.pulseCharges()):g.pulseCharges();
      for(int i=0;i<toUse;i++)if(g.pulse())pulses++;
    }
    if(!mode.equals("SANS_TECH") && (g.state().explosions()>0 || g.isBigBangUnlocked())){collider();collisionRuns++;}
    if(!mode.equals("SANS_TECH")){probes();probeRuns++;}
    if(getsPlanning()){
       if(mode.equals("EXPERT_COMPLET")||mode.equals("DEPENDANCES")){cosmos();cosmos();}
       else if(mode.equals("ECONOMISTE")){cosmos();extraChemistry();}
       else if(mode.equals("SANS_LISTE")){
         // Le profil sans liste ne doit pas utiliser la liste automatiquement non plus.
         g.setAutoFollowList(false);
         if(g.isAutoFollowingMatter())g.setAutoFollowMatter(false);
         g.setMoleculeAutomationEnabled(true);
         for(Molecule m:g.molecules())if(g.canCreateMolecule(m.id())){g.createMolecule(m.id());break;}
         g.formAllSubstances();g.formAllAssemblies();g.formAllBodies();
       } else cosmos();
    }
    if(mode.equals("ECONOMISTE")){g.setAutoMoleculeReserve(0.25);}
    if(mode.equals("PLAN_PRESTIGE")){resourceLoop();if(g.canExplode() && !FirstUniversePolicy.holdExplosions(g))g.explode();}
    else if(mode.equals("DEBUTANT_APPRENANT")){if(policyRandom.nextDouble()<Math.min(1,.35+wall/144000))resourceLoop();}
    else if(mode.equals("EXPERT_COMPLET")){resourceLoop();resourceLoop();}
    else if(mode.equals("RECUPERATION")){resourceLoop();if(wall-lastMilestone>3600){cosmos();extraChemistry();}}
    else resourceLoop();
  }
  void event(String type,String detail){
    events.add(String.format(Locale.ROOT,"%.5f,%s,%s",wall/3600,type,detail.replace(',',';')));
    lastMilestone=wall; meaningful++;
  }
  void captureEvents(){
    if(g.bigBangs()!=prevBl){event("BIG_BANG",String.valueOf(g.bigBangs()));if(g.bigBangs()>=8&&Double.isNaN(reachedB8))reachedB8=wall/3600;}
    if(g.state().explosions()!=prevExpl)event("EXPLOSION",String.valueOf(g.state().explosions()));
    if(g.bodiesMature()!=prevB)event("ASTRES_MURS",String.valueOf(g.bodiesMature()));
    if(g.assembliesFormed()!=prevA)event("ASSEMBLAGES",String.valueOf(g.assembliesFormed()));
    if(g.substancesFormed()!=prevT)event("SUBSTANCES",String.valueOf(g.substancesFormed()));
    if(g.solarSystemsComplete()!=prevS)event("SYSTEMES",String.valueOf(g.solarSystemsComplete()));
    if(RobotCosmosPlanner.cosmosStage(g)!=prevC){event("COSMOS",String.valueOf(RobotCosmosPlanner.cosmosStage(g)));if(RobotCosmosPlanner.cosmosStage(g)>=1&&Double.isNaN(reachedGalaxy))reachedGalaxy=wall/3600;if(RobotCosmosPlanner.cosmosStage(g)>=2&&Double.isNaN(reachedCluster))reachedCluster=wall/3600;if(RobotCosmosPlanner.cosmosStage(g)>=3&&Double.isNaN(reachedUniverse))reachedUniverse=wall/3600;}
    rememberState();
  }
  /** L'état relevé, auquel le pas suivant se compare. */
  void rememberState(){
    prevBl=g.bigBangs();prevExpl=g.state().explosions();prevB=g.bodiesMature();prevA=g.assembliesFormed();prevT=g.substancesFormed();prevS=g.solarSystemsComplete();prevC=RobotCosmosPlanner.cosmosStage(g);prevM=g.moleculeKindsCreated();prevMol=g.moleculesCreated();
  }
  void snapshot(){
    String wish=g.shoppingList().target().replace(',',';').replace('\n',' ');
    rows.add(String.format(Locale.ROOT,"%.5f,%.5f,%d,%d,%.4f,%.4f,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%s,%d,%d,%.3f",wall/3600,g.state().timePlayed()/3600,g.bigBangs(),g.state().explosions(),g.state().particles().log10(),g.state().atoms().log10(),g.state().space(),g.spacePerSecond(),g.freeSpace(),g.moleculesCreated(),g.moleculeKindsCreated(),g.substancesFormed(),g.assembliesFormed(),g.bodiesMature(),g.solarSystemsComplete(),RobotCosmosPlanner.cosmosStage(g),wish,acts,failed,(wall-lastMilestone)/3600));
  }
  void runLab(double horizon,Path out) throws Exception{
    Files.createDirectories(out);
    long ticks=(long)Math.ceil(horizon*3600/step);int endBB=0,endMature=0;
    for(long i=1;i<=ticks;i++){
      wall+=step;g.tick(step);g.buyAllBigBangMilestones();
      if(i%Math.max(1,(long)Math.round(20/step))==0){
        int stage=RobotCosmosPlanner.cosmosStage(g);int bb=g.bigBangs();int mature=g.bodiesMature();int kind=g.moleculeKindsCreated();long mol=g.moleculesCreated();
        endBB=bb;endMature=mature;
        act(); // former l'univers remet tout à zéro (0.83) : le bilan garde endBB et endMature, l'état d'avant
        if(stage==RobotCosmosPlanner.cosmosStage(g) && bb==g.bigBangs() && mature==g.bodiesMature() && kind==g.moleculeKindsCreated() && mol==g.moleculesCreated())failed++;
      }
      captureEvents();
      maxStall=Math.max(maxStall,wall-lastMilestone);
      if(wall-lastMilestone>=1800)idleMinutes+=step/60;
      if(i%Math.max(1,(long)Math.round(600/step))==0)snapshot();
      // Version 0.130 : l'univers visé atteint, la partie s'arrête ; avant, l'univers suivant se lance.
      boolean done=universeProgress();
      // La remise à zéro de l'univers suivant n'est pas un recul : le pas suivant se compare à l'état neuf.
      if(launchedNextUniverse){event("UNIVERS_SUIVANT",String.valueOf(g.universes()+1));rememberState();}
      if(done)break;
    }
    snapshot();
    Files.write(out.resolve("timeline.csv"),rows);
    Files.write(out.resolve("events.csv"),events);
    boolean won=NextUniversePolicy.reached(g,targetUniverses);
    if(won)Files.writeString(out.resolve("winner.save"),SaveCodec.write(g.state(),0));
    String res=String.format(Locale.ROOT,"%s,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%d,%d,%d,%.5f,%.5f,%d,%d,%d,%d,%d,%d,%d,%s,%d",mode,won?1:0,wall/3600,reachedB8,reachedGalaxy,reachedCluster,reachedUniverse,acts,meaningful,failed,maxStall/3600,idleMinutes/60,g.hasCosmos(Cosmos.UNIVERSE)?endBB:g.bigBangs(),g.hasCosmos(Cosmos.UNIVERSE)?endMature:g.bodiesMature(),RobotCosmosPlanner.cosmosStage(g),caught,pulses,extras,g.universes(),universeHours(),skyEventsCaught);
    Files.writeString(out.resolve("result.csv"),"robot,victoire,heures,bb8_h,galaxie_h,amas_h,univers_h,decisions,evenements,sans_progres,plus_longue_stagnation_h,temps_stagnation_30min_h,bb,astres,cosmos,cometes,impulsions,meilleurs_achats,univers_formes,univers_h,anomalies\n"+res+"\n");
    System.out.println("RESULT,"+res);
  }
  public static void main(String[]args)throws Exception{
    if(args.length<6){System.err.println("Usage: mode checkpoint hours step seed output [univers]");System.exit(2);}
    String mode=args[0];Path cp=Path.of(args[1]);double hours=Double.parseDouble(args[2]),dt=Double.parseDouble(args[3]);long seed=Long.parseLong(args[4]);
    GameState st=SaveCodec.read(Files.readString(cp)).state();
    Game g=new Game(st,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(seed));
    System.out.printf(Locale.ROOT,"START,%s,%.3f,%d%n",mode,st.timePlayed()/3600,g.bigBangs());
    AdvancedLab lab=new AdvancedLab(g,dt,mode,seed);
    if(args.length>6)lab.targetUniverses=Integer.parseInt(args[6]);
    lab.runLab(hours,Path.of(args[5]));
  }
}
