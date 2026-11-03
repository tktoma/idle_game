package idle.core.history;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ce qui a été acheté entre deux photos de la partie, et ce que chaque achat a coûté (version 0.165, venu du bot
 * analyste de la 0.161 pour servir aussi à l'historique des achats du joueur). Seules les clés d'achat comptent
 * ({@link #achat}) : améliorations, automatismes, arbre, générateurs, bulles, dimensions, graine ; les compteurs d'actions
 * (fusions, synthèses, impulsions…) n'en sont pas, et les réglages ({@link #REGLAGES}) non plus. Chaque monnaie payée se
 * répartit entre les achats du même moment au prorata de leur prix estimé ({@link #coutRelatif}), sinon à parts égales.
 */
public final class Achats {

    private Achats() {}

    /**
     * Un achat : son groupe (« Amélioration »), la chose (« Couplage »), combien de niveaux, la clé de la monnaie qui l'a
     * payé ({@code null} si inconnue), son nom, le montant en ordres de grandeur (NaN si inconnu), le montant écrit
     * (« 4,2e12 », avec « (lot de 3) » quand plusieurs achats se partagent la monnaie), et la part de la réserve (NaN si
     * inconnue).
     */
    public record Achat(String groupe, String chose, double quantite, String monnaie, String paye, double montantLog, String montant,
            double part, Tenue tenue) {

        /** « Amélioration — Couplage ». */
        public String quoi() {
            return groupe + (chose.isEmpty() ? "" : " — " + chose);
        }
    }

    /**
     * Jusqu'où tient un achat (version 0.168) : la remise à zéro qui l'efface. Les améliorations en particules partent à
     * la fusion (sauf la part que garde la Persistance), celles en atomes et les automatismes à l'explosion, l'arbre de
     * matière noire au Big Bang, ce qui appartient à l'acte du Big Bang et au collisionneur à la formation de l'univers ;
     * les bulles, la graine et les dimensions tiennent toute la partie.
     */
    public enum Tenue {
        FUSION("jusqu'à la prochaine fusion"),
        EXPLOSION("jusqu'à la prochaine explosion"),
        BIG_BANG("jusqu'au prochain Big Bang"),
        UNIVERS("jusqu'à la formation de l'univers"),
        PARTIE("toute la partie"),
        INCONNUE("");

        private final String label;

        Tenue(String label) {
            this.label = label;
        }

        /** « jusqu'à la prochaine explosion ». */
        public String label() {
            return label;
        }
    }

    /** Jusqu'où tient ce qui s'achète à cette clé (version 0.168). */
    public static Tenue tenue(String key, String sub) {
        if (key == null) return Tenue.INCONNUE;
        if (key.equals("upgradeLevels")) return "particles".equals(monnaie(key, sub)) ? Tenue.FUSION : Tenue.EXPLOSION;
        if (key.equals("ownedAutomations") || key.equals("automationSpeedLevels")) return Tenue.EXPLOSION;
        if (key.equals("darkUpgradeLevels") || key.equals("tableWeightLevel")) return Tenue.BIG_BANG;
        if (key.equals("treeBubbles") || key.startsWith("treeBubble") || key.equals("universeUpgrades") || key.equals("dimensionAutomations")
                || key.startsWith("seedUpgrade") || key.equals("unlockedSkins") || key.startsWith("line")) return Tenue.PARTIE;
        for (String d : new String[] {"planeLevel", "volumeLevel", "durationLevel", "loopLevel", "braneLevel", "clickerLevel", "gridLevel",
            "mirrorLevel", "mixStageLevel"}) if (key.startsWith(d)) return Tenue.PARTIE;
        if (key.equals("spaceUpgrades") || key.equals("boughtMilestones") || key.equals("probeUpgrades") || key.startsWith("collider")
                || key.equals("hadrons") || key.equals("hadronsOpen") || key.equals("voidOpen") || key.startsWith("voidLevel")
                || key.equals("automationBatchLevels")) return Tenue.UNIVERS;
        return Tenue.INCONNUE;
    }

    private record Acquis(String groupe, String chose, double quantite, String monnaie, double coutLog, Tenue tenue) {}

    /** Les achats faits entre deux photos, avec leur prix : l'ordre des changements, une bulle et un niveau par achat. */
    public static List<Achat> lire(List<Photo.Changement> changes) {
        Map<String, Photo.Nombre> payes = new LinkedHashMap<>();
        Map<String, Photo.Nombre> reserves = new LinkedHashMap<>();
        for (Photo.Changement c : changes) {
            if (c.sousCle() == null && Noms.ressource(c.cle()) && c.baisse()) {
                payes.put(c.cle(), c.avant().minus(c.apres()));
                reserves.put(c.cle(), c.avant());
            }
            if (c.sousCle() == null && c.cle().equals("voidSpent") && c.monte()) payes.put("voidSpent", c.apres().minus(c.avant()));
        }
        List<Acquis> acquis = new ArrayList<>();
        for (Photo.Changement c : changes) {
            String key = c.cle();
            if (key.equals("treeBubbles") && c.monte()) {
                for (String bubble : Noms.bulles((long) c.avant().toDouble(), (long) c.apres().toDouble())) {
                    acquis.add(new Acquis("Bulle de l'arbre", bubble, 1, "voidSpent", Double.NaN, Tenue.PARTIE));
                }
                continue;
            }
            if (!achat(key)) continue;
            if (c.ajoute() != null) {
                acquis.add(new Acquis(Noms.cle(key), Noms.chose(c.ajoute()), 1, monnaie(key, c.ajoute()), coutRelatif(key, c.ajoute(), 0, 1),
                        tenue(key, c.ajoute())));
            } else if (c.monte()) {
                double from = c.avant().toDouble(), to = c.apres().toDouble();
                String chose = c.sousCle() == null ? "" : Noms.chose(c.sousCle());
                acquis.add(new Acquis(Noms.cle(key), chose, to - from, monnaie(key, c.sousCle()), coutRelatif(key, c.sousCle(), (int) from, (int) to),
                        tenue(key, c.sousCle())));
            }
        }
        List<Achat> out = new ArrayList<>();
        for (Acquis a : acquis) {
            Photo.Nombre total = a.monnaie() == null ? null : payes.get(a.monnaie());
            String montant = "—", paye = a.monnaie() == null ? "—" : Noms.cle(a.monnaie());
            double part = Double.NaN, montantLog = Double.NaN;
            if (total != null && total.signe() > 0) {
                List<Acquis> memes = acquis.stream().filter(x -> a.monnaie().equals(x.monnaie())).toList();
                double share;
                if (memes.stream().allMatch(x -> Double.isFinite(x.coutLog()))) {
                    double max = memes.stream().mapToDouble(Acquis::coutLog).max().orElse(0), sum = 0;
                    for (Acquis x : memes) sum += Math.pow(10, x.coutLog() - max);
                    share = Math.pow(10, a.coutLog() - max) / sum;
                } else {
                    share = 1.0 / memes.size();
                }
                montantLog = total.log10() + Math.log10(share);
                montant = new Photo.Nombre(1, montantLog).texte() + (memes.size() > 1 ? " (lot de " + memes.size() + ")" : "");
                Photo.Nombre reserve = reserves.get(a.monnaie());
                part = reserve == null || reserve.signe() <= 0 ? Double.NaN : Math.pow(10, montantLog - reserve.log10());
            }
            out.add(new Achat(a.groupe(), a.chose(), a.quantite(), a.monnaie(), paye, montantLog, montant, part, a.tenue()));
        }
        return out;
    }

    /** Les clés où ce qui monte a été acheté (version 0.162 : les autres, des compteurs d'actions, ne sont plus pris pour des achats). */
    public static final java.util.Set<String> ACHATS = java.util.Set.of("upgradeLevels", "ownedAutomations", "automationSpeedLevels", "darkUpgradeLevels",
            "spaceUpgrades", "boughtMilestones", "colliderGeneratorsBought", "colliderUpgrades", "probeUpgrades", "dimensionAutomations",
            "hadrons", "universeUpgrades", "tableWeightLevel", "voidOpen", "hadronsOpen", "unlockedSkins", "automationBatchLevels");

    /** Vrai si ce qui monte à cette clé a été acheté. */
    public static boolean achat(String key) {
        if (ACHATS.contains(key)) return true;
        for (String prefix : List.of("voidLevel", "treeBubbleLevel", "line", "planeLevel", "volumeLevel", "durationLevel", "loopLevel", "braneLevel",
                "clickerLevel", "gridLevel", "mirrorLevel", "mixStageLevel", "seedUpgrade")) {
            if (key.startsWith(prefix) && (key.endsWith("Level") || Character.isDigit(key.charAt(key.length() - 1)))) return true;
        }
        return false;
    }

    /** Les réglages : ce que le joueur règle sans rien acheter. Ils se notent comme des choix, pas comme des achats. */
    public static final Map<String, String> REGLAGES = Map.ofEntries(
            Map.entry("autoMoleculeReserve", "réserve des molécules automatiques"), Map.entry("voidShare", "part laissée au vide"),
            Map.entry("cycleChoice", "choix de cycle"), Map.entry("chosenSkins", "apparence choisie"), Map.entry("automationPaused", "tout couper"),
            Map.entry("chronoSpeed", "accélération du chrono"), Map.entry("stateCaps", "plafond des états de la matière"),
            Map.entry("systemPlan", "plan du système"), Map.entry("darkPlan", "plan de l'arbre de matière noire"), Map.entry("plannedBodies", "astres prévus"),
            Map.entry("autoHold", "réserve automatique"), Map.entry("autoMolecules", "molécules automatiques"), Map.entry("autoFollowList", "suivre la liste"),
            Map.entry("autoFollowMatter", "suivre la matière"), Map.entry("autoGather", "rassemblement automatique"), Map.entry("autoForm", "formation automatique"),
            Map.entry("favoriteMolecules", "molécules favorites"), Map.entry("recipeReservation", "réservation des recettes"),
            Map.entry("holdLocked", "réserve verrouillée"), Map.entry("colliderPaused", "collisionneur en pause"));

    private static final String[] RESSOURCE_DIMENSION = {"universeSeeds", "dimensionShards", "planeShards", "volumeShards", "durationShards",
        "loopShards", "braneShards", "clickerShards", "gridShards", "mirrorShards", "mixShards"};

    /** La ressource qui paie ce qui est monté à cette clé : sa clé dans la sauvegarde, ou {@code null} si inconnue. */
    public static String monnaie(String key, String sub) {
        if (key.equals("upgradeLevels")) {
            for (Upgrade u : Upgrades.DEFAULT) if (u.id().equals(sub)) return u.resource() == idle.core.Resource.PARTICLES ? "particles" : "atoms";
            return null;
        }
        if (key.equals("darkUpgradeLevels")) {
            // L'arbre se paie selon la branche : particules, atomes ou matière noire ; la taille s'atteint sans payer.
            for (var d : idle.core.darkmatter.DarkUpgrades.DEFAULT) {
                if (!d.id().equals(sub)) continue;
                return switch (d.branch()) {
                    case PARTICLES -> "particles";
                    case ATOMS -> "atoms";
                    case SIZE -> null;
                    case DARK_MATTER -> "darkMatter";
                };
            }
            return null;
        }
        // Les automatismes ordinaires et leur cadence se paient en atomes.
        if (key.equals("ownedAutomations") || key.equals("automationSpeedLevels")) return "atoms";
        // L'achat en lot des automatismes (version 0.167) se paie en matière noire.
        if (key.equals("automationBatchLevels")) return "darkMatter";
        if (key.equals("spaceUpgrades")) return "space";
        if (key.startsWith("collider")) return "colliderEnergy";
        if (key.startsWith("voidLevel")) return "voidEnergy";
        if (key.startsWith("treeBubble")) return "voidSpent";
        if (key.startsWith("line")) return "dimensionShards";
        if (key.equals("hadrons")) return "quarks";
        String[][] dims = {{"planeLevel", "planeShards"}, {"volumeLevel", "volumeShards"}, {"durationLevel", "durationShards"},
            {"loopLevel", "loopShards"}, {"braneLevel", "braneShards"}, {"clickerLevel", "clickerShards"}, {"gridLevel", "gridShards"},
            {"mirrorLevel", "mirrorShards"}, {"mixStageLevel", "mixShards"}};
        for (String[] d : dims) if (key.startsWith(d[0])) return d[1];
        if (key.startsWith("seedUpgrade")) {
            int i = Integer.parseInt(key.substring("seedUpgrade".length()));
            return RESSOURCE_DIMENSION[Game.SeedUpgrade.values()[i].dimension()];
        }
        if (key.equals("dimensionAutomations") && sub != null) {
            try {
                return RESSOURCE_DIMENSION[DimensionAutomation.valueOf(sub).paidWith()];
            } catch (IllegalArgumentException unknown) {
                return null;
            }
        }
        return null;
    }

    /** Le prix relatif (en ordres de grandeur) des niveaux {@code from} à {@code to} : pour répartir un lot ; NaN si inconnu. */
    public static double coutRelatif(String key, String sub, int from, int to) {
        try {
            if (key.equals("upgradeLevels")) {
                for (Upgrade u : Upgrades.DEFAULT) if (u.id().equals(sub)) return somme(u.baseCost().log10(), Math.log10(u.costGrowth()), from, to);
            }
            if (key.equals("ownedAutomations")) {
                for (var a : idle.core.upgrades.Automations.DEFAULT) if (a.id().equals(sub)) return a.cost().log10();
            }
            if (key.equals("automationSpeedLevels")) {
                for (var a : idle.core.upgrades.Automations.DEFAULT) if (a.id().equals(sub)) return somme(a.speedBaseCost().log10(), Math.log10(a.speedCostGrowth()), from, to);
            }
            if (key.equals("automationBatchLevels")) {
                return somme(Math.log10(Game.AUTOMATION_BATCH_BASE_COST), Math.log10(Game.AUTOMATION_BATCH_COST_GROWTH), from, to);
            }
            if (key.equals("darkUpgradeLevels")) {
                for (var d : idle.core.darkmatter.DarkUpgrades.DEFAULT) if (d.id().equals(sub)) return somme(d.baseCost().log10(), Math.log10(d.costGrowth()), from, to);
            }
            if (key.equals("colliderGeneratorsBought")) {
                var gen = idle.core.collider.ColliderGenerator.valueOf(sub);
                return Math.log10(gen.cost(from)) + Math.log10(Math.max(1, to - from));
            }
            if (key.equals("colliderUpgrades")) return Math.log10(idle.core.collider.ColliderUpgrade.valueOf(sub).cost(from));
            if (key.equals("dimensionAutomations")) return Math.log10(DimensionAutomation.valueOf(sub).cost());
            if (key.startsWith("treeBubbleLevel")) {
                TreeBubble bubble = TreeBubble.values()[Integer.parseInt(key.substring("treeBubbleLevel".length()))];
                return Math.log10(bubble.costAt(from));
            }
            idle.core.GameUniverse.Priced priced = priced(key);
            if (priced != null) return Math.log10(priced.cost(from)) + Math.log10(Math.max(1, to - from));
        } catch (RuntimeException unknown) {
            return Double.NaN;
        }
        return Double.NaN;
    }

    private static double somme(double baseLog, double growthLog, int from, int to) {
        // Une suite géométrique : la somme est dominée par son dernier terme.
        int last = Math.max(from, to - 1);
        return baseLog + growthLog * last;
    }

    private static idle.core.GameUniverse.Priced priced(String key) {
        String[][] prefixes = {{"planeLevel"}, {"volumeLevel"}, {"durationLevel"}, {"loopLevel"}, {"braneLevel"}, {"clickerLevel"},
            {"gridLevel"}, {"mirrorLevel"}, {"mixStageLevel"}, {"voidLevel"}, {"seedUpgrade"}};
        Object[][] enums = {Game.PlaneUpgrade.values(), Game.VolumeUpgrade.values(), Game.DurationUpgrade.values(), Game.LoopUpgrade.values(),
            Game.BraneUpgrade.values(), Game.ClickerUpgrade.values(), Game.GridUpgrade.values(), Game.MirrorUpgrade.values(),
            Game.MixUpgrade.values(), Game.VoidUpgrade.values(), Game.SeedUpgrade.values()};
        for (int i = 0; i < prefixes.length; i++) {
            String p = prefixes[i][0];
            if (key.startsWith(p) && key.length() > p.length()) {
                int index = Integer.parseInt(key.substring(p.length()));
                return (idle.core.GameUniverse.Priced) enums[i][index];
            }
        }
        String[][] line = {{"lineYieldLevel", "YIELD"}, {"lineTicksLevel", "TICKS"}, {"lineSpeedLevel", "SPEED"}, {"lineBallsLevel", "BALLS"},
            {"lineCountLevel", "LINES"}};
        for (String[] l : line) if (key.equals(l[0])) return Game.LineUpgrade.valueOf(l[1]);
        return null;
    }

}
