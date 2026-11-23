package idle.ui.universe;

import javafx.scene.Cursor;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.canvas.*;

/** Une partie de {@link SeedView} (découpée en 0.293 pour rester sous 1 000 lignes) : l'arrosoir : le saisir, le déplacer, verser. */
public abstract class SeedViewCan extends SeedViewBase {
    public SeedViewCan() {
        super();
    }

    public boolean isCanUnlocked() { return canUnlocked; }

    /** La taille de l'arrosoir, et celle de l'étagère : à la mesure de la vue. */
    double canSize() { return Math.max(26, Math.min(getWidth(), getHeight()) * 0.11); }

    /** Le milieu de l'étagère, sur sa planche : là où l'arrosoir se pose, et où se tient le bouton qui le débloque. */
    public double shelfX() {
        // En bas à droite de la vue.
        return getWidth() - Math.max(canSize() * 1.6, getWidth() * 0.12);
    }

    /** Le dessus de la planche de l'étagère. */
    public double shelfTop() { return getHeight() - Math.max(canSize() * 0.55, getHeight() * 0.07); }

    /** Le milieu de l'étagère en hauteur, entre ses deux planches : là où se tient le bouton qui débloque l'arrosoir. */
    public double shelfMiddleY() { return shelfTop() - canSize() * 0.68; }

    /** Où l'arrosoir se pose sur l'étagère. */
    double homeX() { return shelfX(); }

    double homeY() { return shelfTop() - canSize() * 0.42; }

    public double canX() { return Double.isNaN(canX) ? homeX() : canX; }

    public double canY() { return Double.isNaN(canY) ? homeY() : canY; }

    /** De 0 à 1 : penché ou non. */
    public double tilt() { return tilt; }

    boolean overCan(double x, double y) {
        double s = canSize();
        return Math.abs(x - canX()) <= s * 0.75 && Math.abs(y - canY()) <= s * 0.6;
    }

    /** Prend l'arrosoir si le point est dessus. Rend vrai s'il est pris. */
    public boolean grab(double x, double y) {
        if (!canUnlocked || !overCan(x, y)) return false;
        dragging = true;
        grabX = x - canX();
        grabY = y - canY();
        setCursor(Cursor.CLOSED_HAND);
        return true;
    }

    /** L'arrosoir suit la souris, sans sortir de la vue. */
    public void drag(double x, double y) {
        if (!dragging) return;
        double s = canSize();
        canX = Math.max(s * 0.6, Math.min(getWidth() - s, x - grabX));
        canY = Math.max(s * 0.6, Math.min(getHeight() - s * 0.5, y - grabY));
        overPlant = pourPointOverPlant();
    }

    /** Relâché : il se redresse et revient sur l'étagère. */
    public void release() {
        if (!dragging) return;
        dragging = false;
        overPlant = false;
        setCursor(Cursor.DEFAULT);
    }

    /** Là où tombe l'eau de l'arrosoir penché : un peu à droite de son centre, sous le bec. */
    double pourX() { return canX() + canSize() * 0.95; }

    boolean pourPointOverPlant() {
        if (plantHalf <= 0) return false;
        double x = pourX(), y = canY();
        return Math.abs(x - plantX) <= plantHalf && y <= plantBottom && y >= plantTop - getHeight() * 0.4;
    }

    /** Vrai tant que l'arrosoir, tenu au-dessus de la graine, est assez penché pour verser : le jeu compte alors ses arrosages. */
    public boolean isPouring() { return canUnlocked && dragging && overPlant && tilt > 0.6 && seeds >= 1; }

    /** Les gouttes qui coulent du bec : pour les vérifications. */
    public int streamDrops() { return stream.size(); }

    /** Le bout du bec, en points de la vue, l'arrosoir penché comme il l'est. */
    double[] spout() {
        double s = canSize(), a = Math.toRadians(CAN_TILT_DEGREES * tilt);
        double lx = s * 0.95, ly = -s * 0.42;
        return new double[] {canX() + lx * Math.cos(a) - ly * Math.sin(a), canY() + lx * Math.sin(a) + ly * Math.cos(a)};
    }

    /** L'arrosoir avance : il suit la main ou revient sur l'étagère, se penche ou se redresse, et l'eau coule. */
    void moveCan(double dt) {
        if (!dragging && !Double.isNaN(canX)) {
            double k = Math.min(1, dt * 7);
            canX += (homeX() - canX) * k;
            canY += (homeY() - canY) * k;
            if (Math.hypot(homeX() - canX, homeY() - canY) < 0.5) canX = canY = Double.NaN;
        }
        if (dragging) overPlant = pourPointOverPlant();
        double target = dragging && overPlant ? 1 : 0;
        tilt += (target - tilt) * Math.min(1, dt * 6);
        if (isPouring()) {
            streamTimer += dt;
            // Un filet de gouttes qui sort du bec dans son axe, en éventail léger, puis tombe.
            double a = Math.toRadians(CAN_TILT_DEGREES * tilt - 35);
            while (streamTimer >= 0.02) {
                streamTimer -= 0.02;
                double[] tip = spout();
                Stream drop = new Stream();
                double spread = (UniverseView.noise(++dropCount * 5 + 1) - 0.5) * 0.5;
                double speed = canSize() * (1.4 + 0.6 * UniverseView.noise(dropCount * 5 + 2));
                drop.x = tip[0];
                drop.y = tip[1];
                drop.vx = Math.cos(a + spread) * speed;
                drop.vy = Math.sin(a + spread) * speed;
                drop.land = Math.max(plantTop + (plantBottom - plantTop) * 0.1, tip[1] + canSize() * (0.9 + 0.4 * UniverseView.noise(dropCount * 5 + 3)));
                stream.add(drop);
            }
        } else streamTimer = 0;
        // Le tuyau arrose seul : un jet en cloche de sa lance vers le pied de la plante.
        if (hoseFlowing && plantHalf > 0) {
            hoseTimer += dt;
            while (hoseTimer >= 0.05) {
                hoseTimer -= 0.05;
                double[] tip = hoseNozzle();
                Stream drop = new Stream();
                // Une cloche qui retombe sur la plante en un peu plus d'une demi-seconde.
                double spread = (UniverseView.noise(++dropCount * 5 + 4) - 0.5) * 0.3;
                double flight = 0.6 * (1 + spread), fall = Math.max(300, getHeight() * 1.6);
                double reach = Math.max(1, plantX - tip[0]) * (0.75 + 0.2 * UniverseView.noise(dropCount * 5 + 5));
                drop.x = tip[0];
                drop.y = tip[1];
                drop.land = plantBottom - canSize() * 0.1;
                drop.vx = reach / flight;
                drop.vy = (drop.land - tip[1] - 0.5 * fall * flight * flight) / flight;
                hoseSpray.add(drop);
            }
        } else hoseTimer = 0;
        if (chronoShown) chronoHand += dt * (chronoRunning ? 5 : 0.4);
        double gravity = Math.max(300, getHeight() * 1.6);
        for (Stream drop : hoseSpray) {
            drop.age += dt;
            if (drop.splashed) continue;
            drop.vy += gravity * dt;
            drop.x += drop.vx * dt;
            drop.y += drop.vy * dt;
            if (drop.vy > 0 && drop.y >= drop.land || drop.y >= getHeight()) {
                drop.splashed = true;
                drop.age = 0;
            }
        }
        hoseSpray.removeIf(drop -> drop.splashed ? drop.age > 0.35 : drop.age > 3);
        for (Stream drop : stream) {
            drop.age += dt;
            if (drop.splashed) continue;
            drop.vy += gravity * dt;
            drop.x += drop.vx * dt;
            drop.y += drop.vy * dt;
            if (drop.y >= drop.land || drop.y >= getHeight()) {
                drop.splashed = true;
                drop.age = 0;
            }
        }
        stream.removeIf(drop -> drop.splashed ? drop.age > 0.35 : drop.age > 3);
    }

    /** Le nombre de graines à montrer (un réel : il peut être très grand). */
    public void setSeeds(double count) { seeds = Math.max(0, count); }

    /** Graines dessinées autour de la première : pour les vérifications. */
    public int orbiting() { return (int) Math.min(MAX_ORBITING, Math.max(0, Math.floor(seeds) - 1)); }

    /** La graine a-t-elle germé, et combien de fois son germe a-t-il été arrosé. */
    public void setSprout(boolean sprouted, long waterings) {
        if (sprouted && !this.sprouted && shownSprout <= 0) shownSprout = 0.02;
        this.sprouted = sprouted;
        this.waterings = Math.max(0, waterings);
        if (!sprouted) shownSprout = 0;
    }

    /**
     * Le palier de la graine, de 1 à 11. Un palier de plus qu'avant joue le passage ({@link #evolve()}) ; un palier de
     * moins (une nouvelle partie) se pose sans animation.
     */
    public void setStage(int value) {
        int next = Math.max(1, Math.min(11, value));
        if (next > stage && stage >= 2) evolve(stage);
        else if (next < stage) evolveAge = -1;
        stage = next;
        twig = stage >= 2;
    }

    /** Le palier dessiné : pour les vérifications. */
    public int stage() { return stage; }

    /** Joue le passage depuis le palier {@code from} : la vue s'éloigne, une fissure craque dans la graine, la tige grossit. */
    void evolve(int from) {
        evolveFrom = from;
        evolveAge = 0;
        flash = 1;
    }

    /** Vrai pendant le passage d'un palier : pour les vérifications. */
    public boolean isEvolving() { return evolveAge >= 0; }

    /** La part de la vue gardée en ce moment : celle du palier, ou en route d'un palier à l'autre. */
    double zoom() {
        double target = ZOOM[stage - 1];
        if (evolveAge < 0) return target;
        return ZOOM[evolveFrom - 1] + (target - ZOOM[evolveFrom - 1]) * ease((evolveAge - 0.3) / 1.9);
    }

    /** La croissance de ce que le palier ajoute : de 0,55 à 1 pendant le passage, 1 ensuite. */
    double growth() { return evolveAge < 0 ? 1 : 0.55 + 0.45 * ease((evolveAge - 0.6) / 2.0); }

    /** Salue la brindille d'un éclat de lumière. */
    public void celebrate() { flash = 1; }

    /** Une goutte d'eau tombe sur le germe. */
    public void water() { if (drops.size() < 12) drops.add(new Drop(UniverseView.noise(++dropCount * 3 + 7) - 0.5)); }

    /** Les gouttes en train de tomber : pour les vérifications. */
    public int drops() { return drops.size(); }

    /**
     * La longueur que visent les arrosages, de 0 à 1 : 0,15 à la germination, puis de plus en plus lentement vers 1.
     */
    public double sprout() {
        if (!sprouted || seeds < 1) return 0;
        return Math.min(1, 0.15 + 0.85 * (1 - Math.exp(-waterings / 30.0)));
    }

    /** Les feuilles du germe : une à la germination, une de plus tous les quinze arrosages, cinq au plus. */
    public int leaves() { return sprouted ? (int) Math.min(5, 1 + waterings / 15) : 0; }

    public void frame(double elapsed, boolean moving) {
        double dt = Math.min(elapsed, 0.1);
        if (moving) time += dt;
        // Le germe rejoint sa longueur en une seconde ou deux, même animations coupées.
        shownSprout += (sprout() - shownSprout) * Math.min(1, dt * 2.5);
        flash = Math.max(0, flash - dt * 0.6);
        if (evolveAge >= 0) {
            evolveAge += dt;
            if (evolveAge >= EVOLVE_SECONDS) evolveAge = -1;
        }
        for (Drop drop : drops) drop.age += dt;
        drops.removeIf(drop -> drop.age > DROP_SECONDS + 0.6);
        moveCan(dt);
        draw();
        drawBubbles();
        drawShelfAndCan();
        drawBloom();
        drawWeather();
        drawBubbleLabel();
    }
}
