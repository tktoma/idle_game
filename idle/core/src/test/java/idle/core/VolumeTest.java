package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Version 0.128 : le volume, troisième dimension. On plonge dans le cube, on regarde depuis son centre, on cueille
 * les boules qui y apparaissent ; le cube rejette le joueur à la fin du minuteur, et les améliorations s'achètent
 * dehors.
 */
class VolumeTest {

    /** Une partie au troisième univers : la troisième dimension est ouverte. */
    private static Game third() {
        Game game = new Game();
        game.start();
        DeuxiemeUniversTest.formUniverse(game);
        assertTrue(game.testAddUniverse());
        assertTrue(game.testAddUniverse());
        assertEquals(3, game.universes());
        return game;
    }

    private static Game second() {
        Game game = new Game();
        game.start();
        DeuxiemeUniversTest.formUniverse(game);
        assertTrue(game.testAddUniverse());
        return game;
    }

    @Test
    void leVolumeSOuvreAuTroisiemeUnivers() {
        Game game = second();
        assertFalse(game.isVolumeOpen(), "deux univers : la troisième dimension est fermée");
        assertFalse(game.enterVolume(), "on ne plonge pas dans une dimension fermée");
        assertFalse(game.isInVolume());
        assertTrue(game.testAddUniverse());
        assertTrue(game.isVolumeOpen());
        assertTrue(game.enterVolume());
        assertTrue(game.isInVolume());
        assertFalse(game.enterVolume(), "déjà dans le cube");
    }

    @Test
    void lePremierPlongeonDureDouzeSecondesPuisLeCubeRejetteLeJoueur() {
        Game game = third();
        assertEquals(0, game.volumeTimeLeft(), "hors du cube, pas de minuteur");
        assertTrue(game.enterVolume());
        assertEquals(Game.VOLUME_DIVE_SECONDS, game.volumeTimeLeft(), 1e-12);
        assertEquals(12, game.volumeTimeLeft(), 1e-12, "court au début");
        game.tick(5);
        assertTrue(game.isInVolume());
        assertEquals(7, game.volumeTimeLeft(), 1e-9);
        game.tick(7.5);
        assertFalse(game.isInVolume(), "le minuteur à zéro : dehors");
        assertEquals(0, game.volumeTimeLeft());
        assertTrue(game.volumeBalls().isEmpty(), "les boules s'en vont avec la plongée");
    }

    @Test
    void lesBoulesApparaissentDansLeCubeSansDepasserLeMaximum() {
        Game game = third();
        game.tick(60);
        assertTrue(game.volumeBalls().isEmpty(), "hors du cube, rien n'apparaît");
        assertTrue(game.enterVolume());
        assertEquals(1, game.volumeBalls().size(), "la première boule apparaît aussitôt");
        game.state().setVolumeLevel(Game.VolumeUpgrade.TIME.ordinal(), 20);
        game.leaveVolume();
        assertTrue(game.enterVolume());
        for (int k = 0; k < 40; k++) game.tick(0.5);
        assertEquals(Game.VOLUME_MAX_BALLS, game.volumeBalls().size(), "quatre au plus");
        double limit = Game.VOLUME_EDGE - game.volumeBallRadius();
        for (Game.VolumeBall ball : game.volumeBalls()) {
            double d = Math.sqrt(ball.x() * ball.x() + ball.y() * ball.y() + ball.z() * ball.z());
            assertTrue(d >= Game.VOLUME_SPAWN_NEAR - 1e-12 && d <= Game.VOLUME_SPAWN_FAR + 1e-12, "ni sur le joueur, ni dans un coin");
            for (double c : new double[] {ball.x(), ball.y(), ball.z()}) assertTrue(Math.abs(c) <= limit + 1e-12, "dans le cube");
        }
    }

    @Test
    void lesBoulesNaissentAuRythmeDeLaReapparition() {
        Game game = third();
        game.state().setVolumeLevel(Game.VolumeUpgrade.COUNT.ordinal(), Game.VolumeUpgrade.COUNT.max());
        assertTrue(game.enterVolume());
        // 1 aussitôt, puis une toutes les 1,5 s : 1 + 6 en 9,05 s.
        for (int k = 0; k < 91; k++) game.tick(0.1);
        assertEquals(7, game.volumeBalls().size());
    }

    @Test
    void uneLongueImageNeRemplitPasUnCubeDejaFini() {
        Game game = third();
        game.state().setVolumeLevel(Game.VolumeUpgrade.COUNT.ordinal(), Game.VolumeUpgrade.COUNT.max());
        assertTrue(game.enterVolume());
        game.tick(3600);
        assertFalse(game.isInVolume());
        assertEquals(0, game.volumeCollected());
    }

    @Test
    void cueillirUneBouleDonneUnEclatDuVolumeEtLaRetire() {
        Game game = third();
        assertTrue(game.enterVolume());
        game.testClearVolume();
        game.addVolumeBall(0, 0, 0.6);
        game.addVolumeBall(0.6, 0, 0);
        double line = game.state().dimensionShards(), plane = game.state().planeShards();
        Game.VolumeBall hit = game.shootVolume(0, 0, 1);
        assertNotNull(hit);
        assertEquals(0.6, hit.z(), 1e-12);
        assertEquals(1, game.state().volumeShards(), 1e-12);
        assertEquals(1, game.state().volumeShardsEarned(), 1e-12);
        assertEquals(1, game.volumeBalls().size());
        assertEquals(1, game.volumeCollected());
        assertEquals(line, game.state().dimensionShards(), 1e-12, "les éclats de la ligne ne bougent pas");
        assertEquals(plane, game.state().planeShards(), 1e-12, "ni ceux du plan");
        // La même direction, une seconde fois : il n'y a plus rien.
        assertNull(game.shootVolume(0, 0, 1));
        assertEquals(1, game.state().volumeShards(), 1e-12);
    }

    @Test
    void leRegardPrendLaBouleLaPlusProcheEtPasCelleDerriereLui() {
        Game game = third();
        assertTrue(game.enterVolume());
        game.testClearVolume();
        game.addVolumeBall(0, 0, 0.8);
        game.addVolumeBall(0, 0, 0.4);
        game.addVolumeBall(0, 0, -0.5);
        assertEquals(0.4, game.shootVolume(0, 0, 2).z(), 1e-12, "la plus proche d'abord, quelle que soit la longueur de la direction");
        assertEquals(0.8, game.shootVolume(0, 0, 1).z(), 1e-12);
        assertEquals(-0.5, game.shootVolume(0, 0, -1).z(), 1e-12, "derrière : il faut se retourner");
    }

    @Test
    void unRegardAcoteNeCueilleRienEtLaTailleAgranditLaCible() {
        Game game = third();
        assertTrue(game.enterVolume());
        game.testClearVolume();
        game.addVolumeBall(0, 0, 1);
        // À la distance 1, une boule de rayon 0,025 : un regard décalé de 0,04 la manque.
        assertEquals(0.025, Game.VOLUME_BALL_RADIUS, 1e-12, "petite au départ");
        assertNull(game.shootVolume(0.04, 0, 1));
        game.leaveVolume();
        game.state().setVolumeShards(1e6);
        for (int k = 0; k < 4; k++) assertTrue(game.buyVolume(Game.VolumeUpgrade.SIZE));
        assertEquals(Game.VOLUME_BALL_RADIUS * 1.8, game.volumeBallRadius(), 1e-12);
        assertTrue(game.enterVolume());
        game.testClearVolume();
        game.addVolumeBall(0, 0, 1);
        assertNotNull(game.shootVolume(0.04, 0, 1), "plus grosse, elle se touche de plus loin");
    }

    @Test
    void horsDuCubeOuSansDirectionRienNeSeCueille() {
        Game game = third();
        assertNull(game.shootVolume(0, 0, 1), "hors du cube");
        assertTrue(game.enterVolume());
        game.testClearVolume();
        game.addVolumeBall(0, 0, 0.5);
        assertNull(game.shootVolume(0, 0, 0), "direction nulle");
        assertNull(game.shootVolume(Double.NaN, 0, 1), "direction impossible");
        assertEquals(0, game.state().volumeShards(), 1e-12);
    }

    @Test
    void lesAmeliorationsSAchetentDehorsEtPasPendantLaPlongee() {
        Game game = third();
        game.state().setVolumeShards(1e6);
        assertTrue(game.enterVolume());
        for (Game.VolumeUpgrade upgrade : Game.VolumeUpgrade.values()) {
            assertFalse(game.canBuyVolume(upgrade), upgrade + " : pas dans le cube");
            assertFalse(game.buyVolume(upgrade));
        }
        game.leaveVolume();
        for (Game.VolumeUpgrade upgrade : Game.VolumeUpgrade.values()) assertTrue(game.buyVolume(upgrade), upgrade.name());
        assertEquals(Game.VOLUME_DIVE_SECONDS + Game.VOLUME_DIVE_STEP, game.volumeDiveSeconds(), 1e-12);
        assertEquals(2, game.volumeValue(), 1e-12);
        assertEquals(Game.VOLUME_SPAWN_SECONDS * 0.85, game.volumeSpawnSeconds(), 1e-12);
        assertEquals(Game.VOLUME_MAX_BALLS + 1, game.volumeMaxBalls());
        assertEquals(Game.VOLUME_BALL_RADIUS * (1 + Game.VOLUME_SIZE_STEP), game.volumeBallRadius(), 1e-12);
    }

    @Test
    void lesPrixMontentEtLesPlafondsTiennent() {
        Game game = third();
        assertEquals(3, game.volumeCost(Game.VolumeUpgrade.TIME), 1e-12);
        assertEquals(5, game.volumeCost(Game.VolumeUpgrade.VALUE), 1e-12);
        game.state().setVolumeShards(2.9);
        assertFalse(game.canBuyVolume(Game.VolumeUpgrade.TIME), "trop cher");
        game.state().setVolumeShards(3);
        assertTrue(game.buyVolume(Game.VolumeUpgrade.TIME));
        assertEquals(0, game.state().volumeShards(), 1e-12);
        assertEquals(3 * 2.2, game.volumeCost(Game.VolumeUpgrade.TIME), 1e-12);
        game.state().setVolumeShards(1e30);
        game.state().setVolumeLevel(Game.VolumeUpgrade.TIME.ordinal(), Game.VolumeUpgrade.TIME.max());
        assertFalse(game.canBuyVolume(Game.VolumeUpgrade.TIME), "vingt niveaux de durée au plus");
        assertEquals(Double.POSITIVE_INFINITY, game.volumeCost(Game.VolumeUpgrade.TIME));
        assertEquals(72, game.volumeDiveSeconds(), 1e-12, "12 s + 20 × 3 s");
        game.state().setVolumeLevel(Game.VolumeUpgrade.COUNT.ordinal(), Game.VolumeUpgrade.COUNT.max());
        assertEquals(20, game.volumeMaxBalls(), "vingt boules au plus");
        assertTrue(game.canBuyVolume(Game.VolumeUpgrade.VALUE), "la valeur n'a pas de plafond");
    }

    @Test
    void uneAbsenceMetFinALaPlongee() {
        Game game = third();
        assertTrue(game.enterVolume());
        game.tickAway(1);
        assertFalse(game.isInVolume(), "le volume ne se joue qu'au premier plan");
        assertEquals(0, game.volumeCollected());
    }

    @Test
    void leSeuilDuVolumeOuvreLePalierQuatreDeLaGraine() {
        Game game = third();
        assertFalse(game.isDimensionThresholdReached(3));
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        game.state().setPlaneShardsEarned(Game.PLANE_THRESHOLD);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(4));
        assertEquals(3, game.seedStage(), "sans le seuil du volume, la graine reste au palier 3");
        assertFalse(game.canTendSeed());
        game.state().setVolumeShardsEarned(Game.VOLUME_THRESHOLD - 0.5);
        assertFalse(game.isVolumeThresholdReached());
        game.state().setVolumeShardsEarned(Game.VOLUME_THRESHOLD);
        assertTrue(game.isVolumeThresholdReached());
        assertTrue(game.isDimensionThresholdReached(3));
        assertEquals(4, game.seedStage(), "le seuil atteint, la graine passe au palier 4");
        assertFalse(game.isDimensionThresholdReached(4), "la durée n'a pas son seuil");
    }

    @Test
    void leVolumeTraverseLesUniversEtSeRelit() throws Exception {
        Game game = third();
        game.state().setVolumeShards(7);
        game.state().setVolumeShardsEarned(40);
        game.state().setVolumeLevel(Game.VolumeUpgrade.VALUE.ordinal(), 2);
        assertTrue(game.enterVolume());
        assertTrue(game.startNextUniverse());
        assertEquals(7, game.state().volumeShards(), 1e-12, "les éclats du volume traversent les univers");
        assertEquals(40, game.state().volumeShardsEarned(), 1e-12);
        assertEquals(2, game.volumeLevel(Game.VolumeUpgrade.VALUE));
        assertFalse(game.isInVolume(), "un nouvel univers ne commence pas dans le cube");
        GameState read = SaveCodec.read(SaveCodec.write(game.state(), 0)).state();
        assertEquals(7, read.volumeShards(), 1e-12);
        assertEquals(40, read.volumeShardsEarned(), 1e-12);
        assertEquals(2, read.volumeLevel(Game.VolumeUpgrade.VALUE.ordinal()));
    }

    @Test
    void uneSauvegardeDAvantLeVolumePartDeZero() throws Exception {
        Game game = third();
        game.state().setVolumeShards(5);
        game.state().setVolumeShardsEarned(9);
        game.state().setVolumeLevel(0, 3);
        String text = SaveCodec.write(game.state(), 0);
        String old = text.replaceAll("(?m)^volume[A-Za-z0-9]*=.*\\R", "");
        assertFalse(old.contains("volumeShards="), "les clés du volume sont retirées");
        GameState read = SaveCodec.read(old).state();
        assertEquals(0, read.volumeShards(), 1e-12);
        assertEquals(0, read.volumeShardsEarned(), 1e-12);
        for (int i = 0; i < Game.VolumeUpgrade.values().length; i++) assertEquals(0, read.volumeLevel(i));
    }

    @Test
    void relireUnePartieSortDuCube() throws Exception {
        Game game = third();
        assertTrue(game.enterVolume());
        SaveCodec.readInto(game.state(), SaveCodec.export(game.state(), 0));
        game.restored();
        assertFalse(game.isInVolume(), "une partie relue ne reprend pas au milieu d'une plongée");
    }

    @Test
    void lOutilDEssaiRemetLeVolumeAZero() {
        Game game = third();
        game.state().setVolumeShards(5);
        game.state().setVolumeShardsEarned(9);
        game.state().setVolumeLevel(2, 4);
        assertTrue(game.enterVolume());
        game.testResetUniverses();
        assertEquals(0, game.state().volumeShards(), 1e-12);
        assertEquals(0, game.state().volumeShardsEarned(), 1e-12);
        assertEquals(0, game.volumeLevel(Game.VolumeUpgrade.SPAWN));
        assertFalse(game.isInVolume());
    }

    @Test
    void leGuideDeLUniversSuivantDitQueLeVolumeReste() {
        Game second = second();
        assertFalse(second.universeTransition(true).kept().stream().anyMatch(line -> line.contains("volume")),
                "le volume n'est pas encore ouvert : le guide n'en parle pas");
        Game game = third();
        assertTrue(game.universeTransition(true).kept().stream().anyMatch(line -> line.contains("du volume")),
                "les éclats du volume traversent les univers, le guide le dit");
    }
}
