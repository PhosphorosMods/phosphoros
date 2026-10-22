package lucifertotem;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * PURPLE (from a Bottle of Theriac - the old universal antidote):
 *  - regenerates half a heart every 50 ticks (like Regeneration I),
 *  - takes 20% less damage (like Resistance I; applied in CommonEvents),
 *  - cures and wards off Poison and Wither while it lasts (the antidote part, handy against Medusa stings),
 *  - client: the world takes on a soft violet tint with brighter fog (ClientEvents).
 */
public class PotionPurple extends Potion {
    public static final int COLOR = 0x9B30FF;

    public PotionPurple() {
        super(false, COLOR);
        setRegistryName(LuciferTotem.MODID, "purple");
        setPotionName("effect." + LuciferTotem.MODID + ".purple");
        setBeneficial();
    }

    @Override public boolean isReady(int duration, int amplifier) { return true; }

    @Override
    public void performEffect(EntityLivingBase e, int amp) {
        if (e.world.isRemote) return;
        if (e.isPotionActive(MobEffects.POISON)) e.removePotionEffect(MobEffects.POISON);
        if (e.isPotionActive(MobEffects.WITHER)) e.removePotionEffect(MobEffects.WITHER);
        int every = Math.max(10, 50 >> amp);
        if (e.ticksExisted % every == 0 && e.getHealth() < e.getMaxHealth()) e.heal(1.0f);
    }

    @Override public boolean hasStatusIcon() { return false; } // own 18x18 icon below

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) { lucifertotem.client.PurpleIcon.draw(x + 6, y + 7, 1f); }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) { lucifertotem.client.PurpleIcon.draw(x + 3, y + 3, alpha); }
}
