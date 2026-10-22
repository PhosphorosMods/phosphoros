package lucifertotem.block;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.world.PortalLogic;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** The shimmering violet membrane of a Lightfall gate. Unbreakable by hand; collapses when its duskstone-brick frame is broken. */
public class BlockLightfallPortal extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
    private static final AxisAlignedBB X_AABB = new AxisAlignedBB(0.0, 0.0, 0.375, 1.0, 1.0, 0.625);
    private static final AxisAlignedBB Z_AABB = new AxisAlignedBB(0.375, 0.0, 0.0, 0.625, 1.0, 1.0);

    public BlockLightfallPortal() {
        super(Material.PORTAL);
        setRegistryName(LuciferTotem.MODID, "lightfall_portal");
        setTranslationKey(LuciferTotem.MODID + ".lightfall_portal");
        setBlockUnbreakable();
        setResistance(6000000f);
        setLightLevel(0.75f);
        setSoundType(SoundType.GLASS);
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AXIS); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(AXIS, (m & 1) == 1 ? EnumFacing.Axis.Z : EnumFacing.Axis.X); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0; }

    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return s.getValue(AXIS) == EnumFacing.Axis.Z ? Z_AABB : X_AABB; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public int quantityDropped(Random r) { return 0; }
    @Override public ItemStack getItem(World w, BlockPos p, IBlockState s) { return ItemStack.EMPTY; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.TRANSLUCENT; }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState s, IBlockAccess w, BlockPos p, EnumFacing side) {
        IBlockState o = w.getBlockState(p.offset(side));
        return o.getBlock() != this && super.shouldSideBeRendered(s, w, p, side);
    }

    private boolean ok(World w, BlockPos p) {
        Block b = w.getBlockState(p).getBlock();
        return b == this || b == ModContent.DUSKSTONE_BRICKS;
    }

    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos p, Block nb, BlockPos from) {
        EnumFacing.Axis a = s.getValue(AXIS);
        EnumFacing side = a == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.SOUTH;
        if (!ok(w, p.up()) || !ok(w, p.down()) || !ok(w, p.offset(side)) || !ok(w, p.offset(side.getOpposite())))
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
    }

    @Override
    public void onEntityCollision(World w, BlockPos p, IBlockState s, Entity e) {
        if (w.isRemote || !(e instanceof EntityPlayerMP) || e.isRiding() || e.isBeingRidden()) return;
        if (e.timeUntilPortal > 0) return;
        PortalLogic.enterPortal((EntityPlayerMP) e, p);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (r.nextInt(120) == 0)
            w.playSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.4f, r.nextFloat() * 0.4f + 1.2f, false);
        for (int i = 0; i < 3; i++)
            w.spawnParticle(i == 0 ? EnumParticleTypes.END_ROD : EnumParticleTypes.PORTAL, p.getX() + r.nextDouble(), p.getY() + r.nextDouble(), p.getZ() + r.nextDouble(),
                (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3);
    }
}
