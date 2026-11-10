package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.SeedUpgrade;
import idle.ui.FxTestSupport;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Version 0.182 : le chrono hors de la sous-page Graine, une fois son étage acheté ; version 0.203 : une ligne rangée, plus une carte flottante. */
class ChronoCornerTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @Test
    void laCarteApparaitAvecLEtageHorsDeLaSousPageGraineEtRegleLeChrono() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.testFormUniverse();
            assertTrue(game.testAddUniverse());
            game.state().setPlaneShards(1e6);
            ChronoCorner corner = new ChronoCorner(game);
            new Scene(new StackPane(corner), 800, 600);
            assertFalse(corner.refresh(false), "sans l'étage du chrono, rien");
            assertFalse(corner.isVisible());
            assertTrue(game.buySeedUpgrade(SeedUpgrade.CHRONO));
            assertFalse(corner.refresh(true), "sur la sous-page Graine, le chrono y est déjà");
            assertTrue(corner.refresh(false));
            assertTrue(corner.isVisible() && corner.isManaged());
            assertTrue(corner.text().getText().startsWith("Gardé"), corner.text().getText());
            assertTrue(corner.text().getText().contains("×1, il se remplit"), corner.text().getText());
            assertEquals(Game.CHRONO_MAX_SPEED, corner.slider().getMax(), 1e-9);
            corner.slider().setValue(1.6);
            assertEquals(1.5, game.chronoSpeed(), 1e-9, "le réglage va par quarts");
            game.state().setChronoStored(60);
            corner.refresh(false);
            assertTrue(corner.text().getText().contains("×1.5, le jeu accélère"), corner.text().getText());
            // Le réglage fait ailleurs se recopie dans la carte.
            game.setChronoSpeed(2);
            corner.refresh(false);
            assertEquals(2, corner.slider().getValue(), 1e-9);
            return null;
        });
    }
}
