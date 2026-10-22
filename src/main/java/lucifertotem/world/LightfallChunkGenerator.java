package lucifertotem.world;

import lucifertotem.LTConfig;
import lucifertotem.ModContent;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Floating islands over a shallow glowing violet sea.
 *  - sea floor of duskstone around y=22..32, water up to SEA_LEVEL (purple-tinted by the biome), crystal-lit
 *  - main archipelago: islands topped at ~y 66..104 with rounded, tapering undersides
 *  - sparse high islets around y 128..150
 * Structures and decoration are placed in populate(), strictly inside the 16x16 populate window
 * (chunk origin +8), so they never force neighbouring chunks to generate (no cascading worldgen).
 */
public class LightfallChunkGenerator implements IChunkGenerator {
    public static final int SEA_LEVEL = 44;
    private final World world;
    private final long seed;
    private final NoiseGeneratorSimplex nMask1, nMask2, nMask3, nTop, nThick, nFloor, nHigh, nHighTop;
    private final IBlockState STONE, GRASS, WATER = Blocks.WATER.getDefaultState(), BEDROCK = Blocks.BEDROCK.getDefaultState();

    public LightfallChunkGenerator(World world, long seed) {
        this.world = world;
        this.seed = seed;
        Random r = new Random(seed ^ 0x4C1F3EL);
        nMask1 = new NoiseGeneratorSimplex(r); nMask2 = new NoiseGeneratorSimplex(r); nMask3 = new NoiseGeneratorSimplex(r);
        nTop = new NoiseGeneratorSimplex(r); nThick = new NoiseGeneratorSimplex(r); nFloor = new NoiseGeneratorSimplex(r);
        nHigh = new NoiseGeneratorSimplex(r); nHighTop = new NoiseGeneratorSimplex(r);
        STONE = ModContent.DUSKSTONE.getDefaultState();
        GRASS = ModContent.STAR_GRASS.getDefaultState();
    }

    /** island strength 0..1 (0 = open sky) for the main layer */
    double islandT(int x, int z) {
        double m = 0.60 * nMask1.getValue(x / 150.0, z / 150.0) + 0.28 * nMask2.getValue(x / 58.0, z / 58.0) + 0.12 * nMask3.getValue(x / 21.0, z / 21.0);
        double T = 0.22;
        return m <= T ? 0 : Math.min(1.0, (m - T) / 0.55);
    }

    int islandTop(int x, int z, double t) {
        return 82 + (int) Math.round(12 * nTop.getValue(x / 110.0, z / 110.0) + 8 * t);
    }

    /** returns {bottom, top} of main island at column or null */
    int[] mainIsland(int x, int z) {
        double t = islandT(x, z);
        if (t <= 0) return null;
        int top = islandTop(x, z, t);
        int thick = 2 + (int) (Math.sqrt(t) * 24 + 3 * nThick.getValue(x / 11.0, z / 11.0) + t * t * 8);
        if (thick < 2) thick = 2;
        return new int[] {top - thick, top};
    }

    int[] highIslet(int x, int z) {
        double m = nHigh.getValue(x / 44.0, z / 44.0) + 0.15 * nMask3.getValue(x / 9.0, z / 9.0);
        if (m <= 0.62) return null;
        double t = Math.min(1, (m - 0.62) / 0.35);
        int top = 136 + (int) Math.round(10 * nHighTop.getValue(x / 70.0, z / 70.0) + 3 * t);
        int thick = 1 + (int) (Math.sqrt(t) * 13);
        return new int[] {top - thick, top};
    }

    int seaFloor(int x, int z) {
        return 27 + (int) Math.round(5 * nFloor.getValue(x / 40.0, z / 40.0) + 2 * nMask3.getValue(x / 13.0, z / 13.0));
    }

    @Override
    public Chunk generateChunk(int cx, int cz) {
        ChunkPrimer p = new ChunkPrimer();
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
            int x = cx * 16 + lx, z = cz * 16 + lz;
            p.setBlockState(lx, 0, lz, BEDROCK);
            int floor = seaFloor(x, z);
            for (int y = 1; y <= floor; y++) p.setBlockState(lx, y, lz, STONE);
            int[] isl = mainIsland(x, z);
            if (isl != null) {
                for (int y = Math.max(1, isl[0]); y <= isl[1]; y++) p.setBlockState(lx, y, lz, y == isl[1] ? GRASS : STONE);
            }
            for (int y = floor + 1; y <= SEA_LEVEL; y++) if (p.getBlockState(lx, y, lz).getMaterial() == Material.AIR) p.setBlockState(lx, y, lz, WATER);
            int[] hi = highIslet(x, z);
            if (hi != null) for (int y = hi[0]; y <= hi[1]; y++) p.setBlockState(lx, y, lz, y == hi[1] ? GRASS : STONE);
        }
        Chunk c = new Chunk(world, p, cx, cz);
        byte[] b = c.getBiomeArray();
        byte id = (byte) Biome.getIdForBiome(ModContent.BIOME);
        java.util.Arrays.fill(b, id);
        c.generateSkylightMap();
        return c;
    }

    // ---------------------------------------------------------------- population

    public static Random chunkRandom(long seed, int cx, int cz, long salt) {
        Random r = new Random(seed ^ salt);
        long a = r.nextLong() | 1L, b = r.nextLong() | 1L;
        r.setSeed((long) cx * a + (long) cz * b ^ seed ^ salt);
        return r;
    }

    /** highest non-air block y in column, scanning down from 200 to minY; -1 if none */
    public static int topY(World w, int x, int z, int minY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = 200; y >= minY; y--) {
            m.setPos(x, y, z);
            if (!w.isAirBlock(m)) return y;
        }
        return -1;
    }

    @Override
    public void populate(int cx, int cz) {
        net.minecraft.block.BlockFalling.fallInstantly = true;
        try {
            Random r = chunkRandom(seed, cx, cz, 0x5EEDL);
            int bx = cx * 16 + 8, bz = cz * 16 + 8;  // populate window [bx, bx+15] x [bz, bz+15]
            decorate(r, bx, bz);
            LightfallSea.populate(world, chunkRandom(seed, cx, cz, 0x5EA1L), bx, bz);
            LightfallStructures.tryGenerate(world, chunkRandom(seed, cx, cz, 0x57A7L), bx, bz);
            herbs(chunkRandom(seed, cx, cz, 0x4E2BL), bx, bz); // 1.1.0, own random: older decoration unchanged
            LibraryGen.populate(world, seed, bx, bz);          // 1.1.0: slices of any Library of Alexandria overlapping this window
            lucifertotem.v130.V130Gen.populate(world, seed, cx, cz, bx, bz);   // 1.3.0, own randoms: older decoration unchanged
        } finally {
            net.minecraft.block.BlockFalling.fallInstantly = false;
        }
    }

    /** 1.1.0: Shrieking Mandrakes (rare) and Henbane (patches) on island turf */
    private void herbs(Random r, int bx, int bz) {
        if (lucifertotem.hillman.HBlocks.MANDRAKE == null) return;
        int tries = r.nextInt(6) == 0 ? 1 : 0;
        for (int i = 0; i < tries; i++) plant(r, bx, bz, lucifertotem.hillman.HBlocks.MANDRAKE.getDefaultState(), 1);
        if (r.nextInt(4) == 0) plant(r, bx, bz, lucifertotem.hillman.HBlocks.HENBANE.getDefaultState(), 2 + r.nextInt(4));
    }

    private void plant(Random r, int bx, int bz, IBlockState s, int n) {
        int x = bx + 2 + r.nextInt(12), z = bz + 2 + r.nextInt(12);
        for (int k = 0; k < n; k++) {
            int px = x + r.nextInt(5) - 2, pz = z + r.nextInt(5) - 2;
            int y = topY(world, px, pz, SEA_LEVEL + 1);
            if (y > 0 && world.getBlockState(new BlockPos(px, y, pz)).getBlock() == ModContent.STAR_GRASS && world.isAirBlock(new BlockPos(px, y + 1, pz)))
                world.setBlockState(new BlockPos(px, y + 1, pz), s, 2);
        }
    }

    private void decorate(Random r, int bx, int bz) {
        World w = world;
        IBlockState feather = ModContent.FALLEN_FEATHER.getDefaultState();
        IBlockState crystal = ModContent.GLOW_CRYSTAL.getDefaultState();
        // fallen feathers on the turf
        for (int i = 0; i < 10; i++) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16);
            int y = topY(w, x, z, SEA_LEVEL + 1);
            if (y > 0 && w.getBlockState(new BlockPos(x, y, z)).getBlock() == ModContent.STAR_GRASS && r.nextInt(3) > 0)
                w.setBlockState(new BlockPos(x, y + 1, z), feather, 2);
        }
        // pale-barked violet trees
        int trees = r.nextInt(3);
        for (int i = 0; i < trees; i++) {
            int x = bx + 2 + r.nextInt(12), z = bz + 2 + r.nextInt(12);
            int y = topY(w, x, z, SEA_LEVEL + 1);
            if (y > 0 && w.getBlockState(new BlockPos(x, y, z)).getBlock() == ModContent.STAR_GRASS) tree(r, x, y + 1, z);
        }
        // crystals on the surface (rare) and hanging beneath islands (common)
        if (r.nextInt(4) == 0) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16);
            int y = topY(w, x, z, SEA_LEVEL + 1);
            if (y > 0 && w.getBlockState(new BlockPos(x, y, z)).getBlock() == ModContent.STAR_GRASS) w.setBlockState(new BlockPos(x, y + 1, z), crystal, 2);
        }
        for (int i = 0; i < 4; i++) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int y = SEA_LEVEL + 2; y < 160; y++) {
                m.setPos(x, y, z);
                if (w.isAirBlock(m) && w.getBlockState(m.up()).getBlock() == ModContent.DUSKSTONE) { w.setBlockState(m.toImmutable(), crystal, 2); break; }
            }
        }
        // the glowing sea floor
        for (int i = 0; i < 9; i++) {
            int x = bx + r.nextInt(16), z = bz + r.nextInt(16);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, SEA_LEVEL, z);
            while (m.getY() > 2 && w.getBlockState(m).getMaterial() == Material.WATER) m.setY(m.getY() - 1);
            if (m.getY() < SEA_LEVEL - 1 && w.getBlockState(m).getBlock() == ModContent.DUSKSTONE)
                w.setBlockState(m.up(), ModContent.CRYSTAL_CORAL.getDefaultState(), 2);
        }
    }

    private void tree(Random r, int x, int y, int z) {
        IBlockState log = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH).withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y);
        IBlockState leaf = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.OAK)
            .withProperty(BlockLeaves.DECAYABLE, false).withProperty(BlockLeaves.CHECK_DECAY, false);
        int h = 4 + r.nextInt(3);
        for (int dy = 0; dy < h + 2; dy++) if (!w().isAirBlock(new BlockPos(x, y + dy, z))) return;
        for (int dy = h - 2; dy <= h + 1; dy++) {
            int rad = dy >= h ? 1 : 2;
            for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
                if (Math.abs(dx) == rad && Math.abs(dz) == rad && (dy >= h || r.nextInt(2) == 0)) continue;
                BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                if (w().isAirBlock(p)) w().setBlockState(p, leaf, 2);
            }
        }
        for (int dy = 0; dy < h; dy++) w().setBlockState(new BlockPos(x, y + dy, z), log, 2);
        w().setBlockState(new BlockPos(x, y - 1, z), STONE, 2);
        if (r.nextInt(3) == 0) { // a hanging crystal lantern
            BlockPos lp = new BlockPos(x + (r.nextBoolean() ? 2 : -2), y + h - 3, z);
            if (w().isAirBlock(lp)) w().setBlockState(lp, ModContent.GLOW_CRYSTAL.getDefaultState(), 2);
        }
    }

    private World w() { return world; }

    @Override public boolean generateStructures(Chunk c, int x, int z) { return false; }
    @Override public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType t, BlockPos p) { return ModContent.BIOME.getSpawnableList(t); }
    @Override public BlockPos getNearestStructurePos(World w, String name, BlockPos p, boolean f) { return null; }
    @Override public void recreateStructures(Chunk c, int x, int z) { }
    @Override public boolean isInsideStructure(World w, String name, BlockPos p) { return false; }
}
