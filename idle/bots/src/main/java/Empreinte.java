import idle.core.*;
import idle.core.save.*;
import java.nio.file.*;
import java.util.*;

/**
 * L'empreinte du moteur (19/10/2026) : joue quatre parties fixes avec les bots et écrit leur sauvegarde
 * finale. Deux versions du moteur au comportement identique donnent les mêmes sauvegardes, aux tout
 * derniers chiffres des nombres réels près (outils/comparer_sauvegardes.py les compare ainsi). C'est la
 * preuve qu'une optimisation du moteur ne change rien au jeu.
 *
 * <p>Les quatre parties : le début (10 h depuis zéro, profil 1), le premier Big Bang (6 h, profil 3), la
 * fin de partie (bot V2 jusqu'à l'univers) et un joueur absent (30 h, profil 20, 30 minutes toutes les 4 h).
 *
 * <p>Usage : {@code Empreinte <dossier-de-sortie> [debut|bigbang|v2|absent ...]}, depuis le dossier bots.
 */
public class Empreinte {
  static Game save(String file, long seed) throws Exception {
    return Bots.load(Path.of("sauvegardes/reference", file), seed);
  }

  public static void main(String[] args) throws Exception {
    Path out = Path.of(args[0]);
    Files.createDirectories(out);
    List<String> parts = args.length > 1 ? List.of(args).subList(1, args.length) : List.of("debut", "bigbang", "v2", "absent");
    for (String part : parts) {
      Path logs = Files.createTempDirectory("empreinte");
      long t0 = System.nanoTime();
      Game g;
      switch (part) {
        case "debut" -> { g = Bots.fresh(7); new FreshPhaseProfiles(g, FreshPhaseProfiles.PROFILES[0], 7, 20).runFresh(10, logs); }
        case "bigbang" -> { g = save("checkpoint_bigbang.save", 3); new IdleProfiles24(g, IdleProfiles24.PROFILES[2], 3, 20).execute(6, logs); }
        case "v2" -> { g = save("checkpoint_before_eighth.save", 42); new AdvancedLab(g, 20, "DEPENDANCES", 42).runLab(12, logs); }
        case "absent" -> { g = save("checkpoint_before_eighth.save", 5); new IdleProfiles24(g, IdleProfiles24.PROFILES[19], 5, 20).execute(30, logs); }
        default -> throw new IllegalArgumentException("partie inconnue : " + part);
      }
      Files.writeString(out.resolve(part + ".save"), SaveCodec.write(g.state(), 0));
      System.out.printf(Locale.ROOT, "EMPREINTE,%s,%.1f s,temps de jeu %.2f h,%d Big Bangs,cosmos %d%n", part,
          (System.nanoTime() - t0) / 1e9, g.state().timePlayed() / 3600, g.bigBangs(), RobotCosmosPlanner.cosmosStage(g));
    }
  }
}
