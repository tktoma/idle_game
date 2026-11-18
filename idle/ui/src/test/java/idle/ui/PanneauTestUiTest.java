package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Le panneau du profil test UI (version 0.248), sous Xvfb : il ne montre rien avant le jeu prêt, puis suit la lecture. */
class PanneauTestUiTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void lePanneauMontreLaPremiereEtapeQuandLeJeuEstPretPuisSuitLaLecture() {
        FxTestSupport.fx(() -> {
            List<Long> shown = new ArrayList<>();
            List<Long> checked = new ArrayList<>();
            UiTestPanel panel = new UiTestPanel((run, step) -> shown.add(step), (run, progress) -> checked.add(progress.step()), List.of(), null);
            assertTrue(shown.isEmpty(), "rien avant que le jeu soit prêt");
            panel.begin();
            assertEquals(List.of(0L), shown);
            UiTestRun run = panel.testRun();
            run.setSecondsPerStep(1);
            run.setPlaying(true);
            panel.frame(0.9);
            assertEquals(List.of(0L), checked, "la vue vérifiée avant de passer");
            panel.frame(0.2);
            assertEquals(List.of(0L, 1L), shown, "puis l'étape suivante");
            panel.problem("le sous-onglet « X » n'est pas là");
            panel.error(new IllegalStateException("cassé"));
            assertEquals(2, run.errorCount());
            assertTrue(run.report().contains("IllegalStateException : cassé"));
            return null;
        });
    }

    @Test
    void uneImageDUneSeuleCouleurEstVide() {
        FxTestSupport.fx(() -> {
            WritableImage image = new WritableImage(40, 30);
            for (int x = 0; x < 40; x++) for (int y = 0; y < 30; y++) image.getPixelWriter().setColor(x, y, Color.BLACK);
            assertTrue(UiTestStage.uniform(image));
            image.getPixelWriter().setColor(12, 12, Color.WHITE);
            assertFalse(UiTestStage.uniform(image), "un point d'une autre couleur, sur la grille relevée");
            return null;
        });
    }
}
