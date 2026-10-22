package lucifertotem.block;

import lucifertotem.LuciferTotem;
import lucifertotem.world.RiftLogic;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.2.1: a purple rift left open by a converted Ammon. Two blocks tall, walk-through, a two-way door to the Lightfall.
 * It never breaks, replaces or pushes other blocks; explosions, pistons and hands can't remove it. Close it by
 * sneak-using the Totem of Lucifer on it (or break it in creative); either half going takes the other with it.
 */
public class BlockPurpleRift extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockLightfallPortal.AXIS;
    public static final PropertyBool UPPER = PropertyBool.create("upper");
    private static final AxisAlignedBB X_AABB = new AxisAlignedBB(0.1, 0.0, 0.375, 0.9, 1.0, 0.625);
    private static final AxisAlignedBB Z_AABB = new AxisAlignedBB(0.375, 0.0, 0.1, 0.625, 1.0, 0.9);

    public BlockPurpleRift() {
        super(Material.PORTAL);
        setRegistryName(LuciferTotem.MODID, "purple_rift");
        setTranslationKey(LuciferTotem.MODID + ".purple_rift");
        setBlockUnbreakable();
        setResistance(6000000f);
        setLightLevel(0.8f);
        setSoundType(SoundType.GLASS);
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X).withProperty(UPPER, false));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AXIS, UPPER); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(AXIS, (m & 1) == 1 ? EnumFacing.Axis.Z : EnumFacing.Axis.X).withProperty(UPPER, (m & 2) != 0); }
    @Override public int getMetaFromState(IBlockState s) { return (s.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0) | (s.getValue(UPPER) ? 2 : 0); }

    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return s.getValue(AXIS) == EnumFacing.Axis.Z ? Z_AABB : X_AABB; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public boolean isPassable(IBlockAccess w, BlockPos p) { return true; }
    @Override public int quantityDropped(Random r) { return 0; }
    @Override public ItemStack getItem(World w, BlockPos p, IBlockState s) { return ItemStack.EMPTY; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
    @Override public EnumPushReaction getPushReaction(IBlockState s) { return EnumPushReaction.BLOCK; }
    @Override public boolean canDropFromExplosion(Explosion e) { return false; }
    @Override public void onBlockExploded(World w, BlockPos p, Explosion e) { }   // explosions never close it
    @Override public boolean canEntityDestroy(IBlockState s, IBlockAccess w, BlockPos p, Entity e) { return false; }   // withers, dragons

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.TRANSLUCENT; }

    /** the other half went (creative break, a command, a closed rift): this half follows, and the registry forgets it */
    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos p, Block nb, BlockPos from) {
        if (w.isRemote) return;
        BlockPos other = s.getValue(UPPER) ? p.down() : p.up();
        if (w.getBlockState(other).getBlock() != this) RiftLogic.close(w, s.getValue(UPPER) ? p.down() : p, null, false);
    }

    @Override
    public void breakBlock(World w, BlockPos p, IBlockState s) {
        super.breakBlock(w, p, s);
        if (!w.isRemote && !RiftLogic.closing) RiftLogic.close(w, s.getValue(UPPER) ? p.down() : p, null, false);
    }

    @Override
    public void onEntityCollision(World w, BlockPos p, IBlockState s, Entity e) {
        if (w.isRemote || !(e instanceof EntityPlayerMP) || e.isRiding() || e.isBeingRidden()) return;
        if (e.timeUntilPortal > 0) return;
        RiftLogic.enter((EntityPlayerMP) e, s.getValue(UPPER) ? p.down() : p);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (!s.getValue(UPPER) && r.nextInt(100) == 0)
            w.playSound(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.35f, r.nextFloat() * 0.3f + 1.5f, false);
        for (int i = 0; i < 4; i++) {
            double a = r.nextDouble() * Math.PI * 2, rr = 0.25 + r.nextDouble() * 0.35;
            w.spawnParticle(i == 0 ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.PORTAL, p.getX() + 0.5 + Math.cos(a) * rr * 0.6, p.getY() + r.nextDouble(), p.getZ() + 0.5 + Math.sin(a) * rr * 0.6,
                (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.4);
        }
    }
}
