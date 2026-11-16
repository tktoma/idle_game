package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.ui.Sound.Cue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Le volume perçu des sons : les sons rares (les grands moments) tous au même niveau, les sons répétitifs (bruitages,
 * interface, gestes, points, grondement) tous au même niveau, plus bas. Dans chaque banque de sons, enregistrements compris.
 */
class NiveauxTest {

    /** Vérifie qu'un son est au niveau visé, ou plus bas seulement s'il touche la crête permise. */
    static void auNiveau(short[] sound, int channels, double targetDb, String what) {
        double db = 20 * Math.log10(Sound.loudness(sound, channels));
        int peak = 0;
        for (short sample : sound) peak = Math.max(peak, Math.abs(sample));
        assertTrue(peak <= Math.round(Sound.MAX_PEAK * Short.MAX_VALUE) + 1, what + " sans saturer : " + peak);
        boolean capped = peak >= Math.round(Sound.MAX_PEAK * Short.MAX_VALUE) - 2;
        assertTrue(Math.abs(db - targetDb) < 0.3 || capped && db < targetDb, what + " à " + db + " dB au lieu de " + targetDb);
    }

    @Test
    void lesRaresSontPlusFortsQueLesRepetitifs() {
        assertTrue(Sound.RARE_DB - Sound.REPEATED_DB >= 6, "un écart net entre les grands moments et ce qui revient sans cesse");
        for (Cue cue : Cue.values()) {
            boolean repeated = cue == Cue.PARTICLE || cue == Cue.ATOM || cue == Cue.NEW_ATOM || cue == Cue.COLLISION || cue == Cue.HADRON || cue == Cue.VOID
                    || cue == Cue.WATER || cue.isInterface() && cue != Cue.BEAT && cue != Cue.SEED && cue != Cue.PROBE;   // un palier de graine est rare
            assertEquals(repeated, Sound.REPEATED.contains(cue), cue.name());
        }
    }

    @Test
    void chaqueSonDeChaqueBanquePartASonNiveau() throws InterruptedException {
        List<Skin> banks = new ArrayList<>(Arrays.asList(Skin.SOUND_BANKS));
        banks.add(null);
        for (Skin bank : banks) {
            for (Cue cue : Cue.values()) {
                if (cue == Cue.BEAT) continue;
                List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
                Sound sound = new Sound();
                sound.sink = sent::add;
                sound.setSkins(bank, null, null);
                boolean played = sound.play(cue, 100);
                if (sound.player != null) {
                    sound.player.shutdown();
                    assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
                }
                if (!played) continue;                                         // un son que cette banque n'a pas
                Sound.Pcm pcm = sent.get(0);
                short[] samples = new short[pcm.bytes().length / 2];
                for (int i = 0; i < samples.length; i++) samples[i] = (short) ((pcm.bytes()[2 * i] & 0xff) | pcm.bytes()[2 * i + 1] << 8);
                auNiveau(samples, pcm.stereo() ? 2 : 1, Sound.targetDb(cue), cue + " (" + (bank == null ? "Classique" : bank.name()) + ")");
            }
        }
    }

    @Test
    void lesGestesLesPointsEtLeGrondementSontAuNiveauDesRepetitifs() {
        for (int dimension = 1; dimension <= 10; dimension++) {
            auNiveau(Sound.leveled(Sound.synthesizeGesture(dimension), 1, Sound.REPEATED_DB), 1, Sound.REPEATED_DB, "le geste " + dimension);
            auNiveau(Sound.leveled(Sound.synthesizeGain(dimension), 1, Sound.REPEATED_DB), 1, Sound.REPEATED_DB, "les points " + dimension);
        }
        for (Hum.Loop hum : List.of(Hum.DARK_MATTER, Hum.COLLIDER, Hum.EXPANSION, Hum.BRANE, Hum.DURATION, Hum.CHRONO)) {
            short[] loop = new short[hum.samples().length];
            for (int i = 0; i < loop.length; i++) loop[i] = (short) Math.round(hum.samples()[i] * 32768);
            assertTrue(loop.length > 0, "la boucle se charge");
            double db = 20 * Math.log10(Sound.loudness(loop, hum.channels()));
            assertTrue(db < Sound.REPEATED_DB + 0.3 && db > Sound.REPEATED_DB - 6, "les fonds au niveau des répétitifs : " + db);
        }
    }

    @Test
    void unSonVideResteVideEtUnSonFaibleMonte() {
        assertEquals(0, Sound.leveled(new short[0], 1, Sound.RARE_DB).length);
        short[] quiet = new short[4410];
        for (int i = 0; i < quiet.length; i++) quiet[i] = (short) (300 * Math.sin(i * 0.1));
        auNiveau(Sound.leveled(quiet, 1, Sound.RARE_DB), 1, Sound.RARE_DB, "un son faible");
        short[] spike = new short[4410];
        spike[100] = 1000;
        auNiveau(Sound.leveled(spike, 1, Sound.RARE_DB), 1, Sound.RARE_DB, "un clic isolé, plafonné à la crête");
    }
}
