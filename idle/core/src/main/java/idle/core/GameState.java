package idle.core;

import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.CollisionTier;
import idle.core.collider.Quark;
import idle.core.elements.ElementCategory;
import idle.core.math.BigNum;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.probes.ProbeUpgrade;
import idle.core.save.SaveData;
import idle.core.sky.SolarSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Tout ce qui change pendant une partie, et rien d'autre.
 * C'est cet objet qui est écrit dans la sauvegarde ({@link SaveCodec}).
 *
 * <p>Aucune règle du jeu ici : les règles sont dans {@link Game}.
 *
 * <p>Le code est rangé en parties (version 0.098) : une chaîne de classes abstraites, une par thème, dont
 * celle-ci est le bout ; le reste du programme ne voit que {@code GameState}. De la base au bout :
 * {@link GameStateBase}, {@link GameStateUnlocks}, {@link GameStateCollider}, {@link GameStateResets}.
 * Quand une partie a besoin d'une méthode définie plus loin dans la chaîne, elle la déclare abstraite en
 * tête, sous « Ce que fournissent les parties suivantes ».
 */
public final class GameState extends GameStateResets {

    // ------------------------------------------------------------------
    // Sauvegarde
    // ------------------------------------------------------------------

    /**
     * Range toute la partie dans une sauvegarde. Ce qui se recalcule (les compteurs de version,
     * les listes gardées pour aller vite) n'y est pas ; les ensembles sans ordre sont triés, pour
     * que deux parties identiques donnent le même texte.
     *
     * <p>Tout champ ajouté à cette classe doit être écrit ici et relu dans {@link #load(SaveData)} :
     * un test compare champ par champ une partie à sa copie relue, et échoue sinon.
     */
    public void save(SaveData data) {
        data.put("started", started);
        data.put("particles", particles);
        data.put("atoms", atoms);
        data.put("totalAtoms", totalAtoms);
        data.put("timeSinceFusion", timeSinceFusion);
        data.put("upgradeCalm", upgradeCalm);
        data.put("bangCalm", bangCalm);
        data.putDoubleList("formations", formations);
        data.put("timePlayed", timePlayed);
        data.putInts("upgradeLevels", new TreeMap<>(upgradeLevels));
        data.putList("ownedAutomations", new java.util.TreeSet<>(ownedAutomations));
        data.putList("enabledAutomations", new java.util.TreeSet<>(enabledAutomations));
        data.putInts("automationSpeedLevels", new TreeMap<>(automationSpeedLevels));
        data.putDoubles("automationTimers", new TreeMap<>(automationTimers));
        Map<String, Integer> copies = new java.util.LinkedHashMap<>();
        elements.forEach((number, count) -> copies.put(String.valueOf(number), count));
        data.putInts("elements", copies);
        data.put("synthesisCount", synthesisCount);
        data.put("synthesisTarget", synthesisTarget == null ? null : synthesisTarget.name());
        data.put("synthesisTries", synthesisTries);
        data.put("drySyntheses", drySyntheses);
        data.put("darkMatter", darkMatter);
        data.put("darkMatterSpent", darkMatterSpent);
        data.put("darkMatterSize", darkMatterSize);
        data.put("explosions", explosions);
        data.put("tableWeightLevel", tableWeightLevel);
        data.putInts("darkUpgradeLevels", new TreeMap<>(darkUpgradeLevels));
        data.put("fusionThreshold", fusionThreshold);
        data.put("synthesisReserve", synthesisReserve);
        data.put("holdLocked", holdLocked);
        data.put("activeChallenge", activeChallenge == null ? null : SaveData.escape(activeChallenge));
        data.putList("completedChallenges", new java.util.TreeSet<>(completedChallenges));
        data.putDoubles("challengeTimes", new TreeMap<>(challengeTimes));
        data.put("decayDebt", decayDebt);
        data.putList("achievements", achievements);
        data.putList("enabledDarkAutomations", new java.util.TreeSet<>(enabledDarkAutomations));
        data.putDoubles("darkAutomationTimers", new TreeMap<>(darkAutomationTimers));
        data.put("bigBangs", bigBangs);
        // Les molécules, par suites dans l'ordre de leur création : « sorte:nombre ». Le total par sorte s'en déduit.
        StringBuilder log = new StringBuilder();
        for (int run = 0; run < logKinds.size(); run++) {
            if (run > 0) log.append(',');
            log.append(SaveData.escape(logKinds.get(run))).append(':').append(logEnds[run] - (run == 0 ? 0 : logEnds[run - 1]));
        }
        data.put("moleculeLog", log.toString());
        data.putList("substances", substances);
        data.putList("assemblies", assemblies);
        data.putList("bodies", bodies);
        data.putDoubles("maturing", new TreeMap<>(maturing));
        List<String> systems = new ArrayList<>();
        for (SolarSystem system : solarSystems) {
            systems.add(system.star() + "|" + String.join(",", system.planets()) + "|" + String.join(",", system.moons()));
        }
        data.putList("solarSystems", systems);
        data.put("cosmos", cosmos);
        data.put("autoHold", autoHold);
        data.put("autoMolecules", autoMolecules);
        data.put("autoFollowList", autoFollowList);
        data.put("autoFollowMatter", autoFollowMatter);
        data.putList("automatedMolecules", automatedMolecules);
        data.putList("spaceUpgrades", spaceUpgrades);
        data.putList("darkPlan", darkPlan);
        data.putList("savedDarkPlans", savedDarkPlans);
        List<String> plannedSystems = new ArrayList<>();
        for (SolarSystem system : systemPlan) plannedSystems.add(system.line());
        data.putList("systemPlan", plannedSystems);
        data.putList("savedSystemPlans", savedSystemPlans);
        data.put("forkResetArmed", forkResetArmed);
        data.putList("plannedBodies", plannedBodies);
        data.putDoubles("stateCaps", new TreeMap<>(stateCaps));
        data.put("recipeReservation", recipeReservation);
        data.put("replaying", replaying);
        data.put("fusionsAtUniverse", fusionsAtUniverse);
        data.put("explosionsAtUniverse", explosionsAtUniverse);
        data.put("skinsOpen", skinsOpen);
        data.putList("unlockedSkins", unlockedSkins);
        List<String> chosen = new ArrayList<>();
        chosenSkins.forEach((visual, skin) -> chosen.add(visual + ">" + skin));
        data.putList("chosenSkins", chosen);
        data.put("regimeOpen", regimeOpen);
        data.put("regimeLeft", regimeLeft);
        data.put("regimeRest", regimeRest);
        data.put("synergyOpen", synergyOpen);
        data.put("perfectStreak", perfectStreak);
        data.put("perfectLeft", perfectLeft);
        if (cycleChoice != null) data.putList("cycleChoice", List.of(cycleChoice));
        if (preferredFamily != null) data.putList("preferredFamily", List.of(preferredFamily));
        data.put("preferenceDraws", preferenceDraws);
        data.put("freeElementsUsed", freeElementsUsed);
        if (activeTrial != null) data.putList("activeTrial", List.of(activeTrial));
        data.put("trialStart", trialStart);
        data.put("trialBase", trialBase);
        data.put("trialCount", trialCount);
        data.putList("completedTrials", completedTrials);
        data.putList("seenPopups", seenPopups);
        data.put("popupsPrimed", popupsPrimed);
        data.put("rebuildRecord", rebuildRecord);
        data.put("rebuildPending", rebuildPending);
        data.putList("researches", researches);
        data.put("automationPaused", automationPaused);
        data.putList("favoriteMolecules", favoriteMolecules);
        data.put("autoGather", autoGather);
        data.put("autoForm", autoForm);
        data.put("autoMoleculeReserve", autoMoleculeReserve);
        data.put("explosionRecord", explosionRecord);
        data.put("cometWait", cometWait);
        data.put("cometVisible", cometVisible);
        data.put("cometBoost", cometBoost);
        data.put("cometsCaught", cometsCaught);
        if (skyEvent != null) data.put("skyEvent", skyEvent);
        data.put("skyEventVisible", skyEventVisible);
        data.put("skyEventWait", skyEventWait);
        data.put("windLeft", windLeft);
        data.put("skyEventsCaught", skyEventsCaught);
        data.put("endSeen", endSeen);
        data.put("universeFormed", universeFormed);
        data.put("extraUniverses", extraUniverses);
        data.put("universeSeeds", universeSeeds);
        data.put("voidEnergy", voidEnergy);
        data.put("voidOpen", voidOpen);
        data.put("voidHoleOpen", voidHoleOpen);
        data.put("voidShare", voidShare);
        data.put("voidPrice", voidPrice);
        data.put("voidSacrificed", voidSacrificed);
        for (int i = 0; i < VOID_UPGRADES; i++) data.put("voidLevel" + i, voidLevel(i));
        data.putDoubles("voidHistory", voidHistory);
        data.put("dimensionShards", dimensionShards);
        data.put("linePhase", linePhase);
        data.put("lineYieldLevel", lineYieldLevel);
        data.put("lineTicksLevel", lineTicksLevel);
        data.put("lineSpeedLevel", lineSpeedLevel);
        data.put("lineBallsLevel", lineBallsLevel);
        data.put("lineCountLevel", lineCountLevel);
        data.put("dimensionShardsEarned", dimensionShardsEarned);
        data.put("seedSprouted", seedSprouted);
        data.put("wateringCan", wateringCan);
        data.put("twigOpened", twigOpened);
        data.put("seedUniversesOpened", seedUniversesOpened);
        data.put("seedWaterings", seedWaterings);
        data.put("planeShards", planeShards);
        data.put("planeShardsEarned", planeShardsEarned);
        for (int i = 0; i < 5; i++) data.put("planeLevel" + i, planeLevel(i));
        data.put("volumeShards", volumeShards);
        data.put("volumeShardsEarned", volumeShardsEarned);
        for (int i = 0; i < VOLUME_UPGRADES; i++) data.put("volumeLevel" + i, volumeLevel(i));
        data.put("durationShards", durationShards);
        data.put("durationShardsEarned", durationShardsEarned);
        for (int i = 0; i < DURATION_UPGRADES; i++) data.put("durationLevel" + i, durationLevel(i));
        for (int i = 0; i < SEED_UPGRADES; i++) data.put("seedUpgrade" + i, seedUpgradeLevel(i));
        data.put("chronoStored", chronoStored);
        data.put("chronoSpeed", chronoSpeed);
        data.putInts("universeUpgrades", new TreeMap<>(universeUpgrades));
        data.put("pulseCharges", pulseCharges);
        data.put("pulses", pulses);
        data.put("colliderEnergy", colliderEnergy);
        Map<String, Double> generatorAmounts = new TreeMap<>();
        Map<String, Integer> generatorsBought = new TreeMap<>();
        colliderGenerators.forEach((generator, amount) -> generatorAmounts.put(generator.name(), amount));
        colliderGeneratorsBought.forEach((generator, bought) -> generatorsBought.put(generator.name(), bought));
        data.putDoubles("colliderGenerators", generatorAmounts);
        data.putInts("colliderGeneratorsBought", generatorsBought);
        data.put("colliderPhase", colliderPhase);
        data.put("colliderCrossings", colliderCrossings);
        data.put("colliderCaught", colliderCaught);
        Map<String, Integer> colliderLevels = new TreeMap<>();
        colliderUpgrades.forEach((upgrade, level) -> colliderLevels.put(upgrade.name(), level));
        data.putInts("colliderUpgrades", colliderLevels);
        List<String> paused = new ArrayList<>();
        for (ColliderUpgrade upgrade : colliderPaused) paused.add(upgrade.name());
        data.putList("colliderPaused", paused);
        Map<String, Integer> quarkCounts = new TreeMap<>();
        quarks.forEach((quark, count) -> quarkCounts.put(quark.name(), count));
        data.putInts("quarks", quarkCounts);
        data.put("colliderEnergyProduced", colliderEnergyProduced);
        List<String> running = new ArrayList<>();
        for (Collision collision : collisions) {
            running.add(collision.tier().name() + ";" + collision.left() + ";" + collision.resonated() + ";" + collision.doubled());
        }
        data.putList("collisions", running);
        data.put("collisionRuns", collisionRuns);
        data.put("topLeft", topLeft);
        data.put("overheatLeft", overheatLeft);
        List<String> flying = new ArrayList<>();
        for (Probe probe : probes) {
            flying.add(probe.destination().name() + ";" + probe.trip().name() + ";" + probe.left() + ";" + probe.total() + ";" + probe.id());
        }
        data.putList("probes", flying);
        List<String> waiting = new ArrayList<>();
        for (ProbeReward reward : probeRewards) {
            waiting.add(reward.type().name() + ";" + reward.target() + ";" + reward.value() + ";" + reward.destination().name() + ";" + reward.probe());
        }
        data.putList("probeRewards", waiting);
        Map<String, Integer> upgradeLevels = new TreeMap<>();
        probeUpgrades.forEach((upgrade, level) -> upgradeLevels.put(upgrade.name(), level));
        data.putInts("probeUpgrades", upgradeLevels);
        data.put("probeRuns", probeRuns);
        data.put("probeLaunches", probeLaunches);
        data.put("probeSamples", probeSamples);
        data.put("probeBoostLeft", probeBoostLeft);
        data.putDoubles("records", new TreeMap<>(records));
        data.put("activeBangChallenge", activeBangChallenge == null ? null : SaveData.escape(activeBangChallenge));
        data.putList("completedBangChallenges", new java.util.TreeSet<>(completedBangChallenges));
        data.putList("boughtMilestones", new java.util.TreeSet<>(boughtMilestones));
        data.putDoubles("bangChallengeTimes", new TreeMap<>(bangChallengeTimes));
        data.put("bangChallengeStarted", bangChallengeStarted);
        data.putList("atomUpgradeOrder", atomUpgradeOrder);
        data.put("atomUpgradeOrdered", atomUpgradeOrdered);
        data.putList("automationOrder", automationOrder);
        data.put("automationOrdered", automationOrdered);
        data.put("space", space);
        stats.save(data, "stats.");
    }

    /**
     * Remplace toute la partie par celle d'une sauvegarde. Une clé absente laisse la valeur d'une
     * partie neuve : une sauvegarde plus ancienne que le jeu se relit donc sans rien casser.
     *
     * @throws RuntimeException si une valeur est illisible ou impossible (nombre négatif, texte à la
     *                          place d'un nombre) : l'état est alors à moitié rempli, et ne doit pas servir
     */
    public void load(SaveData data) {
        reset();
        started = data.flag("started", false);
        setParticles(data.big("particles", BigNum.ZERO));
        setAtoms(data.big("atoms", BigNum.ZERO));
        setTotalAtoms(data.big("totalAtoms", BigNum.ZERO));
        setTimeSinceFusion(data.real("timeSinceFusion", 0));
        setUpgradeCalm(data.real("upgradeCalm", 0));
        setBangCalm(data.real("bangCalm", 0));
        List<Double> made = data.doubleList("formations");
        for (int generator = 0; generator < made.size(); generator++) setFormation(generator, made.get(generator));
        timePlayed = Math.max(0, data.real("timePlayed", 0));
        data.ints("upgradeLevels").forEach(this::setLevel);
        ownedAutomations.addAll(data.list("ownedAutomations"));
        enabledAutomations.addAll(data.list("enabledAutomations"));
        data.ints("automationSpeedLevels").forEach(this::setAutomationSpeedLevel);
        data.doubles("automationTimers").forEach(this::setAutomationTimer);
        data.ints("elements").forEach((number, count) -> setElementCount(Integer.parseInt(number), count));
        setSynthesisCount(data.whole("synthesisCount", 0));
        String target = data.text("synthesisTarget", null);
        synthesisTarget = null;
        if (target != null) {
            try {
                synthesisTarget = ElementCategory.valueOf(target);
            } catch (IllegalArgumentException unknown) {
                // une famille d'une autre version (version 0.126) : la synthèse repart sans cible, la partie se relit
            }
        }
        setSynthesisTries(data.whole("synthesisTries", 0));
        setDrySyntheses(data.whole("drySyntheses", 0));
        setDarkMatter(data.big("darkMatter", BigNum.ZERO));
        setDarkMatterSpent(data.big("darkMatterSpent", BigNum.ZERO));
        setDarkMatterSize(data.big("darkMatterSize", Game.DARK_MATTER_START_SIZE));
        setExplosions(data.whole("explosions", 0));
        setTableWeightLevel(data.whole("tableWeightLevel", 0));
        data.ints("darkUpgradeLevels").forEach(this::setDarkLevel);
        setFusionThreshold(data.whole("fusionThreshold", 0));
        setSynthesisReserve(data.big("synthesisReserve", BigNum.ZERO));
        holdLocked = data.flag("holdLocked", false);
        String challenge = data.text("activeChallenge", null);
        activeChallenge = challenge == null ? null : SaveData.unescape(challenge);
        completedChallenges.addAll(data.list("completedChallenges"));
        challengesVersion++;
        data.doubles("challengeTimes").forEach(this::setChallengeTime);
        setDecayDebt(data.real("decayDebt", 0));
        achievements.addAll(data.list("achievements"));
        achievementsVersion++;
        enabledDarkAutomations.addAll(data.list("enabledDarkAutomations"));
        data.doubles("darkAutomationTimers").forEach(this::setDarkAutomationTimer);
        setBigBangs(data.whole("bigBangs", 0));
        String log = data.text("moleculeLog", "");
        if (!log.isEmpty()) {
            for (String run : log.split(",", -1)) {
                int colon = run.lastIndexOf(':');
                addMolecules(SaveData.unescape(run.substring(0, colon)), Integer.parseInt(run.substring(colon + 1)));
            }
        }
        substances.addAll(data.list("substances"));
        assemblies.addAll(data.list("assemblies"));
        bodies.addAll(data.list("bodies"));
        solarSystems.clear();
        for (String line : data.list("solarSystems")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 3 || parts[0].isBlank()) continue;
            solarSystems.add(new SolarSystem(parts[0], parts[1].isEmpty() ? List.of() : List.of(parts[1].split(",")),
                    parts[2].isEmpty() ? List.of() : List.of(parts[2].split(","))));
        }
        maturing.clear();
        data.doubles("maturing").forEach((id, left) -> {
            if (bodies.contains(id) && left > 0) maturing.put(id, left);
        });
        setCosmosLevel(data.whole("cosmos", 0));
        autoHold = data.flag("autoHold", true);
        autoMolecules = data.flag("autoMolecules", true);
        autoFollowList = data.flag("autoFollowList", true);
        autoFollowMatter = data.flag("autoFollowMatter", false);
        automatedMolecules.addAll(data.list("automatedMolecules"));
        spaceUpgrades.addAll(data.list("spaceUpgrades"));
        setDarkPlan(data.list("darkPlan"));
        readSlots(data, "savedDarkPlans", "savedDarkPlan", savedDarkPlans);
        systemPlan.clear();
        for (String line : data.list("systemPlan")) {
            SolarSystem system = SolarSystem.parse(line);
            if (system != null) systemPlan.add(system);
        }
        readSlots(data, "savedSystemPlans", "savedSystemPlan", savedSystemPlans);
        forkResetArmed = data.flag("forkResetArmed", false);
        setPlannedBodies(data.list("plannedBodies"));
        stateCaps.clear();
        data.doubles("stateCaps").forEach(this::setStateCap);
        recipeReservation = data.flag("recipeReservation", false);
        replaying = data.flag("replaying", false);
        fusionsAtUniverse = data.count("fusionsAtUniverse", 0);
        explosionsAtUniverse = data.count("explosionsAtUniverse", 0);
        skinsOpen = data.flag("skinsOpen", false);
        unlockedSkins.clear();
        for (String id : data.list("unlockedSkins")) unlockSkin(id);
        chosenSkins.clear();
        for (String pair : data.list("chosenSkins")) {
            int arrow = pair.indexOf('>');
            if (arrow > 0) chosenSkins.put(pair.substring(0, arrow), pair.substring(arrow + 1));
        }
        regimeOpen = data.flag("regimeOpen", false);
        setRegimeLeft(data.real("regimeLeft", 0));
        setRegimeRest(data.real("regimeRest", 0));
        synergyOpen = data.flag("synergyOpen", false);
        setPerfectStreak(data.whole("perfectStreak", 0));
        setPerfectLeft(data.real("perfectLeft", 0));
        setCycleChoice(String.join("", data.list("cycleChoice")));
        setPreferredFamily(String.join("", data.list("preferredFamily")));
        setPreferenceDraws(data.whole("preferenceDraws", 0));
        setFreeElementsUsed(data.whole("freeElementsUsed", 0));
        setActiveTrial(String.join("", data.list("activeTrial")));
        trialStart = data.real("trialStart", 0);
        trialBase = data.real("trialBase", 0);
        setTrialCount(data.whole("trialCount", 0));
        completedTrials.clear();
        for (String id : data.list("completedTrials")) {
            // La fuite était une épreuve en 0.114 : elle est devenue un défi (version 0.115).
            if (id.equals("LEAK")) { completedChallenges.add("challenge_leak"); challengesVersion++; continue; }
            addCompletedTrial(id);
        }
        if ("LEAK".equals(activeTrial)) setActiveTrial("");
        seenPopups.clear();
        for (String id : data.list("seenPopups")) addSeenPopup(id);
        popupsPrimed = data.flag("popupsPrimed", false);
        setRebuildRecord(data.real("rebuildRecord", 0));
        rebuildPending = data.flag("rebuildPending", false);
        researches.clear();
        for (String id : data.list("researches")) addResearch(id);
        automationPaused = data.flag("automationPaused", false);
        favoriteMolecules.addAll(data.list("favoriteMolecules"));
        autoGather = data.flag("autoGather", true);
        autoForm = data.flag("autoForm", true);
        setAutoMoleculeReserve(data.real("autoMoleculeReserve", 0));
        setExplosionRecord(data.real("explosionRecord", 0));
        setCometWait(data.real("cometWait", 0));
        setCometVisible(data.real("cometVisible", 0));
        setCometBoost(data.real("cometBoost", 0));
        setCometsCaught(data.whole("cometsCaught", 0));
        // Une anomalie d'une autre version est oubliée : elle repassera.
        String event = data.text("skyEvent", null);
        setSkyEvent(event != null && java.util.Arrays.stream(idle.core.sky.SkyEvent.values()).anyMatch(e -> e.name().equals(event)) ? event : null);
        setSkyEventVisible(skyEvent == null ? 0 : data.real("skyEventVisible", 0));
        setSkyEventWait(data.real("skyEventWait", 0));
        setWindLeft(data.real("windLeft", 0));
        setSkyEventsCaught(data.whole("skyEventsCaught", 0));
        // Une sauvegarde d'avant la 0.068 dont l'univers est déjà formé ne rouvre pas l'écran de fin.
        endSeen = data.flag("endSeen", data.whole("cosmos", 0) >= 3);
        // Une sauvegarde d'avant la 0.083 dont l'univers est formé : la remise à zéro se fera au chargement (Game.restored).
        universeFormed = data.flag("universeFormed", data.whole("cosmos", 0) >= 3);
        setExtraUniverses(data.whole("extraUniverses", 0));
        setUniverseSeeds(data.big("universeSeeds", BigNum.ZERO));
        setVoidEnergy(data.big("voidEnergy", BigNum.ZERO));
        // Le vide (version 0.129) : absent des sauvegardes d'avant, il part fermé, à zéro.
        setVoidOpen(data.flag("voidOpen", false));
        setVoidHoleOpen(data.flag("voidHoleOpen", false));
        setVoidShare(data.real("voidShare", 0.25));
        setVoidPrice(data.real("voidPrice", 0));
        setVoidSacrificed(data.big("voidSacrificed", BigNum.ZERO));
        for (int i = 0; i < VOID_UPGRADES; i++) setVoidLevel(i, data.whole("voidLevel" + i, 0));
        voidHistory.clear();
        data.doubles("voidHistory").forEach(this::noteVoidHistory);
        setDimensionShards(data.real("dimensionShards", 0));
        setLinePhase(data.real("linePhase", 0));
        setLineYieldLevel(data.whole("lineYieldLevel", 0));
        setLineTicksLevel(data.whole("lineTicksLevel", 0));
        setLineSpeedLevel(data.whole("lineSpeedLevel", 0));
        setLineBallsLevel(data.whole("lineBallsLevel", 0));
        setLineCountLevel(data.whole("lineCountLevel", 0));
        // Une sauvegarde de la 0.101 n'a pas ce total : les éclats en réserve en sont le minimum.
        setDimensionShardsEarned(data.real("dimensionShardsEarned", dimensionShards));
        setSeedSprouted(data.flag("seedSprouted", false));
        // Version 0.137 : une partie d'avant n'a pas d'arrosoir ; il se débloque d'un clic sur l'étagère.
        setWateringCan(data.flag("wateringCan", false));
        setSeedWaterings(data.count("seedWaterings", 0));
        // Une partie d'avant la 0.109 déjà dans l'univers 2 ouvert par la brindille : elle ne le rouvrira pas.
        setTwigOpened(data.flag("twigOpened", data.flag("replaying", false) && seedSprouted && seedWaterings >= GameUniverse.SEED_TWIG_WATERINGS));
        setSeedUniversesOpened(data.whole("seedUniversesOpened", twigOpened ? 1 : 0));
        setPlaneShards(data.real("planeShards", 0));
        setPlaneShardsEarned(data.real("planeShardsEarned", 0));
        for (int i = 0; i < 5; i++) setPlaneLevel(i, data.whole("planeLevel" + i, 0));
        // Le volume (version 0.128) : absent des sauvegardes d'avant, il part de zéro.
        setVolumeShards(data.real("volumeShards", 0));
        setVolumeShardsEarned(data.real("volumeShardsEarned", 0));
        for (int i = 0; i < VOLUME_UPGRADES; i++) setVolumeLevel(i, data.whole("volumeLevel" + i, 0));
        // La durée (version 0.134) : absente des sauvegardes d'avant, elle part de zéro.
        setDurationShards(data.real("durationShards", 0));
        setDurationShardsEarned(data.real("durationShardsEarned", 0));
        for (int i = 0; i < DURATION_UPGRADES; i++) setDurationLevel(i, data.whole("durationLevel" + i, 0));
        // Les améliorations de la graine et le chrono (version 0.139) : absents d'avant, ils partent de zéro.
        for (int i = 0; i < SEED_UPGRADES; i++) setSeedUpgradeLevel(i, data.whole("seedUpgrade" + i, 0));
        setChronoStored(data.real("chronoStored", 0));
        setChronoSpeed(data.real("chronoSpeed", 1));
        universeUpgrades.clear();
        data.ints("universeUpgrades").forEach(this::setUniverseUpgradeLevel);
        setPulseCharges(data.real("pulseCharges", 0));
        setPulses((long) data.real("pulses", 0));
        setColliderEnergy(data.real("colliderEnergy", 0));
        colliderGenerators.clear();
        colliderGeneratorsBought.clear();
        data.doubles("colliderGenerators").forEach((name, amount) -> {
            try {
                setColliderGenerators(ColliderGenerator.valueOf(name), amount);
            } catch (IllegalArgumentException unknown) {
                // un générateur d'une autre version : écarté
            }
        });
        data.ints("colliderGeneratorsBought").forEach((name, bought) -> {
            try {
                setColliderGeneratorsBought(ColliderGenerator.valueOf(name), bought);
            } catch (IllegalArgumentException unknown) {
                // un générateur d'une autre version : écarté
            }
        });
        setColliderPhase(data.real("colliderPhase", 0));
        setColliderCrossings((long) data.real("colliderCrossings", 0));
        colliderCaught = (long) data.real("colliderCaught", -1);
        colliderUpgrades.clear();
        data.ints("colliderUpgrades").forEach((name, level) -> {
            try {
                setColliderUpgradeLevel(ColliderUpgrade.valueOf(name), level);
            } catch (IllegalArgumentException unknown) {
                // une amélioration d'une autre version : écartée
            }
        });
        colliderPaused.clear();
        for (String name : data.list("colliderPaused")) {
            try {
                colliderPaused.add(ColliderUpgrade.valueOf(name));
            } catch (IllegalArgumentException unknown) {
                // un automate d'une autre version : écarté
            }
        }
        quarks.clear();
        data.ints("quarks").forEach((name, count) -> {
            try {
                setQuarks(Quark.valueOf(name), count);
            } catch (IllegalArgumentException unknown) {
                // une saveur d'une autre version : écartée
            }
        });
        // Avant la version 0.062, l'énergie produite n'était pas comptée : l'énergie en réserve en tient lieu.
        setColliderEnergyProduced(data.real("colliderEnergyProduced", colliderEnergy));
        collisions.clear();
        for (String line : data.list("collisions")) {
            String[] parts = line.split(";");
            try {
                collisions.add(new Collision(CollisionTier.valueOf(parts[0]), Math.max(0, Double.parseDouble(parts[1])),
                        Boolean.parseBoolean(parts[2]), Boolean.parseBoolean(parts[3])));
            } catch (RuntimeException unreadable) {
                // une collision illisible : perdue, sans autre dommage
            }
        }
        setCollisionRuns((long) data.real("collisionRuns", 0));
        setTopLeft(data.real("topLeft", 0));
        setOverheatLeft(data.real("overheatLeft", 0));
        probes.clear();
        for (String line : data.list("probes")) {
            String[] parts = line.split(";");
            try {
                probes.add(new Probe(ProbeDestination.valueOf(parts[0]), ProbeTrip.valueOf(parts[1]),
                        Math.max(0, Double.parseDouble(parts[2])), Math.max(1, Double.parseDouble(parts[3])),
                        parts.length > 4 ? Long.parseLong(parts[4]) : -1));
            } catch (RuntimeException unreadable) {
                // une sonde illisible : perdue, sans autre dommage
            }
        }
        probeRewards.clear();
        for (String line : data.list("probeRewards")) {
            String[] parts = line.split(";", -1);
            try {
                probeRewards.add(new ProbeReward(ProbeReward.Type.valueOf(parts[0]), parts[1], Double.parseDouble(parts[2]),
                        ProbeDestination.valueOf(parts[3]), parts.length > 4 ? Long.parseLong(parts[4]) : -1));
            } catch (RuntimeException unreadable) {
                // une récompense illisible : perdue
            }
        }
        probeUpgrades.clear();
        data.ints("probeUpgrades").forEach((name, level) -> {
            try {
                setProbeUpgradeLevel(ProbeUpgrade.valueOf(name), level);
            } catch (IllegalArgumentException unknown) {
                // une amélioration d'une autre version : écartée
            }
        });
        setProbeRuns((long) data.real("probeRuns", 0));
        setProbeLaunches((long) data.real("probeLaunches", 0));
        setProbeSamples((long) data.real("probeSamples", 0));
        setProbeBoostLeft(data.real("probeBoostLeft", 0));
        records.clear();
        data.doubles("records").forEach(this::setRecord);
        String bangChallenge = data.text("activeBangChallenge", null);
        setActiveBangChallenge(bangChallenge == null ? null : SaveData.unescape(bangChallenge));
        data.list("completedBangChallenges").forEach(this::addCompletedBangChallenge);
        data.list("boughtMilestones").forEach(this::addBoughtMilestone);
        // Avant la version 0.124, les paliers étaient gratuits : ceux qu'une ancienne partie a atteints sont acquis.
        if (!data.has("boughtMilestones")) {
            for (idle.core.bigbang.BigBangMilestone milestone : idle.core.bigbang.BigBangMilestones.DEFAULT) {
                if (data.whole("bigBangs", 0) >= milestone.bigBangs()) addBoughtMilestone(milestone.name());
            }
        }
        data.doubles("bangChallengeTimes").forEach(this::setBangChallengeTime);
        setBangChallengeStarted(data.real("bangChallengeStarted", 0));
        atomUpgradeOrder.addAll(data.list("atomUpgradeOrder"));
        atomUpgradeOrdered = data.flag("atomUpgradeOrdered", false);
        automationOrder.addAll(data.list("automationOrder"));
        automationOrdered = data.flag("automationOrdered", false);
        setSpace(data.big("space", BigNum.ZERO));
        stats.load(data, "stats.");
        // Avant la version 0.126, seules les premières du jeu entier étaient datées.
        if (!data.has("stats.universeStart")) stats.adoptLegacySteps(!universeFormed);
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        elementsVersion++;
        moleculesVersion++;
    }

    /**
     * Ce que le jeu connaît : les identifiants de ses catalogues. Une sauvegarde écrite par une autre
     * version du jeu peut en nommer d'autres ; {@link #retainKnown(Known)} les oublie.
     * Un catalogue vide n'écarte rien : c'est celui d'une partie de test, qui n'en a pas.
     */
    record Known(Set<String> upgrades, Set<String> automations, Set<String> darkUpgrades, Set<String> darkAutomations,
                 Set<String> challenges, Set<String> achievements, Set<String> molecules, Set<String> assemblies,
                 Set<String> bodies, Set<String> spaceUpgrades, int elements) {
    }

    /**
     * Oublie tout ce qui porte un identifiant que le jeu ne connaît pas : après une mise à jour,
     * une sauvegarde peut nommer une amélioration ou une molécule disparue, et les règles ne
     * sauraient qu'en faire.
     *
     * @return le nombre d'identifiants oubliés
     */
    int retainKnown(Known known) {
        int before = identifiers();
        if (!known.upgrades().isEmpty()) {
            upgradeLevels.keySet().retainAll(known.upgrades());
            atomUpgradeOrder.retainAll(known.upgrades());
        }
        if (!known.automations().isEmpty()) automationOrder.retainAll(known.automations());
        if (!known.automations().isEmpty()) {
            ownedAutomations.retainAll(known.automations());
            enabledAutomations.retainAll(known.automations());
            automationSpeedLevels.keySet().retainAll(known.automations());
            automationTimers.keySet().retainAll(known.automations());
        }
        if (elements.keySet().removeIf(number -> number < 1 || number > known.elements())) elementsVersion++;
        if (!known.darkUpgrades().isEmpty() && darkUpgradeLevels.keySet().retainAll(known.darkUpgrades())) darkVersion++;
        if (!known.darkAutomations().isEmpty()) {
            enabledDarkAutomations.retainAll(known.darkAutomations());
            darkAutomationTimers.keySet().retainAll(known.darkAutomations());
        }
        if (!known.challenges().isEmpty()) {
            if (activeChallenge != null && !known.challenges().contains(activeChallenge)) activeChallenge = null;
            if (completedChallenges.retainAll(known.challenges())) challengesVersion++;
            challengeTimes.keySet().retainAll(known.challenges());
        }
        if (!known.achievements().isEmpty() && achievements.retainAll(known.achievements())) achievementsVersion++;
        if (!known.molecules().isEmpty()) {
            for (String id : List.copyOf(molecules.keySet())) {
                if (!known.molecules().contains(id)) setMoleculeCount(id, 0);
            }
            substances.retainAll(known.molecules());
            automatedMolecules.retainAll(known.molecules());
            favoriteMolecules.retainAll(known.molecules());
        }
        if (!known.assemblies().isEmpty()) assemblies.retainAll(known.assemblies());
        if (!known.bodies().isEmpty()) bodies.retainAll(known.bodies());
        maturing.keySet().retainAll(bodies);
        solarSystems.removeIf(system -> !bodies.contains(system.star()) || !bodies.containsAll(system.planets())
                || !bodies.containsAll(system.moons()));
        if (!known.spaceUpgrades().isEmpty()) spaceUpgrades.retainAll(known.spaceUpgrades());
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        moleculesVersion++;
        return before - identifiers();
    }

    /** Nombre d'identifiants que porte la partie, tous catalogues confondus. */
    private int identifiers() {
        return upgradeLevels.size() + ownedAutomations.size() + enabledAutomations.size() + automationSpeedLevels.size()
                + automationTimers.size() + elements.size() + darkUpgradeLevels.size() + enabledDarkAutomations.size()
                + darkAutomationTimers.size() + (activeChallenge == null ? 0 : 1) + completedChallenges.size()
                + challengeTimes.size() + achievements.size() + molecules.size() + substances.size()
                + automatedMolecules.size() + favoriteMolecules.size() + atomUpgradeOrder.size() + automationOrder.size() + assemblies.size() + bodies.size() + spaceUpgrades.size();
    }

    /** Vue en lecture seule des éléments possédés (numéro atomique → exemplaires), par numéro croissant. */
    public Map<Integer, Integer> elements() {
        return Collections.unmodifiableMap(elements);
    }

    /** Vue en lecture seule des automatismes achetés, pour la sauvegarde. */
    public Set<String> ownedAutomations() {
        return Set.copyOf(ownedAutomations);
    }

    /** Vue en lecture seule des automatismes en marche, pour la sauvegarde. */
    public Set<String> enabledAutomations() {
        return Set.copyOf(enabledAutomations);
    }

    /** Vue en lecture seule des niveaux, pour la sauvegarde. */
    public Map<String, Integer> upgradeLevels() {
        return Map.copyOf(upgradeLevels);
    }
}
