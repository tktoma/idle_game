package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Le clicker, septième dimension (version 0.155) : une surface carrée, de 0 à 1 sur chaque axe, où chaque clic donne
 * des étincelles (la ressource). Plus le joueur clique vite, plus chaque clic rapporte : la cadence se compte sur les
 * deux dernières secondes. Une fois débloquées, des zones ×2 apparaissent un temps sur la surface ; elles peuvent se
 * superposer, et un clic là où plusieurs se chevauchent rapporte d'autant plus. Améliorations ({@link ClickerUpgrade}) :
 * gain, multiplicateur de cadence, zones, multiplicateur des zones, durée des zones, nombre de zones, taille des zones,
 * multiplicateur des zones superposées.
 *
 * <p>Avec le clicker vient la météo de la graine : un ciel variable, parfois beau (un bonus sur tout l'arrosage), parfois
 * orageux (un léger malus). Trois améliorations de la graine, payées en étincelles, allongent le beau temps, raccourcissent
 * les orages et renforcent le bonus.
 */
public abstract class GameClicker extends GameBrane {

    GameClicker(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Les améliorations du clicker, dans l'ordre de la sauvegarde. */
    public enum ClickerUpgrade implements Priced {
        /** Chaque clic donne une étincelle de plus par niveau. */
        GAIN(5, 3, Integer.MAX_VALUE),
        /** Ce que chaque clic par seconde ajoute : +5 %, et 2,5 points de plus par niveau. */
        SPEED(6, 2.6, 20),
        /** Débloque les zones ×2. */
        ZONES(25, 1, 1),
        /** Le multiplicateur d'une zone : ×2, et 0,5 de plus par niveau. */
        ZONE_MULT(15, 3, 15),
        /** Ce que dure une zone : 4 s, et 1,5 s de plus par niveau. */
        ZONE_TIME(10, 2.6, 15),
        /** Le nombre de zones à la fois : 1, et une de plus par niveau. */
        ZONE_COUNT(30, 3, 7),
        /** La taille des zones : +10 % par niveau. */
        ZONE_SIZE(12, 2.7, 15),
        /** Chaque zone superposée de plus : ×1,5, et 0,25 de plus par niveau. */
        STACK(40, 3.2, 15);

        private final Price price;

        ClickerUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }

        /** Vrai pour celles qui demandent les zones débloquées. */
        public boolean needsZones() {
            return ordinal() > ZONES.ordinal();
        }
    }

    /** Une zone posée sur la surface : son centre, son rayon, et le temps qu'il lui reste. */
    public static final class ClickerZone {
        private final double x, y, radius, lifetime;
        private double left;

        ClickerZone(double x, double y, double radius, double lifetime) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.lifetime = lifetime;
            this.left = lifetime;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double radius() {
            return radius;
        }

        /** Le temps qu'il lui reste, en secondes. */
        public double left() {
            return left;
        }

        /** Sa durée entière, en secondes. */
        public double lifetime() {
            return lifetime;
        }

        /** Vrai si le point (px, py) est dedans. */
        public boolean contains(double px, double py) {
            return Math.hypot(px - x, py - y) <= radius + 1e-12;
        }
    }

    /** La cadence se compte sur les {@value} dernières secondes. */
    public static final double CLICKER_WINDOW = 2;
    /** Au-delà de {@value} clics par seconde, la cadence ne compte plus davantage. */
    public static final double CLICKER_MAX_RATE = 20;
    /** Ce que chaque clic par seconde ajoute : +5 %, et 2,5 points par niveau. */
    public static final double CLICKER_SPEED = 0.05, CLICKER_SPEED_STEP = 0.025;
    /** Une zone : ×2, +0,5 par niveau ; 4 s, +1,5 s par niveau ; rayon 0,1, +10 % par niveau. */
    public static final double CLICKER_ZONE = 2, CLICKER_ZONE_STEP = 0.5, CLICKER_ZONE_SECONDS = 4,
            CLICKER_ZONE_SECONDS_STEP = 1.5, CLICKER_ZONE_RADIUS = 0.1, CLICKER_ZONE_SIZE_STEP = 0.1;
    /** Une nouvelle zone toutes les {@value} secondes, tant qu'il en manque. */
    public static final double CLICKER_ZONE_EVERY = 3;
    /** Chaque zone superposée de plus : ×1,5, +0,25 par niveau. */
    public static final double CLICKER_STACK = 1.5, CLICKER_STACK_STEP = 0.25;
    /** Les clics automatiques : six par seconde, au meilleur endroit. */
    public static final double CLICKER_AUTO_RATE = 6;
    /** En jeu ouvert (pas au plus de {@value} s), l'automatisme clique vraiment ; au-delà, il compte ses clics d'un coup. */
    public static final double CLICKER_PLAYED_STEP = 1;

    private final java.util.Random clickerRandom = new java.util.Random(7);
    private final List<ClickerZone> clickerZones = new ArrayList<>();
    private final ArrayDeque<Double> clickerRecent = new ArrayDeque<>();
    private double clickerClock, clickerZoneTimer, clickerAutoTimer, clickerLastGain;
    private long clickerClicks, clickerAutoClicks;
    private int clickerBestStack;

    /** Vrai dès que la septième dimension est ouverte : au septième univers. */
    public boolean isClickerOpen() {
        return dimensionsUnlocked() >= 7;
    }

    public int clickerLevel(ClickerUpgrade upgrade) {
        return state.clickerLevel(upgrade.ordinal());
    }

    /** Ce que donne un clic, avant la cadence et les zones : 1, plus 1 par niveau de gain, et le bonus des omégas. */
    public double clickerValue() {
        return (1 + clickerLevel(ClickerUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA);
    }

    /** Les clics par seconde, comptés sur les deux dernières secondes. */
    public double clickerRate() {
        return clickerRecent.size() / CLICKER_WINDOW;
    }

    /** Ce que la cadence ajoute pour chaque clic par seconde. */
    public double clickerSpeedStep() {
        return CLICKER_SPEED + CLICKER_SPEED_STEP * clickerLevel(ClickerUpgrade.SPEED);
    }

    /** Le multiplicateur de cadence pour {@code rate} clics par seconde. */
    public double clickerSpeedFactor(double rate) {
        return 1 + clickerSpeedStep() * Math.min(CLICKER_MAX_RATE, Math.max(0, rate));
    }

    public boolean areClickerZonesUnlocked() {
        return clickerLevel(ClickerUpgrade.ZONES) > 0;
    }

    /** Le multiplicateur d'une zone. */
    public double clickerZoneFactor() {
        return CLICKER_ZONE + CLICKER_ZONE_STEP * clickerLevel(ClickerUpgrade.ZONE_MULT);
    }

    /** Ce que donne chaque zone superposée de plus. */
    public double clickerStackFactor() {
        return CLICKER_STACK + CLICKER_STACK_STEP * clickerLevel(ClickerUpgrade.STACK);
    }

    /** Le multiplicateur d'un clic dans {@code stack} zones superposées : 1 hors des zones. */
    public double clickerStackMultiplier(int stack) {
        return stack <= 0 ? 1 : clickerZoneFactor() * Math.pow(clickerStackFactor(), stack - 1);
    }

    public double clickerZoneSeconds() {
        return CLICKER_ZONE_SECONDS + CLICKER_ZONE_SECONDS_STEP * clickerLevel(ClickerUpgrade.ZONE_TIME);
    }

    public double clickerZoneRadius() {
        return CLICKER_ZONE_RADIUS * (1 + CLICKER_ZONE_SIZE_STEP * clickerLevel(ClickerUpgrade.ZONE_SIZE));
    }

    /** Les zones à la fois au plus : 0 avant de les débloquer, puis 1 et une de plus par niveau. */
    public int clickerMaxZones() {
        return areClickerZonesUnlocked() ? 1 + clickerLevel(ClickerUpgrade.ZONE_COUNT) : 0;
    }

    public List<ClickerZone> clickerZones() {
        return Collections.unmodifiableList(clickerZones);
    }

    /** Combien de zones couvrent le point (x, y). */
    public int clickerStackAt(double x, double y) {
        int stack = 0;
        for (ClickerZone zone : clickerZones) if (zone.contains(x, y)) stack++;
        return stack;
    }

    /** Ce que rapporterait un clic en (x, y) maintenant, cadence comprise (le clic lui-même compté). */
    public double clickerGainAt(double x, double y) {
        return clickerValue() * clickerSpeedFactor((clickerRecent.size() + 1) / CLICKER_WINDOW) * clickerStackMultiplier(clickerStackAt(x, y));
    }

    public long clickerClicks() {
        return clickerClicks;
    }

    /** Les clics de l'automatisme depuis le lancement. */
    public long clickerAutoClicks() {
        return clickerAutoClicks;
    }

    public double clickerLastGain() {
        return clickerLastGain;
    }

    /** Le plus de zones superposées sous un clic depuis le lancement. */
    public int clickerBestStack() {
        return clickerBestStack;
    }

    /**
     * Un clic en ({@code x}, {@code y}) : il compte pour la cadence, puis rapporte selon elle et les zones qui couvrent
     * le point. Rend ce qu'il a donné ; 0 si la dimension est fermée.
     */
    public double clickClicker(double x, double y) {
        if (!isClickerOpen()) return 0;
        double gain = clickerGainAt(x, y);
        clickerRecent.addLast(clickerClock);
        int stack = clickerStackAt(x, y);
        clickerBestStack = Math.max(clickerBestStack, stack);
        clickerClicks++;
        clickerLastGain = gain;
        state.setClickerShards(state.clickerShards() + gain);
        state.setClickerShardsEarned(state.clickerShardsEarned() + gain);
        return gain;
    }

    /** Le meilleur endroit où cliquer : le centre de zone (ou le milieu) que couvrent le plus de zones. */
    public double[] clickerBestSpot() {
        double[] best = {0.5, 0.5};
        int most = clickerStackAt(0.5, 0.5);
        for (ClickerZone zone : clickerZones) {
            int stack = clickerStackAt(zone.x(), zone.y());
            if (stack > most) {
                most = stack;
                best = new double[] {zone.x(), zone.y()};
            }
        }
        return best;
    }

    /** Pose une zone : pour les essais. */
    public void addClickerZone(double x, double y) {
        clickerZones.add(new ClickerZone(x, y, clickerZoneRadius(), clickerZoneSeconds()));
    }

    /** Outil d'essai : retire les zones et oublie la cadence. */
    public void testClearClicker() {
        clickerZones.clear();
        clickerRecent.clear();
        clickerZoneTimer = 0;
    }

    /**
     * Le clicker avance de {@code dt} secondes : la cadence oublie les clics trop vieux, les zones s'usent et de
     * nouvelles apparaissent, et l'automatisme clique au meilleur endroit. Un grand pas se découpe en morceaux d'une
     * demi-seconde, pour que les zones vivent et meurent comme en jeu ouvert.
     */
    void runClicker(double dt) {
        if (!isClickerOpen() || dt <= 0) return;
        boolean auto = isClickerAutomatic();
        if (auto && dt > CLICKER_PLAYED_STEP) {
            // Absence : une cadence tenue de six clics par seconde, au meilleur endroit, morceau par morceau.
            double left = dt;
            for (int piece = 0; left > 1e-12 && piece < 10_000; piece++) {
                double step = Math.min(0.5, left);
                advanceClicker(step);
                double[] spot = clickerBestSpot();
                double clicks = CLICKER_AUTO_RATE * step;
                double gain = clickerValue() * clickerSpeedFactor(CLICKER_AUTO_RATE) * clickerStackMultiplier(clickerStackAt(spot[0], spot[1])) * clicks;
                state.setClickerShards(state.clickerShards() + gain);
                state.setClickerShardsEarned(state.clickerShardsEarned() + gain);
                clickerAutoClicks += Math.round(clicks);
                clickerClicks += Math.round(clicks);
                left -= step;
            }
            clickerRecent.clear();
            return;
        }
        advanceClicker(dt);
        if (auto) {
            clickerAutoTimer += dt;
            while (clickerAutoTimer >= 1 / CLICKER_AUTO_RATE) {
                clickerAutoTimer -= 1 / CLICKER_AUTO_RATE;
                double[] spot = clickerBestSpot();
                clickClicker(spot[0], spot[1]);
                clickerAutoClicks++;
            }
        }
    }

    private void advanceClicker(double dt) {
        clickerClock += dt;
        while (!clickerRecent.isEmpty() && clickerRecent.peekFirst() < clickerClock - CLICKER_WINDOW) clickerRecent.removeFirst();
        for (java.util.Iterator<ClickerZone> it = clickerZones.iterator(); it.hasNext(); ) {
            ClickerZone zone = it.next();
            zone.left -= dt;
            if (zone.left <= 0) it.remove();
        }
        if (clickerZones.size() >= clickerMaxZones()) {
            clickerZoneTimer = Math.min(clickerZoneTimer + dt, CLICKER_ZONE_EVERY);
            return;
        }
        clickerZoneTimer += dt;
        while (clickerZoneTimer >= CLICKER_ZONE_EVERY && clickerZones.size() < clickerMaxZones()) {
            clickerZoneTimer -= CLICKER_ZONE_EVERY;
            double r = clickerZoneRadius();
            // Près des autres une fois sur deux, pour que les zones se superposent souvent.
            double x, y;
            if (!clickerZones.isEmpty() && clickerRandom.nextBoolean()) {
                ClickerZone near = clickerZones.get(clickerRandom.nextInt(clickerZones.size()));
                x = near.x() + (clickerRandom.nextDouble() * 2 - 1) * near.radius();
                y = near.y() + (clickerRandom.nextDouble() * 2 - 1) * near.radius();
            } else {
                x = r + clickerRandom.nextDouble() * (1 - 2 * r);
                y = r + clickerRandom.nextDouble() * (1 - 2 * r);
            }
            clickerZones.add(new ClickerZone(Math.max(r, Math.min(1 - r, x)), Math.max(r, Math.min(1 - r, y)), r, clickerZoneSeconds()));
        }
    }

    /** Vrai quand l'automatisme du clicker joue (automatisation des dimensions, version 0.155). */
    boolean isClickerAutomatic() {
        return isDimensionAutomationRunning(idle.core.universe.DimensionAutomation.CLICKER_PLAY);
    }

    /** Prix du prochain niveau, en étincelles ; infini une fois au plus haut. */
    public double clickerCost(ClickerUpgrade upgrade) {
        return upgrade.cost(clickerLevel(upgrade));
    }

    public boolean canBuyClicker(ClickerUpgrade upgrade) {
        return isClickerOpen() && (!upgrade.needsZones() || areClickerZonesUnlocked()) && clickerLevel(upgrade) < upgrade.max()
                && state.clickerShards() >= clickerCost(upgrade) - 1e-9;
    }

    public boolean buyClicker(ClickerUpgrade upgrade) {
        if (!canBuyClicker(upgrade)) return false;
        state.setClickerShards(Math.max(0, state.clickerShards() - clickerCost(upgrade)));
        state.setClickerLevel(upgrade.ordinal(), clickerLevel(upgrade) + 1);
        return true;
    }

    @Override
    boolean buyClickerUpgrade(int index) {
        return buyClicker(ClickerUpgrade.values()[index]);
    }

    // ------------------------------------------------------------------
    // La météo, pour la graine
    // ------------------------------------------------------------------

    /** Le temps qu'il fait sur la graine. */
    public enum Weather {
        VARIABLE("Variable"),
        SUN("Beau temps"),
        STORM("Orage");

        private final String label;

        Weather(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Le ciel variable dure 60 s ; ensuite, beau temps deux fois sur trois, orage sinon. */
    public static final double WEATHER_VARIABLE_SECONDS = 60, WEATHER_SUN_CHANCE = 2.0 / 3;
    /** Le beau temps : 40 s, +10 s par niveau ; +30 % d'arrosage, +10 points par niveau. */
    public static final double WEATHER_SUN_SECONDS = 40, WEATHER_SUN_SECONDS_STEP = 10, WEATHER_SUN_GAIN = 0.3, WEATHER_SUN_GAIN_STEP = 0.1;
    /** L'orage : −10 % d'arrosage, 30 s, −3 s par niveau. */
    public static final double WEATHER_STORM_LOSS = 0.1, WEATHER_STORM_SECONDS = 30, WEATHER_STORM_SECONDS_STEP = 3;

    private Weather weather = Weather.VARIABLE;
    private double weatherLeft = WEATHER_VARIABLE_SECONDS;
    private long sunnySpells, storms;

    /** Vrai une fois la météo là : le clicker ouvert, l'univers formé (elle tombe sur la graine). */
    public boolean isWeatherUnlocked() {
        return isClickerOpen() && isUniverseFormed();
    }

    /** Le temps qu'il fait ; variable tant que la météo n'est pas là. */
    public Weather weather() {
        return isWeatherUnlocked() ? weather : Weather.VARIABLE;
    }

    /** Ce qu'il reste au temps qu'il fait, en secondes. */
    public double weatherLeft() {
        return isWeatherUnlocked() ? Math.max(0, weatherLeft) : 0;
    }

    public double weatherSunSeconds() {
        return WEATHER_SUN_SECONDS + WEATHER_SUN_SECONDS_STEP * seedUpgradeLevel(SeedUpgrade.WEATHER_SUN);
    }

    public double weatherSunGain() {
        return WEATHER_SUN_GAIN + WEATHER_SUN_GAIN_STEP * seedUpgradeLevel(SeedUpgrade.WEATHER_POWER);
    }

    public double weatherStormSeconds() {
        return Math.max(3, WEATHER_STORM_SECONDS - WEATHER_STORM_SECONDS_STEP * seedUpgradeLevel(SeedUpgrade.WEATHER_STORM));
    }

    /** Le multiplicateur de la météo sur l'arrosage : 1 par temps variable. */
    public double weatherFactor() {
        return switch (weather()) {
            case SUN -> 1 + weatherSunGain();
            case STORM -> 1 - WEATHER_STORM_LOSS;
            case VARIABLE -> 1;
        };
    }

    public long sunnySpells() {
        return sunnySpells;
    }

    public long storms() {
        return storms;
    }

    /** L'arrosage par seconde, sous la météo : un bonus au beau temps, un léger malus à l'orage, sur tout le reste. */
    @Override
    public double wateringBoost() {
        return super.wateringBoost() * weatherFactor();
    }

    void runWeather(double dt) {
        if (!isWeatherUnlocked() || dt <= 0) return;
        double left = dt;
        for (int turn = 0; left > 0 && turn < 100_000; turn++) {
            if (weatherLeft > left) {
                weatherLeft -= left;
                return;
            }
            left -= Math.max(0, weatherLeft);
            if (weather == Weather.VARIABLE) {
                if (clickerRandom.nextDouble() < WEATHER_SUN_CHANCE) {
                    weather = Weather.SUN;
                    weatherLeft = weatherSunSeconds();
                    sunnySpells++;
                } else {
                    weather = Weather.STORM;
                    weatherLeft = weatherStormSeconds();
                    storms++;
                }
            } else {
                weather = Weather.VARIABLE;
                weatherLeft = WEATHER_VARIABLE_SECONDS;
            }
        }
    }

    /** Outil d'essai : impose le temps qu'il fait, pour sa durée entière. */
    public void testSetWeather(Weather next) {
        weather = next;
        weatherLeft = switch (next) {
            case SUN -> weatherSunSeconds();
            case STORM -> weatherStormSeconds();
            case VARIABLE -> WEATHER_VARIABLE_SECONDS;
        };
    }
}
