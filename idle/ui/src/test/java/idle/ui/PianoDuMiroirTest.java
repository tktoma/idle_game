package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    void lePianoSonneEtSaCategorieLeCoupe() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound classic = new Sound();
        classic.sink = sent::add;
        assertTrue(classic.piano(3, 100));
        Thread.sleep(50);
        assertTrue(classic.piano(SoundEvents.WRONG, 100), "la faute");
        Sound muted = new Sound();
        muted.sink = sent::add;
        muted.setAllowed(kind -> kind != SoundKind.DIMENSIONS);
        assertFalse(muted.piano(0, 100), "la catégorie des dimensions coupée");
        classic.player.shutdown();
        assertTrue(classic.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(2, sent.size());
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
        assertEquals(0.8, Sound.BRANE_SNAP.length / 44_100.0, 0.01, "le claquement de la membrane, choisi à l'écoute");
        NiveauxTest.auNiveau(Sound.BRANE_SNAP, 1, Sound.REPEATED_DB, "le claquement de la membrane");
        assertEquals(Sound.resampled(Sound.BRANE_SNAP, 1.3).length * 2, sent.get(0).bytes().length, "le claquement, plus aigu");
    }

    private static short[] pad(short[] sound) {
        short[] out = new short[44_100];
        for (int i = 0; i < out.length; i++) out[i] = sound[i % sound.length];
        return out;
    }

    /** Le clicker : le tapotement, et dans une zone ×2 le même tapotement en accord, une note de plus par zone superposée. */
    @Test
    void leClicDuClickerEtSonAccordDansLesZones() throws InterruptedException {
        NiveauxTest.auNiveau(Sound.CLICKER_TAP, 1, Sound.REPEATED_DB, "le tapotement");
        assertEquals(4, Sound.CLICKER_BONUS.length);
        for (short[] chord : Sound.CLICKER_BONUS) NiveauxTest.auNiveau(chord, 1, Sound.REPEATED_DB, "un accord du bonus");
        // Plus de zones, plus de notes : depuis la 0.258, un arpège rapide du clic, une note de plus (et plus long) par zone.
        assertTrue(Sound.CLICKER_BONUS[0].length > Sound.CLICKER_TAP.length, "une zone : plus long que le clic seul");
        for (int k = 1; k < Sound.CLICKER_BONUS.length; k++) {
            assertEquals(Sound.CLICKER_BONUS[k - 1].length + (int) (Sound.CLICKER_STRUM * 44_100), Sound.CLICKER_BONUS[k].length,
                    (k + 1) + " zones : une note de plus");
        }
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.clicker(0, 100));
        assertTrue(sound.clicker(9, 100), "plus de quatre zones : l'accord le plus riche, sans attendre le clic d'avant");
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.CLICKER_TAP.length * 2, sent.get(0).bytes().length);
        assertEquals(Sound.clickerSound(9).length * 2, sent.get(1).bytes().length, "neuf zones : le son de huit, le plus aigu");
    }

    /** Version 0.258 : le clic du joueur (Kenney) sonne pour chaque bouton et pour le clicker, au niveau des répétitifs. */
    @Test
    void leClicDuJoueurSertAuxBoutonsEtAuClicker() throws InterruptedException {
        assertTrue(Sound.CLICK_SOUND.length > 0, "clic.wav se charge");
        assertEquals(0.007, Sound.CLICK_SOUND.length / 44_100.0, 0.003, "un clic de 7 ms");
        NiveauxTest.auNiveau(Sound.CLICK_SOUND, 1, Sound.REPEATED_DB, "le clic");
        assertSame(Sound.CLICK_SOUND, Sound.CLICKER_TAP, "le clicker prend le même clic");
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.click(Sound.Cue.CLICK, 100));
        assertTrue(sound.clicker(0, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.CLICK_SOUND.length * 2, sent.get(0).bytes().length, "un bouton : le clic enregistré");
        assertEquals(Sound.CLICK_SOUND.length * 2, sent.get(1).bytes().length, "le clicker : le même");
    }

    /** Version 0.263 : l'achat prend le son du joueur (achat.wav), au niveau des répétitifs, et c'est lui qui part. */
    @Test
    void lAchatPrendLeSonDuJoueur() throws InterruptedException {
        assertTrue(Sound.PURCHASE_SOUND.length > 0, "achat.wav se charge");
        assertEquals(0.049, Sound.PURCHASE_SOUND.length / 44_100.0, 0.005, "un achat de 49 ms");
        NiveauxTest.auNiveau(Sound.PURCHASE_SOUND, 1, Sound.REPEATED_DB, "l'achat");
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.click(Sound.Cue.PURCHASE, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.PURCHASE_SOUND.length * 2, sent.get(0).bytes().length, "un achat : le son enregistré");
    }

    /** Version 0.259 : le clic dans les zones monte d'un demi-ton par zone superposée, jusqu'à huit, légèrement. */
    @Test
    void leClicDesZonesMonteUnPeuAChaqueZone() {
        assertEquals(1, Sound.clickerPitch(0), 1e-12);
        assertEquals(Math.pow(2, 1 / 12.0), Sound.clickerPitch(1), 1e-12, "un demi-ton");
        for (int k = 1; k <= Sound.CLICKER_RISE_ZONES; k++) assertTrue(Sound.clickerPitch(k) > Sound.clickerPitch(k - 1), k + " zones");
        assertEquals(Sound.clickerPitch(Sound.CLICKER_RISE_ZONES), Sound.clickerPitch(50), 1e-12, "plafonnée");
        assertTrue(Sound.clickerPitch(Sound.CLICKER_RISE_ZONES) < 1.6, "légère : au plus huit demi-tons");
        assertSame(Sound.CLICKER_TAP, Sound.clickerSound(0), "sans zone : le clic tel quel");
        // Plus aigu, donc un peu plus court : le même arpège rééchantillonné.
        assertEquals(Sound.CLICKER_BONUS[0].length / Sound.clickerPitch(1), Sound.clickerSound(1).length, 2);
        assertEquals(Sound.CLICKER_BONUS[3].length / Sound.clickerPitch(6), Sound.clickerSound(6).length, 2, "au-delà de quatre zones, l'accord complet monte encore");
        for (int k = 1; k <= Sound.CLICKER_RISE_ZONES; k++) NiveauxTest.auNiveau(Sound.clickerSound(k), 1, Sound.REPEATED_DB, k + " zones");
        assertSame(Sound.clickerSound(3), Sound.clickerSound(3), "calculé une fois");
    }
}
