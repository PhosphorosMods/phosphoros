package lucifertotem.v130;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Lucifer, the Morning Star, keeping court on the roof of the Morningstar Citadel. Robed in white and gold, a star for a crown. Talks; cannot be harmed. */
public class EntityLucifer extends EntityCreature {
    BlockPos home;
    public EntityLucifer(World w) { super(w); setSize(0.6f, 2.0f); setEntityInvulnerable(true); enablePersistence(); }
    @Override protected void initEntityAI() {
        tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 16f, 1f));
        tasks.addTask(2, new EntityAILookIdle(this));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
    }
    public void setHome(BlockPos p) { home = p; }
    @Override public boolean attackEntityFrom(DamageSource s, float a) { return s == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(s, a); }
    @Override public boolean canBePushed() { return false; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected boolean processInteract(EntityPlayer p, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (!world.isRemote) Interview.menu(p, this);
        return true;
    }
    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!world.isRemote && home != null && getDistanceSq(home) > 4) setPositionAndUpdate(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        if (world.isRemote && rand.nextInt(6) == 0) world.spawnParticle(EnumParticleTypes.END_ROD, posX + (rand.nextDouble() - 0.5) * 0.6, posY + 2.3, posZ + (rand.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
    }
    @Override public void writeEntityToNBT(NBTTagCompound c) { super.writeEntityToNBT(c); if (home != null) c.setLong("Home", home.toLong()); }
    @Override public void readEntityFromNBT(NBTTagCompound c) { super.readEntityFromNBT(c); if (c.hasKey("Home")) home = BlockPos.fromLong(c.getLong("Home")); }
}
