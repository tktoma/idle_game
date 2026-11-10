package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.ui.FxTestSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.203 : l'eau sort de ce qui la verse. L'arrosoir verse en (0,95 s ; −0,42 s) dans son repère : chaque skin de
 * l'arrosoir, le coquillage refait compris, est dessiné jusque-là (avant, le coquillage s'arrêtait à mi-chemin et l'eau
 * naissait en l'air). La fontaine a un bec qui finit à la lance du tuyau, d'où part le jet.
 */
class BecDuCoquillageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static WritableImage shot(Canvas canvas) {
        javafx.scene.SnapshotParameters parameters = new javafx.scene.SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, null);
    }

    /** Vrai si quelque chose est dessiné à moins de {@code reach} pixels de ({@code x}, {@code y}). */
    private static boolean inkedNear(WritableImage image, double x, double y, int reach) {
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                int px = (int) Math.round(x) + dx, py = (int) Math.round(y) + dy;
                if (px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight()) continue;
                if ((image.getPixelReader().getArgb(px, py) >>> 24) > 40) return true;
            }
        }
        return false;
    }

    @Test
    void chaqueArrosoirEstDessineJusquAuBec() {
        FxTestSupport.fx(() -> {
            double s = 100;
            for (Skin skin : Skin.values()) {
                if (skin.visual() != idle.core.universe.SkinVisual.WATERING_CAN) continue;
                for (double degrees : new double[] {0, 35}) {
                    Canvas canvas = new Canvas(400, 400);
                    ToolsSkinArt.can(canvas.getGraphicsContext2D(), 200, 200, s, degrees, false, skin);
                    double a = Math.toRadians(degrees), lx = s * 0.95, ly = -s * 0.42;
                    double x = 200 + lx * Math.cos(a) - ly * Math.sin(a), y = 200 + lx * Math.sin(a) + ly * Math.cos(a);
                    assertTrue(inkedNear(shot(canvas), x, y, 9), skin.label() + " penché de " + degrees + "° : dessiné jusqu'au bec, là où l'eau sort");
                }
            }
            return null;
        });
    }

    @Test
    void laFontaineAUnBecQuiFinitALaLance() {
        FxTestSupport.fx(() -> {
            double s = 60;
            double[] tip = {300, 200};
            Canvas canvas = new Canvas(500, 400);
            ToolsSkinArt.hose(canvas.getGraphicsContext2D(), s, tip, 380, Skin.HOSE_FOUNTAIN);
            WritableImage image = shot(canvas);
            assertTrue(inkedNear(image, tip[0], tip[1], 5), "le bec arrive à la lance");
            // Rien au-delà de la lance, du côté de la plante : plus de vasque sous le jet.
            for (int x = (int) (tip[0] + s * 0.15); x < 500; x += 4) {
                for (int y = 0; y < 400; y += 4) assertTrue((image.getPixelReader().getArgb(x, y) >>> 24) == 0, "rien entre la lance et la plante (" + x + ", " + y + ")");
            }
            return null;
        });
    }
}
