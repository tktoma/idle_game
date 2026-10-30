import java.lang.reflect.Field;
import java.util.List;

/**
 * Les façons de jouer que le bot analyste compare (version 0.162). Chacune reprend les choix d'un des 24 profils (son
 * penchant, son ordre d'achat, ses choix du deuxième univers, du vide et de l'arbre), mais joués sans faute : à vitesse
 * humaine (une décision toutes les 5 s), avec toute son attention, sans oubli ni hésitation. Seuls les choix diffèrent ;
 * comparer leurs parties d'essai dit lequel mène le plus vite au palier suivant.
 */
final class AnalyseStrategies {

    private AnalyseStrategies() {}

    /** Une façon de jouer : son nom, ce qu'elle privilégie, et le profil dont elle prend les choix. */
    record Strategie(String nom, String idee, int profil) {

        /** Le profil sans faute : les choix de ce profil, au rythme et avec l'attention de l'analyste. */
        IdleProfiles24.Profile profile() {
            IdleProfiles24.Profile base = IdleProfiles24.PROFILES[profil - 1];
            return new IdleProfiles24.Profile(profil, nom, idee, BotAnalyste.ANALYSTE.actionEverySeconds(), 0, 0, 1, 1, 1, 1, 1, 0, base.bias());
        }
    }

    /**
     * Les façons de jouer comparées : celles des profils dont les choix diffèrent vraiment (les autres reprennent ceux-ci
     * avec moins d'attention ou moins de présence, ce que l'analyste n'a pas).
     */
    static final List<Strategie> TOUTES = List.of(
            new Strategie("Speedrunner", "l'ordre d'achat du modèle, deux passes de planification", 1),
            new Strategie("Méthodique", "économe : met en réserve, explose moins tôt", 4),
            new Strategie("Chimiste", "la synthèse et les recettes d'abord", 7),
            new Strategie("Astronome", "les astres et les systèmes d'abord", 8),
            new Strategie("Explorateur", "les sondes et le collisionneur d'abord", 9),
            new Strategie("Prestige", "explose dès que possible pour la matière noire", 11));

    /** Donne à ce bot les choix de cette façon de jouer, sans toucher à sa mémoire ni à sa partie. */
    static void appliquer(IdleProfiles24 bot, Strategie s) {
        IdleProfiles24.Profile profile = s.profile();
        RobotCosmosPlanner.EarlyStyle early = IdleProfiles24.EARLY_STYLES[s.profil() - 1];
        champ(bot, IdleProfiles24.class, "p", profile);
        bot.earlyStyle = early;
        NextUniversePolicy.Style next = IdleProfiles24.STYLES[s.profil() - 1];
        champ(bot.nextUniverse, NextUniversePolicy.class, "style", next);
        if (next.cycle() != null) champ(bot.nextUniverse, NextUniversePolicy.class, "cycle", next.cycle());
        champ(bot.voidPolicy, VoidPolicy.class, "style", IdleProfiles24.VOID_STYLES[s.profil() - 1]);
        champ(bot.tree, UniverseTreePolicy.class, "style", IdleProfiles24.treeStyle(profile, early));
    }

    private static void champ(Object target, Class<?> type, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException impossible) {
            throw new IllegalStateException("Impossible de changer " + type.getSimpleName() + "." + name, impossible);
        }
    }
}
