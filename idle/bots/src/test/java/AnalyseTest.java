import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Version 0.161 : le bot analyste. Ses photos de la partie voient ce qu'un achat a fait et ce qu'il a coûté ; il nomme
 * les choses en français ; il range les paliers dans l'ordre ; il écrit son journal, ses tableaux et sa page.
 */
class AnalyseTest {

    @Test
    void lesNombresSeLisentEtSeComparentMemeTresGrands() {
        AnalyseEtat.Nombre huge = AnalyseEtat.Nombre.parse("4.18e27316531");
        AnalyseEtat.Nombre small = AnalyseEtat.Nombre.parse("3.5E7");
        assertTrue(huge.greater(small));
        assertEquals(27316531.62, huge.log10(), 0.01);
        assertEquals(35_000_000, small.toDouble(), 1e-6);
        assertEquals(1_000, AnalyseEtat.Nombre.of(1_500).minus(AnalyseEtat.Nombre.of(500)).toDouble(), 1e-6);
        assertEquals("1,23e9", AnalyseEtat.Nombre.of(1.234e9).texte().replace('.', ','));
        assertEquals(null, AnalyseEtat.Nombre.parse("RESERVE"));
    }

    @Test
    void unAchatSeVoitDansLeDiffAvecSonPrix() {
        Game game = Bots.fresh(7);
        for (int i = 0; i < 600; i++) game.tick(1);
        AnalyseEtat avant = AnalyseEtat.of(game.state());
        String id = game.upgrades(idle.core.Resource.PARTICLES).get(0).id();
        double particles = game.state().particles().toDouble();
        assertTrue(game.buy(id), "assez de particules pour la première amélioration");
        List<AnalyseEtat.Changement> diff = AnalyseEtat.diff(avant, AnalyseEtat.of(game.state()));
        assertTrue(diff.stream().anyMatch(c -> c.cle().equals("upgradeLevels") && id.equals(c.sousCle()) && c.monte()), diff.toString());
        AnalyseEtat.Changement paid = diff.stream().filter(c -> c.cle().equals("particles")).findFirst().orElseThrow();
        assertTrue(paid.baisse());
        assertEquals(particles - game.state().particles().toDouble(), paid.avant().minus(paid.apres()).toDouble(), particles * 1e-9);
        assertEquals("particles", BotAnalyste.monnaie("upgradeLevels", id));
    }

    @Test
    void lesChosesSeNommentEnFrancais() {
        assertEquals("Particules", AnalyseNoms.cle("particles"));
        assertEquals("Plan : Valeur", AnalyseNoms.cle("planeLevel0"));
        assertEquals("Graine : " + Game.SeedUpgrade.ASTRAL_LIGHT.label(), AnalyseNoms.cle("seedUpgrade" + Game.SeedUpgrade.ASTRAL_LIGHT.ordinal()));
        assertEquals("Proton", AnalyseNoms.chose("PROTON"));
        assertEquals("élément Z = 26", AnalyseNoms.chose("26"));
        assertEquals("inconnue", AnalyseNoms.cle("inconnue"), "une clé inconnue garde son nom");
        assertEquals("planeShards", BotAnalyste.monnaie("planeLevel2", null));
        assertEquals("mixShards", BotAnalyste.monnaie("seedUpgrade" + Game.SeedUpgrade.ROOT_BUBBLE.ordinal(), null));
    }

    @Test
    void lesPaliersSeSuivent() {
        assertEquals("Début", BotAnalyste.nomPalier(0));
        assertEquals("Univers 1 : acte 1 : première explosion", BotAnalyste.nomPalier(1));
        assertEquals("Univers 1 formé (acte 3)", BotAnalyste.nomPalier(6));
        assertEquals("Univers 2 : début", BotAnalyste.nomPalier(7));
        assertEquals("Univers 2 : galaxie", BotAnalyste.nomPalier(11));
        assertEquals("01:02:03", AnalyseJournal.chrono(3723));
        assertEquals("82:45:13", AnalyseJournal.chrono(82 * 3600 + 45 * 60 + 13));
        assertEquals("1 h 02 min 03 s", AnalyseJournal.duree(3723));
        assertEquals("76-12-25_Univers_11_forme_acte_3.save", BotAnalyste.nomSauvegarde(76 * 3600 + 12 * 60 + 25, "Univers 11 formé (acte 3)"),
                "un nom de sauvegarde sans accents");
    }

    @Test
    void uneCourtePartieEcritSonJournalSesTableauxEtSaPage(@TempDir Path dir) throws Exception {
        BotAnalyste bot = new BotAnalyste(Bots.fresh(201), 201, 5, dir, 30);
        bot.targetUniverses = 1;
        bot.joue(1);
        for (String file : List.of("journal.txt", "achats.csv", "paliers.csv", "estimations.csv", "rapport.html", "fin.save")) {
            assertTrue(Files.exists(dir.resolve(file)), file);
        }
        assertFalse(bot.journal.achats.isEmpty(), "une heure de jeu : des achats");
        assertTrue(bot.journal.achats.stream().allMatch(a -> a.t() >= 0 && a.t() <= 3_600 + 5));
        assertFalse(bot.journal.estimations.isEmpty(), "une estimation au départ");
        assertTrue(bot.journal.estimations.get(0).secondes() > 0, "la première explosion arrive en moins de 24 h");
        String page = Files.readString(dir.resolve("rapport.html"));
        assertTrue(page.contains("Rapport du bot analyste") && page.contains("<svg"));
        assertTrue(Files.readString(dir.resolve("journal.txt")).contains("[00:00:"));
    }

    @Test
    void seulsLesAchatsSontDesAchats() {
        assertTrue(BotAnalyste.achat("planeLevel2"));
        assertTrue(BotAnalyste.achat("lineYieldLevel"));
        assertTrue(BotAnalyste.achat("upgradeLevels"));
        assertTrue(BotAnalyste.achat("seedUpgrade3"));
        assertFalse(BotAnalyste.achat("pulses"), "un compteur d'impulsions n'est pas un achat");
        assertFalse(BotAnalyste.achat("linePhase"));
        assertFalse(BotAnalyste.achat("synthesisCount"));
        assertTrue(BotAnalyste.REGLAGES.containsKey("voidShare"), "un réglage se note comme un choix");
    }

    @Test
    void uneFaconDeJouerChangeLesChoixDuBot(@TempDir Path dir) {
        BotAnalyste bot = new BotAnalyste(Bots.fresh(201), 201, 5, dir, 30);
        AnalyseStrategies.Strategie chimiste = AnalyseStrategies.TOUTES.stream().filter(s -> s.nom().equals("Chimiste")).findFirst().orElseThrow();
        AnalyseStrategies.appliquer(bot, chimiste);
        assertEquals("chemistry", bot.p.bias());
        assertEquals(5, bot.p.actionEverySeconds(), "à vitesse humaine");
        assertEquals(1, bot.p.attention(), 0, "sans faute");
        assertEquals(IdleProfiles24.EARLY_STYLES[6], bot.earlyStyle);
        assertEquals(6, AnalyseStrategies.TOUTES.size());
    }
}
