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

    /**
     * La bulle que les bots n'achètent pas : celle du bonsaï, à droite (« les automatismes agissent sans délai et achètent
     * tout ce qu'ils peuvent »). Mesuré en 0.152 sur le Speedrunner : achetée au début de l'univers 5, chaque heure de jeu
     * demande plus de dix minutes de calcul au lieu d'une demi-minute (la synthèse automatique fait cent synthèses par
     * pas du moteur, et chacune recalcule les bonus des éléments, {@code GameAtoms.runAutomation} et
     * {@code GameAtoms.bonuses}) : onze univers ne se mesuraient plus. Le réseau s'en passe (l'arbuste au milieu ouvre le
     * jeune arbre, qui rouvre l'arbuste à droite). {@code -Didle.bulleSansDelai=true} la remet à sa place, la troisième.
     */
    static final boolean INSTANT_BUBBLE = Boolean.getBoolean("idle.bulleSansDelai");

    /**
     * Vrai pour une bulle que les bots n'achètent pas : celle des racines (le défi final, version 0.160 : les bots ne
     * jouent pas les dimensions qui y mènent), et celle du bonsaï à droite (sauf demande).
     */
    static boolean skipped(TreeBubble bubble) {
        return bubble == TreeBubble.ROOTS || bubble == TreeBubble.LEAVES_RIGHT && !INSTANT_BUBBLE;
    }

    /**
     * L'ordre d'achat de l'expert. D'abord l'économie (la base, puis ×2 sur l'énergie du vide), ce qui garde le plus
     * d'un univers à l'autre (l'arbre mûr à droite : matière noire, arbre, explosions et défis ; l'arbre ancien à
     * droite : plus de maturation), puis le reste, la bulle du sommet (l'automatisation des dimensions) en dernier
     * avant les niveaux du géant tout à gauche. Les bulles de {@link #skipped} ne sont pas achetées.
     */
    static final List<Step> BEST_ORDER = List.of(
            new Step(TreeBubble.SEED, 1),
            new Step(TreeBubble.LEAVES_LEFT, 1),
            new Step(TreeBubble.LEAVES_RIGHT, 1),
            new Step(TreeBubble.LEAVES_LEFT, 2),
            new Step(TreeBubble.CROWN_RIGHT, 1),
            new Step(TreeBubble.CROWN_MIDDLE, 1),
            new Step(TreeBubble.YOUNG_RIGHT, 1),
            new Step(TreeBubble.MATURE_RIGHT, 1),
            new Step(TreeBubble.CROWN_RIGHT, 1),
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
            new Step(TreeBubble.GIANT_LEFT_HIGH, 1),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 2),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 3),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 4),
            new Step(TreeBubble.GIANT_LEFT_HIGH, 5));

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

    /** Le palier de la graine que vise le joueur : celui qui ouvre l'univers suivant. */
    static int wantedStage(Game game) {
        // Univers formés n : la partie en cours (rejouée) mène à l'univers n + 1, que le palier n + 1 a ouvert ; elle
        // vise le palier n + 2, celui de l'univers d'après. Sans partie en cours, l'univers n + 1 attend le palier n + 1.
        return Math.min(Game.SEED_STAGES, game.universes() + (game.isReplaying() ? 2 : 1));
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
        if (!active(game) || !chance(style.care())) return 0;
        int done = water(game);
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
        for (Step step : BEST_ORDER) {
            if (skipped(step.bubble()) || game.treeBubbleLevel(step.bubble()) >= step.level()) continue;
            if (!game.isTreeBubbleOpen(step.bubble())) continue;
            return step;
        }
        return null;
    }

    /** La bulle ouverte la moins chère qu'on peut payer, hors de celles de {@link #skipped}. */
    static TreeBubble cheapestBubble(Game game) {
        TreeBubble best = null;
        for (TreeBubble bubble : TreeBubble.values()) {
            if (skipped(bubble) || !game.canBuyTreeBubble(bubble)) continue;
            if (best == null || game.treeBubbleCost(bubble) < game.treeBubbleCost(best)) best = bubble;
        }
        return best;
    }

    int buyBubbles(Game game) {
        int done = 0;
        for (int n = 0; n < 40; n++) {
            TreeBubble choice;
            if (style.plan() == Plan.BEST) {
                Step next = nextBest(game);
                choice = next == null ? null : next.bubble();
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
        if (offeredIn != game.universes()) {
            offeredIn = game.universes();
            offeredAt = game.state().timePlayed();
        }
        return game.state().timePlayed() - offeredAt <= SAVING_MAX_SECONDS;
    }

    /** Les quarks de chaque saveur que demandent les prochaines améliorations du collisionneur : ils restent. */
    static Map<Quark, Integer> reserve(Game game) {
        Map<Quark, Integer> keep = new EnumMap<>(Quark.class);
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            if (upgrade.quark() == null || game.state().colliderUpgradeLevel(upgrade) >= upgrade.maxLevel()) continue;
            keep.merge(upgrade.quark(), game.colliderUpgradeQuarks(upgrade), Math::max);
        }
        return keep;
    }

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
     * pousse ; puis la ligne au meilleur rapport.
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
                + " fois, %d liés (gluons %d), ligne %d achats, graine %d achats, %d automatismes des dimensions ; façon de jouer %s",
                game.seedStage(), bubbles, String.join(" ; ", journal), game.voidAvailable(), hadronsOpened, hadronsBound,
                game.gluons(), lineBought, seedBought, automations, style);
    }
}
