package lucifertotem.v130;

import lucifertotem.hillman.HAdv;
import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/**
 * A maenad or satyr-masked reveller of a Bacchic pack: robed, ivy-crowned, carrying a fennel thyrsus.
 * Hostile, and a hit can leave you with Mania, but music calms them: near a playing jukebox or a note block
 * they stop fighting and dance instead.
 */
public class EntityBacchant extends EntityMob {
    static final DataParameter<Boolean> DANCING = EntityDataManager.createKey(EntityBacchant.class, DataSerializers.BOOLEAN);
    static final DataParameter<Integer> LOOK = EntityDataManager.createKey(EntityBacchant.class, DataSerializers.VARINT);
    public static int calmedCount = 0, maniaHits = 0;
    int calm = 0;

    public EntityBacchant(World w) { super(w); setSize(0.6f, 1.95f); }

    @Override protected void entityInit() { super.entityInit(); dataManager.register(DANCING, false); dataManager.register(LOOK, 0); }
    public boolean dancing() { return dataManager.get(DANCING); }
    public int look() { return dataManager.get(LOOK); }

    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.15, false) { @Override public boolean shouldExecute() { return !dancing() && super.shouldExecute(); } });
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.9));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 10f));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true) { @Override public boolean shouldExecute() { return !dancing() && super.shouldExecute(); } });
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true) { @Override public boolean shouldExecute() { return !dancing() && super.shouldExecute(); } });
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(22);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24);
    }
    @Override public IEntityLivingData onInitialSpawn(DifficultyInstance d, IEntityLivingData data) {
        data = super.onInitialSpawn(d, data);
        dataManager.set(LOOK, rand.nextInt(4));
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Stage3.THYRSUS));
        setDropChance(EntityEquipmentSlot.MAINHAND, 0f);
        return data;
    }

    /** a playing jukebox within 16 blocks or a note block within 8 */
    public static boolean music(World w, BlockPos c) {
        for (BlockPos p : BlockPos.getAllInBoxMutable(c.add(-16, -4, -16), c.add(16, 4, 16))) {
            net.minecraft.block.state.IBlockState s = w.getBlockState(p);
            if (s.getBlock() == Blocks.JUKEBOX && s.getValue(BlockJukebox.HAS_RECORD)) return true;
            if (s.getBlock() == Blocks.NOTEBLOCK && Math.abs(p.getX() - c.getX()) <= 8 && Math.abs(p.getZ() - c.getZ()) <= 8) return true;
        }
        return false;
    }

    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            if (dancing() && rand.nextInt(6) == 0) world.spawnParticle(EnumParticleTypes.NOTE, posX, posY + 2.2, posZ, rand.nextInt(24) / 24.0, 0, 0);
            return;
        }
        if (ticksExisted % 20 == (getEntityId() & 15)) {
            boolean m = music(world, getPosition());
            if (m) { if (calm == 0) calmedCount++; calm = 200; } else if (calm > 0) calm = Math.max(0, calm - 20);
            boolean d = calm > 0;
            if (d != dancing()) {
                dataManager.set(DANCING, d);
                if (d) {
                    setAttackTarget(null);
                    getNavigator().clearPath();
                    for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(16))) HAdv.grant(p, "bacchanal");
                }
            }
        }
        if (dancing()) {
            setAttackTarget(null);
            rotationYaw += 18; renderYawOffset = rotationYaw; rotationYawHead = rotationYaw;
            if (onGround && rand.nextInt(14) == 0) jump();
            if (ticksExisted % 10 == 0) swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        }
    }
    @Override public boolean attackEntityAsMob(Entity e) {
        boolean hit = super.attackEntityAsMob(e);
        if (hit && e instanceof EntityLivingBase && rand.nextInt(3) == 0) { ((EntityLivingBase) e).addPotionEffect(new PotionEffect(V130.MANIA, 100, 0)); maniaHits++; }
        return hit;
    }
    @Override protected boolean isValidLightLevel() { return true; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected ResourceLocation getLootTable() { return Stage3.LOOT_BACCHANT; }
    @Override protected net.minecraft.util.SoundEvent getAmbientSound() { return dancing() ? net.minecraft.init.SoundEvents.ENTITY_VILLAGER_YES : null; }
    @Override protected net.minecraft.util.SoundEvent getHurtSound(DamageSource s) { return net.minecraft.init.SoundEvents.ENTITY_VILLAGER_HURT; }
    @Override protected net.minecraft.util.SoundEvent getDeathSound() { return net.minecraft.init.SoundEvents.ENTITY_VILLAGER_DEATH; }
    @Override public void writeEntityToNBT(NBTTagCompound c) { super.writeEntityToNBT(c); c.setInteger("Look", look()); }
    @Override public void readEntityFromNBT(NBTTagCompound c) { super.readEntityFromNBT(c); dataManager.set(LOOK, c.getInteger("Look")); }
}
