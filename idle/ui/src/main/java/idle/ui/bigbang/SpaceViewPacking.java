package idle.ui.bigbang;

import idle.core.Game;
import idle.core.molecules.Molecule;
import java.util.List;
import javafx.scene.paint.Color;

/**
 * La vue de l'espace : la géométrie du rangement (grilles, croissance, soudures, collisions, couleurs).
 */
public abstract class SpaceViewPacking extends SpaceViewPlacement {

    SpaceViewPacking(Game game) {
        super(game);
    }


    /**
     * Pose les clairières l'une après l'autre : la première au centre, chaque suivante contre une de
     * celles qui sont posées, là où elle est le plus près du centre sans rien recouvrir.
     *
     * @param full  le rayon de chaque clairière quand elle a toute sa place ; {@code share} ce qu'il en reste
     * @param limit le rayon du disque où elles doivent tenir
     * @param round reçoit leurs rayons
     * @return faux si l'une d'elles n'a trouvé aucune place
     */
    static boolean pack(double[] full, double share, double limit, double[] cx, double[] cy, double[] round) {
        for (int body = 0; body < full.length; body++) round[body] = share * full[body];
        for (int body = 0; body < full.length; body++) {
            double best = Double.POSITIVE_INFINITY;
            if (body == 0 && round[0] <= limit) {
                cx[0] = 0;
                cy[0] = 0;
                best = 0;
            }
            for (int host = 0; host < body; host++) {
                double away = round[host] + round[body];
                for (int bearing = 0; bearing < BEARINGS; bearing++) {
                    double angle = 2 * Math.PI * bearing / BEARINGS + host;
                    double px = cx[host] + away * Math.cos(angle);
                    double py = cy[host] + away * Math.sin(angle);
                    double out = Math.hypot(px, py);
                    if (out >= best || out + round[body] > limit) continue;
                    boolean free = true;
                    for (int other = 0; other < body && free; other++) {
                        free = other == host || Math.hypot(px - cx[other], py - cy[other]) >= round[other] + round[body] - 1e-9;
                    }
                    if (!free) continue;
                    best = out;
                    cx[body] = px;
                    cy[body] = py;
                }
            }
            if (best == Double.POSITIVE_INFINITY) return false;
        }
        return true;
    }

    /**
     * Donne à un astre les places libres à moins de {@code within} de (cx, cy), les plus proches
     * d'abord, jusqu'à ce qu'il en ait {@code wanted}.
     *
     * @param group reçoit les places, à la suite des {@code held} qu'il a déjà
     * @return le nombre de places qu'il a maintenant
     */
    static int claim(double cx, double cy, double within, int wanted, int cluster, int[] group, int held,
            double[] x, double[] y, int[] owner, Grid grid) {
        if (held >= wanted) return held;
        int close = grid.near(cx - within, cy - within, cx + within, cy + within);
        int[] found = grid.found();
        // Chaque place libre, rangée par sa distance : la distance d'abord, la place ensuite, dans un seul nombre.
        long[] ranked = new long[close];
        int free = 0;
        for (int index = 0; index < close; index++) {
            int cell = found[index];
            if (owner[cell] >= 0) continue;
            double gap = Math.hypot(x[cell] - cx, y[cell] - cy);
            if (gap > within) continue;
            ranked[free++] = ((long) Float.floatToIntBits((float) gap) << 32) | cell;
        }
        java.util.Arrays.sort(ranked, 0, free);
        for (int index = 0; index < free && held < wanted; index++) {
            int cell = (int) ranked[index];
            owner[cell] = cluster;
            group[held++] = cell;
        }
        return held;
    }

    /**
     * Mesure le cercle où le dessin de chaque astre peut tenir : sa clairière, moins tout ce qui
     * s'en approche. Rien de ce qui est dessiné autour ne doit être touché : ni le rond ou le pont
     * d'un amas, ni une molécule isolée, ni le bord du cœur. Les autres astres n'ont pas à être
     * mesurés, leurs clairières ne se recouvrent pas.
     */
    void clear(double[] x, double[] y, int[] owner, double span, Grid grid) {
        for (int body = 0; body < sky.length; body++) {
            double cx = skyAway[body] * Math.cos(skyAngle[body]);
            double cy = skyAway[body] * Math.sin(skyAngle[body]);
            double round = Math.min(skyRound[body], span - skyAway[body]);
            double look = skyRound[body] + FAR;
            int close = grid.near(cx - look, cy - look, cx + look, cy + look);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (hidden[other]) continue;
                round = Math.min(round, Math.hypot(x[other] - cx, y[other] - cy) - (owner[other] < 0 ? radii[other] : blobs[other]));
            }
            for (int cluster = sky.length; cluster < bridges.length; cluster++) {
                int[] links = bridges[cluster];
                for (int link = 0; link < links.length; link += 2) {
                    double width = widths[cluster][link / 2];
                    if (width <= 0) continue;
                    int from = links[link];
                    int to = links[link + 1];
                    if (Math.min(x[from], x[to]) > cx + look || Math.max(x[from], x[to]) < cx - look
                            || Math.min(y[from], y[to]) > cy + look || Math.max(y[from], y[to]) < cy - look) continue;
                    round = Math.min(round, away(cx, cy, x[from], y[from], x[to], y[to]) - width);
                }
            }
            skyRadius[body] = Math.max(0, round - BODY_GAP);
        }
    }

    /**
     * Les ponts d'un amas : ceux qui l'ont construit ({@code tree}, un par molécule ajoutée), plus un
     * pont entre deux de ses molécules chaque fois qu'elles sont à moins de {@code join} l'une de
     * l'autre. Un amas n'est donc pas un chapelet de molécules reliées à la file, mais un tissu où
     * chacune tient à toutes ses voisines.
     *
     * @return les ponts, deux places par pont, sans doublon
     */
    static int[] weld(int[] cells, int[] tree, double[] x, double[] y, double join, Grid grid, int[] rank, int[] near) {
        Pairs seen = new Pairs(4 * cells.length + 16);
        int[] pairs = new int[Math.max(16, 8 * cells.length)];
        int count = 0;
        for (int link = 0; link < tree.length; link += 2) {
            if (seen.add(pair(tree[link], tree[link + 1]))) {
                if (count + 2 > pairs.length) pairs = java.util.Arrays.copyOf(pairs, 2 * pairs.length);
                pairs[count++] = tree[link];
                pairs[count++] = tree[link + 1];
            }
        }
        // Le rang de chaque place dans l'amas : les ponts sont posés dans l'ordre des places, comme si on les comparait toutes deux à deux.
        for (int index = 0; index < cells.length; index++) rank[cells[index]] = index;
        for (int first = 0; first < cells.length; first++) {
            int from = cells[first];
            // Seules les places proches peuvent être à portée : celles de l'amas, plus loin que celle-ci dans son ordre.
            int close = grid.near(x[from] - join, y[from] - join, x[from] + join, y[from] + join);
            int[] found = grid.found();
            int kept = 0;
            for (int each = 0; each < close; each++) {
                int to = found[each];
                if (rank[to] <= first) continue;
                if (Math.abs(x[to] - x[from]) > join || Math.abs(y[to] - y[from]) > join) continue;
                if (Math.hypot(x[to] - x[from], y[to] - y[from]) > join) continue;
                near[kept++] = rank[to];
            }
            java.util.Arrays.sort(near, 0, kept);
            for (int each = 0; each < kept; each++) {
                int to = cells[near[each]];
                if (seen.add(pair(from, to))) {
                    if (count + 2 > pairs.length) pairs = java.util.Arrays.copyOf(pairs, 2 * pairs.length);
                    pairs[count++] = from;
                    pairs[count++] = to;
                }
            }
        }
        for (int cell : cells) rank[cell] = -1;
        return java.util.Arrays.copyOf(pairs, count);
    }

    /**
     * Un ensemble de paires de places ({@link #pair}), sans rien d'autre que deux tableaux : la mise
     * en place en range des dizaines de milliers.
     */
    private static final class Pairs {
        private final long[] keys;
        private final boolean[] used;
        private final int mask;

        Pairs(int room) {
            int size = Integer.highestOneBit(Math.max(16, 2 * room) - 1) << 1;
            keys = new long[size];
            used = new boolean[size];
            mask = size - 1;
        }

        private int slot(long key) {
            long mixed = key * 0x9E3779B97F4A7C15L;
            int slot = (int) (mixed >>> 40) & mask;
            while (used[slot] && keys[slot] != key) slot = (slot + 1) & mask;
            return slot;
        }

        /** Ajoute une paire. Faux si elle y était déjà. */
        boolean add(long key) {
            int slot = slot(key);
            if (used[slot]) return false;
            used[slot] = true;
            keys[slot] = key;
            return true;
        }

        boolean contains(long key) {
            return used[slot(key)];
        }
    }

    /** Une clé pour la paire de places (a, b), la même dans les deux sens. */
    private static long pair(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }

    /**
     * La collision des amas : taille le rond de chaque molécule rassemblée, puis chaque pont, pour
     * qu'aucun amas n'en recouvre un autre. Tout se mesure en rayons de place.
     *
     * <p>Un rond a le rayon {@link #BLOB_SHARE}, sauf s'il rencontre avant :
     * <ul>
     *   <li>le rond d'un autre amas : chacun s'arrête à mi-chemin, et les deux se touchent sans se
     *       recouvrir ;</li>
     *   <li>le dessin d'une molécule qui n'est pas rassemblée : le rond s'arrête à son bord ;</li>
     *   <li>le bord du cœur utilisé : aucun amas n'en dépasse.</li>
     * </ul>
     * Les ronds d'un même amas, eux, se recouvrent librement : c'est ce qui les soude. Un pont est
     * aussi large qu'il peut l'être sans toucher un rond ou un pont d'un autre amas ; s'il n'a pas
     * la place de passer, il n'est pas dessiné ({@link #THINNEST_BRIDGE}). Trois molécules reliées
     * deux à deux ferment un triangle, qui est rempli s'il ne contient rien d'étranger à l'amas.
     *
     * @param owner l'amas de chaque place, ou -1 si sa molécule n'est pas rassemblée
     * @param span  le rayon du cœur utilisé
     */
    void collide(double[] x, double[] y, int[] owner, double span, Grid grid) {
        int count = x.length;
        blobs = new double[count];
        for (int index = 0; index < count; index++) {
            // Une place d'astre n'a pas de rond : c'est l'astre qui est dessiné, et mesuré à part ({@link #clear}).
            if (owner[index] < 0 || hidden[index]) continue;
            double radius = Math.min(BLOB_SHARE, span - Math.hypot(x[index], y[index]));
            // Au-delà de deux ronds entiers, rien ne peut se toucher : seules les places proches sont mesurées.
            int close = grid.near(x[index] - FAR, y[index] - FAR, x[index] + FAR, y[index] + FAR);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (owner[other] == owner[index]) continue;
                // Trop loin pour raccourcir ce rond : la mesure exacte, plus lente, n'apprendrait rien.
                double dx = x[other] - x[index];
                double dy = y[other] - y[index];
                double harmless = owner[other] < 0 ? radius + radii[other] : 2 * radius;
                if (dx * dx + dy * dy > harmless * harmless * SURELY) continue;
                double gap = Math.hypot(dx, dy);
                radius = Math.min(radius, owner[other] < 0 ? gap - radii[other] : gap / 2);
            }
            blobs[index] = Math.max(0, radius);
        }
        overlap = Double.NEGATIVE_INFINITY;
        contacts = 0;
        for (int index = 0; index < count; index++) {
            if (owner[index] < 0 || hidden[index]) continue;
            int close = grid.near(x[index] - FAR, y[index] - FAR, x[index] + FAR, y[index] + FAR);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (other <= index || owner[other] < 0 || hidden[other] || owner[other] == owner[index]) continue;
                double excess = blobs[index] + blobs[other] - Math.hypot(x[other] - x[index], y[other] - y[index]);
                overlap = Math.max(overlap, excess);
                if (Math.abs(excess) < 1e-9) contacts++;
            }
        }
        if (overlap == Double.NEGATIVE_INFINITY) overlap = 0;

        // Les ponts déjà posés, retrouvés par leur place de départ : pour chaque place, le premier pont
        // qui en part, puis de pont en pont. Les rares ponts très longs sont gardés à part.
        int total = 0;
        for (int[] links : bridges) total += links.length / 2;
        int[] laidFrom = new int[total];
        int[] laidTo = new int[total];
        double[] laidWidth = new double[total];
        int[] laidCluster = new int[total];
        int[] laidNext = new int[total];
        int[] firstLaid = new int[count];
        java.util.Arrays.fill(firstLaid, -1);
        List<Integer> longOnes = new java.util.ArrayList<>();
        double longest = 2 * FAR;
        int laid = 0;

        widths = new double[bridges.length][];
        for (int cluster = 0; cluster < bridges.length; cluster++) {
            int[] links = bridges[cluster];
            widths[cluster] = new double[links.length / 2];
            for (int link = 0; link < links.length; link += 2) {
                int from = links[link];
                int to = links[link + 1];
                double width = Math.min(BRIDGE_SHARE, Math.min(blobs[from], blobs[to]));
                double left = Math.min(x[from], x[to]) - FAR;
                double right = Math.max(x[from], x[to]) + FAR;
                double top = Math.min(y[from], y[to]) - FAR;
                double bottom = Math.max(y[from], y[to]) + FAR;
                // Ce qui n'est pas de l'amas ne doit pas être touché : les ronds des autres, les molécules isolées...
                int close = grid.near(left, top, right, bottom);
                int[] found = grid.found();
                for (int each = 0; each < close && width > 0; each++) {
                    int other = found[each];
                    if (owner[other] == cluster) continue;
                    double body = owner[other] < 0 ? radii[other] : blobs[other];
                    // Trop loin pour rétrécir ce pont : inutile de mesurer exactement.
                    if (beyond(x[other], y[other], x[from], y[from], x[to], y[to], width + body)) continue;
                    width = Math.min(width, away(x[other], y[other], x[from], y[from], x[to], y[to]) - body);
                }
                // ... et les ponts déjà posés des autres amas : ceux qui partent d'une place proche, et les très longs.
                if (width > 0) {
                    close = grid.near(left - longest, top - longest, right + longest, bottom + longest);
                    found = grid.found();
                    for (int each = 0; each < close && width > 0; each++) {
                        for (int other = firstLaid[found[each]]; other >= 0 && width > 0; other = laidNext[other]) {
                            if (laidCluster[other] == cluster) continue;
                            if (aside(x[from], y[from], x[to], y[to], x[laidFrom[other]], y[laidFrom[other]],
                                    x[laidTo[other]], y[laidTo[other]], width + laidWidth[other])) continue;
                            width = Math.min(width, apart(x[from], y[from], x[to], y[to],
                                    x[laidFrom[other]], y[laidFrom[other]], x[laidTo[other]], y[laidTo[other]]) - laidWidth[other]);
                        }
                    }
                    for (int index = 0; index < longOnes.size() && width > 0; index++) {
                        int other = longOnes.get(index);
                        if (laidCluster[other] == cluster) continue;
                        width = Math.min(width, apart(x[from], y[from], x[to], y[to],
                                x[laidFrom[other]], y[laidFrom[other]], x[laidTo[other]], y[laidTo[other]]) - laidWidth[other]);
                    }
                }
                if (width < THINNEST_BRIDGE) width = 0;
                widths[cluster][link / 2] = width;
                if (width > 0) {
                    laidFrom[laid] = from;
                    laidTo[laid] = to;
                    laidWidth[laid] = width;
                    laidCluster[laid] = cluster;
                    if (Math.hypot(x[to] - x[from], y[to] - y[from]) > longest) {
                        laidNext[laid] = -1;
                        longOnes.add(laid);
                    } else {
                        laidNext[laid] = firstLaid[from];
                        firstLaid[from] = laid;
                    }
                    laid++;
                }
            }
        }

        fills = new int[bridges.length][];
        int[] start = new int[count];
        int[] next = new int[count];
        for (int cluster = 0; cluster < bridges.length; cluster++) fills[cluster] = triangles(cluster, x, y, owner, grid, start, next);
    }

    /**
     * Les triangles pleins d'un amas : chaque triplet de ses molécules reliées deux à deux par des
     * ponts dessinés, s'il ne contient aucune place étrangère à l'amas. Comme ses trois côtés sont
     * des ponts qui ont eu la place de passer, rien d'un autre amas ne peut y entrer.
     *
     * @return les triangles, trois places par triangle
     */
    private int[] triangles(int cluster, double[] x, double[] y, int[] owner, Grid grid, int[] start, int[] next) {
        int[] links = bridges[cluster];
        if (links.length == 0) return new int[0];
        // Pour chaque place : les places auxquelles un pont dessiné la relie, dans l'ordre des ponts. Elles sont rangées
        // à la suite dans un seul tableau ; start dit où commencent celles de chaque place.
        Pairs drawn = new Pairs(links.length);
        int[] cells = clusters[cluster];
        for (int cell : cells) next[cell] = 0;
        for (int link = 0; link < links.length; link += 2) {
            if (widths[cluster][link / 2] <= 0) continue;
            drawn.add(pair(links[link], links[link + 1]));
            next[links[link]]++;
            next[links[link + 1]]++;
        }
        int total = 0;
        for (int cell : cells) {
            start[cell] = total;
            total += next[cell];
            next[cell] = start[cell];
        }
        int[] joined = new int[total];
        for (int link = 0; link < links.length; link += 2) {
            if (widths[cluster][link / 2] <= 0) continue;
            joined[next[links[link]]++] = links[link + 1];
            joined[next[links[link + 1]]++] = links[link];
        }
        int[] kept = new int[Math.max(16, 3 * links.length)];
        int count = 0;
        for (int link = 0; link < links.length; link += 2) {
            int a = links[link];
            int b = links[link + 1];
            if (!drawn.contains(pair(a, b))) continue;
            for (int index = start[a]; index < next[a]; index++) {
                int c = joined[index];
                // Chaque triangle une seule fois : par son côté dont la troisième pointe a le plus grand numéro.
                if (c <= Math.max(a, b) || !drawn.contains(pair(b, c))) continue;
                int close = grid.near(Math.min(x[a], Math.min(x[b], x[c])), Math.min(y[a], Math.min(y[b], y[c])),
                        Math.max(x[a], Math.max(x[b], x[c])), Math.max(y[a], Math.max(y[b], y[c])));
                int[] found = grid.found();
                boolean empty = true;
                for (int each = 0; each < close && empty; each++) {
                    int other = found[each];
                    if (owner[other] == cluster) continue;
                    double side1 = (x[b] - x[a]) * (y[other] - y[a]) - (y[b] - y[a]) * (x[other] - x[a]);
                    double side2 = (x[c] - x[b]) * (y[other] - y[b]) - (y[c] - y[b]) * (x[other] - x[b]);
                    double side3 = (x[a] - x[c]) * (y[other] - y[c]) - (y[a] - y[c]) * (x[other] - x[c]);
                    empty = !((side1 >= 0 && side2 >= 0 && side3 >= 0) || (side1 <= 0 && side2 <= 0 && side3 <= 0));
                }
                if (!empty) continue;
                if (count + 3 > kept.length) kept = java.util.Arrays.copyOf(kept, 2 * kept.length);
                kept[count++] = a;
                kept[count++] = b;
                kept[count++] = c;
            }
        }
        return java.util.Arrays.copyOf(kept, count);
    }

    /**
     * Vrai si le point (px, py) est à coup sûr à plus de {@code reach} du segment qui va de (ax, ay)
     * à (bx, by) : le même calcul que {@link #away}, sans sa racine, avec une marge qui couvre les
     * arrondis. Faux dans le doute : l'appelant mesure alors exactement.
     */
    private static boolean beyond(double px, double py, double ax, double ay, double bx, double by, double reach) {
        if (reach <= 0) return false;
        double dx = bx - ax;
        double dy = by - ay;
        double length = dx * dx + dy * dy;
        double along = length <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        double offX = px - (ax + along * dx);
        double offY = py - (ay + along * dy);
        return offX * offX + offY * offY > reach * reach * SURELY;
    }

    /** Distance du point (px, py) au segment qui va de (ax, ay) à (bx, by). */
    private static double away(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double length = dx * dx + dy * dy;
        double along = length <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        return Math.hypot(px - (ax + along * dx), py - (ay + along * dy));
    }

    /**
     * Vrai si les segments (a, b) et (c, d) sont à coup sûr à plus de {@code reach} l'un de l'autre :
     * leurs cadres sont plus écartés que cela, en largeur ou en hauteur. Faux dans le doute :
     * l'appelant mesure alors exactement ({@link #apart}).
     */
    private static boolean aside(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy, double reach) {
        if (reach <= 0) return false;
        double far = reach * SURELY;
        return Math.min(cx, dx) - Math.max(ax, bx) > far || Math.min(ax, bx) - Math.max(cx, dx) > far
                || Math.min(cy, dy) - Math.max(ay, by) > far || Math.min(ay, by) - Math.max(cy, dy) > far;
    }

    /** Distance entre le segment (a, b) et le segment (c, d) : nulle s'ils se croisent. */
    private static double apart(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double side1 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        double side2 = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax);
        double side3 = (dx - cx) * (ay - cy) - (dy - cy) * (ax - cx);
        double side4 = (dx - cx) * (by - cy) - (dy - cy) * (bx - cx);
        if (side1 * side2 < 0 && side3 * side4 < 0) return 0;
        return Math.min(Math.min(away(ax, ay, cx, cy, dx, dy), away(bx, by, cx, cy, dx, dy)),
                Math.min(away(cx, cy, ax, ay, bx, by), away(dx, dy, ax, ay, bx, by)));
    }

    /**
     * La couleur de l'amas d'une sorte de molécule : celle de la molécule, fondue dans la teinte de
     * son état, d'autant plus présente que l'état est serré. Elle est opaque, pour que les ronds et
     * les ponts d'un même amas se recouvrent sans que leur teinte s'additionne.
     */
    static Color blobColor(MoleculeArt.Shape shape, Molecule.State state) {
        double share = switch (state) {
            case GAS -> 0.18;
            case LIQUID -> 0.34;
            case SOLID -> 0.5;
            case CRYSTAL -> 0.6;
            case METAL -> 0.7;
        };
        return SPACE.interpolate(placeColor(state), PLACE_TINT).interpolate(shape.dominant(), share);
    }

    void settle(int cell, Molecule molecule, Molecule.State state) {
        if (molecule == null) return;     // une sorte inconnue (sauvegarde d'une autre version) : la place sera reprise plus loin
        // Une grosse molécule est dessinée un peu plus grande qu'une petite, sans dépasser sa place.
        radii[cell] = SMALLEST + (1 - SMALLEST) * Math.min(1, Math.sqrt(molecule.atoms() / (double) BIGGEST_ATOMS));
        shapes[cell] = MoleculeArt.of(molecule);
        states[cell] = state;
    }

    /**
     * La couleur d'un état, celle du liseré de ses amas : pâle pour les gaz, bleue pour les liquides,
     * sable pour les solides, violette pour les cristaux, acier pour les métaux.
     */
    static Color placeColor(Molecule.State place) {
        return switch (place) {
            case GAS -> GAS_COLOR;
            case LIQUID -> LIQUID_COLOR;
            case SOLID -> SOLID_COLOR;
            case CRYSTAL -> CRYSTAL_COLOR;
            case METAL -> METAL_COLOR;
        };
    }

    private static final Color GAS_COLOR = Color.web("#bfe6f2");
    private static final Color LIQUID_COLOR = Color.web("#4d8fe0");
    private static final Color SOLID_COLOR = Color.web("#d9b36c");
    private static final Color CRYSTAL_COLOR = Color.web("#b48cf2");
    private static final Color METAL_COLOR = Color.web("#aeb6c4");
}
