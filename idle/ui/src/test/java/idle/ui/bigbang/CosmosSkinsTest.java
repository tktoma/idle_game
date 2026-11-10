package idle.ui.bigbang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import idle.core.sky.SolarSystem;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.194 : les skins du Big Bang au-delà des molécules, sous Xvfb : la palette des molécules, les amas,
 * les blocs et le gaz de la vue de l'espace, le ciel des astres, la galaxie, l'amas de galaxies et la toile, les
 * systèmes solaires. Chacun se dessine à deux tailles et dans plusieurs états sans exception, et change quelque
 * chose au dessin par rapport au Classique ; sans skin, le dessin est celui d'avant.
 */
class CosmosSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    /** Un jeu avec tous les skins, des sortes rassemblées de chaque état, des assemblages, tous les astres et trois systèmes. */
    private static Game game() {
        Game game = new Game();
        game.start();
        game.testAllSkins(true);
        for (Molecule molecule : game.molecules()) {
            game.state().setMoleculeCount(molecule.id(), molecule.state() == Molecule.State.GAS ? 60 : 30);
            game.state().addSubstance(molecule.id());
        }
        for (idle.core.sky.Assembly assembly : game.assemblies().subList(0, 6)) game.state().addAssembly(assembly.id());
        for (idle.core.sky.Body body : game.bodies()) game.state().addBody(body.id());
        List<SolarSystem> systems = new ArrayList<>();
        systems.add(new SolarSystem("yellow_sun", List.of("rocky_planet", "ocean_planet", "gas_giant_planet"), List.of("rocky_moon")));
        systems.add(new SolarSystem("red_dwarf", List.of("rocky_planet"), List.of()));
        game.state().setSolarSystems(systems);
        game.state().setSpace(idle.core.math.BigNum.of(1e12));
        return game;
    }

    /**
     * La même partie, sans aucun skin choisi : le témoin. Les vues lisent le skin choisi dans la partie à chaque image ;
     * avant la 0.203, le témoin partageait la partie des vues habillées et se dessinait donc dans leur skin (les tests
     * comparaient un skin à lui-même).
     */
    private static Game bare() {
        return game();
    }

    /** Le nombre de pixels qui diffèrent entre deux dessins, un sur trois dans chaque sens. */
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

    @Test
    void lesPalettesChangentLesCouleursDesAtomesEtSeCombinentAuxFormes() {
        FxTestSupport.fx(() -> {
            try {
                List<Skin> palettes = skinsOf(SkinVisual.MOLECULE_PALETTE);
                assertEquals(3, palettes.size());
                Canvas canvas = new Canvas(120, 120);
                for (Skin palette : palettes) {
                    assertTrue(palette.isFree(), palette.name());
                    MoleculeArt.setPalette(null);
                    int classicColors = 0;
                    List<javafx.scene.paint.Color> classic = new ArrayList<>();
                    for (Molecule molecule : Molecules.DEFAULT) classic.add(MoleculeArt.of(molecule).dominant());
                    MoleculeArt.setPalette(palette);
                    assertEquals(palette, MoleculeArt.palette());
                    for (int index = 0; index < Molecules.DEFAULT.size(); index++) {
                        Molecule molecule = Molecules.DEFAULT.get(index);
                        MoleculeArt.Shape shape = MoleculeArt.of(molecule);
                        if (shape.dominant().equals(classic.get(index))) classicColors++;
                        assertEquals(shape.atoms().get(0).color(), shape.dominant(), molecule.id());
                        // Chaque forme des molécules, à deux tailles, dans cette palette.
                        for (Skin form : skinsOf(SkinVisual.MOLECULE)) {
                            for (double size : new double[] {8, 40}) {
                                canvas.getGraphicsContext2D().clearRect(0, 0, 120, 120);
                                shape.draw(canvas.getGraphicsContext2D(), 60, 60, size, 0.3, form);
                            }
                        }
                    }
                    // L'hydrogène garde son blanc dans la palette sûre ; presque toutes les autres dominantes changent.
                    assertTrue(classicColors <= 2, palette.name() + " : " + classicColors + " dominantes inchangées");
                }
                // Un skin d'un autre visuel, ou aucun : la palette classique, et les mises en place d'origine.
                MoleculeArt.setPalette(Skin.TREE_BIRCH);
                assertEquals(null, MoleculeArt.palette());
                Molecule water = Molecules.DEFAULT.get(0);
                assertEquals(MoleculeArt.of(water).dominant(), MoleculeArt.of(water).atoms().get(0).color());
            } finally {
                MoleculeArt.setPalette(null);
            }
            return null;
        });
    }

    /** Fait vivre la vue de l'espace : à deux tailles, vue en entier (tout en gaz) puis rapprochée jusqu'à ce que le gaz disparaisse. */
    private static void live(SpaceView view) {
        for (double size : new double[] {600, 240}) {
            view.setWidth(size);
            view.setHeight(size * 0.6);
            view.fit();
            for (int k = 0; k < 6; k++) view.frame(0.1);
            for (int step = 0; step < 40 && view.mist() > 0; step++) {
                view.zoom(SpaceView.ZOOM_STEP);
                view.frame(0.1);
            }
            assertEquals(0, view.mist(), 1e-9, "de tout près, plus de gaz");
        }
    }

    @Test
    void lesAmasLesBlocsEtLeGazSeDessinentEtChangentLEspace() {
        FxTestSupport.fx(() -> {
            Game game = game();
            SpaceView plain = new SpaceView(bare());
            live(plain);
            for (SkinVisual visual : new SkinVisual[] {SkinVisual.CLUMPS, SkinVisual.BLOCKS, SkinVisual.GAS}) {
                List<Skin> skins = skinsOf(visual);
                assertEquals(visual == SkinVisual.BLOCKS ? 4 : 3, skins.size(), visual.name());
                for (Skin skin : skins) {
                    assertTrue(game.chooseSkin(visual, skin), skin.name());
                    SpaceView view = new SpaceView(game);
                    live(view);
                    // De près, les amas et les blocs ont d'autres formes ; de loin, le gaz une autre couleur.
                    plain.fit();
                    view.fit();
                    if (visual != SkinVisual.GAS) {
                        for (int step = 0; step < 40 && view.mist() > 0; step++) {
                            plain.zoom(SpaceView.ZOOM_STEP);
                            view.zoom(SpaceView.ZOOM_STEP);
                            plain.frame(0.1);
                            view.frame(0.1);
                        }
                    } else {
                        plain.frame(0.1);
                        view.frame(0.1);
                    }
                    assertTrue(differing(plain, view) > 0, skin.name() + " ne change rien");
                }
                assertTrue(game.chooseSkin(visual, null));
            }
            // Les trois ensemble, avec la palette des molécules, puis la galaxie formée.
            assertTrue(game.chooseSkin(SkinVisual.CLUMPS, Skin.CLUMPS_NEON));
            assertTrue(game.chooseSkin(SkinVisual.BLOCKS, Skin.BLOCKS_INGOTS));
            assertTrue(game.chooseSkin(SkinVisual.GAS, Skin.GAS_AURORA));
            assertTrue(game.chooseSkin(SkinVisual.MOLECULE_PALETTE, Skin.PALETTE_SAFE));
            try {
                MoleculeArt.setPalette(game.chosenSkin(SkinVisual.MOLECULE_PALETTE));
                live(new SpaceView(game));
                game.state().setGalaxy(true);
                SpaceView galaxy = new SpaceView(game);
                live(galaxy);
                assertTrue(galaxy.swirled());
                game.state().setCosmosLevel(2);
                galaxy.frame(0.1);
                game.state().setCosmosLevel(3);
                galaxy.frame(0.1);
            } finally {
                MoleculeArt.setPalette(null);
            }
            return null;
        });
    }

    @Test
    void leCielSeDessineDansChaqueSkinEtReserveLaPlaceDesTigesEtDesEtageres() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> skins = skinsOf(SkinVisual.SKY);
            assertEquals(3, skins.size());
            SkyView plain = new SkyView(bare());
            for (Skin skin : skins) {
                assertTrue(game.chooseSkin(SkinVisual.SKY, skin), skin.name());
                SkyView view = new SkyView(game);
                for (double size : new double[] {600, 300}) {
                    plain.setWidth(size);
                    plain.setHeight(size * 0.55);
                    view.setWidth(size);
                    view.setHeight(size * 0.55);
                    for (int k = 0; k < 4; k++) {
                        plain.frame(0.1);
                        view.frame(0.1);
                    }
                    assertEquals(35, view.drawn());
                    assertTrue(view.rows() >= 1);
                    assertTrue(differing(plain, view) > 0, skin.name() + " à " + size);
                }
            }
            assertTrue(SkySkinArt.above(Skin.SKY_MOBILE, 1) > 0, "le mobile réserve la tige");
            assertEquals(0, SkySkinArt.above(Skin.SKY_PLATE, 1));
            assertTrue(SkySkinArt.below(Skin.SKY_SHOWCASE, 1) > 0, "la vitrine réserve l'étagère");
            assertFalse(SkySkinArt.draws(Skin.BODY_PLANETARIUM));
            // Le ciel vide, dans chaque skin.
            Game empty = new Game();
            empty.start();
            empty.testAllSkins(true);
            for (Skin skin : skins) {
                assertTrue(empty.chooseSkin(SkinVisual.SKY, skin));
                SkyView view = new SkyView(empty);
                view.frame(0.1);
                assertEquals(0, view.drawn());
            }
            return null;
        });
    }

    @Test
    void laGalaxieLAmasEtLaToileSeDessinentDansChaqueSkin() {
        FxTestSupport.fx(() -> {
            Game game = game();
            assertEquals(5, skinsOf(SkinVisual.GALAXY).size());
            assertEquals(3, skinsOf(SkinVisual.GALAXY_CLUSTER).size());
            assertEquals(4, skinsOf(SkinVisual.COSMIC_WEB).size());
            Game bare = bare();
            for (int level = 0; level <= 3; level++) {
                game.state().setCosmosLevel(level);
                bare.state().setCosmosLevel(level);
                SkinVisual visual = level >= 3 ? SkinVisual.COSMIC_WEB : level == 2 ? SkinVisual.GALAXY_CLUSTER : SkinVisual.GALAXY;
                GalaxyView plain = new GalaxyView(bare);
                for (Skin skin : skinsOf(visual)) {
                    assertTrue(game.chooseSkin(visual, skin), skin.name());
                    GalaxyView view = new GalaxyView(game);
                    for (double size : new double[] {600, 260}) {
                        plain.setWidth(size);
                        plain.setHeight(size * 0.4);
                        view.setWidth(size);
                        view.setHeight(size * 0.4);
                        for (int k = 0; k < 3; k++) {
                            plain.frame(0.2);
                            view.frame(0.2);
                        }
                        assertTrue(differing(plain, view) > 0, skin.name() + " au niveau " + level + " à " + size);
                    }
                    // La vue de l'espace montre l'amas et la toile de loin : dans le même skin.
                    if (level >= 2) {
                        SpaceView space = new SpaceView(game);
                        // La page du Big Bang choisit l'échelle montrée (ses boutons) ; sans elle, la vue reste sur l'espace.
                        space.showCosmos(level >= 3 ? idle.core.sky.Cosmos.UNIVERSE : idle.core.sky.Cosmos.CLUSTER);
                        space.setWidth(500);
                        space.setHeight(300);
                        space.frame(0.1);
                        space.frame(0.1);
                        assertNotEquals(null, space.cosmosShown());
                    }
                }
                assertTrue(game.chooseSkin(visual, null));
            }
            return null;
        });
    }

    @Test
    void lesSystemesSolairesSeDessinentDansChaqueSkin() {
        FxTestSupport.fx(() -> {
            Game game = game();
            List<Skin> skins = skinsOf(SkinVisual.SYSTEM);
            assertEquals(3, skins.size());
            List<SolarSystem> systems = new ArrayList<>(game.state().solarSystems());
            systems.add(new SolarSystem("blue_giant", List.of(), List.of()));
            Game bare = bare();
            for (SolarSystem system : systems) {
                SystemView plain = new SystemView(bare, system);
                for (Skin skin : skins) {
                    assertTrue(game.chooseSkin(SkinVisual.SYSTEM, skin), skin.name());
                    SystemView view = new SystemView(game, system);
                    for (double size : new double[] {760, 400}) {
                        plain.setWidth(size);
                        plain.setHeight(200);
                        view.setWidth(size);
                        view.setHeight(200);
                        for (int k = 0; k < 3; k++) {
                            plain.frame(0.3);
                            view.frame(0.3);
                        }
                        assertTrue(differing(plain, view) > 0, skin.name() + " pour " + system.star() + " à " + size);
                    }
                }
                assertTrue(game.chooseSkin(SkinVisual.SYSTEM, null));
            }
            assertFalse(SystemSkinArt.draws(Skin.BODY_ENGRAVING));
            return null;
        });
    }
}
