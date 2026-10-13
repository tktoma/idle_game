package idle.core;

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

    /** Le même système, avec ces planètes. */
    public SolarSystem withPlanets(List<String> list) {
        return new SolarSystem(star, list, moons);
    }

    /** Le même système, avec ces lunes. */
    public SolarSystem withMoons(List<String> list) {
        return new SolarSystem(star, planets, list);
    }
}
