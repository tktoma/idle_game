package idle.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import idle.ui.atoms.AtomView;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.192 : les skins des icônes d'onglet sous Xvfb. Chaque skin dessine les treize icônes et l'atome, aux deux
 * tailles, éclairées ou sourdes, sans exception ; chaque icône a un modèle avec des points ; « Figées » arrête le temps
 * et le réglage coupé aussi ; le pixel art ramène la couleur à huit valeurs ; sans skin, tout tourne comme avant.
 */
class TabIconSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @AfterEach
    void classic() {
        TabIcon.setSkin(null);
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    @Test
    void chaqueSkinDessineChaqueIconeAuxDeuxTailles() {
        FxTestSupport.fx(() -> {
            List<Skin> skins = skinsOf(SkinVisual.TAB_ICONS);
            assertEquals(5, skins.size());
            for (Skin skin : skins) {
                TabIcon.setSkin(skin);
                assertEquals(skin, TabIcon.skin());
                assertEquals(skin == Skin.ICONS_STILL, TabIcon.frozen(), skin.name());
                for (TabIcon.Kind kind : TabIcon.Kind.values()) {
                    for (double size : new double[] {18, 30}) {
                        TabIcon icon = new TabIcon(kind, "#9fd0ff", size);
                        icon.setBright(true);
                        for (int k = 0; k < 8; k++) icon.frame(0.3, true);
                        icon.setBright(false);
                        icon.frame(0.3, false);
                        assertEquals(skin == Skin.ICONS_STILL ? 0 : 2.4, icon.time(), 1e-9, skin.name() + " " + kind);
                    }
                }
                for (double size : new double[] {22, 40}) {
                    AtomView atom = new AtomView(size);
                    for (int k = 0; k < 8; k++) atom.frame(0.05);
                }
            }
            // Un skin d'un autre visuel ne dessine rien de spécial, et ne fige pas.
            TabIcon.setSkin(Skin.CARD_INDEX);
            assertFalse(TabIconSkinArt.draws(Skin.CARD_INDEX));
            assertFalse(TabIcon.frozen());
            TabIcon icon = new TabIcon(TabIcon.Kind.PARTICLES, "#9fd0ff", 22);
            icon.frame(0.5, true);
            assertEquals(0.5, icon.time(), 1e-9);
            TabIcon.setSkin(null);
            assertNull(TabIcon.skin());
            return null;
        });
    }

    @Test
    void chaqueIconeAUnModeleAvecDesPointsQuiBougent() {
        for (TabIcon.Kind kind : TabIcon.Kind.values()) {
            TabIconSkinArt.Model now = TabIconSkinArt.model(kind, 0);
            TabIconSkinArt.Model later = TabIconSkinArt.model(kind, 1.3);
            // La paire du vide s'ouvre et se referme : à 0 s comme à 1,3 s, elle est fermée ; elle est ouverte entre les deux.
            TabIconSkinArt.Model between = TabIconSkinArt.model(kind, 0.4);
            assertTrue(now.points() >= 2, kind.name());
            assertEquals(now.points(), later.points(), kind.name());
            boolean moved = false;
            for (int p = 0; p < now.points(); p++) {
                assertTrue(Math.abs(now.x(p)) <= 0.5 && Math.abs(now.y(p)) <= 0.5, kind + " point " + p + " dans l'icône");
                if (Math.abs(now.x(p) - later.x(p)) > 1e-6 || Math.abs(now.y(p) - later.y(p)) > 1e-6) moved = true;
                if (Math.abs(now.x(p) - between.x(p)) > 1e-6 || Math.abs(now.y(p) - between.y(p)) > 1e-6) moved = true;
            }
            assertTrue(moved, kind + " : quelque chose bouge");
            // Toutes les icônes sauf le vide et le profil ont au moins un trait.
            if (kind != TabIcon.Kind.VOID) assertTrue(now.linked() >= 1, kind.name());
        }
        assertEquals(28, AtomView.model(0).points(), "le noyau, trois anneaux de huit, trois électrons");
        assertEquals(24, AtomView.model(0).linked());
    }

    @Test
    void lePixelArtRameneLaCouleurAHuitValeurs() {
        Color eight = TabIconSkinArt.eightLevels(Color.web("#9fd0ff"));
        for (double channel : new double[] {eight.getRed(), eight.getGreen(), eight.getBlue()}) {
            assertEquals(Math.round(channel * 7), channel * 7, 1e-6, "un multiple de 1/7 (la couleur se garde en simple précision)");
        }
        assertNotEquals(Color.web("#9fd0ff"), eight);
        assertEquals(Color.WHITE, TabIconSkinArt.eightLevels(Color.WHITE));
        assertEquals(12, TabIconSkinArt.PIXEL_GRID);
    }
}
