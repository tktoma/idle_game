package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.FxTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.128 : la fenêtre du volume, sous Xvfb. Un clic sur le cube fait plonger la vue, la plongée commence au
 * centre, le clic droit cueille, le clic gauche tenu fait tourner le regard, et le cube rejette le joueur à la fin.
 */
class VolumeWindowTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie au troisième univers. */
    private static Game third() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        assertTrue(game.testAddUniverse());
        assertTrue(game.testAddUniverse());
        assertTrue(game.isVolumeOpen());
        return game;
    }

    /** Une fenêtre du volume ouverte, à sa taille de départ. */
    private static DimensionWindow window(Game game) {
        DimensionWindow window = new DimensionWindow(game);
        window.open(Dimension.VOLUME);
        window.frame(0.05, true);
        return window;
    }

    /** Assez d'images pour finir une animation. */
    private static void frames(DimensionWindow window, int count) {
        for (int k = 0; k < count; k++) window.frame(0.1, true);
    }

    private static void enter(DimensionWindow window) {
        DimensionView view = window.view();
        assertTrue(view.press(view.getWidth() / 2, view.getHeight() / 2, false), "un clic gauche sur le cube");
        frames(window, 12);
    }

    @Test
    void cliquerLeCubeFaitPlongerEtLaPlongeeCommenceAuCentre() {
        FxTestSupport.fx(() -> {
            Game game = third();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            assertEquals("Dimension 3 : Le volume", window.titleText());
            assertEquals(DimensionView.Dive.OUTSIDE, view.dive());
            assertTrue(window.volumeUpgradesShown(), "dehors, les améliorations se montrent");
            assertTrue(window.volumeHintText().contains("Cliquez le cube"));
            assertFalse(view.press(5, 5, false), "un clic loin du cube ne fait rien");
            assertFalse(view.press(view.getWidth() / 2, view.getHeight() / 2, true), "ni un clic droit dehors");
            assertTrue(view.press(view.getWidth() / 2, view.getHeight() / 2, false));
            assertEquals(DimensionView.Dive.ENTERING, view.dive());
            window.frame(0.1, true);
            assertFalse(game.isInVolume(), "l'animation ne mange pas le minuteur");
            frames(window, 12);
            assertEquals(DimensionView.Dive.INSIDE, view.dive());
            assertTrue(game.isInVolume());
            assertEquals(Game.VOLUME_DIVE_SECONDS, game.volumeTimeLeft(), 1e-9);
            assertFalse(window.volumeUpgradesShown(), "dedans, pas d'améliorations");
            assertTrue(window.volumeHintText().contains("Clic droit"));
            return null;
        });
    }

    @Test
    void leClicDroitCueilleLaBouleVisee() {
        FxTestSupport.fx(() -> {
            Game game = third();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            enter(window);
            game.testClearVolume();
            game.addVolumeBall(0.1, -0.1, 0.0);
            Game.VolumeBall ball = game.volumeBalls().get(0);
            double[] at = view.screenOf(ball.x(), ball.y(), ball.z());
            // La boule peut être derrière : on se retourne jusqu'à la voir.
            for (int k = 0; at == null && k < 40; k++) {
                view.press(view.getWidth() / 2, view.getHeight() / 2, false);
                view.dragTo(view.getWidth() / 2 + 60, view.getHeight() / 2);
                at = view.screenOf(ball.x(), ball.y(), ball.z());
            }
            assertNotNull(at, "la boule finit par être en vue");
            assertTrue(view.press(at[0], at[1], true));
            assertEquals(1, game.state().volumeShards(), 1e-12, "une boule cueillie : un éclat du volume");
            assertEquals(1, view.volumePickupsShown(), "son gain monte");
            window.frame(0.05, true);
            assertEquals("Sphères du volume : 1", window.volumeShardsText());
            assertTrue(view.press(5, 5, true), "un clic droit dans le vide sert aussi : il ne cueille rien");
            assertEquals(1, game.state().volumeShards(), 1e-12);
            return null;
        });
    }

    @Test
    void leClicGaucheTenuFaitTournerLeRegard() {
        FxTestSupport.fx(() -> {
            Game game = third();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            enter(window);
            double yaw = view.lookYaw(), pitch = view.lookPitch();
            assertTrue(view.press(200, 200, false));
            assertTrue(view.dragTo(260, 170));
            assertNotEquals(yaw, view.lookYaw(), 1e-9);
            assertNotEquals(pitch, view.lookPitch(), 1e-9);
            return null;
        });
    }

    @Test
    void leCubeRejetteLeJoueurEtLesAmeliorationsReviennent() {
        FxTestSupport.fx(() -> {
            Game game = third();
            game.state().setVolumeShards(100);
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            enter(window);
            assertTrue(window.volumeButton(Game.VolumeUpgrade.TIME).isDisabled(), "pas d'achat pendant la plongée");
            game.tick(Game.VOLUME_DIVE_SECONDS + 0.5);
            assertFalse(game.isInVolume());
            window.frame(0.05, true);
            assertEquals(DimensionView.Dive.LEAVING, view.dive());
            frames(window, 10);
            assertEquals(DimensionView.Dive.OUTSIDE, view.dive());
            assertTrue(window.volumeUpgradesShown());
            assertFalse(window.volumeButton(Game.VolumeUpgrade.TIME).isDisabled());
            window.volumeButton(Game.VolumeUpgrade.TIME).fire();
            assertEquals(1, game.volumeLevel(Game.VolumeUpgrade.TIME));
            assertTrue(window.volumeButton(Game.VolumeUpgrade.TIME).getText().contains("15.0 s"), window.volumeButton(Game.VolumeUpgrade.TIME).getText());
            return null;
        });
    }

    @Test
    void fermerLaFenetrePendantLaPlongeeSortDuCube() {
        FxTestSupport.fx(() -> {
            Game game = third();
            DimensionWindow window = window(game);
            enter(window);
            assertTrue(game.isInVolume());
            window.close();
            assertFalse(game.isInVolume());
            window.open(Dimension.VOLUME);
            assertEquals(DimensionView.Dive.OUTSIDE, window.view().dive(), "la fenêtre rouverte montre le cube du dehors");
            return null;
        });
    }

    @Test
    void sansAnimationLaPlongeeEstImmediate() {
        FxTestSupport.fx(() -> {
            Game game = third();
            DimensionWindow window = window(game);
            DimensionView view = window.view();
            window.frame(0.05, false);
            assertTrue(view.press(view.getWidth() / 2, view.getHeight() / 2, false));
            window.frame(0.05, false);
            assertEquals(DimensionView.Dive.INSIDE, view.dive());
            game.tick(Game.VOLUME_DIVE_SECONDS + 1);
            window.frame(0.05, false);
            assertEquals(DimensionView.Dive.OUTSIDE, view.dive());
            return null;
        });
    }

    /** La direction du centre de la vue, dedans : celle que vise la croix de la caméra libre. */
    private static double[] ahead(DimensionView view) {
        VolumeCamera camera = new VolumeCamera();
        camera.yaw = view.lookYaw();
        camera.pitch = view.lookPitch();
        camera.focal = 300;
        return camera.ray(0, 0);
    }

    @Test
    void laCameraLibreSeChoisitDansLaFenetreEtResteGardee() {
        FxTestSupport.fx(() -> {
            Game game = third();
            boolean[] saved = {false};
            DimensionWindow window = new DimensionWindow(game, () -> saved[0], value -> saved[0] = value);
            window.open(Dimension.VOLUME);
            window.frame(0.05, true);
            assertFalse(window.view().freeCamera(), "coupée au départ");
            assertTrue(window.freeCameraButton().getText().contains("coupée"));
            window.freeCameraButton().fire();
            assertTrue(saved[0], "le choix est rangé dans les réglages");
            assertTrue(window.view().freeCamera());
            assertTrue(window.freeCameraButton().getText().contains("activée"));
            window.close();
            window.open(Dimension.VOLUME);
            assertTrue(window.view().freeCamera(), "la fenêtre rouverte reprend le choix gardé");
            enter(window);
            window.frame(0.05, true);
            assertTrue(window.volumeHintText().contains("bougez la souris"), window.volumeHintText());
            return null;
        });
    }

    @Test
    void enCameraLibreLeRegardSuitLaSourisSansClic() {
        FxTestSupport.fx(() -> {
            Game game = third();
            boolean[] saved = {true};
            DimensionWindow window = new DimensionWindow(game, () -> saved[0], value -> saved[0] = value);
            window.open(Dimension.VOLUME);
            window.frame(0.05, true);
            DimensionView view = window.view();
            enter(window);
            double cx = view.getWidth() / 2, cy = view.getHeight() / 2;
            double yaw = view.lookYaw(), pitch = view.lookPitch();
            assertTrue(view.freeLook(cx, cy));
            assertTrue(view.freeLook(cx + 40, cy - 20), "un mouvement, sans bouton tenu");
            assertTrue(view.lookYaw() > yaw, "la souris à droite : on regarde à droite");
            assertTrue(view.lookPitch() < pitch, "la souris en haut : on regarde en haut");
            // Ce qui était droit devant glisse vers la gauche de l'écran, comme dans un jeu à la première personne.
            double[] before = {Math.sin(yaw), 0, Math.cos(yaw)};
            double[] at = view.screenOf(before[0], before[1], before[2]);
            assertNotNull(at);
            assertTrue(at[0] < cx, "le point d'avant est passé à gauche");
            return null;
        });
    }

    @Test
    void enCameraLibreUnClicCueilleSousLaCroix() {
        FxTestSupport.fx(() -> {
            Game game = third();
            boolean[] saved = {true};
            DimensionWindow window = new DimensionWindow(game, () -> saved[0], value -> saved[0] = value);
            window.open(Dimension.VOLUME);
            window.frame(0.05, true);
            DimensionView view = window.view();
            enter(window);
            game.testClearVolume();
            double[] dir = ahead(view);
            double length = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
            game.addVolumeBall(0.6 * dir[0] / length, 0.6 * dir[1] / length, 0.6 * dir[2] / length);
            assertTrue(view.press(5, 5, false), "un clic gauche, même loin du centre : il vise la croix");
            assertEquals(1, game.state().volumeShards(), 1e-12);
            return null;
        });
    }

    @Test
    void enCameraLibrePousserUnBordFaitTournerSansFin() {
        FxTestSupport.fx(() -> {
            Game game = third();
            boolean[] saved = {true};
            DimensionWindow window = new DimensionWindow(game, () -> saved[0], value -> saved[0] = value);
            window.open(Dimension.VOLUME);
            window.frame(0.05, true);
            DimensionView view = window.view();
            enter(window);
            view.freeLook(view.getWidth() - 2, view.getHeight() / 2);
            double yaw = view.lookYaw();
            window.frame(0.1, true);
            assertEquals(yaw + DimensionView.EDGE_TURN * 0.1, view.lookYaw(), 1e-9, "contre le bord droit, on tourne à droite");
            window.frame(0.1, true);
            assertEquals(yaw + DimensionView.EDGE_TURN * 0.2, view.lookYaw(), 1e-9);
            return null;
        });
    }

    @Test
    void echapRendLaSourisEtUnClicLaReprendSansCueillir() {
        FxTestSupport.fx(() -> {
            Game game = third();
            boolean[] saved = {true};
            DimensionWindow window = new DimensionWindow(game, () -> saved[0], value -> saved[0] = value);
            window.open(Dimension.VOLUME);
            window.frame(0.05, true);
            DimensionView view = window.view();
            assertFalse(window.key(javafx.scene.input.KeyCode.ESCAPE, true), "dehors, Échap n'est pas pris");
            enter(window);
            assertTrue(view.mouseHeld(), "dans le cube, la caméra libre retient la souris");
            assertTrue(window.key(javafx.scene.input.KeyCode.ESCAPE, true));
            assertFalse(view.mouseHeld(), "Échap rend la souris");
            assertTrue(game.isInVolume(), "la plongée continue");
            window.frame(0.05, true);
            assertTrue(window.volumeHintText().contains("Souris rendue"), window.volumeHintText());
            double yaw = view.lookYaw();
            assertFalse(view.freeLook(100, 100));
            assertFalse(view.freeLook(300, 120));
            assertEquals(yaw, view.lookYaw(), 1e-12, "souris rendue : le regard ne la suit plus");
            // Une boule droit devant : le clic qui reprend la souris ne la cueille pas.
            game.testClearVolume();
            double[] dir = ahead(view);
            double length = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
            game.addVolumeBall(0.6 * dir[0] / length, 0.6 * dir[1] / length, 0.6 * dir[2] / length);
            assertTrue(view.press(50, 50, true));
            assertTrue(view.mouseHeld(), "un clic dans la vue reprend la souris");
            assertEquals(0, game.state().volumeShards(), 1e-12, "ce clic-là ne cueille rien");
            assertTrue(view.press(50, 50, true));
            assertEquals(1, game.state().volumeShards(), 1e-12, "le suivant cueille sous la croix");
            return null;
        });
    }
}
