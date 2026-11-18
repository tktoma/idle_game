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
 * Un atome créé, sur le grand atome de l'onglet Atomes : l'enregistrement, en stéréo, sans son silence de fin, au niveau
 * des sons répétitifs. Une banque de sons choisie garde le sien ; la catégorie des fusions le coupe.
 */
class AtomeEnregistreTest {

    @Test
    void lEnregistrementSeChargeSansSonSilence() {
        short[] recorded = Sound.NEW_ATOM_STEREO;
        assertTrue(recorded.length > 0, "la ressource se charge");
        assertEquals(0, recorded.length % 2, "gauche et droite par paires");
        assertEquals(1.45, recorded.length / 2 / 44_100.0, 0.01, "le son entier, sans le silence de fin");
        NiveauxTest.auNiveau(recorded, 2, Sound.REPEATED_DB, "l'atome créé");
        assertTrue(Math.abs(recorded[recorded.length - 1]) < 50 && Math.abs(recorded[recorded.length - 2]) < 50, "la fin est éteinte");
    }

    @Test
    void lAtomeCreePartEnStereoCommeLesBruitagesEtLesBanquesGardentLeLeur() throws InterruptedException {
        assertEquals(SoundKind.ATOMS, Cue.NEW_ATOM.kind(), "rangé avec les fusions");
        assertTrue(Cue.NEW_ATOM.isInterface(), "un bruitage, hors du rythme des annonces");
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.click(Cue.NEW_ATOM, 100), "un atome");
        assertTrue(classic.click(Cue.NEW_ATOM, 100), "deux atomes collés sonnent tous les deux (0.247)");
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.ATOMS);
        assertFalse(muted.click(Cue.NEW_ATOM, 100), "la catégorie des fusions coupée");
        classic.player.shutdown();
        assertTrue(classic.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(2, sent.size());
        assertEquals(2, sent.stream().filter(Sound.Pcm::stereo).count(), "l'enregistrement en stéréo, les deux fois (les banques de sons retirées en 0.251)");
        assertEquals(Sound.NEW_ATOM_STEREO.length * 2, sent.stream().filter(Sound.Pcm::stereo).findFirst().orElseThrow().bytes().length);
    }
}
