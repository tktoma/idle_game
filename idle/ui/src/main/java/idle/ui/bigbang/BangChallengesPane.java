package idle.ui.bigbang;

import idle.core.Game;
import idle.core.bigbang.BangChallenge;
import idle.ui.Confirm;
import idle.ui.GameApp;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Les défis de Big Bang, sous les paliers : refaire le chemin jusqu'au Big Bang suivant sous une
 * contrainte, pour une récompense qui reste ({@link BangChallenge}). Une carte par défi.
 *
 * <p>Commencer un défi remet la partie à zéro comme un Big Bang : le premier clic arme la carte,
 * le second, dans les cinq secondes, commence (ou abandonne). Sans la confirmation des défis
 * ({@link Confirm#challenge()}), un clic suffit.
 */
public final class BangChallengesPane extends VBox {

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
        Labels.note(intro, 760);
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
    public Card card(BangChallenge challenge) {
        return cards.get(challenge);
    }

    /** La phrase d'en-tête : pour les vérifications. */
    public String introText() {
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
            intro.setText("Ils s'ouvrent au cinquième Big Bang : refaire un Big Bang sous une contrainte, pour une "
                    + "récompense qui reste.");
            return;
        }
        // Version 0.116 : dans le deuxième univers, chaque défi réussi multiplie aussi tout le troisième acte.
        String second = !game.isSecondUniverse() ? "" : " Deuxième univers : chaque défi réussi donne aussi particules ×"
                + ElementText.number(Game.BANG_UNIVERSE_PARTICLES)
                + ", molécules par création ×2, ×4 puis ×8 aux défis 1, 3 et 5 (même prix en éléments, chacune prend sa place)"
                + ", maturation des astres ×" + ElementText.number(Game.BANG_UNIVERSE_MATURATION) + ", vitesse des sondes ×"
                + ElementText.number(Game.BANG_UNIVERSE_PROBE_SPEED) + " et récompenses des sondes ×"
                + ElementText.number(Game.BANG_UNIVERSE_PROBE_REWARDS) + ", qui se multiplient d'un défi à l'autre ; les trois réussis, "
                + "chaque planète peut entrer dans deux systèmes solaires."
                + (game.universeBangChallenges() > 0 ? " En ce moment : particules ×" + ElementText.number(game.universeBangFactor(2))
                        + ", molécules par création ×" + ElementText.number(game.universeMoleculeFactor())
                        + ", maturation ×" + ElementText.number(game.universeBangFactor(Game.BANG_UNIVERSE_MATURATION))
                        + ", vitesse des sondes ×" + ElementText.number(game.universeBangFactor(Game.BANG_UNIVERSE_PROBE_SPEED))
                        + ", récompenses des sondes ×" + ElementText.number(game.universeBangFactor(Game.BANG_UNIVERSE_PROBE_REWARDS)) + (game.planetsTwice() ? ", planètes deux fois." : ".") : "");
        intro.setText(game.completedBangChallenges() + " / " + game.bangChallenges().size() + " réussis." + second + " Commencer un défi "
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
            String line = challenge.brief() + "\nRécompense : " + challenge.rewardText()
                    + (game.isSecondUniverse() ? ", et tout le troisième acte plus vite" : "") + (done ? " (acquise)" : "");
            Card.State state = arming ? Card.State.ARMED : running ? Card.State.ON : done ? Card.State.DONE
                    : game.canStartBangChallenge(challenge) ? Card.State.READY : Card.State.WAITING;
            String price = arming ? (running ? "Cliquer encore : abandonner le défi" : "Cliquer encore : tout repart du premier générateur")
                    : running ? "En cours depuis " + Format.duration(game.bangChallengeTime()) + " · cliquer pour abandonner"
                    : active != null ? "Un autre défi est en cours"
                    : game.inChallenge() ? "Pas pendant un défi d'explosion"
                    : !game.canStartBangChallenge(challenge) && game.canStartBangChallenge() ? "Il faut d'abord le ciel du Big Bang suivant"
                    : done ? "Rejouer pour le temps" : "Commencer";
            card.show(state, "", arming ? (int) Math.ceil(armedFor) + " s" : running ? "en cours" : done ? "réussi" : "",
                    challenge.label(), line, challenge.rule(), price, aside);
        }
    }
}
