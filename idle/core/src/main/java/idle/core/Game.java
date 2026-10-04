package idle.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les règles du jeu : comment la matière est produite et ce que font les améliorations.
 *
 * <p>L'interface n'a besoin que de trois choses :
 * <ul>
 *   <li>appeler {@link #tick(double)} régulièrement ;</li>
 *   <li>lire l'état pour l'afficher ({@link #state()}, {@link #productionPerSecond()}) ;</li>
 *   <li>envoyer les actions du joueur ({@link #buy(String)}).</li>
 * </ul>
 */
public final class Game {

    /** Production de départ, avant toute amélioration : 1 matière par seconde. */
    public static final BigNum BASE_PRODUCTION = BigNum.ONE;

    private final GameState state;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();

    /** Nouvelle partie avec le catalogue par défaut. */
    public Game() {
        this(new GameState(), Upgrades.DEFAULT);
    }

    /** Partie reprise depuis un état existant (sauvegarde, test…). */
    public Game(GameState state, List<Upgrade> catalog) {
        this.state = state;
        for (Upgrade upgrade : catalog) {
            if (upgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration en double : " + upgrade.id());
            }
        }
    }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    /**
     * Fait avancer le jeu de {@code dt} secondes.
     *
     * <p>Sert aussi à la progression hors-ligne : au chargement, on appelle
     * {@code tick(secondesÉcoulées)}. Comme la production ne change qu'à l'achat
     * d'une amélioration, un seul gros tick donne le même résultat que mille petits.
     */
    public void tick(double dt) {
        if (dt < 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("Durée invalide : " + dt);
        }
        state.setMatter(state.matter().add(productionPerSecond().multiply(dt)));
        state.setTimePlayed(state.timePlayed() + dt);
    }

    /**
     * Matière produite par seconde :
     * {@code (base + somme des ajouts) × produit des multiplicateurs}.
     */
    public BigNum productionPerSecond() {
        BigNum flat = BASE_PRODUCTION;
        BigNum multiplier = BigNum.ONE;
        for (Upgrade upgrade : upgrades.values()) {
            int level = state.levelOf(upgrade.id());
            if (level == 0) continue;
            switch (upgrade.effect()) {
                case Effect.AddProduction add ->
                        flat = flat.add(add.perLevel().multiply(level));
                case Effect.MultiplyProduction mul ->
                        multiplier = multiplier.multiply(BigNum.of(mul.perLevel()).pow(level));
            }
        }
        return flat.multiply(multiplier);
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    /** Toutes les améliorations, dans l'ordre du catalogue. */
    public Collection<Upgrade> upgrades() {
        return upgrades.values();
    }

    public int levelOf(String upgradeId) {
        return state.levelOf(upgrade(upgradeId).id());
    }

    /** Coût du prochain niveau de cette amélioration. */
    public BigNum costOf(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        return upgrade.costAt(state.levelOf(upgrade.id()));
    }

    public boolean canBuy(String upgradeId) {
        return state.matter().gte(costOf(upgradeId));
    }

    /**
     * Achète un niveau si le joueur a assez de matière.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buy(String upgradeId) {
        BigNum cost = costOf(upgradeId);
        if (state.matter().lt(cost)) return false;
        // max(0) : la soustraction de deux double proches peut donner -1e-15 au lieu de 0
        state.setMatter(state.matter().subtract(cost).max(BigNum.ZERO));
        state.setLevel(upgradeId, state.levelOf(upgradeId) + 1);
        return true;
    }

    public GameState state() {
        return state;
    }

    private Upgrade upgrade(String upgradeId) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration inconnue : " + upgradeId);
        return upgrade;
    }
}