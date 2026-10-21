import static org.junit.jupiter.api.Assertions.*;

import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.save.SaveCodec;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Validations de diversité des bots du PREMIER univers.
 * Les tests de durée sont déterministes et utilisent les vraies méthodes du moteur.
 */
class BotsExtendedValidationTest {
    private static Game fromCheckpoint(long seed) throws Exception {
        GameState state = SaveCodec.read(Files.readString(
                Path.of("sauvegardes/reference/checkpoint_before_eighth.save"))).state();
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
    }

    private static Game fresh(long seed) {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
        game.start();
        return game;
    }

    @Test
    void expertGagneAvecPlusieursGraines(@TempDir Path out) throws Exception {
        // La graine 42 figure deja dans BotsTest. Verifier des parties independantes.
        for (long seed : new long[]{43, 44}) {
            Game game = fromCheckpoint(seed);
            Path folder = out.resolve("seed-" + seed);
            new AdvancedLab(game, 20, "EXPERT_COMPLET", seed).runLab(25, folder);
            assertTrue(game.hasCosmos(Cosmos.UNIVERSE),
                    () -> "Graine " + seed + " : BB=" + game.bigBangs()
                            + " cosmos=" + game.cosmosFormed()
                            + " but=" + game.shoppingList().target()
                            + " matiere=" + game.shoppingList().matter());
            assertTrue(Files.exists(folder.resolve("winner.save")),
                    "La victoire doit etre sauvegardee pour la graine " + seed);
        }
    }

    @Test
    void troisBotsJouentDepuisZeroAvecGrainesDistinctes(@TempDir Path out) throws Exception {
        String[] modes = {"EXPERT_COMPLET", "DEPENDANCES", "RECUPERATION"};
        for (int i = 0; i < modes.length; i++) {
            long seed = 42 + i;
            Game game = fresh(seed);
            new AdvancedLab(game, 20, modes[i], seed)
                    .runLab(3, out.resolve(modes[i]));
            assertTrue(game.state().stats().fusions() > 0,
                    modes[i] + " : aucune fusion apres trois heures simulees");
            assertTrue(game.state().timePlayed() >= 3 * 3600,
                    modes[i] + " : horloge simulee incorrecte");
        }
    }

    @Test
    void vingtQuatreProfilsTournentSurUneVraieSauvegarde(@TempDir Path out) throws Exception {
        for (IdleProfiles24.Profile profile : IdleProfiles24.PROFILES) {
            Game game = fromCheckpoint(profile.id() + 100);
            IdleProfiles24 bot = new IdleProfiles24(game, profile, profile.id() + 100, 20);
            Path folder = out.resolve("profil-" + profile.id());
            bot.execute(0.15, folder);
            assertTrue(game.state().started(), profile.name());
            assertTrue(bot.wallSeconds >= 540, profile.name());
            assertTrue(Files.exists(folder.resolve(
                    String.format("%02d_%s_late.csv", profile.id(), profile.name()))),
                    profile.name());
        }
    }

    @Test
    void profilsRestreintsRespectentLeursInterdictions(@TempDir Path out) throws Exception {
        for (String mode : new String[]{"SANS_IMPULSIONS", "SANS_COMETES", "SANS_TECH", "SANS_LISTE"}) {
            Game game = fromCheckpoint(42);
            AdvancedLab bot = new AdvancedLab(game, 20, mode, 42);
            bot.runLab(0.25, out.resolve(mode));
            if (mode.equals("SANS_IMPULSIONS")) assertEquals(0, bot.pulses);
            if (mode.equals("SANS_COMETES")) assertEquals(0, bot.caught);
            if (mode.equals("SANS_TECH")) {
                assertEquals(0, bot.collisionRuns);
                assertEquals(0, bot.probeRuns);
            }
            if (mode.equals("SANS_LISTE")) {
                assertFalse(game.isAutoFollowingList(),
                        "SANS_LISTE ne doit pas utiliser le suivi automatique de liste");
                assertFalse(game.isAutoFollowingMatter(),
                        "SANS_LISTE ne doit pas contourner la restriction via le suivi de matiere");
            }
        }
    }
}
