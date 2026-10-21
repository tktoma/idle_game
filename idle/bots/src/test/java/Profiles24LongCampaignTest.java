import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.molecules.Molecule;
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
 * Contrôle comparatif de 24 heures SIMULÉES (premier univers seulement).
 *
 * Le profil 1 sert de témoin rapide ; 11, 12 et 17 sont les trois profils
 * historiquement bloqués. Chaque essai reprend exactement la graine utilisée
 * lors de la campagne des 24 profils de huit heures. Un profil lent ne doit
 * pas être déclaré gagnant simplement parce que le programme n'a pas planté.
 */
class Profiles24LongCampaignTest {
    private static Game checkpoint(long seed) throws Exception {
        GameState state = SaveCodec.read(Files.readString(Path.of(
                "sauvegardes/reference/checkpoint_before_eighth.save"))).state();
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
    }

    @Test
    void profilsRapideEtHistoriqueSurVingtQuatreHeures(@TempDir Path out) throws Exception {
        int[] ids = {1, 11, 12, 17};
        int completed = 0;
        for (int id : ids) {
            IdleProfiles24.Profile p = IdleProfiles24.PROFILES[id - 1];
            long seed = 300L + id;
            Game game = checkpoint(seed);
            int startBangs = game.bigBangs();
            IdleProfiles24 bot = new IdleProfiles24(game, p, seed, 60);
            Path dir = out.resolve(String.format("%02d_%s", id, p.name()));
            bot.execute(24, dir);
            boolean win = game.hasCosmos(Cosmos.UNIVERSE);
            // Le test long doit mesurer un vrai jalon, et pas seulement
            // l'absence de crash (comme les tests courts).
            Cosmos minimum = id == 1 ? Cosmos.CLUSTER : Cosmos.GALAXY;
            assertTrue(win || game.hasCosmos(minimum),
                    p.name() + " doit atteindre au moins " + minimum
                    + " apres 24 h ; objectif=" + game.shoppingList().target());
            if (win) completed++;
            assertTrue(game.state().started(), "Le jeu doit rester demarre : " + p.name());
            assertTrue(bot.actions > 0, "Le bot doit prendre des decisions : " + p.name());
            assertTrue(win || game.bigBangs() >= startBangs,
                    "Aucun Big Bang ne doit etre perdu sans victoire : " + p.name());
            Path history = dir.resolve(String.format("%02d_%s_late.csv", id, p.name()));
            assertTrue(Files.exists(history), "Historique manquant : " + p.name());
            assertTrue(Files.size(history) > 100, "Historique tronque : " + p.name());
            Files.writeString(dir.resolve("end.save"), SaveCodec.write(game.state(), 0));
            System.out.printf(
                    "PROFIL24_LONG,%d,%s,heures=%.4f,enligne=%.4f,offline=%.4f,decisions=%d,bb=%d,astres=%d,cosmos=%d,victoire=%s,fusions=%d,explosions=%d,objectif=%s,space=%s,free=%s,automatisation=%s%n",
                    id, p.name(), bot.wallSeconds/3600, bot.onlineSeconds/3600,
                    bot.creditedOfflineSeconds/3600, bot.actions,
                    game.bigBangs(), game.bodiesMature(), game.cosmosFormed(), win,
                    game.state().stats().fusions(), game.state().explosions(),
                    game.shoppingList().target().replace(',', ';'),
                    game.state().space(), game.freeSpace(), game.isMoleculeAutomationEnabled());
            for (var shortage : game.shoppingList().matter().entrySet()) {
                Molecule.State state = shortage.getKey();
                System.out.printf(
                        "PROFIL24_LONG_MATTER,%d,%s,manque=%d,reuni=%d,sortes=%d%n",
                        id, state, shortage.getValue(), game.gatheredInState(state),
                        game.sortsInState(state));
            }
        }
        System.out.printf("PROFIL24_LONG_BILAN,profils=%d,victoires=%d%n", ids.length, completed);
    }
}
