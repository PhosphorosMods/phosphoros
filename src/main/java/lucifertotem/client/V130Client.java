package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.v130.Apothecary;
import lucifertotem.v130.EntityMurex;
import lucifertotem.v130.V130;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** client side of 1.3.0 */
@Mod.EventBusSubscriber(modid = LuciferTotem.MODID, value = Side.CLIENT)
public class V130Client {
    static final ResourceLocation ICONS = new ResourceLocation(LuciferTotem.MODID, "textures/gui/effects_130.png");

    public static void drawEffectIcon(int icon, int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONS);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, icon * 18, 0, 18, 18, 144f, 18f);
    }

    public static void renderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityMurex.class, RenderMurex::new);
        V130Render.renderers();
        V132Client.renderers();
    }

    public static void init() { V130Render.init(); V132Client.init(); }

    /** Somnus: the world closes in like a heavy-lidded dusk */
    @SubscribeEvent
    public static void fog(EntityViewRenderEvent.RenderFogEvent e) {
        if (!(e.getEntity() instanceof EntityLivingBase) || V130.SOMNUS == null) return;
        if (!((EntityLivingBase) e.getEntity()).isPotionActive(V130.SOMNUS)) return;
        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        GlStateManager.setFogStart(2f);
        GlStateManager.setFogEnd(Math.min(e.getFarPlaneDistance(), 22f));
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) { Apothecary.codeTooltip(e.getItemStack(), e.getToolTip()); }

    /** the murex: a spiral, spiny shell on a soft foot. 32x32 */
    public static class RenderMurex extends RenderLiving<EntityMurex> {
        static final ResourceLocation TEX = new ResourceLocation(LuciferTotem.MODID, "textures/entity/murex.png");
        public RenderMurex(RenderManager m) { super(m, new ModelMurex(), 0.2f); }
        @Override protected ResourceLocation getEntityTexture(EntityMurex e) { return TEX; }
    }

    public static class ModelMurex extends ModelBase {
        final ModelRenderer foot, shell, spire, tip, siphon;
        final ModelRenderer[] spines = new ModelRenderer[6];
        public ModelMurex() {
            textureWidth = 32; textureHeight = 32;
            foot = new ModelRenderer(this, 0, 22); foot.addBox(-2f, -1f, -4f, 4, 1, 8); foot.setRotationPoint(0, 24f, 0);
            shell = new ModelRenderer(this, 0, 0); shell.addBox(-2.5f, -4f, -2.5f, 5, 4, 6); shell.setRotationPoint(0, 23f, 0);
            spire = new ModelRenderer(this, 0, 10); spire.addBox(-1.5f, -3f, -1f, 3, 3, 4); spire.setRotationPoint(0, -3.5f, 2f); shell.addChild(spire);
            tip = new ModelRenderer(this, 14, 10); tip.addBox(-0.5f, -2f, 0f, 1, 2, 2); tip.setRotationPoint(0, -2.5f, 2f); spire.addChild(tip);
            siphon = new ModelRenderer(this, 22, 0); siphon.addBox(-0.5f, -1f, -4f, 1, 1, 3); siphon.setRotationPoint(0, -1f, -2f); shell.addChild(siphon);
            for (int i = 0; i < spines.length; i++) {
                spines[i] = new ModelRenderer(this, 22, 6);
                spines[i].addBox(-0.5f, -2f, -0.5f, 1, 2, 1);
                float side = (i % 2 == 0) ? -2.5f : 2.5f;
                spines[i].setRotationPoint(side, -2.5f, -1.5f + (i / 2) * 2f);
                spines[i].rotateAngleZ = side < 0 ? -0.7f : 0.7f;
                shell.addChild(spines[i]);
            }
            spire.rotateAngleX = -0.35f;
        }
        @Override public void render(Entity e, float ls, float la, float age, float yaw, float pitch, float scale) {
            foot.rotationPointZ = (float) Math.sin(age * 0.15f) * 0.3f;
            foot.render(scale); shell.render(scale);
        }
    }
}
