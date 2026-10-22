package lucifertotem.world;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The weird, glowing violet sea. Everything is placed inside the populate window [bx..bx+15]x[bz..bz+15]
 * (bounds-checked), and only into water cells / the sea floor, so islands are never carved and no
 * neighbouring chunk is ever forced to generate.
 *
 * Flora/fauna-ish: tendril stalks with glowing bulbs, dusk coral, starfish, crystal reefs, blinking watcher-eye
 * boulders, upside-down trees hanging from island undersides into the water.
 * Set pieces: floating half-submerged halos, whirlpool spirals, bone reefs, fallen-angel statues, the rare
 * skeleton of a giant winged beast, and the Drowned Choir (sunken chapel with a guardian spawner).
 */
public class LightfallSea {
    public static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();
    public static int outOfWindowWrites = 0;
    static final int SEA = LightfallChunkGenerator.SEA_LEVEL;
    static final IBlockState WATER = Blocks.WATER.getDefaultState();
    static final IBlockState BONE = Blocks.BONE_BLOCK.getDefaultState();

    private final World w; private final Random r; private final int bx, bz;
    private LightfallSea(World w, Random r, int bx, int bz) { this.w = w; this.r = r; this.bx = bx; this.bz = bz; }

    private boolean in(int x, int y, int z) { return x >= bx && x < bx + 16 && z >= bz && z < bz + 16 && y > 0 && y < 255; }
    private IBlockState get(int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)); }
    private boolean wet(int x, int y, int z) { return in(x, y, z) && get(x, y, z).getMaterial() == Material.WATER; }
    private boolean open(int x, int y, int z) { Material m = get(x, y, z).getMaterial(); return m == Material.WATER || m == Material.AIR; }

    private void set(int x, int y, int z, IBlockState s) {
        if (!in(x, y, z)) { outOfWindowWrites++; return; }
        w.setBlockState(new BlockPos(x, y, z), s, 2);
    }
    /** watery crystal coral in water, the dry glow crystal in air */
    private IBlockState crystalAt(int x, int y, int z) { return in(x, y, z) && get(x, y, z).getMaterial() == Material.WATER ? ModContent.CRYSTAL_CORAL.getDefaultState() : ModContent.GLOW_CRYSTAL.getDefaultState(); }
    /** only replace water/air (never island blocks) */
    private void setOpen(int x, int y, int z, IBlockState s) { if (in(x, y, z) && open(x, y, z)) set(x, y, z, s); }

    /** y of the solid sea floor under a water column at x,z (column must be water at SEA); -1 otherwise */
    int floor(int x, int z) {
        if (!wet(x, SEA, z)) return -1;
        int y = SEA;
        while (y > 1 && get(x, y, z).getMaterial() == Material.WATER) y--;
        return y;
    }

    public static void populate(World w, Random r, int bx, int bz) {
        LightfallSea s = new LightfallSea(w, r, bx, bz);
        s.decorate();
        int roll = r.nextInt(1000);
        String made = null;
        LTConfig.Sea c = LTConfig.sea;
        int t = c.drownedChoir;
        if (roll < t) { if (s.choir()) made = "drowned_choir"; }
        else if (roll < (t += c.giantSkeleton)) { if (s.giantSkeleton()) made = "giant_winged_skeleton"; }
        else if (roll < (t += c.angelStatue)) { if (s.statue()) made = "fallen_angel_statue"; }
        else if (roll < (t += c.floatingHalo)) { if (s.halo()) made = "floating_halo"; }
        else if (roll < (t += c.whirlpool)) { if (s.whirlpool()) made = "whirlpool_spiral"; }
        else if (roll < (t += c.boneReef)) { if (s.boneReef()) made = "bone_reef"; }
        if (made != null) {
            COUNTS.merge(made, 1, Integer::sum);
            if (LTConfig.testHarness) LuciferTotem.LOG.info("[LT][GEN] {} at window {},{}", made, bx, bz);
        }
    }

    // ------------------------------------------------------------------ small things
    private void decorate() {
        // tendril stalks (tall, glowing bulbs)
        for (int i = 0; i < 6; i++) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16), f = floor(x, z);
            if (f < 0) continue;
            tendril(x, f + 1, z, 3 + r.nextInt(11));
        }
        for (int i = 0; i < 7; i++) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16), f = floor(x, z);
            if (f >= 0 && f < SEA - 1) set(x, f + 1, z, ModContent.DUSK_CORAL.getDefaultState());
        }
        for (int i = 0; i < 4; i++) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16), f = floor(x, z);
            if (f >= 0 && f < SEA - 1) set(x, f + 1, z, ModContent.STARFISH.getDefaultState());
        }
        if (r.nextInt(3) == 0) reef(bx + 3 + r.nextInt(10), bz + 3 + r.nextInt(10));
        if (r.nextInt(7) == 0) watcherBoulder(bx + 4 + r.nextInt(8), bz + 4 + r.nextInt(8));
        if (r.nextInt(3) == 0) hangingTree(bx + 2 + r.nextInt(12), bz + 2 + r.nextInt(12));
    }

    private void tendril(int x, int y, int z, int h) {
        int top = Math.min(y + h - 1, SEA - 1);
        if (top <= y) return;
        for (int yy = y; yy < top; yy++) { if (!wet(x, yy, z)) return; set(x, yy, z, ModContent.TENDRIL.getDefaultState()); }
        if (wet(x, top, z)) set(x, top, z, ModContent.TENDRIL_BULB.getDefaultState());
    }

    private void reef(int cx, int cz) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx * dx + dz * dz > 10 || r.nextInt(3) == 0) continue;
            int x = cx + dx, z = cz + dz, f = floor(x, z);
            if (f < 0 || f >= SEA - 3) continue;
            int n = r.nextInt(6);
            if (n == 0) { set(x, f + 1, z, crystalAt(x, f + 1, z)); }
            else if (n == 1) { set(x, f + 1, z, Blocks.PURPUR_BLOCK.getDefaultState()); if (wet(x, f + 2, z)) set(x, f + 2, z, crystalAt(x, f + 2, z)); }
            else if (n == 2) { set(x, f + 1, z, Blocks.PRISMARINE.getDefaultState()); if (wet(x, f + 2, z)) set(x, f + 2, z, ModContent.DUSK_CORAL.getDefaultState()); }
            else set(x, f + 1, z, ModContent.DUSK_CORAL.getDefaultState());
        }
    }

    private void watcherBoulder(int cx, int cz) {
        int f = floor(cx, cz);
        if (f < 0 || f > SEA - 6) return;
        int rad = 2 + r.nextInt(2);
        for (int dx = -rad; dx <= rad; dx++) for (int dy = -1; dy <= rad; dy++) for (int dz = -rad; dz <= rad; dz++)
            if (dx * dx + dy * dy * 1.4 + dz * dz <= rad * rad + 0.5) setOpen(cx + dx, f + 1 + dy, cz + dz, ModContent.DUSKSTONE.getDefaultState());
        int eyes = 1 + r.nextInt(3);
        int[][] dirs = {{rad, 1, 0}, {-rad, 1, 0}, {0, 1, rad}, {0, 1, -rad}, {0, rad + 1, 0}};
        for (int i = 0; i < eyes; i++) { int[] d = dirs[r.nextInt(dirs.length)]; set(cx + d[0], f + d[1], cz + d[2], ModContent.WATCHER.getDefaultState()); }
    }

    private void hangingTree(int x, int z) {
        // find an island underside just above the sea
        int y = SEA + 1;
        while (y < SEA + 48 && open(x, y, z)) y++;
        if (y >= SEA + 48 || !(get(x, y, z).getBlock() == ModContent.DUSKSTONE)) return;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) if (!open(x + dx, y - 1, z + dz)) return;
        IBlockState log = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH).withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y);
        IBlockState leaf = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.OAK).withProperty(BlockLeaves.DECAYABLE, false).withProperty(BlockLeaves.CHECK_DECAY, false);
        int end = Math.max(SEA - 4, y - 1 - (5 + r.nextInt(6)));
        for (int yy = y - 1; yy >= end; yy--) setOpen(x, yy, z, log);
        for (int dy = 0; dy <= 3; dy++) {
            int rad = dy == 0 || dy == 3 ? 1 : 2;
            for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
                if (Math.abs(dx) == rad && Math.abs(dz) == rad && r.nextBoolean()) continue;
                setOpen(x + dx, end + 1 - dy, z + dz, leaf);
            }
        }
        setOpen(x, end - 3, z, crystalAt(x, end - 3, z));
        COUNTS.merge("upside_down_tree", 1, Integer::sum);
    }

    // ------------------------------------------------------------------ set pieces
    private int rot;
    private int cx0, cz0;
    private int wx(int lx, int lz) { switch (rot) { case 1: return cx0 - lz; case 2: return cx0 - lx; case 3: return cx0 + lz; default: return cx0 + lx; } }
    private int wz(int lx, int lz) { switch (rot) { case 1: return cz0 + lx; case 2: return cz0 - lz; case 3: return cz0 - lx; default: return cz0 + lz; } }
    private void lset(int lx, int y, int lz, IBlockState s) { set(wx(lx, lz), y, wz(lx, lz), s); }
    private void lsetOpen(int lx, int y, int lz, IBlockState s) { setOpen(wx(lx, lz), y, wz(lx, lz), s); }

    /** centre-of-window water site with floor at least `depth` blocks below sea level; returns floor y or -1 */
    private int seaSite(int half, int depth) {
        cx0 = bx + 8; cz0 = bz + 8; rot = r.nextInt(4);
        int f = floor(cx0, cz0);
        if (f < 0 || SEA - f < depth) return -1;
        int[][] c = {{-half, -half}, {half, -half}, {-half, half}, {half, half}};
        for (int[] k : c) { int g = floor(cx0 + k[0], cz0 + k[1]); if (g < 0 || Math.abs(g - f) > 4) return -1; }
        return f;
    }

    private boolean halo() {
        cx0 = bx + 8; cz0 = bz + 8;
        if (!wet(cx0, SEA, cz0)) return false;
        boolean alongX = r.nextBoolean();
        double rad = 4.5 + r.nextDouble();
        int gap = r.nextInt(3) == 0 ? r.nextInt(360) : -1000;
        for (int a = 0; a < 360; a += 3) {
            if (Math.abs(a - gap) < 35) continue;
            double rr = Math.toRadians(a);
            int u = (int) Math.round(Math.cos(rr) * rad), v = (int) Math.round(Math.sin(rr) * rad);
            int x = alongX ? cx0 + u : cx0, z = alongX ? cz0 : cz0 + u, y = SEA + v;
            setOpen(x, y, z, ModContent.HALO_BRASS.getDefaultState());
        }
        return true;
    }

    private boolean whirlpool() {
        int f = seaSite(7, 8);
        if (f < 0) return false;
        for (int arm = 0; arm < 3; arm++)
            for (double t = 0.6; t < 7.2; t += 0.15) {
                double a = t * 0.9 + arm * (Math.PI * 2 / 3);
                int x = cx0 + (int) Math.round(Math.cos(a) * t), z = cz0 + (int) Math.round(Math.sin(a) * t);
                int g = floor(x, z);
                if (g < 0) continue;
                if (((int) (t * 3)) % 4 == 0) { if (wet(x, g + 1, z)) set(x, g + 1, z, crystalAt(x, g + 1, z)); set(x, g, z, ModContent.DUSKSTONE_BRICKS.getDefaultState()); }
                else set(x, g, z, ModContent.DUSKSTONE_BRICKS.getDefaultState());
                if (((int) (t * 3)) % 5 == 0 && wet(x, g + 1, z)) set(x, g + 1, z, ModContent.DUSK_CORAL.getDefaultState());
            }
        // the eye of the vortex: a pit with a tall bulb-crowned tendril
        for (int d = 0; d <= 2; d++) set(cx0, f - d, cz0, WATER);
        set(cx0, f - 3, cz0, ModContent.WATCHER.getDefaultState());
        tendril(cx0 + 1, floor(cx0 + 1, cz0) + 1, cz0, 14);
        tendril(cx0 - 1, floor(cx0 - 1, cz0) + 1, cz0, 12);
        return true;
    }

    private void ribArch(int lx, int y0, int halfW, int h) {
        for (int lz = -halfW; lz <= halfW; lz++) {
            int a = Math.abs(lz);
            if (a == halfW) for (int y = 1; y <= h - 1; y++) lsetOpen(lx, y0 + y, lz, BONE);
            else if (a >= halfW - 1) lsetOpen(lx, y0 + h, lz, BONE);
            else lsetOpen(lx, y0 + h + 1, lz, BONE);
        }
    }

    private boolean boneReef() {
        int f = seaSite(5, 8);
        if (f < 0) return false;
        int n = 2 + r.nextInt(3);
        for (int i = 0; i < n; i++) ribArch(-4 + i * 2, f, 3, 3 + r.nextInt(2));
        for (int lx = -5; lx <= 4; lx++) lsetOpen(lx, f + 1, 0, BONE);
        for (int i = 0; i < 10; i++) { int x = cx0 - 5 + r.nextInt(11), z = cz0 - 5 + r.nextInt(11), g = floor(x, z); if (g >= 0 && g < SEA - 1) set(x, g + 1, z, r.nextBoolean() ? ModContent.DUSK_CORAL.getDefaultState() : ModContent.STARFISH.getDefaultState()); }
        return true;
    }

    private static IBlockState quartz() { return Blocks.QUARTZ_BLOCK.getDefaultState(); }
    private static IBlockState quartzPillar() { return Blocks.QUARTZ_BLOCK.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.LINES_Y); }

    private boolean statue() {
        int f = seaSite(6, 10);
        if (f < 0) return false;
        int y0 = f;
        for (int lx = -2; lx <= 2; lx++) for (int lz = -2; lz <= 2; lz++) { lset(lx, y0 + 1, lz, ModContent.DUSKSTONE_BRICKS.getDefaultState()); lset(lx, y0, lz, ModContent.DUSKSTONE.getDefaultState()); }
        for (int lx = -1; lx <= 1; lx++) for (int lz = -1; lz <= 1; lz++) lset(lx, y0 + 2, lz, Blocks.PURPUR_BLOCK.getDefaultState());
        // legs, robe, torso, arms raised in a fall
        for (int y = 3; y <= 5; y++) { lsetOpen(0, y0 + y, -1, quartzPillar()); lsetOpen(0, y0 + y, 1, quartzPillar()); lsetOpen(0, y0 + y, 0, quartz()); }
        for (int y = 6; y <= 9; y++) for (int lz = -1; lz <= 1; lz++) lsetOpen(0, y0 + y, lz, quartz());
        for (int y = 8; y <= 11; y++) { lsetOpen(0, y0 + y, -2, quartzPillar()); if (y < 11 || r.nextBoolean()) lsetOpen(0, y0 + y, 2, quartzPillar()); }
        boolean headless = r.nextInt(3) == 0;
        if (!headless) { lsetOpen(0, y0 + 10, 0, quartz()); lsetOpen(0, y0 + 11, 0, quartz()); }
        // great wings of bone-white feathers behind
        for (int lz = -6; lz <= 6; lz++) {
            int a = Math.abs(lz);
            if (a < 2) continue;
            int lo = y0 + 6 + (a - 2) / 2, hi = y0 + 12 - (a > 4 ? a - 4 : 0);
            boolean broken = lz > 0 && r.nextInt(4) == 0;
            for (int y = lo; y <= (broken ? lo + 1 : hi); y++) lsetOpen(-1, y, lz, BONE);
        }
        // halo: floating above the head, or fallen and half-buried beside the statue
        if (!headless) {
            int hy = y0 + 13;
            for (int[] k : new int[][] {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}}) lsetOpen(k[0], hy, k[1], ModContent.HALO_BRASS.getDefaultState());
        } else {
            lsetOpen(3, y0 + 1, 3, quartz()); lsetOpen(3, y0 + 2, 3, quartz()); // the fallen head
            for (int[] k : new int[][] {{2, 2}, {2, 3}, {2, 4}, {3, 2}, {3, 4}, {4, 2}, {4, 3}, {4, 4}}) {
                int x = wx(k[0], k[1]), z = wz(k[0], k[1]), g = floor(x, z);
                if (g >= 0) set(x, g + 1, z, ModContent.HALO_BRASS.getDefaultState());
            }
        }
        return true;
    }

    private void line(int lx1, int lz1, int lx2, int lz2, int y, IBlockState s) {
        int n = Math.max(Math.abs(lx2 - lx1), Math.abs(lz2 - lz1));
        for (int i = 0; i <= n; i++) {
            int lx = lx1 + Math.round((lx2 - lx1) * i / (float) Math.max(1, n)), lz = lz1 + Math.round((lz2 - lz1) * i / (float) Math.max(1, n));
            lsetOpen(lx, y, lz, s);
        }
    }

    private boolean giantSkeleton() {
        int f = seaSite(7, 9);
        if (f < 0) return false;
        int y = f + 1;
        for (int lx = -7; lx <= 5; lx++) lsetOpen(lx, y + (lx > -5 ? 1 : 0), 0, BONE);         // spine + tail
        for (int lx = -3; lx <= 3; lx += 2) ribArch(lx, y, 3, 3);                              // rib cage
        for (int lx = 5; lx <= 7; lx++) for (int dy = 0; dy <= 2; dy++) for (int lz = -1; lz <= 1; lz++) lsetOpen(lx, y + dy, lz, BONE); // skull
        lset(7, y + 1, -1, ModContent.WATCHER.getDefaultState()); lset(7, y + 1, 1, ModContent.WATCHER.getDefaultState()); // it still watches
        lset(7, y, 0, WATER);
        // wings: humerus from the shoulders, then long finger bones fanning out along the sea floor
        for (int s = -1; s <= 1; s += 2) {
            line(2, s, -1, 4 * s, y + 2, BONE);
            line(-1, 4 * s, -6, 7 * s, y + 1, BONE);
            line(-1, 4 * s, -3, 7 * s, y + 1, BONE);
            line(-1, 4 * s, 1, 7 * s, y + 1, BONE);
        }
        return true;
    }

    private boolean choir() {
        int f = seaSite(6, 11);
        if (f < 0) return false;
        int y0 = f;
        IBlockState brick = ModContent.DUSKSTONE_BRICKS.getDefaultState();
        for (int lx = -6; lx <= 6; lx++) for (int lz = -4; lz <= 4; lz++) {
            lset(lx, y0, lz, Blocks.PRISMARINE.getDefaultState());
            boolean wall = Math.abs(lx) == 6 || Math.abs(lz) == 4;
            for (int y = 1; y <= 7; y++) {
                if (wall) lset(lx, y0 + y, lz, r.nextInt(12) == 0 && y > 2 && y < 7 ? WATER : (r.nextInt(6) == 0 ? Blocks.PRISMARINE.getDefaultState().withProperty(net.minecraft.block.BlockPrismarine.VARIANT, net.minecraft.block.BlockPrismarine.EnumType.BRICKS) : brick));
                else lset(lx, y0 + y, lz, WATER);
            }
            boolean hole = lx >= 4 && Math.abs(lz) < 4 && r.nextInt(4) == 0;
            lset(lx, y0 + 8, lz, hole ? WATER : (Math.abs(lz) == 4 || Math.abs(lx) == 6 ? brick : Blocks.PRISMARINE.getDefaultState().withProperty(net.minecraft.block.BlockPrismarine.VARIANT, net.minecraft.block.BlockPrismarine.EnumType.DARK)));
        }
        // foundation so it never hangs
        for (int lx = -6; lx <= 6; lx++) for (int lz = -4; lz <= 4; lz++) for (int d = 1; d <= 6; d++) { int x = wx(lx, lz), z = wz(lx, lz); if (in(x, y0 - d, z) && open(x, y0 - d, z)) set(x, y0 - d, z, ModContent.DUSKSTONE.getDefaultState()); else break; }
        // pillars along the nave
        for (int lx = -4; lx <= 4; lx += 4) for (int lz = -3; lz <= 3; lz += 6) for (int y = 1; y <= 7; y++) lset(lx, y0 + y, lz, Blocks.PURPUR_PILLAR.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y));
        // pews with the drowned choir standing in them (bone figures)
        for (int lx = 0; lx <= 4; lx += 2) for (int lz = -3; lz <= 3; lz++) {
            if (lz == 0) continue;
            lset(lx, y0 + 1, lz, Blocks.PURPUR_SLAB.getDefaultState());
            if (r.nextInt(3) == 0) { lset(lx + 1, y0 + 1, lz, BONE); lset(lx + 1, y0 + 2, lz, BONE); }
        }
        // altar, the choir's treasure, and the guardian spawner before it
        for (int lz = -1; lz <= 1; lz++) lset(-5, y0 + 1, lz, Blocks.PURPUR_BLOCK.getDefaultState());
        int ax = wx(-5, 0), az = wz(-5, 0);
        if (in(ax, y0 + 2, az)) {
            BlockPos p = new BlockPos(ax, y0 + 2, az);
            w.setBlockState(p, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, facingToward(0)), 2);
            TileEntity te = w.getTileEntity(p);
            if (te instanceof TileEntityChest) ((TileEntityChest) te).setLootTable(LuciferTotem.LOOT_CHOIR, r.nextLong());
        }
        int sx = wx(-3, 0), sz = wz(-3, 0);
        if (in(sx, y0 + 1, sz)) {
            BlockPos p = new BlockPos(sx, y0 + 1, sz);
            w.setBlockState(p, Blocks.MOB_SPAWNER.getDefaultState(), 2);
            TileEntity te = w.getTileEntity(p);
            if (te instanceof TileEntityMobSpawner) ((TileEntityMobSpawner) te).getSpawnerBaseLogic().setEntityId(new ResourceLocation("minecraft:guardian"));
        }
        // the door (east end, local +x)
        for (int y = 1; y <= 3; y++) lset(6, y0 + y, 0, WATER);
        return true;
    }

    /** chest facing local +x direction after rotation */
    private EnumFacing facingToward(int unused) {
        switch (rot) { case 1: return EnumFacing.SOUTH; case 2: return EnumFacing.WEST; case 3: return EnumFacing.NORTH; default: return EnumFacing.EAST; }
    }
}
