package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HBlocks;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.world.LibraryGen;
import lucifertotem.world.LightfallChunkGenerator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * The Labyrinth of the Lady: a 64x64 hedge-of-stone maze on its own floating platform (one per region, like the Library),
 * built slice by slice in each populate window so no single chunk pays for it. Its heart is a walled sanctum behind a
 * Gate of Hekate; her Key lies in one of the maze's dead ends. A Red Thread shows the way.
 */
public final class Labyrinth {
    private Labyrinth() {}
    public static final int N = 21, P = 3, SIZE = N * P + 1, LY = 110, H = 4;
    public static final int S0 = 8, S1 = 12, GATE_Z = P * (S1 + 1), MID = N / 2;   // sanctum cells 8..12; gate in the wall row z = 39
    public static int slices = 0, keysPlaced = 0, guardsPlaced = 0, threadPulls = 0, lastPath = -1;
    public static long nanos = 0, worst = 0;

    // ------------------------------------------------------------ where labyrinths are
    static int region() { return Math.max(16, LTConfig.v130.labyrinthRegionChunks); }
    /** NW corner (floor level) of the labyrinth of region (rx, rz), or null */
    public static BlockPos origin(long seed, int rx, int rz) {
        if (LTConfig.v130.labyrinthRegionChunks <= 0) return null;
        int R = region();
        Random r = LightfallChunkGenerator.chunkRandom(seed, rx, rz, 0x1AB7L);
        if (r.nextInt(100) >= LTConfig.v130.labyrinthChancePercent) return null;
        int cx = rx * R + 2 + r.nextInt(R - 8), cz = rz * R + 3 + r.nextInt(R - 9);
        if (Math.abs(cx) < 8 && Math.abs(cz) < 8) return null;              // keep the world origin clear
        BlockPos o = new BlockPos(cx * 16, LY, cz * 16);
        if (LibraryGen.nearest(seed, center(o), 120) != null) return null;   // never on top of a Library
        return o;
    }
    public static BlockPos center(BlockPos o) { return o.add(P * MID + 1, 0, P * MID + 1); }
    public static BlockPos originOf(BlockPos center) { return center.add(-(P * MID + 1), 0, -(P * MID + 1)); }
    public static BlockPos entrance(BlockPos o) { return o.add(P * MID + 1, 0, -3); }
    public static BlockPos nearest(long seed, BlockPos from, int radius) {
        if (LTConfig.v130.labyrinthRegionChunks <= 0) return null;
        int R = region() * 16;
        BlockPos best = null; double bd = (double) radius * radius;
        for (int rx = Math.floorDiv(from.getX() - radius, R); rx <= Math.floorDiv(from.getX() + radius, R); rx++)
            for (int rz = Math.floorDiv(from.getZ() - radius, R); rz <= Math.floorDiv(from.getZ() + radius, R); rz++) {
                BlockPos o = origin(seed, rx, rz);
                if (o == null) continue;
                BlockPos c = center(o);
                double d = sq(c.getX() - from.getX()) + sq(c.getZ() - from.getZ());
                if (d <= bd) { bd = d; best = c; }
            }
        return best;
    }
    static double sq(double v) { return v * v; }

    // ------------------------------------------------------------ the maze (deterministic, cached)
    public static final class Maze {
        public final boolean[][] wall = new boolean[SIZE][SIZE];
        public final List<int[]> deadEnds = new ArrayList<>();
        public boolean open(int i, int j, int di, int dj) {          // can you walk from cell (i,j) to (i+di, j+dj)?
            int ni = i + di, nj = j + dj;
            if (ni < 0 || nj < 0 || ni >= N || nj >= N) return false;
            if (di != 0) return !wall[P * Math.max(i, ni)][P * j + 1];
            return !wall[P * i + 1][P * Math.max(j, nj)];
        }
    }
    public static boolean sanctum(int i, int j) { return i >= S0 && i <= S1 && j >= S0 && j <= S1; }
    private static final Map<Long, Maze> CACHE = Collections.synchronizedMap(new LinkedHashMap<Long, Maze>(8, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Maze> e) { return size() > 6; }
    });
    public static Maze maze(long seed, BlockPos o) {
        long key = o.toLong() * 31 + seed;
        Maze m = CACHE.get(key);
        if (m != null) return m;
        m = new Maze();
        boolean[][] wl = m.wall;
        for (boolean[] row : wl) Arrays.fill(row, true);
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) for (int a = 1; a <= 2; a++) for (int b = 1; b <= 2; b++) wl[P * i + a][P * j + b] = false;
        for (int x = P * S0 + 1; x <= P * S1 + 2; x++) for (int z = P * S0 + 1; z <= P * S1 + 2; z++) wl[x][z] = false;
        Random r = new Random(seed ^ (o.toLong() * 0x9E3779B97F4A7C15L) ^ 0x1AB7L);
        boolean[][] vis = new boolean[N][N];
        for (int i = S0; i <= S1; i++) for (int j = S0; j <= S1; j++) vis[i][j] = true;
        Deque<int[]> st = new ArrayDeque<>();
        st.push(new int[]{MID, 0}); vis[MID][0] = true;
        int[][] D = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!st.isEmpty()) {
            int[] c = st.peek();
            List<int[]> nb = new ArrayList<>();
            for (int[] d : D) { int ni = c[0] + d[0], nj = c[1] + d[1]; if (ni >= 0 && nj >= 0 && ni < N && nj < N && !vis[ni][nj]) nb.add(d); }
            if (nb.isEmpty()) { st.pop(); continue; }
            int[] d = nb.get(r.nextInt(nb.size()));
            carve(wl, c[0], c[1], d[0], d[1]);
            vis[c[0] + d[0]][c[1] + d[1]] = true;
            st.push(new int[]{c[0] + d[0], c[1] + d[1]});
        }
        for (int k = 0; k < 24; k++) {                              // a few loops so it is not a single long corridor
            int i = r.nextInt(N - 1), j = r.nextInt(N - 1); int[] d = D[r.nextInt(2) * 2];
            if (d[0] != 0 && (sanctum(i, j) || sanctum(i + 1, j))) continue;
            if (d[1] != 0 && (sanctum(i, j) || sanctum(i, j + 1))) continue;
            if (d[0] == 1) carve(wl, i, j, 1, 0); else carve(wl, i, j, 0, 1);
        }
        wl[P * MID + 1][0] = wl[P * MID + 2][0] = false;          // the entrance (north)
        wl[P * MID + 1][GATE_Z] = wl[P * MID + 2][GATE_Z] = false; // the sanctum gate (south side of the sanctum)
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
            if (sanctum(i, j) || (i == MID && j == 0)) continue;
            int open = 0; for (int[] d : D) if (m.open(i, j, d[0], d[1])) open++;
            if (open == 1) m.deadEnds.add(new int[]{i, j});
        }
        Collections.shuffle(m.deadEnds, new Random(seed ^ o.toLong() ^ 0xDEADL));
        CACHE.put(key, m);
        return m;
    }
    static void carve(boolean[][] wl, int i, int j, int di, int dj) {
        if (di != 0) { int x = P * Math.max(i, i + di); wl[x][P * j + 1] = wl[x][P * j + 2] = false; }
        else { int z = P * Math.max(j, j + dj); wl[P * i + 1][z] = wl[P * i + 2][z] = false; }
    }
    /** cells from (i,j) to the cell just outside the sanctum gate, by breadth-first search (empty if unreachable) */
    public static List<int[]> path(Maze m, int si, int sj) {
        int[][] prev = new int[N * N][]; boolean[] seen = new boolean[N * N];
        Deque<int[]> q = new ArrayDeque<>(); q.add(new int[]{si, sj}); seen[si * N + sj] = true;
        int[][] D = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int gi = MID, gj = S1 + 1;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] == gi && c[1] == gj) {
                LinkedList<int[]> out = new LinkedList<>();
                for (int[] k = c; k != null; k = prev[k[0] * N + k[1]]) out.addFirst(k);
                return out;
            }
            for (int[] d : D) {
                int ni = c[0] + d[0], nj = c[1] + d[1];
                if (!m.open(c[0], c[1], d[0], d[1]) || seen[ni * N + nj] || sanctum(ni, nj)) continue;
                seen[ni * N + nj] = true; prev[ni * N + nj] = c; q.add(new int[]{ni, nj});
            }
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------ building, one populate window at a time
    public static void populate(World w, long seed, int bx, int bz) {
        if (LTConfig.v130.labyrinthRegionChunks <= 0) return;
        int R = region() * 16, pad = SIZE + 24;
        for (int rx = Math.floorDiv(bx - pad, R); rx <= Math.floorDiv(bx + 16 + pad, R); rx++)
            for (int rz = Math.floorDiv(bz - pad, R); rz <= Math.floorDiv(bz + 16 + pad, R); rz++) {
                BlockPos o = origin(seed, rx, rz);
                if (o == null) continue;
                if (bx + 15 < o.getX() || bx > o.getX() + SIZE - 1 || bz + 15 < o.getZ() - 6 || bz > o.getZ() + SIZE - 1) continue;
                long t0 = System.nanoTime();
                slice(w, seed, o, bx, bz);
                long dt = System.nanoTime() - t0; nanos += dt; worst = Math.max(worst, dt);
                slices++;
            }
    }

    public static void slice(World w, long seed, BlockPos o, int bx, int bz) {
        Gen130 g = new Gen130(w, new Random(seed ^ o.toLong() ^ (bx * 341873128712L + bz * 132897987541L)), bx, bz);
        Maze m = maze(seed, o);
        int ox = o.getX(), oz = o.getZ();
        IBlockState B = g.BRICK(), ST = g.STONE(), Q = Gen130.QUARTZ(), PU = Blocks.PURPUR_BLOCK.getDefaultState(), AIR = Gen130.AIR;
        IBlockState CR = ModContent.CRACKED_DUSKSTONE_BRICKS != null ? ModContent.CRACKED_DUSKSTONE_BRICKS.getDefaultState() : B;
        int x0 = Math.max(bx, ox), x1 = Math.min(bx + 15, ox + SIZE - 1), z0 = Math.max(bz, oz), z1 = Math.min(bz + 15, oz + SIZE - 1);
        Raw raw = new Raw(w);
        List<int[]> lanterns = new ArrayList<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int lx = x - ox, lz = z - oz;
            boolean inner = lx >= P * S0 + 1 && lx <= P * S1 + 2 && lz >= P * S0 + 1 && lz <= P * S1 + 2;
            long h = (x * 3129871L) ^ (z * 116129781L);
            raw.set(x, LY - 2, z, ST);
            raw.set(x, LY - 1, z, inner ? (((lx + lz) & 1) == 0 ? Q : PU) : ((h & 7) == 0 ? CR : B));
            if (m.wall[lx][lz]) {
                for (int y = LY; y < LY + H; y++) raw.set(x, y, z, (h >> 5 & 15) == 0 ? CR : B);
                boolean post = lx % P == 0 && lz % P == 0;
                if (post && lx % 6 == 0 && lz % 6 == 0) lanterns.add(new int[]{x, z});
                for (int y = LY + H; y <= LY + 6; y++) raw.set(x, y, z, AIR);
            } else for (int y = LY; y <= LY + 6; y++) raw.set(x, y, z, AIR);
        }
        raw.finish();
        for (int[] k : lanterns) g.set(k[0], LY + 2, k[1], Blocks.SEA_LANTERN.getDefaultState());
        // the porch north of the entrance, with a ladder shaft down to the ground and the entrance chest (the Red Thread)
        BlockPos e = entrance(o);
        int ex = e.getX(), ez = e.getZ();
        for (int x = ex - 3; x <= ex + 4; x++) for (int z = ez - 2; z <= oz - 1; z++) {
            if (!g.in(x, LY, z)) continue;
            g.set(x, LY - 2, z, ST); g.set(x, LY - 1, z, B);
            for (int y = LY; y <= LY + 5; y++) g.set(x, y, z, AIR);
        }
        for (int x : new int[]{ex - 3, ex + 4}) { g.set(x, LY, ez - 2, B); g.set(x, LY + 1, ez - 2, B); g.torch(x, LY + 2, ez - 2, EnumFacing.UP); }
        g.chest(ex - 2, LY, ez - 1, EnumFacing.EAST, Stage5.LOOT_LAB_ENTRANCE);
        int sx = ex + 3, sz = ez - 1;
        if (g.in(sx, LY, sz) && g.in(sx + 1, LY, sz) && g.in(sx, LY, sz - 1) && g.in(sx, LY, sz + 1)) {
            int gy = -1;
            for (int y = LY - 3; y > 2; y--) { IBlockState s = w.getBlockState(new BlockPos(sx, y, sz)); if (s.getMaterial().isSolid()) { gy = y; break; } }
            int bottom = gy > 0 ? gy + 1 : LY - 24;
            for (int y = bottom; y <= LY - 1; y++) {
                g.set(sx, y, sz, Blocks.LADDER.getDefaultState().withProperty(net.minecraft.block.BlockLadder.FACING, EnumFacing.WEST));
                g.set(sx + 1, y, sz, B);
                if (y < LY - 2) { g.set(sx - 1, y, sz, B); g.set(sx, y, sz - 1, B); g.set(sx, y, sz + 1, B); }
            }
            for (int y = bottom; y < LY - 2; y++) if ((y - bottom) % 8 == 4) g.set(sx - 1, y, sz, Blocks.GLOWSTONE.getDefaultState());
            g.set(sx + 1, LY, sz, B);
            g.set(sx, LY, sz, Blocks.LADDER.getDefaultState().withProperty(net.minecraft.block.BlockLadder.FACING, EnumFacing.WEST));
        }
        // the sanctum gate (a Gate of Hekate, 2 wide, 3 high)
        for (int x = ox + P * MID + 1; x <= ox + P * MID + 2; x++) {
            for (int y = LY; y < LY + 3; y++) g.set(x, y, oz + GATE_Z, Stage3.HEKATE_GATE.getDefaultState());
            g.set(x, LY + 3, oz + GATE_Z, B);
        }
        // the heart: an altar with a light, the treasure, and two of the Lady's paintings
        BlockPos c = center(o);
        int cx = c.getX(), cz = c.getZ();
        g.fill(cx - 1, LY, cz - 1, cx, LY, cz, Gen130.QPILLAR());
        g.fill(cx - 1, LY + 1, cz - 1, cx, LY + 1, cz, Blocks.STONE_SLAB.getStateFromMeta(7));
        g.set(cx - 1, LY + 2, cz - 1, ModContent.GLOW_CRYSTAL.getDefaultState());
        g.chest(cx - 3, LY, oz + P * S0 + 1, EnumFacing.SOUTH, Stage5.LOOT_LABYRINTH);
        g.chest(cx + 2, LY, oz + P * S0 + 1, EnumFacing.SOUTH, Stage5.LOOT_LABYRINTH);
        g.painting(cx - 1, LY + 1, oz + P * S0 + 1, EnumFacing.SOUTH, "labyrinth");
        g.painting(ox + P * S0 + 1, LY + 1, cz - 1, EnumFacing.EAST, "moon_goddess");
        g.painting(ox + P * S1 + 2, LY + 1, cz, EnumFacing.WEST, "hekate");
        for (int[] k : new int[][]{{P * S0 + 1, P * S0 + 1}, {P * S1 + 2, P * S0 + 1}, {P * S0 + 1, P * S1 + 2}, {P * S1 + 2, P * S1 + 2}})
            g.set(ox + k[0], LY, oz + k[1], Blocks.SEA_LANTERN.getDefaultState());
        if (g.in(cx, LY, cz)) StructureIndex.record(w, "labyrinth", new BlockPos(cx, LY, cz));
        // dead ends: the Key of Hekate in the first, treasure in the next three, guard hounds in the three after
        for (int k = 0; k < Math.min(7, m.deadEnds.size()); k++) {
            int[] d = m.deadEnds.get(k);
            int x = ox + P * d[0] + 1, z = oz + P * d[1] + 1;
            if (k == 0) {
                if (!g.in(x, LY, z)) continue;
                BlockPos p = new BlockPos(x, LY, z);
                w.setBlockState(p, Blocks.CHEST.getDefaultState(), 2);
                TileEntity te = w.getTileEntity(p);
                if (te instanceof TileEntityChest) {
                    TileEntityChest ch = (TileEntityChest) te;
                    ch.setInventorySlotContents(13, new ItemStack(Stage3.KEY_OF_HEKATE));
                    ch.setInventorySlotContents(4, new ItemStack(Items.STRING, 3));
                    ch.setInventorySlotContents(22, new ItemStack(Items.BREAD, 2));
                    keysPlaced++;
                }
            } else if (k < 4) g.chest(x, LY, z, EnumFacing.NORTH, Stage5.LOOT_LABYRINTH);
            else {
                EntityHekateHound hd = g.mob(EntityHekateHound::new, x + 1.0, LY, z + 1.0);
                if (hd != null) { hd.guard = true; hd.setHomePosAndDistance(new BlockPos(x, LY, z), 10); guardsPlaced++; }
            }
        }
    }
    /** bulk writes straight into the chunk sections (no per-block light updates); the touched chunks re-light their sky columns once at the end */
    static final class Raw {
        final World w; net.minecraft.world.chunk.Chunk ch; int ccx = Integer.MIN_VALUE, ccz; final Set<net.minecraft.world.chunk.Chunk> touched = new HashSet<>();
        Raw(World w) { this.w = w; }
        void set(int x, int y, int z, IBlockState s) {
            int cx = x >> 4, cz = z >> 4;
            if (ch == null || cx != ccx || cz != ccz) { ch = w.getChunk(cx, cz); ccx = cx; ccz = cz; touched.add(ch); }
            net.minecraft.world.chunk.storage.ExtendedBlockStorage[] arr = ch.getBlockStorageArray();
            net.minecraft.world.chunk.storage.ExtendedBlockStorage es = arr[y >> 4];
            if (es == net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE) {
                if (s.getBlock() == Blocks.AIR) return;
                es = new net.minecraft.world.chunk.storage.ExtendedBlockStorage(y >> 4 << 4, w.provider.hasSkyLight());
                arr[y >> 4] = es;
            }
            IBlockState old = es.get(x & 15, y & 15, z & 15);
            if (old == s) return;
            if (old.getBlock().hasTileEntity(old)) { w.setBlockState(new BlockPos(x, y, z), s, 2); return; }
            es.set(x & 15, y & 15, z & 15, s);
        }
        void finish() { for (net.minecraft.world.chunk.Chunk c : touched) { c.generateSkylightMap(); c.markDirty(); } }
    }

    // ------------------------------------------------------------ the Red Thread and the heart
    /** "far" / "none" / "outside" / "heart" / "path" */
    public static String guide(EntityPlayer p) {
        World w = p.world;
        if (w.isRemote) return "client";
        threadPulls++;
        if (w.provider.getDimension() != LuciferTotem.dimId) { HBlocks.msg(p, "msg.lucifertotem.thread.far"); return "far"; }
        BlockPos c = nearest(w.getSeed(), p.getPosition(), 1200);
        if (c == null) { HBlocks.msg(p, "msg.lucifertotem.thread.none"); return "none"; }
        BlockPos o = originOf(c);
        int lx = (int) Math.floor(p.posX) - o.getX(), lz = (int) Math.floor(p.posZ) - o.getZ();
        boolean inside = lx >= 0 && lz >= 0 && lx < SIZE && lz < SIZE && p.posY >= LY - 1 && p.posY <= LY + 6;
        if (!inside) {
            BlockPos e = entrance(o);
            double dx = e.getX() + 0.5 - p.posX, dz = e.getZ() + 0.5 - p.posZ;
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            HBlocks.msg(p, "msg.lucifertotem.thread.outside", new TextComponentTranslation("dir.lucifertotem." + Stage4.compass(dx, dz)), dist, LY);
            return "outside";
        }
        int i = Math.min(N - 1, Math.max(0, (lx - 1) / P)), j = Math.min(N - 1, Math.max(0, (lz - 1) / P));
        if (sanctum(i, j)) { HBlocks.msg(p, "msg.lucifertotem.thread.heart"); return "heart"; }
        List<int[]> path = path(maze(w.getSeed(), o), i, j);
        lastPath = path.size();
        if (path.isEmpty()) { HBlocks.msg(p, "msg.lucifertotem.thread.lost"); return "lost"; }
        if (p instanceof EntityPlayerMP && w instanceof WorldServer) {
            int n = Math.min(path.size(), 12);
            for (int k = 1; k < n; k++) {
                int[] a = path.get(k - 1), b = path.get(k);
                for (int s = 0; s < 3; s++) {
                    double t = s / 3.0;
                    double x = o.getX() + P * (a[0] + (b[0] - a[0]) * t) + 2.0, z = o.getZ() + P * (a[1] + (b[1] - a[1]) * t) + 2.0;
                    ((WorldServer) w).spawnParticle((EntityPlayerMP) p, EnumParticleTypes.REDSTONE, true, x, LY + 0.3, z, 0, 1.0, 0.05, 0.05, 1.0);
                }
            }
        }
        HBlocks.msg(p, "msg.lucifertotem.thread.path", path.size());
        return "path";
    }
    /** is the player standing in a labyrinth's heart? (grants the advancement) */
    public static boolean checkHeart(EntityPlayer p) {
        if (p.posY < LY - 1 || p.posY > LY + 6) return false;
        BlockPos c = nearest(p.world.getSeed(), p.getPosition(), 48);
        if (c == null) return false;
        if (Math.abs(p.posX - c.getX()) > 7 || Math.abs(p.posZ - c.getZ()) > 7) return false;
        return HAdv.grant(p, "labyrinth") || HAdv.has(p, "labyrinth");
    }
}
