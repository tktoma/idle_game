package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.199 : les skins de la fenêtre autour du jeu, sous Xvfb. Chaque skin des notifications, des panneaux, de
 * l'écran de fin, de la transition, du bouton de démarrage et du curseur s'applique et se dessine sans exception, à deux
 * tailles ; quelque chose change par rapport au classique (style, texte, inclinaison, image) ; et le classique rend aux
 * étiquettes leur style d'origine.
 */
class FenetreSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static Game finished() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (int k = 0; k < 9; k++) assertTrue(game.testAddUniverse());
        game.state().setGameFinished(true);
        return game;
    }

    @Test
    void chaqueSkinDeNotificationChangeLeMessage() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.TOAST).size());
            ToastPane classic = new ToastPane();
            classic.show("Nouveau succès : Première étincelle", "#9fd0ff");
            Label reference = (Label) classic.getChildren().get(0);
            assertEquals(ToastPane.STYLE + "#9fd0ff;", reference.getStyle(), "le classique au caractère près");
            for (double width : new double[] {400, 1200}) {
                for (Skin skin : skinsOf(SkinVisual.TOAST)) {
                    ToastPane toasts = new ToastPane();
                    new Scene(new StackPane(toasts), width, 300);
                    toasts.setSkin(skin);
                    assertEquals(skin, toasts.skin());
                    toasts.show("Nouveau succès : Première étincelle", "#9fd0ff");
                    toasts.show("Big Bang n° 2 : tout repart du premier générateur.", "#c8a2ff");
                    toasts.applyCss();
                    toasts.layout();
                    toasts.frame(0.5);
                    Label toast = (Label) toasts.getChildren().get(0);
                    assertNotEquals(reference.getStyle(), toast.getStyle(), skin.label());
                    assertTrue(toast.getStyle().contains("#9fd0ff"), skin.label() + " garde le liseré de couleur");
                    if (skin == Skin.TOAST_TELEGRAM) assertTrue(toast.getText().endsWith(" STOP"), "le télégramme finit par STOP");
                    else assertEquals("Nouveau succès : Première étincelle", toast.getText(), skin.label());
                    assertEquals(skin == Skin.TOAST_POSTIT ? -1.5 : 0, toast.getRotate(), skin.label());
                    assertEquals(2, toasts.shown(), "les messages se comptent");
                    toasts.frame(6);
                    assertEquals(0, toasts.count());
                    assertEquals(2, toasts.shown(), "effacés, ils restent comptés");
                }
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDePanneauHabilleLAideLAbsenceEtLaFin() {
        FxTestSupport.fx(() -> {
            assertEquals(3, skinsOf(SkinVisual.PANEL).size());
            assertEquals(3, skinsOf(SkinVisual.ENDING).size());
            Game game = finished();
            java.util.prefs.Preferences store = new NotificationsEtSignesTest.MemoryPreferences();
            Settings settings = Settings.load(store);
            List<Skin> skins = new ArrayList<>(skinsOf(SkinVisual.PANEL));
            skins.addAll(skinsOf(SkinVisual.ENDING));
            for (double width : new double[] {640, 1400}) {
                HelpPane help = new HelpPane(game, settings);
                EndPane end = new EndPane(game);
                FinalePane finale = new FinalePane(game);
                AbsencePane absence = new AbsencePane(() -> { });
                StackPane root = new StackPane(help, end, finale, absence);
                new Scene(root, width, 800);
                help.open();
                finale.open();
                end.fill();
                end.reveal();
                absence.open(new idle.core.Absence(game, 120));
                String classicCard = help.card().getStyle(), classicVeil = help.getStyle();
                String classicTitle = finale.titleText();
                Label finaleTitle = (Label) finale.card().getChildren().get(0);
                String classicTitleStyle = finaleTitle.getStyle();
                assertNull(help.skin());
                for (Skin skin : skins) {
                    for (Overlay pane : List.of(help, end, finale, absence)) {
                        pane.setSkin(skin);
                        assertEquals(skin, pane.skin());
                        root.applyCss();
                        root.layout();
                        assertNotEquals(classicCard, pane.card().getStyle(), skin.label() + " change la carte");
                        assertNotEquals(classicVeil, pane.getStyle(), skin.label() + " change le voile");
                        assertTrue(pane.card().getStyle().contains("-fx-padding:"), skin.label() + " garde un rembourrage");
                    }
                    assertNotEquals(classicTitleStyle, finaleTitle.getStyle(), skin.label() + " change les étiquettes");
                    assertEquals(classicTitle, finale.titleText(), "le texte ne change pas");
                    assertEquals(skin == Skin.ENDING_PHOTO ? -2.5 : 0, finale.card().getRotate(), skin.label());
                    // Rouvrir sous le skin : l'aide se réécrit et reste habillée.
                    help.close();
                    help.open();
                    assertTrue(help.isVisible());
                    assertNotEquals(classicCard, help.card().getStyle(), skin.label() + " tient à la réouverture");
                }
                // Le classique rend tout : la carte, le voile, chaque étiquette, l'inclinaison.
                for (Overlay pane : List.of(help, end, finale, absence)) {
                    pane.setSkin(null);
                    assertEquals(classicCard.isEmpty(), pane.card().getStyle().isEmpty());
                    assertEquals(classicVeil, pane.getStyle(), "le voile classique");
                    assertEquals(0, pane.card().getRotate());
                }
                assertEquals(classicCard, help.card().getStyle(), "la carte de l'aide au caractère près");
                assertEquals(classicTitleStyle, finaleTitle.getStyle(), "le titre de la fin au caractère près");
                // Le style d'une étiquette sous un skin clair passe à l'encre, en gardant sa teinte.
                String ink = PanelSkinStyles.label(Skin.PANEL_BOOK, "-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
                assertTrue(ink.startsWith("-fx-font-size: 13px; -fx-text-fill: #"), ink);
                assertFalse(ink.contains("#e8f4ff"), ink);
                assertTrue(ink.contains("Serif"), ink);
                assertEquals("-fx-text-fill: #ffd27f;", PanelSkinStyles.label(Skin.TRANSITION_IRIS, "-fx-text-fill: #ffd27f;"), "le diaphragme garde les couleurs");
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDeTransitionHabilleLeGuideEtJoueSonRideau() {
        FxTestSupport.fx(() -> {
            assertEquals(4, skinsOf(SkinVisual.TRANSITION).size());
            Game game = finished();
            for (double width : new double[] {700, 1300}) {
                TransitionPane guide = new TransitionPane(game);
                TransitionSkinArt curtain = new TransitionSkinArt();
                curtain.fit(width, 700);
                StackPane root = new StackPane(guide, curtain);
                new Scene(root, width, 700);
                guide.setOnConfirmed(curtain::play);
                int[] confirmed = {0};
                guide.open(true, () -> confirmed[0]++);
                String classicCard = guide.card().getStyle();
                guide.close();
                for (Skin skin : skinsOf(SkinVisual.TRANSITION)) {
                    guide.setSkin(skin);
                    guide.open(skin.ordinal() % 2 == 0, () -> confirmed[0]++);
                    root.applyCss();
                    root.layout();
                    assertTrue(guide.isOpen());
                    assertNotEquals(classicCard, guide.card().getStyle(), skin.label());
                    assertFalse(curtain.isPlaying());
                    guide.confirm();
                    assertFalse(guide.isOpen());
                    assertTrue(curtain.isPlaying(), skin.label() + " joue son rideau");
                    assertTrue(curtain.isVisible());
                    for (int i = 0; i < 20; i++) curtain.frame(0.05);
                    assertTrue(curtain.isPlaying(), "à une seconde, le rideau joue encore");
                    curtain.fit(width + 10, 710);
                    for (int i = 0; i < 10; i++) curtain.frame(0.05);
                    assertFalse(curtain.isPlaying(), skin.label() + " s'arrête au bout de " + TransitionSkinArt.SECONDS + " s");
                    assertFalse(curtain.isVisible());
                }
                assertEquals(4, confirmed[0]);
                // Sans skin, pas de rideau ; le classique rend la carte.
                guide.setSkin(null);
                guide.open(false, () -> confirmed[0]++);
                assertEquals(classicCard, guide.card().getStyle());
                guide.confirm();
                assertFalse(curtain.isPlaying());
                curtain.play(null);
                assertFalse(curtain.isPlaying());
                assertEquals(5, confirmed[0]);
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDuBoutonDeDemarrageChangeSonStyleEtLuiDonneUneIcone() {
        FxTestSupport.fx(() -> {
            assertEquals(4, skinsOf(SkinVisual.START_BUTTON).size());
            String classic = "-fx-font-size: 16px; -fx-padding: 12 28; -fx-text-fill: #e8f4ff;";
            for (Skin skin : skinsOf(SkinVisual.START_BUTTON)) {
                Button button = new Button("Créer le premier générateur");
                button.setStyle(classic);
                StartButtonArt.dress(button, skin);
                new Scene(new StackPane(button), 400, 200);
                button.applyCss();
                button.layout();
                assertNotEquals(classic, button.getStyle(), skin.label());
                assertNotNull(button.getGraphic(), skin.label() + " a une icône");
                assertEquals(StartButtonArt.ICON, ((javafx.scene.canvas.Canvas) button.getGraphic()).getWidth());
                assertEquals("Créer le premier générateur", button.getText());
            }
            Button plain = new Button("Créer le premier générateur");
            plain.setStyle(classic);
            StartButtonArt.dress(plain, null);
            StartButtonArt.dress(plain, Skin.TOAST_POSTIT);
            assertEquals(classic, plain.getStyle(), "le classique et un skin d'ailleurs ne changent rien");
            assertNull(plain.getGraphic());
            return null;
        });
    }

    @Test
    void chaqueSkinDeCurseurDonneUneImageEtLEtoileFilanteUneTrainee() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.CURSOR).size());
            assertNull(CursorSkinArt.cursor(null));
            assertNull(CursorSkinArt.cursor(Skin.TOAST_POSTIT));
            for (Skin skin : skinsOf(SkinVisual.CURSOR)) {
                javafx.scene.ImageCursor cursor = CursorSkinArt.cursor(skin);
                assertNotNull(cursor, skin.label());
                assertEquals(cursor, CursorSkinArt.cursor(skin), "dessiné une fois");
                assertEquals(CursorSkinArt.SIZE, (int) cursor.getImage().getWidth());
                // Au moins un pixel dessiné, et la pointe dans l'image.
                boolean drawn = false;
                for (int x = 0; x < CursorSkinArt.SIZE && !drawn; x++) {
                    for (int y = 0; y < CursorSkinArt.SIZE; y++) {
                        if (cursor.getImage().getPixelReader().getColor(x, y).getOpacity() > 0.1) {
                            drawn = true;
                            break;
                        }
                    }
                }
                assertTrue(drawn, skin.label() + " dessine quelque chose");
                double[] hotspot = CursorSkinArt.hotspot(skin);
                assertTrue(hotspot[0] >= 0 && hotspot[0] < CursorSkinArt.SIZE && hotspot[1] >= 0 && hotspot[1] < CursorSkinArt.SIZE);
                // Version 0.203 : tous partout, l'arrosoir et la loupe compris (avant : l'univers et le Big Bang seulement).
                for (GameAppBase.Tab tab : GameAppBase.Tab.values()) assertEquals(skin, CursorSkinArt.shownOn(skin, tab), skin.label() + " sur " + tab);
            }
            assertNull(CursorSkinArt.shownOn(null, GameAppBase.Tab.UNIVERSE));
            CursorSkinArt.Trail trail = new CursorSkinArt.Trail();
            trail.fit(800, 600);
            trail.move(10, 10);
            assertEquals(0, trail.sparks(), "éteinte, la traînée ne garde rien");
            trail.setActive(true);
            for (int i = 0; i < 80; i++) trail.move(i * 5, i * 3);
            assertEquals(60, trail.sparks(), "soixante étincelles au plus");
            trail.frame(0.3);
            assertEquals(60, trail.sparks());
            trail.frame(0.4);
            assertEquals(0, trail.sparks(), "toutes éteintes au bout de " + CursorSkinArt.Trail.LIFE + " s");
            trail.move(5, 5);
            trail.setActive(false);
            assertEquals(0, trail.sparks());
            return null;
        });
    }
}
