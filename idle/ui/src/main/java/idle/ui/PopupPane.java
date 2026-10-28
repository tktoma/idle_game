package idle.ui;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.Trial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Les fenêtres d'explication (version 0.089), par-dessus la fenêtre, une seule fois chacune :
 * <ul>
 *   <li>à l'ouverture d'un onglet, ce qu'est sa ressource et comment elle marche, en deux ou trois phrases ;</li>
 *   <li>dans le deuxième univers, chaque nouveauté des trois actes au moment où elle s'ouvre ;</li>
 *   <li>dans le troisième univers et les suivants, le vide quand il se propose, puis son trou noir
 *       ({@link #voidEntries(Game)}).</li>
 * </ul>
 * Ce qui s'ouvre au même moment se lit dans la même fenêtre. Le jeu continue derrière ; « Compris »
 * la ferme, et la suivante attend son tour. Une partie d'avant cette version ne revoit pas ce
 * qu'elle connaît déjà : au premier relevé, tout ce qui est déjà ouvert est marqué vu sans rien montrer.
 */
final class PopupPane extends Overlay {

    /** Une explication : son identifiant gardé dans la sauvegarde, son titre, son texte, sa couleur et quand elle s'ouvre. */
    record Entry(String id, String group, String heading, String text, String color, BooleanSupplier open) {
    }

    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 22; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #0b0e14; -fx-border-color: #ffffff;";

    private final Game game;
    private final Label group = new Label();
    private final VBox items = new VBox(12);
    private final Button button = new Button("Compris");
    private final VBox card;
    private final List<List<Entry>> waiting = new ArrayList<>();
    private List<Entry> shown = List.of();

    PopupPane(Game game) {
        super("-fx-background-color: #0b0e14b0;");
        this.game = game;
        group.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        button.setOnAction(event -> close());
        card = new VBox(14, group, items, button);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(520);
        card.setMaxHeight(USE_PREF_SIZE);
        mount(card);
    }

    /** Vrai tant qu'une fenêtre est affichée. */
    boolean isOpen() {
        return isVisible();
    }

    /** Les explications de la fenêtre affichée : pour les vérifications. */
    List<Entry> shown() {
        return shown;
    }

    /** Les fenêtres qui attendent leur tour : pour les vérifications. */
    int waiting() {
        return waiting.size();
    }

    /** Coupées pour les essais automatiques, qui n'ont personne pour cliquer « Compris ». */
    static boolean enabled() {
        return !Boolean.getBoolean("idle.noPopups");
    }

    /**
     * Relève ce qui vient de s'ouvrir parmi {@code entries} et l'annonce. Au tout premier relevé d'une
     * partie déjà commencée avant cette version, ce qui est déjà ouvert est marqué vu, en silence.
     *
     * @param quiet vrai quand une autre fenêtre (absence, fin, explosion) a la main : on attend
     */
    void check(List<Entry> entries, boolean quiet) {
        if (!game.state().popupsPrimed()) {
            if (!game.isStarted()) {
                game.state().setPopupsPrimed(true);
            } else {
                for (Entry entry : entries) if (entry.open().getAsBoolean()) game.state().addSeenPopup(entry.id());
                game.state().setPopupsPrimed(true);
                return;
            }
        }
        // Les nouveautés du même moment, groupe par groupe : une fenêtre par groupe.
        List<Entry> fresh = new ArrayList<>();
        for (Entry entry : entries) {
            if (game.state().seenPopups().contains(entry.id()) || !entry.open().getAsBoolean()) continue;
            game.state().addSeenPopup(entry.id());
            fresh.add(entry);
        }
        if (enabled()) {
            String current = null;
            List<Entry> batch = new ArrayList<>();
            for (Entry entry : fresh) {
                if (!entry.group().equals(current) && !batch.isEmpty()) {
                    waiting.add(batch);
                    batch = new ArrayList<>();
                }
                current = entry.group();
                batch.add(entry);
            }
            if (!batch.isEmpty()) waiting.add(batch);
        }
        if (!isVisible() && !quiet && !waiting.isEmpty()) show(waiting.remove(0));
    }

    /**
     * Annonce un événement de la partie dans une fenêtre (version 0.126) : elle prend son tour comme une explication,
     * mais n'est pas gardée dans la sauvegarde, puisqu'elle ne parle que de ce moment-là.
     */
    void announce(String groupText, String heading, String text, String color) {
        if (!enabled()) return;
        waiting.add(List.of(new Entry("event", groupText, heading, text, color, () -> true)));
    }

    private void show(List<Entry> batch) {
        shown = List.copyOf(batch);
        String color = batch.get(0).color();
        group.setText(batch.get(0).group());
        items.getChildren().clear();
        for (Entry entry : batch) {
            Label heading = new Label(entry.heading());
            heading.setStyle("-fx-font-size: " + (batch.size() == 1 ? 22 : 16) + "px; -fx-font-weight: bold; -fx-text-fill: " + entry.color() + ";");
            Label text = new Label(entry.text());
            text.setWrapText(true);
            text.setTextAlignment(TextAlignment.CENTER);
            text.setStyle("-fx-font-size: 14px; -fx-text-fill: #e8f4ff;");
            VBox item = new VBox(6, heading, text);
            item.setAlignment(Pos.CENTER);
            items.getChildren().add(item);
        }
        card.setStyle(cardStyle(color, true, "22 30"));
        button.setStyle(BUTTON_STYLE + " -fx-background-color: " + color + ";");
        setVisible(true);
    }

    /** Ferme la fenêtre ; la suivante s'ouvre au prochain relevé. */
    void close() {
        setVisible(false);
        shown = List.of();
    }

    /** La partie vient d'être remplacée : les fenêtres en attente parlaient de l'ancienne. */
    void forget() {
        waiting.clear();
        close();
    }

    /** Le texte d'un onglet : sa ressource et comment elle marche, sans entrer dans les détails ; {@code null} sans fenêtre. */
    static String tabText(String tab) {
        return switch (tab) {
            case "PARTICLES" -> "Les particules sont la première ressource. Chaque générateur en crée sans arrêt ; achetez-en d'autres "
                    + "et accélérez-les. Dix générateurs réunis peuvent fusionner : ils deviennent un atome, et la course repart du premier. "
                    + "Ce premier atome prend son temps ; une fois là, la création prend son élan.";
            case "ATOMS" -> "Les atomes naissent des fusions. Ils paient leurs propres améliorations, et dans le tableau périodique "
                    + "la synthèse d'éléments : chaque élément obtenu donne un bonus qui dure.";
            case "AUTOMATION" -> "Ici, le jeu achète et fusionne à votre place. Chaque automatisme s'allume ou se coupe d'un clic, "
                    + "et l'ordre de la liste décide de ce qui passe en premier.";
            case "DARK_MATTER" -> "Une fois le tableau périodique complet, il peut exploser : la partie repart de zéro, mais "
                    + "l'explosion laisse de la matière noire. Elle se dépense dans un arbre d'améliorations qui reste d'une explosion à l'autre.";
            case "COLLIDER" -> "Deux faisceaux tournent dans l'anneau et se croisent au détecteur. Chaque croisement produit de "
                    + "l'énergie ; un clic au bon moment en donne bien plus, et certaines collisions libèrent des quarks.";
            case "BIG_BANG" -> "Le Big Bang fait repartir la partie du premier générateur. En échange, l'espace s'ouvre : il "
                    + "s'étend de lui-même, et s'y forment des molécules, puis des astres.";
            case "VOID" -> "Le vide n'est jamais tout à fait vide : le collisionneur y fait naître des paires de particules, qui "
                    + "s'annulent au bout de quelques secondes. Chacune coûte de son énergie et tient de l'espace libre, et donne de "
                    + "l'énergie du vide à sa naissance. Celle-ci fond doucement : elle monte vers un équilibre, affiché. Elle paie "
                    + "les améliorations du vide ; ce qu'il en reste à la formation de l'univers rejoint le cumul.";
            case "UNIVERSE" -> "L'univers est formé : tout ce qui y a mené repart de zéro. Les graines d'univers achètent ici "
                    + "des améliorations qui restent, et « Lancer l'univers » relance une partie pour former l'univers suivant.";
            case "SKINS" -> "Des apparences de rechange pour les visuels du jeu. Chaque skin débloqué ajoute " + Math.round(Skin.BONUS * 100) + "\u00a0% à toutes les "
                    + "ressources, et ces bonus s'additionnent, que le skin soit choisi ou non : le choisir ne change que l'apparence.";
            default -> null;
        };
    }

    /** Les nouveautés du deuxième univers, acte par acte, avec ce qui les ouvre. */
    static List<Entry> secondUniverse(Game game) {
        String one = "Deuxième univers — acte 1", two = "Deuxième univers — acte 2", three = "Deuxième univers — acte 3";
        String blue = GameApp.PARTICLES_COLOR, gold = GameApp.ATOMS_COLOR, violet = GameApp.DARK_MATTER_COLOR, bang = GameApp.BIG_BANG_COLOR;
        String skins = idle.ui.pages.SkinsPage.COLOR;
        BooleanSupplier second = game::isSecondUniverse;
        // Version 0.109 : une nouveauté ne s'annonce que pendant la partie du deuxième univers, et une fois son acte
        // ouvert dans cette partie : l'acte 1 d'emblée, l'acte 2 à la première explosion, l'acte 3 au premier Big Bang.
        // Avant, certaines s'affichaient dès la formation du premier univers, ou en avance sur leur acte.
        BooleanSupplier act1 = game::isReplaying;
        BooleanSupplier act2 = () -> game.isReplaying() && game.isDarkMatterUnlocked();
        BooleanSupplier act3 = () -> game.isReplaying() && game.isBigBangUnlocked();
        return List.of(
                gated(act1, "second:regime", one, "Régime des générateurs", "Un bouton dans l'onglet Particules : +" + Math.round(Game.REGIME_GAIN * 100) + "\u00a0% de cadence "
                        + "pendant " + (int) Game.REGIME_SECONDS + " s, puis " + (int) Game.REGIME_REST_SECONDS + " s de repos. Il se relance toujours à la main.", blue, game::isRegimeUnlocked),
                gated(act1, "second:synergy", one, "Synergie des rangs", "Dès le dixième générateur, chacun produit un peu plus, "
                        + "jusqu'à +" + String.format(java.util.Locale.FRANCE, "%.1f", Game.SYNERGY_MAX * 100) + "\u00a0%, selon son rang.", blue, game::isSynergyUnlocked),
                gated(act1, "second:perfect", one, "Prime de cycle parfait", "Trois fusions de suite sans buter sur le plafond "
                        + "d'atomes : +" + Math.round(Game.PERFECT_GAIN * 100) + "\u00a0% de particules pendant une minute.", gold, game::isPerfectCycleUnlocked),
                gated(act1, "second:free", one, "Éléments gratuits", "Tous les dix éléments découverts, un élément gratuit : dans "
                        + "le tableau périodique, un clic sur un élément pas encore découvert le prend.", gold,
                        () -> second.getAsBoolean() && game.freeElementsAvailable() > 0),
                gated(act1, "second:dense", one, "Recherche sur le noyau dense", "Un million d'atomes créés : quatre paliers payés "
                        + "en atomes, dans l'onglet Skins. Chacun donne un skin d'atome ou de tableau.", skins, game::isDenseResearchUnlocked),
                gated(act2, "second:preference", two, "Famille préférée", "Avec la synthèse ciblée, choisissez une famille dans le "
                        + "tableau : ses chances montent à chaque tirage au hasard, jusqu'à ×" + Math.round(1 + Game.PREFERENCE_STEP * Game.PREFERENCE_MAX_DRAWS) + ".", violet, game::isFamilyPreferenceUnlocked),
                gated(act2, "second:trials", two, "Les épreuves", "Dans l'onglet Skins, des épreuves volontaires : une à la fois, "
                        + "abandonnables sans rien perdre. Chacune réussie donne un skin.", skins,
                        () -> game.isTrialUnlocked(Trial.LONE_GENERATOR) || game.isTrialUnlocked(Trial.SOFT_CONTRACTION)),
                gated(act2, "second:colliderTrials", two, "Les épreuves du collisionneur", "Trois épreuves où il faut capter des "
                        + "croisements sous la contrainte du temps. Le défi du détecteur suit, au Détecteur élargi niveau 3.", skins,
                        () -> game.isTrialUnlocked(Trial.NO_OVERHEAT)),
                gated(act2, "second:leak", two, "La fuite", "Un onzième défi, au milieu des autres : les particules fuient de plus "
                        + "en plus vite tant qu'aucune amélioration n'est achetée, et le tableau ne peut pas exploser avant " + (int) (Game.LEAK_MIN_SECONDS / 60) + " minutes. "
                        + "Réussi, il donne le calme du Big Bang : tant qu'aucune amélioration n'est achetée, même par les "
                        + "automatismes, les particules qu'il demande baissent, jusqu'à une seule au bout d'une heure.", violet,
                        game::isLeakChallengeShown),
                gated(act2, "second:darkTiers", two, "Paliers de matière noire", "La matière noire gagnée dans ce deuxième univers "
                        + "débloque des skins : à 100, 1 000, 10 000 et 100 000.", violet, () -> game.darkSkinTiers() > 0),
                gated(act3, "second:cycle", three, "Le petit arbre du cycle", "Dans l'onglet Big Bang, un choix par cycle : fusion, "
                        + "synthèse ou réserve. Le Big Bang suivant le rend.", bang, game::isCycleChoiceUnlocked),
                gated(act3, "second:coldBang", three, "Big Bang à froid", "Une épreuve : l'expansion va deux fois moins vite jusqu'au "
                        + "Big Bang suivant.", skins, () -> game.isTrialUnlocked(Trial.COLD_BANG)),
                gated(act3, "second:bangTiers", three, "Paliers du Big Bang", "Le premier Big Bang de ce deuxième univers donne un "
                        + "skin, le troisième un autre.", bang, () -> game.bangSkinTiers() > 0));
    }

    /**
     * Le vide (version 0.129) : son offre dans le collisionneur, au premier Big Bang du troisième univers, puis son
     * trou noir, au premier trou noir formé. Une fois chacune, comme les autres explications.
     */
    static List<Entry> voidEntries(Game game) {
        String color = idle.ui.vacuum.VoidPage.COLOR;
        return List.of(
                new Entry("void:offer", "Troisième univers — le vide", "Le vide se propose", "Dans l'onglet Collisionneur, le vide "
                        + "s'ouvre contre dix minutes de production du collisionneur, comptées maintenant. Il y fera naître des paires "
                        + "de particules : elles coûtent de son énergie et de l'espace libre, et donnent l'énergie du vide.", color,
                        game::isVoidOffered),
                new Entry("void:hole", "Troisième univers — le vide", "Le trou noir du vide", "Un trou noir est formé : dans l'onglet "
                        + "du vide, sacrifiez-lui des particules. Sa masse raccourcit la vie des paires, qui rendent leur espace plus "
                        + "vite, et ralentit la fonte de l'énergie du vide.", idle.ui.vacuum.VoidPage.HOLE_COLOR, game::isVoidHoleOpen));
    }

    /** Une nouveauté du deuxième univers, qui attend que son acte soit ouvert (version 0.109). */
    private static Entry gated(BooleanSupplier act, String id, String group, String heading, String text, String color, BooleanSupplier open) {
        return new Entry(id, group, heading, text, color, () -> act.getAsBoolean() && open.getAsBoolean());
    }
}
