package lucifertotem.block;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** A pale-gold feather fallen from the light-bringer's wings, planted upright in the turf. Softly glowing. */
public class BlockFallenFeather extends BlockBush {
    public BlockFallenFeather() {
        super(Material.PLANTS);
        setSoundType(SoundType.CLOTH);
        setHardness(0f);
        setLightLevel(0.5f);
        setRegistryName(LuciferTotem.MODID, "fallen_feather");
        setTranslationKey(LuciferTotem.MODID + ".fallen_feather");
        setCreativeTab(LuciferTotem.TAB);
    }

    @Override
    protected boolean canSustainBush(IBlockState s) {
        return s.getBlock() == ModContent.STAR_GRASS || s.getBlock() == Blocks.GRASS || s.getBlock() == Blocks.DIRT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (r.nextInt(10) == 0)
            w.spawnParticle(EnumParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.7, p.getZ() + 0.5, (r.nextDouble() - 0.5) * 0.02, 0.02, (r.nextDouble() - 0.5) * 0.02);
    }
}
