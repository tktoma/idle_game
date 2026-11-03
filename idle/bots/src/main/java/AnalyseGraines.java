import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le bot analyste sur plusieurs graines (version 0.164) : la même façon de jouer, d'autres tirages du hasard. Pour
 * chaque palier, le plus tôt, la médiane et le plus tard ; l'écart dit ce qui tient au hasard et ce qui tient au jeu.
 * Chaque partie s'écrit dans son dossier (graine-201…), sans les « et si » ; le résumé dans graines.txt, graines.csv et
 * graines.html.
 *
 * <p>Usage : {@code AnalyseGraines <sortie> <univers ou fin> <graine> [graine…]}. Les parties se jouent l'une après
 * l'autre ; {@link #resumer} relit des dossiers déjà joués. Version 0.168 : une graine peut porter l'ordre des bulles
 * imposé ({@code 201:moins-cher}), pour mesurer ce qui tient à l'ordre des bulles et non au hasard.
 */
final class AnalyseGraines {

    private AnalyseGraines() {}

    /** Un palier sur toutes les graines : combien l'ont atteint, et quand (plus tôt, médiane, plus tard). */
    record Resume(String palier, int atteint, double min, double mediane, double max) {
        /** L'écart entre le plus tard et le plus tôt, rapporté à la médiane. */
        double dispersion() {
            return mediane > 0 ? (max - min) / mediane : 0;
        }
    }

    static List<Resume> resumer(List<Path> dossiers) throws Exception {
        Map<String, List<Double>> parPalier = new LinkedHashMap<>();
        for (Path d : dossiers) {
            AnalyseComparaison.paliers(d).forEach((nom, t) -> parPalier.computeIfAbsent(nom, k -> new ArrayList<>()).add(t));
        }
        List<Resume> out = new ArrayList<>();
        parPalier.forEach((nom, ts) -> {
            List<Double> sorted = new ArrayList<>(ts);
            sorted.sort(null);
            int n = sorted.size();
            double med = n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
            out.add(new Resume(nom, n, sorted.get(0), med, sorted.get(n - 1)));
        });
        return out;
    }

    static void ecrire(List<Resume> resumes, int graines, Path sortie) throws Exception {
        StringBuilder txt = new StringBuilder("Le bot analyste sur " + graines + " graines\n\n");
        StringBuilder csv = new StringBuilder("palier,atteint,plus_tot_s,mediane_s,plus_tard_s,dispersion\n");
        StringBuilder html = new StringBuilder("<!doctype html><html lang=fr><head><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\">"
                + "<title>Plusieurs graines</title><style>:root{--bg:#f6f7f9;--fg:#1d232b;--mute:#5d6875;--line:#d9dee5;--bad:#c23b3b}"
                + "@media (prefers-color-scheme:dark){:root{--bg:#12161c;--fg:#e4e9ef;--mute:#93a0ad;--line:#2c3540;--bad:#ff7a7a}}"
                + "body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}main{max-width:1000px;margin:0 auto;padding:24px 16px}"
                + "table{border-collapse:collapse;width:100%;font-size:14px}th,td{text-align:left;padding:6px 8px;border-bottom:1px solid var(--line)}"
                + "td.t{font-family:ui-monospace,Consolas,monospace;white-space:nowrap}.bad{color:var(--bad)}.sub{color:var(--mute)}</style></head><body><main>"
                + "<h1>Le bot analyste sur " + graines + " graines</h1><p class=sub>La même façon de jouer, d'autres tirages du hasard. "
                + "Dispersion : (plus tard − plus tôt) / médiane ; au-delà de 10 %, en rouge.</p><table><thead><tr><th>Palier</th><th>Atteint</th>"
                + "<th>Plus tôt</th><th>Médiane</th><th>Plus tard</th><th>Dispersion</th></tr></thead><tbody>");
        for (Resume r : resumes) {
            txt.append(String.format(Locale.ROOT, "%-55s %d/%d %9s %9s %9s %5.1f %%%n", r.palier(), r.atteint(), graines, AnalyseJournal.chrono(r.min()),
                    AnalyseJournal.chrono(r.mediane()), AnalyseJournal.chrono(r.max()), 100 * r.dispersion()));
            csv.append('"').append(r.palier().replace("\"", "\"\"")).append("\",").append(r.atteint()).append(String.format(Locale.ROOT, ",%.0f,%.0f,%.0f,%.4f%n",
                    r.min(), r.mediane(), r.max(), r.dispersion()));
            html.append("<tr><td>").append(r.palier().replace("&", "&amp;").replace("<", "&lt;")).append("</td><td>").append(r.atteint()).append('/').append(graines)
                    .append("</td><td class=t>").append(AnalyseJournal.chrono(r.min())).append("</td><td class=t>").append(AnalyseJournal.chrono(r.mediane()))
                    .append("</td><td class=t>").append(AnalyseJournal.chrono(r.max())).append("</td><td class=").append(r.dispersion() > 0.1 ? "bad" : "sub")
                    .append(String.format(Locale.FRANCE, ">%.1f %%", 100 * r.dispersion())).append("</td></tr>");
        }
        html.append("</tbody></table></main></body></html>");
        Files.createDirectories(sortie);
        Files.writeString(sortie.resolve("graines.txt"), txt.toString());
        Files.writeString(sortie.resolve("graines.csv"), csv.toString());
        Files.writeString(sortie.resolve("graines.html"), html.toString());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage : AnalyseGraines <sortie> <univers ou fin> <graine> [graine…]");
            System.exit(2);
        }
        Path sortie = Path.of(args[0]);
        List<Path> dossiers = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            // Version 0.168 : « 202 » ou « 201:moins-cher » (une graine et l'ordre des bulles imposé) ; une partie déjà
            // jouée jusqu'au bout (son fin.save écrit) ne se rejoue pas.
            String[] part = args[i].split(":", 2);
            String ordre = part.length > 1 ? part[1] : "";
            Path d = sortie.resolve("graine-" + part[0] + (ordre.isEmpty() ? "" : "-" + ordre));
            // Sans « et si », une estimation par heure : la même partie, plusieurs fois plus vite.
            if (!Files.exists(d.resolve("fin.save"))) {
                BotAnalyste.main(new String[] {d.toString(), args[1], "400", part[0], "5", "60", "false", "false", "", "", "", ordre});
            }
            dossiers.add(d);
        }
        ecrire(resumer(dossiers), dossiers.size(), sortie);
        System.out.println("GRAINES," + dossiers.size() + " parties," + sortie.toAbsolutePath());
    }
}
