package idle.ui.common;

import idle.core.Game;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * La barre d'un plan (version 0.84) : sauvegarder ce qui est en place, le recharger, le copier en
 * texte ou coller un texte, et oublier le plan suivi. Elle sert à l'arbre de matière noire (ses
 * voies et ses liaisons) et aux systèmes solaires (leur disposition).
 *
 * <p>Un plan chargé se suit tout seul : ce qu'il demande se prend dès que c'est possible, joueur
 * présent ou absent, jusqu'à ce qu'il soit oublié.
 */
public final class PlanBar extends VBox {

    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-background-color: #141a26;";

    /** Ce que la barre fait faire au jeu. */
    public interface Plan {
        /** Le code de ce qui est en place. */
        String code();

        /** Fait d'un code le plan suivi. */
        Game.PlanLoad load(String code);

        /** Garde ce qui est en place dans cet emplacement. */
        void save(int slot);

        /** Recharge ce qui a été gardé dans cet emplacement. */
        Game.PlanLoad loadSaved(int slot);

        /** Le code gardé dans cet emplacement ; vide s'il n'y en a pas. */
        String saved(int slot);

        /** Ce que contient un code, en quelques mots : « 4 voies, 1 liaison ». */
        String describe(String code);

        /** Oublie le plan suivi. */
        void forget();

        /** Vrai si un plan est suivi. */
        boolean active();

        /** Où en est le plan suivi, en quelques mots. */
        String progress();
    }

    private final Plan plan;
    private final String what;
    private final Label status = new Label();
    private final Label note = new Label();
    private final Button save = button("Sauvegarder");
    private final Button load = button("Charger");
    private final Button copy = button("Copier le code");
    private final Button paste = button("Coller un code");
    private final Button forget = button("Oublier le plan");
    private final FlowPane buttons;
    /** Les emplacements de sauvegarde (version 0.86) : une liste, chacun avec ce qu'il contient. */
    private final javafx.scene.control.ComboBox<String> slots = new javafx.scene.control.ComboBox<>();
    private int slot = 0;
    private boolean filling = false;
    private Supplier<String> clipboardRead = PlanBar::readClipboard;
    private Consumer<String> clipboardWrite = PlanBar::writeClipboard;

    /**
     * @param what  ce que le plan range, au pluriel : « les voies », « la disposition »
     * @param color la couleur de l'onglet
     */
    public PlanBar(Plan plan, String what, String color) {
        super(4);
        this.plan = plan;
        this.what = what;
        setAlignment(Pos.CENTER);
        for (Button button : new Button[] {save, load, copy, paste, forget}) {
            button.setStyle(BUTTON_STYLE + " -fx-text-fill: " + color + "; -fx-border-color: " + color + "88;");
        }
        slots.setFocusTraversable(false);
        slots.setVisibleRowCount(idle.core.GameState.PLAN_SLOTS);
        slots.setStyle("-fx-font-size: 12px;");
        slots.setOnAction(event -> {
            if (filling) return;
            int chosen = slots.getItems().indexOf(slots.getValue());
            if (chosen >= 0) slot = chosen;
            refresh();
        });
        buttons = new FlowPane(8, 6, slots, save, load, copy, paste, forget);
        buttons.setAlignment(Pos.CENTER);
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #8fa3b8;");
        note.setWrapText(true);
        save.setOnAction(event -> {
            plan.save(slot);
            say("Sauvegardé dans l'emplacement " + (slot + 1) + " : " + describe(plan.code()) + ".");
        });
        load.setOnAction(event -> report(plan.loadSaved(slot), "L'emplacement " + (slot + 1) + " est vide."));
        copy.setOnAction(event -> {
            String code = plan.code();
            try {
                clipboardWrite.accept(code);
                say("Code copié dans le presse-papiers : " + describe(code) + ".");
            } catch (RuntimeException unavailable) {
                say("Le presse-papiers n'est pas accessible.");
            }
        });
        paste.setOnAction(event -> {
            String text;
            try {
                text = clipboardRead.get();
            } catch (RuntimeException unavailable) {
                say("Le presse-papiers n'est pas accessible.");
                return;
            }
            report(plan.load(text), "Rien n'a changé : le presse-papiers ne contient pas de code lisible.");
        });
        forget.setOnAction(event -> {
            plan.forget();
            say("Plan oublié : rien de ce qui est en place ne bouge.");
        });
        getChildren().addAll(status, buttons, note);
        refresh();
    }

    private static Button button(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        return button;
    }

    /** Remplace le presse-papiers : pour les vérifications. */
    public void setClipboard(Supplier<String> read, Consumer<String> write) {
        clipboardRead = read;
        clipboardWrite = write;
    }

    private void report(Game.PlanLoad result, String empty) {
        if (result.empty()) {
            say(empty);
            return;
        }
        say("Plan chargé : " + result.known() + (result.known() > 1 ? " éléments" : " élément")
                + (result.unknown() > 0 ? ", " + result.unknown() + " écarté(s), inconnu(s) ou mal placé(s)" : "")
                + ". Le reste se prendra dès que possible.");
    }

    private String describe(String code) {
        int colon = code.indexOf(':');
        String body = colon < 0 ? code : code.substring(colon + 1);
        return body.isBlank() ? what + " (vide)" : what + ", " + plan.describe(code);
    }

    /** Le texte d'un emplacement dans la liste. */
    private String slotText(int index) {
        String code = plan.saved(index);
        int colon = code.indexOf(':');
        boolean empty = code.isBlank() || (colon >= 0 && code.substring(colon + 1).isBlank());
        return "Emplacement " + (index + 1) + " : " + (empty ? "vide" : plan.describe(code));
    }

    private void say(String text) {
        note.setText(text);
        refresh();
    }

    /** Recopie l'état du plan. */
    public void refresh() {
        status.setText(plan.active() ? "Plan suivi : " + plan.progress() : "Aucun plan suivi.");
        java.util.List<String> texts = new java.util.ArrayList<>();
        for (int i = 0; i < idle.core.GameState.PLAN_SLOTS; i++) texts.add(slotText(i));
        if (!texts.equals(slots.getItems())) {
            filling = true;
            slots.getItems().setAll(texts);
            slots.setValue(texts.get(slot));
            filling = false;
        } else if (!texts.get(slot).equals(slots.getValue())) {
            filling = true;
            slots.setValue(texts.get(slot));
            filling = false;
        }
        load.setDisable(plan.saved(slot).isBlank());
        forget.setDisable(!plan.active());
    }

    /** Le message de la dernière action : pour les vérifications. */
    public String noteText() {
        return note.getText();
    }

    /** Le résumé du plan : pour les vérifications. */
    public String statusText() {
        return status.getText();
    }

    /** La liste des emplacements : pour les vérifications. */
    public javafx.scene.control.ComboBox<String> slots() {
        return slots;
    }

    /** L'emplacement choisi, de 0 à 4 : pour les vérifications. */
    public int slot() {
        return slot;
    }

    /** Un bouton, par sa place (0 sauvegarder, 1 charger, 2 copier, 3 coller, 4 oublier) : pour les vérifications. */
    public void press(int index) {
        new Button[] {save, load, copy, paste, forget}[index].fire();
    }

    private static String readClipboard() {
        return Clipboard.getSystemClipboard().getString();
    }

    private static void writeClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        if (!Clipboard.getSystemClipboard().setContent(content)) throw new IllegalStateException("presse-papiers");
    }
}
