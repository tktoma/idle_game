package idle.core.collider;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Les hadrons (version 0.149) : des quarks liés par les gluons. D'abord les deux nucléons, le proton (u u d) et le
 * neutron (u d d) ; puis, avec la bulle du cœur de l'arbre-monde, les particules exotiques : mésons (un quark et un
 * antiquark) et baryons étranges. Un antiquark coûte un quark de sa saveur : le collisionneur ne fait pas la différence.
 *
 * <p>Version 0.151 : chaque sorte renforce une grandeur, ou fait baisser un prix, d'autant plus qu'on en a
 * ({@link #bonus()}, ×(1 + 0,25 × log₁₀(1 + n))) ; et chaque quark lié compte un gluon, pour les paliers de gluons
 * ({@link GluonMilestone}).
 */
public enum Hadron {
    PROTON("Proton", "u u d", false, "Le noyau de l'hydrogène : deux quarks haut, un quark bas.", 2, 1, 0, 0, 0),
    NEUTRON("Neutron", "u d d", false, "Son jumeau sans charge : un quark haut, deux quarks bas.", 1, 2, 0, 0, 0),
    PION("Pion", "u d̄", true, "Le plus léger des mésons : il colle les nucléons entre eux.", 1, 1, 0, 0, 0),
    KAON("Kaon", "u s̄", true, "Le premier méson étrange.", 1, 0, 1, 0, 0),
    LAMBDA("Lambda", "u d s", true, "Un baryon étrange : un proton dont un quark haut serait devenu étrange.", 1, 1, 1, 0, 0),
    OMEGA("Oméga", "s s s", true, "Trois quarks étranges : il a été prédit avant d'être vu.", 0, 0, 3, 0, 0),
    D_MESON("Méson D", "c ū", true, "Le méson charmé le plus léger.", 1, 0, 0, 1, 0),
    J_PSI("J/ψ", "c c̄", true, "Le charme et son anti-charme : sa découverte a fait accepter les quarks.", 0, 0, 0, 2, 0),
    B_MESON("Méson B", "u b̄", true, "Le méson beau : il se désintègre autrement que son anti-méson.", 1, 0, 0, 0, 1),
    UPSILON("Upsilon", "b b̄", true, "Le quark beau et son antiquark.", 0, 0, 0, 0, 2);

    private final String label;
    private final String formula;
    private final boolean exotic;
    private final String detail;
    private final Map<Quark, Integer> recipe;

    Hadron(String label, String formula, boolean exotic, String detail, int up, int down, int strange, int charm, int bottom) {
        this.label = label;
        this.formula = formula;
        this.exotic = exotic;
        this.detail = detail;
        EnumMap<Quark, Integer> quarks = new EnumMap<>(Quark.class);
        if (up > 0) quarks.put(Quark.UP, up);
        if (down > 0) quarks.put(Quark.DOWN, down);
        if (strange > 0) quarks.put(Quark.STRANGE, strange);
        if (charm > 0) quarks.put(Quark.CHARM, charm);
        if (bottom > 0) quarks.put(Quark.BOTTOM, bottom);
        this.recipe = Collections.unmodifiableMap(quarks);
    }

    public String label() {
        return label;
    }

    /** Ses quarks, comme on les écrit : « u u d », « c c̄ » (la barre : un antiquark). */
    public String formula() {
        return formula;
    }

    /** Vrai pour les particules exotiques, que la bulle du cœur de l'arbre-monde ouvre. */
    public boolean isExotic() {
        return exotic;
    }

    /** Une phrase sur lui. */
    public String detail() {
        return detail;
    }

    /** Ce qu'il renforce (version 0.151) : « atomes par fusion », « prix des améliorations du collisionneur »… */
    public String bonus() {
        return switch (this) {
            case PROTON -> "atomes par fusion";
            case NEUTRON -> "matière noire par explosion";
            case PION -> "énergie du collisionneur";
            case KAON -> "espace par seconde";
            case LAMBDA -> "particules par création";
            case OMEGA -> "éclats des dimensions";
            case D_MESON -> "énergie du vide";
            case J_PSI -> "prix des améliorations du collisionneur";
            case B_MESON -> "prix des générateurs";
            case UPSILON -> "prix des générateurs du collisionneur";
        };
    }

    /** Vrai pour ceux qui font baisser un prix au lieu de multiplier une grandeur. */
    public boolean lowersCost() {
        return this == J_PSI || this == B_MESON || this == UPSILON;
    }

    /** Les quarks liés dans un hadron : autant de gluons. */
    public int quarkCount() {
        int count = 0;
        for (int need : recipe.values()) count += need;
        return count;
    }

    /** Les quarks qu'il consomme, par saveur ; un antiquark compte pour un quark de sa saveur. */
    public Map<Quark, Integer> recipe() {
        return recipe;
    }
}
