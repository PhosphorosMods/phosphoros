package lucifertotem.world;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The Lightfall: eternal violet dusk over a glowing purple sea, islands adrift beneath the Morningstar. */
public class LightfallProvider extends WorldProvider {
    public static final float DUSK_ANGLE = 0.235f;

    @Override
    protected void init() {
        hasSkyLight = true;
        biomeProvider = new BiomeProviderSingle(ModContent.BIOME);
    }

    @Override public DimensionType getDimensionType() { return LuciferTotem.LIGHTFALL_TYPE; }
    @Override public IChunkGenerator createChunkGenerator() { return new LightfallChunkGenerator(world, world.getSeed()); }
    @Override public float calculateCelestialAngle(long time, float pt) { return DUSK_ANGLE; }
    @Override public boolean isSurfaceWorld() { return false; }
    @Override public boolean canRespawnHere() { return false; }
    @Override public boolean isDaytime() { return true; }
    @Override public int getAverageGroundLevel() { return 80; }
    @Override public boolean canCoordinateBeSpawn(int x, int z) { return false; }
    @Override public BlockPos getSpawnCoordinate() { return new BlockPos(0, 90, 0); }
    @Override public boolean doesXZShowFog(int x, int z) { return false; }
    @Override public double getVoidFogYFactor() { return 1.0; }
    @Override public boolean canDoLightning(Chunk c) { return false; }
    @Override public boolean canDoRainSnowIce(Chunk c) { return false; }
    @Override public void calculateInitialWeather() { }  // never rains or thunders here
    @Override public void updateWeather() { }
    @Override public boolean canSnowAt(BlockPos p, boolean b) { return false; }
    @Override public boolean canBlockFreeze(BlockPos p, boolean b) { return false; }
    @Override public WorldSleepResult canSleepAt(EntityPlayer p, BlockPos pos) { return WorldSleepResult.DENY; } // beds never explode here
    @Override public float[] calcSunriseSunsetColors(float a, float pt) { return null; }

    @Override @SideOnly(Side.CLIENT) public float getCloudHeight() { return 200.0f; }
    @Override @SideOnly(Side.CLIENT) public boolean isSkyColored() { return true; }
    @Override @SideOnly(Side.CLIENT) public Vec3d getFogColor(float a, float pt) { return new Vec3d(0.36, 0.20, 0.52); }
    @Override @SideOnly(Side.CLIENT) public Vec3d getSkyColor(Entity cam, float pt) { return new Vec3d(0.26, 0.12, 0.44); }
    @Override @SideOnly(Side.CLIENT) public Vec3d getCloudColor(float pt) { return new Vec3d(0.80, 0.62, 0.92); }
    @Override @SideOnly(Side.CLIENT) public float getStarBrightness(float pt) { return 0.8f; }
    @Override @SideOnly(Side.CLIENT) public IRenderHandler getSkyRenderer() { return lucifertotem.client.LightfallSkyRenderer.INSTANCE; }

    @Override
    @SideOnly(Side.CLIENT)
    public void getLightmapColors(float pt, float sun, float sky, float block, float[] c) {
        // gentle violet cast over everything
        c[0] = Math.min(1f, c[0] * 1.02f + 0.02f);
        c[1] = c[1] * 0.90f;
        c[2] = Math.min(1f, c[2] * 1.06f + 0.03f);
        float r = lucifertotem.client.HClient.ritual; // the witches' chant swallows the light
        if (r > 0) { float d = 1f - 0.4f * r; c[0] *= d; c[1] *= d; c[2] *= d; }
    }
}
