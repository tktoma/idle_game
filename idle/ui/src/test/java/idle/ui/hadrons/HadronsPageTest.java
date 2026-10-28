package idle.ui.hadrons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.ui.FxTestSupport;
import idle.ui.collider.ColliderPage;
import idle.ui.common.Card;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.149 : les hadrons dans l'interface, sous Xvfb. L'offre dans le collisionneur et son bouton, la page des
 * gluons (quarks en main, cartes, boutons « Lier 1 » et « Lier tout »), les exotiques fermées puis ouvertes par la bulle
 * du cœur de l'arbre-monde.
 */
class HadronsPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Un univers lancé après le premier, le collisionneur ouvert dès le début, avec ces bulles. */
    private static Game with(TreeBubble... bubbles) {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        for (TreeBubble bubble : bubbles) game.state().setTreeBubbleBought(bubble.ordinal(), true);
        game.state().setTreeBubbleBought(TreeBubble.CROWN_LEFT.ordinal(), true);
        assertTrue(game.startNextUniverse());
        assertTrue(game.isColliderUnlocked());
        return game;
    }

    /** L'onglet ouvert, trente quarks de chaque saveur en main. */
    private static Game open(TreeBubble... bubbles) {
        Game game = with(bubbles);
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
    void leCollisionneurProposeLesHadronsEtLesOuvre() {
        FxTestSupport.fx(() -> {
            ColliderPage bare = new ColliderPage(with());
            bare.refresh();
            assertFalse(bare.hadronsBox().isVisible(), "sans la bulle de l'arbuste du milieu : pas d'offre");

            Game game = with(TreeBubble.CROWN_MIDDLE);
            assertTrue(game.isHadronsOffered());
            ColliderPage page = new ColliderPage(game);
            page.refresh();
            assertTrue(page.hadronsBox().isVisible());
            assertTrue(page.openHadronsButton().getText().startsWith("Ouvrir les hadrons : "), page.openHadronsButton().getText());
            assertTrue(page.openHadronsButton().getText().endsWith(" énergie"), page.openHadronsButton().getText());
            assertTrue(page.openHadronsButton().isDisable(), "pas assez d'énergie");
            assertFalse(ColliderPage.ready(game));
            game.state().setColliderEnergy(game.hadronsPrice());
            page.refresh();
            assertFalse(page.openHadronsButton().isDisable());
            assertTrue(ColliderPage.ready(game), "la pastille du collisionneur le signale");
            page.openHadronsButton().fire();
            assertTrue(game.isHadronsUnlocked());
            assertFalse(page.hadronsBox().isVisible(), "ouvert, l'offre disparaît");
            assertFalse(page.hadronsBox().isManaged());
            return null;
        });
    }

    @Test
    void lesCartesEtLesBoutonsSuiventLesQuarks() {
        FxTestSupport.fx(() -> {
            Game game = open();
            HadronsPage page = page(game);
            assertEquals("u  haut : 30", page.quarkText(Quark.UP));
            assertEquals("b  beau : 30", page.quarkText(Quark.BOTTOM));
            assertEquals("", page.quarkText(Quark.TOP), "le top ne se garde pas");
            assertTrue(page.effectNote().getText().contains("n'ont pas encore d'effet"), page.effectNote().getText());
            Card proton = page.card(Hadron.PROTON);
            assertEquals(Card.State.READY, proton.state());
            for (String part : new String[] {"Proton", "u u d", "2 u + 1 d", "0 possédé", Hadron.PROTON.detail()}) {
                assertTrue(proton.text().contains(part), part + " : " + proton.text());
            }
            assertEquals("1 u + 2 d", HadronsPage.recipe(Hadron.NEUTRON));
            assertEquals("2 c", HadronsPage.recipe(Hadron.J_PSI));
            assertEquals("Lier tout (15)", page.bindAllButton(Hadron.PROTON).getText());
            assertTrue(HadronsPage.ready(game));

            page.bindOneButton(Hadron.PROTON).fire();
            assertEquals(1, game.hadrons(Hadron.PROTON));
            assertEquals(28, game.state().quarks(Quark.UP));
            assertEquals(29, game.state().quarks(Quark.DOWN));
            assertTrue(proton.text().contains("1 possédé"), proton.text());
            assertEquals("Lier tout (14)", page.bindAllButton(Hadron.PROTON).getText());

            page.bindAllButton(Hadron.PROTON).fire();
            assertEquals(15, game.hadrons(Hadron.PROTON));
            assertEquals(0, game.state().quarks(Quark.UP));
            assertTrue(page.bindOneButton(Hadron.PROTON).isDisable(), "plus de quark haut");
            assertTrue(page.bindAllButton(Hadron.PROTON).isDisable());
            assertTrue(page.bindOneButton(Hadron.NEUTRON).isDisable());
            assertEquals(Card.State.STARTED, proton.state());
            assertTrue(proton.text().contains("15 possédés"), proton.text());
            assertFalse(HadronsPage.ready(game), "plus rien à lier");
            return null;
        });
    }

    @Test
    void lesExotiquesSOuvrentAvecLaBulleDuCoeur() {
        FxTestSupport.fx(() -> {
            Game game = open();
            HadronsPage page = page(game);
            assertTrue(page.exoticNote().isVisible());
            assertTrue(page.exoticNote().getText().contains("La bulle du cœur de l'arbre-monde les ouvre"), page.exoticNote().getText());
            assertFalse(page.exoticGrid().isVisible());
            assertFalse(page.exoticGrid().isManaged());
            assertTrue(page.bindOneButton(Hadron.OMEGA).isDisable(), "fermées : rien ne se lie");
            assertEquals("Lier tout (0)", page.bindAllButton(Hadron.OMEGA).getText());

            game.state().setTreeBubbleBought(TreeBubble.HEART.ordinal(), true);
            page.refresh();
            assertTrue(game.isExoticHadronsUnlocked());
            assertTrue(page.exoticGrid().isVisible());
            assertFalse(page.exoticNote().isVisible());
            assertEquals(Card.State.READY, page.card(Hadron.OMEGA).state());
            assertEquals("Lier tout (10)", page.bindAllButton(Hadron.OMEGA).getText());
            page.bindOneButton(Hadron.OMEGA).fire();
            assertEquals(1, game.hadrons(Hadron.OMEGA));
            assertEquals(27, game.state().quarks(Quark.STRANGE));
            page.bindAllButton(Hadron.J_PSI).fire();
            assertEquals(15, game.hadrons(Hadron.J_PSI));
            assertEquals(0, game.state().quarks(Quark.CHARM));
            assertTrue(page.bindOneButton(Hadron.D_MESON).isDisable(), "plus de quark charmé");
            return null;
        });
    }
}
