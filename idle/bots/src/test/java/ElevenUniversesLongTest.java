import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.save.SaveCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test long (version 0.152), à part de la suite rapide comme les campagnes des profils : le Speedrunner (profil 1,
 * graine 201, pas de 20 s, celui de l'audit depuis zéro) joue une partie neuve jusqu'à onze univers formés, avec la
 * graine, les bulles de l'arbre, les hadrons et l'automatisation des dimensions ({@link UniverseTreePolicy}). Il dure
 * de l'ordre d'une heure. {@code -Didle.onzeUnivers.heures} change le plafond en heures simulées (1 000 par défaut),
 * {@code -Didle.onzeUnivers.univers} le nombre d'univers visés (11).
 */
class ElevenUniversesLongTest {

    static final double HOURS = Double.parseDouble(System.getProperty("idle.onzeUnivers.heures", "1000"));
    static final int TARGET = Integer.getInteger("idle.onzeUnivers.univers", FreshUniverseAudit.MAX_UNIVERSES);

    @Test
    void leSpeedrunnerFormeOnzeUniversDepuisZero(@TempDir Path out) throws Exception {
        IdleProfiles24.Profile profile = IdleProfiles24.PROFILES[0];
        long seed = 201;
        Game game = Bots.fresh(seed);
        IdleProfiles24 bot = new IdleProfiles24(game, profile, seed, 20);
        bot.targetUniverses = TARGET;
        bot.execute(HOURS, out);
        Files.writeString(out.resolve("end.save"), SaveCodec.write(game.state(), 0));
        StringBuilder table = new StringBuilder();
        double before = 0;
        for (int u = 0; u < bot.universeTimes.size(); u++) {
            double at = bot.universeTimes.get(u);
            table.append(String.format(Locale.ROOT, "ONZE_UNIVERS,univers=%d,forme_h=%.4f,duree_h=%.4f%n", u + 1, at / 3600,
                    (at - before) / 3600));
            before = at;
        }
        System.out.print(table);
        System.out.println("ONZE_UNIVERS_ARBRE," + bot.tree.summary(game));
        assertTrue(NextUniversePolicy.reached(game, TARGET), TARGET + " univers en " + HOURS + " h : " + game.universes()
                + " formés, instants " + bot.universeHours() + " ; arbre : " + bot.tree.summary(game));
        assertEquals(TARGET, bot.universeTimes.size(), "chaque univers a son instant de formation");
        for (int u = 1; u < bot.universeTimes.size(); u++) {
            assertTrue(bot.universeTimes.get(u) > bot.universeTimes.get(u - 1), "les univers se forment l'un après l'autre");
        }
        assertTrue(game.seedStage() >= Math.min(Game.SEED_STAGES, TARGET), "la graine a ouvert chaque univers : palier " + game.seedStage());
        if (TARGET >= 4) assertTrue(game.isTreeBubbleBought(TreeBubble.SEED), "la bulle de la graine, au moins");
    }
}
