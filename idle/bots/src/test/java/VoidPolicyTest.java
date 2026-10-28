import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.VoidUpgrade;
import idle.core.math.BigNum;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Version 0.133 : les bots jouent le vide du troisième univers, chacun à sa façon, et rien avant. */
class VoidPolicyTest {

    private static Game fresh() {
        return Bots.fresh(42);
    }

    /** Le troisième univers, juste après son premier Big Bang : le vide se propose, et le collisionneur peut le payer. */
    private static Game thirdUniverse() {
        Game game = fresh();
        game.testFormUniverse();
        game.startNextUniverse();
        game.testFormUniverse();
        game.startNextUniverse();
        game.state().setBigBangs(1);
        game.state().setColliderEnergy(1e15);
        return game;
    }

    /** Un hasard qui refuse d'être tiré. */
    private static final class NoRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        protected int next(int bits) {
            throw new AssertionError("aucun hasard ne devait être tiré");
        }
    }

    @Test
    void rienAvantLeTroisiemeUnivers() {
        Game first = fresh();
        first.state().setBigBangs(1);
        first.state().setColliderEnergy(1e15);
        VoidPolicy policy = new VoidPolicy(IdleProfiles24.VOID_STYLES[15], new NoRandom());
        assertEquals(0, policy.play(first));
        assertFalse(policy.savingForVoid(first));
        Game second = fresh();
        second.testFormUniverse();
        second.startNextUniverse();
        second.state().setBigBangs(3);
        second.state().setColliderEnergy(1e15);
        assertEquals(0, policy.play(second), "dans l'univers 2, le vide ne se propose pas");
        assertFalse(second.isVoidUnlocked());
    }

    @Test
    void lExpertOuvreLeVideEtAcheteDAbordCeQuiRaccourcitLActe3() {
        Game game = thirdUniverse();
        assertTrue(game.isVoidOffered());
        VoidPolicy expert = new VoidPolicy(VoidPolicy.Style.EXPERT, new NoRandom());
        assertTrue(expert.savingForVoid(game), "le collisionneur se garde pour le vide");
        expert.play(game);
        assertTrue(game.isVoidUnlocked());
        assertEquals(0.5, game.state().voidShare(), 1e-12);
        game.state().setVoidEnergy(BigNum.of(1_000));
        expert.play(game);
        // 1 000 : maturation 200, allègement 300, maturation 400 ; il reste 100.
        assertEquals(2, game.voidLevel(VoidUpgrade.MATURE), "le raccourci le moins cher, à chaque achat");
        assertEquals(1, game.voidLevel(VoidUpgrade.LIGHTEN));
        assertEquals(0, game.voidLevel(VoidUpgrade.SETTLE), "800 : pas assez");
        int growth = 0;
        for (VoidUpgrade upgrade : VoidUpgrade.values()) if (!VoidPolicy.SHORTENERS.contains(upgrade)) growth += game.voidLevel(upgrade);
        assertTrue(growth >= 1, "une croissance bon marché à côté du prochain raccourci (600)");
        assertTrue(game.state().voidEnergy().toDouble() < 150, "plus rien à moins du quart de 600");
    }

    @Test
    void lExpertFaitGrandirLeVideQuandLeProchainRaccourciEstHorsDAtteinte() {
        Game game = thirdUniverse();
        game.testOpenVoid();
        game.setVoidShare(0.5);
        // Les trois raccourcis coûtent déjà 9 600 et plus ; l'équilibre du vide est loin en dessous.
        game.state().setVoidLevel(VoidUpgrade.MATURE.ordinal(), 6);
        game.state().setVoidLevel(VoidUpgrade.LIGHTEN.ordinal(), 5);
        game.state().setVoidLevel(VoidUpgrade.SETTLE.ordinal(), 4);
        game.state().setVoidEnergy(BigNum.of(4_000));
        assertTrue(game.voidEquilibrium() < 9_600);
        VoidPolicy expert = new VoidPolicy(VoidPolicy.Style.EXPERT, new NoRandom());
        expert.buy(game);
        assertTrue(expert.bought > 0, "sans croissance, le prochain raccourci ne viendrait jamais");
    }

    @Test
    void leCollisionneurAttendLOuvertureDuVide() {
        Game game = thirdUniverse();
        RobotCosmosPlanner bot = new RobotCosmosPlanner(game, 20);
        double before = game.colliderEnergy();
        bot.collider();
        assertEquals(before, game.colliderEnergy(), 1e-6, "rien d'acheté tant que le vide attend");
    }

    @Test
    void lesAutomatesDuCollisionneurAttendentAussiPuisRepartent() {
        Game game = thirdUniverse();
        var auto = idle.core.collider.ColliderUpgrade.automationOf(idle.core.collider.ColliderGenerator.values()[0]);
        game.state().setColliderUpgradeLevel(auto, 1);
        assertTrue(game.isColliderAutomationRunning(auto));
        RobotCosmosPlanner bot = new RobotCosmosPlanner(game, 20);
        bot.collider();
        assertTrue(game.isColliderUnlocked());
        assertFalse(game.isColliderAutomationRunning(auto), "en pause tant que le vide attend");
        bot.voidPolicy.play(game);
        assertTrue(game.isVoidUnlocked());
        bot.collider();
        assertTrue(game.isColliderAutomationRunning(auto), "relancé une fois le vide ouvert");
    }

    @Test
    void leTrouNoirSeNourritApresLeHuitiemeBigBangMaisPasChezLAvare() {
        Game game = thirdUniverse();
        game.testOpenVoidHole();
        game.state().setBigBangs(8);
        game.state().setParticles(BigNum.of(1, 1_000_000));
        assertTrue(VoidPolicy.worthSacrifice(game));
        VoidPolicy hoarder = new VoidPolicy(IdleProfiles24.VOID_STYLES[11], new Random(1));
        for (int i = 0; i < 20; i++) hoarder.play(game);
        assertEquals(0, hoarder.sacrifices, "le thésauriseur garde ses particules");
        VoidPolicy expert = new VoidPolicy(VoidPolicy.Style.EXPERT, new NoRandom());
        expert.play(game);
        assertEquals(1, expert.sacrifices);
        assertTrue(game.voidMass() > 10);
        game.state().setParticles(BigNum.of(1, 1_000_000));
        assertFalse(VoidPolicy.worthSacrifice(game), "autant de particules encore n'ajoutent presque rien");
    }

    @Test
    void sansParticulesLExpertCoupeLaFusionPuisNourritLeTrouNoir() {
        Game game = thirdUniverse();
        game.testOpenVoidHole();
        game.state().setBigBangs(8);
        game.state().setParticles(BigNum.ZERO);
        game.state().addAutomation("auto_fusion");
        game.setAutomationEnabled("auto_fusion", true);
        VoidPolicy expert = new VoidPolicy(VoidPolicy.Style.EXPERT, new NoRandom());
        expert.play(game);
        assertFalse(game.isAutomationEnabled("auto_fusion"), "la fusion attend : les particules iront au trou noir");
        game.state().setParticles(BigNum.of(1, 1_000_000));
        expert.play(game);
        assertEquals(1, expert.sacrifices);
        assertTrue(game.isAutomationEnabled("auto_fusion"), "et elle repart aussitôt");
    }

    @Test
    void chaqueProfilASaFaconDeJouerLeVide() {
        assertEquals(IdleProfiles24.PROFILES.length, IdleProfiles24.VOID_STYLES.length);
        // Le cliqueur au hasard achète quand même, au hasard.
        Game game = thirdUniverse();
        game.testOpenVoid();
        game.state().setVoidEnergy(BigNum.of(10_000));
        VoidPolicy clicker = new VoidPolicy(IdleProfiles24.VOID_STYLES[14], new Random(7));
        for (int i = 0; i < 20; i++) clicker.play(game);
        assertTrue(clicker.bought > 0);
        assertEquals(0.25, game.state().voidShare(), 1e-12, "il garde le réglage du jeu");
    }

    @Test
    void leMoinsCherAcheteLAmeliorationDuVideLaMoinsChereQuelleQuElleSoit() {
        // Plan.CHEAPEST (AchievementHunter, Collector, PrestigeAddict…) : pas de préférence pour les raccourcis.
        Game game = thirdUniverse();
        game.testOpenVoid();
        game.state().setVoidEnergy(BigNum.of(10_000));
        VoidUpgrade cheapest = null;
        for (VoidUpgrade upgrade : VoidUpgrade.values())
            if (game.canBuyVoid(upgrade) && (cheapest == null || game.voidCost(upgrade) < game.voidCost(cheapest))) cheapest = upgrade;
        assertTrue(cheapest != null && !VoidPolicy.SHORTENERS.contains(cheapest), "la croissance la moins chère est moins chère que les raccourcis : " + cheapest);
        VoidPolicy policy = new VoidPolicy(new VoidPolicy.Style(1, 0.25, VoidPolicy.Plan.CHEAPEST, 1, 1), new NoRandom());
        policy.play(game);
        assertTrue(policy.bought > 1);
        assertTrue(game.voidLevel(cheapest) >= 1, "le premier achat est le moins cher de tous");
        assertTrue(game.state().voidEnergy().toDouble() < 1_000, "il dépense tant qu'il peut payer : " + game.state().voidEnergy());
    }

    @Test
    void laPartDuVideImposeeEstValidee() {
        // Correctif 0.141 (B10) : une valeur fautive de idle.partVide arrêtait la campagne par une NumberFormatException
        // au premier univers 3 ; elle est refusée d'emblée, avec un message clair, et une part hors de 0 à 1 aussi.
        assertEquals(null, VoidPolicy.configuredShare());
        try {
            System.setProperty("idle.partVide", "0.3");
            assertEquals(0.3, VoidPolicy.configuredShare(), 1e-12);
            Game game = thirdUniverse();
            new VoidPolicy(VoidPolicy.Style.EXPERT, new NoRandom()).play(game);
            assertEquals(0.3, game.state().voidShare(), 1e-12, "la part imposée remplace celle du profil");
            for (String bad : new String[] {"abc", "2", "-0.1"}) {
                System.setProperty("idle.partVide", bad);
                IllegalArgumentException refused = org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalArgumentException.class, VoidPolicy::configuredShare, bad);
                assertTrue(refused.getMessage().contains("idle.partVide=" + bad), refused.getMessage());
            }
        } finally {
            System.clearProperty("idle.partVide");
        }
    }

    @Test
    void sansVideLeVideResteFerme() {
        Game game = thirdUniverse();
        System.setProperty("idle.sansVide", "true");
        try {
            VoidPolicy expert = new VoidPolicy();
            assertEquals(0, expert.play(game));
            assertFalse(expert.savingForVoid(game));
            assertFalse(game.isVoidUnlocked());
        } finally {
            System.clearProperty("idle.sansVide");
        }
    }
}
