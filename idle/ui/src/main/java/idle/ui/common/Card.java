package idle.ui.common;

import idle.core.universe.Skin;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/**
 * Une carte cliquable, dessinée comme une case du tableau périodique : c'est le bouton de tout
 * ce qui s'achète ou s'active dans le jeu.
 *
 * <pre>
 *   12                    V      le niveau, comme un numéro atomique ; à droite, la touche ou l'état
 *   Vitesse de création          le nom
 *   +10 % de vitesse             ce que cela fait, en quelques mots
 *   (explication complète)       seulement tant que la touche de détail est tenue
 *   456 particules      42 s     tout en bas, le prix, comme une masse atomique ; à droite, l'attente
 * </pre>
 *
 * <p>Ses états reprennent ceux des cases du tableau : pleine couleur une fois au maximum,
 * teintée quand elle est à portée, entamée ou en marche, éteinte sinon. Une ligne vide ne prend
 * pas de place.
 *
 * <p>Sans action ({@link #setOnAction}), c'est une simple tuile : elle montre, elle ne se clique
 * pas. C'est ainsi que sont dessinés les succès et les ensembles d'éléments.
 *
 * <p>Version 0.190 : les cartes ont un skin, commun à toutes ({@link #setSkin}), pris à leur prochain remplissage.
 * Fiche cartonnée : un fond crème, des textes sombres en police à chasse fixe, le coin haut droit écorné. Tuile de
 * verre : un fond à peine blanchi, coins ronds, et à portée un bord lumineux de la couleur d'accent avec son halo.
 * Carte à jouer : un fond blanc à marge, des textes sombres, le coin répété en bas à droite à l'envers, et un dos
 * rouge hachuré quand elle est fermée. Étiquette : une forme à coins coupés en haut, un œillet et son fil en haut à
 * gauche, et un léger penchement sous la souris. Dans chaque skin, les états se reconnaissent encore : leur couleur
 * d'accent reste sur le bord et le prix, et leur signe devant le nom.
 */
public final class Card extends VBox {

    /** Ce que la carte dit de son contenu, et donc sa couleur. */
    public enum State {
        /** À portée : un clic l'achète ou l'active. */
        READY,
        /** Disponible, mais il manque de quoi la payer. */
        WAITING,
        /** Déjà entamée (un niveau au moins, ou une partie du chemin), mais la suite est hors de portée. */
        STARTED,
        /** Possédée et en marche. */
        ON,
        /** Possédée et coupée : un clic la remet en marche. */
        OFF,
        /** Acquise ou au maximum : il n'y a plus rien à acheter. */
        DONE,
        /** Pas encore accessible. */
        LOCKED,
        /** Un premier clic a été donné sur une action qui efface quelque chose : le second la confirme. */
        ARMED
    }

    private static final String DARK = "#10151f";
    /** Le skin des cartes (version 0.190), commun à toutes, ou {@code null} pour le Classique ; lu à chaque repeinture. */
    private static volatile Skin skin = null;
    /** La taille du coin écorné de la fiche cartonnée, en pixels. */
    private static final double DOG_EAR = 14;
    /** Le penchement d'une étiquette sous la souris, en degrés ; une carte sur deux penche de l'autre côté. */
    private static final double TAG_TILT = 2;
    /** La place laissée à l'œillet de l'étiquette, à gauche. */
    private static final double TAG_ROOM = 14;
    /** Les cartes construites, pour alterner le côté du penchement. */
    private static int built = 0;

    /** Change le skin de toutes les cartes ; chacune le prend à son prochain remplissage, ou sous la souris. */
    public static void setSkin(Skin chosen) {
        skin = chosen;
    }

    /** Le skin des cartes, ou {@code null} pour le Classique : pour les vérifications. */
    public static Skin skin() {
        return skin;
    }
    /** Vrai si chaque carte écrit le signe de son état devant son nom ({@link #sign(State)}) : un réglage, commun à toutes. */
    private static boolean signs = true;

    /** Affiche ou cache le signe d'état devant le nom des cartes ; les cartes le prennent à leur prochain remplissage. */
    public static void setSigns(boolean shown) {
        signs = shown;
    }

    /** Vrai si les cartes écrivent le signe de leur état : pour qui mémorise ce qu'il leur a donné à montrer. */
    public static boolean signsShown() {
        return signs;
    }

    /** Le skin des signes d'état (version 0.192), commun à toutes les cartes, ou {@code null} pour les signes de toujours. */
    private static volatile Skin signSkin = null;

    /** Change la famille des signes d'état de toutes les cartes ; chacune la prend à son prochain remplissage. */
    public static void setSignSkin(Skin chosen) {
        signSkin = chosen;
    }

    /** Le skin des signes d'état, ou {@code null} pour les signes de toujours : pour les vérifications. */
    public static Skin signSkin() {
        return signSkin;
    }

    /**
     * Le signe d'un état, pour le reconnaître sans la couleur : un rond plein pour ce qui est à
     * portée, un rond vide pour ce qui attend, un demi-rond pour ce qui est entamé, une flèche
     * pour ce qui est en marche, un carré vide pour ce qui est coupé, une coche pour ce qui est
     * acquis, une croix pour ce qui est fermé, un point d'exclamation pour ce qui attend un second clic.
     *
     * <p>Version 0.192 : le skin des signes en change la famille, dans le même ordre. « Runes » : le soleil (à portée),
     * la glace (en attente), l'année (entamée), le voyage (en marche), la grêle (coupée), l'héritage (acquise), le besoin
     * (fermée), l'épine (second clic). « Formes pleines » : triangle haut, triangle bas, demi-disque, flèche pleine, carré
     * plein, étoile, croix pleine, losange. « Lettres » : P, A, E, M, C, F, V, A.
     */
    public static String sign(State state) {
        Skin skin = signSkin;
        if (skin == Skin.SIGNS_RUNES) {
            return switch (state) {
                case READY -> "ᛊ";
                case WAITING -> "ᛁ";
                case STARTED -> "ᛃ";
                case ON -> "ᚱ";
                case OFF -> "ᚺ";
                case DONE -> "ᛟ";
                case LOCKED -> "ᚾ";
                case ARMED -> "ᚦ";
            };
        }
        if (skin == Skin.SIGNS_SHAPES) {
            return switch (state) {
                case READY -> "▲";
                case WAITING -> "▼";
                case STARTED -> "◖";
                case ON -> "►";
                case OFF -> "■";
                case DONE -> "★";
                case LOCKED -> "✖";
                case ARMED -> "◆";
            };
        }
        if (skin == Skin.SIGNS_LETTERS) {
            return switch (state) {
                case READY -> "P";
                case WAITING -> "A";
                case STARTED -> "E";
                case ON -> "M";
                case OFF -> "C";
                case DONE -> "F";
                case LOCKED -> "V";
                case ARMED -> "A";
            };
        }
        return switch (state) {
            case READY -> "●";
            case WAITING -> "○";
            case STARTED -> "◐";
            case ON -> "▶";
            case OFF -> "□";
            case DONE -> "✓";
            case LOCKED -> "×";
            case ARMED -> "!";
        };
    }

    private final String accent;
    private final Label corner = new Label();
    private final Label mark = new Label();
    private final Label title = new Label();
    private final Label line = new Label();
    private final Label detail = new Label();
    private final Label price = new Label();
    private final Label aside = new Label();
    private final HBox head;
    private final HBox foot;
    /** Ce que fait un clic, ou {@code null} pour une tuile qui ne se clique pas. */
    private Runnable action = null;
    private State state = State.WAITING;
    private boolean hovered = false;
    /** Vrai si la carte se clique encore une fois acquise (un défi réussi se rejoue). */
    private boolean doneClickable = false;
    private Tooltip tooltip = null;
    /** Un dessin posé entre l'en-tête et le titre, ou au début de l'en-tête ({@link #setInlinePicture}), ou {@code null}. */
    private javafx.scene.Node picture = null;
    /** Vrai si le dessin est dans l'en-tête, à gauche du coin, plutôt que sur sa propre ligne. */
    private boolean inlinePicture = false;
    /** L'apparence déjà appliquée (état, survol, cliquable), pour ne pas la recalculer à chaque image. */
    private int painted = 0;
    /** La clé du dernier remplissage ({@link #filledWith(long)}). */
    private long filled = Long.MIN_VALUE;
    /** Vrai si tenir le clic répète l'action ({@link #setRepeats(boolean)}). */
    private boolean repeats = false;
    /** L'épingle en haut de la carte : ce que fait un clic dessus, ou {@code null} pour une carte sans épingle. */
    private Runnable pinAction = null;
    private final Label pin = new Label();
    private boolean pinned = false;
    /** Les ornements des skins (version 0.190), faits à la première demande puis gardés : rien n'est alloué par image. */
    private Polygon dogEar = null;
    private Label mirror = null;
    private Circle eyelet = null;
    private Line thread = null;
    /** Le côté où penche cette étiquette sous la souris. */
    private final double tilt;
    /** Les marges d'avant l'étiquette, à rendre quand elle s'en va ; {@code null} hors de ce skin. */
    private Insets beforeTag = null;

    // Le clic tenu : une seule carte à la fois peut l'être, celle sous la souris.
    /** Délai avant la première répétition, puis entre deux répétitions, en secondes. */
    private static final double REPEAT_DELAY = 0.4;
    private static final double REPEAT_EVERY = 0.1;
    private static Card held = null;
    private static double heldFor = 0;
    private static double nextRepeat = 0;
    /** Vrai si le clic en cours a déjà répété l'action : son relâchement ne compte alors pas pour un clic de plus. */
    private static boolean heldRepeated = false;

    /** @param accent couleur de la carte, celle de son onglet ou de sa branche (« #9fd0ff ») */
    public Card(String accent) {
        super(3);
        this.accent = accent;
        tilt = (built++ % 2 == 0) ? TAG_TILT : -TAG_TILT;
        setPadding(new Insets(8, 12, 9, 12));
        setMinWidth(0);
        setAlignment(Pos.TOP_LEFT);

        head = new HBox(6, corner, spacer(), pin, mark);
        pin.setVisible(false);
        pin.setManaged(false);
        pin.setMinWidth(Region.USE_PREF_SIZE);
        // Un clic sur l'épingle épingle ; il ne va pas jusqu'à la carte.
        pin.setOnMouseClicked(event -> {
            event.consume();
            if (pinAction != null) pinAction.run();
        });
        pin.setOnMousePressed(event -> event.consume());
        head.setAlignment(Pos.CENTER_LEFT);
        foot = new HBox(6, price, spacer(), aside);
        foot.setAlignment(Pos.CENTER_LEFT);
        for (Label label : new Label[] {title, line, detail, price}) {
            label.setWrapText(true);
            label.setMinWidth(0);
        }
        // Dans une carte étroite, c'est le prix qui passe à la ligne : le niveau, la marque et
        // l'attente, eux, ne se laissent jamais rogner.
        HBox.setHgrow(price, Priority.SOMETIMES);
        for (Label label : new Label[] {corner, mark, aside}) label.setMinWidth(Region.USE_PREF_SIZE);
        // Une carte plus haute que son contenu (ses voisines de rangée ont plus à dire) garde son
        // prix tout en bas, comme la masse atomique d'une case.
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        getChildren().add(head);
        getChildren().add(title);
        getChildren().add(line);
        getChildren().add(detail);
        getChildren().add(gap);
        getChildren().add(foot);

        setOnMouseClicked(event -> {
            // Le relâchement d'un clic tenu qui a déjà répété l'action n'en ajoute pas une.
            boolean repeated = heldRepeated;
            heldRepeated = false;
            if (!repeated) click();
        });
        setOnMousePressed(event -> {
            heldRepeated = false;
            if (!repeats || !isClickable()) return;
            held = this;
            heldFor = 0;
            nextRepeat = REPEAT_DELAY;
        });
        setOnMouseReleased(event -> {
            if (held == this) held = null;
        });
        setOnMouseEntered(event -> {
            hovered = true;
            // L'étiquette penche sous la souris, cliquable ou non : le penchement ne passe pas par la repeinture.
            setRotate(skin == Skin.CARD_TAG ? tilt : 0);
            paint();
        });
        setOnMouseExited(event -> {
            hovered = false;
            if (held == this) held = null;
            setRotate(0);
            paint();
        });
        paint();
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Pose un dessin en haut de la carte, entre l'en-tête et le titre : la molécule d'une carte de
     * molécule. Il s'estompe avec la carte quand elle est hors de portée.
     */
    public void setPicture(javafx.scene.Node picture) {
        removePicture();
        this.picture = picture;
        if (picture != null) getChildren().add(1, picture);
        painted = 0;
        paint();
    }

    /**
     * Pose un petit dessin au début de l'en-tête, à gauche du coin, sur la même ligne que lui : la
     * carte reste basse (version 0.178, les cartes des molécules). Il s'estompe comme l'autre.
     */
    public void setInlinePicture(javafx.scene.Node picture) {
        removePicture();
        this.picture = picture;
        inlinePicture = picture != null;
        if (picture != null) head.getChildren().add(0, picture);
        painted = 0;
        paint();
    }

    private void removePicture() {
        if (picture == null) return;
        getChildren().remove(picture);
        head.getChildren().remove(picture);
        picture = null;
        inlinePicture = false;
    }

    private Runnable look;

    /** Ce que fait un clic sur la carte, quand son état le permet. */
    public void setOnAction(Runnable action) {
        this.action = action;
    }

    /** Une carte acquise reste cliquable : pour ce qui se refait, comme un défi déjà réussi. */
    public void setDoneClickable(boolean doneClickable) {
        this.doneClickable = doneClickable;
    }

    /**
     * Tenir le clic sur la carte répète son action, plusieurs fois par seconde, tant qu'elle reste à
     * portée : pour ce qui s'achète en grand nombre. Un clic simple agit toujours une seule fois.
     */
    public void setRepeats(boolean repeats) {
        this.repeats = repeats;
        if (!repeats && held == this) held = null;
    }

    /** Vrai si tenir le clic répète l'action : pour les vérifications. */
    public boolean repeats() {
        return repeats;
    }

    /**
     * Fait passer le temps du clic tenu : après un court délai, l'action de la carte tenue se
     * répète. À appeler à chaque image.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     * @return le nombre de fois où l'action a été répétée pendant cette image
     */
    public static int repeatHeld(double elapsed) {
        if (held == null) return 0;
        heldFor += elapsed;
        int repeated = 0;
        // Une image très longue ne déclenche pas une rafale : cinq répétitions au plus.
        while (held != null && heldFor >= nextRepeat && repeated < 5) {
            if (!held.repeats || !held.isClickable()) {
                held = null;
                break;
            }
            held.action.run();
            heldRepeated = true;
            nextRepeat += REPEAT_EVERY;
            repeated++;
        }
        return repeated;
    }

    /** La carte dont le clic est tenu en ce moment, ou {@code null} : pour les vérifications. */
    public static Card held() {
        return held;
    }

    /**
     * Donne une épingle à la carte, en haut à droite : un clic dessus appelle {@code toggle}, sans
     * cliquer la carte. {@code null} la retire.
     */
    public void setPin(Runnable toggle) {
        pinAction = toggle;
        pin.setVisible(toggle != null);
        pin.setManaged(toggle != null);
        showPin(pinned);
    }

    /** Montre l'épingle pleine (épinglée) ou creuse. */
    public void showPin(boolean pinned) {
        this.pinned = pinned;
        pin.setText(pinned ? "★" : "☆");
        pin.setStyle("-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 2; -fx-text-fill: " + (pinned ? "#ffd27f" : "#8fa3b8") + ";");
    }

    /** Vrai si l'épingle est pleine : pour les vérifications. */
    public boolean pinned() {
        return pinned;
    }

    /** Clique sur l'épingle : pour les vérifications. */
    public void clickPin() {
        if (pinAction != null) pinAction.run();
    }

    /** Clique sur la carte : sans effet si elle n'est ni à portée ni un interrupteur, sauf ce que {@link #setOnLook} demande. */
    public void click() {
        if (look != null) look.run();
        if (isClickable()) action.run();
    }

    /**
     * Ce que fait un clic, quel que soit l'état de la carte, avant son action (version 0.084) : montrer
     * quelque chose, sans rien acheter. {@code null} : rien.
     */
    public void setOnLook(Runnable look) {
        this.look = look;
    }

    /** Vrai si un clic fait quelque chose : la carte a une action, et elle est à portée ou c'est un interrupteur. */
    public boolean isClickable() {
        if (action == null) return false;
        return state == State.READY || state == State.ON || state == State.OFF || state == State.ARMED
                || (state == State.DONE && doneClickable);
    }

    /**
     * Le texte qui apparaît quand la souris s'arrête sur la carte ; vide ou {@code null}, il n'y
     * en a pas.
     */
    public void setHint(String text) {
        if (empty(text)) {
            if (tooltip != null) Tooltip.uninstall(this, tooltip);
            tooltip = null;
            return;
        }
        if (tooltip == null) {
            tooltip = new Tooltip();
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(340);
            tooltip.setShowDelay(Duration.millis(200));
            tooltip.setShowDuration(Duration.seconds(30));
            tooltip.setStyle("-fx-font-size: 12px; -fx-padding: 8 10; -fx-text-fill: #e8f4ff;"
                    + " -fx-background-color: #16202e; -fx-background-radius: 6; -fx-border-radius: 6;"
                    + " -fx-border-color: " + accent + ";");
            Tooltip.install(this, tooltip);
        }
        if (!text.equals(tooltip.getText())) tooltip.setText(text);
    }

    /** Le texte du survol, ou une chaîne vide : pour les vérifications. */
    public String hint() {
        return tooltip == null ? "" : tooltip.getText();
    }

    public State state() {
        return state;
    }

    /**
     * Pour une liste de cartes dont les textes coûtent à composer : l'appelant résume en un nombre
     * tout ce dont dépend le contenu de la carte, et demande ici si elle a déjà été remplie avec ce
     * même nombre. Si oui, il n'a rien à recomposer.
     *
     * @return vrai si la clé est celle du dernier appel ; sinon elle est retenue
     */
    public boolean filledWith(long key) {
        if (filled == key) return true;
        filled = key;
        return false;
    }

    /**
     * Remplit la carte. Toute chaîne vide ou {@code null} fait disparaître sa ligne.
     *
     * @param corner en haut à gauche : le niveau, ou un rang
     * @param mark   en haut à droite : la touche du clavier, ou l'état (« en marche », « max »)
     * @param title  le nom
     * @param line   ce que cela fait, en quelques mots
     * @param detail l'explication complète, affichée seulement en mode détails
     * @param price  le prix, ou ce qu'il faut pour y accéder
     * @param aside  à droite du prix : l'attente
     */
    public void show(State state, String corner, String mark, String title, String line, String detail, String price, String aside) {
        this.state = state;
        set(this.corner, corner);
        set(this.mark, mark);
        set(this.title, signs && !empty(title) ? sign(state) + " " + title : title);
        set(this.line, line);
        set(this.detail, Detail.shown() ? detail : "");
        set(this.price, price);
        set(this.aside, aside);
        visible(head, !empty(corner) || !empty(mark) || pinAction != null || inlinePicture);
        // La carte à jouer répète son coin en bas à droite, à l'envers ; les autres skins n'ont pas ce second coin.
        boolean mirrored = skin == Skin.CARD_PLAYING && !empty(corner);
        if (mirrored || mirror != null) set(mirrorLabel(), mirrored ? corner : "");
        visible(foot, !empty(price) || !empty(aside) || mirrored);
        paint();
    }

    /** Le second coin de la carte à jouer, tourné d'un demi-tour, au bout du pied : fait à la première demande. */
    private Label mirrorLabel() {
        if (mirror == null) {
            mirror = new Label();
            mirror.setRotate(180);
            mirror.setMinWidth(Region.USE_PREF_SIZE);
            foot.getChildren().add(mirror);
        }
        return mirror;
    }

    /** Tout le texte affiché, ligne par ligne, le second coin de la carte à jouer compris : pour les vérifications. */
    public String text() {
        StringBuilder text = new StringBuilder();
        for (Label label : new Label[] {corner, mark, title, line, detail, price, aside, mirror}) {
            if (label != null && label.isVisible() && !empty(label.getText())) text.append(text.length() == 0 ? "" : " | ").append(label.getText());
        }
        return text.toString();
    }

    private static boolean empty(String text) {
        return text == null || text.isEmpty();
    }

    private static void set(Label label, String text) {
        label.setText(empty(text) ? "" : text);
        visible(label, !empty(text));
    }

    private static void visible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** Colore la carte d'après son état, et un peu plus fort sous la souris quand elle est cliquable. */
    private void paint() {
        boolean lit = hovered && isClickable();
        Skin skin = Card.skin;
        // Rien n'a changé depuis le dernier passage (le skin compris) : les styles sont déjà les bons.
        int look = 1 + 4 * state.ordinal() + (lit ? 2 : 0) + (isClickable() ? 1 : 0) + 64 * (skin == null ? 0 : skin.ordinal() + 1);
        if (look == painted) return;
        painted = look;
        // Le fond, la bordure, puis les encres : le nom, le texte courant et le texte discret.
        String background;
        String border;
        String name;
        String ink;
        String quiet;
        switch (state) {
            case DONE -> {
                background = accent;
                border = "#ffffff";
                name = DARK;
                ink = DARK;
                quiet = "#10151fbb";
            }
            case READY -> {
                background = accent + (lit ? "55" : "33");
                border = lit ? "#ffffff" : accent;
                name = "#ffffff";
                ink = "#e8f4ff";
                quiet = accent;
            }
            case ON -> {
                background = accent + (lit ? "44" : "2b");
                border = lit ? "#ffffff" : accent;
                name = accent;
                ink = "#e8f4ff";
                quiet = accent;
            }
            case OFF -> {
                background = lit ? "#1a2432" : "#121923";
                border = lit ? accent : "#3a4a5e";
                name = "#c2cedb";
                ink = "#8fa3b8";
                quiet = "#8fa3b8";
            }
            case STARTED -> {
                background = accent + "1c";
                border = accent + "88";
                name = accent;
                ink = "#d3deea";
                quiet = accent + "cc";
            }
            case WAITING -> {
                background = "#121923";
                border = accent + "66";
                name = accent;
                ink = "#b7c4d2";
                quiet = "#8fa3b8";
            }
            case ARMED -> {
                background = lit ? "#ffa494" : "#ff8a75";
                border = "#ffffff";
                name = "#1a0a08";
                ink = "#1a0a08";
                quiet = "#1a0a08cc";
            }
            default -> {
                background = "#0e131b";
                border = accent + "33";
                name = accent + "99";
                ink = "#6f8296";
                quiet = "#5d6e80";
            }
        }
        if (picture != null) picture.setOpacity(state == State.LOCKED ? 0.35 : state == State.WAITING ? 0.7 : 1);
        boolean solid = state == State.DONE || state == State.ARMED;
        // Le style de la carte elle-même, la police de ses textes, et l'encre du prix : ceux de toujours sans skin.
        String box;
        String font = "";
        String priceInk = state == State.READY ? "#ffffff" : solid ? name : quiet;
        String faint = solid ? quiet : "#9fb0c2";
        String dashed = state == State.LOCKED ? " -fx-border-style: dashed;" : "";
        String hand = isClickable() ? " -fx-cursor: hand;" : "";
        if (skin == null) {
            box = "-fx-background-color: " + background + "; -fx-background-radius: 6; -fx-border-radius: 6;"
                    + " -fx-border-color: " + border + ";" + dashed + hand;
        } else {
            switch (skin) {
                case CARD_INDEX -> {
                    font = " -fx-font-family: Monospaced;";
                    boolean dim = state == State.LOCKED;
                    background = state == State.DONE ? accent : state == State.ARMED ? "#ff8a75" : dim ? "#d8cfbb" : lit ? "#fff8e8eb" : "#f3ead6eb";
                    name = dim ? "#8a7f6c" : "#2a2218";
                    ink = dim ? "#8a7f6c" : "#3a3022";
                    quiet = dim ? "#9a8f7c" : "#6b5a42";
                    faint = quiet;
                    priceInk = state == State.READY || solid ? "#2a2218" : quiet;
                    border = state == State.READY ? (lit ? "#2a2218" : accent) : solid ? "#2a2218" : state == State.OFF ? "#8a7a60" : border;
                    box = "-fx-background-color: " + background + "; -fx-background-radius: 2; -fx-border-radius: 2;"
                            + " -fx-border-color: " + border + ";" + dashed + hand;
                }
                case CARD_GLASS -> {
                    background = state == State.DONE ? alpha(accent, "55") : state == State.ON ? alpha(accent, "33")
                            : state == State.ARMED ? "#ff8a75cc" : state == State.LOCKED ? "rgba(255, 255, 255, 0.03)"
                            : lit ? "rgba(255, 255, 255, 0.12)" : "rgba(255, 255, 255, 0.06)";
                    if (state == State.DONE) {
                        name = "#ffffff";
                        ink = "#e8f4ff";
                        quiet = "#e8f4ffbb";
                        priceInk = "#ffffff";
                        faint = quiet;
                    }
                    boolean ready = state == State.READY;
                    border = ready ? (lit ? "#ffffff" : alpha(accent, "e6")) : border;
                    box = "-fx-background-color: " + background + "; -fx-background-radius: 10; -fx-border-radius: 10;"
                            + " -fx-border-color: " + border + ";" + dashed + hand
                            + (ready ? " -fx-effect: dropshadow(gaussian, " + alpha(accent, "aa") + ", 14, 0.3, 0, 0);" : " -fx-effect: null;");
                }
                case CARD_PLAYING -> {
                    boolean back = state == State.LOCKED;
                    String inner = switch (state) {
                        case READY -> alpha(accent, lit ? "66" : "44");
                        case ON -> alpha(accent, "55");
                        case STARTED -> alpha(accent, "22");
                        case OFF -> "#e4e4de";
                        case DONE -> accent;
                        case ARMED -> "#ff8a75";
                        case LOCKED -> "linear-gradient(from 0px 0px to 6px 6px, repeat, #9b2226 0%, #9b2226 50%, #7a1a1d 50%, #7a1a1d 100%)";
                        default -> "#eeeee8";
                    };
                    name = back ? "#f3dcdc" : "#1c1c1c";
                    ink = back ? "#e8c8c8" : "#1c1c1c";
                    quiet = back ? "#e8c8c8aa" : "#55554f";
                    faint = quiet;
                    priceInk = back ? ink : state == State.READY || solid ? "#1c1c1c" : quiet;
                    border = state == State.READY ? (lit ? "#1c1c1c" : accent) : back ? "#5a1013" : solid ? "#1c1c1c"
                            : state == State.OFF ? "#b0b0a8" : border;
                    box = "-fx-background-color: #f8f8f4, " + inner + "; -fx-background-insets: 0, 5; -fx-background-radius: 8, 4;"
                            + " -fx-border-color: " + border + "; -fx-border-insets: 5; -fx-border-radius: 4;" + hand;
                }
                case CARD_TAG -> {
                    boolean dim = state == State.LOCKED;
                    background = switch (state) {
                        case DONE -> accent;
                        case ARMED -> "#ff8a75";
                        case LOCKED -> "#4a443a";
                        case READY -> lit ? "#f0e4c8" : "#e4d6b4";
                        case ON -> "#e4d6b4";
                        case STARTED -> "#d9cba8";
                        case OFF -> "#bfb399";
                        default -> "#cdbf9e";
                    };
                    name = dim ? "#9a9282" : "#2b2416";
                    ink = dim ? "#8a8272" : "#3d3425";
                    quiet = dim ? "#7a7262" : "#6a5d44";
                    faint = quiet;
                    priceInk = state == State.READY || solid ? "#2b2416" : quiet;
                    border = state == State.READY ? (lit ? "#2b2416" : accent) : solid ? "#2b2416" : state == State.OFF ? "#8a7a60" : border;
                    box = "-fx-shape: \"M 0 14 L 14 0 L 86 0 L 100 14 L 100 100 L 0 100 Z\"; -fx-background-color: " + background + ";"
                            + " -fx-border-color: " + border + ";" + dashed + hand;
                }
                default -> box = "";     // un skin d'un autre visuel ne peut pas arriver ici : {@code chosenSkin(CARD)} ne rend que les siens
            }
        }
        dress(skin);
        setStyle(box);
        corner.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + quiet + ";" + font);
        if (mirror != null) mirror.setStyle(corner.getStyle());
        mark.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 6; -fx-background-radius: 3;"
                + " -fx-border-radius: 3; -fx-text-fill: " + quiet + "; -fx-border-color: " + alpha(quiet, "66") + ";" + font);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + name + ";" + font);
        line.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ink + ";" + font);
        detail.setStyle("-fx-font-size: 11px; -fx-text-fill: " + faint + ";" + font);
        price.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + priceInk + ";" + font);
        aside.setStyle("-fx-font-size: 11px; -fx-text-fill: " + quiet + ";" + font);
    }

    /**
     * Pose ou retire les ornements d'un skin : le coin écorné de la fiche, l'œillet et le fil de l'étiquette, la place
     * qu'ils demandent et le penchement sous la souris. Chaque ornement est fait une fois, puis seulement montré ou caché.
     */
    private void dress(Skin skin) {
        boolean index = skin == Skin.CARD_INDEX;
        if (index && dogEar == null) {
            dogEar = new Polygon(0, 0, DOG_EAR, 0, DOG_EAR, DOG_EAR);
            dogEar.setFill(Color.web("#2a2218"));
            dogEar.setManaged(false);
            dogEar.setMouseTransparent(true);
            getChildren().add(dogEar);
        }
        if (dogEar != null) dogEar.setVisible(index);
        boolean tag = skin == Skin.CARD_TAG;
        if (tag && eyelet == null) {
            eyelet = new Circle(4.5, Color.web(DARK));
            eyelet.setStroke(Color.web("#8a7a5a"));
            eyelet.setStrokeWidth(1.5);
            eyelet.setManaged(false);
            eyelet.setMouseTransparent(true);
            thread = new Line(0, 0, 0, 22);
            thread.setStroke(Color.web("#c9b58a"));
            thread.setStrokeWidth(1.2);
            thread.setManaged(false);
            thread.setMouseTransparent(true);
            getChildren().add(thread);
            getChildren().add(eyelet);
        }
        if (eyelet != null) {
            eyelet.setVisible(tag);
            thread.setVisible(tag);
        }
        // L'étiquette laisse la place de l'œillet à gauche, et rend les marges d'avant en s'en allant.
        if (tag && beforeTag == null) {
            beforeTag = getPadding();
            setPadding(new Insets(beforeTag.getTop(), beforeTag.getRight(), beforeTag.getBottom(), beforeTag.getLeft() + TAG_ROOM));
        } else if (!tag && beforeTag != null) {
            setPadding(beforeTag);
            beforeTag = null;
        }
        setRotate(tag && hovered ? tilt : 0);
    }

    /** Les ornements, hors du rang des lignes, se placent à la main : le coin écorné en haut à droite, l'œillet en haut à gauche. */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (dogEar != null && dogEar.isVisible()) dogEar.relocate(getWidth() - DOG_EAR, 0);
        if (eyelet != null && eyelet.isVisible()) {
            eyelet.relocate(7.75, 6.75);
            thread.relocate(12.4, -14);
        }
    }

    /**
     * Une couleur {@code #rrggbb} avec cette transparence en deux chiffres ; une couleur qui en a déjà
     * une ({@code #rrggbbaa}) la remplace, sans quoi JavaFX refuse la couleur (version 0.090).
     */
    static String alpha(String color, String alpha) {
        String base = color.length() > 7 ? color.substring(0, 7) : color;
        return base + alpha;
    }
}
