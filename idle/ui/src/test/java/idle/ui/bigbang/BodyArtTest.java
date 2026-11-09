package idle.ui.bigbang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import idle.core.universe.Skin;
import idle.ui.FxTestSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.183 : les couches de détail des astres ({@link BodyArt#DETAIL_FROM}) n'agrandissent
 * pas leur emprise, chaque astre du catalogue se dessine à toutes les tailles, et l'espace vu de
 * très loin, quand tout est devenu du gaz, se dessine avec ses marques nettes par-dessus.
 *
 * <p>Version 0.186 : les trois skins des astres ({@link Skin#BODIES}) dessinent chaque astre à toutes
 * les tailles sans exception, ne changent pas l'emprise (qui ne dépend que de l'astre), et sans skin
 * ({@code null}) le dessin passe par le même chemin que sans argument.
 */
class BodyArtTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** L'emprise attendue d'un astre, en rayons : celle d'avant les couches de détail, qui ne doit pas bouger. */
    private static double[] expected(Body body) {
        // span, reachUp, reachDown, reachLeft, reachRight
        return switch (body.tier()) {
            case RUBBLE -> new double[] {1.2, 1.2, 1.2, 1.2, 1.2};
            case COMET -> new double[] {3.49, 3.4804, 1.7, 1.7, 4.4545};
            case ASTEROID -> new double[] {1, 1, 1, 1, 1};
            case MOON, PLANET -> switch (body.look()) {
                case RINGED -> new double[] {1.75, 1, 1, 1.75, 1.75};
                case OCEAN -> new double[] {1.1, 1, 1, 1, 1};
                case HAZY -> new double[] {1.12, 1, 1, 1, 1};
                case BANDED -> new double[] {1.08, 1, 1, 1, 1};
                case MOLTEN -> new double[] {1.06, 1, 1, 1, 1};
                default -> new double[] {1, 1, 1, 1, 1};
            };
            case STAR, REMNANT -> body.look() == Body.Look.BEAMING
                    ? new double[] {2.8, 2.4517, 2.4517, 1.5, 1.5}
                    : new double[] {1.45, 1.45, 1.45, 1.45, 1.45};
            case BLACK_HOLE -> new double[] {2.35, 1.32, 1.32, 2.35, 2.35};
            case CORE -> new double[] {2.35, 2.3, 2.3, 2.35, 2.35};
            case EXOTIC -> switch (body.look()) {
                case BEAMING -> new double[] {2.8, 2.4517, 2.4517, 1.5, 1.5};
                case NEBULA -> new double[] {1.35, 1.35, 1.35, 1.35, 1.35};
                default -> new double[] {2.35, 2.3, 2.3, 2.35, 2.35};
            };
        };
    }

    @Test
    void lEmpriseDeChaqueAstreNeChangePas() {
        for (Body body : Bodies.DEFAULT) {
            double[] want = expected(body);
            assertEquals(want[0], BodyArt.span(body), 0.01, body.id() + " span");
            assertEquals(want[1], BodyArt.reachUp(body), 0.001, body.id() + " reachUp");
            assertEquals(want[2], BodyArt.reachDown(body), 0.001, body.id() + " reachDown");
            assertEquals(want[3], BodyArt.reachLeft(body), 0.001, body.id() + " reachLeft");
            assertEquals(want[4], BodyArt.reachRight(body), 0.001, body.id() + " reachRight");
            // Le centre du cercle qui contient tout n'est décalé que pour une comète, le long de ses queues.
            if (body.tier() != Body.Tier.COMET) {
                assertEquals(0, BodyArt.centerX(body), 0, body.id());
                assertEquals(0, BodyArt.centerY(body), 0, body.id());
            } else {
                assertTrue(BodyArt.centerX(body) > 0 && BodyArt.centerY(body) < 0, "vers le haut à droite");
            }
        }
    }

    @Test
    void chaqueAstreSeDessineAvecEtSansDetail() {
        FxTestSupport.fx(() -> {
            Canvas canvas = new Canvas(300, 300);
            GraphicsContext g = canvas.getGraphicsContext2D();
            for (Body body : Bodies.DEFAULT) {
                // En dessous du seuil, juste dessus, et bien plus grand : aucun ne doit lever d'exception.
                for (double r : new double[] {2, BodyArt.DETAIL_FROM - 1, BodyArt.DETAIL_FROM, 40}) {
                    g.clearRect(0, 0, 300, 300);
                    BodyArt.draw(g, body, 150, 150, r);
                }
            }
            return null;
        });
    }

    @Test
    void chaqueAstreSeDessineDansChaqueSkin() {
        FxTestSupport.fx(() -> {
            Canvas canvas = new Canvas(300, 300);
            GraphicsContext g = canvas.getGraphicsContext2D();
            assertEquals(3, Skin.BODIES.length);
            for (Skin skin : Skin.BODIES) {
                assertEquals(idle.core.universe.SkinVisual.BODY, skin.visual(), skin.name());
                for (Body body : Bodies.DEFAULT) {
                    // Minuscule, sous et sur le seuil du nom de la Cartographie, et grand : aucun ne doit lever d'exception.
                    for (double r : new double[] {2, BodySkinArt.NAME_FROM - 1, BodySkinArt.NAME_FROM, 40}) {
                        g.clearRect(0, 0, 300, 300);
                        BodyArt.draw(g, body, 150, 150, r, skin);
                    }
                }
            }
            // Un skin qui n'habille pas les astres, ou aucun, donne le dessin classique.
            BodyArt.draw(g, Bodies.DEFAULT.get(0), 150, 150, 20, Skin.TREE_BIRCH);
            BodyArt.draw(g, Bodies.DEFAULT.get(0), 150, 150, 20, null);
            return null;
        });
    }

    @Test
    void lEmpriseNeDependPasDuSkin() {
        // Les emprises sont des fonctions de l'astre seul : les skins dessinent dedans, sans les changer.
        for (Body body : Bodies.DEFAULT) {
            double[] want = expected(body);
            assertEquals(want[0], BodyArt.span(body), 0.01, body.id());
            assertEquals(want[2], BodyArt.reachDown(body), 0.001, body.id());
        }
        assertEquals(BodyArt.DETAIL_FROM, BodySkinArt.NAME_FROM, "le nom de la Cartographie apparaît au rayon des couches de détail");
    }

    @Test
    void sansSkinLeDessinEstLeClassique() {
        FxTestSupport.fx(() -> {
            // Les deux appels sont dessinés sur deux toiles et comparés pixel à pixel.
            Canvas plain = new Canvas(120, 120);
            Canvas nulled = new Canvas(120, 120);
            for (Body body : Bodies.DEFAULT) {
                plain.getGraphicsContext2D().clearRect(0, 0, 120, 120);
                nulled.getGraphicsContext2D().clearRect(0, 0, 120, 120);
                BodyArt.draw(plain.getGraphicsContext2D(), body, 60, 60, 16);
                BodyArt.draw(nulled.getGraphicsContext2D(), body, 60, 60, 16, null);
                javafx.scene.image.WritableImage a = plain.snapshot(null, null);
                javafx.scene.image.WritableImage b = nulled.snapshot(null, null);
                for (int px = 0; px < 120; px += 3) {
                    for (int py = 0; py < 120; py += 3) {
                        assertEquals(a.getPixelReader().getArgb(px, py), b.getPixelReader().getArgb(px, py), body.id() + " en " + px + "," + py);
                    }
                }
            }
            return null;
        });
    }

    @Test
    void leCielDonneAuxAstresDesRayonsOrdonnes() {
        for (Body body : Bodies.DEFAULT) {
            assertTrue(SkyView.radius(body) >= 9, body.id());
        }
        Game game = new Game();
        game.start();
        assertTrue(SkyView.radius(game.body("stony_asteroid")) >= BodyArt.DETAIL_FROM, "un astéroïde a ses détails à l'échelle 1");
        assertTrue(SkyView.radius(game.body("white_dwarf")) >= BodyArt.DETAIL_FROM, "une naine blanche aussi");
        assertTrue(SkyView.radius(game.body("white_dwarf")) < SkyView.radius(game.body("stony_asteroid")), "mais reste plus petite");
    }

    /** Un jeu avec trois sortes rassemblées, une par état, dans un espace immense : vu en entier, tout est du gaz. */
    private static Game gathered() {
        Game game = new Game();
        game.start();
        game.state().setMoleculeCount("H2O", 300);
        game.state().setMoleculeCount("CO2", 200);
        game.state().setMoleculeCount("NaCl", 100);
        game.state().addSubstance("H2O");
        game.state().addSubstance("CO2");
        game.state().addSubstance("NaCl");
        game.state().setSpace(idle.core.math.BigNum.of(1e12));
        return game;
    }

    @Test
    void lEspaceVuDeTresLoinSeDessineAvecSesMarques() {
        FxTestSupport.fx(() -> {
            Game game = gathered();
            SpaceView view = new SpaceView(game);
            view.setWidth(600);
            view.setHeight(360);
            view.fit();
            view.frame(0.1);
            assertEquals(600, view.placed());
            assertEquals(3, view.clusters(), "un amas par sorte rassemblée");
            assertEquals(1, view.mist(), 1e-9, "tout l'espace : la matière n'est que du gaz, et les amas sont marqués par-dessus");
            // Entre les deux, le gaz et les marques se mêlent au dessin de près : cela se dessine aussi.
            for (int step = 0; step < 40 && view.mist() > 0; step++) {
                view.zoom(SpaceView.ZOOM_STEP);
                view.frame(0.1);
            }
            assertEquals(0, view.mist(), 1e-9, "de tout près, plus de gaz");
            return null;
        });
    }
}
