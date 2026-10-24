package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Version 0.128 : la caméra du volume, sans affichage. Elle dessine le cube, et retrouve la direction d'un clic. */
class VolumeCameraTest {

    private static VolumeCamera inside(double yaw, double pitch) {
        VolumeCamera camera = new VolumeCamera();
        camera.yaw = yaw;
        camera.pitch = pitch;
        camera.distance = 0;
        camera.focal = 300;
        camera.centerX = 400;
        camera.centerY = 240;
        return camera;
    }

    @Test
    void sansRotationLeCentreDeLEcranRegardeDroitDevant() {
        VolumeCamera camera = inside(0, 0);
        assertArrayEquals(new double[] {0, 0, 1}, camera.ray(400, 240), 1e-12);
        double[] at = camera.project(0, 0, 0.5);
        assertEquals(400, at[0], 1e-9);
        assertEquals(240, at[1], 1e-9);
        assertNull(camera.project(0, 0, -0.5), "derrière l'œil : pas dessiné");
    }

    @Test
    void leRegardDUnClicPasseParLePointDessineLa() {
        for (double[] look : new double[][] {{0, 0}, {0.7, -0.3}, {-2.4, 1.1}, {3.9, -1.3}}) {
            VolumeCamera camera = inside(look[0], look[1]);
            double[][] points = {{0.3, -0.2, 0.6}, {-0.8, 0.5, 0.1}, {0.2, 0.9, -0.4}, {-0.5, -0.5, -0.7}};
            for (double[] p : points) {
                double[] at = camera.project(p[0], p[1], p[2]);
                if (at == null) continue;
                double[] ray = camera.ray(at[0], at[1]);
                double length = Math.sqrt(ray[0] * ray[0] + ray[1] * ray[1] + ray[2] * ray[2]);
                double distance = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                for (int k = 0; k < 3; k++) assertEquals(p[k] / distance, ray[k] / length, 1e-9, "le rayon vise le point");
            }
        }
    }

    @Test
    void duDehorsLaCameraDessineLeCubeCommeAvant() {
        // Le cube qui tourne de la version 0.081 : profondeur 5 / (5 + z), taille {@code size}.
        double size = 100, a = 0.8, b = 0.56;
        VolumeCamera camera = new VolumeCamera();
        camera.yaw = a;
        camera.pitch = b;
        camera.distance = 5;
        camera.focal = 5 * size;
        camera.centerX = 400;
        camera.centerY = 240;
        double x = 1, y = -1, z = 1;
        double x1 = x * Math.cos(a) - z * Math.sin(a), z1 = x * Math.sin(a) + z * Math.cos(a);
        double y1 = y * Math.cos(b) - z1 * Math.sin(b), z2 = y * Math.sin(b) + z1 * Math.cos(b);
        double depth = 5 / (5 + z2);
        double[] at = camera.project(x, y, z);
        assertEquals(400 + x1 * size * depth, at[0], 1e-9);
        assertEquals(240 + y1 * size * depth, at[1], 1e-9);
    }

    @Test
    void glisserFaitSuivreLeCubeEtLeRegardNePassePasLaTete() {
        VolumeCamera camera = inside(0, 0);
        double[] before = camera.project(0, 0, 1);
        camera.drag(30, 0);
        double[] after = camera.project(0, 0, 1);
        assertTrue(after[0] > before[0], "le cube glisse avec la souris, vers la droite");
        assertEquals(before[1], after[1], 1e-9);
        camera.drag(0, -100_000);
        assertEquals(VolumeCamera.MAX_PITCH, camera.pitch, 1e-12, "on ne regarde pas plus haut que l'angle permis");
        camera.drag(0, 100_000);
        assertEquals(-VolumeCamera.MAX_PITCH, camera.pitch, 1e-12);
    }

    @Test
    void unTraitQuiPasseDerriereLOeilEstCoupe() {
        VolumeCamera camera = inside(0, 0);
        double[][] cut = camera.clip(new double[] {0, 0, -1}, new double[] {1, 0, 1});
        assertNotNull(cut);
        assertEquals(VolumeCamera.NEAR, cut[0][2], 1e-12);
        assertArrayEquals(new double[] {1, 0, 1}, cut[1], 1e-12);
        assertNull(camera.clip(new double[] {0, 0, -1}, new double[] {1, 0, -0.5}), "entièrement derrière");
    }

    @Test
    void lAngleLePlusProcheATourPres() {
        assertEquals(0.5, VolumeCamera.closest(0.5 + 4 * Math.PI, 0), 1e-12);
        assertEquals(0.5 + 2 * Math.PI, VolumeCamera.closest(0.5, 7), 1e-12);
    }
}
