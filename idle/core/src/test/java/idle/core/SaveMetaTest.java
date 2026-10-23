package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/** Version 0.127 : le nom et la version du jeu qu'une sauvegarde porte, et son aperçu. */
class SaveMetaTest {

    @Test
    void leNomEtLaVersionTraversentLExportEtLAperçu() throws Exception {
        Game game = new Game();
        game.start();
        game.state().setTimePlayed(3600);
        GameTestSupport.bangs(game, 2);
        String text = SaveCodec.export(game.state(), 1_000, new SaveCodec.Meta("Avant = le\nBig Bang", "0.127"));
        SaveCodec.Preview preview = SaveCodec.preview(text);
        assertEquals("Avant = le Big Bang", preview.meta().label(), "les retours à la ligne deviennent des espaces");
        assertEquals("0.127", preview.meta().gameVersion());
        assertEquals(3600, preview.timePlayed(), 1e-9);
        assertEquals(2, preview.bigBangs());
        assertEquals(1_000, preview.savedAt());
        assertEquals(SaveCodec.VERSION, preview.format());
    }

    @Test
    void uneSauvegardeSansCesRenseignementsSeLitToujours() throws Exception {
        Game game = new Game();
        game.start();
        SaveCodec.Save save = SaveCodec.read(SaveCodec.write(game.state(), 5));
        assertEquals(SaveCodec.Meta.NONE, save.meta());
        assertTrue(SaveCodec.preview(SaveCodec.write(game.state(), 5)).meta().label().isEmpty());
    }

    @Test
    void unNomTropLongEstCoupe() {
        assertEquals(SaveCodec.Meta.MAX_LABEL, new SaveCodec.Meta("x".repeat(100), "").label().length());
    }
}
