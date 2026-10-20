package idle.ui.pages;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.core.universe.Trial;
import idle.ui.common.Format;
import idle.ui.common.Card;
import idle.ui.common.ElementText;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * L'onglet « Skins » (version 0.87), sous les réglages : il s'ouvre dans le deuxième univers, après
 * cinq fusions. Une section par visuel du jeu, avec son apparence classique et ses skins : un skin
 * débloqué se choisit d'un clic, un skin verrouillé dit comment l'obtenir. Chaque skin débloqué
 * ajoute {@link Skin#BONUS} à toutes les ressources d'en dessous de l'univers.
 */
public final class SkinsPage extends VBox {

    /** La couleur de l'onglet : un rose de palette. */
    public static final String COLOR = "#ff9fd6";

    private final Game game;
    private final Label summary = new Label();
    private final Map<SkinVisual, Card> classics = new EnumMap<>(SkinVisual.class);
    private final Map<Skin, Card> cards = new LinkedHashMap<>();
    private final Label record = new Label();
    private final Card prism = new Card(COLOR);
    private final Card dense = new Card(COLOR);
    private final Map<Trial, Card> trials = new EnumMap<>(Trial.class);
    private Runnable changed = () -> { };

    public SkinsPage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));
        Label title = new Label("Skins");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        summary.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        summary.setWrapText(true);
        summary.setMaxWidth(760);
        summary.setTextAlignment(TextAlignment.CENTER);
        getChildren().addAll(title, summary);
        // Version 0.88 : les recherches, le record et les épreuves qui donnent les skins.
        record.setStyle("-fx-font-size: 12px; -fx-text-fill: #cfe3ff;");
        TileGrid research = new TileGrid(260, 2, 8);
        research.setMaxWidth(820);
        prism.setOnAction(() -> {
            game.buyPrismResearch();
            changed.run();
            refresh();
        });
        dense.setOnAction(() -> {
            game.buyDenseTier();
            changed.run();
            refresh();
        });
        research.add(prism);
        research.add(dense);
        TileGrid trialGrid = new TileGrid(260, 3, 8);
        trialGrid.setMaxWidth(820);
        for (Trial trial : Trial.values()) {
            Card card = new Card(COLOR);
            card.setOnAction(() -> {
                if (game.inTrial(trial)) game.abandonTrial();
                else game.startTrial(trial);
                refresh();
            });
            trials.put(trial, card);
            trialGrid.add(card);
        }
        getChildren().addAll(heading("Recherches et record"), record, research, heading("Épreuves"), trialGrid);
        for (SkinVisual visual : SkinVisual.values()) {
            Label name = new Label(visual.label());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 0 0; -fx-text-fill: " + COLOR + ";");
            TileGrid grid = new TileGrid(200, 4, 8);
            grid.setMaxWidth(820);
            Card classic = new Card(COLOR);
            classic.setDoneClickable(true);
            classic.setOnAction(() -> {
                game.chooseSkin(visual, null);
                changed.run();
                refresh();
            });
            classics.put(visual, classic);
            grid.add(classic);
            for (Skin skin : Skin.values()) {
                if (skin.visual() != visual) continue;
                Card card = new Card(COLOR);
                card.setOnAction(() -> {
                    game.chooseSkin(visual, skin);
                    changed.run();
                    refresh();
                });
                cards.put(skin, card);
                grid.add(card);
            }
            getChildren().addAll(name, grid);
        }
        refresh();
    }

    /** Ce qu'une épreuve demande pour s'ouvrir. */
    static String requirement(Trial trial) {
        return switch (trial) {
            case LONE_GENERATOR -> "après une explosion";
            case SOFT_CONTRACTION -> "matière noire découverte";
            case COLD_BANG -> "après un Big Bang";
            case DETECTOR -> "Détecteur élargi au niveau 3";
            case NO_OVERHEAT, FULL_CHARGE, LONE_COLLIDER -> "collisionneur ouvert";
        };
    }

    private static Label heading(String text) {
        Label name = new Label(text);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 0 0; -fx-text-fill: " + COLOR + ";");
        return name;
    }

    /** Ce qu'il faut refaire quand un skin vient d'être choisi : redessiner ce qu'il habille. */
    public void setOnChanged(Runnable action) {
        changed = action == null ? () -> { } : action;
    }

    public void refresh() {
        int unlocked = game.skinsUnlocked();
        summary.setText(unlocked + " / " + Skin.values().length + " skins débloqués"
                + (unlocked > 0 ? " : +" + ElementText.percent(game.skinBoost() - 1) + " à toutes les ressources d'en dessous de l'univers" : "")
                + ". Choisir un skin ne change que l'apparence ; le bonus vient de chaque skin débloqué, choisi ou non.");
        double best = game.rebuildRecord();
        record.setText("Record de reconstruction (de la fusion à " + game.generatorsPerAtom() + " générateurs) : "
                + (best > 0 ? Format.duration(best) : "aucun encore") + " — en " + (int) Game.REBUILD_FAST_SECONDS
                + " s au plus, il donne le skin « " + Skin.FUSION_FLASH.label() + " ».");
        boolean prismDone = game.hasPrismResearch();
        prism.show(prismDone ? Card.State.DONE : game.canBuyPrismResearch() ? Card.State.READY : Card.State.WAITING, "",
                prismDone ? "faite" : "", "Fusion prismatique", "Une recherche facultative : le skin « Prisme » de la fusion", "",
                prismDone ? "" : Format.big(Game.PRISM_COST) + " particules", "");
        int tiers = game.denseTiers();
        boolean denseOpen = game.isDenseResearchUnlocked();
        dense.show(tiers >= Game.DENSE_COSTS.length ? Card.State.DONE : !denseOpen ? Card.State.LOCKED
                        : game.canBuyDenseTier() ? Card.State.READY : tiers > 0 ? Card.State.STARTED : Card.State.WAITING, "",
                tiers + " / " + Game.DENSE_COSTS.length, "Noyau dense",
                denseOpen ? "Quatre paliers : deux skins d'atome, deux du tableau"
                        : "S'ouvre à " + Format.big(Game.DENSE_UNLOCK_ATOMS) + " atomes créés", "",
                tiers >= Game.DENSE_COSTS.length || !denseOpen ? "" : Format.big(Game.DENSE_COSTS[tiers]) + " atomes", "");
        Trial running = game.activeTrial();
        for (Map.Entry<Trial, Card> entry : trials.entrySet()) {
            Trial trial = entry.getKey();
            boolean done = game.isTrialCompleted(trial);
            String mark = done ? "réussie" : "";
            String price;
            Card.State state;
            if (trial == running) {
                state = Card.State.ON;
                double left = game.trialLeft();
                mark = "en cours " + ElementText.percent(game.trialProgress());
                price = (Double.isInfinite(left) ? "" : Format.duration(left) + " — ") + "Abandonner";
            } else if (!game.isTrialUnlocked(trial)) {
                state = Card.State.LOCKED;
                price = "Verrouillée : " + requirement(trial);
            } else if (game.canStartTrial(trial)) {
                state = Card.State.READY;
                price = done ? "Rejouer" : "Commencer";
            } else {
                state = done ? Card.State.DONE : Card.State.WAITING;
                price = running != null ? "Une autre est en cours" : "Un seul générateur pour commencer";
            }
            entry.getValue().show(state, "", mark, trial.label(), trial.rule(), "", price, "Skin « " + trial.skin().label() + " »");
        }
        for (SkinVisual visual : SkinVisual.values()) {
            boolean chosen = game.chosenSkin(visual) == null;
            classics.get(visual).show(chosen ? Card.State.ON : Card.State.READY, "", chosen ? "choisi" : "", "Classique",
                    "L'apparence d'origine", "", chosen ? "" : "Choisir", "");
        }
        for (Map.Entry<Skin, Card> entry : cards.entrySet()) {
            Skin skin = entry.getKey();
            boolean has = game.hasSkin(skin);
            boolean chosen = game.chosenSkin(skin.visual()) == skin;
            entry.getValue().show(chosen ? Card.State.ON : has ? Card.State.READY : Card.State.LOCKED, "",
                    chosen ? "choisi" : has ? "débloqué" : "", skin.label(),
                    has ? "+" + ElementText.percent(Skin.BONUS) + " à toutes les ressources" : skin.source(), "",
                    chosen ? "" : has ? "Choisir" : "Verrouillé", "");
        }
    }

    /** La carte d'un skin : pour les vérifications. */
    public Card card(Skin skin) {
        return cards.get(skin);
    }

    /** La carte « Classique » d'un visuel : pour les vérifications. */
    public Card classic(SkinVisual visual) {
        return classics.get(visual);
    }

    /** La carte d'une épreuve : pour les vérifications. */
    public Card trial(Trial trial) {
        return trials.get(trial);
    }

    /** Les cartes des deux recherches : pour les vérifications. */
    public Card prismCard() {
        return prism;
    }

    public Card denseCard() {
        return dense;
    }

    /** Le résumé du haut : pour les vérifications. */
    public String summaryText() {
        return summary.getText();
    }
}
