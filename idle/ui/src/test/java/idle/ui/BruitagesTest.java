package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.collider.Hadron;
import idle.core.math.BigNum;
import idle.ui.Sound.Cue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Les bruitages de la partie. Ce qui vient de se passer sonne, là où on le voit ; la première image ne
 * sonne rien ; une catégorie coupée dans les réglages ne sonne pas. Sans affichage ni carte son.
 */
class BruitagesTest {
    private static Game partie() {
        Game game = new Game(42L);
        game.start();
        return game;
    }

    private static List<Cue> cues(List<SoundEvents.Heard> heard) {
        return heard.stream().map(SoundEvents.Heard::cue).toList();
    }

    @Test
    void laPremiereImageNeSonneRien() {
        Game game = partie();
        game.state().setCollisionRuns(12);
        game.state().setSeedWaterings(40);
        assertTrue(new SoundEvents().frame(game, "COLLIDER", 0).isEmpty(), "une partie chargée ne sonne pas ce qu'elle a déjà fait");
    }

    @Test
    void uneParticuleEtUneFusionSonnentSurLeurOnglet() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "PARTICLES", 0);
        // La toute première particule se forme en cinq secondes environ.
        game.tick(6);
        assertEquals(List.of(Cue.PARTICLE), cues(events.frame(game, "PARTICLES", 0)), "une particule créée, sur l'onglet des particules");
        game.tick(6);
        assertTrue(events.frame(game, "STATS", 0).isEmpty(), "ailleurs, rien");
        // De quoi acheter tous les générateurs, puis la fusion.
        for (int round = 0; round < 50 && !game.canFuse(); round++) {
            game.state().setParticles(BigNum.of(1, 30));
            game.buyAllWithParticles();
        }
        assertTrue(game.fuse(), "la fusion part");
        assertTrue(cues(events.frame(game, "ATOMS", 0)).contains(Cue.ATOM), "une fusion, sur l'onglet des atomes");
    }

    @Test
    void lesCollisionsLesHadronsEtLaGraineSonnentLaOuOnLesVoit() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "COLLIDER", 0);
        game.state().setCollisionRuns(game.state().collisionRuns() + 3);
        assertEquals(List.of(Cue.COLLISION), cues(events.frame(game, "COLLIDER", 0)));
        game.state().setHadrons(Hadron.PROTON, 5);
        assertTrue(events.frame(game, "COLLIDER", 0).isEmpty(), "des hadrons ne sonnent que sur leur onglet");
        game.state().setHadrons(Hadron.PROTON, 6);
        assertEquals(List.of(Cue.HADRON), cues(events.frame(game, "HADRONS", 0)));
        game.state().setSeedWaterings(game.state().seedWaterings() + 1);
        assertEquals(List.of(Cue.WATER), cues(events.frame(game, "UNIVERSE", 0)), "un arrosage, sur l'onglet de l'univers");
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(2));
        List<Cue> stage = cues(events.frame(game, "STATS", 0));
        assertTrue(stage.contains(Cue.SEED), "un palier de la graine sonne partout : " + stage);
    }

    @Test
    void unPointGagneSonneDansLaFenetreDeSaDimension() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "UNIVERSE", 0);
        game.state().setClickerShardsEarned(50);
        assertTrue(events.frame(game, "UNIVERSE", 7).isEmpty(), "la fenêtre qui s'ouvre relève d'abord son compte");
        game.state().setClickerShardsEarned(51);
        List<SoundEvents.Heard> heard = events.frame(game, "UNIVERSE", 7);
        assertEquals(1, heard.size());
        assertEquals(7, heard.get(0).dimension());
        game.state().setCollisionRuns(game.state().collisionRuns() + 1);
        assertTrue(events.frame(game, "COLLIDER", 7).isEmpty(), "la fenêtre d'une dimension couvre les onglets");
    }

    @Test
    void chaqueSonAppartientAUneCategorieQuOnPeutCouper() throws InterruptedException {
        for (Cue cue : Cue.values()) {
            if (cue != Cue.BEAT) assertNotNull(cue.kind(), cue + " : une catégorie");
        }
        List<byte[]> sent = new ArrayList<>();
        Sound sound = new Sound();
        sound.sink = pcm -> sent.add(pcm.bytes());
        sound.setAllowed(kind -> kind != SoundKind.NEWS && kind != SoundKind.DIMENSIONS && kind != SoundKind.CLICKS);
        assertFalse(sound.play(Cue.VICTORY, 100), "une annonce coupée");
        assertFalse(sound.gain(3, 100), "un point gagné coupé");
        assertFalse(sound.gesture(3, 100), "un geste coupé");
        assertFalse(sound.click(Cue.CLICK, 100), "un clic coupé");
        assertTrue(sound.play(Cue.BIG_BANG, 100), "le reste sonne");
        assertTrue(sound.click(Cue.TAB, 100));
        assertTrue(sound.beat(100), "le métronome a son propre réglage");
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(3, sent.size());
    }

    @Test
    void lesBruitagesSontCourtsDouxEtPropres() {
        List<short[]> gains = new ArrayList<>();
        for (int dimension = 1; dimension <= 10; dimension++) {
            short[] gain = Sound.synthesizeGain(dimension);
            for (short[] other : gains) assertFalse(Arrays.equals(other, gain), "chaque dimension a sa note : " + dimension);
            gains.add(gain);
        }
        for (Cue cue : List.of(Cue.PARTICLE, Cue.ATOM, Cue.COLLISION, Cue.HADRON, Cue.VOID, Cue.SEED, Cue.WATER, Cue.CLICK, Cue.TAB)) {
            short[] samples = Sound.synthesize(cue);
            assertTrue(samples.length > 0 && samples.length <= 44_100, cue + " : court");
            assertTrue(cue.isInterface(), cue + " : hors du rythme des annonces");
            assertTrue(cue.gap() >= Sound.Cue.CLICK.gap(), cue + " : un écart");
        }
        assertTrue(Cue.PARTICLE.gap() > Cue.CLICK.gap(), "ce qui revient sans cesse attend plus qu'un clic");
    }
}
