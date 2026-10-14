package idle.core.collider;

/**
 * Les améliorations du collisionneur (version 0.59, refaites en 0.60), payées en énergie. Elles
 * s'ouvrent quand les huit générateurs sont là ; au début, seule la première s'offre, « Collisions »,
 * qui donne aux croisements leur chance de collision. Chacune en ouvre d'autres, rangées en quatre
 * familles ({@link Section}) : les collisions et leurs saveurs, une à une ; les faisceaux ; les
 * générateurs et leurs automates ; et la chaîne qui fait passer l'énergie dans le reste du jeu,
 * des particules au dédoublement des molécules, rangée depuis la version 0.61 dans son propre
 * sous-onglet, et payée aussi en quarks depuis la 0.62. Comme tout le collisionneur, elles traversent
 * l'explosion et le Big Bang ; seule la remise à zéro les reprend.
 */
public enum ColliderUpgrade {
    // Les collisions : la chance, puis les saveurs une à une.
    COLLISIONS(Section.COLLISIONS, "Collisions", "Chaque croisement des faisceaux a 15 % de chances de donner un quark haut ou bas",
            1, 1e10, 1, null, 0, Quark.DOWN),
    FLAVOR_STRANGE(Section.COLLISIONS, "Saveur étrange", "Les collisions peuvent donner des quarks étranges (s)",
            1, 1e11, 1, COLLISIONS, 1, Quark.STRANGE),
    FLAVOR_CHARM(Section.COLLISIONS, "Saveur charmée", "Les collisions peuvent donner des quarks charmés (c), et les sondes en rapportent",
            1, 3e12, 1, FLAVOR_STRANGE, 1, Quark.CHARM),
    FLAVOR_BOTTOM(Section.COLLISIONS, "Saveur belle", "Les collisions peuvent donner des quarks beaux (b)",
            1, 1e14, 1, FLAVOR_CHARM, 1, Quark.BOTTOM),
    FLAVOR_TOP(Section.COLLISIONS, "Saveur top", "Les collisions peuvent donner le quark top, et sa surchauffe",
            1, 1e16, 1, FLAVOR_BOTTOM, 1, Quark.TOP),
    LUCK(Section.COLLISIONS, "Faisceaux focalisés", "Chance de collision à chaque croisement : +3 points par niveau",
            8, 5e10, 5, COLLISIONS, 1, null),
    HEAVY(Section.COLLISIONS, "Saveurs lourdes", "Quarks s, c, b et t 40 % plus fréquents par niveau",
            5, 3e11, 8, FLAVOR_STRANGE, 1, null),
    TOP_LIFE(Section.COLLISIONS, "Top stable", "Le quark top attend deux fois plus longtemps, la surchauffe dure 50 % de plus, par niveau",
            3, 1e16, 20, FLAVOR_TOP, 1, null),
    // Les faisceaux.
    SPEED(Section.BEAMS, "Faisceaux rapides", "Les deux faisceaux tournent 20 % plus vite par niveau : plus de croisements",
            5, 2e10, 10, COLLISIONS, 1, null),
    WINDOW(Section.BEAMS, "Détecteur élargi", "Moment du croisement 25 % plus long par niveau : plus facile à cliquer",
            3, 5e10, 8, SPEED, 1, null),
    AUTO_CATCH(Section.BEAMS, "Déclencheur automatique", "Clique seul au bon moment : un croisement sur cinq, puis un sur trois, puis tous",
            3, 1e12, 50, SPEED, 1, null),
    // Les générateurs et leurs automates.
    GENERATOR_POWER(Section.GENERATORS, "Générateurs renforcés", "Chaque générateur, de l'aimant au collisionneur de Planck, produit ×2 par niveau",
            10, 1e11, 10, COLLISIONS, 1, null),
    AUTO_BEAM(ColliderGenerator.BEAM, 1e10, COLLISIONS),
    AUTO_MAGNET(ColliderGenerator.MAGNET, 4e10, AUTO_BEAM),
    AUTO_CAVITY(ColliderGenerator.CAVITY, 1.6e11, AUTO_MAGNET),
    AUTO_SYNCHROTRON(ColliderGenerator.SYNCHROTRON, 6.4e11, AUTO_CAVITY),
    AUTO_STORAGE(ColliderGenerator.STORAGE, 2.6e12, AUTO_SYNCHROTRON),
    AUTO_LINAC(ColliderGenerator.LINAC, 1e13, AUTO_STORAGE),
    AUTO_SUPERCOLLIDER(ColliderGenerator.SUPERCOLLIDER, 4e13, AUTO_LINAC),
    AUTO_PLANCK(ColliderGenerator.PLANCK, 1.6e14, AUTO_SUPERCOLLIDER),
    // La chaîne : l'énergie, puis ce qu'elle renforce ailleurs.
    ENERGY(Section.CHAIN, "Production d'énergie", "Énergie ×2 par niveau", 10, 2e10, 6, COLLISIONS, 1, null, Quark.UP, 10),
    PARTICLES(Section.CHAIN, "Particules", "Particules ×1,5 par niveau", 10, 1e11, 8, ENERGY, 1, null, Quark.UP, 30),
    ATOMS(Section.CHAIN, "Atomes", "Atomes par fusion +10 % par niveau", 10, 1e12, 8, PARTICLES, 1, null, Quark.DOWN, 30),
    DARK_MATTER(Section.CHAIN, "Matière noire", "Croissance de la matière noire +20 % par niveau", 10, 1e13, 8, ATOMS, 1, null, Quark.STRANGE, 15),
    SPACE(Section.CHAIN, "Vitesse de l'espace", "Espace par seconde +25 % par niveau", 4, 1e14, 10, DARK_MATTER, 1, null, Quark.CHARM, 10),
    MOLECULES(Section.CHAIN, "Dédoublement des molécules", "Chaque création de molécule a 5 % de chances par niveau d'en donner deux fois plus",
            10, 1e15, 10, SPACE, 1, null, Quark.BOTTOM, 5);

    /** Les quatre familles d'améliorations, une rangée chacune dans la page. */
    public enum Section {
        COLLISIONS("Collisions et saveurs"),
        BEAMS("Faisceaux"),
        GENERATORS("Générateurs et automates"),
        CHAIN("Énergie → particules → atomes → matière noire → espace → molécules");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Section section;
    private final String label;
    private final String effect;
    private final int maxLevel;
    private final double baseCost;
    private final double growth;
    private final ColliderUpgrade requires;
    private final int requiredLevel;
    private final ColliderGenerator generator;
    private final Quark flavor;
    /** La chaîne se paie aussi en quarks (version 0.62) : leur saveur, et le prix du premier niveau, qui double ensuite. */
    private final Quark quark;
    private final int quarkBase;

    ColliderUpgrade(Section section, String label, String effect, int maxLevel, double baseCost, double growth,
                    ColliderUpgrade requires, int requiredLevel, Quark flavor) {
        this(section, label, effect, maxLevel, baseCost, growth, requires, requiredLevel, flavor, null, 0);
    }

    ColliderUpgrade(Section section, String label, String effect, int maxLevel, double baseCost, double growth,
                    ColliderUpgrade requires, int requiredLevel, Quark flavor, Quark quark, int quarkBase) {
        this.section = section;
        this.label = label;
        this.effect = effect;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.growth = growth;
        this.requires = requires;
        this.requiredLevel = requiredLevel;
        this.generator = null;
        this.flavor = flavor;
        this.quark = quark;
        this.quarkBase = quarkBase;
    }

    /** Un automate : il achète tout seul les exemplaires d'un générateur, dès que l'énergie le permet. */
    ColliderUpgrade(ColliderGenerator generator, double cost, ColliderUpgrade requires) {
        this.section = Section.GENERATORS;
        this.label = "Automate : " + lowerFirst(generator.label());
        this.effect = "Achète tout seul « " + generator.label() + " » dès que l'énergie le permet ; un clic une fois acheté le met en pause";
        this.maxLevel = 1;
        this.baseCost = cost;
        this.growth = 1;
        this.requires = requires;
        this.requiredLevel = 1;
        this.generator = generator;
        this.flavor = null;
        this.quark = null;
        this.quarkBase = 0;
    }

    private static String lowerFirst(String text) {
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    public Section section() {
        return section;
    }

    public String label() {
        return label;
    }

    /** Ce qu'elle fait, en une phrase. */
    public String effect() {
        return effect;
    }

    /**
     * Ce que fait l'amélioration, en quelques mots, pour la carte (version 0.64) ; l'explication
     * complète ({@link #effect()}) n'apparaît qu'en mode détails.
     */
    public String brief() {
        if (generator != null) return "Achète « " + generator.label() + " » tout seul";
        return switch (this) {
            case COLLISIONS -> "15 % de chances de collision";
            case FLAVOR_STRANGE -> "Ouvre les quarks s";
            case FLAVOR_CHARM -> "Ouvre les quarks c";
            case FLAVOR_BOTTOM -> "Ouvre les quarks b";
            case FLAVOR_TOP -> "Ouvre le quark top";
            case LUCK -> "Chance de collision +3 points";
            case HEAVY -> "s, c, b, t +40 %";
            case TOP_LIFE -> "Top ×2 plus long, surchauffe +50 %";
            case SPEED -> "Faisceaux +20 % plus vite";
            case WINDOW -> "Croisement +25 % plus long";
            case AUTO_CATCH -> "Clique seul aux croisements";
            case GENERATOR_POWER -> "Générateurs ×2";
            case ENERGY -> "Énergie ×2";
            case PARTICLES -> "Particules ×1,5";
            case ATOMS -> "Atomes par fusion +10 %";
            case DARK_MATTER -> "Matière noire +20 %";
            case SPACE -> "Espace par seconde +25 %";
            case MOLECULES -> "+5 % de molécules doublées";
            default -> effect;
        };
    }

    public int maxLevel() {
        return maxLevel;
    }

    /** Prix du niveau suivant, quand {@code level} niveaux sont déjà pris. */
    public double cost(int level) {
        return baseCost * Math.pow(growth, level);
    }

    /** La saveur de quarks qu'elle coûte en plus de l'énergie, ou {@code null}. */
    public Quark quark() {
        return quark;
    }

    /** Les quarks que coûte le niveau suivant, quand {@code level} niveaux sont déjà pris : le double à chaque niveau. */
    public int quarkCost(int level) {
        return quark == null ? 0 : (int) Math.min(Integer.MAX_VALUE / 2, quarkBase * Math.pow(2, level));
    }

    /** L'amélioration qui l'ouvre, ou {@code null} pour la première. */
    public ColliderUpgrade requires() {
        return requires;
    }

    /** Le niveau qu'il faut à {@link #requires()} pour l'ouvrir. */
    public int requiredLevel() {
        return requiredLevel;
    }

    /** Le générateur qu'achète cet automate, ou {@code null} si ce n'en est pas un. */
    public ColliderGenerator generator() {
        return generator;
    }

    /** Vrai pour un automate de générateur. */
    public boolean isAutomation() {
        return generator != null;
    }

    /** La saveur qu'ouvre cette amélioration (le quark bas pour « Collisions », qui ouvre aussi le haut), ou {@code null}. */
    public Quark flavor() {
        return flavor;
    }

    /** Vrai pour ce qui se met en pause d'un clic une fois acheté : les automates. */
    public boolean isPausable() {
        return generator != null;
    }

    /** L'automate de ce générateur. */
    public static ColliderUpgrade automationOf(ColliderGenerator generator) {
        return valueOf("AUTO_" + generator.name());
    }

    /** L'amélioration qui ouvre cette saveur. */
    public static ColliderUpgrade flavorOf(Quark quark) {
        return switch (quark) {
            case UP, DOWN -> COLLISIONS;
            case STRANGE -> FLAVOR_STRANGE;
            case CHARM -> FLAVOR_CHARM;
            case BOTTOM -> FLAVOR_BOTTOM;
            case TOP -> FLAVOR_TOP;
        };
    }
}
