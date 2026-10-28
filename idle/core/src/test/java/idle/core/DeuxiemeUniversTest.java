package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Cosmos;
import idle.core.universe.CycleChoice;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import org.junit.jupiter.api.Test;

/**
 * Les univers après le premier : la partie rejouée qui mène au deuxième univers, l'onglet Skins, le régime et la
 * synergie, la graine et ses paliers qui ouvrent les univers suivants, la brindille, les défis de Big Bang du deuxième
 * univers, la fuite, le choix du cycle et la préférence de famille. Les dimensions 1 et 2 ont leurs propres classes
 * ({@link LigneTest}, {@link PlanTest}).
 */
class DeuxiemeUniversTest {

    /** Une partie dans le deuxième univers : le premier formé, la partie suivante lancée, les paliers rouverts. */
    static Game second() {
        Game game = GameTestSupport.universe(1);
        assertTrue(game.startNextUniverse());
        return game;
    }

    @Test
    void laPartieRejoueeMeneALUniversSuivant() {
        Game game = GameTestSupport.realGame();
        assertFalse(game.isSecondUniverse());
        assertFalse(game.canStartNextUniverse());
        game.testFormUniverse();
        assertTrue(game.isSecondUniverse() && game.hasCosmos(Cosmos.UNIVERSE) && !game.isReplaying());
        assertEquals(1, game.universes());
        assertTrue(game.canStartNextUniverse());
        assertTrue(game.startNextUniverse());
        assertFalse(game.startNextUniverse(), "une seule fois par univers");
        // Rejouée : l'univers de cette partie n'est pas formé, mais le premier compte toujours.
        assertTrue(game.isReplaying() && !game.hasCosmos(Cosmos.UNIVERSE) && game.isUniverseFormed());
        assertEquals(1, game.universes());
        assertEquals(Cosmos.GALAXY, game.nextCosmos(), "le cosmos de la partie rejouée repart de la galaxie");
        assertTrue(GameTestSupport.reread(game.state()).replaying());
        // L'univers formé de nouveau : le deuxième, une graine de plus, tout remis à zéro.
        GameTestSupport.bangs(game, 4);
        game.testFormUniverse();
        assertEquals(2, game.universes());
        assertEquals(BigNum.of(2), game.state().universeSeeds());
        assertEquals(0, game.bigBangs());
        assertFalse(game.isReplaying());
        assertTrue(game.hasCosmos(Cosmos.UNIVERSE));
        assertTrue(game.isDimensionsUnlocked(), "deux univers : les dimensions s'ouvrent");
    }

    @Test
    void lOngletSkinsSOuvreACinqFusionsDuDeuxiemeUnivers() {
        Game game = GameTestSupport.universe(1);
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
        game.testFormUniverse();
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
        Game first = GameTestSupport.realGame();
        first.state().setLevel("generator", Game.REGIME_GENERATORS - 1);
        first.tick(0.01);
        assertFalse(first.isRegimeUnlocked());
    }

    @Test
    void laVitesseQuiTourneVraimentPorteLeRegimeEtLaSynergie() {
        // Version 0.123 : la production voit le régime et la synergie, pas seulement l'animation.
        Game game = second();
        assertEquals(game.speed().toDouble(), game.runningSpeed().toDouble(), 1e-12, "sans régime ni synergie, la vitesse commune");
        game.state().setLevel("generator", Game.SYNERGY_GENERATORS - 1);
        game.tick(0.01);
        assertTrue(game.isSynergyUnlocked() && game.isRegimeUnlocked());
        assertTrue(game.startRegime());
        double expected = game.speed().toDouble() * (1 + Game.REGIME_GAIN) * (1 + Game.SYNERGY_MAX);
        assertEquals(expected, game.runningSpeed().toDouble(), expected * 1e-9, "vitesse × régime × synergie");
        assertEquals(1.15575, game.runningSpeed().toDouble() / game.speed().toDouble(), 1e-9, "+15 % de régime et +0,5 % de synergie : ×1,15575");
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
    void laGraineGermeDesLUniversFormePuisSArrose() {
        Game game = GameTestSupport.universe(1);
        assertTrue(!game.state().universeSeeds().lt(BigNum.ONE), "former l'univers donne une graine");
        assertFalse(game.isLineThresholdReached());
        // Version 0.139 : le seuil de la dimension 1 ne la retient plus.
        // Il n'y a qu'une graine, celle de l'univers : le bouton ne demande pas de graine en réserve (0.105).
        game.state().setUniverseSeeds(BigNum.ZERO);
        assertTrue(game.canTendSeed(), "l'univers formé suffit");
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
    void laBrindilleOuvreLeDeuxiemeUnivers() {
        Game game = GameTestSupport.universe(1);
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        assertFalse(game.canOpenNextUniverse(), "pas avant la brindille");
        assertFalse(game.twigOpensUniverse());
        assertEquals(2, game.nextUniverseStage(), "l'univers 2 attend le palier 2");
        assertTrue(game.tendSeed());
        for (long k = 1; k < Game.SEED_TWIG_WATERINGS; k++) assertTrue(game.tendSeed());
        assertFalse(game.isSeedTwig(), "encore un germe à " + (Game.SEED_TWIG_WATERINGS - 1) + " arrosages");
        assertTrue(game.tendSeed());
        assertTrue(game.isSeedTwig(), "la brindille au trentième arrosage");
        assertTrue(game.canOpenNextUniverse());
        assertTrue(game.twigOpensUniverse(), "la brindille est là, l'univers formé, aucune partie en cours : elle ouvre");
        // Ce qui aurait été joué entre-temps (anciens onglets montrés, partie d'avant) ne passe pas dans l'univers 2.
        game.state().setParticles(BigNum.of(1, 50));
        game.state().setAtoms(BigNum.of(1, 9));
        GameTestSupport.bangs(game, 3);
        game.state().setDarkMatter(BigNum.of(500));
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
        assertFalse(game.twigOpensUniverse(), "la brindille a déjà ouvert le sien");
        assertEquals(3, game.nextUniverseStage(), "le suivant attendra le palier 3");
    }

    @Test
    void lesDefisDeBigBangDuDeuxiemeUniversAccelerentLeTroisiemeActe() {
        // Version 0.116 : ×2 particules, ×4 maturation, par défi ; 0.118 : sondes ×2 et ×2, plus d'espace.
        Game first = GameTestSupport.realGame();
        first.state().addCompletedBangChallenge(idle.core.bigbang.BangChallenge.VOID.name());
        assertEquals(1, first.universeBangFactor(2), 0, "rien dans le premier univers");
        Game game = second();
        GameTestSupport.bangs(game, 5);
        var rubble = idle.core.sky.Bodies.DEFAULT.get(0);
        double slow = game.maturationSeconds(rubble);
        var trip = idle.core.probes.ProbeTrip.values()[0];
        double trip0 = game.probeDuration(trip), value0 = game.probeValue(trip);
        BigNum particles0 = game.particlesPerCreation();
        game.state().addCompletedBangChallenge(idle.core.bigbang.BangChallenge.VOID.name());
        game.state().addCompletedBangChallenge(idle.core.bigbang.BangChallenge.FORGOTTEN.name());
        assertEquals(4, game.universeBangFactor(Game.BANG_UNIVERSE_PARTICLES), 0, "deux défis : ×2 ×2");
        assertEquals(slow / 16, game.maturationSeconds(rubble), 1e-9);
        assertEquals(trip0 / 4, game.probeDuration(trip), 1e-9);
        assertEquals(value0 * 4, game.probeValue(trip), 1e-9);
        assertEquals(particles0.toDouble() * 4, game.particlesPerCreation().toDouble(), particles0.toDouble() * 1e-9);
        assertFalse(game.planetsTwice());
        game.state().addCompletedBangChallenge(idle.core.bigbang.BangChallenge.BY_HAND.name());
        assertTrue(game.planetsTwice(), "les trois : planètes deux fois");
    }

    @Test
    void laFuiteSOuvreAuxDixDefisEtDonneLeCalmeDuBigBang() {
        // Version 0.115 : un onzième défi, du deuxième univers, qui apparaît une fois les dix autres réussis.
        Game game = second();
        game.state().setExplosions(1);
        game.state().setDarkMatter(BigNum.of(1000));
        String leak = idle.core.darkmatter.Challenges.LEAK.id();
        assertFalse(game.isLeakChallengeShown(), "pas avant les dix défis");
        assertEquals(10, game.challenges().size());
        for (var challenge : idle.core.darkmatter.Challenges.DEFAULT) game.state().addCompletedChallenge(challenge.id());
        assertTrue(game.isLeakChallengeShown(), "elle apparaît aux dix défis");
        assertEquals(11, game.challenges().size());
        assertTrue(game.isBigBangConditionMet(idle.core.bigbang.BigBangCondition.CHALLENGES), "le Big Bang ne la demande pas");
        assertTrue(game.isChallengeUnlocked(leak));
        assertTrue(game.startChallenge(leak));
        // La fuite accélère tant qu'on n'achète rien ; une amélioration du joueur l'arrête, celle d'un automatisme non.
        game.state().setUpgradeCalm(100);
        double fast = game.leakLossPerSecond();
        assertEquals(Game.LEAK_STEP * 100, fast, 1e-12);
        game.state().setUpgradeCalm(200);
        assertTrue(game.leakLossPerSecond() > fast, "de plus en plus vite");
        game.state().setUpgradeCalm(1e6);
        assertEquals(Game.LEAK_MAX, game.leakLossPerSecond(), 1e-12, "plafonnée");
        game.state().setUpgradeCalm(100);
        game.state().setParticles(BigNum.of(1, 6));
        double before = game.state().particles().toDouble();
        game.tick(1);
        assertTrue(game.state().particles().toDouble() < before, "les particules fuient");
        game.state().setParticles(BigNum.of(1, 9));
        assertTrue(game.buy("generator") || game.buy("speed"));
        assertEquals(0, game.state().upgradeCalm(), 0, "une amélioration du joueur remet la fuite à zéro");
        game.state().setUpgradeCalm(50);
        game.state().setParticles(BigNum.of(1, 12));
        game.automating = true;
        assertTrue(game.buy("generator") || game.buy("speed"));
        game.automating = false;
        assertEquals(50, game.state().upgradeCalm(), 0, "l'achat d'un automatisme ne compte pas");
        // Pas d'explosion avant deux minutes de défi.
        GameTestSupport.fillTable(game, 1);
        assertFalse(game.canExplode(), "moins de deux minutes : pas d'explosion");
        game.stats().addTime(Game.LEAK_MIN_SECONDS);
        assertTrue(game.canExplode());
        assertEquals(0, game.bigBangCalmRelief(), 0, "pas de calme avant de l'avoir réussie");
        assertTrue(game.explode());
        assertTrue(game.isChallengeCompleted(leak));
        assertEquals(11, game.completedChallenges());
        game.state().setBangCalm(1800);
        assertEquals(0.5, game.bigBangCalmRelief(), 1e-12, "une demi-heure : la moitié des ordres de grandeur");
        assertEquals(game.bigBangs() == 0 ? Game.BIG_BANG_PARTICLES.log10() / 2 : Game.BIG_BANG_PARTICLES_LATER.log10() / 2,
                game.bigBangParticles().log10(), 1e-6);
        game.state().setBangCalm(Game.BIG_BANG_CALM_SECONDS);
        assertEquals(1, game.bigBangParticles().toDouble(), 0, "une heure : une seule particule");
        // Version 0.124 : les achats des automatismes rompent aussi le calme, et la barre haute l'ignore.
        game.state().setParticles(BigNum.of(1, 100));
        game.state().setAutomationEnabled("auto_speed", true);
        game.state().addAutomation("auto_speed");
        game.tick(20);
        assertTrue(game.state().bangCalm() < 20, "un achat d'automatisme rompt le calme (" + game.state().bangCalm() + ")");
        game.state().setBangCalm(Game.BIG_BANG_CALM_SECONDS);
        game.state().setActiveBangChallenge(idle.core.bigbang.BangChallenge.RAISED.name());
        assertEquals(0, game.bigBangCalmRelief(), 0, "pas de calme pendant la barre haute");
    }

    @Test
    void uneVieilleSauvegardeDontLUniversEstFormeNeCompteQuUnUnivers() {
        // Version 0.113 : avant la 0.083, l'univers formé gardait son échelle du cosmos ; le chargement en comptait deux.
        Game game = GameTestSupport.realGame();
        game.state().setUniverseFormed(true);
        game.state().setCosmosLevel(Cosmos.values().length);
        game.restored();
        assertEquals(1, game.universes());
        assertEquals(2, game.universeDarkMatterFactor(), 0);
    }

    @Test
    void chaqueUniversFormeDoubleLaMatiereNoireEtSaCroissance() {
        // Version 0.112 : ×2 par univers formé, multiplicatif, sur la matière noire des explosions et sa croissance.
        Game before = GameTestSupport.realGame();
        assertEquals(1, before.universeDarkMatterFactor(), 0);
        BigNum base = before.darkMatterPerExplosion();
        Game game = GameTestSupport.universe(1);
        assertEquals(2, game.universeDarkMatterFactor(), 0, "un univers formé : ×2");
        assertEquals(base.toDouble() * 2, game.darkMatterPerExplosion().toDouble(), 1e-9);
        game.state().setDarkMatter(BigNum.of(10));
        game.state().setExplosions(1);
        double momentum = game.darkMatterMomentum();
        game.testAddUniverse();
        assertEquals(4, game.universeDarkMatterFactor(), 0, "deux univers : ×4");
        assertEquals(momentum * 2, game.darkMatterMomentum(), momentum * 1e-9, "la croissance double aussi");
        assertTrue(game.startNextUniverse());
        assertEquals(4, game.universeDarkMatterFactor(), 0, "le bonus reste pendant la partie suivante");
    }

    @Test
    void laBrindilleOuvreUnUniversAtteintParLEchelleSeule() {
        // Version 0.110 : l'échelle du cosmos montée sans former l'univers bloquait la brindille.
        Game game = GameTestSupport.realGame();
        game.state().setCosmosLevel(Cosmos.values().length);
        game.state().setParticles(BigNum.of(1, 20));
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(44);
        assertFalse(game.state().universeFormed());
        assertTrue(game.openUniverseFromTwig(), "la brindille ouvre l'univers 2");
        assertTrue(game.isReplaying());
        assertTrue(game.isSecondUniverse());
        assertEquals(0, game.state().particles().sign(), "les actes repartent de zéro");
    }

    @Test
    void lesPaliersDeLaGraineOuvrentLesUnivers() {
        // Version 0.120 : le palier N de la graine ouvre l'univers N. Version 0.139 : trente arrosages par palier suffisent,
        // sans les seuils des dimensions.
        Game game = GameTestSupport.universe(1);
        assertEquals(1, game.seedStage());
        assertTrue(game.tendSeed(), "elle germe");
        for (long k = 0; k < Game.SEED_TWIG_WATERINGS - 1; k++) assertTrue(game.tendSeed());
        assertEquals(1, game.seedStage(), "29 arrosages : pas encore");
        assertTrue(game.tendSeed());
        assertEquals(2, game.seedStage(), "la tige");
        assertTrue(game.canOpenNextUniverse(), "la tige ouvre l'univers 2");
        assertTrue(game.startNextUniverse());
        game.testFormUniverse();
        assertEquals(2, game.universes());
        assertFalse(game.canOpenNextUniverse(), "l'univers 3 attend le palier 3");
        assertFalse(game.isPlaneThresholdReached(), "le plan n'a pas son seuil : la graine ne l'attend plus");
        for (long k = 1; k < Game.SEED_TWIG_WATERINGS; k++) assertTrue(game.tendSeed());
        assertEquals(2, game.seedStage(), "29 arrosages de plus : pas encore");
        assertTrue(game.tendSeed());
        assertEquals(3, game.seedStage(), "trente de plus : la graine craquelée");
        assertEquals("la graine craquelée", Game.SEED_STAGE_NAMES.get(2));
        assertTrue(game.canOpenNextUniverse(), "le palier 3 ouvre l'univers 3");
        assertTrue(game.tendSeed(), "la dimension 3 n'existe pas encore, la graine boit quand même");
        assertEquals(11, Game.SEED_STAGE_NAMES.size());
        assertEquals(300, Game.seedStageWaterings(11));
    }

    @Test
    void chaquePalierDeLaGraineJusquAuOnziemeOuvreSonUnivers() {
        // Du palier 4 à l'arbre-monde (palier 11) : chaque palier N ouvre l'univers N, et seulement lui.
        Game game = GameTestSupport.universe(1);
        game.state().setSeedSprouted(true);
        for (int stage = 2; stage <= Game.SEED_STAGES; stage++) {
            game.state().setSeedWaterings(Game.seedStageWaterings(stage) - 1);
            assertEquals(stage - 1, game.seedStage(), "un arrosage de moins : le palier d'avant");
            assertFalse(game.canOpenNextUniverse(), "l'univers " + stage + " attend le palier " + stage);
            assertEquals(stage, game.nextUniverseStage());
            game.state().setSeedWaterings(Game.seedStageWaterings(stage));
            assertEquals(stage, game.seedStage());
            assertEquals(Game.SEED_STAGE_NAMES.get(stage - 1), Game.SEED_STAGE_NAMES.get(game.seedStage() - 1));
            assertTrue(game.canOpenNextUniverse(), "le palier " + stage + " ouvre l'univers " + stage);
            assertTrue(game.startNextUniverse());
            assertFalse(game.canOpenNextUniverse(), "la partie en cours ne rouvre rien");
            game.testFormUniverse();
            assertEquals(stage, game.universes(), "l'univers " + stage + " est formé");
            assertEquals(stage - 1, game.state().seedUniversesOpened());
        }
        assertEquals(11, game.universes());
        assertEquals(300, game.state().seedWaterings(), "trente arrosages par palier : 300 pour l'arbre-monde");
        assertEquals(Game.SEED_STAGES, game.nextUniverseStage(), "au-delà de l'arbre-monde, il n'y a plus de palier à attendre");
        assertFalse(game.canTendSeed(), "l'arbre-monde ne boit plus");
    }

    @Test
    void lePetitArbreDuCycleSeChoisitUneFoisEtSeRendAuBigBang() {
        Game game = second();
        assertFalse(game.isCycleChoiceUnlocked(), "avant le premier Big Bang");
        GameTestSupport.bangs(game, 1);
        assertTrue(game.isCycleChoiceUnlocked());
        BigNum atoms = game.atomsPerFusion();
        assertTrue(game.chooseCycle(CycleChoice.FUSION));
        assertFalse(game.chooseCycle(CycleChoice.RESERVE), "un seul choix par cycle");
        assertEquals(atoms.toDouble() * (1 + CycleChoice.FUSION_GAIN), game.atomsPerFusion().toDouble(), atoms.toDouble() * 1e-9);
        GameTestSupport.readyForBigBang(game);
        assertTrue(game.bigBang());
        assertNull(game.cycleChoice(), "le Big Bang le rend");
        GameTestSupport.payMilestones(game);
        assertTrue(game.chooseCycle(CycleChoice.RESERVE));
        BigNum dark = game.darkMatterPerExplosion();
        game.state().setCycleChoice(null);
        assertEquals(dark.toDouble() / (1 + CycleChoice.RESERVE_GAIN), game.darkMatterPerExplosion().toDouble(), 1e-9);
    }

    @Test
    void laPreferenceDeFamilleMonteAvecLesTiragesEtLesElementsGratuits() {
        Game game = second();
        assertFalse(game.isFamilyPreferenceUnlocked(), "sans la synthèse ciblée, pas de préférence");
        game.state().setDarkLevel("dark_shop_target", 1);
        assertTrue(game.isTargetedSynthesisUnlocked(), "la case « Synthèse ciblée » de l'arbre");
        assertTrue(game.isFamilyPreferenceUnlocked());
        double chance = game.categoryChance(ElementCategory.NOBLE_GAS);
        assertTrue(game.setPreferredFamily(ElementCategory.NOBLE_GAS));
        assertEquals(ElementCategory.NOBLE_GAS, game.preferredFamily());
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
        Game first = GameTestSupport.realGame();
        for (int n = 1; n <= 20; n++) first.state().setElementCount(n, 1);
        assertEquals(0, first.freeElementsAvailable(), "pas avant le deuxième univers");
        assertEquals(PeriodicTable.ELEMENTS.size(), 118);
    }
}
