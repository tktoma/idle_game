package idle.ui.bigbang;

import idle.core.Game;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import idle.ui.GameApp;
import idle.ui.common.CanvasPane;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Les systèmes solaires, sous-page du Big Bang (version 0.54, redessinée en 0.59). En haut, les trois
 * pas à suivre et où l'on en est. Puis un cadre par système : son dessin, où chaque orbite dit si sa
 * planète est à sa place, les planètes à déplacer ou retirer, celles qu'on peut ajouter avec la
 * place qui leur convient, et « Ranger au mieux » quand un autre ordre rapporterait plus.
 *
 * <p>La page ne se reconstruit que lorsque les systèmes ou les astres libres changent : un bouton
 * reste sous le curseur tant qu'on ne l'a pas pressé.
 */
public final class SystemsPane extends VBox {

    private static final String COLOR = GameApp.BIG_BANG_COLOR;
    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 4;"
            + " -fx-border-radius: 4; -fx-text-fill: " + COLOR + "; -fx-background-color: #121923; -fx-border-color: " + COLOR + "66;";
    private static final String STRONG_STYLE = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 5 14; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6; -fx-text-fill: #10151f; -fx-background-color: #9be7a8; -fx-border-color: #d8ffe0;";
    private static final String STEP_STYLE = "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 8; -fx-border-radius: 8;";

    private final Game game;
    private final Runnable changed;
    private final Label intro = new Label();
    private final Label summary = new Label();
    private final Label[] steps = {new Label(), new Label(), new Label()};
    private final Label foundTitle = new Label();
    private final FlowPane stars = new FlowPane(6, 6);
    private final VBox systems = new VBox(14);
    private final List<SystemView> views = new ArrayList<>();
    private String shown = "";

    SystemsPane(Game game, Runnable changed) {
        super(10);
        this.game = game;
        this.changed = changed;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));
        intro.setText("L'harmonie compte un point par planète bien placée (les telluriques — roche, lave, océan — près de "
                + "l'étoile, les glacées et les gazeuses au loin), un par lune (une par planète au plus), un pour une étoile "
                + "géante, et deux pour une planète océan dans la zone habitable d'un Soleil ou d'une naine rouge (orbites 2 et 3). "
                + "Chaque point d'un système complet ajoute " + ElementText.percent(Game.SYSTEM_GAIN) + " à toutes les grandeurs. "
                + "Tout se range et se défait librement : rien ne se perd.");
        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setMaxWidth(760);
        intro.setTextAlignment(TextAlignment.CENTER);
        summary.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        summary.setWrapText(true);
        summary.setMaxWidth(760);
        summary.setTextAlignment(TextAlignment.CENTER);
        HBox stepRow = new HBox(8, steps);
        stepRow.setAlignment(Pos.CENTER);
        stepRow.setMaxWidth(780);
        for (Label step : steps) {
            step.setWrapText(true);
            step.setMaxWidth(250);
            step.setMinHeight(44);
            HBox.setHgrow(step, Priority.ALWAYS);
        }
        foundTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #cfd8e3;");
        foundTitle.setWrapText(true);
        foundTitle.setMaxWidth(760);
        foundTitle.setTextAlignment(TextAlignment.CENTER);
        stars.setAlignment(Pos.CENTER);
        stars.setMaxWidth(780);
        systems.setAlignment(Pos.TOP_CENTER);
        getChildren().addAll(summary, stepRow, intro, foundTitle, stars, systems);
    }

    /** Une étape : faite (verte), en cours (dorée), ou à venir (grise). */
    private void step(int index, String text, boolean done, boolean current) {
        Label step = steps[index];
        step.setText((done ? "✓ " : (index + 1) + ". ") + text);
        String color = done ? "#9be7a8" : current ? COLOR : "#6f8296";
        step.setStyle(STEP_STYLE + " -fx-text-fill: " + color + "; -fx-border-color: " + color + (done || current ? "" : "66")
                + "; -fx-background-color: " + (current ? "#1c1a12" : "#0f151e") + ";");
    }

    void refresh() {
        intro.setVisible(Detail.shown());
        intro.setManaged(Detail.shown());
        int complete = game.solarSystemsComplete();
        int harmony = 0;
        for (SolarSystem system : game.state().solarSystems()) if (game.isSystemComplete(system)) harmony += game.systemHarmony(system);
        summary.setText("Systèmes complets : " + complete + " sur " + Game.SYSTEMS_FOR_GALAXY
                + (complete >= Game.SYSTEMS_FOR_GALAXY ? ", la galaxie a ce qu'il lui faut" : " pour la galaxie")
                + (harmony > 0 ? "\nHarmonie " + harmony + " : +" + ElementText.percent(harmony * Game.SYSTEM_GAIN) + " à toutes les grandeurs" : ""));
        boolean founded = !game.state().solarSystems().isEmpty();
        boolean completed = complete > 0;
        boolean arranged = completed;
        for (int i = 0; i < game.state().solarSystems().size(); i++) arranged &= !game.canArrangeSystem(i);
        step(0, "Fonder un système autour d'une étoile mûre", founded, !founded);
        step(1, "Y placer " + Game.SYSTEM_MIN_PLANETS + " planètes ou plus : il est complet", completed, founded && !completed);
        step(2, "Bien les ranger : chaque point d'harmonie donne +" + ElementText.percent(Game.SYSTEM_GAIN), arranged && harmony > 0,
                completed && !arranged);
        String key = game.state().solarSystems() + "|" + game.freeForSystems(Body.Tier.STAR) + game.freeForSystems(Body.Tier.PLANET)
                + game.freeForSystems(Body.Tier.MOON) + Detail.shown();
        if (key.equals(shown)) return;
        shown = key;
        rebuild();
    }

    /** Fait tourner les planètes : appelé à chaque image tant que la sous-page est affichée. */
    void frame(double elapsed) {
        for (SystemView view : views) view.frame(elapsed);
    }

    /** Le dessin de chaque système : pour les vérifications. */
    List<SystemView> views() {
        return List.copyOf(views);
    }

    /** Refait les boutons : les étoiles libres, puis un cadre par système. */
    private void rebuild() {
        stars.getChildren().clear();
        List<String> freeStars = game.freeForSystems(Body.Tier.STAR);
        for (String star : freeStars) {
            stars.getChildren().add(button("Fonder autour de : " + game.body(star).name() + starNote(star), () -> game.foundSystem(star)));
        }
        boolean any = !freeStars.isEmpty();
        foundTitle.setText(any ? "Étoiles mûres et libres :" : game.state().solarSystems().isEmpty()
                ? "Il faut une étoile formée et mûre pour fonder un système : formez-en une dans « Astres »." : "Plus d'étoile libre : toutes ont leur système.");
        stars.setVisible(any);
        stars.setManaged(any);

        systems.getChildren().clear();
        views.clear();
        List<SolarSystem> all = game.state().solarSystems();
        for (int index = 0; index < all.size(); index++) systems.getChildren().add(box(index, all.get(index)));
    }

    /** Ce que l'étoile apporte de particulier. */
    private static String starNote(String star) {
        return switch (star) {
            case "yellow_sun", "red_dwarf" -> " (zone habitable)";
            case "blue_giant", "red_giant" -> " (géante : +1 harmonie)";
            default -> "";
        };
    }

    private VBox box(int index, SolarSystem system) {
        boolean complete = game.isSystemComplete(system);
        int harmony = game.systemHarmony(system);
        int missing = Game.SYSTEM_MIN_PLANETS - system.planets().size();
        Label title = new Label("Système " + (index + 1) + " : " + game.body(system.star()).name());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + (complete ? "#ffe9a8" : COLOR) + ";");
        Label status = new Label(complete ? "complet · harmonie " + harmony + " : +" + ElementText.percent(harmony * Game.SYSTEM_GAIN)
                : "il manque " + missing + (missing > 1 ? " planètes" : " planète"));
        String statusColor = complete ? "#9be7a8" : "#ffb36b";
        status.setStyle("-fx-font-size: 12px; -fx-padding: 2 10; -fx-background-radius: 10; -fx-border-radius: 10;"
                + " -fx-text-fill: " + statusColor + "; -fx-border-color: " + statusColor + ";");
        HBox head = new HBox(10, title, status);
        head.setAlignment(Pos.CENTER);

        SystemView view = new SystemView(game, system);
        views.add(view);
        CanvasPane picture = CanvasPane.filling(view);
        picture.setMinHeight(200);
        picture.setPrefHeight(200);
        picture.setMaxHeight(200);
        picture.setMaxWidth(760);

        VBox box = new VBox(8, head, picture);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setMaxWidth(780);
        box.setStyle("-fx-background-color: #121923; -fx-background-radius: 10; -fx-border-color: "
                + (complete ? "#ffe9a8" : COLOR) + "55; -fx-border-radius: 10;");

        // Les planètes en place : on les déplace d'une orbite, ou on les retire.
        List<String> planets = system.planets();
        if (!planets.isEmpty()) {
            FlowPane orbitControls = new FlowPane(8, 6);
            orbitControls.setAlignment(Pos.CENTER);
            for (int orbit = 0; orbit < planets.size(); orbit++) {
                String planet = planets.get(orbit);
                Label name = new Label((orbit + 1) + ". " + game.body(planet).name());
                name.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (game.isPlanetWellPlaced(system, orbit) ? "#9be7a8" : "#ffb36b") + ";");
                Button closer = button("◀", () -> game.movePlanet(index, planet, -1));
                closer.setDisable(orbit == 0);
                tip(closer, "Plus près de l'étoile");
                Button farther = button("▶", () -> game.movePlanet(index, planet, 1));
                farther.setDisable(orbit == planets.size() - 1);
                tip(farther, "Plus loin de l'étoile");
                Button remove = button("✕", () -> game.removeFromSystem(index, planet));
                tip(remove, "Retirer du système");
                HBox chip = new HBox(4, name, closer, farther, remove);
                chip.setAlignment(Pos.CENTER);
                chip.setStyle("-fx-padding: 3 6; -fx-background-color: #0f151e; -fx-background-radius: 6;");
                orbitControls.getChildren().add(chip);
            }
            for (String moon : system.moons()) {
                Label name = new Label("Lune : " + game.body(moon).name());
                name.setStyle("-fx-font-size: 12px; -fx-text-fill: #d6e4f0;");
                Button remove = button("✕", () -> game.removeFromSystem(index, moon));
                tip(remove, "Retirer du système");
                HBox chip = new HBox(4, name, remove);
                chip.setAlignment(Pos.CENTER);
                chip.setStyle("-fx-padding: 3 6; -fx-background-color: #0f151e; -fx-background-radius: 6;");
                orbitControls.getChildren().add(chip);
            }
            box.getChildren().add(orbitControls);
        }

        if (game.canArrangeSystem(index)) {
            int gain = game.systemHarmony(game.bestArrangement(system)) - harmony;
            Button arrange = new Button("Ranger au mieux : +" + gain + " d'harmonie");
            arrange.setStyle(STRONG_STYLE);
            arrange.setFocusTraversable(false);
            arrange.setOnAction(event -> {
                game.arrangeSystem(index);
                changed.run();
            });
            box.getChildren().add(arrange);
        }

        // Ce qu'on peut ajouter, avec la place qui lui convient.
        FlowPane add = new FlowPane(6, 6);
        add.setAlignment(Pos.CENTER);
        for (String planet : game.freeForSystems(Body.Tier.PLANET)) {
            add.getChildren().add(button("+ " + game.body(planet).name() + (game.isTelluric(planet) ? " (près)" : " (au loin)"),
                    () -> game.addPlanet(index, planet)));
        }
        if (system.moons().size() < planets.size()) {
            for (String moon : game.freeForSystems(Body.Tier.MOON)) {
                add.getChildren().add(button("+ " + game.body(moon).name() + " (+1)", () -> game.addMoon(index, moon)));
            }
        }
        if (!add.getChildren().isEmpty()) {
            Label addTitle = new Label("Ajouter :");
            addTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
            box.getChildren().addAll(addTitle, add);
        } else if (planets.size() < 2) {
            Label none = new Label("Aucune planète mûre et libre : formez-en dans « Astres », ou retirez-en d'un autre système.");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
            box.getChildren().add(none);
        }

        List<String> reasons = game.systemHarmonyReasons(system);
        StringBuilder text = new StringBuilder();
        for (String reason : reasons) {
            if (reason.isEmpty()) continue;
            text.append(text.length() == 0 ? "" : "   ").append("● ").append(reason);
        }
        if (text.length() > 0) {
            Label why = new Label("Harmonie : " + text);
            why.setStyle("-fx-font-size: 12px; -fx-text-fill: #a8b8c8;");
            why.setWrapText(true);
            why.setMaxWidth(740);
            why.setTextAlignment(TextAlignment.CENTER);
            box.getChildren().add(why);
        }
        box.getChildren().add(button("Défaire ce système", () -> game.dissolveSystem(index)));
        return box;
    }

    private static void tip(Button button, String text) {
        button.setTooltip(new javafx.scene.control.Tooltip(text));
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
