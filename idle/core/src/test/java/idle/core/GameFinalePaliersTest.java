package idle.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste que les paliers du défi final ont été ajustés correctement.
 */
class GameFinalePaliersTest {

    @Test
    void lesPaliersSontAjustes() {
        double facteurAttendu = 0.53;
        
        // Vérifier qu'un palier a bien le facteur appliqué (ex: ligne)
        double palierLigneActuel = Game.finalGoal(1);
        double palierLigneOriginal = 100 * GameUniverseBase.LINE_THRESHOLD;
        
        assertEquals(facteurAttendu, palierLigneActuel / palierLigneOriginal, 0.001, 
            "Le facteur d'échelle doit être appliqué");
    }

    @Test
    void leMelangeEstAussiAjuste() {
        double facteurAttendu = 0.53;
        
        // Vérifier que le palier du mélange (dimension 10) est ajusté
        double palierMixActuel = Game.finalGoal(10);
        double palierMixOriginal = 1e7; /* Mélange: 10^7 fragments */
        
        assertEquals(facteurAttendu, palierMixActuel / palierMixOriginal, 0.001, 
            "Le palier du mélange doit être ajusté");
    }
}
