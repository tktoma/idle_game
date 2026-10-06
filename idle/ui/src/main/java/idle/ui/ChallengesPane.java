package idle.ui;

import idle.core.Challenge;
import idle.core.Game;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Sous-page « Défis » de l'onglet Matière noire : une {@link Card} par défi d'explosion.
 *
 * <p>La carte dit où en est le défi (verrouillé, à tenter, en cours, réussi avec son meilleur
 * temps), sa contrainte et sa récompense en quelques mots ; les phrases complètes viennent en
 * mode détails ({@link Detail}). Un clic sur la carte commence le défi, ou l'abandonne s'il est
 * en cours. Les deux effacent la partie : la carte passe alors au rouge et demande un second
 * clic dans les cinq secondes.
 */
final class ChallengesPane extends VBox {

    /** Temps laissé pour le second clic, en secondes. */
    private static final double CONFIRM_SECONDS = 5;

    private final Game game;
    private final Label intro = new Label();
    private final Map<Challenge, Card> cards = new LinkedHashMap<>();
    private final TileGrid grid = new TileGrid(230, 3, 8);
    /** Défi dont la carte attend son second clic, ou {@code null}. */
    private Challenge armed = null;
    private double armedFor = 0;

    ChallengesPane(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));

        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setTextAlignment(TextAlignment.CENTER);
        intro.setMaxWidth(760);
        getChildren().add(intro);

        for (Challenge challenge : game.challenges()) {
            Card card = new Card(GameApp.DARK_MATTER_COLOR);
            card.setDoneClickable(true);     // un défi réussi se rejoue, pour le temps
            card.setOnAction(() -> click(challenge));
            cards.put(challenge, card);
            grid.add(card);
        }
        grid.setMaxWidth(820);
        getChildren().add(grid);
    }

    /** Premier clic : la carte s'arme. Second clic dans les cinq secondes : le défi commence, ou est abandonné. */
    private void click(Challenge challenge) {
        if (armed == challenge && armedFor > 0) {
            armed = null;
            armedFor = 0;
            Challenge active = game.activeChallenge();
            if (active != null && active.id().equals(challenge.id())) {
                game.abandonChallenge();
            } else {
                game.startChallenge(challenge.id());
            }
        } else {
            armed = challenge;
            armedFor = CONFIRM_SECONDS;
        }
        refresh();
    }

    /**
     * Fait passer le temps de la confirmation : sans second clic, elle s'annule.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        if (armedFor <= 0) return;
        armedFor = Math.max(0, armedFor - elapsed);
        if (armedFor == 0) armed = null;
    }

    /** La sous-page n'est plus affichée : une confirmation en attente est oubliée. */
    void release() {
        armed = null;
        armedFor = 0;
    }

    /** La carte d'un défi : pour les vérifications. */
    Card card(Challenge challenge) {
        return cards.get(challenge);
    }

    /** Recopie l'état des défis dans les cartes. */
    void refresh() {
        Challenge active = game.activeChallenge();
        intro.setText(game.completedChallenges() + " / " + game.challenges().size() + " réussis. "
                + "Une partie rejouée de zéro avec une contrainte ; l'explosion la réussit."
                + Detail.only(" Un défi se joue sans les mémoires de la matière noire : rien n'est gardé de la partie "
                        + "précédente, rien n'est offert au départ, et le tableau y retrouve sa masse du début. Ce que "
                        + "l'arbre multiplie reste acquis. La première réussite rapporte la matière noire de l'explosion "
                        + "et laisse une récompense définitive ; rejoué, un défi ne compte plus que pour son temps. "
                        + "Tenté dès son ouverture, un défi est souvent plus court qu'une partie ordinaire, dont le "
                        + "tableau s'alourdit à chaque explosion."));

        cards.forEach((challenge, card) -> {
            boolean unlocked = game.isChallengeUnlocked(challenge.id());
            boolean done = game.isChallengeCompleted(challenge.id());
            boolean running = active != null && active.id().equals(challenge.id());
            boolean confirming = armed == challenge && armedFor > 0;
            double best = game.challengeBestTime(challenge.id());

            String line = shortRule(challenge) + "\nGain : " + shortReward(challenge);
            String detail = "Contrainte : " + rule(challenge) + "\nRécompense : " + reward(challenge)
                    + (done ? " (acquise)." : ".");
            String record = done && best >= 0 ? "Record " + Format.duration(best) : "";
            if (confirming) {
                card.show(Card.State.ARMED, "", (int) Math.ceil(armedFor) + " s", challenge.name(), line, detail,
                        running ? "Cliquer encore : abandonner" : "Cliquer encore : la partie sera effacée", "");
            } else if (running) {
                card.show(Card.State.ON, "", "en cours", challenge.name(), line, detail,
                        "Depuis " + Format.duration(game.stats().runTime()), "abandonner");
            } else if (!unlocked) {
                card.show(Card.State.LOCKED, "", "verrouillé", challenge.name(), line, detail,
                        "À " + challenge.darkMatter() + " matières noires gagnées", "");
            } else if (active != null || !game.isStarted()) {
                // Rien à cliquer : un autre défi est en cours, ou la partie n'a pas commencé.
                card.show(done ? Card.State.STARTED : Card.State.WAITING, "", done ? "réussi" : "", challenge.name(), line,
                        detail, active != null ? record : "Commencez d'abord une partie", "");
            } else if (done) {
                card.show(Card.State.DONE, "", "réussi", challenge.name(), line,
                        detail + " Rejoué, il ne rapporte plus de matière noire.", record, "rejouer");
            } else {
                card.show(Card.State.READY, "", "à tenter", challenge.name(), line, detail, "Commencer", "");
            }
        });
    }

    /** La contrainte d'un défi, en quelques mots : la ligne de sa carte. */
    static String shortRule(Challenge challenge) {
        return switch (challenge.rule()) {
            case RUSTY_AUTOMATIONS -> "Automatismes " + ElementText.number(Game.RUSTY_FACTOR) + " fois plus lents";
            case HEAVY_FUSION -> Game.HEAVY_FUSION_FACTOR + " fois plus de générateurs par fusion";
            case VOLATILE_PARTICLES -> ElementText.percent(Game.VOLATILE_LOSS_PER_SECOND) + " des particules perdues par seconde";
            case AMNESIA -> "La fusion reprend les achats en atomes";
            case INERT_ELEMENTS -> "Les éléments ne donnent aucun bonus";
            case DECAY -> "Les éléments se désintègrent";
        };
    }

    /** La récompense d'un défi, en quelques mots. */
    static String shortReward(Challenge challenge) {
        return switch (challenge.reward()) {
            case FASTER_AUTOMATIONS -> "automatismes ÷" + ElementText.number(Game.REWARD_AUTOMATION_DIVISOR);
            case GROUP_BONUS -> "prime de groupe à " + ElementText.percent(Game.REWARD_GROUP_BONUS);
            case STRONGER_MILESTONES -> "paliers de vitesse ×3";
            case CHEAPER_ATOM_UPGRADES -> "achats en atomes ÷" + ElementText.number(Game.REWARD_ATOM_UPGRADE_DIVISOR);
            case STRONGER_ELEMENTS -> "éléments +" + ElementText.percent(Game.REWARD_ELEMENT_STRENGTH - 1);
            case EXTRA_ELEMENT -> "+" + Game.REWARD_EXTRA_ELEMENTS + " élément par synthèse";
        };
    }

    /** La contrainte d'un défi, en une phrase. */
    static String rule(Challenge challenge) {
        return switch (challenge.rule()) {
            case RUSTY_AUTOMATIONS -> "tous les automatismes, ordinaires et de matière noire, sont "
                    + ElementText.number(Game.RUSTY_FACTOR) + " fois plus lents, et jamais plus rapides qu'une action toutes les "
                    + ElementText.number(Game.RUSTY_MIN_INTERVAL) + " s. Rien n'empêche d'acheter et de fusionner à la main.";
            case HEAVY_FUSION -> "il faut " + Game.HEAVY_FUSION_FACTOR + " fois plus de générateurs pour fusionner, "
                    + "vingt au lieu de dix, pour le même nombre d'atomes.";
            case VOLATILE_PARTICLES -> "les particules s'évaporent : " + ElementText.percent(Game.VOLATILE_LOSS_PER_SECOND)
                    + " de celles que vous possédez disparaissent chaque seconde. Rien ne s'économise.";
            case AMNESIA -> "chaque fusion remet aussi à zéro les améliorations payées en atomes, Persistance comprise. "
                    + "Les automatismes, eux, restent : mieux vaut y mettre ses atomes, et dans la synthèse.";
            case INERT_ELEMENTS -> "les éléments et leurs ensembles ne donnent aucun bonus. Le tableau reste à remplir.";
            case DECAY -> "les éléments se désintègrent : chaque exemplaire a une demi-vie de "
                    + Format.duration(Math.round(Math.log(2) / Game.DECAY_RATE)) + ". Il faut synthétiser plus vite qu'ils ne disparaissent.";
        };
    }

    /** La récompense d'un défi, en une phrase. */
    static String reward(Challenge challenge) {
        return switch (challenge.reward()) {
            case FASTER_AUTOMATIONS -> "délai des automatismes ordinaires ÷" + ElementText.number(Game.REWARD_AUTOMATION_DIVISOR);
            case GROUP_BONUS -> "la prime de groupe de la fusion passe de " + ElementText.percent(Game.FUSION_GROUP_BONUS)
                    + " à " + ElementText.percent(Game.REWARD_GROUP_BONUS) + " par groupe de générateurs";
            case STRONGER_MILESTONES -> "chaque palier de vitesse multiplie les particules par "
                    + ElementText.number(Game.REWARD_MILESTONE_EXTRA) + " de plus (×3 au lieu de ×2)";
            case CHEAPER_ATOM_UPGRADES -> "améliorations payées en atomes " + ElementText.number(Game.REWARD_ATOM_UPGRADE_DIVISOR)
                    + " fois moins chères";
            case STRONGER_ELEMENTS -> "tous les éléments agissent " + ElementText.percent(Game.REWARD_ELEMENT_STRENGTH - 1)
                    + " plus fort";
            case EXTRA_ELEMENT -> "+" + Game.REWARD_EXTRA_ELEMENTS + " élément à chaque synthèse";
        };
    }
}
