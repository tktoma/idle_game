package idle.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * L'ordre d'achat d'un automatisme de matière noire, réglable par le joueur : un bouton choisit
 * entre « le moins cher d'abord » et « mon ordre », et dans le second cas une liste se range par
 * deux flèches, la première ligne étant servie la première.
 *
 * <p>Dans l'ordre du joueur, l'automatisme n'achète que la première chose de la liste qui n'est
 * pas finie, et l'attend s'il le faut : c'est ce qui permet de dire « d'abord ceci ». Les règles
 * sont dans {@code Game} ; ce panneau ne fait que les montrer et les régler.
 */
final class OrderPane extends VBox {

    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + "; -fx-background-color: #1c1630;"
            + " -fx-border-color: " + GameApp.DARK_MATTER_COLOR + ";";
    private static final String ARROW_STYLE = "-fx-font-size: 11px; -fx-padding: 1 8; -fx-cursor: hand; -fx-background-radius: 5;"
            + " -fx-border-radius: 5; -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + "; -fx-background-color: #1c1630;"
            + " -fx-border-color: #4a3d6b;";

    /**
     * Une ligne de la liste.
     *
     * @param id   ce qu'elle range
     * @param name son nom
     * @param done vrai si l'automatisme n'a plus rien à y acheter : la ligne le dit, et il passe à la suivante
     */
    record Entry(String id, String name, boolean done) {
    }

    private final String cheapest;
    private final BooleanSupplier ordered;
    private final Consumer<Boolean> setOrdered;
    private final Supplier<List<Entry>> entries;
    private final BiPredicate<String, Integer> move;
    private final Runnable changed;
    private final Label title = new Label();
    private final Button mode = new Button();
    private final Label note = new Label();
    private final VBox list = new VBox(3);
    private final List<Label> names = new ArrayList<>();
    private final List<Button> ups = new ArrayList<>();
    private final List<Button> downs = new ArrayList<>();
    private List<Entry> shown = List.of();

    /**
     * @param name       le nom de l'automatisme dont c'est l'ordre
     * @param cheapest   « la moins chère d'abord » ou « le moins cher d'abord », accordé à ce qui est acheté
     * @param ordered    vrai si l'ordre du joueur est suivi
     * @param setOrdered choisit entre l'ordre du joueur et le moins cher d'abord
     * @param entries    la liste, dans l'ordre du joueur
     * @param move       déplace une ligne d'un rang : −1 la fait monter, +1 la fait descendre
     * @param changed    appelé après chaque réglage, pour recopier la page
     */
    OrderPane(String name, String cheapest, BooleanSupplier ordered, Consumer<Boolean> setOrdered,
              Supplier<List<Entry>> entries, BiPredicate<String, Integer> move, Runnable changed) {
        super(6);
        this.cheapest = cheapest;
        this.ordered = ordered;
        this.setOrdered = setOrdered;
        this.entries = entries;
        this.move = move;
        this.changed = changed;
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(778);
        setStyle("-fx-background-color: #151226; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #2f2748;"
                + " -fx-padding: 10 14;");
        title.setText("Ordre d'achat de « " + name + " »");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + ";");
        mode.setStyle(BUTTON_STYLE);
        mode.setFocusTraversable(false);
        mode.setOnAction(event -> {
            setOrdered.accept(!ordered.getAsBoolean());
            changed.run();
        });
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(8, title, gap, mode);
        head.setAlignment(Pos.CENTER_LEFT);
        note.setStyle("-fx-font-size: 12px; -fx-text-fill: #9d93b8;");
        note.setWrapText(true);
        note.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(head);
        getChildren().add(note);
        getChildren().add(list);
    }

    /** Le bouton qui choisit entre les deux ordres : pour les vérifications. */
    Button modeButton() {
        return mode;
    }

    /** Les noms de la liste, de la première ligne à la dernière : pour les vérifications. */
    List<String> names() {
        List<String> all = new ArrayList<>();
        for (Entry entry : shown) all.add(entry.name());
        return all;
    }

    /** Clique la flèche d'une ligne : pour les vérifications. */
    void clickArrow(int row, boolean up) {
        (up ? ups : downs).get(row).fire();
    }

    /** Vrai si la liste est affichée : l'ordre du joueur est suivi. */
    boolean listShown() {
        return list.isVisible();
    }

    /** Recopie le réglage et la liste. */
    void refresh() {
        boolean mine = ordered.getAsBoolean();
        mode.setText(mine ? "Mon ordre" : capitalized(cheapest));
        note.setText(mine
                ? "La première ligne qui n'est pas finie est la seule achetée : l'automatisme l'attend s'il le faut."
                        + Detail.only(" Ce qui dépasse le plafond d'atomes, ou n'est pas encore débloqué, est sauté. Un clic "
                                + "sur le bouton revient à « " + cheapest + " ».")
                : "Il achète ce qui coûte le moins parmi ce que vous pouvez payer."
                        + Detail.only(" Un clic sur le bouton vous laisse ranger la liste : il n'achètera plus que la "
                                + "première ligne qui n'est pas finie."));
        list.setVisible(mine);
        list.setManaged(mine);
        if (!mine) return;
        shown = entries.get();
        while (names.size() < shown.size()) addRow(names.size());
        for (int i = 0; i < names.size(); i++) {
            boolean used = i < shown.size();
            list.getChildren().get(i).setVisible(used);
            list.getChildren().get(i).setManaged(used);
            if (!used) continue;
            Entry entry = shown.get(i);
            names.get(i).setText((i + 1) + ". " + entry.name() + (entry.done() ? "  (fini)" : ""));
            names.get(i).setOpacity(entry.done() ? 0.5 : 1);
            ups.get(i).setDisable(i == 0);
            downs.get(i).setDisable(i == shown.size() - 1);
        }
    }

    private void addRow(int row) {
        Label name = new Label();
        name.setStyle("-fx-font-size: 13px; -fx-text-fill: #d9d0f0;");
        Button up = arrow("▲", row, -1);
        Button down = arrow("▼", row, 1);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox line = new HBox(6, name, gap, up, down);
        line.setAlignment(Pos.CENTER_LEFT);
        names.add(name);
        ups.add(up);
        downs.add(down);
        list.getChildren().add(line);
    }

    private Button arrow(String sign, int row, int direction) {
        Button button = new Button(sign);
        button.setStyle(ARROW_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> {
            if (row < shown.size() && move.test(shown.get(row).id(), direction)) changed.run();
        });
        return button;
    }

    private static String capitalized(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
