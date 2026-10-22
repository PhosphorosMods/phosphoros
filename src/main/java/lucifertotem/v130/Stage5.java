package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.hillman.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Random;

/** Stage 5 "Monsters & Mazes": the Drakaina, the Hydra of the Sunken Chapel, the Labyrinth of the Lady, the Asklepieion */
public final class Stage5 {
    private Stage5() {}
    public static Item DRAKAINA_SCALE, HYDRA_VENOM, RED_THREAD;
    public static Block KLINE;
    static ResourceLocation rl(String p) { return LootTableList.register(new ResourceLocation(LuciferTotem.MODID, p)); }
    public static final ResourceLocation LOOT_DRAKAINA = rl("entities/drakaina"), LOOT_HYDRA = rl("entities/hydra"),
            LOOT_LAIR = rl("chests/drakaina_lair"), LOOT_LABYRINTH = rl("chests/labyrinth"), LOOT_LAB_ENTRANCE = rl("chests/labyrinth_entrance"),
            LOOT_ASKLEPIEION = rl("chests/asklepieion");
    public static int lairs = 0, chapels = 0, incubations = 0;
    public static long dayShift = 0;   // test hook

    public static void items(IForgeRegistry<Item> r) {
        DRAKAINA_SCALE = V130.item(r, new HItems.ItemTip(false), "drakaina_scale");
        HYDRA_VENOM = V130.item(r, new HItems.ItemTip(true), "hydra_venom");
        RED_THREAD = V130.item(r, new ItemRedThread(), "red_thread");
    }
    public static void blocks(IForgeRegistry<Block> r) { KLINE = V130.block(r, new BlockKline(), "kline", true); }
    public static int entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "drakaina"), EntityDrakaina.class, "lucifertotem.drakaina", id++, mod, 80, 3, true, 0x3A6A2A, 0x5A2A7A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "hydra"), EntityHydra.class, "lucifertotem.hydra", id++, mod, 80, 3, true, 0x2A5A4A, 0x8ACA6A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "venom_spit"), EntityVenomSpit.class, "lucifertotem.venom_spit", id++, mod, 64, 10, true);
        return id;
    }
    public static void tiles() {}
    public static void init() {
        Apothecary.CODE.put("lucifertotem:drakaina_scale", "drakaina");
        Apothecary.CODE.put("lucifertotem:hydra_venom", "hydra");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events());
    }

    // ================================================================ the Drakaina's lair under a new Delphic vent
    static long hash(int x, int y, int z) { long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L; h ^= h >>> 17; h *= 0x9E3779B97F4A7C15L; return h ^ (h >>> 29); }
    /** called by LightfallStructures.delphic() after the shrine is built; vent at (c, y, d) */
    public static boolean lair(World w, int bx, int bz, int c, int y, int d) {
        if (!LTConfig.v130.drakaina) return false;
        Gen130 g = new Gen130(w, new Random(hash(c, y, d) ^ 0xD4A1L), bx, bz);
        int ly = y - 11;
        if (ly < 20) return false;
        for (int k = 1; k <= 16; k++) if (w.isAirBlock(new BlockPos(c, y - k, d))) return false;   // island too thin
        IBlockState B = g.BRICK(), A = Gen130.AIR;
        g.box(c - 4, ly - 1, d - 4, c + 4, ly + 5, d + 4, B, A);
        for (int x = c - 3; x <= c + 3; x++) for (int z = d - 3; z <= d + 3; z++) if (((x * 7 + z * 3) & 3) == 0) g.set(x, ly - 1, z, Blocks.MOSSY_COBBLESTONE.getDefaultState());
        g.set(c, ly - 1, d, Blocks.MAGMA.getDefaultState());                // the vent's heat comes from here
        g.set(c - 3, ly, d - 3, Blocks.BONE_BLOCK.getDefaultState()); g.set(c + 3, ly, d + 3, Blocks.BONE_BLOCK.getDefaultState());
        g.set(c - 3, ly + 4, d + 3, Blocks.WEB.getDefaultState()); g.set(c + 3, ly + 4, d - 1, Blocks.WEB.getDefaultState());
        g.chest(c - 3, ly, d, EnumFacing.EAST, LOOT_LAIR);
        g.torch(c + 3, ly + 2, d, EnumFacing.WEST); g.torch(c - 3, ly + 2, d - 2, EnumFacing.EAST);
        // a ladder shaft from under a trapdoor in the shrine floor, down the north wall of the lair
        int sx = c + 2, sz = d - 3;
        for (int yy = ly + 5; yy <= y - 1; yy++) { g.set(sx - 1, yy, sz, B); g.set(sx + 1, yy, sz, B); g.set(sx, yy, sz + 1, B); g.set(sx, yy, sz - 1, B); }
        g.ladder(sx, ly, y - 1, sz, EnumFacing.SOUTH);
        g.set(sx, y, sz, Blocks.TRAPDOOR.getDefaultState().withProperty(net.minecraft.block.BlockTrapDoor.HALF, net.minecraft.block.BlockTrapDoor.DoorHalf.TOP)
                .withProperty(net.minecraft.block.BlockTrapDoor.FACING, EnumFacing.SOUTH));
        g.painting(c - 1, ly + 2, d + 3, EnumFacing.NORTH, "aletheia");
        EntityDrakaina dr = g.mob(EntityDrakaina::new, c + 0.5, ly, d + 1.5);
        if (dr != null) dr.setHomePosAndDistance(new BlockPos(c, ly, d), 5);
        StructureIndex.record(w, "drakaina_lair", new BlockPos(c, ly, d));
        lairs++;
        return true;
    }

    // ================================================================ the Sunken Chapel (a flooded sunken shrine with the Hydra)
    /** called by LightfallStructures.crypt(): room interior x c-3..c+3, z d-3..d+3, floor fy */
    public static boolean sunkenChapel(World w, int bx, int bz, int c, int fy, int d) {
        long h = hash(c, fy, d);
        if (Math.floorMod(h, 100L) >= LTConfig.v130.hydraPercent) return false;
        Gen130 g = new Gen130(w, new Random(h ^ 0x4D7AL), bx, bz);
        for (int x = c - 3; x <= c + 3; x++) for (int z = d - 3; z <= d + 3; z++) {
            if (((x + z) & 1) == 0 && g.get(x, fy, z).getBlock() != Blocks.PURPUR_BLOCK) g.set(x, fy, z, Blocks.PRISMARINE.getStateFromMeta(2));
            if (g.in(x, fy + 1, z) && w.isAirBlock(new BlockPos(x, fy + 1, z))) g.set(x, fy + 1, z, Blocks.WATER.getDefaultState());
        }
        g.set(c, fy + 1, d, Blocks.PRISMARINE.getStateFromMeta(1));        // the spawner becomes an altar
        g.set(c, fy + 2, d, Blocks.SEA_LANTERN.getDefaultState());
        boolean hung = g.painting(c - 3, fy + 3, d - 1, EnumFacing.EAST, "hydra") || g.painting(c - 3, fy + 3, d + 1, EnumFacing.EAST, "hydra")
                || g.painting(c, fy + 3, d - 3, EnumFacing.SOUTH, "hydra");
        EntityHydra hy = g.mob(EntityHydra::new, c + 0.5, fy + 1, d - 1.5);
        if (hy == null) return false;
        hy.setHomePosAndDistance(new BlockPos(c, fy + 1, d), 4);
        StructureIndex.record(w, "sunken_chapel", new BlockPos(c, fy + 1, d));
        chapels++;
        return true;
    }

    // ================================================================ the Asklepieion and incubation on the kline
    public static boolean asklepieion(Gen130 g) {
        int cx = g.bx + 8, cz = g.bz + 8;
        int y = g.site(cx, cz, 6, 2);
        if (y < 0) return false;
        int x0 = cx - 6, x1 = cx + 6, z0 = cz - 5, z1 = cz + 5;
        IBlockState Q = Gen130.QUARTZ(), SLAB = Blocks.STONE_SLAB.getStateFromMeta(7), A = Gen130.AIR;
        g.foundation(x0, z0, x1, z1, y, 6);
        g.fill(x0, y, z0, x1, y, z1, Q);
        g.fill(x0, y + 1, z0, x1, y + 8, z1, A);
        // the cella
        g.box(cx - 4, y, cz - 3, cx + 4, y + 5, cz + 3, Q, A);
        g.fill(cx - 3, y, cz - 2, cx + 3, y, cz + 2, Blocks.QUARTZ_BLOCK.getStateFromMeta(1));
        g.fill(cx - 1, y + 1, cz + 3, cx + 1, y + 3, cz + 3, A);           // door (south)
        for (int z : new int[]{cz - 1, cz + 1}) { g.set(cx - 4, y + 3, z, Blocks.GLASS_PANE.getDefaultState()); g.set(cx + 4, y + 3, z, Blocks.GLASS_PANE.getDefaultState()); }
        // the peristyle
        for (int x = x0; x <= x1; x += 2) for (int z : new int[]{z0, z1}) g.fill(x, y + 1, z, x, y + 4, z, Gen130.QPILLAR());
        for (int z = z0 + 2; z <= z1 - 2; z += 2) for (int x : new int[]{x0, x1}) g.fill(x, y + 1, z, x, y + 4, z, Gen130.QPILLAR());
        g.fill(x0, y + 5, z0, x1, y + 5, z1, Q);
        g.fill(x0 + 1, y + 6, z0 + 1, x1 - 1, y + 6, z1 - 1, SLAB);
        g.fill(x0 + 2, y + 6, cz - 1, x1 - 2, y + 6, cz + 1, Q);
        g.fill(x0 + 2, y + 7, cz - 1, x1 - 2, y + 7, cz + 1, SLAB);
        // the sacred spring (a pool sunk in the floor), the healer's painting over it, the couches of the enkoimeterion
        g.fill(cx - 1, y, cz - 2, cx + 1, y, cz - 2, Blocks.WATER.getDefaultState());
        g.painting(cx, y + 2, cz - 2, EnumFacing.SOUTH, "asklepios");
        for (int z : new int[]{cz - 1, cz + 1}) {
            g.set(cx - 3, y + 1, z, KLINE.getDefaultState().withProperty(BlockKline.FACING, EnumFacing.EAST));
            g.set(cx + 3, y + 1, z, KLINE.getDefaultState().withProperty(BlockKline.FACING, EnumFacing.WEST));
        }
        g.chest(cx + 3, y + 1, cz + 2, EnumFacing.WEST, LOOT_ASKLEPIEION);
        g.torch(cx - 3, y + 3, cz - 2, EnumFacing.EAST); g.torch(cx + 3, y + 3, cz - 2, EnumFacing.WEST);
        g.set(cx - 3, y + 1, cz + 2, Blocks.FLOWER_POT.getDefaultState());
        g.at = new BlockPos(cx, y + 1, cz);
        return true;
    }

    public static final int DREAMS = 6;
    /** "healed" / "rested" */
    public static String incubate(World w, BlockPos pos, EntityPlayer p) {
        long day = (w.getTotalWorldTime() + dayShift) / 24000L;
        NBTTagCompound d = HData.get(p);
        if (d.hasKey("asklepiosDay") && d.getLong("asklepiosDay") == day) { HBlocks.msg(p, "msg.lucifertotem.kline.rested"); return "rested"; }
        d.setLong("asklepiosDay", day);
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) if (e.getPotion().isBadEffect()) p.removePotionEffect(e.getPotion());
        p.removePotionEffect(HPotions.INSANIA);
        if (V130.MANIA != null) p.removePotionEffect(V130.MANIA);
        if (V130.SOMNUS != null) p.removePotionEffect(V130.SOMNUS);
        if (V130.STINK != null) p.removePotionEffect(V130.STINK);
        p.setHealth(p.getMaxHealth());
        p.getFoodStats().addStats(6, 0.6f);
        p.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 2400, 1));
        p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, 0));
        p.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 50, 0));
        w.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_NOTE_HARP, SoundCategory.BLOCKS, 1f, 0.5f);
        TextComponentTranslation dream = new TextComponentTranslation("msg.lucifertotem.kline.dream." + (1 + w.rand.nextInt(DREAMS)));
        dream.getStyle().setColor(TextFormatting.DARK_AQUA).setItalic(true);
        p.sendMessage(dream);
        HBlocks.msg(p, "msg.lucifertotem.kline.healed");
        incubations++;
        HAdv.grant(p, "asklepieion");
        return "healed";
    }
    public static class BlockKline extends Block {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;
        static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 0.5625, 1);
        public BlockKline() { super(Material.WOOD); setHardness(1.5f); setSoundType(SoundType.WOOD); setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex(); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(m & 3)); }
        @Override public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float hx, float hy, float hz, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return f == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
        @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (!w.isRemote && hand == EnumHand.MAIN_HAND) incubate(w, pos, p);
            return true;
        }
    }

    // ================================================================ the Red Thread
    public static class ItemRedThread extends HItems.ItemTip {
        public ItemRedThread() { super(false); setMaxStackSize(1); setMaxDamage(64); }
        @Override public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
            ItemStack s = p.getHeldItem(hand);
            if (!w.isRemote) { Labyrinth.guide(p); s.damageItem(1, p); p.getCooldownTracker().setCooldown(this, 20); }
            return new ActionResult<>(EnumActionResult.SUCCESS, s);
        }
    }

    // ================================================================ events: reaching the heart of a labyrinth
    public static class Events {
        @SubscribeEvent
        public void tick(TickEvent.PlayerTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || e.player.ticksExisted % 40 != 11) return;
            if (e.player.world.provider.getDimension() != LuciferTotem.dimId) return;
            Labyrinth.checkHeart(e.player);
        }
    }
}
