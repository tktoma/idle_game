package idle.core;

import java.util.List;

/**
 * L'arbre d'améliorations de la matière noire : trois branches, dont chaque case est débloquée
 * par la case du dessus.
 *
 * <ul>
 *   <li><b>particules</b> : se paie avec les particules de la partie en cours ;</li>
 *   <li><b>atomes</b> : se paie avec les atomes disponibles. Tant que le plafond d'atomes n'est
 *       pas levé, rien ne peut coûter plus de 118 atomes ;</li>
 *   <li><b>taille</b> : se débloque en faisant grossir la matière noire, sans rien dépenser.</li>
 * </ul>
 *
 * <p>Chaque case demande en plus de <b>posséder</b> un certain nombre de matières noires (sans
 * les dépenser) : c'est ce qui étale l'arbre sur plusieurs explosions au lieu de le laisser
 * s'acheter d'un coup.
 *
 * <p>Réglage vérifié par simulation (joueur qui achète tout dès que possible et garde l'appui
 * 5 % du temps, 15 % une fois le Verrou acquis) : les parties durent environ 24 h, 9 h, 4 h,
 * 3 h 30, 1 h 30, 1 h, 40 min, puis quelques minutes ; l'arbre est complet vers la neuvième
 * explosion, après 44 à 50 h de jeu. Les deux premières cases (Densité à 1 000 particules, Noyau
 * lourd à 10 atomes) sont volontairement bon marché : ce sont elles qui rendent la deuxième
 * partie presque trois fois plus courte que la première.
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
                    BigNum.of(1, 30), 1e12, 9, "dark_threshold", 3,
                    new DarkEffect.StartingGenerators(1)),
            // +10 niveaux de vitesse offerts par niveau, cinq niveaux.
            new DarkUpgrade("dark_priming", "Amorçage", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 40), 1e10, 5, "dark_seeds", 4,
                    new DarkEffect.StartingSpeedLevels(10)),
            // Particules ×2 pour chaque matière noire possédée.
            new DarkUpgrade("dark_mass", "Masse sombre", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 60), 1, 1, "dark_priming", 6,
                    new DarkEffect.ParticlesByDarkMatter(2)),
            // Délai minimal des automatismes : 0,05 s au lieu de 0,1 s.
            new DarkUpgrade("dark_reflexes", "Automatismes vifs", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 80), 1, 1, "dark_mass", 7,
                    new DarkEffect.FasterAutomations(0.05)),
            // Particules × (taille en années-lumière)².
            new DarkUpgrade("dark_resonance", "Résonance", DarkUpgrade.Branch.PARTICLES,
                    BigNum.of(1, 100), 1, 1, "dark_reflexes", 8,
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
                    BigNum.of(100), 1, 1, "dark_multisynthesis", 3,
                    new DarkEffect.EarlierGuaranteedUnique(3)),
            new DarkUpgrade("dark_machines", "Mémoire des machines", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1, 1, "dark_luck", 5,
                    new DarkEffect.KeepAutomationsOnExplosion()),
            // Les éléments uniques survivent à l'explosion.
            new DarkUpgrade("dark_relics", "Tableau entamé", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1, 1, "dark_machines", 7,
                    new DarkEffect.KeepUniqueElementsOnExplosion()),
            new DarkUpgrade("dark_memory", "Mémoire de la matière", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(118), 1, 1, "dark_relics", 8,
                    new DarkEffect.KeepUpgradesOnExplosion()),
            // Explosion possible dès les 118 éléments découverts. Il faut avoir levé le plafond d'atomes.
            new DarkUpgrade("dark_chain", "Réaction en chaîne", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(500), 1, 1, "dark_memory", 9,
                    new DarkEffect.ExplodeWhenDiscovered()),
            // Maximum d'exemplaires 9 → 16 → 25 (et 4 → 9 → 16 pour les lanthanides).
            new DarkUpgrade("dark_isotopes", "Isotopes", DarkUpgrade.Branch.ATOMS,
                    BigNum.of(2000), 5, 2, "dark_chain", 10,
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
                    BigNum.ONE, 1, 1, "dark_lock", 3,
                    new DarkEffect.FusionPulse(0.2)),
            // Grossit seule : 0,1 %, puis 1 %, puis 10 % de la vitesse d'appui. À atteindre : 1, 100, 10 000 années-lumière.
            new DarkUpgrade("dark_spontaneous", "Expansion spontanée", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR, 100, 3, "dark_wave", 4,
                    new DarkEffect.AutoExpansion(0.001, 10)),
            // +1 matière noire par explosion et par niveau, dix niveaux. À atteindre : 1 000 années-lumière, puis ×1 000.
            new DarkUpgrade("dark_condensation", "Condensation", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e3), 1e3, 10, "dark_spontaneous", 5,
                    new DarkEffect.AddDarkMatterPerExplosion(1)),
            // Plus de plafond d'atomes. À atteindre : 100 000 années-lumière, la taille de la Voie lactée.
            new DarkUpgrade("dark_overflow", "Débordement", DarkUpgrade.Branch.SIZE,
                    SizeScale.LIGHT_YEAR.multiply(1e5), 1, 1, "dark_condensation", 6,
                    new DarkEffect.UncapAtoms()));

    private DarkUpgrades() {}
}
