import idle.core.*;
import idle.core.upgrades.*;
import idle.core.darkmatter.*;
import idle.core.achievements.*;
import idle.core.sky.*;
import idle.core.save.*;
import java.nio.file.*;
import java.util.*;

/**
 * Contrôle indépendant de la sauvegarde de victoire.
 *
 * <p>Version 0.126 : la victoire est celle du jeu, et rien d'autre : l'univers formé ({@code hasCosmos(UNIVERSE)}, ce que
 * vérifie l'écran de fin). Avant, il fallait aussi exactement 8 Big Bangs, la galaxie, l'amas, tous les astres mûrs et
 * trois systèmes encore en place : une sauvegarde écrite par le moteur actuel, qui remet tout à zéro dès que l'univers se
 * forme, était donc refusée, et un univers formé après un neuvième Big Bang aussi. Ces chiffres restent affichés, pris
 * dans le bilan de la partie quand la remise à zéro a eu lieu.
 */
public class VerifyVictory {
  public static void main(String[] args) throws Exception {
    String file = args.length == 0 ? "sauvegardes/reference/checkpoint_universe_5s.save" : args[0];
    GameState state = SaveCodec.read(Files.readString(Path.of(file))).state();
    Game g = new Game(state,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,Achievements.DEFAULT,new Random(42));
    if (!g.hasCosmos(Cosmos.UNIVERSE)) throw new AssertionError("Univers absent : la partie n'est pas gagnée");
    // Une sauvegarde écrite après la remise à zéro de l'univers garde son bilan ; une plus ancienne a encore tout en place.
    long[] tally = g.stats().universeTally();
    boolean reset = g.state().cosmosLevel() < Cosmos.values().length;
    long bigBangs = reset ? tally[0] : g.bigBangs();
    System.out.printf(Locale.ROOT,"VICTORY_VERIFIED,timeHours=%.9f,universeAtHours=%.9f,bigbangs=%d,reset=%b,cosmos=%d,molecules=%d,substances=%d,assemblies=%d,bodies=%d,mature=%d,systems=%d,achievements=%d,elements=%d%n",
      g.state().timePlayed()/3600,g.stats().reachedAt(GameStats.Step.UNIVERSE)/3600,bigBangs,reset,g.cosmosFormed(),g.moleculesCreated(),g.substancesFormed(),g.assembliesFormed(),g.bodiesFormed(),g.bodiesMature(),g.solarSystemsComplete(),g.achievementCount(),g.discoveredElements());
  }
}
