package idle.core;

import java.util.List;

/**
 * Catalogue des astres, par échelle ({@link Body.Tier}) : 21 corps du ciel, quatre amas de roches,
 * trois comètes, quatre astéroïdes, quatre lunes et six planètes.
 *
 * <p>Chaque astre au-delà des amas part d'astres de l'échelle d'en dessous : une comète glacée d'un
 * amas glacé, une lune rocheuse de deux astéroïdes, une géante gazeuse de deux lunes et d'un
 * astéroïde. S'y ajoutent des assemblages, de la matière rassemblée et des molécules, de plus en
 * plus : quelques centaines de molécules pour un amas, plusieurs milliers pour une planète. Ces
 * nombres sont un premier réglage, choisis pour que la taille d'un astre reste comparable à ce
 * qu'il représente ; ils ne sont pas encore pensés pour être atteints en jouant.
 */
public final class Bodies {

    private static final Molecule.State GAS = Molecule.State.GAS;
    private static final Molecule.State LIQUID = Molecule.State.LIQUID;
    private static final Molecule.State SOLID = Molecule.State.SOLID;
    private static final Molecule.State CRYSTAL = Molecule.State.CRYSTAL;
    private static final Molecule.State METAL = Molecule.State.METAL;

    public static final List<Body> DEFAULT = List.of(
            // Les amas de roches : des cailloux tenus ensemble, faits d'assemblages.
            Body.of(Body.Tier.RUBBLE, "rocky_rubble", "Amas rocheux", Body.Look.ROCKY, "#a79a94", "#6f655f", Molecule.Stat.ATOMS)
                    .made("granitic_rock", "volcanic_rock", "sandy_rock").matter(CRYSTAL, 400).molecules("SiO2", 250),
            Body.of(Body.Tier.RUBBLE, "icy_rubble", "Amas glacé", Body.Look.ICY, "#d8eef4", "#7fb4d8", Molecule.Stat.PARTICLES)
                    .made("comet_ice", "spring_water").matter(LIQUID, 350).matter(GAS, 80).molecules("H2O", 350),
            Body.of(Body.Tier.RUBBLE, "metal_rubble", "Amas ferreux", Body.Look.MOLTEN, "#6d5a52", "#e0863c", Molecule.Stat.ATOMS)
                    .made("iron_ore", "meteorite", "copper_ore").matter(CRYSTAL, 400).molecules("Fe2O3", 150).molecules("Fe3O4", 80),
            Body.of(Body.Tier.RUBBLE, "carbon_rubble", "Amas carboné", Body.Look.DARK, "#3a3430", "#8a7a6a", Molecule.Stat.DARK_GROWTH)
                    .made("bitumen", "petroleum", "clay_rock").matter(SOLID, 120).matter(LIQUID, 200).molecules("C10H8", 80),

            // Les comètes : un amas, de la glace, et ce qui s'en échappe.
            Body.of(Body.Tier.COMET, "icy_comet", "Comète glacée", Body.Look.ICY, "#e4f4fa", "#8fd0f0", Molecule.Stat.PARTICLES)
                    .from("icy_rubble").made("comet_ice", "interstellar_cloud").matter(LIQUID, 600).matter(GAS, 300)
                    .molecules("H2O", 600).molecules("CO2", 80),
            Body.of(Body.Tier.COMET, "dusty_comet", "Comète poussiéreuse", Body.Look.ROCKY, "#c9b89a", "#f0dca8", Molecule.Stat.ATOMS)
                    .from("icy_rubble", "rocky_rubble").made("comet_ice", "sandy_rock").matter(CRYSTAL, 600).matter(LIQUID, 400)
                    .molecules("SiO2", 300).molecules("H2O", 300),
            Body.of(Body.Tier.COMET, "dark_comet", "Comète sombre", Body.Look.DARK, "#4a423c", "#9fb0c8", Molecule.Stat.DARK_GROWTH)
                    .from("icy_rubble", "carbon_rubble").made("comet_ice", "bitumen").matter(SOLID, 200).matter(GAS, 300)
                    .molecules("CO", 120).molecules("HCN", 15),

            // Les astéroïdes : des amas et des comètes soudés en un bloc.
            Body.of(Body.Tier.ASTEROID, "stony_asteroid", "Astéroïde pierreux", Body.Look.ROCKY, "#9a8f84", "#5f574f", Molecule.Stat.ATOMS)
                    .from("rocky_rubble", "dusty_comet").made("meteorite", "mantle_rock", "volcanic_rock").matter(CRYSTAL, 1000)
                    .molecules("Mg2SiO4", 300).molecules("MgSiO3", 200),
            Body.of(Body.Tier.ASTEROID, "metal_asteroid", "Astéroïde métallique", Body.Look.MOLTEN, "#7c7f88", "#f0b060", Molecule.Stat.PARTICLES)
                    .from("metal_rubble", "dusty_comet").made("iron_ore", "meteorite", "rare_metal_ore").matter(CRYSTAL, 900)
                    .matter(METAL, 60).molecules("Fe3O4", 200).molecules("NiO", 15),
            Body.of(Body.Tier.ASTEROID, "carbon_asteroid", "Astéroïde carboné", Body.Look.DARK, "#2f2b28", "#7a6a5c", Molecule.Stat.DARK_GROWTH)
                    .from("carbon_rubble", "dark_comet").made("clay_rock", "bitumen", "meteorite").matter(SOLID, 300)
                    .matter(CRYSTAL, 700).molecules("Al2Si2O5(OH)4", 200).molecules("H2O", 200),
            Body.of(Body.Tier.ASTEROID, "icy_asteroid", "Astéroïde glacé", Body.Look.ICY, "#cfe4ee", "#6fa8d0", Molecule.Stat.PARTICLES)
                    .from("icy_rubble", "icy_comet").made("comet_ice", "salt_rock").matter(LIQUID, 1000).matter(GAS, 400)
                    .molecules("H2O", 800).molecules("NH3", 40),

            // Les lunes : des astéroïdes assez gros pour s'arrondir.
            Body.of(Body.Tier.MOON, "rocky_moon", "Lune rocheuse", Body.Look.ROCKY, "#b8b4ac", "#7d7a74", Molecule.Stat.ATOMS)
                    .from("stony_asteroid", "metal_asteroid").made("lunar_rock", "volcanic_rock", "mantle_rock").matter(CRYSTAL, 2000)
                    .molecules("CaAl2Si2O8", 350).molecules("FeTiO3", 100),
            Body.of(Body.Tier.MOON, "icy_moon", "Lune glacée", Body.Look.ICY, "#e8f2f6", "#b08a6a", Molecule.Stat.PARTICLES)
                    .from("icy_asteroid", "stony_asteroid").made("comet_ice", "salt_water", "salt_rock").matter(LIQUID, 2000)
                    .matter(CRYSTAL, 800).molecules("H2O", 1600),
            Body.of(Body.Tier.MOON, "volcanic_moon", "Lune volcanique", Body.Look.MOLTEN, "#d8b848", "#e0502a", Molecule.Stat.SPACE)
                    .from("stony_asteroid", "metal_asteroid").made("volcanic_rock", "volcanic_gas", "acid_lake").matter(CRYSTAL, 1500)
                    .matter(SOLID, 250).matter(GAS, 500).molecules("SO2", 200).molecules("S8", 120),
            Body.of(Body.Tier.MOON, "hazy_moon", "Lune brumeuse", Body.Look.HAZY, "#d09040", "#f0c070", Molecule.Stat.DARK_GROWTH)
                    .from("icy_asteroid", "carbon_asteroid").made("icy_moon_atmosphere", "natural_gas", "comet_ice").matter(GAS, 1600)
                    .matter(LIQUID, 1000).molecules("N2", 800).molecules("CH4", 300),

            // Les planètes : des lunes et des astéroïdes rassemblés, avec leurs roches, leurs eaux et leurs airs.
            Body.of(Body.Tier.PLANET, "rocky_planet", "Planète rocheuse", Body.Look.ROCKY, "#c0704a", "#7a4630", Molecule.Stat.ATOMS)
                    .from("rocky_moon", "stony_asteroid", "metal_asteroid")
                    .made("granitic_rock", "volcanic_rock", "mantle_rock", "iron_ore", "foreign_atmosphere")
                    .matter(CRYSTAL, 4000).matter(GAS, 700).molecules("SiO2", 1000).molecules("Fe2O3", 400),
            Body.of(Body.Tier.PLANET, "ocean_planet", "Planète océan", Body.Look.OCEAN, "#2a6fc0", "#e8f0f4", Molecule.Stat.PARTICLES)
                    .from("rocky_moon", "icy_moon").made("salt_water", "air", "granitic_rock", "limestone_rock", "mantle_rock")
                    .matter(LIQUID, 4000).matter(CRYSTAL, 3000).matter(GAS, 1000).molecules("H2O", 3500).molecules("NaCl", 200),
            Body.of(Body.Tier.PLANET, "lava_planet", "Planète de lave", Body.Look.MOLTEN, "#3a2a26", "#ff7a2a", Molecule.Stat.SPACE)
                    .from("volcanic_moon", "metal_asteroid").made("volcanic_rock", "mantle_rock", "volcanic_gas", "titanium_chromium_ore")
                    .matter(CRYSTAL, 4000).matter(GAS, 800).molecules("Mg2SiO4", 1000).molecules("Fe3O4", 350),
            Body.of(Body.Tier.PLANET, "ice_planet", "Planète glacée", Body.Look.ICY, "#dcecf4", "#7fb0d8", Molecule.Stat.PARTICLES)
                    .from("icy_moon", "icy_asteroid").made("comet_ice", "salt_water", "salt_lake", "foreign_atmosphere")
                    .matter(LIQUID, 4500).matter(GAS, 700).molecules("H2O", 4000).molecules("CO2", 300),
            Body.of(Body.Tier.PLANET, "gas_giant_planet", "Géante gazeuse", Body.Look.BANDED, "#d9b38a", "#a8683c", Molecule.Stat.SPACE)
                    .from("hazy_moon", "icy_moon", "icy_asteroid").made("gas_giant", "interstellar_cloud", "natural_gas")
                    .matter(GAS, 6000).molecules("H2", 3500).molecules("CH4", 350).molecules("NH3", 150),
            Body.of(Body.Tier.PLANET, "ice_giant", "Géante de glace", Body.Look.RINGED, "#8fd0e0", "#d8f0f4", Molecule.Stat.DARK_GROWTH)
                    .from("hazy_moon", "icy_moon").made("gas_giant", "comet_ice", "icy_moon_atmosphere")
                    .matter(GAS, 4000).matter(LIQUID, 2500).molecules("H2", 2000).molecules("H2O", 2000).molecules("CH4", 400)
                    .molecules("NH3", 200));

    private Bodies() {}
}
