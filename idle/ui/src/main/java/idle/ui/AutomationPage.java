package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
import idle.core.DarkAutomation;
import idle.core.Game;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Automatisation » : tous les automatismes du jeu au même endroit, chacun
 * n'apparaissant qu'une fois débloqué pour la première fois. Ensuite il reste affiché : si une
 * explosion le reverrouille, sa carte dit ce qu'il faut pour le débloquer de nouveau.
 *
 * <p>D'abord les automatismes ordinaires, une ligne chacun, faite de deux {@link Card}. À gauche,
 * l'automatisme : tant qu'il n'est pas acheté, la carte affiche son prix en atomes et sert à
 * l'acheter ; ensuite elle devient un interrupteur pour le mettre en marche ou le couper. À
 * droite, sa cadence : le délai entre deux actions, et le prix pour le réduire en atomes. La cadence achetée en atomes a un
 * maximum ; au-delà, seuls les éléments du tableau périodique réduisent encore le délai. Ils
 * apparaissent une fois assez d'atomes créés ({@link Game#UNLOCK_TOTAL_ATOMS}) ; la synthèse
 * automatique, elle, attend le premier élément unique du tableau périodique.
 *
 * <p>Ensuite les automatismes de matière noire, qui font ce que les ordinaires laissent au
 * joueur : chacun apparaît quand le joueur a gagné assez de matière noire, puis se met en marche
 * ou se coupe d'un clic. Ils ne coûtent rien, et aucun ne refait le travail d'un automatisme
 * ordinaire : le premier se contente de les offrir au début de chaque partie.
 *
 * <p>Sous chaque groupe, une ligne dit ce qui reste à découvrir, pour que le joueur sache qu'il
 * y a une suite.
 *
 * <p>L'onglet apparaît dès qu'un automatisme est débloqué, et reste là pour de bon une fois la
 * première matière noire obtenue.
 *
 * <p>Les cartes s'en tiennent à quelques mots ; les explications, sur les cartes comme entre
 * elles, n'apparaissent qu'en mode détails ({@link Detail}).
 */
final class AutomationPage extends VBox {

    private static final String HINT_STYLE = "-fx-font-size: 13px; -fx-text-fill: #8fa3b8;";

    private final Game game;
    private final Label balanceLabel = new Label();

    // Les automatismes ordinaires.
    private final Label ordinaryHint = new Label();
    private final Map<Automation, Card> cards = new LinkedHashMap<>();
    private final Map<Automation, Card> cadences = new LinkedHashMap<>();
    private final Map<Automation, HBox> rows = new LinkedHashMap<>();
    /** Ce qui reste à débloquer parmi les automatismes ordinaires. */
    private final Label ordinaryNext = new Label();
    // Le seuil de la fusion automatique, réglable une fois débloqué dans l'arbre de matière noire.
    private final Button lessGenerators = new Button("−");
    private final Button moreGenerators = new Button("+");
    private final Label thresholdLabel = new Label();
    private final HBox thresholdRow = new HBox(8, lessGenerators, thresholdLabel, moreGenerators);

    // La réserve d'atomes de la synthèse automatique, réglable dès qu'on la possède.
    private final Button lessReserve = new Button("−");
    private final Button moreReserve = new Button("+");
    private final Label reserveLabel = new Label();
    private final HBox reserveRow = new HBox(8, lessReserve, reserveLabel, moreReserve);
    /** Les réserves proposées, en atomes ; au-delà (plafond d'atomes levé), elles doublent. */
    private static final double[] RESERVES = {0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 80, 100};

    // Les automatismes de matière noire.
    private final Label darkTitle = new Label("Automatismes de matière noire");
    // L'automatisme acquis avec l'espace, après un Big Bang : une carte qui sert d'interrupteur.
    private final Label spaceTitle = new Label("Automatisme du Big Bang");
    private final Card holdCard = new Card(GameApp.BIG_BANG_COLOR);
    private final Card moleculesCard = new Card(GameApp.BIG_BANG_COLOR);
    private final TileGrid spaceGrid = new TileGrid(230, 2, 8);
    private final Label darkHint = new Label();
    private final Map<DarkAutomation, Card> darkCards = new LinkedHashMap<>();
    private final TileGrid darkGrid = new TileGrid(230, 2, 8);
    /** Ce qui reste à débloquer parmi les automatismes de matière noire. */
    private final Label darkNext = new Label();

    AutomationPage(Game game) {
        super(12);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24));

        Label title = new Label("Automatismes");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        ordinaryHint.setText("Chaque automatisme s'achète une fois, en atomes, puis s'active ou se coupe d'un clic. "
                + "Il agit à intervalles réguliers : accélérez sa cadence en atomes jusqu'à son maximum ; "
                + "au-delà, seuls les éléments du tableau périodique réduisent encore le délai.");
        wrapped(ordinaryHint, HINT_STYLE);
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffd27f;");
        getChildren().add(title);
        getChildren().add(ordinaryHint);
        getChildren().add(balanceLabel);

        // Une ligne par automatisme du catalogue : en ajouter un dans core suffit à le faire apparaître.
        for (Automation automation : game.automations()) {
            Card card = new Card(GameApp.AUTOMATION_COLOR);
            card.setPrefWidth(400);
            card.setMaxWidth(520);
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setOnAction(() -> {
                if (game.ownsAutomation(automation.id())) {
                    game.setAutomationEnabled(automation.id(), !game.isAutomationEnabled(automation.id()));
                } else {
                    game.buyAutomation(automation.id());
                }
                refresh();
            });
            cards.put(automation, card);

            // La cadence se paie en atomes : sa carte en a la couleur.
            Card cadence = new Card(GameApp.ATOMS_COLOR);
            cadence.setPrefWidth(250);
            cadence.setMaxWidth(250);
            cadence.setOnAction(() -> {
                game.speedUpAutomation(automation.id());
                refresh();
            });
            cadences.put(automation, cadence);

            HBox row = new HBox(8, card, cadence);
            row.setAlignment(Pos.CENTER);
            row.setFillHeight(true);     // les deux cartes ont la même hauteur, quelle qu'elle soit
            rows.put(automation, row);
            getChildren().add(row);
            // Le réglage du seuil vient juste sous l'automatisme de fusion.
            if (automation.kind() == Automation.Kind.FUSION) getChildren().add(thresholdRow);
            // Et celui de la réserve, sous la synthèse automatique.
            if (automation.kind() == Automation.Kind.SYNTHESIS) getChildren().add(reserveRow);
        }
        reserveRow.setAlignment(Pos.CENTER);
        wrapped(reserveLabel, "-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        String reserveStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 2 12; -fx-cursor: hand;"
                + " -fx-text-fill: #ffd27f; -fx-background-color: #2a2113; -fx-background-radius: 6;"
                + " -fx-border-color: #ffd27f; -fx-border-radius: 6;";
        lessReserve.setStyle(reserveStyle);
        moreReserve.setStyle(reserveStyle);
        lessReserve.setFocusTraversable(false);
        moreReserve.setFocusTraversable(false);
        lessReserve.setOnAction(event -> {
            game.setSynthesisReserve(BigNum.of(reserveStep(game.synthesisReserve().toDouble(), false)));
            refresh();
        });
        moreReserve.setOnAction(event -> {
            game.setSynthesisReserve(BigNum.of(reserveStep(game.synthesisReserve().toDouble(), true)));
            refresh();
        });
        thresholdRow.setAlignment(Pos.CENTER);
        thresholdLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9a6ff;");
        String stepStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 2 12; -fx-cursor: hand;"
                + " -fx-text-fill: #c9a6ff; -fx-background-color: #1c1630; -fx-background-radius: 6;"
                + " -fx-border-color: #c9a6ff; -fx-border-radius: 6;";
        lessGenerators.setStyle(stepStyle);
        moreGenerators.setStyle(stepStyle);
        lessGenerators.setFocusTraversable(false);
        moreGenerators.setFocusTraversable(false);
        // Un clic déplace le seuil d'un groupe de générateurs : 10, 20, 30…
        lessGenerators.setOnAction(event -> {
            game.setFusionThreshold(game.fusionThreshold() - game.generatorsPerAtom());
            refresh();
        });
        moreGenerators.setOnAction(event -> {
            game.setFusionThreshold(game.fusionThreshold() + game.generatorsPerAtom());
            refresh();
        });
        wrapped(ordinaryNext, "-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        getChildren().add(ordinaryNext);

        // Les automatismes de matière noire : une carte chacun, qui sert d'interrupteur.
        darkTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + GameApp.DARK_MATTER_COLOR + ";");
        darkHint.setText("Ils font ce que les automatismes ordinaires laissent au joueur, et ne coûtent rien. "
                + "Aucun ne fait le travail d'un automatisme ordinaire : le premier les offre, les "
                + "autres achètent ou font exploser. Attention : l'explosion automatique n'attend pas que vous ayez "
                + "dépensé vos particules et vos atomes dans l'arbre.");
        wrapped(darkHint, HINT_STYLE);
        getChildren().add(darkTitle);
        getChildren().add(darkHint);
        for (DarkAutomation automation : game.darkAutomations()) {
            Card card = new Card(GameApp.DARK_MATTER_COLOR);
            card.setOnAction(() -> {
                game.setDarkAutomationEnabled(automation.id(), !game.isDarkAutomationEnabled(automation.id()));
                refresh();
            });
            darkCards.put(automation, card);
            darkGrid.add(card);
            darkGrid.show(card, false);
        }
        darkGrid.setMaxWidth(778);       // la largeur d'une carte ordinaire et de sa cadence réunies
        getChildren().add(darkGrid);
        wrapped(darkNext, "-fx-font-size: 13px; -fx-text-fill: #c9bfe0;");
        getChildren().add(darkNext);

        // L'appui automatique : acquis dans les améliorations du Big Bang, réglé ici.
        spaceTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + GameApp.BIG_BANG_COLOR + ";");
        holdCard.setOnAction(() -> {
            game.setAutoHoldEnabled(!game.isAutoHoldEnabled());
            refresh();
        });
        spaceGrid.add(holdCard);
        // La création automatique des molécules : acquise avec un palier de Big Bang, réglée ici.
        moleculesCard.setOnAction(() -> {
            game.setMoleculeAutomationEnabled(!game.isMoleculeAutomationEnabled());
            refresh();
        });
        spaceGrid.add(moleculesCard);
        spaceGrid.setMaxWidth(778);
        getChildren().add(spaceTitle);
        getChildren().add(spaceGrid);
    }

    /** La carte de l'appui automatique : pour les vérifications. */
    Card holdCard() {
        return holdCard;
    }

    /** La carte de la création automatique des molécules : pour les vérifications. */
    Card moleculesCard() {
        return moleculesCard;
    }

    /**
     * La réserve suivante ou précédente : 0, 5, 10… 100 atomes, puis le double à chaque pas, ce qui
     * ne sert qu'une fois le plafond d'atomes levé.
     */
    static double reserveStep(double reserve, boolean up) {
        double top = RESERVES[RESERVES.length - 1];
        if (up) {
            for (double step : RESERVES) {
                if (step > reserve + 1e-9) return step;
            }
            return Math.min(reserve * 2, 1e300);
        }
        if (reserve > top + 1e-9) return Math.max(top, reserve / 2);
        for (int i = RESERVES.length - 1; i >= 0; i--) {
            if (RESERVES[i] < reserve - 1e-9) return RESERVES[i];
        }
        return 0;
    }

    private static void wrapped(Label label, String style) {
        label.setStyle(style);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /** Affiche ou cache un composant ; caché, il ne laisse pas de trou dans la page. */
    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /**
     * Vrai si l'onglet doit être affiché : un automatisme ordinaire ou de matière noire est
     * débloqué, ou la première matière noire a été obtenue (l'onglet ne disparaît alors plus).
     */
    static boolean hasContent(Game game) {
        if (game.isAutomationUnlocked() || game.isDarkMatterUnlocked() || game.isAutoHoldUnlocked()
                || game.isMoleculeAutomationUnlocked()) return true;
        for (DarkAutomation automation : game.darkAutomations()) {
            if (game.isDarkAutomationUnlocked(automation.id())) return true;
        }
        return false;
    }

    /** Recopie l'état des automatismes dans les cartes, et n'affiche que ceux qui sont débloqués. */
    void refresh() {
        BigNum atoms = game.state().atoms();
        balanceLabel.setText(Format.count(atoms) + (atoms.gt(BigNum.ONE) ? " atomes disponibles" : " atome disponible"));

        boolean ordinary = game.isAutomationUnlocked();

        boolean threshold = ordinary && game.isFusionThresholdUnlocked();
        show(thresholdRow, threshold);
        if (threshold) {
            int generators = game.fusionThreshold();
            thresholdLabel.setText("Fusion automatique à " + generators + " générateurs"
                    + Detail.only(" : elle attend d'en avoir autant, et rapporte "
                            + generators / game.generatorsPerAtom() + " fois les atomes, prime de groupe en plus"));
            lessGenerators.setDisable(generators <= game.generatorsPerAtom());
            moreGenerators.setDisable(generators + game.generatorsPerAtom() > game.maxGeneratorCount());
        }

        // La réserve d'atomes : réglable dès que le joueur possède la synthèse automatique.
        boolean reserve = false;
        for (Automation automation : game.automations()) {
            reserve |= automation.kind() == Automation.Kind.SYNTHESIS && game.ownsAutomation(automation.id());
        }
        show(reserveRow, reserve);
        if (reserve) {
            BigNum kept = game.synthesisReserve();
            BigNum needed = game.synthesisCost().add(kept);
            reserveLabel.setText(kept.sign() == 0
                    ? "Réserve de la synthèse : aucune" + Detail.only(". Elle dépense tous les atomes dès qu'elle le peut.")
                    : "Réserve de la synthèse : " + Format.count(kept) + " atomes"
                            + (game.isSynthesisReserveBlocking() ? ", au-dessus du plafond : baissez-la" : "")
                            + Detail.only(". Elle ne synthétise qu'à partir de " + Format.count(needed)
                                    + " atomes : la réserve reste pour vos achats."));
            lessReserve.setDisable(kept.sign() == 0);
            // Sous le plafond d'atomes, une réserve plus grande que lui ne pourrait jamais être respectée.
            moreReserve.setDisable(!game.isAtomCapLifted()
                    && reserveStep(kept.toDouble(), true) > game.atomCap().toDouble());
        }

        boolean anyShown = false;
        boolean someHidden = false;
        for (Automation automation : game.automations()) {
            boolean available = game.isAutomationAvailable(automation.id());
            // Déjà débloqué une fois : il reste affiché, avec ce qu'il faut pour le débloquer de nouveau.
            boolean known = available || game.wasAutomationEverAvailable(automation.id());
            show(rows.get(automation), known);
            if (!known) {
                someHidden = true;
                continue;
            }
            anyShown = true;
            if (available) {
                refreshCard(automation);
                refreshCadence(automation);
            } else {
                showLocked(automation);
            }
        }
        show(ordinaryHint, anyShown && Detail.shown());
        show(balanceLabel, anyShown);
        // Ce que le joueur n'a encore jamais débloqué reste caché ; une ligne annonce la suite.
        ordinaryNext.setText(!ordinary && !anyShown
                ? "Les automatismes ordinaires se débloquent à " + Format.count(Game.UNLOCK_TOTAL_ATOMS)
                        + " atomes créés (vous : " + Format.count(game.state().totalAtoms()) + ")."
                : "Un autre automatisme apparaîtra avec votre premier élément unique ★ du tableau périodique.");
        show(ordinaryNext, someHidden);

        // Les automatismes de matière noire : seulement ceux que la matière noire gagnée a débloqués.
        boolean anyDark = false;
        DarkAutomation next = null;
        for (DarkAutomation automation : game.darkAutomations()) {
            Card card = darkCards.get(automation);
            boolean unlocked = game.isDarkAutomationUnlocked(automation.id());
            darkGrid.show(card, unlocked);
            if (!unlocked) {
                if (next == null) next = automation;
                continue;
            }
            anyDark = true;
            boolean on = game.isDarkAutomationEnabled(automation.id());
            boolean suspended = automation.kind() == DarkAutomation.Kind.GRANT_AUTOMATIONS && game.inChallenge();
            card.show(on && !suspended ? Card.State.ON : Card.State.OFF, "",
                    suspended ? "suspendu" : on ? "en marche" : "coupé",
                    automation.name(), brief(automation),
                    describe(automation) + "." + (suspended ? " Suspendu pendant le défi." : ""),
                    "", "toutes les " + seconds(game.darkAutomationInterval(automation.id())));
        }
        show(darkTitle, anyDark);
        show(darkHint, anyDark && Detail.shown());
        show(darkGrid, anyDark);
        // L'appui automatique, une fois acquis avec l'espace : il attend la matière noire s'il n'y en a pas encore.
        boolean hold = game.isAutoHoldUnlocked();
        boolean creation = game.isMoleculeAutomationUnlocked();
        show(spaceTitle, hold || creation);
        show(spaceGrid, hold || creation);
        spaceTitle.setText(hold && creation ? "Automatismes du Big Bang" : "Automatisme du Big Bang");
        spaceGrid.show(holdCard, hold);
        spaceGrid.show(moleculesCard, creation);
        if (creation) {
            boolean on = game.isMoleculeAutomationEnabled();
            int chosen = game.automatedMolecules();
            boolean waiting = on && chosen == 0;
            moleculesCard.show(on ? Card.State.ON : Card.State.OFF, "", waiting ? "en attente" : on ? "en marche" : "coupé",
                    "Création automatique", chosen == 0 ? "Aucun amas ne lui est confié"
                            : chosen + (chosen > 1 ? " amas confiés" : " amas confié"),
                    "Une création dans chaque amas confié, au même prix qu'à la main : la formule, prise dans le tableau "
                            + "périodique, où il reste toujours un exemplaire de chaque élément. Quand l'espace manque pour "
                            + "tous, le premier servi change à chaque passage. Les amas se confient un à un, d'un clic sur "
                            + "leur carte, dans la sous-page États de la matière du Big Bang. Acquis au deuxième Big Bang, "
                            + "il traverse les explosions et les Big Bangs."
                            + (waiting ? " Il attend qu'un amas lui soit confié." : "")
                            + " Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", "toutes les " + ElementText.number(Game.MOLECULE_AUTOMATION_SECONDS) + " s");
        }
        if (hold) {
            boolean on = game.isAutoHoldEnabled();
            boolean waiting = on && !game.isAutoHolding();
            holdCard.show(on ? Card.State.ON : Card.State.OFF, "", waiting ? "en attente" : on ? "en marche" : "coupé",
                    "Appui automatique", "Tient l'appui sur la matière noire",
                    "La matière noire grossit comme si vous teniez le clic sur son point, à pleine vitesse d'appui et "
                            + "depuis n'importe quel onglet ; avec le verrou de l'arbre, il fournit ce que le verrou laisse. "
                            + "Acquis avec l'espace, il traverse les explosions et les Big Bangs."
                            + (waiting ? " Il attend la première explosion : sans matière noire, rien à faire grossir." : "")
                            + " Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", waiting ? "après la première explosion" : "");
        }
        // La suite n'est annoncée qu'à qui connaît déjà la matière noire.
        boolean announce = next != null && game.isDarkMatterUnlocked();
        show(darkNext, announce);
        if (announce) {
            darkNext.setText((anyDark ? "Prochain automatisme de matière noire à "
                    : "Un automatisme de matière noire apparaîtra à ")
                    + Format.count(next.darkMatter()) + " matières noires gagnées (vous : "
                    + Format.count(game.darkMatterEarned()) + ").");
        }
    }

    /** Ce que fait un automatisme ordinaire, en quelques mots : la ligne de sa carte. */
    private static String brief(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> "Achète dès que possible";
            case FUSION -> "Fusionne dès que possible";
            case SYNTHESIS -> "Synthétise dès que possible";
        };
    }

    /** Ce que fait un automatisme ordinaire, en une phrase : pour le mode détails. */
    private static String describe(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> "Achète « " + automation.name() + " » dès qu'il y a assez de particules.";
            case FUSION -> "Fusionne dès que tous les générateurs sont débloqués.";
            case SYNTHESIS -> "Synthétise un élément dès qu'il y a assez d'atomes, réserve déduite.";
        };
    }

    /** Un automatisme déjà connu mais reverrouillé par une explosion : sa carte dit comment le débloquer. */
    private void showLocked(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        cards.get(automation).show(Card.State.LOCKED, "", "verrouillé", automation.name(), brief(automation),
                describe(automation) + " " + condition(automation) + ".",
                !game.isAutomationUnlocked() ? "À " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés"
                        : needsUnique ? "Avec un élément unique ★" : "", "");
        cadences.get(automation).show(Card.State.LOCKED, "", "", "Cadence", "", "", "", "");
    }

    /** Ce qu'il manque pour qu'un automatisme ordinaire soit de nouveau disponible. */
    private String condition(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        if (!game.isAutomationUnlocked()) {
            return "Se débloque à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés (vous : "
                    + Format.count(game.state().totalAtoms()) + ")"
                    + (needsUnique ? ", avec un élément unique ★" : "");
        }
        return "Se débloque avec un élément unique ★ du tableau périodique"
                + (isGranting() ? ", ou sera offert une fois les autres automatismes à leur cadence maximale" : "");
    }

    /** Vrai si l'automatisme de matière noire qui offre les automatismes ordinaires est en marche. */
    private boolean isGranting() {
        for (DarkAutomation automation : game.darkAutomations()) {
            if (automation.kind() == DarkAutomation.Kind.GRANT_AUTOMATIONS && game.isDarkAutomationEnabled(automation.id())) {
                return true;
            }
        }
        return false;
    }

    private void refreshCard(Automation automation) {
        Card card = cards.get(automation);
        String id = automation.id();
        if (!game.ownsAutomation(id)) {
            card.show(game.canBuyAutomation(id) ? Card.State.READY : Card.State.WAITING, "", "",
                    automation.name(), brief(automation),
                    describe(automation) + " S'achète une fois, puis s'active ou se coupe d'un clic.",
                    Format.count(automation.cost()) + (automation.cost().gt(BigNum.ONE) ? " atomes" : " atome"),
                    remaining(automation.cost()));
        } else {
            boolean on = game.isAutomationEnabled(id);
            card.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    automation.name(), brief(automation),
                    describe(automation) + " Un clic " + (on ? "le coupe." : "le remet en marche."), "", "");
        }
    }

    private void refreshCadence(Automation automation) {
        Card cadence = cadences.get(automation);
        String id = automation.id();
        int speedLevel = game.automationSpeedLevel(id);
        String level = speedLevel + "/" + automation.maxSpeedLevel();
        String delay = "Toutes les " + seconds(game.automationInterval(id));
        if (!game.ownsAutomation(id)) {
            // Pas encore acheté : sa cadence se lit, mais ne s'achète pas.
            cadence.show(Card.State.LOCKED, level, "", delay, "Après l'achat de l'automatisme", "", "", "");
        } else if (game.isAutomationMaxed(id)) {
            boolean floor = game.automationInterval(id) <= game.minAutomationInterval();
            cadence.show(Card.State.DONE, level, "max", delay, floor ? "Délai le plus court possible" : "Cadence maximale",
                    floor ? "" : "Les éléments du tableau périodique réduisent encore ce délai.", "", "");
        } else {
            BigNum cost = game.automationSpeedCost(id);
            double ratio = automation.intervalAt(speedLevel) / automation.intervalAt(speedLevel + 1);
            cadence.show(game.canSpeedUpAutomation(id) ? Card.State.READY
                            : speedLevel > 0 ? Card.State.STARTED : Card.State.WAITING,
                    level, "", delay, "Niveau suivant : délai ÷" + ElementText.number(ratio),
                    "Le délai entre deux actions. Chaque niveau le réduit, jusqu'au maximum de la cadence ; "
                            + "au-delà, seuls les éléments l'accélèrent.",
                    Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome"), remaining(cost));
        }
    }

    /** Ce qu'il reste à attendre avant de pouvoir payer {@code cost} atomes : un nombre de fusions, ou rien. */
    private String remaining(BigNum cost) {
        long fusions = game.fusionsUntilAtoms(cost);
        if (fusions <= 0) return "";
        return Format.whole(fusions) + (fusions > 1 ? " fusions" : " fusion");
    }

    /** Ce que fait un automatisme de matière noire, en quelques mots : la ligne de sa carte. */
    private static String brief(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> "Offre les automatismes au départ";
            case ATOM_UPGRADES -> "Achète les améliorations en atomes";
            case AUTOMATIONS -> "Achète automatismes et cadences";
            case EXPLOSION -> "Fait exploser le tableau";
        };
    }

    /** Ce que fait un automatisme de matière noire, en une phrase. */
    private static String describe(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> "Offre les automatismes ordinaires au début de chaque partie, sans attendre "
                    + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés ni payer. La synthèse automatique suit, une fois "
                    + "les autres à leur cadence maximale et le tableau périodique ouvert, sans attendre un élément unique";
            case ATOM_UPGRADES -> "Achète les améliorations en atomes, la moins chère d'abord";
            case AUTOMATIONS -> "Achète les automatismes ordinaires et leur cadence, le moins cher d'abord";
            case EXPLOSION -> "Fait exploser le tableau périodique dès que c'est possible";
        };
    }

    /** 4.0 → « 4 s », 0.5 → « 0.5 s », 1.333 → « 1.33 s ». */
    static String seconds(double value) {
        return ElementText.number(value) + " s";
    }
}
