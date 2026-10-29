import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.bigbang.*;
import idle.core.math.*;
import idle.core.molecules.*;
import idle.core.sky.*;
import idle.core.collider.*;
import idle.core.probes.*;
import idle.core.achievements.*;
import java.util.*;

/**
 * Politique à objectifs : progression de matière + prérequis des Big Bangs et du cosmos. C'est le planificateur que
 * tous les bots partagent ; chacun ({@link AdvancedLab}, {@link IdleProfiles24}) pilote lui-même la boucle du jeu et
 * appelle {@link #collider()}, {@link #probes()}, {@link #cosmos()} et {@link #resourceLoop()} à chaque décision.
 */
public class RobotCosmosPlanner {
  final Game g;
  final double step;
  /** Vrai quand c'est le robot qui a coupé la fusion et la synthèse automatiques (avant un Big Bang) : il les rallume ensuite. */
  boolean fusionPausedByRobot=false;
  boolean patiencePausedByRobot=false;
  boolean persistenceReserveManaged=false;
  BigNum previousPersistenceReserve=BigNum.ZERO;
  /** La variante experte sait quand différer volontairement une fusion. */
  boolean waitForPatientFusion(){return false;}
  /**
   * Les choix de jeu des trois actes, dans chaque univers (version 0.135) : un expert suit l'ordre d'achat mesuré du
   * modèle (§14.1) et l'arbre de matière noire au mieux, tente les défis de Big Bang et envoie ses sondes en long voyage
   * avant de s'absenter ; un joueur moins appliqué achète le moins cher, ignore les défis de Big Bang ou n'envoie que
   * des sauts courts.
   *
   * @param opening        suit l'ordre d'ouverture du modèle (sinon, l'amélioration d'atomes la moins chère)
   * @param darkBest       l'arbre de matière noire au mieux (sinon, la case la moins chère)
   * @param bangChallenges tente les défis de Big Bang
   * @param longTrips      choisit la durée des voyages des sondes selon le temps jusqu'à son retour
   */
  record EarlyStyle(boolean opening, boolean darkBest, boolean bangChallenges, boolean longTrips) {
    /** Le joueur expert : c'était le jeu de tous les bots avant la 0.135. */
    static final EarlyStyle EXPERT=new EarlyStyle(true,true,true,false);
  }
  EarlyStyle earlyStyle=EarlyStyle.EXPERT;
  /** Certains profils veulent volontairement éviter l'usage d'une fonctionnalité. */
  boolean useBangChallenges(){return earlyStyle.bangChallenges();}
  /** Le temps, en secondes, jusqu'au prochain retour du joueur : les profils le savent ; 0 pour les autres bots. */
  double timeUntilBack(){return 0;}
  /** La durée des voyages des sondes. */
  ProbeTrip probeTrip(){
    if(earlyStyle.longTrips()){
      double away=timeUntilBack();
      ProbeTrip best=null;
      // La durée réelle du voyage (moteurs, bonus de Big Bang), pas la durée affichée de base.
      for(ProbeTrip t:new ProbeTrip[]{ProbeTrip.HOUR,ProbeTrip.LONG,ProbeTrip.NIGHT})if(g.probeDuration(t)<=away)best=t;
      if(best!=null)return best;
    }
    return !g.shoppingList().matter().isEmpty() || !g.shoppingList().items().isEmpty()
        ? ProbeTrip.SHORT : (g.bigBangs()>=Game.BANG_CHALLENGES_BIG_BANGS ? ProbeTrip.HOUR : ProbeTrip.SHORT);
  }
  /** Les achats en atomes, selon le style. */
  void buyAtoms(int limit){if(earlyStyle.opening())FirstUniversePolicy.atoms(g,limit);else FirstUniversePolicy.cheapestAtoms(g,limit);}
  /** Les achats en matière noire, selon le style. */
  void buyDark(int limit){if(earlyStyle.darkBest())FirstUniversePolicy.dark(g,limit);else FirstUniversePolicy.cheapestDark(g,limit);}
  /**
   * Univers à former avant de s'arrêter (version 0.130) : 1 par défaut, le premier univers comme avant. Au-delà, le
   * bot lance l'univers suivant dès qu'un univers se forme ({@link NextUniversePolicy#launchNext}), et rejoue les trois
   * actes avec ce que le deuxième univers ajoute. Une partie en train de rejouer finit au moins l'univers en cours.
   */
  int targetUniverses=1;
  /** Les gestes du deuxième univers ; aucun dans le premier. Les profils y mettent leur façon de jouer (0.131). */
  NextUniversePolicy nextUniverse=new NextUniversePolicy();
  /** Le vide du troisième univers (version 0.133) ; rien tant qu'il ne se propose pas. Les profils y mettent leur façon. */
  VoidPolicy voidPolicy=new VoidPolicy();
  /**
   * La graine, les bulles de l'arbre, les hadrons et la ligne, à partir de l'univers 4 (version 0.152) ; rien avant.
   * Les profils y mettent leur façon.
   */
  UniverseTreePolicy tree=new UniverseTreePolicy();
  /** Les automates du collisionneur que le bot a mis en pause pour garder l'énergie du vide. */
  final Set<ColliderUpgrade> pausedForVoid=EnumSet.noneOf(ColliderUpgrade.class);
  /** Anomalies du ciel saisies (version 0.131). */
  int skyEventsCaught=0;
  /** Vrai pour jouer sans saisir les anomalies du ciel (propriété {@code idle.sansAnomalies}), comme avant la 0.131. */
  static boolean skyEventsOff(){return Boolean.getBoolean("idle.sansAnomalies");}
  /**
   * Saisit l'anomalie du ciel visible, si elle existe (version 0.131) : un vent de particules ou une fenêtre
   * d'exploration, facultatifs et gratuits. Vrai si une anomalie a été saisie.
   */
  boolean catchSkyEvent(){
    if(skyEventsOff() || g.skyEvent()==null)return false;
    if(g.catchSkyEvent()==null)return false;
    skyEventsCaught++;
    return true;
  }
  /** L'instant de la formation de chaque univers, en secondes de {@link #clockSeconds()}, dans l'ordre. */
  final List<Double> universeTimes=new ArrayList<>();
  /** Univers déjà formés au dernier relevé. */
  int universesSeen;
  /** Vrai au pas où le bot vient de lancer l'univers suivant. */
  boolean launchedNextUniverse=false;
  /** L'horloge des relevés : le temps de jeu ; les profils et le bot V2 comptent le temps réel. */
  double clockSeconds(){return g.state().timePlayed();}

  /**
   * Après chaque pas : note les univers formés, lance le suivant s'il en faut encore. Vrai une fois
   * {@link #targetUniverses} univers formés (hors de la partie qui mène au suivant).
   */
  boolean universeProgress(){
    launchedNextUniverse=false;
    while(universesSeen<g.universes()){universesSeen++;universeTimes.add(clockSeconds());}
    if(NextUniversePolicy.reached(g,targetUniverses))return true;
    // Version 0.152 : à partir de l'univers 4, l'univers suivant attend le palier de la graine qui l'ouvre (l'arrosoir
    // est tenu en attendant) ; les univers 2 et 3 se lancent toujours dès l'univers formé, comme avant.
    if(g.universes()<targetUniverses && g.canStartNextUniverse() && !tree.readyToLaunch(g))return false;
    if(NextUniversePolicy.launchNext(g,targetUniverses)){
      launchedNextUniverse=true;
      // Les explosions repartent de zéro : un défi abandonné dans l'univers d'avant se retente normalement.
      forgetFailedChallenges();
      System.out.printf(Locale.ROOT,"NEXT_UNIVERSE,%.4f,%d%n",clockSeconds()/3600,g.universes()+1);
    }
    return false;
  }

  /** Les instants de formation des univers, en heures, séparés par « ; » (vide s'il n'y en a pas). */
  String universeHours(){
    StringBuilder text=new StringBuilder();
    for(double t:universeTimes){if(text.length()>0)text.append(';');text.append(String.format(Locale.ROOT,"%.4f",t/3600));}
    return text.toString();
  }

  /** Durée de l'absence en cours, en secondes réelles : le jeu n'en rejoue que {@link Absence#MAX_SECONDS} au plus. */
  double awaySeconds=0;

  /**
   * Le jeu fermé pendant {@code dt} secondes, comme le vrai jeu le compte au retour : au taux hors ligne,
   * et pas au-delà de {@link Absence#MAX_SECONDS} (24 h) par absence. Rend le temps de jeu crédité.
   * (Correctif du 19/10/2026 : les anciennes campagnes créditaient toute l'absence, sans plafond.)
   */
  double away(double dt){
    double counted=Math.max(0,Math.min(dt,Absence.MAX_SECONDS-awaySeconds));
    awaySeconds+=dt;
    double credit=counted*g.offlineRate();
    if(credit>0)g.tickAway(credit);
    return credit;
  }

  /** Le joueur revient : la prochaine absence repart de zéro. */
  void back(){awaySeconds=0;}
  /** Échelles du cosmos formées, 3 une fois l'univers formé : former l'univers remet le cosmos à zéro (0.83). */
  static int cosmosStage(Game g){return g.hasCosmos(Cosmos.UNIVERSE)?Cosmos.values().length:g.cosmosFormed();}
  RobotCosmosPlanner(Game game,double step){this.g=game;this.step=step;universesSeen=g.universes();bigBangsSeen=g.bigBangs();explosionsSeen=g.state().explosions();}
  /** Achète, au plus {@code limit} fois, l'amélioration en particules la moins chère. */
  static void particle(Game g,int limit){for(int i=0;i<limit;i++){Upgrade b=Bots.cheapest(g,Resource.PARTICLES);if(b==null)break;g.buy(b.id());}}
  /** Réserve les 25 atomes nécessaires à Persistance seulement après l'automatisation. */
  void prioritizePersistence(){
    if(FirstUniversePolicy.readyForPersistence(g)){
      if(g.canBuy("atom_keep"))g.buy("atom_keep");
      if(FirstUniversePolicy.needsPersistence(g)){
        if(!persistenceReserveManaged){
          previousPersistenceReserve=g.synthesisReserve();
          persistenceReserveManaged=true;
        }
        if(g.synthesisReserve().lt(g.costOf("atom_keep")))
          g.setSynthesisReserve(g.costOf("atom_keep"));
      }
    }
    if(!FirstUniversePolicy.readyForPersistence(g) && persistenceReserveManaged){
      // Une autre politique a pu relever la réserve entre-temps : ne pas l'écraser.
      if(g.synthesisReserve().equals(g.costOf("atom_keep")))
        g.setSynthesisReserve(previousPersistenceReserve);
      persistenceReserveManaged=false;
    }
  }
  void collider(){
    if(!g.isColliderUnlocked())return;
    // Version 0.133 : tant que le vide se propose, l'énergie du collisionneur se garde pour l'ouvrir ; ses automates
    // d'achat attendent aussi, et repartent ensuite.
    // Version 0.152 : de même pour l'onglet des hadrons, à partir de l'univers 4.
    if(voidPolicy.savingForVoid(g) || tree.savingForHadrons(g)){
      for(ColliderGenerator gen:ColliderGenerator.values()){
        ColliderUpgrade auto=ColliderUpgrade.automationOf(gen);
        if(g.isColliderAutomationRunning(auto)){g.toggleColliderAutomation(auto);pausedForVoid.add(auto);}
      }
      if(g.isCrossing() && g.canCatchCrossing())g.catchCrossing();
      return;
    }
    if(!pausedForVoid.isEmpty()){
      for(ColliderUpgrade auto:pausedForVoid)if(g.state().isColliderPaused(auto))g.toggleColliderAutomation(auto);
      pausedForVoid.clear();
    }
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
  /** Exploite les retours de sondes avant d'engager les prochaines missions.
   * L'onde chimique d'urgence est particulièrement utile quand un astre attend des éléments.
   */
  void probes(){
    if(!g.isProbesUnlocked())return;
    for(Probe p:new ArrayList<>(g.landedProbes()))g.claimAllProbeRewards(p.id());
    for(ProbeUpgrade u:ProbeUpgrade.values()){
      while(g.canBuyProbeUpgrade(u))g.buyProbeUpgrade(u);
    }
    // Priorité au dépannage des éléments manquants pour la construction du ciel.
    if(g.canLaunchUrgentWave() && !g.shoppingList().items().isEmpty())g.launchUrgentWave();
    // Quand il reste des places, cibler les rayons dont les recettes demandées
    // utilisent les éléments rapportés. Les sondes partent seulement si leur destination est ouverte.
    java.util.Map<Integer,Integer> shortages=new HashMap<>();
    for(ShoppingList.Item item:g.shoppingList().items()){
      if(item.missing()<=0)continue;
      for(Integer z:item.molecule().recipe().keySet())
        shortages.merge(z,item.missing(),Integer::sum);
    }
    java.util.List<ProbeDestination> targets=new ArrayList<>();
    for(ProbeDestination destination:ProbeDestination.values())
      if(g.isProbeDestinationOpen(destination))targets.add(destination);
    targets.sort((a,b)->Integer.compare(probePriority(b,shortages),probePriority(a,shortages)));
    // La matière des astres/du cosmos demande des réapprovisionnements fréquents :
    // les sauts courts restent utiles ; les longs voyages servent quand la liste est vide.
    ProbeTrip trip=probeTrip();
    for(ProbeDestination destination:targets){
      if(g.canLaunchProbe(destination,trip))g.launchProbe(destination,trip);
    }
  }
  int probePriority(ProbeDestination destination,Map<Integer,Integer> shortage){
    int score=0;
    for(Integer element:g.probeElements(destination))
      score+=shortage.getOrDefault(element,0);
    return score;
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
  /**
   * Vise explicitement les deux étoiles mortes indispensables au huitième Big Bang, et d'abord les astres plus petits
   * qu'elles demandent (version 0.135 : la géante rouge de la naine blanche). Avant, une naine blanche à qui manquait
   * sa géante rouge n'était pas visée, la liste restait sur une nébuleuse verrouillée jusqu'au huitième Big Bang, et
   * le débutant restait bloqué à 25 astres dans l'univers 3. Les molécules et la matière demandées sont celles que le
   * vide allège ({@link Game#bodyMolecules}, {@link Game#bodyMatter}) : avant la 0.141, les quantités brutes du
   * catalogue faisaient produire trop dans l'univers 3.
   */
  boolean preEighthBody(){
    if(g.bigBangs()!=Bots.LAST_BIG_BANG-1 || g.bodiesMature()>=Game.BIG_BANG_SKY[Game.BIG_BANG_SKY.length-1])return false;
    String id=null;
    for(String wanted:new String[]{"white_dwarf","neutron_star"}){id=firstToForm(wanted,0);if(id!=null)break;}
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
    for(var required:g.bodyMolecules(body).entrySet()){
      String m=required.getKey();
      for(int i=0;i<25 && g.moleculeCount(m)<required.getValue() && g.canCreateMolecule(m);i++)g.createMolecule(m);
      if(g.canFormSubstance(m))g.formSubstance(m);
    }
    for(var required:g.bodyMatter(body).entrySet()){
      var matter=required.getKey();long objective=required.getValue();
      if(g.gatheredInState(matter)>=objective)continue;
      Molecule cheapest=Bots.cheapestInState(g,matter,m->g.isMoleculeKindUnlocked(m.kind())&&g.isMoleculeWithinReach(m.id())&&g.canCreateMolecule(m.id())&&g.hasSubstance(m.id()));
      if(cheapest==null){
        cheapest=Bots.cheapestInState(g,matter,m->g.isMoleculeKindUnlocked(m.kind())&&g.isMoleculeWithinReach(m.id())&&g.canCreateMolecule(m.id()));
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
  /** L'astre à former pour avancer vers celui-ci : lui, ou le premier astre plus petit qu'il demande et qui manque. */
  String firstToForm(String bodyId,int depth){
    if(g.hasBody(bodyId) || depth>16)return null;
    for(String needed:g.body(bodyId).bodies()){
      if(g.hasBody(needed))continue;
      String sub=firstToForm(needed,depth+1);
      if(sub!=null)return sub;
    }
    return bodyId;
  }
  /** Confie à la création automatique la matière que demande le prochain pas ; vrai s'il y en a à réunir. */
  boolean automateMatter(){
    // Quand le suivi des besoins de matière du moteur est actif (option du jeu), il gère lui-même les états.
    if(g.isAutoFollowingMatter()) return !g.autoMatterTargets().isEmpty();
    Set<String> chosen=new HashSet<>();
    // Les cinq états ont des exigences indépendantes pour le cosmos.
    // Leurs automatismes doivent progresser simultanément : le ciblage
    // d'un état unique sous-utilisait les matières disponibles.
    for(var need:g.shoppingList().matter().entrySet()){
      if(need.getValue()<=0)continue;
      Molecule best=FirstUniversePolicy.matterTarget(g,need.getKey());
      if(best!=null&&g.setMoleculeAutomated(best.id(),true))chosen.add(best.id());
    }
    for(String id:new ArrayList<>(g.state().automatedMolecules()))if(!chosen.contains(id))g.setMoleculeAutomated(id,false);
    return !chosen.isEmpty();
  }
  void cosmos(){
    for(SpaceUpgrade up:g.spaceUpgrades())if(g.canBuySpaceUpgrade(up.id()))g.buySpaceUpgrade(up.id());
    g.setAutoFollowList(true);
    g.setAutoMoleculeReserve(0.10);
    // Correctif du 19/10/2026. Avant : création automatique toujours coupée (« elle mange tout l'espace »), si bien
    // que la matière des astres et du cosmos ne se créait qu'aux décisions du robot, jamais en son absence.
    // Maintenant, comme un joueur avisé : pour chaque état dont le prochain pas demande de la matière, l'amas
    // rassemblé le moins encombrant est confié à la création automatique (les autres lui sont retirés), et la
    // création automatique ne tourne que tant qu'il y a de la matière à réunir. La réserve d'espace reste de 10 %.
    automateMatter();
    g.setMoleculeAutomationEnabled(true);
    g.setAutoGatherEnabled(true);
    g.setAutoFormEnabled(true);
    for(int i=0;i<5;i++){
       int s=g.formAllSubstances(),a=g.formAllAssemblies(),b=g.formAllBodies();
       if(s+a+b==0)break;
    }
    if(g.nextCosmos()!=null&&g.canFormCosmos(g.nextCosmos())){Cosmos c=g.nextCosmos();if(g.formCosmos(c))System.out.printf(Locale.ROOT,"COSMOS_DONE,%.4f,%s%n",g.state().timePlayed()/3600,c);}
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
    // La synthèse et la fabrication de gaz ne doivent pas affamer les
    // quatre autres états. Répartir les achats manuels à chaque décision.
    for(var e:wish.matter().entrySet()){
      if(e.getValue()<=0)continue;
      long target=g.gatheredInState(e.getKey())+e.getValue(); // shoppingList.matter contient le MANQUE, pas le seuil absolu
      Molecule choice=Bots.cheapestInState(g,e.getKey(),m->g.hasSubstance(m.id())&&g.canCreateMolecule(m.id()));
      if(choice==null){ // Aucun amas rassemblé dans cet état : en ouvrir un avant de produire en masse.
        Molecule starter=Bots.cheapestInState(g,e.getKey(),m->g.isMoleculeKindUnlocked(m.kind()) && !g.hasSubstance(m.id()) && g.isMoleculeWithinReach(m.id()));
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
  /** Dernière minute simulée où un diagnostic des cinq conditions a été écrit. */
  double lastBigBangDiagnostic = -900;
  void logBigBangDiagnostic(){
    double now=g.state().timePlayed();
    if(g.bigBangs()!=0 || now-lastBigBangDiagnostic<900
            || !g.baseChallengesDone() || g.achievementCount()<40)return;
    lastBigBangDiagnostic=now;
    System.out.printf(Locale.ROOT,
      "BB_STATUS,t=%.2fh,dm_disponible=%s,dm_depensee=%s,dm_totale=%s,dm_manquante=%s,conditions=%d/%d,particules=%s,atomes=%s,defis=%d,succes=%d%n",
      now/3600,g.state().darkMatter(),g.state().darkMatterSpent(),g.darkMatterEarned(),
      g.bigBangDarkMatter().subtract(g.state().darkMatter()).max(BigNum.ZERO),
      g.bigBangConditionsMet(),g.bigBangConditions().size(),
      g.isBigBangConditionMet(BigBangCondition.PARTICLES),
      g.isBigBangConditionMet(BigBangCondition.ATOMS),
      g.completedChallenges(),g.achievementCount());
  }
  /**
   * Les trois défis de Big Bang se tentent sur les passages 6, 7 et 8, après le ciel. Dans le premier univers, pas sur
   * une partie déjà prête à Big Bang (elle serait sacrifiée), dans l'ordre du catalogue. À partir du deuxième (version
   * 0.135), aussi quand le Big Bang est prêt, et d'abord les deux défis qui ne remettent rien à zéro : le ciel figé,
   * gratuit quand le Big Bang est prêt, puis la barre haute (maturation ×2). Avant, il n'en passait presque aucun dans
   * les univers suivants, où le ciel se réunit en même temps que le reste, alors que chacun y rend la maturation quatre
   * fois plus rapide et double particules et molécules ({@link Game#universeBangFactor}) : après le huitième Big Bang,
   * les sept derniers astres mûrissaient en quatre heures. La matière noire exigée est celle du moteur
   * ({@link Game#bigBangDarkMatter()} : le double pendant la barre haute).
   */
  boolean bangChallenges(){
    if(!useBangChallenges() || g.bigBangs()<Game.BANG_CHALLENGES_BIG_BANGS
        || g.bigBangs()>=Bots.LAST_BIG_BANG || !FirstUniversePolicy.skyReady(g)
        || !g.baseChallengesDone()
        || g.achievementCount()<Game.BIG_BANG_ACHIEVEMENTS
        || g.state().darkMatter().lt(g.bigBangDarkMatter())
        || !g.canStartBangChallenge())return false;
    // Le premier univers garde la règle d'avant par défaut : c'est la partie de référence de l'équilibrage (27 h).
    String setting=bangChallengesSetting();
    boolean later=(g.isSecondUniverse() || BANG_CHALLENGES_FIRST) && !setting.equals("avant");
    if(g.canBigBang() && !later)return false;
    for(BangChallenge challenge:bangChallengeOrder(later)){
      if(!g.isBangChallengeCompleted(challenge) && g.startBangChallenge(challenge))return true;
    }
    return false;
  }
  /** Les valeurs admises de la propriété {@code idle.defisBB}. */
  static final List<String> BANG_CHALLENGE_SETTINGS=List.of("maturation","gele","avant");
  /**
   * L'ordre des défis de Big Bang ({@code -Didle.defisBB}) : « maturation » (ciel figé, barre haute, puis le
   * catalogue), « gele » (ciel figé, puis le catalogue) ou « avant » (le catalogue, jamais sur une partie prête à Big
   * Bang : la règle d'avant la 0.135). Mesuré en 0.135 avec « maturation », Speedrunner et Régulier : deuxième
   * univers de 20 h 33–21 h 15 à 14 h 57–19 h 23 ; troisième de
   * 12 h 08–20 h 18 à 10 h 40–12 h 31. « gele » et « maturation » font jeu égal (72,7 h sur cinq parties).
   * Lue à chaque usage, et refusée avec un message clair hors de {@link #BANG_CHALLENGE_SETTINGS} : avant la 0.141,
   * une valeur inconnue jouait « gele » en silence.
   */
  static String bangChallengesSetting(){
    String wanted=System.getProperty("idle.defisBB","maturation").trim();
    if(!BANG_CHALLENGE_SETTINGS.contains(wanted))
      throw new IllegalArgumentException("idle.defisBB="+wanted+" : "+String.join(", ",BANG_CHALLENGE_SETTINGS)+" attendu");
    return wanted;
  }
  /**
   * {@code -Didle.defisBBPremier=true} : la même règle dès le premier univers. Mesuré en 0.135 : le Speedrunner y
   * passe de 27 h 01 à 24 h 44, le Régulier de 34 h 26 à 31 h 59. Pas par défaut, parce que le premier univers du
   * Speedrunner est la référence de l'équilibrage : à décider avec le reste de l'équilibrage.
   */
  static final boolean BANG_CHALLENGES_FIRST=Boolean.getBoolean("idle.defisBBPremier");
  List<BangChallenge> bangChallengeOrder(boolean later){
    if(!later)return g.bangChallenges();
    List<BangChallenge> order=new ArrayList<>();
    order.add(BangChallenge.FROZEN);
    if(bangChallengesSetting().equals("maturation"))order.add(BangChallenge.RAISED);
    for(BangChallenge c:g.bangChallenges())if(!order.contains(c))order.add(c);
    return order;
  }
  void resourceLoop(){
    if(earlyStyle.opening()){
      FirstUniversePolicy.bootstrapAutomations(g);
      prioritizePersistence();
    }
    // Version 0.130 : régime rapide, choix de cycle, famille préférée, éléments gratuits ; rien dans le premier univers.
    nextUniverse.play(g);
    voidPolicy.play(g);
    // Version 0.152 : la graine, les bulles, les hadrons et la ligne ; rien avant l'univers 4.
    tree.play(g);
    if(bangChallenges())return;
    logBigBangDiagnostic();
    g.buyAllBigBangMilestones();   // version 0.124 : les paliers de Big Bang se paient
    // Réduire les décisions pendant la phase de production pure.
    particle(g,500);
    boolean finalMode=FirstUniversePolicy.finalBangPreparation(g);
    // Les atomes exigés sont ceux du moteur : cent fois plus pendant la barre haute (avant la 0.141, la fusion se
    // coupait à 1e12 atomes quand le défi en demandait 1e14).
    boolean stopFusion=finalMode && g.state().atoms().gte(g.bigBangAtoms());
    boolean patientWait=waitForPatientFusion() && !stopFusion;
    if(stopFusion){
      g.setAutomationEnabled("auto_fusion",false);
      g.setAutomationEnabled("auto_synthesis",false);
      fusionPausedByRobot=true;
    }else if(fusionPausedByRobot){
      g.setAutomationEnabled("auto_fusion",true);
      g.setAutomationEnabled("auto_synthesis",true);
      fusionPausedByRobot=false;
    }
    if(patientWait){
      if(g.isAutomationEnabled("auto_fusion")){
        g.setAutomationEnabled("auto_fusion",false);
        patiencePausedByRobot=true;
      }
    }else if(patiencePausedByRobot){
      g.setAutomationEnabled("auto_fusion",true);
      patiencePausedByRobot=false;
    }
    for(int i=0;i<5 && !stopFusion && !patientWait && g.canFuse();i++){
      g.fuse();
      if(earlyStyle.opening())prioritizePersistence();
      if(!finalMode)buyAtoms(50);
      particle(g,200);
    }
    if(earlyStyle.opening()){
      FirstUniversePolicy.bootstrapAutomations(g);
      prioritizePersistence();
    }
    // Les automatismes et cadences hors de l'ordre du §14.1 viennent après lui (« ensuite le moins cher ») ; sans
    // l'ordre d'ouverture, ils s'achètent dès qu'ils sont payables.
    if(!earlyStyle.opening() || FirstUniversePolicy.openingDone(g))for(Automation a:g.automations()){
      if(g.canBuyAutomation(a.id()))g.buyAutomation(a.id());
      if(g.canSpeedUpAutomation(a.id()))g.speedUpAutomation(a.id());
    }
    if(patientWait && g.isAutomationEnabled("auto_fusion")){
      g.setAutomationEnabled("auto_fusion",false);
      patiencePausedByRobot=true;
    }
    if(!finalMode)buyAtoms(60);
    // Modèle 0.123 §4 et §14.2 : synthétiser dès l'ouverture du tableau, en gardant seulement les atomes
    // de la prochaine étape de l'ouverture. Avant, rien ne se synthétisait jusqu'à Persistance :
    // le tableau restait vide de 3 h 10 à 4 h 30 et la première explosion venait à 7 h au lieu de 5 h 12.
    if(!finalMode)FirstUniversePolicy.synthesize(g,20,earlyStyle.opening());
    if(g.state().explosions()>0)g.growDarkMatter(step);
    buyDark(50);
    // Les automatismes sombres ouverts gratuitement doivent rester actifs,
    // sauf si le mode « à la main » du défi de Big Bang interdit leur action.
    boolean hold=FirstUniversePolicy.holdExplosions(g);
    for(DarkAutomation automation:g.darkAutomations()){
      if(!g.isDarkAutomationUnlocked(automation.id()) || g.isHandOnly())continue;
      // §15 : couper l'explosion automatique tant que les molécules du ciel ont besoin des éléments.
      boolean wanted=!(hold && automation.kind()==DarkAutomation.Kind.EXPLOSION);
      if(g.isDarkAutomationEnabled(automation.id())!=wanted)g.setDarkAutomationEnabled(automation.id(),wanted);
    }
    // Les succès et les défis (correctif du 19/10/2026, 0.86) : sans eux, un bot parti de zéro restait à 37 succès
    // et 0 défi, et n'atteignait jamais son premier Big Bang (il en faut 40 et les 10 défis).
    hunt();
    // La synthèse ciblée doit alimenter les recettes des astres, pas tirer au hasard
    // pendant les dizaines d'heures où manque le même élément (modèle §8.3 et §15).
    if(g.isTargetedSynthesisUnlocked()
        && g.hasAchievement(Achievements.TARGETED_ACTINIDE)){
      var need=FirstUniversePolicy.chemistryTarget(g);
      if(need!=g.synthesisTarget())g.setSynthesisTarget(need);
    }
    challenges();
    // Ne pas exploser avec une branche de densité incomplète : laisser les particules
    // croître jusqu'au coût astronomique du niveau 3.
    // Correctif du 19/10/2026 (0.86) : le bot explosait à chaque décision, et ne réunissait jamais les particules ou
    // les atomes des cases chères de l'arbre (Densité…) : sa production plafonnait vers 1e45. Il laisse maintenant
    // grandir la partie tant qu'une case de l'arbre, ouverte mais trop chère, peut le devenir, au plus 20 minutes.
    boolean prepareDensity=g.bigBangs()==0 && !g.inChallenge() && g.baseChallengesDone() && g.state().darkMatter().gte(g.bigBangDarkMatter()) && waitsForTree();
    if(g.bigBangs()<Bots.LAST_BIG_BANG && g.canExplode() && (g.inChallenge() || (!finalMode && !prepareDensity && !hold)))g.explode();
    // Le ciel exigé (18, 24, 28 astres mûrs) fait partie des conditions que canBigBang vérifie.
    if(g.canBigBang() && g.bigBangs()<Bots.LAST_BIG_BANG){
        // Temps de jeu : les profils et le bot V2 pilotent eux-mêmes la boucle, le planificateur n'a pas d'horloge à lui.
        double current=g.state().timePlayed()/3600;
        int old=g.bigBangs();
        if(g.bigBang())System.out.printf(Locale.ROOT,"BIGBANG_DONE,%.4f,%d,%d%n",current,old,g.bigBangs());
    }
  }
  /** Une partie ne patiente jamais plus longtemps pour une case de l'arbre, en secondes de jeu. */
  static final double TREE_PATIENCE=6*3600;
  /** La production doit gagner au moins une demi-décade en ce temps, sinon la patience s'arrête. */
  static final double TREE_STALL=30*60;
  double stallMark=-1, stallLog=0, stallRun=-1;

  /**
   * Vrai si une case de l'arbre payée en particules, atomes ou taille est ouverte mais pas encore payable, et que la
   * partie en cours progresse encore assez pour l'atteindre : sa production gagne une demi-décade par quart d'heure.
   */
  boolean waitsForTree(){
    double run=g.stats().runTime();
    if(!g.isDarkMatterUnlocked() || run>TREE_PATIENCE)return false;
    double log=g.productionPerSecond().log10();
    if(run<stallRun || stallMark<0){stallMark=run;stallLog=log;}
    stallRun=run;
    if(run-stallMark>=TREE_STALL){
      if(log-stallLog<0.2){stallMark=-1;return false;}
      stallMark=run;stallLog=log;
    }
    for(DarkUpgrade d:g.darkUpgrades()){
      if(d.branch()==DarkUpgrade.Branch.DARK_MATTER || d.isLink() || g.isDarkMaxed(d.id()))continue;
      if(g.isDarkAvailable(d.id()) && !g.canBuyDark(d.id()))return true;
    }
    return false;
  }

  /** Les défis tentés sans succès, et l'explosion où chacun pourra être retenté. */
  final Map<String,Integer> failedChallenges=new HashMap<>();
  /** Les Big Bangs et les explosions de la partie au dernier passage dans {@link #challenges()}. */
  int bigBangsSeen, explosionsSeen;
  /** Temps de jeu au début du défi en cours. */
  double challengeStart=-1;
  /** Un défi qui ne se réussit pas en ce temps de jeu est abandonné, pour être retenté plus tard. */
  static final double CHALLENGE_GIVE_UP=2*3600;
  /** Explosions avant de retenter un défi abandonné. */
  static final int CHALLENGE_RETRY_EXPLOSIONS=25;

  /** Oublie les défis abandonnés : leur attente se comptait en explosions d'une partie qui vient d'être remise à zéro. */
  void forgetFailedChallenges(){
    failedChallenges.clear();
    challengeStart=-1;
    bigBangsSeen=g.bigBangs();
    explosionsSeen=g.state().explosions();
  }

  /**
   * Les défis, un par un, dans l'ordre : dès que l'arbre a un peu avancé (10 explosions), le premier défi ouvert et
   * pas encore réussi commence ; une explosion le réussit. Trop long (2 h de jeu), il est abandonné et retenté 25
   * explosions plus tard. Le premier abandon donne aussi le succès « Demi-tour ». Un Big Bang, un défi de Big Bang qui
   * remet à zéro ou le lancement d'un univers remettent les explosions à zéro : les attentes notées avant tombent
   * (avant la 0.141, un défi abandonné à 30 explosions attendait 55 explosions de la partie suivante au lieu de 25).
   */
  void challenges(){
    double now=g.state().timePlayed();
    if(g.bigBangs()!=bigBangsSeen || g.state().explosions()<explosionsSeen)forgetFailedChallenges();
    explosionsSeen=g.state().explosions();
    if(g.inChallenge()){
      if(challengeStart<0)challengeStart=now;
      if(now-challengeStart>CHALLENGE_GIVE_UP){
        String id=g.activeChallenge().id();
        if(g.abandonChallenge())failedChallenges.put(id,g.state().explosions()+CHALLENGE_RETRY_EXPLOSIONS);
        challengeStart=-1;
      }
      return;
    }
    challengeStart=-1;
    // Les défis effacent le tableau. Finir les astres de ce Big Bang avant de recommencer.
    if(!FirstUniversePolicy.skyReady(g))return;
    if(g.completedChallenges()>=g.challenges().size() || g.state().explosions()<10)return;
    for(Challenge c:g.challenges()){
      if(!g.isChallengeUnlocked(c.id()) || g.isChallengeCompleted(c.id()))continue;
      if(failedChallenges.getOrDefault(c.id(),0)>g.state().explosions())continue;
      // La Fuite (deuxième univers, version 0.130) n'est pas demandée par le Big Bang : elle ne passe pas devant lui.
      if(c.id().equals(Challenges.LEAK.id()) && (g.canBigBang() || FirstUniversePolicy.finalBangPreparation(g)))continue;
      if(g.startChallenge(c.id())){challengeStart=now;return;}
    }
  }

  /**
   * Les succès que le jeu demande de faire exprès, et qu'un bot ne faisait jamais : verrouiller l'appui, acheter
   * Persistance, viser un actinide, tenir une réserve de synthèse, acheter en gros, « tout acheter ». Chaque geste
   * n'a lieu que tant que son succès manque, et le réglage revient ensuite comme avant.
   */
  void hunt(){
    if(!g.hasAchievement(Achievements.HOLD_LOCK) && g.isHoldLockUnlocked())g.setHoldLocked(true);
    // Sans l'ordre d'ouverture, Persistance s'achète comme le reste, au plus bas prix (relecture 0.135).
    if(earlyStyle.opening())prioritizePersistence();
    if(g.isTargetedSynthesisUnlocked()){
      boolean wantActinide=!g.hasAchievement(Achievements.TARGETED_ACTINIDE);
      if(wantActinide && g.synthesisTarget()!=idle.core.elements.ElementCategory.ACTINIDE)g.setSynthesisTarget(idle.core.elements.ElementCategory.ACTINIDE);
      if(!wantActinide && g.synthesisTarget()==idle.core.elements.ElementCategory.ACTINIDE)g.setSynthesisTarget(null);
    }
    boolean wantReserve=!g.hasAchievement("feat_reserve") && g.isSynthesisAutomationUnlocked();
    if(wantReserve && g.synthesisReserve().lt(BigNum.of(50)))g.setSynthesisReserve(BigNum.of(50));
    if(!wantReserve && g.synthesisReserve().equals(BigNum.of(50)) && g.hasAchievement("feat_reserve"))g.setSynthesisReserve(BigNum.ZERO);
    if(!g.hasAchievement(Achievements.BUY_EVERYTHING))g.buyAllWithParticles();
    if(!g.hasAchievement(Achievements.BULK_PURCHASE)){
      Upgrade cheapest=Bots.cheapest(g,Resource.PARTICLES);
      if(cheapest!=null)g.buy(cheapest.id(),Integer.MAX_VALUE);
    }
  }
}
