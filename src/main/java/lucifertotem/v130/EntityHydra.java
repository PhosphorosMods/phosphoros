package lucifertotem.v130;

import lucifertotem.hillman.HAdv;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The Hydra of the Sunken Chapel. Starts with three heads; a heavy blow (8+) cuts one off, and unless she is burning
 * two grow back five seconds later. Fire cauterizes. Cut the last head and she dies.
 */
public class EntityHydra extends EntityMob {
    public static final DataParameter<Integer> HEADS = EntityDataManager.createKey(EntityHydra.class, DataSerializers.VARINT);
    public static final int MAX_HEADS = 9, REGROW_TICKS = 100;
    public static int cut = 0, regrown = 0, cauterized = 0, slain = 0;
    public int regrowAt = -1, pending = 0;
    private final BossInfoServer boss = new BossInfoServer(getDisplayName(), BossInfo.Color.GREEN, BossInfo.Overlay.NOTCHED_10);
    public EntityHydra(World w) { super(w); setSize(2.2f, 2.6f); experienceValue = 60; }
    @Override protected void entityInit() { super.entityInit(); dataManager.register(HEADS, 3); }
    public int heads() { return dataManager.get(HEADS); }
    public void setHeads(int n) { dataManager.set(HEADS, Math.max(0, Math.min(MAX_HEADS, n))); }
    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.0, false));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0));
        tasks.addTask(7, new EntityAIWander(this, 0.6));
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 12f));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(120);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(4);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.22);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9);
    }
    /** every head bites */
    @Override public boolean attackEntityAsMob(Entity e) {
        boolean b = e.attackEntityFrom(DamageSource.causeMobDamage(this), 2f + 1.5f * heads());
        if (b && e instanceof EntityLivingBase) { ((EntityLivingBase) e).addPotionEffect(new PotionEffect(MobEffects.POISON, 60 + 20 * heads(), 0)); applyEnchantments(this, e); }
        return b;
    }
    @Override public boolean attackEntityFrom(DamageSource src, float amt) {
        boolean hit = super.attackEntityFrom(src, amt);
        if (hit && !world.isRemote && amt >= 8f && src.getTrueSource() != null && heads() > 0 && !isDead && getHealth() > 0) {
            setHeads(heads() - 1);
            cut++;
            playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_HURT, 1.5f, 0.6f);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR, posX, posY + height, posZ, 12, 0.6, 0.3, 0.6, 0.1);
            if (heads() == 0) { hurtResistantTime = 0; super.attackEntityFrom(src, getHealth() + 1000f); return true; }
            if (isBurning()) { cauterized++; pending = 0; regrowAt = -1; }
            else { pending += 2; regrowAt = ticksExisted + REGROW_TICKS; }
        }
        return hit;
    }
    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) return;
        if (regrowAt >= 0 && ticksExisted >= regrowAt) {
            if (isBurning()) cauterized++;
            else { int before = heads(); setHeads(before + pending); regrown += heads() - before; playSound(SoundEvents.ENTITY_SLIME_SQUISH, 1.5f, 0.5f); }
            pending = 0; regrowAt = -1;
        }
        boss.setPercent(getHealth() / getMaxHealth());
    }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByWater() { return false; }
    @Override public boolean isPotionApplicable(PotionEffect p) { return p.getPotion() != MobEffects.POISON && super.isPotionApplicable(p); }
    @Override public void addTrackingPlayer(EntityPlayerMP p) { super.addTrackingPlayer(p); boss.addPlayer(p); }
    @Override public void removeTrackingPlayer(EntityPlayerMP p) { super.removeTrackingPlayer(p); boss.removePlayer(p); }
    @Override public void writeEntityToNBT(NBTTagCompound c) { super.writeEntityToNBT(c); c.setInteger("Heads", heads()); c.setInteger("Pending", pending); c.setInteger("RegrowIn", regrowAt < 0 ? -1 : regrowAt - ticksExisted); }
    @Override public void readEntityFromNBT(NBTTagCompound c) {
        super.readEntityFromNBT(c);
        if (c.hasKey("Heads")) setHeads(c.getInteger("Heads"));
        pending = c.getInteger("Pending");
        int in = c.hasKey("RegrowIn") ? c.getInteger("RegrowIn") : -1;
        regrowAt = in < 0 ? -1 : ticksExisted + in;
    }
    @Override public void onDeath(DamageSource src) {
        super.onDeath(src);
        if (world.isRemote) return;
        slain++;
        if (src.getTrueSource() instanceof EntityPlayer) HAdv.grant((EntityPlayer) src.getTrueSource(), "hydra");
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(24))) HAdv.grant(p, "hydra");
    }
    @Override protected ResourceLocation getLootTable() { return Stage5.LOOT_HYDRA; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_GUARDIAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource d) { return SoundEvents.ENTITY_GUARDIAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH; }
    @Override protected float getSoundPitch() { return 0.5f; }
    @Override public boolean isNonBoss() { return false; }
}
