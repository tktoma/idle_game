package idle.core.state;

import idle.core.*;
import idle.core.collider.*;
import idle.core.math.BigNum;
import idle.core.probes.*;
import java.util.*;

/** Une partie de {@link GameStateCollider} (découpée en 0.293 pour rester sous 1 000 lignes) : les champs du collisionneur, du vide et des univers, et ce que la formation d'un univers garde. */
public abstract class GameStateColliderBase extends GameStateUnlocks {
    // Ce que fournissent les parties suivantes.
    abstract public boolean treeBubbleBought(int index);

    /** Seul {@link GameState} prolonge la chaîne. */
    GameStateColliderBase() {
    }

    // Ce que fournissent les parties suivantes.
    public abstract void reset();

    /** Vrai si cette amélioration d'espace est acquise. Ni l'explosion ni le Big Bang ne la reprennent. */
    public boolean ownsSpaceUpgrade(String spaceUpgradeId) { return spaceUpgrades.contains(spaceUpgradeId); }

    /** Note une amélioration d'espace comme acquise. */
    public void addSpaceUpgrade(String spaceUpgradeId) {
        spaceUpgrades.add(spaceUpgradeId);
        moleculesVersion++;
    }

    /** Nombre d'améliorations d'espace acquises. */
    public int spaceUpgradeCount() { return spaceUpgrades.size(); }

    /**
     * Vrai tant que tous les automatismes sont coupés d'un geste ({@link Game#setAutomationPaused(boolean)}). C'est un
     * réglage : aucune remise à zéro de la partie ne le change, seule une nouvelle partie ({@link PlayerSettings}).
     */
    public boolean automationPaused() { return automationPaused; }

    public void setAutomationPaused(boolean paused) { automationPaused = paused; }

    /** Vrai si le rassemblement automatique est en marche (il ne sert qu'une fois acquis). Un réglage : aucune remise à zéro ne le coupe ({@link PlayerSettings}). */
    public boolean autoGather() { return autoGather; }

    public void setAutoGather(boolean enabled) { autoGather = enabled && !testBlocked("gather"); }

    /** Vrai si la formation automatique est en marche (elle ne sert qu'une fois acquise). Un réglage : aucune remise à zéro ne la coupe ({@link PlayerSettings}). */
    public boolean autoForm() { return autoForm; }

    public void setAutoForm(boolean enabled) { autoForm = enabled && !testBlocked("form"); }

    /** Part de l'espace créé que la création automatique laisse toujours libre, de 0 à 1. C'est un réglage. */
    public double autoMoleculeReserve() { return autoMoleculeReserve; }

    /** Durée de l'explosion qui sert de record depuis le dernier Big Bang, en secondes ; 0 tant qu'aucune n'a eu lieu. */
    public double explosionRecord() { return explosionRecord; }

    public void setExplosionRecord(double seconds) {
        explosionRecord = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes de jeu avant la prochaine comète ; 0 tant qu'aucune n'est attendue. */
    public double cometWait() { return cometWait; }

    public void setCometWait(double seconds) { cometWait = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    /** Secondes pendant lesquelles la comète reste encore à l'écran ; 0 quand il n'y en a pas. */
    public double cometVisible() { return cometVisible; }

    public void setCometVisible(double seconds) { cometVisible = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    /** Secondes pendant lesquelles le sillage de la dernière comète saisie accélère encore l'expansion. */
    public double cometBoost() { return cometBoost; }

    public void setCometBoost(double seconds) { cometBoost = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    /** Énergie du collisionneur disponible, à dépenser en générateurs et en améliorations. */
    public double colliderEnergy() { return colliderEnergy; }

    public void setColliderEnergy(double energy) { colliderEnergy = Double.isNaN(energy) ? 0 : Math.max(0, energy); }

    public double colliderGenerators(ColliderGenerator generator) {
        return colliderGenerators.getOrDefault(generator, 0.0);
    }

    public void setColliderGenerators(ColliderGenerator generator, double amount) {
        if (Double.isNaN(amount) || amount <= 0) colliderGenerators.remove(generator);
        else colliderGenerators.put(generator, amount);
    }

    public int colliderGeneratorsBought(ColliderGenerator generator) {
        return colliderGeneratorsBought.getOrDefault(generator, 0);
    }

    public void setColliderGeneratorsBought(ColliderGenerator generator, int bought) {
        if (bought <= 0) colliderGeneratorsBought.remove(generator);
        else colliderGeneratorsBought.put(generator, bought);
    }

    /** La phase des faisceaux, de 0 à 1 : ils partent ensemble de l'injecteur à 0 et se croisent à 0,5. */
    public double colliderPhase() { return colliderPhase; }

    public void setColliderPhase(double phase) { colliderPhase = Double.isNaN(phase) ? 0 : phase - Math.floor(phase); }

    /** Nombre de croisements des faisceaux depuis le début. */
    public long colliderCrossings() { return colliderCrossings; }

    public void setColliderCrossings(long count) { colliderCrossings = Math.max(0, count); }

    /** Le numéro du dernier croisement capté d'un clic ; −1 avant le premier. */
    public long colliderCaught() { return colliderCaught; }

    public void setColliderCaught(long crossing) { colliderCaught = crossing; }

    public int colliderUpgradeLevel(ColliderUpgrade upgrade) { return colliderUpgrades.getOrDefault(upgrade, 0); }

    public void setColliderUpgradeLevel(ColliderUpgrade upgrade, int level) {
        if (level <= 0) colliderUpgrades.remove(upgrade);
        else colliderUpgrades.put(upgrade, Math.min(level, upgrade.maxLevel()));
    }

    /** Vrai si le joueur a mis cet automate en pause. */
    public boolean isColliderPaused(ColliderUpgrade upgrade) { return colliderPaused.contains(upgrade); }

    public void setColliderPaused(ColliderUpgrade upgrade, boolean paused) {
        if (paused) colliderPaused.add(upgrade);
        else colliderPaused.remove(upgrade);
    }

    public int quarks(Quark quark) { return quarks.getOrDefault(quark, 0); }

    public void setQuarks(Quark quark, int count) {
        if (count <= 0) quarks.remove(quark);
        else quarks.put(quark, count);
    }

    public double colliderEnergyProduced() { return colliderEnergyProduced; }

    public void setColliderEnergyProduced(double energy) {
        colliderEnergyProduced = Double.isNaN(energy) ? 0 : Math.max(0, energy);
    }

    /** Les collisions en cours, dans l'ordre de leur lancement. */
    public List<Collision> collisions() { return Collections.unmodifiableList(collisions); }

    public void setCollisions(List<Collision> running) {
        collisions.clear();
        collisions.addAll(running);
    }

    public long collisionRuns() { return collisionRuns; }

    public void setCollisionRuns(long count) { collisionRuns = Math.max(0, count); }

    /** Secondes avant que le quark top ne se désintègre ; 0 s'il n'y en a pas. */
    public double topLeft() { return topLeft; }

    public void setTopLeft(double seconds) { topLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    /** Secondes de surchauffe qu'il reste. */
    public double overheatLeft() { return overheatLeft; }

    public void setOverheatLeft(double seconds) { overheatLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    /** Les sondes en vol, dans l'ordre de leur départ. */
    public List<Probe> probes() { return Collections.unmodifiableList(probes); }

    public void setProbes(List<Probe> flying) {
        probes.clear();
        probes.addAll(flying);
    }

    /** Les récompenses rapportées, qui attendent d'être réclamées, la prochaine en tête. */
    public List<ProbeReward> probeRewards() { return Collections.unmodifiableList(probeRewards); }

    public void setProbeRewards(List<ProbeReward> waiting) {
        probeRewards.clear();
        probeRewards.addAll(waiting);
    }

    public int probeUpgradeLevel(ProbeUpgrade upgrade) { return probeUpgrades.getOrDefault(upgrade, 0); }

    public void setProbeUpgradeLevel(ProbeUpgrade upgrade, int level) {
        if (level <= 0) probeUpgrades.remove(upgrade);
        else probeUpgrades.put(upgrade, Math.min(level, upgrade.maxLevel()));
    }

    /** Nombre de voyages terminés depuis le début : il fixe le butin de chacun, sans toucher au hasard du jeu. */
    public long probeRuns() { return probeRuns; }

    public void setProbeRuns(long runs) { probeRuns = Math.max(0, runs); }

    public long probeLaunches() { return probeLaunches; }

    public void setProbeLaunches(long launches) { probeLaunches = Math.max(0, launches); }

    public long probeSamples() { return probeSamples; }

    public void setProbeSamples(long samples) { probeSamples = Math.max(0, samples); }

    /** Secondes de sursaut d'expansion qu'il reste. */
    public double probeBoostLeft() { return probeBoostLeft; }

    public void setProbeBoostLeft(double seconds) { probeBoostLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    public double pulseCharges() { return pulseCharges; }

    public void setPulseCharges(double charges) { pulseCharges = Double.isNaN(charges) ? 0 : Math.max(0, charges); }

    public long pulses() { return pulses; }

    public void setPulses(long count) { pulses = Math.max(0, count); }

    /** Nombre de comètes saisies depuis le début du jeu. */
    public int cometsCaught() { return cometsCaught; }

    /** Vrai une fois l'écran de fin vu : il ne s'ouvre qu'une fois par partie. */
    public boolean endSeen() { return endSeen; }

    public void setEndSeen(boolean seen) { endSeen = seen; }

    /**
     * Vrai une fois le premier univers formé : il reste formé après la remise à
     * zéro qui l'accompagne ({@link #startUniverse()}), quand l'échelle du cosmos repart de rien.
     */
    public boolean universeFormed() { return universeFormed; }

    public void setUniverseFormed(boolean formed) { universeFormed = formed; }

    /**
     * Le premier univers se forme : tout ce qui y a mené est effacé, comme pour une nouvelle partie ({@link #reset()}).
     * Restent les succès, le temps de jeu, les statistiques, l'écran de fin déjà vu, et tout ce qui appartient à
     * l'onglet Univers : les univers, leurs graines, l'énergie du vide et les quatre améliorations. La partie reste
     * commencée, pour que l'écran de démarrage ne revienne pas. Depuis la version 0.126, les réglages du joueur restent
     * aussi ({@link PlayerSettings}) : seule une nouvelle partie lancée par le joueur les efface.
     */
    public void startUniverse() {
        PlayerSettings settings = new PlayerSettings((GameStateCollider) this);
        Set<String> keptAchievements = new LinkedHashSet<>(achievements);
        double played = timePlayed;
        GameStats keptStats = stats;
        boolean seen = endSeen;
        // La fin de la partie reste notée.
        boolean finished = gameFinished;
        int extra = extraUniverses;
        BigNum seeds = universeSeeds;
        // Le vide repart de zéro à chaque univers : seul son historique reste.
        Map<String, Double> voidKept = new LinkedHashMap<>(voidHistory);
        double shards = dimensionShards, phase = linePhase;
        int[] lineKeptLevels = lineLevels.clone();
        double earned = dimensionShardsEarned;
        boolean sprouted = seedSprouted;
        long waterings = seedWaterings;
        boolean can = wateringCan;
        boolean twig = twigOpened;
        int seedOpened = seedUniversesOpened;
        double planeKept = planeShards, planeEarned = planeShardsEarned;
        int[] planeKeptLevels = planeLevels.clone();
        double volumeKept = volumeShards, volumeEarned = volumeShardsEarned;
        int[] volumeKeptLevels = volumeLevels.clone();
        double durationKept = durationShards, durationEarned = durationShardsEarned;
        int[] durationKeptLevels = durationLevels.clone();
        // La boucle aussi.
        double loopKept = loopShards, loopEarned = loopShardsEarned;
        int[] loopKeptLevels = loopLevels.clone();
        // La brane aussi.
        double braneKept = braneShards, braneEarned = braneShardsEarned;
        int[] braneKeptLevels = braneLevels.clone();
        // Le clicker aussi.
        double clickerKept = clickerShards, clickerEarned = clickerShardsEarned;
        int[] clickerKeptLevels = clickerLevels.clone();
        // La grille aussi.
        double gridKept = gridShards, gridEarned = gridShardsEarned;
        int[] gridKeptLevels = gridLevels.clone();
        // Le miroir aussi.
        double mirrorKept = mirrorShards, mirrorEarned = mirrorShardsEarned;
        int[] mirrorKeptLevels = mirrorLevels.clone();
        double mirrorWinsKept = mirrorWins;
        // Le mélange aussi.
        double mixKept = mixShards, mixEarned = mixShardsEarned, mixStageEarnedKept = mixStageEarned;
        int mixStageKept = mixStage;
        int[] mixKeptLevels = mixLevels.clone();
        // Les améliorations de la graine et le chrono traversent les univers, comme la graine.
        int[] seedKeptLevels = seedUpgradeLevels.clone();
        double chronoKept = chronoStored, chronoSpeedKept = chronoSpeed;
        // Les bulles de l'arbre et l'énergie du vide dépensée aussi, comme le cumul du vide.
        int[] bubblesKept = treeBubbleLevels.clone();
        // L'automatisation des dimensions traverse les univers, comme les dimensions.
        Set<String> dimensionAutomationsKept = new TreeSet<>(dimensionAutomations);
        Set<String> dimensionAutomationsOffKept = new TreeSet<>(dimensionAutomationsOff);
        double voidSpentKept = voidSpent, trickledKept = voidTrickled, carriedKept = voidCarried;
        List<String> treeCodes = new ArrayList<>(savedDarkPlans), systemsCodes = new ArrayList<>(savedSystemPlans);
        // Les skins sont des apparences : ils restent d'un univers à l'autre.
        boolean skins = skinsOpen;
        Set<String> skinsUnlocked = new TreeSet<>(unlockedSkins);
        Map<String, String> skinsChosen = new TreeMap<>(chosenSkins);
        Set<String> trialsDone = new TreeSet<>(completedTrials);
        Set<String> popups = new TreeSet<>(seenPopups);
        boolean popupsReady = popupsPrimed;
        Set<String> researchesDone = new TreeSet<>(researches);
        double rebuild = rebuildRecord;
        long fusions = stats.fusions();
        long explosionsDone = stats.timedExplosions();
        // Les bulles de l'arbre qui font traverser les univers : le collisionneur, le vide, la matière
        // noire et ses défis, les défis du Big Bang.
        GameStateCollider kept = copyForBubbles();
        reset();
        // Les plans gardés par « Sauvegarder » servent d'une partie à l'autre.
        for (int i = 0; i < PLAN_SLOTS; i++) {
            savedDarkPlans.set(i, treeCodes.get(i));
            savedSystemPlans.set(i, systemsCodes.get(i));
        }
        skinsOpen = skins;
        unlockedSkins.addAll(skinsUnlocked);
        skinsVersion++;
        chosenSkins.putAll(skinsChosen);
        fusionsAtUniverse = fusions;
        explosionsAtUniverse = explosionsDone;
        // Les épreuves réussies, les recherches et le record de reconstruction restent aussi.
        completedTrials.addAll(trialsDone);
        researches.addAll(researchesDone);
        // Les fenêtres d'explication déjà vues ne reviennent pas.
        seenPopups.addAll(popups);
        popupsPrimed = popupsReady;
        rebuildRecord = rebuild;
        started = true;
        timePlayed = played;
        stats = keptStats;
        // Une nouvelle partie commence : ses étapes, ses délais et ses records se comptent d'ici.
        stats.startUniverseSteps(played);
        endSeen = seen;
        gameFinished = finished;
        achievements.addAll(keptAchievements);
        achievementsVersion++;
        universeFormed = true;
        extraUniverses = extra;
        universeSeeds = seeds;
        voidHistory.putAll(voidKept);
        // La ligne appartient à la partie secondaire : elle traverse les univers.
        dimensionShards = shards;
        linePhase = phase;
        System.arraycopy(lineKeptLevels, 0, lineLevels, 0, LINE_UPGRADES);
        dimensionShardsEarned = earned;
        seedSprouted = sprouted;
        seedWaterings = waterings;
        wateringCan = can;
        twigOpened = twig;
        seedUniversesOpened = seedOpened;
        planeShards = planeKept;
        planeShardsEarned = planeEarned;
        System.arraycopy(planeKeptLevels, 0, planeLevels, 0, PLANE_UPGRADES);
        // Le volume traverse les univers, comme la ligne et le plan.
        volumeShards = volumeKept;
        volumeShardsEarned = volumeEarned;
        System.arraycopy(volumeKeptLevels, 0, volumeLevels, 0, VOLUME_UPGRADES);
        // La durée aussi.
        durationShards = durationKept;
        durationShardsEarned = durationEarned;
        System.arraycopy(durationKeptLevels, 0, durationLevels, 0, DURATION_UPGRADES);
        loopShards = loopKept;
        loopShardsEarned = loopEarned;
        System.arraycopy(loopKeptLevels, 0, loopLevels, 0, LOOP_UPGRADES);
        braneShards = braneKept;
        braneShardsEarned = braneEarned;
        System.arraycopy(braneKeptLevels, 0, braneLevels, 0, BRANE_UPGRADES);
        clickerShards = clickerKept;
        clickerShardsEarned = clickerEarned;
        System.arraycopy(clickerKeptLevels, 0, clickerLevels, 0, CLICKER_UPGRADES);
        gridShards = gridKept;
        gridShardsEarned = gridEarned;
        System.arraycopy(gridKeptLevels, 0, gridLevels, 0, GRID_UPGRADES);
        mirrorShards = mirrorKept;
        mirrorShardsEarned = mirrorEarned;
        System.arraycopy(mirrorKeptLevels, 0, mirrorLevels, 0, MIRROR_UPGRADES);
        mirrorWins = mirrorWinsKept;
        mixShards = mixKept;
        mixShardsEarned = mixEarned;
        mixStage = mixStageKept;
        mixStageEarned = mixStageEarnedKept;
        System.arraycopy(mixKeptLevels, 0, mixLevels, 0, MIX_UPGRADES);
        System.arraycopy(seedKeptLevels, 0, seedUpgradeLevels, 0, SEED_UPGRADES);
        chronoStored = chronoKept;
        chronoSpeed = chronoSpeedKept;
        System.arraycopy(bubblesKept, 0, treeBubbleLevels, 0, TREE_BUBBLES);
        dimensionAutomations.addAll(dimensionAutomationsKept);
        dimensionAutomationsOff.addAll(dimensionAutomationsOffKept);
        voidSpent = voidSpentKept;
        voidTrickled = trickledKept;
        voidCarried = carriedKept;
        settings.restore((GameStateCollider) this);
        restoreForBubbles(kept);
    }

    /**
     * La formation de l'univers : comme {@link #startUniverse()}, mais ce que toutes les bulles qui gardent
     * pourraient garder est d'abord mis de côté, avec les bulles déjà achetées. Tant que le joueur n'a pas lancé l'univers
     * suivant ({@link #startUniverseKeeping()}), une bulle achetée garde encore sa part.
     */
    public void startUniverseHolding() {
        GameState held = new GameState();
        ((GameStateCollider) held).restoreForBubbles((GameStateCollider) this, true);
        int mask = 0;
        for (GameUniverse.TreeBubble bubble : KEEPING_BUBBLES) if (treeBubbleBought(bubble.ordinal())) mask |= 1 << bubble.ordinal();
        startUniverse();
        bubbleKeep = held;
        bubbleKeepMask = mask;
    }

    /**
     * Le lancement de l'univers suivant : comme {@link #startUniverse()}, puis chaque bulle qui garde,
     * achetée depuis la formation, rend la part mise de côté à la formation. Celles achetées avant gardent ce qu'elles
     * ont déjà gardé, avec ce qui s'y est ajouté depuis.
     */
    public void startUniverseKeeping() {
        GameState held = bubbleKeep;
        int mask = bubbleKeepMask;
        startUniverse();
        bubbleKeep = null;
        bubbleKeepMask = 0;
        if (held == null) return;
        for (GameUniverse.TreeBubble bubble : KEEPING_BUBBLES) {
            if (!treeBubbleBought(bubble.ordinal()) || (mask & (1 << bubble.ordinal())) != 0) continue;
            restorePart(bubble, held);
            // L'énergie du vide rendue est déjà au cumul depuis la formation : la suivante ne la recomptera pas.
            if (bubble == GameUniverse.TreeBubble.GIANT_LEFT_LOW) voidCarried = voidEnergy.toDouble();
        }
    }

    /** Vrai entre la formation de l'univers et le lancement du suivant : ce que les bulles peuvent garder attend. */
    public boolean isHoldingForBubbles() { return bubbleKeep != null; }

    /** Une copie de ce que les bulles achetées font traverser les univers ; {@code null} si aucune ne le fait. */
    GameStateCollider copyForBubbles() {
        boolean any = false;
        for (GameUniverse.TreeBubble bubble : KEEPING_BUBBLES) any |= treeBubbleBought(bubble.ordinal());
        if (!any) return null;
        GameStateCollider copy = new GameState();
        copy.restoreForBubbles((GameStateCollider) this, true);
        return copy;
    }

    /** Les bulles qui gardent quelque chose d'un univers à l'autre. */
    static final List<GameUniverse.TreeBubble> KEEPING_BUBBLES = List.of(GameUniverse.TreeBubble.MATURE_LEFT,
            GameUniverse.TreeBubble.GIANT_LEFT_LOW, GameUniverse.TreeBubble.MATURE_RIGHT, GameUniverse.TreeBubble.GIANT_RIGHT_LOW,
            GameUniverse.TreeBubble.ANCIENT_LEFT);

    void restoreForBubbles(GameStateCollider kept) { if (kept != null) restoreForBubbles(kept, false); }

    /**
     * Recopie de {@code from} ce que les bulles achetées gardent : toutes si {@code all} (pour faire la copie), sinon
     * celles que ce nouvel univers a encore achetées (les bulles traversent les univers : ce sont les mêmes).
     */
    void restoreForBubbles(GameStateCollider from, boolean all) {
        for (GameUniverse.TreeBubble bubble : KEEPING_BUBBLES) {
            if (all || treeBubbleBought(bubble.ordinal())) restorePart(bubble, from);
        }
    }

    /** Recopie de {@code from} la part que cette bulle garde (version 0.169 : une bulle à la fois). */
    void restorePart(GameUniverse.TreeBubble bubble, GameStateCollider from) {
        if (bubble == GameUniverse.TreeBubble.MATURE_LEFT) {
            // L'arbre mûr, à gauche : tout le collisionneur.
            colliderEnergy = from.colliderEnergy;
            colliderEnergyProduced = from.colliderEnergyProduced;
            colliderGenerators.clear();
            colliderGenerators.putAll(from.colliderGenerators);
            colliderGeneratorsBought.clear();
            colliderGeneratorsBought.putAll(from.colliderGeneratorsBought);
            colliderPhase = from.colliderPhase;
            colliderCrossings = from.colliderCrossings;
            colliderCaught = from.colliderCaught;
            colliderUpgrades.clear();
            colliderUpgrades.putAll(from.colliderUpgrades);
            colliderPaused.clear();
            colliderPaused.addAll(from.colliderPaused);
            quarks.clear();
            quarks.putAll(from.quarks);
            collisions.clear();
            collisions.addAll(from.collisions);
            collisionRuns = from.collisionRuns;
            topLeft = from.topLeft;
            overheatLeft = from.overheatLeft;
        }
        if (bubble == GameUniverse.TreeBubble.GIANT_LEFT_LOW) {
            // Le géant de gauche, en bas : le vide, ouvert, son énergie, son trou noir et ses améliorations.
            voidEnergy = from.voidEnergy;
            voidOpen = from.voidOpen;
            voidHoleOpen = from.voidHoleOpen;
            voidShare = from.voidShare;
            voidPrice = from.voidPrice;
            voidSacrificed = from.voidSacrificed;
            System.arraycopy(from.voidLevels, 0, voidLevels, 0, voidLevels.length);
        }
        if (bubble == GameUniverse.TreeBubble.MATURE_RIGHT) {
            // L'arbre mûr, à droite : la matière noire, sa taille, son arbre, les explosions et les défis réussis.
            darkMatter = from.darkMatter;
            darkMatterSpent = from.darkMatterSpent;
            darkMatterSize = from.darkMatterSize;
            darkMatterReach = from.darkMatterReach;
            explosions = from.explosions;
            tableWeightLevel = from.tableWeightLevel;
            darkUpgradeLevels.clear();
            darkUpgradeLevels.putAll(from.darkUpgradeLevels);
            darkVersion++;
            completedChallenges.clear();
            completedChallenges.addAll(from.completedChallenges);
            challengesVersion++;
            enabledDarkAutomations.clear();
            enabledDarkAutomations.addAll(from.enabledDarkAutomations);
        }
        if (bubble == GameUniverse.TreeBubble.ANCIENT_LEFT) {
            // L'arbre ancien, à gauche : les hadrons, et l'onglet ouvert.
            hadronsOpen = from.hadronsOpen;
            hadronsPrice = from.hadronsPrice;
            hadrons.clear();
            hadrons.putAll(from.hadrons);
        }
        if (bubble == GameUniverse.TreeBubble.GIANT_RIGHT_LOW) {
            // Le géant de droite, en bas : les défis du Big Bang réussis.
            completedBangChallenges.clear();
            completedBangChallenges.addAll(from.completedBangChallenges);
            moleculesVersion++;
        }
    }

    /**
     * Les réglages du joueur : des choix sur la façon de jouer, pas de l'avancée. Ni la fusion, ni
     * l'explosion, ni le Big Bang, ni aucune remise à zéro de la partie ne les changent ; seule une nouvelle partie
     * lancée par le joueur ({@link #reset()}) les remet comme au premier lancement.
     */
    record PlayerSettings(boolean automationPaused, boolean autoGather, boolean autoForm, boolean autoHold,
                                  boolean autoMolecules,
                                  double autoMoleculeReserve, int fusionThreshold, boolean holdLocked,
                                  List<String> automatedMolecules, List<String> favoriteMolecules,
                                  Map<String, Double> stateCaps,
                                  List<String> atomUpgradeOrder, boolean atomUpgradeOrdered,
                                  List<String> automationOrder, boolean automationOrdered,
                                  List<String> enabledDarkAutomations, EnumSet<ColliderUpgrade> colliderPaused) {
        PlayerSettings(GameStateCollider state) {
            this(state.automationPaused, state.autoGather, state.autoForm, state.autoHold, state.autoMolecules,
                    state.autoMoleculeReserve, state.fusionThreshold, state.holdLocked,
                    new ArrayList<>(state.automatedMolecules), new ArrayList<>(state.favoriteMolecules),
                    new TreeMap<>(state.stateCaps),
                    new ArrayList<>(state.atomUpgradeOrder), state.atomUpgradeOrdered,
                    new ArrayList<>(state.automationOrder), state.automationOrdered,
                    new ArrayList<>(state.enabledDarkAutomations), state.colliderPaused.clone());
        }

        void restore(GameStateCollider state) {
            state.automationPaused = automationPaused;
            state.autoGather = autoGather;
            state.autoForm = autoForm;
            state.autoHold = autoHold;
            state.autoMolecules = autoMolecules;
            state.autoMoleculeReserve = autoMoleculeReserve;
            state.fusionThreshold = fusionThreshold;
            state.holdLocked = holdLocked;
            state.automatedMolecules.clear();
            state.automatedMolecules.addAll(automatedMolecules);
            state.favoriteMolecules.clear();
            state.favoriteMolecules.addAll(favoriteMolecules);
            state.stateCaps.clear();
            state.stateCaps.putAll(stateCaps);
            state.atomUpgradeOrder.clear();
            state.atomUpgradeOrder.addAll(atomUpgradeOrder);
            state.atomUpgradeOrdered = atomUpgradeOrdered;
            state.automationOrder.clear();
            state.automationOrder.addAll(automationOrder);
            state.automationOrdered = automationOrdered;
            // Les automatismes de matière noire mis en marche : un réglage que le Big Bang garde déjà (clearDarkMatter).
            state.enabledDarkAutomations.clear();
            state.enabledDarkAutomations.addAll(enabledDarkAutomations);
            // Les pauses du collisionneur sont un choix du joueur. Les plans suivis, eux, repartent de zéro avec la
            // nouvelle partie ; leurs codes sauvegardés restent (voir startUniverse).
            state.colliderPaused.clear();
            state.colliderPaused.addAll(colliderPaused);
        }
    }

    /** Éclats de dimension, gagnés sur la ligne. */
    public double dimensionShards() { return dimensionShards; }

    /** Le niveau rangé à {@code index} dans un tableau de niveaux ; 0 hors du tableau. */
    static int level(int[] levels, int index) {
        return index >= 0 && index < levels.length ? levels[index] : 0;
    }

    /** Range {@code level} (0 au moins) à {@code index} dans un tableau de niveaux ; rien hors du tableau. */
    static void setLevel(int[] levels, int index, int level) {
        if (index >= 0 && index < levels.length) levels[index] = Math.max(0, level);
    }

    // Le profil et les albums.

    /** Le plus long nom de joueur accepté, en caractères. */
    public static final int PLAYER_NAME_MAX = 24;
}
