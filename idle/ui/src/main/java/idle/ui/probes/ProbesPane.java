package idle.ui.probes;

import idle.core.Game;
import idle.core.collider.Quark;
import idle.core.elements.PeriodicTable;
import idle.core.molecules.Molecule;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.probes.ProbeUpgrade;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Les sondes, sous-page du Big Bang : le butin rapporté, à réclamer d'un clic, récompense par
 * récompense ; les sondes en vol ; le départ, une destination puis une durée ; et les améliorations,
 * payées en échantillons. Rien ne s'y perd au Big Bang.
 */
public final class ProbesPane extends VBox {

    public static final String COLOR = "#ffcf8a";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 6 14; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + COLOR + "; -fx-background-color: #2a1f10; -fx-border-color: " + COLOR + ";";
    private static final String CLAIM_STYLE = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 28; -fx-cursor: hand;"
            + " -fx-background-radius: 8; -fx-border-radius: 8; -fx-text-fill: #1a1206; -fx-background-color: " + COLOR
            + "; -fx-border-color: #ffffff;";
    /** Largeur de la barre d'un voyage, en caractères. */
    private static final int BAR = 24;

    private final Game game;
    private final Runnable changed;
    private final Label intro = new Label();
    /** Les sondes arrivées : un bouton chacune, qui ouvre son butin. */
    private final VBox lootBox = new VBox(8);
    private final List<Button> lootButtons = new ArrayList<>();
    private String lootShown = "";
    private final ProbesView view;
    private final CanvasPane viewPane;
    /** Ce qu'ouvre un clic sur une sonde arrivée : le panneau du butin. Sans panneau, tout s'ouvre d'un coup. */
    private java.util.function.LongConsumer opener;
    private final Label boostLabel = new Label();
    private final VBox flying = new VBox(4);
    private final List<Label> flyingLabels = new ArrayList<>();
    private final Label launchTitle = new Label("Envoyer une sonde");
    private final FlowPane destinations = new FlowPane(6, 6);
    private final Map<ProbeDestination, Button> destinationButtons = new EnumMap<>(ProbeDestination.class);
    private final Label destinationNote = new Label();
    private final FlowPane trips = new FlowPane(8, 8);
    private final Map<ProbeTrip, Button> tripButtons = new EnumMap<>(ProbeTrip.class);
    private final Button urgent = new Button();
    private final Label urgentNote = new Label();
    private final Label samplesLabel = new Label();
    private final TileGrid upgradeGrid = new TileGrid(230, 3, 8);
    private final Map<ProbeUpgrade, Card> upgradeCards = new EnumMap<>(ProbeUpgrade.class);
    private ProbeDestination destination = ProbeDestination.VOID;

    public ProbesPane(Game game, Runnable changed) {
        super(10);
        this.game = game;
        this.changed = changed;
        this.opener = id -> {
            game.claimAllProbeRewards(id);
            changed.run();
        };
        this.view = new ProbesView(game, id -> opener.accept(id));
        this.viewPane = CanvasPane.filling(view);
        viewPane.setMaxWidth(780);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));

        intro.setText("Une sonde part " + (int) (ProbeTrip.SHORT.seconds() / 60) + " minutes à " + (int) (ProbeTrip.NIGHT.seconds() / 3600) + " heures. Plus le voyage est long, plus son butin est riche par heure, "
                + "et rien ne se perd si vous revenez tard : la sonde attend à destination. Un clic sur elle ouvre son "
                + "butin, carte par carte, ou tout d'un coup ; elle rentre alors au hangar. Les sondes et leurs améliorations traversent le Big Bang. "
                + "Pendant votre absence, une sonde arrivée vide son butin et repart pour le même voyage : rien à faire au retour.");
        note(intro, "#a8987f");

        lootBox.setAlignment(Pos.CENTER);

        boostLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffe9a8;");
        flying.setAlignment(Pos.CENTER);
        for (int i = 0; i < 4; i++) {
            Label label = new Label();
            label.setStyle("-fx-font-size: 13px; -fx-font-family: monospace; -fx-text-fill: #f3e2c6;");
            flyingLabels.add(label);
            flying.getChildren().add(label);
        }

        launchTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        destinations.setAlignment(Pos.CENTER);
        destinations.setMaxWidth(780);
        for (ProbeDestination where : ProbeDestination.values()) {
            Button button = new Button(where.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                destination = where;
                changed.run();
            });
            destinationButtons.put(where, button);
            destinations.getChildren().add(button);
        }
        note(destinationNote, "#c9b79a");
        trips.setAlignment(Pos.CENTER);
        trips.setMaxWidth(780);
        for (ProbeTrip trip : ProbeTrip.ordinary()) {
            Button button = new Button();
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                game.launchProbe(destination, trip);
                changed.run();
            });
            tripButtons.put(trip, button);
            trips.getChildren().add(button);
        }
        // L'onde d'urgence chimique (version 0.086) : sans destination, elle rapporte l'élément le plus demandé.
        urgent.setStyle(BUTTON_STYLE);
        urgent.setFocusTraversable(false);
        urgent.setOnAction(event -> {
            game.launchUrgentWave();
            changed.run();
        });
        note(urgentNote, "#c9b79a");

        samplesLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        upgradeGrid.setMaxWidth(780);
        for (ProbeUpgrade upgrade : ProbeUpgrade.values()) {
            Card card = new Card(COLOR);
            card.setOnAction(() -> {
                game.buyProbeUpgrade(upgrade);
                changed.run();
            });
            upgradeCards.put(upgrade, card);
            upgradeGrid.add(card);
        }

        getChildren().addAll(intro, viewPane, lootBox, boostLabel, flying, launchTitle, destinations, destinationNote, trips, urgent, urgentNote, samplesLabel, upgradeGrid);
    }

    private static void note(Label label, String color) {
        Labels.centered(label, "-fx-font-size: 12px; -fx-text-fill: " + color + ";");
        label.setMaxWidth(760);
    }

    /** Le panneau qui ouvre le butin d'une sonde, par son numéro. */
    public void setOpener(java.util.function.LongConsumer opener) {
        this.opener = opener;
    }

    /** Ouvre le butin de cette sonde. */
    public void open(long probe) {
        opener.accept(probe);
    }

    /** Le dessin des trajets : pour les vérifications. */
    public ProbesView view() {
        return view;
    }

    /** Les boutons des sondes arrivées : pour les vérifications. */
    public List<Button> lootButtons() {
        return lootButtons;
    }

    /** Fait avancer les sondes à l'écran. */
    public void frame(double elapsed) {
        view.frame(elapsed);
    }

    /** Les boutons des sondes arrivées, refaits seulement quand elles changent : un bouton reste sous le curseur. */
    private void refreshLoot() {
        StringBuilder key = new StringBuilder();
        for (Probe probe : game.landedProbes()) key.append(probe.id()).append(':').append(game.probeRewardsOf(probe.id()).size()).append(' ');
        int loose = game.probeRewardsOf(-1).size();
        key.append("libres ").append(loose);
        if (key.toString().equals(lootShown)) return;
        lootShown = key.toString();
        lootBox.getChildren().clear();
        lootButtons.clear();
        List<Probe> all = game.state().probes();
        for (Probe probe : game.landedProbes()) {
            int count = game.probeRewardsOf(probe.id()).size();
            Button button = new Button("Sonde " + (all.indexOf(probe) + 1) + " arrivée · " + probe.destination().label()
                    + " : ouvrir le butin (" + count + (count > 1 ? " récompenses)" : " récompense)"));
            button.setStyle(CLAIM_STYLE);
            button.setFocusTraversable(false);
            button.setOnAction(event -> opener.accept(probe.id()));
            lootButtons.add(button);
            lootBox.getChildren().add(button);
        }
        if (loose > 0) {
            // Des récompenses d'une version d'avant, sans sonde : elles se réclament d'un coup.
            Button button = new Button("Récompenses en attente : tout réclamer (" + loose + ")");
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                while (!game.probeRewardsOf(-1).isEmpty() && game.claimProbeReward() != null) { }
                changed.run();
            });
            lootButtons.add(button);
            lootBox.getChildren().add(button);
        }
        boolean any = !lootButtons.isEmpty();
        lootBox.setVisible(any);
        lootBox.setManaged(any);
    }

    /** La destination choisie : pour les vérifications. */
    public ProbeDestination destination() {
        return destination;
    }

    public void refresh() {
        intro.setVisible(Detail.shown());
        intro.setManaged(Detail.shown());

        refreshLoot();
        double height = ProbesView.heightFor(Math.min(ProbesView.LANES, game.probeSlots()));
        viewPane.setMinHeight(height);
        viewPane.setPrefHeight(height);
        viewPane.setMaxHeight(height);

        boolean boost = game.state().probeBoostLeft() > 0;
        boostLabel.setVisible(boost);
        boostLabel.setManaged(boost);
        if (boost) {
            boostLabel.setText("Sursaut d'expansion : espace par seconde ×" + ElementText.number(Game.PROBE_BOOST_FACTOR)
                    + ", encore " + Format.wait(game.state().probeBoostLeft()));
        }

        List<Probe> probes = game.state().probes();
        int slots = game.probeSlots();
        for (int i = 0; i < flyingLabels.size(); i++) {
            Label label = flyingLabels.get(i);
            boolean shown = i < slots;
            label.setVisible(shown);
            label.setManaged(shown);
            if (!shown) continue;
            if (i >= probes.size()) {
                label.setText("Sonde " + (i + 1) + " : au hangar, prête à partir");
            } else {
                Probe probe = probes.get(i);
                String where = probe.trip().isUrgent() ? "onde d'urgence chimique" : probe.destination().label();
                label.setText(probe.landed() ? "Sonde " + (i + 1) + " : " + (probe.trip().isUrgent() ? "onde d'urgence revenue" : "arrivée à " + where)
                        + ", son butin attend" : "Sonde " + (i + 1) + " : " + where + ", arrivée dans " + Format.wait(probe.left()));
            }
        }

        if (!game.isProbeDestinationOpen(destination)) destination = ProbeDestination.VOID;
        for (ProbeDestination where : ProbeDestination.values()) {
            Button button = destinationButtons.get(where);
            boolean open = game.isProbeDestinationOpen(where);
            button.setVisible(open);
            button.setManaged(open);
            button.setStyle(chipStyle(where == destination));
        }
        destinationNote.setText(destinationText(destination));
        boolean free = probes.size() < slots;
        boolean wave = game.isUrgentWaveUnlocked();
        urgent.setVisible(wave);
        urgent.setManaged(wave);
        urgentNote.setVisible(wave);
        urgentNote.setManaged(wave);
        if (wave) {
            int element = game.urgentElement();
            String symbol = element > 0 ? PeriodicTable.ELEMENTS.get(element - 1).symbol() : "";
            urgent.setText(ProbeTrip.URGENT.label() + " : " + minutes(game.probeDuration(ProbeTrip.URGENT)) + ", "
                    + (element > 0 ? "un lot de " + symbol : "rien à chercher"));
            urgent.setDisable(!game.canLaunchUrgentWave());
            urgentNote.setText(element > 0 ? "L'onde ramène un petit lot de l'élément déjà découvert que l'astre visé demande le plus : "
                    + symbol + " en ce moment (celui du retour compte). Jamais un élément inconnu."
                    : "L'onde ramène l'élément déjà découvert que l'astre visé demande le plus : il n'y en a pas en ce moment.");
        }
        for (ProbeTrip trip : ProbeTrip.ordinary()) {
            Button button = tripButtons.get(trip);
            button.setText(trip.label() + " : " + minutes(game.probeDuration(trip)) + ", "
                    + game.probeRewardCount(trip) + " récompenses");
            button.setDisable(!free);
        }
        launchTitle.setText(free ? "Envoyer une sonde" : "Toutes les sondes sont en vol");

        samplesLabel.setText("Échantillons : " + Format.whole(game.state().probeSamples()));
        for (ProbeUpgrade upgrade : ProbeUpgrade.values()) {
            Card card = upgradeCards.get(upgrade);
            int level = game.state().probeUpgradeLevel(upgrade);
            boolean max = level >= upgrade.maxLevel();
            boolean can = game.canBuyProbeUpgrade(upgrade);
            card.show(max ? Card.State.DONE : can ? Card.State.READY : Card.State.WAITING, level + "/" + upgrade.maxLevel(), "",
                    upgrade.label(), upgrade.effect() + ".", "", max ? "au maximum" : Format.whole(game.probeUpgradeCost(upgrade)) + " échantillons", "");
        }
    }

    /** Ce que rapporte une destination, en une phrase. */
    private String destinationText(ProbeDestination where) {
        if (where.kind() == null) return "Le vide intersidéral rapporte de l'espace et des sursauts d'expansion.";
        List<Integer> elements = game.probeElements(where);
        StringBuilder symbols = new StringBuilder();
        for (int i = 0; i < Math.min(8, elements.size()); i++) {
            symbols.append(i == 0 ? "" : ", ").append(PeriodicTable.ELEMENTS.get(elements.get(i) - 1).symbol());
        }
        if (elements.size() > 8) symbols.append("…");
        Integer missing = game.missingByKind().get(where.kind());
        return where.label() + " : des éléments du rayon « " + where.kind().label() + " »"
                + (elements.isEmpty() ? " (aucun n'est encore découvert : ce sera de l'espace)" : " (" + symbols + ")")
                + ", de l'espace ou un sursaut d'expansion" + (missing != null && missing > 0 ? ". Ce rayon manque au prochain pas." : ".");
    }

    /** Une récompense, en mots : « +18 Fe », « +4 min d'expansion d'un coup : +1,2 M d'espace »… */
    static String describe(Game game, ProbeReward reward) {
        return switch (reward.type()) {
            case ELEMENTS -> {
                int element = Integer.parseInt(reward.target());
                boolean known = game.state().elementCount(element) > 0;
                yield known ? "+" + game.probeElementsOf(reward) + " " + PeriodicTable.ELEMENTS.get(element - 1).name()
                        + " (" + PeriodicTable.ELEMENTS.get(element - 1).symbol() + ")"
                        : spaceText(game, reward) + " (l'élément n'est plus découvert)";
            }
            case SPACE -> spaceText(game, reward);
            case BOOST -> "Sursaut d'expansion : espace par seconde ×" + ElementText.number(Game.PROBE_BOOST_FACTOR)
                    + " pendant " + minutes(game.probeBoostOf(reward));
            case SAMPLES -> "+" + game.probeSamplesOf(reward) + " échantillons";
            case QUARKS -> "+" + game.probeCharmOf(reward) + " " + Quark.CHARM.symbol()
                    + (game.probeCharmOf(reward) / 2 > 0 ? ", +" + game.probeCharmOf(reward) / 2 + " " + Quark.BOTTOM.symbol() : "")
                    + " pour le collisionneur";
        };
    }

    private static String spaceText(Game game, ProbeReward reward) {
        return "+" + minutes(Game.PROBE_SPACE_SECONDS * reward.value()) + " d'expansion d'un coup : +"
                + Format.count(game.probeSpaceOf(reward)) + " d'espace";
    }

    /** Une durée à la minute près : « 15 min », « 1 h », « 3 h 36 min ». */
    public static String minutes(double seconds) {
        if (seconds < 60) return Format.wait(seconds);
        long total = Math.round(seconds / 60);
        if (total < 60) return total + " min";
        return total / 60 + " h" + (total % 60 == 0 ? "" : " " + (total % 60 < 10 ? "0" : "") + total % 60 + " min");
    }

    private static String chipStyle(boolean selected) {
        return "-fx-font-size: 12px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 12; -fx-border-radius: 12;"
                + (selected ? " -fx-text-fill: #1a1206; -fx-background-color: " + COLOR + "; -fx-border-color: " + COLOR + ";"
                        : " -fx-text-fill: " + COLOR + "; -fx-background-color: transparent; -fx-border-color: #6b5a3e;");
    }

    /** La barre d'un voyage : la part faite. */
    public static String bar(double done) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, done)) * BAR);
        return "[" + "=".repeat(filled) + "·".repeat(BAR - filled) + "]";
    }

    /** Vrai s'il y a quelque chose à faire ici : une récompense à réclamer, ou une sonde au hangar. */
    public static boolean ready(Game game) {
        return game.isProbesUnlocked() && (!game.state().probeRewards().isEmpty() || game.state().probes().size() < game.probeSlots());
    }

    /** Pour les molécules : la destination qui rapporte les éléments de ce rayon. */
    static ProbeDestination of(Molecule.Kind kind) {
        for (ProbeDestination where : ProbeDestination.values()) if (where.kind() == kind) return where;
        return ProbeDestination.VOID;
    }
}
