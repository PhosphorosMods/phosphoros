package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HBlocks;
import lucifertotem.hillman.HItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;
import java.util.Random;

/** Stage 3 "Wild Things": Bacchant packs, Scythian Huntresses, Hekate's Crossroads Shrine and her hounds. */
public final class Stage3 {
    private Stage3() {}
    public static Item THYRSUS, IVY_CROWN, BACCHIC_WINE, TOXICON_ARROW, SCYTHIAN_BOW, KEY_OF_HEKATE;
    public static Block HEKATAION, HEKATE_GATE;
    public static final ItemArmor.ArmorMaterial IVY = EnumHelper.addArmorMaterial("LT_IVY", LuciferTotem.MODID + ":ivy_crown", 8, new int[]{1, 1, 1, 1}, 18, SoundEvents.BLOCK_GRASS_PLACE, 0f);
    public static final Item.ToolMaterial FENNEL = EnumHelper.addToolMaterial("LT_THYRSUS", 0, 120, 2f, 1.5f, 18);
    public static final ResourceLocation LOOT_BACCHANT = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "entities/bacchant"));
    public static final ResourceLocation LOOT_HUNTRESS = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "entities/scythian_huntress"));
    public static final ResourceLocation LOOT_FOUNTAIN = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "chests/bacchic_fountain"));
    public static final ResourceLocation LOOT_HEKATE = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "chests/hekate_vault"));

    public static void items(IForgeRegistry<Item> r) {
        THYRSUS = V130.item(r, new ItemThyrsus(), "thyrsus");
        IVY_CROWN = V130.item(r, new ItemIvyCrown(), "ivy_crown");
        BACCHIC_WINE = V130.item(r, new HItems.ItemDrink(false, p -> {
            p.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 1200, 0));
            p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
            HBlocks.msg(p, "msg.lucifertotem.bacchic_wine");
        }), "bacchic_wine");
        TOXICON_ARROW = V130.item(r, new ItemToxiconArrow(), "toxicon_arrow");
        SCYTHIAN_BOW = V130.item(r, new ItemScythianBow(), "scythian_bow");
        KEY_OF_HEKATE = V130.item(r, new HItems.ItemTip(true), "key_of_hekate");
        KEY_OF_HEKATE.setMaxStackSize(1);
    }
    public static void blocks(IForgeRegistry<Block> r) {
        HEKATAION = V130.block(r, new BlockHekataion(), "hekataion", true);
        HEKATE_GATE = V130.block(r, new BlockHekateGate(), "hekate_gate", true);
    }
    public static int entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "bacchant"), EntityBacchant.class, "lucifertotem.bacchant", id++, mod, 80, 3, true, 0x6A1B4D, 0x4E8A3A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "scythian_huntress"), EntityHuntress.class, "lucifertotem.scythian_huntress", id++, mod, 80, 3, true, 0x8A3A1A, 0xD8B060);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "toxicon_arrow"), EntityToxiconArrow.class, "lucifertotem.toxicon_arrow", id++, mod, 64, 20, true);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "hekate_hound"), EntityHekateHound.class, "lucifertotem.hekate_hound", id++, mod, 80, 3, true, 0x101014, 0xE02020);
        net.minecraft.entity.EntitySpawnPlacementRegistry.setPlacementType(EntityHuntress.class, net.minecraft.entity.EntityLiving.SpawnPlacementType.ON_GROUND);
        return id;
    }
    public static void init() { net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events()); }
    public static void tiles() {}

    // ------------------------------------------------------------ Bacchic gear
    public static class ItemThyrsus extends ItemSword {
        public ItemThyrsus() { super(FENNEL); }
        @Override public boolean hitEntity(ItemStack s, EntityLivingBase target, EntityLivingBase attacker) {
            if (!target.world.isRemote && attacker.getRNG().nextInt(4) == 0) target.addPotionEffect(new PotionEffect(V130.MANIA, 100, 0));
            return super.hitEntity(s, target, attacker);
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(this, tip); }
    }
    public static class ItemIvyCrown extends ItemArmor {
        public ItemIvyCrown() { super(IVY, 0, EntityEquipmentSlot.HEAD); }
        @Override public String getArmorTexture(ItemStack s, net.minecraft.entity.Entity e, EntityEquipmentSlot slot, String type) { return LuciferTotem.MODID + ":textures/models/armor/ivy_crown_layer_1.png"; }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(this, tip); }
    }

    /** Toxicon: Slowness IV for 2 s, then 5 s of Mania (the Scythians' arrow poison; Herodotus 4.9, the "ios") */
    public static void toxicon(EntityLivingBase e) {
        e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 3));
        e.getEntityData().setLong("lt_mania_at", e.world.getTotalWorldTime() + 40);
        toxiconHits++;
        if (e instanceof EntityPlayer) HAdv.grant((EntityPlayer) e, "toxicon");
    }
    public static int toxiconHits = 0, maniaStarts = 0;

    public static class ItemToxiconArrow extends ItemArrow {
        @Override public EntityArrow createArrow(World w, ItemStack s, EntityLivingBase shooter) { return new EntityToxiconArrow(w, shooter); }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(this, tip); }
    }

    /** draws half again as fast as a vanilla bow */
    public static class ItemScythianBow extends ItemBow {
        public ItemScythianBow() { setMaxDamage(500); }
        @Override public void onPlayerStoppedUsing(ItemStack s, World w, EntityLivingBase e, int timeLeft) {
            int used = getMaxItemUseDuration(s) - timeLeft;
            super.onPlayerStoppedUsing(s, w, e, getMaxItemUseDuration(s) - (int) Math.ceil(used * 1.5));
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(this, tip); }
    }

    // ------------------------------------------------------------ Hekate
    /** Hekate's hour: the middle of the counted night (the Lightfall sky never moves, but the days are still counted) */
    public static long clockShift = 0;   // self-test only
    /** the Lightfall's sky never moves, so Hekate keeps the counted days: total world time */
    public static long clock(World w) { return w.getTotalWorldTime() + clockShift; }
    public static boolean hekatesHour(World w) { long t = clock(w) % 24000L; return t >= 17000 && t < 19500; }
    public static final String[] OFFERINGS = {"honey_cake", "egg", "fish"};
    public static int offeringIndex(World w) { return (int) ((clock(w) / 24000L) % OFFERINGS.length); }
    public static boolean isOffering(ItemStack s, int idx) {
        Item it = s.getItem();
        switch (OFFERINGS[idx]) {
            case "honey_cake": return it == HItems.HONEY_CAKE;
            case "egg": return it == Items.EGG;
            default: return it == Items.FISH || it == Items.COOKED_FISH;
        }
    }
    public static int keysGiven = 0, houndPacks = 0;

    /** the three-faced torch statue at a crossroads */
    public static class BlockHekataion extends Block {
        static final AxisAlignedBB BOX = new AxisAlignedBB(0.2, 0, 0.2, 0.8, 1.0, 0.8);
        public BlockHekataion() { super(Material.ROCK); setHardness(3f); setResistance(30f); setSoundType(SoundType.STONE); setLightLevel(0.8f); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
        @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (!w.isRemote && hand == EnumHand.MAIN_HAND) offer(w, pos, p, p.getHeldItem(hand));
            return true;
        }
        @Override @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
            for (int i = 0; i < 3; i++) {
                double a = i * Math.PI * 2 / 3;
                w.spawnParticle(net.minecraft.util.EnumParticleTypes.FLAME, p.getX() + 0.5 + Math.cos(a) * 0.35, p.getY() + 1.05, p.getZ() + 0.5 + Math.sin(a) * 0.35, 0, 0.01, 0);
            }
        }
    }

    /** an offering at the Hekataion. returns "hint", "key", "hounds" */
    public static String offer(World w, BlockPos pos, EntityPlayer p, ItemStack held) {
        int idx = offeringIndex(w);
        if (held.isEmpty()) {
            long t = clock(w) % 24000L;
            long wait = t < 17000 ? 17000 - t : t >= 19500 ? 24000 - t + 17000 : 0;
            HBlocks.msg(p, "msg.lucifertotem.hekate.hint." + OFFERINGS[idx]);
            if (wait > 0) HBlocks.msg(p, "msg.lucifertotem.hekate.wait", (wait + 1199) / 1200);
            else HBlocks.msg(p, "msg.lucifertotem.hekate.now");
            return "hint";
        }
        ItemStack given = held.copy();
        if (!p.capabilities.isCreativeMode) held.shrink(1);
        if (hekatesHour(w) && isOffering(given, idx)) {
            ItemStack key = new ItemStack(KEY_OF_HEKATE);
            if (!p.inventory.addItemStackToInventory(key)) p.dropItem(key, false);
            w.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1f, 0.6f);
            HBlocks.msg(p, "msg.lucifertotem.hekate.key");
            keysGiven++;
            HAdv.grant(p, "key_of_hekate");
            return "key";
        }
        houndPacks++;
        HBlocks.msg(p, "msg.lucifertotem.hekate.hounds");
        for (int i = 0; i < 3; i++) {
            EntityHekateHound h = new EntityHekateHound(w);
            double a = i * Math.PI * 2 / 3;
            h.setLocationAndAngles(pos.getX() + 0.5 + Math.cos(a) * 3, pos.getY(), pos.getZ() + 0.5 + Math.sin(a) * 3, w.rand.nextFloat() * 360, 0);
            h.setAttackTarget(p);
            w.spawnEntity(h);
        }
        w.playSound(null, pos, V130.HOUND_HOWL, SoundCategory.HOSTILE, 1.5f, 0.7f);
        HAdv.grant(p, "hekates_hounds");
        return "hounds";
    }

    /** a black iron gate with a three-way keyhole: the Key of Hekate opens it (and its neighbours) for ten seconds */
    public static class BlockHekateGate extends Block {
        public static final PropertyBool OPEN = PropertyBool.create("open");
        public BlockHekateGate() { super(Material.IRON); setBlockUnbreakable(); setResistance(6000000f); setSoundType(SoundType.METAL); setDefaultState(blockState.getBaseState().withProperty(OPEN, false)); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, OPEN); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(OPEN) ? 1 : 0; }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(OPEN, (m & 1) == 1); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return s.getValue(OPEN) ? NULL_AABB : FULL_BLOCK_AABB; }
        @Override public boolean isPassable(IBlockAccess w, BlockPos p) { return w.getBlockState(p).getValue(OPEN); }
        @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (w.isRemote) return true;
            if (p.getHeldItem(hand).getItem() != KEY_OF_HEKATE) { if (!s.getValue(OPEN)) HBlocks.msg(p, "msg.lucifertotem.hekate.locked"); return true; }
            int n = open(w, pos);
            if (n > 0) HAdv.grant(p, "unlocked");
            return true;
        }
        @Override public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
            if (s.getValue(OPEN)) { w.setBlockState(pos, s.withProperty(OPEN, false), 3); w.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1f, 0.6f); }
        }
    }
    public static int gatesOpened = 0;
    /** opens a connected gate (up to 16 blocks) for 200 ticks */
    public static int open(World w, BlockPos start) {
        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>(); java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        q.add(start); int n = 0;
        while (!q.isEmpty() && n < 16) {
            BlockPos p = q.poll();
            if (!seen.add(p)) continue;
            IBlockState s = w.getBlockState(p);
            if (s.getBlock() != HEKATE_GATE) continue;
            if (!s.getValue(BlockHekateGate.OPEN)) w.setBlockState(p, s.withProperty(BlockHekateGate.OPEN, true), 3);
            w.scheduleUpdate(p, HEKATE_GATE, 200);
            n++;
            for (EnumFacing f : EnumFacing.values()) q.add(p.offset(f));
        }
        if (n > 0) { gatesOpened++; w.playSound(null, start, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1f, 0.6f); }
        return n;
    }

    // ------------------------------------------------------------ events: Mania (and its cure: the Ivy Crown), Toxicon's delayed Mania
    public static class Events {
        @SubscribeEvent
        public void living(LivingEvent.LivingUpdateEvent e) {
            EntityLivingBase l = e.getEntityLiving();
            if (l.world.isRemote) return;
            long at = l.getEntityData().getLong("lt_mania_at");
            if (at > 0 && l.world.getTotalWorldTime() >= at) {
                l.getEntityData().removeTag("lt_mania_at");
                l.addPotionEffect(new PotionEffect(V130.MANIA, 100, 0));
                maniaStarts++;
            }
            if (l.isPotionActive(V130.MANIA) && l.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == IVY_CROWN) {
                l.removePotionEffect(V130.MANIA);
                if (l instanceof EntityPlayer) HAdv.grant((EntityPlayer) l, "ivy_crown");
            }
        }
    }
}
