package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.sky.Cosmos;
import idle.core.universe.CycleChoice;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import org.junit.jupiter.api.Test;

/** Version 0.87 : le deuxième univers, la partie rejouée, l'onglet Skins et ce qui ne s'ouvre qu'à partir de là. */
class DeuxiemeUniversTest {

    /** Une méthode du jeu, cherchée dans toute la chaîne de ses parties. */
    private static java.lang.reflect.Method declared(String name) throws NoSuchMethodException {
        for (Class<?> c = Game.class; c != null; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
        }
        throw new NoSuchMethodException(name);
    }


    /** Forme l'univers comme formCosmos le fait une fois les conditions réunies. */
    static void formUniverse(Game game) {
        game.state().setCosmosLevel(3);
        try {
            java.lang.reflect.Method form = declared("formUniverse");
            form.setAccessible(true);
            form.invoke(game);
        } catch (ReflectiveOperationException broken) {
            throw new AssertionError(broken);
        }
    }

    /** Une partie dans le deuxième univers : le premier formé, les paliers rouverts. */
    static Game second() {
        Game game = new Game();
        game.start();
        formUniverse(game);
        assertTrue(game.startNextUniverse());
        return game;
    }

    private static GameState reread(GameState state) {
        try {
            return SaveCodec.read(SaveCodec.write(state, 0)).state();
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
    }

    @Test
    void laPartieRejoueeMeneALUniversSuivant() {
        Game game = new Game();
        game.start();
        assertFalse(game.isSecondUniverse());
        assertFalse(game.canStartNextUniverse());
        formUniverse(game);
        assertTrue(game.isSecondUniverse() && game.hasCosmos(Cosmos.UNIVERSE) && !game.isReplaying());
        assertEquals(1, game.universes());
        assertTrue(game.canStartNextUniverse());
        assertTrue(game.startNextUniverse());
        assertFalse(game.startNextUniverse(), "une seule fois par univers");
        // Rejouée : l'univers de cette partie n'est pas formé, mais le premier compte toujours.
        assertTrue(game.isReplaying() && !game.hasCosmos(Cosmos.UNIVERSE) && game.isUniverseFormed());
        assertEquals(1, game.universes());
        assertEquals(Cosmos.GALAXY, game.nextCosmos() == null ? Cosmos.GALAXY : game.nextCosmos());
        assertTrue(reread(game.state()).replaying());
        // L'univers formé de nouveau : le deuxième, une graine de plus, tout remis à zéro.
        game.state().setBigBangs(4);
        formUniverse(game);
        assertEquals(2, game.universes());
        assertEquals(BigNum.of(2), game.state().universeSeeds());
        assertEquals(0, game.bigBangs());
        assertFalse(game.isReplaying());
        assertTrue(game.hasCosmos(Cosmos.UNIVERSE));
        assertTrue(game.isDimensionsUnlocked(), "deux univers : les dimensions s'ouvrent");
    }

    @Test
    void lOngletSkinsSOuvreACinqFusionsDuDeuxiemeUnivers() {
        Game game = new Game();
        game.start();
        game.state().stats();
        formUniverse(game);
        assertFalse(game.isSkinsUnlocked());
        long base = game.state().fusionsAtUniverse();
        assertEquals(game.stats().fusions(), base);
        game.startNextUniverse();
        for (int i = 0; i < Game.SKINS_FUSIONS - 1; i++) game.state().stats().noteFusion(BigNum.ONE, 1);
        game.tick(0.1);
        assertFalse(game.isSkinsUnlocked(), "quatre fusions : pas encore");
        game.state().stats().noteFusion(BigNum.ONE, 1);
        game.tick(0.1);
        assertTrue(game.isSkinsUnlocked());
        // L'univers suivant le garde ouvert, comme les skins débloqués et choisis.
        assertTrue(game.unlockSkin(Skin.DARK_VORTEX));
        assertFalse(game.unlockSkin(Skin.DARK_VORTEX), "une seule fois");
        assertTrue(game.chooseSkin(SkinVisual.DARK_ANIMATION, Skin.DARK_VORTEX));
        assertFalse(game.chooseSkin(SkinVisual.ATOM, Skin.DARK_VORTEX), "pas pour un autre visuel");
        assertFalse(game.chooseSkin(SkinVisual.ATOM, Skin.ATOM_DENSE), "pas un skin verrouillé");
        formUniverse(game);
        assertTrue(game.isSkinsUnlocked() && game.hasSkin(Skin.DARK_VORTEX));
        assertEquals(Skin.DARK_VORTEX, game.chosenSkin(SkinVisual.DARK_ANIMATION));
        assertNull(game.chosenSkin(SkinVisual.ATOM));
        assertTrue(game.chooseSkin(SkinVisual.DARK_ANIMATION, null));
        assertNull(game.chosenSkin(SkinVisual.DARK_ANIMATION), "retour au classique");
    }

    @Test
    void chaqueSkinDonneUnPetitPourcentageATouteLesRessources() {
        Game game = second();
        double before = game.moleculeBoost(Molecule.Stat.PARTICLES);
        double atoms = game.moleculeBoost(Molecule.Stat.ATOMS);
        game.unlockSkin(Skin.FUSION_FLASH);
        game.unlockSkin(Skin.BANG_AURORA);
        assertEquals(1 + 2 * Skin.BONUS, game.skinBoost(), 1e-12);
        assertEquals(before * (1 + 2 * Skin.BONUS), game.moleculeBoost(Molecule.Stat.PARTICLES), before * 1e-9);
        assertEquals(atoms * (1 + 2 * Skin.BONUS), game.moleculeBoost(Molecule.Stat.ATOMS), atoms * 1e-9);
    }

    @Test
    void leRegimeDesGenerateursDure20SPuisSeRepose40S() {
        Game game = second();
        assertFalse(game.isRegimeUnlocked());
        game.state().setLevel("generator", Game.REGIME_GENERATORS - 1);
        game.tick(0.01);
        assertTrue(game.generatorCount() >= Game.REGIME_GENERATORS, "trois générateurs : " + game.generatorCount());
        assertTrue(game.isRegimeUnlocked());
        BigNum normal = game.speed(0);
        assertTrue(game.startRegime());
        assertFalse(game.startRegime(), "déjà en cours");
        assertEquals(normal.toDouble() * (1 + Game.REGIME_GAIN), game.speed(0).toDouble(), normal.toDouble() * 1e-9);
        game.tick(Game.REGIME_SECONDS + 0.5);
        assertEquals(1, game.regimeMultiplier(), 0);
        assertFalse(game.canStartRegime(), "au repos");
        game.tick(Game.REGIME_REST_SECONDS);
        assertTrue(game.canStartRegime(), "reposé : il se relance");
        // Hors du deuxième univers, il ne s'ouvre pas.
        Game first = new Game();
        first.start();
        first.state().setLevel("generator", Game.REGIME_GENERATORS - 1);
        first.tick(0.01);
        assertFalse(first.isRegimeUnlocked());
    }

    @Test
    void laSynergieParRangEtLaPrimeDeCycleParfait() {
        Game game = second();
        game.state().setLevel("generator", Game.SYNERGY_GENERATORS - 1);
        game.tick(0.01);
        assertTrue(game.isSynergyUnlocked());
        assertEquals(1 + Game.SYNERGY_MAX, game.synergyMultiplier(), 1e-12, "dix rangs : +0,5 %");
        // La prime : trois fusions sans le plafond d'atomes, après Persistance.
        assertFalse(game.isPerfectCycleUnlocked());
        game.state().setLevel("atom_keep", 1);
        assertTrue(game.isPerfectCycleUnlocked());
        for (int fusion = 0; fusion < Game.PERFECT_FUSIONS; fusion++) {
            game.state().setLevel("generator", game.generatorsPerAtom() - 1);
            assertTrue(game.fuse(), "fusion " + fusion);
        }
        assertEquals(1 + Game.PERFECT_GAIN, game.perfectCycleMultiplier(), 1e-12);
        game.tick(Game.PERFECT_SECONDS + 1);
        assertEquals(1, game.perfectCycleMultiplier(), 0, "elle s'éteint");
    }

    @Test
    void lePetitArbreDuCycleSeChoisitUneFoisEtSeRendAuBigBang() {
        Game game = second();
        assertFalse(game.isCycleChoiceUnlocked(), "avant le premier Big Bang");
        game.state().setBigBangs(1);
        assertTrue(game.isCycleChoiceUnlocked());
        BigNum atoms = game.atomsPerFusion();
        assertTrue(game.chooseCycle(CycleChoice.FUSION));
        assertFalse(game.chooseCycle(CycleChoice.RESERVE), "un seul choix par cycle");
        assertEquals(atoms.toDouble() * (1 + CycleChoice.FUSION_GAIN), game.atomsPerFusion().toDouble(), atoms.toDouble() * 1e-9);
        try {
            java.lang.reflect.Method clear = declared("clearForBigBang");
            clear.setAccessible(true);
            clear.invoke(game);
        } catch (ReflectiveOperationException broken) {
            throw new AssertionError(broken);
        }
        assertNull(game.cycleChoice(), "le Big Bang le rend");
        assertTrue(game.chooseCycle(CycleChoice.RESERVE));
        BigNum dark = game.darkMatterPerExplosion();
        game.state().setCycleChoice(null);
        assertEquals(dark.toDouble() / (1 + CycleChoice.RESERVE_GAIN), game.darkMatterPerExplosion().toDouble(), 1e-9);
    }

    @Test
    void laPreferenceDeFamilleMonteAvecLesTiragesEtLesElementsGratuits() {
        Game game = second();
        game.state().setDarkLevel("dark_targeting", 1);
        if (!game.isTargetedSynthesisUnlocked()) {
            for (var dark : game.darkUpgrades()) {
                if (dark.effect() instanceof idle.core.darkmatter.DarkEffect.TargetedSynthesis) game.state().setDarkLevel(dark.id(), 1);
            }
        }
        assertTrue(game.isFamilyPreferenceUnlocked());
        double chance = game.categoryChance(ElementCategory.NOBLE_GAS);
        assertTrue(game.setPreferredFamily(ElementCategory.NOBLE_GAS));
        assertEquals(chance, game.categoryChance(ElementCategory.NOBLE_GAS), 1e-12, "aucun tirage : rien encore");
        game.state().setPreferenceDraws(Game.PREFERENCE_MAX_DRAWS + 5);
        assertEquals(2, game.preferenceMultiplier(ElementCategory.NOBLE_GAS), 1e-12, "×2 au plus");
        assertEquals(1, game.preferenceMultiplier(ElementCategory.HALOGEN), 0);
        assertTrue(game.categoryChance(ElementCategory.NOBLE_GAS) > chance);
        // Les éléments gratuits : un tous les dix découverts.
        assertEquals(0, game.freeElementsAvailable());
        for (int n = 1; n <= 20; n++) game.state().setElementCount(n, 1);
        assertEquals(2, game.freeElementsAvailable());
        assertFalse(game.claimFreeElement(5), "déjà découvert");
        assertTrue(game.claimFreeElement(92));
        assertEquals(1, game.elementCount(92));
        assertEquals(1, game.freeElementsAvailable());
        Game first = new Game();
        first.start();
        for (int n = 1; n <= 20; n++) first.state().setElementCount(n, 1);
        assertEquals(0, first.freeElementsAvailable(), "pas avant le deuxième univers");
        assertEquals(PeriodicTable.ELEMENTS.size(), 118);
    }
}
