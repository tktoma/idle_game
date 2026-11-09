package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.nio.file.Path;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Version 0.189 : les cadres de l'avatar ({@link SkinVisual#AVATAR_FRAME}) sous Xvfb. Chaque cadre se dessine autour de
 * chaque avatar du jeu et d'une image importée, aux deux tailles du profil ; le Classique revient avec {@code null} ; et
 * l'onglet Profil met le cadre choisi sur l'avatar de l'en-tête et l'aperçu de l'image, pas sur la galerie.
 */
class AvatarFrameTest {

    @TempDir
    Path data;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Les six cadres, dans l'ordre du catalogue. */
    private static java.util.List<Skin> frames() {
        java.util.List<Skin> frames = new java.util.ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == SkinVisual.AVATAR_FRAME) frames.add(skin);
        return frames;
    }

    @Test
    void chaqueCadreSeDessineAutourDeChaqueAvatarEtDUneImage() {
        FxTestSupport.fx(() -> {
            assertEquals(6, frames().size());
            WritableImage imported = new WritableImage(10, 10);
            for (double size : new double[] {96, 56}) {
                AvatarView view = new AvatarView(size);
                for (Skin frame : frames()) {
                    view.setFrame(frame);
                    view.setStampValue(7);
                    assertEquals(frame, view.frame());
                    for (Avatar avatar : Avatar.values()) {
                        view.show(avatar);
                        assertEquals(avatar.id(), view.shown());
                    }
                    view.show(imported);
                    assertEquals(Avatar.CUSTOM, view.shown());
                    view.setStampValue(8);
                }
                view.setFrame(null);
                assertNull(view.frame(), "null : le Classique");
                view.setFrame(Skin.FUSION_FLASH);
                assertNull(view.frame(), "un skin d'un autre visuel ne fait pas un cadre");
                assertEquals(Avatar.CUSTOM, view.shown(), "le cadre ne change pas ce qui est montré");
            }
            return null;
        });
    }

    @Test
    void leProfilMetLeCadreChoisiSurLEnTeteEtLApercu() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.tick(1.5);
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertNull(page.avatarFrame(), "sans skin : le Classique");
            assertNull(page.previewFrame());
            game.testAllSkins(true);
            for (Skin frame : frames()) {
                assertTrue(game.chooseSkin(SkinVisual.AVATAR_FRAME, frame));
                page.refresh();
                assertEquals(frame, page.avatarFrame(), frame.label());
                assertEquals(frame, page.previewFrame(), frame.label());
                assertEquals("particle", page.shownAvatar());
            }
            assertTrue(game.chooseSkin(SkinVisual.AVATAR_FRAME, null));
            page.refresh();
            assertNull(page.avatarFrame(), "retour au Classique");
            game.chooseSkin(SkinVisual.AVATAR_FRAME, Skin.FRAME_STAMP);
            game.testAllSkins(false);
            page.refresh();
            assertNull(page.avatarFrame(), "un cadre reverrouillé ne se porte plus");
            return null;
        });
    }
}
