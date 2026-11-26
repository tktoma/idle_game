package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        // Un arrosage ne sonne pas d'un coup : l'eau de l'arrosoir et du tuyau est un fond en boucle.
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(game.state().seedWaterings() + 1);
        assertTrue(events.frame(game, "UNIVERSE", 0).isEmpty(), "un arrosage : pas de coup");
        // Un palier : il sonne partout, une fois.
        game.state().setSeedWaterings(Game.seedStageWaterings(2));
        List<Cue> stage = cues(events.frame(game, "STATS", 0));
        assertEquals(List.of(Cue.SEED), stage, "un palier de la graine sonne partout");
        assertTrue(events.frame(game, "STATS", 0).isEmpty(), "une fois");
    }

    /**
     * Version 0.256 : un palier sonne d'où qu'il vienne, même sans l'arrosoir tenu ; avant, il ne sonnait presque jamais
     * (la graine au plafond passe ses paliers quand un univers se forme, pas sous l'arrosoir).
     */
    @Test
    void unPalierDeLaGraineSonneDOuQuIlVienne() {
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "UNIVERSE", 0);
        assertFalse(game.isWatering());
        int before = game.seedStage();
        new TestActions(game).nextSeedStage();
        assertTrue(game.seedStage() > before);
        assertEquals(List.of(Cue.SEED), cues(events.frame(game, "UNIVERSE", 0)), "l'arrosoir reposé : il sonne quand même");
        // La graine au plafond garde ses arrosages ; le plafond qui monte (un univers formé) la fait passer, et ça sonne.
        int capped = game.seedStage();
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        events.frame(game, "UNIVERSE", 0);
        if (game.seedStage() < Game.SEED_STAGES && game.seedStage() == game.seedStageCap()) {
            int stageAtCap = game.seedStage();
            game.testAddUniverse();
            if (game.seedStage() > stageAtCap) assertTrue(cues(events.frame(game, "UNIVERSE", 0)).contains(Cue.SEED), "le plafond qui monte : ça sonne");
        }
        assertTrue(capped >= before);
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
        for (Cue cue : List.of(Cue.PARTICLE, Cue.ATOM, Cue.COLLISION, Cue.HADRON, Cue.VOID, Cue.SEED, Cue.BLOOM, Cue.SEASON, Cue.CLICK, Cue.TAB)) {
            short[] samples = Sound.synthesize(cue);
            assertTrue(samples.length > 0 && samples.length <= 44_100, cue + " : court");
            assertTrue(cue.isInterface(), cue + " : hors des fournées d'annonces");
        }
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
        assertEquals(3.0, Sound.PROBE_SOUND.length / 44_100.0, 0.01, "le décollage grondant, choisi à l'écoute");
        NiveauxTest.auNiveau(Sound.PROBE_SOUND, 1, Sound.RARE_DB, "le décollage de la sonde");
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

    /** Le rond de croissance cueilli sonne ; une saison qui commence sonne sur l'onglet de l'univers, avec sa note. */
    @Test
    void leRondCueilliEtLesSaisonsSonnent() {
        Game game = partie();
        new TestActions(game).openDimension(9);
        SoundEvents events = new SoundEvents();
        game.testSetSeason(Game.Season.SPRING);
        events.frame(game, "UNIVERSE", 0);
        assertTrue(events.frame(game, "UNIVERSE", 0).isEmpty());
        game.testSetSeason(Game.Season.AUTUMN);
        assertEquals(List.of(new SoundEvents.Heard(Cue.SEASON, 0, Game.Season.AUTUMN.ordinal())), events.frame(game, "UNIVERSE", 0),
                "l'automne commence");
        game.testSetSeason(Game.Season.WINTER);
        assertTrue(events.frame(game, "STATS", 0).isEmpty(), "ailleurs que sur l'onglet de l'univers : rien");
        game.testShowBloom();
        assertTrue(game.catchBloom());
        assertEquals(List.of(Cue.BLOOM), cues(events.frame(game, "STATS", 0)), "le rond cueilli");
    }

    /** Version 0.247 : les paires du vide ne sonnent que sur le sous-onglet « Paires », pas sur les améliorations. */
    @Test
    void lesPairesDuVideNeSonnentQueSurPaires() {
        assertEquals("VOID", SoundEvents.heardTab("VOID", true, false));
        assertNotEquals("VOID", SoundEvents.heardTab("VOID", false, true), "les améliorations du vide sont muettes");
        assertEquals("HADRONS", SoundEvents.heardTab("HADRONS", false, false), "les autres onglets ne changent pas");
        assertEquals("COLLIDER", SoundEvents.heardTab("COLLIDER", false, true));
    }

    /** Version 0.272 : les collisions ne sonnent que sur le sous-onglet « Énergie » du collisionneur. */
    @Test
    void lesCollisionsNeSonnentQueSurEnergie() {
        assertNotEquals("COLLIDER", SoundEvents.heardTab("COLLIDER", true, false), "la chaîne et les améliorations sont muettes");
        Game game = partie();
        SoundEvents events = new SoundEvents();
        events.frame(game, "COLLIDER", 0);
        game.state().setCollisionRuns(game.state().collisionRuns() + 1);
        assertTrue(events.frame(game, SoundEvents.heardTab("COLLIDER", false, false), 0).isEmpty(), "hors d'Énergie : rien");
        game.state().setCollisionRuns(game.state().collisionRuns() + 1);
        assertEquals(List.of(Cue.COLLISION), cues(events.frame(game, SoundEvents.heardTab("COLLIDER", false, true), 0)), "sur Énergie : la collision");
    }

    /** Version 0.264 : le palier de vitesse prend le son du joueur ; l'annonce de collision, à part, garde le petit bip. */
    @Test
    void lePalierDeVitesseALeSonDuJoueur() {
        assertEquals(Cue.LEVEL, Sound.cueOf("Palier de vitesse : générateur 3"));
        assertEquals(Cue.COLLISION_NEWS, Sound.cueOf("Collision : 12 hadrons"));
        assertNull(Sound.cueOf("Collision : un quark top ! À utiliser dans 4 min"), "le quark top est muet (0.279)");
        assertTrue(Sound.silent("Collision : un quark top ! À utiliser dans 4 min"));
        assertNull(Sound.orNotice(null, List.of("Collision : un quark top ! À utiliser dans 4 min")), "ni le son des autres annonces");
        assertTrue(Sound.SPEED_SOUND.length > 0, "palier-vitesse.wav se charge");
        assertEquals(0.18, Sound.SPEED_SOUND.length / 44_100.0, 0.01, "0,18 s");
        NiveauxTest.auNiveau(Sound.SPEED_SOUND, 1, Sound.RARE_DB, "le palier de vitesse");
        assertEquals(Cue.LEVEL, Sound.strongest(java.util.List.of("Collision : 3 hadrons", "Palier de vitesse : générateur 1")),
                "le palier passe devant l'annonce de collision");
        assertNotEquals(Sound.leveledSound(Cue.COLLISION_NEWS).length, Sound.SPEED_SOUND.length, "la collision garde le bip");
    }

    /**
     * Version 0.265 : un univers formé prend l'enregistrement du joueur, en stéréo, au niveau des rares ; la fin de la
     * partie, à part depuis la 0.266, a le sien depuis la 0.267 (fin.wav).
     */
    @Test
    void lUniversFormeALeSonDuJoueur() {
        assertTrue(Sound.UNIVERSE_STEREO.length > 0, "univers.wav se charge");
        assertEquals(5.22, Sound.UNIVERSE_STEREO.length / 2 / 44_100.0, 0.05, "5,2 s en stéréo");
        NiveauxTest.auNiveau(Sound.UNIVERSE_STEREO, 2, Sound.RARE_DB, "l'univers formé");
        assertEquals(Cue.UNIVERSE, Sound.cueOf("Univers formé : ×2"));
        assertEquals(Cue.GAME_END, Sound.cueOf("Les dix paliers sont atteints : la partie est finie. Merci d'avoir joué !"));
        assertEquals(2.4, Sound.leveledSound(Cue.GAME_END).length / 44_100.0, 0.01, "l'accord de 2,4 s reste en secours");
        assertTrue(Sound.GAME_END_STEREO.length > 0, "fin.wav se charge (0.267)");
        assertEquals(9.62, Sound.GAME_END_STEREO.length / 2 / 44_100.0, 0.05, "9,6 s en stéréo");
        NiveauxTest.auNiveau(Sound.GAME_END_STEREO, 2, Sound.RARE_DB, "la fin de la partie");
    }

    /**
     * Version 0.303 : au lancement, la sortie audio est réveillée par un dixième de seconde de silence, avant tout son ;
     * il ne prend la place d'aucun son, et les six particules qui suivent partent toutes.
     */
    @Test
    void laSortieEstReveilleeAvantLaPremiereParticule() throws InterruptedException {
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        sound.warmUp();
        for (int k = 0; k < Sound.MAX_PARTICLE_VOICES; k++) assertTrue(sound.particle(0, 100), "particule " + k);
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1 + Sound.MAX_PARTICLE_VOICES, sent.size());
        Sound.Pcm silence = sent.get(0);
        assertEquals(Sound.WARM_UP_SAMPLES * 2, silence.bytes().length, "d'abord le silence");
        assertFalse(silence.stereo());
        for (byte b : silence.bytes()) assertEquals(0, b, "muet");
        assertEquals(Sound.PARTICLE_STAGES[0].length * 2, sent.get(1).bytes().length, "puis la première particule");
        assertEquals(0, sound.lost(), "rien de perdu");
        assertTrue(Sound.RETRY_MILLIS > 0 && Sound.RETRY_MILLIS < 100, "un nouvel essai assez tôt pour rester à l'heure");
    }

    /** Version 0.271 : un palier du défi final prend le son du joueur, en mono, au niveau des rares. */
    @Test
    void lePalierDuDefiFinalALeSonDuJoueur() throws InterruptedException {
        assertTrue(Sound.FINALE_TIER_SOUND.length > 0, "defi-palier.wav se charge");
        assertEquals(1.69, Sound.FINALE_TIER_SOUND.length / 44_100.0, 0.02, "1,7 s");
        NiveauxTest.auNiveau(Sound.FINALE_TIER_SOUND, 1, Sound.RARE_DB, "le palier du défi final");
        assertEquals(Cue.FINALE, Sound.cueOf("Défi final : la ligne atteint son palier"));
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        // Les annonces partent par play() : l'enregistrement mono doit en partir (le palier de vitesse ne le faisait pas
        // avant la 0.271).
        assertTrue(sound.play(Cue.FINALE, 100));
        assertTrue(sound.play(Cue.LEVEL, 100));
        assertTrue(sound.play(Cue.COLLISION_NEWS, 100));
        assertTrue(sound.play(Cue.VICTORY, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.FINALE_TIER_SOUND.length * 2, sent.get(0).bytes().length, "le palier du défi final : le son du joueur");
        assertFalse(sent.get(0).stereo());
        assertEquals(Sound.SPEED_SOUND.length * 2, sent.get(1).bytes().length, "le palier de vitesse : le son du joueur");
        assertEquals(Sound.leveledSound(Cue.COLLISION_NEWS).length * 2, sent.get(2).bytes().length, "la collision garde son bip");
        assertEquals(Sound.VICTORY_SOUND.length * 2, sent.get(3).bytes().length, "le défi réussi : le son du joueur (0.271)");
        assertEquals(0.485, Sound.VICTORY_SOUND.length / 44_100.0, 0.01);
        NiveauxTest.auNiveau(Sound.VICTORY_SOUND, 1, Sound.RARE_DB, "le défi réussi");
        assertEquals(Cue.VICTORY, Sound.cueOf("Défi réussi : Rouille"));
        assertEquals(Cue.VICTORY, Sound.cueOf("Défi de Big Bang « Vide » réussi"));
        assertEquals(Cue.CHALLENGE, Sound.cueOf("Nouveau défi : Rouille"));
        assertEquals(0.188, Sound.CHALLENGE_SOUND.length / 44_100.0, 0.01, "le défi qui s'ouvre : 0,19 s");
        NiveauxTest.auNiveau(Sound.CHALLENGE_SOUND, 1, Sound.targetDb(Cue.CHALLENGE), "le défi qui s'ouvre");
        List<Sound.Pcm> opened = Collections.synchronizedList(new ArrayList<>());
        Sound other = new Sound();
        other.sink = opened::add;
        assertTrue(other.play(Cue.CHALLENGE, 100));
        other.player.shutdown();
        assertTrue(other.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(Sound.CHALLENGE_SOUND.length * 2, opened.get(0).bytes().length, "c'est lui qui part");
    }

    /** Version 0.273 : un nouvel élément prend le son du joueur, en stéréo ; l'élément unique et l'astre mûr aussi. */
    @Test
    void leNouvelElementALeSonDuJoueur() throws InterruptedException {
        assertTrue(Sound.ELEMENT_STEREO.length > 0, "nouvel-element.wav se charge");
        assertEquals(1.46, Sound.ELEMENT_STEREO.length / 2 / 44_100.0, 0.02, "1,46 s en stéréo");
        NiveauxTest.auNiveau(Sound.ELEMENT_STEREO, 2, Sound.RARE_DB - 12, "le nouvel élément, 12 dB sous les sons rares (0.279)");
        assertEquals(Sound.RARE_DB - 12, Sound.targetDb(Cue.ELEMENT), 1e-9);
        assertEquals(Cue.ELEMENT, Sound.cueOf("Nouvel élément : Hélium"));
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.play(Cue.ELEMENT, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(sent.get(0).stereo());
        assertEquals(Sound.ELEMENT_STEREO.length * 2, sent.get(0).bytes().length, "c'est lui qui part");
    }

    /** Version 0.277 : un jalon prend le son du joueur, en stéréo, au niveau des rares ; toutes les annonces de jalon. */
    @Test
    void leJalonALeSonDuJoueur() throws InterruptedException {
        assertTrue(Sound.MILESTONE_STEREO.length > 0, "jalon.wav se charge");
        assertEquals(0.224, Sound.MILESTONE_STEREO.length / 2 / 44_100.0, 0.01, "0,22 s en stéréo");
        NiveauxTest.auNiveau(Sound.MILESTONE_STEREO, 2, Sound.RARE_DB, "le jalon");
        assertEquals(Cue.MILESTONE, Sound.cueOf("Jalon : 1e6 particules"));
        assertEquals(Cue.MILESTONE, Sound.cueOf("Premier atome : l'hydrogène"));
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.play(Cue.MILESTONE, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(sent.get(0).stereo());
        assertEquals(Sound.MILESTONE_STEREO.length * 2, sent.get(0).bytes().length, "c'est lui qui part");
    }

    /** Version 0.277 : un skin débloqué et un succès prennent les sons du joueur, au niveau des rares. */
    @Test
    void leSkinEtLeSuccesOntLesSonsDuJoueur() throws InterruptedException {
        assertEquals(0.147, Sound.SKIN_STEREO.length / 2 / 44_100.0, 0.01, "le skin : 0,15 s en stéréo");
        assertEquals(0.136, Sound.ACHIEVEMENT_SOUND.length / 44_100.0, 0.01, "le succès : 0,14 s");
        NiveauxTest.auNiveau(Sound.SKIN_STEREO, 2, Sound.RARE_DB, "le skin");
        NiveauxTest.auNiveau(Sound.ACHIEVEMENT_SOUND, 1, Sound.RARE_DB - 6, "le succès, 6 dB sous les sons rares (0.279)");
        assertEquals(Cue.SKIN, Sound.cueOf("Skin débloqué : Braise"));
        assertEquals(Cue.ACHIEVEMENT, Sound.cueOf("Succès : Premier pas"));
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.play(Cue.SKIN, 100));
        assertTrue(sound.play(Cue.ACHIEVEMENT, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(sent.get(0).stereo());
        assertEquals(Sound.SKIN_STEREO.length * 2, sent.get(0).bytes().length, "le skin : c'est lui qui part");
        assertEquals(Sound.ACHIEVEMENT_SOUND.length * 2, sent.get(1).bytes().length, "le succès : c'est lui qui part");
    }

    /**
     * Version 0.278 : les hadrons prennent le son du joueur (deux au plus à la fois) ; une annonce sans son à elle fait le
     * son des autres annonces ; les récompenses d'une sonde ouvertes d'un coup font le son d'achat une fois par carte.
     */
    @Test
    void hadronsAutresAnnoncesEtRecompensesDesSondes() throws InterruptedException {
        assertEquals(2.106, Sound.HADRON_STEREO.length / 2 / 44_100.0, 0.02, "les hadrons : 2,1 s en stéréo");
        NiveauxTest.auNiveau(Sound.HADRON_STEREO, 2, Sound.REPEATED_DB, "les hadrons");
        assertEquals(0.172, Sound.NOTICE_STEREO.length / 2 / 44_100.0, 0.01, "l'autre annonce : 0,17 s en stéréo");
        NiveauxTest.auNiveau(Sound.NOTICE_STEREO, 2, Sound.RARE_DB - 6, "l'autre annonce");
        List<Sound.Pcm> sent = Collections.synchronizedList(new ArrayList<>());
        Sound sound = new Sound();
        sound.sink = sent::add;
        assertTrue(sound.play(Cue.HADRON, 100));
        assertTrue(sound.play(Cue.HADRON, 100));
        assertFalse(sound.play(Cue.HADRON, 100), "deux hadrons à la fois, pas trois");
        assertTrue(sound.play(Cue.NOTICE, 100));
        assertTrue(sound.purchases(3, 100));
        sound.player.shutdown();
        assertTrue(sound.player.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(4, sent.size());
        assertEquals(Sound.HADRON_STEREO.length * 2, sent.get(0).bytes().length, "les hadrons : le son du joueur");
        assertEquals(Sound.NOTICE_STEREO.length * 2, sent.get(2).bytes().length, "l'autre annonce : le son du joueur");
        assertEquals(Sound.purchaseRun(3).length * 2, sent.get(3).bytes().length, "trois cartes : trois achats à la suite");
        // La suite d'achats : une toutes les 70 ms, douze au plus.
        int gap = (int) (Sound.PURCHASE_RUN_GAP * 44_100);
        assertEquals(Sound.PURCHASE_SOUND.length + 2 * gap, Sound.purchaseRun(3).length);
        assertEquals(Sound.purchaseRun(12).length, Sound.purchaseRun(40).length, "douze au plus");
        // Le son des autres annonces : seulement si rien d'autre ne sonne, et s'il y a une annonce sans son.
        assertEquals(Cue.NOTICE, Sound.orNotice(null, List.of("Une annonce sans son à elle")));
        assertEquals(Cue.SKIN, Sound.orNotice(Cue.SKIN, List.of("Skin débloqué : Braise", "Une annonce sans son à elle")));
        assertNull(Sound.orNotice(null, List.of()));
        assertNull(Sound.orNotice(null, List.of("Skin débloqué : Braise")), "une annonce qui a son son ne fait pas le son des autres");
    }
}
