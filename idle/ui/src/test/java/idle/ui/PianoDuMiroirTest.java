package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Le piano du miroir (Salamander Grand Piano V3, CC BY 3.0) : seize notes de la gamme pentatonique, du grave à l'aigu,
 * au niveau des sons répétitifs ; la faute ; le piano dans toutes les banques, sauf le silence partiel. Et la membrane de
 * la brane relâchée, plus aiguë quand la tirée est forte.
 */
class PianoDuMiroirTest {

    /** Les passages par zéro par seconde : plus la note est haute, plus il y en a. */
    private static double crossings(short[] sound) {
        int count = 0;
        int end = Math.min(sound.length, 22_050);
        for (int i = 2_205; i < end; i++) if ((sound[i - 1] < 0) != (sound[i] < 0)) count++;
        return count / ((end - 2_205) / 44_100.0);
    }

    @Test
    void seizeNotesDuGraveALAiguAuNiveauDesRepetitifs() {
        assertEquals(Sound.PIANO_NOTES, Sound.PIANO.length);
        for (int note = 0; note < Sound.PIANO.length; note++) {
            assertEquals(Sound.PIANO_NOTE_SECONDS, Sound.PIANO[note].length / 44_100.0, 1e-3);
            NiveauxTest.auNiveau(Sound.PIANO[note], 1, Sound.REPEATED_DB, "la note " + note);
            int strong = 0;
            while (Math.abs(Sound.PIANO[note][strong]) < Short.MAX_VALUE / 20) strong++;
            assertTrue(strong / 44_100.0 < 0.02, "le marteau tout de suite : note " + note);
        }
        assertTrue(crossings(Sound.PIANO[15]) > 3 * crossings(Sound.PIANO[0]), "do7 bien plus aigu que do4");
        assertTrue(Sound.PIANO_WRONG.length > 0);
    }

    @Test
    void lePianoSonneDansLesBanquesSaufLeSilence() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.piano(3, 100));
        Thread.sleep(50);
        assertTrue(classic.piano(SoundEvents.WRONG, 100), "la faute");
        Sound wood = new Sound();
        wood.sink = sent::add;
        wood.setSkins(Skin.SOUND_WOOD, null, null);
        assertTrue(wood.piano(15, 100), "le même piano dans la banque Bois");
        Sound silent = new Sound();
        silent.sink = sent::add;
        silent.setSkins(Skin.SOUND_SILENCE, null, null);
        assertFalse(silent.piano(0, 100), "le silence partiel le coupe");
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.DIMENSIONS);
        assertFalse(muted.piano(0, 100), "la catégorie des dimensions coupée");
        for (Sound each : List.of(classic, wood)) {
            each.player.shutdown();
            assertTrue(each.player.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(3, sent.size());
        assertEquals(Sound.PIANO[3].length * 2, sent.get(0).bytes().length);
    }

    @Test
    void laMembraneRelacheeSonneSelonLaTiree() throws InterruptedException {
        short[] tone = new short[4_410];
        for (int i = 0; i < tone.length; i++) tone[i] = (short) (8_000 * Math.sin(i * 0.05));
        assertEquals(tone.length / 2, Sound.resampled(tone, 2).length, 1, "deux fois plus vite : deux fois plus court");
        assertTrue(crossings(pad(Sound.resampled(tone, 1.3))) > crossings(pad(tone)), "plus aigu");
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.gesture(6, 100, 1.3));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.resampled(Sound.leveled(Sound.synthesizeGesture(6), 1, Sound.REPEATED_DB), 1.3).length * 2, sent.get(0).bytes().length);
    }

    private static short[] pad(short[] sound) {
        short[] out = new short[44_100];
        for (int i = 0; i < out.length; i++) out[i] = sound[i % sound.length];
        return out;
    }
}
