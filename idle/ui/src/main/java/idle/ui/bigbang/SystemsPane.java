package idle.ui.bigbang;

import idle.core.Game;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import idle.ui.GameApp;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Les systèmes solaires, sous-page du Big Bang (version 0.54) : on fonde un système autour d'une
 * étoile mûre, on y range des planètes de la plus proche à la plus lointaine et des lunes, et
 * l'harmonie dit ce que chaque système rapporte. La galaxie en demande {@link Game#SYSTEMS_FOR_GALAXY}.
 *
 * <p>La page ne se reconstruit que lorsque les systèmes ou les astres libres changent : un bouton
 * reste sous le curseur tant qu'on ne l'a pas pressé.
 */
public final class SystemsPane extends VBox {

    private static final String COLOR = GameApp.BIG_BANG_COLOR;
    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 4;"
            + " -fx-border-radius: 4; -fx-text-fill: " + COLOR + "; -fx-background-color: #121923; -fx-border-color: " + COLOR + "66;";

    private final Game game;
    private final Runnable changed;
    private final Label intro = new Label();
    private final Label summary = new Label();
    private final Label foundTitle = new Label("Fonder un système autour de :");
    private final FlowPane stars = new FlowPane(6, 6);
    private final VBox systems = new VBox(12);
    private String shown = "";

    SystemsPane(Game game, Runnable changed) {
        super(10);
        this.game = game;
        this.changed = changed;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));
        intro.setText("Autour d'une étoile mûre, rangez des planètes, de la plus proche à la plus lointaine, et des lunes, "
                + "une par planète au plus. Un système est complet à " + Game.SYSTEM_MIN_PLANETS + " planètes ; la galaxie en "
                + "demande " + Game.SYSTEMS_FOR_GALAXY + ". L'harmonie compte un point par planète bien placée (les telluriques "
                + "près de l'étoile, les glacées et les gazeuses au loin), par lune, pour une étoile géante, et deux pour une "
                + "planète océan en deuxième ou troisième orbite d'un Soleil ou d'une naine rouge. Chaque point d'un système "
                + "complet ajoute " + ElementText.percent(Game.SYSTEM_GAIN) + " à toutes les grandeurs. Tout se range et se "
                + "défait librement.");
        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setMaxWidth(760);
        intro.setTextAlignment(TextAlignment.CENTER);
        summary.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        summary.setWrapText(true);
        summary.setMaxWidth(760);
        summary.setTextAlignment(TextAlignment.CENTER);
        foundTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #cfd8e3;");
        stars.setAlignment(Pos.CENTER);
        stars.setMaxWidth(780);
        systems.setAlignment(Pos.TOP_CENTER);
        getChildren().addAll(intro, summary, foundTitle, stars, systems);
    }

    void refresh() {
        intro.setVisible(Detail.shown());
        intro.setManaged(Detail.shown());
        int complete = game.solarSystemsComplete();
        int harmony = 0;
        for (SolarSystem system : game.state().solarSystems()) if (game.isSystemComplete(system)) harmony += game.systemHarmony(system);
        summary.setText("Systèmes complets : " + complete + "/" + Game.SYSTEMS_FOR_GALAXY
                + (complete >= Game.SYSTEMS_FOR_GALAXY ? " · la galaxie a ce qu'il lui faut" : " · la galaxie en demande " + Game.SYSTEMS_FOR_GALAXY)
                + (harmony > 0 ? " · harmonie " + harmony + " : +" + ElementText.percent(harmony * Game.SYSTEM_GAIN) + " à toutes les grandeurs" : ""));
        String key = game.state().solarSystems() + "|" + game.freeForSystems(Body.Tier.STAR) + game.freeForSystems(Body.Tier.PLANET)
                + game.freeForSystems(Body.Tier.MOON) + Detail.shown();
        if (key.equals(shown)) return;
        shown = key;
        rebuild();
    }

    /** Refait les boutons : les étoiles libres, puis un cadre par système. */
    private void rebuild() {
        stars.getChildren().clear();
        List<String> freeStars = game.freeForSystems(Body.Tier.STAR);
        for (String star : freeStars) {
            stars.getChildren().add(button(game.body(star).name(), () -> game.foundSystem(star)));
        }
        boolean any = !freeStars.isEmpty();
        foundTitle.setText(any ? "Fonder un système autour de :" : game.state().solarSystems().isEmpty()
                ? "Il faut une étoile formée et mûre pour fonder un système." : "Plus d'étoile libre : toutes ont leur système.");
        stars.setVisible(any);
        stars.setManaged(any);

        systems.getChildren().clear();
        List<SolarSystem> all = game.state().solarSystems();
        for (int index = 0; index < all.size(); index++) systems.getChildren().add(box(index, all.get(index)));
    }

    private VBox box(int index, SolarSystem system) {
        boolean complete = game.isSystemComplete(system);
        int harmony = game.systemHarmony(system);
        Label title = new Label("Système " + (index + 1) + " : " + game.body(system.star()).name() + " · "
                + (complete ? "complet · harmonie " + harmony + " : +" + ElementText.percent(harmony * Game.SYSTEM_GAIN)
                        : "il manque " + (Game.SYSTEM_MIN_PLANETS - system.planets().size())
                                + (Game.SYSTEM_MIN_PLANETS - system.planets().size() > 1 ? " planètes" : " planète")));
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (complete ? "#ffe9a8" : COLOR) + ";");
        VBox box = new VBox(6, title);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setMaxWidth(780);
        box.setStyle("-fx-background-color: #121923; -fx-background-radius: 8; -fx-border-color: " + COLOR + "55; -fx-border-radius: 8;");

        List<String> planets = system.planets();
        for (int orbit = 0; orbit < planets.size(); orbit++) {
            String planet = planets.get(orbit);
            Label name = new Label("Orbite " + (orbit + 1) + " : " + game.body(planet).name());
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #d6e4f0;");
            name.setMinWidth(260);
            Button closer = button("Plus près", () -> game.movePlanet(index, planet, -1));
            closer.setDisable(orbit == 0);
            Button farther = button("Plus loin", () -> game.movePlanet(index, planet, 1));
            farther.setDisable(orbit == planets.size() - 1);
            HBox row = new HBox(8, name, closer, farther, button("Retirer", () -> game.removeFromSystem(index, planet)));
            row.setAlignment(Pos.CENTER);
            box.getChildren().add(row);
        }
        for (String moon : system.moons()) {
            Label name = new Label("Lune : " + game.body(moon).name());
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #d6e4f0;");
            name.setMinWidth(260);
            HBox row = new HBox(8, name, button("Retirer", () -> game.removeFromSystem(index, moon)));
            row.setAlignment(Pos.CENTER);
            box.getChildren().add(row);
        }

        FlowPane add = new FlowPane(6, 6);
        add.setAlignment(Pos.CENTER);
        for (String planet : game.freeForSystems(Body.Tier.PLANET)) {
            add.getChildren().add(button("+ " + game.body(planet).name(), () -> game.addPlanet(index, planet)));
        }
        if (system.moons().size() < planets.size()) {
            for (String moon : game.freeForSystems(Body.Tier.MOON)) {
                add.getChildren().add(button("+ " + game.body(moon).name(), () -> game.addMoon(index, moon)));
            }
        }
        if (!add.getChildren().isEmpty()) box.getChildren().add(add);

        List<String> reasons = game.systemHarmonyReasons(system);
        StringBuilder text = new StringBuilder();
        for (String reason : reasons) {
            if (reason.isEmpty()) continue;
            text.append(text.length() == 0 ? "" : "\n").append("● ").append(reason);
        }
        if (text.length() > 0) {
            Label why = new Label(text.toString());
            why.setStyle("-fx-font-size: 12px; -fx-text-fill: #a8b8c8;");
            box.getChildren().add(why);
        }
        box.getChildren().add(button("Défaire ce système", () -> game.dissolveSystem(index)));
        return box;
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle(BUTTON_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> {
            action.run();
            changed.run();
        });
        return button;
    }
}
