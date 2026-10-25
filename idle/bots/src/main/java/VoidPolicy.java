import idle.core.Game;
import idle.core.GameUniverse.VoidUpgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ce que les bots font du vide (version 0.133), qui se propose au premier Big Bang du troisième univers et de chaque
 * univers suivant : l'ouvrir contre de l'énergie du collisionneur, régler la part qu'il prend, acheter ses
 * améliorations en énergie du vide, nourrir son trou noir. Elle remplace le jeu du vide que la 0.132 avait mis dans
 * {@link NextUniversePolicy}. Comme celle-ci, une seule classe pour
 * tous les bots, appelée par {@link RobotCosmosPlanner#resourceLoop()}, avec une façon de jouer par profil
 * ({@link Style}, table {@code IdleProfiles24.VOID_STYLES}).
 *
 * <p>Tant que le vide ne se propose pas (univers 1 et 2), rien ne se passe ici et aucun hasard n'est tiré : les
 * parties des deux premiers univers sont celles d'avant. {@code -Didle.sansVide=true} laisse le vide fermé, pour
 * mesurer l'univers 3 sans lui.
 */
final class VoidPolicy {

    /** Comment le joueur dépense l'énergie du vide. */
    enum Plan {
        /**
         * L'expert : d'abord ce qui raccourcit le troisième acte (maturation, rassemblement, allègement), le moins cher
         * des trois ; une amélioration qui fait grandir le vide passe si elle coûte au plus {@link #GROWTH_SHARE} de
         * la prochaine des trois, si l'équilibre du vide n'atteint pas cette prochaine (sans croissance, elle ne
         * viendrait jamais), ou quand les trois sont au plus haut. La croissance choisie est celle qui lève la limite
         * du moment (rythme, énergie ou espace), le gain ou la fonte, la moins chère.
         */
        BEST,
        /** Le moins cher, quel qu'il soit. */
        CHEAPEST,
        /** N'importe laquelle de celles qu'il peut payer. */
        RANDOM
    }

    /**
     * La part d'une amélioration qui raccourcit l'acte 3 qu'une amélioration de croissance peut coûter, pour l'expert.
     * {@code -Didle.croissanceVide} la change pour les essais. Mesuré en version 0.135 sur l'univers 3, depuis
     * {@code checkpoint_universe2_speedrunner.save}. Sans les défis de Big Bang, quatre graines : le Speedrunner fait
     * 14 h 19 en moyenne avec 0,25, 13 h 05 avec 0,5, 14 h 18 avec 1 ; le Régulier 19 h 31, 19 h 05 et 19 h 02. Avec
     * eux, deux graines chacun, quatre parties : 49,9 h en tout avec 0,25, 47,2 h avec 0,5, 47,4 h avec 1 (le Régulier
     * gagne 1 à 2 h, le Speedrunner ne bouge pas). D'où 0,5.
     */
    static final double GROWTH_SHARE = Double.parseDouble(System.getProperty("idle.croissanceVide", "0.5"));
    /** La réserve d'énergie du collisionneur pour le vide ne dure pas plus que ce temps de jeu : ensuite, il achète. */
    static final double SAVING_MAX_SECONDS = 2 * 3600;
    /**
     * La masse au-delà de laquelle le joueur ne cherche plus à nourrir le trou noir (le premier sacrifice en donne ≈ 13) ;
     * {@code -Didle.masseTrouNoir} la change pour les essais (0 : jamais). Mesuré en 0.135 : sans sacrifice, l'univers 3
     * prend 8 à 20 minutes de plus ; 15 ou sans borne (1000) donnent exactement les parties de 10 (un seul sacrifice
     * vaut la peine).
     */
    static final double SACRIFICE_TARGET_MASS = Double.parseDouble(System.getProperty("idle.masseTrouNoir", "10"));
    /** Ce que le sacrifice doit ajouter à la masse du trou noir pour valoir la peine. */
    static final double SACRIFICE_MIN_GAIN = 0.5;
    /** Les améliorations qui raccourcissent le troisième acte. */
    static final List<VoidUpgrade> SHORTENERS = List.of(VoidUpgrade.MATURE, VoidUpgrade.SETTLE, VoidUpgrade.LIGHTEN);

    /**
     * La façon de jouer le vide d'un profil.
     *
     * @param open      chance d'ouvrir le vide, à chaque décision où il est payable
     * @param share     la part du collisionneur et de l'espace libre qu'il lui laisse (0,25 : le réglage du jeu)
     * @param plan      comment il dépense l'énergie du vide
     * @param buyCare   chance de passer par le vide, à chaque décision, pour y acheter
     * @param sacrifice chance de nourrir le trou noir quand ça vaut la peine (0 : jamais)
     */
    record Style(double open, double share, Plan plan, double buyCare, double sacrifice) {
        /** Le joueur expert : il ouvre dès que possible, laisse la moitié au vide, achète au mieux, nourrit le trou noir. */
        static final Style EXPERT = new Style(1, 0.5, Plan.BEST, 1, 1);
    }

    private final Style style;
    private final Random random;
    int opened = 0, bought = 0, sacrifices = 0;
    /** L'univers (son numéro) où la part a déjà été réglée. */
    private int shareSetFor = -1;
    /** Quand le vide s'est proposé, en temps de jeu, et dans quel univers. */
    private double offeredAt = -1;
    private int offeredIn = -1;

    VoidPolicy() {
        this(Style.EXPERT, new Random(0));
    }

    VoidPolicy(Style style, Random random) {
        this.style = style;
        this.random = random;
    }

    Style style() {
        return style;
    }

    static boolean disabled() {
        return Boolean.getBoolean("idle.sansVide");
    }

    /** Avec la propriété {@code idle.traceVide} (version 0.132), chaque geste du vide s'écrit : temps de jeu, geste, énergie du vide. */
    private static void trace(Game game, String what) {
        if (!Boolean.getBoolean("idle.traceVide")) return;
        System.out.printf(java.util.Locale.ROOT, "VIDE,%.3f,%s,%.0f%n", game.state().timePlayed() / 3600, what, game.voidEnergy().toDouble());
    }

    private boolean chance(double p) {
        if (p >= 1) return true;
        if (p <= 0) return false;
        return random.nextDouble() < p;
    }

    /**
     * Vrai tant que le joueur garde l'énergie du collisionneur pour ouvrir le vide : les achats du collisionneur
     * attendent (le prix vaut dix minutes de sa production).
     */
    boolean savingForVoid(Game game) {
        if (disabled() || style.open() <= 0 || !game.isVoidOffered()) return false;
        if (offeredIn != game.universes()) {
            offeredIn = game.universes();
            offeredAt = game.state().timePlayed();
        }
        // Un prix hors d'atteinte ne bloque pas le collisionneur indéfiniment.
        return game.state().timePlayed() - offeredAt <= SAVING_MAX_SECONDS;
    }

    /** Les gestes du vide, à chaque décision ; rien tant qu'il ne se propose pas. */
    int play(Game game) {
        if (disabled() || (!game.isVoidOffered() && !game.isVoidUnlocked())) {
            resumeFusion(game);   // un nouvel univers : la fusion coupée pour le trou noir repart
            return 0;
        }
        int done = 0;
        if (game.canOpenVoid() && chance(style.open()) && game.openVoid()) { opened++; done++; trace(game, "ouvert"); }
        if (!game.isVoidUnlocked()) return done;
        if (shareSetFor != game.universes()) {
            shareSetFor = game.universes();
            // -Didle.partVide=0.25 (par exemple) impose une part à tous les profils, pour les essais.
            String forced = System.getProperty("idle.partVide", "").trim();
            game.setVoidShare(forced.isEmpty() ? style.share() : Double.parseDouble(forced));
        }
        if (chance(style.buyCare())) done += buy(game);
        done += feedHole(game);
        return done;
    }

    /** Vrai quand le bot a coupé la fusion automatique pour laisser des particules au trou noir. */
    private boolean fusionPaused = false;

    /**
     * Nourrit le trou noir. La fusion (automatique et celle du bot) vide les particules : à une décision, il n'y en a
     * souvent plus. Le joueur qui veut nourrir le trou noir coupe alors la fusion automatique jusqu'à la décision
     * suivante, où les particules réunies entre-temps partent au trou noir ; la fusion repart aussitôt.
     */
    private int feedHole(Game game) {
        if (worthSacrifice(game)) {
            if (!chance(style.sacrifice())) return 0;
            boolean fed = game.sacrificeParticles();
            if (fed) { sacrifices++; trace(game, "trou noir, masse " + String.format(java.util.Locale.ROOT, "%.2f", game.voidMass())); }
            resumeFusion(game);
            return fed ? 1 : 0;
        }
        boolean wanted = game.isVoidHoleOpen() && game.bigBangs() >= 8 && game.voidMass() < SACRIFICE_TARGET_MASS;
        if (!wanted) {
            resumeFusion(game);
            return 0;
        }
        if (!fusionPaused && game.isAutomationEnabled("auto_fusion") && chance(style.sacrifice())) {
            game.setAutomationEnabled("auto_fusion", false);
            fusionPaused = true;
        }
        return 0;
    }

    private void resumeFusion(Game game) {
        if (!fusionPaused) return;
        game.setAutomationEnabled("auto_fusion", true);
        fusionPaused = false;
    }

    /**
     * Le trou noir prend toutes les particules : seulement une fois les huit Big Bangs faits (les bots n'en font pas
     * d'autre, les particules ne préparent plus rien), tant que la masse reste sous {@link #SACRIFICE_TARGET_MASS}, et
     * quand elle y gagne au moins {@link #SACRIFICE_MIN_GAIN}.
     */
    static boolean worthSacrifice(Game game) {
        return game.canSacrificeParticles() && game.bigBangs() >= 8 && game.voidMass() < SACRIFICE_TARGET_MASS
                && game.voidMassAfterSacrifice() - game.voidMass() >= SACRIFICE_MIN_GAIN;
    }

    /** Les achats d'une décision, selon le plan du joueur. */
    int buy(Game game) {
        int done = 0;
        for (int n = 0; n < 50; n++) {
            VoidUpgrade choice = switch (style.plan()) {
                case BEST -> best(game);
                case CHEAPEST -> cheapest(game, List.of(VoidUpgrade.values()));
                case RANDOM -> anyAffordable(game);
            };
            if (choice == null || !game.buyVoid(choice)) break;
            bought++;
            trace(game, choice.name() + " " + game.voidLevel(choice));
            done++;
        }
        return done;
    }

    private static boolean open(Game game, VoidUpgrade upgrade) {
        return game.voidLevel(upgrade) < upgrade.max();
    }

    /** Le moins cher de ces améliorations qu'on peut payer, ou {@code null}. */
    private static VoidUpgrade cheapest(Game game, List<VoidUpgrade> among) {
        VoidUpgrade best = null;
        for (VoidUpgrade upgrade : among) {
            if (!game.canBuyVoid(upgrade)) continue;
            if (best == null || game.voidCost(upgrade) < game.voidCost(best)) best = upgrade;
        }
        return best;
    }

    /** L'expert : le raccourci le moins cher ; une croissance seulement si elle est bon marché à côté. */
    private static VoidUpgrade best(Game game) {
        VoidUpgrade shortener = cheapest(game, SHORTENERS);
        if (shortener != null) return shortener;
        double next = Double.POSITIVE_INFINITY;
        for (VoidUpgrade upgrade : SHORTENERS) if (open(game, upgrade)) next = Math.min(next, game.voidCost(upgrade));
        // La croissance utile : la limite du moment, le gain, la fonte.
        VoidUpgrade limit = switch (game.voidLimit()) {
            case "énergie" -> VoidUpgrade.ENERGY;
            case "espace" -> VoidUpgrade.SPACE;
            default -> VoidUpgrade.RATE;
        };
        VoidUpgrade cheap = cheapest(game, List.of(limit, VoidUpgrade.GAIN, VoidUpgrade.DECAY));
        if (cheap == null) return null;
        boolean stuck = game.voidEquilibrium() < next;
        return Double.isInfinite(next) || stuck || game.voidCost(cheap) <= GROWTH_SHARE * next ? cheap : null;
    }

    private VoidUpgrade anyAffordable(Game game) {
        List<VoidUpgrade> affordable = new ArrayList<>();
        for (VoidUpgrade upgrade : VoidUpgrade.values()) if (game.canBuyVoid(upgrade)) affordable.add(upgrade);
        return affordable.isEmpty() ? null : affordable.get(random.nextInt(affordable.size()));
    }
}
