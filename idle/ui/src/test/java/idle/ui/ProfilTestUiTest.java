package idle.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Le profil test UI (version 0.248), sans affichage : chaque visuel du jeu dans un onglet, les combinaisons tout croisées
 * et par paires, la partie préparée, chaque état joué sur elle, et la lecture automatique.
 */
class ProfilTestUiTest {
    /** Les onglets du jeu, par leur nom (sans charger la fenêtre). */
    private static final Set<String> TABS = Set.of("PARTICLES", "ATOMS", "AUTOMATION", "DARK_MATTER", "COLLIDER", "BIG_BANG", "UNIVERSE",
            "VOID", "HADRONS", "PROFILE", "ACHIEVEMENTS", "STATS", "SETTINGS", "SKINS");

    @Test
    void chaqueVisuelEstDansUnSeulOnglet() {
        Map<SkinVisual, String> seen = new EnumMap<>(SkinVisual.class);
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            assertTrue(TABS.contains(group.tab()), group.name() + " : un onglet du jeu");
            assertFalse(group.steps().isEmpty(), group.name() + " : des états");
            Set<String> labels = new HashSet<>();
            for (UiTestPlan.Step step : group.steps()) assertTrue(labels.add(step.label()), group.name() + " : « " + step.label() + " » deux fois");
            for (SkinVisual visual : group.visuals()) assertEquals(null, seen.put(visual, group.name()), visual + " dans deux onglets");
        }
        for (SkinVisual visual : SkinVisual.values()) assertTrue(seen.containsKey(visual), visual + " : dans aucun onglet");
    }

    @Test
    void toutCroiseDonneChaqueCombinaisonUneFois() {
        UiTestPlan.Group atoms = group("Atomes");
        UiTestPlan.Run run = new UiTestPlan.Run(atoms, UiTestPlan.axes(atoms), UiTestPlan.Mode.CROSS);
        // Version 0.287 : l'atome d'un côté, le tableau et sa page de l'autre, jamais croisés ensemble.
        int atom = UiTestPlan.options(SkinVisual.ATOM).size();
        long expected = atom + (long) UiTestPlan.options(SkinVisual.TABLE).size() * UiTestPlan.options(SkinVisual.TABLE_PAGE).size();
        assertEquals(2, run.parts());
        assertEquals(expected, run.combinations());
        assertEquals(expected * 2, run.total(), "chaque part a deux états");
        Set<List<Integer>> distinct = new HashSet<>();
        for (long i = 0; i < run.combinations(); i++) {
            List<Integer> choice = new ArrayList<>();
            for (int c : run.choice(i)) choice.add(c);
            choice.add(run.crosses(i, 0) ? 1 : 0);
            assertTrue(distinct.add(choice), "deux fois la même combinaison");
            assertFalse(run.skins(i).containsKey(SkinVisual.ATOM) && run.skins(i).containsKey(SkinVisual.TABLE), "l'atome croisé avec le tableau");
        }
        assertTrue(run.describe(0).contains("Classique"), "la première : tout au Classique");
        assertEquals("L'atome : Classique", run.describe(0), "la part de l'atome ne parle que de l'atome");
        assertEquals(atoms.steps().get(1), run.stepOf(1));
        assertEquals(0, run.combinationOf(1));
        assertEquals(1, run.combinationOf(2));
        assertNotNull(run.skins(1).get(SkinVisual.ATOM), "le premier visuel change le plus vite");
        // Les états du tableau viennent après tous ceux de l'atome.
        long firstTable = 2L * atom;
        assertEquals("Tableau vide", run.stepOf(firstTable).label());
        assertEquals(atom, run.combinationOf(firstTable));
        assertEquals(Set.of(SkinVisual.TABLE, SkinVisual.TABLE_PAGE), run.skins(atom).keySet());
        assertEquals(0, run.option(atom, SkinVisual.ATOM.name()), "l'atome reste d'origine pendant le tableau");
    }

    /** Version 0.287 : chaque sous-onglet nommé par un visuel existe dans son onglet, et chaque visuel s'y voit au moins une fois. */
    @Test
    void chaqueVisuelSeVoitDansUnSousOngletDeSonOnglet() {
        assertEquals("Atome", UiTestPlan.sceneOf(group("Atomes").steps().get(1)), "le dernier sous-onglet choisi");
        assertEquals("dimension 4", UiTestPlan.sceneOf(group("Dimensions").steps().stream()
                .filter(step -> UiTestPlan.sceneOf(step).equals("dimension 4")).findFirst().orElseThrow()));
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            Set<String> scenes = new HashSet<>();
            for (UiTestPlan.Step step : group.steps()) scenes.add(UiTestPlan.sceneOf(step));
            Set<String> heavy = new HashSet<>();
            for (UiTestPlan.Step step : group.heaviest()) heavy.add(UiTestPlan.sceneOf(step));
            for (SkinVisual visual : group.visuals()) {
                for (String scene : UiTestPlan.scenes(visual)) {
                    assertTrue(scenes.contains(scene), group.name() + " : " + visual + " sur « " + scene + " », qu'aucun état ne montre");
                }
                UiTestPlan.Axis axis = UiTestPlan.Axis.of(visual);
                assertTrue(scenes.stream().anyMatch(scene -> UiTestPlan.shows(scene, axis)), visual + " ne se voit jamais");
                assertTrue(heavy.stream().anyMatch(scene -> UiTestPlan.shows(scene, axis)), visual + " ne se voit jamais à la charge maximale");
            }
        }
        UiTestPlan.Axis theme = new UiTestPlan.Axis(UiTestPlan.THEME, "Thème", List.of("Sombre", "Clair"));
        assertTrue(UiTestPlan.shows("Tableau périodique", theme), "un réglage se voit partout");
        assertFalse(UiTestPlan.shows("Tableau périodique", UiTestPlan.Axis.of(SkinVisual.ATOM)));
        assertTrue(UiTestPlan.shows("", UiTestPlan.Axis.of(SkinVisual.ATOM)), "un état sans sous-onglet montre tout");
    }

    /** Version 0.288 : un sous-onglet sans visuel de l'onglet (les améliorations, les paliers) ne se passe plus : la fenêtre le couvre. */
    @Test
    void lesSousOngletsSansVisuelNeSePassentPas() {
        for (String name : List.of("Collisionneur", "Le vide", "Big Bang")) {
            for (boolean heaviest : new boolean[] {false, true}) {
                UiTestPlan.Group group = group(name);
                UiTestPlan.Run run = new UiTestPlan.Run(group, UiTestPlan.axes(group), UiTestPlan.Mode.PAIRS, heaviest);
                assertTrue(run.steps().size() < group.steps(heaviest).size(), name);
                for (UiTestPlan.Step step : run.steps()) assertNotEquals("Améliorations", UiTestPlan.sceneOf(step), name + " : " + step.label());
            }
        }
        assertTrue(new UiTestPlan.Run(group("Hadrons"), List.of(), UiTestPlan.Mode.CROSS).steps().stream()
                .noneMatch(step -> UiTestPlan.sceneOf(step).equals("Paliers")), "même sans visuel coché : les paliers des hadrons non plus");
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            assertFalse(UiTestPlan.Run.shown(group, false).isEmpty(), group.name());
            assertFalse(UiTestPlan.Run.shown(group, true).isEmpty(), group.name());
        }
        // Les onglets dont les visuels se voient partout gardent tous leurs états.
        for (String name : List.of("Fenêtre", "Particules", "Graine", "Profil", "Statistiques")) {
            UiTestPlan.Group group = group(name);
            assertEquals(group.steps(), UiTestPlan.Run.shown(group, false), name);
        }
    }

    @Test
    void sansVisuelCocheUneSeuleCombinaisonToutAuClassique() {
        UiTestPlan.Run run = new UiTestPlan.Run(group("Particules"), List.of(), UiTestPlan.Mode.CROSS);
        assertEquals(1, run.combinations());
        assertEquals("Tout au Classique", run.describe(0));
        assertEquals(1, new UiTestPlan.Run(group("Particules"), List.of(), UiTestPlan.Mode.PAIRS).combinations());
    }

    @Test
    void lesTresGrandsCroisementsSeComptentSansDeborder() {
        UiTestPlan.Group bang = group("Big Bang");
        UiTestPlan.Run run = new UiTestPlan.Run(bang, UiTestPlan.axes(bang), UiTestPlan.Mode.CROSS);
        assertTrue(run.combinations() > 100_000L, "des centaines de milliers : " + run.combinations());
        assertTrue(run.total() > run.combinations());
        // La dernière combinaison se calcule comme la première, sans rien garder en mémoire : le dernier skin de sa part.
        long lastIndex = run.combinations() - 1;
        while (run.skins(lastIndex).isEmpty()) lastIndex--; // les parts sans visuel (Paliers, Améliorations) : une combinaison chacune
        Map<SkinVisual, Skin> last = run.skins(lastIndex);
        assertFalse(last.isEmpty());
        for (Map.Entry<SkinVisual, Skin> entry : last.entrySet()) {
            List<Skin> options = UiTestPlan.options(entry.getKey());
            assertEquals(options.get(options.size() - 1), entry.getValue(), "le dernier skin de " + entry.getKey());
        }
        double uncut = 1;
        for (SkinVisual visual : bang.visuals()) uncut *= UiTestPlan.options(visual).size();
        assertTrue(run.combinations() < uncut, "moins qu'en croisant aussi les sous-onglets entre eux");
    }

    @Test
    void parPairesChaqueSkinCroiseChaqueAutre() {
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            UiTestPlan.Run run = new UiTestPlan.Run(group, UiTestPlan.axes(group), UiTestPlan.Mode.PAIRS);
            int n = run.crossed.size();
            // Les combinaisons, rangées par part (les axes qui s'y croisent) ; dans chaque part, chaque paire de skins se voit.
            Map<List<Integer>, Set<String>> seen = new HashMap<>();
            for (long c = 0; c < run.combinations(); c++) {
                List<Integer> axes = new ArrayList<>();
                for (int i = 0; i < n; i++) if (run.crosses(c, i)) axes.add(i);
                int[] choice = run.choice(c);
                Set<String> pairs = seen.computeIfAbsent(axes, key -> new HashSet<>());
                for (int i : axes) for (int j : axes) if (i < j) pairs.add(i + ":" + choice[i] + "/" + j + ":" + choice[j]);
            }
            assertEquals(run.parts(), seen.size(), group.name());
            for (UiTestPlan.Step step : run.steps()) {
                List<Integer> axes = new ArrayList<>();
                for (int i = 0; i < n; i++) if (UiTestPlan.shows(UiTestPlan.sceneOf(step), run.crossed.get(i))) axes.add(i);
                assertTrue(seen.containsKey(axes), group.name() + " : « " + step.label() + " » a sa part");
                int wanted = 0;
                for (int i : axes) for (int j : axes) if (i < j) wanted += run.crossed.get(i).options().size() * run.crossed.get(j).options().size();
                assertEquals(wanted, seen.get(axes).size(), group.name() + " : chaque paire de skins vue dans « " + step.label() + " »");
            }
            UiTestPlan.Run cross = new UiTestPlan.Run(group, UiTestPlan.axes(group), UiTestPlan.Mode.CROSS);
            assertTrue(run.combinations() <= cross.combinations(), group.name());
        }
        UiTestPlan.Group seed = group("Graine");
        long pairs = new UiTestPlan.Run(seed, UiTestPlan.axes(seed), UiTestPlan.Mode.PAIRS).combinations();
        assertTrue(pairs < 200, "la graine par paires : quelques dizaines de combinaisons, pas des millions : " + pairs);
        assertEquals(pairs, new UiTestPlan.Run(seed, UiTestPlan.axes(seed), UiTestPlan.Mode.PAIRS).combinations(), "le même jeu à chaque fois");
    }

    /** Une fenêtre factice : les changements de la partie passent pour de vrai, le reste est noté. */
    private static final class Recorder implements UiTestPlan.Driver {
        final TestActions actions;
        final List<String> done = new ArrayList<>();

        Recorder(Game game) { actions = new TestActions(game); }

        @Override public void tab(String tab) { assertTrue(TABS.contains(tab), tab); done.add("onglet " + tab); }
        @Override public boolean subTab(String label) { done.add("sous-onglet " + label); return true; }
        @Override public boolean subTabAt(int index) { done.add("sous-onglet n° " + index); return true; }
        @Override public void act(Consumer<TestActions> change) { change.accept(actions); }
        @Override public void hook(Consumer<TestTools.Hooks> animation) { animation.accept(new TestTools.Hooks() { }); done.add("animation"); }
        @Override public void dimension(int number) { done.add("dimension " + number); }
        @Override public void toast(String text) { done.add("notification"); }
        @Override public void help() { done.add("aide"); }
        @Override public void ending() { done.add("fin"); }
        @Override public void startButton() { done.add("départ"); }
        @Override public void particleSpeed(double log) { done.add("vitesse " + log); }
    }

    @Test
    void laPartiePrepareeOuvreToutEtChaqueEtatSeJoue() {
        Game game = new Game(7);
        UiTestPlan.prepare(game);
        assertTrue(game.isStarted(), "commencée");
        assertTrue(game.isColliderUnlocked(), "le collisionneur");
        assertTrue(game.bigBangs() >= 1, "un Big Bang");
        assertTrue(game.isUniverseFormed(), "l'univers formé");
        assertEquals(Game.MAX_DIMENSIONS, game.dimensionsUnlocked(), "les dix dimensions");
        for (Skin skin : Skin.values()) assertTrue(game.hasSkin(skin), skin + " débloqué");
        Recorder driver = new Recorder(game);
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            for (UiTestPlan.Step step : group.steps()) {
                assertDoesNotThrow(() -> step.action().accept(driver), group.name() + " : " + step.label());
            }
            // Chaque skin de l'onglet se choisit (tout est débloqué).
            for (SkinVisual visual : group.visuals()) {
                for (Skin skin : UiTestPlan.options(visual)) assertTrue(game.chooseSkin(visual, skin), visual + " : " + skin);
            }
        }
        assertTrue(driver.done.contains("dimension 10") && driver.done.contains("fin") && driver.done.contains("départ"));
        assertTrue(driver.done.contains("sous-onglet Graine d'univers"));
    }

    /**
     * Version 0.286 : la charge maximale. Chaque onglet en a ses états ; ils se jouent sans erreur et mettent la partie au
     * plus chargé (l'atome à 118 orbes et 40 copies, 30 générateurs au dernier palier de vitesse, plus d'univers que la
     * vue n'en dessine…) ; le passage les prend à la place de ceux d'habitude et le dit.
     */
    @Test
    void laChargeMaximaleChargeChaqueOnglet() {
        Game game = new Game(11);
        UiTestPlan.prepare(game);
        Recorder driver = new Recorder(game);
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            assertFalse(group.heaviest().isEmpty(), group.name());
            for (UiTestPlan.Step step : group.heaviest()) {
                assertDoesNotThrow(() -> step.action().accept(driver), group.name() + " à la charge maximale : " + step.label());
            }
            UiTestPlan.Run heavy = new UiTestPlan.Run(group, UiTestPlan.axes(group), UiTestPlan.Mode.PAIRS, true);
            assertEquals(Set.copyOf(UiTestPlan.Run.shown(group, true)), Set.copyOf(heavy.steps()));
            assertTrue(group.heaviest().containsAll(heavy.steps()));
            if (heavy.parts() == 1) assertEquals(heavy.combinations() * heavy.steps().size(), heavy.total());
            assertTrue(heavy.modeLabel().endsWith("charge maximale"), heavy.modeLabel());
            assertEquals(UiTestPlan.Mode.PAIRS.label, new UiTestPlan.Run(group, UiTestPlan.axes(group), UiTestPlan.Mode.PAIRS).modeLabel());
        }
        for (String name : List.of("Particules", "Atomes", "Matière noire", "Collisionneur", "Hadrons", "Le vide", "Big Bang", "Univers",
                "Graine", "Dimensions")) {
            assertNotEquals(group(name).steps(), group(name).heaviest(), name + " a ses états de charge maximale");
        }
        assertTrue(game.generatorCount() >= GameAppBase.MAX_VISIBLE_GENERATORS, "au moins 30 générateurs, tous montrés : " + game.generatorCount());
        assertTrue(driver.done.contains("vitesse " + UiTestPlan.MAX_SPEED_LOG), "la vitesse des particules forcée");
        assertEquals(idle.ui.particles.SpeedStages.COUNT - 1, idle.ui.particles.SpeedStages.stage(UiTestPlan.MAX_SPEED_LOG), "le dernier palier");
        assertTrue(game.state().atoms().compareTo(idle.core.math.BigNum.of(1, 12)) >= 0, "assez d'atomes pour les 40 copies");
        assertTrue(game.universes() >= idle.ui.universe.UniverseView.MAX_BUBBLES, "plus d'univers que de bulles dessinées");
        assertTrue(game.isVoidHoleOpen(), "le trou noir du vide");
        assertEquals(UiTestPlan.TOASTS, driver.done.stream().filter("notification"::equals).count(), "les notifications au maximum");
    }

    @Test
    void lesEtatsMettentLaPartieDansLEtatDit() {
        Game game = new Game(3);
        UiTestPlan.prepare(game);
        Recorder driver = new Recorder(game);
        for (UiTestPlan.Step step : group("Particules").steps()) {
            step.action().accept(driver);
            if (Character.isDigit(step.label().charAt(0))) {
                int wanted = Integer.parseInt(step.label().split(" ")[0]);
                assertEquals(Math.min(wanted, game.maxGeneratorCount()), game.generatorCount(), step.label());
            }
        }
        group("Succès").steps().get(0).action().accept(driver);
        assertTrue(game.state().achievements().isEmpty(), "aucun succès");
        group("Succès").steps().get(1).action().accept(driver);
        assertEquals(game.achievements().size(), game.state().achievements().size(), "tous les succès");
        for (UiTestPlan.Step step : group("Graine").steps()) {
            step.action().accept(driver);
            if (step.label().equals("Graine pas encore germée")) assertEquals(1, game.seedStage());
            if (step.label().equals("Arbre")) assertEquals(Math.min(Game.SEED_STAGES, game.seedStageCap()), game.seedStage());
            if (step.label().equals("Arrosoir en main")) assertTrue(game.isWatering());
        }
    }

    @Test
    void laLectureAvanceVerifieUneFoisParEtapeEtSArreteAuBout() {
        UiTestPlan.Group success = group("Succès");
        UiTestRun run = new UiTestRun(new UiTestPlan.Run(success, UiTestPlan.axes(success), UiTestPlan.Mode.CROSS));
        long total = run.run().total();
        run.setSecondsPerStep(1);
        assertFalse(run.frame(5), "à l'arrêt, rien n'avance");
        run.setPlaying(true);
        assertFalse(run.frame(0.5));
        assertFalse(run.takeCheck(), "trop tôt pour vérifier");
        assertFalse(run.frame(0.35));
        assertTrue(run.takeCheck(), "aux quatre cinquièmes");
        assertFalse(run.takeCheck(), "une seule fois");
        assertTrue(run.frame(0.2), "l'étape suivante");
        assertEquals(1, run.step());
        while (run.playing()) run.frame(1.01);
        assertEquals(total - 1, run.step(), "jusqu'à la dernière");
        assertFalse(run.next());
        assertTrue(run.previous());
        assertTrue(run.goTo(0));
        assertFalse(run.previous());
        assertFalse(run.goTo(total));
        run.setSecondsPerStep(100);
        assertEquals(UiTestRun.SLOWEST, run.secondsPerStep());
        run.setSecondsPerStep(Double.NaN);
        assertEquals(1, run.secondsPerStep());
    }

    @Test
    void lesErreursSeRelevantUneFoisEtSeCopient() {
        UiTestPlan.Group atoms = group("Atomes");
        UiTestRun run = new UiTestRun(new UiTestPlan.Run(atoms, UiTestPlan.axes(atoms), UiTestPlan.Mode.CROSS));
        run.error("vue vide");
        run.error("vue vide");
        assertEquals(1, run.errorCount(), "la même à la même étape ne compte qu'une");
        run.next();
        run.error("vue vide");
        run.error(null);
        assertEquals(3, run.errorCount());
        String report = run.report();
        assertTrue(report.contains("Atomes") && report.contains("3 erreurs") && report.contains("Étape 2") && report.contains("erreur sans message"), report);
        assertTrue(report.contains(atoms.steps().get(1).label()), "l'état de l'erreur");
        run.clearErrors();
        assertEquals(0, run.errorCount());
        assertTrue(run.errors().isEmpty());
    }

    @Test
    void leTempsQuIlResteSeDitEnMots() {
        assertEquals("42 s", UiTestRun.duration(42));
        assertEquals("12 min", UiTestRun.duration(720));
        assertEquals("3 h 20", UiTestRun.duration(3 * 3600 + 20 * 60));
        assertEquals("5 j", UiTestRun.duration(5 * 86_400));
        assertEquals("12 ans", UiTestRun.duration(12 * 365 * 86_400.0));
        assertTrue(UiTestRun.duration(1e20).endsWith("ans"));
        UiTestPlan.Group bang = group("Big Bang");
        UiTestRun run = new UiTestRun(new UiTestPlan.Run(bang, UiTestPlan.axes(bang), UiTestPlan.Mode.CROSS));
        assertTrue(run.remaining().endsWith(" j"), "tout croisé, le Big Bang prend des jours (plus des années depuis 0.287) : " + run.remaining());
    }

    private static UiTestPlan.Group group(String name) {
        for (UiTestPlan.Group group : UiTestPlan.groups()) if (group.name().equals(name)) return group;
        throw new AssertionError(name);
    }

    /** Version 0.252 : ce qui touche l'onglet sans être à lui, une fois chacun, et jamais ce qu'il a déjà. */
    @Test
    void ceQuiToucheLOngletSAjouteSansDoublon() {
        for (UiTestPlan.Group group : UiTestPlan.groups()) {
            List<SkinVisual> around = UiTestPlan.around(group);
            for (SkinVisual visual : around) assertFalse(group.visuals().contains(visual), group.name() + " : " + visual + " déjà à lui");
            assertEquals(new HashSet<>(around).size(), around.size());
            for (SkinVisual visual : List.of(SkinVisual.CURSOR, SkinVisual.CARD, SkinVisual.AMBIENT, SkinVisual.TABS)) {
                assertTrue(around.contains(visual) || group.visuals().contains(visual), group.name() + " : " + visual + " le touche");
            }
        }
    }

    /** Version 0.252 : un axe de réglage (le thème) se croise comme un visuel, et ce qui n'est pas croisé reste d'origine. */
    @Test
    void unReglageSeCroiseCommeUnVisuel() {
        UiTestPlan.Group atoms = group("Atomes");
        UiTestPlan.Axis theme = new UiTestPlan.Axis(UiTestPlan.THEME, "Thème", List.of("Sombre", "Clair", "Sépia"));
        List<UiTestPlan.Axis> axes = new ArrayList<>(UiTestPlan.axes(atoms));
        axes.add(theme);
        UiTestPlan.Run run = new UiTestPlan.Run(atoms, axes, UiTestPlan.Mode.CROSS);
        long withoutTheme = new UiTestPlan.Run(atoms, UiTestPlan.axes(atoms), UiTestPlan.Mode.CROSS).combinations();
        assertEquals(withoutTheme * 3, run.combinations(), "trois thèmes : trois fois plus de combinaisons, dans chaque part");
        Set<Integer> themes = new HashSet<>();
        for (long c = 0; c < run.combinations(); c++) themes.add(run.option(c, UiTestPlan.THEME));
        assertEquals(Set.of(0, 1, 2), themes, "chaque thème passe");
        assertEquals(0, run.option(5, UiTestPlan.NOTATION), "la notation, pas croisée, reste d'origine");
        assertEquals(null, run.skin(0, SkinVisual.CURSOR), "un visuel pas croisé reste au Classique");
        assertTrue(run.describe(run.total() - 1).contains("Thème : Sépia"), run.describe(run.total() - 1));
        assertEquals(SkinVisual.ATOM, UiTestPlan.Axis.of(SkinVisual.ATOM).visual());
        assertEquals(null, theme.visual(), "un réglage n'est pas un visuel");
        assertEquals("Classique", UiTestPlan.Axis.of(SkinVisual.ATOM).options().get(0));
        UiTestPlan.Run pairs = new UiTestPlan.Run(atoms, axes, UiTestPlan.Mode.PAIRS);
        Set<String> seen = new HashSet<>();
        for (long c = 0; c < pairs.combinations(); c++) seen.add(pairs.option(c, SkinVisual.ATOM.name()) + "/" + pairs.option(c, UiTestPlan.THEME));
        assertTrue(seen.size() >= UiTestPlan.options(SkinVisual.ATOM).size() * 3, "par paires : chaque skin d'atome croise chaque thème");
        for (int skin = 0; skin < UiTestPlan.options(SkinVisual.ATOM).size(); skin++) {
            for (int t = 0; t < 3; t++) assertTrue(seen.contains(skin + "/" + t), skin + "/" + t);
        }
    }
}
