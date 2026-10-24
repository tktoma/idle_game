package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.VoidUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Version 0.129 : le vide. Il se propose au premier Big Bang du troisième univers et s'achète en énergie du
 * collisionneur ; ses paires coûtent de l'énergie et de l'espace, donnent de l'énergie du vide, qui fond vers un
 * équilibre ; le trou noir, nourri de particules, raccourcit les paires et ralentit la fonte ; sept améliorations ;
 * à la formation de l'univers, ce qui reste rejoint le cumul.
 */
class VideTest {

    /** Le troisième univers, au premier Big Bang : le vide se propose. */
    static Game third() {
        Game game = DeuxiemeUniversTest.second();
        assertTrue(game.testAddUniverse());
        GameTestSupport.bangs(game, 1);
        return game;
    }

    /** Le troisième univers, vide ouvert, avec de quoi faire naître des paires : de l'énergie et de l'espace libre. */
    static Game open() {
        Game game = third();
        game.state().setColliderEnergy(game.voidPrice() + 1e6);
        assertTrue(game.openVoid());
        game.state().setSpace(BigNum.of(1e6));
        return game;
    }

    private static GameState reread(GameState state) {
        try {
            return SaveCodec.read(SaveCodec.write(state, 0)).state();
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
    }

    // ------------------------------------------------------------------
    // L'ouverture
    // ------------------------------------------------------------------

    @Test
    void leVideSeProposeAuPremierBigBangDuTroisiemeUnivers() {
        Game first = new Game();
        first.start();
        GameTestSupport.bangs(first, 3);
        assertFalse(first.isVoidOffered(), "pas dans le premier univers, même après des Big Bangs");
        Game game = DeuxiemeUniversTest.second();
        GameTestSupport.bangs(game, 1);
        assertFalse(game.isVoidOffered(), "pas dans le deuxième univers");
        GameTestSupport.bangs(game, 0);
        assertTrue(game.testAddUniverse());
        assertFalse(game.isVoidOffered(), "avant le premier Big Bang du troisième univers");
        GameTestSupport.bangs(game, 1);
        assertTrue(game.isVoidOffered());
        assertFalse(game.isVoidUnlocked(), "il se propose, il ne s'ouvre pas tout seul");
        game.tick(60);
        assertEquals(0, game.voidEnergy().sign(), "fermé, rien ne s'y gagne");
    }

    @Test
    void sonPrixEstDixMinutesDeCollisionneurFixeesQuandIlSePropose() {
        Game game = third();
        assertEquals(Game.VOID_UNLOCK_COST, game.voidPrice(), 1e-3, "un collisionneur faible : le prix plancher");
        // Un collisionneur fort : dix minutes de sa production.
        game.state().setColliderGenerators(idle.core.collider.ColliderGenerator.BEAM, 9_999_999);
        double production = game.colliderEnergyPerSecond();
        assertTrue(600 * production > Game.VOID_UNLOCK_COST);
        game.tick(0.01);
        double fixed = game.state().voidPrice();
        assertEquals(600 * production, fixed, fixed * 1e-6, "fixé au premier tick où il se propose");
        game.state().setColliderGenerators(idle.core.collider.ColliderGenerator.BEAM, 99_999_999);
        game.tick(0.01);
        assertEquals(fixed, game.voidPrice(), 1e-6, "il ne monte plus avec le collisionneur");
        assertEquals(fixed, reread(game.state()).voidPrice(), 1e-6);
        game.state().setColliderEnergy(fixed);
        assertTrue(game.openVoid());
        assertEquals(0, game.colliderEnergy(), fixed * 1e-9);
        // L'univers suivant le refixe.
        DeuxiemeUniversTest.formUniverse(game);
        assertEquals(0, game.state().voidPrice());
    }

    @Test
    void leVideSAcheteEnEnergieDuCollisionneur() {
        Game game = third();
        game.state().setColliderEnergy(Game.VOID_UNLOCK_COST * 0.999);
        assertFalse(game.canOpenVoid(), "pas assez d'énergie");
        assertFalse(game.openVoid());
        assertFalse(game.isVoidUnlocked());
        game.state().setColliderEnergy(Game.VOID_UNLOCK_COST + 500);
        assertTrue(game.canOpenVoid());
        assertTrue(game.openVoid());
        assertTrue(game.isVoidUnlocked());
        assertEquals(500, game.colliderEnergy(), 1e-3, "le prix est pris au collisionneur");
        assertFalse(game.isVoidOffered(), "ouvert, il ne se propose plus");
        assertFalse(game.openVoid(), "on ne l'achète pas deux fois");
        assertEquals(1e9, Game.VOID_UNLOCK_COST, "un vrai prix, pas presque gratuit");
        assertEquals(600, Game.VOID_UNLOCK_SECONDS);
    }

    @Test
    void leVideOuvertSeRelitEtUneVieilleSauvegardeLeLaisseFerme() {
        Game game = open();
        GameState back = reread(game.state());
        assertTrue(back.voidOpen());
        GameState fresh = reread(new GameState());
        assertFalse(fresh.voidOpen());
        assertFalse(fresh.voidHoleOpen());
        assertEquals(0.25, fresh.voidShare(), 1e-12);
        assertTrue(fresh.voidHistory().isEmpty());
        for (VoidUpgrade upgrade : VoidUpgrade.values()) assertEquals(0, fresh.voidLevel(upgrade.ordinal()));
    }

    // ------------------------------------------------------------------
    // Les paires
    // ------------------------------------------------------------------

    @Test
    void lesPairesCoutentDeLEnergieEtDeLEspaceEtDonnentDeLEnergieDuVide() {
        Game game = open();
        double production = game.colliderEnergyPerSecond();
        assertTrue(production > 0);
        assertEquals(0.5 * production, game.voidPairEnergy(), 1e-9, "une demi-seconde de collisionneur");
        assertEquals(game.steadySpacePerSecond().toDouble() * 0.2, game.voidPairSpace().toDouble(), 1e-9);
        // La part réglée, un quart : le collisionneur paie au plus un quart de sa production, soit 0,5 paire par seconde.
        assertEquals(0.5, game.voidEnergyRateLimit(), 1e-12);
        assertEquals(0.5, game.voidRate(), 1e-12);
        assertEquals("énergie", game.voidLimit());
        double before = game.colliderEnergy();
        game.tick(1);
        assertTrue(game.voidEnergy().toDouble() > 0.49 && game.voidEnergy().toDouble() <= 0.5, "une demi-paire, gain 1");
        assertTrue(game.voidPairs() > 0);
        // Le collisionneur a produit, et payé le quart de sa production aux paires.
        assertEquals(before + production * 0.75, game.colliderEnergy(), production * 0.05);
    }

    @Test
    void laFonteMeneALEquilibreAffiche() {
        Game game = open();
        double equilibrium = game.voidEquilibrium();
        assertEquals(0.5 / 0.002, equilibrium, 1e-9, "gain par seconde ÷ 0,002");
        game.tick(20_000);
        assertEquals(equilibrium, game.voidEnergy().toDouble(), equilibrium * 1e-3);
        // Sans paires, la réserve fond de 0,2 % par seconde : moitié en près de six minutes.
        game.setVoidShare(0);
        assertEquals(0, game.voidRate());
        double stock = game.voidEnergy().toDouble();
        game.tick(Math.log(2) / 0.002);
        assertEquals(stock / 2, game.voidEnergy().toDouble(), stock * 1e-6);
    }

    @Test
    void unLongTickDonneCeQueDonnentBeaucoupDePetits() {
        Game one = open();
        Game many = open();
        one.tick(600);
        for (int k = 0; k < 600; k++) many.tick(1);
        assertEquals(many.voidEnergy().toDouble(), one.voidEnergy().toDouble(), many.voidEnergy().toDouble() * 1e-3);
        assertEquals(many.voidPairs(), one.voidPairs(), 1e-3);
    }

    @Test
    void sansProductionNiEspaceNiPartPasDePaire() {
        Game game = open();
        game.setVoidShare(0);
        assertEquals(0, game.voidRate());
        game.setVoidShare(1);
        assertEquals(2, game.voidEnergyRateLimit(), 1e-12, "toute la production : de quoi payer deux paires par seconde");
        assertEquals(1, game.voidRate(), 1e-12, "le rythme n'en permet qu'une");
        assertEquals("rythme", game.voidLimit());
        game.state().setSpace(BigNum.ZERO);
        assertEquals(0, game.voidRate(), "pas d'espace libre, pas de paire");
        assertEquals("espace", game.voidLimit());
        assertEquals(0, game.voidSpaceLimit());
    }

    @Test
    void lesPairesTiennentLeurEspaceTantQuEllesVivent() {
        Game game = open();
        BigNum used = game.usedSpace();
        game.tick(30);
        double pairs = game.voidPairs();
        assertEquals(0.5 * Game.VOID_PAIR_LIFE, pairs, 0.05, "rythme × vie");
        assertEquals(used.add(game.voidPairSpace().multiply(pairs)).toDouble(), game.usedSpace().toDouble(), 1e-6);
        // L'espace libre fond (une molécule, un Big Bang) : les paires en trop s'annulent tout de suite.
        game.state().setSpace(game.voidPairSpace().multiply(4));
        game.tick(0.01);
        assertTrue(game.voidPairs() <= 1.05, "un quart de quatre paires d'espace (et le peu que l'expansion vient d'ajouter)");
    }

    @Test
    void lAbsenceDonneCeQueDonneraitLeJeuOuvert() {
        Game present = open();
        Game away = open();
        present.tick(3600);
        away.tickAway(3600);
        assertEquals(present.voidEnergy().toDouble(), away.voidEnergy().toDouble(), present.voidEnergy().toDouble() * 1e-6);
    }

    // ------------------------------------------------------------------
    // Le trou noir
    // ------------------------------------------------------------------

    @Test
    void leTrouNoirSOuvreAuPremierTrouNoirForme() {
        Game game = open();
        assertFalse(game.isVoidHoleOpen());
        game.state().setParticles(BigNum.of(1e10));
        assertFalse(game.sacrificeParticles(), "pas de trou noir, pas de sacrifice");
        game.state().addBody("black_hole");
        game.tick(0.1);
        assertTrue(game.isVoidHoleOpen());
        assertTrue(reread(game.state()).voidHoleOpen());
    }

    @Test
    void lesParticulesSacrifieesDonnentSaMasse() {
        Game game = open();
        game.testOpenVoidHole();
        assertEquals(0, game.voidMass());
        assertEquals(Game.VOID_PAIR_LIFE, game.voidPairLife(), 1e-12);
        game.state().setParticles(BigNum.of(1, 99));
        assertEquals(2 * Math.log10(1 + 99), game.voidMassAfterSacrifice(), 1e-9);
        assertTrue(game.sacrificeParticles());
        assertEquals(0, game.state().particles().sign(), "toutes les particules y passent");
        double m = game.voidMass();
        assertEquals(2 * Math.log10(100), m, 1e-6);
        assertEquals(6 / (1 + m / 10), game.voidPairLife(), 1e-9);
        assertEquals(0.002 / (1 + m / 5), game.voidDecay(), 1e-12);
        assertFalse(game.sacrificeParticles(), "plus de particules");
        // Les nombres immenses de la fin de partie : au premier trou noir, des particules à quatre millions de chiffres.
        game.state().setParticles(BigNum.of(1, 4_000_000));
        assertTrue(game.sacrificeParticles());
        assertEquals(2 * Math.log10(1 + 4e6), game.voidMass(), 1e-6, "un peu plus de 13");
        // La masse a un plafond.
        game.state().setParticles(BigNum.of(1, 40_000_000));
        assertTrue(game.sacrificeParticles());
        assertEquals(Game.VOID_MAX_MASS, game.voidMass(), 1e-12);
        game.state().setParticles(BigNum.of(1, 50));
        assertFalse(game.canSacrificeParticles(), "plafond atteint");
    }

    @Test
    void leTrouNoirLibereLEspacePlusViteEtRelevelEquilibre() {
        Game game = open();
        double life = game.voidPairLife(), equilibrium = game.voidEquilibrium();
        game.testOpenVoidHole();
        game.state().setVoidSacrificed(BigNum.of(1, 9999));
        assertTrue(game.voidPairLife() < life);
        assertTrue(game.voidEquilibrium() > equilibrium * 2);
        assertEquals(game.voidSpaceLimit() / game.voidPairLife(), game.voidSpaceRateLimit(), 1e-9);
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    @Test
    void lesAmeliorationsSePaientEnEnergieDuVide() {
        Game game = open();
        assertFalse(game.buyVoid(VoidUpgrade.GAIN), "rien en réserve");
        game.state().setVoidEnergy(BigNum.of(100));
        assertEquals(10, game.voidCost(VoidUpgrade.GAIN), 1e-12);
        assertTrue(game.buyVoid(VoidUpgrade.GAIN));
        assertEquals(90, game.voidEnergy().toDouble(), 1e-9);
        assertEquals(22, game.voidCost(VoidUpgrade.GAIN), 1e-9, "×2,2 par niveau");
        assertEquals(2, game.voidPairGain(), 1e-12);
        // Le rythme, le coût en énergie, l'espace, la fonte.
        game.state().setVoidLevel(VoidUpgrade.RATE.ordinal(), 2);
        assertEquals(1.5625, game.voidBaseRate(), 1e-12);
        double energy = game.voidPairEnergy();
        game.state().setVoidLevel(VoidUpgrade.ENERGY.ordinal(), 1);
        assertEquals(energy * 0.85, game.voidPairEnergy(), 1e-9);
        double space = game.voidPairSpace().toDouble();
        game.state().setVoidLevel(VoidUpgrade.SPACE.ordinal(), 1);
        assertEquals(space * 0.85, game.voidPairSpace().toDouble(), 1e-9);
        game.state().setVoidLevel(VoidUpgrade.DECAY.ordinal(), 1);
        assertEquals(0.0018, game.voidDecay(), 1e-12);
    }

    @Test
    void lesAmeliorationsOntUnPlafond() {
        Game game = open();
        game.state().setVoidEnergy(BigNum.of(1e30));
        for (VoidUpgrade upgrade : VoidUpgrade.values()) {
            if (upgrade.max() == Integer.MAX_VALUE) continue;
            game.state().setVoidLevel(upgrade.ordinal(), upgrade.max());
            assertFalse(game.canBuyVoid(upgrade), upgrade + " au plafond");
            assertFalse(game.buyVoid(upgrade));
        }
        assertTrue(game.buyVoid(VoidUpgrade.GAIN), "le gain n'a pas de plafond");
        Game closed = third();
        closed.state().setVoidEnergy(BigNum.of(1e6));
        assertFalse(closed.buyVoid(VoidUpgrade.GAIN), "vide fermé");
    }

    @Test
    void laMaturationPlusCourteRetireTroisPourCentParNiveau() {
        Game game = open();
        Body star = null;
        for (Body each : game.bodies()) if (each.tier() == Body.Tier.STAR) { star = each; break; }
        assertTrue(star != null);
        double base = game.maturationSeconds(star);
        assertEquals(1, game.maturationShare(), 1e-12);
        game.state().setVoidLevel(VoidUpgrade.MATURE.ordinal(), 10);
        assertEquals(0.7, game.maturationShare(), 1e-12, "dix niveaux : −30 %");
        assertEquals(base * 0.7, game.maturationSeconds(star), 1e-6);
        game.state().setVoidLevel(VoidUpgrade.MATURE.ordinal(), 99);
        assertEquals(0.7, game.maturationShare(), 1e-12, "jamais plus de dix niveaux comptés");
        assertEquals(10, VoidUpgrade.MATURE.max());
    }

    @Test
    void laMaturationAchieteeRaccourcitAussiLesAstresQuiMurissentDeja() {
        Game game = open();
        game.state().addBody("black_hole");
        game.state().setMaturing("black_hole", 1000);
        game.state().setVoidEnergy(BigNum.of(1e6));
        assertEquals(60, game.voidCost(VoidUpgrade.MATURE), 1e-9);
        assertTrue(game.buyVoid(VoidUpgrade.MATURE));
        assertEquals(1, game.voidLevel(VoidUpgrade.MATURE));
        assertEquals(970, game.maturationLeft("black_hole"), 1e-6, "le reste raccourcit de 3 %");
        assertTrue(game.buyVoid(VoidUpgrade.MATURE));
        assertEquals(1000 * 0.94, game.maturationLeft("black_hole"), 1e-6, "deux niveaux : 94 % du reste de départ");
        assertEquals(375, game.voidCost(VoidUpgrade.MATURE), 1e-9, "60 × 2,5², au troisième niveau");
        // Les autres améliorations ne touchent pas à la maturation.
        assertTrue(game.buyVoid(VoidUpgrade.GAIN));
        assertEquals(940, game.maturationLeft("black_hole"), 1e-6);
        // Relue, la partie garde le niveau ; à la formation de l'univers, il repart de zéro.
        assertEquals(2, reread(game.state()).voidLevel(VoidUpgrade.MATURE.ordinal()));
        DeuxiemeUniversTest.formUniverse(game);
        assertEquals(1, game.maturationShare(), 1e-12);
    }

    @Test
    void lAllegementRetireTroisPourCentAuxBesoinsDuCiel() {
        Game game = open();
        Body body = null;
        for (Body each : game.bodies()) {
            if (!each.molecules().isEmpty()) {
                body = each;
                break;
            }
        }
        assertTrue(body != null);
        assertEquals(body.molecules(), game.bodyMolecules(body), "sans allègement, les besoins du catalogue");
        game.state().setVoidLevel(VoidUpgrade.LIGHTEN.ordinal(), 10);
        assertEquals(0.7, game.needShare(), 1e-12);
        for (Map.Entry<String, Integer> need : body.molecules().entrySet()) {
            int lightened = game.bodyMolecules(body).get(need.getKey());
            assertEquals(Math.max(1, (int) Math.ceil(need.getValue() * 0.7 - 1e-9)), lightened);
        }
        for (Map.Entry<Molecule.State, Integer> need : Cosmos.UNIVERSE.matter().entrySet()) {
            assertEquals((int) Math.ceil(need.getValue() * 0.7 - 1e-9), (int) game.cosmosMatter(Cosmos.UNIVERSE).get(need.getKey()));
        }
        assertEquals(1, game.lightened(1), "jamais sous 1");
        // Les conditions de l'astre comptent les besoins allégés.
        game.state().setVoidLevel(VoidUpgrade.LIGHTEN.ordinal(), 0);
        int before = game.bodyConditionsMet(body.id());
        Map.Entry<String, Integer> first = body.molecules().entrySet().iterator().next();
        game.state().setMoleculeCount(first.getKey(), (int) Math.ceil(first.getValue() * 0.7));
        int unlightened = game.bodyConditionsMet(body.id());
        game.state().setVoidLevel(VoidUpgrade.LIGHTEN.ordinal(), 10);
        assertEquals(unlightened + (first.getValue() > 1 && Math.ceil(first.getValue() * 0.7) < first.getValue() ? 1 : 0),
                game.bodyConditionsMet(body.id()));
        assertTrue(game.bodyConditionsMet(body.id()) >= before);
    }

    // ------------------------------------------------------------------
    // La formation de l'univers
    // ------------------------------------------------------------------

    @Test
    void laFormationGardeLeCumulEtRemetLeVideAZero() {
        Game game = open();
        game.state().setVoidEnergy(BigNum.of(1234.5));
        game.state().setVoidLevel(VoidUpgrade.GAIN.ordinal(), 3);
        game.state().setVoidSacrificed(BigNum.of(1, 40));
        game.state().setVoidHoleOpen(true);
        assertTrue(game.universeTransition(false).lost().stream().anyMatch(line -> line.contains("234")),
                "le guide le dit : la réserve rejoint le cumul");
        DeuxiemeUniversTest.formUniverse(game);
        assertEquals(1234.5, game.voidAccumulated(), 1e-9);
        assertEquals(Map.of("3", 1234.5), game.state().voidHistory(), "noté pour l'univers 3");
        assertEquals(0, game.voidEnergy().sign(), "le vide repart de zéro");
        assertFalse(game.isVoidUnlocked(), "il faudra le racheter");
        assertFalse(game.state().voidHoleOpen());
        assertEquals(0, game.voidLevel(VoidUpgrade.GAIN));
        assertEquals(0, game.state().voidSacrificed().sign());
        // Lancer l'univers suivant garde le cumul ; le relire aussi.
        assertTrue(game.startNextUniverse());
        assertEquals(1234.5, game.voidAccumulated(), 1e-9);
        assertEquals(1234.5, reread(game.state()).voidAccumulated(), 1e-9);
        assertFalse(game.isVoidOffered(), "avant le premier Big Bang de l'univers 4");
        GameTestSupport.bangs(game, 1);
        assertTrue(game.isVoidOffered());
    }

    @Test
    void unVideJamaisOuvertNAjouteRienAuCumul() {
        Game game = third();
        game.state().setVoidEnergy(BigNum.of(50));
        DeuxiemeUniversTest.formUniverse(game);
        assertTrue(game.state().voidHistory().isEmpty());
        assertEquals(0, game.voidEnergy().sign());
    }

    @Test
    void lesOutilsDEssaiRemettentLeVideAZero() {
        Game game = open();
        game.state().noteVoidHistory("3", 10);
        game.state().setVoidLevel(VoidUpgrade.RATE.ordinal(), 4);
        game.testResetUniverses();
        assertFalse(game.isVoidUnlocked());
        assertEquals(0, game.voidAccumulated());
        assertEquals(0, game.voidLevel(VoidUpgrade.RATE));
    }
}
