package idle.ui;

import idle.core.Achievement;
import idle.core.Automation;
import idle.core.Challenge;
import idle.core.DarkAutomation;
import idle.core.Element;
import idle.core.ElementSet;
import idle.core.Game;
import idle.core.Molecule;
import idle.core.Landmark;
import idle.core.PeriodicTable;
import idle.core.SizeScale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repère ce qui vient de se passer dans le jeu, pour l'annoncer au joueur : nouvel élément,
 * ensemble réuni, déblocage, succès, explosion possible.
 *
 * <p>Il ne s'accroche à aucune règle : à chaque appel de {@link #poll()}, il compare l'état du jeu
 * à celui qu'il avait vu la fois d'avant. Un automatisme qui agit dix fois par seconde et un clic
 * du joueur sont donc annoncés de la même façon. Ce qui recule (un tableau vidé par une
 * explosion, une partie remise à zéro) n'est jamais annoncé : il se contente de reprendre ses repères.
 */
final class Notifier {

    /** Au-delà, les messages d'un même relevé sont résumés : rien ne sert d'en empiler vingt. */
    private static final int MAX_PER_POLL = 4;
    /** Les paliers de vitesse ne sont annoncés qu'au début : ensuite ils tombent par dizaines. */
    private static final int ANNOUNCED_SPEED_MILESTONES = 5;

    private final Game game;
    private boolean primed = false;
    private boolean started;
    private Set<Integer> elements = new HashSet<>();
    private final Map<String, Double> setStrengths = new HashMap<>();
    private Set<String> automations = new HashSet<>();
    private boolean tableUnlocked;
    private boolean synthesisUnlocked;
    private boolean canExplode;
    private int speedMilestones;
    private int landmarks;
    private final Set<String> darkAutomations = new HashSet<>();
    private final Set<String> challengesOpen = new HashSet<>();
    private final Set<String> challengesDone = new HashSet<>();
    private int explosions;
    private int weightLevel;
    private int bigBangs;
    private int moleculeKinds;
    private final Map<Molecule.Kind, Integer> collections = new java.util.EnumMap<>(Molecule.Kind.class);
    private int bangChallengesDone;
    private idle.core.BangChallenge bangChallenge;

    Notifier(Game game) {
        this.game = game;
    }

    /** La partie vient d'être remplacée par une autre : le prochain relevé reprend ses repères sans rien annoncer. */
    void forget() {
        primed = false;
    }

    /** Ce qui s'est passé depuis le dernier appel, du plus ancien au plus récent. Vide la plupart du temps. */
    List<String> poll() {
        List<String> news = new ArrayList<>();
        List<Achievement> achievements = game.takeNewAchievements();
        List<idle.core.SpaceUpgrade> spaceUpgrades = game.takeNewSpaceUpgrades();
        int gathered = game.takeAutoGathered();
        List<String> formed = game.takeAutoFormed();
        boolean comet = game.takeCometAppeared();
        boolean prime = game.takeSpeedPrime();
        List<idle.core.GameStats.Step> records = game.takeNewRecords();
        // Premier relevé, ou partie remise à zéro : on prend ses repères sans rien annoncer.
        if (!primed || (started && !game.isStarted())) {
            snapshot();
            primed = true;
            return news;
        }

        if (game.bigBangs() > bigBangs) {
            // Tout vient de repartir de zéro : on reprend ses repères, pour que la partie suivante s'annonce comme la première.
            int count = game.bigBangs();
            if (game.completedBangChallenges() > bangChallengesDone && bangChallenge != null) {
                news.add("Défi de Big Bang « " + bangChallenge.label() + " » réussi : " + lower(bangChallenge.rewardText()));
            }
            for (idle.core.GameStats.Step step : records) news.add("Record battu : " + step.label());
            snapshot();
            news.add(game.isDarkMatterUnlocked()
                    ? "Big Bang n° " + count + " : tout repart du premier générateur, mais l'arbre de matière noire est resté."
                    : count > 1 ? "Big Bang n° " + count + " : tout repart du premier générateur. Vos succès restent."
                    : "Big Bang : tout repart du premier générateur. Vos succès restent, et l'onglet Big Bang s'ouvre.");
            return news;
        }

        // Un palier d'espace vient d'être atteint : un rayon de molécules s'ouvre.
        if (game.moleculeKindsUnlocked() > moleculeKinds) {
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                if (kind.ordinal() >= moleculeKinds && game.isMoleculeKindUnlocked(kind)) {
                    news.add("Molécules : le rayon « " + kind.label() + " » s'ouvre");
                }
            }
        }

        // Les améliorations d'espace que le temps vient d'apporter ; l'ouverture d'un rayon s'annonce déjà ci-dessus.
        for (idle.core.SpaceUpgrade upgrade : spaceUpgrades) {
            if (upgrade.effect() instanceof idle.core.SpaceUpgrade.OpenKind) continue;
            news.add("Espace : « " + upgrade.name() + " » est acquise");
        }

        // Un défi de Big Bang vient de commencer : tout est reparti, comme après un Big Bang.
        if (game.activeBangChallenge() != null && game.activeBangChallenge() != bangChallenge) {
            idle.core.BangChallenge started = game.activeBangChallenge();
            snapshot();
            news.add("Défi de Big Bang « " + started.label() + " » : tout repart du premier générateur.");
            return news;
        }
        bangChallenge = game.activeBangChallenge();
        if (comet) {
            news.add("Une comète traverse l'expansion : " + (int) Game.COMET_VISIBLE_SECONDS + " secondes pour la saisir (Big Bang, Expansion de la matière)");
        }
        for (idle.core.GameStats.Step step : records) news.add("Record battu : " + step.label());
        // Une collection vient de passer la moitié de son rayon, ou d'être complétée.
        for (idle.core.KindCollection collection : game.collections()) {
            int level = game.collectionLevel(collection.kind());
            if (level > collections.getOrDefault(collection.kind(), 0)) {
                news.add("Collection " + collection.kind().label() + (level >= 2 ? " complète : " : " à moitié réunie : ")
                        + lower(BigBangPage.what(collection.stat())) + " ×" + ElementText.number(collection.factor(level)));
            }
            collections.put(collection.kind(), level);
        }

        // Ce que le rassemblement et la formation automatiques viennent de faire sans le joueur.
        if (gathered > 0) {
            news.add("Rassemblement automatique : " + gathered + (gathered > 1 ? " sortes rassemblées" : " sorte rassemblée"));
        }
        if (formed.size() > 3) {
            news.add("Formation automatique : " + formed.size() + " formés, dont « " + formed.get(formed.size() - 1) + " »");
        } else {
            for (String name : formed) news.add("Formation automatique : « " + name + " »");
        }

        if (game.state().explosions() > explosions) {
            // Une explosion qui compte alourdit le tableau ; celle d'un défi rejoué ne change rien.
            news.add(game.state().tableWeightLevel() > weightLevel
                    ? "Explosion : +" + Format.count(prime ? game.darkMatterPerExplosion().add(game.speedPrime())
                                    : game.darkMatterPerExplosion()) + " matière noire"
                            + (prime ? ", prime de vitesse comprise" : "")
                            + (game.state().tableWeightLevel() <= Game.TABLE_WEIGHT_MAX_LEVEL
                                    ? ", tableau " + Format.multiplier(game.tableWeight()) + " plus lourd qu'au début" : "")
                    : "Explosion : défi rejoué, sans matière noire");
        }
        for (Challenge challenge : game.challenges()) {
            if (game.isChallengeCompleted(challenge.id()) && !challengesDone.contains(challenge.id())) {
                news.add("Défi réussi : " + challenge.name() + "  (" + ChallengesPane.reward(challenge) + ")");
            }
            if (game.isChallengeUnlocked(challenge.id()) && !challengesOpen.contains(challenge.id())) {
                news.add("Nouveau défi : " + challenge.name());
            }
        }
        for (DarkAutomation automation : game.darkAutomations()) {
            if (game.isDarkAutomationUnlocked(automation.id()) && !darkAutomations.contains(automation.id())) {
                news.add("Nouvel automatisme de matière noire : " + automation.name());
            }
        }

        if (game.isPeriodicTableUnlocked() && !tableUnlocked) {
            news.add("Automatisation et tableau périodique débloqués");
        }
        if (game.isSynthesisAutomationUnlocked() && !synthesisUnlocked) {
            news.add("Élément unique ★ : la synthèse automatique est débloquée");
        }

        // Les éléments nouveaux : un seul se nomme, plusieurs se comptent.
        List<Element> fresh = new ArrayList<>();
        for (int number : game.state().elements().keySet()) {
            if (!elements.contains(number)) fresh.add(PeriodicTable.element(number));
        }
        if (fresh.size() == 1) {
            Element element = fresh.get(0);
            news.add("Nouvel élément : " + element.name() + " (" + element.symbol() + ")  —  " + ElementText.describe(element.effect()));
        } else if (fresh.size() > 1 && game.state().explosions() == explosions) {
            news.add(fresh.size() + " nouveaux éléments");
        }
        for (ElementSet set : game.elementSets()) {
            double before = setStrengths.getOrDefault(set.id(), 0.0);
            double now = game.setStrength(set);
            if (now > before && game.state().explosions() == explosions) {
                news.add((now >= 1 ? "Ensemble complet : " : "Ensemble à moitié réuni : ") + set.name()
                        + "  —  " + ElementText.describe(set.effect()) + (now >= 1 ? "" : " (" + ElementText.percent(now) + " du bonus)"));
            }
        }

        // Un automatisme offert par la matière noire : ceux que le joueur achète, il le sait déjà.
        if (isGranting()) {
            for (Automation automation : game.automations()) {
                if (game.ownsAutomation(automation.id()) && !automations.contains(automation.id())
                        && game.state().explosions() == explosions) {
                    news.add("Automatisme offert : " + automation.name());
                }
            }
        }

        int milestones = game.speedMilestones();
        if (milestones > speedMilestones && milestones <= ANNOUNCED_SPEED_MILESTONES) {
            news.add("Palier de vitesse n° " + milestones + " : particules multipliées");
        }
        int reached = game.landmarksReached();
        if (reached > landmarks) {
            Landmark landmark = SizeScale.MILESTONES.get(reached - 1);
            news.add(reached - landmarks == 1 ? "Palier de taille : " + landmark.name()
                    : (reached - landmarks) + " paliers de taille, jusqu'à : " + landmark.name());
        }
        if (game.canExplode() && !canExplode) {
            news.add("Le tableau périodique peut exploser");
        }
        for (Achievement achievement : achievements) {
            news.add(AchievementText.announce(achievement));
        }

        snapshot();
        if (news.size() > MAX_PER_POLL) {
            int more = news.size() - (MAX_PER_POLL - 1);
            news = new ArrayList<>(news.subList(0, MAX_PER_POLL - 1));
            news.add("… et " + more + " autres nouvelles");
        }
        return news;
    }

    private boolean isGranting() {
        for (DarkAutomation automation : game.darkAutomations()) {
            if (automation.kind() == DarkAutomation.Kind.GRANT_AUTOMATIONS && game.isDarkAutomationEnabled(automation.id())) {
                return true;
            }
        }
        return false;
    }

    /** Retient l'état du jeu, pour le comparer au prochain relevé. */
    private void snapshot() {
        started = game.isStarted();
        // Les éléments et les automatismes possédés changent rarement : ils ne sont recopiés que s'ils ont changé.
        java.util.Set<Integer> owned = game.state().elements().keySet();
        if (!elements.equals(owned)) elements = new HashSet<>(owned);
        setStrengths.clear();
        for (ElementSet set : game.elementSets()) setStrengths.put(set.id(), game.setStrength(set));
        java.util.Set<String> bought = game.state().ownedAutomations();
        if (!automations.equals(bought)) automations = new HashSet<>(bought);
        tableUnlocked = game.isPeriodicTableUnlocked();
        synthesisUnlocked = game.isSynthesisAutomationUnlocked();
        canExplode = game.canExplode();
        speedMilestones = game.speedMilestones();
        landmarks = game.landmarksReached();
        darkAutomations.clear();
        for (DarkAutomation automation : game.darkAutomations()) {
            if (game.isDarkAutomationUnlocked(automation.id())) darkAutomations.add(automation.id());
        }
        challengesOpen.clear();
        challengesDone.clear();
        for (Challenge challenge : game.challenges()) {
            if (game.isChallengeUnlocked(challenge.id())) challengesOpen.add(challenge.id());
            if (game.isChallengeCompleted(challenge.id())) challengesDone.add(challenge.id());
        }
        explosions = game.state().explosions();
        weightLevel = game.state().tableWeightLevel();
        bigBangs = game.bigBangs();
        moleculeKinds = game.moleculeKindsUnlocked();
        for (idle.core.KindCollection collection : game.collections()) {
            collections.put(collection.kind(), game.collectionLevel(collection.kind()));
        }
        bangChallengesDone = game.completedBangChallenges();
        bangChallenge = game.activeBangChallenge();
    }

    private static String lower(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
