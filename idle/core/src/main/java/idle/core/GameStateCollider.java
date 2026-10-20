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

    /** Vrai tant que tous les automatismes sont coupés d'un geste ({@link Game#setAutomationPaused(boolean)}). Aucune remise à zéro ne change ce réglage. */
    public boolean automationPaused() {
        return automationPaused;
    }

    public void setAutomationPaused(boolean paused) {
        automationPaused = paused;
    }

    /** Vrai si le rassemblement automatique est en marche (il ne sert qu'une fois acquis). Aucune remise à zéro ne le coupe. */
    public boolean autoGather() {
        return autoGather;
    }

    public void setAutoGather(boolean enabled) {
        autoGather = enabled;
    }

    /** Vrai si la formation automatique est en marche (elle ne sert qu'une fois acquise). Aucune remise à zéro ne la coupe. */
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

    /** Nombre de comètes saisies depuis le début du jeu. */
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
     * l'écran de démarrage ne revienne pas.
     */
    public void startUniverse() {
        java.util.Set<String> keptAchievements = new java.util.LinkedHashSet<>(achievements);
        double played = timePlayed;
        GameStats keptStats = stats;
        boolean seen = endSeen;
        int extra = extraUniverses;
        BigNum seeds = universeSeeds;
        BigNum energy = voidEnergy;
        double shards = dimensionShards, phase = linePhase;
        int yieldLevel = lineYieldLevel, ticksLevel = lineTicksLevel;
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
        endSeen = seen;
        achievements.addAll(keptAchievements);
        achievementsVersion++;
        universeFormed = true;
        extraUniverses = extra;
        universeSeeds = seeds;
        voidEnergy = energy;
        // La ligne (version 0.101) appartient à la partie secondaire : elle traverse les univers.
        dimensionShards = shards;
        linePhase = phase;
        lineYieldLevel = yieldLevel;
        lineTicksLevel = ticksLevel;
        universeUpgrades.putAll(upgrades);
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
