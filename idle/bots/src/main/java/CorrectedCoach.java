import idle.core.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.save.*;import java.util.*;import java.nio.file.*;
/** Corrige SEULEMENT le cas "preparation Big Bang" apres le 8e.
 * Les automatismes peuvent alors regarnir le tableau pour les recettes cosmiques.
 */
public class CorrectedCoach extends AdvancedLab {
  CorrectedCoach(Game g,double dt,long seed){super(g,dt,"EXPERT_COMPLET",seed);}
  @Override void resourceLoop(){
    if(g.bigBangs()<8){super.resourceLoop();return;}
    // Le 8e Big Bang est deja passe: ne plus bloquer fusion/synthese pour la condition d'un BB suivant.
    g.setAutomationEnabled("auto_fusion",true);
    g.setAutomationEnabled("auto_synthesis",true);
    particle(g,500);
    for(int i=0;i<6&&g.canFuse();i++){g.fuse();atoms(g,50);particle(g,150);}
    for(Automation a:g.automations()){
      if(g.canBuyAutomation(a.id()))g.buyAutomation(a.id());
      if(g.canSpeedUpAutomation(a.id()))g.speedUpAutomation(a.id());
    }
    atoms(g,70);
    for(int i=0;i<30&&g.canSynthesize();i++)g.synthesize();
    if(g.state().explosions()>0)g.growDarkMatter(step);
    dark(g,30);
  }
  public static void main(String[]a)throws Exception{
    if(a.length<5)throw new IllegalArgumentException("checkpoint hours step seed out");
    GameState st=SaveCodec.read(Files.readString(Path.of(a[0]))).state();
    Game g=new Game(st,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(Long.parseLong(a[3])));
    new CorrectedCoach(g,Double.parseDouble(a[2]),Long.parseLong(a[3])).runLab(Double.parseDouble(a[1]),Path.of(a[4]));
  }
}
