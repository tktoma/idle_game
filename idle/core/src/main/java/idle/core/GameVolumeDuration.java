package idle.core;

import idle.core.state.*;
import idle.core.achievements.*;
import idle.core.collider.*;
import idle.core.darkmatter.*;
import idle.core.universe.*;
import idle.core.elements.*;
import idle.core.sky.*;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameUniverse} (découpée en 0.293 pour rester sous 1 000 lignes) : le volume et la durée, troisième et quatrième dimensions. */
public abstract class GameVolumeDuration extends GameLinePlane {
    GameVolumeDuration(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Vrai dès que la troisième dimension est ouverte : au troisième univers. */
    public boolean isVolumeOpen() { return dimensionsUnlocked() >= 3; }

    public int volumeLevel(VolumeUpgrade upgrade) { return state.volumeLevel(upgrade.ordinal()); }

    /** Durée d'une plongée : 12 s, et 3 s de plus par niveau. */
    public double volumeDiveSeconds() {
        return VOLUME_DIVE_SECONDS + VOLUME_DIVE_STEP * volumeLevel(VolumeUpgrade.TIME);
    }

    /** Éclats du volume que donne chaque boule cueillie : 1, et 1 de plus par niveau. */
    public double volumeValue() { return (1 + volumeLevel(VolumeUpgrade.VALUE)) * hadronBoost(Hadron.OMEGA); }

    /** Secondes entre deux boules : 1,5, ×0,85 par niveau. */
    public double volumeSpawnSeconds() {
        return VOLUME_SPAWN_SECONDS * Math.pow(0.85, volumeLevel(VolumeUpgrade.SPAWN));
    }

    /** Boules présentes à la fois, au plus : 4, et 1 de plus par niveau (vingt au plus). */
    public int volumeMaxBalls() { return VOLUME_MAX_BALLS + volumeLevel(VolumeUpgrade.COUNT); }

    /** Rayon d'une boule : 0,025, +20 % par niveau (quinze niveaux : ×4, 0,1). Plus grosse, elle se vise mieux. */
    public double volumeBallRadius() {
        return VOLUME_BALL_RADIUS * (1 + VOLUME_SIZE_STEP * volumeLevel(VolumeUpgrade.SIZE));
    }

    /** Vrai pendant une plongée : le joueur est au centre du cube. */
    public boolean isInVolume() { return inVolume; }

    /** Secondes qu'il reste avant que le cube rejette le joueur ; 0 hors du cube. */
    public double volumeTimeLeft() { return inVolume ? volumeTimeLeft : 0; }

    /** Les boules du cube, dans l'ordre où elles sont apparues. */
    public List<VolumeBall> volumeBalls() { return Collections.unmodifiableList(volumeBalls); }

    /** Boules cueillies depuis le lancement : l'interface y voit celles de la dernière image. */
    public long volumeCollected() { return volumeCollected; }

    /** Plongées commencées depuis le lancement. */
    public long volumeDives() { return volumeDives; }

    /**
     * Plonge dans le cube : le minuteur part de la durée d'une plongée, et le cube vide reçoit aussitôt sa
     * première boule. Impossible si la dimension est fermée ou si le joueur y est déjà.
     */
    public boolean enterVolume() {
        if (!isVolumeOpen() || inVolume) return false;
        inVolume = true;
        volumeTimeLeft = volumeDiveSeconds();
        volumeBalls.clear();
        volumeBalls.add(spawnVolumeBall());
        volumeSpawnTimer = 0;
        volumeDives++;
        return true;
    }

    /** Sort du cube : les boules qui restaient s'en vont avec la plongée. */
    public void leaveVolume() {
        inVolume = false;
        volumeTimeLeft = 0;
        volumeBalls.clear();
        volumeSpawnTimer = 0;
    }

    /**
     * Le volume avance de {@code dt} secondes pendant une plongée : les boules apparaissent à leur rythme tant
     * qu'il en manque, et le minuteur descend ; à zéro, le cube rejette le joueur.
     */
    void runVolume(double dt) {
        // La plongée automatique, sur un grand pas, se joue par morceaux, comme en jeu ouvert. D'un coup,
        // un pas de 10 s (une absence rejouée) laissait expirer la plongée avant de replonger, et le volume rapportait
        // moitié moins en absence.
        if (dt > PLANE_AUTO_STEP && isVolumeAutomatic() && isVolumeOpen()) {
            for (double left = dt; left > 1e-12; left -= PLANE_AUTO_STEP) runVolumeStep(Math.min(PLANE_AUTO_STEP, left));
            return;
        }
        runVolumeStep(dt);
    }

    void runVolumeStep(double dt) {
        // La plongée automatique : elle replonge dès qu'elle sort, et vise une boule à son rythme.
        if (isVolumeAutomatic() && isVolumeOpen() && dt > 0) {
            if (!inVolume) enterVolume();
            double every = volumeShotPeriod();
            volumeShotTimer += dt;
            while (volumeShotTimer >= every && !volumeBalls.isEmpty()) {
                volumeShotTimer -= every;
                VolumeBall ball = volumeBalls.get(0);
                shootVolume(ball.x(), ball.y(), ball.z());
            }
            if (volumeBalls.isEmpty()) volumeShotTimer = Math.min(volumeShotTimer, every);
        }
        if (!inVolume || dt <= 0) return;
        if (!isVolumeOpen()) {
            leaveVolume();
            return;
        }
        // Les boules n'apparaissent que dans le temps qui reste : une longue image ne remplit pas un cube déjà fini.
        double lived = Math.min(dt, volumeTimeLeft);
        volumeSpawnTimer += lived;
        double every = volumeSpawnSeconds();
        while (volumeSpawnTimer >= every && volumeBalls.size() < volumeMaxBalls()) {
            volumeSpawnTimer -= every;
            volumeBalls.add(spawnVolumeBall());
        }
        // Plein : le minuteur attend, il ne s'accumule pas.
        if (volumeBalls.size() >= volumeMaxBalls()) volumeSpawnTimer = Math.min(volumeSpawnTimer, every);
        volumeTimeLeft -= dt;
        if (volumeTimeLeft <= 1e-9) leaveVolume();
    }

    /** Une boule à un endroit tiré au hasard dans le cube, entre {@link #VOLUME_SPAWN_NEAR} et {@link #VOLUME_SPAWN_FAR} du centre. */
    VolumeBall spawnVolumeBall() {
        double limit = VOLUME_EDGE - volumeBallRadius();
        for (int tries = 0; tries < 64; tries++) {
            double x = (volumeRandom.nextDouble() * 2 - 1) * limit;
            double y = (volumeRandom.nextDouble() * 2 - 1) * limit;
            double z = (volumeRandom.nextDouble() * 2 - 1) * limit;
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d >= VOLUME_SPAWN_NEAR && d <= VOLUME_SPAWN_FAR) return new VolumeBall(x, y, z);
        }
        return new VolumeBall(0, 0, (VOLUME_SPAWN_NEAR + VOLUME_SPAWN_FAR) / 2);
    }

    /**
     * Cueille la boule que touche le regard lancé du centre du cube dans la direction ({@code dx}, {@code dy},
     * {@code dz}) : la plus proche que le rayon traverse. Rend la boule cueillie, ou {@code null} s'il n'y en a pas
     * (hors du cube, direction nulle, rayon qui passe à côté).
     */
    public VolumeBall shootVolume(double dx, double dy, double dz) {
        if (!inVolume) return null;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(length > 1e-12) || !Double.isFinite(length)) return null;
        double ux = dx / length, uy = dy / length, uz = dz / length, radius = volumeBallRadius();
        VolumeBall hit = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (VolumeBall ball : volumeBalls) {
            double along = ball.x() * ux + ball.y() * uy + ball.z() * uz;
            if (along <= 0) continue;
            double aside = ball.x() * ball.x() + ball.y() * ball.y() + ball.z() * ball.z() - along * along;
            if (aside <= radius * radius + 1e-12 && along < nearest) {
                nearest = along;
                hit = ball;
            }
        }
        if (hit == null) return null;
        volumeBalls.remove(hit);
        volumeCollected++;
        state.setVolumeShards(state.volumeShards() + volumeValue());
        state.setVolumeShardsEarned(state.volumeShardsEarned() + volumeValue());
        return hit;
    }

    /**
     * Le temps entre deux boules cueillies par la plongée automatique (version 0.209 : celui de son palier ; 0,4 s avant,
     * pour la seule plongée d'alors).
     */
    double volumeShotPeriod() { return Double.POSITIVE_INFINITY; }

    /** Vrai quand la plongée automatique joue le volume. */
    boolean isVolumeAutomatic() { return false; }

    /** Pose une boule dans le cube : pour les essais. */
    public void addVolumeBall(double x, double y, double z) {
        if (volumeBalls.size() < volumeMaxBalls()) volumeBalls.add(new VolumeBall(x, y, z));
    }

    /** Outil d'essai : retire toutes les boules du cube. */
    public void testClearVolume() {
        volumeBalls.clear();
        volumeSpawnTimer = 0;
    }

    /** Vrai une fois le seuil du volume atteint : {@link #VOLUME_THRESHOLD} éclats du volume gagnés en tout. */
    public boolean isVolumeThresholdReached() { return state.volumeShardsEarned() >= VOLUME_THRESHOLD - 1e-9; }

    /** Prix du prochain niveau, en éclats du volume ; infini une fois au plus haut. */
    public double volumeCost(VolumeUpgrade upgrade) { return upgrade.cost(volumeLevel(upgrade)); }

    /** Les améliorations du volume s'achètent hors du cube, jamais pendant une plongée. */
    public boolean canBuyVolume(VolumeUpgrade upgrade) {
        return isVolumeOpen() && !inVolume && volumeLevel(upgrade) < upgrade.max()
                && state.volumeShards() >= volumeCost(upgrade) - 1e-9;
    }

    public boolean buyVolume(VolumeUpgrade upgrade) {
        if (!canBuyVolume(upgrade)) return false;
        state.setVolumeShards(Math.max(0, state.volumeShards() - volumeCost(upgrade)));
        state.setVolumeLevel(upgrade.ordinal(), volumeLevel(upgrade) + 1);
        return true;
    }

    /** La durée s'ouvre avec le quatrième univers : une dimension par univers. */
    public boolean isDurationOpen() { return dimensionsUnlocked() >= 4; }

    public int durationLevel(DurationUpgrade upgrade) { return state.durationLevel(upgrade.ordinal()); }

    /** Le temps au bout duquel les spirales atteignent le centre : 6 s, et 2 s de plus par niveau. */
    public double durationMaxSeconds() {
        return DURATION_MAX_SECONDS + DURATION_TIME_STEP * durationLevel(DurationUpgrade.TIME);
    }

    /** Les tours que fait chaque spirale par seconde tenue : 0,5, ×1,2 par niveau de vitesse. */
    public double durationTurnsPerSecond() {
        return DURATION_TURNS * Math.pow(1 + DURATION_SPEED_STEP, durationLevel(DurationUpgrade.SPEED));
    }

    /** Les spirales qui avancent ensemble : 1, et une de plus par niveau. */
    public int durationSpirals() { return 1 + durationLevel(DurationUpgrade.COUNT); }

    /** Ce que vaut le premier tour d'une spirale ; le n-ième vaut n fois autant. */
    public double durationValue() { return (1 + durationLevel(DurationUpgrade.VALUE)) * hadronBoost(Hadron.OMEGA); }

    /** Les tours d'une spirale entière, du bord au centre. */
    public double durationTurnsMax() { return durationTurnsPerSecond() * durationMaxSeconds(); }

    /** Ce que donne un relâchement après {@code seconds} secondes tenues : chaque spirale, (n² + n) ÷ 2 fois la valeur pour n tours. */
    public double durationRewardFor(double seconds) {
        double turns = durationTurnsPerSecond() * Math.max(0, Math.min(seconds, durationMaxSeconds()));
        return durationSpirals() * durationValue() * (turns * turns + turns) / 2;
    }

    public boolean isDurationHeld() { return durationHeld; }

    /** Depuis combien de secondes le bouton est tenu, sans dépasser le temps au plus. */
    public double durationHeldSeconds() {
        // L'appui automatique compte aussi son attente avant l'appui suivant ; le bouton, lui, est au plus plein.
        return durationHeld ? Math.min(durationMaxSeconds(), durationHeldSeconds) : 0;
    }

    /** Où en sont les spirales : 0 au bord, 1 au centre. */
    public double durationProgress() {
        return durationHeld ? Math.min(1, durationHeldSeconds / durationMaxSeconds()) : 0;
    }

    /** Ce que donnerait un relâchement maintenant. */
    public double durationPending() { return durationHeld ? durationRewardFor(durationHeldSeconds) : 0; }

    /** Les relâchements qui ont donné des éclats, et ce qu'a donné le dernier : pour la fenêtre. */
    public long durationReleases() { return durationReleases; }

    public double durationLastReward() { return durationLastReward; }

    /** Le bouton est enfoncé : les spirales repartent du bord. Faux si la dimension est fermée ou déjà tenue. */
    public boolean holdDuration() {
        if (!isDurationOpen() || durationHeld) return false;
        durationHeld = true;
        durationHeldSeconds = 0;
        return true;
    }

    /** Le bouton est relâché : les spirales donnent leurs éclats et repartent du bord. Rend les éclats donnés. */
    public double releaseDuration() {
        if (!durationHeld) return 0;
        double reward = isDurationOpen() ? durationRewardFor(durationHeldSeconds) : 0;
        durationHeld = false;
        durationHeldSeconds = 0;
        if (reward > 0) {
            state.setDurationShards(state.durationShards() + reward);
            state.setDurationShardsEarned(state.durationShardsEarned() + reward);
            durationReleases++;
            durationLastReward = reward;
        }
        return reward;
    }

    /** Le bouton lâché sans rien donner : une partie relue, un outil d'essai. */
    public void dropDuration() {
        durationHeld = false;
        durationHeldSeconds = 0;
    }

    /** Le temps passe pour le bouton tenu, jusqu'au temps au plus. */
    void runDuration(double dt) {
        // L'appui automatique : tenu jusqu'au centre, relâché, repris, autant de fois que le temps le permet.
        if (isDurationAutomatic() && isDurationOpen() && dt > 0) {
            if (!durationHeld) {
                durationHeld = true;
                durationHeldSeconds = 0;
            }
            // Entre deux appuis, le temps d'une action de son palier (presque rien pour le dernier). Le temps
            // tenu compte ce délai : un tour dure le temps au plus, plus ce délai, et ne paie que le temps au plus.
            double max = durationMaxSeconds(), delay = durationPressDelay(), lap = max + delay, total = durationHeldSeconds + dt;
            long cycles = (long) Math.floor(total / lap + 1e-9);
            if (cycles > 0) {
                double each = durationRewardFor(max), reward = each * cycles;
                state.setDurationShards(state.durationShards() + reward);
                state.setDurationShardsEarned(state.durationShardsEarned() + reward);
                durationReleases += cycles;
                durationLastReward = each;
                total = Math.max(0, total - cycles * lap);
            }
            durationHeldSeconds = total;
            return;
        }
        if (!durationHeld || dt <= 0) return;
        if (!isDurationOpen()) {
            durationHeld = false;
            durationHeldSeconds = 0;
            return;
        }
        durationHeldSeconds = Math.min(durationMaxSeconds(), durationHeldSeconds + dt);
    }

    /** Vrai quand l'appui automatique joue la durée. */
    boolean isDurationAutomatic() { return false; }

    /** Le temps entre un relâcher et l'appui suivant de l'appui automatique : celui de son palier. */
    double durationPressDelay() { return 0; }

    /** Vrai une fois le seuil de la durée atteint : {@link #DURATION_THRESHOLD} éclats de la durée gagnés en tout. */
    public boolean isDurationThresholdReached() { return state.durationShardsEarned() >= DURATION_THRESHOLD - 1e-9; }

    /** Prix du prochain niveau, en éclats de la durée ; infini une fois au plus haut. */
    public double durationCost(DurationUpgrade upgrade) { return upgrade.cost(durationLevel(upgrade)); }

    /**
     * Les améliorations de la durée s'achètent bouton relâché : les spirales ne changent pas en cours de route. Sauf
     * pendant l'appui automatique, qui ne relâche jamais : elles valent alors dès le cycle en cours.
     */
    public boolean canBuyDuration(DurationUpgrade upgrade) {
        return isDurationOpen() && (!durationHeld || isDurationAutomatic()) && durationLevel(upgrade) < upgrade.max()
                && state.durationShards() >= durationCost(upgrade) - 1e-9;
    }

    public boolean buyDuration(DurationUpgrade upgrade) {
        if (!canBuyDuration(upgrade)) return false;
        state.setDurationShards(Math.max(0, state.durationShards() - durationCost(upgrade)));
        state.setDurationLevel(upgrade.ordinal(), durationLevel(upgrade) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // La graine qui germe
    // ------------------------------------------------------------------

    /**
     * Vrai si la graine peut être arrosée : l'univers est formé (il n'y a qu'une graine, la sienne) et elle n'est pas
     * encore l'arbre-monde. Les seuils des dimensions ne la retiennent plus (de la 0.103 à la 0.138, il
     * fallait l'objectif de la dimension 1 pour la faire germer, puis le seuil de la dimension de chaque palier) ; ils
     * ne donnent plus que le rond vert de leur dimension.
     */
    public boolean canTendSeed() { return isUniverseFormed() && seedStage() < SEED_STAGES; }
}
