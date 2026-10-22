package lucifertotem.block;

import lucifertotem.LuciferTotem;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** A flat, softly glowing five-armed "star that fell into the sea". */
public class BlockStarfish extends BlockWatery {
    private static final AxisAlignedBB BOX = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0);

    public BlockStarfish() { super("starfish", SoundType.SLIME, 0.1f, 0.6f, BOX); }

    @Override
    protected boolean supported(World w, BlockPos p) { return w.getBlockState(p.down()).isSideSolid(w, p.down(), EnumFacing.UP); }
}
