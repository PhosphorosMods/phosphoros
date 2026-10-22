package lucifertotem.v130;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.HPotions;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/** 1.3.0 registration hub: called from the existing 1.1 registration points (HItems/HBlocks/HContent/HPotions/HSounds). */
public final class V130 {
    private V130() {}
    public static final List<Item> ITEMS = new ArrayList<>();

    public static <T extends Item> T item(IForgeRegistry<Item> r, T i, String name) {
        i.setRegistryName(LuciferTotem.MODID, name);
        i.setTranslationKey(LuciferTotem.MODID + "." + name);
        i.setCreativeTab(LuciferTotem.TAB);
        r.register(i);
        ITEMS.add(i);
        return i;
    }

    public static <T extends Block> T block(IForgeRegistry<Block> r, T b, String name, boolean item) {
        b.setRegistryName(LuciferTotem.MODID, name);
        b.setTranslationKey(LuciferTotem.MODID + "." + name);
        if (item) { b.setCreativeTab(LuciferTotem.TAB); ModContent.ITEM_BLOCKS.add(b); }
        r.register(b);
        return b;
    }

    // ------------------------------------------------------------ potions (own icon sheet: textures/gui/effects_130.png)
    public static HPotions.PotionLT SOMNUS, STINK, MANIA, KYKEON;

    public static class Potion130 extends HPotions.PotionLT {
        public Potion130(String name, boolean bad, int color, int icon) { super(name, bad, color, icon); }
        @Override
        @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
        public void renderInventoryEffect(net.minecraft.potion.PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z) { lucifertotem.client.V130Client.drawEffectIcon(icon, x + 6, y + 7, 1f); }
        @Override
        @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
        public void renderHUDEffect(net.minecraft.potion.PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z, float alpha) { lucifertotem.client.V130Client.drawEffectIcon(icon, x + 3, y + 3, alpha); }
    }

    public static void potions(IForgeRegistry<Potion> r) {
        SOMNUS = new Potion130("somnus", true, 0x6A4C9C, 0);
        SOMNUS.registerPotionAttributeModifier(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED, "a3c1e2d4-1300-4f00-9a00-000000000130", -0.15, 2);
        STINK = new Potion130("the_stink", true, 0x6B8E23, 1);
        MANIA = new Potion130("mania", true, 0xB0306A, 2);
        KYKEON = new Potion130("kykeon_vision", false, 0xD8C080, 3);
        r.register(SOMNUS); r.register(STINK); r.register(MANIA); r.register(KYKEON);
        Crops.potions(r);
    }

    // ------------------------------------------------------------ sounds (created before HSounds registers its ALL list)
    public static SoundEvent DISC_DEVILS_FLIGHT, MUREX_CRUNCH, CITATION, GOAT_BLEAT, HOUND_HOWL, HYDRA_ROAR, DRAKAINA_HISS, LYRE;
    static SoundEvent s(String name) {
        ResourceLocation rl = new ResourceLocation(LuciferTotem.MODID, name);
        SoundEvent e = new SoundEvent(rl).setRegistryName(rl);
        lucifertotem.hillman.HSounds.ALL.add(e);
        return e;
    }
    /** the disc's sound must exist when the record item is made (items register before sounds) */
    public static SoundEvent disc() {
        if (DISC_DEVILS_FLIGHT == null) { ResourceLocation rl = new ResourceLocation(LuciferTotem.MODID, "record.devils_flight"); DISC_DEVILS_FLIGHT = new SoundEvent(rl).setRegistryName(rl); }
        return DISC_DEVILS_FLIGHT;
    }
    public static void sounds() {
        MUREX_CRUNCH = s("murex.crunch");
        lucifertotem.hillman.HSounds.ALL.add(disc());
        CITATION = s("reviewer.citation");
        GOAT_BLEAT = s("goat.bleat");
        HOUND_HOWL = s("hound.howl");
        HYDRA_ROAR = s("hydra.roar");
        DRAKAINA_HISS = s("drakaina.hiss");
        LYRE = s("lyre.strum");
    }

    public static void items(IForgeRegistry<Item> r) { Apothecary.items(r); Stage2.items(r); Stage3.items(r); Stage4.items(r); Stage5.items(r); Crops.items(r); lucifertotem.v132.V132.items(r); }
    public static void blocks(IForgeRegistry<Block> r) { Apothecary.blocks(r); Stage3.blocks(r); Stage4.blocks(r); Stage5.blocks(r); Crops.blocks(r); lucifertotem.v132.V132.blocks(r); }

    public static int entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "murex"), EntityMurex.class, "lucifertotem.murex", id++, mod, 48, 5, true, 0x7A2A5A, 0xE0C8A0);
        net.minecraft.entity.EntitySpawnPlacementRegistry.setPlacementType(EntityMurex.class, net.minecraft.entity.EntityLiving.SpawnPlacementType.IN_WATER);
        V130Net.network();
        id = Stage2.entities(mod, id);
        id = Stage3.entities(mod, id);
        id = Stage4.entities(mod, id);
        id = Stage5.entities(mod, id);
        id = lucifertotem.v132.V132.entities(mod, id);
        GameRegistry.registerTileEntity(Apothecary.TileCrock.class, new ResourceLocation(LuciferTotem.MODID, "theriac_crock"));
        Stage4.tiles();
        return id;
    }

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Apothecary.Events());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new V130Net.Events());
        GameRegistry.addSmelting(Apothecary.TYRIAN_PURPLE, new net.minecraft.item.ItemStack(Apothecary.BURNING_PURPLE), 0.3f);
        Stage2.init(); Stage3.init(); Stage4.init(); Stage5.init(); Crops.init(); lucifertotem.v132.V132.init();
    }

    public static void recipes(IForgeRegistry<net.minecraft.item.crafting.IRecipe> r) {
        r.register(new Apothecary.RecipePorphyry().setRegistryName(LuciferTotem.MODID, "porphyry_dye"));
    }
}
