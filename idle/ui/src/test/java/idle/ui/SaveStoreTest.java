package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.save.SaveCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Version 0.127 : les points de reprise nommés et la mise de côté. Sans affichage JavaFX. */
class SaveStoreTest {

    private static Game played(double seconds) {
        Game game = new Game();
        game.start();
        game.state().setTimePlayed(seconds);
        return game;
    }

    @Test
    void unPointSEnregistreSeRelitEtSeReprend(@TempDir Path folder) throws Exception {
        SaveStore store = SaveStore.of(List.of("--dossier=" + folder));
        Game game = played(1000);
        assertFalse(store.checkpoint(1).used());
        assertTrue(store.saveCheckpoint(game, 1, "Avant le Big Bang"));
        SaveStore.Checkpoint point = store.checkpoint(1);
        assertTrue(point.used());
        assertEquals("Avant le Big Bang", point.preview().meta().label());
        assertEquals(Version.CURRENT, point.preview().meta().gameVersion());
        assertEquals(1000, point.preview().timePlayed(), 1e-9);
        assertTrue(SaveStore.describe(point.preview()).startsWith("« Avant le Big Bang » · "));

        // La partie avance, puis le point est repris : la partie d'avant est mise de côté.
        game.state().setTimePlayed(5000);
        store.loadCheckpoint(game, 1);
        assertEquals(1000, game.state().timePlayed(), 1e-9);
        SaveStore.Checkpoint undo = store.checkpoint(SaveStore.UNDO_SLOT);
        assertTrue(undo.used());
        assertEquals(5000, undo.preview().timePlayed(), 1e-9, "la reprise par erreur se défait");
        store.loadCheckpoint(game, SaveStore.UNDO_SLOT);
        assertEquals(5000, game.state().timePlayed(), 1e-9);
        assertEquals(1000, store.checkpoint(SaveStore.UNDO_SLOT).preview().timePlayed(), 1e-9,
                "reprendre la mise de côté échange les deux parties : rien ne se perd");

        assertTrue(store.deleteCheckpoint(1));
        assertFalse(store.checkpoint(1).used());
        assertThrows(SaveCodec.Unreadable.class, () -> store.loadCheckpoint(game, 1));
        assertFalse(store.saveCheckpoint(game, SaveStore.MAX_CHECKPOINTS + 1, "hors des cases"));
    }

    @Test
    void unPointIllisibleNeChangeRien(@TempDir Path folder) throws Exception {
        SaveStore store = SaveStore.of(List.of("--dossier=" + folder));
        Game game = played(777);
        assertTrue(store.saveCheckpoint(game, 2, ""));
        Path file = folder.resolve("points-partie").resolve("point-2.sav");
        Files.writeString(file, "pas une sauvegarde");
        assertFalse(store.checkpoint(2).used());
        assertFalse(store.checkpoint(2).problem().isEmpty());
        assertThrows(SaveCodec.Unreadable.class, () -> store.loadCheckpoint(game, 2));
        assertEquals(777, game.state().timePlayed(), 1e-9);
        assertFalse(store.checkpoint(SaveStore.UNDO_SLOT).used(), "rien n'a été mis de côté pour rien");
    }

    @Test
    void leProfilDeTestASesPropresPoints(@TempDir Path folder) {
        SaveStore real = SaveStore.of(List.of("--dossier=" + folder));
        SaveStore test = SaveStore.of(List.of("--dossier=" + folder, "--test"));
        assertTrue(test.saveCheckpoint(played(10), 1, "essai"));
        assertFalse(real.checkpoint(1).used());
    }

    @Test
    void lesVersionsSeComparentPartieParPartie() {
        assertTrue(SaveStore.compareVersions("0.099", "0.127") < 0);
        assertTrue(SaveStore.compareVersions("0.127", "0.126") > 0);
        assertEquals(0, SaveStore.compareVersions("0.127", "0.127"));
    }
}
