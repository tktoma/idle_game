package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.203 : la barre du profil de test. Ses outils défilent dans une zone bornée (un groupe à beaucoup de boutons ne
 * pousse plus la fenêtre, et les autres groupes restent à portée), et elle a un groupe « Animations » qui rejoue les
 * animations plein écran dans le skin choisi, sans rien changer à la partie.
 */
class Retours203Test {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static Button button(Node node, String text) {
        if (node instanceof Button b && b.getText().equals(text)) return b;
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) {
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

    @Test
    void lesOutilsDUnGroupeNePoussentPlusLaFenetre() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            DebugBar bar = new DebugBar(game);
            new Scene(bar, 900, 700);
            bar.applyCss();
            bar.layout();
            double closed = bar.prefHeight(900);
            // Les trois groupes les plus fournis : chacun tient dans la zone bornée.
            for (String group : new String[] {"Dimensions", "Skins", "Univers"}) {
                button(bar, group).fire();
                assertEquals(group, bar.openGroup());
                bar.applyCss();
                bar.layout();
                double open = bar.prefHeight(900);
                assertTrue(open <= closed + DebugBar.TOOLS_MAX_HEIGHT + 12, group + " : la barre ne grandit que de la zone des outils (" + (open - closed) + ")");
                assertTrue(open > closed, group + " : ses outils se montrent");
            }
            return null;
        });
    }

    @Test
    void leGroupeAnimationsRejoueSansChangerLaPartie() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            DebugBar bar = new DebugBar(game);
            new Scene(bar, 1500, 400);
            int[] played = new int[5];
            boolean[] possible = {false};
            bar.setAnimations(new DebugBar.AnimationTools() {
                @Override
                public void explosion() {
                    played[0]++;
                }

                @Override
                public boolean bigBang() {
                    played[1]++;
                    return possible[0];
                }

                @Override
                public boolean birth() {
                    played[2]++;
                    return possible[0];
                }

                @Override
                public boolean launch() {
                    played[3]++;
                    return possible[0];
                }

                @Override
                public boolean curtain() {
                    played[4]++;
                    return possible[0];
                }
            });
            button(bar, "Animations").fire();
            assertEquals("Animations", bar.openGroup());
            int explosions = game.state().explosions();
            for (String tool : new String[] {"Explosion (skin choisi)", "Big Bang (skin choisi)", "Naissance d'un univers", "Lancement d'un univers",
                    "Transition (skin choisi)"}) {
                assertNotNull(button(bar, tool), tool);
                button(bar, tool).fire();
            }
            for (int k = 0; k < played.length; k++) assertEquals(1, played[k], "outil " + k);
            assertEquals(explosions, game.state().explosions(), "rejouer l'explosion n'explose pas");
            assertEquals(0, game.bigBangs());
            assertFalse(game.isUniverseFormed());
            assertNotNull(button(bar, "Jouer la fin"));
            // Les skins retirés n'ont plus de bouton nulle part ; les curseurs se montrent sur tous les onglets.
            for (GameAppBase.Tab tab : GameAppBase.Tab.values()) {
                assertEquals(Skin.CURSOR_WATERING_CAN, CursorSkinArt.shownOn(Skin.CURSOR_WATERING_CAN, tab));
                assertEquals(Skin.CURSOR_MAGNIFIER, CursorSkinArt.shownOn(Skin.CURSOR_MAGNIFIER, tab));
            }
            return null;
        });
    }
}
