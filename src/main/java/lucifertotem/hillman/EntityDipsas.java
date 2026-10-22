package lucifertotem.hillman;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Dipsas: the "thirst viper" of Lucan's Pharsalia. Its bite brings Dipsas Thirst, a burning that food cannot satisfy
 * (only drink helps; aged vinegar cures it). Harmless variant (Trophonius' sacred snakes) never bites.
 * Snakes leave a Mithridates king (venom tolerance 100) alone.
 */
public class EntityDipsas extends EntityMob {
    private static final DataParameter<Boolean> HARMLESS = EntityDataManager.createKey(EntityDipsas.class, DataSerializers.BOOLEAN);

    public EntityDipsas(World w) { super(w); setSize(0.7f, 0.35f); experienceValue = 4; }

    @Override protected void entityInit() { super.entityInit(); dataManager.register(HARMLESS, false); }
    public boolean harmless() { return dataManager.get(HARMLESS); }
    public void setHarmless(boolean b) { dataManager.set(HARMLESS, b); if (b) { setAttackTarget(null); enablePersistence(); } }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.2, false) {
            @Override public boolean shouldExecute() { return !harmless() && super.shouldExecute(); }
        });
        tasks.addTask(5, new EntityAIWander(this, 0.8));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0f));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, 10, true, false,
            p -> wouldTarget(p)));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
    }

    public static int bites = 0;

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (harmless()) return false;
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof EntityLivingBase) bite((EntityLivingBase) target);
        return hit;
    }

    public static void bite(EntityLivingBase t) {
        bites++;
        t.addPotionEffect(new PotionEffect(HPotions.THIRST, 1200, 0));
        if (t instanceof EntityPlayer) HBlocks.msg((EntityPlayer) t, "msg.lucifertotem.dipsas.bite");
        lucifertotem.v132.Crown.dipsasBite(t);   // 1.3.2: a crowned head goes PURPLE
    }

    @Override
    public boolean getCanSpawnHere() {
        if (world.getDifficulty() == net.minecraft.world.EnumDifficulty.PEACEFUL) return false;
        BlockPos p = new BlockPos(posX, getEntityBoundingBox().minY, posZ);
        Block below = world.getBlockState(p.down()).getBlock();
        if (!isNotColliding()) return false;
        if (world.provider.getDimension() == LuciferTotem.dimId) {
            if (below != ModContent.STAR_GRASS && below != ModContent.DUSKSTONE) return false;
            if (rand.nextInt(20) != 0) return false;
        } else {
            if (below != Blocks.SAND && below != Blocks.SANDSTONE) return false;
            if (!world.canSeeSky(p) || rand.nextInt(30) != 0) return false;
        }
        return world.getEntitiesWithinAABB(EntityDipsas.class, new AxisAlignedBB(p).grow(24)).size() < 3;
    }

    @Override protected boolean isValidLightLevel() { return true; }
    @Override public int getMaxSpawnedInChunk() { return 1; }
    @Override protected SoundEvent getAmbientSound() { return HSounds.DIPSAS_HISS; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return HSounds.DIPSAS_HISS; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SILVERFISH_DEATH; }
    @Override protected float getSoundPitch() { return 1.3f + rand.nextFloat() * 0.2f; }
    @Override public int getTalkInterval() { return 200; }
    @Override protected net.minecraft.util.ResourceLocation getLootTable() { return new net.minecraft.util.ResourceLocation(LuciferTotem.MODID, "entities/dipsas"); }
    @Override protected boolean canDespawn() { return !harmless(); }

    @Override public void writeEntityToNBT(NBTTagCompound t) { super.writeEntityToNBT(t); t.setBoolean("Harmless", harmless()); }
    @Override public void readEntityFromNBT(NBTTagCompound t) { super.readEntityFromNBT(t); setHarmless(t.getBoolean("Harmless")); }
    public boolean wouldTarget(EntityPlayer p) { return p != null && !harmless() && HData.venomTolerance(p) < 100; }
}
