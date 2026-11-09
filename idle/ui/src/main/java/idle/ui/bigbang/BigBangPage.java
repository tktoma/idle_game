package idle.ui.bigbang;

import idle.core.Game;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Assembly;
import idle.core.sky.Cosmos;
import idle.ui.GameApp;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.MatterText;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Button;

/**
 * L'onglet « Big Bang » : l'acte qui s'ouvre au premier Big Bang ({@link Game#bigBang()}).
 *
 * <p>En haut, le nombre de Big Bangs déclenchés. En dessous, dix sous-pages ; à partir des états,
 * chacune n'apparaît qu'une fois ouverte par une amélioration ou un Big Bang :
 * <ul>
 *   <li>« Molécules » : le catalogue ({@link Game#molecules()}, 73 molécules), un rayon à la fois
 *       ({@link Molecule.Kind}, quatre rayons). Seuls les rayons ouverts sont proposés : les petites molécules
 *       d'abord, puis un rayon de plus à chaque amélioration d'espace. Chaque molécule est une {@link Card}
 *       basse (version 0.178) : son petit dessin ({@link MoleculeArt}), sa formule, son nom et combien ont
 *       été créées sur la première ligne, son bonus et ce que demande la suivante ensuite, et une ligne de
 *       plus seulement si le prochain pas la demande ; l'espace occupé, le rendement et la formule détaillée
 *       sont dans l'info-bulle. Un clic la crée, avec des exemplaires du tableau périodique. Une
 *       fois la création continue acquise, tenir le clic répète la création. Son étoile l'épingle
 *       parmi les favorites, qui ont leur pastille devant les rayons. Deux filtres, avec la recherche :
 *       par état, et par usage (créable maintenant, utile au prochain pas) ;</li>
 *   <li>« Expansion de la matière » : l'espace créé, ce qu'il gagne par seconde, ce que les
 *       molécules en occupent, et une vue de cet espace ({@link SpaceView}) où l'on peut
 *       s'approcher des molécules ou reculer jusqu'à tout voir ;</li>
 *   <li>« Améliorations » : ce que l'espace gagné ouvre ({@link Game#spaceUpgrades()}) : les
 *       rayons de molécules, un à un, le rassemblement, les gestes de confort. Rien ne s'y dépense
 *       ni ne s'y clique : chacune est acquise d'elle-même quand son seuil est atteint. Une
 *       amélioration n'est montrée que lorsque celle dont elle dépend est acquise ;</li>
 *   <li>« États de la matière » : le rassemblement des molécules en gaz, liquides et cristaux
 *       ({@link Game#formSubstance(String)}), un achat unique par sorte de molécule. La sous-page
 *       a un lieu par état, où se rangent les cartes de ses molécules ; une molécule n'y apparaît
 *       que lorsque le joueur en a créé au moins une ;</li>
 *   <li>« Assemblages » : les roches, minerais, pierres précieuses, eaux, gaz et hydrocarbures faits de
 *       plusieurs sortes de molécules ({@link Game#formAssembly(String)}, un par astre : trente), une section
 *       par famille. Un assemblage n'y apparaît que lorsque le joueur a créé au moins un de ses ingrédients ;</li>
 *   <li>« Astres » : le ciel et les corps qu'on y forme, de l'amas de roches à la planète
 *       ({@link BodiesPane}) ;</li>
 *   <li>« Univers » : la galaxie, l'amas de galaxies et l'univers ({@link GalaxyPane}) ;</li>
 *   <li>« Paliers » : les paliers de Big Bang et, au cinquième, les défis de Big Bang
 *       ({@link BangChallengesPane}) ;</li>
 *   <li>« Sondes » : les voyages et leur butin ({@link idle.ui.probes.ProbesPane}) ;</li>
 *   <li>« Systèmes solaires » : les planètes et lunes rangées autour des étoiles ({@link SystemsPane}).</li>
 * </ul>
 * Une fois les gestes groupés acquis, un bouton en haut des états, des assemblages et des astres
 * fait d'un coup tout ce qui est possible. Un sous-onglet où quelque chose est prêt porte une
 * pastille ({@link Readiness}).
 *
 * <p>Les cartes d'un rayon ne sont construites que la première fois qu'on l'ouvre.
 *
 * <p>Le code est rangé en parties (version 0.098) : une chaîne de classes abstraites, une par thème, dont
 * celle-ci est le bout ; le reste du programme ne voit que {@code BigBangPage}. De la base au bout :
 * {@link BigBangPageBase}, {@link BigBangPageControls}, {@link BigBangPageRefresh}. Quand une partie a
 * besoin d'une méthode définie plus loin dans la chaîne, elle la déclare abstraite en tête, sous « Ce que
 * fournissent les parties suivantes ».
 */
public final class BigBangPage extends BigBangPageRefresh {

    public BigBangPage(Game game) {
        super(game);
    }

    void refreshMolecules() {
        refreshPulse();
        moleculesIntro.setText("Une molécule se crée avec des exemplaires du tableau périodique et de l'espace libre ("
                + Format.count(game.freeSpace()) + " en ce moment), et reste pour toujours."
                + Detail.only(" Les exemplaires quittent le tableau avec ce qu'ils y apportaient ; un exemplaire de "
                        + "chaque élément y reste, pour que le tableau puisse toujours exploser. Une molécule occupe "
                        + ElementText.number(Game.SPACE_PER_PROTON) + " unités d'espace par proton ; un liquide en prend le "
                        + "double, un gaz " + ElementText.number(Molecule.State.GAS.spaceFactor()) + " fois plus. Chaque sorte "
                        + "donne quelque chose à la première molécule, puis chaque fois que leur nombre double (3, 7, 15…) : "
                        + "mieux vaut beaucoup de sortes qu'une seule énorme. Une sorte rassemblée (États de la matière) "
                        + "attire la matière : chaque création y ajoute plusieurs molécules d'un coup."));
        // Les rayons : combien de leurs molécules ont été créées au moins une fois.
        boolean filtered = moleculeFilter.active();
        Map<Molecule.Kind, int[]> totals = new EnumMap<>(Molecule.Kind.class);
        for (Molecule molecule : game.molecules()) {
            int[] total = totals.computeIfAbsent(molecule.kind(), each -> new int[3]);
            total[1]++;
            if (game.moleculeCount(molecule.id()) > 0) total[0]++;
            if (filtered && shown(molecule)) total[2]++;
        }
        // Un rayon fermé n'est pas proposé du tout ; celui qu'on regardait a pu se refermer avec une remise à zéro.
        if (!game.isMoleculeKindUnlocked(kind)) selectKind(Molecule.Kind.SIMPLE);
        // Le meilleur achat, et le bouton qui range les cartes par rendement.
        findBest();
        // La barre garde sa place même sans meilleur achat : rien ne saute sous la souris.
        bestButton.setDisable(best == null);
        if (best == null) {
            bestLabel.setText("Meilleur achat : aucune création possible pour l'instant");
            bestButton.setText("Créer le meilleur achat");
        } else {
            int lot = game.moleculesNextCreation(best.id());
            bestLabel.setText("Meilleur achat : " + best.name() + " " + best.formula()
                    + (best.kind() == kind ? "" : " (rayon " + best.kind().label() + ")") + ", " + gainText(game.moleculeGain(best.id()))
                    + " pour " + Format.count(game.moleculeNextSpace(best.id())) + " d'espace"
                    + Detail.only(". C'est la création possible maintenant qui rapporte le plus par unité d'espace : son bonus "
                            + "et ce que son état ajoute, rapportés à ce que valent déjà ces grandeurs."));
            bestButton.setText(lot > 1 ? "Créer le meilleur achat ×" + lot : "Créer le meilleur achat");
        }
        sortButton.setText(sortedByYield ? "Ordre : meilleur rendement d'abord" : "Ordre : catalogue");
        // Les favorites : leur pastille n'existe que s'il y en a ; la dernière retirée, on revient au rayon.
        List<Molecule> pinned = game.favoriteMolecules();
        if (favoritesShown && pinned.isEmpty()) selectKind(kind);
        favoritesChip.setText("★ Favorites " + pinned.size());
        favoritesChip.setVisible(!pinned.isEmpty());
        favoritesChip.setManaged(!pinned.isEmpty());
        ShoppingList shopping = game.shoppingList();
        needsLabel.setText(needs(shopping, game.missingByKind()));
        needsLabel.setVisible(!needsLabel.getText().isEmpty());
        needsLabel.setManaged(needsLabel.isVisible());
        kindChips.forEach((each, chip) -> {
            int[] total = totals.getOrDefault(each, new int[3]);
            // Pendant une recherche, chaque rayon dit combien de ses molécules correspondent.
            chip.setText(each.label() + " " + (filtered ? total[2] + " sur " + total[1] : total[0] + "/" + total[1]));
            boolean open = game.isMoleculeKindUnlocked(each);
            chip.setVisible(open);
            chip.setManaged(open);
        });
        int[] here = totals.getOrDefault(kind, new int[3]);
        if (favoritesShown) {
            // Parmi les favorites, le compte porte sur elles, tous rayons confondus.
            here = new int[] {0, pinned.size(), 0};
            for (Molecule molecule : pinned) {
                if (filtered && shown(molecule)) here[2]++;
            }
        }
        moleculesFound.setText(!filtered ? "" : here[2] == 0
                ? (favoritesShown ? "Aucune favorite ne correspond." : "Aucune molécule ne correspond, dans aucun rayon ouvert.")
                : here[2] + (here[2] > 1 ? " molécules sur " : " molécule sur ") + here[1]
                        + (favoritesShown ? " parmi les favorites" : " dans ce rayon"));
        moleculesFound.setVisible(filtered);
        moleculesFound.setManaged(filtered);
        boolean batch = filtered || favoritesShown;
        foundRow.setVisible(batch);
        foundRow.setManaged(batch);
        if (batch) {
            boolean possible = false;
            for (Molecule molecule : shownMolecules()) possible |= game.canCreateMolecule(molecule.id());
            createShownButton.setDisable(!possible);
            createShownButton.setText(possible ? "Créer le maximum de celles-ci" : "Rien à créer pour l'instant");
        }
        // Ce que le prochain pas demande de chaque sorte : la troisième ligne des cartes concernées.
        Map<String, ShoppingList.Item> wanted = new java.util.HashMap<>();
        for (ShoppingList.Item item : shopping.items()) wanted.put(item.molecule().id(), item);
        if (favoritesShown) {
            // Les favorites : une carte par molécule épinglée, dans l'ordre où elles l'ont été ; les autres se retirent.
            for (Molecule molecule : pinned) {
                Card card = favoriteCards.computeIfAbsent(molecule, each -> {
                    Card built = moleculeCard(each);
                    favoriteGrid.add(built);
                    return built;
                });
                boolean match = game.isMoleculeKindUnlocked(molecule.kind()) && (!filtered || shown(molecule));
                favoriteGrid.show(card, match);
                if (match) fill(molecule, card, shopping, wanted.get(molecule.id()));
            }
            favoriteCards.forEach((molecule, card) -> {
                if (!pinned.contains(molecule)) favoriteGrid.show(card, false);
            });
            return;
        }
        TileGrid grid = grids.get(kind);
        cards.forEach((molecule, card) -> {
            if (molecule.kind() != kind) return;      // seul le rayon affiché se met à jour
            boolean match = !filtered || shown(molecule);
            grid.show(card, match);
            if (match) fill(molecule, card, shopping, wanted.get(molecule.id()));
        });
    }

    /**
     * Le prochain pas en une ligne : ce qu'il manque, par rayon et par état. « Pour Amas rocheux : 3 à créer
     * en Acides et sels, 2 en Minéraux et matériaux · 4 cristaux à rassembler. » Vide quand il n'y a rien à viser.
     */
    private String needs(ShoppingList shopping, Map<Molecule.Kind, Integer> missing) {
        if (shopping.kind() == ShoppingList.Kind.NONE) return "";
        if (shopping.ready()) return "Pour " + shopping.target() + " : tout est réuni, il n'y a plus qu'à le former.";
        List<String> parts = new ArrayList<>();
        missing.forEach((each, count) -> parts.add(count + " en " + each.label()
                + (game.isMoleculeKindUnlocked(each) ? "" : " (rayon à ouvrir dans les améliorations)")));
        StringBuilder text = new StringBuilder("Pour " + shopping.target() + " : ");
        if (!parts.isEmpty()) text.append(String.join(", ", parts).replaceFirst(" en ", " à créer en "));
        List<String> matter = new ArrayList<>();
        shopping.matter().forEach((state, count) -> matter.add(count + " " + MatterText.plural(state)));
        shopping.sorts().forEach((state, count) -> matter.add(count + (count > 1 ? " sortes de " : " sorte de ") + MatterText.plural(state)));
        if (!matter.isEmpty()) text.append(parts.isEmpty() ? "" : " · ").append(String.join(", ", matter)).append(" à rassembler");
        if (parts.isEmpty() && matter.isEmpty()) text.append("il reste à rassembler ou à assembler ce qui est déjà créé");
        return text.append(".").toString();
    }

    /** Les molécules à l'écran : les favorites ou le rayon affiché, filtres appliqués. */
    public List<Molecule> shownMolecules() {
        boolean filtered = moleculeFilter.active();
        List<Molecule> shown = new ArrayList<>();
        if (favoritesShown) {
            for (Molecule molecule : game.favoriteMolecules()) {
                if (game.isMoleculeKindUnlocked(molecule.kind()) && (!filtered || shown(molecule))) shown.add(molecule);
            }
        } else {
            for (Molecule molecule : game.molecules()) {
                if (molecule.kind() == kind && (!filtered || shown(molecule))) shown.add(molecule);
            }
        }
        return shown;
    }

    /**
     * Crée tout ce qui peut l'être parmi les molécules à l'écran, une de chaque à tour de rôle, pour que
     * l'espace et les éléments se partagent entre elles au lieu d'aller tous à la première.
     *
     * @return le nombre de créations faites
     */
    int createAllShown() {
        List<Molecule> targets = shownMolecules();
        int made = 0;
        boolean any = true;
        while (any && made < MAX_BATCH_CREATIONS) {
            any = false;
            for (Molecule molecule : targets) {
                if (game.createMolecule(molecule.id())) {
                    made++;
                    any = true;
                }
            }
        }
        return made;
    }

    /** Garde-fou : le plus grand nombre de créations d'un seul clic sur « Créer le maximum ». */
    static final int MAX_BATCH_CREATIONS = 5_000;

    /**
     * Remplit la carte d'une molécule (version 0.178, allégée) : en tête son dessin, sa formule et son
     * compte ; son nom ; son bonus, et dessous ce que demande la prochaine création ou pourquoi elle
     * attend ; une ligne de plus seulement si le prochain pas la demande ({@code wanted}). L'espace
     * qu'elle occupe, son rendement et sa formule détaillée sont dans l'info-bulle.
     */
    private void fill(Molecule molecule, Card card, ShoppingList shopping, ShoppingList.Item wanted) {
        card.showPin(game.isMoleculeFavorite(molecule.id()));
        card.setRepeats(game.isHoldCreateUnlocked());
        int count = game.moleculeCount(molecule.id());
        boolean reachable = game.isMoleculeWithinReach(molecule.id());
        boolean elements = game.hasElementsForMolecule(molecule.id());
        boolean ready = game.canCreateMolecule(molecule.id());
        Map<Integer, Integer> cost = game.nextMoleculeCost(molecule.id());
        BigNum volume = game.moleculeVolume(molecule.id());
        // Une sorte rassemblée attire la matière : une création y ajoute plusieurs molécules, s'il y a la place.
        int drawn = game.moleculesPerCreation(molecule.id());
        int next = ready ? game.moleculesNextCreation(molecule.id()) : drawn;
        // Ce que la prochaine création ajouterait aux grandeurs du jeu, en proportion de ce qu'elles valent.
        double gain = reachable ? game.moleculeGain(molecule.id()) : 0;
        boolean full = game.isMoleculeKindFull(molecule.id());
        String price = ready ? (next > 1 ? "Créer ×" + Format.count(BigNum.of(next)) + " : " : "Créer : ") + recipe(cost, false)
                : full ? "Pleine : un milliard au plus par sorte"
                : !reachable ? "Hors de portée : " + recipe(cost, false)
                : !elements ? "Il faut " + recipe(cost, true)
                : "Il manque " + Format.count(volume.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
        // La troisième ligne : « Pour l'assemblage Granite : 120 / 200 », seulement quand le prochain pas la demande.
        String purpose = wanted == null ? "" : "\nPour " + (wanted.purpose().isEmpty() ? shopping.target() : "l'assemblage " + wanted.purpose())
                + " : " + Format.count(BigNum.of(Math.min(wanted.owned(), wanted.needed()))) + " / " + Format.count(BigNum.of(wanted.needed()))
                + (wanted.gather() && wanted.missing() == 0 ? ", à rassembler" : "");
        card.setHint(molecule.name() + " " + molecule.formula() + " : " + molecule.atoms() + (molecule.atoms() > 1 ? " atomes, " : " atome, ")
                + molecule.protons() + " protons (" + recipe(molecule.recipe(), false) + "). Occupe " + Format.count(volume) + " d'espace"
                + (molecule.hasState() ? ", " + MatterText.plural(molecule.state()).toLowerCase(java.util.Locale.ROOT) : "") + "."
                + (gain > 0 ? " Rapporte " + gainText(gain) + " pour " + Format.count(game.moleculeNextSpace(molecule.id())) + " d'espace." : "")
                + (drawn > 1 ? " Rassemblée : une création en ajoute " + drawn + " d'un coup, pour le prix d'une seule"
                        + (next < drawn ? " (" + next + " seulement, faute de place)." : ".") : ""));
        card.show(ready ? Card.State.READY : full ? Card.State.DONE : !reachable ? Card.State.LOCKED
                        : count > 0 ? Card.State.STARTED : Card.State.WAITING,
                molecule.formula(), count > 0 ? "×" + Format.count(BigNum.of(count)) : "", molecule.name(),
                bonus(molecule) + purpose, Detail.shown() ? given(molecule, count).trim() : "",
                price, molecule == best ? "★ meilleur achat" : molecule == bestHere ? "meilleur du rayon" : "");
    }

    /** « Prochain rayon : Acides et sels, à 2 000 d'espace créé, dans 2 h 10. » Vide quand tous les rayons sont ouverts. */
    private String nextKind() {
        Molecule.Kind next = game.nextMoleculeKind();
        if (next == null) return "";
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenKind open && open.kind() == next) {
                double wait = game.secondsUntilSpace(upgrade.space());
                return "Prochain rayon : " + next.label() + ", à " + Format.count(upgrade.space()) + " d'espace créé"
                        + (wait <= 0 || Double.isInfinite(wait) ? "" : ", dans " + Format.wait(wait)) + ".";
            }
        }
        return "";
    }

    /** Un bouton d'impulsion : chaque clic dépense une charge pour dix secondes d'expansion d'un coup. */
    Button pulseButton() {
        Button button = new Button();
        button.setStyle(PULSE_STYLE);
        button.setFocusTraversable(false);
        button.setMinWidth(PULSE_WIDTH);
        button.setVisible(false);       // jusqu'au premier Big Bang
        button.setManaged(false);
        button.setOnAction(event -> {
            game.pulse();
            refresh();
        });
        return button;
    }

    private static final String PULSE_STYLE = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6; -fx-text-fill: #10151f; -fx-background-color: "
            + GameApp.BIG_BANG_COLOR + "; -fx-border-color: #ffffff;";
    /** Largeur fixe : le texte change à chaque clic, le bouton reste sous la souris. */
    private static final double PULSE_WIDTH = 420;

    /** Recopie l'état de l'impulsion sur ses deux boutons. */
    private void refreshPulse() {
        boolean unlocked = game.isPulseUnlocked();
        int charges = game.pulseCharges();
        String text = charges > 0
                ? "Impulsion : +" + Format.count(game.pulseGain()) + " d'espace  (" + charges + " / " + Game.PULSE_MAX_CHARGES + ")"
                : "Impulsion : prochaine charge dans " + Format.wait(Math.max(0.1, game.secondsUntilPulse()));
        for (Button button : List.of(pulseButton, moleculesPulseButton)) {
            button.setVisible(unlocked);
            button.setManaged(unlocked);
            button.setText(text);
            button.setDisable(charges == 0);
        }
    }

    /** Montre ou masque les anomalies du ciel selon ce réglage (version 0.127). */
    public void showSkyEvents(java.util.function.BooleanSupplier shown) {
        skyEventsShown = shown;
    }

    /** Ce que dit le bouton d'une anomalie du ciel : ce qu'elle est, et ce qu'elle donne. */
    static String skyEventText(idle.core.sky.SkyEvent event) {
        return switch (event) {
            case PARTICLE_WIND -> "Un vent de particules souffle : le capter (particules ×" + ElementText.number(Game.WIND_FACTOR)
                    + " pendant " + Format.wait(Game.WIND_SECONDS) + ")";
            case EXPLORATION_WINDOW -> "Une fenêtre d'exploration s'ouvre : en profiter (sondes en vol +"
                    + Format.wait(Game.EXPLORATION_SECONDS) + " de voyage)";
        };
    }

    void refreshExpansion() {
        refreshPulse();
        BigNum space = game.state().space();
        spaceLabel.setText(Format.count(space) + (space.gt(BigNum.ONE) ? " unités d'espace" : " unité d'espace"));
        BigNum reserved = game.reservedSpace();
        boolean comet = game.isCometVisible();
        cometButton.setVisible(comet);
        cometButton.setManaged(comet);
        if (comet) {
            cometButton.setText("Une comète traverse l'expansion : la saisir (" + (int) Math.ceil(game.cometVisibleFor()) + " s)");
        }
        // Les anomalies du ciel (version 0.127) : facultatives, et masquées si le joueur l'a choisi.
        idle.core.sky.SkyEvent sky = skyEventsShown.getAsBoolean() ? game.skyEvent() : null;
        skyEventButton.setVisible(sky != null);
        skyEventButton.setManaged(sky != null);
        if (sky != null) skyEventButton.setText(skyEventText(sky) + " (" + Format.wait(game.skyEventVisibleFor()) + ")");
        rateLabel.setText("+" + Format.amount(game.spacePerSecond()) + " par seconde"
                + (game.darkMatterSpaceBoost() > 1 ? " (matière noire ×" + ElementText.number(game.darkMatterSpaceBoost()) + ")" : "")
                + (game.cometBoost() > 1 ? " (sillage de comète ×" + ElementText.number(game.cometBoost()) + ", encore "
                        + Format.wait(game.cometBoostLeft()) + ")" : "")
                + (game.windMultiplier() > 1 ? " (vent de particules : particules ×" + ElementText.number(game.windMultiplier())
                        + ", encore " + Format.wait(game.state().windLeft()) + ")" : "") + "  ·  "
                + Format.count(game.occupiedSpace()) + " occupées par " + Format.count(BigNum.of(game.moleculesCreated()))
                + (game.moleculesCreated() > 1 ? " molécules" : " molécule")
                + (reserved.isZero() ? "" : "  ·  " + Format.count(reserved) + " réservées aux rassemblements")
                + "  ·  " + Format.count(game.freeSpace()) + " libres");
        rateBreakdown.refresh();
        String next = nextKind();
        expansionNote.setText("L'espace grandit avec le temps de jeu, et les molécules s'y logent."
                + (next.isEmpty() ? " Tous les rayons de molécules sont ouverts." : " " + next)
                + Detail.only(" Une impulsion donne d'un coup " + (int) Game.PULSE_SECONDS + " secondes d'expansion ; une charge "
                        + "revient toutes les " + (int) Game.PULSE_REFILL_SECONDS + " secondes, " + Game.PULSE_MAX_CHARGES
                        + " au plus en réserve (raccourci : I, à tenir)."
                        + " Chaque Big Bang ajoute " + ElementText.number(Game.SPACE_PER_SECOND)
                        + " unité par seconde, et les inflations, les molécules, les assemblages et les astres qui "
                        + "agissent sur l'espace multiplient le tout (par " + ElementText.number(game.moleculeBoost(Molecule.Stat.SPACE))
                        + " en ce moment). L'espace grandit quel que soit l'onglet affiché, sauf en pause ; ni "
                        + "l'explosion ni le Big Bang ne le reprennent, et une molécule garde sa place pour toujours. "
                        + "L'espace libre sert aux molécules et à leurs lieux de rassemblement ; les améliorations, "
                        + "elles, ne regardent que l'espace créé en tout. Dans la vue, le cercle clair est le bord de "
                        + "l'espace créé, le cercle sombre le cœur utilisé ; les molécules rassemblées s'y rangent par "
                        + "état, les plus serrés au plus près du centre : cristaux, liquides, gaz. "
                        + "Celles d'une même sorte partagent un amas de leur couleur, cerné de celle de son état ; les "
                        + "amas se touchent sans se chevaucher. De très loin, la matière se fond en nuages de gaz. "
                        + "La molette ou les boutons rapprochent et éloignent : on ne recule pas plus loin que l'espace "
                        + "créé. Tirer avec la souris déplace la vue. Une fois l'amas de galaxies formé, puis l'univers, "
                        + "un bouton recule la vue jusqu'à eux."));
        Cosmos far = spaceView.cosmosShown();
        zoomOut.setDisable(far != null || spaceView.isFitted());
        zoomAll.setDisable(far != null || spaceView.isFitted());
        zoomIn.setDisable(far != null || spaceView.isClosest());
        viewToggle.setVisible(game.hasGalaxy());
        viewToggle.setManaged(game.hasGalaxy());
        viewToggle.setText(far != null ? "Voir la galaxie" : spaceView.showsGalaxy() ? "Voir la matière" : "Voir la galaxie");
        boolean cluster = game.hasCosmos(Cosmos.CLUSTER) && far != Cosmos.CLUSTER;
        clusterView.setVisible(cluster);
        clusterView.setManaged(cluster);
        boolean universe = game.hasCosmos(Cosmos.UNIVERSE) && far != Cosmos.UNIVERSE;
        universeView.setVisible(universe);
        universeView.setManaged(universe);
    }

    /** Ce que donne une sorte de molécule, en quelques mots : « Espace +5 % par doublement ». Vide si elle ne donne rien. */
    static String bonus(Molecule molecule) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> what(boost.stat()) + " +" + ElementText.percent(boost.perMolecule()) + " par doublement";
            case Molecule.Uncap uncap -> PeriodicTable.element(uncap.element()).name() + " : +1 exemplaire au plus par doublement";
            case null -> "";
        };
    }

    /** Ce que donnent en tout les molécules déjà créées d'une sorte, pour le mode détails. Vide si elles ne donnent rien. */
    private String given(Molecule molecule, int count) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> count == 0 ? "" : " Déjà +"
                    + ElementText.percent(boost.perMolecule() * game.effectiveMolecules(molecule.id()))
                    + " avec celles-ci" + (game.hasSubstance(molecule.id()) ? ", rassemblées" : "") + " ; molécules, assemblages, astres et améliorations réunis multiplient " + reach(boost.stat()) + " par "
                    + ElementText.number(game.moleculeBoost(boost.stat())) + ".";
            case Molecule.Uncap uncap -> " " + PeriodicTable.element(uncap.element()).name() + " : "
                    + game.maxCopiesOf(PeriodicTable.element(uncap.element())) + " exemplaires au plus en ce moment.";
            case null -> "";
        };
    }

    public static String what(Molecule.Stat stat) {
        return switch (stat) {
            case SPACE -> "Espace par seconde";
            case PARTICLES -> "Particules";
            case ATOMS -> "Atomes par fusion";
            case DARK_GROWTH -> "Croissance de la matière noire";
        };
    }

    /** Le milieu de « toutes les molécules réunies multiplient … par N ». */
    private static String reach(Molecule.Stat stat) {
        return switch (stat) {
            case SPACE -> "l'espace gagné";
            case PARTICLES -> "les particules";
            case ATOMS -> "les atomes";
            case DARK_GROWTH -> "la croissance";
        };
    }

    /**
     * Une liste d'éléments et de nombres : « 2 H, 1 O ». Avec {@code owned}, ce que le joueur peut
     * y mettre est ajouté entre parenthèses : ses exemplaires, moins celui qui doit rester.
     */
    private String recipe(Map<Integer, Integer> atoms, boolean owned) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Integer, Integer> atom : atoms.entrySet()) {
            if (text.length() > 0) text.append(", ");
            text.append(atom.getValue()).append(' ').append(PeriodicTable.element(atom.getKey()).symbol());
            if (owned) text.append(" (").append(Math.max(0, game.elementCount(atom.getKey()) - 1)).append(')');
        }
        return text.toString();
    }
}
