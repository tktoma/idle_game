package idle.ui;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Le plan du profil test UI (argument {@code --test-ui}, tâche Gradle {@code runTestUi}, version 0.248) : les visuels
 * du jeu rangés par onglet, et pour chaque onglet les états à montrer (un générateur, deux… une fusion, chaque
 * sous-onglet). Chaque combinaison de skins des visuels cochés passe par tous ces états, l'une après l'autre.
 *
 * <p>Deux façons de combiner : <b>tout croisé</b>, chaque combinaison possible (le produit des choix de chaque visuel,
 * Classique compris), énuméré à la demande sans rien garder en mémoire ; ou <b>par paires</b>, un jeu bien plus court où
 * chaque skin d'un visuel croise au moins une fois chaque skin de chaque autre visuel coché.
 *
 * <p>Le plan ne touche pas l'interface : il passe par un {@link Driver}, que le jeu fournit, et que les vérifications
 * remplacent.
 */
final class UiTestPlan {
    private UiTestPlan() {
    }

    /** Ce que les états demandent à l'interface. */
    interface Driver {
        /** Affiche l'onglet de ce nom ({@code PARTICLES}, {@code BIG_BANG}…). */
        void tab(String tab);

        /** Clique le sous-onglet visible de ce texte, dans l'onglet affiché. Faux s'il n'y est pas. */
        boolean subTab(String label);

        /** Clique le {@code index}-ième sous-onglet visible de l'onglet affiché (0 le premier). Faux s'il n'y en a pas tant. */
        boolean subTabAt(int index);

        /** Change la partie, sans gain : le nombre de générateurs, la graine… */
        void act(Consumer<TestActions> change);

        /** Joue une animation ou un son du jeu. */
        void hook(Consumer<TestTools.Hooks> animation);

        /** Ouvre la fenêtre de la dimension {@code number} (1 à 10). */
        void dimension(int number);

        void toast(String text);

        void help();

        void ending();

        void startButton();
    }

    /**
     * Un état à montrer : son nom, et s'il faut que le temps du jeu passe pendant qu'il est montré (des faisceaux qui
     * tournent, des particules qui naissent, des paires qui apparaissent). Ailleurs, la partie reste figée.
     */
    record Step(String label, boolean live, Consumer<Driver> action) {}

    /** Un onglet : son nom affiché, l'onglet du jeu, ses visuels et ses états. */
    record Group(String name, String tab, List<SkinVisual> visuals, List<Step> steps) {}

    /** Tout croisé, ou par paires. */
    enum Mode {
        CROSS("Tout croisé"), PAIRS("Par paires");

        final String label;

        Mode(String label) { this.label = label; }
    }

    private static Step still(String label, Consumer<Driver> action) { return new Step(label, false, action); }

    private static Step live(String label, Consumer<Driver> action) { return new Step(label, true, action); }

    /** Les onglets du profil test UI ; chaque visuel du jeu est dans un seul. */
    static List<Group> groups() {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("Fenêtre", "PARTICLES", List.of(SkinVisual.AMBIENT, SkinVisual.TABS, SkinVisual.CARD, SkinVisual.TAB_ICONS,
                SkinVisual.CARD_SIGNS, SkinVisual.TOOLTIP, SkinVisual.PANEL, SkinVisual.TOAST, SkinVisual.CURSOR, SkinVisual.START_BUTTON), List.of(
                still("Onglet Particules", d -> d.tab("PARTICLES")),
                still("Onglet Atomes", d -> d.tab("ATOMS")),
                still("Onglet Automatisation", d -> d.tab("AUTOMATION")),
                still("Onglet Matière noire", d -> d.tab("DARK_MATTER")),
                still("Une notification", d -> d.toast("Succès : un essai du profil test UI")),
                still("L'aide ouverte", Driver::help),
                still("L'écran de fin", Driver::ending),
                still("Le bouton de départ", Driver::startButton))));
        List<Step> particles = new ArrayList<>();
        for (int count : new int[] {1, 2, 5, 10, GameAppBase.MAX_VISIBLE_GENERATORS}) {
            particles.add(live(count + (count > 1 ? " générateurs" : " générateur"), d -> d.act(a -> a.setGenerators(count))));
        }
        particles.add(still("Fusion des générateurs", d -> {
            d.act(a -> a.setGenerators(10));
            d.hook(TestTools.Hooks::fusion);
        }));
        groups.add(new Group("Particules", "PARTICLES", List.of(SkinVisual.PARTICLE_FUSION, SkinVisual.AMBIENT_DUST,
                SkinVisual.GENERATOR_FUSION), particles));
        groups.add(new Group("Atomes", "ATOMS", List.of(SkinVisual.ATOM, SkinVisual.TABLE, SkinVisual.TABLE_PAGE), List.of(
                still("Atome", d -> d.subTab("Atome")),
                still("Nouvel atome", d -> {
                    d.subTab("Atome");
                    d.hook(TestTools.Hooks::newAtom);
                }),
                still("Tableau vide", d -> {
                    d.act(TestActions::emptyTable);
                    d.subTab("Tableau périodique");
                }),
                still("Tableau rempli", d -> {
                    d.act(a -> a.fillTable(true));
                    d.subTab("Tableau périodique");
                }))));
        groups.add(new Group("Matière noire", "DARK_MATTER", List.of(SkinVisual.DARK_BALL, SkinVisual.DARK_ANIMATION,
                SkinVisual.DARK_RULER, SkinVisual.DARK_TREE, SkinVisual.CHALLENGES), List.of(
                live("Petite boule", d -> {
                    d.act(a -> a.darkMatterSize(2));
                    d.subTab("Matière noire");
                }),
                live("Grosse boule", d -> {
                    d.act(a -> a.darkMatterSize(40));
                    d.subTab("Matière noire");
                }),
                still("Explosion", d -> {
                    d.subTab("Matière noire");
                    d.hook(TestTools.Hooks::explosion);
                }),
                still("Arbre vide", d -> {
                    d.act(TestActions::emptyTree);
                    d.subTab("Arbre");
                }),
                still("Arbre rempli", d -> {
                    d.act(TestActions::fillTree);
                    d.subTab("Arbre");
                }),
                still("Défis", d -> d.subTab("Défis")))));
        groups.add(new Group("Collisionneur", "COLLIDER", List.of(SkinVisual.COLLIDER_BEAM, SkinVisual.COLLIDER, SkinVisual.DETECTOR,
                SkinVisual.LIGHTNING, SkinVisual.SPARKS, SkinVisual.QUARK_FLIGHT), List.of(
                live("Énergie", d -> d.subTab("Énergie")),
                live("Faisceaux plus rapides", d -> {
                    d.act(a -> {
                        for (int i = 0; i < 3; i++) a.nextBeamSpeed();
                    });
                    d.subTab("Énergie");
                }),
                still("Chaîne", d -> d.subTab("Chaîne")),
                still("Améliorations", d -> d.subTab("Améliorations")))));
        groups.add(new Group("Hadrons", "HADRONS", List.of(SkinVisual.QUARKS, SkinVisual.GLUONS, SkinVisual.HADRON_BACKGROUND), List.of(
                live("Gluons", d -> d.subTab("Gluons")),
                still("Paliers", d -> d.subTab("Paliers")),
                live("Liaison", d -> d.subTab("Liaison")))));
        groups.add(new Group("Le vide", "VOID", List.of(SkinVisual.VOID_FOAM, SkinVisual.VOID_PAIRS, SkinVisual.VOID_HOLE), List.of(
                live("Paires", d -> d.subTab("Paires")),
                live("Trou noir plus massif", d -> {
                    d.act(TestActions::voidMass);
                    d.subTab("Paires");
                }),
                still("Améliorations", d -> d.subTab("Améliorations")))));
        List<Step> bang = new ArrayList<>();
        bang.add(still("Animation du Big Bang", d -> d.hook(TestTools.Hooks::bigBang)));
        for (String sub : List.of("Molécules", "Expansion de la matière", "États de la matière", "Assemblages", "Astres",
                "Systèmes solaires", "Univers", "Sondes", "Paliers", "Améliorations")) {
            boolean moving = sub.equals("Expansion de la matière") || sub.equals("Astres") || sub.equals("Univers");
            bang.add(new Step(sub, moving, d -> d.subTab(sub)));
        }
        groups.add(new Group("Big Bang", "BIG_BANG", List.of(SkinVisual.BIG_BANG, SkinVisual.BODY, SkinVisual.MOLECULE,
                SkinVisual.MOLECULE_PALETTE, SkinVisual.CLUMPS, SkinVisual.BLOCKS, SkinVisual.GAS, SkinVisual.SKY, SkinVisual.GALAXY,
                SkinVisual.GALAXY_CLUSTER, SkinVisual.COSMIC_WEB, SkinVisual.SYSTEM, SkinVisual.PROBE, SkinVisual.MATTER_SIGNS), bang));
        groups.add(new Group("Univers", "UNIVERSE", List.of(SkinVisual.UNIVERSE_BUBBLE, SkinVisual.UNIVERSE_BIRTH, SkinVisual.UNIVERSE_LINKS,
                SkinVisual.LAUNCH, SkinVisual.DIMENSIONS_MAP, SkinVisual.FINALE_FILL, SkinVisual.TRANSITION), List.of(
                live("Les univers", d -> d.subTab("Univers")),
                still("Naissance d'un univers", d -> d.hook(TestTools.Hooks::birth)),
                still("Lancement d'un univers", d -> d.hook(TestTools.Hooks::launch)),
                still("Transition entre univers", d -> d.hook(TestTools.Hooks::curtain)),
                still("Carte des dimensions", d -> {
                    d.act(TestActions::closeFinalChallenge);
                    d.subTab("Dimensions");
                }),
                still("Défi final rempli", d -> {
                    d.act(TestActions::reachAllFinalTiers);
                    d.subTab("Dimensions");
                }))));
        groups.add(new Group("Graine", "UNIVERSE", List.of(SkinVisual.TREE, SkinVisual.SEED, SkinVisual.WATERING_CAN, SkinVisual.SHELF,
                SkinVisual.HOSE, SkinVisual.CHRONO, SkinVisual.GROWTH_RING, SkinVisual.WEATHER, SkinVisual.SEASONS, SkinVisual.TREE_SEASON,
                SkinVisual.TREE_FANTASY, SkinVisual.TREE_GROUND, SkinVisual.TREE_BUBBLES, SkinVisual.TREE_BUBBLE_LINKS), List.of(
                live("Graine pas encore germée", d -> seed(d, 1)),
                live("Pousse", d -> seed(d, Game.SEED_STAGES / 2)),
                live("Arbre", d -> seed(d, Game.SEED_STAGES)),
                live("Arrosoir en main", d -> {
                    seed(d, Game.SEED_STAGES / 2);
                    d.act(a -> a.holdWateringCan(true));
                }),
                live("Beau temps", d -> {
                    seed(d, Game.SEED_STAGES);
                    d.act(a -> a.weather(Game.Weather.SUN));
                }),
                live("Orage", d -> {
                    seed(d, Game.SEED_STAGES);
                    d.act(a -> a.weather(Game.Weather.STORM));
                }),
                live("Chrono plein", d -> {
                    seed(d, Game.SEED_STAGES);
                    d.act(TestActions::fullChrono);
                }),
                live("Bulles de l'arbre", d -> {
                    seed(d, Game.SEED_STAGES);
                    d.act(TestActions::openFinalChallenge);
                }))));
        List<SkinVisual> dimensionVisuals = new ArrayList<>(Arrays.asList(SkinVisual.DIMENSIONS));
        dimensionVisuals.add(SkinVisual.DIMENSION_FRAME);
        dimensionVisuals.add(SkinVisual.DIMENSION_PALETTE);
        List<Step> dimensions = new ArrayList<>();
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            int number = d;
            dimensions.add(live("Dimension " + d + " : " + TestActions.DIMENSIONS[d - 1], driver -> driver.dimension(number)));
        }
        groups.add(new Group("Dimensions", "UNIVERSE", dimensionVisuals, dimensions));
        List<Step> profile = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int index = i;
            profile.add(still("Sous-onglet " + (i + 1), d -> d.subTabAt(index)));
        }
        groups.add(new Group("Profil", "PROFILE", List.of(SkinVisual.AVATAR_FRAME, SkinVisual.AVATAR_STYLE, SkinVisual.ALBUM), profile));
        groups.add(new Group("Succès", "ACHIEVEMENTS", List.of(SkinVisual.ACHIEVEMENT_TILE, SkinVisual.ACHIEVEMENT_LOCKED), List.of(
                still("Aucun succès", d -> d.act(TestActions::clearAchievements)),
                still("Tous les succès", d -> d.act(TestActions::grantAchievements)))));
        List<Step> stats = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int index = i;
            stats.add(still("Sous-onglet " + (i + 1), d -> d.subTabAt(index)));
        }
        groups.add(new Group("Statistiques", "STATS", List.of(SkinVisual.CHART, SkinVisual.CHART_PALETTE, SkinVisual.TIMELINE), stats));
        return groups;
    }

    /** La sous-page Graine, la graine à cette étape et l'arrosoir reposé. */
    private static void seed(Driver d, int stage) {
        d.act(a -> {
            a.seedStage(stage);
            a.holdWateringCan(false);
        });
        d.subTab("Graine d'univers");
    }

    /**
     * La partie du profil test UI : commencée, chaque acte ouvert (collisionneur, vide, hadrons, Big Bang, univers et
     * ses dix dimensions), de quoi remplir chaque vue (molécules, assemblages, astres, galaxie, hadrons), et tous les skins
     * débloqués. Aucun gain ne vient ensuite : le temps ne passe que sur les états qui bougent.
     */
    static void prepare(Game game) {
        TestActions act = new TestActions(game);
        // L'univers d'abord : le former remet la partie au début ; les actes s'ouvrent ensuite, dans l'univers suivant.
        act.openDimension(Game.MAX_DIMENSIONS);
        act.seedUpgrades(true);
        act.setGenerators(10);
        act.addAtoms(idle.core.math.BigNum.of(1000));
        act.unlockTable();
        act.explodeNow();
        act.addDarkMatter(1000);
        act.unlockCollider();
        act.colliderEnergy();
        act.colliderGenerators();
        act.collisionsAndFlavors();
        act.openVoid();
        act.openHadrons();
        act.addHadrons(50);
        act.addQuarks(30);
        act.bangsAtLeast(1);
        act.oneOfEachMolecule();
        act.gatherAll();
        act.formAllAssemblies();
        act.formAllBodies();
        act.formGalaxy();
        game.state().setSkinsOpen(true);
        game.testAllSkins(true);
    }

    /** Les choix d'un visuel : le Classique ({@code null}), puis chacun de ses skins. */
    static List<Skin> options(SkinVisual visual) {
        List<Skin> options = new ArrayList<>();
        options.add(null);
        for (Skin skin : Skin.values()) if (skin.visual() == visual) options.add(skin);
        return options;
    }

    /** L'axe du thème et celui de la notation (version 0.252) : des réglages, que la fenêtre fournit. */
    static final String THEME = "THEME", NOTATION = "NOTATION";

    /**
     * Une chose qui change l'allure d'un onglet, et ses choix, celui d'origine en premier : un visuel et ses skins (le
     * Classique d'abord), ou un réglage (le thème, la notation).
     */
    record Axis(String id, String label, List<String> options) {
        /** L'axe d'un visuel : le Classique, puis chacun de ses skins. */
        static Axis of(SkinVisual visual) {
            List<String> labels = new ArrayList<>();
            for (Skin skin : UiTestPlan.options(visual)) labels.add(skin == null ? "Classique" : skin.label());
            return new Axis(visual.name(), visual.label(), labels);
        }

        /** Le visuel de cet axe, ou {@code null} pour un réglage. */
        SkinVisual visual() {
            for (SkinVisual visual : SkinVisual.values()) if (visual.name().equals(id)) return visual;
            return null;
        }
    }

    /** Les axes des visuels de l'onglet. */
    static List<Axis> axes(Group group) {
        List<Axis> axes = new ArrayList<>();
        for (SkinVisual visual : group.visuals()) axes.add(Axis.of(visual));
        return axes;
    }

    /**
     * Ce qui touche aussi l'onglet sans être à lui (version 0.252) : le fond de la fenêtre, les onglets et leurs icônes, les
     * cartes et leurs signes, les info-bulles, le curseur, le cadre de l'avatar dans l'en-tête, les notifications et les
     * panneaux. Ceux que l'onglet a déjà n'y sont pas.
     */
    static List<SkinVisual> around(Group group) {
        List<SkinVisual> around = new ArrayList<>();
        for (SkinVisual visual : List.of(SkinVisual.AMBIENT, SkinVisual.TABS, SkinVisual.TAB_ICONS, SkinVisual.CARD, SkinVisual.CARD_SIGNS,
                SkinVisual.TOOLTIP, SkinVisual.CURSOR, SkinVisual.AVATAR_FRAME, SkinVisual.TOAST, SkinVisual.PANEL)) {
            if (!group.visuals().contains(visual)) around.add(visual);
        }
        return around;
    }

    /**
     * Un passage : un onglet, ses axes croisés (les autres restent à leur choix d'origine) et la façon de les combiner.
     * Les étapes sont numérotées de 0 à {@link #total()} − 1 : la combinaison {@code étape / états}, l'état
     * {@code étape % états}.
     */
    static final class Run {
        final Group group;
        final List<Axis> crossed;
        final Mode mode;
        private final int[] sizes;
        /** Le jeu par paires, calculé une fois ; {@code null} en tout croisé. */
        private final List<int[]> pairs;

        Run(Group group, List<Axis> crossed, Mode mode) {
            this.group = group;
            this.crossed = List.copyOf(crossed);
            this.mode = mode;
            sizes = new int[this.crossed.size()];
            for (int i = 0; i < sizes.length; i++) sizes[i] = Math.max(1, this.crossed.get(i).options().size());
            pairs = mode == Mode.PAIRS ? pairwise(sizes, 41) : null;
        }

        /** Le nombre de combinaisons ; {@link Long#MAX_VALUE} si le compte déborde. */
        long combinations() {
            if (pairs != null) return pairs.size();
            long count = 1;
            for (int size : sizes) {
                if (count > Long.MAX_VALUE / size) return Long.MAX_VALUE;
                count *= size;
            }
            return count;
        }

        /** Le nombre d'étapes : chaque combinaison par chaque état ; {@link Long#MAX_VALUE} si le compte déborde. */
        long total() {
            long combinations = combinations(), steps = group.steps().size();
            return combinations > Long.MAX_VALUE / steps ? Long.MAX_VALUE : combinations * steps;
        }

        /** Le choix de chaque axe croisé à la combinaison {@code index} (rang dans ses choix). */
        int[] choice(long index) {
            if (pairs != null) return pairs.get((int) index).clone();
            int[] choice = new int[sizes.length];
            // Le premier axe change le plus vite : chaque skin d'un visuel se voit tôt.
            for (int i = 0; i < sizes.length; i++) {
                choice[i] = (int) (index % sizes[i]);
                index /= sizes[i];
            }
            return choice;
        }

        /** Le choix de l'axe {@code id} à la combinaison {@code index} ; 0 (celui d'origine) s'il n'est pas croisé. */
        int option(long index, String id) {
            int[] choice = choice(index);
            for (int i = 0; i < choice.length; i++) if (crossed.get(i).id().equals(id)) return choice[i];
            return 0;
        }

        /** Le skin de ce visuel à la combinaison {@code index} ({@code null} pour le Classique, ou s'il n'est pas croisé). */
        Skin skin(long index, SkinVisual visual) { return options(visual).get(option(index, visual.name())); }

        /** Les skins de la combinaison {@code index} : chaque visuel croisé et son skin ({@code null} pour le Classique). */
        Map<SkinVisual, Skin> skins(long index) {
            Map<SkinVisual, Skin> skins = new LinkedHashMap<>();
            for (Axis axis : crossed) if (axis.visual() != null) skins.put(axis.visual(), skin(index, axis.visual()));
            return skins;
        }

        /** La combinaison de l'étape {@code step}. */
        long combinationOf(long step) { return step / group.steps().size(); }

        /** L'état de l'étape {@code step}. */
        Step stepOf(long step) { return group.steps().get((int) (step % group.steps().size())); }

        /** La combinaison de l'étape {@code step} en mots : « Atome : Craie · Tableau : Classique · Thème : Sépia ». */
        String describe(long step) { return describeCombination(combinationOf(step)); }

        /** La combinaison {@code index} en mots. */
        String describeCombination(long index) {
            int[] choice = choice(index);
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < choice.length; i++) {
                if (!text.isEmpty()) text.append(" · ");
                text.append(crossed.get(i).label()).append(" : ").append(crossed.get(i).options().get(choice[i]));
            }
            return text.isEmpty() ? "Tout au Classique" : text.toString();
        }
    }

    /**
     * Un jeu de combinaisons où chaque choix d'un visuel croise au moins une fois chaque choix de chaque autre visuel :
     * glouton (à chaque tour, de quelques candidats, celui qui couvre le plus de paires pas encore vues), et le même à
     * chaque fois pour la même graine.
     */
    static List<int[]> pairwise(int[] sizes, long seed) {
        int n = sizes.length;
        List<int[]> tests = new ArrayList<>();
        if (n <= 1) {
            for (int option = 0; option < (n == 0 ? 1 : sizes[0]); option++) tests.add(n == 0 ? new int[0] : new int[] {option});
            return tests;
        }
        boolean[][][][] covered = new boolean[n][n][][];
        long uncovered = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                covered[i][j] = new boolean[sizes[i]][sizes[j]];
                uncovered += (long) sizes[i] * sizes[j];
            }
        }
        Random random = new Random(seed);
        while (uncovered > 0) {
            // La première paire pas encore vue : chaque tour en couvre au moins une.
            int fi = -1, fj = -1, fa = -1, fb = -1;
            search:
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    for (int a = 0; a < sizes[i]; a++) {
                        for (int b = 0; b < sizes[j]; b++) {
                            if (!covered[i][j][a][b]) {
                                fi = i;
                                fj = j;
                                fa = a;
                                fb = b;
                                break search;
                            }
                        }
                    }
                }
            }
            int[] best = null;
            int bestGain = -1;
            for (int candidate = 0; candidate < 24; candidate++) {
                int[] test = new int[n];
                Arrays.fill(test, -1);
                test[fi] = fa;
                test[fj] = fb;
                List<Integer> order = new ArrayList<>();
                for (int i = 0; i < n; i++) if (i != fi && i != fj) order.add(i);
                java.util.Collections.shuffle(order, random);
                for (int i : order) {
                    int choice = 0, gain = -1;
                    for (int option = 0; option < sizes[i]; option++) {
                        int g = 0;
                        for (int k = 0; k < n; k++) {
                            if (test[k] < 0 || k == i) continue;
                            if (k < i ? !covered[k][i][test[k]][option] : !covered[i][k][option][test[k]]) g++;
                        }
                        if (g > gain || g == gain && random.nextBoolean()) {
                            gain = g;
                            choice = option;
                        }
                    }
                    test[i] = choice;
                }
                int gain = 0;
                for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) if (!covered[i][j][test[i]][test[j]]) gain++;
                if (gain > bestGain) {
                    bestGain = gain;
                    best = test;
                }
            }
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (!covered[i][j][best[i]][best[j]]) {
                        covered[i][j][best[i]][best[j]] = true;
                        uncovered--;
                    }
                }
            }
            tests.add(best);
        }
        return tests;
    }
}
