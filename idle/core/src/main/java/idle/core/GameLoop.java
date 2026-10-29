package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * La boucle, cinquième dimension (version 0.153) : un jeu de rythme. Sur chaque cercle, un point tourne, très lentement
 * au début ; une ou plusieurs zones sont posées sur le cercle, chacune en trois parts : au bord « mauvais », puis
 * « bien », au centre « excellent ». Un clic (ou la touche du cercle) quand le point est dans une zone donne des nœuds,
 * d'autant plus que le clic est précis, et la zone touchée se pose ailleurs. Après chaque clic, juste ou non, le cercle
 * attend un court instant ({@link #LOOP_COOLDOWN}) : cliquer sans arrêt ne rapporte rien.
 *
 * <p>Six améliorations ({@link LoopUpgrade}) : la rapidité du point, le nombre de zones par cercle, le gain par zone,
 * le multiplicateur de précision, la taille des zones, et des cercles de plus, jusqu'à sept, chacun sur sa touche.
 *
 * <p>Avec la boucle vient le rond de croissance, près de la graine : il apparaît de temps en temps, et cliqué, il
 * accélère l'arrosage pour un temps ({@link #wateringBoost()}) ; deux améliorations de la graine, payées en nœuds,
 * allongent sa durée et renforcent son effet.
 */
public abstract class GameLoop extends GameDimensionAutomation {

    GameLoop(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Les améliorations de la boucle, dans l'ordre de la sauvegarde. */
    public enum LoopUpgrade implements Priced {
        /** Le point tourne 15 % plus vite par niveau. */
        SPEED(5, 2.6, 20),
        /** Une zone de plus par cercle, jusqu'à quatre. */
        ZONES(40, 6, MAX_ZONES - 1),
        /** Chaque zone rapporte un nœud de plus par niveau. */
        GAIN(5, 3, Integer.MAX_VALUE),
        /** Le centre et le « bien » rapportent plus. */
        PRECISION(8, 3, 15),
        /** Les zones sont 12 % plus larges par niveau. */
        SIZE(6, 2.5, 15),
        /** Un cercle de plus, jusqu'à sept. */
        RINGS(60, 7, MAX_RINGS - 1);

        private final Price price;

        LoopUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Ce que vaut un clic, selon l'endroit de la zone où il tombe. */
    public enum LoopHit {
        /** À côté de toute zone : rien, mais le cercle attend quand même. */
        MISS,
        /** Au bord de la zone. */
        BAD,
        /** Entre le bord et le centre. */
        GOOD,
        /** Au centre. */
        EXCELLENT,
        /** Le cercle attendait encore : le clic ne compte pas. */
        WAIT
    }

    /** Sept cercles au plus, et quatre zones par cercle. */
    public static final int MAX_RINGS = 7, MAX_ZONES = 4;
    /** Tours par seconde du point, au départ : un tour en vingt secondes. */
    public static final double LOOP_TURNS = 0.05;
    public static final double LOOP_SPEED_STEP = 1.15;
    /** La demi-largeur d'une zone au départ, en tours (une zone couvre 6 % du cercle), et ce que gagne chaque niveau. */
    public static final double LOOP_ZONE_HALF = 0.03, LOOP_SIZE_STEP = 0.12;
    /** Les parts d'une zone, depuis son centre : jusqu'à 20 % de la demi-largeur « excellent », jusqu'à 55 % « bien ». */
    public static final double LOOP_EXCELLENT = 0.2, LOOP_GOOD = 0.55;
    /** Ce que rapporte chaque part, en nœuds de base : le bord un quart, le « bien » un, le centre trois. */
    public static final double LOOP_BAD_SHARE = 0.25, LOOP_EXCELLENT_SHARE = 3;
    /** Ce que la précision ajoute par niveau : au centre +25 %, au « bien » +10 %. */
    public static final double LOOP_PRECISION_EXCELLENT = 0.25, LOOP_PRECISION_GOOD = 0.1;
    /** L'attente d'un cercle après un clic, en secondes. */
    public static final double LOOP_COOLDOWN = 0.4;

    // Les cercles ne se sauvegardent pas : ils repartent au lancement, avec leurs zones tirées au hasard.
    private final java.util.Random loopRandom = new java.util.Random(5);
    private final double[] loopPhase = new double[MAX_RINGS];
    private final double[] loopWait = new double[MAX_RINGS];
    private final double[][] loopZones = new double[MAX_RINGS][];
    private long loopHits;
    private LoopHit lastLoopHit = LoopHit.MISS;
    /** L'automatisme : la part du prochain clic « bien » qu'il a déjà accumulée. */
    private double loopAutoPending;

    /** Vrai dès que la cinquième dimension est ouverte : au cinquième univers. */
    public boolean isLoopOpen() {
        return dimensionsUnlocked() >= 5;
    }

    public int loopLevel(LoopUpgrade upgrade) {
        return state.loopLevel(upgrade.ordinal());
    }

    /** Les cercles en jeu : un, et un de plus par niveau. */
    public int loopRings() {
        return Math.min(MAX_RINGS, 1 + loopLevel(LoopUpgrade.RINGS));
    }

    /** Les zones de chaque cercle : une, et une de plus par niveau. */
    public int loopZoneCount() {
        return Math.min(MAX_ZONES, 1 + loopLevel(LoopUpgrade.ZONES));
    }

    /** Les tours par seconde du point. */
    public double loopTurnsPerSecond() {
        return LOOP_TURNS * Math.pow(LOOP_SPEED_STEP, loopLevel(LoopUpgrade.SPEED));
    }

    /** La demi-largeur d'une zone, en tours. */
    public double loopZoneHalfWidth() {
        return LOOP_ZONE_HALF * (1 + LOOP_SIZE_STEP * loopLevel(LoopUpgrade.SIZE));
    }

    /** Les nœuds de base d'une zone touchée : 1, plus 1 par niveau de gain, et le bonus des omégas. */
    public double loopValue() {
        return (1 + loopLevel(LoopUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    /** Ce que rapporte un clic de cette sorte. */
    public double loopReward(LoopHit hit) {
        int precision = loopLevel(LoopUpgrade.PRECISION);
        return switch (hit) {
            case EXCELLENT -> loopValue() * LOOP_EXCELLENT_SHARE * (1 + LOOP_PRECISION_EXCELLENT * precision);
            case GOOD -> loopValue() * (1 + LOOP_PRECISION_GOOD * precision);
            case BAD -> loopValue() * LOOP_BAD_SHARE;
            case MISS, WAIT -> 0;
        };
    }

    /** Où est le point du cercle {@code ring}, en tours de 0 à 1. */
    public double loopPhase(int ring) {
        return ring >= 0 && ring < MAX_RINGS ? loopPhase[ring] : 0;
    }

    /** Les centres des zones du cercle {@code ring}, en tours de 0 à 1. */
    public List<Double> loopZones(int ring) {
        List<Double> zones = new ArrayList<>();
        if (ring < 0 || ring >= loopRings()) return zones;
        double[] centers = zonesOf(ring);
        for (int i = 0; i < loopZoneCount(); i++) zones.add(centers[i]);
        return zones;
    }

    /** Ce qu'il reste au cercle {@code ring} à attendre avant de compter un clic, en secondes. */
    public double loopWait(int ring) {
        return ring >= 0 && ring < MAX_RINGS ? Math.max(0, loopWait[ring]) : 0;
    }

    /** Les clics qui ont rapporté, depuis le lancement : l'interface y voit ceux de la dernière image. */
    public long loopHits() {
        return loopHits;
    }

    /** Ce qu'a valu le dernier clic compté. */
    public LoopHit lastLoopHit() {
        return lastLoopHit;
    }

    /** Ce que vaudrait un clic maintenant sur le cercle {@code ring}, sans cliquer. */
    public LoopHit loopAim(int ring) {
        if (!isLoopOpen() || ring < 0 || ring >= loopRings()) return LoopHit.MISS;
        if (loopWait[ring] > 1e-9) return LoopHit.WAIT;
        return hitAt(ring, loopPhase[ring]);
    }

    private LoopHit hitAt(int ring, double phase) {
        double half = loopZoneHalfWidth(), best = Double.POSITIVE_INFINITY;
        for (double center : loopZones(ring)) best = Math.min(best, circular(phase - center));
        if (best > half) return LoopHit.MISS;
        double ratio = best / half;
        return ratio <= LOOP_EXCELLENT ? LoopHit.EXCELLENT : ratio <= LOOP_GOOD ? LoopHit.GOOD : LoopHit.BAD;
    }

    /** La distance sur le cercle, de 0 à un demi-tour. */
    private static double circular(double delta) {
        double d = delta - Math.floor(delta);
        return Math.min(d, 1 - d);
    }

    /**
     * Un clic sur le cercle {@code ring} (ou sa touche) : ce qu'il vaut, et les nœuds qu'il donne. La zone touchée se
     * pose ailleurs, et le cercle attend {@link #LOOP_COOLDOWN} secondes, même pour un clic à côté.
     */
    public LoopHit clickLoop(int ring) {
        LoopHit hit = loopAim(ring);
        if (hit == LoopHit.WAIT || !isLoopOpen() || ring < 0 || ring >= loopRings()) return hit == LoopHit.WAIT ? hit : LoopHit.MISS;
        loopWait[ring] = LOOP_COOLDOWN;
        lastLoopHit = hit;
        if (hit == LoopHit.MISS) return hit;
        gainLoop(loopReward(hit));
        loopHits++;
        moveTouchedZone(ring, loopPhase[ring]);
        return hit;
    }

    private void gainLoop(double nodes) {
        state.setLoopShards(state.loopShards() + nodes);
        state.setLoopShardsEarned(state.loopShardsEarned() + nodes);
    }

    /** La zone la plus proche du point repart ailleurs, loin de lui et des autres zones. */
    private void moveTouchedZone(int ring, double phase) {
        double[] centers = zonesOf(ring);
        int nearest = 0;
        for (int i = 1; i < loopZoneCount(); i++) {
            if (circular(phase - centers[i]) < circular(phase - centers[nearest])) nearest = i;
        }
        placeZone(ring, nearest, phase);
    }

    /** L'écart qu'il reste au moins entre deux zones d'un même cercle, en tours (version 0.154 : elles ne se superposent plus). */
    public static final double LOOP_ZONE_GAP = 0.02;

    /**
     * Pose la zone {@code zone} du cercle {@code ring} au hasard, sans toucher les autres zones du cercle et loin du
     * point ({@code avoid}, en tours) : il ne la touche pas tout de suite. Faute de place, au plus loin de tout.
     */
    private void placeZone(int ring, int zone, double avoid) {
        double[] centers = zonesOf(ring);
        double half = loopZoneHalfWidth();
        // Toutes les places possibles, un tour découpé en 360, décalé au hasard : on en tire une parmi les libres.
        double offset = loopRandom.nextDouble() / PLACES;
        List<Double> free = new ArrayList<>(), farFromPoint = new ArrayList<>();
        double best = Double.NEGATIVE_INFINITY, bestCenter = centers[zone];
        for (int k = 0; k < PLACES; k++) {
            double center = offset + (double) k / PLACES;
            double room = Double.POSITIVE_INFINITY;
            for (int other = 0; other < loopZoneCount(); other++) {
                if (other != zone) room = Math.min(room, circular(center - centers[other]) - 2 * half - LOOP_ZONE_GAP);
            }
            if (room >= 0) {
                free.add(center);
                if (circular(center - avoid) >= half + 0.05) farFromPoint.add(center);
            }
            if (room > best) {
                best = room;
                bestCenter = center;
            }
        }
        List<Double> pick = farFromPoint.isEmpty() ? free : farFromPoint;
        centers[zone] = pick.isEmpty() ? bestCenter : pick.get(loopRandom.nextInt(pick.size()));
    }

    /** Les places essayées sur un tour pour poser une zone. */
    private static final int PLACES = 360;

    /** Déplace les zones qui se chevauchent : quand une zone de plus arrive, ou que les zones grandissent. */
    private void spreadZones(int ring) {
        double[] centers = zonesOf(ring);
        double half = loopZoneHalfWidth();
        for (int zone = 1; zone < loopZoneCount(); zone++) {
            for (int other = 0; other < zone; other++) {
                if (circular(centers[zone] - centers[other]) < 2 * half + LOOP_ZONE_GAP - 1e-12) {
                    placeZone(ring, zone, loopPhase[ring]);
                    break;
                }
            }
        }
    }

    private double[] zonesOf(int ring) {
        if (loopZones[ring] == null) {
            loopZones[ring] = new double[MAX_ZONES];
            // Réparties d'avance sur le cercle, à un quart de tour l'une de l'autre : elles ne se touchent pas.
            double start = 0.3 + loopRandom.nextDouble() * 0.2 + ring * 0.13;
            for (int i = 0; i < MAX_ZONES; i++) loopZones[ring][i] = (start + i * 0.25) % 1;
        }
        return loopZones[ring];
    }

    /** Pose le point d'un cercle : pour les essais. */
    public void setLoopPhase(int ring, double phase) {
        if (ring >= 0 && ring < MAX_RINGS) loopPhase[ring] = phase - Math.floor(phase);
    }

    /** Pose le centre d'une zone : pour les essais. */
    public void setLoopZone(int ring, int zone, double center) {
        if (ring >= 0 && ring < MAX_RINGS && zone >= 0 && zone < MAX_ZONES) zonesOf(ring)[zone] = center - Math.floor(center);
    }

    /**
     * La boucle avance de {@code dt} secondes : les points tournent, les attentes passent, les zones qui se
     * chevauchent s'écartent. L'automatisme de jeu (automatisation des dimensions) joue vraiment (version 0.154) : il
     * clique quand le point arrive dans le « bien » d'une zone, qui part ailleurs, et son cercle attend, comme pour un
     * clic du joueur. Sur un grand pas (une absence, un bot), il compte autant de « bien » que de passages.
     */
    void runLoop(double dt) {
        if (!isLoopOpen() || dt <= 0) return;
        double turns = loopTurnsPerSecond() * dt;
        boolean automatic = isLoopAutomatic();
        boolean played = automatic && dt <= LOOP_PLAYED_STEP;
        for (int ring = 0; ring < loopRings(); ring++) {
            spreadZones(ring);
            double from = loopPhase[ring];
            loopPhase[ring] = (from + turns) % 1;
            if (played) playRing(ring, from, turns, dt);
            loopWait[ring] = Math.max(0, loopWait[ring] - dt);
        }
        if (automatic && !played) {
            loopAutoPending += turns * loopRings() * loopZoneCount();
            long clicks = (long) Math.floor(loopAutoPending);
            if (clicks > 0) {
                loopAutoPending -= clicks;
                gainLoop(clicks * loopReward(LoopHit.GOOD));
                loopHits += clicks;
                lastLoopHit = LoopHit.GOOD;
            }
        }
    }

    /** Au-delà de ce pas, en secondes, l'automatisme compte ses clics au lieu de les jouer. */
    public static final double LOOP_PLAYED_STEP = 1;

    /**
     * L'automatisme sur un cercle : le point a fait {@code turns} tours depuis {@code from} ; s'il a passé l'endroit
     * où il vise dans une zone (au milieu du « bien », côté arrivée) et que le cercle n'attendait plus, il clique là.
     */
    private void playRing(int ring, double from, double turns, double dt) {
        double half = loopZoneHalfWidth(), aim = -half * (LOOP_EXCELLENT + LOOP_GOOD) / 2;
        double[] centers = zonesOf(ring);
        for (int zone = 0; zone < loopZoneCount(); zone++) {
            double ahead = (centers[zone] + aim - from) - Math.floor(centers[zone] + aim - from);
            if (ahead > turns) continue;
            // Le cercle doit avoir fini d'attendre quand le point y arrive.
            double when = turns <= 0 ? 0 : dt * ahead / turns;
            if (loopWait[ring] - when > 1e-9) continue;
            double phase = loopPhase[ring];
            loopPhase[ring] = (from + ahead) % 1;
            LoopHit hit = hitAt(ring, loopPhase[ring]);
            if (hit == LoopHit.GOOD || hit == LoopHit.EXCELLENT) {
                gainLoop(loopReward(hit));
                loopHits++;
                lastLoopHit = hit;
                loopAutoClicks++;
                moveTouchedZone(ring, loopPhase[ring]);
                loopWait[ring] = LOOP_COOLDOWN + when;
            }
            loopPhase[ring] = phase;
            return;
        }
    }

    private long loopAutoClicks;

    /** Les clics de l'automatisme, joués un par un : l'interface les montre comme ceux du joueur. */
    public long loopAutoClicks() {
        return loopAutoClicks;
    }

    /** Vrai quand l'automatisme de la boucle joue (automatisation des dimensions, version 0.153). */
    boolean isLoopAutomatic() {
        return isDimensionAutomationRunning(idle.core.universe.DimensionAutomation.LOOP_PLAY);
    }

    /** Prix du prochain niveau, en nœuds ; infini une fois au plus haut. */
    public double loopCost(LoopUpgrade upgrade) {
        return upgrade.cost(loopLevel(upgrade));
    }

    public boolean canBuyLoop(LoopUpgrade upgrade) {
        return isLoopOpen() && loopLevel(upgrade) < upgrade.max() && state.loopShards() >= loopCost(upgrade) - 1e-9;
    }

    @Override
    boolean buyLoopUpgrade(int index) {
        return buyLoop(LoopUpgrade.values()[index]);
    }

    public boolean buyLoop(LoopUpgrade upgrade) {
        if (!canBuyLoop(upgrade)) return false;
        state.setLoopShards(Math.max(0, state.loopShards() - loopCost(upgrade)));
        state.setLoopLevel(upgrade.ordinal(), loopLevel(upgrade) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // Le rond de croissance, près de la graine
    // ------------------------------------------------------------------

    /** Il revient toutes les deux minutes environ, et reste douze secondes à cueillir. */
    public static final double BLOOM_EVERY = 120, BLOOM_VISIBLE = 12;
    /** Cueilli, il accélère l'arrosage de 25 % pendant 30 s ; chaque niveau ajoute 10 s, ou 10 points. */
    public static final double BLOOM_SECONDS = 30, BLOOM_SECONDS_STEP = 10, BLOOM_GAIN = 0.25, BLOOM_GAIN_STEP = 0.1;

    private double bloomWait = BLOOM_EVERY, bloomVisibleLeft, bloomActiveLeft, bloomX = 0.5, bloomY = 0.5;
    private long bloomsCaught;

    /** Vrai une fois le rond de croissance là : la boucle ouverte, l'univers formé (il pousse près de la graine). */
    public boolean isBloomUnlocked() {
        return isLoopOpen() && isUniverseFormed();
    }

    /** Vrai pendant qu'il attend d'être cueilli. */
    public boolean isBloomVisible() {
        return isBloomUnlocked() && bloomVisibleLeft > 0;
    }

    public double bloomVisibleLeft() {
        return isBloomVisible() ? bloomVisibleLeft : 0;
    }

    /** Où il est, de 0 à 1 sur chaque axe de la vue de la graine. */
    public double bloomX() {
        return bloomX;
    }

    public double bloomY() {
        return bloomY;
    }

    /** Ce que dure son effet, en secondes. */
    public double bloomSeconds() {
        return BLOOM_SECONDS + BLOOM_SECONDS_STEP * seedUpgradeLevel(SeedUpgrade.BLOOM_DURATION);
    }

    /** Ce qu'il ajoute à l'arrosage pendant son effet : 25 %, et 10 points de plus par niveau. */
    public double bloomGain() {
        return BLOOM_GAIN + BLOOM_GAIN_STEP * seedUpgradeLevel(SeedUpgrade.BLOOM_POWER);
    }

    /** Ce qu'il reste à son effet, en secondes ; 0 sans effet en cours. */
    public double bloomActiveLeft() {
        return Math.max(0, bloomActiveLeft);
    }

    public long bloomsCaught() {
        return bloomsCaught;
    }

    /** Le cueille : son effet commence (ou repart de sa durée entière). Faux s'il n'y en a pas. */
    public boolean catchBloom() {
        if (!isBloomVisible()) return false;
        bloomVisibleLeft = 0;
        // Cueilli, il revient comme s'il était reparti seul : sans cela, l'attente déjà écoulée le refaisait pousser aussitôt.
        bloomWait = nextBloomWait();
        bloomActiveLeft = bloomSeconds();
        bloomsCaught++;
        return true;
    }

    /** L'arrosage, accéléré pendant l'effet du rond. */
    @Override
    public double wateringBoost() {
        return super.wateringBoost() * (bloomActiveLeft > 0 ? 1 + bloomGain() : 1);
    }

    void runBloom(double dt) {
        if (dt <= 0) return;
        bloomActiveLeft = Math.max(0, bloomActiveLeft - dt);
        if (!isBloomUnlocked()) {
            bloomVisibleLeft = 0;
            return;
        }
        if (bloomVisibleLeft > 0) {
            bloomVisibleLeft = Math.max(0, bloomVisibleLeft - dt);
            if (bloomVisibleLeft <= 0) bloomWait = nextBloomWait();
            return;
        }
        bloomWait -= dt;
        if (bloomWait <= 0) {
            bloomVisibleLeft = BLOOM_VISIBLE;
            bloomX = 0.15 + 0.7 * loopRandom.nextDouble();
            bloomY = 0.15 + 0.6 * loopRandom.nextDouble();
        }
    }

    /** L'attente avant le prochain rond : deux minutes environ, de 90 à 150 s. */
    private double nextBloomWait() {
        return BLOOM_EVERY * (0.75 + 0.5 * loopRandom.nextDouble());
    }

    /** Outil d'essai : fait apparaître le rond de croissance tout de suite. */
    public void testShowBloom() {
        if (isBloomUnlocked()) bloomVisibleLeft = BLOOM_VISIBLE;
    }
}
