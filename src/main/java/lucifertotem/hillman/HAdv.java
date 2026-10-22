package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/** grants our code-driven advancements (assets/lucifertotem/advancements/*.json, criterion "code" = minecraft:impossible) */
public class HAdv {
    public static boolean grant(EntityPlayer p, String name) {
        if (!(p instanceof EntityPlayerMP) || p.world.isRemote || p.getServer() == null) return false;
        if (p instanceof net.minecraftforge.common.util.FakePlayer) return false;
        Advancement a = p.getServer().getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, name));
        if (a == null) return false;
        EntityPlayerMP mp = (EntityPlayerMP) p;
        Advancement root = p.getServer().getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, "root"));
        if (root != null) mp.getAdvancements().grantCriterion(root, "code");
        return mp.getAdvancements().grantCriterion(a, "code");
    }
    /** does p have our advancement name (server side) */
    public static boolean has(EntityPlayer p, String name) {
        if (!(p instanceof EntityPlayerMP) || p.getServer() == null) return false;
        Advancement a = p.getServer().getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, name));
        return a != null && ((EntityPlayerMP) p).getAdvancements().getProgress(a).isDone();
    }
    public static boolean exists(net.minecraft.server.MinecraftServer s, String name) {
        return s.getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, name)) != null;
    }
}
