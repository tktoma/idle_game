package idle.core;

import idle.core.sky.Cosmos;
import idle.core.sky.SolarSystem;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L'état de la partie : ce qui s'ouvre en cours de route (plafonds d'état, skins, régime, synergie,
 * épreuves, fenêtres d'explication, recherches, maturation, automatismes de la matière, galaxie, cosmos).
 */
public abstract class GameStateUnlocks extends GameStateBase {

    /** Seul {@link GameState} prolonge la chaîne. */
    GameStateUnlocks() {
    }


    /**
     * Le plafond de la création automatique pour un état de la matière (version 0.085) : la part de
     * l'espace que ses molécules peuvent occuper, de 0 à 1 ; 1 (sans plafond) s'il n'est pas réglé.
     */
    public double stateCap(String matter) {
        return stateCaps.getOrDefault(matter, 1.0);
    }

    /** Règle un plafond ; 1 ou plus l'efface. */
    public void setStateCap(String matter, double cap) {
        if (Double.isNaN(cap) || cap >= 1) stateCaps.remove(matter);
        else stateCaps.put(matter, Math.max(0, cap));
    }

    /** Les plafonds réglés, état par état. */
    public Map<String, Double> stateCaps() {
        return Collections.unmodifiableMap(stateCaps);
    }

    // ----- Le deuxième univers (version 0.087) -----

    /** Vrai pendant la partie qui mène à l'univers suivant : les anciens onglets sont revenus. */
    public boolean replaying() {
        return replaying;
    }

    public void setReplaying(boolean on) {
        replaying = on;
    }

    /** Les fusions du jeu au moment où le dernier univers s'est formé : l'onglet Skins s'ouvre cinq fusions plus tard. */
    public long fusionsAtUniverse() {
        return fusionsAtUniverse;
    }

    public long explosionsAtUniverse() {
        return explosionsAtUniverse;
    }

    public void setExplosionsAtUniverse(long explosions) {
        explosionsAtUniverse = Math.max(0, explosions);
    }

    public void setFusionsAtUniverse(long fusions) {
        fusionsAtUniverse = Math.max(0, fusions);
    }

    /** Vrai une fois l'onglet Skins ouvert : il ne se referme plus. */
    public boolean skinsOpen() {
        return skinsOpen;
    }

    public void setSkinsOpen(boolean open) {
        skinsOpen = open;
    }

    /** Les skins débloqués, par identifiant. */
    public Set<String> unlockedSkins() {
        return Collections.unmodifiableSet(unlockedSkins);
    }

    public void unlockSkin(String id) {
        if (id != null && !id.isBlank()) unlockedSkins.add(id);
    }

    public void lockSkin(String id) {
        unlockedSkins.remove(id);
        chosenSkins.values().remove(id);
    }

    /** Le skin choisi pour chaque visuel ; absent : l'apparence classique. */
    public Map<String, String> chosenSkins() {
        return Collections.unmodifiableMap(chosenSkins);
    }

    public void chooseSkin(String visual, String skin) {
        if (skin == null || skin.isBlank()) chosenSkins.remove(visual);
        else chosenSkins.put(visual, skin);
    }

    public boolean regimeOpen() {
        return regimeOpen;
    }

    public void setRegimeOpen(boolean open) {
        regimeOpen = open;
    }

    /** Secondes de régime rapide qu'il reste ; 0 hors régime. */
    public double regimeLeft() {
        return regimeLeft;
    }

    public void setRegimeLeft(double seconds) {
        regimeLeft = Math.max(0, seconds);
    }

    /** Secondes de repos du régime qu'il reste avant de pouvoir le relancer. */
    public double regimeRest() {
        return regimeRest;
    }

    public void setRegimeRest(double seconds) {
        regimeRest = Math.max(0, seconds);
    }

    public boolean synergyOpen() {
        return synergyOpen;
    }

    public void setSynergyOpen(boolean open) {
        synergyOpen = open;
    }

    /** Fusions de suite sans atteindre le plafond d'atomes, pour la prime de cycle parfait. */
    public int perfectStreak() {
        return perfectStreak;
    }

    public void setPerfectStreak(int streak) {
        perfectStreak = Math.max(0, streak);
    }

    /** Secondes qu'il reste à la prime de cycle parfait. */
    public double perfectLeft() {
        return perfectLeft;
    }

    public void setPerfectLeft(double seconds) {
        perfectLeft = Math.max(0, seconds);
    }

    /** Le choix du petit arbre pour ce cycle, par son nom ; {@code null} tant qu'il n'est pas fait. */
    public String cycleChoice() {
        return cycleChoice;
    }

    public void setCycleChoice(String choice) {
        cycleChoice = choice == null || choice.isBlank() ? null : choice;
    }

    /** La famille préférée de la synthèse, par son nom ; {@code null} sans préférence. */
    public String preferredFamily() {
        return preferredFamily;
    }

    public void setPreferredFamily(String family) {
        preferredFamily = family == null || family.isBlank() ? null : family;
    }

    /** Tirages faits depuis le choix de la famille préférée, ou depuis la dernière explosion. */
    public int preferenceDraws() {
        return preferenceDraws;
    }

    public void setPreferenceDraws(int draws) {
        preferenceDraws = Math.max(0, draws);
    }

    /** Éléments gratuits déjà pris depuis la dernière explosion. */
    public int freeElementsUsed() {
        return freeElementsUsed;
    }

    public void setFreeElementsUsed(int used) {
        freeElementsUsed = Math.max(0, used);
    }

    // ----- Les skins à obtenir (version 0.088) -----

    /** L'épreuve en cours, par son nom ; {@code null} sans épreuve. */
    public String activeTrial() {
        return activeTrial;
    }

    public void setActiveTrial(String trial) {
        activeTrial = trial == null || trial.isBlank() ? null : trial;
    }

    /** Temps de jeu au début de l'épreuve en cours. */
    public double trialStart() {
        return trialStart;
    }

    public void setTrialStart(double time) {
        trialStart = time;
    }

    /** Ce que l'épreuve en cours a noté au départ (une énergie produite, un rythme). */
    public double trialBase() {
        return trialBase;
    }

    public void setTrialBase(double base) {
        trialBase = base;
    }

    /** Ce que l'épreuve en cours a compté (croisements captés, fusions). */
    public int trialCount() {
        return trialCount;
    }

    public void setTrialCount(int count) {
        trialCount = Math.max(0, count);
    }

    public Set<String> seenPopups() {
        return Collections.unmodifiableSet(seenPopups);
    }

    /** Retient qu'une fenêtre a été montrée ; vrai si elle ne l'avait pas encore été. */
    public boolean addSeenPopup(String id) {
        return id != null && !id.isBlank() && seenPopups.add(id);
    }

    public boolean popupsPrimed() {
        return popupsPrimed;
    }

    public void setPopupsPrimed(boolean primed) {
        popupsPrimed = primed;
    }

    public Set<String> completedTrials() {
        return Collections.unmodifiableSet(completedTrials);
    }

    public void addCompletedTrial(String trial) {
        if (trial != null && !trial.isBlank()) completedTrials.add(trial);
    }

    /** Le record de reconstruction, en secondes : de la fusion au dixième générateur ; 0 sans record. */
    public double rebuildRecord() {
        return rebuildRecord;
    }

    public void setRebuildRecord(double seconds) {
        rebuildRecord = Math.max(0, seconds);
    }

    /** Vrai entre une fusion et le dixième générateur suivant : une reconstruction est chronométrée. */
    public boolean rebuildPending() {
        return rebuildPending;
    }

    public void setRebuildPending(boolean pending) {
        rebuildPending = pending;
    }

    /** Les recherches facultatives faites : « prism », « dense1 » à « dense4 ». */
    public Set<String> researches() {
        return Collections.unmodifiableSet(researches);
    }

    public void addResearch(String research) {
        if (research != null && !research.isBlank()) researches.add(research);
    }

    /** Vrai si la réservation par recette est allumée (version 0.085). */
    public boolean recipeReservation() {
        return recipeReservation;
    }

    public void setRecipeReservation(boolean on) {
        recipeReservation = on;
    }

    /** Vrai si les bifurcations se rouvriront à la prochaine explosion (version 0.084). */
    public boolean forkResetArmed() {
        return forkResetArmed;
    }

    public void setForkResetArmed(boolean armed) {
        forkResetArmed = armed;
    }

    public void setSolarSystems(List<SolarSystem> systems) {
        solarSystems.clear();
        solarSystems.addAll(systems);
        moleculesVersion++;
    }

    /** Les astres qui mûrissent encore, avec les secondes qu'il leur reste, dans l'ordre de leur formation. */
    public Map<String, Double> maturing() {
        return Collections.unmodifiableMap(maturing);
    }

    /** Secondes de maturation qu'il reste à cet astre ; 0 s'il est mûr ou pas formé. */
    public double maturingLeft(String bodyId) {
        return maturing.getOrDefault(bodyId, 0.0);
    }

    /** Fixe ce qu'il reste à mûrir à un astre ; à 0, il est mûr, et ses bonus se recalculent. */
    public void setMaturing(String bodyId, double seconds) {
        boolean was = maturing.containsKey(bodyId);
        if (Double.isNaN(seconds) || seconds <= 0) {
            maturing.remove(bodyId);
        } else {
            maturing.put(bodyId, seconds);
        }
        if (was != maturing.containsKey(bodyId)) moleculesVersion++;
    }

    /** Les astres formés, dans l'ordre où ils l'ont été. */
    public List<String> bodies() {
        if (bodiesSeen == null || bodiesSeen.size() != bodies.size()) bodiesSeen = List.copyOf(bodies);
        return bodiesSeen;
    }

    /** Vrai si l'appui automatique est en marche (il ne sert qu'une fois acquis). Ni l'explosion ni le Big Bang ne le coupent. */
    public boolean autoHold() {
        return autoHold;
    }

    /** Met en marche ou coupe l'appui automatique. */
    public void setAutoHold(boolean enabled) {
        autoHold = enabled;
    }

    /** Vrai si la création automatique des molécules est en marche (elle ne sert qu'une fois acquise). Ni l'explosion ni le Big Bang ne la coupent. */
    public boolean autoFollowList() {
        return autoFollowList;
    }

    public void setAutoFollowList(boolean follow) {
        autoFollowList = follow;
    }

    public boolean autoFollowMatter() { return autoFollowMatter; }
    public void setAutoFollowMatter(boolean enabled) { autoFollowMatter = enabled; }

    public boolean autoMolecules() {
        return autoMolecules;
    }

    /** Met en marche ou coupe la création automatique des molécules. */
    public void setAutoMolecules(boolean enabled) {
        autoMolecules = enabled;
    }

    /** Vue en lecture seule des sortes que la création automatique entretient, dans l'ordre où elles ont été choisies. */
    public List<String> automatedMolecules() {
        return List.copyOf(automatedMolecules);
    }

    /** Vrai si la création automatique entretient cette sorte. */
    public boolean isMoleculeAutomated(String moleculeId) {
        return automatedMolecules.contains(moleculeId);
    }

    /** Confie une sorte à la création automatique, ou la lui retire. Ni l'explosion ni le Big Bang ne changent ce choix. */
    public void setMoleculeAutomated(String moleculeId, boolean automated) {
        if (moleculeId == null || moleculeId.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        if (automated) automatedMolecules.add(moleculeId);
        else automatedMolecules.remove(moleculeId);
    }

    /** Vrai si la galaxie est formée. Ni l'explosion ni le Big Bang ne la défont. */
    public boolean hasGalaxy() {
        return cosmos >= 1;
    }

    /** Note la galaxie comme formée, ou défaite : défaite, les échelles au-dessus le sont aussi. */
    public void setGalaxy(boolean formed) {
        setCosmosLevel(formed ? Math.max(cosmos, 1) : 0);
    }

    /** Nombre d'échelles du cosmos formées, dans l'ordre de {@link Cosmos} : 0 = aucune, 3 = jusqu'à l'univers. */
    public int cosmosLevel() {
        return cosmos;
    }

    /** Fixe le nombre d'échelles du cosmos formées. */
    public void setCosmosLevel(int level) {
        if (level < 0 || level > Cosmos.values().length) throw new IllegalArgumentException("Échelle invalide : " + level);
        if (cosmos != level) moleculesVersion++;
        cosmos = level;
    }
}
