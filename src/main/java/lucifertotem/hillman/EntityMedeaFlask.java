package lucifertotem.hillman;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.block.material.Material;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/** thrown Medea's Fire: shatters into violet flames that burn even on water */
public class EntityMedeaFlask extends EntityThrowable {
    public EntityMedeaFlask(World w) { super(w); }
    public EntityMedeaFlask(World w, EntityLivingBase thrower) { super(w, thrower); }
    public EntityMedeaFlask(World w, double x, double y, double z) { super(w, x, y, z); }

    /** fly through water instead of stopping: the whole point is setting the sea alight */
    @Override protected float getGravityVelocity() { return 0.05f; }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // burst on touching the water surface
        if (!world.isRemote && !isDead && world.getBlockState(new BlockPos(this)).getMaterial() == Material.WATER) {
            ignite(world, new BlockPos(this));
            setDead();
        }
    }

    @Override
    protected void onImpact(RayTraceResult res) {
        if (world.isRemote) return;
        BlockPos at;
        if (res.typeOfHit == RayTraceResult.Type.ENTITY && res.entityHit != null) { res.entityHit.setFire(8); at = new BlockPos(res.entityHit); }
        else if (res.typeOfHit == RayTraceResult.Type.BLOCK) at = res.getBlockPos().offset(res.sideHit);
        else at = new BlockPos(this);
        ignite(world, at);
        setDead();
    }

    public static int ignite(World w, BlockPos center) {
        w.playSound(null, center, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.NEUTRAL, 1f, 0.7f);
        w.playSound(null, center, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.NEUTRAL, 1f, 0.6f);
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos p = center.add(dx, dy, dz);
                if ((w.isAirBlock(p) || w.getBlockState(p).getBlock().isReplaceable(w, p)) && w.getBlockState(p).getMaterial() != Material.WATER
                    && HBlocks.BlockMedeaFire.validBelow(w, p)) {
                    w.setBlockState(p, HBlocks.MEDEA_FIRE.getDefaultState(), 3);
                    n++;
                    break;
                }
            }
        }
        return n;
    }
}
