import static org.junit.jupiter.api.Assertions.*;
import idle.core.math.BigNum;
import org.junit.jupiter.api.Test;

class ProfileProgressGuardFailureTest {
    private static ProfileProgressGuard.Snapshot sample(double wall,double game,
            double online,int decisions,boolean active) {
        return new ProfileProgressGuard.Snapshot(wall*3600,game*3600,online*3600,
                (wall-online)*3600,decisions,7,27,0,0,0,0,100,0,0,
                BigNum.ZERO,BigNum.ZERO,BigNum.ONE,BigNum.ONE,"amas",active,false);
    }
    @Test void stagnationDiagnostiqueePuisBlocageConfirme() {
        var g=new ProfileProgressGuard(1,"Temoin",4*3600,12*3600,4*3600,20);
        g.observe(sample(0,0,0,0,true));
        assertEquals(ProfileProgressGuard.Verdict.STAGNATION,
                g.assess(sample(5,5,5,30,true)).verdict());
        assertThrows(IllegalStateException.class,
                ()->g.observe(sample(13,13,13,100,true)));
        assertEquals(ProfileProgressGuard.Verdict.BLOCKED,g.latest().verdict());
    }
    @Test void horlogeFigeeEtValeurInvalideFontEchouer() {
        var g=new ProfileProgressGuard(1,"Temoin",4*3600,12*3600,4*3600,20);
        g.observe(sample(0,0,0,0,true));
        assertThrows(IllegalStateException.class,
                ()->g.observe(sample(1,0,1,3,true)));
        var bad=new ProfileProgressGuard(2,"Invalide",4*3600,12*3600,4*3600,20);
        assertThrows(IllegalStateException.class,
                ()->bad.observe(sample(1,Double.NaN,1,1,true)));
        assertEquals(ProfileProgressGuard.Verdict.INVALID,bad.latest().verdict());
    }
}
