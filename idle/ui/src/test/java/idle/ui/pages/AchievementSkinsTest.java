package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.achievements.Achievement;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.198 : les skins des tuiles de succès ({@link SkinVisual#ACHIEVEMENT_TILE}) et des succès non obtenus
 * ({@link SkinVisual#ACHIEVEMENT_LOCKED}), sous Xvfb. Chaque insigne se dessine, aux trois paliers, et deux insignes
 * différents ne se ressemblent pas ; la page des succès met l'insigne du bon skin selon que le succès est obtenu ou
 * non, le refait quand le skin ou l'état change, et n'en a aucun dans le Classique.
 */
class AchievementSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static WritableImage image(Node node) {
        return ((Canvas) node).snapshot(null, null);
    }

    private static boolean same(WritableImage a, WritableImage b) {
        for (int y = 0; y < (int) a.getHeight(); y++) {
            for (int x = 0; x < (int) a.getWidth(); x++) if (a.getPixelReader().getArgb(x, y) != b.getPixelReader().getArgb(x, y)) return false;
        }
        return true;
    }

    @Test
    void chaqueInsigneSeDessineEtSeDistingueDesAutres() {
        FxTestSupport.fx(() -> {
            assertEquals(5, skinsOf(SkinVisual.ACHIEVEMENT_TILE).size());
            assertEquals(3, skinsOf(SkinVisual.ACHIEVEMENT_LOCKED).size());
            assertNull(AchievementSkinArt.icon(null, null, true, 0, "#ffd27f"), "le Classique n'a pas d'insigne");
            assertNull(AchievementSkinArt.icon(null, Skin.LOCKED_PUZZLE, true, 0, "#ffd27f"), "obtenu : le skin des succès non obtenus ne compte pas");
            assertNull(AchievementSkinArt.icon(Skin.TILES_MEDALS, null, false, 0, "#ffd27f"), "non obtenu : le skin des tuiles ne compte pas");
            assertNull(AchievementSkinArt.icon(Skin.FUSION_FLASH, Skin.FUSION_FLASH, true, 0, "#ffd27f"), "un skin d'un autre visuel");
            List<WritableImage> images = new ArrayList<>();
            for (Skin tiles : skinsOf(SkinVisual.ACHIEVEMENT_TILE)) {
                for (int tier = 0; tier < 3; tier++) {
                    Node icon = AchievementSkinArt.icon(tiles, null, true, tier, "#ffd27f");
                    assertNotNull(icon, tiles + " palier " + tier);
                    images.add(image(icon));
                }
            }
            for (Skin locked : skinsOf(SkinVisual.ACHIEVEMENT_LOCKED)) {
                Node icon = AchievementSkinArt.icon(null, locked, false, 0, "#ffd27f");
                assertNotNull(icon, locked.label());
                images.add(image(icon));
            }
            // Les médailles, les étoiles et les trophées changent de métal avec le palier ; chaque skin se distingue des autres.
            for (Skin metal : List.of(Skin.TILES_MEDALS, Skin.TILES_STARS, Skin.TILES_TROPHIES)) {
                int at = skinsOf(SkinVisual.ACHIEVEMENT_TILE).indexOf(metal) * 3;
                assertFalse(same(images.get(at), images.get(at + 2)), metal + " : bronze et or");
            }
            List<WritableImage> distinct = new ArrayList<>();
            for (int i = 0; i < 15; i += 3) distinct.add(images.get(i));
            distinct.addAll(images.subList(15, images.size()));
            assertEquals(8, distinct.size());
            for (int i = 0; i < distinct.size(); i++) {
                for (int j = i + 1; j < distinct.size(); j++) assertFalse(same(distinct.get(i), distinct.get(j)), i + " et " + j);
            }
            assertEquals(0, AchievementSkinArt.tier(1, 93));
            assertEquals(1, AchievementSkinArt.tier(40, 93));
            assertEquals(2, AchievementSkinArt.tier(93, 93));
            assertEquals(2, AchievementSkinArt.tier(19, 19));
            return null;
        });
    }

    @Test
    void laPageMetLInsigneDuBonSkinEtLeRefaitQuandIlChange() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.state().setExplosions(1);
            game.state().setBigBangs(1);
            List<Achievement> catalog = new ArrayList<>(game.achievements());
            for (int i = 0; i < 12; i++) game.state().addAchievement(catalog.get(i).id());
            AchievementsPage page = new AchievementsPage(game);
            page.refresh();
            assertNull(page.tileSkin());
            assertNull(page.lockedSkin());
            for (Achievement achievement : catalog) assertFalse(page.hasIcon(page.tile(achievement)), "Classique : " + achievement.id());
            game.testAllSkins(true);
            for (Skin tiles : skinsOf(SkinVisual.ACHIEVEMENT_TILE)) {
                assertTrue(game.chooseSkin(SkinVisual.ACHIEVEMENT_TILE, tiles));
                page.refresh();
                assertEquals(tiles, page.tileSkin());
                assertTrue(page.hasIcon(page.tile(catalog.get(0))), tiles + " : un succès obtenu porte l'insigne");
                assertFalse(page.hasIcon(page.tile(catalog.get(catalog.size() - 1))), tiles + " : un succès non obtenu n'en a pas sans skin des non obtenus");
            }
            for (Skin locked : skinsOf(SkinVisual.ACHIEVEMENT_LOCKED)) {
                assertTrue(game.chooseSkin(SkinVisual.ACHIEVEMENT_LOCKED, locked));
                page.refresh();
                assertEquals(locked, page.lockedSkin());
                for (Achievement achievement : catalog) assertTrue(page.hasIcon(page.tile(achievement)), locked + " : " + achievement.id());
            }
            // Un succès qui s'obtient change d'insigne ; le Classique les retire tous.
            Achievement next = catalog.get(12);
            game.state().addAchievement(next.id());
            page.refresh();
            assertTrue(page.hasIcon(page.tile(next)));
            assertTrue(game.chooseSkin(SkinVisual.ACHIEVEMENT_TILE, null));
            page.refresh();
            assertFalse(page.hasIcon(page.tile(next)), "obtenu, sans skin des tuiles : plus d'insigne");
            assertTrue(page.hasIcon(page.tile(catalog.get(catalog.size() - 1))), "non obtenu : l'insigne des non obtenus reste");
            assertTrue(game.chooseSkin(SkinVisual.ACHIEVEMENT_LOCKED, null));
            page.refresh();
            for (Achievement achievement : catalog) assertFalse(page.hasIcon(page.tile(achievement)), "Classique de nouveau : " + achievement.id());
            return null;
        });
    }
}
