package idle.ui;

import idle.core.Game;
import idle.core.Resource;
import idle.core.achievements.Achievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.state.GameState;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ce que font les outils du profil de test sur la partie, sans affichage : s'ajouter des ressources, sauter une
 * étape, remettre un palier à zéro. Tout passe à côté des règles du jeu : c'est un outil de développement, pas du
 * gameplay. Les outils ({@link TestTools}) et la bande des dimensions ({@link DimensionTestBar}) s'en servent ; les
 * essais aussi, sans JavaFX.
 */
final class TestActions {

    /** Le nom de chaque dimension et de sa ressource, de la ligne au mélange. */
    static final String[] DIMENSIONS = {"Ligne (segments)", "Plan (facettes)", "Volume (sphères)", "Durée (instants)",
            "Boucle (nœuds)", "Brane (ondes)", "Clicker (étincelles)", "Grille (repères)", "Miroir (reflets)", "Mélange (fragments)"};

    private final Game game;

    TestActions(Game game) { this.game = game; }

    private GameState state() { return game.state(); }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    /** Fait passer {@code seconds} secondes de jeu d'un coup, par tranches d'une minute, comme en temps réel. */
    void skip(double seconds) {
        for (double done = 0; done < seconds; done += 60) game.tick(Math.min(60, seconds - done));
    }

    // ------------------------------------------------------------------
    // Les particules, les atomes, l'automatisation
    // ------------------------------------------------------------------

    void addParticles(BigNum amount) { state().setParticles(state().particles().add(amount)); }

    void multiplyParticles(BigNum factor) { state().setParticles(state().particles().max(BigNum.ONE).multiply(factor)); }

    /** Débloque gratuitement jusqu'à {@code count} générateurs, tant qu'il en reste. */
    void addGenerators(int count) {
        for (Upgrade upgrade : game.upgrades()) {
            if (!(upgrade.effect() instanceof Effect.AddGenerator)) continue;
            while (count-- > 0 && !game.isMaxed(upgrade.id())) state().setLevel(upgrade.id(), game.levelOf(upgrade.id()) + 1);
            return;
        }
    }

    /** Exactement {@code count} générateurs (au moins un, au plus le maximum) : le profil test UI (0.248). */
    void setGenerators(int count) {
        if (!game.isStarted()) game.start();
        for (Upgrade upgrade : game.upgrades()) {
            if (!(upgrade.effect() instanceof Effect.AddGenerator)) continue;
            state().setLevel(upgrade.id(), Math.max(0, count - 1 - game.startingGenerators()));
            return;
        }
    }

    void regimeReady() {
        state().setRegimeLeft(0);
        state().setRegimeRest(0);
    }

    void addAtoms(BigNum count) {
        BigNum atoms = state().atoms().add(count);
        state().setAtoms(game.isAtomCapLifted() ? atoms : atoms.min(game.atomCap()));
        state().setTotalAtoms(state().totalAtoms().add(count));
    }

    /** Assez d'atomes créés pour que l'automatisation et le tableau périodique soient débloqués. */
    void unlockTable() {
        if (state().totalAtoms().isZero()) addAtoms(BigNum.ONE);
        state().setTotalAtoms(state().totalAtoms().max(Game.UNLOCK_TOTAL_ATOMS));
    }

    /** Porte à leur maximum les améliorations payées en atomes qui en ont un, Persistance comprise. */
    void maxAtomUpgrades() {
        unlockTable();
        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) if (upgrade.hasLimit()) state().setLevel(upgrade.id(), upgrade.maxLevel());
    }

    /** Donne tous les automatismes ordinaires, en marche, à leur cadence maximale. */
    void maxAutomations() {
        unlockTable();
        for (Automation automation : game.automations()) {
            state().addAutomation(automation.id());
            state().setAutomationEnabled(automation.id(), true);
            state().setAutomationSpeedLevel(automation.id(), automation.maxSpeedLevel());
        }
    }

    /** Retire tous les automatismes ordinaires. */
    void noAutomations() {
        for (Automation automation : game.automations()) {
            state().setAutomationEnabled(automation.id(), false);
            state().setAutomationSpeedLevel(automation.id(), 0);
        }
    }

    /** Donne au moins {@code copies} exemplaires d'un élément, sans dépasser son maximum. */
    void give(int number, int copies) {
        unlockTable();
        Element element = PeriodicTable.element(number);
        int wanted = Math.min(copies, game.maxCopiesOf(element));
        if (game.elementCount(number) < wanted) state().setElementCount(number, wanted);
    }

    /** Remplit le tableau : un exemplaire de chaque élément, ou tous. Avec Persistance et tous les automatismes. */
    void fillTable(boolean everyCopy) {
        maxAutomations();
        for (Upgrade upgrade : game.upgrades()) {
            if (upgrade.effect() instanceof Effect.KeepUpgradesOnFusion) state().setLevel(upgrade.id(), upgrade.maxLevel());
        }
        for (Element element : PeriodicTable.ELEMENTS) give(element.number(), everyCopy ? Integer.MAX_VALUE : 1);
    }

    void emptyTable() {
        for (Element element : PeriodicTable.ELEMENTS) state().setElementCount(element.number(), 0);
        state().setSynthesisCount(0);
    }

    void freeElement() { state().setFreeElementsUsed(Math.max(0, state().freeElementsUsed() - 1)); }

    // ------------------------------------------------------------------
    // La matière noire
    // ------------------------------------------------------------------

    /** Découvre les éléments qui manquent, puis fait exploser le tableau par les règles du jeu. */
    void explodeNow() {
        for (Element element : PeriodicTable.ELEMENTS) give(element.number(), 1);
        game.explode();
    }

    private void oneExplosion() { if (state().explosions() == 0) state().setExplosions(1); }

    /** Ajoute de la matière noire ; sans explosion encore, en compte une, pour que l'onglet s'ouvre. */
    void addDarkMatter(int count) {
        oneExplosion();
        state().setDarkMatter(state().darkMatter().add(BigNum.of(count)));
    }

    /** La taille de la matière noire à dix puissance {@code decades} : le profil test UI (0.248). */
    void darkMatterSize(int decades) {
        oneExplosion();
        state().setDarkMatterSize(BigNum.of(1, decades));
    }

    /** Multiplie la taille de la matière noire par dix puissance {@code decades}. */
    void growDarkMatter(int decades) {
        oneExplosion();
        state().setDarkMatterSize(state().darkMatterSize().multiply(BigNum.of(1, decades)));
    }

    /** Porte à leur maximum les améliorations de matière noire ; d'une bifurcation, la première voie seulement. */
    void fillTree() {
        oneExplosion();
        Set<String> forks = new HashSet<>();
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.isFork() && !forks.add(upgrade.fork())) continue;
            if (upgrade.hasLimit()) state().setDarkLevel(upgrade.id(), upgrade.maxLevel());
        }
    }

    void emptyTree() { for (DarkUpgrade upgrade : game.darkUpgrades()) state().setDarkLevel(upgrade.id(), 0); }

    /** Marque comme réussi le premier défi qui ne l'est pas encore, sans le jouer. */
    void completeNextChallenge() {
        if (game.inChallenge()) return;
        for (Challenge challenge : game.challenges()) {
            if (game.isChallengeCompleted(challenge.id())) continue;
            oneExplosion();
            state().addCompletedChallenge(challenge.id());
            return;
        }
    }

    void completeAllChallenges() {
        oneExplosion();
        for (Challenge challenge : Challenges.DEFAULT) state().addCompletedChallenge(challenge.id());
    }

    void leakCompleted() { state().addCompletedChallenge(Challenges.LEAK.id()); }

    void bangCalmFull() { state().setBangCalm(Game.BIG_BANG_CALM_SECONDS); }

    void heavierTable() { state().setTableWeightLevel(state().tableWeightLevel() + 1); }

    void tableAtStart() { state().setTableWeightLevel(0); }

    // ------------------------------------------------------------------
    // Le collisionneur, les hadrons, le vide
    // ------------------------------------------------------------------

    /** Le collisionneur s'ouvre à la première explosion : l'outil la compte, s'il n'y en a pas eu. */
    void unlockCollider() { if (state().explosions() == 0 && game.bigBangs() == 0) state().setExplosions(1); }

    void colliderEnergy() {
        unlockCollider();
        state().setColliderEnergy(Math.max(state().colliderEnergy(), 1000) * 1000);
    }

    void colliderGenerators() {
        unlockCollider();
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            if (state().colliderGeneratorsBought(generator) == 0) {
                state().setColliderGeneratorsBought(generator, 1);
                state().setColliderGenerators(generator, Math.max(1, state().colliderGenerators(generator)));
            }
        }
    }

    void collisionsAndFlavors() {
        unlockCollider();
        for (ColliderUpgrade upgrade : List.of(ColliderUpgrade.COLLISIONS, ColliderUpgrade.FLAVOR_STRANGE, ColliderUpgrade.FLAVOR_CHARM,
                ColliderUpgrade.FLAVOR_BOTTOM, ColliderUpgrade.FLAVOR_TOP)) {
            state().setColliderUpgradeLevel(upgrade, Math.max(1, game.colliderUpgradeLevel(upgrade)));
        }
    }

    void colliderUpgrades(boolean max) {
        unlockCollider();
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) state().setColliderUpgradeLevel(upgrade, max ? upgrade.maxLevel() : 0);
    }

    /** La vitesse des faisceaux, d'un niveau de plus (puis retour à zéro après le dernier). */
    void nextBeamSpeed() {
        unlockCollider();
        int level = game.colliderUpgradeLevel(ColliderUpgrade.SPEED);
        state().setColliderUpgradeLevel(ColliderUpgrade.SPEED, level >= ColliderUpgrade.SPEED.maxLevel() ? 0 : level + 1);
    }

    void addQuarks(int count) {
        unlockCollider();
        for (Quark quark : Quark.values()) state().setQuarks(quark, game.quarks(quark) + count);
    }

    void topQuark() {
        unlockCollider();
        state().setTopLeft(Game.TOP_SECONDS);
    }

    void blackHoleEnergy() {
        unlockCollider();
        state().setColliderEnergyProduced(Math.max(state().colliderEnergyProduced(), Game.SUPERMASSIVE_ENERGY));
    }

    void openVoid() {
        unlockCollider();
        game.testOpenVoid();
    }

    void openHadrons() {
        unlockCollider();
        state().setHadronsOpen(true);
    }

    void addHadrons(long count) {
        openHadrons();
        for (Hadron hadron : Hadron.values()) state().setHadrons(hadron, state().hadrons(hadron) + count);
    }

    void addVoidEnergy(BigNum amount) { game.testAddVoidEnergy(amount); }

    void voidMass() {
        game.testOpenVoidHole();
        state().setVoidSacrificed(BigNum.of(1, 40_000_000));
    }

    /** Les améliorations du vide au plus haut (le gain, sans plafond : dix niveaux), ou à zéro. */
    void voidUpgrades(boolean max) {
        for (Game.VoidUpgrade upgrade : Game.VoidUpgrade.values())
            state().setVoidLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.VoidUpgrade.GAIN ? 10 : upgrade.max());
    }

    // ------------------------------------------------------------------
    // Les succès
    // ------------------------------------------------------------------

    void grantAchievements() { for (Achievement achievement : game.achievements()) state().addAchievement(achievement.id()); }

    // ------------------------------------------------------------------
    // Le Big Bang
    // ------------------------------------------------------------------

    /**
     * Réunit les conditions du Big Bang sans rien retirer : ce qu'il manque de particules, d'atomes et de matière
     * noire, les succès et les défis. Un défi en cours est arrêté, sans être compté comme réussi.
     */
    void readyForBigBang() {
        oneExplosion();
        state().setActiveChallenge(null);
        state().setParticles(state().particles().max(game.bigBangParticles()));
        state().setAtoms(state().atoms().max(game.bigBangAtoms()));
        state().setTotalAtoms(state().totalAtoms().max(state().atoms()));
        state().setDarkMatter(state().darkMatter().max(game.bigBangDarkMatter()));
        grantAchievements();
        for (Challenge challenge : game.challenges()) state().addCompletedChallenge(challenge.id());
    }

    void bigBangNow() {
        readyForBigBang();
        game.bigBang();
    }

    /** Au moins ce nombre de Big Bangs, et tous les paliers ouverts payés d'office. */
    void bangsAtLeast(int bigBangs) {
        if (state().bigBangs() < bigBangs) state().setBigBangs(bigBangs);
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            if (game.isBigBangMilestoneOpen(milestone)) state().addBoughtMilestone(milestone.name());
        }
    }

    void oneOfEachMolecule() {
        for (Molecule molecule : game.molecules()) state().setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + 1);
    }

    void noMolecules() { for (Molecule molecule : game.molecules()) state().setMoleculeCount(molecule.id(), 0); }

    void addSpace(BigNum amount) { state().setSpace(state().space().add(amount)); }

    void multiplySpace() { state().setSpace(state().space().max(BigNum.of(100)).multiply(10)); }

    void spaceUpgrades() { for (SpaceUpgrade upgrade : game.spaceUpgrades()) state().addSpaceUpgrade(upgrade.id()); }

    void threeOfEachSmallMolecule() {
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() == Molecule.Kind.SIMPLE) state().setMoleculeCount(molecule.id(), Game.SUBSTANCE_MOLECULES);
        }
    }

    void gatherAll() {
        for (Molecule molecule : game.molecules()) {
            if (molecule.hasState() && game.moleculeCount(molecule.id()) > 0) state().addSubstance(molecule.id());
        }
    }

    /** Forme le premier assemblage qui ne l'est pas encore. @return {@code false} s'ils sont tous formés */
    boolean formNextAssembly() {
        for (Assembly assembly : game.assemblies()) if (!game.hasAssembly(assembly.id())) return form(assembly);
        return false;
    }

    void formAllAssemblies() { while (formNextAssembly()) { } }

    /** Forme cet assemblage, en donnant au joueur ce qu'il demande. */
    private boolean form(Assembly assembly) {
        bangsAtLeast(1);
        spaceUpgrades();
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            state().addSubstance(ingredient.getKey());
            int missing = ingredient.getValue() - game.spareMolecules(ingredient.getKey());
            if (missing > 0) state().setMoleculeCount(ingredient.getKey(), game.moleculeCount(ingredient.getKey()) + missing);
        }
        state().setSpace(state().space().max(game.usedSpace().multiply(3)));
        return game.formAssembly(assembly.id());
    }

    /**
     * Forme le premier astre du catalogue qui ne l'est pas encore, en donnant au joueur ce qu'il demande : ses
     * assemblages, ses molécules, sa matière. Le catalogue va du plus petit au plus grand.
     *
     * @return {@code false} s'ils sont tous formés
     */
    boolean formNextBody() {
        for (Body body : game.bodies()) {
            if (game.hasBody(body.id())) continue;
            bangsAtLeast(1);
            // Une matière exotique attend le huitième Big Bang : l'outil y va d'office.
            if (body.tier() == Body.Tier.EXOTIC && !game.isExoticUnlocked()) bangsAtLeast(8);
            spaceUpgrades();
            for (String assembly : body.assemblies()) if (!game.hasAssembly(assembly)) form(game.assembly(assembly));
            for (Map.Entry<String, Integer> molecule : game.bodyMolecules(body).entrySet()) {
                int missing = molecule.getValue() - game.moleculeCount(molecule.getKey());
                if (missing > 0) state().setMoleculeCount(molecule.getKey(), game.moleculeCount(molecule.getKey()) + missing);
                state().addSubstance(molecule.getKey());
            }
            for (Map.Entry<Molecule.State, Integer> matter : game.bodyMatter(body).entrySet()) {
                int missing = (int) Math.max(0, matter.getValue() - game.gatheredInState(matter.getKey()));
                if (missing <= 0) continue;
                for (Molecule molecule : game.molecules()) {
                    if (molecule.state() != matter.getKey()) continue;
                    state().addSubstance(molecule.id());
                    state().setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + missing);
                    break;
                }
            }
            state().setSpace(state().space().max(game.usedSpace().multiply(3)));
            if (game.needsColliderEnergy(body)) state().setColliderEnergyProduced(Math.max(state().colliderEnergyProduced(), game.colliderEnergyNeeded(body)));
            boolean formed = game.formBody(body.id());
            if (formed) state().setMaturing(body.id(), 0);
            return formed;
        }
        return false;
    }

    void formAllBodies() { while (formNextBody()) { } }

    /** Range les étoiles et les planètes libres en systèmes solaires, jusqu'à ce que la galaxie en ait assez. */
    private void threeSystems() {
        bangsAtLeast(6);
        while (game.solarSystemsComplete() < Game.SYSTEMS_FOR_GALAXY) {
            List<String> stars = game.freeForSystems(Body.Tier.STAR);
            List<String> planets = game.freeForSystems(Body.Tier.PLANET);
            if (stars.isEmpty() || planets.size() < Game.SYSTEM_MIN_PLANETS) return;
            game.foundSystem(stars.get(0));
            int index = state().solarSystems().size() - 1;
            for (int i = 0; i < Game.SYSTEM_MIN_PLANETS; i++) game.addPlanet(index, planets.get(i));
        }
    }

    void formGalaxy() {
        formAllBodies();
        threeSystems();
        game.formGalaxy();
    }

    /** L'échelle suivante du cosmos ; l'univers se forme pour de vrai, remise à zéro comprise. */
    void nextCosmos() {
        formAllBodies();
        threeSystems();
        Cosmos next = game.nextCosmos();
        if (next == Cosmos.UNIVERSE) game.testFormUniverse();
        else if (next != null) state().setCosmosLevel(next.ordinal() + 1);
    }

    void nextBangChallenge() {
        for (BangChallenge challenge : BangChallenge.values()) {
            if (!game.isBangChallengeCompleted(challenge)) {
                state().addCompletedBangChallenge(challenge.name());
                return;
            }
        }
    }

    void allBangChallenges() { for (BangChallenge challenge : BangChallenge.values()) state().addCompletedBangChallenge(challenge.name()); }

    // ------------------------------------------------------------------
    // L'univers, la graine, les dimensions
    // ------------------------------------------------------------------

    /** La graine passe au palier suivant : les arrosages qu'il faut. */
    void nextSeedStage() {
        int stage = game.seedStage();
        if (stage >= Game.SEED_STAGES) return;
        if (!game.isUniverseFormed()) game.testFormUniverse();
        state().setSeedSprouted(true);
        state().setSeedWaterings(Math.max(state().seedWaterings(), Game.seedStageWaterings(stage + 1)));
    }

    /** La graine à l'étape {@code stage} (1 : pas encore germée) : le profil test UI (0.248). */
    void seedStage(int stage) {
        if (!game.isUniverseFormed()) game.testFormUniverse();
        int wanted = Math.max(1, Math.min(Game.SEED_STAGES, stage));
        state().setSeedSprouted(wanted > 1);
        state().setSeedWaterings(wanted > 1 ? Game.seedStageWaterings(wanted) : 0);
    }

    /** L'arrosoir tenu en main ({@code true}) ou reposé sur l'étagère. */
    void holdWateringCan(boolean held) {
        state().setWateringCan(true);
        game.setWatering(held);
    }

    /** Plus aucun succès obtenu : le profil test UI (0.248). */
    void clearAchievements() { state().testClearAchievements(); }

    void water() {
        state().setSeedSprouted(true);
        state().setSeedWaterings(state().seedWaterings() + 1);
    }

    void takeWateringCan() {
        game.setWatering(false);
        state().setWateringCan(false);
    }

    void addVoidHistory(double amount) {
        String last = String.valueOf(Math.max(1, game.universes()));
        state().noteVoidHistory(last, state().voidHistory().getOrDefault(last, 0.0) + amount);
    }

    /** Les améliorations de la graine au plus haut (avec l'arrosoir), ou à zéro avec le chrono vide et réglé à ×1. */
    void seedUpgrades(boolean max) {
        for (Game.SeedUpgrade upgrade : Game.SeedUpgrade.values()) state().setSeedUpgradeLevel(upgrade.ordinal(), max ? upgrade.max() : 0);
        if (max) state().setWateringCan(true);
        else {
            state().setChronoStored(0);
            game.setChronoSpeed(1);
        }
    }

    void fullChrono() {
        if (!game.isChronoUnlocked()) state().setSeedUpgradeLevel(Game.SeedUpgrade.CHRONO.ordinal(), 1);
        state().setChronoStored(game.chronoCapacity());
    }

    /** Ouvre la dimension {@code number} : l'univers formé, puis autant d'univers qu'il en faut. */
    void openDimension(int number) {
        if (!game.isUniverseFormed()) game.testFormUniverse();
        for (int guard = 0; guard < Game.MAX_DIMENSIONS && game.dimensionsUnlocked() < number; guard++) {
            if (!game.testAddUniverse()) break;
        }
    }

    static double dimensionThreshold(int number) {
        return switch (number) {
            case 1 -> Game.LINE_THRESHOLD;
            case 2 -> Game.PLANE_THRESHOLD;
            case 3 -> Game.VOLUME_THRESHOLD;
            case 4 -> Game.DURATION_THRESHOLD;
            case 5 -> Game.LOOP_THRESHOLD;
            case 6 -> Game.BRANE_THRESHOLD;
            case 7 -> Game.CLICKER_THRESHOLD;
            case 8 -> Game.GRID_THRESHOLD;
            case 9 -> Game.MIRROR_THRESHOLD;
            default -> Game.MIX_THRESHOLD;
        };
    }

    /** La ressource de la dimension, à dépenser. */
    double dimensionShards(int number) {
        return switch (number) {
            case 1 -> state().dimensionShards();
            case 2 -> state().planeShards();
            case 3 -> state().volumeShards();
            case 4 -> state().durationShards();
            case 5 -> state().loopShards();
            case 6 -> state().braneShards();
            case 7 -> state().clickerShards();
            case 8 -> state().gridShards();
            case 9 -> state().mirrorShards();
            default -> state().mixShards();
        };
    }

    /** Ce que la dimension a gagné en tout, vers son seuil. */
    double dimensionEarned(int number) {
        return switch (number) {
            case 1 -> state().dimensionShardsEarned();
            case 2 -> state().planeShardsEarned();
            case 3 -> state().volumeShardsEarned();
            case 4 -> state().durationShardsEarned();
            case 5 -> state().loopShardsEarned();
            case 6 -> state().braneShardsEarned();
            case 7 -> state().clickerShardsEarned();
            case 8 -> state().gridShardsEarned();
            case 9 -> state().mirrorShardsEarned();
            default -> state().mixShardsEarned();
        };
    }

    /** Fixe la ressource de la dimension et ce qu'elle a gagné en tout. */
    private void setDimension(int number, double shards, double earned) {
        GameState s = state();
        switch (number) {
            case 1 -> { s.setDimensionShards(shards); s.setDimensionShardsEarned(earned); }
            case 2 -> { s.setPlaneShards(shards); s.setPlaneShardsEarned(earned); }
            case 3 -> { s.setVolumeShards(shards); s.setVolumeShardsEarned(earned); }
            case 4 -> { s.setDurationShards(shards); s.setDurationShardsEarned(earned); }
            case 5 -> { s.setLoopShards(shards); s.setLoopShardsEarned(earned); }
            case 6 -> { s.setBraneShards(shards); s.setBraneShardsEarned(earned); }
            case 7 -> { s.setClickerShards(shards); s.setClickerShardsEarned(earned); }
            case 8 -> { s.setGridShards(shards); s.setGridShardsEarned(earned); }
            case 9 -> { s.setMirrorShards(shards); s.setMirrorShardsEarned(earned); }
            default -> { s.setMixShards(shards); s.setMixShardsEarned(earned); }
        }
    }

    /** Des éclats de la dimension, gagnés : la ressource à dépenser, et autant vers le seuil. */
    void addDimensionShards(int number, double amount) {
        setDimension(number, dimensionShards(number) + amount, dimensionEarned(number) + amount);
    }

    /** Le seuil atteint : les éclats gagnés qu'il faut, sans rien ajouter à la ressource. */
    void reachDimensionThreshold(int number) {
        setDimension(number, dimensionShards(number), Math.max(dimensionEarned(number), dimensionThreshold(number)));
    }

    /**
     * Les améliorations de la dimension au plus haut, ou à zéro. La valeur du volume et de la durée, et le gain des
     * dimensions suivantes, n'ont pas de plafond : dix niveaux.
     */
    void dimensionLevels(int number, boolean max) {
        GameState s = state();
        switch (number) {
            case 1 -> {
                s.setLineYieldLevel(max ? game.lineMaxLevel(Game.LineUpgrade.YIELD) : 0);
                s.setLineTicksLevel(max ? game.lineMaxLevel(Game.LineUpgrade.TICKS) : 0);
                s.setLineSpeedLevel(max ? game.lineMaxLevel(Game.LineUpgrade.SPEED) : 0);
                s.setLineBallsLevel(max ? game.lineMaxLevel(Game.LineUpgrade.BALLS) : 0);
                s.setLineCountLevel(max ? game.lineMaxLevel(Game.LineUpgrade.LINES) : 0);
            }
            case 2 -> { for (Game.PlaneUpgrade u : Game.PlaneUpgrade.values()) s.setPlaneLevel(u.ordinal(), max ? u.max() : 0); }
            case 3 -> { for (Game.VolumeUpgrade u : Game.VolumeUpgrade.values())
                    s.setVolumeLevel(u.ordinal(), !max ? 0 : u == Game.VolumeUpgrade.VALUE ? 10 : u.max()); }
            case 4 -> { for (Game.DurationUpgrade u : Game.DurationUpgrade.values())
                    s.setDurationLevel(u.ordinal(), !max ? 0 : u == Game.DurationUpgrade.VALUE ? 10 : u.max()); }
            case 5 -> { for (Game.LoopUpgrade u : Game.LoopUpgrade.values())
                    s.setLoopLevel(u.ordinal(), !max ? 0 : u == Game.LoopUpgrade.GAIN ? 10 : u.max()); }
            case 6 -> { for (Game.BraneUpgrade u : Game.BraneUpgrade.values())
                    s.setBraneLevel(u.ordinal(), !max ? 0 : u == Game.BraneUpgrade.GAIN ? 10 : u.max()); }
            case 7 -> { for (Game.ClickerUpgrade u : Game.ClickerUpgrade.values())
                    s.setClickerLevel(u.ordinal(), !max ? 0 : u == Game.ClickerUpgrade.GAIN ? 10 : u.max()); }
            case 8 -> { for (Game.GridUpgrade u : Game.GridUpgrade.values())
                    s.setGridLevel(u.ordinal(), !max ? 0 : u == Game.GridUpgrade.GAIN ? 10 : u.max()); }
            case 9 -> { for (Game.MirrorUpgrade u : Game.MirrorUpgrade.values())
                    s.setMirrorLevel(u.ordinal(), !max ? 0 : u == Game.MirrorUpgrade.GAIN ? 10 : u.max()); }
            default -> { for (Game.MixUpgrade u : Game.MixUpgrade.values())
                    s.setMixLevel(u.ordinal(), !max ? 0 : u == Game.MixUpgrade.GAIN ? 10 : u.max()); }
        }
    }

    /** La dimension à son début : rien gagné, rien en réserve, améliorations à zéro, son terrain vidé. Elle reste ouverte. */
    void resetDimension(int number) {
        setDimension(number, 0, 0);
        dimensionLevels(number, false);
        switch (number) {
            case 1 -> state().setLinePhase(0);
            case 2 -> game.setPlaneInput(0, 0);
            case 3 -> game.testClearVolume();
            case 4 -> { if (game.isDurationHeld()) game.releaseDuration(); }
            case 6 -> game.testClearBrane();
            case 7 -> game.testClearClicker();
            case 10 -> mixStageOne();
            default -> { }
        }
    }

    /** Les fragments qui manquent à l'étape du mélange ; faux si le mélange n'est pas ouvert ou à sa dernière étape. */
    boolean mixStageShards() {
        if (!game.isMixOpen() || game.mixStage() >= Game.MIX_STAGES) return false;
        addDimensionShards(10, Math.max(0, game.mixStageGoal() - state().mixStageEarned()));
        state().setMixStageEarned(game.mixStageGoal());
        return true;
    }

    /** Le mélange passe à l'étape suivante ; faux s'il n'est pas ouvert ou à sa dernière étape. */
    boolean nextMixStage() {
        if (!game.isMixOpen() || game.mixStage() >= Game.MIX_STAGES) return false;
        state().setMixStageEarned(game.mixStageGoal());
        game.advanceMix();
        return true;
    }

    void mixStageOne() {
        state().setMixStage(1);
        state().setMixStageEarned(0);
        for (Game.MixUpgrade upgrade : Game.MixUpgrade.values()) state().setMixLevel(upgrade.ordinal(), 0);
        game.testClearMix();
    }

    /** Ouvre le défi final : la dernière amélioration de la graine, puis la bulle des racines, sans rien payer. */
    void openFinalChallenge() {
        state().setSeedUpgradeLevel(Game.SeedUpgrade.ROOT_BUBBLE.ordinal(), 1);
        state().setTreeBubbleBought(Game.TreeBubble.ROOTS.ordinal(), true);
    }

    void reachAllFinalTiers() {
        openFinalChallenge();
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) addDimensionShards(d, Math.max(0, game.finalGoalFor(d) - game.dimensionEarned(d)));
    }

    void closeFinalChallenge() {
        state().setSeedUpgradeLevel(Game.SeedUpgrade.ROOT_BUBBLE.ordinal(), 0);
        state().setTreeBubbleBought(Game.TreeBubble.ROOTS.ordinal(), false);
    }

    /** Impose le temps qu'il fait sur la graine ; faux sans la météo (elle vient avec le clicker). */
    boolean weather(Game.Weather weather) {
        if (!game.isWeatherUnlocked()) return false;
        game.testSetWeather(weather);
        return true;
    }

    // ------------------------------------------------------------------
    // Les paliers et leurs remises à zéro
    // ------------------------------------------------------------------

    /** Acte 1 : la matière (particules, atomes, tableau) repart de zéro. */
    void resetAct1() { state().clearMatter(); }

    /** Acte 2 : la matière et la matière noire repartent de zéro. */
    void resetAct2() {
        state().clearMatter();
        state().clearDarkMatter();
    }

    /** Acte 3 : le Big Bang et ce qui en vient repartent de zéro. */
    void resetAct3() { state().clearBigBangAct(); }

    void resetCollider() { state().clearCollider(); }

    /** La graine à son début : pas germée, sans arrosage, sans arrosoir, améliorations à zéro, chrono vide. */
    void resetSeed() {
        state().setSeedSprouted(false);
        state().setSeedWaterings(0);
        takeWateringCan();
        seedUpgrades(false);
    }

    /** Le vide à son début : fermé, sans énergie, sans masse, améliorations à zéro, son historique oublié. */
    void resetVoid() {
        state().setVoidEnergy(BigNum.ZERO);
        state().setVoidOpen(false);
        state().setVoidHoleOpen(false);
        state().setVoidSacrificed(BigNum.ZERO);
        voidUpgrades(false);
        state().clearVoidHistory();
    }

    void resetTreeBubbles() {
        state().clearTreeBubbles();
        state().setVoidSpent(0);
    }

    /** Toutes les dimensions à leur début (elles restent ouvertes). */
    void resetAllDimensions() { for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) resetDimension(d); }

    /** Toute la partie secondaire à son début : un seul univers, rien en réserve, ni graine, ni vide, ni dimension. */
    void resetUniverses() { game.testResetUniverses(); }
}
