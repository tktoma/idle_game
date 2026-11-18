package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.collider.ColliderUpgrade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Les sons de fond en boucle ({@link Hum}) : le grondement de la matière noire qui grossit et le fond du collisionneur.
 * Leurs boucles se lisent sans couture, ils accélèrent avec ce qu'ils accompagnent, glissent d'une vitesse à l'autre, et
 * ne jouent que lorsqu'ils le doivent. Sans carte son : les blocs sont relevés.
 */
class FondsSonoresTest {

    @Test
    void lesBouclesSeChargentEtSeRejoignentSansClaquer() {
        assertEquals(3.66, Hum.DARK_MATTER.frames() / Hum.DARK_MATTER.rate(), 0.01, "3,66 s de grondement");
        assertEquals(1, Hum.DARK_MATTER.channels());
        assertEquals(1.5, Hum.COLLIDER.frames() / Hum.COLLIDER.rate(), 0.01, "1,5 s de fond du collisionneur");
        assertEquals(2, Hum.COLLIDER.channels(), "en stéréo, comme l'enregistrement");
        assertEquals(4.1, Hum.EXPANSION.frames() / Hum.EXPANSION.rate(), 0.01, "4,1 s de fond de l'expansion");
        assertEquals(2, Hum.EXPANSION.channels());
        assertEquals(1.5, Hum.BRANE.frames() / Hum.BRANE.rate(), 0.01, "1,5 s de fibres qui craquent");
        assertEquals(0.81, Hum.VOID_PAIRS.frames() / Hum.VOID_PAIRS.rate(), 0.01, "0,81 s de fond des paires du vide (0.272)");
        assertEquals(1, Hum.VOID_PAIRS.channels());
        for (Hum.Loop each : List.of(Hum.DARK_MATTER, Hum.COLLIDER, Hum.EXPANSION, Hum.BRANE, Hum.DURATION, Hum.CHRONO, Hum.WATERING_CAN, Hum.HOSE,
                Hum.STORM, Hum.SUN, Hum.VOID_PAIRS)) {
            float[] loop = each.samples();
            int channels = each.channels();
            for (int c = 0; c < channels; c++) {
                float peak = 0, jump = 0;
                for (int i = c; i < loop.length; i += channels) {
                    peak = Math.max(peak, Math.abs(loop[i]));
                    if (i >= channels) jump = Math.max(jump, Math.abs(loop[i] - loop[i - channels]));
                }
                assertTrue(peak <= Sound.MAX_PEAK + 1e-3, "sans saturer : " + peak);
                float seam = Math.abs(loop[c] - loop[loop.length - channels + c]);
                assertTrue(seam <= jump, "la jointure ne saute pas plus qu'un pas ordinaire du son : " + seam + " / " + jump);
            }
        }
    }

    @Test
    void leFondDuCollisionneurSuitLaVitesseDesFaisceaux() {
        assertEquals(1, Hum.colliderRate(Game.RESONANCE_PERIOD), 1e-9, "au départ, vitesse normale");
        assertEquals(2, Hum.colliderRate(Game.RESONANCE_PERIOD / 2), 1e-9, "faisceaux deux fois plus rapides, ×2");
        assertEquals(1, Hum.colliderRate(0), 1e-9, "une période illisible ne casse rien");
        assertEquals(1, Hum.colliderRate(Double.NaN), 1e-9);
        Game game = new Game(1L);
        game.start();
        assertEquals(1, Hum.colliderRate(game.colliderPeriod()), 1e-9);
        game.state().setColliderUpgradeLevel(ColliderUpgrade.SPEED, 5);
        assertEquals(2, Hum.colliderRate(game.colliderPeriod()), 1e-9, "cinq niveaux de faisceaux rapides : ×2");
    }

    @Test
    void laVitesseSuitLOrdreDeGrandeurDeLElanJusquAuSeuil() {
        assertEquals(1, Hum.darkMatterRate(0), 1e-9, "sans élan, vitesse normale");
        assertEquals(1, Hum.darkMatterRate(-5), 1e-9);
        assertEquals(1, Hum.darkMatterRate(Double.NaN), 1e-9, "un élan illisible ne casse rien");
        assertEquals(2, Hum.darkMatterRate(1e12), 0.01, "à mi-chemin des ordres de grandeur, mi-course");
        assertEquals(3, Hum.darkMatterRate(1e24), 0.01, "au seuil de la fin du premier univers, ×3");
        assertEquals(3, Hum.darkMatterRate(1e40), 1e-9, "et pas plus vite ensuite");
        assertEquals(3, Hum.darkMatterRate(Double.POSITIVE_INFINITY), 1e-9);
        double before = 0;
        for (int power = 0; power <= 30; power++) {
            double now = Hum.darkMatterRate(Math.pow(10, power));
            assertTrue(now >= before, "jamais plus lent quand l'élan monte : 10^" + power);
            before = now;
        }
    }

    @Test
    void laVitesseGlisseSansAcoupEtLaLectureResteContinue() {
        Hum.Reader reader = new Hum.Reader(Hum.DARK_MATTER);
        int quarter = (int) (Hum.DARK_MATTER.rate() * Hum.GLIDE);
        reader.read(64, 3);
        assertTrue(reader.speed() < 1.1, "la vitesse ne saute pas d'un coup");
        reader.read(quarter, 3);
        assertTrue(reader.speed() > 2 && reader.speed() < 3, "en un quart de seconde, elle a fait l'essentiel : " + reader.speed());
        reader.read(quarter * 8, 3);
        assertEquals(3, reader.speed(), 1e-3);
        // Plusieurs tours de boucle, à pleine vitesse : aucun saut plus grand qu'au repos.
        float[] fast = reader.read(Hum.DARK_MATTER.frames() * 2, 3);
        float jump = 0;
        for (int i = 1; i < fast.length; i++) jump = Math.max(jump, Math.abs(fast[i] - fast[i - 1]));
        float still = 0;
        float[] loop = Hum.DARK_MATTER.samples();
        for (int i = 1; i < loop.length; i++) still = Math.max(still, Math.abs(loop[i] - loop[i - 1]));
        assertTrue(jump <= 3 * still, "lue trois fois plus vite, les pas grandissent au plus de trois fois : " + jump + " / " + still);
    }

    @Test
    void ilNeJoueQueLorsqueLaMatiereNoireGrossitEtQuIlEstPermis() {
        Hum hum = new Hum(Hum.DARK_MATTER, "essai");
        List<byte[]> sent = Collections.synchronizedList(new ArrayList<>());
        hum.sink = sent::add;
        hum.update(false, 2, true, 80);
        assertNull(hum.player, "elle ne grossit pas : rien ne démarre");
        hum.update(true, 2, false, 80);
        assertNull(hum.player, "catégorie coupée ou sons coupés : rien");
        hum.update(true, 2, true, 0);
        assertNull(hum.player, "volume à zéro : rien");
        hum.update(true, 2, true, 80);
        assertTrue(hum.player != null && hum.player.isAlive(), "elle grossit : le grondement démarre");
    }

    @Test
    void ilEntreEnFonduEtSeTaitEnFondu() throws InterruptedException {
        Hum hum = new Hum(Hum.DARK_MATTER, "essai");
        List<byte[]> sent = Collections.synchronizedList(new ArrayList<>());
        hum.sink = sent::add;
        hum.update(true, 3, true, 100);
        long until = System.currentTimeMillis() + 5_000;
        while (sent.size() < 40 && System.currentTimeMillis() < until) Thread.sleep(5);
        hum.update(false, 3, true, 100);
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

    @Test
    void leTicTacDuChronoSuitSonAccelerationJusquATroisFois() {
        assertEquals(1, Hum.chronoRate(1), 1e-9);
        assertEquals(2, Hum.chronoRate(2), 1e-9);
        assertEquals(3, Hum.chronoRate(10), 1e-9, "au-delà de ×3, il bourdonnerait");
        assertEquals(1, Hum.chronoRate(Double.NaN), 1e-9);
        // L'horloge du joueur (0.260) : six tics en six secondes, une boucle qui commence et finit dans le silence.
        float[] loop = Hum.CHRONO.samples();
        assertEquals(6, Hum.CHRONO.frames() / Hum.CHRONO.rate(), 0.05);
        assertEquals(1, Hum.CHRONO.channels());
        int ticks = 0, last = -44_100;
        for (int i = 0; i < loop.length; i++) {
            if (Math.abs(loop[i]) > 0.3f * peak(loop) && i - last > 22_050) {
                ticks++;
                last = i;
            }
        }
        assertEquals(6, ticks, "six tics par tour");
        assertTrue(Math.abs(loop[0]) < 0.01f && Math.abs(loop[loop.length - 1]) < 0.01f, "le raccord tombe dans le silence");
    }

    private static float peak(float[] loop) {
        float peak = 0;
        for (float v : loop) peak = Math.max(peak, Math.abs(v));
        return peak;
    }

    @Test
    void lesSonsDeLaGraineSeChargent() {
        for (Hum.Loop each : List.of(Hum.WATERING_CAN, Hum.HOSE, Hum.STORM)) {
            assertTrue(each.frames() / each.rate() > 2.5, "une boucle de trois secondes environ");
            assertEquals(1, each.channels());
        }
        // Le beau temps : la nuit de jungle du joueur (0.254), en stéréo, une boucle de 20 s.
        assertEquals(2, Hum.SUN.channels());
        assertEquals(20, Hum.SUN.frames() / Hum.SUN.rate(), 0.01);
        assertEquals(4, Sound.SEASONS.length, "un accord par saison");
        for (short[] chord : Sound.SEASONS) NiveauxTest.auNiveau(chord, 1, Sound.REPEATED_DB, "un accord de saison");
        assertEquals(0.9, Sound.BLOOM_SOUND.length / 44_100.0, 0.01, "la grosse bulle du rond de croissance");
        NiveauxTest.auNiveau(Sound.BLOOM_SOUND, 1, Sound.REPEATED_DB, "le rond de croissance");
    }

    /** Version 0.255 : le palier de la graine joue l'enregistrement du joueur, en stéréo, au niveau des sons rares. */
    @Test
    void lePalierDeLaGraineEstEnregistre() throws InterruptedException {
        assertEquals(2.04, Sound.SEED_STEREO.length / 2 / 44_100.0, 0.01, "le son de la 0.257");
        NiveauxTest.auNiveau(Sound.SEED_STEREO, 2, Sound.RARE_DB, "le palier de la graine");
        java.util.List<Sound.Pcm> sent = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.click(Sound.Cue.SEED, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(sent.get(0).stereo(), "en stéréo");
        assertEquals(Sound.SEED_STEREO.length * 2, sent.get(0).bytes().length, "l'enregistrement, pas la synthèse");
    }
}
