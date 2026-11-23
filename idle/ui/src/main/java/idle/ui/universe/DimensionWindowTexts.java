package idle.ui.universe;

import idle.core.Game;
import idle.core.collider.Hadron;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Une partie de {@link DimensionWindow} (découpée en 0.293 pour rester sous 1 000 lignes) : les textes des améliorations de chaque dimension. */
public abstract class DimensionWindowTexts extends DimensionWindowRefresh {
    public DimensionWindowTexts(Game game, BooleanSupplier freeCamera, Consumer<Boolean> setFreeCamera) {
        super(game, freeCamera, setFreeCamera);
    }

    DimensionWindowTexts(Game game, boolean[] free) {
        super(game, free);
    }

    public DimensionWindowTexts(Game game) {
        super(game);
    }

    /** Une amélioration du miroir : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.MirrorUpgrade upgrade) {
        int level = game.mirrorLevel(upgrade);
        return switch (upgrade) {
            case GAIN -> {
                double omega = game.hadronBoost(Hadron.OMEGA);
                yield "Gain : chaque case d'un motif réussi vaut " + shardText((1 + level) * omega) + " → "
                        + shardText((2 + level) * omega) + " reflets, avant le plateau";
            }
            case LENGTH -> "Multiplicateur de longueur : " + Format.multiplier(game.mirrorLengthFactor()) + " → "
                    + Format.multiplier(game.mirrorLengthFactor() + Game.MIRROR_LENGTH_STEP) + " par case au-delà de " + Game.MIRROR_START;
            case MEMORY -> "Temps de mémorisation : " + ElementText.number(game.mirrorMemorySeconds()) + " s → "
                    + ElementText.number(game.mirrorMemorySeconds() + Game.MIRROR_SHOW_STEP) + " s par case";
            case MISTAKES -> "Seconde chance : " + level + (level > 1 ? " erreurs permises" : " erreur permise") + " → " + (level + 1);
            case BOARD -> "Plateau plus vaste : " + game.mirrorMaxGrowth() + " agrandissements permis → " + (game.mirrorMaxGrowth() + 1)
                    + ", jusqu'à " + (2 * (Game.MIRROR_HALF + game.mirrorMaxGrowth() + 1)) + " × "
                    + (Game.MIRROR_ROWS + game.mirrorMaxGrowth() + 1) + " cases";
        };
    }

    /** Une amélioration du clicker : son nom, où elle en est, et ce que donne le niveau suivant ; celles des zones les attendent. */
    String describe(Game.ClickerUpgrade upgrade) {
        int level = game.clickerLevel(upgrade);
        String text = switch (upgrade) {
            case GAIN -> "Gain : chaque clic vaut " + shardText(game.clickerValue()) + " → "
                    + shardText(game.clickerValue() + game.hadronBoost(Hadron.OMEGA)) + " étincelles";
            case SPEED -> "Multiplicateur de cadence : +" + ElementText.number(game.clickerSpeedStep() * 100) + " % → +"
                    + ElementText.number((game.clickerSpeedStep() + Game.CLICKER_SPEED_STEP) * 100) + " % par clic par seconde";
            case ZONES -> level > 0 ? "Zones ×2 : débloquées" : "Zones ×2 : des zones apparaissent, un clic dedans rapporte double";
            case ZONE_MULT -> "Multiplicateur des zones : " + Format.multiplier(game.clickerZoneFactor()) + " → "
                    + Format.multiplier(game.clickerZoneFactor() + Game.CLICKER_ZONE_STEP);
            case ZONE_TIME -> "Durée des zones : " + seconds(game.clickerZoneSeconds()) + " s → "
                    + seconds(game.clickerZoneSeconds() + Game.CLICKER_ZONE_SECONDS_STEP) + " s";
            case ZONE_COUNT -> "Nombre de zones : " + (1 + level) + " à la fois → " + (2 + level);
            case ZONE_SIZE -> "Taille des zones (×" + ElementText.number(1 + Game.CLICKER_ZONE_SIZE_STEP * level) + ") : "
                    + percent(Game.CLICKER_ZONE_SIZE_STEP) + " plus grandes";
            case STACK -> "Zones superposées : chaque zone de plus " + Format.multiplier(game.clickerStackFactor()) + " → "
                    + Format.multiplier(game.clickerStackFactor() + Game.CLICKER_STACK_STEP);
        };
        return upgrade.needsZones() && !game.areClickerZonesUnlocked() ? text + " (après « Zones ×2 »)" : text;
    }

    /** Une amélioration de la brane : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.BraneUpgrade upgrade) {
        int level = game.braneLevel(upgrade);
        return switch (upgrade) {
            case GAIN -> "Gain : chaque passage vaut " + shardText(game.braneValue()) + " → "
                    + shardText(game.braneValue() + game.hadronBoost(Hadron.OMEGA)) + " ondes";
            case ELASTICITY -> "Élasticité : l'onde porte à fond dès " + Math.round(100 / (1 + Game.BRANE_ELASTIC_STEP * level))
                    + " % de tirée → " + Math.round(100 / (1 + Game.BRANE_ELASTIC_STEP * (level + 1))) + " %";
            case SIZE -> "Grandeur des ondes : portée à fond " + reach(game.braneMaxReach()) + " → "
                    + reach(Game.BRANE_REACH * (1 + Game.BRANE_SIZE_STEP * (level + 1))) + " (côté 2)";
            case BOUNCE -> "Rebond : un passage après un bord vaut " + Format.multiplier(game.braneBounceFactor()) + " → "
                    + Format.multiplier(game.braneBounceFactor() + Game.BRANE_BOUNCE_STEP);
            case COMBO -> "Points en une onde : chaque passage de plus de la même onde +" + percent(game.braneComboStep())
                    + " → +" + percent(game.braneComboStep() + Game.BRANE_COMBO_STEP);
        };
    }

    /** Les touches des cercles en jeu, dans l'ordre : « S, D, F ». */
    String loopKeysText() {
        StringBuilder keys = new StringBuilder();
        for (int ring = 0; ring < game.loopRings(); ring++) {
            KeyCode key = loopSettings.loopKey(ring);
            keys.append(ring == 0 ? "" : ", ").append(key == null ? "aucune" : Detail.keyName(key));
        }
        return keys.toString();
    }

    /** Une amélioration de la boucle : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.LoopUpgrade upgrade) {
        double value = game.loopValue(), next = value + game.hadronBoost(Hadron.OMEGA);
        int precision = game.loopLevel(Game.LoopUpgrade.PRECISION);
        return switch (upgrade) {
            case SPEED -> "Rapidité : un tour en " + seconds(1 / game.loopTurnsPerSecond()) + " s → "
                    + seconds(1 / (game.loopTurnsPerSecond() * Game.LOOP_SPEED_STEP)) + " s";
            case ZONES -> "Zones : " + game.loopZoneCount() + " par cercle, une de plus";
            case GAIN -> "Gain par zone : chaque zone vaut " + shardText(value) + " → " + shardText(next) + " nœuds";
            case PRECISION -> "Précision : le centre donne " + shardText(game.loopReward(Game.LoopHit.EXCELLENT)) + " → "
                    + shardText(value * Game.LOOP_EXCELLENT_SHARE * (1 + Game.LOOP_PRECISION_EXCELLENT * (precision + 1)))
                    + ", « bien » " + shardText(game.loopReward(Game.LoopHit.GOOD)) + " → "
                    + shardText(value * (1 + Game.LOOP_PRECISION_GOOD * (precision + 1)));
            case SIZE -> "Taille des zones (×" + shardText(game.loopZoneHalfWidth() / Game.LOOP_ZONE_HALF) + ") : "
                    + percent(Game.LOOP_SIZE_STEP) + " plus larges";
            case RINGS -> {
                KeyCode key = loopSettings.loopKey(Math.min(Game.MAX_RINGS - 1, game.loopRings()));
                yield "Cercles : " + game.loopRings() + ", un de plus"
                        + (game.loopRings() < Game.MAX_RINGS && key != null ? ", sur la touche " + Detail.keyName(key) : "");
            }
        };
    }

    /** Une amélioration de la durée : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.DurationUpgrade upgrade) {
        return switch (upgrade) {
            case SPEED -> "Vitesse des spirales : " + shardText(game.durationTurnsPerSecond()) + " tour par seconde, " + percent(Game.DURATION_SPEED_STEP) + " plus vite";
            case COUNT -> "Spirales : " + game.durationSpirals() + ", une de plus qui avance en même temps";
            case VALUE -> "Valeur : le premier tour donne " + shardText(game.durationValue()) + " → " + shardText(game.durationValue() + game.hadronBoost(Hadron.OMEGA));
            case TIME -> "Temps au plus : " + seconds(game.durationMaxSeconds()) + " s → "
                    + seconds(game.durationMaxSeconds() + Game.DURATION_TIME_STEP) + " s, des tours de plus";
        };
    }

    /** Une amélioration du volume : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.VolumeUpgrade upgrade) {
        return switch (upgrade) {
            case TIME -> "Durée : " + seconds(game.volumeDiveSeconds()) + " s par plongée → "
                    + seconds(game.volumeDiveSeconds() + Game.VOLUME_DIVE_STEP) + " s";
            case VALUE -> "Valeur : chaque boule cueillie donne " + shardText(game.volumeValue()) + " → " + shardText(game.volumeValue() + game.hadronBoost(Hadron.OMEGA));
            case SPAWN -> "Réapparition : une boule toutes les " + seconds(game.volumeSpawnSeconds()) + " s, 15 % plus vite";
            case COUNT -> "Nombre : " + game.volumeMaxBalls() + " boules à la fois au plus, une de plus";
            case SIZE -> "Taille des boules (×" + shardText(game.volumeBallRadius() / Game.VOLUME_BALL_RADIUS)
                    + ") : " + percent(Game.VOLUME_SIZE_STEP) + " plus grosses, elles se visent mieux";
        };
    }

    /** Une amélioration du plan : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.PlaneUpgrade upgrade) {
        return switch (upgrade) {
            case VALUE -> "Valeur : chaque éclat ramassé donne " + shardText(game.planeValue()) + " → " + shardText(game.planeValue() + game.hadronBoost(Hadron.OMEGA)) + " facettes";
            case SPAWN -> "Réapparition : un éclat toutes les " + seconds(game.planeSpawnSeconds()) + " s, 15 % plus vite";
            case COUNT -> "Nombre : " + game.planeMaxShards() + " éclats à la fois au plus, un de plus";
            case SPEED -> "Vitesse du point : 20 % plus vite (×" + shardText(game.planeSpeed() / Game.PLANE_SPEED) + ")";
            case SIZE -> "Taille du point (×" + shardText(game.planePlayerRadius() / Game.PLANE_PLAYER_RADIUS) + ") : " + percent(Game.PLANE_SIZE_STEP) + " plus gros, il ramasse de plus loin";
        };
    }

    /**
     * Une touche, quand la fenêtre d'une dimension est ouverte : dans le plan, Z Q S D et les flèches déplacent le point ;
     * dans la durée, la barre d'espace tient le bouton ; dans la boucle, la touche de chaque cercle le joue ; dans la
     * grille, les flèches haut et bas changent de couche ; dans le mélange, Z Q S D et les flèches mènent le point, A et E
     * le font remonter ou plonger, la barre d'espace tient la charge. Vrai si la touche a servi (le reste du jeu ne la
     * voit pas).
     */
    public boolean key(KeyCode code, boolean down) {
        if (!isVisible()) return false;
        // La boucle : la touche d'un cercle le joue à l'appui ; tenue, elle ne le rejoue pas. Les touches
        // des cercles pas encore achetés ne servent à rien, mais ne vont pas aux raccourcis : la fenêtre les garde.
        DimensionView loop = shown(Dimension.LOOP);
        if (loop != null && game.isLoopOpen()) {
            int ring = loopRingOf(code);
            if (ring >= 0) {
                if (!down) loopKeysHeld.remove(code);
                else if (loopKeysHeld.add(code) && ring < game.loopRings()) loop.clickRing(ring);
                return true;
            }
        }
        // La grille : les flèches haut et bas changent de couche.
        DimensionView grid = shown(Dimension.GRID);
        if (grid != null && game.isGridOpen() && (code == KeyCode.UP || code == KeyCode.DOWN)) {
            if (down) {
                grid.shiftGridLayer(code == KeyCode.UP ? 1 : -1);
                refresh();
            }
            return true;
        }
        // Le volume : en caméra libre, Échap rend la souris retenue ; la plongée continue.
        DimensionView volume = shown(Dimension.VOLUME);
        if (volume != null && code == KeyCode.ESCAPE && volume.mouseHeld()) {
            if (down) volume.releaseMouse();
            return true;
        }
        // La durée : la barre d'espace tient le bouton du milieu, comme la souris.
        DimensionView duration = shown(Dimension.DURATION);
        if (duration != null && code == KeyCode.SPACE && game.isDurationOpen()) {
            if (down && !spaceHeld) {
                spaceHeld = true;
                if (game.holdDuration()) onGesture.accept(Dimension.DURATION.number());
            } else if (!down) {
                spaceHeld = false;
                duration.releaseHold();
            }
            return true;
        }
        if (view.dimension() == Dimension.MIX) return mixKey(code, down);
        if (view.dimension() != Dimension.PLANE || !game.isPlaneOpen()) return false;
        switch (code) {
            case Z, Q, S, D, UP, DOWN, LEFT, RIGHT -> { }
            default -> { return false; }
        }
        if (down) held.add(code);
        else held.remove(code);
        steer();
        return true;
    }
}
