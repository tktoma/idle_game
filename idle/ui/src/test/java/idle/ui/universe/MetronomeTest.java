package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le métronome de la boucle ({@link LoopBeat}) : un tic à chaque huitième de tour, calé sur la première zone ; ni deux
 * tics pour un battement, ni rattrapage après une fenêtre fermée. Sans affichage.
 */
class MetronomeTest {
    @Test
    void unTicParQuartDeTourCaleSurLaPremiereZone() {
        double offset = 0.6;    // battements à 0,1 · 0,225 · 0,35 · 0,475 · 0,6 · 0,725 · 0,85 · 0,975
        assertTrue(LoopBeat.crossed(0.55, 0.62, offset), "le point passe 0,6");
        assertFalse(LoopBeat.crossed(0.61, 0.7, offset), "entre deux battements : rien");
        assertTrue(LoopBeat.crossed(0.8, 0.86, offset));
        assertTrue(LoopBeat.crossed(0.99, 0.12, offset), "au passage du tour");
        assertFalse(LoopBeat.crossed(0.6, 0.62, offset), "posé sur le battement : déjà joué");
        assertFalse(LoopBeat.crossed(0.1, 0.8, offset), "plus d'un demi-tour : la fenêtre était fermée");
        assertFalse(LoopBeat.crossed(Double.NaN, 0.5, offset));
        // Un tour entier par petits pas : exactement huit tics.
        int ticks = 0;
        double phase = 0.33;
        for (int step = 0; step < 100; step++) {
            double next = (phase + 0.01) % 1;
            if (LoopBeat.crossed(phase, next, offset)) ticks++;
            phase = next;
        }
        assertTrue(ticks == 8, "huit tics par tour : " + ticks);
    }
}
