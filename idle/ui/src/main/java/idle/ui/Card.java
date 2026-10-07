package idle.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
 */
final class Card extends VBox {

    /** Ce que la carte dit de son contenu, et donc sa couleur. */
    enum State {
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
    /** Vrai si chaque carte écrit le signe de son état devant son nom ({@link #sign(State)}) : un réglage, commun à toutes. */
    private static boolean signs = true;

    /** Affiche ou cache le signe d'état devant le nom des cartes ; les cartes le prennent à leur prochain remplissage. */
    static void setSigns(boolean shown) {
        signs = shown;
    }

    /**
     * Le signe d'un état, pour le reconnaître sans la couleur : un rond plein pour ce qui est à
     * portée, un rond vide pour ce qui attend, un demi-rond pour ce qui est entamé, une flèche
     * pour ce qui est en marche, un carré vide pour ce qui est coupé, une coche pour ce qui est
     * acquis, une croix pour ce qui est fermé, un point d'exclamation pour ce qui attend un second clic.
     */
    static String sign(State state) {
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
    /** Un dessin posé entre l'en-tête et le titre, ou {@code null}. */
    private javafx.scene.Node picture = null;
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
    Card(String accent) {
        super(3);
        this.accent = accent;
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
            paint();
        });
        setOnMouseExited(event -> {
            hovered = false;
            if (held == this) held = null;
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
    void setPicture(javafx.scene.Node picture) {
        if (this.picture != null) getChildren().remove(this.picture);
        this.picture = picture;
        if (picture != null) getChildren().add(1, picture);
        painted = 0;
        paint();
    }

    /** Ce que fait un clic sur la carte, quand son état le permet. */
    void setOnAction(Runnable action) {
        this.action = action;
    }

    /** Une carte acquise reste cliquable : pour ce qui se refait, comme un défi déjà réussi. */
    void setDoneClickable(boolean doneClickable) {
        this.doneClickable = doneClickable;
    }

    /**
     * Tenir le clic sur la carte répète son action, plusieurs fois par seconde, tant qu'elle reste à
     * portée : pour ce qui s'achète en grand nombre. Un clic simple agit toujours une seule fois.
     */
    void setRepeats(boolean repeats) {
        this.repeats = repeats;
        if (!repeats && held == this) held = null;
    }

    /** Vrai si tenir le clic répète l'action : pour les vérifications. */
    boolean repeats() {
        return repeats;
    }

    /**
     * Fait passer le temps du clic tenu : après un court délai, l'action de la carte tenue se
     * répète. À appeler à chaque image.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     * @return le nombre de fois où l'action a été répétée pendant cette image
     */
    static int repeatHeld(double elapsed) {
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
    static Card held() {
        return held;
    }

    /**
     * Donne une épingle à la carte, en haut à droite : un clic dessus appelle {@code toggle}, sans
     * cliquer la carte. {@code null} la retire.
     */
    void setPin(Runnable toggle) {
        pinAction = toggle;
        pin.setVisible(toggle != null);
        pin.setManaged(toggle != null);
        showPin(pinned);
    }

    /** Montre l'épingle pleine (épinglée) ou creuse. */
    void showPin(boolean pinned) {
        this.pinned = pinned;
        pin.setText(pinned ? "★" : "☆");
        pin.setStyle("-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 2; -fx-text-fill: " + (pinned ? "#ffd27f" : "#8fa3b8") + ";");
    }

    /** Vrai si l'épingle est pleine : pour les vérifications. */
    boolean pinned() {
        return pinned;
    }

    /** Clique sur l'épingle : pour les vérifications. */
    void clickPin() {
        if (pinAction != null) pinAction.run();
    }

    /** Clique sur la carte : sans effet si elle n'est ni à portée ni un interrupteur. */
    void click() {
        if (isClickable()) action.run();
    }

    /** Vrai si un clic fait quelque chose : la carte a une action, et elle est à portée ou c'est un interrupteur. */
    boolean isClickable() {
        if (action == null) return false;
        return state == State.READY || state == State.ON || state == State.OFF || state == State.ARMED
                || (state == State.DONE && doneClickable);
    }

    /**
     * Le texte qui apparaît quand la souris s'arrête sur la carte ; vide ou {@code null}, il n'y
     * en a pas.
     */
    void setHint(String text) {
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
    String hint() {
        return tooltip == null ? "" : tooltip.getText();
    }

    State state() {
        return state;
    }

    /**
     * Pour une liste de cartes dont les textes coûtent à composer : l'appelant résume en un nombre
     * tout ce dont dépend le contenu de la carte, et demande ici si elle a déjà été remplie avec ce
     * même nombre. Si oui, il n'a rien à recomposer.
     *
     * @return vrai si la clé est celle du dernier appel ; sinon elle est retenue
     */
    boolean filledWith(long key) {
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
    void show(State state, String corner, String mark, String title, String line, String detail, String price, String aside) {
        this.state = state;
        set(this.corner, corner);
        set(this.mark, mark);
        set(this.title, signs && !empty(title) ? sign(state) + " " + title : title);
        set(this.line, line);
        set(this.detail, Detail.shown() ? detail : "");
        set(this.price, price);
        set(this.aside, aside);
        visible(head, !empty(corner) || !empty(mark) || pinAction != null);
        visible(foot, !empty(price) || !empty(aside));
        paint();
    }

    /** Tout le texte affiché, ligne par ligne : pour les vérifications. */
    String text() {
        StringBuilder text = new StringBuilder();
        for (Label label : new Label[] {corner, mark, title, line, detail, price, aside}) {
            if (label.isVisible() && !empty(label.getText())) text.append(text.length() == 0 ? "" : " | ").append(label.getText());
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
        // Rien n'a changé depuis le dernier passage : les styles sont déjà les bons.
        int look = 1 + 4 * state.ordinal() + (lit ? 2 : 0) + (isClickable() ? 1 : 0);
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
        setStyle("-fx-background-color: " + background + "; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-border-color: " + border + ";" + (state == State.LOCKED ? " -fx-border-style: dashed;" : "")
                + (isClickable() ? " -fx-cursor: hand;" : ""));
        corner.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + quiet + ";");
        mark.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 6; -fx-background-radius: 3;"
                + " -fx-border-radius: 3; -fx-text-fill: " + quiet + "; -fx-border-color: " + quiet + "66;");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + name + ";");
        line.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ink + ";");
        boolean solid = state == State.DONE || state == State.ARMED;
        detail.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (solid ? quiet : "#9fb0c2") + ";");
        price.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                + (state == State.READY ? "#ffffff" : solid ? name : quiet) + ";");
        aside.setStyle("-fx-font-size: 11px; -fx-text-fill: " + quiet + ";");
    }
}
