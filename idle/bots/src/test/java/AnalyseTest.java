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
        for (var d : idle.core.darkmatter.DarkUpgrades.DEFAULT) {
            String expected = switch (d.branch()) {
                case PARTICLES -> "particles";
                case ATOMS -> "atoms";
                case SIZE -> null;
                case DARK_MATTER -> "darkMatter";
            };
            assertEquals(expected, BotAnalyste.monnaie("darkUpgradeLevels", d.id()), "l'arbre se paie selon sa branche : " + d.id());
        }
        assertEquals("atoms", BotAnalyste.monnaie("ownedAutomations", idle.core.upgrades.Automations.DEFAULT.get(0).id()), "les automatismes se paient en atomes");
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
        bot.etsi = false;
        bot.joue(1);
        for (String file : List.of("journal.txt", "achats.csv", "paliers.csv", "estimations.csv", "rapport.html", "fin.save", "classement.csv", "etsi.csv")) {
            assertTrue(Files.exists(dir.resolve(file)), file);
        }
        assertFalse(bot.journal.achats.isEmpty(), "une heure de jeu : des achats");
        assertTrue(bot.journal.achats.stream().allMatch(a -> a.t() >= 0 && a.t() <= 3_600 + 5));
        assertFalse(bot.journal.estimations.isEmpty(), "une estimation au départ");
        assertTrue(bot.journal.estimations.get(0).secondes() > 0, "la première explosion arrive en moins de 24 h");
        String page = Files.readString(dir.resolve("rapport.html"));
        assertTrue(page.contains("Rapport du bot analyste") && page.contains("<svg"));
        assertTrue(Files.readString(dir.resolve("journal.txt")).contains("[00:00:"));
        // Version 0.162 : les achats ont un coût en temps de production, et le classement les range.
        assertTrue(bot.journal.achats.stream().anyMatch(a -> Double.isFinite(a.coutSecondes()) && a.coutSecondes() > 0), "un coût en production connu");
        assertTrue(bot.revenuLog.containsKey("particles"), "ce que rapportent les particules est mesuré");
        assertFalse(bot.journal.classement().isEmpty());
        assertTrue(Files.readString(dir.resolve("journal.txt")).contains("Rentabilité : coûte"));
    }

    @Test
    void uneRemiseAZeroDitQuiACoupeLesAutomatismes() {
        AnalyseEtat.Nombre un = AnalyseEtat.Nombre.of(1), deux = AnalyseEtat.Nombre.of(2);
        assertEquals("l'explosion", BotAnalyste.cause(List.of(new AnalyseEtat.Changement("explosions", null, un, deux, null, null))));
        assertEquals("le début du défi", BotAnalyste.cause(List.of(new AnalyseEtat.Changement("activeChallenge", "x", null, null, "x", null))));
        assertEquals(null, BotAnalyste.cause(List.of(new AnalyseEtat.Changement("particles", null, deux, un, null, null))), "un achat n'est pas une remise à zéro");
    }

    @Test
    void laRentabiliteSeLitEnMots() {
        assertEquals("Rentabilité : coûte 1 min 00 s de production (Particules) ; en fait gagner 3 min 00 s : ×3,00, rentable avant le palier.",
                BotAnalyste.rentabiliteTexte(60, 180, "Particules"));
        assertTrue(BotAnalyste.rentabiliteTexte(600, 60, "Atomes").contains("×0,10, pas remboursé avant le palier"));
        assertTrue(BotAnalyste.rentabiliteTexte(60, Double.NaN, "Atomes").contains("gain de temps inconnu"));
    }

    @Test
    void leClassementRangeDuPlusRentableAuMoinsRentable() {
        AnalyseJournal j = new AnalyseJournal();
        j.achats.add(achat("Amélioration", "A", 100, 50));
        j.achats.add(achat("Amélioration", "B", 10, 100));
        j.achats.add(achat("Amélioration", "B", 10, 100));
        j.achats.add(achat("Amélioration", "C", Double.NaN, 100));
        List<AnalyseJournal.Classement> c = j.classement();
        assertEquals("Amélioration — B", c.get(0).quoi());
        assertEquals(2, c.get(0).achats());
        assertEquals(10, c.get(0).rentabilite(), 1e-9);
        assertEquals(0.5, c.get(1).rentabilite(), 1e-9);
        assertTrue(Double.isNaN(c.get(2).rentabilite()), "sans coût connu : à la fin");
    }

    private static AnalyseJournal.Achat achat(String groupe, String chose, double cout, double gain) {
        return new AnalyseJournal.Achat(0, 1, groupe, chose, 1, "Particules", "1", 0.1, 0, "", 0, gain, cout,
                Double.isFinite(cout) ? gain / cout : Double.NaN);
    }

    @Test
    void lesDefisLesSuccesEtLesAutomatismesDisentPourquoi() {
        for (idle.core.darkmatter.Challenge c : idle.core.darkmatter.Challenges.ALL) {
            assertFalse(AnalysePourquoi.contrainte(c).isBlank(), c.id());
            assertFalse(AnalysePourquoi.recompense(c).isBlank(), c.id());
            assertEquals(c, AnalysePourquoi.defi(c.id()));
        }
        Game game = Bots.fresh(7);
        String first = idle.core.darkmatter.Challenges.DEFAULT.get(0).id();
        assertTrue(AnalysePourquoi.lancement(game, first).stream().anyMatch(l -> l.startsWith("Récompense")));
        assertTrue(AnalysePourquoi.lancement(game, "VOID").stream().anyMatch(l -> l.contains(idle.core.bigbang.BangChallenge.VOID.rewardText())));
        idle.core.achievements.Achievement feat = game.achievements().stream().filter(a -> a.hasBonus()).findFirst().orElseThrow();
        List<String> why = AnalysePourquoi.succes(game, feat.id());
        assertTrue(why.get(0).contains(feat.description()), why.toString());
        assertTrue(why.stream().anyMatch(l -> l.startsWith("Bonus propre")));
        String auto = idle.core.upgrades.Automations.DEFAULT.get(0).id();
        assertFalse(AnalysePourquoi.role(auto).isBlank());
        assertTrue(AnalysePourquoi.role("PLANE_AIM").startsWith("joue la dimension 2"));
        assertEquals("", AnalysePourquoi.role("inconnu"));
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
