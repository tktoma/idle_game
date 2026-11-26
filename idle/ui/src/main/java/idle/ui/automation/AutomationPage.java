package idle.ui.automation;

import idle.ui.common.Labels;
import idle.ui.common.ResourceHead;
import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkAutomation;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.ui.GameApp;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.MatterText;
import java.util.List;
import java.util.Map;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Contenu de l'onglet « Automatisation » : tous les automatismes du jeu au même endroit, chacun n'apparaissant qu'une
 * fois débloqué pour la première fois. Ensuite il reste affiché : si une explosion le reverrouille, sa carte dit ce
 * qu'il faut pour le débloquer de nouveau.
 *
 * <p>D'abord les automatismes ordinaires, une ligne chacun, faite de deux {@link Card}. À gauche, l'automatisme : tant
 * qu'il n'est pas acheté, la carte affiche son prix en atomes et sert à l'acheter ; ensuite elle devient un
 * interrupteur pour le mettre en marche ou le couper. À droite, sa cadence : le délai entre deux actions, et le prix
 * pour le réduire en atomes. La cadence achetée en atomes a un maximum ; au-delà, seuls les éléments du tableau
 * périodique réduisent encore le délai. Ils apparaissent une fois assez d'atomes créés ({@link
 * Game#UNLOCK_TOTAL_ATOMS}) ; la synthèse automatique, elle, attend le premier élément unique du tableau périodique.
 *
 * <p>Ensuite les automatismes de matière noire, qui font ce que les ordinaires laissent au joueur : chacun apparaît
 * quand le joueur a gagné assez de matière noire, puis se met en marche ou se coupe d'un clic. Ils ne coûtent rien, et
 * aucun ne refait le travail d'un automatisme ordinaire : le premier se contente de les offrir au début de chaque
 * partie.
 *
 * <p>Puis ceux du Big Bang (l'appui, la création, le rassemblement et la formation automatiques), avec leurs réglages :
 * réserve d'espace, liste de courses, besoins de matière, plafonds par état.
 *
 * <p>Depuis la version 0.147, tout ce qui agit seul s'y retrouve. Les huit automates du collisionneur, une carte
 * chacun, dès que ses améliorations s'ouvrent : pas encore acheté, la carte dit son prix et l'achète si le
 * collisionneur le propose déjà, sinon elle renvoie au collisionneur ; acheté, elle le met en pause ou le relance.
 * Depuis la version 0.167, un bouton coupe ou relance tous les automates d'un geste, et « Couper tous les automatismes
 * » les arrête aussi. Une ligne dit où en est le déclencheur automatique (un croisement sur cinq, sur trois, puis
 * tous), une autre ce que verse le tuyau d'arrosage de la graine. Ces deux-là ne relèvent pas de la coupure générale :
 * le moteur ne les arrête pas quand « Couper tous les automatismes » est actif, et la note de la coupure le dit.
 *
 * <p>Sous chaque groupe, une ligne dit ce qui reste à découvrir, pour que le joueur sache qu'il y a une suite. Chaque
 * section n'apparaît qu'une fois son système ouvert.
 *
 * <p>L'onglet apparaît dès qu'un automatisme est débloqué, et reste là pour de bon une fois la première matière noire
 * obtenue.
 *
 * <p>Depuis la version 0.150, tout cela forme le sous-onglet « Automatismes » ; la bulle du sommet de l'arbre-monde
 * ouvre le second, « Dimensions » ({@link DimensionAutomationPane}), où s'achètent et se coupent les automatismes qui
 * jouent les dimensions et achètent leurs améliorations. Dans la partie secondaire, où les anciens onglets se cachent,
 * l'onglet ne revient qu'avec eux, et ne montre qu'eux.
 *
 * <p>Les cartes s'en tiennent à quelques mots ; les explications, sur les cartes comme entre elles, n'apparaissent
 * qu'en mode détails ({@link Detail}).
 */
public final class AutomationPage extends AutomationPageBase {
    public AutomationPage(Game game) {
        super(game);
    }

    /** Le titre de l'onglet, pour le centre du bandeau. */
    public ResourceHead head() { return head; }

    /** Recopie l'état des automatismes dans les cartes, et n'affiche que ceux qui sont débloqués. */
    void refreshOrdinary() {

        // La coupure générale : le bouton n'existe que s'il y a quelque chose à couper.
        boolean paused = game.isAutomationPaused();
        show(pauseButton, anyToPause(game));
        pauseButton.setText(paused ? "Relancer tous les automatismes" : "Couper tous les automatismes");
        pauseButton.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (paused ? " -fx-text-fill: #1a0a08; -fx-background-color: #ff8a75; -fx-border-color: #ffffff;"
                : " -fx-text-fill: #9be7a8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;"));
        boolean handOnly = game.isHandOnly();
        // Ce que la coupure générale laisse tourner : le moteur n'arrête ni le déclencheur du collisionneur ni le tuyau.
        // Version 0.315 : la note n'est plus sous le bouton, elle passe à son survol.
        boolean uncut = game.colliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH) > 0 || game.hasHose();
        Labels.tip(pauseButton, !((paused || handOnly) && anyToPause(game)) ? "" : handOnly
                ? "Défi de Big Bang « À la main » : les automatismes de matière noire et ceux du Big Bang ne font rien jusqu'au "
                        + "prochain Big Bang. Leurs réglages restent."
                : PAUSE_NOTE + (uncut ? UNCUT_NOTE : ""));
        // Coupés, les automatismes s'estompent : leurs cartes disent toujours leur réglage, pas ce qu'ils font.
        double shade = paused ? 0.45 : 1;
        rows.values().forEach(row -> row.setOpacity(shade));
        colliderGrid.setOpacity(shade);
        darkGrid.setOpacity(shade);
        spaceGrid.setOpacity(shade);
        upgradeOrder.setOpacity(shade);
        automationOrder.setOpacity(shade);
        spaceReserveRow.setOpacity(shade);

        boolean ordinary = game.isAutomationUnlocked();

        boolean threshold = ordinary && game.isFusionThresholdUnlocked();
        show(thresholdRow, threshold);
        if (threshold) {
            int generators = game.fusionThreshold();
            thresholdLabel.setText("Fusion automatique à " + generators + " générateurs"
                    + Detail.only(" : elle attend d'en avoir autant, et rapporte "
                            + generators / game.generatorsPerAtom() + " fois les atomes, prime de groupe en plus"));
            lessGenerators.setDisable(generators <= game.generatorsPerAtom());
            moreGenerators.setDisable(generators + game.generatorsPerAtom() > game.maxGeneratorCount());
        }

        // La réserve d'atomes : réglable dès que le joueur possède la synthèse automatique.
        boolean reserve = false;
        for (Automation automation : game.automations()) {
            reserve |= automation.kind() == Automation.Kind.SYNTHESIS && game.ownsAutomation(automation.id());
        }
        show(reserveRow, reserve);
        if (reserve) {
            BigNum kept = game.synthesisReserve();
            BigNum needed = game.synthesisCost().add(kept);
            reserveLabel.setText(kept.sign() == 0
                    ? "Réserve de la synthèse : aucune" + Detail.only(". Elle dépense tous les atomes dès qu'elle le peut.")
                    : "Réserve de la synthèse : " + Format.count(kept) + " atomes"
                            + (game.isSynthesisReserveBlocking() ? ", au-dessus du plafond : baissez-la" : "")
                            + Detail.only(". Elle ne synthétise qu'à partir de " + Format.count(needed)
                                    + " atomes : la réserve reste pour vos achats."));
            lessReserve.setDisable(kept.sign() == 0);
            // Sous le plafond d'atomes, une réserve plus grande que lui ne pourrait jamais être respectée.
            moreReserve.setDisable(!game.isAtomCapLifted()
                    && reserveStep(kept.toDouble(), true) > game.atomCap().toDouble());
        }

        boolean anyShown = false;
        boolean someHidden = false;
        for (Automation automation : game.automations()) {
            boolean available = game.isAutomationAvailable(automation.id());
            // Déjà débloqué une fois : il reste affiché, avec ce qu'il faut pour le débloquer de nouveau.
            boolean known = available || game.wasAutomationEverAvailable(automation.id());
            show(rows.get(automation), known);
            if (!known) {
                someHidden = true;
                continue;
            }
            anyShown = true;
            if (available) {
                refreshCard(automation);
                refreshCadence(automation);
                refreshBatch(automation);
            } else {
                showLocked(automation);
            }
        }
        show(ordinaryHint, anyShown && Detail.shown());
        // Ce que le joueur n'a encore jamais débloqué reste caché ; une ligne annonce la suite.
        ordinaryNext.setText(!ordinary && !anyShown
                ? "Les automatismes ordinaires se débloquent à " + Format.count(Game.UNLOCK_TOTAL_ATOMS)
                        + " atomes créés (vous : " + Format.count(game.state().totalAtoms()) + ")."
                : "Un autre automatisme apparaîtra avec votre premier élément unique ★ du tableau périodique.");
        show(ordinaryNext, someHidden);

        // Les automatismes de matière noire : seulement ceux que la matière noire gagnée a débloqués.
        boolean anyDark = false;
        DarkAutomation next = null;
        boolean ordersUpgrades = false;
        boolean ordersAutomations = false;
        for (DarkAutomation automation : game.darkAutomations()) {
            Card card = darkCards.get(automation);
            boolean unlocked = game.isDarkAutomationUnlocked(automation.id());
            darkGrid.show(card, unlocked);
            ordersUpgrades |= unlocked && automation.kind() == DarkAutomation.Kind.ATOM_UPGRADES;
            ordersAutomations |= unlocked && automation.kind() == DarkAutomation.Kind.AUTOMATIONS;
            if (!unlocked) {
                if (next == null) next = automation;
                continue;
            }
            anyDark = true;
            boolean on = game.isDarkAutomationEnabled(automation.id());
            boolean suspended = false;   // depuis la 0.047, même l'automatisme qui offre les automatismes agit pendant les défis
            card.show(on && !suspended ? Card.State.ON : Card.State.OFF, "",
                    suspended ? "suspendu" : on ? "en marche" : "coupé",
                    automation.name(), brief(automation),
                    describe(automation) + "." + (suspended ? " Suspendu pendant le défi." : ""),
                    "", "toutes les " + seconds(game.darkAutomationInterval(automation.id())));
        }
        show(darkTitle, anyDark);
        show(darkHint, anyDark && Detail.shown());
        show(darkGrid, anyDark);
        show(upgradeOrder, ordersUpgrades);
        if (ordersUpgrades) upgradeOrder.refresh();
        show(automationOrder, ordersAutomations);
        if (ordersAutomations) automationOrder.refresh();
        // L'appui automatique, une fois acquis avec l'espace : il attend la matière noire s'il n'y en a pas encore.
        boolean hold = game.isAutoHoldUnlocked();
        boolean creation = game.isMoleculeAutomationUnlocked();
        boolean gather = game.isAutoGatherUnlocked();
        boolean form = game.isAutoFormUnlocked();
        int spaceCount = (hold ? 1 : 0) + (creation ? 1 : 0) + (gather ? 1 : 0) + (form ? 1 : 0);
        show(spaceTitle, spaceCount > 0);
        show(spaceGrid, spaceCount > 0);
        spaceTitle.setText(spaceCount > 1 ? "Automatismes du Big Bang" : "Automatisme du Big Bang");
        spaceGrid.show(holdCard, hold);
        spaceGrid.show(moleculesCard, creation);
        spaceGrid.show(gatherCard, gather);
        spaceGrid.show(formCard, form);
        show(spaceReserveRow, creation);
        boolean caps = game.isStateCapUnlocked();
        show(capsBox, caps);
        if (caps) {
            // Version 0.315 : la règle n'est plus au-dessus des listes, elle passe à leur survol.
            String rule = "Plafond par état : la création automatique ne donne pas plus de cette part de l'espace créé "
                    + "aux molécules d'un état. La création à la main n'en tient pas compte. Entre parenthèses, "
                    + "la part qu'elles occupent maintenant.";
            for (Molecule.State matter : Molecule.State.values()) {
                ComboBox<String> choice = capChoices.get(matter);
                Labels.tip(choice, rule);
                Labels.tip(capNames.get(matter), rule);
                String wanted = choiceOf(game.stateCap(matter));
                if (!wanted.equals(choice.getValue())) choice.setValue(wanted);
                capNames.get(matter).setText(MatterText.plural(matter) + " ("
                        + ElementText.percent(game.stateShare(matter)) + ")");
            }
        }
        show(entrustedBox, creation && game.isStatesUnlocked() && !game.state().automatedMolecules().isEmpty());
        entrustedBox.setOpacity(shade);
        if (creation && game.isStatesUnlocked()) refreshEntrusted();
        if (creation) {
            double share = game.autoMoleculeReserve();
            spaceReserveLabel.setText(share <= 0 ? "Réserve d'espace de la création automatique : aucune"
                            + Detail.only(". Elle prend tout l'espace libre dès qu'elle le peut.")
                    : "Réserve d'espace de la création automatique : " + ElementText.percent(share)
                            + Detail.only(". Elle ne crée que si " + ElementText.percent(share) + " de tout l'espace créé reste "
                                    + "libre ensuite : cette part attend vos rassemblements et vos créations à la main."));
            lessSpace.setDisable(share <= 0);
            moreSpace.setDisable(share >= SPACE_RESERVES[SPACE_RESERVES.length - 1] - 1e-9);
        }
        if (gather) {
            boolean on = game.isAutoGatherEnabled();
            gatherCard.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    "Rassemblement automatique", "Rassemble chaque sorte dès qu'elle est prête",
                    "Dès qu'une sorte compte " + Game.SUBSTANCE_MOLECULES + " molécules et que l'espace libre suffit, elle "
                            + "se rassemble d'elle-même, au même prix qu'à la main. Acquis avec l'espace, il traverse les "
                            + "explosions et les Big Bangs. Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", "toutes les " + seconds(Game.AUTO_FORM_SECONDS));
        }
        if (form) {
            boolean on = game.isAutoFormEnabled();
            formCard.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    "Formation automatique", "Forme les assemblages et les astres prêts",
                    "Tout assemblage et tout astre dont les conditions sont réunies se forme de lui-même, et une "
                            + "notification l'annonce. La galaxie, l'amas de galaxies et l'univers restent à vous. Acquise "
                            + "avec l'espace, elle traverse les explosions et les Big Bangs. Un clic "
                            + (on ? "la coupe." : "la remet en marche."),
                    "", "toutes les " + seconds(Game.AUTO_FORM_SECONDS));
        }
        if (creation) {
            boolean on = game.isMoleculeAutomationEnabled();
            int chosen = game.automatedMolecules();
            // Rien à faire en ce moment : la liste est vide ou prête, rien ne manque par état, aucun amas confié.
            boolean idle = game.shoppingList().items().isEmpty() && game.autoMatterTargets().isEmpty()
                    && game.autoSortTargets().isEmpty() && chosen == 0;
            moleculesCard.show(on ? Card.State.ON : Card.State.OFF, "", on && idle ? "en attente" : on ? "en marche" : "coupé",
                    "Création automatique", "Suit la liste de courses, puis la matière et la variété qui manquent, puis "
                            + (chosen == 0 ? "les amas confiés (aucun)" : chosen > 1 ? "les " + chosen + " amas confiés" : "l'amas confié"),
                    "À chaque passage, dans l'ordre : une création de chaque sorte que demande le prochain pas (la liste de "
                            + "courses), puis une dans la sorte la moins encombrante de chaque état où la matière rassemblée ou "
                            + "le nombre de sortes manque encore, puis une dans chaque amas que vous lui avez confié. Au même prix "
                            + "qu'à la main : la formule, prise dans le tableau périodique, où il reste toujours un exemplaire de "
                            + "chaque élément. Quand l'espace manque pour tous, le premier amas servi change à chaque passage. "
                            + "Les amas se confient d'un clic sur leur carte, ou tous d'un coup, dans la sous-page États de la "
                            + "matière du Big Bang, et se retirent ici. Sa réserve d'espace et ses plafonds par état se règlent "
                            + "sous ces cartes. Acquis au deuxième Big Bang, il traverse les explosions et les Big Bangs."
                            + (on && idle ? " Rien à créer en ce moment : il attend." : "")
                            + " Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", "toutes les " + seconds(game.moleculeAutomationInterval()));
        }
        if (hold) {
            boolean on = game.isAutoHoldEnabled();
            boolean waiting = on && !game.isAutoHolding();
            holdCard.show(on ? Card.State.ON : Card.State.OFF, "", waiting ? "en attente" : on ? "en marche" : "coupé",
                    "Appui automatique", "Tient l'appui sur la matière noire",
                    "La matière noire grossit comme si vous teniez le clic sur son point, à pleine vitesse d'appui et "
                            + "depuis n'importe quel onglet ; avec le verrou de l'arbre, il fournit ce que le verrou laisse. "
                            + "Acquis avec l'espace, il traverse les explosions et les Big Bangs."
                            + (waiting ? " Il attend la première explosion : sans matière noire, rien à faire grossir." : "")
                            + " Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", waiting ? "après la première explosion" : "");
        }
        refreshCollider();
        // La suite n'est annoncée qu'à qui connaît déjà la matière noire.
        boolean announce = next != null && game.isDarkMatterUnlocked();
        show(darkNext, announce);
        if (announce) {
            darkNext.setText((anyDark ? "Prochain automatisme de matière noire à "
                    : "Un automatisme de matière noire apparaîtra à ")
                    + Format.count(next.darkMatter()) + " matières noires gagnées (vous : "
                    + Format.darkMatter(game.darkMatterEarned()) + ").");
        }
    }

    /**
     * Les amas confiés à la création automatique : une pastille par amas, dans l'ordre où ils ont été
     * confiés, qui le retire d'un clic. Les pastilles d'amas retirés disparaissent ; celles des autres restent en place.
     */
    void refreshEntrusted() {
        List<String> chosen = game.state().automatedMolecules();
        for (String id : chosen) {
            if (entrustedChips.containsKey(id) || !known(id)) continue;      // une sorte d'une autre version : ignorée
            Molecule molecule = game.molecule(id);
            Button chip = new Button(molecule.name() + " " + molecule.formula() + "  ×");
            chip.setFocusTraversable(false);
            chip.setStyle("-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 12; -fx-border-radius: 12;"
                    + " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: "
                    + GameApp.BIG_BANG_COLOR + "66;");
            chip.setOnAction(event -> {
                game.setMoleculeAutomated(id, false);
                refresh();
            });
            chip.setTooltip(Labels.tip("Amas confié à la création automatique : un clic le lui retire. La liste de courses, "
                    + "la matière et la variété qui manquent passent avant lui. Un clic sur un amas, dans les États de la "
                    + "matière du Big Bang, le confie."));
            entrustedChips.put(id, chip);
            entrustedRow.getChildren().add(chip);
        }
        entrustedChips.forEach((id, chip) -> show(chip, chosen.contains(id)));
    }

    /** Vrai si le catalogue connaît cette sorte de molécule. */
    boolean known(String moleculeId) {
        for (Molecule molecule : game.molecules()) {
            if (molecule.id().equals(moleculeId)) return true;
        }
        return false;
    }

    /**
     * Les automates du collisionneur, son déclencheur automatique et le tuyau d'arrosage : chacun n'apparaît qu'une fois
     * son système ouvert. Depuis la version 0.167, la coupure générale arrête et estompe les automates, et un bouton les
     * coupe ou les relance tous ; elle laisse le déclencheur et le tuyau.
     */
    void refreshCollider() {
        boolean open = game.isColliderUpgradesUnlocked();
        show(colliderTitle, open);
        show(colliderGrid, open);
        if (open) {
            for (Map.Entry<ColliderUpgrade, Card> entry : colliderCards.entrySet()) {
                ColliderUpgrade upgrade = entry.getKey();
                Card card = entry.getValue();
                if (game.colliderUpgradeLevel(upgrade) > 0) {
                    // Son propre réglage ; la coupure générale l'arrête sans le changer (la carte s'estompe).
                    boolean on = !game.state().isColliderPaused(upgrade);
                    card.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "en pause", upgrade.label(),
                            upgrade.brief(), upgrade.effect() + ". Un clic " + (on ? "le met en pause." : "le relance."), "", "");
                } else if (game.isColliderUpgradeOpen(upgrade)) {
                    card.show(game.canBuyColliderUpgrade(upgrade) ? Card.State.READY : Card.State.WAITING, "", "", upgrade.label(),
                            upgrade.brief(), upgrade.effect() + ". S'achète une fois, ici ou dans le collisionneur.",
                            energy(game.colliderUpgradeCost(upgrade)) + " énergie", "");
                } else {
                    card.show(Card.State.LOCKED, "", "", upgrade.label(), upgrade.brief(),
                            upgrade.effect() + ". S'ouvre avec « " + upgrade.requires().label() + " ».",
                            "à acheter dans le collisionneur", "");
                }
            }
        }
        boolean anyAutomate = open && ownsColliderAutomate(game);
        show(colliderPauseButton, anyAutomate);
        if (anyAutomate) {
            boolean anyPaused = game.anyColliderAutomationPaused();
            colliderPauseButton.setText(anyPaused ? "Relancer les automates du collisionneur" : "Couper les automates du collisionneur");
            colliderPauseButton.setStyle("-fx-font-size: 13px; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 6;"
                    + " -fx-border-radius: 6; -fx-text-fill: " + ColliderPane.COLOR + "; -fx-background-color: #16202e;"
                    + " -fx-border-color: " + ColliderPane.COLOR + ";");
        }
        int catchLevel = game.colliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH);
        boolean catching = open && (catchLevel > 0 || game.isColliderUpgradeOpen(ColliderUpgrade.AUTO_CATCH));
        catchText = "";
        if (catching) {
            int max = ColliderUpgrade.AUTO_CATCH.maxLevel();
            String next = energy(game.colliderUpgradeCost(ColliderUpgrade.AUTO_CATCH)) + " énergie";
            catchText = (catchLevel == 0
                    ? "Déclencheur automatique : pas encore acheté. Il s'achète dans le collisionneur, avec les faisceaux : " + next + "."
                    : "Déclencheur automatique, niveau " + catchLevel + "/" + max + " : il capte " + catchShare(catchLevel)
                            + (catchLevel < max ? ". Niveau suivant dans le collisionneur : " + next : "") + "."
                            + " Il capte comme un clic au bon moment. Il ne se met pas en pause, et la coupure "
                                    + "générale ne l'arrête pas.");
        }
        boolean hose = game.hasHose();
        hoseText = "";
        if (hose) {
            double rate = game.hoseRate();
            hoseText = ("Tuyau d'arrosage de la graine : il verse seul " + ElementText.number(rate)
                    + (rate >= 2 ? " arrosages" : " arrosage") + " par seconde, " + ElementText.percent(game.hoseShare())
                    + " de ce que verse l'arrosoir."
                    + " Il arrose tant que la graine peut l'être, sans pause : la coupure générale ne l'arrête pas. "
                            + "Ses niveaux s'achètent avec les améliorations de la graine, dans l'onglet Univers.");
        }
        // Version 0.315 : l'énergie, la règle des automates, le déclencheur et le tuyau ne sont plus des lignes de la
        // page ; ils passent au survol du titre de la section.
        Labels.tip(colliderTitle, !open ? "" : "Énergie du collisionneur : " + energy(game.colliderEnergy())
                + ". Chaque automate achète tout seul les exemplaires de son générateur ; un clic sur la carte le met en "
                + "pause ou le relance, le bouton les coupe tous, et la coupure générale les arrête aussi. Ils s'achètent "
                + "une fois, ici ou dans le collisionneur, chacun après le précédent. Comme tout le collisionneur, ils "
                + "traversent l'explosion et le Big Bang, et leurs pauses aussi."
                + (catchText.isEmpty() ? "" : "\n\n" + catchText) + (hoseText.isEmpty() ? "" : "\n\n" + hoseText));
    }

    /** La carte d'un automate du collisionneur : pour les vérifications. */
    public Card colliderCard(ColliderUpgrade upgrade) { return colliderCards.get(upgrade); }

    /** Le texte du déclencheur automatique (au survol du titre des automates), vide s'il n'y en a pas : pour les vérifications. */
    public String catchText() { return catchText; }

    /** Le texte du tuyau d'arrosage (au survol du titre des automates), vide s'il n'y en a pas : pour les vérifications. */
    public String hoseText() { return hoseText; }

    /** La note de la coupure, au survol du bouton, vide s'il n'y en a pas : pour les vérifications. */
    public String pauseNoteText() { return pauseButton.getTooltip() == null ? "" : pauseButton.getTooltip().getText(); }

    /** Ce que fait un automatisme ordinaire, en quelques mots : la ligne de sa carte. */
    String brief(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> game.buysInBulk(automation) ? "Achète tous ceux qu'il peut payer" : "Achète dès que possible";
            case FUSION -> "Fusionne dès que possible";
            case SYNTHESIS -> "Synthétise dès que possible";
        };
    }

    /** Ce que fait un automatisme ordinaire, en une phrase : pour le mode détails. */
    String describe(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> game.buysInBulk(automation)
                    ? "À chaque action, achète d'un coup tous les générateurs que les particules permettent."
                    : game.automationBatch(automation.id()) > 1
                    ? "Achète « " + automation.name() + " » dès qu'il y a assez de particules, jusqu'à "
                            + game.automationBatch(automation.id()) + " niveaux par action."
                    : "Achète « " + automation.name() + " » dès qu'il y a assez de particules, un niveau par action.";
            case FUSION -> "Fusionne dès que tous les générateurs sont débloqués.";
            case SYNTHESIS -> "Synthétise un élément dès qu'il y a assez d'atomes, réserve déduite.";
        };
    }

    /** Un automatisme déjà connu mais reverrouillé par une explosion : sa carte dit comment le débloquer. */
    void showLocked(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        cards.get(automation).show(Card.State.LOCKED, "", "verrouillé", automation.name(), brief(automation),
                describe(automation) + " " + condition(automation) + ".",
                !game.isAutomationUnlocked() ? "À " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés"
                        : needsUnique ? "Avec un élément unique ★" : "", "");
        cadences.get(automation).show(Card.State.LOCKED, "", "", "Cadence", "", "", "", "");
        Card batch = batches.get(automation);
        if (batch != null) show(batch, false);
    }

    /**
     * L'achat en lot : caché avant la matière noire ; puis le lot actuel, et le prix du suivant en matière
     * noire, jusqu'à 64.
     */
    void refreshBatch(Automation automation) {
        Card batch = batches.get(automation);
        if (batch == null) return;
        String id = automation.id();
        boolean open = game.isAutomationBatchAvailable(id);
        show(batch, open);
        if (!open) return;
        int level = game.automationBatchLevel(id);
        String corner = level + "/" + Game.AUTOMATION_BATCH_LEVELS;
        String now = game.isAutomationInstant() && game.buysInBulk(automation) ? "Tout d'un coup (bulle du bonsaï)"
                : "Par " + game.automationBatch(id) + " à chaque action";
        String detail = "Ce qu'une action achète au plus : 1, puis deux fois plus à chaque niveau, jusqu'à 64. Il se paie en "
                + "matière noire et tient tout l'univers, explosions et Big Bangs compris.";
        if (game.isAutomationBatchMaxed(id)) {
            batch.show(Card.State.DONE, corner, "max", now, "Lot le plus grand", detail, "", "");
        } else {
            BigNum cost = game.automationBatchCost(id);
            batch.show(game.canBuyAutomationBatch(id) ? Card.State.READY : level > 0 ? Card.State.STARTED : Card.State.WAITING,
                    corner, "", now, "Niveau suivant : par " + (game.automationBatch(id) * 2), detail,
                    Format.darkMatter(cost) + " matière noire", "");
        }
    }

    /** Ce qu'il manque pour qu'un automatisme ordinaire soit de nouveau disponible. */
    String condition(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        if (!game.isAutomationUnlocked()) {
            return "Se débloque à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés (vous : "
                    + Format.count(game.state().totalAtoms()) + ")"
                    + (needsUnique ? ", avec un élément unique ★" : "");
        }
        return "Se débloque avec un élément unique ★ du tableau périodique"
                + (isGranting() ? ", ou sera offert une fois les autres automatismes à leur cadence maximale" : "");
    }

    /** Vrai si l'automatisme de matière noire qui offre les automatismes ordinaires est en marche. */
    boolean isGranting() {
        for (DarkAutomation automation : game.darkAutomations()) {
            if (automation.kind() == DarkAutomation.Kind.GRANT_AUTOMATIONS && game.isDarkAutomationEnabled(automation.id())) {
                return true;
            }
        }
        return false;
    }

    void refreshCard(Automation automation) {
        Card card = cards.get(automation);
        String id = automation.id();
        if (!game.ownsAutomation(id)) {
            card.show(game.canBuyAutomation(id) ? Card.State.READY : Card.State.WAITING, "", "",
                    automation.name(), brief(automation),
                    describe(automation) + " S'achète une fois, puis s'active ou se coupe d'un clic.",
                    Format.count(automation.cost()) + (automation.cost().gt(BigNum.ONE) ? " atomes" : " atome"),
                    remaining(automation.cost()));
        } else {
            boolean on = game.isAutomationEnabled(id);
            card.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    automation.name(), brief(automation),
                    describe(automation) + " Un clic " + (on ? "le coupe." : "le remet en marche."), "", "");
        }
    }

    void refreshCadence(Automation automation) {
        Card cadence = cadences.get(automation);
        String id = automation.id();
        int speedLevel = game.automationSpeedLevel(id);
        String level = speedLevel + "/" + automation.maxSpeedLevel();
        // La bulle du bonsaï, à droite : plus de délai du tout.
        String delay = game.isAutomationInstant() ? "Sans délai (bulle du bonsaï)" : "Toutes les " + seconds(game.automationInterval(id));
        if (!game.ownsAutomation(id)) {
            // Pas encore acheté : sa cadence se lit, mais ne s'achète pas.
            cadence.show(Card.State.LOCKED, level, "", delay, "Après l'achat de l'automatisme", "", "", "");
        } else if (game.isAutomationMaxed(id)) {
            boolean floor = game.automationInterval(id) <= game.minAutomationInterval();
            cadence.show(Card.State.DONE, level, "max", delay, floor ? "Délai le plus court possible" : "Cadence maximale",
                    floor ? "" : "Les éléments du tableau périodique réduisent encore ce délai.", "", "");
        } else {
            BigNum cost = game.automationSpeedCost(id);
            double ratio = automation.intervalAt(speedLevel) / automation.intervalAt(speedLevel + 1);
            cadence.show(game.canSpeedUpAutomation(id) ? Card.State.READY
                            : speedLevel > 0 ? Card.State.STARTED : Card.State.WAITING,
                    level, "", delay, "Niveau suivant : délai ÷" + ElementText.number(ratio),
                    "Le délai entre deux actions. Chaque niveau le réduit, jusqu'au maximum de la cadence ; "
                            + "au-delà, seuls les éléments l'accélèrent.",
                    Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome"), remaining(cost));
        }
    }

    /** Ce qu'il reste à attendre avant de pouvoir payer {@code cost} atomes : un nombre de fusions, ou rien. */
    String remaining(BigNum cost) {
        long fusions = game.fusionsUntilAtoms(cost);
        if (fusions <= 0) return "";
        return Format.whole(fusions) + (fusions > 1 ? " fusions" : " fusion");
    }
}
