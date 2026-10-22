package lucifertotem.world;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.EntityEnchantress;
import lucifertotem.hillman.HBlocks;
import lucifertotem.hillman.StructureIndex;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Library of Alexandria: a rare, mansion-like, multi-floor library floating on its own island in the Lightfall.
 * Layout: a 6x6 grid of 8-block cells. Outer ring = rooms (varied templates), middle ring = galleries (corridors),
 * centre 2x2 = the domed Grand Reading Hall. Two full floors, corner towers with pyramid caps on a third.
 * Generation is fully deterministic from (seed, region) and clipped to each chunk's populate window, so every
 * window builds only its own slice: no cascading worldgen, no out-of-window writes.
 */
public class LibraryGen {
    public static final int CELL = 8, GRID = 6, SIZE = CELL * GRID + 1; // 49 x 49
    public static final int Y0 = 100, FH = 7;                            // ground floor level, floor height
    public static final int MARGIN = 3;
    public static int built = 0, slices = 0, ammonius = 0, enchantresses = 0, guards = 0, spawners = 0, chests = 0;
    public static long nanos = 0;

    public static final ResourceLocation LOOT = new ResourceLocation(LuciferTotem.MODID, "chests/library");
    public static final ResourceLocation LOOT_APOTHECARY = new ResourceLocation(LuciferTotem.MODID, "chests/library_apothecary");
    public static final ResourceLocation LOOT_STARMAP = new ResourceLocation(LuciferTotem.MODID, "chests/library_starmap");

    enum Room { ENTRANCE, STUDY, STACKS, SCROLLS, READING, STARMAP, APOTHECARY, ENCHANT, SPAWNER, GALLERY, STAIRS, ATRIUM }

    // ------------------------------------------------------------ where libraries are
    static int region() { return Math.max(12, LTConfig.library.regionChunks); }

    /** NW corner (at floor level) of the library of region (rx, rz), or null */
    public static BlockPos origin(long seed, int rx, int rz) {
        if (!LTConfig.library.enabled) return null;
        int R = region();
        Random r = LightfallChunkGenerator.chunkRandom(seed, rx, rz, 0xA1E8L);
        if (r.nextInt(100) >= LTConfig.library.chancePercent) return null;
        int cx = rx * R + 2 + r.nextInt(R - 6), cz = rz * R + 2 + r.nextInt(R - 6);
        if (Math.abs(cx) < 6 && Math.abs(cz) < 6) return null; // keep the world origin clear
        return new BlockPos(cx * 16, Y0, cz * 16);
    }
    public static BlockPos center(BlockPos origin) { return origin.add(SIZE / 2, 0, SIZE / 2); }
    /** 1.3.2: where the Dionysus icon's lower half hangs (inner wall over the entrance), from the library centre */
    public static BlockPos iconPos(BlockPos center) { BlockPos o = center.add(-SIZE / 2, 0, -SIZE / 2); return new BlockPos(o.getX() + 3 * CELL, Y0 + 5, o.getZ() + SIZE - 2); }

    /** nearest library centre within radius blocks (predicted from the seed; true whether or not it is generated yet) */
    public static BlockPos nearest(long seed, BlockPos from, int radius) {
        int R = region() * 16;
        int rx0 = Math.floorDiv(from.getX() - radius, R), rx1 = Math.floorDiv(from.getX() + radius, R);
        int rz0 = Math.floorDiv(from.getZ() - radius, R), rz1 = Math.floorDiv(from.getZ() + radius, R);
        BlockPos best = null; double bd = (double) radius * radius;
        for (int rx = rx0; rx <= rx1; rx++) for (int rz = rz0; rz <= rz1; rz++) {
            BlockPos o = origin(seed, rx, rz);
            if (o == null) continue;
            BlockPos c = center(o);
            double d = sq(c.getX() - from.getX()) + sq(c.getZ() - from.getZ());
            if (d <= bd) { bd = d; best = c; }
        }
        return best;
    }
    static double sq(double v) { return v * v; }

    /** called for every populate window: build the slices of any library overlapping it */
    public static void populate(World w, long seed, int bx, int bz) {
        if (!LTConfig.library.enabled) return;
        int R = region() * 16, pad = SIZE + MARGIN + 16;
        for (int rx = Math.floorDiv(bx - pad, R); rx <= Math.floorDiv(bx + 16 + pad, R); rx++)
            for (int rz = Math.floorDiv(bz - pad, R); rz <= Math.floorDiv(bz + 16 + pad, R); rz++) {
                BlockPos o = origin(seed, rx, rz);
                if (o == null) continue;
                if (bx + 15 < o.getX() - MARGIN || bx > o.getX() + SIZE - 1 + MARGIN || bz + 15 < o.getZ() - MARGIN || bz > o.getZ() + SIZE - 1 + MARGIN) continue; // exact footprint incl. island margin + steps
                long t0 = System.nanoTime();
                new LibraryGen(w, o, seed, bx, bz).build();
                nanos += System.nanoTime() - t0;
                slices++;
            }
    }

    // ------------------------------------------------------------ one slice
    final World w; final BlockPos o; final int ox, oz, bx, bz; final long seed;
    final Room[][][] rooms = new Room[3][GRID][GRID];
    final boolean[][] tower = new boolean[GRID][GRID];
    final boolean[][][] openE = new boolean[2][GRID][GRID], openS = new boolean[2][GRID][GRID]; // extra doors between outer rooms

    LibraryGen(World w, BlockPos o, long seed, int bx, int bz) {
        this.w = w; this.o = o; this.ox = o.getX(); this.oz = o.getZ(); this.seed = seed; this.bx = bx; this.bz = bz;
        layout(new Random(seed ^ (o.toLong() * 0x9E3779B97F4A7C15L)));
    }

    static int ring(int i, int j) { return Math.min(Math.min(i, j), Math.min(GRID - 1 - i, GRID - 1 - j)); }

    void layout(Random r) {
        for (int f = 0; f < 2; f++) {
            List<int[]> outer = new ArrayList<>();
            for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
                int g = ring(i, j);
                rooms[f][i][j] = g == 2 ? Room.ATRIUM : g == 1 ? Room.GALLERY : null;
                if (g == 0) outer.add(new int[]{i, j});
            }
            List<Room> pool = new ArrayList<>();
            if (f == 0) {
                rooms[0][2][5] = rooms[0][3][5] = Room.ENTRANCE;
                rooms[0][2][0] = rooms[0][3][0] = Room.STUDY;
                Collections.addAll(pool, Room.APOTHECARY, Room.APOTHECARY, Room.ENCHANT, Room.READING, Room.READING, Room.READING, Room.SCROLLS, Room.SCROLLS, Room.SCROLLS);
            } else {
                Collections.addAll(pool, Room.STARMAP, Room.SPAWNER, Room.SPAWNER, Room.SPAWNER, Room.ENCHANT, Room.READING, Room.READING, Room.SCROLLS, Room.SCROLLS, Room.APOTHECARY);
            }
            Collections.shuffle(outer, r);
            int k = 0;
            for (int[] c : outer) {
                if (rooms[f][c[0]][c[1]] != null) continue;
                rooms[f][c[0]][c[1]] = k < pool.size() ? pool.get(k++) : Room.STACKS;
            }
            for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) { openE[f][i][j] = r.nextInt(3) == 0; openS[f][i][j] = r.nextInt(3) == 0; }
        }
        rooms[0][1][1] = rooms[0][4][4] = Room.STAIRS;
        for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
            boolean corner = (i == 0 || i == GRID - 1) && (j == 0 || j == GRID - 1);
            tower[i][j] = corner || (ring(i, j) == 0 && r.nextInt(6) == 0 && rooms[1][i][j] != Room.SPAWNER);
            rooms[2][i][j] = tower[i][j] ? Room.READING : null;
        }
    }

    // ------------------------------------------------------------ clipped writes
    boolean in(int x, int y, int z) { return x >= bx && x < bx + 16 && z >= bz && z < bz + 16 && y > 0 && y < 255; }
    void set(int x, int y, int z, IBlockState s) { if (in(x, y, z)) w.setBlockState(new BlockPos(x, y, z), s, 2); }
    void fill(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState s) {
        int ax = Math.max(x1, bx), az = Math.max(z1, bz), cx = Math.min(x2, bx + 15), cz = Math.min(z2, bz + 15);
        for (int x = ax; x <= cx; x++) for (int y = y1; y <= y2; y++) for (int z = az; z <= cz; z++) set(x, y, z, s);
    }
    long posSeed(int x, int y, int z) { return seed ^ (new BlockPos(x, y, z).toLong() * 31L + 0x11B7L); }
    Random rnd(int x, int y, int z) { return new Random(posSeed(x, y, z)); }

    void chest(int x, int y, int z, EnumFacing f, ResourceLocation loot) {
        if (!in(x, y, z)) return;
        BlockPos p = new BlockPos(x, y, z);
        w.setBlockState(p, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, f), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityChest) { ((TileEntityChest) te).setLootTable(loot, posSeed(x, y, z)); chests++; }
    }
    void spawner(int x, int y, int z, String mob) {
        if (!in(x, y, z)) return;
        BlockPos p = new BlockPos(x, y, z);
        w.setBlockState(p, Blocks.MOB_SPAWNER.getDefaultState(), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityMobSpawner) { ((TileEntityMobSpawner) te).getSpawnerBaseLogic().setEntityId(new ResourceLocation(mob)); spawners++; }
    }
    void banner(int x, int y, int z, EnumFacing f) {
        if (!in(x, y, z)) return;
        BlockPos p = new BlockPos(x, y, z);
        w.setBlockState(p, Blocks.WALL_BANNER.getDefaultState().withProperty(BlockBanner.FACING, f), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityBanner) ((TileEntityBanner) te).setItemValues(new ItemStack(Items.BANNER, 1, EnumDyeColor.PURPLE.getDyeDamage()), false);
    }
    void mob(EntityLiving e, double x, double y, double z) {
        if (!in((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) return;
        e.setLocationAndAngles(x, y, z, rnd((int) x, (int) y, (int) z).nextFloat() * 360f, 0f);
        e.onInitialSpawn(w.getDifficultyForLocation(new BlockPos(x, y, z)), null);
        e.enablePersistence();
        w.spawnEntity(e);
    }

    // palette
    static IBlockState BRICK() { return ModContent.DUSKSTONE_BRICKS.getDefaultState(); }
    static IBlockState STONE() { return ModContent.DUSKSTONE.getDefaultState(); }
    static final IBlockState AIR = Blocks.AIR.getDefaultState();
    static final IBlockState SHELF = Blocks.BOOKSHELF.getDefaultState();
    static final IBlockState QUARTZ = Blocks.QUARTZ_BLOCK.getDefaultState();
    static final IBlockState CHISELED = Blocks.QUARTZ_BLOCK.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.CHISELED);
    static final IBlockState PILLAR = Blocks.QUARTZ_BLOCK.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.LINES_Y);
    static final IBlockState SAND = Blocks.SANDSTONE.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.SMOOTH);
    static final IBlockState SAND_C = Blocks.SANDSTONE.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.CHISELED);
    static final IBlockState GOLD = Blocks.GOLD_BLOCK.getDefaultState();
    static final IBlockState CARPET = Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.PURPLE);
    static final IBlockState WOOL = Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.PURPLE);
    static final IBlockState GLASS = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.PURPLE);
    static final IBlockState PANE = Blocks.STAINED_GLASS_PANE.getDefaultState().withProperty(BlockStainedGlassPane.COLOR, EnumDyeColor.PURPLE);
    static final IBlockState FENCE = Blocks.OAK_FENCE.getDefaultState();
    static final IBlockState PLATE = Blocks.WOODEN_PRESSURE_PLATE.getDefaultState();
    static final IBlockState LIGHT = Blocks.GLOWSTONE.getDefaultState();
    static final IBlockState NIGHT = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.BLACK);
    static IBlockState stair(EnumFacing f) { return Blocks.OAK_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, f); }
    static IBlockState stoneStair(EnumFacing f) { return Blocks.QUARTZ_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, f); }
    static IBlockState rack(EnumFacing f) { return Blocks.TRAPDOOR.getDefaultState().withProperty(BlockTrapDoor.FACING, f).withProperty(BlockTrapDoor.OPEN, true); }

    int cx0(int i) { return ox + i * CELL; }
    int cz0(int j) { return oz + j * CELL; }
    int fy(int f) { return Y0 + f * FH; }

    // ------------------------------------------------------------ build
    void build() {
        int x1 = ox + SIZE - 1, z1 = oz + SIZE - 1;
        int top = Y0 + 3 * FH + 8;
        // the floating island
        for (int k = 0; k <= 13; k++) {
            int in = k <= 1 ? -MARGIN : (k - 1) * 2 - MARGIN;
            if (ox + in > x1 - in) break;
            fill(ox + in, Y0 - k, oz + in, x1 - in, Y0 - k, z1 - in, k == 0 ? ModContent.STAR_GRASS.getDefaultState() : STONE());
        }
        set(ox + SIZE / 2, Y0 - 14, oz + SIZE / 2, ModContent.GLOW_CRYSTAL.getDefaultState());
        fill(ox - MARGIN, Y0 + 1, oz - MARGIN, x1 + MARGIN, top, z1 + MARGIN, AIR);
        // shell: floors and the wall grid, for two full floors
        for (int f = 0; f < 2; f++) {
            int y = fy(f);
            fill(ox, y, oz, x1, y, z1, f == 0 ? SAND : BRICK());
            for (int g = 0; g <= GRID; g++) {
                fill(ox + g * CELL, y + 1, oz, ox + g * CELL, y + FH - 1, z1, BRICK());
                fill(ox, y + 1, oz + g * CELL, x1, y + FH - 1, oz + g * CELL, BRICK());
            }
            for (int gi = 0; gi <= GRID; gi++) for (int gj = 0; gj <= GRID; gj++) fill(cx0(gi), y + 1, cz0(gj), cx0(gi), y + FH - 1, cz0(gj), PILLAR);
        }
        fill(ox, Y0 + 2 * FH, oz, x1, Y0 + 2 * FH, z1, BRICK()); // roof
        // exterior windows (outer rooms), gold string course
        for (int f = 0; f < 2; f++) {
            int y = fy(f);
            for (int g = 0; g < GRID; g++) {
                int m = g * CELL + 4;
                for (int dy = 2; dy <= 4; dy++) {
                    set(ox + m, y + dy, oz, PANE); set(ox + m, y + dy, z1, PANE); set(ox, y + dy, oz + m, PANE); set(x1, y + dy, oz + m, PANE);
                }
            }
            fill(ox, y + FH, oz - 0, x1, y + FH, oz, SAND_C); fill(ox, y + FH, z1, x1, y + FH, z1, SAND_C);
            fill(ox, y + FH, oz, ox, y + FH, z1, SAND_C); fill(x1, y + FH, oz, x1, y + FH, z1, SAND_C);
        }
        // connections
        for (int f = 0; f < 2; f++) connections(f);
        // rooms
        for (int f = 0; f < 2; f++) for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) room(f, i, j);
        atrium();
        roof();
        entranceOutside();
        // index + the librarian (only in the window holding the centre, so exactly once)
        BlockPos c = center(o);
        if (in(c.getX(), c.getY() + 1, c.getZ())) {
            StructureIndex.record(w, "library", c);
            built++;
            EntityAmmonius a = new EntityAmmonius(w);
            a.getEntityData().setBoolean("lt_librarian", true);
            mob(a, c.getX() + 0.5, c.getY() + 1, c.getZ() + 2.5);
            a.setHomePosAndDistance(c, 22);
            ammonius++;
            if (LTConfig.testHarness) LuciferTotem.LOG.info("[LT][GEN] library of Alexandria at {}", c);
        }
    }

    void door(int x, int z, int y, boolean alongX, int width, int height) {
        for (int k = -(width / 2); k <= width / 2; k++)
            for (int dy = 1; dy <= height; dy++) set(alongX ? x + k : x, y + dy, alongX ? z : z + k, AIR);
    }

    void connections(int f) {
        int y = fy(f);
        for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
            Room a = rooms[f][i][j];
            int ga = ring(i, j);
            // east neighbour (shared wall x = cx0(i+1))
            if (i + 1 < GRID) {
                Room b = rooms[f][i + 1][j]; int gb = ring(i + 1, j);
                int wx = cx0(i + 1), mz = cz0(j) + 4;
                if (ga == 1 && gb == 1) fill(wx, y + 1, cz0(j) + 1, wx, y + FH - 1, cz0(j) + 7, AIR);      // gallery to gallery: open
                else if (ga == 2 && gb == 2) fill(wx, y + 1, cz0(j) + 1, wx, y + FH - 1, cz0(j) + 7, AIR); // atrium inside
                else if (ga + gb == 3) arch(wx, y, cz0(j), false, f);                                       // gallery <-> atrium
                else if ((ga == 0) != (gb == 0)) door(wx, mz, y, false, 1, 2);                              // room <-> gallery
                else if (ga == 0 && gb == 0 && (a == b || openE[f][i][j] || (i == 0 || i + 1 == GRID - 1) && (j == 0 || j == GRID - 1))) {
                    if (a == b) fill(wx, y + 1, cz0(j) + 1, wx, y + FH - 1, cz0(j) + 7, AIR); else door(wx, mz, y, false, 1, 2);
                }
            }
            if (j + 1 < GRID) {
                Room b = rooms[f][i][j + 1]; int gb = ring(i, j + 1);
                int wz = cz0(j + 1), mx = cx0(i) + 4;
                if (ga == 1 && gb == 1) fill(cx0(i) + 1, y + 1, wz, cx0(i) + 7, y + FH - 1, wz, AIR);
                else if (ga == 2 && gb == 2) fill(cx0(i) + 1, y + 1, wz, cx0(i) + 7, y + FH - 1, wz, AIR);
                else if (ga + gb == 3) arch(cx0(i), y, wz, true, f);
                else if ((ga == 0) != (gb == 0)) door(mx, wz, y, true, 1, 2);
                else if (ga == 0 && gb == 0 && (a == b || openS[f][i][j] || (i == 0 || i == GRID - 1) && (j == 0 || j + 1 == GRID - 1))) {
                    if (a == b) fill(cx0(i) + 1, y + 1, wz, cx0(i) + 7, y + FH - 1, wz, AIR); else door(mx, wz, y, true, 1, 2);
                }
            }
        }
        // atrium internal grid pillars removed (one big hall)
        fill(cx0(3), y + 1, cz0(3), cx0(3), y + FH - 1, cz0(3), AIR);
    }

    /** gallery <-> atrium: arcade of quartz columns; on the upper floor a balustrade over the hall */
    void arch(int x, int y, int z, boolean alongX, int f) {
        for (int k = 1; k <= 7; k++) {
            int px = alongX ? x + k : x, pz = alongX ? z : z + k;
            boolean col = k == 4;
            for (int dy = 1; dy <= FH - 1; dy++) {
                IBlockState s;
                if (f == 0) s = col ? PILLAR : dy == FH - 1 ? SAND_C : AIR;
                else s = col ? PILLAR : dy == 1 ? FENCE : dy == FH - 1 ? SAND_C : AIR;
                set(px, y + dy, pz, s);
            }
        }
    }

    void room(int f, int i, int j) {
        Room t = rooms[f][i][j];
        if (t == null || t == Room.ATRIUM) return;
        int x = cx0(i), z = cz0(j), y = fy(f);
        Random r = rnd(x, y, z);
        switch (t) {
            case STACKS: stacks(x, y, z, r); break;
            case SCROLLS: scrolls(x, y, z, r); break;
            case READING: reading(x, y, z, r, f); break;
            case STARMAP: starmap(x, y, z, r); break;
            case APOTHECARY: apothecary(x, y, z, r); break;
            case ENCHANT: enchant(x, y, z, r); break;
            case SPAWNER: stacks(x, y, z, r); spawner(x + 4, y + 1, z + 4, LuciferTotem.MODID + ":enchantress"); placeEnchantress(x + 2.5, y + 1, z + 5.5);
                chest(x + 6, y + 1, z + 6, EnumFacing.NORTH, LOOT); break;
            case GALLERY: gallery(x, y, z, r, f, i, j); break;
            case STAIRS: gallery(x, y, z, r, f, i, j); stairs(x, y, z); break;
            case ENTRANCE: entrance(x, y, z, i); break;
            case STUDY: study(x, y, z, i); break;
            default: break;
        }
        set(x + 4, y + FH - 1, z + 4, LIGHT);
    }

    /** bookshelves around the interior walls, leaving the middle of each wall (doors, windows) free */
    void shelfRing(int x, int y, int z, int h) {
        for (int k = 1; k <= 7; k++) for (int dy = 1; dy <= h; dy++) {
            if (k == 4) continue;
            set(x + k, y + dy, z + 1, SHELF); set(x + k, y + dy, z + 7, SHELF); set(x + 1, y + dy, z + k, SHELF); set(x + 7, y + dy, z + k, SHELF);
        }
    }

    void stacks(int x, int y, int z, Random r) {
        shelfRing(x, y, z, 4);
        for (int dy = 1; dy <= 3; dy++) for (int k = 3; k <= 5; k++) { if (k == 4 && dy < 3) continue; set(x + 3, y + dy, z + k, SHELF); set(x + 5, y + dy, z + k, SHELF); }
        for (int k = 2; k <= 6; k++) set(x + 4, y + 1, z + k, CARPET);
        if (r.nextInt(3) == 0) chest(x + 2, y + 1, z + 2, EnumFacing.SOUTH, LOOT);
    }

    void scrolls(int x, int y, int z, Random r) {
        for (int k = 1; k <= 7; k++) for (int dy = 1; dy <= 4; dy++) {
            if (k == 4) continue;
            boolean alt = (k + dy) % 2 == 0;
            set(x + k, y + dy, z + 1, alt ? SHELF : rack(EnumFacing.SOUTH)); set(x + k, y + dy, z + 7, alt ? SHELF : rack(EnumFacing.NORTH));
            set(x + 1, y + dy, z + k, alt ? SHELF : rack(EnumFacing.EAST)); set(x + 7, y + dy, z + k, alt ? SHELF : rack(EnumFacing.WEST));
        }
        set(x + 4, y + 1, z + 4, FENCE); set(x + 4, y + 2, z + 4, Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.WHITE));
        if (r.nextInt(2) == 0) chest(x + 3, y + 1, z + 3, EnumFacing.SOUTH, LOOT);
    }

    void table(int x, int y, int z) {
        set(x, y + 1, z, FENCE); set(x, y + 2, z, PLATE);
        set(x - 1, y + 1, z, stair(EnumFacing.EAST)); set(x + 1, y + 1, z, stair(EnumFacing.WEST));
    }

    void reading(int x, int y, int z, Random r, int f) {
        fill(x + 1, y + 1, z + 1, x + 7, y + 1, z + 7, CARPET);
        for (int k = 1; k <= 7; k++) for (int dy = 1; dy <= 3; dy++) { if (k == 4) continue; set(x + 1, y + dy, z + k, SHELF); set(x + 7, y + dy, z + k, SHELF); }
        table(x + 4, y, z + 3); table(x + 4, y, z + 6);
        if (r.nextInt(2) == 0) chest(x + 2, y + 1, z + 1, EnumFacing.SOUTH, LOOT);
        if (f == 2) chest(x + 6, y + 1, z + 1, EnumFacing.SOUTH, LOOT);
    }

    void starmap(int x, int y, int z, Random r) {
        fill(x + 1, y, z + 1, x + 7, y, z + 7, NIGHT);
        fill(x + 1, y + FH, z + 1, x + 7, y + FH, z + 7, NIGHT);
        for (int n = 0; n < 9; n++) set(x + 1 + r.nextInt(7), y + FH, z + 1 + r.nextInt(7), r.nextBoolean() ? Blocks.SEA_LANTERN.getDefaultState() : LIGHT);
        for (int n = 0; n < 6; n++) set(x + 1 + r.nextInt(7), y, z + 1 + r.nextInt(7), Blocks.LAPIS_BLOCK.getDefaultState());
        set(x + 4, y + 1, z + 4, PILLAR); set(x + 4, y + 2, z + 4, GOLD);
        for (EnumFacing e : EnumFacing.HORIZONTALS) set(x + 4 + e.getXOffset(), y + 2, z + 4 + e.getZOffset(), Blocks.END_ROD.getDefaultState().withProperty(BlockDirectional.FACING, e));
        set(x + 4, y + 3, z + 4, Blocks.END_ROD.getDefaultState().withProperty(BlockDirectional.FACING, EnumFacing.UP));
        chest(x + 6, y + 1, z + 2, EnumFacing.WEST, LOOT_STARMAP);
    }

    void apothecary(int x, int y, int z, Random r) {
        for (int k = 1; k <= 7; k++) for (int dy = 1; dy <= 3; dy++) if (k != 4) set(x + k, y + dy, z + 1, SHELF);
        set(x + 2, y + 1, z + 3, Blocks.BREWING_STAND.getDefaultState()); set(x + 6, y + 1, z + 3, Blocks.BREWING_STAND.getDefaultState());
        set(x + 4, y + 1, z + 3, Blocks.CAULDRON.getDefaultState().withProperty(BlockCauldron.LEVEL, 3));
        for (int k = 2; k <= 6; k += 2) {
            set(x + k, y + 1, z + 6, ModContent.STAR_GRASS.getDefaultState());
            if (HBlocks.MANDRAKE != null) set(x + k, y + 2, z + 6, (k == 4 ? HBlocks.MANDRAKE : HBlocks.HENBANE).getDefaultState());
        }
        chest(x + 1 + (r.nextBoolean() ? 1 : 5), y + 1, z + 5, EnumFacing.NORTH, LOOT_APOTHECARY);
    }

    void enchant(int x, int y, int z, Random r) {
        fill(x + 1, y + 1, z + 1, x + 7, y + 1, z + 7, CARPET);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 2 || dx == 0) continue;
            set(x + 4 + dx, y + 1, z + 4 + dz, SHELF); set(x + 4 + dx, y + 2, z + 4 + dz, SHELF);
        }
        set(x + 4, y + 1, z + 4, Blocks.ENCHANTING_TABLE.getDefaultState());
        set(x + 1, y + 1, z + 1, Blocks.LAPIS_BLOCK.getDefaultState()); set(x + 7, y + 1, z + 7, Blocks.LAPIS_BLOCK.getDefaultState());
        if (r.nextInt(2) == 0) placeEnchantress(x + 2.5, y + 1, z + 6.5);
    }

    void gallery(int x, int y, int z, Random r, int f, int i, int j) {
        for (int k = 1; k <= 7; k++) set(x + k, y, z + 4, f == 0 ? SAND_C : BRICK());
        fill(x + 2, y + 1, z + 2, x + 6, y + 1, z + 6, CARPET);
        // shelves against the outer-room walls (not across doors)
        int[][] sides = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] s : sides) {
            int ni = i + s[0], nj = j + s[1];
            if (ni < 0 || nj < 0 || ni >= GRID || nj >= GRID || ring(ni, nj) != 0) continue;
            for (int k = 1; k <= 7; k++) for (int dy = 1; dy <= 3; dy++) {
                if (k == 4) continue;
                if (s[1] == -1) set(x + k, y + dy, z + 1, SHELF);
                if (s[1] == 1) set(x + k, y + dy, z + 7, SHELF);
                if (s[0] == -1) set(x + 1, y + dy, z + k, SHELF);
                if (s[0] == 1) set(x + 7, y + dy, z + k, SHELF);
            }
        }
        if (r.nextInt(3) == 0) placeGuard(x + 4.5, y + 1, z + 4.5, r);
        if (LTConfig.v130.peerReviewers && rnd(x + 3, y + 1, z + 3).nextInt(3) == 0) placeReviewer(x + 3.5, y + 1, z + 5.5);   // 1.3.0, own random
        if (r.nextInt(4) == 0) banner(x + 4, y + 4, z + 1, EnumFacing.SOUTH);
        // 1.2.1: Lady Babylon paintings above the shelves (outer-room walls), half of the walls
        for (int[] s : sides) {
            int ni = i + s[0], nj = j + s[1];
            if (ni < 0 || nj < 0 || ni >= GRID || nj >= GRID || ring(ni, nj) != 0) continue;
            Random pr = rnd(x + s[0], y + 4, z + s[1]);
            if (pr.nextInt(2) != 0) continue;
            EnumFacing face = s[1] == -1 ? EnumFacing.SOUTH : s[1] == 1 ? EnumFacing.NORTH : s[0] == -1 ? EnumFacing.EAST : EnumFacing.WEST;
            int px = s[0] == -1 ? x + 1 : s[0] == 1 ? x + 7 : x + 4, pz = s[1] == -1 ? z + 1 : s[1] == 1 ? z + 7 : z + 4;
            painting(px, y + 4, pz, face, lucifertotem.paint.Paintings.CLASSIC.get(pr.nextInt(lucifertotem.paint.Paintings.CLASSIC.size())));
        }
    }

    public static int paintingsHung = 0;
    /** hang it only if it and its whole wall lie in this populate window and nothing is in the way */
    void painting(int x, int y, int z, EnumFacing face, lucifertotem.paint.Paintings.Variant v) {
        lucifertotem.paint.EntityLTPainting e = new lucifertotem.paint.EntityLTPainting(w, new BlockPos(x, y, z), face, v, new Random(posSeed(x, y, z)));
        net.minecraft.util.math.AxisAlignedBB bb = e.getEntityBoundingBox();
        for (BlockPos q : BlockPos.getAllInBox(new BlockPos(bb.minX + 0.01, bb.minY + 0.01, bb.minZ + 0.01), new BlockPos(bb.maxX - 0.01, bb.maxY - 0.01, bb.maxZ - 0.01))) {
            BlockPos back = q.offset(face.getOpposite());
            if (!in(q.getX(), q.getY(), q.getZ()) || !in(back.getX(), back.getY(), back.getZ())) return;
            if (!w.isAirBlock(q) || !w.getBlockState(back).getMaterial().isSolid()) return;
        }
        if (!e.onValidSurface()) return;
        w.spawnEntity(e);
        paintingsHung++;
    }

    void stairs(int x, int y, int z) {
        for (int k = 1; k <= 7; k++) {
            for (int dz = 2; dz <= 3; dz++) {
                fill(x + k, y + 1, z + dz, x + k, y + k - 1, z + dz, BRICK());
                set(x + k, y + k, z + dz, stoneStair(EnumFacing.EAST));
                for (int dy = k + 1; dy <= k + 3; dy++) set(x + k, y + dy, z + dz, AIR);
            }
        }
        for (int k = 2; k <= 6; k++) { set(x + k, y + FH + 1, z + 1, FENCE); set(x + k, y + FH + 1, z + 4, FENCE); }
        set(x + 1, y + FH + 1, z + 2, FENCE); set(x + 1, y + FH + 1, z + 3, FENCE);
    }

    void entrance(int x, int y, int z, int i) {
        fill(x + 1, y + 1, z + 1, x + 7, y + 1, z + 7, AIR);
        if (i == 2) for (int dz = 0; dz <= 8; dz++) { set(x + 8, y, z + dz, SAND_C); set(x + 8, y + 1, z + dz, CARPET); }
        banner(i == 2 ? x + 1 : x + 7, y + 4, z + 3, i == 2 ? EnumFacing.EAST : EnumFacing.WEST);
        banner(i == 2 ? x + 1 : x + 7, y + 4, z + 5, i == 2 ? EnumFacing.EAST : EnumFacing.WEST);
        set(i == 2 ? x + 2 : x + 6, y + 1, z + 6, PILLAR); set(i == 2 ? x + 2 : x + 6, y + 2, z + 6, GOLD);
    }

    void study(int x, int y, int z, int i) {
        fill(x + 1, y + 1, z + 1, x + 7, y + 1, z + 7, Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.PURPLE));
        for (int k = 1; k <= 7; k++) for (int dy = 1; dy <= 4; dy++) set(x + k, y + dy, z + 1, SHELF);
        if (i == 2) { table(x + 5, y, z + 4); chest(x + 2, y + 1, z + 2, EnumFacing.SOUTH, LOOT); set(x + 2, y + 1, z + 6, Blocks.BREWING_STAND.getDefaultState()); }
        else { set(x + 3, y + 1, z + 3, WOOL); set(x + 3, y + 2, z + 3, ModContent.GLOW_CRYSTAL.getDefaultState()); chest(x + 6, y + 1, z + 2, EnumFacing.SOUTH, LOOT); }
        banner(x + 4, y + 4, z + 2, EnumFacing.SOUTH);
    }

    void atrium() {
        int x0 = cx0(2), z0 = cz0(2), cx = x0 + CELL, cz = z0 + CELL;
        // open the hall through both floors (keep a balcony ring 1 block wide on the upper floor)
        fill(x0 + 1, fy(1), z0 + 1, x0 + 2 * CELL - 1, fy(1), z0 + 2 * CELL - 1, AIR);
        fill(x0 + 1, fy(0) + 1, z0 + 1, x0 + 2 * CELL - 1, fy(2), z0 + 2 * CELL - 1, AIR);
        // floor: quartz with a golden star
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            boolean ray = (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz)) && Math.abs(dx) <= 6 && Math.abs(dz) <= 6;
            set(cx + dx, fy(0), cz + dz, ray ? (dx == 0 && dz == 0 ? GOLD : CHISELED) : QUARTZ);
        }
        // four great bookshelf towers and reading desks
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            fill(cx + 4 * sx, fy(0) + 1, cz + 4 * sz, cx + 4 * sx, fy(0) + 11, cz + 4 * sz, SHELF);
            set(cx + 4 * sx, fy(0) + 12, cz + 4 * sz, GOLD);
            table(cx + 2 * sx, fy(0), cz + 6 * sz);
        }
        for (EnumFacing e : EnumFacing.HORIZONTALS) banner(cx + e.getXOffset() * 7, fy(1) + 3, cz + e.getZOffset() * 7, e.getOpposite());
        chest(cx - 6, fy(0) + 1, cz - 1, EnumFacing.EAST, LOOT);
        chest(cx + 6, fy(0) + 1, cz + 1, EnumFacing.WEST, LOOT);
        set(cx, fy(0) + 1, cz, Blocks.ENCHANTING_TABLE.getDefaultState());
        // the dome
        int base = Y0 + 2 * FH, R = 8;
        fill(x0 + 1, base, z0 + 1, x0 + 2 * CELL - 1, base, z0 + 2 * CELL - 1, AIR);
        for (int dx = -R - 1; dx <= R + 1; dx++) for (int dz = -R - 1; dz <= R + 1; dz++) for (int dy = 0; dy <= R + 1; dy++) {
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < R - 0.5 || d >= R + 0.5) continue;
            boolean rib = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
            set(cx + dx, base + dy, cz + dz, rib ? QUARTZ : GLASS);
        }
        set(cx, base + R + 1, cz, GOLD);
        set(cx, base + R - 1, cz, LIGHT);
    }

    void roof() {
        int y = Y0 + 2 * FH, x1 = ox + SIZE - 1, z1 = oz + SIZE - 1;
        for (int k = 0; k < SIZE; k += 2) {
            set(ox + k, y + 1, oz, BRICK()); set(ox + k, y + 1, z1, BRICK()); set(ox, y + 1, oz + k, BRICK()); set(x1, y + 1, oz + k, BRICK());
        }
        for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
            if (!tower[i][j]) continue;
            int x = cx0(i), z = cz0(j), yt = fy(2);
            fill(x, yt + 1, z, x + CELL, yt + FH - 1, z, BRICK()); fill(x, yt + 1, z + CELL, x + CELL, yt + FH - 1, z + CELL, BRICK());
            fill(x, yt + 1, z, x, yt + FH - 1, z + CELL, BRICK()); fill(x + CELL, yt + 1, z, x + CELL, yt + FH - 1, z + CELL, BRICK());
            for (int c = 0; c <= CELL; c += CELL) for (int d = 0; d <= CELL; d += CELL) fill(x + c, yt + 1, z + d, x + c, yt + FH - 1, z + d, PILLAR);
            for (int dy = 2; dy <= 4; dy++) { set(x + 4, yt + dy, z, PANE); set(x + 4, yt + dy, z + CELL, PANE); set(x, yt + dy, z + 4, PANE); set(x + CELL, yt + dy, z + 4, PANE); }
            room(2, i, j);
            set(x + 1, yt + 2, z + 1, AIR); set(x + 1, yt + 3, z + 1, AIR);
            // a ladder up from the floor below
            for (int dy = fy(1) + 1; dy <= yt; dy++) set(x + 1, dy, z + 1, Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, EnumFacing.SOUTH));
            set(x + 1, yt + 1, z + 1, Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, EnumFacing.SOUTH));
            // stepped sandstone pyramid cap with a golden pyramidion
            for (int s = 0; s <= 4; s++) fill(x + s, yt + FH + s, z + s, x + CELL - s, yt + FH + s, z + CELL - s, s == 4 ? GOLD : SAND);
        }
    }

    void entranceOutside() {
        int mx = ox + 3 * CELL, z1 = oz + SIZE - 1;
        door(mx, z1, Y0, true, 3, 4);
        for (int k = -3; k <= 3; k++) {
            set(mx + k, Y0, z1 + 1, SAND_C); set(mx + k, Y0, z1 + 2, SAND);
            set(mx + k, Y0 - 1, z1 + 3, SAND); set(mx + k, Y0, z1 + 3, stoneStair(EnumFacing.NORTH));
        }
        for (int s = -1; s <= 1; s += 2) {
            fill(mx + 3 * s, Y0 + 1, z1 + 1, mx + 3 * s, Y0 + 5, z1 + 1, PILLAR);
            set(mx + 3 * s, Y0 + 6, z1 + 1, GOLD);
            banner(mx + 2 * s, Y0 + 4, z1 + 1, EnumFacing.SOUTH);
        }
        fill(mx - 3, Y0 + 6, z1 + 1, mx + 3, Y0 + 6, z1 + 1, SAND_C);
        // 1.3.2: the life-sized, purple-splashed Dionysus over the door, on the inner wall, looking into the entrance hall
        if (in(mx, Y0 + 5, z1 - 1)) lucifertotem.v132.Tall.hangIcon(w, new BlockPos(mx, Y0 + 5, z1 - 1), EnumFacing.NORTH);
    }

    // ------------------------------------------------------------ inhabitants
    void placeEnchantress(double x, double y, double z) {
        if (!in((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) return;
        mob(new EntityEnchantress(w), x, y, z);
        enchantresses++;
    }

    public static int reviewers = 0;
    void placeReviewer(double x, double y, double z) {
        if (!in((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) return;
        mob(new lucifertotem.v130.EntityPeerReviewer(w), x, y, z);
        reviewers++;
    }

    static final Item[] WEAPONS = {Items.IRON_SWORD, Items.IRON_AXE, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD};
    static final Item[][] ARMOUR = {{Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET}, {Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE},
        {Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS}, {Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS}};
    static final EntityEquipmentSlot[] SLOTS = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};

    void placeGuard(double x, double y, double z, Random r) {
        if (!in((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) return;
        EntityLiving g = r.nextBoolean() ? new EntityHusk(w) : new EntitySkeleton(w);
        mob(g, x, y, z);
        equipEnchanted(g, r);
        guards++;
    }

    /** random enchanted gear, dropped like an illager's (8.5 % per piece) */
    public static void equipEnchanted(EntityLiving g, Random r) {
        if (!(g instanceof EntitySkeleton)) g.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, EnchantmentHelper.addRandomEnchantment(r, new ItemStack(WEAPONS[r.nextInt(WEAPONS.length)]), 10 + r.nextInt(20), false));
        else g.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, EnchantmentHelper.addRandomEnchantment(r, new ItemStack(Items.BOW), 10 + r.nextInt(20), false));
        for (int s = 0; s < 4; s++) {
            if (r.nextInt(3) == 0) continue;
            Item[] opts = ARMOUR[s];
            g.setItemStackToSlot(SLOTS[s], EnchantmentHelper.addRandomEnchantment(r, new ItemStack(opts[r.nextInt(opts.length)]), 8 + r.nextInt(20), false));
        }
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) g.setDropChance(s, 0.085f);
    }
}
