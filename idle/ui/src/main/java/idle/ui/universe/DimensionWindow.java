package idle.ui.universe;

import idle.core.Game;
import idle.core.collider.Hadron;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * La fenêtre d'une dimension : elle couvre tout le jeu, avec son visuel, son nom et une phrase, et un bouton « Retour »
 * pour revenir au jeu. Le jeu continue de tourner derrière.
 *
 * <p>La première, la ligne, a sa logique depuis la version 0.101 : ses éclats de dimension, son seuil et ses cinq
 * améliorations sous le visuel. Le plan a la sienne depuis la version 0.104, et le volume depuis la version 0.128 : on
 * plonge dans le cube, et ses améliorations s'achètent ici, une fois ressorti. La durée a la sienne depuis la version
 * 0.134 : on tient le bouton du milieu, les spirales avancent, on relâche. La boucle a la sienne depuis la version
 * 0.153 : on clique chaque cercle (ou sa touche) quand son point passe dans une zone ; ses six améliorations, son
 * métronome et le rappel des touches sont sous la vue. La brane a la sienne depuis la version 0.154 : on appuie la
 * membrane, on glisse pour la tendre, on relâche, et l'onde qui part rebondit sur ses bords ; ses cinq améliorations
 * sont sous la vue. Le clicker a la sienne depuis la version 0.155 : on clique la surface, d'autant plus vite que
 * chaque clic rapporte davantage, et dans les zones ×2 qui s'y superposent ; ses huit améliorations sont sous la vue.
 * La grille a la sienne depuis la version 0.156 : on choisit une couche du cube (boutons, molette ou flèches haut et
 * bas) et l'on sonde ses cases ; ce que la grille montre du point le plus proche suit l'amélioration « Affichage du
 * point », jamais plus ; refaite en 0.157 : mille cases par côté au départ, on vise une case (un clic sur la carte, ou
 * ses coordonnées) et on l'essaie, un essai par point caché. Le miroir a la sienne depuis la version 0.157 : un motif
 * s'allume à gauche, on le refait de mémoire, en miroir, à droite ; ses cinq améliorations sont sous la vue.
 *
 * <p>Le mélange a la sienne depuis la version 0.158, refaite en 0.159 : l'arène en grand, où se mêlent étape par étape
 * les mécaniques des autres dimensions ; au-dessus, l'étape, sa barre de progression et « Mêler la dimension suivante »
 * ; dessous, les fragments, le malus, les multiplicateurs, la charge, la cadence, ce qu'on fait à l'étape en cours, et
 * ses onze améliorations, grisées tant que leur étape n'est pas atteinte. Au clavier, Z Q S D et les flèches mènent le
 * point, A et E le font remonter ou plonger, la barre d'espace tient la charge.
 *
 * <p>Depuis la version 0.150, le jeu peut jouer seul le plan, le volume et la durée (l'automatisation des dimensions) :
 * une mention « automatique », à droite du titre, le dit tant que l'automatisme de jeu de la dimension tourne (pour la
 * ligne, qui avance seule, tant qu'un de ses achats automatiques tourne), et les explications disent ce qu'il fait.
 * Fermer la fenêtre n'arrête alors ni la plongée ni l'appui : ils continuent derrière. Le rythme automatique de la
 * boucle, la tirée automatique de la brane, les clics automatiques du clicker (version 0.155), les essais automatiques
 * de la grille et le miroir automatique jouent de même, fenêtre ouverte ou non.
 */
public final class DimensionWindow extends DimensionWindowTexts {
    public DimensionWindow(Game game, BooleanSupplier freeCamera, Consumer<Boolean> setFreeCamera) {
        super(game, freeCamera, setFreeCamera);
    }

    DimensionWindow(Game game, boolean[] free) {
        super(game, free);
    }

    public DimensionWindow(Game game) {
        super(game);
    }

    /** La vue de la fenêtre si elle montre la dimension {@code dimension}, {@code null} sinon. */
    DimensionView shown(Dimension dimension) { return view.dimension() == dimension ? view : null; }

    /**
     * Une touche dans le mélange : Z Q S D et les flèches mènent le point (dès le plan), A le fait
     * remonter et E plonger (dès le volume), la barre d'espace tient la charge (dès la durée). Vrai si elle a servi.
     */
    boolean mixKey(KeyCode code, boolean down) {
        if (!game.isMixOpen()) return false;
        switch (code) {
            case Z, Q, S, D, UP, DOWN, LEFT, RIGHT -> {
                if (!game.isMixing(2)) return false;
                if (down) held.add(code);
                else held.remove(code);
                double[] direction = direction();
                game.setMixDirection(direction[0], direction[1]);
                return true;
            }
            case A, E -> {
                if (!game.isMixing(3)) return false;
                if (down) view.shiftMixDepth(code == KeyCode.E ? 1 : -1);
                return true;
            }
            case SPACE -> {
                if (!game.isMixing(4)) return false;
                if (down && !spaceHeld) {
                    spaceHeld = true;
                    game.holdMix(true);
                } else if (!down && spaceHeld) {
                    spaceHeld = false;
                    game.holdMix(false);
                }
                return true;
            }
            default -> { return false; }
        }
    }

    /** Le cercle qui porte cette touche, ou −1. */
    int loopRingOf(KeyCode code) {
        for (int ring = 0; ring < Game.MAX_RINGS; ring++) if (code != null && code == loopSettings.loopKey(ring)) return ring;
        return -1;
    }

    void steer() {
        double[] direction = direction();
        game.setPlaneInput(direction[0], direction[1]);
    }

    /** La direction des touches tenues : x vers la droite, y vers le bas ; (0, 0) sans touche. */
    double[] direction() {
        double dx = 0, dy = 0;
        if (held.contains(KeyCode.Q) || held.contains(KeyCode.LEFT)) dx -= 1;
        if (held.contains(KeyCode.D) || held.contains(KeyCode.RIGHT)) dx += 1;
        if (held.contains(KeyCode.Z) || held.contains(KeyCode.UP)) dy -= 1;
        if (held.contains(KeyCode.S) || held.contains(KeyCode.DOWN)) dy += 1;
        return new double[] {dx, dy};
    }

    /** Une amélioration de la ligne : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.LineUpgrade upgrade) {
        return switch (upgrade) {
            case YIELD -> "Densité (niveau " + game.lineLevel(upgrade) + ") : " + shardText(game.lineShardsPerTouch())
                    + " → " + shardText(game.lineShardsPerTouch() + Game.LINE_SHARD * game.hadronBoost(Hadron.OMEGA)) + " segment par trait touché";
            case TICKS -> "Graduation (" + game.lineTicks() + " traits) : un trait de plus, tous à égale distance";
            case SPEED -> "Élan (vitesse ×" + shardText(game.lineSpeed()) + ") : les boules vont " + percent(Game.LINE_SPEED_STEP) + " plus vite";
            case BALLS -> "Boules (" + game.lineBalls() + " par ligne) : une boule de plus sur chaque ligne";
            case LINES -> "Lignes (" + game.lineCount() + ") : une ligne de plus, avec ses traits et ses boules";
        };
    }

    /** Ouvre la fenêtre de cette dimension. */
    public void open(Dimension dimension) {
        leaveMix();
        view.show(dimension);
        // Le miroir rejoue son motif à l'entrée, s'il attend encore sa reproduction : on l'entend avant de le refaire.
        if (dimension == Dimension.MIRROR) game.showMirrorPattern();
        view.setFreeCamera(freeCamera.getAsBoolean());
        title.setText("Dimension " + dimension.number() + " : " + dimension.label());
        held.clear();
        loopKeysHeld.clear();
        game.setPlaneInput(0, 0);
        line.setText(switch (dimension) {
            case LINE -> dimension.line() + " Chaque trait que touche la boule donne des segments.";
            case PLANE -> dimension.line() + " Chaque éclat ramassé donne des facettes, propres à cette dimension.";
            case VOLUME -> dimension.line() + " Plongez dans le cube : chaque boule cueillie de son centre donne des sphères.";
            case LOOP -> dimension.line() + (game.isLoopOpen()
                    ? " Cliquez chaque cercle quand son point passe dans une zone : plus le clic est juste, plus il donne de nœuds."
                    : " Il n'y a rien à y faire pour l'instant.");
            case MIX -> dimension.line() + (game.isMixOpen()
                    ? " Un seul mini-jeu, qui mêle étape par étape les mécaniques des autres dimensions : chaque étape gagnée y mêle la suivante."
                    : " Il n'y a rien à y faire pour l'instant.");
            case MIRROR -> dimension.line() + (game.isMirrorOpen()
                    ? " Retenez le motif qui s'allume à gauche, puis refaites-le en miroir, à droite : chaque motif réussi donne des reflets."
                    : " Il n'y a rien à y faire pour l'instant.");
            case GRID -> dimension.line() + (game.isGridOpen()
                    ? " Sondez ses cases, couche par couche : plus une case est près d'un point caché, plus elle donne de repères."
                    : " Il n'y a rien à y faire pour l'instant.");
            case CLICKER -> dimension.line() + (game.isClickerOpen()
                    ? " Cliquez la surface, et vite : plus la cadence est haute, plus chaque clic donne d'étincelles."
                    : " Il n'y a rien à y faire pour l'instant.");
            case BRANE -> dimension.line() + (game.isBraneOpen()
                    ? " Tirez la membrane et relâchez-la : chaque point que touche l'onde, ou l'un de ses rebonds, donne des ondes."
                    : " Il n'y a rien à y faire pour l'instant.");
            default -> dimension.line() + (dimension == Dimension.DURATION && game.isDurationOpen()
                    ? " Tenez le bouton du milieu : plus les spirales vont loin vers le centre, plus elles donnent d'instants."
                    : " Il n'y a rien à y faire pour l'instant.");
        });
        setVisible(true);
        refresh();
    }

    /**
     * Referme la fenêtre : retour au jeu. Une plongée en cours s'arrête là, le joueur ressort du cube, sauf si la
     * plongée automatique la mène : elle continue sans la fenêtre.
     */
    public void close() {
        // La durée : fermer la fenêtre relâche le bouton, qui donne ce qu'il avait ; l'appui automatique, lui, continue.
        view.releaseHold();
        // La brane : la membrane tenue se lâche sans onde.
        view.cancelBrane();
        leaveMix();
        held.clear();
        loopKeysHeld.clear();
        game.setPlaneInput(0, 0);
        if (game.isInVolume() && !game.isDimensionAutoPlayed(3)) game.leaveVolume();
        view.resetDive();
        setVisible(false);
    }

    /**
     * Le mélange lâche ce que la fenêtre tenait : la charge (versée, comme au relâchement) et la direction du point. Rien
     * si le mélange n'est pas montré.
     */
    void leaveMix() {
        if (view.dimension() == Dimension.MIX && game.isMixOpen()) {
            view.releaseMix(true);
            if (spaceHeld) game.holdMix(false);
            game.setMixDirection(0, 0);
        }
        spaceHeld = false;
    }

    public boolean isOpen() { return isVisible(); }

    /** La dimension montrée. */
    public Dimension dimension() { return view.dimension(); }

    public String titleText() { return title.getText(); }

    /** Une image : la vue montrée, puis ce qui est écrit sous elle. */
    public void frame(double elapsed, boolean moving) {
        if (!isVisible()) return;
        // Le temps passé la fenêtre ouverte, que l'onglet Statistiques montre.
        if (view.dimension() != null) game.noteDimensionPlayed(view.dimension().number(), elapsed);
        view.frame(elapsed, moving);
        refresh();
        // Le cadre : sa marge quand un cadre est choisi, aucune sinon, et son dessin sur le calque.
        Skin frameSkin = game.chosenSkin(SkinVisual.DIMENSION_FRAME);
        double inset = DimensionFrameArt.frames(frameSkin) ? DimensionFrameArt.THICKNESS : 0;
        if (framed.getPadding().getTop() != inset) framed.setPadding(new Insets(inset));
        if (moving) frameTime += Math.min(elapsed, 0.1);
        DimensionFrameArt.draw(frameLayer, frameSkin, frameTime);
    }

    /** Le bouton d'une amélioration du mélange : pour les vérifications. */
    public Button mixButton(Game.MixUpgrade upgrade) { return mixButtons.get(upgrade); }

    /** L'étape, ce qu'il en reste à gagner, et le bouton qui mêle la dimension suivante : pour les vérifications. */
    public String mixStageText() { return mixStage.getText(); }

    public String mixProgressText() { return mixProgress.getText(); }

    /** La part remplie de la barre de progression, de 0 à 1. */
    public double mixProgressShare() { return mixBarFill.getPrefWidth() / MIX_BAR_WIDTH; }

    public Button mixAdvanceButton() { return mixAdvance; }

    /** Vrai si l'étape et sa barre se montrent, au-dessus de l'arène. */
    public boolean mixStageShown() { return mixStageBox.isVisible(); }

    public String mixHintText() { return mixHint.getText(); }

    /** Le malus, les multiplicateurs, la charge, la cadence, les trésors, les paires et le dernier gain : pour les vérifications. */
    public String mixInfoText() { return mixInfo.getText(); }

    /** Le visuel : pour les vérifications. */
    public DimensionView view() { return view; }

    /** Le bouton d'une amélioration de la ligne : pour les vérifications. */
    public Button lineButton(Game.LineUpgrade upgrade) { return buttons.get(upgrade); }

    public Button yieldButton() { return buttons.get(Game.LineUpgrade.YIELD); }

    public Button ticksButton() { return buttons.get(Game.LineUpgrade.TICKS); }

    /** Le bouton d'une amélioration du plan : pour les vérifications. */
    public Button planeButton(Game.PlaneUpgrade upgrade) { return planeButtons.get(upgrade); }

    /** Le bouton d'une amélioration du volume : pour les vérifications. */
    public Button volumeButton(Game.VolumeUpgrade upgrade) { return volumeButtons.get(upgrade); }

    public String volumeShardsText() { return volumeShards.getText(); }

    /** Le bouton d'une amélioration de la durée : pour les vérifications. */
    public Button durationButton(Game.DurationUpgrade upgrade) { return durationButtons.get(upgrade); }

    public String durationShardsText() { return durationShards.getText(); }

    public String durationHintText() { return durationHint.getText(); }

    /** Le bouton d'une amélioration de la boucle : pour les vérifications. */
    public Button loopButton(Game.LoopUpgrade upgrade) { return loopButtons.get(upgrade); }

    public String loopShardsText() { return loopShards.getText(); }

    public String loopHintText() { return loopHint.getText(); }

    /** Le bouton d'une amélioration du miroir : pour les vérifications. */
    public Button mirrorButton(Game.MirrorUpgrade upgrade) { return mirrorButtons.get(upgrade); }

    public String mirrorShardsText() { return mirrorShards.getText(); }

    public String mirrorThresholdText() { return mirrorThreshold.getText(); }

    public String mirrorHintText() { return mirrorHint.getText(); }

    public String mirrorCountsText() { return mirrorCounts.getText(); }

    /** La taille du plateau, et les réussites qu'il manque pour l'agrandir : pour les vérifications. */
    public String mirrorBoardText() { return mirrorBoard.getText(); }

    /** Le bouton d'une amélioration de la grille : pour les vérifications. */
    public Button gridButton(Game.GridUpgrade upgrade) { return gridButtons.get(upgrade); }

    public String gridShardsText() { return gridShards.getText(); }

    public String gridThresholdText() { return gridThreshold.getText(); }

    public String gridHintText() { return gridHint.getText(); }

    public String gridLayerText() { return gridLayerLabel.getText(); }

    /** La taille de la grille et les essais par point : pour les vérifications. */
    public String gridSizeText() { return gridSize.getText(); }

    /** Les champs de la case visée (x, y, z), le bouton qui l'essaie, et ceux du zoom : pour les vérifications. */
    public TextField[] gridFields() {
        return new TextField[] {gridX, gridY, gridZ};
    }

    public Button gridTryButton() { return gridTry; }

    public Button gridZoomInButton() { return gridZoomIn; }

    public Button gridZoomOutButton() { return gridZoomOut; }

    public Button gridUpButton() { return gridUp; }

    /** Le bouton d'une amélioration du clicker : pour les vérifications. */
    public Button clickerButton(Game.ClickerUpgrade upgrade) { return clickerButtons.get(upgrade); }

    public String clickerShardsText() { return clickerShards.getText(); }

    public String clickerThresholdText() { return clickerThreshold.getText(); }

    public String clickerHintText() { return clickerHint.getText(); }

    /** Le bouton d'une amélioration de la brane : pour les vérifications. */
    public Button braneButton(Game.BraneUpgrade upgrade) { return braneButtons.get(upgrade); }

    public String braneShardsText() { return braneShards.getText(); }

    public String braneThresholdText() { return braneThreshold.getText(); }

    public String braneHintText() { return braneHint.getText(); }

    /** Le bouton du métronome : pour les vérifications. */
    public Button metronomeButton() { return metronomeButton; }

    /** Le bouton de la caméra libre : pour les vérifications. */
    public Button freeCameraButton() { return freeCameraButton; }

    public String volumeHintText() { return volumeHint.getText(); }

    /** Vrai si les améliorations du volume se montrent : hors du cube seulement. */
    public boolean volumeUpgradesShown() { return volumeRow.isVisible(); }

    public String planeShardsText() { return planeShards.getText(); }

    public String thresholdText() { return threshold.getText(); }

    public String shardsText() { return shards.getText(); }

    /** La mention « automatique », vide quand elle est cachée : pour les vérifications. */
    public String autoBadgeText() { return autoBadge.isVisible() ? autoBadge.getText() : ""; }

    public String planeHintText() { return planeHint.getText(); }
}
