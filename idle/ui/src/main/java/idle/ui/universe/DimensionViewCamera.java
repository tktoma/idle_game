package idle.ui.universe;

import idle.core.Game;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.paint.*;
import javafx.scene.robot.Robot;
import javafx.scene.shape.*;
import java.util.Arrays;
import java.util.List;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : la caméra libre du volume, le changement de dimension et l'image qui avance. */
public abstract class DimensionViewCamera extends DimensionViewBase {
    public DimensionViewCamera(Game game) {
        super(game);
    }

    /** La caméra libre : dedans, le regard suit la souris sans clic ; dehors et sans elle, rien ne change. */
    public void setFreeCamera(boolean free) {
        if (freeCamera == free) return;
        freeCamera = free;
        dragging = false;
        mouseX = Double.NaN;
        mouseY = Double.NaN;
        mouseReleased = false;
        if (free && dive == Dive.INSIDE) recenter();
        setCursor(cursorAt(getWidth() / 2, getHeight() / 2));
    }

    /** Vrai quand la caméra libre tient la souris : dans le cube, sans Échap depuis le dernier clic. */
    public boolean mouseHeld() { return freeInside(); }

    /** Échap, en caméra libre : la souris est rendue, la plongée continue ; un clic dans la vue la reprend. */
    public void releaseMouse() {
        if (!freeInside()) return;
        mouseReleased = true;
        mouseX = Double.NaN;
        mouseY = Double.NaN;
        setCursor(Cursor.DEFAULT);
    }

    /** Vrai si la fenêtre du jeu a la main : sans elle (Alt+Tab), la souris n'est jamais retenue. */
    boolean windowFocused() {
        return getScene() != null && getScene().getWindow() != null && getScene().getWindow().isFocused();
    }

    /**
     * La souris retenue dans la vue, à chaque image : si elle en est sortie (un geste trop vif, une autre fenêtre
     * passée devant), elle y revient ; au retour de la main sur la fenêtre, elle revient au centre sans tourner.
     */
    void holdMouse() {
        boolean focused = windowFocused();
        if (!freeInside() || !focused) {
            hadFocus = focused;
            return;
        }
        if (!hadFocus) {
            hadFocus = true;
            mouseX = Double.NaN;
            mouseY = Double.NaN;
            recenter();
            return;
        }
        if (robot == null || robotRefused) return;
        Bounds area = localToScreen(getBoundsInLocal());
        if (area == null) return;
        double x = robot.getMouseX(), y = robot.getMouseY();
        if (x < area.getMinX() + 2 || x > area.getMaxX() - 2 || y < area.getMinY() + 2 || y > area.getMaxY() - 2) recenter();
    }

    public boolean freeCamera() { return freeCamera; }

    boolean freeInside() {
        return freeCamera && !mouseReleased && dive == Dive.INSIDE && dimension == Dimension.VOLUME;
    }

    /**
     * Un mouvement de souris en caméra libre : le regard tourne d'autant, dans le sens de la souris (à droite, on
     * regarde à droite). Quand la souris s'éloigne du centre, elle y est ramenée, pour pouvoir tourner sans fin ;
     * sa dernière position devient alors le centre, et le mouvement que cela provoque, s'il arrive, ne tourne rien.
     * Vrai si le geste a servi.
     */
    public boolean freeLook(double x, double y) {
        if (!freeInside()) return false;
        double cx = getWidth() / 2, cy = getHeight() / 2;
        if (!Double.isNaN(mouseX)) turn(-(x - mouseX), -(y - mouseY));
        mouseX = x;
        mouseY = y;
        // Ramenée dès qu'elle s'écarte un peu : elle ne s'approche jamais des bords de la vue.
        if (Math.abs(x - cx) > getWidth() * 0.1 || Math.abs(y - cy) > getHeight() * 0.1) recenter();
        return true;
    }

    /** Tourne le regard comme si la souris tenue avait glissé de ({@code dx}, {@code dy}). */
    void turn(double dx, double dy) {
        camera.focal = insideFocal();
        camera.yaw = lookYaw;
        camera.pitch = lookPitch;
        camera.drag(dx, dy);
        lookYaw = camera.yaw;
        lookPitch = camera.pitch;
    }

    /**
     * Ramène la souris au centre du volume, et en fait sa dernière position : le mouvement de retour, qu'il arrive ou
     * non, ne fait pas tourner le regard. Si le système refuse de la déplacer (une erreur, ou la souris restée où elle
     * était), on n'essaie plus : la caméra libre tourne alors en poussant les bords.
     */
    void recenter() {
        if (robotRefused || getScene() == null) return;
        Point2D center = localToScreen(getWidth() / 2, getHeight() / 2);
        if (center == null) return;
        try {
            if (robot == null) robot = new Robot();
            robot.mouseMove(center.getX(), center.getY());
            if (Math.abs(robot.getMouseX() - center.getX()) > 3 || Math.abs(robot.getMouseY() - center.getY()) > 3) {
                robotRefused = true;
                return;
            }
            mouseX = getWidth() / 2;
            mouseY = getHeight() / 2;
        } catch (RuntimeException | Error refused) {
            robotRefused = true;
        }
    }

    /** En caméra libre, la souris contre un bord fait tourner le regard en continu : sans fin, même sans recentrage. */
    void edgeTurn(double elapsed) {
        if (!freeInside() || Double.isNaN(mouseX) || elapsed <= 0) return;
        double w = getWidth(), h = getHeight(), band = EDGE_BAND;
        double sx = mouseX < w * band ? -1 : mouseX > w * (1 - band) ? 1 : 0;
        double sy = mouseY < h * band ? -1 : mouseY > h * (1 - band) ? 1 : 0;
        if (sx == 0 && sy == 0) return;
        double step = EDGE_TURN * elapsed * insideFocal();
        turn(-sx * step, -sy * step);
    }

    /** La dimension à montrer ; son animation repart du début. */
    public void show(Dimension dimension) {
        this.dimension = dimension;
        time = 0;
        resetDive();
        touches.clear();
        pickups.clear();
        trailCount = 0;
        lastPhase = Double.NaN;
        lastCollected = -1;
        lastVolumeCollected = -1;
        lastReleases = -1;
        loopMarks.clear();
        lastLoopHits = -1;
        lastBeatPhase = Double.NaN;
        Arrays.fill(ringFlash, 0);
        lastLoopAuto = -1;
        cancelBrane();
        braneMarks.clear();
        braneFlashes.clear();
        branePointPasses.clear();
        braneSeen.clear();
        braneSeenPoints = List.of();
        lastBraneTouches = -1;
        clickerMarks.clear();
        clickerMouseX = Double.NaN;
        clickerMouseY = Double.NaN;
        lastClickerAuto = -1;
        gridMarks.clear();
        gridHoverI = -1;
        gridHoverJ = -1;
        lastGridProbes = -1;
        mirrorFlashes.clear();
        lastMirrorCount = -1;
        mirrorGrowAge = -1;
        releaseMix(true);
        mixSteering = false;
        mixDepthAim = Double.NaN;
        mixMarks.clear();
        mixFlashes.clear();
        mixSeen.clear();
        lastMixReleases = -1;
        lastMixTreasures = -1;
        lastMixStage = -1;
    }

    /** Les traits allumés à l'instant : pour les vérifications. */
    public int touchesShown() { return touches.size(); }

    public Dimension dimension() { return dimension; }

    public void frame(double elapsed, boolean moving) {
        if (moving) time += Math.min(elapsed, 0.1);
        motion = moving;
        if (dimension == Dimension.LINE) noteTouches(Math.min(elapsed, 0.1), moving);
        if (dimension == Dimension.PLANE) notePickups(Math.min(elapsed, 0.1));
        if (dimension == Dimension.DURATION) {
            for (Pickup pickup : durationPickups) pickup.age += Math.min(elapsed, 0.1);
            durationPickups.removeIf(pickup -> pickup.age > 1.4);
            // Les relâchements de l'appui automatique : leurs instants montent du bouton, comme à la main.
            long releases = game.durationReleases();
            if (lastReleases >= 0 && releases > lastReleases) durationPickup(game.durationLastReward());
            lastReleases = releases;
        }
        if (dimension == Dimension.LOOP) runLoop(Math.min(elapsed, 0.1));
        if (dimension == Dimension.BRANE) runBrane(Math.min(elapsed, 0.1));
        if (dimension == Dimension.CLICKER) runClicker(Math.min(elapsed, 0.1));
        if (dimension == Dimension.GRID) runGrid(Math.min(elapsed, 0.1));
        if (dimension == Dimension.MIRROR) runMirror(Math.min(elapsed, 0.1));
        if (dimension == Dimension.MIX) runMix(Math.min(elapsed, 0.1));
        if (dimension == Dimension.VOLUME) {
            runDive(Math.min(elapsed, 0.1));
            edgeTurn(Math.min(elapsed, 0.1));
            holdMouse();
        }
        draw();
    }
}
