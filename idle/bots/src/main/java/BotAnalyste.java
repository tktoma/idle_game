import idle.core.history.*;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import idle.core.*;
import idle.core.state.GameState;
import idle.core.bigbang.BigBangCondition;
import idle.core.save.SaveCodec;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Le bot analyste : il joue comme le meilleur profil (le Speedrunner), mais à vitesse humaine, une décision toutes les
 * cinq secondes, et note tout, à la seconde de jeu près :
 * <ul>
 * <li>chaque achat : quoi, combien de niveaux, ce qu'il a coûté et dans quelle monnaie, la part de la réserve que cela
 * représentait, son poids sur chaque grandeur du jeu (particules, atomes, matière noire, espace…) et le temps qu'il
 * fait gagner selon l'estimation du moment ;</li>
 * <li>chaque chose obtenue sans payer (succès, titres, défis réussis), chaque automatisme mis en marche ou coupé ;</li>
 * <li>chaque palier : les trois actes de chaque univers (première explosion, premier Big Bang, univers formé) et leurs
 * étapes (huitième Big Bang, galaxie, amas), les paliers de la graine, les dimensions, leurs seuils et leurs paliers
 * finals, le défi final et la fin ; avec une sauvegarde à chaque palier ;</li>
 * <li>une estimation régulière du temps jusqu'au prochain palier, en jouant la suite sur une copie de la partie, et le
 * goulot du moment.</li>
 * </ul> Si rien n'avance (plus de 24 h estimées avant le prochain palier, ou 30 minutes sans palier, achat ni dépense),
 * il s'arrête et dit pourquoi. Tout s'écrit en texte brut, en CSV et en une page HTML ({@link AnalyseRapport}).
 *
 * <p>Il dit aussi pourquoi. Chaque achat a sa rentabilité (le temps de production qu'il a coûté, contre le temps qu'il
 * fait gagner) et le rapport classe tout ce qui a été acheté. Chaque défi lancé dit sa contrainte, sa récompense,
 * pourquoi maintenant et combien de temps il prendra (joué sur une copie) ; chaque succès, ce qu'il demandait, ce qu'il
 * apporte et son poids ; chaque automatisme, ce qu'il fait ; chaque explosion, la matière noire qu'elle rapporte par
 * heure. À chaque palier principal, « et si » : la suite jouée sur des copies par un joueur qui passe une fois par
 * minute, puis toutes les dix minutes.
 *
 * <p>Il va jusqu'à la fin de la partie. Après l'univers 11, il continue de former des univers (l'énergie du vide paie
 * les bulles), prend les bulles du sommet (l'automatisation des dimensions) et des racines (le défi final), laisse les
 * automatismes jouer les dimensions, et s'arrête quand les dix paliers du défi final sont atteints ; le défi ouvert, il
 * estime le palier suivant du défi. Il peut aussi repartir d'une sauvegarde (un palier d'une partie d'avant, par
 * exemple).
 *
 * <p>Usage : {@code BotAnalyste <dossier> [univers=fin] [heures=400] [graine=201] [pas=5] [estimation_min=30]
 * [comparer=false] [etsi=true] [depart=sauvegarde] [sans=id,id] [profil=speedrunner] [ordre]} ; {@code univers} est un
 * nombre d'univers, ou {@code fin} pour jouer jusqu'à la fin. Version 0.164 : {@code sans} retire ces améliorations du
 * jeu (identifiants des améliorations ordinaires ou de l'arbre de matière noire) ; avec {@link AnalyseComparaison}, on
 * voit ce que chacune apporte. À la fin, les détecteurs d'illogismes ({@link AnalyseIllogismes}) relisent la partie.
 * Avec {@code comparer=true}, il compare aussi six façons de jouer à chaque palier principal et garde la plus rapide
 * ({@link #choisirStrategie()}) : six parties d'essai par palier, la partie dure plusieurs fois plus longtemps. Avec
 * {@code etsi=false}, il ne joue pas les « et si » : la partie se calcule environ deux fois plus vite.
 *
 * <p>Version 0.166 : {@code profil} choisit le joueur : le Speedrunner (par défaut), le Régulier (une décision par
 * minute, moins attentif), ou l'absent (le Speedrunner, cinq minutes par heure, le jeu fermé entre deux). Chaque palier
 * dit sa cause (la dernière chose faite dans la minute d'avant ; pour un Big Bang, sa dernière condition remplie), le
 * temps réel, en ligne et hors ligne ; le suivi continu ({@link AnalyseSuivi}) mesure les améliorations des dimensions
 * et de la graine sur leur propre palier et nourrit deux détecteurs de plus ; decisions.csv compte les décisions par
 * heure.
 *
 * <p>À chaque palier, la mémoire du bot et le hasard de la partie s'écrivent à côté de la sauvegarde ({@link
 * AnalyseMemoire}) ; repartie de cette sauvegarde, la tranche suivante continue la même partie. {@code sans} retire
 * aussi des bulles de l'arbre ({@code bulle:LEAVES_LEFT}), et {@code ordre} impose l'ordre des bulles à tous les
 * profils ({@code meilleur} ou {@code moins-cher}). Les achats faits au pas d'une remise à zéro se notent, le palier
 * final de chaque dimension aussi, et l'attente d'une seule condition de la galaxie dit laquelle.
 */
public final class BotAnalyste extends BotAnalysteBase {
    BotAnalyste(Game game, Profile profile, long seed, double step, Path dossier, double estimationMinutes) {
        super(game, profile, seed, step, dossier, estimationMinutes);
    }

    BotAnalyste(Game game, long seed, double step, Path dossier, double estimationMinutes) {
        super(game, seed, step, dossier, estimationMinutes);
    }

    /**
     * Joue la suite sur une copie de la partie, avec le même jeu, jusqu'au prochain palier principal : le temps qu'il a
     * fallu, ou −1 au-delà de l'horizon (24 h).
     */
    double essai(int cible, int bigBangCible, double horizon) { return essai(cible, bigBangCible, horizon, null); }

    /** Le même essai, la copie jouant avec les choix de cette façon de jouer ({@code null} : la sienne). */
    double essai(int cible, int bigBangCible, double horizon, AnalyseStrategies.Strategie strategie) {
        return essai(game -> indexPrincipal(game) >= cible || bigBangCible > 0 && game.bigBangs() >= bigBangCible, horizon, strategie, 0);
    }

    /**
     * Joue la suite sur une copie jusqu'à ce que {@code but} soit vrai : le temps qu'il a fallu, ou −1 au-delà de
     * l'horizon. La copie joue avec les choix de {@code strategie} ({@code null} : les siens), et passe toutes les
     * {@code rythme} secondes ({@code 0} : à son rythme).
     */
    double essai(Predicate<Game> but, double horizon, AnalyseStrategies.Strategie strategie, double rythme) {
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
            Field field = RobotCosmosPlanner.class.getDeclaredField("step");
            field.setAccessible(true);
            field.setDouble(clone, pas);
        } catch (ReflectiveOperationException impossible) {
            // Le pas reste celui du bot : l'essai est seulement plus lent.
        }
        return clone;
    }

    /** Le palier que viserait une estimation maintenant. */
    String viseeDuMoment() {
        if (g.isFinalChallengeOpen()) return "Défi final : " + (g.finalTiersReached() + 1) + " palier(s) sur 10";
        int cible = indexPrincipal + 1 + ((indexPrincipal + 1) % 7 == 0 ? 1 : 0);
        int bang = g.bigBangs() >= 1 && g.bigBangs() + 1 < Bots.LAST_BIG_BANG && etape(g) == 2 ? g.bigBangs() + 1 : 0;
        return bang > 0 ? "Univers " + univers() + " : Big Bang n° " + bang : nomPalier(cible);
    }

    void estimer() {
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
     * Le défi final ouvert : l'estimation vise son palier suivant, joué sur une copie. Au-delà de 24 h,
     * le bot s'arrête, comme ailleurs.
     */
    void estimerDefiFinal() {
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
    void espacer(double secondes) {
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
     * Le choix de la façon de jouer jusqu'au prochain palier principal : chacune joue la suite sur une
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
     * « Et si » au palier principal qui vient d'être atteint : la suite jusqu'au palier principal suivant,
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
            IntStream.range(0, RYTHMES_ETSI.length).parallel()
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

    // ------------------------------------------------------------------
    // Le blocage
    // ------------------------------------------------------------------

    void bloque(String raison) {
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
        String fin = blocage != null ? "Arrêt sur blocage : " + blocage : fini ? "Fin de la partie atteinte"
                : NextUniversePolicy.reached(g, targetUniverses) ? "Objectif atteint : " + targetUniverses + " univers" : "Limite de " + heures + " h atteinte";
        // Les détecteurs d'illogismes relisent la partie.
        suivi.fin(temps());
        illogismes = AnalyseIllogismes.detecter(this);
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, illogismes.size() + " illogismes repérés (détail dans illogismes.txt et le rapport)");
        journal.ajoute(temps(), univers(), AnalyseJournal.Sorte.INFO, fin, List.of(String.format(Locale.FRANCE, "Calcul : %.0f s de temps réel", seconds)));
        journal.ecrire(dossier);
        AnalyseIllogismes.ecrire(illogismes, dossier);
        Files.writeString(dossier.resolve("fin.save"), SaveCodec.write(g.state(), 0));
        // La mémoire du bot à côté, pour qu'une tranche suivante reparte de fin.save comme d'un palier.
        AnalyseMemoire.ecrire(this, dossier.resolve("fin.save"));
        AnalyseRapport.ecrire(this, dossier.resolve("rapport.html"), fin);
        System.out.println("ANALYSTE," + fin + "," + AnalyseJournal.chrono(temps()) + "," + dossier.toAbsolutePath());
    }
}
