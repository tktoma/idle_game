package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.math.BigNum;
import java.time.LocalTime;
import java.util.List;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

/**
 * Version 0.200 : les huit thèmes nouveaux passent par le filtre sans erreur et en diffèrent du thème sombre ; leurs
 * conditions et celles des notations nouvelles s'ouvrent au bon moment, et pas avant.
 */
class ThemesEtNotationsNouveauxTest {

    private static final List<Settings.Theme> NEW = List.of(Settings.Theme.INK, Settings.Theme.CYANOTYPE, Settings.Theme.OLD_FILM,
            Settings.Theme.THERMAL, Settings.Theme.COLORBLIND, Settings.Theme.MOON, Settings.Theme.FOREST, Settings.Theme.BLAZE);

    @Test
    void chaqueThemeNouveauFiltreLaFenetre() {
        assertEquals(19, Settings.Theme.values().length, "onze thèmes d'avant, huit de la 0.200");
        assertNull(Settings.Theme.COLORBLIND.unlock(), "le thème Daltonien est libre : de l'accessibilité");
        for (Settings.Theme theme : NEW) {
            assertFalse(theme.isPlain(), theme.label());
            if (theme != Settings.Theme.COLORBLIND) assertNotNull(theme.unlock(), theme.label() + " se débloque par une énigme");
        }
        FxTestSupport.start();
        for (double size : new double[] {320, 1280}) {
            FxTestSupport.fx(() -> {
                Pane pane = new Pane();
                pane.resize(size, size * 0.75);
                ThemeFilter filter = new ThemeFilter(pane);
                filter.apply(Settings.Theme.DARK);
                assertNull(pane.getEffect(), "le thème sombre ne filtre rien");
                for (Settings.Theme theme : NEW) {
                    filter.apply(theme);
                    Effect effect = pane.getEffect();
                    assertNotNull(effect, theme.label());
                    // Les thèmes voilés finissent par le mélange « écran » du voile ; les autres non.
                    assertEquals(theme.shade() != null, effect instanceof Blend blend && blend.getMode() == javafx.scene.effect.BlendMode.SCREEN, theme.label());
                    filter.fit();
                }
                filter.apply(Settings.Theme.CYANOTYPE);
                Blend screen = (Blend) pane.getEffect();
                assertEquals(javafx.scene.paint.Color.web("#1a3a6b"), ((javafx.scene.effect.ColorInput) screen.getTopInput()).getPaint(), "le bleu de Prusse");
                assertEquals(size, ((javafx.scene.effect.ColorInput) screen.getTopInput()).getWidth(), 1e-9, "le voile couvre la zone");
                return null;
            });
        }
    }

    @Test
    void lesConditionsDesThemesEtNotationsNouveauxSOuvrentAuBonMoment() {
        Game game = new Game(5L);
        game.start();
        Settings settings = Settings.load(new NotificationsEtSignesTest.MemoryPreferences());
        Cosmetics cosmetics = new Cosmetics(game, settings);
        cosmetics.clock = () -> LocalTime.of(14, 0);
        cosmetics.frame(0.1, false);
        for (Cosmetic cosmetic : Cosmetic.values()) assertFalse(settings.hasCosmetic(cosmetic), cosmetic + " fermé au départ");
        for (Settings.Theme theme : NEW) assertEquals(theme == Settings.Theme.COLORBLIND, settings.isOpen(theme), theme.label());
        assertFalse(settings.isOpen(Settings.Notation.BINARY));

        game.state().setExplosions(99);
        game.state().setCometsCaught(9);
        game.state().setParticles(BigNum.of(1023));
        game.state().setDarkMatter(BigNum.of(254));
        game.noteNotificationsShown(99);
        assertTrue(cosmetics.frame(0.1, false).isEmpty(), "un de moins partout : rien");
        game.state().setExplosions(100);
        game.state().setCometsCaught(10);
        game.state().setParticles(BigNum.of(1024));
        game.state().setDarkMatter(BigNum.of(255));
        game.state().addCompletedChallenge("challenge_rust");
        game.noteNotificationsShown(100);
        List<Cosmetic> opened = cosmetics.frame(0.1, false);
        assertEquals(List.of(Cosmetic.CYANOTYPE, Cosmetic.MOON, Cosmetic.BLAZE, Cosmetic.BINARY, Cosmetic.HEX, Cosmetic.MORSE), opened);
        assertTrue(settings.isOpen(Settings.Theme.BLAZE));
        assertTrue(settings.isOpen(Settings.Notation.HEX));
        game.state().setParticles(BigNum.of(1, 30));
        assertEquals(List.of(Cosmetic.SI), cosmetics.frame(0.1, false), "un quetta de particules");
        // Le chrono plein : l'étage acheté et la réserve au bord.
        game.state().setSeedUpgradeLevel(Game.SeedUpgrade.CHRONO.ordinal(), 1);
        game.state().setChronoStored(game.chronoCapacity() - 1);
        assertTrue(cosmetics.frame(0.1, false).isEmpty(), "pas plein : rien");
        game.state().setChronoStored(game.chronoCapacity());
        assertEquals(List.of(Cosmetic.HEARTBEATS), cosmetics.frame(0.1, false));
        // Le Big Bang de nuit : un de plus depuis l'image d'avant, avant six heures ; de jour, rien ; plusieurs d'un coup, rien.
        game.state().setBigBangs(1);
        assertTrue(cosmetics.frame(0.1, false).isEmpty(), "de jour");
        cosmetics.clock = () -> LocalTime.of(3, 30);
        game.state().setBigBangs(4);
        assertTrue(cosmetics.frame(0.1, false).isEmpty(), "trois d'un coup : un chargement, pas un Big Bang");
        game.state().setBigBangs(5);
        assertEquals(List.of(Cosmetic.OLD_FILM), cosmetics.frame(0.1, false));
        // Une notation ouverte se choisit et s'écrit dans les réglages ; refermée, la scientifique revient.
        settings.setNotation(Settings.Notation.HEX);
        assertEquals(Settings.Notation.HEX, settings.notation());
        settings.lock(Cosmetic.HEX);
        assertEquals(Settings.Notation.SCIENTIFIC, settings.notation());
        settings.setTheme(Settings.Theme.COLORBLIND);
        settings.lockCosmetics();
        assertEquals(Settings.Theme.COLORBLIND, settings.theme(), "un thème libre reste choisi");
        idle.ui.common.Format.setNotation(Settings.Notation.SCIENTIFIC);
    }
}
