package idle.core;

import java.util.List;

/**
 * Catalogue des astres, par échelle ({@link Body.Tier}) : 30 corps du ciel, quatre amas de roches,
 * trois comètes, quatre astéroïdes, quatre lunes et six planètes, puis cinq étoiles, deux étoiles
 * mortes, un trou noir et le trou noir supermassif.
 *
 * <p>Un astre est fait d'assemblages, et chaque assemblage du catalogue entre dans un astre et un
 * seul : un pour un amas, deux pour une comète, un astéroïde ou une lune, trois pour une planète,
 * un seul, mais énorme, pour une étoile, deux pour une étoile morte ou un trou noir, trois pour le
 * trou noir supermassif. Les 58 assemblages y trouvent tous leur place, et un astre plus gros est
 * donc aussi plus gros dans l'espace.
 *
 * <p>Chaque astre au-delà des amas part d'astres de l'échelle d'en dessous : une comète glacée d'un
 * amas glacé, une lune rocheuse de deux astéroïdes, une géante gazeuse de deux lunes et d'un
 * astéroïde, une naine brune de la géante gazeuse, le Soleil de trois planètes, une naine blanche
 * de la géante rouge et du Soleil, un trou noir d'une étoile à neutrons. S'y ajoutent de la matière
 * rassemblée et des molécules, de plus en plus : quelques centaines de molécules pour un amas,
 * plusieurs milliers pour une planète, des dizaines de milliers de molécules de gaz pour une étoile
 * ou un trou noir. Un gaz occupe vingt fois la place d'un cristal ({@link Molecule.State#spaceFactor()}) :
 * c'est l'espace qui fait le prix d'un astre, et celui des étoiles vient de là.
 *
 * <p>Ces nombres sont passés par la simulation du troisième acte, paliers de Big Bang compris
 * ({@link BigBangMilestones}). Un joueur présent qui enchaîne les Big Bangs jusqu'au cinquième forme
 * son premier amas onze heures après le premier Big Bang et la galaxie à seize heures ; en
 * s'arrêtant au quatrième, dix-sept heures ; au troisième, vingt-six heures et demie ; au deuxième,
 * trente-sept heures et demie. Le cinquième Big Bang garde l'arbre de matière noire : il ne coûte
 * qu'un quart d'heure de chemin à refaire au lieu de trois heures, et c'est ce qui le rend rentable.
 *
 * <p>Trois astres seulement augmentent l'espace : l'astéroïde glacé, la lune volcanique, et le trou
 * noir supermassif, tout à la fin. L'espace s'entretient lui-même : chaque astre de plus qui
 * l'accélère rendrait tous les suivants trop faciles.
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
                    .made("sandy_rock").matter(CRYSTAL, 400).molecules("SiO2", 250),
            Body.of(Body.Tier.RUBBLE, "icy_rubble", "Amas glacé", Body.Look.ICY, "#d8eef4", "#7fb4d8", Molecule.Stat.PARTICLES)
                    .made("sparkling_water").matter(LIQUID, 350).matter(GAS, 40).molecules("H2O", 350),
            Body.of(Body.Tier.RUBBLE, "metal_rubble", "Amas ferreux", Body.Look.MOLTEN, "#6d5a52", "#e0863c", Molecule.Stat.ATOMS)
                    .made("iron_ore").matter(CRYSTAL, 400).molecules("Fe2O3", 150).molecules("Fe3O4", 80),
            Body.of(Body.Tier.RUBBLE, "carbon_rubble", "Amas carboné", Body.Look.DARK, "#3a3430", "#8a7a6a", Molecule.Stat.DARK_GROWTH)
                    .made("bitumen").matter(SOLID, 120).matter(LIQUID, 200).molecules("C10H8", 80),

            // Les comètes : un amas, de la glace, et ce qui s'en échappe.
            Body.of(Body.Tier.COMET, "icy_comet", "Comète glacée", Body.Look.ICY, "#e4f4fa", "#8fd0f0", Molecule.Stat.PARTICLES)
                    .from("icy_rubble").made("comet_ice", "spring_water").matter(LIQUID, 600).matter(GAS, 300)
                    .molecules("H2O", 600).molecules("CO2", 80),
            Body.of(Body.Tier.COMET, "dusty_comet", "Comète poussiéreuse", Body.Look.ROCKY, "#c9b89a", "#f0dca8", Molecule.Stat.ATOMS)
                    .from("icy_rubble", "rocky_rubble").made("clay_rock", "phosphate_rock").matter(CRYSTAL, 600).matter(LIQUID, 400)
                    .molecules("SiO2", 300).molecules("H2O", 300),
            Body.of(Body.Tier.COMET, "dark_comet", "Comète sombre", Body.Look.DARK, "#4a423c", "#9fb0c8", Molecule.Stat.DARK_GROWTH)
                    .from("icy_rubble", "carbon_rubble").made("petroleum", "thermal_water").matter(SOLID, 200).matter(GAS, 300)
                    .molecules("CO", 120).molecules("HCN", 15),

            // Les astéroïdes : des amas et des comètes soudés en un bloc.
            Body.of(Body.Tier.ASTEROID, "stony_asteroid", "Astéroïde pierreux", Body.Look.ROCKY, "#9a8f84", "#5f574f", Molecule.Stat.ATOMS)
                    .from("rocky_rubble", "dusty_comet").made("meteorite", "peridot_gems").matter(CRYSTAL, 1000)
                    .molecules("Mg2SiO4", 300).molecules("MgSiO3", 200),
            Body.of(Body.Tier.ASTEROID, "metal_asteroid", "Astéroïde métallique", Body.Look.MOLTEN, "#7c7f88", "#f0b060", Molecule.Stat.PARTICLES)
                    .from("metal_rubble", "dusty_comet").made("rare_metal_ore", "tin_tungsten_ore").matter(CRYSTAL, 900)
                    .matter(METAL, 60).molecules("Fe3O4", 200).molecules("NiO", 15),
            Body.of(Body.Tier.ASTEROID, "carbon_asteroid", "Astéroïde carboné", Body.Look.DARK, "#2f2b28", "#7a6a5c", Molecule.Stat.DARK_GROWTH)
                    .from("carbon_rubble", "dark_comet").made("limestone_rock", "volcanic_rain").matter(SOLID, 300)
                    .matter(CRYSTAL, 700).molecules("Al2Si2O5(OH)4", 200).molecules("H2O", 200),
            Body.of(Body.Tier.ASTEROID, "icy_asteroid", "Astéroïde glacé", Body.Look.ICY, "#cfe4ee", "#6fa8d0", Molecule.Stat.SPACE)
                    .from("icy_rubble", "icy_comet").made("salt_rock", "soda_lake").matter(LIQUID, 1000).matter(GAS, 400)
                    .molecules("H2O", 800).molecules("NH3", 40),

            // Les lunes : des astéroïdes assez gros pour s'arrondir.
            Body.of(Body.Tier.MOON, "rocky_moon", "Lune rocheuse", Body.Look.ROCKY, "#b8b4ac", "#7d7a74", Molecule.Stat.ATOMS)
                    .from("stony_asteroid", "metal_asteroid").made("lunar_rock", "aluminium_ore").matter(CRYSTAL, 2000)
                    .molecules("CaAl2Si2O8", 350).molecules("FeTiO3", 100),
            Body.of(Body.Tier.MOON, "icy_moon", "Lune glacée", Body.Look.ICY, "#e8f2f6", "#b08a6a", Molecule.Stat.PARTICLES)
                    .from("icy_asteroid", "stony_asteroid").made("salt_lake", "upper_atmosphere").matter(LIQUID, 2000)
                    .matter(CRYSTAL, 800).molecules("H2O", 1600),
            Body.of(Body.Tier.MOON, "volcanic_moon", "Lune volcanique", Body.Look.MOLTEN, "#d8b848", "#e0502a", Molecule.Stat.SPACE)
                    .from("stony_asteroid", "metal_asteroid").made("volcanic_gas", "acid_lake").matter(CRYSTAL, 1500)
                    .matter(SOLID, 250).matter(GAS, 500).molecules("SO2", 200).molecules("S8", 120),
            Body.of(Body.Tier.MOON, "hazy_moon", "Lune brumeuse", Body.Look.HAZY, "#d09040", "#f0c070", Molecule.Stat.DARK_GROWTH)
                    .from("icy_asteroid", "carbon_asteroid").made("icy_moon_atmosphere", "natural_gas").matter(GAS, 1600)
                    .matter(LIQUID, 1000).molecules("N2", 800).molecules("CH4", 300),

            // Les planètes : des lunes et des astéroïdes rassemblés, avec leurs roches, leurs eaux et leurs airs.
            Body.of(Body.Tier.PLANET, "rocky_planet", "Planète rocheuse", Body.Look.ROCKY, "#c0704a", "#7a4630", Molecule.Stat.ATOMS)
                    .from("rocky_moon", "stony_asteroid", "metal_asteroid")
                    .made("granitic_rock", "copper_ore", "foreign_atmosphere")
                    .matter(CRYSTAL, 4000).matter(GAS, 700).molecules("SiO2", 1000).molecules("Fe2O3", 400),
            Body.of(Body.Tier.PLANET, "ocean_planet", "Planète océan", Body.Look.OCEAN, "#2a6fc0", "#e8f0f4", Molecule.Stat.PARTICLES)
                    .from("rocky_moon", "icy_moon").made("salt_water", "air", "quartz_gems")
                    .matter(LIQUID, 4000).matter(CRYSTAL, 3000).matter(GAS, 1000).molecules("H2O", 3500).molecules("NaCl", 200),
            Body.of(Body.Tier.PLANET, "lava_planet", "Planète de lave", Body.Look.MOLTEN, "#3a2a26", "#ff7a2a", Molecule.Stat.ATOMS)
                    .from("volcanic_moon", "metal_asteroid").made("volcanic_rock", "titanium_chromium_ore", "garnet_gems")
                    .matter(CRYSTAL, 4000).matter(GAS, 800).molecules("Mg2SiO4", 1000).molecules("Fe3O4", 350),
            Body.of(Body.Tier.PLANET, "ice_planet", "Planète glacée", Body.Look.ICY, "#dcecf4", "#7fb0d8", Molecule.Stat.PARTICLES)
                    .from("icy_moon", "icy_asteroid").made("mantle_rock", "lead_zinc_ore", "topaz_gems")
                    .matter(LIQUID, 4500).matter(GAS, 700).molecules("H2O", 4000).molecules("CO2", 300),
            Body.of(Body.Tier.PLANET, "gas_giant_planet", "Géante gazeuse", Body.Look.BANDED, "#d9b38a", "#a8683c", Molecule.Stat.PARTICLES)
                    .from("hazy_moon", "icy_moon", "icy_asteroid").made("gas_giant", "rare_earth_ore", "corundum_gems")
                    .matter(GAS, 6000).molecules("H2", 3500).molecules("CH4", 350).molecules("NH3", 150),
            Body.of(Body.Tier.PLANET, "ice_giant", "Géante de glace", Body.Look.RINGED, "#8fd0e0", "#d8f0f4", Molecule.Stat.DARK_GROWTH)
                    .from("hazy_moon", "icy_moon").made("interstellar_cloud", "beryl_gems", "copper_gems")
                    .matter(GAS, 4000).matter(LIQUID, 2500).molecules("H2", 2000).molecules("H2O", 2000).molecules("CH4", 400)
                    .molecules("NH3", 200),

            // Les étoiles : des planètes géantes qui grossissent finissent par briller. Leur matière se compte par milliers.
            Body.of(Body.Tier.STAR, "brown_dwarf", "Naine brune", Body.Look.BANDED, "#8a4a5a", "#c87a6a", Molecule.Stat.DARK_GROWTH)
                    .from("gas_giant_planet").made("brown_dwarf_atmosphere").matter(GAS, 9000)
                    .molecules("H2", 6000).molecules("CH4", 500),
            Body.of(Body.Tier.STAR, "red_dwarf", "Naine rouge", Body.Look.GLOWING, "#e0503a", "#ffb07a", Molecule.Stat.PARTICLES)
                    .from("gas_giant_planet", "ice_giant").made("red_dwarf_envelope").matter(GAS, 11000).molecules("H2", 8000),
            Body.of(Body.Tier.STAR, "yellow_sun", "Soleil", Body.Look.GLOWING, "#ffcc4a", "#fff2b0", Molecule.Stat.ATOMS)
                    .from("rocky_planet", "ocean_planet", "gas_giant_planet").made("solar_plasma").matter(GAS, 13000)
                    .molecules("H2", 10000),
            Body.of(Body.Tier.STAR, "blue_giant", "Géante bleue", Body.Look.GLOWING, "#8ab8ff", "#e6f0ff", Molecule.Stat.PARTICLES)
                    .from("ice_giant", "ice_planet", "lava_planet").made("blue_giant_envelope").matter(GAS, 15000).molecules("H2", 12000).molecules("N2", 1200),
            Body.of(Body.Tier.STAR, "red_giant", "Géante rouge", Body.Look.GLOWING, "#ff7a3a", "#ffc890", Molecule.Stat.PARTICLES)
                    .from("lava_planet", "rocky_planet", "gas_giant_planet").made("red_giant_envelope").matter(GAS, 17000).matter(CRYSTAL, 4500)
                    .molecules("H2", 14000).molecules("CO", 600),

            // Ce qu'une étoile laisse en mourant : petit, et très dense.
            Body.of(Body.Tier.REMNANT, "white_dwarf", "Naine blanche", Body.Look.GLOWING, "#eef4ff", "#bcd6ff", Molecule.Stat.ATOMS)
                    .from("red_giant", "yellow_sun").made("carbon_core", "planetary_nebula").matter(GAS, 14000).matter(CRYSTAL, 6000)
                    .molecules("CO", 900).molecules("CO2", 500),
            Body.of(Body.Tier.REMNANT, "neutron_star", "Étoile à neutrons", Body.Look.BEAMING, "#cfe0ff", "#7ab0ff", Molecule.Stat.DARK_GROWTH)
                    .from("blue_giant").made("iron_crust", "supernova_remnant").matter(GAS, 10000).matter(CRYSTAL, 10000)
                    .matter(METAL, 200).molecules("Fe3O4", 1200),

            // Les trous noirs : ce qui reste d'une étoile trop lourde, puis celui qui tient une galaxie.
            Body.of(Body.Tier.BLACK_HOLE, "black_hole", "Trou noir", Body.Look.VOID, "#ff9a3a", "#ffe0a0", Molecule.Stat.DARK_GROWTH)
                    .from("neutron_star", "blue_giant").made("accretion_disc", "heavy_ashes").matter(GAS, 22000)
                    .matter(CRYSTAL, 6000).molecules("H2", 19000),
            Body.of(Body.Tier.CORE, "supermassive_black_hole", "Trou noir supermassif", Body.Look.VOID, "#ffb85a", "#fff0c8",
                            Molecule.Stat.SPACE)
                    .from("black_hole", "white_dwarf", "yellow_sun").made("galactic_core_gas", "bulge_dust", "stellar_wreckage")
                    .matter(GAS, 30000).matter(CRYSTAL, 10000).matter(LIQUID, 6000).molecules("H2", 26000));

    private Bodies() {}
}
