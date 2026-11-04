import static org.junit.jupiter.api.Assertions.*;

import idle.core.Game;
import idle.core.sky.Cosmos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Campagne de validation des 24 comportements de joueur.
 *
 * Deux origines indépendantes : la partie neuve et la sauvegarde avant le
 * huitième Big Bang. Le temps de décision est suffisamment fin pour ne pas
 * court-circuiter les automations de matière (rattrapage toutes les 5 s).
 *
 * Attention : certains profils sont volontairement peu attentifs. L'objectif
 * de ce premier passage est l'absence de crash, la continuité des sauvegardes,
 * des actions réelles et le bilan de progression pour chaque profil. Une
 * victoire dans un délai fixe n'est PAS un invariant pour ces profils.
 *
 * Version 0.172 : chaque profil est un test à lui (une ligne par profil dans le rapport), et les tests de la classe
 * se jouent en même temps, un par cœur ({@code @Execution(CONCURRENT)} sur chaque méthode, réglages dans
 * {@code junit-platform.properties}) : la campagne prend le temps du profil le plus long, pas la somme des vingt-quatre.
 * Chaque profil a sa partie, sa graine et son dossier ; le moteur ne partage rien entre deux parties, et la classe
 * suivante ne commence qu'une fois toutes ces parties finies (l'annotation est sur les méthodes, pas sur la classe :
 * sur la classe, elle la ferait courir en même temps que les autres classes, dont AnalyseTest, qui règle un état
 * partagé des bots). Les lignes PROFIL24_* sont les mêmes qu'avant, dans un autre ordre.
 */
class Profiles24CampaignTest {
    // Un segment n'est utilisé qu'en CI ; par défaut, les 24 profils sont testés.
    private static final int FIRST = Integer.getInteger("idle.testProfilesStart", 1);
    private static final int LAST = Integer.getInteger("idle.testProfilesEnd", 24);

    /** Les victoires en 16 h, comptées par les tests avancés pour le bilan de fin de classe. */
    private static final AtomicInteger VICTOIRES = new AtomicInteger();
    private static final AtomicInteger AVANCES = new AtomicInteger();

    private static boolean selected(IdleProfiles24.Profile p) {
        return p.id() >= FIRST && p.id() <= LAST;
    }

    private static void validateRange() {
        assertTrue(FIRST >= 1 && LAST <= 24 && FIRST <= LAST,
                "Bornes de campagne invalides : " + FIRST + "-" + LAST);
        assertEquals(24, IdleProfiles24.PROFILES.length,
                "Le nombre de profils a changé : mettre à jour la matrice CI");
    }

    /** Les profils du segment, dans l'ordre ; le segment doit être couvert en entier. */
    private static List<IdleProfiles24.Profile> profils() {
        validateRange();
        List<IdleProfiles24.Profile> liste = Stream.of(IdleProfiles24.PROFILES).filter(Profiles24CampaignTest::selected).toList();
        assertEquals(LAST - FIRST + 1, liste.size(), "Segment incomplet : " + FIRST + "-" + LAST);
        for (int id = FIRST; id <= LAST; id++) {
            int cherche = id;
            assertTrue(liste.stream().anyMatch(p -> p.id() == cherche), "Profil omis du segment : " + id);
        }
        return liste;
    }

    private static Game fresh(long seed) {
        return Bots.fresh(seed);
    }

    private static Game late(long seed) throws Exception {
        return Bots.load(Path.of("sauvegardes/reference/checkpoint_before_eighth.save"), seed);
    }

    /** Le nom du CSV qu'écrit un profil : il porte l'étape (propriété idle.stage, « late » par défaut). */
    private static String history(IdleProfiles24.Profile p) {
        return String.format(Locale.ROOT, "%02d_%s_%s.csv", p.id(), p.name(), System.getProperty("idle.stage", "late"));
    }

    private static String nom(IdleProfiles24.Profile p) {
        return String.format(Locale.ROOT, "%02d %s", p.id(), p.name());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void profilsPrestigeEtEpargneNeSabotentPasLeCosmos() throws Exception {
        Game g = late(42);
        g.state().setBigBangs(8);
        assertFalse(IdleProfiles24.allowExtraPrestige(g),
                "Le huitieme Big Bang marque la fin des explosions supplementaires");
        Game sky = fresh(43);
        sky.state().setBigBangs(5);
        assertFalse(IdleProfiles24.allowExtraPrestige(sky),
                "Le ciel incomplet du sixieme Big Bang doit etre preserve");
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void cadenceDeVingtSecondesRespecteeMalgreUnTickDeSoixante(@TempDir Path out) throws Exception {
        Game rapide = fresh(101);
        IdleProfiles24 speedy = new IdleProfiles24(
                rapide, IdleProfiles24.PROFILES[0], 101, 60);
        speedy.execute(0.05, out.resolve("rapide"));
        assertEquals(9, speedy.actions,
                "Le speedrunner doit pouvoir décider 3 fois par tick de 60 s");
        Game absent = fresh(102);
        IdleProfiles24 afk = new IdleProfiles24(
                absent, IdleProfiles24.PROFILES[16], 102, 60);
        afk.execute(0.05, out.resolve("afk"));
        assertEquals(1, afk.actions,
                "Le profil AFK doit garder sa cadence d'une fois par heure");
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void aucuneExplosionDActionsAuRetourDuneAbsence(@TempDir Path out) throws Exception {
        Game game = fresh(103);
        IdleProfiles24 mobile = new IdleProfiles24(
                game, IdleProfiles24.PROFILES[18], 103, 60);
        mobile.execute(2, out.resolve("mobile"));
        assertTrue(mobile.actions > 0,
                "Le profil intermittent doit agir quand il est en ligne");
        assertTrue(mobile.actions <= 12,
                "Les decisions manquees hors ligne ne doivent pas se rejouer d'un coup");
    }

    /** Deux heures depuis une partie neuve, pour chaque profil du segment : un test par profil. */
    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    Stream<DynamicTest> tousLesProfilsProgressentDepuisLeDebut(@TempDir Path out) {
        return profils().stream().map(p -> DynamicTest.dynamicTest("depuis le début : " + nom(p), () -> depuisLeDebut(p, out)));
    }

    private static void depuisLeDebut(IdleProfiles24.Profile p, Path out) throws Exception {
        long seed = 200 + p.id();
        Game g = fresh(seed);
        var bot = new IdleProfiles24(g, p, seed, 60);
        bot.execute(2, out.resolve("fresh").resolve(p.name()));
        assertTrue(g.state().started(), p.name() + " : partie non demarree");
        assertTrue(g.state().timePlayed() > 0, p.name() + " : temps de jeu immobile");
        assertTrue(bot.actions > 0, p.name() + " : aucun tour de decision");
        Path csv = out.resolve("fresh").resolve(p.name()).resolve(history(p));
        assertTrue(Files.size(csv) > 60, p.name() + " : bilan CSV vide");
        System.out.printf("PROFIL24_FRESH,%d,%s,actions=%d,fusions=%d,explosions=%d,bb=%d,heures=%.3f%n",
                p.id(), p.name(), bot.actions, g.state().stats().fusions(),
                g.state().explosions(), g.bigBangs(), g.state().timePlayed() / 3600);
    }

    /** Seize heures depuis la sauvegarde avant le huitième Big Bang, pour chaque profil du segment : un test par profil. */
    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    Stream<DynamicTest> tousLesProfilsJouentSeizeHeuresAvancees(@TempDir Path out) {
        return profils().stream().map(p -> DynamicTest.dynamicTest("seize heures avancées : " + nom(p), () -> seizeHeuresAvancees(p, out)));
    }

    private static void seizeHeuresAvancees(IdleProfiles24.Profile p, Path out) throws Exception {
        long seed = 300 + p.id();
        Game g = late(seed);
        int bbBefore = g.bigBangs();
        int bodiesBefore = g.bodiesMature();
        int cosmosBefore = g.cosmosFormed();
        var bot = new IdleProfiles24(g, p, seed, 60);
        bot.execute(16, out.resolve("late").resolve(p.name()));
        assertTrue(g.state().started(), p.name() + " : sauvegarde perdue");
        assertTrue(bot.actions > 0, p.name() + " : aucune decision en 16 h");
        boolean won = g.hasCosmos(Cosmos.UNIVERSE);
        // Former l'univers remet volontairement les compteurs a zero :
        // une victoire n'est pas une regression de Big Bangs/astres/cosmos.
        assertTrue(won || g.bigBangs() >= bbBefore,
                p.name() + " : Big Bangs perdus sans formation d'univers");
        assertTrue(won || g.bodiesMature() >= bodiesBefore || g.bigBangs() > bbBefore,
                p.name() + " : perte des astres sans nouveau Big Bang");
        assertTrue(won || g.cosmosFormed() >= cosmosBefore,
                p.name() + " : cosmos perdu sans formation d'univers");
        AVANCES.incrementAndGet();
        if (won) VICTOIRES.incrementAndGet();
        System.out.printf(
                "PROFIL24_LATE,%d,%s,actions=%d,bb=%d,astres=%d,cosmos=%d,victoire=%s,enligne=%.3f,offline=%.3f%n",
                p.id(), p.name(), bot.actions, g.bigBangs(), g.bodiesMature(),
                g.cosmosFormed(), won, bot.onlineSeconds / 3600,
                bot.creditedOfflineSeconds / 3600);
    }

    /** Le bilan des seize heures, une fois tous les profils joués (il clôturait la boucle avant la 0.172). */
    @AfterAll
    static void bilan() {
        System.out.printf("PROFIL24_BILAN,segment=%d-%d,valides=%d,victoiresEn16h=%d%n",
                FIRST, LAST, AVANCES.get(), VICTOIRES.get());
    }
}
