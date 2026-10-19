package idle.core.sky;

import java.util.List;

/**
 * Un système solaire (version 0.54) : une étoile formée, ses planètes de la plus proche à la plus
 * lointaine, et des lunes. Chaque astre n'appartient qu'à un système. Un système est complet à
 * partir de {@link Game#SYSTEM_MIN_PLANETS} planètes ; la galaxie en demande
 * {@link Game#SYSTEMS_FOR_GALAXY}.
 *
 * @param star    l'étoile
 * @param planets les planètes, dans l'ordre des orbites
 * @param moons   les lunes
 */
public record SolarSystem(String star, List<String> planets, List<String> moons) {

    public SolarSystem {
        if (star == null || star.isBlank()) throw new IllegalArgumentException("Système sans étoile");
        planets = List.copyOf(planets);
        moons = List.copyOf(moons);
    }

    /** Le système en une ligne : « étoile|planète,planète|lune », comme dans la sauvegarde. */
    public String line() {
        return star + "|" + String.join(",", planets) + "|" + String.join(",", moons);
    }

    /** Relit une ligne écrite par {@link #line()} ; {@code null} si elle ne se lit pas. */
    public static SolarSystem parse(String line) {
        if (line == null) return null;
        String[] parts = line.trim().split("\\|", -1);
        if (parts.length != 3 || parts[0].isBlank()) return null;
        return new SolarSystem(parts[0].trim(), split(parts[1]), split(parts[2]));
    }

    private static List<String> split(String list) {
        List<String> ids = new java.util.ArrayList<>();
        for (String id : list.split(",")) if (!id.isBlank()) ids.add(id.trim());
        return ids;
    }

    /** Le même système, avec ces planètes. */
    public SolarSystem withPlanets(List<String> list) {
        return new SolarSystem(star, list, moons);
    }

    /** Le même système, avec ces lunes. */
    public SolarSystem withMoons(List<String> list) {
        return new SolarSystem(star, planets, list);
    }
}
