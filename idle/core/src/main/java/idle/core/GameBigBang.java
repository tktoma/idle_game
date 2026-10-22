package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.BangAchievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.BigBangCondition;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.BigBangMilestones;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.KindCollection;
import idle.core.molecules.KindCollections;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Le Big Bang : ses défis et ses paliers, l'expansion, les collections et les améliorations d'espace.
 */
public abstract class GameBigBang extends GameDarkMatter {

    GameBigBang(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }


    // ------------------------------------------------------------------
    // Le Big Bang
    // ------------------------------------------------------------------

    /** Vrai si cette condition du Big Bang est remplie en ce moment. */
    public boolean isBigBangConditionMet(BigBangCondition condition) {
        return switch (condition) {
            case PARTICLES -> darkBalance(DarkUpgrade.Branch.PARTICLES).gte(bigBangParticles());
            case ATOMS -> state.atoms().gte(bigBangAtoms());
            case DARK_MATTER -> state.darkMatter().gte(bigBangDarkMatter());
            case ACHIEVEMENTS -> achievementCount() >= BIG_BANG_ACHIEVEMENTS;
            // Les dix de toujours (version 0.115) : la fuite, facultative, n'y entre pas.
            case CHALLENGES -> !challenges().isEmpty() && baseChallengesDone();
            case SKY -> bodiesMature() >= bigBangSkyRequirement();
        };
    }

    /** Astres mûrs que demandent les Big Bangs six, sept et huit : le ciel doit avancer avant les derniers paliers. */
    public static final int[] BIG_BANG_SKY = {18, 24, 28};

    /** Astres mûrs que demande le prochain Big Bang : rien avant le sixième ni après le huitième. */
    public int bigBangSkyRequirement() {
        int next = state.bigBangs() + 1;
        return next >= 6 && next <= 5 + BIG_BANG_SKY.length ? BIG_BANG_SKY[next - 6] : 0;
    }

    /** Les conditions du prochain Big Bang : les cinq de toujours, et le ciel pour les Big Bangs six à huit. */
    public List<BigBangCondition> bigBangConditions() {
        List<BigBangCondition> all = new ArrayList<>(List.of(BigBangCondition.values()));
        if (bigBangSkyRequirement() == 0) all.remove(BigBangCondition.SKY);
        return all;
    }

    /** Nombre de conditions du Big Bang remplies en ce moment, sur les cinq. */
    public int bigBangConditionsMet() {
        int met = 0;
        for (BigBangCondition condition : bigBangConditions()) {
            if (isBigBangConditionMet(condition)) met++;
        }
        return met;
    }

    /**
     * Vrai si le Big Bang peut être déclenché : les cinq conditions ({@link BigBangCondition}) sont
     * remplies au même moment, hors de tout défi. Les particules et les atomes se comptent dans la
     * partie en cours : une explosion les reprend, et il faut les réunir de nouveau.
     */
    public boolean canBigBang() {
        return state.started() && !inChallenge()
                && bigBangConditionsMet() == bigBangConditions().size();
    }

    /**
     * Déclenche le Big Bang : le passage à l'acte suivant.
     *
     * <p>Tout ce que les trois premiers actes ont construit disparaît : particules, atomes,
     * améliorations, automatismes, éléments, puis la matière noire, sa taille, son arbre, ses
     * automatismes, la masse du tableau et les défis réussis. La partie repart d'un seul
     * générateur, comme au premier jour. Restent les succès et leurs bonus, les records des
     * défis, le temps de jeu, les statistiques, et le nombre de Big Bangs ({@link #bigBangs()}).
     *
     * <p>Reste aussi tout ce qui appartient à l'acte que le Big Bang ouvre : l'espace et ses
     * améliorations, les molécules, leurs rassemblements, les assemblages, les astres et la galaxie.
     * Chaque Big Bang accélère l'expansion ({@link #spacePerSecond()}) ; en échange le tableau
     * périodique est vide, et il faut le regarnir avant de créer de nouvelles molécules.
     *
     * <p>À partir du Big Bang qui atteint le palier voulu ({@link #bigBangKeepsDarkTree()}), l'arbre
     * de matière noire reste aussi, avec ses automatismes : seuls partent la réserve, la taille et
     * la masse du tableau. Les défis réussis, eux, restent dès un palier plus tôt
     * ({@link #bigBangKeepsChallenges()}).
     *
     * @return {@code true} si le Big Bang a eu lieu
     */
    public boolean bigBang() {
        if (!canBigBang()) return false;
        // Le Big Bang à froid (version 0.088) : le Big Bang le réussit.
        if (inTrial(idle.core.universe.Trial.COLD_BANG)) completeTrial();
        noteHeldPeaks();
        state.stats().noteProduction(productionAtFusion());
        // Un défi de Big Bang en cours est réussi : sa récompense est acquise, son temps noté.
        BangChallenge challenge = activeBangChallenge();
        if (challenge != null) {
            double time = bangChallengeTime();
            Double best = state.bangChallengeTimes().get(challenge.name());
            if (best == null || time < best) state.setBangChallengeTime(challenge.name(), time);
            state.addCompletedBangChallenge(challenge.name());
            state.setActiveBangChallenge(null);
        }
        boolean first = !state.stats().reached(GameStats.Step.FIRST_BIG_BANG);
        state.stats().noteBigBang(state.explosions(), state.timePlayed());
        if (first) noteRecord(GameStats.Step.FIRST_BIG_BANG);
        clearForBigBang();
        state.setBigBangs(state.bigBangs() + 1);
        state.stats().restartRun();
        return true;
    }

    /**
     * Rouvre les bifurcations : leurs voies et les liaisons qui en dépendent repartent de zéro, et la
     * matière noire des liaisons cesse d'être comptée comme dépensée. Ce que fait chaque Big Bang ; et
     * l'explosion qui suit « Rouvrir les bifurcations » (version 0.084).
     */
    void reopenForks() {
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.isLink()) {
                BigNum paid = BigNum.ZERO;
                for (int level = 0; level < state.darkLevelOf(dark.id()); level++) paid = paid.add(dark.costAt(level));
                state.setDarkMatterSpent(state.darkMatterSpent().subtract(paid).max(BigNum.ZERO));
            }
            if (dark.isFork() || dark.isLink()) state.setDarkLevel(dark.id(), 0);
        }
    }

    /** Efface ce qu'un Big Bang efface, en gardant ce que les paliers atteints (ou celui qu'il atteint) font garder. */
    private void clearForBigBang() {
        // Le petit arbre du deuxième univers (version 0.087) : le Big Bang rend le choix, gratuitement.
        state.setCycleChoice(null);
        boolean keepsTree = bigBangKeepsDarkTree();
        java.util.Set<String> keptChallenges = bigBangKeepsChallenges()
                ? new java.util.LinkedHashSet<>(state.completedChallenges()) : java.util.Set.of();
        state.clearMatter();
        if (keepsTree) {
            state.clearDarkMatterKeepingTree();
            // Les bifurcations se rouvrent à chaque Big Bang, même quand l'arbre reste : le joueur peut changer de voie.
            // Les liaisons, qui en dépendent, repartent avec elles ; la matière noire qu'elles avaient coûté
            // cesse d'être comptée comme dépensée. Avec la Superposition (0.070), il n'y a plus de choix à refaire :
            // voies et liaisons restent.
            if (!hasSuperposition()) reopenForks();
        } else {
            state.clearDarkMatter();
        }
        for (String kept : keptChallenges) state.addCompletedChallenge(kept);
        // Le record d'explosion repart : après un Big Bang, la première explosion redemande des heures.
        state.setExplosionRecord(0);
    }

    // ------------------------------------------------------------------
    // Les défis de Big Bang
    // ------------------------------------------------------------------

    /** Les défis de Big Bang, dans l'ordre du catalogue. */
    public List<BangChallenge> bangChallenges() {
        return List.of(BangChallenge.values());
    }

    /** Big Bangs qui ouvrent les défis de Big Bang : le cinquième, comme avant les paliers six à huit. */
    public static final int BANG_CHALLENGES_BIG_BANGS = 5;

    /** Vrai à partir du cinquième Big Bang : les défis de Big Bang s'ouvrent, pour donner un sens aux Big Bangs suivants. */
    public boolean isBangChallengesUnlocked() {
        return state.bigBangs() >= BANG_CHALLENGES_BIG_BANGS;
    }

    /** Le défi de Big Bang en cours, ou {@code null}. */
    public BangChallenge activeBangChallenge() {
        String name = state.activeBangChallenge();
        if (name == null) return null;
        for (BangChallenge challenge : BangChallenge.values()) {
            if (challenge.name().equals(name)) return challenge;
        }
        return null;
    }

    /** Vrai si ce défi de Big Bang a été réussi : sa récompense agit. */
    public boolean isBangChallengeCompleted(BangChallenge challenge) {
        return state.completedBangChallenges().contains(challenge.name());
    }

    /** Nombre de défis de Big Bang réussis. */
    public int completedBangChallenges() {
        int done = 0;
        for (BangChallenge challenge : BangChallenge.values()) {
            if (isBangChallengeCompleted(challenge)) done++;
        }
        return done;
    }

    /** Le meilleur temps d'un défi de Big Bang réussi, en secondes, ou {@code NaN} s'il ne l'a jamais été. */
    public double bangChallengeBest(BangChallenge challenge) {
        return state.bangChallengeTimes().getOrDefault(challenge.name(), Double.NaN);
    }

    /** Temps de jeu écoulé depuis le début du défi de Big Bang en cours, en secondes ; 0 hors défi. */
    public double bangChallengeTime() {
        return state.activeBangChallenge() == null ? 0 : Math.max(0, state.timePlayed() - state.bangChallengeStarted());
    }

    /** Vrai si un défi de Big Bang peut commencer : ils sont ouverts, aucun n'est en cours, et aucun défi d'explosion non plus. */
    public boolean canStartBangChallenge() {
        return state.started() && isBangChallengesUnlocked() && state.activeBangChallenge() == null && !inChallenge();
    }

    /**
     * Vrai si ce défi-là peut commencer. Le ciel figé (version 0.117) attend que le ciel du Big Bang suivant soit déjà
     * réuni : sans nouvel astre, il ne le serait jamais.
     */
    public boolean canStartBangChallenge(BangChallenge challenge) {
        if (!canStartBangChallenge()) return false;
        return challenge != BangChallenge.FROZEN || isBigBangConditionMet(BigBangCondition.SKY);
    }

    /**
     * Commence un défi de Big Bang : la partie repart comme après un Big Bang (mêmes choses
     * gardées), sans en compter un, et la contrainte tient jusqu'au Big Bang suivant.
     *
     * @return {@code true} si le défi a commencé
     */
    public boolean startBangChallenge(BangChallenge challenge) {
        if (challenge == null || !canStartBangChallenge() || !canStartBangChallenge(challenge)) return false;
        // Les deux défis de la 0.117 ne remettent rien à zéro : la partie continue.
        if (!challenge.keepsRun()) {
            noteHeldPeaks();
            clearForBigBang();
            state.stats().restartRun();
        }
        state.setActiveBangChallenge(challenge.name());
        state.setBangChallengeStarted(state.timePlayed());
        return true;
    }

    /** Abandonne le défi de Big Bang en cours : la contrainte tombe, la partie continue, rien n'est gagné. */
    public boolean abandonBangChallenge() {
        if (state.activeBangChallenge() == null) return false;
        state.setActiveBangChallenge(null);
        return true;
    }

    /** Vrai si cette contrainte de défi de Big Bang s'applique en ce moment. */
    boolean bangRuled(BangChallenge challenge) {
        return challenge.name().equals(state.activeBangChallenge());
    }

    /** Ce que les défis de Big Bang réussis multiplient pour cette récompense : 1 sans défi réussi. */
    public double bangReward(BangChallenge.Reward reward) {
        double factor = 1;
        for (BangChallenge challenge : BangChallenge.values()) {
            if (challenge.reward() == reward && isBangChallengeCompleted(challenge)) factor *= BANG_REWARDS.get(reward);
        }
        return factor;
    }

    /** Ce que vaut chaque récompense de défi de Big Bang. */
    public static final Map<BangChallenge.Reward, Double> BANG_REWARDS = Map.of(
            BangChallenge.Reward.SPACE, 2.0, BangChallenge.Reward.ACCRETION, 1.5, BangChallenge.Reward.DARK_MATTER, 2.0,
            BangChallenge.Reward.MATURATION, 2.0, BangChallenge.Reward.PROBES, 2.0);

    // ------------------------------------------------------------------
    // Les paliers de Big Bang
    // ------------------------------------------------------------------

    /** Les paliers de Big Bang, du premier au cinquième ({@link BigBangMilestones}). */
    public List<BigBangMilestone> bigBangMilestones() {
        return BigBangMilestones.DEFAULT;
    }

    /** Vrai si ce palier agit : assez de Big Bangs ont été déclenchés, et il est payé s'il a un prix (version 0.124). */
    public boolean isBigBangMilestoneReached(BigBangMilestone milestone) {
        return isBigBangMilestoneOpen(milestone) && (!milestone.hasPrice() || state.boughtMilestones().contains(milestone.name()));
    }

    /** Vrai une fois le nombre de Big Bangs du palier atteint : il s'achète alors, s'il a un prix (version 0.124). */
    public boolean isBigBangMilestoneOpen(BigBangMilestone milestone) {
        return state.bigBangs() >= milestone.bigBangs();
    }

    /** Ce que le joueur a pour payer un palier : les particules comme l'arbre les compte (en main ou une seconde de production), ou les atomes en main. */
    public BigNum milestoneBalance(BigBangMilestone milestone) {
        return milestone.resource() == Resource.ATOMS ? state.atoms() : darkBalance(DarkUpgrade.Branch.PARTICLES);
    }

    /** Vrai si ce palier est ouvert, pas encore payé, et que le joueur a de quoi le payer. */
    public boolean canBuyBigBangMilestone(BigBangMilestone milestone) {
        return milestone != null && state.started() && isBigBangMilestoneOpen(milestone) && !isBigBangMilestoneReached(milestone)
                && milestoneBalance(milestone).gte(milestone.price());
    }

    /** Paie un palier de Big Bang (version 0.124) : ses effets agissent dès maintenant et jusqu'à l'univers suivant. */
    public boolean buyBigBangMilestone(BigBangMilestone milestone) {
        if (!canBuyBigBangMilestone(milestone)) return false;
        if (milestone.resource() == Resource.ATOMS) state.setAtoms(state.atoms().subtract(milestone.price()).max(BigNum.ZERO));
        else state.setParticles(state.particles().subtract(milestone.price()).max(BigNum.ZERO));
        state.addBoughtMilestone(milestone.name());
        return true;
    }

    /** Paie tous les paliers payables, du moins cher au plus cher dans l'ordre du catalogue ; le nombre payé. */
    public int buyAllBigBangMilestones() {
        int bought = 0;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) if (buyBigBangMilestone(milestone)) bought++;
        return bought;
    }

    /** Le premier palier ouvert qui reste à payer, ou {@code null}. */
    public BigBangMilestone nextMilestoneToBuy() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (isBigBangMilestoneOpen(milestone) && !isBigBangMilestoneReached(milestone)) return milestone;
        }
        return null;
    }

    /** Nombre de paliers de Big Bang atteints. */
    public int bigBangMilestonesReached() {
        int reached = 0;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (isBigBangMilestoneReached(milestone)) reached++;
        }
        return reached;
    }

    /** Le prochain palier de Big Bang, ou {@code null} quand ils sont tous atteints. */
    public BigBangMilestone nextBigBangMilestone() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) return milestone;
        }
        return null;
    }

    /** Ce par quoi les paliers de Big Bang atteints multiplient une grandeur ({@link BigBangMilestone.Boost}). Vaut 1 sans aucun. */
    public double bigBangMilestoneBoost(Molecule.Stat stat) {
        if (bangRuled(BangChallenge.FORGOTTEN)) return 1;
        double factor = 1;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.Boost boost && boost.stat() == stat) factor *= boost.factor();
            }
        }
        return factor;
    }

    /**
     * Ce par quoi la matière noire en réserve multiplie l'expansion, une fois atteint le palier qui
     * le permet ({@link BigBangMilestone.DarkMatterSpace}) : {@code 1 + part × √(matière noire)}.
     * Avec 100 de matière noire et une part de 0,1 : le double ; avec 2 500 : six fois. Vaut 1 avant
     * le palier, et après un Big Bang tant que la matière noire n'est pas revenue.
     */
    public double darkMatterSpaceBoost() {
        if (bangRuled(BangChallenge.FORGOTTEN)) return 1;
        double perRoot = 0;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.DarkMatterSpace dark) perRoot += dark.perRoot();
            }
        }
        if (perRoot == 0) return 1;
        return 1 + perRoot * Math.sqrt(Math.max(0, state.darkMatter().toDouble()));
    }

    // ------------------------------------------------------------------
    // Les collections : la moitié d'un rayon, puis le rayon entier
    // ------------------------------------------------------------------

    private final Map<Molecule.Kind, int[]> kindCounts = new java.util.EnumMap<>(Molecule.Kind.class);
    int kindCountsVersion = -1;

    private int[] kindCount(Molecule.Kind kind) {
        if (kindCountsVersion != state.moleculesVersion()) {
            kindCountsVersion = state.moleculesVersion();
            kindCounts.clear();
            for (Molecule molecule : molecules.values()) {
                int[] count = kindCounts.computeIfAbsent(molecule.kind(), each -> new int[2]);
                count[1]++;
                if (state.moleculeCount(molecule.id()) > 0) count[0]++;
            }
        }
        return kindCounts.getOrDefault(kind, new int[2]);
    }

    /** Les collections des rayons, dans l'ordre du catalogue ({@link KindCollections}). */
    public List<KindCollection> collections() {
        return KindCollections.DEFAULT;
    }

    /** Nombre de sortes de ce rayon dont le joueur a créé au moins une molécule. */
    public int kindCreated(Molecule.Kind kind) {
        return kindCount(kind)[0];
    }

    /** Nombre de sortes de ce rayon dans le catalogue. */
    public int kindSize(Molecule.Kind kind) {
        return kindCount(kind)[1];
    }

    /** Nombre de sortes à créer pour la moitié de la collection de ce rayon : la moitié du rayon, arrondie au-dessus. */
    public int collectionHalf(Molecule.Kind kind) {
        return (kindSize(kind) + 1) / 2;
    }

    /** Où en est la collection d'un rayon : 0 tant que la moitié n'y est pas, 1 à la moitié, 2 une fois complète. */
    public int collectionLevel(Molecule.Kind kind) {
        int size = kindSize(kind);
        int created = kindCreated(kind);
        if (size == 0 || !isBigBangUnlocked()) return 0;
        return created >= size ? 2 : created >= collectionHalf(kind) ? 1 : 0;
    }

    /** Nombre de collections au moins à moitié réunies, et nombre de collections complètes. */
    public int collectionsAt(int level) {
        int count = 0;
        for (KindCollection collection : KindCollections.DEFAULT) {
            if (collectionLevel(collection.kind()) >= level) count++;
        }
        return count;
    }

    /** Ce par quoi les collections réunies multiplient une grandeur : leurs facteurs se multiplient entre eux. Vaut 1 sans aucune. */
    public double collectionBoost(Molecule.Stat stat) {
        double factor = 1;
        for (KindCollection collection : KindCollections.DEFAULT) {
            if (collection.stat() == stat) factor *= collection.factor(collectionLevel(collection.kind()));
        }
        return factor;
    }

    /**
     * Ce par quoi les améliorations d'espace acquises multiplient une grandeur
     * ({@link SpaceUpgrade.Boost}) : leurs facteurs se multiplient entre eux. Vaut 1 sans aucune.
     */
    public double spaceUpgradeBoost(Molecule.Stat stat) {
        refreshMoleculeBonuses();
        return upgradeBoosts.getOrDefault(stat, 1.0);
    }

    /**
     * Nombre d'exemplaires que les molécules créées ajoutent au maximum de cet élément : la partie
     * entière de ce pour quoi compte chaque sorte dont c'est le bonus ({@link Molecule.Uncap}).
     */
    public int elementUncap(int atomicNumber) {
        refreshMoleculeBonuses();
        return elementUncaps.getOrDefault(atomicNumber, 0);
    }

    /**
     * Recalcule ce que donnent les molécules, rassemblées ou non, quand leur liste a changé. Une
     * sorte de molécule compte pour {@link #effectiveMolecules(String)} : un plafond relevé prend
     * la partie entière de ce nombre. Une sorte rassemblée ajoute en plus les particules et les
     * atomes de son état, comptés de la même façon.
     */
    void refreshMoleculeBonuses() {
        if (moleculeBonusesVersion == state.moleculesVersion()) return;
        moleculeBonusesVersion = state.moleculesVersion();
        moleculeBoosts.clear();
        elementUncaps.clear();
        upgradeBoosts.clear();
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.Boost boost && state.ownsSpaceUpgrade(upgrade.id())) {
                upgradeBoosts.merge(boost.stat(), boost.factor(), (a, b) -> a * b);
            }
        }
        for (String id : state.molecules().keySet()) {
            Molecule molecule = molecules.get(id);
            if (molecule == null) continue;
            double count = effectiveMolecules(id);
            switch (molecule.bonus()) {
                case Molecule.Boost boost -> moleculeBoosts.merge(boost.stat(), boost.perMolecule() * count, Double::sum);
                case Molecule.Uncap uncap -> elementUncaps.merge(uncap.element(), (int) Math.floor(count + 1e-9), Integer::sum);
                case null -> { }
            }
            if (gatheredMolecules(id) > 0) {
                Molecule.State matter = molecule.state();
                if (matter.particles() > 0) moleculeBoosts.merge(Molecule.Stat.PARTICLES, matter.particles() * count, Double::sum);
                if (matter.atoms() > 0) moleculeBoosts.merge(Molecule.Stat.ATOMS, matter.atoms() * count, Double::sum);
            }
        }
        // Les assemblages formés ajoutent chacun leur part à la grandeur de leur famille, et les astres à la leur.
        for (String id : state.assemblies()) {
            Assembly assembly = assemblies.get(id);
            if (assembly != null) moleculeBoosts.merge(assembly.boost().stat(), assembly.boost().perMolecule(), Double::sum);
        }
        for (String id : state.bodies()) {
            Body body = bodies.get(id);
            // Un astre qui mûrit donne déjà une part de son bonus.
            double share = state.maturingLeft(id) > 0 ? MATURING_SHARE : 1;
            if (body != null) moleculeBoosts.merge(body.boost().stat(), body.boost().perMolecule() * share, Double::sum);
        }
        // Chaque système solaire complet augmente toutes les grandeurs, selon son harmonie.
        for (SolarSystem system : state.solarSystems()) {
            if (!isSystemComplete(system)) continue;
            double gain = SYSTEM_GAIN * systemHarmony(system);
            for (Molecule.Stat stat : Molecule.Stat.values()) moleculeBoosts.merge(stat, gain, Double::sum);
        }
    }

    // ------------------------------------------------------------------
    // Les améliorations d'espace
    // ------------------------------------------------------------------

    /** Toutes les améliorations liées à l'espace, dans l'ordre du catalogue. */
    public List<SpaceUpgrade> spaceUpgrades() {
        if (spaceUpgradeList == null) spaceUpgradeList = List.copyOf(spaceUpgrades.values());
        return spaceUpgradeList;
    }

    SpaceUpgrade spaceUpgrade(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrades.get(spaceUpgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration d'espace inconnue : " + spaceUpgradeId);
        return upgrade;
    }

    /** Vrai si cette amélioration d'espace est acquise. */
    public boolean ownsSpaceUpgrade(String spaceUpgradeId) {
        return state.ownsSpaceUpgrade(spaceUpgrade(spaceUpgradeId).id());
    }

    /**
     * Vrai si cette amélioration sera acquise dès que l'espace créé suffira : le premier Big Bang
     * a eu lieu, et celle dont elle dépend est déjà acquise.
     */
    public boolean isSpaceUpgradeAvailable(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrade(spaceUpgradeId);
        return isBigBangUnlocked() && (upgrade.requires() == null || state.ownsSpaceUpgrade(upgrade.requires()));
    }

    /**
     * Vrai si cette amélioration peut être acquise maintenant : elle est accessible, pas encore
     * acquise, et l'expansion a créé en tout autant d'espace qu'elle en demande
     * ({@link SpaceUpgrade#space()}). L'espace libre n'entre pas en compte.
     */
    public boolean canBuySpaceUpgrade(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrade(spaceUpgradeId);
        return state.started() && isSpaceUpgradeAvailable(spaceUpgradeId) && !state.ownsSpaceUpgrade(upgrade.id())
                && hasCreatedSpace(upgrade.space());
    }

    /** Les améliorations d'espace acquises depuis le dernier appel de {@link #takeNewSpaceUpgrades()}. */
    final List<SpaceUpgrade> newSpaceUpgrades = new ArrayList<>();

    /**
     * Acquiert toutes les améliorations d'espace dont le seuil est atteint : elles ne coûtent rien,
     * il n'y a donc rien à décider. Une amélioration peut en ouvrir une autre, déjà à portée : on
     * recommence tant qu'il s'en prend.
     */
    void takeSpaceUpgrades() {
        if (!state.started() || !isBigBangUnlocked() || state.spaceUpgradeCount() >= spaceUpgrades.size()) return;
        boolean taken;
        do {
            taken = false;
            for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
                if (!canBuySpaceUpgrade(upgrade.id())) continue;
                state.addSpaceUpgrade(upgrade.id());
                newSpaceUpgrades.add(upgrade);
                taken = true;
            }
        } while (taken);
    }

    /**
     * Les améliorations d'espace que le temps a apportées depuis le dernier appel, dans l'ordre :
     * l'interface s'en sert pour les annoncer. La liste est vidée à chaque lecture.
     */
    public List<SpaceUpgrade> takeNewSpaceUpgrades() {
        List<SpaceUpgrade> taken = List.copyOf(newSpaceUpgrades);
        newSpaceUpgrades.clear();
        return taken;
    }

    boolean ownsSpaceEffect(Class<? extends SpaceUpgrade.Effect> type) {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (type.isInstance(upgrade.effect()) && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Vrai une fois acquise l'amélioration d'espace « Création continue » : tenir le clic sur une molécule répète sa création. */
    public boolean isHoldCreateUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.HoldCreate.class);
    }

    /** Vrai une fois acquise l'amélioration d'espace « Gestes groupés » : tout rassembler, tout assembler, former tous les astres prêts. */
    public boolean isBulkFormUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.BulkForm.class);
    }

    /**
     * Rassemble toutes les sortes de molécules qui peuvent l'être, dans l'ordre du catalogue, tant
     * que l'espace libre suffit ({@link #formSubstance(String)}). Il faut les gestes groupés.
     *
     * @return le nombre de sortes rassemblées
     */
    public int formAllSubstances() {
        if (!isBulkFormUnlocked()) return 0;
        int formed = 0;
        for (Molecule molecule : moleculeList) {
            if (formSubstance(molecule.id())) formed++;
        }
        return formed;
    }

    /**
     * Forme tous les assemblages qui peuvent l'être, dans l'ordre du catalogue
     * ({@link #formAssembly(String)}). Il faut les gestes groupés.
     *
     * @return le nombre d'assemblages formés
     */
    public int formAllAssemblies() {
        if (!isBulkFormUnlocked()) return 0;
        int formed = 0;
        for (Assembly assembly : assemblies.values()) {
            if (formAssembly(assembly.id())) formed++;
        }
        return formed;
    }

    /**
     * Forme tous les astres qui peuvent l'être ({@link #formBody(String)}). Un astre formé peut en
     * rendre un plus grand possible : on recommence tant qu'il s'en forme. Il faut les gestes groupés.
     *
     * @return le nombre d'astres formés
     */
    public int formAllBodies() {
        if (!isBulkFormUnlocked()) return 0;
        int formed = 0;
        boolean again;
        do {
            again = false;
            for (Body body : bodies.values()) {
                if (formBody(body.id())) {
                    formed++;
                    again = true;
                }
            }
        } while (again);
        return formed;
    }

    /** Nombre de sortes qui peuvent être rassemblées en ce moment, chacune prise seule. */
    public int substancesReady() {
        if (!isStatesUnlocked()) return 0;
        int ready = 0;
        for (String id : state.molecules().keySet()) {
            if (molecules.containsKey(id) && canFormSubstance(id)) ready++;
        }
        return ready;
    }

    /** Nombre d'assemblages qui peuvent être formés en ce moment. */
    public int assembliesReady() {
        if (!isAssembliesUnlocked()) return 0;
        int ready = 0;
        for (Assembly assembly : assemblies.values()) {
            if (canFormAssembly(assembly.id())) ready++;
        }
        return ready;
    }

    /** Nombre d'astres qui peuvent être formés en ce moment. */
    public int bodiesReady() {
        if (!isBodiesUnlocked()) return 0;
        int ready = 0;
        for (Body body : bodies.values()) {
            if (canFormBody(body.id())) ready++;
        }
        return ready;
    }

    // ------------------------------------------------------------------
    // L'expansion de la matière
    // ------------------------------------------------------------------

    /**
     * Espace que l'expansion de la matière ajoute chaque seconde : {@link #SPACE_PER_SECOND} par
     * Big Bang déclenché, augmenté par les molécules, les assemblages et les astres qui
     * l'accélèrent, par les améliorations d'inflation, par les paliers de Big Bang, et, une fois le
     * palier atteint, par la matière noire en réserve ({@link #darkMatterSpaceBoost()}). Nul avant le
     * premier Big Bang.
     */
    public BigNum spacePerSecond() {
        if (!isBigBangUnlocked()) return BigNum.ZERO;
        return steadySpacePerSecond().multiply(cometBoost());
    }

    /** L'espace par seconde sans le sillage d'une comète : ce que l'expansion donne en régime ordinaire. */
    public BigNum steadySpacePerSecond() {
        if (!isBigBangUnlocked()) return BigNum.ZERO;
        return BigNum.of(SPACE_PER_SECOND * state.bigBangs() * moleculeBoost(Molecule.Stat.SPACE) * darkMatterSpaceBoost()
                * (inTrial(idle.core.universe.Trial.COLD_BANG) ? idle.core.universe.Trial.COLD_EXPANSION : 1)
                * bangReward(BangChallenge.Reward.SPACE) * colliderSpaceMultiplier() * probeBoostMultiplier()
                * bangAchievementMultiplier(BangAchievement.Reward.SPACE));
    }

    /**
     * Fait passer le temps de l'expansion : l'espace grandit de {@link #spacePerSecond()} par
     * seconde de jeu, quoi que fasse le joueur. Rien ne le reprend, ni l'explosion ni le Big Bang ;
     * les molécules en occupent une part ({@link #occupiedSpace()}), leurs lieux de rassemblement en réservent une autre
     * ({@link #reservedSpace()}).
     */
    void expandSpace(double dt) {
        if (dt <= 0 || !state.started() || !isBigBangUnlocked()) return;
        // Le sillage d'une comète ne compte que pour le temps qu'il lui reste, même si le pas de temps est plus long.
        double boosted = Math.min(dt, state.cometBoost());
        state.setSpace(state.space().add(steadySpacePerSecond().multiply(dt + boosted * (COMET_BOOST - 1))));
    }
}
