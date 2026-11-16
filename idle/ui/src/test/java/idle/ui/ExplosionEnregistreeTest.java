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
 * L'explosion du tableau par défaut : l'enregistrement, en stéréo, au niveau de l'explosion d'avant. Une banque de sons
 * choisie garde la sienne ; le silence partiel la coupe toujours. Sans carte son : les sons partis sont relevés.
 */
class ExplosionEnregistreeTest {

    @Test
    void lEnregistrementEstStereoCourtEtSousLaCreteDesAnnonces() {
        short[] recorded = Sound.EXPLOSION_STEREO;
        assertTrue(recorded.length > 0, "la ressource se charge");
        assertEquals(0, recorded.length % 2);
        assertEquals(3.6, recorded.length / 2 / 44_100.0, 0.01, "la queue inaudible est coupée, en fondu");
        double same = 0, left = 0, right = 0;
        for (int i = 0; i < recorded.length; i += 2) {
            same += (double) recorded[i] * recorded[i + 1];
            left += (double) recorded[i] * recorded[i];
            right += (double) recorded[i + 1] * recorded[i + 1];
        }
        NiveauxTest.auNiveau(recorded, 2, Sound.RARE_DB, "l'explosion");
        assertTrue(Math.abs(recorded[recorded.length - 1]) < 50 && Math.abs(recorded[recorded.length - 2]) < 50, "la fin s'éteint sans claquer");
        assertTrue(same / Math.sqrt(left * right) < 0.9, "deux canaux différents : la stéréo est gardée");
    }

    @Test
    void lExplosionParDefautPartEnStereoEtLesBanquesGardentLaLeur() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.play(Cue.EXPLOSION, 100));
        Sound wood = new Sound();
        wood.sink = sent::add;
        wood.setSkins(Skin.SOUND_WOOD, null, null);
        assertTrue(wood.play(Cue.EXPLOSION, 100));
        Sound silent = new Sound();
        silent.sink = sent::add;
        silent.setSkins(Skin.SOUND_SILENCE, null, null);
        assertFalse(silent.play(Cue.EXPLOSION, 100), "le silence partiel coupe l'explosion, comme avant");
        for (Sound each : List.of(classic, wood)) {
            each.player.shutdown();
            assertTrue(each.player.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(2, sent.size());
        Sound.Pcm recorded = sent.stream().filter(Sound.Pcm::stereo).findFirst().orElseThrow();
        assertEquals(Sound.EXPLOSION_STEREO.length * 2, recorded.bytes().length, "l'enregistrement entier, au volume plein");
        assertEquals(1, sent.stream().filter(Sound.Pcm::stereo).count(), "la banque Bois garde son explosion, en mono");
    }
}
