package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.common.Card;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.190 : les skins du chrome sous Xvfb. Chaque skin de carte se montre dans chaque état sans exception, chaque
 * skin d'onglet donne un style dans les deux dispositions, chaque fond se dessine à deux tailles ; et sans skin, le style
 * d'un onglet et celui d'une carte sont, au caractère près, ceux d'avant la version 0.190.
 */
class ChromeSkinsTest {

    private static final String ACCENT = "#9fd0ff";

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @AfterEach
    void classic() {
        Card.setSkin(null);
    }

    /** Les skins d'un visuel, dans l'ordre du catalogue. */
    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    @Test
    void chaqueSkinDeCarteSeMontreDansChaqueEtat() {
        FxTestSupport.fx(() -> {
            assertEquals(4, skinsOf(SkinVisual.CARD).size());
            for (Skin skin : skinsOf(SkinVisual.CARD)) {
                Card.setSkin(skin);
                assertEquals(skin, Card.skin());
                for (Card.State state : Card.State.values()) {
                    Card card = new Card(ACCENT);
                    card.setOnAction(() -> { });
                    new Scene(card, 200, 120);
                    card.show(state, "12", "V", "Vitesse", "+10 % de vitesse", "L'explication", "456 particules", "42 s");
                    card.applyCss();
                    card.layout();
                    assertEquals(state, card.state());
                    assertFalse(card.getStyle().isEmpty());
                    assertTrue(card.text().contains("Vitesse"));
                    // La carte à jouer répète son coin ; les autres non.
                    assertEquals(skin == Skin.CARD_PLAYING, card.text().endsWith("| 12") && card.text().startsWith("12"));
                }
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDOngletDonneUnStyleDansLesDeuxDispositions() {
        assertEquals(4, skinsOf(SkinVisual.TABS).size());
        for (Skin skin : skinsOf(SkinVisual.TABS)) {
            for (GameAppBase.TabRole role : GameAppBase.TabRole.values()) {
                for (boolean selected : new boolean[] {true, false}) {
                    String top = GameAppTabs.tabStyle(skin, ACCENT, selected, role, false, false);
                    String narrow = GameAppTabs.tabStyle(skin, ACCENT, selected, role, false, true);
                    String left = GameAppTabs.tabStyle(skin, ACCENT, selected, role, true, false);
                    for (String style : new String[] {top, narrow, left}) {
                        assertTrue(style.startsWith("-fx-cursor: hand;"), style);
                        assertTrue(style.contains("-fx-background-color:"), style);
                        assertTrue(style.contains("-fx-border-color:"), style);
                    }
                    // En colonne, l'onglet a sa largeur fixe ; en haut, resserré, sa police rétrécit (sauf les pastilles, toujours rondes).
                    assertEquals(skin != Skin.TABS_DOTS, left.contains("-fx-min-width: 176;"), left);
                    assertTrue(narrow.contains("-fx-font-size: 12px;"), narrow);
                    assertTrue(top.contains("-fx-font-size: 14px;"), top);
                }
            }
        }
    }

    @Test
    void chaqueFondSeDessineAuxDeuxTailles() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.AMBIENT).size());
            Chrome chrome = new Chrome();
            assertNull(chrome.skin());
            assertFalse(chrome.isVisible());
            for (Skin skin : skinsOf(SkinVisual.AMBIENT)) {
                chrome.setSkin(skin);
                assertTrue(chrome.isVisible());
                chrome.fit(800, 600);
                assertEquals(800, chrome.getWidth());
                chrome.frame(0.3);
                chrome.frame(0.3);     // le second passe la demi-seconde : la Nuit étoilée et le Terminal se redessinent
                chrome.fit(1600, 900);
                assertEquals(900, chrome.getHeight());
                chrome.frame(1);
            }
            chrome.setSkin(null);
            assertFalse(chrome.isVisible());
            assertEquals(Chrome.CLASSIC_ROOT_STYLE, Chrome.rootStyle(null));
            assertEquals(0, Chrome.rootPadding(null).getLeft());
            assertEquals(Chrome.MARGIN, Chrome.rootPadding(Skin.AMBIENT_PAPER).getLeft());
            return null;
        });
    }

    @Test
    void sansSkinLeStyleDesOngletsEstCeluiDeToujours() {
        assertEquals("-fx-cursor: hand; -fx-border-width: 1.5; -fx-font-size: 14px; -fx-padding: 9 10; -fx-background-radius: 8 8 0 0;"
                + " -fx-border-radius: 8 8 0 0; -fx-border-style: solid; -fx-border-color: rgba(159, 208, 255, 1.0); -fx-text-fill: #9fd0ff;"
                + " -fx-background-color: rgba(159, 208, 255, 0.14); -fx-effect: dropshadow(gaussian, rgba(159, 208, 255, 0.55), 12, 0.2, 0, 0);",
                GameAppTabs.tabStyle(null, ACCENT, true, GameAppBase.TabRole.ACT, false, false));
        assertEquals("-fx-cursor: hand; -fx-border-width: 1.5; -fx-font-size: 12px; -fx-padding: 8 10; -fx-background-radius: 8 8 0 0;"
                + " -fx-border-radius: 8 8 0 0; -fx-border-style: segments(5, 4); -fx-border-color: rgba(159, 208, 255, 0.5);"
                + " -fx-text-fill: #a9bacb; -fx-background-color: #111824; -fx-effect: null;",
                GameAppTabs.tabStyle(null, ACCENT, false, GameAppBase.TabRole.SIDE, false, true));
        assertEquals("-fx-cursor: hand; -fx-border-width: 1.5; -fx-font-size: 14px; -fx-padding: 8 12; -fx-alignment: center-left;"
                + " -fx-min-width: 176; -fx-max-width: 176; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-style: solid;"
                + " -fx-border-color: transparent; -fx-text-fill: #7d8fa3; -fx-background-color: transparent; -fx-effect: null;",
                GameAppTabs.tabStyle(null, ACCENT, false, GameAppBase.TabRole.META, true, false));
    }

    @Test
    void sansSkinLeStyleDesCartesEstCeluiDeToujours() {
        FxTestSupport.fx(() -> {
            Card card = new Card(ACCENT);
            assertEquals("-fx-background-color: #121923; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #9fd0ff66;",
                    card.getStyle());
            card.setOnAction(() -> { });
            card.show(Card.State.READY, "12", "V", "Vitesse", "+10 % de vitesse", "", "456 particules", "");
            assertEquals("-fx-background-color: #9fd0ff33; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #9fd0ff; -fx-cursor: hand;",
                    card.getStyle());
            Label title = (Label) card.getChildren().get(1);
            assertEquals("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;", title.getStyle());
            card.show(Card.State.LOCKED, "", "", "Vitesse", "", "", "", "");
            assertEquals("-fx-background-color: #0e131b; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #9fd0ff33; -fx-border-style: dashed;",
                    card.getStyle());
            assertEquals("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9fd0ff99;", title.getStyle());
            assertEquals(0, card.getPadding().getLeft() - 12);
            return null;
        });
    }
}
