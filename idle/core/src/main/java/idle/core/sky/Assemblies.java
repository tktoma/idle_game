package idle.core.sky;

import java.util.List;

/**
 * Catalogue des assemblages, rangés par famille ({@link Assembly.Family}) : 30 matières qu'on
 * trouve dans la nature, de la roche granitique à la glace de comète, puis à la matière des étoiles
 * (version 0.178 : 58 avant ; chaque astre n'en demande plus qu'un, le plus caractéristique). Rien
 * de vivant, rien de fabriqué non plus, ni verre, ni alliage, ni batterie.
 *
 * <p>Les noms sont généraux : un assemblage est une sorte de matière (« Roche calcaire », « Lac
 * salé »), pas un échantillon précis, et réunit trois à sept sortes de molécules qui vont ensemble.
 *
 * <p>Chaque assemblage demande 260 à 420 molécules, et ceux de la matière d'étoiles jusqu'à 1 500,
 * presque tout en hydrogène : une étoile est bien plus grosse qu'une planète. Les proportions sont
 * proches de la matière réelle : la base en centaines, les traces à l'unité. Un assemblage ajoute
 * à la grandeur de sa famille son nombre de molécules divisé par {@link Assembly#MOLECULES_PER_WHOLE}
 * (100 depuis la 0.178 : les collections de rayon ont disparu, les assemblages comptent double).
 */
public final class Assemblies {

    public static final List<Assembly> DEFAULT = List.of(
            // Les roches : des grains de plusieurs minéraux, d'ici ou tombés du ciel.
            of(Assembly.Family.ROCK, "granitic_rock", "Roche granitique", "#a79a94")
                    .with("SiO2", 120).with("KAlSi3O8", 90).with("NaAlSi3O8", 60),
            of(Assembly.Family.ROCK, "volcanic_rock", "Roche volcanique", "#4a4a52")
                    .with("CaAl2Si2O8", 110).with("MgSiO3", 90).with("Mg2SiO4", 50).with("Fe3O4", 10).with("FeTiO3", 10),
            of(Assembly.Family.ROCK, "limestone_rock", "Roche calcaire", "#cfc7b0")
                    .with("CaCO3", 220).with("CaMg(CO3)2", 40).with("MgCO3", 20).with("SiO2", 10),
            of(Assembly.Family.ROCK, "sandy_rock", "Roche sableuse", "#c9a36b")
                    .with("SiO2", 230).with("CaCO3", 30).with("Fe2O3", 10).with("ZrSiO4", 5),
            of(Assembly.Family.ROCK, "clay_rock", "Roche argileuse", "#b5764f")
                    .with("Al2Si2O5(OH)4", 160).with("Mg3Si4O10(OH)2", 40).with("SiO2", 40).with("H2O", 40),
            of(Assembly.Family.ROCK, "salt_rock", "Roche saline", "#e8d9d2")
                    .with("NaCl", 200).with("KCl", 30).with("CaSO4", 40).with("MgCl2", 10).with("MgSO4", 10),
            of(Assembly.Family.ROCK, "mantle_rock", "Roche des profondeurs", "#5f7a4a")
                    .with("Mg2SiO4", 180).with("MgSiO3", 60).with("Mg3Al2Si3O12", 20).with("FeCr2O4", 10).with("MgAl2O4", 10),
            of(Assembly.Family.ROCK, "lunar_rock", "Roche lunaire", "#b8b4ac")
                    .with("CaAl2Si2O8", 150).with("FeTiO3", 50).with("MgSiO3", 50).with("Mg2SiO4", 30),

            // Les minerais : ce dont on tire les métaux, avec la roche qui les porte.
            of(Assembly.Family.ORE, "iron_ore", "Minerai de fer", "#8b3a2e")
                    .with("Fe2O3", 140).with("Fe3O4", 60).with("FeO(OH)", 40).with("FeCO3", 20).with("SiO2", 40),
            of(Assembly.Family.ORE, "tin_tungsten_ore", "Minerai d'étain et de tungstène", "#5a4a3c")
                    .with("SnO2", 120).with("CaWO4", 50).with("FeWO4", 30).with("SiO2", 80),

            // Les pierres précieuses : un cristal pur, et ses traces colorées.
            of(Assembly.Family.GEM, "peridot_gems", "Péridots", "#a6c838")
                    .with("Mg2SiO4", 240).with("Fe2O3", 20).with("NiO", 2),

            // Les eaux et les glaces : de l'eau, et ce qui s'y dissout.
            of(Assembly.Family.WATER, "sparkling_water", "Eau pétillante", "#7fc4ea")
                    .with("H2O", 300).with("CO2", 12).with("H2CO3", 10),
            of(Assembly.Family.WATER, "salt_lake", "Lac salé", "#c9d9d0")
                    .with("H2O", 240).with("NaCl", 80).with("KCl", 15).with("MgCl2", 15).with("CaCl2", 5).with("LiCl", 5),
            of(Assembly.Family.WATER, "acid_lake", "Lac acide", "#9fc24a")
                    .with("H2O", 300).with("H2SO4", 30).with("HCl", 20).with("HF", 5).with("H2SO3", 5),
            of(Assembly.Family.WATER, "comet_ice", "Glace de comète", "#d8eef4")
                    .with("H2O", 300).with("CO2", 40).with("CO", 30).with("NH3", 10).with("CH4", 10).with("CH3OH", 8).with("HCN", 2),

            // Les gaz : des atmosphères et des nuages.
            of(Assembly.Family.AIR, "air", "Air", "#bcd8ee")
                    .with("N2", 312).with("O2", 84).with("CO2", 4),
            of(Assembly.Family.AIR, "gas_giant", "Atmosphère d'une géante gazeuse", "#d9b38a")
                    .with("H2", 330).with("CH4", 20).with("NH3", 10).with("H2O", 5).with("H2S", 3).with("PH3", 2),
            of(Assembly.Family.AIR, "interstellar_cloud", "Nuage interstellaire", "#6a5a9a")
                    .with("H2", 300).with("CO", 40).with("H2O", 20).with("NH3", 10).with("CH2O", 5).with("CH3OH", 5).with("HCN", 5),

            // Les hydrocarbures : du charbon au gaz naturel.
            of(Assembly.Family.FUEL, "petroleum", "Pétrole", "#3a322a")
                    .with("C8H18", 120).with("C6H14", 70).with("C5H12", 40).with("C6H12", 30).with("C7H8", 30).with("C8H10", 20).with("C6H6", 10),
            of(Assembly.Family.FUEL, "natural_gas", "Gaz naturel", "#8fa0c8")
                    .with("CH4", 240).with("C2H6", 30).with("C3H8", 15).with("C4H10", 10).with("CO2", 5),
            of(Assembly.Family.FUEL, "bitumen", "Bitume", "#26221e")
                    .with("C10H8", 80).with("C14H10", 60).with("C7H8", 40).with("C8H10", 40).with("C8H18", 30).with("C4H4S", 10),

            // La matière des étoiles : presque tout en hydrogène, et ce qui reste quand elles meurent.
            of(Assembly.Family.STELLAR, "brown_dwarf_atmosphere", "Atmosphère de naine brune", "#8a4a5a")
                    .with("H2", 560).with("CH4", 70).with("H2O", 35).with("NH3", 20).with("CO", 15),
            of(Assembly.Family.STELLAR, "red_dwarf_envelope", "Enveloppe de naine rouge", "#e0503a")
                    .with("H2", 700).with("CO", 60).with("H2O", 40).with("TiO2", 30).with("LiH", 20),
            of(Assembly.Family.STELLAR, "solar_plasma", "Plasma solaire", "#ffcc4a")
                    .with("H2", 950).with("O2", 50).with("CO", 40).with("N2", 30).with("SiH4", 30),
            of(Assembly.Family.STELLAR, "blue_giant_envelope", "Enveloppe de géante bleue", "#8ab8ff")
                    .with("H2", 1100).with("N2", 90).with("O2", 70).with("CO", 40),
            of(Assembly.Family.STELLAR, "red_giant_envelope", "Enveloppe de géante rouge", "#ff7a3a")
                    .with("H2", 1050).with("CO", 150).with("H2O", 80).with("SiC", 50).with("TiO2", 30).with("C2H2", 25).with("HCN", 15),
            of(Assembly.Family.STELLAR, "carbon_core", "Cœur de carbone et d'oxygène", "#dfe8f8")
                    .with("CO", 200).with("CO2", 100).with("SiC", 40).with("MgO", 20),
            of(Assembly.Family.STELLAR, "iron_crust", "Croûte de fer et de nickel", "#8a8f9c")
                    .with("Fe3O4", 200).with("Fe2O3", 80).with("NiO", 60).with("FeS2", 25).with("Cr2O3", 15),
            of(Assembly.Family.STELLAR, "accretion_disc", "Disque d'accrétion", "#ff9a3a")
                    .with("H2", 650).with("CO", 90).with("H2O", 60).with("SiO2", 60).with("Fe2O3", 40),
            of(Assembly.Family.STELLAR, "galactic_core_gas", "Gaz du cœur de la galaxie", "#b890e0")
                    .with("H2", 1150).with("CO", 150).with("NH3", 60).with("CH3OH", 50).with("HCN", 40).with("C2H5OH", 30).with("CH2O", 20));

    private static Assembly of(Assembly.Family family, String id, String name, String tint) {
        return Assembly.of(family, id, name, tint);
    }

    private Assemblies() {}
}
