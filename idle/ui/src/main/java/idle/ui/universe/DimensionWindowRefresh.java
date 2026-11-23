package idle.ui.universe;

import idle.core.Game;
import idle.core.collider.Hadron;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Une partie de {@link DimensionWindow} (découpée en 0.293 pour rester sous 1 000 lignes) : ce que chaque dimension affiche à chaque image. */
public abstract class DimensionWindowRefresh extends DimensionWindowBase {
    public DimensionWindowRefresh(Game game, BooleanSupplier freeCamera, Consumer<Boolean> setFreeCamera) {
        super(game, freeCamera, setFreeCamera);
    }

    DimensionWindowRefresh(Game game, boolean[] free) {
        super(game, free);
    }

    public DimensionWindowRefresh(Game game) {
        super(game);
    }

    /**
     * Les étiquettes qui portent la couleur de leur dimension (version 0.197 : celle de la palette choisie) : les éclats de
     * chaque dimension, la couche et les axes de la grille, le tableau du miroir, l'étape du mélange et sa barre.
     */
    void restyle() {
        String bold = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: ";
        shards.setStyle(bold + color(1) + ";");
        planeShards.setStyle(bold + color(2) + ";");
        volumeShards.setStyle(bold + color(3) + ";");
        durationShards.setStyle(bold + color(4) + ";");
        loopShards.setStyle(bold + color(5) + ";");
        braneShards.setStyle(bold + color(6) + ";");
        clickerShards.setStyle(bold + color(7) + ";");
        gridShards.setStyle(bold + color(8) + ";");
        gridLayerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color(8) + ";");
        for (Label name : gridAxisNames) name.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color(8) + ";");
        mirrorShards.setStyle(bold + color(9) + ";");
        mirrorBoard.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color(9) + ";");
        mixShards.setStyle(bold + color(10) + ";");
        mixInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color(10) + ";");
        mixStage.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + color(10) + ";");
        mixBarFill.setStyle("-fx-background-color: " + color(10) + "; -fx-background-radius: 4 0 0 4;");
    }

    /** Le cadre des mini-jeux : pour les vérifications, la marge que la boîte de l'arène laisse au cadre. */
    public double frameInset() { return framed.getPadding().getTop(); }

    /** Le style de l'étiquette des éclats de la dimension {@code number} (1 à 10), qui porte sa couleur : pour les vérifications. */
    public String shardsStyle(int number) {
        Label label = switch (number) {
            case 2 -> planeShards;
            case 3 -> volumeShards;
            case 4 -> durationShards;
            case 5 -> loopShards;
            case 6 -> braneShards;
            case 7 -> clickerShards;
            case 8 -> gridShards;
            case 9 -> mirrorShards;
            case 10 -> mixShards;
            default -> shards;
        };
        return label.getStyle();
    }

    /** Vrai si {@code key} diffère de ce que {@code node} a montré en dernier ; la retient pour la prochaine image. */
    boolean changed(Object node, String key) { return !key.equals(painted.put(node, key)); }

    /**
     * Les boutons d'une dimension : la description de chacun, son prix (au singulier sous 2) ou « Au maximum », et sa
     * couleur selon qu'il s'achète. {@code unit} est le singulier de l'éclat, précédé d'une espace ; le pluriel ajoute un s.
     * Tout ce qui s'affiche ne dépend que des niveaux et de ce qui est achetable : la rangée ne se refait que s'ils changent.
     */
    <U extends Enum<U>> void paintUpgrades(Map<U, Button> buttons, Function<U, String> describe,
            ToIntFunction<U> level, ToIntFunction<U> max, ToDoubleFunction<U> cost,
            Predicate<U> canBuy, String unit, String accent) {
        StringBuilder key = new StringBuilder();
        for (U upgrade : buttons.keySet()) {
            key.append(level.applyAsInt(upgrade)).append(canBuy.test(upgrade) ? '+' : '-');
        }
        key.append(accent);
        if (!changed(buttons, key.toString())) return;
        for (U upgrade : buttons.keySet()) {
            Button button = buttons.get(upgrade);
            boolean full = level.applyAsInt(upgrade) >= max.applyAsInt(upgrade);
            double price = cost.applyAsDouble(upgrade);
            button.setText(describe.apply(upgrade) + "\n" + (full ? "Au maximum" : "Prix : " + shardText(price) + unit + (price < 2 ? "" : "s")));
            button.setDisable(!canBuy.test(upgrade));
            paintUpgrade(button, canBuy.test(upgrade), full, accent);
        }
    }

    void refreshPlane() {
        planeShards.setText("Facettes du plan : " + shardText(game.state().planeShards()));
        // Le seuil du plan, comme celui de la ligne : son rond vert (version 0.139 : il n'ouvre plus de palier de la graine).
        threshold(planeThreshold, game.isPlaneThresholdReached(), game.state().planeShardsEarned(), Game.PLANE_THRESHOLD, "facettes gagnées", 2);
        planeHint.setText("Z Q S D (ou les flèches) déplacent le point. Passez sur un éclat pour le ramasser."
                + (game.isDimensionAutoPlayed(2)
                        ? " Visée automatique : touches lâchées, le point file seul vers l'éclat le plus proche (il reprend sa direction "
                                + cadence(2) + ")." : ""));
        paintUpgrades(planeButtons, this::describe, game::planeLevel, Game.PlaneUpgrade::max, game::planeCost, game::canBuyPlane, " facette", color(2));
        if (changed(planeDetail, Detail.shown() + "/" + game.planeMaxShards() + "/" + game.planeSpawnSeconds() + "/" + game.planeValue())) {
            planeDetail.setText(Detail.only("Un éclat apparaît toutes les " + seconds(game.planeSpawnSeconds()) + " s, "
                    + game.planeMaxShards() + " à la fois au plus ; le point qui passe dessus le ramasse, pour "
                    + shardText(game.planeValue()) + " facette" + (game.planeValue() >= 2 ? "s" : "") + ". Les facettes, le seuil et "
                    + "les améliorations du plan traversent les univers ; le seuil marque la dimension d'un rond vert, et les "
                    + "facettes paient aussi l'étage du chrono et sa réserve, dans les améliorations de la graine."));
            planeDetail.setVisible(!planeDetail.getText().isEmpty());
            planeDetail.setManaged(planeDetail.isVisible());
        }
    }

    /**
     * Le volume : ses éclats et son seuil, toujours ; ses améliorations seulement dehors, puisqu'elles ne
     * s'achètent pas pendant une plongée ; dedans, ce qu'on y fait et le temps qu'il reste.
     */
    void refreshVolume() {
        volumeShards.setText("Sphères du volume : " + shardText(game.state().volumeShards()));
        threshold(volumeThreshold, game.isVolumeThresholdReached(), game.state().volumeShardsEarned(), Game.VOLUME_THRESHOLD, "sphères gagnées", 3);
        boolean outside = view.dive() == DimensionView.Dive.OUTSIDE;
        boolean diving = game.isDimensionAutoPlayed(3);
        volumeHint.setText((diving ? "Plongée automatique : elle replonge seule et cueille une boule " + cadence(3)
                + " ; vous pouvez cueillir avec elle. " : "") + switch (view.dive()) {
            case OUTSIDE -> diving ? "Les améliorations s'achètent ici, entre deux plongées."
                    : "Cliquez le cube pour y plonger, " + seconds(game.volumeDiveSeconds()) + " s par plongée. "
                    + "Les améliorations s'achètent ici, hors du cube.";
            case ENTERING -> "Vous plongez dans le cube…";
            case INSIDE -> (!view.freeCamera()
                    ? "Clic gauche tenu : regarder autour de soi. Clic droit : cueillir la boule visée. "
                    : view.mouseHeld()
                    ? "Caméra libre : bougez la souris pour regarder autour de vous. Un clic cueille la boule sous la croix. "
                            + "Échap rend la souris. "
                    : "Souris rendue : cliquez dans la vue pour reprendre la caméra libre. ")
                    + "Le cube vous rejette dans " + seconds(game.volumeTimeLeft()) + " s.";
            case LEAVING -> "Le cube vous rejette : les améliorations reviennent.";
        });
        if (changed(volumeDetail, Detail.shown() + "/" + game.volumeLevel(Game.VolumeUpgrade.SPAWN) + "/" + game.volumeLevel(Game.VolumeUpgrade.COUNT))) {
            volumeDetail.setText(Detail.only("Une plongée commence quand la vue arrive au centre du cube. Une "
                + "boule apparaît aussitôt, puis une toutes les " + seconds(game.volumeSpawnSeconds()) + " s, "
                + game.volumeMaxBalls() + " à la fois au plus, entre " + shardText(Game.VOLUME_SPAWN_NEAR) + " et "
                + shardText(Game.VOLUME_SPAWN_FAR) + " du centre. Le clic droit cueille la plus proche que traverse le "
                + "regard ; une boule plus grosse se vise mieux. À la fin du minuteur, ou si le jeu passe en absence, la "
                + "plongée s'arrête et les boules restantes s'en vont. Les sphères, le seuil et les améliorations du volume "
                + "traversent les univers ; le seuil marque la dimension d'un rond vert, et les sphères paient aussi des améliorations de la graine. En caméra libre, la souris disparaît "
                + "dans le cube et y reste retenue : chaque mouvement tourne le regard et elle revient seule au centre. "
                + "Échap la rend sans finir la plongée, un clic dans la vue la reprend ; quand le jeu n'a pas la main "
                + "(Alt+Tab), elle n'est jamais retenue. Si le système refuse de la déplacer, pousser un bord fait tourner "
                + "le regard sans fin."));
            volumeDetail.setVisible(!volumeDetail.getText().isEmpty());
            volumeDetail.setManaged(volumeDetail.isVisible());
        }
        freeCameraButton.setText(view.freeCamera() ? "Caméra libre : activée (le regard suit la souris)"
                : "Caméra libre : coupée (clic gauche tenu pour regarder)");
        // Dedans, la rangée se cache mais garde sa place : le cube ne se déplace pas pendant la plongée.
        volumeRow.setVisible(outside);
        paintUpgrades(volumeButtons, this::describe, game::volumeLevel, Game.VolumeUpgrade::max, game::volumeCost,
                u -> outside && game.canBuyVolume(u), " sphère", color(3));
    }

    /** La durée : ses éclats, son seuil, ce qu'on y fait ; ses améliorations, bouton relâché. */
    void refreshDuration() {
        durationShards.setText("Instants de la durée : " + shardText(game.state().durationShards()));
        threshold(durationThreshold, game.isDurationThresholdReached(), game.state().durationShardsEarned(), Game.DURATION_THRESHOLD, "instants gagnés", 4);
        boolean held = game.isDurationHeld();
        durationHint.setText(game.isDimensionAutoPlayed(4)
                ? "Appui automatique : tenu " + seconds(game.durationHeldSeconds()) + " s sur " + seconds(game.durationMaxSeconds())
                        + " s, il relâche seul au centre (+" + shardText(game.durationRewardFor(game.durationMaxSeconds()))
                        + ") et rappuie " + cadence(4) + ". Les améliorations s'achètent quand même, et valent dès le cycle en cours."
                : !held
                ? "Tenez le bouton du milieu (clic tenu, ou la barre d'espace) : les spirales avancent vers le centre, qu'elles "
                        + "atteignent en " + seconds(game.durationMaxSeconds()) + " s. Relâchez pour gagner leurs instants : plus on tient, plus chaque tour rapporte."
                        + (game.durationReleases() > 0 ? " Dernier relâchement : +" + shardText(game.durationLastReward()) + "." : "")
                : game.durationProgress() >= 1
                ? "Les spirales sont au centre : tenir plus ne rapporte plus rien. Relâchez pour gagner +" + shardText(game.durationPending()) + "."
                : "Tenu " + seconds(game.durationHeldSeconds()) + " s sur " + seconds(game.durationMaxSeconds())
                        + " : relâcher maintenant donnerait +" + shardText(game.durationPending()) + ".");
        StringBuilder levels = new StringBuilder().append(Detail.shown());
        for (Game.DurationUpgrade upgrade : Game.DurationUpgrade.values()) levels.append('/').append(game.durationLevel(upgrade));
        if (changed(durationDetail, levels.toString())) {
            durationDetail.setText(Detail.only("Chaque spirale fait " + shardText(game.durationTurnsPerSecond())
                + " tour par seconde tenue, jusqu'au centre, " + shardText(game.durationTurnsMax()) + " tours en "
                + seconds(game.durationMaxSeconds()) + " s. Au relâchement, chaque tour vaut un de plus que le précédent : n tours "
                + "donnent (n² + n) ÷ 2 fois la valeur (" + shardText(game.durationValue()) + "), pour chacune des "
                + game.durationSpirals() + " spirale" + (game.durationSpirals() > 1 ? "s" : "") + ". Tenir au-delà du centre ne "
                + "rapporte rien de plus et ne fait rien perdre. Les améliorations s'achètent bouton relâché. Une absence relâche "
                + "le bouton, qui donne ce qu'il avait. Les instants, le seuil et les améliorations de la durée traversent les "
                + "univers ; le seuil marque la dimension d'un rond vert, et les instants paient aussi des améliorations de la graine."));
            durationDetail.setVisible(!durationDetail.getText().isEmpty());
            durationDetail.setManaged(durationDetail.isVisible());
        }
        paintUpgrades(durationButtons, this::describe, game::durationLevel, Game.DurationUpgrade::max, game::durationCost,
                u -> !held && game.canBuyDuration(u), " instant", color(4));
    }

    /**
     * La boucle : ses nœuds, son seuil, comment on y joue (les touches des cercles en jeu) ou ce que fait le rythme
     * automatique, le métronome, le détail des parts d'une zone, et ses améliorations, qui s'achètent toujours.
     */
    void refreshLoop() {
        loopShards.setText("Nœuds de la boucle : " + shardText(game.state().loopShards()));
        threshold(loopThreshold, game.isLoopThresholdReached(), game.state().loopShardsEarned(), Game.LOOP_THRESHOLD, "nœuds gagnés", 5);
        Game.LoopHit last = game.lastLoopHit();
        String lastText = game.loopHits() == 0 && last == Game.LoopHit.MISS ? "" : " Dernier clic : " + DimensionView.hitWord(last).toLowerCase(Locale.ROOT) + ".";
        loopHint.setText(game.isDimensionAutoPlayed(5)
                ? "Rythme automatique : il clique « bien » au passage d'un point dans une zone, sur chaque cercle, un clic " + cadence(5) + " au plus. "
                        + "Vos clics comptent en plus ; les améliorations s'achètent quand même."
                : "Cliquez un cercle, ou sa touche (" + loopKeysText() + "), quand son point passe dans une zone : au centre "
                        + "« excellent », puis « bien », au bord « mauvais ». Après chaque clic, le cercle attend "
                        + seconds(Game.LOOP_COOLDOWN) + " s." + lastText);
        metronomeButton.setText(loopSettings.metronome() ? "Métronome : activé (un tic à chaque huitième de tour du premier cercle)"
                : "Métronome : coupé");
        StringBuilder levels = new StringBuilder().append(Detail.shown());
        for (Game.LoopUpgrade upgrade : Game.LoopUpgrade.values()) levels.append('/').append(game.loopLevel(upgrade));
        if (changed(loopDetail, levels.toString())) {
            loopDetail.setText(Detail.only("Chaque point fait un tour en " + seconds(1 / game.loopTurnsPerSecond())
                + " s. Une zone couvre " + Math.round(game.loopZoneHalfWidth() * 200) + " % de son cercle : au centre, "
                + Math.round(Game.LOOP_EXCELLENT * 100) + " % de sa largeur, « excellent » donne "
                + shardText(game.loopReward(Game.LoopHit.EXCELLENT)) + " nœuds ; jusqu'à " + Math.round(Game.LOOP_GOOD * 100)
                + " %, « bien » en donne " + shardText(game.loopReward(Game.LoopHit.GOOD)) + " ; au bord, « mauvais », "
                + shardText(game.loopReward(Game.LoopHit.BAD)) + ". Un clic à côté ne donne rien. La zone touchée se pose "
                + "ailleurs sur son cercle. Après chaque clic, juste ou non, le cercle attend " + seconds(Game.LOOP_COOLDOWN)
                + " s : cliquer sans arrêt ne rapporte rien. Chaque cercle a sa touche, qui se change dans les réglages "
                + "(Clavier). Les nœuds, le seuil et les améliorations de la boucle traversent les univers ; le seuil marque "
                + "la dimension d'un rond vert, et les nœuds paient aussi le rond de croissance de la graine."));
            loopDetail.setVisible(!loopDetail.getText().isEmpty());
            loopDetail.setManaged(loopDetail.isVisible());
        }
        paintUpgrades(loopButtons, this::describe, game::loopLevel, Game.LoopUpgrade::max, game::loopCost, game::canBuyLoop,
                " nœud", color(5));
    }

    /**
     * La brane : ses ondes, son seuil, comment on la tire (la force et la portée pendant qu'on la tient) ou ce que fait la
     * tirée automatique, le gain du dernier passage, le détail des passages et des rebonds, et ses améliorations, qui
     * s'achètent toujours.
     */
    void refreshBrane() {
        braneShards.setText("Ondes de la brane : " + shardText(game.state().braneShards()));
        threshold(braneThreshold, game.isDimensionThresholdReached(6), game.state().braneShardsEarned(), Game.BRANE_THRESHOLD, "ondes gagnées", 6);
        // Les rebonds du dernier passage : ceux de l'onde qui court, sinon on ne les sait plus.
        int bounces = game.braneWaves().isEmpty() ? 0 : game.braneWaves().get(0).lastBounces();
        String last = game.braneTouches() == 0 ? "" : " Dernier passage : " + DimensionView.passText(game.braneLastGain(), bounces) + ".";
        braneHint.setText(view.braneHeld()
                ? "Tirée à " + Math.round(view.branePull() * 100) + " % : l'onde portera à " + reach(game.braneReach(view.branePull()))
                        + " (" + reach(game.braneMaxReach()) + " à fond). Relâchez pour la lancer."
                : game.isDimensionAutoPlayed(6)
                ? "Tirée automatique : " + cadence(6) + ", dès que l'onde d'avant s'est éteinte, elle tire la membrane à fond sur "
                        + "le point qui en touche le plus. Vos tirées comptent en plus ; les améliorations s'achètent quand même." + last
                : !game.canPullBrane()
                ? "Une onde court déjà : attendez qu'elle s'éteigne pour tirer de nouveau." + last
                : "Appuyez sur la membrane, glissez pour la tendre (plus loin, plus fort), relâchez : l'onde part du point "
                        + "appuyé et rebondit sur les bords. Chaque point qu'elle touche donne des ondes." + last);
        StringBuilder levels = new StringBuilder().append(Detail.shown());
        for (Game.BraneUpgrade upgrade : Game.BraneUpgrade.values()) levels.append('/').append(game.braneLevel(upgrade));
        if (changed(braneDetail, levels.toString())) {
            braneDetail.setText(Detail.only("La membrane fait 2 de côté. Une onde tirée à fond porte à "
                + reach(game.braneMaxReach()) + ", à peine tirée à " + Math.round(Game.BRANE_MIN_PULL * 100) + " % de cela ; un glissé de "
                + reach(DimensionView.BRANE_FULL_PULL) + " la tire à fond. Son front avance de " + reach(Game.BRANE_WAVE_SPEED)
                + " par seconde et rebondit sur les bords : chaque passage sur un point compte, en direct puis après chaque "
                + "rebond qui le retrouve, plusieurs fois si elle porte assez loin (le plus de rebonds en un passage depuis le "
                + "lancement : " + game.braneMostBounces() + "). Un passage donne " + shardText(game.braneValue()) + " onde"
                + (game.braneValue() >= 2 ? "s" : "") + ", " + Format.multiplier(game.braneBounceFactor())
                + " par rebond, et " + percent(game.braneComboStep()) + " de plus par passage de plus de la même onde. "
                + "Les points touchés restent tant que l'onde court, puis partent avec elle ; un point paraît toutes les "
                + seconds(Game.BRANE_SPAWN_SECONDS) + " s, " + Game.BRANE_MAX_POINTS + " au plus, et une seule onde court à "
                + "la fois. Les ondes, le seuil et les améliorations de la brane traversent les "
                + "univers ; le seuil marque la dimension d'un rond vert, et les ondes paient aussi la lumière astrale de la graine."));
            braneDetail.setVisible(!braneDetail.getText().isEmpty());
            braneDetail.setManaged(braneDetail.isVisible());
        }
        paintUpgrades(braneButtons, this::describe, game::braneLevel, Game.BraneUpgrade::max, game::braneCost, game::canBuyBrane,
                " onde", color(6));
    }

    /**
     * Le clicker : ses étincelles, son seuil, comment on y joue (la cadence, son multiplicateur, les zones) ou ce que font
     * les clics automatiques, le dernier gain, le détail, et ses améliorations, qui s'achètent toujours ; celles des
     * zones, une fois les zones débloquées.
     */
    void refreshClicker() {
        clickerShards.setText("Étincelles du clicker : " + shardText(game.state().clickerShards()));
        threshold(clickerThreshold, game.isDimensionThresholdReached(7), game.state().clickerShardsEarned(), Game.CLICKER_THRESHOLD,
                "étincelles gagnées", 7);
        String last = game.clickerClicks() == 0 ? "" : " Dernier clic : +" + shardText(game.clickerLastGain()) + ".";
        String zones = game.areClickerZonesUnlocked()
                ? " Dans une zone, ×" + ElementText.number(game.clickerZoneFactor()) + ", et davantage là où elles se superposent."
                : "";
        clickerHint.setText(game.isDimensionAutoPlayed(7)
                ? "Clics automatiques : " + ElementText.number(game.clickerAutoRate()) + " clics par seconde au "
                        + "meilleur endroit (cadence " + view.cadenceText(game.clickerRate()) + "). Vos clics comptent en plus ; "
                        + "les améliorations s'achètent quand même." + last
                : "Cliquez la surface : chaque clic donne des étincelles, d'autant plus que vous cliquez vite (cadence "
                        + view.cadenceText(game.clickerRate()) + ")." + zones + last);
        StringBuilder levels = new StringBuilder().append(Detail.shown());
        for (Game.ClickerUpgrade upgrade : Game.ClickerUpgrade.values()) levels.append('/').append(game.clickerLevel(upgrade));
        if (changed(clickerDetail, levels.toString())) {
            clickerDetail.setText(Detail.only("Un clic donne " + shardText(game.clickerValue()) + " étincelle"
                + (game.clickerValue() >= 2 ? "s" : "") + ", multipliée par la cadence : +"
                + ElementText.number(game.clickerSpeedStep() * 100) + " % par clic par seconde, comptés sur les "
                + seconds(Game.CLICKER_WINDOW) + " dernières secondes, jusqu'à " + Math.round(Game.CLICKER_MAX_RATE)
                + " clics par seconde (" + Format.multiplier(game.clickerSpeedFactor(Game.CLICKER_MAX_RATE)) + ")."
                + (game.areClickerZonesUnlocked()
                        ? " Une zone apparaît toutes les " + seconds(Game.CLICKER_ZONE_EVERY) + " s, " + game.clickerMaxZones()
                                + " à la fois au plus, et dure " + seconds(game.clickerZoneSeconds()) + " s ; un clic dedans vaut "
                                + Format.multiplier(game.clickerZoneFactor()) + ", et chaque zone superposée de plus "
                                + Format.multiplier(game.clickerStackFactor()) + ". Le plus de zones sous un clic depuis le "
                                + "lancement : " + game.clickerBestStack() + "."
                        : " L'amélioration « Zones ×2 » fait apparaître des zones où un clic rapporte double.")
                + " Les étincelles, le seuil et les améliorations du clicker traversent les univers ; le seuil marque la "
                + "dimension d'un rond vert, et les étincelles paient aussi la météo de la graine."));
            clickerDetail.setVisible(!clickerDetail.getText().isEmpty());
            clickerDetail.setManaged(clickerDetail.isVisible());
        }
        paintUpgrades(clickerButtons, this::describe, game::clickerLevel, Game.ClickerUpgrade::max, game::clickerCost,
                game::canBuyClicker, " étincelle", color(7));
    }

    /**
     * La grille : ses repères, son seuil, sa taille, comment on y joue ou ce que font les essais automatiques, le dernier
     * essai tel que l'affichage permet de le dire (le gain, puis la distance, la couche et la colonne du point visé) et
     * les essais qu'il reste sur ce point, la couche et la case visée, le zoom, le détail, et ses améliorations.
     */
    void refreshGrid() {
        gridShards.setText("Repères de la grille : " + shardText(game.state().gridShards()));
        threshold(gridThreshold, game.isDimensionThresholdReached(8), game.state().gridShardsEarned(), Game.GRID_THRESHOLD, "repères gagnés", 8);
        int layer = view.gridLayer(), n = game.gridDivisions();
        gridLayerLabel.setText(DimensionView.layerText(layer, n));
        gridDown.setDisable(layer <= 0);
        gridUp.setDisable(layer >= n - 1);
        // La case visée : les champs suivent la vue (un clic sur la carte, une couche changée), sans écraser la saisie en cours.
        int[] at = view.gridSelected();
        TextField[] fields = {gridX, gridY, gridZ};
        for (int axis = 0; axis < 3; axis++) {
            if (!fields[axis].isFocused() && parsed(fields[axis]) != at[axis]) fields[axis].setText(String.valueOf(at[axis]));
        }
        gridTry.setDisable(!game.canProbeGrid());
        boolean big = !view.gridClickable();
        gridZoomIn.setDisable(!big || view.gridSpan() <= DimensionView.GRID_CLICK_CELLS);
        gridZoomOut.setDisable(!big || view.gridZoom() == 0);
        String size = Format.whole(n) + " × " + Format.whole(n) + " × " + Format.whole(n);
        gridSize.setText("Grille de " + size + " cases" + (big ? ", la carte en montre " + Format.whole(view.gridSpan()) + " × "
                + Format.whole(view.gridSpan()) : "") + " · " + game.gridAttempts() + (game.gridAttempts() > 1 ? " essais" : " essai")
                + " par point");
        String last = lastProbeText();
        String how = big ? "Visez une case (un clic sur la carte, ou ses coordonnées), puis « Essayer »"
                : "Cliquez une case pour l'essayer";
        gridHint.setText(game.isDimensionAutoPlayed(8)
                ? "Essais automatiques : " + cadence(8) + ", une case près d'un point ; sur "
                        + "une grande grille ils ne trouvent presque jamais, réduite souvent. Vos essais comptent en plus." + last
                : how + " (la molette ou les flèches haut et bas changent de couche) : plus elle est près d'un point caché, "
                        + "plus elle donne de repères ; celle qui le contient le trouve, " + Format.multiplier(Game.GRID_FOUND) + "." + last);
        StringBuilder levels = new StringBuilder().append(Detail.shown());
        for (Game.GridUpgrade upgrade : Game.GridUpgrade.values()) levels.append('/').append(game.gridLevel(upgrade));
        if (changed(gridDetail, levels.toString())) {
            gridDetail.setText(Detail.only("La grille compte " + size + " cases ; " + game.gridPointCount() + " point"
                + (game.gridPointCount() > 1 ? "s s'y cachent" : " s'y cache") + ", et chacun n'a que " + game.gridAttempts()
                + (game.gridAttempts() > 1 ? " essais" : " essai") + " : un essai vise le point le plus proche, et une fois ses "
                + "essais épuisés, ou trouvé, le point se cache ailleurs. Un essai donne " + shardText(game.gridValue())
                + " repère" + (game.gridValue() >= 2 ? "s" : "") + " multiplié par la proximité, comptée en part de la grille : "
                + Format.multiplier(game.gridProximityFactor(n * Math.sqrt(3))) + " au plus loin, "
                + Format.multiplier(game.gridProximityFactor(0)) + " sur le point ; la case du point le trouve ("
                + Format.multiplier(Game.GRID_FOUND) + "). Après chaque essai, " + seconds(Game.GRID_COOLDOWN)
                + " s d'attente. La réduction divise la grille par " + ElementText.number(1 / Game.GRID_SHRINK)
                + " par niveau, jusqu'à " + Game.GRID_MIN_CELLS + " cases par côté : moins de cases, plus de chances. "
                + revealText(game.gridReveal())
                + " Les repères, le seuil et les améliorations de la grille traversent les univers ; le seuil marque la "
                + "dimension d'un rond vert, et les repères paient aussi la graine profonde."));
            gridDetail.setVisible(!gridDetail.getText().isEmpty());
            gridDetail.setManaged(gridDetail.isVisible());
        }
        paintUpgrades(gridButtons, this::describe, game::gridLevel, Game.GridUpgrade::max, game::gridCost, game::canBuyGrid,
                " repère", color(8));
    }

    /** Vise la case des trois champs ; ce qui n'est pas un nombre garde la case d'avant sur son axe. */
    void selectFromFields() {
        int[] at = view.gridSelected();
        int x = parsed(gridX), y = parsed(gridY), z = parsed(gridZ);
        view.selectGrid(x < 0 ? at[0] : x, y < 0 ? at[1] : y, z < 0 ? at[2] : z);
    }

    /**
     * Le dernier essai, autant que l'affichage le permet : son gain (au niveau 0, rien d'autre), puis la distance au
     * point visé, sa couche, sa colonne ; et les essais qu'il reste sur ce point. Rien sans essai.
     */
    String lastProbeText() {
        Game.GridProbe probe = game.gridLastProbe();
        if (probe == null) return "";
        StringBuilder text = new StringBuilder(" Dernier essai : ").append(probe.found() ? "trouvé ! " : "")
                .append('+').append(shardText(probe.gain()));
        int reveal = game.gridReveal();
        if (reveal >= 1) text.append(", à ").append(ElementText.number(probe.distance())).append(" cases du point visé");
        int[] revealed = view.gridRevealed();
        if (reveal >= 2 && revealed[0] >= 0) text.append(", dans la couche z = ").append(revealed[0]);
        if (reveal >= 3 && revealed[1] >= 0) text.append(", la colonne x = ").append(revealed[1]);
        if (!probe.found()) text.append(probe.triesLeft() > 0 ? " ; encore " + probe.triesLeft() + (probe.triesLeft() > 1 ? " essais" : " essai")
                + " sur ce point" : " ; plus d'essai : le point s'est caché ailleurs");
        return text.append('.').toString();
    }

    /** Une amélioration de la grille : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.GridUpgrade upgrade) {
        int level = game.gridLevel(upgrade);
        return switch (upgrade) {
            case DIVISION -> "Réduction de la grille : " + Format.whole(game.gridDivisions()) + " → "
                    + Format.whole(Game.cellsAt(level + 1)) + " cases par côté";
            case GAIN -> "Gain : chaque essai vaut " + shardText(game.gridValue()) + " → "
                    + shardText(game.gridValue() + game.hadronBoost(Hadron.OMEGA)) + " repères, avant la proximité";
            case REVEAL -> switch (level) {
                case 0 -> "Affichage du point : rien que le gain → la distance de chaque essai";
                case 1 -> "Affichage du point : la distance → aussi la couche du point visé";
                case 2 -> "Affichage du point : la couche → aussi la colonne du point visé";
                default -> "Affichage du point : distance, couche et colonne";
            };
            case POINTS -> "Nombre de points : " + game.gridPointCount() + " cachés → " + (game.gridPointCount() + 1);
            case PROXIMITY -> "Multiplicateur de proximité : sur le point " + Format.multiplier(game.gridProximityFactor(0)) + " → "
                    + Format.multiplier(game.gridProximityFactor(0) + Game.GRID_PROXIMITY_STEP);
            case ATTEMPTS -> "Essais : " + game.gridAttempts() + " par point → " + (game.gridAttempts() + 1);
        };
    }

    /**
     * Le miroir : ses reflets, son seuil, son plateau (sa taille, et les réussites qu'il manque pour l'agrandir), ses
     * compteurs (longueur, record, réussis, manqués, erreurs), ce qu'il faut faire
     * selon la phase ou ce que fait le miroir automatique, le dernier résultat, le détail, et ses améliorations.
     */
    /** Le temps par case que donne la barre de vitesse du motif. */
    String mirrorPaceText() {
        return "Vitesse du motif : " + seconds(game.mirrorShowSeconds()) + " s par case";
    }

    /** La barre de vitesse du motif du miroir : sa valeur de départ, et qui garder au courant quand le joueur la bouge. */
    public void setMirrorPace(double pace, java.util.function.DoubleConsumer whenChosen) {
        onMirrorPace = whenChosen == null ? chosen -> { } : whenChosen;
        game.setMirrorPace(pace);
        mirrorPace.setValue(game.mirrorPace());
        mirrorPaceText.setText(mirrorPaceText());
    }

    void refreshMirror() {
        // La mémorisation achetée allonge le temps par case : le texte de la barre suit.
        if (changed(mirrorPaceText, mirrorPaceText())) mirrorPaceText.setText(mirrorPaceText());
        mirrorShards.setText("Reflets du miroir : " + shardText(game.state().mirrorShards()));
        threshold(mirrorThreshold, game.isDimensionThresholdReached(9), game.state().mirrorShardsEarned(), Game.MIRROR_THRESHOLD,
                "reflets gagnés", 9);
        mirrorCounts.setText("Motif de " + game.mirrorLength() + " cases · record " + game.mirrorBest() + " · réussis "
                + Format.whole(game.mirrorSuccesses()) + " · manqués " + Format.whole(game.mirrorFails())
                + (game.mirrorAllowedMistakes() > 0 ? " · erreurs " + game.mirrorMistakes() + " / " + game.mirrorAllowedMistakes() : ""));
        int wins = game.mirrorWinsToGrow();
        mirrorBoard.setText("Plateau " + game.mirrorColumns() + " × " + game.mirrorRows() + " — " + (wins <= 0 ? "au plus grand"
                : "encore " + wins + (wins > 1 ? " réussites" : " réussite") + " avant de l'agrandir"));
        String last = switch (game.mirrorLastResult()) {
            case SUCCESS -> " Dernier motif réussi : +" + shardText(game.mirrorLastGain()) + ".";
            case FAIL -> " Dernier motif manqué : +" + shardText(game.mirrorLastGain()) + ".";
            default -> "";
        };
        mirrorHint.setText(game.isDimensionAutoPlayed(9)
                ? "Miroir automatique : il reproduit une case " + cadence(9) + ", sans faute "
                        + "jusqu'à " + Game.MIRROR_AUTO_MAX + " cases, puis se trompe. Vos clics comptent en plus." + last
                : switch (game.mirrorPhase()) {
                    case SHOW -> "Regardez le motif s'allumer à gauche, case après case : vous le referez de mémoire, en miroir, "
                            + "à droite, dans le même ordre." + last;
                    case INPUT -> "De mémoire, cliquez à droite le reflet de chaque case, dans l'ordre : "
                            + game.mirrorPosition() + " / " + game.mirrorLength() + "." + last;
                    case PAUSE -> "Le motif suivant arrive." + last;
                });
        StringBuilder levels = new StringBuilder().append(Detail.shown());
        for (Game.MirrorUpgrade upgrade : Game.MirrorUpgrade.values()) levels.append('/').append(game.mirrorLevel(upgrade));
        levels.append('/').append(game.mirrorGrowth());
        if (changed(mirrorDetail, levels.toString())) {
            mirrorDetail.setText(Detail.only("Le plateau fait " + game.mirrorColumns() + " × " + game.mirrorRows()
                + " cases, coupé au milieu par le miroir. Chaque case du motif reste allumée " + seconds(game.mirrorShowSeconds())
                + " s ; ensuite, le motif s'éteint et attend. Réussi, un motif de L cases donne " + shardText(game.mirrorValue())
                + " × L × " + ElementText.number(game.mirrorLengthFactor()) + " puissance (L − " + Game.MIRROR_START
                + ") reflets (" + shardText(game.mirrorReward(game.mirrorLength())) + " pour le motif en cours), et le suivant a une "
                + "case de plus, jusqu'à " + Game.MIRROR_MAX + " ; manqué, il donne un quart de reflet par case juste, et le suivant "
                + "a une case de moins, jamais moins de " + Game.MIRROR_START + ". "
                + (game.mirrorAllowedMistakes() > 0 ? game.mirrorAllowedMistakes() + (game.mirrorAllowedMistakes() > 1
                        ? " erreurs sont permises" : " erreur est permise") + " par motif. " : "")
                + "Les reflets, le seuil et les améliorations du miroir traversent les univers ; le seuil marque la dimension "
                + "d'un rond vert, et les reflets paient aussi les saisons de la graine. Toutes les " + Game.MIRROR_GROW_EVERY
                + " réussites, le plateau s'agrandit d'une colonne de chaque côté et d'une ligne, et le gain de "
                + Format.multiplier(1 + Game.MIRROR_BOARD) + " de plus, " + game.mirrorMaxGrowth() + " fois au plus (gain "
                + Format.multiplier(1 + Game.MIRROR_BOARD * game.mirrorGrowth()) + " maintenant)."));
            mirrorDetail.setVisible(!mirrorDetail.getText().isEmpty());
            mirrorDetail.setManaged(mirrorDetail.isVisible());
        }
        paintUpgrades(mirrorButtons, this::describe, game::mirrorLevel, Game.MirrorUpgrade::max, game::mirrorCost, game::canBuyMirror,
                " reflet", color(9));
    }

    /**
     * Le mélange : l'étape, sa barre vers l'étape suivante et son bouton ; les fragments, le seuil, les compteurs ; ce
     * qu'on fait à l'étape en cours, ou ce que fait le mélange automatique ; le détail, et les améliorations : celles des
     * étapes pas encore atteintes restent grises, avec l'étape qui les ouvre.
     */
    void refreshMix() {
        int stage = game.mixStage();
        mixStage.setText(mixStageText(stage));
        double goal = game.mixStageGoal(), earned = game.state().mixStageEarned();
        boolean last = stage >= Game.MIX_STAGES;
        double share = last ? 1 : Math.max(0, Math.min(1, earned / goal));
        mixBarFill.setMinWidth(MIX_BAR_WIDTH * share);
        mixBarFill.setPrefWidth(MIX_BAR_WIDTH * share);
        mixBarFill.setMaxWidth(MIX_BAR_WIDTH * share);
        mixBarRest.setMinWidth(MIX_BAR_WIDTH * (1 - share));
        mixBarRest.setPrefWidth(MIX_BAR_WIDTH * (1 - share));
        mixBarRest.setMaxWidth(MIX_BAR_WIDTH * (1 - share));
        mixProgress.setText(last ? "Tout est mêlé" : shardText(Math.min(earned, goal)) + " / " + shardText(goal) + " fragments gagnés à cette étape");
        mixAdvance.setVisible(!last);
        mixAdvance.setManaged(!last);
        mixAdvance.setDisable(!game.canAdvanceMix());
        mixShards.setText("Fragments du mélange : " + shardText(game.state().mixShards()));
        threshold(mixThreshold, game.isDimensionThresholdReached(10), game.state().mixShardsEarned(), Game.MIX_THRESHOLD,
                "fragments gagnés", 10);
        StringBuilder info = new StringBuilder("Malus " + percentOf(game.mixMalus()) + " · étape "
                + Format.multiplier(game.mixStageMultiplier()) + " · une graduation vaut " + ElementText.number(game.mixValue()));
        if (game.isMixing(4)) {
            info.append(" · charge ").append(shardText(game.mixPot())).append(' ').append(Format.multiplier(game.mixChargeMultiplier()));
        }
        if (game.isMixing(7)) {
            info.append(" · cadence ").append(ElementText.number(game.mixClickRate())).append(" clics/s ")
                    .append(Format.multiplier(game.mixClickMultiplier()));
        }
        if (game.isMixing(8)) info.append(" · trésors ").append(game.mixTreasures());
        if (game.isMixing(9)) info.append(" · paires de suite ").append(game.mixPairStreak());
        if (game.mixLastGain() > 0) info.append(" · dernier gain +").append(ElementText.number(game.mixLastGain()));
        mixInfo.setText(info.toString());
        mixHint.setText((game.isDimensionAutoPlayed(10)
                ? "Mélange automatique : " + cadence(10) + ", il mène le point vers l'éclat le plus proche, plonge à sa profondeur, tient et relâche "
                        + "la charge en rythme, clique et passe seul à l'étape suivante. Vous pouvez jouer avec lui. "
                : "") + mixHintFor(stage));
        StringBuilder levels = new StringBuilder().append(Detail.shown()).append('/').append(stage);
        for (Game.MixUpgrade upgrade : Game.MixUpgrade.values()) levels.append('/').append(game.mixLevel(upgrade));
        if (changed(mixDetail, levels.toString())) {
            mixDetail.setText(Detail.only("Chaque graduation que passe la boule vaut le gain (" + (1 + game.mixLevel(Game.MixUpgrade.GAIN))
                    + ", fois les omégas), multiplié par le malus (" + percentOf(game.mixMalus()) + ", au départ de chaque étape "
                    + percentOf(Game.MIX_MALUS) + ") et par les dimensions mêlées (+" + percent(Game.MIX_STAGE_BONUS) + " chacune après la "
                    + "ligne, ici " + Format.multiplier(game.mixStageMultiplier()) + "). "
                    + (stage >= 2 ? "Un éclat vaut " + Math.round(Game.MIX_SHARD_VALUE) + " graduations ; " + Game.MIX_MAX_SHARDS
                            + " au plus sur l'arène. " : "")
                    + (stage >= 3 ? "Un éclat profond vaut jusqu'au double ; le point le ramasse à " + ElementText.number(game.mixDepthTolerance())
                            + " de profondeur près. " : "")
                    + (stage >= 4 ? "La charge monte jusqu'à " + Format.multiplier(1 + 1 + Game.MIX_CHARGE_STEP * game.mixLevel(Game.MixUpgrade.CHARGE))
                            + " en " + seconds(Game.MIX_CHARGE_SECONDS) + " s tenues. " : "")
                    + (stage >= 5 ? "Relâcher dans la zone du cercle : excellent ×3, bien ×1.5, mauvais ×1, hors de la zone ×0.5. " : "")
                    + (stage >= 6 ? "L'onde porte à " + ElementText.number(game.mixWaveReach()) + " et donne ×"
                            + ElementText.number(Game.MIX_WAVE_BOUNCE) + " par rebond, tout de suite. " : "")
                    + (stage >= 7 ? "Chaque clic par seconde ajoute " + percent(Game.MIX_CLICK) + " (" + Math.round(Game.MIX_CLICK_MAX)
                            + " au plus) ; " + game.mixMaxZones() + (game.mixMaxZones() > 1 ? " zones ×2" : " zone ×2") + " à la fois au plus. " : "")
                    + (stage >= 8 ? "Le trésor vaut " + ElementText.number(game.mixTreasureValue()) + " graduations. " : "")
                    + (stage >= 9 ? "Un reflet ramassé juste après son éclat vaut " + Format.multiplier(game.mixPairFactor())
                            + ", +" + percent(Game.MIX_PAIR_STREAK) + " par paire de suite. " : "")
                    + "Passer à l'étape suivante remet à zéro les améliorations du mélange ; les fragments, eux, restent, et paient "
                    + "aussi la bulle des racines de la graine."));
            mixDetail.setVisible(!mixDetail.getText().isEmpty());
            mixDetail.setManaged(mixDetail.isVisible());
        }
        // Les améliorations : la rangée ne se refait que si l'étape, un niveau ou ce qui s'achète change.
        StringBuilder key = new StringBuilder().append(stage);
        for (Game.MixUpgrade upgrade : Game.MixUpgrade.values()) {
            key.append('/').append(game.mixLevel(upgrade)).append(game.canBuyMix(upgrade) ? '+' : '-');
        }
        if (!changed(mixRowKey, key.toString())) return;
        painted.remove(mixButtons);
        paintUpgrades(mixButtons, this::describe, game::mixLevel, Game.MixUpgrade::max, game::mixCost, game::canBuyMix,
                " fragment", color(10));
        for (Game.MixUpgrade upgrade : Game.MixUpgrade.values()) {
            if (stage >= upgrade.stage()) continue;
            Button button = mixButtons.get(upgrade);
            button.setText(mixName(upgrade) + "\nÀ l'étape " + upgrade.stage());
            button.setDisable(true);
            button.setStyle(UPGRADE_STYLE + " -fx-padding: 8 16; -fx-border-width: 1; -fx-text-fill: #5c6580; -fx-background-color: #0b0e17;"
                    + " -fx-border-color: #1f2638; -fx-border-style: dashed;");
        }
    }

    /** Une amélioration du mélange : son nom, où elle en est, et ce que donne le niveau suivant. */
    String describe(Game.MixUpgrade upgrade) {
        int level = game.mixLevel(upgrade);
        return mixName(upgrade) + " : " + switch (upgrade) {
            case GAIN -> (1 + level) + " → " + (2 + level) + " par graduation";
            case MALUS -> "il garde " + percentOf(game.mixMalus()) + " → " + percentOf(Math.min(1, game.mixMalus() * Game.MIX_MALUS_STEP));
            case TICKS -> game.mixTicks() + " → " + (game.mixTicks() + 1) + " sur la ligne";
            case SPEED -> "+" + percent(Game.MIX_CURSOR_STEP * level) + " → +" + percent(Game.MIX_CURSOR_STEP * (level + 1));
            case DEPTH -> "à " + ElementText.number(game.mixDepthTolerance()) + " près → "
                    + ElementText.number(game.mixDepthTolerance() + Game.MIX_DEPTH_STEP);
            case CHARGE -> "jusqu'à " + Format.multiplier(2 + Game.MIX_CHARGE_STEP * level) + " → " + Format.multiplier(2 + Game.MIX_CHARGE_STEP * (level + 1));
            case RHYTHM -> "zone +" + percent(Game.MIX_RING_STEP * level) + " → +" + percent(Game.MIX_RING_STEP * (level + 1));
            case REACH -> "portée " + ElementText.number(game.mixWaveReach()) + " → "
                    + ElementText.number(Game.MIX_WAVE_REACH * (1 + Game.MIX_WAVE_STEP * (level + 1)));
            case ZONES -> (1 + level) + " → " + (2 + level) + " à la fois";
            case TREASURE -> ElementText.number(game.mixTreasureValue()) + " → "
                    + ElementText.number(Game.MIX_TREASURE * (1 + Game.MIX_TREASURE_STEP * (level + 1))) + " graduations";
            case PAIRS -> Format.multiplier(game.mixPairFactor()) + " → " + Format.multiplier(game.mixPairFactor() + Game.MIX_PAIR_STEP);
        };
    }
}
