package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;


import java.util.List;

/**
 * The Enchantress: a robed scholar-sorceress of the Library of Alexandria. Keeps her distance and casts
 * enchantment-themed spells: a levitating "Lift" bolt, hexes of Slowness and Weakness, and calls Book Wraiths
 * (animated, flying books) to her side. Drops enchanted books, lapis and plenty of XP.
 */
public class EntityEnchantress extends EntityMob implements IRangedAttackMob {
    public static final ResourceLocation LOOT = new ResourceLocation(LuciferTotem.MODID, "entities/enchantress");
    public static int casts = 0, lifts = 0, hexes = 0, summons = 0;
    private int summonCooldown = 100;

    public EntityEnchantress(World w) { super(w); setSize(0.6f, 1.95f); experienceValue = 20; }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackRanged(this, 0.9, 40, 16f));
        tasks.addTask(3, new EntityAIMoveTowardsRestriction(this, 0.8));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.6));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 12f));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0);
    }

    
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance d, IEntityLivingData data) {
        data = super.onInitialSpawn(d, data);
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.ENCHANTED_BOOK));
        setDropChance(EntityEquipmentSlot.MAINHAND, 0f);
        return data;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (summonCooldown > 0) summonCooldown--;
        if (world.isRemote && rand.nextInt(4) == 0)
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, posX + (rand.nextDouble() - 0.5), posY + 1.8 + rand.nextDouble() * 0.5, posZ + (rand.nextDouble() - 0.5), 0, -0.6, 0);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float power) {
        int spell;
        if (summonCooldown <= 0 && wraiths() < 2) spell = 3;
        else spell = rand.nextInt(3);
        cast(target, spell);
    }

    /** 0 = Lift (levitation bolt), 1 = Hex of Slowness, 2 = Hex of Weakness, 3 = call Book Wraiths */
    public void cast(EntityLivingBase target, int spell) {
        if (world.isRemote || target == null) return;
        casts++;
        playSound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, 1f, 1.2f);
        swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        switch (spell) {
            case 0:
                EntityShulkerBullet b = new EntityShulkerBullet(world, this, target, EnumFacing.Axis.Y);
                b.setPosition(posX, posY + getEyeHeight(), posZ);
                world.spawnEntity(b);
                lifts++;
                break;
            case 1:
            case 2:
                if (!canEntityBeSeen(target)) return;
                target.addPotionEffect(new PotionEffect(spell == 1 ? MobEffects.SLOWNESS : MobEffects.WEAKNESS, 120, 1));
                beam(target);
                hexes++;
                break;
            default:
                for (int i = 0; i < 2; i++) {
                    EntityBookWraith wr = new EntityBookWraith(world);
                    wr.setLocationAndAngles(posX + rand.nextGaussian(), posY + 1.5, posZ + rand.nextGaussian(), 0, 0);
                    wr.setOwner(this);
                    wr.setLimitedLife(20 * (25 + rand.nextInt(15)));
                    wr.setAttackTarget(target);
                    world.spawnEntity(wr);
                }
                summonCooldown = 400;
                summons++;
                break;
        }
    }

    int wraiths() {
        List<EntityBookWraith> l = world.getEntitiesWithinAABB(EntityBookWraith.class, getEntityBoundingBox().grow(16), b -> b != null && b.getOwner() == this);
        return l.size();
    }

    void beam(EntityLivingBase t) {
        if (!(world instanceof WorldServer)) return;
        Vec3d a = new Vec3d(posX, posY + getEyeHeight(), posZ), d = new Vec3d(t.posX, t.posY + t.height * 0.6, t.posZ).subtract(a);
        for (int i = 0; i <= 12; i++) {
            Vec3d p = a.add(d.scale(i / 12.0));
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override public void setSwingingArms(boolean swinging) { }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_EVOCATION_ILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_EVOCATION_ILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.EVOCATION_ILLAGER_DEATH; }
    @Override protected float getSoundPitch() { return 1.25f + (rand.nextFloat() - 0.5f) * 0.1f; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
