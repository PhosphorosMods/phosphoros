package lucifertotem.world;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.block.BlockPurpleRift;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Iterator;

/** 1.2.1: permanent purple rifts (two-block, walk-through doors to the Lightfall) left by converted Ammons. */
public final class RiftLogic {
    private RiftLogic() {}
    public static int opened = 0, closedCount = 0, entered = 0, refused = 0;
    public static boolean closing = false;

    static boolean free(World w, BlockPos p) { return w.isAirBlock(p); }   // only ever opens in empty air: never replaces a block

    /** an empty 1x2 column standing on solid ground, not touching another rift or a gate */
    public static boolean spotOK(World w, BlockPos b) {
        if (b.getY() < 1 || b.getY() + 2 >= w.getActualHeight()) return false;
        if (!free(w, b) || !free(w, b.up())) return false;
        if (!w.getBlockState(b.down()).isSideSolid(w, b.down(), EnumFacing.UP)) return false;
        for (BlockPos q : BlockPos.getAllInBox(b.add(-1, -1, -1), b.add(1, 2, 1))) {
            net.minecraft.block.Block k = w.getBlockState(q).getBlock();
            if (k == ModContent.RIFT || k == ModContent.PORTAL) return false;
        }
        return true;
    }

    /** nearest good spot to 'pref' (searching outward up to 4 blocks, +-3 vertically) */
    public static BlockPos findSpot(World w, BlockPos pref) {
        for (int r = 0; r <= 4; r++)
            for (int dy : new int[]{0, -1, 1, -2, 2, -3, 3})
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos b = pref.add(dx, dy, dz);
                    if (spotOK(w, b)) return b;
                }
        return null;
    }

    public static int count() {
        int n = 0;
        for (PortalData.Gate g : PortalData.get().gates) if (g.rift) n++;
        return n;
    }

    public static boolean mayOpenPermanent() {
        return LTConfig.ammon.permanentRifts && count() < LTConfig.ammon.maxPermanentRifts;
    }

    /** open a permanent rift at b (must pass spotOK); returns the registered gate or null */
    public static PortalData.Gate open(World w, BlockPos b, EnumFacing.Axis a, EntityPlayer maker) {
        if (w.isRemote || !spotOK(w, b) || !mayOpenPermanent()) { refused++; return null; }
        net.minecraft.block.state.IBlockState s = ModContent.RIFT.getDefaultState().withProperty(BlockPurpleRift.AXIS, a);
        w.setBlockState(b, s.withProperty(BlockPurpleRift.UPPER, false), 2);
        w.setBlockState(b.up(), s.withProperty(BlockPurpleRift.UPPER, true), 3);
        PortalData d = PortalData.get();
        PortalData.Gate g = d.findExact(w.provider.getDimension(), b);
        if (g == null) { g = new PortalData.Gate(w.provider.getDimension(), b, a); d.gates.add(g); }
        g.rift = true;
        g.maker = maker == null ? "" : maker.getUniqueID().toString();
        d.markDirty();
        opened++;
        if (w instanceof WorldServer) {
            ((WorldServer) w).spawnParticle(EnumParticleTypes.PORTAL, b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5, 140, 0.3, 1.0, 0.3, 1.0);
            w.playSound(null, b, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.8f, 1.5f);
        }
        LuciferTotem.LOG.info("A purple rift opened at {} in dim {}", b, w.provider.getDimension());
        return g;
    }

    public static boolean intact(World w, BlockPos b) {
        return w.getBlockState(b).getBlock() == ModContent.RIFT && w.getBlockState(b.up()).getBlock() == ModContent.RIFT;
    }

    /** may this player close this rift? */
    public static boolean mayClose(EntityPlayer p, PortalData.Gate g) {
        if (LTConfig.ammon.anyoneMayClose || g == null || g.maker == null || g.maker.isEmpty()) return true;
        if (p.getUniqueID().toString().equals(g.maker) || p.capabilities.isCreativeMode) return true;
        return p.getServer() != null && p.getServer().getPlayerList().canSendCommands(p.getGameProfile());
    }

    /** sneak-use of the totem on a rift block */
    public static boolean tryClose(EntityPlayer p, World w, BlockPos hit) {
        if (w.isRemote) return true;
        BlockPos b = w.getBlockState(hit).getValue(BlockPurpleRift.UPPER) ? hit.down() : hit;
        PortalData.Gate g = PortalData.get().findExact(w.provider.getDimension(), b);
        if (!mayClose(p, g)) { p.sendStatusMessage(new TextComponentTranslation("rift.lucifertotem.not_yours"), true); return false; }
        close(w, b, p, true);
        p.sendStatusMessage(new TextComponentTranslation("rift.lucifertotem.closed"), true);
        return true;
    }

    /** removes both blocks (only rift blocks), forgets the rift, unlinks whatever led to it */
    public static void close(World w, BlockPos b, EntityPlayer by, boolean effects) {
        if (closing) return;
        closing = true;
        try {
            for (BlockPos q : new BlockPos[]{b, b.up()})
                if (w.getBlockState(q).getBlock() == ModContent.RIFT) w.setBlockState(q, net.minecraft.init.Blocks.AIR.getDefaultState(), 3);
            PortalData d = PortalData.get();
            int dim = w.provider.getDimension();
            boolean known = false;
            for (Iterator<PortalData.Gate> it = d.gates.iterator(); it.hasNext(); ) {
                PortalData.Gate g = it.next();
                if (g.rift && g.dim == dim && g.base.equals(b)) { it.remove(); known = true; }
            }
            for (PortalData.Gate g : d.gates) if (g.linked && g.ldim == dim && g.lbase.equals(b)) g.linked = false;
            d.markDirty();
            if (known) closedCount++;
            if (effects && w instanceof WorldServer) {
                ((WorldServer) w).spawnParticle(EnumParticleTypes.SPELL_WITCH, b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5, 50, 0.3, 0.8, 0.3, 0.05);
                w.playSound(null, b, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1f, 0.5f);
            }
        } finally { closing = false; }
    }

    /** a player stepped into a rift */
    public static void enter(EntityPlayerMP p, BlockPos b) {
        PortalData d = PortalData.get();
        PortalData.Gate g = d.findExact(p.dimension, b);
        if (g == null) {   // registry lost (a copied world, a schematic): adopt it
            g = new PortalData.Gate(p.dimension, b, p.world.getBlockState(b).getValue(BlockPurpleRift.AXIS));
            g.rift = true; g.maker = ""; d.gates.add(g); d.markDirty();
        }
        entered++;
        PortalLogic.enterPortal(p, b);
    }

    /** {x,y,z,yaw}: a standable spot beside the rift (never inside it, never digging); falls back to the surface above */
    public static double[] arrival(World w, PortalData.Gate t) {
        BlockPos b = t.base;
        EnumFacing.Axis a = t.axis;
        int[][] prefs = a == EnumFacing.Axis.X ? new int[][]{{0, 2}, {0, -2}, {0, 1}, {0, -1}} : new int[][]{{2, 0}, {-2, 0}, {1, 0}, {-1, 0}};
        for (int[] o : prefs) for (int dy : new int[]{0, 1, -1}) {
            BlockPos f = b.add(o[0], dy, o[1]);
            if (PortalLogic.standable(w, f) && w.getBlockState(f).getBlock() != ModContent.RIFT) return face(f, b);
        }
        for (int r = 1; r <= 5; r++) for (int dy = -2; dy <= 3; dy++)
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                BlockPos f = b.add(dx, dy, dz);
                if (PortalLogic.standable(w, f) && w.getBlockState(f).getBlock() != ModContent.RIFT) return face(f, b);
            }
        BlockPos top = w.getHeight(b.add(2, 0, 0));
        return face(top, b);
    }

    static double[] face(BlockPos f, BlockPos rift) {
        double dx = rift.getX() - f.getX(), dz = rift.getZ() - f.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        return new double[]{f.getX() + 0.5, f.getY(), f.getZ() + 0.5, yaw};
    }
}
