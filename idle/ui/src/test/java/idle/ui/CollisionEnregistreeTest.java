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
 * Une collision, sur l'onglet du collisionneur : l'enregistrement, en stéréo, la queue inaudible coupée en fondu, au niveau
 * des sons répétitifs. Une banque de sons choisie garde la sienne ; la catégorie des collisions la coupe.
 */
class CollisionEnregistreeTest {

    @Test
    void lEnregistrementSeChargeEtSEteintEnDouceur() {
        short[] recorded = Sound.COLLISION_STEREO;
        assertTrue(recorded.length > 0, "la ressource se charge");
        assertEquals(0, recorded.length % 2, "gauche et droite par paires");
        assertEquals(1.5, recorded.length / 2 / 44_100.0, 0.01, "le son entier, sans le silence de fin");
        NiveauxTest.auNiveau(recorded, 2, Sound.COLLISION_DB, "la collision");
        int strong = 0;
        while (strong < recorded.length && Math.abs(recorded[strong]) < Short.MAX_VALUE / 10) strong++;
        assertTrue(strong / 2 / 44_100.0 < 0.03, "le choc tombe tout de suite, sans montée : " + strong / 2 / 44_100.0 + " s");
        assertTrue(Math.abs(recorded[recorded.length - 1]) < 50 && Math.abs(recorded[recorded.length - 2]) < 50, "la fin est éteinte");
    }

    @Test
    void laCollisionPartEnStereoCommeLesBruitagesEtLesBanquesGardentLaLeur() throws InterruptedException {
        assertEquals(SoundKind.COLLIDER, Cue.COLLISION.kind(), "rangée avec les collisions");
        assertTrue(Cue.COLLISION.isInterface(), "un bruitage, hors du rythme des annonces");
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.click(Cue.COLLISION, 100), "une collision");
        assertTrue(classic.click(Cue.COLLISION, 100), "deux collisions collées sonnent toutes les deux (0.247)");
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.COLLIDER);
        assertFalse(muted.click(Cue.COLLISION, 100), "la catégorie des collisions coupée");
        classic.player.shutdown();
        assertTrue(classic.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(2, sent.size());
        assertEquals(2, sent.stream().filter(Sound.Pcm::stereo).count(), "l'enregistrement en stéréo, les deux fois (les banques de sons retirées en 0.251)");
        assertEquals(Sound.COLLISION_STEREO.length * 2, sent.stream().filter(Sound.Pcm::stereo).findFirst().orElseThrow().bytes().length);
    }
}
