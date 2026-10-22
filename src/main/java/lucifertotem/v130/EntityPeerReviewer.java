package lucifertotem.v130;

import lucifertotem.hillman.EntityAmmonius;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** a rival scholar in a tweed jacket who haunts the Library of Alexandria, hurling books. "Citation needed!" */
public class EntityPeerReviewer extends EntityMob implements IRangedAttackMob {
    public static final String[] SHOUTS = {"citation", "reviewer2", "source", "rejected", "minor"};
    public static int thrown = 0, shouts = 0;
    private int shoutCool;

    public EntityPeerReviewer(World w) { super(w); setSize(0.6f, 1.95f); experienceValue = 8; }

    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackRanged(this, 1.0, 30, 12f));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.7));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 10f));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityAmmonius.class, true));
    }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(20.0);
    }

    @Override protected void setEquipmentBasedOnDifficulty(net.minecraft.world.DifficultyInstance d) { setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOOK)); }
    @Override public net.minecraft.entity.IEntityLivingData onInitialSpawn(net.minecraft.world.DifficultyInstance d, net.minecraft.entity.IEntityLivingData data) {
        data = super.onInitialSpawn(d, data); setEquipmentBasedOnDifficulty(d); setDropChance(EntityEquipmentSlot.MAINHAND, 0f); return data;
    }

    @Override public void onLivingUpdate() { super.onLivingUpdate(); if (shoutCool > 0) shoutCool--; }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase t, float f) {
        EntityThrownBook b = new EntityThrownBook(world, this);
        double dx = t.posX - posX, dy = t.getEntityBoundingBox().minY + t.height / 3.0 - b.posY, dz = t.posZ - posZ;
        double h = Math.sqrt(dx * dx + dz * dz);
        b.shoot(dx, dy + h * 0.2, dz, 1.3f, 6f);
        world.spawnEntity(b);
        thrown++;
        swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        shout(t);
    }

    /** "Citation needed!" (actionbar to the target and anyone within 12 blocks); no voice, just the subtitle cue */
    public void shout(EntityLivingBase t) {
        if (shoutCool > 0) return;
        shoutCool = 60;
        shouts++;
        String k = "shout.lucifertotem.reviewer." + SHOUTS[rand.nextInt(SHOUTS.length)];
        TextComponentTranslation c = new TextComponentTranslation(k);
        c.getStyle().setColor(TextFormatting.RED).setBold(true);
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(12))) p.sendStatusMessage(c, true);
        if (t instanceof EntityPlayerMP && t.getDistanceSq(this) > 144) ((EntityPlayerMP) t).sendStatusMessage(c, true);
        playSound(V130.CITATION, 0.8f, 0.8f + rand.nextFloat() * 0.3f);
    }

    @Override public void setSwingingArms(boolean b) { }
    @Override protected ResourceLocation getLootTable() { return Stage2.LOOT_REVIEWER; }
    @Override protected net.minecraft.util.SoundEvent getAmbientSound() { return net.minecraft.init.SoundEvents.ENTITY_VILLAGER_AMBIENT; }
    @Override protected net.minecraft.util.SoundEvent getHurtSound(net.minecraft.util.DamageSource s) { return net.minecraft.init.SoundEvents.ENTITY_VILLAGER_HURT; }
    @Override protected net.minecraft.util.SoundEvent getDeathSound() { return net.minecraft.init.SoundEvents.ENTITY_VILLAGER_DEATH; }
    @Override protected float getSoundPitch() { return 0.7f + rand.nextFloat() * 0.1f; }
}
