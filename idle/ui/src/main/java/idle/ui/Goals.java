package idle.ui;

import idle.core.*;
import idle.core.history.Pace;
import idle.ui.common.*;
import java.util.*;
import java.util.function.Predicate;
import idle.core.state.GameStats;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.*;
import idle.core.upgrades.*;

/**
 * Le prochain grand pas de la partie, en une phrase, et le temps qu'il demande quand le jeu peut
 * l'estimer. Sert aux objectifs du bandeau, en haut à gauche ({@link #lines}), et à l'onglet Statistiques.
 */
public final class Goals {
    /**
     * Les morceaux de la ligne d'objectif, que les réglages montrent ou cachent un par un. Le chrono garde
     * son propre réglage, d'avant ({@code Settings.chrono}).
     */
    public enum Part {
        GOAL("L'objectif de la partie", "Le prochain grand pas de la partie."),
        ESTIMATE("Le temps qu'il demande", "Ce qui sépare de l'objectif, quand le jeu peut l'estimer : fusions, synthèse, générateur."),
        BIG_BANG("Le prochain Big Bang", "Les conditions du prochain Big Bang, sans ouvrir l'arbre."),
        ACT("Le troisième acte", "Le prochain pas du ciel, après le premier Big Bang."),
        CHRONO("Le chrono", "L'avance ou le retard sur le record de la prochaine étape, quand il y en a un."),
        PACE("La prochaine étape", "Le temps qu'il reste avant la prochaine étape, au rythme des dix dernières minutes.");

        private final String label;
        private final String detail;

        Part(String label, String detail) {
            this.label = label;
            this.detail = detail;
        }

        public String label() { return label; }

        public String detail() { return detail; }
    }

    /**
     * La ligne d'objectif, morceau par morceau : l'objectif et son temps estimé, le prochain Big Bang, le
     * troisième acte, le chrono, la prochaine étape au rythme du moment ; seuls ceux que {@code shown} garde. Ce qui reste se suit, séparé par des barres ; vide
     * si rien ne reste.
     */
    public static String line(Game game, Predicate<Part> shown, Pace pace) {
        List<String> parts = new ArrayList<>();
        if (shown.test(Part.GOAL)) parts.add("Objectif : " + (shown.test(Part.ESTIMATE) ? withEstimate(game, game.stats()) : next(game)));
        // Entre les deux, l'avancée du prochain Big Bang : ses conditions, sans ouvrir l'arbre.
        String bang = shown.test(Part.BIG_BANG) ? NextReset.bigBangProgress(game) : "";
        if (!bang.isEmpty()) parts.add("Prochain Big Bang : " + bang);
        // Après un premier Big Bang, deux objectifs se suivent de front : celui de la partie, et celui du troisième acte.
        String act = shown.test(Part.ACT) ? act(game) : "";
        if (!act.isEmpty()) {
            parts.add(game.hasCosmos(Cosmos.UNIVERSE) ? "Quatrième acte : la suite viendra" : "Troisième acte : " + act);
        }
        // Le mode chrono : l'écart au record sur la prochaine étape, quand il y a un record à battre.
        String chrono = shown.test(Part.CHRONO) ? chrono(game) : "";
        if (!chrono.isEmpty()) parts.add(chrono);
        // La prochaine étape et le temps qu'elle demande, quand le rythme permet de l'estimer.
        String next = shown.test(Part.PACE) ? pace(pace) : "";
        if (!next.isEmpty()) parts.add(next);
        return String.join("   |   ", parts);
    }

    /** Une ligne d'objectif du bandeau : le texte court, et le texte complet montré au survol. */
    public record Line(String brief, String full) {}

    /**
     * Les objectifs du bandeau, version 0.309 : les mêmes morceaux que {@link #line}, une ligne courte chacun (« Objectif :
     * le Big Bang », « Acte 3 : autres assemblages », « Record galaxie : 14 h 37 »), le texte de toujours au survol.
     */
    public static List<Line> lines(Game game, Predicate<Part> shown, Pace pace) {
        List<Line> lines = new ArrayList<>();
        if (shown.test(Part.GOAL)) {
            lines.add(new Line("Objectif : " + brief(game),
                    "Objectif : " + (shown.test(Part.ESTIMATE) ? withEstimate(game, game.stats()) : next(game))));
        }
        String bang = shown.test(Part.BIG_BANG) ? NextReset.bigBangProgress(game) : "";
        if (!bang.isEmpty()) {
            lines.add(new Line("Big Bang : " + game.bigBangConditionsMet() + "/" + game.bigBangConditions().size(),
                    "Prochain Big Bang : " + bang));
        }
        String act = shown.test(Part.ACT) ? act(game) : "";
        if (!act.isEmpty()) {
            lines.add(game.hasCosmos(Cosmos.UNIVERSE) ? new Line("Acte 4 : la suite", "Quatrième acte : la suite viendra")
                    : new Line("Acte 3 : " + briefAct(game, act), "Troisième acte : " + act));
        }
        String chrono = shown.test(Part.CHRONO) ? chrono(game) : "";
        if (!chrono.isEmpty()) {
            GameStats.Step step = game.nextStep();
            lines.add(new Line("Record " + Character.toLowerCase(step.label().charAt(0)) + step.label().substring(1) + " : "
                    + Format.duration(game.recordOf(step)), chrono));
        }
        String next = shown.test(Part.PACE) ? pace(pace) : "";
        if (!next.isEmpty()) lines.add(new Line(pace.step().label() + " : " + Format.wait(pace.secondsLeft()), next));
        return lines;
    }

    /** Le prochain grand pas en quelques mots : « le Big Bang », « 118 éléments (96) », « les 10 générateurs ». */
    static String brief(Game game) {
        if (!game.isStarted()) return "un premier générateur";
        if (game.canBigBang()) return "le Big Bang";
        if (game.canExplode()) return "l'explosion";
        if (game.isPeriodicTableUnlocked()) {
            if (!game.isSynthesisAutomationUnlocked()) return "un élément unique ★";
            return PeriodicTable.ELEMENTS.size() + " éléments (" + game.discoveredElements() + ")";
        }
        if (game.state().totalAtoms().sign() > 0) return Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes";
        if (!game.hasAllGenerators()) return "les " + game.generatorsPerAtom() + " générateurs";
        return "un premier atome";
    }

    /**
     * Le pas du troisième acte en quelques mots : la phrase de {@link #act} sans ce qui vient après la première parenthèse
     * ou la première virgule ; l'attente du rassemblement, elle, devient « rassemblement, 12 min ».
     */
    static String briefAct(Game game, String act) {
        Cosmos scale = game.nextCosmos();
        if (scale != null && act.startsWith("attendre que ")) {
            return "rassemblement, " + (long) Math.ceil(game.cosmosSettleRemaining(scale) / 60) + " min";
        }
        if (act.startsWith("former d'autres ")) return act.substring("former d'".length());
        if (act.startsWith("créer d'autres ")) return act.substring("créer d'".length());
        int cut = act.length();
        for (String mark : List.of(" (", ", ")) {
            int at = act.indexOf(mark);
            if (at > 0) cut = Math.min(cut, at);
        }
        return act.substring(0, cut);
    }

    /** Le prochain grand pas de la partie en cours, sans estimation de temps. */
    public static String next(Game game) {
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
     * Le prochain pas du troisième acte, celui qui s'ouvre au premier Big Bang et que ni l'explosion ni le Big Bang
     * n'effacent : une première molécule, un premier rassemblement, un premier assemblage, le prochain astre, la
     * galaxie, l'amas de galaxies, l'univers. Vide avant le premier Big Bang.
     */
    public static String act(Game game) {
        if (!game.isBigBangUnlocked()) return "";
        Cosmos scale = game.nextCosmos();
        if (scale == null) return "l'univers est formé";
        if (game.canFormCosmos(scale)) return "former " + scale.phrase();
        // La galaxie formée, il ne reste qu'à réunir la matière de l'échelle suivante.
        if (scale != Cosmos.GALAXY) {
            // Toute la matière est là, mais l'échelle d'avant se rassemble encore.
            double settle = game.cosmosSettleRemaining(scale);
            if (settle > 0 && game.cosmosConditionsMet(scale) == game.cosmosConditions(scale)) {
                return "attendre que " + scale.previous().phrase() + " se rassemble pour former " + scale.phrase()
                        + " (encore " + (long) Math.ceil(settle / 60) + " min)";
            }
            // L'état le plus en retard : c'est lui qu'il faut pousser.
            Molecule.State behind = null;
            double worst = 1;
            for (Map.Entry<Molecule.State, Integer> need : game.cosmosMatter(scale).entrySet()) {
                double share = (double) game.gatheredInState(need.getKey()) / need.getValue();
                if (share < worst) { worst = share; behind = need.getKey(); }
            }
            return "réunir la matière de " + scale.phrase() + " (" + game.cosmosConditionsMet(scale) + "/"
                    + game.cosmosConditions(scale) + " conditions"
                    + (behind == null ? "" : ", le plus en retard : " + MatterText.plural(behind) + " " + Math.round(worst * 100) + " %")
                    + ")" + spaceWait(game);
        }
        if (game.moleculesCreated() == 0) {
            // Après un Big Bang le tableau périodique est vide : il faut d'abord le regarnir, et laisser l'espace grandir.
            boolean elements = false;
            for (Molecule molecule : game.molecules()) {
                if (game.canCreateMolecule(molecule.id())) return "créer une première molécule";
                elements |= game.isMoleculeKindUnlocked(molecule.kind()) && game.hasElementsForMolecule(molecule.id());
            }
            return elements ? "attendre l'espace d'une première molécule"
                    : "regarnir le tableau périodique pour créer une première molécule";
        }
        // Le prochain astre : celui que vise aussi la liste de courses, à portée et le plus avancé.
        Body body = game.nextBody();
        if (body != null) {
            // Il ne lui manque que l'énergie du collisionneur (les deux trous noirs) : on le dit.
            if (game.needsColliderEnergy(body) && !game.hasColliderEnergyFor(body)
                    && game.bodyConditionsMet(body.id()) == game.bodyConditions(body.id()) - 1) {
                return "produire " + Format.count(BigNum.of(game.colliderEnergyNeeded(body))) + " d'énergie au collisionneur pour former "
                        + article(body.name()) + " (" + Format.count(BigNum.of(game.colliderEnergyProduced())) + " produite)";
            }
            return "former " + article(body.name()) + " (" + game.bodyConditionsMet(body.id()) + "/" + game.bodyConditions(body.id())
                    + " conditions)" + spaceWait(game);
        }
        if (game.isAssembliesUnlocked() && game.assembliesFormed() == 0) return "former un premier assemblage";
        if (game.isStatesUnlocked() && game.substancesFormed() == 0) {
            return "rassembler " + Game.SUBSTANCE_MOLECULES + " molécules d'une même sorte";
        }
        // Sinon, la prochaine amélioration d'espace : la moins chère de celles qui sont proposées.
        SpaceUpgrade next = null;
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
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

    /**
     * « , assez d'espace dans 12 min environ » : le temps d'expansion qu'il faut encore à la liste
     * de courses du prochain pas ({@link Game#secondsUntilShopping()}). Vide quand l'espace suffit
     * déjà, ou qu'il ne grandit pas. C'est un minimum : il reste à regarnir le tableau périodique.
     */
    static String spaceWait(Game game) {
        double wait = game.secondsUntilShopping();
        return wait <= 0 || Double.isInfinite(wait) || Double.isNaN(wait) ? "" : ", assez d'espace dans " + about(wait);
    }

    /** « 12 min environ », ou « plus d'un mois » : une attente estimée, sans « environ » quand elle n'est déjà qu'un ordre de grandeur. */
    public static String about(double seconds) {
        String wait = Format.wait(seconds);
        return wait.startsWith("plus") ? wait : wait + " environ";
    }

    /**
     * Le mode chrono : « Chrono : première explosion, record 23 h 50, 2 h 10 d'avance ». L'écart au
     * record sur la prochaine étape datée ({@link Game#recordGap()}). Vide tant qu'il n'y a pas de
     * record à battre : à la première partie, donc.
     */
    static String chrono(Game game) {
        GameStats.Step next = game.nextStep();
        if (next == null) return "";
        double record = game.recordOf(next);
        double gap = game.recordGap();
        if (Double.isNaN(record) || Double.isNaN(gap)) return "";
        String label = Character.toLowerCase(next.label().charAt(0)) + next.label().substring(1);
        return "Chrono : " + label + ", record " + Format.duration(record) + ", "
                + (gap <= 0 ? Format.duration(-gap) + " d'avance" : Format.duration(gap) + " de retard");
    }

    /** « Premier Big Bang dans 1 h 20 environ » ; vide sans étape à venir ou sans estimation. */
    static String pace(Pace pace) {
        if (pace == null || pace.step() == null) return "";
        double left = pace.secondsLeft();
        return Double.isNaN(left) ? "" : pace.step().label() + " dans " + about(left);
    }

    /** « un amas rocheux », « une comète glacée », « le Soleil » : le nom d'un astre avec son article. */
    public static String article(String name) {
        if (name.equals("Soleil")) return "le Soleil";
        String lower = name.toLowerCase(Locale.ROOT);
        // Les nébuleuses aussi (« une nébuleuse planétaire », pas « un »).
        boolean feminine = lower.startsWith("comète") || lower.startsWith("lune") || lower.startsWith("planète")
                || lower.startsWith("géante") || lower.startsWith("naine") || lower.startsWith("étoile")
                || lower.startsWith("nébuleuse");
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
