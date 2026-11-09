package idle.ui.darkmatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.193 : les skins de la matière noire hors de la boule, sous Xvfb. L'échelle des grandeurs dessine ses
 * trois skins du proton à l'univers observable, à deux tailles ; l'arbre pose ses vignettes et change ses traits
 * sous ses quatre skins, à deux largeurs et à plusieurs niveaux ; les défis posent leurs vignettes sous leurs trois
 * skins, avant et après une réussite, et les lames du tarot se retournent à l'arrivée de la fuite. Le Classique ne
 * garde rien de tout cela.
 */
class DarkPanesSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Un jeu après une première explosion, tous les skins ouverts. */
    private static Game game() {
        Game game = new Game(42L);
        game.start();
        game.state().setExplosions(1);
        game.testAllSkins(true);
        assertTrue(game.isDarkMatterUnlocked());
        return game;
    }

    private static final BigNum[] SIZES = {BigNum.of(1, -15), BigNum.of(1.27, 7), BigNum.of(1.39, 9), BigNum.of(8.8, 26)};

    @Test
    void lEchelleDesGrandeursSeDessineSousSesTroisSkins() {
        FxTestSupport.fx(() -> {
            List<Skin> rulers = skinsOf(SkinVisual.DARK_RULER);
            assertEquals(3, rulers.size());
            for (Skin ruler : rulers) {
                DarkMatterView view = new DarkMatterView(400, 300);
                view.setRuler(ruler);
                assertEquals(ruler, view.ruler());
                for (double size : new double[] {420, 160}) {
                    view.setWidth(size);
                    view.setHeight(size * 0.7);
                    for (BigNum matter : SIZES) {
                        // La caméra recule peu à peu : les repères défilent, les silhouettes avec eux.
                        for (int k = 0; k < 12; k++) view.frame(0.05, matter, false, 0);
                        for (int k = 0; k < 6; k++) view.frame(0.05, matter, true, 50);
                    }
                }
                // Une fenêtre large et basse, où les textes passent à droite ; puis trop petite pour dessiner.
                view.setWidth(900);
                view.setHeight(120);
                for (int k = 0; k < 6; k++) view.frame(0.05, SIZES[2], false, 0);
                view.setWidth(20);
                view.setHeight(20);
                view.frame(0.05, SIZES[1], false, 0);
            }
            // Un skin d'un autre visuel ramène au classique.
            DarkMatterView view = new DarkMatterView(400, 300);
            view.setRuler(Skin.DARK_TREE_ROOTS);
            assertNull(view.ruler());
            view.frame(0.05, SIZES[1], false, 0);
            return null;
        });
    }

    @Test
    void lArbrePoseSesVignettesEtChangeSesTraitsSousSesQuatreSkins() {
        FxTestSupport.fx(() -> {
            Game game = game();
            DarkMatterPage page = new DarkMatterPage(game);
            DarkMatterTreePane tree = page.treePane();
            page.step(1);
            assertEquals(1, page.selected());
            List<DarkUpgrade> cells = new ArrayList<>();
            for (DarkUpgrade dark : game.darkUpgrades()) if (!dark.isLink()) cells.add(dark);
            String first = cells.get(0).id();
            String link = game.darkLinks().get(0).id();
            // Le Classique : traits de deux pixels, pas de vignette.
            page.refresh();
            assertNull(tree.skin());
            assertEquals(2, tree.linkWidth(first), 1e-9);
            assertFalse(tree.hasArt(first) || tree.hasArt(link));
            tree.resize(720, 600);
            tree.layout();
            List<Skin> skins = skinsOf(SkinVisual.DARK_TREE);
            assertEquals(4, skins.size());
            for (Skin skin : skins) {
                assertTrue(game.chooseSkin(SkinVisual.DARK_TREE, skin));
                page.refresh();
                assertEquals(skin, tree.skin());
                assertTrue(tree.hasArt(first), "une vignette sur la case : " + skin);
                assertTrue(tree.hasArt(link), "et sur la liaison : " + skin);
                double width = tree.linkWidth(first);
                assertTrue(width != 2 || skin == Skin.DARK_TREE_ROOTS, "le trait change : " + skin + " " + width);
                // Des niveaux achetés : les racines s'épaississent, les étoiles brillent, les stations se remplissent.
                for (int k = 0; k < cells.size(); k += 2) game.state().setDarkLevel(cells.get(k).id(), 1 + k % 5);
                page.refresh();
                if (skin == Skin.DARK_TREE_ROOTS) assertTrue(tree.linkWidth(first) > 2, "une racine épaissie par le niveau");
                for (double w : new double[] {900, 480}) {
                    tree.resize(w, 600);
                    tree.layout();
                }
                page.frame(0.05, 1);
                for (DarkUpgrade dark : cells) game.state().setDarkLevel(dark.id(), 0);
            }
            assertEquals(6, tree.linkWidth(first), 1e-9, "le métro : six pixels");
            // Classique de nouveau : tout s'en va.
            assertTrue(game.chooseSkin(SkinVisual.DARK_TREE, null));
            page.refresh();
            assertNull(tree.skin());
            assertFalse(tree.hasArt(first) || tree.hasArt(link));
            assertEquals(2, tree.linkWidth(first), 1e-9);
            tree.layout();
            return null;
        });
    }

    @Test
    void lesDefisPosentLeursVignettesEtLeTarotSeRetourneALArrivee() {
        FxTestSupport.fx(() -> {
            Game game = game();
            DarkMatterPage page = new DarkMatterPage(game);
            ChallengesPane challenges = page.challengesPane();
            page.showChallenges();
            assertEquals(2, page.selected());
            Challenge firstChallenge = Challenges.DEFAULT.get(0);
            page.refresh();
            assertNull(challenges.skin());
            assertFalse(challenges.hasArt(firstChallenge) || challenges.hasArt(Challenges.LEAK));
            List<Skin> skins = skinsOf(SkinVisual.CHALLENGES);
            assertEquals(3, skins.size());
            int done = 0;
            for (Skin skin : skins) {
                assertTrue(game.chooseSkin(SkinVisual.CHALLENGES, skin));
                page.refresh();
                assertEquals(skin, challenges.skin());
                assertTrue(challenges.hasArt(firstChallenge), "une vignette sur le défi : " + skin);
                assertTrue(challenges.hasArt(Challenges.LEAK), "et sur la fuite : " + skin);
                for (double w : new double[] {820, 400}) {
                    challenges.resize(w, 600);
                    challenges.layout();
                }
                // Trois défis réussis de plus : la lame à l'endroit, le tampon, le sceau.
                for (int i = 0; i < 3; i++) game.state().addCompletedChallenge(Challenges.DEFAULT.get(done++).id());
                page.refresh();
                challenges.layout();
                // L'arrivée de la fuite, jusqu'au bout : les lames se retournent sous le tarot.
                page.playLeakReveal();
                assertTrue(page.isRevealing());
                boolean flipped = false;
                for (int k = 0; k < 80 && page.isRevealing(); k++) {
                    page.frame(0.05, 1);
                    flipped |= challenges.card(firstChallenge).getScaleX() < 0.9;
                }
                assertFalse(page.isRevealing());
                assertEquals(skin == Skin.CHALLENGES_TAROT, flipped, "seules les lames se retournent : " + skin);
                assertEquals(1, challenges.card(firstChallenge).getScaleX(), 1e-9);
            }
            assertEquals(9, done);
            assertTrue(game.chooseSkin(SkinVisual.CHALLENGES, null));
            page.refresh();
            assertNull(challenges.skin());
            assertFalse(challenges.hasArt(firstChallenge) || challenges.hasArt(Challenges.LEAK));
            return null;
        });
    }
}
