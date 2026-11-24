package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.ElementBonuses;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.bigbang.BangChallenge;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Version 0.298 : les raccourcis du moteur donnent exactement ce que donnaient les anciens calculs. Chaque test refait
 * l'ancienne façon à côté de la nouvelle. (La partie témoin des bots, rejouée à l'identique, le vérifie en grand.)
 */
class Optimisations298Test {
    @Test
    void lesPaliersDeTailleSeComptentSansBoucle() {
        for (int reached = 0; reached <= SizeScale.MILESTONES.size(); reached++) {
            for (SizeScale.Bonus bonus : SizeScale.Bonus.values()) {
                int loop = 0;
                for (int index = 0; index < reached; index++) if (SizeScale.bonusOf(index) == bonus) loop++;
                assertEquals(loop, GameDarkMatter.landmarksOf(reached, bonus), reached + " " + bonus);
            }
        }
    }

    @Test
    void laChanceNeLitQueSesElementsEtDonneLaMemeSomme() {
        Random random = new Random(5);
        for (int essai = 0; essai < 200; essai++) {
            int[] counts = new int[PeriodicTable.ELEMENTS.size() + 1];
            for (int z = 1; z < counts.length; z++) counts[z] = random.nextInt(4) == 0 ? random.nextInt(10) : 0;
            double strength = essai % 2 == 0 ? 1 : 1.15;
            double[] fast = ElementBonuses.luckAndDoubleDraw(counts, strength);
            ElementBonuses full = ElementBonuses.of(counts, strength);
            assertEquals(full.luckMultiplier(), fast[0], 1e-12);
            assertEquals(full.doubleDrawChance(), Math.min(1, fast[1]), 1e-12);
        }
    }

    @Test
    void lesFamillesEtLesPeriodesSontCellesDuTableau() {
        for (ElementCategory category : ElementCategory.values()) {
            List<Element> filtered = PeriodicTable.ELEMENTS.stream().filter(e -> e.category() == category).toList();
            assertEquals(filtered, PeriodicTable.elements(category));
            assertSame(PeriodicTable.elements(category), PeriodicTable.elements(category), "construite une fois");
        }
        for (int period = 1; period <= PeriodicTable.PERIODS; period++) {
            int p = period;
            assertEquals(PeriodicTable.ELEMENTS.stream().filter(e -> PeriodicTable.period(e.number()) == p).toList(),
                    PeriodicTable.elementsOfPeriod(period));
        }
    }

    @Test
    void lIdentifiantDUnDefiDUniversNeChangePas() {
        for (BangChallenge challenge : BangChallenge.values()) {
            assertEquals("univ_bang_" + challenge.name().toLowerCase(Locale.ROOT), Achievements.universeBangId(challenge));
            assertTrue(Achievements.byId(Achievements.universeBangId(challenge)) != null, challenge.name());
        }
    }

    @Test
    void lesPrixGardesSontLesPrixDuCatalogue() {
        // Une partie qui a déjà tout calculé une fois, et une partie neuve pour chaque prix : les mêmes nombres.
        Game warm = GameTestSupport.realGame();
        for (int level : new int[] {0, 1, 7, 25, 301, 350, 4095, 4096, 5000}) {
            for (Upgrade upgrade : warm.upgrades()) warm.costAt(upgrade, level);
        }
        for (Upgrade upgrade : warm.upgrades()) {
            for (int level : new int[] {0, 1, 7, 25, 301, 350, 4095, 4096, 5000}) {
                assertEquals(GameTestSupport.realGame().costAt(upgrade, level), warm.costAt(upgrade, level), upgrade.id() + " niveau " + level);
            }
        }
    }
}
