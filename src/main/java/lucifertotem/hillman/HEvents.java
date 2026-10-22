package lucifertotem.hillman;

import lucifertotem.EntityLandMedusa;
import lucifertotem.EntityMedusa;
import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side (common) game logic of 1.1.0. Registered on the Forge bus in LuciferTotem.init. */
public class HEvents {
    public static int randomShouts = 0, medusaShouts = 0, insaniaCures = 0, revivals = 0, coatedHits = 0, gloomyLines = 0, foodDenied = 0, lightsBlocked = 0;

    // ------------------------------------------------------------ Theriac
    /** called by ItemTheriac / Great Theriac after drinking (server) */
    public static void onTheriac(EntityPlayer p) {
        EntityAmmonius a = EntityAmmonius.nearest(p.world, p, 16);
        if (a != null) {
            Shout.shoutAt(p.world, a, "ammonius", HSounds.AMMONIUS_PURPLE, true);
            Shout.burst(p.world, p.posX, p.posY + 1, p.posZ);
            HAdv.grant(p, "something_in_the_drink");
        } else Shout.shout(p, "theriac");
    }

    public static void drinkGreatTheriac(EntityPlayer p) {
        for (PotionEffect pe : new java.util.ArrayList<>(p.getActivePotionEffects()))
            if (pe.getPotion().isBadEffect()) p.removePotionEffect(pe.getPotion());
        HData.get(p).setLong("lastMeal", p.world.getTotalWorldTime());
        p.addPotionEffect(new PotionEffect(ModContent.PURPLE, 2400, 1));
        onTheriac(p);
    }

    // ------------------------------------------------------------ interaction: venom milking
    @SubscribeEvent
    public void interact(PlayerInteractEvent.EntityInteract e) {
        if (Venom.milk(e.getEntityPlayer(), e.getHand(), e.getTarget())) { e.setCanceled(true); e.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS); }
    }

    // ------------------------------------------------------------ ticks
    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.world.isRemote) return;
        EntityPlayer p = e.player;
        long t = p.world.getTotalWorldTime();
        // PURPLE cures henbane madness
        if (p.isPotionActive(HPotions.INSANIA) && p.isPotionActive(ModContent.PURPLE)) {
            p.removePotionEffect(HPotions.INSANIA);
            insaniaCures++;
            Shout.shout(p, "insania_cure");
        }
        // random PURPLE! in the Lightfall
        if (t % 1200 == (p.getEntityId() & 1023) % 1200 && p.world.provider.getDimension() == LuciferTotem.dimId
            && LTConfig.purpleRandomOneIn > 0 && p.getRNG().nextInt(LTConfig.purpleRandomOneIn) == 0) { randomShouts++; Shout.shout(p, "random"); }
        if (t % 10 == 0) mandrakeDogs(p);
        if (Trophonius.inPit(p)) pitTick(p, t);
        if (t % 600 == 0 && p.isPotionActive(HPotions.ORACLE)) Trophonius.oracleTick(p);
    }

    public static void mandrakeDogs(EntityPlayer p) {
        for (EntityWolf d : p.world.getEntitiesWithinAABB(EntityWolf.class, p.getEntityBoundingBox().grow(16))) {
            NBTTagCompound tag = d.getEntityData();
            if (!tag.hasKey("lt_mandrake")) continue;
            BlockPos m = BlockPos.fromLong(tag.getLong("lt_mandrake"));
            if (p.world.getBlockState(m).getBlock() != HBlocks.MANDRAKE || !d.getLeashed()) { tag.removeTag("lt_mandrake"); continue; }
            if (d.getDistanceSq(m) > 25) { tag.removeTag("lt_mandrake"); HBlocks.BlockMandrake.dogPull(p.world, m, d); }
        }
    }

    static void pitTick(EntityPlayer p, long t) {
        NBTTagCompound d = HData.get(p);
        BlockPos altar = BlockPos.fromLong(d.getLong("trophAltar"));
        // left the pit some other way (death, teleport, dimension)? forget it
        if (p.posY > Trophonius.MAZE_FLOOR + 8 || Math.abs(p.posX - altar.getX()) > 12 || Math.abs(p.posZ - altar.getZ()) > 12) { d.setBoolean("trophIn", false); return; }
        if (t % 40 == 0) p.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, false));
        if (t % 160 == 0 && p.getRNG().nextBoolean())
            p.world.playSound(null, p.posX + p.getRNG().nextGaussian() * 3, p.posY, p.posZ + p.getRNG().nextGaussian() * 3, HSounds.TROPHONIUS_WHISPER, SoundCategory.AMBIENT, 0.6f, 0.7f + p.getRNG().nextFloat() * 0.4f);
    }

    // ------------------------------------------------------------ Dipsas thirst + meals
    private static final Map<UUID, NBTTagCompound> FOOD_SNAPSHOT = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void useStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntityLiving() instanceof EntityPlayer) || e.getEntityLiving().world.isRemote) return;
        EntityPlayer p = (EntityPlayer) e.getEntityLiving();
        if (e.getItem().getItem() instanceof ItemFood && p.isPotionActive(HPotions.THIRST)) {
            NBTTagCompound snap = new NBTTagCompound();
            p.getFoodStats().writeNBT(snap);
            FOOD_SNAPSHOT.put(p.getUniqueID(), snap);
        }
    }

    @SubscribeEvent
    public void useFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntityLiving() instanceof EntityPlayer) || e.getEntityLiving().world.isRemote) return;
        EntityPlayer p = (EntityPlayer) e.getEntityLiving();
        ItemStack s = e.getItem();
        potionHeard(p, s);
        finishUse(p, s);
    }

    /** Doctor Ammonius blurts a PURPLE line whenever a player within 16 blocks drinks a potion */
    public static boolean potionHeard(EntityPlayer p, ItemStack s) {
        if (!(s.getItem() instanceof net.minecraft.item.ItemPotion) || PotionUtils.getPotionFromItem(s) == PotionTypes.WATER) return false;
        EntityAmmonius a = EntityAmmonius.nearest(p.world, p, 16);
        if (a == null) return false;
        a.blurtPurple();
        return true;
    }

    /** split out for the test harness */
    public static void finishUse(EntityPlayer p, ItemStack s) {
        PotionEffect thirst = p.getActivePotionEffect(HPotions.THIRST);
        if (s.getItem() instanceof ItemFood) {
            NBTTagCompound snap = FOOD_SNAPSHOT.remove(p.getUniqueID());
            if (thirst != null && snap != null) { p.getFoodStats().readNBT(snap); foodDenied++; HBlocks.msg(p, "msg.lucifertotem.dipsas.food"); }
            else HData.get(p).setLong("lastMeal", p.world.getTotalWorldTime());
            return;
        }
        if (thirst == null) return;
        if (s.getItem() == Items.MILK_BUCKET) {
            if (thirst.getDuration() > 600) setThirst(p, 600, thirst.getAmplifier());
        } else if (s.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(s) == PotionTypes.WATER) {
            setThirst(p, thirst.getDuration() / 2, thirst.getAmplifier());
            HBlocks.msg(p, "msg.lucifertotem.dipsas.water");
        }
    }

    static void setThirst(EntityPlayer p, int dur, int amp) {
        p.removePotionEffect(HPotions.THIRST);
        if (dur > 0) p.addPotionEffect(new PotionEffect(HPotions.THIRST, dur, amp));
    }

    // ------------------------------------------------------------ Mithridatism
    @SubscribeEvent(priority = EventPriority.LOW)
    public void potionApplicable(PotionEvent.PotionApplicableEvent e) {
        if (e.getResult() == Event.Result.DENY) return;
        if (Venom.filter(e.getEntityLiving(), e.getPotionEffect())) e.setResult(Event.Result.DENY);
    }

    // ------------------------------------------------------------ death: shouts, statues, the right vein
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void death(LivingDeathEvent e) {
        EntityLivingBase dead = e.getEntityLiving();
        if (dead.world.isRemote) return;
        if (dead instanceof EntityPlayer && reviveWithBlood((EntityPlayer) dead)) { e.setCanceled(true); return; }
        Entity killer = e.getSource().getTrueSource();
        if (killer instanceof EntityPlayer && (dead instanceof EntityMedusa || dead instanceof EntityLandMedusa)
            && LTConfig.purpleOnMedusaKillOneIn > 0 && dead.getRNG().nextInt(LTConfig.purpleOnMedusaKillOneIn) == 0) {
            medusaShouts++;
            Shout.shout((EntityPlayer) killer, "medusa");
        }
        if (killer instanceof EntityGorgon && !(dead instanceof EntityPlayer)) {
            BlockPos at = new BlockPos(dead);
            if (dead.world.isAirBlock(at)) dead.world.setBlockState(at, HBlocks.STATUE.getDefaultState(), 3);
        }
    }

    public static boolean reviveWithBlood(EntityPlayer p) {
        for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s.getItem() == HItems.BLOOD_RIGHT) {
                s.shrink(1);
                p.setHealth(p.getMaxHealth() * 0.5f);
                p.clearActivePotions();
                p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 600, 1));
                p.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 200, 1));
                p.extinguish();
                revivals++;
                p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1f, 0.7f);
                Shout.burst(p.world, p.posX, p.posY + 1, p.posZ);
                HBlocks.msg(p, "msg.lucifertotem.blood.revived");
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ coated weapon (left vein)
    @SubscribeEvent
    public void hurt(LivingHurtEvent e) {
        Entity src = e.getSource().getImmediateSource();
        if (!(src instanceof EntityPlayer) || src.world.isRemote) return;
        ItemStack w = ((EntityPlayer) src).getHeldItemMainhand();
        if (w.hasTagCompound() && w.getTagCompound().getInteger("lt_gorgon_coat") > 0) {
            int n = w.getTagCompound().getInteger("lt_gorgon_coat") - 1;
            if (n <= 0) w.getTagCompound().removeTag("lt_gorgon_coat"); else w.getTagCompound().setInteger("lt_gorgon_coat", n);
            e.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.WITHER, 100, 2));
            coatedHits++;
        }
    }

    // ------------------------------------------------------------ petrified: no jumping
    @SubscribeEvent
    public void jump(LivingEvent.LivingJumpEvent e) {
        if (HPotions.PETRIFIED != null && e.getEntityLiving().isPotionActive(HPotions.PETRIFIED)) e.getEntityLiving().motionY = 0;
    }

    // ------------------------------------------------------------ Trophonian gloom: chat suffix
    @SubscribeEvent
    public void chat(ServerChatEvent e) {
        if (e.getPlayer() != null && HData.gloomy(e.getPlayer())) {
            gloomyLines++;
            e.setComponent(gloomify(e.getComponent()));
        }
    }

    public static net.minecraft.util.text.ITextComponent gloomify(net.minecraft.util.text.ITextComponent c) {
        TextComponentString suffix = new TextComponentString(" *(gloomily)*");
        suffix.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        return c.createCopy().appendSibling(suffix);
    }

    // ------------------------------------------------------------ the pit stays dark (and whole)
    @SubscribeEvent
    public void place(BlockEvent.PlaceEvent e) {
        EntityPlayer p = e.getPlayer();
        if (p == null || p.world.isRemote || !Trophonius.inPit(p)) return;
        IBlockState s = e.getPlacedBlock();
        if (s.getLightValue(e.getWorld(), e.getPos()) > 0) {
            e.setCanceled(true);
            lightsBlocked++;
            HBlocks.msg(p, "msg.lucifertotem.trophonius.no_light");
        }
    }

    @SubscribeEvent
    public void breakBlock(BlockEvent.BreakEvent e) {
        EntityPlayer p = e.getPlayer();
        if (p != null && !p.world.isRemote && Trophonius.inPit(p) && !p.capabilities.isCreativeMode) e.setCanceled(true);
    }
}
