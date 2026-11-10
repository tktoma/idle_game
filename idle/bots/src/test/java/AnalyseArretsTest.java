import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Version 0.204 : l'outil qui mesure à partir de quand une chose ne sert plus ({@link AnalyseArrets}). Ici, sans rejouer
 * de partie longue : les familles de coupures, la lecture des écarts, et le fait qu'une coupure tient vraiment (un
 * automatisme coupé ne se rallume pas sous le bot, une mécanique coupée n'est plus jouée, une suite rejouée sans coupure
 * depuis un jalon repart du même état).
 */
class AnalyseArretsTest {

    @Test
    void lesFamillesCouvrentToutCeQuiSAllumeEtSAchete() {
        Game catalogue = Bots.fresh(201);
        List<AnalyseArrets.Coupure> automatismes = AnalyseArrets.automatismes(catalogue);
        Set<String> ids = new HashSet<>();
        for (AnalyseArrets.Coupure c : automatismes) assertTrue(ids.add(c.id()), "un identifiant par coupure : " + c.id());
        catalogue.automations().forEach(a -> assertTrue(ids.contains(a.id()), a.id()));
        catalogue.darkAutomations().forEach(a -> assertTrue(ids.contains(a.id()), a.id()));
        for (String autre : List.of("auto_collisionneur", "auto_rassemblement", "auto_formation", "auto_molecules", "auto_appui")) assertTrue(ids.contains(autre), autre);
        assertEquals(10, AnalyseArrets.dimensions().size(), "une coupure par dimension");
        assertEquals(catalogue.upgrades().size(), AnalyseArrets.ameliorations(catalogue).size());
        assertEquals(catalogue.darkUpgrades().size(), AnalyseArrets.arbre(catalogue).size());
        // Une amélioration coupée est retirée de la partie rejouée ; un automatisme ne retire rien.
        for (AnalyseArrets.Coupure c : AnalyseArrets.ameliorations(catalogue)) assertEquals(Set.of(c.id()), c.sans());
        for (AnalyseArrets.Coupure c : automatismes) assertTrue(c.sans().isEmpty());
    }

    @Test
    void unChoixSeFaitParFamilleOuParIdentifiant() {
        assertEquals(AnalyseArrets.automatismes(Bots.fresh(201)).size(), AnalyseArrets.choisies("automatismes").size());
        List<AnalyseArrets.Coupure> deux = AnalyseArrets.choisies("auto_fusion, cometes");
        assertEquals(2, deux.size());
        assertEquals("automatismes", deux.get(0).famille());
        assertEquals("mecaniques", deux.get(1).famille());
        assertEquals(AnalyseMecaniques.toutes().size(), AnalyseArrets.choisies("mecaniques").size());
        assertThrows(IllegalArgumentException.class, () -> AnalyseArrets.choisies("nimportequoi"));
    }

    @Test
    void lePremierJalonInutileEstCeluiDOuToutResteSousLaTolerance() {
        double[] tolerance = {120, 120, 120, 120};
        double nan = Double.NaN, plafond = Double.POSITIVE_INFINITY;
        assertEquals(4, AnalyseArrets.premierInutile(new double[] {nan, plafond, plafond, 900}, tolerance), "sert encore au dernier jalon");
        assertEquals(2, AnalyseArrets.premierInutile(new double[] {plafond, 600, 40, -20}, tolerance));
        assertEquals(0, AnalyseArrets.premierInutile(new double[] {0, 10, -5, 100}, tolerance), "ne sert jamais");
        // Un jalon utile entre deux inutiles : seuls ceux d'après lui comptent.
        assertEquals(3, AnalyseArrets.premierInutile(new double[] {0, 0, 500, 0}, tolerance));
        // Un jalon non mesuré arrête la remontée.
        assertEquals(2, AnalyseArrets.premierInutile(new double[] {0, nan, 0, 0}, tolerance));
        // Deux hasards : le pire des deux compte ; un gain ne compte que s'il est confirmé.
        double[][] deux = {{0, -900, -900, 30}, {nan, 600, -700, nan}};
        assertEquals(600, AnalyseArrets.pire(deux)[1]);
        assertEquals(2, AnalyseArrets.premierInutile(AnalyseArrets.pire(deux), tolerance), "au jalon 2, le second hasard dit que la chose sert");
        assertEquals(List.of(2), AnalyseArrets.gains(deux, tolerance), "un seul gain confirmé par les deux hasards");
        assertEquals("+2min05", AnalyseArrets.ecartLisible(125));
        assertEquals("−1h02", AnalyseArrets.ecartLisible(-3725));
        assertEquals("0s", AnalyseArrets.ecartLisible(0.2));
        assertEquals("plafond", AnalyseArrets.ecartLisible(plafond));
        assertEquals("·", AnalyseArrets.ecartLisible(nan));
    }

    @Test
    void unAutomatismeCoupeNeSeRallumePasSousLeBot() throws Exception {
        // Une partie avancée, où le bot tient ses automatismes allumés.
        Game game = Bots.load(java.nio.file.Path.of("sauvegardes", "reference", "checkpoint_fresh_fifth_bang.save"), 201);
        IdleProfiles24 bot = new IdleProfiles24(game, IdleProfiles24.PROFILES[0], 201, 20);
        bot.quiet = true;
        bot.begin();
        AnalyseArrets.Coupure fusion = AnalyseArrets.choisies("auto_fusion").get(0);
        fusion.regler().accept(bot);
        assertFalse(game.isAutomationEnabled("auto_fusion"));
        long fusions = game.state().stats().fusions();
        boolean autre = false;
        for (int k = 0; k < 90; k++) {
            bot.stepOnce();
            assertFalse(game.isAutomationEnabled("auto_fusion"), "au pas " + k);
            autre |= game.isAutomationEnabled("auto_speed");
        }
        assertTrue(autre, "les autres automatismes tournent toujours");
        // Le bot fusionne encore à la main s'il le veut ; l'automatisme, lui, n'a rien fait de plus qu'un bot sans lui.
        assertTrue(game.state().stats().fusions() >= fusions);
    }

    @Test
    void uneMecaniqueCoupeeNEstPlusJouee() throws Exception {
        Game game = Bots.load(java.nio.file.Path.of("sauvegardes", "reference", "checkpoint_fresh_fifth_bang.save"), 201);
        IdleProfiles24 bot = new IdleProfiles24(game, IdleProfiles24.PROFILES[0], 201, 20);
        bot.quiet = true;
        assertFalse(bot.sansJouer("collisionneur"));
        for (AnalyseArrets.Coupure c : AnalyseMecaniques.toutes()) c.regler().accept(bot);
        assertFalse(bot.holdsBuyAll(), "M relâchée (le Speedrunner ne la tient pas ; un profil qui la tient la relâche)");
        List<String> noms = new ArrayList<>();
        for (AnalyseArrets.Coupure c : AnalyseMecaniques.toutes()) {
            assertTrue(bot.sansJouer(c.id()), c.id());
            noms.add(c.id());
        }
        assertEquals(List.of("collisionneur", "sondes", "defis", "cometes", "anomalies", "impulsions", "appui_m"), noms);
        // La mémoire d'un bot copié garde ses coupures.
        IdleProfiles24 copie = new IdleProfiles24(Bots.fresh(201), IdleProfiles24.PROFILES[0], 201, 20);
        AnalyseCopie.memoire(bot, copie);
        assertTrue(copie.sansJouer("sondes"));
    }
}
