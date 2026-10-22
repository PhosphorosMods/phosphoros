package lucifertotem;

import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CommonEvents {
    /** true if a Priapus Totem is in the hotbar (slots 0-8) or offhand; several totems never stack */
    public static boolean carriesPriapus(EntityPlayer p) {
        for (int i = 0; i < 9; i++) if (p.inventory.getStackInSlot(i).getItem() == ModContent.PRIAPUS) return true;
        for (ItemStack s : p.inventory.offHandInventory) if (s.getItem() == ModContent.PRIAPUS) return true;
        return false;
    }

    /** applied every second (server): Speed I (refreshed, hidden particles) + extra natural healing */
    public static void applyPriapus(EntityPlayer p, long tick) {
        if (!carriesPriapus(p)) return;
        PotionEffect cur = p.getActivePotionEffect(MobEffects.SPEED);
        if (cur == null || (cur.getAmplifier() == 0 && cur.getDuration() < 60))
            p.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 0, true, false));
        int every = Math.max(5, LTConfig.priapusHealInterval);
        if (tick % every == 0 && p.shouldHeal() && p.isEntityAlive()) p.heal(1.0f);
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.world.isRemote) return;
        long t = e.player.world.getTotalWorldTime();
        int every = Math.max(5, LTConfig.priapusHealInterval);
        if (t % 20 == 0 || t % every == 0) applyPriapus(e.player, t);
    }

    /** PURPLE: 20% less damage (Resistance I equivalent) */
    @SubscribeEvent
    public void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if (ModContent.PURPLE != null && e.getEntityLiving().isPotionActive(ModContent.PURPLE) && !e.getSource().canHarmInCreative())
            e.setAmount(e.getAmount() * 0.8f);
    }

    /** PURPLE also wards off poison/wither entirely while active */
    @SubscribeEvent
    public void potionApplicable(net.minecraftforge.event.entity.living.PotionEvent.PotionApplicableEvent e) {
        net.minecraft.potion.Potion p = e.getPotionEffect().getPotion();
        if ((p == MobEffects.POISON || p == MobEffects.WITHER) && ModContent.PURPLE != null && e.getEntityLiving().isPotionActive(ModContent.PURPLE))
            e.setResult(Event.Result.DENY);
    }

    /** Vanilla squid only spawn above y=45; let them live in the (lower) Lightfall sea too. */
    @SubscribeEvent
    public void checkSpawn(LivingSpawnEvent.CheckSpawn e) {
        if (e.getEntityLiving() instanceof EntitySquid && !e.isSpawner() && e.getWorld().provider.getDimension() == LuciferTotem.dimId
            && e.getWorld().getBlockState(new BlockPos(e.getX(), e.getY(), e.getZ())).getMaterial() == Material.WATER)
            e.setResult(Event.Result.ALLOW);
    }
}
