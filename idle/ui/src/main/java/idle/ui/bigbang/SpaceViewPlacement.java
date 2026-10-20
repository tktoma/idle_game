package idle.ui.bigbang;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import java.util.List;
import javafx.scene.paint.Color;

/**
 * La vue de l'espace : où va chaque molécule, chaque amas et chaque astre.
 */
public abstract class SpaceViewPlacement extends SpaceViewBase {

    SpaceViewPlacement(Game game) {
        super(game);
    }

    // Ce que fournissent les parties suivantes.
    abstract void clear(double[] x, double[] y, int[] owner, double span, Grid grid);
    abstract void collide(double[] x, double[] y, int[] owner, double span, Grid grid);
    abstract void settle(int cell, Molecule molecule, Molecule.State state);


    /**
     * Range les molécules créées sur la spirale, quand leur liste a changé. Les places sont comptées
     * en parts du cœur utilisé, pas en unités d'espace : elles se resserrent ou s'écartent avec lui,
     * et aucune ne peut se retrouver hors de l'espace.
     *
     * <p>Les assemblages choisissent d'abord : chacun prend au centre un bloc de places qui se
     * touchent, et y mêle les molécules de ses ingrédients. Les molécules rassemblées qui restent
     * choisissent ensuite. Chaque sorte prend des places qui se touchent, en partant de la place
     * libre la plus proche du centre ({@link Growth#gather}) ; les sortes passent dans l'ordre des
     * états, du plus serré au plus lâche, et les métaux se retrouvent donc près du cœur, les gaz
     * vers le bord. Les autres molécules prennent les places qui
     * restent, du centre vers le bord, dans l'ordre de leur création. Les ronds des amas sont
     * ensuite taillés pour ne pas se chevaucher ({@link #collide}).
     *
     * <p>Tout cela ne regarde jamais que les places proches les unes des autres ({@link Grid}) :
     * le temps passé grandit comme le nombre de molécules, pas comme son carré.
     */
    void place() {
        if (placedVersion == game.state().moleculesVersion() || calm) return;
        placedVersion = game.state().moleculesVersion();
        sincePlaced = 0;
        List<String> log = game.state().moleculeLog();
        // Trop de molécules pour un point chacune : chaque sorte garde sa part, un point en valant plusieurs.
        long total = game.state().moleculeTotal();
        perDot = total <= MOST_DOTS ? 1 : (int) ((total + MOST_DOTS - 1) / MOST_DOTS);
        int count = (int) Math.min(total, MOST_DOTS);
        if (perDot > 1) {
            count = 0;
            for (Molecule molecule : game.molecules()) count += dots(game.moleculeCount(molecule.id()));
        }
        distances = new double[count];
        angles = new double[count];
        radii = new double[count];
        shapes = new MoleculeArt.Shape[count];
        states = new Molecule.State[count];
        // La position de chaque place, en rayons de place : c'est dans cette unité que tout se mesure ici.
        double span = count == 0 ? 0 : Math.sqrt(count) / CELL_SHARE;
        double[] x = new double[count];
        double[] y = new double[count];
        cosines = new double[count];
        sines = new double[count];
        swayCos = new double[count];
        swaySin = new double[count];
        heaveCos = new double[count];
        heaveSin = new double[count];
        xs = new double[count];
        ys = new double[count];
        for (int index = 0; index < count; index++) {
            // La spirale du tournesol : la k-ième place est à la racine de sa part du disque.
            distances[index] = count == 1 ? 0 : Math.sqrt((index + 0.5) / count);
            angles[index] = index * GOLDEN_ANGLE;
            cosines[index] = Math.cos(angles[index]);
            sines[index] = Math.sin(angles[index]);
            x[index] = distances[index] * (span - 1) * cosines[index];
            y[index] = distances[index] * (span - 1) * sines[index];
            swayCos[index] = Math.cos(index * 1.7);
            swaySin[index] = Math.sin(index * 1.7);
            heaveCos[index] = Math.cos(index * 2.3);
            heaveSin[index] = Math.sin(index * 2.3);
        }
        Grid grid = new Grid(x, y, span);
        // Ce qu'il reste à placer de chaque sorte rassemblée, une fois servis les assemblages.
        java.util.Map<String, Integer> spare = new java.util.HashMap<>();
        for (String id : game.state().substances()) {
            if (game.gatheredMolecules(id) > 0) spare.put(id, dots(game.gatheredMolecules(id)));
        }
        // Les astres passent avant tout, du plus gros au plus petit. Chacun réunit ses assemblages : leurs molécules,
        // mêlées comme dans un bloc, toujours de la même façon pour un assemblage donné.
        List<Body> launched = new java.util.ArrayList<>();
        List<List<Assembly>> parts = new java.util.ArrayList<>();
        List<List<List<Molecule>>> contents = new java.util.ArrayList<>();
        List<Integer> sizes = new java.util.ArrayList<>();
        java.util.Set<String> inside = new java.util.HashSet<>();
        for (String id : game.state().bodies()) {
            Body body = game.body(id);
            List<Assembly> made = new java.util.ArrayList<>();
            List<List<Molecule>> held = new java.util.ArrayList<>();
            int size = 0;
            for (String part : body.assemblies()) {
                if (!game.hasAssembly(part)) continue;
                inside.add(part);
                List<Molecule> mixed = new java.util.ArrayList<>();
                take(game.assembly(part), spare, mixed);
                if (mixed.isEmpty()) continue;
                java.util.Collections.shuffle(mixed, new java.util.Random(part.hashCode()));
                made.add(game.assembly(part));
                held.add(mixed);
                size += mixed.size();
            }
            if (size == 0) continue;
            // Le trou noir supermassif tient le centre, quelle que soit sa taille ; les autres suivent, du plus gros au plus petit.
            int rank = body.tier() == Body.Tier.CORE || launched.isEmpty() || launched.get(0).tier() != Body.Tier.CORE ? 0 : 1;
            if (body.tier() != Body.Tier.CORE) {
                while (rank < launched.size() && sizes.get(rank) >= size) rank++;
            }
            launched.add(rank, body);
            parts.add(rank, made);
            contents.add(rank, held);
            sizes.add(rank, size);
        }
        // Puis les blocs des assemblages qui ne sont dans aucun astre formé, dans l'ordre où ils ont été
        // formés, puis les amas. Pour chacun : ses molécules, une par place.
        List<List<Molecule>> bodies = new java.util.ArrayList<>();
        List<Assembly> formed = new java.util.ArrayList<>();
        for (String id : game.state().assemblies()) {
            if (inside.contains(id)) continue;
            Assembly assembly = game.assembly(id);
            List<Molecule> mixed = new java.util.ArrayList<>();
            take(assembly, spare, mixed);
            if (mixed.isEmpty()) continue;
            // Les ingrédients sont mêlés, toujours de la même façon pour un assemblage donné.
            java.util.Collections.shuffle(mixed, new java.util.Random(assembly.id().hashCode()));
            bodies.add(mixed);
            formed.add(assembly);
        }
        for (int place = 0; place < PLACES.length; place++) {
            gathered[place] = 0;
            for (Molecule molecule : game.molecules()) {
                int left = spare.getOrDefault(molecule.id(), 0);
                if (molecule.state() != PLACES[place] || left == 0) continue;
                bodies.add(java.util.Collections.nCopies(left, molecule));
                formed.add(null);
                gathered[place] += (long) left * perDot;
            }
        }
        int[] owner = new int[count];
        java.util.Arrays.fill(owner, -1);
        assembled = new boolean[count];
        hidden = new boolean[count];
        tints = new Color[count];
        // Deux places sont voisines en deçà de cette distance : un peu plus que l'écart entre deux places de la spirale.
        double reachable = count == 0 ? 0 : NEIGHBOUR * (span - 1) / span;
        double join = count == 0 ? 0 : JOIN * (span - 1) / span;
        // Pour souder les amas : le rang de chaque place dans l'amas en cours (-1 ailleurs), et les voisines trouvées.
        int[] rank = new int[count];
        java.util.Arrays.fill(rank, -1);
        int[] near = new int[count];
        // Les corps, à mesure qu'ils prennent place : leurs places, leurs ponts, et leur assemblage si ce sont des blocs.
        List<int[]> cells = new java.util.ArrayList<>();
        List<int[]> links = new java.util.ArrayList<>();
        List<Assembly> block = new java.util.ArrayList<>();
        List<double[]> marks = new java.util.ArrayList<>();
        int[][] groups = launch(launched, sizes, x, y, owner, grid, span);
        int taken = 0;
        for (int body = 0; body < groups.length; body++) {
            cells.add(null);
            links.add(new int[0]);
            block.add(null);
            marks.add(null);
            taken += groups[body].length;
        }
        // Chaque astre garde le milieu de sa clairière pour son dessin ; ses assemblages se rangent autour, en blocs.
        for (int body = 0; body < groups.length; body++) {
            double cx = skyAway[body] * Math.cos(skyAngle[body]);
            double cy = skyAway[body] * Math.sin(skyAngle[body]);
            double middle = HEART * skyRound[body];
            int[] group = groups[body];
            int[] heart = new int[group.length];
            int inner = 0;
            long[] around = new long[group.length];
            int outer = 0;
            for (int cell : group) {
                if (Math.hypot(x[cell] - cx, y[cell] - cy) <= middle) {
                    heart[inner++] = cell;
                } else {
                    // Rangées par leur angle autour de l'astre : l'angle d'abord, la place ensuite, dans un seul nombre.
                    float angle = (float) (Math.atan2(y[cell] - cy, x[cell] - cx) + Math.PI);
                    around[outer++] = ((long) Float.floatToIntBits(angle) << 32) | cell;
                }
            }
            java.util.Arrays.sort(around, 0, outer);
            List<Assembly> made = parts.get(body);
            List<List<Molecule>> held = contents.get(body);
            // La couronne se partage entre les assemblages, à proportion de leur taille ; le reste de leurs molécules est sous l'astre.
            int[] share = new int[made.size()];
            int given = 0;
            for (int part = 0; part < share.length; part++) {
                share[part] = (int) ((long) outer * held.get(part).size() / sizes.get(body));
                given += share[part];
            }
            for (int part = 0; given < outer && part < share.length; part++) {
                int more = Math.min(outer - given, held.get(part).size() - share[part]);
                share[part] += more;
                given += more;
            }
            List<Molecule> under = new java.util.ArrayList<>();
            int from = 0;
            for (int part = 0; part < share.length; part++) {
                List<Molecule> molecules = held.get(part);
                int[] sector = new int[share[part]];
                Color tint = Color.web(made.get(part).tint());
                for (int index = 0; index < sector.length; index++) {
                    int cell = (int) around[from + index];
                    sector[index] = cell;
                    owner[cell] = cells.size();
                    settle(cell, molecules.get(index), molecules.get(index).state());
                    assembled[cell] = true;
                    tints[cell] = tint;
                }
                under.addAll(molecules.subList(sector.length, molecules.size()));
                if (sector.length > 0) {
                    // Son nom s'écrit au milieu de son arc, à mi-épaisseur de la couronne : le milieu de ses places serait sous l'astre.
                    double first = Float.intBitsToFloat((int) (around[from] >>> 32)) - Math.PI;
                    double last = Float.intBitsToFloat((int) (around[from + sector.length - 1] >>> 32)) - Math.PI;
                    double ring = (middle + skyRound[body]) / 2;
                    marks.add(new double[] {cx + ring * Math.cos((first + last) / 2), cy + ring * Math.sin((first + last) / 2)});
                    cells.add(sector);
                    links.add(SpaceView.weld(sector, new int[0], x, y, join, grid, rank, near));
                    block.add(made.get(part));
                }
                from += sector.length;
            }
            // Ce qui resterait de la couronne sans molécule à y mettre rejoint le milieu.
            for (; from < outer; from++) heart[inner++] = (int) around[from];
            for (int index = 0; index < inner; index++) {
                Molecule molecule = under.isEmpty() ? held.get(0).get(0) : under.get(Math.min(index, under.size() - 1));
                settle(heart[index], molecule, molecule.state());
                hidden[heart[index]] = true;
            }
            cells.set(body, java.util.Arrays.copyOf(heart, inner));
            skyRound[body] = middle;
        }
        Growth growth = new Growth(x, y, owner, grid, reachable);
        for (int index = 0; index < bodies.size(); index++) {
            List<Molecule> body = bodies.get(index);
            int size = Math.min(body.size(), count - taken);
            int[] tree = new int[2 * Math.max(0, size - 1)];
            int cluster = cells.size();
            int[] got = growth.gather(size, cluster, tree);
            cells.add(got);
            links.add(SpaceView.weld(got, tree, x, y, join, grid, rank, near));
            block.add(formed.get(index));
            marks.add(null);
            Color tint = formed.get(index) == null ? null : Color.web(formed.get(index).tint());
            for (int each = 0; each < size; each++) {
                int cell = got[each];
                settle(cell, body.get(each), body.get(each).state());
                assembled[cell] = tint != null;
                tints[cell] = tint;
            }
            taken += size;
        }
        clusters = cells.toArray(new int[0][]);
        bridges = links.toArray(new int[0][]);
        blocks = block.toArray(new Assembly[0]);
        int next = 0;
        if (perDot == 1) {
            // Les molécules rassemblées d'une sorte sont déjà placées dans leurs amas ; le reste de la sorte, libre,
            // prend sa place ici (version 0.090 : avant, toute la sorte était sautée et des places restaient vides).
            java.util.Map<String, Integer> skip = new java.util.HashMap<>();
            for (String id : game.state().substances()) {
                if (game.gatheredMolecules(id) > 0) skip.put(id, game.gatheredMolecules(id));
            }
            for (String id : log) {
                int left = skip.getOrDefault(id, 0);
                if (left > 0) {
                    skip.put(id, left - 1);
                    continue;
                }
                while (next < count && owner[next] >= 0) next++;
                if (next >= count) break;
                settle(next++, game.molecule(id), null);
            }
        } else {
            // Un point pour plusieurs molécules : l'ordre de création ne se lit plus, les sortes passent dans celui du catalogue.
            for (Molecule molecule : game.molecules()) {
                int gathered = game.gatheredMolecules(molecule.id());
                int free = Math.max(0, dots(game.moleculeCount(molecule.id())) - (gathered > 0 ? dots(gathered) : 0));
                for (int left = free; left > 0; left--) {
                    while (next < count && owner[next] >= 0) next++;
                    if (next >= count) break;
                    settle(next++, molecule, null);
                }
            }
        }
        // Version 0.090 : le compte des molécules peut dépasser le journal (molécules d'avant le journal, sortes
        // retirées du catalogue…). Les places restées vides prennent une sorte réellement possédée, sinon se cachent :
        // une place sans dessin faisait tomber toute la vue.
        java.util.List<Molecule> owned = new java.util.ArrayList<>();
        for (Molecule molecule : game.molecules()) {
            if (game.moleculeCount(molecule.id()) > 0) owned.add(molecule);
        }
        boolean stand = owned.isEmpty();
        if (stand) owned = game.molecules();
        int nextOwned = 0;
        refilled = 0;
        for (int cell = 0; cell < count && !owned.isEmpty(); cell++) {
            if (shapes[cell] != null) continue;
            refilled++;
            settle(cell, owned.get(nextOwned++ % owned.size()), null);
            if (stand) hidden[cell] = true;
        }
        collide(x, y, owner, span, grid);
        clear(x, y, owner, span, grid);
        swirl(span);
        // Ce que chaque image redemanderait : les couleurs de chaque corps, et le cercle qui le contient.
        rims = new Color[clusters.length];
        inks = new Color[clusters.length];
        middleX = new double[clusters.length];
        middleY = new double[clusters.length];
        labelX = new double[clusters.length];
        labelY = new double[clusters.length];
        rounds = new double[clusters.length];
        for (int cluster = launched.size(); cluster < clusters.length; cluster++) {
            int[] members = clusters[cluster];
            if (members.length == 0) continue;
            int first = members[0];
            rims[cluster] = blocks[cluster] != null ? BLOCK_EDGE : SpaceView.placeColor(states[first]);
            inks[cluster] = blocks[cluster] != null ? Color.web(blocks[cluster].tint()) : SpaceView.blobColor(shapes[first], states[first]);
            double sumX = 0;
            double sumY = 0;
            for (int cell : members) {
                sumX += x[cell];
                sumY += y[cell];
            }
            middleX[cluster] = sumX / members.length;
            middleY[cluster] = sumY / members.length;
            double[] mark = marks.get(cluster);
            labelX[cluster] = mark == null ? middleX[cluster] : mark[0];
            labelY[cluster] = mark == null ? middleY[cluster] : mark[1];
            double farthest = 0;
            for (int cell : members) farthest = Math.max(farthest, Math.hypot(x[cell] - middleX[cluster], y[cell] - middleY[cluster]));
            rounds[cluster] = farthest + BLOB_SHARE;
        }
    }

    /**
     * Prépare la galaxie, si elle est formée : où chaque place va quand le cœur s'enroule en bras,
     * et où les astres se rangent le long de ces bras.
     *
     * <p>Une place garde sa distance au centre ; son angle est tiré vers le bras le plus proche
     * ({@link Spiral#bend}). Les places se resserrent donc dans les bras, et chacune n'y est plus
     * qu'un point, aussi gros que le permet sa plus proche voisine. Le trou noir supermassif reste
     * au centre ; les autres astres se suivent le long des deux bras, les plus gros près du centre,
     * sans se toucher. S'ils n'y tiennent pas tous, ils rapetissent ensemble.
     */
    private void swirl(double span) {
        int count = distances.length;
        if (!game.hasGalaxy() || count == 0) {
            swirlCos = new double[0];
            swirlSin = new double[0];
            rooms = new double[0];
            swirlTints = new Color[0];
            swirlAway = new double[0];
            swirlAngle = new double[0];
            swirlRadius = new double[0];
            return;
        }
        swirlCos = new double[count];
        swirlSin = new double[count];
        rooms = new double[count];
        swirlTints = new Color[count];
        // Une couleur ne se mêle qu'une fois par sorte de molécule et par anneau de la galaxie.
        java.util.Map<Color, Color[]> toned = new java.util.HashMap<>();
        for (int index = 0; index < count; index++) {
            Color own = tints[index] != null ? tints[index] : shapes[index].dominant();
            Color[] rings = toned.computeIfAbsent(own, each -> new Color[TONES]);
            int ring = Math.min(TONES - 1, (int) (distances[index] * TONES));
            if (rings[ring] == null) {
                double out = (ring + 0.5) / TONES;
                rings[ring] = own.interpolate(BULGE_TONE.interpolate(ARM_TONE, Math.min(1, out / 0.45)), TONE_SHARE);
            }
            swirlTints[index] = rings[ring];
        }
        double limit = span - 1;
        double[] gx = new double[count];
        double[] gy = new double[count];
        for (int index = 0; index < count; index++) {
            double angle = Spiral.bend(distances[index], angles[index]);
            swirlCos[index] = Math.cos(angle);
            swirlSin[index] = Math.sin(angle);
            gx[index] = distances[index] * limit * swirlCos[index];
            gy[index] = distances[index] * limit * swirlSin[index];
        }
        Grid grid = new Grid(gx, gy, span);
        for (int index = 0; index < count; index++) {
            if (hidden[index]) continue;
            double least = 2 * DOT_SHARE;
            int close = grid.near(gx[index] - least, gy[index] - least, gx[index] + least, gy[index] + least);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (other == index || hidden[other]) continue;
                double dx = gx[other] - gx[index];
                double dy = gy[other] - gy[index];
                if (dx * dx + dy * dy < least * least) least = Math.sqrt(dx * dx + dy * dy);
            }
            rooms[index] = least / 2;
        }
        // Les astres : celui du centre s'il y est, puis les autres le long des bras, tour à tour sur chacun.
        int many = sky.length;
        swirlAway = new double[many];
        swirlAngle = new double[many];
        swirlRadius = new double[many];
        double size = 0.8;
        while (!line(size, limit) && size > 0.05) size *= 0.9;
    }

    /**
     * Range les astres le long des bras de la galaxie, chacun dessiné à {@code size} fois sa taille
     * d'avant, sans dépasser ce qu'un bras a de large.
     *
     * @return faux s'ils n'y tiennent pas tous
     */
    private boolean line(double size, double limit) {
        int first = 0;
        double[] lastX = new double[Spiral.ARMS];
        double[] lastY = new double[Spiral.ARMS];
        double[] lastRadius = new double[Spiral.ARMS];
        double[] at = new double[Spiral.ARMS];
        // Deux bras voisins sont à cette distance l'un de l'autre : un astre n'en prend pas plus des quatre dixièmes.
        double widest = 0.4 * limit * (2 * Math.PI / Spiral.ARMS) / Spiral.TWIST;
        if (sky.length > 0 && sky[0].tier() == Body.Tier.CORE) {
            swirlAway[0] = 0;
            swirlAngle[0] = 0;
            swirlRadius[0] = Math.min(size * skyRadius[0] / HEART, 0.5 * widest + 0.06 * limit);
            java.util.Arrays.fill(lastRadius, swirlRadius[0]);
            first = 1;
        }
        java.util.Arrays.fill(at, 0.1);
        for (int body = first; body < sky.length; body++) {
            int arm = (body - first) % Spiral.ARMS;
            double radius = Math.min(size * skyRadius[body] * SpaceView.share(sky[body].tier()), widest);
            double share = at[arm];
            double x;
            double y;
            // On avance le long du bras jusqu'à ne plus toucher l'astre d'avant.
            while (true) {
                double angle = Spiral.arm(share) + arm * 2 * Math.PI / Spiral.ARMS;
                x = share * limit * Math.cos(angle);
                y = share * limit * Math.sin(angle);
                if (Math.hypot(x - lastX[arm], y - lastY[arm]) >= radius + lastRadius[arm] + BODY_GAP) break;
                share += 0.002;
                if (share * limit + radius > 0.97 * limit) return false;
            }
            if (share * limit + radius > 0.97 * limit) return false;
            swirlAway[body] = share * limit;
            swirlAngle[body] = Spiral.arm(share) + arm * 2 * Math.PI / Spiral.ARMS;
            swirlRadius[body] = radius;
            lastX[arm] = x;
            lastY[arm] = y;
            lastRadius[arm] = radius;
            at[arm] = share;
        }
        return true;
    }

    /** Retire de {@code spare} les molécules d'un assemblage, et les ajoute à {@code into}, une par exemplaire. */
    private void take(Assembly assembly, java.util.Map<String, Integer> spare, List<Molecule> into) {
        for (java.util.Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            int taken = Math.min(dots(ingredient.getValue()), spare.getOrDefault(ingredient.getKey(), 0));
            spare.merge(ingredient.getKey(), -taken, Integer::sum);
            for (int copy = 0; copy < taken; copy++) into.add(game.molecule(ingredient.getKey()));
        }
    }

    /**
     * Installe les astres : ce sont les premiers corps, et chacun prend autant de places qu'il a de
     * molécules.
     *
     * <p>Chaque astre a une clairière ronde, de la surface de ses places. Les clairières sont posées
     * l'une après l'autre, la plus grande au centre, chaque suivante accolée aux précédentes au plus
     * près du centre, sans qu'aucune en recouvre une autre ni ne sorte du cœur. Si elles n'y tiennent
     * pas toutes, elles rétrécissent ensemble jusqu'à y tenir ({@link #TIGHTER}) : un astre garde
     * alors autour de sa clairière les places qui n'y entrent plus. L'astre lui-même n'occupe que le
     * milieu de sa clairière ({@link #HEART}) : le reste est la couronne de ses assemblages.
     *
     * @return les places de chaque astre
     */
    private int[][] launch(List<Body> launched, List<Integer> wanted, double[] x, double[] y, int[] owner, Grid grid, double span) {
        int many = launched.size();
        int count = x.length;
        sky = launched.toArray(new Body[0]);
        skyAway = new double[many];
        skyAngle = new double[many];
        skyRound = new double[many];
        skyRadius = new double[many];
        skyTints = new Color[many];
        skyX = new double[many];
        skyY = new double[many];
        tightened = 1;
        if (many == 0) return new int[0][];
        // Les places tiennent dans le disque de rayon span - 1 : n places y occupent un disque de rayon (span - 1) × √(n / toutes).
        double limit = span - 1;
        double[] full = new double[many];
        int[] sizes = new int[many];
        int left = count;
        for (int body = 0; body < many; body++) {
            sizes[body] = Math.min(wanted.get(body), left);
            left -= sizes[body];
            full[body] = limit * Math.sqrt(CLEARING * sizes[body] / count);
        }
        double[] cx = new double[many];
        double[] cy = new double[many];
        while (!SpaceView.pack(full, tightened, limit, cx, cy, skyRound) && tightened > 0.02) tightened *= TIGHTER;
        // D'abord ce qui est dans chaque clairière, pour tous : aucune ne peut alors mordre sur une autre.
        int[][] groups = new int[many][];
        int[] held = new int[many];
        for (int body = 0; body < many; body++) {
            groups[body] = new int[sizes[body]];
            held[body] = SpaceView.claim(cx[body], cy[body], skyRound[body], sizes[body], body, groups[body], 0, x, y, owner, grid);
        }
        // Puis ce qui manque à chacun : les places libres les plus proches de sa clairière.
        for (int body = 0; body < many; body++) {
            double within = skyRound[body] + FAR;
            while (held[body] < sizes[body]) {
                held[body] = SpaceView.claim(cx[body], cy[body], within, sizes[body], body, groups[body], held[body], x, y, owner, grid);
                within = within * 1.3 + FAR;
            }
            skyAway[body] = Math.hypot(cx[body], cy[body]);
            skyAngle[body] = Math.atan2(cy[body], cx[body]);
            skyTints[body] = Color.web(sky[body].tint());
        }
        return groups;
    }

    /**
     * Les places rangées dans les cases d'un quadrillage, pour retrouver vite celles qui sont près
     * d'un endroit donné sans les passer toutes en revue. Une case fait {@link #FAR} de côté : ce qui
     * peut toucher un rond tient dans les cases voisines de la sienne.
     */
    static final class Grid {
        private final double[] x;
        private final double[] y;
        private final double origin;
        private final int side;
        /** Pour chaque case : la première place qui s'y trouve, ou -1 ; pour chaque place : la suivante de sa case, ou -1. */
        private final int[] head;
        private final int[] next;
        /** Les places trouvées par la dernière recherche ({@link #near}). */
        private int[] found = new int[64];

        Grid(double[] x, double[] y, double span) {
            this.x = x;
            this.y = y;
            origin = -span - 1;
            side = Math.max(1, (int) Math.ceil(2 * (span + 1) / FAR));
            head = new int[side * side];
            next = new int[x.length];
            java.util.Arrays.fill(head, -1);
            for (int cell = 0; cell < x.length; cell++) {
                int box = box(x[cell]) + side * box(y[cell]);
                next[cell] = head[box];
                head[box] = cell;
            }
        }

        private int box(double coordinate) {
            return Math.max(0, Math.min(side - 1, (int) ((coordinate - origin) / FAR)));
        }

        /**
         * Cherche les places des cases que touche le rectangle donné : toutes celles qui sont dedans,
         * et quelques-unes autour. Elles sont dans {@link #found()}, valable jusqu'à la recherche suivante.
         *
         * @return le nombre de places trouvées
         */
        int near(double left, double top, double right, double bottom) {
            int count = 0;
            int lastColumn = box(right);
            int lastRow = box(bottom);
            for (int row = box(top); row <= lastRow; row++) {
                for (int column = box(left); column <= lastColumn; column++) {
                    for (int cell = head[column + side * row]; cell >= 0; cell = next[cell]) {
                        if (count == found.length) found = java.util.Arrays.copyOf(found, 2 * count);
                        found[count++] = cell;
                    }
                }
            }
            return count;
        }

        int[] found() {
            return found;
        }
    }

    /**
     * Le choix des places des amas, l'un après l'autre. Un amas part de la place libre la plus
     * proche du centre qui a encore des voisines libres ; chaque place suivante est la place libre
     * la plus proche de celles qu'il a déjà, avec une préférence pour celles qui restent près de la
     * première ({@link #COMPACT}). Les places d'un amas se touchent donc, tant qu'il en reste de
     * libres à côté. Une place libre isolée entre deux amas est laissée à une molécule qui n'est
     * pas rassemblée, sauf s'il n'y a plus que cela.
     */
    private static final class Growth {
        private final double[] x;
        private final double[] y;
        private final int[] owner;
        private final Grid grid;
        private final double reachable;
        /** Jusqu'où un amas regarde autour de ses places pour trouver la suivante. */
        private final double sight;
        /** La première place qui peut encore être libre : tout ce qui est avant est pris. */
        private int from = 0;
        /** Le bord de l'amas en cours : les places libres à portée de vue de l'une des siennes. */
        private int[] front = new int[64];
        private int fronted = 0;
        private final boolean[] inFront;
        /** Pour chaque place du bord : la plus proche de celles de l'amas, et à quelle distance. */
        private final double[] nearest;
        private final int[] anchor;

        Growth(double[] x, double[] y, int[] owner, Grid grid, double reachable) {
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.grid = grid;
            this.reachable = reachable;
            this.sight = 1.5 * reachable;
            inFront = new boolean[x.length];
            nearest = new double[x.length];
            anchor = new int[x.length];
        }

        /**
         * Choisit les {@code size} places de l'amas numéro {@code cluster}, et les note dans {@code owner}.
         *
         * @param link reçoit les ponts qui ont construit l'amas, deux places par pont : chaque place y
         *             est reliée à celle de l'amas dont elle était la plus proche
         * @return les places choisies
         */
        int[] gather(int size, int cluster, int[] link) {
            int[] group = new int[size];
            if (size == 0) return group;
            int seed = seed(Math.min(size - 1, 2));
            group[0] = seed;
            owner[seed] = cluster;
            fronted = 0;
            look(seed);
            for (int step = 1; step < size; step++) {
                int chosen = -1;
                double least = Double.POSITIVE_INFINITY;
                int kept = 0;
                for (int index = 0; index < fronted; index++) {
                    int cell = front[index];
                    if (owner[cell] >= 0) {
                        inFront[cell] = false;
                        continue;
                    }
                    front[kept++] = cell;
                    double cost = nearest[cell] + COMPACT * Math.hypot(x[cell] - x[seed], y[cell] - y[seed]);
                    if (cost < least) {
                        least = cost;
                        chosen = cell;
                    }
                }
                fronted = kept;
                if (chosen < 0) chosen = stray(seed, group, step);
                group[step] = chosen;
                owner[chosen] = cluster;
                link[2 * step - 2] = anchor[chosen];
                link[2 * step - 1] = chosen;
                look(chosen);
            }
            for (int index = 0; index < fronted; index++) inFront[front[index]] = false;
            fronted = 0;
            return group;
        }

        /**
         * La première place d'un amas : la place libre la plus proche du centre qui a au moins
         * {@code wanted} voisines libres, ou à défaut la place libre la plus proche du centre.
         */
        private int seed(int wanted) {
            while (owner[from] >= 0) from++;
            for (int cell = from; cell < owner.length; cell++) {
                if (owner[cell] >= 0) continue;
                int free = 0;
                int close = grid.near(x[cell] - reachable, y[cell] - reachable, x[cell] + reachable, y[cell] + reachable);
                int[] found = grid.found();
                for (int index = 0; index < close && free < wanted; index++) {
                    int other = found[index];
                    if (other != cell && owner[other] < 0 && Math.hypot(x[other] - x[cell], y[other] - y[cell]) <= reachable) free++;
                }
                if (free >= wanted) return cell;
            }
            return from;
        }

        /** Ajoute au bord de l'amas les places libres à portée de vue de {@code member}, qui vient de le rejoindre. */
        private void look(int member) {
            int close = grid.near(x[member] - sight, y[member] - sight, x[member] + sight, y[member] + sight);
            int[] found = grid.found();
            for (int index = 0; index < close; index++) {
                int cell = found[index];
                if (owner[cell] >= 0) continue;
                double gap = Math.hypot(x[cell] - x[member], y[cell] - y[member]);
                if (gap > sight) continue;
                if (!inFront[cell]) {
                    inFront[cell] = true;
                    nearest[cell] = gap;
                    anchor[cell] = member;
                    if (fronted == front.length) front = java.util.Arrays.copyOf(front, 2 * fronted);
                    front[fronted++] = cell;
                } else if (gap < nearest[cell]) {
                    nearest[cell] = gap;
                    anchor[cell] = member;
                }
            }
        }

        /**
         * Quand l'amas est cerné et qu'il lui manque encore des places : la place libre la plus proche
         * de sa première place, où qu'elle soit, rattachée à la plus proche de ses molécules.
         */
        private int stray(int seed, int[] group, int members) {
            int chosen = -1;
            double least = Double.POSITIVE_INFINITY;
            for (int cell = from; cell < owner.length; cell++) {
                if (owner[cell] >= 0) continue;
                double gap = Math.hypot(x[cell] - x[seed], y[cell] - y[seed]);
                if (gap < least) {
                    least = gap;
                    chosen = cell;
                }
            }
            nearest[chosen] = Double.POSITIVE_INFINITY;
            for (int index = 0; index < members; index++) {
                double gap = Math.hypot(x[chosen] - x[group[index]], y[chosen] - y[group[index]]);
                if (gap < nearest[chosen]) {
                    nearest[chosen] = gap;
                    anchor[chosen] = group[index];
                }
            }
            return chosen;
        }
    }
}
