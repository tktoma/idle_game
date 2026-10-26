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
        return measure("ligne", maxSeconds);
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
        return measure("plan", maxSeconds);
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
        return measure("volume", maxSeconds);
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
        return measure("duree", maxSeconds);
    }

    // ------------------------------------------------------------------
    // Le joueur appliqué, pas à pas (version 0.140)
    // ------------------------------------------------------------------

    /** Le numéro de la dimension d'un bot : 1 pour la ligne, 2 le plan, 3 le volume, 4 la durée. */
    static int number(String name) {
        return switch (name) {
            case "ligne" -> 1;
            case "plan" -> 2;
            case "volume" -> 3;
            case "duree" -> 4;
            default -> throw new IllegalArgumentException("ligne, plan, volume ou duree : " + name);
        };
    }

    /** Le joueur appliqué d'une dimension, pour cette partie : celle d'une mesure, ou celle du profil de test. */
    static Player player(Game game, String name) {
        return new Player(game, name);
    }

    /**
     * Le joueur appliqué d'une dimension, pas à pas (version 0.140) : avant chaque pas du jeu, {@link #decide} achète,
     * vise, plonge, tient ou relâche, comme décrit en tête de la classe. Les mesures s'en servent sur une partie neuve ;
     * le profil de test, sur la partie en cours (outils « Bots dimension »), en direct ou en avance rapide.
     */
    static final class Player {
        final Game g;
        final String name;
        final int number;
        final List<String> purchases = new ArrayList<>();
        /** Le temps de jeu depuis que le joueur a commencé : il date ses achats. */
        double t;
        private double nextCheck, pause, outside, aiming, released = DURATION_REACTION_SECONDS;
        private long collected;

        Player(Game game, String name) {
            this.g = game;
            this.name = name;
            this.number = number(name);
            this.collected = game.planeCollected();
        }

        /** Vrai si la dimension est ouverte dans cette partie : sinon le joueur n'y fait rien. */
        boolean isOpen() {
            return g.dimensionsUnlocked() >= number;
        }

        /** Les éclats gagnés dans cette dimension. */
        double earned() {
            return switch (number) {
                case 1 -> g.state().dimensionShardsEarned();
                case 2 -> g.state().planeShardsEarned();
                case 3 -> g.state().volumeShardsEarned();
                default -> g.state().durationShardsEarned();
            };
        }

        /** Le seuil du jeu pour cette dimension, celui du rond vert. */
        double threshold() {
            return switch (number) {
                case 1 -> Game.LINE_THRESHOLD;
                case 2 -> Game.PLANE_THRESHOLD;
                case 3 -> Game.VOLUME_THRESHOLD;
                default -> Game.DURATION_THRESHOLD;
            };
        }

        /** Vrai si la dimension a son rond vert dans cette partie. */
        boolean reached() {
            return earned() >= threshold();
        }

        List<String> purchases() {
            return purchases;
        }

        /** Ce que fait le joueur avant un pas de {@code dt} secondes du jeu. Vrai s'il a pu jouer (la dimension est ouverte). */
        boolean decide(double dt) {
            if (!isOpen()) return false;
            switch (number) {
                case 1 -> line();
                case 2 -> plane(dt);
                case 3 -> volume(dt);
                default -> duration(dt);
            }
            t += dt;
            return true;
        }

        /** La ligne tourne seule : un passage toutes les quelques secondes pour acheter. */
        private void line() {
            if (t < nextCheck) return;
            nextCheck += LINE_CHECK_SECONDS;
            for (LineUpgrade best; (best = bestLine(g)) != null && g.buyLine(best); )
                purchases.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.lineLevel(best)));
        }

        /** Le plan : droit vers l'éclat le plus proche, une courte pause après chaque éclat ramassé. */
        private void plane(double dt) {
            for (PlaneUpgrade best; (best = bestPlane(g)) != null && g.buyPlane(best); )
                purchases.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.planeLevel(best)));
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
                double step = g.planeSpeed() * dt;
                double k = len > 1e-9 ? Math.min(1, len / step) / len : 0;
                g.setPlaneInput(dx * k, dy * k);
            }
            pause -= dt;
        }

        /** Le volume : achats entre deux plongées, puis chaque boule visée le temps qu'il faut. */
        private void volume(double dt) {
            if (!g.isInVolume()) {
                for (VolumeUpgrade best; (best = bestVolume(g)) != null && g.buyVolume(best); )
                    purchases.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.volumeLevel(best)));
                outside += dt;
                if (outside >= VOLUME_REENTRY_SECONDS && g.enterVolume()) { outside = 0; aiming = 0; }
            } else if (!g.volumeBalls().isEmpty()) {
                aiming += dt;
                if (aiming >= volumeAimSeconds(g.volumeBallRadius())) {
                    VolumeBall ball = g.volumeBalls().get(0);
                    if (g.shootVolume(ball.x(), ball.y(), ball.z()) != null) aiming = 0;
                }
            }
        }

        /** La durée : tenir jusqu'au centre, relâcher, acheter, rappuyer. */
        private void duration(double dt) {
            if (g.isDurationHeld()) {
                if (g.durationProgress() >= 1) { g.releaseDuration(); released = 0; }
            } else {
                for (DurationUpgrade best; (best = bestDuration(g)) != null && g.buyDuration(best); )
                    purchases.add(String.format(Locale.ROOT, "%.0fs %s %d", t, best, g.durationLevel(best)));
                released += dt;
                if (released >= DURATION_REACTION_SECONDS) g.holdDuration();
            }
        }
    }

    /** Une mesure : le joueur appliqué, sur une partie où la dimension vient de s'ouvrir, jusqu'au rond vert. */
    static Result measure(String name, double maxSeconds) throws Exception {
        Player p = player(open(number(name)), name);
        List<double[]> curve = new ArrayList<>();
        double t = 0, nextCurve = 0;
        while (t < maxSeconds && p.earned() < goal(p.threshold())) {
            p.decide(STEP);
            if (t >= nextCurve) { curve.add(new double[]{t, p.earned()}); nextCurve += 60; }
            p.g.tick(STEP);
            t += STEP;
        }
        return new Result(name, t, p.earned() >= goal(p.threshold()), p.purchases(), curve);
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
