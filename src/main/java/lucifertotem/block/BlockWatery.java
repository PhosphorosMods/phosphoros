package lucifertotem.block;

import lucifertotem.LuciferTotem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12 "waterlogged" sea decor, done the way Biomes O' Plenty does coral/seaweed: the block's material IS water and it
 * carries BlockLiquid.LEVEL (always 0 = a still source), so vanilla treats the cell as water: water renders around it,
 * entities swim and drown in it, water never flows into it, light dims like water (opacity 3).
 * The LEVEL property is never written to metadata (meta stays 0), so blocks from older saves load unchanged and simply
 * become the watery version. It can only be placed into still water and only stays while supported and wet;
 * when it goes, the cell turns back into water (never an air pocket).
 */
public abstract class BlockWatery extends Block {
    public static final PropertyInteger LEVEL = BlockLiquid.LEVEL;
    protected final AxisAlignedBB box;

    protected BlockWatery(String name, SoundType sound, float hardness, float light, AxisAlignedBB box) {
        super(Material.WATER);
        this.box = box;
        setDefaultState(blockState.getBaseState().withProperty(LEVEL, 0));
        setSoundType(sound);
        setHardness(hardness);
        setLightLevel(light);
        setLightOpacity(3);
        setRegistryName(LuciferTotem.MODID, name);
        setTranslationKey(LuciferTotem.MODID + "." + name);
        setCreativeTab(LuciferTotem.TAB);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, LEVEL); }
    @Override public int getMetaFromState(IBlockState s) { return 0; }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState(); }

    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return box; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
    @Override public boolean isPassable(IBlockAccess w, BlockPos p) { return true; }
    /** not replaceable like plain water: placing a block into it must not silently delete it */
    @Override public boolean isReplaceable(IBlockAccess w, BlockPos p) { return false; }
    /** still targetable/breakable with the crosshair even though its material is a liquid */
    @Override public boolean canCollideCheck(IBlockState s, boolean hitIfLiquid) { return true; }
    /** heads inside it are under water (drowning, water fog) */
    @Override public Boolean isEntityInsideMaterial(IBlockAccess w, BlockPos p, IBlockState s, Entity e, double y, Material m, boolean head) {
        return m == Material.WATER ? Boolean.TRUE : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    /** what holds it up (sea floor, a stalk below, an island underside above...) */
    protected abstract boolean supported(World w, BlockPos p);

    public static boolean isWater(IBlockState s) { return s.getMaterial() == Material.WATER; }
    public static boolean isStillWater(IBlockState s) {
        Block b = s.getBlock();
        return (b == Blocks.WATER || b == Blocks.FLOWING_WATER) && s.getValue(BlockLiquid.LEVEL) == 0;
    }
    /** water (or other watery decor) above or beside it */
    public static boolean wetAround(World w, BlockPos p) {
        if (isWater(w.getBlockState(p.up()))) return true;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) if (isWater(w.getBlockState(p.offset(f)))) return true;
        return false;
    }

    /** only into still water, supported, with water above or around */
    @Override
    public boolean canPlaceBlockAt(World w, BlockPos p) { return isStillWater(w.getBlockState(p)) && supported(w, p) && wetAround(w, p); }
    public boolean canBlockStay(World w, BlockPos p) { return supported(w, p) && wetAround(w, p); }

    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos p, Block b, BlockPos from) {
        if (!w.isRemote && !canBlockStay(w, p)) {
            dropBlockAsItem(w, p, s, 0);
            w.setBlockState(p, wetAround(w, p) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
        }
    }

    /** broken by hand: leave a water source behind, not an air pocket */
    @Override
    public boolean removedByPlayer(IBlockState s, World w, BlockPos p, EntityPlayer pl, boolean willHarvest) {
        boolean wet = wetAround(w, p);
        boolean r = super.removedByPlayer(s, w, p, pl, willHarvest);
        if (r && wet && !w.isRemote) w.setBlockState(p, Blocks.WATER.getDefaultState(), 3);
        return r;
    }

    @Override
    public void onBlockExploded(World w, BlockPos p, Explosion e) {
        boolean wet = wetAround(w, p);
        super.onBlockExploded(w, p, e);
        if (wet && !w.isRemote) w.setBlockState(p, Blocks.WATER.getDefaultState(), 3);
    }
}
