package idle.ui.vacuum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.VoidUpgrade;
import idle.core.math.BigNum;
import idle.ui.FxTestSupport;
import idle.ui.collider.ColliderPage;
import idle.ui.common.Card;
import idle.ui.universe.UniversePage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.129 : le vide dans l'interface, sous Xvfb. L'offre dans le collisionneur, la page du vide (réserve et
 * équilibre, réglage, améliorations — huit depuis 0.132 —, trou noir), l'écume qui suit les paires de la partie, le cumul dans la graine.
 */
class VoidPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Le troisième univers, au premier Big Bang : le vide se propose. */
    private static Game third() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        assertTrue(game.startNextUniverse());
        assertTrue(game.testAddUniverse());
        game.state().setBigBangs(1);
        assertTrue(game.isVoidOffered());
        return game;
    }

    private static Game open() {
        Game game = third();
        game.state().setColliderEnergy(game.voidPrice() * 2);
        assertTrue(game.openVoid());
        game.state().setSpace(BigNum.of(1e6));
        return game;
    }

    @Test
    void leCollisionneurProposeLeVideEtLOuvre() {
        FxTestSupport.fx(() -> {
            Game game = third();
            ColliderPage page = new ColliderPage(game);
            page.refresh();
            assertTrue(page.voidBox().isVisible());
            assertTrue(page.openVoidButton().isDisable(), "pas assez d'énergie");
            assertTrue(page.openVoidButton().getText().contains("Ouvrir le vide"));
            assertFalse(ColliderPage.ready(game) && game.canOpenVoid());
            game.state().setColliderEnergy(game.voidPrice());
            page.refresh();
            assertFalse(page.openVoidButton().isDisable());
            assertTrue(ColliderPage.ready(game), "le point de l'onglet le signale");
            page.openVoidButton().fire();
            assertTrue(game.isVoidUnlocked());
            assertFalse(page.voidBox().isVisible(), "ouvert, l'offre disparaît");
            assertFalse(page.voidBox().isManaged());
            return null;
        });
    }

    @Test
    void sansOffreLeCollisionneurNeMontreRien() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.state().setBigBangs(1);
            ColliderPage page = new ColliderPage(game);
            page.refresh();
            assertFalse(page.voidBox().isVisible(), "premier univers : pas de vide");
            return null;
        });
    }

    @Test
    void laPageMontreLaReserveLEquilibreEtLeReglage() {
        FxTestSupport.fx(() -> {
            Game game = open();
            game.tick(10);
            VoidPage page = new VoidPage(game);
            page.refresh();
            javafx.scene.control.Label energy = page.energyLabel();
            assertTrue(energy.getText().contains("Énergie du vide"));
            assertTrue(energy.getText().contains("équilibre : 250"), energy.getText());
            assertEquals(25, page.share().getValue(), 1e-9);
            page.share().setValue(50);
            assertEquals(0.5, game.state().voidShare(), 1e-12, "le curseur règle la part");
            page.share().setValue(100);
            assertEquals(1, game.state().voidShare(), 1e-12);
            game.setVoidShare(0.25);
            page.refresh();
            assertEquals(25, page.share().getValue(), 1e-9, "il suit la partie");
            assertEquals(0.25, game.state().voidShare(), 1e-12, "le recopier ne règle rien");
            return null;
        });
    }

    @Test
    void lesCartesAchetentLesAmeliorations() {
        FxTestSupport.fx(() -> {
            Game game = open();
            VoidPage page = new VoidPage(game);
            assertFalse(VoidPage.ready(game));
            game.state().setVoidEnergy(BigNum.of(10));
            page.refresh();
            assertTrue(VoidPage.ready(game));
            Card gain = page.card(VoidUpgrade.GAIN);
            gain.click();
            assertEquals(1, game.voidLevel(VoidUpgrade.GAIN));
            assertEquals(0, game.voidEnergy().toDouble(), 1e-9);
            game.state().setVoidLevel(VoidUpgrade.RATE.ordinal(), VoidUpgrade.RATE.max());
            page.refresh();
            assertEquals(Card.State.DONE, page.card(VoidUpgrade.RATE).state());
            return null;
        });
    }

    @Test
    void lesAmeliorationsOntLeurSousOnglet() {
        FxTestSupport.fx(() -> {
            Game game = open();
            VoidPage page = new VoidPage(game);
            assertEquals(VoidPage.PAIRS, page.selected(), "on arrive sur les paires");
            assertEquals(2, page.subTabs().getChildren().size());
            page.upgradesTab().fire();
            assertEquals(VoidPage.UPGRADES, page.selected());
            page.step(1);
            assertEquals(VoidPage.PAIRS, page.selected(), "en boucle");
            page.step(-1);
            assertEquals(VoidPage.UPGRADES, page.selected());
            // Le point du sous-onglet dit qu'une amélioration est payable.
            assertEquals("Améliorations", page.upgradesTab().getText(), "rien à payer, pas de point");
            game.state().setVoidEnergy(BigNum.of(VoidUpgrade.MATURE.baseCost()));
            page.refresh();
            assertTrue(page.upgradesTab().getText().length() > "Améliorations".length(), page.upgradesTab().getText());
            // La septième carte : la maturation plus courte (0.132 : 200 d'énergie du vide).
            Card mature = page.card(VoidUpgrade.MATURE);
            mature.click();
            assertEquals(1, game.voidLevel(VoidUpgrade.MATURE));
            assertEquals(0, game.voidEnergy().toDouble(), 1e-9);
            // La huitième (0.132) : le rassemblement plus court, 800 d'énergie du vide.
            Card settle = page.card(VoidUpgrade.SETTLE);
            assertTrue(settle != null);
            game.state().setVoidEnergy(BigNum.of(VoidUpgrade.SETTLE.baseCost() - 1));
            page.refresh();
            settle.click();
            assertEquals(0, game.voidLevel(VoidUpgrade.SETTLE), "799 ne suffisent pas");
            game.state().setVoidEnergy(BigNum.of(VoidUpgrade.SETTLE.baseCost()));
            page.refresh();
            settle.click();
            assertEquals(1, game.voidLevel(VoidUpgrade.SETTLE));
            page.refresh();
            assertEquals(8, VoidUpgrade.values().length);
            return null;
        });
    }

    @Test
    void leTrouNoirSeMontreEtPrendLesParticules() {
        FxTestSupport.fx(() -> {
            Game game = open();
            VoidPage page = new VoidPage(game);
            page.refresh();
            assertFalse(page.sacrificeButton().isVisible() && page.sacrificeButton().getParent().isVisible(), "pas encore de trou noir");
            game.state().addBody("black_hole");
            game.tick(0.1);
            game.state().setParticles(BigNum.of(1, 99));
            page.refresh();
            assertTrue(page.sacrificeButton().getParent().isVisible());
            assertFalse(page.sacrificeButton().isDisable());
            assertTrue(page.sacrificeButton().getText().contains("Sacrifier"));
            page.sacrificeButton().fire();
            assertTrue(game.voidMass() > 3.9);
            assertTrue(page.sacrificeButton().isDisable(), "plus de particules");
            return null;
        });
    }

    @Test
    void lEcumeSuitLesPairesDeLaPartie() {
        FxTestSupport.fx(() -> {
            Game game = open();
            VoidPage page = new VoidPage(game);
            page.frame(0.05, true);
            assertEquals(0, page.view().drawnPairs(), "rien n'est encore né");
            for (int k = 0; k < 40; k++) {
                game.tick(0.1);
                page.frame(0.1, true);
            }
            int drawn = page.view().drawnPairs();
            assertTrue(drawn >= 1 && drawn <= 3, "une demi-paire par seconde, pendant quatre secondes : " + drawn);
            // Beaucoup de paires : l'écume en dessine au plus quelques centaines.
            page.view().follow(1e9, 1e6, 6, 10);
            page.frame(0.05, true);
            assertTrue(page.view().drawnPairs() <= VoidView.MAX_DRAWN);
            return null;
        });
    }

    @Test
    void laGraineMontreLeCumulDuVide() {
        FxTestSupport.fx(() -> {
            Game game = open();
            game.state().setVoidEnergy(BigNum.of(1234));
            game.testFormUniverse();
            UniversePage page = new UniversePage(game);
            page.refresh();
            assertTrue(page.voidCumulLabel().isVisible());
            assertTrue(page.voidCumulLabel().getText().contains("1234") || page.voidCumulLabel().getText().contains("1 234"),
                    page.voidCumulLabel().getText());
            assertTrue(page.voidCumulLabel().getText().contains("univers 3"));
            Game fresh = third();
            UniversePage none = new UniversePage(fresh);
            none.refresh();
            assertFalse(none.voidCumulLabel().isVisible(), "pas de cumul, pas de ligne");
            return null;
        });
    }

    @Test
    void lesStatistiquesDisentCeQueLeVideRaccourcit() {
        FxTestSupport.fx(() -> {
            Game game = open();
            assertEquals("rien encore", VoidPage.actThreeCuts(game));
            assertEquals(0, VoidPage.levelsOwned(game));
            game.state().setVoidLevel(VoidUpgrade.MATURE.ordinal(), 7);
            game.state().setVoidLevel(VoidUpgrade.SETTLE.ordinal(), 5);
            game.state().setVoidLevel(VoidUpgrade.GAIN.ordinal(), 3);
            assertEquals("maturation −49 %, rassemblement −80 %", VoidPage.actThreeCuts(game));
            assertEquals(15, VoidPage.levelsOwned(game));
            game.state().setVoidLevel(VoidUpgrade.LIGHTEN.ordinal(), 10);
            assertTrue(VoidPage.actThreeCuts(game).startsWith("besoins du ciel −30 %"), VoidPage.actThreeCuts(game));
            return null;
        });
    }
}
