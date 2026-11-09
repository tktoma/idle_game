package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse;
import idle.core.profile.Title;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Version 0.198 : les styles des avatars ({@link SkinVisual#AVATAR_STYLE}) et les huit avatars de plus, sous Xvfb. Chaque
 * style redessine chacun des vingt avatars aux deux tailles du profil, dans chaque cadre, sans exception et autrement que
 * le Classique ; une image importée reste la même ; le profil met le style sur l'en-tête et la galerie ; les nouveaux
 * avatars s'ouvrent par leur titre ou leur condition.
 */
class AvatarStyleTest {

    @TempDir
    Path data;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> styles() {
        List<Skin> styles = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == SkinVisual.AVATAR_STYLE) styles.add(skin);
        return styles;
    }

    private static boolean same(WritableImage a, WritableImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < (int) a.getHeight(); y++) {
            for (int x = 0; x < (int) a.getWidth(); x++) if (a.getPixelReader().getArgb(x, y) != b.getPixelReader().getArgb(x, y)) return false;
        }
        return true;
    }

    @Test
    void chaqueStyleRedessineChaqueAvatarAutrementQueLeClassique() {
        FxTestSupport.fx(() -> {
            assertEquals(4, styles().size());
            assertEquals(20, Avatar.values().length, "douze avatars d'avant, huit de la 0.198");
            for (double size : new double[] {96, 56}) {
                AvatarView plain = new AvatarView(size);
                AvatarView styled = new AvatarView(size);
                for (Avatar avatar : Avatar.values()) {
                    plain.show(avatar);
                    WritableImage classic = plain.snapshot(null, null);
                    for (Skin style : styles()) {
                        styled.setAvatarStyle(style);
                        styled.show(avatar);
                        assertEquals(style, styled.avatarStyle());
                        assertEquals(avatar.id(), styled.shown());
                        assertFalse(same(classic, styled.snapshot(null, null)), style + " sur " + avatar + " à " + size);
                    }
                    styled.setAvatarStyle(null);
                    assertNull(styled.avatarStyle());
                    assertTrue(same(classic, styled.snapshot(null, null)), "le Classique revient : " + avatar + " à " + size);
                }
                // Dans chaque cadre aussi, et sur l'image importée, rien ne change.
                WritableImage imported = new WritableImage(10, 10);
                for (Skin frame : Skin.values()) {
                    if (frame.visual() != SkinVisual.AVATAR_FRAME) continue;
                    styled.setFrame(frame);
                    for (Skin style : styles()) {
                        styled.setAvatarStyle(style);
                        for (Avatar avatar : Avatar.values()) styled.show(avatar);
                    }
                }
                styled.setFrame(null);
                plain.show(imported);
                WritableImage picture = plain.snapshot(null, null);
                for (Skin style : styles()) {
                    styled.setAvatarStyle(style);
                    styled.show(imported);
                    assertTrue(same(picture, styled.snapshot(null, null)), "l'image importée n'est pas retouchée : " + style);
                }
                styled.setAvatarStyle(Skin.FUSION_FLASH);
                assertNull(styled.avatarStyle(), "un skin d'un autre visuel ne fait pas un style");
            }
            return null;
        });
    }

    @Test
    void leProfilMetLeStyleSurLEnTeteEtLaGalerie() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.tick(1.5);
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertNull(page.avatarStyle(), "sans skin : le Classique");
            game.testAllSkins(true);
            for (Skin style : styles()) {
                assertTrue(game.chooseSkin(SkinVisual.AVATAR_STYLE, style));
                page.refresh();
                assertEquals(style, page.avatarStyle(), "l'en-tête et toute la galerie");
            }
            assertTrue(game.chooseSkin(SkinVisual.AVATAR_STYLE, null));
            page.refresh();
            assertNull(page.avatarStyle());
            return null;
        });
    }

    @Test
    void lesNouveauxAvatarsSOuvrentParLeurTitreOuLeurCondition() {
        Game game = new Game();
        game.start();
        for (Avatar avatar : Avatar.values()) {
            if (avatar == Avatar.PARTICLE) {
                assertTrue(avatar.how().isEmpty() && avatar.condition().isEmpty());
                continue;
            }
            assertFalse(avatar.how().isEmpty(), avatar + " dit comment il s'ouvre");
            assertFalse(avatar.condition().isEmpty(), avatar + " dit ce qu'il demande");
            if (avatar.unlock() != null) assertEquals("avec le titre « " + avatar.unlock().label() + " »", avatar.how());
        }
        assertEquals(Title.GLUON_WEAVER, Avatar.HADRON.unlock());
        assertEquals(Title.UNIVERSE_CREATOR, Avatar.COSMIC_WEB.unlock());
        assertEquals(Title.MULTIVERSE_WEAVER, Avatar.BUBBLE.unlock());
        assertEquals(Title.MIRROR_KEEPER, Avatar.MIRROR.unlock());
        assertEquals(Title.MIX_MASTER, Avatar.KALEIDOSCOPE.unlock());
        // Sans titre : la sonde, l'arrosoir et le chrono ont leur condition à eux.
        assertNull(Avatar.PROBE.unlock());
        assertFalse(Avatar.PROBE.isOpen(game));
        game.state().setProbeRuns(10);
        assertTrue(Avatar.PROBE.isOpen(game));
        assertFalse(Avatar.WATERING_CAN.isOpen(game));
        game.state().setSeedUpgradeLevel(GameUniverse.SeedUpgrade.HOSE.ordinal(), 1);
        assertTrue(game.hasHose());
        assertTrue(Avatar.WATERING_CAN.isOpen(game));
        assertFalse(Avatar.CHRONO.isOpen(game));
        game.state().setSeedUpgradeLevel(GameUniverse.SeedUpgrade.CHRONO.ordinal(), 1);
        assertTrue(game.isChronoUnlocked());
        assertTrue(Avatar.CHRONO.isOpen(game));
        assertEquals("avec le chrono", Avatar.CHRONO.how());
        assertEquals("probe", Avatar.PROBE.id());
        assertEquals(Avatar.PROBE, Avatar.of("probe"));
        assertNotEquals(Avatar.PROBE.color(), Avatar.HADRON.color());
    }
}
