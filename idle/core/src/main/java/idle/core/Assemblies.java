package idle.core;

import java.util.List;

/**
 * Catalogue des assemblages, rangés par famille ({@link Assembly.Family}) : 44 matières qu'on
 * trouve dans la nature, de la roche granitique à la glace de comète. Rien de vivant, et aucune
 * molécule du rayon du vivant n'y entre ; rien de fabriqué non plus, ni verre, ni alliage, ni
 * batterie. Le catalogue est large à dessein, pour pouvoir y trier ensuite.
 *
 * <p>Les noms sont généraux : un assemblage est une sorte de matière (« Roche calcaire », « Minerai
 * de cuivre », « Lac salé »), pas un échantillon précis, et réunit trois à huit sortes de molécules
 * qui vont ensemble.
 *
 * <p>Chaque assemblage demande 200 à 420 molécules, dans des proportions proches de la matière
 * réelle : la base en centaines, les traces à l'unité (quatre oxydes de chrome dans trois cents
 * corindons). Ces nombres viennent de mémoire et sont un premier réglage : ils sont choisis pour
 * que le bloc soit grand à côté d'une molécule, pas encore pour être atteints en jouant.
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
            of(Assembly.Family.ROCK, "phosphate_rock", "Roche phosphatée", "#8a7a5c")
                    .with("Ca5(PO4)3F", 150).with("Ca5(PO4)3OH", 40).with("CaCO3", 50).with("SiO2", 20),
            of(Assembly.Family.ROCK, "lunar_rock", "Roche lunaire", "#b8b4ac")
                    .with("CaAl2Si2O8", 150).with("FeTiO3", 50).with("MgSiO3", 50).with("Mg2SiO4", 30),
            of(Assembly.Family.ROCK, "meteorite", "Météorite", "#5a5048")
                    .with("Mg2SiO4", 130).with("MgSiO3", 90).with("Fe3O4", 30).with("FeS2", 20).with("NiO", 5),

            // Les minerais : ce dont on tire les métaux, avec la roche qui les porte.
            of(Assembly.Family.ORE, "iron_ore", "Minerai de fer", "#8b3a2e")
                    .with("Fe2O3", 140).with("Fe3O4", 60).with("FeO(OH)", 40).with("FeCO3", 20).with("SiO2", 40),
            of(Assembly.Family.ORE, "aluminium_ore", "Minerai d'aluminium", "#b0552f")
                    .with("AlO(OH)", 160).with("Al2O3", 30).with("FeO(OH)", 50).with("Al2Si2O5(OH)4", 30).with("TiO2", 10),
            of(Assembly.Family.ORE, "copper_ore", "Minerai de cuivre", "#b8963c")
                    .with("CuFeS2", 140).with("Cu2S", 40).with("CuS", 20).with("FeS2", 60).with("Cu2O", 10).with("SiO2", 30),
            of(Assembly.Family.ORE, "lead_zinc_ore", "Minerai de plomb, de zinc et d'argent", "#6f7480")
                    .with("PbS", 110).with("ZnS", 110).with("FeS2", 40).with("Ag2S", 10).with("PbCO3", 10).with("ZnCO3", 10).with("CaCO3", 20),
            of(Assembly.Family.ORE, "tin_tungsten_ore", "Minerai d'étain et de tungstène", "#5a4a3c")
                    .with("SnO2", 120).with("CaWO4", 50).with("FeWO4", 30).with("SiO2", 80),
            of(Assembly.Family.ORE, "titanium_chromium_ore", "Minerai de titane et de chrome", "#5d5a6b")
                    .with("TiO2", 100).with("FeTiO3", 90).with("FeCr2O4", 60).with("MnO2", 20),
            of(Assembly.Family.ORE, "rare_metal_ore", "Minerai de métaux rares", "#7c6f8f")
                    .with("MoS2", 60).with("CoAsS", 40).with("NiAs", 40).with("Sb2S3", 30).with("Bi2S3", 20).with("HgS", 20)
                    .with("FeTa2O6", 20).with("FeNb2O6", 20),
            of(Assembly.Family.ORE, "rare_earth_ore", "Minerai de terres rares", "#b7a36e")
                    .with("CePO4", 110).with("CeCO3F", 90).with("YPO4", 40).with("CaF2", 20),

            // Les pierres précieuses : un cristal presque pur, et les traces qui lui donnent ses couleurs.
            of(Assembly.Family.GEM, "corundum_gems", "Rubis et saphirs", "#d1264f")
                    .with("Al2O3", 300).with("Cr2O3", 4).with("Fe2O3", 3).with("TiO2", 2),
            of(Assembly.Family.GEM, "beryl_gems", "Émeraudes et aigues-marines", "#1fa565")
                    .with("Be3Al2Si6O18", 200).with("Cr2O3", 4).with("Fe2O3", 3).with("V2O5", 1),
            of(Assembly.Family.GEM, "quartz_gems", "Quartz colorés", "#9b5fd0")
                    .with("SiO2", 320).with("H2O", 20).with("Fe2O3", 3).with("TiO2", 2).with("MnO2", 1),
            of(Assembly.Family.GEM, "garnet_gems", "Grenats et spinelles", "#8e1c2c")
                    .with("Mg3Al2Si3O12", 130).with("MgAl2O4", 100).with("Fe2O3", 15).with("Cr2O3", 4),
            of(Assembly.Family.GEM, "copper_gems", "Pierres de cuivre", "#1f8a5b")
                    .with("Cu2CO3(OH)2", 180).with("Cu3(CO3)2(OH)2", 60).with("CuO", 5),
            of(Assembly.Family.GEM, "topaz_gems", "Topazes et zircons", "#f0a030")
                    .with("Al2SiO4F2", 150).with("ZrSiO4", 110).with("HfO2", 3).with("Cr2O3", 2),
            of(Assembly.Family.GEM, "peridot_gems", "Péridots", "#a6c838")
                    .with("Mg2SiO4", 240).with("Fe2O3", 20).with("NiO", 2),

            // Les eaux et les glaces : de l'eau, et ce qui y est dissous ou pris.
            of(Assembly.Family.WATER, "salt_water", "Eau salée", "#1f5f9e")
                    .with("H2O", 380).with("NaCl", 30).with("MgCl2", 4).with("MgSO4", 3).with("KCl", 2).with("CaCl2", 1),
            of(Assembly.Family.WATER, "spring_water", "Eau de source", "#5aa9e0")
                    .with("H2O", 350).with("CaCO3", 3).with("CO2", 3).with("MgSO4", 2).with("NaHCO3", 2),
            of(Assembly.Family.WATER, "sparkling_water", "Eau pétillante", "#7fc4ea")
                    .with("H2O", 300).with("CO2", 25).with("H2CO3", 10),
            of(Assembly.Family.WATER, "thermal_water", "Eau thermale", "#6fb8a8")
                    .with("H2O", 300).with("H2S", 10).with("CO2", 10).with("SiO2", 10).with("NaCl", 10).with("H3BO3", 5),
            of(Assembly.Family.WATER, "salt_lake", "Lac salé", "#c9d9d0")
                    .with("H2O", 240).with("NaCl", 80).with("KCl", 15).with("MgCl2", 15).with("CaCl2", 5).with("LiCl", 5),
            of(Assembly.Family.WATER, "acid_lake", "Lac acide", "#9fc24a")
                    .with("H2O", 300).with("H2SO4", 30).with("HCl", 20).with("HF", 5).with("H2SO3", 5),
            of(Assembly.Family.WATER, "soda_lake", "Lac de soude", "#c98a9a")
                    .with("H2O", 280).with("Na2CO3", 40).with("NaHCO3", 30).with("NaCl", 20).with("NaF", 3),
            of(Assembly.Family.WATER, "volcanic_rain", "Pluie volcanique", "#8fa87a")
                    .with("H2O", 400).with("H2CO3", 5).with("H2SO4", 4).with("HNO3", 3).with("H2SO3", 2),
            of(Assembly.Family.WATER, "comet_ice", "Glace de comète", "#d8eef4")
                    .with("H2O", 300).with("CO2", 40).with("CO", 30).with("NH3", 10).with("CH4", 10).with("CH3OH", 8).with("HCN", 2),

            // Les gaz mêlés : les airs d'ici et d'ailleurs.
            of(Assembly.Family.AIR, "air", "Air", "#bcd8ee")
                    .with("N2", 312).with("O2", 84).with("CO2", 4),
            of(Assembly.Family.AIR, "upper_atmosphere", "Haute atmosphère", "#7a9ad8")
                    .with("N2", 250).with("O2", 60).with("O3", 30).with("NO", 5).with("NO2", 5),
            of(Assembly.Family.AIR, "volcanic_gas", "Gaz volcanique", "#a8907a")
                    .with("H2O", 200).with("CO2", 60).with("SO2", 30).with("H2S", 6).with("HCl", 4),
            of(Assembly.Family.AIR, "foreign_atmosphere", "Atmosphère d'un autre monde", "#d8b060")
                    .with("CO2", 250).with("N2", 40).with("CH4", 15).with("SO2", 3),
            of(Assembly.Family.AIR, "icy_moon_atmosphere", "Atmosphère d'une lune glacée", "#d09040")
                    .with("N2", 285).with("CH4", 20).with("C2H6", 5).with("C2H2", 3).with("HCN", 2),
            of(Assembly.Family.AIR, "gas_giant", "Atmosphère d'une géante gazeuse", "#d9b38a")
                    .with("H2", 330).with("CH4", 20).with("NH3", 10).with("H2O", 5).with("H2S", 3).with("PH3", 2),
            of(Assembly.Family.AIR, "interstellar_cloud", "Nuage interstellaire", "#6a5a9a")
                    .with("H2", 300).with("CO", 40).with("H2O", 20).with("NH3", 10).with("CH2O", 5).with("CH3OH", 5).with("HCN", 5),

            // Les hydrocarbures du sous-sol.
            of(Assembly.Family.FUEL, "petroleum", "Pétrole", "#3a322a")
                    .with("C8H18", 120).with("C6H14", 70).with("C5H12", 40).with("C6H12", 30).with("C7H8", 30).with("C8H10", 20).with("C6H6", 10),
            of(Assembly.Family.FUEL, "natural_gas", "Gaz naturel", "#8fa0c8")
                    .with("CH4", 240).with("C2H6", 30).with("C3H8", 15).with("C4H10", 10).with("CO2", 5),
            of(Assembly.Family.FUEL, "bitumen", "Bitume", "#26221e")
                    .with("C10H8", 80).with("C14H10", 60).with("C7H8", 40).with("C8H10", 40).with("C8H18", 30).with("C4H4S", 10));

    private static Assembly of(Assembly.Family family, String id, String name, String tint) {
        return Assembly.of(family, id, name, tint);
    }

    private Assemblies() {}
}
