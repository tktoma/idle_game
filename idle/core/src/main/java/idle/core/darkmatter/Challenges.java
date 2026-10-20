package idle.core.darkmatter;

import java.util.List;

/**
 * Catalogue des défis d'explosion, dans l'ordre où ils s'ouvrent.
 *
 * <p>Chaque défi retourne une habitude du jeu, et sa récompense répond à sa contrainte : qui a
 * joué avec des automatismes rouillés repart avec des automatismes plus vifs, qui a rempli un
 * tableau inerte voit ensuite ses éléments agir plus fort.
 *
 * <p>Les seuils de matière noire sont des seuils d'accès, pas des conseils. Un défi se rejoue
 * depuis le premier générateur, sans les mémoires de la matière noire, mais avec un tableau de
 * masse 1 ({@link Game#tableWeight()}) : tenté dès son ouverture, il est souvent plus court
 * qu'une partie ordinaire du même moment, et sa première réussite rapporte la matière noire de
 * l'explosion. C'est un raccourci autant qu'une épreuve : les tenter tous dès leur ouverture
 * retire une dizaine d'heures au deuxième acte. Rejoué, il ne compte plus que pour son temps.
 * Durées simulées à l'ouverture, en cliquant soi-même : une demi-heure pour Rouille, un quart
 * d'heure pour Fusion lourde, quelques minutes pour les suivants.
 *
 * <p>Les cinq premiers s'ouvrent tous les trois matières noires, de 3 à 15, pendant que chaque
 * explosion n'en rapporte qu'une ; le sixième attend la Condensation. Les quatre derniers (version
 * 0.071) jouent sur l'arbre lui-même : ils éteignent une branche, les voies et les liaisons, ou tout
 * sauf un style. « Présence » et « Absence », corsés en 0.073, s'ouvrent à 300 et 350 matières
 * noires : avec les voies du bon style ils prennent une ou deux minutes, sans elles vingt à
 * quarante (mesuré au robot).
 */
public final class Challenges {

    public static final List<Challenge> DEFAULT = List.of(
            // Sans toucher à rien, il demande des heures ; à la main, une demi-heure de clics.
            new Challenge("challenge_rust", "Rouille", Challenge.Rule.RUSTY_AUTOMATIONS, 3,
                    Challenge.Reward.FASTER_AUTOMATIONS),
            new Challenge("challenge_heavy", "Fusion lourde", Challenge.Rule.HEAVY_FUSION, 6,
                    Challenge.Reward.GROUP_BONUS),
            // Rien ne s'économise : chaque achat doit tenir dans une seconde et demie de production.
            new Challenge("challenge_volatile", "Particules volatiles", Challenge.Rule.VOLATILE_PARTICLES, 9,
                    Challenge.Reward.STRONGER_MILESTONES),
            // Faisable à condition de ne plus acheter d'améliorations en atomes : elles sont perdues à la
            // fusion suivante. Les atomes vont aux automatismes et à la synthèse. Sinon, il ne se finit pas.
            new Challenge("challenge_amnesia", "Amnésie", Challenge.Rule.AMNESIA, 12,
                    Challenge.Reward.CHEAPER_ATOM_UPGRADES),
            new Challenge("challenge_inert", "Éléments inertes", Challenge.Rule.INERT_ELEMENTS, 15,
                    Challenge.Reward.STRONGER_ELEMENTS),
            // Avec un taux deux fois et demie plus fort, il ne se finit plus à ce stade.
            new Challenge("challenge_decay", "Désintégration", Challenge.Rule.DECAY, 21,
                    Challenge.Reward.EXTRA_ELEMENT),
            // Version 0.070 : quatre défis qui demandent un autre passage dans l'arbre. Les voies prises jusqu'au
            // prochain Big Bang décident de leur difficulté : « Présence » est court avec les voies de gauche, long
            // sans ; « Absence » l'inverse. Réussir les dix demande donc soit de changer de voies d'un Big Bang à
            // l'autre, soit d'accepter un défi plus long.
            new Challenge("challenge_cut", "Branche coupée", Challenge.Rule.CUT_BRANCH, 24,
                    Challenge.Reward.STRONGER_LINKS),
            new Challenge("challenge_bare", "Tronc nu", Challenge.Rule.BARE_TREE, 27,
                    Challenge.Reward.EXTRA_DARK_MATTER),
            // Corsés en 0.073 : ils s'ouvrent tard, quand l'arbre a toutes ses voies, et se jouent sur un tableau très
            // lourd ({@link idle.core.Game#STYLE_CHALLENGE_WEIGHT}), où seul agit ce qui est du bon style.
            new Challenge("challenge_presence", "Présence", Challenge.Rule.PRESENCE_ONLY, 300,
                    Challenge.Reward.STRONGER_PRESENCE),
            new Challenge("challenge_absence", "Absence", Challenge.Rule.ABSENCE_ONLY, 350,
                    Challenge.Reward.STRONGER_ABSENCE));

    private Challenges() {}
}
