package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.140 : les outils d'essai des dimensions, refaits en une rangée par dimension (plus la graine et son chrono),
 * et le groupe « Bots dimension » à côté. Les bots ne sont pas dans les tests de l'interface : le groupe le dit.
 */
class DebugBarDimensionsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static DebugBar bar(Game game) {
        DebugBar bar = new DebugBar(game);
        new Scene(bar, 1500, 300);
        bar.applyCss();
        bar.layout();
        return bar;
    }

    /** Le bouton de cette rangée (« Plan (facettes) ») qui porte ce texte. */
    private static Button tool(Node node, String row, String text) {
        if (node instanceof HBox box && !box.getChildren().isEmpty() && box.getChildren().get(0) instanceof Label label
                && label.getText().equals(row)) {
            for (Node child : box.getChildren()) if (child instanceof Button b && b.getText().equals(text)) return b;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = tool(child, row, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Button button(Node node, String text) {
        if (node instanceof Button b && b.getText().equals(text)) return b;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = button(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    void uneRangeeParDimensionOuvreRemplitEtRegle() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            DebugBar bar = bar(game);
            button(bar, "Dimensions").fire();
            assertEquals("Dimensions", bar.openGroup());
            tool(bar, "Durée (instants)", "Ouvrir").fire();
            assertEquals(4, game.dimensionsUnlocked(), "ouvrir la durée forme l'univers et en ajoute trois");
            tool(bar, "Plan (facettes)", "+10 % du seuil").fire();
            assertEquals(Game.PLANE_THRESHOLD / 10, game.state().planeShards(), 1e-9);
            assertEquals(Game.PLANE_THRESHOLD / 10, game.state().planeShardsEarned(), 1e-9);
            tool(bar, "Volume (sphères)", "Seuil atteint").fire();
            assertTrue(game.isVolumeThresholdReached());
            assertEquals(0, game.state().volumeShards(), 1e-9, "le seuil n'ajoute rien à dépenser");
            tool(bar, "Ligne (segments)", "Améliorations au maximum").fire();
            for (Game.LineUpgrade u : Game.LineUpgrade.values()) assertEquals(game.lineMaxLevel(u), game.lineLevel(u));
            tool(bar, "Ligne (segments)", "Améliorations à zéro").fire();
            for (Game.LineUpgrade u : Game.LineUpgrade.values()) assertEquals(0, game.lineLevel(u));
            tool(bar, "Durée (instants)", "Améliorations au maximum").fire();
            assertEquals(Game.DurationUpgrade.SPEED.max(), game.durationLevel(Game.DurationUpgrade.SPEED));
            // La graine et son chrono.
            tool(bar, "Graine et chrono", "Chrono plein").fire();
            assertTrue(game.isChronoUnlocked(), "le chrono plein demande son étage");
            assertEquals(game.chronoCapacity(), game.chronoStored(), 1e-9);
            tool(bar, "Graine et chrono", "Améliorations de la graine au maximum").fire();
            for (Game.SeedUpgrade u : Game.SeedUpgrade.values()) assertEquals(u.max(), game.seedUpgradeLevel(u));
            assertTrue(game.isWateringCanUnlocked());
            game.setChronoSpeed(2);
            tool(bar, "Graine et chrono", "Améliorations de la graine à zéro").fire();
            for (Game.SeedUpgrade u : Game.SeedUpgrade.values()) assertEquals(0, game.seedUpgradeLevel(u));
            assertEquals(0, game.chronoStored(), 1e-9);
            assertEquals(1, game.chronoSpeed(), 1e-9);
            return null;
        });
    }

    @Test
    void lesBotsDesDimensionsSontACoteEtDisentSIlsManquent() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            DebugBar bar = bar(game);
            Button dimensions = button(bar, "Dimensions"), bots = button(bar, "Bots dimension");
            assertNotNull(bots);
            assertEquals(dimensions.getParent(), bots.getParent());
            int at = dimensions.getParent().getChildrenUnmodifiable().indexOf(dimensions);
            assertEquals(at + 1, bots.getParent().getChildrenUnmodifiable().indexOf(bots), "juste à côté de Dimensions");
            bots.fire();
            assertEquals(java.util.List.of("Ligne", "Plan", "Volume", "Durée"), bar.dimensionBotChoice().getItems());
            assertFalse(DimensionBotBridge.available(), "les bots ne sont pas dans les tests de l'interface");
            bar.dimensionLiveButton().fire();
            assertFalse(bar.dimensionBotLive());
            assertTrue(bar.botNote().contains("Les bots ne sont pas là"), bar.botNote());
            button(bar, "+1 min avec le bot").fire();
            assertFalse(bar.isFastForwarding());
            return null;
        });
    }
}
