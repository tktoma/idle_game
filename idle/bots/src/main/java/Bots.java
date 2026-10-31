import idle.core.Game;
import idle.core.GameState;
import idle.core.Resource;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Ce que tous les bots partagent (version 0.141) : la façon de charger une partie sur le vrai moteur, avec les
 * catalogues par défaut et un hasard de graine connue, et deux choix « le moins cher » que plusieurs politiques
 * refaisaient chacune de leur côté.
 */
final class Bots {
    private Bots() {}

    /** Le dernier Big Bang que les bots préparent : le cinquième, puis un par palier du ciel (18, 24, 28 astres mûrs). */
    static final int LAST_BIG_BANG = 5 + Game.BIG_BANG_SKY.length;

    /** Une partie sur cet état, avec les catalogues du jeu et ce hasard. */
    static Game game(GameState state, long seed) {
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT,
                new Random(seed));
    }

    /**
     * Version 0.164 : une partie sans certaines améliorations (ordinaires ou de l'arbre de matière noire), nommées par leur
     * identifiant. Elles restent au catalogue (le jeu et les bots les nomment), mais à un prix qu'on n'atteint jamais :
     * personne ne les achète. Ce que le jeu offre lui-même (la Persistance d'une bulle de l'arbre, par exemple) reste offert.
     */
    static Game game(GameState state, long seed, java.util.Set<String> sans) {
        if (sans.isEmpty()) return game(state, seed);
        idle.core.math.BigNum jamais = idle.core.math.BigNum.pow10(1e15);
        java.util.List<idle.core.upgrades.Upgrade> upgrades = Upgrades.DEFAULT.stream().map(u -> !sans.contains(u.id()) ? u
                : new idle.core.upgrades.Upgrade(u.id(), u.name(), u.resource(), jamais, u.costGrowth(), u.maxLevel(), u.effect())).toList();
        java.util.List<idle.core.darkmatter.DarkUpgrade> dark = DarkUpgrades.DEFAULT.stream().map(d -> !sans.contains(d.id()) ? d
                : new idle.core.darkmatter.DarkUpgrade(d.id(), d.name(), d.branch(), jamais, d.costGrowth(), d.maxLevel(), d.requires(), d.effect(),
                        d.fork())).toList();
        return new Game(state, upgrades, Automations.DEFAULT, dark, Achievements.DEFAULT, new Random(seed));
    }

    /**
     * Vrai pour une amélioration retirée du jeu ({@link #game(GameState, long, java.util.Set)}) : son prix est hors
     * d'atteinte. Les politiques la sautent au lieu de l'attendre (version 0.164).
     */
    static boolean interdite(Game game, String upgradeId) {
        return game.costOf(upgradeId).log10() > 1e12;
    }

    /** Une partie neuve, démarrée, avec ce hasard. */
    static Game fresh(long seed) {
        Game game = game(new GameState(), seed);
        game.start();
        return game;
    }

    /** La partie d'une sauvegarde, avec ce hasard. */
    static Game load(Path save, long seed) throws Exception {
        return game(SaveCodec.read(Files.readString(save)).state(), seed);
    }

    /** L'amélioration payable la moins chère de cette monnaie (la première en cas d'égalité), ou {@code null}. */
    static Upgrade cheapest(Game game, Resource resource) {
        Upgrade best = null;
        for (Upgrade upgrade : game.upgrades(resource)) {
            if (game.canBuy(upgrade.id()) && (best == null || game.costOf(upgrade.id()).lt(game.costOf(best.id()))))
                best = upgrade;
        }
        return best;
    }

    /**
     * La molécule de cet état, parmi celles que le filtre retient, dont un exemplaire occupe le moins d'espace (la
     * première en cas d'égalité), ou {@code null}.
     */
    static Molecule cheapestInState(Game game, Molecule.State state, Predicate<Molecule> filter) {
        Molecule best = null;
        double smallest = Double.POSITIVE_INFINITY;
        for (Molecule molecule : game.molecules()) {
            if (!molecule.hasState() || molecule.state() != state || !filter.test(molecule)) continue;
            double volume = game.moleculeVolume(molecule.id()).toDouble();
            if (volume < smallest) {
                best = molecule;
                smallest = volume;
            }
        }
        return best;
    }
}
