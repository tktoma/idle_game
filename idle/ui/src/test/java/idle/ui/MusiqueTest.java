package idle.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.*;
import idle.ui.Music.Mood;
import idle.ui.Music.Scene;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * La musique de fond. Elle suit la partie, scène par scène ; elle se compose sans saturer, sans couper,
 * de la même façon à chaque fois ; chaque univers a sa tonalité ; elle se coupe et reprend avec un fondu. Sans affichage
 * ni carte son : les blocs partent dans un relevé.
 */
class MusiqueTest {
    private static Game partie() {
        Game game = new Game(42L);
        game.start();
        return game;
    }

    /** Rend {@code seconds} secondes de musique, bloc par bloc. */
    private static float[] rendre(Music.Engine engine, double seconds) {
        float[] all = new float[(int) (seconds * Music.RATE / Music.BLOCK) * Music.BLOCK];
        float[] block = new float[Music.BLOCK];
        for (int at = 0; at < all.length; at += Music.BLOCK) {
            engine.render(block);
            System.arraycopy(block, 0, all, at, Music.BLOCK);
        }
        return all;
    }

    private static double rms(float[] samples) {
        double sum = 0;
        for (float sample : samples) sum += sample * sample;
        return Math.sqrt(sum / samples.length);
    }

    @Test
    void laSceneSuitLeMomentDeLaPartie() {
        Game game = partie();
        assertEquals(Scene.AUBE, Music.sceneOf(game, 0), "avant la première explosion");
        game.state().setExplosions(1);
        assertEquals(Scene.ELAN, Music.sceneOf(game, 0), "vers le premier Big Bang");
        game.state().setBigBangs(2);
        assertEquals(Scene.BIG_BANGS, Music.sceneOf(game, 0));
        game.state().setBigBangs(Music.LAST_BIG_BANG);
        assertEquals(Scene.GALAXIE, Music.sceneOf(game, 0), "le dernier Big Bang fait : on attend les astres");
        game.state().setCosmosLevel(1);
        assertEquals(Scene.AMAS, Music.sceneOf(game, 0));
        game.state().setCosmosLevel(2);
        assertEquals(Scene.FORMATION, Music.sceneOf(game, 0));
        assertEquals(Scene.DIMENSION, Music.sceneOf(game, 3), "une dimension ouverte passe devant l'étape");
        game.state().setActiveChallenge(game.challenges().get(0).id());
        assertEquals(Scene.DEFI, Music.sceneOf(game, 3), "un défi passe devant la dimension ouverte");
        game.state().setActiveChallenge(null);
        game.testFormUniverse();
        assertEquals(Scene.UNIVERS, Music.sceneOf(game, 0));
        game.state().setTreeBubbleLevel(GameUniverse.TreeBubble.ROOTS.ordinal(), 1);
        assertTrue(game.isFinalChallengeOpen());
        assertEquals(Scene.FINALE, Music.sceneOf(game, 2), "le défi final passe devant tout, sauf la fin");
        game.state().setGameFinished(true);
        assertEquals(Scene.FIN, Music.sceneOf(game, 2));
    }

    @Test
    void lUniversEnCoursDonneLaTonalite() {
        Game game = partie();
        assertEquals(1, Music.moodOf(game, 0).universe(), "le premier univers se joue");
        game.testFormUniverse();
        assertEquals(1, Music.moodOf(game, 0).universe(), "il vient de se former, le suivant n'est pas lancé");
        assertTrue(game.testAddUniverse());
        assertEquals(2, Music.moodOf(game, 0).universe());
        assertEquals(4, Music.moodOf(game, 4).dimension());
        Set<Integer> tonics = new HashSet<>();
        for (int universe = 1; universe <= 11; universe++) {
            int tonic = Music.tonic(universe);
            assertTrue(tonic >= 40 && tonic <= 51, "du mi grave au ré dièse : " + tonic);
            tonics.add(tonic);
        }
        assertEquals(11, tonics.size(), "onze univers, onze tonalités");
        assertEquals(45, Music.tonic(1), "le premier univers en la");
        assertEquals(40, Music.tonic(2), "de quinte en quinte : mi sous le la");
        assertEquals(110, Music.frequency(45), 1e-9);
    }

    @Test
    void chaqueSceneSonneSansSaturerEtToujoursPareil() {
        for (Scene scene : Scene.values()) {
            Mood mood = new Mood(scene, 3, scene == Scene.DIMENSION ? 7 : 0);
            Music.Engine a = new Music.Engine(201), b = new Music.Engine(201);
            a.setMood(mood);
            b.setMood(mood);
            float[] one = rendre(a, 8), two = rendre(b, 8);
            assertArrayEquals(one, two, scene + " : même graine, même musique");
            for (float sample : one) assertTrue(Math.abs(sample) <= 0.7f, scene + " : jamais au-dessus de 70 % de la pleine échelle");
            assertTrue(rms(one) > 0.03, scene + " doit s'entendre : " + rms(one));
        }
    }

    @Test
    void lesScenesQuiPressentJouentPlusDeNotesEtPlusVite() {
        Music.Engine calme = new Music.Engine(201), defi = new Music.Engine(201);
        calme.setMood(new Mood(Scene.GALAXIE, 1, 0));
        defi.setMood(new Mood(Scene.DEFI, 1, 0));
        rendre(calme, 30);
        rendre(defi, 30);
        assertTrue(defi.notesStarted() > 3 * calme.notesStarted(), defi.notesStarted() + " notes contre " + calme.notesStarted());
        // Le style retenu garde un tempo fixe : c'est la densité qui presse, jamais une accélération.
        assertEquals(Music.Style.DEFAULT.tempo(), defi.bpm(), 1e-9, "le tempo du défi");
        assertEquals(Music.Style.DEFAULT.tempo(), calme.bpm(), 1e-9, "celui des astres");
    }

    @Test
    void unChangementDeSceneGlisseSansCouper() {
        Music.Engine engine = new Music.Engine(201);
        engine.setMood(new Mood(Scene.AUBE, 1, 0));
        float[] before = rendre(engine, 10);
        engine.setMood(new Mood(Scene.FINALE, 11, 0));
        // La nouvelle scène attend la fin de la section : 8 mesures, moins de 30 s au tempo 69.
        float[] after = rendre(engine, 30);
        assertEquals(Music.Style.DEFAULT.tempo(), engine.bpm(), 1e-9, "le tempo ne bouge pas");
        double jump = Math.abs(after[0] - before[before.length - 1]);
        for (int i = 1; i < after.length; i++) jump = Math.max(jump, Math.abs(after[i] - after[i - 1]));
        assertTrue(jump < 0.25, "aucun claquement au changement : " + jump);
        assertEquals(Music.tonic(11), engine.playingTonic(), "la nouvelle tonalité est entrée à la fin de la section");
    }

    /**
     * Les règles d'écriture, dans chaque scène et trois tonalités : toutes les notes dans la gamme ; les nappes, la basse
     * et l'arpège dans l'accord ; la mélodie dans l'accord à chaque temps fort, jamais un demi-ton au-dessus d'une note de
     * l'accord, jamais un saut de plus d'une quinte ni un triton.
     */
    @Test
    void laMusiqueSuitSesReglesDEcriture() {
        for (Scene scene : Scene.values()) {
            for (int universe : new int[] {1, 4, 11}) {
                Music.Engine engine = new Music.Engine(universe * 31L);
                engine.setMood(new Mood(scene, universe, scene == Scene.DIMENSION ? universe % 10 + 1 : 0));
                List<Music.Engine.Played> played = new ArrayList<>();
                engine.onPlayed = played::add;
                rendre(engine, 40);
                String where = scene + ", univers " + universe;
                assertTrue(played.size() > 15, where + " : " + played.size() + " notes");
                int previous = -1;
                for (Music.Engine.Played note : played) {
                    int pitchClass = Math.floorMod(note.midi() - note.tonic(), 12);
                    boolean inChord = false;
                    for (int tone : note.chord()) inChord |= tone == pitchClass;
                    assertTrue(note.mode().contains(pitchClass), where + " : " + note + " hors de la gamme");
                    boolean melody = note.role() == Music.Engine.Role.FLUTE || note.role() == Music.Engine.Role.BELL;
                    if (!melody || note.strong()) assertTrue(inChord, where + " : " + note + " hors de l'accord");
                    if (!melody) continue;
                    if (!inChord) for (int tone : note.chord()) assertTrue(Math.floorMod(pitchClass - tone, 12) != 1, where + " : " + note + " grince");
                    if (previous >= 0) {
                        int leap = Math.abs(note.midi() - previous);
                        assertTrue(leap <= Music.Engine.MAX_LEAP && leap != 6, where + " : saut de " + leap + " demi-tons vers " + note);
                    }
                    previous = note.midi();
                }
            }
        }
    }

    /** Le style retenu, mais avec des nappes tenues d'un accord à l'autre. */
    private static Music.Style tenues() {
        Music.Style d = Music.Style.DEFAULT;
        return new Music.Style(d.tempo(), d.tempoSpread(), d.changeEvery(), d.fluteHigh(), d.bellHigh(), d.bell(), d.lead(), d.shaker(),
                d.pulse(), d.reverb(), d.echo(), d.drive(), d.rests(), d.arpeggio(), d.melody(), d.pad(), d.padBright(), d.padDetune(),
                d.bass(), 0, 0);
    }

    @Test
    void lesNappesRespirentAChaqueAccord() {
        // Le style retenu : chaque accord entre, puis s'éteint avant le suivant, en laissant deux temps de silence.
        Music.Engine engine = new Music.Engine(201);
        engine.setMood(new Mood(Scene.ELAN, 1, 0));
        List<Double> entries = new ArrayList<>();
        long[] clock = {0};
        engine.onPlayed = note -> {
            if (note.role() == Music.Engine.Role.PAD) entries.add(clock[0] / (double) Music.RATE);
        };
        float[] block = new float[Music.BLOCK];
        for (int i = 0; i < 60 * Music.RATE / Music.BLOCK; i++) {
            engine.render(block);
            clock[0] += Music.BLOCK;
        }
        assertEquals(0, entries.size() % 4, "chaque accord entre en entier : " + entries.size());
        assertTrue(entries.size() >= 4 * 7, "un accord toutes les deux mesures : " + entries.size());
    }

    @Test
    void lesNappesPassentDUnAccordAuSuivantParLePlusCourtChemin() {
        Music.Engine engine = new Music.Engine(201, tenues());
        engine.setMood(new Mood(Scene.ELAN, 1, 0));
        List<Music.Engine.Played> pads = new ArrayList<>();
        engine.onPlayed = note -> { if (note.role() == Music.Engine.Role.PAD) pads.add(note); };
        rendre(engine, 60);
        assertTrue(pads.size() >= 8, "des accords se suivent : " + pads.size());
        // Une note commune à deux accords reste tenue : chaque accord n'en fait entrer que deux ou trois.
        assertTrue(pads.size() < 4 * 8, "les notes communes ne se rejouent pas : " + pads.size() + " entrées de nappes");
        for (Music.Engine.Played pad : pads) {
            assertTrue(pad.midi() >= Music.Engine.PAD_LOW && pad.midi() <= Music.Engine.PAD_HIGH, "dans le registre des nappes : " + pad);
        }
    }

    @Test
    void unGrandMomentFaitMonterLaMusique() {
        Music.Engine sans = new Music.Engine(201), avec = new Music.Engine(201);
        for (Music.Engine engine : List.of(sans, avec)) {
            engine.setMood(new Mood(Scene.BIG_BANGS, 2, 0));
            rendre(engine, 4);
        }
        avec.accent();
        assertTrue(rms(rendre(avec, 3)) > 1.1 * rms(rendre(sans, 3)), "un Big Bang fait monter la musique");
    }

    @Test
    void laMusiqueCoupeeNeDemarrePasEtReprendAvecUnFondu() throws InterruptedException {
        Music music = new Music();
        List<byte[]> blocks = Collections.synchronizedList(new ArrayList<>());
        music.sink = blocks::add;
        Mood mood = new Mood(Scene.ELAN, 1, 0);
        music.update(mood, false, 50);
        assertNull(music.player, "coupée : aucun fil, aucune sortie");
        music.update(mood, true, 0);
        assertNull(music.player, "au volume nul non plus");
        music.update(mood, true, 50);
        assertTrue(music.player != null && music.player.isAlive());
        while (blocks.size() < 20) Thread.sleep(5);
        music.update(mood, false, 50);
        music.player.join(5_000);
        assertTrue(!music.player.isAlive(), "coupée, elle s'arrête d'elle-même après le fondu");
        byte[] first = blocks.get(0), last = blocks.get(blocks.size() - 1);
        assertEquals(0, sample(first, 0), "le fondu part du silence");
        assertTrue(Math.abs(sample(last, last.length / 2 - 1)) < 64, "et y revient");
    }

    @Test
    void leGainGlisseSurUnBloc() {
        float[] block = new float[100];
        Arrays.fill(block, 0.5f);
        byte[] pcm = Music.pcm(block, 0, 1);
        assertEquals(0, sample(pcm, 0), "le premier échantillon au gain de départ");
        assertEquals(Math.round(0.5 * 0.99 * Short.MAX_VALUE), sample(pcm, 99), 2, "le dernier presque au gain d'arrivée");
    }

    /** L'échantillon {@code index} d'un bloc en octets 16 bits petit-boutistes. */
    private static short sample(byte[] pcm, int index) {
        return (short) ((pcm[2 * index] & 0xff) | pcm[2 * index + 1] << 8);
    }
}
