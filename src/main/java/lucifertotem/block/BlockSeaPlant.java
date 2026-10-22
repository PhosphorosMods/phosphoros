package lucifertotem.block;

import lucifertotem.LuciferTotem;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Cross-model, collision-less glowing sea flora (tendril stalks, tendril bulbs, dusk coral).
 * Watery (see BlockWatery): the cell stays water. Breaks (no cascade beyond its own column) when the block beneath is removed.
 */
public class BlockSeaPlant extends BlockWatery {
    private static final AxisAlignedBB BOX = new AxisAlignedBB(0.15, 0.0, 0.15, 0.85, 1.0, 0.85);
    private final boolean bubbles;

    public BlockSeaPlant(String name, float light, boolean bubbles) {
        super(name, SoundType.PLANT, 0.0f, light, BOX);
        this.bubbles = bubbles;
    }

    @Override
    protected boolean supported(World w, BlockPos p) {
        IBlockState b = w.getBlockState(p.down());
        return b.getBlock() instanceof BlockSeaPlant || b.isSideSolid(w, p.down(), EnumFacing.UP);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (bubbles && r.nextInt(6) == 0)
            w.spawnParticle(w.getBlockState(p.up()).getMaterial() == Material.WATER ? EnumParticleTypes.WATER_BUBBLE : EnumParticleTypes.SPELL_WITCH,
                p.getX() + 0.3 + r.nextDouble() * 0.4, p.getY() + 0.8, p.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.05, 0);
    }
}
