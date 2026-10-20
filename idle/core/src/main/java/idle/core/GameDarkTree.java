package idle.core;

import idle.core.achievements.Achievement;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * L'arbre de matière noire : ses achats et ses effets, calculés une fois par version des niveaux.
 */
public abstract class GameDarkTree extends GameSystems {

    GameDarkTree(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    public abstract boolean inChallenge();
    abstract boolean rewarded(Challenge.Reward reward);
    public abstract boolean isDarkMatterUnlocked();
    abstract double darkMatterUnits();
    public abstract BigNum darkMatterLightYears();
    public abstract int elementUncap(int atomicNumber);
    abstract int effectLevel(DarkUpgrade dark);
    abstract boolean hasDark(Class<? extends DarkEffect> type);
    abstract List<DarkUpgrade> darkScaled();
    public abstract int maxGeneratorCount();
    public abstract int generatorsPerAtom();
    abstract DarkUpgrade darkUpgrade(String darkUpgradeId);


    // ------------------------------------------------------------------
    // L'arbre de matière noire
    // ------------------------------------------------------------------

    /** Toutes les améliorations de matière noire, dans l'ordre du catalogue : l'arbre, puis celles payées en matière noire. */
    public Collection<DarkUpgrade> darkUpgrades() {
        return darkUpgrades.values();
    }

    /** Les améliorations de matière noire d'une branche, dans l'ordre du catalogue, sans les liaisons. */
    public List<DarkUpgrade> darkUpgrades(DarkUpgrade.Branch branch) {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.branch() == branch && !dark.isLink()) result.add(dark);
        }
        return result;
    }

    /** Les liaisons entre branches (version 0.70), dans l'ordre du catalogue. */
    public List<DarkUpgrade> darkLinks() {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (dark.isLink()) result.add(dark);
        return result;
    }

    public int darkLevelOf(String darkUpgradeId) {
        return state.darkLevelOf(darkUpgrade(darkUpgradeId).id());
    }

    public boolean isDarkMaxed(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return state.darkLevelOf(dark.id()) >= dark.maxLevel();
    }

    /**
     * Vrai si la case est accessible : la matière noire existe et la case du dessus est acquise
     * (s'il y en a une). Depuis la version 0.70, aucune case n'attend plus un nombre de matières noires
     * gagnées : seuls les prix rythment l'arbre.
     */
    public boolean isDarkAvailable(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkMatterUnlocked() && hasDarkParent(dark) && darkForkRival(darkUpgradeId) == null;
    }

    /** Vrai si la case du dessus est acquise : une case ordinaire, ou l'une des voies d'une bifurcation. */
    private boolean hasDarkParent(DarkUpgrade dark) {
        if (dark.requires() == null) return true;
        String fork = dark.requiredFork();
        if (dark.isAccord()) {
            for (String linked : dark.linkedIds()) if (state.darkLevelOf(linked) == 0) return false;
            return true;
        }
        if (dark.isLink()) {
            for (String linked : dark.linkedIds()) if (state.darkLevelOf(linked) > 0) return true;
            return false;
        }
        if (fork == null) return state.darkLevelOf(dark.requires()) > 0;
        for (DarkUpgrade other : darkUpgrades.values()) {
            if (fork.equals(other.fork()) && state.darkLevelOf(other.id()) > 0) return true;
        }
        return false;
    }

    /**
     * L'autre voie de la bifurcation, si le joueur l'a prise : elle ferme celle-ci jusqu'au prochain
     * Big Bang (version 0.69). {@code null} si la case n'est pas une voie, ou si l'autre n'est pas prise.
     */
    public DarkUpgrade darkForkRival(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        if (!dark.isFork() || hasSuperposition()) return null;
        for (DarkUpgrade other : darkUpgrades.values()) {
            if (other != dark && dark.fork().equals(other.fork()) && state.darkLevelOf(other.id()) > 0) return other;
        }
        return null;
    }

    /**
     * Vrai une fois la « Superposition » du collisionneur prise (version 0.70) : les deux voies de chaque
     * bifurcation se prennent, et elles restent, avec les liaisons, d'un Big Bang à l'autre.
     */
    public boolean hasSuperposition() {
        return colliderUpgradeLevel(ColliderUpgrade.SUPERPOSITION) > 0;
    }

    // Les effets de l'arbre qui ne dépendent que de ses niveaux : calculés une fois par version des
    // niveaux. Sans ce cache, chaque appel parcourait tout l'arbre, des milliers de fois par seconde
    // de jeu une fois les automatismes rapides : c'était le gros du temps du rattrapage d'absence.
    private final Map<Object, Object> darkMemo = new HashMap<>();
    private long darkMemoVersion = Long.MIN_VALUE;

    @SuppressWarnings("unchecked")
    <T> T darkMemo(Object key, java.util.function.Supplier<T> compute) {
        // Depuis la version 0.70, les défis et leurs récompenses changent ce que valent les cases : ils comptent aussi.
        long stamp = state.darkVersion() * 1_000_003L + state.challengesVersion() * 31L
                + (state.activeChallenge() == null ? 0 : state.activeChallenge().hashCode());
        if (darkMemoVersion != stamp) {
            darkMemo.clear();
            darkMemoVersion = stamp;
        }
        Object value = darkMemo.get(key);
        if (value == null) {
            value = compute.get();
            darkMemo.put(key, value);
        }
        return (T) value;
    }

    /** Atomes de base ajoutés à chaque fusion par l'arbre. */
    public double darkAtomsPerFusion() {
        return darkMemo("atomsPerFusion", () -> {
            double added = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.AddAtomsPerFusion add) {
                    added += add.perLevel() * effectLevel(dark);
                }
            }
            return added;
        });
    }

    /** Nombre d'éléments tirés à chaque synthèse, avant le tirage double : un, plus ceux de l'arbre. */
    public int elementsPerSynthesis() {
        int count = 1 + this.<Integer>darkMemo("elementsPerSynthesis", () -> {
            int added = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.AddElementsPerSynthesis add) {
                    added += add.perLevel() * effectLevel(dark);
                }
            }
            return added;
        });
        if (rewarded(Challenge.Reward.EXTRA_ELEMENT)) count += REWARD_EXTRA_ELEMENTS;
        return count;
    }

    /**
     * Ce que l'arbre multiplie dans les particules de chaque création : le multiplicateur de base,
     * et la taille de la matière noire en années-lumière (à partir d'une année-lumière).
     */
    public BigNum darkParticlesMultiplier() {
        BigNum result = this.<BigNum>darkMemo("baseParticles", () -> {
            BigNum base = BigNum.ONE;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (level > 0 && dark.effect() instanceof DarkEffect.MultiplyBaseParticles multiply) {
                    base = base.multiply(BigNum.of(multiply.perLevel()).pow(level));
                }
            }
            return base;
        });
        for (DarkUpgrade dark : darkScaled()) {
            int level = effectLevel(dark);
            if (level == 0) continue;
            if (dark.effect() instanceof DarkEffect.ParticlesByDarkMatter byDarkMatter) {
                result = result.multiply(BigNum.of(byDarkMatter.perUnit()).pow(darkMatterUnits() * level));
            } else if (dark.effect() instanceof DarkEffect.ParticlesByLightYears byLightYears) {
                BigNum lightYears = darkMatterLightYears();
                if (lightYears.gt(BigNum.ONE)) result = result.multiply(lightYears.pow(byLightYears.exponent() * level));
            }
        }
        return result;
    }

    /** Ce que l'arbre multiplie dans la vitesse de croissance de la matière noire. */
    public BigNum darkExpansionMultiplier() {
        return darkMemo("expansion", () -> {
            BigNum result = BigNum.ONE;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.MultiplyExpansion multiply) {
                    result = result.multiply(BigNum.of(multiply.perLevel()).pow(effectLevel(dark)));
                }
            }
            return result;
        });
    }

    /** Part de la vitesse d'appui à laquelle la matière noire grossit seule (0 sans l'amélioration). */
    public double darkAutoExpansionShare() {
        return darkMemo("autoExpansion", () -> {
            double share = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (dark.effect() instanceof DarkEffect.AutoExpansion auto && level > 0) {
                    share += auto.share() * Math.pow(auto.growth(), level - 1);
                }
            }
            return share;
        });
    }

    /** Secondes d'appui que vaut chaque fusion pour la matière noire (0 sans l'amélioration). */
    public double darkFusionPulse() {
        return darkMemo("fusionPulse", () -> {
            double seconds = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.FusionPulse pulse) {
                    seconds += pulse.seconds() * effectLevel(dark);
                }
            }
            return seconds;
        });
    }

    /** Niveaux de vitesse offerts par l'arbre : ils s'ajoutent à ceux achetés et ne se perdent jamais. */
    public int startingSpeedLevels() {
        if (inChallenge()) return 0;
        return this.<Integer>darkMemo("startingSpeedLevels", () -> {
            int levels = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.StartingSpeedLevels start) {
                    levels += start.perLevel() * effectLevel(dark);
                }
            }
            return levels;
        });
    }

    /** Rang de la synthèse qui donne un élément unique à coup sûr, tant qu'aucun n'est sorti. */
    public int guaranteedUniqueSynthesis() {
        return darkMemo("guaranteedUnique", () -> {
            int rank = GUARANTEED_UNIQUE_SYNTHESIS;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.EarlierGuaranteedUnique earlier && effectLevel(dark) > 0) {
                    rank = Math.min(rank, earlier.rank());
                }
            }
            return rank;
        });
    }

    /**
     * Nombre maximal d'exemplaires d'un élément de cette famille, arbre de matière noire compris.
     * Les familles uniques restent à un exemplaire ; pour les autres, c'est la force maximale
     * (la racine du maximum) qui gagne un cran par niveau : 9 → 16 → 25.
     */
    public int maxCopiesOf(ElementCategory category) {
        if (category.unique()) return 1;
        int steps = this.<Integer>darkMemo("copySteps", () -> {
            int sum = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.IncreaseMaxCopies more) {
                    sum += more.perLevel() * effectLevel(dark);
                }
            }
            return sum;
        });
        if (steps == 0) return category.maxCopies();
        int strength = (int) Math.round(Math.sqrt(category.maxCopies())) + steps;
        return strength * strength;
    }

    /** Nombre total d'exemplaires qu'on peut posséder en ce moment : le tableau est complet quand on les a tous. */
    public int maxTotalCopies() {
        int copies = 0;
        for (Element element : PeriodicTable.ELEMENTS) copies += maxCopiesOf(element);
        return copies;
    }

    /**
     * Nombre maximal d'exemplaires de cet élément : celui de sa famille
     * ({@link #maxCopiesOf(ElementCategory)}), plus un par molécule créée qui relève son plafond
     * ({@link Molecule.Uncap}). Un élément unique reste unique.
     */
    public int maxCopiesOf(Element element) {
        if (element.category().unique()) return 1;
        return maxCopiesOf(element.category()) + elementUncap(element.number());
    }

    /** Vrai quand l'arbre a débloqué le verrou de l'appui sur la matière noire. */
    public boolean isHoldLockUnlocked() {
        return hasDark(DarkEffect.HoldLock.class);
    }

    /** Vrai quand l'arbre a débloqué le réglage du seuil de fusion. */
    public boolean isFusionThresholdUnlocked() {
        return hasDark(DarkEffect.FusionThreshold.class);
    }

    /**
     * Nombre de générateurs que la fusion automatique attend avant de fusionner : celui qu'il
     * faut pour fusionner, ou le réglage du joueur une fois le seuil débloqué, ramené entre ce
     * minimum et la limite de générateurs. La fusion à la main, elle, reste possible dès le minimum.
     */
    public int fusionThreshold() {
        int minimum = generatorsPerAtom();
        if (!isFusionThresholdUnlocked() || state.fusionThreshold() == 0) return minimum;
        return Math.max(minimum, Math.min(state.fusionThreshold(), maxGeneratorCount()));
    }

    /**
     * Règle le seuil de la fusion automatique.
     *
     * @return {@code true} si le réglage a été pris en compte (il faut l'avoir débloqué)
     */
    public boolean setFusionThreshold(int generators) {
        if (!isFusionThresholdUnlocked()) return false;
        state.setFusionThreshold(Math.max(generatorsPerAtom(), Math.min(generators, maxGeneratorCount())));
        return true;
    }

    /** Vrai quand l'arbre a levé le plafond d'atomes. */
    public boolean isAtomCapLifted() {
        return hasDark(DarkEffect.UncapAtoms.class);
    }
}
