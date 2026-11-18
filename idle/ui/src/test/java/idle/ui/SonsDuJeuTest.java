package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.ui.Sound.Cue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Les sons des grands moments qui n'en avaient pas, les sons d'interface (achat, interrupteur) et les
 * gestes dans les dimensions. Sans affichage ni carte son : les sons partent dans un relevé.
 */
class SonsDuJeuTest {
    @Test
    void chaqueGrandMomentASonSon() {
        Map<String, Cue> expected = Map.ofEntries(
                Map.entry("Univers formé : les défis pèsent maintenant ×2 plus qu'au premier univers (×2 par univers formé).", Cue.UNIVERSE),
                Map.entry("Les dix paliers sont atteints : la partie est finie. Merci d'avoir joué !", Cue.GAME_END),
                Map.entry("Défi final : Le clicker atteint son palier (3 / 10).", Cue.FINALE),
                Map.entry("Skin débloqué : « Ciel de papier » (fond). +2 %", Cue.SKIN),
                Map.entry("Nouveau défi : Rouille", Cue.CHALLENGE),
                Map.entry("Défi « Rouille » commencé : les améliorations en particules ne servent à rien", Cue.CHALLENGE),
                Map.entry("« Noyaux sombres » à portée dans l'arbre : l'amélioration de matière noire qui compte le plus", Cue.DARK),
                Map.entry("Nouvel automatisme de matière noire : Arbre", Cue.DARK),
                Map.entry("Palier de taille : Molécule", Cue.DARK),
                Map.entry("Le tableau périodique peut exploser", Cue.DARK),
                Map.entry("Jalon : gaz réunis pour l'amas de galaxies", Cue.MILESTONE),
                Map.entry("Ensemble complet : Gaz nobles", Cue.MILESTONE),
                Map.entry("Palier de gluons : premier", Cue.MILESTONE),
                Map.entry(Notifier.NEW_TITLE + "Bâtisseur", Cue.MILESTONE),
                Map.entry("Premier atome", Cue.MILESTONE),
                // Ce qui sonnait déjà garde son son.
                Map.entry("Big Bang n° 3 : tout repart", Cue.BIG_BANG),
                Map.entry("Explosion : +12 matière noire", Cue.EXPLOSION),
                Map.entry("Défi réussi : Rouille", Cue.VICTORY),
                Map.entry("Défi de Big Bang « Ciel » réussi", Cue.VICTORY),
                Map.entry("Succès : Cent fusions", Cue.ACHIEVEMENT));
        for (Map.Entry<String, Cue> entry : expected.entrySet()) assertEquals(entry.getValue(), Sound.cueOf(entry.getKey()), entry.getKey());
        // Ce qui revient sans cesse avec les automatismes ne sonne pas, ni un défi abandonné.
        for (String quiet : List.of("Formation automatique : « Soleil jaune »", "Rassemblement automatique : 3 sortes rassemblées",
                "Défi « Rouille » abandonné : la partie repart sans contrainte.")) {
            assertNull(Sound.cueOf(quiet), quiet);
        }
        assertEquals(Cue.UNIVERSE, Sound.strongest(List.of("Skin débloqué : « Bois »", "Explosion : +3", "Univers formé : ×2")),
                "l'univers formé passe devant l'explosion et le skin");
        assertEquals(Cue.BIG_BANG, Sound.strongest(List.of("Univers formé : ×2", "Big Bang n° 1")));
    }

    @Test
    void lesSonsDInterfaceNeRetardentPasLesAnnonces() throws InterruptedException {
        List<byte[]> sent = new ArrayList<>();
        Sound sound = new Sound();
        sound.sink = pcm -> sent.add(pcm.bytes());
        assertTrue(sound.play(Cue.ACHIEVEMENT, 100));
        assertTrue(sound.click(Cue.PURCHASE, 100), "un achat juste après une annonce sonne");
        // Version 0.247 : aucun délai entre deux sons, même pareils.
        assertTrue(sound.play(Cue.SKIN, 100), "une seconde annonce collée sonne aussi");
        assertTrue(sound.play(Cue.SKIN, 100), "et la même encore");
        assertTrue(sound.click(Cue.TOGGLE, 100), "un interrupteur aussi");
        assertTrue(sound.click(Cue.PURCHASE, 100), "deux achats collés sonnent tous les deux");
        assertFalse(sound.click(Cue.VICTORY, 100), "une annonce ne passe pas par les sons d'interface");
        assertTrue(sound.play(Cue.PURCHASE, 100), "un achat joué comme une annonce sonne en son d'interface");
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(7, sent.size());
    }

    @Test
    void chaqueDimensionASonGeste() {
        List<short[]> gestures = new ArrayList<>();
        for (int dimension = 1; dimension <= 10; dimension++) {
            if (dimension == 7) continue; // le clicker a son tapotement, testé avec le piano du miroir
            short[] gesture = Sound.synthesizeGesture(dimension);
            assertTrue(gesture.length > 0 && gesture.length <= 0.4 * 44_100, "court : dimension " + dimension);
            assertTrue(loud(gesture), "s'entend : dimension " + dimension);
            for (short sample : gesture) assertTrue(Math.abs(sample) <= Math.round(0.7 * Short.MAX_VALUE) + 1, "sans saturation : dimension " + dimension);
            for (short[] other : gestures) assertFalse(Arrays.equals(other, gesture), "chaque dimension sonne à sa façon : " + dimension);
            gestures.add(gesture);
        }
    }

    @Test
    void lesGestesSuiventLeurCategorie() throws InterruptedException {
        List<byte[]> sent = new ArrayList<>();
        Sound sound = new Sound();
        sound.sink = pcm -> sent.add(pcm.bytes());
        assertFalse(sound.gesture(0, 100), "pas de dimension zéro");
        assertFalse(sound.gesture(11, 100), "ni onze");
        assertFalse(sound.gesture(7, 0), "au volume nul, rien");
        assertTrue(sound.gesture(7, 100));
        assertTrue(sound.gesture(7, 100), "deux clics collés sonnent tous les deux (0.247)");
        assertTrue(sound.gesture(3, 100), "une autre dimension non plus n'attend pas");
        sound.setAllowed(kind -> kind != SoundKind.DIMENSIONS);
        assertFalse(sound.gesture(7, 100), "la catégorie des dimensions coupe les gestes");
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(3, sent.size());
        assertEquals(Sound.CLICKER_TAP.length * 2, sent.get(0).length);
        short loudest = 0;
        for (int i = 0; i < sent.get(0).length; i += 2) {
            loudest = (short) Math.max(loudest, Math.abs((short) ((sent.get(0)[i] & 0xff) | sent.get(0)[i + 1] << 8)));
        }
        assertTrue(loudest <= Math.round(Sound.MAX_PEAK * Short.MAX_VALUE) + 1, "sans saturer : " + loudest);
    }

    /** Vrai si le son dépasse un dixième de la pleine échelle à un moment. */
    private static boolean loud(short[] samples) {
        for (short sample : samples) if (Math.abs(sample) > Short.MAX_VALUE / 10) return true;
        return false;
    }

    /** Version 0.247 : aucun délai entre deux sons ; seul le nombre de sons en même temps est borné, pour ne pas saturer la sortie. */
    @Test
    void sansDelaiMaisAuPlusQuelquesSonsEnMemeTemps() throws InterruptedException {
        List<byte[]> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = pcm -> sent.add(pcm.bytes());
        for (int voice = 0; voice < Sound.MAX_VOICES; voice++) assertTrue(sound.gesture(3, 100), "le son " + (voice + 1) + " part tout de suite");
        assertFalse(sound.gesture(3, 100), "au-delà, la sortie n'est pas noyée");
        assertFalse(sound.click(Cue.PURCHASE, 100), "la borne vaut pour tous les sons");
        // Le geste du volume dure 0,16 s : une fois fini, sa place se libère.
        Thread.sleep(250);
        assertTrue(sound.gesture(3, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.MAX_VOICES + 1, sent.size());
    }
}
