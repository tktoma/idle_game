package idle.ui;

import idle.core.Game;
import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.profile.Title;
import idle.core.universe.SkinVisual;
import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Ce que le joueur a déjà vu de la partie (version 0.318) : les actes et les grandes ouvertures. Ce qui n'a jamais été
 * débloqué ne se montre nulle part hors du jeu lui-même (réglages, skins, profil, statistiques, succès) : ni nom, ni
 * indice, ni explication, pour ne rien gâcher de la suite.
 * <p>Une étape vue le reste : la partie peut la refermer (le vide et les hadrons repartent de zéro à chaque univers,
 * « Recommencer de zéro » efface tout), le souvenir est gardé dans les réglages ({@link Memory}).
 */
public enum Seen {
    /** Le deuxième acte : la matière noire, gagnée par une explosion. */
    EXPLOSION(game -> game.isDarkMatterUnlocked()),
    /** Le collisionneur, ouvert avec la matière noire. */
    COLLIDER(game -> game.isColliderUnlocked()),
    /** Le troisième acte : le premier Big Bang, les molécules, l'espace, les comètes. */
    BIG_BANG(game -> game.isBigBangUnlocked()),
    /** Les sondes, un palier du Big Bang. */
    PROBES(game -> game.isProbesUnlocked()),
    /** Les systèmes solaires, un palier du Big Bang. */
    SYSTEMS(game -> game.isSolarSystemsUnlocked()),
    /** La galaxie formée. */
    GALAXY(game -> game.hasGalaxy()),
    /** Le quatrième acte : un univers formé, la graine, les dimensions, les skins. */
    UNIVERSE(game -> game.universes() > 0),
    /** Les hadrons. */
    HADRONS(game -> game.isHadronsUnlocked()),
    /** Le vide. */
    VOID(game -> game.isVoidUnlocked()),
    /** Le chrono de l'étagère. */
    CHRONO(game -> game.isChronoUnlocked()),
    /** Le tuyau d'arrosage. */
    HOSE(game -> game.hasHose()),
    /** La météo de la graine. */
    WEATHER(game -> game.isWeatherUnlocked()),
    /** Le rond de croissance. */
    BLOOM(game -> game.isBloomUnlocked()),
    /** Les dimensions, une par univers (la ligne d'abord) : {@link #dimension(int)}. */
    DIMENSION_2(game -> game.dimensionsUnlocked() >= 2),
    DIMENSION_3(game -> game.dimensionsUnlocked() >= 3),
    DIMENSION_4(game -> game.dimensionsUnlocked() >= 4),
    DIMENSION_5(game -> game.dimensionsUnlocked() >= 5),
    DIMENSION_6(game -> game.dimensionsUnlocked() >= 6),
    DIMENSION_7(game -> game.dimensionsUnlocked() >= 7),
    DIMENSION_8(game -> game.dimensionsUnlocked() >= 8),
    DIMENSION_9(game -> game.dimensionsUnlocked() >= 9),
    DIMENSION_10(game -> game.dimensionsUnlocked() >= 10),
    /** Le défi final. */
    FINAL(game -> game.isFinalChallengeOpen() || game.isGameFinished());

    private final Predicate<Game> reached;

    Seen(Predicate<Game> reached) {
        this.reached = reached;
    }

    /** Vrai si la partie l'a ouverte en ce moment (sans le souvenir). */
    public boolean in(Game game) { return reached.test(game); }

    /** L'étape d'une dimension : la première vient avec le premier univers. */
    public static Seen dimension(int number) {
        return number <= 1 ? UNIVERSE : valueOf("DIMENSION_" + Math.min(10, number));
    }

    // ------------------------------------------------------------------
    // Ce que chaque chose montrée hors du jeu demande d'avoir vu ; null : rien, elle se montre toujours.
    // ------------------------------------------------------------------

    /** Une catégorie de notifications. */
    public static Seen of(Notice notice) {
        return switch (notice) {
            case EXPLOSION, CHALLENGES -> EXPLOSION;
            case COLLIDER -> COLLIDER;
            case BIG_BANG, MOLECULES, SKY, AUTO_SKY, COMETS, SKY_EVENTS -> BIG_BANG;
            case PROBES -> PROBES;
            case VOID -> VOID;
            case FINAL -> FINAL;
            default -> null;
        };
    }

    /** Une sorte de pastille d'onglet. */
    public static Seen of(Readiness.Kind kind) {
        return switch (kind) {
            case TREE, CHALLENGES -> EXPLOSION;
            case COLLIDER -> COLLIDER;
            case MILESTONES, SKY, COMET -> BIG_BANG;
            case PROBES -> PROBES;
            case SYSTEMS -> SYSTEMS;
            case VOID -> VOID;
            case HADRONS -> HADRONS;
        };
    }

    /** Une sorte de son. */
    public static Seen of(SoundKind kind) {
        return switch (kind) {
            case DARK_MATTER, EXPLOSION -> EXPLOSION;
            case BIG_BANG, EXPANSION -> BIG_BANG;
            case UNIVERSE, DIMENSIONS, SEED -> UNIVERSE;
            case COLLIDER -> COLLIDER;
            case HADRONS -> HADRONS;
            case VOID -> VOID;
            case PROBES -> PROBES;
            case CHRONO -> CHRONO;
            default -> null;
        };
    }

    /** Un morceau de la ligne d'objectif. */
    public static Seen of(Goals.Part part) {
        return switch (part) {
            case BIG_BANG -> EXPLOSION;
            case ACT -> BIG_BANG;
            default -> null;
        };
    }

    /** Un thème ou une notation à débloquer : son indice parle de ce qu'il faut avoir vu. */
    public static Seen of(Cosmetic cosmetic) {
        return switch (cosmetic) {
            case PHOSPHOR, INFRARED, GOLD -> COLLIDER;
            case NEBULA, HEX, CYANOTYPE, BLAZE -> EXPLOSION;
            case OLD_FILM, MOON, JAPANESE -> BIG_BANG;
            case THERMAL, FOREST -> SYSTEMS;
            case HEARTBEATS -> CHRONO;
            default -> null;
        };
    }

    /** Un visuel de l'onglet Skins. */
    public static Seen of(SkinVisual visual) {
        return switch (visual) {
            case COLLIDER_BEAM, COLLIDER, DETECTOR, LIGHTNING, SPARKS, QUARK_FLIGHT -> COLLIDER;
            case QUARKS, GLUONS, HADRON_BACKGROUND -> HADRONS;
            case VOID_FOAM, VOID_PAIRS, VOID_HOLE -> VOID;
            case PROBE -> PROBES;
            case SYSTEM -> SYSTEMS;
            case LINE -> dimension(1);
            case PLANE -> dimension(2);
            case VOLUME -> dimension(3);
            case DURATION -> dimension(4);
            case LOOP -> dimension(5);
            case BRANE -> dimension(6);
            case CLICKER -> dimension(7);
            case GRID -> dimension(8);
            case MIRROR -> dimension(9);
            case MIX -> dimension(10);
            case CHRONO -> CHRONO;
            case HOSE -> HOSE;
            case WEATHER -> WEATHER;
            case GROWTH_RING -> BLOOM;
            case FINALE_FILL -> FINAL;
            default -> null;
        };
    }

    /** Un titre du profil : ce que dit sa condition. */
    public static Seen of(Title title) {
        return switch (title) {
            case SHADOW_KEEPER, CHALLENGER -> EXPLOSION;
            case QUARK_HUNTER, TOP_LORD -> COLLIDER;
            case BANG_MAKER, MOLECULE_BLOWER, COMET_CHASER, STAR_SMITH, VOID_WATCHER, GALAXY_ARCHITECT -> BIG_BANG;
            case PLANET_GARDENER -> SYSTEMS;
            case CLUSTER_BUILDER, UNIVERSE_CREATOR -> GALAXY;
            case MULTIVERSE_WEAVER, SEED_GARDENER, WORLD_TREE_KEEPER, SKIN_COLLECTOR -> UNIVERSE;
            case LINE_WALKER -> dimension(1);
            case PLANE_WANDERER -> dimension(2);
            case VOLUME_DIVER -> dimension(3);
            case TIME_KEEPER -> dimension(4);
            case LOOP_DANCER -> dimension(5);
            case BRANE_SURFER -> dimension(6);
            case CLICK_VIRTUOSO -> dimension(7);
            case GRID_SEEKER -> dimension(8);
            case MIRROR_KEEPER -> dimension(9);
            case MIX_MASTER -> dimension(10);
            case WORLD_ROOTS -> FINAL;
            case VOID_TAMER, STAR_EATER -> VOID;
            case GLUON_WEAVER, EXOTIC_HUNTER -> HADRONS;
            default -> null;
        };
    }

    /**
     * Un succès de la liste ordinaire : ils y sont rangés dans l'ordre de la partie, l'explosion, le premier molécule,
     * l'univers, puis ceux des univers suivants.
     */
    public static Seen of(Achievement achievement) {
        String id = achievement.id();
        if (id.startsWith("late_void")) return VOID;
        if (id.equals("late_hadrons") || id.equals("late_exotic")) return HADRONS;
        if (id.equals("late_first_final") || id.equals("late_roots") || id.equals("late_five_finals") || id.equals("late_end")) return FINAL;
        if (id.startsWith("late_") || id.startsWith("univ_") || id.equals("step_universe_cluster")) return UNIVERSE;
        int index = Achievements.DEFAULT.indexOf(achievement);
        if (index < 0) return UNIVERSE;
        if (index >= position("step_universe")) return GALAXY;
        if (index >= position("step_first_molecule")) return BIG_BANG;
        if (index >= position("step_explosion")) return EXPLOSION;
        return null;
    }

    /**
     * Ce que nomme une condition écrite (celle d'un skin : « 10 voyages de sonde », « La partie finie ») : chaque étape
     * dont un mot y figure. Les mots sont ceux du jeu ; une condition qui n'en nomme aucun ne demande rien.
     */
    public static EnumSet<Seen> mentioned(String text) {
        String low = text.toLowerCase(java.util.Locale.ROOT);
        EnumSet<Seen> steps = EnumSet.noneOf(Seen.class);
        if (low.contains("sonde")) steps.add(PROBES);
        if (low.contains("système")) steps.add(SYSTEMS);
        if (low.contains("hadron") || low.contains("gluon")) steps.add(HADRONS);
        if (low.contains("du vide") || low.contains("le vide") || low.contains("paires nées")) steps.add(VOID);
        if (low.contains("chrono")) steps.add(CHRONO);
        if (low.contains("tuyau")) steps.add(HOSE);
        if (low.contains("orage") || low.contains("saison")) steps.add(WEATHER);
        if (low.contains("rond") && low.contains("croissance")) steps.add(BLOOM);
        if (low.contains("partie finie") || low.contains("défi final")) steps.add(FINAL);
        return steps;
    }

    private static int position(String id) {
        for (int i = 0; i < Achievements.DEFAULT.size(); i++) if (Achievements.DEFAULT.get(i).id().equals(id)) return i;
        return Integer.MAX_VALUE;
    }

    /**
     * Le souvenir de ce qui a été vu. Il se complète à chaque {@link #note(Game)} ; une étape plus loin dans la partie
     * vaut pour celles d'avant (un univers formé : l'explosion et le Big Bang ont été vus).
     */
    public static final class Memory {
        private final EnumSet<Seen> seen;
        private final Runnable onChange;

        /** {@code seen} : le souvenir gardé (repris tel quel) ; {@code onChange} : appelé quand il grandit, pour l'écrire. */
        public Memory(EnumSet<Seen> seen, Runnable onChange) {
            this.seen = seen;
            this.onChange = onChange;
        }

        /** Un souvenir vide, qui ne s'écrit nulle part : seule la partie en cours compte. */
        public static Memory none() { return new Memory(EnumSet.noneOf(Seen.class), () -> {}); }

        /** Retient ce que la partie montre en ce moment. */
        public void note(Game game) {
            boolean grew = false;
            for (Seen step : values()) if (step.in(game) && seen.add(step)) grew = true;
            if (seen.contains(UNIVERSE)) grew |= seen.addAll(EnumSet.of(EXPLOSION, COLLIDER, BIG_BANG, GALAXY));
            if (seen.contains(GALAXY) || seen.contains(PROBES) || seen.contains(SYSTEMS)) grew |= seen.addAll(EnumSet.of(EXPLOSION, BIG_BANG));
            if (seen.contains(BIG_BANG)) grew |= seen.add(EXPLOSION);
            if (grew) onChange.run();
        }

        /** Vrai si cette étape a été vue : maintenant ou une fois. */
        public boolean has(Seen step) { return seen.contains(step); }

        /** Vrai si la partie en cours la montre, ou si elle a été vue une fois. */
        public boolean has(Seen step, Game game) {
            if (step.in(game)) note(game);
            return seen.contains(step);
        }

        /** Le souvenir, pour l'écrire. */
        public EnumSet<Seen> all() { return EnumSet.copyOf(seen); }
    }
}
