import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Cosmos;
import java.util.Locale;
import java.util.Objects;

/** Garde-fou des 24 profils du premier univers : l'AFK n'est pas un blocage. */
final class ProfileProgressGuard {
    enum Verdict { PROGRESS, ABSENCE, SLOW, STAGNATION, BLOCKED, INVALID }
    record Snapshot(double wall, double gameTime, double online, double offline,
            int decisions, int bangs, int mature, int cosmos, long molecules,
            long creations, long gathered, long deficits, long fusions, long syntheses,
            BigNum particles, BigNum atoms, BigNum space, BigNum freeSpace,
            String target, boolean active, boolean won) {}
    record Assessment(Verdict verdict, String reason) {}
    static final double SAMPLE_SECONDS=3600, STAGNATION_SECONDS=4*3600,
            BLOCK_SECONDS=12*3600, MIN_ACTIVE_SECONDS=4*3600;
    static final int MIN_DECISIONS=20;
    final int id; final String name;
    final double staleThreshold, blockedThreshold, activeThreshold;
    final int decisionThreshold;
    private Snapshot previous, lastObjective, lastProduction;
    private Assessment latest;
    int samples;
    ProfileProgressGuard(IdleProfiles24.Profile p) {
        this(p.id(),p.name(),STAGNATION_SECONDS,BLOCK_SECONDS,MIN_ACTIVE_SECONDS,MIN_DECISIONS);
    }
    ProfileProgressGuard(int id,String name,double stale,double blocked,double active,int decisions) {
        if(stale<=0||blocked<=stale||active<=0||decisions<=0)
            throw new IllegalArgumentException("Seuils invalides");
        this.id=id;this.name=name;staleThreshold=stale;blockedThreshold=blocked;
        activeThreshold=active;decisionThreshold=decisions;
    }
    static Snapshot capture(Game g,double wall,double online,double offline,
            int decisions,boolean active) {
        ShoppingList list=g.shoppingList();
        long missing=0,gathered=0;
        for(ShoppingList.Item item:list.items())missing+=Math.max(0,item.missing());
        for(int n:list.matter().values())missing+=Math.max(0,n);
        for(int n:list.sorts().values())missing+=Math.max(0,n);
        for(Molecule.State state:Molecule.State.values())gathered+=g.gatheredInState(state);
        return new Snapshot(wall,g.state().timePlayed(),online,offline,decisions,
                g.bigBangs(),g.bodiesMature(),RobotCosmosPlanner.cosmosStage(g),
                g.moleculesCreated(),g.state().stats().moleculeCreations(),gathered,missing,
                g.state().stats().fusions(),g.state().stats().syntheses(),
                g.state().stats().particlesCreated(),g.state().stats().atomsCreated(),
                g.state().space(),g.freeSpace(),list.target(),active,
                g.hasCosmos(Cosmos.UNIVERSE));
    }
    private static boolean valid(double n){return Double.isFinite(n)&&n>=0;}
    private static boolean valid(BigNum n){return n!=null&&Double.isFinite(n.mantissa())&&n.sign()>=0;}
    private static String invalid(Snapshot s) {
        if(!valid(s.wall())||!valid(s.gameTime())||!valid(s.online())||!valid(s.offline()))
            return "temps invalide";
        if(s.online()>s.wall()+.01||s.offline()>s.wall()+.01)
            return "temps actif/hors ligne incoherent";
        if(s.decisions()<0||s.bangs()<0||s.mature()<0||s.cosmos()<0
                ||s.molecules()<0||s.creations()<0||s.gathered()<0||s.deficits()<0
                ||s.fusions()<0||s.syntheses()<0)return "compteur negatif";
        if(!valid(s.particles())||!valid(s.atoms())||!valid(s.space())
                ||!valid(s.freeSpace()))return "ressource invalide";
        return null;
    }
    Assessment assess(Snapshot s) {
        String error=invalid(s);
        if(error!=null)return save(s,Verdict.INVALID,error);
        if(previous==null){lastObjective=s;lastProduction=s;return save(s,Verdict.PROGRESS,"premier releve");}
        if(s.wall()+.001<previous.wall()||s.gameTime()+.001<previous.gameTime()
                ||s.online()+.001<previous.online()||s.offline()+.001<previous.offline()
                ||s.decisions()<previous.decisions())
            return save(s,Verdict.INVALID,"horloge ou decisions en regression");
        boolean objective=s.won()||s.bangs()>previous.bangs()
                ||s.mature()>previous.mature()||s.cosmos()>previous.cosmos()
                ||s.gathered()>previous.gathered()
                ||(Objects.equals(s.target(),previous.target())&&s.deficits()<previous.deficits())
                ||!Objects.equals(s.target(),previous.target());
        boolean production=s.molecules()>previous.molecules()||s.creations()>previous.creations()
                ||s.fusions()>previous.fusions()||s.syntheses()>previous.syntheses()
                ||s.particles().gt(previous.particles())||s.atoms().gt(previous.atoms());
        if(objective)lastObjective=s;
        if(production)lastProduction=s;
        if(!s.won()&&s.online()-previous.online()>=1800
                &&s.gameTime()-previous.gameTime()<1)
            return save(s,Verdict.BLOCKED,"horloge moteur figee pendant session active");
        if(objective)return save(s,Verdict.PROGRESS,"jalon ou deficit ameliore");
        double stale=s.wall()-lastObjective.wall();
        double active=s.online()-lastObjective.online();
        int decisions=s.decisions()-lastObjective.decisions();
        if(stale<staleThreshold)return save(s,Verdict.PROGRESS,"observation trop courte");
        if(active<3600||decisions<2||!s.active())
            return save(s,Verdict.ABSENCE,"absence ou rares decisions");
        if(s.gameTime()-lastProduction.gameTime()>=blockedThreshold
                &&s.online()-lastProduction.online()>=activeThreshold
                &&s.decisions()-lastProduction.decisions()>=decisionThreshold)
            return save(s,Verdict.BLOCKED,"aucun jalon ni production malgre decisions actives");
        if(decisions<decisionThreshold||active<activeThreshold
                ||s.molecules()>lastObjective.molecules()||s.creations()>lastObjective.creations()
                ||s.fusions()>lastObjective.fusions()||s.syntheses()>lastObjective.syntheses()
                ||s.particles().gt(lastObjective.particles())
                ||s.atoms().gt(lastObjective.atoms()))
            return save(s,Verdict.SLOW,"production ou rythme lent sans nouveau jalon");
        return save(s,Verdict.STAGNATION,"aucun jalon ni production recente");
    }
    private Assessment save(Snapshot s,Verdict v,String reason) {
        previous=s;latest=new Assessment(v,reason);samples++;return latest;
    }
    double lastWall(){return previous==null?-1:previous.wall();}
    Assessment latest(){return latest;}
    void observe(Snapshot s) {
        Assessment a=assess(s);
        System.out.printf(Locale.ROOT,
                "PROFIL24_GUARD,profil=%02d_%s,heure=%.3f,jeu_h=%.3f,enligne_h=%.3f,offline_h=%.3f,decisions=%d,bb=%d,astres_murs=%d,cosmos=%d,matiere=%d,creations=%d,rassemblee=%d,deficits=%d,fusions=%d,syntheses=%d,particules=%s,atomes=%s,espace=%s,libre=%s,statut=%s%n",
                id,name,s.wall()/3600,s.gameTime()/3600,s.online()/3600,s.offline()/3600,
                s.decisions(),s.bangs(),s.mature(),s.cosmos(),s.molecules(),s.creations(),
                s.gathered(),s.deficits(),s.fusions(),s.syntheses(),s.particles(),
                s.atoms(),s.space(),s.freeSpace(),a.verdict());
        if(a.verdict()!=Verdict.PROGRESS)
            System.err.printf(Locale.ROOT,
                    "PROFIL24_STAGNATION,profil=%02d_%s,heure=%.3f,classification=%s,objectif=%s,deficits=%d,decisions=%d,raison=%s%n",
                    id,name,s.wall()/3600,a.verdict(),String.valueOf(s.target()).replace(',',';'),
                    s.deficits(),s.decisions(),a.reason());
        if(a.verdict()==Verdict.BLOCKED||a.verdict()==Verdict.INVALID)
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Profil %02d_%s a %.3f h : %s (%s)",id,name,s.wall()/3600,
                    a.verdict(),a.reason()));
    }
}
