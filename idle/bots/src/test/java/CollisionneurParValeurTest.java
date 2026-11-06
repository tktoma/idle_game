import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.bigbang.BangChallenge;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.174 : le collisionneur s'achète par valeur dès le deuxième univers, et les défis de Big Bang laissent la barre
 * haute de côté une fois la maturation supprimée par l'arbre ancien.
 */
class CollisionneurParValeurTest {

    /** Le troisième univers, juste après son premier Big Bang : le collisionneur repart de zéro. */
    private static Game thirdUniverse() {
        Game game = Bots.fresh(42);
        game.testFormUniverse();
        game.startNextUniverse();
        game.testFormUniverse();
        game.startNextUniverse();
        game.state().setBigBangs(1);
        return game;
    }

    /** L'ordre d'achat de la 0.173, recopié : le premier générateur achetable de la liste, puis les améliorations. */
    private static void order0173(RobotCosmosPlanner planner) {
        Game game = planner.g;
        for (int i = 0; i < 70; i++) {
            boolean bought = false;
            for (ColliderGenerator gen : ColliderGenerator.values()) {
                if (game.canBuyColliderGenerator(gen)) { game.buyColliderGenerator(gen); bought = true; break; }
            }
            if (!bought) break;
        }
        planner.buyColliderUpgrades(ColliderUpgrade.values());
    }

    /** L'énergie produite en deux heures de jeu, des achats toutes les 20 secondes. */
    private static double twoHours(boolean byValue) {
        Game game = thirdUniverse();
        RobotCosmosPlanner planner = new RobotCosmosPlanner(game, 20);
        assertTrue(game.isColliderUnlocked());
        for (int s = 0; s < 2 * 3600; s += 20) {
            if (byValue) planner.colliderByValue(); else order0173(planner);
            game.tick(20);
        }
        return game.colliderEnergyProduced();
    }

    @Test
    void lAchatParValeurProduitPlusQueLAncienOrdreEtTientSonRythme() {
        double before = twoHours(false);
        double now = twoHours(true);
        // Mesuré : 1,00e10 avec l'ordre de la 0.173, 1,27e10 par valeur (+27 %).
        assertTrue(now > before * 1.15, "par valeur " + now + " contre " + before);
        // Garde de durée : en deux heures, de quoi former le trou noir (10¹²) n'est pas encore là, 10¹⁰ si.
        assertTrue(now > 1e10 && now < 1e12, "énergie produite en 2 h : " + now);
    }

    @Test
    void lesDoublementsPassentAvantLesGenerateurs() {
        Game game = thirdUniverse();
        RobotCosmosPlanner planner = new RobotCosmosPlanner(game, 20);
        game.state().setColliderEnergy(1e6);
        boolean energyFirst = game.canBuyColliderUpgrade(ColliderUpgrade.ENERGY);
        planner.colliderByValue();
        if (energyFirst) assertTrue(game.colliderUpgradeLevel(ColliderUpgrade.ENERGY) > 0, "la production d'énergie d'abord");
        // Un générateur ne s'achète qu'après celui d'en dessous : la chaîne monte d'un cran à chaque décision.
        for (int i = 0; i < 3; i++) {
            game.state().setColliderEnergy(1e6);
            planner.colliderByValue();
        }
        ColliderGenerator[] gens = ColliderGenerator.values();
        int highest = -1;
        for (int k = 0; k < gens.length; k++) if (game.state().colliderGeneratorsBought(gens[k]) > 0) highest = k;
        assertTrue(highest > 0, "au moins un générateur au-dessus du faisceau");
    }

    @Test
    void sansMaturationLaBarreHauteNePasseplusEnPremier() {
        RobotCosmosPlanner planner = new RobotCosmosPlanner(thirdUniverse(), 20);
        List<BangChallenge> order = planner.bangChallengeOrder(true);
        assertEquals(BangChallenge.RAISED, order.get(1), "avec la maturation, la barre haute ensuite");
        planner.g.state().setTreeBubbleLevel(TreeBubble.ANCIENT_RIGHT.ordinal(), 1);
        order = planner.bangChallengeOrder(true);
        assertEquals(List.of(BangChallenge.FROZEN, BangChallenge.VOID, BangChallenge.FORGOTTEN, BangChallenge.BY_HAND),
                order.subList(0, 4), "les trois défis d'origine juste après le ciel figé");
        assertFalse(order.subList(0, 4).contains(BangChallenge.RAISED));
        assertEquals(BangChallenge.values().length, order.size(), "tous, une fois chacun");
    }
}
