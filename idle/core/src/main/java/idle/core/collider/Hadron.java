package idle.core.collider;

import java.util.Map;

/**
 * Les hadrons que l'on forme avec les quarks du collisionneur. Chacun donne un bonus permanent à
 * la partie principale : ni l'explosion ni le Big Bang ne les reprennent.
 */
public enum Hadron {
    PROTON("Proton", Map.of(Quark.UP, 2, Quark.DOWN, 1), "Particules +50 % par proton"),
    NEUTRON("Neutron", Map.of(Quark.UP, 1, Quark.DOWN, 2), "Atomes par fusion +5 % par neutron"),
    KAON("Kaon", Map.of(Quark.UP, 1, Quark.STRANGE, 1), "Croissance de la matière noire +10 % par kaon"),
    LAMBDA("Lambda", Map.of(Quark.UP, 1, Quark.DOWN, 1, Quark.STRANGE, 1), "Croissance de la matière noire +25 % par lambda"),
    JPSI("J/psi", Map.of(Quark.CHARM, 2), "Espace par seconde +20 % par J/psi"),
    UPSILON("Upsilon", Map.of(Quark.BOTTOM, 2), "Ouvre l'énergie extrême ; le cœur de galaxie en demande un");

    private final String label;
    private final Map<Quark, Integer> recipe;
    private final String bonus;

    Hadron(String label, Map<Quark, Integer> recipe, String bonus) {
        this.label = label;
        this.recipe = recipe;
        this.bonus = bonus;
    }

    public String label() {
        return label;
    }

    /** Les quarks qu'il faut pour en former un. */
    public Map<Quark, Integer> recipe() {
        return recipe;
    }

    /** Ce qu'il apporte, en une phrase. */
    public String bonus() {
        return bonus;
    }
}
