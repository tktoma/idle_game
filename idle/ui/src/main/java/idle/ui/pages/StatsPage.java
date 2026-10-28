package idle.ui.pages;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.StatsHistory;
import idle.core.bigbang.BangChallenge;
import idle.core.collider.ColliderAchievement;
import idle.core.collider.Quark;
import idle.core.darkmatter.Challenge;
import idle.core.math.BigNum;
import idle.ui.EventLog;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Statistiques » : tout ce que le jeu compte, rangé en sous-pages, une par
 * ressource. Chaque sous-page n'apparaît qu'avec la ressource dont elle parle :
 * <ul>
 *   <li>« Général » : toujours là ;</li>
 *   <li>« Particules » : dès le premier générateur ;</li>
 *   <li>« Atomes » : dès la première fusion ;</li>
 *   <li>« Automatisation » : dès qu'un automatisme est débloqué ;</li>
 *   <li>« Tableau périodique » : dès que le tableau est ouvert ;</li>
 *   <li>« Matière noire » : dès la première explosion ;</li>
 *   <li>« Big Bang » : dès le premier Big Bang ;</li>
 *   <li>« Collisionneur » : dès la première explosion, avec lui (version 0.063) ;</li>
 *   <li>« Records » : toujours là, acte par acte (version 0.063) ;</li>
 *   <li>« Journal » : toujours là.</li>
 * </ul>
 * Une sous-page débloquée le reste, même si une explosion fait disparaître la ressource. À
 * l'intérieur d'une sous-page, certaines lignes n'apparaissent elles aussi qu'en avançant : le
 * multiplicateur des éléments n'a pas de sens avant le tableau périodique.
 *
 * <p>Chaque sous-page commence par ses courbes : un menu déroulant propose toutes les
 * statistiques de la sous-page qui évoluent avec le temps, et le graphique ({@link ChartPane})
 * trace celle qui est choisie. Les courbes viennent de l'historique que tient le jeu
 * ({@link StatsHistory}), qui relève une valeur par statistique suivie. Une courbe n'est
 * proposée qu'une fois sa statistique apparue dans la partie. Après la première explosion, deux
 * boutons choisissent la période : tout le jeu, ou la partie en cours.
 *
 * <p>Les succès ont leur propre onglet ({@link AchievementsPage}) ; seule leur courbe est ici,
 * dans la vue d'ensemble de « Général ».
 *
 * <p>Sous les courbes, les valeurs du moment, une ligne chacune. Une ligne dont la statistique
 * a une courbe se clique : le menu passe à cette courbe. Ce qui ne se trace pas reste en lignes
 * seulement : les records, les réglages, le détail par famille.
 *
 * <p>Rien n'est calculé ici : les compteurs viennent de {@link GameStats}, les valeurs du moment
 * de {@link Game}. Seule la sous-page affichée est mise à jour.
 *
 * <p>Le code est rangé en parties (version 0.098) : une chaîne de classes abstraites, une par thème, dont
 * celle-ci est le bout ; le reste du programme ne voit que {@code StatsPage}. De la base au bout :
 * {@link StatsPageBase}, {@link StatsPageLines}. Quand une partie a besoin d'une méthode définie plus loin
 * dans la chaîne, elle la déclare abstraite en tête, sous « Ce que fournissent les parties suivantes ».
 */
public final class StatsPage extends StatsPageLines {

    public StatsPage(Game game) {
        super(game);
    }

    // ------------------------------------------------------------------
    // Construction et affichage
    // ------------------------------------------------------------------

    /** Nombre d'annonces du journal affichées : pour les vérifications. */
    public int journalRows() {
        return journalList.getChildren().size();
    }

    /** Le texte d'une ligne du journal, la plus récente d'abord : pour les vérifications. */
    public String journalRow(int index) {
        HBox box = (HBox) journalList.getChildren().get(index);
        return ((Label) box.getChildren().get(0)).getText() + " | " + ((Label) box.getChildren().get(1)).getText();
    }

    /** Ouvre la sous-page du journal : pour les vérifications. */
    public void openJournal() {
        select(pages.get(pages.size() - 1));
        refresh();
    }

    /** Le collisionneur (version 0.063) : son énergie, ses croisements et ses collisions, ses quarks, ses générateurs. */
    void collider() {
        page("Collisionneur", "#6fe3ff", game::isColliderUnlocked, "à la première explosion");
        curves();
        colliderEnergy();
        log("Énergie par seconde", "ce que produisent les faisceaux croisés", ChartPane.GREEN, StatsHistory.Stat.COLLIDER_ENERGY_RATE, () -> true);
        log("Énergie en réserve", "ce que les achats n'ont pas encore dépensé", ChartPane.TEAL, StatsHistory.Stat.COLLIDER_ENERGY, () -> true);
        count("Collisions", "depuis le début", ChartPane.ORANGE, StatsHistory.Stat.COLLISIONS, game::isCollisionsUnlocked);
        count("Quarks en réserve", "toutes saveurs ensemble", ChartPane.MAGENTA, StatsHistory.Stat.QUARKS, game::isCollisionsUnlocked);
        section("Énergie");
        charted("Énergie en réserve", row("Énergie possédée", () -> Format.big(BigNum.of(game.colliderEnergy()))));
        charted("Énergie par seconde", row("Production", () -> Format.big(BigNum.of(game.colliderEnergyPerSecond())) + " par seconde"));
        charted("Énergie produite au collisionneur", row("Produite depuis le début", () -> Format.big(BigNum.of(game.colliderEnergyProduced()))));
        section("Croisements et collisions");
        row("Croisements", () -> Format.whole(game.state().colliderCrossings()));
        row("Un croisement toutes les", () -> ElementText.number(game.colliderPeriod()) + " s");
        row("Chance de collision", () -> ElementText.percent(game.collisionChance()), game::isCollisionsUnlocked);
        charted("Collisions", row("Collisions", () -> Format.whole(game.state().collisionRuns()), game::isCollisionsUnlocked));
        row("Captés à la main de suite", () -> Format.whole(game.catchStreak()), () -> game.catchStreak() > 0);
        section("Quarks");
        for (Quark quark : Quark.values()) {
            row(quark.symbol() + "  " + quark.label(), () -> Format.whole(game.quarks(quark)), () -> game.isQuarkOpen(quark) || game.quarks(quark) > 0);
        }
        charted("Quarks en réserve", row("Tous les quarks", () -> Format.whole(game.totalQuarks()), game::isCollisionsUnlocked));
        row("Quark top en attente", () -> "encore " + Format.wait(game.topLeft()), game::hasTop);
        row("Surchauffe", () -> "particules ×" + (int) Game.OVERHEAT_FACTOR + ", encore " + Format.wait(game.overheatLeft()), () -> game.overheatLeft() > 0);
        section("Générateurs et succès");
        row("Générateurs achetés", () -> Format.whole(game.colliderGeneratorsBoughtTotal()));
        row("Succès du collisionneur", () -> game.colliderAchievements() + " / " + ColliderAchievement.values().length);
    }

    /** Le détail des multiplicateurs (version 0.123) : ce qui fait grandir chaque grandeur, aspect par aspect. */
    void multipliers() {
        page("Multiplicateurs", "#ffd27f", game::isStarted, "en créant le premier générateur");
        Label intro = new Label("D'où vient chaque grandeur : la part de chaque aspect du jeu dans ses ordres de grandeur, "
                + "comme le détail des multiplicateurs d'Antimatter Dimensions.");
        intro.setStyle(NAME_STYLE);
        intro.setWrapText(true);
        intro.setMaxWidth(MAX_WIDTH);
        building.content.getChildren().add(intro);
        multiplierPane = new MultiplierPane(game);
        building.content.getChildren().add(multiplierPane);
        building.updaters.add(multiplierPane::refresh);
        multiplierPage = building;
    }

    // Sans valeur de départ : ils sont posés pendant la construction de la page, appelée par le constructeur de la base.
    private MultiplierPane multiplierPane;
    private SubPage multiplierPage;

    /** Ouvre la sous-page des multiplicateurs : pour les vérifications. */
    public void openMultipliers() {
        select(multiplierPage);
        refresh();
    }

    /** Les lignes du détail affiché, « aspect | valeur | part » : pour les vérifications. */
    public List<String> multiplierLines() {
        return multiplierPane.shownLines();
    }

    /** Choisit la grandeur détaillée : pour les vérifications. */
    public void chooseMultiplier(idle.core.Multipliers.Quantity quantity) {
        multiplierPane.choose(quantity);
    }

    /** Déplie ou replie un aspect : pour les vérifications. */
    public void toggleMultiplier(idle.core.Multipliers.Aspect aspect) {
        multiplierPane.toggle(aspect);
    }

    /** Nombre de lignes du détail, facteurs dépliés compris : pour les vérifications. */
    public int multiplierLineCount() {
        return multiplierPane.lineCount();
    }

    /**
     * Les records, acte par acte (version 0.063). Les temps des étapes traversent la remise à zéro
     * du jeu ; les autres valent depuis le début de la partie.
     */
    void records() {
        page("Records", "#ffd27f", () -> true, "");
        Label note = new Label("Les temps des étapes sont les meilleurs de toutes les parties : la remise à zéro les garde. "
                + "Les autres records valent depuis le début de la partie.");
        note.setStyle(NAME_STYLE);
        note.setWrapText(true);
        note.setMaxWidth(MAX_WIDTH);
        building.content.getChildren().add(note);
        row("Écart sur la prochaine étape", () -> {
            double gap = game.recordGap();
            return gap <= 0 ? Format.duration(-gap) + " d'avance" : Format.duration(gap) + " de retard";
        }, () -> !Double.isNaN(game.recordGap()));

        // Chaque record a sa ligne dès le début, « pas encore » tant qu'il n'est pas tenu : on voit ce qu'il reste à battre.
        section("Premier acte : particules et atomes");
        stepRecord(GameStats.Step.FIRST_FUSION);
        row("Fusion la plus rapide", () -> time(stats().fastestFusionTime()));
        row("Meilleure production", () -> stats().bestProduction().isZero() ? NOT_YET
                : Format.amount(stats().bestProduction()) + " particules par seconde");
        row("Le plus d'atomes en une fusion", () -> stats().bestAtomsPerFusion().isZero() ? NOT_YET : Format.amount(stats().bestAtomsPerFusion()));

        section("Deuxième acte : matière noire");
        stepRecord(GameStats.Step.FIRST_EXPLOSION);
        row("Explosion la plus rapide", () -> time(stats().fastestExplosionTime()));
        for (Challenge challenge : game.challenges()) {
            row("Défi « " + challenge.name() + " »", () -> time(game.state().challengeTimes().getOrDefault(challenge.id(), 0.0)));
        }

        section("Troisième acte : Big Bang");
        for (GameStats.Step step : GameStats.Step.values()) {
            if (step.ordinal() >= GameStats.Step.FIRST_BIG_BANG.ordinal()) stepRecord(step);
        }
        row("Big Bang le plus rapide", () -> time(stats().fastestBigBangTime()));
        row("La plus grosse création de molécules", () -> stats().biggestCreation() > 0 ? Format.whole(stats().biggestCreation()) : NOT_YET);
        for (BangChallenge challenge : game.bangChallenges()) {
            row("Défi « " + challenge.label() + " »", () -> time(game.state().bangChallengeTimes().getOrDefault(challenge.name(), 0.0)));
        }

        section("Collisionneur");
        row("Énergie produite", () -> game.colliderEnergyProduced() > 0 ? Format.big(BigNum.of(game.colliderEnergyProduced())) : NOT_YET);
        row("Collisions", () -> game.state().collisionRuns() > 0 ? Format.whole(game.state().collisionRuns()) : NOT_YET);
        row("Succès du collisionneur", () -> game.colliderAchievements() + " / " + ColliderAchievement.values().length);
    }

    /** Ce qu'affiche un record pas encore tenu. */
    private static final String NOT_YET = "pas encore";

    /** Une durée record, ou « pas encore » (0 : jamais fait). */
    private static String time(double seconds) {
        return seconds > 0 ? Format.duration(seconds) : NOT_YET;
    }

    /** Le record d'une étape, toutes parties confondues, ou « pas encore ». */
    private void stepRecord(GameStats.Step step) {
        row(step.label(), () -> Double.isNaN(game.recordOf(step)) ? NOT_YET : Format.duration(game.recordOf(step)));
    }

    /**
     * Le journal des événements : les dernières annonces du jeu ({@link EventLog}), la plus récente
     * en haut, avec l'heure et le temps de jeu. Il n'est recomposé que lorsqu'une annonce arrive.
     */
    void journal() {
        page("Journal", STATS_COLOR, () -> true, "");
        section("Les " + EventLog.CAPACITY + " dernières annonces");
        journalNote.setStyle(NAME_STYLE);
        journalNote.setWrapText(true);
        journalNote.setMaxWidth(MAX_WIDTH);
        journalList.setMaxWidth(MAX_WIDTH);
        building.content.getChildren().add(journalNote);
        building.content.getChildren().add(journalList);
        building.updaters.add(() -> {
            EventLog log = EventLog.of(game);
            if (log.version() == journalVersion) return;
            journalVersion = log.version();
            journalNote.setText(log.size() == 0
                    ? "Rien pour l'instant. Tout ce que le jeu annonce (palier, succès, explosion, amélioration acquise) "
                            + "viendra se noter ici, même si les notifications sont coupées."
                    : "La plus récente en haut. Le journal dure le temps d'un lancement : il n'est pas sauvegardé.");
            journalList.getChildren().clear();
            for (EventLog.Entry entry : log.entries()) {
                Label when = new Label(entry.clock() + "  ·  " + Format.duration(entry.played()) + " de jeu");
                when.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
                when.setMinWidth(170);
                Label text = new Label(entry.text());
                text.setStyle(NAME_STYLE);
                text.setWrapText(true);
                HBox.setHgrow(text, Priority.ALWAYS);
                HBox line = new HBox(10, when, text);
                line.setStyle(ROW_STYLE);
                line.setMaxWidth(MAX_WIDTH);
                journalList.getChildren().add(line);
            }
        });
    }

    void page(String name, String color, BooleanSupplier unlocked, String howToUnlock) {
        SubPage page = new SubPage(name, color, unlocked, howToUnlock);
        page.content.setAlignment(Pos.TOP_CENTER);
        page.content.setPadding(new Insets(8, 0, 12, 0));
        Labels.transparent(page.scroll);
        page.tab.setOnAction(event -> {
            select(page);
            refresh();
        });
        pages.add(page);
        subTabs.getChildren().add(page.tab);
        stack.getChildren().add(page.scroll);
        building = page;
    }

    void section(String title) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12 0 2 0; -fx-text-fill: " + building.color + ";");
        header.setMaxWidth(MAX_WIDTH);
        building.content.getChildren().add(header);
    }

    /**
     * Ouvre la section des courbes de la sous-page en construction : le menu déroulant, puis la
     * place du graphique choisi. Les courbes s'y ajoutent ensuite avec {@link #curve}, {@link #log}
     * et {@link #count}, dans l'ordre où le menu les proposera ; la première est celle affichée d'abord.
     */
    void curves() {
        section("Évolution");
        SubPage page = building;
        // Le menu prend les couleurs du jeu : un fond sombre, et la couleur de la sous-page pour le choix en cours.
        page.choice.setStyle("-fx-font-size: 13px; -fx-base: #16202e; -fx-background: #16202e;"
                + " -fx-control-inner-background: #121821; -fx-control-inner-background-alt: #121821;"
                + " -fx-accent: " + page.color + "; -fx-focus-color: " + page.color + "; -fx-faint-focus-color: transparent;"
                + " -fx-selection-bar: " + page.color + "; -fx-selection-bar-non-focused: " + page.color + ";");
        page.choice.setPrefWidth(360);
        page.choice.setMaxWidth(MAX_WIDTH);
        page.choice.setVisibleRowCount(14);
        page.choice.setFocusTraversable(false);    // les flèches du clavier ne doivent pas changer de courbe par accident
        page.choice.setOnAction(event -> {
            if (updating) return;
            show(page, page.choice.getValue());
        });
        Label label = new Label("Courbe");
        label.setStyle(NAME_STYLE);
        page.chooser.getChildren().add(label);
        page.chooser.getChildren().add(page.choice);
        page.chooser.setAlignment(Pos.CENTER);
        page.holder.setMaxWidth(MAX_WIDTH);
        page.content.getChildren().add(page.chooser);
        page.content.getChildren().add(page.holder);
    }

    /** Affiche la courbe de ce nom sur une sous-page, si elle en a une ; le haut de la page, où elle est, revient à l'écran. */
    private void show(SubPage page, String name) {
        for (Graph graph : page.graphs) {
            if (!graph.name.equals(name)) continue;
            page.shown = graph;
            page.scroll.setVvalue(0);
            refresh();
            return;
        }
    }

    /**
     * Ajoute une courbe qui suit le temps : ses séries sont lues dans l'historique de la période choisie.
     *
     * @param name      son nom dans le menu déroulant
     * @param available vrai quand elle a quelque chose à montrer ; avant, le menu ne la propose pas
     * @param maker     comment construire son graphique, le jour où elle est choisie
     * @param lines     une ou deux séries, lues dans chaque relevé
     */
    void curve(String name, BooleanSupplier available, Supplier<ChartPane> maker, Line... lines) {
        building.graphs.add(new Graph(name, available, maker, () -> {
            List<StatsHistory.Sample> samples = samples();
            List<ChartPane.Series> series = new ArrayList<>();
            for (Line line : lines) {
                double[] x = new double[samples.size()];
                double[] y = new double[samples.size()];
                for (int i = 0; i < x.length; i++) {
                    x[i] = samples.get(i).time();
                    y[i] = line.value().applyAsDouble(samples.get(i));
                }
                series.add(new ChartPane.Series(line.name(), line.color(), x, y));
            }
            return series;
        }, true));
    }

    /** Une courbe en échelle logarithmique, pour une quantité que l'historique note en puissance de dix. */
    void log(String name, String unit, String color, StatsHistory.Stat stat, BooleanSupplier available) {
        curve(name, available,
                () -> new ChartPane(name, (unit.isEmpty() ? "" : unit + " · ") + "échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, Double.NaN, Double.NaN),
                new Line(name, color, sample -> sample.value(stat)));
    }

    /** Une courbe pour un compteur : des entiers, à partir de zéro. */
    void count(String name, String unit, String color, StatsHistory.Stat stat, BooleanSupplier available) {
        curve(name, available,
                () -> new ChartPane(name, unit, true, Format::clock, value -> Format.whole(Math.round(value)),
                        true, 0, Double.NaN),
                new Line(name, color, sample -> sample.value(stat)));
    }

    Row row(String name, Supplier<String> text) {
        return row(name, text, () -> true);
    }

    Row row(String name, Supplier<String> text, BooleanSupplier visible) {
        Label label = new Label(name);
        label.setStyle(NAME_STYLE);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        Label value = new Label();
        value.setStyle(VALUE_STYLE);
        value.setWrapText(true);
        value.setTextAlignment(TextAlignment.RIGHT);
        HBox box = new HBox(12, label, value);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(ROW_STYLE);
        box.setMaxWidth(MAX_WIDTH);
        Row row = new Row(name, box, value, text, visible);
        building.rows.add(row);
        building.content.getChildren().add(box);
        return row;
    }

    /**
     * Relie une ligne à la courbe de sa statistique : un clic sur la ligne choisit cette courbe
     * dans le menu. La ligne s'éclaire sous la souris pour le faire savoir.
     */
    void charted(String graph, Row row) {
        SubPage page = building;
        Node box = row.box();
        box.setStyle(ROW_STYLE + " -fx-cursor: hand;");
        box.setOnMouseEntered(event -> box.setStyle(ROW_HOVER_STYLE));
        box.setOnMouseExited(event -> box.setStyle(ROW_STYLE + " -fx-cursor: hand;"));
        box.setOnMouseClicked(event -> show(page, graph));
        Tooltip.install(box, new Tooltip("Voir la courbe : " + graph));
    }

    /** Les noms des sous-pages débloquées, dans l'ordre des onglets : pour les vérifications. */
    public List<String> openSubPages() {
        List<String> names = new ArrayList<>();
        for (SubPage page : pages) if (page.unlocked.getAsBoolean()) names.add(page.tab.getText());
        return names;
    }

    /** Ouvre la sous-page de ce nom et la met à jour ; faux si elle n'existe pas ou n'est pas débloquée : pour les vérifications. */
    public boolean openSubPage(String name) {
        for (SubPage page : pages) {
            if (!page.tab.getText().equals(name) || !page.unlocked.getAsBoolean()) continue;
            select(page);
            refresh();
            return true;
        }
        return false;
    }

    /** Les lignes visibles de la sous-page à l'écran, « nom : valeur » : pour les vérifications. */
    public List<String> shownRows() {
        List<String> lines = new ArrayList<>();
        for (Row row : selected.rows) if (row.box().isVisible()) lines.add(row.name() + " : " + row.value().getText());
        return lines;
    }

    /** Les courbes que le menu de la sous-page à l'écran propose : pour les vérifications. */
    public List<String> offeredGraphs() {
        return List.copyOf(selected.offered);
    }

    /** Passe à la sous-page ouverte suivante ou précédente, en boucle. */
    public void step(int direction) {
        int index = pages.indexOf(selected);
        for (int tries = 0; tries < pages.size(); tries++) {
            index = Math.floorMod(index + direction, pages.size());
            if (pages.get(index).unlocked.getAsBoolean()) {
                select(pages.get(index));
                refresh();
                return;
            }
        }
    }

    void select(SubPage page) {
        selected = page;
        for (SubPage each : pages) {
            each.scroll.setVisible(each == page);
            each.tab.setStyle(SubTabs.style(SubTabs.NARROW_STYLE, each == page, each.color));
        }
    }

    private static String rangeStyle(boolean chosen) {
        return "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 12; -fx-border-radius: 12;"
                + (chosen ? " -fx-text-fill: #0b0e14; -fx-background-color: " + STATS_COLOR + "; -fx-border-color: " + STATS_COLOR + ";"
                : " -fx-text-fill: #b9c7d6; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;");
    }

    /**
     * Met à jour le menu déroulant d'une sous-page et trace la courbe choisie. Le menu ne propose
     * que les courbes dont la statistique existe déjà ; si celle qui était affichée disparaît
     * (après une remise à zéro), la première disponible la remplace.
     */
    private void refreshCurves(SubPage page) {
        List<String> names = new ArrayList<>();
        Graph first = null;
        boolean keep = false;
        for (Graph graph : page.graphs) {
            if (!graph.available.getAsBoolean()) continue;
            names.add(graph.name);
            if (first == null) first = graph;
            keep |= graph == page.shown;
        }
        if (!keep) page.shown = first;
        boolean any = page.shown != null;
        page.chooser.setVisible(any);
        page.chooser.setManaged(any);
        page.holder.setVisible(any);
        page.holder.setManaged(any);
        if (!any) return;

        // Remplir le menu ou y reporter le choix déclenche son action : ce n'est pas le joueur qui choisit.
        updating = true;
        if (!names.equals(page.offered)) {
            page.offered = names;
            page.choice.getItems().setAll(names);
        }
        if (!Objects.equals(page.choice.getValue(), page.shown.name)) page.choice.setValue(page.shown.name);
        updating = false;

        Graph graph = page.shown;
        if (graph.pane == null) graph.pane = graph.maker.get();
        if (page.holder.getChildren().size() != 1 || page.holder.getChildren().get(0) != graph.pane) {
            page.holder.getChildren().clear();
            page.holder.getChildren().add(graph.pane);
        }
        graph.pane.show(graph.series.get());
    }

    /** Affiche les sous-pages débloquées et met à jour la courbe et les lignes de celle qui est à l'écran. */
    public void refresh() {
        current = game.stats();   // relève au passage le record de production
        SubPage nextLocked = null;
        for (SubPage page : pages) {
            boolean unlocked = page.unlocked.getAsBoolean();
            page.tab.setVisible(unlocked);
            page.tab.setManaged(unlocked);
            if (!unlocked && nextLocked == null) nextLocked = page;
        }
        // Une remise à zéro peut refermer la sous-page affichée.
        if (!selected.unlocked.getAsBoolean()) select(pages.get(0));

        refreshCurves(selected);
        // Le choix de la période n'a de sens qu'après une explosion, et pour une courbe qui suit le temps.
        boolean range = exploded() && selected.shown != null && selected.shown.timeline;
        rangeRow.setVisible(range);
        rangeRow.setManaged(range);
        wholeButton.setStyle(rangeStyle(!runOnly));
        runButton.setStyle(rangeStyle(runOnly));

        for (Row row : selected.rows) {
            boolean visible = row.visible().getAsBoolean();
            row.box().setVisible(visible);
            row.box().setManaged(visible);
            if (visible) row.value().setText(row.text().get());
        }
        for (Runnable updater : selected.updaters) updater.run();

        boolean more = nextLocked != null;
        nextLabel.setVisible(more);
        nextLabel.setManaged(more);
        if (more) {
            nextLabel.setText("D'autres statistiques apparaîtront avec votre avancée. Prochaines : « "
                    + nextLocked.tab.getText() + " », " + nextLocked.howToUnlock + ".");
        }
    }
}
