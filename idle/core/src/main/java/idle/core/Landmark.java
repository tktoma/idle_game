package idle.core;

/**
 * Un repère de l'échelle des grandeurs : quelque chose de connu, et sa taille.
 *
 * @param name nom affiché au joueur, sans article (« atome d'hydrogène »)
 * @param size taille en mètres (diamètre, largeur ou distance, selon l'objet)
 */
public record Landmark(String name, BigNum size) {

    public Landmark {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("nom manquant");
        if (size.sign() <= 0) throw new IllegalArgumentException("La taille doit être positive");
    }
}
