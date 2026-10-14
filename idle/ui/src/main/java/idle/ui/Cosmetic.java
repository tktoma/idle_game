package idle.ui;

/**
 * Les thèmes et les notations à débloquer (version 0.63). Rien ne les annonce : chacun s'ouvre en
 * faisant quelque chose de très précis, qu'un indice sibyllin suggère dans les réglages. Ils ne
 * changent rien aux règles, seulement à l'allure du jeu. Ce qui est débloqué est gardé dans les
 * réglages ({@link Settings}), pas dans la partie : une nouvelle partie le garde.
 *
 * <p>Les conditions sont vérifiées par {@link Cosmetics}.
 */
public enum Cosmetic {
    SEPIA("le thème « Sépia »", "« Le temps passe, même quand on ne touche à rien. »",
            "rester dix minutes sans toucher au clavier ni à la souris, le jeu en marche"),
    PHOSPHOR("le thème « Phosphore »", "« Trente fois de suite, sans en laisser passer un. »",
            "capter à la main trente croisements du collisionneur de suite, sans en manquer un"),
    INFRARED("le thème « Infrarouge »", "« Une heure de fièvre. »",
            "avoir une heure de surchauffe d'avance au collisionneur"),
    NEBULA("le thème « Nébuleuse »", "« La réponse, en matière noire. »",
            "avoir exactement 42 de matière noire en réserve"),
    GOLD("le thème « Or »", "« Tout l'anneau, en succès. »",
            "obtenir tous les succès du collisionneur"),
    ALPHABET("la notation « Alphabet »", "« Des uns partout : quatre en particules, trois en atomes. »",
            "avoir exactement 1 111 particules et 111 atomes au même instant"),
    LOG("la notation « Logarithme »", "« Un gogol. »",
            "avoir 1e100 particules"),
    ROMAN("la notation « Romaine »", "« Une heure d'arrêt, comme une légion au repos. »",
            "laisser le jeu en pause une heure d'affilée");

    private final String label;
    private final String hint;
    private final String condition;

    Cosmetic(String label, String hint, String condition) {
        this.label = label;
        this.hint = hint;
        this.condition = condition;
    }

    /** Ce qu'il ouvre, avec son article : « le thème « Sépia » ». */
    public String label() {
        return label;
    }

    /** L'indice montré tant qu'il est fermé. */
    public String hint() {
        return hint;
    }

    /** Ce qu'il fallait faire, dit en clair une fois débloqué. */
    public String condition() {
        return condition;
    }
}
