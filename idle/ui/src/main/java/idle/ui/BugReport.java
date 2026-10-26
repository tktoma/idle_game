package idle.ui;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.bigbang.BigBangMilestone;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;

/**
 * Le rapport de bug : tout ce qu'il faut pour reproduire un problème, en un seul texte à coller.
 *
 * <p>Depuis la version 0.127, il dit aussi la session (graine du hasard, onglet affiché, profil de test), où en est
 * la partie (acte, dernière étape atteinte et prochaine, univers, graine d'univers, paliers de Big Bang payés et
 * en attente, défi ou épreuve en cours), et garde une trace courte de la première erreur. Il ne contient rien de
 * personnel au-delà du nécessaire : ni chemin de fichier, ni nom de compte (le dossier personnel est remplacé par
 * « ~ » partout), seulement le système et la version de Java.
 *
 * <p>Rien n'est envoyé nulle part : le texte va dans le presse-papiers, et c'est le joueur qui
 * décide à qui le donner.
 */
final class BugReport {

    /** Nombre de lignes de la trace d'erreur gardées dans le rapport : le haut de la pile suffit. */
    static final int TRACE_LINES = 12;

    private BugReport() {
    }

    /**
     * Ce que la fenêtre sait de la session, et que la partie ne garde pas.
     *
     * @param seed    la graine du hasard de cette session
     * @param tab     l'onglet où jouait le joueur (le dernier avant les réglages, d'où le rapport se copie)
     * @param testing vrai dans le profil de test
     */
    record Session(long seed, String tab, boolean testing) {
    }

    /** Le rapport pour cette partie, à l'instant présent. */
    static String of(Game game, SaveStore store, Session session) {
        StringBuilder text = new StringBuilder();
        text.append("Idle ").append(Version.CURRENT).append(" — rapport de bug\n");
        text.append("Java ").append(property("java.version")).append(" · ").append(property("os.name")).append(' ')
                .append(property("os.version")).append(" · ").append(property("os.arch")).append('\n');
        text.append("Session : graine du hasard ").append(session.seed()).append(" (tirages de synthèse depuis le lancement)")
                .append(" · onglet ").append(session.tab())
                .append(session.testing() ? " · profil de test" : "").append('\n');
        text.append('\n');
        text.append("Temps de jeu : ").append(Format.duration(game.state().timePlayed()))
                .append(" · acte : ").append(act(game))
                .append(" · Big Bangs : ").append(game.bigBangs())
                .append(" · explosions : ").append(game.state().explosions())
                .append(" · échelles du cosmos : ").append(game.cosmosFormed())
                .append(" · univers : ").append(game.universes()).append('\n');
        text.append("Jalon : ").append(milestone(game)).append('\n');
        if (game.universes() > 0) {
            text.append("Graine d'univers : palier ").append(game.seedStage()).append(" · ")
                    .append(Format.count(game.state().universeSeeds())).append(" graine(s) d'univers").append('\n');
            // Le chrono (version 0.139) fait passer tout le jeu plus vite : le rapport dit son réglage.
            if (game.isChronoUnlocked()) {
                text.append("Chrono : ×").append(game.chronoSpeed()).append(" · gardé ")
                        .append(Format.duration(game.chronoStored())).append(" sur ")
                        .append(Format.duration(game.chronoCapacity())).append('\n');
            }
        }
        text.append("Paliers de Big Bang : ").append(milestones(game)).append('\n');
        text.append("En cours : ").append(running(game)).append('\n');
        text.append("Sauvegarde : ").append(store.statusWithoutPath()).append('\n');
        text.append('\n');
        if (Crash.count() == 0) {
            text.append("Aucune erreur depuis le lancement.\n");
        } else {
            text.append("Erreurs depuis le lancement : ").append(Crash.count()).append(". La première, en bref :\n")
                    .append(shortTrace(Crash.first()));
        }
        // L'export vient après l'anonymisation : remplacer des lettres dans la ligne codée abîmerait la partie jointe.
        return anonymous(text.toString()) + "\nPartie :\n" + store.export(game) + '\n';
    }

    /** L'acte où en est la partie : 1 à 3, ou la partie secondaire une fois l'univers formé. */
    private static String act(Game game) {
        if (game.isUniverseFormed() && !game.isReplaying()) return "univers formé (partie secondaire)";
        String act = game.isBigBangUnlocked() ? "3" : game.isDarkMatterUnlocked() ? "2" : "1";
        return game.isReplaying() ? act + " (univers " + (game.universes() + 1) + ")" : act;
    }

    /** La dernière grande étape atteinte dans la partie en cours, et la prochaine. */
    private static String milestone(Game game) {
        GameStats.Step last = null;
        for (GameStats.Step step : GameStats.Step.values()) {
            if (game.stats().reachedThisUniverse(step)) last = step;
        }
        GameStats.Step next = game.nextStep();
        return (last == null ? "aucune étape encore" : "dernière étape « " + last.label() + " »")
                + (next == null ? "" : ", prochaine « " + next.label() + " »");
    }

    /** Les paliers payés, et ceux qui sont ouverts sans l'être encore. */
    private static String milestones(Game game) {
        List<String> bought = new ArrayList<>(), waiting = new ArrayList<>();
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            if (game.isBigBangMilestoneReached(milestone)) bought.add(milestone.name());
            else if (game.isBigBangMilestoneOpen(milestone)) waiting.add(milestone.name());
        }
        if (bought.isEmpty() && waiting.isEmpty()) return "aucun ouvert";
        return bought.size() + " payé(s)" + (bought.isEmpty() ? "" : " (" + String.join(", ", bought) + ")")
                + (waiting.isEmpty() ? "" : " · à payer : " + String.join(", ", waiting));
    }

    /** Le défi, le défi de Big Bang ou l'épreuve en cours. */
    private static String running(Game game) {
        List<String> parts = new ArrayList<>();
        if (game.activeChallenge() != null) parts.add("défi « " + game.activeChallenge().name() + " »");
        if (game.activeBangChallenge() != null) parts.add("défi de Big Bang « " + game.activeBangChallenge().label() + " »");
        if (game.activeTrial() != null) parts.add("épreuve « " + game.activeTrial().label() + " »");
        if (game.state().automationPaused()) parts.add("automatismes coupés");
        return parts.isEmpty() ? "rien de particulier" : String.join(" · ", parts);
    }

    /** Les {@link #TRACE_LINES} premières lignes d'une trace d'erreur : le message et le haut de la pile. */
    static String shortTrace(String trace) {
        String[] lines = trace.split("\\R");
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, TRACE_LINES); i++) kept.append(lines[i].stripTrailing()).append('\n');
        if (lines.length > TRACE_LINES) kept.append("… (").append(lines.length - TRACE_LINES).append(" lignes de plus)\n");
        return kept.toString();
    }

    /**
     * Le dossier personnel (qui contient le nom du compte) remplacé par « ~ », puis le nom du compte s'il apparaît
     * encore comme un mot à part entière (dans un chemin hors du dossier personnel, par exemple). La partie codée n'y
     * passe pas.
     */
    static String anonymous(String text) {
        String home = property("user.home");
        String name = property("user.name");
        String result = text;
        if (!home.equals("?") && home.length() > 2) result = result.replace(home, "~");
        if (!name.equals("?") && name.length() > 2) {
            result = result.replaceAll("(?<![\\p{L}\\p{N}])" + java.util.regex.Pattern.quote(name) + "(?![\\p{L}\\p{N}])",
                    "<compte>");
        }
        return result;
    }

    private static String property(String name) {
        try {
            return System.getProperty(name, "?");
        } catch (RuntimeException hidden) {
            return "?";
        }
    }
}
