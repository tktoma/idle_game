package idle.core;

import idle.core.math.BigNum;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L'état de la partie : les ordres d'achat, les favorites, l'espace, les défis, les succès,
 * les automatismes et l'arbre de matière noire, puis les remises à zéro.
 */
public abstract class GameStateResets extends GameStateCollider {

    /** Seul {@link GameState} prolonge la chaîne. */
    GameStateResets() {
    }


    public void setAutoMoleculeReserve(double share) {
        if (!(share >= 0 && share < 1)) throw new IllegalArgumentException("Part invalide : " + share);
        autoMoleculeReserve = share;
    }

    /** L'ordre d'achat choisi par le joueur pour les améliorations en atomes : des identifiants, du premier au dernier. */
    public List<String> atomUpgradeOrder() {
        return List.copyOf(atomUpgradeOrder);
    }

    public void setAtomUpgradeOrder(List<String> order) {
        atomUpgradeOrder.clear();
        atomUpgradeOrder.addAll(order);
    }

    /** Vrai si l'automatisme suit l'ordre du joueur plutôt que « le moins cher d'abord ». */
    public boolean atomUpgradeOrdered() {
        return atomUpgradeOrdered;
    }

    public void setAtomUpgradeOrdered(boolean ordered) {
        atomUpgradeOrdered = ordered;
    }

    /** L'ordre d'achat choisi par le joueur pour les automatismes ordinaires : des identifiants, du premier au dernier. */
    public List<String> automationOrder() {
        return List.copyOf(automationOrder);
    }

    public void setAutomationOrder(List<String> order) {
        automationOrder.clear();
        automationOrder.addAll(order);
    }

    /** Vrai si l'automatisme suit l'ordre du joueur plutôt que « le moins cher d'abord ». */
    public boolean automationOrdered() {
        return automationOrdered;
    }

    public void setAutomationOrdered(boolean ordered) {
        automationOrdered = ordered;
    }

    /** Vrai si le joueur a épinglé cette molécule. */
    public boolean isMoleculeFavorite(String moleculeId) {
        return favoriteMolecules.contains(moleculeId);
    }

    /** Épingle une molécule, ou la retire des favorites. Ni l'explosion ni le Big Bang ne changent ce choix. */
    public void setMoleculeFavorite(String moleculeId, boolean favorite) {
        if (moleculeId == null || moleculeId.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        if (favorite) favoriteMolecules.add(moleculeId);
        else favoriteMolecules.remove(moleculeId);
    }

    /** Vue en lecture seule des molécules épinglées, dans l'ordre où elles l'ont été. */
    public List<String> favoriteMolecules() {
        return List.copyOf(favoriteMolecules);
    }

    /** Vue en lecture seule des améliorations d'espace acquises, pour la sauvegarde. */
    public Set<String> spaceUpgrades() {
        return Set.copyOf(spaceUpgrades);
    }

    /** Nombre de sortes de molécules différentes créées. */
    public int moleculeKinds() {
        return molecules.size();
    }

    /** Change à chaque molécule ajoutée ou retirée : permet de ne recalculer ce qui en dépend que lorsqu'il le faut. */
    public int moleculesVersion() {
        return moleculesVersion;
    }

    /** Vrai si aucune molécule n'a été créée. */
    public boolean hasNoMolecule() {
        return molecules.isEmpty();
    }

    /** Espace : ce que l'expansion de la matière accumule avec le temps, depuis le premier Big Bang. */
    public BigNum space() {
        return space;
    }

    public void setSpace(BigNum space) {
        if (space.sign() < 0) throw new IllegalArgumentException("Espace négatif : " + space);
        this.space = space;
    }

    /** Nombre de Big Bangs déclenchés. Aucun Big Bang ne le remet à zéro. */
    public int bigBangs() {
        return bigBangs;
    }

    public void setBigBangs(int bigBangs) {
        if (bigBangs < 0) throw new IllegalArgumentException("Nombre de Big Bangs négatif : " + bigBangs);
        this.bigBangs = bigBangs;
    }

    /**
     * Nombre de générateurs que la fusion automatique attend avant de fusionner, choisi par le
     * joueur ; 0 tant qu'il n'a rien réglé. C'est un réglage : aucune explosion ne l'efface.
     */
    public int fusionThreshold() {
        return fusionThreshold;
    }

    public void setFusionThreshold(int fusionThreshold) {
        if (fusionThreshold < 0) throw new IllegalArgumentException("Seuil négatif");
        this.fusionThreshold = fusionThreshold;
    }

    /**
     * Atomes que la synthèse automatique laisse toujours au joueur. C'est un réglage : ni la
     * fusion ni l'explosion ne le changent.
     */
    public BigNum synthesisReserve() {
        return synthesisReserve;
    }

    public void setSynthesisReserve(BigNum synthesisReserve) {
        if (synthesisReserve.sign() < 0) throw new IllegalArgumentException("Réserve négative");
        this.synthesisReserve = synthesisReserve;
    }

    /** Vrai si l'appui sur la matière noire est verrouillé : elle grossit sans que le joueur tienne le clic. */
    public boolean holdLocked() {
        return holdLocked;
    }

    public void setHoldLocked(boolean holdLocked) {
        this.holdLocked = holdLocked;
    }

    /** Identifiant du défi en cours, ou {@code null} en partie ordinaire. */
    public String activeChallenge() {
        return activeChallenge;
    }

    public void setActiveChallenge(String activeChallenge) {
        this.activeChallenge = activeChallenge;
    }

    /** Vue en lecture seule des défis réussis. */
    public Set<String> completedChallenges() {
        return completedChallengesView;
    }

    public void addCompletedChallenge(String challengeId) {
        if (completedChallenges.add(challengeId)) challengesVersion++;
    }

    /** Version des défis réussis : change dès qu'un défi est ajouté ou que la liste est vidée. */
    public int challengesVersion() {
        return challengesVersion;
    }

    /** Meilleur temps de chaque défi réussi, en secondes de jeu (identifiant → durée de la partie). */
    public Map<String, Double> challengeTimes() {
        return Collections.unmodifiableMap(challengeTimes);
    }

    public void setChallengeTime(String challengeId, double seconds) {
        if (seconds < 0 || Double.isNaN(seconds)) throw new IllegalArgumentException("Durée invalide : " + seconds);
        challengeTimes.put(challengeId, seconds);
    }

    /** Vue en lecture seule des succès obtenus, dans l'ordre où ils l'ont été. Aucune explosion ne les reprend. */
    public Set<String> achievements() {
        return Collections.unmodifiableSet(achievements);
    }

    /** @return {@code true} si le succès vient d'être ajouté, faux s'il était déjà obtenu */
    public boolean addAchievement(String achievementId) {
        boolean added = achievements.add(achievementId);
        if (added) achievementsVersion++;
        return added;
    }

    /** Numéro de version des succès : il change dès qu'un succès est ajouté ou retiré. */
    public int achievementsVersion() {
        return achievementsVersion;
    }

    /** Fraction d'exemplaire qui attend de se désintégrer, pendant le défi de la désintégration. */
    public double decayDebt() {
        return decayDebt;
    }

    public void setDecayDebt(double decayDebt) {
        this.decayDebt = Math.max(0, decayDebt);
    }

    /** Vrai si le joueur a mis cet automatisme de matière noire en marche. Aucune explosion ne le coupe. */
    public boolean isDarkAutomationEnabled(String darkAutomationId) {
        return enabledDarkAutomations.contains(darkAutomationId);
    }

    public void setDarkAutomationEnabled(String darkAutomationId, boolean enabled) {
        if (enabled) {
            enabledDarkAutomations.add(darkAutomationId);
        } else {
            enabledDarkAutomations.remove(darkAutomationId);
        }
    }

    /** Vue en lecture seule des automatismes de matière noire en marche, pour la sauvegarde. */
    public Set<String> enabledDarkAutomations() {
        return Set.copyOf(enabledDarkAutomations);
    }

    /** Temps écoulé depuis la dernière action d'un automatisme de matière noire, en secondes. */
    public double darkAutomationTimer(String darkAutomationId) {
        return darkAutomationTimers.getOrDefault(darkAutomationId, 0.0);
    }

    public void setDarkAutomationTimer(String darkAutomationId, double seconds) {
        if (!(seconds >= 0)) throw new IllegalArgumentException("Durée invalide : " + seconds);
        darkAutomationTimers.put(darkAutomationId, seconds);
    }

    /** Niveau possédé pour une amélioration de matière noire (0 si jamais achetée). */
    public int darkLevelOf(String darkUpgradeId) {
        return darkUpgradeLevels.getOrDefault(darkUpgradeId, 0);
    }

    public void setDarkLevel(String darkUpgradeId, int level) {
        if (level < 0) throw new IllegalArgumentException("Niveau négatif");
        darkUpgradeLevels.put(darkUpgradeId, level);
        darkVersion++;
    }

    /** Version des niveaux de l'arbre : change dès qu'un niveau change. */
    public int darkVersion() {
        return darkVersion;
    }

    /** Vue en lecture seule des niveaux des améliorations de matière noire, pour la sauvegarde. */
    public Map<String, Integer> darkUpgradeLevels() {
        return Map.copyOf(darkUpgradeLevels);
    }

    /**
     * Efface absolument tout, matière noire, succès, Big Bangs et statistiques compris : l'état
     * redevient celui d'une partie jamais commencée.
     */
    public void reset() {
        clearMatter();
        clearDarkMatter();
        clearCollider();
        clearBigBangAct();
        fusionThreshold = 0;
        holdLocked = false;
        enabledDarkAutomations.clear();
        started = false;
        timePlayed = 0;
        challengeTimes.clear();
        achievements.clear();
        achievementsVersion++;
        logKinds.clear();
        autoHold = true;
        autoMolecules = true;
        autoFollowList = true;
        autoFollowMatter = false;
        automationPaused = false;
        autoGather = true;
        autoForm = true;
        autoMoleculeReserve = 0;
        explosionRecord = 0;
        cometsCaught = 0;
        skyEventsCaught = 0;
        endSeen = false;
        extraUniverses = 0;
        universeFormed = false;
        darkPlan.clear();
        java.util.Collections.fill(savedDarkPlans, "");
        systemPlan.clear();
        java.util.Collections.fill(savedSystemPlans, "");
        forkResetArmed = false;
        stateCaps.clear();
        recipeReservation = false;
        replaying = false;
        fusionsAtUniverse = 0;
        explosionsAtUniverse = 0;
        skinsOpen = false;
        unlockedSkins.clear();
        chosenSkins.clear();
        regimeOpen = false;
        regimeLeft = 0;
        regimeRest = 0;
        synergyOpen = false;
        perfectStreak = 0;
        perfectLeft = 0;
        cycleChoice = null;
        preferredFamily = null;
        preferenceDraws = 0;
        freeElementsUsed = 0;
        activeTrial = null;
        trialStart = 0;
        trialBase = 0;
        trialCount = 0;
        completedTrials.clear();
        seenPopups.clear();
        popupsPrimed = false;
        rebuildRecord = 0;
        rebuildPending = false;
        researches.clear();
        universeSeeds = BigNum.ZERO;
        voidEnergy = BigNum.ZERO;
        voidOpen = false;
        voidHoleOpen = false;
        voidShare = 0.25;
        voidPrice = 0;
        voidSacrificed = BigNum.ZERO;
        java.util.Arrays.fill(voidLevels, 0);
        voidHistory.clear();
        dimensionShards = 0;
        linePhase = 0;
        java.util.Arrays.fill(lineLevels, 0);
        dimensionShardsEarned = 0;
        seedSprouted = false;
        wateringCan = false;
        twigOpened = false;
        seedUniversesOpened = 0;
        seedWaterings = 0;
        planeShards = 0;
        planeShardsEarned = 0;
        java.util.Arrays.fill(planeLevels, 0);
        volumeShards = 0;
        volumeShardsEarned = 0;
        java.util.Arrays.fill(volumeLevels, 0);
        durationShards = 0;
        durationShardsEarned = 0;
        java.util.Arrays.fill(durationLevels, 0);
        loopShards = 0;
        loopShardsEarned = 0;
        java.util.Arrays.fill(loopLevels, 0);
        braneShards = 0;
        braneShardsEarned = 0;
        java.util.Arrays.fill(braneLevels, 0);
        java.util.Arrays.fill(seedUpgradeLevels, 0);
        chronoStored = 0;
        chronoSpeed = 1;
        java.util.Arrays.fill(treeBubbleLevels, 0);
        hadronsOpen = false;
        dimensionAutomations.clear();
        dimensionAutomationsOff.clear();
        hadronsPrice = 0;
        hadrons.clear();
        voidSpent = 0;
        voidTrickled = 0;
        voidCarried = 0;
        universeUpgrades.clear();
        pulseCharges = 0;
        pulses = 0;
        probeLaunches = 0;
        probeSamples = 0;
        bangChallengeTimes.clear();
        atomUpgradeOrder.clear();
        atomUpgradeOrdered = false;
        automationOrder.clear();
        automationOrdered = false;
        stats = new GameStats();
    }

    /**
     * Efface tout ce qui vient de l'explosion : la matière noire, gagnée comme dépensée, sa taille,
     * son arbre, la masse du tableau, le nombre d'explosions et les défis réussis. Les réglages du
     * joueur restent, pour resservir dès que l'arbre les rouvre : les automatismes de matière noire
     * qu'il avait mis en marche, le verrou de l'appui, le seuil de fusion. Seule la réserve de la
     * synthèse automatique repart de zéro : sous le plafond d'atomes revenu, elle la bloquerait.
     * Les records des défis restent, comme
     * les succès, le temps de jeu et les statistiques, et ce qui appartient à l'acte du Big Bang :
     * les molécules, les substances, les assemblages, les astres, l'espace et ses améliorations. C'est ce que fait un Big Bang, en plus de
     * {@link #clearMatter()}.
     */
    public void clearDarkMatter() {
        darkMatter = BigNum.ZERO;
        darkMatterSpent = BigNum.ZERO;
        darkMatterSize = Game.DARK_MATTER_START_SIZE;
        explosions = 0;
        tableWeightLevel = 0;
        darkUpgradeLevels.clear();
        darkVersion++;
        synthesisReserve = BigNum.ZERO;
        activeChallenge = null;
        completedChallenges.clear();
        challengesVersion++;
        darkAutomationTimers.clear();
    }

    /**
     * Comme {@link #clearDarkMatter()}, mais l'arbre reste : ses cases, la matière noire qu'elles
     * ont coûté, ses automatismes et les réglages qu'il avait ouverts. Partent la réserve, la
     * taille, la masse du tableau, le nombre d'explosions et les défis réussis. C'est ce que fait
     * un Big Bang une fois atteint le palier qui garde l'arbre.
     */
    public void clearDarkMatterKeepingTree() {
        darkMatter = BigNum.ZERO;
        darkMatterSize = Game.DARK_MATTER_START_SIZE;
        explosions = 0;
        tableWeightLevel = 0;
        activeChallenge = null;
        completedChallenges.clear();
        challengesVersion++;
        darkAutomationTimers.clear();
    }

    /**
     * Efface toute la matière : particules, atomes, améliorations, automatismes et éléments.
     * Il ne reste que le premier générateur, la matière noire, sa taille et ses améliorations,
     * le nombre d'explosions et le temps de jeu.
     */
    public void clearMatter() {
        // Le deuxième univers (version 0.087) : ce qui suit le tableau repart avec lui.
        preferenceDraws = 0;
        freeElementsUsed = 0;
        perfectStreak = 0;
        particles = BigNum.ZERO;
        atoms = BigNum.ZERO;
        totalAtoms = BigNum.ZERO;
        timeSinceFusion = 0;
        upgradeCalm = 0;
        bangCalm = 0;
        formations.clear();
        upgradeLevels.clear();
        ownedAutomations.clear();
        enabledAutomations.clear();
        automationSpeedLevels.clear();
        automationTimers.clear();
        elements.clear();
        elementsVersion++;
        synthesisCount = 0;
        synthesisTarget = null;
        synthesisTries = 0;
        drySyntheses = 0;
        decayDebt = 0;
    }

    /**
     * Efface tout le collisionneur : son énergie, produite comme dépensée, ses générateurs, ses
     * améliorations et leurs pauses, les quarks, les collisions en cours, le quark top et la
     * surchauffe. Rien d'autre ne bouge. Aucune étape du jeu ne le fait seul (le collisionneur
     * traverse les actes) : c'est un outil de test, et une part de {@link #reset()}.
     */
    public void clearCollider() {
        colliderEnergy = 0;
        colliderEnergyProduced = 0;
        colliderGenerators.clear();
        colliderGeneratorsBought.clear();
        colliderPhase = 0;
        colliderCrossings = 0;
        colliderCaught = -1;
        colliderUpgrades.clear();
        colliderPaused.clear();
        quarks.clear();
        collisions.clear();
        collisionRuns = 0;
        topLeft = 0;
        overheatLeft = 0;
    }

    /**
     * Efface l'acte du Big Bang : les Big Bangs, les molécules et leurs automatismes, les
     * substances, les assemblages, les astres, les systèmes solaires, l'espace et ses
     * améliorations, le cosmos, les comètes, les sondes et les défis du Big Bang. La matière, la
     * matière noire et le collisionneur restent ; les succès et les records aussi. Un outil de
     * test, et une part de {@link #reset()} : aucune étape du jeu ne revient en arrière ainsi.
     */
    public void clearBigBangAct() {
        bigBangs = 0;
        plannedBodies.clear();
        molecules.clear();
        substances.clear();
        assemblies.clear();
        bodies.clear();
        maturing.clear();
        solarSystems.clear();
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        cosmos = 0;
        automatedMolecules.clear();
        spaceUpgrades.clear();
        favoriteMolecules.clear();
        cometWait = 0;
        cometVisible = 0;
        cometBoost = 0;
        skyEvent = null;
        skyEventVisible = 0;
        skyEventWait = 0;
        windLeft = 0;
        probes.clear();
        probeRewards.clear();
        probeUpgrades.clear();
        probeRuns = 0;
        probeBoostLeft = 0;
        activeBangChallenge = null;
        completedBangChallenges.clear();
        boughtMilestones.clear();
        bangChallengeStarted = 0;
        moleculesVersion++;
        space = BigNum.ZERO;
    }
}
