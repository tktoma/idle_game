package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La sauvegarde ({@link SaveCodec}) et le retour après une absence ({@link Absence}).
 *
 * <p>Le test central compare une partie à sa copie relue <b>champ par champ, par réflexion</b> : un
 * champ ajouté plus tard à {@link GameState}, {@link GameStats} ou {@link StatsHistory} et oublié
 * dans la sauvegarde le fait échouer. Un second test vérifie que la partie d'essai a bien sorti
 * chaque champ de sa valeur de départ : sans cela, un champ oublié passerait inaperçu.
 */
class SaveTest {

    /** Ce qui se recalcule et ne fait pas partie de la sauvegarde. */
    private static final Set<String> DERIVED = Set.of("elementsVersion", "moleculesVersion", "substancesSeen",
            "assembliesSeen", "bodiesSeen", "moleculeLog");

    // ------------------------------------------------------------------
    // Une partie où tout a bougé
    // ------------------------------------------------------------------

    /** Une partie d'essai où chaque champ a quitté sa valeur de départ. Elle ne respecte aucune règle : seule la sauvegarde l'intéresse. */
    static GameState richState() {
        GameState state = new GameState();
        state.setStarted(true);
        state.setParticles(BigNum.of(1.234567890123, 345));
        state.setAtoms(BigNum.of(42));
        state.setTotalAtoms(BigNum.of(9.87, 14));
        state.setTimeSinceFusion(12.5);
        state.setFormation(0, 0.25);
        state.setFormation(3, 0.999);
        state.setTimePlayed(123456.789);
        state.setLevel("speed", 31);
        state.setLevel("coupling", 4);
        state.addAutomation("auto_speed");
        state.addAutomation("auto_fusion");
        state.setAutomationEnabled("auto_speed", true);
        state.setAutomationSpeedLevel("auto_speed", 7);
        state.setAutomationTimer("auto_speed", 0.125);
        state.setElementCount(1, 3);
        state.setElementCount(26, 1);
        state.setElementCount(118, 2);
        state.setSynthesisCount(77);
        state.setSynthesisTarget(ElementCategory.values()[2]);
        state.setSynthesisTries(3);
        state.setDarkMatter(BigNum.of(17));
        state.setDarkMatterSpent(BigNum.of(350));
        state.setDarkMatterSize(BigNum.of(3.3, 12));
        state.setExplosions(64);
        state.setTableWeightLevel(9);
        state.setDarkLevel("dark_a", 2);
        state.setDarkLevel("dark_b", 1);
        state.setFusionThreshold(12);
        state.setSynthesisReserve(BigNum.of(5, 6));
        state.setHoldLocked(true);
        state.setActiveChallenge("défi, avec : des = signes | bizarres % et *");
        state.addCompletedChallenge("challenge_a");
        state.addCompletedChallenge("challenge_b");
        state.setChallengeTime("challenge_a", 4321.5);
        state.setDecayDebt(0.75);
        state.addAchievement("second");
        state.addAchievement("first");
        state.setDarkAutomationEnabled("dark_auto", true);
        state.setDarkAutomationTimer("dark_auto", 1.5);
        state.setBigBangs(5);
        state.addMolecules("H2O", 12);
        state.addMolecules("Ca5(PO4)3F", 3);
        state.addMolecules("H2O", 1_000_000);
        state.addMolecules("CO2", 1);
        state.addSubstance("H2O");
        state.addSubstance("CO2");
        state.addAssembly("ocean");
        state.addAssembly("atmosphere");
        state.addBody("planet");
        state.setCosmosLevel(2);
        state.setAutoHold(false);
        state.setAutoMolecules(false);
        state.setMoleculeAutomated("H2O", true);
        state.setMoleculeAutomated("CO2", true);
        state.setAutomationPaused(true);
        state.setAutoGather(false);
        state.setAutoForm(false);
        state.setAutoMoleculeReserve(0.25);
        state.setExplosionRecord(431.5);
        state.setCometWait(512.25);
        state.setCometVisible(12.5);
        state.setCometBoost(88.75);
        state.setCometsCaught(7);
        state.setRecord("FIRST_FUSION", 2280.5);
        state.setRecord("GALAXY", 231_456.75);
        state.setActiveBangChallenge("FORGOTTEN");
        state.addCompletedBangChallenge("VOID");
        state.setBangChallengeTime("VOID", 3120.5);
        state.setBangChallengeStarted(190_000.25);
        state.setAtomUpgradeOrder(List.of("atom_mass", "atom_double"));
        state.setAtomUpgradeOrdered(true);
        state.setAutomationOrder(List.of("auto_fusion", "auto_speed"));
        state.setAutomationOrdered(true);
        state.setMoleculeFavorite("CO2", true);
        state.setMoleculeFavorite("H2O", true);
        state.addSpaceUpgrade("space_b");
        state.addSpaceUpgrade("space_a");
        state.setSpace(BigNum.of(5.17, 10));

        GameStats stats = state.stats();
        stats.addParticles(BigNum.of(3, 400));
        stats.noteProduction(BigNum.of(2, 380));
        stats.noteParticleUpgrade(BigNum.of(1, 50), true);
        stats.noteParticleUpgrade(BigNum.of(1, 60), false);
        stats.noteAtomUpgrade(BigNum.of(25));
        stats.noteFusion(BigNum.of(8), 30);
        stats.noteFusion(BigNum.of(11), 12.25);
        stats.noteAutomationAction();
        stats.noteDarkAutomationAction();
        stats.noteSpeedPrime();
        stats.noteDarkAutomationAction();
        stats.noteSynthesis(2, true);
        stats.noteSynthesisTry();
        stats.noteTargetedSynthesis();
        stats.addHoldTime(456.5);
        stats.noteUnlocked("auto_speed");
        stats.noteUnlocked("auto_fusion");
        stats.addTime(900);
        stats.endRun();
        stats.addTime(700);
        stats.endRun();
        stats.addTime(1200);
        stats.noteBigBang(104, 5000);
        stats.addTime(800);
        stats.noteBigBang(23, 9000);
        stats.noteMoleculeCreation(120, 44, true);
        stats.noteMoleculeCreation(1, 3, false);
        stats.noteStep(GameStats.Step.FIRST_MOLECULE, 5100.5);
        stats.noteStep(GameStats.Step.UNIVERSE, 245040);
        // Après les remises à zéro : ce qui court « depuis la dernière explosion » doit avoir une valeur aussi.
        stats.addTime(333.25);
        stats.addParticles(BigNum.of(7, 20));
        stats.noteFusion(BigNum.of(2), 40);
        fill(stats.history(), 11, 640);
        fill(stats.runHistory(), 23, 5);
        return state;
    }

    /** Remplit un historique jusqu'à ce qu'il ait dû s'éclaircir une fois, avec des valeurs de toutes sortes. */
    private static void fill(StatsHistory history, long seed, double step) {
        Random random = new Random(seed);
        int stats = StatsHistory.Stat.values().length;
        for (int i = 0; i < StatsHistory.CAPACITY + 7; i++) {
            double[] values = new double[stats];
            for (int s = 0; s < stats; s++) {
                values[s] = switch (s % 6) {
                    case 0 -> Double.NaN;
                    case 1 -> i / 40;                              // une valeur qui se répète longtemps
                    case 2 -> random.nextDouble() * 3000;
                    case 3 -> 133_026_596L + i;                    // un grand entier, exact
                    case 4 -> i < 50 ? Double.NEGATIVE_INFINITY : -random.nextDouble();
                    default -> i % 9 == 0 ? Double.NaN : 1e-9 * random.nextDouble();
                };
            }
            Map<String, Double> delays = new HashMap<>();
            if (i > 20) delays.put("auto_speed", Math.max(0.1, 5.0 / (1 + i / 30)));
            if (i > 300) delays.put("auto: étrange, id", 0.5);
            history.add(new StatsHistory.Sample(i * step, values, delays));
        }
        history.notePeak(StatsHistory.Stat.PARTICLES, 123.456);
        history.notePeak(StatsHistory.Stat.DARK_MATTER, 7);
    }

    // ------------------------------------------------------------------
    // Comparaison champ par champ
    // ------------------------------------------------------------------

    /**
     * Les endroits où deux objets diffèrent, par réflexion, en descendant dans les classes du jeu,
     * les listes, les tables et les tableaux. Les valeurs des courbes ({@code values} et
     * {@code delays} d'un relevé) sont comparées à sept chiffres près : c'est la précision gardée.
     */
    private static List<String> differences(String path, Object a, Object b, boolean loose) {
        List<String> found = new ArrayList<>();
        compare(path, a, b, loose, found);
        return found;
    }

    private static void compare(String path, Object a, Object b, boolean loose, List<String> found) {
        if (a == null || b == null) {
            if (a != b) found.add(path + " : " + a + " ≠ " + b);
        } else if (a instanceof Double x && b instanceof Double y) {
            if (!sameNumber(x, y, loose)) found.add(path + " : " + x + " ≠ " + y);
        } else if (a instanceof double[] x && b instanceof double[] y) {
            if (x.length != y.length) found.add(path + " : longueurs " + x.length + " ≠ " + y.length);
            for (int i = 0; i < Math.min(x.length, y.length); i++) {
                if (!sameNumber(x[i], y[i], loose)) found.add(path + "[" + i + "] : " + x[i] + " ≠ " + y[i]);
            }
        } else if (a instanceof Map<?, ?> x && b instanceof Map<?, ?> y) {
            if (!x.keySet().equals(y.keySet())) found.add(path + " : clés " + x.keySet() + " ≠ " + y.keySet());
            for (Object key : x.keySet()) {
                if (y.containsKey(key)) compare(path + "{" + key + "}", x.get(key), y.get(key), loose, found);
            }
        } else if (a instanceof List<?> x && b instanceof List<?> y) {
            if (x.size() != y.size()) found.add(path + " : tailles " + x.size() + " ≠ " + y.size());
            for (int i = 0; i < Math.min(x.size(), y.size()); i++) compare(path + "[" + i + "]", x.get(i), y.get(i), loose, found);
        } else if (a instanceof java.util.LinkedHashSet<?> x && b instanceof java.util.LinkedHashSet<?> y) {
            compare(path, new ArrayList<Object>(x), new ArrayList<Object>(y), loose, found);   // l'ordre compte
        } else if (a instanceof Collection<?> x && b instanceof Collection<?> y) {
            if (!Set.copyOf(x).equals(Set.copyOf(y))) found.add(path + " : " + x + " ≠ " + y);
        } else if (a instanceof Record && !(a instanceof BigNum)) {
            for (RecordComponent component : a.getClass().getRecordComponents()) {
                try {
                    boolean curve = component.getName().equals("values") || component.getName().equals("delays");
                    compare(path + "." + component.getName(), component.getAccessor().invoke(a),
                            component.getAccessor().invoke(b), loose || curve, found);
                } catch (ReflectiveOperationException impossible) {
                    throw new AssertionError(impossible);
                }
            }
        } else if (a instanceof GameState || a instanceof GameStats || a instanceof StatsHistory) {
            for (Field field : fields(a.getClass())) {
                try {
                    Object x = field.get(a);
                    Object y = field.get(b);
                    if (field.getName().equals("logEnds")) {
                        // Seules comptent les suites réellement posées : le tableau est plus long que nécessaire.
                        int runs = ((GameState) a).moleculeLog().isEmpty() ? 0 : ((List<?>) read(a, "logKinds")).size();
                        x = java.util.Arrays.copyOf((int[]) x, runs);
                        y = java.util.Arrays.copyOf((int[]) y, Math.min(runs, ((int[]) y).length));
                        if (!java.util.Arrays.equals((int[]) x, (int[]) y)) found.add(path + ".logEnds");
                        continue;
                    }
                    compare(path + "." + field.getName(), x, y, loose, found);
                } catch (IllegalAccessException impossible) {
                    throw new AssertionError(impossible);
                }
            }
        } else if (!a.equals(b)) {
            found.add(path + " : " + a + " ≠ " + b);
        }
    }

    private static boolean sameNumber(double x, double y, boolean loose) {
        if (Double.compare(x, y) == 0) return true;
        return loose && Math.abs(x - y) <= 1e-6 * Math.max(Math.abs(x), Math.abs(y));
    }

    /** Les champs d'une classe du jeu qui font partie de la sauvegarde. */
    private static List<Field> fields(Class<?> type) {
        List<Field> kept = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || DERIVED.contains(field.getName())) continue;
            field.setAccessible(true);
            kept.add(field);
        }
        return kept;
    }

    private static Object read(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Les champs, en descendant dans les statistiques et les historiques, qui ont encore leur valeur de départ. */
    private static List<String> untouched(String path, Object rich, Object fresh) {
        List<String> same = new ArrayList<>();
        for (Field field : fields(rich.getClass())) {
            try {
                Object x = field.get(rich);
                Object y = field.get(fresh);
                String here = path + "." + field.getName();
                if (x instanceof GameStats || x instanceof StatsHistory) same.addAll(untouched(here, x, y));
                else if (differences(here, x, y, false).isEmpty()) same.add(here);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            }
        }
        return same;
    }

    private static GameState copy(GameState state) throws SaveCodec.Unreadable {
        return SaveCodec.read(SaveCodec.write(state, 1_700_000_000_000L)).state();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Nested
    class Sauvegarde {

        @Test
        void laPartieDEssaiASortiChaqueChampDeSaValeurDeDepart() {
            assertEquals(List.of(), untouched("partie", richState(), new GameState()),
                    "ces champs ont encore leur valeur de départ dans richState() : un oubli dans la sauvegarde ne se verrait pas");
        }

        @Test
        void unePartieRelueEstLaMemeChampParChamp() throws Exception {
            GameState state = richState();
            assertEquals(List.of(), differences("partie", state, copy(state), false));
        }

        @Test
        void unePartieNeuveSeRelitAussi() throws Exception {
            assertEquals(List.of(), differences("partie", new GameState(), copy(new GameState()), false));
        }

        @Test
        void ecrireRelireEcrireDonneLeMemeTexte() throws Exception {
            String first = SaveCodec.write(richState(), 123);
            assertEquals(first, SaveCodec.write(SaveCodec.read(first).state(), 123));
        }

        @Test
        void lInstantDeLaSauvegardeEtLaVersionSontRendus() throws Exception {
            SaveCodec.Save save = SaveCodec.read(SaveCodec.write(new GameState(), 1_700_000_123_456L));
            assertEquals(1_700_000_123_456L, save.savedAt());
            assertEquals(SaveCodec.VERSION, save.version());
        }

        @Test
        void lesMoleculesGardentLOrdreDeLeurCreation() throws Exception {
            GameState state = richState();
            GameState read = copy(state);
            assertEquals(state.moleculeLog().size(), read.moleculeLog().size());
            assertEquals("H2O", read.moleculeLog().get(0));
            assertEquals("Ca5(PO4)3F", read.moleculeLog().get(12));
            assertEquals("H2O", read.moleculeLog().get(15));
            assertEquals("CO2", read.moleculeLog().get(read.moleculeLog().size() - 1));
            assertEquals(1_000_012, read.moleculeCount("H2O"));
            assertEquals(List.of("H2O", "CO2"), read.substances());
        }

        @Test
        void unIdentifiantPeutContenirLesSeparateurs() throws Exception {
            assertEquals("défi, avec : des = signes | bizarres % et *", copy(richState()).activeChallenge());
            assertEquals("a%2Cb%3Ac%3Dd%25e", SaveData.escape("a,b:c=d%e"));
            assertEquals("a,b:c=d%e", SaveData.unescape(SaveData.escape("a,b:c=d%e")));
        }

        @Test
        void lesNombresSeRelisentSansPerte() {
            for (double value : new double[] {0, 1, -1, 0.1, 1e-300, 1e300, 123456.789, Math.PI, 1e15, 12345678901234567.0,
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                assertEquals(value, SaveData.parse(SaveData.number(value)), "valeur " + value);
            }
            assertTrue(Double.isNaN(SaveData.parse(SaveData.number(Double.NaN))));
            assertEquals("12", SaveData.number(12.0));
        }

        @Test
        void uneColonneDHistoriqueSeReplieEtSeDeplie() {
            double[] column = {Double.NaN, Double.NaN, 3, 3, 3, 1.5, Double.NEGATIVE_INFINITY, 133_026_596, 0, 0};
            String packed = StatsHistory.pack(column);
            assertEquals("*2;3*3;1.5;-Infinity;133026596;0*2", packed);
            double[] read = StatsHistory.unpack(packed, column.length);
            for (int i = 0; i < column.length; i++) assertEquals(column[i], read[i], "rang " + i);
            // Une valeur absente seule en tête, puis des valeurs : le séparateur ne doit pas se perdre.
            double[] holes = {Double.NaN, 4.5, Double.NaN, Double.NaN, 2, Double.NaN};
            assertEquals(";4.5;*2;2;", StatsHistory.pack(holes));
            double[] back = StatsHistory.unpack(StatsHistory.pack(holes), holes.length);
            for (int i = 0; i < holes.length; i++) assertEquals(holes[i], back[i], "rang " + i);
            // Une colonne absente de la sauvegarde : aucune valeur.
            for (double value : StatsHistory.unpack("", 4)) assertTrue(Double.isNaN(value));
        }

        @Test
        void uneListeQuiCommenceParUneValeurAbsenteSeRelit() {
            SaveData data = new SaveData();
            data.putDoubleList("liste", List.of(Double.NaN, 1.5, Double.NaN, 3.0));
            List<Double> read = data.doubleList("liste");
            assertEquals(4, read.size());
            assertTrue(Double.isNaN(read.get(0)) && read.get(1) == 1.5 && Double.isNaN(read.get(2)) && read.get(3) == 3.0);
        }

        @Test
        void lExportTientSurUneLigneEtSeRelit() throws Exception {
            GameState state = richState();
            String exported = SaveCodec.export(state, 55);
            assertTrue(exported.startsWith(SaveCodec.EXPORT_PREFIX));
            assertFalse(exported.contains("\n"));
            assertTrue(exported.matches("idle1:[A-Za-z0-9+/=]+"));
            SaveCodec.Save save = SaveCodec.read("  \n" + exported + "\r\n ");
            assertEquals(55, save.savedAt());
            assertEquals(List.of(), differences("partie", state, save.state(), false));
            // Un copier-coller qui a replié la ligne ne la casse pas.
            String folded = exported.substring(0, 80) + "\n" + exported.substring(80, 200) + " \r\n" + exported.substring(200);
            assertEquals(List.of(), differences("partie", state, SaveCodec.read(folded).state(), false));
        }

        @Test
        void lExportEstBienPlusCourtQueLeFichier() {
            GameState state = richState();
            assertTrue(SaveCodec.export(state, 0).length() < SaveCodec.write(state, 0).length());
        }

        @Test
        void ceQuiNEstPasUneSauvegardeEstRefuse() {
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(null));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("   "));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("bonjour"));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("idle1:pas du base64 !!"));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("idle1:aGVsbG8="));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("idle-save x\n"));
        }

        @Test
        void unExportTronqueEstRefuse() {
            String exported = SaveCodec.export(richState(), 0);
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(exported.substring(0, exported.length() / 2)));
        }

        @Test
        void uneSauvegardeDUneVersionPlusRecenteEstRefusee() {
            String future = SaveCodec.write(new GameState(), 0).replaceFirst("idle-save " + SaveCodec.VERSION,
                    "idle-save " + (SaveCodec.VERSION + 1));
            SaveCodec.Unreadable refused = assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(future));
            assertTrue(refused.getMessage().contains("plus récente"));
        }

        @Test
        void uneValeurImpossibleEstRefusee() {
            String text = SaveCodec.write(richState(), 0);
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(text.replaceFirst("\nexplosions=64\n", "\nexplosions=-3\n")));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(text.replaceFirst("\nexplosions=64\n", "\nexplosions=beaucoup\n")));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(text.replaceFirst("\nsynthesisTarget=[A-Z_]+\n", "\nsynthesisTarget=INCONNUE\n")));
        }

        @Test
        void uneCleAbsentePrendLaValeurDUnePartieNeuve() throws Exception {
            String text = SaveCodec.write(richState(), 0);
            GameState read = SaveCodec.read(text.replaceFirst("\nexplosions=64\n", "\n").replaceFirst("\nautoHold=0\n", "\n")).state();
            assertEquals(0, read.explosions());
            assertTrue(read.autoHold());
            assertEquals(9, read.tableWeightLevel());      // le reste est bien là
        }

        @Test
        void uneCleInconnueEstIgnoree() throws Exception {
            String text = SaveCodec.write(richState(), 0) + "champDUneAutreVersion=42\nligne sans signe egal\n\n";
            assertEquals(List.of(), differences("partie", richState(), SaveCodec.read(text).state(), false));
        }

        @Test
        void uneStatistiqueInconnueDeLHistoriqueEstIgnoreeEtUneAbsenteResteSansValeur() throws Exception {
            String text = SaveCodec.write(richState(), 0);
            String changed = text.replaceFirst("\nstats\\.history\\.v\\.PRODUCTION=", "\nstats.history.v.DISPARUE=");
            assertFalse(changed.equals(text));
            GameState read = SaveCodec.read(changed).state();
            assertEquals(richState().stats().history().samples().size(), read.stats().history().samples().size());
            for (StatsHistory.Sample sample : read.stats().history().samples()) {
                assertTrue(Double.isNaN(sample.value(StatsHistory.Stat.PRODUCTION)));
            }
        }

        @Test
        void lireDansUnePartieEnCoursLaRemplace() throws Exception {
            GameState target = new GameState();
            target.setStarted(true);
            target.setExplosions(3);
            target.addAchievement("ancien");
            GameStats before = target.stats();
            SaveCodec.Save save = SaveCodec.readInto(target, SaveCodec.export(richState(), 77));
            assertEquals(77, save.savedAt());
            assertEquals(List.of(), differences("partie", richState(), target, false));
            assertFalse(target.achievements().contains("ancien"));
            assertFalse(before == target.stats(), "les statistiques d'avant ne doivent plus servir");
        }

        @Test
        void unTexteIllisibleNeToucheParALaPartieEnCours() {
            GameState target = richState();
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.readInto(target, "n'importe quoi"));
            assertEquals(List.of(), differences("partie", richState(), target, false));
        }

        @Test
        void laSauvegardeDUneLonguePartieResteLegere() {
            GameState state = richState();
            String text = SaveCodec.write(state, 0);
            String exported = SaveCodec.export(state, 0);
            assertTrue(text.length() < 400_000, "fichier de " + text.length() + " caractères");
            assertTrue(exported.length() < 200_000, "export de " + exported.length() + " caractères");
        }
    }

    @Nested
    class PartieReelle {

        private Game play(GameState state, long seed) {
            return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
        }

        /** Joue quelque temps comme un joueur pressé : tout acheter, fusionner dès que possible. */
        private void rush(Game game, double seconds) {
            for (double t = 0; t < seconds; t += 0.5) {
                game.tick(0.5);
                game.buyAllWithParticles();
                if (game.canFuse()) game.fuse();
                for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                    if (game.canBuy(upgrade.id())) game.buy(upgrade.id());
                }
            }
        }

        @Test
        void unePartieJoueeSeRelitChampParChamp() throws Exception {
            Game game = play(new GameState(), 1);
            game.start();
            rush(game, 3000);
            assertTrue(game.stats().fusions() > 0, "la partie d'essai doit avoir fusionné");
            assertTrue(game.stats().history().samples().size() > 50);
            assertEquals(List.of(), differences("partie", game.state(), copy(game.state()), false));
        }

        @Test
        void unePartieRelueContinueCommeLOriginale() throws Exception {
            Game game = play(new GameState(), 1);
            game.start();
            rush(game, 2000);
            Game reloaded = play(copy(game.state()), 1);
            assertEquals(0, reloaded.restored(), "rien d'inconnu dans une sauvegarde de cette version");
            assertEquals(game.productionPerSecond(), reloaded.productionPerSecond());
            assertEquals(game.atomsPerFusion(), reloaded.atomsPerFusion());
            assertEquals(game.generatorCount(), reloaded.generatorCount());
            rush(game, 600);
            rush(reloaded, 600);
            assertEquals(game.state().particles(), reloaded.state().particles());
            assertEquals(game.state().totalAtoms(), reloaded.state().totalAtoms());
            assertEquals(game.stats().fusions(), reloaded.stats().fusions());
            assertEquals(game.state().upgradeLevels(), reloaded.state().upgradeLevels());
            assertEquals(game.state().achievements(), reloaded.state().achievements());
        }

        @Test
        void chargerDansUnePartieOuverteOublieCeQuiEtaitCalculeDAvance() throws Exception {
            Game source = play(new GameState(), 1);
            source.start();
            rush(source, 2000);
            Game open = play(new GameState(), 1);
            open.start();
            rush(open, 50);
            BigNum before = open.productionPerSecond();
            SaveCodec.readInto(open.state(), SaveCodec.export(source.state(), 0));
            open.restored();
            assertFalse(before.equals(open.productionPerSecond()));
            assertEquals(source.productionPerSecond(), open.productionPerSecond());
            assertEquals(source.achievementCount(), open.achievementCount());
            assertEquals(source.achievementParticlesMultiplier(), open.achievementParticlesMultiplier());
            assertEquals(List.of(), open.takeNewAchievements());
        }

        @Test
        void ceQueLeJeuNeConnaitPasEstEcarteAuChargement() throws Exception {
            Game game = play(new GameState(), 1);
            game.start();
            rush(game, 600);
            GameState state = game.state();
            state.setLevel("amelioration_disparue", 3);
            state.addAutomation("automatisme_disparu");
            state.setAutomationEnabled("automatisme_disparu", true);
            state.setElementCount(999, 2);
            state.setDarkLevel("case_disparue", 1);
            state.setActiveChallenge("defi_disparu");
            state.addCompletedChallenge("defi_disparu");
            state.addAchievement("succes_disparu");
            state.addMolecules("XxYy", 5);
            state.addSubstance("XxYy");
            state.setMoleculeAutomated("XxYy", true);
            state.addAssembly("assemblage_disparu");
            state.addBody("astre_disparu");
            state.addSpaceUpgrade("espace_disparu");
            state.setMoleculeFavorite("XxYy", true);
            int known = state.upgradeLevels().size() - 1;

            Game reloaded = play(copy(state), 1);
            assertEquals(15, reloaded.restored());
            GameState kept = reloaded.state();
            assertEquals(known, kept.upgradeLevels().size());
            assertFalse(kept.ownsAutomation("automatisme_disparu"));
            assertEquals(0, kept.elementCount(999));
            assertEquals(0, kept.darkLevelOf("case_disparue"));
            assertEquals(null, kept.activeChallenge());
            assertTrue(kept.completedChallenges().isEmpty());
            assertFalse(kept.achievements().contains("succes_disparu"));
            assertEquals(0, kept.moleculeCount("XxYy"));
            assertTrue(kept.moleculeLog().isEmpty());
            assertFalse(kept.hasSubstance("XxYy"));
            assertFalse(kept.isMoleculeAutomated("XxYy"));
            assertFalse(kept.isMoleculeFavorite("XxYy"));
            assertFalse(kept.hasAssembly("assemblage_disparu"));
            assertFalse(kept.hasBody("astre_disparu"));
            assertFalse(kept.ownsSpaceUpgrade("espace_disparu"));
            // Et le jeu tourne : rien ne cherche plus ce qui a disparu.
            reloaded.tick(60);
            reloaded.activeChallenge();
            reloaded.moleculesCreated();
            reloaded.spacePerSecond();
        }
    }

    @Nested
    class RetourApresUneAbsence {

        private Game started() {
            Game game = new Game();
            game.start();
            return game;
        }

        @Test
        void laPartRejoueeGranditAvecLesActes() {
            Game game = started();
            assertEquals(0.10, game.offlineRate());
            game.state().setExplosions(1);
            assertEquals(0.40, game.offlineRate());
            game.state().setBigBangs(1);
            game.state().setExplosions(0);
            assertEquals(0.70, game.offlineRate());
            game.state().setCosmosLevel(2);
            assertEquals(0.70, game.offlineRate(), "l'amas de galaxies n'est pas encore la fin");
            game.state().setCosmosLevel(3);
            assertEquals(1.0, game.offlineRate());
        }

        @Test
        void lArbreAchetePuisLeBigBangNeFontPasRetomberLaPart() {
            Game game = started();
            // Toute la matière noire dépensée dans l'arbre, puis une explosion à zéro : toujours le deuxième acte.
            game.state().setDarkMatterSpent(BigNum.of(5));
            assertEquals(0.40, game.offlineRate());
        }

        @Test
        void uneAbsenceRejoueSaPartDuTemps() {
            Game game = started();
            double before = game.state().timePlayed();
            Absence absence = new Absence(game, 3600);
            assertEquals(3600, absence.away());
            assertEquals(0.10, absence.rate());
            assertEquals(360, absence.total(), 1e-9);
            assertFalse(absence.isDone());
            absence.advance(100);
            assertEquals(100, absence.done(), 1e-9);
            assertEquals(100.0 / 360, absence.progress(), 1e-9);
            absence.advance(1e9);
            assertTrue(absence.isDone());
            assertEquals(360, absence.done(), 1e-9);
            assertEquals(1, absence.progress());
            assertEquals(before + 360, game.state().timePlayed(), 1e-6);
            // Rien de plus une fois finie.
            absence.advance(50);
            assertEquals(before + 360, game.state().timePlayed(), 1e-6);
        }

        @Test
        void uneAbsenceDonneCeQueLeMemeTempsDeJeuAuraitDonne() {
            Game away = started();
            Game open = started();
            new Absence(away, 5000).advance(1e9);
            for (int i = 0; i < 50; i++) open.tick(10);
            assertEquals(open.state().particles(), away.state().particles());
            assertEquals(open.state().timePlayed(), away.state().timePlayed(), 1e-6);
        }

        @Test
        void uneAbsenceTropCourteNeComptePas() {
            Game game = started();
            Absence absence = new Absence(game, Absence.MIN_SECONDS - 1);
            assertEquals(0, absence.total());
            assertTrue(absence.isDone());
            assertTrue(absence.report().isEmpty());
            assertFalse(new Absence(game, Absence.MIN_SECONDS).isDone());
        }

        @Test
        void uneHorlogeReculeeOuIllisibleNeComptePas() {
            Game game = started();
            assertTrue(new Absence(game, -5000).isDone());
            assertEquals(0, new Absence(game, -5000).away());
            assertTrue(new Absence(game, Double.NaN).isDone());
        }

        @Test
        void rienNEstCompteAuDelaDUneJournee() {
            Game game = started();
            Absence absence = new Absence(game, 10 * 24 * 3600);
            assertEquals(Absence.MAX_SECONDS * 0.10, absence.total(), 1e-9);
            assertEquals(10 * 24 * 3600, absence.report().away());
            assertEquals(Absence.MAX_SECONDS, absence.report().counted());
        }

        @Test
        void unePartiePasCommenceeNeRattrapeRien() {
            Game game = new Game();
            Absence absence = new Absence(game, 7200);
            assertTrue(absence.isDone());
            assertEquals(0, game.state().timePlayed());
        }

        @Test
        void leJoueurPeutPasserLaFin() {
            Game game = started();
            Absence absence = new Absence(game, 36000);
            absence.advance(600);
            absence.skip();
            assertTrue(absence.isDone());
            absence.advance(1e9);
            assertEquals(600, absence.done(), 1e-9);
            assertTrue(absence.report().skipped());
            assertEquals(600, absence.report().played(), 1e-9);
        }

        @Test
        void passerUneAbsenceFinieNEstPasLaPasser() {
            Game game = started();
            Absence absence = new Absence(game, 600);
            absence.advance(1e9);
            absence.skip();
            assertFalse(absence.report().skipped());
        }

        @Test
        void leRapportDitCeQueLAbsenceAAjoute() {
            Game game = started();
            game.tick(500);
            BigNum created = game.stats().particlesCreated();
            Absence absence = new Absence(game, 20000);
            absence.advance(1e9);
            Absence.Report report = absence.report();
            assertFalse(report.isEmpty());
            assertEquals(0.10, report.rate());
            assertEquals(2000, report.played(), 1e-9);
            assertEquals(game.stats().particlesCreated().subtract(created), report.particles());
            assertEquals(0, report.fusions());
            assertTrue(report.space().isZero());
        }

        @Test
        void leRattrapageParTranchesDeTempsReelFinitParToutRejouer() {
            Game game = started();
            Absence absence = new Absence(game, 3600);
            int rounds = 0;
            while (!absence.isDone() && rounds++ < 100_000) absence.advanceFor(1_000_000);
            assertTrue(absence.isDone());
            assertEquals(360, absence.done(), 1e-9);
        }
    }
}
