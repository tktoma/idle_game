package idle.ui;

import idle.core.BigBangCondition;
import idle.core.BigBangMilestone;
import idle.core.BigNum;
import idle.core.Challenge;
import idle.core.DarkAutomation;
import idle.core.DarkUpgrade;
import idle.core.Game;
import java.util.ArrayList;
import java.util.List;

/**
 * Ce que rapportera la prochaine remise à zéro, pour qu'elle se lise comme une accélération et
 * non comme une perte : ce que la matière noire de la prochaine explosion ouvrira, le palier que
 * donnera le prochain Big Bang, et ce qu'il manque encore à ses cinq conditions.
 *
 * <p>Rien n'est calculé ici que les règles ne sachent déjà : ce sont des phrases, faites avec ce
 * que {@link Game} expose.
 */
final class NextReset {

    private NextReset() {
    }

    /**
     * Ce que la matière noire de la prochaine explosion ouvrira : les automatismes de matière
     * noire, les défis et les cases de l'arbre dont le seuil sera franchi. Vide si elle n'ouvre
     * rien, ou si elle ne rapporte rien (un défi rejoué).
     */
    static List<String> explosionOpens(Game game) {
        List<String> opens = new ArrayList<>();
        BigNum gain = game.nextExplosionDarkMatter();
        if (gain.sign() <= 0) return opens;
        BigNum before = game.darkMatterEarned();
        BigNum after = before.add(gain);
        for (DarkAutomation automation : game.darkAutomations()) {
            // Celui de l'arbre attend aussi un premier Big Bang.
            if (automation.kind() == DarkAutomation.Kind.DARK_TREE && game.bigBangs() == 0) continue;
            if (crossed(automation.darkMatter(), before, after)) opens.add("l'automatisme « " + automation.name() + " »");
        }
        for (Challenge challenge : game.challenges()) {
            if (crossed(BigNum.of(challenge.darkMatter()), before, after)) opens.add("le défi « " + challenge.name() + " »");
        }
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            // Les cases du haut de l'arbre s'ouvrent toutes à la première matière noire : c'est « l'arbre », pas une liste.
            if (upgrade.darkMatter() > 1 && crossed(BigNum.of(upgrade.darkMatter()), before, after)) {
                opens.add("la case « " + upgrade.name() + " »");
            }
        }
        return opens;
    }

    private static boolean crossed(BigNum threshold, BigNum before, BigNum after) {
        return before.lt(threshold) && after.gte(threshold);
    }

    /** « , ouvre le défi « Sobriété » et 2 autres » : la suite du texte du bouton d'explosion. Vide s'il n'y a rien à annoncer. */
    static String explosionSuffix(Game game) {
        // La toute première : elle ouvre l'arbre, et c'est ce qu'il faut dire avant tout le reste.
        if (game.darkMatterEarned().sign() == 0 && game.nextExplosionDarkMatter().sign() > 0) return ", ouvre l'arbre de matière noire";
        List<String> opens = explosionOpens(game);
        if (opens.isEmpty()) return "";
        if (opens.size() == 1) return ", ouvre " + opens.get(0);
        if (opens.size() == 2) return ", ouvre " + opens.get(0) + " et " + opens.get(1);
        return ", ouvre " + opens.get(0) + " et " + (opens.size() - 1) + " autres";
    }

    /** Les paliers que donnera le prochain Big Bang : ceux du rang suivant. Vide une fois tous les paliers atteints. */
    static List<BigBangMilestone> bigBangMilestones(Game game) {
        List<BigBangMilestone> next = new ArrayList<>();
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            if (milestone.bigBangs() == game.bigBangs() + 1) next.add(milestone);
        }
        return next;
    }

    /**
     * Ce que donnera le prochain Big Bang, en une ligne : « Donnera : Expansion accélérée (espace
     * par seconde ×2) et Création automatique ». Le premier ouvre aussi le troisième acte.
     */
    static String bigBangGives(Game game) {
        List<BigBangMilestone> next = bigBangMilestones(game);
        String space = "+" + ElementText.number(Game.SPACE_PER_SECOND) + " d'espace par seconde";
        if (next.isEmpty()) return "Donnera : " + space;
        StringBuilder text = new StringBuilder("Donnera : ");
        for (int i = 0; i < next.size(); i++) {
            if (i > 0) text.append(i == next.size() - 1 ? " et " : ", ");
            BigBangMilestone milestone = next.get(i);
            text.append("« ").append(milestone.name()).append(" »");
            if (!milestone.effects().isEmpty()) {
                text.append(" (").append(lower(BigBangPage.milestoneLine(milestone.effects().get(0)))).append(')');
            }
        }
        return text + (game.bigBangs() == 0 ? ", et le troisième acte" : ", et " + space);
    }

    /**
     * L'avancée du prochain Big Bang en une ligne : « 3 conditions sur 5, il manque 120 matières
     * noires et 2 défis ». Vide tant que la matière noire n'est pas connue, et quand il est prêt.
     */
    static String bigBangProgress(Game game) {
        if (!game.isDarkMatterUnlocked() || game.canBigBang()) return "";
        int all = BigBangCondition.values().length;
        int met = game.bigBangConditionsMet();
        if (met == all) return all + " conditions sur " + all + ", pas pendant un défi";
        List<String> missing = new ArrayList<>();
        for (BigBangCondition condition : BigBangCondition.values()) {
            if (!game.isBigBangConditionMet(condition)) missing.add(missing(game, condition));
        }
        StringBuilder text = new StringBuilder(met + (met > 1 ? " conditions sur " : " condition sur ") + all + ", il manque ");
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) text.append(i == missing.size() - 1 ? " et " : ", ");
            text.append(missing.get(i));
        }
        return text.toString();
    }

    /** Ce qu'il manque à une condition du Big Bang : « 120 matières noires », « 2 défis ». */
    private static String missing(Game game, BigBangCondition condition) {
        return switch (condition) {
            case PARTICLES -> Format.count(lacking(Game.BIG_BANG_PARTICLES, game.darkBalance(DarkUpgrade.Branch.PARTICLES))) + " particules";
            case ATOMS -> Format.count(lacking(Game.BIG_BANG_ATOMS, game.state().atoms())) + " atomes";
            case DARK_MATTER -> {
                BigNum lacking = lacking(Game.BIG_BANG_DARK_MATTER, game.state().darkMatter());
                yield Format.count(lacking) + (lacking.gt(BigNum.ONE) ? " matières noires" : " matière noire");
            }
            case ACHIEVEMENTS -> Math.max(1, Game.BIG_BANG_ACHIEVEMENTS - game.achievementCount()) + " succès";
            case CHALLENGES -> {
                int lacking = Math.max(1, game.challenges().size() - game.completedChallenges());
                yield lacking + (lacking > 1 ? " défis" : " défi");
            }
        };
    }

    private static BigNum lacking(BigNum needed, BigNum owned) {
        return needed.gt(owned) ? needed.subtract(owned) : BigNum.ONE;
    }

    private static String lower(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
