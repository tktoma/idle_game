package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.save.SaveCodec;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Version 0.127 : le rapport de bug dit la session et où en est la partie, sans rien de personnel. Sans affichage. */
class BugReportTest {

    @AfterEach
    void forget() {
        Crash.clear();
    }

    @Test
    void leRapportDitLaSessionEtLaPartie(@TempDir Path folder) throws Exception {
        SaveStore store = SaveStore.of(List.of("--dossier=" + folder));
        Game game = new Game(123456789L);
        game.start();
        game.state().setTimePlayed(3600);
        store.save(game);
        String report = BugReport.of(game, store, new BugReport.Session(123456789L, "Big Bang", true));

        assertTrue(report.contains("Idle " + Version.CURRENT));
        assertTrue(report.contains("graine du hasard 123456789"));
        assertTrue(report.contains("onglet Big Bang"));
        assertTrue(report.contains("profil de test"));
        assertTrue(report.contains("acte : 1"));
        assertTrue(report.contains("Jalon : "));
        assertTrue(report.contains("Paliers de Big Bang : "));
        assertTrue(report.contains("En cours : rien de particulier"));
        assertTrue(report.contains("Aucune erreur depuis le lancement."));
        assertFalse(report.contains("Chrono :"), "pas de chrono avant son étage");
        // La partie est au bout, et se relit.
        String exported = report.substring(report.indexOf("Partie :\n") + "Partie :\n".length()).strip();
        assertEquals(3600, SaveCodec.read(exported).state().timePlayed(), 1e-9);
    }

    /** Version 0.139 : le chrono accélère tout le jeu ; le rapport dit son réglage et ce qu'il garde. */
    @Test
    void leRapportDitLeChrono(@TempDir Path folder) {
        SaveStore store = SaveStore.of(List.of("--dossier=" + folder));
        Game game = new Game(7L);
        game.start();
        game.testFormUniverse();
        while (game.universes() < 4) assertTrue(game.testAddUniverse());
        game.state().setDurationShards(1e7);
        assertTrue(game.buySeedUpgrade(Game.SeedUpgrade.CHRONO));
        game.state().setChronoStored(90);
        game.setChronoSpeed(1.5);
        String report = BugReport.of(game, store, new BugReport.Session(7L, "Univers", false));
        assertTrue(report.contains("Chrono : ×1.5 · gardé "), report);
    }

    @Test
    void leRapportNeDitNiCheminNiCompte(@TempDir Path folder) {
        SaveStore store = SaveStore.of(List.of("--dossier=" + folder));
        Game game = new Game(1L);
        game.start();
        store.save(game);
        Crash.note(new IllegalStateException("cassé dans " + System.getProperty("user.home") + "/jeu"));
        String report = BugReport.of(game, store, new BugReport.Session(1L, "Particules", false));
        assertFalse(report.contains(folder.toString()), "le dossier de sauvegarde n'apparaît pas");
        String home = System.getProperty("user.home");
        if (home != null && home.length() > 2) assertFalse(report.contains(home), "le dossier personnel devient « ~ »");
        assertTrue(report.contains("cassé dans ~/jeu"));
        assertFalse(report.contains("profil de test"));
    }

    @Test
    void leNomDuCompteNAbimePasLaPartieJointe(@TempDir Path folder) throws Exception {
        String name = System.getProperty("user.name");
        // Un nom qui se trouve à coup sûr dans la ligne codée d'une partie (l'en-tête gzip en est plein).
        System.setProperty("user.name", "AAAA");
        try {
            SaveStore store = SaveStore.of(List.of("--dossier=" + folder));
            Game game = new Game(2L);
            game.start();
            game.state().setTimePlayed(77);
            Crash.note(new IllegalStateException("le compte AAAA a cassé"));
            String report = BugReport.of(game, store, new BugReport.Session(2L, "Particules", false));
            assertTrue(report.contains("le compte <compte> a cassé"));
            String exported = report.substring(report.indexOf("Partie :\n") + "Partie :\n".length()).strip();
            assertTrue(exported.contains("AAAA"), "la partie codée garde ses lettres");
            assertEquals(77, SaveCodec.read(exported).state().timePlayed(), 1e-9);
        } finally {
            if (name != null) System.setProperty("user.name", name);
        }
    }

    @Test
    void laTraceEstCourte() {
        StringBuilder trace = new StringBuilder();
        for (int i = 0; i < 40; i++) trace.append("\tat ligne").append(i).append('\n');
        String kept = BugReport.shortTrace(trace.toString());
        assertEquals(BugReport.TRACE_LINES + 1, kept.split("\n").length);
        assertTrue(kept.endsWith("… (28 lignes de plus)\n"));
    }
}
