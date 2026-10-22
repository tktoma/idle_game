package idle.core.darkmatter;

import idle.core.math.BigNum;
import idle.core.upgrades.Upgrade;
import java.util.List;

/**
 * Les améliorations de matière noire : l'arbre, puis celles qui se paient en matière noire.
 *
 * <p>L'arbre a trois branches, dont chaque case est débloquée par la case du dessus. Depuis la
 * version 0.069, des bifurcations (sept depuis la 0.070) ({@link DarkUpgrade#fork()}) proposent deux voies côte à côte :
 * en prendre une ferme l'autre jusqu'au prochain Big Bang, et la case d'après demande l'une ou
 * l'autre. Trois cases de plus ferment les branches. Depuis la
 * 0.070, six liaisons ({@link DarkUpgrade#isLink()}) relient les voies de deux branches voisines,
 * et la « Superposition » du collisionneur ouvre les deux voies de chaque bifurcation.
 *
 * <ul>
 *   <li><b>particules</b> : se paie avec les particules de la partie en cours ;</li>
 *   <li><b>atomes</b> : se paie avec les atomes disponibles. Tant que le plafond d'atomes n'est
 *       pas levé, rien ne peut coûter plus de 118 atomes ;</li>
 *   <li><b>taille</b> : se débloque en faisant grossir la matière noire, sans rien dépenser.</li>
 * </ul>
 *
 * <p>Depuis la version 0.070, aucune case n'attend plus un nombre de matières noires gagnées : seuls
 * les prix rythment l'arbre. Ils ont été réglés au robot sur ce que chaque partie du deuxième acte
 * réunit (particules, atomes, taille), pour que l'arbre s'étale sur une vingtaine d'explosions au lieu
 * de tomber d'un coup. Les bifurcations sont fortes, et leurs deux voies se valent à quelques minutes
 * près : aucune ne rapporte de matière noire par explosion, ce qui ferait de celle-là le seul bon choix
 * (la matière noire en réserve est ce qui ferme le deuxième acte). Ce sont les accords de présence
 * qui en rapportent, pour qui garde un style.
 *
 * <p>Hors de l'arbre, six améliorations se paient en matière noire
 * ({@link DarkUpgrade.Branch#DARK_MATTER}). La matière noire dépensée reste comptée comme gagnée,
 * et leur effet grandit avec toute la matière noire gagnée : chaque explosion les renforce.
 *
 * <p>Depuis la version 0.071, la voie de gauche de chaque bifurcation est celle de la présence et la
 * voie de droite celle de l'absence ({@link Game#darkStyle}) : chaque voie d'absence rend un peu plus
 * du temps passé jeu fermé. Quatre accords demandent trois voies du même style : ceux de présence
 * rapportent de la matière noire à chaque explosion, ceux d'absence du temps d'absence. Un ensemble
 * cohérent rapporte ainsi plus qu'un tirage, et le bon ensemble dépend de la façon de jouer.
 *
 * <p>Réglage vérifié par simulation (version 0.073, robot qui choisit bien) : deuxième acte de 5 h
 * avec toutes les voies de présence, 5 h 30 avec toutes celles d'absence. Les cases du bas de
 * chaque branche s'étalent jusqu'à la fin de l'acte : Singularité en trente niveaux, Réacteur en
 * huit, Horizon en dix. Le premier Big Bang demande 10¹⁶⁵³⁰ particules depuis la version 0.124 ({@link Game#BIG_BANG_PARTICLES}) ; les prix en particules de l'arbre ont suivi (×0,435 en ordres de grandeur).
 */
public final class DarkUpgrades {

    public static final List<DarkUpgrade> DEFAULT = List.of(
            // ----- Particules -----
            // Particules de base ×2 par niveau.
            new DarkUpgrade("dark_density", "Densité", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(20), 2.51e17, Upgrade.NO_LIMIT, null,
                    new DarkEffect.MultiplyBaseParticles(2)),
            // Jusqu'à 100 générateurs ; la fusion rapporte ses atomes une fois par groupe de 10.
            new DarkUpgrade("dark_swarm", "Essaim", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 17), 1, 1, "dark_density",
                    new DarkEffect.UncapGenerators(100)),
            // La fusion automatique attend le nombre de générateurs choisi : c'est ce qui rend l'Essaim utile.
            new DarkUpgrade("dark_threshold", "Seuil de fusion", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 22), 1, 1, "dark_swarm",
                    new DarkEffect.FusionThreshold()),
            // +1 générateur au départ par niveau, jusqu'à repartir avec les dix.
            new DarkUpgrade("dark_seeds", "Germes", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 24), 1.66e5, 9, "dark_threshold",
                    new DarkEffect.StartingGenerators(1)),
            // +10 niveaux de vitesse offerts par niveau, cinq niveaux.
            new DarkUpgrade("dark_priming", "Amorçage", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 30), 2.24e4, 5, "dark_seeds",
                    new DarkEffect.StartingSpeedLevels(10)),
            // Particules ×2 pour chaque matière noire possédée.
            new DarkUpgrade("dark_mass", "Masse sombre", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 38), 1, 1, "dark_priming",
                    new DarkEffect.ParticlesByDarkMatter(2)),
            // Délai minimal des automatismes : 0,05 s au lieu de 0,1 s.
            // Bifurcation « tempo » (0.69, refaite en 0.073) : paliers de vitesse +0,5, ou automatismes +50 % de cadence par matière noire gagnée.
            new DarkUpgrade("dark_flow", "Croissance continue", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 40), 1, 1, "dark_mass",
                    new DarkEffect.StrongerMilestones(0.5), "tempo"),
            new DarkUpgrade("dark_burst", "Cadence sombre", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 40), 1, 1, "dark_mass",
                    new DarkEffect.AutomationsByDarkMatter(0.5), "tempo"),
            new DarkUpgrade("dark_reflexes", "Automatismes vifs", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 130), 1, 1, DarkUpgrade.FORK_PREFIX + "tempo",
                    new DarkEffect.FasterAutomations(0.05)),
            // Particules × (taille en années-lumière)².
            new DarkUpgrade("dark_resonance", "Résonance", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 652), 1, 1, "dark_reflexes",
                    new DarkEffect.ParticlesByLightYears(2)),
            // Bifurcation « élan » (0.70, refaite en 0.073) : paliers de vitesse +1, ou +0,3 pour la voie d'absence.
            new DarkUpgrade("dark_impetus", "Poussée", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 1740), 1, 1, "dark_resonance",
                    new DarkEffect.StrongerMilestones(1), "elan"),
            new DarkUpgrade("dark_shadow_echo", "Écho sombre", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 1740), 1, 1, "dark_resonance",
                    new DarkEffect.StrongerMilestones(0.3), "elan"),
            // Le bout de la branche (0.069) : particules ×100 par niveau, trente niveaux (0.073), à 10⁸⁰⁰⁰ particules puis ×10³⁰⁰.
            new DarkUpgrade("dark_singularity", "Singularité", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 3480), 3.16e130, 30, DarkUpgrade.FORK_PREFIX + "elan",
                    new DarkEffect.MultiplyBaseParticles(100)),
            // Le seuil du Big Bang (version 0.097) : six cases au-delà de la Singularité, de 10⁶⁰⁹⁰ à 10¹⁴⁷⁹⁰
            // particules (0.124), pendant la montée vers les 10¹⁶⁵³⁰ du premier Big Bang : la fin du deuxième acte a encore
            // des étapes, une toutes les vingt à trente minutes. Chacune demande la précédente ; elles donnent
            // particules ×10³⁰⁰ ou +1 matière noire par explosion.
            new DarkUpgrade("dark_protostars", "Proto-étoiles", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 6090), 1, 1, "dark_singularity",
                    new DarkEffect.MultiplyBaseParticles(1e300)),
            new DarkUpgrade("dark_primordial", "Nuage primordial", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 7830), 1, 1, "dark_protostars",
                    new DarkEffect.MultiplyBaseParticles(1e300)),
            new DarkUpgrade("dark_threshold_bang", "Seuil", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 9570), 1, 1, "dark_primordial",
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            new DarkUpgrade("dark_point_zero", "Point zéro", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 11310), 1, 1, "dark_threshold_bang",
                    new DarkEffect.MultiplyBaseParticles(1e300)),
            new DarkUpgrade("dark_fluctuation", "Fluctuation primordiale", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 13050), 1, 1, "dark_point_zero",
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            new DarkUpgrade("dark_eve", "Veille du Big Bang", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 14790), 1, 1, "dark_fluctuation",
                    new DarkEffect.MultiplyBaseParticles(1e300)),

            // ----- Atomes -----
            // +1 atome par fusion et par niveau : 10, 25, 63 atomes, puis il faut avoir levé le plafond de 118.
            new DarkUpgrade("dark_nucleus", "Noyau lourd", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(10), 2.5, Upgrade.NO_LIMIT, null,
                    new DarkEffect.AddAtomsPerFusion(1)),
            // +1 élément par synthèse et par niveau.
            new DarkUpgrade("dark_multisynthesis", "Synthèse multiple", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1.5, 4, "dark_nucleus",
                    new DarkEffect.AddElementsPerSynthesis(1)),
            // Élément unique garanti à la 3ᵉ synthèse au lieu de la 8ᵉ.
            new DarkUpgrade("dark_luck", "Coup de pouce", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(100), 1, 1, "dark_multisynthesis",
                    new DarkEffect.EarlierGuaranteedUnique(3)),
            // Bifurcation « matière » (0.69, renforcée en 0.070) : +25 atomes à chaque fusion, ou deux éléments de plus à chaque synthèse.
            new DarkUpgrade("dark_rich_fusion", "Fusion riche", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(1000), 1, 1, "dark_luck",
                    new DarkEffect.AddAtomsPerFusion(25), "matiere"),
            new DarkUpgrade("dark_fine_synthesis", "Synthèse fine", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(1000), 1, 1, "dark_luck",
                    new DarkEffect.AddElementsPerSynthesis(2), "matiere"),
            new DarkUpgrade("dark_machines", "Mémoire des machines", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(5000), 1, 1, DarkUpgrade.FORK_PREFIX + "matiere",
                    new DarkEffect.KeepAutomationsOnExplosion()),
            // Les éléments uniques survivent à l'explosion.
            new DarkUpgrade("dark_relics", "Tableau entamé", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(2, 5), 1, 1, "dark_machines",
                    new DarkEffect.KeepUniqueElementsOnExplosion()),
            new DarkUpgrade("dark_memory", "Mémoire de la matière", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(5, 6), 1, 1, "dark_relics",
                    new DarkEffect.KeepUpgradesOnExplosion()),
            // Bifurcation « noyau » (0.070) : +40 atomes par fusion, ou deux éléments de plus à chaque synthèse.
            new DarkUpgrade("dark_dense_core", "Noyau dense", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(1, 9), 1, 1, "dark_memory",
                    new DarkEffect.AddAtomsPerFusion(40), "noyau"),
            new DarkUpgrade("dark_double_synthesis", "Synthèse double", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(1, 9), 1, 1, "dark_memory",
                    new DarkEffect.AddElementsPerSynthesis(2), "noyau"),
            // Maximum d'exemplaires 9 → 16 → 25 (et 4 → 9 → 16 pour les lanthanides).
            new DarkUpgrade("dark_isotopes", "Isotopes", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(1, 10), 5, 2, DarkUpgrade.FORK_PREFIX + "noyau",
                    new DarkEffect.IncreaseMaxCopies(1)),
            // Le bout de la branche (0.069) : +5 atomes par fusion et par niveau, cinq niveaux.
            new DarkUpgrade("dark_reactor", "Réacteur", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(1, 11), 10, 8, "dark_isotopes",
                    new DarkEffect.AddAtomsPerFusion(5)),

            // ----- Taille de la matière noire -----
            // Croissance ×2 par niveau, cinq niveaux : à 1 nm, 1 mm, 1 km, un million de km, puis 1e15 m.
            // Limité à cinq niveaux (×32 en tout) : sans limite, chaque niveau amènerait le suivant deux
            // fois plus vite, et la taille s'emballerait.
            new DarkUpgrade("dark_inflation", "Inflation", DarkUpgrade.Branch.SIZE,
                    BigNum.of(1, -9), 1e6, 5, null,
                    new DarkEffect.MultiplyExpansion(2)),
            // Un clic verrouille l'appui. À atteindre : 1 micromètre.
            new DarkUpgrade("dark_lock", "Verrou", DarkUpgrade.Branch.SIZE,
                    BigNum.of(1, -6), 1, 1, "dark_inflation",
                    new DarkEffect.HoldLock()),
            // Chaque fusion vaut 0,2 s d'appui. À atteindre : 1 mètre.
            new DarkUpgrade("dark_wave", "Onde de fusion", DarkUpgrade.Branch.SIZE,
                    BigNum.ONE, 1, 1, "dark_lock",
                    new DarkEffect.FusionPulse(0.2)),
            // Bifurcation « croissance » (0.069) : croissance ×10, ou 2 s d'appui à chaque fusion. À atteindre : 10 mètres.
            new DarkUpgrade("dark_steady", "Croissance soutenue", DarkUpgrade.Branch.SIZE,
                    BigNum.of(10), 1, 1, "dark_wave",
                    new DarkEffect.MultiplyExpansion(10), "croissance"),
            new DarkUpgrade("dark_echo", "Onde amplifiée", DarkUpgrade.Branch.SIZE,
                    BigNum.of(10), 1, 1, "dark_wave",
                    new DarkEffect.FusionPulse(2), "croissance"),
            // Grossit seule : 0,1 %, puis 1 %, puis 10 % de la vitesse d'appui. À atteindre : 1, 100, 10 000 années-lumière.
            new DarkUpgrade("dark_spontaneous", "Expansion spontanée", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR, 100, 3, DarkUpgrade.FORK_PREFIX + "croissance",
                    new DarkEffect.AutoExpansion(0.001, 10)),
            // +1 matière noire par explosion et par niveau, dix niveaux. À atteindre : 1 000 années-lumière, puis ×1 000.
            new DarkUpgrade("dark_condensation", "Condensation", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e3), 1e3, 10, "dark_spontaneous",
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            // Plus de plafond d'atomes. À atteindre : 10¹⁰ années-lumière.
            new DarkUpgrade("dark_overflow", "Débordement", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e10), 1, 1, "dark_condensation",
                    new DarkEffect.UncapAtoms()),
            // Bifurcation « horizon » (0.069) : croissance ×10, ou atomes +50 % par matière noire gagnée. À atteindre : 10²⁵ années-lumière.
            new DarkUpgrade("dark_gravity", "Gravité", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e25), 1, 1, "dark_overflow",
                    new DarkEffect.MultiplyExpansion(10), "horizon"),
            new DarkUpgrade("dark_harvest", "Moisson", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e25), 1, 1, "dark_overflow",
                    new DarkEffect.AtomsByDarkMatter(0.5), "horizon"),
            // Le bout de la branche (0.069) : croissance ×2 par niveau, trois niveaux. À atteindre : 10⁴⁰ années-lumière, puis ×100 000.
            new DarkUpgrade("dark_horizon", "Horizon", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e100), 1e5, 10, DarkUpgrade.FORK_PREFIX + "horizon",
                    new DarkEffect.MultiplyExpansion(2)),
            // Bifurcation « abîme » (0.070), tout au bout : particules × la taille en années-lumière, ou atomes +100 % par
            // matière noire gagnée. À atteindre : 10⁵⁵ années-lumière.
            new DarkUpgrade("dark_abyss", "Abîme", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e125), 1, 1, "dark_horizon",
                    new DarkEffect.ParticlesByLightYears(1), "abime"),
            new DarkUpgrade("dark_deep_harvest", "Moisson profonde", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e125), 1, 1, "dark_horizon",
                    new DarkEffect.AtomsByDarkMatter(1), "abime"),
            // Après l'abîme (version 0.097) : deux cases de taille pour la fin du deuxième acte. +1 matière noire par
            // explosion, puis la croissance ×2. À atteindre : 10¹²⁹, puis 10¹³³ années-lumière.
            new DarkUpgrade("dark_background", "Fond diffus", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(BigNum.of(1, 129)), 1, 1, DarkUpgrade.FORK_PREFIX + "abime",
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            new DarkUpgrade("dark_planck_wall", "Mur de Planck", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(BigNum.of(1, 133)), 1, 1, "dark_background",
                    new DarkEffect.MultiplyExpansion(2)),

            // ----- Liaisons (0.070) -----
            // Entre deux branches : chacune s'ouvre dès que l'une de ses deux voies voisines est prise
            // (la première, à gauche des particules, n'en a qu'une). Elles se paient en matière noire et,
            // comme les voies, se reprennent au Big Bang. Deux rangées : une par niveau de bifurcations.
            new DarkUpgrade("link_flow", "Flux lié", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(5), 1, 1, DarkUpgrade.LINK_PREFIX + "dark_flow",
                    new DarkEffect.MultiplyBaseParticles(100)),
            new DarkUpgrade("link_nuclei", "Pont des noyaux", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(6), 1, 1, DarkUpgrade.LINK_PREFIX + "dark_burst|dark_rich_fusion",
                    new DarkEffect.AddAtomsPerFusion(4)),
            new DarkUpgrade("link_growth", "Pont d'expansion", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(5), 1, 1, DarkUpgrade.LINK_PREFIX + "dark_fine_synthesis|dark_steady",
                    new DarkEffect.MultiplyExpansion(2)),
            new DarkUpgrade("link_impetus", "Poussée liée", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(20), 1, 1, DarkUpgrade.LINK_PREFIX + "dark_impetus",
                    new DarkEffect.StartingSpeedLevels(20)),
            new DarkUpgrade("link_shadow", "Pont sombre", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(25), 1, 1, DarkUpgrade.LINK_PREFIX + "dark_shadow_echo|dark_dense_core",
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            new DarkUpgrade("link_elements", "Pont des éléments", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(25), 1, 1, DarkUpgrade.LINK_PREFIX + "dark_double_synthesis|dark_gravity",
                    new DarkEffect.AddElementsPerSynthesis(1)),
            // Les accords (0.070) : trois voies du même style, une par branche, toutes prises. Celui de présence
            // rapporte de la matière noire à chaque explosion (ce qui raccourcit le deuxième acte de qui est là),
            // le premier d'absence fait tenter les défis pendant l'absence, le second rend plus du temps passé jeu fermé.
            new DarkUpgrade("accord_presence", "Accord de présence", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(15), 1, 1, DarkUpgrade.ALL_PREFIX + "dark_flow|dark_rich_fusion|dark_steady",
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            new DarkUpgrade("accord_absence", "Accord d'absence", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(15), 1, 1, DarkUpgrade.ALL_PREFIX + "dark_burst|dark_fine_synthesis|dark_echo",
                    new DarkEffect.ChallengesWhileAway()),
            new DarkUpgrade("accord_presence_2", "Grand accord de présence", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(40), 1, 1, DarkUpgrade.ALL_PREFIX + "dark_impetus|dark_dense_core|dark_gravity",
                    new DarkEffect.AddDarkMatterPerExplosion(2)),
            new DarkUpgrade("accord_absence_2", "Grand accord d'absence", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(40), 1, 1, DarkUpgrade.ALL_PREFIX + "dark_shadow_echo|dark_double_synthesis|dark_harvest",
                    new DarkEffect.OfflineBonus(0.10)),

            // ----- Améliorations payées en matière noire (hors de l'arbre) -----
            // La matière noire est dépensée, mais reste comptée comme gagnée : leur effet grandit
            // avec tout ce que le joueur a gagné depuis le début, pas avec ce qui lui reste.

            // Les deux premières n'ont pas de niveau maximal : leur prix double à chaque niveau, leur
            // effet ne fait que s'additionner. La matière noire garde ainsi toujours un usage, sans que
            // rien s'emballe : dix niveaux coûtent 1 023 matières noires.

            // Atomes par fusion +20 % par matière noire gagnée et par niveau. Prix : 1, 2, 4, 8, 16…
            new DarkUpgrade("dark_shop_atoms", "Noyaux sombres", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.ONE, 2, Upgrade.NO_LIMIT, null,
                    new DarkEffect.AtomsByDarkMatter(0.2)),
            // Automatismes ordinaires +10 % de cadence par matière noire gagnée et par niveau. Prix : 1, 2, 4…
            // Leur délai ne descend de toute façon jamais sous le minimum du jeu.
            new DarkUpgrade("dark_shop_cadence", "Rouages sombres", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.ONE, 2, Upgrade.NO_LIMIT, null,
                    new DarkEffect.AutomationsByDarkMatter(0.1)),
            // Prix de la synthèse divisé par 1 + 10 % par matière noire gagnée et par niveau. Prix : 2, 4, 8.
            new DarkUpgrade("dark_shop_synthesis", "Synthèse sombre", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(2), 2, 3, null,
                    new DarkEffect.SynthesisByDarkMatter(0.1)),
            // 5 atomes comptés comme créés au départ de chaque partie, par matière noire gagnée : à
            // 6 matières noires, l'automatisation et le tableau périodique sont ouverts d'entrée.
            new DarkUpgrade("dark_shop_start", "Départ lancé", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(2), 1, 1, null,
                    new DarkEffect.HeadStartByDarkMatter(5)),
            // Ouvre la synthèse ciblée. Elle coûte cher en synthèses : elle n'a d'intérêt que lorsque
            // les atomes ne manquent plus, donc après la première explosion.
            new DarkUpgrade("dark_shop_target", "Synthèse ciblée", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(2), 1, 1, null,
                    new DarkEffect.TargetedSynthesis()),
            // Ouvre l'achat par dix et « max » des améliorations en atomes : un confort, pour une matière noire.
            new DarkUpgrade("dark_shop_bulk", "Achat groupé", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.ONE, 1, 1, null,
                    new DarkEffect.BulkAtomUpgrades()));

    private DarkUpgrades() {}
}
