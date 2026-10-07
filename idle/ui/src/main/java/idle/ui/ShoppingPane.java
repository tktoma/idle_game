package idle.ui;

import idle.core.Assembly;
import idle.core.BigNum;
import idle.core.Game;
import idle.core.Molecule;
import idle.core.ShoppingList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * La liste de courses du troisième acte, au-dessus des assemblages, des astres et de l'univers :
 * ce qu'il manque pour le prochain pas ({@link Game#shoppingList()}), molécule par molécule, avec
 * sur chaque ligne le bouton qui la crée ou la rassemble. Le joueur n'a plus à passer de la carte
 * d'un astre à la page des molécules.
 *
 * <p>Elle se replie d'un clic sur son titre. Les règles ne sont pas ici : chaque bouton fait ce
 * que ferait la carte de la molécule, au même prix.
 */
final class ShoppingPane extends VBox {

    /** Au-delà, les lignes restantes sont résumées en une phrase : la liste ne doit pas pousser la page hors de l'écran. */
    static final int MAX_ROWS = 10;

    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 2 10; -fx-cursor: hand;"
            + " -fx-background-radius: 4; -fx-border-radius: 4; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";"
            + " -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;";
    private static final String TEXT_STYLE = "-fx-font-size: 12px; -fx-text-fill: #c6d2df;";
    private static final String DIM_STYLE = "-fx-font-size: 12px; -fx-text-fill: #8fa3b8;";

    /** Une ligne de la liste : une molécule, où elle en est, à quoi elle sert, et son bouton. */
    private static final class Row {
        final Label name = new Label();
        final Label count = new Label();
        final Label purpose = new Label();
        final Button action = new Button();
        final HBox box;
        String moleculeId = "";
        boolean gathers = false;

        Row() {
            name.setStyle(TEXT_STYLE);
            name.setMinWidth(190);
            count.setStyle(TEXT_STYLE + " -fx-font-weight: bold;");
            count.setMinWidth(110);
            purpose.setStyle(DIM_STYLE);
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            action.setStyle(BUTTON_STYLE);
            action.setFocusTraversable(false);
            action.setMinWidth(120);
            box = new HBox(8, name, count, purpose, gap, action);
            box.setAlignment(Pos.CENTER_LEFT);
        }
    }

    private final Game game;
    private final Runnable changed;
    private final Button title = new Button();
    private final Label note = new Label();
    private final VBox list = new VBox(3);
    private final Label more = new Label();
    private final Label matter = new Label();
    private final HBox ready = new HBox(6);
    private final List<Row> rows = new ArrayList<>();
    private boolean open = true;

    /**
     * @param changed appelé après chaque création, rassemblement ou formation, pour recopier la page
     */
    ShoppingPane(Game game, Runnable changed) {
        super(5);
        this.game = game;
        this.changed = changed;
        setAlignment(Pos.TOP_LEFT);
        setMaxWidth(820);
        setStyle("-fx-background-color: #0f1620; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #26333f;"
                + " -fx-padding: 8 12;");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand; -fx-background-color: transparent;"
                + " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        title.setFocusTraversable(false);
        title.setOnAction(event -> {
            open = !open;
            refresh();
        });
        note.setStyle(DIM_STYLE);
        note.setWrapText(true);
        more.setStyle(DIM_STYLE);
        matter.setStyle(TEXT_STYLE);
        matter.setWrapText(true);
        ready.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(title);
        getChildren().add(note);
        getChildren().add(list);
        getChildren().add(more);
        getChildren().add(matter);
        getChildren().add(ready);
    }

    /** Vrai si la liste est dépliée. */
    boolean isOpen() {
        return open;
    }

    /** Le titre, qui replie et déplie la liste : pour les vérifications. */
    Button titleButton() {
        return title;
    }

    /** Nombre de lignes de molécules affichées : pour les vérifications. */
    int shownRows() {
        int shown = 0;
        for (Row row : rows) {
            if (row.box.isVisible()) shown++;
        }
        return list.isVisible() ? shown : 0;
    }

    /** Le texte d'une ligne, « nom | compte | usage | bouton » : pour les vérifications. */
    String rowText(int index) {
        Row row = rows.get(index);
        return row.name.getText() + " | " + row.count.getText() + " | " + row.purpose.getText() + " | " + row.action.getText();
    }

    /** Clique le bouton d'une ligne : pour les vérifications. */
    void clickRow(int index) {
        rows.get(index).action.fire();
    }

    /** Tout le texte hors des lignes : titre, note, matière, suite. Pour les vérifications. */
    String summary() {
        return title.getText() + " | " + (note.isVisible() ? note.getText() : "") + " | " + (matter.isVisible() ? matter.getText() : "")
                + " | " + (more.isVisible() ? more.getText() : "");
    }

    /** Recopie la liste de courses. Cachée quand il n'y a rien à viser. */
    void refresh() {
        ShoppingList shopping = game.shoppingList();
        boolean any = shopping.kind() != ShoppingList.Kind.NONE;
        setVisible(any);
        setManaged(any);
        if (!any) return;
        title.setText((open ? "▾ " : "▸ ") + "Liste de courses : " + target(shopping));
        show(note, open);
        show(list, open);
        show(more, false);
        show(matter, false);
        show(ready, false);
        if (!open) return;

        double wait = game.secondsUntilShopping();
        note.setText(shopping.ready()
                ? "Tout est réuni : il n'y a plus qu'à " + (shopping.kind() == ShoppingList.Kind.ASSEMBLY ? "l'assembler." : "le former.")
                : shopping.items().isEmpty() && shopping.matter().isEmpty() && !shopping.assemblies().isEmpty()
                ? "Il ne reste qu'à former ce qui est prêt."
                : (wait <= 0 ? "L'espace libre suffit déjà à tout ce qui manque."
                        : Double.isInfinite(wait) ? "L'espace ne grandit pas : rien ne se libérera."
                        : "L'espace libre suffira à tout dans " + Goals.about(wait) + ".")
                        + Detail.only(" Chaque bouton fait ce que ferait la carte de la molécule, au même prix. L'estimation "
                                + "ne compte que l'espace : il faut aussi regarnir le tableau périodique."));

        List<ShoppingList.Item> items = shopping.items();
        int shown = Math.min(items.size(), MAX_ROWS);
        while (rows.size() < shown) addRow();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            boolean used = i < shown;
            show(row.box, used);
            if (used) fill(row, items.get(i));
        }
        if (items.size() > shown) {
            more.setText("… et " + (items.size() - shown) + (items.size() - shown > 1 ? " autres sortes" : " autre sorte")
                    + ", montrées quand celles-ci seront réunies.");
            show(more, true);
        }

        if (!shopping.matter().isEmpty()) {
            StringBuilder text = new StringBuilder("Matière rassemblée qui manque : ");
            boolean first = true;
            for (Map.Entry<Molecule.State, Integer> lacking : shopping.matter().entrySet()) {
                if (!first) text.append(", ");
                first = false;
                text.append(Format.whole(lacking.getValue())).append(' ').append(MatterText.plural(lacking.getKey()));
            }
            matter.setText(text + Detail.only(". N'importe quelle sorte rassemblée de cet état compte : le filtre « Utiles au "
                    + "prochain astre » de la page Molécules les montre."));
            show(matter, true);
        }

        if (!shopping.sorts().isEmpty()) {
            StringBuilder text = new StringBuilder(matter.isVisible() ? matter.getText() + "\n" : "");
            text.append("Sortes à rassembler en plus : ");
            boolean first = true;
            for (Map.Entry<Molecule.State, Integer> lacking : shopping.sorts().entrySet()) {
                if (!first) text.append(", ");
                first = false;
                text.append(lacking.getValue()).append(' ').append(MatterText.plural(lacking.getKey()));
            }
            matter.setText(text + Detail.only(". Une sorte compte dès qu'elle est rassemblée, même avec trois molécules."));
            show(matter, true);
        }

        ready.getChildren().clear();
        if (!shopping.assemblies().isEmpty()) {
            Label label = new Label("Prêts à former :");
            label.setStyle(TEXT_STYLE);
            ready.getChildren().add(label);
            for (Assembly assembly : shopping.assemblies()) {
                Button form = new Button(assembly.name());
                form.setStyle(BUTTON_STYLE);
                form.setFocusTraversable(false);
                form.setOnAction(event -> {
                    game.formAssembly(assembly.id());
                    changed.run();
                });
                ready.getChildren().add(form);
            }
            show(ready, true);
        }
    }

    /** « la comète glacée », « l'amas de galaxies », « Roche granitique » : ce que vise la liste. */
    private static String target(ShoppingList shopping) {
        return switch (shopping.kind()) {
            case BODY -> Goals.article(shopping.target());
            case ASSEMBLY -> "l'assemblage « " + shopping.target() + " »";
            case COSMOS, NONE -> shopping.target();
        };
    }

    private void addRow() {
        Row row = new Row();
        row.action.setOnAction(event -> {
            if (row.gathers) game.formSubstance(row.moleculeId);
            else game.createMolecule(row.moleculeId);
            Readiness.of(game).compute();
            changed.run();
        });
        rows.add(row);
        list.getChildren().add(row.box);
    }

    private void fill(Row row, ShoppingList.Item item) {
        Molecule molecule = item.molecule();
        String id = molecule.id();
        row.moleculeId = id;
        row.name.setText(molecule.name() + "  " + molecule.formula());
        row.count.setText(Format.whole(item.owned()) + " / " + Format.whole(item.needed()));
        // Il manque des molécules : on crée. Le compte y est : il ne reste qu'à rassembler.
        row.gathers = item.missing() == 0 && item.gather();
        row.purpose.setText((item.purpose().isEmpty() ? "" : "pour " + item.purpose())
                + (item.gather() && !row.gathers ? (item.purpose().isEmpty() ? "" : ", ") + "puis à rassembler" : ""));
        if (row.gathers) {
            boolean can = game.canFormSubstance(id);
            row.action.setText(can ? "Rassembler" : "Rassembler : " + lackingSpace(game.substanceSpace(id)));
            row.action.setDisable(!can);
        } else {
            boolean can = game.canCreateMolecule(id);
            int next = can ? game.moleculesNextCreation(id) : 1;
            row.action.setText(can ? (next > 1 ? "Créer ×" + next : "Créer")
                    : !game.isMoleculeKindUnlocked(molecule.kind()) ? "Rayon fermé"
                    : !game.hasElementsForMolecule(id) ? "Éléments à regarnir"
                    : "Créer : " + lackingSpace(game.moleculeVolume(id)));
            row.action.setDisable(!can);
        }
    }

    /** « il manque 120 d'espace ». */
    private String lackingSpace(BigNum needed) {
        return "il manque " + Format.count(needed.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
