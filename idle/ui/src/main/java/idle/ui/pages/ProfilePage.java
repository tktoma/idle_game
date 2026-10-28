package idle.ui.pages;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.profile.Title;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.ui.EventLog;
import idle.ui.GameApp;
import idle.ui.Readiness;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.MatterText;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * L'onglet « Profil » (version 0.147), là dès le premier générateur : qui joue, jusqu'où il est allé, et ce qu'il a
 * trouvé en route. Six sous-onglets :
 * <ul>
 *   <li>« Profil » : l'avatar, le nom du joueur (modifiable : Entrée ou un clic ailleurs le valide, le jeu le nettoie),
 *       le titre porté, le temps de jeu, les univers, les Big Bangs et les titres obtenus ; puis la galerie des avatars
 *       ({@link Avatar}, dessinés par le jeu, la plupart ouverts par un titre, et l'image que le joueur importe, copiée
 *       dans le dossier des données et recadrée en rond) ; enfin la liste des {@link Title titres}, obtenus (cliquables
 *       pour les porter) et à venir (avec leur condition) ;</li>
 *   <li>« Journal » : les dernières annonces du jeu ({@link EventLog}), la plus récente en haut ; il était la dernière
 *       sous-page des statistiques jusqu'à la 0.144 ;</li>
 *   <li>« Molécules », « États de la matière », « Assemblages », « Astres » : les albums ({@link AlbumPane}) de ce qui a
 *       été obtenu au moins une fois, ouverts avec le premier Big Bang ou dès qu'ils ont quelque chose.</li>
 * </ul>
 * Titres, nom, avatar et albums traversent le Big Bang, l'univers et « Recommencer de zéro » : la page ne fait que les
 * lire dans la partie, et seul le sous-onglet affiché se recopie.
 */
public final class ProfilePage extends VBox {

    /** La couleur du profil : un orangé tiède, à part des ressources. */
    public static final String COLOR = "#f4b183";
    /** Les sous-onglets, dans l'ordre. */
    public static final int PROFILE = 0, JOURNAL = 1, MOLECULES = 2, SUBSTANCES = 3, ASSEMBLIES = 4, BODIES = 5;
    private static final String[] NAMES = {"Profil", "Journal", "Molécules", "États de la matière", "Assemblages", "Astres"};
    /** Le nom du fichier de l'image importée, dans le dossier des données, suivi de son extension. */
    private static final String AVATAR_FILE = "avatar";
    /** Les extensions sous lesquelles l'image importée se range ; un « .jpeg » choisi devient « .jpg ». */
    private static final List<String> STORED_EXTENSIONS = List.of("png", "jpg", "gif", "bmp");
    private static final long MAX_IMAGE_BYTES = 16L << 20;
    /** Le côté auquel l'image importée est lue : assez pour l'en-tête, sans garder en mémoire une photo entière. */
    private static final double IMAGE_SIDE = 192;

    private static final String NAME_STYLE = "-fx-font-size: 13px; -fx-text-fill: #b9c7d6;";
    private static final String ROW_STYLE = "-fx-padding: 5 12; -fx-background-color: #121821; -fx-background-radius: 4;";
    private static final String HEADING_STYLE = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12 0 2 0; -fx-text-fill: " + COLOR + ";";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + COLOR + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";
    private static final double MAX_WIDTH = 760;

    private final Game game;
    /** Le dossier des données, où se copie l'image importée ; {@code null} quand le jeu ne sauvegarde pas. */
    private final Supplier<Path> folder;
    private final Button[] tabs = new Button[NAMES.length];
    private final HBox subTabs = new HBox();
    private final ScrollPane[] pages = new ScrollPane[NAMES.length];
    private int selected = PROFILE;

    // L'en-tête.
    private final AvatarView avatarView = new AvatarView(96);
    private final TextField nameField = new TextField();
    private final Label titleLabel = new Label();
    private final Label playedLabel = new Label();
    private final Label universesLabel = new Label();
    private final Label bangsLabel = new Label();
    private final Label titlesLabel = new Label();

    // La galerie.
    private final Map<Avatar, Card> avatarCards = new EnumMap<>(Avatar.class);
    private final Card customCard = new Card(COLOR);
    private final AvatarView customPreview = new AvatarView(56);
    private final Button importButton = new Button("Choisir une image…");
    private final Label importNote = new Label();
    private String galleryKey = "";

    // Les titres.
    private final Map<Title, Card> titleCards = new EnumMap<>(Title.class);
    private final Button wearLatest = new Button("Porter le plus récent");
    private final Label titlesNote = new Label();
    private String titlesKey = "";
    /** Les titres que le joueur a vus en ouvrant le profil : au-delà, l'onglet porte sa pastille. −1 : pas encore compté. */
    private int seenTitles = -1;

    // Le journal.
    private final Label journalNote = new Label();
    private final VBox journalList = new VBox(3);
    private int journalVersion = -1;

    // Les albums, et la phrase qui présente chacun ; la suite de la phrase n'apparaît qu'en mode détails.
    private final AlbumPane[] albums = new AlbumPane[4];
    private final Label[] albumIntros = {new Label(), new Label(), new Label(), new Label()};
    private static final String[] ALBUM_INTROS = {
            "Toutes les molécules du catalogue : celles que vous avez déjà créées montrent leur nom et leur formule.",
            "Les sortes qui se rassemblent en une substance : celles déjà rassemblées montrent leur état.",
            "Les assemblages du ciel : ceux déjà formés montrent leur famille.",
            "Les astres, de l'amas de roches au cœur de galaxie : ceux déjà formés montrent leur échelle."};
    private static final String ALBUM_DETAIL = " Un album garde ce qui a été obtenu une fois, même si le Big Bang, l'univers "
            + "ou « Recommencer de zéro » l'ont repris depuis.";

    // L'image importée, lue une fois tant que son fichier ne change pas.
    private Path imagePath;
    private long imageStamp;
    private Image image;

    /**
     * @param game   la partie
     * @param folder le dossier des données, ou {@code null} quand le jeu ne sauvegarde pas ; demandé à chaque fois,
     *               car il n'est connu qu'au lancement
     */
    public ProfilePage(Game game, Supplier<Path> folder) {
        super(8);
        this.game = game;
        this.folder = folder;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));
        for (int i = 0; i < NAMES.length; i++) tabs[i] = new Button(NAMES[i]);
        SubTabs.wire(true, this::select, this::refresh, tabs);
        subTabs.getChildren().addAll(tabs);
        subTabs.setAlignment(Pos.CENTER);

        pages[PROFILE] = scroll(profile());
        pages[JOURNAL] = scroll(journal());
        String bang = GameApp.BIG_BANG_COLOR;
        albums[0] = new AlbumPane(bang, "molécule créée au moins une fois", this::moleculeEntries,
                () -> game.state().foundMolecules(), true);
        albums[1] = new AlbumPane(bang, "sorte rassemblée au moins une fois", this::substanceEntries,
                () -> game.state().foundSubstances(), false);
        albums[2] = new AlbumPane(bang, "assemblage formé au moins une fois", this::assemblyEntries,
                () -> game.state().foundAssemblies(), false);
        albums[3] = new AlbumPane(bang, "astre formé au moins une fois", this::bodyEntries,
                () -> game.state().foundBodies(), false);
        for (int k = 0; k < albums.length; k++) {
            Labels.note(albumIntros[k], MAX_WIDTH);
            VBox box = new VBox(10, heading(NAMES[MOLECULES + k]), albumIntros[k], albums[k]);
            box.setAlignment(Pos.TOP_CENTER);
            pages[MOLECULES + k] = scroll(box);
        }
        StackPane stack = new StackPane(pages);
        stack.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(stack, Priority.ALWAYS);
        getChildren().addAll(subTabs, stack);
        select(PROFILE);
    }

    private static ScrollPane scroll(VBox content) {
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(8, 0, 12, 0));
        ScrollPane scroll = new ScrollPane(content);
        Labels.transparent(scroll);
        return scroll;
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle(HEADING_STYLE);
        label.setMaxWidth(MAX_WIDTH);
        return label;
    }

    /** Le sous-onglet « Profil » : l'en-tête, la galerie, les titres. */
    private VBox profile() {
        nameField.setPromptText(Game.DEFAULT_PLAYER_NAME);
        nameField.setPrefWidth(340);
        nameField.setMaxWidth(420);
        nameField.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff; -fx-prompt-text-fill: #6f8296;"
                + " -fx-background-color: #0e131b; -fx-background-radius: 6; -fx-border-color: #3a4a5e; -fx-border-radius: 6;"
                + " -fx-highlight-fill: " + COLOR + "; -fx-highlight-text-fill: #0b0e14;");
        // Entrée valide le nom et rend le clavier aux raccourcis ; perdre le clavier le valide aussi.
        nameField.setOnAction(event -> {
            commitName();
            requestFocus();
        });
        nameField.focusedProperty().addListener((seen, before, focused) -> {
            if (!focused) commitName();
        });
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        for (Label label : new Label[] {playedLabel, universesLabel, bangsLabel, titlesLabel}) label.setStyle(NAME_STYLE);
        VBox identity = new VBox(6, nameField, titleLabel, playedLabel, universesLabel, bangsLabel, titlesLabel);
        identity.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(20, avatarView, identity);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(MAX_WIDTH);
        header.setStyle("-fx-padding: 16 20; -fx-background-color: #121821; -fx-background-radius: 8;"
                + " -fx-border-color: " + COLOR + "55; -fx-border-radius: 8;");

        TileGrid gallery = new TileGrid(150, 5, 8);
        gallery.setMaxWidth(MAX_WIDTH + 80);
        for (Avatar avatar : Avatar.values()) {
            Card card = new Card(COLOR);
            AvatarView picture = new AvatarView(56);
            picture.show(avatar);
            card.setPicture(picture);
            card.setOnAction(() -> {
                if (!avatar.isOpen(game)) return;
                game.state().setAvatar(avatar.id());
                refresh();
            });
            avatarCards.put(avatar, card);
            gallery.add(card);
        }
        customCard.setPicture(customPreview);
        customCard.setOnAction(() -> {
            if (customImage() == null) return;
            game.state().setAvatar(Avatar.CUSTOM);
            refresh();
        });
        gallery.add(customCard);
        importButton.setStyle(BUTTON_STYLE);
        importButton.setFocusTraversable(false);
        importButton.setOnAction(event -> chooseImage());
        Labels.note(importNote, MAX_WIDTH);
        importNote.setVisible(false);
        importNote.setManaged(false);

        TileGrid titles = new TileGrid(230, 3, 8);
        titles.setMaxWidth(MAX_WIDTH + 80);
        for (Title title : Title.values()) {
            Card card = new Card(COLOR);
            card.setOnAction(() -> {
                game.chooseTitle(title);
                refresh();
            });
            titleCards.put(title, card);
            titles.add(card);
        }
        wearLatest.setStyle(BUTTON_STYLE);
        wearLatest.setFocusTraversable(false);
        wearLatest.setOnAction(event -> {
            game.chooseTitle(null);
            refresh();
        });
        Labels.note(titlesNote, MAX_WIDTH);
        return new VBox(10, header, heading("Avatar"), gallery, importButton, importNote, heading("Titres"), titlesNote,
                wearLatest, titles);
    }

    /** Le sous-onglet « Journal » : les dernières annonces, recomposées seulement quand une arrive. */
    private VBox journal() {
        journalNote.setStyle(NAME_STYLE);
        journalNote.setWrapText(true);
        journalNote.setMaxWidth(MAX_WIDTH);
        journalList.setMaxWidth(MAX_WIDTH);
        return new VBox(6, heading("Les " + EventLog.CAPACITY + " dernières annonces"), journalNote, journalList);
    }

    private List<AlbumPane.Entry> moleculeEntries() {
        List<AlbumPane.Entry> entries = new ArrayList<>();
        for (Molecule molecule : game.molecules()) entries.add(new AlbumPane.Entry(molecule.id(), molecule.name(), molecule.formula(), molecule.kind()));
        return entries;
    }

    private List<AlbumPane.Entry> substanceEntries() {
        List<AlbumPane.Entry> entries = new ArrayList<>();
        for (Molecule molecule : game.molecules()) {
            if (!molecule.hasState()) continue;
            entries.add(new AlbumPane.Entry(molecule.id(), molecule.name(),
                    MatterText.sign(molecule.state()) + " " + molecule.state().label(), null));
        }
        return entries;
    }

    private List<AlbumPane.Entry> assemblyEntries() {
        List<AlbumPane.Entry> entries = new ArrayList<>();
        for (Assembly assembly : game.assemblies()) entries.add(new AlbumPane.Entry(assembly.id(), assembly.name(), assembly.family().label(), null));
        return entries;
    }

    private List<AlbumPane.Entry> bodyEntries() {
        List<AlbumPane.Entry> entries = new ArrayList<>();
        for (Body body : game.bodies()) entries.add(new AlbumPane.Entry(body.id(), body.name(), body.tier().label(), null));
        return entries;
    }

    // ------------------------------------------------------------------
    // Les sous-onglets
    // ------------------------------------------------------------------

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    /** Montre ce sous-onglet ; un album pas encore ouvert ramène au profil. */
    public void select(int index) {
        selected = index >= 0 && index < NAMES.length && isOpen(index) ? index : PROFILE;
        for (int i = 0; i < NAMES.length; i++) {
            pages[i].setVisible(i == selected);
            tabs[i].setStyle(SubTabs.style(SubTabs.NARROW_STYLE, i == selected, i >= MOLECULES ? GameApp.BIG_BANG_COLOR : COLOR));
        }
    }

    public int selected() {
        return selected;
    }

    /** Passe au sous-onglet ouvert suivant ou précédent, en boucle. */
    public void step(int direction) {
        int index = selected;
        for (int tries = 0; tries < NAMES.length; tries++) {
            index = Math.floorMod(index + direction, NAMES.length);
            if (isOpen(index)) break;
        }
        select(index);
        refresh();
    }

    /** Vrai si ce sous-onglet existe dans la partie : le profil et le journal toujours, un album avec le Big Bang ou sa première découverte. */
    private boolean isOpen(int index) {
        return switch (index) {
            case MOLECULES -> game.isBigBangUnlocked() || !game.state().foundMolecules().isEmpty();
            case SUBSTANCES -> game.isBigBangUnlocked() || !game.state().foundSubstances().isEmpty();
            case ASSEMBLIES -> game.isBigBangUnlocked() || !game.state().foundAssemblies().isEmpty();
            case BODIES -> game.isBigBangUnlocked() || !game.state().foundBodies().isEmpty();
            default -> true;
        };
    }

    /** Les noms des sous-onglets ouverts, dans l'ordre : pour les vérifications. */
    public List<String> openSubPages() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) if (isOpen(i)) names.add(NAMES[i]);
        return names;
    }

    /** Ouvre le sous-onglet de ce nom ; faux s'il n'existe pas ou n'est pas ouvert : pour les vérifications. */
    public boolean openSubPage(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (!NAMES[i].equals(name) || !isOpen(i)) continue;
            select(i);
            refresh();
            return true;
        }
        return false;
    }

    /** Vrai si un titre est tombé depuis la dernière visite du profil : la pastille de l'onglet. */
    public boolean hasNews() {
        int earned = game.state().earnedTitles().size();
        if (seenTitles < 0) seenTitles = earned;
        return earned > seenTitles;
    }

    /** La partie vient d'être remplacée ou remise à zéro : la page oublie ce qu'elle en avait retenu. */
    public void forget() {
        seenTitles = -1;
        galleryKey = "";
        titlesKey = "";
        journalVersion = -1;
        imagePath = null;
        image = null;
        nameField.setText(game.state().playerName());
        for (AlbumPane album : albums) album.forget();
    }

    /** Recopie le sous-onglet affiché, et montre les sous-onglets ouverts. */
    public void refresh() {
        for (int i = 0; i < NAMES.length; i++) {
            boolean open = isOpen(i);
            tabs[i].setVisible(open);
            tabs[i].setManaged(open);
        }
        if (!isOpen(selected)) select(PROFILE);
        tabs[PROFILE].setText(NAMES[PROFILE] + Readiness.dot(selected != PROFILE && hasNews()));
        switch (selected) {
            case PROFILE -> refreshProfile();
            case JOURNAL -> refreshJournal();
            default -> {
                albumIntros[selected - MOLECULES].setText(ALBUM_INTROS[selected - MOLECULES] + Detail.only(ALBUM_DETAIL));
                albums[selected - MOLECULES].refresh();
            }
        }
    }

    // ------------------------------------------------------------------
    // Le profil
    // ------------------------------------------------------------------

    private void refreshProfile() {
        seenTitles = game.state().earnedTitles().size();
        // Le joueur écrit : son texte n'est pas recopié par-dessus.
        if (!nameField.isFocused() && !nameField.getText().equals(game.state().playerName())) nameField.setText(game.state().playerName());
        Title worn = game.currentTitle();
        titleLabel.setText(worn == null ? "Pas encore de titre" : "« " + worn.label() + " »");
        playedLabel.setText("Temps de jeu : " + Format.duration(game.state().timePlayed()));
        universesLabel.setText("Univers formés : " + Format.whole(game.universes()));
        bangsLabel.setText("Big Bangs : " + Format.whole(game.bigBangs()));
        titlesLabel.setText("Titres obtenus : " + game.state().earnedTitles().size() + " / " + Title.values().length);
        Image custom = customImage();
        String avatar = wornAvatar(custom);
        Avatar drawn = Avatar.of(avatar);
        if (drawn == null) avatarView.show(custom);
        else avatarView.show(drawn);

        String key = avatar + "/" + game.state().earnedTitles().size() + "/" + (custom != null) + "/" + Detail.shown() + "/" + Card.signsShown()
                + "/" + (folder.get() != null);
        if (!key.equals(galleryKey)) {
            galleryKey = key;
            for (Map.Entry<Avatar, Card> each : avatarCards.entrySet()) {
                Avatar shown = each.getKey();
                boolean open = shown.isOpen(game);
                boolean wearing = open && shown.id().equals(avatar);
                each.getValue().show(!open ? Card.State.LOCKED : wearing ? Card.State.ON : Card.State.READY, "",
                        wearing ? "porté" : "", shown.label(),
                        open ? "" : "avec le titre « " + shown.unlock().label() + " »",
                        open ? "" : shown.unlock().condition() + ".", "", "");
            }
            customPreview.show(custom);
            boolean wearing = Avatar.CUSTOM.equals(avatar);
            customCard.show(custom == null ? Card.State.LOCKED : wearing ? Card.State.ON : Card.State.READY, "", wearing ? "porté" : "",
                    "Votre image", custom == null ? "à choisir ci-dessous" : "importée",
                    "Une image PNG, JPG, GIF ou BMP : elle est copiée dans le dossier du jeu, à côté de la sauvegarde, et "
                            + "recadrée en rond.", "", "");
            importButton.setDisable(folder.get() == null);
            importButton.setText(folder.get() == null ? "Jeu lancé sans sauvegarde : pas d'image importée" : "Choisir une image…");
        }

        key = game.state().chosenTitle() + "/" + game.state().earnedTitles().size() + "/" + Detail.shown() + "/" + Card.signsShown();
        if (!key.equals(titlesKey)) {
            titlesKey = key;
            int number = 0;
            for (Map.Entry<Title, Card> each : titleCards.entrySet()) {
                Title title = each.getKey();
                number++;
                boolean earned = game.hasTitle(title);
                boolean wearing = title == worn;
                each.getValue().show(!earned ? Card.State.LOCKED : wearing ? Card.State.ON : Card.State.READY, "n° " + number,
                        wearing ? "porté" : "", title.label(), title.condition(), "",
                        earned ? "obtenu à " + Format.duration(game.titleEarnedAt(title)) + " de jeu" : "à venir", "");
            }
            boolean chosen = !game.state().chosenTitle().isEmpty() && worn != null;
            wearLatest.setDisable(!chosen);
            wearLatest.setVisible(worn != null);
            wearLatest.setManaged(worn != null);
            titlesNote.setText(worn == null ? "Le premier titre tombe avec le premier générateur. Un titre ne donne aucun bonus : il dit jusqu'où vous êtes allé."
                    : "Un clic sur un titre obtenu le porte." + (chosen ? "" : " Sans choix, vous portez le plus récent.")
                            + Detail.only(" Le plus récent est le dernier de la liste parmi ceux obtenus. Un titre ne donne aucun bonus ; "
                                    + "il reste acquis pour de bon, Big Bang, univers et « Recommencer de zéro » compris."));
        }
    }

    /**
     * L'avatar porté, tel qu'il se montre : l'image importée si elle se lit encore, l'avatar choisi s'il est ouvert, et
     * sinon (rien de choisi, fichier disparu, avatar verrouillé ou inconnu) l'avatar par défaut, sans erreur.
     */
    private String wornAvatar(Image custom) {
        String id = game.state().avatar();
        if (Avatar.CUSTOM.equals(id) && custom != null) return Avatar.CUSTOM;
        Avatar chosen = Avatar.of(id);
        return (chosen != null && chosen.isOpen(game) ? chosen : Avatar.PARTICLE).id();
    }

    private void commitName() {
        game.state().setPlayerName(nameField.getText());
        if (!nameField.getText().equals(game.state().playerName())) nameField.setText(game.state().playerName());
    }

    /** L'image importée, s'il y en a une qui se lit dans le dossier des données ; {@code null} sinon. */
    private Image customImage() {
        Path file = customFile();
        if (file == null) {
            imagePath = null;
            image = null;
            return null;
        }
        long stamp;
        try {
            stamp = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
        if (!file.equals(imagePath) || stamp != imageStamp) {
            imagePath = file;
            imageStamp = stamp;
            image = load(file);
        }
        return image;
    }

    private Path customFile() {
        Path dir = folder.get();
        if (dir == null) return null;
        for (String extension : STORED_EXTENSIONS) {
            Path file = dir.resolve(AVATAR_FILE + "." + extension);
            if (Files.isRegularFile(file)) return file;
        }
        return null;
    }

    /** Lit une image, réduite à {@link #IMAGE_SIDE} ; {@code null} si elle ne se lit pas. */
    private static Image load(Path file) {
        try {
            Image read = new Image(file.toUri().toString(), IMAGE_SIDE, IMAGE_SIDE, true, true);
            return read.isError() || read.getWidth() <= 0 ? null : read;
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    /** Le bouton « Choisir une image… » : le sélecteur de fichiers, puis la copie. */
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image pour l'avatar");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images (PNG, JPG, GIF, BMP)",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.PNG", "*.JPG", "*.JPEG", "*.GIF", "*.BMP"));
        javafx.stage.Window owner = getScene() == null ? null : getScene().getWindow();
        java.io.File file = chooser.showOpenDialog(owner);
        if (file != null) importAvatar(file.toPath());
    }

    /**
     * Copie une image dans le dossier des données, sous {@code avatar.png} (ou .jpg, .gif, .bmp), et la porte. L'image
     * d'avant est effacée. Rien ne change si le fichier n'est pas une image lisible.
     *
     * @return la phrase qui dit ce qui s'est passé, affichée sous le bouton
     */
    public String importAvatar(Path source) {
        Path dir = folder.get();
        if (dir == null) return say("Jeu lancé sans sauvegarde : l'image ne peut pas être gardée.");
        String name = source.getFileName() == null ? "" : source.getFileName().toString();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (extension.equals("jpeg")) extension = "jpg";
        if (!STORED_EXTENSIONS.contains(extension)) return say("Seules les images PNG, JPG, GIF et BMP sont acceptées.");
        try {
            if (Files.size(source) > MAX_IMAGE_BYTES) return say("Cette image est trop lourde : " + (MAX_IMAGE_BYTES >> 20) + " Mo au plus.");
            if (load(source) == null) return say("Ce fichier ne se lit pas comme une image.");
            Files.createDirectories(dir);
            Path target = dir.resolve(AVATAR_FILE + "." + extension);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            for (String other : STORED_EXTENSIONS) {
                if (!other.equals(extension)) Files.deleteIfExists(dir.resolve(AVATAR_FILE + "." + other));
            }
        } catch (IOException | RuntimeException failure) {
            return say("L'image n'a pas pu être copiée : " + failure.getMessage());
        }
        imagePath = null;
        game.state().setAvatar(Avatar.CUSTOM);
        refresh();
        return say("Image importée : elle est rangée dans le dossier du jeu, à côté de la sauvegarde.");
    }

    private String say(String text) {
        importNote.setText(text);
        importNote.setVisible(true);
        importNote.setManaged(true);
        return text;
    }

    // ------------------------------------------------------------------
    // Le journal
    // ------------------------------------------------------------------

    private void refreshJournal() {
        EventLog log = EventLog.of(game);
        if (log.version() == journalVersion) return;
        journalVersion = log.version();
        journalNote.setText(log.size() == 0
                ? "Rien pour l'instant. Tout ce que le jeu annonce (palier, succès, titre, explosion, amélioration acquise) "
                        + "viendra se noter ici, même si les notifications sont coupées."
                : "La plus récente en haut. Le journal dure le temps d'un lancement : il n'est pas sauvegardé.");
        journalList.getChildren().clear();
        for (EventLog.Entry entry : log.entries()) {
            Label when = new Label(entry.clock() + "  ·  " + Format.duration(entry.played()) + " de jeu");
            when.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
            when.setMinWidth(170);
            Label text = new Label(entry.text());
            text.setStyle(NAME_STYLE);
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox line = new HBox(10, when, text);
            line.setStyle(ROW_STYLE);
            line.setMaxWidth(MAX_WIDTH);
            journalList.getChildren().add(line);
        }
    }

    // ------------------------------------------------------------------
    // Pour les vérifications
    // ------------------------------------------------------------------

    /** Ouvre le journal et le recopie. */
    public void openJournal() {
        select(JOURNAL);
        refresh();
    }

    /** Nombre d'annonces du journal affichées. */
    public int journalRows() {
        return journalList.getChildren().size();
    }

    /** Le texte d'une ligne du journal, la plus récente d'abord : « 12:00  ·  3 s de jeu | texte ». */
    public String journalRow(int index) {
        HBox box = (HBox) journalList.getChildren().get(index);
        return ((Label) box.getChildren().get(0)).getText() + " | " + ((Label) box.getChildren().get(1)).getText();
    }

    /** Le champ du nom. */
    public TextField nameField() {
        return nameField;
    }

    /** Les lignes de l'en-tête : le titre porté, le temps de jeu, les univers, les Big Bangs, les titres. */
    public List<String> headerLines() {
        return List.of(titleLabel.getText(), playedLabel.getText(), universesLabel.getText(), bangsLabel.getText(), titlesLabel.getText());
    }

    /** Ce que montre l'avatar de l'en-tête : l'identifiant d'un avatar du jeu, ou {@link Avatar#CUSTOM}. */
    public String shownAvatar() {
        return avatarView.shown();
    }

    /** La carte d'un avatar de la galerie. */
    public Card avatarCard(Avatar avatar) {
        return avatarCards.get(avatar);
    }

    /** La carte de l'image importée. */
    public Card customCard() {
        return customCard;
    }

    /** Le bouton qui ouvre le sélecteur de fichiers. */
    public Button importButton() {
        return importButton;
    }

    /** La carte d'un titre. */
    public Card titleCard(Title title) {
        return titleCards.get(title);
    }

    /** Le bouton « Porter le plus récent ». */
    public Button wearLatestButton() {
        return wearLatest;
    }

    /** Le compte d'un album ({@link #MOLECULES} à {@link #BODIES}), « 3 / 120 », après l'avoir recopié. */
    public String albumCounter(int index) {
        albums[index - MOLECULES].refresh();
        return albums[index - MOLECULES].counterText();
    }

    /** Le texte des cases affichées d'un album. */
    public List<String> albumTexts(int index) {
        albums[index - MOLECULES].refresh();
        return albums[index - MOLECULES].shownTexts();
    }

    /** Le texte de la case de cet identifiant dans un album. */
    public String albumText(int index, String id) {
        albums[index - MOLECULES].refresh();
        return albums[index - MOLECULES].textOf(id);
    }

    /** Ne montre que les molécules d'un rayon ; {@code null} les montre toutes. */
    public void filterMolecules(Molecule.Kind kind) {
        albums[0].filter(kind);
    }
}
