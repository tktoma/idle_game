package idle.core.universe;

import java.util.List;

/**
 * Le guide de transition entre univers (version 0.127) : avant chaque remise à zéro de l'univers, ce qui repart de
 * zéro, ce qui reste, et ce que la transition apporte. Le moteur le dresse ({@link idle.core.Game#universeTransition})
 * à côté du code qui fait la remise à zéro, et un test vérifie chacune de ses promesses : l'interface n'a qu'à le
 * montrer.
 *
 * @param launching vrai pour le lancement de l'univers suivant, faux pour la formation de l'univers
 * @param lost      ce qui repart de zéro, une ligne par chose
 * @param kept      ce qui reste, une ligne par chose
 * @param gained    ce que la transition apporte
 */
public record UniverseTransition(boolean launching, List<String> lost, List<String> kept, List<String> gained) {

    public UniverseTransition {
        lost = List.copyOf(lost);
        kept = List.copyOf(kept);
        gained = List.copyOf(gained);
    }
}
