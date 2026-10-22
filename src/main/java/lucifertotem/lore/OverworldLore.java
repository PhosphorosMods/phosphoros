package lucifertotem.lore;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.world.LightfallChunkGenerator;
import lucifertotem.world.PortalLogic;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.*;

/**
 * 1.2.0 Overworld structures: the wrecked Alexandrian galley (ocean/beach chunks) and the Fallen Altar (land chunks).
 * Whether a chunk holds one is a pure function of the seed and the chunk's biome, so the totem's whispers can point
 * at places that have not been generated yet; every built one is also recorded in the StructureIndex.
 */
public class OverworldLore implements IWorldGenerator {
    public static final ResourceLocation LOOT_GALLEY = new ResourceLocation(LuciferTotem.MODID, "chests/alexandrian_galley");
    public static final ResourceLocation LOOT_ALTAR = new ResourceLocation(LuciferTotem.MODID, "chests/fallen_altar");
    public static final String GALLEY = "alexandrian_galley", ALTAR = "fallen_altar";
    static final long SALT_GALLEY = 0x6A11E7L, SALT_ALTAR = 0xA17A2L;
    public static int galleys = 0, altars = 0;

    // ------------------------------------------------------------ where
    static boolean wet(Biome b) { return BiomeDictionary.hasType(b, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(b, BiomeDictionary.Type.BEACH); }
    static boolean dry(Biome b) { return !wet(b) && !BiomeDictionary.hasType(b, BiomeDictionary.Type.RIVER) && !BiomeDictionary.hasType(b, BiomeDictionary.Type.WATER); }

    /** window centre of chunk (cx, cz): structures sit inside the populate window [cx*16+8, cx*16+24) */
    static BlockPos centre(int cx, int cz) { return new BlockPos(cx * 16 + 16, 64, cz * 16 + 16); }

    public static boolean candidate(World w, String type, int cx, int cz) {
        int chance = GALLEY.equals(type) ? LTConfig.lore.galleyChance : LTConfig.lore.altarChance;
        if (chance <= 0) return false;
        Random r = LightfallChunkGenerator.chunkRandom(w.getSeed(), cx, cz, GALLEY.equals(type) ? SALT_GALLEY : SALT_ALTAR);
        if (r.nextInt(chance) != 0) return false;
        Biome b = w.getBiomeProvider().getBiome(centre(cx, cz));
        if (GALLEY.equals(type)) return wet(b);
        return dry(b) && !candidate(w, GALLEY, cx, cz);
    }

    @Override
    public void generate(Random random, int cx, int cz, World w, IChunkGenerator gen, IChunkProvider prov) {
        if (w.isRemote || w.provider.getDimension() != 0 || !LuciferTotem.dimRegistered) return;
        try {
            if (candidate(w, GALLEY, cx, cz)) { BlockPos p = Galley.build(w, cx, cz); if (p != null) { galleys++; StructureIndex.record(w, GALLEY, p); LuciferTotem.LOG.debug("[LT][GEN] alexandrian_galley at {}", p); } }
            else if (candidate(w, ALTAR, cx, cz)) { BlockPos p = Altar.build(w, cx, cz); if (p != null) { altars++; StructureIndex.record(w, ALTAR, p); LuciferTotem.LOG.debug("[LT][GEN] fallen_altar at {}", p); } }
        } catch (Throwable t) { LuciferTotem.LOG.error("[LT] Overworld lore structure failed at chunk {},{}", cx, cz, t); }
    }

    /** nearest built or predicted site of a type (predicted ones only in chunks not populated yet) */
    public static BlockPos nearest(WorldServer w, String type, BlockPos from, int radiusChunks) {
        BlockPos best = null; double bd = Double.MAX_VALUE;
        for (long l : StructureIndex.get(w).all(type)) {
            BlockPos p = BlockPos.fromLong(l); double d = sq(p, from);
            if (d < bd && d <= (radiusChunks * 16.0) * (radiusChunks * 16.0)) { bd = d; best = p; }
        }
        int fcx = from.getX() >> 4, fcz = from.getZ() >> 4;
        for (int r = 0; r <= radiusChunks; r++) {
            if ((r * 16.0 - 24) * (r * 16.0 - 24) > bd) break;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int cx = fcx + dx, cz = fcz + dz;
                if (!candidate(w, type, cx, cz)) continue;
                if (w.isChunkGeneratedAt(cx, cz) && w.getChunk(cx, cz).isTerrainPopulated()) continue; // built ones are in the index
                BlockPos c = centre(cx, cz); double d = sq(c, from);
                if (d < bd) { bd = d; best = c; }
            }
        }
        return best;
    }

    static double sq(BlockPos a, BlockPos b) { double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ(); return dx * dx + dz * dz; }

    // ------------------------------------------------------------ helpers
    static boolean ground(IBlockState s) {
        Material m = s.getMaterial();
        return m.isSolid() && !m.isLiquid() && m != Material.LEAVES && m != Material.WOOD && m != Material.PLANTS && m != Material.VINE && m != Material.CACTUS && m != Material.GOURD && m != Material.SNOW;
    }

    /** highest ground block (ignoring trees, plants and water) at x,z; water depth above it in out[0] */
    static int groundY(World w, int x, int z, int[] water) {
        int y = w.getHeight(new BlockPos(x, 0, z)).getY();
        int depth = 0;
        for (; y > 1; y--) {
            BlockPos p = new BlockPos(x, y, z);
            IBlockState s = w.getBlockState(p);
            if (s.getMaterial() == Material.WATER) { depth++; continue; }
            if (ground(s)) break;
        }
        if (water != null) water[0] = depth;
        return y;
    }

    static void chest(World w, BlockPos p, EnumFacing facing, ResourceLocation loot, Random r, net.minecraft.item.ItemStack extra) {
        w.setBlockState(p, Blocks.CHEST.getDefaultState().withProperty(net.minecraft.block.BlockChest.FACING, facing), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntityChest) {
            TileEntityChest c = (TileEntityChest) te;
            c.setLootTable(loot, r.nextLong());
            if (extra != null) c.setInventorySlotContents(13, extra);
        }
    }

    static IBlockState planks(BlockPlanks.EnumType t) { return Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, t); }
    static IBlockState wool(EnumDyeColor c) { return Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, c); }
    static IBlockState carpet(EnumDyeColor c) { return Blocks.CARPET.getDefaultState().withProperty(net.minecraft.block.BlockCarpet.COLOR, c); }

    // ------------------------------------------------------------ the galley
    public static class Galley {
        /** half-width of the hull at station x (0 = stern, 14 = bow) */
        static final int[] HW = {1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0};

        public static BlockPos build(World w, int cx, int cz) {
            Random r = LightfallChunkGenerator.chunkRandom(w.getSeed(), cx, cz, SALT_GALLEY ^ 0x77L);
            boolean alongX = r.nextBoolean();
            int ox = cx * 16 + 8, oz = cz * 16 + 8;
            int[] water = new int[1];
            int gy = groundY(w, ox + 8, oz + 8, water);
            if (gy < 30) return null;
            int sea = w.getSeaLevel();
            boolean ocean = water[0] > 0;
            int y0 = ocean ? gy : gy;            // keel bedded in the sand / sea floor
            return build(w, ox, y0, oz, alongX, r, ocean, sea);
        }

        /** builds the wreck in the 16x16 square starting at ox,oz; keel at y0. Returns the chest position. */
        public static BlockPos build(World w, int ox, int y0, int oz, boolean alongX, Random r, boolean ocean, int sea) {
            IBlockState hull = planks(BlockPlanks.EnumType.SPRUCE), dark = planks(BlockPlanks.EnumType.DARK_OAK);
            IBlockState keel = Blocks.LOG2.getDefaultState().withProperty(net.minecraft.block.BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK).withProperty(BlockLog.LOG_AXIS, alongX ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.Z);
            IBlockState fence = Blocks.SPRUCE_FENCE.getDefaultState(), oarFence = Blocks.DARK_OAK_FENCE.getDefaultState();
            int list = r.nextBoolean() ? 1 : -1;         // the side she lists to: that side sits one lower
            int brk = 5 + r.nextInt(4);                  // where the hull broke
            Map<BlockPos, IBlockState> m = new LinkedHashMap<>();
            for (int x = 0; x <= 14; x++) {
                int hw = HW[x];
                for (int z = -hw; z <= hw; z++) {
                    int dy = (z * list > 0) ? -1 : 0;
                    put(m, x, dy, z, Math.abs(z) == 0 ? keel : dark);                       // bottom
                    for (int y = 1; y <= 2 + (x <= 1 ? 1 : 0); y++) {
                        boolean side = Math.abs(z) == hw;
                        if (side) {
                            boolean hole = (x >= brk && x <= brk + 1 && y <= 2) || r.nextInt(9) == 0;
                            if (!hole) put(m, x, y + dy, z, hull);
                        } else put(m, x, y + dy, z, null);                                         // open hold: air or water
                    }
                }
                // oars: dark fences sticking out of the oar ports, some snapped
                if (x >= 3 && x <= 11 && x % 2 == 1) for (int s = -1; s <= 1; s += 2) if (r.nextInt(3) != 0) {
                    int dy = (s * list > 0) ? -1 : 0;
                    put(m, x, 1 + dy, s * (hw + 1), oarFence);
                    if (r.nextBoolean()) put(m, x, dy, s * (hw + 2), oarFence);
                }
            }
            // deck planking fore and aft (rowers' benches between)
            for (int x = 0; x <= 2; x++) for (int z = -HW[x] + 1; z <= HW[x] - 1; z++) put(m, x, 2 + (z * list > 0 ? -1 : 0), z, hull);
            for (int x = 11; x <= 13; x++) for (int z = -HW[x] + 1; z <= HW[x] - 1; z++) put(m, x, 2, z, hull);
            for (int x = 4; x <= 10; x += 2) if (x < brk || x > brk + 1) for (int z = -1; z <= 1; z++) put(m, x, 1 + (z * list > 0 ? -1 : 0), z, Blocks.WOODEN_SLAB.getDefaultState().withProperty(net.minecraft.block.BlockWoodSlab.VARIANT, BlockPlanks.EnumType.SPRUCE));
            // upswept stern post and bow, bronze ram
            for (int y = 1; y <= 5; y++) put(m, 0, y + (y >= 4 ? 0 : 0), 0, y >= 4 ? fence : dark);
            put(m, 14, 1, 0, dark); put(m, 14, 2, 0, dark); put(m, 14, 3, 0, fence);
            put(m, 15, 0, 0, ModContent.HALO_BRASS.getDefaultState());
            // broken mast and yard with purple sail scraps
            int mx = brk - 2 > 3 ? brk - 2 : 4;
            for (int y = 1; y <= 4; y++) put(m, mx, y, 0, Blocks.LOG.getDefaultState().withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y));
            put(m, mx, 5, 0, fence);
            for (int z = -3; z <= 2; z++) { put(m, mx + 2 + (z + 3) / 3, 2, z, fence); }
            for (int i = 0; i < 9; i++) {
                int sx = mx - 1 + r.nextInt(6), sz = r.nextInt(5) - 2;
                int hw = sx >= 0 && sx <= 14 ? HW[sx] : 0;
                if (Math.abs(sz) > hw) continue;
                put(m, sx, i < 4 ? 3 : 2 + (sz * list > 0 ? -1 : 0), sz, i < 5 ? wool(EnumDyeColor.PURPLE) : carpet(EnumDyeColor.PURPLE));
            }
            put(m, mx + 1, 3, 1, wool(EnumDyeColor.PURPLE)); put(m, mx + 2, 3, -1, wool(EnumDyeColor.MAGENTA));
            // amphorae on the foredeck
            put(m, 12, 3, 0, Blocks.FLOWER_POT.getDefaultState()); put(m, 11, 3, -1, Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.ORANGE));
            put(m, 12, 3, 1, Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.BROWN));
            // place
            for (Map.Entry<BlockPos, IBlockState> e : m.entrySet()) {
                BlockPos lp = e.getKey();
                BlockPos p = alongX ? new BlockPos(ox + lp.getX(), y0 + lp.getY(), oz + 8 + lp.getZ()) : new BlockPos(ox + 8 + lp.getZ(), y0 + lp.getY(), oz + lp.getX());
                IBlockState s = e.getValue();
                if (s == null) s = ocean && p.getY() <= sea - 1 ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
                else if (s.getBlock() == Blocks.CARPET && ocean && p.getY() <= sea - 1) s = wool(EnumDyeColor.PURPLE); // no carpets underwater
                else if (s.getBlock() == Blocks.FLOWER_POT && ocean && p.getY() <= sea - 1) s = Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.ORANGE);
                w.setBlockState(p, s, 2);
            }
            // the captain's chest in the stern, with the Lecture Notes
            BlockPos lc = new BlockPos(1, 1, 0);
            BlockPos cp = alongX ? new BlockPos(ox + lc.getX(), y0 + lc.getY(), oz + 8) : new BlockPos(ox + 8, y0 + lc.getY(), oz + lc.getX());
            chest(w, cp, alongX ? EnumFacing.EAST : EnumFacing.SOUTH, LOOT_GALLEY, r, LoreBooks.notes());
            return cp;
        }

        static void put(Map<BlockPos, IBlockState> m, int x, int y, int z, IBlockState s) { m.put(new BlockPos(x, y, z), s); }
    }

    // ------------------------------------------------------------ the Fallen Altar
    public static class Altar {
        public static BlockPos build(World w, int cx, int cz) {
            Random r = LightfallChunkGenerator.chunkRandom(w.getSeed(), cx, cz, SALT_ALTAR ^ 0x99L);
            int ox = cx * 16 + 12, oz = cz * 16 + 16;
            int[] water = new int[1];
            int gy = groundY(w, ox + 2, oz, water);
            if (water[0] > 0 || gy < 4 || gy > 240) return null;
            // level enough? (no more than 3 blocks of relief under the platform)
            int lo = gy, hi = gy;
            for (int dx = -2; dx <= 5; dx += 7) for (int dz = -2; dz <= 2; dz += 4) { int y = groundY(w, ox + dx, oz + dz, null); lo = Math.min(lo, y); hi = Math.max(hi, y); }
            if (hi - lo > 4) return null;
            return build(w, new BlockPos(ox, gy + 1, oz), r.nextBoolean() ? EnumFacing.Axis.X : EnumFacing.Axis.Z, r);
        }

        /** frame base (u=0, v=0) at b; the platform is one below. Returns the frame base. */
        public static BlockPos build(World w, BlockPos b, EnumFacing.Axis a, Random r) {
            IBlockState brick = ModContent.DUSKSTONE_BRICKS.getDefaultState(), cracked = ModContent.CRACKED_DUSKSTONE_BRICKS.getDefaultState(), stone = ModContent.DUSKSTONE.getDefaultState();
            // platform + footing down to the ground, clear the air above
            for (int u = -2; u <= 5; u++) for (int k = -2; k <= 2; k++) {
                boolean edge = u == -2 || u == 5 || Math.abs(k) == 2;
                if (edge && r.nextInt(4) == 0) continue;                      // ragged edge
                BlockPos p = PortalLogic.at(b, a, u, -1, k);
                float f = r.nextFloat();
                w.setBlockState(p, f < 0.5f ? brick : f < 0.8f ? cracked : stone, 2);
                for (int d = 1; d <= 4; d++) {
                    BlockPos q = p.down(d);
                    if (ground(w.getBlockState(q))) break;
                    w.setBlockState(q, stone, 2);
                }
                for (int v = 0; v <= 6; v++) {
                    BlockPos q = PortalLogic.at(b, a, u, v, k);
                    if (!w.isAirBlock(q)) w.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
                }
            }
            // the frame, then break it: 2-4 cracked stones and 1-3 missing ones (the upper ones fell first)
            List<int[]> cells = new ArrayList<>();
            for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) if (AltarLogic.frameCell(u, v)) cells.add(new int[]{u, v});
            for (int[] c : cells) w.setBlockState(PortalLogic.at(b, a, c[0], c[1], 0), brick, 2);
            Collections.shuffle(cells, r);
            Map<int[], Integer> key = new IdentityHashMap<>();
            for (int[] c : cells) key.put(c, -2 * c[1] + r.nextInt(5));   // mostly top-first
            cells.sort(Comparator.comparingInt(key::get));
            int missing = 1 + r.nextInt(3), crack = 2 + r.nextInt(3), i = 0;
            for (; i < missing; i++) w.setBlockState(PortalLogic.at(b, a, cells.get(i)[0], cells.get(i)[1], 0), Blocks.AIR.getDefaultState(), 2);
            Collections.shuffle(cells.subList(missing, cells.size()), r);
            for (int j = 0; j < crack; j++, i++) w.setBlockState(PortalLogic.at(b, a, cells.get(i)[0], cells.get(i)[1], 0), cracked, 2);
            // the fallen stones lie at its feet
            for (int j = 0; j < missing; j++) w.setBlockState(PortalLogic.at(b, a, -1 + r.nextInt(6), 0, (r.nextBoolean() ? 1 : -1) * (1 + r.nextInt(2))), cracked, 2);
            // stumps of two side pillars, glow crystals, scorched purple ground
            w.setBlockState(PortalLogic.at(b, a, -2, 0, 0), cracked, 2);
            if (r.nextBoolean()) w.setBlockState(PortalLogic.at(b, a, -2, 1, 0), cracked, 2);
            w.setBlockState(PortalLogic.at(b, a, 5, 0, 0), brick, 2);
            w.setBlockState(PortalLogic.at(b, a, 5, 1, 0), cracked, 2);
            w.setBlockState(PortalLogic.at(b, a, -1, 0, -1), ModContent.GLOW_CRYSTAL.getDefaultState(), 2);
            w.setBlockState(PortalLogic.at(b, a, 4, 0, 1), ModContent.GLOW_CRYSTAL.getDefaultState(), 2);
            for (int j = 0; j < 10; j++) {
                BlockPos p = PortalLogic.at(b, a, -4 + r.nextInt(12), 0, -4 + r.nextInt(9));
                int y = groundY(w, p.getX(), p.getZ(), null);
                BlockPos g = new BlockPos(p.getX(), y, p.getZ());
                if (Math.abs(y - (b.getY() - 1)) <= 3 && w.getBlockState(g).getBlock() != ModContent.DUSKSTONE_BRICKS && w.getBlockState(g).getBlock() != ModContent.CRACKED_DUSKSTONE_BRICKS && w.getBlockState(g).getBlock() != ModContent.DUSKSTONE)
                    w.setBlockState(g, Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.PURPLE), 2);
            }
            // a half-buried chest with a loose page of the Doctor's notes
            chest(w, PortalLogic.at(b, a, 4, -1, -1), a == EnumFacing.Axis.X ? EnumFacing.NORTH : EnumFacing.WEST, LOOT_ALTAR, r, LoreBooks.page());
            return b;
        }
    }
}
