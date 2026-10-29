package idle.ui;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.achievements.Achievement;
import idle.core.bigbang.BangAchievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.collider.ColliderAchievement;
import idle.core.collider.GluonMilestone;
import idle.core.collider.Quark;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.Landmark;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.ElementSet;
import idle.core.elements.PeriodicTable;
import idle.core.molecules.KindCollection;
import idle.core.molecules.Molecule;
import idle.core.probes.ProbeReward;
import idle.core.profile.Title;
import idle.core.upgrades.Automation;
import idle.ui.bigbang.BigBangPage;
import idle.ui.common.AchievementText;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.darkmatter.ChallengesPane;
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
 *
 * <p>Les titres du profil (version 0.147) s'annoncent un par un, « Nouveau titre : … ». Ceux qu'une partie relue
 * méritait déjà ne s'annoncent pas : ils tombent ensemble à la première seconde de jeu, et la file est vidée sans bruit.
 */
final class Notifier {

    /** Au-delà, les messages d'un même relevé sont résumés : rien ne sert d'en empiler vingt. */
    private static final int MAX_PER_POLL = 4;
    /** Les paliers de vitesse ne sont annoncés qu'au début : ensuite ils tombent par dizaines. */
    private static final int ANNOUNCED_SPEED_MILESTONES = 5;

    private final Game game;
    private boolean primed = false;
    /** Vrai si les améliorations du collisionneur étaient déjà ouvertes au dernier relevé. */
    private boolean colliderUpgradesSeen = false;
    /** Version 0.132 : le vide qui se propose, son trou noir, et les améliorations du troisième acte au plus haut, annoncés une fois. */
    private boolean voidOfferedSeen = false, voidHoleSeen = false;
    private final java.util.EnumSet<Game.VoidUpgrade> voidMaxedSeen = java.util.EnumSet.noneOf(Game.VoidUpgrade.class);
    /** Version 0.149 : les hadrons qui se proposent dans le collisionneur, annoncés une fois par univers, comme le vide. */
    private boolean hadronsOfferedSeen = false;
    /** Version 0.160 : les paliers du défi final atteints, et la fin de la partie, au dernier relevé. */
    private final boolean[] finalTiersSeen = new boolean[Game.MAX_DIMENSIONS + 1];
    private boolean finishedSeen;
    /**
     * Version 0.151 : les paliers de gluons atteints au dernier relevé. Chacun s'annonce une fois, quand il est atteint ;
     * ceux qu'une partie relue avait déjà sont pris au premier relevé, sans bruit. Un univers qui reprend les hadrons
     * les fait redescendre : atteint de nouveau, il s'annonce de nouveau.
     */
    private final java.util.EnumSet<GluonMilestone> gluonMilestonesSeen = java.util.EnumSet.noneOf(GluonMilestone.class);
    /** Les succès du collisionneur déjà gagnés au dernier relevé. */
    private final java.util.EnumSet<ColliderAchievement> colliderAchievementsSeen = java.util.EnumSet.noneOf(ColliderAchievement.class);
    private final java.util.EnumSet<BangAchievement> bangAchievementsSeen = java.util.EnumSet.noneOf(BangAchievement.class);
    private boolean started;
    private Set<Integer> elements = new HashSet<>();
    private final Map<String, Double> setStrengths = new HashMap<>();
    private Set<String> automations = new HashSet<>();
    private boolean tableUnlocked;
    private boolean synthesisUnlocked;
    private boolean canExplode;
    private int speedMilestones;
    /** L'énergie produite au dernier relevé : pour annoncer une fois les paliers des trous noirs. */
    private double blackHoleEnergySeen = 0;
    /** Les jalons du cosmos déjà vus (version 0.068) : « CLUSTER:GAS »… */
    private final java.util.Set<String> cosmosMilestonesSeen = new HashSet<>();
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
    private BangChallenge bangChallenge;
    /** Les skins débloqués au dernier relevé (version 0.089) : chacun s'annonce avec le bonus cumulé. */
    /** Le ralentissement du premier élan au dernier relevé (version 0.095, par paliers en 0.096). */
    private double slowSeen = 1;
    private final java.util.EnumSet<idle.core.universe.Skin> skinsSeen = java.util.EnumSet.noneOf(idle.core.universe.Skin.class);

    /** Le début de l'annonce d'un titre (version 0.147) : « Nouveau titre : Étincelle ». */
    static final String NEW_TITLE = "Nouveau titre : ";
    /**
     * Les titres que la partie remplissait déjà au premier relevé, sans les avoir encore notés : une sauvegarde d'avant
     * les titres les reçoit tous à la première seconde de jeu. Ils arrivent sans être annoncés.
     */
    private final java.util.EnumSet<Title> quietTitles = java.util.EnumSet.noneOf(Title.class);

    /** Vrai si le joueur veut voir les anomalies du ciel (version 0.127, un réglage). */
    private java.util.function.BooleanSupplier skyEventsShown = () -> true;

    Notifier(Game game) {
        this.game = game;
    }

    /** Annonce ou tait les anomalies du ciel selon ce réglage. */
    void showSkyEvents(java.util.function.BooleanSupplier shown) {
        skyEventsShown = shown;
    }

    /** La partie vient d'être remplacée par une autre : le prochain relevé reprend ses repères sans rien annoncer. */
    void forget() {
        primed = false;
    }

    /** Ce qui s'est passé depuis le dernier appel, du plus ancien au plus récent. Vide la plupart du temps. */
    List<News> poll() {
        List<News> news = new ArrayList<>();
        List<Achievement> achievements = game.takeNewAchievements();
        List<SpaceUpgrade> spaceUpgrades = game.takeNewSpaceUpgrades();
        int gathered = game.takeAutoGathered();
        List<String> formed = game.takeAutoFormed();
        boolean comet = game.takeCometAppeared();
        boolean sky = game.takeSkyEventAppeared();
        boolean prime = game.takeSpeedPrime();
        List<GameStats.Step> records = game.takeNewRecords();
        List<Game.CollisionResult> collisions = game.takeFinishedCollisions();
        List<Title> titles = game.takeNewTitles();
        // Premier relevé, ou partie remise à zéro : on prend ses repères sans rien annoncer. Les titres déjà mérités
        // (une partie relue) tomberont à la prochaine seconde de jeu : ils seront tus aussi.
        if (!primed || (started && !game.isStarted())) {
            snapshot();
            quietTitles.clear();
            // Une partie pas encore commencée n'a rien mérité : son premier générateur, lui, s'annoncera.
            if (game.isStarted()) for (Title title : Title.values()) if (!game.hasTitle(title) && title.isReachedBy(game)) quietTitles.add(title);
            primed = true;
            return news;
        }
        for (Title title : titles) {
            if (!quietTitles.remove(title)) add(news, Notice.TITLES, NEW_TITLE + title.label());
        }
        // Ils tombent tous au même relevé : ensuite, plus rien n'est tu.
        if (!titles.isEmpty()) quietTitles.clear();
        // Les collisions sont trop nombreuses pour être toutes annoncées : seulement le quark top.
        boolean top = false;
        for (Game.CollisionResult result : collisions) if (result.quark() == Quark.TOP) top = true;
        if (top) add(news, Notice.COLLIDER, "Collision : un quark top ! À utiliser dans " + Format.wait(game.topLeft()) + ", dans l'onglet du collisionneur.");
        // Le défi final (version 0.160) : chaque palier atteint, puis la fin de la partie.
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            boolean tier = game.isFinalChallengeOpen() && game.isFinalTierReached(d);
            if (tier && !finalTiersSeen[d]) {
                add(news, Notice.FINAL, "Défi final : " + idle.ui.universe.Dimension.of(d).label() + " atteint son palier ("
                        + game.finalTiersReached() + " / " + Game.MAX_DIMENSIONS + ").");
            }
            finalTiersSeen[d] = tier;
        }
        if (game.isGameFinished() && !finishedSeen) add(news, Notice.FINAL, "Les dix paliers sont atteints : la partie est finie. Merci d'avoir joué !");
        finishedSeen = game.isGameFinished();
        boolean upgradesOpen = game.isColliderUpgradesUnlocked();
        if (upgradesOpen && !colliderUpgradesSeen) add(news, Notice.COLLIDER, "Collisionneur : les huit générateurs sont là, les améliorations s'ouvrent.");
        colliderUpgradesSeen = upgradesOpen;
        boolean voidOffered = game.isVoidOffered();
        if (voidOffered && !voidOfferedSeen) add(news, Notice.VOID, "Le vide se propose dans l'onglet du collisionneur : il s'ouvre contre de l'énergie du collisionneur.");
        voidOfferedSeen = voidOffered;
        boolean hadronsOffered = game.isHadronsOffered();
        if (hadronsOffered && !hadronsOfferedSeen) add(news, Notice.COLLIDER, "Collisionneur : les hadrons se proposent. Leur onglet s'ouvre contre de l'énergie du collisionneur.");
        hadronsOfferedSeen = hadronsOffered;
        for (GluonMilestone milestone : GluonMilestone.values()) {
            if (game.isGluonMilestoneReached(milestone) && gluonMilestonesSeen.add(milestone)) {
                add(news, Notice.COLLIDER, "Palier de gluons : " + milestone.label() + ". " + milestone.effect());
            }
        }
        boolean voidHole = game.isVoidHoleOpen();
        if (voidHole && !voidHoleSeen) add(news, Notice.VOID, "Vide : le trou noir s'ouvre. Les particules qu'on lui sacrifie raccourcissent la vie des paires.");
        voidHoleSeen = voidHole;
        for (Game.VoidUpgrade upgrade : new Game.VoidUpgrade[] {Game.VoidUpgrade.MATURE, Game.VoidUpgrade.SETTLE, Game.VoidUpgrade.LIGHTEN}) {
            boolean maxed = game.isVoidUnlocked() && game.voidLevel(upgrade) >= upgrade.max();
            if (maxed && voidMaxedSeen.add(upgrade)) add(news, Notice.VOID, "Vide : « " + voidTitle(upgrade) + " » est au plus haut.");
            if (!maxed) voidMaxedSeen.remove(upgrade);
        }
        for (ColliderAchievement achievement : ColliderAchievement.values()) {
            if (colliderAchievementsSeen.contains(achievement) || !game.hasColliderAchievement(achievement)) continue;
            colliderAchievementsSeen.add(achievement);
            add(news, Notice.ACHIEVEMENTS, "Succès du collisionneur : " + achievement.condition() + ", " + achievement.reward() + ".");
        }
        for (BangAchievement achievement : BangAchievement.values()) {
            if (bangAchievementsSeen.contains(achievement) || !game.hasBangAchievement(achievement)) continue;
            bangAchievementsSeen.add(achievement);
            add(news, Notice.ACHIEVEMENTS, "Succès du Big Bang : " + achievement.title() + ", +" + ElementText.percent(achievement.amount()) + " " + achievement.reward().label() + ".");
        }

        // Le premier élan (version 0.095, par paliers en 0.096) : chaque palier s'annonce.
        double slow = game.firstRunSlowdown();
        if (slow < slowSeen) {
            add(news, Notice.START, slow <= 1 ? "La création a pris tout son élan : elle va à pleine vitesse."
                    : (game.stats().fusions() <= 1 ? "Premier atome" : Game.FIRST_ATOMS_UNTIL + " atomes")
                            + " : la création prend de l'élan, encore " + idle.ui.common.ElementText.number(slow) + " fois moins vite qu'à plein régime.");
        }
        slowSeen = slow;
        // Un skin débloqué (version 0.089) : son bonus s'ajoute aux autres, qu'il soit choisi ou non.
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) {
            if (skinsSeen.contains(skin) || !game.hasSkin(skin)) continue;
            skinsSeen.add(skin);
            add(news, Notice.COSMETICS, "Skin débloqué : « " + skin.label() + " » (" + skin.visual().label().toLowerCase(java.util.Locale.ROOT) + "). +"
                    + ElementText.percent(idle.core.universe.Skin.BONUS) + " à toutes les ressources, soit +"
                    + ElementText.percent(game.skinBoost() - 1) + " au total, choisi ou non.");
        }

        List<String> matured = game.takeMaturedBodies();
        if (matured.size() == 1) {
            add(news, Notice.SKY, "Astre mûr : " + game.body(matured.get(0)).name() + ". Il compte désormais en entier.");
        } else if (matured.size() > 1) {
            add(news, Notice.SKY, matured.size() + " astres mûrs. Ils comptent désormais en entier.");
        }
        List<ProbeReward> probes = game.takeReturnedProbes();
        if (!probes.isEmpty()) {
            ProbeReward first = probes.get(0);
            add(news, Notice.PROBES, probes.size() == 1 && first != null
                    ? "Sonde revenue : " + first.destination().label() + ". Son butin attend dans Big Bang, Sondes."
                    : probes.size() + " sondes revenues. Leur butin attend dans Big Bang, Sondes.");
        }
        // Les deux trous noirs (version 0.062) : l'énergie produite qu'ils demandent est atteinte.
        double produced = game.colliderEnergyProduced();
        if (blackHoleEnergySeen < Game.BLACK_HOLE_ENERGY && produced >= Game.BLACK_HOLE_ENERGY) {
            add(news, Notice.COLLIDER, "Collisionneur : assez d'énergie produite pour le trou noir.");
        }
        if (blackHoleEnergySeen < Game.SUPERMASSIVE_ENERGY && produced >= Game.SUPERMASSIVE_ENERGY) {
            add(news, Notice.COLLIDER, "Collisionneur : assez d'énergie produite pour le trou noir supermassif.");
        }
        blackHoleEnergySeen = produced;
        // Les jalons de la fin de partie (version 0.068) : un état de la matière réuni pour l'amas ou l'univers.
        for (idle.core.sky.Cosmos scale : new idle.core.sky.Cosmos[] {idle.core.sky.Cosmos.CLUSTER, idle.core.sky.Cosmos.UNIVERSE}) {
            if (game.hasCosmos(scale)) continue;
            for (idle.core.molecules.Molecule.State matter : scale.matter().keySet()) {
                if (game.isCosmosMilestoneMet(scale, matter) && cosmosMilestonesSeen.add(scale.name() + ":" + matter.name())) {
                    add(news, Notice.SKY, "Jalon : " + idle.ui.common.MatterText.plural(matter) + " réunis pour " + scale.phrase()
                            + ". Toutes les grandeurs +" + Math.round(Game.COSMOS_MILESTONE_GAIN * 100) + " % (" + game.cosmosMilestones() + "/10).");
                }
            }
        }

        if (game.bigBangs() > bigBangs) {
            // Tout vient de repartir de zéro : on reprend ses repères, pour que la partie suivante s'annonce comme la première.
            int count = game.bigBangs();
            if (game.completedBangChallenges() > bangChallengesDone && bangChallenge != null) {
                add(news, Notice.BIG_BANG, "Défi de Big Bang « " + bangChallenge.label() + " » réussi : " + lower(bangChallenge.rewardText()));
            }
            for (GameStats.Step step : records) add(news, Notice.RECORDS, "Record battu : " + step.label());
            snapshot();
            add(news, Notice.BIG_BANG, game.isDarkMatterUnlocked()
                    ? "Big Bang n° " + count + " : tout repart du premier générateur, mais l'arbre de matière noire est resté."
                    : count > 1 ? "Big Bang n° " + count + " : tout repart du premier générateur. Vos succès restent."
                    : "Big Bang : tout repart du premier générateur. Vos succès restent, et l'onglet Big Bang s'ouvre.");
            return news;
        }

        // Un palier d'espace vient d'être atteint : un rayon de molécules s'ouvre.
        if (game.moleculeKindsUnlocked() > moleculeKinds) {
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                if (kind.ordinal() >= moleculeKinds && game.isMoleculeKindUnlocked(kind)) {
                    add(news, Notice.MOLECULES, "Molécules : le rayon « " + kind.label() + " » s'ouvre");
                }
            }
        }

        // Les améliorations d'espace que le temps vient d'apporter ; l'ouverture d'un rayon s'annonce déjà ci-dessus.
        for (SpaceUpgrade upgrade : spaceUpgrades) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenKind) continue;
            add(news, Notice.MOLECULES, "Espace : « " + upgrade.name() + " » est acquise");
        }

        // Un défi de Big Bang vient de commencer : tout est reparti, comme après un Big Bang.
        if (game.activeBangChallenge() != null && game.activeBangChallenge() != bangChallenge) {
            BangChallenge started = game.activeBangChallenge();
            snapshot();
            add(news, Notice.BIG_BANG, "Défi de Big Bang « " + started.label() + " » : tout repart du premier générateur.");
            return news;
        }
        bangChallenge = game.activeBangChallenge();
        if (comet) {
            add(news, Notice.COMETS, "Une comète traverse l'expansion : " + (int) Game.COMET_VISIBLE_SECONDS + " secondes pour la saisir (Big Bang, Expansion de la matière)");
        }
        if (sky && skyEventsShown.getAsBoolean() && game.skyEvent() != null) {
            add(news, Notice.SKY_EVENTS, game.skyEvent().label() + " dans l'expansion : " + Format.wait(Game.SKY_EVENT_VISIBLE_SECONDS)
                    + " pour en profiter, si vous voulez (Big Bang, Expansion de la matière)");
        }
        for (GameStats.Step step : records) add(news, Notice.RECORDS, "Record battu : " + step.label());
        // Une collection vient de passer la moitié de son rayon, ou d'être complétée.
        for (KindCollection collection : game.collections()) {
            int level = game.collectionLevel(collection.kind());
            if (level > collections.getOrDefault(collection.kind(), 0)) {
                add(news, Notice.MOLECULES, "Collection " + collection.kind().label() + (level >= 2 ? " complète : " : " à moitié réunie : ")
                        + lower(BigBangPage.what(collection.stat())) + " ×" + ElementText.number(collection.factor(level)));
            }
            collections.put(collection.kind(), level);
        }

        // Ce que le rassemblement et la formation automatiques viennent de faire sans le joueur.
        if (gathered > 0) {
            add(news, Notice.AUTO_SKY, "Rassemblement automatique : " + gathered + (gathered > 1 ? " sortes rassemblées" : " sorte rassemblée"));
        }
        if (formed.size() > 3) {
            add(news, Notice.AUTO_SKY, "Formation automatique : " + formed.size() + " formés, dont « " + formed.get(formed.size() - 1) + " »");
        } else {
            for (String name : formed) add(news, Notice.AUTO_SKY, "Formation automatique : « " + name + " »");
        }

        if (game.state().explosions() > explosions) {
            // Une explosion qui compte alourdit le tableau ; celle d'un défi rejoué ne change rien.
            add(news, Notice.EXPLOSION, game.state().tableWeightLevel() > weightLevel
                    ? "Explosion : +" + Format.darkMatter(prime ? game.darkMatterPerExplosion().add(game.speedPrime())
                                    : game.darkMatterPerExplosion()) + " matière noire"
                            + (prime ? ", prime de vitesse comprise" : "")
                            + (game.state().tableWeightLevel() <= Game.TABLE_WEIGHT_MAX_LEVEL
                                    ? ", tableau " + Format.multiplier(game.tableWeight()) + " plus lourd qu'au début" : "")
                    : "Explosion : défi rejoué, sans matière noire");
        }
        for (Challenge challenge : game.challenges()) {
            if (game.isChallengeCompleted(challenge.id()) && !challengesDone.contains(challenge.id())) {
                add(news, Notice.CHALLENGES, "Défi réussi : " + challenge.name() + "  (" + ChallengesPane.reward(challenge) + ")");
            }
            if (game.isChallengeUnlocked(challenge.id()) && !challengesOpen.contains(challenge.id())) {
                add(news, Notice.CHALLENGES, "Nouveau défi : " + challenge.name());
            }
        }
        for (DarkAutomation automation : game.darkAutomations()) {
            if (game.isDarkAutomationUnlocked(automation.id()) && !darkAutomations.contains(automation.id())) {
                add(news, Notice.AUTOMATION, "Nouvel automatisme de matière noire : " + automation.name());
            }
        }

        if (game.isPeriodicTableUnlocked() && !tableUnlocked) {
            add(news, Notice.AUTOMATION, "Automatisation et tableau périodique débloqués");
        }
        if (game.isSynthesisAutomationUnlocked() && !synthesisUnlocked) {
            add(news, Notice.AUTOMATION, "Élément unique ★ : la synthèse automatique est débloquée");
        }

        // Les éléments nouveaux : un seul se nomme, plusieurs se comptent.
        List<Element> fresh = new ArrayList<>();
        for (int number : game.state().elements().keySet()) {
            if (!elements.contains(number)) fresh.add(PeriodicTable.element(number));
        }
        if (fresh.size() == 1) {
            Element element = fresh.get(0);
            add(news, Notice.ELEMENTS, "Nouvel élément : " + element.name() + " (" + element.symbol() + ")  —  " + ElementText.describe(element.effect()));
        } else if (fresh.size() > 1 && game.state().explosions() == explosions) {
            add(news, Notice.ELEMENTS, fresh.size() + " nouveaux éléments");
        }
        for (ElementSet set : game.elementSets()) {
            double before = setStrengths.getOrDefault(set.id(), 0.0);
            double now = game.setStrength(set);
            if (now > before && game.state().explosions() == explosions) {
                add(news, Notice.ELEMENTS, (now >= 1 ? "Ensemble complet : " : "Ensemble à moitié réuni : ") + set.name()
                        + "  —  " + ElementText.describe(set.effect()) + (now >= 1 ? "" : " (" + ElementText.percent(now) + " du bonus)"));
            }
        }

        // Un automatisme offert par la matière noire : ceux que le joueur achète, il le sait déjà.
        if (isGranting()) {
            for (Automation automation : game.automations()) {
                if (game.ownsAutomation(automation.id()) && !automations.contains(automation.id())
                        && game.state().explosions() == explosions) {
                    add(news, Notice.AUTOMATION, "Automatisme offert : " + automation.name());
                }
            }
        }

        int milestones = game.speedMilestones();
        if (milestones > speedMilestones && milestones <= ANNOUNCED_SPEED_MILESTONES) {
            add(news, Notice.MILESTONES, "Palier de vitesse n° " + milestones + " : particules multipliées");
        }
        int reached = game.landmarksReached();
        if (reached > landmarks) {
            Landmark landmark = SizeScale.MILESTONES.get(reached - 1);
            add(news, Notice.MILESTONES, reached - landmarks == 1 ? "Palier de taille : " + landmark.name()
                    : (reached - landmarks) + " paliers de taille, jusqu'à : " + landmark.name());
        }
        if (game.canExplode() && !canExplode) {
            add(news, Notice.EXPLOSION, "Le tableau périodique peut exploser");
        }
        for (Achievement achievement : achievements) {
            add(news, Notice.ACHIEVEMENTS, AchievementText.announce(achievement));
        }

        snapshot();
        return news;
    }

    /** Une nouvelle : sa catégorie (version 0.146), qui se coupe dans les réglages, et sa phrase. */
    record News(Notice kind, String text) { }

    private static void add(List<News> news, Notice kind, String text) {
        news.add(new News(kind, text));
    }

    /**
     * Les phrases à afficher, au plus {@link #MAX_PER_POLL} : au-delà, les dernières se résument (« … et 3 autres
     * nouvelles »). Version 0.146 : le résumé se fait après le tri des catégories coupées, pour ne compter que ce qui
     * s'affiche.
     */
    static List<String> summarize(List<String> texts) {
        if (texts.size() <= MAX_PER_POLL) return texts;
        int more = texts.size() - (MAX_PER_POLL - 1);
        List<String> shown = new ArrayList<>(texts.subList(0, MAX_PER_POLL - 1));
        shown.add("… et " + more + " autres nouvelles");
        return shown;
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
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) finalTiersSeen[d] = game.isFinalChallengeOpen() && game.isFinalTierReached(d);
        finishedSeen = game.isGameFinished();
        slowSeen = game.firstRunSlowdown();
        skinsSeen.clear();
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) if (game.hasSkin(skin)) skinsSeen.add(skin);
        colliderUpgradesSeen = game.isColliderUpgradesUnlocked();
        voidOfferedSeen = game.isVoidOffered();
        hadronsOfferedSeen = game.isHadronsOffered();
        gluonMilestonesSeen.clear();
        for (GluonMilestone milestone : GluonMilestone.values()) if (game.isGluonMilestoneReached(milestone)) gluonMilestonesSeen.add(milestone);
        voidHoleSeen = game.isVoidHoleOpen();
        voidMaxedSeen.clear();
        for (Game.VoidUpgrade upgrade : Game.VoidUpgrade.values()) if (game.isVoidUnlocked() && game.voidLevel(upgrade) >= upgrade.max()) voidMaxedSeen.add(upgrade);
        colliderAchievementsSeen.clear();
        for (ColliderAchievement achievement : ColliderAchievement.values()) {
            if (game.hasColliderAchievement(achievement)) colliderAchievementsSeen.add(achievement);
        }
        bangAchievementsSeen.clear();
        for (BangAchievement achievement : BangAchievement.values()) {
            if (game.hasBangAchievement(achievement)) bangAchievementsSeen.add(achievement);
        }
        blackHoleEnergySeen = game.colliderEnergyProduced();
        cosmosMilestonesSeen.clear();
        for (idle.core.sky.Cosmos scale : idle.core.sky.Cosmos.values()) {
            for (idle.core.molecules.Molecule.State matter : scale.matter().keySet()) {
                if (game.isCosmosMilestoneMet(scale, matter)) cosmosMilestonesSeen.add(scale.name() + ":" + matter.name());
            }
        }
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
        for (KindCollection collection : game.collections()) {
            collections.put(collection.kind(), game.collectionLevel(collection.kind()));
        }
        bangChallengesDone = game.completedBangChallenges();
        bangChallenge = game.activeBangChallenge();
    }

    private static String lower(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /** Le nom d'une amélioration du vide qui raccourcit le troisième acte, comme dans son onglet. */
    private static String voidTitle(Game.VoidUpgrade upgrade) {
        return switch (upgrade) {
            case MATURE -> "Maturation plus courte";
            case SETTLE -> "Rassemblement plus court";
            case LIGHTEN -> "Allègement du ciel";
            default -> upgrade.name();
        };
    }
}
