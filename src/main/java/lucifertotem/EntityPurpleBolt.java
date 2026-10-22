package lucifertotem;

import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Morningstar's violet lightning. Deliberately NOT an EntityLightningBolt (so lightning-reacting mods and lightning
 * filters leave it alone, and it never sets fire, never damages blocks, never turns pigs into pigmen, etc).
 * Purely visual + thunder; the judgement itself is applied by EntityThrownTotem.smite.
 */
public class EntityPurpleBolt extends Entity {
    private int lightningState;
    public long boltVertex;
    private int boltLivingTime;

    public EntityPurpleBolt(World w) {
        super(w);
        ignoreFrustumCheck = true;
        lightningState = 2;
        boltVertex = rand.nextLong();
        boltLivingTime = rand.nextInt(3) + 1;
        setSize(0.1f, 0.1f);
    }

    public EntityPurpleBolt(World w, double x, double y, double z) {
        this(w);
        setLocationAndAngles(x, y, z, 0f, 0f);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (lightningState == 2) {
            if (!world.isRemote) {
                world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 10000.0f, 0.6f + rand.nextFloat() * 0.2f);
                world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_LIGHTNING_IMPACT, SoundCategory.WEATHER, 2.0f, 0.5f + rand.nextFloat() * 0.2f);
            } else {
                LuciferTotem.proxy.boltFlash(world);
            }
        }
        --lightningState;
        if (lightningState < 0) {
            if (boltLivingTime == 0) {
                setDead();
            } else if (lightningState < -rand.nextInt(10)) {
                --boltLivingTime;
                lightningState = 1;
                boltVertex = rand.nextLong();
                if (world.isRemote) LuciferTotem.proxy.boltFlash(world);
            }
        }
        if (ticksExisted > 60) setDead();
    }

    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound c) { }
    @Override protected void writeEntityToNBT(NBTTagCompound c) { }
    @Override public boolean writeToNBTOptional(NBTTagCompound c) { return false; } // never saved to disk

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double d) { return true; }
}
