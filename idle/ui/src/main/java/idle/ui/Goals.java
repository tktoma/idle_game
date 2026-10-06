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

    /**
     * Le prochain pas du troisième acte, celui qui s'ouvre au premier Big Bang et que ni l'explosion
     * ni le Big Bang n'effacent : une amélioration d'espace à prendre, une première molécule, un
     * premier rassemblement, un premier assemblage, le prochain astre, la galaxie. Vide avant le
     * premier Big Bang.
     */
    static String act(Game game) {
        if (!game.isBigBangUnlocked()) return "";
        if (game.hasGalaxy()) return "la galaxie est formée";
        if (game.canFormGalaxy()) return "former la galaxie";
        // Une amélioration que l'espace créé permet déjà de prendre : elle ne coûte rien, elle passe d'abord.
        for (idle.core.SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (game.canBuySpaceUpgrade(upgrade.id())) return "prendre l'amélioration « " + upgrade.name() + " »";
        }
        if (game.moleculesCreated() == 0) {
            // Après un Big Bang le tableau périodique est vide : il faut d'abord le regarnir, et laisser l'espace grandir.
            boolean elements = false;
            for (idle.core.Molecule molecule : game.molecules()) {
                if (game.canCreateMolecule(molecule.id())) return "créer une première molécule";
                elements |= game.isMoleculeKindUnlocked(molecule.kind()) && game.hasElementsForMolecule(molecule.id());
            }
            return elements ? "attendre l'espace d'une première molécule"
                    : "regarnir le tableau périodique pour créer une première molécule";
        }
        if (game.isBodiesUnlocked()) {
            // Le prochain astre : le premier du catalogue qui n'est pas formé et dont les astres de départ le sont.
            for (idle.core.Body body : game.bodies()) {
                if (game.hasBody(body.id())) continue;
                boolean reachable = true;
                for (String smaller : body.bodies()) reachable &= game.hasBody(smaller);
                if (!reachable) continue;
                return "former " + article(body.name()) + " (" + game.bodyConditionsMet(body.id()) + "/" + body.conditions()
                        + " conditions)";
            }
        }
        if (game.isAssembliesUnlocked() && game.assembliesFormed() == 0) return "former un premier assemblage";
        if (game.isStatesUnlocked() && game.substancesFormed() == 0) {
            return "rassembler " + Game.SUBSTANCE_MOLECULES + " molécules d'une même sorte";
        }
        // Sinon, la prochaine amélioration d'espace : la moins chère de celles qui sont proposées.
        idle.core.SpaceUpgrade next = null;
        for (idle.core.SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (game.ownsSpaceUpgrade(upgrade.id()) || !game.isSpaceUpgradeAvailable(upgrade.id())) continue;
            if (next == null || upgrade.space().lt(next.space())) next = upgrade;
        }
        if (next != null) {
            double wait = game.secondsUntilSpace(next.space());
            return "atteindre " + Format.count(next.space()) + " d'espace pour « " + next.name() + " »"
                    + (wait <= 0 || Double.isInfinite(wait) ? "" : ", dans " + Format.wait(wait));
        }
        return game.isAssembliesUnlocked() ? "former d'autres assemblages" : "créer d'autres molécules";
    }

    /** « un amas rocheux », « une comète glacée », « le Soleil » : le nom d'un astre avec son article. */
    private static String article(String name) {
        if (name.equals("Soleil")) return "le Soleil";
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        boolean feminine = lower.startsWith("comète") || lower.startsWith("lune") || lower.startsWith("planète")
                || lower.startsWith("géante") || lower.startsWith("naine") || lower.startsWith("étoile");
        return (feminine ? "une " : "un ") + lower;
    }

    /** « 3 fusions, environ 12 min » : le temps vient de la durée de la dernière partie entre deux fusions. */
    private static String fusions(long count, GameStats stats) {
        String text = Format.whole(count) + (count > 1 ? " fusions" : " fusion");
        double last = stats.fusions() > 0 ? stats.lastFusionTime() : 0;
        return last > 0 ? text + ", environ " + Format.wait(count * last) : text;
    }

    private Goals() {}
}
