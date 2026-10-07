package idle.core;

import java.util.List;

/**
 * Les paliers de Big Bang : sept, répartis sur les cinq premiers Big Bangs (le deuxième et le
 * troisième en donnent deux). Au-delà du cinquième, un Big Bang n'apporte plus que ce qu'il apporte toujours : une unité
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

    public static final List<BigBangMilestone> DEFAULT = List.of(
            new BigBangMilestone(1, "Matière noire primordiale", List.of(new BigBangMilestone.DarkMatter(2))),
            new BigBangMilestone(2, "Expansion accélérée", List.of(new BigBangMilestone.Boost(Molecule.Stat.SPACE, 2))),
            new BigBangMilestone(2, "Création automatique", List.of(new BigBangMilestone.AutoMolecules())),
            new BigBangMilestone(3, "Gravité", List.of(new BigBangMilestone.Accretion(2))),
            new BigBangMilestone(3, "Mémoire des défis", List.of(new BigBangMilestone.KeepChallenges())),
            new BigBangMilestone(4, "Énergie sombre", List.of(new BigBangMilestone.DarkMatterSpace(0.1))),
            new BigBangMilestone(5, "Univers mûr", List.of(new BigBangMilestone.Boost(Molecule.Stat.SPACE, 3),
                    new BigBangMilestone.KeepDarkTree())));

    private BigBangMilestones() {}
}
