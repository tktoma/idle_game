import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Garde les bots cohérents avec le jeu : s'ils ne compilent plus, ce test ne compile pas ; s'ils
 * plantent sur le moteur actuel, ou si une sauvegarde de référence ne se relit plus, il échoue.
 * Il ne mesure rien : les campagnes se lancent avec run.py.
 */
class BotsTest {

    private static List<Path> saves(String folder) throws Exception {
        try (Stream<Path> files = Files.list(Path.of("sauvegardes", folder))) {
            return files.filter(path -> path.toString().endsWith(".save")).sorted().toList();
        }
    }

    private static Game load(Path save) throws Exception {
        GameState state = SaveCodec.read(Files.readString(save)).state();
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(42));
    }

    @Test
    void toutesLesSauvegardesDesBotsSeRelisent() throws Exception {
        int count = 0;
        for (String folder : List.of("reference", "profils_bloques", "victoires_corrigees", "victoires_historiques")) {
            for (Path save : saves(folder)) {
                Game game = load(save);
                assertTrue(game.state().started(), save + " : partie démarrée");
                count++;
            }
        }
        assertEquals(32, count, "6 de référence, 3 bloquées, 2 corrigées, 21 victoires historiques");
    }

    @Test
    void lesVictoiresSontToujoursDesVictoires() throws Exception {
        VerifyVictory.main(new String[] {"sauvegardes/reference/checkpoint_universe_5s.save"});
        for (Path save : saves("victoires_historiques")) VerifyVictory.main(new String[] {save.toString()});
        for (Path save : saves("victoires_corrigees")) VerifyVictory.main(new String[] {save.toString()});
    }

    @Test
    void leBotV2TourneSurLeMoteurActuel(@TempDir Path out) throws Exception {
        AdvancedLab.main(new String[] {"DEPENDANCES", "sauvegardes/reference/checkpoint_before_eighth.save", "0.05", "20", "42", out.toString()});
        assertTrue(Files.exists(out.resolve("result.csv")));
        assertFalse(Files.readString(out.resolve("timeline.csv")).isBlank());
    }

    @Test
    void lesProfilsTournentSurLeMoteurActuel(@TempDir Path out) throws Exception {
        IdleProfiles24.main(new String[] {"19", "sauvegardes/reference/checkpoint_before_eighth.save", "0.05", "20", out.toString(), "42"});
        FreshPhaseProfiles.main(new String[] {"1", "0.05", "20", out.resolve("debut").toString(), "42"});
        try (Stream<Path> files = Files.list(out)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().endsWith(".csv")));
        }
    }
}
