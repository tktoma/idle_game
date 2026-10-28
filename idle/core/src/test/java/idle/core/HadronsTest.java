package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.core.profile.Title;
import idle.core.save.SaveCodec;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Version 0.149 : l'onglet des hadrons. Les gluons lient les quarks du collisionneur en protons (u u d) et en neutrons
 * (u d d), puis, avec la bulle du cœur de l'arbre-monde, en particules exotiques. L'onglet se propose dans le
 * collisionneur avec la bulle de l'arbuste du milieu, et s'y paie comme le vide. Pas encore d'effet.
 */
class HadronsTest {

    /** Un univers lancé après le premier, le collisionneur ouvert, avec ces bulles et des quarks. */
    private static Game with(TreeBubble... bubbles) {
        Game game = GameTestSupport.realGame();
        game.testFormUniverse();
        for (TreeBubble bubble : bubbles) game.state().setTreeBubbleBought(bubble.ordinal(), true);
        game.state().setTreeBubbleBought(TreeBubble.CROWN_LEFT.ordinal(), true);
        assertTrue(game.startNextUniverse());
        assertTrue(game.isColliderUnlocked());
        for (Quark quark : Quark.values()) game.state().setQuarks(quark, 30);
        return game;
    }

    @Test
    void lesRecettesSuiventLesQuarks() {
        assertEquals(Map.of(Quark.UP, 2, Quark.DOWN, 1), Hadron.PROTON.recipe());
        assertEquals(Map.of(Quark.UP, 1, Quark.DOWN, 2), Hadron.NEUTRON.recipe());
        assertEquals("u u d", Hadron.PROTON.formula());
        assertEquals(Map.of(Quark.UP, 1, Quark.STRANGE, 1), Hadron.KAON.recipe(), "le kaon : avec le quark étrange");
        assertEquals(Map.of(Quark.CHARM, 1, Quark.UP, 1), Hadron.D_MESON.recipe(), "le méson charmé");
        assertFalse(Hadron.PROTON.isExotic() || Hadron.NEUTRON.isExotic());
        for (Hadron hadron : Hadron.values()) {
            if (hadron != Hadron.PROTON && hadron != Hadron.NEUTRON) assertTrue(hadron.isExotic(), hadron.toString());
            assertFalse(hadron.recipe().containsKey(Quark.TOP), "le top ne se garde pas");
            int quarks = hadron.recipe().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(hadron.formula().split(" ").length, quarks, hadron + " : la formule dit ses quarks");
        }
    }

    @Test
    void sansLaBulleDuMilieuRienNeSePropose() {
        Game game = with();
        assertFalse(game.isHadronsOffered());
        assertFalse(game.isHadronsUnlocked());
        assertEquals(0, game.hadronsBindable(Hadron.PROTON));
    }

    @Test
    void lOngletSeProposeEtSePaieCommeLeVide() {
        Game game = with(TreeBubble.CROWN_MIDDLE);
        assertTrue(game.isHadronsOffered());
        game.tick(1);
        double price = game.hadronsPrice();
        assertTrue(price >= GameHadrons.HADRONS_UNLOCK_COST);
        assertEquals(price, game.state().hadronsPrice(), 0, "le prix se fixe dès qu'il se propose");
        game.state().setColliderGenerators(ColliderGenerator.BEAM, 50);
        game.tick(1);
        assertEquals(price, game.hadronsPrice(), 0, "il ne monte plus avec le collisionneur");
        game.state().setColliderEnergy(price * 0.5);
        assertFalse(game.openHadrons(), "il manque de l'énergie");
        game.state().setColliderEnergy(price + 10);
        assertTrue(game.openHadrons());
        assertEquals(10, game.colliderEnergy(), 1e-6 * price);
        assertTrue(game.isHadronsUnlocked() && !game.isHadronsOffered());
    }

    @Test
    void lesGluonsLientLesQuarks() {
        Game game = with(TreeBubble.YOUNG_LEFT);
        assertTrue(game.isHadronsUnlocked(), "le jeune arbre, à gauche : ouvert dès le début, sans payer");
        game.state().setQuarks(Quark.UP, 7);
        game.state().setQuarks(Quark.DOWN, 2);
        assertEquals(2, game.hadronsBindable(Hadron.PROTON), "le bas manque après deux");
        assertTrue(game.bindHadron(Hadron.PROTON));
        assertEquals(5, game.state().quarks(Quark.UP));
        assertEquals(1, game.state().quarks(Quark.DOWN));
        assertEquals(1, game.hadrons(Hadron.PROTON));
        assertEquals(1, game.bindAllHadrons(Hadron.PROTON));
        assertEquals(0, game.bindAllHadrons(Hadron.NEUTRON), "plus de quark bas");
        assertEquals(2, game.hadronsMade());
        assertFalse(game.isHadronAvailable(Hadron.PION), "les exotiques attendent le cœur de l'arbre-monde");
        assertEquals(0, game.bindAllHadrons(Hadron.PION));
        game.tick(1);
        assertTrue(game.hasTitle(Title.GLUON_WEAVER));
        assertFalse(game.hasTitle(Title.EXOTIC_HUNTER));
    }

    @Test
    void leCoeurOuvreLesExotiques() {
        Game game = with(TreeBubble.YOUNG_LEFT, TreeBubble.HEART);
        assertTrue(game.isExoticHadronsUnlocked());
        for (Hadron hadron : Hadron.values()) assertTrue(game.isHadronAvailable(hadron), hadron.toString());
        assertEquals(10, game.bindAllHadrons(Hadron.OMEGA), "trente quarks étranges, trois chacun");
        assertEquals(0, game.state().quarks(Quark.STRANGE));
        assertEquals(15, game.bindAllHadrons(Hadron.J_PSI));
        game.tick(1);
        assertTrue(game.hasTitle(Title.EXOTIC_HUNTER));
    }

    @Test
    void unNouvelUniversLesReprendSaufAvecLArbreAncien() {
        Game game = with(TreeBubble.CROWN_MIDDLE);
        game.state().setHadronsOpen(true);
        game.bindAllHadrons(Hadron.PROTON);
        assertTrue(game.universeTransition(false).lost().stream().anyMatch(line -> line.startsWith("Les hadrons")), "à la formation");
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertFalse(game.state().hadronsOpen());
        assertEquals(0, game.hadronsMade());
        Game kept = with(TreeBubble.CROWN_MIDDLE, TreeBubble.ANCIENT_LEFT);
        kept.state().setHadronsOpen(true);
        long made = kept.bindAllHadrons(Hadron.PROTON);
        assertTrue(made > 0);
        assertTrue(kept.universeTransition(true).kept().stream().anyMatch(line -> line.startsWith("Les hadrons")));
        kept.state().setReplaying(false);
        assertTrue(kept.startNextUniverse());
        assertTrue(kept.isHadronsUnlocked());
        assertEquals(made, kept.hadrons(Hadron.PROTON));
    }

    @Test
    void lesHadronsSeSauvent() throws Exception {
        Game game = with(TreeBubble.CROWN_MIDDLE);
        game.tick(1);
        game.state().setHadronsOpen(true);
        game.bindAllHadrons(Hadron.NEUTRON);
        GameState back = GameTestSupport.reread(game.state());
        assertTrue(back.hadronsOpen());
        assertEquals(game.state().hadronsPrice(), back.hadronsPrice(), 0);
        assertEquals(15, back.hadrons(Hadron.NEUTRON), "trente quarks bas, deux chacun");
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("hadrons")) old.append(line).append('\n');
        GameState older = SaveCodec.read(old.toString()).state();
        assertFalse(older.hadronsOpen());
        assertEquals(0, older.hadrons(Hadron.NEUTRON));
    }
}
