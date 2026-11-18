package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Le rond d'aide au clic (version 0.261), sous Xvfb : les ronds vivent puis s'effacent, et un clic sur rien se reconnaît. */
class RondAuClicTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void unRondParClicQuiSEfface() {
        FxTestSupport.fx(() -> {
            ClickRings rings = new ClickRings();
            rings.fit(200, 100);
            assertTrue(rings.isMouseTransparent(), "il ne prend pas les clics");
            rings.add(10, 10);
            rings.add(50, 50);
            assertEquals(2, rings.rings());
            rings.frame(ClickRings.LIFE / 2);
            assertEquals(2, rings.rings(), "encore visibles à mi-vie");
            rings.frame(ClickRings.LIFE);
            assertEquals(0, rings.rings(), "effacés");
            for (int i = 0; i < 100; i++) rings.add(i, i);
            assertTrue(rings.rings() <= 24, "un nombre borné");
            return null;
        });
    }

    /** Version 0.262 : tout clic fait le son du clic, sauf un onglet ou le mini-jeu d'une dimension, qui ont le leur. */
    @Test
    void seulsLesOngletsEtLesMiniJeuxOntLeurPropreSon() {
        FxTestSupport.fx(() -> {
            String tabKey = "ongletDuJeu";
            Pane empty = new Pane();
            Label text = new Label("un texte");
            VBox page = new VBox(text, empty);
            new ScrollPane(page);
            assertFalse(ClickRings.hasOwnSound(empty, tabKey), "un fond vide : le clic");
            assertFalse(ClickRings.hasOwnSound(text, tabKey), "un texte : le clic");
            assertFalse(ClickRings.hasOwnSound(null, tabKey), "rien du tout : le clic");
            Button button = new Button("Acheter");
            Label inside = new Label("dans le bouton");
            button.setGraphic(inside);
            assertFalse(ClickRings.hasOwnSound(button, tabKey), "un bouton qui déclenche une action : le clic aussi");
            assertFalse(ClickRings.hasOwnSound(inside, tabKey));
            assertFalse(ClickRings.hasOwnSound(new TextField(), tabKey));
            assertFalse(ClickRings.hasOwnSound(new Slider(), tabKey));
            Pane card = new Pane();
            card.setOnMousePressed(event -> { });
            assertFalse(ClickRings.hasOwnSound(card, tabKey), "une carte : le clic");
            Button tab = new Button("Particules");
            tab.getProperties().put(tabKey, true);
            Label tabText = new Label("Particules");
            tab.setGraphic(tabText);
            assertTrue(ClickRings.hasOwnSound(tab, tabKey), "un onglet a son propre son");
            assertTrue(ClickRings.hasOwnSound(tabText, tabKey));
            return null;
        });
    }
}
