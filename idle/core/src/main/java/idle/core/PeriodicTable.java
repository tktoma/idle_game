package idle.core;

import java.util.List;

/**
 * Le tableau périodique : les 118 éléments, rangés par numéro atomique, chacun avec son effet.
 *
 * <p>Les effets suivent la logique du vrai tableau :
 * <ul>
 *   <li><b>métaux de transition</b> : les dix colonnes du bloc central correspondent aux dix
 *       générateurs. La 4ᵉ et la 6ᵉ période multiplient les particules du générateur de leur
 *       colonne (×1,5 puis ×2), la 5ᵉ et la 7ᵉ sa vitesse (×1,25 puis ×1,5) ;</li>
 *   <li><b>métaux pauvres</b> : réductions de prix, plus fortes pour les éléments lourds. La
 *       colonne de l'aluminium baisse le prix de la vitesse, l'étain, le plomb, le bismuth et
 *       leurs voisins celui des améliorations en atomes, le polonium celui de la synthèse ;</li>
 *   <li><b>non-métaux</b> : chacun renforce une amélioration existante ;</li>
 *   <li><b>métaux alcalins</b> : chacun accélère un automatisme, les deux derniers les accélèrent tous ;</li>
 *   <li><b>alcalino-terreux</b> : atomes par fusion ;</li>
 *   <li><b>métalloïdes</b> : synergies, qui grandissent avec le reste du jeu ;</li>
 *   <li><b>halogènes</b> : tirages doubles et chance ;</li>
 *   <li><b>lanthanides</b> : multiplicateurs qui se multiplient entre eux ;</li>
 *   <li><b>gaz nobles</b> et <b>actinides</b> : uniques, les plus puissants.</li>
 * </ul>
 *
 * <p>Les familles et les périodes forment des ensembles, qui donnent un bonus de plus une fois
 * réunis ({@link ElementSets}).
 *
 * <p>Les effets qui accélèrent la synthèse elle-même (cadence des automatismes, atomes par
 * fusion, prix de la synthèse) sont volontairement modestes : ce sont eux qui décident de la
 * durée du tableau. Plus forts, la seconde moitié du tableau se remplirait en une heure.
 */
public final class PeriodicTable {

    public static final List<Element> ELEMENTS = List.of(
            new Element(1, "H", "Hydrogène", ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.DOUBLING, 0.10)),
            new Element(2, "He", "Hélium", ElementCategory.NOBLE_GAS,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(3, "Li", "Lithium", ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.SPEED_UPGRADES, 0.09)),
            new Element(4, "Be", "Béryllium", ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Add(ElementEffect.Stat.ATOMS, 0.015)),
            new Element(5, "B", "Bore", ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.DISTINCT_ELEMENTS, ElementEffect.Stat.PARTICLES, 0.02)),
            new Element(6, "C", "Carbone", ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.MASS, 0.01)),
            new Element(7, "N", "Azote", ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.PATIENCE, 0.05)),
            new Element(8, "O", "Oxygène", ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.CATALYST, 0.002)),
            new Element(9, "F", "Fluor", ElementCategory.HALOGEN,
                    new ElementEffect.DoubleDraw(0.05)),
            new Element(10, "Ne", "Néon", ElementCategory.NOBLE_GAS,
                    new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 1.50)),
            new Element(11, "Na", "Sodium", ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.GENERATORS, 0.09)),
            new Element(12, "Mg", "Magnésium", ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Add(ElementEffect.Stat.ATOMS, 0.02)),
            new Element(13, "Al", "Aluminium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SPEED_UPGRADES, 0.05)),
            new Element(14, "Si", "Silicium", ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.SPEED_LEVELS, ElementEffect.Stat.PARTICLES, 0.01)),
            new Element(15, "P", "Phosphore", ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.YIELD, 0.01)),
            new Element(16, "S", "Soufre", ElementCategory.NONMETAL,
                    new ElementEffect.UpgradeBoost(ElementEffect.UpgradeTarget.SPEED, 0.005)),
            new Element(17, "Cl", "Chlore", ElementCategory.HALOGEN,
                    new ElementEffect.DoubleDraw(0.05)),
            new Element(18, "Ar", "Argon", ElementCategory.NOBLE_GAS,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.15)),
            new Element(19, "K", "Potassium", ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.FUSION, 0.09)),
            new Element(20, "Ca", "Calcium", ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Add(ElementEffect.Stat.ATOMS, 0.025)),
            new Element(21, "Sc", "Scandium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(1, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(22, "Ti", "Titane", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(2, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(23, "V", "Vanadium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(3, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(24, "Cr", "Chrome", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(4, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(25, "Mn", "Manganèse", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(5, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(26, "Fe", "Fer", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(6, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(27, "Co", "Cobalt", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(7, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(28, "Ni", "Nickel", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(8, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(29, "Cu", "Cuivre", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(9, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(30, "Zn", "Zinc", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(10, ElementEffect.Stat.PARTICLES, 1.50)),
            new Element(31, "Ga", "Gallium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SPEED_UPGRADES, 0.08)),
            new Element(32, "Ge", "Germanium", ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.GENERATORS, ElementEffect.Stat.PARTICLES, 0.05)),
            new Element(33, "As", "Arsenic", ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.AVAILABLE_ATOMS, ElementEffect.Stat.SPEED, 0.005)),
            new Element(34, "Se", "Sélénium", ElementCategory.NONMETAL,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.GENERATORS, ElementEffect.Stat.SPEED, 0.02)),
            new Element(35, "Br", "Brome", ElementCategory.HALOGEN,
                    new ElementEffect.DoubleDraw(0.10)),
            new Element(36, "Kr", "Krypton", ElementCategory.NOBLE_GAS,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.ALL, 0.40)),
            new Element(37, "Rb", "Rubidium", ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.SYNTHESIS, 0.09)),
            new Element(38, "Sr", "Strontium", ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Add(ElementEffect.Stat.ATOMS, 0.03)),
            new Element(39, "Y", "Yttrium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(1, ElementEffect.Stat.SPEED, 1.25)),
            new Element(40, "Zr", "Zirconium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(2, ElementEffect.Stat.SPEED, 1.25)),
            new Element(41, "Nb", "Niobium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(3, ElementEffect.Stat.SPEED, 1.25)),
            new Element(42, "Mo", "Molybdène", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(4, ElementEffect.Stat.SPEED, 1.25)),
            new Element(43, "Tc", "Technétium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(5, ElementEffect.Stat.SPEED, 1.25)),
            new Element(44, "Ru", "Ruthénium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(6, ElementEffect.Stat.SPEED, 1.25)),
            new Element(45, "Rh", "Rhodium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(7, ElementEffect.Stat.SPEED, 1.25)),
            new Element(46, "Pd", "Palladium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(8, ElementEffect.Stat.SPEED, 1.25)),
            new Element(47, "Ag", "Argent", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(9, ElementEffect.Stat.SPEED, 1.25)),
            new Element(48, "Cd", "Cadmium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(10, ElementEffect.Stat.SPEED, 1.25)),
            new Element(49, "In", "Indium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SPEED_UPGRADES, 0.10)),
            new Element(50, "Sn", "Étain", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.ATOM_UPGRADES, 0.10)),
            new Element(51, "Sb", "Antimoine", ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.AUTOMATIONS, ElementEffect.Stat.PARTICLES, 0.10)),
            new Element(52, "Te", "Tellure", ElementCategory.METALLOID,
                    new ElementEffect.Synergy(ElementEffect.SynergySource.DISTINCT_ELEMENTS, ElementEffect.Stat.ATOMS, 0.001)),
            new Element(53, "I", "Iode", ElementCategory.HALOGEN,
                    new ElementEffect.Luck(0.10)),
            new Element(54, "Xe", "Xénon", ElementCategory.NOBLE_GAS,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SYNTHESIS, 0.50)),
            new Element(55, "Cs", "Césium", ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.ALL, 0.06)),
            new Element(56, "Ba", "Baryum", ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Add(ElementEffect.Stat.ATOMS, 0.04)),
            new Element(57, "La", "Lanthane", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 1.30)),
            new Element(58, "Ce", "Cérium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 1.30)),
            new Element(59, "Pr", "Praséodyme", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 1.30)),
            new Element(60, "Nd", "Néodyme", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 1.30)),
            new Element(61, "Pm", "Prométhium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.PARTICLES, 1.30)),
            new Element(62, "Sm", "Samarium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 1.12)),
            new Element(63, "Eu", "Europium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 1.12)),
            new Element(64, "Gd", "Gadolinium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 1.12)),
            new Element(65, "Tb", "Terbium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 1.12)),
            new Element(66, "Dy", "Dysprosium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.SPEED, 1.12)),
            new Element(67, "Ho", "Holmium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.03)),
            new Element(68, "Er", "Erbium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.03)),
            new Element(69, "Tm", "Thulium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.03)),
            new Element(70, "Yb", "Ytterbium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.03)),
            new Element(71, "Lu", "Lutécium", ElementCategory.LANTHANIDE,
                    new ElementEffect.Multiply(ElementEffect.Stat.ATOMS, 1.03)),
            new Element(72, "Hf", "Hafnium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(2, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(73, "Ta", "Tantale", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(3, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(74, "W", "Tungstène", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(4, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(75, "Re", "Rhénium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(5, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(76, "Os", "Osmium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(6, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(77, "Ir", "Iridium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(7, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(78, "Pt", "Platine", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(8, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(79, "Au", "Or", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(9, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(80, "Hg", "Mercure", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(10, ElementEffect.Stat.PARTICLES, 2.00)),
            new Element(81, "Tl", "Thallium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SPEED_UPGRADES, 0.15)),
            new Element(82, "Pb", "Plomb", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.ATOM_UPGRADES, 0.15)),
            new Element(83, "Bi", "Bismuth", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.ATOM_UPGRADES, 0.05)),
            new Element(84, "Po", "Polonium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SYNTHESIS, 0.05)),
            new Element(85, "At", "Astate", ElementCategory.HALOGEN,
                    new ElementEffect.Luck(0.15)),
            new Element(86, "Rn", "Radon", ElementCategory.NOBLE_GAS,
                    new ElementEffect.DoubleDraw(0.25)),
            new Element(87, "Fr", "Francium", ElementCategory.ALKALI_METAL,
                    new ElementEffect.AutomationSpeed(ElementEffect.AutomationTarget.ALL, 0.11)),
            new Element(88, "Ra", "Radium", ElementCategory.ALKALINE_EARTH_METAL,
                    new ElementEffect.Add(ElementEffect.Stat.ATOMS, 0.05)),
            new Element(89, "Ac", "Actinium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(90, "Th", "Thorium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(91, "Pa", "Protactinium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(92, "U", "Uranium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(93, "Np", "Neptunium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(94, "Pu", "Plutonium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(95, "Am", "Américium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(96, "Cm", "Curium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(97, "Bk", "Berkélium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(98, "Cf", "Californium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(99, "Es", "Einsteinium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(100, "Fm", "Fermium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(101, "Md", "Mendélévium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(102, "No", "Nobélium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(103, "Lr", "Lawrencium", ElementCategory.ACTINIDE,
                    new ElementEffect.MultiplyProduction(2.00)),
            new Element(104, "Rf", "Rutherfordium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(2, ElementEffect.Stat.SPEED, 1.50)),
            new Element(105, "Db", "Dubnium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(3, ElementEffect.Stat.SPEED, 1.50)),
            new Element(106, "Sg", "Seaborgium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(4, ElementEffect.Stat.SPEED, 1.50)),
            new Element(107, "Bh", "Bohrium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(5, ElementEffect.Stat.SPEED, 1.50)),
            new Element(108, "Hs", "Hassium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(6, ElementEffect.Stat.SPEED, 1.50)),
            new Element(109, "Mt", "Meitnérium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(7, ElementEffect.Stat.SPEED, 1.50)),
            new Element(110, "Ds", "Darmstadtium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(8, ElementEffect.Stat.SPEED, 1.50)),
            new Element(111, "Rg", "Roentgenium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(9, ElementEffect.Stat.SPEED, 1.50)),
            new Element(112, "Cn", "Copernicium", ElementCategory.TRANSITION_METAL,
                    new ElementEffect.GeneratorBoost(10, ElementEffect.Stat.SPEED, 1.50)),
            new Element(113, "Nh", "Nihonium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SPEED_UPGRADES, 0.20)),
            new Element(114, "Fl", "Flérovium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.ATOM_UPGRADES, 0.20)),
            new Element(115, "Mc", "Moscovium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.ATOM_UPGRADES, 0.10)),
            new Element(116, "Lv", "Livermorium", ElementCategory.POST_TRANSITION_METAL,
                    new ElementEffect.CostReduction(ElementEffect.CostTarget.SYNTHESIS, 0.10)),
            new Element(117, "Ts", "Tennesse", ElementCategory.HALOGEN,
                    new ElementEffect.Luck(0.25)),
            new Element(118, "Og", "Oganesson", ElementCategory.NOBLE_GAS,
                    new ElementEffect.MultiplyEverything(1.25)));

    /**
     * L'élément qui porte ce symbole (« Fe »).
     *
     * @throws IllegalArgumentException si aucun élément ne le porte
     */
    public static Element bySymbol(String symbol) {
        for (Element element : ELEMENTS) {
            if (element.symbol().equals(symbol)) return element;
        }
        throw new IllegalArgumentException("Symbole inconnu : " + symbol);
    }

    /** L'élément de numéro atomique donné, de 1 à 118. */
    public static Element element(int number) {
        if (number < 1 || number > ELEMENTS.size()) {
            throw new IllegalArgumentException("Numéro atomique inconnu : " + number);
        }
        return ELEMENTS.get(number - 1);
    }

    /** Les éléments d'une famille, par numéro atomique croissant. */
    public static List<Element> elements(ElementCategory category) {
        return ELEMENTS.stream().filter(element -> element.category() == category).toList();
    }

    /** Nombre de périodes (de lignes) du tableau. */
    public static final int PERIODS = 7;

    /** Dernier numéro atomique de chaque période : 2, 10, 18, 36, 54, 86, 118. */
    private static final int[] PERIOD_ENDS = {2, 10, 18, 36, 54, 86, 118};

    /**
     * Période d'un élément, de 1 à 7 : sa ligne dans le vrai tableau périodique. Les lanthanides
     * appartiennent à la 6ᵉ et les actinides à la 7ᵉ, même si on les dessine à part.
     */
    public static int period(int number) {
        element(number);   // refuse un numéro inconnu
        for (int period = 1; period <= PERIODS; period++) {
            if (number <= PERIOD_ENDS[period - 1]) return period;
        }
        throw new IllegalStateException("Période introuvable pour l'élément " + number);
    }

    /** Les éléments d'une période (de 1 à 7), par numéro atomique croissant. */
    public static List<Element> elementsOfPeriod(int period) {
        if (period < 1 || period > PERIODS) throw new IllegalArgumentException("Période inconnue : " + period);
        return ELEMENTS.stream().filter(element -> period(element.number()) == period).toList();
    }

    private PeriodicTable() {}
}
