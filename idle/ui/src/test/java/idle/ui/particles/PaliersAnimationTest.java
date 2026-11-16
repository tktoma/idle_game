package idle.ui.particles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * L'animation d'un générateur suit son palier de vitesse : l'allure repart de zéro à chaque palier, et une onde qui
 * atteint le noyau fait sonner la particule récupérée, au palier du générateur.
 */
class PaliersAnimationTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void lAllureRepartDeZeroAChaquePalierEtLaParticuleSonneALArrivee() {
        FxTestSupport.fx(() -> {
            List<Integer> heard = new ArrayList<>();
            ParticleView.setOnCollected(heard::add);
            try {
                ParticleView view = new ParticleView();
                // Fin du deuxième palier (10 à 100 par seconde) : l'allure y est presque pleine.
                for (int k = 0; k < 80; k++) view.frame(0.05, 1, 95, Math.log10(95));
                assertEquals(1, view.stage());
                assertTrue(view.intensity() > 0.8, "presque au bout du palier : " + view.intensity());
                // Un palier de plus : l'allure repart de zéro.
                view.frame(0.05, 1, 150, Math.log10(150));
                assertEquals(2, view.stage());
                assertTrue(view.intensity() < 0.1, "elle repart de zéro : " + view.intensity());
                // Très vite, sans plafond : le dernier palier, et des particules récupérées qui sonnent à ce palier.
                for (int k = 0; k < 60; k++) view.frame(0.05, 1, 1e9, 4200);
                assertEquals(13, view.stage());
                assertTrue(heard.contains(13), "une onde a atteint le noyau au dernier palier : " + heard);
            } finally {
                ParticleView.setOnCollected(stage -> { });
            }
            return null;
        });
    }
}
