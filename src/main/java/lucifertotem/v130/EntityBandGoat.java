package lucifertotem.v130;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** the black goat of Devil's Flight: harmless, and it headbangs whenever music plays nearby */
public class EntityBandGoat extends EntityCreature {
    public boolean rocking;     // client and server: music within reach (rechecked every second)
    public static int bangs = 0;
    public EntityBandGoat(World w) { super(w); setSize(0.9f, 1.3f); }
    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 1.4));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.8) { @Override public boolean shouldExecute() { return !rocking && super.shouldExecute(); } });
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8f));
        tasks.addTask(7, new EntityAILookIdle(this));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.22);
    }
    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        if (ticksExisted % 20 == 1) {
            boolean r = EntityBacchant.music(world, getPosition());
            if (r && !rocking && !world.isRemote) bangs++;
            rocking = r;
        }
        if (rocking && !world.isRemote) getNavigator().clearPath();
    }
    @Override protected boolean canDespawn() { return false; }
    @Override protected SoundEvent getAmbientSound() { return V130.GOAT_BLEAT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return V130.GOAT_BLEAT; }
    @Override protected SoundEvent getDeathSound() { return V130.GOAT_BLEAT; }
    @Override protected float getSoundPitch() { return rocking ? 0.8f : 1f; }
    @Override protected net.minecraft.util.ResourceLocation getLootTable() { return null; }
}
