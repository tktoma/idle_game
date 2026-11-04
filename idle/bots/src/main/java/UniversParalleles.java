import idle.core.Game;
import idle.core.save.SaveCodec;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Les univers d'une partie de référence joués en parallèle (version 0.170), pour qu'une simulation de onze univers ne
 * prenne plus onze fois le temps d'un univers.
 *
 * <p>Le principe : une partie jouée d'un trait écrit un <em>jalon</em> à chaque univers lancé (la sauvegarde du jeu et la
 * mémoire du bot, {@link AnalyseMemoire}, au moment où l'univers suivant démarre). Rejouée, chaque tranche d'univers
 * repart de son jalon ; les tranches sont indépendantes, et se jouent donc toutes en même temps, une par cœur. Comme le
 * moteur et les bots sont déterministes (0.162, 0.168), une tranche qui repart d'un jalon exact donne exactement la suite
 * de la partie : la chaîne se vérifie d'elle-même, jalon par jalon.
 *
 * <p>Après un changement du jeu ou des bots, les jalons de référence ne sont peut-être plus justes. L'outil les emploie
 * quand même, comme des <em>paris</em> : toutes les tranches partent en parallèle de leurs anciens jalons, puis le
 * jalon que produit la tranche {@code k} est comparé à celui dont la tranche {@code k + 1} est partie. S'ils sont
 * identiques (le changement n'a rien modifié jusque-là), la tranche {@code k + 1} était la bonne : son résultat est
 * acquis. Sinon, elle est rejouée depuis le nouveau jalon, et la vérification reprend, jusqu'à ce que toute la chaîne
 * tienne. Un changement qui n'agit qu'à partir de l'univers 8 ne coûte alors que les univers 8 à 11, et les quatre
 * ensemble ; un changement qui agit dès le début ne coûte pas plus cher qu'une partie d'un trait (chaque tranche repart
 * dès que la précédente finit, ce que l'outil fait aussi en l'absence de tout jalon).
 *
 * <p>Ce que l'outil vérifie : la sauvegarde <em>et</em> la mémoire du bot, octet pour octet. Une partie recomposée par
 * cet outil est donc exactement celle qu'aurait donnée une partie d'un trait, ou l'outil le dit.
 *
 * <pre>
 *   UniversParalleles jalons    &lt;dossier&gt; &lt;univers&gt; [pas=20] [graine=201] [profil=1] [heuresMax=2000]
 *   UniversParalleles parallele &lt;référence&gt; &lt;sortie&gt; &lt;univers&gt; [pas=20] [graine=201] [profil=1] [heuresMax=2000] [fils=cœurs]
 * </pre>
 *
 * <p>{@code jalons} joue d'un trait (comme {@code FreshUniverseAudit}, la même partie exactement) et écrit les jalons.
 * {@code parallele} rejoue depuis les jalons de {@code référence} (un dossier vide ou absent convient : tout se joue
 * alors en chaîne), écrit les nouveaux jalons dans {@code sortie} et le bilan de la chaîne dans {@code chaine.txt}. Les
 * fichiers : {@code apres_univers_k.save} et {@code .bot} (l'état au départ de l'univers {@code k + 1}), {@code fin.save}
 * et {@code .bot} (l'univers visé formé), {@code evenements.csv} (les événements de chaque tranche, en heures de jeu).
 */
public final class UniversParalleles {
    private UniversParalleles() {}

    /** Le nom du jalon écrit quand {@code formes} univers sont formés et le suivant lancé. */
    static String nomJalon(int formes) {
        return "apres_univers_" + formes + ".save";
    }

    /**
     * Une tranche de partie : le bot des profils ({@link IdleProfiles24}, le même que celui de {@code FreshUniverseAudit}),
     * qui écrit un jalon à chaque univers lancé et s'arrête quand l'univers {@code jusquA} est lancé (ou formé, s'il
     * est le dernier visé).
     */
    static final class Tranche extends IdleProfiles24 {
        final Path dossier;
        final int jusquA;
        final List<String> evenements = new ArrayList<>();
        /** Les jalons écrits par cette tranche : nombre d'univers formés → fichier. */
        final List<Path> jalons = new ArrayList<>();
        Path fin;

        Tranche(Game game, Profile profile, long seed, double step, Path dossier, int jusquA) {
            super(game, profile, seed, step);
            this.dossier = dossier;
            this.jusquA = jusquA;
        }

        @Override
        boolean afterStep(boolean done) {
            try {
                if (launchedNextUniverse) {
                    int formes = g.universes();
                    Path jalon = dossier.resolve(nomJalon(formes));
                    ecrire(jalon);
                    jalons.add(jalon);
                    evenements.add(String.format(Locale.ROOT, "%d,UNIVERS_LANCE,%.4f,%.4f", formes + 1, wallSeconds / 3600, g.state().timePlayed() / 3600));
                    if (formes + 1 > jusquA) return true;
                }
                if (done) {
                    fin = dossier.resolve("fin.save");
                    ecrire(fin);
                    evenements.add(String.format(Locale.ROOT, "%d,UNIVERS_FORME,%.4f,%.4f", g.universes(), wallSeconds / 3600, g.state().timePlayed() / 3600));
                }
            } catch (Exception failure) {
                throw new IllegalStateException("Jalon impossible à écrire : " + failure, failure);
            }
            return done;
        }

        void ecrire(Path save) throws Exception {
            Files.createDirectories(save.getParent());
            Files.writeString(save, SaveCodec.write(g.state(), 0));
            AnalyseMemoire.ecrire(this, save);
        }
    }

    /** Ce qu'une tranche a donné. */
    record Resultat(int univers, Path depart, String verdict, double heuresJeu, double secondes, List<String> evenements, Path jalon, String sortie) {}

    /**
     * Joue la tranche qui mène à l'univers {@code univers} : depuis une partie neuve ({@code depart} nul) ou depuis un
     * jalon (sa sauvegarde et sa mémoire), jusqu'à ce que l'univers suivant soit lancé, ou l'univers visé formé.
     */
    static Resultat jouer(int univers, Path depart, Path dossier, int cible, double pas, long graine, int profil, double heuresMax) {
        long debut = System.nanoTime();
        ByteArrayOutputStream texte = new ByteArrayOutputStream();
        SortieParFil.brancher(texte);
        Tranche tranche = null;
        String verdict;
        try {
            tranche = tranche(depart, dossier, univers, cible, pas, graine, profil);
            boolean fini = avancer(tranche, heuresMax);
            verdict = !fini ? "LIMITE" : tranche.fin != null ? "FIN" : "OK";
        } catch (Exception failure) {
            verdict = "ERREUR " + failure;
        } finally {
            SortieParFil.debrancher();
        }
        double secondes = (System.nanoTime() - debut) / 1e9;
        Path jalon = tranche == null ? null : tranche.fin != null ? tranche.fin : tranche.jalons.isEmpty() ? null : tranche.jalons.get(tranche.jalons.size() - 1);
        return new Resultat(univers, depart, verdict, tranche == null ? 0 : tranche.g.state().timePlayed() / 3600, secondes,
                tranche == null ? List.of() : List.copyOf(tranche.evenements), jalon, texte.toString(StandardCharsets.UTF_8));
    }

    /**
     * La tranche prête à jouer : depuis une partie neuve ({@code depart} nul) ou depuis un jalon, dont la sauvegarde
     * devient la partie et la mémoire du bot ({@code .bot} à côté) remplit le bot neuf, comme le fait l'analyste.
     */
    static Tranche tranche(Path depart, Path dossier, int univers, int cible, double pas, long graine, int profil) throws Exception {
        IdleProfiles24.Profile profile = IdleProfiles24.PROFILES[profil - 1];
        Game game = depart == null ? Bots.fresh(graine) : Bots.load(depart, graine);
        Tranche tranche = new Tranche(game, profile, graine, pas, dossier, univers);
        tranche.targetUniverses = cible;
        tranche.begin();
        if (depart != null) {
            Map<String, byte[]> memoire = AnalyseMemoire.relire(depart);
            if (memoire.isEmpty()) throw new IllegalStateException("Pas de mémoire du bot à côté de " + depart);
            AnalyseMemoire.remettre(tranche, memoire);
        }
        return tranche;
    }

    /** Joue au plus {@code heures} heures de plus ; vrai si la tranche s'est arrêtée d'elle-même (univers lancé ou formé). */
    static boolean avancer(Tranche tranche, double heures) {
        int count = (int) Math.ceil(heures * 3600 / tranche.step);
        for (int i = 0; i < count; i++) {
            if (tranche.stepOnce()) return true;
        }
        return false;
    }

    /** Vrai si deux jalons (sauvegarde et mémoire du bot) sont identiques octet pour octet ; faux si l'un manque. */
    static boolean identiques(Path a, Path b) throws Exception {
        if (a == null || b == null || !Files.exists(a) || !Files.exists(b)) return false;
        if (!Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b))) return false;
        Path ma = AnalyseMemoire.fichier(a), mb = AnalyseMemoire.fichier(b);
        return Files.exists(ma) && Files.exists(mb) && Arrays.equals(Files.readAllBytes(ma), Files.readAllBytes(mb));
    }

    /** Ce qui diffère entre deux jalons, pour le bilan : les lignes de la sauvegarde, puis les champs de la mémoire. */
    static String ecart(Path a, Path b) throws Exception {
        StringBuilder out = new StringBuilder();
        if (a == null || b == null || !Files.exists(a) || !Files.exists(b)) return "un des deux jalons manque";
        List<String> la = Files.readAllLines(a), lb = Files.readAllLines(b);
        int lignes = 0;
        for (int i = 0; i < Math.max(la.size(), lb.size()); i++) {
            String x = i < la.size() ? la.get(i) : "", y = i < lb.size() ? lb.get(i) : "";
            if (!x.equals(y) && lignes++ < 5) out.append("    sauvegarde : « ").append(abrege(x)).append(" » ≠ « ").append(abrege(y)).append(" »\n");
        }
        if (lignes > 5) out.append("    … ").append(lignes).append(" lignes de la sauvegarde diffèrent\n");
        Map<String, byte[]> ma = AnalyseMemoire.relire(a), mb = AnalyseMemoire.relire(b);
        int champs = 0;
        for (String cle : ma.keySet()) {
            if (!Arrays.equals(ma.get(cle), mb.get(cle)) && champs++ < 8) out.append("    mémoire : ").append(cle).append('\n');
        }
        for (String cle : mb.keySet()) if (!ma.containsKey(cle) && champs++ < 8) out.append("    mémoire : ").append(cle).append(" (seulement dans le nouveau)\n");
        if (champs > 8) out.append("    … ").append(champs).append(" champs de la mémoire diffèrent\n");
        return out.length() == 0 ? "aucun écart (?)" : out.toString();
    }

    private static String abrege(String s) {
        return s.length() <= 80 ? s : s.substring(0, 77) + "…";
    }

    /** Le jalon de départ de la tranche {@code k} dans un dossier : la partie neuve pour la première, sinon {@code apres_univers_(k-1)}. */
    static Path jalonDepart(Path dossier, int k) {
        if (k == 1) return null;
        Path p = dossier.resolve(nomJalon(k - 1));
        return Files.exists(p) && Files.exists(AnalyseMemoire.fichier(p)) ? p : null;
    }

    /** Un dossier à part pour chaque tranche et chaque tentative : deux tranches n'écrivent jamais le même fichier. */
    private static Path dossierTranche(Path sortie, int k, int tentative) {
        return sortie.resolve(String.format(Locale.ROOT, "tranche_%02d_%d", k, tentative));
    }

    /**
     * Rejoue les {@code univers} tranches en parallèle depuis les jalons de {@code reference}, vérifie la chaîne et
     * rejoue ce qui doit l'être, jusqu'à ce que chaque tranche soit partie du jalon exact que la précédente a produit.
     * Rend le nombre de tranches jouées en tout (le minimum est {@code univers}).
     */
    static int parallele(Path reference, Path sortie, int univers, double pas, long graine, int profil, double heuresMax, int fils) throws Exception {
        Files.createDirectories(sortie);
        long debut = System.nanoTime();
        StringBuilder bilan = new StringBuilder();
        bilan.append(String.format(Locale.ROOT, "Univers en parallèle : %d univers, pas %.0f s, graine %d, profil %d, %d fils ; référence %s%n%n",
                univers, pas, graine, profil, fils, reference));
        // Par tranche : le jalon dont elle est partie, son résultat, et le jalon final (validé) qu'elle a produit.
        Path[] depart = new Path[univers + 1];
        Resultat[] resultat = new Resultat[univers + 1];
        boolean[] valide = new boolean[univers + 1];
        int[] tentatives = new int[univers + 1];
        int jouees = 0;
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, fils));
        try {
            while (!valide[univers]) {
                // Le meilleur départ connu de chaque tranche : le jalon validé de la précédente, sinon le dernier qu'elle a
                // produit, sinon celui de la référence.
                List<Integer> aJouer = new ArrayList<>();
                for (int k = 1; k <= univers; k++) {
                    if (valide[k]) continue;
                    Path meilleur = k == 1 ? null
                            : resultat[k - 1] != null && resultat[k - 1].jalon() != null ? resultat[k - 1].jalon()
                            : jalonDepart(reference, k);
                    boolean dejaJouee = resultat[k] != null && (k == 1 || (depart[k] != null && meilleur != null && (depart[k].equals(meilleur) || identiques(depart[k], meilleur))));
                    if (k > 1 && meilleur == null) continue;   // rien d'où partir encore : la tranche attend la précédente
                    if (!dejaJouee) {
                        depart[k] = meilleur;
                        aJouer.add(k);
                    }
                }
                if (!aJouer.isEmpty()) {
                    List<Future<Resultat>> futures = new ArrayList<>();
                    for (int k : aJouer) {
                        int tentative = ++tentatives[k];
                        Path dossier = dossierTranche(sortie, k, tentative);
                        Path d = depart[k];
                        futures.add(pool.submit(() -> jouer(k, d, dossier, univers, pas, graine, profil, heuresMax)));
                        System.out.printf(Locale.ROOT, "TRANCHE_LANCEE,%d,tentative=%d,depart=%s%n", k, tentative, d == null ? "partie neuve" : d);
                    }
                    for (Future<Resultat> f : futures) {
                        Resultat r = f.get();
                        resultat[r.univers()] = r;
                        jouees++;
                        System.out.printf(Locale.ROOT, "TRANCHE_FINIE,%d,%s,jeu_h=%.3f,calcul_s=%.0f,evenements=%s%n", r.univers(), r.verdict(), r.heuresJeu(), r.secondes(), String.join(" | ", r.evenements()));
                        if (r.verdict().startsWith("ERREUR") || r.jalon() == null) {
                            bilan.append("Tranche ").append(r.univers()).append(" : ").append(r.verdict()).append(" — chaîne interrompue.\n").append(r.sortie());
                            Files.writeString(sortie.resolve("chaine.txt"), bilan.toString());
                            throw new IllegalStateException("Tranche " + r.univers() + " : " + r.verdict());
                        }
                    }
                }
                // La validation de la chaîne : la tranche k est acquise si la précédente l'est et si k est partie du
                // jalon exact que la précédente a produit.
                for (int k = 1; k <= univers; k++) {
                    if (valide[k] || resultat[k] == null) break;
                    if (k == 1) {
                        valide[1] = true;
                    } else if (valide[k - 1] && (depart[k].equals(resultat[k - 1].jalon()) || identiques(depart[k], resultat[k - 1].jalon()))) {
                        valide[k] = true;
                    } else {
                        break;
                    }
                }
                if (aJouer.isEmpty() && !valide[univers]) throw new IllegalStateException("La chaîne ne progresse plus (tranche " + premiereInvalide(valide) + ")");
            }
        } finally {
            pool.shutdownNow();
        }
        // Les jalons validés, copiés à plat dans la sortie ; les événements ; le bilan.
        StringBuilder evenements = new StringBuilder("univers,evenement,heure_reelle_h,heure_jeu_h\n");
        for (int k = 1; k <= univers; k++) {
            Resultat r = resultat[k];
            Files.copy(r.jalon(), sortie.resolve(r.jalon().getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.copy(AnalyseMemoire.fichier(r.jalon()), sortie.resolve(AnalyseMemoire.fichier(r.jalon()).getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            for (String e : r.evenements()) evenements.append(e).append('\n');
            Path ancien = k < univers ? reference.resolve(nomJalon(k)) : reference.resolve("fin.save");
            boolean pareil = Files.exists(ancien) && identiques(r.jalon(), ancien);
            bilan.append(String.format(Locale.ROOT, "Univers %2d : %s, fin à %.3f h de jeu, %.0f s de calcul, %d tentative(s), départ %s ; jalon %s%n",
                    k, r.verdict(), r.heuresJeu(), r.secondes(), tentatives[k], depart[k] == null ? "partie neuve" : depart[k].getFileName(),
                    !Files.exists(ancien) ? "sans référence" : pareil ? "identique à la référence" : "DIFFÉRENT de la référence"));
            if (Files.exists(ancien) && !pareil) bilan.append(ecart(r.jalon(), ancien));
        }
        double total = 0;
        for (int k = 1; k <= univers; k++) total += resultat[k].secondes();
        bilan.append(String.format(Locale.ROOT, "%n%d tranches jouées pour %d univers ; %.0f s de calcul cumulé, %.0f s de temps réel.%n",
                jouees, univers, total, (System.nanoTime() - debut) / 1e9));
        Files.writeString(sortie.resolve("evenements.csv"), evenements.toString());
        Files.writeString(sortie.resolve("chaine.txt"), bilan.toString());
        System.out.print(bilan);
        return jouees;
    }

    private static int premiereInvalide(boolean[] valide) {
        for (int k = 1; k < valide.length; k++) if (!valide[k]) return k;
        return -1;
    }

    /** Joue d'un trait et écrit les jalons : la partie de référence. */
    static void jalons(Path dossier, int univers, double pas, long graine, int profil, double heuresMax) throws Exception {
        Files.createDirectories(dossier);
        Resultat r = jouer(univers, null, dossier, univers, pas, graine, profil, heuresMax);
        StringBuilder evenements = new StringBuilder("univers,evenement,heure_reelle_h,heure_jeu_h\n");
        for (String e : r.evenements()) evenements.append(e).append('\n');
        Files.writeString(dossier.resolve("evenements.csv"), evenements.toString());
        Files.writeString(dossier.resolve("journal.txt"), r.sortie());
        System.out.printf(Locale.ROOT, "JALONS,%s,jeu_h=%.3f,calcul_s=%.0f,%s%n", r.verdict(), r.heuresJeu(), r.secondes(), String.join(" | ", r.evenements()));
        if (!r.verdict().equals("FIN")) throw new IllegalStateException("Partie de référence inachevée : " + r.verdict());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage : UniversParalleles jalons <dossier> <univers> [pas] [graine] [profil] [heuresMax]");
            System.err.println("        UniversParalleles parallele <reference> <sortie> <univers> [pas] [graine] [profil] [heuresMax] [fils]");
            System.exit(2);
        }
        String mode = args[0].toLowerCase(Locale.ROOT);
        int i = mode.equals("parallele") ? 3 : 2;
        int univers = Integer.parseInt(args[i++]);
        double pas = args.length > i ? Double.parseDouble(args[i]) : 20;
        i++;
        long graine = args.length > i ? Long.parseLong(args[i]) : 201;
        i++;
        int profil = args.length > i ? Integer.parseInt(args[i]) : 1;
        i++;
        double heuresMax = args.length > i ? Double.parseDouble(args[i]) : 2000;
        i++;
        int fils = args.length > i ? Integer.parseInt(args[i]) : Runtime.getRuntime().availableProcessors();
        if (univers < 1 || univers > FreshUniverseAudit.MAX_UNIVERSES || profil < 1 || profil > IdleProfiles24.PROFILES.length || pas <= 0 || pas > 60) {
            throw new IllegalArgumentException("Univers (1 à " + FreshUniverseAudit.MAX_UNIVERSES + "), profil (1 à " + IdleProfiles24.PROFILES.length + ") ou pas invalide");
        }
        switch (mode) {
            case "jalons" -> jalons(Path.of(args[1]), univers, pas, graine, profil, heuresMax);
            case "parallele" -> parallele(Path.of(args[1]), Path.of(args[2]), univers, pas, graine, profil, heuresMax, fils);
            default -> throw new IllegalArgumentException("Mode inconnu : " + args[0] + " (jalons, parallele)");
        }
    }

    /**
     * La sortie standard répartie par fil : ce qu'écrit une tranche (ses événements, son garde-fou) va dans son propre
     * tampon, pour que des tranches jouées en même temps ne mêlent pas leurs lignes. Les fils non branchés écrivent où
     * ils écrivaient.
     */
    static final class SortieParFil extends OutputStream {
        private static final ThreadLocal<OutputStream> CIBLE = new ThreadLocal<>();
        private static final PrintStream ORIGINE = System.out;
        private static volatile boolean installee;

        private static synchronized void installer() {
            if (installee) return;
            System.setOut(new PrintStream(new SortieParFil(), true, StandardCharsets.UTF_8));
            installee = true;
        }

        static void brancher(OutputStream cible) {
            installer();
            CIBLE.set(cible);
        }

        static void debrancher() {
            CIBLE.remove();
        }

        @Override
        public void write(int b) throws java.io.IOException {
            OutputStream cible = CIBLE.get();
            (cible == null ? ORIGINE : cible).write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            OutputStream cible = CIBLE.get();
            (cible == null ? ORIGINE : cible).write(b, off, len);
        }

        @Override
        public void flush() throws java.io.IOException {
            OutputStream cible = CIBLE.get();
            (cible == null ? ORIGINE : cible).flush();
        }
    }
}
