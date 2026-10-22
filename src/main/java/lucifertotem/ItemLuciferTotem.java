package lucifertotem;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Totem of Lucifer: thrown like a pearl, returns to nothing (never consumed), calls down violet lightning. */
public class ItemLuciferTotem extends Item {
    public ItemLuciferTotem() {
        setRegistryName(LuciferTotem.MODID, "totem_of_lucifer");
        setTranslationKey(LuciferTotem.MODID + ".totem_of_lucifer");
        setMaxStackSize(1);
        setCreativeTab(LuciferTotem.TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, SoundCategory.PLAYERS, 0.6f, 1.4f + world.rand.nextFloat() * 0.2f);
        player.getCooldownTracker().setCooldown(this, LTConfig.throwCooldownTicks);
        if (!world.isRemote) {
            EntityThrownTotem t = new EntityThrownTotem(world, player);
            t.shoot(player, player.rotationPitch, player.rotationYaw, 0.0f, 1.8f, 0.4f);
            world.spawnEntity(t);
        }
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.SUCCESS, stack); // never consumed
    }

    /** 1.2.0: strike (use) the totem on an altar frame of duskstone bricks to light it; anywhere else it is thrown as before */
    @Override
    public net.minecraft.util.EnumActionResult onItemUse(EntityPlayer p, World w, net.minecraft.util.math.BlockPos pos, EnumHand hand, net.minecraft.util.EnumFacing face, float hx, float hy, float hz) {
        net.minecraft.block.Block b = w.getBlockState(pos).getBlock();
        if (b == ModContent.RIFT) {   // 1.2.1: sneak + use the totem on a purple rift to close it
            if (!p.isSneaking()) return EnumActionResult.PASS;
            lucifertotem.world.RiftLogic.tryClose(p, w, pos);
            p.swingArm(hand);
            return EnumActionResult.SUCCESS;
        }
        if (b != ModContent.DUSKSTONE_BRICKS && b != ModContent.CRACKED_DUSKSTONE_BRICKS) return EnumActionResult.PASS;
        lucifertotem.lore.AltarLogic.Frame f = lucifertotem.lore.AltarLogic.find(w, pos);
        if (f == null) return EnumActionResult.PASS;
        if (!w.isRemote) lucifertotem.lore.AltarLogic.tryLight(w, f, p);
        p.swingArm(hand);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public EnumRarity getRarity(ItemStack s) { return EnumRarity.EPIC; }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack s) { return true; }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
        tip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + I18n.translateToLocal("tooltip.lucifertotem.totem.1"));
        tip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.lucifertotem.totem.2"));
        tip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("tooltip.lucifertotem.totem.3"));
    }
}
