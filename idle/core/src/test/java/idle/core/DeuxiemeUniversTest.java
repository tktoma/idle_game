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

/** Version 0.087 : le deuxième univers, la partie rejouée, l'onglet Skins et ce qui ne s'ouvre qu'à partir de là. */
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

    /** Une partie au premier univers formé : la première dimension est ouverte. */
    private static Game firstUniverse() {
        Game game = new Game();
        game.start();
        formUniverse(game);
        return game;
    }

    @Test
    void laLigneDonneUnDixiemeDEclatParTraitTouche() {
        Game before = new Game();
        before.start();
        before.tick(100);
        assertFalse(before.isLineOpen(), "avant l'univers, pas de dimension");
        assertEquals(0, before.state().dimensionShards(), 0);
        Game game = firstUniverse();
        assertTrue(game.isLineOpen());
        assertEquals(2, game.lineTicks(), "un trait au début, un à la fin");
        game.state().setLinePhase(0);
        game.state().setDimensionShards(0);
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0, game.state().dimensionShards(), 1e-9, "à mi-chemin, aucun trait touché");
        assertEquals(0.5, game.linePosition(), 1e-9);
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0.1, game.state().dimensionShards(), 1e-9, "le bout : 0,1 éclat");
        game.tick(Game.LINE_HALF_TRIP);
        assertEquals(0.2, game.state().dimensionShards(), 1e-9, "retour au début : 0,1 de plus");
        // Un grand pas compte autant que beaucoup de petits.
        Game big = firstUniverse(), small = firstUniverse();
        big.state().setLinePhase(0);
        small.state().setLinePhase(0);
        big.state().setDimensionShards(0);
        small.state().setDimensionShards(0);
        big.tick(100);
        for (int k = 0; k < 1000; k++) small.tick(0.1);
        assertEquals(big.state().dimensionShards(), small.state().dimensionShards(), 1e-6);
        assertEquals(Math.floor(100 / Game.LINE_HALF_TRIP) * 0.1, big.state().dimensionShards(), 1e-6);
    }

    @Test
    void lesDeuxAmeliorationsDeLaLigne() {
        Game game = firstUniverse();
        game.state().setLinePhase(0);
        game.state().setDimensionShards(0.5);
        assertFalse(game.canBuyLineYield(), "il faut " + Game.LINE_YIELD_COST + " éclat");
        game.state().setDimensionShards(Game.LINE_YIELD_COST);
        assertTrue(game.buyLineYield());
        assertEquals(0, game.state().dimensionShards(), 1e-9);
        assertEquals(0.2, game.lineShardsPerTouch(), 1e-9, "0,1 de plus par trait");
        assertEquals(Game.LINE_YIELD_COST * Game.LINE_COST_GROWTH, game.lineYieldCost(), 1e-9);
        game.state().setDimensionShards(Game.LINE_TICKS_COST);
        assertTrue(game.buyLineTicks());
        assertEquals(3, game.lineTicks(), "un trait au milieu");
        game.state().setDimensionShards(0);
        game.state().setLinePhase(0);
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0.2, game.state().dimensionShards(), 1e-9, "le trait du milieu, à égale distance des bouts");
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0.4, game.state().dimensionShards(), 1e-9);
        assertEquals(2 * 0.2 / Game.LINE_HALF_TRIP, game.lineShardsPerSecond(), 1e-12);
        game.state().setLineTicksLevel(Game.LINE_MAX_EXTRA_TICKS);
        game.state().setDimensionShards(1e9);
        assertFalse(game.canBuyLineTicks(), "quarante traits au plus");
        // Les éclats et les améliorations traversent les univers.
        game.state().setDimensionShards(5);
        assertTrue(game.startNextUniverse());
        assertEquals(5, game.state().dimensionShards(), 1e-9);
        assertEquals(1, game.state().lineYieldLevel());
    }

    @Test
    void vitesseBoulesEtLignesMultiplientLesEclats() {
        Game game = firstUniverse();
        double base = game.lineShardsPerSecond();
        assertEquals(0.1 / Game.LINE_HALF_TRIP, base, 1e-12);
        game.state().setDimensionShards(1e6);
        assertTrue(game.buyLine(Game.LineUpgrade.SPEED));
        assertEquals(1.25, game.lineSpeed(), 1e-12, "+25 % de vitesse");
        assertEquals(Game.LINE_HALF_TRIP / 1.25, game.lineHalfTrip(), 1e-12);
        assertTrue(game.buyLine(Game.LineUpgrade.BALLS));
        assertEquals(2, game.lineBalls());
        assertTrue(game.buyLine(Game.LineUpgrade.LINES));
        assertEquals(2, game.lineCount());
        assertEquals(base * 1.25 * 2 * 2, game.lineShardsPerSecond(), 1e-12);
        // Le compte exact suit le débit annoncé, sur un long moment.
        game.state().setDimensionShards(0);
        game.state().setLinePhase(0);
        game.tick(600);
        assertEquals(game.lineShardsPerSecond() * 600, game.state().dimensionShards(), 0.5);
        // Les boules d'une ligne sont réparties : la deuxième est au bout quand la première part.
        game.state().setLinePhase(0);
        assertEquals(0, game.linePosition(0, 0), 1e-12);
        assertEquals(1, game.linePosition(0, 1), 1e-12);
        game.state().setLineCountLevel(Game.LINE_MAX_EXTRA_LINES);
        assertFalse(game.canBuyLine(Game.LineUpgrade.LINES), "cinq lignes au plus");
    }

    @Test
    void leSeuilDeLaLigneCompteLesEclatsGagnes() {
        Game game = firstUniverse();
        assertFalse(game.isLineThresholdReached());
        game.state().setDimensionShards(0);
        game.state().setLinePhase(0);
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD - 0.1);
        game.tick(Game.LINE_HALF_TRIP);
        assertTrue(game.isLineThresholdReached(), "le centième éclat gagné");
        game.state().setDimensionShards(Game.LINE_YIELD_COST);
        assertTrue(game.buyLineYield());
        assertTrue(game.isLineThresholdReached(), "dépenser ne fait pas redescendre sous le seuil");
    }

    @Test
    void laGraineGermeAuSeuilDeLaDimensionUnPuisSArrose() {
        Game game = firstUniverse();
        assertTrue(!game.state().universeSeeds().lt(idle.core.math.BigNum.ONE), "former l'univers donne une graine");
        assertFalse(game.canTendSeed(), "sous le seuil de la dimension 1");
        assertFalse(game.tendSeed());
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        // Il n'y a qu'une graine, celle de l'univers : le bouton ne demande pas de graine en réserve (0.105).
        game.state().setUniverseSeeds(idle.core.math.BigNum.ZERO);
        assertTrue(game.canTendSeed(), "l'objectif atteint suffit");
        assertTrue(game.tendSeed());
        assertTrue(game.state().seedSprouted(), "le premier clic la fait germer");
        assertEquals(0, game.state().seedWaterings());
        assertTrue(game.tendSeed());
        assertTrue(game.tendSeed());
        assertEquals(2, game.state().seedWaterings(), "les suivants arrosent le germe");
        assertTrue(game.startNextUniverse());
        assertTrue(game.state().seedSprouted(), "le germe traverse les univers");
        assertEquals(2, game.state().seedWaterings());
    }

    @Test
    void lePlanSeJoueAuClavierEtRamasseSesEclats() {
        Game game = firstUniverse();
        assertFalse(game.isPlaneOpen(), "un seul univers : la deuxième dimension est fermée");
        game.testAddUniverse();
        assertTrue(game.isPlaneOpen());
        game.tick(Game.PLANE_SPAWN_SECONDS * 10);
        assertEquals(Game.PLANE_MAX_SHARDS, game.planeShardsOnBoard().size(), "trois éclats au plus");
        // Le point va à droite, et ne sort pas du plan.
        game.setPlanePosition(0, 0);
        game.setPlaneInput(1, 0);
        for (int k = 0; k < 200; k++) game.tick(0.1);
        assertEquals(Game.PLANE_EDGE - game.planePlayerRadius(), game.planeX(), 1e-9, "arrêté au bord");
        game.setPlaneInput(0, 0);
        // Passer sur un éclat le ramasse : un éclat du plan, propre à cette dimension.
        double line = game.state().dimensionShards();
        game.state().setPlaneShards(0);
        game.setPlanePosition(0, 0);
        int before = game.planeShardsOnBoard().size();
        java.util.List<Game.PlaneShard> left = new java.util.ArrayList<>(game.planeShardsOnBoard());
        for (Game.PlaneShard shard : left) game.setPlanePosition(shard.x(), shard.y());
        game.setPlanePosition(left.get(0).x(), left.get(0).y());
        game.tick(0.01);
        assertTrue(game.state().planeShards() >= 1, "au moins un éclat ramassé");
        assertTrue(game.planeShardsOnBoard().size() < before);
        assertEquals(line, game.state().dimensionShards(), 1e-6, "les éclats de la ligne ne bougent pas");
    }

    @Test
    void lesCinqAmeliorationsDuPlan() {
        Game game = firstUniverse();
        game.testAddUniverse();
        game.state().setPlaneShards(1e6);
        for (Game.PlaneUpgrade upgrade : Game.PlaneUpgrade.values()) assertTrue(game.buyPlane(upgrade), upgrade.name());
        assertEquals(2, game.planeValue(), 1e-12);
        assertEquals(Game.PLANE_SPAWN_SECONDS * 0.85, game.planeSpawnSeconds(), 1e-12);
        assertEquals(Game.PLANE_MAX_SHARDS + 1, game.planeMaxShards());
        assertEquals(Game.PLANE_SPEED * 1.2, game.planeSpeed(), 1e-12);
        assertEquals(Game.PLANE_PLAYER_RADIUS * (1 + Game.PLANE_SIZE_STEP), game.planePlayerRadius(), 1e-12);
        game.state().setPlaneLevel(Game.PlaneUpgrade.COUNT.ordinal(), Game.PlaneUpgrade.COUNT.max());
        assertFalse(game.canBuyPlane(Game.PlaneUpgrade.COUNT), "vingt éclats au plus");
        assertEquals(20, game.planeMaxShards());
        game.state().setPlaneShards(7);
        assertTrue(game.startNextUniverse());
        assertEquals(7, game.state().planeShards(), 1e-9, "les éclats du plan traversent les univers");
    }

    @Test
    void laBrindilleOuvreLeDeuxiemeUnivers() {
        Game game = firstUniverse();
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        assertFalse(game.canOpenNextUniverse(), "pas avant la brindille");
        assertTrue(game.tendSeed());
        for (long k = 1; k < Game.SEED_TWIG_WATERINGS; k++) assertTrue(game.tendSeed());
        assertFalse(game.isSeedTwig(), "encore un germe à " + (Game.SEED_TWIG_WATERINGS - 1) + " arrosages");
        assertTrue(game.tendSeed());
        assertTrue(game.isSeedTwig(), "la brindille au trentième arrosage");
        assertTrue(game.canOpenNextUniverse());
        // Ce qui aurait été joué entre-temps (anciens onglets montrés, partie d'avant) ne passe pas dans l'univers 2.
        game.state().setParticles(idle.core.math.BigNum.of(1, 50));
        game.state().setAtoms(idle.core.math.BigNum.of(1, 9));
        game.state().setBigBangs(3);
        game.state().setDarkMatter(idle.core.math.BigNum.of(500));
        double shards = game.state().dimensionShards();
        assertTrue(game.startNextUniverse());
        assertTrue(game.isReplaying(), "le jeu recommence, pour l'univers 2");
        assertEquals(0, game.state().particles().sign(), "les trois actes repartent de zéro : les particules");
        assertEquals(0, game.state().atoms().sign(), "les atomes");
        assertEquals(0, game.state().bigBangs(), "les Big Bangs");
        assertEquals(0, game.state().darkMatter().sign(), "la matière noire");
        assertTrue(game.isSeedTwig(), "la graine garde sa brindille");
        assertEquals(shards, game.state().dimensionShards(), 1e-9, "les dimensions gardent leurs éclats");
        assertEquals(1, game.universes(), "l'onglet Univers reste : un univers déjà formé");
        assertFalse(game.canOpenNextUniverse(), "pendant la partie, on ne rouvre rien");
    }

    @Test
    void leVideSOuvreAuPremierBigBangDuDeuxiemeUnivers() {
        Game first = new Game();
        first.state().setBigBangs(3);
        assertFalse(first.isVoidUnlocked(), "pas dans le premier univers, même après des Big Bangs");
        Game game = second();
        assertFalse(game.isVoidUnlocked(), "avant le premier Big Bang du deuxième univers");
        game.state().setBigBangs(1);
        assertTrue(game.isVoidUnlocked());
        assertEquals(0, game.state().voidEnergy().sign(), "pour l'instant sans logique : rien ne s'y gagne");
        game.tick(60);
        assertEquals(0, game.state().voidEnergy().sign());
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
