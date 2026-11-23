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

/** Une partie de {@link GameUniverse} (découpée en 0.293 pour rester sous 1 000 lignes) : la ligne et le plan, les deux premières dimensions. */
public abstract class GameLinePlane extends GameUniverseBase {
    GameLinePlane(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Vrai dès que la première dimension est ouverte : les boules courent, les éclats tombent. */
    public boolean isLineOpen() { return dimensionsUnlocked() >= 1; }

    /** Traits sur chaque ligne, ses deux bouts compris, également espacés. */
    public int lineTicks() { return 2 + state.lineTicksLevel(); }

    /** Boules sur chaque ligne. */
    public int lineBalls() { return 1 + state.lineBallsLevel(); }

    /** Lignes. */
    public int lineCount() { return 1 + state.lineCountLevel(); }

    /** La vitesse des boules, ×1 au départ. */
    public double lineSpeed() { return 1 + LINE_SPEED_STEP * state.lineSpeedLevel(); }

    /** Secondes pour un aller, à la vitesse actuelle. */
    public double lineHalfTrip() { return LINE_HALF_TRIP / lineSpeed(); }

    /** Éclats de chaque trait touché : 0,1, et 0,1 de plus par niveau de l'amélioration. */
    public double lineShardsPerTouch() {
        // Les omégas renforcent les éclats des quatre dimensions.
        return LINE_SHARD * (1 + state.lineYieldLevel()) * hadronBoost(Hadron.OMEGA);
    }

    /**
     * Le décalage d'une boule sur la route (aller puis retour, longueur 2) : les boules d'une même ligne
     * se répartissent également, et chaque ligne est un peu décalée de la précédente.
     */
    public double lineOffset(int line, int ball) { return (2.0 * ball / lineBalls() + 0.5 * line / lineCount()) % 2; }

    /** Où est cette boule de cette ligne : 0 au début, 1 au bout. */
    public double linePosition(int line, int ball) {
        double phase = (state.linePhase() + lineOffset(line, ball)) % 2;
        return phase <= 1 ? phase : 2 - phase;
    }

    /** Où est la première boule de la première ligne. */
    public double linePosition() { return linePosition(0, 0); }

    /** Éclats par seconde : chaque aller d'une boule touche tous les traits sauf celui d'où elle part. */
    public double lineShardsPerSecond() {
        return isLineOpen() ? (lineTicks() - 1) * lineShardsPerTouch() * lineBalls() * lineCount() / lineHalfTrip() : 0;
    }

    /** Vrai une fois le seuil du plan atteint : {@link #PLANE_THRESHOLD} éclats du plan gagnés en tout. */
    public boolean isPlaneThresholdReached() { return state.planeShardsEarned() >= PLANE_THRESHOLD - 1e-9; }

    public boolean isDimensionThresholdReached(int number) {
        return switch (number) {
            case 1 -> isLineThresholdReached();
            case 2 -> isPlaneThresholdReached();
            case 3 -> isVolumeThresholdReached();
            case 4 -> isDurationThresholdReached();
            case 5 -> isLoopThresholdReached();
            case 6 -> state.braneShardsEarned() >= BRANE_THRESHOLD - 1e-9;
            case 7 -> state.clickerShardsEarned() >= CLICKER_THRESHOLD - 1e-9;
            case 8 -> state.gridShardsEarned() >= GRID_THRESHOLD - 1e-9;
            case 9 -> state.mirrorShardsEarned() >= MIRROR_THRESHOLD - 1e-9;
            case 10 -> state.mixShardsEarned() >= MIX_THRESHOLD - 1e-9;
            default -> false;
        };
    }

    /** Vrai une fois le seuil de la ligne atteint : {@link #LINE_THRESHOLD} éclats gagnés en tout. */
    public boolean isLineThresholdReached() { return state.dimensionShardsEarned() >= LINE_THRESHOLD - 1e-9; }

    /**
     * Fait avancer les boules de {@code dt} secondes et donne les éclats des traits touchés. L'aller et
     * le retour mis bout à bout font une seule route de longueur 2 où les traits tombent tous les
     * 1/(traits − 1) : le compte est exact quel que soit le pas, même pour une longue absence.
     */
    void runLine(double dt) {
        if (!isLineOpen() || dt <= 0) return;
        int gaps = lineTicks() - 1;
        double from = state.linePhase(), move = dt / lineHalfTrip();
        long touches = 0;
        for (int line = 0; line < lineCount(); line++) {
            for (int ball = 0; ball < lineBalls(); ball++) {
                double start = from + lineOffset(line, ball);
                touches += (long) Math.floor((start + move) * gaps + 1e-9) - (long) Math.floor(start * gaps + 1e-9);
            }
        }
        if (touches > 0) {
            double gain = touches * lineShardsPerTouch();
            state.setDimensionShards(state.dimensionShards() + gain);
            state.setDimensionShardsEarned(state.dimensionShardsEarned() + gain);
        }
        state.setLinePhase((from + move) % 2);
    }

    /** Le niveau d'une amélioration de la ligne. */
    public int lineLevel(LineUpgrade upgrade) { return state.lineLevel(upgrade.ordinal()); }

    /** Le niveau le plus haut d'une amélioration de la ligne ; sans limite pour la densité. */
    public int lineMaxLevel(LineUpgrade upgrade) { return upgrade.max(); }

    /** Prix du prochain niveau, en éclats ; infini une fois au plus haut. */
    public double lineCost(LineUpgrade upgrade) { return upgrade.cost(lineLevel(upgrade)); }

    public boolean canBuyLine(LineUpgrade upgrade) {
        return isLineOpen() && lineLevel(upgrade) < lineMaxLevel(upgrade) && state.dimensionShards() >= lineCost(upgrade) - 1e-9;
    }

    /** Achète un niveau d'une amélioration de la ligne. */
    public boolean buyLine(LineUpgrade upgrade) {
        if (!canBuyLine(upgrade)) return false;
        state.setDimensionShards(Math.max(0, state.dimensionShards() - lineCost(upgrade)));
        state.setLineLevel(upgrade.ordinal(), lineLevel(upgrade) + 1);
        return true;
    }

    // Les améliorations de densité et de traits sous leur nom de la 0.101 : des tests s'en servent encore.
    public double lineYieldCost() { return lineCost(LineUpgrade.YIELD); }

    public double lineTicksCost() { return lineCost(LineUpgrade.TICKS); }

    public boolean canBuyLineYield() { return canBuyLine(LineUpgrade.YIELD); }

    public boolean canBuyLineTicks() { return canBuyLine(LineUpgrade.TICKS); }

    public boolean buyLineYield() { return buyLine(LineUpgrade.YIELD); }

    public boolean buyLineTicks() { return buyLine(LineUpgrade.TICKS); }

    /** Vrai dès que la deuxième dimension est ouverte. */
    public boolean isPlaneOpen() { return dimensionsUnlocked() >= 2; }

    public int planeLevel(PlaneUpgrade upgrade) { return state.planeLevel(upgrade.ordinal()); }

    /** Éclats du plan que donne chaque éclat ramassé : 1, et 1 de plus par niveau. */
    public double planeValue() { return (1 + planeLevel(PlaneUpgrade.VALUE)) * hadronBoost(Hadron.OMEGA); }

    /** Secondes entre deux apparitions : 3, ×0,85 par niveau. */
    public double planeSpawnSeconds() { return PLANE_SPAWN_SECONDS * Math.pow(0.85, planeLevel(PlaneUpgrade.SPAWN)); }

    /** Éclats présents à la fois, au plus : 3, et 1 de plus par niveau (vingt au plus). */
    public int planeMaxShards() { return PLANE_MAX_SHARDS + planeLevel(PlaneUpgrade.COUNT); }

    /** Vitesse du point du joueur : 0,6 par seconde, +20 % par niveau. */
    public double planeSpeed() { return PLANE_SPEED * (1 + 0.2 * planeLevel(PlaneUpgrade.SPEED)); }

    /** Rayon du point du joueur : 0,025, +25 % par niveau (quinze niveaux : ×4,75). */
    public double planePlayerRadius() {
        return PLANE_PLAYER_RADIUS * (1 + PLANE_SIZE_STEP * planeLevel(PlaneUpgrade.SIZE));
    }

    public double planeX() { return planeX; }

    public double planeY() { return planeY; }

    /** Les éclats posés sur le plan, dans l'ordre où ils sont apparus. */
    public List<PlaneShard> planeShardsOnBoard() { return Collections.unmodifiableList(planeShardsOnBoard); }

    /** Éclats ramassés depuis le lancement : l'interface y voit ceux de la dernière image. */
    public long planeCollected() { return planeCollected; }

    /** La direction que tient le joueur, de −1 à 1 sur chaque axe (ZQSD) ; 0, 0 pour s'arrêter. */
    public void setPlaneInput(double dx, double dy) {
        planeInputX = Math.max(-1, Math.min(1, dx));
        planeInputY = Math.max(-1, Math.min(1, dy));
    }

    /** Pose le point du joueur : pour les essais. */
    public void setPlanePosition(double x, double y) {
        planeX = clampPlane(x, planePlayerRadius());
        planeY = clampPlane(y, planePlayerRadius());
    }

    /**
     * Le plan avance de {@code dt} secondes : un éclat apparaît à son rythme tant qu'il en manque, le point
     * suit la direction tenue sans sortir du plan, et ramasse les éclats qu'il touche.
     */
    void runPlane(double dt) {
        if (!isPlaneOpen() || dt <= 0) return;
        // La visée automatique, sur un grand pas (une absence rejouée par pas de 10 s), se joue par morceaux, comme en jeu
        // ouvert. Avant, un pas ne valait jamais plus d'un quart de seconde de course : sur un pas de 10 s, le
        // point automatique avançait quarante fois moins vite, et le plan ne rapportait presque rien en absence.
        if (dt > PLANE_AUTO_STEP && planeInputX == 0 && planeInputY == 0 && isPlaneAutomatic()) {
            for (double left = dt; left > 1e-12; left -= PLANE_AUTO_STEP) runPlaneStep(Math.min(PLANE_AUTO_STEP, left));
            return;
        }
        runPlaneStep(dt);
    }

    void runPlaneStep(double dt) {
        planeSpawnTimer += dt;
        double every = planeSpawnSeconds();
        while (planeSpawnTimer >= every && planeShardsOnBoard.size() < planeMaxShards()) {
            planeSpawnTimer -= every;
            double margin = PLANE_SHARD_RADIUS * 2;
            planeShardsOnBoard.add(new PlaneShard((planeRandom.nextDouble() * 2 - 1) * (PLANE_EDGE - margin),
                    (planeRandom.nextDouble() * 2 - 1) * (PLANE_EDGE - margin)));
        }
        // Plein : le minuteur attend, il ne s'accumule pas.
        if (planeShardsOnBoard.size() >= planeMaxShards()) planeSpawnTimer = Math.min(planeSpawnTimer, every);
        // La visée automatique ne prend la main que quand le joueur ne tient aucune direction.
        double inputX = planeInputX, inputY = planeInputY;
        if (inputX == 0 && inputY == 0) {
            double[] auto = planeAutoDirection(dt);
            if (auto != null) {
                inputX = auto[0];
                inputY = auto[1];
            }
        }
        double length = Math.hypot(inputX, inputY);
        if (length > 0) {
            double step = planeSpeed() * Math.min(dt, 0.25) / Math.max(1, length);
            setPlanePosition(planeX + inputX * step, planeY + inputY * step);
        }
        double reach = planePlayerRadius() + PLANE_SHARD_RADIUS;
        for (Iterator<PlaneShard> it = planeShardsOnBoard.iterator(); it.hasNext(); ) {
            PlaneShard shard = it.next();
            if (Math.hypot(shard.x() - planeX, shard.y() - planeY) > reach) continue;
            it.remove();
            planeCollected++;
            state.setPlaneShards(state.planeShards() + planeValue());
            state.setPlaneShardsEarned(state.planeShardsEarned() + planeValue());
        }
    }

    /**
     * La direction que donne la visée automatique du plan ; {@code null} sans elle. Elle
     * reçoit le temps écoulé, pour ne reprendre sa direction qu'à son rythme.
     */
    double[] planeAutoDirection(double dt) { return null; }

    /** Vrai quand quelqu'un d'autre que le joueur mène le point du plan : un palier de la visée, un bot. */
    boolean isPlaneAutomatic() { return false; }

    /** Prix du prochain niveau, en éclats du plan ; infini une fois au plus haut. */
    public double planeCost(PlaneUpgrade upgrade) { return upgrade.cost(planeLevel(upgrade)); }

    public boolean canBuyPlane(PlaneUpgrade upgrade) {
        return isPlaneOpen() && planeLevel(upgrade) < upgrade.max() && state.planeShards() >= planeCost(upgrade) - 1e-9;
    }

    public boolean buyPlane(PlaneUpgrade upgrade) {
        if (!canBuyPlane(upgrade)) return false;
        state.setPlaneShards(Math.max(0, state.planeShards() - planeCost(upgrade)));
        state.setPlaneLevel(upgrade.ordinal(), planeLevel(upgrade) + 1);
        return true;
    }
}
