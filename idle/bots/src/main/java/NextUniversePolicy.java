import idle.core.Game;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.universe.CycleChoice;
import java.util.Locale;
import java.util.Map;

/**
 * Ce que les bots font en plus à partir du deuxième univers (version 0.130), par-dessus {@link FirstUniversePolicy}
 * et {@link RobotCosmosPlanner}, qui restent la base des trois actes. Une seule classe pour tous les bots : le bot
 * V2, les 24 profils et l'audit depuis zéro l'appellent par {@link RobotCosmosPlanner#resourceLoop()}.
 *
 * <p>Dans le premier univers, rien ne s'y passe : chaque geste attend {@link Game#isSecondUniverse()}. Les parties
 * du premier univers sont donc exactement celles d'avant (même hasard, mêmes décisions).
 *
 * <p>Ce que la politique joue : le régime rapide, le choix de cycle, la préférence de famille et les éléments
 * gratuits ; la fuite, onzième défi, passe par la boucle des défis de {@link RobotCosmosPlanner}. Ce qu'elle ne joue
 * pas, pour l'instant : les dimensions, la graine, l'onglet Univers (améliorations, recherches, épreuves, skins).
 * Comme la graine n'est pas jouée, l'univers suivant se lance comme l'outil d'essai « Rouvrir les paliers »
 * ({@link #launchNext}), sans attendre le palier de la graine qu'un joueur doit atteindre.
 */
final class NextUniversePolicy {

    /**
     * Le choix de cycle par défaut. Mesuré en version 0.130 sur le deuxième univers, depuis
     * {@code checkpoint_universe1_speedrunner.save} : le Speedrunner fait 21 h 08 (synthèse), 21 h 20 (fusion) et
     * 21 h 26 (réserve), écarts dans le bruit ; le Régulier, sur trois graines, 23 h 26 à 23 h 51 avec la fusion contre
     * 23 h 34 à 25 h 12 avec la synthèse (26 h 19 avec la réserve, une graine). La fusion est la plus régulière. La
     * propriété {@code idle.cycle} (FUSION, SYNTHESIS, RESERVE) le remplace pour les essais.
     */
    static final CycleChoice DEFAULT_CYCLE = CycleChoice.FUSION;
    /** Décisions de suite où la famille qui manque diffère de la famille préférée avant d'en changer. */
    static final int FAMILY_PATIENCE = 10;

    private final CycleChoice cycle;
    private int familyDisagreements = 0;
    int regimes = 0, cycles = 0, families = 0, freeElements = 0;

    NextUniversePolicy() {
        this(configuredCycle());
    }

    NextUniversePolicy(CycleChoice cycle) {
        this.cycle = cycle;
    }

    static CycleChoice configuredCycle() {
        String wanted = System.getProperty("idle.cycle", "").trim();
        if (wanted.isEmpty()) return DEFAULT_CYCLE;
        try {
            return CycleChoice.valueOf(wanted.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("idle.cycle=" + wanted + " : FUSION, SYNTHESIS ou RESERVE attendu", unknown);
        }
    }

    CycleChoice cycle() {
        return cycle;
    }

    /** Vrai une fois {@code target} univers formés, hors de la partie qui mène au suivant. */
    static boolean reached(Game game, int target) {
        return game.universes() >= target && !game.isReplaying() && game.isUniverseFormed();
    }

    /**
     * Lance l'univers suivant si l'univers vient d'être formé et qu'il en faut encore : les trois actes repartent de
     * zéro. Le moteur le permet dès l'univers formé ; l'interface attend en plus le palier de la graine, que les bots
     * ne jouent pas.
     */
    static boolean launchNext(Game game, int target) {
        if (game.universes() >= target || !game.canStartNextUniverse()) return false;
        return game.startNextUniverse();
    }

    /**
     * Vrai pour mesurer l'univers 2 sans ses nouveautés (propriété {@code idle.sansNouveautesUnivers2}) : les bots
     * rejouent alors les trois actes comme dans le premier univers.
     */
    static boolean disabled() {
        return Boolean.getBoolean("idle.sansNouveautesUnivers2");
    }

    /** Les gestes du deuxième univers, à chaque décision ; rien dans le premier. */
    int play(Game game) {
        if (!game.isSecondUniverse() || disabled()) return 0;
        int done = 0;
        // Le régime rapide se relance à la main : à chaque décision où il est prêt.
        if (game.canStartRegime() && game.startRegime()) { regimes++; done++; }
        // Un choix par cycle, rendu au Big Bang.
        if (game.isCycleChoiceUnlocked() && game.cycleChoice() == null && game.chooseCycle(cycle)) { cycles++; done++; }
        done += preferFamily(game);
        done += claimFreeElements(game);
        return done;
    }

    /**
     * La famille préférée suit la famille qui rationne les recettes (la même que vise la synthèse ciblée). Changer
     * remet la préférence à zéro : on n'en change qu'après {@link #FAMILY_PATIENCE} décisions de désaccord.
     */
    int preferFamily(Game game) {
        if (!game.isFamilyPreferenceUnlocked()) return 0;
        ElementCategory wanted = FirstUniversePolicy.chemistryTarget(game);
        if (wanted == null) return 0;
        ElementCategory current = game.preferredFamily();
        if (current == wanted) {
            familyDisagreements = 0;
            return 0;
        }
        if (current != null && ++familyDisagreements < FAMILY_PATIENCE) return 0;
        familyDisagreements = 0;
        if (!game.setPreferredFamily(wanted)) return 0;
        families++;
        return 1;
    }

    /** Les éléments gratuits : d'abord ceux qui manquent aux recettes de la liste, sinon le plus lourd pas encore découvert. */
    int claimFreeElements(Game game) {
        int done = 0;
        while (game.freeElementsAvailable() > 0) {
            int choice = neededUndiscovered(game);
            if (choice < 0) choice = heaviestUndiscovered(game);
            if (choice < 0 || !game.claimFreeElement(choice)) break;
            freeElements++;
            done++;
        }
        return done;
    }

    private static int neededUndiscovered(Game game) {
        for (var item : game.shoppingList().items()) {
            for (Map.Entry<Integer, Integer> need : game.nextMoleculeCost(item.molecule().id()).entrySet()) {
                if (need.getValue() > 0 && game.elementCount(need.getKey()) == 0) return need.getKey();
            }
        }
        return -1;
    }

    private static int heaviestUndiscovered(Game game) {
        int best = -1;
        for (var element : PeriodicTable.ELEMENTS) {
            if (game.elementCount(element.number()) == 0 && element.number() > best) best = element.number();
        }
        return best;
    }
}
