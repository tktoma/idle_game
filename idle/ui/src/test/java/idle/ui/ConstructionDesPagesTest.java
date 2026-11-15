package idle.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import idle.core.history.*;
import idle.ui.common.BurstView;
import idle.ui.hadrons.HadronsPage;
import idle.ui.probes.LootPane;
import idle.ui.vacuum.VoidPage;

import idle.core.Game;
import idle.ui.atoms.AtomsPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.collider.ColliderPage;
import idle.ui.darkmatter.DarkMatterPage;
import idle.ui.pages.*;
import idle.ui.automation.AutomationPage;
import idle.ui.universe.*;
import org.junit.jupiter.api.*;

/**
 * Chaque page et chaque panneau de la fenêtre se construisent comme {@link GameAppBase} le fait, sur une
 * partie neuve puis sur une partie avancée. Le constructeur d'une classe mère qui appelle une méthode de sa fille avant
 * que les champs de celle-ci soient posés (la 0.200 plantait ainsi au lancement, sur les dessins des cartes de
 * molécules) se voit ici, sans attendre le lancement du jeu.
 */
class ConstructionDesPagesTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void toutesLesPagesSeConstruisentSurUnePartieNeuve() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            construct(game);
            return null;
        });
    }

    @Test
    void toutesLesPagesSeConstruisentSurUnePartieAvancee() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.testFormUniverse();
            game.testAddUniverse();
            game.state().setSkinsOpen(true);
            game.tick(1);
            game.tick(1);
            construct(game);
            return null;
        });
    }

    private static void construct(Game game) {
        Settings settings = Settings.load(new NotificationsEtSignesTest.MemoryPreferences());
        assertNotNull(new AtomsPage(game, settings));
        assertNotNull(new AutomationPage(game));
        assertNotNull(new DarkMatterPage(game));
        assertNotNull(new ColliderPage(game));
        assertNotNull(new BigBangPage(game));
        assertNotNull(new UniversePage(game));
        assertNotNull(new VoidPage(game));
        assertNotNull(new HadronsPage(game));
        assertNotNull(new DimensionWindow(game, settings::freeCamera, settings::setFreeCamera));
        assertNotNull(new AchievementsPage(game));
        assertNotNull(new StatsPage(game, new PurchaseHistory(), new Pace()));
        assertNotNull(new ProfilePage(game, () -> null));
        assertNotNull(new SettingsPage(settings, () -> { }, () -> { }, NotificationsEtSignesPageTest.saves()));
        assertNotNull(new SkinsPage(game));
        assertNotNull(new ToastPane());
        assertNotNull(new ChronoCorner(game));
        assertNotNull(new Notifier(game));
        assertNotNull(new TransitionPane(game));
        assertNotNull(new BurstView());
        assertNotNull(new Chrome());
        assertNotNull(new TransitionSkinArt());
        assertNotNull(new CursorSkinArt.Trail());
        assertNotNull(new AbsencePane(() -> { }));
        assertNotNull(new PopupPane(game));
        assertNotNull(new EndPane(game));
        assertNotNull(new FinalePane(game));
        assertNotNull(new LootPane(game, () -> { }));
        assertNotNull(new HelpPane(game, settings));
    }
}
