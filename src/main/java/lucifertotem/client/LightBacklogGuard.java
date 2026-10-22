package lucifertotem.client;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 1.3.1 lag fix (client). Vanilla 1.12 queues every client-side light change in RenderGlobal.setLightUpdates and only
 * drains that set on a tick when NO chunk meshes are waiting to be rebuilt. Flying over the Lightfall keeps the rebuild
 * queue busy, so the set keeps growing; a tester's 2026-10-21 crash (OutOfMemoryError, 6 GB heap) died inside exactly
 * that HashSet.add. When the backlog passes the cap we hand the positions to
 * the chunk renderer ourselves (same call vanilla makes, just earlier) and empty the set.
 */
@Mod.EventBusSubscriber(modid = LuciferTotem.MODID, value = Side.CLIENT)
public class LightBacklogGuard {
    private static Field fSet;
    private static boolean broken;
    public static int drains = 0; public static long drained = 0; public static int biggest = 0;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || broken || LTConfig.lightBacklogCap <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.renderGlobal == null) return;
        try {
            if (fSet == null) fSet = ReflectionHelper.findField(RenderGlobal.class, "field_184387_ae", "setLightUpdates");
            @SuppressWarnings("unchecked") Set<BlockPos> s = (Set<BlockPos>) fSet.get(mc.renderGlobal);
            int n = drain(s, LTConfig.lightBacklogCap, (x, y, z) -> mc.renderGlobal.markBlockRangeForRenderUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1));
            if (n > 0) { drains++; drained += n; }
        } catch (Throwable t) {
            broken = true;
            LuciferTotem.LOG.warn("Phosphoros: light-backlog guard disabled (RenderGlobal changed?)", t);
        }
    }

    public interface Mark { void at(int x, int y, int z); }

    /** if the set holds more than cap positions, mark each for re-render and empty it; returns how many were handed over */
    public static int drain(Set<BlockPos> s, int cap, Mark m) {
        int size = s.size();
        if (size > biggest) biggest = size;
        if (size <= cap) return 0;
        List<BlockPos> copy = new ArrayList<>(s);
        s.clear();
        for (BlockPos p : copy) m.at(p.getX(), p.getY(), p.getZ());
        return copy.size();
    }
}
