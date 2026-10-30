import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.bigbang.SpaceUpgrades;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkAutomations;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les noms en français de ce que le bot analyste voit changer dans la sauvegarde (version 0.161) : la clé (« Arbre de
 * matière noire », « Particules ») et la chose (« Persistance », « Proton »). Les catalogues du jeu donnent le nom de
 * leurs améliorations, succès et défis ; les énumérations, leur {@code label()} ; le reste se nomme ici. Une clé
 * inconnue garde son nom de sauvegarde : le rapport ne perd rien.
 */
final class AnalyseNoms {

    private AnalyseNoms() {}

    /** Ce que désigne chaque clé de la sauvegarde. */
    private static final Map<String, String> CLES = new LinkedHashMap<>();
    /** Le nom de chaque identifiant des catalogues et des énumérations. */
    private static final Map<String, String> CHOSES = new HashMap<>();
    /** Les clés numérotées (« planeLevel3 ») : leur préfixe, et les noms dans l'ordre. */
    private static final Map<String, String[]> RANGS = new LinkedHashMap<>();
    private static final Map<String, String> RANGS_CLES = new LinkedHashMap<>();

    static {
        String[][] cles = {
            {"particles", "Particules"}, {"atoms", "Atomes"}, {"totalAtoms", "Atomes en tout"}, {"darkMatter", "Matière noire"},
            {"darkMatterSpent", "Matière noire dépensée"}, {"space", "Espace"}, {"universeSeeds", "Graines d'univers"},
            {"voidEnergy", "Énergie du vide"}, {"voidSpent", "Énergie du vide dépensée en bulles"}, {"colliderEnergy", "Énergie du collisionneur"},
            {"dimensionShards", "Segments de la ligne"}, {"planeShards", "Facettes du plan"}, {"volumeShards", "Sphères du volume"},
            {"durationShards", "Instants de la durée"}, {"loopShards", "Nœuds de la boucle"}, {"braneShards", "Ondes de la brane"},
            {"clickerShards", "Étincelles du clicker"}, {"gridShards", "Repères de la grille"}, {"mirrorShards", "Reflets du miroir"},
            {"mixShards", "Fragments du mélange"}, {"upgradeLevels", "Amélioration"}, {"ownedAutomations", "Automatisme acheté"},
            {"enabledAutomations", "Automatisme en marche"}, {"automationSpeedLevels", "Vitesse d'automatisme"},
            {"darkUpgradeLevels", "Arbre de matière noire"}, {"achievements", "Succès"}, {"completedChallenges", "Défi de matière noire réussi"},
            {"enabledDarkAutomations", "Automatisme de matière noire en marche"}, {"elements", "Éléments du tableau"},
            {"spaceUpgrades", "Amélioration d'espace"}, {"completedBangChallenges", "Défi de Big Bang réussi"},
            {"boughtMilestones", "Palier de Big Bang acheté"}, {"colliderGeneratorsBought", "Générateur du collisionneur"},
            {"colliderGenerators", "Générateurs du collisionneur (en stock)"}, {"colliderUpgrades", "Amélioration du collisionneur"},
            {"probeUpgrades", "Amélioration des sondes"}, {"quarks", "Quarks"}, {"hadrons", "Hadrons"}, {"titles", "Titre"},
            {"dimensionAutomations", "Automatisme de dimension"}, {"dimensionAutomationsOff", "Automatisme de dimension coupé"},
            {"universeUpgrades", "Amélioration de l'univers"}, {"treeBubbles", "Bulles de l'arbre"}, {"bigBangs", "Big Bangs"},
            {"explosions", "Explosions"}, {"cosmos", "Échelle du cosmos"}, {"bodies", "Astres"}, {"solarSystems", "Systèmes solaires"},
            {"substances", "Substances"}, {"assemblies", "Assemblages"}, {"unlockedSkins", "Apparence débloquée"},
            {"lineYieldLevel", "Ligne : densité"}, {"lineTicksLevel", "Ligne : graduations"}, {"lineSpeedLevel", "Ligne : élan"},
            {"lineBallsLevel", "Ligne : boules"}, {"lineCountLevel", "Ligne : lignes"}, {"tableWeightLevel", "Poids du tableau"},
            {"synthesisCount", "Synthèses"}, {"hadronsOpen", "Hadrons ouverts"}, {"voidOpen", "Vide ouvert"}, {"seedWaterings", "Arrosages de la graine"},
            {"mixStage", "Étape du mélange"}, {"mirrorWins", "Réussites du miroir"}, {"gameFinished", "Fin de la partie"},
            {"chronoStored", "Temps gardé par le chrono"}, {"extraUniverses", "Univers formés en plus"}, {"universeFormed", "Univers formé"},
        };
        for (String[] c : cles) CLES.put(c[0], c[1]);
        rang("planeLevel", "Plan", "Valeur", "Réapparition", "Nombre", "Vitesse du point", "Taille du point");
        rang("volumeLevel", "Volume", "Durée", "Valeur", "Réapparition", "Nombre", "Taille des boules");
        rang("durationLevel", "Durée", "Vitesse des spirales", "Spirales", "Valeur", "Temps au plus");
        rang("loopLevel", "Boucle", "Rapidité", "Zones", "Gain par zone", "Précision", "Taille des zones", "Cercles");
        rang("braneLevel", "Brane", "Gain", "Élasticité", "Grandeur des ondes", "Rebond", "Points en une onde");
        rang("clickerLevel", "Clicker", "Gain", "Multiplicateur de cadence", "Zones ×2", "Multiplicateur des zones", "Durée des zones",
                "Nombre de zones", "Taille des zones", "Zones superposées");
        rang("gridLevel", "Grille", "Réduction", "Gain", "Affichage du point", "Nombre de points", "Proximité", "Essais");
        rang("mirrorLevel", "Miroir", "Gain", "Longueur", "Mémorisation", "Seconde chance", "Plateau plus vaste");
        rang("mixStageLevel", "Mélange", "Gain", "Malus", "Graduations", "Vitesse du point", "Profondeur", "Charge", "Zone du rythme",
                "Portée de l'onde", "Zones ×2", "Trésor", "Paires");
        rang("voidLevel", "Vide", "Gain", "Rythme", "Énergie", "Espace", "Fonte", "Allègement", "Maturation", "Assise");
        String[] seed = new String[Game.SeedUpgrade.values().length];
        for (Game.SeedUpgrade upgrade : Game.SeedUpgrade.values()) seed[upgrade.ordinal()] = upgrade.label();
        rang("seedUpgrade", "Graine", seed);
        String[] bubbles = new String[TreeBubble.values().length];
        for (TreeBubble bubble : TreeBubble.values()) bubbles[bubble.ordinal()] = bubble.label();
        rang("treeBubbleLevel", "Bulle", bubbles);

        for (Upgrade u : Upgrades.DEFAULT) CHOSES.put(u.id(), u.name());
        for (Automation a : Automations.DEFAULT) CHOSES.put(a.id(), a.name());
        for (DarkUpgrade d : DarkUpgrades.DEFAULT) CHOSES.put(d.id(), d.name());
        for (Achievement a : Achievements.DEFAULT) CHOSES.put(a.id(), a.name());
        for (Challenge c : Challenges.ALL) CHOSES.put(c.id(), c.name());
        for (DarkAutomation d : DarkAutomations.DEFAULT) CHOSES.put(d.id(), d.name());
        for (SpaceUpgrade s : SpaceUpgrades.DEFAULT) CHOSES.put(s.id(), s.name());
        for (Class<?> type : List.of(idle.core.collider.ColliderUpgrade.class, idle.core.collider.ColliderGenerator.class,
                idle.core.probes.ProbeUpgrade.class, idle.core.collider.Hadron.class, idle.core.collider.Quark.class,
                idle.core.bigbang.BangChallenge.class, idle.core.universe.DimensionAutomation.class, idle.core.profile.Title.class,
                idle.core.universe.UniverseUpgrade.class, idle.core.universe.Skin.class)) {
            enumLabels(type);
        }
    }

    private static void rang(String prefix, String group, String... names) {
        RANGS.put(prefix, names);
        RANGS_CLES.put(prefix, group);
    }

    private static void enumLabels(Class<?> type) {
        try {
            Method label = type.getMethod("label");
            for (Object constant : type.getEnumConstants()) CHOSES.putIfAbsent(((Enum<?>) constant).name(), String.valueOf(label.invoke(constant)));
        } catch (ReflectiveOperationException none) {
            // Pas de label : le nom de la constante suffit.
        }
    }

    /** Le nom d'une clé, numérotée ou non : « Plan : Valeur », « Particules ». */
    static String cle(String key) {
        if (CLES.containsKey(key)) return CLES.get(key);
        for (Map.Entry<String, String[]> e : RANGS.entrySet()) {
            String prefix = e.getKey();
            if (key.startsWith(prefix) && key.length() > prefix.length() && key.substring(prefix.length()).chars().allMatch(Character::isDigit)) {
                int i = Integer.parseInt(key.substring(prefix.length()));
                String[] names = e.getValue();
                return RANGS_CLES.get(prefix) + " : " + (i < names.length ? names[i] : "n° " + i);
            }
        }
        return key;
    }

    /** Le nom d'une chose d'une table ou d'un ensemble : l'amélioration, le succès, le hadron… */
    static String chose(String id) {
        if (id == null) return "";
        String name = CHOSES.get(id);
        if (name != null) return name;
        if (id.chars().allMatch(Character::isDigit)) return "élément Z = " + id;
        return id;
    }

    /** Les bulles de l'arbre achetées, lues dans le champ de bits de la sauvegarde. */
    static java.util.List<String> bulles(long avant, long apres) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (TreeBubble bubble : TreeBubble.values()) {
            long bit = 1L << bubble.ordinal();
            if ((apres & bit) != 0 && (avant & bit) == 0) out.add(bubble.label());
        }
        return out;
    }

    /** Vrai pour les clés de ressources : ce qui baisse là a été payé. */
    static boolean ressource(String key) {
        return switch (key) {
            case "particles", "atoms", "darkMatter", "space", "universeSeeds", "voidEnergy", "colliderEnergy", "dimensionShards",
                    "planeShards", "volumeShards", "durationShards", "loopShards", "braneShards", "clickerShards", "gridShards",
                    "mirrorShards", "mixShards", "quarks", "elements", "synthesisReserve" -> true;
            default -> false;
        };
    }
}
