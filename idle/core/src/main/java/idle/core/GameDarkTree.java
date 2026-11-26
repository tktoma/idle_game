package idle.core;

import idle.core.state.GameState;
import java.util.function.*;

import idle.core.achievements.Achievement;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** L'arbre de matière noire : ses achats et ses effets, calculés une fois par version des niveaux. */
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
    public abstract BigNum darkMatterReachLightYears();
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
    public Collection<DarkUpgrade> darkUpgrades() { return darkUpgrades.values(); }

    /** Les améliorations de matière noire d'une branche, dans l'ordre du catalogue, sans les liaisons. */
    public List<DarkUpgrade> darkUpgrades(DarkUpgrade.Branch branch) {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.branch() == branch && !dark.isLink()) result.add(dark);
        }
        return result;
    }

    /** Les liaisons entre branches, dans l'ordre du catalogue. */
    public List<DarkUpgrade> darkLinks() {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (dark.isLink()) result.add(dark);
        return result;
    }

    public int darkLevelOf(String darkUpgradeId) { return state.darkLevelOf(darkUpgrade(darkUpgradeId).id()); }

    public boolean isDarkMaxed(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return state.darkLevelOf(dark.id()) >= dark.maxLevel();
    }

    /**
     * Vrai si la case est accessible : la matière noire existe et la case du dessus est acquise (s'il y en a une).
     * Depuis la version 0.070, aucune case n'attend plus un nombre de matières noires gagnées : seuls les prix rythment
     * l'arbre. Depuis la version 0.209, ce qui se paie en matière noire attend Noyaux sombres ({@link #darkKeyFirst}).
     */
    public boolean isDarkAvailable(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkMatterUnlocked() && hasDarkParent(dark) && darkForkRival(darkUpgradeId) == null && darkKeyFirst(dark);
    }

    /**
     * La première matière noire dépensée va à Noyaux sombres ({@link #KEY_DARK_UPGRADE}), la case qui
     * compte le plus (sans elle, l'univers 2 du Speedrunner se bloque, mesure 0.208). Tout ce qui se paie en matière
     * noire, accords compris, attend qu'elle ait un niveau ; elle-même n'attend rien. Les niveaux déjà pris dans une
     * ancienne sauvegarde restent : seul l'achat suivant attend.
     */
    boolean darkKeyFirst(DarkUpgrade dark) {
        return dark.branch() != DarkUpgrade.Branch.DARK_MATTER || dark.id().equals(KEY_DARK_UPGRADE)
                || state.darkLevelOf(KEY_DARK_UPGRADE) > 0;
    }

    /** Vrai si cette case ne s'achète pas encore parce que Noyaux sombres n'a pas de niveau : pour les textes. */
    public boolean darkWaitsForKey(String darkUpgradeId) { return !darkKeyFirst(darkUpgrade(darkUpgradeId)); }

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
     * Big Bang. {@code null} si la case n'est pas une voie, ou si l'autre n'est pas prise.
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
     * Vrai une fois la « Superposition » du collisionneur prise : les deux voies de chaque
     * bifurcation se prennent, et elles restent, avec les liaisons, d'un Big Bang à l'autre.
     */
    public boolean hasSuperposition() { return colliderUpgradeLevel(ColliderUpgrade.SUPERPOSITION) > 0; }

    // Les effets de l'arbre qui ne dépendent que de ses niveaux : calculés une fois par version des
    // niveaux. Sans ce cache, chaque appel parcourait tout l'arbre, des milliers de fois par seconde
    // de jeu une fois les automatismes rapides : c'était le gros du temps du rattrapage d'absence.
    // Version 0.298 : par identité. Les clés sont des chaînes écrites dans le code (une seule instance chacune) et des
    // classes d'effets : la même clé est toujours le même objet, et la recherche ne calcule plus de hachage de chaîne.
    private final Map<Object, Object> darkMemo = new IdentityHashMap<>();
    private int darkMemoVersion = Integer.MIN_VALUE, darkMemoChallenges;
    private String darkMemoActive;

    @SuppressWarnings("unchecked")
    <T> T darkMemo(Object key, Supplier<T> compute) {
        // Depuis la version 0.070, les défis et leurs récompenses changent ce que valent les cases : ils comptent aussi.
        // Version 0.298 : les trois versions se comparent une à une (avant, un nombre mêlé, avec le hachage du défi en
        // cours, à chaque appel) ; un défi changé se voit à sa référence : au pire, le cache se vide une fois de trop.
        int dark = state.darkVersion(), challenges = state.challengesVersion();
        String active = state.activeChallenge();
        if (darkMemoVersion != dark || darkMemoChallenges != challenges || darkMemoActive != active) {
            darkMemo.clear();
            darkMemoVersion = dark;
            darkMemoChallenges = challenges;
            darkMemoActive = active;
        }
        Object value = darkMemo.get(key);
        if (value == null) {
            value = compute.get();
            darkMemo.put(key, value);
        }
        return (T) value;
    }

    /** La somme, sur les cases de l'arbre dont l'effet est de cette sorte, de {@code perLevel(effet) × niveau effectif}. */
    private <E extends DarkEffect> double darkSum(Class<E> type, ToDoubleFunction<E> perLevel) {
        double sum = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (type.isInstance(dark.effect())) sum += perLevel.applyAsDouble(type.cast(dark.effect())) * effectLevel(dark);
        }
        return sum;
    }

    /** Atomes de base ajoutés à chaque fusion par l'arbre. */
    public double darkAtomsPerFusion() {
        return darkMemo("atomsPerFusion", () -> darkSum(DarkEffect.AddAtomsPerFusion.class, DarkEffect.AddAtomsPerFusion::perLevel));
    }

    /** Nombre d'éléments tirés à chaque synthèse, avant le tirage double : un, plus ceux de l'arbre. */
    public int elementsPerSynthesis() {
        int count = 1 + this.<Integer>darkMemo("elementsPerSynthesis",
                () -> (int) darkSum(DarkEffect.AddElementsPerSynthesis.class, DarkEffect.AddElementsPerSynthesis::perLevel));
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
                // La sorte d'effet d'abord : le niveau effectif (défis, styles) ne se calcule que pour les bonnes cases (0.298).
                if (!(dark.effect() instanceof DarkEffect.MultiplyBaseParticles multiply)) continue;
                int level = effectLevel(dark);
                if (level > 0) {
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
                BigNum lightYears = darkMatterReachLightYears();
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
                if (!(dark.effect() instanceof DarkEffect.AutoExpansion auto)) continue;
                int level = effectLevel(dark);
                if (level > 0) {
                    share += auto.share() * Math.pow(auto.growth(), level - 1);
                }
            }
            return share;
        });
    }

    /** Secondes d'appui que vaut chaque fusion pour la matière noire (0 sans l'amélioration). */
    public double darkFusionPulse() {
        return darkMemo("fusionPulse", () -> darkSum(DarkEffect.FusionPulse.class, DarkEffect.FusionPulse::seconds));
    }

    /** Niveaux de vitesse offerts par l'arbre : ils s'ajoutent à ceux achetés et ne se perdent jamais. */
    public int startingSpeedLevels() {
        if (inChallenge()) return 0;
        return this.<Integer>darkMemo("startingSpeedLevels",
                () -> (int) darkSum(DarkEffect.StartingSpeedLevels.class, DarkEffect.StartingSpeedLevels::perLevel));
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
        int steps = this.<Integer>darkMemo("copySteps",
                () -> (int) darkSum(DarkEffect.IncreaseMaxCopies.class, DarkEffect.IncreaseMaxCopies::perLevel));
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
     * ({@link idle.core.molecules.Molecule.Uncap}). Un élément unique reste unique.
     */
    public int maxCopiesOf(Element element) {
        if (element.category().unique()) return 1;
        return maxCopiesOf(element.category()) + elementUncap(element.number());
    }

    /** Vrai quand l'arbre a débloqué le verrou de l'appui sur la matière noire. */
    public boolean isHoldLockUnlocked() { return hasDark(DarkEffect.HoldLock.class); }

    /** Vrai quand l'arbre a débloqué le réglage du seuil de fusion. */
    public boolean isFusionThresholdUnlocked() { return hasDark(DarkEffect.FusionThreshold.class); }

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
    public boolean isAtomCapLifted() { return hasDark(DarkEffect.UncapAtoms.class); }
}
