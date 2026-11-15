import idle.core.Game;
import idle.core.universe.DimensionAutomation;
import java.util.Locale;

/**
 * Le temps de jeu à la main qu'il faut, dans chaque dimension, pour payer ses automatismes. Une partie
 * d'essai ouvre la dimension et ses automatismes d'un coup, dimension neuve ; un joueur appliqué la joue seule, au pas
 * de 0,05 s : le plan, le volume et la durée avec leur joueur de {@link DimensionBots}, la ligne avec ses achats toutes
 * les cinq secondes, les autres avec ce que ferait l'automatisme qui les joue, une action par seconde
 * ({@code Game.playDimensionByHand}). Il achète chaque automatisme dès qu'il le peut, et les améliorations comme les
 * bots ({@link UniverseTreePolicy#handBuys}). La sortie : l'instant de chaque achat d'automatisme.
 *
 * <p>Usage : {@code CalibrageAutomatismes [minutes=60] [dimensions=1-10]}. Le réglage visé : le premier palier qui
 * joue vers dix minutes, le dernier vers une demi-heure.
 */
final class CalibrageAutomatismes {
    static final double STEP = 0.05;

    public static void main(String[] args) {
        double minutes = args.length > 0 ? Double.parseDouble(args[0]) : 60;
        int from = 1, to = 10;
        if (args.length > 1) {
            String[] range = args[1].split("-");
            from = Integer.parseInt(range[0]);
            to = Integer.parseInt(range[range.length - 1]);
        }
        for (int dimension = from; dimension <= to; dimension++) play(dimension, minutes);
    }

    /** Une partie neuve où la dimension et ses automatismes sont ouverts. */
    static Game fresh(int dimension) {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        while (game.universes() < Game.dimensionAutomationUniverses(dimension)) {
            if (!game.testAddUniverse()) throw new IllegalStateException("univers " + (game.universes() + 1));
        }
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Math.max(game.state().seedWaterings(), Game.seedStageWaterings(Game.SEED_STAGES)));
        if (!game.isDimensionAutomationOpen(dimension)) throw new IllegalStateException("automatismes fermés : " + dimension);
        return game;
    }

    static void play(int dimension, double minutes) {
        Game game = fresh(dimension);
        DimensionBots.Player player = dimension >= 2 && dimension <= UniverseTreePolicy.LAST_APPLIED
                ? DimensionBots.player(game, DimensionBots.name(dimension)) : null;
        if (dimension > UniverseTreePolicy.LAST_APPLIED) game.playDimensionByHand(dimension, UniverseTreePolicy.HAND_SECONDS);
        StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "DIM %d %s :", dimension, Game.dimensionResourceName(dimension)));
        double t = 0, nextCheck = 0, firstPlay = -1, instant = -1;
        while (t < minutes * 60) {
            if (player != null) player.decide(STEP);
            game.tick(STEP);
            t += STEP;
            if (t < nextCheck) continue;
            nextCheck = t + 1;
            for (DimensionAutomation automation : DimensionAutomation.values()) {
                if (automation.dimension() != dimension || !game.canBuyDimensionAutomation(automation)) continue;
                game.buyDimensionAutomation(automation);
                line.append(String.format(Locale.ROOT, " %s@%.1f", automation.name(), t / 60));
                if (automation.kind() == DimensionAutomation.Kind.PLAY && firstPlay < 0) firstPlay = t / 60;
                if (automation.isInstant()) instant = t / 60;
            }
            if (dimension == 1) {
                for (int n = 0; n < 50; n++) {
                    Game.LineUpgrade best = DimensionBots.bestLine(game);
                    if (best == null || !UniverseTreePolicy.handBuys(game, 1, game.lineCost(best)) || !game.buyLine(best)) break;
                }
            } else if (player == null) {
                UniverseTreePolicy.buyByHand(game, dimension);
            }
            if (instant >= 0 || dimension == 1 && game.nextDimensionAutomation(1) == null) break;
        }
        System.out.println(line);
        System.out.printf(Locale.ROOT, "RESUME %d premier_palier=%.1f min sans_attente=%.1f min fin=%.1f min gagne=%.4g%n", dimension,
                firstPlay, instant, t / 60, earned(game, dimension));
    }

    static double earned(Game game, int dimension) {
        return switch (dimension) {
            case 1 -> game.state().dimensionShardsEarned();
            case 2 -> game.state().planeShardsEarned();
            case 3 -> game.state().volumeShardsEarned();
            case 4 -> game.state().durationShardsEarned();
            case 5 -> game.state().loopShardsEarned();
            case 6 -> game.state().braneShardsEarned();
            case 7 -> game.state().clickerShardsEarned();
            case 8 -> game.state().gridShardsEarned();
            case 9 -> game.state().mirrorShardsEarned();
            default -> game.state().mixShardsEarned();
        };
    }
}
