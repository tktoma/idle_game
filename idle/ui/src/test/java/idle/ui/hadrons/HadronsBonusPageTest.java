package idle.ui.hadrons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.GluonMilestone;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.ui.FxTestSupport;
import idle.ui.common.Card;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.151 : ce que font les hadrons, dans l'interface, sous Xvfb. Les bonus sous les cartes et les gluons en tête,
 * le sous-onglet « Paliers » et ses barres, le sous-onglet « Liaison » et son animation, qui démarre au clic « Lier » et
 * ne s'anime qu'à l'écran.
 */
class HadronsBonusPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Un univers lancé après le premier, l'onglet des hadrons ouvert, trente quarks de chaque saveur en main. */
    private static Game open() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        game.state().setTreeBubbleBought(TreeBubble.CROWN_LEFT.ordinal(), true);
        assertTrue(game.startNextUniverse());
        game.state().setHadronsOpen(true);
        for (Quark quark : Quark.values()) game.state().setQuarks(quark, 30);
        return game;
    }

    private static HadronsPage page(Game game) {
        HadronsPage page = new HadronsPage(game);
        new Scene(page, 1200, 760);
        page.refresh();
        page.applyCss();
        page.layout();
        return page;
    }

    @Test
    void lesCartesMontrentLeurBonusEtCeQuIlDeviendrait() {
        assertEquals("Atomes par fusion : ×1 → ×1,08 avec un de plus, ×1,30 avec « Lier tout »", HadronsPage.bonusText(Hadron.PROTON, 0, 15));
        assertEquals("Atomes par fusion : ×1,7501 → ×1,7502 avec un de plus", HadronsPage.bonusText(Hadron.PROTON, 1000, 0),
                "assez de décimales pour que la hausse se lise");
        assertEquals("Prix des générateurs : ÷1,25 → ÷1,26 avec un de plus", HadronsPage.bonusText(Hadron.B_MESON, 9, 1));
        FxTestSupport.fx(() -> {
            Game game = open();
            HadronsPage page = page(game);
            assertEquals(HadronsPage.bonusText(Hadron.PROTON, 0, 15), page.bonusText(Hadron.PROTON));
            assertTrue(page.bonusText(Hadron.J_PSI).startsWith("Prix des améliorations du collisionneur : ÷1 → ÷"), page.bonusText(Hadron.J_PSI));
            assertEquals("Gluons : 0   |   paliers atteints : 0 / 5", page.gluonsLine().getText());
            assertFalse(page.effectNote().getText().contains("pas encore d'effet"), page.effectNote().getText());
            assertTrue(page.effectNote().getText().contains("log₁₀(1 + n)"), page.effectNote().getText());
            page.bindOneButton(Hadron.PROTON).fire();
            assertTrue(page.bonusText(Hadron.PROTON).startsWith("Atomes par fusion : ×1,08 → ×1,12"), page.bonusText(Hadron.PROTON));
            assertEquals(game.hadronBoost(Hadron.PROTON), 1 + Game.HADRON_BOOST_STEP * Math.log10(2), 1e-12);
            page.bindAllButton(Hadron.PROTON).fire();
            assertTrue(page.gluonsLine().getText().startsWith("Gluons : 45 "), page.gluonsLine().getText());
            assertFalse(page.bonusText(Hadron.PROTON).contains("Lier tout"), "plus rien à lier : " + page.bonusText(Hadron.PROTON));
            return null;
        });
    }

    @Test
    void lesPaliersDisentCeQuIlsGardentEtCeQuIlManque() {
        FxTestSupport.fx(() -> {
            Game game = open();
            game.state().setHadrons(Hadron.PROTON, 15);
            HadronsPage page = page(game);
            page.milestonesTab().fire();
            assertEquals(HadronsPage.MILESTONES, page.selected());
            Card first = page.milestoneCard(GluonMilestone.UPGRADES);
            assertEquals(Card.State.STARTED, first.state());
            for (String part : new String[] {"Mémoire des liaisons", GluonMilestone.UPGRADES.effect(), "Il manque 955 gluons", "5 %"}) {
                assertTrue(first.text().contains(part), part + " : " + first.text());
            }
            assertEquals(0.045, page.gaugeShare(GluonMilestone.UPGRADES), 1e-9);
            game.state().setHadrons(Hadron.PROTON, 334);
            page.refresh();
            assertTrue(game.isGluonMilestoneReached(GluonMilestone.UPGRADES));
            assertEquals(Card.State.DONE, first.state());
            assertTrue(first.text().contains("atteint") && !first.text().contains("Il manque"), first.text());
            assertEquals(1, page.gaugeShare(GluonMilestone.UPGRADES), 1e-9);
            assertTrue(page.gluonsLine().getText().endsWith("paliers atteints : 1 / 5"), page.gluonsLine().getText());
            assertEquals(Card.State.STARTED, page.milestoneCard(GluonMilestone.AUTOMATIONS).state());
            return null;
        });
    }

    @Test
    void laLiaisonSAnimeAuClicEtSeulementALEcran() {
        FxTestSupport.fx(() -> {
            Game game = open();
            HadronsPage page = page(game);
            BindingView view = page.view();
            page.bindingTab().fire();
            assertEquals(HadronsPage.BINDING, page.selected());
            assertEquals(0, view.bindingsPlaying());
            page.frame(0.1, true);
            assertTrue(view.floatingQuarks() > 0 && view.floatingQuarks() <= BindingView.MAX_FLOATING, "quarks : " + view.floatingQuarks());
            assertFalse(page.bindHereButton(Hadron.PION).isVisible(), "les exotiques fermées : pas de bouton");
            page.bindHereButton(Hadron.PROTON).fire();
            assertEquals(1, game.hadrons(Hadron.PROTON));
            assertEquals(1, view.bindingsPlaying(), "le clic « Lier » la fait démarrer");
            page.frame(0.5, true);
            assertEquals(1, view.bindingsPlaying());
            for (int i = 0; i < 30; i++) page.frame(0.1, true);
            assertEquals(0, view.bindingsPlaying(), "puis elle finit");

            page.gluonsTab().fire();
            page.bindOneButton(Hadron.NEUTRON).fire();
            assertEquals(1, view.bindingsPlaying(), "liée depuis « Gluons »");
            for (int i = 0; i < 40; i++) page.frame(0.1, true);
            assertEquals(1, view.bindingsPlaying(), "sous-onglet caché : elle attend");
            page.bindingTab().fire();
            for (int i = 0; i < 10; i++) page.bindHereButton(Hadron.NEUTRON).fire();
            assertEquals(BindingView.MAX_BINDINGS, view.bindingsPlaying(), "jamais plus de six à la fois");
            page.frame(1 / 60.0, false);
            assertEquals(0, view.bindingsPlaying(), "sans animation, elles finissent d'un coup");
            page.step(1);
            assertEquals(HadronsPage.GLUONS, page.selected(), "Haut/Bas : de « Liaison », retour aux gluons");
            return null;
        });
    }
}
