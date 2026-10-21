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
 * Campagne de validation des 24 comportements de joueur.
 *
 * Deux origines indépendantes : la partie neuve et la sauvegarde avant le
 * huitième Big Bang. Le temps de décision est suffisamment fin pour ne pas
 * court-circuiter les automations de matière (rattrapage toutes les 5 s).
 *
 * Attention : certains profils sont volontairement peu attentifs. L'objectif
 * de ce premier passage est l'absence de crash, la continuité des sauvegardes,
 * des actions réelles et le bilan de progression pour chaque profil. Une
 * victoire dans un délai fixe n'est PAS un invariant pour ces profils.
 */
class Profiles24CampaignTest {
    private static Game fresh(long seed) {
        Game g = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
        g.start();
        return g;
    }

    private static Game late(long seed) throws Exception {
        var text = Files.readString(Path.of("sauvegardes/reference/checkpoint_before_eighth.save"));
        GameState state = SaveCodec.read(text).state();
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
    }

    @Test
    void profilsPrestigeEtEpargneNeSabotentPasLeCosmos() throws Exception {
        Game g = late(42);
        g.state().setBigBangs(8);
        assertFalse(IdleProfiles24.allowExtraPrestige(g),
                "Le huitieme Big Bang marque la fin des explosions supplementaires");
        Game sky = fresh(43);
        sky.state().setBigBangs(5);
        assertFalse(IdleProfiles24.allowExtraPrestige(sky),
                "Le ciel incomplet du sixieme Big Bang doit etre preserve");
    }

    @Test
    void tousLesProfilsProgressentDepuisLeDebut(@TempDir Path out) throws Exception {
        int visited = 0;
        for (IdleProfiles24.Profile p : IdleProfiles24.PROFILES) {
            long seed = 200 + p.id();
            Game g = fresh(seed);
            var bot = new IdleProfiles24(g, p, seed, 60);
            bot.execute(2, out.resolve("fresh").resolve(p.name()));
            assertTrue(g.state().started(), p.name() + " : partie non demarree");
            assertTrue(g.state().timePlayed() > 0, p.name() + " : temps de jeu immobile");
            assertTrue(bot.actions > 0, p.name() + " : aucun tour de decision");
            Path csv = out.resolve("fresh").resolve(p.name()).resolve(
                    String.format("%02d_%s_late.csv", p.id(), p.name()));
            assertTrue(Files.size(csv) > 60, p.name() + " : bilan CSV vide");
            System.out.printf("PROFIL24_FRESH,%d,%s,actions=%d,fusions=%d,explosions=%d,bb=%d,heures=%.3f%n",
                    p.id(), p.name(), bot.actions, g.state().stats().fusions(),
                    g.state().explosions(), g.bigBangs(), g.state().timePlayed() / 3600);
            visited++;
        }
        assertEquals(24, visited);
    }

    @Test
    void tousLesProfilsJouentUneCampagneAvancee(@TempDir Path out) throws Exception {
        int visited = 0;
        int winners = 0;
        for (IdleProfiles24.Profile p : IdleProfiles24.PROFILES) {
            long seed = 300 + p.id();
            Game g = late(seed);
            int bbBefore = g.bigBangs();
            int bodiesBefore = g.bodiesMature();
            int cosmosBefore = g.cosmosFormed();
            var bot = new IdleProfiles24(g, p, seed, 60);
            bot.execute(8, out.resolve("late").resolve(p.name()));
            assertTrue(g.state().started(), p.name() + " : sauvegarde perdue");
            assertTrue(bot.actions > 0, p.name() + " : aucune decision en 8 h");
            boolean won = g.hasCosmos(Cosmos.UNIVERSE);
            // Former l'univers remet volontairement les compteurs a zero :
            // une victoire n'est pas une regression de Big Bangs/astres/cosmos.
            assertTrue(won || g.bigBangs() >= bbBefore,
                    p.name() + " : Big Bangs perdus sans formation d'univers");
            assertTrue(won || g.bodiesMature() >= bodiesBefore || g.bigBangs() > bbBefore,
                    p.name() + " : perte des astres sans nouveau Big Bang");
            assertTrue(won || g.cosmosFormed() >= cosmosBefore,
                    p.name() + " : cosmos perdu sans formation d'univers");
            if (won) winners++;
            System.out.printf(
                    "PROFIL24_LATE,%d,%s,actions=%d,bb=%d,astres=%d,cosmos=%d,victoire=%s,enligne=%.3f,offline=%.3f%n",
                    p.id(), p.name(), bot.actions, g.bigBangs(), g.bodiesMature(),
                    g.cosmosFormed(), won, bot.onlineSeconds / 3600,
                    bot.creditedOfflineSeconds / 3600);
            visited++;
        }
        assertEquals(24, visited);
        System.out.printf("PROFIL24_BILAN,valides=%d,victoiresEn8h=%d%n", visited, winners);
    }
}
