package idle.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final List<Double> formations = new ArrayList<>();
    private double timePlayed = 0;
    private final Map<String, Integer> upgradeLevels = new HashMap<>();

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

    /** Atomes possédés : la deuxième ressource, obtenue en fusionnant les générateurs. */
    public BigNum atoms() {
        return atoms;
    }

    public void setAtoms(BigNum atoms) {
        if (atoms.sign() < 0) throw new IllegalArgumentException("Le nombre d'atomes ne peut pas être négatif");
        this.atoms = atoms;
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

    /** Oublie toutes les améliorations achetées (après une fusion). */
    public void clearUpgradeLevels() {
        upgradeLevels.clear();
    }

    /** Vue en lecture seule des niveaux, pour la sauvegarde. */
    public Map<String, Integer> upgradeLevels() {
        return Map.copyOf(upgradeLevels);
    }
}