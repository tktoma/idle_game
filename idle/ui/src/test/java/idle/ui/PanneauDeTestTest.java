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
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Le panneau du profil de test ({@link TestPanel}) et la bande des dimensions ({@link DimensionTestBar}), à l'écran :
 * le tiroir s'ouvre et se ferme, la section de l'onglet affiché passe en tête, la recherche trouve dans toutes les
 * sections, la vitesse se choisit, et la bande se refait pour la dimension ouverte.
 */
class PanneauDeTestTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    private static Button button(Node node, String text) {
        if (node instanceof Button b && b.getText() != null && b.getText().startsWith(text)) return b;
        if (node instanceof javafx.scene.control.ScrollPane scroll && scroll.getContent() != null) {
            Button found = button(scroll.getContent(), text);
            if (found != null) return found;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = button(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static TextField field(Node node) {
        if (node instanceof TextField f) return f;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TextField found = field(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    void leTiroirSuitLOngletCherchePartoutEtReglesLaVitesse() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            TestPanel panel = new TestPanel(game, new TestTools.Hooks() { });
            new Scene(panel, 420, 800);
            assertTrue(panel.isOpen());
            panel.toggle();
            assertFalse(panel.isOpen(), "la poignée reste, le tiroir se ferme");
            panel.toggle();
            panel.followTab("COLLIDER");
            assertNotNull(button(panel, "▾ Collisionneur"), "la section de l'onglet affiché, dépliée, en tête");
            assertNotNull(button(panel, "Énergie ×1 000"));
            field(panel).setText("energie du vide");
            assertNotNull(button(panel, "+1 000 énergie du vide"), "la recherche trouve dans une autre section");
            assertEquals(null, button(panel, "Énergie ×1 000"), "ce qui ne répond pas disparaît");
            field(panel).setText("");
            assertEquals(1, panel.timeFactor(), 1e-9, "vitesse normale au départ");
            button(panel, "×100").fire();
            assertEquals(100, panel.timeFactor(), 1e-9);
            button(panel, "Figé").fire();
            assertEquals(0, panel.timeFactor(), 1e-9);
            int before = game.state().explosions();
            panel.followTab("DARK_MATTER");
            button(panel, "Exploser maintenant").fire();
            assertEquals(before + 1, game.state().explosions(), "un outil agit sur la partie");
            return null;
        });
    }

    @Test
    void laBandeSeRefaitPourLaDimensionOuverte() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            TestPanel panel = new TestPanel(game, new TestTools.Hooks() { });
            int[] open = {0};
            DimensionTestBar bar = new DimensionTestBar(game, panel.actions(), panel.bots(), () -> open[0], panel::note, panel::refresh);
            new Scene(bar, 1200, 120);
            bar.refresh();
            assertTrue(bar.getChildren().isEmpty(), "aucune dimension ouverte : rien");
            panel.actions().openDimension(10);
            open[0] = 2;
            bar.refresh();
            assertEquals(2, bar.shown());
            button(bar, "+10 % du seuil").fire();
            assertEquals(Game.PLANE_THRESHOLD / 10, game.state().planeShardsEarned(), 1e-9);
            assertNotNull(button(bar, "Bot en direct"), "le plan a son bot");
            open[0] = 10;
            bar.refresh();
            assertNotNull(button(bar, "Étape suivante"), "le mélange a ses étapes");
            assertEquals(null, button(bar, "Bot en direct"), "pas de bot pour le mélange");
            return null;
        });
    }
}
