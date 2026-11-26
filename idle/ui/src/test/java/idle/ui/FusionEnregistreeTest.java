package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * Version 0.306 : la fusion manuelle attend l'animation de regroupement seulement si l'onglet des particules est
     * affiché et qu'aucune animation n'est en cours ; demandée pendant une animation, le moteur la fait tout de suite
     * (avant, elle était refusée).
     */
    @Test
    void uneFusionDemandeePendantLAnimationSeFaitToutDeSuite() {
        assertFalse(ManualFusion.atOnce(true, false), "onglet affiché, pas d'animation : on regarde le regroupement");
        assertTrue(ManualFusion.atOnce(true, true), "pendant l'animation : tout de suite, dans le moteur");
        assertTrue(ManualFusion.atOnce(false, false), "onglet caché : personne ne verrait l'animation");
        assertTrue(ManualFusion.atOnce(false, true));
    }

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
    void laFusionPartEnStereoCommeLesBruitagesEtLesBanquesGardentLaLeur() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.play(Cue.ATOM, 100), "une fusion");
        assertTrue(classic.play(Cue.ATOM, 100), "deux fusions collées sonnent toutes les deux (0.247)");
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.ATOMS);
        assertFalse(muted.play(Cue.ATOM, 100), "la catégorie des fusions coupée");
        classic.player.shutdown();
        assertTrue(classic.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(2, sent.size());
        assertEquals(2, sent.stream().filter(Sound.Pcm::stereo).count(), "l'enregistrement en stéréo, les deux fois (les banques de sons retirées en 0.251)");
        assertEquals(Sound.FUSION_STEREO.length * 2, sent.stream().filter(Sound.Pcm::stereo).findFirst().orElseThrow().bytes().length);
    }
}
