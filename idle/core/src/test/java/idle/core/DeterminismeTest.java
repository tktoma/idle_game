package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import idle.core.save.SaveCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Version 0.296 : le moteur reste déterministe. En 0.162, un {@code Set.copyOf} parcouru donnait un ordre différent à
 * chaque lancement de Java, et deux parties de même graine divergeaient. Relu en 0.296 : tous les ensembles et
 * tableaux associatifs non ordonnés du moteur ne servent qu'à des recherches, jamais à un parcours. Ce test garde
 * les deux choses : le code ne reprend pas ces constructions, et une partie rejouée donne la même sauvegarde.
 */
class DeterminismeTest {
    /**
     * Les constructions dont l'ordre de parcours change d'un lancement de Java à l'autre. {@code Set.of} et
     * {@code Map.of} non vides ne sont admis qu'aux endroits listés : des catalogues seulement consultés.
     */
    private static final Pattern RANDOM_ORDER = Pattern.compile(
            "(?:Collectors\\.toSet\\(|Collectors\\.toMap\\(|groupingBy\\(|toUnmodifiableSet\\(|toUnmodifiableMap\\()"
                    + "|(?<![\\w.])(?:Set\\.copyOf\\(|Map\\.copyOf\\(|Set\\.of\\((?!\\))|Map\\.of\\((?!\\))|Map\\.ofEntries\\()");

    /** Les catalogues admis : lus par {@code get} ou {@code contains}, jamais parcourus (vérifié en 0.296). */
    private static final List<String> ALLOWED = List.of(
            "GameBigBang.java: BANG_REWARDS", "Photo.java: IGNOREES", "Achats.java: ACHATS", "Achats.java: REGLAGES");

    @Test
    void leMoteurNeParcourtRienDansUnOrdreAuHasard() throws IOException {
        Path root = Path.of("src/main/java");
        if (!Files.isDirectory(root)) root = Path.of("core/src/main/java");
        Assumptions.assumeTrue(Files.isDirectory(root), "lancé hors du projet : pas de sources à relire");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).replaceAll("//.*", "");
                    if (line.trim().startsWith("*") || line.trim().startsWith("/*")) continue;
                    Matcher m = RANDOM_ORDER.matcher(line);
                    if (!m.find()) continue;
                    String where = file.getFileName() + ": ";
                    if (ALLOWED.stream().anyMatch(a -> a.startsWith(where) && line.contains(a.substring(where.length())))) continue;
                    found.add(where + (i + 1) + " " + line.trim());
                }
            }
        }
        assertEquals(List.of(), found, "un parcours dans un ordre qui change d'un lancement à l'autre ; préférer TreeSet, "
                + "LinkedHashMap, EnumMap ou une liste");
    }

    @Test
    void unePartieRejoueeDonneLaMemeSauvegarde() {
        String first = play(), second = play();
        assertEquals(first, second);
        assertNotEquals(start(), first, "la partie a bien avancé");
    }

    private static Game game() {
        Game game = GameTestSupport.universe(4);
        GameTestSupport.openDimensionAutomations(game, 3);
        return game;
    }

    private static String start() { return SaveCodec.write(game().state(), 0); }

    /** Vingt minutes de jeu au quart de seconde, automatismes compris. */
    private static String play() {
        Game game = game();
        for (int k = 0; k < 4 * 60 * 20; k++) game.tick(0.25);
        return SaveCodec.write(game.state(), 0);
    }
}
