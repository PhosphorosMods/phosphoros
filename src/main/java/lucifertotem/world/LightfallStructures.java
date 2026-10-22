package lucifertotem.world;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockRotatedPillar;
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
 * Dungeons of the Lightfall. Every structure fits inside the 16x16 populate window [bx..bx+15]x[bz..bz+15]
 * and every block write is bounds-checked against it, so generation never touches ungenerated chunks.
 *  - Light Shrine (peaceful ruin + chest)
 *  - Fallen Spire (ruined tower: two spawner floors, chest at the top)
 *  - Sunken Shrine (crypt hollowed inside an island, reached by a ladder shaft under a small roofed shrine)
 *  - Morningstar Citadel (rare walled fortress, four spawners, vault with the best loot)
 */
public class LightfallStructures {
    public static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();
    public static int outOfWindowWrites = 0, gorgons = 0;

    private final World w;
    private final Random r;
    private final int bx, bz;
    /** representative position of the structure just built (recorded in StructureIndex) */
    private BlockPos at;

    private LightfallStructures(World w, Random r, int bx, int bz) { this.w = w; this.r = r; this.bx = bx; this.bz = bz; }

    public static void tryGenerate(World w, Random r, int bx, int bz) {
        LTConfig.Structures c = LTConfig.structures;
        int roll = r.nextInt(1000);
        LightfallStructures s = new LightfallStructures(w, r, bx, bz);
        String made = null;
        int t = c.morningstarCitadel;
        if (roll < t) { if (s.citadel()) made = "morningstar_citadel"; }
        else if (roll < (t += c.fallenSpire)) { if (s.spire()) made = "fallen_spire"; }
        else if (roll < (t += c.sunkenShrine)) { if (s.crypt()) made = "sunken_shrine"; }
        else if (roll < (t += c.lightShrine)) { if (s.shrine()) made = "light_shrine"; }
        // 1.1.0 bands come after the original ones, so older structure rolls are unchanged
        else if (roll < (t += LTConfig.structures110.trophoniusPit)) { if (s.trophonius()) made = "trophonius_pit"; }
        else if (roll < (t += LTConfig.structures110.delphicVent)) { if (s.delphic()) made = "delphic_vent"; }
        else if (roll < (t += LTConfig.structures110.witchCircle)) { if (s.witchCircle()) made = "witch_circle"; }
        if (made != null) {
            COUNTS.merge(made, 1, Integer::sum);
            if (s.at != null) lucifertotem.hillman.StructureIndex.record(w, made, s.at);
            if (LTConfig.testHarness) LuciferTotem.LOG.info("[LT][GEN] {} at window {},{}", made, bx, bz);
        }
    }

    // ------------------------------------------------------------ helpers
    private IBlockState BRICK() { return ModContent.DUSKSTONE_BRICKS.getDefaultState(); }
    private IBlockState STONE() { return ModContent.DUSKSTONE.getDefaultState(); }
    private IBlockState weathered() { int n = r.nextInt(10); return n == 0 ? STONE() : n == 1 ? Blocks.PURPUR_BLOCK.getDefaultState() : BRICK(); }
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static IBlockState pillar() { return Blocks.PURPUR_PILLAR.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y); }

    private boolean in(int x, int y, int z) { return x >= bx && x < bx + 16 && z >= bz && z < bz + 16 && y > 0 && y < 255; }

    private void set(int x, int y, int z, IBlockState s) {
        if (!in(x, y, z)) { outOfWindowWrites++; return; }
        w.setBlockState(new BlockPos(x, y, z), s, 2);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState s) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) set(x, y, z, s);
    }

    private void spawner(int x, int y, int z, String mob) {
        if (!in(x, y, z)) { outOfWindowWrites++; return; }
        BlockPos p = new BlockPos(x, y, z);
        w.setBlockState(p, Blocks.MOB_SPAWNER.getDefaultState(), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityMobSpawner) ((TileEntityMobSpawner) te).getSpawnerBaseLogic().setEntityId(new ResourceLocation(mob));
    }

    private void chest(int x, int y, int z, EnumFacing f, ResourceLocation loot) {
        if (!in(x, y, z)) { outOfWindowWrites++; return; }
        BlockPos p = new BlockPos(x, y, z);
        w.setBlockState(p, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, f), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityChest) ((TileEntityChest) te).setLootTable(loot, r.nextLong());
    }

    private void ladder(int x, int y1, int y2, int z, EnumFacing facing) {
        for (int y = y1; y <= y2; y++) set(x, y, z, Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, facing));
    }

    /** fill air/water/plants under a footprint so it doesn't float (max depth) */
    private void foundation(int x1, int z1, int x2, int z2, int y, int maxDepth) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++)
            for (int d = 1; d <= maxDepth; d++) {
                BlockPos p = new BlockPos(x, y - d, z);
                if (!in(x, y - d, z)) break;
                IBlockState s = w.getBlockState(p);
                if (s.getMaterial().isSolid() && s.getBlock() != Blocks.LEAVES && s.getBlock() != Blocks.LOG) break;
                w.setBlockState(p, STONE(), 2);
            }
    }

    private int surface(int x, int z) {
        int y = LightfallChunkGenerator.topY(w, x, z, LightfallChunkGenerator.SEA_LEVEL + 1);
        while (y > LightfallChunkGenerator.SEA_LEVEL) {
            IBlockState s = w.getBlockState(new BlockPos(x, y, z));
            if (s.getBlock() == ModContent.STAR_GRASS || s.getBlock() == ModContent.DUSKSTONE) return y;
            if (s.getMaterial().isSolid() && s.getBlock() != Blocks.LEAVES && s.getBlock() != Blocks.LOG) return -1;
            y--; // skip plants / leaves / logs / crystals
        }
        return -1;
    }

    /** all corner surfaces present and within tol of centre; returns min corner surface or -1 */
    private int site(int cx, int cz, int half, int tol) {
        int c = surface(cx, cz);
        if (c < 60) return -1;
        int min = c;
        int[][] pts = {{-half, -half}, {half, -half}, {-half, half}, {half, half}};
        for (int[] p : pts) {
            int s = surface(cx + p[0], cz + p[1]);
            if (s < 0 || Math.abs(s - c) > tol) return -1;
            min = Math.min(min, s);
        }
        return min;
    }

    // ------------------------------------------------------------ Light Shrine (peaceful)
    private boolean shrine() {
        int c = bx + 8, d = bz + 8;
        int y = site(c, d, 3, 2);
        if (y < 0) return false;
        foundation(c - 3, d - 3, c + 3, d + 3, y, 6);
        fill(c - 3, y, d - 3, c + 3, y, d + 3, BRICK());
        fill(c - 3, y + 1, d - 3, c + 3, y + 5, d + 3, AIR);
        fill(c - 1, y, d - 1, c + 1, y, d + 1, Blocks.PURPUR_BLOCK.getDefaultState());
        int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        boolean lit = false;
        for (int[] k : corners) {
            int h = 1 + r.nextInt(4);
            for (int i = 1; i <= h; i++) set(c + k[0], y + i, d + k[1], pillar());
            if (h == 4 && !lit) { set(c + k[0], y + 5, d + k[1], ModContent.GLOW_CRYSTAL.getDefaultState()); lit = true; }
        }
        if (!lit) set(c + 3, y + 1 + 1, d + 3, ModContent.GLOW_CRYSTAL.getDefaultState());
        chest(c, y + 1, d, EnumFacing.SOUTH, LuciferTotem.LOOT_LIGHT_SHRINE);
        at = new BlockPos(c, y + 1, d);
        for (int i = 0; i < 4; i++) {
            int fx = c - 2 + r.nextInt(5), fz = d - 2 + r.nextInt(5);
            if (Math.abs(fx - c) == 2 || Math.abs(fz - d) == 2) {
                set(fx, y, fz, ModContent.STAR_GRASS.getDefaultState());
                set(fx, y + 1, fz, ModContent.FALLEN_FEATHER.getDefaultState());
            }
        }
        return true;
    }

    // ------------------------------------------------------------ Fallen Spire
    private boolean spire() {
        int c = bx + 4 + r.nextInt(8), d = bz + 4 + r.nextInt(8);
        int base = site(c, d, 3, 3);
        if (base < 0) return false;
        int top = base + 20;
        foundation(c - 3, d - 3, c + 3, d + 3, base, 14);
        // shell
        for (int y = base; y <= top; y++)
            for (int x = c - 3; x <= c + 3; x++) for (int z = d - 3; z <= d + 3; z++) {
                boolean wall = x == c - 3 || x == c + 3 || z == d - 3 || z == d + 3;
                boolean corner = (x == c - 3 || x == c + 3) && (z == d - 3 || z == d + 3);
                if (wall) set(x, y, z, corner ? pillar() : weathered());
                else set(x, y, z, (y - base) % 5 == 0 ? BRICK() : AIR);
            }
        // ladder shaft (east side, centred) through every floor
        for (int f = 1; f <= 3; f++) set(c + 2, base + 5 * f, d, AIR);
        ladder(c + 2, base + 1, top - 1, d, EnumFacing.WEST);
        // door (north) on the ground floor
        fill(c, base + 1, d - 3, c, base + 2, d - 3, AIR);
        set(c, base, d - 4, BRICK());
        // top room windows (top floor only, so spawner floors stay dark)
        set(c, top - 3, d + 3, AIR); set(c - 3, top - 3, d, AIR); set(c, top - 3, d - 3, AIR);
        // spawners on floors 1 and 2
        spawner(c - 1, base + 6, d + 1, r.nextBoolean() ? "minecraft:skeleton" : "minecraft:stray");
        String upper = r.nextInt(3) == 0 ? "minecraft:witch" : "minecraft:vex";
        spawner(c - 1, base + 11, d + 1, r.nextBoolean() ? lucifertotem.AetherCompat.spawnerMob(upper) : upper); // fallen angels (Aether Valkyries) when installed
        // treasure at the top
        chest(c - 2, base + 16, d + 2, EnumFacing.SOUTH, LuciferTotem.LOOT_SPIRE);
        at = new BlockPos(c, base + 1, d);
        // ruined parapet + broken spire tip with a crystal
        for (int x = c - 3; x <= c + 3; x++) for (int z = d - 3; z <= d + 3; z++) {
            boolean wall = x == c - 3 || x == c + 3 || z == d - 3 || z == d + 3;
            if (wall && ((x + z) & 1) == 0) { int h = r.nextInt(3); for (int i = 1; i <= h; i++) set(x, top + i, z, weathered()); }
        }
        int tip = 2 + r.nextInt(4);
        for (int i = 1; i <= tip; i++) set(c, top + i, d, pillar());
        set(c, top + tip + 1, d, ModContent.GLOW_CRYSTAL.getDefaultState());
        return true;
    }

    // ------------------------------------------------------------ Sunken Shrine (crypt)
    private boolean crypt() {
        int c = bx + 4 + r.nextInt(8), d = bz + 4 + r.nextInt(8);
        int y = surface(c, d);
        if (y < 60) return false;
        int fy = y - 12;
        for (int k = 1; k <= 13; k++) if (w.isAirBlock(new BlockPos(c, y - k, d))) return false; // island too thin
        // room shell 9x9x7, interior 7x7x5
        for (int yy = fy; yy <= fy + 6; yy++)
            for (int x = c - 4; x <= c + 4; x++) for (int z = d - 4; z <= d + 4; z++) {
                boolean shell = yy == fy || yy == fy + 6 || x == c - 4 || x == c + 4 || z == d - 4 || z == d + 4;
                set(x, yy, z, shell ? (yy == fy && ((x + z) & 1) == 0 ? Blocks.PURPUR_BLOCK.getDefaultState() : weathered()) : AIR);
            }
        for (int[] k : new int[][] {{-3, -3}, {3, 3}, {-3, 3}}) for (int i = 1; i <= 5; i++) set(c + k[0], fy + i, d + k[1], pillar());
        String mob; int roll = r.nextInt(100);
        mob = roll < 45 ? "minecraft:zombie" : roll < 80 ? "minecraft:witch" : "minecraft:enderman";
        spawner(c, fy + 1, d, mob);
        chest(c - 3, fy + 1, d, EnumFacing.EAST, LuciferTotem.LOOT_CRYPT);
        if (r.nextBoolean()) chest(c, fy + 1, d + 3, EnumFacing.NORTH, LuciferTotem.LOOT_CRYPT);
        // shaft in the NE interior corner, ladder against the east wall, up to the surface
        int sx = c + 3, sz = d - 3;
        for (int yy = fy + 1; yy <= y; yy++) {
            set(sx, yy, sz, AIR);
            if (yy > fy + 5) { // line the shaft above the room
                set(sx + 1, yy, sz, BRICK()); set(sx - 1, yy, sz, BRICK()); set(sx, yy, sz - 1, BRICK()); set(sx, yy, sz + 1, BRICK());
            }
        }
        ladder(sx, fy + 1, y, sz, EnumFacing.WEST);
        at = new BlockPos(sx, y + 1, sz);
        // roofed shrine over the shaft (keeps direct skylight out of the crypt)
        for (int[] k : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) for (int i = 1; i <= 3; i++) set(sx + k[0], y + i, sz + k[1], pillar());
        fill(sx - 1, y + 1, sz - 1, sx + 1, y + 3, sz + 1, AIR);
        for (int[] k : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) for (int i = 1; i <= 3; i++) set(sx + k[0], y + i, sz + k[1], pillar());
        fill(sx - 1, y + 4, sz - 1, sx + 1, y + 4, sz + 1, BRICK());
        set(sx, y + 5, sz, ModContent.GLOW_CRYSTAL.getDefaultState());
        set(sx, y, sz - 1, BRICK()); set(sx - 1, y, sz, BRICK()); set(sx, y, sz + 1, BRICK()); set(sx + 1, y, sz, BRICK());
        lucifertotem.v130.Stage5.sunkenChapel(w, bx, bz, c, fy, d);     // 1.3.0: some become the Hydra's Sunken Chapel
        return true;
    }

    // ------------------------------------------------------------ Morningstar Citadel
    private boolean citadel() {
        int x0 = bx, z0 = bz, x1 = bx + 14, z1 = bz + 14, cx = bx + 7, cz = bz + 7;
        int g = site(cx, cz, 7, 6);
        boolean floating = g < 0;
        if (floating && r.nextInt(3) != 0) return false; // floating fallback only a third of the time
        if (floating) g = 92;
        if (floating) { // inverted pyramid of duskstone beneath
            for (int k = 1; k <= 7; k++) fill(x0 + k, g - k, z0 + k, x1 - k, g - k, z1 - k, STONE());
            set(cx, g - 8, cz, ModContent.GLOW_CRYSTAL.getDefaultState());
        } else {
            foundation(x0, z0, x1, z1, g, 20);
        }
        fill(x0, g, z0, x1, g, z1, BRICK());
        fill(x0, g + 1, z0, x1, g + 13, z1, AIR);
        // outer walls + crenellations
        for (int y = g + 1; y <= g + 12; y++)
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
                if (x == x0 || x == x1 || z == z0 || z == z1) set(x, y, z, weathered());
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            if ((x == x0 || x == x1 || z == z0 || z == z1) && ((x + z) & 1) == 0) set(x, g + 13, z, BRICK());
        // corner towers (solid 3x3) crowned with crystals
        for (int[] k : new int[][] {{x0, z0}, {x1 - 2, z0}, {x0, z1 - 2}, {x1 - 2, z1 - 2}}) {
            fill(k[0], g + 1, k[1], k[0] + 2, g + 17, k[1] + 2, BRICK());
            fill(k[0] + 1, g + 1, k[1] + 1, k[0] + 1, g + 17, k[1] + 1, pillar());
            set(k[0] + 1, g + 18, k[1] + 1, ModContent.GLOW_CRYSTAL.getDefaultState());
        }
        // mid floor and roof
        fill(x0 + 1, g + 6, z0 + 1, x1 - 1, g + 6, z1 - 1, BRICK());
        fill(x0 + 1, g + 12, z0 + 1, x1 - 1, g + 12, z1 - 1, BRICK());
        // entrance (south) + approach step
        fill(cx, g + 1, z1, cx, g + 3, z1, AIR);
        // central vault (5x5) on the ground floor
        for (int y = g + 1; y <= g + 5; y++)
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
                boolean wall = x == cx - 2 || x == cx + 2 || z == cz - 2 || z == cz + 2;
                set(x, y, z, wall ? BRICK() : AIR);
            }
        fill(cx, g + 1, cz - 2, cx, g + 2, cz - 2, AIR); // vault door (north side)
        chest(cx - 1, g + 1, cz + 1, EnumFacing.NORTH, LuciferTotem.LOOT_CITADEL);
        chest(cx + 1, g + 1, cz + 1, EnumFacing.NORTH, LuciferTotem.LOOT_CITADEL);
        // ladder to the upper hall (west wall), hole in mid floor only (roof stays sealed → halls stay dark)
        set(x0 + 1, g + 6, cz, AIR);
        ladder(x0 + 1, g + 1, g + 6, cz, EnumFacing.EAST);
        // spawners: ground hall blaze (near the door, blazes ignore light) + skeleton, upper hall vex + witch
        spawner(x1 - 3, g + 1, z1 - 3, "minecraft:blaze");
        spawner(x0 + 3, g + 1, z0 + 3, "minecraft:skeleton");
        spawner(x0 + 3, g + 7, z1 - 3, lucifertotem.AetherCompat.spawnerMob("minecraft:vex"));
        spawner(x1 - 3, g + 7, z0 + 3, r.nextBoolean() ? "minecraft:witch" : "minecraft:enderman");
        // upper hall reliquary chest
        chest(cx, g + 7, cz, EnumFacing.SOUTH, LuciferTotem.LOOT_CITADEL);
        at = new BlockPos(cx, g + 1, z1);
        if (LTConfig.gorgonInCitadel) { // the Gorgon keeps the upper hall
            lucifertotem.hillman.EntityGorgon gorgon = new lucifertotem.hillman.EntityGorgon(w);
            gorgon.setLocationAndAngles(cx + 3.5, g + 7, cz + 0.5, r.nextFloat() * 360f, 0f);
            gorgon.setHomePosAndDistance(new BlockPos(cx, g + 7, cz), 7);
            gorgon.onInitialSpawn(w.getDifficultyForLocation(new BlockPos(cx, g + 7, cz)), null);
            w.spawnEntity(gorgon);
            gorgons++;
        }
        // roof altar: the Morningstar beacon
        for (int i = 13; i <= 16; i++) set(cx, g + i, cz, pillar());
        set(cx, g + 17, cz, ModContent.GLOW_CRYSTAL.getDefaultState());
        return true;
    }

    // ------------------------------------------------------------ 1.1.0: Oracle of Trophonius
    /** a small shrine with the altar on top; the pitch-black maze of the snake pit is cut into the bedrock-near
     *  duskstone at y 5..8 right under the window (arrival in the NW cell, exit in the SE cell). */
    private boolean trophonius() {
        int c = bx + 8, d = bz + 8;
        int y = site(c, d, 2, 2);
        if (y < 0) return false;
        foundation(c - 2, d - 2, c + 2, d + 2, y, 6);
        fill(c - 2, y, d - 2, c + 2, y, d + 2, BRICK());
        fill(c - 2, y + 1, d - 2, c + 2, y + 4, d + 2, AIR);
        for (int[] k : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) for (int i = 1; i <= 3; i++) set(c + k[0], y + i, d + k[1], pillar());
        fill(c - 2, y + 4, d - 2, c + 2, y + 4, d + 2, BRICK());
        fill(c - 1, y + 4, d - 1, c + 1, y + 4, d + 1, AIR); // open oculus
        set(c, y + 1, d, lucifertotem.hillman.HBlocks.ALTAR.getDefaultState());
        set(c - 1, y, d, Blocks.OBSIDIAN.getDefaultState()); set(c + 1, y, d, Blocks.OBSIDIAN.getDefaultState());
        // the maze: 7x7 cells of 1 wide corridors at y 6..7 (floor 5, ceiling 8), all solid duskstone bricks around
        int x0 = bx + 1, z0 = bz + 1, fy = lucifertotem.hillman.Trophonius.MAZE_FLOOR;
        fill(bx, fy, bz, bx + 14, fy + 3, bz + 14, BRICK());
        boolean[][] seen = new boolean[7][7];
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[] {0, 0}); seen[0][0] = true;
        carve(x0, fy, z0);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            java.util.List<int[]> opts = new java.util.ArrayList<>();
            for (int[] dd : dirs) { int nx = cur[0] + dd[0], nz = cur[1] + dd[1]; if (nx >= 0 && nz >= 0 && nx < 7 && nz < 7 && !seen[nx][nz]) opts.add(dd); }
            if (opts.isEmpty()) { stack.pop(); continue; }
            int[] dd = opts.get(r.nextInt(opts.size()));
            int nx = cur[0] + dd[0], nz = cur[1] + dd[1];
            carve(x0 + cur[0] * 2 + dd[0], fy, z0 + cur[1] * 2 + dd[1]);
            carve(x0 + nx * 2, fy, z0 + nz * 2);
            seen[nx][nz] = true;
            stack.push(new int[] {nx, nz});
        }
        set(x0 + 12, fy + 1, z0 + 12, lucifertotem.hillman.HBlocks.EXIT.getDefaultState());
        set(x0 + 12, fy + 2, z0 + 12, lucifertotem.hillman.HBlocks.EXIT.getDefaultState());
        at = new BlockPos(c, y + 1, d);
        return true;
    }

    private void carve(int x, int fy, int z) { set(x, fy + 1, z, AIR); set(x, fy + 2, z, AIR); }

    // ------------------------------------------------------------ 1.1.0: Delphic vent with the Pythia
    private boolean delphic() {
        int c = bx + 8, d = bz + 8;
        int y = site(c, d, 3, 2);
        if (y < 0) return false;
        foundation(c - 3, d - 3, c + 3, d + 3, y, 6);
        fill(c - 3, y, d - 3, c + 3, y, d + 3, BRICK());
        fill(c - 3, y + 1, d - 3, c + 3, y + 6, d + 3, AIR);
        // six columns in a ring, a broken architrave
        for (int[] k : new int[][] {{-3, -1}, {-3, 2}, {3, -1}, {3, 2}, {-1, 3}, {1, 3}}) {
            int h = 3 + r.nextInt(3);
            for (int i = 1; i <= h; i++) set(c + k[0], y + i, d + k[1], Blocks.QUARTZ_BLOCK.getStateFromMeta(2));
        }
        set(c, y, d, lucifertotem.hillman.HBlocks.VENT.getDefaultState());
        set(c - 1, y, d - 1, Blocks.QUARTZ_BLOCK.getDefaultState()); set(c + 1, y, d - 1, Blocks.QUARTZ_BLOCK.getDefaultState());
        // laurel offering chest at the back
        chest(c, y + 1, d + 2, EnumFacing.NORTH, LuciferTotem.LOOT_DELPHI);
        lucifertotem.hillman.EntityPythia py = new lucifertotem.hillman.EntityPythia(w);
        py.setLocationAndAngles(c + 0.5, y + 1, d - 0.9, 180f, 0f); // beside the vent, facing north (the visitor)
        py.rotationYawHead = 180f; py.renderYawOffset = 180f;
        w.spawnEntity(py);
        lucifertotem.v130.Stage5.lair(w, bx, bz, c, y, d);              // 1.3.0: the Drakaina's lair under new vents
        at = new BlockPos(c, y + 1, d);
        return true;
    }

    // ------------------------------------------------------------ 1.1.0: Thessalian witch circle
    private boolean witchCircle() {
        int c = bx + 8, d = bz + 8;
        int y = site(c, d, 5, 3);
        if (y < 0) return false;
        // standing stones on a ring of radius 5
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 9;
            int x = c + (int) Math.round(Math.cos(a) * 5), z = d + (int) Math.round(Math.sin(a) * 5);
            int sy = surface(x, z);
            if (sy < 0) continue;
            int h = 2 + r.nextInt(2);
            for (int k = 1; k <= h; k++) set(x, sy + k, z, k == h && r.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : STONE());
        }
        set(c, y, d, Blocks.OBSIDIAN.getDefaultState());
        set(c, y + 1, d, AIR);
        BlockPos center = new BlockPos(c, y + 1, d);
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2 / 3;
            lucifertotem.hillman.EntityThessalianWitch wi = new lucifertotem.hillman.EntityThessalianWitch(w);
            wi.setLocationAndAngles(c + 0.5 + Math.cos(a) * 2.5, y + 1, d + 0.5 + Math.sin(a) * 2.5, r.nextFloat() * 360, 0);
            wi.setCenter(center);
            w.spawnEntity(wi);
        }
        at = center;
        return true;
    }
}
