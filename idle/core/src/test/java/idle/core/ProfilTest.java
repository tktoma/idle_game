package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.profile.Title;
import idle.core.save.SaveCodec;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.147 : le profil du joueur (nom, avatar, titres) et les albums de ce qu'il a obtenu. Rien de tout cela ne
 * donne de bonus ; tout traverse le Big Bang, l'univers suivant et « Recommencer de zéro », comme les records.
 */
class ProfilTest {

    @Test
    void lesTitresTombentQuandLeurConditionEstVraieEtSAnnoncentUneFois() {
        Game game = new Game(42L);
        game.tick(1);
        assertTrue(game.titles().isEmpty(), "rien avant le premier générateur");
        assertNull(game.currentTitle());
        game.start();
        game.tick(1);
        assertEquals(List.of(Title.SPARK), game.titles());
        assertEquals(List.of(Title.SPARK), game.takeNewTitles());
        assertTrue(game.takeNewTitles().isEmpty(), "annoncé une seule fois");
        assertEquals(Title.SPARK, game.currentTitle());
        assertEquals(game.state().timePlayed(), game.titleEarnedAt(Title.SPARK), 1e-9, "le temps de jeu où il est tombé");
        game.state().stats().noteFusion(idle.core.math.BigNum.ONE, 10);
        game.tick(1);
        assertTrue(game.hasTitle(Title.APPRENTICE_ALCHEMIST));
        assertEquals(Title.APPRENTICE_ALCHEMIST, game.currentTitle(), "sans choix, le plus avancé");
    }

    @Test
    void onPorteLeTitreQuOnVeutParmiCeuxQuOnA() {
        Game game = GameTestSupport.realGame();
        game.state().stats().noteFusion(idle.core.math.BigNum.ONE, 10);
        game.tick(1);
        assertFalse(game.chooseTitle(Title.GALAXY_ARCHITECT), "pas encore la galaxie");
        assertEquals(Title.APPRENTICE_ALCHEMIST, game.currentTitle());
        assertTrue(game.chooseTitle(Title.SPARK));
        assertEquals(Title.SPARK, game.currentTitle());
        assertTrue(game.chooseTitle(null));
        assertEquals(Title.APPRENTICE_ALCHEMIST, game.currentTitle(), "null revient au plus avancé");
        game.state().setChosenTitle("UN_TITRE_DISPARU");
        assertEquals(Title.APPRENTICE_ALCHEMIST, game.currentTitle(), "un nom inconnu ne casse rien");
    }

    @Test
    void lesQuatreTitresDuDeroulementTombentAuBonMoment() {
        Game game = GameTestSupport.realGame();
        game.state().addBody("brown_dwarf");
        game.tick(1);
        assertTrue(game.hasTitle(Title.STAR_SMITH), "une étoile formée");
        assertFalse(game.hasTitle(Title.GALAXY_ARCHITECT));
        game.testFormUniverse();
        game.tick(1);
        assertTrue(game.hasTitle(Title.UNIVERSE_CREATOR));
        assertFalse(game.hasTitle(Title.MULTIVERSE_WEAVER), "un seul univers formé");
        assertTrue(game.startNextUniverse());
        game.testFormUniverse();
        game.tick(1);
        assertTrue(game.hasTitle(Title.MULTIVERSE_WEAVER));
        assertTrue(game.hasTitle(Title.STAR_SMITH), "le nouvel univers ne reprend pas les titres");
    }

    @Test
    void leNomEstNettoyeEtBorne() {
        Game game = GameTestSupport.realGame();
        assertEquals(Game.DEFAULT_PLAYER_NAME, game.playerName(), "« Joueur » tant qu'il n'en a pas choisi");
        game.state().setPlayerName("   Zoé    la   grande  ");
        assertEquals("Zoé la grande", game.playerName());
        game.state().setPlayerName("x".repeat(40));
        assertEquals(GameStateCollider.PLAYER_NAME_MAX, game.playerName().length());
        game.state().setPlayerName(null);
        assertEquals(Game.DEFAULT_PLAYER_NAME, game.playerName());
    }

    @Test
    void lesAlbumsNotentLaPremiereFoisEtTraversentTout() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 1);
        game.state().setMoleculeCount("H2O", 3);
        game.state().addSubstance("H2O");
        game.state().addBody("rocky_rubble");
        game.tick(1);
        assertTrue(game.state().foundMolecules().containsKey("H2O"));
        assertTrue(game.state().foundSubstances().containsKey("H2O"));
        assertTrue(game.state().foundBodies().containsKey("rocky_rubble"));
        assertFalse(game.state().foundAssemblies().containsKey("granitic_rock"));
        double first = game.state().foundMolecules().get("H2O");
        game.tick(5);
        assertEquals(first, game.state().foundMolecules().get("H2O"), 0, "la première fois reste");
        // L'univers suivant repart sans molécules ni astres ; l'album les garde.
        game.testFormUniverse();
        assertTrue(game.startNextUniverse());
        assertEquals(0, game.moleculeCount("H2O"));
        assertFalse(game.hasBody("rocky_rubble"));
        game.tick(1);
        assertTrue(game.state().foundMolecules().containsKey("H2O") && game.state().foundBodies().containsKey("rocky_rubble"));
        // « Recommencer de zéro » garde le profil et les albums, comme les records.
        game.state().setPlayerName("Zoé");
        game.reset();
        assertTrue(game.state().foundBodies().containsKey("rocky_rubble"));
        assertTrue(game.hasTitle(Title.SPARK));
        assertEquals("Zoé", game.playerName());
    }

    @Test
    void leProfilEtLesAlbumsSeSauventEtUneVieilleSauvegardeRepartVide() throws Exception {
        GameState state = SaveTest.richState();
        GameState copy = GameTestSupport.reread(state);
        assertEquals("Zoé ; « la » 100 % = 1|2", copy.playerName(), "les caractères spéciaux passent");
        assertEquals("custom", copy.avatar());
        assertEquals("STAR_SMITH", copy.chosenTitle());
        assertEquals(98_765.25, copy.earnedTitles().get("STAR_SMITH"), 0);
        assertEquals(60_000.75, copy.foundBodies().get("rocky_rubble"), 0);
        // Une sauvegarde d'avant la 0.147 : sans ses clés, le profil part vide.
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(state, 0).split("\n")) {
            if (line.startsWith("playerName=") || line.startsWith("avatar=") || line.startsWith("chosenTitle=")
                    || line.startsWith("titles=") || line.startsWith("found.")) continue;
            old.append(line).append('\n');
        }
        GameState older = SaveCodec.read(old.toString()).state();
        assertEquals("", older.playerName());
        assertEquals("", older.avatar());
        assertTrue(older.earnedTitles().isEmpty() && older.foundMolecules().isEmpty() && older.foundBodies().isEmpty());
    }
}
