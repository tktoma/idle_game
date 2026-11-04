package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.VoidUpgrade;
import idle.core.bigbang.BangChallenge;
import idle.core.math.BigNum;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import org.junit.jupiter.api.Test;

/**
 * Version 0.171 : la maturation des défis de Big Bang du deuxième univers ×4 au lieu de ×5 (racine cubique de 2 par
 * défi), et les prix du vide étalés sur toute la partie : le premier niveau dix fois moins cher, le dernier trente fois
 * plus.
 */
class Reglages171Test {

    /** Le plus long des astres à mûrir, le trou noir supermassif. */
    private static Body supermassif(Game game) {
        Body longest = null;
        for (Body b : Bodies.DEFAULT) if (longest == null || game.maturationSeconds(b) > game.maturationSeconds(longest)) longest = b;
        return longest;
    }

    @Test
    void lesTroisDefisEtLaBarreHauteFontMurirQuatreFoisPlusVite() {
        Game game = DeuxiemeUniversTest.second();
        Body body = supermassif(game);
        assertEquals(6 * 3600, game.maturationSeconds(body), 1e-6, "sans défi : 6 h");
        for (BangChallenge c : new BangChallenge[] {BangChallenge.VOID, BangChallenge.FORGOTTEN, BangChallenge.RAISED}) {
            game.state().addCompletedBangChallenge(c.name());
        }
        assertEquals(2, game.universeBangFactor(Game.BANG_UNIVERSE_MATURATION), 1e-12, "trois défis : ×2 pour l'univers");
        assertEquals(90 * 60, game.maturationSeconds(body), 1e-6, "et ×2 de la barre haute : ×4, 90 minutes (72 en 0.166)");
    }

    @Test
    void sansLaBarreHauteTroisDefisDonnentDeux() {
        Game game = DeuxiemeUniversTest.second();
        Body body = supermassif(game);
        for (BangChallenge c : new BangChallenge[] {BangChallenge.VOID, BangChallenge.FORGOTTEN, BangChallenge.BY_HAND}) {
            game.state().addCompletedBangChallenge(c.name());
        }
        assertEquals(3 * 3600, game.maturationSeconds(body), 1e-6, "×2 : 3 h");
    }

    @Test
    void chaqueDefiCompteLaRacineCubiqueDeDeux() {
        assertEquals(Math.cbrt(2), Game.BANG_UNIVERSE_MATURATION, 0);
        Game game = DeuxiemeUniversTest.second();
        Body body = supermassif(game);
        double avant = game.maturationSeconds(body);
        game.state().addCompletedBangChallenge(BangChallenge.VOID.name());
        assertEquals(avant / Math.cbrt(2), game.maturationSeconds(body), 1e-6, "un défi : ×1,26");
        game.state().addCompletedBangChallenge(BangChallenge.FORGOTTEN.name());
        assertEquals(avant / Math.cbrt(4), game.maturationSeconds(body), 1e-6, "deux défis : ×1,59");
    }

    @Test
    void lePremierUniversNeChangePas() {
        Game first = GameTestSupport.realGame();
        Body body = supermassif(first);
        double avant = first.maturationSeconds(body);
        first.state().addCompletedBangChallenge(BangChallenge.VOID.name());
        first.state().addCompletedBangChallenge(BangChallenge.FORGOTTEN.name());
        assertEquals(1, first.universeBangFactor(Game.BANG_UNIVERSE_MATURATION), 0, "aucun bonus d'univers avant le deuxième");
        assertEquals(avant, first.maturationSeconds(body), 1e-9, "la maturation du premier univers ne bouge pas");
    }

    @Test
    void lesPrixDuVideVontDuPremierAuDernierNiveau() {
        double[][] attendus = {
                {50, 12_000_000}, {80, 15_000_000}, {60, 9_000_000}, {60, 9_000_000}, {100, 18_000_000},
                {200, 6_000_000}, {150, 4_500_000}, {500, 2_100_000}};
        for (VoidUpgrade upgrade : VoidUpgrade.values()) {
            double[] prix = attendus[upgrade.ordinal()];
            assertEquals(prix[0], upgrade.cost(0), 1e-9, upgrade + " : premier niveau");
            assertEquals(prix[1], upgrade.cost(upgrade.max() - 1), prix[1] * 1e-12, upgrade + " : dernier niveau");
            assertEquals(Double.POSITIVE_INFINITY, upgrade.cost(upgrade.max()), upgrade + " : rien au-delà du plafond");
            for (int level = 1; level < upgrade.max(); level++) {
                assertTrue(upgrade.cost(level) > upgrade.cost(level - 1), upgrade + " : chaque niveau coûte plus que le précédent");
            }
        }
    }

    /** Garde-fou : le premier niveau à la portée d'un seul univers, le dernier à celle de l'équilibre de fin de partie. */
    @Test
    void lesPrixDuVideRestentDansLeursFourchettes() {
        double total = 0;
        for (VoidUpgrade upgrade : VoidUpgrade.values()) {
            assertTrue(upgrade.cost(0) <= 500, upgrade + " : moins que les 1 800 d'énergie que le Régulier gagnait par univers en 0.169");
            double last = upgrade.cost(upgrade.max() - 1);
            assertTrue(last >= 2_000_000 && last <= 20_000_000, upgrade + " : dernier niveau " + last);
            assertTrue(upgrade.growth() >= 1.8 && upgrade.growth() <= 8.1, upgrade + " : ×" + upgrade.growth() + " par niveau");
            for (int level = 0; level < upgrade.max(); level++) total += upgrade.cost(level);
        }
        // 1,3 × 10⁸ en tout : sous le plafond de l'équilibre (5,4 × 10⁷) multiplié par les quelques univers où il est atteint.
        assertTrue(total > 1.2e8 && total < 1.4e8, "tous les niveaux : " + total);
    }

    @Test
    void uneSauvegardeDAvantGardeSesNiveauxEtPaieLesNouveauxPrix() {
        Game game = VideTest.open();
        game.state().setVoidLevel(VoidUpgrade.GAIN.ordinal(), 7);
        game.state().setVoidLevel(VoidUpgrade.SETTLE.ordinal(), 5);
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(7, back.voidLevel(VoidUpgrade.GAIN.ordinal()), "les niveaux achetés restent : rien de nouveau dans la sauvegarde");
        assertEquals(5, back.voidLevel(VoidUpgrade.SETTLE.ordinal()));
        assertEquals(VoidUpgrade.GAIN.cost(7), game.voidCost(VoidUpgrade.GAIN), 1e-9, "le niveau suivant au nouveau prix");
        assertEquals(50 * Math.pow(VoidUpgrade.GAIN.growth(), 7), game.voidCost(VoidUpgrade.GAIN), 1e-6);
        game.state().setVoidEnergy(BigNum.of(1e9));
        assertFalse(game.canBuyVoid(VoidUpgrade.SETTLE), "au plafond");
    }

    @Test
    void lAchatDuPremierNiveauSeFaitAvecPeuDEnergie() {
        Game game = VideTest.open();
        game.state().setVoidEnergy(BigNum.of(500));
        assertTrue(game.buyVoid(VoidUpgrade.SETTLE), "le rassemblement plus court dès 500");
        assertEquals(0.9, game.settleShare(), 1e-12);
        assertEquals(0, game.voidEnergy().toDouble(), 1e-9);
    }
}
