package lucifertotem;

import lucifertotem.world.LightfallBiome;
import lucifertotem.world.LightfallProvider;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = LuciferTotem.MODID, name = "Phosphoros: Purple Mysteries", version = "1.3.3", acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = LuciferTotem.MODID)
public class LuciferTotem {
    public static final String MODID = "lucifertotem";
    public static final Logger LOG = LogManager.getLogger("LuciferTotem");

    @Mod.Instance(MODID)
    public static LuciferTotem INSTANCE;

    @SidedProxy(clientSide = "lucifertotem.client.ClientProxy", serverSide = "lucifertotem.CommonProxy")
    public static CommonProxy proxy;

    public static DimensionType LIGHTFALL_TYPE;
    public static int dimId = 616;
    public static boolean dimRegistered = false;

    public static final ResourceLocation LOOT_LIGHT_SHRINE = new ResourceLocation(MODID, "chests/light_shrine");
    public static final ResourceLocation LOOT_SPIRE = new ResourceLocation(MODID, "chests/fallen_spire");
    public static final ResourceLocation LOOT_CRYPT = new ResourceLocation(MODID, "chests/sunken_shrine");
    public static final ResourceLocation LOOT_CITADEL = new ResourceLocation(MODID, "chests/morningstar_citadel");
    public static final ResourceLocation LOOT_CHOIR = new ResourceLocation(MODID, "chests/drowned_choir");
    public static final ResourceLocation LOOT_DELPHI = new ResourceLocation(MODID, "chests/delphi");

    public static final CreativeTabs TAB = new CreativeTabs(MODID) {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() { return new ItemStack(ModContent.TOTEM); }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        dimId = LTConfig.dimensionId;
        if (DimensionManager.isDimensionRegistered(dimId)) {
            LOG.error("Dimension id {} is already registered by another mod; the Lightfall is DISABLED. Change dimensionId in config/lucifertotem.cfg.", dimId);
        } else {
            LIGHTFALL_TYPE = DimensionType.register("lightfall", "_lightfall", dimId, LightfallProvider.class, false);
            DimensionManager.registerDimension(dimId, LIGHTFALL_TYPE);
            dimRegistered = true;
            LOG.info("Registered the Lightfall as dimension {}", dimId);
        }
        int id = 0;
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "thrown_totem"), EntityThrownTotem.class, "lucifertotem.thrown_totem", id++, this, 64, 10, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "purple_bolt"), EntityPurpleBolt.class, "lucifertotem.purple_bolt", id++, this, 160, 20, false);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "medusa"), EntityMedusa.class, "lucifertotem.medusa", id++, this, 64, 3, true, 0x6A1FB0, 0xF0B8FF);
        net.minecraft.entity.EntitySpawnPlacementRegistry.setPlacementType(EntityMedusa.class, net.minecraft.entity.EntityLiving.SpawnPlacementType.IN_WATER);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "land_medusa"), EntityLandMedusa.class, "lucifertotem.land_medusa", id++, this, 80, 3, true, 0xEDE4FF, 0x8E2BD9);
        net.minecraft.entity.EntitySpawnPlacementRegistry.setPlacementType(EntityLandMedusa.class, net.minecraft.entity.EntityLiving.SpawnPlacementType.ON_GROUND);
        lucifertotem.hillman.HContent.entities(this, id);
        LootTableList.register(LOOT_LIGHT_SHRINE);
        LootTableList.register(LOOT_SPIRE);
        LootTableList.register(LOOT_CRYPT);
        LootTableList.register(LOOT_CITADEL);
        LootTableList.register(LOOT_CHOIR);
        LootTableList.register(lucifertotem.lore.OverworldLore.LOOT_GALLEY);
        LootTableList.register(lucifertotem.lore.OverworldLore.LOOT_ALTAR);
        lucifertotem.hillman.Alabastron.network();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new CommonEvents());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new lucifertotem.hillman.HEvents());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new lucifertotem.hillman.Alabastron.Events());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new lucifertotem.block.WateryEvents());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new lucifertotem.v131.LeakGuard());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new FishingLoot());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new lucifertotem.lore.LoreEvents());
        net.minecraftforge.fml.common.registry.GameRegistry.registerWorldGenerator(new lucifertotem.lore.OverworldLore(), 50);
        net.minecraftforge.fml.common.registry.GameRegistry.addSmelting(ModContent.CRACKED_DUSKSTONE_BRICKS, new ItemStack(ModContent.DUSKSTONE_BRICKS), 0.1f);
        lucifertotem.hillman.HContent.brewing();
        lucifertotem.hillman.HContent.spawns();
        lucifertotem.v130.V130.init();
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent e) {
        AetherCompat.init();
        if (AetherCompat.active()) net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new AetherCompat());
    }

    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent e) {
        e.registerServerCommand(new lucifertotem.v130.Interview.Cmd());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent e) {
        if (LTConfig.testHarness) TestHarness.start();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> e) { ModContent.registerBlocks(e.getRegistry()); lucifertotem.hillman.HBlocks.register(e.getRegistry()); lucifertotem.v130.V130.blocks(e.getRegistry()); }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<net.minecraft.util.SoundEvent> e) { lucifertotem.hillman.HSounds.register(e.getRegistry()); }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> e) { lucifertotem.hillman.HContent.recipes(e.getRegistry()); lucifertotem.v130.V130.recipes(e.getRegistry()); }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<net.minecraft.potion.Potion> e) {
        ModContent.PURPLE = new PotionPurple();
        e.getRegistry().register(ModContent.PURPLE);
        lucifertotem.hillman.HPotions.register(e.getRegistry());
        lucifertotem.v130.V130.potions(e.getRegistry());
    }

    @SubscribeEvent
    public static void registerPotionTypes(RegistryEvent.Register<net.minecraft.potion.PotionType> e) { lucifertotem.v130.Crops.potionTypes(e.getRegistry()); }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> e) { ModContent.registerItems(e.getRegistry()); lucifertotem.hillman.HItems.register(e.getRegistry()); lucifertotem.v130.V130.items(e.getRegistry()); }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> e) {
        LightfallBiome b = new LightfallBiome();
        b.setRegistryName(MODID, "lightfall");
        e.getRegistry().register(b);
        ModContent.BIOME = b;
        BiomeDictionary.addTypes(b, BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.VOID);
    }

    @SubscribeEvent
    public static void onConfigChanged(net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent e) {
        if (MODID.equals(e.getModID())) net.minecraftforge.common.config.ConfigManager.sync(MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
    }
}
