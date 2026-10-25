package idle.core;

import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.Quark;
import idle.core.math.BigNum;
import idle.core.probes.Probe;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeUpgrade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * L'état de la partie : les améliorations d'espace, la comète, le collisionneur, les sondes,
 * l'impulsion, les univers, les records et les défis de Big Bang.
 */
public abstract class GameStateCollider extends GameStateUnlocks {

    /** Seul {@link GameState} prolonge la chaîne. */
    GameStateCollider() {
    }

    // Ce que fournissent les parties suivantes.
    public abstract void reset();


    /** Vrai si cette amélioration d'espace est acquise. Ni l'explosion ni le Big Bang ne la reprennent. */
    public boolean ownsSpaceUpgrade(String spaceUpgradeId) {
        return spaceUpgrades.contains(spaceUpgradeId);
    }

    /** Note une amélioration d'espace comme acquise. */
    public void addSpaceUpgrade(String spaceUpgradeId) {
        spaceUpgrades.add(spaceUpgradeId);
        moleculesVersion++;
    }

    /** Nombre d'améliorations d'espace acquises. */
    public int spaceUpgradeCount() {
        return spaceUpgrades.size();
    }

    /**
     * Vrai tant que tous les automatismes sont coupés d'un geste ({@link Game#setAutomationPaused(boolean)}). C'est un
     * réglage : aucune remise à zéro de la partie ne le change, seule une nouvelle partie ({@link PlayerSettings}).
     */
    public boolean automationPaused() {
        return automationPaused;
    }

    public void setAutomationPaused(boolean paused) {
        automationPaused = paused;
    }

    /** Vrai si le rassemblement automatique est en marche (il ne sert qu'une fois acquis). Un réglage : aucune remise à zéro ne le coupe ({@link PlayerSettings}). */
    public boolean autoGather() {
        return autoGather;
    }

    public void setAutoGather(boolean enabled) {
        autoGather = enabled;
    }

    /** Vrai si la formation automatique est en marche (elle ne sert qu'une fois acquise). Un réglage : aucune remise à zéro ne la coupe ({@link PlayerSettings}). */
    public boolean autoForm() {
        return autoForm;
    }

    public void setAutoForm(boolean enabled) {
        autoForm = enabled;
    }

    /** Part de l'espace créé que la création automatique laisse toujours libre, de 0 à 1. C'est un réglage. */
    public double autoMoleculeReserve() {
        return autoMoleculeReserve;
    }

    /** Durée de l'explosion qui sert de record depuis le dernier Big Bang, en secondes ; 0 tant qu'aucune n'a eu lieu. */
    public double explosionRecord() {
        return explosionRecord;
    }

    public void setExplosionRecord(double seconds) {
        explosionRecord = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes de jeu avant la prochaine comète ; 0 tant qu'aucune n'est attendue. */
    public double cometWait() {
        return cometWait;
    }

    public void setCometWait(double seconds) {
        cometWait = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes pendant lesquelles la comète reste encore à l'écran ; 0 quand il n'y en a pas. */
    public double cometVisible() {
        return cometVisible;
    }

    public void setCometVisible(double seconds) {
        cometVisible = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes pendant lesquelles le sillage de la dernière comète saisie accélère encore l'expansion. */
    public double cometBoost() {
        return cometBoost;
    }

    public void setCometBoost(double seconds) {
        cometBoost = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Énergie du collisionneur disponible, à dépenser en générateurs et en améliorations. */
    public double colliderEnergy() {
        return colliderEnergy;
    }

    public void setColliderEnergy(double energy) {
        colliderEnergy = Double.isNaN(energy) ? 0 : Math.max(0, energy);
    }

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
    public double colliderPhase() {
        return colliderPhase;
    }

    public void setColliderPhase(double phase) {
        colliderPhase = Double.isNaN(phase) ? 0 : phase - Math.floor(phase);
    }

    /** Nombre de croisements des faisceaux depuis le début. */
    public long colliderCrossings() {
        return colliderCrossings;
    }

    public void setColliderCrossings(long count) {
        colliderCrossings = Math.max(0, count);
    }

    /** Le numéro du dernier croisement capté d'un clic ; −1 avant le premier. */
    public long colliderCaught() {
        return colliderCaught;
    }

    public void setColliderCaught(long crossing) {
        colliderCaught = crossing;
    }

    public int colliderUpgradeLevel(ColliderUpgrade upgrade) {
        return colliderUpgrades.getOrDefault(upgrade, 0);
    }

    public void setColliderUpgradeLevel(ColliderUpgrade upgrade, int level) {
        if (level <= 0) colliderUpgrades.remove(upgrade);
        else colliderUpgrades.put(upgrade, Math.min(level, upgrade.maxLevel()));
    }

    /** Vrai si le joueur a mis cet automate en pause. */
    public boolean isColliderPaused(ColliderUpgrade upgrade) {
        return colliderPaused.contains(upgrade);
    }

    public void setColliderPaused(ColliderUpgrade upgrade, boolean paused) {
        if (paused) colliderPaused.add(upgrade);
        else colliderPaused.remove(upgrade);
    }

    public int quarks(Quark quark) {
        return quarks.getOrDefault(quark, 0);
    }

    public void setQuarks(Quark quark, int count) {
        if (count <= 0) quarks.remove(quark);
        else quarks.put(quark, count);
    }

    public double colliderEnergyProduced() {
        return colliderEnergyProduced;
    }

    public void setColliderEnergyProduced(double energy) {
        colliderEnergyProduced = Double.isNaN(energy) ? 0 : Math.max(0, energy);
    }

    /** Les collisions en cours, dans l'ordre de leur lancement. */
    public List<Collision> collisions() {
        return Collections.unmodifiableList(collisions);
    }

    public void setCollisions(List<Collision> running) {
        collisions.clear();
        collisions.addAll(running);
    }

    public long collisionRuns() {
        return collisionRuns;
    }

    public void setCollisionRuns(long count) {
        collisionRuns = Math.max(0, count);
    }

    /** Secondes avant que le quark top ne se désintègre ; 0 s'il n'y en a pas. */
    public double topLeft() {
        return topLeft;
    }

    public void setTopLeft(double seconds) {
        topLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes de surchauffe qu'il reste. */
    public double overheatLeft() {
        return overheatLeft;
    }

    public void setOverheatLeft(double seconds) {
        overheatLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Les sondes en vol, dans l'ordre de leur départ. */
    public List<Probe> probes() {
        return Collections.unmodifiableList(probes);
    }

    public void setProbes(List<Probe> flying) {
        probes.clear();
        probes.addAll(flying);
    }

    /** Les récompenses rapportées, qui attendent d'être réclamées, la prochaine en tête. */
    public List<ProbeReward> probeRewards() {
        return Collections.unmodifiableList(probeRewards);
    }

    public void setProbeRewards(List<ProbeReward> waiting) {
        probeRewards.clear();
        probeRewards.addAll(waiting);
    }

    public int probeUpgradeLevel(ProbeUpgrade upgrade) {
        return probeUpgrades.getOrDefault(upgrade, 0);
    }

    public void setProbeUpgradeLevel(ProbeUpgrade upgrade, int level) {
        if (level <= 0) probeUpgrades.remove(upgrade);
        else probeUpgrades.put(upgrade, Math.min(level, upgrade.maxLevel()));
    }

    /** Nombre de voyages terminés depuis le début : il fixe le butin de chacun, sans toucher au hasard du jeu. */
    public long probeRuns() {
        return probeRuns;
    }

    public void setProbeRuns(long runs) {
        probeRuns = Math.max(0, runs);
    }

    public long probeLaunches() {
        return probeLaunches;
    }

    public void setProbeLaunches(long launches) {
        probeLaunches = Math.max(0, launches);
    }

    public long probeSamples() {
        return probeSamples;
    }

    public void setProbeSamples(long samples) {
        probeSamples = Math.max(0, samples);
    }

    /** Secondes de sursaut d'expansion qu'il reste. */
    public double probeBoostLeft() {
        return probeBoostLeft;
    }

    public void setProbeBoostLeft(double seconds) {
        probeBoostLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    public double pulseCharges() {
        return pulseCharges;
    }

    public void setPulseCharges(double charges) {
        pulseCharges = Double.isNaN(charges) ? 0 : Math.max(0, charges);
    }

    public long pulses() {
        return pulses;
    }

    public void setPulses(long count) {
        pulses = Math.max(0, count);
    }

    /** Nombre de comètes saisies depuis le début du jeu. */
    public int cometsCaught() {
        return cometsCaught;
    }

    /** Vrai une fois l'écran de fin vu : il ne s'ouvre qu'une fois par partie. */
    public boolean endSeen() {
        return endSeen;
    }

    public void setEndSeen(boolean seen) {
        endSeen = seen;
    }

    /**
     * Vrai une fois le premier univers formé (version 0.083) : il reste formé après la remise à
     * zéro qui l'accompagne ({@link #startUniverse()}), quand l'échelle du cosmos repart de rien.
     */
    public boolean universeFormed() {
        return universeFormed;
    }

    public void setUniverseFormed(boolean formed) {
        universeFormed = formed;
    }

    /**
     * Le premier univers se forme (version 0.083) : tout ce qui y a mené est effacé, comme pour une
     * nouvelle partie ({@link #reset()}). Restent les succès, le temps de jeu, les statistiques,
     * l'écran de fin déjà vu, et tout ce qui appartient à l'onglet Univers : les univers, leurs
     * graines, l'énergie du vide et les quatre améliorations. La partie reste commencée, pour que
     * l'écran de démarrage ne revienne pas. Depuis la version 0.126, les réglages du joueur restent
     * aussi ({@link PlayerSettings}) : seule une nouvelle partie lancée par le joueur les efface.
     */
    public void startUniverse() {
        PlayerSettings settings = new PlayerSettings(this);
        java.util.Set<String> keptAchievements = new java.util.LinkedHashSet<>(achievements);
        double played = timePlayed;
        GameStats keptStats = stats;
        boolean seen = endSeen;
        int extra = extraUniverses;
        BigNum seeds = universeSeeds;
        // Le vide (version 0.129) repart de zéro à chaque univers : seul son historique reste.
        Map<String, Double> voidKept = new java.util.LinkedHashMap<>(voidHistory);
        double shards = dimensionShards, phase = linePhase;
        int yieldLevel = lineYieldLevel, ticksLevel = lineTicksLevel;
        int speedLevel = lineSpeedLevel, ballsLevel = lineBallsLevel, countLevel = lineCountLevel;
        double earned = dimensionShardsEarned;
        boolean sprouted = seedSprouted;
        long waterings = seedWaterings;
        boolean twig = twigOpened;
        int seedOpened = seedUniversesOpened;
        double planeKept = planeShards, planeEarned = planeShardsEarned;
        int[] planeKeptLevels = {planeValueLevel, planeSpawnLevel, planeCountLevel, planeSpeedLevel, planeSizeLevel};
        double volumeKept = volumeShards, volumeEarned = volumeShardsEarned;
        int[] volumeKeptLevels = new int[VOLUME_UPGRADES];
        for (int i = 0; i < VOLUME_UPGRADES; i++) volumeKeptLevels[i] = volumeLevel(i);
        double durationKept = durationShards, durationEarned = durationShardsEarned;
        int[] durationKeptLevels = new int[DURATION_UPGRADES];
        for (int i = 0; i < DURATION_UPGRADES; i++) durationKeptLevels[i] = durationLevel(i);
        Map<String, Integer> upgrades = new TreeMap<>(universeUpgrades);
        List<String> treeCodes = new ArrayList<>(savedDarkPlans), systemsCodes = new ArrayList<>(savedSystemPlans);
        // Les skins (version 0.087) sont des apparences : ils restent d'un univers à l'autre.
        boolean skins = skinsOpen;
        Set<String> skinsUnlocked = new java.util.TreeSet<>(unlockedSkins);
        Map<String, String> skinsChosen = new TreeMap<>(chosenSkins);
        Set<String> trialsDone = new java.util.TreeSet<>(completedTrials);
        Set<String> popups = new java.util.TreeSet<>(seenPopups);
        boolean popupsReady = popupsPrimed;
        Set<String> researchesDone = new java.util.TreeSet<>(researches);
        double rebuild = rebuildRecord;
        long fusions = stats.fusions();
        long explosionsDone = stats.timedExplosions();
        reset();
        // Les plans gardés par « Sauvegarder » (version 0.084) servent d'une partie à l'autre.
        for (int i = 0; i < PLAN_SLOTS; i++) {
            savedDarkPlans.set(i, treeCodes.get(i));
            savedSystemPlans.set(i, systemsCodes.get(i));
        }
        skinsOpen = skins;
        unlockedSkins.addAll(skinsUnlocked);
        chosenSkins.putAll(skinsChosen);
        fusionsAtUniverse = fusions;
        explosionsAtUniverse = explosionsDone;
        // Les épreuves réussies, les recherches et le record de reconstruction (version 0.088) restent aussi.
        completedTrials.addAll(trialsDone);
        researches.addAll(researchesDone);
        // Les fenêtres d'explication déjà vues (version 0.089) ne reviennent pas.
        seenPopups.addAll(popups);
        popupsPrimed = popupsReady;
        rebuildRecord = rebuild;
        started = true;
        timePlayed = played;
        stats = keptStats;
        // Une nouvelle partie commence (version 0.126) : ses étapes, ses délais et ses records se comptent d'ici.
        stats.startUniverseSteps(played);
        endSeen = seen;
        achievements.addAll(keptAchievements);
        achievementsVersion++;
        universeFormed = true;
        extraUniverses = extra;
        universeSeeds = seeds;
        voidHistory.putAll(voidKept);
        // La ligne (version 0.101) appartient à la partie secondaire : elle traverse les univers.
        dimensionShards = shards;
        linePhase = phase;
        lineYieldLevel = yieldLevel;
        lineTicksLevel = ticksLevel;
        lineSpeedLevel = speedLevel;
        lineBallsLevel = ballsLevel;
        lineCountLevel = countLevel;
        dimensionShardsEarned = earned;
        seedSprouted = sprouted;
        seedWaterings = waterings;
        twigOpened = twig;
        seedUniversesOpened = seedOpened;
        planeShards = planeKept;
        planeShardsEarned = planeEarned;
        for (int i = 0; i < planeKeptLevels.length; i++) setPlaneLevel(i, planeKeptLevels[i]);
        // Le volume (version 0.128) traverse les univers, comme la ligne et le plan.
        volumeShards = volumeKept;
        volumeShardsEarned = volumeEarned;
        for (int i = 0; i < volumeKeptLevels.length; i++) setVolumeLevel(i, volumeKeptLevels[i]);
        // La durée (version 0.134) aussi.
        durationShards = durationKept;
        durationShardsEarned = durationEarned;
        for (int i = 0; i < durationKeptLevels.length; i++) setDurationLevel(i, durationKeptLevels[i]);
        universeUpgrades.putAll(upgrades);
        settings.restore(this);
    }

    /**
     * Les réglages du joueur (version 0.126) : des choix sur la façon de jouer, pas de l'avancée. Ni la fusion, ni
     * l'explosion, ni le Big Bang, ni aucune remise à zéro de la partie ne les changent ; seule une nouvelle partie
     * lancée par le joueur ({@link #reset()}) les remet comme au premier lancement.
     */
    private record PlayerSettings(boolean automationPaused, boolean autoGather, boolean autoForm, boolean autoHold,
                                  boolean autoMolecules, boolean autoFollowList, boolean autoFollowMatter,
                                  double autoMoleculeReserve, int fusionThreshold, boolean holdLocked,
                                  List<String> automatedMolecules, List<String> favoriteMolecules,
                                  Map<String, Double> stateCaps, boolean recipeReservation,
                                  List<String> atomUpgradeOrder, boolean atomUpgradeOrdered,
                                  List<String> automationOrder, boolean automationOrdered,
                                  List<String> enabledDarkAutomations, java.util.EnumSet<ColliderUpgrade> colliderPaused) {

        PlayerSettings(GameStateCollider state) {
            this(state.automationPaused, state.autoGather, state.autoForm, state.autoHold,
                    state.autoMolecules, state.autoFollowList, state.autoFollowMatter,
                    state.autoMoleculeReserve, state.fusionThreshold, state.holdLocked,
                    new ArrayList<>(state.automatedMolecules), new ArrayList<>(state.favoriteMolecules),
                    new TreeMap<>(state.stateCaps), state.recipeReservation,
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
            state.autoFollowList = autoFollowList;
            state.autoFollowMatter = autoFollowMatter;
            state.autoMoleculeReserve = autoMoleculeReserve;
            state.fusionThreshold = fusionThreshold;
            state.holdLocked = holdLocked;
            state.automatedMolecules.clear();
            state.automatedMolecules.addAll(automatedMolecules);
            state.favoriteMolecules.clear();
            state.favoriteMolecules.addAll(favoriteMolecules);
            state.stateCaps.clear();
            state.stateCaps.putAll(stateCaps);
            state.recipeReservation = recipeReservation;
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

    /** Éclats de dimension, gagnés sur la ligne (version 0.101). */
    public double dimensionShards() {
        return dimensionShards;
    }

    public void setDimensionShards(double shards) {
        dimensionShards = shards >= 0 && Double.isFinite(shards) ? shards : 0;
    }

    /** Où en est la boule de la ligne : de 0 (au début) à 1 (au bout) à l'aller, puis de 1 à 2 au retour. */
    public double linePhase() {
        return linePhase;
    }

    public void setLinePhase(double phase) {
        linePhase = phase >= 0 && phase < 2 ? phase : 0;
    }

    /** Niveau de l'amélioration qui augmente les éclats de chaque trait touché. */
    public int lineYieldLevel() {
        return lineYieldLevel;
    }

    public void setLineYieldLevel(int level) {
        lineYieldLevel = Math.max(0, level);
    }

    /** Niveau de l'amélioration qui ajoute des traits sur la ligne : un trait de plus par niveau. */
    public int lineTicksLevel() {
        return lineTicksLevel;
    }

    public void setLineTicksLevel(int level) {
        lineTicksLevel = Math.max(0, level);
    }

    /** Niveau de l'amélioration qui accélère la boule. */
    public int lineSpeedLevel() {
        return lineSpeedLevel;
    }

    public void setLineSpeedLevel(int level) {
        lineSpeedLevel = Math.max(0, level);
    }

    /** Niveau de l'amélioration qui ajoute des boules : une de plus par niveau, sur chaque ligne. */
    public int lineBallsLevel() {
        return lineBallsLevel;
    }

    public void setLineBallsLevel(int level) {
        lineBallsLevel = Math.max(0, level);
    }

    /** Niveau de l'amélioration qui ajoute des lignes : une de plus par niveau. */
    public int lineCountLevel() {
        return lineCountLevel;
    }

    public void setLineCountLevel(int level) {
        lineCountLevel = Math.max(0, level);
    }

    /** Tous les éclats de dimension gagnés, dépensés ou non : c'est eux que compte le seuil. */
    public double dimensionShardsEarned() {
        return dimensionShardsEarned;
    }

    public void setDimensionShardsEarned(double earned) {
        dimensionShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Vrai une fois la graine d'univers germée. */
    public boolean seedSprouted() {
        return seedSprouted;
    }

    public void setSeedSprouted(boolean sprouted) {
        seedSprouted = sprouted;
    }

    /** Combien de fois le germe a été arrosé. */
    public long seedWaterings() {
        return seedWaterings;
    }

    public void setSeedWaterings(long waterings) {
        seedWaterings = Math.max(0, waterings);
    }

    /** Univers que la graine a ouverts (version 0.120). */
    public int seedUniversesOpened() {
        return seedUniversesOpened;
    }

    public void setSeedUniversesOpened(int opened) {
        seedUniversesOpened = Math.max(0, opened);
    }

    /** Vrai une fois que la brindille a ouvert la partie du deuxième univers (version 0.109). */
    public boolean twigOpened() {
        return twigOpened;
    }

    public void setTwigOpened(boolean opened) {
        twigOpened = opened;
    }

    /** Éclats du plan (version 0.104) : propres à la deuxième dimension. */
    public double planeShards() {
        return planeShards;
    }

    public void setPlaneShards(double shards) {
        planeShards = shards >= 0 && Double.isFinite(shards) ? shards : 0;
    }

    /** Tous les éclats du plan gagnés, dépensés ou non. */
    public double planeShardsEarned() {
        return planeShardsEarned;
    }

    public void setPlaneShardsEarned(double earned) {
        planeShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration du plan, dans l'ordre de {@code Game.PlaneUpgrade}. */
    public int planeLevel(int index) {
        return switch (index) {
            case 0 -> planeValueLevel;
            case 1 -> planeSpawnLevel;
            case 2 -> planeCountLevel;
            case 3 -> planeSpeedLevel;
            case 4 -> planeSizeLevel;
            default -> 0;
        };
    }

    public void setPlaneLevel(int index, int level) {
        int value = Math.max(0, level);
        switch (index) {
            case 0 -> planeValueLevel = value;
            case 1 -> planeSpawnLevel = value;
            case 2 -> planeCountLevel = value;
            case 3 -> planeSpeedLevel = value;
            case 4 -> planeSizeLevel = value;
            default -> { }
        }
    }

    /** Éclats du volume (version 0.128) : propres à la troisième dimension. */
    public double volumeShards() {
        return volumeShards;
    }

    public void setVolumeShards(double shards) {
        volumeShards = shards >= 0 && Double.isFinite(shards) ? shards : 0;
    }

    /** Tous les éclats du volume gagnés, dépensés ou non. */
    public double volumeShardsEarned() {
        return volumeShardsEarned;
    }

    public void setVolumeShardsEarned(double earned) {
        volumeShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration du volume, dans l'ordre de {@code Game.VolumeUpgrade} ; 0 hors des cinq. */
    public int volumeLevel(int index) {
        return switch (index) {
            case 0 -> volumeTimeLevel;
            case 1 -> volumeValueLevel;
            case 2 -> volumeSpawnLevel;
            case 3 -> volumeCountLevel;
            case 4 -> volumeSizeLevel;
            default -> 0;
        };
    }

    public void setVolumeLevel(int index, int level) {
        int value = Math.max(0, level);
        switch (index) {
            case 0 -> volumeTimeLevel = value;
            case 1 -> volumeValueLevel = value;
            case 2 -> volumeSpawnLevel = value;
            case 3 -> volumeCountLevel = value;
            case 4 -> volumeSizeLevel = value;
            default -> { }
        }
    }

    /** Éclats de la durée (version 0.134) : propres à la quatrième dimension. */
    public double durationShards() {
        return durationShards;
    }

    public void setDurationShards(double shards) {
        durationShards = shards >= 0 && Double.isFinite(shards) ? shards : 0;
    }

    /** Tous les éclats de la durée gagnés, dépensés ou non. */
    public double durationShardsEarned() {
        return durationShardsEarned;
    }

    public void setDurationShardsEarned(double earned) {
        durationShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration de la durée, dans l'ordre de {@code Game.DurationUpgrade} ; 0 hors des quatre. */
    public int durationLevel(int index) {
        return switch (index) {
            case 0 -> durationSpeedLevel;
            case 1 -> durationCountLevel;
            case 2 -> durationValueLevel;
            case 3 -> durationTimeLevel;
            default -> 0;
        };
    }

    public void setDurationLevel(int index, int level) {
        int value = Math.max(0, level);
        switch (index) {
            case 0 -> durationSpeedLevel = value;
            case 1 -> durationCountLevel = value;
            case 2 -> durationValueLevel = value;
            case 3 -> durationTimeLevel = value;
            default -> { }
        }
    }

    /** Univers créés après le premier (version 0.081). */
    public int extraUniverses() {
        return extraUniverses;
    }

    public void setExtraUniverses(int count) {
        extraUniverses = Math.max(0, count);
    }

    /** Graines d'univers : la ressource principale de l'onglet Univers (version 0.081). */
    public BigNum universeSeeds() {
        return universeSeeds;
    }

    public void setUniverseSeeds(BigNum seeds) {
        universeSeeds = seeds == null || seeds.sign() < 0 ? BigNum.ZERO : seeds;
    }

    /** Énergie du vide : la ressource du deuxième acte de la partie secondaire, le Vide (version 0.081). */
    public BigNum voidEnergy() {
        return voidEnergy;
    }

    public void setVoidEnergy(BigNum energy) {
        voidEnergy = energy == null || energy.sign() < 0 ? BigNum.ZERO : energy;
    }

    /** Vrai une fois le vide ouvert dans cet univers (version 0.129) : payé en énergie du collisionneur. */
    public boolean voidOpen() {
        return voidOpen;
    }

    public void setVoidOpen(boolean open) {
        voidOpen = open;
    }

    /** Vrai une fois le trou noir du vide ouvert dans cet univers : au premier trou noir formé. */
    public boolean voidHoleOpen() {
        return voidHoleOpen;
    }

    public void setVoidHoleOpen(boolean open) {
        voidHoleOpen = open;
    }

    /** La part de l'expansion donnée au vide, de 0 à 1. */
    public double voidShare() {
        return voidShare;
    }

    public void setVoidShare(double share) {
        voidShare = Double.isFinite(share) ? Math.max(0, Math.min(1, share)) : 0.25;
    }

    /** Toutes les particules sacrifiées au trou noir du vide dans cet univers. */
    public double voidPrice() {
        return voidPrice;
    }

    public void setVoidPrice(double price) {
        voidPrice = Double.isFinite(price) && price > 0 ? price : 0;
    }

    public BigNum voidSacrificed() {
        return voidSacrificed;
    }

    public void setVoidSacrificed(BigNum sacrificed) {
        voidSacrificed = sacrificed == null || sacrificed.sign() < 0 ? BigNum.ZERO : sacrificed;
    }

    /** Niveau d'une amélioration du vide, dans l'ordre de {@code Game.VoidUpgrade} ; 0 hors des huit. */
    public int voidLevel(int index) {
        return switch (index) {
            case 0 -> voidGainLevel;
            case 1 -> voidRateLevel;
            case 2 -> voidEnergyLevel;
            case 3 -> voidSpaceLevel;
            case 4 -> voidDecayLevel;
            case 5 -> voidLightenLevel;
            case 6 -> voidMatureLevel;
            case 7 -> voidSettleLevel;
            default -> 0;
        };
    }

    public void setVoidLevel(int index, int level) {
        int value = Math.max(0, level);
        switch (index) {
            case 0 -> voidGainLevel = value;
            case 1 -> voidRateLevel = value;
            case 2 -> voidEnergyLevel = value;
            case 3 -> voidSpaceLevel = value;
            case 4 -> voidDecayLevel = value;
            case 5 -> voidLightenLevel = value;
            case 6 -> voidMatureLevel = value;
            case 7 -> voidSettleLevel = value;
            default -> { }
        }
    }

    /** Le vide de chaque univers formé : son numéro → l'énergie du vide qu'il restait à sa formation. */
    public Map<String, Double> voidHistory() {
        return java.util.Collections.unmodifiableMap(voidHistory);
    }

    /** Note l'énergie du vide qu'il restait à la formation de cet univers. */
    public void noteVoidHistory(String universe, double stock) {
        if (universe == null || !Double.isFinite(stock) || stock < 0) return;
        voidHistory.put(universe, stock);
    }

    /** Le cumul : l'énergie du vide qu'il restait à la formation de chaque univers, additionnée. */
    public void clearVoidHistory() {
        voidHistory.clear();
    }

    public double voidAccumulated() {
        double total = 0;
        for (double stock : voidHistory.values()) total += stock;
        return total;
    }

    /** Niveau d'une amélioration de l'univers. */
    public int universeUpgradeLevel(String id) {
        return universeUpgrades.getOrDefault(id, 0);
    }

    public void setUniverseUpgradeLevel(String id, int level) {
        if (level <= 0) universeUpgrades.remove(id);
        else universeUpgrades.put(id, level);
    }

    public void setCometsCaught(int count) {
        cometsCaught = Math.max(0, count);
    }

    /** L'anomalie du ciel à saisir en ce moment (version 0.127), par son nom ; {@code null} s'il n'y en a pas. */
    public String skyEvent() {
        return skyEvent;
    }

    public void setSkyEvent(String event) {
        skyEvent = event == null || event.isBlank() ? null : event;
    }

    /** Secondes pendant lesquelles l'anomalie reste à saisir ; 0 quand il n'y en a pas. */
    public double skyEventVisible() {
        return skyEventVisible;
    }

    public void setSkyEventVisible(double seconds) {
        skyEventVisible = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes avant la prochaine anomalie ; 0 tant qu'elle n'est pas tirée. */
    public double skyEventWait() {
        return skyEventWait;
    }

    public void setSkyEventWait(double seconds) {
        skyEventWait = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes pendant lesquelles le vent de particules souffle encore (il peut être négatif juste après sa fin). */
    public double windLeft() {
        return windLeft;
    }

    public void setWindLeft(double seconds) {
        windLeft = Double.isNaN(seconds) ? 0 : seconds;
    }

    /** Nombre d'anomalies du ciel saisies depuis le début du jeu. */
    public int skyEventsCaught() {
        return skyEventsCaught;
    }

    public void setSkyEventsCaught(int count) {
        skyEventsCaught = Math.max(0, count);
    }

    /** Les records du joueur : nom d'une étape → temps de jeu le plus court auquel elle a été atteinte. */
    public Map<String, Double> records() {
        return Collections.unmodifiableMap(records);
    }

    /** Note un record ; un temps négatif ou qui n'est pas un nombre est refusé. */
    public void setRecord(String step, double seconds) {
        if (step == null || step.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        records.put(step, seconds);
    }

    /** Oublie tous les records : la seule chose que la remise à zéro du jeu ne fait pas d'elle-même. */
    public void clearRecords() {
        records.clear();
    }

    /** Le défi de Big Bang en cours, ou {@code null}. */
    public String activeBangChallenge() {
        return activeBangChallenge;
    }

    public void setActiveBangChallenge(String challenge) {
        activeBangChallenge = challenge == null || challenge.isBlank() ? null : challenge;
    }

    /** Les paliers de Big Bang payés (version 0.124) : un palier atteint n'agit qu'une fois payé. */
    public Set<String> boughtMilestones() {
        return Collections.unmodifiableSet(boughtMilestones);
    }

    public void addBoughtMilestone(String name) {
        if (name != null && !name.isBlank()) boughtMilestones.add(name);
    }

    /** Les défis de Big Bang réussis. */
    public Set<String> completedBangChallenges() {
        return Collections.unmodifiableSet(completedBangChallenges);
    }

    public void addCompletedBangChallenge(String challenge) {
        if (challenge != null && !challenge.isBlank()) completedBangChallenges.add(challenge);
    }

    /** Le meilleur temps de chaque défi de Big Bang réussi, en secondes. */
    public Map<String, Double> bangChallengeTimes() {
        return Collections.unmodifiableMap(bangChallengeTimes);
    }

    /** Le temps de jeu auquel le défi de Big Bang en cours a commencé, en secondes. */
    public double bangChallengeStarted() {
        return bangChallengeStarted;
    }

    public void setBangChallengeStarted(double timePlayed) {
        bangChallengeStarted = Double.isNaN(timePlayed) ? 0 : Math.max(0, timePlayed);
    }

    public void setBangChallengeTime(String challenge, double seconds) {
        if (challenge == null || challenge.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        bangChallengeTimes.put(challenge, seconds);
    }
}
