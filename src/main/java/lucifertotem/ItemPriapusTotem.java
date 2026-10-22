package lucifertotem;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Priapus Totem: a little carved garden-guardian herm. Carried in the hotbar or offhand: Speed I + faster healing. */
public class ItemPriapusTotem extends Item {
    public ItemPriapusTotem() {
        setRegistryName(LuciferTotem.MODID, "priapus_totem");
        setTranslationKey(LuciferTotem.MODID + ".priapus_totem");
        setMaxStackSize(1);
        setCreativeTab(LuciferTotem.TAB);
    }

    @Override public EnumRarity getRarity(ItemStack s) { return EnumRarity.RARE; }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
        tip.add(TextFormatting.GREEN + I18n.translateToLocal("tooltip.lucifertotem.priapus.1"));
        tip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.lucifertotem.priapus.2"));
        tip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + I18n.translateToLocal("tooltip.lucifertotem.priapus.3"));
    }
}
