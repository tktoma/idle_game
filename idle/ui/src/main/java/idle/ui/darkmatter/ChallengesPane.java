package idle.ui.darkmatter;

import idle.core.Game;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.ui.Confirm;
import idle.ui.GameApp;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Sous-page « Défis » de l'onglet Matière noire : une {@link Card} par défi d'explosion.
 *
 * <p>La carte dit où en est le défi (verrouillé, à tenter, en cours, réussi avec son meilleur
 * temps), sa contrainte et sa récompense en quelques mots ; les phrases complètes viennent en
 * mode détails ({@link Detail}). Un clic sur la carte commence le défi, ou l'abandonne s'il est
 * en cours. Les deux effacent la partie : la carte passe alors au rouge et demande un second
 * clic dans les cinq secondes.
 */
public final class ChallengesPane extends VBox {

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

        Labels.note(intro, 760);
        getChildren().add(intro);

        // La fuite (version 0.115) prend place au milieu de la grille, au centre de sa deuxième rangée : elle n'y
        // apparaît qu'une fois les dix autres réussis, dans le deuxième univers.
        java.util.List<Challenge> order = new java.util.ArrayList<>(Challenges.DEFAULT);
        order.add(Math.min(LEAK_SLOT, order.size()), Challenges.LEAK);
        for (Challenge challenge : order) {
            Card card = new Card(GameApp.DARK_MATTER_COLOR);
            card.setDoneClickable(true);     // un défi réussi se rejoue, pour le temps
            card.setOnAction(() -> click(challenge));
            cards.put(challenge, card);
            grid.add(card);
        }
        grid.show(cards.get(Challenges.LEAK), false);
        grid.setMaxWidth(820);
        getChildren().add(grid);
    }

    /**
     * Premier clic : la carte s'arme. Second clic dans les cinq secondes : le défi commence, ou est
     * abandonné. Sans la confirmation ({@link Confirm#challenge()}), le premier clic suffit.
     */
    private void click(Challenge challenge) {
        if ((armed == challenge && armedFor > 0) || !Confirm.challenge()) {
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
    public void frame(double elapsed) {
        if (reveal >= 0) animateReveal(Math.min(Math.max(0, elapsed), 0.1));
        if (armedFor <= 0) return;
        armedFor = Math.max(0, armedFor - elapsed);
        if (armedFor == 0) armed = null;
    }

    /** La place de la fuite dans la grille : la cinquième, au centre de la deuxième rangée de trois. */
    static final int LEAK_SLOT = 4;
    /** L'arrivée de la fuite (version 0.115) : les défis s'écartent, puis elle apparaît au milieu. */
    public static final double MAKE_ROOM_SECONDS = 0.7, REVEAL_SECONDS = 2.4;
    /** Âge de l'arrivée, en secondes ; négatif hors animation. */
    private double reveal = -1;

    /** Joue l'arrivée de la fuite parmi les défis. */
    public void playReveal() {
        reveal = 0;
        grid.show(cards.get(Challenges.LEAK), false);
        animateReveal(0);
    }

    /** Vrai tant que la fuite arrive : la vue reste sur les défis. */
    public boolean isRevealing() {
        return reveal >= 0;
    }

    private void animateReveal(double dt) {
        reveal += dt;
        Card leak = cards.get(Challenges.LEAK);
        java.util.List<Card> after = new java.util.ArrayList<>(), before = new java.util.ArrayList<>();
        int index = 0;
        for (Map.Entry<Challenge, Card> entry : cards.entrySet()) {
            if (entry.getKey() == Challenges.LEAK) continue;
            (index++ < LEAK_SLOT ? before : after).add(entry.getValue());
        }
        if (reveal < MAKE_ROOM_SECONDS) {
            // Les défis s'écartent : ceux d'avant reculent un peu, ceux d'après glissent vers la droite pour faire la place.
            double t = ease(reveal / MAKE_ROOM_SECONDS);
            for (Card card : before) { card.setTranslateX(-14 * t); card.setOpacity(1 - 0.35 * t); }
            for (Card card : after) { card.setTranslateX(70 * t); card.setOpacity(1 - 0.35 * t); }
            return;
        }
        grid.show(leak, true);
        if (reveal < REVEAL_SECONDS) {
            // Les autres reviennent à leur place, la fuite grandit au milieu, dépasse un peu, puis se pose.
            double t = Math.min(1, (reveal - MAKE_ROOM_SECONDS) / 0.8);
            double back = 1 - ease(t);
            for (Card card : before) { card.setTranslateX(-14 * back); card.setOpacity(1 - 0.35 * back); }
            for (Card card : after) { card.setTranslateX(-90 * back); card.setOpacity(1 - 0.35 * back); }
            double grow = Math.min(1, (reveal - MAKE_ROOM_SECONDS) / 0.9);
            double scale = grow < 0.7 ? 0.2 + 0.95 * grow / 0.7 : 1.15 - 0.15 * (grow - 0.7) / 0.3;
            double glow = Math.max(0, 1 - (reveal - MAKE_ROOM_SECONDS) / (REVEAL_SECONDS - MAKE_ROOM_SECONDS));
            leak.setScaleX(scale + 0.03 * glow * Math.sin(reveal * 9));
            leak.setScaleY(scale + 0.03 * glow * Math.sin(reveal * 9));
            leak.setOpacity(Math.min(1, grow * 1.5));
            return;
        }
        for (Card card : before) { card.setTranslateX(0); card.setOpacity(1); }
        for (Card card : after) { card.setTranslateX(0); card.setOpacity(1); }
        leak.setScaleX(1);
        leak.setScaleY(1);
        leak.setOpacity(1);
        reveal = -1;
    }

    private static double ease(double t) {
        double x = Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }

    /** La sous-page n'est plus affichée : une confirmation en attente est oubliée. */
    void release() {
        armed = null;
        armedFor = 0;
    }

    /** La carte d'un défi : pour les vérifications. */
    public Card card(Challenge challenge) {
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

        // La fuite ne se montre que dans le deuxième univers, les dix autres réussis ; pendant son arrivée, c'est l'animation qui décide.
        if (reveal < 0) grid.show(cards.get(Challenges.LEAK), game.isLeakChallengeShown());
        cards.forEach((challenge, card) -> {
            boolean unlocked = game.isChallengeUnlocked(challenge.id());
            boolean done = game.isChallengeCompleted(challenge.id());
            boolean running = active != null && active.id().equals(challenge.id());
            boolean confirming = armed == challenge && armedFor > 0;
            double best = game.challengeBestTime(challenge.id());

            String line = shortRule(challenge) + "\nGain : " + shortReward(challenge);
            String detail = "Contrainte : " + rule(challenge) + "\nRécompense : " + reward(challenge)
                    + (done ? " (acquise)." : ".") + paths(challenge);
            String record = done && best >= 0 ? "Record " + Format.duration(best) : "";
            if (confirming) {
                card.show(Card.State.ARMED, "", (int) Math.ceil(armedFor) + " s", challenge.name(), line, detail,
                        running ? "Cliquer encore : abandonner" : "Cliquer encore : la partie sera effacée", "");
            } else if (running) {
                card.show(Card.State.ON, "", challenge == Challenges.LEAK ? "fuite " + ElementText.percent(game.leakLossPerSecond()) + "/s" : "en cours",
                        challenge.name(), line, detail,
                        "Depuis " + Format.duration(game.stats().runTime()) + (challenge == Challenges.LEAK
                                && game.stats().runTime() < Game.LEAK_MIN_SECONDS ? " (explosion possible à " + (int) (Game.LEAK_MIN_SECONDS / 60) + " min)" : ""), "abandonner");
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

    /** Pour les défis qui jouent sur l'arbre (version 0.070) : les voies prises en ce moment. */
    private String paths(Challenge challenge) {
        if (challenge.rule() != Challenge.Rule.PRESENCE_ONLY && challenge.rule() != Challenge.Rule.ABSENCE_ONLY) return "";
        return "\nVoies prises : " + game.darkPathsOwned(Game.Style.PRESENCE) + " de présence, "
                + game.darkPathsOwned(Game.Style.ABSENCE) + " d'absence.";
    }

    /** La contrainte d'un défi, en quelques mots : la ligne de sa carte. */
    public static String shortRule(Challenge challenge) {
        return switch (challenge.rule()) {
            case RUSTY_AUTOMATIONS -> "Automatismes " + ElementText.number(Game.RUSTY_FACTOR) + " fois plus lents";
            case HEAVY_FUSION -> Game.HEAVY_FUSION_FACTOR + " fois plus de générateurs par fusion";
            case VOLATILE_PARTICLES -> ElementText.percent(Game.VOLATILE_LOSS_PER_SECOND) + " des particules perdues par seconde";
            case AMNESIA -> "La fusion reprend les achats en atomes";
            case INERT_ELEMENTS -> "Les éléments ne donnent aucun bonus";
            case DECAY -> "Les éléments se désintègrent";
            case PRESENCE_ONLY -> "Seules les voies de présence agissent, tableau lourd";
            case ABSENCE_ONLY -> "Seules les voies d'absence agissent, tableau lourd";
            case CUT_BRANCH -> "La branche des particules n'agit plus";
            case BARE_TREE -> "Ni voie ni liaison n'agit";
            case LEAK -> "Les particules fuient de plus en plus vite sans achat";
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
            case STRONGER_PRESENCE -> "voies de présence ×2";
            case STRONGER_ABSENCE -> "voies d'absence ×2";
            case STRONGER_LINKS -> "liaisons ×2";
            case EXTRA_DARK_MATTER -> "+1 matière noire par explosion";
            case BIG_BANG_CALM -> "le calme du Big Bang";
        };
    }

    /** La contrainte d'un défi, en une phrase. */
    public static String rule(Challenge challenge) {
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
            case PRESENCE_ONLY -> "de tout l'arbre, seules agissent les voies de présence (à gauche de chaque bifurcation) et leurs "
                    + "accords, et le tableau pèse " + Format.count(Game.STYLE_CHALLENGE_WEIGHT) + " fois plus. Avec les voies de présence, "
                    + "il se finit en quelques minutes ; sans, il demande une demi-heure ou plus.";
            case ABSENCE_ONLY -> "de tout l'arbre, seules agissent les voies d'absence (à droite de chaque bifurcation) et leurs "
                    + "accords, et le tableau pèse " + Format.count(Game.STYLE_CHALLENGE_WEIGHT) + " fois plus. Avec les voies d'absence, "
                    + "il se finit en quelques minutes ; sans, il demande une demi-heure ou plus.";
            case CUT_BRANCH -> "les cases de la branche des particules n'agissent plus. Restent les atomes, la taille et les liaisons.";
            case BARE_TREE -> "aucune voie de bifurcation, aucune liaison ni aucun accord n'agit : il ne reste que le tronc de l'arbre.";
            case LEAK -> "les particules fuient d'une part qui grandit de " + ElementText.percent(Game.LEAK_STEP)
                    + " par seconde à chaque seconde sans amélioration achetée (jusqu'à " + ElementText.percent(Game.LEAK_MAX)
                    + " par seconde). Chaque amélioration achetée soi-même l'arrête et la fait repartir de zéro ; celles des "
                    + "automatismes, non. Le tableau ne peut pas exploser avant " + (int) (Game.LEAK_MIN_SECONDS / 60) + " minutes de défi.";
        };
    }

    /** La récompense d'un défi, en une phrase. */
    public static String reward(Challenge challenge) {
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
            case STRONGER_PRESENCE -> "les voies de présence comptent double";
            case STRONGER_ABSENCE -> "les voies d'absence comptent double, et ce qu'elles rendent du temps d'absence aussi";
            case STRONGER_LINKS -> "les liaisons et les accords comptent double";
            case EXTRA_DARK_MATTER -> "une matière noire de plus à chaque explosion";
            case BIG_BANG_CALM -> "le calme du Big Bang : tant qu'aucune amélioration n'est achetée, ni par vous ni par les "
                    + "automatismes, les particules qu'il demande baissent peu à peu, jusqu'à une seule au bout d'une heure ; "
                    + "pour en profiter, coupez les automatismes";
        };
    }
}
