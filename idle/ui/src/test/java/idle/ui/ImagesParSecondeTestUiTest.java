package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Les images par seconde du profil test UI (version 0.253), sans affichage : une mesure par combinaison de skins, sur
 * tous ses états, la mise en place de chaque état à part, et une ligne par combinaison dans le fichier.
 */
class ImagesParSecondeTestUiTest {
    private static UiTestPlan.Group group(String name) {
        for (UiTestPlan.Group group : UiTestPlan.groups()) if (group.name().equals(name)) return group;
        throw new AssertionError(name);
    }

    /** Un passage sur les Atomes (quatre états par combinaison), la mesure envoyée dans une liste. */
    private static UiTestRun atoms(List<UiTestRun.Fps> sent) {
        UiTestPlan.Group atoms = group("Atomes");
        UiTestRun run = new UiTestRun(new UiTestPlan.Run(atoms, UiTestPlan.axes(atoms), UiTestPlan.Mode.CROSS));
        run.setFpsSink(sent::add);
        return run;
    }

    /** Montre l'état en cours {@code seconds} secondes, une image toutes les {@code frame} secondes, chacune avec ce calcul. */
    private static void show(UiTestRun run, double seconds, double frame, double work) {
        for (double t = 0; t < seconds - 1e-9; t += frame) run.measure(frame, work);
    }

    @Test
    void uneMesureParCombinaisonSurTousSesEtats() {
        List<UiTestRun.Fps> sent = new ArrayList<>();
        UiTestRun run = atoms(sent);
        int states = run.run().group.steps().size();
        for (int s = 0; s < states; s++) {
            // Le deuxième état est lent : une image toutes les 50 ms au lieu de 1/60 s.
            show(run, 1, s == 1 ? 0.05 : 1 / 60.0, 0.002);
            if (s < states - 1) {
                run.next();
                assertTrue(sent.isEmpty(), "toujours la même combinaison : rien d'écrit");
            }
        }
        run.next();
        assertEquals(1, sent.size(), "la combinaison suivante : une ligne pour la première");
        UiTestRun.Fps fps = sent.get(0);
        assertEquals(0, fps.combination());
        assertEquals(states, fps.states(), "tous ses états mesurés");
        assertEquals(run.run().group.steps().get(1).label(), fps.slowestState(), "la pire image dans l'état lent");
        assertEquals(50, fps.worstMs(), 1e-6);
        assertEquals(20, fps.lowest(), 1e-6);
        assertTrue(fps.average() > 20 && fps.average() < 60, "entre les deux : " + fps.average());
        assertEquals(2, fps.workMs(), 1e-6);
        assertEquals("Atomes", fps.group());
        assertEquals(run.run().describeCombination(0), fps.skins());
        assertEquals(fps, run.lastFps());
        assertEquals(fps, run.slowestFps());
    }

    @Test
    void laMiseEnPlaceDeChaqueEtatNeCompteQuAPart() {
        List<UiTestRun.Fps> sent = new ArrayList<>();
        UiTestRun run = atoms(sent);
        // La première image d'un état est longue (la mise en place), le reste à 60 images par seconde.
        run.measure(0.2, 0.15);
        show(run, 1, 1 / 60.0, 0.001);
        run.goTo(run.run().group.steps().size());
        UiTestRun.Fps fps = sent.get(0);
        assertEquals(200, fps.setupMs(), 1e-6, "la mise en place : la pire image des premières secondes");
        assertEquals(60, fps.lowest(), 0.5, "elle ne compte pas dans le plus bas");
        assertEquals(60, fps.average(), 0.5);
        assertEquals(1, fps.states());
    }

    @Test
    void laPlusLenteEtLaFinDeSeance() {
        List<UiTestRun.Fps> sent = new ArrayList<>();
        UiTestRun run = atoms(sent);
        int states = run.run().group.steps().size();
        assertNull(run.lastFps());
        show(run, 1, 1 / 60.0, 0);
        run.goTo(states);
        show(run, 1, 1 / 30.0, 0);
        run.goTo(2L * states);
        show(run, 1, 1 / 50.0, 0);
        assertEquals(2, sent.size());
        assertEquals(1, run.slowestFps().combination(), "la plus lente : celle à 30");
        run.flushFps();
        assertEquals(3, sent.size(), "la fin de séance écrit celle en cours");
        run.flushFps();
        assertEquals(3, sent.size(), "une seule fois");
        assertEquals(3, run.fpsLog().size());
        assertEquals(60, run.liveFps() > 0 ? 60 : 0, "le moment présent se mesure");
        // Changer de passage (un autre onglet) termine aussi la mesure en cours.
        show(run, 1, 1 / 60.0, 0);
        run.setRun(new UiTestPlan.Run(group("Succès"), UiTestPlan.axes(group("Succès")), UiTestPlan.Mode.CROSS));
        assertEquals(4, sent.size());
    }

    @Test
    void leFichierAUneLigneParCombinaisonEtSonEnTeteUneFois() throws Exception {
        Path folder = Files.createTempDirectory("idle-fps");
        Path file = folder.resolve("sous-dossier").resolve(UiTestFpsFile.NAME);
        UiTestFpsFile out = new UiTestFpsFile(file, () -> 60);
        UiTestRun.Fps fps = new UiTestRun.Fps(4, "Atomes", "Tout croisé", "Atome : Craie ; Tableau : Classique", 4, "Nouvel atome",
                240, 4.0, 59.5, 41.2, 24.3, 2.25, 180.0);
        out.write(fps);
        out.write(fps);
        assertEquals("", out.problem());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(3, lines.size(), "l'en-tête une fois, puis une ligne par combinaison");
        assertTrue(lines.get(0).endsWith(UiTestFpsFile.HEADER));
        String line = UiTestFpsFile.line(fps, LocalDateTime.of(2026, 11, 17, 23, 30), 60);
        assertEquals("2026-11-17 23:30:00;" + Version.CURRENT + ";Atomes;Tout croisé;5;Atome : Craie , Tableau : Classique;4;Nouvel atome;240;"
                + "4,00;59,5;41,2;24,3;2,25;180,0;60", line);
        assertEquals(UiTestFpsFile.HEADER.split(";").length, line.split(";").length, "autant de colonnes que l'en-tête");
        // Un dossier qu'on ne peut pas créer : l'erreur est gardée, pas lancée.
        Path blocked = folder.resolve("un-fichier");
        Files.writeString(blocked, "x");
        UiTestFpsFile nowhere = new UiTestFpsFile(blocked.resolve(UiTestFpsFile.NAME), () -> 60);
        nowhere.write(fps);
        assertTrue(!nowhere.problem().isEmpty());
    }
}
