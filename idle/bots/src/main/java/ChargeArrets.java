import idle.core.Game;
import idle.core.save.SaveCodec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Version 0.205 : ce que pèse dans le calcul d'une simulation tout ce qu'{@link AnalyseArrets} a mesuré inutile.
 *
 * <p>Deux parties du premier univers (Speedrunner, graine 201, pas de 20 s), jouées l'une après l'autre : le témoin, puis
 * la même où chaque chose est coupée à partir du jalon où elle ne sert plus. Pour chaque tronçon entre deux jalons :
 * l'heure de jeu, le temps de calcul et le nombre de décisions. Les deux parties sont reconstruites à chaque jalon de la
 * même façon (même hasard), pour que seule la coupure les sépare.
 *
 * <p>Usage : {@code ChargeArrets [plan]} ; {@code plan} : {@code tout} (défaut), {@code automatismes},
 * {@code ameliorations} ou {@code mecaniques}.
 */
final class ChargeArrets {
    private ChargeArrets() {}

    /** Ce qui se coupe à chaque jalon, d'après les tableaux de la 0.204 (univers 1). */
    static Map<String, String> plan(String quoi) {
        boolean tout = quoi.equals("tout");
        Map<String, String> plan = new LinkedHashMap<>();
        for (String jalon : List.of("Big Bang 1", "Big Bang 5", "Big Bang 8", "galaxie", "amas")) plan.put(jalon, "");
        if (tout || quoi.equals("automatismes")) {
            ajoute(plan, "Big Bang 1", "auto_coupling,dark_auto_start,auto_collisionneur,auto_rassemblement,auto_formation,auto_appui");
            ajoute(plan, "Big Bang 5", "auto_speed,auto_generator,dark_auto_atoms,dark_auto_machines,dark_auto_tree");
            ajoute(plan, "galaxie", "auto_fusion,dark_auto_explosion");
            ajoute(plan, "amas", "auto_molecules");
        }
        if (tout || quoi.equals("ameliorations")) {
            ajoute(plan, "Big Bang 1", "coupling,atom_double,atom_mass,atom_discount,atom_bulk");
            ajoute(plan, "Big Bang 5", "speed,generator,atom_yield,atom_boost,atom_keep,atom_patience,atom_overload");
        }
        if (tout || quoi.equals("mecaniques")) {
            ajoute(plan, "Big Bang 5", "defis");
            ajoute(plan, "Big Bang 8", "collisionneur");
        }
        return plan;
    }

    private static void ajoute(Map<String, String> plan, String jalon, String ids) {
        plan.merge(jalon, ids, (a, b) -> a.isEmpty() ? b : a + "," + b);
    }

    /** Un tronçon : son nom (le jalon qui l'ouvre), l'heure de jeu à son début, son temps de calcul, ses décisions. */
    record Troncon(String nom, double debut, double calcul, long decisions) {}

    /** Une partie jouée : ses tronçons et son heure d'arrivée. */
    record Partie(List<Troncon> troncons, double fin) {}

    static Partie joue(Map<String, String> plan) throws Exception {
        Game game = Bots.fresh(AnalyseArrets.GRAINE);
        IdleProfiles24 bot = bot(game);
        bot.begin();
        List<AnalyseArrets.Coupure> coupees = new ArrayList<>();
        Set<String> sans = new LinkedHashSet<>();
        List<Troncon> troncons = new ArrayList<>();
        String nom = "début";
        double debut = 0;
        long depart = System.nanoTime(), decisions = 0;
        int explosions = 0, bangs = 0, cosmos = 0;
        for (long k = 0; k < 2000L * 3600 / 20; k++) {
            boolean fini = bot.stepOnce();
            decisions++;
            String jalon = null;
            if (explosions == 0 && game.state().explosions() > 0 && game.bigBangs() == 0) jalon = "explosion 1";
            else if (game.bigBangs() > bangs && (game.bigBangs() == 1 || game.bigBangs() == 5 || game.bigBangs() == Bots.LAST_BIG_BANG)) {
                jalon = "Big Bang " + game.bigBangs();
            } else if (cosmos < 1 && RobotCosmosPlanner.cosmosStage(game) == 1) jalon = "galaxie";
            else if (cosmos < 2 && RobotCosmosPlanner.cosmosStage(game) == 2) jalon = "amas";
            explosions = game.state().explosions();
            bangs = game.bigBangs();
            cosmos = RobotCosmosPlanner.cosmosStage(game);
            if (!fini && jalon == null) continue;
            troncons.add(new Troncon(nom, debut, (System.nanoTime() - depart) / 1e9, decisions));
            if (fini) return new Partie(troncons, game.state().timePlayed());
            // Le jalon : la partie reconstruite (témoin compris, pour le même hasard), les coupures du plan en plus.
            String ids = plan == null ? "" : plan.getOrDefault(jalon, "");
            if (!ids.isEmpty()) {
                for (AnalyseArrets.Coupure c : AnalyseArrets.choisies(ids)) {
                    coupees.add(c);
                    sans.addAll(c.sans());
                }
            }
            String etat = SaveCodec.write(game.state(), 0);
            Game suite = Bots.game(SaveCodec.read(etat).state(), AnalyseArrets.GRAINE + troncons.size(), Set.copyOf(sans));
            IdleProfiles24 suivant = bot(suite);
            AnalyseCopie.memoire(bot, suivant);
            suivant.quiet = true;
            for (AnalyseArrets.Coupure c : coupees) c.regler().accept(suivant);
            game = suite;
            bot = suivant;
            nom = jalon;
            debut = game.state().timePlayed();
            depart = System.nanoTime();
            decisions = 0;
        }
        return new Partie(troncons, Double.NaN);
    }

    private static IdleProfiles24 bot(Game game) {
        IdleProfiles24 bot = new IdleProfiles24(game, IdleProfiles24.PROFILES[0], AnalyseArrets.GRAINE, AnalyseArrets.PAS);
        bot.targetUniverses = 1;
        bot.quiet = true;
        return bot;
    }

    public static void main(String[] args) throws Exception {
        String quoi = args.length > 0 ? args[0] : "tout";
        Map<String, String> plan = plan(quoi);
        // Le planificateur du bot écrit ses étapes sur la sortie : elles sont tues pendant les trois parties.
        java.io.PrintStream sortie = System.out;
        System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        Partie temoin, coupee;
        try {
            joue(null); // une partie pour rien : la JVM chaude avant de chronométrer
            temoin = joue(null);
            coupee = joue(plan);
        } finally {
            System.setOut(sortie);
        }
        System.out.printf(Locale.ROOT, "Plan « %s » :%n", quoi);
        plan.forEach((jalon, ids) -> { if (!ids.isEmpty()) System.out.printf(Locale.ROOT, "  à partir de %-11s : %s%n", jalon, ids); });
        System.out.printf(Locale.ROOT, "%n%-13s %10s %10s | %9s %9s %7s%n", "tronçon", "témoin", "coupé", "calcul", "calcul", "gain");
        double a = 0, b = 0;
        for (int i = 0; i < temoin.troncons().size() && i < coupee.troncons().size(); i++) {
            Troncon t = temoin.troncons().get(i), c = coupee.troncons().get(i);
            a += t.calcul();
            b += c.calcul();
            System.out.printf(Locale.ROOT, "%-13s %10s %10s | %7.1f s %7.1f s %6.0f %%%n", t.nom(), AnalyseJournal.chrono(t.debut()),
                    AnalyseJournal.chrono(c.debut()), t.calcul(), c.calcul(), 100 * (1 - c.calcul() / t.calcul()));
        }
        System.out.printf(Locale.ROOT, "%-13s %10s %10s | %7.1f s %7.1f s %6.0f %%%n", "arrivée", AnalyseJournal.chrono(temoin.fin()),
                AnalyseJournal.chrono(coupee.fin()), a, b, 100 * (1 - b / a));
    }
}
