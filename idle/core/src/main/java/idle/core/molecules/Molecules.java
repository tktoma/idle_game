package idle.core.molecules;

import java.util.List;

/**
 * Catalogue des molécules, rangées par rayon ({@link Molecule.Kind}) : 73 molécules réelles, de H₂ au
 * naphtalène, en quatre rayons (version 0.178 : 430 molécules et huit rayons avant). Chaque molécule
 * du catalogue sert au ciel : elle entre dans un assemblage ({@link idle.core.sky.Assemblies}) ou un
 * astre ({@link idle.core.sky.Bodies}) la demande. Rien n'y est de trop.
 *
 * <p>Chaque molécule est décrite par sa formule en clair ({@link Molecule#of}), qui lui sert
 * aussi d'identifiant. Aucune ne contient d'élément unique (gaz nobles, actinides) : un élément
 * unique ne peut pas quitter le tableau périodique.
 *
 * <p>Une création demande toujours la formule, une fois ({@link Game#nextMoleculeCost(String)}),
 * et un exemplaire de chaque élément doit rester dans le tableau. Ce qui limite une sorte, c'est
 * l'espace : chaque molécule en occupe, d'après son nombre de protons et son état
 * ({@link Game#moleculeVolume(String)}). Une sorte rassemblée attire en plus la matière : chaque
 * création y ajoute plusieurs molécules ({@link Game#moleculesPerCreation(String)}), et c'est ainsi
 * qu'on réunit les milliers de molécules d'un astre.
 *
 * <p>Chaque sorte donne quelque chose ({@link Molecule.Bonus}), et peu : quelques pour cent
 * d'espace, de particules, d'atomes ou de croissance de la matière noire, ou un exemplaire de
 * plus au maximum d'un de ses éléments. Elle le donne à sa première molécule, puis chaque fois que
 * leur nombre double ({@link Game#effectiveMolecules(String)}) : mille molécules d'une sorte ne
 * valent que dix fois la première, et il vaut mieux beaucoup de sortes qu'une seule énorme.
 * Les bonus sont ceux de la version 0.166, inchangés ; seules les molécules de trop sont parties.
 *
 * <p>Chaque molécule a aussi un état ({@link Molecule.State}), celui de sa substance à température
 * ambiante, qui décide du lieu où elle se rassemble ({@link Game#formSubstance(String)}) : 18 gaz,
 * 15 liquides et 40 cristaux. Depuis la version 0.178, tout ce qui est solide se range avec les
 * cristaux : les anciens solides (le soufre, le naphtalène, l'anthracène, l'acide borique) sont
 * des cristaux, et il n'y a plus de métal.
 */
public final class Molecules {

    public static final List<Molecule> DEFAULT = List.of(
            // Les petites molécules : corps simples, gaz et petites molécules des atmosphères et des glaces.
            Molecule.of(Molecule.Kind.SIMPLE, "Dihydrogène", "H2").uncapping("H").gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Dioxygène", "O2").uncapping("O").gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Diazote", "N2").uncapping("N").gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Soufre", "S8").uncapping("S").crystal(),
            Molecule.of(Molecule.Kind.SIMPLE, "Eau", "H2O").boosting(Molecule.Stat.PARTICLES, 0.05).liquid(),
            Molecule.of(Molecule.Kind.SIMPLE, "Monoxyde de carbone", "CO").boosting(Molecule.Stat.ATOMS, 0.02).gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Dioxyde de carbone", "CO2").boosting(Molecule.Stat.SPACE, 0.015).gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Ammoniac", "NH3").boosting(Molecule.Stat.ATOMS, 0.03).gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Méthane", "CH4").uncapping("C").gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Dioxyde de soufre", "SO2").boosting(Molecule.Stat.PARTICLES, 0.05).gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Sulfure d'hydrogène", "H2S").boosting(Molecule.Stat.ATOMS, 0.02).gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Phosphine", "PH3").boosting(Molecule.Stat.DARK_GROWTH, 0.05).gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Silane", "SiH4").uncapping("Si").gas(),
            Molecule.of(Molecule.Kind.SIMPLE, "Cyanure d'hydrogène", "HCN").boosting(Molecule.Stat.DARK_GROWTH, 0.05).liquid(),

            // Les acides et les sels : ceux des lacs, des mers et des roches salines.
            Molecule.of(Molecule.Kind.ACID, "Acide chlorhydrique", "HCl").boosting(Molecule.Stat.PARTICLES, 0.04).gas(),
            Molecule.of(Molecule.Kind.ACID, "Acide fluorhydrique", "HF").boosting(Molecule.Stat.ATOMS, 0.04).gas(),
            Molecule.of(Molecule.Kind.ACID, "Acide sulfurique", "H2SO4").boosting(Molecule.Stat.PARTICLES, 0.06).liquid(),
            Molecule.of(Molecule.Kind.ACID, "Acide sulfureux", "H2SO3").boosting(Molecule.Stat.ATOMS, 0.06).liquid(),
            Molecule.of(Molecule.Kind.ACID, "Acide carbonique", "H2CO3").boosting(Molecule.Stat.ATOMS, 0.06).liquid(),
            Molecule.of(Molecule.Kind.ACID, "Sel de table", "NaCl").uncapping("Na").crystal(),
            Molecule.of(Molecule.Kind.ACID, "Chlorure de potassium", "KCl").uncapping("K").crystal(),
            Molecule.of(Molecule.Kind.ACID, "Chlorure de lithium", "LiCl").uncapping("Li").crystal(),
            Molecule.of(Molecule.Kind.ACID, "Chlorure de calcium", "CaCl2").uncapping("Ca").crystal(),
            Molecule.of(Molecule.Kind.ACID, "Chlorure de magnésium", "MgCl2").uncapping("Mg").crystal(),
            Molecule.of(Molecule.Kind.ACID, "Sulfate de magnésium", "MgSO4").boosting(Molecule.Stat.DARK_GROWTH, 0.09).crystal(),

            // Les minéraux et les matériaux : oxydes, sulfures, carbonates, silicates, et ce qui fait les étoiles mortes.
            Molecule.of(Molecule.Kind.MINERAL, "Quartz", "SiO2").uncapping("Si").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Hématite", "Fe2O3").uncapping("Fe").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Magnétite", "Fe3O4").boosting(Molecule.Stat.ATOMS, 0.12).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Goethite", "FeO(OH)").boosting(Molecule.Stat.SPACE, 0.025).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Pyrite", "FeS2").boosting(Molecule.Stat.ATOMS, 0.08).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Calcite", "CaCO3").uncapping("Ca").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Dolomite", "CaMg(CO3)2").uncapping("Mg").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Magnésite", "MgCO3").boosting(Molecule.Stat.ATOMS, 0.08).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Sidérite", "FeCO3").boosting(Molecule.Stat.DARK_GROWTH, 0.08).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Anhydrite", "CaSO4").boosting(Molecule.Stat.DARK_GROWTH, 0.10).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Périclase", "MgO").boosting(Molecule.Stat.ATOMS, 0.06).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Rutile", "TiO2").uncapping("Ti").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Cassitérite", "SnO2").boosting(Molecule.Stat.DARK_GROWTH, 0.10).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Ilménite", "FeTiO3").boosting(Molecule.Stat.PARTICLES, 0.10).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Chromite", "FeCr2O4").uncapping("Cr").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Spinelle", "MgAl2O4").boosting(Molecule.Stat.DARK_GROWTH, 0.10).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Scheelite", "CaWO4").uncapping("W").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Ferbérite", "FeWO4").boosting(Molecule.Stat.ATOMS, 0.12).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Zircon", "ZrSiO4").uncapping("Zr").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Olivine", "Mg2SiO4").boosting(Molecule.Stat.ATOMS, 0.10).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Enstatite", "MgSiO3").boosting(Molecule.Stat.DARK_GROWTH, 0.08).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Orthose", "KAlSi3O8").uncapping("K").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Albite", "NaAlSi3O8").boosting(Molecule.Stat.SPACE, 0.035).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Anorthite", "CaAl2Si2O8").boosting(Molecule.Stat.PARTICLES, 0.12).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Talc", "Mg3Si4O10(OH)2").boosting(Molecule.Stat.ATOMS, 0.14).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Kaolinite", "Al2Si2O5(OH)4").boosting(Molecule.Stat.DARK_GROWTH, 0.12).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Grenat pyrope", "Mg3Al2Si3O12").boosting(Molecule.Stat.SPACE, 0.04).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Carbure de silicium", "SiC").uncapping("Si").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Hydrure de lithium", "LiH").boosting(Molecule.Stat.SPACE, 0.02).crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Oxyde de chrome", "Cr2O3").uncapping("Cr").crystal(),
            Molecule.of(Molecule.Kind.MINERAL, "Oxyde de nickel", "NiO").boosting(Molecule.Stat.PARTICLES, 0.09).crystal(),

            // La chimie du carbone : hydrocarbures, alcools et petites molécules organiques des nuages interstellaires.
            Molecule.of(Molecule.Kind.ORGANIC, "Éthane", "C2H6").boosting(Molecule.Stat.PARTICLES, 0.08).gas(),
            Molecule.of(Molecule.Kind.ORGANIC, "Propane", "C3H8").boosting(Molecule.Stat.SPACE, 0.025).gas(),
            Molecule.of(Molecule.Kind.ORGANIC, "Butane", "C4H10").boosting(Molecule.Stat.PARTICLES, 0.10).gas(),
            Molecule.of(Molecule.Kind.ORGANIC, "Pentane", "C5H12").boosting(Molecule.Stat.ATOMS, 0.10).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Hexane", "C6H14").boosting(Molecule.Stat.DARK_GROWTH, 0.10).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Octane", "C8H18").boosting(Molecule.Stat.PARTICLES, 0.12).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Cyclohexane", "C6H12").boosting(Molecule.Stat.SPACE, 0.03).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Acétylène", "C2H2").boosting(Molecule.Stat.DARK_GROWTH, 0.08).gas(),
            Molecule.of(Molecule.Kind.ORGANIC, "Benzène", "C6H6").boosting(Molecule.Stat.PARTICLES, 0.10).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Toluène", "C7H8").boosting(Molecule.Stat.ATOMS, 0.10).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Xylène", "C8H10").boosting(Molecule.Stat.DARK_GROWTH, 0.10).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Naphtalène", "C10H8").boosting(Molecule.Stat.SPACE, 0.035).crystal(),
            Molecule.of(Molecule.Kind.ORGANIC, "Anthracène", "C14H10").boosting(Molecule.Stat.PARTICLES, 0.12).crystal(),
            Molecule.of(Molecule.Kind.ORGANIC, "Méthanol", "CH3OH").boosting(Molecule.Stat.DARK_GROWTH, 0.08).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Éthanol", "C2H5OH").boosting(Molecule.Stat.PARTICLES, 0.08).liquid(),
            Molecule.of(Molecule.Kind.ORGANIC, "Formaldéhyde", "CH2O").boosting(Molecule.Stat.PARTICLES, 0.08).gas(),
            Molecule.of(Molecule.Kind.ORGANIC, "Thiophène", "C4H4S").boosting(Molecule.Stat.DARK_GROWTH, 0.10).liquid());

    private Molecules() {}
}
