package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.achievements.Achievements;
import idle.core.profile.Title;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

/**
 * Version 0.318 : ce qui n'a jamais été vu ne se montre nulle part hors du jeu (réglages, skins, profil, statistiques,
 * succès). Le souvenir de ce qui a été vu, ses règles et ce que chaque chose demande. Sans affichage JavaFX
 * (les réglages et les raccourcis, qui en ont besoin, sont vérifiés dans {@link ReglagesSansSpoilTest}).
 */
class RienDeJamaisVuTest {
    @Test
    void auDebutRienNEstVu() {
        Game game = new Game(1L);
        game.start();
        Seen.Memory memory = Seen.Memory.none();
        memory.note(game);
        assertTrue(memory.all().isEmpty(), "rien d'ouvert : " + memory.all());
        for (Seen step : Seen.values()) assertFalse(memory.has(step, game), step.name());
    }

    @Test
    void ceQuiAEteVuLeReste() {
        Game game = new Game(1L);
        game.start();
        int[] writes = {0};
        Seen.Memory memory = new Seen.Memory(EnumSet.noneOf(Seen.class), () -> writes[0]++);
        game.state().setExplosions(1);
        assertTrue(memory.has(Seen.EXPLOSION, game));
        assertEquals(1, writes[0], "le souvenir s'écrit quand il grandit");
        game.state().setExplosions(0);
        assertFalse(Seen.EXPLOSION.in(game), "la partie l'a refermé");
        assertTrue(memory.has(Seen.EXPLOSION), "le souvenir le garde");
        memory.note(game);
        assertEquals(1, writes[0], "rien de nouveau : rien à écrire");
    }

    @Test
    void uneEtapePlusLoinVautPourCellesDAvant() {
        Seen.Memory memory = new Seen.Memory(EnumSet.of(Seen.UNIVERSE), () -> {});
        Game game = new Game(1L);
        memory.note(game);
        for (Seen step : new Seen[] {Seen.EXPLOSION, Seen.COLLIDER, Seen.BIG_BANG, Seen.GALAXY}) assertTrue(memory.has(step), step.name());
        assertFalse(memory.has(Seen.VOID), "le vide n'est pas une étape obligée");
        assertFalse(memory.has(Seen.FINAL));
    }

    @Test
    void chaqueChoseDemandeCeQuElleNomme() {
        assertNull(Seen.of(Notice.START));
        assertEquals(Seen.EXPLOSION, Seen.of(Notice.CHALLENGES));
        assertEquals(Seen.BIG_BANG, Seen.of(Notice.COMETS));
        assertEquals(Seen.VOID, Seen.of(Readiness.Kind.VOID));
        assertEquals(Seen.CHRONO, Seen.of(SoundKind.CHRONO));
        assertNull(Seen.of(SoundKind.PARTICLES));
        assertEquals(Seen.COLLIDER, Seen.of(Cosmetic.GOLD));
        assertNull(Seen.of(Cosmetic.SEPIA));
        assertEquals(Seen.dimension(7), Seen.of(SkinVisual.CLICKER));
        assertEquals(Seen.UNIVERSE, Seen.dimension(1), "la ligne vient avec le premier univers");
        assertEquals(Seen.HADRONS, Seen.of(SkinVisual.GLUONS));
        assertEquals(Seen.VOID, Seen.of(Title.VOID_TAMER));
        assertNull(Seen.of(Title.SPARK));
    }

    @Test
    void lesSuccesSuiventLOrdreDeLaPartie() {
        assertNull(Seen.of(Achievements.byId("step_first_particle")));
        assertEquals(Seen.EXPLOSION, Seen.of(Achievements.byId("step_explosion")));
        assertEquals(Seen.BIG_BANG, Seen.of(Achievements.byId("step_first_molecule")));
        assertEquals(Seen.GALAXY, Seen.of(Achievements.byId("step_universe")));
        assertEquals(Seen.UNIVERSE, Seen.of(Achievements.byId(Achievements.UNIVERSE_SKINS)));
        assertEquals(Seen.VOID, Seen.of(Achievements.byId("late_void")));
        assertEquals(Seen.FINAL, Seen.of(Achievements.byId("late_end")));
    }

    @Test
    void uneConditionEcriteDemandeCeQuElleNomme() {
        assertEquals(EnumSet.of(Seen.PROBES), Seen.mentioned("10 voyages de sonde"));
        assertEquals(EnumSet.of(Seen.HADRONS), Seen.mentioned("100 gluons"));
        assertEquals(EnumSet.of(Seen.FINAL), Seen.mentioned("La partie finie, 9 univers formés"));
        assertTrue(Seen.mentioned("1 000 fusions").isEmpty());
        // Chaque skin qui nomme une chose jamais vue reste caché, même au onzième univers.
        for (Skin skin : Skin.values()) {
            for (Seen step : Seen.mentioned(skin.source())) assertTrue(step != Seen.UNIVERSE, skin + " : " + step);
        }
    }
}
