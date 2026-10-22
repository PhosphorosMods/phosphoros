package lucifertotem.hillman;

import lucifertotem.LTConfig;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thessalian witches: three (or more) chant in a stone circle to draw the Morningstar down from the sky
 * (the Thessalian witches' boast of pulling down the moon). While at least three are alive and a player is near,
 * the chant progresses; the star sinks and swells in the Lightfall sky (client, from synced progress).
 * Kill one and the spell snaps. Let it finish and the star falls: a Fallen Star block at the centre.
 */
public class EntityThessalianWitch extends EntityWitch {
    private static final DataParameter<Integer> PROGRESS = EntityDataManager.createKey(EntityThessalianWitch.class, DataSerializers.VARINT);
    /** circle centre -> progress permille (server), read by the Pythia */
    public static final Map<Long, Integer> ACTIVE = new ConcurrentHashMap<>();
    public static int completed = 0, snapped = 0;
    private BlockPos center;
    private double prog; // leader only

    public EntityThessalianWitch(World w) { super(w); }

    @Override protected void entityInit() { super.entityInit(); dataManager.register(PROGRESS, 0); }
    public int progress() { return dataManager.get(PROGRESS); }
    public BlockPos center() { return center; }

    public void setCenter(BlockPos c) { center = c; setHomePosAndDistance(c, 5); enablePersistence(); }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        tasks.addTask(1, new EntityAIMoveTowardsRestriction(this, 1.0));
    }

    List<EntityThessalianWitch> coven() {
        return world.getEntitiesWithinAABB(EntityThessalianWitch.class, new AxisAlignedBB(center).grow(16), w -> w != null && w.isEntityAlive() && center.equals(w.center));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || center == null) return;
        if (ticksExisted % 5 != 0) return;
        List<EntityThessalianWitch> cov = coven();
        int lowest = Integer.MAX_VALUE;
        for (EntityThessalianWitch w : cov) lowest = Math.min(lowest, w.getEntityId());
        if (getEntityId() != lowest) return; // the leader drives the ritual
        boolean near = world.getClosestPlayer(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 64, false) != null;
        int p = progress();
        if (cov.size() < 3) {
            if (p > 0) snap(cov);
            return;
        }
        if (!near) return;
        prog = Math.max(prog, p);
        prog += 1000.0 * 5 / (LTConfig.ritualSeconds * 20.0);
        int np = (int) Math.min(1000, prog);
        for (EntityThessalianWitch w : cov) w.dataManager.set(PROGRESS, np);
        ACTIVE.put(center.toLong(), np);
        if (world instanceof WorldServer && ticksExisted % 20 == 0) {
            for (EntityThessalianWitch w : cov)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, w.posX, w.posY + 2.2, w.posZ, 6, 0.2, 0.3, 0.2, 0.0);
            if (ticksExisted % 100 == 0) world.playSound(null, center, SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.5f, 0.6f);
        }
        if (np >= 1000) complete(cov);
    }

    /** the leader died or the coven is broken: the spell snaps */
    void snap(List<EntityThessalianWitch> cov) {
        snapped++;
        prog = 0;
        for (EntityThessalianWitch w : cov) w.dataManager.set(PROGRESS, 0);
        dataManager.set(PROGRESS, 0);
        ACTIVE.remove(center.toLong());
        world.playSound(null, center, HSounds.RITUAL_SNAP, SoundCategory.HOSTILE, 2f, 1f);
        for (EntityPlayer pl : world.playerEntities) if (pl.getDistanceSq(center) < 64 * 64) HBlocks.msg(pl, "msg.lucifertotem.ritual.snap");
    }

    void complete(List<EntityThessalianWitch> cov) {
        completed++;
        ACTIVE.remove(center.toLong());
        BlockPos at = center;
        while (at.getY() < 250 && !world.isAirBlock(at)) at = at.up();
        world.setBlockState(at, HBlocks.FALLEN_STAR.getDefaultState(), 3);
        world.addWeatherEffect(new net.minecraft.entity.effect.EntityLightningBolt(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, true));
        world.playSound(null, at, HSounds.STAR_FALL, SoundCategory.HOSTILE, 4f, 1f);
        for (EntityPlayer pl : world.playerEntities) if (pl.getDistanceSq(center) < 64 * 64) { HAdv.grant(pl, "star_fall"); HBlocks.msg(pl, "msg.lucifertotem.ritual.done"); }
        for (EntityThessalianWitch w : cov) {
            if (world instanceof WorldServer) ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, w.posX, w.posY + 1, w.posZ, 30, 0.4, 0.8, 0.4, 0.1);
            w.setDead();
        }
    }

    @Override
    public void onDeath(net.minecraft.util.DamageSource s) {
        super.onDeath(s);
        if (!world.isRemote && center != null && progress() > 0) snap(coven());
    }

    @Override protected boolean canDespawn() { return center == null && super.canDespawn(); }

    @Override public void writeEntityToNBT(NBTTagCompound t) { super.writeEntityToNBT(t); if (center != null) t.setLong("RitualCenter", center.toLong()); t.setInteger("Progress", progress()); }
    @Override public void readEntityFromNBT(NBTTagCompound t) {
        super.readEntityFromNBT(t);
        if (t.hasKey("RitualCenter")) setCenter(BlockPos.fromLong(t.getLong("RitualCenter")));
        dataManager.set(PROGRESS, t.getInteger("Progress"));
        prog = t.getInteger("Progress");
    }
}
