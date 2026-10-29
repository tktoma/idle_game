import idle.core.Game;
import idle.core.save.SaveCodec;
import idle.core.sky.Cosmos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Audit de bout en bout : une vraie partie neuve independante par profil.
 * Ne jamais confondre "pas de plantage" et "univers termine".
 *
 * Fin d'un essai : univers forme, garde-fou BLOCKED/INVALID, erreur, ou limite
 * de securite en heures simulees. Cette derniere n'est PAS une victoire. L'horizon est global a l'essai, quel que
 * soit le nombre d'univers vises : rien ne repart d'une sauvegarde de reference si un univers manque.
 *
 * <p>Version 0.126 : par defaut, la campagne n'echoue que sur un vrai probleme (BLOCKED ou ERROR) ; un
 * profil trop lent qui atteint le plafond (LIMIT) est signale, sans faire echouer le segment. Le sixieme
 * argument {@code strict} exige au contraire que chaque profil forme son univers.
 *
 * <p>Version 0.130 : le septieme argument {@code univers} (1 par defaut ; 3 au plus de la 0.133 a la 0.151, 11 depuis la 0.152) dit combien d'univers former. Au-dela du
 * premier, le bot lance l'univers suivant des que l'univers se forme, et rejoue les trois actes avec ce que le deuxieme
 * univers ajoute ({@link NextUniversePolicy}), sans dimensions ni graine. La victoire est alors le dernier univers vise.
 *
 * <p>Version 0.152 : jusqu'a {@link #MAX_UNIVERSES} univers (11). A partir du quatrieme, le bot attend le palier de la
 * graine qui ouvre l'univers suivant, et joue l'arbre : bulles, hadrons, ligne, automatisation des dimensions
 * ({@link UniverseTreePolicy}). Les univers 1 a 3 se jouent exactement comme avant.
 */
public final class FreshUniverseAudit {
    private FreshUniverseAudit() {}

    /**
     * Le plus d'univers qu'un essai peut viser (version 0.152 : 11, la graine a onze paliers ; 3 de la 0.133 à la 0.151).
     * À partir du quatrième, le bot arrose la graine, achète les bulles et lie les hadrons ({@link UniverseTreePolicy}).
     */
    static final int MAX_UNIVERSES = Game.SEED_STAGES;

    private static String clean(String text) {
        return String.valueOf(text).replace('\n', ' ').replace('\r', ' ');
    }

    private static String csv(String value) {
        return "\"" + clean(value).replace("\"", "\"\"") + "\"";
    }

    private static String diagnosis(Game game, IdleProfiles24 bot,
                                    String verdict, String reason, long seed) {
        StringBuilder report = new StringBuilder();
        report.append("profil=").append(bot.p.id()).append('_').append(bot.p.name()).append('\n');
        report.append("graine=").append(seed).append('\n');
        report.append("verdict=").append(verdict).append('\n');
        report.append("raison=").append(clean(reason)).append('\n');
        report.append(String.format(Locale.ROOT,
                "temps_mur_h=%.3f,temps_jeu_h=%.3f,en_ligne_h=%.3f,hors_ligne_credite_h=%.3f%n",
                bot.wallSeconds / 3600, game.state().timePlayed() / 3600,
                bot.onlineSeconds / 3600, bot.creditedOfflineSeconds / 3600));
        report.append("decisions=").append(bot.actions).append('\n');
        report.append("big_bangs=").append(game.bigBangs()).append('\n');
        report.append("explosions=").append(game.state().explosions()).append('\n');
        report.append("succes=").append(game.achievementCount())
                .append("/").append(Game.BIG_BANG_ACHIEVEMENTS)
                .append(" requis au premier Big Bang").append('\n');
        report.append("defis=").append(game.completedChallenges())
                .append('/').append(game.challenges().size()).append('\n');
        report.append("peut_big_bang=").append(game.canBigBang()).append('\n');
        report.append("astres_murs=").append(game.bodiesMature()).append('\n');
        report.append("etape_cosmos=").append(RobotCosmosPlanner.cosmosStage(game)).append('\n');
        report.append("univers_forme=").append(game.hasCosmos(Cosmos.UNIVERSE)).append('\n');
        report.append("univers_vises=").append(bot.targetUniverses).append('\n');
        report.append("univers_formes=").append(game.universes()).append(game.isReplaying() ? " (le suivant en cours)" : "")
                .append('\n');
        report.append("univers_h=").append(bot.universeHours()).append('\n');
        report.append("deuxieme_univers=regimes ").append(bot.nextUniverse.regimes).append(", cycles ")
                .append(bot.nextUniverse.cycles).append(" (").append(bot.nextUniverse.cycle() == null ? "au hasard" : bot.nextUniverse.cycle()).append("), familles ")
                .append(bot.nextUniverse.families).append(", elements offerts ").append(bot.nextUniverse.freeElements)
                .append(" ; facon de jouer ").append(bot.nextUniverse.style()).append('\n');
        report.append("anomalies_du_ciel_saisies=").append(bot.skyEventsCaught).append('\n');
        report.append("vide=ouvert ").append(bot.voidPolicy.opened).append(" fois, ").append(bot.voidPolicy.bought)
                .append(" ameliorations, ").append(bot.voidPolicy.sacrifices).append(" sacrifices ; facon de jouer ")
                .append(bot.voidPolicy.style()).append('\n');
        report.append("arbre=").append(bot.tree.summary(game)).append('\n');
        report.append("production_particules_par_s=").append(game.productionPerSecond()).append('\n');
        report.append("particules=").append(game.state().particles()).append('\n');
        report.append("atomes=").append(game.state().atoms()).append('\n');
        report.append("matiere_noire=").append(game.state().darkMatter()).append('\n');
        report.append("objectif=").append(clean(game.shoppingList().target())).append('\n');
        report.append("besoins=").append(clean(game.shoppingList().items().toString())).append('\n');
        report.append("matiere_manquante=").append(clean(game.shoppingList().matter().toString())).append('\n');
        if (bot.progressGuard.latest() != null) {
            report.append("garde_verdict=").append(bot.progressGuard.latest().verdict()).append('\n');
            report.append("garde_raison=").append(bot.progressGuard.latest().reason()).append('\n');
        }
        if (game.bigBangs() == 0 && !game.hasCosmos(Cosmos.UNIVERSE)) {
            report.append("diagnostic_premier_big_bang=non atteint dans l'univers en cours; comparer succes, defis, ")
                    .append("production, explosions et prerequis avant de conclure a un blocage\n");
        }
        return report.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5 || args.length > 7) {
            throw new IllegalArgumentException(
                    "Usage: firstId lastId maxWallHours stepSeconds outputDirectory [strict] [univers]");
        }
        boolean strict = args.length >= 6 && Boolean.parseBoolean(args[5]);
        int universes = args.length == 7 ? Integer.parseInt(args[6]) : 1;
        int first = Integer.parseInt(args[0]);
        int last = Integer.parseInt(args[1]);
        double maxHours = Double.parseDouble(args[2]);
        double dt = Double.parseDouble(args[3]);
        Path root = Path.of(args[4]);
        if (first < 1 || last > 24 || first > last
                || !Double.isFinite(maxHours) || maxHours <= 0
                || !Double.isFinite(dt) || dt <= 0 || dt > 60 || universes < 1 || universes > MAX_UNIVERSES) {
            throw new IllegalArgumentException("Bornes, horizon, pas de simulation ou univers (1 à " + MAX_UNIVERSES + ") invalides");
        }
        Files.createDirectories(root);
        StringBuilder csv = new StringBuilder(
                "id,profil,verdict,raison,heures,heures_jeu,heures_en_ligne,big_bangs,"
                + "explosions,succes,defis,astres_murs,cosmos,decisions,univers,univers_vises,univers_formes,univers_h,anomalies\n");
        int wins = 0, blocked = 0, limits = 0, errors = 0;
        for (int id = first; id <= last; id++) {
            IdleProfiles24.Profile profile = IdleProfiles24.PROFILES[id - 1];
            long seed = 200L + id;
            Game game = Bots.fresh(seed);
            IdleProfiles24 bot = new IdleProfiles24(game, profile, seed, dt);
            bot.targetUniverses = universes;
            Path dir = root.resolve(String.format(Locale.ROOT, "%02d_%s", id, profile.name()));
            Files.createDirectories(dir);
            String verdict = "LIMIT";
            String reason = "Plafond de securite de " + maxHours
                    + " heures simulees, sans univers forme";
            try {
                // execute s'arrete immediatement a la formation de l'univers.
                // Son garde-fou detecte les blocages reels et les etats invalides.
                bot.execute(maxHours, dir);
                if (NextUniversePolicy.reached(game, universes)) {
                    verdict = "WIN";
                    reason = universes == 1 ? "Premier univers termine" : "Univers " + universes + " termine";
                } else if (universes > 1) {
                    reason = "Plafond de securite de " + maxHours + " heures simulees, "
                            + game.universes() + " univers forme(s) sur " + universes;
                }
            } catch (Exception failure) {
                ProfileProgressGuard.Assessment assessment = bot.progressGuard.latest();
                if (assessment != null
                        && assessment.verdict() == ProfileProgressGuard.Verdict.BLOCKED) {
                    verdict = "BLOCKED";
                    reason = assessment.reason();
                } else {
                    verdict = "ERROR";
                    reason = failure.toString();
                    failure.printStackTrace(System.err);
                }
            }
            try {
                Files.writeString(dir.resolve("end.save"), SaveCodec.write(game.state(), 0));
                Files.writeString(dir.resolve("diagnostic.txt"),
                        diagnosis(game, bot, verdict, reason, seed));
            } catch (Exception failure) {
                verdict = "ERROR";
                reason = "Impossible de sauvegarder le diagnostic : " + failure;
                failure.printStackTrace(System.err);
            }
            switch (verdict) {
                case "WIN" -> wins++;
                case "BLOCKED" -> blocked++;
                case "LIMIT" -> limits++;
                default -> errors++;
            }
            csv.append(String.format(Locale.ROOT,
                    "%d,%s,%s,%s,%.4f,%.4f,%.4f,%d,%d,%d,%d,%d,%d,%d,%s,%d,%d,%s,%d%n",
                    id, csv(profile.name()), csv(verdict), csv(reason),
                    bot.wallSeconds / 3600, game.state().timePlayed() / 3600,
                    bot.onlineSeconds / 3600, game.bigBangs(), game.state().explosions(),
                    game.achievementCount(), game.completedChallenges(),
                    game.bodiesMature(), RobotCosmosPlanner.cosmosStage(game),
                    bot.actions, game.hasCosmos(Cosmos.UNIVERSE), universes, game.universes(),
                    csv(bot.universeHours()), bot.skyEventsCaught));
            Files.writeString(root.resolve(String.format(Locale.ROOT,
                    "resume_%02d-%02d.csv", first, last)), csv.toString());
            System.out.printf(Locale.ROOT,
                    "FRESH_AUDIT,%02d_%s,verdict=%s,wall_h=%.3f,bb=%d,"
                    + "succes=%d,defis=%d/%d,astres=%d,cosmos=%d,univers=%d/%d,univers_h=%s,raison=%s%n",
                    id, profile.name(), verdict, bot.wallSeconds / 3600,
                    game.bigBangs(), game.achievementCount(), game.completedChallenges(),
                    game.challenges().size(), game.bodiesMature(),
                    RobotCosmosPlanner.cosmosStage(game), game.universes(), universes, bot.universeHours(), clean(reason));
        }
        System.out.printf(Locale.ROOT,
                "FRESH_AUDIT_BILAN,segment=%d-%d,victoires=%d,bloques=%d,"
                + "limites=%d,erreurs=%d%n",
                first, last, wins, blocked, limits, errors);
        // L'avertissement d'abord : il reste dans le journal meme quand un blocage fait echouer le segment.
        if (limits > 0) {
            System.out.printf(Locale.ROOT, "FRESH_AUDIT_AVERTISSEMENT,%d profil(s) au plafond de %.0f h sans univers :"
                    + " trop lents, pas bloques. Relancer avec strict=true pour en faire un echec.%n", limits, maxHours);
        }
        if (blocked + errors > 0 || (strict && wins != last - first + 1)) {
            throw new IllegalStateException(
                    "Campagne non validee : " + wins + "/" + (last - first + 1)
                    + " univers termines, " + blocked + " bloques, " + errors + " erreurs"
                    + (strict ? " (mode strict)" : "") + ". Lire les diagnostics et les CSV sauvegardes.");
        }
    }
}
