import idle.core.Game;
import idle.core.GameUniverse.LineUpgrade;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.core.universe.DimensionAutomation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Ce que les bots font en plus à partir de l'univers 4 (version 0.152), par-dessus {@link NextUniversePolicy} et
 * {@link VoidPolicy} : la graine d'univers et son arrosoir, les bulles de l'arbre, les hadrons, la ligne et
 * l'automatisation des dimensions. Une seule classe pour tous les bots, appelée par
 * {@link RobotCosmosPlanner#resourceLoop()} et {@link RobotCosmosPlanner#universeProgress()}.
 *
 * <p>Rien ne se passe ici avant que trois univers soient formés ({@link #active}) : aucun geste, aucun hasard tiré.
 * Les parties des univers 1 à 3 sont donc exactement celles d'avant, et leurs mesures de référence ne bougent pas (les
 * univers 2 et 3 se lancent toujours dès l'univers formé, sans attendre la graine, comme avant la 0.152).
 *
 * <p>Ce que la politique joue, dans l'ordre d'une décision :
 * <ul>
 *   <li><b>la graine</b> : l'arrosoir débloqué (gratuit) et tenu penché jusqu'au palier qui ouvre l'univers suivant
 *       (le palier N ouvre l'univers N) ; l'univers suivant ne se lance qu'une fois ce palier atteint
 *       ({@link #readyToLaunch}), comme l'interface l'exige ;</li>
 *   <li><b>les bulles</b> de l'arbre, payées avec l'énergie du vide accumulée : l'expert suit {@link #BEST_ORDER} (il
 *       économise pour la prochaine de la liste), les autres prennent la moins chère ;</li>
 *   <li><b>les hadrons</b> : l'onglet ouvert dès qu'il est payable (le collisionneur se garde pour lui, deux heures au
 *       plus, comme pour le vide), puis les quarks en trop liés, la sorte la moins fournie d'abord (le bonus croît comme
 *       le logarithme du nombre) ; les quarks que demandent les prochaines améliorations du collisionneur restent ;</li>
 *   <li><b>la ligne</b> (elle tourne seule) : ses améliorations au meilleur rapport ({@link DimensionBots#bestLine}),
 *       le débit de l'arrosoir tant que la graine n'est pas l'arbre-monde, et les automatismes des dimensions dès la
 *       bulle du sommet. Le plan, le volume et la durée se jouent à la souris, au dixième de seconde : les bots à pas
 *       de 20 s ne les jouent pas (voir {@link DimensionBots}), ils ne servent d'ailleurs pas à former un univers.</li>
 * </ul>
 * {@code -Didle.sansArbre=true} coupe tout cela : l'univers suivant se lance alors dès l'univers formé, comme avant.
 */
final class UniverseTreePolicy {

    /** Univers formés à partir desquels la politique joue : trois (la partie de l'univers 4 et les suivantes). */
    static final int FROM_UNIVERSES = 3;
    /** La réserve d'énergie du collisionneur pour les hadrons ne dure pas plus que ce temps de jeu. */
    static final double SAVING_MAX_SECONDS = 2 * 3600;

    /** Comment le joueur choisit ses bulles. */
    enum Plan {
        /** L'expert : la liste {@link #BEST_ORDER}, dans l'ordre ; il économise pour la suivante. */
        BEST,
        /** La moins chère des bulles ouvertes qu'il peut payer (sauf celles dont l'effet est « à venir »). */
        CHEAPEST
    }

    /**
     * La façon de jouer l'arbre d'un profil.
     *
     * @param care chance, à chaque décision, de s'occuper de la graine, des bulles, des hadrons et de la ligne
     * @param plan comment il choisit ses bulles
     */
    record Style(double care, Plan plan) {
        /** Le joueur expert : à chaque décision, les bulles dans l'ordre de {@link #BEST_ORDER}. */
        static final Style EXPERT = new Style(1, Plan.BEST);
    }

    /** Une bulle de la liste de l'expert, au niveau voulu. */
    record Step(TreeBubble bubble, int level) {}

    /** Version 0.163 : le nombre d'univers formés après lequel les bots prennent la bulle des racines (le défi final). */
    static final int ROOTS_AFTER = 11;

    /**
     * Vrai pour une bulle que les bots n'achètent pas encore : celle des racines (le défi final) tant que l'univers 11
     * n'est pas formé (version 0.163 : avant, jamais, les bots ne jouant pas les dimensions qui y mènent). Celle du bonsaï,
     * à droite, s'achète depuis la version 0.167 : sans elle, les automatismes n'achètent qu'un niveau par action.
     */
    static boolean skipped(Game game, TreeBubble bubble) {
        return bubble == TreeBubble.ROOTS && game.universes() < ROOTS_AFTER || withoutBubbles.contains(bubble);
    }

    /**
     * Version 0.168 : les bulles qu'une mesure « sans » retire ({@code sans=bulle:NOM} du bot analyste) : aucun profil ne
     * les achète. Une mesure par JVM : la valeur vaut pour la partie et ses copies d'essai.
     */
    static volatile java.util.Set<TreeBubble> withoutBubbles = java.util.Set.of();

    /** Version 0.168 : l'ordre des bulles imposé à tous les profils pour une mesure ({@code null} : celui de chaque profil). */
    static volatile Plan forcedPlan = null;

    /**
     * L'ordre d'achat de l'expert, depuis la version 0.174 : par utilité mesurée sur les parties de la 0.171 (graine 201),
     * et par les liens de l'arbre qui y mènent.
     * <ol>
     *   <li>La base, puis le bonsaï à droite (l'acte 1 passe de 3 h 17 à 40 min) et ×2 sur l'énergie du vide.</li>
     *   <li>L'arbuste à gauche et à droite : presque rien par eux-mêmes, mais ce sont les liens vers l'arbre mûr.</li>
     *   <li>L'arbre mûr à gauche (le collisionneur gardé : la galaxie vient une à deux minutes après le dernier Big Bang
     *   au lieu de 2 h 42 à 5 h 04), à droite (la matière noire gardée), l'arbre ancien à droite (plus de maturation),
     *   les deux bulles du bas de l'arbre géant (le vide et les défis de Big Bang gardés).</li>
     *   <li>Les liens vers l'arbre-monde, le sommet (l'automatisation des dimensions) avant le cœur, puis les rachats.</li>
     *   <li>En dernier, ce qui ne change rien de mesurable : le jeune arbre à droite (aucun bot ne dépasse dix générateurs,
     *   l'arbre de matière noire en permet déjà cent) et l'arbre géant tout à gauche (le cumul des bulles vaut déjà cent
     *   fois ce qui reste à acheter quand elle naît).</li>
     * </ol>
     * L'ordre de la 0.173 ({@link #ORDER_0173}) reste pour les mesures : {@code -Didle.cheminBulles=0173}.
     */
    static final List<Step> BEST_ORDER = List.of(
            new Step(TreeBubble.SEED, 1),
            new Step(TreeBubble.LEAVES_RIGHT, 1),
            new Step(TreeBubble.LEAVES_LEFT, 1),
            new Step(TreeBubble.CROWN_LEFT, 1),
            new Step(TreeBubble.CROWN_RIGHT, 1),
            new Step(TreeBubble.MATURE_LEFT, 1),
            new Step(TreeBubble.MATURE_RIGHT, 1),
            new Step(TreeBubble.ANCIENT_RIGHT, 1),
            new Step(TreeBubble.GIANT_LEFT_LOW, 1),
            new Step(TreeBubble.GIANT_RIGHT_LOW, 1),
            new Step(TreeBubble.LEAVES_LEFT, 2),
            new Step(TreeBubble.CROWN_MIDDLE, 1),
            new Step(TreeBubble.YOUNG_LEFT, 1),
            new Step(TreeBubble.ANCIENT_LEFT, 1),
            new Step(TreeBubble.SUMMIT, 1),
            new Step(TreeBubble.HEART, 1),
            new Step(TreeBubble.LEAVES_LEFT, 3),
            new Step(TreeBubble.CROWN_RIGHT, 2),
            new Step(TreeBubble.LEAVES_LEFT, 4),
            new Step(TreeBubble.LEAVES_LEFT, 5),
            new Step(TreeBubble.CROWN_RIGHT, 3),
            new Step(TreeBubble.CROWN_RIGHT, 4),
            new Step(TreeBubble.CROWN_RIGHT, 5),
            new Step(TreeBubble.GIANT_RIGHT_HIGH, 1),
            new Step(TreeBubble.YOUNG_RIGHT, 1),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 1),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 2),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 3),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 4),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 5));

    /** L'ordre de l'expert jusqu'à la version 0.173, gardé pour les mesures ({@code -Didle.cheminBulles=0173}). */
    static final List<Step> ORDER_0173 = List.of(
            new Step(TreeBubble.SEED, 1),
            new Step(TreeBubble.LEAVES_LEFT, 1),
            new Step(TreeBubble.LEAVES_RIGHT, 1),
            new Step(TreeBubble.LEAVES_LEFT, 2),
            new Step(TreeBubble.CROWN_RIGHT, 1),
            new Step(TreeBubble.CROWN_MIDDLE, 1),
            new Step(TreeBubble.YOUNG_RIGHT, 1),
            new Step(TreeBubble.MATURE_RIGHT, 1),
            new Step(TreeBubble.CROWN_RIGHT, 2),
            new Step(TreeBubble.LEAVES_LEFT, 3),
            new Step(TreeBubble.ANCIENT_RIGHT, 1),
            new Step(TreeBubble.CROWN_LEFT, 1),
            new Step(TreeBubble.LEAVES_LEFT, 4),
            new Step(TreeBubble.GIANT_RIGHT_LOW, 1),
            new Step(TreeBubble.YOUNG_LEFT, 1),
            new Step(TreeBubble.LEAVES_LEFT, 5),
            new Step(TreeBubble.MATURE_LEFT, 1),
            new Step(TreeBubble.GIANT_RIGHT_HIGH, 1),
            new Step(TreeBubble.ANCIENT_LEFT, 1),
            new Step(TreeBubble.GIANT_LEFT_LOW, 1),
            new Step(TreeBubble.HEART, 1),
            new Step(TreeBubble.SUMMIT, 1),
            new Step(TreeBubble.CROWN_RIGHT, 3),
            new Step(TreeBubble.CROWN_RIGHT, 4),
            new Step(TreeBubble.CROWN_RIGHT, 5),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 1),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 2),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 3),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 4),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 5));

    /** L'ordre que suit l'expert : celui de la 0.174, ou celui d'avant pour une mesure. */
    static List<Step> order() {
        return "0173".equals(System.getProperty("idle.cheminBulles")) ? ORDER_0173 : BEST_ORDER;
    }

    /**
     * Version 0.174 : {@code -Didle.attenteBulles=false} fait acheter à l'expert la première bulle de son ordre qu'il peut
     * payer, sans économiser pour celle qui la précède (par défaut, il attend la suivante de la liste).
     */
    static final boolean WAIT_FOR_NEXT = !"false".equals(System.getProperty("idle.attenteBulles"));

    private final Style style;
    private final Random random;
    int bubbles = 0, hadronsOpened = 0, lineBought = 0, seedBought = 0, automations = 0;
    long hadronsBound = 0;
    /** Quand l'onglet des hadrons s'est proposé, en temps de jeu, et dans quel univers. */
    private double offeredAt = -1;
    private int offeredIn = -1;
    /** Les bulles achetées, dans l'ordre, avec l'univers où elles l'ont été : pour le bilan. */
    final List<String> journal = new ArrayList<>();

    UniverseTreePolicy() {
        this(Style.EXPERT, new Random(0));
    }

    /**
     * @param style  la façon de jouer du profil
     * @param random son propre hasard : les gestes de l'arbre ne déplacent pas les tirages du profil
     */
    UniverseTreePolicy(Style style, Random random) {
        this.style = style;
        this.random = random;
    }

    Style style() {
        return style;
    }

    static boolean disabled() {
        return Boolean.getBoolean("idle.sansArbre");
    }

    /** Vrai à partir de trois univers formés : la partie de l'univers 4, et toutes les suivantes. */
    static boolean active(Game game) {
        return !disabled() && game.universes() >= FROM_UNIVERSES;
    }

    private boolean chance(double p) {
        if (p >= 1) return true;
        if (p <= 0) return false;
        return random.nextDouble() < p;
    }

    /**
     * Le palier de la graine que vise le joueur. Version 0.179 : l'arbre-monde, toujours. Les paliers demandent maintenant
     * des heures d'arrosoir, et chacun ouvre des bulles : un joueur présent tient l'arrosoir tant que la graine grandit,
     * dès l'univers 1 formé. Avant (0.152 à 0.178), le palier de l'univers suivant, trente arrosages plus loin.
     */
    static int wantedStage(Game game) {
        return Game.SEED_STAGES;
    }

    /** Vrai dès qu'il y a une graine à arroser : l'univers 1 formé. La politique des bulles attend {@link #FROM_UNIVERSES}. */
    static boolean watering(Game game) {
        return !disabled() && game.isUniverseFormed();
    }

    // ------------------------------------------------------------------
    // Les dimensions jouées à la main pendant les attentes (version 0.179)
    // ------------------------------------------------------------------

    /** Le joueur appliqué de la dimension en cours, ou {@code null} ; sa dimension est reconsidérée toutes les {@link #DIMENSION_RETHINK_SECONDS} secondes. */
    private DimensionBots.Player dimensionPlayer;
    private int dimensionPlayed;
    private double dimensionRethink;
    /** Le temps de jeu entre deux choix de dimension. */
    static final double DIMENSION_RETHINK_SECONDS = 60;
    /** Les dimensions jouées à la main : le plan, le volume et la durée ; la ligne tourne seule. */
    static final int FIRST_PLAYED = 2, LAST_PLAYED = 4;

    /**
     * Avant chaque pas du jeu, le joueur présent joue une dimension à la main (version 0.179), celle dont les éclats
     * manquent le plus à la prochaine amélioration de la graine : les attentes de l'amas, de la formation et de la
     * maturation se passent dans les dimensions, et ce qu'elles rapportent paie la graine. Rien si la politique est
     * coupée, avant l'univers 1 formé, ou pour un joueur qui ne s'occupe pas de l'arbre à chaque décision ({@link Style#care}).
     */
    void tick(Game game, double dt) {
        if (!watering(game) || style.care() < 1 || dt <= 0) return;
        dimensionRethink -= dt;
        if (dimensionPlayer == null || dimensionRethink <= 0) {
            dimensionRethink = DIMENSION_RETHINK_SECONDS;
            int best = bestDimension(game);
            if (best != dimensionPlayed || dimensionPlayer == null) {
                dimensionPlayer = best == 0 ? null : DimensionBots.player(game, DimensionBots.name(best));
                dimensionPlayed = best;
            }
        }
        if (dimensionPlayer != null) dimensionPlayer.decide(dt);
    }

    /** La dimension à jouer : parmi celles ouvertes de {@link #FIRST_PLAYED} à {@link #LAST_PLAYED}, celle dont la réserve couvre le moins sa prochaine amélioration de la graine ; 0 sans. */
    static int bestDimension(Game game) {
        int best = 0;
        double worst = Double.POSITIVE_INFINITY;
        for (int dimension = FIRST_PLAYED; dimension <= Math.min(LAST_PLAYED, game.dimensionsUnlocked()); dimension++) {
            double cost = cheapestSeedUpgrade(game, dimension);
            double cover = cost == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : game.dimensionResource(dimension) / cost;
            if (cover < worst) {
                worst = cover;
                best = dimension;
            }
        }
        return best;
    }

    /** Le prix de la moins chère des améliorations de la graine ouvertes et pas au plus haut que paie cette dimension ; infini sans. */
    static double cheapestSeedUpgrade(Game game, int dimension) {
        double cheapest = Double.POSITIVE_INFINITY;
        for (SeedUpgrade upgrade : SeedUpgrade.values()) {
            if (upgrade.dimension() != dimension || !game.isSeedUpgradeOpen(upgrade) || game.seedUpgradeLevel(upgrade) >= upgrade.max()) continue;
            cheapest = Math.min(cheapest, game.seedUpgradeCost(upgrade));
        }
        return cheapest;
    }

    /**
     * Vrai si l'univers suivant peut se lancer. Avant l'univers 4 (ou la politique coupée) : toujours, comme avant la
     * 0.152. Ensuite, seulement une fois la graine au palier de cet univers ; en attendant, l'arrosoir est tenu.
     */
    boolean readyToLaunch(Game game) {
        if (!active(game)) return true;
        if (game.seedStage() >= Math.min(Game.SEED_STAGES, game.universes() + 1)) return true;
        water(game);
        return false;
    }

    /** L'arrosoir : débloqué, puis penché tant que la graine n'a pas le palier voulu, repris ensuite. */
    int water(Game game) {
        int done = 0;
        if (!watering(game)) return 0;
        if (game.canUnlockWateringCan() && game.unlockWateringCan()) done++;
        boolean wanted = game.canTendSeed() && game.seedStage() < wantedStage(game);
        if (game.isWatering() != wanted) {
            game.setWatering(wanted);
            done++;
        }
        return done;
    }

    /** Les gestes de l'arbre, à chaque décision ; rien avant l'univers 4. */
    int play(Game game) {
        if (!watering(game) || !chance(style.care())) return 0;
        int done = water(game);
        if (!active(game)) return done;
        done += buyBubbles(game);
        done += hadrons(game);
        done += line(game);
        return done;
    }

    // ------------------------------------------------------------------
    // Les bulles
    // ------------------------------------------------------------------

    /** La prochaine bulle de l'expert : la première de la liste pas encore au niveau voulu et ouverte, ou {@code null}. */
    static Step nextBest(Game game) {
        for (Step step : order()) {
            if (skipped(game, step.bubble()) || game.treeBubbleLevel(step.bubble()) >= step.level()) continue;
            if (!game.isTreeBubbleOpen(step.bubble())) continue;
            if (!WAIT_FOR_NEXT && !game.canBuyTreeBubble(step.bubble())) continue;
            return step;
        }
        return null;
    }

    /** Vrai quand toutes les bulles de l'ordre de l'expert (hors celles qu'on saute) sont au niveau voulu. */
    static boolean allBestDone(Game game) {
        for (Step step : order()) {
            if (!skipped(game, step.bubble()) && game.treeBubbleLevel(step.bubble()) < step.level()) return false;
        }
        return true;
    }

    /** La bulle ouverte la moins chère qu'on peut payer, hors de celles de {@link #skipped}. */
    static TreeBubble cheapestBubble(Game game) {
        TreeBubble best = null;
        for (TreeBubble bubble : TreeBubble.values()) {
            if (skipped(game, bubble) || !game.canBuyTreeBubble(bubble)) continue;
            if (best == null || game.treeBubbleCost(bubble) < game.treeBubbleCost(best)) best = bubble;
        }
        return best;
    }

    int buyBubbles(Game game) {
        int done = 0;
        for (int n = 0; n < 40; n++) {
            TreeBubble choice;
            if ((forcedPlan != null ? forcedPlan : style.plan()) == Plan.BEST) {
                // Version 0.163 : l'ordre de l'expert fini, les bulles qui restent (celle des racines), la moins chère d'abord.
                Step next = nextBest(game);
                choice = next == null ? (allBestDone(game) ? cheapestBubble(game) : null) : next.bubble();
            } else {
                choice = cheapestBubble(game);
            }
            if (choice == null || !game.buyTreeBubble(choice)) break;
            bubbles++;
            done++;
            journal.add(String.format(Locale.ROOT, "%s %d (univers %d, %.2f h)", choice.name(), game.treeBubbleLevel(choice),
                    game.universes() + 1, game.state().timePlayed() / 3600));
            trace(game, "bulle " + choice.name() + " " + game.treeBubbleLevel(choice));
        }
        return done;
    }

    // ------------------------------------------------------------------
    // Les hadrons
    // ------------------------------------------------------------------

    /** Vrai tant que le joueur garde l'énergie du collisionneur pour ouvrir l'onglet des hadrons (deux heures au plus). */
    boolean savingForHadrons(Game game) {
        if (!active(game) || !game.isHadronsOffered()) return false;
        // Version 0.174 : comme pour le vide, pas pour un prix trop loin de la production du collisionneur.
        if (!VoidPolicy.nearPrice(game, game.hadronsPrice())) return false;
        if (offeredIn != game.universes()) {
            offeredIn = game.universes();
            offeredAt = game.state().timePlayed();
        }
        return game.state().timePlayed() - offeredAt <= SAVING_MAX_SECONDS;
    }

    /**
     * Les quarks de chaque saveur que demandent les prochaines améliorations du collisionneur : ils restent. Version
     * 0.174 : seulement pour une amélioration à portée, qui demande au plus {@link #QUARK_REACH} fois les quarks qu'on a.
     * Avant, la réserve gardait aussi les niveaux hors d'atteinte (7 680 quarks u pour les particules niveau 9) : mesuré
     * sur le Speedrunner de la 0.171, les protons et les neutrons restaient figés des heures avec des milliers de quarks.
     */
    static Map<Quark, Integer> reserve(Game game) {
        Map<Quark, Integer> keep = new EnumMap<>(Quark.class);
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            if (upgrade.quark() == null || game.state().colliderUpgradeLevel(upgrade) >= upgrade.maxLevel()) continue;
            int need = game.colliderUpgradeQuarks(upgrade);
            if (need > QUARK_REACH * Math.max(1, game.state().quarks(upgrade.quark()))) continue;
            keep.merge(upgrade.quark(), need, Math::max);
        }
        return keep;
    }

    /** Version 0.174 : une amélioration du collisionneur est « à portée » si elle demande au plus deux fois les quarks qu'on a. */
    static final int QUARK_REACH = 2;

    /** Combien de ce hadron se lient sans entamer la réserve du collisionneur. */
    static long spare(Game game, Hadron hadron, Map<Quark, Integer> keep) {
        long most = game.hadronsBindable(hadron);
        for (Map.Entry<Quark, Integer> need : hadron.recipe().entrySet()) {
            long free = game.state().quarks(need.getKey()) - keep.getOrDefault(need.getKey(), 0);
            most = Math.min(most, Math.max(0, free) / need.getValue());
        }
        return most;
    }

    /**
     * Ouvre l'onglet s'il se propose et se paie, puis lie les quarks en trop : à chaque tour, la sorte ouverte la moins
     * fournie qui se lie, autant qu'elle en a déjà (au moins un), pour garder les sortes au même ordre de grandeur.
     */
    int hadrons(Game game) {
        int done = 0;
        if (game.canOpenHadrons() && game.openHadrons()) {
            hadronsOpened++;
            done++;
            trace(game, "hadrons ouverts");
        }
        if (!game.isHadronsUnlocked()) return done;
        Map<Quark, Integer> keep = reserve(game);
        for (int n = 0; n < 200; n++) {
            Hadron pick = null;
            long room = 0;
            for (Hadron hadron : Hadron.values()) {
                if (!game.isHadronAvailable(hadron)) continue;
                long free = spare(game, hadron, keep);
                if (free <= 0) continue;
                if (pick == null || game.hadrons(hadron) < game.hadrons(pick)) { pick = hadron; room = free; }
            }
            if (pick == null) break;
            long made = game.bindHadrons(pick, Math.min(room, Math.max(1, game.hadrons(pick))));
            if (made <= 0) break;
            hadronsBound += made;
            done++;
        }
        return done;
    }

    // ------------------------------------------------------------------
    // La ligne, l'arrosoir et l'automatisation des dimensions
    // ------------------------------------------------------------------

    /**
     * Les automatismes des dimensions (bulle du sommet), dans l'ordre du jeu ; le débit de l'arrosoir tant que la graine
     * pousse ; les autres améliorations de la graine dès qu'elles sont payables (version 0.163) ; puis la ligne au
     * meilleur rapport.
     */
    int line(Game game) {
        int done = 0;
        if (game.isDimensionAutomationUnlocked()) {
            for (DimensionAutomation automation : DimensionAutomation.values()) {
                if (game.canBuyDimensionAutomation(automation) && game.buyDimensionAutomation(automation)) {
                    automations++;
                    done++;
                    trace(game, "automatisme " + automation.name());
                }
            }
        }
        if (game.canTendSeed() && game.canBuySeedUpgrade(SeedUpgrade.WATERING) && game.buySeedUpgrade(SeedUpgrade.WATERING)) {
            seedBought++;
            done++;
        }
        // Version 0.163 : les autres améliorations de la graine, payées avec les ressources des dimensions, dès qu'elles
        // sont payables (avant, les bots ne prenaient que le débit de l'arrosoir) ; la bulle des racines d'abord, puisqu'elle
        // ouvre le défi final.
        if (game.canBuySeedUpgrade(SeedUpgrade.ROOT_BUBBLE) && game.buySeedUpgrade(SeedUpgrade.ROOT_BUBBLE)) {
            seedBought++;
            done++;
        }
        for (int round = 0; round < 20; round++) {
            boolean any = false;
            for (SeedUpgrade upgrade : SeedUpgrade.values()) {
                if (upgrade == SeedUpgrade.WATERING) continue;
                if (game.canBuySeedUpgrade(upgrade) && game.buySeedUpgrade(upgrade)) {
                    seedBought++;
                    done++;
                    any = true;
                }
            }
            if (!any) break;
        }
        for (int n = 0; n < 50; n++) {
            LineUpgrade best = DimensionBots.bestLine(game);
            if (best == null || !game.buyLine(best)) break;
            lineBought++;
            done++;
        }
        return done;
    }

    /** Avec la propriété {@code idle.traceArbre}, chaque geste de l'arbre s'écrit : temps de jeu, univers, geste. */
    private static void trace(Game game, String what) {
        if (!Boolean.getBoolean("idle.traceArbre")) return;
        System.out.printf(Locale.ROOT, "ARBRE,%.3f,u%d,%s,vide_cumul=%.0f%n", game.state().timePlayed() / 3600,
                game.universes() + 1, what, game.voidAvailable());
    }

    /** Le bilan, pour les diagnostics. */
    String summary(Game game) {
        return String.format(Locale.ROOT, "graine palier %d, %d bulles (%s), énergie du vide à dépenser %.0f, hadrons ouverts %d"
                + " fois, %d liés (gluons %d), ligne %d achats, graine %d achats, %d automatismes des dimensions, dimension jouée %d ; façon de jouer %s",
                game.seedStage(), bubbles, String.join(" ; ", journal), game.voidAvailable(), hadronsOpened, hadronsBound,
                game.gluons(), lineBought, seedBought, automations, dimensionPlayed, style);
    }
}
