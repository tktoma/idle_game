package idle.ui.bigbang;

import idle.core.Game;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.ui.GameApp;
import idle.ui.Readiness;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.MatterText;
import idle.ui.probes.ProbesPane;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * La page du Big Bang : la mise à jour des paliers, des améliorations, des états de la matière et des assemblages.
 */
public abstract class BigBangPageRefresh extends BigBangPageControls {

    BigBangPageRefresh(Game game) {
        super(game);
    }

    // Ce que fournissent les parties suivantes.
    abstract void refreshMolecules();
    abstract void refreshExpansion();


    /** Recopie l'état du jeu dans les textes. */
    public void refresh() {
        int count = game.bigBangs();
        countLabel.setText(count + (count > 1 ? " Big Bangs" : " Big Bang"));
        refreshCycle();
        sinceLabel.setText("Partie en cours depuis " + Format.duration(game.stats().runTime())
                + ". Le prochain se déclenche au bout de l'arbre de matière noire.");
        long created = game.moleculesCreated();
        moleculesTab.setText(created > 0 ? "Molécules (" + Format.count(BigNum.of(created)) + ")" : "Molécules");
        expansionTab.setText("Expansion de la matière" + Readiness.dot(Readiness.of(game).comet()));
        milestonesTab.setText("Paliers (" + game.bigBangMilestonesReached() + "/" + game.bigBangMilestones().size() + ")");
        // Le rassemblement n'a son onglet qu'une fois ouvert ; si on le regardait et qu'il se referme (remise à zéro), retour au début.
        boolean states = game.isStatesUnlocked();
        statesTab.setVisible(states);
        statesTab.setManaged(states);
        int formed = game.substancesFormed();
        Readiness ready = Readiness.of(game);
        statesTab.setText((formed > 0 ? "États de la matière (" + formed + ")" : "États de la matière") + Readiness.dot(ready.states()));
        if (selected == 3 && !states) select(0);
        boolean assemblies = game.isAssembliesUnlocked();
        assembliesTab.setVisible(assemblies);
        assembliesTab.setManaged(assemblies);
        int assembled = game.assembliesFormed();
        assembliesTab.setText((assembled > 0 ? "Assemblages (" + assembled + ")" : "Assemblages") + Readiness.dot(ready.assemblies()));
        if (selected == 4 && !assemblies) select(0);
        boolean bodies = game.isBodiesUnlocked();
        bodiesTab.setVisible(bodies);
        bodiesTab.setManaged(bodies);
        int sky = game.bodiesFormed();
        bodiesTab.setText((sky > 0 ? "Astres (" + sky + ")" : "Astres") + Readiness.dot(ready.bodies()));
        if (selected == 5 && !bodies) select(0);
        // La galaxie n'a son onglet qu'à la première étoile.
        boolean galaxy = game.isGalaxyUnlocked();
        galaxyTab.setVisible(galaxy);
        galaxyTab.setManaged(galaxy);
        galaxyTab.setText((game.cosmosFormed() == 0 ? "Univers" : game.nextCosmos() == null ? "Univers (formé)"
                : "Univers (" + game.cosmosFormed() + "/" + Cosmos.values().length + ")") + Readiness.dot(ready.cosmos()));
        if (selected == 6 && !galaxy) select(0);
        boolean probes = game.isProbesUnlocked();
        probesTab.setVisible(probes);
        probesTab.setManaged(probes);
        int waiting = game.state().probeRewards().size();
        probesTab.setText((waiting > 0 ? "Sondes (" + waiting + ")" : "Sondes") + Readiness.dot(ProbesPane.ready(game)));
        if (selected == 8 && !probes) select(0);
        boolean systems = game.isSolarSystemsUnlocked();
        systemsTab.setVisible(systems);
        systemsTab.setManaged(systems);
        int complete = game.solarSystemsComplete();
        systemsTab.setText("Systèmes solaires (" + complete + "/" + Game.SYSTEMS_FOR_GALAXY + ")"
                + Readiness.dot(systems && complete < Game.SYSTEMS_FOR_GALAXY && !game.freeForSystems(Body.Tier.PLANET).isEmpty()
                        && (!game.state().solarSystems().isEmpty() || !game.freeForSystems(Body.Tier.STAR).isEmpty())));
        if (selected == 9 && !systems) select(0);
        // La liste de courses accompagne les trois sous-pages où l'on forme quelque chose.
        if (selected >= 4 && selected <= 6) {
            shoppingPane.refresh();
        } else {
            shoppingPane.setVisible(false);
            shoppingPane.setManaged(false);
        }
        switch (selected) {
            case 0 -> refreshMolecules();
            case 1 -> refreshExpansion();
            case 2 -> refreshUpgrades();
            case 3 -> refreshStates();
            case 4 -> refreshAssemblies();
            case 5 -> bodiesPane.refresh();
            case 6 -> galaxyPane.refresh();
            case 8 -> probesPane.refresh();
            case 9 -> systemsPane.refresh();
            default -> refreshMilestones();
        }
    }

    /** Les cinq paliers de Big Bang : ceux qui sont atteints, le prochain, et ceux qui attendent. */
    private void refreshMilestones() {
        int count = game.bigBangs();
        BigBangMilestone next = game.nextBigBangMilestone();
        milestonesIntro.setText(next == null
                ? "Les " + game.bigBangMilestones().size() + " paliers sont atteints. Un Big Bang de plus ajoute encore "
                        + ElementText.number(Game.SPACE_PER_SECOND) + " unité d'espace par seconde, rien d'autre."
                : "Chaque Big Bang déclenché, jusqu'au huitième, donne un gros bonus pour de bon, parfois plusieurs (" + count
                        + (count > 1 ? " Big Bangs déclenchés" : " Big Bang déclenché") + ")."
                        + Detail.only(" Rien à acheter : un palier agit dès que son Big Bang a eu lieu, et ni l'explosion ni un "
                                + "autre Big Bang ne le reprennent. C'est ce qui paie le détour : après un Big Bang tout repart "
                                + "d'un seul générateur, et le tableau périodique reste vide quelques heures, donc sans nouvelle "
                                + "molécule. Chaque Big Bang ajoute aussi, comme toujours, "
                                + ElementText.number(Game.SPACE_PER_SECOND) + " unité d'espace par seconde."));
        milestoneCards.forEach((milestone, card) -> {
            boolean reached = game.isBigBangMilestoneReached(milestone);
            // Un même Big Bang peut donner deux paliers : tous ceux du prochain Big Bang sont « à venir ».
            boolean coming = next != null && milestone.bigBangs() == next.bigBangs();
            StringBuilder line = new StringBuilder();
            StringBuilder detail = new StringBuilder();
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (line.length() > 0) line.append("\n");
                if (detail.length() > 0) detail.append(' ');
                line.append(milestoneLine(effect));
                detail.append(milestoneDetail(effect));
            }
            int missing = milestone.bigBangs() - count;
            card.show(reached ? Card.State.DONE : coming ? Card.State.WAITING : Card.State.LOCKED,
                    rank(milestone.bigBangs()) + " Big Bang", reached ? "atteint" : "", milestone.name(), line.toString(),
                    Detail.shown() ? detail.toString() : "",
                    reached ? "" : coming && missing == 1 ? "Au prochain Big Bang" : "Dans " + missing + " Big Bangs", "");
        });
        bangChallenges.refresh();
    }

    /** « la racine carrée », ou « 2 fois la racine carrée » une fois le palier de la gravité atteint. */
    private String root() {
        double factor = game.accretionFactor();
        return factor == 1 ? "la racine carrée" : ElementText.number(factor) + " fois la racine carrée";
    }

    /** Ce qu'une création ajoute à un amas de {@code count} molécules, avec les paliers atteints. */
    private int drawnAt(int count) {
        return 1 + (int) Math.floor(game.accretionFactor() * Math.sqrt(count) + 1e-9);
    }

    /** « 1er », « 2e », « 3e ». */
    private static String rank(int number) {
        return number == 1 ? "1er" : number + "e";
    }

    /** Ce que donne un palier, en quelques mots : « Espace par seconde ×2 ». */
    public static String milestoneLine(BigBangMilestone.Effect effect) {
        return switch (effect) {
            case BigBangMilestone.Boost boost -> BigBangPage.what(boost.stat()) + " ×" + ElementText.number(boost.factor());
            case BigBangMilestone.DarkMatter dark -> "Matière noire des explosions ×" + ElementText.number(dark.factor());
            case BigBangMilestone.Accretion accretion -> "Les amas attirent ×" + ElementText.number(accretion.factor());
            case BigBangMilestone.DarkMatterSpace dark -> "La matière noire en réserve accélère l'expansion";
            case BigBangMilestone.AutoMolecules auto -> "Les amas choisis se créent tout seuls";
            case BigBangMilestone.KeepDarkTree keep -> "L'arbre de matière noire traverse le Big Bang";
            case BigBangMilestone.KeepChallenges keep -> "Les défis réussis le restent";
            case BigBangMilestone.Probes probes -> "Les sondes : une nouvelle sous-page";
            case BigBangMilestone.SolarSystems systems -> "Les systèmes solaires : la galaxie en demande " + Game.SYSTEMS_FOR_GALAXY;
            case BigBangMilestone.MorePlaces more -> "+" + more.maturation() + " places de maturation, +" + more.probes() + " sonde en vol";
            case BigBangMilestone.ExoticMatter exotic -> "Les matières exotiques : des astres de plus pour la galaxie";
        };
    }

    private String milestoneDetail(BigBangMilestone.Effect effect) {
        return switch (effect) {
            case BigBangMilestone.Boost boost -> switch (boost.stat()) {
                case SPACE -> "L'expansion crée " + ElementText.number(boost.factor()) + " fois plus d'espace chaque seconde. "
                        + "Cela se multiplie avec les inflations, les molécules, les assemblages et les astres.";
                case ATOMS -> "Chaque fusion donne " + ElementText.number(boost.factor()) + " fois plus d'atomes : le tableau "
                        + "périodique se regarnit bien plus vite après un Big Bang.";
                case PARTICLES -> "Chaque création donne " + ElementText.number(boost.factor()) + " fois plus de particules.";
                case DARK_GROWTH -> "La matière noire grossit " + ElementText.number(boost.factor()) + " fois plus vite.";
            };
            case BigBangMilestone.DarkMatter dark -> "Chaque explosion laisse " + ElementText.number(dark.factor())
                    + " fois plus de matière noire. L'arbre et le Big Bang suivant en demandent autant qu'avant : il faut donc "
                    + "bien moins d'explosions pour refaire le chemin.";
            case BigBangMilestone.Accretion accretion -> "Dans une sorte rassemblée, une création ajoute une molécule plus "
                    + ElementText.number(accretion.factor()) + " fois la racine carrée de l'amas, au lieu d'une fois : "
                    + "21 d'un coup à 100 molécules, 201 à 10 000.";
            case BigBangMilestone.DarkMatterSpace dark -> "L'espace par seconde est multiplié par 1 + "
                    + ElementText.number(dark.perRoot()) + " × la racine carrée de la matière noire en réserve : ×"
                    + ElementText.number(1 + dark.perRoot() * Math.sqrt(100)) + " avec 100, "
                    + "×" + ElementText.number(1 + dark.perRoot() * Math.sqrt(500)) + " avec 500, ×"
                    + ElementText.number(1 + dark.perRoot() * Math.sqrt(2_500)) + " avec 2 500"
                    + (game.darkMatterSpaceBoost() > 1 ? " (×" + ElementText.number(game.darkMatterSpaceBoost()) + " en ce moment)" : "")
                    + ". L'arbre fini, chaque explosion de plus sert donc encore ; dépenser la matière noire ralentit "
                    + "l'expansion, et un Big Bang la reprend : ce bonus repart alors de rien, le temps qu'elle revienne.";
            case BigBangMilestone.AutoMolecules auto -> "Donne l'automatisme « Création automatique », à régler dans l'onglet "
                    + "Automatisation. Dans la sous-page États de la matière, un clic sur un amas le lui confie : toutes les "
                    + ElementText.number(game.moleculeAutomationInterval()) + " secondes, il y fait une création, au même prix "
                    + "qu'à la main, tant que le tableau périodique et l'espace le permettent.";
            case BigBangMilestone.SolarSystems systems -> "Une sous-page « Systèmes solaires » s'ouvre : autour d'une étoile mûre, "
                    + "on range des planètes, de la plus proche à la plus lointaine, et des lunes. Un système est complet à "
                    + Game.SYSTEM_MIN_PLANETS + " planètes, et la galaxie en demande " + Game.SYSTEMS_FOR_GALAXY + ". Chaque point "
                    + "d'harmonie d'un système complet (une planète bien placée, une lune, une étoile géante, la zone habitable) "
                    + "ajoute " + ElementText.percent(Game.SYSTEM_GAIN) + " à toutes les grandeurs.";
            case BigBangMilestone.MorePlaces more -> "Trois astres mûrissaient à la fois : il en mûrit " + more.maturation()
                    + " de plus. Et une sonde de plus peut voler, en plus du hangar.";
            case BigBangMilestone.ExoticMatter exotic -> "Les matières exotiques s'ouvrent dans les astres : pulsar, magnétar, "
                    + "nébuleuses, quasar. Ils ne demandent aucun assemblage, seulement des astres déjà formés et beaucoup de "
                    + "matière rassemblée, et la galaxie les demande aussi.";
            case BigBangMilestone.Probes probes -> "Une sous-page « Sondes » s'ouvre : une sonde part de 15 minutes à 8 heures "
                    + "et rapporte des éléments d'un rayon, de l'espace, des sursauts d'expansion et des échantillons. Chaque "
                    + "récompense se réclame d'un clic. Les échantillons paient les améliorations des sondes ; tout traverse le Big Bang.";
            case BigBangMilestone.KeepChallenges keep -> "Dès ce Big Bang-là, et à tous les suivants, les défis réussis "
                    + "traversent le Big Bang : ils restent réussis, avec leurs récompenses, et la condition « les "
                    + game.challenges().size() + " défis réussis » du Big Bang suivant est déjà remplie. Rien n'empêche de les "
                    + "rejouer pour leur temps.";
            case BigBangMilestone.KeepDarkTree keep -> "Dès ce Big Bang-là, et à tous les suivants, l'arbre de matière noire "
                    + "reste en place : ses cases, ses automatismes et leurs réglages. Le Big Bang ne reprend plus que la "
                    + "matière noire en réserve, sa taille et la masse du tableau. Le chemin à refaire "
                    + "tombe de quelques heures à un quart d'heure, et toute la matière noire des explosions va à la "
                    + "réserve, donc à l'expansion.";
        };
    }

    private void refreshUpgrades() {
        upgradesIntro.setText("Une amélioration est acquise d'elle-même quand l'expansion a créé assez d'espace ("
                + Format.count(game.state().space()) + " en ce moment). Elle ne coûte rien, et il n'y a rien à cliquer."
                + Detail.only(" C'est l'espace gagné depuis le premier Big Bang qui compte, pas celui qui reste libre : "
                        + "remplir l'espace de molécules et de leurs lieux de rassemblement ne retarde aucune amélioration. Les "
                        + "rayons de molécules s'ouvrent l'un après l'autre ; chacun n'est montré qu'une fois le précédent "
                        + "ouvert. Les premières multiplient pour de bon les atomes, les particules et l'espace : après un Big "
                        + "Bang tout repart d'un seul générateur, et ce sont elles qui raccourcissent le chemin. Ni "
                        + "l'explosion ni le Big Bang ne reprennent une amélioration."));
        upgradeCards.forEach((upgrade, card) -> {
            boolean owned = game.ownsSpaceUpgrade(upgrade.id());
            boolean available = game.isSpaceUpgradeAvailable(upgrade.id());
            upgradesGrid.show(card, owned || available);
            if (!owned && !available) return;
            boolean ready = game.canBuySpaceUpgrade(upgrade.id());
            double wait = game.secondsUntilSpace(upgrade.space());
            String line = switch (upgrade.effect()) {
                case SpaceUpgrade.OpenKind open -> "Ouvre un rayon de " + kindSize(open.kind()) + " molécules";
                case SpaceUpgrade.OpenStates states -> "Rassembler les molécules selon leur état";
                case SpaceUpgrade.OpenAssemblies assemblies -> "Assembler des roches, des minerais, des eaux, des gaz";
                case SpaceUpgrade.OpenBodies bodies -> "Former des astres, de l'amas de roches au trou noir";
                case SpaceUpgrade.AutoHold hold -> "La matière noire grossit sans tenir le clic";
                case SpaceUpgrade.HoldCreate hold -> "Tenir le clic répète la création";
                case SpaceUpgrade.BulkForm bulk -> "Tout rassembler, assembler ou former d'un clic";
                case SpaceUpgrade.AutoGather gather -> "Les sortes se rassemblent toutes seules";
                case SpaceUpgrade.AutoForm form -> "Assemblages et astres se forment tout seuls";
                case SpaceUpgrade.CreationPace pace -> "Création automatique toutes les " + ElementText.number(pace.seconds()) + " s";
                case SpaceUpgrade.Boost boost -> BigBangPage.what(boost.stat()) + " ×" + ElementText.number(boost.factor());
            };
            String detail = switch (upgrade.effect()) {
                case SpaceUpgrade.OpenKind open -> "Le rayon « " + open.kind().label() + " » apparaît dans la sous-page "
                        + "Molécules. Chacune de ses molécules donne un léger bonus ou relève le plafond d'un de ses "
                        + "éléments, et a son état : gaz, liquide, solide, cristal ou métal.";
                case SpaceUpgrade.OpenStates states -> "Ouvre la sous-page « États de la matière » : à partir de "
                        + Game.SUBSTANCE_MOLECULES + " molécules d'une même sorte, un achat unique les rassemble dans le "
                        + "lieu de leur état : gaz, liquides, solides, cristaux ou métaux. Aucune n'est consommée ; leur bonus est "
                        + "renforcé, la sorte ajoute des particules ou des atomes, et surtout son amas attire la matière : "
                        + "chaque création y ajoute plusieurs molécules d'un coup, de plus en plus.";
                case SpaceUpgrade.OpenAssemblies assemblies -> "Ouvre la sous-page « Assemblages » : plusieurs sortes de "
                        + "molécules rassemblées, par centaines, forment une matière de la nature : une roche, un minerai, "
                        + "une pierre précieuse, une eau, un gaz ou un hydrocarbure. Rien de vivant, rien de fabriqué. "
                        + "Aucune molécule n'est consommée.";
                case SpaceUpgrade.OpenBodies bodies -> "Ouvre la sous-page « Astres » : des assemblages, de la matière rassemblée et "
                        + "des molécules se cumulent pour former un amas de roches, puis une comète, un astéroïde, une lune, une "
                        + "planète, puis des étoiles et des trous noirs, chacun de plusieurs types et chacun parti des plus petits.";
                case SpaceUpgrade.AutoHold hold -> "Donne l'automatisme « Appui automatique », à régler dans l'onglet "
                        + "Automatisation : la matière noire grossit comme si vous teniez le clic sur son point, à pleine "
                        + "vitesse et depuis n'importe quel onglet. Il sert dès qu'il y a de la matière noire, donc après la "
                        + "première explosion qui suit un Big Bang, et ni l'explosion ni le Big Bang ne le reprennent.";
                case SpaceUpgrade.HoldCreate hold -> "Dans la sous-page Molécules, tenir le clic sur une carte répète sa "
                        + "création, plusieurs fois par seconde, tant que le tableau périodique et l'espace suivent. Un "
                        + "clic simple crée toujours une seule fois.";
                case SpaceUpgrade.BulkForm bulk -> "Ajoute un bouton en haut de trois sous-pages : « Tout rassembler » dans "
                        + "les États de la matière, « Tout assembler » dans les Assemblages, « Former les astres prêts » dans "
                        + "les Astres. Chacun fait d'un coup tout ce qui est possible, dans l'ordre du catalogue.";
                case SpaceUpgrade.AutoGather gather -> "Donne l'automatisme « Rassemblement automatique », à régler dans "
                        + "l'onglet Automatisation : dès qu'une sorte compte " + Game.SUBSTANCE_MOLECULES + " molécules et que "
                        + "l'espace libre suffit à son lieu, elle se rassemble d'elle-même, une fois par seconde.";
                case SpaceUpgrade.AutoForm form -> "Donne l'automatisme « Formation automatique », à régler dans l'onglet "
                        + "Automatisation : tout assemblage et tout astre dont les conditions sont réunies se forme de "
                        + "lui-même, et une notification l'annonce. La galaxie, l'amas de galaxies et l'univers restent à vous.";
                case SpaceUpgrade.CreationPace pace -> "La création automatique passe sur les amas confiés toutes les "
                        + ElementText.number(pace.seconds()) + " secondes au lieu de toutes les "
                        + ElementText.number(Game.MOLECULE_AUTOMATION_SECONDS) + ". Elle ne sert qu'une fois la création "
                        + "automatique acquise, au deuxième Big Bang.";
                case SpaceUpgrade.Boost boost -> switch (boost.stat()) {
                    case ATOMS -> "Chaque fusion donne " + ElementText.number(boost.factor()) + " fois plus d'atomes, pour de bon. "
                            + "Après un Big Bang il faut refaire tout le chemin depuis un seul générateur : avec ceci, le "
                            + "tableau périodique s'ouvre en quelques minutes et se remplit bien plus vite que la première fois.";
                    case PARTICLES -> "Chaque création donne " + ElementText.number(boost.factor()) + " fois plus de particules, "
                            + "pour de bon. Avec la Matière primordiale, c'est ce qui rend court le chemin jusqu'au Big Bang suivant.";
                    case SPACE -> "L'expansion crée " + ElementText.number(boost.factor()) + " fois plus d'espace chaque seconde, "
                            + "pour de bon. Les inflations se multiplient entre elles, puis avec les Big Bangs, les molécules, "
                            + "les assemblages et les astres qui agissent sur l'espace.";
                    case DARK_GROWTH -> "La matière noire grossit " + ElementText.number(boost.factor()) + " fois plus vite, pour de bon.";
                };
            };
            card.show(owned ? Card.State.DONE : ready ? Card.State.READY : Card.State.WAITING,
                    "", owned ? "acquise" : upgrade.effect() instanceof SpaceUpgrade.OpenKind ? "rayon"
                            : upgrade.effect() instanceof SpaceUpgrade.AutoHold || upgrade.effect() instanceof SpaceUpgrade.AutoGather
                                    || upgrade.effect() instanceof SpaceUpgrade.AutoForm ? "automatisme"
                            : upgrade.effect() instanceof SpaceUpgrade.Boost ? "pour de bon" : "",
                    upgrade.name(), line, detail,
                    owned ? "" : "À " + Format.count(upgrade.space()) + " d'espace créé",
                    owned || ready || Double.isInfinite(wait) ? "" : "dans " + Format.wait(wait));
        });
    }

    /** Nombre de molécules d'un rayon du catalogue. */
    private int kindSize(Molecule.Kind wanted) {
        int size = 0;
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() == wanted) size++;
        }
        return size;
    }

    private void refreshStates() {
        Map<Molecule.State, int[]> places = new EnumMap<>(Molecule.State.class);    // par lieu : cartes montrées, sortes rassemblées, molécules rangées
        for (Molecule molecule : game.molecules()) {
            if (!molecule.hasState()) continue;
            Molecule.State matter = molecule.state();
            int molecules = game.moleculeCount(molecule.id());
            boolean gathered = game.hasSubstance(molecule.id());
            // Une molécule n'a sa carte ici que lorsque le joueur en a créé, ou les a déjà rassemblées.
            boolean visible = molecules > 0 || gathered;
            Card card = visible ? stateCard(molecule) : stateCards.get(molecule);
            if (card != null) placeGrids.get(matter).show(card, visible);
            if (!visible) continue;
            int[] place = places.computeIfAbsent(matter, each -> new int[3]);
            place[0]++;
            if (gathered) {
                place[1]++;
                place[2] += molecules;
            }
            boolean ready = game.canFormSubstance(molecule.id());
            // Ce qu'écrit la carte ne dépend que de ces quelques valeurs : tant qu'elles n'ont pas bougé, elle dit déjà ce
            // qu'il faut. Seul l'espace qui manque change à chaque image ; cette carte-là est toujours recomposée.
            boolean lacking = !gathered && !ready && molecules >= Game.SUBSTANCE_MOLECULES;
            // Ce qu'une création ajoute dépend aussi du palier de la gravité : il entre dans la clé.
            boolean automation = gathered && game.isMoleculeAutomationUnlocked();
            boolean automated = automation && game.isMoleculeAutomated(molecule.id());
            long key = lacking ? -1 : 64L * molecules + (gathered ? 1 : 0) + (ready ? 2 : 0) + (Detail.shown() ? 4 : 0)
                    + (game.accretionFactor() > 1 ? 8 : 0) + (automation ? 16 : 0) + (automated ? 32 : 0);
            if (card.filledWith(key) && !lacking) continue;
            BigNum space = game.substanceSpace(molecule.id());
            String power = ElementText.number(matter.exponent());
            // Un plafond ne se relève que d'exemplaires entiers : la carte montre ce qui compte vraiment.
            double effective = game.effectiveMolecules(molecule.id());
            String counted = ElementText.number(molecule.bonus() instanceof Molecule.Uncap ? Math.floor(effective + 1e-9) : effective);
            String whole = molecule.bonus() instanceof Molecule.Uncap ? ", arrondi en dessous pour un plafond" : "";
            int drawn = game.moleculesPerCreation(molecule.id());
            String line = gathered
                    ? molecules + (molecules > 1 ? " molécules comptées pour " : " molécule comptée pour ") + counted
                            + "\n" + capitalized(generation(matter, Math.max(1, effective)))
                            + "\nUne création en ajoute " + drawn
                    : "Renforce son bonus et attire la matière\n" + capitalized(generation(matter, 1)) + " par doublement";
            String doubled = " Une sorte compte pour ses doublements : 1 molécule compte pour 1, 3 pour 2, 7 pour 3, 15 pour 4, "
                    + "et ainsi de suite. ";
            String detail = !Detail.shown() ? "" : gathered
                    ? "Rassemblées dans " + placeName(matter) + "." + doubled + "Rassemblée, ce compte est élevé à la puissance "
                            + power + whole + " : c'est lui qui multiplie le bonus (" + BigBangPage.bonus(molecule) + ") et ce que l'état "
                            + "ajoute (" + generation(matter, 1) + " par unité). L'amas attire la matière : chaque création y "
                            + "ajoute une molécule, plus " + root() + " de leur nombre, pour le prix d'une seule et s'il "
                            + "y a la place."
                    : "Un seul achat : toutes vos " + molecule.formula() + " vont se ranger dans " + placeName(matter)
                            + ", sans être consommées." + doubled + "Rassemblée, ce compte est élevé à la puissance " + power + whole
                            + " : 3 molécules comptent pour " + ElementText.number(Math.pow(Game.doublings(3), matter.exponent()))
                            + ", 100 pour " + ElementText.number(Math.pow(Game.doublings(100), matter.exponent()))
                            + ". La sorte ajoute aussi " + generation(matter, 1) + " par unité de ce compte. Enfin l'amas "
                            + "attire la matière : chaque création y ajoute une molécule, plus " + root() + " de leur "
                            + "nombre (" + drawnAt(9) + " d'un coup à 9 molécules, " + drawnAt(100) + " à 100, "
                            + drawnAt(10_000) + " à 10 000), pour le prix d'une seule.";
            String price = automated ? "Création automatique : retirer"
                    : automation ? "Confier à la création automatique"
                    : gathered ? ""
                    : ready ? "Rassembler : " + Format.count(space) + " d'espace"
                    : molecules < Game.SUBSTANCE_MOLECULES ? "Il faut " + Game.SUBSTANCE_MOLECULES + " " + molecule.formula()
                            + " (" + molecules + ")"
                    : "Il manque " + Format.count(space.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
            card.setDoneClickable(automation);
            card.show(automated ? Card.State.ON : gathered ? Card.State.DONE : ready ? Card.State.READY : Card.State.WAITING,
                    "×" + Format.count(BigNum.of(molecules)), automated ? "automatique" : gathered ? "rassemblées" : "", molecule.name(), line, detail, price, "");
        }
        int shown = 0;
        for (Molecule.State matter : Molecule.State.values()) {
            int[] place = places.getOrDefault(matter, new int[3]);
            shown += place[0];
            Label title = placeTitles.get(matter);
            title.setText(MatterText.signed(matter) + (place[1] == 0 ? " : rien de rassemblé"
                    : " : " + place[2] + (place[2] > 1 ? " molécules de " : " molécule de ") + place[1]
                            + (place[1] > 1 ? " sortes" : " sorte")));
            title.setVisible(place[0] > 0);
            title.setManaged(place[0] > 0);
        }
        showBulk(gatherAll, game.isBulkFormUnlocked() && shown > 0, "Tout rassembler", game.substancesReady());
        int gatheredKinds = game.state().substances().size();
        boolean entrust = game.isMoleculeAutomationUnlocked() && gatheredKinds > 0;
        entrustAll.setVisible(entrust);
        entrustAll.setManaged(entrust);
        if (entrust) {
            int entrusted = game.automatedMolecules();
            entrustAll.setText(entrusted >= gatheredKinds ? "Tout retirer à la création automatique"
                    : "Tout confier à la création automatique (" + (gatheredKinds - entrusted) + ")");
        }
        statesIntro.setText(shown == 0
                ? "Créez d'abord une molécule : elle apparaîtra ici, dans le lieu où elle se rassemble."
                : "À partir de " + Game.SUBSTANCE_MOLECULES + " molécules d'une sorte, un achat unique les rassemble dans le lieu "
                        + "de leur état. Un amas attire la matière : chaque création y ajoute alors plusieurs molécules d'un "
                        + "coup (" + Format.count(game.freeSpace()) + " d'espace libre)."
                        + (!game.isMoleculeAutomationUnlocked() ? "" : " Un clic sur un amas le confie à la création automatique ("
                                + game.automatedMolecules() + (game.automatedMolecules() > 1 ? " amas confiés" : " amas confié")
                                + (game.isMoleculeAutomationEnabled() ? ")." : ", automatisme coupé)."))
                        + Detail.only(" Aucune molécule n'est consommée : elles se rangent toutes dans le lieu de leur état, "
                                + "visible dans l'expansion de la matière, les plus serrés au plus près du centre : les "
                                + "métaux, les cristaux, les solides, les liquides, puis les gaz. Le lieu réserve de l'espace une fois "
                                + "pour toutes : la place de " + Game.SUBSTANCE_MOLECULES + " de ses molécules. Une molécule de "
                                + "gaz occupe " + ElementText.number(Molecule.State.GAS.spaceFactor()) + " fois la place d'un "
                                + "cristal du même poids, un liquide " + ElementText.number(Molecule.State.LIQUID.spaceFactor())
                                + " fois, un métal les trois quarts. Une sorte compte pour ses doublements (1 molécule, puis "
                                + "3, 7, 15…), et rassemblée ce compte est élevé à la puissance de son état : "
                                + ElementText.number(Molecule.State.GAS.exponent()) + " pour un gaz, "
                                + ElementText.number(Molecule.State.METAL.exponent()) + " pour un métal. C'est surtout par "
                                + "l'amas que viennent les grands nombres : chaque création y ajoute une molécule, plus la "
                                + "racine carrée de celles qu'il contient. Seules les molécules que vous avez déjà créées "
                                + "sont montrées."));
    }

    /**
     * La carte d'une molécule dans le lieu de son état, construite la première fois qu'on en a
     * besoin : le catalogue compte plusieurs centaines de molécules, et le joueur n'en possède
     * jamais qu'une partie. Les cartes d'un lieu sont donc dans l'ordre où leurs molécules ont été créées.
     */
    private Card stateCard(Molecule molecule) {
        return stateCards.computeIfAbsent(molecule, each -> {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setPicture(picture(molecule));
            // Pas encore rassemblée : le clic rassemble. Ensuite, avec la création automatique, il confie l'amas à l'automatisme.
            card.setOnAction(() -> {
                if (!game.hasSubstance(molecule.id())) game.formSubstance(molecule.id());
                else game.setMoleculeAutomated(molecule.id(), !game.isMoleculeAutomated(molecule.id()));
                refresh();
            });
            placeGrids.get(molecule.state()).add(card);
            return card;
        });
    }

    private void refreshAssemblies() {
        Map<Assembly.Family, int[]> families = new EnumMap<>(Assembly.Family.class);    // par famille : cartes montrées, assemblages formés
        for (Assembly assembly : game.assemblies()) {
            boolean formed = game.hasAssembly(assembly.id());
            // Un assemblage n'a sa carte que lorsque le joueur a créé au moins un de ses ingrédients, ou l'a déjà formé.
            boolean visible = formed;
            for (String ingredient : assembly.ingredients().keySet()) visible |= game.moleculeCount(ingredient) > 0;
            Card card = visible ? assemblyCard(assembly) : assemblyCards.get(assembly);
            if (card != null) familyGrids.get(assembly.family()).show(card, visible);
            if (!visible) continue;
            int[] family = families.computeIfAbsent(assembly.family(), each -> new int[2]);
            family[0]++;
            if (formed) family[1]++;
            boolean ready = game.canFormAssembly(assembly.id());
            // Ce que le joueur a de chaque ingrédient, hors des autres assemblages, et ce qui manque en premier.
            StringBuilder have = new StringBuilder();
            StringBuilder names = new StringBuilder();
            String missing = "";
            int complete = 0;
            for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
                Molecule molecule = game.molecule(ingredient.getKey());
                int need = ingredient.getValue();
                int spare = game.spareMolecules(molecule.id());
                if (have.length() > 0) have.append("  ·  ");
                have.append(molecule.formula()).append(' ').append(formed ? need : Math.min(spare, need) + "/" + need);
                if (names.length() > 0) names.append(", ");
                names.append(need).append(' ').append(molecule.name().toLowerCase());
                if (formed || spare >= need) {
                    complete++;
                } else if (missing.isEmpty()) {
                    missing = !game.hasSubstance(molecule.id())
                            ? (game.moleculeCount(molecule.id()) == 0 ? "Il faut créer " : "Il faut rassembler ") + molecule.formula()
                            : "Il manque " + (need - spare) + " " + molecule.formula();
                }
            }
            String gain = bonusOf(assembly);
            int sorts = assembly.ingredients().size();
            // La carte reste courte : le gain et la taille. La liste des ingrédients est pour le mode détails.
            card.show(formed ? Card.State.DONE : ready ? Card.State.READY : complete > 0 ? Card.State.STARTED : Card.State.WAITING,
                    formed ? "" : complete + "/" + sorts, formed ? "assemblé" : "", assembly.name(),
                    gain + "\n" + assembly.size() + " molécules de " + sorts + " sortes",
                    !Detail.shown() ? "" : (formed ? "Dans le bloc : " : "Ce que vous avez : ") + have + ". Fait de " + names + ". Les molécules ne sont "
                            + "pas consommées : elles quittent l'amas de leur sorte pour former un seul bloc, visible dans "
                            + "l'expansion de la matière, et gardent leur bonus. Un autre assemblage ne peut pas compter sur "
                            + "les mêmes molécules." + (game.isBodiesUnlocked() ? (game.hasBody(game.bodyOf(assembly.id()).id())
                                    ? " Ce bloc entoure maintenant un astre : " : " Il entre dans un astre : ")
                                    + game.bodyOf(assembly.id()).name().toLowerCase() + "." : ""),
                    formed ? "" : ready ? "Assembler" : missing, "");
        }
        int shown = 0;
        for (Assembly.Family family : Assembly.Family.values()) {
            int[] counts = families.getOrDefault(family, new int[2]);
            shown += counts[0];
            Label title = familyTitles.get(family);
            title.setText(family.label() + (counts[1] == 0 ? "" : " : " + counts[1] + (counts[1] > 1 ? " formés" : " formé")));
            title.setVisible(counts[0] > 0);
            title.setManaged(counts[0] > 0);
        }
        showBulk(assembleAll, game.isBulkFormUnlocked() && shown > 0, "Tout assembler", game.assembliesReady());
        assembliesIntro.setText(shown == 0
                ? "Créez d'abord des molécules : les assemblages qu'elles permettent apparaîtront ici."
                : "Plusieurs sortes de molécules rassemblées, par centaines, forment une matière de la nature : une roche, un "
                        + "minerai, une eau, un gaz. Un achat unique, qui ne consomme rien."
                        + Detail.only(" Chaque ingrédient doit être rassemblé dans les états de la matière, et vous devez en "
                                + "avoir le nombre demandé hors de tout autre assemblage. Les proportions sont à peu près "
                                + "celles de la matière réelle : la base par centaines, les traces à l'unité. Un assemblage "
                                + "augmente la grandeur de sa famille de 100 % par "
                                + ElementText.number(Assembly.MOLECULES_PER_WHOLE) + " molécules. Rien de vivant ici, et rien de fabriqué. Seuls "
                                + "les assemblages dont vous avez créé au moins un ingrédient sont montrés."));
    }

    /** Ce que donne un assemblage, en quelques mots : « Atomes par fusion +135 % ». */
    static String bonusOf(Assembly assembly) {
        return BigBangPage.what(assembly.boost().stat()) + " +" + ElementText.percent(assembly.boost().perMolecule());
    }

    /**
     * La carte d'un assemblage dans la section de sa famille, construite la première fois qu'on en
     * a besoin, avec un petit dessin de son bloc ({@link #blockPicture}).
     */
    private Card assemblyCard(Assembly assembly) {
        return assemblyCards.computeIfAbsent(assembly, each -> {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setPicture(blockPicture(assembly));
            card.setOnAction(() -> {
                game.formAssembly(assembly.id());
                refresh();
            });
            familyGrids.get(assembly.family()).add(card);
            return card;
        });
    }

    /**
     * Le dessin d'un assemblage sur sa carte : un bloc de sa couleur, au contour clair, semé de
     * grains de la couleur de chacun de ses ingrédients, en proportion. C'est le même rendu que dans
     * la vue de l'espace ({@link SpaceView}), en petit.
     */
    private HBox blockPicture(Assembly assembly) {
        double width = PICTURE_WIDTH;
        double height = 46;
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        g.setFill(SpaceView.BLOCK_EDGE);
        g.fillRoundRect(8, 3, width - 16, height - 6, 22, 22);
        g.setFill(Color.web(assembly.tint()));
        g.fillRoundRect(10.5, 5.5, width - 21, height - 11, 18, 18);
        // Trois rangées de grains, dont les couleurs suivent les proportions de l'assemblage, mêlées d'une façon fixe.
        int columns = 13;
        int rows = 3;
        java.util.List<Color> grains = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            Color color = MoleculeArt.of(game.molecule(ingredient.getKey())).dominant();
            int share = Math.max(1, (int) Math.round(columns * rows * ingredient.getValue() / (double) assembly.size()));
            for (int grain = 0; grain < share; grain++) grains.add(color);
        }
        java.util.Collections.shuffle(grains, new java.util.Random(assembly.id().hashCode()));
        double[] xs = new double[4];
        double[] ys = new double[4];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index >= grains.size()) break;
                double x = 21 + column * (width - 42) / (columns - 1) + (row % 2 == 0 ? 0 : 4);
                double y = 13 + row * 10;
                SpaceView.grain(g, grains.get(index), x, y, 3.2, index * 0.9, xs, ys);
            }
        }
        HBox box = new HBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);     // le clic va à la carte
        return box;
    }

    /** Le titre d'un lieu de rassemblement : « Gaz ». */
    public static String placeTitle(Molecule.State matter) {
        return MatterText.title(matter);
    }

    /** Le lieu d'un état, dans une phrase : « le lieu des gaz ». */
    private static String placeName(Molecule.State matter) {
        return switch (matter) {
            case GAS -> "le lieu des gaz";
            case LIQUID -> "le lieu des liquides";
            case SOLID -> "le lieu des solides";
            case CRYSTAL -> "le lieu des cristaux";
            case METAL -> "le lieu des métaux";
        };
    }

    /** Ce qu'ajoute une sorte rassemblée dans un état quand elle compte pour {@code counted} : « +15 % de particules et +6 % d'atomes ». */
    private static String generation(Molecule.State matter, double counted) {
        String particles = matter.particles() > 0 ? "+" + ElementText.percent(matter.particles() * counted) + " de particules" : "";
        String atoms = matter.atoms() > 0 ? "+" + ElementText.percent(matter.atoms() * counted) + " d'atomes" : "";
        return particles.isEmpty() ? atoms : atoms.isEmpty() ? particles : particles + " et " + atoms;
    }

    private static String capitalized(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
