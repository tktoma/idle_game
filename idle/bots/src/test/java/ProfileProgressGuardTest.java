import static org.junit.jupiter.api.Assertions.*;
import idle.core.math.BigNum;
import org.junit.jupiter.api.Test;

class ProfileProgressGuardTest {
    static ProfileProgressGuard.Snapshot sample(double h,double online,int actions,
            long matter,long deficit,long fusions,boolean active) {
        return new ProfileProgressGuard.Snapshot(h*3600,h*3600,online*3600,
                (h-online)*3600,actions,7,27,0,matter,matter,matter,deficit,
                fusions,fusions,BigNum.of(fusions),BigNum.ZERO,BigNum.ONE,BigNum.ONE,
                "amas",active,false);
    }
    @Test void productionSansJalonEstLenteEtNonBloquee() {
        var g=new ProfileProgressGuard(1,"Rapide",4*3600,12*3600,4*3600,20);
        g.observe(sample(0,0,0,0,100,0,true));
        assertEquals(ProfileProgressGuard.Verdict.PROGRESS,
                g.assess(sample(5,5,100,5,95,50,true)).verdict());
        assertEquals(ProfileProgressGuard.Verdict.SLOW,
                g.assess(sample(10,10,200,5,95,100,true)).verdict());
    }
    @Test void absenceEtAfkNeSontPasBloques() {
        var g=new ProfileProgressGuard(22,"Weekend",4*3600,12*3600,4*3600,20);
        g.observe(sample(0,0,0,0,100,0,true));
        assertEquals(ProfileProgressGuard.Verdict.ABSENCE,
                g.assess(sample(16,0.5,1,0,100,0,false)).verdict());
        var afk=new ProfileProgressGuard(17,"AFK",4*3600,12*3600,4*3600,20);
        afk.observe(sample(0,0,0,0,100,0,true));
        assertEquals(ProfileProgressGuard.Verdict.SLOW,
                afk.assess(sample(13,13,13,0,100,0,true)).verdict());
    }
    @Test void couverture24Profils() {
        assertEquals(24,IdleProfiles24.PROFILES.length);
        for(IdleProfiles24.Profile p:IdleProfiles24.PROFILES){
            var g=new ProfileProgressGuard(p);
            assertEquals(p.id(),g.id);
            assertEquals(ProfileProgressGuard.Verdict.PROGRESS,g.assess(sample(0,0,0,0,100,0,true)).verdict(),
                    p.name()+" : le premier relevé est un progrès");
        }
    }
}
