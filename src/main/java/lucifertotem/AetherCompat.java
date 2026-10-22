package lucifertotem;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Soft dependency on the Aether (aether_legacy): its Valkyries become the Lightfall's fallen angels.
 * Everything goes through registry lookups by name, so nothing here touches an Aether class and the mod
 * loads normally when the Aether is absent.
 *
 * Notes from reading Aether Legacy 1.5.4.1 (EntityValkyrie):
 *  - EntityMob: removed on Peaceful; never despawns by itself (canDespawn false) - natural Lightfall spawns are
 *    tagged and released by {@link #allowDespawn} when no player is near, spawner ones stay like any spawner mob;
 *  - no target-finding AI: neutral until a player hits it, then melee (7 dmg) with chat lines;
 *  - ignores all damage whose immediate source is not a player (teleports/extinguishes instead);
 *  - drops a Victory Medal (its own loot table); right-click chat; no medal or Aether-dimension requirement.
 * The Valkyrie Queen is deliberately NOT used: it rewrites "dungeon" blocks around its stored dungeon position every
 * tick once fighting (null outside an Aether dungeon) and needs the medal dialogue to start.
 */
public class AetherCompat {
    public static final String MODID = "aether_legacy";
    public static final ResourceLocation VALKYRIE = new ResourceLocation(MODID, "valkyrie");
    public static final ResourceLocation VALKYRIE_QUEEN = new ResourceLocation(MODID, "valkyrie_queen");
    public static final String NATURAL_TAG = "lucifertotem_natural";

    public static Class<? extends Entity> valkyrieClass;

    public static boolean active() { return valkyrieClass != null; }

    /** postInit: resolve the Valkyrie by registry name and add it to the Lightfall monster list. */
    @SuppressWarnings("unchecked")
    public static void init() {
        if (!Loader.isModLoaded(MODID)) { LuciferTotem.LOG.info("Aether not installed; Lightfall Valkyries disabled"); return; }
        EntityEntry ee = ForgeRegistries.ENTITIES.getValue(VALKYRIE);
        if (ee == null || !EntityLiving.class.isAssignableFrom(ee.getEntityClass())) {
            LuciferTotem.LOG.warn("Aether is loaded but {} was not found; Lightfall Valkyries disabled", VALKYRIE);
            return;
        }
        valkyrieClass = ee.getEntityClass();
        if (LTConfig.valkyrieWeight > 0 && ModContent.BIOME instanceof lucifertotem.world.LightfallBiome)
            ((lucifertotem.world.LightfallBiome) ModContent.BIOME).monsters.add(new Biome.SpawnListEntry((Class<? extends EntityLiving>) valkyrieClass, LTConfig.valkyrieWeight, 1, 1));
        LuciferTotem.LOG.info("Aether found: Valkyries ({}) will haunt the Lightfall", valkyrieClass.getName());
    }

    public static boolean isValkyrie(Entity e) { return valkyrieClass != null && e.getClass() == valkyrieClass; }

    /** spawner mob for a structure: a Valkyrie when available, else the vanilla fallback */
    public static String spawnerMob(String fallback) { return active() && LTConfig.valkyrieSpawners ? VALKYRIE.toString() : fallback; }

    /** Natural Valkyrie spawns: only on island tops under the open sky, rarely, and never crowded. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void checkSpawn(LivingSpawnEvent.CheckSpawn e) {
        if (!isValkyrie(e.getEntityLiving()) || e.isSpawner()) return;
        World w = e.getWorld();
        if (w.provider.getDimension() != LuciferTotem.dimId) return;
        e.setResult(naturalOk(w, e.getEntityLiving(), new BlockPos(e.getX(), e.getY(), e.getZ())) ? Event.Result.ALLOW : Event.Result.DENY);
        if (e.getResult() == Event.Result.ALLOW) e.getEntityLiving().addTag(NATURAL_TAG);
    }

    static boolean naturalOk(World w, Entity v, BlockPos p) {
        Block below = w.getBlockState(p.down()).getBlock();
        if (below != ModContent.STAR_GRASS && below != ModContent.DUSKSTONE && below != ModContent.DUSKSTONE_BRICKS) return false;
        if (!w.isAirBlock(p) || !w.isAirBlock(p.up()) || !w.canSeeSky(p)) return false;
        if (!isRoost(w, p.getX() >> 4, p.getZ() >> 4)) return false;
        if (w.rand.nextDouble() >= LTConfig.valkyrieSpawnChance) return false;
        int near = 0;
        for (Entity o : w.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(p).grow(64)))
            if (isValkyrie(o) && o.getTags().contains(NATURAL_TAG)) near++;
        return near < LTConfig.valkyrieLocalCap;
    }

    /** deterministic per world: only some chunks are fallen-angel roosts */
    public static boolean isRoost(World w, int cx, int cz) {
        long h = w.getSeed() ^ (cx * 341873128712L) ^ (cz * 132897987541L) ^ 0x4C55434946455221L;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
        return Math.floorMod(h, (long) Math.max(1, LTConfig.valkyrieRoostRarity)) == 0;
    }

    /** Valkyries never despawn by themselves; let the naturally spawned Lightfall ones go when nobody is near and they are calm. */
    @SubscribeEvent
    public void allowDespawn(LivingSpawnEvent.AllowDespawn e) {
        if (!isValkyrie(e.getEntityLiving()) || !e.getEntityLiving().getTags().contains(NATURAL_TAG)) return;
        EntityLiving v = (EntityLiving) e.getEntityLiving();
        if (v.hasCustomName() || v.getAttackTarget() != null) return;
        EntityPlayer p = v.world.getClosestPlayerToEntity(v, 48.0);
        if (p == null) e.setResult(Event.Result.ALLOW);
    }
}
