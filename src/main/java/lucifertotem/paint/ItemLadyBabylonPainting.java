package lucifertotem.paint;

import lucifertotem.LuciferTotem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** "Lady Babylon Painting": without NBT it hangs the largest painting that fits; with NBT 'variant' exactly that one. */
public class ItemLadyBabylonPainting extends Item {
    public static Item INSTANCE;
    public ItemLadyBabylonPainting() { setCreativeTab(LuciferTotem.TAB); INSTANCE = this; }

    public static ItemStack of(Paintings.Variant v) {
        ItemStack s = new ItemStack(INSTANCE);
        if (v != null) { NBTTagCompound t = new NBTTagCompound(); t.setString("variant", v.name); s.setTagCompound(t); }
        return s;
    }
    public static Paintings.Variant variant(ItemStack s) {
        return s.hasTagCompound() ? Paintings.byName(s.getTagCompound().getString("variant")) : null;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing face, float hx, float hy, float hz) {
        ItemStack s = p.getHeldItem(hand);
        BlockPos at = pos.offset(face);
        if (face.getAxis() == EnumFacing.Axis.Y || !p.canPlayerEdit(at, face, s)) return EnumActionResult.FAIL;
        EntityLTPainting e = new EntityLTPainting(w, at, face, variant(s), w.rand);
        if (!e.onValidSurface()) return EnumActionResult.FAIL;
        if (!w.isRemote) {
            e.playPlaceSound();
            w.spawnEntity(e);
            EntityLTPainting.placed++;
        }
        s.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        items.add(new ItemStack(this));
        for (Paintings.Variant v : Paintings.ALL) items.add(of(v));
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getItemStackDisplayName(ItemStack s) {
        String base = super.getItemStackDisplayName(s);
        Paintings.Variant v = variant(s);
        return v == null ? base : base + ": " + I18n.translateToLocal(v.titleKey());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("deprecation")
    public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
        Paintings.Variant v = variant(s);
        tip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("tooltip.lucifertotem.lady_babylon_painting.1"));
        if (v == null) { tip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.lucifertotem.lady_babylon_painting.random")); return; }
        tip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + I18n.translateToLocal(v.authorKey()));
        tip.add(TextFormatting.GRAY + "" + v.w + "x" + v.h);
    }
}
