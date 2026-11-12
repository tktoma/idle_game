import idle.core.Game;
import idle.core.GameState;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * À partir de quand une chose ne sert plus (version 0.204) : mesuré, pas estimé. La partie de référence se joue une fois
 * (Speedrunner, graine 201) et s'arrête un instant à chaque jalon (début, première explosion, Big Bangs 1, 5 et 8, galaxie, amas) :
 * l'état de la partie et la mémoire du bot y sont gardés. Depuis chaque jalon, la suite se rejoue sur une copie, une fois
 * telle quelle (le témoin) et une fois par chose coupée à partir de là : un automatisme interdit d'allumage
 * ({@link GameState#testBlock}), une amélioration qui ne s'achète plus ({@link Bots#game(GameState, long, Set)}), une
 * mécanique que le bot ne joue plus. L'écart d'arrivée avec le témoin dit ce que la chose rapportait encore à partir de ce
 * jalon ; le premier jalon d'où tous les écarts restent sous la tolérance est celui d'où elle ne sert plus.
 *
 * <p>Le témoin, pas la référence : une copie reprend avec un autre hasard de synthèse que la partie d'origine, et arrive
 * à une heure un peu différente (depuis le tout début, à 40 min près). La tolérance est le plus grand de 2 min et de
 * 0,5 % du temps qui restait. Un écart de plus de 30 s qui n'est pas au plafond se rejoue avec un second hasard, témoin
 * compris : une chose n'est dite inutile, ou gênante, que si les deux hasards le disent.
 *
 * <p>Une coupe qui retarde s'arrête dès qu'elle dépasse le témoin d'un plafond (30 min, ou 5 % du temps qui restait) :
 * son écart est alors noté « plus de ». En remontant les jalons, deux retards de suite au plafond arrêtent la remontée :
 * la chose sert, les jalons d'avant ne sont pas mesurés.
 *
 * <p>Usage : {@code AnalyseArrets <sortie> <neuf|sauvegarde> <univers visé> <familles ou identifiants, séparés par des
 * virgules> [fils]}. Familles : {@code automatismes}, {@code ameliorations}, {@code arbre}, {@code dimensions},
 * {@code mecaniques}. Résultat dans {@code arrets.txt} (le tableau) et {@code arrets.csv} (chaque mesure).
 */
final class AnalyseArrets {

    private AnalyseArrets() {}

    static final long GRAINE = 201;
    static final double PAS = 20;
    /** Au-delà de cet écart (en secondes), une mesure se rejoue avec un second hasard. */
    static final double CONFIRMER = 30;
    private static final PrintStream SILENCE = new PrintStream(OutputStream.nullOutputStream());

    /** Une chose à couper : ce qui ne s'achète plus ({@code sans}), et ce qu'on règle sur la copie avant de la jouer. */
    record Coupure(String id, String nom, String famille, Set<String> sans, Consumer<IdleProfiles24> regler) {}

    /** Un arrêt de la partie de référence : son nom, l'état sauvegardé, le bot figé (sa mémoire), l'heure de jeu. */
    /**
     * Version 0.207 : la mesure courte ({@code -Didle.arretsCourt=true}). En remontant les jalons, elle s'arrête au premier
     * où la coupe retarde (les deux hasards d'accord, ou le plafond) : elle donne le seuil, pas les jalons d'avant. Pour les
     * univers longs, où rejouer chaque coupe depuis chaque jalon coûte des jours de calcul. Le second hasard n'y est joué que
     * pour un écart qui dépasse la tolérance du jalon (au lieu de 30 s).
     */
    static final boolean COURT = Boolean.getBoolean("idle.arretsCourt");

    /** Version 0.207 : nombre de jalons gardés en fin d'univers ({@code -Didle.arretsJalons}) ; 0 : tous. */
    static final int DERNIERS = Integer.getInteger("idle.arretsJalons", 0);

    record Jalon(String nom, String etat, IdleProfiles24 bot, double jeu) {}

    /** Une mesure : l'heure d'arrivée ({@code +∞} au plafond), ou NaN si la partie s'est arrêtée sans arriver. */
    record Mesure(String coupure, int jalon, double fin) {}

    // ------------------------------------------------------------------
    // Les familles
    // ------------------------------------------------------------------

    private static Coupure bloque(String id, String nom, String cle, Consumer<Game> eteindre) {
        return new Coupure(id, nom, "automatismes", Set.of(), bot -> {
            bot.g.state().testBlock(cle);
            eteindre.accept(bot.g);
        });
    }

    /** Tous les automatismes du jeu : ceux de l'onglet, ceux de matière noire, le collisionneur, la matière, l'appui. */
    static List<Coupure> automatismes(Game catalogue) {
        List<Coupure> list = new ArrayList<>();
        for (Automation a : catalogue.automations()) {
            list.add(bloque(a.id(), a.name(), a.id(), g -> g.state().setAutomationEnabled(a.id(), false)));
        }
        for (DarkAutomation a : catalogue.darkAutomations()) {
            list.add(bloque(a.id(), "Matière noire : " + a.name(), a.id(), g -> g.state().setDarkAutomationEnabled(a.id(), false)));
        }
        list.add(bloque("auto_collisionneur", "Automates du collisionneur", "collider", g -> { }));
        list.add(bloque("auto_rassemblement", "Rassemblement automatique", "gather", g -> g.state().setAutoGather(false)));
        list.add(bloque("auto_formation", "Formation automatique des astres", "form", g -> g.state().setAutoForm(false)));
        list.add(bloque("auto_molecules", "Création automatique des molécules", "molecules", g -> g.state().setAutoMolecules(false)));
        list.add(bloque("auto_appui", "Appui automatique", "hold", g -> g.state().setAutoHold(false)));
        return list;
    }

    /** Les automatismes des dimensions, une coupure par dimension. */
    static List<Coupure> dimensions() {
        List<Coupure> list = new ArrayList<>();
        for (int d = 1; d <= 10; d++) {
            int n = d;
            StringBuilder noms = new StringBuilder();
            for (DimensionAutomation a : DimensionAutomation.values()) if (a.dimension() == n) noms.append(noms.length() == 0 ? "" : ", ").append(a.name());
            list.add(new Coupure("dimension_" + n, "Automatismes de la dimension " + n, "dimensions", Set.of(), bot -> bot.g.state().testBlock("dimension:" + n)));
        }
        return list;
    }

    /** Les améliorations ordinaires (particules, atomes) : chacune ne s'achète plus à partir du jalon. */
    static List<Coupure> ameliorations(Game catalogue) {
        List<Coupure> list = new ArrayList<>();
        for (Upgrade u : catalogue.upgrades()) list.add(new Coupure(u.id(), u.name(), "ameliorations", Set.of(u.id()), bot -> { }));
        return list;
    }

    /** Les cases de l'arbre de matière noire : chacune ne s'achète plus à partir du jalon. */
    static List<Coupure> arbre(Game catalogue) {
        List<Coupure> list = new ArrayList<>();
        for (DarkUpgrade u : catalogue.darkUpgrades()) list.add(new Coupure(u.id(), "Arbre : " + u.name(), "arbre", Set.of(u.id()), bot -> { }));
        return list;
    }

    /** Un lot : plusieurs coupures faites ensemble, comptées comme une seule (version 0.207). */
    private static Coupure lot(String id, String nom, List<Coupure> membres) {
        Set<String> sans = new java.util.LinkedHashSet<>();
        for (Coupure c : membres) sans.addAll(c.sans());
        return new Coupure(id, nom, "lots", Set.copyOf(sans), bot -> { for (Coupure c : membres) c.regler().accept(bot); });
    }

    /**
     * Version 0.207 : les coupures par lots, pour mesurer tous les univers sans rejouer chaque chose une à une (34 coupures
     * par univers, c'est des jours de calcul). Un lot qui ne retarde rien dit que rien de ce qu'il contient ne sert ; un
     * lot qui retarde demande d'être détaillé, chose par chose, sur cet univers-là.
     */
    static List<Coupure> lots(Game catalogue) {
        Map<String, Coupure> autos = new LinkedHashMap<>();
        for (Coupure c : automatismes(catalogue)) autos.put(c.id(), c);
        List<Coupure> noire = new ArrayList<>(), particules = new ArrayList<>(), atomes = new ArrayList<>();
        for (DarkAutomation a : catalogue.darkAutomations()) noire.add(autos.get(a.id()));
        for (Coupure c : ameliorations(catalogue)) (c.id().startsWith("atom_") ? atomes : particules).add(c);
        List<Coupure> list = new ArrayList<>();
        list.add(lot("lot_auto_particules", "Automatismes des particules (vitesse, couplage, générateur)",
                List.of(autos.get("auto_speed"), autos.get("auto_coupling"), autos.get("auto_generator"))));
        list.add(lot("lot_auto_atomes", "Automatismes des atomes (fusion, synthèse)", List.of(autos.get("auto_fusion"), autos.get("auto_synthesis"))));
        list.add(lot("lot_auto_noire", "Automatismes de matière noire (les " + noire.size() + ")", noire));
        list.add(lot("lot_auto_ciel", "Automatismes du ciel (collisionneur, rassemblement, formation, molécules, appui)",
                List.of(autos.get("auto_collisionneur"), autos.get("auto_rassemblement"), autos.get("auto_formation"), autos.get("auto_molecules"),
                        autos.get("auto_appui"))));
        list.add(lot("lot_ameliorations_particules", "Améliorations en particules (les " + particules.size() + ")", particules));
        list.add(lot("lot_ameliorations_atomes", "Améliorations en atomes (les " + atomes.size() + ")", atomes));
        list.add(lot("lot_arbre", "Arbre de matière noire (plus aucun achat)", arbre(catalogue)));
        list.add(lot("lot_dimensions", "Automatismes des dix dimensions", dimensions()));
        return list;
    }

    static List<Coupure> choisies(String liste) {
        Game catalogue = Bots.fresh(GRAINE);
        Map<String, Coupure> toutes = new LinkedHashMap<>();
        for (Coupure c : automatismes(catalogue)) toutes.put(c.id(), c);
        for (Coupure c : dimensions()) toutes.put(c.id(), c);
        for (Coupure c : ameliorations(catalogue)) toutes.put(c.id(), c);
        for (Coupure c : arbre(catalogue)) toutes.putIfAbsent(c.id(), c);
        for (Coupure c : AnalyseMecaniques.toutes()) toutes.putIfAbsent(c.id(), c);
        for (Coupure c : lots(catalogue)) toutes.putIfAbsent(c.id(), c);
        List<Coupure> list = new ArrayList<>();
        for (String mot : liste.split(",")) {
            String cle = mot.trim();
            if (cle.isEmpty()) continue;
            boolean famille = false;
            for (Coupure c : toutes.values()) {
                if (c.famille().equals(cle)) {
                    list.add(c);
                    famille = true;
                }
            }
            if (famille) continue;
            Coupure c = toutes.get(cle);
            if (c == null) throw new IllegalArgumentException("Ni famille ni identifiant : " + cle);
            list.add(c);
        }
        return list;
    }

    // ------------------------------------------------------------------
    // La partie de référence et ses jalons
    // ------------------------------------------------------------------

    private static IdleProfiles24 bot(Game game, int univers) {
        IdleProfiles24 bot = new IdleProfiles24(game, IdleProfiles24.PROFILES[0], GRAINE, PAS);
        bot.targetUniverses = univers;
        return bot;
    }

    /** Un bot figé sur une copie de la partie, avec la mémoire de celui-ci. */
    private static Jalon jalon(String nom, IdleProfiles24 source) throws Exception {
        String etat = SaveCodec.write(source.g.state(), 0);
        IdleProfiles24 fige = bot(Bots.game(SaveCodec.read(etat).state(), GRAINE), source.targetUniverses);
        AnalyseCopie.memoire(source, fige);
        fige.quiet = true;
        return new Jalon(nom, etat, fige, source.g.state().timePlayed());
    }

    /**
     * Joue la partie de référence jusqu'à l'univers visé et garde un jalon à chaque étape : le début, la première
     * explosion, les Big Bangs 1, 5 et le dernier, la galaxie et l'amas de chaque univers. Rend l'heure d'arrivée.
     */
    static double reference(Game game, int univers, List<Jalon> jalons, PrintStream journal) throws Exception {
        return reference(game, univers, jalons, journal, Map.of());
    }

    /** Version 0.207 : {@code memoire}, la mémoire du bot d'un jalon d'{@code UniversParalleles} (vide : un bot neuf). */
    static double reference(Game game, int univers, List<Jalon> jalons, PrintStream journal, Map<String, byte[]> memoire) throws Exception {
        IdleProfiles24 bot = bot(game, univers);
        bot.quiet = true;
        bot.begin();
        if (!memoire.isEmpty()) {
            AnalyseMemoire.remettre(bot, memoire);
            bot.targetUniverses = univers;
            bot.quiet = true;
        }
        jalons.add(jalon("U" + (game.universes() + 1) + " début", bot));
        int explosions = game.state().explosions(), bangs = game.bigBangs(), cosmos = RobotCosmosPlanner.cosmosStage(game), formes = game.universes();
        long limite = (long) (2000 * 3600 / PAS);
        for (long k = 0; k < limite; k++) {
            boolean fini = bot.stepOnce();
            int u = game.universes() + 1;
            String nom = null;
            if (game.universes() > formes && !fini) nom = "U" + u + " début";
            else if (explosions == 0 && game.state().explosions() > 0 && game.bigBangs() == 0) nom = "U" + u + " explosion 1";
            else if (game.bigBangs() > bangs && (game.bigBangs() == 1 || game.bigBangs() == 5 || game.bigBangs() == Bots.LAST_BIG_BANG)) {
                nom = "U" + u + " Big Bang " + game.bigBangs();
            } else if (cosmos < 1 && RobotCosmosPlanner.cosmosStage(game) == 1) nom = "U" + u + " galaxie";
            else if (cosmos < 2 && RobotCosmosPlanner.cosmosStage(game) == 2) nom = "U" + u + " amas";
            explosions = game.state().explosions();
            bangs = game.bigBangs();
            cosmos = RobotCosmosPlanner.cosmosStage(game);
            formes = game.universes();
            if (fini) return game.state().timePlayed();
            if (nom != null) {
                jalons.add(jalon(nom, bot));
                journal.printf(Locale.ROOT, "jalon %-22s %s%n", nom, AnalyseJournal.chrono(game.state().timePlayed()));
            }
        }
        return Double.NaN;
    }

    // ------------------------------------------------------------------
    // Une suite rejouée depuis un jalon
    // ------------------------------------------------------------------

    /** Rejoue la suite depuis ce jalon, {@code coupure} appliquée ({@code null} : le témoin) ; l'heure d'arrivée. */
    static double suite(Jalon jalon, Coupure coupure, long graine, double plafond) {
        try {
            GameState etat = SaveCodec.read(jalon.etat()).state();
            Game game = Bots.game(etat, graine, coupure == null ? Set.of() : coupure.sans());
            IdleProfiles24 bot = bot(game, jalon.bot().targetUniverses);
            AnalyseCopie.memoire(jalon.bot(), bot);
            bot.quiet = true;
            if (coupure != null) coupure.regler().accept(bot);
            long limite = (long) (2000 * 3600 / PAS);
            for (long k = 0; k < limite; k++) {
                if (bot.stepOnce()) return game.state().timePlayed();
                if (game.state().timePlayed() > plafond) return Double.POSITIVE_INFINITY;
            }
            return Double.NaN;
        } catch (Exception echec) {
            return Double.NaN;
        }
    }

    private static long graineDuJalon(int jalon, int tirage) {
        return GRAINE * 7919L + jalon * 104729L + tirage * 15485863L;
    }

    // ------------------------------------------------------------------
    // Le programme
    // ------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage : AnalyseArrets <sortie> <neuf|sauvegarde> <univers visé> <familles ou identifiants> [fils]");
            System.exit(2);
        }
        Path sortie = Path.of(args[0]);
        Files.createDirectories(sortie);
        int univers = Integer.parseInt(args[2]);
        List<Coupure> coupures = choisies(args[3]);
        int fils = args.length > 4 ? Integer.parseInt(args[4]) : Math.max(1, Runtime.getRuntime().availableProcessors());
        // Le journal s'écrit à l'écran et, ligne à ligne, dans journal.txt : une mesure dure, on la suit de là.
        PrintStream ecran = System.out;
        PrintStream fichier = new PrintStream(Files.newOutputStream(sortie.resolve("journal.txt")), true, "UTF-8");
        PrintStream journal = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                ecran.write(b);
                fichier.write(b);
            }

            @Override
            public void flush() {
                ecran.flush();
                fichier.flush();
            }
        }, true, "UTF-8");
        System.setOut(SILENCE);
        try {
            long depart = System.currentTimeMillis();
            Game game = args[1].equals("neuf") ? Bots.fresh(GRAINE) : Bots.load(Path.of(args[1]), GRAINE);
            List<Jalon> jalons = new ArrayList<>();
            // Version 0.207 : un jalon d'UniversParalleles en départ rend aussi la mémoire du bot (le fichier .bot à côté).
            Map<String, byte[]> memoire = args[1].equals("neuf") ? Map.of() : AnalyseMemoire.relire(Path.of(args[1]));
            double arrivee = reference(game, univers, jalons, journal, memoire);
            journal.printf(Locale.ROOT, "référence : arrivée à %s, %d jalons, %d s de calcul%n", AnalyseJournal.chrono(arrivee), jalons.size(),
                    (System.currentTimeMillis() - depart) / 1000);
            if (Double.isNaN(arrivee)) throw new IllegalStateException("La partie de référence n'arrive pas à l'univers " + univers);
            // Version 0.207 : -Didle.arretsJalons=N ne garde que les N derniers jalons (univers longs). « Ne sert à aucun
            // moment » se lit alors « ne sert déjà plus au premier jalon gardé ».
            if (DERNIERS > 0 && jalons.size() > DERNIERS) jalons.subList(0, jalons.size() - DERNIERS).clear();

            ExecutorService pool = Executors.newFixedThreadPool(fils);
            // Les témoins : un par jalon ; celui du second hasard ne se joue que si une coupure le demande.
            int n = jalons.size();
            double[] temoin = new double[n], tolerance = new double[n], plafond = new double[n];
            List<Future<Double>> attendus = new ArrayList<>();
            List<java.util.concurrent.FutureTask<Double>> seconds = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                int jj = j;
                attendus.add(pool.submit(() -> suite(jalons.get(jj), null, graineDuJalon(jj, 0), Double.MAX_VALUE)));
                seconds.add(new java.util.concurrent.FutureTask<>(() -> suite(jalons.get(jj), null, graineDuJalon(jj, 1), Double.MAX_VALUE)));
            }
            for (int j = 0; j < n; j++) {
                temoin[j] = attendus.get(j).get();
                double reste = temoin[j] - jalons.get(j).jeu();
                tolerance[j] = Math.max(120, 0.005 * reste);
                plafond[j] = temoin[j] + Math.max(1800, 0.05 * reste);
                journal.printf(Locale.ROOT, "témoin %-22s arrivée %s (référence %s), tolérance %.0f s%n", jalons.get(j).nom(),
                        AnalyseJournal.chrono(temoin[j]), AnalyseJournal.chrono(arrivee), tolerance[j]);
            }

            // Chaque coupure remonte les jalons, du dernier au premier ; deux retards de suite au plafond l'arrêtent. Un
            // écart qui n'est ni nul ni au plafond se rejoue avec un second hasard (le témoin aussi) : une coupe ne compte
            // comme inutile, ou comme gênante, que si les deux hasards le disent.
            Map<String, double[][]> ecarts = new LinkedHashMap<>();
            List<Future<double[][]>> lignes = new ArrayList<>();
            for (Coupure c : coupures) {
                lignes.add(pool.submit(() -> {
                    double[][] ecart = new double[2][n];
                    java.util.Arrays.fill(ecart[0], Double.NaN);
                    java.util.Arrays.fill(ecart[1], Double.NaN);
                    int auPlafond = 0;
                    boolean sert = false;
                    for (int j = n - 1; j >= 0 && auPlafond < 2 && !(COURT && sert); j--) {
                        double fin = suite(jalons.get(j), c, graineDuJalon(j, 0), plafond[j]);
                        ecart[0][j] = fin - temoin[j];
                        auPlafond = Double.isInfinite(fin) || Double.isNaN(fin) ? auPlafond + 1 : 0;
                        if (Double.isFinite(ecart[0][j]) && Math.abs(ecart[0][j]) > (COURT ? tolerance[j] : CONFIRMER)) {
                            seconds.get(j).run();
                            double second = seconds.get(j).get();
                            ecart[1][j] = suite(jalons.get(j), c, graineDuJalon(j, 1), second + (plafond[j] - temoin[j])) - second;
                        }
                        // Mesure courte : le premier jalon (en remontant) où la coupe retarde donne le seuil, on s'arrête là.
                        sert = auPlafond > 0 || ecart[0][j] > tolerance[j] && !(ecart[1][j] <= tolerance[j]);
                    }
                    return ecart;
                }));
            }
            for (int k = 0; k < coupures.size(); k++) {
                double[][] ecart = lignes.get(k).get();
                ecarts.put(coupures.get(k).id(), ecart);
                journal.printf(Locale.ROOT, "%3d/%d %-28s %s  (%d s)%n", k + 1, coupures.size(), coupures.get(k).id(),
                        verdict(ecart, tolerance, jalons), (System.currentTimeMillis() - depart) / 1000);
                // Le tableau se réécrit à chaque coupure mesurée : une mesure interrompue garde ce qu'elle a fait.
                double[] second = new double[n];
                for (int j = 0; j < n; j++) second[j] = seconds.get(j).isDone() ? seconds.get(j).get() : Double.NaN;
                ecrire(sortie, coupures.subList(0, k + 1), jalons, arrivee, temoin, second, tolerance, ecarts);
            }
            pool.shutdown();
        } finally {
            System.setOut(ecran);
            fichier.close();
        }
    }

    // ------------------------------------------------------------------
    // Le résultat
    // ------------------------------------------------------------------

    /** Le pire des deux hasards à chaque jalon (le second n'est joué que si le premier montrait un écart). */
    static double[] pire(double[][] ecart) {
        double[] pire = ecart[0].clone();
        for (int j = 0; j < pire.length; j++) if (!Double.isNaN(ecart[1][j])) pire[j] = Math.max(pire[j], ecart[1][j]);
        return pire;
    }

    /** Le premier jalon d'où couper ne coûte plus rien : tous les jalons mesurés à partir de lui sont sous la tolérance. */
    static int premierInutile(double[] ecart, double[] tolerance) {
        int premier = ecart.length;
        for (int j = ecart.length - 1; j >= 0; j--) {
            if (Double.isNaN(ecart[j]) || ecart[j] > tolerance[j]) break;
            premier = j;
        }
        return premier;
    }

    /** Les jalons d'où couper fait gagner du temps avec les deux hasards (le pire des deux reste sous −tolérance). */
    static List<Integer> gains(double[][] ecart, double[] tolerance) {
        List<Integer> list = new ArrayList<>();
        double[] pire = pire(ecart);
        for (int j = 0; j < pire.length; j++) if (!Double.isNaN(ecart[1][j]) && pire[j] < -tolerance[j]) list.add(j);
        return list;
    }

    static String verdict(double[][] ecart, double[] tolerance, List<Jalon> jalons) {
        double[] pire = pire(ecart);
        int premier = premierInutile(pire, tolerance);
        StringBuilder texte = new StringBuilder(premier == pire.length ? "sert encore au dernier jalon (" + jalons.get(pire.length - 1).nom() + ")"
                : premier == 0 ? "ne sert à aucun moment de cette partie" : "ne sert plus à partir de : " + jalons.get(premier).nom());
        List<Integer> gains = gains(ecart, tolerance);
        if (!gains.isEmpty()) {
            texte.append(" ; la couper fait gagner du temps à partir de :");
            for (int j : gains) texte.append(' ').append(jalons.get(j).nom()).append(" (").append(ecartLisible(pire[j])).append(")");
        }
        return texte.toString();
    }

    static String ecartLisible(double ecart) {
        if (Double.isNaN(ecart)) return "·";
        if (Double.isInfinite(ecart)) return "plafond";
        long s = Math.round(Math.abs(ecart));
        String signe = ecart < -0.5 ? "−" : ecart > 0.5 ? "+" : "";
        return signe + (s >= 3600 ? String.format(Locale.ROOT, "%dh%02d", s / 3600, (s / 60) % 60) : s >= 60 ? String.format(Locale.ROOT, "%dmin%02d", s / 60, s % 60) : s + "s");
    }

    static void ecrire(Path sortie, List<Coupure> coupures, List<Jalon> jalons, double arrivee, double[] temoin, double[] second, double[] tolerance,
            Map<String, double[][]> ecarts) throws Exception {
        int n = jalons.size();
        StringBuilder txt = new StringBuilder("À partir de quand chaque chose ne sert plus (mesuré : la suite rejouée sans elle depuis chaque jalon)\n");
        txt.append("Speedrunner, graine 201, décision toutes les 20 s. Arrivée de la partie de référence : ").append(AnalyseJournal.chrono(arrivee)).append("\n\n");
        txt.append("Les jalons (le témoin : la suite rejouée sans rien couper ; le second : avec un autre hasard, joué seulement s'il a fallu confirmer) :\n");
        for (int j = 0; j < n; j++) {
            txt.append(String.format(Locale.ROOT, "  %2d  %-22s à %s ; témoin %s ; second témoin %s ; tolérance %s%n", j + 1, jalons.get(j).nom(),
                    AnalyseJournal.chrono(jalons.get(j).jeu()), AnalyseJournal.chrono(temoin[j]), AnalyseJournal.chrono(second[j]), ecartLisible(tolerance[j])));
        }
        txt.append("\nL'écart d'arrivée quand la chose est coupée à partir du jalon (+ : plus tard ; « plafond » : arrêtée, trop en retard ; · : pas mesuré).\n");
        txt.append("Deux valeurs : les deux hasards. Une seule : le premier hasard ne montrait rien (moins de 30 s) ou était au plafond.\n\n");
        txt.append(String.format(Locale.ROOT, "%-24s", ""));
        for (int j = 0; j < n; j++) txt.append(String.format(Locale.ROOT, " %17d", j + 1));
        txt.append("   verdict\n");
        StringBuilder csv = new StringBuilder("famille,id,nom,jalon,heure_jalon_s,arrivee_temoin_s,ecart_s,ecart_second_hasard_s,tolerance_s,mesure\n");
        String famille = "";
        for (Coupure c : coupures) {
            double[][] ecart = ecarts.get(c.id());
            double[] pire = pire(ecart);
            if (!c.famille().equals(famille)) {
                famille = c.famille();
                txt.append("\n[").append(famille).append("]\n");
            }
            txt.append(String.format(Locale.ROOT, "%-24s", c.id().length() > 24 ? c.id().substring(0, 24) : c.id()));
            for (int j = 0; j < n; j++) {
                txt.append(String.format(Locale.ROOT, " %17s", ecartLisible(ecart[0][j]) + (Double.isNaN(ecart[1][j]) ? "" : " / " + ecartLisible(ecart[1][j]))));
            }
            txt.append("   ").append(verdict(ecart, tolerance, jalons)).append("  — ").append(c.nom()).append('\n');
            for (int j = 0; j < n; j++) {
                String mesure = Double.isNaN(pire[j]) ? "non mesuré" : Double.isInfinite(pire[j]) ? "plafond" : pire[j] > tolerance[j] ? "sert"
                        : pire[j] < -tolerance[j] && !Double.isNaN(ecart[1][j]) ? "gêne" : "ne sert plus";
                csv.append(String.format(Locale.ROOT, "%s,%s,\"%s\",\"%s\",%.0f,%.0f,%s,%s,%.0f,%s%n", c.famille(), c.id(), c.nom(), jalons.get(j).nom(),
                        jalons.get(j).jeu(), temoin[j], nombre(ecart[0][j]), nombre(ecart[1][j]), tolerance[j], mesure));
            }
        }
        Files.writeString(sortie.resolve("arrets.txt"), txt.toString());
        Files.writeString(sortie.resolve("arrets.csv"), csv.toString());
    }

    private static String nombre(double ecart) {
        return Double.isNaN(ecart) || Double.isInfinite(ecart) ? "" : String.format(Locale.ROOT, "%.0f", ecart);
    }
}
