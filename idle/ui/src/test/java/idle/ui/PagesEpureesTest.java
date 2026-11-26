package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.automation.AutomationPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.common.FilterBar;
import idle.ui.common.Labels;
import idle.ui.common.ResourceHead;
import java.util.Map;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;

/**
 * Version 0.315 : les pages épurées. L'automatisation met son titre dans le bandeau, la Matière noire et le Big Bang
 * passent leurs explications au survol, l'Expansion montre l'espace dans le bandeau et se passe du chrono, la
 * recherche des molécules a ses filtres à sa droite.
 */
class PagesEpureesTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void unTitreSeulNAniNombreNiLigneDessous() {
        FxTestSupport.fx(() -> {
            ResourceHead title = ResourceHead.title("Automatismes", "#9be7a8");
            assertEquals("Automatismes", title.titleText(), "sans les deux points");
            assertFalse(title.value().isVisible());
            assertFalse(title.sub().isManaged());
            ResourceHead atoms = new ResourceHead("Atomes", GameApp.ATOMS_COLOR);
            assertEquals("Atomes :", atoms.titleText());
            assertTrue(atoms.value().isVisible());
            return null;
        });
    }

    @Test
    void lAutomatisationMetSonTitreDansLeBandeauEtSaNoteAuSurvol() {
        FxTestSupport.fx(() -> {
            Game game = new Game(1L);
            game.start();
            AutomationPage page = new AutomationPage(game);
            new Scene(page, 1200, 760);
            page.refresh();
            assertEquals("Automatismes", page.head().titleText());
            assertEquals("", page.pauseNoteText(), "rien de coupé : pas de bulle");
            game.setAutomationPaused(true);
            page.refresh();
            // Sans automatisme à couper, la note reste vide ; elle ne s'écrit plus jamais sous le bouton.
            assertTrue(page.pauseNoteText().isEmpty() || page.pauseNoteText().startsWith("Tous les automatismes sont coupés"),
                    page.pauseNoteText());
            return null;
        });
    }

    @Test
    void lExpansionMontreLEspaceDansLeBandeauSansChrono() {
        FxTestSupport.fx(() -> {
            Game game = new Game(1L);
            game.start();
            BigBangPage page = new BigBangPage(game);
            new Scene(page, 1200, 760);
            page.refresh();
            assertEquals("Big Bangs :", page.head().titleText());
            assertFalse(page.hidesChrono());
            page.select(BigBangPage.EXPANSION);
            page.refresh();
            assertEquals("Espace :", page.head().titleText());
            assertTrue(page.hidesChrono(), "l'Expansion se passe du chrono");
            assertFalse(page.head().value().getText().isEmpty(), "l'espace est compté");
            assertTrue(page.head().value().getTooltip().getText().contains("L'espace grandit"), "l'explication au survol");
            page.select(0);
            assertEquals("Big Bangs :", page.head().titleText(), "retour au nombre de Big Bangs");
            assertTrue(page.bestButton().getParent() instanceof FlowPane, "le bouton d'achat sur la ligne du cycle");
            return null;
        });
    }

    @Test
    void leRondGardeSonNombreEtSaToucheEnDedans() {
        FxTestSupport.fx(() -> {
            idle.ui.common.Card card = new idle.ui.common.Card(GameApp.ATOMS_COLOR);
            card.setRound(150);
            card.show(idle.ui.common.Card.State.READY, "10", "F", "Fusion des générateurs", "+2.56e25 atomes", "", "", "");
            new Scene(new StackPane(card), 400, 400);
            card.applyCss();
            card.layout();
            // Version 0.316 : le nombre et la touche se resserrent au milieu ; aucun ne touche le bord du rond.
            javafx.scene.Node head = card.getChildren().get(0);
            assertTrue(head.getLayoutBounds().getWidth() < 110, "la ligne du haut tient dans le rond : " + head.getLayoutBounds());
            assertTrue(head.getLayoutY() > 15, "la ligne du haut descend dans le rond : " + head.getLayoutY());
            assertTrue(card.text().contains("10") && card.text().contains("F"));
            return null;
        });
    }

    @Test
    void lesFiltresPassentADroiteDuChamp() {
        FxTestSupport.fx(() -> {
            StackPane home = new StackPane();
            FilterBar bar = new FilterBar("Rechercher", "#ffd877", home, () -> {});
            bar.group("Tous", Map.of(1, "un"));
            bar.group("Toutes", Map.of(2, "deux"));
            assertEquals(3, bar.getChildren().size(), "le champ et deux rangées, l'un sous l'autre");
            bar.besideField();
            assertEquals(1, bar.getChildren().size(), "une seule ligne : le champ, et les rangées à sa droite");
            return null;
        });
    }

    @Test
    void uneBulleVideSeRetire() {
        FxTestSupport.fx(() -> {
            Button button = new Button("Couper");
            Labels.tip(button, "Tous les automatismes sont coupés.");
            assertEquals("Tous les automatismes sont coupés.", button.getTooltip().getText());
            javafx.scene.control.Tooltip same = button.getTooltip();
            Labels.tip(button, "Autre texte");
            assertSame(same, button.getTooltip(), "la même bulle, un autre texte");
            Labels.tip(button, "");
            assertNull(button.getTooltip());
            return null;
        });
    }
}
