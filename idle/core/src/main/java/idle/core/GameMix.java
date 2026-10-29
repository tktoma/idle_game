package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Le mélange, dixième dimension (version 0.158, refait à la 0.159) : un seul mini-jeu qui mêle, étape par étape, les
 * mécaniques des neuf autres dimensions. À l'étape 1, il n'y a que la ligne ; chaque étape suivante y mêle la dimension
 * d'après, jusqu'à l'étape 9 où tout est mêlé. Les fragments (la ressource) gagnés à une étape ouvrent la suivante ; y
 * passer remet à zéro les améliorations du mélange, propres à chaque étape. Chaque dimension mêlée de plus multiplie
 * le gain, et un énorme malus pèse au départ de chaque étape.
 *
 * <p>Les mécaniques, sur une arène carrée de 0 à 1 :
 * <ol>
 *   <li>la ligne : une boule fait l'aller-retour sur une ligne graduée ; chaque graduation passée rapporte ;</li>
 *   <li>le plan : chaque graduation passée pose plutôt un éclat sur l'arène, qu'un point mené par le joueur ramasse ;</li>
 *   <li>le volume : chaque éclat est à une profondeur, que le point doit rejoindre pour le ramasser (plus profond, plus
 *       riche) ;</li>
 *   <li>la durée : ce qui est ramassé va dans une charge, qui grandit tant que le joueur tient le bouton et se verse au
 *       relâchement ;</li>
 *   <li>la boucle : un point tourne sur un cercle ; relâcher quand il est dans la zone multiplie la charge ;</li>
 *   <li>la brane : relâcher lance aussi une onde, qui ramasse les éclats qu'elle touche, davantage après un rebond ;</li>
 *   <li>le clicker : cliquer vite multiplie tout, et des zones ×2 apparaissent sur l'arène ;</li>
 *   <li>la grille : un trésor est caché ; plus le point en est proche, plus il chauffe, et passer dessus le trouve ;</li>
 *   <li>le miroir : les éclats viennent par paires symétriques ; ramasser un éclat puis son reflet double le second.</li>
 * </ol>
 *
 * <p>Avec le mélange vient la dernière amélioration de la graine : elle ouvre la bulle des racines, sous la graine.
 */
public abstract class GameMix extends GameMirror {

    GameMix(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Les améliorations du mélange, dans l'ordre de la sauvegarde ; chacune s'ouvre à l'étape de sa mécanique. */
    public enum MixUpgrade implements Priced {
        /** Le gain : +1 par niveau. */
        GAIN(1, 5, 2.5, Integer.MAX_VALUE),
        /** Le malus : ×1,5 par niveau, de 10 % à plus rien. */
        MALUS(1, 8, 2.5, 6),
        /** La ligne : une graduation de plus par niveau. */
        TICKS(1, 6, 2.4, 6),
        /** Le plan : le point va 15 % plus vite par niveau. */
        SPEED(2, 6, 2.4, 10),
        /** Le volume : le point ramasse plus loin en profondeur, +0,05 par niveau. */
        DEPTH(3, 8, 2.5, 5),
        /** La durée : la charge monte 50 points plus haut par niveau. */
        CHARGE(4, 10, 2.6, 6),
        /** La boucle : la zone du rythme 15 % plus large par niveau. */
        RHYTHM(5, 10, 2.6, 8),
        /** La brane : l'onde porte 20 % plus loin par niveau. */
        REACH(6, 12, 2.7, 10),
        /** Le clicker : une zone ×2 de plus à la fois par niveau. */
        ZONES(7, 15, 2.8, 4),
        /** La grille : le trésor rapporte 25 % de plus par niveau. */
        TREASURE(8, 15, 2.8, 10),
        /** Le miroir : une paire rapporte 0,25 de plus par niveau. */
        PAIRS(9, 20, 2.8, 10);

        private final int stage;
        private final Price price;

        MixUpgrade(int stage, double cost, double growth, int max) {
            this.stage = stage;
            price = new Price(cost, growth, max);
        }

        /** L'étape où elle s'ouvre : celle où sa mécanique est mêlée. */
        public int stage() {
            return stage;
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Un éclat posé sur l'arène : où, à quelle profondeur, son numéro et celui de son reflet (0 sans reflet). */
    public record MixShard(long id, double x, double y, double depth, long twin) {
    }

    /** Une onde lancée au relâchement : d'où, son rayon, sa portée. */
    public static final class MixWave {
        private final double x, y, reach;
        private double radius;
        private final java.util.Set<Long> hit = new java.util.HashSet<>();

        MixWave(double x, double y, double reach) {
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
    }

    /** Une zone ×2 du clicker : son centre, son rayon, son temps restant. */
    public record MixZone(double x, double y, double radius, double left) {
        public boolean contains(double px, double py) {
            return Math.hypot(px - x, py - y) <= radius;
        }
    }

    /** Ce qu'a donné le dernier relâchement, selon le rythme. */
    public enum MixRhythm { NONE, MISS, BAD, GOOD, EXCELLENT }

    /** La dernière étape : tout est mêlé. */
    public static final int MIX_STAGES = 9;
    /**
     * Les fragments à gagner à chaque étape (de 1 à 8) pour ouvrir la suivante : de quoi y passer un quart d'heure
     * environ avec le mélange automatique et ses achats (mesuré à la 0.159 ; chaque mécanique mêlée multiplie beaucoup
     * le gain, surtout la profondeur et la charge).
     */
    private static final double[] MIX_STAGE_GOALS = {1_000, 12_000, 80_000, 1e6, 2e6, 2e6, 3.5e6, 4e6};
    /** Chaque dimension mêlée de plus : +50 % au gain. */
    public static final double MIX_STAGE_BONUS = 0.5;
    /** Le malus de départ : 10 % ; ×1,5 par niveau de réduction, jusqu'à 1. */
    public static final double MIX_MALUS = 0.1, MIX_MALUS_STEP = 1.5;
    /** La ligne : y = 0,5 ; la boule fait l'aller-retour en 8 s ; 3 graduations au départ. */
    public static final double MIX_LINE_Y = 0.5, MIX_BALL_SPEED = 0.25;
    public static final int MIX_TICKS = 3;
    /** Les éclats : 12 au plus ; un éclat vaut deux graduations. */
    public static final int MIX_MAX_SHARDS = 12;
    public static final double MIX_SHARD_VALUE = 2;
    /** Le point : 0,3 par seconde, +15 % par niveau ; il ramasse à 0,05. */
    public static final double MIX_CURSOR_SPEED = 0.3, MIX_CURSOR_STEP = 0.15, MIX_REACH_RADIUS = 0.05;
    /** La profondeur : le point ramasse à 0,15 près, +0,05 par niveau ; il plonge d'une profondeur entière par seconde. */
    public static final double MIX_DEPTH_TOLERANCE = 0.15, MIX_DEPTH_STEP = 0.05, MIX_DIVE_SPEED = 1;
    /** La charge : jusqu'à ×2 en 8 s tenues, +0,5 par niveau. */
    public static final double MIX_CHARGE_SECONDS = 8, MIX_CHARGE_STEP = 0.5;
    /** Le rythme : un tour en 4 s ; zone de 0,08 tour de demi-largeur, +15 % par niveau. */
    public static final double MIX_RING_TURNS = 0.25, MIX_RING_HALF = 0.08, MIX_RING_STEP = 0.15;
    /** L'onde : portée 0,3, +20 % par niveau ; 0,6 par seconde ; ×1,5 par rebond. */
    public static final double MIX_WAVE_REACH = 0.3, MIX_WAVE_STEP = 0.2, MIX_WAVE_SPEED = 0.6, MIX_WAVE_BOUNCE = 1.5;
    /** Le clicker : +5 % par clic par seconde (20 au plus), une zone ×2 toutes les 3 s, qui dure 6 s, de rayon 0,1. */
    public static final double MIX_CLICK = 0.05, MIX_CLICK_MAX = 20, MIX_ZONE_EVERY = 3, MIX_ZONE_SECONDS = 6, MIX_ZONE_RADIUS = 0.1;
    /** La grille : le trésor vaut 25 éclats, +25 % par niveau ; il se trouve en passant dans sa case (10 × 10). */
    public static final double MIX_TREASURE = 25, MIX_TREASURE_STEP = 0.25;
    public static final int MIX_GRID = 10;
    /** Le miroir : le reflet ramassé juste après son éclat vaut ×2, +0,25 par niveau ; chaque paire de suite +10 %. */
    public static final double MIX_PAIR = 2, MIX_PAIR_STEP = 0.25, MIX_PAIR_STREAK = 0.1;
    /** L'automatisme clique six fois par seconde. */
    public static final double MIX_AUTO_CLICKS = 6;

    private final java.util.Random mixRandom = new java.util.Random(10);
    private final List<MixShard> mixShards = new ArrayList<>();
    private final List<MixWave> mixWaves = new ArrayList<>();
    private final List<MixZone> mixZones = new ArrayList<>();
    private final ArrayDeque<Double> mixClicks = new ArrayDeque<>();
    private double mixClock, mixBall, mixBallDirection = 1, mixX = 0.5, mixY = 0.3, mixTargetX = 0.5, mixTargetY = 0.3;
    private double mixDepth, mixTargetDepth, mixDirX, mixDirY, mixPot, mixHoldTime, mixRingPhase, mixRingZone = 0.5;
    private double mixZoneTimer, mixTreasureX = -1, mixTreasureY = -1, mixLastGain, mixAutoClickTimer;
    private boolean mixHolding, mixUseDirection;
    private long mixNextId = 1, mixLastCollected, mixPairStreak, mixTreasures, mixReleases;
    private MixRhythm mixLastRhythm = MixRhythm.NONE;

    /** Vrai dès que la dixième dimension est ouverte : au dixième univers. */
    public boolean isMixOpen() {
        return dimensionsUnlocked() >= 10;
    }

    public int mixLevel(MixUpgrade upgrade) {
        return state.mixLevel(upgrade.ordinal());
    }

    /** L'étape : le nombre de dimensions mêlées. */
    public int mixStage() {
        return state.mixStage();
    }

    /** Vrai si la mécanique de la dimension {@code dimension} est mêlée à l'étape en cours. */
    public boolean isMixing(int dimension) {
        return mixStage() >= dimension;
    }

    /** Les fragments à gagner à cette étape pour ouvrir la suivante ; infini à la dernière. */
    public double mixStageGoal() {
        return mixStage() >= MIX_STAGES ? Double.POSITIVE_INFINITY : MIX_STAGE_GOALS[mixStage() - 1];
    }

    public boolean canAdvanceMix() {
        return isMixOpen() && mixStage() < MIX_STAGES && state.mixStageEarned() >= mixStageGoal() - 1e-9;
    }

    /**
     * Passe à l'étape suivante : la dimension d'après se mêle aux autres. Les améliorations du mélange repartent de
     * zéro, l'arène se vide. Faux si l'étape n'est pas gagnée.
     */
    public boolean advanceMix() {
        if (!canAdvanceMix()) return false;
        state.setMixStage(mixStage() + 1);
        state.setMixStageEarned(0);
        for (int i = 0; i < MixUpgrade.values().length; i++) state.setMixLevel(i, 0);
        clearMixArena();
        return true;
    }

    private void clearMixArena() {
        mixShards.clear();
        mixWaves.clear();
        mixZones.clear();
        mixPot = 0;
        mixHoldTime = 0;
        mixHolding = false;
        mixTreasureX = -1;
        mixLastCollected = 0;
        mixPairStreak = 0;
    }

    // ------------------------------------------------------------------
    // Ce que vaut un gain
    // ------------------------------------------------------------------

    /** Ce que garde le malus : 10 % au départ de chaque étape, jusqu'à tout. */
    public double mixMalus() {
        return Math.min(1, MIX_MALUS * Math.pow(MIX_MALUS_STEP, mixLevel(MixUpgrade.MALUS)));
    }

    /** Le multiplicateur des dimensions mêlées : +50 % par dimension de plus. */
    public double mixStageMultiplier() {
        return 1 + MIX_STAGE_BONUS * (mixStage() - 1);
    }

    /** Le multiplicateur de cadence du clicker (étape 7) : 1 avant. */
    public double mixClickMultiplier() {
        if (!isMixing(7)) return 1;
        return 1 + MIX_CLICK * Math.min(MIX_CLICK_MAX, mixClicks.size() / 2.0);
    }

    /** Ce que vaut une graduation : gain, omégas, malus, dimensions mêlées, cadence. */
    public double mixValue() {
        return (1 + mixLevel(MixUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA) * mixMalus() * mixStageMultiplier()
                * mixClickMultiplier();
    }

    private void earnMix(double gain) {
        if (gain <= 0) return;
        mixLastGain = gain;
        state.setMixShards(state.mixShards() + gain);
        state.setMixShardsEarned(state.mixShardsEarned() + gain);
        state.setMixStageEarned(state.mixStageEarned() + gain);
    }

    // ------------------------------------------------------------------
    // Ce que voit l'interface
    // ------------------------------------------------------------------

    public int mixTicks() {
        return MIX_TICKS + mixLevel(MixUpgrade.TICKS);
    }

    /** Où est la graduation {@code index} (de 0 à {@link #mixTicks()} − 1) sur la ligne. */
    public double mixTickX(int index) {
        return (index + 1.0) / (mixTicks() + 1);
    }

    /** Où est la boule sur la ligne, de 0 à 1. */
    public double mixBall() {
        return mixBall;
    }

    public List<MixShard> mixShardList() {
        return Collections.unmodifiableList(mixShards);
    }

    public double mixX() {
        return mixX;
    }

    public double mixY() {
        return mixY;
    }

    /** La profondeur du point (étape 3), de 0 à 1. */
    public double mixDepth() {
        return mixDepth;
    }

    public double mixCursorSpeed() {
        return MIX_CURSOR_SPEED * (1 + MIX_CURSOR_STEP * mixLevel(MixUpgrade.SPEED));
    }

    public double mixDepthTolerance() {
        return MIX_DEPTH_TOLERANCE + MIX_DEPTH_STEP * mixLevel(MixUpgrade.DEPTH);
    }

    /** La charge en attente (étape 4). */
    public double mixPot() {
        return mixPot;
    }

    public boolean isMixHolding() {
        return mixHolding;
    }

    /** Le multiplicateur de la charge en ce moment : de 1 à 2 (et plus) en 8 s tenues. */
    public double mixChargeMultiplier() {
        double top = 1 + 1 + MIX_CHARGE_STEP * mixLevel(MixUpgrade.CHARGE);
        return 1 + (top - 1) * Math.min(1, mixHoldTime / MIX_CHARGE_SECONDS);
    }

    /** Où est le point du rythme sur son cercle, en tours (étape 5). */
    public double mixRingPhase() {
        return mixRingPhase;
    }

    /** Le centre de la zone du rythme, en tours. */
    public double mixRingZone() {
        return mixRingZone;
    }

    public double mixRingHalfWidth() {
        return MIX_RING_HALF * (1 + MIX_RING_STEP * mixLevel(MixUpgrade.RHYTHM));
    }

    /** Ce que donnerait un relâchement maintenant, selon le rythme : NONE avant l'étape 5. */
    public MixRhythm mixRhythmNow() {
        if (!isMixing(5)) return MixRhythm.NONE;
        double d = Math.abs(mixRingPhase - mixRingZone);
        d = Math.min(d, 1 - d) / mixRingHalfWidth();
        return d <= 0.2 ? MixRhythm.EXCELLENT : d <= 0.55 ? MixRhythm.GOOD : d <= 1 ? MixRhythm.BAD : MixRhythm.MISS;
    }

    /** Le multiplicateur d'un rythme : ×3, ×1,5, ×1, ×0,5. */
    public static double mixRhythmFactor(MixRhythm rhythm) {
        return switch (rhythm) {
            case EXCELLENT -> 3;
            case GOOD -> 1.5;
            case BAD, NONE -> 1;
            case MISS -> 0.5;
        };
    }

    public MixRhythm mixLastRhythm() {
        return mixLastRhythm;
    }

    public List<MixWave> mixWaves() {
        return Collections.unmodifiableList(mixWaves);
    }

    public double mixWaveReach() {
        return MIX_WAVE_REACH * (1 + MIX_WAVE_STEP * mixLevel(MixUpgrade.REACH));
    }

    public List<MixZone> mixZones() {
        return Collections.unmodifiableList(mixZones);
    }

    public int mixMaxZones() {
        return isMixing(7) ? 1 + mixLevel(MixUpgrade.ZONES) : 0;
    }

    /** Les clics par seconde, comptés sur deux secondes. */
    public double mixClickRate() {
        return mixClicks.size() / 2.0;
    }

    /** La chaleur du trésor sous le point (étape 8) : de 0 (loin) à 1 (dessus) ; 0 avant. */
    public double mixHeat() {
        if (!isMixing(8) || mixTreasureX < 0) return 0;
        return Math.max(0, 1 - Math.hypot(mixX - mixTreasureX, mixY - mixTreasureY) / Math.sqrt(2));
    }

    public long mixTreasures() {
        return mixTreasures;
    }

    public double mixTreasureValue() {
        return MIX_TREASURE * (1 + MIX_TREASURE_STEP * mixLevel(MixUpgrade.TREASURE));
    }

    public double mixPairFactor() {
        return MIX_PAIR + MIX_PAIR_STEP * mixLevel(MixUpgrade.PAIRS);
    }

    /** Les paires ramassées de suite. */
    public long mixPairStreak() {
        return mixPairStreak;
    }

    public long mixReleases() {
        return mixReleases;
    }

    public double mixLastGain() {
        return mixLastGain;
    }

    // ------------------------------------------------------------------
    // Ce que fait le joueur
    // ------------------------------------------------------------------

    /** Le point file vers ({@code x}, {@code y}) : la souris (étape 2). */
    public void setMixTarget(double x, double y) {
        mixTargetX = clamp(x);
        mixTargetY = clamp(y);
        mixUseDirection = false;
    }

    /** Le point file dans cette direction tant qu'elle est tenue : le clavier (étape 2) ; (0, 0) l'arrête. */
    public void setMixDirection(double dx, double dy) {
        mixDirX = Math.signum(dx);
        mixDirY = Math.signum(dy);
        mixUseDirection = dx != 0 || dy != 0;
        if (!mixUseDirection) {
            mixTargetX = mixX;
            mixTargetY = mixY;
        }
    }

    /** Le point plonge vers cette profondeur (étape 3). */
    public void setMixDepth(double depth) {
        mixTargetDepth = clamp(depth);
    }

    /** Tient le bouton (étape 4) : la charge monte. Le relâcher la verse, au rythme du cercle, et lance l'onde. */
    public void holdMix(boolean hold) {
        if (!isMixOpen() || !isMixing(4)) return;
        if (hold && !mixHolding) {
            mixHolding = true;
            mixHoldTime = 0;
        } else if (!hold && mixHolding) {
            releaseMix();
        }
    }

    private void releaseMix() {
        mixHolding = false;
        MixRhythm rhythm = mixRhythmNow();
        mixLastRhythm = rhythm;
        earnMix(mixPot * mixChargeMultiplier() * mixRhythmFactor(rhythm));
        mixPot = 0;
        mixHoldTime = 0;
        mixReleases++;
        if (isMixing(5)) mixRingZone = mixRandom.nextDouble();
        if (isMixing(6)) mixWaves.add(new MixWave(mixX, mixY, mixWaveReach()));
    }

    /** Un clic sur l'arène (étape 7) : il compte pour la cadence. */
    public void clickMix() {
        if (!isMixOpen() || !isMixing(7)) return;
        mixClicks.addLast(mixClock);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    /**
     * Le mélange avance de {@code dt} secondes, par morceaux d'au plus un dixième de seconde pour que chaque mécanique
     * se joue comme en jeu ouvert. Au-delà d'une demi-heure d'un coup (une absence), la suite se compte au rythme de la
     * dernière demi-heure.
     */
    void runMix(double dt) {
        if (!isMixOpen() || dt <= 0) return;
        double played = Math.min(dt, 1_800), before = state.mixShardsEarned();
        for (double left = played; left > 1e-12; ) {
            double step = Math.min(0.1, left);
            stepMix(step);
            left -= step;
        }
        if (dt > played) earnMix((state.mixShardsEarned() - before) / played * (dt - played));
        if (isMixAutomatic()) advanceMix();
    }

    private void stepMix(double dt) {
        mixClock += dt;
        while (!mixClicks.isEmpty() && mixClicks.peekFirst() < mixClock - 2) mixClicks.removeFirst();
        boolean auto = isMixAutomatic();
        if (auto) playMixAutomatically(dt);
        moveBall(dt);
        if (isMixing(2)) moveCursor(dt);
        if (isMixing(4) && mixHolding) mixHoldTime += dt;
        if (isMixing(5)) mixRingPhase = (mixRingPhase + MIX_RING_TURNS * dt) % 1;
        if (isMixing(6)) runWaves(dt);
        if (isMixing(7)) runZones(dt);
        if (isMixing(8)) runTreasure();
    }

    /** La boule avance ; chaque graduation passée rapporte (étape 1) ou pose un éclat (à partir de l'étape 2). */
    private void moveBall(double dt) {
        double from = mixBall, to = from + mixBallDirection * MIX_BALL_SPEED * dt;
        if (to > 1) {
            crossTicks(from, 1);
            crossTicks(1, 2 - to);
            mixBall = 2 - to;
            mixBallDirection = -1;
        } else if (to < 0) {
            crossTicks(from, 0);
            crossTicks(0, -to);
            mixBall = -to;
            mixBallDirection = 1;
        } else {
            crossTicks(from, to);
            mixBall = to;
        }
    }

    private void crossTicks(double from, double to) {
        double low = Math.min(from, to), high = Math.max(from, to);
        for (int i = 0; i < mixTicks(); i++) {
            double x = mixTickX(i);
            if (x > low && x <= high) {
                if (!isMixing(2)) earnMix(mixValue());
                else dropShard(x);
            }
        }
    }

    private void dropShard(double x) {
        if (mixShards.size() >= MIX_MAX_SHARDS) return;
        double y = 0.1 + 0.8 * mixRandom.nextDouble();
        double depth = isMixing(3) ? mixRandom.nextDouble() : 0;
        long id = mixNextId++;
        if (isMixing(9) && mixShards.size() + 2 <= MIX_MAX_SHARDS) {
            long twin = mixNextId++;
            mixShards.add(new MixShard(id, x, y, depth, twin));
            mixShards.add(new MixShard(twin, 1 - x, y, depth, id));
        } else {
            mixShards.add(new MixShard(id, x, y, depth, 0));
        }
    }

    private void moveCursor(double dt) {
        double speed = mixCursorSpeed() * dt;
        if (mixUseDirection) {
            double n = Math.hypot(mixDirX, mixDirY);
            mixX = clamp(mixX + mixDirX / n * speed);
            mixY = clamp(mixY + mixDirY / n * speed);
        } else {
            double dx = mixTargetX - mixX, dy = mixTargetY - mixY, d = Math.hypot(dx, dy);
            if (d <= speed) {
                mixX = mixTargetX;
                mixY = mixTargetY;
            } else {
                mixX += dx / d * speed;
                mixY += dy / d * speed;
            }
        }
        if (isMixing(3)) {
            double dd = mixTargetDepth - mixDepth, step = MIX_DIVE_SPEED * dt;
            mixDepth = Math.abs(dd) <= step ? mixTargetDepth : mixDepth + Math.signum(dd) * step;
        }
        for (java.util.Iterator<MixShard> it = mixShards.iterator(); it.hasNext(); ) {
            MixShard shard = it.next();
            if (Math.hypot(shard.x() - mixX, shard.y() - mixY) > MIX_REACH_RADIUS) continue;
            if (isMixing(3) && Math.abs(shard.depth() - mixDepth) > mixDepthTolerance() + 1e-9) continue;
            it.remove();
            collect(shard, 1, false);
        }
    }

    /**
     * Un éclat ramassé : sa valeur, sa profondeur, les zones ×2, son reflet. Il va dans la charge si le bouton est tenu,
     * sinon tout de suite aux fragments ; ce que ramasse une onde ({@code direct}) y va toujours tout de suite.
     */
    private void collect(MixShard shard, double factor, boolean direct) {
        double value = mixValue() * MIX_SHARD_VALUE * factor;
        if (isMixing(3)) value *= 1 + shard.depth();
        if (isMixing(7)) for (MixZone zone : mixZones) if (zone.contains(shard.x(), shard.y())) value *= 2;
        if (isMixing(9)) {
            if (shard.twin() != 0 && shard.twin() == mixLastCollected) {
                mixPairStreak++;
                value *= mixPairFactor() * (1 + MIX_PAIR_STREAK * (mixPairStreak - 1));
            } else if (shard.twin() == 0 || mixShards.stream().noneMatch(s -> s.id() == shard.twin())) {
                mixPairStreak = 0;
            }
            mixLastCollected = shard.id();
        }
        if (!direct && isMixing(4) && mixHolding) mixPot += value;
        else earnMix(value);
    }

    /** Les ondes grandissent et ramassent les éclats que leur front (ou un rebond) passe, d'un coup, sans la charge. */
    private void runWaves(double dt) {
        for (java.util.Iterator<MixWave> it = mixWaves.iterator(); it.hasNext(); ) {
            MixWave wave = it.next();
            double from = wave.radius, to = Math.min(wave.reach, from + MIX_WAVE_SPEED * dt);
            wave.radius = to;
            // L'arène va de 0 à 1 ; la brane, de −1 à 1 : on passe d'un repère à l'autre pour réutiliser ses images.
            double[][] xs = GameBrane.images(2 * wave.x - 1), ys = GameBrane.images(2 * wave.y - 1);
            for (java.util.Iterator<MixShard> s = mixShards.iterator(); s.hasNext(); ) {
                MixShard shard = s.next();
                double best = Double.POSITIVE_INFINITY;
                int bounces = 0;
                for (double[] ix : xs) {
                    for (double[] iy : ys) {
                        double d = Math.hypot(2 * shard.x() - 1 - ix[0], 2 * shard.y() - 1 - iy[0]) / 2;
                        if (d < best) {
                            best = d;
                            bounces = (int) (ix[1] + iy[1]);
                        }
                    }
                }
                if (best > from - 1e-12 && best <= to + 1e-12) {
                    s.remove();
                    collect(shard, Math.pow(MIX_WAVE_BOUNCE, bounces), true);
                }
            }
            if (to >= wave.reach - 1e-12) it.remove();
        }
    }

    private void runZones(double dt) {
        List<MixZone> next = new ArrayList<>();
        for (MixZone zone : mixZones) if (zone.left() > dt) next.add(new MixZone(zone.x(), zone.y(), zone.radius(), zone.left() - dt));
        mixZones.clear();
        mixZones.addAll(next);
        mixZoneTimer += dt;
        if (mixZones.size() >= mixMaxZones()) {
            mixZoneTimer = Math.min(mixZoneTimer, MIX_ZONE_EVERY);
            return;
        }
        if (mixZoneTimer >= MIX_ZONE_EVERY) {
            mixZoneTimer -= MIX_ZONE_EVERY;
            mixZones.add(new MixZone(0.1 + 0.8 * mixRandom.nextDouble(), 0.1 + 0.8 * mixRandom.nextDouble(), MIX_ZONE_RADIUS, MIX_ZONE_SECONDS));
        }
    }

    /** Le trésor : caché dans une case de la grille 10 × 10 ; le point qui entre dans sa case le trouve. */
    private void runTreasure() {
        if (mixTreasureX < 0) hideTreasure();
        int cx = Math.min(MIX_GRID - 1, (int) (mixX * MIX_GRID)), cy = Math.min(MIX_GRID - 1, (int) (mixY * MIX_GRID));
        int tx = Math.min(MIX_GRID - 1, (int) (mixTreasureX * MIX_GRID)), ty = Math.min(MIX_GRID - 1, (int) (mixTreasureY * MIX_GRID));
        if (cx == tx && cy == ty) {
            earnMix(mixValue() * mixTreasureValue());
            mixTreasures++;
            hideTreasure();
        }
    }

    private void hideTreasure() {
        mixTreasureX = mixRandom.nextDouble();
        mixTreasureY = mixRandom.nextDouble();
    }

    /**
     * Le mélange automatique : le point file vers l'éclat le plus proche (et plonge à sa profondeur) ; il tient la charge
     * et la relâche une fois pleine, quand le rythme est au moins « bien » ; il clique six fois par seconde.
     */
    private void playMixAutomatically(double dt) {
        if (isMixing(2) && !mixShards.isEmpty()) {
            MixShard best = null;
            double distance = Double.POSITIVE_INFINITY;
            for (MixShard shard : mixShards) {
                double d = Math.hypot(shard.x() - mixX, shard.y() - mixY) + (isMixing(3) ? Math.abs(shard.depth() - mixDepth) : 0);
                if (d < distance) {
                    distance = d;
                    best = shard;
                }
            }
            setMixTarget(best.x(), best.y());
            if (isMixing(3)) setMixDepth(best.depth());
        }
        if (isMixing(4)) {
            if (!mixHolding) holdMix(true);
            else if (mixHoldTime >= MIX_CHARGE_SECONDS && mixPot > 0
                    && (!isMixing(5) || mixRhythmNow() == MixRhythm.GOOD || mixRhythmNow() == MixRhythm.EXCELLENT)) holdMix(false);
        }
        if (isMixing(7)) {
            mixAutoClickTimer += dt;
            while (mixAutoClickTimer >= 1 / MIX_AUTO_CLICKS) {
                mixAutoClickTimer -= 1 / MIX_AUTO_CLICKS;
                clickMix();
            }
        }
    }

    /** Vrai quand le mélange automatique tourne (automatisation des dimensions, version 0.158). */
    boolean isMixAutomatic() {
        return isDimensionAutomationRunning(DimensionAutomation.MIX_AUTO);
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    /** Prix du prochain niveau, en fragments ; infini une fois au plus haut. */
    public double mixCost(MixUpgrade upgrade) {
        return upgrade.cost(mixLevel(upgrade));
    }

    public boolean canBuyMix(MixUpgrade upgrade) {
        return isMixOpen() && mixStage() >= upgrade.stage() && mixLevel(upgrade) < upgrade.max()
                && state.mixShards() >= mixCost(upgrade) - 1e-9;
    }

    public boolean buyMix(MixUpgrade upgrade) {
        if (!canBuyMix(upgrade)) return false;
        state.setMixShards(Math.max(0, state.mixShards() - mixCost(upgrade)));
        state.setMixLevel(upgrade.ordinal(), mixLevel(upgrade) + 1);
        return true;
    }

    @Override
    boolean buyMixUpgrade(int index) {
        return buyMix(MixUpgrade.values()[index]);
    }

    // ------------------------------------------------------------------
    // Outils d'essai
    // ------------------------------------------------------------------

    /** Pose un éclat ; {@code twin} 0 sans reflet. Rend son numéro. */
    public long testAddMixShard(double x, double y, double depth, long twin) {
        long id = mixNextId++;
        mixShards.add(new MixShard(id, x, y, depth, twin));
        return id;
    }

    /** Pose le point, sans le faire filer. */
    public void testPlaceMixCursor(double x, double y, double depth) {
        mixX = mixTargetX = clamp(x);
        mixY = mixTargetY = clamp(y);
        mixDepth = mixTargetDepth = clamp(depth);
        mixUseDirection = false;
    }

    /** Vide l'arène et met la boule au bord gauche. */
    public void testClearMix() {
        clearMixArena();
        mixBall = 0;
        mixBallDirection = 1;
    }

    /** Cache le trésor ici. */
    public void testHideTreasure(double x, double y) {
        mixTreasureX = x;
        mixTreasureY = y;
    }

    /** Met le point du rythme ici, en tours. */
    public void testSetMixRing(double phase, double zone) {
        mixRingPhase = phase;
        mixRingZone = zone;
    }

    /** Pose une zone ×2. */
    public void testAddMixZone(double x, double y) {
        mixZones.add(new MixZone(x, y, MIX_ZONE_RADIUS, MIX_ZONE_SECONDS));
    }

    // ------------------------------------------------------------------
    // La bulle des racines
    // ------------------------------------------------------------------

    /** La bulle des racines s'ouvre avec la dernière amélioration de la graine. */
    @Override
    public boolean isTreeBubbleOpen(TreeBubble bubble) {
        if (bubble == TreeBubble.ROOTS && seedUpgradeLevel(SeedUpgrade.ROOT_BUBBLE) == 0) return false;
        return super.isTreeBubbleOpen(bubble);
    }
}
