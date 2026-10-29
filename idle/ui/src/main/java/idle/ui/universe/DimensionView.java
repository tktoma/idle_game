package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.CanvasPane;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Le visuel d'une dimension, dans sa fenêtre (version 0.081) : une tentative, sans logique, pour
 * chacune des dix. De la ligne (un point qui va et vient) au point qui contient tout, en passant
 * par le plan, le cube qui tourne, ses instants mis bout à bout, la boucle, tous les débuts, et d'autres lois.
 *
 * <p>La ligne a sa logique depuis la version 0.101 : la boule est celle du jeu ({@link Game#linePosition()}),
 * un trait à chaque bout et, avec l'amélioration, d'autres à égale distance entre les deux. Chaque trait
 * touché s'allume et laisse monter les éclats qu'il donne.
 *
 * <p>Le volume a la sienne depuis la version 0.128 : on clique le cube qui tourne, la vue plonge jusqu'en son
 * centre, et l'on regarde autour de soi (clic gauche tenu) pour cueillir les boules qui y apparaissent (clic
 * droit). À la fin du minuteur, le cube rejette le joueur : la vue ressort, et le cube tourne de nouveau.
 *
 * <p>La durée a la sienne depuis la version 0.134 : un rond, un bouton au milieu ; tant qu'on le tient, les spirales
 * avancent du bord vers le centre, et au relâchement leurs éclats montent du bouton.
 *
 * <p>Depuis la version 0.150, le jeu peut jouer seul le volume et la durée : la vue suit alors la partie. Une plongée
 * commencée sans clic fait plonger la vue, les boules cueillies seules laissent monter leur gain, et chaque relâchement
 * de l'appui automatique fait monter ses instants du bouton. Le joueur peut cueillir avec la plongée automatique ;
 * ses clics sur le bouton de la durée, eux, ne font rien tant que l'appui automatique le tient.
 *
 * <p>La boucle a la sienne depuis la version 0.153 : des cercles concentriques, jusqu'à sept, le premier dehors. Sur
 * chacun, un point tourne et des zones attendent, chacune en trois parts (le bord « mauvais », puis « bien », le centre
 * « excellent »). Un clic sur un cercle (ou sa touche, que la fenêtre transmet) le joue : le mot de ce qu'il a valu
 * monte du point, et un anneau autour du point se remplit tant que le cercle attend. Les clics du rythme automatique
 * font monter leurs nœuds du centre. Le métronome, s'il est branché ({@link #setOnBeat}), bat à chaque passage du point
 * du premier cercle sur le centre d'une zone.
 */
public final class DimensionView extends Canvas {

    private static final Color BACK = Color.web("#05060e");
    private static final Color[] PALETTE = {
            Color.web("#9fd0ff"), Color.web("#ffd27f"), Color.web("#c9a6ff"), Color.web("#9be7a8"), Color.web("#ff9eb5"),
            Color.web("#7fe0d4"), Color.web("#ffb36b"), Color.web("#a8b8ff"), Color.web("#ff7ad9"), Color.web("#fff1c9")};

    /** Un trait qui vient d'être touché : son éclat et les éclats qui montent. */
    private static final class Touch {
        int line;
        double at, age;
    }

    /** Un éclat du plan qui vient d'être ramassé (version 0.104). */
    private static final class Pickup {
        double x, y, age;
        String text;
    }

    private static final Color SHARD = Color.web("#ffd27f");

    private final Game game;
    private final List<Touch> touches = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private long lastCollected = -1;
    /** La traînée du point du plan : ses dernières positions, la plus récente en tête. */
    private static final int TRAIL = 90;
    private final double[] trailX = new double[TRAIL], trailY = new double[TRAIL];
    private int trailHead, trailCount;
    /** La route de la boule à l'image d'avant, pour voir les traits passés depuis. */
    private double lastPhase = Double.NaN;
    private Dimension dimension = Dimension.LINE;
    private double time;

    // Le volume (version 0.128).
    /** Où en est la vue : dehors (le cube tourne), en train de plonger, dedans, en train de ressortir. */
    public enum Dive { OUTSIDE, ENTERING, INSIDE, LEAVING }

    /** Durées de la plongée et de la sortie, en secondes. */
    public static final double DIVE_IN_SECONDS = 0.9, DIVE_OUT_SECONDS = 0.7;
    /** Le cube du dehors : sa taille relative à l'écran, et la distance d'où on le regarde. */
    private static final double OUTSIDE_SIZE = 0.22, OUTSIDE_DISTANCE = 5;
    /** La focale du dedans, relative à l'écran : un regard large, comme dans une pièce. */
    private static final double INSIDE_FOCAL = 0.6;
    private final VolumeCamera camera = new VolumeCamera();
    private Dive dive = Dive.OUTSIDE;
    /** Avancement de la plongée ou de la sortie, de 0 à 1. */
    private double diveProgress;
    /** L'orientation au début de la plongée ou de la sortie, d'où la vue part. */
    private double fromYaw, fromPitch;
    /** Le regard dedans : la caméra y revient après chaque image d'animation. */
    private double lookYaw, lookPitch;
    private double dragX, dragY;
    private boolean dragging;
    /** Quand chaque boule est apparue, pour la faire grossir en naissant. */
    private final java.util.Map<Game.VolumeBall, Double> born = new java.util.IdentityHashMap<>();
    /** Les clics droits qui n'ont rien touché : un petit cercle qui s'efface. */
    private final List<Pickup> misses = new ArrayList<>();
    private final List<Pickup> volumePickups = new ArrayList<>();
    private boolean motion = true;
    /** La durée (version 0.134) : les éclats donnés au relâchement, qui montent du bouton. */
    private final List<Pickup> durationPickups = new ArrayList<>();
    /**
     * Les boules cueillies et les relâchements de la durée que la vue a déjà montrés (version 0.150) : ce que la partie
     * compte de plus vient de l'automatisation des dimensions. −1 tant que la vue n'a rien vu.
     */
    private long lastVolumeCollected = -1, lastReleases = -1;
    /** Les tours qu'une spirale dessine au plus : au-delà, le rond serait plein ; elle en représente alors davantage. */
    public static final double DURATION_DRAWN_TURNS = 8;
    /** Le rond et le bouton, relatifs au plus petit côté de la vue. */
    private static final double DURATION_RING = 0.4, DURATION_BUTTON = 0.08;

    // La caméra libre (version 0.128) : dedans, le regard suit la souris sans clic, comme dans un jeu à la première personne.
    /** Vrai quand le joueur a choisi la caméra libre (un pixel de souris tourne le regard d'un pixel d'écran au centre). */
    private boolean freeCamera;
    /** Où était la souris à son dernier mouvement sur le volume ; NaN quand elle n'y est pas. */
    private double mouseX = Double.NaN, mouseY = Double.NaN;
    /** De quoi ramener la souris au centre ; {@code null} tant qu'il n'a pas servi, ou si le système le refuse. */
    private javafx.scene.robot.Robot robot;
    private boolean robotRefused;
    /** Échap a libéré la souris (version 0.128) : le regard ne la suit plus jusqu'au prochain clic dans la vue. */
    private boolean mouseReleased;
    /** La fenêtre du jeu avait-elle la main à l'image d'avant : au retour, la souris revient au centre sans tourner le regard. */
    private boolean hadFocus = true;
    /** Contre un bord, en caméra libre, le regard tourne seul à cette vitesse (radians par seconde). */
    public static final double EDGE_TURN = 1.8;
    /** La part de l'écran, de chaque côté, où la souris fait tourner le regard en continu. */
    public static final double EDGE_BAND = 0.04;

    public DimensionView(Game game) {
        super(800, 480);
        this.game = game;
        // Le volume se joue à la souris (version 0.128) : ces gestes ne servent à rien dans les autres dimensions.
        setOnMousePressed(event -> {
            if (press(event.getX(), event.getY(), event.getButton() == javafx.scene.input.MouseButton.SECONDARY)) event.consume();
            setCursor(cursorAt(event.getX(), event.getY()));
        });
        setOnMouseDragged(event -> {
            if (freeLook(event.getX(), event.getY()) || event.isPrimaryButtonDown() && dragTo(event.getX(), event.getY())) event.consume();
        });
        setOnMouseReleased(event -> {
            // La durée (version 0.134) : relâcher le bouton, où que soit la souris, donne les éclats.
            releaseHold();
            dragging = false;
            setCursor(cursorAt(event.getX(), event.getY()));
        });
        setOnMouseMoved(event -> {
            freeLook(event.getX(), event.getY());
            setCursor(cursorAt(event.getX(), event.getY()));
        });
        setOnMouseExited(event -> {
            // La caméra libre retient la souris dans la vue : sortie trop vite, elle compte son mouvement et revient.
            if (freeInside() && windowFocused()) {
                freeLook(event.getX(), event.getY());
                recenter();
                return;
            }
            mouseX = Double.NaN;
            mouseY = Double.NaN;
        });
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
    public boolean mouseHeld() {
        return freeInside();
    }

    /** Échap, en caméra libre : la souris est rendue, la plongée continue ; un clic dans la vue la reprend. */
    public void releaseMouse() {
        if (!freeInside()) return;
        mouseReleased = true;
        mouseX = Double.NaN;
        mouseY = Double.NaN;
        setCursor(javafx.scene.Cursor.DEFAULT);
    }

    /** Vrai si la fenêtre du jeu a la main : sans elle (Alt+Tab), la souris n'est jamais retenue. */
    private boolean windowFocused() {
        return getScene() != null && getScene().getWindow() != null && getScene().getWindow().isFocused();
    }

    /**
     * La souris retenue dans la vue, à chaque image : si elle en est sortie (un geste trop vif, une autre fenêtre
     * passée devant), elle y revient ; au retour de la main sur la fenêtre, elle revient au centre sans tourner.
     */
    private void holdMouse() {
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
        javafx.geometry.Bounds area = localToScreen(getBoundsInLocal());
        if (area == null) return;
        double x = robot.getMouseX(), y = robot.getMouseY();
        if (x < area.getMinX() + 2 || x > area.getMaxX() - 2 || y < area.getMinY() + 2 || y > area.getMaxY() - 2) recenter();
    }

    public boolean freeCamera() {
        return freeCamera;
    }

    private boolean freeInside() {
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
    private void turn(double dx, double dy) {
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
    private void recenter() {
        if (robotRefused || getScene() == null) return;
        javafx.geometry.Point2D center = localToScreen(getWidth() / 2, getHeight() / 2);
        if (center == null) return;
        try {
            if (robot == null) robot = new javafx.scene.robot.Robot();
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
    private void edgeTurn(double elapsed) {
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
        java.util.Arrays.fill(ringFlash, 0);
    }

    /** Les traits allumés à l'instant : pour les vérifications. */
    public int touchesShown() {
        return touches.size();
    }

    public Dimension dimension() {
        return dimension;
    }

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
        if (dimension == Dimension.VOLUME) {
            runDive(Math.min(elapsed, 0.1));
            edgeTurn(Math.min(elapsed, 0.1));
            holdMouse();
        }
        draw();
    }

    // ------------------------------------------------------------------
    // Le volume (version 0.128) : la plongée, le regard, la cueillette
    // ------------------------------------------------------------------

    /** Où en est la vue du volume : pour les vérifications et la fenêtre. */
    public Dive dive() {
        return dive;
    }

    /** Le regard dedans : pour les vérifications. */
    public double lookYaw() {
        return lookYaw;
    }

    public double lookPitch() {
        return lookPitch;
    }

    /** Les gains de boules cueillies qui montent encore : pour les vérifications. */
    public int volumePickupsShown() {
        return volumePickups.size();
    }

    /** Où se dessine ce point du cube à l'image en cours, ou {@code null} s'il est derrière l'œil : pour les vérifications. */
    public double[] screenOf(double x, double y, double z) {
        aimCamera();
        return camera.project(x, y, z);
    }

    /** La vue revient dehors, sans animation : à l'ouverture de la fenêtre, et quand elle se ferme. */
    public void resetDive() {
        dive = Dive.OUTSIDE;
        diveProgress = 0;
        dragging = false;
        born.clear();
        misses.clear();
        volumePickups.clear();
        durationPickups.clear();
    }

    /** L'orientation du cube qui tourne, dehors, à cet instant : la même que depuis la version 0.081. */
    private double outsideYaw() {
        return time * 0.6;
    }

    private double outsidePitch() {
        return time * 0.6 * 0.7;
    }

    /** Le rayon, à l'écran, de la zone où un clic sur le cube du dehors y fait plonger. */
    private double cubeReach() {
        return Math.min(getWidth(), getHeight()) * OUTSIDE_SIZE * 1.8;
    }

    private boolean overCube(double x, double y) {
        return Math.hypot(x - getWidth() / 2, y - getHeight() / 2) <= cubeReach();
    }

    private javafx.scene.Cursor cursorAt(double x, double y) {
        if (dimension == Dimension.DURATION && game.isDurationOpen() && overDurationButton(x, y)) return javafx.scene.Cursor.HAND;
        if (dimension == Dimension.LOOP && game.isLoopOpen() && loopRingAt(x, y) >= 0) return javafx.scene.Cursor.HAND;
        if (dimension != Dimension.VOLUME || !game.isVolumeOpen()) return javafx.scene.Cursor.DEFAULT;
        // La caméra libre cache la souris : la croix au centre de la vue la remplace.
        if (freeInside()) return robotRefused ? javafx.scene.Cursor.CROSSHAIR : javafx.scene.Cursor.NONE;
        if (dive == Dive.INSIDE) return dragging ? javafx.scene.Cursor.CLOSED_HAND : javafx.scene.Cursor.CROSSHAIR;
        return dive == Dive.OUTSIDE && overCube(x, y) ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT;
    }

    /**
     * Un bouton de la souris enfoncé sur le volume : dehors, un clic gauche sur le cube y fait plonger ; dedans, le
     * gauche commence à faire tourner le regard et le droit cueille la boule visée. En caméra libre, le regard suit
     * déjà la souris : les deux boutons cueillent la boule sous la croix, au centre. Vrai si le geste a servi.
     */
    public boolean press(double x, double y, boolean secondary) {
        if (dimension == Dimension.DURATION) return pressDuration(x, y);
        if (dimension == Dimension.LOOP) return pressLoop(x, y);
        if (dimension != Dimension.VOLUME || !game.isVolumeOpen()) return false;
        if (dive == Dive.OUTSIDE) {
            if (secondary || !overCube(x, y)) return false;
            fromYaw = wrap(outsideYaw());
            fromPitch = wrap(outsidePitch());
            dive = Dive.ENTERING;
            diveProgress = 0;
            if (!motion) runDive(0);
            return true;
        }
        if (dive != Dive.INSIDE) return false;
        if (freeCamera && mouseReleased) {
            // Après Échap, un clic dans la vue reprend la souris ; il ne cueille rien.
            mouseReleased = false;
            mouseX = Double.NaN;
            mouseY = Double.NaN;
            recenter();
            setCursor(cursorAt(x, y));
            return true;
        }
        if (freeCamera) {
            x = getWidth() / 2;
            y = getHeight() / 2;
        } else if (!secondary) {
            dragging = true;
            dragX = x;
            dragY = y;
            return true;
        }
        aimCamera();
        double[] ray = camera.ray(x, y);
        Game.VolumeBall hit = game.shootVolume(ray[0], ray[1], ray[2]);
        Pickup mark = new Pickup();
        if (hit != null) {
            // Cueillie à la main : elle a déjà son gain, la vue ne la compte pas parmi celles de la plongée automatique.
            born.remove(hit);
            lastVolumeCollected = game.volumeCollected();
            double[] at = camera.project(hit.x(), hit.y(), hit.z());
            mark.x = at == null ? x : at[0];
            mark.y = at == null ? y : at[1];
            mark.text = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.volumeValue()));
            if (volumePickups.size() < 30) volumePickups.add(mark);
        } else {
            mark.x = x;
            mark.y = y;
            if (misses.size() < 10) misses.add(mark);
        }
        return true;
    }

    /** La souris tenue se déplace : le regard suit. Vrai si elle tournait le regard. */
    public boolean dragTo(double x, double y) {
        if (!dragging || dive != Dive.INSIDE) return false;
        camera.focal = insideFocal();
        camera.yaw = lookYaw;
        camera.pitch = lookPitch;
        camera.drag(x - dragX, y - dragY);
        lookYaw = camera.yaw;
        lookPitch = camera.pitch;
        dragX = x;
        dragY = y;
        return true;
    }

    private double insideFocal() {
        return Math.min(getWidth(), getHeight()) * INSIDE_FOCAL;
    }

    private static double wrap(double angle) {
        return VolumeCamera.closest(angle, 0);
    }

    /** Fait avancer la plongée ou la sortie, et suit le jeu : le cube qui rejette le joueur fait ressortir la vue. */
    private void runDive(double elapsed) {
        for (Pickup pickup : volumePickups) pickup.age += elapsed;
        volumePickups.removeIf(pickup -> pickup.age > 0.9);
        for (Pickup miss : misses) miss.age += elapsed;
        misses.removeIf(miss -> miss.age > 0.4);
        if (!game.isVolumeOpen() && dive != Dive.OUTSIDE) {
            resetDive();
            return;
        }
        switch (dive) {
            case ENTERING -> {
                diveProgress = motion ? Math.min(1, diveProgress + elapsed / DIVE_IN_SECONDS) : 1;
                if (diveProgress >= 1) {
                    // La plongée du jeu ne commence qu'une fois la vue arrivée au centre : l'animation ne mange pas le minuteur.
                    if (game.enterVolume() || game.isInVolume()) {
                        dive = Dive.INSIDE;
                        lookYaw = fromYaw;
                        lookPitch = 0;
                        born.clear();
                        mouseX = Double.NaN;
                        mouseY = Double.NaN;
                        mouseReleased = false;
                        if (freeCamera) recenter();
                        setCursor(cursorAt(getWidth() / 2, getHeight() / 2));
                    } else {
                        dive = Dive.OUTSIDE;
                    }
                    diveProgress = 0;
                }
            }
            case INSIDE -> {
                if (!game.isInVolume()) {
                    fromYaw = lookYaw;
                    fromPitch = lookPitch;
                    dragging = false;
                    dive = Dive.LEAVING;
                    setCursor(javafx.scene.Cursor.DEFAULT);
                    diveProgress = 0;
                    if (!motion) runDive(0);
                }
            }
            case LEAVING -> {
                diveProgress = motion ? Math.min(1, diveProgress + elapsed / DIVE_OUT_SECONDS) : 1;
                if (diveProgress >= 1) {
                    dive = Dive.OUTSIDE;
                    diveProgress = 0;
                    born.clear();
                }
            }
            case OUTSIDE -> {
                // La plongée automatique (version 0.150) a plongé sans clic : la vue plonge à son tour.
                if (game.isInVolume()) {
                    fromYaw = wrap(outsideYaw());
                    fromPitch = wrap(outsidePitch());
                    dive = Dive.ENTERING;
                    diveProgress = 0;
                }
            }
        }
        // Les boules nées depuis l'image d'avant : on note l'instant pour les faire grossir.
        java.util.Set<Game.VolumeBall> present = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        present.addAll(game.volumeBalls());
        // Celles que la plongée automatique a cueillies depuis : leur gain monte de là où elles étaient.
        long collected = game.volumeCollected();
        if (dive == Dive.INSIDE && lastVolumeCollected >= 0 && collected > lastVolumeCollected) {
            long left = collected - lastVolumeCollected;
            aimCamera();
            for (Game.VolumeBall ball : born.keySet()) {
                if (left <= 0) break;
                if (present.contains(ball)) continue;
                left--;
                double[] at = camera.project(ball.x(), ball.y(), ball.z());
                if (at == null || volumePickups.size() >= 30) continue;
                Pickup mark = new Pickup();
                mark.x = at[0];
                mark.y = at[1];
                mark.text = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.volumeValue()));
                volumePickups.add(mark);
            }
        }
        lastVolumeCollected = collected;
        born.keySet().retainAll(present);
        for (Game.VolumeBall ball : game.volumeBalls()) born.putIfAbsent(ball, time);
    }

    /** Pose la caméra pour l'image : dehors, en train de plonger, dedans ou en train de ressortir. */
    private void aimCamera() {
        double w = getWidth(), h = getHeight(), u = Math.min(w, h);
        double outsideFocal = OUTSIDE_DISTANCE * u * OUTSIDE_SIZE;
        camera.centerX = w / 2;
        camera.centerY = h / 2;
        switch (dive) {
            case OUTSIDE -> {
                camera.yaw = outsideYaw();
                camera.pitch = outsidePitch();
                camera.distance = OUTSIDE_DISTANCE;
                camera.focal = outsideFocal;
            }
            case INSIDE -> {
                camera.yaw = lookYaw;
                camera.pitch = lookPitch;
                camera.distance = 0;
                camera.focal = insideFocal();
            }
            case ENTERING -> {
                double e = ease(diveProgress);
                camera.yaw = fromYaw;
                camera.pitch = fromPitch * (1 - e);
                camera.distance = OUTSIDE_DISTANCE * (1 - e);
                camera.focal = outsideFocal + (insideFocal() - outsideFocal) * e;
            }
            case LEAVING -> {
                double e = ease(diveProgress);
                // La vue ressort vers le cube qui tourne de nouveau : elle finit là où il en est.
                double toYaw = VolumeCamera.closest(outsideYaw(), fromYaw), toPitch = VolumeCamera.closest(outsidePitch(), fromPitch);
                camera.yaw = fromYaw + (toYaw - fromYaw) * e;
                camera.pitch = fromPitch + (toPitch - fromPitch) * e;
                camera.distance = OUTSIDE_DISTANCE * e;
                camera.focal = insideFocal() + (outsideFocal - insideFocal()) * e;
            }
        }
    }

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    /** Ce que la plongée a d'avancé, de 0 (dehors) à 1 (au centre) : pour estomper le petit cube du dedans. */
    private double depthIn() {
        return switch (dive) {
            case OUTSIDE -> 0;
            case INSIDE -> 1;
            case ENTERING -> ease(diveProgress);
            case LEAVING -> 1 - ease(diveProgress);
        };
    }

    // 3. Le volume : dehors, le cube qui tourne et un plus petit dedans, dans l'autre sens (version 0.081) ;
    // depuis la version 0.128, on y plonge, et l'on cueille de son centre les boules qui y apparaissent.
    private void volumeScene(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        if (dive == Dive.OUTSIDE) {
            volume(g, cx, cy, u * OUTSIDE_SIZE, time * 0.6, 1);
            if (game.isVolumeOpen()) {
                g.setGlobalAlpha(0.55 + 0.25 * Math.sin(time * 2.5));
                g.setFill(Color.WHITE);
                g.setFont(Font.font(14));
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText("Cliquez le cube pour y plonger", cx, cy + cubeReach() + 18);
            }
            return;
        }
        aimCamera();
        Color c = color();
        double inside = depthIn();
        // Les faces vues du dedans : une grille légère, pour sentir où l'on regarde.
        g.setLineWidth(1);
        g.setStroke(c);
        g.setGlobalAlpha(0.12 * inside);
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                for (double t = -0.5; t <= 0.51; t += 0.5) {
                    segment(g, face(axis, side, t, -1), face(axis, side, t, 1));
                    segment(g, face(axis, side, -1, t), face(axis, side, 1, t));
                }
            }
        }
        // Les douze arêtes du cube.
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.8);
        for (int k = 0; k < 8; k++) {
            for (int bit : new int[] {1, 2, 4}) {
                int j = k ^ bit;
                if (j > k) segment(g, corner(k), corner(j));
            }
        }
        // Le petit cube blanc du dehors s'estompe à mesure qu'on entre.
        if (inside < 1) innerCube(g, (1 - inside) * (1 - inside));
        // Les boules, de la plus lointaine à la plus proche.
        List<double[]> shown = new ArrayList<>();
        for (Game.VolumeBall ball : game.volumeBalls()) {
            double[] at = camera.project(ball.x(), ball.y(), ball.z());
            if (at == null) continue;
            double grow = Math.min(1, (time - born.getOrDefault(ball, time)) / 0.35);
            if (!motion) grow = 1;
            shown.add(new double[] {at[0], at[1], at[2], camera.focal * game.volumeBallRadius() / at[2] * (0.2 + 0.8 * grow)});
        }
        shown.sort((a, b) -> Double.compare(b[2], a[2]));
        for (double[] ball : shown) {
            double r = Math.max(2, ball[3]);
            UniverseView.glow(g, ball[0], ball[1], r * 2.4, c, 0.55);
            g.setGlobalAlpha(1);
            g.setFill(new javafx.scene.paint.RadialGradient(0, 0, ball[0] - r * 0.35, ball[1] - r * 0.35, r * 1.3, false,
                    javafx.scene.paint.CycleMethod.NO_CYCLE, new javafx.scene.paint.Stop(0, Color.WHITE), new javafx.scene.paint.Stop(1, c)));
            g.fillOval(ball[0] - r, ball[1] - r, 2 * r, 2 * r);
        }
        // Ce qui vient d'être cueilli : un éclat et le gain qui monte ; un clic dans le vide, un petit cercle.
        g.setFont(Font.font(15));
        g.setTextAlign(TextAlignment.CENTER);
        for (Pickup pickup : volumePickups) {
            double t = pickup.age / 0.9;
            UniverseView.glow(g, pickup.x, pickup.y, 12 + 34 * t, c, 0.8 * (1 - t));
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.fillText(pickup.text, pickup.x, pickup.y - 18 - 30 * t);
        }
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.2);
        for (Pickup miss : misses) {
            double t = miss.age / 0.4, r = 6 + 10 * t;
            g.setGlobalAlpha(0.6 * (1 - t));
            g.strokeOval(miss.x - r, miss.y - r, 2 * r, 2 * r);
        }
        if (dive == Dive.INSIDE && freeCamera) crosshair(g, w / 2, h / 2);
        if (dive == Dive.INSIDE) timer(g, w, c);
    }

    /** La croix de la caméra libre, au centre : c'est elle qui vise. */
    private static void crosshair(GraphicsContext g, double x, double y) {
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.85);
        g.strokeLine(x - 10, y, x - 4, y);
        g.strokeLine(x + 4, y, x + 10, y);
        g.strokeLine(x, y - 10, x, y - 4);
        g.strokeLine(x, y + 4, x, y + 10);
        g.setGlobalAlpha(1);
    }

    /** Le minuteur de la plongée, en haut : une barre qui se vide, rouge dans les trois dernières secondes. */
    private void timer(GraphicsContext g, double w, Color c) {
        double left = game.volumeTimeLeft(), share = Math.max(0, Math.min(1, left / game.volumeDiveSeconds()));
        double width = w * 0.4, x = (w - width) / 2, y = 14;
        Color fill = left <= 3 ? Color.web("#ff7a7a") : c;
        // Un fond sombre derrière la barre et le temps : lisibles même devant une boule.
        g.setGlobalAlpha(0.65);
        g.setFill(BACK);
        g.fillRoundRect(x - 12, y - 8, width + 24, 42, 12, 12);
        g.setGlobalAlpha(0.35);
        g.setFill(Color.web("#1a1f33"));
        g.fillRoundRect(x, y, width, 8, 8, 8);
        g.setGlobalAlpha(left <= 3 ? 0.7 + 0.3 * Math.sin(time * 12) : 0.9);
        g.setFill(fill);
        g.fillRoundRect(x, y, width * share, 8, 8, 8);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.setFont(Font.font(13));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(String.format(java.util.Locale.ROOT, "%.1f s", Math.max(0, left)), w / 2, y + 26);
    }

    /** Un coin du cube, de 0 à 7 : un bit par axe. */
    private static double[] corner(int k) {
        return new double[] {(k & 1) == 0 ? -1 : 1, (k & 2) == 0 ? -1 : 1, (k & 4) == 0 ? -1 : 1};
    }

    /** Un point d'une face du cube : la face {@code side} de l'axe {@code axis}, aux coordonnées (p, q) des deux autres. */
    private static double[] face(int axis, int side, double p, double q) {
        return switch (axis) {
            case 0 -> new double[] {side, p, q};
            case 1 -> new double[] {p, side, q};
            default -> new double[] {p, q, side};
        };
    }

    /** Un trait entre deux points du cube, coupé là où il passe derrière l'œil. */
    private void segment(GraphicsContext g, double[] a, double[] b) {
        double[][] cut = camera.clip(camera.toView(a[0], a[1], a[2]), camera.toView(b[0], b[1], b[2]));
        if (cut == null) return;
        double[] p = camera.screen(cut[0]), q = camera.screen(cut[1]);
        if (p != null && q != null) g.strokeLine(p[0], p[1], q[0], q[1]);
    }

    /** Le petit cube blanc qui tourne dans l'autre sens, posé devant l'œil comme dehors, de plus en plus effacé. */
    private void innerCube(GraphicsContext g, double alpha) {
        double a = -time * 0.6 * 1.3, b = time * 0.6 * 0.5, scale = 0.45;
        double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
        double[][] view = new double[8][];
        for (int k = 0; k < 8; k++) {
            double[] p = corner(k);
            double x = p[0] * scale, y = p[1] * scale, z = p[2] * scale;
            double x1 = x * ca - z * sa, z1 = x * sa + z * ca;
            double y1 = y * cb - z1 * sb, z2 = y * sb + z1 * cb;
            view[k] = new double[] {x1, y1, z2 + camera.distance};
        }
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.8 * 0.8 * alpha);
        for (int k = 0; k < 8; k++) {
            for (int bit : new int[] {1, 2, 4}) {
                int j = k ^ bit;
                if (j <= k) continue;
                double[][] cut = camera.clip(view[k], view[j]);
                if (cut == null) continue;
                double[] p = camera.screen(cut[0]), q = camera.screen(cut[1]);
                if (p != null && q != null) g.strokeLine(p[0], p[1], q[0], q[1]);
            }
        }
    }

    /** Les éclats ramassés depuis l'image d'avant : un éclat là où est le point. */
    private void notePickups(double elapsed) {
        for (Pickup pickup : pickups) pickup.age += elapsed;
        pickups.removeIf(pickup -> pickup.age > 0.9);
        // La traînée suit le point, une position par image.
        trailX[trailHead] = game.planeX();
        trailY[trailHead] = game.planeY();
        trailHead = (trailHead + 1) % TRAIL;
        trailCount = Math.min(TRAIL, trailCount + 1);
        long collected = game.planeCollected();
        if (lastCollected >= 0 && collected > lastCollected) {
            double[] at = onPlane(getWidth(), getHeight(), getWidth() / 2, game.planeX(), game.planeY());
            Pickup pickup = new Pickup();
            pickup.x = at[0];
            pickup.y = at[1];
            pickup.text = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.planeValue() * (collected - lastCollected)));
            if (pickups.size() < 20) pickups.add(pickup);
        }
        lastCollected = collected;
    }

    /** Les éclats ramassés à l'instant : pour les vérifications. */
    public int pickupsShown() {
        return pickups.size();
    }

    /** Les traits que les boules ont touchés depuis l'image d'avant : chacun s'allume. */
    private void noteTouches(double elapsed, boolean moving) {
        for (Touch touch : touches) touch.age += elapsed;
        touches.removeIf(touch -> touch.age > 1.2);
        double phase = game.state().linePhase();
        if (!Double.isNaN(lastPhase) && moving) {
            double move = phase < lastPhase ? phase + 2 - lastPhase : phase - lastPhase;
            // Plus d'un aller depuis la dernière image (la fenêtre était fermée) : rien à rejouer.
            if (move < 1) {
                int gaps = game.lineTicks() - 1;
                for (int line = 0; line < game.lineCount(); line++) {
                    for (int ball = 0; ball < game.lineBalls(); ball++) {
                        double start = lastPhase + game.lineOffset(line, ball);
                        long first = (long) Math.floor(start * gaps + 1e-9) + 1, last = (long) Math.floor((start + move) * gaps + 1e-9);
                        for (long m = first; m <= last && touches.size() < 400; m++) {
                            double unfolded = (double) (m % (2L * gaps)) / gaps;
                            Touch touch = new Touch();
                            touch.line = line;
                            touch.at = unfolded <= 1 ? unfolded : 2 - unfolded;
                            touches.add(touch);
                        }
                    }
                }
            }
        }
        lastPhase = phase;
    }

    private Color color() {
        return PALETTE[dimension.ordinal()];
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = CanvasPane.begin(this, BACK);
        if (g == null) return;
        double cx = w / 2, cy = h / 2, u = Math.min(w, h);
        switch (dimension) {
            case LINE -> line(g, w, cx, cy, u);
            case PLANE -> plane(g, w, h, cx, cy, u);
            case VOLUME -> volumeScene(g, w, h, cx, cy, u);
            case DURATION -> {
                if (game.isDurationOpen()) durationScene(g, cx, cy, u);
                else duration(g, w, cx, cy, u);
            }
            case LOOP -> loopScene(g, cx, cy);
            case POSSIBLES -> possibles(g, w, cx, cy, u);
            case BEGINNINGS -> beginnings(g, w, h, u);
            case WEAVE -> weave(g, cx, cy, u);
            case LAWS -> laws(g, cx, cy, u);
            case POINT -> point(g, w, h, cx, cy, u);
        }
        g.setGlobalAlpha(1);
    }

    // 1. La ligne (version 0.101) : les boules du jeu vont et viennent, un trait à chaque bout et d'autres à
    // égale distance ; chaque trait touché s'allume, et ses éclats montent. Avec l'amélioration (0.102),
    // plusieurs lignes l'une sous l'autre, et plusieurs boules sur chacune.
    private void line(GraphicsContext g, double w, double cx, double cy, double u) {
        int lines = game.lineCount();
        double h = getHeight(), spacing = Math.min(h * 0.8 / lines, 140);
        for (int l = 0; l < lines; l++) oneLine(g, w, cy + (l - (lines - 1) / 2.0) * spacing, u, l);
    }

    private void oneLine(GraphicsContext g, double w, double cy, double u, int index) {
        Color c = color();
        double x0 = w * 0.1, x1 = w * 0.9;
        g.setStroke(c);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.35);
        g.strokeLine(x0, cy, x1, cy);
        int gaps = game.lineTicks() - 1;
        for (int k = 0; k <= gaps; k++) {
            double at = (double) k / gaps, x = x0 + (x1 - x0) * at;
            double lit = 0;
            for (Touch touch : touches) if (touch.line == index && Math.abs(touch.at - at) < 1e-9) lit = Math.max(lit, 1 - touch.age / 0.6);
            boolean end = k == 0 || k == gaps;
            double half = (end ? 16 : 10) + 6 * Math.max(0, lit);
            g.setLineWidth(end ? 3 : 2);
            g.setStroke(lit > 0 ? Color.WHITE : c);
            g.setGlobalAlpha(0.55 + 0.45 * Math.max(0, lit));
            g.strokeLine(x, cy - half, x, cy + half);
            if (lit > 0) UniverseView.glow(g, x, cy, u * 0.035 * lit + 4, c, 0.8 * lit);
        }
        // Les éclats de chaque trait touché montent et s'effacent.
        g.setFont(Font.font(13));
        g.setTextAlign(TextAlignment.CENTER);
        String gain = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.lineShardsPerTouch()));
        for (Touch touch : touches) {
            if (touch.line != index) continue;
            double x = x0 + (x1 - x0) * touch.at;
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - touch.age / 1.2));
            g.fillText(gain, x, cy - 26 - touch.age * 30);
        }
        // Les boules, chacune avec sa traînée le long de la route qu'elle vient de faire.
        double phase = game.state().linePhase(), trail = 0.012 * game.lineSpeed();
        for (int ball = 0; ball < game.lineBalls(); ball++) {
            double own = (phase + game.lineOffset(index, ball)) % 2;
            for (int k = 14; k >= 1; k--) {
                double back = own - k * trail;
                if (back < 0) back += 2;
                double pos = back <= 1 ? back : 2 - back;
                UniverseView.glow(g, x0 + (x1 - x0) * pos, cy, u * 0.03 * (1 - k / 16.0), c, 0.8 * (1 - k / 15.0));
            }
            double x = x0 + (x1 - x0) * game.linePosition(index, ball);
            UniverseView.glow(g, x, cy, u * 0.06, c, 0.9);
            g.setGlobalAlpha(1);
            g.setFill(Color.WHITE);
            g.fillOval(x - 5, cy - 5, 10, 10);
        }
    }

    // 2. Le plan : la grille vue en perspective (version 0.081), où le joueur mène le point (Z Q S D,
    // version 0.104) ; sa traînée dessine la courbe de son passage, et les éclats attendent sur la grille.
    private void plane(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        Color c = color();
        double top = h * 0.25, bottom = h * 0.9;
        g.setStroke(c);
        g.setLineWidth(1);
        for (int k = 0; k <= 12; k++) {
            double t = k / 12.0;
            double y = top + (bottom - top) * t * t;
            double half = w * (0.18 + 0.32 * t * t);
            g.setGlobalAlpha(0.12 + 0.25 * t);
            g.strokeLine(cx - half, y, cx + half, y);
        }
        for (int k = -8; k <= 8; k++) {
            g.setGlobalAlpha(0.2);
            g.strokeLine(cx + k * w * 0.0225, top, cx + k * w * 0.0625, bottom);
        }
        // Les éclats : des losanges posés sur la grille, plus petits au loin.
        int k = 0;
        for (Game.PlaneShard shard : game.planeShardsOnBoard()) {
            double[] at = onPlane(w, h, cx, shard.x(), shard.y());
            double r = Math.max(3, Game.PLANE_SHARD_RADIUS * w * 0.4 * at[2]) * (1 + 0.15 * Math.sin(time * 4 + k++));
            UniverseView.glow(g, at[0], at[1], r * 2.6, SHARD, 0.6);
            g.setGlobalAlpha(1);
            g.setFill(SHARD);
            g.fillPolygon(new double[] {at[0], at[0] + r, at[0], at[0] - r}, new double[] {at[1] - r * 1.3, at[1], at[1] + r * 1.3, at[1]}, 4);
        }
        // La traînée : la courbe que dessine le point en passant.
        if (trailCount > 1) {
            double[] xs = new double[trailCount], ys = new double[trailCount];
            for (int i = 0; i < trailCount; i++) {
                int at = Math.floorMod(trailHead - 1 - i, TRAIL);
                double[] q = onPlane(w, h, cx, trailX[at], trailY[at]);
                xs[i] = q[0];
                ys[i] = q[1];
            }
            g.setStroke(Color.WHITE);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.7);
            g.strokePolyline(xs, ys, trailCount);
        }
        // Ce qui vient d'être ramassé : un éclat et le gain qui monte.
        g.setFont(Font.font(14));
        g.setTextAlign(TextAlignment.CENTER);
        for (Pickup pickup : pickups) {
            double t = pickup.age / 0.9;
            UniverseView.glow(g, pickup.x, pickup.y, 10 + 30 * t, SHARD, 0.7 * (1 - t));
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.fillText(pickup.text, pickup.x, pickup.y - 18 - 30 * t);
        }
        // Le point du joueur, à sa taille, plus petit au loin.
        double[] p = onPlane(w, h, cx, game.planeX(), game.planeY());
        // Un plancher bas : au départ, le point est petit, et la taille se voit grandir à chaque niveau.
        double pr = Math.max(2.5, game.planePlayerRadius() * w * 0.4 * p[2]);
        UniverseView.glow(g, p[0], p[1], pr * 2.6 + 4, c, 0.9);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(p[0] - pr * 0.6, p[1] - pr * 0.6, pr * 1.2, pr * 1.2);
        g.setStroke(c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.8);
        g.strokeOval(p[0] - pr, p[1] - pr, pr * 2, pr * 2);
    }

    /**
     * Un point du plan (−1 à 1 dans les deux sens) sur la grille en perspective : x et y à l'écran,
     * et l'échelle à cette profondeur (1 au premier plan).
     */
    private static double[] onPlane(double w, double h, double cx, double x, double y) {
        double top = h * 0.25, bottom = h * 0.9;
        double depth = (y + 1) / 2;
        double half = w * (0.18 + 0.32 * depth * depth);
        return new double[] {cx + x * half * 0.9, top + (bottom - top) * depth * depth, half / (w * 0.5)};
    }

    // 3. Un cube qui tourne, et un plus petit dedans, dans l'autre sens.
    private void volume(GraphicsContext g, double cx, double cy, double size, double angle, double alpha) {
        cube(g, cx, cy, size, angle, angle * 0.7, color(), alpha);
        cube(g, cx, cy, size * 0.45, -angle * 1.3, angle * 0.5, Color.WHITE, alpha * 0.8);
    }

    private static void cube(GraphicsContext g, double cx, double cy, double size, double a, double b, Color c, double alpha) {
        double[][] p = new double[8][2];
        for (int k = 0; k < 8; k++) {
            double x = (k & 1) == 0 ? -1 : 1, y = (k & 2) == 0 ? -1 : 1, z = (k & 4) == 0 ? -1 : 1;
            double x1 = x * Math.cos(a) - z * Math.sin(a), z1 = x * Math.sin(a) + z * Math.cos(a);
            double y1 = y * Math.cos(b) - z1 * Math.sin(b), z2 = y * Math.sin(b) + z1 * Math.cos(b);
            double depth = 5 / (5 + z2);
            p[k][0] = cx + x1 * size * depth;
            p[k][1] = cy + y1 * size * depth;
        }
        g.setStroke(c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.8 * alpha);
        for (int k = 0; k < 8; k++) {
            for (int bit : new int[] {1, 2, 4}) {
                int j = k ^ bit;
                if (j > k) g.strokeLine(p[k][0], p[k][1], p[j][0], p[j][1]);
            }
        }
        g.setFill(c);
        for (double[] q : p) g.fillOval(q[0] - 2.5, q[1] - 2.5, 5, 5);
    }

    // 4. Le cube à chaque instant, mis bout à bout le long du temps ; « maintenant » avance.
    private void duration(GraphicsContext g, double w, double cx, double cy, double u) {
        int copies = 9;
        double now = (time * 0.15) % 1;
        g.setStroke(color());
        g.setLineWidth(1);
        g.setGlobalAlpha(0.3);
        g.strokeLine(w * 0.06, cy + u * 0.2, w * 0.94, cy + u * 0.2);
        for (int k = 0; k < copies; k++) {
            double t = k / (copies - 1.0);
            double x = w * (0.1 + 0.8 * t);
            double near = 1 - Math.min(1, Math.abs(t - now) * 4);
            cube(g, x, cy, u * 0.05 * (0.8 + 0.4 * near), t * 4, t * 2, color(), 0.25 + 0.75 * near);
        }
        double x = w * (0.1 + 0.8 * now);
        g.setStroke(Color.WHITE);
        g.setGlobalAlpha(0.8);
        g.strokeLine(x, cy - u * 0.15, x, cy + u * 0.22);
    }

    // ------------------------------------------------------------------
    // La durée (version 0.134) : le rond, le bouton, les spirales
    // ------------------------------------------------------------------

    private double durationRing() {
        return Math.min(getWidth(), getHeight()) * DURATION_RING;
    }

    private double durationButton() {
        return Math.min(getWidth(), getHeight()) * DURATION_BUTTON;
    }

    /** Vrai si ce point de la vue est sur le bouton du milieu. */
    public boolean overDurationButton(double x, double y) {
        return Math.hypot(x - getWidth() / 2, y - getHeight() / 2) <= durationButton();
    }

    /** Un clic sur le bouton du milieu : les spirales partent du bord. Ailleurs dans le rond, rien. */
    private boolean pressDuration(double x, double y) {
        if (!game.isDurationOpen() || !overDurationButton(x, y)) return false;
        return game.holdDuration();
    }

    /**
     * Le bouton relâché (la souris, la barre d'espace, la fenêtre qui se ferme) : les spirales donnent leurs éclats,
     * qui montent du bouton. Vrai s'il était tenu. Tant que l'appui automatique le tient (version 0.150), le joueur ne
     * le relâche pas : c'est l'automatisme qui relâche, au centre.
     */
    public boolean releaseHold() {
        if (dimension != Dimension.DURATION || !game.isDurationHeld()
                || game.isDimensionAutomationRunning(idle.core.universe.DimensionAutomation.DURATION_HOLD)) return false;
        durationPickup(game.releaseDuration());
        lastReleases = game.durationReleases();
        return true;
    }

    /** Les éclats d'un relâchement montent du bouton ; rien pour un relâchement qui n'a rien donné. */
    private void durationPickup(double reward) {
        if (reward <= 0 || durationPickups.size() >= 12) return;
        Pickup mark = new Pickup();
        mark.x = getWidth() / 2;
        mark.y = getHeight() / 2;
        mark.text = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(reward));
        durationPickups.add(mark);
    }

    /** Les gains de la durée qui montent encore : pour les vérifications. */
    public int durationPickupsShown() {
        return durationPickups.size();
    }

    /**
     * Le rond et ses spirales : chacune part du bord et s'enroule jusqu'au bouton ; la part déjà faite brille, la
     * pointe avance tant qu'on tient. Au centre, le bouton, son anneau qui se remplit, et ce que donnerait un
     * relâchement maintenant.
     */
    private void durationScene(GraphicsContext g, double cx, double cy, double u) {
        Color c = color();
        double ring = durationRing(), button = durationButton(), progress = game.durationProgress();
        boolean held = game.isDurationHeld();
        // Le rond.
        UniverseView.glow(g, cx, cy, ring * 1.08, c, 0.12);
        g.setStroke(c);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.7);
        g.strokeOval(cx - ring, cy - ring, ring * 2, ring * 2);
        // Les spirales : au plus DURATION_DRAWN_TURNS tours dessinés, quoi que vaille la vitesse.
        int spirals = game.durationSpirals();
        double turns = Math.max(0.5, Math.min(DURATION_DRAWN_TURNS, game.durationTurnsMax()));
        double spin = time * 0.08;
        int steps = (int) Math.ceil(turns * 72);
        for (int k = 0; k < spirals; k++) {
            double start = spin + Math.PI * 2 * k / spirals;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double f = s / (double) steps, angle = start + f * turns * Math.PI * 2, r = ring - (ring - button * 1.3) * f;
                xs[s] = cx + Math.cos(angle) * r;
                ys[s] = cy + Math.sin(angle) * r;
            }
            g.setLineWidth(1);
            g.setGlobalAlpha(0.22);
            g.strokePolyline(xs, ys, steps + 1);
            int done = (int) Math.round(progress * steps);
            if (done > 0) {
                g.setLineWidth(2.4);
                g.setGlobalAlpha(0.95);
                g.strokePolyline(xs, ys, done + 1);
                UniverseView.glow(g, xs[done], ys[done], 14, c, 0.9);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(1);
                g.fillOval(xs[done] - 3, ys[done] - 3, 6, 6);
            } else {
                g.setFill(c);
                g.setGlobalAlpha(0.8);
                g.fillOval(xs[0] - 3, ys[0] - 3, 6, 6);
            }
        }
        // Le bouton : un anneau qui se remplit avec le temps tenu, et qui bat une fois au centre.
        boolean full = held && progress >= 1;
        double beat = full ? 0.5 + 0.5 * Math.sin(time * 8) : 0;
        UniverseView.glow(g, cx, cy, button * (1.8 + 0.6 * beat), c, held ? 0.6 + 0.3 * beat : 0.3);
        g.setGlobalAlpha(1);
        g.setFill(held ? c.deriveColor(0, 1, 0.55, 1) : Color.web("#141a2c"));
        g.fillOval(cx - button, cy - button, button * 2, button * 2);
        g.setStroke(c);
        g.setLineWidth(2);
        g.strokeOval(cx - button, cy - button, button * 2, button * 2);
        if (held) {
            g.setLineWidth(4);
            g.strokeArc(cx - button - 6, cy - button - 6, button * 2 + 12, button * 2 + 12, 90, -360 * progress,
                    javafx.scene.shape.ArcType.OPEN);
        }
        g.setFill(Color.WHITE);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(14));
        g.fillText(!held ? "Tenir" : full ? "Relâchez" : String.format(java.util.Locale.ROOT, "%.1f s", game.durationHeldSeconds()),
                cx, cy + 5);
        if (held) {
            g.setFont(Font.font(16));
            g.fillText("+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.durationPending())), cx, cy - button - 18);
        }
        // Les éclats donnés au relâchement montent du bouton.
        for (Pickup pickup : durationPickups) {
            double t = pickup.age / 1.4;
            UniverseView.glow(g, pickup.x, pickup.y, button + 60 * t, c, 0.7 * (1 - t));
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFont(Font.font(20));
            g.fillText(pickup.text, pickup.x, pickup.y - button - 24 - 50 * t);
        }
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // La boucle (version 0.153) : les cercles, leurs points, leurs zones
    // ------------------------------------------------------------------

    /** Le premier cercle (dehors) et le dernier possible (dedans), relatifs au plus petit côté de la vue. */
    private static final double LOOP_OUTER = 0.42, LOOP_INNER = 0.1;
    /** Les couleurs des trois parts d'une zone : le bord « mauvais », le « bien », le centre « excellent ». */
    public static final Color LOOP_BAD = Color.web("#b0566e"), LOOP_GOOD = Color.web("#ff9eb5"), LOOP_EXCELLENT = Color.web("#fff1c9");
    private static final Color LOOP_MISS = Color.web("#8a93b0");
    /** Durée de vie du mot d'un clic, qui monte du point et s'efface, en secondes. */
    private static final double LOOP_MARK_SECONDS = 0.9;

    /** Le mot d'un clic sur un cercle (ou le gain du rythme automatique, au centre) : où il est né, et son âge. */
    private static final class LoopMark {
        double x, y, age;
        String text, gain;
        Game.LoopHit hit;
    }

    private final List<LoopMark> loopMarks = new ArrayList<>();
    /** Les clics qui ont rapporté, que la vue a déjà montrés : ce que la partie compte de plus vient du rythme automatique. */
    private long lastLoopHits = -1;
    /** Où était le point du premier cercle à l'image d'avant : pour entendre ses passages sur le centre d'une zone. */
    private double lastBeatPhase = Double.NaN;
    /** Le métronome : ce qu'il joue à chaque battement ; {@code null} quand il est coupé. */
    private Runnable onBeat;
    private long beats;
    /** L'éclat de chaque cercle juste joué, de 1 à 0, et ce que le clic a valu. */
    private final double[] ringFlash = new double[Game.MAX_RINGS];
    private final Game.LoopHit[] ringFlashHit = new Game.LoopHit[Game.MAX_RINGS];

    /** Le métronome de la boucle : ce qu'il joue à chaque battement, ou {@code null} pour le couper. */
    public void setOnBeat(Runnable beat) {
        onBeat = beat;
    }

    /** Les battements du métronome depuis l'ouverture du jeu : pour les vérifications. */
    public long beats() {
        return beats;
    }

    /** Les mots de clics qui montent encore : pour les vérifications. */
    public int loopMarksShown() {
        return loopMarks.size();
    }

    /** Le texte du dernier mot de clic, ou rien : pour les vérifications. */
    public String lastLoopMark() {
        return loopMarks.isEmpty() ? "" : loopMarks.get(loopMarks.size() - 1).text;
    }

    /** L'écart entre deux cercles voisins : sept tiennent entre le premier et le dernier rayon. */
    private double loopGap() {
        return Math.min(getWidth(), getHeight()) * (LOOP_OUTER - LOOP_INNER) / (Game.MAX_RINGS - 1);
    }

    /** Le rayon du cercle {@code ring} : le premier dehors, les suivants vers le centre. */
    public double loopRadius(int ring) {
        return Math.min(getWidth(), getHeight()) * LOOP_OUTER - ring * loopGap();
    }

    /** Le cercle sous ce point de la vue (à une demi-largeur d'écart près), ou −1. */
    public int loopRingAt(double x, double y) {
        double d = Math.hypot(x - getWidth() / 2, y - getHeight() / 2), reach = Math.max(10, loopGap() / 2);
        int best = -1;
        for (int ring = 0; ring < game.loopRings(); ring++) {
            double off = Math.abs(d - loopRadius(ring));
            if (off <= reach && (best < 0 || off < Math.abs(d - loopRadius(best)))) best = ring;
        }
        return best;
    }

    /** Où est le point du cercle {@code ring} à l'écran. */
    public double[] loopPoint(int ring) {
        double a = -Math.PI / 2 + game.loopPhase(ring) * Math.PI * 2, r = loopRadius(ring);
        return new double[] {getWidth() / 2 + Math.cos(a) * r, getHeight() / 2 + Math.sin(a) * r};
    }

    /** Un clic dans la vue de la boucle : il joue le cercle sous la souris. Vrai s'il en a touché un. */
    private boolean pressLoop(double x, double y) {
        if (!game.isLoopOpen()) return false;
        int ring = loopRingAt(x, y);
        if (ring < 0) return false;
        clickRing(ring);
        return true;
    }

    /**
     * Joue le cercle {@code ring}, à la souris ou par sa touche : ce que le clic vaut monte du point (« Excellent »,
     * « Bien », « Mauvais » ou « Raté », et le gain), et le cercle s'éclaire de sa couleur. Un clic pendant l'attente du
     * cercle ne compte pas : rien ne monte.
     */
    public Game.LoopHit clickRing(int ring) {
        Game.LoopHit hit = game.clickLoop(ring);
        lastLoopHits = game.loopHits();
        if (hit == Game.LoopHit.WAIT || ring < 0 || ring >= Game.MAX_RINGS) return hit;
        double[] at = loopPoint(ring);
        LoopMark mark = new LoopMark();
        mark.x = at[0];
        mark.y = at[1];
        mark.hit = hit;
        mark.text = hitWord(hit);
        mark.gain = hit == Game.LoopHit.MISS ? "" : "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.loopReward(hit)));
        if (loopMarks.size() >= 20) loopMarks.remove(0);
        loopMarks.add(mark);
        ringFlash[ring] = 1;
        ringFlashHit[ring] = hit;
        return hit;
    }

    /** Le mot d'un clic : « Excellent », « Bien », « Mauvais », « Raté » ; rien pour un clic qui attendait. */
    public static String hitWord(Game.LoopHit hit) {
        return switch (hit) {
            case EXCELLENT -> "Excellent";
            case GOOD -> "Bien";
            case BAD -> "Mauvais";
            case MISS -> "Raté";
            case WAIT -> "";
        };
    }

    private static Color hitColor(Game.LoopHit hit) {
        return switch (hit) {
            case EXCELLENT -> LOOP_EXCELLENT;
            case GOOD -> LOOP_GOOD;
            case BAD -> LOOP_BAD;
            case MISS, WAIT -> LOOP_MISS;
        };
    }

    /**
     * La boucle avance d'une image : les mots montent, les éclats des cercles s'éteignent, les clics du rythme
     * automatique font monter leurs nœuds du centre, et le métronome bat si le point du premier cercle a passé le centre
     * d'une zone depuis l'image d'avant.
     */
    private void runLoop(double elapsed) {
        for (LoopMark mark : loopMarks) mark.age += elapsed;
        loopMarks.removeIf(mark -> mark.age > LOOP_MARK_SECONDS);
        for (int ring = 0; ring < ringFlash.length; ring++) ringFlash[ring] = Math.max(0, ringFlash[ring] - elapsed / 0.4);
        if (!game.isLoopOpen()) return;
        long hits = game.loopHits();
        if (lastLoopHits >= 0 && hits > lastLoopHits && loopMarks.size() < 20) {
            LoopMark mark = new LoopMark();
            mark.x = getWidth() / 2;
            mark.y = getHeight() / 2;
            mark.hit = Game.LoopHit.GOOD;
            mark.text = "";
            mark.gain = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.loopReward(Game.LoopHit.GOOD) * (hits - lastLoopHits)));
            loopMarks.add(mark);
        }
        lastLoopHits = hits;
        double phase = game.loopPhase(0);
        if (!Double.isNaN(lastBeatPhase) && onBeat != null) {
            double move = phase - lastBeatPhase;
            if (move < 0) move += 1;
            // Plus d'un demi-tour depuis l'image d'avant (la fenêtre était fermée) : rien à rattraper.
            if (move > 0 && move < 0.5) {
                for (double center : game.loopZones(0)) {
                    double ahead = center - lastBeatPhase;
                    ahead -= Math.floor(ahead);
                    if (ahead > 0 && ahead <= move) {
                        beats++;
                        onBeat.run();
                        break;
                    }
                }
            }
        }
        lastBeatPhase = phase;
    }

    /**
     * Les cercles, leurs zones et leurs points. Chaque zone se dessine en trois arcs l'un sur l'autre, du bord au centre ;
     * le point brille davantage quand il est dans une zone, et pendant l'attente du cercle il pâlit, entouré d'un anneau
     * qui se remplit. Fermée, la boucle montre un seul cercle, sans zone, dont le point tourne au rythme du départ.
     */
    private void loopScene(GraphicsContext g, double cx, double cy) {
        Color c = color();
        boolean open = game.isLoopOpen();
        int rings = open ? game.loopRings() : 1;
        double band = Math.max(6, Math.min(16, loopGap() * 0.42)), half = game.loopZoneHalfWidth();
        UniverseView.glow(g, cx, cy, loopRadius(rings - 1) * 0.6, c, 0.15);
        for (int ring = 0; ring < rings; ring++) {
            double r = loopRadius(ring), flash = ringFlash[ring];
            // Le cercle, qui s'éclaire un instant de la couleur du dernier clic.
            g.setStroke(flash > 0 && ringFlashHit[ring] != null ? hitColor(ringFlashHit[ring]) : c);
            g.setLineWidth(2 + 2 * flash);
            g.setGlobalAlpha(Math.min(1, 0.35 + 0.5 * flash));
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
            if (open) {
                for (double center : game.loopZones(ring)) {
                    zoneArc(g, cx, cy, r, center, half, band, LOOP_BAD, 0.75);
                    zoneArc(g, cx, cy, r, center, half * Game.LOOP_GOOD, band, LOOP_GOOD, 0.9);
                    zoneArc(g, cx, cy, r, center, half * Game.LOOP_EXCELLENT, band, LOOP_EXCELLENT, 1);
                }
            }
            double phase = open ? game.loopPhase(ring) : (time * Game.LOOP_TURNS) % 1;
            double a = -Math.PI / 2 + phase * Math.PI * 2, x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
            double wait = open ? game.loopWait(ring) : 0;
            boolean inside = open && wait <= 0 && game.loopAim(ring) != Game.LoopHit.MISS;
            UniverseView.glow(g, x, y, band * (inside ? 2.4 : 1.6), inside ? Color.WHITE : c, wait > 0 ? 0.35 : 0.9);
            g.setGlobalAlpha(wait > 0 ? 0.5 : 1);
            g.setFill(Color.WHITE);
            double dot = Math.max(3, band * 0.38);
            g.fillOval(x - dot, y - dot, dot * 2, dot * 2);
            if (wait > 0) {
                // L'attente du cercle : un anneau autour du point, qui se remplit jusqu'au prochain clic compté.
                double done = 1 - Math.min(1, wait / Game.LOOP_COOLDOWN), ring2 = dot + 5;
                g.setGlobalAlpha(0.3);
                g.setStroke(Color.WHITE);
                g.setLineWidth(2);
                g.strokeOval(x - ring2, y - ring2, ring2 * 2, ring2 * 2);
                g.setGlobalAlpha(0.95);
                g.setStroke(c);
                g.strokeArc(x - ring2, y - ring2, ring2 * 2, ring2 * 2, 90, -360 * done, javafx.scene.shape.ArcType.OPEN);
            }
        }
        // Ce qu'ont valu les derniers clics : le mot et le gain montent du point, et s'effacent.
        g.setTextAlign(TextAlignment.CENTER);
        for (LoopMark mark : loopMarks) {
            double t = mark.age / LOOP_MARK_SECONDS;
            Color tint = hitColor(mark.hit);
            if (!mark.text.isEmpty()) UniverseView.glow(g, mark.x, mark.y, 8 + 22 * t, tint, 0.6 * (1 - t));
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFill(tint);
            g.setFont(Font.font(15));
            if (!mark.text.isEmpty()) g.fillText(mark.text, mark.x, mark.y - 16 - 26 * t);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(12));
            if (!mark.gain.isEmpty()) g.fillText(mark.gain, mark.x, mark.y - (mark.text.isEmpty() ? 6 : 32) - 26 * t);
        }
        g.setGlobalAlpha(1);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);
    }

    /** Un arc de zone : de {@code center − half} à {@code center + half} tours, dans le sens du point. */
    private static void zoneArc(GraphicsContext g, double cx, double cy, double r, double center, double half, double band,
            Color color, double alpha) {
        // Le point part du haut et tourne dans le sens des aiguilles d'une montre ; l'arc de JavaFX compte à l'envers.
        double start = 90 - (center - half) * 360, extent = -half * 2 * 360;
        g.setStroke(color);
        g.setLineWidth(band);
        g.setGlobalAlpha(alpha);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
        g.strokeArc(cx - r, cy - r, r * 2, r * 2, start, extent, javafx.scene.shape.ArcType.OPEN);
    }

    // 6. Toutes les suites d'un même début, en éventail, et des passages qui sautent de l'une à l'autre.
    private void possibles(GraphicsContext g, double w, double cx, double cy, double u) {
        double x0 = w * 0.1;
        int count = 22;
        double[] ends = new double[count];
        for (int k = 0; k < count; k++) {
            double t = k / (count - 1.0);
            double end = cy + (t - 0.5) * u * 0.8;
            ends[k] = end;
            int steps = 24;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double f = s / (double) steps;
                xs[s] = x0 + (w * 0.8) * f;
                ys[s] = cy + (end - cy) * f + Math.sin(f * 9 + k + time) * 4 * f;
            }
            g.setStroke(PALETTE[k % PALETTE.length]);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.45);
            g.strokePolyline(xs, ys, steps + 1);
        }
        // Les passages : des arcs qui relient deux suites, et un éclat qui les emprunte.
        for (int k = 0; k < 6; k++) {
            int a = (int) (UniverseView.noise(k * 5 + 1) * count), b = (int) (UniverseView.noise(k * 5 + 2) * count);
            double x = x0 + w * 0.8 * (0.4 + 0.5 * UniverseView.noise(k * 5 + 3));
            double ya = cy + (ends[a] - cy) * (x - x0) / (w * 0.8), yb = cy + (ends[b] - cy) * (x - x0) / (w * 0.8);
            int steps = 16;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double f = s / (double) steps;
                xs[s] = x + Math.sin(f * Math.PI) * u * 0.08;
                ys[s] = ya + (yb - ya) * f;
            }
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.2);
            g.setGlobalAlpha(0.6);
            g.strokePolyline(xs, ys, steps + 1);
            double f = (time * 0.5 + k * 0.17) % 1;
            UniverseView.glow(g, x + Math.sin(f * Math.PI) * u * 0.08, ya + (yb - ya) * f, 5, Color.WHITE, 0.9);
        }
        UniverseView.glow(g, x0, cy, u * 0.05, color(), 0.9);
    }

    // 7. Plusieurs débuts, chacun avec son éventail de possibles.
    private void beginnings(GraphicsContext g, double w, double h, double u) {
        int roots = 5;
        for (int r = 0; r < roots; r++) {
            double y = h * (0.15 + 0.7 * r / (roots - 1.0));
            double x = w * (0.08 + 0.04 * Math.sin(time * 0.6 + r));
            Color c = PALETTE[(r * 3) % PALETTE.length];
            for (int k = 0; k < 9; k++) {
                double t = k / 8.0 - 0.5;
                int steps = 18;
                double[] xs = new double[steps + 1], ys = new double[steps + 1];
                for (int s = 0; s <= steps; s++) {
                    double f = s / (double) steps;
                    xs[s] = x + w * 0.82 * f;
                    ys[s] = y + t * h * 0.16 * f + Math.sin(f * 7 + time * 0.8 + k + r) * 3 * f;
                }
                g.setStroke(c);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.4);
                g.strokePolyline(xs, ys, steps + 1);
            }
            UniverseView.glow(g, x, y, u * 0.035, c, 0.9);
        }
    }

    // 8. Tous les débuts en cercle, chacun son éventail tourné vers le milieu, et tous reliés.
    private void weave(GraphicsContext g, double cx, double cy, double u) {
        int roots = 12;
        double r = u * 0.38;
        for (int k = 0; k < roots; k++) {
            double a = 2 * Math.PI * k / roots + time * 0.08;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            Color c = PALETTE[k % PALETTE.length];
            for (int j = 0; j < 5; j++) {
                double spread = (j - 2) * 0.12;
                double tx = cx + r * 0.25 * Math.cos(a + Math.PI + spread), ty = cy + r * 0.25 * Math.sin(a + Math.PI + spread);
                g.setStroke(c);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.35);
                g.strokeLine(x, y, tx, ty);
            }
            int next = (k + 5) % roots;
            double b = 2 * Math.PI * next / roots + time * 0.08;
            g.setStroke(Color.WHITE);
            g.setGlobalAlpha(0.12);
            g.strokeLine(x, y, cx + r * Math.cos(b), cy + r * Math.sin(b));
            UniverseView.glow(g, x, y, u * 0.03, c, 0.9);
        }
        UniverseView.glow(g, cx, cy, u * 0.12, color(), 0.35);
    }

    // 9. Des anneaux, chacun avec ses lois : on tourne, on s'arrête, on spirale, on tremble.
    private void laws(GraphicsContext g, double cx, double cy, double u) {
        int rings = 6;
        for (int ring = 0; ring < rings; ring++) {
            double r = u * (0.08 + 0.065 * ring);
            Color c = PALETTE[(ring * 2 + 1) % PALETTE.length];
            g.setStroke(c);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.25);
            g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            int dots = 6 + ring * 3;
            for (int k = 0; k < dots; k++) {
                double base = 2 * Math.PI * k / dots;
                double a, d = r;
                switch (ring % 4) {
                    case 0 -> a = base + time * 0.8;                                      // ça tourne
                    case 1 -> a = base;                                                   // rien ne bouge
                    case 2 -> {                                                           // ça spirale
                        a = base + time * 0.4;
                        d = r * (0.85 + 0.15 * Math.sin(time * 2 + k));
                    }
                    default -> {                                                          // ça tremble
                        a = base - time * 0.3;
                        d = r + 3 * Math.sin(time * 17 + k * 3);
                    }
                }
                double size = 2 + (ring % 3);
                g.setFill(c);
                g.setGlobalAlpha(0.9);
                g.fillOval(cx + d * Math.cos(a) - size / 2, cy + d * Math.sin(a) - size / 2, size, size);
            }
        }
        UniverseView.glow(g, cx, cy, u * 0.06, color(), 0.8);
    }

    // 10. Tout en un point : les motifs des neuf autres tournent en s'y resserrant, et la lumière en sort.
    private void point(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        for (int k = 0; k < 48; k++) {
            double a = 2 * Math.PI * k / 48 + time * 0.05;
            g.setStroke(PALETTE[k % PALETTE.length]);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.15 + 0.1 * Math.sin(time * 2 + k));
            g.strokeLine(cx, cy, cx + Math.cos(a) * Math.max(w, h), cy + Math.sin(a) * Math.max(w, h));
        }
        for (int k = 0; k < 9; k++) {
            double phase = (time * 0.12 + k / 9.0) % 1;
            double r = u * 0.42 * (1 - phase);
            double a = 2 * Math.PI * k / 9 + time * 0.4 + phase * 3;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            Color c = PALETTE[k];
            if (k == 2) cube(g, x, y, u * 0.025 * (1 - phase) + 2, time, time * 0.7, c, 1 - phase * 0.5);
            else UniverseView.glow(g, x, y, u * 0.03 * (1 - phase) + 3, c, 0.9 * (1 - phase * 0.5));
        }
        UniverseView.glow(g, cx, cy, u * (0.1 + 0.02 * Math.sin(time * 3)), color(), 1);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(cx - 4, cy - 4, 8, 8);
    }
}
