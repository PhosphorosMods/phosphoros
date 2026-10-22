package lucifertotem.hillman;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/** Per-player data kept in the Forge persisted tag (survives death and relog; copied by Forge on respawn). */
public class HData {
    public static NBTTagCompound get(EntityPlayer p) {
        NBTTagCompound root = p.getEntityData();
        NBTTagCompound per = root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!root.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) root.setTag(EntityPlayer.PERSISTED_NBT_TAG, per);
        NBTTagCompound lt = per.getCompoundTag("lucifertotem");
        if (!per.hasKey("lucifertotem")) per.setTag("lucifertotem", lt);
        return lt;
    }
    public static int venomTolerance(EntityPlayer p) { return get(p).getInteger("venomTolerance"); }
    public static void setVenomTolerance(EntityPlayer p, int v) { get(p).setInteger("venomTolerance", Math.max(0, Math.min(100, v))); }
    /** world-time tick until which the Trophonian gloom (chat suffix) lasts */
    public static long gloomUntil(EntityPlayer p) { return get(p).getLong("gloomUntil"); }
    public static boolean gloomy(EntityPlayer p) { return p.world.getTotalWorldTime() < gloomUntil(p); }
}
