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
 * La fusion des générateurs par défaut : l'enregistrement tel quel (stéréo, canaux presque opposés), joué au début de
 * l'animation de fusion. Une banque de sons choisie garde la sienne. Sans carte son : les sons partis sont relevés.
 */
class FusionEnregistreeTest {

    @Test
    void lEnregistrementEstGardeTelQuel() {
        short[] recorded = Sound.FUSION_STEREO;
        assertTrue(recorded.length > 0, "la ressource se charge");
        assertEquals(2.14, recorded.length / 2 / 44_100.0, 0.01, "le son entier, sans le silence de fin");
        double same = 0, left = 0, right = 0;
        for (int i = 0; i < recorded.length; i += 2) {
            same += (double) recorded[i] * recorded[i + 1];
            left += (double) recorded[i] * recorded[i];
            right += (double) recorded[i + 1] * recorded[i + 1];
        }
        NiveauxTest.auNiveau(recorded, 2, Sound.REPEATED_DB, "la fusion");
        assertTrue(same / Math.sqrt(left * right) < -0.9, "les canaux restent presque opposés, comme le fichier");
    }

    @Test
    void laFusionPartEnStereoAuRythmeDesBruitagesEtLesBanquesGardentLaLeur() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.play(Cue.ATOM, 100), "une fusion");
        assertFalse(classic.play(Cue.ATOM, 100), "deux fusions collées n'en font qu'une");
        Sound wood = new Sound();
        wood.sink = sent::add;
        wood.setSkins(Skin.SOUND_WOOD, null, null);
        assertTrue(wood.play(Cue.ATOM, 100));
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.ATOMS);
        assertFalse(muted.play(Cue.ATOM, 100), "la catégorie des fusions coupée");
        for (Sound each : List.of(classic, wood)) {
            each.player.shutdown();
            assertTrue(each.player.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(2, sent.size());
        assertEquals(1, sent.stream().filter(Sound.Pcm::stereo).count(), "l'enregistrement en stéréo, la banque Bois en mono");
        assertEquals(Sound.FUSION_STEREO.length * 2, sent.stream().filter(Sound.Pcm::stereo).findFirst().orElseThrow().bytes().length);
    }
}
