package idle.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Une molécule : une formule faite d'éléments du tableau périodique.
 *
 * <p>Les molécules appartiennent à l'acte du Big Bang. Le joueur les crée
 * ({@link Game#createMolecule(String)}) en y mettant des exemplaires de ses éléments, autant que
 * la formule en demande (deux hydrogènes et un oxygène pour une molécule d'eau), et chaque
 * molécule créée occupe de l'espace ({@link #protons()}). Plus une molécule est complexe ou faite
 * d'éléments lourds, plus elle demande des deux.
 *
 * <p>Certaines molécules donnent quelque chose en retour ({@link Bonus}), autant de fois qu'elles
 * ont été créées : un léger bonus sur une grandeur du jeu, ou un exemplaire de plus au maximum
 * d'un de leurs éléments dans le tableau périodique. Pour l'instant, seules les petites molécules
 * en ont un ; les autres n'en portent pas ({@code bonus} vaut {@code null}).
 *
 * @param id      identifiant stable (sert de clé dans la sauvegarde) : la formule en clair, « H2O »
 * @param name    nom affiché
 * @param formula formule affichée, avec ses indices (« H₂O »)
 * @param recipe  numéro atomique → nombre d'atomes dans une molécule, dans l'ordre de la formule
 * @param kind    le rayon du catalogue où elle est rangée
 * @param bonus   ce que donne chaque molécule créée, ou {@code null} si elle ne donne rien
 */
public record Molecule(String id, String name, String formula, Map<Integer, Integer> recipe, Kind kind, Bonus bonus) {

    /** Les grandeurs qu'une molécule peut augmenter. */
    public enum Stat {
        /** Espace gagné par seconde ({@link Game#spacePerSecond()}). */
        SPACE,
        /** Particules de chaque création. */
        PARTICLES,
        /** Atomes de chaque fusion. */
        ATOMS,
        /** Croissance de la matière noire. */
        DARK_GROWTH
    }

    /**
     * Ce que donne une molécule, pour chaque molécule de sa sorte créée.
     *
     * <p>Interface scellée : pour ajouter une sorte de bonus, on ajoute un record ici, puis le
     * compilateur signale les {@code switch} à compléter.
     */
    public sealed interface Bonus {}

    /**
     * Augmente une grandeur de {@code perMolecule} par molécule créée (0.05 = +5 %). Les bonus de
     * toutes les molécules sur une même grandeur s'additionnent : la grandeur est multipliée par
     * {@code 1 + leur somme} ({@link Game#moleculeBoost(Stat)}).
     */
    public record Boost(Stat stat, double perMolecule) implements Bonus {
        public Boost {
            if (stat == null) throw new IllegalArgumentException("Bonus sans grandeur");
            if (!(perMolecule > 0)) throw new IllegalArgumentException("Bonus par molécule invalide : " + perMolecule);
        }
    }

    /**
     * Relève le plafond d'un élément dans le tableau périodique : un exemplaire de plus au maximum
     * par molécule créée ({@link Game#maxCopiesOf(Element)}). L'élément est toujours l'un de ceux de
     * la formule : c'est en mettant de l'hydrogène dans du dihydrogène qu'on apprend à en garder plus.
     *
     * @param element numéro atomique de l'élément dont le maximum augmente
     */
    public record Uncap(int element) implements Bonus {}

    /**
     * Les rayons du catalogue, du plus simple au plus rare. Seules les petites molécules sont là
     * dès le premier Big Bang : chaque rayon suivant s'ouvre quand l'expansion de la matière a créé
     * assez d'espace ({@link #space()}), dans l'ordre de l'énumération.
     *
     * <p>À une unité d'espace par seconde, le deuxième rayon s'ouvre après une demi-heure, le
     * quatrième après huit heures ; les derniers demandent plusieurs Big Bangs, puisque chacun
     * accélère l'expansion. C'est un premier réglage, pas encore passé par la simulation.
     */
    public enum Kind {
        SIMPLE("Petites molécules", 0),
        ACID("Acides et bases", 2_000),
        SALT("Sels", 10_000),
        MINERAL("Minéraux", 30_000),
        MATERIAL("Matériaux", 80_000),
        ORGANIC("Chimie organique", 200_000),
        LIFE("Vivant", 500_000),
        RARE("Terres rares et éléments lourds", 1_000_000);

        private final String label;
        private final double space;

        Kind(String label, double space) {
            this.label = label;
            this.space = space;
        }

        /** Nom affiché au joueur. */
        public String label() {
            return label;
        }

        /**
         * Le palier d'espace du rayon : l'espace que l'expansion doit avoir créé en tout pour qu'il
         * s'ouvre. C'est l'espace créé qui compte, pas l'espace libre : remplir l'espace de
         * molécules ne referme rien.
         */
        public double space() {
            return space;
        }
    }

    public Molecule {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Molécule sans nom : " + id);
        if (recipe == null || recipe.isEmpty()) throw new IllegalArgumentException("Molécule sans formule : " + id);
        for (Map.Entry<Integer, Integer> atom : recipe.entrySet()) {
            PeriodicTable.element(atom.getKey());     // refuse un numéro atomique inconnu
            if (atom.getValue() <= 0) throw new IllegalArgumentException("Nombre d'atomes invalide dans " + id);
        }
        if (kind == null) throw new IllegalArgumentException("Molécule sans rayon : " + id);
        if (bonus instanceof Uncap uncap) {
            if (!recipe.containsKey(uncap.element())) {
                throw new IllegalArgumentException(id + " ne contient pas l'élément dont elle relève le plafond");
            }
            if (PeriodicTable.element(uncap.element()).category().unique()) {
                throw new IllegalArgumentException(id + " : un élément unique n'a pas de plafond à relever");
            }
        }
        recipe = Collections.unmodifiableMap(new LinkedHashMap<>(recipe));
    }

    /**
     * Une molécule décrite par sa formule en clair : « H2O », « Ca3(PO4)2 », « CH3COOH ». Les
     * parenthèses multiplient ce qu'elles contiennent, et un élément écrit plusieurs fois est
     * additionné. La formule sert d'identifiant ; ses chiffres deviennent des indices à l'affichage.
     *
     * @throws IllegalArgumentException si la formule ne se lit pas ou nomme un élément inconnu
     */
    public static Molecule of(Kind kind, String name, String plainFormula) {
        Map<Integer, Integer> recipe = new LinkedHashMap<>();
        int end = parse(plainFormula, 0, 1, recipe, plainFormula);
        if (end != plainFormula.length()) throw new IllegalArgumentException("Formule illisible : " + plainFormula);
        return new Molecule(plainFormula, name, subscripts(plainFormula), recipe, kind, null);
    }

    /** La même molécule, qui augmente une grandeur de {@code perMolecule} par molécule créée (0.05 = +5 %). */
    public Molecule boosting(Stat stat, double perMolecule) {
        return new Molecule(id, name, formula, recipe, kind, new Boost(stat, perMolecule));
    }

    /**
     * La même molécule, qui relève d'un exemplaire par molécule créée le plafond de l'élément de ce
     * symbole (« H »).
     *
     * @throws IllegalArgumentException si la molécule ne contient pas cet élément
     */
    public Molecule uncapping(String symbol) {
        return new Molecule(id, name, formula, recipe, kind, new Uncap(PeriodicTable.bySymbol(symbol).number()));
    }

    /** Vrai si chaque molécule créée de cette sorte donne quelque chose. */
    public boolean hasBonus() {
        return bonus != null;
    }

    /**
     * Lit un groupe de la formule à partir de {@code start}, jusqu'à sa parenthèse fermante ou la
     * fin du texte, en ajoutant ses atomes multipliés par {@code factor}. Rend la position suivante.
     */
    private static int parse(String text, int start, int factor, Map<Integer, Integer> recipe, String whole) {
        int index = start;
        while (index < text.length()) {
            char letter = text.charAt(index);
            if (letter == ')') return index;
            if (letter == '(') {
                // Le multiplicateur d'une parenthèse est écrit après elle : on la lit à blanc pour le trouver.
                int close = parse(text, index + 1, 0, new LinkedHashMap<>(), whole);
                if (close >= text.length()) throw new IllegalArgumentException("Parenthèse ouverte : " + whole);
                int after = close + 1;
                int digits = after;
                while (digits < text.length() && Character.isDigit(text.charAt(digits))) digits++;
                int count = digits > after ? Integer.parseInt(text.substring(after, digits)) : 1;
                parse(text, index + 1, factor * count, recipe, whole);
                index = digits;
                continue;
            }
            if (!Character.isUpperCase(letter)) throw new IllegalArgumentException("Formule illisible : " + whole);
            int symbolEnd = index + 1;
            while (symbolEnd < text.length() && Character.isLowerCase(text.charAt(symbolEnd))) symbolEnd++;
            int digits = symbolEnd;
            while (digits < text.length() && Character.isDigit(text.charAt(digits))) digits++;
            int count = digits > symbolEnd ? Integer.parseInt(text.substring(symbolEnd, digits)) : 1;
            if (count <= 0) throw new IllegalArgumentException("Nombre d'atomes invalide dans " + whole);
            if (factor > 0) {
                recipe.merge(PeriodicTable.bySymbol(text.substring(index, symbolEnd)).number(), count * factor, Integer::sum);
            }
            index = digits;
        }
        return index;
    }

    /** « Ca3(PO4)2 » → « Ca₃(PO₄)₂ ». */
    private static String subscripts(String plainFormula) {
        StringBuilder pretty = new StringBuilder();
        for (char letter : plainFormula.toCharArray()) {
            pretty.append(Character.isDigit(letter) ? (char) ('₀' + (letter - '0')) : letter);
        }
        return pretty.toString();
    }

    /** Nombre d'atomes d'une molécule, tous éléments confondus : 3 pour l'eau. */
    public int atoms() {
        int atoms = 0;
        for (int count : recipe.values()) atoms += count;
        return atoms;
    }

    /**
     * Nombre de protons d'une molécule : la somme des numéros atomiques de ses atomes, 10 pour
     * l'eau. C'est sa taille : une molécule complexe en a beaucoup parce qu'elle a beaucoup
     * d'atomes, une molécule rare parce que ses éléments sont lourds.
     */
    public int protons() {
        int protons = 0;
        for (Map.Entry<Integer, Integer> atom : recipe.entrySet()) protons += atom.getKey() * atom.getValue();
        return protons;
    }
}
