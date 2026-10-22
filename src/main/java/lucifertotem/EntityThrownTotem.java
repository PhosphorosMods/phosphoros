package lucifertotem;

import lucifertotem.world.PortalLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityThrownTotem extends EntityThrowable {
    public EntityThrownTotem(World w) { super(w); }
    public EntityThrownTotem(World w, EntityLivingBase thrower) { super(w, thrower); }

    @Override protected float getGravityVelocity() { return 0.02f; }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            for (int i = 0; i < 2; i++)
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX + (rand.nextDouble() - 0.5) * 0.3, posY + (rand.nextDouble() - 0.5) * 0.3, posZ + (rand.nextDouble() - 0.5) * 0.3, 0, 0, 0);
        } else if (ticksExisted > 200 || posY < -64) {
            setDead();
        }
    }

    /** No friendly fire: never the thrower, players, tamed pets, or anything carrying a player. */
    public static boolean isProtected(Entity target, Entity thrower) {
        if (target == null || target == thrower) return true;
        if (target instanceof EntityPlayer || target instanceof EntityArmorStand) return true;
        if (target instanceof EntityTameable && ((EntityTameable) target).isTamed()) return true;
        if (target instanceof AbstractHorse && ((AbstractHorse) target).isTame()) return true;
        if (target instanceof IEntityOwnable && ((IEntityOwnable) target).getOwnerId() != null) return true;
        for (Entity p : target.getPassengers()) if (p instanceof EntityPlayer) return true;
        return false;
    }

    @Override
    protected void onImpact(RayTraceResult r) {
        if (world.isRemote) return;
        if (r.typeOfHit == RayTraceResult.Type.ENTITY && r.entityHit != null) {
            Entity hit = r.entityHit;
            Entity root = hit instanceof MultiPartEntityPart && ((MultiPartEntityPart) hit).parent instanceof Entity ? (Entity) ((MultiPartEntityPart) hit).parent : hit;
            if (isProtected(root, getThrower()) || !(root instanceof EntityLivingBase)) return; // pass straight through
            smite(this, getThrower(), hit, (EntityLivingBase) root);
            setDead();
            return;
        }
        if (r.typeOfHit == RayTraceResult.Type.BLOCK) {
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY, posZ, 16, 0.2, 0.2, 0.2, 0.05);
            setDead();
        }
    }

    /** damage type "lucifer" (death message death.attack.lucifer[.player]); magic, ignores armour */
    public static class LuciferDamage extends EntityDamageSourceIndirect {
        public LuciferDamage(Entity proj, Entity thrower, boolean absolute) {
            super("lucifer", proj, thrower);
            setDamageBypassesArmor();
            setMagicDamage();
            if (absolute) setDamageIsAbsolute();
        }
    }

    public static DamageSource lethalSource(Entity proj, Entity thrower) { return new LuciferDamage(proj, thrower, true); }
    public static DamageSource bossSource(Entity proj, Entity thrower) { return new LuciferDamage(proj, thrower, false); }

    /** Calls the violet bolt onto the target and applies its judgement. Server side only. */
    public static void smite(Entity proj, EntityLivingBase thrower, Entity hitPart, EntityLivingBase target) {
        World w = target.world;
        w.spawnEntity(new EntityPurpleBolt(w, target.posX, target.posY, target.posZ));
        target.hurtResistantTime = 0;
        if (target instanceof EntityVillager) {
            BlockPos at = new BlockPos(target);
            target.attackEntityFrom(lethalSource(proj, thrower), 100000f);
            if (LTConfig.villagerPortals && LuciferTotem.dimRegistered) PortalLogic.openGateNear(w, at);
        } else if (target.isNonBoss()) {
            boolean took = target.attackEntityFrom(lethalSource(proj, thrower), 100000f);
            // some mobs (e.g. the Aether's Valkyries) only accept damage dealt directly by a player
            if (!took && target.isEntityAlive() && thrower instanceof net.minecraft.entity.player.EntityPlayer) {
                target.hurtResistantTime = 0;
                target.attackEntityFrom(new LuciferDamage(thrower, thrower, true), 100000f);
            }
        } else {
            hitPart.attackEntityFrom(bossSource(proj, thrower), (float) LTConfig.bossDamage);
        }
    }
}
