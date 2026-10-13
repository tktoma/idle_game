package idle.ui.probes;

import idle.core.Game;
import idle.core.probes.Probe;
import idle.core.probes.ProbeReward;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Le butin d'une sonde, par-dessus la fenêtre : une carte au premier plan, retournée. Un clic
 * l'ouvre (la récompense s'applique), le suivant passe à la carte d'après ; « Tout ouvrir »
 * ouvre d'un coup ce qui reste. Les cartes ouvertes se rangent en petit, dessous.
 */
public final class LootPane extends StackPane {

    private static final String COLOR = ProbesPane.COLOR;
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + COLOR + "; -fx-background-color: #2a1f10; -fx-border-color: " + COLOR + ";";
    /** Durée de l'apparition d'une carte, en secondes. */
    private static final double POP = 0.18;

    private final Game game;
    private final Runnable changed;
    private final Label title = new Label();
    private final Label counter = new Label();
    private final Label cardMark = new Label();
    private final Label cardKind = new Label();
    private final Label cardText = new Label();
    private final Label cardHint = new Label();
    private final VBox card = new VBox(8, cardMark, cardKind, cardText, cardHint);
    private final FlowPane opened = new FlowPane(8, 8);
    private final Button openAll = new Button("Tout ouvrir");
    private final Button close = new Button("Fermer");
    private long probe = -1;
    private String destination = "";
    private int total;
    /** La dernière récompense ouverte, montrée face visible sur la carte ; {@code null} avant le premier clic. */
    private String shownKind;
    private String shownText;
    private final List<String[]> history = new ArrayList<>();
    private double pop;

    public LootPane(Game game, Runnable changed) {
        this.game = game;
        this.changed = changed;
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fff2dc;");
        counter.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9b79a;");
        cardKind.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        cardText.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fff2dc;");
        cardText.setWrapText(true);
        cardText.setTextAlignment(TextAlignment.CENTER);
        cardText.setMaxWidth(300);
        cardHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #a8987f;");
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(340, 210);
        card.setMinSize(340, 210);
        card.setMaxSize(340, 210);
        card.setPadding(new Insets(18));
        card.setOnMouseClicked(event -> next());
        opened.setAlignment(Pos.CENTER);
        opened.setMaxWidth(720);
        openAll.setStyle(BUTTON_STYLE);
        openAll.setFocusTraversable(false);
        openAll.setOnAction(event -> openAll());
        close.setStyle(BUTTON_STYLE);
        close.setFocusTraversable(false);
        close.setOnAction(event -> close());
        HBox buttons = new HBox(12, openAll, close);
        buttons.setAlignment(Pos.CENTER);
        VBox column = new VBox(14, title, counter, card, buttons, opened);
        column.setAlignment(Pos.CENTER);
        column.setMaxWidth(760);
        column.setMaxHeight(USE_PREF_SIZE);
        setStyle("-fx-background-color: #0a0806dd;");
        setPadding(new Insets(24));
        getChildren().add(column);
        setVisible(false);
        // Un clic à côté de la carte ne ferme rien : seul « Fermer » le fait, pour ne pas perdre le fil.
    }

    /** Ouvre le butin de cette sonde : la première carte, retournée. */
    public void open(long probeId) {
        Probe found = null;
        for (Probe each : game.state().probes()) if (each.id() == probeId) found = each;
        if (found == null || game.probeRewardsOf(probeId).isEmpty()) return;
        probe = probeId;
        destination = found.destination().label();
        total = game.probeRewardsOf(probeId).size();
        shownKind = null;
        shownText = null;
        history.clear();
        pop = POP;
        setVisible(true);
        refresh();
    }

    /** Ouvre le butin de la première sonde arrivée, s'il y en a une. Vrai si le panneau s'est ouvert. */
    public boolean openFirst() {
        List<Probe> landed = game.landedProbes();
        if (landed.isEmpty()) return false;
        open(landed.get(0).id());
        return isOpen();
    }

    public boolean isOpen() {
        return probe >= 0;
    }

    void close() {
        probe = -1;
        setVisible(false);
        changed.run();
    }

    /** Un clic sur la carte : la récompense suivante s'ouvre ; tout ouvert, le clic ferme. */
    public void next() {
        if (!isOpen()) return;
        List<ProbeReward> left = game.probeRewardsOf(probe);
        if (left.isEmpty()) {
            close();
            return;
        }
        reveal(left.get(0));
        pop = POP;
        refresh();
        changed.run();
    }

    /** Ouvre d'un coup tout ce qui reste. */
    void openAll() {
        if (!isOpen()) return;
        for (ProbeReward reward : game.probeRewardsOf(probe)) reveal(reward);
        pop = POP;
        refresh();
        changed.run();
    }

    /** Applique une récompense, et la garde pour l'affichage : son texte se calcule avant, au prix du moment. */
    private void reveal(ProbeReward reward) {
        String kind = kind(reward);
        String text = ProbesPane.describe(game, reward);
        game.claimProbeReward(probe);
        shownKind = kind;
        shownText = text;
        history.add(new String[] {kind, text});
    }

    /** La couleur d'une sorte de récompense. */
    private static String tint(String kind) {
        return switch (kind) {
            case "Éléments" -> "#8ad0ff";
            case "Espace" -> "#c8a6ff";
            case "Sursaut d'expansion" -> "#ffe98a";
            case "Quarks" -> "#ff8ad8";
            default -> COLOR;
        };
    }

    static String kind(ProbeReward reward) {
        return switch (reward.type()) {
            case ELEMENTS -> "Éléments";
            case SPACE -> "Espace";
            case BOOST -> "Sursaut d'expansion";
            case SAMPLES -> "Échantillons";
            case QUARKS -> "Quarks";
        };
    }

    /** Fait apparaître la carte en douceur : appelé à chaque image tant que le panneau est ouvert. */
    public void frame(double elapsed) {
        if (!isOpen() || pop <= 0) return;
        pop = Math.max(0, pop - elapsed);
        double scale = 1 - 0.12 * pop / POP;
        card.setScaleX(scale);
        card.setScaleY(scale);
    }

    void refresh() {
        if (!isOpen()) return;
        int left = game.probeRewardsOf(probe).size();
        title.setText("Butin de la sonde · " + destination);
        int done = total - left;
        counter.setText(left == 0 ? "Tout est ouvert : " + total + (total > 1 ? " récompenses" : " récompense")
                : done == 0 ? total + (total > 1 ? " récompenses à ouvrir" : " récompense à ouvrir")
                : "Ouvertes : " + done + " sur " + total);
        boolean faceUp = shownText != null;
        String border = faceUp ? COLOR : "#6b5a3e";
        card.setStyle("-fx-background-color: " + (faceUp ? "#3a2a14" : "#1c140a") + "; -fx-background-radius: 14; -fx-border-radius: 14;"
                + " -fx-border-width: 2; -fx-border-color: " + border + "; -fx-cursor: hand;");
        String tint = faceUp ? tint(shownKind) : "#6b5a3e";
        cardMark.setText(faceUp ? "◆" : "?");
        cardMark.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: " + tint + ";");
        cardKind.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (faceUp ? tint : COLOR) + ";");
        if (!faceUp) {
            cardKind.setText("Récompense");
            cardText.setText("Une récompense de " + destination);
            cardHint.setText("Cliquer pour l'ouvrir");
        } else {
            cardKind.setText(shownKind);
            cardText.setText(shownText);
            cardHint.setText(left == 0 ? "Cliquer pour fermer" : "Cliquer pour la suivante (" + left + " encore)");
        }
        openAll.setVisible(left > 0);
        openAll.setManaged(left > 0);
        openAll.setText("Tout ouvrir (" + left + ")");
        opened.getChildren().clear();
        for (String[] each : history) opened.getChildren().add(small(each[0], each[1]));
    }

    private static VBox small(String kind, String text) {
        Label top = new Label(kind);
        top.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR + ";");
        Label line = new Label(text);
        line.setStyle("-fx-font-size: 12px; -fx-text-fill: #f3e2c6;");
        line.setWrapText(true);
        line.setMaxWidth(150);
        VBox box = new VBox(4, top, line);
        box.setPadding(new Insets(8));
        box.setPrefWidth(170);
        box.setStyle("-fx-background-color: #2a1f10; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #6b5a3e;");
        return box;
    }

    /** Pour les vérifications. */
    public String cardText() {
        return cardKind.getText() + " | " + cardText.getText() + " | " + cardHint.getText();
    }

    public String counterText() {
        return counter.getText();
    }

    public int openedCount() {
        return history.size();
    }
}
