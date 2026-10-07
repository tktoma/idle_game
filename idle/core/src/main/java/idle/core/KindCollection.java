package idle.core;

/**
 * La collection d'un rayon du catalogue de molécules : ce que rapporte le fait d'avoir créé au
 * moins une molécule de la moitié de ses sortes, puis de toutes ({@link Game#collectionLevel}).
 * C'est une raison de créer les molécules qui ne servent à aucun astre, sur le modèle des
 * ensembles du tableau périodique.
 *
 * <p>Les deux facteurs ne s'ajoutent pas : la collection complète remplace la moitié. Ils
 * multiplient la grandeur à côté des améliorations d'espace et des paliers de Big Bang, et rien ne
 * les défait, puisqu'une molécule créée l'est pour toujours.
 *
 * @param kind le rayon
 * @param stat la grandeur que la collection multiplie
 * @param half le facteur une fois la moitié des sortes créées (arrondie au-dessus)
 * @param full le facteur une fois toutes les sortes créées
 */
public record KindCollection(Molecule.Kind kind, Molecule.Stat stat, double half, double full) {

    public KindCollection {
        if (kind == null || stat == null) throw new IllegalArgumentException("rayon ou grandeur manquant");
        if (half < 1 || full < half) throw new IllegalArgumentException("Une collection complète rapporte au moins autant que sa moitié : " + kind);
    }

    /** Le facteur d'un niveau : 1 sans rien, {@link #half()} à la moitié, {@link #full()} une fois complète. */
    public double factor(int level) {
        return level >= 2 ? full : level == 1 ? half : 1;
    }
}
