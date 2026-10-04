package idle.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Set<String> automatedUpgrades = new HashSet<>();

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

    /** Vrai si le joueur a activé l'achat automatique de cette amélioration. */
    public boolean isAutomated(String upgradeId) {
        return automatedUpgrades.contains(upgradeId);
    }

    public void setAutomated(String upgradeId, boolean automated) {
        if (automated) {
            automatedUpgrades.add(upgradeId);
        } else {
            automatedUpgrades.remove(upgradeId);
        }
    }

    /** Vue en lecture seule des achats automatiques activés, pour la sauvegarde. */
    public Set<String> automatedUpgrades() {
        return Set.copyOf(automatedUpgrades);
    }

    /** Vue en lecture seule des niveaux, pour la sauvegarde. */
    public Map<String, Integer> upgradeLevels() {
        return Map.copyOf(upgradeLevels);
    }
}
