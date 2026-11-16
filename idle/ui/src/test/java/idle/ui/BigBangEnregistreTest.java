package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.ui.Sound.Cue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Le Big Bang par défaut : l'explosion enregistrée, en stéréo, et l'accord du Big Bang par-dessus. Les skins de sons
 * qui ont leur propre Big Bang le gardent, en mono. Sans carte son : les sons partis sont relevés.
 */
class BigBangEnregistreTest {

    @Test
    void leMelangeEstStereoAuNiveauRetenuEtSansSaturation() {
        short[] mix = Sound.BIG_BANG_STEREO;
        assertTrue(mix.length > 0, "la ressource se charge");
        assertEquals(0, mix.length % 2, "gauche et droite par paires");
        double seconds = mix.length / 2 / 44_100.0;
        assertTrue(seconds > 2.4 && seconds < 2.7, "l'explosion entière, l'accord compris : " + seconds);
        int peak = 0;
        double same = 0, left = 0, right = 0;
        for (int i = 0; i < mix.length; i += 2) {
            peak = Math.max(peak, Math.max(Math.abs(mix[i]), Math.abs(mix[i + 1])));
            same += (double) mix[i] * mix[i + 1];
            left += (double) mix[i] * mix[i];
            right += (double) mix[i + 1] * mix[i + 1];
        }
        assertEquals(Math.round(Sound.BIG_BANG_PEAK * Short.MAX_VALUE), peak, 2, "la crête retenue à l'écoute, sans écrêter");
        double correlation = same / Math.sqrt(left * right);
        assertTrue(correlation < 0.9, "deux canaux bien différents : la stéréo est gardée (" + correlation + ")");
    }

    @Test
    void leBigBangParDefautSeuleQuandAucunSkinNaLeSien() {
        assertTrue(SoundBanks.classicBigBang(null, null), "le Classique");
        assertTrue(SoundBanks.classicBigBang(Skin.SOUND_SILENCE, null), "le silence partiel garde le Big Bang par défaut");
        for (Skin bank : List.of(Skin.SOUND_WOOD, Skin.SOUND_GLASS, Skin.SOUND_BITS, Skin.SOUND_ORCHESTRA, Skin.SOUND_NATURE)) {
            assertFalse(SoundBanks.classicBigBang(bank, null), bank + " a son Big Bang");
        }
        assertFalse(SoundBanks.classicBigBang(null, Skin.BANG_SOUND_WIND), "un son de Big Bang choisi passe devant");
    }

    @Test
    void leBigBangPartEnStereoEtLesAutresSonsEnMono() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.play(Cue.BIG_BANG, 100));
        Sound wood = new Sound();
        wood.sink = sent::add;
        wood.setSkins(Skin.SOUND_WOOD, null, null);
        assertTrue(wood.play(Cue.BIG_BANG, 100));
        Sound other = new Sound();
        other.sink = sent::add;
        assertTrue(other.play(Cue.EXPLOSION, 100));
        for (Sound each : List.of(sound, wood, other)) {
            each.player.shutdown();
            assertTrue(each.player.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(3, sent.size());
        Sound.Pcm bang = sent.stream().filter(Sound.Pcm::stereo).findFirst().orElseThrow();
        assertEquals(Sound.BIG_BANG_STEREO.length * 2, bang.bytes().length, "le mélange entier, au volume plein");
        assertEquals(1, sent.stream().filter(Sound.Pcm::stereo).count(), "seul le Big Bang par défaut est en stéréo");
    }

    @Test
    void leBigBangSonneMemeNoyeDansLeResumeDesAnnonces() {
        // Un Big Bang qui bat des records et réussit un défi : plus de quatre annonces, le Big Bang en dernier.
        List<String> all = List.of("Défi de Big Bang « Rouille » réussi : ×2", "Record battu : premier atome", "Record battu : explosion",
                "Record battu : matière noire", "Skin débloqué : « Papier » (fond)", "Big Bang n° 3 : tout repart du premier générateur.");
        List<String> shown = Notifier.summarize(all);
        assertFalse(shown.stream().anyMatch(text -> text.startsWith("Big Bang")), "le résumé le cache à l'écran");
        assertEquals(Cue.BIG_BANG, Sound.strongest(all), "le son se choisit sur tout ce qui s'est passé, pas sur le résumé : le Big Bang sonne");
    }

    @Test
    void laStereoRameneeEnMonoGardeLeSon() {
        byte[] stereo = Sound.pcm(Sound.BIG_BANG_STEREO, 1);
        byte[] mono = Sound.mono(stereo);
        assertEquals(stereo.length / 2, mono.length);
        int loud = 0;
        for (int i = 0; i < mono.length / 2; i++) loud = Math.max(loud, Math.abs((short) ((mono[2 * i] & 0xff) | (mono[2 * i + 1] << 8))));
        assertTrue(loud > Short.MAX_VALUE / 4, "en mono, il s'entend encore : " + loud);
    }
}
