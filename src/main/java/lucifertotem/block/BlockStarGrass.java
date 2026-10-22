package lucifertotem.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.IPlantable;

/** Violet star-speckled turf of the Lightfall. Does not spread; sustains plants like grass. */
public class BlockStarGrass extends BlockLT {
    public BlockStarGrass() {
        super(Material.GRASS, SoundType.PLANT, 0.6f, 3f, "star_grass");
        setHarvestLevel("shovel", 0);
        setLightLevel(0.2f);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
        switch (plantable.getPlantType(world, pos.offset(direction))) {
            case Plains: return true;
            default: return false;
        }
    }
}
