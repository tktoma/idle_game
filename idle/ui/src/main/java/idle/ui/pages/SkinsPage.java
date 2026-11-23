package idle.ui.pages;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.core.universe.Trial;
import idle.ui.common.Card;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import idle.ui.common.SubTabs;
import javafx.geometry.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * L'onglet « Skins », sous les réglages : il s'ouvre dans le deuxième univers, après
 * cinq fusions. Une section par visuel du jeu, avec son apparence classique et ses skins : un skin
 * débloqué se choisit d'un clic, un skin verrouillé dit comment l'obtenir. Chaque skin débloqué
 * ajoute {@link Skin#BONUS} à toutes les ressources d'en dessous de l'univers. Ce bonus n'est plus
 * écrit sur chaque carte ; une carte en tête de page donne le nombre de skins débloqués, puis le bonus cumulé.
 *
 * <p>Version 0.249 : les visuels sont rangés par catégorie, un sous-onglet chacune ({@link SkinShelf}), après
 * « Collection » (la carte de la collection, les recherches, le record et les épreuves). Un skin ne se montre qu'une
 * fois son palier atteint (son univers, et pour une dimension, la dimension ouverte) ; un visuel sans skin montré, et
 * une catégorie sans visuel montré, restent cachés.
 */
public final class SkinsPage extends VBox {
    /** La couleur de l'onglet : un rose de palette. */
    public static final String COLOR = "#ff9fd6";

    private final Game game;
    private final Label summary = new Label();
    /** Version 0.209 : la carte de la collection, en tête : skins débloqués, puis le bonus cumulé. */
    private final Card collection = new Card(COLOR);
    private final Map<SkinVisual, Card> classics = new EnumMap<>(SkinVisual.class);
    private final Map<Skin, Card> cards = new LinkedHashMap<>();
    private final Label record = new Label();
    private final Card prism = new Card(COLOR);
    private final Card dense = new Card(COLOR);
    private final Map<Trial, Card> trials = new EnumMap<>(Trial.class);
    /** Les sous-onglets : « Collection », puis une catégorie chacun (version 0.249). */
    private final FlowPane subTabs = new FlowPane();
    private final Button collectionTab = new Button("Collection");
    private final Map<SkinShelf.Category, Button> categoryTabs = new EnumMap<>(SkinShelf.Category.class);
    private final VBox collectionPage = new VBox(10);
    private final Map<SkinShelf.Category, VBox> categoryPages = new EnumMap<>(SkinShelf.Category.class);
    /** La section de chaque visuel (son nom, où il se voit, ses cartes), et la grille de ses cartes. */
    private final Map<SkinVisual, VBox> sections = new EnumMap<>(SkinVisual.class);
    private final Map<SkinVisual, TileGrid> grids = new EnumMap<>(SkinVisual.class);
    /** La catégorie affichée ; {@code null} : la collection. */
    private SkinShelf.Category selected;

    public SkinsPage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));
        Label title = new Label("Skins");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        Labels.centered(summary, "-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        summary.setMaxWidth(760);
        TileGrid collectionGrid = new TileGrid(320, 1, 8);
        collectionGrid.setMaxWidth(340);
        collectionGrid.add(collection);
        collectionPage.setAlignment(Pos.TOP_CENTER);
        collectionPage.getChildren().addAll(collectionGrid, summary);
        subTabs.setAlignment(Pos.CENTER);
        subTabs.getChildren().add(collectionTab);
        collectionTab.setFocusTraversable(false);
        collectionTab.setOnAction(event -> select(null));
        for (SkinShelf.Category category : SkinShelf.Category.values()) {
            Button tab = new Button(category.label());
            tab.setFocusTraversable(false);
            tab.setOnAction(event -> select(category));
            categoryTabs.put(category, tab);
            subTabs.getChildren().add(tab);
            VBox page = new VBox(10);
            page.setAlignment(Pos.TOP_CENTER);
            categoryPages.put(category, page);
        }
        getChildren().addAll(title, subTabs, collectionPage);
        for (VBox page : categoryPages.values()) getChildren().add(page);
        // Les recherches, le record et les épreuves qui donnent les skins.
        record.setStyle("-fx-font-size: 12px; -fx-text-fill: #cfe3ff;");
        TileGrid research = new TileGrid(260, 2, 8);
        research.setMaxWidth(820);
        prism.setOnAction(() -> {
            game.buyPrismResearch();
            refresh();
        });
        dense.setOnAction(() -> {
            game.buyDenseTier();
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
        collectionPage.getChildren().addAll(heading("Recherches et record"), record, research, heading("Épreuves"), trialGrid);
        for (SkinVisual visual : SkinVisual.values()) {
            Label name = new Label(visual.label());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 0 0; -fx-text-fill: " + COLOR + ";");
            TileGrid grid = new TileGrid(200, 4, 8);
            grid.setMaxWidth(820);
            Card classic = new Card(COLOR);
            classic.setDoneClickable(true);
            classic.setOnAction(() -> {
                game.chooseSkin(visual, null);
                refresh();
            });
            classics.put(visual, classic);
            grid.add(classic);
            for (Skin skin : Skin.values()) {
                if (skin.visual() != visual) continue;
                Card card = new Card(COLOR);
                card.setOnAction(() -> {
                    game.chooseSkin(visual, skin);
                    refresh();
                });
                cards.put(skin, card);
                grid.add(card);
            }
            // Sous le nom du visuel, où il se voit et ce qu'il change (SkinPlaces).
            Label place = new Label(SkinPlaces.of(visual));
            place.setWrapText(true);
            place.setMaxWidth(820);
            place.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a9bb0;");
            VBox section = new VBox(4, name, place, grid);
            section.setAlignment(Pos.TOP_CENTER);
            section.setMaxWidth(820);
            sections.put(visual, section);
            grids.put(visual, grid);
        }
        for (SkinShelf.Category category : SkinShelf.Category.values()) {
            for (SkinVisual visual : category.visuals()) categoryPages.get(category).getChildren().add(sections.get(visual));
        }
        select(null);
    }

    /** Affiche une catégorie ({@code null} : la collection). */
    public void select(SkinShelf.Category category) {
        selected = category;
        refresh();
    }

    /** La catégorie affichée ; {@code null} : la collection. */
    public SkinShelf.Category selected() { return selected; }

    /** La barre des sous-onglets : la fenêtre la range en haut ou dans la page. */
    public FlowPane subTabs() { return subTabs; }

    /** Le mode détails d'une épreuve (version 0.291) : comment elle se joue, s'arrête et se rejoue. */
    private static String trialDetail(Trial trial) {
        return "Une seule épreuve à la fois : un clic la commence, un clic sur la carte en cours l'abandonne sans rien coûter. "
                + (trial.seconds() > 0 ? "Le temps écoulé sans atteindre le but, elle échoue et se retente. "
                        : "Elle dure jusqu'à son but, ou jusqu'à l'abandon. ")
                + "Réussie, elle débloque le skin « " + trial.skin().label() + " » et peut se rejouer.";
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

    public void refresh() {
        int unlocked = game.skinsUnlocked();
        int counted = game.skinsCounted();
        collection.show(counted > 0 ? Card.State.DONE : Card.State.WAITING, "", "", "Collection",
                unlocked + " / " + Skin.values().length + " skins débloqués",
                counted + (counted > 1 ? " skins comptés" : " skin compté") + " × " + ElementText.percent(Skin.BONUS)
                        + ", sur toutes les ressources d'en dessous de l'univers",
                "Bonus cumulé : ×" + ElementText.number(game.skinBoost()) + " (+" + ElementText.percent(game.skinBoost() - 1) + ")", "");
        summary.setText("Choisir un skin ne change que l'apparence ; chaque skin débloqué, choisi ou non, ajoute "
                + ElementText.percent(Skin.BONUS) + " au bonus, sans plafond (les skins libres, ouverts d'office, n'en donnent pas). "
                // La collection s'étale sur la partie.
                + "Les skins viennent au fil des univers, une trentaine par univers formé jusqu'au " + (Skin.LAST_SKIN_UNIVERSE + 1)
                + "ᵉ : un skin ne se montre qu'une fois son univers atteint (et sa dimension ouverte), puis attend sa condition. "
                + "Les sous-onglets rangent les skins par partie du jeu.");
        double best = game.rebuildRecord();
        record.setText("Record de reconstruction (de la fusion à " + game.generatorsPerAtom() + " générateurs) : "
                + (best > 0 ? Format.duration(best) : "aucun encore") + " — en " + (int) Game.REBUILD_FAST_SECONDS
                + " s au plus, il donne le skin « " + Skin.FUSION_FLASH.label() + " ».");
        boolean prismDone = game.hasPrismResearch();
        prism.show(prismDone ? Card.State.DONE : game.canBuyPrismResearch() ? Card.State.READY : Card.State.WAITING, "",
                prismDone ? "faite" : "", "Fusion prismatique", "Une recherche facultative : le skin « Prisme » de la fusion",
                "Un seul achat, en particules, qui ne change rien au jeu : il débloque le skin « Prisme » de la fusion des particules.",
                prismDone ? "" : Format.big(Game.PRISM_COST) + " particules", "");
        int tiers = game.denseTiers();
        boolean denseOpen = game.isDenseResearchUnlocked();
        dense.show(tiers >= Game.DENSE_COSTS.length ? Card.State.DONE : !denseOpen ? Card.State.LOCKED
                        : game.canBuyDenseTier() ? Card.State.READY : tiers > 0 ? Card.State.STARTED : Card.State.WAITING, "",
                tiers + " / " + Game.DENSE_COSTS.length, "Noyau dense",
                denseOpen ? "Quatre paliers : deux skins d'atome, deux du tableau"
                        : "S'ouvre à " + Format.big(Game.DENSE_UNLOCK_ATOMS) + " atomes créés",
                "Chaque palier se paie en atomes et débloque un skin, dans l'ordre : « " + Skin.ATOM_DENSE.label() + " » (l'atome), « "
                        + Skin.TABLE_LEAD.label() + " » (le tableau), « " + Skin.ATOM_STELLAR.label() + " » (l'atome), « "
                        + Skin.TABLE_GOLD.label() + " » (le tableau). La recherche ne change rien au jeu.",
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
            entry.getValue().show(state, "", mark, trial.label(), trial.rule(), trialDetail(trial), price, "Skin « " + trial.skin().label() + " »");
        }
        // Les sous-onglets : une catégorie sans skin montré reste cachée ; celle qu'on regardait revient à la collection.
        if (selected != null && !SkinShelf.shown(game, selected)) selected = null;
        collectionTab.setStyle(SubTabs.style(SubTabs.NARROW_STYLE, selected == null, COLOR));
        collectionPage.setVisible(selected == null);
        collectionPage.setManaged(selected == null);
        for (SkinShelf.Category category : SkinShelf.Category.values()) {
            boolean open = SkinShelf.shown(game, category);
            Button tab = categoryTabs.get(category);
            tab.setVisible(open);
            tab.setManaged(open);
            int[] counts = SkinShelf.counts(game, category);
            tab.setText(category.label() + " (" + counts[0] + "/" + counts[1] + ")");
            tab.setStyle(SubTabs.style(SubTabs.NARROW_STYLE, category == selected, COLOR));
            VBox page = categoryPages.get(category);
            page.setVisible(category == selected);
            page.setManaged(category == selected);
        }
        for (SkinVisual visual : SkinVisual.values()) {
            boolean open = SkinShelf.shown(game, visual);
            sections.get(visual).setVisible(open);
            sections.get(visual).setManaged(open);
        }
        for (Map.Entry<Skin, Card> entry : cards.entrySet()) grids.get(entry.getKey().visual()).show(entry.getValue(), SkinShelf.shown(game, entry.getKey()));
        for (SkinVisual visual : SkinVisual.values()) {
            boolean chosen = game.chosenSkin(visual) == null;
            classics.get(visual).show(chosen ? Card.State.ON : Card.State.READY, "", chosen ? "choisi" : "", "Classique",
                    "L'apparence d'origine", "", chosen ? "" : "Choisir", "");
        }
        for (Map.Entry<Skin, Card> entry : cards.entrySet()) {
            Skin skin = entry.getKey();
            boolean has = game.hasSkin(skin);
            boolean chosen = game.chosenSkin(skin.visual()) == skin;
            // Un skin dont le palier n'est pas atteint reste caché (0.249) : une carte montrée attend seulement sa condition.
            entry.getValue().show(chosen ? Card.State.ON : has ? Card.State.READY : Card.State.LOCKED, "",
                    chosen ? "choisi" : has ? "débloqué" : "", skin.label(), skin.source(), "",
                    chosen ? "" : has ? "Choisir" : "Verrouillé", "");
        }
    }

    /** La carte de la collection : pour les vérifications. */
    public Card collectionCard() { return collection; }

    /** Le texte du résumé en tête de page : pour les vérifications. */
    public String summaryText() { return summary.getText(); }

    /** La carte d'un skin : pour les vérifications. */
    public Card card(Skin skin) { return cards.get(skin); }

    /** Vrai si la carte de ce skin est montrée dans sa grille : pour les vérifications. */
    public boolean cardShown(Skin skin) { return grids.get(skin.visual()).shownTiles().contains(cards.get(skin)); }

    /** Vrai si la section de ce visuel est montrée : pour les vérifications. */
    public boolean sectionShown(SkinVisual visual) { return sections.get(visual).isVisible(); }

    /** Le sous-onglet d'une catégorie : pour les vérifications. */
    public Button categoryTab(SkinShelf.Category category) { return categoryTabs.get(category); }

    /** La carte « Classique » d'un visuel : pour les vérifications. */
    public Card classic(SkinVisual visual) { return classics.get(visual); }

    /** La carte d'une épreuve : pour les vérifications. */
    public Card trial(Trial trial) { return trials.get(trial); }
}
