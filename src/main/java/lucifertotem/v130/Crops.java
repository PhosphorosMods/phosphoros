package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.*;

/**
 * Stage 4.5 "The Garden of the Gods": four crops.
 *  Silphium (Cyrene's lost spice, goes extinct locally if over-harvested), Moly (Hermes' herb: the Moly Ward),
 *  Eleusinian Barley (with the occasional ergot: the strongest Kykeon), Purple Ambrosia (the food of the gods).
 */
public final class Crops {
    private Crops() {}
    public static Item SILPHIUM_SEEDS, SILPHIUM_RESIN, MOLY_SEEDS, MOLY, BARLEY_SEEDS, BARLEY, ERGOT, KYKEON_HIEROPHANT, AMBROSIA_SEEDS, PURPLE_AMBROSIA, PURPLE_NECTAR;
    public static BlockCropLT SILPHIUM, MOLY_PLANT, BARLEY_PLANT, AMBROSIA;
    public static Block BLESSED_FARMLAND;
    public static Potion MOLY_WARD;
    public static PotionType MOLY_WARD_TYPE, MOLY_WARD_LONG;
    public static int extinctions = 0, digFails = 0, ergots = 0, ambrosiaEaten = 0, ambrosial = 0, blessings = 0, wardBlocks = 0, barleyBreeds = 0, seasonedEaten = 0;
    public static long timeShift = 0;   // test hook
    public static final String SEASONED = "lt_silphium";

    // ================================================================ registration
    public static void blocks(IForgeRegistry<Block> r) {
        SILPHIUM = V130.block(r, new BlockSilphium(), "silphium", false);
        MOLY_PLANT = V130.block(r, new BlockMoly(), "moly", false);
        BARLEY_PLANT = V130.block(r, new BlockBarley(), "barley", false);
        AMBROSIA = V130.block(r, new BlockAmbrosia(), "ambrosia", false);
        BLESSED_FARMLAND = V130.block(r, new BlockBlessedFarmland(), "blessed_farmland", true);
    }
    public static void items(IForgeRegistry<Item> r) {
        SILPHIUM_SEEDS = V130.item(r, new ItemSeedsLT(() -> SILPHIUM), "silphium_seeds");
        SILPHIUM_RESIN = V130.item(r, new HItems.ItemTip(false), "silphium_resin");
        MOLY_SEEDS = V130.item(r, new ItemSeedsLT(() -> MOLY_PLANT), "moly_seeds");
        MOLY = V130.item(r, new ItemFoodLT(2, 0.3f, true, Crops::eatMoly), "moly");
        BARLEY_SEEDS = V130.item(r, new ItemSeedsLT(() -> BARLEY_PLANT), "barley_seeds");
        BARLEY = V130.item(r, new HItems.ItemTip(false), "barley");
        ERGOT = V130.item(r, new ItemFoodLT(1, 0.1f, true, Crops::eatErgot), "ergot");
        KYKEON_HIEROPHANT = V130.item(r, new HItems.ItemDrink(true, Crops::drinkHierophant), "kykeon_hierophant");
        AMBROSIA_SEEDS = V130.item(r, new ItemSeedsLT(() -> AMBROSIA), "ambrosia_seeds");
        PURPLE_AMBROSIA = V130.item(r, new ItemFoodLT(20, 1.0f, true, p -> ambrosia(p, false)), "purple_ambrosia");
        PURPLE_NECTAR = V130.item(r, new HItems.ItemDrink(true, p -> ambrosia(p, true)), "purple_nectar");
        ((Item) PURPLE_AMBROSIA).setMaxStackSize(16);
    }
    public static void potions(IForgeRegistry<Potion> r) {
        MOLY_WARD = new V130.Potion130("moly_ward", false, 0xF4F0FF, 4);
        r.register(MOLY_WARD);
    }
    public static void potionTypes(IForgeRegistry<PotionType> r) {
        MOLY_WARD_TYPE = new PotionType("moly_ward", new PotionEffect(MOLY_WARD, 3600)).setRegistryName(LuciferTotem.MODID, "moly_ward");
        MOLY_WARD_LONG = new PotionType("moly_ward", new PotionEffect(MOLY_WARD, 9600)).setRegistryName(LuciferTotem.MODID, "long_moly_ward");
        r.register(MOLY_WARD_TYPE); r.register(MOLY_WARD_LONG);
    }
    public static void init() {
        Apothecary.CODE.put("lucifertotem:silphium_resin", "silphium");
        BrewingRecipeRegistry.addRecipe(new PotionBrew(PotionTypes.AWKWARD, () -> MOLY, () -> MOLY_WARD_TYPE));
        BrewingRecipeRegistry.addRecipe(new PotionBrew(null, () -> Items.REDSTONE, () -> MOLY_WARD_LONG) {
            @Override public boolean isInput(ItemStack s) { return s.getItem() instanceof ItemPotion && PotionUtils.getPotionFromItem(s) == MOLY_WARD_TYPE; }
        });
        if (LTConfig.v130.eleusinianBarley) net.minecraftforge.common.MinecraftForge.addGrassSeed(new ItemStack(BARLEY_SEEDS), 2);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events());
    }
    static boolean on(String crop) {
        switch (crop) {
            case "silphium": return LTConfig.v130.silphium;
            case "moly": return LTConfig.v130.moly;
            case "barley": return LTConfig.v130.eleusinianBarley;
            case "ambrosia": return LTConfig.v130.ambrosia;
        }
        return true;
    }
    static long now(World w) { return w.getTotalWorldTime() + timeShift; }

    // ================================================================ crop base
    public abstract static class BlockCropLT extends BlockCrops {
        final String key;
        BlockCropLT(String key) { this.key = key; setSoundType(SoundType.PLANT); setHardness(0f); }
        protected abstract IProperty<?>[] props();
        protected abstract PropertyInteger ageProp();
        @Override protected PropertyInteger getAgeProperty() { return ageProp(); }
        @Override public int getMaxAge() { return Collections.max(ageProp().getAllowedValues()); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, props()); }
        @Override public IBlockState getStateFromMeta(int m) { return withAge(Math.min(m & 7, getMaxAge())); }
        @Override public int getMetaFromState(IBlockState s) { return getAge(s); }
        /** soils it can stand on */
        public boolean soil(IBlockState s) { return s.getBlock() == Blocks.FARMLAND || s.getBlock() == BLESSED_FARMLAND; }
        @Override protected boolean canSustainBush(IBlockState s) { return soil(s); }
        @Override public boolean canBlockStay(World w, BlockPos pos, IBlockState s) { return soil(w.getBlockState(pos.down())); }
        @Override public boolean canPlaceBlockAt(World w, BlockPos pos) { return w.getBlockState(pos).getBlock().isReplaceable(w, pos) && soil(w.getBlockState(pos.down())); }
        /** the special conditions of this crop (and its config toggle) */
        public boolean canGrowHere(World w, BlockPos pos) { return on(key); }
        public int growthOneIn() { return 3; }
        public float bonemealChance() { return 1f; }
        protected boolean lit(World w, BlockPos pos) { return w.provider.getDimension() == LuciferTotem.dimId || w.getLightFromNeighbors(pos.up()) >= 9; }
        @Override public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
            checkAndDropBlock(w, pos, s);
            if (w.getBlockState(pos).getBlock() != this || !w.isAreaLoaded(pos, 1)) return;
            tryGrow(w, pos, w.getBlockState(pos), r);
        }
        /** one random growth attempt: true if it grew */
        public boolean tryGrow(World w, BlockPos pos, IBlockState s, Random r) {
            if (isMaxAge(s) || !lit(w, pos) || r.nextInt(growthOneIn()) != 0 || !canGrowHere(w, pos)) return false;
            w.setBlockState(pos, grown(s, getAge(s) + 1, r), 2);
            return true;
        }
        protected IBlockState grown(IBlockState s, int age, Random r) { return withAge(Math.min(age, getMaxAge())); }
        @Override public boolean canGrow(World w, BlockPos pos, IBlockState s, boolean client) { return !isMaxAge(s) && (client || canGrowHere(w, pos)); }
        @Override public boolean canUseBonemeal(World w, Random r, BlockPos pos, IBlockState s) { return r.nextFloat() < bonemealChance(); }
        @Override public void grow(World w, BlockPos pos, IBlockState s) { w.setBlockState(pos, grown(s, getAge(s) + 1, w.rand), 2); }
        @Override public void grow(World w, Random r, BlockPos pos, IBlockState s) { grow(w, pos, s); }
        @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
            Random r = w instanceof World ? ((World) w).rand : RANDOM;
            if (!isMaxAge(s)) { drops.add(new ItemStack(getSeed())); return; }
            harvest(drops, s, r, fortune);
        }
        protected abstract void harvest(NonNullList<ItemStack> drops, IBlockState s, Random r, int fortune);
    }
    static final PropertyInteger AGE3 = PropertyInteger.create("age", 0, 3), AGE4 = PropertyInteger.create("age", 0, 4);
    public static final PropertyBool ERGOT_P = PropertyBool.create("ergot");

    public static class ItemSeedsLT extends HItems.ItemTip implements IPlantable {
        final java.util.function.Supplier<BlockCropLT> crop;
        public ItemSeedsLT(java.util.function.Supplier<BlockCropLT> crop) { super(false); this.crop = crop; }
        @Override public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            ItemStack s = p.getHeldItem(hand);
            BlockPos up = pos.up();
            if (f != EnumFacing.UP || !p.canPlayerEdit(up, f, s) || !crop.get().canPlaceBlockAt(w, up)) return EnumActionResult.FAIL;
            w.setBlockState(up, crop.get().getDefaultState(), 3);
            w.playSound(null, up, net.minecraft.init.SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS, 1f, 1f);
            s.shrink(1);
            return EnumActionResult.SUCCESS;
        }
        @Override public EnumPlantType getPlantType(IBlockAccess w, BlockPos pos) { return EnumPlantType.Crop; }
        @Override public IBlockState getPlant(IBlockAccess w, BlockPos pos) { return crop.get().getDefaultState(); }
    }
    public static class ItemFoodLT extends ItemFood {
        final java.util.function.Consumer<EntityPlayer> eat;
        public ItemFoodLT(int food, float sat, boolean always, java.util.function.Consumer<EntityPlayer> eat) { super(food, sat, false); this.eat = eat; if (always) setAlwaysEdible(); }
        @Override protected void onFoodEaten(ItemStack s, World w, EntityPlayer p) { if (!w.isRemote) eat.accept(p); }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, net.minecraft.client.util.ITooltipFlag f) { HItems.tips(this, tip); }
        @Override @SideOnly(Side.CLIENT) public boolean hasEffect(ItemStack s) { return this == PURPLE_AMBROSIA; }
    }
    /** a potion recipe matching on the potion type (Forge's default ignores NBT) */
    static class PotionBrew implements IBrewingRecipe {
        final PotionType in; final java.util.function.Supplier<Item> ing; final java.util.function.Supplier<PotionType> out;
        PotionBrew(PotionType in, java.util.function.Supplier<Item> ing, java.util.function.Supplier<PotionType> out) { this.in = in; this.ing = ing; this.out = out; }
        @Override public boolean isInput(ItemStack s) { return s.getItem() instanceof ItemPotion && PotionUtils.getPotionFromItem(s) == in; }
        @Override public boolean isIngredient(ItemStack s) { return s.getItem() == ing.get(); }
        @Override public ItemStack getOutput(ItemStack input, ItemStack i) {
            if (!isInput(input) || !isIngredient(i)) return ItemStack.EMPTY;
            return PotionUtils.addPotionToItemStack(new ItemStack(input.getItem()), out.get());
        }
    }

    // ================================================================ SILPHIUM
    public static class BlockSilphium extends BlockCropLT {
        public BlockSilphium() { super("silphium"); }
        @Override protected IProperty<?>[] props() { return new IProperty<?>[]{AGE3}; }
        @Override protected PropertyInteger ageProp() { return AGE3; }
        @Override protected Item getSeed() { return SILPHIUM_SEEDS; }
        @Override protected Item getCrop() { return SILPHIUM_RESIN; }
        @Override public boolean soil(IBlockState s) {
            Block b = s.getBlock();
            return super.soil(s) || b == Blocks.SAND || b == ModContent.STAR_GRASS || (b == Blocks.DIRT && s.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.COARSE_DIRT);
        }
        @Override public int growthOneIn() { return Math.max(1, LTConfig.v130.silphiumGrowthOneIn); }
        @Override public float bonemealChance() { return 0.33f; }
        @Override public boolean canGrowHere(World w, BlockPos pos) { return super.canGrowHere(w, pos) && nearSea(w, pos) && !SilphiumData.get(w).extinct(pos, now(w)); }
        @Override protected void harvest(NonNullList<ItemStack> d, IBlockState s, Random r, int fortune) {
            d.add(new ItemStack(SILPHIUM_RESIN, 1 + r.nextInt(2)));
            if (r.nextInt(2) == 0) d.add(new ItemStack(SILPHIUM_SEEDS));
        }
        @Override public void onBlockHarvested(World w, BlockPos pos, IBlockState s, EntityPlayer p) {
            super.onBlockHarvested(w, pos, s, p);
            if (!w.isRemote && isMaxAge(s)) harvested(w, pos, p);
        }
    }
    /** the purple sea (any water) within 6 blocks */
    public static boolean nearSea(World w, BlockPos pos) {
        for (BlockPos q : BlockPos.getAllInBoxMutable(pos.add(-6, -2, -6), pos.add(6, 0, 6)))
            if (w.getBlockState(q).getMaterial() == net.minecraft.block.material.Material.WATER) return true;
        return false;
    }
    /** a mature silphium was cut: too many in one place within the window and it goes extinct there */
    public static boolean harvested(World w, BlockPos pos, EntityPlayer p) {
        SilphiumData d = SilphiumData.get(w);
        long t = now(w);
        if (d.extinct(pos, t)) return true;
        boolean gone = d.harvest(pos, t);
        if (gone) {
            extinctions++;
            for (EntityPlayer q : w.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(32))) {
                TextComponentTranslation m = new TextComponentTranslation("msg.lucifertotem.silphium.extinct");
                m.getStyle().setColor(TextFormatting.GOLD);
                q.sendMessage(m);
            }
            if (p != null) HAdv.grant(p, "extinct_again");
        }
        return gone;
    }
    public static class SilphiumData extends WorldSavedData {
        static final String NAME = "lucifertotem_silphium";
        final Map<Long, List<Long>> cuts = new HashMap<>();
        public final Map<Long, Long> until = new HashMap<>();
        public SilphiumData() { super(NAME); }
        public SilphiumData(String n) { super(n); }
        public static SilphiumData get(World w) {
            net.minecraft.world.storage.MapStorage ms = w.getPerWorldStorage();
            SilphiumData d = (SilphiumData) ms.getOrLoadData(SilphiumData.class, NAME);
            if (d == null) { d = new SilphiumData(); ms.setData(NAME, d); }
            return d;
        }
        public static long cell(BlockPos p) { return ((long) (p.getX() >> 5) << 32) | ((p.getZ() >> 5) & 0xFFFFFFFFL); }
        public boolean extinct(BlockPos p, long now) { Long u = until.get(cell(p)); return u != null && now < u; }
        public long extinctUntil(BlockPos p) { return until.getOrDefault(cell(p), 0L); }
        boolean harvest(BlockPos p, long now) {
            long c = cell(p), win = LTConfig.v130.silphiumWindowDays * 24000L;
            List<Long> l = cuts.computeIfAbsent(c, k -> new ArrayList<>());
            l.removeIf(t -> now - t > win);
            l.add(now);
            markDirty();
            if (l.size() >= LTConfig.v130.silphiumExtinctHarvests) { l.clear(); until.put(c, now + LTConfig.v130.silphiumExtinctDays * 24000L); return true; }
            return false;
        }
        @Override public void readFromNBT(NBTTagCompound t) {
            cuts.clear(); until.clear();
            NBTTagList l = t.getTagList("Cells", 10);
            for (int i = 0; i < l.tagCount(); i++) {
                NBTTagCompound c = l.getCompoundTagAt(i);
                long k = c.getLong("Cell");
                List<Long> ts = new ArrayList<>(); for (long v : getLongs(c, "Cuts")) ts.add(v);
                cuts.put(k, ts);
                if (c.hasKey("Until")) until.put(k, c.getLong("Until"));
            }
        }
        static long[] getLongs(NBTTagCompound c, String k) {
            NBTTagList l = c.getTagList(k, 4); long[] out = new long[l.tagCount()];
            for (int i = 0; i < out.length; i++) out[i] = ((net.minecraft.nbt.NBTTagLong) l.get(i)).getLong();
            return out;
        }
        @Override public NBTTagCompound writeToNBT(NBTTagCompound t) {
            NBTTagList l = new NBTTagList();
            Set<Long> keys = new HashSet<>(cuts.keySet()); keys.addAll(until.keySet());
            for (long k : keys) {
                NBTTagCompound c = new NBTTagCompound(); c.setLong("Cell", k);
                NBTTagList ts = new NBTTagList(); for (long v : cuts.getOrDefault(k, Collections.emptyList())) ts.appendTag(new net.minecraft.nbt.NBTTagLong(v));
                c.setTag("Cuts", ts);
                if (until.containsKey(k)) c.setLong("Until", until.get(k));
                l.appendTag(c);
            }
            t.setTag("Cells", l);
            return t;
        }
    }

    // ================================================================ MOLY
    public static class BlockMoly extends BlockCropLT {
        public BlockMoly() { super("moly"); }
        @Override protected IProperty<?>[] props() { return new IProperty<?>[]{AGE3}; }
        @Override protected PropertyInteger ageProp() { return AGE3; }
        @Override protected Item getSeed() { return MOLY_SEEDS; }
        @Override protected Item getCrop() { return MOLY; }
        @Override public int growthOneIn() { return 4; }
        @Override protected void harvest(NonNullList<ItemStack> d, IBlockState s, Random r, int fortune) {
            d.add(new ItemStack(MOLY, 1 + r.nextInt(2)));
            d.add(new ItemStack(MOLY_SEEDS, 1 + r.nextInt(2)));
        }
    }
    /** "Hard for mortals to dig up": bare hands (or the wrong tool) fail half the time; a golden hoe or shears never do */
    public static boolean digFails(EntityPlayer p, Random r) {
        if (p.capabilities.isCreativeMode) return false;
        Item h = p.getHeldItemMainhand().getItem();
        if (h == Items.GOLDEN_HOE || h instanceof ItemShears) return false;
        return r.nextInt(2) == 0;
    }
    static void eatMoly(EntityPlayer p) { ward(p, LTConfig.v130.molyWardSeconds * 20); }
    public static void ward(EntityLivingBase e, int ticks) {
        e.removePotionEffect(V130.MANIA); e.removePotionEffect(HPotions.INSANIA); e.removePotionEffect(HPotions.PETRIFYING);
        e.addPotionEffect(new PotionEffect(MOLY_WARD, ticks, 0));
        for (EntityHekateHound h : e.world.getEntitiesWithinAABB(EntityHekateHound.class, e.getEntityBoundingBox().grow(32))) if (h.getAttackTarget() == e) h.setAttackTarget(null);
        if (e instanceof EntityPlayer) HAdv.grant((EntityPlayer) e, "hermes_gift");
    }
    public static boolean warded(Entity e) { return e instanceof EntityLivingBase && MOLY_WARD != null && ((EntityLivingBase) e).isPotionActive(MOLY_WARD); }

    // ================================================================ ELEUSINIAN BARLEY
    public static class BlockBarley extends BlockCropLT {
        public BlockBarley() { super("barley"); setDefaultState(blockState.getBaseState().withProperty(AGE, 0).withProperty(ERGOT_P, false)); }
        @Override protected IProperty<?>[] props() { return new IProperty<?>[]{AGE, ERGOT_P}; }
        @Override protected PropertyInteger ageProp() { return AGE; }
        @Override protected Item getSeed() { return BARLEY_SEEDS; }
        @Override protected Item getCrop() { return BARLEY; }
        @Override public IBlockState getStateFromMeta(int m) { return withAge(m & 7).withProperty(ERGOT_P, (m & 8) != 0); }
        @Override public int getMetaFromState(IBlockState s) { return getAge(s) | (s.getValue(ERGOT_P) ? 8 : 0); }
        @Override protected IBlockState grown(IBlockState s, int age, Random r) {
            IBlockState n = withAge(Math.min(age, getMaxAge()));
            if (isMaxAge(n) && r.nextInt(100) < LTConfig.v130.ergotPercent) { n = n.withProperty(ERGOT_P, true); ergots++; }
            return n;
        }
        @Override protected void harvest(NonNullList<ItemStack> d, IBlockState s, Random r, int fortune) {
            d.add(new ItemStack(BARLEY));
            int seeds = 1 + r.nextInt(2 + fortune);
            d.add(new ItemStack(BARLEY_SEEDS, seeds));
            if (s.getValue(ERGOT_P)) d.add(new ItemStack(ERGOT));
        }
    }
    static void eatErgot(EntityPlayer p) {
        p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 300, 0));
        p.addPotionEffect(new PotionEffect(HPotions.INSANIA, 600, 0));
        HBlocks.msg(p, "msg.lucifertotem.ergot.eaten");
        HAdv.grant(p, "you_were_warned");
    }
    static void drinkHierophant(EntityPlayer p) {
        p.addPotionEffect(new PotionEffect(V130.KYKEON, 6000, 1));
        p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 160, 0));
        p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 1200, 0));
        HAdv.grant(p, "kykeon");
        Stage4.vision(p, true);
    }

    // ================================================================ PURPLE AMBROSIA (and the blessed farmland)
    public static class BlockAmbrosia extends BlockCropLT {
        public BlockAmbrosia() { super("ambrosia"); }
        @Override protected IProperty<?>[] props() { return new IProperty<?>[]{AGE4}; }
        @Override protected PropertyInteger ageProp() { return AGE4; }
        @Override protected Item getSeed() { return AMBROSIA_SEEDS; }
        @Override protected Item getCrop() { return PURPLE_AMBROSIA; }
        @Override public int growthOneIn() { return Math.max(1, LTConfig.v130.ambrosiaGrowthOneIn); }
        @Override public float bonemealChance() { return 0.5f; }
        @Override public boolean canGrowHere(World w, BlockPos pos) {
            return super.canGrowHere(w, pos) && (w.provider.getDimension() == LuciferTotem.dimId || w.getBlockState(pos.down()).getBlock() == BLESSED_FARMLAND);
        }
        @Override public int getLightValue(IBlockState s, IBlockAccess w, BlockPos pos) { return getAge(s) >= 4 ? 10 : getAge(s) == 3 ? 4 : 0; }
        @Override protected void harvest(NonNullList<ItemStack> d, IBlockState s, Random r, int fortune) {
            d.add(new ItemStack(PURPLE_AMBROSIA));
            if (r.nextInt(4) == 0) d.add(new ItemStack(AMBROSIA_SEEDS));
        }
        @Override @SideOnly(Side.CLIENT) public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
            if (getAge(s) < 3) return;
            for (int i = 0; i < 2; i++) w.spawnParticle(r.nextBoolean() ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.END_ROD, p.getX() + 0.2 + r.nextDouble() * 0.6, p.getY() + 0.3 + r.nextDouble() * 0.6, p.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }
    public static class BlockBlessedFarmland extends BlockFarmland {
        public BlockBlessedFarmland() { setHardness(0.6f); setSoundType(SoundType.GROUND); setDefaultState(blockState.getBaseState().withProperty(MOISTURE, 7)); setLightOpacity(255); }
        @Override public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
            if (s.getValue(MOISTURE) < 7) w.setBlockState(pos, s.withProperty(MOISTURE, 7), 2);   // theriac-watered: it never dries
        }
        @Override public void onFallenUpon(World w, BlockPos pos, Entity e, float d) { e.fall(d, 1f); }             // and it is not trampled
        @Override public boolean canSustainPlant(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing f, IPlantable p) {
            return p.getPlantType(w, pos.offset(f)) == EnumPlantType.Crop || super.canSustainPlant(s, w, pos, f, p);
        }
        @Override public boolean isFertile(World w, BlockPos pos) { return true; }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return Item.getItemFromBlock(Blocks.DIRT); }
        @Override @SideOnly(Side.CLIENT) public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
            if (r.nextInt(4) == 0) w.spawnParticle(EnumParticleTypes.SPELL_WITCH, p.getX() + r.nextDouble(), p.getY() + 1.0, p.getZ() + r.nextDouble(), 0, 0, 0);
        }
    }
    public static boolean theriac(ItemStack s) {
        Item i = s.getItem();
        return i == ModContent.THERIAC || i == HItems.GREAT_THERIAC || i == Apothecary.IMPERIAL_THERIAC;
    }
    /** right-click farmland with a theriac: it becomes Theriac-watered (blessed) farmland */
    public static boolean bless(World w, BlockPos pos, EntityPlayer p, ItemStack s) {
        if (!theriac(s) || w.getBlockState(pos).getBlock() != Blocks.FARMLAND) return false;
        w.setBlockState(pos, BLESSED_FARMLAND.getDefaultState(), 3);
        if (!p.capabilities.isCreativeMode) { s.shrink(1); ItemStack b = new ItemStack(Items.GLASS_BOTTLE); if (!p.inventory.addItemStackToInventory(b)) p.dropItem(b, false); }
        w.playSound(null, pos, net.minecraft.init.SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1f, 0.8f);
        if (w instanceof WorldServer) ((WorldServer) w).spawnParticle(EnumParticleTypes.SPELL_WITCH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.4, 0.1, 0.4, 0.02);
        HBlocks.msg(p, "msg.lucifertotem.farmland.blessed");
        blessings++;
        return true;
    }
    /** Purple Ambrosia (nectar = stronger) */
    public static void ambrosia(EntityPlayer p, boolean nectar) {
        ambrosiaEaten++;
        p.addPotionEffect(new PotionEffect(ModContent.PURPLE, nectar ? 6000 : 2400, nectar ? 2 : 1));
        p.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, nectar ? 3600 : 2400, 1));
        p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, nectar ? 200 : 100, 1));
        if (nectar) p.getFoodStats().addStats(6, 1.0f);
        if (p.world.rand.nextInt(Math.max(1, LTConfig.v130.ambrosialOneIn)) == 0) {
            ambrosial++;
            p.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 600, 0));
            TextComponentTranslation m = new TextComponentTranslation("msg.lucifertotem.ambrosia.ambrosial");
            m.getStyle().setColor(TextFormatting.LIGHT_PURPLE).setItalic(true);
            p.sendMessage(m);
        }
        EntityAmmonius a = EntityAmmonius.nearest(p.world, p, 16);
        if (a != null) a.blurtPurple();
        HAdv.grant(p, "food_of_the_gods");
    }
    /** a one-time gift of ambrosia seeds (Lucifer's interview, the Ammon U diploma) */
    public static boolean gift(EntityPlayer p, String flag, int n) {
        if (!LTConfig.v130.ambrosia) return false;
        NBTTagCompound d = HData.get(p);
        if (d.getBoolean(flag)) return false;
        d.setBoolean(flag, true);
        ItemStack s = new ItemStack(AMBROSIA_SEEDS, n);
        if (!p.inventory.addItemStackToInventory(s)) p.dropItem(s, false);
        HBlocks.msg(p, "msg.lucifertotem.ambrosia.gift");
        return true;
    }

    // ================================================================ events
    public static class Events {
        /** Moly Ward: no Mania, Insania or petrification; witches' curses slide off */
        @SubscribeEvent
        public void applicable(PotionEvent.PotionApplicableEvent e) {
            EntityLivingBase t = e.getEntityLiving();
            if (t.world.isRemote || !warded(t)) return;
            Potion p = e.getPotionEffect().getPotion();
            boolean deny = p == V130.MANIA || p == HPotions.INSANIA || p == HPotions.PETRIFYING || p == HPotions.PETRIFIED;
            if (!deny && p.isBadEffect() && !t.world.getEntitiesWithinAABB(EntityWitch.class, t.getEntityBoundingBox().grow(16)).isEmpty()) deny = true;
            if (deny) { e.setResult(Event.Result.DENY); wardBlocks++; }
        }
        /** ...and Medea's sorcerous fire cannot touch the warded */
        @SubscribeEvent
        public void attacked(LivingAttackEvent e) {
            EntityLivingBase t = e.getEntityLiving();
            if (t.world.isRemote || !warded(t)) return;
            DamageSource s = e.getSource();
            if ((s == DamageSource.IN_FIRE || s == DamageSource.ON_FIRE) && medeaFireNear(t)) { e.setCanceled(true); t.extinguish(); wardBlocks++; }
            if (s.getTrueSource() instanceof EntityWitch && s.isMagicDamage()) { e.setCanceled(true); wardBlocks++; }
        }
        @SubscribeEvent
        public void breakBlock(BlockEvent.BreakEvent e) {
            if (e.getWorld().isRemote) return;
            IBlockState s = e.getState();
            if (s.getBlock() == MOLY_PLANT && MOLY_PLANT.isMaxAge(s) && digFails(e.getPlayer(), e.getWorld().rand)) {
                e.setCanceled(true); digFails++;
                HBlocks.msg(e.getPlayer(), "msg.lucifertotem.moly.holds");
            }
        }
        /** wild seeds from the plants of the Lightfall */
        @SubscribeEvent
        public void drops(BlockEvent.HarvestDropsEvent e) {
            World w = e.getWorld();
            if (w.isRemote || e.getHarvester() == null || w.provider.getDimension() != LuciferTotem.dimId) return;
            Block b = e.getState().getBlock();
            if (!(b instanceof net.minecraft.block.BlockBush) || b instanceof BlockCrops || b instanceof net.minecraft.block.BlockSapling) return;
            Random r = w.rand;
            if (on("barley") && r.nextInt(10) == 0) e.getDrops().add(new ItemStack(BARLEY_SEEDS));
            if (on("silphium") && r.nextInt(40) == 0) e.getDrops().add(new ItemStack(SILPHIUM_SEEDS));
            if (on("moly") && r.nextInt(80) == 0) e.getDrops().add(new ItemStack(MOLY_SEEDS));
        }
        /** Circe's herb grows where witches go: they drop Moly seeds */
        @SubscribeEvent
        public void mobDrops(LivingDropsEvent e) {
            if (e.getEntityLiving() instanceof EntityWitch && on("moly") && e.getEntityLiving().world.rand.nextInt(6) == 0)
                e.getEntityLiving().entityDropItem(new ItemStack(MOLY_SEEDS), 0.5f);
        }
        @SubscribeEvent
        public void loot(LootTableLoadEvent e) {
            String n = e.getName().toString();
            if (n.equals("lucifertotem:chests/library")) e.getTable().addPool(pool("lt_crops", new Object[][]{{SILPHIUM_SEEDS, 2}, {AMBROSIA_SEEDS, 1}, {MOLY_SEEDS, 1}, {null, 30}}));
            else if (n.equals("lucifertotem:chests/morningstar_citadel")) e.getTable().addPool(pool("lt_crops", new Object[][]{{AMBROSIA_SEEDS, 1}, {null, 15}}));
            else if (n.equals("lucifertotem:entities/bacchant") || n.equals("lucifertotem:entities/scythian_huntress")) e.getTable().addPool(pool("lt_crops", new Object[][]{{SILPHIUM_SEEDS, 1}, {BARLEY_SEEDS, 2}, {null, 20}}));
        }
        static LootPool pool(String name, Object[][] es) {
            List<LootEntry> l = new ArrayList<>();
            for (Object[] o : es) {
                if (o[0] == null) l.add(new LootEntryEmpty((Integer) o[1], 0, new LootCondition[0], "empty"));
                else l.add(new LootEntryItem((Item) o[0], (Integer) o[1], 0, new LootFunction[0], new LootCondition[0], ((Item) o[0]).getRegistryName().getPath()));
            }
            return new LootPool(l.toArray(new LootEntry[0]), new LootCondition[0], new RandomValueRange(1), new RandomValueRange(0), name);
        }
        @SubscribeEvent
        public void rightClick(PlayerInteractEvent.RightClickBlock e) {
            if (e.getWorld().isRemote || !on("ambrosia")) return;
            if (bless(e.getWorld(), e.getPos(), e.getEntityPlayer(), e.getItemStack())) { e.setCanceled(true); e.setCancellationResult(EnumActionResult.SUCCESS); }
        }
        /** cows and sheep (and Doctor Ammonius) eat barley as well as wheat */
        @SubscribeEvent
        public void feed(PlayerInteractEvent.EntityInteract e) {
            if (e.getWorld().isRemote) return;
            if (feedBarley(e.getEntityPlayer(), e.getTarget(), e.getItemStack())) { e.setCanceled(true); e.setCancellationResult(EnumActionResult.SUCCESS); }
        }
        /** silphium-seasoned food */
        @SubscribeEvent
        public void ate(LivingEntityUseItemEvent.Finish e) {
            if (e.getEntityLiving().world.isRemote || !(e.getEntityLiving() instanceof EntityPlayer)) return;
            seasoned((EntityPlayer) e.getEntityLiving(), e.getItem());
        }
    }
    public static boolean feedBarley(EntityPlayer p, Entity t, ItemStack s) {
        if (s.getItem() != BARLEY || !(t instanceof EntityCow || t instanceof EntitySheep)) return false;
        EntityAnimal a = (EntityAnimal) t;
        if (a.isChild() || a.getGrowingAge() != 0 || a.isInLove()) return false;
        a.setInLove(p);
        if (!p.capabilities.isCreativeMode) s.shrink(1);
        barleyBreeds++;
        return true;
    }
    public static boolean seasoned(EntityPlayer p, ItemStack s) {
        if (!(s.getItem() instanceof ItemFood) || !s.hasTagCompound() || !s.getTagCompound().getBoolean(SEASONED)) return false;
        p.addPotionEffect(new PotionEffect(MobEffects.SATURATION, 4, 0));
        p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 0));
        seasonedEaten++;
        return true;
    }
    public static boolean medeaFireNear(Entity e) {
        for (BlockPos q : BlockPos.getAllInBoxMutable(new BlockPos(e).add(-1, -1, -1), new BlockPos(e).add(1, 1, 1)))
            if (e.world.getBlockState(q).getBlock() == HBlocks.MEDEA_FIRE) return true;
        return false;
    }
}
