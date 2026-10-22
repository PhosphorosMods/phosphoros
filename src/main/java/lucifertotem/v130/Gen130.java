package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.paint.EntityLTPainting;
import lucifertotem.paint.Paintings;
import lucifertotem.world.LightfallChunkGenerator;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 1.3.0 Lightfall structures. Each chunk gets ONE extra roll (salt 0x130B, see StructureIndex.roll130) so the
 * 1.0/1.1 rolls are untouched; every structure fits the 16x16 populate window and every write is clipped to it.
 */
public final class Gen130 {
    public static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();
    public static int outOfWindowWrites = 0, paintings = 0, mobs = 0;
    public static long nanos = 0, worst = 0;

    final World w; final Random r; final int bx, bz; BlockPos at;
    Gen130(World w, Random r, int bx, int bz) { this.w = w; this.r = r; this.bx = bx; this.bz = bz; }

    public static void structures(World w, long seed, int cx, int cz, int bx, int bz) {
        int roll = StructureIndex.roll130(seed, cx, cz);
        String type = StructureIndex.typeForRoll130(roll);
        if (type == null) return;
        build(w, type, bx, bz, LightfallChunkGenerator.chunkRandom(seed, cx, cz, 0x130CL));
    }

    /** build one 1.3.0 structure in the window bx..bx+15 (also used by the tests) */
    public static boolean build(World w, String type, int bx, int bz, Random rand) {
        long t0 = System.nanoTime();
        Gen130 g = new Gen130(w, rand, bx, bz);
        boolean ok;
        switch (type) {
            case "bacchic_fountain": ok = g.fountain(); break;
            case "crossroads_shrine": ok = g.crossroads(); break;
            case "mystery_hall": ok = Stage4.hall(g); break;
            case "on_air_studio": ok = Stage4.studio(g); break;
            case "band_camp": ok = Stage4.camp(g); break;
            case "asklepieion": ok = Stage5.asklepieion(g); break;
            default: ok = false;
        }
        long dt = System.nanoTime() - t0; nanos += dt; worst = Math.max(worst, dt);
        if (ok) {
            COUNTS.merge(type, 1, Integer::sum);
            if (g.at != null) StructureIndex.record(w, type, g.at);
            if (LTConfig.testHarness) LuciferTotem.LOG.info("[LT][GEN] {} at window {},{} ({} us)", type, bx, bz, dt / 1000);
        }
        lastAt = g.at;
        return ok;
    }
    public static BlockPos lastAt;

    // ------------------------------------------------------------ helpers (clipped to the populate window)
    public IBlockState BRICK() { return ModContent.DUSKSTONE_BRICKS.getDefaultState(); }
    public IBlockState STONE() { return ModContent.DUSKSTONE.getDefaultState(); }
    public static final IBlockState AIR = Blocks.AIR.getDefaultState();
    public static IBlockState QUARTZ() { return Blocks.QUARTZ_BLOCK.getDefaultState(); }
    public static IBlockState QPILLAR() { return Blocks.QUARTZ_BLOCK.getStateFromMeta(2); }
    public static IBlockState wool(EnumDyeColor c) { return Blocks.WOOL.getStateFromMeta(c.getMetadata()); }
    public static IBlockState carpet(EnumDyeColor c) { return Blocks.CARPET.getStateFromMeta(c.getMetadata()); }
    public static IBlockState pillar() { return Blocks.PURPUR_PILLAR.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y); }
    public static IBlockState stairs(IBlockState base, EnumFacing f) { return base.withProperty(BlockStairs.FACING, f); }

    public boolean in(int x, int y, int z) { return x >= bx && x < bx + 16 && z >= bz && z < bz + 16 && y > 0 && y < 255; }
    public void set(int x, int y, int z, IBlockState s) { if (!in(x, y, z)) { outOfWindowWrites++; return; } w.setBlockState(new BlockPos(x, y, z), s, 2); }
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
    }
    public void box(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState wall, IBlockState inside) {
        fill(x1, y1, z1, x2, y2, z2, wall);
        if (inside != null) fill(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, inside);
    }
    public IBlockState get(int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)); }
    public void chest(int x, int y, int z, EnumFacing f, ResourceLocation loot) {
        if (!in(x, y, z)) { outOfWindowWrites++; return; }
        BlockPos p = new BlockPos(x, y, z);
        w.setBlockState(p, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, f), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityChest) ((TileEntityChest) te).setLootTable(loot, r.nextLong());
    }
    public void ladder(int x, int y1, int y2, int z, EnumFacing facing) { for (int y = y1; y <= y2; y++) set(x, y, z, Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, facing)); }
    public void torch(int x, int y, int z, EnumFacing f) { set(x, y, z, Blocks.TORCH.getDefaultState().withProperty(BlockTorch.FACING, f)); }
    public void foundation(int x1, int z1, int x2, int z2, int y, int maxDepth) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++)
            for (int d = 1; d <= maxDepth; d++) {
                if (!in(x, y - d, z)) break;
                BlockPos p = new BlockPos(x, y - d, z);
                IBlockState s = w.getBlockState(p);
                if (s.getMaterial().isSolid() && s.getBlock() != Blocks.LEAVES && s.getBlock() != Blocks.LOG) break;
                w.setBlockState(p, STONE(), 2);
            }
    }
    public int surface(int x, int z) {
        int y = LightfallChunkGenerator.topY(w, x, z, LightfallChunkGenerator.SEA_LEVEL + 1);
        while (y > LightfallChunkGenerator.SEA_LEVEL) {
            IBlockState s = w.getBlockState(new BlockPos(x, y, z));
            if (s.getBlock() == ModContent.STAR_GRASS || s.getBlock() == ModContent.DUSKSTONE) return y;
            if (s.getMaterial().isSolid() && s.getBlock() != Blocks.LEAVES && s.getBlock() != Blocks.LOG) return -1;
            y--;
        }
        return -1;
    }
    public int site(int cx, int cz, int half, int tol) {
        int c = surface(cx, cz);
        if (c < 60) return -1;
        int min = c;
        int[][] pts = {{-half, -half}, {half, -half}, {-half, half}, {half, half}};
        for (int[] p : pts) { int s = surface(cx + p[0], cz + p[1]); if (s < 0 || Math.abs(s - c) > tol) return -1; min = Math.min(min, s); }
        return min;
    }
    /** an island deep enough to dig a room (height y) under the surface: all corners solid down to y */
    public boolean solidBelow(int cx, int cz, int half, int top, int bottom) {
        for (int x = cx - half; x <= cx + half; x += half) for (int z = cz - half; z <= cz + half; z += half)
            for (int y = bottom; y <= top; y += 2) if (!get(x, y, z).getMaterial().isSolid()) return false;
        return true;
    }
    /** hang a Lady Babylon painting only if it and its wall lie in the window and nothing is in the way */
    public boolean painting(int x, int y, int z, EnumFacing face, String name) {
        Paintings.Variant v = Paintings.byName(name);
        if (v == null) return false;
        EntityLTPainting e = new EntityLTPainting(w, new BlockPos(x, y, z), face, v, new Random(r.nextLong()));
        AxisAlignedBB bb = e.getEntityBoundingBox();
        for (BlockPos q : BlockPos.getAllInBox(new BlockPos(bb.minX + 0.01, bb.minY + 0.01, bb.minZ + 0.01), new BlockPos(bb.maxX - 0.01, bb.maxY - 0.01, bb.maxZ - 0.01))) {
            BlockPos back = q.offset(face.getOpposite());
            if (!in(q.getX(), q.getY(), q.getZ()) || !in(back.getX(), back.getY(), back.getZ())) return false;
            if (!w.isAirBlock(q) || !w.getBlockState(back).getMaterial().isSolid()) return false;
        }
        if (!e.onValidSurface()) return false;
        w.spawnEntity(e);
        paintings++;
        return true;
    }
    /** a persistent mob placed by the structure */
    public <T extends EntityLiving> T mob(Function<World, T> make, double x, double y, double z) {
        if (!in((int) Math.floor(x), (int) y, (int) Math.floor(z))) return null;
        T e = make.apply(w);
        e.setLocationAndAngles(x, y, z, r.nextFloat() * 360f, 0);
        e.onInitialSpawn(w.getDifficultyForLocation(new BlockPos(x, y, z)), null);
        e.enablePersistence();
        w.spawnEntity(e);
        mobs++;
        return e;
    }

    // ------------------------------------------------------------ Bacchic Fountain (stage 3)
    /** a marble basin of dark wine-water under a vine pergola; a pack of 4-6 Bacchants; an empty jukebox as a hint */
    boolean fountain() {
        int cx = bx + 8, cz = bz + 8;
        int y = site(cx, cz, 5, 2);
        if (y < 0) return false;
        foundation(cx - 5, cz - 5, cx + 5, cz + 5, y, 6);
        fill(cx - 5, y, cz - 5, cx + 5, y, cz + 5, QUARTZ());
        fill(cx - 5, y + 1, cz - 5, cx + 5, y + 6, cz + 5, AIR);
        for (int x = cx - 5; x <= cx + 5; x++) for (int z = cz - 5; z <= cz + 5; z++) if ((x + z) % 2 == 0 && Math.abs(x - cx) > 2 | Math.abs(z - cz) > 2) set(x, y, z, Blocks.PURPUR_BLOCK.getDefaultState());
        // basin: purple glass under shallow water reads as wine
        fill(cx - 2, y - 1, cz - 2, cx + 2, y - 1, cz + 2, QUARTZ());
        fill(cx - 2, y + 1, cz - 2, cx + 2, y + 1, cz + 2, Blocks.QUARTZ_BLOCK.getStateFromMeta(1));
        fill(cx - 1, y, cz - 1, cx + 1, y, cz + 1, Blocks.STAINED_GLASS.getStateFromMeta(EnumDyeColor.PURPLE.getMetadata()));
        fill(cx - 1, y + 1, cz - 1, cx + 1, y + 1, cz + 1, Blocks.WATER.getDefaultState());
        set(cx, y + 1, cz, QPILLAR()); set(cx, y + 2, cz, QPILLAR()); set(cx, y + 3, cz, Blocks.CAULDRON.getStateFromMeta(3));
        // pergola: four columns, oak beams, vine leaves
        int[][] cols = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] c : cols) fill(cx + c[0], y + 1, cz + c[1], cx + c[0], y + 4, cz + c[1], QPILLAR());
        for (int d = -4; d <= 4; d++) { set(cx + d, y + 5, cz - 4, Blocks.PLANKS.getStateFromMeta(5)); set(cx + d, y + 5, cz + 4, Blocks.PLANKS.getStateFromMeta(5)); set(cx - 4, y + 5, cz + d, Blocks.PLANKS.getStateFromMeta(5)); set(cx + 4, y + 5, cz + d, Blocks.PLANKS.getStateFromMeta(5)); }
        for (int d = -3; d <= 3; d += 2) for (int e = -3; e <= 3; e++) if (r.nextInt(3) != 0) set(cx + d, y + 5, cz + e, Blocks.LEAVES.getStateFromMeta(4));
        for (int[] c : cols) if (r.nextBoolean()) set(cx + c[0], y + 4, cz + c[1] + (c[1] < 0 ? 1 : -1), Blocks.VINE.getStateFromMeta(c[1] < 0 ? 4 : 1));
        // kline benches, a chest of the god's gifts, and the empty jukebox
        set(cx - 3, y + 1, cz, stairs(Blocks.PURPUR_STAIRS.getDefaultState(), EnumFacing.WEST)); set(cx + 3, y + 1, cz, stairs(Blocks.PURPUR_STAIRS.getDefaultState(), EnumFacing.EAST));
        chest(cx, y + 1, cz + 4, EnumFacing.NORTH, Stage3.LOOT_FOUNTAIN);
        set(cx, y + 1, cz - 4, Blocks.JUKEBOX.getDefaultState());
        set(cx - 1, y + 1, cz - 4, QUARTZ()); set(cx + 1, y + 1, cz - 4, QUARTZ());
        // a little screen wall for the painting
        fill(cx + 1, y + 1, cz + 5, cx + 3, y + 2, cz + 5, QUARTZ());
        painting(cx + 2, y + 2, cz + 4, EnumFacing.NORTH, "bacchanal");
        int n = 4 + r.nextInt(3);
        // 1.3.2: the god's mask hangs on the north-west column, facing the basin side (a wild one may hold a Crown of Thorns)
        if (LTConfig.v132.maskInFountains && lucifertotem.v132.V132.MASK != null)
            set(cx - 4, y + 3, cz - 3, lucifertotem.v132.V132.MASK.getDefaultState().withProperty(lucifertotem.v132.V132.BlockMask.FACING, EnumFacing.SOUTH).withProperty(lucifertotem.v132.V132.BlockMask.WILD, true));
        for (int i = 0; i < n; i++) { double a = i * Math.PI * 2 / n; mob(EntityBacchant::new, cx + 0.5 + Math.cos(a) * 3.5, y + 1, cz + 0.5 + Math.sin(a) * 3.5); }
        at = new BlockPos(cx, y + 1, cz);
        return true;
    }

    // ------------------------------------------------------------ Hekate's Crossroads Shrine (stage 3)
    /** three gravel roads meet at a three-faced Hekataion; under the plinth a vault behind a Hekate gate */
    boolean crossroads() {
        int cx = bx + 8, cz = bz + 8;
        int y = site(cx, cz, 4, 2);
        if (y < 0 || !solidBelow(cx, cz, 3, y - 1, y - 7)) return false;
        // three roads, 120 degrees apart
        double base = r.nextInt(3) * (Math.PI / 6);
        for (int k = 0; k < 3; k++) {
            double a = base + k * Math.PI * 2 / 3;
            for (double d = 1; d <= 7; d += 0.5) {
                int x = cx + (int) Math.round(Math.cos(a) * d), z = cz + (int) Math.round(Math.sin(a) * d);
                int sy = surface(x, z);
                if (sy > 0 && Math.abs(sy - y) <= 2) { set(x, sy, z, Blocks.GRAVEL.getDefaultState()); fill(x, sy + 1, z, x, sy + 2, z, AIR); }
            }
            int tx = cx + (int) Math.round(Math.cos(a + Math.PI / 3) * 3), tz = cz + (int) Math.round(Math.sin(a + Math.PI / 3) * 3);
            set(tx, y + 1, tz, Blocks.DARK_OAK_FENCE.getDefaultState()); set(tx, y + 2, tz, Blocks.DARK_OAK_FENCE.getDefaultState()); set(tx, y + 3, tz, Blocks.TORCH.getDefaultState());
        }
        fill(cx - 1, y, cz - 1, cx + 1, y, cz + 1, BRICK());
        fill(cx - 1, y + 1, cz - 1, cx + 1, y + 3, cz + 1, AIR);
        set(cx, y + 1, cz, Stage3.HEKATAION.getDefaultState());
        set(cx + 1, y, cz + 1, Blocks.SOUL_SAND.getDefaultState());
        // the vault: shaft at (cx-1, cz-1) closed by a gate, ladder down to a 5x5 room
        int vy = y - 6;
        box(cx - 3, vy - 1, cz - 3, cx + 3, vy + 3, cz + 3, BRICK(), AIR);
        set(cx - 1, y, cz - 1, Stage3.HEKATE_GATE.getDefaultState());
        fill(cx - 1, vy + 3, cz - 1, cx - 1, y - 1, cz - 1, AIR);
        ladder(cx - 1, vy, y - 1, cz - 1, EnumFacing.SOUTH);
        set(cx - 1, vy, cz - 2, BRICK());
        fill(cx - 1, vy, cz - 2, cx - 1, y - 1, cz - 2, BRICK());
        chest(cx + 2, vy, cz + 2, EnumFacing.NORTH, Stage3.LOOT_HEKATE);
        torch(cx + 2, vy + 1, cz - 2, EnumFacing.SOUTH); torch(cx - 2, vy + 1, cz + 2, EnumFacing.EAST);
        set(cx, vy - 1, cz, Blocks.SOUL_SAND.getDefaultState());
        painting(cx + 1, vy + 1, cz + 2, EnumFacing.NORTH, "hekate");
        at = new BlockPos(cx, y + 1, cz);
        return true;
    }
}
