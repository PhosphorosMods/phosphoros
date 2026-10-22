package lucifertotem.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Crystal coral: the glow crystal as it grows under the sea (watery, see BlockWatery). Sits on the floor or hangs from above. */
public class BlockSeaCrystal extends BlockWatery {
    private static final AxisAlignedBB BOX = new AxisAlignedBB(0.15, 0.0, 0.15, 0.85, 0.85, 0.85);

    public BlockSeaCrystal() { super("crystal_coral", SoundType.GLASS, 0.4f, 1.0f, BOX); setResistance(1.5f); }

    @Override
    protected boolean supported(World w, BlockPos p) {
        return w.getBlockState(p.down()).isSideSolid(w, p.down(), EnumFacing.UP) || w.getBlockState(p.up()).getMaterial().blocksMovement(); // hangs from islands, leaves, rock
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (r.nextInt(5) == 0)
            w.spawnParticle(EnumParticleTypes.WATER_BUBBLE, p.getX() + 0.2 + r.nextDouble() * 0.6, p.getY() + 0.3 + r.nextDouble() * 0.6, p.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.04, 0);
    }
}
