import idle.core.history.Achats;
import idle.core.history.Photo;
import idle.core.history.Noms;
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
        Photo.Nombre huge = Photo.Nombre.parse("4.18e27316531");
        Photo.Nombre small = Photo.Nombre.parse("3.5E7");
        assertTrue(huge.greater(small));
        assertEquals(27316531.62, huge.log10(), 0.01);
        assertEquals(35_000_000, small.toDouble(), 1e-6);
        assertEquals(1_000, Photo.Nombre.of(1_500).minus(Photo.Nombre.of(500)).toDouble(), 1e-6);
        assertEquals("1,23e9", Photo.Nombre.of(1.234e9).texte().replace('.', ','));
        assertEquals(null, Photo.Nombre.parse("RESERVE"));
    }

    @Test
    void unAchatSeVoitDansLeDiffAvecSonPrix() {
        Game game = Bots.fresh(7);
        for (int i = 0; i < 600; i++) game.tick(1);
        Photo avant = Photo.of(game.state());
        String id = game.upgrades(idle.core.Resource.PARTICLES).get(0).id();
        double particles = game.state().particles().toDouble();
        assertTrue(game.buy(id), "assez de particules pour la première amélioration");
        List<Photo.Changement> diff = Photo.diff(avant, Photo.of(game.state()));
        assertTrue(diff.stream().anyMatch(c -> c.cle().equals("upgradeLevels") && id.equals(c.sousCle()) && c.monte()), diff.toString());
        Photo.Changement paid = diff.stream().filter(c -> c.cle().equals("particles")).findFirst().orElseThrow();
        assertTrue(paid.baisse());
        assertEquals(particles - game.state().particles().toDouble(), paid.avant().minus(paid.apres()).toDouble(), particles * 1e-9);
        assertEquals("particles", Achats.monnaie("upgradeLevels", id));
        for (var d : idle.core.darkmatter.DarkUpgrades.DEFAULT) {
            String expected = switch (d.branch()) {
                case PARTICLES -> "particles";
                case ATOMS -> "atoms";
                case SIZE -> null;
                case DARK_MATTER -> "darkMatter";
            };
            assertEquals(expected, Achats.monnaie("darkUpgradeLevels", d.id()), "l'arbre se paie selon sa branche : " + d.id());
        }
        assertEquals("atoms", Achats.monnaie("ownedAutomations", idle.core.upgrades.Automations.DEFAULT.get(0).id()), "les automatismes se paient en atomes");
    }

    @Test
    void lesChosesSeNommentEnFrancais() {
        assertEquals("Particules", Noms.cle("particles"));
        assertEquals("Plan : Valeur", Noms.cle("planeLevel0"));
        assertEquals("Graine : " + Game.SeedUpgrade.ASTRAL_LIGHT.label(), Noms.cle("seedUpgrade" + Game.SeedUpgrade.ASTRAL_LIGHT.ordinal()));
        assertEquals("Proton", Noms.chose("PROTON"));
        assertEquals("élément Z = 26", Noms.chose("26"));
        assertEquals("inconnue", Noms.cle("inconnue"), "une clé inconnue garde son nom");
        assertEquals("planeShards", Achats.monnaie("planeLevel2", null));
        assertEquals("mixShards", Achats.monnaie("seedUpgrade" + Game.SeedUpgrade.ROOT_BUBBLE.ordinal(), null));
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
        Photo.Nombre un = Photo.Nombre.of(1), deux = Photo.Nombre.of(2);
        assertEquals("l'explosion", BotAnalyste.cause(List.of(new Photo.Changement("explosions", null, un, deux, null, null))));
        assertEquals("le début du défi", BotAnalyste.cause(List.of(new Photo.Changement("activeChallenge", "x", null, null, "x", null))));
        assertEquals(null, BotAnalyste.cause(List.of(new Photo.Changement("particles", null, deux, un, null, null))), "un achat n'est pas une remise à zéro");
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
        assertTrue(Achats.achat("planeLevel2"));
        assertTrue(Achats.achat("lineYieldLevel"));
        assertTrue(Achats.achat("upgradeLevels"));
        assertTrue(Achats.achat("seedUpgrade3"));
        assertFalse(Achats.achat("pulses"), "un compteur d'impulsions n'est pas un achat");
        assertFalse(Achats.achat("linePhase"));
        assertFalse(Achats.achat("synthesisCount"));
        assertTrue(Achats.REGLAGES.containsKey("voidShare"), "un réglage se note comme un choix");
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

    @Test
    void lesDetecteursSignalentCeQuIlsVoient(@TempDir Path dir) throws Exception {
        BotAnalyste bot = new BotAnalyste(Bots.fresh(201), 201, 5, dir, 30);
        AnalyseJournal j = bot.journal;
        j.etsi.add(new AnalyseJournal.EtSi(0, 5, "Univers 5 : début", "Univers 5 : acte 1 : première explosion", 720, 2160, 3635));
        j.etsi.add(new AnalyseJournal.EtSi(0, 1, "Début", "Univers 1 : acte 1 : première explosion", 24000, 67800, 14255));
        j.defis.add(new AnalyseJournal.Defi(10, "Rouille", 25, true));
        j.defis.add(new AnalyseJournal.Defi(20, "Fuite", 900, true));
        for (int i = 0; i < 6; i++) j.ajoute(100, 1, AnalyseJournal.Sorte.SUCCES, "Succès obtenu : " + i);
        j.paliers.add(new AnalyseJournal.Palier(5000, 1, "Univers 1 : acte 2 : premier Big Bang", 4000, 3000));
        for (int i = 0; i < 25; i++) bot.coupures.merge("Couplage", 1, Integer::sum);
        List<AnalyseIllogismes.Illogisme> found = AnalyseIllogismes.detecter(bot);
        assertEquals(1, found.stream().filter(i -> i.numero() == 1).count(), "le joueur à la minute plus rapide, une fois");
        assertTrue(found.stream().anyMatch(i -> i.numero() == 4 && i.titre().equals("Rouille")));
        assertFalse(found.stream().anyMatch(i -> i.numero() == 4 && i.titre().equals("Fuite")), "un quart d'heure n'est pas trivial");
        assertTrue(found.stream().anyMatch(i -> i.numero() == 5));
        assertTrue(found.stream().anyMatch(i -> i.numero() == 7), "annoncé 2 000 s trop tôt sur 4 000");
        assertTrue(found.stream().anyMatch(i -> i.numero() == 8 && i.titre().startsWith("Couplage")));
        assertTrue(found.stream().anyMatch(i -> i.numero() == 3), "rien acheté : tout le catalogue est jamais acheté");
        AnalyseIllogismes.ecrire(found, dir);
        assertTrue(Files.readString(dir.resolve("illogismes.txt")).contains("4. Défi trivial : 1"));
    }

    @Test
    void deuxPartiesSeComparentPalierParPalier(@TempDir Path dir) throws Exception {
        Path a = dir.resolve("a"), b = dir.resolve("b");
        Files.createDirectories(a);
        Files.createDirectories(b);
        String head = "chrono,secondes,univers,palier,depuis_precedent_s,annonce_pour_s,ecart_s\n";
        Files.writeString(a.resolve("paliers.csv"), head + "01:00:00,3600,1,\"Univers 1 : acte 1 : première explosion\",3600,,\n"
                + "02:00:00,7200,1,\"Univers 1, encore\",3600,,\n");
        Files.writeString(b.resolve("paliers.csv"), head + "00:50:00,3000,1,\"Univers 1 : acte 1 : première explosion\",3000,,\n");
        assertEquals(List.of("a", "b, c", ""), AnalyseComparaison.cellules("a,\"b, c\","));
        List<AnalyseComparaison.Ligne> lignes = AnalyseComparaison.comparer(AnalyseComparaison.paliers(a), AnalyseComparaison.paliers(b));
        assertEquals(-600, lignes.get(0).ecart(), 1e-9, "B arrive dix minutes plus tôt");
        assertTrue(Double.isNaN(lignes.get(1).b()), "pas atteint dans B");
        AnalyseComparaison.ecrire(a, b, dir.resolve("sortie"));
        assertTrue(Files.readString(dir.resolve("sortie/comparaison.txt")).contains("1 paliers plus tôt dans B"));
        List<AnalyseGraines.Resume> r = AnalyseGraines.resumer(List.of(a, b));
        assertEquals(2, r.get(0).atteint());
        assertEquals(3300, r.get(0).mediane(), 1e-9);
        assertEquals(600.0 / 3300, r.get(0).dispersion(), 1e-9);
    }

    @Test
    void uneAmeliorationSeRetireDuJeu() {
        assertEquals(java.util.Set.of("atom_keep"), BotAnalyste.sans("atom_keep"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> BotAnalyste.sans("n_existe_pas"));
        Game game = Bots.game(new idle.core.GameState(), 7, java.util.Set.of("atom_keep"));
        game.start();
        assertTrue(game.costOf("atom_keep").log10() > 1e14, "Persistance garde son nom, à un prix impossible");
        for (int i = 0; i < 600; i++) game.tick(1);
        assertFalse(game.canBuy("atom_keep"));
    }
}
