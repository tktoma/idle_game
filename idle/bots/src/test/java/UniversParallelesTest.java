import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Les univers en parallèle (version 0.170) : une tranche reprise d'un jalon continue exactement la partie jouée d'un
 * trait, la comparaison des jalons dit ce qui diffère, et des tranches jouées en même temps n'écrivent pas l'une chez
 * l'autre. L'enchaînement complet des univers est l'affaire de l'outil lui-même (il vérifie sa chaîne) et de la campagne
 * longue : il ne tient pas dans un test.
 */
class UniversParallelesTest {

    @Test
    void uneTrancheRepriseDUnJalonContinueLaMemePartie(@TempDir Path dir) throws Exception {
        // Une heure et demie d'un trait, contre une heure puis une demi-heure reprise du jalon : même sauvegarde, même mémoire.
        UniversParalleles.Tranche trait = UniversParalleles.tranche(null, dir.resolve("trait"), 1, 1, 20, 201, 1);
        assertFalse(UniversParalleles.avancer(trait, 1.5), "pas d'univers en une heure et demie");
        Path finTrait = dir.resolve("trait").resolve("fin.save");
        trait.ecrire(finTrait);

        UniversParalleles.Tranche premiere = UniversParalleles.tranche(null, dir.resolve("a"), 1, 1, 20, 201, 1);
        assertFalse(UniversParalleles.avancer(premiere, 1));
        Path jalon = dir.resolve("a").resolve(UniversParalleles.nomJalon(0));
        premiere.ecrire(jalon);
        assertTrue(Files.exists(AnalyseMemoire.fichier(jalon)), "la mémoire du bot à côté du jalon");

        UniversParalleles.Tranche suite = UniversParalleles.tranche(jalon, dir.resolve("b"), 1, 1, 20, 201, 1);
        assertEquals(premiere.wallSeconds, suite.wallSeconds, 1e-9, "le temps réel du bot reprend où il était");
        assertEquals(premiere.stepIndex, suite.stepIndex, "le compte des pas aussi");
        assertFalse(UniversParalleles.avancer(suite, 0.5));
        Path finSuite = dir.resolve("b").resolve("fin.save");
        suite.ecrire(finSuite);

        assertTrue(UniversParalleles.identiques(finTrait, finSuite), "sauvegarde et mémoire identiques :\n" + UniversParalleles.ecart(finTrait, finSuite));
        assertEquals(trait.g.state().timePlayed(), suite.g.state().timePlayed(), 1e-6);
    }

    @Test
    void laComparaisonDesJalonsDitCeQuiDiffere(@TempDir Path dir) throws Exception {
        UniversParalleles.Tranche a = UniversParalleles.tranche(null, dir, 1, 1, 20, 201, 1);
        UniversParalleles.avancer(a, 0.2);
        Path ja = dir.resolve("a.save");
        a.ecrire(ja);
        UniversParalleles.Tranche b = UniversParalleles.tranche(null, dir, 1, 1, 20, 202, 1);   // une autre graine
        UniversParalleles.avancer(b, 0.2);
        Path jb = dir.resolve("b.save");
        b.ecrire(jb);
        assertTrue(UniversParalleles.identiques(ja, ja));
        assertFalse(UniversParalleles.identiques(ja, jb));
        assertFalse(UniversParalleles.identiques(ja, dir.resolve("absent.save")), "un jalon qui manque n'est jamais identique");
        String ecart = UniversParalleles.ecart(ja, jb);
        assertTrue(ecart.contains("mémoire : "), "les hasards du bot diffèrent au moins : " + ecart);
        assertNull(UniversParalleles.jalonDepart(dir, 1), "la première tranche part d'une partie neuve");
        assertNull(UniversParalleles.jalonDepart(dir, 2), "sans jalon apres_univers_1 : rien d'où partir");
        Files.copy(ja, dir.resolve(UniversParalleles.nomJalon(1)));
        assertNull(UniversParalleles.jalonDepart(dir, 2), "une sauvegarde sans sa mémoire ne fait pas un jalon");
        Files.copy(AnalyseMemoire.fichier(ja), AnalyseMemoire.fichier(dir.resolve(UniversParalleles.nomJalon(1))));
        assertEquals(dir.resolve(UniversParalleles.nomJalon(1)), UniversParalleles.jalonDepart(dir, 2));
    }

    @Test
    void laSortieSeRepartitParFil() throws Exception {
        ByteArrayOutputStream a = new ByteArrayOutputStream(), b = new ByteArrayOutputStream();
        CountDownLatch pret = new CountDownLatch(2), fin = new CountDownLatch(2);
        Runnable[] taches = {
            () -> ecrire(a, "A", pret, fin),
            () -> ecrire(b, "B", pret, fin)
        };
        for (Runnable t : taches) new Thread(t).start();
        fin.await();
        assertEquals("A1\nA2\n", a.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"));
        assertEquals("B1\nB2\n", b.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    private static void ecrire(ByteArrayOutputStream cible, String nom, CountDownLatch pret, CountDownLatch fin) {
        UniversParalleles.SortieParFil.brancher(cible);
        try {
            pret.countDown();
            pret.await();
            System.out.println(nom + "1");
            System.out.println(nom + "2");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            UniversParalleles.SortieParFil.debrancher();
            fin.countDown();
        }
    }
}
