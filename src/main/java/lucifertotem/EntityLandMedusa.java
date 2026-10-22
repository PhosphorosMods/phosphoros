package lucifertotem;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Land Medusa: a purple-haired, winged fallen angel of the Lightfall islands (our own take on the Aether's Valkyrie,
 * no Aether needed). Neutral until struck, then fights back in melee with sticky goo (brief Slowness).
 * Flutters instead of falling, never takes fall damage, and leaves a cartoonish trail of dripping purple goo (client particles only).
 * Drops: sometimes a Bottle of Theriac (Looting raises the chance), a feather or two.
 */
public class EntityLandMedusa extends EntityMob {
    public float wingPhase, prevWingPhase; // client animation

    public EntityLandMedusa(World w) {
        super(w);
        setSize(0.6f, 1.95f);
        experienceValue = 8;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.0, false));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.6));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0f));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true)); // neutral: only answers attacks (and calls her sisters)
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(4.0);
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof EntityLivingBase) ((EntityLivingBase) target).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 0)); // sticky goo
        return hit;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        // wings: glide down instead of dropping
        if (!onGround && motionY < -0.12) { motionY = -0.12; fallDistance = 0; }
        if (world.isRemote) {
            prevWingPhase = wingPhase;
            wingPhase += onGround ? 0.08f : 0.45f;
            LuciferTotem.proxy.gooDrip(this);
        }
    }

    @Override public void fall(float distance, float mult) { } // winged

    /** island tops only: star grass / duskstone under the open (dusk) sky; light does not matter in the eternal twilight */
    @Override
    public boolean getCanSpawnHere() {
        if (world.getDifficulty() == net.minecraft.world.EnumDifficulty.PEACEFUL) return false;
        BlockPos p = new BlockPos(posX, getEntityBoundingBox().minY, posZ);
        Block below = world.getBlockState(p.down()).getBlock();
        if (below != ModContent.STAR_GRASS && below != ModContent.DUSKSTONE) return false;
        if (!world.canSeeSky(p) || !isNotColliding()) return false;
        if (rand.nextDouble() >= LTConfig.landMedusaSpawnChance) return false;
        return world.getEntitiesWithinAABB(EntityLandMedusa.class, new AxisAlignedBB(p).grow(32)).size() < LTConfig.landMedusaLocalCap;
    }

    @Override public int getMaxSpawnedInChunk() { return 2; }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        if (rand.nextFloat() < LTConfig.theriacDropChance + LTConfig.theriacLootingBonus * looting)
            entityDropItem(new ItemStack(ModContent.THERIAC), 0.5f);
        int f = rand.nextInt(2 + looting);
        if (f > 0) entityDropItem(new ItemStack(net.minecraft.init.Items.FEATHER, f), 0.5f);
    }

    @Override protected SoundEvent getAmbientSound() { return lucifertotem.hillman.HSounds.SQUISH_SMALL; } // vanilla small_slime.squish is silent in 1.12
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SMALL_SLIME_DEATH; }
    @Override public int getTalkInterval() { return 240; }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() { return Math.max(super.getBrightnessForRender(), 0x00A000A0); } // faint glow
}
