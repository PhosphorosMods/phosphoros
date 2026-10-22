package lucifertotem;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Bottle of Theriac: drink (like a potion) for 60 s of PURPLE; gives the glass bottle back. */
public class ItemTheriac extends Item {
    public static final int DURATION = 1200;

    public ItemTheriac() {
        setRegistryName(LuciferTotem.MODID, "theriac");
        setTranslationKey(LuciferTotem.MODID + ".theriac");
        setMaxStackSize(1);
        setCreativeTab(LuciferTotem.TAB);
    }

    @Override public EnumAction getItemUseAction(ItemStack s) { return EnumAction.DRINK; }
    @Override public int getMaxItemUseDuration(ItemStack s) { return 32; }
    @Override public EnumRarity getRarity(ItemStack s) { return EnumRarity.UNCOMMON; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
        p.setActiveHand(h);
        return new ActionResult<>(EnumActionResult.SUCCESS, p.getHeldItem(h));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World w, EntityLivingBase e) {
        EntityPlayer p = e instanceof EntityPlayer ? (EntityPlayer) e : null;
        if (p instanceof EntityPlayerMP) net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) p, stack);
        if (!w.isRemote) e.addPotionEffect(new PotionEffect(ModContent.PURPLE, DURATION, 0));
        if (!w.isRemote && p != null) lucifertotem.hillman.HEvents.onTheriac(p);
        if (p != null) p.addStat(StatList.getObjectUseStats(this));
        if (p == null || !p.capabilities.isCreativeMode) {
            stack.shrink(1);
            if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
            if (p != null) p.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack s) { return true; }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
        tip.add(TextFormatting.LIGHT_PURPLE + I18n.translateToLocal("effect.lucifertotem.purple") + " (1:00)");
        tip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.lucifertotem.theriac.1"));
        tip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + I18n.translateToLocal("tooltip.lucifertotem.theriac.2"));
    }
}
