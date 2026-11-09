package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.197 : l'arbre au-delà de ses essences, sous Xvfb. Chaque saison figée, chaque fantaisie et chaque sol se
 * dessine à chaque palier, du bonsaï à l'arbre-monde, avec chaque essence, sans exception, le feuillage dans la vue et le
 * coût borné ; chacun change le dessin par rapport au Classique, et l'arbre inversé porte son feuillage sous le milieu
 * de sa hauteur. Les bulles de l'arbre et leurs liens se dessinent sous chaque skin, dans les quatre états, et changent
 * le dessin. Sans skin, {@link TreeSkinArt.Look#CLASSIC}.
 */
class TreeLooksSkinsTest {

    private static final double W = 800, H = 600, BASE_X = 400, BASE_Y = 560;
    private static final int MAX_TIPS = 400;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static int differing(Canvas a, Canvas b) {
        WritableImage first = a.snapshot(null, null);
        WritableImage second = b.snapshot(null, null);
        int count = 0;
        for (int x = 0; x < (int) a.getWidth(); x += 3) {
            for (int y = 0; y < (int) a.getHeight(); y += 3) {
                if (first.getPixelReader().getArgb(x, y) != second.getPixelReader().getArgb(x, y)) count++;
            }
        }
        return count;
    }

    /** Un arbre immobile du palier {@code stage}, de l'essence et des skins donnés, sur un fond sombre. */
    private static Canvas tree(int stage, Skin essence, Skin season, Skin fantasy, Skin ground) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(javafx.scene.paint.Color.web("#0a0c16"));
        g.fillRect(0, 0, W, H);
        SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 0, essence, season, fantasy, ground);
        return canvas;
    }

    @Test
    void chaqueSaisonFantaisieEtSolSeDessineAChaquePalierAvecChaqueEssence() {
        FxTestSupport.fx(() -> {
            GraphicsContext g = new Canvas(W, H).getGraphicsContext2D();
            List<Skin> seasons = skinsOf(SkinVisual.TREE_SEASON), fantasies = skinsOf(SkinVisual.TREE_FANTASY), grounds = skinsOf(SkinVisual.TREE_GROUND);
            assertEquals(3, seasons.size());
            assertEquals(6, fantasies.size());
            assertEquals(6, grounds.size());
            List<Skin> essences = new ArrayList<>(List.of(Skin.TREES));
            essences.add(null);
            for (Skin essence : essences) {
                for (int stage = 5; stage <= 11; stage++) {
                    for (int k = 0; k < 6; k++) {
                        Skin season = seasons.get(k % 3), fantasy = fantasies.get(k), ground = grounds.get(k);
                        for (double growth : new double[] {0, 1}) {
                            double[] crown = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, growth, 2.3, essence, season, fantasy, ground);
                            String what = essence + ", palier " + stage + ", " + season + ", " + fantasy + ", " + ground;
                            assertTrue(crown[0] > 0 && crown[0] < W, what + " : le centre du feuillage dans la largeur (" + crown[0] + ")");
                            assertTrue(crown[1] > 0 && crown[1] < H, what + " : dans la hauteur (" + crown[1] + ")");
                            assertTrue(crown[2] > 0, what + " : un rayon");
                            assertTrue(SeedTree.lastTips > 0 && SeedTree.lastTips <= MAX_TIPS, what + " : " + SeedTree.lastTips + " touffes");
                        }
                    }
                }
            }
            return null;
        });
    }

    @Test
    void lArbreInverseAUnFeuillageEnBasEtLesAutresEnHaut() {
        FxTestSupport.fx(() -> {
            GraphicsContext g = new Canvas(W, H).getGraphicsContext2D();
            for (int stage = 7; stage <= 11; stage++) {
                double height = H * new double[] {0.36, 0.40, 0.50, 0.51, 0.54, 0.56, 0.58}[stage - 5];
                double classic = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 0, null, null, null, null)[1];
                double inverted = SeedTree.draw(g, W, H, BASE_X, BASE_Y, stage, 1, 0, null, null, Skin.TREE_INVERTED, null)[1];
                assertTrue(classic <= BASE_Y - height * 0.45, "palier " + stage + " : le feuillage Classique dans le haut de l'arbre (" + classic + ")");
                assertTrue(inverted >= BASE_Y - height * 0.55, "palier " + stage + " : le feuillage inversé dans le bas (" + inverted + ")");
                assertEquals(classic, 2 * (BASE_Y - height * 0.5) - inverted, 1e-6, "le même arbre, retourné autour du milieu de sa hauteur");
            }
            return null;
        });
    }

    @Test
    void chaqueSkinDeLArbreChangeLeDessinEtLeClassiqueNeBougePas() {
        FxTestSupport.fx(() -> {
            for (int stage : new int[] {5, 8, 11}) {
                Canvas classic = tree(stage, null, null, null, null);
                assertEquals(0, differing(classic, tree(stage, null, null, null, null)), "deux Classiques identiques");
                for (Skin skin : skinsOf(SkinVisual.TREE_SEASON)) {
                    assertTrue(differing(classic, tree(stage, null, skin, null, null)) > 30, skin.label() + " au palier " + stage);
                }
                for (Skin skin : skinsOf(SkinVisual.TREE_FANTASY)) {
                    assertTrue(differing(classic, tree(stage, null, null, skin, null)) > 30, skin.label() + " au palier " + stage);
                }
                for (Skin skin : skinsOf(SkinVisual.TREE_GROUND)) {
                    assertTrue(differing(classic, tree(stage, null, null, null, skin)) > 10, skin.label() + " au palier " + stage);
                }
                // L'essence et la fantaisie se combinent : le bouleau de cristal n'est ni le bouleau ni le cristal.
                Canvas birch = tree(stage, Skin.TREE_BIRCH, null, null, null), crystal = tree(stage, null, null, Skin.TREE_CRYSTAL, null);
                Canvas both = tree(stage, Skin.TREE_BIRCH, null, Skin.TREE_CRYSTAL, null);
                assertTrue(differing(birch, both) > 30 && differing(crystal, both) > 30, "bouleau de cristal au palier " + stage);
                // L'hiver figé reste l'hiver sur un sol de sable : la neige au pied change le dessin du sable seul.
                assertTrue(differing(tree(stage, null, null, null, Skin.GROUND_SAND), tree(stage, null, Skin.TREE_WINTER, null, Skin.GROUND_SAND)) > 30,
                        "hiver sur le sable au palier " + stage);
            }
            return null;
        });
    }

    @Test
    void sansSkinLeRegardEstLeClassique() {
        assertSame(TreeSkinArt.Look.CLASSIC, TreeSkinArt.Look.of(null, null, null));
        assertSame(TreeSkinArt.Look.CLASSIC, TreeSkinArt.Look.of(Skin.TREE_BIRCH, Skin.BUBBLES_FRUITS, Skin.MAP_DIAL), "d'autres visuels : le Classique");
        TreeSkinArt.Look look = TreeSkinArt.Look.of(Skin.TREE_WINTER, Skin.TREE_PIXEL, Skin.GROUND_WATER);
        assertEquals(TreeSkinArt.Season.WINTER, look.season());
        assertEquals(TreeSkinArt.Fantasy.PIXEL, look.fantasy());
        assertEquals(TreeSkinArt.Ground.WATER, look.ground());
        assertTrue(TreeSkinArt.Look.of(Skin.TREE_WINTER, null, null).bare(), "l'hiver sans fantaisie : l'arbre nu");
        assertTrue(!look.bare(), "l'hiver avec une fantaisie garde le feuillage de la fantaisie");
        assertTrue(TreeSkinArt.Look.of(Skin.TREE_SPRING, null, null).blossoms());
    }

    /** Une vue immobile des bulles de l'arbre dans les quatre états, habillée des skins donnés. */
    private static SeedView bubbles(double width, Skin bubbleSkin, Skin linkSkin) {
        SeedView view = new SeedView();
        view.setWidth(width);
        view.setHeight(420);
        view.setSeeds(3);
        view.setSprout(true, 40);
        view.setStage(8);
        List<SeedView.Bubble> shown = new ArrayList<>();
        SeedView.BubbleState[] states = {SeedView.BubbleState.BOUGHT, SeedView.BubbleState.BOUGHT, SeedView.BubbleState.BUY, SeedView.BubbleState.SHORT,
            SeedView.BubbleState.CLOSED, SeedView.BubbleState.CLOSED, SeedView.BubbleState.CLOSED, SeedView.BubbleState.CLOSED};
        for (int place = 0; place < states.length; place++) {
            List<Integer> below = place == 0 ? List.of() : List.of(Math.max(0, place - 1));
            shown.add(new SeedView.Bubble(place, "Bulle " + place, "Un effet", "1 000", states[place], below));
        }
        view.setBubbles(shown);
        if (bubbleSkin != null) view.setVisualSkin(SkinVisual.TREE_BUBBLES, bubbleSkin);
        if (linkSkin != null) view.setVisualSkin(SkinVisual.TREE_BUBBLE_LINKS, linkSkin);
        for (int k = 0; k < 3; k++) view.frame(0.1, false);
        return view;
    }

    @Test
    void lesBullesEtLeursLiensSeDessinentSousChaqueSkinEtChangentLeDessin() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.TREE_BUBBLES).size());
            assertEquals(3, skinsOf(SkinVisual.TREE_BUBBLE_LINKS).size());
            for (double width : new double[] {520, 1100}) {
                SeedView classic = bubbles(width, null, null);
                assertEquals(8, classic.bubbleCount());
                assertEquals(0, differing(classic, bubbles(width, null, null)), "deux Classiques identiques");
                for (Skin skin : skinsOf(SkinVisual.TREE_BUBBLES)) {
                    SeedView skinned = bubbles(width, skin, null);
                    assertTrue(differing(classic, skinned) > 20, skin.label() + " à " + width);
                    // Les bulles restent là où elles sont : le survol et le clic ne changent pas.
                    double[] at = skinned.bubblePoint(2);
                    assertEquals(2, skinned.bubbleAt(at[0], at[1]));
                    for (int k = 0; k < 5; k++) skinned.frame(0.1, true);
                }
                for (Skin skin : skinsOf(SkinVisual.TREE_BUBBLE_LINKS)) {
                    SeedView skinned = bubbles(width, null, skin);
                    assertTrue(differing(classic, skinned) > 5, skin.label() + " à " + width);
                    for (int k = 0; k < 5; k++) skinned.frame(0.1, true);
                }
                SeedView both = bubbles(width, Skin.BUBBLES_LANTERNS, Skin.BUBBLE_LINKS_ROOTS);
                assertNotEquals(0, differing(bubbles(width, Skin.BUBBLES_LANTERNS, null), both), "les liens s'ajoutent aux bulles");
            }
            assertTrue(TreeBubbleSkinArt.bubbles(Skin.BUBBLES_GEMS) && !TreeBubbleSkinArt.bubbles(Skin.BUBBLE_LINKS_LIGHT) && !TreeBubbleSkinArt.bubbles(null));
            assertTrue(TreeBubbleSkinArt.links(Skin.BUBBLE_LINKS_LIGHT) && !TreeBubbleSkinArt.links(Skin.BUBBLES_GEMS));
            assertEquals(5, SeedView.TREE_LOOKS.length);
            return null;
        });
    }
}
