package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.ui.Sound.Cue;
import idle.ui.particles.SpeedStages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Les paliers de vitesse des particules et leur son : quatorze paliers du clair au noir, le premier de la vitesse de
 * base à 10 par seconde, puis 100, 10⁴, 10⁸… jusqu'à 10⁴⁰⁹⁶ (la fin du premier univers) ; à chaque palier, l'enregistrement
 * d'une particule récupérée descend d'une note. Sans affichage ni carte son.
 */
class PaliersParticulesTest {

    @Test
    void lesPaliersCommencentALaVitesseDeBaseEtVontJusquALaFinDuPremierUnivers() {
        assertEquals(14, SpeedStages.COUNT);
        assertEquals(0, SpeedStages.stage(Math.log10(3)), "la vitesse de base : le premier palier");
        assertEquals(0, SpeedStages.within(Math.log10(3)), 1e-9, "à son tout début");
        assertEquals(0, SpeedStages.stage(Math.log10(0.2)), "plus lent (le premier élan) : le premier palier aussi");
        assertEquals(0, SpeedStages.stage(Double.NEGATIVE_INFINITY));
        assertEquals(0, SpeedStages.stage(Double.NaN));
        assertEquals(0, SpeedStages.stage(Math.log10(9.99)));
        assertEquals(1, SpeedStages.stage(1), "10 par seconde");
        assertEquals(2, SpeedStages.stage(2), "100 par seconde");
        assertEquals(3, SpeedStages.stage(4), "10⁴");
        assertEquals(4, SpeedStages.stage(8), "10⁸");
        assertEquals(12, SpeedStages.stage(2048));
        assertEquals(13, SpeedStages.stage(4096), "10⁴⁰⁹⁶ : la fin du premier univers, le noir");
        assertEquals(13, SpeedStages.stage(1e9), "et pas au-delà");
        assertEquals(0.5, SpeedStages.within(1.5), 1e-9, "à mi-chemin de 10 à 100");
        assertEquals(0.5, SpeedStages.within(3), 1e-9, "à mi-chemin de 10² à 10⁴, en ordre de grandeur");
        int before = -1;
        for (double log = -2; log < 5000; log += 0.37) {
            int stage = SpeedStages.stage(log);
            assertTrue(stage >= before, "jamais un palier plus bas quand la vitesse monte : " + log);
            double within = SpeedStages.within(log);
            assertTrue(within >= 0 && within <= 1, "l'avancée reste dans le palier : " + log);
            before = stage;
        }
    }

    @Test
    void lesCouleursVontDuClairAuNoirEtLeSonDescend() {
        double light = 2;
        for (int stage = 0; stage < SpeedStages.COUNT; stage++) {
            double now = brightness(SpeedStages.color(stage));
            assertTrue(now <= light, "de plus en plus sombre : palier " + (stage + 1));
            light = now;
            if (stage > 0) assertTrue(SpeedStages.pitch(stage) < SpeedStages.pitch(stage - 1), "de plus en plus grave : palier " + (stage + 1));
            assertTrue(brightness(SpeedStages.rim(stage)) > 0.55, "un liseré visible, même autour du noir");
        }
        assertEquals("#000000", SpeedStages.color(SpeedStages.COUNT - 1), "le dernier est noir");
        assertEquals(2, SpeedStages.pitch(0), 1e-9, "le premier, une octave au-dessus de l'enregistrement");
    }

    @Test
    void lEnregistrementSeChargeAChaqueHauteurSansSaturer() {
        short[][] stages = Sound.PARTICLE_STAGES;
        assertEquals(SpeedStages.COUNT, stages.length, "un son par palier");
        for (int stage = 0; stage < stages.length; stage++) {
            double seconds = stages[stage].length / 44_100.0;
            assertEquals(0.12 / SpeedStages.pitch(stage), seconds, 0.005, "le bong (0.274), plus grave, plus long : palier " + (stage + 1));
            // Le son tombe avec l'onde qui atteint le noyau : sa partie forte commence tout de suite, même au plus grave.
            int strong = 0;
            while (strong < stages[stage].length && Math.abs(stages[stage][strong]) < Short.MAX_VALUE / 20) strong++;
            assertTrue(strong / 44_100.0 < 0.02, "sans montée, palier " + (stage + 1) + " : " + strong / 44_100.0 + " s");
            NiveauxTest.auNiveau(stages[stage], 1, Sound.REPEATED_DB, "le palier " + stage);
        }
        // Les passages par zéro par échantillon suivent la hauteur : 2^(32/12), plus de six fois plus au premier palier.
        double first = crossings(stages[0]) / (double) stages[0].length, last = crossings(stages[13]) / (double) stages[13].length;
        assertTrue(first > 3 * last, "le premier palier est bien plus aigu que le dernier : " + first / last);
    }

    /**
     * Version 0.275 : les skins suivent le système des paliers. L'onde part du bord et se resserre sur le noyau, nette en
     * le touchant ; les traits s'épaississent à chaque palier ; un rond par palier passé, chacun plus loin.
     */
    @Test
    void lesSkinsSuiventLesPaliers() {
        assertEquals(95, SpeedStages.waveRadius(0, 10, 95), 1e-9, "créée : au bord");
        assertEquals(10, SpeedStages.waveRadius(1, 10, 95), 1e-9, "récupérée : sur le noyau");
        double previous = 96;
        for (int i = 0; i <= 10; i++) {
            double r = SpeedStages.waveRadius(i / 10.0, 10, 95);
            assertTrue(r < previous, "elle ne fait que se resserrer");
            previous = r;
        }
        assertTrue(SpeedStages.waveAlpha(1) > SpeedStages.waveAlpha(0), "plus nette en arrivant");
        assertEquals(1, SpeedStages.skinWidth(0), 1e-9);
        assertEquals(1 + 0.15 * 13, SpeedStages.skinWidth(99), 1e-9, "au plus près de trois fois au dernier palier");
        assertTrue(SpeedStages.skinWidth(5) > SpeedStages.skinWidth(4));
        for (int k = 2; k < SpeedStages.COUNT; k++) {
            assertTrue(SpeedStages.stageRingRadius(k, 8, 1) > SpeedStages.stageRingRadius(k - 1, 8, 1), "chaque rond plus loin : " + k);
        }
        assertTrue(SpeedStages.stageRingRadius(1, 8, 1) > 8 * 1.2, "le premier rond hors du noyau");
        assertTrue(SpeedStages.stageRingRadius(13, 16, 1) < 100 * 0.8, "le dernier tient dans la vue");
    }

    @Test
    void uneParticuleRecupereeSuitLesReglages() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.particle(13, 100), "le dernier palier");
        assertTrue(sound.particle(99, 100), "un palier hors gamme prend le plus proche, sans attendre la première (0.247)");
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.PARTICLES);
        assertFalse(muted.particle(3, 100), "la catégorie des particules coupée");
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(2, sent.size());
        assertEquals(Sound.PARTICLE_STAGES[13].length * 2, sent.get(0).bytes().length, "l'enregistrement du dernier palier");
        assertEquals(Sound.PARTICLE_STAGES[13].length * 2, sent.get(1).bytes().length);
    }

    /** La clarté d'une couleur {@code #rrggbb}, de 0 à 1 : sa composante la plus forte. */
    private static double brightness(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return Math.max(rgb >> 16, Math.max((rgb >> 8) & 255, rgb & 255)) / 255.0;
    }

    private static int crossings(short[] samples) {
        int count = 0;
        for (int i = 1; i < samples.length; i++) if ((samples[i - 1] < 0) != (samples[i] < 0)) count++;
        return count;
    }

    /** Des particules se superposent, même au même instant (0.247) ; au plus quelques-unes en même temps. */
    @Test
    void lesParticulesSeSuperposentSansNoyerLaSortie() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        for (int voice = 0; voice < Sound.MAX_PARTICLE_VOICES; voice++) {
            assertTrue(sound.particle(SpeedStages.COUNT - 1, 100), "la particule " + (voice + 1) + " se superpose aux autres");
        }
        assertFalse(sound.particle(SpeedStages.COUNT - 1, 100), "au-delà, la sortie n'est pas noyée");
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.MAX_PARTICLE_VOICES, sent.size());
    }
}
