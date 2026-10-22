package lucifertotem.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** A great glowing eye set in the sea floor. It blinks (animated texture). It is only watching. */
public class BlockWatcher extends BlockLT {
    public BlockWatcher() {
        super(Material.ROCK, SoundType.SLIME, 1.2f, 6f, "watcher_eye");
        setLightLevel(0.7f);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (r.nextInt(8) == 0)
            w.spawnParticle(EnumParticleTypes.WATER_BUBBLE, p.getX() + r.nextDouble(), p.getY() + 1.05, p.getZ() + r.nextDouble(), 0, 0.04, 0);
    }
}
