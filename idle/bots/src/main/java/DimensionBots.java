import idle.core.Game;
import idle.core.GameState;
import idle.core.GameUniverse.DurationUpgrade;
import idle.core.GameUniverse.LineUpgrade;
import idle.core.GameUniverse.PlaneUpgrade;
import idle.core.GameUniverse.VolumeUpgrade;
import idle.core.GameUniverse.PlaneShard;
import idle.core.GameUniverse.VolumeBall;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * Un bot par dimension (version 0.136) : la ligne, le plan, le volume et la durée se jouent chacun à part, sans le
 * reste de la partie, parce que leurs mécaniques ne dépendent de rien d'autre (leurs éclats ne servent qu'à elles). Chaque
 * bot part de la dimension qui vient de s'ouvrir, joue comme un joueur appliqué, et mesure le temps jusqu'au rond vert
 * (le seuil de la dimension).
 *
 * <p>Le joueur appliqué, ce que le bot imite :
 * <ul>
 *   <li><b>la ligne</b> tourne seule ; le joueur passe toutes les {@link #LINE_CHECK_SECONDS} secondes acheter ;</li>
 *   <li><b>le plan</b> : il file droit vers l'éclat le plus proche, et met {@link #PLANE_REACTION_SECONDS} à repartir
 *       après chaque éclat ramassé ;</li>
 *   <li><b>le volume</b> : il vise chaque boule, en un temps qui dépend de sa taille ({@link #volumeAimSeconds}), replonge
 *       {@link #VOLUME_REENTRY_SECONDS} après la fin d'une plongée et achète entre deux ;</li>
 *   <li><b>la durée</b> : il tient jusqu'au centre, relâche, et met {@link #DURATION_REACTION_SECONDS} à rappuyer.</li>
 * </ul>
 * Les achats suivent le meilleur rapport : le gain d'éclats par seconde que donne un niveau, divisé par son prix, d'après
 * un modèle simple de chaque dimension (le modèle choisit, la partie réelle du jeu mesure).
 *
 * <p>Usage : {@code DimensionBots [ligne|plan|volume|duree|toutes] [minutes au plus] [sortie.csv]}.
 */
final class DimensionBots {

    static final Path START = Path.of("sauvegardes", "reference", "checkpoint_universe1_speedrunner.save");
    /** Le pas de la simulation : petit, pour que le point du plan ne saute pas par-dessus les éclats. */
    static final double STEP = 0.05;
    /**
     * Le joueur plus lent ({@code -Didle.lenteurDimension=2} : deux fois plus lent à réagir, viser, revenir), et le joueur
     * qui achète le moins cher au lieu du meilleur rapport ({@code -Didle.achatDimension=moinsCher}) : pour voir l'écart
     * avec le joueur appliqué.
     */
    static final double SLOW = Double.parseDouble(System.getProperty("idle.lenteurDimension", "1"));
    static final boolean CHEAPEST = "moinsCher".equals(System.getProperty("idle.achatDimension", ""));
    static final double LINE_CHECK_SECONDS = 5 * SLOW;
    static final double PLANE_REACTION_SECONDS = 0.25 * SLOW;
    static final double VOLUME_REENTRY_SECONDS = 1 * SLOW;
    static final double DURATION_REACTION_SECONDS = 0.3 * SLOW;

    /** Ce qu'un bot a fait : le temps jusqu'au rond vert, les achats, et l'avancée minute par minute. */
    record Result(String dimension, double seconds, boolean reached, List<String> purchases, List<double[]> curve) {
        String minutes() {
            return reached ? String.format(Locale.ROOT, "%.1f min", seconds / 60) : "pas atteint";
        }
    }

    /** Une partie où la dimension {@code number} vient de s'ouvrir, avec tout ce qui la concerne à zéro. */
    static Game open(int number) throws Exception {
        GameState state = SaveCodec.read(Files.readString(START)).state();
        Game game = new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT,
                new Random(7));
        game.testResetUniverses();
        while (game.universes() < number) {
            if (!game.testAddUniverse()) throw new IllegalStateException("univers non formé dans " + START);
        }
        return game;
    }

    // ------------------------------------------------------------------
    // La ligne
    // ------------------------------------------------------------------

    /** Éclats de la ligne par seconde, avec ces niveaux. */
    static double lineRate(int yield, int ticks, int speed, int balls, int lines) {
        return (ticks + 1) * Game.LINE_SHARD * (1 + yield) * (1 + balls) * (1 + lines)
                * (1 + Game.LINE_SPEED_STEP * speed) / Game.LINE_HALF_TRIP;
    }

    static double lineRateWith(Game g, LineUpgrade extra) {
        int[] l = new int[LineUpgrade.values().length];
        for (LineUpgrade u : LineUpgrade.values()) l[u.ordinal()] = g.lineLevel(u) + (u == extra ? 1 : 0);
        return lineRate(l[0], l[1], l[2], l[3], l[4]);
    }

    static Result line(double maxSeconds) throws Exception {
        Game g = open(1);
        List<String> bought = new ArrayList<>();
        List<double[]> curve = new ArrayList<>();
        double t = 0, nextCheck = 0, nextCurve = 0;
        while (t < maxSeconds && g.state().dimensionShardsEarned() < goal(Game.LINE_THRESHOLD)) {
            if (t >= nextCheck) {
                nextCheck += LINE_CHECK_SECONDS;
                for (LineUpgrade best; (best = bestLine(g)) != null && g.buyLine(best); )
                    bought.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.lineLevel(best)));
            }
            if (t >= nextCurve) { curve.add(new double[]{t, g.state().dimensionShardsEarned()}); nextCurve += 60; }
            g.tick(STEP);
            t += STEP;
        }
        return new Result("ligne", t, g.state().dimensionShardsEarned() >= goal(Game.LINE_THRESHOLD), bought, curve);
    }

    /** Le niveau de la ligne au meilleur rapport, s'il se paie maintenant ; sinon on attend le meilleur. */
    static LineUpgrade bestLine(Game g) {
        double now = lineRateWith(g, null);
        LineUpgrade best = null;
        double bestScore = 0;
        for (LineUpgrade u : LineUpgrade.values()) {
            double cost = g.lineCost(u);
            if (!Double.isFinite(cost)) continue;
            double score = CHEAPEST ? 1 / cost : (lineRateWith(g, u) - now) / cost;
            if (score > bestScore) { bestScore = score; best = u; }
        }
        return best != null && g.canBuyLine(best) ? best : null;
    }

    // ------------------------------------------------------------------
    // Le plan
    // ------------------------------------------------------------------

    /**
     * Éclats du plan par seconde, d'après un modèle simple : un éclat prend le temps qu'il met à apparaître, ou le temps
     * d'aller le chercher s'il est plus long (la distance moyenne au plus proche des éclats posés, moins la portée).
     */
    static double planeRate(double value, double spawn, int max, double speed, double radius) {
        double reach = radius + Game.PLANE_SHARD_RADIUS;
        double travelOne = Math.max(0, 1.04 - reach) / speed + PLANE_REACTION_SECONDS;
        if (travelOne <= spawn) return value / spawn;
        // Le plan se remplit : le plus proche des éclats posés est plus près.
        double travelFull = Math.max(0, 1 / Math.sqrt(max) - reach) / speed + PLANE_REACTION_SECONDS;
        return value / Math.max(spawn, travelFull);
    }

    static double planeRateWith(Game g, PlaneUpgrade extra) {
        int[] l = new int[PlaneUpgrade.values().length];
        for (PlaneUpgrade u : PlaneUpgrade.values()) l[u.ordinal()] = g.planeLevel(u) + (u == extra ? 1 : 0);
        return planeRate(1 + l[PlaneUpgrade.VALUE.ordinal()],
                Game.PLANE_SPAWN_SECONDS * Math.pow(0.85, l[PlaneUpgrade.SPAWN.ordinal()]),
                Game.PLANE_MAX_SHARDS + l[PlaneUpgrade.COUNT.ordinal()],
                Game.PLANE_SPEED * (1 + 0.2 * l[PlaneUpgrade.SPEED.ordinal()]),
                Game.PLANE_PLAYER_RADIUS * (1 + Game.PLANE_SIZE_STEP * l[PlaneUpgrade.SIZE.ordinal()]));
    }

    static PlaneUpgrade bestPlane(Game g) {
        double now = planeRateWith(g, null);
        PlaneUpgrade best = null;
        double bestScore = 0;
        for (PlaneUpgrade u : PlaneUpgrade.values()) {
            double cost = g.planeCost(u);
            if (!Double.isFinite(cost)) continue;
            double score = CHEAPEST ? 1 / cost : (planeRateWith(g, u) - now) / cost;
            if (score > bestScore) { bestScore = score; best = u; }
        }
        return best != null && g.canBuyPlane(best) ? best : null;
    }

    static Result plane(double maxSeconds) throws Exception {
        Game g = open(2);
        List<String> bought = new ArrayList<>();
        List<double[]> curve = new ArrayList<>();
        double t = 0, nextCurve = 0, pause = 0;
        long collected = g.planeCollected();
        while (t < maxSeconds && g.state().planeShardsEarned() < goal(Game.PLANE_THRESHOLD)) {
            for (PlaneUpgrade best; (best = bestPlane(g)) != null && g.buyPlane(best); )
                bought.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.planeLevel(best)));
            if (g.planeCollected() > collected) { collected = g.planeCollected(); pause = PLANE_REACTION_SECONDS; }
            PlaneShard target = null;
            double nearest = Double.POSITIVE_INFINITY;
            for (PlaneShard s : g.planeShardsOnBoard()) {
                double d = Math.hypot(s.x() - g.planeX(), s.y() - g.planeY());
                if (d < nearest) { nearest = d; target = s; }
            }
            if (pause > 0 || target == null) g.setPlaneInput(0, 0);
            else {
                double dx = target.x() - g.planeX(), dy = target.y() - g.planeY(), len = Math.hypot(dx, dy);
                // Ralentir au dernier pas pour ne pas passer à côté : le joueur vise.
                double step = g.planeSpeed() * STEP;
                double k = len > 1e-9 ? Math.min(1, len / step) / len : 0;
                g.setPlaneInput(dx * k, dy * k);
            }
            pause -= STEP;
            if (t >= nextCurve) { curve.add(new double[]{t, g.state().planeShardsEarned()}); nextCurve += 60; }
            g.tick(STEP);
            t += STEP;
        }
        return new Result("plan", t, g.state().planeShardsEarned() >= goal(Game.PLANE_THRESHOLD), bought, curve);
    }

    // ------------------------------------------------------------------
    // Le volume
    // ------------------------------------------------------------------

    /** Le temps de viser une boule : plus elle est petite, plus il est long (0,78 s au départ, 0,42 s à 0,1 de rayon). */
    static double volumeAimSeconds(double radius) {
        return (0.3 + 0.012 / radius) * SLOW;
    }

    /** Éclats du volume par seconde : par plongée, les boules qu'on a le temps de viser et qui ont eu le temps de naître. */
    static double volumeRate(double dive, double value, double spawn, double radius) {
        double aim = volumeAimSeconds(radius);
        double balls = Math.min(dive / aim, 1 + dive / spawn);
        return balls * value / (dive + VOLUME_REENTRY_SECONDS);
    }

    static double volumeRateWith(Game g, VolumeUpgrade extra) {
        int[] l = new int[VolumeUpgrade.values().length];
        for (VolumeUpgrade u : VolumeUpgrade.values()) l[u.ordinal()] = g.volumeLevel(u) + (u == extra ? 1 : 0);
        double rate = volumeRate(Game.VOLUME_DIVE_SECONDS + Game.VOLUME_DIVE_STEP * l[VolumeUpgrade.TIME.ordinal()],
                1 + l[VolumeUpgrade.VALUE.ordinal()],
                Game.VOLUME_SPAWN_SECONDS * Math.pow(0.85, l[VolumeUpgrade.SPAWN.ordinal()]),
                Game.VOLUME_BALL_RADIUS * (1 + Game.VOLUME_SIZE_STEP * l[VolumeUpgrade.SIZE.ordinal()]));
        // Le nombre de boules à la fois ne compte que si le joueur vise plus lentement qu'elles ne naissent : il en
        // garde alors une réserve. Une petite valeur pour qu'il passe en dernier.
        return rate * (1 + 0.001 * l[VolumeUpgrade.COUNT.ordinal()]);
    }

    static VolumeUpgrade bestVolume(Game g) {
        double now = volumeRateWith(g, null);
        VolumeUpgrade best = null;
        double bestScore = 0;
        for (VolumeUpgrade u : VolumeUpgrade.values()) {
            double cost = g.volumeCost(u);
            if (!Double.isFinite(cost)) continue;
            double score = CHEAPEST ? 1 / cost : (volumeRateWith(g, u) - now) / cost;
            if (score > bestScore) { bestScore = score; best = u; }
        }
        return best != null && g.canBuyVolume(best) ? best : null;
    }

    static Result volume(double maxSeconds) throws Exception {
        Game g = open(3);
        List<String> bought = new ArrayList<>();
        List<double[]> curve = new ArrayList<>();
        double t = 0, nextCurve = 0, outside = 0, aiming = 0;
        while (t < maxSeconds && g.state().volumeShardsEarned() < goal(Game.VOLUME_THRESHOLD)) {
            if (!g.isInVolume()) {
                for (VolumeUpgrade best; (best = bestVolume(g)) != null && g.buyVolume(best); )
                    bought.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.volumeLevel(best)));
                outside += STEP;
                if (outside >= VOLUME_REENTRY_SECONDS && g.enterVolume()) { outside = 0; aiming = 0; }
            } else if (!g.volumeBalls().isEmpty()) {
                aiming += STEP;
                if (aiming >= volumeAimSeconds(g.volumeBallRadius())) {
                    VolumeBall ball = g.volumeBalls().get(0);
                    if (g.shootVolume(ball.x(), ball.y(), ball.z()) != null) aiming = 0;
                }
            }
            if (t >= nextCurve) { curve.add(new double[]{t, g.state().volumeShardsEarned()}); nextCurve += 60; }
            g.tick(STEP);
            t += STEP;
        }
        return new Result("volume", t, g.state().volumeShardsEarned() >= goal(Game.VOLUME_THRESHOLD), bought, curve);
    }

    // ------------------------------------------------------------------
    // La durée
    // ------------------------------------------------------------------

    /** Éclats de la durée par seconde : un appui tenu jusqu'au centre, puis le temps de rappuyer. */
    static double durationRate(int speed, int count, int value, int time) {
        double max = Game.DURATION_MAX_SECONDS + Game.DURATION_TIME_STEP * time;
        double turns = Game.DURATION_TURNS * Math.pow(1 + Game.DURATION_SPEED_STEP, speed) * max;
        return (1 + count) * (1 + value) * (turns * turns + turns) / 2 / (max + DURATION_REACTION_SECONDS);
    }

    static double durationRateWith(Game g, DurationUpgrade extra) {
        int[] l = new int[DurationUpgrade.values().length];
        for (DurationUpgrade u : DurationUpgrade.values()) l[u.ordinal()] = g.durationLevel(u) + (u == extra ? 1 : 0);
        return durationRate(l[0], l[1], l[2], l[3]);
    }

    static DurationUpgrade bestDuration(Game g) {
        double now = durationRateWith(g, null);
        DurationUpgrade best = null;
        double bestScore = 0;
        for (DurationUpgrade u : DurationUpgrade.values()) {
            double cost = g.durationCost(u);
            if (!Double.isFinite(cost)) continue;
            double score = CHEAPEST ? 1 / cost : (durationRateWith(g, u) - now) / cost;
            if (score > bestScore) { bestScore = score; best = u; }
        }
        return best != null && g.canBuyDuration(best) ? best : null;
    }

    static Result duration(double maxSeconds) throws Exception {
        Game g = open(4);
        List<String> bought = new ArrayList<>();
        List<double[]> curve = new ArrayList<>();
        double t = 0, nextCurve = 0, released = DURATION_REACTION_SECONDS;
        while (t < maxSeconds && g.state().durationShardsEarned() < goal(Game.DURATION_THRESHOLD)) {
            if (g.isDurationHeld()) {
                if (g.durationProgress() >= 1) { g.releaseDuration(); released = 0; }
            } else {
                for (DurationUpgrade best; (best = bestDuration(g)) != null && g.buyDuration(best); )
                    bought.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.durationLevel(best)));
                released += STEP;
                if (released >= DURATION_REACTION_SECONDS) g.holdDuration();
            }
            if (t >= nextCurve) { curve.add(new double[]{t, g.state().durationShardsEarned()}); nextCurve += 60; }
            g.tick(STEP);
            t += STEP;
        }
        return new Result("duree", t, g.state().durationShardsEarned() >= goal(Game.DURATION_THRESHOLD), bought, curve);
    }

    // ------------------------------------------------------------------

    /**
     * L'objectif du bot : le seuil du jeu, ou {@code -Didle.objectifDimension} éclats gagnés (pour chercher le seuil qui
     * donne la durée voulue ; {@code infini} : jouer jusqu'au temps au plus).
     */
    static double goal(double threshold) {
        String wanted = System.getProperty("idle.objectifDimension", "").trim();
        if (wanted.isEmpty()) return threshold;
        return wanted.equals("infini") ? Double.POSITIVE_INFINITY : Double.parseDouble(wanted);
    }

    interface Bot {
        Result run(double maxSeconds) throws Exception;
    }

    static Bot bot(String name) {
        return switch (name) {
            case "ligne" -> DimensionBots::line;
            case "plan" -> DimensionBots::plane;
            case "volume" -> DimensionBots::volume;
            case "duree" -> DimensionBots::duration;
            default -> throw new IllegalArgumentException("ligne, plan, volume, duree ou toutes : " + name);
        };
    }

    public static void main(String[] args) throws Exception {
        String which = args.length > 0 ? args[0] : "toutes";
        double maxSeconds = (args.length > 1 ? Double.parseDouble(args[1]) : 120) * 60;
        List<String> names = which.equals("toutes") ? List.of("ligne", "plan", "volume", "duree") : List.of(which);
        StringBuilder csv = new StringBuilder("dimension,atteint,minutes,achats\n");
        for (String name : names) {
            Result r = bot(name).run(maxSeconds);
            System.out.printf(Locale.ROOT, "DIMENSION,%s,atteint=%b,minutes=%.2f,achats=%d%n", r.dimension(), r.reached(),
                    r.seconds() / 60, r.purchases().size());
            for (double[] p : r.curve()) System.out.printf(Locale.ROOT, "  %4.0f min : %.1f éclats%n", p[0] / 60, p[1]);
            System.out.println("  achats : " + String.join(", ", r.purchases()));
            csv.append(String.format(Locale.ROOT, "%s,%b,%.2f,%d%n", r.dimension(), r.reached(), r.seconds() / 60,
                    r.purchases().size()));
        }
        if (args.length > 2) Files.writeString(Path.of(args[2]), csv.toString());
    }
}
