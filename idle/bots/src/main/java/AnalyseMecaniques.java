import java.util.*;

/**
 * Les mécaniques du jeu que le bot peut cesser de jouer, pour {@link AnalyseArrets} : à partir du jalon,
 * il n'y touche plus. Ce qui est déjà acquis reste (les générateurs du collisionneur continuent de produire, un défi
 * réussi garde sa récompense) ; seuls ses gestes s'arrêtent.
 */
final class AnalyseMecaniques {
    private AnalyseMecaniques() {}

    private static AnalyseArrets.Coupure sans(String id, String nom) {
        return new AnalyseArrets.Coupure(id, nom, "mecaniques", Set.of(), bot -> bot.sansMecaniques.add(id));
    }

    static List<AnalyseArrets.Coupure> toutes() {
        List<AnalyseArrets.Coupure> list = new ArrayList<>();
        list.add(sans("collisionneur", "Le collisionneur : plus d'achats ni de réglages"));
        list.add(sans("sondes", "Les sondes : plus de lancement ni de récolte"));
        list.add(sans("defis", "Les défis de matière noire : plus de défi lancé"));
        list.add(sans("cometes", "Les comètes : plus attrapées"));
        list.add(sans("anomalies", "Les anomalies du ciel : plus attrapées"));
        list.add(sans("impulsions", "Les impulsions d'espace : plus déclenchées"));
        list.add(sans("calme", "Le calme du Big Bang : plus d'attente sans achat quand seules les particules manquent (0.294)"));
        list.add(sans("epreuves", "Les épreuves de l'onglet Skins : plus tentées (0.294)"));
        list.add(sans("rond", "Le rond de croissance de la graine : plus cueilli (0.294)"));
        list.add(sans("rassemblement", "Le rassemblement accéléré : plus de clic tenu sur l'amas ou l'univers (0.299)"));
        list.add(sans("appui_m", "La touche M tenue : un achat « max » par décision au lieu d'un toutes les secondes (sans effet sur un profil qui ne la tient pas, comme le Régulier)"));
        return list;
    }
}
