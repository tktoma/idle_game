package idle.ui.pages;

import idle.core.Game;
import idle.core.history.Pace;
import idle.core.history.PurchaseHistory;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Vos achats (version 0.165), comme le bot analyste les note : la prochaine étape et le temps qu'elle demande au rythme
 * des dix dernières minutes, ce qui a fait gagner le plus de temps pendant la session, puis les derniers achats, avec
 * ce qu'ils ont coûté, leur poids sur les grandeurs et le temps gagné. Seuls les achats du joueur comptent : ceux des
 * automatismes passent pendant le temps de jeu, ils n'y sont pas. Recomposé deux fois par seconde au plus, et seulement
 * quand la sous-page est à l'écran.
 */
final class PurchasesPane extends VBox {

    static final double WIDTH = 640;
    /** Les choses les plus utiles montrées, et les derniers achats. */
    static final int BEST = 10, LAST = 40;
    private static final long EVERY_NANOS = 500_000_000L;

    private final Game game;
    private final PurchaseHistory history;
    private final Pace pace;
    private final Label next = new Label();
    private final Label empty = new Label("Aucun achat pendant cette session. Les achats que vous faites s'écrivent ici, avec leur prix, "
            + "leur poids sur les grandeurs et le temps qu'ils font gagner vers la prochaine étape.");
    private final VBox best = new VBox(3);
    private final VBox last = new VBox(6);
    private final Button clear = new Button("Vider l'historique");
    private long writtenAt = 0;
    /** Le texte des lignes au dernier dessin : pour les vérifications. */
    private final List<String> shown = new ArrayList<>();

    PurchasesPane(Game game, PurchaseHistory history, Pace pace) {
        super(8);
        this.game = game;
        this.history = history;
        this.pace = pace;
        setMaxWidth(WIDTH);
        next.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        next.setWrapText(true);
        next.setMaxWidth(WIDTH);
        empty.setStyle(StatsPageBase.NAME_STYLE);
        empty.setWrapText(true);
        empty.setMaxWidth(WIDTH);
        clear.setFocusTraversable(false);
        clear.setOnAction(event -> {
            history.clear();
            writtenAt = 0;
            refresh();
        });
        HBox actions = new HBox(clear);
        actions.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(next, header("Ce qui a fait gagner le plus de temps"), best, header("Derniers achats"), empty, last, actions);
    }

    private static Label header(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 0 0; -fx-text-fill: #ffd27f;");
        return label;
    }

    /** « Premier Big Bang dans 1 h 20 environ », ou ce qui manque pour l'estimer. */
    static String nextStep(Pace pace) {
        if (pace.step() == null) return "Toutes les étapes de cette partie sont atteintes.";
        double left = pace.secondsLeft();
        return "Prochaine étape : " + pace.step().label()
                + (Double.isNaN(left) ? " — pas d'estimation (" + pace.why() + ")" : ", dans " + about(left));
    }

    /** « 12 min environ » : une durée estimée. */
    static String about(double seconds) {
        String wait = Format.wait(seconds);
        return wait.startsWith("plus") ? wait : wait + " environ";
    }

    /** Recompose la sous-page, deux fois par seconde au plus. */
    void refresh() {
        long now = System.nanoTime();
        if (writtenAt != 0 && now - writtenAt < EVERY_NANOS) return;
        writtenAt = now;
        shown.clear();
        next.setText(nextStep(pace));
        shown.add(next.getText());

        best.getChildren().clear();
        List<PurchaseHistory.Total> totals = history.totals();
        for (PurchaseHistory.Total total : totals.subList(0, Math.min(BEST, totals.size()))) {
            String text = total.quoi() + " — " + total.achats() + (total.achats() > 1 ? " achats" : " achat")
                    + (total.gainSecondes() >= 1 ? ", " + Format.wait(total.gainSecondes()) + " gagnées" : "");
            best.getChildren().add(line(text, StatsPageBase.VALUE_STYLE));
            shown.add(text);
        }

        List<PurchaseHistory.Entry> recent = history.recent();
        empty.setVisible(recent.isEmpty());
        empty.setManaged(recent.isEmpty());
        last.getChildren().clear();
        for (PurchaseHistory.Entry e : recent.subList(0, Math.min(LAST, recent.size()))) {
            VBox box = new VBox(1);
            long levels = Math.round(e.quantite());
            String title = Format.duration(e.t()) + "  " + e.quoi() + (levels > 1 ? " ×" + levels : "");
            box.getChildren().add(line(title, StatsPageBase.VALUE_STYLE));
            box.getChildren().add(line(detail(e), StatsPageBase.NAME_STYLE));
            last.getChildren().add(box);
            shown.add(title);
        }
        clear.setDisable(recent.isEmpty());
    }

    /** « Payé 4,2e12 particules (12 % de la réserve) · Particules par seconde ×1,25 · 3 min gagnées vers « Premier Big Bang » ». */
    static String detail(PurchaseHistory.Entry e) {
        StringBuilder text = new StringBuilder();
        if (!e.montant().equals("—")) {
            text.append("Payé ").append(e.montant()).append(' ').append(e.paye().toLowerCase(Locale.FRANCE));
            if (!Double.isNaN(e.part()) && e.part() >= 0.0005) text.append(String.format(Locale.FRANCE, " (%.1f %% de la réserve)", 100 * e.part()));
            text.append("  ·  ");
        }
        text.append(e.poids());
        if (Double.isFinite(e.gainSecondes()) && e.gainSecondes() >= 1) {
            text.append("  ·  ").append(Format.wait(e.gainSecondes())).append(" gagnées vers « ").append(e.vers()).append(" »");
        }
        return text.toString();
    }

    private static Label line(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        label.setWrapText(true);
        label.setMaxWidth(WIDTH);
        return label;
    }

    /** Les lignes du dernier dessin. */
    List<String> shown() {
        return List.copyOf(shown);
    }
}
