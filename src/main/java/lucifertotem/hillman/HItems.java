package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** All 1.1.0 items. */
public class HItems {
    public static Item VENOM_VIAL, STAR_FOAM, PYTHIAN_LAUREL, SIBYL_LEAF;
    public static ItemDrink DILUTED_VENOM, MITHRIDATIUM, AGED_VINEGAR, GREAT_THERIAC;
    public static ItemYoungVinegar YOUNG_VINEGAR;
    public static ItemMandrakeRoot MANDRAKE_ROOT;
    public static ItemFood HENBANE_LEAVES, HONEY_CAKE;
    public static ItemSibylline SIBYLLINE;
    public static ItemMedeaFlask MEDEA_FLASK;
    public static ItemBronzeShield BRONZE_SHIELD;
    public static ItemGorgonBlood BLOOD_LEFT, BLOOD_RIGHT;
    public static Alabastron.ItemAlabastron ALABASTRON;
    public static lucifertotem.paint.ItemLadyBabylonPainting PAINTING;

    static <T extends Item> T named(T i, String name) {
        i.setRegistryName(LuciferTotem.MODID, name);
        i.setTranslationKey(LuciferTotem.MODID + "." + name);
        i.setCreativeTab(LuciferTotem.TAB);
        return i;
    }

    /** generic tooltip: lines tooltip.lucifertotem.<name>.1..n that exist */
    @SideOnly(Side.CLIENT)
    public static void tips(Item it, List<String> tip) {
        String base = "tooltip." + it.getRegistryName().getPath() + ".";
        base = "tooltip.lucifertotem." + it.getRegistryName().getPath() + ".";
        for (int i = 1; i <= 4; i++) {
            if (!I18n.canTranslate(base + i)) break;
            tip.add((i == 1 ? TextFormatting.GRAY : TextFormatting.DARK_GRAY) + I18n.translateToLocal(base + i));
        }
    }

    public static class ItemTip extends Item {
        boolean glint;
        public ItemTip(boolean glint) { this.glint = glint; }
        @Override @SideOnly(Side.CLIENT) public boolean hasEffect(ItemStack s) { return glint; }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { tips(this, tip); }
    }

    /** a drinkable bottle; effect runs server side; gives the glass bottle back */
    public static class ItemDrink extends ItemTip {
        final Consumer<EntityPlayer> effect;
        public ItemDrink(boolean glint, Consumer<EntityPlayer> effect) { super(glint); this.effect = effect; setMaxStackSize(16); }
        @Override public EnumAction getItemUseAction(ItemStack s) { return EnumAction.DRINK; }
        @Override public int getMaxItemUseDuration(ItemStack s) { return 32; }
        @Override public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) { p.setActiveHand(h); return new ActionResult<>(EnumActionResult.SUCCESS, p.getHeldItem(h)); }
        @Override
        public ItemStack onItemUseFinish(ItemStack stack, World w, EntityLivingBase e) {
            if (!(e instanceof EntityPlayer)) return stack;
            EntityPlayer p = (EntityPlayer) e;
            if (p instanceof EntityPlayerMP) net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) p, stack);
            if (!w.isRemote) effect.accept(p);
            if (!p.capabilities.isCreativeMode) {
                stack.shrink(1);
                if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
                if (!p.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE))) p.dropItem(new ItemStack(Items.GLASS_BOTTLE), false);
            }
            return stack;
        }
    }

    /** vinegar ages for one Minecraft day (24000 ticks, counted from when it was made) and then turns into Aged Vinegar */
    public static class ItemYoungVinegar extends ItemTip {
        public static final long AGE = 24000;
        public ItemYoungVinegar() { super(false); setMaxStackSize(1); }
        public static long born(ItemStack s, World w) {
            NBTTagCompound t = s.getTagCompound();
            if (t == null || !t.hasKey("born")) { if (t == null) s.setTagCompound(t = new NBTTagCompound()); t.setLong("born", w.getTotalWorldTime()); }
            return t.getLong("born");
        }
        @Override public void onCreated(ItemStack s, World w, EntityPlayer p) { born(s, w); }
        @Override
        public void onUpdate(ItemStack s, World w, net.minecraft.entity.Entity e, int slot, boolean sel) {
            if (w.isRemote || !(e instanceof EntityPlayer)) return;
            if (w.getTotalWorldTime() - born(s, w) >= AGE) ((EntityPlayer) e).inventory.setInventorySlotContents(slot, new ItemStack(AGED_VINEGAR, s.getCount()));
        }
        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
            super.addInformation(s, w, tip, f);
            if (w != null && s.hasTagCompound()) {
                long left = AGE - (w.getTotalWorldTime() - s.getTagCompound().getLong("born"));
                tip.add(TextFormatting.YELLOW + I18n.translateToLocalFormatted("tooltip.lucifertotem.young_vinegar.left", Math.max(0, left / 1200)));
            }
        }
    }

    /** eat: drowsy (Slowness + Weakness); sneak-right-click a block top to plant it as a squeaking pet-root */
    public static class ItemMandrakeRoot extends ItemFood {
        public ItemMandrakeRoot() { super(2, 0.3f, false); setAlwaysEdible(); }
        @Override
        protected void onFoodEaten(ItemStack s, World w, EntityPlayer p) {
            if (!w.isRemote) { p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 400, 1)); p.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 400, 0)); }
        }
        @Override
        public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand h, EnumFacing f, float hx, float hy, float hz) {
            if (!p.isSneaking() || f != EnumFacing.UP) return EnumActionResult.PASS;
            BlockPos at = pos.up();
            if (!w.isAirBlock(at) || !HBlocks.PET_ROOT.canPlaceBlockAt(w, at)) return EnumActionResult.FAIL;
            if (!w.isRemote) {
                w.setBlockState(at, HBlocks.PET_ROOT.getDefaultState(), 3);
                w.playSound(null, at, HSounds.MANDRAKE_SQUEAK, SoundCategory.BLOCKS, 0.6f, 1.3f);
                if (!p.capabilities.isCreativeMode) p.getHeldItem(h).shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { tips(this, tip); }
    }

    public static class ItemHenbane extends ItemFood {
        public ItemHenbane() { super(1, 0.1f, false); setAlwaysEdible(); }
        @Override protected void onFoodEaten(ItemStack s, World w, EntityPlayer p) {
            if (!w.isRemote) p.addPotionEffect(new PotionEffect(HPotions.INSANIA, lucifertotem.LTConfig.insaniaSeconds * 20, 0));
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { tips(this, tip); }
    }

    public static class ItemHoneyCake extends ItemFood {
        public ItemHoneyCake() { super(5, 0.6f, false); }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { tips(this, tip); }
    }

    // ---------------------------------------------------------------- Sibylline Leaves
    /** a prophecy book (opens in the vanilla book screen); outdoors in the Lightfall it may scatter in the wind (10%) */
    public static class ItemSibylline extends ItemTip {
        public static int scatters = 0;
        public ItemSibylline() { super(true); setMaxStackSize(1); }
        public static boolean hasPages(ItemStack s) { return s.hasTagCompound() && s.getTagCompound().getTagList("pages", 8).tagCount() > 0; }
        public static List<String> pages(ItemStack s) {
            List<String> out = new ArrayList<>();
            if (!s.hasTagCompound()) return out;
            NBTTagList l = s.getTagCompound().getTagList("pages", 8);
            for (int i = 0; i < l.tagCount(); i++) out.add(l.getStringTagAt(i));
            return out;
        }
        public static void setPages(ItemStack s, List<String> pages, long seed) {
            NBTTagCompound t = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
            NBTTagList l = new NBTTagList();
            for (String p : pages) l.appendTag(new NBTTagString(p));
            t.setTag("pages", l);
            t.setLong("seed", seed);
            t.setInteger("opens", 0);
            s.setTagCompound(t);
        }
        /** same answer on client and server: seed + open count */
        public static boolean windRoll(ItemStack s) {
            NBTTagCompound t = s.getTagCompound();
            long x = t.getLong("seed") * 6364136223846793005L + t.getInteger("opens") * 1442695040888963407L;
            x ^= (x >>> 33); x *= 0xff51afd7ed558ccdL; x ^= (x >>> 33);
            return Math.floorMod(x, 100L) < lucifertotem.LTConfig.sibylScatterPercent;
        }
        public static boolean windy(World w, EntityPlayer p) {
            return w.provider.getDimension() == LuciferTotem.dimId && w.canSeeSky(new BlockPos(p.posX, p.posY + p.getEyeHeight(), p.posZ));
        }
        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            ItemStack s = p.getHeldItem(h);
            if (!hasPages(s)) {
                if (!w.isRemote) { setPages(s, Prophecies.sibylPages(w, p), w.rand.nextLong()); HAdv.grant(p, "sibyl"); HBlocks.msg(p, "msg.lucifertotem.sibyl.written"); }
                return new ActionResult<>(EnumActionResult.SUCCESS, s);
            }
            if (windy(w, p) && windRoll(s)) {
                if (!w.isRemote) scatter(w, p, s);
                return new ActionResult<>(EnumActionResult.SUCCESS, s.isEmpty() ? ItemStack.EMPTY : s);
            }
            if (w.isRemote) LuciferTotem.proxy.openSibylBook(p, s);
            else s.getTagCompound().setInteger("opens", s.getTagCompound().getInteger("opens") + 1);
            return new ActionResult<>(EnumActionResult.SUCCESS, s);
        }
        /** the leaves fly off on the wind: one Sibylline Leaf per page */
        public static List<EntityItem> scatter(World w, EntityPlayer p, ItemStack s) {
            List<EntityItem> out = new ArrayList<>();
            List<String> pg = pages(s);
            long id = s.getTagCompound().getLong("seed");
            double wind = w.rand.nextDouble() * Math.PI * 2;
            for (int i = 0; i < pg.size(); i++) {
                ItemStack leaf = new ItemStack(SIBYL_LEAF);
                NBTTagCompound t = new NBTTagCompound();
                t.setString("page", pg.get(i)); t.setInteger("index", i); t.setLong("book", id);
                leaf.setTagCompound(t);
                EntityItem ei = new EntityItem(w, p.posX, p.posY + 1.4, p.posZ, leaf);
                double a = wind + (w.rand.nextDouble() - 0.5) * 1.2, sp = 0.35 + w.rand.nextDouble() * 0.4;
                ei.motionX = Math.cos(a) * sp; ei.motionZ = Math.sin(a) * sp; ei.motionY = 0.25 + w.rand.nextDouble() * 0.3;
                ei.setPickupDelay(30);
                w.spawnEntity(ei);
                out.add(ei);
            }
            s.shrink(1);
            scatters++;
            w.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_ENDERDRAGON_FLAP, SoundCategory.PLAYERS, 0.8f, 1.4f);
            HBlocks.msg(p, "msg.lucifertotem.sibyl.scatter", pg.size());
            return out;
        }
        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
            super.addInformation(s, w, tip, f);
            tip.add(TextFormatting.LIGHT_PURPLE + I18n.translateToLocalFormatted("tooltip.lucifertotem.sibylline_leaves.pages", pages(s).size()));
        }
    }

    /** re-binding: string + the scattered leaves of a book -> the book again, but the wind always keeps one page */
    public static class RecipeRebind extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting inv, World w) { return !getCraftingResult(inv).isEmpty(); }
        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            int string = 0; List<String> pages = new ArrayList<>(); long book = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.isEmpty()) continue;
                if (s.getItem() == Items.STRING) string++;
                else if (s.getItem() == SIBYL_LEAF && s.hasTagCompound()) {
                    long b = s.getTagCompound().getLong("book");
                    if (!pages.isEmpty() && b != book) return ItemStack.EMPTY;
                    book = b; pages.add(s.getTagCompound().getString("page"));
                } else return ItemStack.EMPTY;
            }
            if (string != 1 || pages.size() < 2) return ItemStack.EMPTY;
            ItemStack out = new ItemStack(SIBYLLINE);
            setRebound(out, pages, book);
            return out;
        }
        public static void setRebound(ItemStack out, List<String> pages, long book) {
            List<String> kept = new ArrayList<>(pages.subList(0, pages.size() - 1));
            ItemSibylline.setPages(out, kept, book * 31 + kept.size());
        }
        @Override public boolean canFit(int w, int h) { return w * h >= 3; }
        @Override public ItemStack getRecipeOutput() { return new ItemStack(SIBYLLINE); }
        @Override public boolean isDynamic() { return true; }
    }

    /** one scattered page; its text shows in the tooltip */
    public static class ItemSibylLeaf extends ItemTip {
        public ItemSibylLeaf() { super(false); setMaxStackSize(1); }
        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
            super.addInformation(s, w, tip, f);
            if (!s.hasTagCompound()) return;
            try {
                String txt = net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(s.getTagCompound().getString("page")).getUnformattedText();
                for (String line : net.minecraft.client.Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(txt, 180))
                    tip.add(TextFormatting.LIGHT_PURPLE + "" + TextFormatting.ITALIC + line);
            } catch (Exception ignored) { }
        }
    }

    // ---------------------------------------------------------------- Medea's Fire flask
    public static class ItemMedeaFlask extends ItemTip {
        public ItemMedeaFlask() { super(false); setMaxStackSize(16); }
        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            ItemStack s = p.getHeldItem(h);
            w.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.PLAYERS, 0.5f, 0.4f);
            if (!w.isRemote) {
                EntityMedeaFlask f = new EntityMedeaFlask(w, p);
                f.shoot(p, p.rotationPitch, p.rotationYaw, -20f, 0.6f, 1.0f);
                w.spawnEntity(f);
            }
            if (!p.capabilities.isCreativeMode) s.shrink(1);
            return new ActionResult<>(EnumActionResult.SUCCESS, s);
        }
    }

    // ---------------------------------------------------------------- Bronze Shield (Perseus)
    /** a polished bronze shield: blocking while facing the Gorgon shows only her reflection (no petrification) */
    public static class ItemBronzeShield extends ItemShield {
        public ItemBronzeShield() { setMaxDamage(500); }
        @Override public String getItemStackDisplayName(ItemStack s) { return I18n.translateToLocal(getTranslationKey() + ".name").trim(); }
        @Override public boolean isShield(ItemStack s, EntityLivingBase e) { return true; }
        @Override public boolean getIsRepairable(ItemStack a, ItemStack b) { return b.getItem() == Items.GOLD_INGOT; }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { tips(this, tip); }
    }

    // ---------------------------------------------------------------- Gorgon blood
    /** left vein: right-click with a weapon in the other hand to coat it (24 hits of Wither III).
     *  right vein: kept anywhere in the inventory, it raises you from death once (Asclepius). */
    public static class ItemGorgonBlood extends ItemTip {
        public final boolean right;
        public ItemGorgonBlood(boolean right) { super(true); this.right = right; setMaxStackSize(right ? 1 : 16); }
        @Override public EnumRarity getRarity(ItemStack s) { return EnumRarity.EPIC; }
        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            ItemStack s = p.getHeldItem(h);
            if (right) return new ActionResult<>(EnumActionResult.PASS, s);
            ItemStack weapon = p.getHeldItem(h == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
            if (weapon.isEmpty() || weapon.getMaxStackSize() != 1) { if (!w.isRemote) HBlocks.msg(p, "msg.lucifertotem.blood.need_weapon"); return new ActionResult<>(EnumActionResult.FAIL, s); }
            if (!w.isRemote) {
                coat(weapon);
                if (!p.capabilities.isCreativeMode) s.shrink(1);
                w.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 1f, 0.6f);
                HBlocks.msg(p, "msg.lucifertotem.blood.coated");
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, s);
        }
        public static void coat(ItemStack weapon) {
            if (!weapon.hasTagCompound()) weapon.setTagCompound(new NBTTagCompound());
            weapon.getTagCompound().setInteger("lt_gorgon_coat", 24);
        }
    }

    public static void register(IForgeRegistry<Item> r) {
        r.register(VENOM_VIAL = named(new ItemTip(false), "venom_vial"));
        r.register(DILUTED_VENOM = named(new ItemDrink(false, Venom::drinkDiluted), "diluted_venom"));
        r.register(MITHRIDATIUM = named(new ItemDrink(true, Venom::drinkMithridatium), "mithridatium"));
        r.register(YOUNG_VINEGAR = named(new ItemYoungVinegar(), "young_vinegar"));
        r.register(AGED_VINEGAR = named(new ItemDrink(false, p -> { p.removePotionEffect(HPotions.THIRST); HBlocks.msg(p, "msg.lucifertotem.vinegar.cured"); }), "aged_vinegar"));
        r.register(MANDRAKE_ROOT = named(new ItemMandrakeRoot(), "mandrake_root"));
        r.register(HENBANE_LEAVES = named(new ItemHenbane(), "henbane_leaves"));
        r.register(HONEY_CAKE = named(new ItemHoneyCake(), "honey_cake"));
        r.register(SIBYLLINE = named(new ItemSibylline(), "sibylline_leaves"));
        r.register(SIBYL_LEAF = named(new ItemSibylLeaf(), "sibyl_leaf"));
        r.register(MEDEA_FLASK = named(new ItemMedeaFlask(), "medea_fire_flask"));
        r.register(STAR_FOAM = named(new ItemTip(true), "star_foam"));
        r.register(GREAT_THERIAC = named(new ItemDrink(true, HEvents::drinkGreatTheriac), "great_theriac"));
        GREAT_THERIAC.setMaxStackSize(1);
        r.register(PYTHIAN_LAUREL = named(new ItemTip(false), "pythian_laurel"));
        r.register(BRONZE_SHIELD = named(new ItemBronzeShield(), "bronze_shield"));
        r.register(BLOOD_LEFT = named(new ItemGorgonBlood(false), "gorgon_blood_left"));
        r.register(BLOOD_RIGHT = named(new ItemGorgonBlood(true), "gorgon_blood_right"));
        r.register(ALABASTRON = named(new Alabastron.ItemAlabastron(), "alabastron"));
        r.register(PAINTING = named(new lucifertotem.paint.ItemLadyBabylonPainting(), "lady_babylon_painting"));
    }

    public static Item[] all() {
        return new Item[]{VENOM_VIAL, DILUTED_VENOM, MITHRIDATIUM, YOUNG_VINEGAR, AGED_VINEGAR, MANDRAKE_ROOT, HENBANE_LEAVES, HONEY_CAKE, SIBYLLINE, SIBYL_LEAF,
            MEDEA_FLASK, STAR_FOAM, GREAT_THERIAC, PYTHIAN_LAUREL, BRONZE_SHIELD, BLOOD_LEFT, BLOOD_RIGHT, ALABASTRON, PAINTING};
    }
    /** 1.1.x items plus the 1.3.0 ones (for model registration) */
    public static Item[] allWith130() {
        java.util.List<Item> l = new java.util.ArrayList<>(java.util.Arrays.asList(all()));
        l.addAll(lucifertotem.v130.V130.ITEMS);
        return l.toArray(new Item[0]);
    }
}
