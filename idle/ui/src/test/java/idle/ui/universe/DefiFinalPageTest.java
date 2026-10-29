package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.GameUniverse.TreeBubble;
import idle.ui.FinalePane;
import idle.ui.FxTestSupport;
import idle.ui.Version;
import idle.ui.pages.StatsPage;
import java.util.List;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.160 : le défi final, sous Xvfb. Sans la bulle des racines, la carte des dimensions ne montre pas de palier ;
 * avec elle, chaque point se remplit de vert selon son avancée (en puissances de dix), tout vert au palier atteint. Les
 * dix atteints pendant qu'on joue, la fin se joue sur la carte (les traits verdissent, le point du milieu grandit), puis
 * l'écran de fin ; « Revoir la fin » la rejoue ; une partie déjà finie au chargement ne la rejoue pas. Les statistiques
 * et la note de version.
 */
class DefiFinalPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

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
            for (int i = 0; i < 60 && !finale.isOpen(); i++) page.frame(0.05, true);
            assertTrue(finale.isOpen(), "puis l'écran de fin");
            assertEquals("Toutes les dimensions sont mêlées", finale.titleText());
            assertTrue(finale.summaryText().contains("Temps de jeu : ") && finale.summaryText().contains("10 univers formés"), finale.summaryText());
            assertTrue(finale.thanksText().startsWith("Merci"));
            finale.continueButton().fire();
            assertFalse(finale.isOpen());
            page.refresh();
            assertTrue(page.replayFinaleButton().isVisible());
            assertEquals(20, map.greenLinks(), "la partie finie, les traits restent verts");
            page.replayFinaleButton().fire();
            assertTrue(page.isFinalePlaying(), "« Revoir la fin » la rejoue");
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
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            stats.openSubPage("Univers");
            List<String> rows = stats.shownRows();
            assertTrue(rows.contains("Paliers du défi final : 10 / 10 atteints"), rows.toString());
            assertTrue(rows.contains("Fin de la partie : atteinte : les dix paliers du défi final"), rows.toString());
            assertTrue(rows.stream().anyMatch(r -> r.startsWith("Défi final, le mélange : 100 % (")), rows.toString());
            assertEquals("0.160", Version.CURRENT);
            return null;
        });
    }
}
