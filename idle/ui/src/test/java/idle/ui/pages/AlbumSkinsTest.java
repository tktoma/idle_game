package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import idle.ui.bigbang.CatalogArt;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Version 0.198 : les skins des albums ({@link SkinVisual#ALBUM}), sous Xvfb. Les dessins du catalogue se font pour
 * chaque molécule, astre et assemblage, à deux tailles et dans chaque skin de leur visuel ; chaque case trouvée ou non
 * se dessine dans chaque skin des albums, autrement que vide ; le profil recopie l'album dans le skin choisi sans
 * changer ses textes, et revient au Classique sans dessin.
 */
class AlbumSkinsTest {

    @TempDir
    Path data;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        skins.add(null);
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    private static boolean blank(Canvas canvas) {
        WritableImage image = canvas.snapshot(null, null);
        int first = image.getPixelReader().getArgb(0, 0);
        for (int y = 0; y < (int) image.getHeight(); y++) {
            for (int x = 0; x < (int) image.getWidth(); x++) if (image.getPixelReader().getArgb(x, y) != first) return false;
        }
        return true;
    }

    @Test
    void leCatalogueSeDessineDansChaqueSkinEtATouteTaille() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            for (double[] size : new double[][] {{150, 70}, {60, 30}}) {
                Canvas canvas = new Canvas(size[0], size[1]);
                for (Skin skin : skinsOf(SkinVisual.MOLECULE)) {
                    for (Skin palette : skinsOf(SkinVisual.MOLECULE_PALETTE)) {
                        for (Molecule molecule : game.molecules()) CatalogArt.drawMolecule(canvas.getGraphicsContext2D(), molecule, size[0], size[1], skin, palette);
                    }
                }
                for (Skin skin : skinsOf(SkinVisual.BODY)) {
                    for (Body body : game.bodies()) CatalogArt.drawBody(canvas.getGraphicsContext2D(), body, size[0], size[1], skin);
                }
                for (Skin skin : skinsOf(SkinVisual.BLOCKS)) {
                    for (Assembly assembly : game.assemblies()) CatalogArt.drawAssembly(canvas.getGraphicsContext2D(), assembly, size[0], size[1], skin);
                }
                assertFalse(blank(canvas), "quelque chose est dessiné à " + size[0] + " × " + size[1]);
            }
            return null;
        });
    }

    @Test
    void chaqueCaseSeDessineDansChaqueSkinDesAlbums() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            Molecule molecule = game.molecules().get(0);
            AlbumPane.Entry drawn = new AlbumPane.Entry(molecule.id(), molecule.name(), molecule.formula(), molecule.kind(),
                    (g, w, h) -> CatalogArt.drawMolecule(g, molecule, w, h, null, null));
            AlbumPane.Entry bare = new AlbumPane.Entry("x", "Sans dessin", "", null);
            assertNull(bare.picture());
            int skins = 0;
            for (Skin skin : skinsOf(SkinVisual.ALBUM)) {
                if (skin == null) continue;
                skins++;
                for (AlbumPane.Entry entry : List.of(drawn, bare)) {
                    for (boolean found : new boolean[] {true, false}) {
                        Node node = AlbumSkinArt.picture(entry, found, 7, skin, "#ffd27f");
                        assertNotNull(node);
                        Canvas canvas = (Canvas) ((javafx.scene.layout.HBox) node).getChildren().get(0);
                        // « Illustré » ne dessine que le dessin de la case : trouvée et sans dessin, elle reste nue (aucune case
                        // du jeu n'est sans dessin ; les autres skins ont leur cadre, leur timbre ou leur étiquette).
                        boolean nue = skin == Skin.ALBUM_ILLUSTRATED && found && entry.picture() == null;
                        assertEquals(nue, blank(canvas), skin + (found ? " trouvée" : " vide") + " : " + entry.name());
                    }
                }
            }
            assertEquals(5, skins);
            return null;
        });
    }

    @Test
    void leProfilRecopieLAlbumDansLeSkinChoisiSansChangerSesTextes() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.tick(1.5);
            game.state().setBigBangs(1);
            Molecule first = game.molecules().get(0);
            game.state().noteFound("molecules", first.id(), 125);
            Body body = game.bodies().get(0);
            game.state().noteFound("bodies", body.id(), 10);
            Assembly assembly = game.assemblies().get(0);
            game.state().noteFound("assemblies", assembly.id(), 20);
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertNull(page.albumSkin(ProfilePage.MOLECULES), "sans skin : le Classique");
            String classic = page.albumText(ProfilePage.MOLECULES, first.id());
            game.testAllSkins(true);
            for (Skin skin : skinsOf(SkinVisual.ALBUM)) {
                assertTrue(game.chooseSkin(SkinVisual.ALBUM, skin));
                for (int album = ProfilePage.MOLECULES; album <= ProfilePage.BODIES; album++) {
                    assertEquals(skin, page.albumSkin(album), "album " + album);
                    assertFalse(page.albumTexts(album).isEmpty());
                }
                assertEquals(classic, page.albumText(ProfilePage.MOLECULES, first.id()), "le texte ne change pas : " + skin);
                assertTrue(page.albumText(ProfilePage.BODIES, body.id()).contains(body.name()));
                assertTrue(page.albumText(ProfilePage.ASSEMBLIES, assembly.id()).contains(assembly.name()));
            }
            // Un autre skin des molécules redessine l'album : rien ne casse, les textes tiennent.
            assertTrue(game.chooseSkin(SkinVisual.MOLECULE, Skin.MOLECULE_PEARLS));
            assertEquals(Skin.ALBUM_HERBARIUM, page.albumSkin(ProfilePage.MOLECULES));
            assertEquals(classic, page.albumText(ProfilePage.MOLECULES, first.id()));
            return null;
        });
    }
}
