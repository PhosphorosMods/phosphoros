package lucifertotem.v132;

import java.util.List;

import lucifertotem.LTConfig;
import lucifertotem.hillman.HItems;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Two-block-tall figures of Dionysus (robed, long-haired, bearded, splashed with purple wine):
 *  - the Icon: a life-sized wall ornament, mounted flat over the Library of Alexandria's entrance (no item of its own);
 *  - the Statue: a free-standing 2-tall statue, which is what the Icon drops. Set on a T of four logs, he comes to life.
 * Both halves are one object: break either and the whole figure comes away (the lower half carries the drop).
 */
public abstract class Tall extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool UPPER = PropertyBool.create("upper");

    Tall() {
        super(Material.WOOD);
        setHardness(1.2f); setResistance(4f);
        setSoundType(net.minecraft.block.SoundType.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(UPPER, false));
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, UPPER); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(m & 3)).withProperty(UPPER, (m & 4) != 0); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex() | (s.getValue(UPPER) ? 4 : 0); }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override @SideOnly(Side.CLIENT) public net.minecraft.util.BlockRenderLayer getRenderLayer() { return net.minecraft.util.BlockRenderLayer.CUTOUT; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    public static BlockPos partner(BlockPos p, IBlockState s) { return s.getValue(UPPER) ? p.down() : p.up(); }
    public static BlockPos lower(BlockPos p, IBlockState s) { return s.getValue(UPPER) ? p.down() : p; }

    /** put both halves down (worldgen, the statue item, tests) */
    public boolean placeBoth(World w, BlockPos lower, EnumFacing f, int flags) {
        IBlockState s = getDefaultState().withProperty(FACING, f);
        w.setBlockState(lower, s.withProperty(UPPER, false), flags);
        w.setBlockState(lower.up(), s.withProperty(UPPER, true), flags);
        return true;
    }

    /** breaking the upper half harvests the lower one (which carries the drop) */
    @Override public void onBlockHarvested(World w, BlockPos p, IBlockState s, EntityPlayer player) {
        if (s.getValue(UPPER) && w.getBlockState(p.down()).getBlock() == this) {
            if (player.capabilities.isCreativeMode) w.setBlockState(p.down(), Blocks.AIR.getDefaultState(), 3);
            else w.destroyBlock(p.down(), true);
        }
        super.onBlockHarvested(w, p, s, player);
    }
    /** ...and the lower half takes the upper one with it, silently */
    @Override public void breakBlock(World w, BlockPos p, IBlockState s) {
        BlockPos q = partner(p, s);
        if (!s.getValue(UPPER) && w.getBlockState(q).getBlock() == this) w.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        super.breakBlock(w, p, s);
    }
    @Override public void neighborChanged(IBlockState s, World w, BlockPos p, Block b, BlockPos from) {
        if (!w.isRemote && w.getBlockState(partner(p, s)).getBlock() != this) w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
    }
    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos p, IBlockState s, int fortune) {
        if (!s.getValue(UPPER)) drops.add(new ItemStack(V132.STATUE));
    }
    @Override public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos p, EntityPlayer pl) { return new ItemStack(V132.STATUE); }
    @Override public int damageDropped(IBlockState s) { return 0; }

    // ------------------------------------------------------------ the Icon: flat on the wall
    public static class Icon extends Tall {
        static final AxisAlignedBB N = new AxisAlignedBB(1 / 16d, 0, 14 / 16d, 15 / 16d, 1, 1), S = new AxisAlignedBB(1 / 16d, 0, 0, 15 / 16d, 1, 2 / 16d),
                                   W = new AxisAlignedBB(14 / 16d, 0, 1 / 16d, 1, 1, 15 / 16d), E = new AxisAlignedBB(0, 0, 1 / 16d, 2 / 16d, 1, 15 / 16d);
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) {
            switch (s.getValue(FACING)) { case SOUTH: return S; case WEST: return W; case EAST: return E; default: return N; }
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { }
    }

    // ------------------------------------------------------------ the Statue: free-standing; the item places both halves
    public static class Statue extends Tall {
        static final AxisAlignedBB BOX = new AxisAlignedBB(3 / 16d, 0, 3 / 16d, 13 / 16d, 1, 13 / 16d);
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public boolean canPlaceBlockAt(World w, BlockPos p) {
            return super.canPlaceBlockAt(w, p) && p.getY() < w.getHeight() - 1 && w.getBlockState(p.up()).getBlock().isReplaceable(w, p.up());
        }
        @Override public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing side, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
            return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(UPPER, false);
        }
        @Override public void onBlockPlacedBy(World w, BlockPos p, IBlockState s, EntityLivingBase placer, ItemStack st) {
            w.setBlockState(p.up(), s.withProperty(UPPER, true), 2);
            if (!w.isRemote && placer instanceof EntityPlayer) Xoanon.tryRaise(w, p, (EntityPlayer) placer);
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(Item.getItemFromBlock(this), tip); }
    }

    /** worldgen: the life-sized Icon on the inner wall over the Library entrance (lower half at p, facing into the hall) */
    public static int hung;
    public static boolean hangIcon(World w, BlockPos lower, EnumFacing facing) {
        if (!LTConfig.v132.dionysus || !LTConfig.v132.libraryIcon || V132.ICON == null) return false;
        V132.ICON.placeBoth(w, lower, facing, 2);
        hung++;
        return true;
    }
}
