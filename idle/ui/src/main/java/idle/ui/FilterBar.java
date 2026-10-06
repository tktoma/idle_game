package idle.ui;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * De quoi trier une grille de cartes : une barre de recherche, et des rangées de pastilles où l'on
 * choisit un type ({@link #group}). La barre ne cache rien elle-même : la page lui demande, pour
 * chaque carte, si son texte correspond ({@link #matches(String)}) et ce que disent les pastilles
 * ({@link Group#selected()}), et elle est prévenue à chaque changement.
 *
 * <p>La recherche ne regarde ni les majuscules, ni les accents, ni les indices des formules :
 * « h2o » trouve « H₂O », « oxygene » trouve « Eau oxygénée ». Plusieurs mots doivent tous s'y trouver.
 *
 * <p>Tant qu'on écrit dans la barre, les raccourcis clavier du jeu sont suspendus
 * ({@link GameApp}) : Entrée ou Échap rendent le clavier au jeu, Échap en effaçant la recherche.
 */
final class FilterBar extends VBox {

    private static final String FIELD_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-background-radius: 12;"
            + " -fx-border-radius: 12; -fx-background-color: #121923; -fx-text-fill: #e8ecf4;"
            + " -fx-prompt-text-fill: #7c8798; -fx-border-color: ";
    private static final String CHIP_STYLE = "-fx-font-size: 11px; -fx-padding: 2 9; -fx-cursor: hand;"
            + " -fx-background-radius: 10; -fx-border-radius: 10;";

    /** Une rangée de pastilles : « tout », ou un seul type à la fois. */
    final class Group<T> {
        private final Map<T, Button> chips = new LinkedHashMap<>();
        private final Button all;
        private T selected;

        private Group(String everything, Map<T, String> choices, FlowPane row) {
            all = chip(everything, row);
            all.setOnAction(event -> select(null));
            for (Map.Entry<T, String> choice : choices.entrySet()) {
                Button chip = chip(choice.getValue(), row);
                chip.setOnAction(event -> select(choice.getKey()));
                chips.put(choice.getKey(), chip);
            }
            paint();
        }

        /** Le type choisi, ou {@code null} pour tout montrer. */
        T selected() {
            return selected;
        }

        /** Choisit un type, ou tout avec {@code null} ; choisir celui qui l'est déjà revient à tout montrer. */
        void select(T choice) {
            selected = choice != null && choice.equals(selected) ? null : choice;
            paint();
            changed.run();
        }

        private void paint() {
            all.setStyle(chipStyle(selected == null));
            chips.forEach((choice, chip) -> chip.setStyle(chipStyle(choice.equals(selected))));
        }
    }

    private final TextField field = new TextField();
    private final String color;
    private final Runnable changed;
    private final Node keyboardHome;
    private final List<Group<?>> groups = new ArrayList<>();
    private String[] words = new String[0];

    /**
     * @param prompt       ce que dit la barre tant qu'elle est vide
     * @param color        la couleur de la page
     * @param keyboardHome le nœud qui reprend le clavier quand la recherche le rend
     * @param changed      appelé à chaque changement du texte ou d'une pastille
     */
    FilterBar(String prompt, String color, Node keyboardHome, Runnable changed) {
        super(6);
        this.color = color;
        this.changed = changed;
        this.keyboardHome = keyboardHome;
        setAlignment(Pos.TOP_CENTER);
        field.setPromptText(prompt);
        field.setStyle(FIELD_STYLE + color + "66;");
        field.setMaxWidth(320);
        // Le champ ne prend le clavier que si on clique dedans : jamais tout seul à l'ouverture de la page.
        field.setFocusTraversable(false);
        field.textProperty().addListener((observable, before, now) -> {
            String text = normalized(now);
            words = text.isEmpty() ? new String[0] : text.split(" +");
            changed.run();
        });
        field.setOnAction(event -> release());
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                field.setText("");
                release();
            }
        });
        getChildren().add(field);
    }

    /** Ajoute une rangée de pastilles : la première montre tout, les autres un type chacune. */
    <T> Group<T> group(String everything, Map<T, String> choices) {
        FlowPane row = new FlowPane(5, 5);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(820);
        Group<T> group = new Group<>(everything, choices, row);
        groups.add(group);
        getChildren().add(row);
        return group;
    }

    private Button chip(String text, FlowPane row) {
        Button chip = new Button(text);
        chip.setFocusTraversable(false);
        row.getChildren().add(chip);
        return chip;
    }

    private String chipStyle(boolean selected) {
        return CHIP_STYLE + (selected
                ? " -fx-text-fill: #10151f; -fx-background-color: " + color + "cc; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + color + "; -fx-background-color: transparent; -fx-border-color: " + color + "44;");
    }

    /** Vrai si le texte cherché se trouve dans {@code text}, déjà passé par {@link #normalized(String)}. Toujours vrai sans recherche. */
    boolean matches(String text) {
        for (String word : words) {
            if (!text.contains(word)) return false;
        }
        return true;
    }

    /** Vrai si quelque chose est filtré : un texte cherché, ou une pastille autre que « tout ». */
    boolean active() {
        if (words.length > 0) return true;
        for (Group<?> group : groups) {
            if (group.selected() != null) return true;
        }
        return false;
    }

    /** Le texte cherché, tel qu'il est écrit. */
    String text() {
        return field.getText();
    }

    /** Écrit dans la barre, comme le ferait le joueur. */
    void search(String text) {
        field.setText(text);
    }

    /** Vrai tant que le joueur écrit dans la barre. */
    boolean typing() {
        return field.isFocused();
    }

    /** Rend le clavier au jeu, si la barre l'avait. */
    void release() {
        if (field.isFocused()) keyboardHome.requestFocus();
    }

    /**
     * Un texte tel que la recherche le compare : en minuscules, sans accents, les indices et
     * exposants des formules ramenés à des chiffres, les espaces en trop retirés.
     */
    static String normalized(String text) {
        if (text == null) return "";
        String plain = Normalizer.normalize(text, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        StringBuilder kept = new StringBuilder(plain.length());
        for (int index = 0; index < plain.length(); index++) {
            char letter = plain.charAt(index);
            // Les accents, détachés de leur lettre par la décomposition, s'en vont.
            if (Character.getType(letter) == Character.NON_SPACING_MARK) continue;
            kept.append(Character.isWhitespace(letter) ? ' ' : letter);
        }
        return kept.toString().trim().replaceAll(" +", " ");
    }
}
