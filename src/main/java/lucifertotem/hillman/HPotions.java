package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/** Custom status effects of 1.1.0. Client visuals are driven from the synced effect in client.HClient. */
public class HPotions {
    public static PotionLT INSANIA, PNEUMA, THIRST, PETRIFYING, PETRIFIED, ORACLE, GLOOM;

    public static class PotionLT extends Potion {
        public final int icon;
        public PotionLT(String name, boolean bad, int color, int icon) {
            super(bad, color);
            this.icon = icon;
            setRegistryName(LuciferTotem.MODID, name);
            setPotionName("effect." + LuciferTotem.MODID + "." + name);
            if (!bad) setBeneficial();
        }
        @Override public boolean hasStatusIcon() { return false; }
        @Override
        @SideOnly(Side.CLIENT)
        public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) { lucifertotem.client.HClient.drawEffectIcon(icon, x + 6, y + 7, 1f); }
        @Override
        @SideOnly(Side.CLIENT)
        public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) { lucifertotem.client.HClient.drawEffectIcon(icon, x + 3, y + 3, alpha); }
    }

    /** Dipsas Thirst: the stomach burns, food drains away; milk does not wash it out (only drinking helps, see HEvents) */
    public static class PotionThirst extends PotionLT {
        PotionThirst() { super("dipsas_thirst", true, 0xD8B25A, 2); }
        @Override public boolean isReady(int d, int a) { return true; }
        @Override public void performEffect(EntityLivingBase e, int amp) {
            if (!e.world.isRemote && e instanceof EntityPlayer) ((EntityPlayer) e).addExhaustion(0.06f * (amp + 1));
        }
        @Override public List<ItemStack> getCurativeItems() { return new ArrayList<>(); }
    }

    /** Petrifying: harmless stacks (amplifier = stacks-1) from the Gorgon's gaze; 5 stacks petrify */
    public static class PotionPetrifying extends PotionLT {
        PotionPetrifying() { super("petrifying", true, 0x9A9A8A, 4); }
        @Override public List<ItemStack> getCurativeItems() { return new ArrayList<>(); }
    }

    public static void register(IForgeRegistry<Potion> r) {
        INSANIA = new PotionLT("insania", true, 0xC8B830, 0);
        PNEUMA = new PotionLT("pneuma", false, 0xB070FF, 1);
        THIRST = new PotionThirst();
        PETRIFYING = new PotionPetrifying();
        PETRIFIED = new PotionLT("petrified", true, 0x707070, 5);
        PETRIFIED.registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED, "6b1f6f0e-3f2c-4b7e-9a65-1d2c0f4a7e11", -1.0, 2);
        ORACLE = new PotionLT("oracle_of_trophonius", false, 0x6040A0, 6);
        GLOOM = new PotionLT("trophonian_gloom", true, 0x303048, 7) {
            @Override public List<ItemStack> getCurativeItems() { return new ArrayList<>(); }
        };
        r.register(INSANIA); r.register(PNEUMA); r.register(THIRST); r.register(PETRIFYING); r.register(PETRIFIED); r.register(ORACLE); r.register(GLOOM);
    }
}
