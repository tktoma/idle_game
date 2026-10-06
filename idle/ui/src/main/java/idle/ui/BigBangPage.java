package idle.ui;

import idle.core.BigNum;
import idle.core.Game;
import idle.core.Molecule;
import idle.core.PeriodicTable;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * L'onglet « Big Bang » : l'acte qui s'ouvre au premier Big Bang ({@link Game#bigBang()}).
 *
 * <p>En haut, le nombre de Big Bangs déclenchés. En dessous, deux sous-pages :
 * <ul>
 *   <li>« Molécules » : le catalogue ({@link Game#molecules()}), un rayon à la fois
 *       ({@link Molecule.Kind}). Seuls les rayons ouverts sont proposés : les petites molécules
 *       d'abord, puis un rayon de plus à chaque palier d'espace créé ; une ligne annonce le prochain. Chaque molécule est une {@link Card} qui porte son dessin
 *       ({@link MoleculeArt}), sa formule, l'espace qu'elle occupe, combien ont été créées et ce
 *       que demande la suivante ; un clic la crée, avec des exemplaires du tableau périodique ;</li>
 *   <li>« Expansion de la matière » : l'espace créé, ce qu'il gagne par seconde, ce que les
 *       molécules en occupent, et une vue de cet espace ({@link SpaceView}) où l'on peut
 *       s'approcher des molécules ou reculer jusqu'à tout voir.</li>
 * </ul>
 * Les cartes d'un rayon ne sont construites que la première fois qu'on l'ouvre : le catalogue
 * compte plusieurs centaines de molécules.
 */
final class BigBangPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    private static final String CHIP_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 12; -fx-border-radius: 12;";
    private static final String ZOOM_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 4; -fx-border-radius: 4; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";"
            + " -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;";
    /** Taille du dessin d'une molécule sur sa carte. */
    private static final double PICTURE_WIDTH = 150;
    private static final double PICTURE_HEIGHT = 80;

    private final Game game;
    private final Label countLabel = new Label();
    private final Label sinceLabel = new Label();
    private final Button moleculesTab = new Button("Molécules");
    private final Button expansionTab = new Button("Expansion de la matière");
    private int selected = 0;

    // Sous-page « Molécules »
    private final VBox moleculesPane = new VBox(10);
    private final ScrollPane moleculesScroll = new ScrollPane(moleculesPane);
    private final Label moleculesIntro = new Label();
    private final Label nextKindLabel = new Label();
    private final Map<Molecule.Kind, Button> kindChips = new EnumMap<>(Molecule.Kind.class);
    private final Map<Molecule.Kind, TileGrid> grids = new EnumMap<>(Molecule.Kind.class);
    private final Map<Molecule, Card> cards = new LinkedHashMap<>();
    private final StackPane gridHolder = new StackPane();
    private Molecule.Kind kind = Molecule.Kind.SIMPLE;

    // Sous-page « Expansion de la matière »
    private final VBox expansionPane = new VBox(6);
    private final Label spaceLabel = new Label();
    private final Label rateLabel = new Label();
    private final Label expansionNote = new Label();
    private final SpaceView spaceView;
    private final Button zoomOut = new Button("Reculer");
    private final Button zoomIn = new Button("Approcher");
    private final Button zoomAll = new Button("Tout voir");

    BigBangPage(Game game) {
        super(8);
        this.game = game;
        this.spaceView = new SpaceView(game);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        countLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        sinceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        sinceLabel.setWrapText(true);
        sinceLabel.setTextAlignment(TextAlignment.CENTER);

        moleculesTab.setOnAction(event -> select(0));
        expansionTab.setOnAction(event -> select(1));
        moleculesTab.setFocusTraversable(false);
        expansionTab.setFocusTraversable(false);
        HBox subTabs = new HBox(moleculesTab, expansionTab);
        subTabs.setAlignment(Pos.CENTER);

        // Les molécules : une phrase, les rayons du catalogue, puis les cartes du rayon choisi.
        moleculesPane.setAlignment(Pos.TOP_CENTER);
        moleculesPane.setPadding(new Insets(10, 0, 10, 0));
        note(moleculesIntro);
        FlowPane chips = new FlowPane(6, 6);
        chips.setAlignment(Pos.CENTER);
        for (Molecule.Kind each : Molecule.Kind.values()) {
            Button chip = new Button();
            chip.setFocusTraversable(false);
            chip.setOnAction(event -> selectKind(each));
            kindChips.put(each, chip);
            chips.getChildren().add(chip);
        }
        chips.setMaxWidth(820);
        nextKindLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        nextKindLabel.setWrapText(true);
        nextKindLabel.setTextAlignment(TextAlignment.CENTER);
        nextKindLabel.setMaxWidth(760);
        gridHolder.setMaxWidth(900);
        moleculesPane.getChildren().add(moleculesIntro);
        moleculesPane.getChildren().add(chips);
        moleculesPane.getChildren().add(nextKindLabel);
        moleculesPane.getChildren().add(gridHolder);
        moleculesScroll.setFitToWidth(true);
        moleculesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        moleculesScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // L'expansion : les nombres, la vue de l'espace, et de quoi s'en approcher ou s'en éloigner.
        expansionPane.setAlignment(Pos.TOP_CENTER);
        expansionPane.setPadding(new Insets(10, 0, 0, 0));
        spaceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fffaf0;");
        rateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        rateLabel.setWrapText(true);
        rateLabel.setTextAlignment(TextAlignment.CENTER);
        note(expansionNote);
        zoomOut.setOnAction(event -> spaceView.zoom(1 / SpaceView.ZOOM_STEP));
        zoomIn.setOnAction(event -> spaceView.zoom(SpaceView.ZOOM_STEP));
        zoomAll.setOnAction(event -> spaceView.fit());
        for (Button button : new Button[] {zoomOut, zoomIn, zoomAll}) {
            button.setStyle(ZOOM_STYLE);
            button.setFocusTraversable(false);
        }
        HBox zoomRow = new HBox(6, zoomOut, zoomIn, zoomAll);
        zoomRow.setAlignment(Pos.CENTER);
        // La vue s'étire avec la fenêtre ; les textes gardent leur hauteur.
        CanvasPane viewPane = CanvasPane.filling(spaceView);
        VBox.setVgrow(viewPane, Priority.ALWAYS);
        spaceView.setStyle("-fx-cursor: open-hand;");
        expansionPane.getChildren().add(spaceLabel);
        expansionPane.getChildren().add(rateLabel);
        expansionPane.getChildren().add(expansionNote);
        expansionPane.getChildren().add(viewPane);
        expansionPane.getChildren().add(zoomRow);

        // Les sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        StackPane pages = new StackPane(moleculesScroll, expansionPane);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(countLabel);
        getChildren().add(sinceLabel);
        getChildren().add(subTabs);
        getChildren().add(pages);
        selectKind(kind);
        select(0);
    }

    /** Une phrase d'explication : discrète, centrée, qui passe à la ligne. */
    private static void note(Label label) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(760);
    }

    /** Affiche une sous-page : 0 = les molécules, 1 = l'expansion de la matière. */
    void select(int index) {
        selected = index;
        moleculesScroll.setVisible(index == 0);
        expansionPane.setVisible(index == 1);
        moleculesTab.setStyle(subTabStyle(index == 0));
        expansionTab.setStyle(subTabStyle(index == 1));
    }

    /** La sous-page affichée : 0 = les molécules, 1 = l'expansion de la matière. */
    int selected() {
        return selected;
    }

    private static String subTabStyle(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-border-color: transparent transparent "
                        + GameApp.BIG_BANG_COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Affiche un rayon du catalogue ; ses cartes sont construites à la première visite. */
    void selectKind(Molecule.Kind wanted) {
        kind = wanted;
        TileGrid grid = grids.computeIfAbsent(wanted, this::build);
        gridHolder.getChildren().clear();
        gridHolder.getChildren().add(grid);
        kindChips.forEach((each, chip) -> chip.setStyle(CHIP_STYLE + (each == wanted
                ? " -fx-text-fill: #10151f; -fx-background-color: " + GameApp.BIG_BANG_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: "
                        + GameApp.BIG_BANG_COLOR + "66;")));
    }

    /** Le rayon affiché : pour les vérifications. */
    Molecule.Kind kind() {
        return kind;
    }

    /** Les cartes d'un rayon, dans l'ordre du catalogue, chacune avec le dessin de sa molécule. */
    private TileGrid build(Molecule.Kind wanted) {
        TileGrid grid = new TileGrid(200, 4, 8);
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() != wanted) continue;
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setPicture(picture(molecule));
            card.setOnAction(() -> {
                game.createMolecule(molecule.id());
                refresh();
            });
            cards.put(molecule, card);
            grid.add(card);
        }
        return grid;
    }

    /** Le dessin d'une molécule, fait une fois pour toutes, centré dans la largeur de sa carte. */
    private static HBox picture(Molecule molecule) {
        Canvas canvas = new Canvas(PICTURE_WIDTH, PICTURE_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, PICTURE_WIDTH, PICTURE_HEIGHT);
        MoleculeArt.Shape shape = MoleculeArt.of(molecule);
        // Les petites molécules ne sont pas gonflées jusqu'au bord : leurs atomes gardent une taille raisonnable.
        double radius = Math.min(PICTURE_HEIGHT / 2 - 3, shape.extent() * 24);
        shape.draw(g, PICTURE_WIDTH / 2, PICTURE_HEIGHT / 2, radius, -Math.PI / 6);
        HBox box = new HBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);     // le clic va à la carte
        return box;
    }

    /** La carte d'une molécule, ou {@code null} si son rayon n'a pas encore été ouvert : pour les vérifications. */
    Card card(Molecule molecule) {
        return cards.get(molecule);
    }

    /** La vue de l'espace : pour les vérifications. */
    SpaceView spaceView() {
        return spaceView;
    }

    /** Le jeu recommence de zéro : retour à la première sous-page et au premier rayon. */
    void reset() {
        selectKind(Molecule.Kind.SIMPLE);
        spaceView.fit();
        select(0);
    }

    /**
     * Fait avancer l'animation de la vue de l'espace, quand elle est affichée.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        if (selected == 1) spaceView.frame(elapsed);
    }

    /** Recopie l'état du jeu dans les textes. */
    void refresh() {
        int count = game.bigBangs();
        countLabel.setText(count + (count > 1 ? " Big Bangs" : " Big Bang"));
        sinceLabel.setText("Partie en cours depuis " + Format.duration(game.stats().runTime())
                + ". Le prochain se déclenche au bout de l'arbre de matière noire.");
        int created = game.moleculesCreated();
        moleculesTab.setText(created > 0 ? "Molécules (" + created + ")" : "Molécules");
        if (selected == 0) refreshMolecules();
        else refreshExpansion();
    }

    private void refreshMolecules() {
        moleculesIntro.setText("Une molécule se crée avec des exemplaires du tableau périodique et de l'espace libre ("
                + Format.count(game.freeSpace()) + " en ce moment), et reste pour toujours."
                + Detail.only(" Les exemplaires quittent le tableau avec ce qu'ils y apportaient ; un exemplaire de "
                        + "chaque élément y reste, pour que le tableau puisse toujours exploser. Chaque molécule créée "
                        + "double ce que demande la suivante de la même sorte : sa formule une fois, deux fois, quatre "
                        + "fois. Elle occupe " + ElementText.number(Game.SPACE_PER_PROTON) + " unités d'espace par "
                        + "proton, que l'expansion de la matière doit avoir créées : plus elle est complexe ou faite "
                        + "d'éléments lourds, plus elle en prend. Ni l'explosion ni le Big Bang ne reprennent une "
                        + "molécule. Seules les petites molécules donnent quelque chose pour l'instant : un léger "
                        + "bonus par molécule créée, ou un exemplaire de plus au maximum d'un de leurs éléments dans "
                        + "le tableau périodique."));
        // Les rayons : combien de leurs molécules ont été créées au moins une fois.
        Map<Molecule.Kind, int[]> totals = new EnumMap<>(Molecule.Kind.class);
        for (Molecule molecule : game.molecules()) {
            int[] total = totals.computeIfAbsent(molecule.kind(), each -> new int[2]);
            total[1]++;
            if (game.moleculeCount(molecule.id()) > 0) total[0]++;
        }
        // Un rayon fermé n'est pas proposé du tout ; celui qu'on regardait a pu se refermer avec une remise à zéro.
        if (!game.isMoleculeKindUnlocked(kind)) selectKind(Molecule.Kind.SIMPLE);
        kindChips.forEach((each, chip) -> {
            int[] total = totals.getOrDefault(each, new int[2]);
            chip.setText(each.label() + "  " + total[0] + "/" + total[1]);
            boolean open = game.isMoleculeKindUnlocked(each);
            chip.setVisible(open);
            chip.setManaged(open);
        });
        nextKindLabel.setText(nextKind());
        nextKindLabel.setVisible(!nextKindLabel.getText().isEmpty());
        nextKindLabel.setManaged(!nextKindLabel.getText().isEmpty());
        cards.forEach((molecule, card) -> {
            if (molecule.kind() != kind) return;      // seul le rayon affiché se met à jour
            int count = game.moleculeCount(molecule.id());
            boolean reachable = game.isMoleculeWithinReach(molecule.id());
            boolean elements = game.hasElementsForMolecule(molecule.id());
            boolean ready = game.canCreateMolecule(molecule.id());
            Map<Integer, Integer> cost = game.nextMoleculeCost(molecule.id());
            BigNum volume = game.moleculeVolume(molecule.id());
            String detail = molecule.atoms() + (molecule.atoms() > 1 ? " atomes, " : " atome, ") + molecule.protons()
                    + " protons. Formule : " + recipe(molecule.recipe(), false) + "."
                    + (count > 0 ? " La prochaine demande " + (1L << Math.min(20, count)) + " fois la formule." : "")
                    + given(molecule, count);
            String price = ready ? "Créer : " + recipe(cost, false)
                    : !reachable ? "Hors de portée : " + recipe(cost, false)
                    : !elements ? "Il faut " + recipe(cost, true)
                    : "Il manque " + Format.count(volume.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
            card.show(ready ? Card.State.READY : !reachable ? Card.State.LOCKED
                            : count > 0 ? Card.State.STARTED : Card.State.WAITING,
                    count > 0 ? "×" + count : "", molecule.formula(), molecule.name(),
                    (molecule.hasBonus() ? bonus(molecule) + "\n" : "") + "Occupe " + Format.count(volume) + " d'espace",
                    detail, price, "");
        });
    }

    /** « Prochain rayon : Sels, à 10 000 d'espace créé (dans 2 h 10). » Vide quand tous les rayons sont ouverts. */
    private String nextKind() {
        Molecule.Kind next = game.nextMoleculeKind();
        if (next == null) return "";
        double wait = game.secondsUntilSpace(next.space());
        return "Prochain rayon : " + next.label() + ", à " + Format.count(BigNum.of(next.space())) + " d'espace créé"
                + (Double.isInfinite(wait) ? "" : " (dans " + Format.wait(wait) + ")") + ".";
    }

    private void refreshExpansion() {
        BigNum space = game.state().space();
        spaceLabel.setText(Format.count(space) + (space.gt(BigNum.ONE) ? " unités d'espace" : " unité d'espace"));
        rateLabel.setText("+" + Format.amount(game.spacePerSecond()) + " par seconde  ·  "
                + Format.count(game.occupiedSpace()) + " occupées par " + game.moleculesCreated()
                + (game.moleculesCreated() > 1 ? " molécules" : " molécule") + "  ·  " + Format.count(game.freeSpace()) + " libres");
        String next = nextKind();
        expansionNote.setText("L'espace grandit avec le temps de jeu, et les molécules s'y logent."
                + (next.isEmpty() ? " Tous les rayons de molécules sont ouverts." : " " + next)
                + Detail.only(" Chaque Big Bang ajoute " + ElementText.number(Game.SPACE_PER_SECOND)
                        + " unité par seconde, et les petites molécules qui agissent sur l'espace multiplient le "
                        + "tout (par " + ElementText.number(game.moleculeBoost(Molecule.Stat.SPACE))
                        + " en ce moment). L'espace grandit quel que soit l'onglet affiché, sauf en pause ; ni "
                        + "l'explosion ni le Big Bang ne le reprennent, et une molécule garde sa place pour toujours. "
                        + "Chaque palier d'espace créé ouvre un rayon de molécules : c'est l'espace créé qui compte, "
                        + "pas l'espace libre. Dans la vue, le cercle clair est le bord de l'espace créé, le cercle "
                        + "sombre le cœur occupé, les cercles en pointillés les paliers. "
                        + "La molette ou les boutons rapprochent et éloignent : on ne recule pas plus loin que l'espace "
                        + "créé. Tirer avec la souris déplace la vue."));
        zoomOut.setDisable(spaceView.isFitted());
        zoomAll.setDisable(spaceView.isFitted());
        zoomIn.setDisable(spaceView.isClosest());
    }

    /** Ce que donne chaque molécule créée, en quelques mots : « Espace +5 % par molécule ». Vide si elle ne donne rien. */
    static String bonus(Molecule molecule) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> what(boost.stat()) + " +" + ElementText.percent(boost.perMolecule()) + " par molécule";
            case Molecule.Uncap uncap -> PeriodicTable.element(uncap.element()).name() + " : +1 exemplaire au plus par molécule";
            case null -> "";
        };
    }

    /** Ce que donnent en tout les molécules déjà créées d'une sorte, pour le mode détails. Vide si elles ne donnent rien. */
    private String given(Molecule molecule, int count) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> count == 0 ? "" : " Déjà +" + ElementText.percent(boost.perMolecule() * count)
                    + " avec celles-ci ; toutes les molécules réunies multiplient " + reach(boost.stat()) + " par "
                    + ElementText.number(game.moleculeBoost(boost.stat())) + ".";
            case Molecule.Uncap uncap -> " " + PeriodicTable.element(uncap.element()).name() + " : "
                    + game.maxCopiesOf(PeriodicTable.element(uncap.element())) + " exemplaires au plus en ce moment.";
            case null -> "";
        };
    }

    private static String what(Molecule.Stat stat) {
        return switch (stat) {
            case SPACE -> "Espace par seconde";
            case PARTICLES -> "Particules";
            case ATOMS -> "Atomes par fusion";
            case DARK_GROWTH -> "Croissance de la matière noire";
        };
    }

    /** Le milieu de « toutes les molécules réunies multiplient … par N ». */
    private static String reach(Molecule.Stat stat) {
        return switch (stat) {
            case SPACE -> "l'espace gagné";
            case PARTICLES -> "les particules";
            case ATOMS -> "les atomes";
            case DARK_GROWTH -> "la croissance";
        };
    }

    /**
     * Une liste d'éléments et de nombres : « 2 H, 1 O ». Avec {@code owned}, ce que le joueur peut
     * y mettre est ajouté entre parenthèses : ses exemplaires, moins celui qui doit rester.
     */
    private String recipe(Map<Integer, Integer> atoms, boolean owned) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Integer, Integer> atom : atoms.entrySet()) {
            if (text.length() > 0) text.append(", ");
            text.append(atom.getValue()).append(' ').append(PeriodicTable.element(atom.getKey()).symbol());
            if (owned) text.append(" (").append(Math.max(0, game.elementCount(atom.getKey()) - 1)).append(')');
        }
        return text.toString();
    }
}
