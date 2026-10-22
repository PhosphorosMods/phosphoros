package lucifertotem.lore;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.block.BlockLightfallPortal;
import lucifertotem.hillman.HAdv;
import lucifertotem.world.PortalData;
import lucifertotem.world.PortalLogic;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Fallen Altars and player-built altars: any 4-wide, 5-tall frame (corners included) of Duskstone Bricks with a clear
 * 2x3 middle is a Lightfall gate waiting to be lit. Strike (use) the Totem of Lucifer on the frame: in the Overworld
 * only at dusk (config), in the Lightfall at any time (it is always dusk there), elsewhere at any time.
 */
public final class AltarLogic {
    private AltarLogic() {}
    public static int lit = 0;

    public static class Frame {
        public final BlockPos base; public final EnumFacing.Axis axis; public final int whole, cracked, missing; public final boolean clear, burning;
        Frame(BlockPos b, EnumFacing.Axis a, int whole, int cracked, int missing, boolean clear, boolean burning) {
            base = b; axis = a; this.whole = whole; this.cracked = cracked; this.missing = missing; this.clear = clear; this.burning = burning;
        }
        public boolean complete() { return whole == 14; }
        public String toString() { return "frame[" + base + " " + axis + " whole=" + whole + " cracked=" + cracked + " missing=" + missing + (clear ? " clear" : "") + (burning ? " burning" : "") + "]"; }
    }

    static boolean frameCell(int u, int v) { return u == 0 || u == 3 || v == 0 || v == 4; }

    static boolean openCell(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s.getBlock() == ModContent.PORTAL || s.getBlock().isAir(s, w, p) || (s.getBlock().isReplaceable(w, p) && !s.getMaterial().isLiquid());
    }

    /** examine the frame at base/axis; null unless it is plausibly an altar (>= 8 duskstone stones, whole or cracked) */
    public static Frame examine(World w, BlockPos base, EnumFacing.Axis a) {
        int whole = 0, cracked = 0, missing = 0; boolean clear = true, burning = true;
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) {
            BlockPos p = PortalLogic.at(base, a, u, v, 0);
            Block b = w.getBlockState(p).getBlock();
            if (frameCell(u, v)) {
                if (b == ModContent.DUSKSTONE_BRICKS) whole++;
                else if (b == ModContent.CRACKED_DUSKSTONE_BRICKS) cracked++;
                else missing++;
            } else {
                if (b != ModContent.PORTAL) burning = false;
                if (!openCell(w, p)) clear = false;
            }
        }
        if (whole + cracked < 8) return null;
        return new Frame(base, a, whole, cracked, missing, clear, burning);
    }

    /** the best frame containing pos as one of its 14 stones (or as a missing stone's neighbour: a clicked stone) */
    public static Frame find(World w, BlockPos pos) {
        Frame best = null;
        for (EnumFacing.Axis a : new EnumFacing.Axis[]{EnumFacing.Axis.X, EnumFacing.Axis.Z})
            for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) {
                if (!frameCell(u, v)) continue;
                BlockPos base = a == EnumFacing.Axis.X ? pos.add(-u, -v, 0) : pos.add(0, -v, -u);
                Frame f = examine(w, base, a);
                if (f == null || !f.clear) continue;
                if (best == null || f.whole + f.cracked > best.whole + best.cracked) best = f;
            }
        return best;
    }

    public static boolean isDusk(World w) {
        long t = w.getWorldTime() % 24000L;
        return t >= 11000 && t <= 14500;
    }

    public static boolean timeOK(World w) {
        if (w.provider.getDimension() == LuciferTotem.dimId) return true;
        if (w.provider.getDimension() != 0 || !LTConfig.lore.altarDuskOnly) return true;
        return isDusk(w);
    }

    /** server side: light the frame if it is whole, clear and it is dusk; tells the player why not otherwise. */
    public static boolean tryLight(World w, Frame f, EntityPlayer p) {
        if (w.isRemote || f == null) return false;
        if (!LuciferTotem.dimRegistered) return false;
        if (f.burning) { msg(p, "altar.lucifertotem.burning"); return false; }
        if (!f.complete()) {
            msg(p, "altar.lucifertotem.cracked", f.cracked + f.missing);
            w.playSound(null, f.base, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 1f, 0.6f);
            return false;
        }
        if (!timeOK(w)) { msg(p, "altar.lucifertotem.not_dusk"); w.playSound(null, f.base, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.6f, 0.7f); return false; }
        light(w, f.base, f.axis);
        if (p != null) { msg(p, "altar.lucifertotem.lit"); HAdv.grant(p, "fallen_altar"); }
        return true;
    }

    /** fill the middle with the gate membrane and register the gate (unlinked until someone walks through) */
    public static void light(World w, BlockPos base, EnumFacing.Axis a) {
        IBlockState portal = ModContent.PORTAL.getDefaultState().withProperty(BlockLightfallPortal.AXIS, a);
        for (int u = 1; u <= 2; u++) for (int v = 1; v <= 3; v++) w.setBlockState(PortalLogic.at(base, a, u, v, 0), portal, 2);
        for (int u = 1; u <= 2; u++) for (int v = 1; v <= 3; v++) { BlockPos p = PortalLogic.at(base, a, u, v, 0); w.notifyBlockUpdate(p, Blocks.AIR.getDefaultState(), portal, 3); }
        PortalData d = PortalData.get();
        PortalData.Gate g = d.findExact(w.provider.getDimension(), base);
        if (g == null) { d.gates.add(new PortalData.Gate(w.provider.getDimension(), base, a)); }
        else { g.axis = a; g.linked = false; }
        d.markDirty();
        lit++;
        BlockPos c = PortalLogic.at(base, a, 2, 2, 0);
        if (w instanceof WorldServer) {
            ((WorldServer) w).spawnParticle(EnumParticleTypes.PORTAL, c.getX(), c.getY(), c.getZ(), 160, 1.0, 1.5, 1.0, 0.8);
            ((WorldServer) w).spawnParticle(EnumParticleTypes.SPELL_WITCH, c.getX(), c.getY(), c.getZ(), 60, 1.0, 1.5, 1.0, 0.1);
        }
        w.playSound(null, c, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.9f, 1.3f);
        w.playSound(null, c, lucifertotem.hillman.HSounds.DISTANT_AMMONIUS, SoundCategory.AMBIENT, 1.2f, 1.0f);
        LuciferTotem.LOG.info("An altar was lit: Lightfall gate at {} in dim {}", base, w.provider.getDimension());
    }

    static void msg(EntityPlayer p, String key, Object... args) {
        if (p == null) return;
        TextComponentTranslation t = new TextComponentTranslation(key, args);
        t.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
        p.sendStatusMessage(t, true);
    }
}
