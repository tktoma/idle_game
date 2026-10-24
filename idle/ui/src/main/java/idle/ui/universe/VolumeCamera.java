package idle.ui.universe;

/**
 * La caméra du volume (version 0.128) : de quoi dessiner le cube en perspective, du dehors comme de son centre,
 * et retrouver la direction d'un clic. Sans JavaFX : de simples calculs, vérifiables seuls.
 *
 * <p>Le cube va de −1 à 1 sur les trois axes. La caméra le tourne d'abord de {@code yaw} autour de l'axe vertical,
 * puis de {@code pitch} autour de l'axe horizontal, comme le cube qui tourne de la dimension 3 depuis la version
 * 0.081, puis le regarde depuis {@code distance} en arrière de son centre (0 : depuis le centre même), avec une
 * focale de {@code focal} pixels.
 */
public final class VolumeCamera {

    /** Ce qui est plus près que cela de l'œil n'est pas dessiné : un trait qui passe derrière est coupé là. */
    public static final double NEAR = 0.02;
    /** Le regard ne monte ni ne descend au-delà de cet angle (en radians) : on ne passe pas la tête à l'envers. */
    public static final double MAX_PITCH = 1.4;

    public double yaw, pitch, distance, focal = 1, centerX, centerY;

    /** Tourne un point du cube dans le repère de la caméra : x à droite, y en bas, z droit devant. */
    public double[] toView(double x, double y, double z) {
        double ca = Math.cos(yaw), sa = Math.sin(yaw), cb = Math.cos(pitch), sb = Math.sin(pitch);
        double x1 = x * ca - z * sa, z1 = x * sa + z * ca;
        double y1 = y * cb - z1 * sb, z2 = y * sb + z1 * cb;
        return new double[] {x1, y1, z2 + distance};
    }

    /** L'inverse, pour une direction : du repère de la caméra vers celui du cube. */
    public double[] toCube(double vx, double vy, double vz) {
        double ca = Math.cos(yaw), sa = Math.sin(yaw), cb = Math.cos(pitch), sb = Math.sin(pitch);
        double y = vy * cb + vz * sb, z1 = -vy * sb + vz * cb;
        return new double[] {vx * ca + z1 * sa, y, -vx * sa + z1 * ca};
    }

    /** Un point du repère de la caméra à l'écran : x, y, et sa profondeur ; {@code null} s'il est derrière l'œil. */
    public double[] screen(double[] view) {
        if (view[2] < NEAR) return null;
        return new double[] {centerX + focal * view[0] / view[2], centerY + focal * view[1] / view[2], view[2]};
    }

    /** Un point du cube à l'écran, ou {@code null} s'il est derrière l'œil. */
    public double[] project(double x, double y, double z) {
        return screen(toView(x, y, z));
    }

    /** La direction, dans le repère du cube, du regard qui passe par ce point de l'écran. */
    public double[] ray(double sx, double sy) {
        return toCube((sx - centerX) / focal, (sy - centerY) / focal, 1);
    }

    /**
     * Le regard suit la souris tenue : le cube glisse avec elle, comme une photo panoramique qu'on fait tourner.
     * Glisser vers la droite fait donc regarder vers la gauche.
     */
    public void drag(double dx, double dy) {
        yaw -= dx / focal;
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch - dy / focal));
    }

    /**
     * Les deux bouts d'un trait du repère de la caméra, coupé là où il passe derrière l'œil ; {@code null} s'il est
     * entièrement derrière.
     */
    public double[][] clip(double[] a, double[] b) {
        if (a[2] < NEAR && b[2] < NEAR) return null;
        if (a[2] >= NEAR && b[2] >= NEAR) return new double[][] {a, b};
        double t = (NEAR - a[2]) / (b[2] - a[2]);
        double[] cut = {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, NEAR};
        return a[2] < NEAR ? new double[][] {cut, b} : new double[][] {a, cut};
    }

    /** L'angle le plus proche de {@code near} qui vaut {@code angle} à un tour près. */
    public static double closest(double angle, double near) {
        double turn = 2 * Math.PI;
        return angle + turn * Math.round((near - angle) / turn);
    }
}
