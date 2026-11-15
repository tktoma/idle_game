package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.upgrades.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveCodec;
import idle.core.universe.Skin;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Les systèmes du jeu pris ensemble, et non plus un par un. Sept systèmes qui se posent sur une partie
 * (skins, galaxie, arbre de matière noire, univers formés, bulle du jeune arbre, améliorations en atomes, Big Bangs) se
 * combinent de toutes les façons possibles, 128 parties. Pour chacune, ce que chaque système vérifie seul doit encore tenir :
 *
 * <ul>
 *   <li>la décomposition des multiplicateurs redonne chaque grandeur, facteur par facteur ;</li>
 *   <li>une sauvegarde relue redonne les mêmes grandeurs, le même plafond de générateurs, le même bonus des skins ;</li>
 *   <li>l'ordre dans lequel les systèmes sont posés ne change rien à la sauvegarde ;</li>
 *   <li>un système qui aide n'en fait jamais perdre un autre (les grandeurs ne baissent pas quand on en ajoute un) ;</li>
 *   <li>couper l'effet de la bulle (outil des mesures) revient exactement à ne pas l'avoir, quels que soient les autres ;</li>
 *   <li>deux parties combinées pareil jouent pareil, à l'octet près, pendant une minute de jeu ;</li>
 *   <li>un Big Bang garde ce qui doit durer (skins, arbre, univers, galaxie, bulle), quoi qu'on ait combiné.</li>
 * </ul>
 */
class InteractionSystemesTest {
    /** Les systèmes combinés : chacun se pose sur une partie au premier univers formé, sans rien payer. */
    enum Systeme {
        SKINS {
            @Override
            void poser(Game game) {
                game.state().setSkinsOpen(true);
                int poses = 0;
                for (Skin skin : Skin.values()) {
                    if (skin.isFree() || poses >= SKINS_POSES) continue;
                    game.state().unlockSkin(skin.id());
                    poses++;
                }
            }
        },
        GALAXIE {
            @Override
            void poser(Game game) { game.state().setCosmosLevel(1); }
        },
        MATIERE_NOIRE {
            @Override
            void poser(Game game) {
                game.state().setDarkLevel(Game.KEY_DARK_UPGRADE, 1);
                game.state().setDarkLevel("dark_density", 40);
                game.state().setDarkLevel("dark_singularity", 2);
                game.state().setDarkLevel("dark_shop_cadence", 3);
            }
        },
        UNIVERS {
            @Override
            void poser(Game game) {
                assertTrue(game.testAddUniverse());
                assertTrue(game.testAddUniverse());
            }
        },
        BULLE {
            @Override
            void poser(Game game) { game.state().setTreeBubbleLevel(GameUniverse.TreeBubble.YOUNG_RIGHT.ordinal(), 1); }
        },
        ATOMES {
            @Override
            void poser(Game game) {
                game.state().setLevel("atom_yield", 5);
                game.state().setLevel("atom_boost", 3);
                game.state().setLevel("coupling", 50);
            }
        },
        BIG_BANGS {
            @Override
            void poser(Game game) { GameTestSupport.bangs(game, 5); }
        };

        abstract void poser(Game game);
    }

    /** Les skins posés par le système des skins : assez pour peser (×2,2), pas tous. */
    static final int SKINS_POSES = 60;

    /** Toutes les combinaisons des sept systèmes, de l'ensemble vide à l'ensemble complet. */
    static Stream<Set<Systeme>> combinaisons() {
        List<Set<Systeme>> all = new ArrayList<>();
        Systeme[] systemes = Systeme.values();
        for (int mask = 0; mask < 1 << systemes.length; mask++) all.add(of(mask));
        return all.stream();
    }

    private static Set<Systeme> of(int mask) {
        Set<Systeme> set = EnumSet.noneOf(Systeme.class);
        for (Systeme systeme : Systeme.values()) if ((mask & 1 << systeme.ordinal()) != 0) set.add(systeme);
        return set;
    }

    /** La partie de départ, puis les systèmes posés dans cet ordre. */
    static Game partie(List<Systeme> ordre) {
        Game game = GameTestSupport.universe(1);
        for (Systeme systeme : ordre) systeme.poser(game);
        return game;
    }

    static Game partie(Set<Systeme> systemes) { return partie(new ArrayList<>(systemes)); }

    /** Une partie rechargée depuis sa sauvegarde, avec le même hasard. */
    static Game relue(Game game) {
        return new Game(GameTestSupport.reread(game.state()), Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(42));
    }

    static String sauvegarde(Game game) {
        try {
            return SaveCodec.write(game.state(), 0);
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Les grandeurs présentes dans la partie, en ordres de grandeur. */
    static Map<Multipliers.Quantity, Double> grandeurs(Game game) {
        Map<Multipliers.Quantity, Double> out = new LinkedHashMap<>();
        for (Multipliers.Quantity quantity : Multipliers.Quantity.values()) {
            if (Multipliers.isAvailable(game, quantity)) out.put(quantity, Multipliers.total(game, quantity));
        }
        return out;
    }

    private static double tolerance(double value) { return 1e-6 * Math.max(1, Math.abs(value)); }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void laDecompositionRedonneChaqueGrandeur(Set<Systeme> systemes) {
        Game game = partie(systemes);
        for (Map.Entry<Multipliers.Quantity, Double> entry : grandeurs(game).entrySet()) {
            double sum = 0;
            for (Multipliers.Factor factor : Multipliers.of(game, entry.getKey())) sum += factor.log10();
            assertEquals(entry.getValue(), sum, tolerance(entry.getValue()), systemes + " : " + entry.getKey());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void uneSauvegardeRelueRedonneLaMemePartie(Set<Systeme> systemes) {
        Game game = partie(systemes);
        Game back = relue(game);
        Map<Multipliers.Quantity, Double> avant = grandeurs(game), apres = grandeurs(back);
        assertEquals(avant.keySet(), apres.keySet(), systemes + " : les mêmes grandeurs");
        for (Multipliers.Quantity quantity : avant.keySet()) {
            assertEquals(avant.get(quantity), apres.get(quantity), tolerance(avant.get(quantity)), systemes + " : " + quantity);
        }
        assertEquals(game.maxGeneratorCount(), back.maxGeneratorCount(), systemes + " : plafond de générateurs");
        assertEquals(game.skinBoost(), back.skinBoost(), 1e-12, systemes + " : bonus des skins");
        assertEquals(game.universes(), back.universes(), systemes + " : univers");
        assertEquals(sauvegarde(game), sauvegarde(back), systemes + " : la sauvegarde se réécrit à l'identique");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void lOrdreDesSystemesNeChangeRien(Set<Systeme> systemes) {
        List<Systeme> ordre = new ArrayList<>(systemes);
        String reference = sauvegarde(partie(ordre));
        Random random = new Random(201 + ordre.size());
        for (int essai = 0; essai < 3; essai++) {
            Collections.shuffle(ordre, random);
            assertEquals(reference, sauvegarde(partie(ordre)), "ordre " + ordre);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void unSystemeDePlusNeFaitRienPerdre(Set<Systeme> systemes) {
        Map<Multipliers.Quantity, Double> base = grandeurs(partie(systemes));
        for (Systeme ajout : Systeme.values()) {
            if (systemes.contains(ajout)) continue;
            Set<Systeme> plus = EnumSet.copyOf(systemes.isEmpty() ? EnumSet.of(ajout) : systemes);
            plus.add(ajout);
            Map<Multipliers.Quantity, Double> avec = grandeurs(partie(plus));
            for (Multipliers.Quantity quantity : base.keySet()) {
                assertTrue(avec.containsKey(quantity), systemes + " + " + ajout + " : " + quantity + " ne disparaît pas");
                assertTrue(avec.get(quantity) >= base.get(quantity) - tolerance(base.get(quantity)),
                        systemes + " + " + ajout + " : " + quantity + " passe de " + base.get(quantity) + " à " + avec.get(quantity));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void couperLaBulleRevientANePasLAvoir(Set<Systeme> systemes) {
        if (!systemes.contains(Systeme.BULLE)) return;
        Set<Systeme> sans = EnumSet.copyOf(systemes);
        sans.remove(Systeme.BULLE);
        Game coupee = partie(systemes);
        coupee.state().testBlock("bulle:YOUNG_RIGHT");
        Game jamais = partie(sans);
        assertEquals(jamais.maxGeneratorCount(), coupee.maxGeneratorCount(), systemes + " : le plafond revient");
        Map<Multipliers.Quantity, Double> a = grandeurs(jamais), b = grandeurs(coupee);
        assertEquals(a.keySet(), b.keySet());
        for (Multipliers.Quantity quantity : a.keySet()) assertEquals(a.get(quantity), b.get(quantity), tolerance(a.get(quantity)), systemes + " : " + quantity);
        assertTrue(coupee.isTreeBubbleBought(GameUniverse.TreeBubble.YOUNG_RIGHT), "la bulle reste achetée");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void deuxPartiesCombineesPareilJouentPareil(Set<Systeme> systemes) {
        // Les combinaisons d'un nombre pair de systèmes suffisent (64 sur 128) : la minute de jeu coûte plus que le reste.
        if (systemes.size() % 2 == 1) return;
        Game a = partie(systemes), b = partie(systemes);
        for (int seconde = 0; seconde < 60; seconde++) {
            a.tick(1);
            b.tick(1);
        }
        assertEquals(sauvegarde(a), sauvegarde(b), systemes + " : même jeu, même partie");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaisons")
    void unBigBangGardeCeQuiDoitDurer(Set<Systeme> systemes) {
        Game game = partie(systemes);
        int skins = game.skinsCounted(), univers = game.universes(), noyaux = game.darkLevelOf(Game.KEY_DARK_UPGRADE);
        int cosmos = game.cosmosFormed(), bigBangs = game.bigBangs();
        boolean bulle = game.isTreeBubbleBought(GameUniverse.TreeBubble.YOUNG_RIGHT);
        // L'arbre ne reste qu'une fois le palier de Big Bang qui le garde atteint : sinon il repart de zéro, comme la matière noire.
        GameTestSupport.readyForBigBang(game);
        if (!game.bigBangKeepsDarkTree()) noyaux = 0;
        assertTrue(game.bigBang(), systemes + " : le Big Bang part");
        assertEquals(bigBangs + 1, game.bigBangs(), systemes + " : un Big Bang de plus");
        assertEquals(skins, game.skinsCounted(), systemes + " : les skins restent");
        assertEquals(univers, game.universes(), systemes + " : les univers restent");
        assertEquals(noyaux, game.darkLevelOf(Game.KEY_DARK_UPGRADE), systemes + " : l'arbre reste");
        assertEquals(cosmos, game.cosmosFormed(), systemes + " : la galaxie reste");
        assertEquals(bulle, game.isTreeBubbleBought(GameUniverse.TreeBubble.YOUNG_RIGHT), systemes + " : la bulle reste");
        assertEquals(0, game.state().explosions(), systemes + " : les explosions repartent de zéro");
    }
}
