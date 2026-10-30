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

/**
 * Le bot analyste (version 0.161) : il joue comme le meilleur profil (le Speedrunner), mais à vitesse humaine, une
 * décision toutes les cinq secondes, et note tout, à la seconde de jeu près :
 * <ul>
 *   <li>chaque achat : quoi, combien de niveaux, ce qu'il a coûté et dans quelle monnaie, la part de la réserve que
 *       cela représentait, son poids sur chaque grandeur du jeu (particules, atomes, matière noire, espace…) et le temps
 *       qu'il fait gagner selon l'estimation du moment ;</li>
 *   <li>chaque chose obtenue sans payer (succès, titres, défis réussis), chaque automatisme mis en marche ou coupé ;</li>
 *   <li>chaque palier : les trois actes de chaque univers (première explosion, premier Big Bang, univers formé) et leurs
 *       étapes (huitième Big Bang, galaxie, amas), les paliers de la graine, les dimensions, leurs seuils, le défi final
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
 * <p>Usage : {@code BotAnalyste <dossier> [univers=11] [heures=200] [graine=201] [pas=5] [estimation_min=30] [comparer=false] [etsi=true]}.
 * Avec {@code comparer=true}, il compare aussi six façons de jouer à chaque palier principal et garde la plus rapide
 * ({@link #choisirStrategie()}) : six parties d'essai par palier, la partie dure plusieurs fois plus longtemps. Avec
 * {@code etsi=false}, il ne joue pas les « et si » : la partie se calcule environ deux fois plus vite.
 */
public final class BotAnalyste extends IdleProfiles24 {

    /** Le Speedrunner, à vitesse humaine : une décision toutes les cinq secondes. */
    static final Profile ANALYSTE = new Profile(1, "Analyste", "Le meilleur jeu connu, à vitesse humaine", 5, 0, 0, 1, 1, 1, 1, 1, 0, "doubleplan");
    /** Le pas des parties d'essai : plus long, pour estimer vite. */
    static final double ESSAI_PAS = 10;
    /** Au-delà, l'estimation abandonne : la partie est bloquée. */
    static final double HORIZON = 24 * 3600;
    /** Sans palier, achat ni dépense pendant ce temps, la partie est bloquée. */
    static final double IMMOBILE = 30 * 60;

    final AnalyseJournal journal = new AnalyseJournal();
    /** Version 0.162 : la façon de jouer du moment, et si le bot les compare à chaque palier principal. */
    AnalyseStrategies.Strategie strategie = AnalyseStrategies.TOUTES.get(0);
    boolean comparer = false;
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

    private AnalyseEtat avant;
    private Map<Multipliers.Quantity, Double> grandeursAvant;
    private int indexPrincipal, seedStage, dimensions, finalTiers, bigBangs;
    private final boolean[] seuils = new boolean[Game.MAX_DIMENSIONS + 1];
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
    private int etsiCible = -1;

    BotAnalyste(Game game, long seed, double step, Path dossier, double estimationMinutes) {
        super(game, ANALYSTE, seed, step);
        this.graine = seed;
        this.dossier = dossier;
        this.estimationEvery = estimationMinutes * 60;
        indexPrincipal = indexPrincipal(game);
        seedStage = game.seedStage();
        dimensions = game.dimensionsUnlocked();
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
        journal.paliers.add(new AnalyseJournal.Palier(t, u, nom, t - dernierPalier, annonce));
        List<String> details = new ArrayList<>();
        details.add("Depuis le palier précédent : " + AnalyseJournal.duree(t - dernierPalier));
        if (!Double.isNaN(annonce)) {
            details.add("La dernière estimation l'annonçait vers " + AnalyseJournal.chrono(annonce) + " : écart de "
                    + (t >= annonce ? "+" : "−") + AnalyseJournal.duree(Math.abs(t - annonce)));
        }
        details.addAll(decomposition());
        journal.ajoute(t, u, AnalyseJournal.Sorte.PALIER, nom, details);
        dernierPalier = t;
        sauvegarde(nom);
        // Le journal s'écrit aussi à chaque palier : une partie interrompue garde ce qu'elle a vu.
        try {
            journal.ecrire(dossier);
        } catch (Exception failure) {
            // L'écriture finale réessaiera.
        }
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
            Files.writeString(saves.resolve(nomSauvegarde(temps(), nom)), SaveCodec.write(g.state(), 0));
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
        avant = AnalyseEtat.of(g.state());
        grandeursAvant = grandeurs();
        // Ce que le temps a apporté depuis les décisions du pas d'avant : les succès, titres et défis tombent souvent là,
        // et ce que chaque ressource a rapporté pendant ce pas sans décision.
        if (precedent != null) {
            List<AnalyseEtat.Changement> changes = AnalyseEtat.diff(precedent, avant);
            revenus(changes);
            evenements(temps(), changes);
        }
    }

    /** Ce que chaque ressource a rapporté par seconde pendant le pas, quand elle a monté (une baisse : un automatisme a payé). */
    private void revenus(List<AnalyseEtat.Changement> changes) {
        for (AnalyseEtat.Changement c : changes) {
            if (c.sousCle() != null || !AnalyseNoms.ressource(c.cle()) || c.cle().equals("darkMatter") || !c.monte()) continue;
            AnalyseEtat.Nombre gained = c.apres().minus(c.avant());
            if (gained.signe() > 0) revenuLog.put(c.cle(), gained.log10() - Math.log10(step));
        }
    }

    /** La photo prise après les décisions du pas d'avant. */
    private AnalyseEtat precedent;

    /**
     * Ce qui arrive sans être un achat, entre deux photos : succès, titres, défis lancés, réussis ou abandonnés,
     * automatismes mis en marche ou coupés, réglages. Noté que ce soit pendant le temps de jeu ou pendant les
     * décisions, et même au pas d'une explosion (un défi se réussit en explosant).
     */
    private void evenements(double t, List<AnalyseEtat.Changement> changes) {
        List<String> succes = new ArrayList<>();
        List<AnalyseEtat.Changement> automatismes = new ArrayList<>();
        // Dans l'ordre où le joueur les vit : le défi qui finit, puis celui qui commence, puis le reste.
        List<AnalyseEtat.Changement> ordre = new ArrayList<>(changes);
        ordre.sort(java.util.Comparator.comparingInt(c -> !c.cle().startsWith("active") ? 2 : c.retire() != null ? 0 : 1));
        for (AnalyseEtat.Changement c : ordre) {
            String key = c.cle();
            if (key.equals("achievements") && c.ajoute() != null) {
                succes.add(c.ajoute());
            } else if (key.equals("titles") && c.monte() && c.avant().signe() == 0) {
                journal.ajoute(t, univers(), AnalyseJournal.Sorte.SUCCES, "Titre obtenu : " + AnalyseNoms.chose(c.sousCle()));
            } else if ((key.equals("activeChallenge") || key.equals("activeBangChallenge")) && c.ajoute() != null) {
                defiLance(t, c.ajoute());
            } else if ((key.equals("activeChallenge") || key.equals("activeBangChallenge")) && c.retire() != null) {
                defiFini(t, c.retire(), changes);
            } else if (REGLAGES.containsKey(key)) {
                String from = c.avant() != null ? c.avant().texte() : c.retire() != null ? AnalyseNoms.chose(c.retire()) : "—";
                String to = c.apres() != null ? c.apres().texte() : c.ajoute() != null ? AnalyseNoms.chose(c.ajoute()) : "—";
                journal.ajoute(t, univers(), AnalyseJournal.Sorte.CHOIX, "Réglage : " + REGLAGES.get(key) + (c.sousCle() != null && c.ajoute() == null && c.retire() == null
                        ? " (" + AnalyseNoms.chose(c.sousCle()) + ")" : "") + " : " + from + " → " + to);
            } else if (key.equals("enabledAutomations") || key.equals("enabledDarkAutomations") || key.equals("dimensionAutomationsOff")) {
                automatismes.add(c);
            }
        }
        if (!automatismes.isEmpty()) automatismes(t, automatismes, cause(changes));
        if (!succes.isEmpty()) succes(t, succes);
    }

    /** Ce qui a remis la partie à zéro pendant ces changements ({@code null} : rien) : une explosion, un défi, un Big Bang… */
    static String cause(List<AnalyseEtat.Changement> changes) {
        for (AnalyseEtat.Changement c : changes) {
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
                poids.append(e.getKey().label()).append(" ×").append(facteur(delta));
            }
        }
        partSucces = now;
        for (String id : ids) {
            List<String> details = new ArrayList<>(AnalysePourquoi.succes(g, id));
            if (poids.length() > 0) details.add("Poids" + (ids.size() > 1 ? " des " + ids.size() + " succès de ce moment" : "") + " : " + poids);
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.SUCCES, "Succès obtenu : " + AnalyseNoms.chose(id), details);
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
        journal.ajoute(t, univers(), AnalyseJournal.Sorte.DEFI, "Défi lancé : " + AnalyseNoms.chose(id), details);
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
    private void defiFini(double t, String id, List<AnalyseEtat.Changement> changes) {
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
        journal.ajoute(t, univers(), AnalyseJournal.Sorte.DEFI, (reussi ? "Défi réussi : " : "Défi abandonné : ") + AnalyseNoms.chose(id), details);
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
        if (avant != null) decisions();
        paliers();
        double t = temps();
        if (t + 1e-6 >= prochainReleve) {
            releve();
            prochainReleve = t + 60;
        }
        if (t + 1e-6 >= prochaineEstimation && blocage == null) {
            estimer();
            prochaineEstimation = t + estimationEvery;
        }
        String now = signature();
        if (!now.equals(signature)) {
            signature = now;
            derniereActivite = t;
        }
        if (blocage == null && t - Math.max(dernierPalier, derniereActivite) > IMMOBILE) {
            bloque("Plus rien ne bouge depuis " + AnalyseJournal.duree(t - Math.max(dernierPalier, derniereActivite))
                    + " : ni palier, ni achat, ni compteur (Big Bangs, astres, arrosages…), ni grandeur qui monte");
        }
        return done || blocage != null;
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

    private void releve() {
        Map<Multipliers.Quantity, Double> now = grandeurs();
        double[] row = new double[Multipliers.Quantity.values().length + 1];
        row[0] = temps();
        for (Multipliers.Quantity q : Multipliers.Quantity.values()) row[q.ordinal() + 1] = now.getOrDefault(q, Double.NaN);
        releves.add(row);
    }

    /** Ce que les décisions du pas ont fait : le diff des deux photos, rangé en achats, choses obtenues, automatismes. */
    private void decisions() {
        AnalyseEtat apres = AnalyseEtat.of(g.state());
        List<AnalyseEtat.Changement> changes = AnalyseEtat.diff(avant, apres);
        avant = null;
        precedent = apres;
        if (changes.isEmpty()) return;
        double t = temps();
        evenements(t, changes);
        boolean reset = changes.stream().anyMatch(c -> (c.cle().equals("explosions") || c.cle().equals("bigBangs") || c.cle().equals("extraUniverses")
                || c.cle().equals("replaying") || c.cle().equals("cosmos")) && c.monte());
        if (reset) {
            for (AnalyseEtat.Changement c : changes) {
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
                poids.append(e.getKey().label()).append(" ×").append(facteur(delta));
                poidsMax = Math.max(poidsMax, delta);
            }
        }
        Double moteurAvant = grandeursAvant == null ? null : grandeursAvant.get(moteur());
        Double moteurApres = grandeursApres.get(moteur());
        double deltaMoteur = moteurAvant != null && moteurApres != null ? moteurApres - moteurAvant : 0;
        // Le temps qu'il restait avant le palier visé, selon la dernière estimation, raccourci du gain de la grandeur qui fait avancer.
        double reste = Double.isNaN(annonceAjustee) ? Double.NaN : Math.max(0, annonceAjustee - t);
        double gain = Double.isNaN(reste) || deltaMoteur <= 0 ? 0 : reste * (1 - Math.pow(10, -deltaMoteur));

        // Ce qui a été payé, par monnaie.
        Map<String, AnalyseEtat.Nombre> payes = new LinkedHashMap<>();
        Map<String, AnalyseEtat.Nombre> reserves = new LinkedHashMap<>();
        for (AnalyseEtat.Changement c : changes) {
            if (c.sousCle() == null && AnalyseNoms.ressource(c.cle()) && c.baisse()) {
                payes.put(c.cle(), c.avant().minus(c.apres()));
                reserves.put(c.cle(), c.avant());
            }
            if (c.sousCle() == null && c.cle().equals("voidSpent") && c.monte()) {
                payes.put("voidSpent", c.apres().minus(c.avant()));
            }
        }
        // Ce qui a été obtenu.
        List<Acquis> acquis = new ArrayList<>();
        List<String> elements = new ArrayList<>();
        for (AnalyseEtat.Changement c : changes) {
            String key = c.cle();
            if (AnalyseNoms.ressource(key) || key.equals("voidSpent") || key.equals("darkMatterSpent") || key.equals("totalAtoms")) continue;
            if (REGLAGES.containsKey(key) || key.equals("enabledAutomations") || key.equals("enabledDarkAutomations")
                    || key.equals("dimensionAutomationsOff") || key.equals("achievements") || key.equals("titles")) continue;
            if (key.equals("treeBubbles") && c.monte()) {
                for (String bubble : AnalyseNoms.bulles((long) c.avant().toDouble(), (long) c.apres().toDouble())) {
                    acquis.add(new Acquis("Bulle de l'arbre", bubble, 1, "voidSpent", Double.NaN));
                }
                continue;
            }
            if (key.equals("elements") && c.monte()) {
                elements.add(AnalyseNoms.chose(c.sousCle()) + (c.apres().toDouble() - c.avant().toDouble() > 1 ? " ×" + (long) (c.apres().toDouble() - c.avant().toDouble()) : ""));
                continue;
            }
            // Les compteurs d'actions (fusions, synthèses, impulsions, molécules…) ne sont pas des achats : les grandeurs les suivent.
            if (!achat(key)) continue;
            if (c.ajoute() != null) {
                acquis.add(new Acquis(AnalyseNoms.cle(key), AnalyseNoms.chose(c.ajoute()), 1, monnaie(key, c.ajoute()), coutRelatif(key, c.ajoute(), 0, 1)));
            } else if (c.monte()) {
                double from = c.avant().toDouble(), to = c.apres().toDouble();
                String chose = c.sousCle() == null ? "" : AnalyseNoms.chose(c.sousCle());
                acquis.add(new Acquis(AnalyseNoms.cle(key), chose, to - from, monnaie(key, c.sousCle()), coutRelatif(key, c.sousCle(), (int) from, (int) to)));
            }
        }
        if (!elements.isEmpty()) {
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.OBTENU, "Éléments synthétisés : " + String.join(", ", elements));
        }
        if (!payes.isEmpty() || !acquis.isEmpty()) derniereActivite = t;
        if (acquis.isEmpty()) return;
        if (gain > 0) annonceAjustee -= gain;
        // Chaque monnaie payée se répartit entre ses achats, au prorata de leur prix estimé (sinon à parts égales).
        for (Acquis a : acquis) {
            AnalyseEtat.Nombre total = a.monnaie == null ? null : payes.get(a.monnaie);
            String montant = "—", paye = a.monnaie == null ? "—" : AnalyseNoms.cle(a.monnaie);
            double part = Double.NaN, montantLog = Double.NaN;
            if (total != null && total.signe() > 0) {
                List<Acquis> memes = acquis.stream().filter(x -> a.monnaie.equals(x.monnaie)).toList();
                double share;
                if (memes.stream().allMatch(x -> Double.isFinite(x.coutLog))) {
                    double max = memes.stream().mapToDouble(x -> x.coutLog).max().orElse(0), sum = 0;
                    for (Acquis x : memes) sum += Math.pow(10, x.coutLog - max);
                    share = Math.pow(10, a.coutLog - max) / sum;
                } else {
                    share = 1.0 / memes.size();
                }
                montantLog = total.log10() + Math.log10(share);
                montant = new AnalyseEtat.Nombre(1, montantLog).texte() + (memes.size() > 1 ? " (lot de " + memes.size() + ")" : "");
                AnalyseEtat.Nombre reserve = reserves.get(a.monnaie);
                part = reserve == null || reserve.signe() <= 0 ? Double.NaN : Math.pow(10, montantLog - reserve.log10());
            }
            int lot = acquis.size();
            String texte = (lot > 1 ? "(lot de " + lot + " achats) " : "") + (poids.length() == 0 ? "aucun effet mesuré sur les grandeurs" : poids);
            // La rentabilité : le temps de production que l'achat a coûté, contre le temps qu'il fait gagner.
            Double revenu = a.monnaie == null ? null : revenuLog.get(a.monnaie);
            double cout = revenu == null || !Double.isFinite(montantLog) ? Double.NaN : Math.pow(10, montantLog - revenu);
            double gagne = Double.isNaN(reste) ? Double.NaN : gain / lot;
            double rentabilite = Double.isFinite(cout) && cout > 0 && Double.isFinite(gagne) ? gagne / cout : Double.NaN;
            journal.achats.add(new AnalyseJournal.Achat(t, univers(), a.groupe, a.chose, a.quantite, paye, montant, part, montantLog, texte,
                    poidsMax / lot, gain / lot, cout, rentabilite));
            List<String> details = new ArrayList<>();
            details.add("Payé : " + montant + " " + paye + (Double.isNaN(part) ? "" : String.format(Locale.FRANCE, " (%.1f %% de la réserve)", 100 * part)));
            details.add("Poids : " + texte);
            if (gain > 0) details.add("Gain de temps estimé vers « " + visee + " » : " + AnalyseJournal.duree(gain / lot));
            if (Double.isFinite(cout)) details.add(rentabiliteTexte(cout, gagne, paye));
            long niveaux = Math.round(a.quantite);
            String quoi = a.groupe + (a.chose.isEmpty() ? "" : " — " + a.chose) + (niveaux > 1 ? " ×" + niveaux : "");
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.ACHAT, quoi, details);
        }
    }

    /** « Coûte 3 min 10 s de production de particules ; en fait gagner 12 min : ×3,8, rentable avant le palier. » */
    static String rentabiliteTexte(double cout, double gagne, String paye) {
        String out = "Rentabilité : coûte " + AnalyseJournal.duree(cout) + " de production (" + paye + ")";
        if (!Double.isFinite(gagne)) return out + " ; gain de temps inconnu (pas encore d'estimation).";
        double r = cout > 0 ? gagne / cout : Double.POSITIVE_INFINITY;
        String ratio = r >= 1000 ? "×" + facteur(Math.log10(r)) : String.format(Locale.FRANCE, "×%.2f", r);
        return out + " ; en fait gagner " + AnalyseJournal.duree(gagne) + " : " + ratio
                + (r >= 1 ? ", rentable avant le palier." : ", pas remboursé avant le palier (il sert peut-être plus loin).");
    }

    /** Une explosion : la matière noire qu'elle rapporte, par heure depuis la précédente, et pourquoi maintenant. */
    private void explosion(double t, long numero, List<AnalyseEtat.Changement> changes) {
        List<String> details = new ArrayList<>();
        for (AnalyseEtat.Changement c : changes) {
            if (!c.cle().equals("darkMatter") || c.sousCle() != null || !c.monte()) continue;
            AnalyseEtat.Nombre gained = c.apres().minus(c.avant());
            double depuis = t - derniereExplosion;
            details.add("Matière noire : +" + gained.texte() + " (réserve " + c.avant().texte() + " → " + c.apres().texte() + ")");
            if (depuis > 0 && gained.signe() > 0) {
                revenuLog.put("darkMatter", gained.log10() - Math.log10(depuis));
                details.add("Soit " + new AnalyseEtat.Nombre(1, gained.log10() + Math.log10(3600 / depuis)).texte() + " par heure depuis "
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

    private static String facteur(double log10) {
        if (Math.abs(log10) < 3) return String.format(Locale.FRANCE, "%.3f", Math.pow(10, log10));
        return String.format(Locale.ROOT, "10^%.2f", log10);
    }

    /**
     * Les automatismes mis en marche ou coupés d'un même pas. Ceux qu'une remise à zéro a coupés ({@code cause}) se
     * notent en une ligne, avec cette cause : ce n'est pas le bot qui les a coupés. Trois ou plus d'un coup, aussi en une
     * ligne, avec ce que fait chacun.
     */
    private void automatismes(double t, List<AnalyseEtat.Changement> changes, String cause) {
        List<String> on = new ArrayList<>(), off = new ArrayList<>();
        for (AnalyseEtat.Changement c : changes) {
            String id = c.ajoute() != null ? c.ajoute() : c.retire();
            boolean marche = c.cle().equals("dimensionAutomationsOff") ? c.retire() != null : c.ajoute() != null;
            (marche ? on : off).add(id);
        }
        if (!off.isEmpty() && cause != null) {
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.AUTOMATISME, "Coupés par " + cause + " : " + noms(off),
                    List.of("Ce n'est pas le bot : " + cause + " remet les automatismes à zéro ; il les remet en marche dès qu'il peut."));
            off.clear();
        }
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
                if (!role.isEmpty()) details.add(AnalyseNoms.chose(id) + " : il " + role + ".");
            }
            details.add(marche ? pourquoiMarche() : pourquoiCoupe());
            journal.ajoute(t, univers(), AnalyseJournal.Sorte.AUTOMATISME, (marche ? "Mis en marche : " : "Coupés : ") + noms(ids), details);
        }
    }

    private static String noms(List<String> ids) {
        return String.join(", ", ids.stream().map(AnalyseNoms::chose).toList());
    }

    private static String pourquoiMarche() {
        return "Pourquoi : il fait ce geste plus souvent et sans oubli ; le bot garde ses propres décisions pour le reste.";
    }

    private String pourquoiCoupe() {
        String defi = defiActif(g);
        return "Pourquoi : il dépenserait ce que le bot met de côté" + (defi != null ? " pendant le défi « " + AnalyseNoms.chose(defi) + " »"
                : " pour « " + (visee.isEmpty() ? nomPalier(indexPrincipal + 1) : visee) + " »") + " (goulot : " + goulot() + ").";
    }

    private void automatisme(double t, String id, boolean on) {
        String name = AnalyseNoms.chose(id);
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

    private record Acquis(String groupe, String chose, double quantite, String monnaie, double coutLog) {}

    /** Les clés où ce qui monte a été acheté (version 0.162 : les autres, des compteurs d'actions, ne sont plus pris pour des achats). */
    static final java.util.Set<String> ACHATS = java.util.Set.of("upgradeLevels", "ownedAutomations", "automationSpeedLevels", "darkUpgradeLevels",
            "spaceUpgrades", "boughtMilestones", "colliderGeneratorsBought", "colliderUpgrades", "probeUpgrades", "dimensionAutomations",
            "hadrons", "universeUpgrades", "tableWeightLevel", "voidOpen", "hadronsOpen", "unlockedSkins");

    /** Vrai si ce qui monte à cette clé a été acheté. */
    static boolean achat(String key) {
        if (ACHATS.contains(key)) return true;
        for (String prefix : List.of("voidLevel", "treeBubbleLevel", "line", "planeLevel", "volumeLevel", "durationLevel", "loopLevel", "braneLevel",
                "clickerLevel", "gridLevel", "mirrorLevel", "mixStageLevel", "seedUpgrade")) {
            if (key.startsWith(prefix) && (key.endsWith("Level") || Character.isDigit(key.charAt(key.length() - 1)))) return true;
        }
        return false;
    }

    /** Les réglages : ce que le joueur règle sans rien acheter. Ils se notent comme des choix, pas comme des achats. */
    static final Map<String, String> REGLAGES = Map.ofEntries(
            Map.entry("autoMoleculeReserve", "réserve des molécules automatiques"), Map.entry("voidShare", "part laissée au vide"),
            Map.entry("cycleChoice", "choix de cycle"), Map.entry("chosenSkins", "apparence choisie"), Map.entry("automationPaused", "tout couper"),
            Map.entry("chronoSpeed", "accélération du chrono"), Map.entry("stateCaps", "plafond des états de la matière"),
            Map.entry("systemPlan", "plan du système"), Map.entry("darkPlan", "plan de l'arbre de matière noire"), Map.entry("plannedBodies", "astres prévus"),
            Map.entry("autoHold", "réserve automatique"), Map.entry("autoMolecules", "molécules automatiques"), Map.entry("autoFollowList", "suivre la liste"),
            Map.entry("autoFollowMatter", "suivre la matière"), Map.entry("autoGather", "rassemblement automatique"), Map.entry("autoForm", "formation automatique"),
            Map.entry("favoriteMolecules", "molécules favorites"), Map.entry("recipeReservation", "réservation des recettes"),
            Map.entry("holdLocked", "réserve verrouillée"), Map.entry("colliderPaused", "collisionneur en pause"));

    private static final String[] RESSOURCE_DIMENSION = {"universeSeeds", "dimensionShards", "planeShards", "volumeShards", "durationShards",
        "loopShards", "braneShards", "clickerShards", "gridShards", "mirrorShards", "mixShards"};

    /** La ressource qui paie ce qui est monté à cette clé : sa clé dans la sauvegarde, ou {@code null} si inconnue. */
    static String monnaie(String key, String sub) {
        if (key.equals("upgradeLevels")) {
            for (Upgrade u : Upgrades.DEFAULT) if (u.id().equals(sub)) return u.resource() == idle.core.Resource.PARTICLES ? "particles" : "atoms";
            return null;
        }
        if (key.equals("darkUpgradeLevels")) {
            // L'arbre se paie selon la branche : particules, atomes ou matière noire ; la taille s'atteint sans payer.
            for (var d : idle.core.darkmatter.DarkUpgrades.DEFAULT) {
                if (!d.id().equals(sub)) continue;
                return switch (d.branch()) {
                    case PARTICLES -> "particles";
                    case ATOMS -> "atoms";
                    case SIZE -> null;
                    case DARK_MATTER -> "darkMatter";
                };
            }
            return null;
        }
        // Les automatismes ordinaires et leur cadence se paient en atomes.
        if (key.equals("ownedAutomations") || key.equals("automationSpeedLevels")) return "atoms";
        if (key.equals("spaceUpgrades")) return "space";
        if (key.startsWith("collider")) return "colliderEnergy";
        if (key.startsWith("voidLevel")) return "voidEnergy";
        if (key.startsWith("treeBubble")) return "voidSpent";
        if (key.startsWith("line")) return "dimensionShards";
        if (key.equals("hadrons")) return "quarks";
        String[][] dims = {{"planeLevel", "planeShards"}, {"volumeLevel", "volumeShards"}, {"durationLevel", "durationShards"},
            {"loopLevel", "loopShards"}, {"braneLevel", "braneShards"}, {"clickerLevel", "clickerShards"}, {"gridLevel", "gridShards"},
            {"mirrorLevel", "mirrorShards"}, {"mixStageLevel", "mixShards"}};
        for (String[] d : dims) if (key.startsWith(d[0])) return d[1];
        if (key.startsWith("seedUpgrade")) {
            int i = Integer.parseInt(key.substring("seedUpgrade".length()));
            return RESSOURCE_DIMENSION[Game.SeedUpgrade.values()[i].dimension()];
        }
        if (key.equals("dimensionAutomations") && sub != null) {
            try {
                return RESSOURCE_DIMENSION[DimensionAutomation.valueOf(sub).paidWith()];
            } catch (IllegalArgumentException unknown) {
                return null;
            }
        }
        return null;
    }

    /** Le prix relatif (en ordres de grandeur) des niveaux {@code from} à {@code to} : pour répartir un lot ; NaN si inconnu. */
    static double coutRelatif(String key, String sub, int from, int to) {
        try {
            if (key.equals("upgradeLevels")) {
                for (Upgrade u : Upgrades.DEFAULT) if (u.id().equals(sub)) return somme(u.baseCost().log10(), Math.log10(u.costGrowth()), from, to);
            }
            if (key.equals("ownedAutomations")) {
                for (var a : idle.core.upgrades.Automations.DEFAULT) if (a.id().equals(sub)) return a.cost().log10();
            }
            if (key.equals("automationSpeedLevels")) {
                for (var a : idle.core.upgrades.Automations.DEFAULT) if (a.id().equals(sub)) return somme(a.speedBaseCost().log10(), Math.log10(a.speedCostGrowth()), from, to);
            }
            if (key.equals("darkUpgradeLevels")) {
                for (var d : idle.core.darkmatter.DarkUpgrades.DEFAULT) if (d.id().equals(sub)) return somme(d.baseCost().log10(), Math.log10(d.costGrowth()), from, to);
            }
            if (key.equals("colliderGeneratorsBought")) {
                var gen = idle.core.collider.ColliderGenerator.valueOf(sub);
                return Math.log10(gen.cost(from)) + Math.log10(Math.max(1, to - from));
            }
            if (key.equals("colliderUpgrades")) return Math.log10(idle.core.collider.ColliderUpgrade.valueOf(sub).cost(from));
            if (key.equals("dimensionAutomations")) return Math.log10(DimensionAutomation.valueOf(sub).cost());
            if (key.startsWith("treeBubbleLevel")) {
                TreeBubble bubble = TreeBubble.values()[Integer.parseInt(key.substring("treeBubbleLevel".length()))];
                return Math.log10(bubble.costAt(from));
            }
            idle.core.GameUniverse.Priced priced = priced(key);
            if (priced != null) return Math.log10(priced.cost(from)) + Math.log10(Math.max(1, to - from));
        } catch (RuntimeException unknown) {
            return Double.NaN;
        }
        return Double.NaN;
    }

    private static double somme(double baseLog, double growthLog, int from, int to) {
        // Une suite géométrique : la somme est dominée par son dernier terme.
        int last = Math.max(from, to - 1);
        return baseLog + growthLog * last;
    }

    private static idle.core.GameUniverse.Priced priced(String key) {
        String[][] prefixes = {{"planeLevel"}, {"volumeLevel"}, {"durationLevel"}, {"loopLevel"}, {"braneLevel"}, {"clickerLevel"},
            {"gridLevel"}, {"mirrorLevel"}, {"mixStageLevel"}, {"voidLevel"}, {"seedUpgrade"}};
        Object[][] enums = {Game.PlaneUpgrade.values(), Game.VolumeUpgrade.values(), Game.DurationUpgrade.values(), Game.LoopUpgrade.values(),
            Game.BraneUpgrade.values(), Game.ClickerUpgrade.values(), Game.GridUpgrade.values(), Game.MirrorUpgrade.values(),
            Game.MixUpgrade.values(), Game.VoidUpgrade.values(), Game.SeedUpgrade.values()};
        for (int i = 0; i < prefixes.length; i++) {
            String p = prefixes[i][0];
            if (key.startsWith(p) && key.length() > p.length()) {
                int index = Integer.parseInt(key.substring(p.length()));
                return (idle.core.GameUniverse.Priced) enums[i][index];
            }
        }
        String[][] line = {{"lineYieldLevel", "YIELD"}, {"lineTicksLevel", "TICKS"}, {"lineSpeedLevel", "SPEED"}, {"lineBallsLevel", "BALLS"},
            {"lineCountLevel", "LINES"}};
        for (String[] l : line) if (key.equals(l[0])) return Game.LineUpgrade.valueOf(l[1]);
        return null;
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
        Game cg = Bots.game(copy, graine + 7919L * (long) temps());
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
        IdleProfiles24 clone = new IdleProfiles24(copie, ANALYSTE, graine, pas);
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

    private void estimer() {
        if (NextUniversePolicy.reached(g, targetUniverses)) return;
        int cible = indexPrincipal + 1 + ((indexPrincipal + 1) % 7 == 0 ? 1 : 0);
        // Entre le premier et le dernier Big Bang, le prochain palier est le Big Bang suivant.
        int bang = g.bigBangs() >= 1 && g.bigBangs() + 1 < Bots.LAST_BIG_BANG && etape(g) == 2 ? g.bigBangs() + 1 : 0;
        visee = bang > 0 ? "Univers " + univers() + " : Big Bang n° " + bang : nomPalier(cible);
        double secondes = essai(cible, bang, HORIZON);
        String goulot = goulot();
        journal.estimations.add(new AnalyseJournal.Estimation(temps(), visee, secondes, goulot));
        if (secondes < 0) {
            estimationCourante = Double.NaN;
            bloque("Plus de " + AnalyseJournal.duree(HORIZON) + " estimées avant « " + visee + " » en jouant la suite sur une copie");
            return;
        }
        estimationCourante = secondes;
        annonceAbsolue = temps() + secondes;
        annonceAjustee = annonceAbsolue;
        annonceVisee = visee;
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.ESTIMATION, "Prochain palier « " + visee + " » dans " + AnalyseJournal.duree(secondes)
                + " (vers " + AnalyseJournal.chrono(temps() + secondes) + ")", List.of("Goulot : " + goulot));
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
        details.add("Le temps du bot (une décision toutes les " + (int) ANALYSTE.actionEverySeconds() + " s) s'écrit au palier suivant, dans etsi.csv et le rapport.");
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
        journal.ajoute(0, 1, AnalyseJournal.Sorte.INFO, "Début de la partie", List.of("Profil : le Speedrunner, une décision toutes les "
                + (int) ANALYSTE.actionEverySeconds() + " s ; pas de " + (int) step + " s ; graine " + graine + " ; " + targetUniverses + " univers visés"));
        begin();
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
                : NextUniversePolicy.reached(g, targetUniverses) ? "Objectif atteint : " + targetUniverses + " univers" : "Limite de " + heures + " h atteinte";
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, fin, List.of(String.format(Locale.FRANCE, "Calcul : %.0f s de temps réel", seconds)));
        journal.ecrire(dossier);
        Files.writeString(dossier.resolve("fin.save"), SaveCodec.write(g.state(), 0));
        AnalyseRapport.ecrire(this, dossier.resolve("rapport.html"), fin);
        System.out.println("ANALYSTE," + fin + "," + AnalyseJournal.chrono(temps()) + "," + dossier.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage : BotAnalyste <dossier> [univers=11] [heures=200] [graine=201] [pas=5] [estimation_min=30] [comparer=false] [etsi=true]");
            System.exit(2);
        }
        Path dossier = Path.of(args[0]);
        int univers = args.length > 1 ? Integer.parseInt(args[1]) : 11;
        double heures = args.length > 2 ? Double.parseDouble(args[2]) : 200;
        long graine = args.length > 3 ? Long.parseLong(args[3]) : 201;
        double pas = args.length > 4 ? Double.parseDouble(args[4]) : 5;
        double estimation = args.length > 5 ? Double.parseDouble(args[5]) : 30;
        boolean comparer = args.length > 6 && Boolean.parseBoolean(args[6]);
        boolean etsi = args.length <= 7 || Boolean.parseBoolean(args[7]);
        BotAnalyste bot = new BotAnalyste(Bots.fresh(graine), graine, pas, dossier, estimation);
        bot.targetUniverses = univers;
        bot.comparer = comparer;
        bot.etsi = etsi;
        bot.joue(heures);
    }
}
