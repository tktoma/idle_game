package idle.ui;

/**
 * Les thèmes et les notations à débloquer (version 0.063 ; treize de plus en 0.200). Rien ne les annonce : chacun s'ouvre en
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
            "laisser le jeu en pause une heure d'affilée"),
    // Version 0.200 : sept thèmes et six notations de plus (SKINS.md, section 8) ; le thème « Daltonien » est libre.
    INK("le thème « Encre »", "« Mille fois deux qui n'en font qu'un. »",
            "réussir mille fusions"),
    CYANOTYPE("le thème « Cyanotype »", "« Le bleu vient après la rouille. »",
            "réussir le défi « Rouille »"),
    OLD_FILM("le thème « Vieux film »", "« Une séance de minuit : le grand boum pendant que la ville dort. »",
            "faire un Big Bang entre minuit et six heures du matin"),
    THERMAL("le thème « Thermique »", "« Trois soleils, chacun avec toute sa cour. »",
            "avoir trois systèmes solaires complets"),
    MOON("le thème « Lune »", "« Dix visiteuses à queue, attrapées au passage. »",
            "attraper dix comètes"),
    FOREST("le thème « Forêt »", "« Quand les systèmes se prennent dans une spirale. »",
            "former la galaxie"),
    BLAZE("le thème « Brasier »", "« Cent fois tout faire sauter. »",
            "faire cent explosions"),
    BINARY("la notation « Binaire »", "« Deux puissance dix, en particules, tout juste. »",
            "avoir exactement 1 024 particules"),
    HEX("la notation « Hexadécimale »", "« FF, en matière noire. »",
            "avoir exactement 255 de matière noire en réserve"),
    JAPANESE("la notation « Chiffres japonais »", "« 万 : dix mille molécules. »",
            "créer dix mille molécules"),
    MORSE("la notation « Morse »", "« Cent télégrammes reçus d'une traite. »",
            "recevoir cent notifications sans fermer le jeu"),
    SI("la notation « Préfixes SI »", "« Un quetta : trente zéros de particules. »",
            "avoir 1e30 particules"),
    HEARTBEATS("la notation « Battements de cœur »", "« Quand le chrono n'a plus de place pour un battement. »",
            "remplir le chrono à ras bord");

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
