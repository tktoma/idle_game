import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.math.*;
import idle.core.molecules.*;
import idle.core.sky.*;
import idle.core.collider.*;
import idle.core.probes.*;
import idle.core.save.*;
import idle.core.universe.CycleChoice;
import java.util.*;
import java.nio.file.*;
public class IdleProfiles24 extends RobotCosmosPlanner {
record Profile(int id, String name, String label, int actionEverySeconds,
int cycleSeconds, int onlineSeconds, double attention, double planning,
double ordinaryActions, double techAttention, double skyAttention,
int shopMode, String bias) {}
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
/**
 * La façon de jouer le deuxième univers de chaque profil, dans l'ordre de {@link #PROFILES} (version 0.131) : choix de
 * cycle (null : au hasard), chance de relancer le régime rapide, de faire le choix de cycle, ce qu'il fait de la famille
 * préférée, chance de prendre les éléments offerts, et lequel.
 */
static final NextUniversePolicy.Style[] STYLES = {
NextUniversePolicy.Style.EXPERT,                                                                           //  1 Speedrunner : l'expert
style(CycleChoice.RESERVE,   .95,1.00,NextUniversePolicy.Family.FOLLOW,1.00,NextUniversePolicy.Pick.NEEDED), //  2 Veteran : presque l'expert
style(CycleChoice.RESERVE,   .70, .90,NextUniversePolicy.Family.FOLLOW, .90,NextUniversePolicy.Pick.NEEDED), //  3 Regular : le bon choix, pas toujours le geste
style(CycleChoice.RESERVE,   .90,1.00,NextUniversePolicy.Family.FOLLOW,1.00,NextUniversePolicy.Pick.NEEDED), //  4 Methodical : l'économe planifie et met en réserve
style(CycleChoice.FUSION,    .80, .90,NextUniversePolicy.Family.FOLLOW, .90,NextUniversePolicy.Pick.NEEDED), //  5 AchievementHunter : attentif, sans tout optimiser
style(CycleChoice.SYNTHESIS, .60, .90,NextUniversePolicy.Family.FOLLOW,1.00,NextUniversePolicy.Pick.NEEDED), //  6 Collector : des éléments pour ses molécules
style(CycleChoice.SYNTHESIS, .80,1.00,NextUniversePolicy.Family.FOLLOW,1.00,NextUniversePolicy.Pick.NEEDED), //  7 Chemist : la synthèse
style(CycleChoice.SYNTHESIS, .60, .90,NextUniversePolicy.Family.ONCE,   .90,NextUniversePolicy.Pick.NEEDED), //  8 Astronomer : les molécules du ciel
style(CycleChoice.FUSION,    .60, .90,NextUniversePolicy.Family.ONCE,   .90,NextUniversePolicy.Pick.NEEDED), //  9 Explorer : ses gestes vont aux sondes
style(CycleChoice.RESERVE,   .00, .90,NextUniversePolicy.Family.ONCE,   .90,NextUniversePolicy.Pick.NEEDED), // 10 PassiveOptimist : compte sur les automatismes, jamais de régime (geste répété à la main)
style(CycleChoice.RESERVE,   .80,1.00,NextUniversePolicy.Family.ONCE,   .90,NextUniversePolicy.Pick.NEEDED), // 11 PrestigeAddict : plus de matière noire par explosion
style(CycleChoice.RESERVE,   .40, .80,NextUniversePolicy.Family.ONCE,  1.00,NextUniversePolicy.Pick.NEEDED), // 12 Hoarder : la réserve ; ce qui est gratuit, il le prend
style(CycleChoice.FUSION,    .50, .80,NextUniversePolicy.Family.ONCE,  1.00,NextUniversePolicy.Pick.NEEDED), // 13 BulkBuyer : passe rarement : le régime une fois sur deux
style(CycleChoice.FUSION,    .50, .80,NextUniversePolicy.Family.ONCE,   .80,NextUniversePolicy.Pick.FIRST),  // 14 OneClick : prend le premier élément de la liste
style(null,                  .45, .70,NextUniversePolicy.Family.NONE,   .70,NextUniversePolicy.Pick.RANDOM), // 15 RandomClicker : au hasard
style(CycleChoice.FUSION,    .00, .60,NextUniversePolicy.Family.NONE,   .60,NextUniversePolicy.Pick.FIRST),  // 16 Beginner : le premier choix, oublie le régime
style(CycleChoice.RESERVE,   .00,1.00,NextUniversePolicy.Family.ONCE,  1.00,NextUniversePolicy.Pick.NEEDED), // 17 AFKOpen : une visite par heure, le régime ne sert à rien
style(CycleChoice.RESERVE,   .10, .90,NextUniversePolicy.Family.ONCE,   .90,NextUniversePolicy.Pick.NEEDED), // 18 BackgroundWorker : une décision toutes les dix minutes : le régime, rarement
style(CycleChoice.FUSION,    .50, .90,NextUniversePolicy.Family.ONCE,   .90,NextUniversePolicy.Pick.NEEDED), // 19 MobileSnacker : cinq minutes par heure
style(CycleChoice.RESERVE,   .60, .90,NextUniversePolicy.Family.FOLLOW, .90,NextUniversePolicy.Pick.NEEDED), // 20 LunchBreak : des sessions de trente minutes bien remplies
style(CycleChoice.RESERVE,   .90,1.00,NextUniversePolicy.Family.FOLLOW,1.00,NextUniversePolicy.Pick.NEEDED), // 21 EveningPlayer : deux heures d'expert par jour
style(CycleChoice.RESERVE,   .70, .90,NextUniversePolicy.Family.FOLLOW, .90,NextUniversePolicy.Pick.NEEDED), // 22 WeekendPlayer : longues sessions, un peu moins appliqué
style(CycleChoice.SYNTHESIS, .20, .50,NextUniversePolicy.Family.NONE,   .50,NextUniversePolicy.Pick.FIRST),  // 23 Forgetful : oublie souvent
style(CycleChoice.RESERVE,  1.00,1.00,NextUniversePolicy.Family.FOLLOW,1.00,NextUniversePolicy.Pick.NEEDED), // 24 ReturningVeteran : l'expert, peu présent
};
/**
 * La façon de jouer le vide (troisième univers) de chaque profil, dans l'ordre de {@link #PROFILES} (version 0.133) :
 * chance de l'ouvrir dès qu'il est payable, part laissée au vide, plan d'achat, chance d'y passer pour acheter,
 * chance de nourrir le trou noir.
 */
static final VoidPolicy.Style[] VOID_STYLES = {
VoidPolicy.Style.EXPERT,                             //  1 Speedrunner : l'expert
vstyle(1.00, .50,VoidPolicy.Plan.BEST,     .95,1.00), //  2 Veteran : presque l'expert
vstyle( .90, .25,VoidPolicy.Plan.BEST,     .70, .80), //  3 Regular : garde le réglage du jeu
vstyle(1.00, .50,VoidPolicy.Plan.BEST,     .90,1.00), //  4 Methodical : planifie
vstyle( .90, .25,VoidPolicy.Plan.CHEAPEST, .80,1.00), //  5 AchievementHunter : achète tout ce qui s'offre
vstyle( .90, .25,VoidPolicy.Plan.CHEAPEST, .80, .80), //  6 Collector : le moins cher, pour tout avoir
vstyle( .90, .25,VoidPolicy.Plan.BEST,     .80, .80), //  7 Chemist
vstyle(1.00, .50,VoidPolicy.Plan.BEST,     .90,1.00), //  8 Astronomer : ce qui raccourcit le ciel d'abord
vstyle(1.00, .75,VoidPolicy.Plan.BEST,     .90, .90), //  9 Explorer : le collisionneur et le vide sont son terrain
vstyle( .80, .25,VoidPolicy.Plan.CHEAPEST, .50, .30), // 10 PassiveOptimist : peu de gestes
vstyle(1.00, .50,VoidPolicy.Plan.CHEAPEST, .80,1.00), // 11 PrestigeAddict
vstyle( .50, .25,VoidPolicy.Plan.BEST,     .30, .00), // 12 Hoarder : hésite à payer, ne sacrifie jamais ses particules
vstyle( .80, .25,VoidPolicy.Plan.CHEAPEST, .50, .50), // 13 BulkBuyer : de grosses séries, rarement
vstyle( .70, .25,VoidPolicy.Plan.CHEAPEST, .60, .50), // 14 OneClick
vstyle( .60, .25,VoidPolicy.Plan.RANDOM,   .60, .50), // 15 RandomClicker : au hasard
vstyle( .50, .25,VoidPolicy.Plan.CHEAPEST, .50, .20), // 16 Beginner : le premier venu
vstyle(1.00, .50,VoidPolicy.Plan.BEST,    1.00,1.00), // 17 AFKOpen : une visite par heure, mais complète
vstyle( .90, .50,VoidPolicy.Plan.BEST,     .80, .80), // 18 BackgroundWorker
vstyle( .90, .25,VoidPolicy.Plan.CHEAPEST, .90, .80), // 19 MobileSnacker : vite, le moins cher
vstyle( .90, .50,VoidPolicy.Plan.BEST,     .90, .90), // 20 LunchBreak
vstyle(1.00, .50,VoidPolicy.Plan.BEST,    1.00,1.00), // 21 EveningPlayer
vstyle( .90, .50,VoidPolicy.Plan.BEST,     .90, .90), // 22 WeekendPlayer
vstyle( .40, .25,VoidPolicy.Plan.CHEAPEST, .40, .30), // 23 Forgetful : oublie souvent
vstyle(1.00, .50,VoidPolicy.Plan.BEST,    1.00,1.00), // 24 ReturningVeteran : l'expert, peu présent
};
/**
 * Les choix de jeu des trois actes de chaque profil, dans l'ordre de {@link #PROFILES} (version 0.135) : suit l'ordre
 * d'achat du modèle, l'arbre de matière noire au mieux, tente les défis de Big Bang, règle ses sondes sur son absence.
 */
static final EarlyStyle[] EARLY_STYLES = {
EarlyStyle.EXPERT,              //  1 Speedrunner : l'expert, toujours là
early(true, true, true, true ), //  2 Veteran
early(true, true, true, true ), //  3 Regular
early(true, true, true, true ), //  4 Methodical
early(true, true, true, true ), //  5 AchievementHunter : les défis de Big Bang donnent des succès
early(true, false,true, true ), //  6 Collector : prend toutes les cases, la moins chère d'abord
early(true, true, true, true ), //  7 Chemist
early(true, true, true, true ), //  8 Astronomer
early(true, true, true, true ), //  9 Explorer : ses sondes partent jusqu'à son retour
early(false,false,false,true ), // 10 PassiveOptimist : compte sur les automatismes, pas d'effort de plus
early(true, true, true, true ), // 11 PrestigeAddict
early(true, true, false,true ), // 12 Hoarder : ne risque pas un défi de Big Bang
early(false,false,true, true ), // 13 BulkBuyer : achète en séries, le moins cher d'abord
early(false,false,false,false), // 14 OneClick : un achat, le moins cher
early(false,false,false,false), // 15 RandomClicker
early(false,false,false,false), // 16 Beginner : ne connaît ni l'ordre d'achat ni les défis de Big Bang
early(true, true, true, true ), // 17 AFKOpen : planifie bien, une visite par heure
early(true, true, true, true ), // 18 BackgroundWorker
early(true, true, true, true ), // 19 MobileSnacker : sondes d'une heure avant de fermer le jeu
early(true, true, true, true ), // 20 LunchBreak
early(true, true, true, true ), // 21 EveningPlayer : sondes en expédition pour la nuit
early(true, true, true, true ), // 22 WeekendPlayer
early(false,false,false,false), // 23 Forgetful : oublie l'ordre et les sondes
early(true, true, true, true ), // 24 ReturningVeteran : l'expert, peu présent
};
private static EarlyStyle early(boolean opening,boolean darkBest,boolean bangChallenges,boolean longTrips){
return new EarlyStyle(opening,darkBest,bangChallenges,longTrips);
}
private static VoidPolicy.Style vstyle(double open,double share,VoidPolicy.Plan plan,double buyCare,double sacrifice){
return new VoidPolicy.Style(open,share,plan,buyCare,sacrifice);
}
private static NextUniversePolicy.Style style(CycleChoice cycle,double regime,double cycleCare,NextUniversePolicy.Family family,double freeCare,NextUniversePolicy.Pick pick){
return new NextUniversePolicy.Style(cycle,regime,cycleCare,family,freeCare,pick);
}
final Profile p;
final Random rng;
/** Le hasard des anomalies du ciel, à part : il ne déplace pas les autres tirages du profil. */
final Random skyRng;
double wallSeconds=0, onlineSeconds=0, creditedOfflineSeconds=0;
int actions=0, planningActions=0, techActions=0, otherActions=0;
int first8=-1, galaxy=-1, cluster=-1, universe=-1;
int lastBang, lastCosmos, lastExplosions;
final ProfileProgressGuard progressGuard;
double nextGuardSampleAt=ProfileProgressGuard.SAMPLE_SECONDS;
IdleProfiles24(Game game, Profile profile, long seed, double decisionStep){
super(game,decisionStep);this.p=profile;this.rng=new Random(seed ^ (profile.id*11939L));
this.skyRng=new Random(seed ^ (profile.id*7907L) ^ 0x5c1L);
// Version 0.131 : chaque profil joue le deuxième univers à sa façon, avec son propre hasard.
this.nextUniverse=new NextUniversePolicy(STYLES[profile.id-1],new Random(seed ^ (profile.id*3571L) ^ 0x2b2L));
// Version 0.135 : ses choix dans les trois actes de chaque univers.
this.earlyStyle=EARLY_STYLES[profile.id-1];
// Version 0.133 : et le vide du troisième univers, à sa façon aussi.
this.voidPolicy=new VoidPolicy(VOID_STYLES[profile.id-1],new Random(seed ^ (profile.id*6151L) ^ 0x701dL));
lastBang=game.bigBangs();lastCosmos=RobotCosmosPlanner.cosmosStage(game);lastExplosions=game.state().explosions();
progressGuard=new ProfileProgressGuard(profile);
}
/** Les relevés des univers se comptent en temps réel, comme le reste du bilan des profils. */
@Override double clockSeconds(){return wallSeconds;}
/**
 * Le temps jusqu'au prochain retour du joueur : s'il va fermer le jeu avant sa prochaine décision, toute son absence ;
 * sinon, le temps jusqu'à sa prochaine décision.
 */
@Override double timeUntilBack(){
double next=Math.max(1,p.actionEverySeconds());
if(p.cycleSeconds()==0)return next;
double left=p.onlineSeconds()-(wallSeconds%p.cycleSeconds());
return left<=next+step ? left+(p.cycleSeconds()-p.onlineSeconds()) : next;
}
@Override void probes(){ if(rng.nextDouble()<p.techAttention()) { super.probes(); techActions++; } }
@Override void collider(){ if(rng.nextDouble()<p.techAttention()) super.collider(); }
static boolean allowExtraPrestige(Game game){
return game.bigBangs()<Bots.LAST_BIG_BANG
&& !game.canBigBang()
&& !FirstUniversePolicy.finalBangPreparation(game)
&& !FirstUniversePolicy.holdExplosions(game)
&& FirstUniversePolicy.skyReady(game)
&& !game.inChallenge()
&& game.activeBangChallenge()==null;
}
@Override void cosmos(){
if(rng.nextDouble()>=p.planning())return;
planningActions++;
if(p.bias().equals("collect") || p.bias().equals("random")){
int max=p.bias().equals("collect")?12:8;
int count=0;
// Version 0.135 : hors de la liste, seulement avec de l'espace à revendre. Avant, le collectionneur remplissait tout
// l'espace de molécules hors recette : dès le premier astre, la liste n'avait plus la place de créer les siennes
// (un amas glacé attendu 150 h, sans fin).
for(Molecule m:g.molecules()){
if(++count>max)break;
if(g.freeSpace().lte(g.shoppingSpace().multiply(2).add(g.moleculeNextSpace(m.id()))))continue;
if(g.canCreateMolecule(m.id()))g.createMolecule(m.id());
}
}
if(p.bias().equals("tech") && planningActions%3==0){super.probes();super.collider();}
if(p.bias().equals("sky") && planningActions%4==0){systems();preEighthBody();}
if(p.bias().equals("random") && rng.nextDouble()<.60)return;
if(p.shopMode()==1 && rng.nextDouble()<.22)return;
super.cosmos();
if(p.bias().equals("doubleplan")){
super.cosmos();
}else if(p.bias().equals("chemistry") && rng.nextDouble()<.50){
super.cosmos();
}
if(p.bias().equals("auto"))g.setMoleculeAutomationEnabled(true);
}
@Override void resourceLoop(){
if(rng.nextDouble()>p.ordinaryActions())return;
otherActions++;
if(p.shopMode()==1){
if(rng.nextDouble()<.35)return;
}
if(p.bias().equals("hoard") && rng.nextDouble()<.50){
if(allowExtraPrestige(g) && g.canExplode() && rng.nextDouble()<.2)g.explode();
return;
}
super.resourceLoop();
if(p.bias().equals("doubleplan"))super.resourceLoop();
if(p.bias().equals("prestige") && allowExtraPrestige(g) && g.canExplode())g.explode();
if(p.bias().equals("auto")){g.setMoleculeAutomationEnabled(true);g.setAutoGatherEnabled(true);g.setAutoFormEnabled(true);}
}
private boolean online(){
if(p.cycleSeconds()==0)return true;
return (wallSeconds % p.cycleSeconds()) < p.onlineSeconds();
}
private static String fmt(double d){return String.format(Locale.ROOT,"%.4f",d);}
private void event(String what){
System.out.printf(Locale.ROOT,"EVENT,%d,%s,%s,wall_h=%.4f,game_h=%.4f,bb=%d,bodies=%d,cosmos=%d%n",p.id(),p.name(),what,wallSeconds/3600,g.state().timePlayed()/3600,g.bigBangs(),g.bodiesFormed(),RobotCosmosPlanner.cosmosStage(g));
}
void execute(double horizonWallHours,Path output) throws Exception{
double dt=step;
int count=(int)Math.ceil(horizonWallHours*3600/dt);
double actionInterval=Math.max(1,p.actionEverySeconds());
double nextActionAt=Math.min(actionInterval,dt);
double startGameTime=g.state().timePlayed();
StringBuilder logs=new StringBuilder("wall_hours,game_hours,online_hours,bb,bodies,mature,systems,cosmos,particles_log,atoms_log,space,shop\n");
progressGuard.observe(ProfileProgressGuard.capture(g,wallSeconds,onlineSeconds,creditedOfflineSeconds,actions,online()));
for(int i=0;i<count;i++){
boolean isOnline=online();
if(isOnline){back();g.tick(dt);onlineSeconds+=dt;}
else creditedOfflineSeconds+=away(dt);
wallSeconds+=dt;
if(isOnline){
int opportunities=0;
int maxOpportunities=(int)Math.ceil(dt/actionInterval)+1;
while(wallSeconds+0.000001>=nextActionAt && opportunities++<maxOpportunities){
nextActionAt+=actionInterval;
if(rng.nextDouble()>=p.attention())continue;
actions++;
if(g.isCometVisible() && rng.nextDouble()<p.skyAttention())g.catchComet();
// Version 0.131 : l'anomalie du ciel, selon l'attention au ciel du profil.
if(g.skyEvent()!=null && !skyEventsOff() && skyRng.nextDouble()<p.skyAttention())catchSkyEvent();
if(g.isPulseUnlocked() && rng.nextDouble()<p.skyAttention()){
      int charges=g.pulseCharges();
      for(int k=0;k<charges;k++){
        // Pour les conditions 18/24/28 astres et tous les astres du catalogue (la galaxie),
        // une minute de maturation gagne davantage qu'une impulsion d'espace.
        boolean needsMaturation=g.bigBangs()>=Game.BANG_CHALLENGES_BIG_BANGS
            && (g.bigBangs()<Bots.LAST_BIG_BANG && !FirstUniversePolicy.skyReady(g)
                || g.bigBangs()>=Bots.LAST_BIG_BANG && g.bodiesMature()<g.bodies().size());
        if(needsMaturation && !g.maturingBodies().isEmpty()){
          if(!g.hastenMaturation(g.maturingBodies().get(0)))g.pulse();
        }else g.pulse();
      }
    }
if(g.state().explosions()>0 || g.isBigBangUnlocked())collider();
probes();cosmos();resourceLoop();
}
}else{
nextActionAt=Math.max(nextActionAt,wallSeconds+Math.min(actionInterval,dt));
}
// Les jalons de chaque univers sont annoncés ; BB8at, galaxyAt, clusterAt et universeAt gardent ceux du premier.
// Version 0.130 : la fin des deux premiers actes de chaque univers (première explosion, premier Big Bang).
if(lastExplosions==0 && g.state().explosions()>0 && g.bigBangs()==0)event("EXPLOSION1");
if(lastBang==0 && g.bigBangs()>0)event("BB1");
if(lastBang<Bots.LAST_BIG_BANG && g.bigBangs()>=Bots.LAST_BIG_BANG){if(first8<0)first8=i;event("BB8");}
if(lastCosmos<1 && RobotCosmosPlanner.cosmosStage(g)>=1){if(galaxy<0)galaxy=i;event("GALAXY");}
if(lastCosmos<2 && RobotCosmosPlanner.cosmosStage(g)>=2){if(cluster<0)cluster=i;event("CLUSTER");}
if(lastCosmos<3 && RobotCosmosPlanner.cosmosStage(g)>=3){if(universe<0)universe=i;event("UNIVERSE");}
// Version 0.130 : l'univers visé atteint, la partie s'arrête ; avant, l'univers suivant se lance.
boolean done=universeProgress();
if(launchedNextUniverse)event("NEXT_UNIVERSE");
lastBang=g.bigBangs();lastCosmos=RobotCosmosPlanner.cosmosStage(g);lastExplosions=g.state().explosions();
if(wallSeconds+0.000001>=nextGuardSampleAt || done){
progressGuard.observe(ProfileProgressGuard.capture(g,wallSeconds,onlineSeconds,creditedOfflineSeconds,actions,isOnline));
while(nextGuardSampleAt<=wallSeconds+0.000001)nextGuardSampleAt+=ProfileProgressGuard.SAMPLE_SECONDS;
}
if((i+1)%(int)Math.ceil(3600/dt)==0 || done){
logs.append(String.format(Locale.ROOT,"%.4f,%.4f,%.4f,%d,%d,%d,%d,%d,%.3f,%.3f,%s,%s%n",wallSeconds/3600,g.state().timePlayed()/3600,onlineSeconds/3600,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),RobotCosmosPlanner.cosmosStage(g),g.state().particles().log10(),g.state().atoms().log10(),g.state().space(),g.shoppingList().target().replace(',',';')));
}
if(done)break;
}
if(wallSeconds>progressGuard.lastWall()+0.000001)
progressGuard.observe(ProfileProgressGuard.capture(g,wallSeconds,onlineSeconds,creditedOfflineSeconds,actions,online()));
Files.createDirectories(output);
Files.writeString(output.resolve(String.format(Locale.ROOT,"%02d_%s_%s.csv",p.id(),p.name(),System.getProperty("idle.stage","late"))),logs.toString());
if(NextUniversePolicy.reached(g,targetUniverses))Files.writeString(output.resolve(String.format(Locale.ROOT,"%02d_%s_winner.save",p.id(),p.name())),SaveCodec.write(g.state(),0));
System.out.printf(Locale.ROOT,"RESULT,%d,%s,%s,wall=%.4f,game=%.4f,startgame=%.4f,online=%.4f,offlineCredited=%.4f,bb=%d,bodies=%d,mature=%d,systems=%d,cosmos=%d,actions=%d,plans=%d,tech=%d,ordinary=%d,BB8at=%s,galaxyAt=%s,clusterAt=%s,universeAt=%s,universes=%d,target=%d,won=%b,universeTimes=%s,regimes=%d,cycles=%d,families=%d,freeElements=%d,skyEvents=%d,voidOpened=%d,voidBought=%d,sacrifices=%d%n",p.id(),p.name(),p.bias(),wallSeconds/3600,g.state().timePlayed()/3600,startGameTime/3600,onlineSeconds/3600,creditedOfflineSeconds/3600,g.bigBangs(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),RobotCosmosPlanner.cosmosStage(g),actions,planningActions,techActions,otherActions,first8<0?"-":fmt((first8+1)*dt/3600),galaxy<0?"-":fmt((galaxy+1)*dt/3600),cluster<0?"-":fmt((cluster+1)*dt/3600),universe<0?"-":fmt((universe+1)*dt/3600),g.universes(),targetUniverses,NextUniversePolicy.reached(g,targetUniverses),universeHours(),nextUniverse.regimes,nextUniverse.cycles,nextUniverse.families,nextUniverse.freeElements,skyEventsCaught,voidPolicy.opened,voidPolicy.bought,voidPolicy.sacrifices);
}
public static void main(String[]args)throws Exception{
if(args.length<5){System.err.println("Usage: id  checkpoint  wallHours  stepSeconds outputDir [seed] [stage] [univers]");System.exit(2);}
int id=Integer.parseInt(args[0]);Profile p=PROFILES[id-1];
Path file=Path.of(args[1]);double hours=Double.parseDouble(args[2]),step=Double.parseDouble(args[3]);
Path out=Path.of(args[4]);long seed=args.length>5?Long.parseLong(args[5]):42;
if(args.length>6)System.setProperty("idle.stage",args[6]);
Game game=Bots.load(file,seed);
IdleProfiles24 bot=new IdleProfiles24(game,p,seed,step);
if(args.length>7)bot.targetUniverses=Integer.parseInt(args[7]);
bot.execute(hours,out);
}
}
