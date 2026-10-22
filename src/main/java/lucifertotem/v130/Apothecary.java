package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HBlocks;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HItems;
import lucifertotem.hillman.HPotions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Stage 1 "The Apothecary": Brain Soaps, Dusk Poppy + Poppy Tears (Somnus), Galen's Theriac Crock + Imperial Theriac, Tyrian Purple. */
public final class Apothecary {
    private Apothecary() {}
    public static Item BRAIN_SOAP, POPPY_TEARS, IMPERIAL_THERIAC, TYRIAN_PURPLE, BURNING_PURPLE;
    public static Block DUSK_POPPY, CROCK;
    public static int naps = 0, soapsUsed = 0, stinks = 0;

    public static void items(IForgeRegistry<Item> r) {
        BRAIN_SOAP = V130.item(r, new ItemBrainSoap(), "brain_soap");
        POPPY_TEARS = V130.item(r, new HItems.ItemDrink(false, Apothecary::drinkPoppyTears), "poppy_tears");
        IMPERIAL_THERIAC = V130.item(r, new HItems.ItemDrink(true, Apothecary::drinkImperial), "imperial_theriac");
        IMPERIAL_THERIAC.setMaxStackSize(1);
        TYRIAN_PURPLE = V130.item(r, new HItems.ItemTip(false), "tyrian_purple");
        BURNING_PURPLE = V130.item(r, new HItems.ItemTip(true), "burning_purple");
    }

    public static void blocks(IForgeRegistry<Block> r) {
        DUSK_POPPY = V130.block(r, new BlockDuskPoppy(), "dusk_poppy", true);
        CROCK = V130.block(r, new BlockCrock(), "theriac_crock", true);
    }

    // ------------------------------------------------------------ Brain Soaps
    /** "They're on brain soaps." Washes out Insania, Mania, Somnus and Nausea. */
    public static class ItemBrainSoap extends HItems.ItemTip {
        public ItemBrainSoap() { super(false); setMaxStackSize(16); }
        @Override public EnumAction getItemUseAction(ItemStack s) { return EnumAction.NONE; }
        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            ItemStack s = p.getHeldItem(h);
            if (!LTConfig.v130.brainSoaps) return new ActionResult<>(EnumActionResult.PASS, s);
            if (!w.isRemote) {
                wash(p);
                if (!p.capabilities.isCreativeMode) s.shrink(1);
                p.getCooldownTracker().setCooldown(this, 20);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, s);
        }
    }

    public static void wash(EntityPlayer p) {
        p.removePotionEffect(HPotions.INSANIA);
        p.removePotionEffect(MobEffects.NAUSEA);
        if (V130.MANIA != null) p.removePotionEffect(V130.MANIA);
        if (V130.SOMNUS != null) p.removePotionEffect(V130.SOMNUS);
        if (V130.STINK != null) p.removePotionEffect(V130.STINK);
        p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 0.6f, 1.6f);
        if (p.world instanceof WorldServer)
            ((WorldServer) p.world).spawnParticle(EnumParticleTypes.WATER_SPLASH, p.posX, p.posY + 1.7, p.posZ, 30, 0.3, 0.3, 0.3, 0.1);
        TextComponentTranslation t = new TextComponentTranslation("msg.lucifertotem.brain_soap");
        t.getStyle().setColor(TextFormatting.LIGHT_PURPLE).setItalic(true);
        p.sendStatusMessage(t, true);
        soapsUsed++;
        HAdv.grant(p, "brain_soaps");
    }

    // ------------------------------------------------------------ Dusk Poppy / Poppy Tears / Somnus
    public static void drinkPoppyTears(EntityPlayer p) {
        p.addPotionEffect(new PotionEffect(V130.SOMNUS, LTConfig.v130.somnusSeconds * 20, 0));
        HBlocks.msg(p, "msg.lucifertotem.poppy_tears");
        HAdv.grant(p, "poppy_tears");
    }

    /** a dark violet poppy of the Lightfall islands; shears bleed it (Poppy Tears), it heals over a few random ticks */
    public static class BlockDuskPoppy extends BlockBush {
        public static final PropertyBool TAPPED = PropertyBool.create("tapped");
        public BlockDuskPoppy() {
            super(Material.PLANTS);
            setSoundType(SoundType.PLANT);
            setTickRandomly(true);
            setDefaultState(blockState.getBaseState().withProperty(TAPPED, false));
        }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, TAPPED); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(TAPPED) ? 1 : 0; }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(TAPPED, (m & 1) == 1); }
        @Override protected boolean canSustainBush(IBlockState s) { Block b = s.getBlock(); return b == ModContent.STAR_GRASS || b == Blocks.GRASS || b == Blocks.DIRT; }
        @Override public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
            super.updateTick(w, pos, s, r);
            if (w.getBlockState(pos).getBlock() == this && s.getValue(TAPPED) && r.nextInt(3) == 0) w.setBlockState(pos, s.withProperty(TAPPED, false), 2);
        }
        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            ItemStack held = p.getHeldItem(hand);
            if (!(held.getItem() instanceof ItemShears)) return false;
            if (!w.isRemote) tap(w, pos, p, held);
            return true;
        }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    public static boolean tap(World w, BlockPos pos, EntityPlayer p, ItemStack shears) {
        IBlockState s = w.getBlockState(pos);
        if (s.getBlock() != DUSK_POPPY) return false;
        if (s.getValue(BlockDuskPoppy.TAPPED)) { if (p != null) HBlocks.msg(p, "msg.lucifertotem.poppy_dry"); return false; }
        w.setBlockState(pos, s.withProperty(BlockDuskPoppy.TAPPED, true), 2);
        Block.spawnAsEntity(w, pos, new ItemStack(POPPY_TEARS));
        w.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 0.8f, 1.3f);
        if (p != null && !shears.isEmpty()) shears.damageItem(1, p);
        return true;
    }

    /** a bed in the Lightfall explodes as ever, unless you are under Somnus: then you get a dreamless nap instead. */
    public static boolean nap(EntityPlayer p, BlockPos bed) {
        if (p.world.provider.getDimension() != LuciferTotem.dimId || V130.SOMNUS == null || !p.isPotionActive(V130.SOMNUS)) return false;
        p.removePotionEffect(V130.SOMNUS);
        p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, 1));
        p.addPotionEffect(new PotionEffect(MobEffects.SATURATION, 20, 0));
        p.heal(6f);
        HBlocks.msg(p, "msg.lucifertotem.somnus_nap");
        naps++;
        HAdv.grant(p, "somnus_nap");
        return true;
    }

    // ------------------------------------------------------------ Galen's Theriac Crock
    /** Galen's ingredients with their code names (Pseudo-Galen's "Blood of Hephaistos" style). required ones are flagged */
    public static final Map<String, String> CODE = new LinkedHashMap<>();
    static {
        CODE.put("lucifertotem:honey_cake", "honey");            // required base
        CODE.put("lucifertotem:venom_vial", "viper");            // required, x3
        CODE.put("lucifertotem:poppy_tears", "poppy");           // required
        CODE.put("lucifertotem:henbane_leaves", "henbane");
        CODE.put("lucifertotem:mandrake_root", "mandrake");
        CODE.put("lucifertotem:tyrian_purple", "murex");
        CODE.put("lucifertotem:star_foam", "star");
        CODE.put("lucifertotem:aged_vinegar", "vinegar");
        CODE.put("lucifertotem:pythian_laurel", "laurel");
        CODE.put("minecraft:red_mushroom", "hephaistos");
        CODE.put("minecraft:spider_eye", "weaver");
        CODE.put("minecraft:golden_carrot", "midas");
        CODE.put("minecraft:speckled_melon", "glistering");
        CODE.put("minecraft:rabbit_foot", "hare");
        CODE.put("minecraft:ghast_tear", "weeping");
        CODE.put("minecraft:glowstone_dust", "sun");
        CODE.put("minecraft:sugar", "reed");
        CODE.put("minecraft:wheat", "demeter");
    }
    public static String codeOf(ItemStack s) { return s.isEmpty() || s.getItem().getRegistryName() == null ? null : CODE.get(s.getItem().getRegistryName().toString()); }
    public static final int THERIAC = 1, GREAT = 2, IMPERIAL = 3;
    public static final int[] DAYS = {0, 3, 5, 10};

    public static class TileCrock extends TileEntity {
        public final Map<String, Integer> contents = new LinkedHashMap<>();
        public long sealedAt = 0;
        public boolean sealed;
        public int tier;

        public int venoms() { return contents.getOrDefault("viper", 0); }
        public int potency() { return contents.size(); }
        public String missing() {
            if (!contents.containsKey("honey")) return "honey";
            if (venoms() < 3) return "viper";
            if (!contents.containsKey("poppy")) return "poppy";
            return null;
        }
        public int tierFor() { int p = potency(); return p >= 10 ? IMPERIAL : p >= 7 ? GREAT : THERIAC; }
        public long readyAt() { return sealedAt + (long) DAYS[tier] * LTConfig.v130.crockDayTicks; }

        public boolean insert(String code) {
            if (sealed || code == null) return false;
            int n = contents.getOrDefault(code, 0);
            if (n >= (code.equals("viper") ? 9 : 1)) return false;
            contents.put(code, n + 1);
            markDirty();
            return true;
        }
        public boolean seal(long now) {
            if (sealed || missing() != null) return false;
            tier = tierFor(); sealedAt = now; sealed = true; markDirty(); sync();
            return true;
        }
        public NonNullList<ItemStack> collect(long now) {
            NonNullList<ItemStack> out = NonNullList.create();
            if (!sealed || now < readyAt()) return out;
            if (tier == IMPERIAL) { out.add(new ItemStack(IMPERIAL_THERIAC)); out.add(new ItemStack(HItems.GREAT_THERIAC)); }
            else if (tier == GREAT) { out.add(new ItemStack(HItems.GREAT_THERIAC)); out.add(new ItemStack(HItems.GREAT_THERIAC)); }
            else out.add(new ItemStack(ModContent.THERIAC, 3));
            contents.clear(); sealed = false; sealedAt = 0; tier = 0; markDirty(); sync();
            return out;
        }
        void sync() { if (world != null && !world.isRemote) { IBlockState s = world.getBlockState(pos); world.notifyBlockUpdate(pos, s, s, 3); } }

        @Override public NBTTagCompound writeToNBT(NBTTagCompound t) {
            super.writeToNBT(t);
            NBTTagCompound c = new NBTTagCompound();
            for (Map.Entry<String, Integer> e : contents.entrySet()) c.setInteger(e.getKey(), e.getValue());
            t.setTag("contents", c); t.setLong("sealedAt", sealedAt); t.setBoolean("sealed", sealed); t.setInteger("tier", tier);
            return t;
        }
        @Override public void readFromNBT(NBTTagCompound t) {
            super.readFromNBT(t);
            contents.clear();
            NBTTagCompound c = t.getCompoundTag("contents");
            for (String k : c.getKeySet()) contents.put(k, c.getInteger(k));
            sealedAt = t.getLong("sealedAt"); sealed = t.getBoolean("sealed"); tier = t.getInteger("tier");
        }
        @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
        @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
        @Override public void onDataPacket(NetworkManager n, SPacketUpdateTileEntity p) { readFromNBT(p.getNbtCompound()); }
    }

    public static class BlockCrock extends Block {
        static final AxisAlignedBB BOX = new AxisAlignedBB(0.125, 0, 0.125, 0.875, 0.875, 0.875);
        public BlockCrock() { super(Material.ROCK); setHardness(1.2f); setResistance(4f); setSoundType(SoundType.STONE); }
        @Override public boolean hasTileEntity(IBlockState s) { return true; }
        @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileCrock(); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
        @Override public net.minecraft.block.state.BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return net.minecraft.block.state.BlockFaceShape.UNDEFINED; }
        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (!LTConfig.v130.theriacCrock) return false;
            if (!w.isRemote) use(w, pos, p, hand);
            return true;
        }
        @Override public void breakBlock(World w, BlockPos pos, IBlockState s) {
            TileEntity te = w.getTileEntity(pos);
            if (te instanceof TileCrock && !((TileCrock) te).sealed) {
                // an unsealed crock spills its herbs back out
                for (Map.Entry<String, Integer> e : ((TileCrock) te).contents.entrySet())
                    for (Map.Entry<String, String> c : CODE.entrySet())
                        if (c.getValue().equals(e.getKey())) {
                            Item it = Item.getByNameOrId(c.getKey());
                            if (it != null) spawnAsEntity(w, pos, new ItemStack(it, e.getValue()));
                        }
            }
            super.breakBlock(w, pos, s);
        }
    }

    /** server-side crock logic, also driven directly by the self-test */
    public static String use(World w, BlockPos pos, EntityPlayer p, EnumHand hand) {
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof TileCrock)) return "none";
        TileCrock c = (TileCrock) te;
        ItemStack held = p.getHeldItem(hand);
        long now = w.getTotalWorldTime();
        if (c.sealed) {
            if (now >= c.readyAt()) {
                NonNullList<ItemStack> out = c.collect(now);
                for (ItemStack o : out) if (!p.inventory.addItemStackToInventory(o)) p.dropItem(o, false);
                w.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.7f, 0.6f);
                HBlocks.msg(p, "msg.lucifertotem.crock.collect");
                HAdv.grant(p, "aged_to_perfection");
                return "collected";
            }
            long days = (c.readyAt() - now + LTConfig.v130.crockDayTicks - 1) / LTConfig.v130.crockDayTicks;
            HBlocks.msg(p, "msg.lucifertotem.crock.aging", new TextComponentTranslation("tier.lucifertotem.crock." + c.tier), days);
            return "aging";
        }
        if (held.isEmpty()) {
            if (!p.isSneaking()) {
                HBlocks.msg(p, "msg.lucifertotem.crock.contents", c.potency(), c.venoms());
                return "contents";
            }
            String miss = c.missing();
            if (miss != null) { HBlocks.msg(p, "msg.lucifertotem.crock.missing", new TextComponentTranslation("code.lucifertotem." + miss)); return "missing"; }
            c.seal(now);
            w.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1f, 0.5f);
            HBlocks.msg(p, "msg.lucifertotem.crock.sealed", new TextComponentTranslation("tier.lucifertotem.crock." + c.tier), DAYS[c.tier]);
            return "sealed";
        }
        String code = codeOf(held);
        if (code == null) { HBlocks.msg(p, "msg.lucifertotem.crock.refuse"); return "refused"; }
        if (!c.insert(code)) { HBlocks.msg(p, "msg.lucifertotem.crock.enough"); return "full"; }
        if (!p.capabilities.isCreativeMode) {
            Item container = held.getItem().getContainerItem() != null ? held.getItem().getContainerItem() : null;
            held.shrink(1);
            if (container != null) p.inventory.addItemStackToInventory(new ItemStack(container));
        }
        w.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.4f, 1.4f);
        HBlocks.msg(p, "msg.lucifertotem.crock.added", new TextComponentTranslation("code.lucifertotem." + code), c.potency());
        return "added";
    }

    /** Imperial Theriac: a long Purple, Regeneration, bad effects purged -- but the body learns it (theriacTolerance) */
    public static void drinkImperial(EntityPlayer p) {
        NBTTagCompound d = HData.get(p);
        long day = p.world.getTotalWorldTime() / 24000L;
        int tol = d.getInteger("theriacTolerance");
        long last = d.getLong("theriacDay");
        if (last > 0) tol = (int) Math.max(0, tol - (day - last));          // the habit fades a point a day
        for (PotionEffect pe : new java.util.ArrayList<>(p.getActivePotionEffects()))
            if (pe.getPotion().isBadEffect()) p.removePotionEffect(pe.getPotion());
        int dur = (int) (6000 * Math.max(0.3, 1.0 - tol * 0.1));
        p.addPotionEffect(new PotionEffect(ModContent.PURPLE, dur, 2));
        p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 600, 1));
        d.setInteger("theriacTolerance", Math.min(10, tol + 1));
        d.setLong("theriacDay", Math.max(1, day));
        d.setLong("lastMeal", p.world.getTotalWorldTime());
        lucifertotem.hillman.HEvents.onTheriac(p);
        HBlocks.msg(p, tol >= 3 ? "msg.lucifertotem.imperial.habit" : "msg.lucifertotem.imperial", Math.min(10, tol + 1));
        HAdv.grant(p, "galens_patient");
    }

    // ------------------------------------------------------------ Porphyry: leather armour + Tyrian Purple
    public static final int PORPHYRY = 0x66023C;
    public static class RecipePorphyry extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        ItemStack armour(InventoryCrafting inv) {
            ItemStack a = ItemStack.EMPTY; int purple = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.isEmpty()) continue;
                if (s.getItem() == TYRIAN_PURPLE) purple++;
                else if (s.getItem() instanceof ItemArmor && ((ItemArmor) s.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER && a.isEmpty()) a = s;
                else return ItemStack.EMPTY;
            }
            return purple == 1 ? a : ItemStack.EMPTY;
        }
        @Override public boolean matches(InventoryCrafting inv, World w) { return !armour(inv).isEmpty(); }
        @Override public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack a = armour(inv);
            if (a.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = a.copy(); out.setCount(1);
            ((ItemArmor) out.getItem()).setColor(out, PORPHYRY);
            out.setStackDisplayName(TextFormatting.DARK_PURPLE + I18n.translateToLocal("item.lucifertotem.porphyry_prefix") + " " + a.getDisplayName());
            return out;
        }
        @Override public boolean canFit(int w, int h) { return w * h >= 2; }
        @Override public ItemStack getRecipeOutput() { return ItemStack.EMPTY; }
        @Override public boolean isDynamic() { return true; }
    }

    // ------------------------------------------------------------ events: beds, the Stink, passive flight
    public static class Events {
        @SubscribeEvent
        public void bed(PlayerInteractEvent.RightClickBlock e) {
            if (e.getWorld().isRemote) return;
            if (!(e.getWorld().getBlockState(e.getPos()).getBlock() instanceof BlockBed)) return;
            if (nap(e.getEntityPlayer(), e.getPos())) { e.setCanceled(true); e.setCancellationResult(EnumActionResult.SUCCESS); }
        }

        @SubscribeEvent
        public void death(LivingDeathEvent e) {
            if (e.getEntityLiving() instanceof EntityPeerReviewer && e.getSource().getTrueSource() instanceof EntityPlayer) HAdv.grant((EntityPlayer) e.getSource().getTrueSource(), "peer_review");
            if (!(e.getEntityLiving() instanceof EntityMurex) || e.getEntityLiving().world.isRemote) return;
            if (e.getSource().getTrueSource() instanceof EntityPlayer) crushed((EntityPlayer) e.getSource().getTrueSource());
        }

        @SubscribeEvent
        public void tick(TickEvent.PlayerTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || V130.STINK == null) return;
            if (!e.player.isPotionActive(V130.STINK) || e.player.ticksExisted % 20 != 0) return;
            stinkScatter(e.player);
        }
    }

    /** ten murex crushed within a minute: The Stink (the dye-works of Tyre were downwind of the city for a reason) */
    public static boolean crushed(EntityPlayer p) {
        NBTTagCompound d = HData.get(p);
        long now = p.world.getTotalWorldTime();
        long[] t = new long[10];
        int[] arr = d.getIntArray("murexTimes");
        int n = 0;
        for (int i = 0; i < arr.length; i += 2) { long v = ((long) arr[i] << 32) | (arr[i + 1] & 0xFFFFFFFFL); if (now - v <= 1200) t[n++] = v; if (n >= 9) break; }
        t[n++] = now;
        if (n >= 10) {
            d.setIntArray("murexTimes", new int[0]);
            p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 100, 0));
            p.addPotionEffect(new PotionEffect(V130.STINK, 1200, 0));
            HBlocks.msg(p, "msg.lucifertotem.the_stink");
            stinks++;
            HAdv.grant(p, "the_stink");
            return true;
        }
        int[] out = new int[n * 2];
        for (int i = 0; i < n; i++) { out[i * 2] = (int) (t[i] >>> 32); out[i * 2 + 1] = (int) t[i]; }
        d.setIntArray("murexTimes", out);
        return false;
    }

    public static int stinkScatter(EntityPlayer p) {
        int n = 0;
        for (EntityAnimal a : p.world.getEntitiesWithinAABB(EntityAnimal.class, p.getEntityBoundingBox().grow(12))) {
            Vec3d away = new Vec3d(a.posX - p.posX, 0, a.posZ - p.posZ).normalize().scale(10);
            a.getNavigator().tryMoveToXYZ(a.posX + away.x, a.posY, a.posZ + away.z, 1.4);
            n++;
        }
        return n;
    }

    // tooltip with Galen's code names (client)
    @SideOnly(Side.CLIENT)
    public static void codeTooltip(ItemStack s, List<String> tip) {
        String c = codeOf(s);
        if (c != null && I18n.canTranslate("code.lucifertotem." + c))
            tip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + I18n.translateToLocalFormatted("tooltip.lucifertotem.galen_code", I18n.translateToLocal("code.lucifertotem." + c)));
    }
}
