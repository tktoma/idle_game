package idle.ui;

import javafx.application.Application;

/**
 * Point d'entrée de l'interface pour le module {@code app}.
 *
 * <p>Le {@code main} du jeu ne doit pas être une classe qui hérite de
 * {@link Application}, sinon Java refuse de démarrer avec l'erreur
 * « JavaFX runtime components are missing ». On passe donc par cette classe.
 */
public final class UiLauncher {

    public static void launch(String[] args) {
        Application.launch(GameApp.class, args);
    }

    private UiLauncher() {}
}