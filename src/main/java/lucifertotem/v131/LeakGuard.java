package lucifertotem.v131;

import lucifertotem.LTConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 1.3.1: a player who leaves a dimension still remembers the last mob they hit there (vanilla's lastAttackedEntity is never
 * cleared) and, for a few seconds, the mob that hit them. That one stale reference keeps the whole unloaded world - its
 * entities, villages, pathfinders, chunk provider - reachable, so each Lightfall trip that ended in a fight leaked one
 * WorldServer. We forget those references on the way out and again when the world unloads.
 */
public class LeakGuard {
    public static int cleared = 0;

    /** drop every reference this player holds into a world other than the one they are standing in (or into the given world) */
    public static boolean forget(EntityPlayer p, World gone) {
        boolean any = false;
        EntityLivingBase a = p.getLastAttackedEntity();
        if (a != null && (a.world == gone || a.world != p.world)) { p.setLastAttackedEntity(null); any = true; }
        EntityLivingBase r = p.getRevengeTarget();
        if (r != null && (r.world == gone || r.world != p.world)) { p.setRevengeTarget(null); any = true; }
        if (any) { p.getCombatTracker().reset(); cleared++; }
        return any;
    }

    @SubscribeEvent
    public void changed(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!LTConfig.leakGuard || e.player.world.isRemote) return;
        forget(e.player, null);
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload e) {
        if (!LTConfig.leakGuard || e.getWorld().isRemote) return;
        MinecraftServer s = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (s == null) return;
        for (EntityPlayerMP p : s.getPlayerList().getPlayers()) forget(p, e.getWorld());
    }
}
