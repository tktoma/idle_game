package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.history.Achats;
import idle.core.history.Noms;
import idle.core.history.Pace;
import idle.core.history.Photo;
import idle.core.history.PurchaseHistory;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.165 : l'historique des achats du joueur et le compteur de la prochaine étape. Deux photos de la partie sans
 * temps entre elles disent ce qui a été acheté et ce que cela a coûté ; le rythme de l'avancée dit dans combien de temps
 * vient l'étape suivante.
 */
class HistoriqueTest {

    @Test
    void deuxPhotosDisentCeQuiAEteAcheteEtSonPrix() {
        Game game = GameTestSupport.gameWithParticles(1e6);
        Photo avant = Photo.of(game.state());
        String id = game.upgrades(Resource.PARTICLES).get(0).id();
        assertTrue(game.buy(id));
        List<Achats.Achat> achats = Achats.lire(Photo.diff(avant, Photo.of(game.state())));
        assertEquals(1, achats.size());
        Achats.Achat a = achats.get(0);
        assertEquals("Amélioration — " + Noms.chose(id), a.quoi());
        assertEquals("Particules", a.paye());
        assertEquals(1e6 - game.state().particles().toDouble(), Math.pow(10, a.montantLog()), 1e-3);
        assertEquals(1, a.quantite(), 0);
        assertEquals("particles", Achats.monnaie("upgradeLevels", id));
        assertEquals("Particules", Noms.cle("particles"));
    }

    @Test
    void lesReglagesEtLesCompteursNeSontPasDesAchats() {
        assertFalse(Achats.achat("pulses"));
        assertFalse(Achats.achat("synthesisCount"));
        assertTrue(Achats.achat("planeLevel2"));
        assertTrue(Achats.REGLAGES.containsKey("voidShare"));
        Photo.Nombre big = Photo.Nombre.parse("4.18e27316531");
        assertTrue(big.greater(Photo.Nombre.of(1e300)));
    }

    @Test
    void lHistoriqueNoteLesAchatsEntreDeuxPhotosEtRienSansAchat() {
        Game game = GameTestSupport.gameWithParticles(1e6);
        PurchaseHistory history = new PurchaseHistory();
        Pace pace = new Pace();
        pace.observe(game);
        assertTrue(history.after(game, pace).isEmpty(), "sans photo d'avant, rien");
        history.before(game);
        assertTrue(history.isOpen());
        assertTrue(history.after(game, pace).isEmpty(), "une photo, mais pas d'achat : rien");
        assertFalse(history.isOpen());
        history.before(game);
        String id = game.upgrades(Resource.PARTICLES).get(0).id();
        game.buy(id);
        game.buy(id);
        List<PurchaseHistory.Entry> added = history.after(game, pace);
        assertEquals(1, added.size(), "deux niveaux de la même amélioration : un achat ×2");
        assertEquals(2, added.get(0).quantite(), 0);
        assertTrue(added.get(0).poids().contains("×"), added.get(0).poids());
        assertEquals(1, history.recent().size());
        assertEquals(1, history.totals().get(0).achats());
        // Ce qui s'achète pendant le temps de jeu (les automatismes) n'est pas dans une fenêtre : rien n'est noté.
        game.tick(10);
        assertTrue(history.after(game, pace).isEmpty());
        history.clear();
        assertTrue(history.recent().isEmpty());
    }

    @Test
    void leCompteurDeLaProchaineEtapeSuitLeRythme() {
        Game game = GameTestSupport.newGame();
        Pace pace = new Pace();
        pace.observe(game);
        assertEquals(GameStats.Step.FIRST_FUSION, pace.step());
        assertTrue(Double.isNaN(pace.secondsLeft()), "pas de rythme sans relevés");
        // Un générateur de plus sur les trois de la fusion : l'avancée monte, le rythme se mesure.
        for (int k = 0; k <= 1; k++) {
            game.state().setLevel(GameTestSupport.GENERATOR.id(), k);
            for (int s = 0; s < 30; s++) {
                game.state().setTimePlayed(game.state().timePlayed() + 5);
                pace.observe(game);
            }
        }
        double left = pace.secondsLeft();
        assertTrue(left > 0 && Double.isFinite(left), "une estimation : " + left);
        assertEquals(Multipliers.Quantity.PRODUCTION, Pace.engine(GameStats.Step.FIRST_EXPLOSION));
        assertEquals(Multipliers.Quantity.DARK_MATTER, Pace.engine(GameStats.Step.FIRST_BIG_BANG));
    }

    @Test
    void uneAvanceeQuiNeBougePasNeDonneAucuneEstimation() {
        Game game = GameTestSupport.newGame();
        Pace pace = new Pace();
        pace.observe(game);
        assertEquals("il faut quelques minutes d'avancée", pace.why());
        // Rien de plus pendant dix minutes : la pente est nulle, il n'y a rien à estimer.
        for (int s = 0; s < 130; s++) {
            game.state().setTimePlayed(game.state().timePlayed() + 5);
            pace.observe(game);
        }
        assertTrue(Double.isNaN(pace.secondsLeft()), "une avancée immobile : " + pace.secondsLeft());
        assertEquals("l'avancée ne bouge pas assez en ce moment", pace.why());
        game.state().setLevel(GameTestSupport.GENERATOR.id(), 1);
        game.state().setTimePlayed(game.state().timePlayed() + 5);
        pace.observe(game);
        assertTrue(pace.secondsLeft() > 0, "elle repart : " + pace.secondsLeft());
        assertEquals("", pace.why());
    }

    @Test
    void lesParticulesDuBigBangSeComptentCommeLaCondition() throws Exception {
        Game game = GameTestSupport.newGame();
        java.lang.reflect.Method part = Pace.class.getDeclaredMethod("part", Game.class, idle.core.bigbang.BigBangCondition.class);
        part.setAccessible(true);
        // La condition lit la branche de matière noire (le plus grand des particules en main et de la production d'une
        // seconde) ; la moitié des ordres de grandeur demandés, c'est la moitié de la condition.
        game.state().setParticles(idle.core.math.BigNum.pow10(game.bigBangParticles().log10() / 2));
        assertEquals(0.5, (double) part.invoke(null, game, idle.core.bigbang.BigBangCondition.PARTICLES), 1e-3);
        assertEquals(0, (double) part.invoke(null, game, idle.core.bigbang.BigBangCondition.ACHIEVEMENTS), 1e-12);
    }

    @Test
    void sansEtapeAVenirPasDEstimation() {
        Pace pace = new Pace();
        assertNull(pace.step());
        assertTrue(Double.isNaN(pace.secondsLeft()));
        assertEquals("10^4.20", PurchaseHistory.facteur(4.2));
        assertEquals("1,250", PurchaseHistory.facteur(Math.log10(1.25)));
    }
}
