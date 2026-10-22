package lucifertotem.world;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.block.BlockLightfallPortal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * Lightfall gates: a 4x5 duskstone-brick frame (2x3 portal), always standing on a 6x5 platform with the cells in
 * front of and behind it cleared, so arriving players never fall into the void or suffocate.
 */
public class PortalLogic {
    public static BlockPos at(BlockPos base, EnumFacing.Axis a, int u, int v, int w) {
        return a == EnumFacing.Axis.X ? base.add(u, v, w) : base.add(w, v, u);
    }

    private static boolean replaceable(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s.getBlock().isAir(s, w, p) || (s.getBlock().isReplaceable(w, p) && !s.getMaterial().isLiquid());
    }

    private static boolean solidGround(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s.getMaterial().isSolid() && !s.getMaterial().isLiquid() && s.isFullCube() && s.getBlock() != Blocks.LEAVES && s.getBlock() != Blocks.LEAVES2;
    }

    /** strict check used next to villagers: nothing but air/plants in the volume, solid ground under the frame. */
    static boolean siteOK(World w, BlockPos base, EnumFacing.Axis a) {
        if (base.getY() < 2 || base.getY() + 5 > w.getActualHeight()) return false;
        for (int u = 0; u <= 3; u++) if (!solidGround(w, at(base, a, u, -1, 0))) return false;
        for (int u = -1; u <= 4; u++) for (int v = 0; v <= 4; v++) for (int k = -1; k <= 1; k++)
            if (!replaceable(w, at(base, a, u, v, k))) return false;
        return true;
    }

    /** Builds the gate. Platform fills only non-solid cells; clearing only removes replaceable blocks unless force. */
    public static void buildGate(World w, BlockPos base, EnumFacing.Axis a, boolean force) {
        IBlockState frame = ModContent.DUSKSTONE_BRICKS.getDefaultState();
        IBlockState plat = ModContent.DUSKSTONE_BRICKS.getDefaultState();
        for (int u = -1; u <= 4; u++) for (int k = -2; k <= 2; k++) {
            BlockPos p = at(base, a, u, -1, k);
            if (!solidGround(w, p) && (force || replaceable(w, p) || w.getBlockState(p).getMaterial().isLiquid())) w.setBlockState(p, plat, 2);
            for (int v = 0; v <= 4; v++) {
                if (k == 0 && u >= 0 && u <= 3) continue;
                BlockPos q = at(base, a, u, v, k);
                if (!w.isAirBlock(q) && (force || replaceable(w, q))) w.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
            }
        }
        IBlockState portal = ModContent.PORTAL.getDefaultState().withProperty(BlockLightfallPortal.AXIS, a);
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) {
            boolean edge = u == 0 || u == 3 || v == 0 || v == 4;
            w.setBlockState(at(base, a, u, v, 0), edge ? frame : portal, 2);
        }
        // notify neighbours once the whole structure is in place
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) w.notifyBlockUpdate(at(base, a, u, v, 0), Blocks.AIR.getDefaultState(), w.getBlockState(at(base, a, u, v, 0)), 3);
        if (w instanceof WorldServer) {
            BlockPos c = at(base, a, 2, 2, 0);
            ((WorldServer) w).spawnParticle(EnumParticleTypes.PORTAL, c.getX(), c.getY(), c.getZ(), 120, 1.0, 1.5, 1.0, 0.6);
            w.playSound(null, c, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.8f, 1.3f);
        }
    }

    /** a gate's frame and membrane, or a rift's two blocks */
    static boolean intact(World w, PortalData.Gate known, BlockPos base, EnumFacing.Axis a) {
        if (known != null && known.rift) return RiftLogic.intact(w, base);
        return gateIntact(w, base, a);
    }

    public static boolean gateIntact(World w, BlockPos base, EnumFacing.Axis a) {
        for (int u = 1; u <= 2; u++) for (int v = 1; v <= 3; v++)
            if (w.getBlockState(at(base, a, u, v, 0)).getBlock() != ModContent.PORTAL) return false;
        return true;
    }

    /** Smiting a villager: raise a gate at the nearest safe open spot (never breaks village blocks). */
    public static PortalData.Gate openGateNear(World w, BlockPos center) {
        BlockPos found = null; EnumFacing.Axis fa = EnumFacing.Axis.X;
        int[] dys = {0, -1, 1, -2, 2, -3, 3, 4, 5};
        outer:
        for (int r = 2; r <= 10; r++)
            for (int dy : dys)
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos b = center.add(dx, dy, dz);
                    for (EnumFacing.Axis a : new EnumFacing.Axis[] {EnumFacing.Axis.X, EnumFacing.Axis.Z})
                        if (siteOK(w, b, a)) { found = b; fa = a; break outer; }
                }
        if (found == null) { // fallback: hover above the terrain on its own platform (only air is replaced)
            int y = w.getHeight(center).getY() + 1;
            BlockPos b = new BlockPos(center.getX() + 3, y, center.getZ());
            while (b.getY() < w.getActualHeight() - 8 && !volumeAir(w, b, fa)) b = b.up();
            found = b;
        }
        buildGate(w, found, fa, false);
        PortalData d = PortalData.get();
        PortalData.Gate g = d.findExact(w.provider.getDimension(), found);
        if (g == null) { g = new PortalData.Gate(w.provider.getDimension(), found, fa); d.gates.add(g); }
        d.markDirty();
        LuciferTotem.LOG.info("A villager's soul opened a Lightfall gate at {} in dim {}", found, w.provider.getDimension());
        return g;
    }

    private static boolean volumeAir(World w, BlockPos base, EnumFacing.Axis a) {
        for (int u = -1; u <= 4; u++) for (int v = -1; v <= 4; v++) for (int k = -2; k <= 2; k++)
            if (!replaceable(w, at(base, a, u, v, k))) return false;
        return true;
    }

    /** Recover gate geometry from a portal block (for gates missing from the registry). */
    static PortalData.Gate detect(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        if (s.getBlock() != ModContent.PORTAL) return null;
        EnumFacing.Axis a = s.getValue(BlockLightfallPortal.AXIS);
        EnumFacing back = a == EnumFacing.Axis.X ? EnumFacing.WEST : EnumFacing.NORTH;
        BlockPos q = p;
        while (w.getBlockState(q.down()).getBlock() == ModContent.PORTAL) q = q.down();
        while (w.getBlockState(q.offset(back)).getBlock() == ModContent.PORTAL) q = q.offset(back);
        return new PortalData.Gate(w.provider.getDimension(), q.down().offset(back), a);
    }

    /** Find a spot for a gate in the destination world near x,z. */
    static BlockPos findSite(WorldServer w, int x, int z, EnumFacing.Axis a) {
        boolean lightfall = w.provider.getDimension() == LuciferTotem.dimId;
        boolean ceiling = w.provider.isNether() || !w.provider.hasSkyLight();
        for (int r = 0; r <= (lightfall ? 96 : 48); r += 4)
            for (int dx = -r; dx <= r; dx += 4) for (int dz = -r; dz <= r; dz += 4) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int cx = x + dx, cz = z + dz;
                if (ceiling) {
                    for (int y = Math.min(110, w.getActualHeight() - 8); y > 8; y--) {
                        BlockPos b = new BlockPos(cx, y, cz);
                        if (siteOK(w, b, a)) return b;
                    }
                } else {
                    int top = lightfall ? LightfallChunkGenerator.topY(w, cx, cz, LightfallChunkGenerator.SEA_LEVEL + 6) : w.getHeight(new BlockPos(cx, 0, cz)).getY() - 1;
                    if (top < 2) continue;
                    BlockPos b = new BlockPos(cx, top + 1, cz);
                    if (siteOK(w, b, a)) return b;
                }
            }
        // fallback: a floating platform (buildGate(force) lays it down)
        int y = lightfall ? 100 : ceiling ? 64 : Math.max(w.getHeight(new BlockPos(x, 0, z)).getY() + 1, w.getSeaLevel() + 2);
        return new BlockPos(x, y, z);
    }

    /** Called when a player touches a Lightfall gate (server side). */
    public static boolean enterPortal(EntityPlayerMP p, BlockPos pos) {
        World src = p.world;
        int srcDim = src.provider.getDimension();
        PortalData d = PortalData.get();
        PortalData.Gate g = d.find(srcDim, pos);
        if (g == null) {
            g = detect(src, pos);
            if (g == null) return false;
            d.gates.add(g);
        }
        boolean leaving = srcDim == LuciferTotem.dimId;
        int dstDim = g.linked ? g.ldim : (leaving ? 0 : LuciferTotem.dimId);
        // going home through a gate that leads nowhere yet (or whose far side is gone): head for where this player came from
        int[] ret = leaving ? returnPoint(p) : null;
        if (leaving && ret != null && (!g.linked || g.ldim == srcDim)) dstDim = ret[0];
        if (dstDim == srcDim) dstDim = leaving ? 0 : LuciferTotem.dimId;
        if (!DimensionManager.isDimensionRegistered(dstDim)) dstDim = 0;
        WorldServer dst = p.getServer().getWorld(dstDim);
        if (!leaving) setReturn(p, srcDim, g.base);   // remember the way home (player NBT, survives death)
        PortalData.Gate target = null;
        if (g.linked && g.ldim == dstDim && intact(dst, d.findExact(dstDim, g.lbase), g.lbase, g.laxis)) {
            target = d.findExact(dstDim, g.lbase);
            if (target == null) { target = new PortalData.Gate(dstDim, g.lbase, g.laxis); d.gates.add(target); }
        }
        if (target == null && leaving && ret != null && ret[0] == dstDim) {
            // the gate the player entered by, if it still stands
            BlockPos rb = new BlockPos(ret[1], ret[2], ret[3]);
            PortalData.Gate rg = d.findExact(dstDim, rb);
            if (rg != null && intact(dst, rg, rg.base, rg.axis)) target = rg;
        }
        if (target == null) {
            int ax = g.base.getX(), az = g.base.getZ();
            if (leaving) {
                BlockPos home = ret != null && ret[0] == dstDim ? new BlockPos(ret[1], ret[2], ret[3]) : homeOf(p, dst);
                ax = home.getX(); az = home.getZ();
            }
            BlockPos b = findSite(dst, ax, az, g.axis);
            buildGate(dst, b, g.axis, true);
            target = d.findExact(dstDim, b);
            if (target == null) { target = new PortalData.Gate(dstDim, b, g.axis); d.gates.add(target); }
            LuciferTotem.LOG.info("Built linked Lightfall gate at {} in dim {}", b, dstDim);
        }
        g.linkTo(target);
        target.linkTo(g);
        d.markDirty();
        // arrive standing in front of (or behind) the gate, facing it, never inside a wall, in water or over the void
        double[] at = target.rift ? RiftLogic.arrival(dst, target) : arrival(dst, target);
        p.timeUntilPortal = 100;
        p.changeDimension(dstDim, new LightfallTeleporter(at[0], at[1], at[2], (float) at[3]));
        p.timeUntilPortal = 100;
        if (leaving && p.dimension != LuciferTotem.dimId) { returnTrips++; lucifertotem.hillman.HAdv.grant(p, "return_lightfall"); }
        if (p.dimension == LuciferTotem.dimId) lucifertotem.hillman.HAdv.grant(p, "enter_lightfall");
        return true;
    }

    public static int returnTrips = 0, carved = 0;

    // ------------------------------------------------------------ the way home (per player)
    public static void setReturn(EntityPlayerMP p, int dim, BlockPos pos) {
        net.minecraft.nbt.NBTTagCompound t = lucifertotem.hillman.HData.get(p);
        t.setInteger("returnDim", dim); t.setLong("returnPos", pos.toLong()); t.setBoolean("hasReturn", true);
    }

    /** {dim, x, y, z} of the gate this player last entered the Lightfall by, or null */
    public static int[] returnPoint(net.minecraft.entity.player.EntityPlayer p) {
        net.minecraft.nbt.NBTTagCompound t = lucifertotem.hillman.HData.get(p);
        if (!t.getBoolean("hasReturn")) return null;
        BlockPos b = BlockPos.fromLong(t.getLong("returnPos"));
        int dim = t.getInteger("returnDim");
        if (dim == LuciferTotem.dimId || !DimensionManager.isDimensionRegistered(dim)) dim = 0;
        return new int[]{dim, b.getX(), b.getY(), b.getZ()};
    }

    /** no return point: the player's bed (if in that world), else the world spawn */
    static BlockPos homeOf(EntityPlayerMP p, WorldServer dst) {
        BlockPos bed = p.getBedLocation(dst.provider.getDimension());
        if (bed != null) return bed;
        return dst.getSpawnPoint();
    }

    static boolean standable(World w, BlockPos f) {
        return replaceable(w, f) && replaceable(w, f.up()) && solidGround(w, f.down());
    }

    /** {x, y, z, yaw}: a dry, solid, open spot by the gate; clears a doorway in front as a last resort */
    public static double[] arrival(World w, PortalData.Gate t) {
        int[] ks = {2, -2, 1, -1, 3, -3};
        for (int k : ks) {
            boolean ok = true;
            for (int u = 1; u <= 2 && ok; u++) ok = standable(w, at(t.base, t.axis, u, 0, k));
            if (ok) return spot(t, k, t.base.getY());
        }
        // last resort: dig a 2x2 doorway in front of the gate (never bedrock, never a tile entity) on a duskstone step
        carved++;
        for (int u = 1; u <= 2; u++) {
            BlockPos f = at(t.base, t.axis, u, 0, 1);
            for (BlockPos q : new BlockPos[]{f, f.up(), at(t.base, t.axis, u, 0, 2), at(t.base, t.axis, u, 1, 2)}) {
                IBlockState s = w.getBlockState(q);
                if (s.getBlock() != Blocks.BEDROCK && w.getTileEntity(q) == null && s.getBlock() != ModContent.PORTAL) w.setBlockState(q, Blocks.AIR.getDefaultState(), 3);
            }
            for (BlockPos q : new BlockPos[]{f.down(), at(t.base, t.axis, u, -1, 2)})
                if (!solidGround(w, q) && w.getBlockState(q).getBlock() != Blocks.BEDROCK && w.getTileEntity(q) == null) w.setBlockState(q, ModContent.DUSKSTONE_BRICKS.getDefaultState(), 3);
        }
        return spot(t, 1, t.base.getY());
    }

    static double[] spot(PortalData.Gate t, int k, int y) {
        BlockPos f = at(t.base, t.axis, 1, 0, k);
        if (t.axis == EnumFacing.Axis.X) return new double[]{t.base.getX() + 2.0, y, f.getZ() + 0.5, k > 0 ? 180 : 0};
        return new double[]{f.getX() + 0.5, y, t.base.getZ() + 2.0, k > 0 ? 90 : 270};
    }
}
