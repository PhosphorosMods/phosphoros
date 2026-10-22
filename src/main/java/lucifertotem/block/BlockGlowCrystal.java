package lucifertotem.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
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

/** A cluster of radiant violet crystals; full light. */
public class BlockGlowCrystal extends BlockLT {
    private static final AxisAlignedBB BOX = new AxisAlignedBB(0.15, 0.0, 0.15, 0.85, 0.85, 0.85);

    public BlockGlowCrystal() {
        super(Material.GLASS, SoundType.GLASS, 0.4f, 1.5f, "glow_crystal");
        setLightLevel(1.0f);
        setLightOpacity(0);
    }

    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
        if (r.nextInt(4) == 0)
            w.spawnParticle(EnumParticleTypes.SPELL_WITCH, p.getX() + 0.2 + r.nextDouble() * 0.6, p.getY() + 0.3 + r.nextDouble() * 0.6, p.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.02, 0);
    }
}
