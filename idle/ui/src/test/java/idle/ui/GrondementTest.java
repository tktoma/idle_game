package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Le grondement de la matière noire qui grossit ({@link DarkMatterHum}) : sa boucle se lit sans couture, il accélère avec
 * l'élan jusqu'au seuil de la fin du premier univers, glisse d'une vitesse à l'autre, et ne joue que lorsqu'il le doit.
 * Sans carte son : les blocs sont relevés.
 */
class GrondementTest {

    @Test
    void laBoucleSeChargeEtSeRejointSansClaquer() {
        float[] loop = DarkMatterHum.LOOP;
        assertEquals(3.66, loop.length / DarkMatterHum.RATE, 0.01, "3,66 s de boucle");
        float peak = 0, jump = 0;
        for (int i = 0; i < loop.length; i++) {
            peak = Math.max(peak, Math.abs(loop[i]));
            if (i > 0) jump = Math.max(jump, Math.abs(loop[i] - loop[i - 1]));
        }
        assertTrue(peak <= Sound.MAX_PEAK + 1e-3, "sans saturer : " + peak);
        float seam = Math.abs(loop[0] - loop[loop.length - 1]);
        assertTrue(seam <= jump, "la jointure ne saute pas plus qu'un pas ordinaire du son : " + seam + " / " + jump);
    }

    @Test
    void laVitesseSuitLOrdreDeGrandeurDeLElanJusquAuSeuil() {
        assertEquals(1, DarkMatterHum.rate(0), 1e-9, "sans élan, vitesse normale");
        assertEquals(1, DarkMatterHum.rate(-5), 1e-9);
        assertEquals(1, DarkMatterHum.rate(Double.NaN), 1e-9, "un élan illisible ne casse rien");
        assertEquals(2, DarkMatterHum.rate(1e12), 0.01, "à mi-chemin des ordres de grandeur, mi-course");
        assertEquals(3, DarkMatterHum.rate(1e24), 0.01, "au seuil de la fin du premier univers, ×3");
        assertEquals(3, DarkMatterHum.rate(1e40), 1e-9, "et pas plus vite ensuite");
        assertEquals(3, DarkMatterHum.rate(Double.POSITIVE_INFINITY), 1e-9);
        double before = 0;
        for (int power = 0; power <= 30; power++) {
            double now = DarkMatterHum.rate(Math.pow(10, power));
            assertTrue(now >= before, "jamais plus lent quand l'élan monte : 10^" + power);
            before = now;
        }
    }

    @Test
    void laVitesseGlisseSansAcoupEtLaLectureResteContinue() {
        DarkMatterHum.Reader reader = new DarkMatterHum.Reader();
        int quarter = (int) (DarkMatterHum.RATE * DarkMatterHum.GLIDE);
        reader.read(64, 3);
        assertTrue(reader.speed() < 1.1, "la vitesse ne saute pas d'un coup");
        reader.read(quarter, 3);
        assertTrue(reader.speed() > 2 && reader.speed() < 3, "en un quart de seconde, elle a fait l'essentiel : " + reader.speed());
        reader.read(quarter * 8, 3);
        assertEquals(3, reader.speed(), 1e-3);
        // Plusieurs tours de boucle, à pleine vitesse : aucun saut plus grand qu'au repos.
        float[] fast = reader.read(DarkMatterHum.LOOP.length * 2, 3);
        float jump = 0;
        for (int i = 1; i < fast.length; i++) jump = Math.max(jump, Math.abs(fast[i] - fast[i - 1]));
        float still = 0;
        for (int i = 1; i < DarkMatterHum.LOOP.length; i++) still = Math.max(still, Math.abs(DarkMatterHum.LOOP[i] - DarkMatterHum.LOOP[i - 1]));
        assertTrue(jump <= 3 * still, "lue trois fois plus vite, les pas grandissent au plus de trois fois : " + jump + " / " + still);
    }

    @Test
    void ilNeJoueQueLorsqueLaMatiereNoireGrossitEtQuIlEstPermis() {
        DarkMatterHum hum = new DarkMatterHum();
        List<byte[]> sent = Collections.synchronizedList(new ArrayList<>());
        hum.sink = sent::add;
        hum.update(false, 1e10, true, 80);
        assertNull(hum.player, "elle ne grossit pas : rien ne démarre");
        hum.update(true, 1e10, false, 80);
        assertNull(hum.player, "catégorie coupée ou sons coupés : rien");
        hum.update(true, 1e10, true, 0);
        assertNull(hum.player, "volume à zéro : rien");
        hum.update(true, 1e10, true, 80);
        assertTrue(hum.player != null && hum.player.isAlive(), "elle grossit : le grondement démarre");
    }

    @Test
    void ilEntreEnFonduEtSeTaitEnFondu() throws InterruptedException {
        DarkMatterHum hum = new DarkMatterHum();
        List<byte[]> sent = Collections.synchronizedList(new ArrayList<>());
        hum.sink = sent::add;
        hum.update(true, 1e20, true, 100);
        long until = System.currentTimeMillis() + 5_000;
        while (sent.size() < 40 && System.currentTimeMillis() < until) Thread.sleep(5);
        hum.update(false, 1e20, true, 100);
        hum.player.join(5_000);
        assertFalse(hum.player.isAlive(), "il se tait de lui-même après son fondu");
        List<byte[]> blocks;
        synchronized (sent) { blocks = new ArrayList<>(sent); }
        assertTrue(Math.abs(sample(blocks.get(0), 0)) < 50, "le premier échantillon part du silence");
        byte[] last = blocks.get(blocks.size() - 1);
        assertTrue(Math.abs(sample(last, last.length / 2 - 1)) < 50, "le dernier y revient");
        int loud = 0;
        for (byte[] block : blocks) for (int i = 0; i < block.length / 2; i++) loud = Math.max(loud, Math.abs(sample(block, i)));
        assertTrue(loud > 3_000, "entre les deux, il s'entend : " + loud);
    }

    private static int sample(byte[] pcm, int index) { return (short) ((pcm[2 * index] & 0xff) | (pcm[2 * index + 1] << 8)); }
}
