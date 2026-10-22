package lucifertotem.v132;

import lucifertotem.LTConfig;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HBlocks;
import lucifertotem.v130.V130;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Raising Dionysus: like an iron golem, four logs in a T (two stacked, two arms on the upper one) with his statue set on top.
 *
 *      S          S = Dionysus statue (2 tall, any facing)
 *    L L L        L = any log
 *      L          the two spaces beside the lower log must be empty
 */
public final class Xoanon {
    private Xoanon() {}
    public static int raised, refused;

    /** the T below a mask at p, arms along the given axis */
    public static boolean shape(World w, BlockPos p, EnumFacing.Axis axis) {
        BlockPos c = p.down(), b = p.down(2);
        EnumFacing side = axis == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.SOUTH;
        if (!V132.isLog(w, c) || !V132.isLog(w, b)) return false;
        if (!V132.isLog(w, c.offset(side)) || !V132.isLog(w, c.offset(side.getOpposite()))) return false;
        return w.isAirBlock(b.offset(side)) && w.isAirBlock(b.offset(side.getOpposite()));
    }

    /** the statue (lower half) was just set at p by the player: if it crowns a proper T, Dionysus wakes. Returns the new Dionysus or null. */
    public static EntityDionysus tryRaise(World w, BlockPos p, EntityPlayer owner) {
        if (w.isRemote || !LTConfig.v132.dionysus) return null;
        if (w.getBlockState(p).getBlock() != V132.STATUE) return null;
        EnumFacing.Axis axis = shape(w, p, EnumFacing.Axis.X) ? EnumFacing.Axis.X : shape(w, p, EnumFacing.Axis.Z) ? EnumFacing.Axis.Z : null;
        if (axis == null) { if (V132.isLog(w, p.down())) refused++; return null; }
        EnumFacing side = axis == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.SOUTH;
        BlockPos c = p.down(), b = p.down(2);
        for (BlockPos q : new BlockPos[]{p.up(), p, c, c.offset(side), c.offset(side.getOpposite()), b}) w.setBlockState(q, net.minecraft.init.Blocks.AIR.getDefaultState(), 2);
        for (BlockPos q : new BlockPos[]{p.up(), p, c, c.offset(side), c.offset(side.getOpposite()), b}) w.notifyNeighborsRespectDebug(q, net.minecraft.init.Blocks.AIR, false);
        EntityDionysus d = new EntityDionysus(w);
        d.setLocationAndAngles(b.getX() + 0.5, b.getY() + 0.05, b.getZ() + 0.5, owner.rotationYaw + 180f, 0f);
        d.setTamedBy(owner);
        d.setHealth(d.getMaxHealth());
        w.spawnEntity(d);
        raised++;
        w.playSound(null, d.posX, d.posY, d.posZ, V130.LYRE != null ? V130.LYRE : SoundEvents.BLOCK_NOTE_HARP, SoundCategory.NEUTRAL, 1f, 0.9f);
        w.playSound(null, d.posX, d.posY, d.posZ, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.NEUTRAL, 1f, 0.7f);
        if (w instanceof WorldServer) ((WorldServer) w).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, d.posX, d.posY + 1.2, d.posZ, 24, 0.5, 0.9, 0.5, 0.02);
        HBlocks.msg(owner, "msg.lucifertotem.dionysus_wakes");
        HAdv.grant(owner, "xoanon");
        V132.codexOnce(owner, "dionysus");
        return d;
    }
}
