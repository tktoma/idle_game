package idle.core.bigbang;

import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import java.util.List;

/**
 * Les paliers de Big Bang : onze, répartis sur les huit premiers Big Bangs (le deuxième en donne
 * deux, le troisième trois, dont les sondes). Les trois derniers (version 0.054) donnent une raison
 * aux Big Bangs six à huit : les systèmes solaires, des places de plus, les matières exotiques ; la
 * galaxie demande les premiers et les derniers. Au-delà du cinquième, un Big Bang n'apporte plus que ce qu'il apporte toujours : une unité
 * d'espace de plus par seconde.
 *
 * <p>Chacun répond à ce qui freine le troisième acte à ce moment-là. Le premier double la matière
 * noire des explosions : c'est elle qui fixe la durée du chemin jusqu'au Big Bang suivant, qu'aucun
 * autre bonus ne raccourcit. Le deuxième double l'expansion, et donne la création automatique : c'est
 * le moment où les assemblages et les astres s'ouvrent, et où il faudrait cliquer des centaines de
 * fois. Le troisième double ce que les amas attirent, quand les astres se mettent à demander des
 * milliers de molécules, et garde les défis réussis : les refaire prenait plus d'une heure à chaque
 * Big Bang, pour rien de neuf. Le quatrième fait dépendre l'expansion de la matière noire en réserve :
 * elle ne servait plus à rien une fois l'arbre fini, et chaque explosion de plus compte désormais.
 * Le cinquième triple l'expansion, pour les étoiles et les trous noirs, et garde l'arbre de matière
 * noire à travers le Big Bang, celui-là compris : sans cela ce Big Bang coûtait trois heures de
 * chemin à refaire, plus que ce qu'il restait à attendre jusqu'à la galaxie.
 *
 * <p>Ces nombres sont passés par la simulation du troisième acte ({@link Bodies}).
 */
public final class BigBangMilestones {

    /** Les ressources des prix (version 0.124). */
    private static final idle.core.Resource P = idle.core.Resource.PARTICLES, A = idle.core.Resource.ATOMS;

    public static final List<BigBangMilestone> DEFAULT = List.of(
            new BigBangMilestone(1, "Matière noire primordiale", List.of(new BigBangMilestone.DarkMatter(2)), P, BigNum.of(1, 90)),
            new BigBangMilestone(2, "Expansion accélérée", List.of(new BigBangMilestone.Boost(Molecule.Stat.SPACE, 2)), P, BigNum.of(1, 220)),
            new BigBangMilestone(2, "Création automatique", List.of(new BigBangMilestone.AutoMolecules()), P, BigNum.of(1, 160)),
            new BigBangMilestone(3, "Gravité", List.of(new BigBangMilestone.Accretion(2)), P, BigNum.of(1, 400)),
            new BigBangMilestone(3, "Mémoire des défis", List.of(new BigBangMilestone.KeepChallenges()), P, BigNum.of(1, 700)),
            new BigBangMilestone(3, "Sondes spatiales", List.of(new BigBangMilestone.Probes()), A, BigNum.of(1, 10)),
            new BigBangMilestone(4, "Énergie sombre", List.of(new BigBangMilestone.DarkMatterSpace(0.1)), P, BigNum.of(1, 1300)),
            new BigBangMilestone(5, "Univers mûr", List.of(new BigBangMilestone.Boost(Molecule.Stat.SPACE, 3),
                    new BigBangMilestone.KeepDarkTree()), P, BigNum.of(1, 17500)),
            new BigBangMilestone(6, "Systèmes solaires", List.of(new BigBangMilestone.SolarSystems()), P, BigNum.of(1, 43000)),
            new BigBangMilestone(7, "Ateliers du ciel", List.of(new BigBangMilestone.MorePlaces(2, 1)), A, BigNum.of(3, 21)),
            new BigBangMilestone(8, "Matières exotiques", List.of(new BigBangMilestone.ExoticMatter()), P, BigNum.of(1, 47000)));

    private BigBangMilestones() {}
}
