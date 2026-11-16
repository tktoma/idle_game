package idle.ui.particles;

/**
 * Les paliers de vitesse d'un générateur de particules, du clair au noir. Le premier va de la vitesse de base (trois
 * créations par seconde) à 10 par seconde, le deuxième de 10 à 100 ; ensuite, chaque palier élève la vitesse au carré
 * (10⁴, 10⁸, 10¹⁶…). Le quatorzième et dernier commence à 10⁴⁰⁹⁶ : la vitesse qu'atteint le joueur le plus rapide juste
 * avant de former le premier univers. Au ×10, il aurait fallu plus de quatre mille couleurs.
 *
 * <p>Chaque palier a sa couleur de noyau, son liseré (la lumière des noyaux sombres) et la hauteur du son de la
 * particule récupérée, une note plus grave à chaque palier sur la gamme pentatonique de fa.
 */
public final class SpeedStages {

    private SpeedStages() {
    }

    /** Le nombre de paliers. */
    public static final int COUNT = 14;
    /** L'ordre de grandeur de la vitesse de base : trois créations par seconde. */
    static final double BASE_LOG = Math.log10(3);

    /** Les couleurs des noyaux, du presque blanc au noir. */
    static final String[] COLORS = {"#f4fbff", "#cdeeff", "#a6dbff", "#84bfff", "#8a9cff", "#9a7cff", "#9157f0", "#7a3dcc",
            "#5f2ca3", "#45207c", "#2f1658", "#1d0d38", "#0e061c", "#000000"};
    /** Les liserés : la lumière autour de chaque noyau, qui reste visible quand le noyau devient sombre. */
    static final String[] RIMS = {"#ffffff", "#e6f6ff", "#cde9ff", "#b8d6ff", "#b9c3ff", "#c4b0ff", "#c09aff", "#b086f0",
            "#a275e0", "#9568d0", "#8a5cc4", "#7f52b8", "#7448ac", "#6a3fa0"};
    /**
     * La hauteur du son à chaque palier, en demi-tons autour de l'enregistrement (un fa, 176 Hz) : la pentatonique de fa,
     * du fa aigu au la grave, retenue à l'écoute.
     */
    static final int[] SEMITONES = {12, 9, 7, 4, 2, 0, -3, -5, -8, -10, -12, -15, -17, -20};

    /** Le bas du palier {@code stage}, en ordre de grandeur de la vitesse. */
    static double lowLog(int stage) { return stage == 0 ? BASE_LOG : Math.pow(2, stage - 1); }

    /** Le haut du palier {@code stage} (le bas du suivant) ; le dernier va jusqu'au double de son bas. */
    static double highLog(int stage) { return Math.pow(2, stage); }

    /** Le palier d'une vitesse donnée par son ordre de grandeur (log10 des créations par seconde), de 0 à 13. */
    public static int stage(double speedLog) {
        if (!(speedLog >= 1)) return 0;                               // sous 10 par seconde, ou illisible
        int stage = 1 + (int) Math.floor(Math.log(speedLog) / Math.log(2) + 1e-9);
        return Math.min(COUNT - 1, stage);
    }

    /** L'avancée dans son palier, de 0 (on vient d'y entrer) à 1 (on va passer au suivant). */
    public static double within(double speedLog) {
        int stage = stage(speedLog);
        double low = lowLog(stage), high = highLog(stage);
        if (!(speedLog > low)) return 0;
        return Math.min(1, (speedLog - low) / (high - low));
    }

    /** Le facteur de lecture du son à ce palier : plus grand que 1, plus aigu et plus court ; plus petit, plus grave. */
    public static double pitch(int stage) { return Math.pow(2, SEMITONES[clamp(stage)] / 12.0); }

    /** La couleur du noyau au palier {@code stage}, en {@code #rrggbb}. */
    public static String color(int stage) { return COLORS[clamp(stage)]; }

    /** Le liseré du palier {@code stage}, en {@code #rrggbb}. */
    public static String rim(int stage) { return RIMS[clamp(stage)]; }

    private static int clamp(int stage) { return Math.max(0, Math.min(COUNT - 1, stage)); }
}
