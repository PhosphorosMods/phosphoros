package lucifertotem.hillman;

import lucifertotem.EntityLandMedusa;
import lucifertotem.EntityMedusa;
import lucifertotem.LTConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;

/** Venom milking + Mithridatism (King Mithridates VI dosed himself daily until no poison could touch him). */
public class Venom {
    public static final int COOLDOWN = 6000;
    public static final DamageSource VENOM = new DamageSource("venom") {{ setDamageBypassesArmor(); }};

    public static boolean milkable(Entity e) {
        return e instanceof EntityCaveSpider || e instanceof EntityMedusa || e instanceof EntityLandMedusa || e instanceof EntityDipsas;
    }

    /** glass bottle on a venomous creature. returns true if handled */
    public static boolean milk(EntityPlayer p, EnumHand hand, Entity target) {
        ItemStack held = p.getHeldItem(hand);
        if (held.getItem() != Items.GLASS_BOTTLE || !milkable(target)) return false;
        if (p.world.isRemote) return true;
        long now = p.world.getTotalWorldTime();
        long last = target.getEntityData().getLong("lt_milked");
        if (target.getEntityData().hasKey("lt_milked") && now - last < COOLDOWN) { HBlocks.msg(p, "msg.lucifertotem.venom.dry"); return true; }
        target.getEntityData().setLong("lt_milked", now);
        if (!p.capabilities.isCreativeMode) held.shrink(1);
        ItemStack vial = new ItemStack(HItems.VENOM_VIAL);
        if (!p.inventory.addItemStackToInventory(vial)) p.dropItem(vial, false);
        p.world.playSound(null, target.posX, target.posY, target.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 0.8f);
        if (p.getRNG().nextFloat() < 0.2f && !p.capabilities.isCreativeMode) {
            p.addPotionEffect(new PotionEffect(MobEffects.POISON, 100, 0));
            HBlocks.msg(p, "msg.lucifertotem.venom.bitten");
        } else HBlocks.msg(p, "msg.lucifertotem.venom.milked");
        return true;
    }

    /** a small dose: half a heart of damage and +1 tolerance, at most once per Minecraft day */
    public static void drinkDiluted(EntityPlayer p) {
        p.attackEntityFrom(VENOM, 1.0f);
        long day = p.world.getTotalWorldTime() / 24000L;
        if (HData.get(p).hasKey("lastDose") && HData.get(p).getLong("lastDose") == day) {
            HBlocks.msg(p, "msg.lucifertotem.venom.too_soon", HData.venomTolerance(p));
            return;
        }
        HData.get(p).setLong("lastDose", day);
        addTolerance(p, 1);
    }

    public static void drinkMithridatium(EntityPlayer p) {
        p.removePotionEffect(MobEffects.POISON);
        addTolerance(p, 5);
    }

    public static void addTolerance(EntityPlayer p, int n) {
        int before = HData.venomTolerance(p);
        HData.setVenomTolerance(p, before + n);
        int t = HData.venomTolerance(p);
        HBlocks.msg(p, "msg.lucifertotem.venom.tolerance", t);
        if (t >= 100 && before < 100) { HAdv.grant(p, "poison_king"); HBlocks.msg(p, "msg.lucifertotem.venom.king"); }
    }

    /** damage/duration multiplier from tolerance: 10% less per 10 points, immune at 100 */
    public static double factor(int tol) {
        if (tol >= 100) return 0;
        return 1.0 - Math.floor(tol / 10.0) * 0.1;
    }

    /** guard so our re-added (scaled) effect is not processed again */
    static boolean reapplying = false;

    /** returns true if the incoming effect should be denied (it is replaced by a scaled one, or immune) */
    public static boolean filter(EntityLivingBase e, PotionEffect pe) {
        if (reapplying || !(e instanceof EntityPlayer) || e.world.isRemote) return false;
        Potion pot = pe.getPotion();
        if (pot != MobEffects.POISON && pot != MobEffects.WITHER) return false;
        int tol = HData.venomTolerance((EntityPlayer) e);
        if (tol < 10) return false;
        double f = factor(tol);
        if (f <= 0) return true;
        int dur = (int) Math.max(1, pe.getDuration() * f);
        reapplying = true;
        try { e.addPotionEffect(new PotionEffect(pot, dur, pe.getAmplifier(), pe.getIsAmbient(), pe.doesShowParticles())); }
        finally { reapplying = false; }
        return true;
    }
}
