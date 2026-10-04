package idle.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Tout ce qui change pendant une partie, et rien d'autre.
 * C'est cet objet qui sera écrit dans la sauvegarde.
 *
 * <p>Aucune règle du jeu ici : les règles sont dans {@link Game}.
 */
public final class GameState {

    private BigNum matter = BigNum.ZERO;
    private double timePlayed = 0;
    private final Map<String, Integer> upgradeLevels = new HashMap<>();

    /** Matière actuellement possédée. */
    public BigNum matter() {
        return matter;
    }

    public void setMatter(BigNum matter) {
        if (matter.sign() < 0) throw new IllegalArgumentException("La matière ne peut pas être négative");
        this.matter = matter;
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

    /** Vue en lecture seule des niveaux, pour la sauvegarde. */
    public Map<String, Integer> upgradeLevels() {
        return Map.copyOf(upgradeLevels);
    }
}