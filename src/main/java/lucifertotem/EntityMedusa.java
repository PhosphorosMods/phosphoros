package lucifertotem;

import lucifertotem.world.LightfallChunkGenerator;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Medusa: a glowing violet jellyfish that patrols the Lightfall sea in slow pulses.
 * Passive, but anything that touches or strikes it gets stung (small damage + brief poison and slowness).
 */
public class EntityMedusa extends EntityWaterMob {
    public float pulse, prevPulse;          // animation phase (both sides)
    private double tx, ty, tz;
    private int retarget, stingCooldown;

    public EntityMedusa(World w) {
        super(w);
        setSize(0.9f, 1.0f);
        pulse = rand.nextFloat() * 6.28f;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(12.0);
    }

    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_SQUID_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SQUID_DEATH; }
    @Override protected float getSoundVolume() { return 0.5f; }
    @Override public boolean isPushedByWater() { return false; }

    @Override
    public boolean getCanSpawnHere() {
        BlockPos p = new BlockPos(this);
        return world.getBlockState(p).getMaterial() == Material.WATER && world.getBlockState(p.up()).getMaterial() == Material.WATER && super.getCanSpawnHere();
    }

    @Override public int getMaxSpawnedInChunk() { return 3; }

    private void pickTarget() {
        for (int i = 0; i < 6; i++) { // only open water counts as a patrol point (never into air pockets of sunken ruins)
            tx = posX + (rand.nextDouble() - 0.5) * 24;
            tz = posZ + (rand.nextDouble() - 0.5) * 24;
            ty = posY + (rand.nextDouble() - 0.5) * 6;
            if (world.provider.getDimension() == LuciferTotem.dimId) ty = Math.min(ty, LightfallChunkGenerator.SEA_LEVEL - 1.5);
            if (isWater(tx, ty, tz)) break;
            tx = posX; ty = posY; tz = posZ;
        }
        retarget = 120 + rand.nextInt(120);
    }

    private boolean isWater(double x, double y, double z) {
        BlockPos p = new BlockPos(x, y, z);
        return world.isBlockLoaded(p) && world.getBlockState(p).getMaterial() == Material.WATER;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        prevPulse = pulse;
        pulse += 0.21f; // one pulse ≈ 30 ticks
        if (stingCooldown > 0) stingCooldown--;
        if (world.isRemote) {
            if (rand.nextInt(12) == 0)
                world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, posX + (rand.nextDouble() - 0.5) * 0.6, posY + 0.2, posZ + (rand.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
            return;
        }
        if (isInWater()) {
            double dx = tx - posX, dy = ty - posY, dz = tz - posZ;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (--retarget <= 0 || d < 1.5 || collidedHorizontally) { pickTarget(); return; }
            if (ticksExisted % 30 == 0) { // a pulse of the bell
                double f = 0.12 / d;
                motionX += dx * f; motionY += dy * f + 0.02; motionZ += dz * f;
            }
            float yaw = (float) (MathHelper.atan2(motionZ, motionX) * 180.0 / Math.PI) - 90f;
            renderYawOffset += MathHelper.wrapDegrees(yaw - renderYawOffset) * 0.05f;
            rotationYaw = renderYawOffset;
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (isInWater()) {
            // stay in the water: don't drift out of it into air (sunken rooms, the surface)
            if (!isWater(posX + motionX * 4, posY + 0.5 + motionY * 4, posZ + motionZ * 4)) {
                motionX *= -0.3; motionZ *= -0.3; if (motionY > 0) motionY = -0.02; else motionY *= -0.3;
            }
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.93; motionY *= 0.93; motionZ *= 0.93;
            motionY -= 0.0015; // gentle sinking between pulses
        } else {
            motionY -= 0.08;
            move(MoverType.SELF, motionX * 0.3, motionY, motionZ * 0.3);
            motionY *= 0.98;
        }
    }

    public void sting(EntityLivingBase target) {
        if (world.isRemote || stingCooldown > 0 || target == null || !target.isEntityAlive()) return;
        if (target instanceof EntityPlayer && (((EntityPlayer) target).capabilities.isCreativeMode || ((EntityPlayer) target).isSpectator())) return;
        stingCooldown = 20;
        target.attackEntityFrom(new EntityDamageSource("medusa", this), 2.0f);
        target.addPotionEffect(new PotionEffect(MobEffects.POISON, 60, 0));
        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0));
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer p) { sting(p); }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        Entity a = src.getTrueSource();
        if (a instanceof EntityLivingBase && a == src.getImmediateSource() && a.getDistanceSq(this) < 16) sting((EntityLivingBase) a);
        return super.attackEntityFrom(src, amount);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        if (rand.nextFloat() < LTConfig.priapusDropChance + LTConfig.priapusLootingBonus * looting)
            entityDropItem(new ItemStack(ModContent.PRIAPUS), 0.3f);
        if (rand.nextInt(3) == 0) entityDropItem(new ItemStack(net.minecraft.init.Items.GLOWSTONE_DUST, 1 + rand.nextInt(2 + looting)), 0.3f);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() { return 0xF000F0; } // glows in the dark sea
}
