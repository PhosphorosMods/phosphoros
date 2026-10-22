package lucifertotem.v130;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** a hurled monograph: 3 damage and a bit of knockback */
public class EntityThrownBook extends EntityThrowable {
    public static int hits = 0, impacts = 0;
    public EntityThrownBook(World w) { super(w); }
    public EntityThrownBook(World w, EntityLivingBase thrower) { super(w, thrower); }
    /** self-test: the impact on e */
    public void hit(net.minecraft.entity.Entity e) { onImpact(new RayTraceResult(e)); }
    @Override protected float getGravityVelocity() { return 0.04f; }
    @Override
    protected void onImpact(RayTraceResult r) {
        if (world.isRemote) return;
        if (r.entityHit != null && r.entityHit != getThrower()) {
            impacts++;
            if (r.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), 3f)) {
                hits++;
                r.entityHit.addVelocity(motionX * 0.2, 0.1, motionZ * 0.2);
                if (r.entityHit instanceof EntityPlayer) {
                    TextComponentTranslation c = new TextComponentTranslation("shout.lucifertotem.reviewer.citation");
                    c.getStyle().setColor(TextFormatting.RED).setBold(true);
                    ((EntityPlayer) r.entityHit).sendStatusMessage(c, true);
                }
            }
        }
        ((WorldServer) world).spawnParticle(EnumParticleTypes.ITEM_CRACK, posX, posY, posZ, 8, 0.1, 0.1, 0.1, 0.05, net.minecraft.item.Item.getIdFromItem(net.minecraft.init.Items.BOOK));
        setDead();
    }
}
