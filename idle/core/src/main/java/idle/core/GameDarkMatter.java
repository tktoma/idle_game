package idle.core;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BangAchievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * La matière noire : l'explosion, ses défis, les paliers de taille et les automatismes de matière noire.
 */
public abstract class GameDarkMatter extends GameParticles {

    GameDarkMatter(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract void reopenForks();


    // ------------------------------------------------------------------
    // L'explosion
    // ------------------------------------------------------------------

    /**
     * Vrai quand le tableau périodique peut exploser : les 118 éléments sont découverts. Un
     * exemplaire de chacun suffit ; les suivants renforcent les éléments, ils ne conditionnent rien.
     */
    public boolean canExplode() {
        return state.started() && state.elements().size() == PeriodicTable.ELEMENTS.size();
    }

    /**
     * Fait exploser le tableau périodique, une fois tous ses éléments découverts.
     *
     * <p>Tout ce qui est fait de matière disparaît : particules, atomes, améliorations (même
     * celles payées en atomes), automatismes et éléments. La partie repart d'un seul générateur,
     * comme au tout début, sauf ce que l'arbre de matière noire permet de garder. En échange, le
     * joueur gagne {@link #darkMatterPerExplosion()} de matière noire, qu'aucune explosion ne lui
     * reprendra.
     *
     * @return {@code true} si l'explosion a eu lieu
     */
    public boolean explode() {
        if (!canExplode()) return false;
        BigNum reward = nextExplosionDarkMatter();
        boolean replay = isChallengeReplay();
        // La prime de vitesse : la première explosion depuis le Big Bang pose le record, les suivantes le battent de la marge ou non.
        boolean primed = earnsSpeedPrime();
        double duration = state.stats().runTime();
        if (!replay && (primed || state.explosionRecord() <= 0)) state.setExplosionRecord(duration);
        if (primed) {
            speedPrimePaid = true;
            state.stats().noteSpeedPrime();
        }
        boolean firstExplosion = state.stats().timedExplosions() == 0;
        // Un défi en cours est réussi : il laisse sa récompense, et son meilleur temps est noté.
        Challenge challenge = activeChallenge();
        if (challenge != null) {
            double time = state.stats().runTime();
            Double best = state.challengeTimes().get(challenge.id());
            if (best == null || time < best) state.setChallengeTime(challenge.id(), time);
            state.addCompletedChallenge(challenge.id());
            state.setActiveChallenge(null);
        }
        noteHeldPeaks();
        state.stats().noteProduction(productionAtFusion());
        // Une explosion rapide ne compte qu'à partir de la deuxième : la première n'a pas de précédente.
        if (state.explosions() >= 1 && state.stats().runTime() < Achievements.FAST_EXPLOSION_SECONDS) {
            award(Achievements.FAST_EXPLOSION);
        }
        state.setDarkMatter(state.darkMatter().add(reward));
        state.setExplosions(state.explosions() + 1);
        // Un défi rejoué ne compte que pour son temps : ni matière noire, ni tableau plus lourd.
        if (!replay) state.setTableWeightLevel(state.tableWeightLevel() + 1);
        // L'appui verrouillé est un réglage du joueur : l'explosion ne le défait pas.
        clearRun();
        state.stats().endRun();
        if (firstExplosion) noteStep(GameStats.Step.FIRST_EXPLOSION);
        // La contraction douce (version 0.88) : l'explosion la réussit.
        if (inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) completeTrial();
        // Demandé par le joueur (version 0.84) : les bifurcations se rouvrent avec cette explosion.
        if (state.forkResetArmed()) {
            if (!hasSuperposition()) reopenForks();
            state.setForkResetArmed(false);
        }
        return true;
    }

    /**
     * Efface la partie en cours comme le fait une explosion : toute la matière disparaît, sauf ce
     * que l'arbre de matière noire permet de garder, et le départ lancé est appliqué.
     */
    private void clearRun() {
        // Ce que la matière noire permet de garder, mis de côté avant de tout effacer.
        Map<String, Integer> keptLevels = hasDark(DarkEffect.KeepUpgradesOnExplosion.class)
                ? state.upgradeLevels() : Map.of();
        boolean keepAutomations = hasDark(DarkEffect.KeepAutomationsOnExplosion.class);
        java.util.Set<String> owned = keepAutomations ? state.ownedAutomations() : java.util.Set.of();
        java.util.Set<String> enabled = keepAutomations ? state.enabledAutomations() : java.util.Set.of();
        List<Integer> keptElements = new ArrayList<>();
        if (hasDark(DarkEffect.KeepUniqueElementsOnExplosion.class)) {
            for (int number : state.elements().keySet()) {
                if (PeriodicTable.element(number).category().unique()) keptElements.add(number);
            }
        }
        Map<String, Integer> cadences = keepAutomations ? state.automationSpeedLevels() : Map.of();

        state.clearMatter();

        for (Map.Entry<String, Integer> level : keptLevels.entrySet()) {
            Upgrade upgrade = upgrades.get(level.getKey());
            // Les générateurs sont de la matière : ils disparaissent toujours.
            if (upgrade != null && !(upgrade.effect() instanceof Effect.AddGenerator)) {
                state.setLevel(level.getKey(), level.getValue());
            }
        }
        for (String id : owned) {
            state.addAutomation(id);
            state.setAutomationEnabled(id, enabled.contains(id));
        }
        cadences.forEach(state::setAutomationSpeedLevel);
        for (int number : keptElements) state.setElementCount(number, 1);
        applyHeadStart();
    }

    // ------------------------------------------------------------------
    // Les défis d'explosion
    // ------------------------------------------------------------------

    /** Tous les défis, dans l'ordre du catalogue. */
    public List<Challenge> challenges() {
        return Challenges.DEFAULT;
    }

    private Challenge challenge(String challengeId) {
        for (Challenge challenge : Challenges.DEFAULT) {
            if (challenge.id().equals(challengeId)) return challenge;
        }
        throw new IllegalArgumentException("Défi inconnu : " + challengeId);
    }

    /** Le défi en cours, ou {@code null} en partie ordinaire. */
    public Challenge activeChallenge() {
        return state.activeChallenge() == null ? null : challenge(state.activeChallenge());
    }

    /** Vrai quand le joueur a gagné assez de matière noire pour tenter ce défi. Rien n'est dépensé. */
    public boolean isChallengeUnlocked(String challengeId) {
        Challenge challenge = challenge(challengeId);
        return isDarkMatterUnlocked() && darkMatterEarned().gte(BigNum.of(challenge.darkMatter()));
    }

    /** Vrai si ce défi a été réussi au moins une fois : sa récompense est acquise. */
    public boolean isChallengeCompleted(String challengeId) {
        return state.completedChallenges().contains(challenge(challengeId).id());
    }

    /** Nombre de défis réussis. */
    public int completedChallenges() {
        return state.completedChallenges().size();
    }

    /** Meilleur temps de ce défi, en secondes de jeu, ou −1 s'il n'a jamais été réussi. */
    public double challengeBestTime(String challengeId) {
        Double best = state.challengeTimes().get(challenge(challengeId).id());
        return best == null ? -1 : best;
    }

    /**
     * Commence un défi : la partie en cours est effacée, sans matière noire en échange, et la
     * contrainte du défi s'applique jusqu'à la prochaine explosion ou jusqu'à l'abandon. Un défi
     * déjà réussi peut être rejoué, pour le temps.
     *
     * <p>Un défi se joue sans les mémoires de la matière noire : rien n'est gardé de la partie
     * précédente, et tant qu'il dure il n'y a ni générateurs ni niveaux de vitesse offerts, ni
     * départ lancé, ni automatismes offerts. Tout ce que l'arbre multiplie reste acquis. C'est ce
     * qui fait d'un défi une vraie partie, à rejouer depuis le premier générateur.
     *
     * @return {@code true} si le défi a commencé ; faux s'il est verrouillé ou si un défi est déjà en cours
     */
    public boolean startChallenge(String challengeId) {
        Challenge challenge = challenge(challengeId);
        if (!state.started() || !isChallengeUnlocked(challengeId) || state.activeChallenge() != null) return false;
        state.setActiveChallenge(challenge.id());
        noteHeldPeaks();
        state.clearMatter();   // rien n'est gardé : ni automatismes, ni améliorations, ni éléments
        state.stats().restartRun();
        return true;
    }

    /**
     * Abandonne le défi en cours : la partie est effacée de nouveau et repart sans contrainte.
     *
     * @return {@code true} s'il y avait un défi à abandonner
     */
    public boolean abandonChallenge() {
        if (state.activeChallenge() == null) return false;
        state.setActiveChallenge(null);
        clearRun();
        state.stats().restartRun();
        award(Achievements.ABANDON);
        return true;
    }

    /** Vrai pendant un défi, quel qu'il soit. */
    public boolean inChallenge() {
        return state.activeChallenge() != null;
    }

    /** Vrai si le défi en cours impose cette contrainte. */
    boolean ruled(Challenge.Rule rule) {
        String active = state.activeChallenge();
        return active != null && challenge(active).rule() == rule;
    }

    /** Vrai si le joueur a réussi un défi qui donne cette récompense. */
    boolean rewarded(Challenge.Reward reward) {
        if (rewardsVersion != state.challengesVersion()) {
            rewards.clear();
            for (Challenge challenge : Challenges.DEFAULT) {
                if (state.completedChallenges().contains(challenge.id())) rewards.add(challenge.reward());
            }
            rewardsVersion = state.challengesVersion();
        }
        return rewards.contains(reward);
    }

    /** Les récompenses des défis réussis, recalculées seulement quand la liste des défis change. */
    private final java.util.EnumSet<Challenge.Reward> rewards = java.util.EnumSet.noneOf(Challenge.Reward.class);
    private int rewardsVersion = -1;

    /**
     * La désintégration du défi du même nom : chaque exemplaire possédé a {@link #DECAY_RATE}
     * chance de disparaître par seconde. Les fractions s'accumulent : avec 50 exemplaires, il en
     * disparaît un par seconde.
     */
    void decay(double dt) {
        if (!ruled(Challenge.Rule.DECAY) || dt <= 0) return;
        int copies = 0;
        for (int count : state.elements().values()) copies += count;
        if (copies == 0) return;
        double debt = state.decayDebt() + copies * DECAY_RATE * dt;
        while (debt >= 1 && copies > 0) {
            // Un exemplaire au hasard : un élément possédé en neuf exemplaires a neuf fois plus de chances d'en perdre un.
            int roll = random.nextInt(copies);
            for (Map.Entry<Integer, Integer> entry : state.elements().entrySet()) {
                roll -= entry.getValue();
                if (roll < 0) {
                    state.setElementCount(entry.getKey(), entry.getValue() - 1);
                    break;
                }
            }
            copies--;
            debt -= 1;
        }
        state.setDecayDebt(copies == 0 ? 0 : debt);
    }

    // ------------------------------------------------------------------
    // Les paliers de taille
    // ------------------------------------------------------------------

    /**
     * Nombre de paliers de taille atteints : les repères de l'échelle des grandeurs que la
     * matière noire a dépassés ({@link SizeScale#MILESTONES}). Aucun avant la première explosion.
     */
    public int landmarksReached() {
        return isDarkMatterUnlocked() ? SizeScale.milestonesReached(state.darkMatterSize()) : 0;
    }

    /** Nombre de paliers atteints qui donnent ce bonus. */
    public int landmarksReached(SizeScale.Bonus bonus) {
        int reached = landmarksReached();
        int count = 0;
        for (int index = 0; index < reached; index++) {
            if (SizeScale.bonusOf(index) == bonus) count++;
        }
        return count;
    }

    /** Ce que les paliers de taille multiplient dans les particules de chaque création. */
    public BigNum landmarkParticlesMultiplier() {
        return BigNum.of(LANDMARK_PARTICLES).pow(landmarksReached(SizeScale.Bonus.PARTICLES));
    }

    /** Ce que les paliers de taille multiplient dans les atomes de chaque fusion. */
    public double landmarkAtomsMultiplier() {
        return 1 + LANDMARK_ATOMS * landmarksReached(SizeScale.Bonus.ATOMS);
    }

    /** Ce que les paliers de taille multiplient dans la croissance de la matière noire. */
    public double landmarkExpansionMultiplier() {
        return Math.pow(LANDMARK_EXPANSION, landmarksReached(SizeScale.Bonus.EXPANSION));
    }

    /**
     * Vrai si l'appui sur la matière noire est verrouillé : elle grossit toute seule à chaque
     * {@link #tick(double)}, quel que soit l'onglet que regarde le joueur. Il faut la case
     * « Verrou » de l'arbre.
     */
    public boolean isHoldLocked() {
        return state.holdLocked() && isHoldLockUnlocked();
    }

    /**
     * Verrouille ou libère l'appui. C'est un réglage : ni l'explosion ni le Big Bang ne le changent,
     * et il ressert dès que la case « Verrou » de l'arbre est de nouveau acquise.
     *
     * @return {@code true} si le verrou a pris l'état demandé ; faux s'il n'est pas débloqué
     */
    public boolean setHoldLocked(boolean locked) {
        if (locked && !isHoldLockUnlocked()) return false;
        state.setHoldLocked(locked);
        if (locked) award(Achievements.HOLD_LOCK);
        return true;
    }

    /**
     * Vrai une fois acquise l'amélioration d'espace « Appui automatique » ({@link SpaceUpgrade.AutoHold}).
     * Comme toute amélioration d'espace, ni l'explosion ni le Big Bang ne la reprennent.
     */
    public boolean isAutoHoldUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.AutoHold && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Vrai si l'appui automatique est en marche (il ne sert qu'une fois acquis). */
    public boolean isAutoHoldEnabled() {
        return state.autoHold();
    }

    /**
     * Met en marche ou coupe l'appui automatique.
     *
     * @return {@code true} s'il a pris l'état demandé ; faux s'il n'est pas acquis
     */
    public boolean setAutoHoldEnabled(boolean enabled) {
        if (!isAutoHoldUnlocked()) return false;
        state.setAutoHold(enabled);
        return true;
    }

    /**
     * Vrai si la matière noire grossit toute seule en ce moment, à pleine vitesse d'appui : l'appui
     * automatique est acquis, en marche, et la matière noire existe (il faut une première explosion
     * depuis le dernier Big Bang). Tenir le clic n'ajoute alors rien : c'est déjà fait.
     */
    public boolean isAutoHolding() {
        return isAutoHoldUnlocked() && state.autoHold() && isDarkMatterUnlocked() && !state.automationPaused()
                && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai si le défi en cours a déjà été réussi : on le rejoue pour son temps, sans rien y gagner d'autre. */
    public boolean isChallengeReplay() {
        Challenge challenge = activeChallenge();
        return challenge != null && state.completedChallenges().contains(challenge.id());
    }

    /**
     * Matière noire que laissera vraiment la prochaine explosion : {@link #darkMatterPerExplosion()},
     * ou rien si elle termine un défi déjà réussi.
     */
    public BigNum nextExplosionDarkMatter() {
        if (isChallengeReplay()) return BigNum.ZERO;
        return earnsSpeedPrime() ? darkMatterPerExplosion().add(speedPrime()) : darkMatterPerExplosion();
    }

    /**
     * Part du record qu'il faut battre pour toucher la prime de vitesse : exploser en moins de
     * 90 % du temps de la dernière explosion primée. Sans cette marge, une explosion automatique
     * qui gagne une fraction de seconde à chaque fois toucherait la prime sans fin.
     */
    public static final double SPEED_PRIME_MARGIN = 0.9;
    /** La prime de vitesse, avant les paliers de Big Bang : une matière noire de plus. */
    public static final BigNum SPEED_PRIME = BigNum.ONE;
    boolean speedPrimePaid = false;

    /**
     * Le temps à battre pour la prime de vitesse, en secondes : {@link #SPEED_PRIME_MARGIN} du
     * record ({@link #explosionRecord()}). 0 tant qu'aucune explosion n'a eu lieu depuis le dernier
     * Big Bang : la première pose le record, sans prime.
     */
    public double speedPrimeTime() {
        return state.explosionRecord() * SPEED_PRIME_MARGIN;
    }

    /** Durée de l'explosion qui sert de record depuis le dernier Big Bang, en secondes ; 0 s'il n'y en a pas encore. */
    public double explosionRecord() {
        return state.explosionRecord();
    }

    /** Vrai si une explosion déclenchée maintenant toucherait la prime de vitesse : le record est battu de la marge voulue, hors défi rejoué. */
    public boolean earnsSpeedPrime() {
        return !isChallengeReplay() && state.explosionRecord() > 0 && state.stats().runTime() <= speedPrimeTime();
    }

    /** Secondes qu'il reste pour toucher la prime de vitesse ; 0 quand il est trop tard ou qu'il n'y a pas de record. */
    public double speedPrimeLeft() {
        return state.explosionRecord() > 0 ? Math.max(0, speedPrimeTime() - state.stats().runTime()) : 0;
    }

    /** Ce que vaut la prime de vitesse : {@link #SPEED_PRIME}, multipliée comme la matière noire d'une explosion. */
    public BigNum speedPrime() {
        return SPEED_PRIME.multiply(bigBangDarkMatterFactor() * bangReward(BangChallenge.Reward.DARK_MATTER));
    }

    /** Vrai si la dernière explosion a touché la prime de vitesse : pour l'annoncer. Remis à faux à chaque lecture. */
    public boolean takeSpeedPrime() {
        boolean paid = speedPrimePaid;
        speedPrimePaid = false;
        return paid;
    }

    /**
     * Matière noire que laisse une explosion : {@link #DARK_MATTER_PER_EXPLOSION}, plus ce qu'ajoute
     * l'arbre, le tout multiplié par les paliers de Big Bang ({@link #bigBangDarkMatterFactor()}).
     */
    public BigNum darkMatterPerExplosion() {
        BigNum amount = DARK_MATTER_PER_EXPLOSION;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.AddDarkMatterPerExplosion add) {
                amount = amount.add(BigNum.of(add.perLevel() * effectLevel(dark)));
            }
        }
        if (rewarded(Challenge.Reward.EXTRA_DARK_MATTER)) amount = amount.add(BigNum.ONE);
        double factor = (state.bigBangs() == 0 && firstRunPace > 1 ? FIRST_CYCLE_DARK_MATTER : 1) * bigBangDarkMatterFactor() * bangReward(BangChallenge.Reward.DARK_MATTER)
                * (cycleChoice() == idle.core.universe.CycleChoice.RESERVE ? 1 + idle.core.universe.CycleChoice.RESERVE_GAIN : 1);
        return factor == 1 ? amount : amount.multiply(factor);
    }

    /**
     * Vrai une fois la première explosion déclenchée : la matière noire existe. Vrai aussi dès le
     * début d'une partie quand l'arbre a traversé le Big Bang ({@link #bigBangKeepsDarkTree()}).
     */
    public boolean isDarkMatterUnlocked() {
        return state.explosions() > 0 || state.darkMatterSpent().sign() > 0;
    }

    /**
     * Matière noire gagnée depuis le début du jeu : celle qui reste, plus celle dépensée en
     * améliorations. C'est elle, et non ce qui reste, qui ouvre les cases de l'arbre et les
     * automatismes, qui donne son élan à la croissance et dont dépendent les effets « par matière
     * noire » : dépenser de la matière noire ne fait donc rien perdre.
     */
    public BigNum darkMatterEarned() {
        return state.darkMatter().add(state.darkMatterSpent());
    }

    /** La matière noire gagnée, bornée pour rester calculable, bien au-delà de ce qu'un joueur peut avoir. */
    double darkMatterUnits() {
        return darkMatterEarned().min(BigNum.of(1, 15)).toDouble();
    }

    /**
     * Ce que l'avancement de la partie en cours apporte à la croissance de la matière noire :
     * {@code 1 + ordres de grandeur de la production + ordres de grandeur des atomes créés}. La
     * production se compte à partir de {@link #TEMPO} particules par seconde, celle d'une partie
     * qui commence. Avec 8 × 10¹² particules par seconde et 1 000 atomes créés depuis la dernière
     * explosion, le facteur vaut 1 + 12 + 3 = 16. L'élan de la matière noire suit ainsi le rythme des particules
     * et des atomes : faible au début d'une partie, de plus en plus fort ensuite.
     *
     * <p>La production comptée est celle de tous les générateurs réunis
     * ({@link #productionAtFusion()}), pour ne pas retomber à chaque fusion.
     */
    public double darkMatterProgressFactor() {
        double production = Math.max(0, productionAtFusion().divide(TEMPO).log10());
        double atoms = Math.max(0, state.totalAtoms().log10());
        return 1 + production + atoms;
    }

    /**
     * Élan de la croissance de la matière noire :
     * {@code DARK_MATTER_GROWTH_PER_UNIT × matière noire gagnée × facteur d'avancement ×
     * multiplicateur de l'arbre}. C'est son taux de croissance par seconde d'appui quand elle a
     * la taille d'un proton ; ensuite sa résistance le divise.
     */
    public double darkMatterMomentum() {
        return DARK_MATTER_GROWTH_PER_UNIT * darkMatterUnits() * darkMatterProgressFactor()
                * darkExpansionMultiplier().min(BigNum.of(1, 100)).toDouble() * landmarkExpansionMultiplier()
                * (1 + achievementBonus(Achievement.Bonus.EXPANSION))
                * moleculeBoost(Molecule.Stat.DARK_GROWTH)
                * colliderDarkGrowthMultiplier()
                * bangAchievementMultiplier(BangAchievement.Reward.DARK_GROWTH);
    }

    /**
     * Ce qui freine la croissance : la taille, comptée en protons, à la puissance
     * {@link #DARK_MATTER_RESISTANCE}. Vaut 1 au départ, 2 après trois ordres de grandeur, 4 après six.
     */
    public BigNum darkMatterResistance() {
        return state.darkMatterSize().divide(DARK_MATTER_START_SIZE).max(BigNum.ONE).pow(DARK_MATTER_RESISTANCE);
    }

    /**
     * Ce par quoi la taille de la matière noire est multipliée, en ce moment, à chaque seconde
     * d'appui : l'élan divisé par la résistance donne son taux de croissance. Plus elle est
     * grande, plus ce nombre se rapproche de 1.
     */
    public BigNum darkMatterGrowthPerSecond() {
        if (darkMatterEarned().isZero()) return BigNum.ONE;
        double rate = BigNum.of(darkMatterMomentum()).divide(darkMatterResistance()).toDouble();
        return BigNum.pow10(rate / Math.log(10));     // e^taux, même quand le taux est énorme
    }

    /** Taille de la matière noire en années-lumière : la distance qu'elle a « parcourue » en grossissant. */
    public BigNum darkMatterLightYears() {
        return state.darkMatterSize().divide(SizeScale.LIGHT_YEAR);
    }

    /**
     * Fait grossir la matière noire comme si le joueur la maintenait appuyée pendant {@code seconds}
     * secondes. L'interface l'appelle à chaque image tant que le clic est maintenu. La taille ne
     * diminue jamais, et aucune explosion ne la remet à zéro.
     *
     * @return {@code true} si la matière noire a grossi (il en faut au moins un peu pour cela)
     */
    public boolean growDarkMatter(double seconds) {
        boolean grown = expandDarkMatter(seconds);
        if (grown) state.stats().addHoldTime(seconds);
        return grown;
    }

    /**
     * La croissance elle-même, qu'elle vienne d'un appui du joueur ({@link #growDarkMatter(double)}),
     * de l'expansion spontanée ou de l'onde de fusion : seul l'appui compte dans les statistiques.
     */
    boolean expandDarkMatter(double seconds) {
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            throw new IllegalArgumentException("Durée invalide : " + seconds);
        }
        if (!isDarkMatterUnlocked() || darkMatterEarned().isZero() || seconds == 0) return false;
        // La résistance grandit avec la taille pendant l'appui. Le calcul est exact : la résistance
        // augmente de (puissance × élan) par seconde, et la taille s'en déduit. Un long appui donne
        // donc le même résultat que beaucoup de petits.
        BigNum resistance = darkMatterResistance()
                .add(BigNum.of(DARK_MATTER_RESISTANCE * darkMatterMomentum() * seconds));
        state.setDarkMatterSize(DARK_MATTER_START_SIZE.multiply(resistance.pow(1 / DARK_MATTER_RESISTANCE))
                .max(state.darkMatterSize()));
        return true;
    }

    // ------------------------------------------------------------------
    // Les automatismes de matière noire
    // ------------------------------------------------------------------

    /** Tous les automatismes de matière noire, dans l'ordre du catalogue. */
    public Collection<DarkAutomation> darkAutomations() {
        return darkAutomations.values();
    }

    /** Vrai quand le joueur a gagné assez de matière noire pour cet automatisme. Rien n'est dépensé. */
    public boolean isDarkAutomationUnlocked(String darkAutomationId) {
        DarkAutomation automation = darkAutomation(darkAutomationId);
        // Celui qui rachète l'arbre ne vient qu'après un premier Big Bang : avant, l'arbre se découvre à la main.
        if (automation.kind() == DarkAutomation.Kind.DARK_TREE && state.bigBangs() == 0) return false;
        return darkMatterEarned().gte(automation.darkMatter());
    }

    /** Vrai si cet automatisme de matière noire est débloqué et en marche. */
    public boolean isDarkAutomationEnabled(String darkAutomationId) {
        DarkAutomation automation = darkAutomation(darkAutomationId);
        return state.isDarkAutomationEnabled(automation.id()) && isDarkAutomationUnlocked(darkAutomationId)
                && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai si le défi de Big Bang en cours interdit les automatismes de matière noire et du troisième acte : leurs réglages restent, ils ne font rien. */
    public boolean isHandOnly() {
        return bangRuled(BangChallenge.BY_HAND);
    }

    /**
     * Met en marche ou coupe un automatisme de matière noire débloqué.
     *
     * @return {@code true} si le réglage a été pris en compte
     */
    public boolean setDarkAutomationEnabled(String darkAutomationId, boolean enabled) {
        if (!isDarkAutomationUnlocked(darkAutomationId)) return false;
        state.setDarkAutomationEnabled(darkAutomation(darkAutomationId).id(), enabled);
        return true;
    }

    /** Délai actuel entre deux actions de cet automatisme de matière noire, en secondes. */
    public double darkAutomationInterval(String darkAutomationId) {
        double interval = darkAutomation(darkAutomationId).interval();
        return ruled(Challenge.Rule.RUSTY_AUTOMATIONS) ? Math.max(RUSTY_MIN_INTERVAL, interval * RUSTY_FACTOR) : interval;
    }

    /** Fait agir les automatismes de matière noire en marche dont le délai est écoulé : une action par délai. */
    void runDarkAutomation(double dt) {
        for (DarkAutomation automation : darkAutomations.values()) {
            if (!isDarkAutomationEnabled(automation.id())) continue;
            double waited = state.darkAutomationTimer(automation.id()) + dt;
            double interval = darkAutomationInterval(automation.id());
            if (waited >= interval - 1e-9) {
                // Rien à faire pour l'instant : il reste prêt, et agira dès que ce sera possible.
                boolean acted = act(automation);
                if (acted) state.stats().noteDarkAutomationAction();
                waited = acted ? Math.max(0, waited - interval) : interval;
            }
            state.setDarkAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme de matière noire une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> grantAutomation();
            case ATOM_UPGRADES -> buyCheapestAtomUpgrade();
            case AUTOMATIONS -> buyCheapestAutomation();
            case EXPLOSION -> explode();
            case DARK_TREE -> buyCheapestDarkUpgrade();
        };
    }

    /**
     * Achète, dans l'ordre du catalogue, la première chose payable parmi : les cases de l'arbre de
     * matière noire et les améliorations payées en matière noire dont le nombre de niveaux est limité.
     * Une case sans dernier niveau est prise d'abord à son premier niveau, celui qui ouvre la case du
     * dessous ; ses niveaux suivants ne le sont que lorsqu'ils coûtent au plus 1 % de la branche
     * ({@link #TREE_REPEAT_SHARE}), pour ne pas avaler ce qu'il faut réunir pour le Big Bang. Les
     * améliorations de la boutique sans limite restent entièrement au joueur.
     */
    private boolean buyCheapestDarkUpgrade() {
        if (inChallenge()) return false;
        for (DarkUpgrade upgrade : darkUpgrades.values()) {
            if (!upgrade.hasLimit() && (!upgrade.inTree() || state.darkLevelOf(upgrade.id()) >= 1)) continue;
            // Les bifurcations restent au joueur : c'est lui qui choisit sa voie (version 0.69).
            if (upgrade.isFork()) continue;
            if (canBuyDark(upgrade.id()) && buyDark(upgrade.id())) return true;
        }
        // Les niveaux suivants des cases sans fin (Densité, Noyau lourd…), seulement quand ils ne coûtent
        // presque rien : au plus TREE_REPEAT_SHARE de ce que possède la branche. Leur prix grandit à chaque
        // niveau, si bien que l'automatisme n'entame jamais vraiment ce qu'il faut réunir pour le Big Bang.
        for (DarkUpgrade upgrade : darkUpgrades.values()) {
            if (upgrade.hasLimit() || !upgrade.branch().inTree() || state.darkLevelOf(upgrade.id()) < 1) continue;
            if (!canBuyDark(upgrade.id())) continue;
            BigNum cost = darkCostOf(upgrade.id());
            if (cost.lte(darkBalance(upgrade.branch()).multiply(TREE_REPEAT_SHARE)) && buyDark(upgrade.id())) return true;
        }
        return false;
    }

    /** Part de la branche au-delà de laquelle l'automatisme de l'arbre ne prend plus un niveau de case sans fin : 1 %. */
    public static final double TREE_REPEAT_SHARE = 0.01;

    /**
     * Offre au joueur un automatisme ordinaire qu'il ne possède pas encore, mis en marche
     * aussitôt : un par action, dans l'ordre du catalogue. Ceux qui achètent et fusionnent sont
     * offerts tout de suite. La synthèse automatique ne l'est qu'une fois le tableau périodique
     * ouvert et les autres à leur cadence maximale : avant, les atomes servent mieux ailleurs, et
     * elle ne doit pas les dépenser dans le dos du joueur. Elle n'attend pas d'élément unique.
     *
     * @return {@code true} si un automatisme a été offert
     */
    private boolean grantAutomation() {
        // Les défis gardent cet automatisme (version 0.47) : sans lui, chaque défi obligeait à racheter
        // les automatismes à la main. Les autres mémoires de la matière noire restent sans effet en défi.
        for (Automation automation : automations.values()) {
            if (state.ownsAutomation(automation.id())) continue;
            if (automation.kind() == Automation.Kind.SYNTHESIS
                    && !(isPeriodicTableUnlocked() && ordinaryAutomationsMaxed())) continue;
            state.addAutomation(automation.id());
            state.setAutomationEnabled(automation.id(), true);
            return true;
        }
        return false;
    }

    /** Vrai quand tous les automatismes ordinaires, hors synthèse, sont achetés et à leur cadence maximale. */
    private boolean ordinaryAutomationsMaxed() {
        for (Automation automation : automations.values()) {
            if (automation.kind() == Automation.Kind.SYNTHESIS) continue;
            if (!state.ownsAutomation(automation.id()) || !isAutomationMaxed(automation.id())) return false;
        }
        return true;
    }

    /**
     * Les améliorations payées en atomes dans l'ordre où l'automatisme les achète quand le joueur a
     * choisi le sien : son ordre, complété par celles qu'il n'a pas rangées, dans l'ordre du catalogue.
     */
    public List<Upgrade> atomUpgradeOrder() {
        List<Upgrade> ordered = new ArrayList<>();
        for (String id : state.atomUpgradeOrder()) {
            Upgrade upgrade = upgrades.get(id);
            if (upgrade != null && upgrade.resource() == Resource.ATOMS && !ordered.contains(upgrade)) ordered.add(upgrade);
        }
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() == Resource.ATOMS && !ordered.contains(upgrade)) ordered.add(upgrade);
        }
        return ordered;
    }

    /** Vrai si l'automatisme « Améliorations en atomes » suit l'ordre du joueur plutôt que « la moins chère d'abord ». */
    public boolean isAtomUpgradeOrdered() {
        return state.atomUpgradeOrdered();
    }

    public void setAtomUpgradeOrdered(boolean ordered) {
        state.setAtomUpgradeOrdered(ordered);
    }

    /**
     * Fait monter ou descendre une amélioration d'un rang dans l'ordre d'achat.
     *
     * @param direction −1 pour la faire monter (achetée plus tôt), +1 pour la faire descendre
     * @return vrai si elle a bougé ; faux au bout de la liste
     */
    public boolean moveAtomUpgrade(String upgradeId, int direction) {
        List<String> ids = new ArrayList<>();
        for (Upgrade upgrade : atomUpgradeOrder()) ids.add(upgrade.id());
        if (!moved(ids, upgrade(upgradeId).id(), direction)) return false;
        state.setAtomUpgradeOrder(ids);
        return true;
    }

    private static boolean moved(List<String> ids, String id, int direction) {
        int at = ids.indexOf(id);
        int to = at + Integer.signum(direction);
        if (at < 0 || to < 0 || to >= ids.size() || to == at) return false;
        java.util.Collections.swap(ids, at, to);
        return true;
    }

    /**
     * Dans l'ordre du joueur : la première amélioration qui n'est pas au maximum est la seule
     * achetée, et l'automatisme l'attend s'il le faut. Une amélioration dont le prix dépasse le
     * plafond d'atomes est sautée : l'attendre bloquerait tout.
     */
    private boolean buyNextAtomUpgrade() {
        for (Upgrade upgrade : atomUpgradeOrder()) {
            if (isMaxed(upgrade.id())) continue;
            if (!isAtomCapLifted() && costOf(upgrade.id()).gt(atomCap())) continue;
            return canBuy(upgrade.id()) && buy(upgrade.id());
        }
        return false;
    }

    /** Achète l'amélioration payée en atomes la moins chère que le joueur peut s'offrir, ou la suivante dans son ordre s'il en a choisi un. */
    private boolean buyCheapestAtomUpgrade() {
        if (state.atomUpgradeOrdered()) return buyNextAtomUpgrade();
        Upgrade cheapest = null;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() != Resource.ATOMS || !canBuy(upgrade.id())) continue;
            if (cheapest == null || costOf(upgrade.id()).lt(costOf(cheapest.id()))) cheapest = upgrade;
        }
        return cheapest != null && buy(cheapest.id());
    }

    /**
     * Les automatismes ordinaires dans l'ordre où l'automatisme les achète quand le joueur a choisi
     * le sien : son ordre, complété par ceux qu'il n'a pas rangés, dans l'ordre du catalogue.
     */
    public List<Automation> automationOrder() {
        List<Automation> ordered = new ArrayList<>();
        for (String id : state.automationOrder()) {
            Automation automation = automations.get(id);
            if (automation != null && !ordered.contains(automation)) ordered.add(automation);
        }
        for (Automation automation : automations.values()) {
            if (!ordered.contains(automation)) ordered.add(automation);
        }
        return ordered;
    }

    /** Vrai si l'automatisme « Achat des automatismes » suit l'ordre du joueur plutôt que « le moins cher d'abord ». */
    public boolean isAutomationOrdered() {
        return state.automationOrdered();
    }

    public void setAutomationOrdered(boolean ordered) {
        state.setAutomationOrdered(ordered);
    }

    /**
     * Fait monter ou descendre un automatisme d'un rang dans l'ordre d'achat.
     *
     * @param direction −1 pour le faire monter (acheté plus tôt), +1 pour le faire descendre
     * @return vrai s'il a bougé ; faux au bout de la liste
     */
    public boolean moveAutomation(String automationId, int direction) {
        List<String> ids = new ArrayList<>();
        for (Automation automation : automationOrder()) ids.add(automation.id());
        if (!moved(ids, automation(automationId).id(), direction)) return false;
        state.setAutomationOrder(ids);
        return true;
    }

    /**
     * Dans l'ordre du joueur : le premier automatisme qui n'est pas acheté, ou pas encore à sa
     * cadence maximale, est le seul servi, et l'automatisme l'attend s'il le faut. Un automatisme
     * pas encore débloqué, ou dont le prix dépasse le plafond d'atomes, est sauté.
     */
    private boolean buyNextAutomation() {
        for (Automation automation : automationOrder()) {
            String id = automation.id();
            if (!isAutomationAvailable(id)) continue;
            boolean owned = state.ownsAutomation(id);
            if (owned && isAutomationMaxed(id)) continue;
            BigNum cost = owned ? automationSpeedCost(id) : automation.cost();
            if (!isAtomCapLifted() && cost.gt(atomCap())) continue;
            if (!owned) return canBuyAutomation(id) && buyAutomation(id);
            return canSpeedUpAutomation(id) && speedUpAutomation(id);
        }
        return false;
    }

    /** Achète l'automatisme ordinaire, ou le niveau de cadence, le moins cher que le joueur peut s'offrir, ou le suivant dans son ordre s'il en a choisi un. */
    private boolean buyCheapestAutomation() {
        if (state.automationOrdered()) return buyNextAutomation();
        Automation cheapest = null;
        BigNum cheapestCost = null;
        for (Automation automation : automations.values()) {
            BigNum cost;
            if (canBuyAutomation(automation.id())) {
                cost = automation.cost();
            } else if (canSpeedUpAutomation(automation.id())) {
                cost = automationSpeedCost(automation.id());
            } else {
                continue;
            }
            if (cheapestCost == null || cost.lt(cheapestCost)) {
                cheapest = automation;
                cheapestCost = cost;
            }
        }
        if (cheapest == null) return false;
        return state.ownsAutomation(cheapest.id()) ? speedUpAutomation(cheapest.id()) : buyAutomation(cheapest.id());
    }
}
