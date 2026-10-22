package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**
 * The Gorgon (boss, guards the Morningstar Citadel). Meeting her eyes adds Petrifying stacks; five petrify you
 * (Petrified: cannot move or jump, grey world). Block with a Bronze Shield while facing her and you see only her
 * reflection: no stacks, and the gaze rebounds on her. She is day-blind: strong block light (13+) on her stuns her.
 * Calls Dipsas vipers at half and quarter health. Creatures she kills turn to stone. Drops the blood of both veins
 * (Asclepius: the left vein kills, the right vein raises the dead).
 */
public class EntityGorgon extends EntityMob {
    private final BossInfoServer bar = (BossInfoServer) new BossInfoServer(getDisplayName(), BossInfo.Color.GREEN, BossInfo.Overlay.NOTCHED_10).setDarkenSky(true);
    private boolean called50, called25;
    public static int gazeHits = 0, reflections = 0, petrifications = 0;

    public EntityGorgon(World w) { super(w); setSize(0.9f, 2.4f); experienceValue = 120; isImmuneToFire = true; }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.0, true));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.8));
        tasks.addTask(6, new EntityAIWander(this, 0.6));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 24f));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(200.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.24);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(9.0);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(8.0);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance d, IEntityLivingData data) { enablePersistence(); return super.onInitialSpawn(d, data); }

    public boolean stunned() { return world.getLightFor(EnumSkyBlock.BLOCK, new BlockPos(posX, posY + 1.5, posZ)) >= 13; }

    /** true if the player is looking into her eyes (within ~11 degrees) and can see her */
    public boolean meetsGaze(EntityPlayer p) {
        Vec3d look = p.getLook(1f).normalize();
        Vec3d to = new Vec3d(posX - p.posX, posY + getEyeHeight() - (p.posY + p.getEyeHeight()), posZ - p.posZ);
        double len = to.length();
        if (len > 24 || len < 1e-3) return false;
        return look.dotProduct(to.scale(1 / len)) > 0.98 && p.canEntityBeSeen(this);
    }

    public static boolean shieldUp(EntityPlayer p) {
        ItemStack a = p.getActiveItemStack();
        return p.isActiveItemStackBlocking() && a.getItem() == HItems.BRONZE_SHIELD;
    }

    /** one gaze tick for one player; returns 0 none, 1 stack, 2 petrified, 3 reflected */
    public int gaze(EntityPlayer p) {
        if (p.isCreative() || p.isSpectator() || stunned()) return 0;
        if (shieldUp(p)) {
            Vec3d look = p.getLook(1f);
            Vec3d to = new Vec3d(posX - p.posX, 0, posZ - p.posZ).normalize();
            if (look.x * to.x + look.z * to.z > 0.5) {
                reflections++;
                attackEntityFrom(DamageSource.causePlayerDamage(p).setMagicDamage(), 2.0f);
                if (p.getEntityData().getLong("lt_reflect_msg") + 200 < world.getTotalWorldTime()) {
                    p.getEntityData().setLong("lt_reflect_msg", world.getTotalWorldTime());
                    HBlocks.msg(p, "msg.lucifertotem.gorgon.reflect");
                }
                return 3;
            }
        }
        if (!meetsGaze(p)) return 0;
        return petrifyStack(p);
    }

    public static int petrifyStack(EntityLivingBase p) {
        gazeHits++;
        PotionEffect cur = p.getActivePotionEffect(HPotions.PETRIFYING);
        int stacks = cur == null ? 1 : cur.getAmplifier() + 2;
        if (stacks >= 5) {
            p.removePotionEffect(HPotions.PETRIFYING);
            p.addPotionEffect(new PotionEffect(HPotions.PETRIFIED, 80, 0));
            p.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 80, 3));
            petrifications++;
            p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.HOSTILE, 1.5f, 0.5f);
            return 2;
        }
        p.addPotionEffect(new PotionEffect(HPotions.PETRIFYING, 100, stacks - 1));
        return 1;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) return;
        if (stunned()) {
            addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30, 3, false, false));
            if (ticksExisted % 20 == 0) ((net.minecraft.world.WorldServer) world).spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL, posX, posY + 2.4, posZ, 8, 0.3, 0.2, 0.3, 0.01);
        } else if (ticksExisted % 10 == 0) {
            for (EntityPlayer p : world.playerEntities) if (p.getDistanceSq(this) < 24 * 24) gaze(p);
        }
        float hp = getHealth() / getMaxHealth();
        if (!called50 && hp <= 0.5f) { called50 = true; callSnakes(3); }
        if (!called25 && hp <= 0.25f) { called25 = true; callSnakes(4); }
    }

    void callSnakes(int n) {
        playSound(HSounds.DIPSAS_HISS, 3f, 0.6f);
        for (int i = 0; i < n; i++) {
            EntityDipsas d = new EntityDipsas(world);
            d.setLocationAndAngles(posX + rand.nextGaussian() * 2, posY, posZ + rand.nextGaussian() * 2, rand.nextFloat() * 360, 0);
            if (getAttackTarget() != null) d.setAttackTarget(getAttackTarget());
            world.spawnEntity(d);
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity t) {
        boolean hit = super.attackEntityAsMob(t);
        if (hit && t instanceof EntityLivingBase) ((EntityLivingBase) t).addPotionEffect(new PotionEffect(MobEffects.POISON, 100, 1)); // snake hair
        return hit;
    }

    @Override
    protected void dropFewItems(boolean hit, int looting) {
        entityDropItem(new ItemStack(HItems.BLOOD_LEFT, 2 + rand.nextInt(2 + looting)), 0.5f);
        entityDropItem(new ItemStack(HItems.BLOOD_RIGHT, 1), 0.5f);
        entityDropItem(new ItemStack(lucifertotem.ModContent.SHARD, 1 + rand.nextInt(2)), 0.5f);
    }

    @Override
    public void onDeath(DamageSource s) {
        super.onDeath(s);
        if (!world.isRemote) for (EntityPlayer p : world.playerEntities) if (p.getDistanceSq(this) < 48 * 48) HAdv.grant(p, "perseus");
    }

    @Override public boolean isNonBoss() { return false; }
    @Override protected boolean canDespawn() { return false; }
    @Override public void addTrackingPlayer(EntityPlayerMP p) { super.addTrackingPlayer(p); bar.addPlayer(p); }
    @Override public void removeTrackingPlayer(EntityPlayerMP p) { super.removeTrackingPlayer(p); bar.removePlayer(p); }
    @Override protected void updateAITasks() { super.updateAITasks(); bar.setPercent(getHealth() / getMaxHealth()); }
    @Override protected SoundEvent getAmbientSound() { return HSounds.DIPSAS_HISS; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_ELDER_GUARDIAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH; }
    @Override protected float getSoundPitch() { return 0.7f; }
    @Override public void fall(float d, float m) { }
    @Override public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound t) { super.writeEntityToNBT(t); t.setBoolean("C50", called50); t.setBoolean("C25", called25); }
    @Override public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound t) { super.readEntityFromNBT(t); called50 = t.getBoolean("C50"); called25 = t.getBoolean("C25"); if (hasCustomName()) bar.setName(getDisplayName()); }
}
