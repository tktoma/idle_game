package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.ElementBonuses;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Version 0.202 : la synthèse lit la chance des familles rares et le double tirage sans recalculer tous les bonus des
 * éléments (ils prenaient jusqu'à 42 % du calcul d'une partie tardive). Ce sont les mêmes nombres, au bit près.
 */
class ChanceDesElementsTest {

    @Test
    void laChanceEtLeDoubleTirageSontLesMemesQueDansTousLesBonus() {
        Random random = new Random(202);
        for (int essai = 0; essai < 500; essai++) {
            int[] tableau = new int[129];
            int combien = random.nextInt(PeriodicTable.ELEMENTS.size() + 1);
            for (int i = 0; i < combien; i++) {
                int numero = 1 + random.nextInt(PeriodicTable.ELEMENTS.size());
                tableau[numero] = random.nextInt(4) == 0 ? 1 + random.nextInt(40) : 1;
            }
            if (essai % 5 == 0) for (int n = 1; n <= PeriodicTable.ELEMENTS.size(); n++) tableau[n] = 1 + random.nextInt(3);
            double force = essai % 3 == 0 ? 1.15 : 1;
            ElementBonuses tous = ElementBonuses.of(tableau, force);
            double[] seuls = ElementBonuses.luckAndDoubleDraw(tableau, force);
            assertEquals(tous.luckMultiplier(), seuls[0], 0.0, "chance, essai " + essai);
            assertEquals(tous.doubleDrawChance(), Math.min(1, seuls[1]), 0.0, "double tirage, essai " + essai);
        }
    }

    @Test
    void avantChaqueSyntheseLeJeuLitLeMemeDoubleTirageQueTousLesBonus() {
        // Le double tirage passe par le même calcul gardé que la chance des familles rares : il le vérifie en jeu, synthèse
        // après synthèse, le tableau changeant à chaque fois.
        Game game = GameTestSupport.realGame();
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        for (int i = 0; i < 1_500; i++) {
            game.state().setAtoms(BigNum.of(1e30));
            assertEquals(game.bonuses().doubleDrawChance(), game.doubleDrawChance(), 0.0, "double tirage, synthèse " + i);
            double[] seuls = ElementBonuses.luckAndDoubleDraw(game.state().elementCounts, 1);
            assertEquals(game.bonuses().luckMultiplier(), seuls[0], 0.0, "chance, synthèse " + i);
            game.synthesize();
        }
        assertTrue(game.discoveredElements() > 50, "le tableau s'est rempli : " + game.discoveredElements());
    }
}
