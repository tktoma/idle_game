package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * La brane, sixième dimension (version 0.154) : une membrane carrée, de −1 à 1 sur chaque axe. Le joueur la tire en
 * un point et la relâche : une onde en part, un cercle qui grandit jusqu'à sa portée, et rebondit sur les bords. Des
 * points apparaissent sur la membrane ; chacun que l'onde touche donne des ondes (la ressource), davantage s'il est
 * touché après un rebond, et davantage pour chaque point de plus que la même onde touche. Petites au début, les ondes
 * grandissent avec les améliorations ({@link BraneUpgrade}) : gain, élasticité, grandeur, multiplicateur de rebond,
 * multiplicateur de points touchés en une onde.
 *
 * <p>Les rebonds se calculent par les images : une onde partie de (x, y) et renvoyée par un bord est la même que celle
 * partie du symétrique de (x, y) par rapport à ce bord ; renvoyée plusieurs fois, c'est celle partie d'une image plus
 * lointaine ({@link #images}). Chaque source touche un point quand son front passe sur lui : une fois en direct, puis
 * une fois par rebond qui le retrouve, aussi souvent que l'onde rebondit (version 0.155) ; le nombre de bords traversés
 * est celui des rebonds. Exact quel que soit le pas. Les points touchés restent, et comptent encore, tant que l'onde
 * court, puis partent avec elle. Une seule onde à la fois.
 *
 * <p>Avec la brane vient la lumière astrale : une amélioration de la graine, payée en ondes, qui multiplie l'arrosage
 * par seconde de l'arrosoir et du tuyau.
 */
public abstract class GameBrane extends GameLoop {

    GameBrane(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Les améliorations de la brane, dans l'ordre de la sauvegarde. */
    public enum BraneUpgrade implements Priced {
        /** Chaque point touché donne une onde de plus par niveau. */
        GAIN(5, 3, Integer.MAX_VALUE),
        /** Une tirée plus courte donne déjà une onde entière : +20 % par niveau. */
        ELASTICITY(6, 2.6, 15),
        /** La portée des ondes : +30 % par niveau (version 0.155 : assez loin, au plus haut, pour plusieurs rebonds). */
        SIZE(8, 2.8, 25),
        /** Un point touché après un rebond : ×1,5, et 0,25 de plus par niveau. */
        BOUNCE(10, 3, 15),
        /** Chaque point de plus touché par la même onde : +10 %, et 5 points de plus par niveau. */
        COMBO(12, 3.2, 15);

        private final Price price;

        BraneUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Un point posé sur la membrane, qui attend une onde. */
    public record BranePoint(double x, double y) {
    }

    /** Une onde : d'où elle part, son rayon, sa portée, et les points qu'elle a déjà touchés (et leur nombre). */
    public static final class BraneWave {
        private final double x, y, reach;
        private double radius;
        private int touched, lastBounces;
        private final java.util.Set<Strike> hit = new java.util.HashSet<>();
        private final List<BranePoint> struck = new ArrayList<>();

        BraneWave(double x, double y, double reach) {
            this.x = x;
            this.y = y;
            this.reach = reach;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double radius() {
            return radius;
        }

        public double reach() {
            return reach;
        }

        /** Ses passages sur un point, rebonds compris. */
        public int touched() {
            return touched;
        }

        /** Les rebonds de son dernier passage sur un point : 0 en direct. */
        public int lastBounces() {
            return lastBounces;
        }

        /** Vrai si elle a touché ce point : il s'en ira avec elle. */
        public boolean hasStruck(BranePoint point) {
            return struck.contains(point);
        }
    }

    /** La membrane va de −{@value} à {@value} sur chaque axe. */
    public static final double BRANE_EDGE = 1;
    /** La portée d'une onde tirée à fond, au départ : 0,35 (petite, sur une membrane de côté 2). */
    public static final double BRANE_REACH = 0.35, BRANE_SIZE_STEP = 0.3, BRANE_ELASTIC_STEP = 0.2;
    /** Le moins qu'une onde porte, même tirée à peine : 30 % de sa portée. */
    public static final double BRANE_MIN_PULL = 0.3;
    /** La vitesse du front, en unités par seconde. */
    public static final double BRANE_WAVE_SPEED = 0.6;
    /** Une seule onde à la fois (version 0.155) : on ne lance pas la suivante avant que la précédente s'éteigne. */
    public static final int BRANE_MAX_WAVES = 1;
    /** Les points : six au plus, un nouveau toutes les deux secondes. */
    public static final int BRANE_MAX_POINTS = 6;
    public static final double BRANE_SPAWN_SECONDS = 2;
    /** Le rebond : ×1,5 au départ, +0,25 par niveau ; la suite : +10 % par point de plus, +5 points par niveau. */
    public static final double BRANE_BOUNCE = 1.5, BRANE_BOUNCE_STEP = 0.25, BRANE_COMBO = 0.1, BRANE_COMBO_STEP = 0.05;
    /** L'automatisme tire une onde à fond toutes les {@value} secondes, au milieu des points. */
    public static final double BRANE_AUTO_SECONDS = 3;

    private final java.util.Random braneRandom = new java.util.Random(6);
    private final List<BranePoint> branePoints = new ArrayList<>();
    private final List<BraneWave> braneWaves = new ArrayList<>();
    private double braneSpawnTimer, braneAutoTimer;
    private long braneTouches, braneBounces, braneWavesSent;
    private int braneMostBounces;
    private double braneLastGain;

    /** Vrai dès que la sixième dimension est ouverte : au sixième univers. */
    public boolean isBraneOpen() {
        return dimensionsUnlocked() >= 6;
    }

    public int braneLevel(BraneUpgrade upgrade) {
        return state.braneLevel(upgrade.ordinal());
    }

    /** Ce que donne un point touché, avant les multiplicateurs : 1, plus 1 par niveau de gain, et le bonus des omégas. */
    public double braneValue() {
        return (1 + braneLevel(BraneUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    /** La portée d'une onde tirée à fond. */
    public double braneMaxReach() {
        return BRANE_REACH * (1 + BRANE_SIZE_STEP * braneLevel(BraneUpgrade.SIZE));
    }

    /** La portée d'une onde tirée de {@code pull} (0 à peine, 1 à fond), élasticité comprise. */
    public double braneReach(double pull) {
        double strength = Math.min(1, Math.max(0, pull) * (1 + BRANE_ELASTIC_STEP * braneLevel(BraneUpgrade.ELASTICITY)));
        return braneMaxReach() * Math.max(BRANE_MIN_PULL, strength);
    }

    /** Le multiplicateur d'un point touché après un rebond. */
    public double braneBounceFactor() {
        return BRANE_BOUNCE + BRANE_BOUNCE_STEP * braneLevel(BraneUpgrade.BOUNCE);
    }

    /** Ce que gagne chaque point de plus touché par une même onde. */
    public double braneComboStep() {
        return BRANE_COMBO + BRANE_COMBO_STEP * braneLevel(BraneUpgrade.COMBO);
    }

    /**
     * Ce que donne le {@code rank}-ième passage (1 le premier) d'une onde, après {@code bounces} rebonds (un par bord
     * touché, coins compris) : le multiplicateur de rebond s'applique une fois par rebond.
     */
    public double braneReward(int rank, int bounces) {
        return braneValue() * Math.pow(braneBounceFactor(), bounces) * (1 + braneComboStep() * Math.max(0, rank - 1));
    }

    public List<BranePoint> branePoints() {
        return Collections.unmodifiableList(branePoints);
    }

    public List<BraneWave> braneWaves() {
        return Collections.unmodifiableList(braneWaves);
    }

    /** Les points touchés depuis le lancement : l'interface y voit ceux de la dernière image. */
    public long braneTouches() {
        return braneTouches;
    }

    /** Les passages après un rebond depuis le lancement. */
    public long braneBounces() {
        return braneBounces;
    }

    /** Le plus de rebonds d'un passage depuis le lancement. */
    public int braneMostBounces() {
        return braneMostBounces;
    }

    public long braneWavesSent() {
        return braneWavesSent;
    }

    /** Ce qu'a donné le dernier point touché. */
    public double braneLastGain() {
        return braneLastGain;
    }

    /** Vrai si une onde de plus peut partir. */
    public boolean canPullBrane() {
        return isBraneOpen() && braneWaves.size() < BRANE_MAX_WAVES;
    }

    /**
     * La membrane, tirée en ({@code x}, {@code y}) de {@code pull} (0 à 1), est relâchée : une onde en part. Faux si
     * la dimension est fermée ou qu'une onde court déjà.
     */
    public boolean pullBrane(double x, double y, double pull) {
        if (!canPullBrane()) return false;
        double px = Math.max(-BRANE_EDGE, Math.min(BRANE_EDGE, x)), py = Math.max(-BRANE_EDGE, Math.min(BRANE_EDGE, y));
        braneWaves.add(new BraneWave(px, py, braneReach(pull)));
        braneWavesSent++;
        return true;
    }

    /** Pose un point : pour les essais. */
    public void addBranePoint(double x, double y) {
        if (branePoints.size() < BRANE_MAX_POINTS) branePoints.add(new BranePoint(x, y));
    }

    /** Outil d'essai : retire tous les points. */
    public void testClearBrane() {
        branePoints.clear();
        braneWaves.clear();
        braneSpawnTimer = 0;
    }

    /**
     * La brane avance de {@code dt} secondes : un point apparaît à son rythme tant qu'il en manque ; chaque onde
     * grandit, touche les points que son front (ou l'un de ses rebonds) passe, et s'éteint à sa portée. L'automatisme
     * de jeu tire une onde à fond au milieu des points, toutes les {@link #BRANE_AUTO_SECONDS} secondes.
     */
    void runBrane(double dt) {
        if (!isBraneOpen() || dt <= 0) return;
        braneSpawnTimer += dt;
        while (braneSpawnTimer >= BRANE_SPAWN_SECONDS && branePoints.size() < BRANE_MAX_POINTS) {
            braneSpawnTimer -= BRANE_SPAWN_SECONDS;
            branePoints.add(new BranePoint((braneRandom.nextDouble() * 2 - 1) * 0.9, (braneRandom.nextDouble() * 2 - 1) * 0.9));
        }
        if (branePoints.size() >= BRANE_MAX_POINTS) braneSpawnTimer = Math.min(braneSpawnTimer, BRANE_SPAWN_SECONDS);
        if (isBraneAutomatic()) {
            braneAutoTimer += dt;
            while (braneAutoTimer >= BRANE_AUTO_SECONDS && canPullBrane()) {
                braneAutoTimer -= BRANE_AUTO_SECONDS;
                double cx = 0, cy = 0;
                for (BranePoint point : branePoints) {
                    cx += point.x();
                    cy += point.y();
                }
                int n = Math.max(1, branePoints.size());
                pullBrane(cx / n, cy / n, 1);
            }
            if (!canPullBrane()) braneAutoTimer = Math.min(braneAutoTimer, BRANE_AUTO_SECONDS);
        }
        for (java.util.Iterator<BraneWave> it = braneWaves.iterator(); it.hasNext(); ) {
            BraneWave wave = it.next();
            double from = wave.radius, to = Math.min(wave.reach, from + BRANE_WAVE_SPEED * dt);
            wave.radius = to;
            touch(wave, from, to);
            if (to >= wave.reach - 1e-12) {
                it.remove();
                branePoints.removeAll(wave.struck);
            }
        }
    }

    /**
     * Les passages du front entre les rayons {@code from} et {@code to}, dans l'ordre où ils arrivent : chacune des neuf
     * sources (l'onde et ses rebonds) touche un point une fois. Le point touché reste sur la membrane tant que l'onde
     * court, pour que ses rebonds le retrouvent, et s'en va avec elle.
     */
    private void touch(BraneWave wave, double from, double to) {
        record Hit(BranePoint point, int image, double distance, int bounces) {
        }
        List<Hit> hits = new ArrayList<>();
        double[][] xs = images(wave.x), ys = images(wave.y);
        for (BranePoint point : branePoints) {
            for (int i = 0; i < xs.length; i++) {
                for (int j = 0; j < ys.length; j++) {
                    int image = xs.length * i + j;
                    if (wave.hit.contains(new Strike(point, image))) continue;
                    double d = Math.hypot(point.x() - xs[i][0], point.y() - ys[j][0]);
                    if (d > from - 1e-12 && d <= to + 1e-12) hits.add(new Hit(point, image, d, (int) (xs[i][1] + ys[j][1])));
                }
            }
        }
        hits.sort(java.util.Comparator.comparingDouble(Hit::distance));
        for (Hit hit : hits) {
            wave.hit.add(new Strike(hit.point(), hit.image()));
            if (!wave.struck.contains(hit.point())) wave.struck.add(hit.point());
            wave.touched++;
            double gain = braneReward(wave.touched, hit.bounces());
            state.setBraneShards(state.braneShards() + gain);
            state.setBraneShardsEarned(state.braneShardsEarned() + gain);
            braneTouches++;
            if (hit.bounces() > 0) braneBounces++;
            braneMostBounces = Math.max(braneMostBounces, hit.bounces());
            wave.lastBounces = hit.bounces();
            braneLastGain = gain;
        }
    }

    /** Un passage : tel point, touché par telle source (0 l'onde même, les autres ses rebonds). */
    private record Strike(BranePoint point, int image) {
    }

    /** Combien de fois, au plus, une onde peut repasser par le même bord de chaque côté : assez pour la plus grande portée. */
    static final int BRANE_IMAGE_RANGE = 2;

    /**
     * Une coordonnée et ses images par les bords, chacune avec son nombre de rebonds : c + 4m après 2|m| rebonds, et
     * 2 − c + 4m après |2m + 1| rebonds, pour m de −{@value #BRANE_IMAGE_RANGE} à {@value #BRANE_IMAGE_RANGE}. Une onde
     * renvoyée plusieurs fois entre deux bords opposés est la même que celle partie de ces images.
     */
    public static double[][] images(double c) {
        int n = 2 * BRANE_IMAGE_RANGE + 1;
        double[][] out = new double[2 * n][];
        int k = 0;
        for (int m = -BRANE_IMAGE_RANGE; m <= BRANE_IMAGE_RANGE; m++) {
            out[k++] = new double[] {c + 4 * BRANE_EDGE * m, 2 * Math.abs(m)};
            out[k++] = new double[] {2 * BRANE_EDGE - c + 4 * BRANE_EDGE * m, Math.abs(2 * m + 1)};
        }
        return out;
    }

    /** Vrai quand l'automatisme de la brane joue (automatisation des dimensions, version 0.154). */
    boolean isBraneAutomatic() {
        return isDimensionAutomationRunning(idle.core.universe.DimensionAutomation.BRANE_PULL);
    }

    /** Prix du prochain niveau, en ondes ; infini une fois au plus haut. */
    public double braneCost(BraneUpgrade upgrade) {
        return upgrade.cost(braneLevel(upgrade));
    }

    public boolean canBuyBrane(BraneUpgrade upgrade) {
        return isBraneOpen() && braneLevel(upgrade) < upgrade.max() && state.braneShards() >= braneCost(upgrade) - 1e-9;
    }

    public boolean buyBrane(BraneUpgrade upgrade) {
        if (!canBuyBrane(upgrade)) return false;
        state.setBraneShards(Math.max(0, state.braneShards() - braneCost(upgrade)));
        state.setBraneLevel(upgrade.ordinal(), braneLevel(upgrade) + 1);
        return true;
    }

    @Override
    boolean buyBraneUpgrade(int index) {
        return buyBrane(BraneUpgrade.values()[index]);
    }

    // ------------------------------------------------------------------
    // La lumière astrale, pour la graine
    // ------------------------------------------------------------------

    /** Ce que chaque niveau de lumière astrale ajoute à l'arrosage par seconde : +20 %. */
    public static final double ASTRAL_STEP = 0.2;

    /** Le multiplicateur de la lumière astrale : 1 sans elle. */
    public double astralLight() {
        return 1 + ASTRAL_STEP * seedUpgradeLevel(SeedUpgrade.ASTRAL_LIGHT);
    }

    /** L'arrosage par seconde de l'arrosoir et du tuyau, multiplié par la lumière astrale. */
    @Override
    public double wateringBoost() {
        return super.wateringBoost() * astralLight();
    }
}
