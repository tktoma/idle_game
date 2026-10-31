import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Compare deux parties du bot analyste (version 0.164) : deux versions du jeu, ou une partie avec et sans une
 * amélioration ({@code sans=}). Pour chaque palier atteint dans les deux, le moment dans chacune et l'écart ; puis ce qui
 * n'est atteint que d'un côté. Écrit comparaison.txt et comparaison.html.
 *
 * <p>Usage : {@code AnalyseComparaison <dossier A> <dossier B> <sortie>}.
 */
final class AnalyseComparaison {

    private AnalyseComparaison() {}

    /** Un palier dans les deux parties : son nom, son moment dans A et dans B (NaN s'il n'y est pas). */
    record Ligne(String palier, double a, double b) {
        double ecart() {
            return b - a;
        }
    }

    /** Les paliers d'une partie : nom → moment de la première fois (secondes de jeu). */
    static Map<String, Double> paliers(Path dossier) throws Exception {
        Map<String, Double> out = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(dossier.resolve("paliers.csv"));
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = cellules(line);
            if (cells.size() < 4) continue;
            out.putIfAbsent(cells.get(3), Double.parseDouble(cells.get(1)));
        }
        return out;
    }

    /** Une ligne CSV découpée, guillemets compris. */
    static List<String> cellules(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** Les paliers des deux parties, dans l'ordre de A puis ceux de B seuls. */
    static List<Ligne> comparer(Map<String, Double> a, Map<String, Double> b) {
        List<Ligne> out = new ArrayList<>();
        a.forEach((nom, t) -> out.add(new Ligne(nom, t, b.getOrDefault(nom, Double.NaN))));
        b.forEach((nom, t) -> {
            if (!a.containsKey(nom)) out.add(new Ligne(nom, Double.NaN, t));
        });
        return out;
    }

    private static String ecart(double seconds) {
        if (Double.isNaN(seconds)) return "—";
        return (seconds >= 0 ? "+" : "−") + AnalyseJournal.duree(Math.abs(seconds));
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void ecrire(Path a, Path b, Path sortie) throws Exception {
        List<Ligne> lignes = comparer(paliers(a), paliers(b));
        Files.createDirectories(sortie);
        StringBuilder txt = new StringBuilder("Comparaison de deux parties du bot analyste\nA : " + a + "\nB : " + b + "\n\n");
        StringBuilder html = new StringBuilder("<!doctype html><html lang=fr><head><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\">"
                + "<title>Comparaison de parties</title><style>:root{--bg:#f6f7f9;--fg:#1d232b;--mute:#5d6875;--line:#d9dee5;--bad:#c23b3b;--good:#2f8f5b}"
                + "@media (prefers-color-scheme:dark){:root{--bg:#12161c;--fg:#e4e9ef;--mute:#93a0ad;--line:#2c3540;--bad:#ff7a7a;--good:#5fd39a}}"
                + "body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}main{max-width:1000px;margin:0 auto;padding:24px 16px}"
                + "table{border-collapse:collapse;width:100%;font-size:14px}th,td{text-align:left;padding:6px 8px;border-bottom:1px solid var(--line)}"
                + "td.t{font-family:ui-monospace,Consolas,monospace;white-space:nowrap}.bad{color:var(--bad)}.good{color:var(--good)}.sub{color:var(--mute)}"
                + "</style></head><body><main><h1>Comparaison de deux parties</h1><p class=sub>A : " + esc(a.toString()) + "<br>B : " + esc(b.toString())
                + "<br>Un écart négatif : B arrive plus tôt.</p><table><thead><tr><th>Palier</th><th>A</th><th>B</th><th>Écart (B − A)</th></tr></thead><tbody>");
        int plusTot = 0, plusTard = 0;
        for (Ligne l : lignes) {
            double e = l.ecart();
            if (!Double.isNaN(e) && e < -0.5) plusTot++;
            if (!Double.isNaN(e) && e > 0.5) plusTard++;
            txt.append(String.format(Locale.ROOT, "%-55s %9s %9s %s%n", l.palier(), AnalyseJournal.chrono(l.a()), AnalyseJournal.chrono(l.b()), ecart(e)));
            html.append("<tr><td>").append(esc(l.palier())).append("</td><td class=t>").append(AnalyseJournal.chrono(l.a())).append("</td><td class=t>")
                    .append(AnalyseJournal.chrono(l.b())).append("</td><td class=").append(Double.isNaN(e) ? "sub" : e < -0.5 ? "good" : e > 0.5 ? "bad" : "sub")
                    .append('>').append(ecart(e)).append("</td></tr>");
        }
        String bilan = plusTot + " paliers plus tôt dans B, " + plusTard + " plus tard, sur " + lignes.size() + ".";
        txt.append('\n').append(bilan).append('\n');
        html.append("</tbody></table><p>").append(esc(bilan)).append("</p></main></body></html>");
        Files.writeString(sortie.resolve("comparaison.txt"), txt.toString());
        Files.writeString(sortie.resolve("comparaison.html"), html.toString());
        System.out.println("COMPARAISON," + bilan + "," + sortie.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage : AnalyseComparaison <dossier A> <dossier B> <sortie>");
            System.exit(2);
        }
        ecrire(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
    }
}
