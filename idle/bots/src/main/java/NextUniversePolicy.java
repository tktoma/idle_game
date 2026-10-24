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
 * gratuits, chacun à la façon du profil ({@link Style}, version 0.131) ; la fuite, onzième défi, passe par la boucle des défis de {@link RobotCosmosPlanner}. Ce qu'elle ne joue
 * pas, pour l'instant : les dimensions, la graine, l'onglet Univers (améliorations, recherches, épreuves, skins).
 * Comme la graine n'est pas jouée, l'univers suivant se lance comme l'outil d'essai « Rouvrir les paliers »
 * ({@link #launchNext}), sans attendre le palier de la graine qu'un joueur doit atteindre.
 */
final class NextUniversePolicy {

    /**
     * Le choix de cycle du joueur expert. Mesuré en version 0.130 sur le deuxième univers, depuis
     * {@code checkpoint_universe1_speedrunner.save} : le Speedrunner fait 21 h 08 (synthèse), 21 h 20 (fusion) et
     * 21 h 26 (réserve), écarts dans le bruit ; le Régulier, sur trois graines, 23 h 26 à 23 h 51 avec la fusion contre
     * 23 h 34 à 25 h 12 avec la synthèse (26 h 19 avec la réserve, une graine). La fusion est la plus régulière. La
     * propriété {@code idle.cycle} (FUSION, SYNTHESIS, RESERVE) remplace le choix de tous les profils pour les essais.
     */
    static final CycleChoice DEFAULT_CYCLE = CycleChoice.FUSION;
    /** Décisions de suite où la famille qui manque diffère de la famille préférée avant d'en changer. */
    static final int FAMILY_PATIENCE = 10;

    /** Ce que fait le joueur de la famille préférée. */
    enum Family {
        /** Elle suit la famille qui manque aux recettes ; il en change après {@link #FAMILY_PATIENCE} désaccords. */
        FOLLOW,
        /** Il la choisit une fois, la première qui manque, et n'y revient plus. */
        ONCE,
        /** Il ne s'en sert pas. */
        NONE
    }

    /** Quel élément offert il prend. */
    enum Pick {
        /** Celui qui manque aux recettes de la liste, sinon le plus lourd pas encore découvert (le plus rare). */
        NEEDED,
        /** Le premier de la liste : le plus léger pas encore découvert. */
        FIRST,
        /** N'importe lequel pas encore découvert. */
        RANDOM
    }

    /**
     * La façon de jouer le deuxième univers d'un profil (version 0.131). Le régime rapide, geste répété, se joue à
     * chaque décision où il est prêt, avec sa chance. Le choix de cycle et les éléments offerts se jouent une fois par
     * occasion : le joueur qui n'y pense pas laisse passer tout le cycle (jusqu'au Big Bang suivant), ou la fournée
     * d'éléments (jusqu'aux dix éléments découverts suivants).
     *
     * @param cycle     son choix de cycle ; {@code null} : au hasard, à chaque cycle
     * @param regime    chance de relancer le régime rapide quand il est prêt (0 : il l'oublie)
     * @param cycleCare chance de faire le choix de cycle d'un cycle (sinon, il l'oublie jusqu'au Big Bang suivant)
     * @param family    ce qu'il fait de la famille préférée
     * @param freeCare  chance de prendre une fournée d'éléments offerts (sinon, il la laisse)
     * @param pick      quel élément offert il prend
     */
    record Style(CycleChoice cycle, double regime, double cycleCare, Family family, double freeCare, Pick pick) {
        /** Le joueur expert : tout, toujours, au mieux. Le Speedrunner, le bot V2 et les bots sans profil jouent ainsi. */
        static final Style EXPERT = new Style(DEFAULT_CYCLE, 1, 1, Family.FOLLOW, 1, Pick.NEEDED);
    }

    private final Style style;
    private final CycleChoice cycle;
    private final java.util.Random random;
    private int familyDisagreements = 0;
    /** L'occasion de choix de cycle et la fournée d'éléments offerts déjà tranchées (on n'y revient pas). */
    private long cycleOccasion = -1, freeOccasion = -1;
    int regimes = 0, cycles = 0, families = 0, freeElements = 0, voidOpened = 0, voidUpgrades = 0, voidSacrifices = 0;

    NextUniversePolicy() {
        this(Style.EXPERT, new java.util.Random(0));
    }

    /** L'expert, avec ce choix de cycle-là, quelle que soit la propriété {@code idle.cycle} : pour les essais. */
    NextUniversePolicy(CycleChoice cycle) {
        this(new Style(cycle, 1, 1, Family.FOLLOW, 1, Pick.NEEDED), new java.util.Random(0), false);
    }

    /**
     * @param style  la façon de jouer du profil
     * @param random son propre hasard : les gestes du deuxième univers ne déplacent pas les tirages du profil
     */
    NextUniversePolicy(Style style, java.util.Random random) {
        this(style, random, true);
    }

    private NextUniversePolicy(Style style, java.util.Random random, boolean configurable) {
        this.style = style;
        CycleChoice forced = configurable ? configuredCycle() : null;
        this.cycle = forced != null ? forced : style.cycle();
        this.random = random;
    }

    Style style() {
        return style;
    }

    /** Vrai avec cette chance ; ne tire rien quand elle vaut 0 ou 1, pour que l'expert ne consomme aucun hasard. */
    private boolean chance(double p) {
        if (p >= 1) return true;
        if (p <= 0) return false;
        return random.nextDouble() < p;
    }

    static CycleChoice configuredCycle() {
        String wanted = System.getProperty("idle.cycle", "").trim();
        if (wanted.isEmpty()) return null;
        try {
            return CycleChoice.valueOf(wanted.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("idle.cycle=" + wanted + " : FUSION, SYNTHESIS ou RESERVE attendu", unknown);
        }
    }

    /** Le choix de cycle du profil, ou {@code null} quand il le tire au hasard. */
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

    /** Les gestes du deuxième univers, à chaque décision, selon la façon de jouer du profil ; rien dans le premier. */
    int play(Game game) {
        if (!game.isSecondUniverse() || disabled()) return 0;
        int done = 0;
        // Le régime rapide se relance à la main : à chaque décision où il est prêt, si le joueur y pense.
        if (game.canStartRegime() && chance(style.regime()) && game.startRegime()) { regimes++; done++; }
        // Un choix par cycle, rendu au Big Bang.
        // Une seule fois par cycle : celui qui n'y pense pas laisse passer le cycle entier.
        long cycleNow = game.universes() * 1000L + game.bigBangs();
        if (game.isCycleChoiceUnlocked() && game.cycleChoice() == null && cycleNow != cycleOccasion) {
            cycleOccasion = cycleNow;
            if (chance(style.cycleCare())) {
                CycleChoice choice = cycle != null ? cycle : CycleChoice.values()[random.nextInt(CycleChoice.values().length)];
                if (game.chooseCycle(choice)) { cycles++; done++; }
            }
        }
        done += preferFamily(game);
        // Une seule fois par fournée : dix éléments découverts de plus, ou une nouvelle explosion.
        long freeNow = (game.universes() * 1000L + game.bigBangs()) * 1_000_000L + game.state().explosions() * 100L
                + game.discoveredElements() / Game.FREE_ELEMENT_EVERY;
        if (game.freeElementsAvailable() > 0 && freeNow != freeOccasion) {
            freeOccasion = freeNow;
            if (chance(style.freeCare())) done += claimFreeElements(game);
        }
        done += playVoid(game);
        return done;
    }

    /** La part du collisionneur et de l'espace libre que le bot laisse au vide (version 0.132). */
    static final double VOID_SHARE = 0.5;
    /** Le trou noir du vide ne reçoit les particules que si sa masse y gagne au moins autant. */
    static final double VOID_MASS_STEP = 0.25;

    /**
     * Le vide (troisième univers, version 0.132) : l'ouvrir dès que l'énergie du collisionneur le paie, lui laisser la
     * moitié du collisionneur et de l'espace libre, acheter d'abord ce qui raccourcit le troisième acte (maturation,
     * rassemblement, allègement du ciel), puis l'amélioration la moins chère ; sacrifier les particules en main au
     * trou noir quand sa masse y gagne vraiment. La propriété {@code idle.sansVide} le coupe, pour mesurer sans lui.
     */
    int playVoid(Game game) {
        if (Boolean.getBoolean("idle.sansVide")) return 0;
        int done = 0;
        if (game.isVoidOffered() && game.canOpenVoid() && game.openVoid()) {
            game.setVoidShare(VOID_SHARE);
            voidOpened++;
            trace(game, "ouvert");
            done++;
        }
        if (!game.isVoidUnlocked()) return done;
        boolean bought = true;
        while (bought) {
            bought = false;
            Game.VoidUpgrade pick = null;
            for (Game.VoidUpgrade first : VOID_FIRST) {
                if (game.canBuyVoid(first)) { pick = first; break; }
            }
            if (pick == null) {
                for (Game.VoidUpgrade upgrade : Game.VoidUpgrade.values()) {
                    if (game.canBuyVoid(upgrade) && (pick == null || game.voidCost(upgrade) < game.voidCost(pick))) pick = upgrade;
                }
            }
            if (pick != null && game.buyVoid(pick)) {
                voidUpgrades++;
                done++;
                bought = true;
                trace(game, pick.name() + " " + game.voidLevel(pick));
            }
        }
        if (game.canSacrificeParticles() && game.voidMassAfterSacrifice() >= game.voidMass() + VOID_MASS_STEP && game.sacrificeParticles()) {
            voidSacrifices++;
            done++;
        }
        return done;
    }

    /** Avec la propriété {@code idle.traceVide}, chaque geste du vide s'écrit : temps de jeu, geste, énergie du vide. */
    private static void trace(Game game, String what) {
        if (!Boolean.getBoolean("idle.traceVide")) return;
        System.out.printf(Locale.ROOT, "VIDE,%.3f,%s,%.0f%n", game.state().timePlayed() / 3600, what, game.voidEnergy().toDouble());
    }

    /** Ce que le bot achète d'abord dans le vide : ce qui raccourcit les attentes du troisième acte. */
    static final Game.VoidUpgrade[] VOID_FIRST = {Game.VoidUpgrade.MATURE, Game.VoidUpgrade.SETTLE, Game.VoidUpgrade.LIGHTEN};

    /**
     * La famille préférée suit la famille qui rationne les recettes (la même que vise la synthèse ciblée). Changer
     * remet la préférence à zéro : on n'en change qu'après {@link #FAMILY_PATIENCE} décisions de désaccord. Le joueur
     * {@link Family#ONCE} la choisit une fois, la première famille qui manque, et n'y touche plus.
     */
    int preferFamily(Game game) {
        if (style.family() == Family.NONE || !game.isFamilyPreferenceUnlocked()) return 0;
        ElementCategory current = game.preferredFamily();
        if (style.family() == Family.ONCE && current != null) return 0;
        ElementCategory wanted = FirstUniversePolicy.chemistryTarget(game);
        if (wanted == null) return 0;
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

    /** Les éléments offerts, tous ceux qui attendent, choisis selon {@link Style#pick()}. */
    int claimFreeElements(Game game) {
        int done = 0;
        while (game.freeElementsAvailable() > 0) {
            int choice = switch (style.pick()) {
                case NEEDED -> {
                    int needed = neededUndiscovered(game);
                    yield needed >= 0 ? needed : heaviestUndiscovered(game);
                }
                case FIRST -> lightestUndiscovered(game);
                case RANDOM -> randomUndiscovered(game);
            };
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

    private static int lightestUndiscovered(Game game) {
        for (var element : PeriodicTable.ELEMENTS) {
            if (game.elementCount(element.number()) == 0) return element.number();
        }
        return -1;
    }

    private int randomUndiscovered(Game game) {
        java.util.List<Integer> left = new java.util.ArrayList<>();
        for (var element : PeriodicTable.ELEMENTS) if (game.elementCount(element.number()) == 0) left.add(element.number());
        return left.isEmpty() ? -1 : left.get(random.nextInt(left.size()));
    }

    private static int heaviestUndiscovered(Game game) {
        int best = -1;
        for (var element : PeriodicTable.ELEMENTS) {
            if (game.elementCount(element.number()) == 0 && element.number() > best) best = element.number();
        }
        return best;
    }
}
