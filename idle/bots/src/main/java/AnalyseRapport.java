import idle.core.Multipliers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * La page HTML du bot analyste (version 0.161) : un seul fichier, sans rien à télécharger, lisible dans n'importe quel
 * navigateur. En haut, le résultat ; puis les paliers (avec ce que l'estimation annonçait), les courbes des grandeurs,
 * les achats (filtrables, triables par poids) et tout le journal, filtrable par sorte.
 */
final class AnalyseRapport {

    private AnalyseRapport() {}

    private static String esc(String s) {
        return String.valueOf(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String[] COULEURS = {"#4f8ff7", "#e0773a", "#2fa66a", "#b05cd6", "#d64a6a", "#2bb3c0", "#c9a227", "#7a8a99"};

    static void ecrire(BotAnalyste bot, Path file, String fin) throws Exception {
        AnalyseJournal j = bot.journal;
        StringBuilder h = new StringBuilder();
        h.append("""
                <!doctype html><html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Rapport du bot analyste</title><style>
                :root{--bg:#f6f7f9;--fg:#1d232b;--mute:#5d6875;--card:#fff;--line:#d9dee5;--accent:#2f6fdb;--bad:#c23b3b;--good:#2f8f5b}
                @media (prefers-color-scheme:dark){:root{--bg:#12161c;--fg:#e4e9ef;--mute:#93a0ad;--card:#1a2028;--line:#2c3540;--accent:#6ea2ff;--bad:#ff7a7a;--good:#5fd39a}}
                body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,-apple-system,"Segoe UI",sans-serif}
                main{max-width:1180px;margin:0 auto;padding:24px 16px 64px}
                h1{font-size:26px;margin:0 0 4px}h2{font-size:19px;margin:36px 0 10px}
                .sub{color:var(--mute)}.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px 16px}
                .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px;margin-top:16px}
                .kpi b{display:block;font-size:22px}.kpi span{color:var(--mute);font-size:13px}
                table{border-collapse:collapse;width:100%;font-size:13.5px}th,td{text-align:left;padding:6px 8px;border-bottom:1px solid var(--line);vertical-align:top}
                th{position:sticky;top:0;background:var(--card);cursor:pointer;user-select:none}td.t{font-family:ui-monospace,Consolas,monospace;white-space:nowrap}
                .scroll{max-height:620px;overflow:auto;border:1px solid var(--line);border-radius:10px;background:var(--card)}
                input[type=search]{width:100%;max-width:420px;padding:7px 10px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg)}
                .filters label{margin-right:12px;font-size:13.5px;white-space:nowrap}.bad{color:var(--bad)}.good{color:var(--good)}
                .det{color:var(--mute);font-size:12.5px}svg text{fill:var(--mute);font-size:11px}.legend span{margin-right:14px;font-size:13px;white-space:nowrap}
                .legend i{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:5px}
                </style></head><body><main>
                """);
        h.append("<h1>Rapport du bot analyste</h1><div class=sub>").append(esc(fin)).append("</div>");
        h.append("<div class=grid>");
        kpi(h, AnalyseJournal.chrono(bot.temps()), "temps de jeu");
        kpi(h, String.valueOf(bot.univers()), "univers en cours");
        kpi(h, String.valueOf(j.paliers.size()), "paliers atteints");
        kpi(h, String.valueOf(j.achats.size()), "achats notés");
        kpi(h, String.valueOf(j.lignes.stream().filter(l -> l.sorte() == AnalyseJournal.Sorte.SUCCES).count()), "succès et titres");
        h.append("</div>");
        if (bot.blocage != null) {
            h.append("<h2 class=bad>Arrêt sur blocage</h2><div class=card>");
            j.lignes.stream().filter(l -> l.sorte() == AnalyseJournal.Sorte.BLOCAGE).forEach(l -> {
                for (String d : l.details()) h.append("<div>").append(esc(d)).append("</div>");
            });
            h.append("</div>");
        }

        // La frise et les résumés.
        h.append("<h2>Frise de la partie</h2><div class=card>");
        frise(h, j.paliers, bot.temps());
        h.append("</div>");
        resumes(h, j);

        // Les paliers.
        h.append("<h2>Paliers</h2><div class=scroll><table><thead><tr><th>Chrono</th><th>Univers</th><th>Palier</th><th>Depuis le précédent</th><th>Annoncé pour</th></tr></thead><tbody>");
        for (AnalyseJournal.Palier p : j.paliers) {
            String annonce = Double.isNaN(p.annonce()) ? "—" : AnalyseJournal.chrono(p.annonce()) + " (écart " + (p.t() >= p.annonce() ? "+" : "−")
                    + AnalyseJournal.duree(Math.abs(p.t() - p.annonce())) + ")";
            h.append("<tr><td class=t>").append(AnalyseJournal.chrono(p.t())).append("</td><td>").append(p.univers()).append("</td><td>")
                    .append(esc(p.nom())).append("</td><td>").append(AnalyseJournal.duree(p.depuis())).append("</td><td>").append(annonce).append("</td></tr>");
        }
        h.append("</tbody></table></div>");

        // Les courbes.
        h.append("<h2>Grandeurs au fil du temps</h2><div class=card>");
        courbes(h, bot.releves);
        h.append("</div>");
        if (!j.estimations.isEmpty()) {
            h.append("<h2>Estimations du prochain palier</h2><div class=scroll><table><thead><tr><th>Chrono</th><th>Palier visé</th><th>Estimé</th><th>Goulot</th></tr></thead><tbody>");
            for (AnalyseJournal.Estimation e : j.estimations) {
                h.append("<tr><td class=t>").append(AnalyseJournal.chrono(e.t())).append("</td><td>").append(esc(e.vise())).append("</td><td>")
                        .append(e.secondes() < 0 ? "<span class=bad>au-delà de l'horizon</span>" : AnalyseJournal.duree(e.secondes()))
                        .append("</td><td class=det>").append(esc(e.goulot())).append("</td></tr>");
            }
            h.append("</tbody></table></div>");
        }

        // Les achats : regroupés par chose achetée (la liste complète est dans achats.csv), puis les plus lourds.
        record Somme(String groupe, String chose, String paye, long fois, double quantite, double montantLog, double premier, double dernier,
                double poids) {}
        java.util.Map<String, Somme> sommes = new java.util.LinkedHashMap<>();
        for (AnalyseJournal.Achat a : j.achats) {
            String key = a.groupe() + "|" + a.chose() + "|" + a.paye();
            Somme old = sommes.get(key);
            double log = Double.isFinite(a.montantLog()) ? a.montantLog() : Double.NEGATIVE_INFINITY;
            if (old == null) {
                sommes.put(key, new Somme(a.groupe(), a.chose(), a.paye(), 1, a.quantite(), log, a.t(), a.t(), a.poidsMax()));
            } else {
                double m = Math.max(old.montantLog(), log);
                double sum = Double.isInfinite(m) ? m : m + Math.log10(Math.pow(10, old.montantLog() - m) + Math.pow(10, log - m));
                sommes.put(key, new Somme(old.groupe(), old.chose(), old.paye(), old.fois() + 1, old.quantite() + a.quantite(), sum, old.premier(), a.t(),
                        old.poids() + a.poidsMax()));
            }
        }
        h.append("<h2>Achats, par chose achetée</h2><p class=sub>").append(j.achats.size()).append(" achats en tout (la liste complète, avec "
                + "chaque prix et chaque poids, est dans achats.csv). Un clic sur un titre de colonne trie ; le poids cumulé additionne les "
                + "ordres de grandeur gagnés à chaque achat.</p><input type=search id=qa placeholder=\"Filtrer…\">");
        h.append("<div class=scroll style=margin-top:8px><table id=ta><thead><tr><th>Groupe</th><th>Chose</th><th data-n>Fois</th><th data-n>Niveaux</th>"
                + "<th>Payé en</th><th>Total investi</th><th>Premier</th><th>Dernier</th><th data-n>Poids cumulé (log)</th></tr></thead><tbody>");
        for (Somme x : sommes.values()) {
            h.append("<tr><td>").append(esc(x.groupe())).append("</td><td>").append(esc(x.chose())).append("</td><td>").append(x.fois()).append("</td><td>")
                    .append(Math.round(x.quantite())).append("</td><td>").append(esc(x.paye())).append("</td><td>")
                    .append(Double.isFinite(x.montantLog()) ? new AnalyseEtat.Nombre(1, x.montantLog()).texte() : "—").append("</td><td class=t>")
                    .append(AnalyseJournal.chrono(x.premier())).append("</td><td class=t>").append(AnalyseJournal.chrono(x.dernier())).append("</td><td>")
                    .append(String.format(Locale.ROOT, "%.3f", x.poids())).append("</td></tr>");
        }
        h.append("</tbody></table></div>");
        h.append("<h2>Les 200 achats qui ont fait gagner le plus de temps</h2><div class=scroll><table><thead><tr><th>Chrono</th><th>U</th><th>Quoi</th><th>Payé</th><th>Part</th>"
                + "<th>Poids</th><th data-n>Gain estimé (s)</th></tr></thead><tbody>");
        j.achats.stream().sorted((x, y) -> {
            double gx = Double.isFinite(x.gainSecondes()) ? x.gainSecondes() : 0, gy = Double.isFinite(y.gainSecondes()) ? y.gainSecondes() : 0;
            return gx != gy ? Double.compare(gy, gx) : Double.compare(y.poidsMax(), x.poidsMax());
        }).limit(200).forEach(a -> h.append("<tr><td class=t>")
                .append(AnalyseJournal.chrono(a.t())).append("</td><td>").append(a.univers()).append("</td><td>")
                .append(esc(a.groupe() + (a.chose().isEmpty() ? "" : " — " + a.chose()) + (Math.round(a.quantite()) > 1 ? " ×" + Math.round(a.quantite()) : "")))
                .append("</td><td>").append(esc(a.montant() + " " + a.paye())).append("</td><td>")
                .append(Double.isNaN(a.part()) ? "—" : String.format(Locale.FRANCE, "%.1f %%", 100 * a.part())).append("</td><td class=det>")
                .append(esc(a.poids())).append("</td><td>").append(Double.isFinite(a.gainSecondes()) ? String.format(Locale.ROOT, "%.0f", a.gainSecondes()) : "—").append("</td></tr>"));
        h.append("</tbody></table></div>");

        // Le journal.
        h.append("<h2>Journal</h2><p class=sub>Tout sauf les achats un par un (dans achats.csv et journal.txt).</p><div class=filters>");
        for (AnalyseJournal.Sorte s : AnalyseJournal.Sorte.values()) {
            if (s == AnalyseJournal.Sorte.ACHAT) continue;
            h.append("<label><input type=checkbox checked data-s=\"").append(s.name()).append("\"> ").append(s.label).append("</label>");
        }
        h.append("</div><input type=search id=qj placeholder=\"Filtrer le journal…\" style=margin-top:8px>");
        h.append("<div class=scroll style=margin-top:8px><table id=tj><thead><tr><th>Chrono</th><th>U</th><th>Sorte</th><th>Quoi</th></tr></thead><tbody>");
        for (AnalyseJournal.Ligne l : j.lignes) {
            if (l.sorte() == AnalyseJournal.Sorte.ACHAT) continue;
            h.append("<tr data-s=\"").append(l.sorte().name()).append("\"><td class=t>").append(AnalyseJournal.chrono(l.t())).append("</td><td>").append(l.univers())
                    .append("</td><td>").append(l.sorte().label).append("</td><td>").append(esc(l.titre()));
            for (String d : l.details()) h.append("<div class=det>").append(esc(d)).append("</div>");
            h.append("</td></tr>");
        }
        h.append("</tbody></table></div>");
        h.append("""
                <script>
                function filt(table,q,extra){for(const r of document.querySelectorAll('#'+table+' tbody tr')){const ok=r.textContent.toLowerCase().includes(q)&&(!extra||extra(r));r.style.display=ok?'':'none';}}
                const qa=document.getElementById('qa');qa.oninput=()=>filt('ta',qa.value.toLowerCase());
                const qj=document.getElementById('qj');const boxes=[...document.querySelectorAll('[data-s][type=checkbox]')];
                function fj(){const on=new Set(boxes.filter(b=>b.checked).map(b=>b.dataset.s));filt('tj',qj.value.toLowerCase(),r=>on.has(r.dataset.s));}
                qj.oninput=fj;boxes.forEach(b=>b.onchange=fj);
                document.querySelectorAll('th').forEach((th,i)=>th.onclick=()=>{const tb=th.closest('table').tBodies[0];const idx=[...th.parentNode.children].indexOf(th);
                const num=th.hasAttribute('data-n');const rows=[...tb.rows];const dir=th.dataset.d==='1'?-1:1;th.dataset.d=dir===1?'1':'0';
                rows.sort((a,b)=>{const x=a.cells[idx].textContent,y=b.cells[idx].textContent;return dir*(num?(parseFloat(x)||0)-(parseFloat(y)||0):x.localeCompare(y,'fr'));});
                rows.forEach(r=>tb.appendChild(r));});
                </script></main></body></html>
                """);
        Files.writeString(file, h.toString());
    }

    /** Les paliers sur une ligne de temps : une marque par palier, les univers en bandes alternées. */
    private static void frise(StringBuilder h, List<AnalyseJournal.Palier> paliers, double fin) {
        if (paliers.isEmpty() || fin <= 0) {
            h.append("<div class=sub>Aucun palier.</div>");
            return;
        }
        double w = 1100, ht = 110, left = 10, right = 10;
        h.append("<svg viewBox=\"0 0 ").append((int) w).append(' ').append((int) ht).append("\" style=\"width:100%;height:auto\">");
        double start = 0;
        int u = 1;
        for (AnalyseJournal.Palier p : paliers) {
            if (p.univers() != u || p == paliers.get(paliers.size() - 1)) {
                double end = p == paliers.get(paliers.size() - 1) ? fin : p.t();
                double x0 = left + (w - left - right) * start / fin, x1 = left + (w - left - right) * end / fin;
                h.append(String.format(Locale.ROOT, "<rect x=%.1f y=40 width=%.1f height=26 fill=\"%s\" opacity=.18 />", x0, Math.max(1, x1 - x0),
                        u % 2 == 0 ? "#2f8f5b" : "#2f6fdb"));
                h.append(String.format(Locale.ROOT, "<text x=%.1f y=80>u%d</text>", x0 + 2, u));
                start = p.t();
                u = p.univers();
            }
        }
        for (AnalyseJournal.Palier p : paliers) {
            double x = left + (w - left - right) * p.t() / fin;
            boolean main = p.nom().contains("acte") || p.nom().contains("formé") || p.nom().startsWith("Fin");
            h.append(String.format(Locale.ROOT, "<line x1=%.1f x2=%.1f y1=%d y2=70 stroke=\"%s\" stroke-width=%s><title>%s — %s</title></line>", x, x,
                    main ? 28 : 36, main ? "#d64a6a" : "currentColor", main ? "2" : "1", AnalyseJournal.chrono(p.t()), esc(p.nom())));
        }
        for (int k = 0; k <= 8; k++) {
            double t = fin * k / 8, x = left + (w - left - right) * k / 8;
            h.append(String.format(Locale.ROOT, "<text x=%.1f y=102 text-anchor=middle>%s</text>", x, AnalyseJournal.chrono(t).substring(0, 5)));
        }
        h.append("</svg><div class=det>Trait rouge : les actes et les univers formés ; trait fin : les autres paliers. Survoler un trait donne son nom.</div>");
    }

    /**
     * Le résumé de chaque étape, d'un palier principal au suivant : sa durée, ses achats, et les trois achats qui ont le
     * plus pesé sur les grandeurs.
     */
    private static void resumes(StringBuilder h, AnalyseJournal j) {
        List<AnalyseJournal.Palier> main = j.paliers.stream()
                .filter(p -> p.nom().contains("acte") || p.nom().contains("formé") || p.nom().contains("galaxie") || p.nom().contains("amas")
                        || p.nom().contains("dernier Big Bang") || p.nom().startsWith("Fin"))
                .toList();
        if (main.isEmpty()) return;
        h.append("<h2>Résumé par étape</h2><div class=scroll><table><thead><tr><th style=min-width:260px>Étape</th><th>De</th><th>À</th><th style=min-width:110px>Durée</th><th>Achats</th><th>Ce qui a le plus pesé</th></tr></thead><tbody>");
        double from = 0;
        String fromName = "Début";
        for (AnalyseJournal.Palier p : main) {
            final double a = from, b = p.t();
            List<AnalyseJournal.Achat> inside = j.achats.stream().filter(x -> x.t() > a && x.t() <= b).toList();
            // Ce qui a le plus pesé : ce qui a fait gagner le plus de temps vers la suite ; à défaut d'estimation, le plus
            // gros multiplicateur (un multiplicateur d'exposant peut valoir ×10^500000 sans avancer plus qu'un autre).
            List<AnalyseJournal.Achat> top = inside.stream().sorted((x, y) -> {
                double gx = Double.isFinite(x.gainSecondes()) ? x.gainSecondes() : 0, gy = Double.isFinite(y.gainSecondes()) ? y.gainSecondes() : 0;
                return gx != gy ? Double.compare(gy, gx) : Double.compare(y.poidsMax(), x.poidsMax());
            }).limit(3).toList();
            StringBuilder best = new StringBuilder();
            for (AnalyseJournal.Achat x : top) {
                if (x.poidsMax() <= 0) continue;
                if (best.length() > 0) best.append("<br>");
                best.append(esc(x.groupe() + (x.chose().isEmpty() ? "" : " — " + x.chose()))).append(" (").append(x.poidsMax() < 3
                        ? String.format(Locale.FRANCE, "×%.2f", Math.pow(10, x.poidsMax())) : String.format(Locale.ROOT, "×10^%.1f", x.poidsMax()));
                if (Double.isFinite(x.gainSecondes()) && x.gainSecondes() >= 1) best.append(", gain de ").append(esc(AnalyseJournal.duree(x.gainSecondes())));
                best.append(")");
            }
            h.append("<tr><td>").append(esc(fromName + " → " + p.nom())).append("</td><td class=t>").append(AnalyseJournal.chrono(a)).append("</td><td class=t>")
                    .append(AnalyseJournal.chrono(b)).append("</td><td>").append(AnalyseJournal.duree(b - a)).append("</td><td>").append(inside.size())
                    .append("</td><td class=det>").append(best.length() == 0 ? "—" : best).append("</td></tr>");
            from = b;
            fromName = p.nom();
        }
        h.append("</tbody></table></div>");
    }

    private static void kpi(StringBuilder h, String value, String label) {
        h.append("<div class=\"card kpi\"><b>").append(esc(value)).append("</b><span>").append(esc(label)).append("</span></div>");
    }

    /** Une courbe par grandeur, en ordres de grandeur, sur le temps de jeu. */
    private static void courbes(StringBuilder h, List<double[]> releves) {
        if (releves.size() < 2) {
            h.append("<div class=sub>Pas assez de relevés.</div>");
            return;
        }
        double w = 1100, ht = 300, left = 46, bottom = 24;
        double t0 = releves.get(0)[0], t1 = releves.get(releves.size() - 1)[0];
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double[] r : releves) for (int i = 1; i < r.length; i++) if (Double.isFinite(r[i])) {
            min = Math.min(min, r[i]);
            max = Math.max(max, r[i]);
        }
        if (!Double.isFinite(min)) return;
        if (max - min < 1) max = min + 1;
        h.append("<svg viewBox=\"0 0 ").append((int) w).append(' ').append((int) ht).append("\" style=\"width:100%;height:auto\">");
        for (int k = 0; k <= 4; k++) {
            double v = min + (max - min) * k / 4, y = ht - bottom - (ht - bottom - 10) * k / 4;
            h.append(String.format(Locale.ROOT, "<line x1=%.0f x2=%.0f y1=%.1f y2=%.1f stroke=\"currentColor\" opacity=.12 />", left, w, y, y));
            h.append(String.format(Locale.ROOT, "<text x=4 y=%.1f>10^%.0f</text>", y + 4, v));
        }
        for (int k = 0; k <= 6; k++) {
            double t = t0 + (t1 - t0) * k / 6, x = left + (w - left - 10) * k / 6;
            h.append(String.format(Locale.ROOT, "<text x=%.1f y=%.0f text-anchor=middle>%s</text>", x, ht - 6, AnalyseJournal.chrono(t).substring(0, 5)));
        }
        Multipliers.Quantity[] qs = Multipliers.Quantity.values();
        StringBuilder legend = new StringBuilder("<div class=legend>");
        for (Multipliers.Quantity q : qs) {
            StringBuilder path = new StringBuilder();
            boolean pen = false;
            for (double[] r : releves) {
                double v = r[q.ordinal() + 1];
                if (!Double.isFinite(v)) {
                    pen = false;
                    continue;
                }
                double x = left + (w - left - 10) * (r[0] - t0) / Math.max(1, t1 - t0), y = ht - bottom - (ht - bottom - 10) * (v - min) / (max - min);
                path.append(pen ? " L" : " M").append(String.format(Locale.ROOT, "%.1f %.1f", x, y));
                pen = true;
            }
            if (path.length() == 0) continue;
            String color = COULEURS[q.ordinal() % COULEURS.length];
            h.append("<path d=\"").append(path).append("\" fill=none stroke=\"").append(color).append("\" stroke-width=1.6 />");
            legend.append("<span><i style=\"background:").append(color).append("\"></i>").append(esc(q.label())).append("</span>");
        }
        h.append("</svg>").append(legend).append("</div>");
    }
}
