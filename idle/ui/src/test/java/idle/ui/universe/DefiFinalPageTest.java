package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import idle.core.history.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverseBase.SeedUpgrade;
import idle.core.GameUniverseBase.TreeBubble;
import idle.ui.*;
import idle.ui.pages.StatsPage;
import java.util.List;
import javafx.scene.Scene;
import org.junit.jupiter.api.*;

/**
 * Le défi final, sous Xvfb. Sans la bulle des racines, la carte des dimensions ne montre pas de palier ;
 * avec elle, chaque point se remplit de vert selon son avancée (en puissances de dix), tout vert au palier atteint. Les
 * dix atteints pendant qu'on joue, la fin se joue sur la carte (les traits verdissent, le point du milieu grandit), puis
 * l'écran de fin ; « Revoir la fin » la rejoue ; une partie déjà finie au chargement ne la rejoue pas. Les statistiques
 * et la note de version.
 */
class DefiFinalPageTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    /** Une partie au dixième univers. */
    private static Game tenth() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 9; k++) assertTrue(game.testAddUniverse());
        return game;
    }

    private static void openChallenge(Game game) {
        game.state().setSeedUpgradeLevel(SeedUpgrade.ROOT_BUBBLE.ordinal(), 1);
        game.state().setTreeBubbleBought(TreeBubble.ROOTS.ordinal(), true);
    }

    /** Toutes les dimensions à leur palier. */
    private static void reachAll(Game game) {
        game.state().setDimensionShardsEarned(Game.finalGoal(1));
        game.state().setPlaneShardsEarned(Game.finalGoal(2));
        game.state().setVolumeShardsEarned(Game.finalGoal(3));
        game.state().setDurationShardsEarned(Game.finalGoal(4));
        game.state().setLoopShardsEarned(Game.finalGoal(5));
        game.state().setBraneShardsEarned(Game.finalGoal(6));
        game.state().setClickerShardsEarned(Game.finalGoal(7));
        game.state().setGridShardsEarned(Game.finalGoal(8));
        game.state().setMirrorShardsEarned(Game.finalGoal(9));
        game.state().setMixShardsEarned(Game.finalGoal(10));
    }

    private static UniversePage page(Game game) {
        UniversePage page = new UniversePage(game);
        new Scene(page, 1200, 760);
        page.select(UniversePage.DIMENSIONS);
        page.refresh();
        page.frame(0.016, true);
        return page;
    }

    @Test
    void laCarteSeRemplitDeVertUneFoisLaBulleAchetee() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            UniversePage page = page(game);
            DimensionsView map = page.dimensionsView();
            assertFalse(map.isFinalOpen(), "sans la bulle des racines, pas de palier");
            assertEquals(0, map.filled(1));
            assertFalse(page.replayFinaleButton().isVisible());
            openChallenge(game);
            game.state().setDimensionShardsEarned(1000);
            game.state().setPlaneShardsEarned(Game.finalGoal(2));
            page.refresh();
            assertTrue(map.isFinalOpen());
            assertEquals(Math.log10(1001) / Math.log10(1 + Game.finalGoal(1)), map.filled(1), 1e-9, "en puissances de dix");
            assertEquals(1, map.filled(2), "au palier, tout vert");
            assertTrue(map.progressText(2).startsWith("Palier atteint : "), map.progressText(2));
            assertTrue(map.progressText(1).startsWith("1000 / "), map.progressText(1));
            assertEquals("1 / 10 palier", map.tiersText());
            assertTrue(page.dimensionsLineText().startsWith("Le défi final : 1 / 10 paliers atteints"), page.dimensionsLineText());
            return null;
        });
    }

    /** Version 0.312 : les univers dans le bandeau, la carte des dimensions seule, sans sa ligne d'infos. */
    @Test
    void lesUniversSontDansLeBandeauEtLaCarteEstSeule() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            UniversePage page = page(game);
            assertEquals(String.valueOf(game.universes()), page.head().value().getText());
            // Version 0.317 : la ligne du dessous passe au survol du nombre.
            assertEquals("", page.head().sub().getText());
            assertTrue(page.head().value().getTooltip().getText().contains("matière noire"), page.head().value().getTooltip().getText());
            page.select(UniversePage.MAIN);
            assertFalse(page.bare(), "la sous-page Univers garde le bandeau");
            page.select(1);
            assertTrue(page.bare(), "la carte des dimensions est seule");
            assertFalse(page.dimensionsLineVisible(), "sans sa ligne d'infos");
            return null;
        });
    }

    @Test
    void lesDixPaliersJouentLaFinPuisLEcranDeFin() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            openChallenge(game);
            UniversePage page = page(game);
            FinalePane finale = new FinalePane(game);
            page.setOnFinale(finale::open);
            assertFalse(page.checkFinale(), "le premier relevé ne lance rien");
            page.select(UniversePage.MAIN);
            reachAll(game);
            game.tick(0.05);
            assertTrue(game.isGameFinished());
            assertTrue(page.checkFinale(), "la partie finie pendant qu'on joue : la fin se lance");
            assertEquals(UniversePage.DIMENSIONS, page.selected());
            DimensionsView map = page.dimensionsView();
            for (int i = 0; i < 40; i++) page.frame(0.05, true);
            assertTrue(map.greenLinks() > 0 && map.greenLinks() < 20, "les traits verdissent en vague : " + map.greenLinks());
            double small = map.centerRadius();
            for (int i = 0; i < 60; i++) page.frame(0.05, true);
            assertTrue(map.centerRadius() > small * 3, "le point du milieu grandit");
            // Version 0.267 : la vue toute jaune tient un instant (1 s) avant l'écran de fin.
            for (int i = 0; i < 31; i++) page.frame(0.05, true);
            assertTrue(page.isFinalePlaying(), "à 6,6 s, la vue jaune tient encore");
            assertFalse(finale.isOpen());
            for (int i = 0; i < 60 && !finale.isOpen(); i++) page.frame(0.05, true);
            assertTrue(finale.isOpen(), "puis l'écran de fin");
            assertEquals(7.5, DimensionsView.FINALE_SECONDS, 1e-9, "4 s de vague, 2,5 s de croissance, 1 s de jaune");
            assertEquals("Toutes les dimensions sont mêlées", finale.titleText());
            assertTrue(finale.summaryText().contains("Temps de jeu : ") && finale.summaryText().contains("10 univers formés"), finale.summaryText());
            assertTrue(finale.thanksText().startsWith("Merci"));
            // Deux choix : recommencer de zéro (deux clics) ou continuer la sauvegarde.
            int[] restarts = {0};
            finale.setOnRestart(() -> restarts[0]++);
            assertEquals("Recommencer de zéro", finale.restartButton().getText());
            assertEquals("Continuer la sauvegarde", finale.continueButton().getText());
            finale.restartButton().fire();
            assertEquals(0, restarts[0], "un clic ne suffit pas");
            assertTrue(finale.isOpen());
            assertEquals("Cliquer encore pour tout effacer", finale.restartButton().getText());
            finale.continueButton().fire();
            assertFalse(finale.isOpen(), "continuer ferme l'écran");
            assertEquals(0, restarts[0], "et ne recommence rien");
            finale.open();
            assertEquals("Recommencer de zéro", finale.restartButton().getText(), "rouvert, il faut de nouveau deux clics");
            finale.restartButton().fire();
            finale.restartButton().fire();
            assertEquals(1, restarts[0], "deux clics : on recommence de zéro");
            assertFalse(finale.isOpen());
            page.refresh();
            assertTrue(page.replayFinaleButton().isVisible());
            assertEquals(20, map.greenLinks(), "la partie finie, les traits restent verts");
            int[] replays = {0};
            page.setOnReplay(() -> replays[0]++);
            page.replayFinaleButton().fire();
            assertTrue(page.isFinalePlaying(), "« Revoir la fin » la rejoue");
            assertEquals(1, replays[0], "et rejoue son son (0.270)");
            return null;
        });
    }

    @Test
    void unePartieDejaFinieAuChargementNeRejouePasLaFin() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            openChallenge(game);
            reachAll(game);
            game.tick(0.05);
            UniversePage page = page(game);
            assertFalse(page.checkFinale());
            assertFalse(page.checkFinale());
            page.forgetFinale();
            assertFalse(page.checkFinale());
            assertFalse(page.isFinalePlaying());
            assertTrue(page.replayFinaleButton().isVisible(), "mais elle se revoit");
            return null;
        });
    }

    @Test
    void lesStatistiquesEtLaNoteDeVersion() {
        FxTestSupport.fx(() -> {
            Game game = tenth();
            openChallenge(game);
            reachAll(game);
            game.tick(0.05);
            StatsPage stats = new StatsPage(game, new PurchaseHistory(), new Pace());
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.contains("Paliers du défi final : 10 / 10 atteints"), rows.toString());
            assertTrue(rows.contains("Fin de la partie : atteinte : les dix paliers du défi final"), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Défi final, le mélange : 100 % (")), rows.toString());
            assertTrue(Version.CURRENT.compareTo("0.160") >= 0, "la version 0.160 ou après");
            return null;
        });
    }
}
