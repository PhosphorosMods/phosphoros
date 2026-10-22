package lucifertotem.v130;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** the Drakaina's venom: a green gob that stings and poisons */
public class EntityVenomSpit extends EntityThrowable {
    public static int hits = 0;
    public EntityVenomSpit(World w) { super(w); }
    public EntityVenomSpit(World w, EntityLivingBase thrower) { super(w, thrower); }
    @Override protected float getGravityVelocity() { return 0.02f; }
    @Override protected void onImpact(RayTraceResult r) {
        if (world.isRemote) return;
        if (r.entityHit != null && r.entityHit == getThrower()) return;
        if (r.entityHit instanceof EntityLivingBase) {
            EntityLivingBase t = (EntityLivingBase) r.entityHit;
            t.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), 5f);
            t.addPotionEffect(new PotionEffect(MobEffects.POISON, 100, 1));
            hits++;
        }
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME, posX, posY, posZ, 10, 0.2, 0.2, 0.2, 0.05);
        setDead();
    }
    /** for the tests */
    public void hit(net.minecraft.entity.Entity e) { onImpact(new RayTraceResult(e)); }
}
