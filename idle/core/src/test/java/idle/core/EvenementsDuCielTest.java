package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeTrip;
import idle.core.sky.SkyEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Version 0.127 : les anomalies du ciel, facultatives, petites, et sans effet sur le hasard du jeu. */
class EvenementsDuCielTest {

    private static Game afterBigBang() {
        Game game = new Game();
        game.start();
        GameTestSupport.bangs(game, 1);
        return game;
    }

    /** Fait passer le temps jusqu'à la prochaine anomalie, et la rend. */
    private static SkyEvent waitForEvent(Game game) {
        for (int i = 0; i < 400 && game.skyEvent() == null; i++) game.tick(10);
        return game.skyEvent();
    }

    @Test
    void rienAvantLePremierBigBang() {
        Game game = new Game();
        game.start();
        game.tick(3 * 3600);
        assertNull(game.skyEvent());
        assertNull(game.catchSkyEvent());
    }

    @Test
    void uneAnomalieVientPuisRepartSiOnLaLaisse() {
        Game game = afterBigBang();
        assertEquals(SkyEvent.PARTICLE_WIND, waitForEvent(game), "sans sonde en vol, c'est un vent de particules");
        assertTrue(game.takeSkyEventAppeared());
        assertEquals(Game.SKY_EVENT_VISIBLE_SECONDS, game.skyEventVisibleFor(), 10.0001);
        game.tick(Game.SKY_EVENT_VISIBLE_SECONDS + 1);
        assertNull(game.skyEvent(), "partie sans rien coûter");
        assertEquals(1, game.windMultiplier());
        assertEquals(0, game.state().skyEventsCaught());
    }

    @Test
    void leVentDoubleLesParticulesPuisRetombe() {
        Game game = afterBigBang();
        waitForEvent(game);
        double before = game.windMultiplier();
        assertEquals(SkyEvent.PARTICLE_WIND, game.catchSkyEvent());
        assertEquals(1, before);
        assertEquals(Game.WIND_FACTOR, game.windMultiplier());
        assertEquals(1, game.state().skyEventsCaught());
        assertNull(game.skyEvent());
        game.tick(Game.WIND_SECONDS / 2);
        assertEquals(Game.WIND_FACTOR, game.windMultiplier());
        game.tick(Game.WIND_SECONDS);
        assertEquals(1, game.windMultiplier(), "le vent est passé");
    }

    @Test
    void leVentDoubleVraimentAvecLesPuissances() {
        boolean off = GameParticles.powersOff;
        GameParticles.powersOff = false;
        try {
            Game game = afterBigBang();
            game.achievements().stream().limit(30).forEach(achievement -> game.state().addAchievement(achievement.id()));
            game.state().setParticles(idle.core.math.BigNum.of(1, 50));
            assertEquals(SkyEvent.PARTICLE_WIND, waitForEvent(game));
            assertTrue(game.systemsPower() > 0, "une puissance agit");
            double before = game.particlesPerCreation().log10();
            game.catchSkyEvent();
            double after = game.particlesPerCreation().log10();
            assertEquals(Math.log10(Game.WIND_FACTOR), after - before, 1e-9, "×2, pas ×2 élevé à la puissance");
        } finally {
            GameParticles.powersOff = off;
        }
    }

    @Test
    void laFenetreDExplorationFaitAvancerLesSondesEnVol() {
        Game game = afterBigBang();
        game.state().setProbes(List.of(new Probe(ProbeDestination.values()[0], ProbeTrip.values()[ProbeTrip.values().length - 1],
                5 * 3600, 5 * 3600, 1)));
        game.state().setSkyEvent(SkyEvent.EXPLORATION_WINDOW.name());
        game.state().setSkyEventVisible(60);
        assertEquals(SkyEvent.EXPLORATION_WINDOW, game.catchSkyEvent());
        assertEquals(5 * 3600 - Game.EXPLORATION_SECONDS, game.state().probes().get(0).left(), 1e-6);
        assertEquals(1, game.windMultiplier(), "pas de vent en plus");
    }

    @Test
    void sansSondeEnVolLaFenetreLaissePasserUnVent() {
        Game game = afterBigBang();
        game.state().setSkyEvent(SkyEvent.EXPLORATION_WINDOW.name());
        game.state().setSkyEventVisible(60);
        assertEquals(SkyEvent.PARTICLE_WIND, game.catchSkyEvent());
        assertEquals(Game.WIND_FACTOR, game.windMultiplier());
    }

    @Test
    void leVentCompteExactementSonTempsSurUnGrosPas() {
        Game big = afterBigBang(), small = afterBigBang();
        for (Game game : List.of(big, small)) {
            game.state().setLevel("generator", 5);
            game.state().setWindLeft(5);
        }
        big.tick(12);
        for (int i = 0; i < 1200; i++) small.tick(0.01);
        double ratio = big.state().particles().toDouble() / small.state().particles().toDouble();
        assertEquals(1, ratio, 0.02);
    }

    @Test
    void lesAnomaliesNeTouchentPasAuHasardDuJeu() {
        // Deux parties identiques, l'une où l'on saisit tout ce qui passe : la synthèse tire les mêmes éléments.
        Game catching = afterBigBang(), ignoring = afterBigBang();
        for (int i = 0; i < 300; i++) {
            catching.tick(10);
            ignoring.tick(10);
            catching.catchSkyEvent();
        }
        assertTrue(catching.state().skyEventsCaught() > 0, "il en est passé au moins une");
        for (Game game : List.of(catching, ignoring)) {
            game.state().setAtoms(BigNum.of(1, 9));
            for (int i = 0; i < 20; i++) game.synthesize();
        }
        for (int number = 1; number <= 118; number++) {
            assertEquals(ignoring.elementCount(number), catching.elementCount(number), "élément " + number);
        }
    }

    @Test
    void lAnomalieSeSauvegarde() throws Exception {
        Game game = afterBigBang();
        assertNotNull(waitForEvent(game));
        GameState back = idle.core.save.SaveCodec.read(idle.core.save.SaveCodec.write(game.state(), 0)).state();
        assertEquals(game.state().skyEvent(), back.skyEvent());
        assertEquals(game.state().skyEventVisible(), back.skyEventVisible(), 1e-9);
    }
}
