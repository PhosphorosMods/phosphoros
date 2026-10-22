package lucifertotem.v130;

import lucifertotem.LuciferTotem;
import lucifertotem.world.LightfallChunkGenerator;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;

/** Tyrian Murex: a spiny purple sea snail that creeps over the Lightfall sea floor. Crush it for Tyrian Purple. */
public class EntityMurex extends EntityWaterMob {
    public static final ResourceLocation LOOT = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "entities/murex"));
    private int creep;

    public EntityMurex(World w) { super(w); setSize(0.5f, 0.35f); experienceValue = 1; }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(2.0);
    }

    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) return;
        if (isInWater()) {
            motionY -= 0.02;                       // a snail sinks
            if (onGround || collidedVertically) {
                if (--creep <= 0) { creep = 40 + rand.nextInt(80); rotationYaw = rand.nextFloat() * 360f; }
                double sp = 0.008;
                motionX = -Math.sin(Math.toRadians(rotationYaw)) * sp;
                motionZ = Math.cos(Math.toRadians(rotationYaw)) * sp;
            }
        } else {
            motionX *= 0.5; motionZ *= 0.5;
        }
        renderYawOffset = rotationYaw;
    }

    @Override public void travel(float s, float v, float f) { move(net.minecraft.entity.MoverType.SELF, motionX, motionY, motionZ); motionX *= 0.9; motionZ *= 0.9; motionY *= 0.8; }

    @Override public boolean getCanSpawnHere() {
        if (posY >= LightfallChunkGenerator.SEA_LEVEL - 3) return false;
        if (world.getEntitiesWithinAABB(EntityMurex.class, new AxisAlignedBB(new BlockPos(this)).grow(16)).size() >= 6) return false;
        return super.getCanSpawnHere();
    }
    @Override public int getMaxSpawnedInChunk() { return 3; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
    @Override protected SoundEvent getDeathSound() { return V130.MUREX_CRUNCH; }
    @Override protected SoundEvent getHurtSound(DamageSource d) { return SoundEvents.ENTITY_GENERIC_SMALL_FALL; }
    @Override protected boolean canTriggerWalking() { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
}
