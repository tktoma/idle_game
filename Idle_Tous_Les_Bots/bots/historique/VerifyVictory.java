import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.bigbang.*;
import idle.core.achievements.*;
import idle.core.sky.*;
import idle.core.save.*;
import java.nio.file.*;
import java.util.*;

/** Contrôle indépendant de la sauvegarde de victoire. */
public class VerifyVictory {
  public static void main(String[] args) throws Exception {
    String file = args.length == 0 ? "/mnt/data/idle_analysis/checkpoint_universe_finished.save" : args[0];
    GameState state = SaveCodec.read(Files.readString(Path.of(file))).state();
    Game g = new Game(state,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,Achievements.DEFAULT,new Random(42));
    if (!g.hasCosmos(Cosmos.GALAXY)) throw new AssertionError("Galaxie absente");
    if (!g.hasCosmos(Cosmos.CLUSTER)) throw new AssertionError("Amas absent");
    if (!g.hasCosmos(Cosmos.UNIVERSE)) throw new AssertionError("Univers absent");
    if (g.bigBangs() != 8) throw new AssertionError("8 Big Bangs attendus");
    if (g.bodiesMature()!=g.bodies().size()) throw new AssertionError("Astres pas tous matures");
    if (g.solarSystemsComplete() < 3) throw new AssertionError("Systèmes incomplets");
    System.out.printf(Locale.ROOT,"VICTORY_VERIFIED,timeHours=%.9f,bigbangs=%d,cosmos=%d,molecules=%d,substances=%d,assemblies=%d,bodies=%d,mature=%d,systems=%d,achievements=%d,elements=%d%n",
      g.state().timePlayed()/3600,g.bigBangs(),g.cosmosFormed(),g.moleculesCreated(),g.substancesFormed(),g.assembliesFormed(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),g.achievementCount(),g.discoveredElements());
  }
}
