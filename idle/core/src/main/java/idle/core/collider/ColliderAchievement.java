package idle.core.collider;

/**
 * Les succès du collisionneur (version 0.060) : ils se gagnent en achetant des générateurs et en
 * enchaînant les collisions, et chacun renforce les générateurs d'une façon. Pour chaque
 * générateur, un premier succès double sa production, un second divise ses prix par deux ; trois
 * succès d'ensemble renforcent l'énergie, tous les générateurs, et la chance de collision. Ils se
 * déduisent de ce qui a été acheté : rien ne les reprend, sauf la remise à zéro.
 */
public enum ColliderAchievement {
    BEAM_OUTPUT(ColliderGenerator.BEAM, Kind.OUTPUT, 10),
    BEAM_COST(ColliderGenerator.BEAM, Kind.COST, 30),
    MAGNET_OUTPUT(ColliderGenerator.MAGNET, Kind.OUTPUT, 10),
    MAGNET_COST(ColliderGenerator.MAGNET, Kind.COST, 30),
    CAVITY_OUTPUT(ColliderGenerator.CAVITY, Kind.OUTPUT, 10),
    CAVITY_COST(ColliderGenerator.CAVITY, Kind.COST, 30),
    SYNCHROTRON_OUTPUT(ColliderGenerator.SYNCHROTRON, Kind.OUTPUT, 10),
    SYNCHROTRON_COST(ColliderGenerator.SYNCHROTRON, Kind.COST, 30),
    STORAGE_OUTPUT(ColliderGenerator.STORAGE, Kind.OUTPUT, 5),
    STORAGE_COST(ColliderGenerator.STORAGE, Kind.COST, 15),
    LINAC_OUTPUT(ColliderGenerator.LINAC, Kind.OUTPUT, 5),
    LINAC_COST(ColliderGenerator.LINAC, Kind.COST, 15),
    SUPERCOLLIDER_OUTPUT(ColliderGenerator.SUPERCOLLIDER, Kind.OUTPUT, 5),
    SUPERCOLLIDER_COST(ColliderGenerator.SUPERCOLLIDER, Kind.COST, 15),
    PLANCK_OUTPUT(ColliderGenerator.PLANCK, Kind.OUTPUT, 5),
    PLANCK_COST(ColliderGenerator.PLANCK, Kind.COST, 15),
    /** 100 générateurs achetés, toutes sortes confondues : l'énergie ×1,5. */
    HUNDRED(null, Kind.ENERGY, 100),
    /** 250 générateurs achetés : tous les générateurs produisent ×1,5. */
    QUARTER_THOUSAND(null, Kind.ALL_OUTPUT, 250),
    /** 1 000 collisions : la chance de collision +2 points. */
    THOUSAND_COLLISIONS(null, Kind.CHANCE, 1000);

    /** Ce que donne un succès. */
    public enum Kind {
        /** Son générateur produit ×2 (de l'énergie pour les faisceaux croisés). */
        OUTPUT,
        /** Les prix de son générateur sont divisés par deux. */
        COST,
        /** L'énergie ×1,5. */
        ENERGY,
        /** Tous les générateurs produisent ×1,5. */
        ALL_OUTPUT,
        /** La chance de collision +2 points. */
        CHANCE
    }

    private final ColliderGenerator generator;
    private final Kind kind;
    private final int threshold;

    ColliderAchievement(ColliderGenerator generator, Kind kind, int threshold) {
        this.generator = generator;
        this.kind = kind;
        this.threshold = threshold;
    }

    /** Le générateur qu'il concerne, ou {@code null} pour un succès d'ensemble. */
    public ColliderGenerator generator() {
        return generator;
    }

    public Kind kind() {
        return kind;
    }

    /** Ce qu'il faut : des exemplaires achetés de son générateur, des générateurs en tout, ou des collisions. */
    public int threshold() {
        return threshold;
    }

    /** Son nom : « Aimant supraconducteur, production », « Cent générateurs »… */
    public String title() {
        if (generator != null) return generator.label() + (kind == Kind.OUTPUT ? ", production" : ", prix");
        return switch (this) {
            case HUNDRED -> "Cent générateurs";
            case QUARTER_THOUSAND -> "Deux cent cinquante générateurs";
            default -> "Mille collisions";
        };
    }

    /** Ce qu'il demande, en mots. */
    public String condition() {
        if (generator != null) return threshold + " « " + generator.label() + " » achetés";
        return kind == Kind.CHANCE ? threshold + " collisions" : threshold + " générateurs achetés en tout";
    }

    /** Ce qu'il donne, en mots. */
    public String reward() {
        return switch (kind) {
            case OUTPUT -> generator == ColliderGenerator.BEAM ? "énergie des faisceaux ×2" : "production ×2";
            case COST -> "prix ÷2";
            case ENERGY -> "énergie ×1,5";
            case ALL_OUTPUT -> "tous les générateurs produisent ×1,5";
            case CHANCE -> "chance de collision +2 points";
        };
    }

    /** Le succès de ce générateur et de cette sorte. */
    public static ColliderAchievement of(ColliderGenerator generator, Kind kind) {
        return valueOf(generator.name() + "_" + kind.name());
    }
}
