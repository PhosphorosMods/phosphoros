package lucifertotem.v130;

import lucifertotem.LTConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/**
 * A Scythian huntress of the steppe: trousers, a felt cap and a gorytos quiver, usually on horseback.
 * Shoots toxicon arrows (Slowness, then Mania); drops them, and rarely her fast-drawing bow.
 */
public class EntityHuntress extends EntityMob implements IRangedAttackMob {
    public static int shots = 0, mounted = 0;
    public EntityHuntress(World w) { super(w); setSize(0.6f, 1.95f); }

    @Override protected void initEntityAI() {
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(3, new EntityAIAttackRangedBow<>(this, 1.0, 20, 18f));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 1.0));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 12f));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
    }
    @Override public IEntityLivingData onInitialSpawn(DifficultyInstance d, IEntityLivingData data) {
        data = super.onInitialSpawn(d, data);
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Stage3.SCYTHIAN_BOW));
        setDropChance(EntityEquipmentSlot.MAINHAND, 0f);
        if (!world.isRemote && rand.nextInt(4) != 0) mount();
        return data;
    }
    public boolean mount() {
        EntityHorse h = new EntityHorse(world);
        h.setLocationAndAngles(posX, posY, posZ, rotationYaw, 0);
        h.onInitialSpawn(world.getDifficultyForLocation(getPosition()), null);
        h.setHorseTamed(true);
        h.setGrowingAge(0);
        world.spawnEntity(h);
        boolean ok = startRiding(h);
        if (ok) mounted++;
        return ok;
    }
    @Override public boolean getCanSpawnHere() {
        if (LTConfig.v130.huntressWeight <= 0 || rand.nextDouble() >= LTConfig.v130.huntressSpawnChance) return false;
        if (!world.getEntitiesWithinAABB(EntityHuntress.class, getEntityBoundingBox().grow(48)).isEmpty()) return false;
        return world.getDifficulty() != net.minecraft.world.EnumDifficulty.PEACEFUL && world.getCollisionBoxes(this, getEntityBoundingBox()).isEmpty() && world.checkNoEntityCollision(getEntityBoundingBox(), this);
    }
    @Override protected boolean isValidLightLevel() { return true; }
    @Override public void attackEntityWithRangedAttack(EntityLivingBase t, float dist) {
        EntityToxiconArrow a = new EntityToxiconArrow(world, this);
        double dx = t.posX - posX, dz = t.posZ - posZ, dy = t.getEntityBoundingBox().minY + t.height / 3f - a.posY;
        double h = MathHelper.sqrt(dx * dx + dz * dz);
        a.shoot(dx, dy + h * 0.2, dz, 1.7f, 10 - world.getDifficulty().getId() * 3);
        a.setDamage(a.getDamage() * 0.8 + dist * 0.5 + world.getDifficulty().getId() * 0.11);
        playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1f, 1f / (rand.nextFloat() * 0.4f + 0.8f));
        world.spawnEntity(a);
        shots++;
    }
    @Override public void setSwingingArms(boolean b) {}
    @Override public double getYOffset() { return -0.35; }
    @Override protected ResourceLocation getLootTable() { return Stage3.LOOT_HUNTRESS; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ILLAGER_DEATH; }
}
