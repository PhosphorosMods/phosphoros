package lucifertotem.hillman;

import io.netty.buffer.ByteBuf;
import lucifertotem.ItemTheriac;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTippedArrow;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * The Alabastron: a little ceramic flask, laced with any potion. Bonk someone with it and nothing happens;
 * sneak up and deliver it FROM BEHIND and they get a boosted dose (amplifier +1, duration x1.5) and the flask
 * stays comically lodged on their lower back until someone sneak-right-clicks them, they pull it out themselves
 * (sneak + right-click air with an empty hand) or they die. Slapstick only.
 */
public class Alabastron {
    public static final String TAG_EFFECTS = "lt_effects";
    public static final String TAG_LODGED = "lt_alabastron";
    /** the attacker must stand within this many degrees of the target's back */
    public static final double BEHIND_DEGREES = 60.0;
    public static SimpleNetworkWrapper NET;
    public static int doses = 0, bonks = 0, retrieved = 0, dropped = 0, selfDoses = 0;

    // ------------------------------------------------------------ network
    public static void network() {
        NET = NetworkRegistry.INSTANCE.newSimpleChannel("lt_alabastron");
        NET.registerMessage(StateHandler.class, MsgState.class, 0, Side.CLIENT);
        NET.registerMessage(PullHandler.class, MsgPull.class, 1, Side.SERVER);
    }

    public static class MsgState implements IMessage {
        public int id; public boolean lodged;
        public MsgState() { }
        public MsgState(int id, boolean lodged) { this.id = id; this.lodged = lodged; }
        @Override public void fromBytes(ByteBuf b) { id = b.readInt(); lodged = b.readBoolean(); }
        @Override public void toBytes(ByteBuf b) { b.writeInt(id); b.writeBoolean(lodged); }
    }
    public static class StateHandler implements IMessageHandler<MsgState, IMessage> {
        @Override public IMessage onMessage(MsgState m, MessageContext ctx) { LuciferTotem.proxy.alabastronState(m.id, m.lodged); return null; }
    }
    public static class MsgPull implements IMessage {
        @Override public void fromBytes(ByteBuf b) { }
        @Override public void toBytes(ByteBuf b) { }
    }
    public static class PullHandler implements IMessageHandler<MsgPull, IMessage> {
        @Override public IMessage onMessage(MsgPull m, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            ((WorldServer) p.world).addScheduledTask(() -> pullOwn(p));
            return null;
        }
    }

    static boolean realClient(EntityPlayerMP p) { return !(p instanceof FakePlayer) && p.connection != null && p.connection.getNetworkManager() != null && p.connection.getNetworkManager().channel() != null; }

    public static void sync(Entity e) {
        if (NET == null || e.world.isRemote) return;
        MsgState m = new MsgState(e.getEntityId(), isLodged(e));
        try { NET.sendToAllTracking(m, e); } catch (Throwable t) { LuciferTotem.LOG.debug("alabastron sync", t); }
        if (e instanceof EntityPlayerMP && realClient((EntityPlayerMP) e)) try { NET.sendTo(m, (EntityPlayerMP) e); } catch (Throwable t) { LuciferTotem.LOG.debug("alabastron sync", t); }
    }
    static void syncTo(Entity e, EntityPlayer to) {
        if (NET == null || !(to instanceof EntityPlayerMP) || !realClient((EntityPlayerMP) to) || !isLodged(e)) return;
        try { NET.sendTo(new MsgState(e.getEntityId(), true), (EntityPlayerMP) to); } catch (Throwable t) { LuciferTotem.LOG.debug("alabastron sync", t); }
    }

    // ------------------------------------------------------------ effects in NBT
    public static List<PotionEffect> effects(ItemStack s) {
        List<PotionEffect> out = new ArrayList<>();
        if (s.hasTagCompound() && s.getTagCompound().hasKey(TAG_EFFECTS)) {
            NBTTagList l = s.getTagCompound().getTagList(TAG_EFFECTS, 10);
            for (int i = 0; i < l.tagCount(); i++) { PotionEffect pe = PotionEffect.readCustomPotionEffectFromNBT(l.getCompoundTagAt(i)); if (pe != null) out.add(pe); }
        }
        return out;
    }
    public static ItemStack withEffects(ItemStack s, List<PotionEffect> fx) {
        NBTTagCompound t = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
        if (fx == null || fx.isEmpty()) t.removeTag(TAG_EFFECTS);
        else {
            NBTTagList l = new NBTTagList();
            for (PotionEffect pe : fx) l.appendTag(pe.writeCustomPotionEffectToNBT(new NBTTagCompound()));
            t.setTag(TAG_EFFECTS, l);
        }
        s.setTagCompound(t.isEmpty() ? null : t);
        return s;
    }
    public static boolean laced(ItemStack s) { return s.getItem() == HItems.ALABASTRON && !effects(s).isEmpty(); }

    /** effects of anything potion-like: vanilla/modded potions (drink, splash, lingering), Theriac, Great Theriac */
    public static List<PotionEffect> potionEffects(ItemStack s) {
        List<PotionEffect> fx = new ArrayList<>();
        if (s.isEmpty() || s.getItem() == HItems.ALABASTRON || s.getItem() instanceof ItemTippedArrow) return fx;
        if (s.getItem() == ModContent.THERIAC) { fx.add(new PotionEffect(ModContent.PURPLE, ItemTheriac.DURATION, 0)); return fx; }
        if (s.getItem() == HItems.GREAT_THERIAC) { fx.add(new PotionEffect(ModContent.PURPLE, 2400, 1)); return fx; }
        for (PotionEffect pe : PotionUtils.getEffectsFromStack(s)) fx.add(new PotionEffect(pe));
        return fx;
    }
    /** drinkable potions leave a glass bottle behind; splash/lingering don't */
    public static boolean leavesBottle(ItemStack s) {
        Item i = s.getItem();
        return i == ModContent.THERIAC || i == HItems.GREAT_THERIAC || (i instanceof ItemPotion && !(i instanceof ItemSplashPotion) && !(i instanceof ItemLingeringPotion));
    }

    // ------------------------------------------------------------ the dose
    public static boolean fromBehind(Entity attacker, EntityLivingBase target) {
        double yaw = Math.toRadians(target instanceof EntityPlayer ? target.rotationYaw : target.renderYawOffset);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);                // where the target faces
        double dx = attacker.posX - target.posX, dz = attacker.posZ - target.posZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4) return false;
        double dot = -(fx * dx + fz * dz) / len;                        // cosine to the target's back
        return dot >= Math.cos(Math.toRadians(BEHIND_DEGREES));
    }

    public static PotionEffect boosted(PotionEffect pe) {
        int amp = Math.min(127, pe.getAmplifier() + 1);
        int dur = pe.getPotion().isInstant() ? pe.getDuration() : (int) Math.min(Integer.MAX_VALUE / 2, Math.round(pe.getDuration() * 1.5));
        return new PotionEffect(pe.getPotion(), dur, amp, pe.getIsAmbient(), pe.doesShowParticles());
    }

    public static void dose(EntityLivingBase from, EntityLivingBase target, List<PotionEffect> fx) {
        for (PotionEffect pe : fx) {
            PotionEffect b = boosted(pe);
            Potion p = b.getPotion();
            if (p.isInstant()) p.affectEntity(from, from, target, b.getAmplifier(), 1.0);
            else target.addPotionEffect(b);
        }
    }

    public static boolean isLodged(Entity e) { return e.getEntityData().hasKey(TAG_LODGED); }
    public static ItemStack lodgedStack(Entity e) {
        if (!isLodged(e)) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(e.getEntityData().getCompoundTag(TAG_LODGED));
        return s.isEmpty() ? new ItemStack(HItems.ALABASTRON) : s;
    }
    public static void lodge(Entity e, ItemStack s) { e.getEntityData().setTag(TAG_LODGED, s.writeToNBT(new NBTTagCompound())); sync(e); }
    public static ItemStack unlodge(Entity e) { ItemStack s = lodgedStack(e); e.getEntityData().removeTag(TAG_LODGED); sync(e); return s; }

    static boolean pvpAllowed(EntityPlayer attacker, EntityLivingBase target) {
        if (!(target instanceof EntityPlayer)) return true;
        if (target == attacker) return false;
        MinecraftServer srv = attacker.getServer();
        return srv != null && srv.isPVPEnabled() && attacker.canAttackPlayer((EntityPlayer) target);
    }

    static void bar(EntityPlayer p, String key) { p.sendStatusMessage(new TextComponentTranslation(key), true); }

    /** returns: 0 not an alabastron / not handled, 1 bonk, 2 dosed, 3 empty, 4 occupied, 5 pvp off */
    public static int strike(EntityPlayer p, Entity t) {
        ItemStack s = p.getHeldItemMainhand();
        if (p.world.isRemote || s.getItem() != HItems.ALABASTRON || !(t instanceof EntityLivingBase) || !t.isEntityAlive()) return 0;
        EntityLivingBase target = (EntityLivingBase) t;
        if (!pvpAllowed(p, target)) return 5;
        if (!fromBehind(p, target)) {
            bonks++;
            p.world.playSound(null, target.posX, target.posY + target.height * 0.6, target.posZ, HSounds.ALABASTRON_BONK, SoundCategory.PLAYERS, 1f, 0.9f + p.getRNG().nextFloat() * 0.3f);
            bar(p, "msg.lucifertotem.alabastron.bonk");
            return 1;
        }
        List<PotionEffect> fx = effects(s);
        if (fx.isEmpty()) { bar(p, "msg.lucifertotem.alabastron.empty"); return 3; }
        if (AmmonConversion.eligible(target)) {   // 1.2.1: a librarian, from behind: he remembers who he is
            ItemStack empty = withEffects(s.copy(), null); empty.setCount(1);
            s.shrink(1);
            if (s.isEmpty()) p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            doses++;
            AmmonConversion.convert(p, (net.minecraft.entity.passive.EntityVillager) target, empty);
            bar(p, "msg.lucifertotem.alabastron.converted");
            return 6;
        }
        if (isLodged(target)) { bar(p, "msg.lucifertotem.alabastron.occupied"); return 4; }
        dose(p, target, fx);
        ItemStack empty = withEffects(s.copy(), null); empty.setCount(1);
        s.shrink(1);
        if (s.isEmpty()) p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        lodge(target, empty);
        doses++;
        // a squidgy gulp: low-pitched eating layered with a small slime squish, at the same moment
        float jitter = (p.getRNG().nextFloat() - 0.5f) * 0.12f;
        p.world.playSound(null, target.posX, target.posY + target.height * 0.5, target.posZ, HSounds.ALABASTRON_DOSE, SoundCategory.PLAYERS, 1.0f, 1.0f + jitter);
        p.world.playSound(null, target.posX, target.posY + target.height * 0.5, target.posZ, HSounds.SQUISH_SMALL, SoundCategory.PLAYERS, 0.9f, 1.3f + jitter);
        bar(p, "msg.lucifertotem.alabastron.dosed");
        if (target instanceof EntityPlayer) bar((EntityPlayer) target, "msg.lucifertotem.alabastron.received");
        return 2;
    }

    /** sneak + use a laced alabastron (nothing potion-like in the off hand): returns 2 dosed, 3 empty, 4 occupied, 0 not applicable */
    public static int selfDose(EntityPlayer p) {
        ItemStack s = p.getHeldItemMainhand();
        if (p.world.isRemote || s.getItem() != HItems.ALABASTRON || !p.isSneaking() || !potionEffects(p.getHeldItemOffhand()).isEmpty()) return 0;
        List<PotionEffect> fx = effects(s);
        if (fx.isEmpty()) { bar(p, "msg.lucifertotem.alabastron.empty"); return 3; }
        if (isLodged(p)) { bar(p, "msg.lucifertotem.alabastron.occupied"); return 4; }
        dose(p, p, fx);
        ItemStack empty = withEffects(s.copy(), null); empty.setCount(1);
        s.shrink(1);
        if (s.isEmpty()) p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        lodge(p, empty);
        doses++; selfDoses++;
        float jitter = (p.getRNG().nextFloat() - 0.5f) * 0.12f;
        p.world.playSound(null, p.posX, p.posY + p.height * 0.5, p.posZ, HSounds.ALABASTRON_DOSE, SoundCategory.PLAYERS, 1.0f, 1.0f + jitter);
        p.world.playSound(null, p.posX, p.posY + p.height * 0.5, p.posZ, HSounds.SQUISH_SMALL, SoundCategory.PLAYERS, 0.9f, 1.3f + jitter);
        bar(p, "msg.lucifertotem.alabastron.self");
        return 2;
    }

    public static boolean retrieve(EntityPlayer by, Entity from) {
        if (by.world.isRemote || !isLodged(from)) return false;
        ItemStack s = unlodge(from);
        if (!by.inventory.addItemStackToInventory(s)) by.dropItem(s, false);
        retrieved++;
        by.world.playSound(null, from.posX, from.posY + from.height * 0.5, from.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.8f, 0.6f);
        bar(by, "msg.lucifertotem.alabastron.retrieved");
        return true;
    }

    /** sneak + right-click air with an empty main hand pulls your own out (sent by the client) */
    public static boolean pullOwn(EntityPlayer p) {
        if (!p.isSneaking() || !p.getHeldItemMainhand().isEmpty()) return false;
        return retrieve(p, p);
    }

    // ------------------------------------------------------------ events
    public static class Events {
        @SubscribeEvent(priority = EventPriority.LOW)
        public void attack(AttackEntityEvent e) {
            int r = strike(e.getEntityPlayer(), e.getTarget());
            if (r == 6) { e.setCanceled(true); return; }   // the villager is gone: no swing damage on him
            if (r == 5 || r == 3 || r == 4) return; // the vanilla swing proceeds (and PvP rules still decide damage)
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public void interact(PlayerInteractEvent.EntityInteract e) {
            EntityPlayer p = e.getEntityPlayer();
            if (e.getHand() != EnumHand.MAIN_HAND || !p.isSneaking() || !isLodged(e.getTarget()) || e.getTarget() == p) return;
            if (!e.getWorld().isRemote) retrieve(p, e.getTarget());
            e.setCanceled(true);
            e.setCancellationResult(EnumActionResult.SUCCESS);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void death(LivingDeathEvent e) {
            EntityLivingBase d = e.getEntityLiving();
            if (d.world.isRemote || !isLodged(d)) return;
            ItemStack s = unlodge(d);
            d.entityDropItem(s, 0.5f);
            dropped++;
        }

        @SubscribeEvent
        public void track(PlayerEvent.StartTracking e) { syncTo(e.getTarget(), e.getEntityPlayer()); }
        @SubscribeEvent
        public void login(PlayerLoggedInEvent e) { syncTo(e.player, e.player); }
        @SubscribeEvent
        public void respawn(PlayerRespawnEvent e) { syncTo(e.player, e.player); }
        @SubscribeEvent
        public void dim(PlayerChangedDimensionEvent e) { syncTo(e.player, e.player); }
    }

    // ------------------------------------------------------------ the item
    public static class ItemAlabastron extends Item {
        public ItemAlabastron() { setMaxStackSize(1); }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
            ItemStack s = p.getHeldItem(hand);
            if (hand != EnumHand.MAIN_HAND) return new ActionResult<>(EnumActionResult.PASS, s);
            ItemStack off = p.getHeldItemOffhand();
            List<PotionEffect> fx = potionEffects(off);
            if (laced(s)) {
                // self-dose: sneaking, laced flask in the main hand, no potion in the off hand (that would be lacing)
                if (!p.isSneaking() || !fx.isEmpty()) return new ActionResult<>(EnumActionResult.PASS, s);
                if (!w.isRemote) selfDose(p);
                return new ActionResult<>(EnumActionResult.SUCCESS, p.getHeldItem(hand));
            }
            if (fx.isEmpty()) return new ActionResult<>(EnumActionResult.PASS, s);
            if (!w.isRemote) lace(p, s, off, fx);
            return new ActionResult<>(EnumActionResult.SUCCESS, s);
        }

        public static void lace(EntityPlayer p, ItemStack s, ItemStack off, List<PotionEffect> fx) {
            withEffects(s, fx);
            boolean bottle = leavesBottle(off);
            if (!p.capabilities.isCreativeMode) {
                off.shrink(1);
                if (bottle) {
                    if (off.isEmpty()) p.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
                    else if (!p.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE))) p.dropItem(new ItemStack(Items.GLASS_BOTTLE), false);
                }
            }
            p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 1.2f);
            bar(p, "msg.lucifertotem.alabastron.laced");
        }

        @Override @SideOnly(Side.CLIENT) public boolean hasEffect(ItemStack s) { return laced(s); }

        @Override
        public String getTranslationKey(ItemStack s) { return laced(s) ? super.getTranslationKey(s) + ".laced" : super.getTranslationKey(s); }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
            List<PotionEffect> fx = effects(s);
            if (fx.isEmpty()) tip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.lucifertotem.alabastron.empty"));
            else {
                tip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("tooltip.lucifertotem.alabastron.contains"));
                for (PotionEffect pe : fx) {
                    String n = I18n.translateToLocal(pe.getEffectName()).trim();
                    if (pe.getAmplifier() > 0) n += " " + I18n.translateToLocal("potion.potency." + pe.getAmplifier()).trim();
                    if (!pe.getPotion().isInstant()) n += " (" + Potion.getPotionDurationString(pe, 1f) + ")";
                    tip.add((pe.getPotion().isBadEffect() ? TextFormatting.RED : TextFormatting.BLUE) + " " + n);
                }
            }
            tip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("tooltip.lucifertotem.alabastron.1"));
            tip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("tooltip.lucifertotem.alabastron.self"));
            tip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC + I18n.translateToLocal("tooltip.lucifertotem.alabastron.lore"));
        }
    }

    /** crafting: an empty alabastron + any potion = a laced alabastron (the glass bottle comes back for drinkables) */
    public static class RecipeLace extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override public boolean matches(InventoryCrafting inv, World w) { return !getCraftingResult(inv).isEmpty(); }
        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack flask = ItemStack.EMPTY, potion = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.isEmpty()) continue;
                if (s.getItem() == HItems.ALABASTRON) { if (!flask.isEmpty() || laced(s)) return ItemStack.EMPTY; flask = s; }
                else if (!potionEffects(s).isEmpty()) { if (!potion.isEmpty()) return ItemStack.EMPTY; potion = s; }
                else return ItemStack.EMPTY;
            }
            if (flask.isEmpty() || potion.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = flask.copy(); out.setCount(1);
            return withEffects(out, potionEffects(potion));
        }
        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            NonNullList<ItemStack> r = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
            for (int i = 0; i < r.size(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem() != HItems.ALABASTRON && leavesBottle(s)) r.set(i, new ItemStack(Items.GLASS_BOTTLE));
            }
            return r;
        }
        @Override public boolean canFit(int w, int h) { return w * h >= 2; }
        @Override public ItemStack getRecipeOutput() { return new ItemStack(HItems.ALABASTRON); }
        @Override public boolean isDynamic() { return true; }
    }
}
