package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.ui.pages.SettingsPage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.146 : le sous-onglet « Notifications et signes » des réglages, sous Xvfb. Les notifications, l'objectif et les
 * signes d'état y sont réunis ; chaque catégorie a son bouton, grisé quand son grand interrupteur est coupé.
 */
class NotificationsEtSignesPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @AfterEach
    void showAllDots() {
        Readiness.setHidden(java.util.Set.of());
    }

    /** Ce que la page demande à la fenêtre : rien à sauvegarder ici. */
    static SettingsPage.Saves saves() {
        return new SettingsPage.Saves() {
            @Override public String status() { return ""; }
            @Override public String saveNow() { return ""; }
            @Override public String copyExport() { return ""; }
            @Override public String pasteImport() { return ""; }
            @Override public String previewImport() { return ""; }
            @Override public int checkpointSlots() { return 0; }
            @Override public String checkpointLine(int slot) { return "vide"; }
            @Override public boolean checkpointUsed(int slot) { return false; }
            @Override public String saveCheckpoint(int slot, String name) { return ""; }
            @Override public String loadCheckpoint(int slot) { return ""; }
            @Override public String deleteCheckpoint(int slot) { return ""; }
            @Override public String copyBugReport() { return ""; }
            @Override public String records() { return ""; }
            @Override public void clearRecords() { }
        };
    }

    static Button button(Node node, String text) {
        if (node instanceof Button b && text.equals(b.getText())) return b;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = button(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    void lesTroisReglagesSontReunisEtSeReglentParCategorie() {
        FxTestSupport.fx(() -> {
            Settings settings = Settings.load(new NotificationsEtSignesTest.MemoryPreferences());
            SettingsPage page = new SettingsPage(settings, () -> { }, () -> { }, saves());
            new Scene(page, 1100, 900);
            page.select(SettingsPage.Section.NOTICES);
            page.refresh();
            page.applyCss();
            page.layout();
            assertNotNull(button(page, "Notifications : affichées"));
            assertNotNull(button(page, "Objectif : affiché"));
            assertNotNull(button(page, "Signes d'état : affichés"));
            for (Notice kind : Notice.values()) assertNotNull(button(page, "✓ " + kind.label()), kind.label());
            for (Goals.Part part : Goals.Part.values()) assertNotNull(button(page, "✓ " + part.label()), part.label());
            for (Readiness.Kind kind : Readiness.Kind.values()) assertNotNull(button(page, "✓ " + kind.label()), kind.label());
            // Une catégorie coupée d'un clic.
            Button comets = button(page, "✓ Comètes");
            comets.fire();
            assertFalse(settings.noticeChosen(Notice.COMETS));
            assertEquals("✗ Comètes", comets.getText());
            assertNotNull(comets.getTooltip(), "son bouton dit ce qu'elle annonce");
            // Le chrono se règle avec l'objectif : c'est le réglage d'avant.
            button(page, "✓ Le chrono").fire();
            assertFalse(settings.chrono());
            assertNull(button(page, "Chrono : affiché"), "plus de bouton du chrono à part");
            // Une pastille cachée.
            button(page, "✓ Comète").fire();
            assertEquals("", Readiness.dot(Readiness.Kind.COMET, true));
            // Le grand interrupteur grise les catégories, sans changer leur choix.
            button(page, "Notifications : affichées").fire();
            assertTrue(button(page, "✓ Succès").isDisabled());
            assertTrue(settings.noticeChosen(Notice.ACHIEVEMENTS));
            button(page, "Notifications : coupées").fire();
            button(page, "Toutes coupées").fire();
            for (Notice kind : Notice.values()) assertFalse(settings.noticeChosen(kind));
            button(page, "Toutes affichées").fire();
            for (Notice kind : Notice.values()) assertTrue(settings.noticeChosen(kind));
            return null;
        });
    }
}
