import idle.core.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.save.*;import java.util.*;import java.nio.file.*;
public class RecreateLosers {
 public static void main(String[]a)throws Exception{
  int id=Integer.parseInt(a[0]); double hours=Double.parseDouble(a[1]);long seed=42;
  Path cp=Path.of(a[2]),out=Path.of(a[3]);Files.createDirectories(out);
  GameState st=SaveCodec.read(Files.readString(cp)).state();
  Game g=new Game(st,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(seed));
  IdleProfiles24 bot=new IdleProfiles24(g,IdleProfiles24.PROFILES[id-1],seed,20);
  bot.execute(hours,out);
  Files.writeString(out.resolve("etat_72h.save"),SaveCodec.write(g.state(),0));
  System.out.println("CHECKPOINT_RECREATED,"+id+",bb="+g.bigBangs()+",cosmos="+g.cosmosFormed()+",mature="+g.bodiesMature());
 }
}
