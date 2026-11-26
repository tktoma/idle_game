package idle.ui;

import idle.core.Game;
import idle.core.sky.Cosmos;
import idle.core.universe.*;
import idle.ui.hadrons.HadronsPage;
import idle.ui.universe.UniversePage;
import idle.ui.vacuum.VoidPage;
import idle.core.bigbang.BangChallenge;
import idle.core.darkmatter.Challenge;
import idle.core.math.BigNum;
import idle.core.upgrades.*;
import idle.ui.automation.AutomationPage;
import idle.ui.collider.ColliderPage;
import idle.ui.common.*;
import idle.ui.darkmatter.ChallengesPane;
import java.util.List;
import java.util.Locale;
import javafx.scene.input.KeyCode;

/**
 * La fenêtre du jeu : les générateurs animés, le compteur de particules, la production, et une carte par amélioration.
 *
 * <p>Une partie neuve s'ouvre sur un écran de démarrage : un fond de poussières et un bouton qui crée le premier
 * générateur. Quand tous les générateurs sont débloqués, un bouton permet de les fusionner en un atome.
 *
 * <p>En haut de la fenêtre, des onglets. Quatre sont là dès le début : « Particules » (les générateurs et leurs
 * améliorations), « Succès » ({@link AchievementsPage}), « Statistiques » ({@link StatsPage}) et « Réglages » ({@link
 * SettingsPage}) ; « Profil » ({@link idle.ui.pages.ProfilePage}, version 0.147) les rejoint au premier générateur. Les
 * autres apparaissent avec la partie : « Atomes » ({@link AtomsPage}) au premier atome, « Automatisation » ({@link
 * AutomationPage}) dès qu'un automatisme est débloqué. Le jeu continue de tourner quel que soit l'onglet affiché, sauf
 * en pause.
 *
 * <p>Quand les 118 éléments du tableau périodique sont découverts, une pastille violette s'allume sous le bandeau
 * ({@link StepDots}, une barre violette au-dessus des onglets jusqu'à la 0.308) : un clic déclenche une explosion qui remet toute la partie à zéro et fait apparaître l'onglet « Matière noire » ({@link
 * DarkMatterPage}). Au bout de son arbre, le Big Bang efface tout de nouveau et fait apparaître l'onglet « Big Bang »
 * ({@link BigBangPage}).
 *
 * <p>Lancé avec l'argument {@code --test} (tâche Gradle {@code runTest}), le jeu affiche en plus le panneau de test
 * ({@link TestPanel}, à droite, {@value TestPanel#KEY_NAME}) et une bande en bas de chaque fenêtre de dimension
 * ({@link DimensionTestBar}) : la vitesse du jeu, l'avance rapide, les bots, les sons, les animations, les paliers.
 *
 * <p>Les améliorations payées en particules s'achètent par un, par dix ou « au maximum » (réglage en haut à droite, sous l'aide), au
 * clic ou au clavier : voir {@link #onKey(KeyCode)}. Chacune est une {@link Card} : son niveau, sa touche, ce qu'elle
 * rapporte en quelques mots, son prix et le temps qu'il reste à attendre.
 *
 * <p>Partout dans le jeu, les explications complètes n'apparaissent qu'en mode détails ({@link Detail}) : tant que le
 * joueur tient la touche de détail, réglable ({@link Settings#detailKey()}). Un rappel de cette touche reste affiché
 * sous les onglets.
 *
 * <p>Sous les onglets, une ligne rappelle l'objectif du moment ({@link Goals}). Par-dessus tout le reste, les
 * notifications ({@link ToastPane}) annoncent ce que {@link Notifier} a repéré : nouvel élément, succès, déblocage.
 * Toute la fenêtre passe par {@link ScaledPane} (la taille de l'interface) et par le filtre du thème choisi ({@link
 * ThemeFilter}). Des thèmes et des notations se débloquent en faisant des choses très précises ({@link Cosmetics}).
 *
 * <p>La partie est sauvegardée dans un fichier ({@link SaveStore}) : relue au lancement, écrite au rythme choisi dans
 * les réglages (toutes les trente secondes d'origine) et à la fermeture. Au retour du joueur, une part du temps passé
 * jeu fermé est rejouée ({@link Absence}), sous un panneau qui en montre l'avancée puis le bilan ({@link AbsencePane}).
 * La fenêtre retrouve aussi sa place, sa taille et son onglet.
 *
 * <p>La fenêtre ne contient aucune règle : elle appelle {@code game.tick()}, affiche l'état, et transmet les clics à
 * {@code game.buy()}.
 *
 * <p>Le code est rangé en parties : une chaîne de classes abstraites, une par thème, dont celle-ci est le bout ; le
 * reste du programme ne voit que {@code GameApp}. De la base au bout : {@link GameAppBase}, {@link GameAppWindow},
 * {@link GameAppTabs}. Quand une partie a besoin d'une méthode définie plus loin dans la chaîne, elle la déclare
 * abstraite en tête, sous « Ce que fournissent les parties suivantes ».
 */
public final class GameApp extends GameAppTabs {
    /**
     * Les skins de la fenêtre autour du jeu. Les notifications prennent leur skin à l'affichage et le jeu
     * apprend combien ont été affichées ; les panneaux (aide, absence) suivent le visuel des panneaux, l'écran de fin et
     * la fin de partie aussi (leurs skins à eux sont retirés depuis la 0.267) ; le guide de transition le sien ;
     * le curseur se pose sur toute la fenêtre, l'arrosoir et la loupe seulement sur leur onglet.
     */
    private void refreshWindowSkins() {
        toasts.setSkin(game.chosenSkin(SkinVisual.TOAST));
        game.noteNotificationsShown(toasts.shown());
        Skin panel = game.chosenSkin(SkinVisual.PANEL);
        helpPane.setSkin(panel);
        absencePane.setSkin(panel);
        endPane.setSkin(panel);
        finalePane.setSkin(panel);
        transitionPane.setSkin(game.chosenSkin(SkinVisual.TRANSITION));
        Skin cursor = CursorSkinArt.shownOn(game.chosenSkin(SkinVisual.CURSOR), selectedTab);
        if (cursor != cursorSkin) {
            cursorSkin = cursor;
            if (windowPane != null) windowPane.setCursor(CursorSkinArt.cursor(cursor));
            cursorTrail.setActive(cursor == Skin.CURSOR_SHOOTING_STAR);
        }
    }

    /** Recopie l'état du jeu dans les composants. */
    void refresh() {
        // Version 0.318 : ce que la partie montre est retenu, pour ne rien dévoiler ailleurs de ce qui n'a jamais été vu.
        settings.seen().note(game);
        // Fenêtre étroite : les onglets se resserrent pour tenir sur une ligne.
        // Avec l'onglet « Big Bang » ils sont au moins huit (douze au plus) : la fenêtre de 800 px ne suffit plus aux noms entiers.
        boolean bigBang = game.isBigBangUnlocked();
        // En colonne, les onglets ont toute leur largeur : pas de noms courts.
        boolean nowNarrow = !tabsLeftApplied && root.getWidth() > 0 && root.getWidth() < (bigBang ? NARROW_WINDOW_WITH_BIG_BANG : NARROW_WINDOW);
        if (nowNarrow != narrow) {
            narrow = nowNarrow;
            styleTabs();
        }
        // Le chrome : les skins du fond, des onglets et des cartes, appliqués quand le choix change.
        Skin ambient = game.chosenSkin(SkinVisual.AMBIENT);
        if (ambient != chrome.skin()) {
            chrome.setSkin(ambient);
            root.setStyle(Chrome.rootStyle(ambient));
            root.setPadding(Chrome.rootPadding(ambient));
        }
        Skin tabs = game.chosenSkin(SkinVisual.TABS);
        if (tabs != tabSkin) {
            tabSkin = tabs;
            styleTabs();
        }
        Card.setSkin(game.chosenSkin(SkinVisual.CARD));
        // Les icônes d'onglet, les signes, les info-bulles, le fond des particules et leur fusion.
        TabIcon.setSkin(game.chosenSkin(SkinVisual.TAB_ICONS));
        Card.setSignSkin(game.chosenSkin(SkinVisual.CARD_SIGNS));
        MatterText.setSkin(game.chosenSkin(SkinVisual.MATTER_SIGNS));
        Breakdown.setSkin(game.chosenSkin(SkinVisual.TOOLTIP));
        generators.setDustSkin(game.chosenSkin(SkinVisual.AMBIENT_DUST));
        generators.setFusionSkin(game.chosenSkin(SkinVisual.GENERATOR_FUSION));
        refreshWindowSkins();
        if (testPanel != null) {
            testPanel.refresh();
            dimensionTestBar.refresh();
        }

        // Le bandeau : au centre et sous l'aide, ce que l'onglet affiché y met. Les onglets y passent un par un (0.309) :
        // pour l'instant, les particules, leur débit et la quantité par achat.
        boolean particlesHeader = selectedTab == Tab.PARTICLES && game.isStarted();
        header.setCenter(particlesHeader ? particlesHead : headOf(selectedTab));
        // Version 0.312 : la carte des dimensions seule, sans le bandeau, ses objectifs, ses pastilles ni le chrono.
        boolean bare = selectedTab == Tab.UNIVERSE && universePage.bare();
        if (header.isVisible() == bare) {
            for (javafx.scene.Node node : new javafx.scene.Node[] {header, steps}) {
                node.setVisible(!bare);
                node.setManaged(!bare);
            }
        }
        header.setExtra(particlesHeader ? amountRow : null);
        // L'onglet « Particules » : ses compteurs, ses cartes et sa fusion ne sont recopiés que lorsqu'il est affiché.
        if (selectedTab == Tab.PARTICLES) {
            BigNum particles = game.state().particles();

            // La production se calcule une fois pour les trois textes (toute la formule à chaque appel).
            BigNum production = game.productionPerSecond();
            double rate = Math.min(production.toDouble(), 1e9);
            String perSecond = rate <= 0 ? "Aucun générateur" : rate < 1
                    ? String.format(Locale.ROOT, "1 particule toutes les %.1f s", 1 / rate)
                    : "+" + Format.big(production) + " /s";
            int hidden = game.generatorCount() - MAX_VISIBLE_GENERATORS;
            particlesHead.set(Format.count(particles), perSecond + "   |   " + Format.perMinute(production) + " p/m"
                    + (hidden > 0 ? "   |   " + game.generatorCount() + " générateurs (" + hidden + " non dessinés)" : ""));
            productionBreakdown.refresh();
            refreshSecondUniverse();

            upgradeCards.forEach(this::show);
            amountRow.paint(settings.buyAmount(), PARTICLES_COLOR);
            // Les autres raccourcis, ceux qui ne sont écrits sur aucune carte : en mode détails seulement.
            boolean keys = settings.shortcuts() && Detail.shown();
            if (keys) shortcutsLabel.setText(shortcutsHint());   // caché, son texte n'a pas à suivre les touches
            shortcutsLabel.setVisible(keys);
            shortcutsLabel.setManaged(keys);

            // Au plafond d'atomes, la carte reste affichée mais éteinte, pour expliquer pourquoi rien ne se passe.
            boolean capped = game.isAtomCapReached();
            boolean table = game.isPeriodicTableUnlocked() && !game.isPeriodicTableComplete();
            BigNum gain = game.atomsPerFusion();
            fuseCard.show(capped ? Card.State.WAITING : Card.State.READY, String.valueOf(game.generatorCount()),
                    settings.shortcuts() && settings.key(Shortcuts.Action.FUSE) != null ? Shortcuts.name(settings.key(Shortcuts.Action.FUSE)) : "",
                    "Fusion des générateurs",
                    capped ? "Maximum de " + Format.count(game.atomCap()) + " atomes atteint"
                            : "+" + Format.amount(gain) + (gain.gt(BigNum.ONE) ? " atomes" : " atome"),
                    capped ? "Dépensez des atomes" + (table ? " ou synthétisez un élément" : "") + " pour fusionner de nouveau."
                            : "Les générateurs fusionnent : il n'en reste qu'un, et les particules repartent de zéro. "
                                    + (game.fusionKeepShare() >= 1
                                            ? "Les améliorations payées en particules sont gardées (Persistance)."
                                            : game.keepsUpgradesOnFusion()
                                            ? "Les améliorations payées en particules gardent " + Math.round(game.fusionKeepShare() * 100)
                                                    + " % de leurs niveaux (Persistance)."
                                            : "Les améliorations payées en particules aussi."),
                    "", "");
            // Version 0.306 : le bouton reste là pendant l'animation ; une fusion demandée alors se fait tout de suite.
            fuseCard.setVisible(game.hasAllGenerators());
        }

        // Un atome de plus qu'à l'image précédente, par fusion manuelle ou automatique.
        double totalAtoms = game.state().totalAtoms().toDouble();
        if (lastTotalAtoms >= 0 && totalAtoms > lastTotalAtoms) {
            celebrateNewAtom();
            // Le son de l'atome créé, là où on le voit : le grand atome de l'onglet Atomes.
            if (selectedTab == Tab.ATOMS && atomsPage.selected() == 0 && settings.sound()) sound.click(Sound.Cue.NEW_ATOM, settings.volume());
        }
        lastTotalAtoms = totalAtoms;

        // Après une explosion, les atomes ont disparu mais l'onglet « Matière noire » reste.
        // L'onglet « Atomes » n'existe qu'à partir du premier atome.
        // La partie secondaire : une fois l'univers formé, tous les anciens onglets disparaissent,
        // particules et collisionneur compris ; restent l'univers et les pages pour s'y retrouver.
        // Le profil test UI (0.248) : tous les onglets ouverts, et la vue reste où le panneau l'a mise.
        boolean everything = uiTestPanel != null;
        boolean phase = universePhase() && !everything;
        particlesTab.setVisible(!phase);
        particlesTab.setManaged(!phase);
        boolean hasAtoms = game.state().totalAtoms().sign() > 0 && !phase || everything;
        boolean darkMatter = game.isDarkMatterUnlocked() && !phase || everything;
        atomsTab.setVisible(hasAtoms);
        atomsTab.setManaged(hasAtoms);
        readiness.frame();
        atomsTab.setText("Atomes (" + Format.count(game.state().atoms()) + ")");
        atomsPage.refresh(selectedTab == Tab.ATOMS);

        // L'onglet « Automatisation » n'existe qu'une fois un automatisme débloqué, ordinaire ou de matière noire. Dans la
        // partie secondaire, il ne revient qu'avec l'automatisation des dimensions, seule montrée alors.
        boolean automation = phase ? game.isDimensionAutomationUnlocked() : AutomationPage.hasContent(game) || everything;
        automationTab.setVisible(automation);
        automationTab.setManaged(automation);
        automationTab.setText((narrow ? "Auto." : "Automatisation") + (game.isAutomationPaused() && AutomationPage.anyToPause(game) ? " (coupée)" : ""));
        automationPage.setOldTabsHidden(phase);
        if (selectedTab == Tab.AUTOMATION) automationPage.refresh();

        // L'onglet « Matière noire » n'existe qu'après la première explosion.
        darkMatterTab.setVisible(darkMatter);
        darkMatterTab.setManaged(darkMatter);
        darkMatterTab.setText((narrow ? "M. noire (" : "Matière noire (") + Format.darkMatter(game.state().darkMatter()) + ")"
                + Readiness.dot(readiness.darkMatter()));
        darkMatterPage.refresh(selectedTab == Tab.DARK_MATTER);

        // L'onglet du collisionneur s'ouvre à la première explosion et ne se referme plus.
        boolean collider = game.isColliderUnlocked() && !phase || everything;
        colliderTab.setVisible(collider);
        colliderTab.setManaged(collider);
        colliderTab.setText((narrow ? "Collis." : "Collisionneur") + Readiness.dot(Readiness.Kind.COLLIDER, ColliderPage.ready(game)));
        if (selectedTab == Tab.COLLIDER) colliderPage.refresh();

        // L'onglet « Big Bang » n'existe qu'après le premier Big Bang. Il vient d'y en avoir un : on y va,
        // comme une explosion mène à l'onglet « Matière noire ».
        if (everything) bigBang = true;
        bigBangTab.setVisible(bigBang && !phase);
        bigBangTab.setManaged(bigBang && !phase);
        bigBangTab.setText((narrow ? "Big Bang" : "Big Bang (" + game.bigBangs() + ")") + Readiness.dot(readiness.bigBang()));
        if (lastBigBangs >= 0 && game.bigBangs() > lastBigBangs && !everything) {
            selectTab(Tab.BIG_BANG);
            // L'animation plein écran du Big Bang : celle de son skin, ou la classique (version 0.246).
            if (settings.effect(Settings.Effect.FLASH)) playBigBang();
        }
        lastBigBangs = game.bigBangs();
        if (selectedTab == Tab.BIG_BANG) bigBangPage.refresh();
        if (selectedTab == Tab.UNIVERSE) universePage.refresh();
        // Le chrono hors de la Graine : partout sauf sur la sous-page Graine, où il est déjà. Version 0.203 :
        // une ligne sous l'objectif, et la même dans l'en-tête de la fenêtre d'une dimension quand elle est ouverte.
        // Version 0.315 : ni sur l'Expansion du Big Bang, toute à sa vue.
        chronoCorner.refresh(selectedTab == Tab.UNIVERSE && universePage.selected() == UniversePage.SEED
                || selectedTab == Tab.BIG_BANG && bigBangPage.hidesChrono());
        dimensionChrono.refresh(!dimensionWindow.isVisible());

        // La fuite : quand elle apparaît parmi les défis, une seule fois, la vue va aux défis et y reste
        // le temps de son arrivée.
        if (game.isLeakChallengeShown() && darkMatter && !everything && game.state().addSeenPopup("reveal:challenge_leak")) {
            selectTab(Tab.DARK_MATTER);
            darkMatterPage.playLeakReveal();
        }
        if (darkMatterPage.isRevealing() && (selectedTab != Tab.DARK_MATTER || darkMatterPage.selected() != 2)) {
            selectTab(Tab.DARK_MATTER);
            darkMatterPage.showChallenges();
        }

        // L'onglet « Univers » s'ouvre quand l'univers est formé. S'il vient de se former,
        // on y va et sa naissance se joue ; l'écran de fin attend qu'elle soit finie.
        boolean universe = game.hasCosmos(Cosmos.UNIVERSE);
        // Pendant la partie de l'univers suivant, l'onglet reste : les univers déjà formés sont là.
        universeTab.setVisible(game.universes() > 0 || everything);
        universeTab.setManaged(game.universes() > 0 || everything);
        if (universe && hadUniverse == 0 && !everything) {
            // Former l'univers a tout remis à zéro : les anciens onglets, cachés, repartent de rien.
            forgetOldPages();
            selectTab(Tab.UNIVERSE);
            universePage.playBirth();
        }
        hadUniverse = universe ? 1 : 0;

        // Le vide : le deuxième acte de la partie secondaire. Depuis la 0.129, il se propose dans le
        // collisionneur au premier Big Bang du troisième univers, et son onglet s'ouvre une fois qu'il est acheté.
        boolean vacuum = game.isVoidUnlocked() && !phase || everything;
        voidTab.setVisible(vacuum);
        voidTab.setManaged(vacuum);
        voidTab.setText("Le vide" + Readiness.dot(Readiness.Kind.VOID, VoidPage.ready(game)));
        if (selectedTab == Tab.VOID) voidPage.refresh();

        // Les hadrons : ils se proposent dans le collisionneur avec la bulle de l'arbuste, au milieu, et
        // leur onglet s'ouvre une fois payé, ou dès le début de l'univers avec la bulle du jeune arbre, à gauche.
        boolean hadrons = game.isHadronsUnlocked() && !phase || everything;
        hadronsTab.setVisible(hadrons);
        hadronsTab.setManaged(hadrons);
        hadronsTab.setText("Hadrons" + Readiness.dot(Readiness.Kind.HADRONS, HadronsPage.ready(game)));
        if (selectedTab == Tab.HADRONS) hadronsPage.refresh();

        // Les succès et les statistiques ne se recalculent que lorsqu'on les regarde. L'onglet des succès
        // n'existe que si le jeu en a.
        boolean achievements = !game.achievements().isEmpty() || everything;
        achievementsTab.setVisible(achievements);
        achievementsTab.setManaged(achievements);
        if (selectedTab == Tab.ACHIEVEMENTS) achievementsPage.refresh();
        if (selectedTab == Tab.STATS) statsPage.refresh();
        // Le profil : dès le premier générateur ; sa pastille dit qu'un titre attend d'être vu.
        boolean profile = game.isStarted() || everything;
        profileTab.setVisible(profile);
        profileTab.setManaged(profile);
        profileTab.setText("Profil" + Readiness.dot(selectedTab != Tab.PROFILE && profilePage.hasNews()));
        if (selectedTab == Tab.PROFILE) profilePage.refresh();
        settingsTab.setText(!settings.paused() ? "Réglages" : narrow ? "En pause" : "Réglages (pause)");
        // Les skins : l'onglet s'ouvre après cinq fusions du deuxième univers, et ne se referme plus.
        boolean skins = game.isSkinsUnlocked() || everything;
        skinsTab.setVisible(skins);
        skinsTab.setManaged(skins);
        skinsTab.setText(narrow ? "Skins" : "Skins (" + game.skinsUnlocked() + ")");
        if (selectedTab == Tab.SKINS) skinsPage.refresh();

        // Un onglet qui vient de disparaître (après une explosion) ne peut pas rester affiché.
        if (((selectedTab == Tab.ATOMS && !hasAtoms) || (selectedTab == Tab.AUTOMATION && !automation)
                || (selectedTab == Tab.DARK_MATTER && !darkMatter) || (selectedTab == Tab.BIG_BANG && (!bigBang || phase))
                || (selectedTab == Tab.UNIVERSE && game.universes() == 0) || (selectedTab == Tab.PARTICLES && phase)
                || (selectedTab == Tab.COLLIDER && !collider) || (selectedTab == Tab.VOID && !vacuum)
                || (selectedTab == Tab.HADRONS && !hadrons) || (selectedTab == Tab.PROFILE && !profile)) && !everything) {
            selectTab(phase ? Tab.UNIVERSE : Tab.PARTICLES);
            // L'onglet « Particules » n'était plus recopié tant qu'il était caché : il l'est maintenant, avec tout le reste.
            refresh();
            return;
        }

        // Les pastilles des grands pas possibles, sous le bandeau (version 0.309) : elles remplacent la barre de l'explosion.
        Challenge challenge = game.activeChallenge();
        boolean canExplode = game.canExplode() && !exploding;
        stepDots.show(StepDots.Step.FUSION, game.canFuse() && !phase, "");
        stepDots.show(StepDots.Step.EXPLOSION, canExplode && !phase, canExplode ? explosionHint(challenge) : "");
        stepDots.show(StepDots.Step.BIG_BANG, game.canBigBang() && !phase, "");
        stepDots.show(StepDots.Step.UNIVERSE, game.canFormCosmos(Cosmos.UNIVERSE), "");
        // Le thème clair suit la taille de la fenêtre.
        themeFilter.fit();
        // Les objectifs du moment, en haut à gauche du bandeau, une ligne courte chacun (le texte complet au survol).
        boolean goal = settings.showGoal() && game.isStarted();
        header.setGoals(!goal ? List.of() : phase ? (settings.goalPart(Goals.Part.GOAL) ? List.of(new Goals.Line(
                "Acte 4 : " + game.universes() + " univers", "Quatrième acte : l'univers. " + game.universes()
                + " univers" + (game.isDimensionsUnlocked() ? ", " + game.dimensionsUnlocked()
                        + (game.dimensionsUnlocked() > 1 ? " dimensions sur " : " dimension sur ") + Game.MAX_DIMENSIONS : "")
                + ". La suite viendra.")) : List.of()) : Goals.lines(game, settings::goalPart, pace));
        String detailKey = Detail.keyName(settings.detailKey());
        detailHint.setText(Detail.shown()
                ? (settings.detailToggle() ? detailKey + " : cacher les détails" : "Détails affichés")
                : detailKey + " : détails");
        detailHint.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 1 8; -fx-background-radius: 3;"
                + " -fx-border-radius: 3;" + (Detail.shown()
                ? " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: transparent; -fx-border-color: #3a4a5e;"));
        KeyCode helpKey = settings.shortcuts() ? settings.key(Shortcuts.Action.HELP) : null;
        helpButton.setText(helpPane.isOpen() ? "Fermer l'aide" : helpKey == null ? "Aide" : Shortcuts.name(helpKey) + " : aide");
        helpButton.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 1 8; -fx-cursor: hand; -fx-background-radius: 3;"
                + " -fx-border-radius: 3;" + (helpPane.isOpen()
                ? " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: transparent; -fx-border-color: #3a4a5e;"));
        if (selectedTab == Tab.SETTINGS) settingsPage.refresh();
        // Le défi en cours reste sous les yeux, quel que soit l'onglet.
        BangChallenge bangChallenge = game.activeBangChallenge();
        challengeLabel.setVisible((challenge != null || bangChallenge != null) && !phase);
        challengeLabel.setManaged((challenge != null || bangChallenge != null) && !phase);
        if (challenge != null || bangChallenge != null) {
            String ordinary = challenge == null ? "" : "Défi « " + challenge.name() + " » depuis " + Format.duration(game.stats().runTime())
                    + " : " + (Detail.shown() ? ChallengesPane.rule(challenge) : ChallengesPane.shortRule(challenge));
            String constraint = bangChallenge == null ? "" : "Défi de Big Bang « " + bangChallenge.label() + " » depuis "
                    + Format.duration(game.bangChallengeTime()) + " : " + (Detail.shown() ? bangChallenge.rule()
                    : lowerFirst(bangChallenge.brief()) + ", jusqu'au prochain Big Bang");
            challengeLabel.setText(constraint + (constraint.isEmpty() || ordinary.isEmpty() ? "" : "\n") + ordinary);
        }
    }

    /**
     * Remplit la carte d'une amélioration payée en particules : son niveau, sa touche, ce qu'elle
     * rapporte, ce que le clic achèterait et à quel prix, et le temps qu'il reste à attendre.
     */
    private void show(Upgrade upgrade, Card card) {
        String id = upgrade.id();
        KeyCode key = settings.shortcuts() ? keyOf(upgrade) : null;
        boolean generator = upgrade.effect() instanceof Effect.AddGenerator;
        // Les générateurs se comptent : c'est leur nombre qui décide de la fusion.
        // Sans plafond (bulle du jeune arbre, version 0.148), le nombre seul : « 124/1000000 » ne dirait rien.
        String level = generator ? game.generatorCount() + (game.maxGeneratorCount() > game.generatorCap() ? "" : "/" + game.maxGeneratorCount())
                : game.levelOf(id) + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : "");
        String mark = key == null ? "" : Shortcuts.name(key);
        if (game.isMaxed(id)) {
            card.show(Card.State.DONE, level, "max", upgrade.name(), effectOf(upgrade), detailOf(upgrade), "", "");
            return;
        }
        // Ce que le clic achèterait : la quantité choisie, ramenée à ce qui est à portée, un niveau au moins.
        int count = Math.max(1, game.affordableLevels(id, settings.buyAmount().count()));
        BigNum cost = game.costOf(id, count);
        double wait = game.secondsUntilParticles(cost);
        boolean affordable = game.canBuy(id);
        card.show(affordable ? Card.State.READY : game.levelOf(id) > 0 ? Card.State.STARTED : Card.State.WAITING,
                level, mark, upgrade.name(), effectOf(upgrade), detailOf(upgrade),
                (count > 1 ? "×" + count + " pour " : "") + Format.count(cost)
                        + (cost.gt(BigNum.ONE) ? " particules" : " particule"),
                wait <= 0 || Double.isInfinite(wait) ? "" : Format.wait(wait));
    }

    /** Ce que rapporte une amélioration payée en particules, en quelques mots. */
    private String effectOf(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplySpeed speed -> {
                String text = "+" + percent(speed.perLevel() - 1 + game.speedExtraPerLevel()) + " % de vitesse";
                if (!speed.hasMilestones()) yield text;
                int left = game.nextSpeedMilestone(upgrade.id()) - game.speedLevels(upgrade.id());
                yield text + "\nPalier " + Format.multiplier(BigNum.of(game.speedMilestoneFactor(upgrade.id())))
                        + " dans " + left + (left > 1 ? " niveaux" : " niveau");
            }
            case Effect.MultiplyByGenerators coupling -> {
                int level = game.levelOf(upgrade.id());
                yield "+" + percent(coupling.perGenerator()) + " % par autre générateur"
                        + (level > 0 ? "\nActuellement " + Format.multiplier(game.particlesMultiplier(upgrade.id(), level)) : "");
            }
            case Effect.AddGenerator generator -> game.generatorCount() < game.generatorsPerAtom()
                    ? "Il en faut " + game.generatorsPerAtom() + " pour fusionner"
                    : game.isMaxed(upgrade.id()) ? "Tous réunis : prêts à fusionner"
                    : "Chaque groupe de " + game.generatorsPerAtom() + " compte à la fusion";
            default -> "";
        };
    }

    /** L'explication complète d'une amélioration payée en particules, pour le mode détails. */
    private String detailOf(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplySpeed speed -> {
                String text = "Chaque niveau accélère tous les générateurs.";
                if (!speed.hasMilestones()) yield text;
                yield text + " Tous les " + speed.milestoneEvery() + " niveaux, un palier multiplie les particules créées par "
                        + ElementText.number(game.speedMilestoneFactor(upgrade.id())) + ". Paliers atteints : "
                        + game.speedMilestones() + ", particules " + Format.multiplier(game.speedMilestoneMultiplier()) + ".";
            }
            case Effect.MultiplyByGenerators coupling ->
                    "Chaque niveau ajoute " + percent(coupling.perGenerator()) + " % de particules par autre générateur "
                            + "possédé : plus il y a de générateurs, plus chaque niveau compte.";
            case Effect.AddGenerator generator -> game.maxGeneratorCount() > game.generatorsPerAtom()
                    ? "Chaque générateur crée des particules. La fusion rapporte ses atomes une fois par groupe de "
                            + game.generatorsPerAtom() + ", avec une prime par groupe au-delà du premier."
                    : "Chaque générateur crée des particules. Réunis, les " + game.generatorsPerAtom()
                            + " générateurs fusionnent en atomes.";
            default -> "";
        };
    }

    /** 0.1 → « 10 », 0.125 → « 12.5 ». */
    private static String percent(double fraction) {
        String text = String.format(Locale.ROOT, "%.1f", fraction * 100);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    /** La ligne qui rappelle les raccourcis qu'aucune carte ne porte, sous les cartes. */
    private String shortcutsHint() {
        KeyCode all = settings.key(Shortcuts.Action.BUY_ALL);
        KeyCode pause = settings.key(Shortcuts.Action.PAUSE);
        KeyCode help = settings.key(Shortcuts.Action.HELP);
        return "Autres raccourcis : " + (all == null ? "" : Shortcuts.name(all) + " achète tout ce qui est à portée, ")
                + (pause == null ? "" : Shortcuts.name(pause) + " met en pause, ")
                + (help == null ? "" : Shortcuts.name(help) + " ouvre l'aide et la liste complète. ")
                + "Les touches des cartes achètent la quantité choisie, depuis n'importe quel onglet.";
    }
}
