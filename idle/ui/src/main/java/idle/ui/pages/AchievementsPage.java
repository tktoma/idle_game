package idle.ui.pages;

import idle.core.Game;
import idle.core.achievements.Achievement;
import idle.core.bigbang.BangAchievement;
import idle.core.collider.ColliderAchievement;
import idle.core.math.BigNum;
import idle.ui.GameApp;
import idle.ui.collider.ColliderPane;
import idle.ui.common.AchievementText;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.TileGrid;
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
 * tuile par succès ({@link Card}), obtenu ou non. Suivent, depuis la version 0.061, les succès du
 * collisionneur (avec la matière noire) et ceux du Big Bang (au premier Big Bang).
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
public final class AchievementsPage extends VBox {

    private final Game game;
    private final Label countLabel = new Label();
    private final Label bonusLabel = new Label();
    private final Label hintLabel = new Label();
    private final Map<Achievement, Card> tiles = new LinkedHashMap<>();
    /** Les succès du collisionneur, ouverts avec la matière noire, et ceux du Big Bang, au premier Big Bang (version 0.061). */
    private final Label colliderTitle = new Label();
    private final TileGrid colliderGrid = new TileGrid(150, 5, 6);
    private final Map<ColliderAchievement, Card> colliderTiles = new java.util.EnumMap<>(ColliderAchievement.class);
    private final Label bangTitle = new Label();
    private final TileGrid bangGrid = new TileGrid(150, 5, 6);
    private final Map<BangAchievement, Card> bangTiles = new java.util.EnumMap<>(BangAchievement.class);

    public AchievementsPage(Game game) {
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

        for (ColliderAchievement achievement : ColliderAchievement.values()) {
            Card tile = new Card(ColliderPane.COLOR);
            colliderTiles.put(achievement, tile);
            colliderGrid.add(tile);
        }
        for (BangAchievement achievement : BangAchievement.values()) {
            Card tile = new Card(GameApp.BIG_BANG_COLOR);
            bangTiles.put(achievement, tile);
            bangGrid.add(tile);
        }
        for (Label title : new Label[] {colliderTitle, bangTitle}) {
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 14 0 0 0; -fx-text-fill: "
                    + (title == colliderTitle ? ColliderPane.COLOR : GameApp.BIG_BANG_COLOR) + ";");
        }
        for (TileGrid section : new TileGrid[] {colliderGrid, bangGrid}) section.setMaxWidth(820);

        getChildren().add(countLabel);
        getChildren().add(bonusLabel);
        getChildren().add(hintLabel);
        getChildren().add(grid);
        getChildren().addAll(colliderTitle, colliderGrid, bangTitle, bangGrid);
    }

    /** La tuile d'un succès : pour les vérifications. */
    Card tile(Achievement achievement) {
        return tiles.get(achievement);
    }

    /** Recopie l'état des succès dans le compte, le bilan des bonus et les tuiles. */
    public void refresh() {
        refreshCollider();
        refreshBang();
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

    /** Les succès du collisionneur : avec la matière noire, comme le collisionneur. */
    private void refreshCollider() {
        boolean open = game.isColliderUnlocked();
        colliderTitle.setVisible(open);
        colliderTitle.setManaged(open);
        colliderGrid.setVisible(open);
        colliderGrid.setManaged(open);
        if (!open) return;
        colliderTitle.setText("Succès du collisionneur : " + game.colliderAchievements() + " / " + ColliderAchievement.values().length);
        int rank = 0;
        for (Map.Entry<ColliderAchievement, Card> entry : colliderTiles.entrySet()) {
            ColliderAchievement achievement = entry.getKey();
            boolean owned = game.hasColliderAchievement(achievement);
            String progress = owned ? "" : game.colliderAchievementProgress(achievement) + "/" + achievement.threshold();
            entry.getValue().show(owned ? Card.State.STARTED : Card.State.LOCKED, String.valueOf(++rank), owned ? "obtenu" : progress,
                    achievement.title(), achievement.condition(), "", achievement.reward(), "");
            entry.getValue().setHint(achievement.title() + (owned ? "  (obtenu)" : "  (" + progress + ")") + "\n"
                    + achievement.condition() + " : " + achievement.reward() + ".");
        }
    }

    /** Les succès du Big Bang : à partir du premier Big Bang. */
    private void refreshBang() {
        boolean open = game.isBangAchievementsUnlocked();
        bangTitle.setVisible(open);
        bangTitle.setManaged(open);
        bangGrid.setVisible(open);
        bangGrid.setManaged(open);
        if (!open) return;
        bangTitle.setText("Succès du Big Bang : " + game.bangAchievements() + " / " + BangAchievement.values().length);
        int rank = 0;
        for (Map.Entry<BangAchievement, Card> entry : bangTiles.entrySet()) {
            BangAchievement achievement = entry.getKey();
            boolean owned = game.hasBangAchievement(achievement);
            String progress = owned ? "" : Format.whole(game.bangAchievementProgress(achievement)) + "/" + Format.whole(achievement.threshold());
            String condition = Format.whole(achievement.threshold()) + " " + achievement.stat().label();
            if (achievement.stat() == BangAchievement.Stat.GALAXY) condition = "Former la galaxie";
            String reward = "+" + ElementText.percent(achievement.amount()) + " " + achievement.reward().label();
            entry.getValue().show(owned ? Card.State.STARTED : Card.State.LOCKED, String.valueOf(++rank), owned ? "obtenu" : progress,
                    achievement.title(), condition, "", reward, "");
            entry.getValue().setHint(achievement.title() + (owned ? "  (obtenu)" : "  (" + progress + ")") + "\n" + condition + " : " + reward + ".");
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
