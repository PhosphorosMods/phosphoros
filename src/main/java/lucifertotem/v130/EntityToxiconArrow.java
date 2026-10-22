package lucifertotem.v130;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

/** an arrow smeared with toxicon: Slowness IV for two seconds, then Mania */
public class EntityToxiconArrow extends EntityArrow {
    public EntityToxiconArrow(World w) { super(w); }
    public EntityToxiconArrow(World w, EntityLivingBase shooter) { super(w, shooter); }
    public EntityToxiconArrow(World w, double x, double y, double z) { super(w, x, y, z); }
    @Override protected ItemStack getArrowStack() { return new ItemStack(Stage3.TOXICON_ARROW); }
    @Override protected void arrowHit(EntityLivingBase e) { super.arrowHit(e); Stage3.toxicon(e); }
    @Override public void onUpdate() {
        super.onUpdate();
        if (world.isRemote && !inGround && ticksExisted % 2 == 0) world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX, posY, posZ, 0.35, 0.55, 0.1);
    }
}
