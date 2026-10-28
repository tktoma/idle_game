package idle.ui.bigbang;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Body;
import idle.ui.GameApp;
import idle.ui.Readiness;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.MatterText;
import idle.ui.common.TileGrid;
import idle.ui.probes.ProbesPane;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * La sous-page « Astres » de l'onglet Big Bang : les corps du ciel que le joueur forme en cumulant
 * ce qu'il a fait de plus petit ({@link Game#formBody(String)}).
 *
 * <p>En haut, le ciel ({@link SkyView}) : les astres formés, côte à côte, du plus petit au plus
 * grand. En dessous, une section par échelle ({@link Body.Tier}), avec une carte par astre : son
 * dessin, ce qu'il donne, combien de ses conditions sont réunies et ce qui manque en premier. La
 * liste complète de ce qu'il demande n'apparaît qu'en mode détails.
 *
 * <p>Les amas de roches sont montrés d'emblée. Un astre plus gros n'a sa carte que lorsqu'un des
 * astres dont il part est formé : le joueur découvre les échelles l'une après l'autre.
 */
public final class BodiesPane extends VBox {

    /** Hauteur du ciel, en pixels : de quoi montrer les petits astres sur une ligne et les planètes sur une autre. */
    private static final double SKY_HEIGHT = 380;
    private static final double PICTURE_WIDTH = 150;
    private static final double PICTURE_HEIGHT = 64;

    private final Game game;
    private final Runnable changed;
    private final SkyView sky;
    private final Label intro = new Label();
    /** Le geste groupé, une fois acquis : former d'un clic tous les astres prêts. */
    private final javafx.scene.control.Button formAll = new javafx.scene.control.Button();
    private final Map<Body.Tier, Label> tierTitles = new EnumMap<>(Body.Tier.class);
    private final Map<Body.Tier, TileGrid> tierGrids = new EnumMap<>(Body.Tier.class);
    private final Map<Body, Card> cards = new LinkedHashMap<>();
    /** Les astres qui mûrissent : une ligne et un bouton « Accélérer » par place. */
    private final Label maturationTitle = new Label();
    private final VBox maturationBox = new VBox(6);
    private final List<Label> maturingLabels = new java.util.ArrayList<>();
    private final List<javafx.scene.control.Button> hastenButtons = new java.util.ArrayList<>();
    private final List<String> maturingShown = new java.util.ArrayList<>();
    /** Le plan de plusieurs astres (version 0.085) : ses besoins réunis, ce qui manque d'abord. */
    private final VBox planBox = new VBox(4);
    private final Label planTitle = new Label();
    private final Label planMissing = new Label();
    private final Label planDone = new Label();

    /**
     * @param changed ce qu'il faut refaire quand un astre vient d'être formé : rafraîchir toute la page
     */
    BodiesPane(Game game, Runnable changed) {
        super(8);
        this.game = game;
        this.changed = changed;
        this.sky = new SkyView(game);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 0, 0));

        Labels.note(intro, 760);

        CanvasPane skyPane = CanvasPane.filling(sky);
        skyPane.setMinHeight(SKY_HEIGHT);
        skyPane.setPrefHeight(SKY_HEIGHT);
        skyPane.setMaxHeight(SKY_HEIGHT);
        skyPane.setMaxWidth(820);

        VBox list = new VBox(10);
        list.setAlignment(Pos.TOP_CENTER);
        list.setPadding(new Insets(4, 0, 10, 0));
        for (Body.Tier tier : Body.Tier.values()) {
            Label title = new Label();
            title.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-padding: 8 0 0 0;");
            TileGrid grid = new TileGrid(230, 3, 8);
            grid.setMaxWidth(820);
            tierTitles.put(tier, title);
            tierGrids.put(tier, grid);
            list.getChildren().add(title);
            list.getChildren().add(grid);
        }
        // Pas de défilement propre à la liste : toute la page défile d'un bloc (BigBangPage), ciel compris.
        // Avec un défilement sous un ciel de hauteur fixe, une petite fenêtre ne laissait plus de place à la liste.

        formAll.setStyle("-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;"
                + " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;");
        formAll.setFocusTraversable(false);
        formAll.setOnAction(event -> {
            game.formAllBodies();
            Readiness.of(game).compute();
            this.changed.run();
        });
        maturationTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        maturationBox.getChildren().add(maturationTitle);
        maturationBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            int place = i;
            Label label = new Label();
            label.setStyle("-fx-font-size: 13px; -fx-font-family: monospace; -fx-text-fill: #d6e4f0;");
            label.setMinWidth(430);
            javafx.scene.control.Button hasten = new javafx.scene.control.Button();
            hasten.setStyle("-fx-font-size: 12px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;"
                    + " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;");
            hasten.setFocusTraversable(false);
            hasten.setOnAction(event -> {
                if (place < maturingShown.size()) game.hastenMaturation(maturingShown.get(place));
                this.changed.run();
            });
            HBox row = new HBox(10, label, hasten);
            row.setAlignment(Pos.CENTER);
            maturingLabels.add(label);
            hastenButtons.add(hasten);
            maturationBox.getChildren().add(row);
        }
        Labels.centered(planTitle, "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        planTitle.setMaxWidth(760);
        planMissing.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8f4ff;");
        planDone.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f9f86;");
        for (Label label : new Label[] {planMissing, planDone}) {
            label.setWrapText(true);
            label.setMaxWidth(760);
        }
        planBox.setAlignment(Pos.CENTER);
        planBox.setMaxWidth(780);
        planBox.setStyle("-fx-padding: 8 12; -fx-background-color: #10161f; -fx-background-radius: 8; -fx-border-radius: 8;"
                + " -fx-border-color: " + GameApp.BIG_BANG_COLOR + "55;");
        planBox.getChildren().addAll(planTitle, planMissing, planDone);
        getChildren().add(intro);
        getChildren().add(maturationBox);
        getChildren().add(planBox);
        getChildren().add(formAll);
        getChildren().add(skyPane);
        getChildren().add(list);
    }

    /** Le ciel : pour les vérifications. */
    public SkyView sky() {
        return sky;
    }

    /** La carte d'un astre, ou {@code null} si elle n'a pas encore été montrée : pour les vérifications. */
    public Card card(Body body) {
        return cards.get(body);
    }

    /** Fait avancer le ciel d'une image. */
    void frame(double elapsed) {
        sky.frame(elapsed);
    }

    /** Le bouton « Former les astres prêts » : pour les vérifications. */
    public javafx.scene.control.Button formAllButton() {
        return formAll;
    }

    /** Recopie l'état du jeu dans les cartes. */
    public void refresh() {
        refreshMaturation();
        refreshPlan();
        BigBangPage.showBulk(formAll, game.isBulkFormUnlocked(), "Former les astres prêts", game.bodiesReady());
        Map<Body.Tier, int[]> tiers = new EnumMap<>(Body.Tier.class);     // par échelle : cartes montrées, astres formés
        for (Body body : game.bodies()) {
            boolean formed = game.hasBody(body.id());
            // Un amas est montré d'emblée ; un astre plus gros, dès qu'un de ceux dont il part est formé.
            boolean visible = formed || body.bodies().isEmpty();
            for (String smaller : body.bodies()) visible |= game.hasBody(smaller);
            // Les matières exotiques attendent leur palier, au huitième Big Bang.
            if (body.tier() == Body.Tier.EXOTIC && !game.isExoticUnlocked() && !formed) visible = false;
            Card card = visible ? cardFor(body) : cards.get(body);
            if (card != null) tierGrids.get(body.tier()).show(card, visible);
            if (!visible) continue;
            int[] tier = tiers.computeIfAbsent(body.tier(), each -> new int[2]);
            tier[0]++;
            if (formed) tier[1]++;
            boolean ready = game.canFormBody(body.id());
            boolean maturing = game.isBodyMaturing(body.id());
            int met = game.bodyConditionsMet(body.id());
            String gain = BigBangPage.what(body.boost().stat()) + " +" + ElementText.percent(body.boost().perMolecule());
            // L'étoile de la planification (version 0.085) : un clic l'ajoute au plan, ou l'en retire.
            if (game.isBodyPlanningUnlocked() && !formed) {
                card.setPin(() -> {
                    game.togglePlannedBody(body.id());
                    changed.run();
                });
                card.showPin(game.isBodyPlanned(body.id()));
            } else {
                card.setPin(null);
            }
            card.show(maturing ? Card.State.STARTED : formed ? Card.State.DONE : ready ? Card.State.READY : met > 0 ? Card.State.STARTED : Card.State.WAITING,
                    formed ? "" : met + "/" + game.bodyConditions(body.id()), maturing ? "mûrit" : formed ? "formé" : "", body.name(),
                    gain + "\n" + origin(body), Detail.shown() ? needs(body, formed) : "",
                    maturing ? "Mûrit encore " + ProbesPane.minutes(game.maturationLeft(body.id())) + " · bonus au quart en attendant"
                            : formed ? "" : ready ? "Former" : missing(body), "");
        }
        int formedAll = 0;
        for (Body.Tier tier : Body.Tier.values()) {
            int[] counts = tiers.getOrDefault(tier, new int[2]);
            formedAll += counts[1];
            Label title = tierTitles.get(tier);
            title.setText(tier.label() + (counts[1] == 0 ? "" : " : " + counts[1] + (counts[1] > 1 ? " formés" : " formé")));
            title.setVisible(counts[0] > 0);
            title.setManaged(counts[0] > 0);
        }
        intro.setText((formedAll == 0
                ? "Un astre est fait de ses assemblages, avec assez de matière rassemblée et de molécules autour : d'abord un amas de roches."
                : "Un astre est fait de ses assemblages et part d'astres plus petits : amas de roches, comètes, astéroïdes, lunes, "
                        + "planètes, puis étoiles, étoiles mortes et trous noirs.")
                + Detail.only(" Un achat unique. Ses assemblages entrent en lui : dans l'expansion de la matière, leurs blocs "
                        + "se rangent en couronne autour de l'astre, qui prend le milieu. Ils restent formés et gardent "
                        + "leur bonus, et chaque assemblage n'entre que dans un seul astre. Le reste, ce sont des seuils à "
                        + "atteindre, qui servent aussi aux autres astres : les astres plus petits, la matière, qui compte "
                        + "toutes les molécules rassemblées dans un état quelle que soit leur sorte, et les molécules. Un astre augmente une grandeur de 200 % pour un amas, et du double "
                        + "à chaque échelle : 3 200 % pour une planète, 6 400 % pour une étoile, jusqu'à 51 200 % pour le trou noir supermassif. Un astre plus gros n'est montré que lorsqu'un "
                        + "de ceux dont il part est formé."));
    }

    /** La carte d'un astre dans la section de son échelle, construite la première fois qu'on en a besoin, avec son dessin. */
    private Card cardFor(Body body) {
        return cards.computeIfAbsent(body, each -> {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            Canvas canvas = new Canvas(PICTURE_WIDTH, PICTURE_HEIGHT);
            canvas.getGraphicsContext2D().clearRect(0, 0, PICTURE_WIDTH, PICTURE_HEIGHT);
            // Le plus grand rayon qui laisse tenir tout l'astre, queue ou anneaux compris, dans le cadre.
            double r = Math.min((PICTURE_HEIGHT - 6) / (BodyArt.reachUp(body) + BodyArt.reachDown(body)),
                    (PICTURE_WIDTH - 6) / (BodyArt.reachLeft(body) + BodyArt.reachRight(body)));
            r = Math.min(r, 26);
            double x = PICTURE_WIDTH / 2 + (BodyArt.reachLeft(body) - BodyArt.reachRight(body)) * r / 2;
            double y = PICTURE_HEIGHT / 2 + (BodyArt.reachUp(body) - BodyArt.reachDown(body)) * r / 2;
            BodyArt.draw(canvas.getGraphicsContext2D(), body, x, y, r);
            HBox picture = new HBox(canvas);
            picture.setAlignment(Pos.CENTER);
            picture.setMouseTransparent(true);     // le clic va à la carte
            card.setPicture(picture);
            card.setOnAction(() -> {
                game.formBody(body.id());
                changed.run();
            });
            tierGrids.get(body.tier()).add(card);
            return card;
        });
    }

    /** Le plan de plusieurs astres : caché tant qu'il n'est pas ouvert. */
    private void refreshPlan() {
        boolean open = game.isBodyPlanningUnlocked();
        planBox.setVisible(open);
        planBox.setManaged(open);
        if (!open) return;
        List<Body> planned = game.plannedBodies();
        if (planned.isEmpty()) {
            planTitle.setText("Planifier plusieurs astres : l'étoile ☆ d'un astre l'ajoute au plan, " + Game.MAX_PLANNED_BODIES + " au plus.");
            planMissing.setText("");
            planDone.setText("");
            return;
        }
        List<String> names = new java.util.ArrayList<>();
        for (Body body : planned) names.add(body.name());
        planTitle.setText("Plan : " + String.join(", ", names) + " (" + planned.size() + "/" + Game.MAX_PLANNED_BODIES + ")");
        StringBuilder missing = new StringBuilder(), done = new StringBuilder();
        int doneCount = 0;
        for (Game.PlanNeed need : game.plannedNeeds()) {
            String text = needText(need);
            if (need.satisfied()) {
                doneCount++;
                if (done.length() > 0) done.append('\n');
                done.append("● ").append(text);
            } else {
                if (missing.length() > 0) missing.append('\n');
                missing.append("○ ").append(text);
            }
        }
        planMissing.setText(missing.length() == 0 ? "Tout est réuni : il n'y a plus qu'à former." : "À réunir :\n" + missing);
        planDone.setText(doneCount == 0 ? "" : "Déjà réuni (" + doneCount + ") :\n" + done);
    }

    /** Une exigence du plan, en une ligne ; « en même temps » quand plusieurs astres du plan la demandent. */
    private String needText(Game.PlanNeed need) {
        String what = switch (need.kind()) {
            case BODY -> game.body(need.id()).name() + " formé et mûr";
            case ASSEMBLY -> "Assemblage : " + game.assembly(need.id()).name();
            case MOLECULE -> game.molecule(need.id()).formula() + " : " + Format.count(BigNum.of(need.have())) + " / "
                    + Format.count(BigNum.of(need.needed())) + (need.gather() && !need.gathered() ? ", à rassembler" : "");
            case MATTER -> matterName(Molecule.State.valueOf(need.id())) + " : " + Format.count(BigNum.of(need.have()))
                    + " / " + Format.count(BigNum.of(need.needed()));
        };
        if (!need.shared()) return what;
        return what + (need.kind() == Game.PlanNeed.Kind.MOLECULE && need.gather()
                ? " · pour " + need.bodies().size() + " astres, leurs assemblages s'ajoutent"
                : " · en même temps pour " + need.bodies().size() + " astres, compté une fois");
    }

    /** Le texte du plan : pour les vérifications. */
    public String planText() {
        return planTitle.getText() + "\n" + planMissing.getText() + "\n" + planDone.getText();
    }

    /** De quoi un astre est fait, en une ligne : « Fait de roche sableuse » ou « Fait de glace de comète et eau de source ». */
    private String origin(Body body) {
        if (body.assemblies().isEmpty()) return "Fait de matière rassemblée, sans assemblage";
        StringBuilder text = new StringBuilder("Fait de ");
        List<String> parts = body.assemblies();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) text.append(index == parts.size() - 1 ? " et " : ", ");
            text.append(game.assembly(parts.get(index)).name().toLowerCase());
        }
        return text.toString();
    }

    /** Ce qui manque en premier à un astre, dans l'ordre de ses conditions : astres, assemblages, matière, molécules. */
    private String missing(Body body) {
        for (String smaller : body.bodies()) {
            if (!game.hasBody(smaller)) return "Il faut former : " + game.body(smaller).name().toLowerCase();
            if (game.isBodyMaturing(smaller)) {
                return "Il faut que " + article(game.body(smaller).name()) + " mûrisse : " + ProbesPane.minutes(game.maturationLeft(smaller));
            }
        }
        for (String assembly : body.assemblies()) {
            if (!game.hasAssembly(assembly)) return "Il faut assembler : " + game.assembly(assembly).name().toLowerCase();
        }
        for (Map.Entry<Molecule.State, Integer> matter : game.bodyMatter(body).entrySet()) {
            int lacking = (int) Math.max(0, matter.getValue() - game.gatheredInState(matter.getKey()));
            if (lacking > 0) return "Il manque " + lacking + " " + matterName(matter.getKey());
        }
        for (Map.Entry<String, Integer> molecule : game.bodyMolecules(body).entrySet()) {
            int lacking = molecule.getValue() - game.moleculeCount(molecule.getKey());
            if (lacking > 0) return "Il manque " + lacking + " " + game.molecule(molecule.getKey()).formula();
        }
        if (game.needsColliderEnergy(body) && !game.hasColliderEnergyFor(body)) {
            return "Il faut " + Format.count(BigNum.of(game.colliderEnergyNeeded(body))) + " d'énergie produite au collisionneur";
        }
        if (!game.hasMaturationPlace()) return "Les " + game.maturationPlaces() + " places de maturation sont prises";
        return "";
    }

    /** Tout ce que demande un astre, pour le mode détails : un rond plein devant ce qui est réuni, vide devant le reste. */
    private String needs(Body body, boolean formed) {
        StringBuilder text = new StringBuilder();
        for (String smaller : body.bodies()) {
            line(text, formed || game.isBodyMature(smaller), game.body(smaller).name() + (game.isBodyMaturing(smaller) ? " (mûrit)" : ""));
        }
        for (String assembly : body.assemblies()) {
            line(text, formed || game.hasAssembly(assembly), game.assembly(assembly).name());
        }
        for (Map.Entry<Molecule.State, Integer> matter : game.bodyMatter(body).entrySet()) {
            long have = game.gatheredInState(matter.getKey());
            line(text, formed || have >= matter.getValue(), Format.count(BigNum.of(matter.getValue())) + " " + matterName(matter.getKey())
                    + (formed || have >= matter.getValue() ? "" : " (" + Format.count(BigNum.of(have)) + ")"));
        }
        for (Map.Entry<String, Integer> molecule : game.bodyMolecules(body).entrySet()) {
            int have = game.moleculeCount(molecule.getKey());
            line(text, formed || have >= molecule.getValue(), Format.count(BigNum.of(molecule.getValue())) + " " + game.molecule(molecule.getKey()).formula()
                    + (formed || have >= molecule.getValue() ? "" : " (" + Format.count(BigNum.of(have)) + ")"));
        }
        if (game.needsColliderEnergy(body)) {
            boolean enough = formed || game.hasColliderEnergyFor(body);
            line(text, enough, "Énergie produite au collisionneur : " + Format.count(BigNum.of(game.colliderEnergyNeeded(body)))
                    + (enough ? "" : " (" + Format.count(BigNum.of(game.colliderEnergyProduced())) + ")"));
        }
        return text.toString();
    }

    /** Les astres qui mûrissent : une ligne chacun, avec ce qu'il reste et le bouton qui avance la maturation d'une charge d'impulsion. */
    private void refreshMaturation() {
        List<String> maturing = game.maturingBodies();
        maturingShown.clear();
        maturingShown.addAll(maturing);
        boolean shown = game.bodiesFormed() > 0;
        maturationBox.setVisible(shown);
        maturationBox.setManaged(shown);
        if (!shown) return;
        int charges = (int) Math.floor(game.state().pulseCharges() + 1e-9);
        maturationTitle.setText("Maturation : " + maturing.size() + "/" + game.maturationPlaces() + " places"
                + (maturing.isEmpty() ? " · un astre formé mûrit avant de compter pour les plus gros et pour la galaxie" : ""));
        for (int i = 0; i < maturingLabels.size(); i++) {
            HBox row = (HBox) maturationBox.getChildren().get(i + 1);
            boolean used = i < maturing.size();
            row.setVisible(used);
            row.setManaged(used);
            if (!used) continue;
            String id = maturing.get(i);
            Body body = game.body(id);
            double left = game.maturationLeft(id), total = game.maturationSeconds(body);
            maturingLabels.get(i).setText(body.name() + " : encore " + ProbesPane.minutes(left) + "  " + ProbesPane.bar(1 - left / total));
            javafx.scene.control.Button hasten = hastenButtons.get(i);
            hasten.setVisible(game.isPulseUnlocked());
            hasten.setText("Accélérer : 1 impulsion = " + idle.ui.common.ElementText.number(Game.MATURATION_HASTE_SECONDS) + " s (" + charges + ")");
            hasten.setDisable(charges < 1);
        }
    }

    /** « l'amas rocheux », « la comète glacée » : le nom avec son article, en minuscules. */
    private static String article(String name) {
        if (name.equals("Soleil")) return "le Soleil";
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if ("aeiouéèêhâ".indexOf(lower.charAt(0)) >= 0) return "l'" + lower;
        boolean feminine = lower.startsWith("comète") || lower.startsWith("lune") || lower.startsWith("planète")
                || lower.startsWith("étoile") || lower.startsWith("naine") || lower.startsWith("géante") || lower.startsWith("ceinture");
        return (feminine ? "la " : "le ") + lower;
    }

    private static void line(StringBuilder text, boolean met, String what) {
        if (text.length() > 0) text.append('\n');
        text.append(met ? "● " : "○ ").append(what);
    }

    /** Un état, comme on compte sa matière : « gaz rassemblés », « cristaux rassemblés ». */
    private static String matterName(Molecule.State matter) {
        return MatterText.plural(matter) + " rassemblés";
    }
}
