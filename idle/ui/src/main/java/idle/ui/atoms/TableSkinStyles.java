package idle.ui.atoms;

import idle.core.universe.Skin;

/**
 * Les styles CSS des cases du tableau périodique sous un skin (version 0.089 pour « Plomb » et « Or »,
 * 0.191 pour les six autres), à côté de {@link PeriodicTablePage} pour la garder lisible. Chaque
 * style dit la police, le fond, la bordure et la couleur du texte d'une case selon qu'elle est pas
 * encore obtenue, possédée ou au maximum, {@code family} étant la couleur de sa famille (« #f4a6a6 »).
 * <ul>
 * <li>« Plomb » : des plaques de métal brut, carrées, au relief marqué, la famille en liseré du haut et la
 * lettre gravée en caractères de machine ;</li>
 * <li>« Or » : des lingots dorés, arrondis, la lettre embossée en brun ; pas encore obtenu, un moule vide ;</li>
 * <li>« Bois gravé » : des planchettes brunes au veinage léger, les lettres brûlées sombres, les possédées
 * vernies et liserées de la famille ;</li>
 * <li>« Cristal » : des facettes translucides bleutées teintées de la famille, un reflet blanc, les cases au
 * maximum qui scintillent ;</li>
 * <li>« Rouille » : du métal piqué orangé-brun aux bords écaillés, poli quand il est possédé, chromé au
 * maximum ;</li>
 * <li>« Néon » : des cases noires au trait de néon de la famille, texte lumineux, éteint tant que la case
 * n'est pas obtenue ;</li>
 * <li>« Carte au trésor » : un vélin sombre (version 0.203 : parchemin jaune avant), les cases obtenues en îles à l'encre d'or, les autres en
 * pointillés, une croix rouge sur les trésors déterrés (au maximum) et sur les îles de la famille visée ;</li>
 * <li>« Minuscule » : rien que les lettres, sans fond ni bordure, dans la couleur de la famille.</li>
 * </ul>
 */
final class TableSkinStyles {

    private static final String COMMON = "-fx-font-weight: bold; -fx-cursor: hand;";

    private TableSkinStyles() {
    }

    /** Vrai si ce skin réduit les cases de moitié : « Minuscule ». */
    static boolean compact(Skin skin) {
        return skin == Skin.TABLE_TINY;
    }

    /** Le style du fond du tableau entier sous ce skin, vide pour ceux qui n'en changent pas. */
    static String tableStyle(Skin skin) {
        if (skin == Skin.TABLE_NEON) return "-fx-background-color: #050508; -fx-background-radius: 8; -fx-padding: 10;";
        if (skin == Skin.TABLE_MAP) {
            return "-fx-background-color: #191510; -fx-background-radius: 6; -fx-padding: 12;"
                    + " -fx-border-color: #6a5230; -fx-border-width: 2; -fx-border-radius: 6; -fx-border-style: solid;";
        }
        return "";
    }

    /**
     * Le style d'une case.
     *
     * @param owned  vrai si le joueur en a au moins un exemplaire
     * @param maxed  vrai s'il en a le maximum
     * @param spot   vrai si la case est à marquer d'une croix bien qu'elle ne soit pas obtenue : sur la carte au
     *               trésor, une île de la famille que la synthèse ciblée vise
     */
    static String cellStyle(Skin skin, String family, boolean owned, boolean maxed, boolean spot) {
        return switch (skin) {
            case TABLE_LEAD -> lead(family, owned, maxed);
            case TABLE_WOOD -> wood(family, owned, maxed);
            case TABLE_CRYSTAL -> crystal(family, owned, maxed);
            case TABLE_RUST -> rust(family, owned, maxed);
            case TABLE_NEON -> neon(family, owned, maxed);
            case TABLE_MAP -> map(owned, maxed, spot);
            case TABLE_TINY -> tiny(family, owned, maxed);
            default -> gold(family, owned, maxed);
        };
    }

    private static String lead(String family, boolean owned, boolean maxed) {
        String face = maxed ? "linear-gradient(to bottom, #8a919a, #4a5058)" : owned ? "linear-gradient(to bottom, #5d636b, #33373d)"
                : "linear-gradient(to bottom, #2a2d32, #1c1e22)";
        return "-fx-font-family: 'Courier New', monospace; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0;"
                + " -fx-border-radius: 0; -fx-background-color: " + face + "; -fx-text-fill: " + (owned ? "#e6e9ec" : "#6d737b") + ";"
                + " -fx-border-color: " + family + (owned ? "" : "66") + " #15181c #0c0e10 #15181c; -fx-border-width: 3 1 3 1;";
    }

    private static String gold(String family, boolean owned, boolean maxed) {
        String face = maxed ? "linear-gradient(to bottom, #fff6c4, #f0c24c 50%, #b07a12)"
                : owned ? "linear-gradient(to bottom, #f5dc8a, #c99a2e 55%, #8a5f10)" : "#1f1808";
        return "-fx-font-family: Georgia, serif; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10 10 4 4;"
                + " -fx-border-radius: 10 10 4 4; -fx-background-color: " + face + "; -fx-text-fill: " + (owned ? "#4a2f05" : "#8a6a2a") + ";"
                + " -fx-border-color: " + (owned ? "#fff3c0 #7a5210 " + family + " #7a5210" : "#6b5220") + ";"
                + " -fx-border-width: " + (owned ? "1 1 3 1" : "1") + ";" + (owned ? "" : " -fx-border-style: dashed;");
    }

    private static String wood(String family, boolean owned, boolean maxed) {
        // Le veinage : de fines rayures horizontales qui se répètent, plus claires sur le bois verni.
        String grain = maxed ? "linear-gradient(from 0% 0% to 0% 14%, repeat, #b8884e 0%, #a0703c 45%, #b8884e 100%)"
                : owned ? "linear-gradient(from 0% 0% to 0% 14%, repeat, #8a6440 0%, #76522f 45%, #8a6440 100%)"
                : "linear-gradient(from 0% 0% to 0% 14%, repeat, #4a3320 0%, #3e2a18 45%, #4a3320 100%)";
        return "-fx-font-family: Georgia, serif; " + COMMON + " -fx-background-radius: 2; -fx-border-radius: 2;"
                + " -fx-background-color: " + grain + "; -fx-text-fill: " + (owned ? "#2a1a0a" : "#1a1008aa") + ";"
                + " -fx-border-color: " + (maxed ? family + " " + family + " #3a2410 " + family : owned ? "#5a3e22 #5a3e22 " + family + " #5a3e22" : "#2e2014") + ";"
                + " -fx-border-width: " + (owned ? "1 1 3 1" : "1") + ";";
    }

    private static String crystal(String family, boolean owned, boolean maxed) {
        // Deux facettes séparées par une arête claire, le reflet blanc en haut à gauche.
        String face = owned
                ? "linear-gradient(to bottom right, #ffffff" + (maxed ? "cc" : "88") + " 0%, " + family + (maxed ? "cc" : "77") + " 45%, "
                        + family + (maxed ? "ee" : "aa") + " 50%, " + family + (maxed ? "99" : "55") + " 100%)"
                : "linear-gradient(to bottom right, #9fd0ff33 0%, #9fd0ff11 50%, #9fd0ff22 100%)";
        return "-fx-font-family: 'Segoe UI', sans-serif; " + COMMON + " -fx-background-radius: 3; -fx-border-radius: 3;"
                + " -fx-background-color: " + face + "; -fx-text-fill: " + (owned ? "#ffffff" : "#9fd0ff77") + ";"
                + " -fx-border-color: " + (owned ? "#ffffffcc" : "#9fd0ff44") + ";"
                + (maxed ? " -fx-effect: dropshadow(gaussian, #ffffff, 8, 0.5, 0, 0);" : owned ? " -fx-effect: dropshadow(gaussian, " + family + ", 5, 0.3, 0, 0);" : "");
    }

    private static String rust(String family, boolean owned, boolean maxed) {
        String face = maxed ? "linear-gradient(to bottom, #e8ecf0, #9aa2aa 50%, #d0d4d8)"
                : owned ? "radial-gradient(center 30% 30%, radius 90%, #a8785a, #8a5a3a 55%, " + family + "66)"
                : "radial-gradient(center 35% 30%, radius 90%, #8a5a3a, #6e4a3a 50%, #3a2418)";
        return "-fx-font-family: 'Courier New', monospace; " + COMMON + " -fx-background-radius: 1; -fx-border-radius: 1;"
                + " -fx-background-color: " + face + "; -fx-text-fill: " + (maxed ? "#2a2a30" : owned ? "#ffe9c2" : "#d8a070aa") + ";"
                // Les bords écaillés : une bordure par segments irréguliers.
                + " -fx-border-color: " + (maxed ? "#5a5e66" : "#3a2418") + "; -fx-border-width: 2; -fx-border-style: segments(4, 2, 7, 3, 2, 2);";
    }

    private static String neon(String family, boolean owned, boolean maxed) {
        return "-fx-font-family: 'Segoe UI', sans-serif; " + COMMON + " -fx-background-radius: 5; -fx-border-radius: 5;"
                + " -fx-background-color: " + (maxed ? family + "33" : "#0a0a10") + "; -fx-text-fill: " + (maxed ? "#ffffff" : owned ? family : family + "55") + ";"
                + " -fx-border-color: " + family + (owned ? "" : "44") + "; -fx-border-width: " + (owned ? "1.5" : "1") + ";"
                + (owned ? " -fx-effect: dropshadow(gaussian, " + family + ", " + (maxed ? 14 : 8) + ", " + (maxed ? 0.6 : 0.45) + ", 0, 0);" : "");
    }

    private static String map(boolean owned, boolean maxed, boolean spot) {
        if (!owned) {
            // Une île pas encore découverte : un contour en pointillés, rouge si la synthèse ciblée la vise.
            return "-fx-font-family: Georgia, serif; -fx-font-style: italic; " + COMMON + " -fx-background-radius: 8; -fx-border-radius: 8;"
                    + " -fx-background-color: transparent; -fx-text-fill: " + (spot ? "#e06a5a" : "#c9a46888") + ";"
                    + " -fx-border-color: " + (spot ? "#e06a5a" : "#7a6238") + "; -fx-border-width: 1.5; -fx-border-style: dotted;";
        }
        return "-fx-font-family: Georgia, serif; " + COMMON + " -fx-background-radius: 8 12 9 11; -fx-border-radius: 8 12 9 11;"
                + " -fx-background-color: " + (maxed ? "#3d4424" : "#2e2618") + "; -fx-text-fill: " + (maxed ? "#ff8a78" : "#e6cf9c") + ";"
                + " -fx-border-color: #c9a468; -fx-border-width: " + (maxed ? 2 : 1.5) + ";";
    }

    private static String tiny(String family, boolean owned, boolean maxed) {
        return COMMON + " -fx-background-color: transparent; -fx-text-fill: " + family + (owned ? "" : "55") + ";"
                + (maxed ? " -fx-underline: true;" : "");
    }
}
