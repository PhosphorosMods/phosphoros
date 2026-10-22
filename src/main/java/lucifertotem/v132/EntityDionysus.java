package lucifertotem.v132;

import lucifertotem.LTConfig;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HBlocks;
import lucifertotem.v130.Stage3;
import lucifertotem.v130.V130;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Dionysus, stepped down from his statue: shoulder-length hair, full beard, a cream robe with a red sash, sandals,
 * and purple wine splashed over robe and face. Follows (or sits for) whoever set the statue, fights what hunts them, never strikes a player.
 * Water bottle -> Bacchic Wine. Bacchic Wine heals him.
 */
public class EntityDionysus extends EntityTameable {
    public static int conversions, swings;

    public EntityDionysus(World w) { super(w); setSize(0.7f, 2.2f); }

    @Override protected void initEntityAI() {
        aiSit = new EntityAISit(this);
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(2, aiSit);
        tasks.addTask(3, new EntityAIAttackMelee(this, 1.1, true));
        tasks.addTask(5, new EntityAIFollowOwner(this, 1.0, 8f, 2.5f));
        tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.8));
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8f));
        tasks.addTask(9, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIOwnerHurtByTarget(this));
        targetTasks.addTask(2, new EntityAIOwnerHurtTarget(this));
        targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(4, new EntityAINearestAttackableTarget<>(this, EntityLiving.class, 10, true, false, e -> e instanceof IMob && !(e instanceof EntityCreeper)));
    }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(LTConfig.v132.dionysusHealth);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(4);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24);
    }

    /** never a player (and never another's pet) */
    @Override public boolean shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
        if (target instanceof EntityPlayer || target instanceof EntityCreeper) return false;
        if (target instanceof EntityTameable && ((EntityTameable) target).isTamed()) return false;
        return true;
    }
    @Override public void setAttackTarget(EntityLivingBase t) { super.setAttackTarget(t instanceof EntityPlayer ? null : t); }
    @Override public boolean canAttackClass(Class<? extends EntityLivingBase> c) { return !EntityPlayer.class.isAssignableFrom(c) && c != EntityCreeper.class; }

    @Override public boolean attackEntityAsMob(Entity e) {
        if (e instanceof EntityPlayer) return false;
        float dmg = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean b = e.attackEntityFrom(DamageSource.causeMobDamage(this), dmg);
        if (b) {
            swings++;
            applyEnchantments(this, e);
            // the thyrsus touch: a short bout of Mania on what he strikes
            if (e instanceof EntityLivingBase && V130.MANIA != null && rand.nextInt(3) == 0) ((EntityLivingBase) e).addPotionEffect(new PotionEffect(V130.MANIA, 100, 0));
            world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.NEUTRAL, 1f, 0.8f);
        }
        return b;
    }

    @Override public boolean processInteract(EntityPlayer p, EnumHand h) {
        ItemStack s = p.getHeldItem(h);
        if (!isOwner(p)) return false;
        if (!s.isEmpty() && s.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(s) == PotionTypes.WATER) {
            if (!world.isRemote) convert(p, h);
            return true;
        }
        if (!s.isEmpty() && s.getItem() == Stage3.BACCHIC_WINE && getHealth() < getMaxHealth()) {
            if (!world.isRemote) {
                heal(12f);
                if (!p.capabilities.isCreativeMode) { s.shrink(1); V132.give(p, new ItemStack(Items.GLASS_BOTTLE)); }
                ((WorldServer) world).spawnParticle(EnumParticleTypes.HEART, posX, posY + height, posZ, 4, 0.3, 0.2, 0.3, 0);
            }
            return true;
        }
        if (s.isEmpty() && h == EnumHand.MAIN_HAND) {
            if (!world.isRemote) {
                boolean sit = !isSitting();
                setSitting(sit);
                aiSit.setSitting(sit);
                isJumping = false;
                navigator.clearPath();
                setAttackTarget(null);
                HBlocks.msg(p, isSitting() ? "msg.lucifertotem.dionysus_sits" : "msg.lucifertotem.dionysus_follows");
            }
            return true;
        }
        return super.processInteract(p, h);
    }

    /** "It is finished... fermenting": water to wine */
    public boolean convert(EntityPlayer p, EnumHand h) {
        if (!LTConfig.v132.wineMiracle || Stage3.BACCHIC_WINE == null) { HBlocks.msg(p, "msg.lucifertotem.dionysus_no_miracle"); return false; }
        ItemStack s = p.getHeldItem(h);
        ItemStack wine = new ItemStack(Stage3.BACCHIC_WINE);
        if (!p.capabilities.isCreativeMode) s.shrink(1);
        if (s.isEmpty()) p.setHeldItem(h, wine); else V132.give(p, wine);
        conversions++;
        world.playSound(null, posX, posY, posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1f, 0.8f);
        if (V130.LYRE != null) world.playSound(null, posX, posY, posZ, V130.LYRE, SoundCategory.NEUTRAL, 0.6f, 1.2f);
        if (world instanceof WorldServer) ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, p.posX, p.posY + 1.2, p.posZ, 16, 0.3, 0.3, 0.3, 0.05);
        HBlocks.msg(p, "msg.lucifertotem.dionysus_wine");
        HAdv.grant(p, "it_is_finished");
        return true;
    }

    @Override public void setTamedBy(EntityPlayer p) {
        setTamed(true);
        setOwnerId(p.getUniqueID());
    }

    @Override public EntityAgeable createChild(EntityAgeable a) { return null; }
    @Override public boolean isBreedingItem(ItemStack s) { return false; }
    @Override public boolean canMateWith(net.minecraft.entity.passive.EntityAnimal a) { return false; }
    @Override protected boolean canDespawn() { return false; }
    @Override public boolean isChild() { return false; }
    @Override public int getTalkInterval() { return 600; }
    @Override protected SoundEvent getAmbientSound() { return rand.nextInt(3) == 0 && V130.LYRE != null ? V130.LYRE : null; }
    @Override protected float getSoundVolume() { return 0.4f; }
    @Override protected SoundEvent getHurtSound(DamageSource d) { return SoundEvents.BLOCK_WOOD_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLOCK_WOOD_BREAK; }
    @Override protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.Block b) { playSound(SoundEvents.BLOCK_WOOD_STEP, 0.3f, 0.8f); }
    @Override protected ResourceLocation getLootTable() { return null; }
    @Override protected void dropFewItems(boolean byPlayer, int looting) {
        entityDropItem(new ItemStack(V132.STATUE), 0.5f);   // he died, and he will come back
        entityDropItem(new ItemStack(Blocks.LOG, 2), 0.5f);
    }
    @Override public boolean isPotionApplicable(PotionEffect e) { return e.getPotion() != net.minecraft.init.MobEffects.POISON && super.isPotionApplicable(e); }
}
