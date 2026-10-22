package lucifertotem.v130;

import lucifertotem.hillman.HAdv;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;

/** The Drakaina of Delphi: a robed serpent-woman coiled under the vent. Poison bite, venom spit, a terrible hiss. */
public class EntityDrakaina extends EntityMob implements IRangedAttackMob {
    public static int spits = 0, bites = 0, slain = 0;
    int spitCooldown = 40;
    private final BossInfoServer boss = (BossInfoServer) new BossInfoServer(getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
    public EntityDrakaina(World w) { super(w); setSize(0.9f, 2.4f); experienceValue = 50; }
    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.1, false));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0));
        tasks.addTask(7, new EntityAIWander(this, 0.8));
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 12f));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(80);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.26);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
    }
    @Override public boolean attackEntityAsMob(Entity e) {
        boolean b = super.attackEntityAsMob(e);
        if (b && e instanceof EntityLivingBase) { ((EntityLivingBase) e).addPotionEffect(new PotionEffect(MobEffects.POISON, 140, 1)); bites++; }
        return b;
    }
    @Override protected void updateAITasks() {
        super.updateAITasks();
        if (spitCooldown > 0) spitCooldown--;
        EntityLivingBase t = getAttackTarget();
        if (t != null && spitCooldown <= 0 && getDistanceSq(t) > 16 && getDistanceSq(t) < 400 && canEntityBeSeen(t)) { attackEntityWithRangedAttack(t, 1f); spitCooldown = 50; }
        boss.setPercent(getHealth() / getMaxHealth());
    }
    @Override public void attackEntityWithRangedAttack(EntityLivingBase t, float f) {
        EntityVenomSpit s = new EntityVenomSpit(world, this);
        double dx = t.posX - posX, dz = t.posZ - posZ, dy = t.posY + t.getEyeHeight() * 0.5 - s.posY;
        s.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.1, dz, 1.3f, 4f);
        playSound(SoundEvents.ENTITY_LLAMA_SPIT, 1f, 0.6f);
        world.spawnEntity(s);
        spits++;
    }
    @Override public void setSwingingArms(boolean b) {}
    @Override public boolean isPotionApplicable(PotionEffect p) { return p.getPotion() != MobEffects.POISON && super.isPotionApplicable(p); }
    @Override public void addTrackingPlayer(EntityPlayerMP p) { super.addTrackingPlayer(p); boss.addPlayer(p); }
    @Override public void removeTrackingPlayer(EntityPlayerMP p) { super.removeTrackingPlayer(p); boss.removePlayer(p); }
    @Override public void onDeath(DamageSource src) {
        super.onDeath(src);
        if (world.isRemote) return;
        slain++;
        if (src.getTrueSource() instanceof EntityPlayer) HAdv.grant((EntityPlayer) src.getTrueSource(), "drakaina");
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(24))) HAdv.grant(p, "drakaina");
    }
    @Override protected ResourceLocation getLootTable() { return Stage5.LOOT_DRAKAINA; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_CAT_HISS; }
    @Override protected SoundEvent getHurtSound(DamageSource d) { return SoundEvents.ENTITY_CAT_HISS; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ENDERDRAGON_GROWL; }
    @Override protected float getSoundPitch() { return 0.55f; }
    @Override public boolean isNonBoss() { return false; }
}
