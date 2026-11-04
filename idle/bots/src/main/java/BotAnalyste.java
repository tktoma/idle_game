import idle.core.history.Photo;
import idle.core.history.PurchaseHistory;
import idle.core.history.Noms;
import idle.core.history.Achats;
import idle.core.Game;
import idle.core.GameState;
import idle.core.GameUniverse.TreeBubble;
import idle.core.Multipliers;
import idle.core.bigbang.BigBangCondition;
import idle.core.save.SaveCodec;
import idle.core.sky.Cosmos;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Le bot analyste (version 0.161) : il joue comme le meilleur profil (le Speedrunner), mais à vitesse humaine, une
 * décision toutes les cinq secondes, et note tout, à la seconde de jeu près :
 * <ul>
 *   <li>chaque achat : quoi, combien de niveaux, ce qu'il a coûté et dans quelle monnaie, la part de la réserve que
 *       cela représentait, son poids sur chaque grandeur du jeu (particules, atomes, matière noire, espace…) et le temps
 *       qu'il fait gagner selon l'estimation du moment ;</li>
 *   <li>chaque chose obtenue sans payer (succès, titres, défis réussis), chaque automatisme mis en marche ou coupé ;</li>
 *   <li>chaque palier : les trois actes de chaque univers (première explosion, premier Big Bang, univers formé) et leurs
 *       étapes (huitième Big Bang, galaxie, amas), les paliers de la graine, les dimensions, leurs seuils et leurs paliers
 *       finals (version 0.168), le défi final
 *       et la fin ; avec une sauvegarde à chaque palier ;</li>
 *   <li>une estimation régulière du temps jusqu'au prochain palier, en jouant la suite sur une copie de la partie, et
 *       le goulot du moment.</li>
 * </ul>
 * Si rien n'avance (plus de 24 h estimées avant le prochain palier, ou 30 minutes sans palier, achat ni dépense), il
 * s'arrête et dit pourquoi. Tout s'écrit en texte brut, en CSV et en une page HTML ({@link AnalyseRapport}).
 *
 * <p>Version 0.162 : il dit aussi pourquoi. Chaque achat a sa rentabilité (le temps de production qu'il a coûté, contre
 * le temps qu'il fait gagner) et le rapport classe tout ce qui a été acheté. Chaque défi lancé dit sa contrainte, sa
 * récompense, pourquoi maintenant et combien de temps il prendra (joué sur une copie) ; chaque succès, ce qu'il
 * demandait, ce qu'il apporte et son poids ; chaque automatisme, ce qu'il fait ; chaque explosion, la matière noire
 * qu'elle rapporte par heure. À chaque palier principal, « et si » : la suite jouée sur des copies par un joueur qui
 * passe une fois par minute, puis toutes les dix minutes.
 *
 * <p>Version 0.163 : il va jusqu'à la fin de la partie. Après l'univers 11, il continue de former des univers (l'énergie
 * du vide paie les bulles), prend les bulles du sommet (l'automatisation des dimensions) et des racines (le défi final),
 * laisse les automatismes jouer les dimensions, et s'arrête quand les dix paliers du défi final sont atteints ; le défi
 * ouvert, il estime le palier suivant du défi. Il peut aussi repartir d'une sauvegarde (un palier d'une partie
 * d'avant, par exemple).
 *
 * <p>Usage : {@code BotAnalyste <dossier> [univers=fin] [heures=400] [graine=201] [pas=5] [estimation_min=30] [comparer=false]
 * [etsi=true] [depart=sauvegarde] [sans=id,id] [profil=speedrunner] [ordre]} ; {@code univers} est un nombre d'univers, ou {@code fin} pour jouer
 * jusqu'à la fin. Version 0.164 : {@code sans} retire ces améliorations du jeu (identifiants des améliorations ordinaires
 * ou de l'arbre de matière noire) ; avec {@link AnalyseComparaison}, on voit ce que chacune apporte. À la fin, les
 * détecteurs d'illogismes ({@link AnalyseIllogismes}) relisent la partie.
 * Avec {@code comparer=true}, il compare aussi six façons de jouer à chaque palier principal et garde la plus rapide
 * ({@link #choisirStrategie()}) : six parties d'essai par palier, la partie dure plusieurs fois plus longtemps. Avec
 * {@code etsi=false}, il ne joue pas les « et si » : la partie se calcule environ deux fois plus vite.
 *
 * <p>Version 0.166 : {@code profil} choisit le joueur : le Speedrunner (par défaut), le Régulier (une décision par minute,
 * moins attentif), ou l'absent (le Speedrunner, cinq minutes par heure, le jeu fermé entre deux). Chaque palier dit sa
 * cause (la dernière chose faite dans la minute d'avant ; pour un Big Bang, sa dernière condition remplie), le temps réel,
 * en ligne et hors ligne ; le suivi continu ({@link AnalyseSuivi}) mesure les améliorations des dimensions et de la graine
 * sur leur propre palier et nourrit deux détecteurs de plus ; decisions.csv compte les décisions par heure.
 *
 * <p>Version 0.168 : à chaque palier, la mémoire du bot et le hasard de la partie s'écrivent à côté de la sauvegarde
 * ({@link AnalyseMemoire}) ; repartie de cette sauvegarde, la tranche suivante continue la même partie. {@code sans}
 * retire aussi des bulles de l'arbre ({@code bulle:LEAVES_LEFT}), et {@code ordre} impose l'ordre des bulles à tous les
 * profils ({@code meilleur} ou {@code moins-cher}). Les achats faits au pas d'une remise à zéro se notent, le palier final
 * de chaque dimension aussi, et l'attente d'une seule condition de la galaxie dit laquelle.
 */
public final class BotAnalyste extends IdleProfiles24 {

    /** Le Speedrunner, à vitesse humaine : une décision toutes les cinq secondes. */
    static final Profile ANALYSTE = new Profile(1, "Analyste", "Le meilleur jeu connu, à vitesse humaine", 5, 0, 0, 1, 1, 1, 1, 1, 0, "doubleplan");
    /**
     * Version 0.166 : le joueur absent. Il joue comme le Speedrunner, mais ne passe que cinq minutes par heure ; le jeu
     * est fermé entre deux visites et compte l'absence au taux hors ligne. Il garde le numéro du Speedrunner pour en
     * garder les choix (styles des univers, du vide, de l'arbre).
     */
    static final Profile ABSENT = new Profile(1, "Absent", "Une visite de 5 minutes toutes les heures, le jeu fermé entre deux", 5, 3600, 300,
            1, 1, 1, 1, 1, 0, "doubleplan");

    /** Le profil d'un nom (version 0.166) : speedrunner (par défaut), regulier (le Régulier des profils), absent. */
    static Profile profil(String nom) {
        return switch (nom == null ? "" : nom.trim().toLowerCase(Locale.ROOT)) {
            case "", "speedrunner", "analyste" -> ANALYSTE;
            case "regulier", "régulier", "regular" -> IdleProfiles24.PROFILES[2];
            case "absent" -> ABSENT;
            default -> throw new IllegalArgumentException("Profil inconnu : " + nom + " (speedrunner, regulier, absent)");
        };
    }
    /** Le pas des parties d'essai : plus long, pour estimer vite. */
    static final double ESSAI_PAS = 10;
    /** Au-delà, l'estimation abandonne : la partie est bloquée. */
    static final double HORIZON = 24 * 3600;
    /** Sans palier, achat ni dépense pendant ce temps, la partie est bloquée. */
    static final double IMMOBILE = 30 * 60;
    /**
     * Version 0.170 : la cadence des estimations suit le palier. Une estimation qui annonce le palier dans {@code d} fait
     * attendre la suivante {@code d × PART_ESTIMATION} au moins (jamais moins que {@code estimationEvery}). Un palier à
     * 20 h se mesure ainsi cinq ou six fois (à 0, 10, 15, 17 h 30…) au lieu de quarante, et les copies jouées pour
     * l'estimer pèsent au plus deux fois le temps du palier au lieu de vingt fois. La partie elle-même ne change pas :
     * une estimation ne décide rien, elle ne fait qu'annoncer et mesurer (vérifié : sauvegarde et mémoire du bot identiques).
     */
    static final double PART_ESTIMATION = 0.5;

    final AnalyseJournal journal = new AnalyseJournal();
    /** Version 0.162 : la façon de jouer du moment, et si le bot les compare à chaque palier principal. */
    AnalyseStrategies.Strategie strategie = AnalyseStrategies.TOUTES.get(0);
    boolean comparer = false;
    /** Version 0.164 : les améliorations retirées du jeu (leurs identifiants), pour mesurer ce que chacune apporte. */
    java.util.Set<String> sans = java.util.Set.of();
    /** Version 0.168 : la mémoire du bot écrite à côté de la sauvegarde de départ ({@link AnalyseMemoire}), vide sans. */
    java.util.Map<String, byte[]> memoireReprise = java.util.Map.of();
    /** Version 0.163 : jouer jusqu'à la fin de la partie (les dix paliers du défi final), plutôt qu'à un nombre d'univers. */
    boolean jusquALaFin = false;
    /** Version 0.162 : les « et si » à chaque palier principal (deux parties d'essai de plus par palier). */
    boolean etsi = true;
    /** Les rythmes des « et si » : un joueur qui passe une fois par minute, puis toutes les dix minutes. */
    static final double[] RYTHMES_ETSI = {60, 600};
    /**
     * Le pas des copies des « et si » : une minute. Un joueur qui ne passe qu'une fois par minute au plus ne décide pas
     * plus souvent, et la copie joue six fois plus vite qu'au pas de {@value #ESSAI_PAS} s.
     */
    static final double ETSI_PAS = 60;
    /** L'horizon d'une comparaison : au-delà, une façon de jouer n'arrive pas au palier. */
    static final double HORIZON_CHOIX = 12 * 3600;
    final Path dossier;
    final long graine;
    final double estimationEvery;
    String blocage;

    private Photo avant;
    private Map<Multipliers.Quantity, Double> grandeursAvant;
    private int indexPrincipal, seedStage, dimensions, finalTiers, bigBangs;
    private final boolean[] seuils = new boolean[Game.MAX_DIMENSIONS + 1];
    // Version 0.168 : le palier final de chaque dimension, noté à part (avant, le journal n'en gardait que le nombre).
    private final boolean[] finals = new boolean[Game.MAX_DIMENSIONS + 1];
    private boolean fini;
    /** Le dernier palier, et la dernière activité (un achat, une chose obtenue, une dépense). */
    private double dernierPalier, derniereActivite, prochaineEstimation, estimationCourante = Double.NaN;
    /** Le moment que la dernière estimation annonçait pour le palier visé (temps de jeu), et ce palier. */
    private double annonceAbsolue = Double.NaN;
    /**
     * Version 0.162 : le même moment, raccourci de ce que chaque achat a fait gagner depuis l'estimation. Chaque achat
     * part de ce qui reste après les précédents : les gains d'une même attente ne dépassent jamais l'attente.
     */
    private double annonceAjustee = Double.NaN;
    /** Le moment de la dernière estimation (version 0.163). */
    private double derniereEstimation = Double.NEGATIVE_INFINITY;
    private String annonceVisee = "";
    /** Ce qui bouge dans la partie, même sans décision : les compteurs et les grandeurs ; changé, la partie avance. */
    private String signature = "";
    private String visee = "";
    private final Map<String, Double> grandeursLog = new LinkedHashMap<>();
    /** Les relevés des grandeurs, une fois par minute de jeu : de quoi tracer les courbes du rapport. */
    final List<double[]> releves = new ArrayList<>();
    double prochainReleve;
    /**
     * Version 0.162 : ce que rapporte chaque ressource par seconde (en ordres de grandeur), mesuré sur le dernier pas où
     * elle a monté sans décision ; la matière noire, sur la dernière explosion. De quoi dire ce qu'un achat a coûté en
     * temps de production.
     */
    final Map<String, Double> revenuLog = new LinkedHashMap<>();
    /** La part des succès dans chaque grandeur (en ordres de grandeur), au dernier succès noté. */
    private Map<Multipliers.Quantity, Double> partSucces;
    /** Les défis en cours : quand ils ont commencé, et le temps que la copie annonçait (NaN sans). */
    private final Map<String, double[]> defisEnCours = new LinkedHashMap<>();
    private double derniereExplosion;
    /** Le « et si » qui attend le temps réel de son palier, et l'indice de ce palier. */
    private AnalyseJournal.EtSi etsiEnAttente;
    /** Version 0.166 : le suivi continu (réserves, conditions, dimensions et graine). */
    final AnalyseSuivi suivi = new AnalyseSuivi();
    /**
     * Version 0.166 : le temps en ligne au dernier palier ou à la dernière activité. Le blocage se juge sur le temps où le
     * joueur est là : un joueur absent ne fait rien pendant 55 minutes, sans que rien ne soit bloqué.
     */
    private double ligneTouchee;
    /** Version 0.166 : la dernière chose faite (un achat, une chose obtenue, un défi), et quand : la cause d'un palier. */
    private String derniereAction = "";
    private double derniereActionT = Double.NEGATIVE_INFINITY;
    /** Le délai au-delà duquel un palier ne doit rien à la dernière action : il vient du temps qui passe. */
    static final double CAUSE_DELAI = 60;
    private int etsiCible = -1;

    BotAnalyste(Game game, long seed, double step, Path dossier, double estimationMinutes) {
        this(game, ANALYSTE, seed, step, dossier, estimationMinutes);
    }

    /** Version 0.166 : avec un autre profil que le Speedrunner ({@link #profil}). */
    BotAnalyste(Game game, Profile profile, long seed, double step, Path dossier, double estimationMinutes) {
        super(game, profile, seed, step);
        this.graine = seed;
        this.dossier = dossier;
        this.estimationEvery = estimationMinutes * 60;
        indexPrincipal = indexPrincipal(game);
        seedStage = game.seedStage();
        dimensions = game.dimensionsUnlocked();
        // Version 0.163 : une partie reprise d'une sauvegarde garde ce qu'elle avait déjà atteint ; seul le neuf est un palier.
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) seuils[d] = game.isDimensionThresholdReached(d);
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) finals[d] = game.isFinalTierReached(d);
        finalTiers = game.isFinalChallengeOpen() ? game.finalTiersReached() : 0;
        fini = game.isGameFinished();
        bigBangs = game.bigBangs();
        dernierPalier = game.state().timePlayed();
        derniereActivite = dernierPalier;
        prochainReleve = dernierPalier;
    }

    // ------------------------------------------------------------------
    // Les paliers
    // ------------------------------------------------------------------

    /** Les étapes d'un univers, dans l'ordre : de rien à l'amas ; l'univers formé compte pour l'étape 0 du suivant. */
    static final String[] ETAPES = {"début", "acte 1 : première explosion", "acte 2 : premier Big Bang", "dernier Big Bang préparé",
        "galaxie", "amas"};

    /** L'étape de l'univers en cours, de 0 à 6 (6 : formé). */
    static int etape(Game game) {
        int cosmos = RobotCosmosPlanner.cosmosStage(game);
        if (cosmos >= 3) return 6;
        if (cosmos == 2) return 5;
        if (cosmos == 1) return 4;
        if (game.bigBangs() >= Bots.LAST_BIG_BANG) return 3;
        if (game.bigBangs() >= 1) return 2;
        if (game.state().explosions() > 0) return 1;
        return 0;
    }

    /** L'avancée principale : 7 par univers d'avant, plus l'étape de l'univers en cours (0 à 6). Elle ne fait que monter. */
    static int indexPrincipal(Game game) {
        // L'univers en cours : celui qui vient de se former tant que le suivant n'est pas lancé.
        int universe = !game.isUniverseFormed() ? 1 : game.isReplaying() ? game.universes() + 1 : game.universes();
        return (universe - 1) * 7 + etape(game);
    }

    /** Le nom du palier d'indice {@code index}. */
    static String nomPalier(int index) {
        int universe = index / 7 + 1, e = index % 7;
        if (e == 6) return "Univers " + universe + " formé (acte 3)";
        if (e == 0) return universe == 1 ? "Début" : "Univers " + universe + " : début";
        return "Univers " + universe + " : " + ETAPES[e];
    }

    /**
     * Le numéro de l'univers en cours, lu sur les étapes : celui qui vient de se former tant que le suivant n'est pas
     * lancé (la dimension qu'il ouvre et les paliers de graine qu'il donne restent les siens).
     */
    int univers() {
        return indexPrincipal / 7 + 1;
    }

    private void palier(String nom) {
        palier(nom, univers());
    }

    /** Un palier de l'univers {@code u} (un univers formé reste le sien, même si le suivant est déjà lancé). */
    private void palier(String nom, int u) {
        double t = temps();
        double annonce = nom.equals(annonceVisee) ? annonceAbsolue : Double.NaN;
        String cause = cause(nom);
        journal.paliers.add(new AnalyseJournal.Palier(t, u, nom, t - dernierPalier, annonce, wallSeconds, onlineSeconds, creditedOfflineSeconds, cause));
        List<String> details = new ArrayList<>();
        details.add("Depuis le palier précédent : " + AnalyseJournal.duree(t - dernierPalier));
        details.add("Cause : " + cause);
        if (wallSeconds > t + 1) {
            details.add("Temps réel : " + AnalyseJournal.chrono(wallSeconds) + " ; en ligne : " + AnalyseJournal.duree(onlineSeconds)
                    + " ; hors ligne, compté au taux d'absence : " + AnalyseJournal.duree(creditedOfflineSeconds) + " de jeu");
        }
        if (!Double.isNaN(annonce)) {
            details.add("La dernière estimation l'annonçait vers " + AnalyseJournal.chrono(annonce) + " : écart de "
                    + (t >= annonce ? "+" : "−") + AnalyseJournal.duree(Math.abs(t - annonce)));
        }
        details.addAll(decomposition());
        journal.ajoute(t, u, AnalyseJournal.Sorte.PALIER, nom, details);
        dernierPalier = t;
        ligneTouchee = onlineSeconds;
        sauvegarde(nom);
        // Le journal s'écrit aussi à chaque palier : une partie interrompue garde ce qu'elle a vu.
        try {
            journal.ecrire(dossier);
        } catch (Exception failure) {
            // L'écriture finale réessaiera.
        }
    }

    /**
     * Vrai pour un joueur qui ferme le jeu entre deux visites (version 0.166) : une estimation au-delà de l'horizon n'est
     * pas un blocage pour lui, il avance surtout hors ligne.
     */
    boolean absences() {
        return p.cycleSeconds() > 0;
    }

    /**
     * Ce qui a débloqué un palier (version 0.166) : la dernière chose faite dans la minute d'avant, ou le temps qui passe ;
     * pour un Big Bang, la dernière de ses conditions à se remplir, et combien de temps elle a manqué seule.
     */
    private String cause(String nom) {
        double t = temps();
        String out = t - derniereActionT <= CAUSE_DELAI ? derniereAction : "le temps qui passe (rien de fait dans la minute d'avant)";
        if (nom.contains("Big Bang") && !nom.contains("dernier Big Bang")) {
            String derniere = suivi.derniereCondition(g);
            if (!derniere.isEmpty()) out = derniere + " ; " + out;
        }
        return out;
    }

    /**
     * D'où vient chaque grandeur du moment, aspect par aspect (« Améliorations 42 %, Succès 20 % ») : ce qui pèse le
     * plus au moment d'un palier.
     */
    List<String> decomposition() {
        List<String> out = new ArrayList<>();
        for (Multipliers.Quantity q : Multipliers.Quantity.values()) {
            if (!Multipliers.isAvailable(g, q)) continue;
            List<Multipliers.Share> shares = Multipliers.byAspect(Multipliers.of(g, q));
            if (shares.isEmpty()) continue;
            StringBuilder line = new StringBuilder(q.label()).append(String.format(Locale.FRANCE, " (10^%.1f) : ", Multipliers.total(g, q)));
            int n = 0;
            for (Multipliers.Share share : shares) {
                double part = Multipliers.shareOf(share, shares);
                if (share.log10() < 0) {
                    line.append(n++ > 0 ? ", " : "").append(share.aspect().label()).append(String.format(Locale.FRANCE, " ÷10^%.1f", -share.log10()));
                } else if (part >= 0.01) {
                    line.append(n++ > 0 ? ", " : "").append(share.aspect().label()).append(String.format(Locale.FRANCE, " %.0f %%", 100 * part));
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    /**
     * Le nom du fichier de sauvegarde d'un palier : son chrono et son nom, sans accents ni signes (selon le système,
     * « formé » ou « sphères » ne passent pas dans un nom de fichier).
     */
    static String nomSauvegarde(double t, String nom) {
        String ascii = java.text.Normalizer.normalize(nom, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return AnalyseJournal.chrono(t).replace(':', '-') + "_" + ascii.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "") + ".save";
    }

    private void sauvegarde(String nom) {
        try {
            Path saves = dossier.resolve("sauvegardes");
            Files.createDirectories(saves);
            Path save = saves.resolve(nomSauvegarde(temps(), nom));
            Files.writeString(save, SaveCodec.write(g.state(), 0));
            // Version 0.168 : la mémoire du bot et le hasard de la partie, à côté, pour reprendre la même partie.
            AnalyseMemoire.ecrire(this, save);
        } catch (Exception failure) {
            journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, "Sauvegarde impossible : " + failure);
        }
    }

    private void paliers() {
        int index = indexPrincipal(g);
        while (indexPrincipal < index) {
            indexPrincipal++;
            // Le début d'un univers n'est pas un palier : c'est le lancement, noté comme un choix.
            if (indexPrincipal % 7 != 0) palier(nomPalier(indexPrincipal), indexPrincipal / 7 + 1);
            if (etsiEnAttente != null && indexPrincipal >= etsiCible) {
                AnalyseJournal.EtSi e = etsiEnAttente;
                journal.etsi.add(new AnalyseJournal.EtSi(e.t(), e.univers(), e.depuis(), e.vise(), e.parMinute(), e.parDixMinutes(), temps() - e.t()));
                etsiEnAttente = null;
            }
            if (indexPrincipal == index) {
                choisirStrategie();
                etSi();
            }
            estimationCourante = Double.NaN;
            annonceAbsolue = Double.NaN;
            annonceAjustee = Double.NaN;
            prochaineEstimation = temps();
        }
        if (index < indexPrincipal) indexPrincipal = index;
        // Les Big Bangs entre le premier et le dernier : des paliers aussi, sans être des étapes de l'univers.
        if (g.bigBangs() > bigBangs) {
            if (g.bigBangs() > 1 && g.bigBangs() < Bots.LAST_BIG_BANG) palier("Univers " + univers() + " : Big Bang n° " + g.bigBangs());
            bigBangs = g.bigBangs();
        }
        if (g.bigBangs() < bigBangs) bigBangs = g.bigBangs();
        while (g.seedStage() > seedStage) {
            seedStage++;
            palier("Graine : palier " + seedStage);
        }
        if (g.dimensionsUnlocked() > dimensions) {
            dimensions = g.dimensionsUnlocked();
            // La dimension n s'ouvre quand l'univers n se forme : elle est rangée avec lui, même si le suivant est lancé.
            palier("Dimension " + dimensions + " ouverte : " + Game.dimensionResourceName(dimensions), Math.min(dimensions, univers()));
        }
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            if (!seuils[d] && g.isDimensionThresholdReached(d)) {
                seuils[d] = true;
                palier("Seuil de la dimension " + d + " atteint");
            }
        }
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            if (!finals[d] && g.isFinalTierReached(d)) {
                finals[d] = true;
                palier("Palier final de la dimension " + d + " atteint");
            }
        }
        if (g.isFinalChallengeOpen() && g.finalTiersReached() > finalTiers) {
            finalTiers = g.finalTiersReached();
            palier("Défi final : " + finalTiers + " palier(s) sur 10");
        }
        if (!fini && g.isGameFinished()) {
            fini = true;
            palier("Fin de la partie");
        }
    }

    // ------------------------------------------------------------------
    // Les décisions
    // ------------------------------------------------------------------

    double temps() {
        return g.state().timePlayed();
    }

    private Map<Multipliers.Quantity, Double> grandeurs() {
        Map<Multipliers.Quantity, Double> out = new EnumMap<>(Multipliers.Quantity.class);
        for (Multipliers.Quantity q : Multipliers.Quantity.values()) {
            if (!Multipliers.isAvailable(g, q)) continue;
            double v = Multipliers.total(g, q);
            if (Double.isFinite(v)) out.put(q, v);
        }
        return out;
    }

    @Override
    void beforeDecisions() {
        avant = Photo.of(g.state());
        grandeursAvant = grandeurs();
        // Ce que le temps a apporté depuis les décisions du pas d'avant : les succès, titres et défis tombent souvent là,
        // et ce que chaque ressource a rapporté pendant ce pas sans décision.
        if (precedent != null) {
            List<Photo.Changement> changes = Photo.diff(precedent, avant);
            revenus(changes);
            evenements(temps(), changes);
        }
    }

    /** Ce que chaque ressource a rapporté par seconde pendant le pas, quand elle a monté (une baisse : un automatisme a payé). */
    private void revenus(List<Photo.Changement> changes) {
        for (Photo.Changement c : changes) {
            if (c.sousCle() != null || !Noms.ressource(c.cle()) || c.cle().equals("darkMatter") || !c.monte()) continue;
            Photo.Nombre gained = c.apres().minus(c.avant());
            if (gained.signe() > 0) revenuLog.put(c.cle(), gained.log10() - Math.log10(step));
        }
    }

    /** La photo prise après les décisions du pas d'avant. */
    private Photo precedent;

    /**
     * Ce qui arrive sans être un achat, entre deux photos : succès, titres, défis lancés, réussis ou abandonnés,
     * automatismes mis en marche ou coupés, réglages. Noté que ce soit pendant le temps de jeu ou pendant les
     * décisions, et même au pas d'une explosion (un défi se réussit en explosant).
     */
    private void evenements(double t, List<Photo.Changement> changes) {
        List<String> succes = new ArrayList<>();
        List<Photo.Changement> automatismes = new ArrayList<>();
        // Dans l'ordre où le joueur les vit : le défi qui finit, puis celui qui commence, puis le reste.
        List<Photo.Changement> ordre = new ArrayList<>(changes);
        ordre.sort(java.util.Comparator.comparingInt(c -> !c.cle().startsWith("active") ? 2 : c.retire() != null ? 0 : 1));
        for (Photo.Changement c : ordre) {
            String key = c.cle();
            if (key.equals("achievements") && c.ajoute() != null) {
                succes.add(c.ajoute());
            } else if (key.equals("titles") && c.monte() && c.avant().signe() == 0) {
                journal.ajoute(t, univers(), AnalyseJournal.Sorte.SUCCES, "Titre obtenu : " + Noms.chose(c.sousCle()));
            } else if ((key.equals("activeChallenge") || key.equals("activeBangChallenge")) && c.ajoute() != null) {
                defiLance(t, c.ajoute());
            } else if ((key.equals("activeChallenge") || key.equals("activeBangChallenge")) && c.retire() != null) {
                defiFini(t, c.retire(), changes);
            } else if (Achats.REGLAGES.containsKey(key)) {
                String from = c.avant() != null ? c.avant().texte() : c.retire() != null ? Noms.chose(c.retire()) : "—";
                String to = c.apres() != null ? c.apres().texte() : c.ajoute() != null ? Noms.chose(c.ajoute()) : "—";
                journal.ajoute(t, univers(), AnalyseJournal.Sorte.CHOIX, "Réglage : " + Achats.REGLAGES.get(key) + (c.sousCle() != null && c.ajoute() == null && c.retire() == null
                        ? " (" + Noms.chose(c.sousCle()) + ")" : "") + " : " + from + " → " + to);
            } else if (key.equals("enabledAutomations") || key.equals("enabledDarkAutomations") || key.equals("dimensionAutomationsOff")) {
                automatismes.add(c);
            }
        }
        if (!automatismes.isEmpty()) automatismes(t, automatismes, cause(changes));
        if (!succes.isEmpty()) succes(t, succes);
    }

    /** Ce qui a remis la partie à zéro pendant ces changements ({@code null} : rien) : une explosion, un défi, un Big Bang… */
    static String cause(List<Photo.Changement> changes) {
        for (Photo.Changement c : changes) {
            switch (c.cle()) {
                case "explosions" -> { if (c.monte()) return "l'explosion"; }
                case "bigBangs" -> { if (c.monte()) return "le Big Bang"; }
                case "activeChallenge", "activeBangChallenge" -> { if (c.ajoute() != null) return "le début du défi"; }
                case "extraUniverses", "replaying" -> { if (c.monte()) return "l'univers suivant"; }
                default -> { }
            }
        }
        return null;
    }

    /** La part des succès dans chaque grandeur, en ordres de grandeur. */
    private Map<Multipliers.Quantity, Double> partSucces() {
        Map<Multipliers.Quantity, Double> out = new EnumMap<>(Multipliers.Quantity.class);
        for (Multipliers.Quantity q : Multipliers.Quantity.values()) {
            if (!Multipliers.isAvailable(g, q)) continue;
            double sum = 0;
            for (Multipliers.Share share : Multipliers.byAspect(Multipliers.of(g, q))) {
                if (share.aspect() == Multipliers.Aspect.ACHIEVEMENTS) sum += share.log10();
            }
            out.put(q, sum);
        }
        return out;
    }

    /** Des succès obtenus : ce qu'ils demandaient, ce qu'ils apportent, et leur poids ensemble sur chaque grandeur. */
    private void succes(double t, List<String> ids) {
        Map<Multipliers.Quantity, Double> now = partSucces();
        StringBuilder poids = new StringBuilder();
        if (partSucces != null) {
            for (Map.Entry<Multipliers.Quantity, Double> e : now.entrySet()) {
                double delta = e.getValue() - partSucces.getOrDefault(e.getKey(), 0.0);
                if (Math.abs(delta) < 1e-6) continue;
                if (poids.length() > 0) poids.append(" ; ");
                poids.append(e.getKey().label()).append(" ×").append(PurchaseHistory.facteur(delta));
            }
        }
        partSucces = now;
        for (String id : ids) {
            List<String> details = new ArrayList<>(AnalysePourquoi.succes(g, id));
            if (poids.length() > 0) details.add("Poids" + (ids.size() > 1 ? " des " + ids.size() + " succès de ce moment" : "") + " : " + poids);
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.SUCCES, "Succès obtenu : " + Noms.chose(id), details);
        }
    }

    /** Un défi lancé : sa contrainte, sa récompense, pourquoi maintenant, et le temps qu'il prendra, joué sur une copie. */
    private void defiLance(double t, String id) {
        List<String> details = new ArrayList<>(AnalysePourquoi.lancement(g, id));
        double duree = essai(game -> !id.equals(defiActif(game)), HORIZON, null, 0);
        boolean reussi = duree >= 0 && essaiReussi;
        if (duree < 0) details.add("Joué sur une copie : pas fini en " + AnalyseJournal.duree(HORIZON) + ".");
        else details.add("Joué sur une copie : " + (reussi ? "réussi" : "abandonné") + " en " + AnalyseJournal.duree(duree) + ".");
        defisEnCours.put(id, new double[] {t, reussi ? duree : Double.NaN});
        journal.ajoute(t, univers(), AnalyseJournal.Sorte.DEFI, "Défi lancé : " + Noms.chose(id), details);
    }

    /** Vrai si ce défi, d'explosion ou de Big Bang, est réussi dans cette partie. */
    private static boolean reussi(Game game, String id) {
        var bang = AnalysePourquoi.defiBigBang(id);
        return game.state().completedChallenges().contains(id) || bang != null && game.isBangChallengeCompleted(bang);
    }

    /** Le défi en cours, d'explosion ou de Big Bang ; {@code null} sans. */
    private static String defiActif(Game game) {
        if (game.activeChallenge() != null) return game.activeChallenge().id();
        return game.activeBangChallenge() == null ? null : game.activeBangChallenge().name();
    }

    /** Un défi qui n'est plus en cours : réussi (il est dans les défis réussis) ou abandonné. */
    private void defiFini(double t, String id, List<Photo.Changement> changes) {
        boolean reussi = reussi(g, id);
        double[] start = defisEnCours.remove(id);
        List<String> details = new ArrayList<>();
        if (start != null) {
            details.add("Commencé à " + AnalyseJournal.chrono(start[0]) + " : " + AnalyseJournal.duree(t - start[0]) + " de défi"
                    + (Double.isNaN(start[1]) ? "" : " (la copie annonçait " + AnalyseJournal.duree(start[1]) + ")") + ".");
        }
        if (reussi) {
            var c = AnalysePourquoi.defi(id);
            var b = AnalysePourquoi.defiBigBang(id);
            if (c != null) details.add("Récompense gagnée : " + AnalysePourquoi.recompense(c) + ".");
            if (b != null) details.add("Récompense gagnée : " + b.rewardText() + ".");
        }
        if (start != null) journal.defis.add(new AnalyseJournal.Defi(t, Noms.chose(id), t - start[0], reussi));
        journal.ajoute(t, univers(), AnalyseJournal.Sorte.DEFI, (reussi ? "Défi réussi : " : "Défi abandonné : ") + Noms.chose(id), details);
    }

    /** La grandeur qui fait avancer vers le prochain palier principal : celle dont l'achat raccourcit l'attente. */
    Multipliers.Quantity moteur() {
        return switch (etape(g)) {
            case 0 -> Multipliers.Quantity.PRODUCTION;
            case 1 -> Multipliers.Quantity.DARK_MATTER;
            default -> g.isUniverseFormed() && g.universes() >= 3 && Multipliers.isAvailable(g, Multipliers.Quantity.LINE_SHARDS)
                    ? Multipliers.Quantity.LINE_SHARDS : Multipliers.Quantity.SPACE;
        };
    }

    @Override
    boolean afterStep(boolean done) {
        int lignes = journal.lignes.size();
        if (avant != null) decisions();
        actionsDuPas(lignes);
        paliers();
        double t = temps();
        suivi.observe(g, t, univers(), journal);
        if (t + 1e-6 >= prochainReleve) {
            releve();
            prochainReleve = t + 60;
        }
        if (t + 1e-6 >= prochaineEstimation && blocage == null) {
            // Version 0.163 : pas deux fois de suite la même estimation (deux paliers dans la même minute).
            if (t - derniereEstimation >= 60 || !Objects.equals(viseeDuMoment(), annonceVisee)) {
                prochaineEstimation = t + estimationEvery;
                estimer();
                derniereEstimation = t;
            } else {
                prochaineEstimation = t + estimationEvery;
            }
        }
        String now = signature();
        if (!now.equals(signature)) {
            signature = now;
            derniereActivite = t;
            ligneTouchee = onlineSeconds;
        }
        if (blocage == null && onlineSeconds - ligneTouchee > IMMOBILE) {
            bloque("Plus rien ne bouge depuis " + AnalyseJournal.duree(onlineSeconds - ligneTouchee) + " de jeu en ligne"
                    + " : ni palier, ni achat, ni compteur (Big Bangs, astres, arrosages…), ni grandeur qui monte");
        }
        return done || blocage != null || jusquALaFin && fini;
    }

    /** Les compteurs de la partie et ses grandeurs (au centième d'ordre de grandeur) : si rien n'y change, rien n'avance. */
    private String signature() {
        StringBuilder sig = new StringBuilder();
        sig.append(g.bigBangs()).append('|').append(g.state().explosions()).append('|').append(g.bodiesFormed()).append('|').append(g.bodiesMature())
                .append('|').append(g.solarSystemsComplete()).append('|').append(RobotCosmosPlanner.cosmosStage(g)).append('|').append(g.universes())
                .append('|').append(g.state().seedWaterings()).append('|').append(g.achievementCount()).append('|').append(g.stats().fusions());
        for (Map.Entry<Multipliers.Quantity, Double> e : grandeurs().entrySet()) sig.append('|').append(Math.round(e.getValue() * 100));
        return sig.toString();
    }

    /** La dernière chose faite pendant le pas (un achat, une chose obtenue, un défi) : la cause possible du palier suivant. */
    private void actionsDuPas(int depuis) {
        for (int i = journal.lignes.size() - 1; i >= depuis; i--) {
            AnalyseJournal.Ligne l = journal.lignes.get(i);
            if (l.sorte() == AnalyseJournal.Sorte.ACHAT || l.sorte() == AnalyseJournal.Sorte.OBTENU || l.sorte() == AnalyseJournal.Sorte.DEFI) {
                derniereAction = l.sorte().label.toLowerCase(Locale.FRANCE) + " : " + l.titre();
                derniereActionT = l.t();
                return;
            }
        }
    }

    private void releve() {
        Map<Multipliers.Quantity, Double> now = grandeurs();
        double[] row = new double[Multipliers.Quantity.values().length + 1];
        row[0] = temps();
        for (Multipliers.Quantity q : Multipliers.Quantity.values()) row[q.ordinal() + 1] = now.getOrDefault(q, Double.NaN);
        releves.add(row);
    }

    /** Ce que les décisions du pas ont fait : le diff des deux photos, rangé en achats, choses obtenues, automatismes. */
    private void decisions() {
        Photo apres = Photo.of(g.state());
        List<Photo.Changement> changes = Photo.diff(avant, apres);
        avant = null;
        precedent = apres;
        if (changes.isEmpty()) return;
        double t = temps();
        evenements(t, changes);
        boolean reset = changes.stream().anyMatch(c -> (c.cle().equals("explosions") || c.cle().equals("bigBangs") || c.cle().equals("extraUniverses")
                || c.cle().equals("replaying") || c.cle().equals("cosmos")) && c.monte());
        if (reset) {
            for (Photo.Changement c : changes) {
                if (!c.monte()) continue;
                switch (c.cle()) {
                    case "explosions" -> explosion(t, (long) c.apres().toDouble(), changes);
                    case "bigBangs" -> journal.ajoute(t, univers(), AnalyseJournal.Sorte.CHOIX, "Big Bang n° " + (long) c.apres().toDouble(),
                            List.of("Toutes ses conditions étaient remplies : le bot le lance aussitôt, car chaque Big Bang ouvre un palier de vitesse."));
                    case "cosmos" -> journal.ajoute(t, univers(), AnalyseJournal.Sorte.CHOIX, "Échelle du cosmos formée : " + cosmosNom((int) c.apres().toDouble()));
                    case "extraUniverses", "replaying" -> journal.ajoute(t, univers(), AnalyseJournal.Sorte.CHOIX, "Univers suivant lancé",
                            List.of("Dès que l'univers est formé : rien ne sert d'attendre, le suivant garde graines, dimensions et graine."));
                    default -> { }
                }
            }
            grandeursAvant = null;
            achatsDuPasDeRemise(t, changes);
            return;
        }
        Map<Multipliers.Quantity, Double> grandeursApres = grandeurs();
        StringBuilder poids = new StringBuilder();
        double poidsMax = 0;
        if (grandeursAvant != null) {
            for (Map.Entry<Multipliers.Quantity, Double> e : grandeursApres.entrySet()) {
                Double before = grandeursAvant.get(e.getKey());
                if (before == null) continue;
                double delta = e.getValue() - before;
                if (Math.abs(delta) < 1e-6) continue;
                if (poids.length() > 0) poids.append(" ; ");
                poids.append(e.getKey().label()).append(" ×").append(PurchaseHistory.facteur(delta));
                poidsMax = Math.max(poidsMax, delta);
            }
        }
        Double moteurAvant = grandeursAvant == null ? null : grandeursAvant.get(moteur());
        Double moteurApres = grandeursApres.get(moteur());
        double deltaMoteur = moteurAvant != null && moteurApres != null ? moteurApres - moteurAvant : 0;
        // Le temps qu'il restait avant le palier visé, selon la dernière estimation, raccourci du gain de la grandeur qui fait avancer.
        double reste = Double.isNaN(annonceAjustee) ? Double.NaN : Math.max(0, annonceAjustee - t);
        double gain = Double.isNaN(reste) || deltaMoteur <= 0 ? 0 : reste * (1 - Math.pow(10, -deltaMoteur));

        // Ce qui a été acheté et payé (version 0.165 : la lecture vit dans le moteur, l'historique du jeu s'en sert aussi).
        List<Achats.Achat> acquis = Achats.lire(changes);
        boolean depense = changes.stream().anyMatch(c -> c.sousCle() == null && Noms.ressource(c.cle()) && c.baisse());
        // Les éléments synthétisés : obtenus, pas achetés.
        List<String> elements = new ArrayList<>();
        for (Photo.Changement c : changes) {
            if (c.cle().equals("elements") && c.monte()) {
                elements.add(Noms.chose(c.sousCle()) + (c.apres().toDouble() - c.avant().toDouble() > 1 ? " ×" + (long) (c.apres().toDouble() - c.avant().toDouble()) : ""));
            }
        }
        if (!elements.isEmpty()) {
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.OBTENU, "Éléments synthétisés : " + String.join(", ", elements));
        }
        if (depense || !acquis.isEmpty()) {
            derniereActivite = t;
            ligneTouchee = onlineSeconds;
        }
        if (acquis.isEmpty()) return;
        if (gain > 0) annonceAjustee -= gain;
        for (Achats.Achat a : acquis) {
            String montant = a.montant(), paye = a.paye();
            double part = a.part(), montantLog = a.montantLog();
            int lot = acquis.size();
            String texte = (lot > 1 ? "(lot de " + lot + " achats) " : "") + (poids.length() == 0 ? "aucun effet mesuré sur les grandeurs" : poids);
            // La rentabilité : le temps de production que l'achat a coûté, contre le temps qu'il fait gagner.
            Double revenu = a.monnaie() == null ? null : revenuLog.get(a.monnaie());
            double cout = revenu == null || !Double.isFinite(montantLog) ? Double.NaN : Math.pow(10, montantLog - revenu);
            double gagne = Double.isNaN(reste) ? Double.NaN : gain / lot;
            double rentabilite = Double.isFinite(cout) && cout > 0 && Double.isFinite(gagne) ? gagne / cout : Double.NaN;
            journal.achats.add(new AnalyseJournal.Achat(t, univers(), a.groupe(), a.chose(), a.quantite(), paye, montant, part, montantLog, texte,
                    poidsMax / lot, gain / lot, cout, rentabilite));
            // Version 0.166 : une amélioration de dimension ou de graine se juge sur son propre palier, cinq minutes plus tard.
            boolean suivie = suivi.achat(g, t, journal.achats.size() - 1, a.groupe(), a.monnaie(), montantLog, lot);
            List<String> details = new ArrayList<>();
            details.add("Payé : " + montant + " " + paye + (Double.isNaN(part) ? "" : String.format(Locale.FRANCE, " (%.1f %% de la réserve)", 100 * part)));
            details.add("Poids : " + texte);
            if (suivie) details.add("Gain de temps : mesuré dans cinq minutes sur le rythme de « "
                    + AnalyseSuivi.palierVise(g, AnalyseSuivi.cible(a.groupe(), a.monnaie())) + " » (achats.csv)");
            else if (gain > 0) details.add("Gain de temps estimé vers « " + visee + " » : " + AnalyseJournal.duree(gain / lot));
            if (Double.isFinite(cout)) details.add(rentabiliteTexte(cout, gagne, paye));
            long niveaux = Math.round(a.quantite());
            String quoi = a.quoi() + (niveaux > 1 ? " ×" + niveaux : "");
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.ACHAT, quoi, details);
        }
    }

    /**
     * Version 0.168 : les achats faits au pas d'une remise à zéro (l'univers lancé, une explosion, un Big Bang) se notent
     * aussi. Les bulles de l'arbre s'y prennent souvent, juste avant de lancer l'univers suivant ; elles manquaient dans
     * achats.csv. Leur poids sur les grandeurs ne se mesure pas (la remise à zéro du même pas change tout), et le montant
     * payé n'est lisible que si la monnaie n'a fait que baisser.
     */
    private void achatsDuPasDeRemise(double t, List<Photo.Changement> changes) {
        List<Achats.Achat> acquis = Achats.lire(changes);
        if (acquis.isEmpty()) return;
        derniereActivite = t;
        ligneTouchee = onlineSeconds;
        String texte = (acquis.size() > 1 ? "(lot de " + acquis.size() + " achats) " : "") + POIDS_REMISE;
        for (Achats.Achat a : acquis) {
            journal.achats.add(new AnalyseJournal.Achat(t, univers(), a.groupe(), a.chose(), a.quantite(), a.paye(), a.montant(), a.part(),
                    a.montantLog(), texte, 0, Double.NaN, Double.NaN, Double.NaN));
            long niveaux = Math.round(a.quantite());
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.ACHAT, a.quoi() + (niveaux > 1 ? " ×" + niveaux : ""),
                    List.of("Payé : " + a.montant() + " " + a.paye(), "Au pas d'une remise à zéro : " + POIDS_REMISE + "."));
        }
    }

    /** Le poids d'un achat fait au pas d'une remise à zéro (version 0.168). */
    static final String POIDS_REMISE = "poids non mesuré, remise à zéro au même pas";

    /** « Coûte 3 min 10 s de production de particules ; en fait gagner 12 min : ×3,8, rentable avant le palier. » */
    static String rentabiliteTexte(double cout, double gagne, String paye) {
        String out = "Rentabilité : coûte " + AnalyseJournal.duree(cout) + " de production (" + paye + ")";
        if (!Double.isFinite(gagne)) return out + " ; gain de temps inconnu (pas encore d'estimation).";
        double r = cout > 0 ? gagne / cout : Double.POSITIVE_INFINITY;
        String ratio = r >= 1000 ? "×" + PurchaseHistory.facteur(Math.log10(r)) : String.format(Locale.FRANCE, "×%.2f", r);
        return out + " ; en fait gagner " + AnalyseJournal.duree(gagne) + " : " + ratio
                + (r >= 1 ? ", rentable avant le palier." : ", pas remboursé avant le palier (il sert peut-être plus loin).");
    }

    /** Une explosion : la matière noire qu'elle rapporte, par heure depuis la précédente, et pourquoi maintenant. */
    private void explosion(double t, long numero, List<Photo.Changement> changes) {
        List<String> details = new ArrayList<>();
        for (Photo.Changement c : changes) {
            if (!c.cle().equals("darkMatter") || c.sousCle() != null || !c.monte()) continue;
            Photo.Nombre gained = c.apres().minus(c.avant());
            double depuis = t - derniereExplosion;
            details.add("Matière noire : +" + gained.texte() + " (réserve " + c.avant().texte() + " → " + c.apres().texte() + ")");
            if (depuis > 0 && gained.signe() > 0) {
                revenuLog.put("darkMatter", gained.log10() - Math.log10(depuis));
                details.add("Soit " + new Photo.Nombre(1, gained.log10() + Math.log10(3600 / depuis)).texte() + " par heure depuis "
                        + (numero <= 1 ? "le début" : "l'explosion précédente") + " (" + AnalyseJournal.duree(depuis) + ").");
            }
        }
        boolean finDeDefi = changes.stream().anyMatch(c -> c.cle().equals("activeChallenge") && c.retire() != null);
        if (finDeDefi) {
            details.add("Pourquoi maintenant : exploser termine le défi en cours.");
        } else {
            details.add("Pourquoi maintenant : le bot explose dès que le tableau le permet, sauf quand il garde la partie pour préparer un Big Bang ;"
                    + " la matière noire gagnée remplace les particules et les atomes.");
        }
        derniereExplosion = t;
        journal.ajoute(t, univers(), AnalyseJournal.Sorte.CHOIX, "Explosion (n° " + numero + ")", details);
    }

    /**
     * Les automatismes mis en marche ou coupés d'un même pas. Ceux qu'une remise à zéro a coupés ({@code cause}) se
     * notent en une ligne, avec cette cause : ce n'est pas le bot qui les a coupés. Trois ou plus d'un coup, aussi en une
     * ligne, avec ce que fait chacun.
     */
    private void automatismes(double t, List<Photo.Changement> changes, String cause) {
        List<String> on = new ArrayList<>(), off = new ArrayList<>();
        for (Photo.Changement c : changes) {
            String id = c.ajoute() != null ? c.ajoute() : c.retire();
            boolean marche = c.cle().equals("dimensionAutomationsOff") ? c.retire() != null : c.ajoute() != null;
            (marche ? on : off).add(id);
        }
        if (!off.isEmpty() && cause != null) {
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.AUTOMATISME, "Coupés par " + cause + " : " + noms(off),
                    List.of("Ce n'est pas le bot : " + cause + " remet les automatismes à zéro ; il les remet en marche dès qu'il peut."));
            off.clear();
        }
        for (String id : off) coupures.merge(Noms.chose(id), 1, Integer::sum);
        for (List<String> ids : List.of(on, off)) {
            if (ids.isEmpty()) continue;
            boolean marche = ids == on;
            if (ids.size() < 3) {
                for (String id : ids) automatisme(t, id, marche);
                continue;
            }
            List<String> details = new ArrayList<>();
            for (String id : ids) {
                String role = AnalysePourquoi.role(id);
                if (!role.isEmpty()) details.add(Noms.chose(id) + " : il " + role + ".");
            }
            details.add(marche ? pourquoiMarche() : pourquoiCoupe());
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.AUTOMATISME, (marche ? "Mis en marche : " : "Coupés : ") + noms(ids), details);
        }
    }

    private static String noms(List<String> ids) {
        return String.join(", ", ids.stream().map(Noms::chose).toList());
    }

    private static String pourquoiMarche() {
        return "Pourquoi : il fait ce geste plus souvent et sans oubli ; le bot garde ses propres décisions pour le reste.";
    }

    private String pourquoiCoupe() {
        String defi = defiActif(g);
        return "Pourquoi : il dépenserait ce que le bot met de côté" + (defi != null ? " pendant le défi « " + Noms.chose(defi) + " »"
                : " pour « " + (visee.isEmpty() ? nomPalier(indexPrincipal + 1) : visee) + " »") + " (goulot : " + goulot() + ").";
    }

    /** Version 0.164 : les illogismes repérés à la fin de la partie. */
    List<AnalyseIllogismes.Illogisme> illogismes = List.of();

    /** Version 0.164 : combien de fois le bot a coupé chaque automatisme lui-même (pas une remise à zéro). */
    final Map<String, Integer> coupures = new LinkedHashMap<>();

    private void automatisme(double t, String id, boolean on) {
        String name = Noms.chose(id);
        List<String> details = new ArrayList<>();
        String role = AnalysePourquoi.role(id);
        if (!role.isEmpty()) details.add("Il " + role + ".");
        details.add(on ? pourquoiMarche() : pourquoiCoupe());
        journal.ajoute(t, univers(), AnalyseJournal.Sorte.AUTOMATISME, (on ? "Mis en marche : " : "Coupé : ") + name, details);
    }

    private static String cosmosNom(int stage) {
        Cosmos[] all = Cosmos.values();
        return stage >= 1 && stage <= all.length ? all[stage - 1].label() : "échelle " + stage;
    }

    // ------------------------------------------------------------------
    // L'estimation
    // ------------------------------------------------------------------

    private static final PrintStream SILENCE = new PrintStream(OutputStream.nullOutputStream());

    /**
     * Joue la suite sur une copie de la partie, avec le même jeu, jusqu'au prochain palier principal : le temps qu'il a
     * fallu, ou −1 au-delà de l'horizon (24 h).
     */
    double essai(int cible, int bigBangCible, double horizon) {
        return essai(cible, bigBangCible, horizon, null);
    }

    /** Le même essai, la copie jouant avec les choix de cette façon de jouer ({@code null} : la sienne). */
    double essai(int cible, int bigBangCible, double horizon, AnalyseStrategies.Strategie strategie) {
        return essai(game -> indexPrincipal(game) >= cible || bigBangCible > 0 && game.bigBangs() >= bigBangCible, horizon, strategie, 0);
    }

    /** Vrai si le dernier essai d'un défi l'a réussi (et pas abandonné). */
    private boolean essaiReussi;

    /**
     * Joue la suite sur une copie jusqu'à ce que {@code but} soit vrai : le temps qu'il a fallu, ou −1 au-delà de
     * l'horizon. La copie joue avec les choix de {@code strategie} ({@code null} : les siens), et passe toutes les
     * {@code rythme} secondes ({@code 0} : à son rythme).
     */
    double essai(java.util.function.Predicate<Game> but, double horizon, AnalyseStrategies.Strategie strategie, double rythme) {
        GameState copy;
        try {
            copy = SaveCodec.read(SaveCodec.write(g.state(), 0)).state();
        } catch (Exception failure) {
            return -1;
        }
        Game cg = Bots.game(copy, graine + 7919L * (long) temps(), sans);
        double pas = rythme > 0 ? Math.min(rythme, ETSI_PAS) : ESSAI_PAS;
        IdleProfiles24 clone = copieDuBot(cg, pas);
        if (strategie != null) AnalyseStrategies.appliquer(clone, strategie);
        if (rythme > 0) {
            clone.actionInterval = rythme;
            clone.nextActionAt = clone.wallSeconds + rythme;
        }
        String defi = defiActif(g);
        // Les « et si » se jouent à plusieurs en même temps : ils coupent la sortie ensemble, une seule fois.
        boolean silence = System.out != SILENCE;
        PrintStream out = System.out, err = System.err;
        if (silence) {
            System.setOut(SILENCE);
            System.setErr(SILENCE);
        }
        try {
            double t = 0;
            while (t < horizon) {
                boolean done = clone.stepOnce();
                t += pas;
                if (but.test(cg)) {
                    essaiReussi = defi != null && reussi(cg, defi);
                    return t;
                }
                if (done) return -1;
            }
            return -1;
        } catch (RuntimeException failure) {
            return -1;
        } finally {
            if (silence) {
                System.setOut(out);
                System.setErr(err);
            }
        }
    }

    /**
     * Un bot neuf sur la copie de la partie, avec la mémoire de celui-ci ({@link AnalyseCopie}) : sans elle, la copie
     * joue autrement (mesuré à la 0.161 : une copie prise à 2 h n'explosait plus). Il joue par pas de
     * {@code pas} secondes, sans rien écrire.
     */
    IdleProfiles24 copieDuBot(Game copie, double pas) {
        IdleProfiles24 clone = new IdleProfiles24(copie, p, graine, pas);
        AnalyseCopie.memoire(this, clone);
        clone.quiet = true;
        try {
            java.lang.reflect.Field field = RobotCosmosPlanner.class.getDeclaredField("step");
            field.setAccessible(true);
            field.setDouble(clone, pas);
        } catch (ReflectiveOperationException impossible) {
            // Le pas reste celui du bot : l'essai est seulement plus lent.
        }
        return clone;
    }

    /** Le palier que viserait une estimation maintenant. */
    private String viseeDuMoment() {
        if (g.isFinalChallengeOpen()) return "Défi final : " + (g.finalTiersReached() + 1) + " palier(s) sur 10";
        int cible = indexPrincipal + 1 + ((indexPrincipal + 1) % 7 == 0 ? 1 : 0);
        int bang = g.bigBangs() >= 1 && g.bigBangs() + 1 < Bots.LAST_BIG_BANG && etape(g) == 2 ? g.bigBangs() + 1 : 0;
        return bang > 0 ? "Univers " + univers() + " : Big Bang n° " + bang : nomPalier(cible);
    }

    private void estimer() {
        if (NextUniversePolicy.reached(g, targetUniverses) || fini) return;
        if (g.isFinalChallengeOpen()) {
            estimerDefiFinal();
            return;
        }
        int cible = indexPrincipal + 1 + ((indexPrincipal + 1) % 7 == 0 ? 1 : 0);
        // Entre le premier et le dernier Big Bang, le prochain palier est le Big Bang suivant.
        int bang = g.bigBangs() >= 1 && g.bigBangs() + 1 < Bots.LAST_BIG_BANG && etape(g) == 2 ? g.bigBangs() + 1 : 0;
        visee = bang > 0 ? "Univers " + univers() + " : Big Bang n° " + bang : nomPalier(cible);
        double secondes = essai(cible, bang, HORIZON);
        String goulot = goulot();
        journal.estimations.add(new AnalyseJournal.Estimation(temps(), visee, secondes, goulot));
        if (secondes < 0) {
            estimationCourante = Double.NaN;
            if (absences()) {
                journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.ESTIMATION, "Prochain palier « " + visee + " » : au-delà de "
                        + AnalyseJournal.duree(HORIZON) + " de jeu", List.of("Le joueur ferme le jeu entre deux visites : une longue attente n'est pas un blocage."));
                return;
            }
            bloque("Plus de " + AnalyseJournal.duree(HORIZON) + " estimées avant « " + visee + " » en jouant la suite sur une copie");
            return;
        }
        estimationCourante = secondes;
        annonceAbsolue = temps() + secondes;
        annonceAjustee = annonceAbsolue;
        annonceVisee = visee;
        espacer(secondes);
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.ESTIMATION, "Prochain palier « " + visee + " » dans " + AnalyseJournal.duree(secondes)
                + " (vers " + AnalyseJournal.chrono(temps() + secondes) + ")", List.of("Goulot : " + goulot));
    }

    /**
     * Le défi final ouvert (version 0.163) : l'estimation vise son palier suivant, joué sur une copie. Au-delà de 24 h,
     * le bot s'arrête, comme ailleurs.
     */
    private void estimerDefiFinal() {
        int next = g.finalTiersReached() + 1;
        visee = "Défi final : " + next + " palier(s) sur 10";
        double secondes = essai(game -> game.finalTiersReached() >= next || game.isGameFinished(), HORIZON, null, 0);
        String goulot = goulotDefiFinal();
        journal.estimations.add(new AnalyseJournal.Estimation(temps(), visee, secondes, goulot));
        if (secondes < 0) {
            estimationCourante = Double.NaN;
            if (absences()) {
                journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.ESTIMATION, "Prochain palier « " + visee + " » : au-delà de "
                        + AnalyseJournal.duree(HORIZON) + " de jeu", List.of("Le joueur ferme le jeu entre deux visites : une longue attente n'est pas un blocage."));
                return;
            }
            bloque("Plus de " + AnalyseJournal.duree(HORIZON) + " estimées avant « " + visee + " » en jouant la suite sur une copie");
            return;
        }
        estimationCourante = secondes;
        annonceAbsolue = temps() + secondes;
        annonceAjustee = annonceAbsolue;
        annonceVisee = visee;
        espacer(secondes);
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.ESTIMATION, "Prochain palier « " + visee + " » dans " + AnalyseJournal.duree(secondes)
                + " (vers " + AnalyseJournal.chrono(temps() + secondes) + ")", List.of("Goulot : " + goulot));
    }

    /** Version 0.170 : la prochaine estimation attend une part du temps annoncé ({@link #PART_ESTIMATION}). */
    private void espacer(double secondes) {
        if (secondes > 0) prochaineEstimation = Math.max(prochaineEstimation, temps() + secondes * PART_ESTIMATION);
    }

    /** Pour le défi final : la dimension la plus loin de son palier, en puissances de dix, et son avancée. */
    String goulotDefiFinal() {
        int worst = 0;
        double lowest = 2;
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            if (g.isFinalTierReached(d)) continue;
            double p = g.finalProgress(d);
            if (p < lowest) {
                lowest = p;
                worst = d;
            }
        }
        if (worst == 0) return "tous les paliers sont atteints";
        return String.format(Locale.FRANCE, "la dimension %d (%s) : %.0f %% de son palier, en puissances de dix", worst,
                Game.dimensionResourceName(worst), 100 * lowest);
    }

    /**
     * Le choix de la façon de jouer jusqu'au prochain palier principal (version 0.162) : chacune joue la suite sur une
     * copie de la partie, avec la mémoire du bot ; la plus rapide est gardée. Le journal donne le classement, et ce que
     * chaque autre aurait coûté : le « et si ».
     */
    void choisirStrategie() {
        if (!comparer || NextUniversePolicy.reached(g, targetUniverses)) return;
        int cible = indexPrincipal + 1 + ((indexPrincipal + 1) % 7 == 0 ? 1 : 0);
        String vise = nomPalier(cible);
        record Essai(AnalyseStrategies.Strategie s, double secondes) {}
        List<Essai> essais = new ArrayList<>();
        for (AnalyseStrategies.Strategie s : AnalyseStrategies.TOUTES) essais.add(new Essai(s, essai(cible, 0, HORIZON_CHOIX, s)));
        essais.sort((a, b) -> Double.compare(a.secondes() < 0 ? Double.MAX_VALUE : a.secondes(), b.secondes() < 0 ? Double.MAX_VALUE : b.secondes()));
        Essai best = essais.get(0);
        if (best.secondes() < 0) {
            journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.CHOIX, "Aucune façon de jouer n'atteint « " + vise + " » en "
                    + AnalyseJournal.duree(HORIZON_CHOIX) + " : le bot garde « " + strategie.nom() + " »");
            return;
        }
        // À égalité (à moins d'une minute), le bot garde sa façon de jouer : changer ne rapporte rien.
        Essai current = essais.stream().filter(e -> e.s() == strategie).findFirst().orElse(best);
        Essai chosen = current.secondes() >= 0 && current.secondes() - best.secondes() < 60 ? current : best;
        List<String> details = new ArrayList<>();
        details.add("Chaque façon de jouer a joué la suite jusqu'à « " + vise + " » sur une copie de la partie :");
        for (Essai e : essais) {
            String temps = e.secondes() < 0 ? "pas atteint en " + AnalyseJournal.duree(HORIZON_CHOIX) : AnalyseJournal.duree(e.secondes());
            String ecart = e == chosen || e.secondes() < 0 ? "" : " (" + AnalyseJournal.duree(e.secondes() - chosen.secondes()) + " de plus)";
            details.add((e == chosen ? "→ " : "   ") + e.s().nom() + " — " + e.s().idee() + " : " + temps + ecart);
        }
        if (chosen.s() != strategie) {
            details.add("Le bot passe de « " + strategie.nom() + " » à « " + chosen.s().nom() + " ».");
            strategie = chosen.s();
            AnalyseStrategies.appliquer(this, strategie);
        } else {
            details.add("Le bot garde « " + strategie.nom() + " ».");
        }
        journal.choix.add(new AnalyseJournal.Choix(temps(), vise, chosen.s().nom(),
                essais.stream().map(e -> e.s().nom() + "=" + Math.round(e.secondes())).toList()));
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.CHOIX, "Façon de jouer jusqu'à « " + vise + " » : " + chosen.s().nom()
                + " (" + AnalyseJournal.duree(chosen.secondes()) + ")", details);
    }

    /**
     * « Et si » au palier principal qui vient d'être atteint (version 0.162) : la suite jusqu'au palier principal suivant,
     * jouée sur des copies par un joueur qui passe une fois par minute, puis toutes les dix minutes. Le temps réel, celui
     * du bot, s'ajoute quand le palier suivant arrive.
     */
    void etSi() {
        if (!etsi || NextUniversePolicy.reached(g, targetUniverses)) return;
        int cible = indexPrincipal + 1 + ((indexPrincipal + 1) % 7 == 0 ? 1 : 0);
        double[] temps = new double[RYTHMES_ETSI.length];
        // Les copies ne partagent rien : elles se jouent en même temps, une par cœur du processeur.
        PrintStream out = System.out, err = System.err;
        System.setOut(SILENCE);
        System.setErr(SILENCE);
        try {
            java.util.stream.IntStream.range(0, RYTHMES_ETSI.length).parallel()
                    .forEach(i -> temps[i] = essai(game -> indexPrincipal(game) >= cible, HORIZON, null, RYTHMES_ETSI[i]));
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
        String depuis = nomPalier(indexPrincipal), vise = nomPalier(cible);
        etsiEnAttente = new AnalyseJournal.EtSi(temps(), univers(), depuis, vise, temps[0], temps[1], Double.NaN);
        etsiCible = cible;
        List<String> details = new ArrayList<>();
        for (int i = 0; i < RYTHMES_ETSI.length; i++) {
            details.add("Un joueur qui passe " + (RYTHMES_ETSI[i] < 120 ? "une fois par minute" : "toutes les " + (int) (RYTHMES_ETSI[i] / 60) + " minutes")
                    + " : " + (temps[i] < 0 ? "pas atteint en " + AnalyseJournal.duree(HORIZON) : AnalyseJournal.duree(temps[i])));
        }
        details.add("Le temps du bot (une décision toutes les " + (int) p.actionEverySeconds() + " s) s'écrit au palier suivant, dans etsi.csv et le rapport.");
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.CHOIX, "Et si : jusqu'à « " + vise + " » à un autre rythme", details);
    }

    /** Ce qui freine le plus en ce moment, selon l'étape. */
    String goulot() {
        StringBuilder out = new StringBuilder();
        int e = etape(g);
        if (e >= 1 && e <= 3 || g.bigBangs() >= 1 && g.bigBangs() < Bots.LAST_BIG_BANG) {
            List<String> missing = new ArrayList<>();
            for (BigBangCondition c : g.bigBangConditions()) if (!g.isBigBangConditionMet(c)) missing.add(condition(c));
            if (!missing.isEmpty()) out.append("Big Bang n° ").append(g.bigBangs() + 1).append(" : il manque ").append(String.join(", ", missing));
        }
        if (e >= 3 && e < 6 && g.nextCosmos() != null) {
            if (out.length() > 0) out.append(" ; ");
            out.append("prochaine échelle du cosmos : ").append(g.nextCosmos().label())
                    .append(g.canFormCosmos(g.nextCosmos()) ? " (formable)" : " (pas encore formable : astres mûrs " + g.bodiesMature() + "/" + g.bodies().size() + ")");
        }
        if (out.length() == 0) out.append("la grandeur qui fait avancer : ").append(moteur().label());
        return out.toString();
    }

    /** Le nom d'une condition du Big Bang dans une phrase (« les particules »). */
    static String conditionNom(BigBangCondition c) {
        return condition(c);
    }

    private static String condition(BigBangCondition c) {
        return switch (c) {
            case PARTICLES -> "les particules";
            case ATOMS -> "les atomes";
            case DARK_MATTER -> "la matière noire";
            case ACHIEVEMENTS -> "des succès";
            case CHALLENGES -> "les défis d'explosion";
            case SKY -> "des astres mûrs";
        };
    }

    // ------------------------------------------------------------------
    // Le blocage
    // ------------------------------------------------------------------

    private void bloque(String raison) {
        blocage = raison;
        List<String> details = new ArrayList<>();
        details.add("Raison : " + raison);
        details.add("Prochain palier visé : « " + nomPalier(indexPrincipal + 1) + " »");
        details.add("Goulot : " + goulot());
        Map<Multipliers.Quantity, Double> now = grandeurs();
        for (Map.Entry<Multipliers.Quantity, Double> e : now.entrySet()) {
            details.add(String.format(Locale.FRANCE, "%s : 10^%.2f", e.getKey().label(), e.getValue()));
        }
        details.add("Dernier palier : " + AnalyseJournal.chrono(dernierPalier) + " ; dernière activité : " + AnalyseJournal.chrono(derniereActivite));
        ProfileProgressGuard.Assessment guard = progressGuard.latest();
        if (guard != null) details.add("Garde-fou des profils : " + guard.verdict() + " — " + guard.reason());
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.BLOCAGE, "Le bot s'arrête : il n'avance plus", details);
        sauvegarde("blocage");
    }

    // ------------------------------------------------------------------
    // Le lancement
    // ------------------------------------------------------------------

    /** Joue jusqu'à l'univers visé, le blocage ou la limite d'heures, puis écrit tout. */
    void joue(double heures) throws Exception {
        long start = System.nanoTime();
        journal.ajoute(0, 1, AnalyseJournal.Sorte.INFO, "Début de la partie", List.of("Profil : " + (p == ANALYSTE ? "le Speedrunner" : p.name() + " (" + p.label() + ")")
                + ", une décision toutes les "
                + (int) p.actionEverySeconds() + " s ; pas de " + (int) step + " s ; graine " + graine + " ; "
                + (jusquALaFin ? "jusqu'à la fin de la partie" : targetUniverses + " univers visés")
                + (temps() > 0 ? " ; reprise d'une sauvegarde à " + AnalyseJournal.chrono(temps()) : "")
                + (sans.isEmpty() ? "" : " ; sans " + String.join(", ", sans.stream().map(BotAnalyste::nomSans).toList()))
                + (UniverseTreePolicy.forcedPlan == null ? "" : " ; bulles achetées " + (UniverseTreePolicy.forcedPlan == UniverseTreePolicy.Plan.BEST
                        ? "dans l'ordre de l'expert" : "la moins chère d'abord"))));
        begin();
        if (!memoireReprise.isEmpty()) {
            int remis = AnalyseMemoire.remettre(this, memoireReprise);
            journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, "Mémoire du bot reprise de la sauvegarde : " + remis + " champs",
                    List.of("La même partie continue : les politiques, leurs compteurs et les hasards reprennent où ils étaient."));
        }
        partSucces = partSucces();
        prochaineEstimation = 0;
        choisirStrategie();
        etSi();
        int count = (int) Math.ceil(heures * 3600 / step);
        try {
            for (int i = 0; i < count; i++) {
                if (stepOnce()) break;
            }
        } catch (IllegalStateException guard) {
            // Le garde-fou des profils a jugé la partie bloquée : le bot s'arrête aussi, et le dit.
            bloque("Garde-fou des profils : " + guard.getMessage());
        }
        double seconds = (System.nanoTime() - start) / 1e9;
        String fin = blocage != null ? "Arrêt sur blocage : " + blocage
                : fini ? "Fin de la partie atteinte"
                : NextUniversePolicy.reached(g, targetUniverses) ? "Objectif atteint : " + targetUniverses + " univers" : "Limite de " + heures + " h atteinte";
        // Version 0.164 : les détecteurs d'illogismes relisent la partie.
        suivi.fin(temps());
        illogismes = AnalyseIllogismes.detecter(this);
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, illogismes.size() + " illogismes repérés (détail dans illogismes.txt et le rapport)");
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, fin, List.of(String.format(Locale.FRANCE, "Calcul : %.0f s de temps réel", seconds)));
        journal.ecrire(dossier);
        AnalyseIllogismes.ecrire(illogismes, dossier);
        Files.writeString(dossier.resolve("fin.save"), SaveCodec.write(g.state(), 0));
        // Version 0.168 : la mémoire du bot à côté, pour qu'une tranche suivante reparte de fin.save comme d'un palier.
        AnalyseMemoire.ecrire(this, dossier.resolve("fin.save"));
        AnalyseRapport.ecrire(this, dossier.resolve("rapport.html"), fin);
        System.out.println("ANALYSTE," + fin + "," + AnalyseJournal.chrono(temps()) + "," + dossier.toAbsolutePath());
    }

    /** Les identifiants d'améliorations à retirer, séparés par des virgules ; refuse ceux qui n'existent pas. */
    /** L'ordre des bulles imposé : « meilleur » (l'ordre de l'expert), « moins-cher », ou rien (celui du profil). */
    static UniverseTreePolicy.Plan ordre(String text) {
        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "" -> null;
            case "meilleur", "expert" -> UniverseTreePolicy.Plan.BEST;
            case "moins-cher", "moinscher", "cheapest" -> UniverseTreePolicy.Plan.CHEAPEST;
            default -> throw new IllegalArgumentException("Ordre des bulles inconnu : " + text + " (meilleur, moins-cher)");
        };
    }

    /** Le préfixe d'une bulle de l'arbre dans {@code sans} (version 0.168). */
    static final String BULLE = "bulle:";

    /** Les bulles retirées par {@code sans} (version 0.168). */
    static java.util.Set<Game.TreeBubble> bullesSans(java.util.Set<String> sans) {
        java.util.Set<Game.TreeBubble> out = java.util.EnumSet.noneOf(Game.TreeBubble.class);
        for (String id : sans) if (id.startsWith(BULLE)) out.add(Game.TreeBubble.valueOf(id.substring(BULLE.length())));
        return out;
    }

    /** Le nom de ce que {@code sans} retire : une amélioration, une case de l'arbre ou une bulle (version 0.168). */
    static String nomSans(String id) {
        return id.startsWith(BULLE) ? Game.TreeBubble.valueOf(id.substring(BULLE.length())).label() : Noms.chose(id);
    }

    static java.util.Set<String> sans(String text) {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        for (String part : text.split(",")) if (!part.isBlank()) ids.add(part.trim());
        for (String id : ids) {
            if (id.startsWith(BULLE)) {
                // Version 0.168 : une bulle de l'arbre, par son nom (bulle:LEAVES_LEFT) ; une inconnue lève une erreur.
                Game.TreeBubble.valueOf(id.substring(BULLE.length()));
                continue;
            }
            boolean known = Upgrades.DEFAULT.stream().anyMatch(u -> u.id().equals(id))
                    || idle.core.darkmatter.DarkUpgrades.DEFAULT.stream().anyMatch(d -> d.id().equals(id));
            if (!known) throw new IllegalArgumentException("Amélioration inconnue : " + id + " (identifiants dans le catalogue : upgrades, arbre de matière noire)");
        }
        return ids;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage : BotAnalyste <dossier> [univers=fin] [heures=400] [graine=201] [pas=5] [estimation_min=30] [comparer=false] [etsi=true] [depart=sauvegarde] [sans=id,id] [profil=speedrunner|regulier|absent]");
            System.exit(2);
        }
        Path dossier = Path.of(args[0]);
        String univers = args.length > 1 ? args[1] : "fin";
        double heures = args.length > 2 ? Double.parseDouble(args[2]) : 400;
        long graine = args.length > 3 ? Long.parseLong(args[3]) : 201;
        double pas = args.length > 4 ? Double.parseDouble(args[4]) : 5;
        double estimation = args.length > 5 ? Double.parseDouble(args[5]) : 30;
        boolean comparer = args.length > 6 && Boolean.parseBoolean(args[6]);
        boolean etsi = args.length <= 7 || Boolean.parseBoolean(args[7]);
        String depart = args.length > 8 && !args[8].isBlank() ? args[8] : null;
        java.util.Set<String> sans = args.length > 9 ? sans(args[9]) : java.util.Set.of();
        Profile profile = profil(args.length > 10 ? args[10] : "");
        // Version 0.168 : les bulles retirées, et l'ordre des bulles imposé (meilleur, moins-cher) pour mesurer la dispersion.
        UniverseTreePolicy.withoutBubbles = bullesSans(sans);
        UniverseTreePolicy.forcedPlan = args.length > 11 ? ordre(args[11]) : null;
        GameState state = depart == null ? new GameState() : SaveCodec.read(Files.readString(Path.of(depart))).state();
        Game game = Bots.game(state, graine, sans);
        if (depart == null) game.start();
        BotAnalyste bot = new BotAnalyste(game, profile, graine, pas, dossier, estimation);
        bot.jusquALaFin = univers.equalsIgnoreCase("fin");
        bot.sans = sans;
        bot.targetUniverses = bot.jusquALaFin ? Integer.MAX_VALUE : Integer.parseInt(univers);
        bot.comparer = comparer;
        bot.etsi = etsi;
        if (depart != null) bot.memoireReprise = AnalyseMemoire.relire(Path.of(depart));
        bot.joue(heures);
    }
}
