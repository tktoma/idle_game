package idle.core.state;

import idle.core.*;
import idle.core.collider.*;
import idle.core.math.BigNum;
import idle.core.probes.*;
import java.util.*;

/**
 * L'état de la partie : les améliorations d'espace, la comète, le collisionneur, les sondes,
 * l'impulsion, les univers, les records et les défis de Big Bang.
 */
public abstract class GameStateCollider extends GameStateColliderBase {
    GameStateCollider() {
        super();
    }

    public void setDimensionShards(double shards) {
        dimensionShards = shards >= 0 && Double.isFinite(shards) ? shards : 0;
    }

    /** Où en est la boule de la ligne : de 0 (au début) à 1 (au bout) à l'aller, puis de 1 à 2 au retour. */
    public double linePhase() { return linePhase; }

    public void setLinePhase(double phase) { linePhase = phase >= 0 && phase < 2 ? phase : 0; }

    /** Niveau d'une amélioration de la ligne, dans l'ordre de {@code Game.LineUpgrade} ; 0 hors des cinq. */
    public int lineLevel(int index) { return level(lineLevels, index); }

    public void setLineLevel(int index, int level) { setLevel(lineLevels, index, level); }

    /** Niveau de l'amélioration qui augmente les éclats de chaque trait touché. */
    public int lineYieldLevel() { return lineLevels[0]; }

    public void setLineYieldLevel(int level) { setLineLevel(0, level); }

    /** Niveau de l'amélioration qui ajoute des traits sur la ligne : un trait de plus par niveau. */
    public int lineTicksLevel() { return lineLevels[1]; }

    public void setLineTicksLevel(int level) { setLineLevel(1, level); }

    /** Niveau de l'amélioration qui accélère la boule. */
    public int lineSpeedLevel() { return lineLevels[2]; }

    public void setLineSpeedLevel(int level) { setLineLevel(2, level); }

    /** Niveau de l'amélioration qui ajoute des boules : une de plus par niveau, sur chaque ligne. */
    public int lineBallsLevel() { return lineLevels[3]; }

    public void setLineBallsLevel(int level) { setLineLevel(3, level); }

    /** Niveau de l'amélioration qui ajoute des lignes : une de plus par niveau. */
    public int lineCountLevel() { return lineLevels[4]; }

    public void setLineCountLevel(int level) { setLineLevel(4, level); }

    /** Tous les éclats de dimension gagnés, dépensés ou non : c'est eux que compte le seuil. */
    public double dimensionShardsEarned() { return dimensionShardsEarned; }

    public void setDimensionShardsEarned(double earned) {
        dimensionShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Vrai une fois l'arrosoir débloqué : il reste d'un univers à l'autre, comme la graine. */
    public boolean wateringCan() { return wateringCan; }

    public void setWateringCan(boolean unlocked) { wateringCan = unlocked; }

    /** Vrai une fois la graine d'univers germée. */
    public boolean seedSprouted() { return seedSprouted; }

    public void setSeedSprouted(boolean sprouted) { seedSprouted = sprouted; }

    /** Combien de fois le germe a été arrosé. */
    public long seedWaterings() { return seedWaterings; }

    public void setSeedWaterings(long waterings) { seedWaterings = Math.max(0, waterings); }

    /** Univers que la graine a ouverts. */
    public int seedUniversesOpened() { return seedUniversesOpened; }

    public void setSeedUniversesOpened(int opened) { seedUniversesOpened = Math.max(0, opened); }

    /** Vrai une fois que la brindille a ouvert la partie du deuxième univers. */
    public boolean twigOpened() { return twigOpened; }

    public void setTwigOpened(boolean opened) { twigOpened = opened; }

    /** Éclats du plan : propres à la deuxième dimension. */
    public double planeShards() { return planeShards; }

    public void setPlaneShards(double shards) { planeShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Tous les éclats du plan gagnés, dépensés ou non. */
    public double planeShardsEarned() { return planeShardsEarned; }

    public void setPlaneShardsEarned(double earned) {
        planeShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration du plan, dans l'ordre de {@code Game.PlaneUpgrade} ; 0 hors des cinq. */
    public int planeLevel(int index) { return level(planeLevels, index); }

    public void setPlaneLevel(int index, int level) { setLevel(planeLevels, index, level); }

    /** Éclats du volume : propres à la troisième dimension. */
    public double volumeShards() { return volumeShards; }

    public void setVolumeShards(double shards) { volumeShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Tous les éclats du volume gagnés, dépensés ou non. */
    public double volumeShardsEarned() { return volumeShardsEarned; }

    public void setVolumeShardsEarned(double earned) {
        volumeShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration du volume, dans l'ordre de {@code Game.VolumeUpgrade} ; 0 hors des cinq. */
    public int volumeLevel(int index) { return level(volumeLevels, index); }

    public void setVolumeLevel(int index, int level) { setLevel(volumeLevels, index, level); }

    /** Éclats de la durée : propres à la quatrième dimension. */
    /** Les fragments du mélange. */
    public double mixShards() { return mixShards; }

    public void setMixShards(double shards) { mixShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Les fragments gagnés en tout : ce qui compte pour le seuil du mélange. */
    public double mixShardsEarned() { return mixShardsEarned; }

    public void setMixShardsEarned(double earned) {
        mixShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Vrai une fois la partie finie. */
    public boolean gameFinished() { return gameFinished; }

    public void setGameFinished(boolean finished) { gameFinished = finished; }

    /** L'étape du mélange : le nombre de dimensions mêlées, de 1 à 9. */
    public int mixStage() { return mixStage; }

    public void setMixStage(int stage) { mixStage = Math.max(1, Math.min(9, stage)); }

    /** Les fragments gagnés depuis le début de l'étape : ce qui ouvre la suivante. */
    public double mixStageEarned() { return mixStageEarned; }

    public void setMixStageEarned(double earned) {
        mixStageEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration du mélange, dans l'ordre de {@code Game.MixUpgrade}. */
    public int mixLevel(int index) { return level(mixLevels, index); }

    public void setMixLevel(int index, int level) { setLevel(mixLevels, index, level); }

    /** Les reflets du miroir. */
    public double mirrorShards() { return mirrorShards; }

    public void setMirrorShards(double shards) { mirrorShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Les reflets gagnés en tout : ce qui compte pour le seuil du miroir. */
    public double mirrorShardsEarned() { return mirrorShardsEarned; }

    public void setMirrorShardsEarned(double earned) {
        mirrorShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Les motifs réussis en tout. */
    public double mirrorWins() { return mirrorWins; }

    public void setMirrorWins(double wins) { mirrorWins = wins >= 0 && Double.isFinite(wins) ? Math.floor(wins) : 0; }

    /** Niveau d'une amélioration du miroir, dans l'ordre de {@code Game.MirrorUpgrade}. */
    public int mirrorLevel(int index) { return level(mirrorLevels, index); }

    public void setMirrorLevel(int index, int level) { setLevel(mirrorLevels, index, level); }

    /** Les repères de la grille. */
    public double gridShards() { return gridShards; }

    public void setGridShards(double shards) { gridShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Les repères gagnés en tout : ce qui compte pour le seuil de la grille. */
    public double gridShardsEarned() { return gridShardsEarned; }

    public void setGridShardsEarned(double earned) {
        gridShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration de la grille, dans l'ordre de {@code Game.GridUpgrade}. */
    public int gridLevel(int index) { return level(gridLevels, index); }

    public void setGridLevel(int index, int level) { setLevel(gridLevels, index, level); }

    /** Les étincelles du clicker. */
    public double clickerShards() { return clickerShards; }

    public void setClickerShards(double shards) { clickerShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Les étincelles gagnées en tout : ce qui compte pour le seuil du clicker. */
    public double clickerShardsEarned() { return clickerShardsEarned; }

    public void setClickerShardsEarned(double earned) {
        clickerShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration du clicker, dans l'ordre de {@code Game.ClickerUpgrade}. */
    public int clickerLevel(int index) { return level(clickerLevels, index); }

    public void setClickerLevel(int index, int level) { setLevel(clickerLevels, index, level); }

    /** Les ondes de la brane. */
    public double braneShards() { return braneShards; }

    public void setBraneShards(double shards) { braneShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Les ondes gagnées en tout : ce qui compte pour le seuil de la brane. */
    public double braneShardsEarned() { return braneShardsEarned; }

    public void setBraneShardsEarned(double earned) {
        braneShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration de la brane, dans l'ordre de {@code Game.BraneUpgrade}. */
    public int braneLevel(int index) { return level(braneLevels, index); }

    public void setBraneLevel(int index, int level) { setLevel(braneLevels, index, level); }

    /** Les nœuds de la boucle. */
    public double loopShards() { return loopShards; }

    public void setLoopShards(double shards) { loopShards = shards >= 0 && Double.isFinite(shards) ? shards : 0; }

    /** Les nœuds gagnés en tout : ce qui compte pour le seuil de la boucle. */
    public double loopShardsEarned() { return loopShardsEarned; }

    public void setLoopShardsEarned(double earned) {
        loopShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration de la boucle, dans l'ordre de {@code Game.LoopUpgrade}. */
    public int loopLevel(int index) { return level(loopLevels, index); }

    public void setLoopLevel(int index, int level) { setLevel(loopLevels, index, level); }

    public double durationShards() { return durationShards; }

    public void setDurationShards(double shards) {
        durationShards = shards >= 0 && Double.isFinite(shards) ? shards : 0;
    }

    /** Tous les éclats de la durée gagnés, dépensés ou non. */
    public double durationShardsEarned() { return durationShardsEarned; }

    public void setDurationShardsEarned(double earned) {
        durationShardsEarned = earned >= 0 && Double.isFinite(earned) ? earned : 0;
    }

    /** Niveau d'une amélioration de la graine, dans l'ordre de {@code Game.SeedUpgrade} ; 0 hors des huit. */
    public int seedUpgradeLevel(int index) { return level(seedUpgradeLevels, index); }

    public void setSeedUpgradeLevel(int index, int level) { setLevel(seedUpgradeLevels, index, level); }

    /** Le temps gardé dans le chrono, en secondes. */
    public double chronoStored() { return chronoStored; }

    public void setChronoStored(double seconds) {
        chronoStored = seconds >= 0 && Double.isFinite(seconds) ? seconds : 0;
    }

    /** L'accélération choisie pour le chrono : 1 (arrêté) au moins. */
    public double chronoSpeed() { return chronoSpeed; }

    public void setChronoSpeed(double speed) { chronoSpeed = speed >= 1 && Double.isFinite(speed) ? speed : 1; }

    /** Vrai si la bulle de l'arbre {@code index} (ordre de {@code Game.TreeBubble}) est achetée. */
    public boolean treeBubbleBought(int index) { return treeBubbleLevel(index) > 0; }

    public void setTreeBubbleBought(int index, boolean bought) {
        if (index < 0 || index >= TREE_BUBBLES) return;
        treeBubbleLevels[index] = bought ? Math.max(1, treeBubbleLevels[index]) : 0;
    }

    /** Le niveau de la bulle {@code index} : 0 pas achetée, 1 achetée, plus pour celles qui se rachètent. */
    public int treeBubbleLevel(int index) { return index >= 0 && index < TREE_BUBBLES ? treeBubbleLevels[index] : 0; }

    public void setTreeBubbleLevel(int index, int level) {
        if (index >= 0 && index < TREE_BUBBLES) treeBubbleLevels[index] = Math.max(0, level);
    }

    /** Les bulles achetées, un bit par bulle : la clé de la sauvegarde d'avant la 0.148, toujours écrite. */
    public int treeBubbles() {
        int bits = 0;
        for (int i = 0; i < TREE_BUBBLES; i++) if (treeBubbleLevels[i] > 0) bits |= 1 << i;
        return bits;
    }

    /** Relit les bulles achetées d'un champ de bits : chacune au niveau 1. */
    public void setTreeBubbles(int bits) {
        for (int i = 0; i < TREE_BUBBLES; i++) treeBubbleLevels[i] = bits > 0 && (bits & (1 << i)) != 0 ? 1 : 0;
    }

    public void clearTreeBubbles() { Arrays.fill(treeBubbleLevels, 0); }

    /** Les automatismes des dimensions achetés, par nom. */
    public Set<String> dimensionAutomations() { return Collections.unmodifiableSet(dimensionAutomations); }

    public void addDimensionAutomation(String name) {
        if (name != null && !name.isBlank()) dimensionAutomations.add(name);
    }

    /** Ceux qui sont coupés à part. */
    public Set<String> dimensionAutomationsOff() { return Collections.unmodifiableSet(dimensionAutomationsOff); }

    public void setDimensionAutomationOff(String name, boolean off) {
        if (name == null) return;
        if (off) dimensionAutomationsOff.add(name);
        else dimensionAutomationsOff.remove(name);
    }

    public void clearDimensionAutomations() {
        dimensionAutomations.clear();
        dimensionAutomationsOff.clear();
    }

    /** Vrai une fois l'onglet des hadrons payé dans cet univers. */
    public boolean hadronsOpen() { return hadronsOpen; }

    public void setHadronsOpen(boolean open) { hadronsOpen = open; }

    /** Le prix d'ouverture des hadrons, fixé quand l'onglet s'est proposé ; 0 avant. */
    public double hadronsPrice() { return hadronsPrice; }

    public void setHadronsPrice(double price) { hadronsPrice = Double.isFinite(price) && price > 0 ? price : 0; }

    /** Les hadrons de cette sorte assemblés. */
    public long hadrons(Hadron hadron) { return hadrons.getOrDefault(hadron, 0L); }

    public void setHadrons(Hadron hadron, long count) {
        if (hadron == null) return;
        if (count > 0) hadrons.put(hadron, count);
        else hadrons.remove(hadron);
    }

    /** L'énergie du vide gardée par la bulle du géant de gauche, en bas, et déjà comptée au cumul. */
    public double voidCarried() { return voidCarried; }

    public void setVoidCarried(double carried) { voidCarried = carried >= 0 && Double.isFinite(carried) ? carried : 0; }

    /** L'énergie du vide que la bulle du géant de gauche a versée au cumul. */
    public double voidTrickled() { return voidTrickled; }

    public void setVoidTrickled(double trickled) {
        voidTrickled = trickled >= 0 && Double.isFinite(trickled) ? trickled : 0;
    }

    /** L'énergie du vide accumulée déjà dépensée : le cumul affiché reste celui des univers formés. */
    public double voidSpent() { return voidSpent; }

    public void setVoidSpent(double spent) { voidSpent = spent >= 0 && Double.isFinite(spent) ? spent : 0; }

    /** Niveau d'une amélioration de la durée, dans l'ordre de {@code Game.DurationUpgrade} ; 0 hors des quatre. */
    public int durationLevel(int index) { return level(durationLevels, index); }

    public void setDurationLevel(int index, int level) { setLevel(durationLevels, index, level); }

    /** Univers créés après le premier. */
    public int extraUniverses() { return extraUniverses; }

    public void setExtraUniverses(int count) { extraUniverses = Math.max(0, count); }

    /** Graines d'univers : la ressource principale de l'onglet Univers. */
    public BigNum universeSeeds() { return universeSeeds; }

    public void setUniverseSeeds(BigNum seeds) {
        universeSeeds = seeds == null || seeds.sign() < 0 ? BigNum.ZERO : seeds;
    }

    /** Énergie du vide : la ressource du deuxième acte de la partie secondaire, le Vide. */
    public BigNum voidEnergy() { return voidEnergy; }

    public void setVoidEnergy(BigNum energy) {
        voidEnergy = energy == null || energy.sign() < 0 ? BigNum.ZERO : energy;
    }

    /** Vrai une fois le vide ouvert dans cet univers : payé en énergie du collisionneur. */
    public boolean voidOpen() { return voidOpen; }

    public void setVoidOpen(boolean open) { voidOpen = open; }

    /** Vrai une fois le trou noir du vide ouvert dans cet univers : au premier trou noir formé. */
    public boolean voidHoleOpen() { return voidHoleOpen; }

    public void setVoidHoleOpen(boolean open) { voidHoleOpen = open; }

    /** La part de l'expansion donnée au vide, de 0 à 1. */
    public double voidShare() { return voidShare; }

    public void setVoidShare(double share) {
        voidShare = Double.isFinite(share) ? Math.max(0, Math.min(1, share)) : 0.25;
    }

    /** Toutes les particules sacrifiées au trou noir du vide dans cet univers. */
    public double voidPrice() { return voidPrice; }

    public void setVoidPrice(double price) { voidPrice = Double.isFinite(price) && price > 0 ? price : 0; }

    public BigNum voidSacrificed() { return voidSacrificed; }

    public void setVoidSacrificed(BigNum sacrificed) {
        voidSacrificed = sacrificed == null || sacrificed.sign() < 0 ? BigNum.ZERO : sacrificed;
    }

    /** Niveau d'une amélioration du vide, dans l'ordre de {@code Game.VoidUpgrade} ; 0 hors des huit. */
    public int voidLevel(int index) { return level(voidLevels, index); }

    public void setVoidLevel(int index, int level) { setLevel(voidLevels, index, level); }

    /** Le vide de chaque univers formé : son numéro → l'énergie du vide qu'il restait à sa formation. */
    public Map<String, Double> voidHistory() { return Collections.unmodifiableMap(voidHistory); }

    /** Note l'énergie du vide qu'il restait à la formation de cet univers. */
    public void noteVoidHistory(String universe, double stock) {
        if (universe == null || !Double.isFinite(stock) || stock < 0) return;
        voidHistory.put(universe, stock);
    }

    /** Le cumul : l'énergie du vide qu'il restait à la formation de chaque univers, additionnée. */
    public void clearVoidHistory() { voidHistory.clear(); }

    /** Le cumul, plus ce que la bulle du géant de gauche y a versé. */
    public double voidAccumulated() {
        double total = voidTrickled;
        for (double stock : voidHistory.values()) total += stock;
        return total;
    }

    public void setCometsCaught(int count) { cometsCaught = Math.max(0, count); }

    /** L'anomalie du ciel à saisir en ce moment, par son nom ; {@code null} s'il n'y en a pas. */
    public String skyEvent() { return skyEvent; }

    public void setSkyEvent(String event) { skyEvent = event == null || event.isBlank() ? null : event; }

    /** Secondes pendant lesquelles l'anomalie reste à saisir ; 0 quand il n'y en a pas. */
    public double skyEventVisible() { return skyEventVisible; }

    public void setSkyEventVisible(double seconds) {
        skyEventVisible = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes avant la prochaine anomalie ; 0 tant qu'elle n'est pas tirée. */
    public double skyEventWait() { return skyEventWait; }

    public void setSkyEventWait(double seconds) { skyEventWait = Double.isNaN(seconds) ? 0 : Math.max(0, seconds); }

    /** Secondes pendant lesquelles le vent de particules souffle encore (il peut être négatif juste après sa fin). */
    public double windLeft() { return windLeft; }

    public void setWindLeft(double seconds) { windLeft = Double.isNaN(seconds) ? 0 : seconds; }

    /** Nombre d'anomalies du ciel saisies depuis le début du jeu. */
    public int skyEventsCaught() { return skyEventsCaught; }

    public void setSkyEventsCaught(int count) { skyEventsCaught = Math.max(0, count); }

    /** Les records du joueur : nom d'une étape → temps de jeu le plus court auquel elle a été atteinte. */
    public Map<String, Double> records() { return Collections.unmodifiableMap(records); }

    public String playerName() { return playerName; }

    /** Le nom du joueur, sans espaces autour et coupé à {@link #PLAYER_NAME_MAX} caractères ; {@code null} le vide. */
    public void setPlayerName(String name) {
        String clean = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        playerName = clean.length() > PLAYER_NAME_MAX ? clean.substring(0, PLAYER_NAME_MAX).strip() : clean;
    }

    public String avatar() { return avatar; }

    public void setAvatar(String id) { avatar = id == null ? "" : id.strip(); }

    public String chosenTitle() { return chosenTitle; }

    public void setChosenTitle(String name) { chosenTitle = name == null ? "" : name; }

    /** Les titres obtenus et le temps de jeu de chacun. */
    public Map<String, Double> earnedTitles() { return Collections.unmodifiableMap(earnedTitles); }

    /** Note un titre obtenu ; le premier temps reste. */
    public void earnTitle(String name, double seconds) {
        if (name == null || name.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        earnedTitles.putIfAbsent(name, seconds);
    }

    public Map<String, Double> foundMolecules() { return Collections.unmodifiableMap(foundMolecules); }

    public Map<String, Double> foundSubstances() { return Collections.unmodifiableMap(foundSubstances); }

    public Map<String, Double> foundAssemblies() { return Collections.unmodifiableMap(foundAssemblies); }

    public Map<String, Double> foundBodies() { return Collections.unmodifiableMap(foundBodies); }

    /** Note une découverte d'un album ({@code molecules}, {@code substances}, {@code assemblies} ou {@code bodies}) ; vrai si elle est nouvelle. */
    public boolean noteFound(String album, String id, double seconds) {
        Map<String, Double> map = switch (album) {
            case "molecules" -> foundMolecules;
            case "substances" -> foundSubstances;
            case "assemblies" -> foundAssemblies;
            case "bodies" -> foundBodies;
            default -> throw new IllegalArgumentException("Album inconnu : " + album);
        };
        if (id == null || id.isBlank() || map.containsKey(id)) return false;
        map.put(id, Double.isNaN(seconds) || seconds < 0 ? 0 : seconds);
        return true;
    }

    /** Note un record ; un temps négatif ou qui n'est pas un nombre est refusé. */
    public void setRecord(String step, double seconds) {
        if (step == null || step.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        records.put(step, seconds);
    }

    /** Oublie tous les records : la seule chose que la remise à zéro du jeu ne fait pas d'elle-même. */
    public void clearRecords() { records.clear(); }

    /** Le défi de Big Bang en cours, ou {@code null}. */
    public String activeBangChallenge() { return activeBangChallenge; }

    public void setActiveBangChallenge(String challenge) {
        activeBangChallenge = challenge == null || challenge.isBlank() ? null : challenge;
    }

    /** Les paliers de Big Bang payés : un palier atteint n'agit qu'une fois payé. */
    public Set<String> boughtMilestones() { return Collections.unmodifiableSet(boughtMilestones); }

    public void addBoughtMilestone(String name) { if (name != null && !name.isBlank()) boughtMilestones.add(name); }

    /** Les défis de Big Bang réussis. */
    public Set<String> completedBangChallenges() { return Collections.unmodifiableSet(completedBangChallenges); }

    public void addCompletedBangChallenge(String challenge) {
        if (challenge != null && !challenge.isBlank()) completedBangChallenges.add(challenge);
    }

    /** Le meilleur temps de chaque défi de Big Bang réussi, en secondes. */
    public Map<String, Double> bangChallengeTimes() { return Collections.unmodifiableMap(bangChallengeTimes); }

    /** Le temps de jeu auquel le défi de Big Bang en cours a commencé, en secondes. */
    public double bangChallengeStarted() { return bangChallengeStarted; }

    public void setBangChallengeStarted(double timePlayed) {
        bangChallengeStarted = Double.isNaN(timePlayed) ? 0 : Math.max(0, timePlayed);
    }

    public void setBangChallengeTime(String challenge, double seconds) {
        if (challenge == null || challenge.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        bangChallengeTimes.put(challenge, seconds);
    }
}
