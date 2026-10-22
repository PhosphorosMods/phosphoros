package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.ModContent;
import lucifertotem.world.LightfallChunkGenerator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/** 1.3.0 worldgen in the Lightfall populate window (bx..bx+15, bz..bz+15). Every feature uses its own salted random. */
public final class V130Gen {
    private V130Gen() {}
    public static int poppyPatches = 0;

    public static void populate(World w, long seed, int cx, int cz, int bx, int bz) {
        if (LTConfig.v130.duskPoppy && Apothecary.DUSK_POPPY != null) {
            Random r = LightfallChunkGenerator.chunkRandom(seed, cx, cz, 0x130AL);
            if (r.nextInt(5) == 0) { plant(w, r, bx, bz, Apothecary.DUSK_POPPY.getDefaultState(), 3 + r.nextInt(5)); poppyPatches++; }
        }
        Gen130.structures(w, seed, cx, cz, bx, bz);
        Labyrinth.populate(w, seed, bx, bz);
    }

    public static int plant(World w, Random r, int bx, int bz, IBlockState s, int n) {
        int x = bx + 3 + r.nextInt(10), z = bz + 3 + r.nextInt(10), placed = 0;
        for (int k = 0; k < n; k++) {
            int px = x + r.nextInt(5) - 2, pz = z + r.nextInt(5) - 2;
            int y = LightfallChunkGenerator.topY(w, px, pz, LightfallChunkGenerator.SEA_LEVEL + 1);
            BlockPos at = new BlockPos(px, y + 1, pz);
            if (y > 0 && w.getBlockState(at.down()).getBlock() == ModContent.STAR_GRASS && w.isAirBlock(at)) { w.setBlockState(at, s, 2); placed++; }
        }
        return placed;
    }
}
