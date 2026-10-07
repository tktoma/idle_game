package idle.core;

import java.util.List;

/**
 * Les améliorations de matière noire : l'arbre, puis celles qui se paient en matière noire.
 *
 * <p>L'arbre a trois branches, dont chaque case est débloquée par la case du dessus.
 *
 * <ul>
 *   <li><b>particules</b> : se paie avec les particules de la partie en cours ;</li>
 *   <li><b>atomes</b> : se paie avec les atomes disponibles. Tant que le plafond d'atomes n'est
 *       pas levé, rien ne peut coûter plus de 118 atomes ;</li>
 *   <li><b>taille</b> : se débloque en faisant grossir la matière noire, sans rien dépenser.</li>
 * </ul>
 *
 * <p>Chaque case demande en plus d'avoir <b>gagné</b> un certain nombre de matières noires
 * ({@link Game#darkMatterEarned()}), sans les dépenser : c'est ce qui étale l'arbre sur
 * plusieurs explosions au lieu de le laisser s'acheter d'un coup. Ces seuils vont de 1 à 60.
 * Jusqu'à 16, chaque explosion ne rapporte qu'une matière noire, et chaque matière noire ouvre
 * quelque chose : une case, un automatisme ({@link DarkAutomations}) ou un défi
 * ({@link Challenges}). À 16, la Condensation porte l'explosion à onze matières noires, et les
 * dernières cases (20, 24, 32, 40, 60) tombent en quelques explosions.
 *
 * <p>Hors de l'arbre, six améliorations se paient en matière noire
 * ({@link DarkUpgrade.Branch#DARK_MATTER}). La matière noire dépensée reste comptée comme gagnée,
 * et leur effet grandit avec toute la matière noire gagnée : chaque explosion les renforce.
 *
 * <p>Réglage vérifié par simulation, avec le tableau qui s'alourdit à chaque explosion
 * ({@link Game#TABLE_WEIGHT_GROWTH}, dix fois au plus) : après la première explosion (environ
 * 24 h), les parties durent 7 à 9 h, puis 2 à 3 h, puis entre vingt minutes et deux heures
 * pendant six ou sept explosions. Quand le tableau cesse de s'alourdir, deux parties de deux à
 * trois heures et demie, puis des parties de plus en plus courtes jusqu'à la Condensation ;
 * quelques minutes pour finir. L'arbre est complet après 18 à 23 h de plus si les défis sont
 * tentés dès leur ouverture (ils raccourcissent le chemin), 29 à 31 h s'ils sont gardés pour la
 * fin : le deuxième acte dure à peu près autant que le premier. Les deux premières cases
 * (Densité à 1 000 particules, Noyau lourd à 10 atomes) sont volontairement bon marché : avec
 * la première amélioration payée en matière noire, ce sont elles qui rendent la deuxième partie
 * trois fois plus courte que la première.
 *
 * <p>Ce qui allonge ou raccourcit le deuxième acte : le seuil de la Condensation (chaque
 * matière noire de plus avant elle ajoute une partie d'une heure environ) et
 * {@link Game#TABLE_WEIGHT_MAX_LEVEL} (un niveau de plus double à peu près les parties du
 * milieu : à onze, l'une d'elles dépasse huit heures).
 */
public final class DarkUpgrades {

    public static final List<DarkUpgrade> DEFAULT = List.of(
            // ----- Particules -----
            // Particules de base ×2 par niveau.
            new DarkUpgrade("dark_density", "Densité", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 3), 1e40, Upgrade.NO_LIMIT, null, 1,
                    new DarkEffect.MultiplyBaseParticles(2)),
            // Jusqu'à 100 générateurs ; la fusion rapporte ses atomes une fois par groupe de 10.
            new DarkUpgrade("dark_swarm", "Essaim", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 20), 1, 1, "dark_density", 2,
                    new DarkEffect.UncapGenerators(100)),
            // La fusion automatique attend le nombre de générateurs choisi : c'est ce qui rend l'Essaim utile.
            new DarkUpgrade("dark_threshold", "Seuil de fusion", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 25), 1, 1, "dark_swarm", 2,
                    new DarkEffect.FusionThreshold()),
            // +1 générateur au départ par niveau, jusqu'à repartir avec les dix.
            new DarkUpgrade("dark_seeds", "Germes", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 30), 1e12, 9, "dark_threshold", 4,
                    new DarkEffect.StartingGenerators(1)),
            // +10 niveaux de vitesse offerts par niveau, cinq niveaux.
            new DarkUpgrade("dark_priming", "Amorçage", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 40), 1e10, 5, "dark_seeds", 6,
                    new DarkEffect.StartingSpeedLevels(10)),
            // Particules ×2 pour chaque matière noire possédée.
            new DarkUpgrade("dark_mass", "Masse sombre", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 60), 1, 1, "dark_priming", 11,
                    new DarkEffect.ParticlesByDarkMatter(2)),
            // Délai minimal des automatismes : 0,05 s au lieu de 0,1 s.
            new DarkUpgrade("dark_reflexes", "Automatismes vifs", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 80), 1, 1, "dark_mass", 13,
                    new DarkEffect.FasterAutomations(0.05)),
            // Particules × (taille en années-lumière)².
            new DarkUpgrade("dark_resonance", "Résonance", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 100), 1, 1, "dark_reflexes", 40,
                    new DarkEffect.ParticlesByLightYears(2)),

            // ----- Atomes -----
            // +1 atome par fusion et par niveau : 10, 25, 63 atomes, puis il faut avoir levé le plafond de 118.
            new DarkUpgrade("dark_nucleus", "Noyau lourd", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(10), 2.5, Upgrade.NO_LIMIT, null, 1,
                    new DarkEffect.AddAtomsPerFusion(1)),
            // +1 élément par synthèse et par niveau.
            new DarkUpgrade("dark_multisynthesis", "Synthèse multiple", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1.5, 4, "dark_nucleus", 2,
                    new DarkEffect.AddElementsPerSynthesis(1)),
            // Élément unique garanti à la 3ᵉ synthèse au lieu de la 8ᵉ.
            new DarkUpgrade("dark_luck", "Coup de pouce", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(100), 1, 1, "dark_multisynthesis", 4,
                    new DarkEffect.EarlierGuaranteedUnique(3)),
            new DarkUpgrade("dark_machines", "Mémoire des machines", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1, 1, "dark_luck", 10,
                    new DarkEffect.KeepAutomationsOnExplosion()),
            // Les éléments uniques survivent à l'explosion.
            new DarkUpgrade("dark_relics", "Tableau entamé", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1, 1, "dark_machines", 20,
                    new DarkEffect.KeepUniqueElementsOnExplosion()),
            new DarkUpgrade("dark_memory", "Mémoire de la matière", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1, 1, "dark_relics", 32,
                    new DarkEffect.KeepUpgradesOnExplosion()),
            // Maximum d'exemplaires 9 → 16 → 25 (et 4 → 9 → 16 pour les lanthanides).
            new DarkUpgrade("dark_isotopes", "Isotopes", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(2000), 5, 2, "dark_memory", 60,
                    new DarkEffect.IncreaseMaxCopies(1)),

            // ----- Taille de la matière noire -----
            // Croissance ×2 par niveau, cinq niveaux : à 1 nm, 1 mm, 1 km, un million de km, puis 1e15 m.
            // Limité à cinq niveaux (×32 en tout) : sans limite, chaque niveau amènerait le suivant deux
            // fois plus vite, et la taille s'emballerait.
            new DarkUpgrade("dark_inflation", "Inflation", DarkUpgrade.Branch.SIZE,
                    BigNum.of(1, -9), 1e6, 5, null, 1,
                    new DarkEffect.MultiplyExpansion(2)),
            // Un clic verrouille l'appui. À atteindre : 1 micromètre.
            new DarkUpgrade("dark_lock", "Verrou", DarkUpgrade.Branch.SIZE,
                    BigNum.of(1, -6), 1, 1, "dark_inflation", 2,
                    new DarkEffect.HoldLock()),
            // Chaque fusion vaut 0,2 s d'appui. À atteindre : 1 mètre.
            new DarkUpgrade("dark_wave", "Onde de fusion", DarkUpgrade.Branch.SIZE,
                    BigNum.ONE, 1, 1, "dark_lock", 4,
                    new DarkEffect.FusionPulse(0.2)),
            // Grossit seule : 0,1 %, puis 1 %, puis 10 % de la vitesse d'appui. À atteindre : 1, 100, 10 000 années-lumière.
            new DarkUpgrade("dark_spontaneous", "Expansion spontanée", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR, 100, 3, "dark_wave", 7,
                    new DarkEffect.AutoExpansion(0.001, 10)),
            // +1 matière noire par explosion et par niveau, dix niveaux. À atteindre : 1 000 années-lumière, puis ×1 000.
            new DarkUpgrade("dark_condensation", "Condensation", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e3), 1e3, 10, "dark_spontaneous", 16,
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            // Plus de plafond d'atomes. À atteindre : 100 000 années-lumière, la taille de la Voie lactée.
            new DarkUpgrade("dark_overflow", "Débordement", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e5), 1, 1, "dark_condensation", 24,
                    new DarkEffect.UncapAtoms()),

            // ----- Améliorations payées en matière noire (hors de l'arbre) -----
            // La matière noire est dépensée, mais reste comptée comme gagnée : leur effet grandit
            // avec tout ce que le joueur a gagné depuis le début, pas avec ce qui lui reste.

            // Les deux premières n'ont pas de niveau maximal : leur prix double à chaque niveau, leur
            // effet ne fait que s'additionner. La matière noire garde ainsi toujours un usage, sans que
            // rien s'emballe : dix niveaux coûtent 1 023 matières noires.

            // Atomes par fusion +20 % par matière noire gagnée et par niveau. Prix : 1, 2, 4, 8, 16…
            new DarkUpgrade("dark_shop_atoms", "Noyaux sombres", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.ONE, 2, Upgrade.NO_LIMIT, null, 0,
                    new DarkEffect.AtomsByDarkMatter(0.2)),
            // Automatismes ordinaires +10 % de cadence par matière noire gagnée et par niveau. Prix : 1, 2, 4…
            // Leur délai ne descend de toute façon jamais sous le minimum du jeu.
            new DarkUpgrade("dark_shop_cadence", "Rouages sombres", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.ONE, 2, Upgrade.NO_LIMIT, null, 0,
                    new DarkEffect.AutomationsByDarkMatter(0.1)),
            // Prix de la synthèse divisé par 1 + 10 % par matière noire gagnée et par niveau. Prix : 2, 4, 8.
            new DarkUpgrade("dark_shop_synthesis", "Synthèse sombre", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(2), 2, 3, null, 0,
                    new DarkEffect.SynthesisByDarkMatter(0.1)),
            // 5 atomes comptés comme créés au départ de chaque partie, par matière noire gagnée : à
            // 6 matières noires, l'automatisation et le tableau périodique sont ouverts d'entrée.
            new DarkUpgrade("dark_shop_start", "Départ lancé", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(2), 1, 1, null, 0,
                    new DarkEffect.HeadStartByDarkMatter(5)),
            // Ouvre la synthèse ciblée. Elle coûte cher en synthèses : elle n'a d'intérêt que lorsque
            // les atomes ne manquent plus, donc après la première explosion.
            new DarkUpgrade("dark_shop_target", "Synthèse ciblée", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.of(2), 1, 1, null, 0,
                    new DarkEffect.TargetedSynthesis()),
            // Ouvre l'achat par dix et « max » des améliorations en atomes : un confort, pour une matière noire.
            new DarkUpgrade("dark_shop_bulk", "Achat groupé", DarkUpgrade.Branch.DARK_MATTER,
                    BigNum.ONE, 1, 1, null, 0,
                    new DarkEffect.BulkAtomUpgrades()));

    private DarkUpgrades() {}
}
