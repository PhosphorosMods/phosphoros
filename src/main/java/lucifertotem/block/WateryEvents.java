package lucifertotem.block;

import lucifertotem.ModContent;
import lucifertotem.world.LightfallChunkGenerator;
import lucifertotem.world.LightfallProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * - an empty bucket can't scoop up watery sea decor (it would delete the plant);
 * - Lightfall chunks saved before 1.1.1 get their underwater glow crystals turned into watery crystal coral once,
 *   on the first load (a flag in the chunk NBT marks them done). Tendrils/coral/starfish need nothing: same ids, meta 0.
 */
public class WateryEvents {
    public static final String FLAG = "LtWatered111";
    public static int converted = 0, chunksConverted = 0;
    /** keyed by dimension id, never by World: a World key kept every unloaded Lightfall alive (1.3.1 leak fix) */
    private static final Map<Integer, Set<Long>> PENDING = new ConcurrentHashMap<>();
    static int key(World w) { return w.provider.getDimension(); }
    /** for tests: how many dimensions still have pending conversions */
    public static int pendingWorlds() { return PENDING.size(); }

    @SubscribeEvent
    public void unload(net.minecraftforge.event.world.WorldEvent.Unload e) {
        if (e.getWorld().isRemote) return;
        PENDING.remove(key(e.getWorld()));
    }

    @SubscribeEvent
    public void bucket(FillBucketEvent e) {
        RayTraceResult t = e.getTarget();
        if (t == null || t.typeOfHit != RayTraceResult.Type.BLOCK) return;
        if (e.getWorld().getBlockState(t.getBlockPos()).getBlock() instanceof BlockWatery) e.setCanceled(true);
    }

    static boolean lightfall(World w) { return w != null && !w.isRemote && w.provider instanceof LightfallProvider; }

    @SubscribeEvent
    public void load(ChunkDataEvent.Load e) {
        if (!lightfall(e.getWorld()) || e.getData().getBoolean(FLAG)) return;
        Chunk c = e.getChunk();
        PENDING.computeIfAbsent(key(e.getWorld()), k -> ConcurrentHashMap.newKeySet()).add(ChunkPos.asLong(c.x, c.z));
    }

    @SubscribeEvent
    public void save(ChunkDataEvent.Save e) {
        if (!lightfall(e.getWorld())) return;
        Set<Long> p = PENDING.get(key(e.getWorld()));
        Chunk c = e.getChunk();
        if (p == null || !p.contains(ChunkPos.asLong(c.x, c.z))) e.getData().setBoolean(FLAG, true);
    }

    @SubscribeEvent
    public void tick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !lightfall(e.world)) return;
        Set<Long> p = PENDING.get(key(e.world));
        if (p == null || p.isEmpty()) return;
        int budget = 8;
        for (Iterator<Long> it = p.iterator(); it.hasNext() && budget > 0; budget--) {
            long k = it.next();
            it.remove();
            Chunk c = e.world.getChunkProvider().getLoadedChunk((int) k, (int) (k >> 32));
            if (c != null) convert(e.world, c);
        }
    }

    /** underwater glow crystals in this chunk -> crystal coral; returns how many */
    public static int convert(World w, Chunk c) {
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y < LightfallChunkGenerator.SEA_LEVEL; y++) {
            if (c.getBlockState(x, y, z).getBlock() != ModContent.GLOW_CRYSTAL) continue;
            m.setPos(c.x * 16 + x, y, c.z * 16 + z);
            if (!BlockWatery.wetAround(w, m)) continue;
            w.setBlockState(m.toImmutable(), ModContent.CRYSTAL_CORAL.getDefaultState(), 2);
            n++;
        }
        if (n > 0) { converted += n; chunksConverted++; }
        return n;
    }
}
