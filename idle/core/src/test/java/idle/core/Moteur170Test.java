package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.Element;
import idle.core.elements.ElementBonuses;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.universe.Skin;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Version 0.170 : le moteur calcule plus vite les mêmes nombres exactement. Le cumul des bonus des éléments lu dans le
 * tableau des exemplaires vaut celui lu dans la table ; le tirage des éléments, sans listes, respecte toujours les
 * plafonds et tire bien parmi ce qui reste ; l'identifiant d'un skin ne change pas.
 */
class Moteur170Test extends GameTestSupport {

    @Test
    void lesBonusDesElementsSontLesMemesDepuisLaTableOuLeTableau() {
        Random random = new Random(170);
        for (int essai = 0; essai < 200; essai++) {
            Map<Integer, Integer> table = essai % 2 == 0 ? new TreeMap<>() : new HashMap<>();
            int[] tableau = new int[PeriodicTable.ELEMENTS.size() + 1];
            int combien = random.nextInt(PeriodicTable.ELEMENTS.size() + 1);
            for (int i = 0; i < combien; i++) {
                int numero = 1 + random.nextInt(PeriodicTable.ELEMENTS.size());
                int exemplaires = random.nextInt(4) == 0 ? 1 + random.nextInt(25) : 1;
                table.put(numero, exemplaires);
                tableau[numero] = exemplaires;
            }
            if (essai % 7 == 0) {                                        // un élément à zéro exemplaire ne compte pas
                int zero = 1 + random.nextInt(118);
                table.put(zero, 0);
                tableau[zero] = 0;
            }
            double force = essai % 3 == 0 ? 1.15 : 1;
            ElementBonuses a = ElementBonuses.of(table, force), b = ElementBonuses.of(tableau, force);
            memes(a, b);
        }
    }

    private static void memes(ElementBonuses a, ElementBonuses b) {
        for (ElementEffect.Stat stat : ElementEffect.Stat.values()) assertEquals(a.stat(stat), b.stat(stat), stat.toString());
        for (int g = 0; g <= 12; g++) {
            assertEquals(a.generatorParticles(g), b.generatorParticles(g));
            assertEquals(a.generatorSpeed(g), b.generatorSpeed(g));
        }
        for (ElementEffect.CostTarget t : ElementEffect.CostTarget.values()) assertEquals(a.costDivisor(t), b.costDivisor(t));
        for (ElementEffect.AutomationTarget t : ElementEffect.AutomationTarget.values()) assertEquals(a.automationDivisor(t), b.automationDivisor(t));
        for (ElementEffect.UpgradeTarget t : ElementEffect.UpgradeTarget.values()) assertEquals(a.upgradeBoost(t), b.upgradeBoost(t));
        assertEquals(a.synergies(), b.synergies());
        assertEquals(a.doubleDrawChance(), b.doubleDrawChance());
        assertEquals(a.luckMultiplier(), b.luckMultiplier());
        assertEquals(a.completedSets(), b.completedSets());
        assertEquals(a.halfSets(), b.halfSets());
    }

    @Test
    void lesBonusDuJeuSuiventLeTableauDesExemplaires() {
        Game game = realGame();
        game.state().setElementCount(26, 4);     // du fer, deux fois plus fort qu'un exemplaire
        game.state().setElementCount(1, 1);
        game.state().setElementCount(2, 1);      // la première période complète
        ElementBonuses attendu = ElementBonuses.of(game.state().elements(), 1);
        for (ElementEffect.Stat stat : ElementEffect.Stat.values()) {
            assertEquals(attendu.stat(stat), game.bonuses().stat(stat), stat.toString());
        }
        assertEquals(attendu.completedSets(), game.completedSets());
    }

    @Test
    void laSyntheseRespecteLesPlafondsEtTireParmiCeQuiReste() {
        Game game = realGame();
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        Random random = new Random(7);
        int[] vus = new int[PeriodicTable.ELEMENTS.size() + 1];
        for (int i = 0; i < 3000 && !game.isPeriodicTableComplete(); i++) {
            game.state().setAtoms(BigNum.of(1e12));
            if (i == 0) game.state().setDarkLevel("dark_shop_target", 1);
            if (random.nextInt(4) == 0) {
                ElementCategory[] categories = ElementCategory.values();
                game.setSynthesisTarget(categories[random.nextInt(categories.length)]);
            } else {
                game.setSynthesisTarget(null);
            }
            for (Element element : game.synthesize()) vus[element.number()]++;
        }
        assertTrue(game.isPeriodicTableComplete(), "trois mille synthèses remplissent le tableau");
        for (Element element : PeriodicTable.ELEMENTS) {
            int possedes = game.elementCount(element.number());
            assertEquals(game.maxCopiesOf(element), possedes, element.symbol() + " au plafond");
            assertEquals(possedes, vus[element.number()], element.symbol() + " : chaque exemplaire vient d'une synthèse");
            assertTrue(game.isElementMaxed(element.number()));
        }
        assertFalse(game.canSynthesize(), "plus rien à synthétiser");
    }

    @Test
    void laSyntheseCibleeDonneDAbordLesElementsInconnusDeLaFamille() {
        Game game = realGame();
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        game.state().setDarkLevel("dark_shop_target", 1);
        assertTrue(game.isTargetedSynthesisUnlocked());
        ElementCategory famille = ElementCategory.ALKALI_METAL;
        assertTrue(game.setSynthesisTarget(famille));
        int membres = PeriodicTable.elements(famille).size();
        int connus = 0;
        // Tant qu'il reste des métaux alcalins inconnus, chaque alcalin obtenu est un nouveau (la synthèse garantie d'un
        // élément unique, qui passe avant la cible, donne autre chose : elle ne compte pas).
        for (int i = 0; i < 200 && connus < membres; i++) {
            game.state().setAtoms(BigNum.of(1e12));
            for (Element element : game.synthesize()) {
                if (element.category() != famille) continue;
                assertEquals(1, game.elementCount(element.number()), element.symbol() + " : un alcalin déjà connu avant les autres");
                connus++;
            }
        }
        assertEquals(membres, connus, "tous les métaux alcalins en moins de deux cents synthèses");
        for (Element element : PeriodicTable.elements(famille)) assertEquals(1, game.elementCount(element.number()), element.symbol());
        // La famille entière connue, la cible donne des exemplaires de plus, sans dépasser le plafond.
        for (int i = 0; i < 200 && game.synthesisTarget() == famille; i++) {
            game.state().setAtoms(BigNum.of(1e12));
            game.synthesize();
        }
        for (Element element : PeriodicTable.elements(famille)) {
            assertTrue(game.elementCount(element.number()) <= game.maxCopiesOf(element), element.symbol());
        }
    }

    @Test
    void lIdentifiantDUnSkinEstSonNomEnMinuscules() {
        for (Skin skin : Skin.values()) {
            assertEquals(skin.name().toLowerCase(Locale.ROOT), skin.id());
        }
    }
}
