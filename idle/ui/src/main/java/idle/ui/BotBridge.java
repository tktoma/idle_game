package idle.ui;

import idle.core.Game;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Les bots dans le profil de test (version 0.84) : le bot V2 du module {@code bots}
 * ({@code AdvancedLab}) joue la partie en cours, en direct ou en avance rapide.
 *
 * <p>Les bots ne font pas partie du jeu : ils vivent dans leur module, sans paquet Java, et
 * l'interface ne les connaît pas. Le pont les trouve à l'exécution, s'ils sont là (le lanceur les
 * ajoute avec {@code runtimeOnly(project(":bots"))}) ; sinon les outils le disent et ne font rien.
 */
final class BotBridge {

    /** Le nom de la classe du bot V2. */
    static final String BOT_CLASS = "AdvancedLab";
    /** Les politiques du bot V2, si le bot n'est pas là pour les donner. */
    static final List<String> FALLBACK_MODES = List.of("DEPENDANCES", "EXPERT_COMPLET", "ECONOMISTE", "PLAN_PRESTIGE", "ADAPTATIF",
            "DEBUTANT_APPRENANT", "GUIDE_UI", "RECUPERATION", "SANS_IMPULSIONS", "SANS_COMETES", "SANS_TECH", "SANS_LISTE");
    /** Le pas des décisions du bot, en secondes de jeu : celui des campagnes. */
    static final double STEP = 20;
    private static final long SEED = 42;

    private final Object bot;
    private final Method act;
    private final String mode;

    private BotBridge(Object bot, Method act, String mode) {
        this.bot = bot;
        this.act = act;
        this.mode = mode;
    }

    /** La classe du bot, ou {@code null} si le module des bots n'est pas là. */
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

    /** Vrai si les bots sont là. */
    static boolean available() {
        return botClass() != null;
    }

    /** Les politiques que le bot connaît, dans son ordre. */
    static List<String> modes() {
        Class<?> type = botClass();
        if (type == null) return FALLBACK_MODES;
        try {
            java.lang.reflect.Field field = type.getDeclaredField("MODES");
            field.setAccessible(true);
            return List.of((String[]) field.get(null));
        } catch (ReflectiveOperationException | RuntimeException unexpected) {
            return FALLBACK_MODES;
        }
    }

    /** Un bot pour cette partie et cette politique, ou {@code null} si les bots ne sont pas là. */
    static BotBridge of(Game game, String mode) {
        Class<?> type = botClass();
        if (type == null) return null;
        try {
            Constructor<?> make = type.getDeclaredConstructor(Game.class, double.class, String.class, long.class);
            make.setAccessible(true);
            Method act = type.getDeclaredMethod("act");
            act.setAccessible(true);
            return new BotBridge(make.newInstance(game, STEP, mode, SEED), act, mode);
        } catch (ReflectiveOperationException | RuntimeException incompatible) {
            return null;
        }
    }

    String mode() {
        return mode;
    }

    /** Une décision du bot. Faux si elle a échoué (le bot ne suit plus le jeu). */
    boolean act() {
        try {
            act.invoke(bot);
            return true;
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return false;
        }
    }
}
