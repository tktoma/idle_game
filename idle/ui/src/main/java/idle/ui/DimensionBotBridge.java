package idle.ui;

import idle.core.Game;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Les bots des dimensions dans le profil de test (version 0.140) : le joueur appliqué de {@code DimensionBots}, celui
 * qui règle les seuils (un quart d'heure par dimension), joue la ligne, le plan, le volume ou la durée de la partie en
 * cours, en direct ou en avance rapide.
 *
 * <p>Comme {@link BotBridge}, le pont trouve les bots à l'exécution, s'ils sont là ; sinon il le dit et ne fait rien.
 */
final class DimensionBotBridge {

    /** La classe des bots des dimensions, dans le module {@code bots}, sans paquet. */
    static final String BOT_CLASS = "DimensionBots";
    /** Les noms des bots, dans l'ordre des dimensions. */
    static final List<String> NAMES = List.of("ligne", "plan", "volume", "duree");
    /** Ce que la barre affiche pour chacun. */
    static final List<String> LABELS = List.of("Ligne", "Plan", "Volume", "Durée");
    /** Le pas des décisions en avance rapide : celui des mesures, assez fin pour que le point du plan vise juste. */
    static final double STEP = 0.05;

    private final Object player;
    private final Method decide;
    private final Method reached;
    private final Method purchases;
    private final int number;

    private DimensionBotBridge(Object player, Method decide, Method reached, Method purchases, int number) {
        this.player = player;
        this.decide = decide;
        this.reached = reached;
        this.purchases = purchases;
        this.number = number;
    }

    private static Class<?> botClass() {
        try {
            return Class.forName(BOT_CLASS, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException | LinkageError absent) {
            try {
                return Class.forName(BOT_CLASS);
            } catch (ClassNotFoundException | LinkageError stillAbsent) {
                return null;
            }
        }
    }

    /** Vrai si les bots des dimensions sont là. */
    static boolean available() {
        return botClass() != null;
    }

    /** Le bot de la dimension {@code number} (1 à 4) pour cette partie, ou {@code null} s'il n'est pas là. */
    static DimensionBotBridge of(Game game, int number) {
        Class<?> type = botClass();
        if (type == null || number < 1 || number > NAMES.size()) return null;
        try {
            Method make = type.getDeclaredMethod("player", Game.class, String.class);
            make.setAccessible(true);
            Object player = make.invoke(null, game, NAMES.get(number - 1));
            Class<?> kind = player.getClass();
            Method decide = kind.getDeclaredMethod("decide", double.class);
            Method reached = kind.getDeclaredMethod("reached");
            Method purchases = kind.getDeclaredMethod("purchases");
            decide.setAccessible(true);
            reached.setAccessible(true);
            purchases.setAccessible(true);
            return new DimensionBotBridge(player, decide, reached, purchases, number);
        } catch (ReflectiveOperationException | RuntimeException incompatible) {
            return null;
        }
    }

    /** La dimension que joue ce bot, de 1 à 4. */
    int number() {
        return number;
    }

    /** Sa dimension, pour les messages : « de la ligne », « du plan »… */
    String of() {
        return switch (number) {
            case 1 -> "de la ligne";
            case 2 -> "du plan";
            case 3 -> "du volume";
            default -> "de la durée";
        };
    }

    /** Ce que fait le joueur avant {@code dt} secondes de jeu. Faux si la dimension est fermée ou si le bot a échoué. */
    boolean decide(double dt) {
        try {
            return (Boolean) decide.invoke(player, dt);
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return false;
        }
    }

    /** Vrai si la dimension a son rond vert. */
    boolean reached() {
        try {
            return (Boolean) reached.invoke(player);
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return false;
        }
    }

    /** Les achats du bot depuis qu'il joue. */
    int purchases() {
        try {
            return ((List<?>) purchases.invoke(player)).size();
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return 0;
        }
    }
}
