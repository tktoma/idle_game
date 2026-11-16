package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.collider.Hadron;
import idle.core.math.BigNum;
import idle.core.upgrades.Upgrades;
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
    void niLaParticuleNiLaFusionNeSonnentIci() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "PARTICLES", 0);
        // La toute première particule se forme en cinq secondes environ : elle sonne depuis son générateur, pas d'ici.
        game.tick(6);
        assertTrue(events.frame(game, "PARTICLES", 0).isEmpty(), "une particule créée sonne quand son onde atteint le noyau");
        game.tick(6);
        assertTrue(events.frame(game, "STATS", 0).isEmpty(), "ailleurs, rien");
        // De quoi acheter tous les générateurs, puis la fusion.
        for (int round = 0; round < 50 && !game.canFuse(); round++) {
            game.state().setParticles(BigNum.of(1, 30));
            game.buyAllWithParticles();
        }
        assertTrue(game.fuse(), "la fusion part");
        assertFalse(cues(events.frame(game, "ATOMS", 0)).contains(Cue.ATOM), "une fusion sonne au début de son animation, pas d'ici");
    }

    @Test
    void lesCollisionsLesHadronsEtLaGraineSonnentLaOuOnLesVoit() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "COLLIDER", 0);
        game.state().setCollisionRuns(game.state().collisionRuns() + 3);
        assertEquals(List.of(Cue.COLLISION), cues(events.frame(game, "COLLIDER", 0)));
        // Un croisement capté au milieu est réussi, même sans collision (avant l'amélioration « Collisions »).
        game.state().setColliderCaught(game.state().colliderCaught() + 1);
        assertEquals(List.of(Cue.COLLISION), cues(events.frame(game, "COLLIDER", 0)), "un croisement capté sonne");
        assertTrue(events.frame(game, "COLLIDER", 0).isEmpty(), "une fois");
        game.state().setHadrons(Hadron.PROTON, 5);
        assertTrue(events.frame(game, "COLLIDER", 0).isEmpty(), "des hadrons ne sonnent que sur leur onglet");
        game.state().setHadrons(Hadron.PROTON, 6);
        assertEquals(List.of(Cue.HADRON), cues(events.frame(game, "HADRONS", 0)));
        // Sans l'arrosoir tenu (le tuyau, un outil, un gain), la graine se tait.
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(game.state().seedWaterings() + 1);
        assertTrue(events.frame(game, "UNIVERSE", 0).isEmpty(), "le tuyau arrose en silence");
        // L'arrosoir tenu : la goutte sur l'onglet de l'univers, le palier partout.
        game.state().setWateringCan(true);
        game.setWatering(true);
        assertTrue(game.isWatering());
        game.state().setSeedWaterings(game.state().seedWaterings() + 1);
        assertEquals(List.of(Cue.WATER), cues(events.frame(game, "UNIVERSE", 0)), "une goutte sous l'arrosoir tenu");
        game.state().setSeedWaterings(Game.seedStageWaterings(2));
        List<Cue> stage = cues(events.frame(game, "STATS", 0));
        assertTrue(stage.contains(Cue.SEED), "un palier de la graine, arrosoir tenu, sonne partout : " + stage);
    }

    @Test
    void unPalierDeLaGraineSansLArrosoirTenuSeTait() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "UNIVERSE", 0);
        new TestActions(game).nextSeedStage();
        assertTrue(game.seedStage() > 0 || game.state().seedWaterings() > 0);
        assertTrue(events.frame(game, "UNIVERSE", 0).isEmpty(), "un outil d'essai fait passer un palier en silence");
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

    @Test
    void seulCeQuAmeneUnAutomatismeCompteCommeUnAutomatisme() {
        // Le quark top et les sondes qui reviennent sonnent : le joueur s'en sert (0.226 annulée en 0.232).
        assertFalse(Notifier.isAutomatic(new Notifier.News(Notice.COLLIDER, "Collision : un quark top ! À utiliser dans 4 min")));
        assertFalse(Notifier.isAutomatic(new Notifier.News(Notice.PROBES, "3 sondes revenues. Leur butin attend dans Big Bang, Sondes.")));
        assertFalse(Notifier.isAutomatic(new Notifier.News(Notice.COMETS, "Une comète traverse l'expansion : 30 secondes pour la saisir")),
                "une comète se saisit à la main : elle sonne");
        assertFalse(Notifier.isAutomatic(new Notifier.News(Notice.COLLIDER, "Palier de gluons : tout")), "un palier, rare, sonne");
        assertFalse(Notifier.isAutomatic(new Notifier.News(Notice.ACHIEVEMENTS, "Succès : Voie lactée")));
        assertTrue(Notifier.isAutomatic(new Notifier.News(Notice.AUTOMATION, "Automatisme offert : Couplage", true)));
    }

    /**
     * Un palier de vitesse atteint pendant que l'achat automatique de la vitesse est en marche vient de lui : il revient à
     * chaque partie, il a le son des automatismes. Coupé (ou tous les automatismes en pause), c'est le joueur : il sonne.
     */
    @Test
    void unPalierDeVitesseDeLAchatAutomatiqueCompteCommeUnAutomatisme() {
        for (int mode = 0; mode < 3; mode++) {
            Game game = new Game(1L);
            game.start();
            game.state().addAutomation("auto_speed");
            game.state().setAutomationEnabled("auto_speed", mode != 1);
            game.setAutomationPaused(mode == 2);
            Notifier notifier = new Notifier(game);
            notifier.poll();
            game.state().setLevel("speed", game.state().levelOf("speed") + Upgrades.SPEED_MILESTONE_EVERY);
            Notifier.News palier = notifier.poll().stream().filter(item -> item.text().startsWith("Palier de vitesse")).findFirst().orElseThrow();
            assertEquals(mode == 0, Notifier.isAutomatic(palier), "achat automatique " + (mode == 0 ? "en marche" : mode == 1 ? "coupé" : "en pause"));
        }
    }

    /** Une sonde qui part sonne, où qu'on soit. */
    @Test
    void uneSondeLanceeSonne() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "STATS", 0);
        game.state().setProbeLaunches(game.state().probeLaunches() + 1);
        assertEquals(List.of(Cue.PROBE), cues(events.frame(game, "STATS", 0)));
        assertTrue(events.frame(game, "STATS", 0).isEmpty(), "une fois");
    }

    /**
     * Le miroir, fenêtre ouverte : le motif qui s'allume joue ses notes de piano, chaque case reproduite la sienne (la
     * même que son reflet), une faute les notes graves ensemble ; fenêtre fermée, rien.
     */
    @Test
    void leMiroirJoueSonMotifPuisLesCasesReproduitesAuPiano() {
        Game game = partie();
        new TestActions(game).openDimension(9);
        List<Game.MirrorCell> pattern = List.of(new Game.MirrorCell(0, 0), new Game.MirrorCell(1, 2), new Game.MirrorCell(0, 3));
        for (Game.MirrorCell cell : pattern)
            assertEquals(SoundEvents.mirrorNote(game, cell), SoundEvents.mirrorNote(game, game.mirrored(cell)), "une case et son reflet : même note");
        assertTrue(SoundEvents.mirrorNote(game, pattern.get(0)) != SoundEvents.mirrorNote(game, pattern.get(1)));
        SoundEvents events = new SoundEvents();
        game.testSetMirrorPattern(pattern);
        events.frame(game, "UNIVERSE", 0);
        assertTrue(game.showMirrorPattern(), "entrer dans le miroir rallume le motif");
        List<SoundEvents.Heard> first = events.frame(game, "UNIVERSE", 9);
        assertEquals(List.of(new SoundEvents.Heard(Cue.MIRROR, 9, SoundEvents.mirrorNote(game, pattern.get(0)))), first, "la première case sonne");
        game.tick(game.mirrorShowSeconds());
        assertEquals(SoundEvents.mirrorNote(game, pattern.get(1)), events.frame(game, "UNIVERSE", 9).get(0).note(), "puis la deuxième");
        game.tick(game.mirrorShowSeconds() * 2);
        events.frame(game, "UNIVERSE", 9);
        assertEquals(Game.MirrorPhase.INPUT, game.mirrorPhase());
        Game.MirrorCell right = game.mirrored(pattern.get(0));
        game.clickMirror(right.column(), right.row());
        assertEquals(List.of(new SoundEvents.Heard(Cue.MIRROR, 9, SoundEvents.mirrorNote(game, pattern.get(0)))), events.frame(game, "UNIVERSE", 9),
                "la case reproduite rejoue la note du motif");
        Game.MirrorCell wrong = game.mirrored(pattern.get(2));
        game.clickMirror(wrong.column(), wrong.row());
        assertTrue(events.frame(game, "UNIVERSE", 9).contains(new SoundEvents.Heard(Cue.MIRROR, 9, SoundEvents.WRONG)), "une faute");
        game.testSetMirrorPattern(pattern);
        game.showMirrorPattern();
        assertTrue(events.frame(game, "UNIVERSE", 0).stream().noneMatch(h -> h.cue() == Cue.MIRROR), "fenêtre fermée : rien");
    }

    /**
     * Le mélange sonne à chaque éclat ramassé, même quand le total des fragments est trop grand pour bouger d'un gain
     * minuscule (les étapes avancées, où l'onde de la brane ramasse sous un gros malus).
     */
    @Test
    void leMelangeSonneAChaqueEclatRamasse() {
        Game game = partie();
        new TestActions(game).openDimension(10);
        game.state().setMixStage(6);       // l'étape de la brane : le point ramasse, l'onde aussi
        game.state().setMixShardsEarned(1e300);
        SoundEvents events = new SoundEvents();
        events.frame(game, "UNIVERSE", 10);
        events.frame(game, "UNIVERSE", 10);
        game.testPlaceMixCursor(0.5, 0.5, 0);
        game.testAddMixShard(0.5, 0.5, 0, 0);
        long before = game.mixPickups();
        game.tick(0.05);
        assertTrue(game.mixPickups() > before, "l'éclat sous le point est ramassé");
        assertEquals(1e300, game.state().mixShardsEarned(), 0, "le total ne bouge pas d'un gain minuscule");
        assertTrue(events.frame(game, "UNIVERSE", 10).contains(new SoundEvents.Heard(null, 10)), "le ramassage sonne quand même");
    }
}
