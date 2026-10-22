package lucifertotem.v130;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * One of Hekate's black hounds, loosed on whoever brings the wrong offering (or brings it at the wrong hour).
 * Glowing eyes, cannot be tamed, fades back into the dark after a while.
 */
public class EntityHekateHound extends EntityWolf {
    public static int bites = 0;
    int life = 0;
    /** a labyrinth guard: never fades */
    public boolean guard = false;
    public EntityHekateHound(World w) { super(w); setAngry(true); }
    @Override protected void initEntityAI() {
        aiSit = new EntityAISit(this);   // EntityTameable reads it from NBT
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(3, new EntityAILeapAtTarget(this, 0.4f));
        tasks.addTask(4, new EntityAIAttackMelee(this, 1.2, true));
        tasks.addTask(8, new EntityAIWanderAvoidWater(this, 1.0));
        tasks.addTask(10, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 10, false, false, p -> !Crops.warded(p)));   // Moly wards off her hounds
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.36);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
    }
    @Override public boolean attackEntityAsMob(Entity e) {
        boolean b = e.attackEntityFrom(DamageSource.causeMobDamage(this), 4f);
        if (b) { applyEnchantments(this, e); bites++; }
        return b;
    }
    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        setAngry(true);
        if (!world.isRemote && getAttackTarget() != null && Crops.warded(getAttackTarget())) setAttackTarget(null);
        if (world.isRemote) { if (rand.nextInt(4) == 0) world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height, posZ + (rand.nextDouble() - 0.5) * width, 0, 0, 0); return; }
        if (getAttackTarget() == null) life++; else life = Math.max(0, life - 2);
        if (!guard && (life > 2400 || ticksExisted > 12000)) {
            ((net.minecraft.world.WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + 0.5, posZ, 12, 0.3, 0.4, 0.3, 0.02);
            setDead();
        }
    }
    @Override public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound c) { super.writeEntityToNBT(c); c.setBoolean("Guard", guard); }
    @Override public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound c) { super.readEntityFromNBT(c); guard = c.getBoolean("Guard"); }
    @Override protected boolean canDespawn() { return !guard && super.canDespawn(); }
    @Override public boolean processInteract(EntityPlayer p, EnumHand h) { return false; }
    @Override public boolean isBreedingItem(net.minecraft.item.ItemStack s) { return false; }
    @Override public boolean canMateWith(net.minecraft.entity.passive.EntityAnimal a) { return false; }
    @Override protected ResourceLocation getLootTable() { return null; }
    @Override protected net.minecraft.util.SoundEvent getAmbientSound() { return rand.nextInt(8) == 0 ? V130.HOUND_HOWL : net.minecraft.init.SoundEvents.ENTITY_WOLF_GROWL; }
}
