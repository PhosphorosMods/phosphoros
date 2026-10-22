package lucifertotem.client;

import lucifertotem.LuciferTotem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** draws the 18x18 PURPLE status icon */
public class PurpleIcon {
    private static final ResourceLocation ICON = new ResourceLocation(LuciferTotem.MODID, "textures/gui/purple_effect.png");
    public static void draw(int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICON);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18f, 18f);
    }
}
