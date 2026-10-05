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
    private BigNum darkMatter = BigNum.ZERO;
    private BigNum darkMatterSpent = BigNum.ZERO;
    private BigNum darkMatterSize = Game.DARK_MATTER_START_SIZE;
    private int explosions = 0;
    private final Map<String, Integer> darkUpgradeLevels = new HashMap<>();
    private int fusionThreshold = 0;
    private final Set<String> enabledDarkAutomations = new HashSet<>();
    private final Map<String, Double> darkAutomationTimers = new HashMap<>();
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
     * Efface absolument tout, matière noire et statistiques comprises : l'état redevient celui
     * d'une partie jamais commencée.
     */
    public void reset() {
        clearMatter();
        started = false;
        timePlayed = 0;
        darkMatter = BigNum.ZERO;
        darkMatterSpent = BigNum.ZERO;
        darkMatterSize = Game.DARK_MATTER_START_SIZE;
        explosions = 0;
        darkUpgradeLevels.clear();
        fusionThreshold = 0;
        enabledDarkAutomations.clear();
        darkAutomationTimers.clear();
        stats = new GameStats();
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
