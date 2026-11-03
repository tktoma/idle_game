package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import org.junit.jupiter.api.Test;

/**
 * Version 0.169 : une bulle qui garde, achetée entre la formation de l'univers et le lancement du suivant, garde encore
 * sa part de l'univers formé ; le vide raccourcit le rassemblement et la maturation de moitié au plus ; le jalon des
 * racines, en gluons, abaisse les paliers du défi final.
 */
class Reglages169Test {

    /** Une partie au premier univers en cours, le collisionneur bien avancé. */
    private static Game avecCollisionneur() {
        Game game = GameTestSupport.realGame();
        game.state().setColliderEnergyProduced(5e20);
        return game;
    }

    @Test
    void uneBulleAcheteeAvantLeLancementGardeSaPartDeLUniversForme() {
        Game game = avecCollisionneur();
        game.testFormUniverse();
        assertEquals(0, game.state().colliderEnergyProduced(), 1e-9, "la formation remet le collisionneur à zéro");
        assertTrue(game.state().isHoldingForBubbles(), "ce que les bulles peuvent garder attend le lancement");
        game.state().setTreeBubbleLevel(TreeBubble.MATURE_LEFT.ordinal(), 1);
        game.state().setColliderEnergyProduced(12);
        game.state().startUniverseKeeping();
        assertEquals(5e20, game.state().colliderEnergyProduced(), 1e5, "l'arbre mûr, acheté avant de lancer, rend le collisionneur formé");
        assertFalse(game.state().isHoldingForBubbles());
    }

    @Test
    void sansLaBulleRienNeRevient() {
        Game game = avecCollisionneur();
        game.testFormUniverse();
        game.state().startUniverseKeeping();
        assertEquals(0, game.state().colliderEnergyProduced(), 1e-9);
    }

    @Test
    void uneBulleDejaAcheteeGardeCeQuiSEstAjouteDepuisLaFormation() {
        Game game = avecCollisionneur();
        game.state().setTreeBubbleLevel(TreeBubble.MATURE_LEFT.ordinal(), 1);
        game.testFormUniverse();
        assertEquals(5e20, game.state().colliderEnergyProduced(), 1e5, "gardé dès la formation");
        game.state().setColliderEnergyProduced(7e20);
        game.state().startUniverseKeeping();
        assertEquals(7e20, game.state().colliderEnergyProduced(), 1e5, "pas de retour en arrière au lancement");
    }

    @Test
    void ceQuiAttendSeSauvegarde() throws Exception {
        Game game = avecCollisionneur();
        game.testFormUniverse();
        GameState back = GameTestSupport.reread(game.state());
        assertTrue(back.isHoldingForBubbles(), "l'attente traverse la sauvegarde");
        back.setTreeBubbleLevel(TreeBubble.MATURE_LEFT.ordinal(), 1);
        back.startUniverseKeeping();
        assertEquals(5e20, back.colliderEnergyProduced(), 1e5);
    }

    @Test
    void leVideRaccourcitDeMoitieAuPlus() {
        Game game = GameTestSupport.realGame();
        for (int k = 0; k < 20; k++) {
            game.state().setVoidLevel(Game.VoidUpgrade.SETTLE.ordinal(), k);
            game.state().setVoidLevel(Game.VoidUpgrade.MATURE.ordinal(), k);
            assertTrue(game.settleShare() >= 0.5 - 1e-12 && game.maturationShare() >= 0.5 - 1e-12, "niveau " + k);
        }
        game.state().setVoidLevel(Game.VoidUpgrade.SETTLE.ordinal(), Game.VoidUpgrade.SETTLE.max());
        game.state().setVoidLevel(Game.VoidUpgrade.MATURE.ordinal(), Game.VoidUpgrade.MATURE.max());
        assertEquals(0.5, game.settleShare(), 1e-12, "Assise : 5 niveaux de 10 %");
        assertEquals(0.5, game.maturationShare(), 1e-12, "Maturation : 10 niveaux de 5 %");
    }

    /** Le vide de l'univers formé, rendu au lancement par la bulle achetée depuis : déjà au cumul, il n'y revient pas. */
    @Test
    void leVideRenduAuLancementNeCompteQuUneFoisAuCumul() {
        Game game = GameTestSupport.universe(3);
        game.state().setVoidOpen(true);
        game.state().setVoidEnergy(idle.core.math.BigNum.of(5_000));
        double before = game.voidAccumulated();
        game.testFormUniverse();
        assertEquals(before + 5_000, game.voidAccumulated(), 1e-6, "la formation compte le stock");
        game.state().setTreeBubbleLevel(TreeBubble.GIANT_LEFT_LOW.ordinal(), 1);
        assertTrue(game.startNextUniverse());
        assertEquals(5_000, game.voidEnergy().toDouble(), 1e-6, "la bulle rend l'énergie de l'univers formé");
        game.state().setVoidEnergy(idle.core.math.BigNum.of(6_000));
        game.testFormUniverse();
        assertEquals(before + 6_000, game.voidAccumulated(), 1e-6, "seuls les 1 000 gagnés depuis s'ajoutent");
    }

    /** Le lancement dit ce que la bulle achetée depuis la formation a gardé. */
    @Test
    void leLancementDitCeQueLaBulleAGarde() {
        Game game = avecCollisionneur();
        game.testFormUniverse();
        game.state().setTreeBubbleLevel(TreeBubble.MATURE_LEFT.ordinal(), 1);
        assertTrue(game.universeTransition(true).kept().stream().anyMatch(line -> line.startsWith("Le collisionneur")));
    }

    /** Une partie avec la bulle des racines et {@code protons} protons liés (trois gluons chacun). */
    private static Game racines(long protons, boolean bulle) {
        Game game = GameTestSupport.realGame();
        if (bulle) game.state().setTreeBubbleLevel(TreeBubble.ROOTS.ordinal(), 1);
        game.state().setHadrons(idle.core.collider.Hadron.PROTON, protons);
        return game;
    }

    @Test
    void leJalonDesRacinesCompteDixPaliersDeGluons() {
        assertEquals(10, Game.ROOT_GLUON_TIERS.length);
        for (int i = 1; i < Game.ROOT_GLUON_TIERS.length; i++) {
            assertTrue(Game.ROOT_GLUON_TIERS[i] > Game.ROOT_GLUON_TIERS[i - 1], "des paliers croissants");
        }
        // Les huit premiers à +10 % au plus l'un de l'autre ; les deux derniers, bien plus loin.
        for (int i = 1; i < 8; i++) assertTrue(Game.ROOT_GLUON_TIERS[i] <= 1.12 * Game.ROOT_GLUON_TIERS[i - 1], "palier " + (i + 1));
        assertTrue(Game.ROOT_GLUON_TIERS[8] >= 1.5 * Game.ROOT_GLUON_TIERS[7], "le neuvième est long à atteindre");
        assertTrue(Game.ROOT_GLUON_TIERS[9] >= 1.5 * Game.ROOT_GLUON_TIERS[8], "le dixième encore plus");
        assertEquals(0, racines(1_000_000, false).rootTiersReached(), "rien sans la bulle des racines");
        assertEquals(0, racines(26_000, true).rootTiersReached(), "78 000 gluons : pas encore");
        assertEquals(1, racines(27_000, true).rootTiersReached(), "81 000 gluons : le premier");
        assertEquals(8, racines(60_000, true).rootTiersReached(), "180 000 gluons : huit");
        assertEquals(10, racines(140_000, true).rootTiersReached(), "420 000 gluons : tous");
    }

    @Test
    void leJalonDesRacinesDiviseLesPaliersDuDefiFinalParDeuxAuPlus() {
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            assertEquals(Game.finalGoal(d), racines(0, true).finalGoalFor(d), 1e-9 * Game.finalGoal(d), "aucun palier : le palier entier");
            assertEquals(Game.finalGoal(d) / 2, racines(140_000, true).finalGoalFor(d), 1e-9 * Game.finalGoal(d), "dix paliers : la moitié");
        }
        Game game = racines(27_000, true);
        assertEquals(Game.finalGoal(3) * Math.pow(2, -0.1), game.finalGoalFor(3), 1e-6 * Game.finalGoal(3), "un palier : −7 %");
        // Le palier abaissé compte pour le défi final.
        game.state().setVolumeShardsEarned(Game.finalGoal(3) * 0.95);
        assertTrue(game.isFinalTierReached(3), "95 % du palier entier suffisent avec un palier du jalon");
        assertFalse(racines(0, true).isFinalTierReached(3));
    }

    /** Les prix du vide : le premier niveau près des premières bulles, le dernier près des dernières. */
    @Test
    void lesPrixDuVideSuiventCeuxDesBulles() {
        for (Game.VoidUpgrade upgrade : Game.VoidUpgrade.values()) {
            double first = upgrade.baseCost();
            double last = upgrade.baseCost() * Math.pow(upgrade.growth(), upgrade.max() - 1);
            assertTrue(first >= 500 && first <= 5_200, upgrade + " : premier niveau " + first);
            assertTrue(last >= 69_300 && last <= 835_938 * 1.001, upgrade + " : dernier niveau " + last);
        }
        assertEquals(5, Game.VoidUpgrade.SETTLE.max(), "5 × 10 % = −50 %");
        assertEquals(10, Game.VoidUpgrade.MATURE.max(), "10 × 5 % = −50 %");
    }

    /** Les statistiques disent ce que les hadrons font au reste du jeu : les protons, aux atomes par fusion. */
    @Test
    void lImpactDesHadronsSeLitDansLesMultiplicateurs() {
        Game game = GameTestSupport.realGame();
        game.state().setStarted(true);
        game.testAddFusions(1);
        assertTrue(Multipliers.impactsOf(game, Multipliers.Aspect.HADRONS).isEmpty(), "sans hadron, rien");
        game.state().setHadrons(idle.core.collider.Hadron.PROTON, 99);
        java.util.List<Multipliers.Impact> impacts = Multipliers.impactsOf(game, Multipliers.Aspect.HADRONS);
        assertEquals(1, impacts.size(), impacts.toString());
        assertEquals(Multipliers.Quantity.ATOMS, impacts.get(0).quantity());
        assertEquals(Math.log10(game.hadronBoost(idle.core.collider.Hadron.PROTON)), impacts.get(0).log10(), 1e-9);
        assertTrue(impacts.get(0).share() > 0 && impacts.get(0).share() <= 1);
    }

    /** Ce qu'un aspect fait à ses propres grandeurs ne compte pas comme un effet sur le reste. */
    @Test
    void leCollisionneurNeSeComptePasLuiMeme() {
        Game game = GameTestSupport.universe(3);
        game.testOpenVoid();
        for (Multipliers.Impact impact : Multipliers.impactsOf(game, Multipliers.Aspect.COLLIDER, Multipliers.Quantity.COLLIDER_ENERGY)) {
            assertTrue(impact.quantity() != Multipliers.Quantity.COLLIDER_ENERGY);
        }
    }

    /** Ce que le vide prend au collisionneur : jamais plus que la part réglée, rien fermé. */
    @Test
    void leVidePrendAuPlusSaPartDuCollisionneur() {
        Game game = GameTestSupport.universe(3);
        assertEquals(0, game.voidColliderShare(), 1e-12, "fermé");
        assertEquals(0, game.voidSpaceShare(), 1e-12);
        game.testOpenVoid();
        game.setVoidShare(0.25);
        assertTrue(game.voidColliderShare() <= 0.25 + 1e-9, "au plus la part réglée : " + game.voidColliderShare());
        assertTrue(game.voidSpaceShare() >= 0 && game.voidSpaceShare() <= 1);
    }
}
