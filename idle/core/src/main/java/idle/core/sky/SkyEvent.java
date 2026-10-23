package idle.core.sky;

/**
 * Les petites anomalies du ciel (version 0.127), à côté de la comète : elles traversent l'expansion de temps en
 * temps, après le premier Big Bang, et restent quelques minutes à saisir d'un clic ({@link idle.core.Game#catchSkyEvent()}).
 *
 * <p>Elles sont facultatives : rien ne s'y achète, aucune ne débloque quoi que ce soit, et en manquer une ne coûte
 * rien. Leur récompense est petite et passagère. Leur attente se déduit du temps de jeu, sans toucher au hasard du
 * jeu : les tirages de la synthèse, et les parties des bots, ne dépendent pas d'elles.
 */
public enum SkyEvent {
    /** Particules ×{@link idle.core.Game#WIND_FACTOR} pendant {@link idle.core.Game#WIND_SECONDS} secondes. */
    PARTICLE_WIND("Vent de particules"),
    /** Les sondes en vol avancent d'un coup de {@link idle.core.Game#EXPLORATION_SECONDS} secondes. */
    EXPLORATION_WINDOW("Fenêtre d'exploration");

    private final String label;

    SkyEvent(String label) {
        this.label = label;
    }

    /** Son nom affiché. */
    public String label() {
        return label;
    }
}
