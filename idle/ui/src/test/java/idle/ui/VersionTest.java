package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/** Version 0.126 : le numéro affiché dans le jeu et celui que déclare Gradle sont le même. Sans affichage JavaFX. */
class VersionTest {

    @Test
    void laVersionDuJeuEstCelleDuBuild() {
        String declared = System.getProperty("idle.version");
        assumeTrue(declared != null, "lancé hors de Gradle : rien à comparer");
        assertEquals(declared, Version.CURRENT, "build.gradle.kts (racine) et Version.java doivent avancer ensemble");
    }
}
