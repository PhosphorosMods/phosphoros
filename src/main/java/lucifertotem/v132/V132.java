package lucifertotem.v132;

import java.util.List;
import java.util.Random;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HBlocks;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HItems;
import lucifertotem.v130.V130;
import lucifertotem.v130.V130Net;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

/** 1.3.2: Dionysus (the mask, the wooden xoanon that follows you) and the Crown of Thorns. */
public final class V132 {
    private V132() {}

    public static Item CROWN, MASK_ITEM;
    public static BlockMask MASK;
    public static Tall.Icon ICON;
    public static Tall.Statue STATUE;
    public static final ItemArmor.ArmorMaterial THORNS = EnumHelper.addArmorMaterial("LT_THORNS", LuciferTotem.MODID + ":crown_of_thorns", 10, new int[]{1, 1, 1, 1}, 22, SoundEvents.BLOCK_GRASS_PLACE, 0f);

    public static void blocks(IForgeRegistry<Block> r) {
        MASK = V130.block(r, new BlockMask(), "dionysus_mask", true);
        ICON = V130.block(r, new Tall.Icon(), "dionysus_icon", false);
        STATUE = V130.block(r, new Tall.Statue(), "dionysus_statue", true);
    }

    public static void items(IForgeRegistry<Item> r) {
        CROWN = V130.item(r, new ItemCrown(), "crown_of_thorns");
    }

    public static int entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "dionysus"), EntityDionysus.class, "lucifertotem.dionysus", id++, mod, 64, 3, true, 0x8A5A2E, 0x5A1E6E);
        return id;
    }

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Crown.Events());
    }

    static void give(EntityPlayer p, ItemStack s) { if (!p.inventory.addItemStackToInventory(s)) p.dropItem(s, false); }

    // ------------------------------------------------------------ the codex (a written book, one per subject, handed out once)
    public static final String CODEX_TAG = "ltCodex132";
    static final String[] CODEX_DIONYSUS = {
        "THE VINTNER'S CODEX\n~ Dionysus ~\n\nThe god who arrives. He came late to Olympus and early to every party. Born of a god and a mortal woman, died and came back, turned water into wine. Remind you of anyone? It should.",
        "The Icon.\nOver the entrance of the Library hangs a life-sized figure: long hair, a beard, a white robe with a red sash, sandals, and wine splashed all over. Take it down and it comes away as a statue.",
        "To raise him: four logs in a T, the way you would stack iron for a golem, and the statue on top. He steps down and follows whoever set it.",
        "His habits.\nHe follows. Right-click him with an empty hand and he sits and waits.\nHe fights what hunts you. He will not raise a hand to a person.\nHand him Bacchic Wine and he mends.\nHand him a bottle of water...",
        "...and watch.\n\nThe tradition says the water in his temples turned to wine overnight. Here it is quicker. He is a good host and a terrible influence.\n\n        - A."
    };
    static final String[] CODEX_CROWN = {
        "THE VINTNER'S CODEX\n~ The Crown of Thorns ~\n\nA circlet of dried vine and poppy stems, woven tight and left to harden. The Lightfall knows the smell of it.",
        "Wear it (helmet, or sneak + use to wear it over your helmet) in the Lightfall and its creatures let you pass: Bacchants, Huntresses, Hounds, the Peer Reviewers, the medusae, even the Hydra. Strike one and that one remembers.",
        "Not the Dipsas.\nThe little thirst-snake does not care what you wear. Its bite, on a crowned head, goes straight to PURPLE.\n\nWhere to find it: the wooden masks on the Bacchic fountains sometimes hold one. Or weave it: vines, Dusk Poppies, Bacchic Wine.",
        "To take a worn crown back off: sneak and right-click a block with an empty hand.\n\nOutside the Lightfall it is just a hat with opinions.\n\n        - A."
    };

    public static ItemStack codex(String kind) {
        String[] pages = "crown".equals(kind) ? CODEX_CROWN : CODEX_DIONYSUS;
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound t = new NBTTagCompound();
        t.setString("title", "crown".equals(kind) ? "Codex: Crown of Thorns" : "Codex: Dionysus");
        t.setString("author", "Dr. Ammonius");
        t.setInteger("generation", 0);
        t.setBoolean("resolved", true);
        NBTTagList l = new NBTTagList();
        for (String p : pages) { ITextComponent c = new TextComponentString(p); l.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(c))); }
        t.setTag("pages", l);
        t.setString(CODEX_TAG, kind);
        s.setTagCompound(t);
        return s;
    }

    /** hand out a codex entry once per player */
    public static boolean codexOnce(EntityPlayer p, String kind) {
        NBTTagCompound d = HData.get(p);
        String k = "codex132_" + kind;
        if (d.getBoolean(k)) return false;
        d.setBoolean(k, true);
        give(p, codex(kind));
        return true;
    }

    // ------------------------------------------------------------ the Crown of Thorns
    public static class ItemCrown extends ItemArmor {
        public ItemCrown() { super(THORNS, 0, EntityEquipmentSlot.HEAD); }
        @Override public String getArmorTexture(ItemStack s, net.minecraft.entity.Entity e, EntityEquipmentSlot slot, String type) { return LuciferTotem.MODID + ":textures/models/armor/crown_of_thorns.png"; }
        @Override @SideOnly(Side.CLIENT)
        public net.minecraft.client.model.ModelBiped getArmorModel(EntityLivingBase e, ItemStack s, EntityEquipmentSlot slot, net.minecraft.client.model.ModelBiped def) {
            return lucifertotem.client.V132Client.crownArmor();
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(this, tip); }
        /** sneak + use: wear it as a cosmetic, over whatever helmet you have on */
        @Override public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            ItemStack s = p.getHeldItem(h);
            if (p.isSneaking()) {
                if (!w.isRemote) wearCosmetic(p, s);
                return new ActionResult<>(EnumActionResult.SUCCESS, s);
            }
            return super.onItemRightClick(w, p, h);
        }
    }

    public static boolean wearCosmetic(EntityPlayer p, ItemStack s) {
        if (HData.get(p).getBoolean(Crown.COSMETIC)) return false;
        HData.get(p).setBoolean(Crown.COSMETIC, true);
        if (!p.capabilities.isCreativeMode) s.shrink(1);
        V130Net.sync(p);
        HBlocks.msg(p, "msg.lucifertotem.crown_on");
        return true;
    }

    public static boolean takeOffCosmetic(EntityPlayer p) {
        if (!HData.get(p).getBoolean(Crown.COSMETIC)) return false;
        HData.get(p).setBoolean(Crown.COSMETIC, false);
        give(p, new ItemStack(CROWN));
        V130Net.sync(p);
        HBlocks.msg(p, "msg.lucifertotem.crown_off");
        return true;
    }

    // ------------------------------------------------------------ the mask: a wall ornament
    public static class BlockMask extends Block {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;
        /** found on a fountain (may hold a crown), as opposed to one a player hung */
        public static final PropertyBool WILD = PropertyBool.create("wild");
        static final AxisAlignedBB N = new AxisAlignedBB(2 / 16d, 1 / 16d, 13 / 16d, 14 / 16d, 15 / 16d, 1d);
        static final AxisAlignedBB S = new AxisAlignedBB(2 / 16d, 1 / 16d, 0d, 14 / 16d, 15 / 16d, 3 / 16d);
        static final AxisAlignedBB W = new AxisAlignedBB(13 / 16d, 1 / 16d, 2 / 16d, 1d, 15 / 16d, 14 / 16d);
        static final AxisAlignedBB E = new AxisAlignedBB(0d, 1 / 16d, 2 / 16d, 3 / 16d, 15 / 16d, 14 / 16d);

        public BlockMask() {
            super(Material.WOOD);
            setHardness(1.0f);
            setResistance(3f);
            setSoundType(net.minecraft.block.SoundType.WOOD);
            setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(WILD, false));
        }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, WILD); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(m & 3)).withProperty(WILD, (m & 4) != 0); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex() | (s.getValue(WILD) ? 4 : 0); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) {
            switch (s.getValue(FACING)) { case SOUTH: return S; case WEST: return W; case EAST: return E; default: return N; }
        }
        /** the block a mask hangs on (behind it), or for a mask on top of a log pole, the block under it */
        static BlockPos support(BlockPos p, IBlockState s) { return p.offset(s.getValue(FACING).getOpposite()); }
        static boolean held(World w, BlockPos p, IBlockState s) {
            BlockPos b = support(p, s);
            if (w.getBlockState(b).getBlockFaceShape(w, b, s.getValue(FACING)) == BlockFaceShape.SOLID) return true;
            return w.getBlockState(p.down()).getBlockFaceShape(w, p.down(), EnumFacing.UP) == BlockFaceShape.SOLID;
        }
        @Override public boolean canPlaceBlockAt(World w, BlockPos p) {
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) if (w.isSideSolid(p.offset(f), f.getOpposite())) return super.canPlaceBlockAt(w, p);
            return super.canPlaceBlockAt(w, p) && w.isSideSolid(p.down(), EnumFacing.UP);
        }
        @Override public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing side, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
            EnumFacing f = side.getAxis().isHorizontal() ? side : placer.getHorizontalFacing().getOpposite();
            return getDefaultState().withProperty(FACING, f);
        }
        @Override public void neighborChanged(IBlockState s, World w, BlockPos p, Block b, BlockPos from) {
            if (!w.isRemote && !held(w, p, s)) { dropBlockAsItem(w, p, s, 0); w.setBlockToAir(p); }
        }
        @Override public int damageDropped(IBlockState s) { return 0; }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(Item.getItemFromBlock(this), tip); }
        @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos p, IBlockState s, int fortune) {
            drops.add(new ItemStack(this));
            Random r = w instanceof World ? ((World) w).rand : RANDOM;
            if (s.getValue(WILD) && wildCrown(r)) drops.add(new ItemStack(CROWN));
        }
    }

    public static int wildCrownRolls, wildCrownHits;
    public static boolean wildCrown(Random r) {
        int n = LTConfig.v132.crownFromMaskOneIn;
        wildCrownRolls++;
        boolean hit = LTConfig.v132.crownOfThorns && n > 0 && r.nextInt(n) == 0;
        if (hit) wildCrownHits++;
        return hit;
    }

    static boolean isLog(World w, BlockPos p) { IBlockState s = w.getBlockState(p); return s.getBlock() == Blocks.LOG || s.getBlock() == Blocks.LOG2 || s.getBlock().isWood(w, p); }
}
