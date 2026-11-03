package idle.ui.pages;

import idle.core.Game;
import idle.core.GameStats;
import idle.ui.common.Format;
import idle.ui.universe.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * La chronologie de la partie (version 0.168) : la durée de chaque univers formé et de ses cinq phases, en barres à la
 * même échelle pour que le joueur voie sa courbe ; puis le temps de chaque dimension (ouverte, jouée, premier
 * automatisme, seuil, palier final) et sa part de la partie, à côté des paliers de la graine et du défi final.
 * Recomposée deux fois par seconde au plus, et seulement quand la sous-page est à l'écran.
 */
final class TimelinePane extends VBox {

    static final double WIDTH = 640;
    /** La largeur de la barre de l'univers le plus long. */
    static final double BAR_WIDTH = 460;
    /** Les derniers univers montrés : au-delà, les plus anciens ne s'affichent plus. */
    static final int SHOWN_UNIVERSES = 40;
    /** Les couleurs des cinq phases d'un univers, dans l'ordre de {@link GameStats.UniverseRecord#PHASES}. */
    static final List<String> PHASE_COLORS = List.of("#9fd0ff", "#c9a2ff", "#ffd27f", "#9be7a8", "#f0a8c0");
    private static final long EVERY_NANOS = 500_000_000L;

    private final Game game;
    private final Label universesEmpty = new Label("Aucun univers formé depuis la version 0.168 : le premier qui se forme s'inscrit ici, "
            + "avec la durée de ses cinq phases.");
    private final HBox legend = new HBox(10);
    private final VBox universes = new VBox(4);
    private final Label current = new Label();
    private final VBox dimensions = new VBox(6);
    private final Label seed = new Label();
    private final Label finale = new Label();
    private long writtenAt = 0;
    /** Le texte des lignes au dernier dessin : pour les vérifications. */
    private final List<String> shown = new ArrayList<>();

    TimelinePane(Game game) {
        super(8);
        this.game = game;
        setMaxWidth(WIDTH);
        for (Label label : List.of(universesEmpty, current, seed, finale)) {
            label.setStyle(StatsPageBase.NAME_STYLE);
            label.setWrapText(true);
            label.setMaxWidth(WIDTH);
        }
        legend.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < PHASE_COLORS.size(); i++) {
            Region swatch = new Region();
            swatch.setMinSize(10, 10);
            swatch.setMaxSize(10, 10);
            swatch.setStyle("-fx-background-color: " + PHASE_COLORS.get(i) + ";");
            Label name = new Label(shortPhase(i));
            name.setStyle(StatsPageBase.NAME_STYLE);
            HBox item = new HBox(4, swatch, name);
            item.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(item);
        }
        getChildren().addAll(header("Les univers formés"), legend, universesEmpty, universes, current, header("Les dimensions"), dimensions,
                header("La graine"), seed, header("Le défi final"), finale);
    }

    private static Label header(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 0 0; -fx-text-fill: #ffd27f;");
        return label;
    }

    /** « acte 1 », « galaxie »… : le nom court d'une phase, pour la légende. */
    static String shortPhase(int i) {
        return List.of("acte 1", "acte 2", "Big Bangs et astres", "galaxie", "amas").get(i);
    }

    /** « 87 h 23 min », « 42 min 05 s » : un instant de la partie, en temps de jeu, sans passer aux jours. */
    static String at(double seconds) {
        if (Double.isNaN(seconds)) return "avant la version 0.168";
        long total = (long) Math.floor(Math.max(0, seconds));
        long hours = total / 3_600, minutes = total % 3_600 / 60;
        if (hours == 0) return Format.duration(seconds);
        return hours + " h " + (minutes < 10 ? "0" : "") + minutes + " min";
    }

    /** « 6 h 06 min » : une durée, NaN écrit « inconnue ». */
    static String span(double seconds) {
        return Double.isFinite(seconds) && seconds >= 0 ? at(seconds) : "inconnue";
    }

    /** « 4,1 % » : une part, avec une décimale. */
    static String share(double part) {
        return String.format(Locale.FRANCE, "%.1f %%", 100 * part);
    }

    /** Recompose la sous-page, deux fois par seconde au plus. */
    void refresh() {
        long now = System.nanoTime();
        if (writtenAt != 0 && now - writtenAt < EVERY_NANOS) return;
        writtenAt = now;
        shown.clear();
        GameStats stats = game.state().stats();
        double played = game.state().timePlayed();

        universes.getChildren().clear();
        List<GameStats.UniverseRecord> log = stats.universeLog();
        List<GameStats.UniverseRecord> last = log.subList(Math.max(0, log.size() - SHOWN_UNIVERSES), log.size());
        universesEmpty.setVisible(log.isEmpty());
        universesEmpty.setManaged(log.isEmpty());
        double longest = 0;
        for (GameStats.UniverseRecord r : last) if (Double.isFinite(r.duration())) longest = Math.max(longest, r.duration());
        for (GameStats.UniverseRecord r : last) {
            String title = "Univers " + r.number() + " — " + span(r.duration());
            universes.getChildren().add(universeRow(r, title, longest));
            shown.add(title + " : " + phases(r));
        }
        current.setText(currentText(stats, played));
        current.setVisible(!current.getText().isEmpty());
        current.setManaged(current.isVisible());
        if (current.isVisible()) shown.add(current.getText());

        dimensions.getChildren().clear();
        for (int d = 1; d <= Math.min(Game.MAX_DIMENSIONS, game.dimensionsUnlocked()); d++) {
            VBox box = new VBox(1);
            String title = "Dimension " + d + " : " + Dimension.values()[d - 1].label().toLowerCase(Locale.FRANCE);
            String detail = dimensionText(stats, d, played);
            box.getChildren().add(line(title, StatsPageBase.VALUE_STYLE));
            box.getChildren().add(line(detail, StatsPageBase.NAME_STYLE));
            dimensions.getChildren().add(box);
            shown.add(title + " : " + detail);
        }
        if (game.dimensionsUnlocked() == 0) {
            dimensions.getChildren().add(line("Aucune dimension ouverte : la première s'ouvre avec le premier univers formé.", StatsPageBase.NAME_STYLE));
        }

        seed.setText(seedText(stats));
        shown.add(seed.getText());
        finale.setText(finaleText(stats, played));
        shown.add(finale.getText());
    }

    /** « acte 1 1 h 18 min · acte 2 2 h 00 min · … » : les cinq phases d'un univers. */
    static String phases(GameStats.UniverseRecord r) {
        double[] p = r.phases();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) text.append("  ·  ");
            text.append(shortPhase(i)).append(' ').append(span(p[i]));
        }
        return text.toString();
    }

    /** Une ligne d'univers : son titre, sa barre en cinq couleurs à l'échelle du plus long, ses phases au survol. */
    private static VBox universeRow(GameStats.UniverseRecord r, String title, double longest) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        double[] p = r.phases();
        for (int i = 0; i < p.length; i++) {
            if (!(p[i] > 0) || !(longest > 0)) continue;
            Region piece = new Region();
            double width = Math.max(1, BAR_WIDTH * p[i] / longest);
            piece.setMinSize(width, 12);
            piece.setMaxSize(width, 12);
            piece.setStyle("-fx-background-color: " + PHASE_COLORS.get(i) + ";");
            Tooltip.install(piece, new Tooltip(GameStats.UniverseRecord.PHASES.get(i) + " : " + span(p[i])));
            bar.getChildren().add(piece);
        }
        Label name = line(title, StatsPageBase.VALUE_STYLE);
        Tooltip.install(name, new Tooltip(phases(r)));
        return new VBox(2, name, bar);
    }

    /** L'univers en cours : depuis quand, et la dernière étape atteinte ; vide avant le premier lancement. */
    static String currentText(GameStats stats, double played) {
        double start = stats.universeStart();
        if (Double.isNaN(start)) return "";
        String step = "";
        for (GameStats.Step s : List.of(GameStats.Step.FIRST_EXPLOSION, GameStats.Step.FIRST_BIG_BANG, GameStats.Step.GALAXY,
                GameStats.Step.CLUSTER)) {
            if (stats.reachedThisUniverse(s)) step = s.label().toLowerCase(Locale.FRANCE) + " à " + span(stats.reachedThisUniverseAt(s) - start);
        }
        return "Univers en cours : " + span(played - start) + " depuis son lancement" + (step.isEmpty() ? "" : ", " + step);
    }

    /**
     * « Ouverte à 48 h 41 min · jouée 12 min · premier automatisme à 80 h 46 min · seuil à 81 h 02 min · palier final à
     * 87 h 08 min (6 h 06 min après le seuil) · 38 h 27 min de la partie, 44,0 % ».
     */
    static String dimensionText(GameStats stats, int d, double played) {
        String key = "dimension." + d + ".";
        StringBuilder text = new StringBuilder();
        double open = stats.moment(key + "ouverte");
        text.append("Ouverte à ").append(at(open));
        double play = stats.dimensionPlayTime(d);
        text.append("  ·  jouée ").append(play > 0 ? span(play) : "jamais, fenêtre fermée");
        if (stats.momentKnown(key + "automatisme")) text.append("  ·  premier automatisme à ").append(at(stats.moment(key + "automatisme")));
        double threshold = stats.moment(key + "seuil");
        if (stats.momentKnown(key + "seuil")) text.append("  ·  seuil à ").append(at(threshold));
        else text.append("  ·  seuil pas encore atteint");
        double fin = stats.moment(key + "final");
        if (stats.momentKnown(key + "final")) {
            text.append("  ·  palier final à ").append(at(fin));
            if (Double.isFinite(fin) && Double.isFinite(threshold)) text.append(" (").append(span(fin - threshold)).append(" après le seuil)");
        }
        double end = stats.momentKnown(key + "final") ? fin : played;
        if (Double.isFinite(open) && Double.isFinite(end) && played > 0) {
            text.append("  ·  ").append(span(end - open)).append(stats.momentKnown(key + "final") ? " de l'ouverture au palier final, " : " ouverte, ")
                    .append(share((end - open) / played)).append(" de la partie");
        }
        return text.toString();
    }

    /** « Paliers : 1 à 26 h 09 min, 2 à 48 h 41 min… » : les paliers de la graine. */
    static String seedText(GameStats stats) {
        StringBuilder text = new StringBuilder();
        for (int stage = 1; stage <= Game.SEED_STAGES; stage++) {
            if (!stats.momentKnown("graine." + stage)) continue;
            text.append(text.length() == 0 ? "Paliers : " : ", ").append(stage).append(" à ").append(at(stats.moment("graine." + stage)));
        }
        return text.length() == 0 ? "Aucun palier de la graine atteint." : text.toString();
    }

    /** « Ouvert à 83 h 49 min, fini à 87 h 23 min : 3 h 34 min, 4,1 % de la partie », ou ce qui manque pour l'ouvrir. */
    static String finaleText(GameStats stats, double played) {
        if (!stats.momentKnown("defiFinal.ouvert")) return "Pas encore ouvert : la bulle des racines l'ouvre.";
        double open = stats.moment("defiFinal.ouvert");
        StringBuilder text = new StringBuilder("Ouvert à ").append(at(open));
        double end = stats.momentKnown("fin") ? stats.moment("fin") : played;
        if (stats.momentKnown("fin")) text.append(", fini à ").append(at(end));
        if (Double.isFinite(open) && Double.isFinite(end) && played > 0) {
            text.append(stats.momentKnown("fin") ? " : " : " : en cours depuis ").append(span(end - open)).append(", ")
                    .append(share((end - open) / played)).append(" de la partie");
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
