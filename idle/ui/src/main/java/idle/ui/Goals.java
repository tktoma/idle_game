package idle.ui;

import idle.core.BigNum;
import idle.core.Game;
import idle.core.GameStats;
import idle.core.PeriodicTable;
import idle.core.Upgrade;
import idle.core.Effect;
import idle.core.Resource;

/**
 * Le prochain grand pas de la partie, en une phrase, et le temps qu'il demande quand le jeu peut
 * l'estimer. Sert à la ligne d'objectif sous les onglets et à l'onglet Statistiques.
 */
final class Goals {

    /** Le prochain grand pas de la partie en cours, sans estimation de temps. */
    static String next(Game game) {
        if (!game.isStarted()) return "créer le premier générateur";
        if (game.canBigBang()) return "déclencher le Big Bang, au bout de l'arbre de matière noire";
        if (game.canExplode()) return "faire exploser le tableau périodique";
        if (game.isPeriodicTableUnlocked()) {
            if (!game.isSynthesisAutomationUnlocked()) return "obtenir un élément unique ★ pour automatiser la synthèse";
            // Les 118 découverts, le tableau peut exploser : c'est le cas traité plus haut.
            return "découvrir les " + PeriodicTable.ELEMENTS.size() + " éléments (" + game.discoveredElements() + " pour l'instant)";
        }
        if (game.state().totalAtoms().sign() > 0) {
            return "créer " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes pour ouvrir l'automatisation et le tableau périodique ("
                    + Format.count(game.state().totalAtoms()) + " pour l'instant)";
        }
        if (!game.hasAllGenerators()) return "débloquer les " + game.generatorsPerAtom() + " générateurs";
        return "fusionner les générateurs en un premier atome";
    }

    /**
     * L'objectif, suivi d'une estimation quand il y en a une : le temps avant le prochain
     * générateur, ou le nombre de fusions et le temps qu'il reste avant le but.
     */
    static String withEstimate(Game game, GameStats stats) {
        String goal = next(game);
        if (!game.isStarted() || game.canBigBang() || game.canExplode()) return goal;
        String estimate = "";
        if (game.isPeriodicTableUnlocked()) {
            // Dans le tableau : ce qui sépare de la prochaine synthèse.
            if (!game.isPeriodicTableComplete()) {
                long fusions = game.fusionsUntilAtoms(game.synthesisCost());
                estimate = fusions <= 0 ? "prochaine synthèse payable" : "prochaine synthèse dans " + fusions(fusions, stats);
            }
        } else if (game.state().totalAtoms().sign() > 0) {
            BigNum missing = Game.UNLOCK_TOTAL_ATOMS.subtract(game.state().totalAtoms());
            long fusions = missing.sign() <= 0 ? 0
                    : (long) Math.ceil(missing.divide(game.atomsPerFusion()).toDouble() - 1e-9);
            if (fusions > 0) estimate = "encore " + fusions(fusions, stats);
        } else if (!game.hasAllGenerators()) {
            for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
                if (!(upgrade.effect() instanceof Effect.AddGenerator)) continue;
                double wait = game.secondsUntilParticles(game.costOf(upgrade.id()));
                estimate = wait <= 0 ? "prochain générateur payable"
                        : Double.isInfinite(wait) ? "" : "prochain générateur dans " + Format.wait(wait);
            }
        }
        return estimate.isEmpty() ? goal : goal + "  ·  " + estimate;
    }

    /** « 3 fusions, environ 12 min » : le temps vient de la durée de la dernière partie entre deux fusions. */
    private static String fusions(long count, GameStats stats) {
        String text = Format.whole(count) + (count > 1 ? " fusions" : " fusion");
        double last = stats.fusions() > 0 ? stats.lastFusionTime() : 0;
        return last > 0 ? text + ", environ " + Format.wait(count * last) : text;
    }

    private Goals() {}
}
