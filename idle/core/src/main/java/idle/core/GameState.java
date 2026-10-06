package idle.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Tout ce qui change pendant une partie, et rien d'autre.
 * C'est cet objet qui sera écrit dans la sauvegarde.
 *
 * <p>Aucune règle du jeu ici : les règles sont dans {@link Game}.
 */
public final class GameState {

    private boolean started = false;
    private BigNum particles = BigNum.ZERO;
    private BigNum atoms = BigNum.ZERO;
    private BigNum totalAtoms = BigNum.ZERO;
    private double timeSinceFusion = 0;
    private final List<Double> formations = new ArrayList<>();
    private double timePlayed = 0;
    private final Map<String, Integer> upgradeLevels = new HashMap<>();
    private final Set<String> ownedAutomations = new HashSet<>();
    private final Set<String> enabledAutomations = new HashSet<>();
    private final Map<String, Integer> automationSpeedLevels = new HashMap<>();
    private final Map<String, Double> automationTimers = new HashMap<>();
    private final Map<Integer, Integer> elements = new TreeMap<>();
    private int elementsVersion = 0;
    private int synthesisCount = 0;
    private ElementCategory synthesisTarget = null;
    private int synthesisTries = 0;
    private BigNum darkMatter = BigNum.ZERO;
    private BigNum darkMatterSpent = BigNum.ZERO;
    private BigNum darkMatterSize = Game.DARK_MATTER_START_SIZE;
    private int explosions = 0;
    private int tableWeightLevel = 0;
    private final Map<String, Integer> darkUpgradeLevels = new HashMap<>();
    private int fusionThreshold = 0;
    private BigNum synthesisReserve = BigNum.ZERO;
    private boolean holdLocked = false;
    private String activeChallenge = null;
    private final Set<String> completedChallenges = new HashSet<>();
    private final Map<String, Double> challengeTimes = new HashMap<>();
    private double decayDebt = 0;
    private final Set<String> achievements = new java.util.LinkedHashSet<>();
    private final Set<String> enabledDarkAutomations = new HashSet<>();
    private final Map<String, Double> darkAutomationTimers = new HashMap<>();
    private int bigBangs = 0;
    private final Map<String, Integer> molecules = new HashMap<>();
    private final List<String> moleculeLog = new ArrayList<>();
    private int moleculesVersion = 0;
    private final Set<String> substances = new java.util.LinkedHashSet<>();
    private final Set<String> assemblies = new java.util.LinkedHashSet<>();
    private final Set<String> bodies = new java.util.LinkedHashSet<>();
    private final Set<String> spaceUpgrades = new java.util.LinkedHashSet<>();
    private BigNum space = BigNum.ZERO;
    private GameStats stats = new GameStats();

    /** Les statistiques de la partie : des compteurs pour l'affichage, que les règles ne lisent pas. */
    public GameStats stats() {
        return stats;
    }

    /** Faux tant que le joueur n'a pas créé son premier générateur. */
    public boolean started() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    /** Particules possédées : la ressource de base, créée par les générateurs. */
    public BigNum particles() {
        return particles;
    }

    public void setParticles(BigNum particles) {
        if (particles.sign() < 0) throw new IllegalArgumentException("Le nombre de particules ne peut pas être négatif");
        this.particles = particles;
    }

    /** Atomes disponibles : la deuxième ressource, gagnée par fusion et dépensée en améliorations. */
    public BigNum atoms() {
        return atoms;
    }

    public void setAtoms(BigNum atoms) {
        if (atoms.sign() < 0) throw new IllegalArgumentException("Le nombre d'atomes ne peut pas être négatif");
        this.atoms = atoms;
    }

    /** Atomes créés depuis le début du jeu, dépensés ou non. Ne diminue jamais. */
    public BigNum totalAtoms() {
        return totalAtoms;
    }

    public void setTotalAtoms(BigNum totalAtoms) {
        if (totalAtoms.sign() < 0) throw new IllegalArgumentException("Le nombre d'atomes ne peut pas être négatif");
        this.totalAtoms = totalAtoms;
    }

    /** Temps écoulé depuis la dernière fusion (ou depuis le démarrage), en secondes. */
    public double timeSinceFusion() {
        return timeSinceFusion;
    }

    public void setTimeSinceFusion(double timeSinceFusion) {
        if (!(timeSinceFusion >= 0)) throw new IllegalArgumentException("Durée invalide : " + timeSinceFusion);
        this.timeSinceFusion = timeSinceFusion;
    }

    /**
     * Avancement de la particule en cours sur un générateur, de 0 (rien) à 1 (exclu).
     * Arrivé à 1, la création est terminée : les particules sont ajoutées et on repart de 0.
     *
     * @param generator numéro du générateur, 0 étant le premier
     */
    public double formation(int generator) {
        return generator < formations.size() ? formations.get(generator) : 0;
    }

    public void setFormation(int generator, double formation) {
        if (!(formation >= 0 && formation < 1)) {
            throw new IllegalArgumentException("La formation doit être dans [0, 1[ : " + formation);
        }
        while (formations.size() <= generator) {
            formations.add(0.0);
        }
        formations.set(generator, formation);
    }

    /** Remet tous les générateurs à zéro (après une fusion). */
    public void clearFormations() {
        formations.clear();
    }

    /** Vue en lecture seule des avancements, pour la sauvegarde. */
    public List<Double> formations() {
        return List.copyOf(formations);
    }

    /** Temps de jeu cumulé, en secondes. */
    public double timePlayed() {
        return timePlayed;
    }

    public void setTimePlayed(double timePlayed) {
        this.timePlayed = timePlayed;
    }

    /** Niveau possédé pour une amélioration (0 si jamais achetée). */
    public int levelOf(String upgradeId) {
        return upgradeLevels.getOrDefault(upgradeId, 0);
    }

    public void setLevel(String upgradeId, int level) {
        if (level < 0) throw new IllegalArgumentException("Niveau négatif");
        upgradeLevels.put(upgradeId, level);
    }

    /** Vrai si le joueur a acheté cet automatisme. */
    public boolean ownsAutomation(String automationId) {
        return ownedAutomations.contains(automationId);
    }

    public void addAutomation(String automationId) {
        ownedAutomations.add(automationId);
    }

    /** Vrai si cet automatisme est en marche. Un automatisme acheté peut être coupé. */
    public boolean isAutomationEnabled(String automationId) {
        return enabledAutomations.contains(automationId);
    }

    public void setAutomationEnabled(String automationId, boolean enabled) {
        if (enabled) {
            enabledAutomations.add(automationId);
        } else {
            enabledAutomations.remove(automationId);
        }
    }

    /** Niveau de cadence d'un automatisme : plus il est haut, plus le délai entre deux actions est court. */
    public int automationSpeedLevel(String automationId) {
        return automationSpeedLevels.getOrDefault(automationId, 0);
    }

    public void setAutomationSpeedLevel(String automationId, int level) {
        if (level < 0) throw new IllegalArgumentException("Niveau négatif");
        automationSpeedLevels.put(automationId, level);
    }

    /** Temps écoulé depuis la dernière action d'un automatisme, en secondes. */
    public double automationTimer(String automationId) {
        return automationTimers.getOrDefault(automationId, 0.0);
    }

    public void setAutomationTimer(String automationId, double seconds) {
        if (!(seconds >= 0)) throw new IllegalArgumentException("Durée invalide : " + seconds);
        automationTimers.put(automationId, seconds);
    }

    /** Vue en lecture seule des niveaux de cadence, pour la sauvegarde. */
    public Map<String, Integer> automationSpeedLevels() {
        return Map.copyOf(automationSpeedLevels);
    }

    /** Nombre d'exemplaires possédés d'un élément du tableau périodique (0 si jamais obtenu). */
    public int elementCount(int atomicNumber) {
        return elements.getOrDefault(atomicNumber, 0);
    }

    public void setElementCount(int atomicNumber, int count) {
        if (count < 0) throw new IllegalArgumentException("Nombre d'exemplaires négatif");
        if (count == 0) {
            elements.remove(atomicNumber);
        } else {
            elements.put(atomicNumber, count);
        }
        elementsVersion++;
    }

    /** Compteur qui change à chaque modification des éléments possédés ; sert à savoir quand recalculer leurs bonus. */
    public int elementsVersion() {
        return elementsVersion;
    }

    /** Nombre de synthèses déjà faites : c'est lui qui fixe le prix de la suivante. */
    public int synthesisCount() {
        return synthesisCount;
    }

    public void setSynthesisCount(int synthesisCount) {
        if (synthesisCount < 0) throw new IllegalArgumentException("Nombre de synthèses négatif");
        this.synthesisCount = synthesisCount;
    }

    /** Famille visée par la synthèse ciblée, ou {@code null} quand la synthèse tire au hasard. */
    public ElementCategory synthesisTarget() {
        return synthesisTarget;
    }

    public void setSynthesisTarget(ElementCategory synthesisTarget) {
        this.synthesisTarget = synthesisTarget;
    }

    /** Synthèses déjà payées pour la synthèse ciblée en cours, sans avoir encore rien donné. */
    public int synthesisTries() {
        return synthesisTries;
    }

    public void setSynthesisTries(int synthesisTries) {
        if (synthesisTries < 0) throw new IllegalArgumentException("Nombre d'essais négatif");
        this.synthesisTries = synthesisTries;
    }

    /** Matière noire possédée : la troisième ressource, laissée par chaque explosion du tableau périodique. */
    public BigNum darkMatter() {
        return darkMatter;
    }

    public void setDarkMatter(BigNum darkMatter) {
        if (darkMatter.sign() < 0) throw new IllegalArgumentException("La quantité de matière noire ne peut pas être négative");
        this.darkMatter = darkMatter;
    }

    /** Taille de la matière noire, en mètres : elle grossit tant que le joueur la maintient appuyée. */
    /**
     * Matière noire dépensée en améliorations depuis le début du jeu. Ajoutée à celle qui reste
     * ({@link #darkMatter()}), elle donne la matière noire gagnée en tout.
     */
    public BigNum darkMatterSpent() {
        return darkMatterSpent;
    }

    public void setDarkMatterSpent(BigNum darkMatterSpent) {
        if (darkMatterSpent.sign() < 0) throw new IllegalArgumentException("La matière noire dépensée ne peut pas être négative");
        this.darkMatterSpent = darkMatterSpent;
    }

    public BigNum darkMatterSize() {
        return darkMatterSize;
    }

    public void setDarkMatterSize(BigNum darkMatterSize) {
        if (darkMatterSize.sign() <= 0) throw new IllegalArgumentException("La taille de la matière noire doit être positive");
        this.darkMatterSize = darkMatterSize;
    }

    /** Nombre d'explosions déclenchées depuis le début du jeu. */
    public int explosions() {
        return explosions;
    }

    public void setExplosions(int explosions) {
        if (explosions < 0) throw new IllegalArgumentException("Nombre d'explosions négatif");
        this.explosions = explosions;
    }

    /**
     * Nombre de fois où le tableau périodique s'est alourdi : une par explosion, sauf celles qui
     * terminent un défi déjà réussi. Aucune explosion ne le remet à zéro.
     */
    public int tableWeightLevel() {
        return tableWeightLevel;
    }

    public void setTableWeightLevel(int tableWeightLevel) {
        if (tableWeightLevel < 0) throw new IllegalArgumentException("Masse du tableau négative");
        this.tableWeightLevel = tableWeightLevel;
    }

    /** Nombre de molécules de cette sorte créées. Ni l'explosion ni le Big Bang ne les reprennent. */
    public int moleculeCount(String moleculeId) {
        return molecules.getOrDefault(moleculeId, 0);
    }

    /** Ajoute une molécule de cette sorte, à la suite de celles déjà créées. */
    public void addMolecule(String moleculeId) {
        if (moleculeId == null || moleculeId.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        molecules.merge(moleculeId, 1, Integer::sum);
        moleculeLog.add(moleculeId);
        moleculesVersion++;
    }

    /**
     * Fixe le nombre de molécules d'une sorte : celles qui manquent sont ajoutées à la suite, celles
     * en trop sont retirées en commençant par les dernières créées.
     */
    public void setMoleculeCount(String moleculeId, int count) {
        if (count < 0) throw new IllegalArgumentException("Nombre de molécules négatif : " + count);
        while (moleculeCount(moleculeId) < count) addMolecule(moleculeId);
        while (moleculeCount(moleculeId) > count) {
            moleculeLog.remove(moleculeLog.lastIndexOf(moleculeId));
            if (molecules.merge(moleculeId, -1, Integer::sum) == 0) molecules.remove(moleculeId);
            moleculesVersion++;
        }
    }

    /** Vue en lecture seule des molécules créées (identifiant → nombre), pour la sauvegarde. */
    public Map<String, Integer> molecules() {
        return Map.copyOf(molecules);
    }

    /**
     * Les molécules créées, une entrée par molécule, dans l'ordre de leur création : c'est l'ordre
     * dans lequel elles ont pris place dans l'espace.
     */
    public List<String> moleculeLog() {
        return Collections.unmodifiableList(moleculeLog);
    }

    /**
     * Vrai si les molécules de cette sorte sont rassemblées en leur substance (gaz, liquide ou
     * solide). Ni l'explosion ni le Big Bang ne défont un rassemblement.
     */
    public boolean hasSubstance(String moleculeId) {
        return substances.contains(moleculeId);
    }

    /** Note les molécules de cette sorte comme rassemblées. */
    public void addSubstance(String moleculeId) {
        if (substances.add(moleculeId)) moleculesVersion++;
    }

    /** Les sortes de molécules rassemblées, dans l'ordre où elles l'ont été. */
    public List<String> substances() {
        return List.copyOf(substances);
    }

    /** Vrai si cet assemblage est formé. Ni l'explosion ni le Big Bang ne le défont. */
    public boolean hasAssembly(String assemblyId) {
        return assemblies.contains(assemblyId);
    }

    /** Note un assemblage comme formé. */
    public void addAssembly(String assemblyId) {
        if (assemblies.add(assemblyId)) moleculesVersion++;
    }

    /** Les assemblages formés, dans l'ordre où ils l'ont été. */
    public List<String> assemblies() {
        return List.copyOf(assemblies);
    }

    /** Vrai si cet astre est formé. Ni l'explosion ni le Big Bang ne le défont. */
    public boolean hasBody(String bodyId) {
        return bodies.contains(bodyId);
    }

    /** Note un astre comme formé. */
    public void addBody(String bodyId) {
        if (bodies.add(bodyId)) moleculesVersion++;
    }

    /** Les astres formés, dans l'ordre où ils l'ont été. */
    public List<String> bodies() {
        return List.copyOf(bodies);
    }

    /** Vrai si cette amélioration d'espace est acquise. Ni l'explosion ni le Big Bang ne la reprennent. */
    public boolean ownsSpaceUpgrade(String spaceUpgradeId) {
        return spaceUpgrades.contains(spaceUpgradeId);
    }

    /** Note une amélioration d'espace comme acquise. */
    public void addSpaceUpgrade(String spaceUpgradeId) {
        spaceUpgrades.add(spaceUpgradeId);
        moleculesVersion++;
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
        return Collections.unmodifiableSet(completedChallenges);
    }

    public void addCompletedChallenge(String challengeId) {
        completedChallenges.add(challengeId);
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
        return achievements.add(achievementId);
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
        started = false;
        timePlayed = 0;
        challengeTimes.clear();
        achievements.clear();
        bigBangs = 0;
        molecules.clear();
        moleculeLog.clear();
        substances.clear();
        assemblies.clear();
        bodies.clear();
        spaceUpgrades.clear();
        moleculesVersion++;
        space = BigNum.ZERO;
        stats = new GameStats();
    }

    /**
     * Efface tout ce qui vient de l'explosion : la matière noire, gagnée comme dépensée, sa taille,
     * son arbre, ses automatismes, la masse du tableau, le nombre d'explosions et les défis
     * réussis, avec les réglages que l'arbre avait ouverts. Les records des défis restent, comme
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
        fusionThreshold = 0;
        synthesisReserve = BigNum.ZERO;
        holdLocked = false;
        activeChallenge = null;
        completedChallenges.clear();
        enabledDarkAutomations.clear();
        darkAutomationTimers.clear();
    }

    /**
     * Efface toute la matière : particules, atomes, améliorations, automatismes et éléments.
     * Il ne reste que le premier générateur, la matière noire, sa taille et ses améliorations,
     * le nombre d'explosions et le temps de jeu.
     */
    public void clearMatter() {
        particles = BigNum.ZERO;
        atoms = BigNum.ZERO;
        totalAtoms = BigNum.ZERO;
        timeSinceFusion = 0;
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
        decayDebt = 0;
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
