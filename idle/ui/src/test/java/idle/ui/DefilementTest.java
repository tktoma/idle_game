package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.ui.common.WheelScroll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.307 : la molette fait défiler les onglets trois fois plus loin par cran, s'arrête aux bords, passe la main à
 * la page qui entoure quand elle ne peut plus bouger, et un clic sur le rail saute à l'endroit visé.
 */
class DefilementTest {
    @Test
    void unCranVaTroisFoisPlusLoin() {
        // Page de 2 000 pixels dans une fenêtre de 500 : 1 500 pixels de course. Un cran de 40 pixels vers le bas.
        double next = WheelScroll.next(0.5, -40, 2000, 500);
        assertEquals(0.5 + 40 * WheelScroll.SPEED / 1500, next, 1e-12);
        assertEquals(3, WheelScroll.SPEED);
    }

    @Test
    void lesBordsArretentLaPage() {
        assertEquals(0, WheelScroll.next(0.01, 400, 2000, 500), "en haut");
        assertEquals(1, WheelScroll.next(0.99, -400, 2000, 500), "en bas");
        assertEquals(0.3, WheelScroll.next(0.3, -40, 400, 500), "une page qui tient dans sa fenêtre ne bouge pas");
    }

    @Test
    void auBordLaMainPasseALaPageQuiEntoure() {
        assertFalse(WheelScroll.canMove(0, 40, 2000, 500), "déjà en haut, cran vers le haut");
        assertTrue(WheelScroll.canMove(0, -40, 2000, 500));
        assertFalse(WheelScroll.canMove(1, -40, 2000, 500), "déjà en bas, cran vers le bas");
        assertFalse(WheelScroll.canMove(0.5, -40, 400, 500), "rien à faire défiler");
    }

    @Test
    void unClicSurLeRailYSaute() {
        assertEquals(0.25, WheelScroll.jump(100, 400), 1e-12);
        assertEquals(0, WheelScroll.jump(-5, 400));
        assertEquals(1, WheelScroll.jump(500, 400));
        assertEquals(0, WheelScroll.jump(10, 0));
    }
}
