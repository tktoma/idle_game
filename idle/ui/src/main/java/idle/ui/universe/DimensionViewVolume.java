package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.universe.SkinVisual;
import idle.ui.common.CanvasPane;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : la plongée dans le volume et le dessin de la ligne, du plan, du volume et de la durée. */
public abstract class DimensionViewVolume extends DimensionViewCamera {
    public DimensionViewVolume(Game game) {
        super(game);
    }

    // ------------------------------------------------------------------
    // Le volume : la plongée, le regard, la cueillette
    // ------------------------------------------------------------------

    /** Où en est la vue du volume : pour les vérifications et la fenêtre. */
    public Dive dive() { return dive; }

    /** Le regard dedans : pour les vérifications. */
    public double lookYaw() { return lookYaw; }

    public double lookPitch() { return lookPitch; }

    /** Les gains de boules cueillies qui montent encore : pour les vérifications. */
    public int volumePickupsShown() { return volumePickups.size(); }

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
    double outsideYaw() { return time * 0.6; }

    double outsidePitch() { return time * 0.6 * 0.7; }

    /** Le rayon, à l'écran, de la zone où un clic sur le cube du dehors y fait plonger. */
    double cubeReach() { return Math.min(getWidth(), getHeight()) * OUTSIDE_SIZE * 1.8; }

    boolean overCube(double x, double y) {
        return Math.hypot(x - getWidth() / 2, y - getHeight() / 2) <= cubeReach();
    }

    Cursor cursorAt(double x, double y) {
        if (dimension == Dimension.DURATION && game.isDurationOpen() && overDurationButton(x, y)) return Cursor.HAND;
        if (dimension == Dimension.LOOP && game.isLoopOpen() && loopRingAt(x, y) >= 0) return Cursor.HAND;
        if (dimension == Dimension.BRANE && game.isBraneOpen() && (braneHeld || overBrane(x, y) && game.canPullBrane())) {
            return braneHeld ? Cursor.CLOSED_HAND : Cursor.HAND;
        }
        if (dimension == Dimension.CLICKER && game.isClickerOpen() && overClicker(x, y)) return Cursor.HAND;
        if (dimension == Dimension.MIX && game.isMixOpen() && game.isMixing(2) && mixAt(x, y) != null) return Cursor.CROSSHAIR;
        if (dimension == Dimension.GRID && game.isGridOpen() && gridCellAt(x, y) != null) return Cursor.HAND;
        if (dimension == Dimension.MIRROR && game.isMirrorOpen()) {
            Game.MirrorCell cell = mirrorCellAt(x, y);
            if (cell != null && cell.column() >= game.mirrorHalf()) return Cursor.HAND;
        }
        if (dimension != Dimension.VOLUME || !game.isVolumeOpen()) return Cursor.DEFAULT;
        // La caméra libre cache la souris : la croix au centre de la vue la remplace.
        if (freeInside()) return robotRefused ? Cursor.CROSSHAIR : Cursor.NONE;
        if (dive == Dive.INSIDE) return dragging ? Cursor.CLOSED_HAND : Cursor.CROSSHAIR;
        return dive == Dive.OUTSIDE && overCube(x, y) ? Cursor.HAND : Cursor.DEFAULT;
    }

    /**
     * Un bouton de la souris enfoncé sur le volume : dehors, un clic gauche sur le cube y fait plonger ; dedans, le
     * gauche commence à faire tourner le regard et le droit cueille la boule visée. En caméra libre, le regard suit
     * déjà la souris : les deux boutons cueillent la boule sous la croix, au centre. Dans la durée, la boucle, la brane, le
     * clicker, la grille et le miroir, le bouton du milieu, le cercle, la membrane, la surface ou la case ; dans le
     * mélange, l'arène. Vrai si le geste a servi.
     */
    public boolean press(double x, double y, boolean secondary) {
        if (dimension == Dimension.DURATION) return gestured(pressDuration(x, y));
        if (dimension == Dimension.LOOP) return pressLoop(x, y);
        if (dimension == Dimension.BRANE) return !secondary && pressBrane(x, y);
        if (dimension == Dimension.CLICKER) return !secondary && gestured(pressClicker(x, y));
        if (dimension == Dimension.GRID) return !secondary && gestured(pressGrid(x, y));
        if (dimension == Dimension.MIRROR) return !secondary && gestured(pressMirror(x, y));
        if (dimension == Dimension.MIX) return pressMix(x, y, secondary);
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
        gesture();
        Pickup mark = new Pickup();
        if (hit != null) {
            // Cueillie à la main : elle a déjà son gain, la vue ne la compte pas parmi celles de la plongée automatique.
            born.remove(hit);
            lastVolumeCollected = game.volumeCollected();
            double[] at = camera.project(hit.x(), hit.y(), hit.z());
            mark.x = at == null ? x : at[0];
            mark.y = at == null ? y : at[1];
            mark.text = "+" + Format.darkMatter(BigNum.of(game.volumeValue()));
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

    double insideFocal() { return Math.min(getWidth(), getHeight()) * INSIDE_FOCAL; }

    /** Fait avancer la plongée ou la sortie, et suit le jeu : le cube qui rejette le joueur fait ressortir la vue. */
    void runDive(double elapsed) {
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
                    setCursor(Cursor.DEFAULT);
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
                // La plongée automatique a plongé sans clic : la vue plonge à son tour.
                if (game.isInVolume()) {
                    fromYaw = wrap(outsideYaw());
                    fromPitch = wrap(outsidePitch());
                    dive = Dive.ENTERING;
                    diveProgress = 0;
                }
            }
        }
        // Les boules nées depuis l'image d'avant : on note l'instant pour les faire grossir.
        Set<Game.VolumeBall> present = Collections.newSetFromMap(new IdentityHashMap<>());
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
                mark.text = "+" + Format.darkMatter(BigNum.of(game.volumeValue()));
                volumePickups.add(mark);
            }
        }
        lastVolumeCollected = collected;
        born.keySet().retainAll(present);
        for (Game.VolumeBall ball : game.volumeBalls()) born.putIfAbsent(ball, time);
    }

    /** Pose la caméra pour l'image : dehors, en train de plonger, dedans ou en train de ressortir. */
    void aimCamera() {
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

    /** Ce que la plongée a d'avancé, de 0 (dehors) à 1 (au centre) : pour estomper le petit cube du dedans. */
    double depthIn() {
        return switch (dive) {
            case OUTSIDE -> 0;
            case INSIDE -> 1;
            case ENTERING -> ease(diveProgress);
            case LEAVING -> 1 - ease(diveProgress);
        };
    }

    // 3. Le volume : dehors, le cube qui tourne et un plus petit dedans, dans l'autre sens ;
    // depuis la version 0.128, on y plonge, et l'on cueille de son centre les boules qui y apparaissent.
    void volumeScene(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        if (dive == Dive.OUTSIDE) {
            // Sous le skin, le cube du dehors est l'aquarium : des arêtes claires, et le petit cube blanc dedans.
            if (skinned) {
                cube(g, cx, cy, u * OUTSIDE_SIZE, time * 0.6, time * 0.6 * 0.7, AQUA_EDGE, 1);
                cube(g, cx, cy, u * OUTSIDE_SIZE * 0.45, -time * 0.6 * 1.3, time * 0.6 * 0.5, Color.WHITE, 0.8);
            } else {
                volume(g, cx, cy, u * OUTSIDE_SIZE, time * 0.6, 1);
            }
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
        Color c = color(), edge = skinned ? AQUA_EDGE : c;
        double inside = depthIn();
        // Les faces vues du dedans : une grille légère, pour sentir où l'on regarde.
        g.setLineWidth(1);
        g.setStroke(edge);
        g.setGlobalAlpha((skinned ? 0.2 : 0.12) * inside);
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
        int k = 0;
        for (double[] ball : shown) {
            double r = Math.max(2, ball[3]);
            if (skinned) {
                // Un poisson, de la robe de son rang, qui regarde vers le centre de la vue.
                Color robe = FISH[k++ % FISH.length];
                UniverseView.glow(g, ball[0], ball[1], r * 2.2, robe, 0.35);
                g.setGlobalAlpha(1);
                fish(g, ball[0], ball[1], r, ball[0] < w / 2, robe);
                continue;
            }
            UniverseView.glow(g, ball[0], ball[1], r * 2.4, c, 0.55);
            g.setGlobalAlpha(1);
            g.setFill(new RadialGradient(0, 0, ball[0] - r * 0.35, ball[1] - r * 0.35, r * 1.3, false,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(1, c)));
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

    /** Le minuteur de la plongée, en haut : une barre qui se vide, rouge dans les trois dernières secondes. */
    void timer(GraphicsContext g, double w, Color c) {
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
        g.fillText(String.format(Locale.ROOT, "%.1f s", Math.max(0, left)), w / 2, y + 26);
    }

    /** Un trait entre deux points du cube, coupé là où il passe derrière l'œil. */
    void segment(GraphicsContext g, double[] a, double[] b) {
        double[][] cut = camera.clip(camera.toView(a[0], a[1], a[2]), camera.toView(b[0], b[1], b[2]));
        if (cut == null) return;
        double[] p = camera.screen(cut[0]), q = camera.screen(cut[1]);
        if (p != null && q != null) g.strokeLine(p[0], p[1], q[0], q[1]);
    }

    /** Le petit cube blanc qui tourne dans l'autre sens, posé devant l'œil comme dehors, de plus en plus effacé. */
    void innerCube(GraphicsContext g, double alpha) {
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
    void notePickups(double elapsed) {
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
            pickup.text = "+" + Format.darkMatter(BigNum.of(game.planeValue() * (collected - lastCollected)));
            if (pickups.size() < 20) pickups.add(pickup);
            // Un éclat ramassé par le joueur sonne ; celui que l'automatisme ramasse, non.
            if (!game.isDimensionAutoPlayed(Dimension.PLANE.number())) gesture();
        }
        lastCollected = collected;
    }

    /** Les éclats ramassés à l'instant : pour les vérifications. */
    public int pickupsShown() { return pickups.size(); }

    /** Les traits que les boules ont touchés depuis l'image d'avant : chacun s'allume. */
    void noteTouches(double elapsed, boolean moving) {
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

    Color color() { return DimensionPalette.color(dimension.number()); }

    void draw() {
        double w = getWidth(), h = getHeight();
        skinned = game.chosenSkin(SkinVisual.DIMENSIONS[dimension.ordinal()]) != null;
        // La palette des dimensions : le skin choisi, lu à chaque image, vaut pour toutes les couleurs.
        DimensionPalette.setSkin(game.chosenSkin(SkinVisual.DIMENSION_PALETTE));
        GraphicsContext g = CanvasPane.begin(this, skinned ? SKIN_BACKS[dimension.ordinal()] : BACK);
        if (g == null) return;
        double cx = w / 2, cy = h / 2, u = Math.min(w, h);
        if (skinned) backdrop(g, w, h);
        switch (dimension) {
            case LINE -> line(g, w, cx, cy, u);
            case PLANE -> plane(g, w, h, cx, cy, u);
            case VOLUME -> volumeScene(g, w, h, cx, cy, u);
            case DURATION -> {
                if (game.isDurationOpen()) durationScene(g, cx, cy, u);
                else duration(g, w, cx, cy, u);
            }
            case LOOP -> loopScene(g, cx, cy);
            case BRANE -> braneScene(g, cx, cy);
            case CLICKER -> clickerScene(g, cx, cy);
            case GRID -> gridScene(g, w, h);
            case MIRROR -> mirrorScene(g, w, h);
            case MIX -> mixScene(g, w, h, cx, cy, u);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Le décor de fond d'un skin, derrière le jeu : le ciel de nuit sur la terre des rails, les carreaux et la marge du
     * cahier, l'eau, les bulles et le sable de l'aquarium, l'herbe derrière la cible, le ciel des bulles de savon. Les
     * autres skins dessinent leur décor dans leur scène, sur leur fond uni.
     */
    void backdrop(GraphicsContext g, double w, double h) {
        g.setGlobalAlpha(1);
        switch (dimension) {
            case LINE -> {
                g.setFill(RAILS_SKY);
                g.fillRect(0, 0, w, h);
                g.setFill(Color.WHITE);
                for (int k = 0; k < 40; k++) {
                    double x = UniverseView.noise(k) * w, y = UniverseView.noise(k + 100) * h * 0.45;
                    g.setGlobalAlpha(0.3 + 0.4 * UniverseView.noise(k + 200) * (0.6 + 0.4 * Math.sin(time * 1.5 + k)));
                    g.fillOval(x, y, 2, 2);
                }
            }
            case PLANE -> {
                // La feuille est le plan lui-même, couchée sur le bureau et vue en perspective comme la grille
                // du Classique ; ses carreaux fuient avec elle, plus serrés au loin. Avant : une feuille à plat, de face, sur
                // toute la vue, et la perspective tracée au crayon par-dessus : deux quadrillages qui se contredisaient.
                double cx = w / 2, top = h * 0.25, bottom = h * 0.9, lean = w * 0.32 / (bottom - top);
                double head = top - h * 0.1, headHalf = w * 0.18 - lean * (top - head), footHalf = w * 0.5 + lean * (h - bottom);
                g.setFill(new LinearGradient(0, head, 0, h, false, CycleMethod.NO_CYCLE,
                        new Stop(0, PAPER_FAR), new Stop(1, PAPER)));
                g.fillPolygon(new double[] {cx - headHalf, cx + headHalf, cx + footHalf, cx - footHalf}, new double[] {head, head, h, h}, 4);
                // Les petits carreaux : deux fois plus serrés que les traits du jeu, que plane() repasse en plus appuyé.
                g.setStroke(PAPER_SQUARE);
                g.setLineWidth(1);
                for (int k = 0; k <= 24; k++) {
                    double t = k / 24.0, y = top + (bottom - top) * t * t, half = w * (0.18 + 0.32 * t * t);
                    g.setGlobalAlpha(0.35 + 0.3 * t);
                    g.strokeLine(cx - half, y, cx + half, y);
                }
                g.setGlobalAlpha(0.5);
                for (int k = -16; k <= 16; k++) g.strokeLine(cx + k * w * 0.01125, top, cx + k * w * 0.03125, bottom);
                // La marge rouge suit une ligne de fuite, à gauche ; trois trous de classeur entre elle et le bord.
                g.setStroke(PAPER_MARGIN);
                g.setLineWidth(1.5);
                g.setGlobalAlpha(0.9);
                double marginTop = cx - 7 * w * 0.0225, marginSlope = -7 * w * 0.04 / (bottom - top);
                g.strokeLine(marginTop + marginSlope * (head - top), head, marginTop + marginSlope * (h - top), h);
                g.setFill(SKIN_BACKS[1]);
                for (double t : new double[] {0.3, 0.62, 0.9}) {
                    double y = top + (bottom - top) * t * t, x = cx - 7.5 * w * (0.0225 + 0.04 * t * t), r = 3 + 5 * t;
                    g.fillOval(x - r, y - r * 0.45, 2 * r, r * 0.9);
                }
                g.setGlobalAlpha(1);
            }
            case VOLUME -> {
                g.setFill(WATER);
                g.fillRect(0, 0, w, h);
                // Les bulles montent du sable, chacune à sa colonne et à son rythme, et reviennent du bas.
                g.setStroke(BUBBLE);
                g.setLineWidth(1);
                for (int k = 0; k < 24; k++) {
                    double r = 2 + 3 * UniverseView.noise(k + 300), speed = 20 + 30 * UniverseView.noise(k + 400);
                    double x = UniverseView.noise(k) * w + Math.sin(time * 2 + k) * 4;
                    double y = h - ((time * speed + UniverseView.noise(k + 500) * h) % (h + 20));
                    g.setGlobalAlpha(0.35);
                    g.strokeOval(x - r, y - r, 2 * r, 2 * r);
                }
                g.setGlobalAlpha(1);
                g.setFill(SAND);
                for (int k = 0; k < 12; k++) {
                    sandX[k] = w * k / 11;
                    sandY[k] = h - h * (0.04 + 0.025 * UniverseView.noise(k + 600));
                }
                sandX[12] = w;
                sandY[12] = h;
                sandX[13] = 0;
                sandY[13] = h;
                g.fillPolygon(sandX, sandY, 14);
            }
            case LOOP -> {
                g.setFill(GRASS);
                g.fillRect(0, 0, w, h);
            }
            case CLICKER -> {
                g.setFill(SKY);
                g.fillRect(0, 0, w, h);
            }
            default -> {
            }
        }
        g.setGlobalAlpha(1);
    }

    // 1. La ligne : les boules du jeu vont et viennent, un trait à chaque bout et d'autres à
    // égale distance ; chaque trait touché s'allume, et ses éclats montent. Avec l'amélioration (0.102),
    // plusieurs lignes l'une sous l'autre, et plusieurs boules sur chacune.
    void line(GraphicsContext g, double w, double cx, double cy, double u) {
        int lines = game.lineCount();
        double h = getHeight(), spacing = Math.min(h * 0.8 / lines, 140);
        for (int l = 0; l < lines; l++) oneLine(g, w, cy + (l - (lines - 1) / 2.0) * spacing, u, l);
    }

    void oneLine(GraphicsContext g, double w, double cy, double u, int index) {
        Color c = color();
        double x0 = w * 0.1, x1 = w * 0.9;
        if (skinned) {
            // Les rails : deux fers sur des traverses de bois.
            g.setGlobalAlpha(1);
            g.setFill(SLEEPER);
            for (double x = x0; x <= x1; x += u * 0.035) g.fillRect(x - 2, cy - 7, 4, 14);
            g.setStroke(RAIL);
            g.setLineWidth(2.5);
            g.strokeLine(x0, cy - 4, x1, cy - 4);
            g.strokeLine(x0, cy + 4, x1, cy + 4);
        } else {
            g.setStroke(c);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.35);
            g.strokeLine(x0, cy, x1, cy);
        }
        int gaps = game.lineTicks() - 1;
        for (int k = 0; k <= gaps; k++) {
            double at = (double) k / gaps, x = x0 + (x1 - x0) * at;
            double lit = 0;
            for (Touch touch : touches) if (touch.line == index && Math.abs(touch.at - at) < 1e-9) lit = Math.max(lit, 1 - touch.age / 0.6);
            boolean end = k == 0 || k == gaps;
            double half = (end ? 16 : 10) + 6 * Math.max(0, lit);
            if (skinned) {
                // Un panneau de gare : un poteau, et sa pancarte qui s'éclaire au passage du wagon.
                g.setGlobalAlpha(0.9);
                g.setStroke(RAIL);
                g.setLineWidth(2);
                g.strokeLine(x, cy - half - 8, x, cy - 8);
                g.setFill(lit > 0 ? Color.WHITE : c);
                g.fillRect(x - (end ? 9 : 7), cy - half - 16, end ? 18 : 14, 8);
                g.setStroke(SLEEPER);
                g.setLineWidth(1);
                g.strokeRect(x - (end ? 9 : 7), cy - half - 16, end ? 18 : 14, 8);
            } else {
                g.setLineWidth(end ? 3 : 2);
                g.setStroke(lit > 0 ? Color.WHITE : c);
                g.setGlobalAlpha(0.55 + 0.45 * Math.max(0, lit));
                g.strokeLine(x, cy - half, x, cy + half);
            }
            if (lit > 0) UniverseView.glow(g, x, cy, u * 0.035 * lit + 4, c, 0.8 * lit);
        }
        // Les éclats de chaque trait touché montent et s'effacent.
        g.setFont(Font.font(13));
        g.setTextAlign(TextAlignment.CENTER);
        String gain = "+" + Format.darkMatter(BigNum.of(game.lineShardsPerTouch()));
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
            if (skinned) wagon(g, x, cy, c);
            else {
                g.setFill(Color.WHITE);
                g.fillOval(x - 5, cy - 5, 10, 10);
            }
        }
    }

    /** Un petit wagon posé sur les rails : une caisse, deux roues, une cheminée et sa fumée. */
    void wagon(GraphicsContext g, double x, double cy, Color c) {
        g.setFill(c);
        g.fillRoundRect(x - 10, cy - 12, 20, 12, 3, 3);
        g.setFill(IRON);
        g.fillOval(x - 8, cy - 3, 7, 7);
        g.fillOval(x + 1, cy - 3, 7, 7);
        g.fillRect(x + 4, cy - 18, 3, 6);
        g.setFill(Color.WHITE);
        g.fillRect(x - 7, cy - 9, 5, 4);
        g.fillRect(x, cy - 9, 5, 4);
        for (int k = 0; k < 3; k++) {
            double age = (time * 1.5 + k / 3.0) % 1, size = 4 + 6 * age;
            g.setGlobalAlpha(0.5 * (1 - age));
            g.fillOval(x + 5.5 - size / 2 - 3 * age, cy - 20 - 14 * age - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
    }

    // 2. Le plan : la grille vue en perspective, où le joueur mène le point (Z Q S D,
    // version 0.104) ; sa traînée dessine la courbe de son passage, et les éclats attendent sur la grille.
    void plane(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        // Sur la feuille de cahier, la perspective est celle de la feuille, les éclats des taches d'encre, le point un stylo.
        Color c = skinned ? INK : color(), gem = skinned ? INK : SHARD;
        double top = h * 0.25, bottom = h * 0.9;
        // Sur le cahier, ces traits sont les grands carreaux de la feuille couchée (voir backdrop).
        g.setStroke(skinned ? PAPER_SQUARE.darker() : c);
        g.setLineWidth(1);
        for (int k = 0; k <= 12; k++) {
            double t = k / 12.0;
            double y = top + (bottom - top) * t * t;
            double half = w * (0.18 + 0.32 * t * t);
            g.setGlobalAlpha((skinned ? 0.5 : 0.12) + 0.25 * t);
            g.strokeLine(cx - half, y, cx + half, y);
        }
        for (int k = -8; k <= 8; k++) {
            g.setGlobalAlpha(skinned ? 0.65 : 0.2);
            g.strokeLine(cx + k * w * 0.0225, top, cx + k * w * 0.0625, bottom);
        }
        // Les éclats : des losanges posés sur la grille, plus petits au loin ; des taches d'encre sur le cahier.
        int k = 0;
        for (Game.PlaneShard shard : game.planeShardsOnBoard()) {
            double[] at = onPlane(w, h, cx, shard.x(), shard.y());
            double r = Math.max(3, Game.PLANE_SHARD_RADIUS * w * 0.4 * at[2]) * (1 + 0.15 * Math.sin(time * 4 + k++));
            UniverseView.glow(g, at[0], at[1], r * 2.6, gem, 0.6);
            g.setGlobalAlpha(1);
            g.setFill(gem);
            if (skinned) inkBlot(g, at[0], at[1], r, k);
            else g.fillPolygon(new double[] {at[0], at[0] + r, at[0], at[0] - r}, new double[] {at[1] - r * 1.3, at[1], at[1] + r * 1.3, at[1]}, 4);
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
            g.setStroke(skinned ? INK : Color.WHITE);
            g.setLineWidth(skinned ? 2.2 : 2);
            g.setGlobalAlpha(skinned ? 0.9 : 0.7);
            g.strokePolyline(xs, ys, trailCount);
        }
        // Ce qui vient d'être ramassé : un éclat et le gain qui monte.
        g.setFont(Font.font(14));
        g.setTextAlign(TextAlignment.CENTER);
        for (Pickup pickup : pickups) {
            double t = pickup.age / 0.9;
            UniverseView.glow(g, pickup.x, pickup.y, 10 + 30 * t, gem, 0.7 * (1 - t));
            g.setFill(skinned ? INK : Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.fillText(pickup.text, pickup.x, pickup.y - 18 - 30 * t);
        }
        // Le point du joueur, à sa taille, plus petit au loin.
        double[] p = onPlane(w, h, cx, game.planeX(), game.planeY());
        // Un plancher bas : au départ, le point est petit, et la taille se voit grandir à chaque niveau.
        double pr = Math.max(2.5, game.planePlayerRadius() * w * 0.4 * p[2]);
        UniverseView.glow(g, p[0], p[1], pr * 2.6 + 4, c, skinned ? 0.4 : 0.9);
        g.setGlobalAlpha(1);
        if (skinned) pen(g, p[0], p[1]);
        else {
            g.setFill(Color.WHITE);
            g.fillOval(p[0] - pr * 0.6, p[1] - pr * 0.6, pr * 1.2, pr * 1.2);
        }
        g.setStroke(c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(skinned ? 0.5 : 0.8);
        g.strokeOval(p[0] - pr, p[1] - pr, pr * 2, pr * 2);
    }

    // 3. Un cube qui tourne, et un plus petit dedans, dans l'autre sens.
    void volume(GraphicsContext g, double cx, double cy, double size, double angle, double alpha) {
        cube(g, cx, cy, size, angle, angle * 0.7, color(), alpha);
        cube(g, cx, cy, size * 0.45, -angle * 1.3, angle * 0.5, Color.WHITE, alpha * 0.8);
    }

    // 4. Le cube à chaque instant, mis bout à bout le long du temps ; « maintenant » avance.
    void duration(GraphicsContext g, double w, double cx, double cy, double u) {
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
}
