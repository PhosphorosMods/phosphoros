package lucifertotem.v132;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.hillman.EntityDipsas;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The Crown of Thorns: worn by a player in the Lightfall, the Lightfall's creatures (the mod's and vanilla hostiles alike)
 * leave that player alone until the player strikes one of them; that one creature may then fight back. The Dipsas ignores
 * the crown entirely, and its bite on a crowned head gives PURPLE (see EntityDipsas.bite).
 */
public final class Crown {
    private Crown() {}
    public static final String COSMETIC = "cosmeticCrown";
    /** on the mob's entity data: space-separated UUIDs of crowned players who struck it first */
    public static final String PROVOKED = "ltCrownProvoked";
    public static int pacified, cancelledHits, provocations;

    public static boolean worn(EntityLivingBase e) {
        if (V132.CROWN == null) return false;
        if (e.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == V132.CROWN) return true;
        return e instanceof EntityPlayer && HData.get((EntityPlayer) e).getBoolean(COSMETIC);
    }

    public static boolean inLightfall(Entity e) { return e.world != null && e.world.provider.getDimension() == LuciferTotem.dimId; }

    /** the crown is doing its work for this player right now */
    public static boolean active(Entity e) {
        return LTConfig.v132.crownOfThorns && e instanceof EntityPlayer && !e.world.isRemote && inLightfall(e) && worn((EntityPlayer) e);
    }

    /** creatures the crown does not speak for: the Dipsas, owned/tamed animals and Dionysus himself, other players */
    public static boolean exempt(Entity mob) {
        if (!(mob instanceof EntityLiving)) return true;
        if (mob instanceof EntityDipsas) return true;
        if (mob instanceof IEntityOwnable && ((IEntityOwnable) mob).getOwnerId() != null) return true;
        return false;
    }

    public static boolean provoked(Entity mob, EntityPlayer p) {
        return mob.getEntityData().getString(PROVOKED).contains(p.getCachedUniqueIdString());
    }

    public static void provoke(Entity mob, EntityPlayer p) {
        NBTTagCompound d = mob.getEntityData();
        String s = d.getString(PROVOKED);
        if (s.contains(p.getCachedUniqueIdString())) return;
        d.setString(PROVOKED, s.isEmpty() ? p.getCachedUniqueIdString() : s + " " + p.getCachedUniqueIdString());
        provocations++;
    }

    /** should this mob leave this target alone? */
    public static boolean spares(Entity mob, Entity target) {
        return target instanceof EntityPlayer && active(target) && inLightfall(mob) && !exempt(mob) && !provoked(mob, (EntityPlayer) target);
    }

    public static class Events {
        @SubscribeEvent
        public void target(LivingSetAttackTargetEvent e) {
            if (e.getTarget() == null || e.getEntityLiving().world.isRemote) return;
            if (!spares(e.getEntityLiving(), e.getTarget())) return;
            EntityLiving mob = (EntityLiving) e.getEntityLiving();
            if (mob.getAttackTarget() == e.getTarget()) { mob.setAttackTarget(null); pacified++; }
            if (mob.getRevengeTarget() == e.getTarget()) mob.setRevengeTarget(null);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public void attack(LivingAttackEvent e) {
            EntityLivingBase victim = e.getEntityLiving();
            if (victim.world.isRemote) return;
            Entity src = e.getSource().getTrueSource();
            if (src == null) return;
            // the wearer struck first: that creature is free to answer
            if (src instanceof EntityPlayer && active(src) && victim instanceof EntityLiving && !exempt(victim)) { provoke(victim, (EntityPlayer) src); return; }
            // an unprovoked creature's blow (melee, arrow, spit) on a crowned player does nothing
            if (spares(src, victim)) {
                e.setCanceled(true);
                cancelledHits++;
                if (src instanceof EntityLiving && ((EntityLiving) src).getAttackTarget() == victim) ((EntityLiving) src).setAttackTarget(null);
            }
        }

        /** mobs that hold a target without re-firing the event (or set the field directly) are let go every half second */
        @SubscribeEvent
        public void update(LivingEvent.LivingUpdateEvent e) {
            EntityLivingBase l = e.getEntityLiving();
            if (l.world.isRemote || !(l instanceof EntityLiving) || (l.ticksExisted + l.getEntityId()) % 10 != 0) return;
            EntityLiving m = (EntityLiving) l;
            if (m.getAttackTarget() != null && spares(m, m.getAttackTarget())) { m.setAttackTarget(null); pacified++; }
        }

        @SubscribeEvent
        public void tick(TickEvent.PlayerTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || e.player.ticksExisted % 40 != 0) return;
            if (active(e.player) && HAdv.grant(e.player, "crown_of_thorns")) V132.codexOnce(e.player, "crown");
        }

        /** sneak + right-click a block with an empty hand: take a cosmetic crown back off */
        @SubscribeEvent
        public void rightClick(PlayerInteractEvent.RightClickBlock e) {
            EntityPlayer p = e.getEntityPlayer();
            if (p.world.isRemote || !p.isSneaking() || e.getHand() != net.minecraft.util.EnumHand.MAIN_HAND || !p.getHeldItemMainhand().isEmpty()) return;
            if (V132.takeOffCosmetic(p)) e.setCanceled(true);
        }
    }

    /** called from the Dipsas' bite: a crowned head in the Lightfall goes straight to PURPLE */
    public static int dipsasPurples;
    public static void dipsasBite(EntityLivingBase t) {
        if (!active(t)) return;
        t.addPotionEffect(new net.minecraft.potion.PotionEffect(lucifertotem.ModContent.PURPLE, LTConfig.v132.dipsasPurpleTicks, 0));
        dipsasPurples++;
        lucifertotem.hillman.HBlocks.msg((EntityPlayer) t, "msg.lucifertotem.crown_dipsas");
    }
}
