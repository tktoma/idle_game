package idle.ui;

import idle.core.Achievement;
import idle.core.Game;

/** Les textes des succès : ce que rapporte chacun, et le bonus commun à tous. */
final class AchievementText {

    /** Le bonus propre d'un succès, en quelques mots, ou une chaîne vide s'il n'en a pas. */
    static String bonus(Achievement achievement) {
        String amount = ElementText.percent(achievement.amount());
        return switch (achievement.bonus()) {
            case NONE -> "";
            case PARTICLES -> "+" + amount + " de particules";
            case ATOMS -> "+" + amount + " d'atomes par fusion";
            case AUTOMATION -> "+" + amount + " de cadence pour les automatismes";
            case SYNTHESIS_COST -> "synthèse " + amount + " moins chère";
            case GENERATOR_COST -> "générateurs " + amount + " moins chers";
            case EXPANSION -> "+" + amount + " de croissance pour la matière noire";
        };
    }

    /** Le même bonus en trois mots, pour la tuile du succès. */
    static String shortBonus(Achievement achievement) {
        String amount = ElementText.percent(achievement.amount());
        return switch (achievement.bonus()) {
            case NONE -> "";
            case PARTICLES -> "+" + amount + " particules";
            case ATOMS -> "+" + amount + " atomes";
            case AUTOMATION -> "+" + amount + " cadence";
            case SYNTHESIS_COST -> "synthèse −" + amount;
            case GENERATOR_COST -> "générateurs −" + amount;
            case EXPANSION -> "+" + amount + " croissance";
        };
    }

    /** Le bonus commun : ce que chaque succès ajoute, quel qu'il soit. */
    static String common() {
        return "+" + ElementText.percent(Game.ACHIEVEMENT_PARTICLES) + " de particules";
    }

    /** Ce qu'annonce la notification d'un succès : son nom, puis ce qu'il rapporte. */
    static String announce(Achievement achievement) {
        String own = bonus(achievement);
        return "Succès : " + achievement.name() + "  (" + common() + (own.isEmpty() ? "" : " ; bonus propre : " + own) + ")";
    }

    private AchievementText() {}
}
