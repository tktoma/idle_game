package idle.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.Sound.Cue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Version 0.200 : les skins des sons. Chaque son se synthétise dans chaque banque, et sonne autrement que le Classique. Sans affichage ni carte son. */
class SoundBanksTest {

    private static List<Skin> of(SkinVisual visual) {
        return Arrays.stream(Skin.values()).filter(skin -> skin.visual() == visual).toList();
    }

    @Test
    void chaqueBanqueChangeChaqueSon() {
        List<Skin> banks = of(SkinVisual.SOUND_BANK);
        assertEquals(6, banks.size());
        for (Cue cue : Cue.values()) {
            short[] classic = Sound.synthesize(cue);
            assertArrayEquals(classic, SoundBanks.synthesize(cue, null, null, null), "sans skin, le Classique : " + cue);
            for (Skin bank : banks) {
                short[] skinned = SoundBanks.synthesize(cue, bank, null, null);
                if (bank == Skin.SOUND_SILENCE) {
                    if (cue == Cue.BIG_BANG) assertArrayEquals(classic, skinned, "le silence partiel garde le Big Bang");
                    else assertEquals(0, skinned.length, "le silence partiel coupe " + cue);
                    continue;
                }
                assertTrue(skinned.length > 0, bank + " " + cue);
                assertFalse(Arrays.equals(classic, skinned), bank + " doit changer " + cue);
                assertTrue(loud(skinned), bank + " " + cue + " doit s'entendre");
                for (short sample : skinned) assertTrue(Math.abs(sample) <= Math.round(0.7 * Short.MAX_VALUE) + 1, "jamais de saturation : " + bank + " " + cue);
            }
        }
    }

    @Test
    void leBigBangEtLeMetronomeOntLeurSonEtPassentDevantLaBanque() {
        short[] classicBang = Sound.synthesize(Cue.BIG_BANG), classicBeat = Sound.synthesize(Cue.BEAT);
        List<Skin> bangs = of(SkinVisual.BIG_BANG_SOUND);
        assertEquals(3, bangs.size());
        for (Skin bang : bangs) {
            short[] sound = SoundBanks.synthesize(Cue.BIG_BANG, null, bang, null);
            assertFalse(Arrays.equals(classicBang, sound), bang.label());
            assertTrue(loud(sound), bang.label());
            assertArrayEquals(sound, SoundBanks.synthesize(Cue.BIG_BANG, Skin.SOUND_WOOD, bang, null), "le son du Big Bang passe devant la banque");
            assertArrayEquals(sound, SoundBanks.synthesize(Cue.BIG_BANG, Skin.SOUND_SILENCE, bang, null), "même en silence partiel");
        }
        List<Skin> metronomes = of(SkinVisual.METRONOME);
        assertEquals(4, metronomes.size());
        for (Skin metronome : metronomes) {
            short[] sound = SoundBanks.synthesize(Cue.BEAT, null, null, metronome);
            assertFalse(Arrays.equals(classicBeat, sound), metronome.label());
            assertTrue(loud(sound), metronome.label());
            assertArrayEquals(sound, SoundBanks.synthesize(Cue.BEAT, Skin.SOUND_GLASS, null, metronome), "le tic choisi passe devant la banque");
        }
        // Un son d'annonce n'est pas touché par ces deux skins-là.
        assertArrayEquals(Sound.synthesize(Cue.COMET), SoundBanks.synthesize(Cue.COMET, null, Skin.BANG_SOUND_ORGAN, Skin.METRONOME_HEART));
    }

    @Test
    void lesSonsRelusSuiventLeChoixEtLeSilenceNePartPas() throws InterruptedException {
        List<byte[]> sent = new java.util.ArrayList<>();
        Sound silent = new Sound();
        silent.sink = sent::add;
        silent.setSkins(Skin.SOUND_SILENCE, null, null);
        assertFalse(silent.play(Cue.ACHIEVEMENT, 100), "le silence partiel ne joue pas les annonces");
        assertFalse(silent.beat(100), "ni le métronome");
        assertTrue(silent.play(Cue.BIG_BANG, 100), "mais le Big Bang");
        Sound skinned = new Sound();
        skinned.sink = sent::add;
        skinned.setSkins(Skin.SOUND_BITS, Skin.BANG_SOUND_WIND, Skin.METRONOME_METAL);
        assertTrue(skinned.beat(100));
        assertTrue(skinned.play(Cue.BIG_BANG, 100));
        skinned.player.shutdown();
        silent.player.shutdown();
        assertTrue(skinned.player.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS) && silent.player.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(3, sent.size());
        assertEquals(Sound.pcm(SoundBanks.synthesize(Cue.BIG_BANG, null, Skin.BANG_SOUND_WIND, null), 1).length, sent.get(2).length, "le souffle de vent, au volume plein");
    }

    /** Vrai si le son dépasse un dixième de la pleine échelle à un moment. */
    private static boolean loud(short[] samples) {
        for (short sample : samples) if (Math.abs(sample) > Short.MAX_VALUE / 10) return true;
        return false;
    }
}
