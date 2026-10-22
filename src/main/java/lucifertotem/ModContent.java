package lucifertotem;

import lucifertotem.block.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class ModContent {
    public static BlockLT DUSKSTONE, DUSKSTONE_BRICKS, STAR_GRASS;
    public static BlockGlowCrystal GLOW_CRYSTAL;
    public static BlockFallenFeather FALLEN_FEATHER;
    public static BlockLightfallPortal PORTAL;
    public static BlockPurpleRift RIFT;
    public static BlockSeaPlant TENDRIL, TENDRIL_BULB, DUSK_CORAL;
    public static BlockStarfish STARFISH;
    public static BlockSeaCrystal CRYSTAL_CORAL;
    public static BlockWatcher WATCHER;
    public static BlockLT HALO_BRASS, CRACKED_DUSKSTONE_BRICKS;
    public static ItemLuciferTotem TOTEM;
    public static Item SHARD;
    public static ItemPriapusTotem PRIAPUS;
    public static ItemTheriac THERIAC;
    public static PotionPurple PURPLE;
    public static Biome BIOME;
    public static final List<Block> ITEM_BLOCKS = new ArrayList<>();

    static void registerBlocks(IForgeRegistry<Block> r) {
        DUSKSTONE = reg(r, new BlockLT(Material.ROCK, SoundType.STONE, 1.5f, 10f, "duskstone"), true);
        DUSKSTONE_BRICKS = reg(r, new BlockLT(Material.ROCK, SoundType.STONE, 2.0f, 12f, "duskstone_bricks"), true);
        STAR_GRASS = reg(r, new BlockStarGrass(), true);
        GLOW_CRYSTAL = reg(r, new BlockGlowCrystal(), true);
        FALLEN_FEATHER = reg(r, new BlockFallenFeather(), true);
        PORTAL = reg(r, new BlockLightfallPortal(), false);
        RIFT = reg(r, new BlockPurpleRift(), false);
        TENDRIL = reg(r, new BlockSeaPlant("tendril", 0.4f, false), true);
        TENDRIL_BULB = reg(r, new BlockSeaPlant("tendril_bulb", 1.0f, true), true);
        DUSK_CORAL = reg(r, new BlockSeaPlant("dusk_coral", 0.5f, true), true);
        STARFISH = reg(r, new BlockStarfish(), true);
        WATCHER = reg(r, new BlockWatcher(), true);
        CRYSTAL_CORAL = reg(r, new BlockSeaCrystal(), true);
        CRACKED_DUSKSTONE_BRICKS = reg(r, new BlockLT(Material.ROCK, SoundType.STONE, 1.5f, 8f, "cracked_duskstone_bricks"), true);
        HALO_BRASS = reg(r, new BlockLT(Material.IRON, SoundType.METAL, 3.0f, 10f, "halo_brass"), true);
        HALO_BRASS.setLightLevel(0.8f);
        HALO_BRASS.setHarvestLevel("pickaxe", 1);
    }

    private static <T extends Block> T reg(IForgeRegistry<Block> r, T b, boolean item) {
        r.register(b);
        if (item) ITEM_BLOCKS.add(b);
        return b;
    }

    static void registerItems(IForgeRegistry<Item> r) {
        for (Block b : ITEM_BLOCKS) r.register(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        TOTEM = new ItemLuciferTotem();
        r.register(TOTEM);
        SHARD = new Item() {
            @Override
            @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
            public boolean hasEffect(net.minecraft.item.ItemStack s) { return true; }
        }.setRegistryName(LuciferTotem.MODID, "morningstar_shard").setTranslationKey(LuciferTotem.MODID + ".morningstar_shard").setCreativeTab(LuciferTotem.TAB);
        r.register(SHARD);
        PRIAPUS = new ItemPriapusTotem();
        r.register(PRIAPUS);
        THERIAC = new ItemTheriac();
        r.register(THERIAC);
    }
}
