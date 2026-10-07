package idle.ui;

import idle.core.BangChallenge;
import idle.core.Game;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Les défis de Big Bang, sous les paliers : refaire le chemin jusqu'au Big Bang suivant sous une
 * contrainte, pour une récompense qui reste ({@link BangChallenge}). Une carte par défi.
 *
 * <p>Commencer un défi remet la partie à zéro comme un Big Bang : le premier clic arme la carte,
 * le second, dans les cinq secondes, commence (ou abandonne). Sans la confirmation des défis
 * ({@link Confirm#challenge()}), un clic suffit.
 */
final class BangChallengesPane extends VBox {

    private static final double CONFIRM_SECONDS = 5;

    private final Game game;
    private final Runnable changed;
    private final Label title = new Label("Défis de Big Bang");
    private final Label intro = new Label();
    private final Map<BangChallenge, Card> cards = new EnumMap<>(BangChallenge.class);
    private final TileGrid grid = new TileGrid(250, 3, 8);
    private BangChallenge armed = null;
    private double armedFor = 0;

    /**
     * @param changed appelé après qu'un défi a commencé ou été abandonné, pour recopier la page
     */
    BangChallengesPane(Game game, Runnable changed) {
        super(8);
        this.game = game;
        this.changed = changed;
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(820);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 14 0 0 0; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setTextAlignment(TextAlignment.CENTER);
        intro.setMaxWidth(760);
        for (BangChallenge challenge : game.bangChallenges()) {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setDoneClickable(true);      // un défi réussi se rejoue pour son temps
            card.setOnAction(() -> click(challenge));
            cards.put(challenge, card);
            grid.add(card);
        }
        grid.setMaxWidth(820);
        getChildren().add(title);
        getChildren().add(intro);
        getChildren().add(grid);
    }

    /** La carte d'un défi : pour les vérifications. */
    Card card(BangChallenge challenge) {
        return cards.get(challenge);
    }

    /** La phrase d'en-tête : pour les vérifications. */
    String introText() {
        return intro.getText();
    }

    private void click(BangChallenge challenge) {
        if ((armed == challenge && armedFor > 0) || !Confirm.challenge()) {
            armed = null;
            armedFor = 0;
            if (game.activeBangChallenge() == challenge) game.abandonBangChallenge();
            else game.startBangChallenge(challenge);
            changed.run();
        } else {
            armed = challenge;
            armedFor = CONFIRM_SECONDS;
            refresh();
        }
    }

    /** Fait passer le temps de la confirmation : sans second clic, elle s'annule. */
    void frame(double elapsed) {
        if (armedFor <= 0) return;
        armedFor = Math.max(0, armedFor - elapsed);
        if (armedFor == 0) {
            armed = null;
            refresh();
        }
    }

    /** La sous-page n'est plus affichée : une confirmation en attente est oubliée. */
    void release() {
        armed = null;
        armedFor = 0;
    }

    /** Recopie l'état des défis dans les cartes. Avant le dernier palier, une phrase annonce seulement qu'ils existent. */
    void refresh() {
        boolean unlocked = game.isBangChallengesUnlocked();
        grid.setVisible(unlocked);
        grid.setManaged(unlocked);
        BangChallenge active = game.activeBangChallenge();
        if (!unlocked) {
            intro.setText("Ils s'ouvrent une fois tous les paliers atteints : refaire un Big Bang sous une contrainte, pour une "
                    + "récompense qui reste.");
            return;
        }
        intro.setText(game.completedBangChallenges() + " / " + game.bangChallenges().size() + " réussis. Commencer un défi "
                + "remet la partie à zéro comme un Big Bang, sans en compter un ; le Big Bang suivant le réussit."
                + Detail.only(" Tout ce qu'un Big Bang garde est gardé : l'arbre de matière noire, les défis d'explosion "
                        + "réussis, et tout le troisième acte. La contrainte tient jusqu'au Big Bang, qui compte normalement. "
                        + "Un défi s'abandonne d'un clic sur sa carte : la contrainte tombe, la partie continue, rien n'est "
                        + "gagné. Réussi, il se rejoue pour son temps. Un seul à la fois, et pas pendant un défi d'explosion."));
        for (BangChallenge challenge : game.bangChallenges()) {
            Card card = cards.get(challenge);
            boolean done = game.isBangChallengeCompleted(challenge);
            boolean running = active == challenge;
            boolean arming = armed == challenge && armedFor > 0;
            double best = game.bangChallengeBest(challenge);
            String aside = Double.isNaN(best) ? "" : "meilleur temps " + Format.duration(best);
            String line = challenge.brief() + "\nRécompense : " + challenge.rewardText() + (done ? " (acquise)" : "");
            Card.State state = arming ? Card.State.ARMED : running ? Card.State.ON : done ? Card.State.DONE
                    : game.canStartBangChallenge() ? Card.State.READY : Card.State.WAITING;
            String price = arming ? (running ? "Cliquer encore : abandonner le défi" : "Cliquer encore : tout repart du premier générateur")
                    : running ? "En cours depuis " + Format.duration(game.bangChallengeTime()) + " · cliquer pour abandonner"
                    : active != null ? "Un autre défi est en cours"
                    : game.inChallenge() ? "Pas pendant un défi d'explosion"
                    : done ? "Rejouer pour le temps" : "Commencer";
            card.show(state, "", arming ? (int) Math.ceil(armedFor) + " s" : running ? "en cours" : done ? "réussi" : "",
                    challenge.label(), line, challenge.rule(), price, aside);
        }
    }
}
