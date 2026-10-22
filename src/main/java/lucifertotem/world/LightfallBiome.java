package lucifertotem.world;

import lucifertotem.ModContent;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The single, peaceful biome of the Lightfall. No natural spawns at all (other mods' spawn injections are ignored too). */
public class LightfallBiome extends Biome {
    /** vivid violet; tints the (greyscale, see client.LightfallWaterClient) Lightfall water sprites via BiomeColorHelper */
    public static final int WATER_COLOR = 0xA040FF;

    public LightfallBiome() {
        super(new BiomeProperties("Lightfall").setRainDisabled().setTemperature(0.7f).setRainfall(0.0f).setWaterColor(WATER_COLOR).setBaseHeight(0.1f).setHeightVariation(0.1f));
        spawnableMonsterList.clear();
        spawnableCreatureList.clear();
        spawnableWaterCreatureList.clear();
        spawnableCaveCreatureList.clear();
        if (ModContent.STAR_GRASS != null) topBlock = ModContent.STAR_GRASS.getDefaultState();
        if (ModContent.DUSKSTONE != null) fillerBlock = ModContent.DUSKSTONE.getDefaultState();
    }

    private final List<SpawnListEntry> water = new java.util.ArrayList<>();
    /** filled only by AetherCompat (Valkyries) when the Aether is installed */
    public final List<SpawnListEntry> monsters = new java.util.ArrayList<>();
    private boolean landAdded;

    /** only the sea is alive: medusae (and a few squid), plus rare fallen-angel Valkyries if the Aether is installed; other mods' injections are ignored */
    @Override public List<SpawnListEntry> getSpawnableList(EnumCreatureType t) {
        if (t == EnumCreatureType.WATER_CREATURE) {
            if (water.isEmpty() && lucifertotem.LTConfig.medusaWeight > 0) {
                water.add(new SpawnListEntry(lucifertotem.EntityMedusa.class, lucifertotem.LTConfig.medusaWeight, 1, 3));
                water.add(new SpawnListEntry(net.minecraft.entity.passive.EntitySquid.class, 3, 1, 2));
                if (lucifertotem.LTConfig.v130.murexWeight > 0) water.add(new SpawnListEntry(lucifertotem.v130.EntityMurex.class, lucifertotem.LTConfig.v130.murexWeight, 2, 4));
            }
            return water;
        }
        if (t == EnumCreatureType.MONSTER) {
            if (!landAdded) {
                landAdded = true;
                if (lucifertotem.LTConfig.landMedusaWeight > 0) monsters.add(0, new SpawnListEntry(lucifertotem.EntityLandMedusa.class, lucifertotem.LTConfig.landMedusaWeight, 1, 2));
                if (lucifertotem.LTConfig.dipsasWeight > 0) monsters.add(new SpawnListEntry(lucifertotem.hillman.EntityDipsas.class, lucifertotem.LTConfig.dipsasWeight, 1, 1));
                if (lucifertotem.LTConfig.v130.huntressWeight > 0) monsters.add(new SpawnListEntry(lucifertotem.v130.EntityHuntress.class, lucifertotem.LTConfig.v130.huntressWeight, 1, 1));
                if (lucifertotem.LTConfig.ammoniusWeight > 0) monsters.add(new SpawnListEntry(lucifertotem.hillman.EntityAmmonius.class, lucifertotem.LTConfig.ammoniusWeight, 1, 1));
            }
            return monsters;
        }
        return Collections.emptyList();
    }
    @Override public void decorate(World w, Random r, BlockPos p) { } // all decoration happens in LightfallChunkGenerator.populate
    @Override public float getSpawningChance() { return 0.02f; }

    @Override @SideOnly(Side.CLIENT) public int getSkyColorByTemp(float t) { return 0x4A2478; }
    @Override @SideOnly(Side.CLIENT) public int getGrassColorAtPos(BlockPos p) { return 0x9C6ADE; }
    @Override @SideOnly(Side.CLIENT) public int getFoliageColorAtPos(BlockPos p) { return 0xC27BEA; }
}
