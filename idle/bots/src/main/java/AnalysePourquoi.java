import idle.core.Game;
import idle.core.achievements.Achievement;
import idle.core.bigbang.BangChallenge;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkAutomations;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Automations;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Le « pourquoi » du bot analyste (version 0.162) : ce que demande et rapporte un défi, ce qu'apporte un succès, ce
 * que fait un automatisme. Les textes reprennent ceux du jeu (les fenêtres des défis et des succès), en plus court ;
 * les nombres viennent des constantes du moteur, pour rester justes quand l'équilibrage change.
 */
final class AnalysePourquoi {

    private AnalysePourquoi() {}

    private static String fois(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.FRANCE, "%.2f", value);
    }

    private static String pourcent(double fraction) {
        return String.format(Locale.FRANCE, "%.0f %%", 100 * fraction);
    }

    /** Le défi d'explosion de cet identifiant, ou {@code null}. */
    static Challenge defi(String id) {
        for (Challenge c : Challenges.ALL) if (c.id().equals(id)) return c;
        return null;
    }

    /** Le défi de Big Bang de cet identifiant, ou {@code null}. */
    static BangChallenge defiBigBang(String id) {
        for (BangChallenge c : BangChallenge.values()) if (c.name().equals(id)) return c;
        return null;
    }

    /** La contrainte d'un défi d'explosion, en quelques mots. */
    static String contrainte(Challenge c) {
        return switch (c.rule()) {
            case RUSTY_AUTOMATIONS -> "automatismes " + fois(Game.RUSTY_FACTOR) + " fois plus lents";
            case HEAVY_FUSION -> Game.HEAVY_FUSION_FACTOR + " fois plus de générateurs par fusion";
            case VOLATILE_PARTICLES -> pourcent(Game.VOLATILE_LOSS_PER_SECOND) + " des particules perdues par seconde";
            case AMNESIA -> "la fusion reprend les achats en atomes";
            case INERT_ELEMENTS -> "les éléments ne donnent aucun bonus";
            case DECAY -> "les éléments se désintègrent";
            case PRESENCE_ONLY -> "seules les voies de présence de l'arbre agissent";
            case ABSENCE_ONLY -> "seules les voies d'absence de l'arbre agissent";
            case CUT_BRANCH -> "la branche des particules de l'arbre n'agit plus";
            case BARE_TREE -> "ni voie ni liaison de l'arbre n'agit";
            case LEAK -> "les particules fuient de plus en plus vite sans achat";
        };
    }

    /** Ce que laisse un défi d'explosion réussi, en quelques mots. */
    static String recompense(Challenge c) {
        return switch (c.reward()) {
            case FASTER_AUTOMATIONS -> "automatismes ÷" + fois(Game.REWARD_AUTOMATION_DIVISOR);
            case GROUP_BONUS -> "prime de groupe de la fusion de " + pourcent(Game.FUSION_GROUP_BONUS) + " à " + pourcent(Game.REWARD_GROUP_BONUS);
            case STRONGER_MILESTONES -> "paliers de vitesse ×" + fois(2 + Game.REWARD_MILESTONE_EXTRA) + " au lieu de ×2";
            case CHEAPER_ATOM_UPGRADES -> "achats en atomes " + fois(Game.REWARD_ATOM_UPGRADE_DIVISOR) + " fois moins chers";
            case STRONGER_ELEMENTS -> "éléments " + pourcent(Game.REWARD_ELEMENT_STRENGTH - 1) + " plus forts";
            case EXTRA_ELEMENT -> "+" + Game.REWARD_EXTRA_ELEMENTS + " élément par synthèse";
            case STRONGER_PRESENCE -> "voies de présence ×2";
            case STRONGER_ABSENCE -> "voies d'absence ×2";
            case STRONGER_LINKS -> "liaisons et accords ×2";
            case EXTRA_DARK_MATTER -> "+1 matière noire par explosion";
            case BIG_BANG_CALM -> "le calme du Big Bang";
        };
    }

    /** Pourquoi un défi se lance : ce qu'il demande, ce qu'il rapporte, et ce qui le rend utile maintenant. */
    static List<String> lancement(Game g, String id) {
        List<String> out = new ArrayList<>();
        Challenge c = defi(id);
        if (c != null) {
            out.add("Contrainte : " + contrainte(c) + ". Commencer remet la partie à zéro sans rien rapporter.");
            out.add("Récompense, gardée pour toujours : " + recompense(c) + ".");
            out.add("Ouvert dès " + c.darkMatter() + " matière noire gagnée (elle n'est pas dépensée).");
        }
        BangChallenge b = defiBigBang(id);
        if (b != null) {
            out.add("Contrainte : " + b.rule());
            out.add("Récompense, gardée pour toujours : " + b.rewardText() + ".");
            out.add(b.keepsRun() ? "Commencer ne remet rien à zéro." : "Commencer remet la partie à zéro comme un Big Bang, sans en compter un.");
        }
        for (var condition : g.bigBangConditions()) {
            if (condition == idle.core.bigbang.BigBangCondition.CHALLENGES && !g.isBigBangConditionMet(condition)) {
                out.add("Les défis d'explosion sont une condition du Big Bang n° " + (g.bigBangs() + 1) + ", pas encore remplie : il faut les faire.");
            }
        }
        return out;
    }

    /** Ce qu'apporte un succès : ce qu'il demandait, son bonus propre, et le bonus commun à tous. */
    static List<String> succes(Game g, String id) {
        List<String> out = new ArrayList<>();
        for (Achievement a : g.achievements()) {
            if (!a.id().equals(id)) continue;
            out.add("Il demandait : " + a.description());
            if (a.hasBonus()) out.add("Bonus propre : +" + pourcent(a.amount()) + " " + bonus(a.bonus()) + ".");
        }
        out.add("Comme chaque succès : +" + pourcent(Game.ACHIEVEMENT_PARTICLES) + " de particules.");
        for (var condition : g.bigBangConditions()) {
            if (condition == idle.core.bigbang.BigBangCondition.ACHIEVEMENTS) {
                out.add("Les succès comptent pour le Big Bang n° " + (g.bigBangs() + 1)
                        + (g.isBigBangConditionMet(condition) ? " : la condition est remplie." : " : la condition n'est pas encore remplie."));
            }
        }
        return out;
    }

    /** Le bonus propre d'un succès, en mots. */
    static String bonus(Achievement.Bonus bonus) {
        return switch (bonus) {
            case NONE -> "";
            case PARTICLES -> "de particules à chaque création";
            case ATOMS -> "d'atomes à chaque fusion";
            case AUTOMATION -> "de cadence des automatismes ordinaires";
            case SYNTHESIS_COST -> "de réduction du prix de la synthèse";
            case GENERATOR_COST -> "de réduction du prix des générateurs";
            case EXPANSION -> "de croissance de la matière noire";
        };
    }

    /** Ce que fait un automatisme, ordinaire, de matière noire ou de dimension ; vide s'il est inconnu. */
    static String role(String id) {
        for (Automation a : Automations.DEFAULT) {
            if (!a.id().equals(id)) continue;
            return switch (a.kind()) {
                case UPGRADE -> "achète « " + AnalyseNoms.chose(a.upgradeId()) + " » dès que possible";
                case FUSION -> "fusionne les générateurs dès que possible";
                case SYNTHESIS -> "synthétise un élément dès que possible";
            } + ", une action toutes les " + fois(a.baseInterval()) + " s au départ (moins avec la cadence)";
        }
        for (DarkAutomation d : DarkAutomations.DEFAULT) {
            if (!d.id().equals(id)) continue;
            return switch (d.kind()) {
                case GRANT_AUTOMATIONS -> "offre les automatismes ordinaires dès le début de chaque partie";
                case ATOM_UPGRADES -> "achète les améliorations payées en atomes, la moins chère d'abord";
                case AUTOMATIONS -> "achète les automatismes ordinaires et leur cadence, le moins cher d'abord";
                case EXPLOSION -> "fait exploser le tableau dès que c'est possible";
                case DARK_TREE -> "rachète l'arbre de matière noire après un Big Bang";
            } + ", toutes les " + fois(d.interval()) + " s";
        }
        for (DimensionAutomation d : DimensionAutomation.values()) {
            if (!d.name().equals(id)) continue;
            return d.kind() == DimensionAutomation.Kind.PLAY ? "joue la dimension " + d.dimension() + " à la place du joueur (" + d.label() + ")"
                    : "achète « " + d.label() + " » de la dimension " + d.dimension() + " dès que c'est payable";
        }
        return "";
    }
}
