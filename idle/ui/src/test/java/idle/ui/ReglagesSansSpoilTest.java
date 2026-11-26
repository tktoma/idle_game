package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.pages.SettingsPage;
import org.junit.jupiter.api.*;

/**
 * Version 0.318 : les réglages ne parlent que de ce que le joueur a déjà vu. Au début, ni Big Bang, ni boucle, ni
 * anomalies du ciel ; une fois vus, tout revient.
 */
class ReglagesSansSpoilTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void leSouvenirSeGardeDansLesReglages() {
        NotificationsEtSignesTest.MemoryPreferences store = new NotificationsEtSignesTest.MemoryPreferences();
        Settings settings = Settings.load(store);
        Game game = new Game(1L);
        game.start();
        game.state().setBigBangs(1);
        settings.seen().note(game);
        Settings again = Settings.load(store);
        assertTrue(again.seen().has(Seen.BIG_BANG));
        assertTrue(again.seen().has(Seen.EXPLOSION), "le Big Bang vaut pour l'explosion");
        assertFalse(again.seen().has(Seen.UNIVERSE));
    }

    @Test
    void lesRaccourcisDeLaSuiteAttendent() {
        assertEquals(Seen.PROBES, SettingsPage.of(Shortcuts.Action.CLAIM));
        assertEquals(Seen.BIG_BANG, SettingsPage.of(Shortcuts.Action.PULSE));
        assertNull(SettingsPage.of(Shortcuts.Action.FUSE));
    }

    @Test
    void auDebutLesReglagesNeDevoilentRien() {
        FxTestSupport.fx(() -> {
            Settings settings = Settings.load(new NotificationsEtSignesTest.MemoryPreferences());
            SettingsPage page = new SettingsPage(settings, () -> { }, () -> { }, NotificationsEtSignesPageTest.saves());
            page.refresh();
            assertFalse(page.shownBlocks().contains("Événements du ciel"));
            assertFalse(page.shownBlocks().contains("Touches de la boucle"));
            assertFalse(page.shownBlocks().contains("Confirmations"));
            assertTrue(page.shownBlocks().contains("Pause"));
            String sounds = page.explanation("Sons et musique");
            for (String word : new String[] {"Big Bang", "explosion", "univers", "sonde", "vide", "collision", "hadron", "chrono"}) {
                assertFalse(sounds.contains(word), word + " : " + sounds);
            }
            Game game = new Game(1L);
            game.start();
            game.state().setBigBangs(1);
            settings.seen().note(game);
            page.refresh();
            assertTrue(page.shownBlocks().contains("Événements du ciel"));
            assertTrue(page.shownBlocks().contains("Confirmations"));
            assertTrue(page.explanation("Sons et musique").contains("Big Bang"));
            assertFalse(page.shownBlocks().contains("Touches de la boucle"), "la boucle attend sa dimension");
            return null;
        });
    }
}
