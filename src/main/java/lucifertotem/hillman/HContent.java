package lucifertotem.hillman;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.HashSet;
import java.util.Set;

/** Registration glue for 1.1.0. */
public class HContent {
    public static final ResourceLocation LOOT_DIPSAS = new ResourceLocation(LuciferTotem.MODID, "entities/dipsas");

    /** entity ids continue after the 1.0.x ones (0..3) */
    public static void entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "dipsas"), EntityDipsas.class, "lucifertotem.dipsas", id++, mod, 64, 3, true, 0xC9A55A, 0x5A3A1A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "doctor_ammonius"), EntityAmmonius.class, "lucifertotem.doctor_ammonius", id++, mod, 80, 3, true, 0xF2EEE0, 0x7A2AC8);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "thessalian_witch"), EntityThessalianWitch.class, "lucifertotem.thessalian_witch", id++, mod, 80, 3, true, 0x2A1A3A, 0xC0C0E0);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "pythia"), EntityPythia.class, "lucifertotem.pythia", id++, mod, 64, 10, false, 0xF0E6C8, 0xB070FF);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "gorgon"), EntityGorgon.class, "lucifertotem.gorgon", id++, mod, 96, 3, true, 0x4A6A3A, 0xB08A3A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "medea_flask"), EntityMedeaFlask.class, "lucifertotem.medea_flask", id++, mod, 64, 10, true);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "enchantress"), EntityEnchantress.class, "lucifertotem.enchantress", id++, mod, 80, 3, true, 0x3A1A5A, 0x6AA0FF);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "book_wraith"), EntityBookWraith.class, "lucifertotem.book_wraith", id++, mod, 64, 3, true);
        // 1.2.1: Lady Babylon paintings (tracked like vanilla paintings: 160 blocks, never re-sent, no velocity)
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "lady_babylon_painting"), lucifertotem.paint.EntityLTPainting.class, "lucifertotem.lady_babylon_painting", id++, mod, 160, Integer.MAX_VALUE, false);
        id = lucifertotem.v130.V130.entities(mod, id);   // 1.3.0
        LootTableList.register(EntityEnchantress.LOOT);
        LootTableList.register(lucifertotem.world.LibraryGen.LOOT);
        LootTableList.register(lucifertotem.world.LibraryGen.LOOT_APOTHECARY);
        LootTableList.register(lucifertotem.world.LibraryGen.LOOT_STARMAP);
        for (Class<? extends EntityLiving> c : new Class[]{EntityDipsas.class, EntityAmmonius.class, EntityThessalianWitch.class, EntityGorgon.class, EntityPythia.class})
            EntitySpawnPlacementRegistry.setPlacementType(c, EntityLiving.SpawnPlacementType.ON_GROUND);
        LootTableList.register(LOOT_DIPSAS);
        LootTableList.register(LuciferTotem.LOOT_DELPHI);
    }

    /** natural spawns outside the Lightfall (the Lightfall list lives in LightfallBiome) */
    public static void spawns() {
        if (LTConfig.dipsasOverworldWeight > 0) {
            Set<Biome> sandy = new HashSet<>(BiomeDictionary.getBiomes(BiomeDictionary.Type.SANDY));
            sandy.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.BEACH));
            sandy.removeIf(b -> BiomeDictionary.hasType(b, BiomeDictionary.Type.NETHER) || BiomeDictionary.hasType(b, BiomeDictionary.Type.END) || b == ModContent.BIOME
                || BiomeDictionary.hasType(b, BiomeDictionary.Type.MESA));
            EntityRegistry.addSpawn(EntityDipsas.class, LTConfig.dipsasOverworldWeight, 1, 1, EnumCreatureType.MONSTER, sandy.toArray(new Biome[0]));
        }
        if (LTConfig.ammoniusOverworldWeight > 0)
            EntityRegistry.addSpawn(EntityAmmonius.class, LTConfig.ammoniusOverworldWeight, 1, 1, EnumCreatureType.MONSTER, Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.SAVANNA);
    }

    /** awkward potion + venom vial -> Mithridatium */
    public static class MithridatiumBrew implements IBrewingRecipe {
        @Override public boolean isInput(ItemStack in) { return in.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(in) == PotionTypes.AWKWARD; }
        @Override public boolean isIngredient(ItemStack ing) { return ing.getItem() == HItems.VENOM_VIAL; }
        @Override public ItemStack getOutput(ItemStack in, ItemStack ing) { return isInput(in) && isIngredient(ing) ? new ItemStack(HItems.MITHRIDATIUM) : ItemStack.EMPTY; }
    }

    public static void brewing() { BrewingRecipeRegistry.addRecipe(new MithridatiumBrew()); }

    public static void recipes(IForgeRegistry<IRecipe> r) {
        r.register(new HItems.RecipeRebind().setRegistryName(LuciferTotem.MODID, "sibyl_rebind"));
        r.register(new Alabastron.RecipeLace().setRegistryName(LuciferTotem.MODID, "alabastron_lace"));
    }
}
