package idle.ui;

import idle.core.Achievement;
import idle.core.BigNum;
import idle.core.Game;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Succès » : le compte, ce que les succès rapportent en tout, puis une
 * tuile par succès ({@link Card}), obtenu ou non.
 *
 * <p>Les tuiles sont rangées dans l'ordre du catalogue, qui est celui où un joueur les obtient
 * d'ordinaire, et numérotées. Chacune porte le nom du succès, ce qu'il demande en quelques mots
 * et, s'il en a un, son bonus propre ; le texte complet vient au survol, ou en mode détails
 * ({@link Detail}). Les succès liés à une action, qui ont leur bonus à eux, sont dorés ; les
 * étapes ont la couleur de l'onglet.
 *
 * <p>Il n'y a pas de courbe ici : celle des succès obtenus est avec les autres, dans l'onglet
 * « Statistiques ».
 */
final class AchievementsPage extends VBox {

    private final Game game;
    private final Label countLabel = new Label();
    private final Label bonusLabel = new Label();
    private final Label hintLabel = new Label();
    private final Map<Achievement, Card> tiles = new LinkedHashMap<>();

    AchievementsPage(Game game) {
        super(8);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        countLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.ACHIEVEMENTS_COLOR + ";");
        bonusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        bonusLabel.setWrapText(true);
        bonusLabel.setTextAlignment(TextAlignment.CENTER);
        hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        hintLabel.setWrapText(true);
        hintLabel.setTextAlignment(TextAlignment.CENTER);
        hintLabel.setMaxWidth(640);
        hintLabel.setText("Chaque succès ajoute " + AchievementText.common() + ", quel qu'il soit. Les tuiles dorées "
                + "récompensent une action précise et portent en plus un bonus à elles. L'ordre est celui où on les "
                + "obtient d'ordinaire ; survolez une tuile pour lire ce qu'elle demande.");

        TileGrid grid = new TileGrid(150, 5, 6);
        grid.setMaxWidth(820);
        grid.setPadding(new Insets(8, 0, 0, 0));
        for (Achievement achievement : game.achievements()) {
            Card tile = new Card(achievement.hasBonus() ? GameApp.ATOMS_COLOR : GameApp.ACHIEVEMENTS_COLOR);
            tiles.put(achievement, tile);
            grid.add(tile);
        }

        getChildren().add(countLabel);
        getChildren().add(bonusLabel);
        getChildren().add(hintLabel);
        getChildren().add(grid);
    }

    /** La tuile d'un succès : pour les vérifications. */
    Card tile(Achievement achievement) {
        return tiles.get(achievement);
    }

    /** Recopie l'état des succès dans le compte, le bilan des bonus et les tuiles. */
    void refresh() {
        int got = game.achievementCount();
        countLabel.setText(got + " / " + game.achievements().size() + (got > 1 ? " succès obtenus" : " succès obtenu"));
        bonusLabel.setText(summary());
        hintLabel.setVisible(Detail.shown());
        hintLabel.setManaged(Detail.shown());

        int rank = 0;
        for (Map.Entry<Achievement, Card> entry : tiles.entrySet()) {
            Achievement achievement = entry.getKey();
            Card tile = entry.getValue();
            boolean owned = game.hasAchievement(achievement.id());
            String own = AchievementText.bonus(achievement);
            tile.show(owned ? Card.State.STARTED : Card.State.LOCKED, String.valueOf(++rank), owned ? "obtenu" : "",
                    achievement.name(), achievement.summary(), achievement.description(),
                    AchievementText.shortBonus(achievement), "");
            tile.setHint(achievement.name() + (owned ? "  (obtenu)" : "  (pas encore obtenu)") + "\n"
                    + achievement.description() + "\n"
                    + (own.isEmpty() ? "Bonus : " + AchievementText.common() + ", comme chaque succès."
                            : "Bonus propre : " + own + ".\nEn plus : " + AchievementText.common() + ", comme chaque succès."));
        }
    }

    /** Ce que les succès obtenus rapportent en tout : le bonus commun, puis les bonus propres qui ne sont pas nuls. */
    private String summary() {
        List<String> parts = new ArrayList<>();
        parts.add("particules " + Format.multiplier(BigNum.of(1 + Game.ACHIEVEMENT_PARTICLES * game.achievementCount())));
        add(parts, "+", " de particules", Achievement.Bonus.PARTICLES);
        add(parts, "+", " d'atomes par fusion", Achievement.Bonus.ATOMS);
        add(parts, "+", " de cadence", Achievement.Bonus.AUTOMATION);
        add(parts, "synthèse −", "", Achievement.Bonus.SYNTHESIS_COST);
        add(parts, "générateurs −", "", Achievement.Bonus.GENERATOR_COST);
        add(parts, "+", " de croissance de la matière noire", Achievement.Bonus.EXPANSION);
        return "Ce qu'ils rapportent : " + String.join(", ", parts);
    }

    private void add(List<String> parts, String before, String after, Achievement.Bonus bonus) {
        double amount = game.achievementBonus(bonus);
        if (amount > 0) parts.add(before + ElementText.percent(amount) + after);
    }
}
